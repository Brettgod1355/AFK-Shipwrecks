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
import net.runelite.api.Skill;
import net.runelite.api.Tile;
import net.runelite.api.WorldView;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.OverheadTextChanged;
import net.runelite.api.events.StatChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.events.WorldEntityDespawned;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.Notifier;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.config.Notification;
import net.runelite.client.config.NotificationSound;
import net.runelite.client.config.RuneLiteConfig;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.RuneScapeProfileChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Alerts when the cargo hold of the boat you are sailing fills up.
 * <p>
 * The exact count comes from the hold's item container, which the game only transmits while
 * the cargo hold interface is open. Between openings the count is kept current from crewmate
 * salvage speech, the ghost crewmate's XP drops, and the player's own deposits and withdrawals,
 * then snaps back to the real contents on the next container update. The capacity comes from
 * the cargo hold object built on the boat, refined by the numbers in the hold interface.
 */
@PluginDescriptor(
	name = "Cargo Hold Alert",
	description = "Sound and on-screen alerts when your boat's cargo hold fills up while sailing",
	tags = {"sailing", "cargo", "hold", "salvage", "boat", "notification", "alert"}
)
public class CargoFullPlugin extends Plugin
{
	private static final Logger LOG = LoggerFactory.getLogger(CargoFullPlugin.class);

	/** RuneScape-profile config key prefix for the remembered count of each boat slot. */
	private static final String USED_KEY_PREFIX = "used.";
	/** Ticks to wait for the inventory to change after a deposit or withdraw click. */
	private static final int DEPOSIT_DELTA_TICKS = 3;

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private Notifier notifier;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private ConfigManager configManager;

	@Inject
	private RuneLiteConfig runeLiteConfig;

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

	private int lastSailingXp = -1;
	/** Game tick in which the ghost crewmate last spoke, or -1. */
	private int ghostSpeechTick = -1;
	/** Ticks left to wait for the inventory to change after a deposit or withdraw click; 0 when idle. */
	private int pendingDeltaTicks;
	private int inventoryBefore;
	private boolean inventoryChanged;

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
				lastSailingXp = client.getSkillExperience(Skill.SAILING);
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
	public void onRuneScapeProfileChanged(RuneScapeProfileChanged event)
	{
		reloadHold();
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
		if (event.getContainerId() == InventoryID.INV)
		{
			if (pendingDeltaTicks > 0)
			{
				inventoryChanged = true;
			}
			return;
		}

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
		// The real contents beat any guess still waiting to be applied.
		pendingDeltaTicks = 0;
		inventoryChanged = false;
		saveUsed();
		refreshCapacity();
		evaluate();
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		String option = event.getMenuOption() == null ? "" : Text.removeTags(event.getMenuOption()).toLowerCase();
		if (!option.startsWith("deposit") && !option.startsWith("withdraw"))
		{
			return;
		}
		String target = event.getMenuTarget() == null ? "" : Text.removeTags(event.getMenuTarget()).toLowerCase();
		if (!target.contains("cargo hold") && !isCargoInterfaceOpen())
		{
			return;
		}
		// Quick deposits on the hold itself never open the interface, so the container is not
		// resent. Watch the inventory instead and apply the difference to the hold.
		pendingDeltaTicks = DEPOSIT_DELTA_TICKS;
		inventoryBefore = occupiedInventorySlots();
		inventoryChanged = false;
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
		if (pendingDeltaTicks > 0)
		{
			if (inventoryChanged)
			{
				int delta = inventoryBefore - occupiedInventorySlots();
				pendingDeltaTicks = 0;
				inventoryChanged = false;
				if (delta != 0)
				{
					cargoEstimated(delta);
				}
			}
			else
			{
				pendingDeltaTicks--;
			}
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

		String text = event.getOverheadText();
		if (CrewSpeech.reportsFullHold(text))
		{
			holdReportedFull();
		}
		else if (CrewSpeech.reportsCrewSalvage(text))
		{
			cargoEstimated(1);
		}
		else if (CrewSpeech.isGhostSpeech(text))
		{
			ghostSpeechTick = client.getTickCount();
		}
	}

	@Subscribe
	public void onStatChanged(StatChanged event)
	{
		if (event.getSkill() != Skill.SAILING)
		{
			return;
		}
		int xp = event.getXp();
		boolean gained = lastSailingXp >= 0 && xp > lastSailingXp;
		lastSailingXp = xp;
		if (gained && ghostSpeechTick == client.getTickCount())
		{
			// The ghost crewmate cannot say he salvaged something; the XP he earns you says it for him.
			ghostSpeechTick = -1;
			cargoEstimated(1);
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
		String text = event.getMessage();
		if (CrewSpeech.reportsFullHold(text))
		{
			holdReportedFull();
		}
		else if (CrewSpeech.reportsPlayerDeposit(text))
		{
			cargoEstimated(1);
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
		lastSailingXp = -1;
		ghostSpeechTick = -1;
		pendingDeltaTicks = 0;
		inventoryChanged = false;
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

	/** Re-reads the hold of the boat the player last boarded, falling back to the remembered count. */
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
			saveUsed();
		}
		else if (monitor.getUsed() == CargoHoldCapacity.UNKNOWN)
		{
			loadSavedUsed(slot);
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
			saveUsed();
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

	private boolean isCargoInterfaceOpen()
	{
		Widget root = client.getWidget(InterfaceID.SailingBoatCargohold.UNIVERSE);
		return root != null && !root.isHidden();
	}

	private int occupiedInventorySlots()
	{
		ItemContainer inventory = client.getItemContainer(InventoryID.INV);
		return inventory == null ? 0 : CargoHoldMonitor.countUsed(inventory.getItems());
	}

	private void holdReportedFull()
	{
		monitor.markFullByGame();
		saveUsed();
		evaluate();
	}

	/** Applies cargo seen going in (positive) or out (negative) while the hold is closed. */
	private void cargoEstimated(int delta)
	{
		if (!monitor.adjust(delta))
		{
			return;
		}
		saveUsed();
		evaluate();
	}

	private void saveUsed()
	{
		if (monitoredSlot == 0 || configManager.getRSProfileKey() == null)
		{
			return;
		}
		String key = USED_KEY_PREFIX + monitoredSlot;
		if (monitor.getUsed() == CargoHoldCapacity.UNKNOWN)
		{
			configManager.unsetRSProfileConfiguration(CargoFullConfig.GROUP, key);
		}
		else
		{
			configManager.setRSProfileConfiguration(CargoFullConfig.GROUP, key, monitor.getUsed());
		}
	}

	private void loadSavedUsed(int slot)
	{
		if (configManager.getRSProfileKey() == null)
		{
			return;
		}
		String saved = configManager.getRSProfileConfiguration(CargoFullConfig.GROUP, USED_KEY_PREFIX + slot);
		if (saved == null)
		{
			return;
		}
		try
		{
			monitor.setEstimatedUsed(Integer.parseInt(saved.trim()));
		}
		catch (NumberFormatException e)
		{
			LOG.debug("Ignoring unreadable remembered cargo count '{}' for boat {}", saved, slot);
		}
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
		Notification notification = config.notification();
		if (level == CargoHoldMonitor.Level.NEARLY_FULL && !config.earlyWarningSound())
		{
			notification = withoutSound(notification);
		}
		notifier.notify(notification, messageFor(level));
	}

	/**
	 * The same notification with its sound turned off. A notification the player has not customised
	 * would otherwise be replaced by RuneLite's global settings on the way out, so those are copied in
	 * first, exactly as the Notifier does, before the sound is switched off.
	 */
	private Notification withoutSound(Notification base)
	{
		Notification resolved = base;
		if (!base.isOverride() || !base.isInitialized())
		{
			resolved = new Notification()
				.withEnabled(base.isEnabled())
				.withInitialized(true)
				.withOverride(true)
				.withTray(runeLiteConfig.enableTrayNotifications())
				.withRequestFocus(runeLiteConfig.notificationRequestFocus())
				.withVolume(runeLiteConfig.notificationVolume())
				.withTimeout(runeLiteConfig.notificationTimeout())
				.withGameMessage(runeLiteConfig.enableGameMessageNotification())
				.withFlash(runeLiteConfig.flashNotification())
				.withFlashColor(runeLiteConfig.notificationFlashColor())
				.withSendWhenFocused(runeLiteConfig.sendNotificationsWhenFocused());
		}
		return resolved.withSound(NotificationSound.OFF);
	}

	private String messageFor(CargoHoldMonitor.Level level)
	{
		String count = "";
		if (monitor.hasCount() && !monitor.isReportedFullByGame())
		{
			count = " (" + monitor.getUsed() + "/" + monitor.getCapacity() + ")";
		}
		return level == CargoHoldMonitor.Level.FULL
			? "Your cargo hold is full" + count + "."
			: "Your cargo hold is nearly full" + count + ".";
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
