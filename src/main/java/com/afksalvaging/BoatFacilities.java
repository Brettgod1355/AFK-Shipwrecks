/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.gameval.ObjectID;

/**
 * The salvaging facilities built on one boat, learned from the objects in its world view.
 * <p>
 * Objects are remembered by their hash so a hook that changes appearance (idle, in use) is still
 * one hook. Everything is forgotten when the boat's world view goes away.
 */
public final class BoatFacilities
{
	/** One salvaging hook on the boat. */
	public static final class Hook
	{
		private final long hash;
		private final SalvagingHookTier tier;
		private final boolean second;

		Hook(long hash, SalvagingHookTier tier, boolean second)
		{
			this.hash = hash;
			this.tier = tier;
			this.second = second;
		}

		public long getHash()
		{
			return hash;
		}

		public SalvagingHookTier getTier()
		{
			return tier;
		}

		/** Whether this is the second hook position on a sloop. */
		public boolean isSecond()
		{
			return second;
		}
	}

	private final Map<Long, Hook> hooks = new HashMap<>();
	private final Map<Long, Integer> stations = new HashMap<>();
	private final Map<Long, Integer> kegs = new HashMap<>();
	private final Map<Long, Integer> extractors = new HashMap<>();
	private int worldViewId = -1;

	/** Whether this object is a salvaging station built on a boat. */
	public static boolean isBoatStation(int objectId)
	{
		return objectId == ObjectID.SAILING_SALVAGING_STATION_2X5A
			|| objectId == ObjectID.SAILING_SALVAGING_STATION_2X5B
			|| objectId == ObjectID.SAILING_SALVAGING_STATION_3X8;
	}

	/** Whether this object is the crystal extractor facility, in either state. */
	public static boolean isExtractor(int objectId)
	{
		return objectId == ObjectID.SAILING_CRYSTAL_EXTRACTOR_ACTIVATED || objectId == ObjectID.SAILING_CRYSTAL_EXTRACTOR_DEACTIVATED;
	}

	/** Whether this object is a keg of whirlpool surprise, which makes every crewmate at least a level 2 deckhand. */
	public static boolean isWhirlpoolKeg(int objectId)
	{
		return objectId == ObjectID.SAILING_KEG_WHIRLPOOL_SURPRISE;
	}

	/** Starts tracking a different boat, forgetting the last one. */
	public void follow(int worldViewId)
	{
		if (this.worldViewId != worldViewId)
		{
			hooks.clear();
			stations.clear();
			kegs.clear();
			extractors.clear();
			this.worldViewId = worldViewId;
		}
	}

	public int getWorldViewId()
	{
		return worldViewId;
	}

	/**
	 * Notes an object appearing in a world view.
	 *
	 * @return true when it was a facility on the followed boat
	 */
	public boolean add(int objectWorldViewId, long objectHash, int objectId)
	{
		if (objectWorldViewId != worldViewId)
		{
			return false;
		}
		SalvagingHookTier tier = SalvagingHookTier.fromObjectId(objectId);
		if (tier != null)
		{
			hooks.put(objectHash, new Hook(objectHash, tier, SalvagingHookTier.isSecondPosition(objectId)));
			return true;
		}
		if (isBoatStation(objectId))
		{
			stations.put(objectHash, objectId);
			return true;
		}
		if (isWhirlpoolKeg(objectId))
		{
			kegs.put(objectHash, objectId);
			return true;
		}
		if (isExtractor(objectId))
		{
			extractors.put(objectHash, objectId);
			return true;
		}
		return false;
	}

	/** Notes an object leaving a world view. */
	public void remove(int objectWorldViewId, long objectHash)
	{
		if (objectWorldViewId == worldViewId)
		{
			hooks.remove(objectHash);
			stations.remove(objectHash);
			kegs.remove(objectHash);
			extractors.remove(objectHash);
		}
	}

	public boolean hasWhirlpoolKeg()
	{
		return !kegs.isEmpty();
	}

	public boolean hasExtractor()
	{
		return !extractors.isEmpty();
	}

	/** Whether this object hash is the extractor aboard. */
	public boolean isExtractorHash(long objectHash)
	{
		return extractors.containsKey(objectHash);
	}

	public int hookCount()
	{
		return hooks.size();
	}

	public List<Hook> hooks()
	{
		return new ArrayList<>(hooks.values());
	}

	/** The weakest hook aboard, or null. */
	public SalvagingHookTier lowestHookTier()
	{
		SalvagingHookTier lowest = null;
		for (Hook hook : hooks.values())
		{
			if (lowest == null || hook.tier.ordinal() < lowest.ordinal())
			{
				lowest = hook.tier;
			}
		}
		return lowest;
	}

	/** The best hook aboard, or null. */
	public SalvagingHookTier bestHookTier()
	{
		SalvagingHookTier best = null;
		for (Hook hook : hooks.values())
		{
			if (best == null || hook.tier.ordinal() > best.ordinal())
			{
				best = hook.tier;
			}
		}
		return best;
	}

	/**
	 * The hook a crewmate with this assignment is working. On a sloop the two hook positions are
	 * distinct objects, so a crewmate on the second position gets the second hook when the tiers
	 * differ; anything unclear falls back to the weakest hook, which is the cautious guess.
	 */
	public SalvagingHookTier tierForAssignment(int position)
	{
		if (hooks.isEmpty())
		{
			return null;
		}
		if (position == CrewAssignment.HOOK_SLOOP_1 || position == CrewAssignment.HOOK_SLOOP_2)
		{
			boolean wantSecond = position == CrewAssignment.HOOK_SLOOP_2;
			for (Hook hook : hooks.values())
			{
				if (hook.second == wantSecond)
				{
					return hook.tier;
				}
			}
		}
		return lowestHookTier();
	}

	public boolean hasStation()
	{
		return !stations.isEmpty();
	}

	public void clear()
	{
		hooks.clear();
		stations.clear();
		kegs.clear();
		extractors.clear();
		worldViewId = -1;
	}
}
