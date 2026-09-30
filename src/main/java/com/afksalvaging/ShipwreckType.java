/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.ObjectID;

/**
 * The eight kinds of shipwreck that can be salvaged, with the numbers the plugin needs about each.
 * <p>
 * A wreck is one game object while it can be salvaged and a different "stump" object once it has sunk.
 * The lifetime is how long a wreck lasts once anyone starts salvaging it, from the wiki's "average
 * duration" column; it is an average, not a guarantee. The XP figures are per piece of salvage and
 * are what let a Sailing XP drop be traced to whoever earned it.
 */
public enum ShipwreckType
{
	SMALL("Small", 15, 60, 10, 5.5, ObjectID.SAILING_SMALL_SHIPWRECK, ObjectID.SAILING_SMALL_SHIPWRECK_STUMP, ItemID.SAILING_SMALL_SHIPWRECK_SALVAGE),
	FISHERMAN("Fisherman's", 26, 180, 17, 9, ObjectID.SAILING_FISHERMAN_SHIPWRECK, ObjectID.SAILING_FISHERMAN_SHIPWRECK_STUMP, ItemID.SAILING_FISHERMAN_SHIPWRECK_SALVAGE),
	BARRACUDA("Barracuda", 35, 180, 31, 15.5, ObjectID.SAILING_BARRACUDA_SHIPWRECK, ObjectID.SAILING_BARRACUDA_SHIPWRECK_STUMP, ItemID.SAILING_BARRACUDA_SHIPWRECK_SALVAGE),
	LARGE("Large", 53, 180, 48, 24, ObjectID.SAILING_LARGE_SHIPWRECK, ObjectID.SAILING_LARGE_SHIPWRECK_STUMP, ItemID.SAILING_LARGE_SHIPWRECK_SALVAGE),
	PIRATE("Pirate", 64, 180, 76, 31.5, ObjectID.SAILING_PIRATE_SHIPWRECK, ObjectID.SAILING_PIRATE_SHIPWRECK_STUMP, ItemID.SAILING_PIRATE_SHIPWRECK_SALVAGE),
	MERCENARY("Mercenary", 73, 195, 138, 63.5, ObjectID.SAILING_MERCENARY_SHIPWRECK, ObjectID.SAILING_MERCENARY_SHIPWRECK_STUMP, ItemID.SAILING_MERCENARY_SHIPWRECK_SALVAGE),
	FREMENNIK("Fremennik", 80, 220, 162, 75, ObjectID.SAILING_FREMENNIK_SHIPWRECK, ObjectID.SAILING_FREMENNIK_SHIPWRECK_STUMP, ItemID.SAILING_FREMENNIK_SHIPWRECK_SALVAGE),
	MERCHANT("Merchant", 87, 240, 200, 95, ObjectID.SAILING_MERCHANT_SHIPWRECK, ObjectID.SAILING_MERCHANT_SHIPWRECK_STUMP, ItemID.SAILING_MERCHANT_SHIPWRECK_SALVAGE);

	private final String displayName;
	private final int sailingLevel;
	private final int lifetimeSeconds;
	private final double salvagingXp;
	private final double sortingXp;
	private final int activeObjectId;
	private final int stumpObjectId;
	private final int salvageItemId;

	ShipwreckType(String displayName, int sailingLevel, int lifetimeSeconds, double salvagingXp, double sortingXp,
		int activeObjectId, int stumpObjectId, int salvageItemId)
	{
		this.displayName = displayName;
		this.sailingLevel = sailingLevel;
		this.lifetimeSeconds = lifetimeSeconds;
		this.salvagingXp = salvagingXp;
		this.sortingXp = sortingXp;
		this.activeObjectId = activeObjectId;
		this.stumpObjectId = stumpObjectId;
		this.salvageItemId = salvageItemId;
	}

	public String getDisplayName()
	{
		return displayName;
	}

	/** Sailing level needed to salvage this wreck; crewmates use the player's level too. */
	public int getSailingLevel()
	{
		return sailingLevel;
	}

	/** How long the wreck lasts once anyone starts salvaging it, on average. */
	public int getLifetimeSeconds()
	{
		return lifetimeSeconds;
	}

	/** Sailing XP the player gets for one piece of salvage; a crewmate earns 10% of it per point of deckhandiness. */
	public double getSalvagingXp()
	{
		return salvagingXp;
	}

	/** Sailing XP for sorting one piece of this wreck's salvage. */
	public double getSortingXp()
	{
		return sortingXp;
	}

	public int getActiveObjectId()
	{
		return activeObjectId;
	}

	public int getStumpObjectId()
	{
		return stumpObjectId;
	}

	/** The unsorted salvage item this wreck gives. */
	public int getSalvageItemId()
	{
		return salvageItemId;
	}

	/** The wreck this object is, active or sunk, or null for any other object. */
	public static ShipwreckType fromObjectId(int objectId)
	{
		for (ShipwreckType type : values())
		{
			if (type.activeObjectId == objectId || type.stumpObjectId == objectId)
			{
				return type;
			}
		}
		return null;
	}

	/** Whether this object is a wreck that can currently be salvaged. */
	public static boolean isActiveWreck(int objectId)
	{
		for (ShipwreckType type : values())
		{
			if (type.activeObjectId == objectId)
			{
				return true;
			}
		}
		return false;
	}

	/** Whether this object is the stump left behind by a sunk wreck. */
	public static boolean isStump(int objectId)
	{
		for (ShipwreckType type : values())
		{
			if (type.stumpObjectId == objectId)
			{
				return true;
			}
		}
		return false;
	}

	/** The wreck whose salvage this item is, or null for any other item. */
	public static ShipwreckType fromSalvageItemId(int itemId)
	{
		for (ShipwreckType type : values())
		{
			if (type.salvageItemId == itemId)
			{
				return type;
			}
		}
		return null;
	}
}
