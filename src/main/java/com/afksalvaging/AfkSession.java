/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.runelite.api.coords.WorldPoint;

/**
 * Everything the plugin knows about the current salvaging session, and how it fits together.
 * <p>
 * The plugin turns client events into calls on this class and reads back a {@link View} each tick;
 * nothing in here touches the client, so the way the signals combine can be tested with scripted
 * sessions. Times are milliseconds from a steady clock; ticks are game ticks.
 */
public final class AfkSession
{
	/** Tiles from a hook within which a wreck can be worked. */
	public static final int HOOK_RANGE = 8;
	/** Tiles from the player used when no hook position is known. */
	public static final int PLAYER_RANGE = 12;
	/** Ticks the boat must have stood still before it counts as parked. */
	public static final int PARKED_TICKS = 5;
	/** Ticks to wait for the inventory to change after a deposit or withdraw click: the walk to the hold takes a few. */
	public static final int HOLD_ACTION_TICKS = 10;
	/** Ticks the player needs to sort one piece of salvage. */
	public static final int SORT_TICKS = 3;
	/** Unanswered wails from the ghost, in a row, before he is taken to have stopped salvaging. */
	public static final int GHOST_SILENCE_LINES = 6;
	/** How near capacity the tally must be for the ghost's silence to mean the hold is full. */
	public static final int GHOST_SILENCE_SLOTS = 3;
	/** Assumed share of time a wreck is up when nothing has been measured yet. */
	public static final double AVAILABILITY_PRIOR_SALVAGING_WORLD = 0.93;
	public static final double AVAILABILITY_PRIOR_OTHER_WORLD = 0.8;
	public static final long AVAILABILITY_PRIOR_WEIGHT_MILLIS = 5 * 60_000L;
	/** How long the crew must have waited for a wreck before the world tip is offered. */
	public static final long WORLD_TIP_AFTER_MILLIS = 60_000;
	/** Client ticks are 20 ms; the game's idle timeout is measured in them. */
	public static final int CLIENT_TICK_MILLIS = 20;
	/** Inventory slots. */
	public static final int INVENTORY_SLOTS = 28;

	/** Somewhere to remember the learned rate correction between sessions. */
	public interface MemoryStore
	{
		String load(ShipwreckType wreck);

		void save(ShipwreckType wreck, String memory);

		/** Forgets what was remembered for a wreck. */
		void clear(ShipwreckType wreck);
	}

	/** Running totals since the client started, for the sidebar. Logging out does not clear them. */
	public static final class Stats
	{
		/** Salvage hooked aboard the player's own boat, by crew and player. */
		public int salvages;
		/** Sailing XP earned aboard, salvaging and sorting alike. */
		public long sailingXp;
		/** Times the hold was confirmed full. */
		public int holdsFilled;
		/** Time spent with no wreck up to salvage. */
		public long waitingMillis;
		/** Drop clicks on items the sorter boxed red. */
		public int dropped;
		/** High Level Alchemy casts. */
		public int alched;

		public void reset()
		{
			salvages = 0;
			sailingXp = 0;
			holdsFilled = 0;
			waitingMillis = 0;
			dropped = 0;
			alched = 0;
		}
	}

	/** Player choices the session needs. */
	public static final class Settings
	{
		public boolean countHookedSalvage = true;
		public int warnSlotsRemaining;
		public long repeatFullMillis;
		public long graceMillis = 15_000;
		public long repeatReminderMillis = 60_000;
		public boolean salvagingWorld;
		/** Warn this long before the idle logout; 0 for never. */
		public long idleWarnMillis = 60_000;
	}

	/** A hook on the boat as the client sees it right now. */
	public static final class HookInput
	{
		public final WorldPoint point;
		public final SalvagingHookTier tier;
		public final boolean second;

		public HookInput(WorldPoint point, SalvagingHookTier tier, boolean second)
		{
			this.point = point;
			this.tier = tier;
			this.second = second;
		}
	}

	/** What the client knows this tick. */
	public static final class TickInputs
	{
		public int tick;
		public long now;
		public boolean sailing;
		public boolean ownBoat;
		/** Crew slots the boat can carry. */
		public int boatCrewCapacity;
		public int animation;
		public int boostedSailingLevel;
		public List<HookInput> hooks = Collections.emptyList();
		/** Where the boat itself is, in the top-level world; it does not move when the player walks the deck. */
		public WorldPoint boatPoint;
		/** Where the player stands, in the top-level world. */
		public WorldPoint playerPoint;
		public boolean cargoInterfaceOpen;
		/** Milliseconds before the game logs the player out for idling, or -1 when not known. */
		public long idleLogoutMillis = -1;
	}

	/** Things the plugin should tell the player about this tick. */
	public enum Notice
	{
		HOLD_FULL,
		HOLD_NEARLY_FULL,
		HOOK_EMPTY,
		HOOK_IDLE,
		/** A stronger crewmate is idle while a weaker one works a hook. */
		BETTER_CREW,
		BOOST_DROPPED,
		WORLD_TIP,
		/** The game will log the player out for idling soon, and they are aboard their own boat. */
		IDLE_LOGOUT_SOON
	}

	/** What to show. */
	public static final class View
	{
		public AfkEstimate estimate = AfkEstimate.of(AfkEstimate.State.NOT_SAILING);
		/** Smoothed countdown for the estimate, or -1. */
		public long countdownMillis = -1;
		public PlayerActivity activity = PlayerActivity.IDLE;
		public int hookCount;
		public List<Crewmate> crewOnHooks = Collections.emptyList();
		public boolean playerAtHook;
		public int emptyHooks;
		public int spareCrew;
		public boolean spareCrewCannotUseHook;
		public HookWatch.Reason reminder;
		/** A swap already suggested and still worth making, or null. */
		public CrewSwapWatch.Suggestion betterCrew;
		/** How long the boat has stood on one tile, 0 while it moves or when not sailing. */
		public long boatStillMillis;
		/** When a wreck should next rise in reach while we wait, or null when nothing says. */
		public WreckTracker.NextRise nextWreck;
		/** Usable wrecks up in reach. */
		public int wrecksUp;
		/** Wrecks up in reach that the player's level is too low for. */
		public int higherWrecksUp;
		public long wreckWindowMillis;
		public boolean wreckWindowAnchored;
		public ShipwreckType wreckType;
		public int levelNeeded;
		public long waitingMillis = -1;
		public long sortingLeftMillis = -1;
		public long idleLogoutMillis = -1;
		/** The idle logout is within the warning window and the player is aboard their own boat. */
		public boolean idleWarning;
		public boolean showWorldTip;
		public boolean hazardous;
	}

	private final CargoHoldMonitor monitor = new CargoHoldMonitor();
	private final CrewRoster roster = new CrewRoster();
	private final BoatFacilities boat = new BoatFacilities();
	private final WreckTracker wrecks = new WreckTracker();
	private final SalvageEvents events = new SalvageEvents();
	private final HookWatch hookWatch = new HookWatch();
	private final CrewSwapWatch crewSwap = new CrewSwapWatch();
	private final PlayerActivity.Detector activity = new PlayerActivity.Detector();
	private final SmoothedCountdown countdown = new SmoothedCountdown();
	private final MemoryStore memory;
	private SalvageRateModel rate = new SalvageRateModel();
	private ShipwreckType rateWreck;

	private final Settings settings = new Settings();
	private final View view = new View();
	private final Stats stats = new Stats();
	private boolean wasConfirmedFull;
	private long lastTickNow = -1;

	private int tick;
	private long now;
	/** Whether the last tick found the player on their own boat: crew speech heard elsewhere is not ours. */
	private boolean aboard;
	private int lastContainerTick = Integer.MIN_VALUE;
	private int fullLineTick = Integer.MIN_VALUE;
	private boolean inventoryKnown;
	private int inventorySalvage;
	private int freeInventorySlots = INVENTORY_SLOTS;
	private int occupiedInventorySlots;
	private Set<ShipwreckType> inventorySalvageTypes = Collections.emptySet();
	private int hookedInInventory;
	private int pendingHoldTicks;
	private boolean pendingWithdraw;
	private int inventoryBefore;
	private boolean inventoryChanged;
	private int sortingFinishTick = -1;
	private PlayerActivity lastActivity = PlayerActivity.IDLE;
	private WorldPoint lastBoatPoint;
	private int stillTicks;
	private boolean hazardous;
	private WorldPoint hazardAt;
	private long waitingSince = -1;
	private boolean worldTipGiven;
	private boolean boostDropNotified;
	private boolean idleWarned;
	private long lastAvailabilityAt = Long.MIN_VALUE;

	public AfkSession(MemoryStore memory)
	{
		this.memory = memory;
	}

	public Settings settings()
	{
		return settings;
	}

	public Stats stats()
	{
		return stats;
	}

	/**
	 * Throws away the learned rate correction for every wreck, in memory and in the store, so the
	 * timer starts again from the published tables.
	 */
	public void forgetLearnedRates()
	{
		rate = new SalvageRateModel();
		if (memory != null)
		{
			for (ShipwreckType type : ShipwreckType.values())
			{
				memory.clear(type);
			}
		}
	}

	public CargoHoldMonitor monitor()
	{
		return monitor;
	}

	public CrewRoster roster()
	{
		return roster;
	}

	public BoatFacilities boat()
	{
		return boat;
	}

	public WreckTracker wrecks()
	{
		return wrecks;
	}

	public SalvageRateModel rate()
	{
		return rate;
	}

	public View view()
	{
		return view;
	}

	public PlayerActivity activity()
	{
		return activity.current();
	}

	public int getHookedInInventory()
	{
		return hookedInInventory;
	}

	/** Milliseconds before the game's idle logout, from the client's idle counters, or -1 when it is not counting. */
	public static long idleLogoutMillis(int timeoutClientTicks, int mouseIdleTicks, int keyboardIdleTicks)
	{
		if (timeoutClientTicks <= 0)
		{
			return -1;
		}
		int idle = Math.min(mouseIdleTicks, keyboardIdleTicks);
		return Math.max(0, (long) (timeoutClientTicks - idle) * CLIENT_TICK_MILLIS);
	}

	// ---- Events from the client ----

	/** A crewmate on this boat said they hooked salvage. Ignored unless the player is on their own boat. */
	public void crewLine(String speakerName, int tick)
	{
		if (!aboard)
		{
			return;
		}
		int slot = roster.slotByName(speakerName);
		int deckhandiness = 0;
		if (slot >= 0)
		{
			roster.noteSalvage(slot, tick);
			deckhandiness = effectiveDeckhandiness(roster.getCrewmate(slot));
		}
		events.crewLine(deckhandiness, tick);
	}

	/** The ghost crewmate wailed. */
	public void ghostLine(int tick)
	{
		if (aboard)
		{
			events.ghostLine(tick);
		}
	}

	/** A crewmate on this boat, or the game, said the hold is full. */
	public void fullLine()
	{
		monitor.markFullByGame();
		fullLineTick = tick;
	}

	/** The game said it is not safe to salvage here. */
	public void hazardLine(WorldPoint boatPoint)
	{
		hazardous = true;
		hazardAt = boatPoint;
	}

	/** Sailing XP arrived. */
	public void sailingXp(int delta, int tick)
	{
		if (!aboard)
		{
			return;
		}
		SalvageEvents.XpContext context = new SalvageEvents.XpContext();
		context.wreck = view.wreckType;
		context.playerSalvaging = activity.current() == PlayerActivity.SALVAGING;
		context.playerSorting = activity.current() == PlayerActivity.SORTING;
		context.crewDeckhandiness = deckhandinessOnHooks(tick);
		double[] sorting = new double[inventorySalvageTypes.size()];
		int i = 0;
		for (ShipwreckType type : inventorySalvageTypes)
		{
			sorting[i++] = type.getSortingXp();
		}
		context.sortingXp = sorting;
		events.sailingXp(delta, tick, context);
		stats.sailingXp += delta;
	}

	/** The player used the crystal extractor, whose XP must not be read as salvage. */
	public void extractorUsed(int tick)
	{
		events.ignoreXpUntil(tick + SalvageEvents.WINDOW_TICKS);
	}

	/**
	 * The player's inventory changed. The first call after a reset only records where things stand.
	 *
	 * @param occupied     occupied inventory slots
	 * @param salvage      unsorted salvage items in it
	 * @param salvageTypes which wrecks that salvage came from
	 */
	public void inventoryChanged(int occupied, int salvage, Set<ShipwreckType> salvageTypes, int tick)
	{
		if (inventoryKnown)
		{
			if (pendingHoldTicks > 0)
			{
				// Only a change in the direction the click implies can be that click's doing.
				boolean matches = pendingWithdraw ? occupied > inventoryBefore : occupied < inventoryBefore;
				if (matches)
				{
					inventoryChanged = true;
				}
			}
			int gained = salvage - inventorySalvage;
			boolean withdrawing = pendingHoldTicks > 0 && pendingWithdraw;
			if (gained > 0 && withdrawing)
			{
				hookedInInventory = 0;
			}
			else if (gained > 0 && aboard && activity.current() == PlayerActivity.SALVAGING)
			{
				events.playerGain(gained, tick);
				hookedInInventory += gained;
			}
			if (salvage < hookedInInventory)
			{
				hookedInInventory = salvage;
			}
		}
		inventoryKnown = true;
		occupiedInventorySlots = occupied;
		inventorySalvage = salvage;
		freeInventorySlots = Math.max(0, INVENTORY_SLOTS - occupied);
		inventorySalvageTypes = salvageTypes == null ? Collections.emptySet() : salvageTypes;
	}

	/** The player clicked Deposit or Withdraw on the hold, so the inventory is about to change. */
	public void holdActionClicked(boolean withdraw)
	{
		pendingHoldTicks = HOLD_ACTION_TICKS;
		pendingWithdraw = withdraw;
		inventoryBefore = occupiedInventorySlots;
		inventoryChanged = false;
		hookWatch.noteHoldUse();
		if (withdraw)
		{
			hookedInInventory = 0;
		}
	}

	/** The player clicked something else before reaching the hold, so the deposit or withdrawal is off. */
	public void holdActionCancelled()
	{
		pendingHoldTicks = 0;
		inventoryChanged = false;
	}

	/** An exact count of the hold arrived from the game. */
	public void holdCount(int used, int tick)
	{
		monitor.setUsed(used);
		lastContainerTick = tick;
		pendingHoldTicks = 0;
		inventoryChanged = false;
	}

	/** The game said the player deposited one piece of carried cargo without opening the hold. */
	public void depositLine()
	{
		monitor.adjust(1);
	}

	/** A wreck object is in view. */
	public void wreckSeen(WorldPoint point, int objectId, long now)
	{
		wrecks.observe(point, objectId, now);
	}

	/** A wreck object left the scene. */
	public void wreckGone(WorldPoint point, int objectId, long now)
	{
		wrecks.despawn(point, objectId, now);
	}

	/** The top-level scene is being reloaded, so wreck objects will be seen again shortly. */
	public void sceneReloading(long now)
	{
		wrecks.markAllAbsent(now);
	}

	/** The player hopped worlds: the wrecks are different ones, the crew and the hold are the same. */
	public void worldHopped()
	{
		wrecks.clear();
		hookWatch.reset();
		crewSwap.reset();
		activity.reset();
		waitingSince = -1;
		worldTipGiven = false;
		hazardous = false;
		countdown.reset();
	}

	/** Everything the boat knows is stale, for example after logging out. Learned rates are kept in memory. */
	public void reset()
	{
		flushMemory();
		monitor.reset();
		roster.clear();
		boat.clear();
		wrecks.clear();
		events.reset();
		hookWatch.reset();
		crewSwap.reset();
		activity.reset();
		countdown.reset();
		rate = new SalvageRateModel();
		rateWreck = null;
		aboard = false;
		lastContainerTick = Integer.MIN_VALUE;
		fullLineTick = Integer.MIN_VALUE;
		inventoryKnown = false;
		inventorySalvage = 0;
		freeInventorySlots = INVENTORY_SLOTS;
		occupiedInventorySlots = 0;
		inventorySalvageTypes = Collections.emptySet();
		hookedInInventory = 0;
		pendingHoldTicks = 0;
		pendingWithdraw = false;
		inventoryChanged = false;
		sortingFinishTick = -1;
		lastActivity = PlayerActivity.IDLE;
		lastBoatPoint = null;
		stillTicks = 0;
		hazardous = false;
		hazardAt = null;
		waitingSince = -1;
		worldTipGiven = false;
		boostDropNotified = false;
		idleWarned = false;
		lastAvailabilityAt = Long.MIN_VALUE;
		wasConfirmedFull = false;
		lastTickNow = -1;
	}

	/** Writes the learned rate correction to memory. */
	public void flushMemory()
	{
		if (memory != null && rateWreck != null)
		{
			String encoded = rate.encodeMemory();
			if (encoded != null)
			{
				memory.save(rateWreck, encoded);
			}
		}
	}

	// ---- The tick ----

	/**
	 * Advances the session by one game tick.
	 *
	 * @return notices due this tick, in the order they should be given
	 */
	public List<Notice> tick(TickInputs in)
	{
		List<Notice> notices = new ArrayList<>();
		tick = in.tick;
		now = in.now;
		aboard = in.sailing && in.ownBoat;

		resolvePendingHoldAction();

		PlayerActivity current;
		if (in.sailing)
		{
			current = activity.update(in.animation, tick);
		}
		else
		{
			activity.reset();
			current = PlayerActivity.IDLE;
			if (monitor.isReportedFullByGame())
			{
				monitor.clearFullByGame();
			}
		}
		if (current == PlayerActivity.SORTING && lastActivity != PlayerActivity.SORTING)
		{
			hookWatch.noteSortingStarted();
			hookedInInventory = 0;
			sortingFinishTick = tick + SORT_TICKS * inventorySalvage;
		}
		else if (current != PlayerActivity.SORTING)
		{
			sortingFinishTick = -1;
		}
		lastActivity = current;

		boolean parked = updateParked(in);
		roster.setBoatCapacity(in.boatCrewCapacity);

		List<WorldPoint> points = new ArrayList<>();
		for (HookInput hook : in.hooks)
		{
			if (hook.point != null)
			{
				points.add(hook.point);
			}
		}
		int range = HOOK_RANGE;
		if (points.isEmpty() && in.playerPoint != null)
		{
			points.add(in.playerPoint);
			range = PLAYER_RANGE;
		}
		int level = Math.max(1, in.boostedSailingLevel);

		wrecks.prune(now);
		List<WreckTracker.Site> eligible = aboard ? wrecks.eligibleActive(points, range, level) : Collections.emptyList();
		boolean wreckInReach = !eligible.isEmpty();
		ShipwreckType type = wreckInReach ? eligible.get(0).getType() : null;
		int levelNeeded = 0;
		int higherWrecksUp = 0;
		if (aboard)
		{
			for (WreckTracker.Site site : wrecks.nearby(points, range))
			{
				if (site.isActive() && site.getType().getSailingLevel() > level)
				{
					higherWrecksUp++;
					if (levelNeeded == 0 || site.getType().getSailingLevel() < levelNeeded)
					{
						levelNeeded = site.getType().getSailingLevel();
					}
				}
			}
		}
		if (type != null)
		{
			rekeyRate(type);
		}

		boolean confirmedFull = monitor.isConfirmedFull();
		List<Integer> hookSlots = roster.hookSlots(tick);
		boolean crewOnHooks = !hookSlots.isEmpty();
		int playerLevel = boat.hasWhirlpoolKeg() ? level + 2 : level;

		// What the tables say everyone on a hook would produce with a wreck up.
		ShipwreckType rateType = type != null ? type : rateWreck;
		double crewPotential = 0;
		if (rateType != null)
		{
			for (int slot : hookSlots)
			{
				SalvagingHookTier tier = boat.tierForAssignment(roster.getPosition(slot));
				int d = effectiveDeckhandiness(roster.getCrewmate(slot));
				crewPotential += SalvageChance.crew(rateType, tier, level, d) / SalvageRateModel.CREW_ROLL_TICKS;
			}
		}
		boolean inventoryFull = freeInventorySlots <= 0;
		boolean playerRolling = aboard && current == PlayerActivity.SALVAGING && wreckInReach && !inventoryFull;
		// Standing at a hook with a wreck up and not rolling is not manning it: the player will not restart.
		boolean playerAtHook = aboard && (current == PlayerActivity.SALVAGING
			|| (current == PlayerActivity.AT_HOOK_IDLE && !wreckInReach));
		double playerPotential = rateType == null ? 0
			: SalvageChance.player(rateType, playerHookTier(in), playerLevel) / SalvageRateModel.PLAYER_ROLL_TICKS;

		boolean crewRolling = aboard && wreckInReach && !confirmedFull && crewOnHooks;
		double expected = (crewRolling ? crewPotential : 0) + (playerRolling ? playerPotential : 0);
		if (type != null)
		{
			rate.addExpected(expected);
			if (expected > 0)
			{
				wrecks.noteRolling(points, range, level, now);
			}
		}

		// Salvage that has arrived since last tick.
		for (SalvageEvents.Event event : events.drain(tick))
		{
			if (!aboard)
			{
				continue;
			}
			rate.recordSalvage();
			stats.salvages++;
			if (event.getSource() != SalvageEvents.Source.CREW || event.getTick() <= fullLineTick)
			{
				// The player's own salvage goes to their inventory; salvage hooked before the game said
				// "full" was already in the hold when it said so.
				continue;
			}
			if (monitor.isFullByEstimate())
			{
				monitor.crewSalvagedWhileFull();
			}
			else if (!in.cargoInterfaceOpen && lastContainerTick != tick)
			{
				monitor.adjust(1);
			}
		}
		boolean ghostShouldBeSalvaging = ghostOnHook(hookSlots) && wreckInReach && parked && !confirmedFull;
		if (!ghostShouldBeSalvaging)
		{
			// His wails only mean something while he ought to be hooking salvage.
			events.resetUnmatchedGhostLines();
		}
		else if (events.unmatchedGhostLines() >= GHOST_SILENCE_LINES)
		{
			events.resetUnmatchedGhostLines();
			if (monitor.hasCount() && monitor.remaining() <= GHOST_SILENCE_SLOTS)
			{
				monitor.markFullByGame();
				fullLineTick = tick;
			}
		}
		confirmedFull = monitor.isConfirmedFull();
		if (confirmedFull && !wasConfirmedFull)
		{
			stats.holdsFilled++;
		}
		wasConfirmedFull = confirmedFull;

		if (aboard && parked && !confirmedFull && (crewOnHooks || playerAtHook))
		{
			long dt = lastAvailabilityAt == Long.MIN_VALUE ? 600 : Math.max(0, Math.min(5_000, now - lastAvailabilityAt));
			wrecks.recordAvailability(wreckInReach, dt, now);
		}
		lastAvailabilityAt = now;

		AfkEstimator.Inputs est = new AfkEstimator.Inputs();
		est.sailing = aboard;
		est.hookCount = boat.hookCount();
		est.holdKnown = monitor.hasCount();
		est.holdFull = confirmedFull;
		est.holdFullUnconfirmed = monitor.isFullByEstimate();
		est.holdDrifted = monitor.isDriftKnown();
		est.holdRemaining = monitor.remaining();
		est.crewOnHooks = crewOnHooks;
		est.crewExpectedPerTick = crewPotential;
		est.playerExpectedPerTick = playerPotential;
		est.playerAtHook = playerAtHook || (aboard && current == PlayerActivity.AT_HOOK_IDLE);
		est.playerRolling = playerRolling;
		est.inventoryFull = inventoryFull;
		est.inventorySalvage = hookedInInventory;
		est.freeInventorySlots = freeInventorySlots;
		est.countInventorySalvage = settings.countHookedSalvage;
		est.wreckInReach = wreckInReach;
		est.onlyHigherWrecksInReach = higherWrecksUp > 0 && !wreckInReach;
		est.hazardous = hazardous;
		est.stalled = rate.isStalled() && expected > 0;
		est.wreckWindowMillis = wrecks.allSunkWithin(points, range, level, now);
		est.availability = wrecks.availability(settings.salvagingWorld ? AVAILABILITY_PRIOR_SALVAGING_WORLD
			: AVAILABILITY_PRIOR_OTHER_WORLD, AVAILABILITY_PRIOR_WEIGHT_MILLIS, now);
		est.correction = rate.correction();
		est.confident = rate.isConfident();
		AfkEstimate estimate = AfkEstimator.estimate(est);

		long shown;
		if (estimate.getState() == AfkEstimate.State.STALLED)
		{
			// Hold the last figure rather than blanking it for what is probably a short gap.
			shown = countdown.remainingAt(now, false);
		}
		else
		{
			boolean running = estimate.getState() == AfkEstimate.State.COUNTING_DOWN
				|| estimate.getState() == AfkEstimate.State.INVENTORY_FILLS_FIRST
				|| estimate.getState() == AfkEstimate.State.WRECK_SINKS_FIRST;
			shown = countdown.update(estimate.hasEta() ? estimate.getEtaMillis() : -1, running, now);
		}

		if (estimate.getState() == AfkEstimate.State.WAITING_FOR_WRECK)
		{
			if (waitingSince < 0)
			{
				waitingSince = now;
			}
			else if (lastTickNow >= 0 && now > lastTickNow)
			{
				stats.waitingMillis += now - lastTickNow;
			}
		}
		else
		{
			waitingSince = -1;
		}
		lastTickNow = now;

		// Alerts.
		CargoHoldMonitor.Level alert = monitor.poll(now, settings.warnSlotsRemaining, settings.repeatFullMillis);
		if (alert == CargoHoldMonitor.Level.FULL)
		{
			notices.add(Notice.HOLD_FULL);
		}
		else if (alert == CargoHoldMonitor.Level.NEARLY_FULL)
		{
			notices.add(Notice.HOLD_NEARLY_FULL);
		}

		int spare = roster.countSpareFor(emptyHookTier(in, hookSlots), tick, boat.hasWhirlpoolKeg() ? 2 : 0);
		HookWatch.Situation situation = new HookWatch.Situation();
		situation.hookCount = boat.hookCount();
		situation.crewOnHooks = hookSlots.size();
		situation.playerAtHook = playerAtHook;
		situation.playerSorting = current == PlayerActivity.SORTING;
		situation.spareCrew = spare;
		situation.holdFull = confirmedFull;
		situation.wreckInReach = wreckInReach;
		situation.parked = aboard && parked;
		HookWatch.Signal signal = hookWatch.update(now, situation, settings.graceMillis, settings.repeatReminderMillis);
		if (signal != HookWatch.Signal.NONE)
		{
			notices.add(hookWatch.getReason() == HookWatch.Reason.PLAYER_NEEDED ? Notice.HOOK_IDLE : Notice.HOOK_EMPTY);
		}

		// Only once every hook is manned: an empty hook is the thing to fix first, and the hook watch says so.
		boolean hooksAllManned = situation.parked && !confirmedFull && boat.hookCount() > 0
			&& HookWatch.emptyHooks(boat.hookCount(), hookSlots.size(), playerAtHook) == 0;
		if (crewSwap.update(now, hooksAllManned ? betterCrew(tick) : null) != null)
		{
			notices.add(Notice.BETTER_CREW);
		}

		if (estimate.getState() == AfkEstimate.State.LEVEL_TOO_LOW && crewOnHooks)
		{
			if (!boostDropNotified)
			{
				boostDropNotified = true;
				notices.add(Notice.BOOST_DROPPED);
			}
		}
		else if (wreckInReach)
		{
			boostDropNotified = false;
		}

		// The idle logout: one warning per idle stretch, only aboard the player's own boat, where
		// being logged out is what the whole plugin exists to prevent going unnoticed.
		boolean idleWarning = in.sailing && in.ownBoat && settings.idleWarnMillis > 0
			&& in.idleLogoutMillis >= 0 && in.idleLogoutMillis <= settings.idleWarnMillis;
		if (idleWarning)
		{
			if (!idleWarned)
			{
				idleWarned = true;
				notices.add(Notice.IDLE_LOGOUT_SOON);
			}
		}
		else
		{
			idleWarned = false;
		}

		boolean showTip = false;
		if (estimate.getState() == AfkEstimate.State.WAITING_FOR_WRECK && !settings.salvagingWorld
			&& waitingSince >= 0 && now - waitingSince >= WORLD_TIP_AFTER_MILLIS)
		{
			showTip = true;
			if (!worldTipGiven)
			{
				worldTipGiven = true;
				notices.add(Notice.WORLD_TIP);
			}
		}

		// The view.
		view.estimate = estimate;
		view.countdownMillis = shown;
		view.activity = current;
		view.hookCount = boat.hookCount();
		view.crewOnHooks = roster.onHooks(tick);
		view.playerAtHook = playerAtHook;
		view.emptyHooks = HookWatch.emptyHooks(boat.hookCount(), hookSlots.size(), playerAtHook);
		view.spareCrew = spare;
		view.spareCrewCannotUseHook = spare == 0 && !roster.idle(tick).isEmpty();
		view.reminder = hookWatch.getReason();
		view.betterCrew = crewSwap.standing();
		view.boatStillMillis = stillTicks * 600L;
		view.wrecksUp = eligible.size();
		view.higherWrecksUp = higherWrecksUp;
		view.wreckWindowMillis = est.wreckWindowMillis;
		view.wreckWindowAnchored = wrecks.allAnchored(points, range, level);
		view.nextWreck = aboard ? wrecks.nextRise(points, range, level, settings.salvagingWorld, now) : null;
		view.wreckType = type;
		view.levelNeeded = levelNeeded;
		view.waitingMillis = waitingSince < 0 ? -1 : now - waitingSince;
		view.sortingLeftMillis = sortingFinishTick < 0 ? -1 : Math.max(0, (sortingFinishTick - tick) * 600L);
		view.idleLogoutMillis = in.idleLogoutMillis;
		view.idleWarning = idleWarning;
		view.showWorldTip = showTip;
		view.hazardous = hazardous;
		return notices;
	}

	/** The message for a hold alert, with the count when it is known and not just the game's word. */
	/** The swap to suggest, in words, or null when none stands. */
	public String betterCrewMessage()
	{
		CrewSwapWatch.Suggestion swap = crewSwap.standing();
		if (swap == null)
		{
			return null;
		}
		return crewName(swap.in) + " (deckhandiness " + swap.inDeckhandiness + ") is free: put them on the hook instead of "
			+ crewName(swap.out) + " (" + swap.outDeckhandiness + ").";
	}

	/** A crewmate's name, or a stand-in when the crew table could not be read for them. */
	private static String crewName(Crewmate crewmate)
	{
		String name = crewmate.getName();
		return name == null || name.trim().isEmpty() ? "A crewmate" : name;
	}

	/**
	 * The weakest crewmate on a hook and the strongest idle crewmate who could take that hook, when
	 * the idle one is deckhandier; null otherwise. A crewmate must meet the hook's deckhandiness to
	 * work it, and a keg of whirlpool surprise lifts everyone to at least 2.
	 */
	private CrewSwapWatch.Suggestion betterCrew(int tick)
	{
		int floor = boat.hasWhirlpoolKeg() ? 2 : 0;
		Crewmate weakest = null;
		int weakestDeckhandiness = Integer.MAX_VALUE;
		SalvagingHookTier tier = null;
		for (int slot : roster.hookSlots(tick))
		{
			Crewmate crewmate = roster.getCrewmate(slot);
			if (crewmate == null)
			{
				continue;
			}
			int d = effectiveDeckhandiness(crewmate);
			if (d < weakestDeckhandiness)
			{
				weakest = crewmate;
				weakestDeckhandiness = d;
				tier = boat.tierForAssignment(roster.getPosition(slot));
			}
		}
		if (weakest == null)
		{
			return null;
		}
		int needed = tier == null ? 1 : tier.getDeckhandiness();
		Crewmate best = null;
		int bestDeckhandiness = weakestDeckhandiness;
		for (Crewmate crewmate : roster.idle(tick))
		{
			if (crewmate.getDeckhandiness() <= 0 && Crewmate.knownDeckhandiness(crewmate.getName()) <= 0)
			{
				// Only a guess at this one's deckhandiness: not grounds to send someone off a hook.
				continue;
			}
			int d = effectiveDeckhandiness(crewmate);
			if (d > bestDeckhandiness && Math.max(floor, crewmate.effectiveDeckhandiness()) >= needed)
			{
				best = crewmate;
				bestDeckhandiness = d;
			}
		}
		return best == null ? null : new CrewSwapWatch.Suggestion(weakest, best, weakestDeckhandiness, bestDeckhandiness);
	}

	public String holdMessage(Notice notice)
	{
		String count = "";
		if (monitor.hasCount() && !monitor.isReportedFullByGame())
		{
			count = " (" + monitor.getUsed() + "/" + monitor.getCapacity() + ")";
		}
		return notice == Notice.HOLD_FULL
			? "Your cargo hold is full" + count + "."
			: "Your cargo hold is nearly full" + count + ".";
	}

	// ---- Internals ----

	private void resolvePendingHoldAction()
	{
		if (pendingHoldTicks <= 0)
		{
			return;
		}
		if (inventoryChanged)
		{
			int delta = inventoryBefore - occupiedInventorySlots;
			pendingHoldTicks = 0;
			inventoryChanged = false;
			if (delta > 0)
			{
				hookedInInventory = Math.max(0, hookedInInventory - delta);
			}
			if (delta != 0)
			{
				monitor.adjust(delta);
			}
		}
		else
		{
			pendingHoldTicks--;
		}
	}

	/** Whether the boat has stood still for a while. The boat's own position, not the player's on deck. */
	private boolean updateParked(TickInputs in)
	{
		WorldPoint point = in.boatPoint;
		if (point == null)
		{
			for (HookInput hook : in.hooks)
			{
				if (hook.point != null)
				{
					point = hook.point;
					break;
				}
			}
		}
		if (point == null)
		{
			point = in.playerPoint;
		}
		if (point == null || !in.sailing)
		{
			stillTicks = 0;
			lastBoatPoint = point;
			hazardous = false;
			return false;
		}
		if (lastBoatPoint != null && lastBoatPoint.getPlane() == point.getPlane() && lastBoatPoint.distanceTo2D(point) == 0)
		{
			stillTicks++;
		}
		else
		{
			stillTicks = 0;
		}
		lastBoatPoint = point;
		if (hazardous && hazardAt != null && hazardAt.distanceTo2D(point) > 1)
		{
			hazardous = false;
		}
		return stillTicks >= PARKED_TICKS;
	}

	private void rekeyRate(ShipwreckType type)
	{
		if (type == rateWreck)
		{
			return;
		}
		flushMemory();
		rate = new SalvageRateModel();
		if (memory != null)
		{
			rate.restoreMemory(memory.load(type));
		}
		rateWreck = type;
	}

	private int effectiveDeckhandiness(Crewmate crewmate)
	{
		int d = crewmate == null ? SalvageRateModel.DEFAULT_DECKHANDINESS : crewmate.effectiveDeckhandiness();
		return boat.hasWhirlpoolKeg() ? Math.max(2, d) : d;
	}

	private int[] deckhandinessOnHooks(int tick)
	{
		boolean[] seen = new boolean[5];
		for (int slot : roster.hookSlots(tick))
		{
			seen[effectiveDeckhandiness(roster.getCrewmate(slot))] = true;
		}
		int count = 0;
		for (int d = 1; d <= 4; d++)
		{
			if (seen[d])
			{
				count++;
			}
		}
		int[] values = new int[count];
		int i = 0;
		for (int d = 1; d <= 4; d++)
		{
			if (seen[d])
			{
				values[i++] = d;
			}
		}
		return values;
	}

	private boolean ghostOnHook(List<Integer> hookSlots)
	{
		for (int slot : hookSlots)
		{
			Crewmate crewmate = roster.getCrewmate(slot);
			if (crewmate != null && crewmate.getName().toLowerCase(Locale.ROOT).contains("jenkins"))
			{
				return true;
			}
		}
		return false;
	}

	/** The hook nearest the player, if they are standing at one. */
	private HookInput hookAtPlayer(TickInputs in)
	{
		if (in.playerPoint == null)
		{
			return null;
		}
		HookInput nearest = null;
		int nearestDistance = Integer.MAX_VALUE;
		for (HookInput hook : in.hooks)
		{
			if (hook.point == null || hook.point.getPlane() != in.playerPoint.getPlane())
			{
				continue;
			}
			int distance = hook.point.distanceTo2D(in.playerPoint);
			if (distance < nearestDistance)
			{
				nearestDistance = distance;
				nearest = hook;
			}
		}
		return nearest != null && nearestDistance <= 2 ? nearest : null;
	}

	/** The hook the player is standing at, else the best hook aboard: the player normally takes the best. */
	private SalvagingHookTier playerHookTier(TickInputs in)
	{
		HookInput at = hookAtPlayer(in);
		if (at != null && at.tier != null)
		{
			return at.tier;
		}
		SalvagingHookTier best = boat.bestHookTier();
		return best != null ? best : SalvagingHookTier.BRONZE;
	}

	/**
	 * The easiest hook nobody is working, which is where a spare crewmate would go. Crew are matched
	 * to hooks by their assignment and the player to the hook they stand at; anything left is empty.
	 * Falls back to the weakest hook aboard when nothing can be told apart.
	 */
	private SalvagingHookTier emptyHookTier(TickInputs in, List<Integer> hookSlots)
	{
		List<BoatFacilities.Hook> free = new ArrayList<>(boat.hooks());
		if (free.isEmpty())
		{
			return null;
		}
		for (int slot : hookSlots)
		{
			int position = roster.getPosition(slot);
			boolean wantSecond = position == CrewAssignment.HOOK_SLOOP_2;
			boolean known = position == CrewAssignment.HOOK_SLOOP_1 || position == CrewAssignment.HOOK_SLOOP_2;
			removeOne(free, known ? wantSecond : null);
		}
		HookInput at = hookAtPlayer(in);
		if (at != null && aboard && activity.current().isAtHook())
		{
			removeOne(free, at.second);
		}
		SalvagingHookTier easiest = null;
		for (BoatFacilities.Hook hook : free)
		{
			if (easiest == null || hook.getTier().ordinal() < easiest.ordinal())
			{
				easiest = hook.getTier();
			}
		}
		return easiest != null ? easiest : boat.lowestHookTier();
	}

	/** Removes the hook in the given position (null for any) from the list, if there is one. */
	private static void removeOne(List<BoatFacilities.Hook> hooks, Boolean second)
	{
		for (int i = 0; i < hooks.size(); i++)
		{
			if (second == null || hooks.get(i).isSecond() == second)
			{
				hooks.remove(i);
				return;
			}
		}
		if (!hooks.isEmpty())
		{
			hooks.remove(0);
		}
	}
}
