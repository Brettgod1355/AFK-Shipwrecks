/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

import com.afksalvaging.AfkEstimate.State;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AfkEstimatorTest
{
	/** Two good crewmates: one salvage per 7.5 s between them, so 0.08 per tick. */
	private static final double TWO_CREW = 0.08;
	/** The player: one salvage per 4.8 s, so 0.125 per tick. */
	private static final double PLAYER = 0.125;
	private static final long LONG_WINDOW = 3_600_000;

	private static AfkEstimator.Inputs afkWithTwoCrew()
	{
		AfkEstimator.Inputs in = new AfkEstimator.Inputs();
		in.sailing = true;
		in.hookCount = 2;
		in.holdKnown = true;
		in.holdRemaining = 100;
		in.crewExpectedPerTick = TWO_CREW;
		in.wreckInReach = true;
		in.wreckWindowMillis = LONG_WINDOW;
		in.availability = 1;
		in.correction = 1;
		in.confident = true;
		in.freeInventorySlots = 28;
		return in;
	}

	@Test
	public void statesComeInOrderOfWhatIsWrongFirst()
	{
		AfkEstimator.Inputs in = afkWithTwoCrew();
		in.sailing = false;
		assertEquals(State.NOT_SAILING, AfkEstimator.estimate(in).getState());

		in = afkWithTwoCrew();
		in.hookCount = 0;
		assertEquals(State.NO_HOOK, AfkEstimator.estimate(in).getState());

		in = afkWithTwoCrew();
		in.holdKnown = false;
		assertEquals(State.HOLD_UNKNOWN, AfkEstimator.estimate(in).getState());

		in = afkWithTwoCrew();
		in.holdFull = true;
		assertEquals(State.HOLD_FULL, AfkEstimator.estimate(in).getState());
		in.holdFull = false;
		in.holdRemaining = 0;
		assertEquals(State.HOLD_FULL_UNCONFIRMED, AfkEstimator.estimate(in).getState());
		in.holdRemaining = 5;
		in.holdFullUnconfirmed = true;
		assertEquals(State.HOLD_FULL_UNCONFIRMED, AfkEstimator.estimate(in).getState());

		in = afkWithTwoCrew();
		in.crewExpectedPerTick = 0;
		assertEquals(State.NOBODY_SALVAGING, AfkEstimator.estimate(in).getState());

		in = afkWithTwoCrew();
		in.hazardous = true;
		assertEquals(State.HAZARDOUS, AfkEstimator.estimate(in).getState());

		in = afkWithTwoCrew();
		in.wreckInReach = false;
		in.onlyHigherWrecksInReach = true;
		assertEquals(State.LEVEL_TOO_LOW, AfkEstimator.estimate(in).getState());

		in = afkWithTwoCrew();
		in.wreckInReach = false;
		in.expectedWaitMillis = 90_000;
		AfkEstimate waiting = AfkEstimator.estimate(in);
		assertEquals(State.WAITING_FOR_WRECK, waiting.getState());
		assertEquals(90_000, waiting.getEtaMillis());
		// The paused states still say how much salvaging is left once a wreck is up.
		assertEquals(750_000, waiting.getWorkMillis());
		in.expectedWaitMillis = -1;
		assertFalse(AfkEstimator.estimate(in).hasEta());
		assertTrue(AfkEstimator.estimate(in).hasWork());

		in = afkWithTwoCrew();
		in.stalled = true;
		assertEquals(State.STALLED, AfkEstimator.estimate(in).getState());
		assertEquals(750_000, AfkEstimator.estimate(in).getWorkMillis());
		assertFalse(AfkEstimate.of(State.NO_HOOK).hasWork());
	}

	@Test
	public void twoCrewFillTheHoldAtTheirCombinedRate()
	{
		AfkEstimate estimate = AfkEstimator.estimate(afkWithTwoCrew());
		assertEquals(State.COUNTING_DOWN, estimate.getState());
		// 0.08 per tick = 0.1333 per second. 100 slots: 750 s.
		assertEquals(750_000, estimate.getEtaMillis());
		assertEquals(480, estimate.getHoldPerHour(), 1e-9);
		assertFalse(estimate.isApproximate());
	}

	@Test
	public void theCorrectionScalesTheRate()
	{
		AfkEstimator.Inputs in = afkWithTwoCrew();
		in.correction = 0.5;
		in.confident = false;
		AfkEstimate estimate = AfkEstimator.estimate(in);
		assertEquals(1_500_000, estimate.getEtaMillis());
		assertTrue(estimate.isApproximate());
		in.correction = 0;
		assertEquals(750_000, AfkEstimator.estimate(in).getEtaMillis());
	}

	@Test
	public void downtimeOnlyAppliesAfterTheCurrentWreckSinks()
	{
		AfkEstimator.Inputs in = afkWithTwoCrew();
		in.availability = 0.5;
		// The wreck is up for another 300 s, in which the crew do 40 slots at full rate; the other
		// 60 take twice as long as they would: 900 s. Total 1200 s.
		in.wreckWindowMillis = 300_000;
		AfkEstimate estimate = AfkEstimator.estimate(in);
		assertEquals(1_200_000, estimate.getEtaMillis());
		assertEquals(750_000, estimate.getWorkMillis());
		// If the hold fills before the wreck sinks, downtime does not matter at all.
		in.holdRemaining = 30;
		assertEquals(225_000, AfkEstimator.estimate(in).getEtaMillis());
		// No wreck window at all: everything is at the reduced rate.
		in.holdRemaining = 100;
		in.wreckWindowMillis = 0;
		assertEquals(1_500_000, AfkEstimator.estimate(in).getEtaMillis());
		assertEquals(750_000, AfkEstimator.estimate(in).getWorkMillis());
	}

	@Test
	public void salvageAlreadyInTheInventoryCountsTowardsTheHold()
	{
		AfkEstimator.Inputs in = afkWithTwoCrew();
		in.inventorySalvage = 25;
		assertEquals(562_500, AfkEstimator.estimate(in).getEtaMillis());
		in.countInventorySalvage = false;
		assertEquals(750_000, AfkEstimator.estimate(in).getEtaMillis());
		in.countInventorySalvage = true;
		in.inventorySalvage = 100;
		AfkEstimate estimate = AfkEstimator.estimate(in);
		assertEquals(State.COUNTING_DOWN, estimate.getState());
		assertEquals(0, estimate.getEtaMillis());
	}

	@Test
	public void thePlayerOnAHookSpeedsThingsUpUntilTheirInventoryFills()
	{
		AfkEstimator.Inputs in = afkWithTwoCrew();
		in.playerAtHook = true;
		in.playerRolling = true;
		in.playerExpectedPerTick = PLAYER;
		in.freeInventorySlots = 28;
		// Player 0.2083/s, crew 0.1333/s, together 0.3417/s. Inventory fills after 134.4 s, by
		// which time 45.9 slots are covered; the crew do the other 54.1 at 0.1333/s = 405.8 s.
		AfkEstimate estimate = AfkEstimator.estimate(in);
		assertEquals(State.COUNTING_DOWN, estimate.getState());
		assertEquals(540_200, estimate.getEtaMillis(), 600);

		// With a small hold left the inventory never fills first.
		in.holdRemaining = 10;
		assertEquals(Math.round(10 / (0.08 / 0.6 + 0.125 / 0.6) * 1000), AfkEstimator.estimate(in).getEtaMillis(), 2);

		// Standing at the hook without rolling adds nothing.
		in.playerRolling = false;
		in.holdRemaining = 100;
		assertEquals(750_000, AfkEstimator.estimate(in).getEtaMillis());
	}

	@Test
	public void thePlayerStopsWhenTheWreckSinks()
	{
		AfkEstimator.Inputs in = afkWithTwoCrew();
		in.playerAtHook = true;
		in.playerRolling = true;
		in.playerExpectedPerTick = PLAYER;
		in.wreckWindowMillis = 60_000;
		in.availability = 1;
		// In 60 s: crew 8 slots, player 12.5 slots. The other 79.5 slots take the crew 596 s. Total 656 s.
		assertEquals(656_250, AfkEstimator.estimate(in).getEtaMillis(), 2);
	}

	@Test
	public void thePlayerAloneIsLimitedByInventoryOrTheWreck()
	{
		AfkEstimator.Inputs in = afkWithTwoCrew();
		in.crewExpectedPerTick = 0;
		in.playerAtHook = true;
		in.playerRolling = true;
		in.playerExpectedPerTick = PLAYER;
		in.freeInventorySlots = 10;
		in.holdRemaining = 100;
		AfkEstimate estimate = AfkEstimator.estimate(in);
		assertEquals(State.INVENTORY_FILLS_FIRST, estimate.getState());
		assertEquals(48_000, estimate.getEtaMillis());

		in.wreckWindowMillis = 30_000;
		estimate = AfkEstimator.estimate(in);
		assertEquals(State.WRECK_SINKS_FIRST, estimate.getState());
		assertEquals(30_000, estimate.getEtaMillis());

		in.wreckWindowMillis = LONG_WINDOW;
		in.holdRemaining = 8;
		estimate = AfkEstimator.estimate(in);
		assertEquals(State.COUNTING_DOWN, estimate.getState());
		assertEquals(38_400, estimate.getEtaMillis());
	}

	@Test
	public void crewWithNoChanceOfAnotherWreckLeaveThePlayerWaiting()
	{
		AfkEstimator.Inputs in = afkWithTwoCrew();
		in.availability = 0;
		in.wreckWindowMillis = 60_000;
		in.expectedWaitMillis = -1;
		AfkEstimate estimate = AfkEstimator.estimate(in);
		assertEquals(State.WAITING_FOR_WRECK, estimate.getState());
		assertFalse(estimate.hasEta());
	}

	@Test
	public void noEtaIsReportedAsNoEta()
	{
		assertFalse(AfkEstimate.of(State.NO_HOOK).hasEta());
		assertEquals(-1, AfkEstimate.of(State.NO_HOOK).getEtaMillis());
		assertTrue(AfkEstimator.estimate(afkWithTwoCrew()).hasEta());
		assertEquals("COUNTING_DOWN 12:30", AfkEstimator.estimate(afkWithTwoCrew()).toString());
	}
}
