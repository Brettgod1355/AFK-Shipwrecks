/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.cargofull;

import net.runelite.api.gameval.ObjectID;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CargoHoldCapacityTest
{
	@Test
	public void capacityGrowsWithTierAndBoatSize()
	{
		assertEquals(20, CargoHoldCapacity.forObjectId(ObjectID.SAILING_BOAT_CARGO_HOLD_REGULAR_RAFT));
		assertEquals(30, CargoHoldCapacity.forObjectId(ObjectID.SAILING_BOAT_CARGO_HOLD_REGULAR_2X5));
		assertEquals(40, CargoHoldCapacity.forObjectId(ObjectID.SAILING_BOAT_CARGO_HOLD_REGULAR_LARGE));
		assertEquals(90, CargoHoldCapacity.forObjectId(ObjectID.SAILING_BOAT_CARGO_HOLD_TEAK_LARGE));
		assertEquals(160, CargoHoldCapacity.forObjectId(ObjectID.SAILING_BOAT_CARGO_HOLD_CAMPHOR_LARGE));
		assertEquals(120, CargoHoldCapacity.forObjectId(ObjectID.SAILING_BOAT_CARGO_HOLD_ROSEWOOD_RAFT));
		assertEquals(CargoHoldCapacity.MAX_SLOTS, CargoHoldCapacity.forObjectId(ObjectID.SAILING_BOAT_CARGO_HOLD_ROSEWOOD_LARGE));
	}

	@Test
	public void everyVariantOfAHoldSharesItsCapacity()
	{
		assertEquals(45, CargoHoldCapacity.forObjectId(ObjectID.SAILING_BOAT_CARGO_HOLD_OAK_2X5));
		assertEquals(45, CargoHoldCapacity.forObjectId(ObjectID.SAILING_BOAT_CARGO_HOLD_OAK_2X5_CARGO));
		assertEquals(45, CargoHoldCapacity.forObjectId(ObjectID.SAILING_BOAT_CARGO_HOLD_OAK_2X5_NO_CARGO));
		assertTrue(CargoHoldCapacity.isCargoHold(ObjectID.SAILING_BOAT_CARGO_HOLD_IRONWOOD_RAFT_NO_CARGO));
	}

	@Test
	public void otherObjectsAreNotHolds()
	{
		assertFalse(CargoHoldCapacity.isCargoHold(ObjectID.SAILING_BOAT_HULL_KANDARIN_1X3_WOOD));
		assertEquals(CargoHoldCapacity.UNKNOWN, CargoHoldCapacity.forObjectId(ObjectID.SAILING_BOAT_HULL_KANDARIN_3X8_ROSEWOOD));
		assertEquals(CargoHoldCapacity.UNKNOWN, CargoHoldCapacity.forObjectId(-1));
	}

	@Test
	public void readsNumbersOutOfInterfaceText()
	{
		assertEquals(37, CargoHoldCapacity.firstNumber("37"));
		assertEquals(37, CargoHoldCapacity.firstNumber("37 / 40"));
		assertEquals(40, CargoHoldCapacity.lastNumber("37 / 40"));
		assertEquals(40, CargoHoldCapacity.lastNumber("40"));
		assertEquals(240, CargoHoldCapacity.lastNumber("Capacity: 12/240"));
	}

	@Test
	public void textWithoutNumbersIsUnknown()
	{
		assertEquals(CargoHoldCapacity.UNKNOWN, CargoHoldCapacity.firstNumber("Capacity"));
		assertEquals(CargoHoldCapacity.UNKNOWN, CargoHoldCapacity.lastNumber(""));
		assertEquals(CargoHoldCapacity.UNKNOWN, CargoHoldCapacity.firstNumber(null));
		assertEquals(CargoHoldCapacity.UNKNOWN, CargoHoldCapacity.lastNumber(null));
	}
}
