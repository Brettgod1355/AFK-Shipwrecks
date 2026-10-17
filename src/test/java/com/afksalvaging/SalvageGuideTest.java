/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

import com.afksalvaging.SalvageGuide.HookFacts;
import com.afksalvaging.SalvageGuide.Result;
import com.afksalvaging.SalvageGuide.Target;
import java.util.Arrays;
import java.util.Collections;
import net.runelite.api.gameval.ObjectID;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** The salvage highlight's loop, step by step (owner, 2026-10-12). */
public class SalvageGuideTest
{
	private static final HookFacts FREE_NEAR = new HookFacts(1, SalvagingHookTier.DRAGON, -1, 200);
	private static final HookFacts FREE_FAR = new HookFacts(2, SalvagingHookTier.DRAGON, -1, 900);
	private static final HookFacts JIM = new HookFacts(3, SalvagingHookTier.DRAGON, 4, 300);
	private static final HookFacts ADA = new HookFacts(4, SalvagingHookTier.DRAGON, 1, 800);

	private SalvageGuide.Inputs in;

	@Before
	public void onASloopWithAStation()
	{
		in = new SalvageGuide.Inputs();
		in.ownBoat = true;
		in.hasStation = true;
		in.wreckInReach = true;
		in.hooks = Arrays.asList(JIM, ADA);
	}

	@Test
	public void salvageCarriedLightsTheStation()
	{
		in.inventorySalvage = 5;
		Result r = SalvageGuide.decide(in);
		assertEquals(Collections.singletonList(Target.STATION), r.getTargets());
		assertFalse(r.isStrong());
	}

	@Test
	public void withTheHoldOpenOnlyTheStationShows()
	{
		in.inventorySalvage = 28;
		in.inventoryFull = true;
		Result r = SalvageGuide.decide(in).stationOnly();
		assertEquals(Collections.singletonList(Target.STATION), r.getTargets());
		assertTrue(r.isStrong());

		in.inventorySalvage = 0;
		in.inventoryFull = false;
		in.holdHasSalvage = true;
		assertFalse(SalvageGuide.decide(in).isEmpty());
		assertTrue(SalvageGuide.decide(in).stationOnly().isEmpty());

		in.inventorySalvage = 5;
		in.mode = GuideTarget.HOLD;
		assertTrue(SalvageGuide.decide(in).stationOnly().isEmpty());
	}

	@Test
	public void aFullInventoryOfSalvageIsTheStrongLook()
	{
		in.inventorySalvage = 28;
		in.inventoryFull = true;
		in.playerAtHook = true;
		Result r = SalvageGuide.decide(in);
		assertTrue(r.shows(Target.STATION));
		assertTrue(r.isStrong());
	}

	@Test
	public void stillHookingWithRoomLeftLightsNothing()
	{
		in.inventorySalvage = 3;
		in.playerAtHook = true;
		assertTrue(SalvageGuide.decide(in).isEmpty());
	}

	@Test
	public void noStationMeansTheHoldTakesTheSalvage()
	{
		// Owner: players who have not unlocked the station only have the hold.
		in.hasStation = false;
		in.inventorySalvage = 5;
		assertEquals(Collections.singletonList(Target.HOLD), SalvageGuide.decide(in).getTargets());
	}

	@Test
	public void dropBoxesComeFirstThenAlchBoxes()
	{
		in.dropBoxed = 2;
		in.alchBoxed = 1;
		in.depositBoxed = 3;
		in.holdHasSalvage = true;
		assertTrue("drop first", SalvageGuide.decide(in).isEmpty());
		in.dropBoxed = 0;
		assertTrue("then alch", SalvageGuide.decide(in).isEmpty());
		in.alchBoxed = 0;
		assertEquals("then deposit", Collections.singletonList(Target.HOLD), SalvageGuide.decide(in).getTargets());
	}

	@Test
	public void nothingLeftToDoLightsTheHoldAndTheWeakestCrewmatesHook()
	{
		in.holdHasSalvage = true;
		Result r = SalvageGuide.decide(in);
		assertEquals(Arrays.asList(Target.HOLD, Target.HOOK), r.getTargets());
		assertEquals(ADA.hash, r.getHookHash());
	}

	@Test
	public void theHoldWhenItHasAFullInventorysWorthOtherwiseTheHook()
	{
		// Owner, 2026-10-17: "perfect if it can just know how much salvage is in the hold and know how much free
		// inventory you have".
		in.holdHasSalvage = true;
		in.freeSlots = 20;
		in.holdSalvage = 20;
		assertEquals("a full withdraw waiting: the hold", Collections.singletonList(Target.HOLD), SalvageGuide.decide(in).getTargets());
		in.holdSalvage = 45;
		assertEquals(Collections.singletonList(Target.HOLD), SalvageGuide.decide(in).getTargets());
		in.holdSalvage = 19;
		Result r = SalvageGuide.decide(in);
		assertEquals("less than a full withdraw: hook it yourself", Collections.singletonList(Target.HOOK), r.getTargets());
		assertEquals(ADA.hash, r.getHookHash());
	}

	@Test
	public void bothStayLitWhenTheCountOrTheSpaceIsNotKnown()
	{
		in.holdHasSalvage = true;
		in.freeSlots = 20;
		assertEquals("count unknown", Arrays.asList(Target.HOLD, Target.HOOK), SalvageGuide.decide(in).getTargets());
		in.holdSalvage = 5;
		in.freeSlots = 0;
		assertEquals("no room to take any out", Arrays.asList(Target.HOLD, Target.HOOK), SalvageGuide.decide(in).getTargets());
	}

	@Test
	public void theCountOnlyChoosesWhenBothCouldLight()
	{
		in.holdHasSalvage = true;
		in.freeSlots = 20;
		in.holdSalvage = 3;
		in.wreckInReach = false;
		assertEquals("no wreck to hook: the hold, however little", Collections.singletonList(Target.HOLD), SalvageGuide.decide(in).getTargets());
		in.wreckInReach = true;
		in.hookChoice = GuideHook.OFF;
		assertEquals("no hook pointed at: the hold", Collections.singletonList(Target.HOLD), SalvageGuide.decide(in).getTargets());
		in.hookChoice = GuideHook.WEAKEST_CREW;
		in.holdSalvage = 50;
		in.holdTurnedDown = true;
		assertEquals("the hold just turned down: the hook, however much", Collections.singletonList(Target.HOOK), SalvageGuide.decide(in).getTargets());
	}

	@Test
	public void anEmptyHoldLeavesOnlyTheHook()
	{
		Result r = SalvageGuide.decide(in);
		assertEquals(Collections.singletonList(Target.HOOK), r.getTargets());
	}

	@Test
	public void theHoldTurnedDownLeavesOnlyTheHook()
	{
		// Owner: open the hold, close it with nothing taken, and the hook lights up.
		in.holdHasSalvage = true;
		in.holdTurnedDown = true;
		assertEquals(Collections.singletonList(Target.HOOK), SalvageGuide.decide(in).getTargets());
	}

	@Test
	public void noHookWithoutAWreckInReachOrWhileAlreadyOnOne()
	{
		in.holdHasSalvage = true;
		in.wreckInReach = false;
		assertEquals(Collections.singletonList(Target.HOLD), SalvageGuide.decide(in).getTargets());
		in.playerAtHook = true;
		assertEquals("waiting at a hook with no wreck up: the hold", Collections.singletonList(Target.HOLD),
			SalvageGuide.decide(in).getTargets());
		in.wreckInReach = true;
		// In game (2026-10-17, Original RS2, ticks 2145-2148): clicking Deploy on a hook left the hold lit until the
		// first salvage landed. Already hooking, nothing lights, however much the hold holds.
		assertTrue("hooking: nothing", SalvageGuide.decide(in).isEmpty());
		in.holdSalvage = 60;
		in.freeSlots = 18;
		assertTrue("hooking with a full withdraw waiting: still nothing", SalvageGuide.decide(in).isEmpty());
	}

	@Test
	public void keepBoxesNeverHoldAStepBack()
	{
		// Keep is not counted at all: with only kept items left, the loop carries on.
		in.holdHasSalvage = true;
		assertTrue(SalvageGuide.decide(in).shows(Target.HOLD));
	}

	@Test
	public void theStationChoiceNeverLightsTheHold()
	{
		in.mode = GuideTarget.STATION;
		in.inventorySalvage = 5;
		assertTrue(SalvageGuide.decide(in).shows(Target.STATION));
		in.hasStation = false;
		assertTrue("no station, nothing", SalvageGuide.decide(in).isEmpty());
		in.inventorySalvage = 0;
		in.depositBoxed = 1;
		assertTrue(SalvageGuide.decide(in).isEmpty());
	}

	@Test
	public void theHoldChoiceSendsSalvageToTheHoldEvenWithAStation()
	{
		in.mode = GuideTarget.HOLD;
		in.inventorySalvage = 5;
		assertEquals(Collections.singletonList(Target.HOLD), SalvageGuide.decide(in).getTargets());
	}

	@Test
	public void offOrAnotherBoatLightsNothing()
	{
		in.inventorySalvage = 5;
		in.mode = GuideTarget.OFF;
		assertTrue(SalvageGuide.decide(in).isEmpty());
		in.mode = GuideTarget.AUTOMATIC;
		in.ownBoat = false;
		assertTrue(SalvageGuide.decide(in).isEmpty());
	}

	@Test
	public void aFreeHookWinsNearestFirst()
	{
		assertEquals(FREE_NEAR, SalvageGuide.pickHook(Arrays.asList(JIM, FREE_FAR, FREE_NEAR), GuideHook.WEAKEST_CREW));
		assertEquals(FREE_NEAR, SalvageGuide.pickHook(Arrays.asList(JIM, FREE_FAR, FREE_NEAR), GuideHook.FREE_ONLY));
	}

	@Test
	public void freeHooksOnlyNeverPointsAtACrewedHook()
	{
		assertNull(SalvageGuide.pickHook(Arrays.asList(JIM, ADA), GuideHook.FREE_ONLY));
		assertNull(SalvageGuide.pickHook(Arrays.asList(FREE_NEAR), GuideHook.OFF));
	}

	@Test
	public void tiesGoToTheWeakerHookThenTheNearerOne()
	{
		HookFacts rune = new HookFacts(5, SalvagingHookTier.RUNE, 2, 900);
		HookFacts dragon = new HookFacts(6, SalvagingHookTier.DRAGON, 2, 100);
		assertEquals(rune, SalvageGuide.pickHook(Arrays.asList(dragon, rune), GuideHook.WEAKEST_CREW));
		HookFacts near = new HookFacts(7, SalvagingHookTier.DRAGON, 2, 50);
		assertEquals(near, SalvageGuide.pickHook(Arrays.asList(dragon, near), GuideHook.WEAKEST_CREW));
	}

	@Test
	public void aSloopsTwoPositionsAreItsTwoHooks()
	{
		BoatFacilities boat = new BoatFacilities();
		boat.follow(1);
		boat.add(1, 10L, ObjectID.SALVAGING_HOOK_LARGE_DRAGON);
		boat.add(1, 11L, ObjectID.SALVAGING_HOOK_LARGE_DRAGON_B);
		BoatFacilities.Hook first = null;
		BoatFacilities.Hook second = null;
		for (BoatFacilities.Hook hook : boat.hooks())
		{
			if (hook.isSecond())
			{
				second = hook;
			}
			else
			{
				first = hook;
			}
		}
		assertTrue(AfkSalvagingPlugin.worksHook(CrewAssignment.HOOK_SLOOP_1, first, 2));
		assertFalse(AfkSalvagingPlugin.worksHook(CrewAssignment.HOOK_SLOOP_1, second, 2));
		assertTrue(AfkSalvagingPlugin.worksHook(CrewAssignment.HOOK_SLOOP_2, second, 2));
		assertFalse(AfkSalvagingPlugin.worksHook(CrewAssignment.HOOK_SKIFF, first, 2));
		assertTrue(AfkSalvagingPlugin.worksHook(CrewAssignment.HOOK_SKIFF, first, 1));
	}
}
