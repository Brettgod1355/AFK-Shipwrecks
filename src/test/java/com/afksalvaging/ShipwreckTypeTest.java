/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.ObjectID;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ShipwreckTypeTest
{
	@Test
	public void tellsActiveWrecksFromStumps()
	{
		assertEquals(ShipwreckType.MERCHANT, ShipwreckType.fromObjectId(ObjectID.SAILING_MERCHANT_SHIPWRECK));
		assertEquals(ShipwreckType.MERCHANT, ShipwreckType.fromObjectId(ObjectID.SAILING_MERCHANT_SHIPWRECK_STUMP));
		assertTrue(ShipwreckType.isActiveWreck(ObjectID.SAILING_SMALL_SHIPWRECK));
		assertFalse(ShipwreckType.isActiveWreck(ObjectID.SAILING_SMALL_SHIPWRECK_STUMP));
		assertTrue(ShipwreckType.isStump(ObjectID.SAILING_PIRATE_SHIPWRECK_STUMP));
		assertFalse(ShipwreckType.isStump(ObjectID.SAILING_PIRATE_SHIPWRECK));
	}

	@Test
	public void ignoresOtherObjects()
	{
		assertNull(ShipwreckType.fromObjectId(ObjectID.SAILING_BOAT_CARGO_HOLD_OAK_RAFT));
		assertNull(ShipwreckType.fromObjectId(-1));
		assertFalse(ShipwreckType.isActiveWreck(ObjectID.SALVAGING_HOOK_BRONZE));
		assertFalse(ShipwreckType.isStump(0));
	}

	@Test
	public void knowsWhichSalvageComesFromWhere()
	{
		assertEquals(ShipwreckType.FREMENNIK, ShipwreckType.fromSalvageItemId(ItemID.SAILING_FREMENNIK_SHIPWRECK_SALVAGE));
		assertNull(ShipwreckType.fromSalvageItemId(ItemID.COINS));
	}

	@Test
	public void levelsAndLifetimesRiseWithTheTier()
	{
		ShipwreckType[] types = ShipwreckType.values();
		assertEquals(8, types.length);
		for (int i = 1; i < types.length; i++)
		{
			assertTrue(types[i].getSailingLevel() > types[i - 1].getSailingLevel());
			assertTrue(types[i].getLifetimeSeconds() >= types[i - 1].getLifetimeSeconds());
		}
		assertEquals(60, ShipwreckType.SMALL.getLifetimeSeconds());
		assertEquals(240, ShipwreckType.MERCHANT.getLifetimeSeconds());
		assertEquals(87, ShipwreckType.MERCHANT.getSailingLevel());
	}
}
