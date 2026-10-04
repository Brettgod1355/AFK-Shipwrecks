/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

import com.afksalvaging.AfkEstimate.State;
import com.afksalvaging.AfkSession.Notice;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.AnimationID;
import net.runelite.api.gameval.ObjectID;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Scripted sessions: a sloop with two dragon hooks parked at a merchant wreck, and what the plugin
 * should make of what the client reports.
 */
public class AfkSessionTest
{
	private static final long TICK = 600;
	private static final int BOAT_VIEW = 7;
	private static final WorldPoint HOOK_A = new WorldPoint(1800, 4000, 0);
	private static final WorldPoint HOOK_B = new WorldPoint(1800, 4004, 0);
	private static final WorldPoint PLAYER = new WorldPoint(1801, 4001, 0);
	private static final WorldPoint BOAT = new WorldPoint(1799, 4002, 0);
	private static final WorldPoint WRECK = new WorldPoint(1806, 4002, 0);
	private static final int SALVAGING = AnimationID.SAILING_HUMAN_SALVAGE_HOOK_KANDARIN_3X8_DROP01;
	private static final int SORTING = AnimationID.HUMAN_SAILING_SALVAGE01_LARGE01_INTERACT01;
	private static final Crewmate JENKINS = new Crewmate(7, "Cabin Boy Jenkins", 4);
	private static final Crewmate JOLLY = new Crewmate(9, "Jolly Jim", 4);
	private static final Crewmate ADA = new Crewmate(3, "Adventurer Ada", 1);

	private final Map<ShipwreckType, String> memory = new EnumMap<>(ShipwreckType.class);
	private final AfkSession session = new AfkSession(new AfkSession.MemoryStore()
	{
		@Override
		public String load(ShipwreckType wreck)
		{
			return memory.get(wreck);
		}

		@Override
		public void save(ShipwreckType wreck, String value)
		{
			memory.put(wreck, value);
		}

		@Override
		public void clear(ShipwreckType wreck)
		{
			memory.remove(wreck);
		}
	});

	private int tick;
	private long now = 1_000_000;
	private int animation = -1;
	private boolean sailing = true;
	private boolean ownBoat = true;
	private int level = 99;
	private boolean interfaceOpen;
	private WorldPoint playerPoint = PLAYER;
	private WorldPoint boatPoint = BOAT;
	private long idleLogout = -1;
	private final List<Notice> notices = new ArrayList<>();

	@Before
	public void parkAtAMerchantWreck()
	{
		session.boat().follow(BOAT_VIEW);
		session.boat().add(BOAT_VIEW, 1L, ObjectID.SALVAGING_HOOK_LARGE_DRAGON);
		session.boat().add(BOAT_VIEW, 2L, ObjectID.SALVAGING_HOOK_LARGE_DRAGON_B);
		session.monitor().setCapacity(240);
		session.holdCount(200, tick);
		session.wreckSeen(WRECK, ObjectID.SAILING_MERCHANT_SHIPWRECK, now);
		session.settings().salvagingWorld = true;
		// The plugin reads the inventory once at login so later changes have a baseline.
		session.inventoryChanged(0, 0, Collections.emptySet(), tick);
	}

	private void twoCrewOnHooks()
	{
		session.roster().setCrewmate(0, JENKINS);
		session.roster().setPosition(0, CrewAssignment.HOOK_SLOOP_1);
		session.roster().setCrewmate(1, JOLLY);
		session.roster().setPosition(1, CrewAssignment.HOOK_SLOOP_2);
	}

	private AfkSession.View tick()
	{
		tick++;
		now += TICK;
		AfkSession.TickInputs in = new AfkSession.TickInputs();
		in.tick = tick;
		in.now = now;
		in.sailing = sailing;
		in.ownBoat = ownBoat;
		in.boatCrewCapacity = 5;
		in.animation = animation;
		in.boostedSailingLevel = level;
		in.hooks = Arrays.asList(
			new AfkSession.HookInput(HOOK_A, SalvagingHookTier.DRAGON, false),
			new AfkSession.HookInput(HOOK_B, SalvagingHookTier.DRAGON, true));
		in.boatPoint = boatPoint;
		in.playerPoint = playerPoint;
		in.cargoInterfaceOpen = interfaceOpen;
		in.idleLogoutMillis = idleLogout;
		notices.addAll(session.tick(in));
		return session.view();
	}

	private AfkSession.View ticks(int count)
	{
		AfkSession.View view = null;
		for (int i = 0; i < count; i++)
		{
			view = tick();
		}
		return view;
	}

	private int count(Notice notice)
	{
		int n = 0;
		for (Notice given : notices)
		{
			if (given == notice)
			{
				n++;
			}
		}
		return n;
	}

	@Test
	public void twoCrewGiveACountdownFromTheFirstTick()
	{
		twoCrewOnHooks();
		AfkSession.View view = ticks(AfkSession.PARKED_TICKS + 1);
		assertEquals(State.COUNTING_DOWN, view.estimate.getState());
		assertTrue(view.estimate.isApproximate());
		// Merchant with a dragon hook at 99: (1 + 67) / 256 per player roll; each level 4 deckhand
		// gets 40% of that every 5 ticks. Two of them: 0.0425 per tick, 0.0708 per second. The wreck
		// is good for 240 s (17 slots); the other 23 come at the salvaging-world availability of 93%.
		// A few ticks of expected-but-unseen salvage nudge the correction a percent under 1, hence the slack.
		double perSecond = 2 * (68 / 256.0) * 0.4 / 5 / 0.6;
		double expected = 240 + (40 - perSecond * 240) / (perSecond * AfkSession.AVAILABILITY_PRIOR_SALVAGING_WORLD);
		assertEquals(expected * 1000, view.estimate.getEtaMillis(), 12_000);
		assertEquals(40 / perSecond * 1000, view.estimate.getWorkMillis(), 12_000);
		assertEquals(2, view.crewOnHooks.size());
		assertEquals(0, view.emptyHooks);
		assertEquals(1, view.wrecksUp);
		assertEquals(ShipwreckType.MERCHANT, view.wreckType);
		assertTrue(view.wreckWindowAnchored);
		assertTrue(notices.isEmpty());
	}

	@Test
	public void crewLinesFillTheHoldAndTeachTheRate()
	{
		twoCrewOnHooks();
		ticks(10);
		// Fifteen salvages from Jenkins (by his XP: 40% of 200) and Jolly Jim (line plus XP).
		for (int i = 0; i < 15; i++)
		{
			ticks(20);
			session.sailingXp(80, tick);
			session.crewLine("Jolly Jim", tick);
			session.sailingXp(80, tick);
		}
		ticks(3);
		assertEquals(230, session.monitor().getUsed());
		assertTrue(session.monitor().isEstimate());
		assertEquals(30, session.rate().lifetimeEvents());
		assertTrue(session.rate().correction() > 1.0);
		assertEquals(State.COUNTING_DOWN, session.view().estimate.getState());
	}

	@Test
	public void theHoldFillsAndTheAlertFires()
	{
		twoCrewOnHooks();
		session.holdCount(238, tick);
		ticks(6);
		session.crewLine("Jolly Jim", tick);
		ticks(3);
		assertEquals(239, session.monitor().getUsed());
		session.crewLine("Jolly Jim", tick);
		ticks(3);
		assertEquals(240, session.monitor().getUsed());
		assertEquals(1, count(Notice.HOLD_FULL));
		assertEquals(State.HOLD_FULL_UNCONFIRMED, session.view().estimate.getState());
		assertEquals("Your cargo hold is full (240/240).", session.holdMessage(Notice.HOLD_FULL));
		session.fullLine();
		ticks(1);
		assertEquals(State.HOLD_FULL, session.view().estimate.getState());
		assertEquals(1, count(Notice.HOLD_FULL));
	}

	@Test
	public void crewStillProducingMeansTheTallyWasHigh()
	{
		twoCrewOnHooks();
		session.holdCount(238, tick);
		ticks(6);
		session.crewLine("Jolly Jim", tick);
		session.crewLine("Jolly Jim", tick);
		ticks(3);
		assertEquals(State.HOLD_FULL_UNCONFIRMED, session.view().estimate.getState());
		assertEquals(1, count(Notice.HOLD_FULL));
		// One more could be a race with the count.
		session.crewLine("Jolly Jim", tick);
		ticks(3);
		assertEquals(State.HOLD_FULL_UNCONFIRMED, session.view().estimate.getState());
		// Two mean the tally was high: it is held just under capacity and the timer says so.
		session.crewLine("Jolly Jim", tick);
		ticks(3);
		assertEquals(239, session.monitor().getUsed());
		assertEquals(State.HOLD_DRIFTED, session.view().estimate.getState());
		// However much more arrives, it does not announce full again on its own.
		for (int i = 0; i < 5; i++)
		{
			session.crewLine("Jolly Jim", tick);
			ticks(3);
		}
		assertEquals(239, session.monitor().getUsed());
		assertEquals(State.HOLD_DRIFTED, session.view().estimate.getState());
		assertEquals(1, count(Notice.HOLD_FULL));
		// A real count puts things right.
		session.holdCount(236, tick);
		ticks(1);
		assertEquals(State.COUNTING_DOWN, session.view().estimate.getState());
		// And the game's own word is final.
		session.fullLine();
		ticks(1);
		assertEquals(State.HOLD_FULL, session.view().estimate.getState());
		assertEquals(2, count(Notice.HOLD_FULL));
	}

	@Test
	public void salvageHookedBeforeTheGameSaidFullIsNotAddedAgain()
	{
		twoCrewOnHooks();
		session.holdCount(238, tick);
		ticks(6);
		// The crewmate's line and the "full" message arrive together, and the player opens the hold
		// in the same tick: the delayed crew event must not push the fresh count back up.
		session.crewLine("Jolly Jim", tick);
		session.fullLine();
		session.holdCount(239, tick);
		ticks(3);
		assertEquals(239, session.monitor().getUsed());
		assertEquals(State.COUNTING_DOWN, session.view().estimate.getState());
		assertEquals(0, count(Notice.HOLD_FULL));
	}

	@Test
	public void theEarlyWarningFiresAtTheChosenMargin()
	{
		twoCrewOnHooks();
		session.settings().warnSlotsRemaining = 2;
		session.holdCount(237, tick);
		ticks(6);
		assertEquals(0, count(Notice.HOLD_NEARLY_FULL));
		session.crewLine("Jolly Jim", tick);
		ticks(3);
		assertEquals(238, session.monitor().getUsed());
		assertEquals(1, count(Notice.HOLD_NEARLY_FULL));
		assertEquals("Your cargo hold is nearly full (238/240).", session.holdMessage(Notice.HOLD_NEARLY_FULL));
		session.crewLine("Jolly Jim", tick);
		ticks(3);
		assertEquals(1, count(Notice.HOLD_NEARLY_FULL));
		assertEquals(0, count(Notice.HOLD_FULL));
		session.crewLine("Jolly Jim", tick);
		ticks(3);
		assertEquals(1, count(Notice.HOLD_FULL));
	}

	@Test
	public void steppingOffAHookRemindsToAssignTheSpareCrewmate()
	{
		session.roster().setCrewmate(0, JENKINS);
		session.roster().setPosition(0, CrewAssignment.HOOK_SLOOP_1);
		session.roster().setCrewmate(1, JOLLY);
		animation = SALVAGING;
		AfkSession.View view = ticks(10);
		assertTrue(view.playerAtHook);
		assertEquals(0, view.emptyHooks);
		assertEquals(State.COUNTING_DOWN, view.estimate.getState());

		// The player walks to the station and sorts.
		animation = SORTING;
		session.settings().graceMillis = 15_000;
		ticks(PlayerActivity.Detector.HOLD_TICKS + 1);
		assertEquals(1, session.view().emptyHooks);
		assertEquals(PlayerActivity.SORTING, session.view().activity);
		// Sorting cuts the grace to three seconds.
		ticks(6);
		assertEquals(1, count(Notice.HOOK_EMPTY));
		assertEquals(HookWatch.Reason.SPARE_CREW, session.view().reminder);

		// Jolly Jim is put on the hook.
		session.roster().setPosition(1, CrewAssignment.HOOK_SLOOP_2);
		ticks(2);
		assertNull(session.view().reminder);
		assertEquals(0, session.view().emptyHooks);
		assertEquals(1, count(Notice.HOOK_EMPTY));
	}

	@Test
	public void aCrewmateWhoCannotUseTheHookIsNotOffered()
	{
		session.roster().setCrewmate(0, JENKINS);
		session.roster().setPosition(0, CrewAssignment.HOOK_SLOOP_1);
		session.roster().setCrewmate(1, ADA);
		session.settings().graceMillis = 3_000;
		AfkSession.View view = ticks(40);
		assertEquals(1, view.emptyHooks);
		assertTrue(view.spareCrewCannotUseHook);
		assertEquals(0, count(Notice.HOOK_EMPTY));
		// With a wreck up and nobody else able to take it, the player is asked instead.
		assertEquals(1, count(Notice.HOOK_IDLE));
		assertEquals(HookWatch.Reason.PLAYER_NEEDED, view.reminder);
	}

	@Test
	public void aStrongerIdleCrewmateIsSuggestedOnceForAWeakerOneOnAHook()
	{
		session.roster().setCrewmate(0, ADA);
		session.roster().setPosition(0, CrewAssignment.HOOK_SLOOP_1);
		session.roster().setCrewmate(1, JOLLY);
		session.roster().setPosition(1, CrewAssignment.HOOK_SLOOP_2);
		session.roster().setCrewmate(2, JENKINS);
		AfkSession.View view = ticks(10);
		// Ten seconds of grace first, in case the crew are only being shuffled (the boat also has to
		// settle as parked before the watch starts counting).
		assertEquals(0, count(Notice.BETTER_CREW));
		assertNull(view.betterCrew);
		view = ticks(20);
		assertEquals(1, count(Notice.BETTER_CREW));
		assertEquals(JENKINS, view.betterCrew.in);
		assertEquals(ADA, view.betterCrew.out);
		assertTrue(session.betterCrewMessage().startsWith("Cabin Boy Jenkins (deckhandiness 4) is free"));
		// Said once; it stands on the overlay but is not repeated.
		ticks(40);
		assertEquals(1, count(Notice.BETTER_CREW));
		// Once swapped, nothing more to say.
		session.roster().setPosition(0, CrewAssignment.NONE);
		session.roster().setPosition(2, CrewAssignment.HOOK_SLOOP_1);
		view = ticks(2);
		assertNull(view.betterCrew);
		assertNull(session.betterCrewMessage());
	}

	@Test
	public void aSwapNeedsTheIdleCrewmateToMeetTheHookAndToReallyBeBetter()
	{
		// The test boat's hooks are dragon, which need deckhandiness 4: a 3 cannot take one, however weak the 1 on it.
		Crewmate tom = new Crewmate(11, "Test Tom", 3);
		session.roster().setCrewmate(0, ADA);
		session.roster().setPosition(0, CrewAssignment.HOOK_SLOOP_1);
		session.roster().setCrewmate(1, JOLLY);
		session.roster().setPosition(1, CrewAssignment.HOOK_SLOOP_2);
		session.roster().setCrewmate(2, tom);
		ticks(40);
		assertEquals(0, count(Notice.BETTER_CREW));
		// A crewmate whose deckhandiness is only guessed at is never offered either.
		session.roster().setCrewmate(2, new Crewmate(12, "", 0));
		ticks(40);
		assertEquals(0, count(Notice.BETTER_CREW));
	}

	@Test
	public void noSwapIsSuggestedWhileAHookIsEmptyOrTheIdleCrewmateIsNoBetter()
	{
		// One hook empty: filling it comes first, and the hook watch says so.
		session.roster().setCrewmate(0, ADA);
		session.roster().setPosition(0, CrewAssignment.HOOK_SLOOP_1);
		session.roster().setCrewmate(2, JENKINS);
		ticks(40);
		assertEquals(0, count(Notice.BETTER_CREW));
		// Both hooks manned by the strong pair, a weak one idle: nothing to gain.
		session.roster().setPosition(0, CrewAssignment.NONE);
		session.roster().setPosition(2, CrewAssignment.HOOK_SLOOP_1);
		session.roster().setCrewmate(1, JOLLY);
		session.roster().setPosition(1, CrewAssignment.HOOK_SLOOP_2);
		ticks(40);
		assertEquals(0, count(Notice.BETTER_CREW));
	}

	@Test
	public void theBoatsStillTimeGrowsWhileItStandsAndResetsWhenItMoves()
	{
		AfkSession.View view = ticks(10);
		assertTrue("stood " + view.boatStillMillis, view.boatStillMillis >= 5_000);
		boatPoint = new WorldPoint(BOAT.getX() + 3, BOAT.getY(), 0);
		view = tick();
		assertEquals(0, view.boatStillMillis);
		view = ticks(4);
		assertEquals(4 * 600L, view.boatStillMillis);
		boatPoint = BOAT;
	}

	@Test
	public void anEmptyHookAwayFromAnySpotIsNobodysBusiness()
	{
		// No wreck site in view: parked at a dock, say. A spare crewmate and an empty hook, but no reminder.
		session.wrecks().clear();
		session.roster().setCrewmate(0, JENKINS);
		session.roster().setPosition(0, CrewAssignment.HOOK_SLOOP_1);
		session.roster().setCrewmate(1, JOLLY);
		session.settings().graceMillis = 3_000;
		AfkSession.View view = ticks(40);
		assertEquals(1, view.emptyHooks);
		assertEquals(0, count(Notice.HOOK_EMPTY));
		assertNull(view.reminder);
		// A wreck site comes into view: now it matters.
		session.wreckSeen(WRECK, ObjectID.SAILING_MERCHANT_SHIPWRECK, now);
		ticks(40);
		assertEquals(1, count(Notice.HOOK_EMPTY));
	}

	@Test
	public void theWreckSinksAndTheTimerWaits()
	{
		twoCrewOnHooks();
		ticks(10);
		session.wreckSeen(WRECK, ObjectID.SAILING_MERCHANT_SHIPWRECK_STUMP, now);
		AfkSession.View view = ticks(2);
		assertEquals(State.WAITING_FOR_WRECK, view.estimate.getState());
		assertTrue(view.estimate.hasWork());
		assertEquals(0, view.wrecksUp);
		assertTrue(view.waitingMillis >= 0);
		assertFalse(view.showWorldTip);
		long waited = view.waitingMillis;
		view = ticks(10);
		assertTrue(view.waitingMillis > waited);

		// Another rises; the clock is anchored once our crew are rolling on it.
		session.wreckSeen(WRECK, ObjectID.SAILING_MERCHANT_SHIPWRECK, now);
		view = ticks(2);
		assertEquals(State.COUNTING_DOWN, view.estimate.getState());
		assertTrue(view.wreckWindowAnchored);
		assertEquals(240_000 - TICK, view.wreckWindowMillis, TICK);
		assertNull(session.view().reminder);
	}

	@Test
	public void waitingOnAnOrdinaryWorldEarnsTheTipOnce()
	{
		twoCrewOnHooks();
		session.settings().salvagingWorld = false;
		ticks(5);
		session.wreckSeen(WRECK, ObjectID.SAILING_MERCHANT_SHIPWRECK_STUMP, now);
		ticks((int) (AfkSession.WORLD_TIP_AFTER_MILLIS / TICK) - 1);
		assertEquals(0, count(Notice.WORLD_TIP));
		assertFalse(session.view().showWorldTip);
		ticks(3);
		assertEquals(1, count(Notice.WORLD_TIP));
		assertTrue(session.view().showWorldTip);
		ticks(200);
		assertEquals(1, count(Notice.WORLD_TIP));
	}

	@Test
	public void losingTheLevelStopsTheCrewAndSaysSoOnce()
	{
		twoCrewOnHooks();
		ticks(6);
		level = 85;
		AfkSession.View view = ticks(2);
		assertEquals(State.LEVEL_TOO_LOW, view.estimate.getState());
		assertEquals(87, view.levelNeeded);
		assertEquals(1, count(Notice.BOOST_DROPPED));
		ticks(50);
		assertEquals(1, count(Notice.BOOST_DROPPED));
		level = 99;
		ticks(2);
		assertEquals(State.COUNTING_DOWN, session.view().estimate.getState());
		level = 80;
		ticks(2);
		assertEquals(2, count(Notice.BOOST_DROPPED));
	}

	@Test
	public void theGhostsSilenceNearCapacityMeansTheHoldIsFull()
	{
		session.roster().setCrewmate(0, JENKINS);
		session.roster().setPosition(0, CrewAssignment.HOOK_SLOOP_1);
		session.holdCount(238, tick);
		ticks(6);
		for (int i = 0; i < AfkSession.GHOST_SILENCE_LINES; i++)
		{
			session.ghostLine(tick);
			ticks(5);
		}
		assertTrue(session.monitor().isReportedFullByGame());
		assertEquals(State.HOLD_FULL, session.view().estimate.getState());
		assertEquals(1, count(Notice.HOLD_FULL));
	}

	@Test
	public void theGhostsSilenceWithRoomLeftMeansNothing()
	{
		session.roster().setCrewmate(0, JENKINS);
		session.roster().setPosition(0, CrewAssignment.HOOK_SLOOP_1);
		ticks(6);
		for (int i = 0; i < 2 * AfkSession.GHOST_SILENCE_LINES; i++)
		{
			session.ghostLine(tick);
			ticks(5);
		}
		assertFalse(session.monitor().isReportedFullByGame());
		assertEquals(200, session.monitor().getUsed());
	}

	@Test
	public void ghostXpCountsForTheHoldWithoutAnyLine()
	{
		session.roster().setCrewmate(0, JENKINS);
		session.roster().setPosition(0, CrewAssignment.HOOK_SLOOP_1);
		ticks(6);
		session.ghostLine(tick);
		session.sailingXp(80, tick);
		ticks(4);
		assertEquals(201, session.monitor().getUsed());
		session.sailingXp(80, tick);
		ticks(4);
		assertEquals(202, session.monitor().getUsed());
		// Harvesting the extractor is not salvage.
		session.extractorUsed(tick);
		session.sailingXp(250, tick);
		ticks(4);
		assertEquals(202, session.monitor().getUsed());
	}

	@Test
	public void thePlayersOwnSalvageCountsTowardsTheHoldUntilWithdrawn()
	{
		twoCrewOnHooks();
		animation = SALVAGING;
		ticks(10);
		long etaBefore = session.view().estimate.getEtaMillis();
		session.inventoryChanged(5, 5, java.util.Collections.singleton(ShipwreckType.MERCHANT), tick);
		ticks(1);
		assertEquals(5, session.getHookedInInventory());
		assertTrue(session.view().estimate.getEtaMillis() < etaBefore);
		assertEquals(5, session.rate().lifetimeEvents());

		// Depositing them moves them into the hold.
		animation = -1;
		session.holdActionClicked(false);
		session.inventoryChanged(0, 0, java.util.Collections.emptySet(), tick);
		ticks(1);
		assertEquals(205, session.monitor().getUsed());
		assertEquals(0, session.getHookedInInventory());

		// Withdrawing to sort takes them back out and they do not count.
		session.holdActionClicked(true);
		session.inventoryChanged(28, 28, java.util.Collections.singleton(ShipwreckType.MERCHANT), tick);
		ticks(1);
		assertEquals(177, session.monitor().getUsed());
		assertEquals(0, session.getHookedInInventory());
		animation = SORTING;
		AfkSession.View view = ticks(2);
		assertEquals(PlayerActivity.SORTING, view.activity);
		assertEquals(28 * 3 * TICK - 2 * TICK, view.sortingLeftMillis, TICK);
	}

	@Test
	public void nothingIsShownOrSaidOnSomeoneElsesBoat()
	{
		twoCrewOnHooks();
		ownBoat = false;
		AfkSession.View view = ticks(60);
		assertEquals(State.NOT_SAILING, view.estimate.getState());
		assertTrue(notices.isEmpty());
		// The full-hold safety net from a crewmate's line still works there.
		session.fullLine();
		ticks(1);
		assertEquals(1, count(Notice.HOLD_FULL));
	}

	@Test
	public void movingTheBoatSuspendsTheReminder()
	{
		session.roster().setCrewmate(0, JENKINS);
		session.roster().setPosition(0, CrewAssignment.HOOK_SLOOP_1);
		session.roster().setCrewmate(1, JOLLY);
		session.settings().graceMillis = 3_000;
		for (int i = 0; i < 40; i++)
		{
			boatPoint = new WorldPoint(1799 + i, 4002, 0);
			tick();
		}
		assertEquals(0, count(Notice.HOOK_EMPTY));
		boatPoint = BOAT;
		ticks(AfkSession.PARKED_TICKS + 6);
		assertEquals(1, count(Notice.HOOK_EMPTY));
	}

	@Test
	public void walkingTheDeckIsNotMovingTheBoat()
	{
		session.roster().setCrewmate(0, JENKINS);
		session.roster().setPosition(0, CrewAssignment.HOOK_SLOOP_1);
		session.roster().setCrewmate(1, JOLLY);
		session.settings().graceMillis = 3_000;
		for (int i = 0; i < 12; i++)
		{
			playerPoint = new WorldPoint(1801, 4001 + (i % 4), 0);
			tick();
		}
		assertEquals(1, count(Notice.HOOK_EMPTY));
	}

	@Test
	public void forgettingTheLearnedRatesClearsTheStoreAndTheModel()
	{
		memory.put(ShipwreckType.BARRACUDA, "0.8000");
		memory.put(ShipwreckType.SMALL, "1.2000");
		session.forgetLearnedRates();
		assertTrue(memory.isEmpty());
	}

	@Test
	public void theLearnedRateIsRememberedPerWreck()
	{
		twoCrewOnHooks();
		ticks(10);
		for (int i = 0; i < 30; i++)
		{
			ticks(10);
			session.crewLine("Jolly Jim", tick);
			session.sailingXp(80, tick);
		}
		ticks(3);
		// 30 salvage against about 13 expected, shrunk towards the prior: roughly 1.4.
		double learned = session.rate().correction();
		assertTrue(String.valueOf(learned), learned > 1.3 && learned < 1.6);
		session.flushMemory();
		assertTrue(memory.containsKey(ShipwreckType.MERCHANT));

		// Sail to a pirate wreck and back: the pirate memory is empty, the merchant one comes back.
		session.wreckSeen(WRECK, ObjectID.SAILING_MERCHANT_SHIPWRECK_STUMP, now);
		session.wreckSeen(new WorldPoint(1806, 4006, 0), ObjectID.SAILING_PIRATE_SHIPWRECK, now);
		ticks(2);
		assertEquals(ShipwreckType.PIRATE, session.view().wreckType);
		assertEquals(1.0, session.rate().correction(), 0.02);
		session.wreckGone(new WorldPoint(1806, 4006, 0), ObjectID.SAILING_PIRATE_SHIPWRECK, now);
		session.wreckSeen(WRECK, ObjectID.SAILING_MERCHANT_SHIPWRECK, now);
		ticks(2);
		assertEquals(ShipwreckType.MERCHANT, session.view().wreckType);
		assertEquals(learned, session.rate().correction(), 0.02);
		assertFalse(session.rate().isConfident());
	}

	@Test
	public void hazardousWaterIsReportedUntilTheBoatMoves()
	{
		twoCrewOnHooks();
		ticks(6);
		session.hazardLine(boatPoint);
		assertEquals(State.HAZARDOUS, tick().estimate.getState());
		boatPoint = new WorldPoint(1810, 4002, 0);
		ticks(1);
		boatPoint = BOAT;
		assertEquals(State.COUNTING_DOWN, ticks(AfkSession.PARKED_TICKS + 1).estimate.getState());
	}

	@Test
	public void leavingTheBoatHidesEverythingAndForgetsTheFullLine()
	{
		twoCrewOnHooks();
		session.fullLine();
		ticks(2);
		assertTrue(session.monitor().isReportedFullByGame());
		sailing = false;
		AfkSession.View view = ticks(2);
		assertEquals(State.NOT_SAILING, view.estimate.getState());
		assertFalse(session.monitor().isReportedFullByGame());
	}

	@Test
	public void crewSpeechOnSomeoneElsesBoatIsNotCounted()
	{
		twoCrewOnHooks();
		ownBoat = false;
		ticks(6);
		session.crewLine("Jolly Jim", tick);
		session.sailingXp(80, tick);
		session.ghostLine(tick);
		session.sailingXp(80, tick);
		ticks(4);
		assertEquals(200, session.monitor().getUsed());
		assertEquals(0, session.rate().lifetimeEvents());
		// Back on our own boat the same speech counts.
		ownBoat = true;
		ticks(2);
		session.crewLine("Jolly Jim", tick);
		ticks(4);
		assertEquals(201, session.monitor().getUsed());
	}

	@Test
	public void hoppingWorldsKeepsTheHoldAndForgetsTheWrecks()
	{
		twoCrewOnHooks();
		ticks(10);
		assertEquals(State.COUNTING_DOWN, session.view().estimate.getState());
		session.worldHopped();
		AfkSession.View view = ticks(2);
		assertEquals(State.WAITING_FOR_WRECK, view.estimate.getState());
		assertEquals(200, session.monitor().getUsed());
		assertEquals(2, view.crewOnHooks.size());
		assertEquals(0, view.wrecksUp);
		// The new world's wreck is seen and things carry on.
		session.wreckSeen(WRECK, ObjectID.SAILING_MERCHANT_SHIPWRECK, now);
		view = ticks(AfkSession.PARKED_TICKS + 1);
		assertEquals(State.COUNTING_DOWN, view.estimate.getState());
	}

	@Test
	public void aHoldClickOnlyExplainsAnInventoryChangeInItsOwnDirection()
	{
		twoCrewOnHooks();
		ticks(6);
		session.inventoryChanged(5, 5, Collections.singleton(ShipwreckType.MERCHANT), tick);
		ticks(1);
		// Not hooked by the player, so not counted towards the hold.
		assertEquals(0, session.getHookedInInventory());

		// A withdraw click followed by the inventory shrinking is not a withdrawal.
		session.holdActionClicked(true);
		session.inventoryChanged(0, 0, Collections.emptySet(), tick);
		ticks(AfkSession.HOLD_ACTION_TICKS + 1);
		assertEquals(200, session.monitor().getUsed());

		// A deposit click the player walked away from changes nothing either.
		session.inventoryChanged(5, 5, Collections.singleton(ShipwreckType.MERCHANT), tick);
		session.holdActionClicked(false);
		session.holdActionCancelled();
		session.inventoryChanged(0, 0, Collections.emptySet(), tick);
		ticks(2);
		assertEquals(200, session.monitor().getUsed());

		// The walk to the hold takes a few ticks; the deposit is still recognised when it lands.
		session.inventoryChanged(5, 5, Collections.singleton(ShipwreckType.MERCHANT), tick);
		session.holdActionClicked(false);
		ticks(AfkSession.HOLD_ACTION_TICKS - 2);
		session.inventoryChanged(0, 0, Collections.emptySet(), tick);
		ticks(1);
		assertEquals(205, session.monitor().getUsed());
	}

	@Test
	public void aStallKeepsTheLastCountdownUp()
	{
		twoCrewOnHooks();
		ticks(10);
		long shown = session.view().countdownMillis;
		assertTrue(shown > 0);
		// Long enough with nothing arriving that the crew cannot really be salvaging. The figure
		// grew as the correction learned they were slow; now it is held rather than blanked.
		ticks(320);
		assertEquals(State.STALLED, session.view().estimate.getState());
		long held = session.view().countdownMillis;
		assertTrue(held > shown);
		ticks(20);
		assertEquals(State.STALLED, session.view().estimate.getState());
		assertEquals(held, session.view().countdownMillis);
		// The first piece to arrive ends the stall.
		session.crewLine("Jolly Jim", tick);
		ticks(3);
		assertEquals(State.COUNTING_DOWN, session.view().estimate.getState());
	}

	@Test
	public void theIdleLogoutIsWarnedOncePerIdleStretchAndOnlyAboard()
	{
		session.settings().idleWarnMillis = 60_000;
		idleLogout = 120_000;
		tick();
		assertFalse(notices.contains(Notice.IDLE_LOGOUT_SOON));
		idleLogout = 59_000;
		AfkSession.View view = tick();
		assertTrue(view.idleWarning);
		assertEquals(1, count(Notice.IDLE_LOGOUT_SOON));
		idleLogout = 30_000;
		ticks(5);
		assertEquals("said once", 1, count(Notice.IDLE_LOGOUT_SOON));
		// The player moved: the idle clock is back up, and the next stretch warns again.
		idleLogout = 300_000;
		assertFalse(tick().idleWarning);
		idleLogout = 10_000;
		tick();
		assertEquals(2, count(Notice.IDLE_LOGOUT_SOON));
		// Not on our own boat: nothing, however idle.
		ownBoat = false;
		idleLogout = 300_000;
		tick();
		idleLogout = 5_000;
		assertFalse(tick().idleWarning);
		assertEquals(2, count(Notice.IDLE_LOGOUT_SOON));
		// Switched off.
		ownBoat = true;
		session.settings().idleWarnMillis = 0;
		idleLogout = 300_000;
		tick();
		idleLogout = 5_000;
		assertFalse(tick().idleWarning);
		assertEquals(2, count(Notice.IDLE_LOGOUT_SOON));
	}

	@Test
	public void idleLogoutComesFromTheClientsIdleCounters()
	{
		assertEquals(-1, AfkSession.idleLogoutMillis(0, 10, 10));
		assertEquals(-1, AfkSession.idleLogoutMillis(-1, 10, 10));
		// 1000 client ticks of 20 ms, the keyboard idle 200 of them and the mouse 500: the keyboard counts.
		assertEquals(16_000, AfkSession.idleLogoutMillis(1000, 500, 200));
		assertEquals(0, AfkSession.idleLogoutMillis(100, 200, 300));
	}
}
