/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.cargofull;

import com.google.inject.Provides;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import net.runelite.api.Actor;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.ItemContainer;
import net.runelite.api.NPC;
import net.runelite.api.ObjectComposition;
import net.runelite.api.Player;
import net.runelite.api.Scene;
import net.runelite.api.Tile;
import net.runelite.api.WorldView;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.OverheadTextChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.events.WorldEntityDespawned;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.Notifier;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Alerts when the cargo hold of the boat you are sailing fills up.
 * <p>
 * The used count comes from the hold's item container, which the server updates as cargo
 * moves. The capacity comes from the cargo hold object built on the boat, refined by the
 * numbers in the cargo hold interface whenever it is opened. A crewmate saying the hold is
 * full, or the matching game message, also triggers the alert.
 */
@PluginDescriptor(
	name = "Cargo Full",
	description = "Sound and on-screen alerts when your boat's cargo hold fills up while sailing",
	tags = {"sailing", "cargo", "hold", "salvage", "boat", "notification", "alert"}
)
public class CargoFullPlugin extends Plugin
{
	private static final Logger LOG = LoggerFactory.getLogger(CargoFullPlugin.class);

	/** Appears in the crewmate's speech and in game messages when nothing more fits in the hold. */
	static final String HOLD_FULL_TEXT = "cargo hold is full";

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private Notifier notifier;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private CargoFullOverlay overlay;

	@Inject
	private CargoFullConfig config;

	/** Cargo hold object id seen in each boat's world view. */
	private final Map<Integer, Integer> cargoHoldByWorldView = new HashMap<>();
	/** Capacity read from the cargo hold interface, indexed by boat slot 1 to 5. */
	private final int[] capacityFromInterface = new int[CargoHoldContainers.BOAT_SLOTS + 1];
	private final CargoHoldMonitor monitor = new CargoHoldMonitor();
	/** Boat slot the monitor currently describes, or 0 when unknown. */
	private int monitoredSlot;
	private long alertShownAt;
	private boolean readInterfaceNextTick;

	@Provides
	CargoFullConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(CargoFullConfig.class);
	}

	@Override
	protected void startUp()
	{
		clearState();
		overlayManager.add(overlay);
		clientThread.invokeLater(() ->
		{
			if (client.getGameState() == GameState.LOGGED_IN)
			{
				rescan();
			}
		});
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(overlay);
		clearState();
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		GameState state = event.getGameState();
		if (state == GameState.LOGIN_SCREEN || state == GameState.HOPPING)
		{
			clearState();
		}
	}

	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned event)
	{
		GameObject object = event.getGameObject();
		int id = resolveObjectId(object.getId());
		if (CargoHoldCapacity.isCargoHold(id))
		{
			cargoHoldByWorldView.put(object.getWorldView().getId(), id);
		}
	}

	@Subscribe
	public void onGameObjectDespawned(GameObjectDespawned event)
	{
		GameObject object = event.getGameObject();
		int worldViewId = object.getWorldView().getId();
		Integer tracked = cargoHoldByWorldView.get(worldViewId);
		if (tracked != null && tracked == resolveObjectId(object.getId()))
		{
			cargoHoldByWorldView.remove(worldViewId);
		}
	}

	@Subscribe
	public void onWorldEntityDespawned(WorldEntityDespawned event)
	{
		cargoHoldByWorldView.remove(event.getWorldEntity().getWorldView().getId());
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		if (event.getVarbitId() == VarbitID.SAILING_LAST_PERSONAL_BOAT_BOARDED)
		{
			reloadHold();
		}
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		int slot = CargoHoldContainers.slotFor(event.getContainerId());
		if (slot == 0)
		{
			return;
		}
		int current = currentBoatSlot();
		if (current != 0 && slot != current)
		{
			// One of the player's other boats, for example viewed from the dock.
			return;
		}
		if (slot != monitoredSlot)
		{
			monitor.reset();
			monitoredSlot = slot;
		}
		monitor.setUsed(CargoHoldMonitor.countUsed(event.getItemContainer().getItems()));
		refreshCapacity();
		evaluate();
	}

	@Subscribe
	public void onWidgetLoaded(WidgetLoaded event)
	{
		if (event.getGroupId() == InterfaceID.SAILING_BOAT_CARGOHOLD)
		{
			// The interface script fills in the numbers after the interface opens.
			readInterfaceNextTick = true;
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (readInterfaceNextTick)
		{
			readInterfaceNextTick = false;
			readCargoInterface();
		}
		if (monitor.isReportedFullByGame() && !isSailing())
		{
			monitor.clearFullByGame();
		}
		refreshCapacity();
		evaluate();
	}

	@Subscribe
	public void onOverheadTextChanged(OverheadTextChanged event)
	{
		Actor actor = event.getActor();
		if (!(actor instanceof NPC) || !isSailing())
		{
			return;
		}
		WorldView mine = client.getLocalPlayer().getWorldView();
		WorldView theirs = actor.getWorldView();
		if (mine == null || theirs == null || mine.getId() != theirs.getId())
		{
			return;
		}
		if (mentionsFullHold(event.getOverheadText()))
		{
			holdReportedFull();
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		ChatMessageType type = event.getType();
		if (type != ChatMessageType.GAMEMESSAGE && type != ChatMessageType.SPAM
			&& type != ChatMessageType.ENGINE && type != ChatMessageType.MESBOX)
		{
			return;
		}
		if (mentionsFullHold(event.getMessage()))
		{
			holdReportedFull();
		}
	}

	/** Whether the local player is currently aboard a boat rather than on land. */
	public boolean isSailing()
	{
		Player player = client.getLocalPlayer();
		if (player == null)
		{
			return false;
		}
		WorldView view = player.getWorldView();
		return view != null && !view.isTopLevel();
	}

	public boolean hasCount()
	{
		return monitor.hasCount();
	}

	public int getUsed()
	{
		return monitor.getUsed();
	}

	public int getCapacity()
	{
		return monitor.getCapacity();
	}

	/** The banner to draw right now, or OK for none. */
	public CargoHoldMonitor.Level bannerLevel(long now)
	{
		if (!config.showBanner())
		{
			return CargoHoldMonitor.Level.OK;
		}
		CargoHoldMonitor.Level level = monitor.level(config.warnSlotsRemaining());
		int seconds = config.bannerSeconds();
		if (level != CargoHoldMonitor.Level.OK && seconds > 0 && now - alertShownAt > seconds * 1000L)
		{
			return CargoHoldMonitor.Level.OK;
		}
		return level;
	}

	private void clearState()
	{
		cargoHoldByWorldView.clear();
		Arrays.fill(capacityFromInterface, CargoHoldCapacity.UNKNOWN);
		monitor.reset();
		monitoredSlot = 0;
		alertShownAt = 0;
		readInterfaceNextTick = false;
	}

	/** Picks up what the plugin missed when it was enabled while the player was already aboard. */
	private void rescan()
	{
		Player player = client.getLocalPlayer();
		WorldView view = player == null ? null : player.getWorldView();
		if (view != null && !view.isTopLevel())
		{
			Scene scene = view.getScene();
			Tile[][][] tiles = scene == null ? null : scene.getTiles();
			if (tiles != null)
			{
				for (Tile[][] plane : tiles)
				{
					for (Tile[] row : plane)
					{
						for (Tile tile : row)
						{
							registerCargoHold(view.getId(), tile);
						}
					}
				}
			}
		}
		reloadHold();
	}

	private void registerCargoHold(int worldViewId, Tile tile)
	{
		GameObject[] objects = tile == null ? null : tile.getGameObjects();
		if (objects == null)
		{
			return;
		}
		for (GameObject object : objects)
		{
			if (object == null)
			{
				continue;
			}
			int id = resolveObjectId(object.getId());
			if (CargoHoldCapacity.isCargoHold(id))
			{
				cargoHoldByWorldView.put(worldViewId, id);
				return;
			}
		}
	}

	/** Re-reads the hold of the boat the player last boarded. */
	private void reloadHold()
	{
		int slot = currentBoatSlot();
		if (slot != monitoredSlot)
		{
			monitor.reset();
			monitoredSlot = slot;
		}
		if (slot == 0)
		{
			return;
		}
		ItemContainer container = client.getItemContainer(CargoHoldContainers.containerFor(slot));
		if (container != null)
		{
			monitor.setUsed(CargoHoldMonitor.countUsed(container.getItems()));
		}
		refreshCapacity();
		evaluate();
	}

	/** Prefers the capacity the game showed in the hold interface, then the hold object on the boat. */
	private void refreshCapacity()
	{
		int slot = monitoredSlot != 0 ? monitoredSlot : currentBoatSlot();
		int capacity = slot != 0 ? capacityFromInterface[slot] : CargoHoldCapacity.UNKNOWN;
		if (capacity == CargoHoldCapacity.UNKNOWN && isSailing())
		{
			Integer objectId = cargoHoldByWorldView.get(client.getLocalPlayer().getWorldView().getId());
			if (objectId != null)
			{
				capacity = CargoHoldCapacity.forObjectId(objectId);
			}
		}
		if (capacity != CargoHoldCapacity.UNKNOWN)
		{
			monitor.setCapacity(capacity);
		}
	}

	private void readCargoInterface()
	{
		int capacity = widgetNumber(InterfaceID.SailingBoatCargohold.CAPACITY, true);
		int used = widgetNumber(InterfaceID.SailingBoatCargohold.OCCUPIEDSLOTS, false);
		int slot = monitoredSlot != 0 ? monitoredSlot : currentBoatSlot();
		if (capacity > 0 && capacity <= CargoHoldCapacity.MAX_SLOTS && slot != 0)
		{
			capacityFromInterface[slot] = capacity;
			monitor.setCapacity(capacity);
			LOG.debug("Cargo hold interface reports capacity {} for boat {}", capacity, slot);
		}
		if (used >= 0 && used <= CargoHoldCapacity.MAX_SLOTS)
		{
			monitor.setUsed(used);
		}
		evaluate();
	}

	private int widgetNumber(int componentId, boolean last)
	{
		Widget widget = client.getWidget(componentId);
		String text = widget == null ? null : widget.getText();
		if (text == null)
		{
			return CargoHoldCapacity.UNKNOWN;
		}
		String plain = Text.removeTags(text);
		return last ? CargoHoldCapacity.lastNumber(plain) : CargoHoldCapacity.firstNumber(plain);
	}

	private void holdReportedFull()
	{
		if (!config.alertOnGameMessage())
		{
			return;
		}
		monitor.markFullByGame();
		evaluate();
	}

	private void evaluate()
	{
		long now = System.currentTimeMillis();
		CargoHoldMonitor.Level level = monitor.poll(now, config.warnSlotsRemaining(), config.repeatSeconds() * 1000L);
		if (level == null)
		{
			return;
		}
		alertShownAt = now;
		notifier.notify(config.notification(), messageFor(level));
	}

	private String messageFor(CargoHoldMonitor.Level level)
	{
		String count = monitor.hasCount() && !monitor.isReportedFullByGame()
			? " (" + monitor.getUsed() + "/" + monitor.getCapacity() + ")"
			: "";
		return level == CargoHoldMonitor.Level.FULL
			? "Your cargo hold is full" + count + "."
			: "Your cargo hold is nearly full" + count + ".";
	}

	private boolean mentionsFullHold(String text)
	{
		return text != null && Text.removeTags(text).toLowerCase().contains(HOLD_FULL_TEXT);
	}

	/** The boat slot (1 to 5) the player last boarded, or 0 if none. */
	private int currentBoatSlot()
	{
		int slot = client.getVarbitValue(VarbitID.SAILING_LAST_PERSONAL_BOAT_BOARDED);
		return slot >= 1 && slot <= CargoHoldContainers.BOAT_SLOTS ? slot : 0;
	}

	/** Follows a multi-state object to the variant currently shown so its tier can be read. */
	private int resolveObjectId(int objectId)
	{
		if (CargoHoldCapacity.isCargoHold(objectId))
		{
			return objectId;
		}
		ObjectComposition definition = client.getObjectDefinition(objectId);
		if (definition == null || definition.getImpostorIds() == null)
		{
			return objectId;
		}
		ObjectComposition shown = definition.getImpostor();
		return shown == null ? objectId : shown.getId();
	}
}
