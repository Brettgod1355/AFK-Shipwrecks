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
	/** Ticks to wait for the inventory to change after a deposit or withdraw click. */
	public static final int HOLD_ACTION_TICKS = 3;
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

	/** Somewhere to remember the learned rate correction between sessions. */
	public interface MemoryStore
	{
		String load(ShipwreckType wreck);

		void save(ShipwreckType wreck, String memory);
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
		BOOST_DROPPED,
		WORLD_TIP
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
		public int wrecksUp;
		public long wreckWindowMillis;
		public boolean wreckWindowAnchored;
		public ShipwreckType wreckType;
		public int levelNeeded;
		public long waitingMillis = -1;
		public long sortingLeftMillis = -1;
		public long idleLogoutMillis = -1;
		public boolean showWorldTip;
		public boolean hazardous;
	}

	private final CargoHoldMonitor monitor = new CargoHoldMonitor();
	private final CrewRoster roster = new CrewRoster();
	private final BoatFacilities boat = new BoatFacilities();
	private final WreckTracker wrecks = new WreckTracker();
	private final SalvageEvents events = new SalvageEvents();
	private final HookWatch hookWatch = new HookWatch();
	private final PlayerActivity.Detector activity = new PlayerActivity.Detector();
	private final SmoothedCountdown countdown = new SmoothedCountdown();
	private final MemoryStore memory;
	private SalvageRateModel rate = new SalvageRateModel();
	private ShipwreckType rateWreck;

	private final Settings settings = new Settings();
	private final View view = new View();

	private int tick;
	private long now;
	private int lastContainerTick = Integer.MIN_VALUE;
	private int inventorySalvage;
	private int freeInventorySlots = 28;
	private int occupiedInventorySlots;
	private Set<ShipwreckType> inventorySalvageTypes = Collections.emptySet();
	private int hookedInInventory;
	private int pendingHoldTicks;
	private boolean pendingWithdraw;
	private int inventoryBefore;
	private boolean inventoryChanged;
	private int sortingFinishTick = -1;
	private PlayerActivity lastActivity = PlayerActivity.IDLE;
	private WorldPoint lastPlayerPoint;
	private int stillTicks;
	private boolean hazardous;
	private WorldPoint hazardAt;
	private long waitingSince = -1;
	private boolean worldTipGiven;
	private boolean boostDropNotified;
	private long lastAvailabilityAt = Long.MIN_VALUE;

	public AfkSession(MemoryStore memory)
	{
		this.memory = memory;
	}

	public Settings settings()
	{
		return settings;
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

	// ---- Events from the client ----

	/** A crewmate on this boat said they hooked salvage. */
	public void crewLine(String speakerName, int tick)
	{
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
		events.ghostLine(tick);
	}

	/** A crewmate, or the game, said the hold is full. */
	public void fullLine()
	{
		monitor.markFullByGame();
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
	}

	/** The player clicked the crystal extractor, whose XP must not be read as salvage. */
	public void extractorUsed(int tick)
	{
		events.ignoreXpUntil(tick + SalvageEvents.WINDOW_TICKS);
	}

	/**
	 * The player's inventory changed.
	 *
	 * @param occupied     occupied inventory slots
	 * @param salvage      unsorted salvage items in it
	 * @param salvageTypes which wrecks that salvage came from
	 */
	public void inventoryChanged(int occupied, int salvage, Set<ShipwreckType> salvageTypes, int tick)
	{
		if (pendingHoldTicks > 0)
		{
			inventoryChanged = true;
		}
		int gained = salvage - inventorySalvage;
		if (gained > 0 && activity.current() == PlayerActivity.SALVAGING && !(pendingHoldTicks > 0 && pendingWithdraw))
		{
			events.playerGain(gained, tick);
			hookedInInventory += gained;
		}
		else if (gained > 0 && pendingHoldTicks > 0 && pendingWithdraw)
		{
			hookedInInventory = 0;
		}
		if (salvage < hookedInInventory)
		{
			hookedInInventory = salvage;
		}
		occupiedInventorySlots = occupied;
		inventorySalvage = salvage;
		freeInventorySlots = Math.max(0, 28 - occupied);
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
		activity.reset();
		countdown.reset();
		rate = new SalvageRateModel();
		rateWreck = null;
		lastContainerTick = Integer.MIN_VALUE;
		inventorySalvage = 0;
		freeInventorySlots = 28;
		occupiedInventorySlots = 0;
		inventorySalvageTypes = Collections.emptySet();
		hookedInInventory = 0;
		pendingHoldTicks = 0;
		pendingWithdraw = false;
		inventoryChanged = false;
		sortingFinishTick = -1;
		lastActivity = PlayerActivity.IDLE;
		lastPlayerPoint = null;
		stillTicks = 0;
		hazardous = false;
		hazardAt = null;
		waitingSince = -1;
		worldTipGiven = false;
		boostDropNotified = false;
		lastAvailabilityAt = Long.MIN_VALUE;
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

		resolvePendingHoldAction();

		PlayerActivity current = in.sailing ? activity.update(in.animation, tick) : activity.current();
		if (!in.sailing)
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
		boolean aboard = in.sailing && in.ownBoat;

		wrecks.prune(now);
		List<WreckTracker.Site> eligible = aboard ? wrecks.eligibleActive(points, range, level) : Collections.emptyList();
		boolean wreckInReach = !eligible.isEmpty();
		boolean anyActive = aboard && wrecks.anyActive(points, range);
		ShipwreckType type = wreckInReach ? eligible.get(0).getType() : null;
		int levelNeeded = 0;
		if (anyActive && !wreckInReach)
		{
			for (WreckTracker.Site site : wrecks.nearby(points, range))
			{
				if (site.isActive() && (levelNeeded == 0 || site.getType().getSailingLevel() < levelNeeded))
				{
					levelNeeded = site.getType().getSailingLevel();
				}
			}
		}
		if (type != null)
		{
			rekeyRate(type);
		}

		boolean confirmedFull = monitor.isConfirmedFull();
		boolean estimatedFull = monitor.isFullByEstimate();
		List<Integer> hookSlots = roster.hookSlots(tick);
		int playerLevel = boat.hasWhirlpoolKeg() ? level + 2 : level;

		// What the tables say everyone on a hook would produce with a wreck up.
		double crewPotential = 0;
		ShipwreckType rateType = type != null ? type : rateWreck;
		for (int slot : hookSlots)
		{
			SalvagingHookTier tier = boat.tierForAssignment(roster.getPosition(slot));
			int d = effectiveDeckhandiness(roster.getCrewmate(slot));
			crewPotential += rateType == null ? 0.01 : SalvageChance.crew(rateType, tier, level, d) / SalvageRateModel.CREW_ROLL_TICKS;
		}
		boolean playerAtHook = aboard && current.isAtHook();
		boolean inventoryFull = freeInventorySlots <= 0;
		boolean playerRolling = aboard && current == PlayerActivity.SALVAGING && wreckInReach && !inventoryFull;
		double playerPotential = rateType == null ? 0
			: SalvageChance.player(rateType, playerHookTier(in), playerLevel) / SalvageRateModel.PLAYER_ROLL_TICKS;

		boolean crewRolling = aboard && wreckInReach && !confirmedFull && !hookSlots.isEmpty();
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
			rate.recordSalvage();
			if (event.getSource() != SalvageEvents.Source.CREW)
			{
				continue;
			}
			if (monitor.isFullByEstimate())
			{
				if (monitor.crewSalvagedWhileFull())
				{
					monitor.adjust(1);
				}
			}
			else if (!in.cargoInterfaceOpen && lastContainerTick != tick)
			{
				monitor.adjust(1);
			}
		}
		if (events.unmatchedGhostLines() >= GHOST_SILENCE_LINES)
		{
			events.resetUnmatchedGhostLines();
			if (ghostOnHook(hookSlots) && wreckInReach && parked && !confirmedFull && monitor.hasCount()
				&& monitor.remaining() <= Math.max(settings.warnSlotsRemaining, GHOST_SILENCE_SLOTS))
			{
				monitor.markFullByGame();
				confirmedFull = true;
			}
		}
		confirmedFull = monitor.isConfirmedFull();
		estimatedFull = monitor.isFullByEstimate();

		if (aboard && parked && !confirmedFull && (!hookSlots.isEmpty() || playerAtHook))
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
		est.holdFullUnconfirmed = estimatedFull;
		est.holdRemaining = monitor.remaining();
		est.crewExpectedPerTick = crewPotential;
		est.playerExpectedPerTick = playerPotential;
		est.playerAtHook = playerAtHook;
		est.playerRolling = playerRolling;
		est.inventorySalvage = hookedInInventory;
		est.freeInventorySlots = freeInventorySlots;
		est.countInventorySalvage = settings.countHookedSalvage;
		est.wreckInReach = wreckInReach;
		est.onlyHigherWrecksInReach = anyActive && !wreckInReach;
		est.hazardous = hazardous;
		est.stalled = rate.isStalled() && expected > 0;
		est.wreckWindowMillis = wrecks.allSunkWithin(points, range, level, now);
		est.availability = wrecks.availability(settings.salvagingWorld ? AVAILABILITY_PRIOR_SALVAGING_WORLD
			: AVAILABILITY_PRIOR_OTHER_WORLD, AVAILABILITY_PRIOR_WEIGHT_MILLIS, now);
		est.correction = rate.correction();
		est.confident = rate.isConfident();
		AfkEstimate estimate = AfkEstimator.estimate(est);

		boolean running = estimate.getState() == AfkEstimate.State.COUNTING_DOWN
			|| estimate.getState() == AfkEstimate.State.INVENTORY_FILLS_FIRST
			|| estimate.getState() == AfkEstimate.State.WRECK_SINKS_FIRST;
		long shown = countdown.update(estimate.hasEta() ? estimate.getEtaMillis() : -1, running, now);

		if (estimate.getState() == AfkEstimate.State.WAITING_FOR_WRECK)
		{
			if (waitingSince < 0)
			{
				waitingSince = now;
			}
		}
		else
		{
			waitingSince = -1;
		}

		// Alerts.
		CargoHoldMonitor.Level level1 = monitor.poll(now, settings.warnSlotsRemaining, settings.repeatFullMillis);
		if (level1 == CargoHoldMonitor.Level.FULL)
		{
			notices.add(Notice.HOLD_FULL);
		}
		else if (level1 == CargoHoldMonitor.Level.NEARLY_FULL)
		{
			notices.add(Notice.HOLD_NEARLY_FULL);
		}

		int spare = roster.countSpareFor(boat.lowestHookTier(), tick);
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

		if (estimate.getState() == AfkEstimate.State.LEVEL_TOO_LOW && !hookSlots.isEmpty())
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
		view.wrecksUp = eligible.size();
		view.wreckWindowMillis = est.wreckWindowMillis;
		view.wreckWindowAnchored = wrecks.allAnchored(points, range, level);
		view.wreckType = type;
		view.levelNeeded = levelNeeded;
		view.waitingMillis = waitingSince < 0 ? -1 : now - waitingSince;
		view.sortingLeftMillis = sortingFinishTick < 0 ? -1 : Math.max(0, (sortingFinishTick - tick) * 600L);
		view.idleLogoutMillis = in.idleLogoutMillis;
		view.showWorldTip = showTip;
		view.hazardous = hazardous;
		return notices;
	}

	/** The message for a hold alert, with the count when it is known and not just the game's word. */
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

	private boolean updateParked(TickInputs in)
	{
		WorldPoint point = in.playerPoint;
		if (point == null || !in.sailing)
		{
			stillTicks = 0;
			lastPlayerPoint = point;
			hazardous = false;
			return false;
		}
		if (lastPlayerPoint != null && lastPlayerPoint.distanceTo2D(point) == 0 && lastPlayerPoint.getPlane() == point.getPlane())
		{
			stillTicks++;
		}
		else
		{
			stillTicks = 0;
		}
		lastPlayerPoint = point;
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

	/** The hook the player is standing at, else the best hook aboard: the player normally takes the best. */
	private SalvagingHookTier playerHookTier(TickInputs in)
	{
		if (in.playerPoint != null)
		{
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
			if (nearest != null && nearestDistance <= 2 && nearest.tier != null)
			{
				return nearest.tier;
			}
		}
		SalvagingHookTier best = boat.bestHookTier();
		return best != null ? best : SalvagingHookTier.BRONZE;
	}
}
