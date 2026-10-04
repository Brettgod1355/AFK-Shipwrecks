/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

import java.util.Collection;

/**
 * The one chat line said after the Plugin Hub updates the plugin. The Hub swaps the jar and
 * restarts the plugin in place, and the plugin also restarts with the client, so the only way to
 * tell an update from a restart is to remember the last version seen.
 * <p>
 * Bump {@link #VERSION} and rewrite {@link #NOTE} with every release; a test keeps the version in
 * step with the build.
 */
public final class WhatsNew
{
	/** The version being run. Must match {@code version} in build.gradle and runelite-plugin.properties. */
	public static final String VERSION = "2.0";

	/**
	 * One line on what this version brings, no links; a full stop is added when it is said. Leave
	 * it empty for a release not worth a line (a fix, a wording change) and nothing is said.
	 */
	public static final String NOTE = "a countdown to a full hold, hook and crew reminders, and a sidebar of salvaging spots "
		+ "with the world map, Shortest Path routes, favourites, auto route and the boxes that show where to park";

	private WhatsNew()
	{
	}

	/**
	 * Whether Cargo Hold Alert 1.0 was really used here: some character has a remembered cargo
	 * count, which only the plugin itself ever writes. Plugin-wide settings prove nothing, since
	 * RuneLite stores every default before the plugin starts.
	 *
	 * @param rememberedCountsPerCharacter for each character RuneLite knows, its remembered-count keys
	 */
	public static boolean usedBefore(Collection<? extends Collection<String>> rememberedCountsPerCharacter)
	{
		for (Collection<String> counts : rememberedCountsPerCharacter)
		{
			if (counts != null && !counts.isEmpty())
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * The line to say, or null when nothing changed.
	 *
	 * @param lastVersion     the version the plugin last ran as, or null when none was remembered
	 * @param installedBefore whether Cargo Hold Alert 1.0 was used here (see {@link #usedBefore}),
	 *                        which tells a fresh install (say nothing) from an update from a version
	 *                        too old to remember itself
	 */
	public static String message(String lastVersion, boolean installedBefore)
	{
		if (VERSION.equals(lastVersion) || NOTE.isEmpty())
		{
			return null;
		}
		if (lastVersion == null || lastVersion.isEmpty())
		{
			return installedBefore
				? "Cargo Hold Alert is now AFK Shipwrecks " + VERSION + ": " + NOTE + "."
				: null;
		}
		return "AFK Shipwrecks updated to " + VERSION + ": " + NOTE + ".";
	}
}
