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
 * is sorted by what the item is: an untradeable item gets no box; a ship cannonball is deposited
 * (never a noted one, which the hold refuses); anything else the cargo hold accepts, such as repair
 * kits or fish, gets no box; an item that alchs for at least the threshold is alched (or kept, by
 * the GE rule); everything else gets no box. Drop is only ever the player's own mark. Pure, so it
 * can be tested without a client.
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
		/** Whether the unnoted item is a Deposit by default: a plain steel to dragon cannonball. */
		public final boolean defaultDeposit;
		public final int alchValue;
		public final long gePrice;

		public ItemFacts(int id, boolean noted, boolean tradeable, boolean holdTakes, boolean defaultDeposit,
			int alchValue, long gePrice)
		{
			this.id = id;
			this.noted = noted;
			this.tradeable = tradeable;
			this.holdTakes = holdTakes;
			this.defaultDeposit = defaultDeposit;
			this.alchValue = alchValue;
			this.gePrice = gePrice;
		}
	}

	/** The player's marks: an item is in at most one list. */
	public static final class Lists
	{
		private final Map<SortRule, Set<Integer>> lists = new EnumMap<>(SortRule.class);
		/** Items the player took out of the defaults: no box, whatever they are. */
		private final Set<Integer> excluded = new LinkedHashSet<>();

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

		/** Marks an item, taking it out of any other list and off the excluded set; null unmarks it. */
		public void mark(int itemId, SortRule rule)
		{
			for (Set<Integer> list : lists.values())
			{
				list.remove(itemId);
			}
			excluded.remove(itemId);
			if (rule != null)
			{
				lists.get(rule).add(itemId);
			}
		}

		/**
		 * Takes an item out of the defaults: it gets no box until marked again. For the ship
		 * cannonballs, which are Deposit by default and listed with an × like a mark (owner,
		 * 2026-10-03: "so the default options look the same as manual options").
		 */
		public void exclude(int itemId)
		{
			mark(itemId, null);
			excluded.add(itemId);
		}

		public boolean isExcluded(int itemId)
		{
			return excluded.contains(itemId);
		}

		public List<Integer> ids(SortRule rule)
		{
			return Collections.unmodifiableList(new ArrayList<>(lists.get(rule)));
		}

		public List<Integer> excludedIds()
		{
			return Collections.unmodifiableList(new ArrayList<>(excluded));
		}

		/** Adds items to one list, leaving alone any already marked or excluded: the first mark read wins. */
		public void add(SortRule rule, List<Integer> ids)
		{
			for (int id : ids)
			{
				if (markOf(id) == null && !excluded.contains(id))
				{
					lists.get(rule).add(id);
				}
			}
		}

		/** Adds items to the excluded set, leaving alone any already marked: the first mark read wins. */
		public void addExcluded(List<Integer> ids)
		{
			for (int id : ids)
			{
				if (markOf(id) == null)
				{
					excluded.add(id);
				}
			}
		}

		/** Becomes a copy of another set of lists. */
		public void replaceWith(Lists other)
		{
			for (SortRule rule : SortRule.values())
			{
				lists.get(rule).clear();
				lists.get(rule).addAll(other.lists.get(rule));
			}
			excluded.clear();
			excluded.addAll(other.excluded);
		}

		/** Item ids as stored: joined with commas. */
		public static String join(List<Integer> ids)
		{
			StringBuilder out = new StringBuilder();
			for (int id : ids)
			{
				out.append(out.length() == 0 ? "" : ",").append(id);
			}
			return out.toString();
		}

		/** Item ids from storage; anything unreadable is dropped rather than failing. */
		public static List<Integer> parse(String stored)
		{
			List<Integer> ids = new ArrayList<>();
			if (stored == null || stored.isEmpty())
			{
				return ids;
			}
			for (String part : stored.split(","))
			{
				try
				{
					int id = Integer.parseInt(part.trim());
					if (!ids.contains(id))
					{
						ids.add(id);
					}
				}
				catch (NumberFormatException ignored)
				{
					// A hand-edited setting; skip the bad entry.
				}
			}
			return ids;
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
	 * The box for an item, or null for none: an unmarked item that is not a cannonball and is not
	 * worth alching, or is untradeable, or is something else the hold takes.
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
		if (lists.isExcluded(facts.id))
		{
			// Taken out of the defaults with the × in the sidebar.
			return null;
		}
		if (!facts.tradeable)
		{
			// Untradeables are left alone unless the player marks them (owner, 2026-10-02).
			return null;
		}
		if (facts.defaultDeposit && !facts.noted)
		{
			// Only the plain steel to dragon cannonballs are a deposit by default (owner, 2026-10-02, trimmed 2026-10-03).
			return SortRule.HOLD;
		}
		if (facts.holdTakes)
		{
			// Repair kits, fish, drinks: never a drop, and only a deposit if the player marks them.
			return null;
		}
		if (facts.alchValue >= alchThreshold)
		{
			if (geOverAlchPercent > 0 && facts.gePrice >= facts.alchValue * (100L + geOverAlchPercent) / 100)
			{
				return SortRule.KEEP;
			}
			return SortRule.ALCH;
		}
		// Nothing is a drop by default; only what the player marks (owner, 2026-10-02).
		return null;
	}
}
