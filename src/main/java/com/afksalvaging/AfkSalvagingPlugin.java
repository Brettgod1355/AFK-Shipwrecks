/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

import com.google.inject.Provides;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import javax.inject.Inject;
import net.runelite.api.Actor;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.Item;
import net.runelite.api.ItemComposition;
import net.runelite.api.KeyCode;
import net.runelite.api.Menu;
import net.runelite.api.ItemContainer;
import net.runelite.api.MenuAction;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.ObjectComposition;
import net.runelite.api.Player;
import net.runelite.api.Scene;
import net.runelite.api.Skill;
import net.runelite.api.Tile;
import net.runelite.api.WorldEntity;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.OverheadTextChanged;
import net.runelite.api.events.StatChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.events.WorldEntityDespawned;
import net.runelite.api.events.WorldEntitySpawned;
import net.runelite.api.events.WorldViewLoaded;
import net.runelite.api.events.WorldViewUnloaded;
import net.runelite.api.gameval.DBTableID;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.Notifier;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.config.Notification;
import net.runelite.client.config.NotificationSound;
import net.runelite.client.config.RuneLiteConfig;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.RuneScapeProfileChanged;
import net.runelite.client.events.WorldsFetch;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.WorldService;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.Text;
import net.runelite.http.api.item.ItemPrice;
import net.runelite.http.api.worlds.World;
import net.runelite.http.api.worlds.WorldResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A countdown to a full cargo hold while your crew salvage a shipwreck, plus the alerts that make
 * salvaging AFK-able: the hold is full, a hook is standing empty, the crew have stopped.
 * <p>
 * This class only turns client events into calls on {@link AfkSession} and shows what the session
 * decides. The hold count itself comes from the hold's item container while the hold interface is
 * open and is kept current in between from crew speech, Sailing XP drops and the player's own
 * deposits and withdrawals; the capacity from the cargo hold object built on the boat.
 */
@PluginDescriptor(
	name = "AFK Salvaging",
	description = "Countdown to a full cargo hold while your crew salvage, with alerts for a full hold, an empty hook and stopped crew",
	tags = {
		"sailing", "sail", "sailor", "boat", "ship", "raft", "skiff", "sloop", "sea", "ocean", "voyage",
		"cargo", "cargo hold", "hold", "capacity", "full", "salvage", "salvaging", "shipwreck", "wreck",
		"crew", "crewmate", "deckhand", "hook", "salvaging hook", "timer", "eta", "countdown", "estimate",
		"alert", "notification", "notify", "reminder", "warning", "sound", "overlay", "counter", "banner",
		"afk", "idle"
	}
)
public class AfkSalvagingPlugin extends Plugin
{
	private static final Logger LOG = LoggerFactory.getLogger(AfkSalvagingPlugin.class);

	/** RuneScape-profile config key prefix for the remembered count of each boat slot. */
	private static final String USED_KEY_PREFIX = "used.";
	/** RuneScape-profile config key prefix for the learned rate correction of each wreck. */
	private static final String RATE_KEY_PREFIX = "rate.";
	/** How often the learned rate is written out while salvaging. */
	private static final long MEMORY_FLUSH_MILLIS = 5 * 60_000L;
	/** RuneScape-profile config key for the spots pinned in the sidebar. */
	private static final String FAVOURITES_KEY = "spots.favourites";
	/** Ticks between updates of the sidebar's distances and session line. */
	private static final int SIDEBAR_REFRESH_TICKS = 4;
	/** Ticks after login before boarding counts as boarding: logging in aboard is not. */
	private static final int BOARDING_SETTLE_TICKS = 3;
	/** How long the banner shows for a test alert when the banner setting keeps it up indefinitely. */
	private static final int TEST_BANNER_SECONDS = 10;
	/** Ticks between fallback scans of the boat while no hook has been found. */
	private static final int RESCAN_TICKS = 5;
	private static final int[] CREW_SLOT_VARBITS = {
		VarbitID.SAILING_CREW_SLOT_1, VarbitID.SAILING_CREW_SLOT_2, VarbitID.SAILING_CREW_SLOT_3,
		VarbitID.SAILING_CREW_SLOT_4, VarbitID.SAILING_CREW_SLOT_5
	};
	private static final int[] CREW_POSITION_VARBITS = {
		VarbitID.SAILING_CREW_SLOT_1_POSITION, VarbitID.SAILING_CREW_SLOT_2_POSITION, VarbitID.SAILING_CREW_SLOT_3_POSITION,
		VarbitID.SAILING_CREW_SLOT_4_POSITION, VarbitID.SAILING_CREW_SLOT_5_POSITION
	};
	/**
	 * World entity config ids of the player's own boats: raft, skiff, sloop. Literals because the
	 * 1.13.1 gameval classes have no constants for world entity configs; the values were read from
	 * the client in game (WorldEntity.getConfig().getId()).
	 */
	private static final int ENTITY_RAFT = 1;
	private static final int ENTITY_SKIFF = 2;
	private static final int ENTITY_SLOOP = 3;

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
	private WorldService worldService;

	@Inject
	private ScheduledExecutorService executor;

	@Inject
	private AfkSalvagingOverlay overlay;

	@Inject
	private AfkSalvagingConfig config;

	@Inject
	private EventBus eventBus;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private InfoBoxManager infoBoxManager;

	@Inject
	private ItemManager itemManager;

	@Inject
	private InventorySortOverlay sortOverlay;

	/** The player's keep / hold / alch / drop marks, from the settings. */
	private final SalvageSorter.Lists sortLists = new SalvageSorter.Lists();
	/** What the cargo hold accepts: the wiki's list, corrected by what the game says while it is open. */
	private final HoldWhitelist holdWhitelist = new HoldWhitelist();
	/** The box decided for each item id, cleared when the marks or the thresholds change and now and then for prices. */
	private final Map<Integer, SortRule> sortCache = new HashMap<>();
	/** Ticks between clearing the sort cache so Grand Exchange price changes show up. */
	private static final int SORT_CACHE_TICKS = 500;
	/** The one entry the marking choices sit under in an item's right-click menu. */
	private static final String SORT_MENU = "AFK Salvaging";
	/** Whether the inventory boxes are drawn, decided once per tick rather than per item per frame. */
	private boolean sortActive;

	private HoldInfoBox infoBox;

	@Inject
	private PluginManager pluginManager;

	@Inject
	private SalvageBoxOverlay boxOverlay;

	/** The spot last sent to the map or routed to; the sidebar highlights it. */
	private SalvagingSpot pickedSpot;
	/** The route to a salvaging spot that Shortest Path is drawing for us, or null. */
	private SpotRoute spotRoute;

	/** The sidebar entry and its panel, present while the setting is on. */
	private NavigationButton spotsButton;
	private SalvagingSpotPanel spotsPanel;
	/** A spot to centre the world map on once it has opened, or null. */
	private WorldPoint pendingMapTarget;
	/** Ticks to wait after the world map opens before it will take a position. */
	private int mapFocusTicks;

	private final AfkSession session = new AfkSession(new ProfileMemory());
	/** Cargo hold object id seen in each boat's world view. */
	private final Map<Integer, Integer> cargoHoldByWorldView = new HashMap<>();
	/** Capacity read from the cargo hold interface, indexed by boat slot 1 to 5. */
	private final int[] capacityFromInterface = new int[CargoHoldContainers.BOAT_SLOTS + 1];
	/** Hook objects on the followed boat, by object hash. */
	private final Map<Long, GameObject> hookObjects = new HashMap<>();
	private final Map<Integer, Crewmate> crewCache = new HashMap<>();
	/** Boat slot the monitor currently describes, or 0 when unknown. */
	private int monitoredSlot;
	private long alertShownAt;
	private long reminderShownAt;
	private boolean readInterfaceNextTick;
	private int lastSailingXp = -1;
	private volatile WorldResult worldResult;
	/** The one-off world list read on the executor, kept so shutDown can cancel it. */
	private Future<?> worldRequest;
	private volatile boolean worldDirty = true;
	private int worldChecked = -1;
	private boolean salvagingWorld;
	private long lastMemoryFlushAt;
	/** Whether the next LOGGED_IN is a real login rather than a scene reload. */
	private boolean loginPending = true;
	/** Whether the full-hold shout for someone else's boat has been given this trip. */
	private boolean otherBoatFullNotified;
	/** Last tick's footing, to notice the player stepping from a dock onto their own boat. */
	private boolean lastSailing;
	private boolean lastOwnBoat;
	private int ticksSinceLogin;
	private long testAlertAt;
	/** The update line owed to the player, said on the first tick they are logged in. */
	private String updateMessage;

	@Provides
	AfkSalvagingConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(AfkSalvagingConfig.class);
	}

	@Override
	protected void startUp()
	{
		clearState();
		noteVersion();
		overlayManager.add(overlay);
		overlayManager.add(boxOverlay);
		overlayManager.add(sortOverlay);
		reloadSortLists();
		applySpotSettings();
		applyInfoBox();
		clientThread.invokeLater(() ->
		{
			if (client.getGameState() == GameState.LOGGED_IN)
			{
				loggedIn();
			}
		});
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(overlay);
		overlayManager.remove(boxOverlay);
		overlayManager.remove(sortOverlay);
		removeInfoBox();
		if (worldRequest != null)
		{
			worldRequest.cancel(false);
			worldRequest = null;
		}
		removeSidebar();
		pendingMapTarget = null;
		spotRoute = null;
		session.flushMemory();
		clearState();
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (AfkSalvagingConfig.GROUP.equals(event.getGroup()))
		{
			applySpotSettings();
			applyInfoBox();
			reloadSortLists();
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		switch (event.getGameState())
		{
			case LOGIN_SCREEN:
				clearState();
				break;
			case HOPPING:
				session.worldHopped();
				forgetBoat();
				loginPending = true;
				break;
			case CONNECTION_LOST:
				loginPending = true;
				break;
			case LOADING:
				session.sceneReloading(clock());
				break;
			case LOGGED_IN:
				// The game reports LOGGED_IN after every scene load, not only at login.
				if (loginPending)
				{
					loginPending = false;
					worldDirty = true;
					clientThread.invokeLater(this::loggedIn);
				}
				break;
			default:
				break;
		}
	}

	@Subscribe
	public void onWorldsFetch(WorldsFetch event)
	{
		worldResult = event.getWorldResult();
		worldDirty = true;
	}

	@Subscribe
	public void onRuneScapeProfileChanged(RuneScapeProfileChanged event)
	{
		session.flushMemory();
		reloadHold();
		if (spotsPanel != null)
		{
			spotsPanel.setFavourites(loadFavourites());
		}
	}

	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned event)
	{
		objectSeen(event.getGameObject());
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
		session.boat().remove(worldViewId, object.getHash());
		if (worldViewId == session.boat().getWorldViewId())
		{
			// Object hashes carry no world view, so the same hook on another boat must not evict ours.
			hookObjects.remove(object.getHash());
		}
		if (object.getWorldView().isTopLevel())
		{
			session.wreckGone(object.getWorldLocation(), object.getId(), clock());
		}
	}

	@Subscribe
	public void onWorldEntitySpawned(WorldEntitySpawned event)
	{
		WorldView view = event.getWorldEntity().getWorldView();
		Player player = client.getLocalPlayer();
		if (view != null && player != null && player.getWorldView() != null && player.getWorldView().getId() == view.getId())
		{
			followBoat(view);
		}
	}

	@Subscribe
	public void onWorldEntityDespawned(WorldEntityDespawned event)
	{
		int worldViewId = event.getWorldEntity().getWorldView().getId();
		cargoHoldByWorldView.remove(worldViewId);
		if (session.boat().getWorldViewId() == worldViewId)
		{
			session.boat().clear();
			hookObjects.clear();
		}
	}

	@Subscribe
	public void onWorldViewLoaded(WorldViewLoaded event)
	{
		WorldView view = event.getWorldView();
		if (view.isTopLevel())
		{
			scanWrecks(view);
			return;
		}
		Player player = client.getLocalPlayer();
		if (player != null && player.getWorldView() != null && player.getWorldView().getId() == view.getId())
		{
			followBoat(view);
		}
	}

	@Subscribe
	public void onWorldViewUnloaded(WorldViewUnloaded event)
	{
		WorldView view = event.getWorldView();
		if (view.isTopLevel())
		{
			session.sceneReloading(clock());
		}
		else if (session.boat().getWorldViewId() == view.getId())
		{
			session.boat().clear();
			hookObjects.clear();
		}
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		int id = event.getVarbitId();
		if (id == VarbitID.SAILING_LAST_PERSONAL_BOAT_BOARDED)
		{
			reloadHold();
			return;
		}
		for (int slot = 0; slot < CrewRoster.SLOTS; slot++)
		{
			if (id == CREW_SLOT_VARBITS[slot] || id == CREW_POSITION_VARBITS[slot])
			{
				loadCrewSlot(slot);
				return;
			}
		}
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		if (event.getContainerId() == InventoryID.INV)
		{
			inventoryChanged(event.getItemContainer());
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
			session.monitor().reset();
			monitoredSlot = slot;
		}
		session.holdCount(CargoHoldMonitor.countUsed(event.getItemContainer().getItems()), client.getTickCount());
		saveUsed();
		refreshCapacity();
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		String option = event.getMenuOption() == null ? "" : Text.removeTags(event.getMenuOption()).toLowerCase();
		String target = event.getMenuTarget() == null ? "" : Text.removeTags(event.getMenuTarget()).toLowerCase();
		MenuAction action = event.getMenuAction();
		if (action == MenuAction.CANCEL || option.startsWith("examine"))
		{
			return;
		}
		if (option.equals("drop") && event.getItemId() > 0 && sortRule(event.getItemId()) == SortRule.DROP)
		{
			session.stats().dropped++;
		}
		else if (option.startsWith("cast") && target.contains("high level alchemy"))
		{
			session.stats().alched++;
		}
		boolean objectOp = action == MenuAction.GAME_OBJECT_FIRST_OPTION || action == MenuAction.GAME_OBJECT_SECOND_OPTION
			|| action == MenuAction.GAME_OBJECT_THIRD_OPTION || action == MenuAction.GAME_OBJECT_FOURTH_OPTION
			|| action == MenuAction.GAME_OBJECT_FIFTH_OPTION;
		if (objectOp && (target.contains("crystal extractor") || BoatFacilities.isExtractor(event.getId())))
		{
			session.extractorUsed(client.getTickCount());
			return;
		}
		boolean holdAction = option.startsWith("deposit") || option.startsWith("withdraw");
		if (holdAction && (target.contains("cargo hold") || isCargoInterfaceOpen()))
		{
			// Quick deposits on the hold itself never open the interface, so the container is not
			// resent. The session watches the inventory instead and applies the difference to the hold.
			session.holdActionClicked(option.startsWith("withdraw"));
			return;
		}
		// Anything else the player does means they are not walking to the hold any more.
		session.holdActionCancelled();
	}

	@Subscribe
	public void onWidgetLoaded(WidgetLoaded event)
	{
		if (event.getGroupId() == InterfaceID.SAILING_BOAT_CARGOHOLD)
		{
			// The interface script fills in the numbers after the interface opens.
			readInterfaceNextTick = true;
		}
		else if (event.getGroupId() == InterfaceID.WORLDMAP && pendingMapTarget != null)
		{
			// The map takes a position only once its own script has laid it out, a tick or two later.
			mapFocusTicks = 2;
		}
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
		int tick = client.getTickCount();
		if (CrewSpeech.reportsFullHold(text))
		{
			holdReportedFull();
		}
		else if (CrewSpeech.reportsCrewSalvage(text))
		{
			String name = actor.getName() == null ? "" : Text.removeTags(actor.getName());
			session.crewLine(name, tick);
		}
		else if (CrewSpeech.isGhostSpeech(text))
		{
			session.ghostLine(tick);
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
		int delta = lastSailingXp >= 0 ? xp - lastSailingXp : 0;
		lastSailingXp = xp;
		if (delta > 0)
		{
			session.sailingXp(delta, client.getTickCount());
		}
		shareSailingLevel();
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
			session.depositLine();
			saveUsed();
		}
		else if (CrewSpeech.reportsHazardousWaters(text))
		{
			session.hazardLine(playerTopLevelPoint());
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		long now = clock();
		int tick = client.getTickCount();
		if (readInterfaceNextTick)
		{
			readInterfaceNextTick = false;
			readCargoInterface();
		}
		learnHoldWhitelist();
		if (mapFocusTicks > 0 && --mapFocusTicks == 0 && pendingMapTarget != null)
		{
			WorldPoint target = pendingMapTarget;
			pendingMapTarget = null;
			if (isWorldMapOpen())
			{
				client.getWorldMap().setWorldMapPositionTarget(target);
			}
		}
		refreshWorld();
		refreshCapacity();

		Player player = client.getLocalPlayer();
		boolean sailing = isSailing();
		WorldEntity boatEntity = sailing ? boatEntity() : null;
		if (sailing && player != null && session.boat().getWorldViewId() != player.getWorldView().getId())
		{
			followBoat(player.getWorldView());
		}
		else if (sailing && player != null && tick % RESCAN_TICKS == 0 && hooksMissingObjects())
		{
			scanBoat(player.getWorldView());
		}

		if (!sailing)
		{
			otherBoatFullNotified = false;
		}
		AfkSession.Settings settings = session.settings();
		settings.countHookedSalvage = config.countHookedSalvage();
		settings.warnSlotsRemaining = config.warnSlotsRemaining();
		settings.repeatFullMillis = config.repeatSeconds() * 1000L;
		settings.graceMillis = config.reminderGraceSeconds() * 1000L;
		settings.repeatReminderMillis = config.reminderRepeatSeconds() * 1000L;
		settings.salvagingWorld = salvagingWorld;
		settings.idleWarnMillis = config.idleLogoutWarnSeconds() * 1000L;

		AfkSession.TickInputs in = new AfkSession.TickInputs();
		in.tick = tick;
		in.now = now;
		in.sailing = sailing;
		in.ownBoat = boatEntity != null && boatEntity.getOwnerType() == WorldEntity.OWNER_TYPE_SELF_PLAYER;
		in.boatCrewCapacity = crewCapacity(boatEntity);
		in.animation = player == null ? -1 : player.getAnimation();
		in.boostedSailingLevel = client.getBoostedSkillLevel(Skill.SAILING);
		in.hooks = hookInputs(boatEntity);
		in.boatPoint = boatEntity == null ? null : topLevelPoint(boatEntity);
		in.playerPoint = playerTopLevelPoint();
		in.cargoInterfaceOpen = isCargoInterfaceOpen();
		in.idleLogoutMillis = AfkSession.idleLogoutMillis(client.getIdleTimeout(), client.getMouseIdleTicks(), client.getKeyboardIdleTicks());

		List<AfkSession.Notice> notices = session.tick(in);
		for (AfkSession.Notice notice : notices)
		{
			give(notice, now);
		}
		if (updateMessage != null)
		{
			if (config.updateMessage())
			{
				client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "<col=" + tipColour() + ">" + updateMessage + "</col>", null);
			}
			updateMessage = null;
		}
		if (in.ownBoat && !lastOwnBoat && !lastSailing && ticksSinceLogin > BOARDING_SETTLE_TICKS)
		{
			boarded();
		}
		watchSpotRoute(in);
		lastOwnBoat = in.ownBoat;
		lastSailing = sailing;
		ticksSinceLogin++;
		if (tick % SORT_CACHE_TICKS == 0)
		{
			sortCache.clear();
		}
		sortActive = decideSortActive();
		if (spotsPanel != null && tick % SIDEBAR_REFRESH_TICKS == 0)
		{
			spotsPanel.setPosition(in.playerPoint);
			Mooring nearest = Mooring.nearest(in.playerPoint);
			Mooring usable = config.dockRequirements()
				? Mooring.nearestUsable(in.playerPoint, client.getRealSkillLevel(Skill.SAILING), quest -> quest.getState(client))
				: nearest;
			spotsPanel.setDock(usable, nearest);
			spotsPanel.setStats(statsLine());
		}
		if (session.monitor().isEstimate())
		{
			saveUsed();
		}
		if (now - lastMemoryFlushAt > MEMORY_FLUSH_MILLIS)
		{
			lastMemoryFlushAt = now;
			session.flushMemory();
		}
	}

	// ---- What the overlay needs ----

	public AfkSession getSession()
	{
		return session;
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

	public boolean isSalvagingWorld()
	{
		return salvagingWorld;
	}

	/** Where our own boat's hooks are in the top-level world right now; empty ashore or on another boat. */
	public List<WorldPoint> hookPoints()
	{
		List<WorldPoint> points = new ArrayList<>();
		WorldEntity boat = boatEntity();
		if (boat == null || boat.getOwnerType() != WorldEntity.OWNER_TYPE_SELF_PLAYER)
		{
			return points;
		}
		for (BoatFacilities.Hook hook : session.boat().hooks())
		{
			GameObject object = hookObjects.get(hook.getHash());
			WorldPoint point = object == null ? null : topLevelPoint(boat, object.getLocalLocation());
			if (point != null)
			{
				points.add(point);
			}
		}
		return points;
	}

	/** Whether the player is aboard a boat that is their own. */
	public boolean isOwnBoat()
	{
		WorldEntity boat = boatEntity();
		return boat != null && boat.getOwnerType() == WorldEntity.OWNER_TYPE_SELF_PLAYER;
	}

	/** The salvaging worlds to name in the tip: the player's list, or the default if they emptied it. */
	public String salvagingWorldsText()
	{
		String configured = config.salvagingWorlds() == null ? "" : config.salvagingWorlds().trim();
		return configured.isEmpty() ? SalvagingWorlds.DEFAULT_WORLDS : configured;
	}

	/** The hold banner to draw right now, or OK for none. */
	public CargoHoldMonitor.Level bannerLevel(long now)
	{
		if (!config.showBanner())
		{
			return CargoHoldMonitor.Level.OK;
		}
		CargoHoldMonitor.Level level = session.monitor().level(config.warnSlotsRemaining());
		int seconds = config.bannerSeconds();
		if (level == CargoHoldMonitor.Level.OK && testAlertAt > 0
			&& now - testAlertAt < (seconds > 0 ? seconds : TEST_BANNER_SECONDS) * 1000L)
		{
			return CargoHoldMonitor.Level.FULL;
		}
		if (level != CargoHoldMonitor.Level.OK && seconds > 0 && now - alertShownAt > seconds * 1000L)
		{
			return CargoHoldMonitor.Level.OK;
		}
		return level;
	}

	/** Why a reminder banner should be drawn right now, or null. */
	public HookWatch.Reason reminderBanner(long now)
	{
		if (!config.showBanner())
		{
			return null;
		}
		HookWatch.Reason reason = session.view().reminder;
		int seconds = config.bannerSeconds();
		if (reason != null && seconds > 0 && now - reminderShownAt > seconds * 1000L)
		{
			return null;
		}
		return reason;
	}

	/** A steady clock for durations, unaffected by the wall clock being adjusted. */
	public static long clock()
	{
		return System.nanoTime() / 1_000_000L;
	}

	// ---- Internals ----

	private void loggedIn()
	{
		// The stat packets may not have arrived yet; a zero would turn the first drop into a huge delta.
		int xp = client.getSkillExperience(Skill.SAILING);
		lastSailingXp = xp > 0 ? xp : -1;
		inventoryChanged(client.getItemContainer(InventoryID.INV));
		Player player = client.getLocalPlayer();
		if (player != null && player.getWorldView() != null && !player.getWorldView().isTopLevel())
		{
			followBoat(player.getWorldView());
		}
		WorldView top = client.getTopLevelWorldView();
		if (top != null)
		{
			scanWrecks(top);
		}
		for (int slot = 0; slot < CrewRoster.SLOTS; slot++)
		{
			loadCrewSlot(slot);
		}
		reloadHold();
		requestWorlds();
		shareSailingLevel();
		if (spotsPanel != null)
		{
			spotsPanel.setFavourites(loadFavourites());
		}
	}

	/**
	 * Works out whether this is the first run of a new version and, if so, what to say once the
	 * player is logged in. Reads the settings before anything in this run writes to them, so a
	 * fresh install (nothing of the plugin stored anywhere) is told from an update from 1.0,
	 * which stored settings and remembered counts under the same group but no version.
	 */
	private void noteVersion()
	{
		String last = config.lastVersion();
		boolean installedBefore = !configManager.getConfigurationKeys(AfkSalvagingConfig.GROUP + ".").isEmpty()
			|| configManager.getConfigurationKeys(ConfigManager.RSPROFILE_GROUP + ".").stream()
				.anyMatch(key -> key.contains("." + AfkSalvagingConfig.GROUP + "."));
		updateMessage = WhatsNew.message(last, installedBefore);
		if (!WhatsNew.VERSION.equals(last))
		{
			configManager.setConfiguration(AfkSalvagingConfig.GROUP, "lastVersion", WhatsNew.VERSION);
		}
	}

	// ---- Inventory sorting: boxes round items, marks, the sidebar lists ----

	/** Whether the inventory boxes are drawn right now, as decided on the last game tick. */
	public boolean inventorySortActive()
	{
		return sortActive;
	}

	/** The "Show boxes" setting applied to where the player is. */
	private boolean decideSortActive()
	{
		if (!config.inventorySort() || client.getGameState() != GameState.LOGGED_IN)
		{
			return false;
		}
		switch (config.sortWhere())
		{
			case EVERYWHERE:
				return true;
			case ABOARD:
				return isOwnBoat();
			default:
				AfkSession.View view = session.view();
				return isOwnBoat() && (view.wrecksUp > 0 || view.higherWrecksUp > 0 || !session.wrecks().presentSites().isEmpty());
		}
	}

	/** The box for an item, or null for one the boxes never apply to. Client thread. */
	public SortRule sortRule(int itemId)
	{
		if (SalvageSorter.excluded(itemId))
		{
			return null;
		}
		// Null is a real answer here (no box), so look it up by key.
		if (sortCache.containsKey(itemId))
		{
			return sortCache.get(itemId);
		}
		SortRule rule = SalvageSorter.rule(itemFacts(itemId), sortLists, config.alchThreshold(), config.geOverAlchPercent());
		sortCache.put(itemId, rule);
		return rule;
	}

	private SalvageSorter.ItemFacts itemFacts(int itemId)
	{
		ItemComposition item = itemManager.getItemComposition(itemId);
		// A noted item is worth what the real one is worth; the hold refuses it either way.
		int real = itemManager.canonicalize(itemId);
		ItemComposition realItem = real == itemId ? item : itemManager.getItemComposition(real);
		boolean noted = item.getNote() != -1;
		return new SalvageSorter.ItemFacts(itemId, noted, realItem.isTradeable(), holdWhitelist.takes(real),
			HoldWhitelist.isCannonball(real), realItem.getHaPrice(), itemManager.getItemPrice(real));
	}

	/**
	 * Shift-right-click on an inventory item offers one "AFK Salvaging" entry that opens to Keep /
	 * Deposit / Alch / Drop and Unmark, so the item's own menu grows by one line rather than four.
	 * These are RuneLite-side entries: clicking one changes a setting and sends nothing to the game.
	 */
	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		if (!config.sortMenu() || event.getActionParam1() != InterfaceID.Inventory.ITEMS
			|| !client.isKeyPressed(KeyCode.KC_SHIFT) || !"Examine".equals(event.getOption()))
		{
			return;
		}
		// Only on your own boat (owner, 2026-10-02), unless the boxes are set to show everywhere.
		if (!isOwnBoat() && config.sortWhere() != SortWhere.EVERYWHERE)
		{
			return;
		}
		int itemId = event.getItemId();
		if (itemId <= 0 || SalvageSorter.excluded(itemId))
		{
			return;
		}
		Menu marks = client.getMenu().createMenuEntry(-1)
			.setOption(SORT_MENU)
			.setTarget(event.getTarget())
			.setType(MenuAction.RUNELITE)
			.setItemId(itemId)
			.createSubMenu();
		SortRule marked = sortLists.markOf(itemId);
		// A submenu lists its entries in the order they are made, as Inventory Tags does.
		for (SortRule rule : SortRule.values())
		{
			if (rule == marked)
			{
				continue;
			}
			marks.createMenuEntry(0)
				.setOption(rule.getLabel())
				.setType(MenuAction.RUNELITE)
				.onClick(e -> markItem(itemId, rule));
		}
		if (marked != null)
		{
			marks.createMenuEntry(0)
				.setOption("Unmark")
				.setType(MenuAction.RUNELITE)
				.onClick(e -> markItem(itemId, null));
		}
	}

	/** Puts an item in one list (null: none), saves, and refreshes the boxes and the sidebar. Client thread. */
	private void markItem(int itemId, SortRule rule)
	{
		sortLists.mark(itemId, rule);
		for (SortRule each : SortRule.values())
		{
			String key = sortListKey(each);
			String encoded = sortLists.encode(each);
			String stored = configManager.getConfiguration(AfkSalvagingConfig.GROUP, key);
			if (encoded.equals(stored == null ? "" : stored))
			{
				// Unchanged: writing it would only fire another settings refresh.
				continue;
			}
			if (encoded.isEmpty())
			{
				configManager.unsetConfiguration(AfkSalvagingConfig.GROUP, key);
			}
			else
			{
				configManager.setConfiguration(AfkSalvagingConfig.GROUP, key, encoded);
			}
		}
		sortCache.clear();
		String name = itemManager.getItemComposition(itemId).getName();
		setSpotStatus(rule == null ? name + " unmarked; it is sorted by what it is again." : name + " marked: " + rule.getLabel() + ".", false);
		pushSortLists();
	}

	private static String sortListKey(SortRule rule)
	{
		switch (rule)
		{
			case KEEP:
				return "sortKeepIds";
			case HOLD:
				return "sortHoldIds";
			case ALCH:
				return "sortAlchIds";
			default:
				return "sortDropIds";
		}
	}

	/** Reads the four lists and what the hold accepts from the settings, and forgets the decided boxes. */
	private void reloadSortLists()
	{
		sortLists.decode(SortRule.KEEP, config.sortKeepIds());
		sortLists.decode(SortRule.HOLD, config.sortHoldIds());
		sortLists.decode(SortRule.ALCH, config.sortAlchIds());
		sortLists.decode(SortRule.DROP, config.sortDropIds());
		holdWhitelist.decode(config.holdAcceptedIds(), config.holdRefusedIds());
		sortCache.clear();
		pushSortLists();
	}

	/**
	 * While the cargo hold is open, the game marks each inventory slot it will accept in a varp
	 * (its own side panel greys out the rest). Learn from it, by unnoted item id. Client thread.
	 */
	private void learnHoldWhitelist()
	{
		if (!isCargoInterfaceOpen())
		{
			holdWhitelist.resetObservation();
			return;
		}
		ItemContainer inventory = client.getItemContainer(InventoryID.INV);
		if (inventory == null)
		{
			return;
		}
		Item[] items = inventory.getItems();
		int[] slots = new int[items.length];
		for (int slot = 0; slot < items.length; slot++)
		{
			Item item = items[slot];
			boolean present = item != null && item.getId() > 0 && item.getQuantity() > 0;
			// A noted item is refused whatever it is, so it says nothing about the item itself.
			slots[slot] = present && itemManager.getItemComposition(item.getId()).getNote() == -1 ? item.getId() : -1;
		}
		int bits = client.getVarpValue(VarPlayerID.SAILING_BOAT_CARGOHOLD_SIDE_WHITELIST);
		CargoHoldMonitor monitor = session.monitor();
		boolean hasRoom = monitor.hasCount() && monitor.getUsed() < monitor.getCapacity();
		if (holdWhitelist.observe(slots, bits, hasRoom))
		{
			// Encode both first: saving one fires a settings reload that reads the other back.
			String accepted = holdWhitelist.encodeAccepted();
			String refused = holdWhitelist.encodeRefused();
			configManager.setConfiguration(AfkSalvagingConfig.GROUP, "holdRefusedIds", refused);
			configManager.setConfiguration(AfkSalvagingConfig.GROUP, "holdAcceptedIds", accepted);
			sortCache.clear();
		}
	}

	/** Sends the lists, with item names, to the sidebar. Names need the client thread. */
	private void pushSortLists()
	{
		if (spotsPanel == null)
		{
			return;
		}
		clientThread.invokeLater(() ->
		{
			Map<SortRule, List<String[]>> named = new EnumMap<>(SortRule.class);
			for (SortRule rule : SortRule.values())
			{
				List<String[]> rows = new ArrayList<>();
				for (int id : sortLists.ids(rule))
				{
					rows.add(new String[]{String.valueOf(id), itemManager.getItemComposition(id).getName()});
				}
				named.put(rule, rows);
			}
			SalvagingSpotPanel.SortDefaults defaults = new SalvagingSpotPanel.SortDefaults(
				config.alchThreshold(), config.geOverAlchPercent());
			if (spotsPanel != null)
			{
				spotsPanel.setSortLists(named, defaults);
			}
		});
	}

	/**
	 * Adds an item typed into the sidebar. The search covers tradeable items by name; anything
	 * else is marked by shift-right-clicking it in the inventory.
	 */
	private void addSortItemByName(String typed, SortRule rule)
	{
		String name = typed == null ? "" : typed.trim();
		if (name.isEmpty())
		{
			return;
		}
		clientThread.invokeLater(() ->
		{
			List<ItemPrice> found = itemManager.search(name);
			ItemPrice pick = null;
			for (ItemPrice candidate : found)
			{
				if (candidate.getName().equalsIgnoreCase(name))
				{
					pick = candidate;
					break;
				}
			}
			if (pick == null && found.size() == 1)
			{
				pick = found.get(0);
			}
			if (pick == null)
			{
				if (found.isEmpty())
				{
					setSpotStatus("No tradeable item is called \"" + name + "\". Shift-right-click it in your inventory to mark it instead.", true);
				}
				else
				{
					StringBuilder names = new StringBuilder();
					for (int i = 0; i < Math.min(5, found.size()); i++)
					{
						names.append(i == 0 ? "" : ", ").append(found.get(i).getName());
					}
					setSpotStatus("Several items match \"" + name + "\": " + names + (found.size() > 5 ? ", ..." : "") + ". Type the full name.", true);
				}
				return;
			}
			markItem(pick.getId(), rule);
		});
	}

	/** Adds or removes the countdown infobox to match the setting. */
	private void applyInfoBox()
	{
		if (config.showInfoBox())
		{
			if (infoBox == null)
			{
				infoBox = new HoldInfoBox(ImageUtil.loadImageResource(getClass(), "infobox_icon.png"), this,
					session::view, () -> session.monitor().level(config.warnSlotsRemaining()));
				infoBoxManager.addInfoBox(infoBox);
			}
		}
		else
		{
			removeInfoBox();
		}
	}

	private void removeInfoBox()
	{
		if (infoBox != null)
		{
			infoBoxManager.removeInfoBox(infoBox);
			infoBox = null;
		}
	}

	// ---- Salvage spots: sidebar, double spot boxes ----

	/** The plugin's chat line colour as the game's tag wants it: six hex digits, no transparency. */
	private String tipColour()
	{
		return Palette.CHAT_TIP;
	}

	/** The sorting colours from the settings, for the sidebar's list headings. */
	private void applyPalette()
	{
		Map<SortRule, Color> sorting = new EnumMap<>(SortRule.class);
		sorting.put(SortRule.KEEP, config.keepColor());
		sorting.put(SortRule.HOLD, config.holdColor());
		sorting.put(SortRule.ALCH, config.alchColor());
		sorting.put(SortRule.DROP, config.dropColor());
		spotsPanel.setSortColours(sorting);
	}

	/** Brings the sidebar in line with the settings. */
	private void applySpotSettings()
	{
		if (config.spotSidebar())
		{
			addSidebar();
			applyPalette();
			spotsPanel.setDockShown(config.nearestDock());
			spotsPanel.setAutoRoute(SpotList.spotNamed(config.autoRouteSpot()));
		}
		else
		{
			removeSidebar();
		}
	}

	private void addSidebar()
	{
		if (spotsButton != null)
		{
			return;
		}
		spotsPanel = new SalvagingSpotPanel(new SalvagingSpotPanel.Actions()
		{
			@Override
			public void showOnMap(SalvagingSpot spot)
			{
				showSpotOnMap(spot);
			}

			@Override
			public void routeTo(SalvagingSpot spot)
			{
				routeToSpot(spot);
			}

			@Override
			public void clearRoute()
			{
				clearSpotRoute();
			}

			@Override
			public void showDockOnMap(Mooring dock)
			{
				showPointOnMap(dock.getPoint(), dock.getDisplayName() + " dock");
			}

			@Override
			public void routeToDock(Mooring dock)
			{
				route(dock.getPoint(), dock.getDisplayName() + " dock", "Route sent to Shortest Path: ", null);
			}

			@Override
			public void filterChanged(String filterKey)
			{
				configManager.setConfiguration(AfkSalvagingConfig.GROUP, "spotFilter", filterKey);
			}

			@Override
			public void sortChanged(boolean nearestFirst)
			{
				configManager.setConfiguration(AfkSalvagingConfig.GROUP, "spotNearestFirst", nearestFirst);
			}

			@Override
			public void favouriteChanged(SalvagingSpot spot, boolean favourite)
			{
				saveFavourite(spot, favourite);
			}

			@Override
			public void autoRouteChanged(SalvagingSpot spot)
			{
				if (spot == null)
				{
					configManager.unsetConfiguration(AfkSalvagingConfig.GROUP, "autoRouteSpot");
				}
				else
				{
					configManager.setConfiguration(AfkSalvagingConfig.GROUP, "autoRouteSpot", spot.name());
				}
				setSpotStatus(spot == null ? "Auto route off." : "Auto route: " + spot.getSalvageName() + ", " + spot.getWhere()
					+ ". It is sent to Shortest Path when you board your boat from a dock.", false);
			}

			@Override
			public void testAlert()
			{
				sendTestAlert();
			}

			@Override
			public void sortItemAdd(String name, SortRule rule)
			{
				addSortItemByName(name, rule);
			}

			@Override
			public void sortItemRemove(int itemId)
			{
				clientThread.invokeLater(() -> markItem(itemId, null));
			}

			@Override
			public void forgetLearnedRates()
			{
				clientThread.invokeLater(() ->
				{
					session.forgetLearnedRates();
					setSpotStatus("Learned rates forgotten. The timer starts again from the published tables and "
						+ "learns afresh as salvage comes in.", false);
				});
			}
		});
		spotsPanel.setChoices(config.spotFilter(), config.spotNearestFirst());
		pushSortLists();
		spotsPanel.setFavourites(loadFavourites());
		spotsPanel.setAutoRoute(SpotList.spotNamed(config.autoRouteSpot()));
		spotsPanel.setDockShown(config.nearestDock());
		spotsPanel.setSailingLevel(sailingLevelForSpots());
		spotsPanel.setPicked(pickedSpot);
		String problem = ShortestPathPresence.check(pluginManager).problem();
		spotsPanel.setStatus(problem, problem != null);
		spotsButton = NavigationButton.builder()
			.tooltip("Salvaging spots")
			.icon(ImageUtil.loadImageResource(getClass(), "spots_icon.png"))
			.priority(7)
			.panel(spotsPanel)
			.build();
		clientToolbar.addNavigation(spotsButton);
	}

	private void removeSidebar()
	{
		if (spotsButton != null)
		{
			clientToolbar.removeNavigation(spotsButton);
			spotsButton = null;
			spotsPanel = null;
		}
	}

	/** Tells the sidebar what level the player salvages at. */
	private void shareSailingLevel()
	{
		int level = sailingLevelForSpots();
		if (spotsPanel != null)
		{
			spotsPanel.setSailingLevel(level);
		}
	}

	/** The level that decides which wrecks can be salvaged: the boosted one, as the crew use it. */
	private int sailingLevelForSpots()
	{
		return client.getGameState() == GameState.LOGGED_IN ? client.getBoostedSkillLevel(Skill.SAILING) : 0;
	}

	/**
	 * Centres the world map on a spot. The map can only be moved while it is open, and nothing in
	 * the API opens it for the player, so when it is closed the spot is kept until they open it.
	 * Called from the sidebar, so the work moves to the client thread.
	 */
	private void showSpotOnMap(SalvagingSpot spot)
	{
		clientThread.invokeLater(() -> pickSpot(spot));
		showPointOnMap(spot.getPoint(), spot.getSalvageName() + ", " + spot.getWhere());
	}

	private void showPointOnMap(WorldPoint point, String what)
	{
		clientThread.invokeLater(() ->
		{
			if (isWorldMapOpen())
			{
				pendingMapTarget = null;
				client.getWorldMap().setWorldMapPositionTarget(point);
			}
			else
			{
				pendingMapTarget = point;
				client.addChatMessage(ChatMessageType.GAMEMESSAGE, "",
					"<col=" + tipColour() + ">Open the world map and it will jump to " + what + ".</col>", null);
			}
		});
	}

	/**
	 * Hands the spot to the Shortest Path plugin over the event bus. The bus gives no reply, so the
	 * plugin list is checked first and a missing or switched-off Shortest Path is reported in the
	 * sidebar and in chat instead of silently doing nothing.
	 */
	private void routeToSpot(SalvagingSpot spot)
	{
		clientThread.invokeLater(() -> pickSpot(spot));
		route(spot.getPoint(), spot.getSalvageName() + ", " + spot.getWhere(), "Route sent to Shortest Path: ", spot);
	}

	/**
	 * Sends a route over the bus, or reports why it could not; the report starts with {@code sent}.
	 * A route to a salvaging spot is watched so it can be cleared on arrival; pass null for anything else.
	 */
	private void route(WorldPoint point, String what, String sent, SalvagingSpot spot)
	{
		clientThread.invokeLater(() ->
		{
			ShortestPathPresence presence = ShortestPathPresence.check(pluginManager);
			String problem = presence.problem();
			if (problem != null)
			{
				setSpotStatus(problem, true);
				client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "<col=" + tipColour() + ">" + problem + "</col>", null);
				return;
			}
			eventBus.post(ShortestPathMessages.routeTo(point));
			spotRoute = spot == null ? null : new SpotRoute(spot);
			setSpotStatus(sent + what + ".", false);
		});
	}

	/** Takes a route to a salvaging spot off the screen once the player is there or has left the boat. */
	private void watchSpotRoute(AfkSession.TickInputs in)
	{
		if (spotRoute == null)
		{
			return;
		}
		SpotRoute.End end = spotRoute.tick(in.ownBoat, in.playerPoint, session.wrecks().presentSites());
		if (end == SpotRoute.End.NONE)
		{
			return;
		}
		spotRoute = null;
		eventBus.post(ShortestPathMessages.clearRoute());
		setSpotStatus(end.getStatus(), false);
	}

	/** The player has just stepped from a dock onto their own boat: send the marked spot, if any. */
	private void boarded()
	{
		SalvagingSpot spot = SpotList.spotNamed(config.autoRouteSpot());
		if (spot == null || !config.autoRouteOnBoarding())
		{
			return;
		}
		pickSpot(spot);
		route(spot.getPoint(), spot.getSalvageName() + ", " + spot.getWhere(), "Auto route sent to Shortest Path: ", spot);
		if (ShortestPathPresence.check(pluginManager).problem() == null)
		{
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "<col=" + tipColour() + ">Auto route: "
				+ spot.getSalvageName() + ", " + spot.getWhere() + ".</col>", null);
		}
	}

	private void clearSpotRoute()
	{
		clientThread.invokeLater(() ->
		{
			eventBus.post(ShortestPathMessages.clearRoute());
			spotRoute = null;
			setSpotStatus(ShortestPathPresence.check(pluginManager).problem(), true);
		});
	}

	/** Fires the full-hold notification and banner so the player can check what they set up. */
	private void sendTestAlert()
	{
		clientThread.invokeLater(() ->
		{
			testAlertAt = clock();
			notifier.notify(config.notification(), "Test alert: this is the full cargo hold alert.");
			int seconds = config.bannerSeconds() > 0 ? config.bannerSeconds() : TEST_BANNER_SECONDS;
			setSpotStatus("Test alert sent" + (config.showBanner() ? ", with the banner for " + seconds + " s." : "."), false);
		});
	}

	private void setSpotStatus(String text, boolean problem)
	{
		if (spotsPanel != null)
		{
			spotsPanel.setStatus(text, problem);
		}
	}

	/** The sidebar's session line. */
	private String statsLine()
	{
		AfkSession.Stats stats = session.stats();
		if (stats.salvages == 0 && stats.sailingXp == 0 && stats.holdsFilled == 0 && stats.waitingMillis == 0)
		{
			return "This session: nothing salvaged yet.";
		}
		String sorted = "";
		if (stats.dropped > 0 || stats.alched > 0)
		{
			sorted = " · " + stats.dropped + " dropped · " + stats.alched + " alched";
		}
		return String.format("This session: %,d salvage · %,d Sailing XP · %d hold%s filled · %s waiting for wrecks%s",
			stats.salvages, stats.sailingXp, stats.holdsFilled, stats.holdsFilled == 1 ? "" : "s",
			Durations.coarse(stats.waitingMillis), sorted);
	}

	private Set<SalvagingSpot> loadFavourites()
	{
		if (configManager.getRSProfileKey() == null)
		{
			return SpotList.decodeFavourites(null);
		}
		return SpotList.decodeFavourites(configManager.getRSProfileConfiguration(AfkSalvagingConfig.GROUP, FAVOURITES_KEY));
	}

	private void saveFavourite(SalvagingSpot spot, boolean favourite)
	{
		if (configManager.getRSProfileKey() == null)
		{
			setSpotStatus("Favourites are kept per account: log in and press the star again to keep it.", true);
			return;
		}
		Set<SalvagingSpot> favourites = loadFavourites();
		if (favourite)
		{
			favourites.add(spot);
		}
		else
		{
			favourites.remove(spot);
		}
		String encoded = SpotList.encodeFavourites(favourites);
		if (encoded.isEmpty())
		{
			configManager.unsetRSProfileConfiguration(AfkSalvagingConfig.GROUP, FAVOURITES_KEY);
		}
		else
		{
			configManager.setRSProfileConfiguration(AfkSalvagingConfig.GROUP, FAVOURITES_KEY, encoded);
		}
	}

	private void pickSpot(SalvagingSpot spot)
	{
		pickedSpot = spot;
		if (spotsPanel != null)
		{
			spotsPanel.setPicked(spot);
		}
	}

	private boolean isWorldMapOpen()
	{
		return client.getWidget(InterfaceID.Worldmap.MAP_CONTAINER) != null;
	}

	private void clearState()
	{
		session.reset();
		cargoHoldByWorldView.clear();
		Arrays.fill(capacityFromInterface, CargoHoldCapacity.UNKNOWN);
		hookObjects.clear();
		monitoredSlot = 0;
		alertShownAt = 0;
		reminderShownAt = 0;
		readInterfaceNextTick = false;
		lastSailingXp = -1;
		worldChecked = -1;
		worldDirty = true;
		loginPending = true;
		lastSailing = false;
		lastOwnBoat = false;
		ticksSinceLogin = 0;
		sortActive = false;
	}

	private void forgetBoat()
	{
		cargoHoldByWorldView.clear();
		session.boat().clear();
		hookObjects.clear();
	}

	private void give(AfkSession.Notice notice, long now)
	{
		switch (notice)
		{
			case HOLD_FULL:
				alertShownAt = now;
				notifier.notify(config.notification(), session.holdMessage(notice));
				break;
			case HOLD_NEARLY_FULL:
				alertShownAt = now;
				Notification notification = config.notification();
				if (!config.earlyWarningSound())
				{
					notification = withoutSound(notification);
				}
				notifier.notify(notification, session.holdMessage(notice));
				break;
			case HOOK_EMPTY:
				reminderShownAt = now;
				notifier.notify(config.hookEmptyNotification(), "A salvaging hook is empty. Assign a crewmate to it.");
				break;
			case HOOK_IDLE:
				reminderShownAt = now;
				notifier.notify(config.hookIdleNotification(), "Your salvaging hook is idle and a wreck is up. Click the hook, or assign a crewmate.");
				break;
			case BOOST_DROPPED:
				int needed = session.view().levelNeeded;
				notifier.notify(config.boostDroppedNotification(), "Your crew stopped salvaging: your Sailing level is too low for this wreck"
					+ (needed > 0 ? " (needs " + needed + ")." : "."));
				break;
			case IDLE_LOGOUT_SOON:
				notifier.notify(config.idleLogoutNotification(), "You will be logged out for idling in about "
					+ Durations.countdown(session.view().idleLogoutMillis) + ". Move the mouse or press a key.");
				break;
			case WORLD_TIP:
				if (config.worldTip())
				{
					client.addChatMessage(ChatMessageType.GAMEMESSAGE, "",
						"<col=" + tipColour() + ">Waiting for a wreck? On the salvaging worlds (" + salvagingWorldsText()
							+ ") every site is worked, so wrecks near you come back sooner.</col>", null);
				}
				break;
			default:
				break;
		}
	}

	private void followBoat(WorldView view)
	{
		if (view == null)
		{
			return;
		}
		if (session.boat().getWorldViewId() != view.getId())
		{
			session.boat().follow(view.getId());
			hookObjects.clear();
		}
		scanBoat(view);
	}

	/** Picks up the facilities on the boat that were placed before the plugin was watching. */
	private void scanBoat(WorldView view)
	{
		Scene scene = view == null ? null : view.getScene();
		Tile[][][] tiles = scene == null ? null : scene.getTiles();
		if (tiles == null)
		{
			return;
		}
		for (Tile[][] plane : tiles)
		{
			for (Tile[] row : plane)
			{
				for (Tile tile : row)
				{
					GameObject[] objects = tile == null ? null : tile.getGameObjects();
					if (objects == null)
					{
						continue;
					}
					for (GameObject object : objects)
					{
						if (object != null)
						{
							objectSeen(object);
						}
					}
				}
			}
		}
	}

	/** Picks up the wrecks already in view. */
	private void scanWrecks(WorldView top)
	{
		Scene scene = top == null ? null : top.getScene();
		Tile[][][] tiles = scene == null ? null : scene.getTiles();
		if (tiles == null)
		{
			return;
		}
		long now = clock();
		for (Tile[][] plane : tiles)
		{
			for (Tile[] row : plane)
			{
				for (Tile tile : row)
				{
					GameObject[] objects = tile == null ? null : tile.getGameObjects();
					if (objects == null)
					{
						continue;
					}
					for (GameObject object : objects)
					{
						if (object != null && ShipwreckType.fromObjectId(object.getId()) != null)
						{
							session.wreckSeen(object.getWorldLocation(), object.getId(), now);
						}
					}
				}
			}
		}
	}

	private void objectSeen(GameObject object)
	{
		int worldViewId = object.getWorldView().getId();
		int id = object.getId();
		if (object.getWorldView().isTopLevel())
		{
			if (ShipwreckType.fromObjectId(id) != null)
			{
				session.wreckSeen(object.getWorldLocation(), id, clock());
			}
			return;
		}
		int resolved = resolveObjectId(id);
		if (CargoHoldCapacity.isCargoHold(resolved))
		{
			cargoHoldByWorldView.put(worldViewId, resolved);
		}
		if (session.boat().add(worldViewId, object.getHash(), id))
		{
			if (SalvagingHookTier.isHook(id))
			{
				hookObjects.put(object.getHash(), object);
			}
		}
		else if (worldViewId == session.boat().getWorldViewId() && SalvagingHookTier.fromObjectId(resolved) != null)
		{
			// A hook shown through a multi-state object: take the variant on display.
			session.boat().add(worldViewId, object.getHash(), resolved);
			hookObjects.put(object.getHash(), object);
		}
	}

	private void inventoryChanged(ItemContainer inventory)
	{
		Item[] items = inventory == null ? null : inventory.getItems();
		int occupied = 0;
		int salvage = 0;
		Set<ShipwreckType> types = EnumSet.noneOf(ShipwreckType.class);
		if (items != null)
		{
			for (Item item : items)
			{
				if (item == null || item.getId() <= -1 || item.getQuantity() <= 0)
				{
					continue;
				}
				occupied++;
				ShipwreckType type = ShipwreckType.fromSalvageItemId(item.getId());
				if (type != null)
				{
					salvage += item.getQuantity();
					types.add(type);
				}
			}
		}
		session.inventoryChanged(occupied, salvage, types, client.getTickCount());
	}

	private void loadCrewSlot(int slot)
	{
		int uniqueId = client.getVarbitValue(CREW_SLOT_VARBITS[slot]);
		int position = client.getVarbitValue(CREW_POSITION_VARBITS[slot]);
		Crewmate crewmate = null;
		if (uniqueId != 0)
		{
			crewmate = crewCache.get(uniqueId);
			if (crewmate == null)
			{
				crewmate = lookupCrewmate(uniqueId);
				if (!crewmate.getName().isEmpty())
				{
					crewCache.put(uniqueId, crewmate);
				}
			}
		}
		session.roster().setCrewmate(slot, crewmate);
		session.roster().setPosition(slot, position);
	}

	/** Reads a crewmate's name and deckhandiness from the game's crew table, as far as it will let us. */
	private Crewmate lookupCrewmate(int uniqueId)
	{
		String name = "";
		int deckhandiness = 0;
		try
		{
			List<Integer> rows = client.getDBRowsByValue(DBTableID.SailingCrew.ID, DBTableID.SailingCrew.COL_UNIQUE_ID, 0, uniqueId);
			if (rows != null && !rows.isEmpty())
			{
				int row = rows.get(0);
				Object[] stat = client.getDBTableField(row, DBTableID.SailingCrew.COL_STAT_DECKHANDINESS, 0);
				if (stat != null && stat.length > 0 && stat[0] instanceof Integer)
				{
					deckhandiness = (Integer) stat[0];
				}
				name = npcName(client.getDBTableField(row, DBTableID.SailingCrew.COL_CARGO_NPC, 0));
				if (name.isEmpty())
				{
					name = npcName(client.getDBTableField(row, DBTableID.SailingCrew.COL_BOAT_NPC, 0));
				}
			}
		}
		catch (RuntimeException e)
		{
			LOG.debug("Could not read crewmate {} from the crew table", uniqueId, e);
		}
		return new Crewmate(uniqueId, name, deckhandiness);
	}

	private String npcName(Object[] field)
	{
		if (field == null || field.length == 0 || !(field[0] instanceof Integer))
		{
			return "";
		}
		NPCComposition definition = client.getNpcDefinition((Integer) field[0]);
		return definition == null || definition.getName() == null ? "" : Text.removeTags(definition.getName());
	}

	/** Re-reads the hold of the boat the player last boarded, falling back to the remembered count. */
	private void reloadHold()
	{
		int slot = currentBoatSlot();
		if (slot != monitoredSlot)
		{
			session.monitor().reset();
			monitoredSlot = slot;
		}
		if (slot == 0)
		{
			return;
		}
		// The client keeps the hold's container after the interface closes, but the server only
		// updates it while the interface is open, so it is only the truth while that is showing.
		ItemContainer container = isCargoInterfaceOpen() ? client.getItemContainer(CargoHoldContainers.containerFor(slot)) : null;
		if (container != null)
		{
			session.holdCount(CargoHoldMonitor.countUsed(container.getItems()), client.getTickCount());
			saveUsed();
		}
		else if (session.monitor().getUsed() == CargoHoldCapacity.UNKNOWN)
		{
			loadSavedUsed(slot);
		}
		refreshCapacity();
	}

	/** A crewmate said the hold is full: ours if this is our boat, otherwise just worth a shout. */
	private void holdReportedFull()
	{
		if (isOwnBoat() || !isSailing())
		{
			session.fullLine();
			saveUsed();
			return;
		}
		if (!otherBoatFullNotified)
		{
			otherBoatFullNotified = true;
			notifier.notify(config.notification(), "The cargo hold is full.");
		}
	}

	/** Whether a hook the boat is known to have lacks the object needed to place it. */
	private boolean hooksMissingObjects()
	{
		for (BoatFacilities.Hook hook : session.boat().hooks())
		{
			if (!hookObjects.containsKey(hook.getHash()))
			{
				return true;
			}
		}
		return false;
	}

	/** Prefers the capacity the game showed in the hold interface, then the hold object on the boat. */
	private void refreshCapacity()
	{
		int slot = monitoredSlot != 0 ? monitoredSlot : currentBoatSlot();
		int capacity = slot != 0 ? capacityFromInterface[slot] : CargoHoldCapacity.UNKNOWN;
		if (capacity == CargoHoldCapacity.UNKNOWN && isSailing() && isOwnBoat())
		{
			Integer objectId = cargoHoldByWorldView.get(client.getLocalPlayer().getWorldView().getId());
			if (objectId != null)
			{
				capacity = CargoHoldCapacity.forObjectId(objectId);
			}
		}
		if (capacity != CargoHoldCapacity.UNKNOWN)
		{
			session.monitor().setCapacity(capacity);
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
			session.monitor().setCapacity(capacity);
			LOG.debug("Cargo hold interface reports capacity {} for boat {}", capacity, slot);
		}
		if (used >= 0 && used <= CargoHoldCapacity.MAX_SLOTS)
		{
			session.holdCount(used, client.getTickCount());
			saveUsed();
		}
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

	/** Whether the cargo hold window is open on screen. */
	boolean isCargoInterfaceOpen()
	{
		Widget root = client.getWidget(InterfaceID.SailingBoatCargohold.UNIVERSE);
		return root != null && !root.isHidden();
	}

	private void saveUsed()
	{
		if (monitoredSlot == 0 || configManager.getRSProfileKey() == null)
		{
			return;
		}
		String key = USED_KEY_PREFIX + monitoredSlot;
		int used = session.monitor().getUsed();
		if (used == CargoHoldCapacity.UNKNOWN)
		{
			configManager.unsetRSProfileConfiguration(AfkSalvagingConfig.GROUP, key);
		}
		else
		{
			String saved = configManager.getRSProfileConfiguration(AfkSalvagingConfig.GROUP, key);
			if (saved == null || !saved.equals(String.valueOf(used)))
			{
				configManager.setRSProfileConfiguration(AfkSalvagingConfig.GROUP, key, used);
			}
		}
	}

	private void loadSavedUsed(int slot)
	{
		if (configManager.getRSProfileKey() == null)
		{
			return;
		}
		String saved = configManager.getRSProfileConfiguration(AfkSalvagingConfig.GROUP, USED_KEY_PREFIX + slot);
		if (saved == null)
		{
			return;
		}
		try
		{
			session.monitor().setEstimatedUsed(Integer.parseInt(saved.trim()));
		}
		catch (NumberFormatException e)
		{
			LOG.debug("Ignoring unreadable remembered cargo count '{}' for boat {}", saved, slot);
		}
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

	/** The boat slot (1 to 5) the player last boarded, or 0 if none. */
	private int currentBoatSlot()
	{
		int slot = client.getVarbitValue(VarbitID.SAILING_LAST_PERSONAL_BOAT_BOARDED);
		return slot >= 1 && slot <= CargoHoldContainers.BOAT_SLOTS ? slot : 0;
	}

	/** Follows a multi-state object to the variant currently shown so its tier can be read. */
	private int resolveObjectId(int objectId)
	{
		if (CargoHoldCapacity.isCargoHold(objectId) || SalvagingHookTier.isHook(objectId))
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

	/** The world entity of the boat the player is on, or null. */
	private WorldEntity boatEntity()
	{
		Player player = client.getLocalPlayer();
		WorldView top = client.getTopLevelWorldView();
		if (player == null || top == null || player.getWorldView() == null || player.getWorldView().isTopLevel())
		{
			return null;
		}
		return top.worldEntities().byIndex(player.getWorldView().getId());
	}

	private static int crewCapacity(WorldEntity boat)
	{
		if (boat == null || boat.getConfig() == null)
		{
			return CrewRoster.SLOTS;
		}
		switch (boat.getConfig().getId())
		{
			case ENTITY_RAFT:
				return 0;
			case ENTITY_SKIFF:
				return 2;
			case ENTITY_SLOOP:
				return CrewRoster.SLOTS;
			default:
				return CrewRoster.SLOTS;
		}
	}

	/** Where the player stands in the top-level world, even while aboard a boat. */
	private WorldPoint playerTopLevelPoint()
	{
		Player player = client.getLocalPlayer();
		if (player == null)
		{
			return null;
		}
		WorldView view = player.getWorldView();
		if (view == null || view.isTopLevel())
		{
			return player.getWorldLocation();
		}
		WorldEntity boat = boatEntity();
		return boat == null ? null : topLevelPoint(boat, player.getLocalLocation());
	}

	private WorldPoint topLevelPoint(WorldEntity boat, LocalPoint local)
	{
		if (local == null)
		{
			return null;
		}
		LocalPoint top = boat.transformToMainWorld(local);
		return top == null ? null : WorldPoint.fromLocal(client, top);
	}

	/** Where the boat itself is in the top-level world. */
	private WorldPoint topLevelPoint(WorldEntity boat)
	{
		LocalPoint local = boat.getLocalLocation();
		return local == null ? null : WorldPoint.fromLocal(client, local);
	}

	private List<AfkSession.HookInput> hookInputs(WorldEntity boat)
	{
		List<AfkSession.HookInput> hooks = new ArrayList<>();
		if (boat == null)
		{
			return hooks;
		}
		for (BoatFacilities.Hook hook : session.boat().hooks())
		{
			GameObject object = hookObjects.get(hook.getHash());
			WorldPoint point = object == null ? null : topLevelPoint(boat, object.getLocalLocation());
			hooks.add(new AfkSession.HookInput(point, hook.getTier(), hook.isSecond()));
		}
		return hooks;
	}

	private void requestWorlds()
	{
		if (worldRequest != null)
		{
			worldRequest.cancel(false);
		}
		worldRequest = executor.submit(() ->
		{
			try
			{
				worldResult = worldService.getWorlds();
			}
			catch (RuntimeException e)
			{
				LOG.debug("World list not available", e);
			}
			worldDirty = true;
		});
	}

	private void refreshWorld()
	{
		int world = client.getWorld();
		if (!worldDirty && world == worldChecked)
		{
			return;
		}
		worldDirty = false;
		worldChecked = world;
		String activity = null;
		WorldResult result = worldResult;
		if (result != null)
		{
			World entry = result.findWorld(world);
			activity = entry == null ? null : entry.getActivity();
		}
		salvagingWorld = SalvagingWorlds.isSalvagingWorld(world, activity, SalvagingWorlds.parse(config.salvagingWorlds()));
	}

	/** Remembers the learned rate correction per wreck in the RuneScape profile's config. */
	private final class ProfileMemory implements AfkSession.MemoryStore
	{
		@Override
		public String load(ShipwreckType wreck)
		{
			if (configManager.getRSProfileKey() == null)
			{
				return null;
			}
			return configManager.getRSProfileConfiguration(AfkSalvagingConfig.GROUP, RATE_KEY_PREFIX + wreck.name());
		}

		@Override
		public void save(ShipwreckType wreck, String memory)
		{
			if (configManager.getRSProfileKey() == null || memory == null)
			{
				return;
			}
			configManager.setRSProfileConfiguration(AfkSalvagingConfig.GROUP, RATE_KEY_PREFIX + wreck.name(), memory);
		}

		@Override
		public void clear(ShipwreckType wreck)
		{
			if (configManager.getRSProfileKey() != null)
			{
				configManager.unsetRSProfileConfiguration(AfkSalvagingConfig.GROUP, RATE_KEY_PREFIX + wreck.name());
			}
		}
	}
}
