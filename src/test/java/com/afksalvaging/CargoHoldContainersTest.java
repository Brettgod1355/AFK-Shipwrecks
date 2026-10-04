/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

import net.runelite.api.gameval.InventoryID;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CargoHoldContainersTest
{
	@Test
	public void mapsEachBoatToItsHold()
	{
		assertEquals(1, CargoHoldContainers.slotFor(InventoryID.SAILING_BOAT_1_CARGOHOLD));
		assertEquals(3, CargoHoldContainers.slotFor(InventoryID.SAILING_BOAT_3_CARGOHOLD));
		assertEquals(5, CargoHoldContainers.slotFor(InventoryID.SAILING_BOAT_5_CARGOHOLD));
		assertEquals(InventoryID.SAILING_BOAT_2_CARGOHOLD, CargoHoldContainers.containerFor(2));
	}

	@Test
	public void ignoresOtherContainers()
	{
		assertEquals(0, CargoHoldContainers.slotFor(InventoryID.INV));
		assertEquals(0, CargoHoldContainers.slotFor(InventoryID.SAILING_TRAWLING_NET));
		assertEquals(0, CargoHoldContainers.slotFor(InventoryID.SAILING_BOAT_1_CARGOHOLD - 1));
	}

	@Test
	public void acceptsTheFlaggedFormOfTheId()
	{
		assertEquals(4, CargoHoldContainers.slotFor(InventoryID.SAILING_BOAT_4_CARGOHOLD | 0x8000));
	}

	@Test(expected = IllegalArgumentException.class)
	public void rejectsBoatSlotsThatDoNotExist()
	{
		CargoHoldContainers.containerFor(6);
	}
}
