/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

import net.runelite.api.gameval.ObjectID;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class BoatFacilitiesTest
{
	private final BoatFacilities boat = new BoatFacilities();

	@Test
	public void countsHooksAndStationsOnTheFollowedBoatOnly()
	{
		boat.follow(3);
		assertTrue(boat.add(3, 100L, ObjectID.SALVAGING_HOOK_LARGE_DRAGON));
		assertTrue(boat.add(3, 101L, ObjectID.SALVAGING_HOOK_LARGE_RUNE_B));
		assertTrue(boat.add(3, 102L, ObjectID.SAILING_SALVAGING_STATION_3X8));
		assertFalse(boat.add(3, 103L, ObjectID.SAILING_BOAT_CARGO_HOLD_ROSEWOOD_LARGE));
		assertFalse(boat.add(4, 104L, ObjectID.SALVAGING_HOOK_BRONZE));

		assertEquals(2, boat.hookCount());
		assertEquals(2, boat.hooks().size());
		assertEquals(SalvagingHookTier.RUNE, boat.lowestHookTier());
		assertEquals(SalvagingHookTier.DRAGON, boat.bestHookTier());
		assertTrue(boat.hasStation());
	}

	@Test
	public void crewOnTheSecondSloopPositionGetTheSecondHook()
	{
		boat.follow(3);
		boat.add(3, 100L, ObjectID.SALVAGING_HOOK_LARGE_DRAGON);
		boat.add(3, 101L, ObjectID.SALVAGING_HOOK_LARGE_RUNE_B);
		assertEquals(SalvagingHookTier.DRAGON, boat.tierForAssignment(CrewAssignment.HOOK_SLOOP_1));
		assertEquals(SalvagingHookTier.RUNE, boat.tierForAssignment(CrewAssignment.HOOK_SLOOP_2));
		assertEquals(SalvagingHookTier.RUNE, boat.tierForAssignment(CrewAssignment.NONE));
		boat.remove(3, 101L);
		assertEquals(SalvagingHookTier.DRAGON, boat.tierForAssignment(CrewAssignment.HOOK_SLOOP_2));
		boat.follow(4);
		assertNull(boat.tierForAssignment(CrewAssignment.HOOK_SKIFF));
		boat.add(4, 200L, ObjectID.SALVAGING_HOOK_ADAMANT);
		assertEquals(SalvagingHookTier.ADAMANT, boat.tierForAssignment(CrewAssignment.HOOK_SKIFF));
		assertFalse(boat.hooks().get(0).isSecond());
	}

	@Test
	public void theSameHookSeenTwiceIsOneHook()
	{
		boat.follow(1);
		boat.add(1, 55L, ObjectID.SALVAGING_HOOK_MITHRIL);
		boat.add(1, 55L, ObjectID.SALVAGING_HOOK_MITHRIL);
		assertEquals(1, boat.hookCount());
		boat.remove(1, 55L);
		assertEquals(0, boat.hookCount());
		assertNull(boat.lowestHookTier());
	}

	@Test
	public void followingAnotherBoatForgetsTheFirst()
	{
		boat.follow(1);
		boat.add(1, 1L, ObjectID.SALVAGING_HOOK_RAFT_STEEL);
		boat.follow(1);
		assertEquals(1, boat.hookCount());
		boat.follow(2);
		assertEquals(0, boat.hookCount());
		assertEquals(2, boat.getWorldViewId());
		boat.clear();
		assertEquals(-1, boat.getWorldViewId());
	}

	@Test
	public void knowsTheBoatStations()
	{
		assertTrue(BoatFacilities.isBoatStation(ObjectID.SAILING_SALVAGING_STATION_2X5A));
		assertTrue(BoatFacilities.isBoatStation(ObjectID.SAILING_SALVAGING_STATION_2X5B));
		assertFalse(BoatFacilities.isBoatStation(ObjectID.SAILING_PORT_SALVAGING_STATION));
	}
}
