/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

import java.util.HashMap;
import java.util.Map;
import net.runelite.api.gameval.ObjectID;

/**
 * The salvaging hook facilities a boat can carry.
 * <p>
 * Each tier is a separate object on a raft, on a skiff, and in each of the two hook positions on a
 * sloop. A crewmate needs at least the tier's deckhandiness to operate it.
 */
public enum SalvagingHookTier
{
	BRONZE("Bronze", 15, 1, ObjectID.SALVAGING_HOOK_RAFT_BRONZE, ObjectID.SALVAGING_HOOK_BRONZE, ObjectID.SALVAGING_HOOK_LARGE_BRONZE, ObjectID.SALVAGING_HOOK_LARGE_BRONZE_B),
	IRON("Iron", 21, 1, ObjectID.SALVAGING_HOOK_RAFT_IRON, ObjectID.SALVAGING_HOOK_IRON, ObjectID.SALVAGING_HOOK_LARGE_IRON, ObjectID.SALVAGING_HOOK_LARGE_IRON_B),
	STEEL("Steel", 27, 1, ObjectID.SALVAGING_HOOK_RAFT_STEEL, ObjectID.SALVAGING_HOOK_STEEL, ObjectID.SALVAGING_HOOK_LARGE_STEEL, ObjectID.SALVAGING_HOOK_LARGE_STEEL_B),
	MITHRIL("Mithril", 44, 2, ObjectID.SALVAGING_HOOK_RAFT_MITHRIL, ObjectID.SALVAGING_HOOK_MITHRIL, ObjectID.SALVAGING_HOOK_LARGE_MITHRIL, ObjectID.SALVAGING_HOOK_LARGE_MITHRIL_B),
	ADAMANT("Adamant", 59, 2, ObjectID.SALVAGING_HOOK_RAFT_ADAMANT, ObjectID.SALVAGING_HOOK_ADAMANT, ObjectID.SALVAGING_HOOK_LARGE_ADAMANT, ObjectID.SALVAGING_HOOK_LARGE_ADAMANT_B),
	RUNE("Rune", 74, 3, ObjectID.SALVAGING_HOOK_RAFT_RUNE, ObjectID.SALVAGING_HOOK_RUNE, ObjectID.SALVAGING_HOOK_LARGE_RUNE, ObjectID.SALVAGING_HOOK_LARGE_RUNE_B),
	DRAGON("Dragon", 86, 4, ObjectID.SALVAGING_HOOK_RAFT_DRAGON, ObjectID.SALVAGING_HOOK_DRAGON, ObjectID.SALVAGING_HOOK_LARGE_DRAGON, ObjectID.SALVAGING_HOOK_LARGE_DRAGON_B);

	private static final Map<Integer, SalvagingHookTier> BY_OBJECT = new HashMap<>();

	static
	{
		for (SalvagingHookTier tier : values())
		{
			for (int id : tier.objectIds)
			{
				BY_OBJECT.put(id, tier);
			}
		}
	}

	private final String displayName;
	private final int sailingLevel;
	private final int deckhandiness;
	private final int[] objectIds;

	SalvagingHookTier(String displayName, int sailingLevel, int deckhandiness, int... objectIds)
	{
		this.displayName = displayName;
		this.sailingLevel = sailingLevel;
		this.deckhandiness = deckhandiness;
		this.objectIds = objectIds;
	}

	public String getDisplayName()
	{
		return displayName;
	}

	public int getSailingLevel()
	{
		return sailingLevel;
	}

	/** Minimum deckhandiness a crewmate needs to work this hook. */
	public int getDeckhandiness()
	{
		return deckhandiness;
	}

	/** The tier of this hook object, or null for any other object. */
	public static SalvagingHookTier fromObjectId(int objectId)
	{
		return BY_OBJECT.get(objectId);
	}

	/** Whether this hook object is the second hook position on a sloop (the "_B" variant). */
	public static boolean isSecondPosition(int objectId)
	{
		SalvagingHookTier tier = BY_OBJECT.get(objectId);
		return tier != null && tier.objectIds[3] == objectId;
	}

	public static boolean isHook(int objectId)
	{
		return BY_OBJECT.containsKey(objectId);
	}
}
