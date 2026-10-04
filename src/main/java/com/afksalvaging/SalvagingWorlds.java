/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

/**
 * Knows which worlds count as salvaging worlds.
 * <p>
 * On the official salvaging worlds every wreck site has someone on it, so a sunk wreck near the
 * boat is replaced almost at once. Elsewhere the crew can sit idle until another player happens to
 * salvage nearby. Jagex labels the official worlds with a "Salvaging" activity in the world list,
 * and the player can add their own, for example the community's unofficial world.
 */
public final class SalvagingWorlds
{
	/** The worlds Jagex assigned to salvaging when the plugin was written. */
	public static final String DEFAULT_WORLDS = "596, 597";

	private SalvagingWorlds()
	{
	}

	/** Parses a comma, space or semicolon separated list of world numbers, ignoring anything unreadable. */
	public static Set<Integer> parse(String list)
	{
		if (list == null || list.trim().isEmpty())
		{
			return Collections.emptySet();
		}
		Set<Integer> worlds = new TreeSet<>();
		for (String part : list.split("[,;\\s]+"))
		{
			String digits = part.trim();
			if (digits.isEmpty())
			{
				continue;
			}
			try
			{
				int world = Integer.parseInt(digits);
				if (world > 0)
				{
					worlds.add(world);
				}
			}
			catch (NumberFormatException e)
			{
				// Not a world number; skip it.
			}
		}
		return worlds;
	}

	/** Whether the world list's activity text marks a world as a salvaging world. */
	public static boolean isSalvagingActivity(String activity)
	{
		return activity != null && activity.toLowerCase(Locale.ROOT).contains("salvag");
	}

	/**
	 * Whether this world should be treated as a salvaging world.
	 *
	 * @param world     the world number
	 * @param activity  the activity text from the world list, or null when it is not known
	 * @param extra     worlds the player has listed themselves
	 */
	public static boolean isSalvagingWorld(int world, String activity, Set<Integer> extra)
	{
		return isSalvagingActivity(activity) || (extra != null && extra.contains(world));
	}
}
