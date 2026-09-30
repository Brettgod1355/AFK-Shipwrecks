/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

import com.afksalvaging.HookWatch.Reason;
import com.afksalvaging.HookWatch.Signal;
import com.afksalvaging.HookWatch.Situation;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class HookWatchTest
{
	private static final long GRACE = 15_000;
	private static final long NO_REPEAT = 0;

	private final HookWatch watch = new HookWatch();

	/** Two hooks, one crewmate on one of them, a spare crewmate aboard, a wreck up. */
	private static Situation playerLeft()
	{
		Situation s = new Situation();
		s.hookCount = 2;
		s.crewOnHooks = 1;
		s.spareCrew = 1;
		s.wreckInReach = true;
		return s;
	}

	@Test
	public void remindsOnceAfterTheGraceWhenAHookIsEmptyAndACrewmateIsSpare()
	{
		Situation fine = playerLeft();
		fine.playerAtHook = true;
		assertEquals(Signal.NONE, watch.update(0, fine, GRACE, NO_REPEAT));
		assertEquals(Signal.NONE, watch.update(1_000, playerLeft(), GRACE, NO_REPEAT));
		assertEquals(Signal.NONE, watch.update(15_999, playerLeft(), GRACE, NO_REPEAT));
		assertEquals(Signal.REMIND, watch.update(16_000, playerLeft(), GRACE, NO_REPEAT));
		assertTrue(watch.isReminding());
		assertEquals(Reason.SPARE_CREW, watch.getReason());
		assertEquals(Signal.NONE, watch.update(60_000, playerLeft(), GRACE, NO_REPEAT));
		Situation manned = playerLeft();
		manned.crewOnHooks = 2;
		manned.spareCrew = 0;
		assertEquals(Signal.NONE, watch.update(61_000, manned, GRACE, NO_REPEAT));
		assertFalse(watch.isReminding());
		assertNull(watch.getReason());
	}

	@Test
	public void steppingOffBrieflyDoesNotTriggerIt()
	{
		watch.update(0, playerLeft(), GRACE, NO_REPEAT);
		Situation back = playerLeft();
		back.playerAtHook = true;
		assertEquals(Signal.NONE, watch.update(5_000, back, GRACE, NO_REPEAT));
		assertEquals(Signal.NONE, watch.update(19_000, playerLeft(), GRACE, NO_REPEAT));
		assertEquals(Signal.REMIND, watch.update(34_000, playerLeft(), GRACE, NO_REPEAT));
	}

	@Test
	public void usingTheHoldBuysMoreTime()
	{
		watch.update(0, playerLeft(), GRACE, NO_REPEAT);
		watch.noteHoldUse();
		assertEquals(Signal.NONE, watch.update(GRACE + 9_999, playerLeft(), GRACE, NO_REPEAT));
		assertEquals(Signal.REMIND, watch.update(GRACE + 10_000, playerLeft(), GRACE, NO_REPEAT));
	}

	@Test
	public void settlingInToSortCutsTheGraceShort()
	{
		watch.update(0, playerLeft(), GRACE, NO_REPEAT);
		Situation sorting = playerLeft();
		sorting.playerSorting = true;
		watch.noteSortingStarted();
		assertEquals(Signal.NONE, watch.update(2_999, sorting, GRACE, NO_REPEAT));
		assertEquals(Signal.REMIND, watch.update(3_000, sorting, GRACE, NO_REPEAT));
	}

	@Test
	public void thePlayerIsAskedToClickWhenNoCrewmateCanTakeOver()
	{
		Situation s = playerLeft();
		s.spareCrew = 0;
		assertEquals(Signal.NONE, watch.update(0, s, GRACE, NO_REPEAT));
		assertEquals(Signal.REMIND, watch.update(GRACE, s, GRACE, NO_REPEAT));
		assertEquals(Reason.PLAYER_NEEDED, watch.getReason());
	}

	@Test
	public void thePlayerIsNotNaggedWhileSortingOrWithNoWreckUp()
	{
		Situation sorting = playerLeft();
		sorting.spareCrew = 0;
		sorting.playerSorting = true;
		assertEquals(Signal.NONE, watch.update(0, sorting, GRACE, NO_REPEAT));
		assertEquals(Signal.NONE, watch.update(100_000, sorting, GRACE, NO_REPEAT));

		Situation noWreck = playerLeft();
		noWreck.spareCrew = 0;
		noWreck.wreckInReach = false;
		assertEquals(Signal.NONE, watch.update(200_000, noWreck, GRACE, NO_REPEAT));
		assertEquals(Signal.NONE, watch.update(300_000, noWreck, GRACE, NO_REPEAT));

		// A spare crewmate should be assigned even with no wreck up: they resume by themselves.
		Situation spare = playerLeft();
		spare.wreckInReach = false;
		assertEquals(Signal.NONE, watch.update(400_000, spare, GRACE, NO_REPEAT));
		assertEquals(Signal.REMIND, watch.update(400_000 + GRACE, spare, GRACE, NO_REPEAT));
	}

	@Test
	public void nothingToRemindWithAFullHoldOrWhileMovingOrWithoutHooks()
	{
		Situation full = playerLeft();
		full.holdFull = true;
		assertEquals(Signal.NONE, watch.update(0, full, GRACE, NO_REPEAT));
		assertEquals(Signal.NONE, watch.update(100_000, full, GRACE, NO_REPEAT));

		Situation moving = playerLeft();
		moving.parked = false;
		assertEquals(Signal.NONE, watch.update(200_000, moving, GRACE, NO_REPEAT));
		assertEquals(Signal.NONE, watch.update(300_000, moving, GRACE, NO_REPEAT));

		Situation noHooks = playerLeft();
		noHooks.hookCount = 0;
		assertEquals(Signal.NONE, watch.update(400_000, noHooks, GRACE, NO_REPEAT));
		assertEquals(Signal.NONE, watch.update(500_000, noHooks, GRACE, NO_REPEAT));
	}

	@Test
	public void repeatsWhileItStaysEmpty()
	{
		long repeat = 60_000;
		watch.update(0, playerLeft(), GRACE, repeat);
		assertEquals(Signal.REMIND, watch.update(GRACE, playerLeft(), GRACE, repeat));
		assertEquals(Signal.NONE, watch.update(GRACE + 59_999, playerLeft(), GRACE, repeat));
		assertEquals(Signal.REPEAT, watch.update(GRACE + 60_000, playerLeft(), GRACE, repeat));
		assertEquals(Signal.REPEAT, watch.update(GRACE + 120_000, playerLeft(), GRACE, repeat));
	}

	@Test
	public void zeroGraceRemindsAtOnce()
	{
		assertEquals(Signal.REMIND, watch.update(5_000, playerLeft(), 0, NO_REPEAT));
	}

	@Test
	public void countsEmptyHooks()
	{
		assertEquals(0, HookWatch.emptyHooks(2, 1, true));
		assertEquals(1, HookWatch.emptyHooks(2, 1, false));
		assertEquals(2, HookWatch.emptyHooks(2, 0, false));
		assertEquals(0, HookWatch.emptyHooks(1, 2, true));
		assertEquals(0, HookWatch.emptyHooks(0, 0, false));
		watch.update(0, playerLeft(), 0, NO_REPEAT);
		watch.reset();
		assertFalse(watch.isReminding());
	}
}
