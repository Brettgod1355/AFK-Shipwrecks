/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.runelite.api.gameval.ItemID;

/**
 * Decides which box an inventory item gets. The player's own marks come first; anything unmarked
 * is sorted by what the item is: an untradeable item gets no box, an item the cargo hold accepts
 * goes to the hold (never a noted one, which the hold refuses), an item that alchs for at least the
 * threshold is alched, the rest is dropped. Pure, so it can be tested without a client.
 */
public final class SalvageSorter
{
	/** What the sorter needs to know about an item. */
	public static final class ItemFacts
	{
		public final int id;
		public final boolean noted;
		public final boolean tradeable;
		/** Whether the cargo hold accepts the unnoted item ({@link HoldWhitelist}). */
		public final boolean holdTakes;
		public final int alchValue;
		public final long gePrice;

		public ItemFacts(int id, boolean noted, boolean tradeable, boolean holdTakes, int alchValue, long gePrice)
		{
			this.id = id;
			this.noted = noted;
			this.tradeable = tradeable;
			this.holdTakes = holdTakes;
			this.alchValue = alchValue;
			this.gePrice = gePrice;
		}
	}

	/** The player's marks: an item is in at most one list. */
	public static final class Lists
	{
		private final Map<SortRule, Set<Integer>> lists = new EnumMap<>(SortRule.class);

		public Lists()
		{
			for (SortRule rule : SortRule.values())
			{
				lists.put(rule, new LinkedHashSet<>());
			}
		}

		/** The list an item is marked in, or null. */
		public SortRule markOf(int itemId)
		{
			for (SortRule rule : SortRule.values())
			{
				if (lists.get(rule).contains(itemId))
				{
					return rule;
				}
			}
			return null;
		}

		/** Marks an item, taking it out of any other list; null unmarks it. */
		public void mark(int itemId, SortRule rule)
		{
			for (Set<Integer> list : lists.values())
			{
				list.remove(itemId);
			}
			if (rule != null)
			{
				lists.get(rule).add(itemId);
			}
		}

		public List<Integer> ids(SortRule rule)
		{
			return Collections.unmodifiableList(new ArrayList<>(lists.get(rule)));
		}

		/** One list as stored: item ids joined with commas. */
		public String encode(SortRule rule)
		{
			StringBuilder out = new StringBuilder();
			for (int id : lists.get(rule))
			{
				out.append(out.length() == 0 ? "" : ",").append(id);
			}
			return out.toString();
		}

		/** Replaces one list from storage; anything unreadable is dropped rather than failing. */
		public void decode(SortRule rule, String stored)
		{
			Set<Integer> list = lists.get(rule);
			list.clear();
			if (stored == null || stored.isEmpty())
			{
				return;
			}
			for (String part : stored.split(","))
			{
				try
				{
					int id = Integer.parseInt(part.trim());
					for (Set<Integer> other : lists.values())
					{
						other.remove(id);
					}
					list.add(id);
				}
				catch (NumberFormatException ignored)
				{
					// A hand-edited setting; skip the bad entry.
				}
			}
		}
	}

	private SalvageSorter()
	{
	}

	/** Items the boxes never apply to: the unsorted salvage itself, and coins. */
	public static boolean excluded(int itemId)
	{
		return itemId == ItemID.COINS || ShipwreckType.fromSalvageItemId(itemId) != null;
	}

	/**
	 * The box for an item, or null for none (an unmarked untradeable).
	 *
	 * @param alchThreshold     high-alch value from which an unmarked item is alched
	 * @param geOverAlchPercent keep instead when the GE price beats the alch value by this much; 0 never
	 */
	public static SortRule rule(ItemFacts facts, Lists lists, int alchThreshold, int geOverAlchPercent)
	{
		SortRule marked = lists.markOf(facts.id);
		if (marked != null)
		{
			return marked;
		}
		if (!facts.tradeable)
		{
			// Untradeables are left alone unless the player marks them (owner, 2026-10-02).
			return null;
		}
		if (facts.holdTakes && !facts.noted)
		{
			return SortRule.HOLD;
		}
		if (facts.alchValue >= alchThreshold)
		{
			if (geOverAlchPercent > 0 && facts.gePrice >= facts.alchValue * (100L + geOverAlchPercent) / 100)
			{
				return SortRule.KEEP;
			}
			return SortRule.ALCH;
		}
		return SortRule.DROP;
	}
}
