/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Which item types a boat's hold holds, so a deposit of a stackable item the hold already has (a
 * cannonball type, say) is known to join its stack rather than use a new slot (owner, 2026-10-08:
 * "it will understand that those are already there and they do not take up any extra slots"). Read
 * whole from the hold whenever it is opened, learned from deposits in between, and remembered per
 * boat across restarts.
 */
final class HoldItems
{
	private HoldItems()
	{
	}

	/** The ids as a remembered setting: sorted, comma separated. */
	static String encode(Collection<Integer> ids)
	{
		StringBuilder text = new StringBuilder();
		for (int id : new TreeSet<>(ids))
		{
			if (text.length() > 0)
			{
				text.append(',');
			}
			text.append(id);
		}
		return text.toString();
	}

	/** Reads {@link #encode}'s form into {@code into}; anything unreadable is skipped. */
	static void decode(String text, Set<Integer> into)
	{
		if (text == null)
		{
			return;
		}
		for (String part : text.split(","))
		{
			try
			{
				int id = Integer.parseInt(part.trim());
				if (id > 0)
				{
					into.add(id);
				}
			}
			catch (NumberFormatException e)
			{
				// A hand-edited or damaged value; the next look in the hold rewrites it.
			}
		}
	}

	/**
	 * The item types that left the inventory between two counts of it, by id: on a deposit, those are
	 * now in the hold.
	 */
	static Set<Integer> gone(Map<Integer, Integer> before, Map<Integer, Integer> after)
	{
		Set<Integer> gone = new TreeSet<>();
		for (Map.Entry<Integer, Integer> entry : before.entrySet())
		{
			if (after.getOrDefault(entry.getKey(), 0) < entry.getValue())
			{
				gone.add(entry.getKey());
			}
		}
		return gone;
	}
}
