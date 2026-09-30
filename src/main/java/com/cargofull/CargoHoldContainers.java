/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.cargofull;

import net.runelite.api.gameval.InventoryID;

/**
 * Relates the five boat slots a player can own to the item containers that hold their cargo.
 */
public final class CargoHoldContainers
{
	public static final int BOAT_SLOTS = 5;

	/** Container ids carry the top bit when the server flags them as not the player's own inventory. */
	private static final int ID_MASK = 0x7FFF;

	private CargoHoldContainers()
	{
	}

	/**
	 * @return the boat slot (1 to 5) whose cargo hold this container is, or 0 for any other container
	 */
	public static int slotFor(int containerId)
	{
		int id = containerId & ID_MASK;
		if (id < InventoryID.SAILING_BOAT_1_CARGOHOLD || id > InventoryID.SAILING_BOAT_5_CARGOHOLD)
		{
			return 0;
		}
		return id - InventoryID.SAILING_BOAT_1_CARGOHOLD + 1;
	}

	/**
	 * @param slot a boat slot from 1 to 5
	 * @return the cargo hold container id for that boat
	 */
	public static int containerFor(int slot)
	{
		if (slot < 1 || slot > BOAT_SLOTS)
		{
			throw new IllegalArgumentException("boat slot out of range: " + slot);
		}
		return InventoryID.SAILING_BOAT_1_CARGOHOLD + slot - 1;
	}
}
