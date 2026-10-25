/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginManager;

/**
 * Whether the Sailing plugin draws what this plugin draws too: a count on the cargo hold and outlines on the wrecks,
 * all on by default there. A player coming from it sees both and may switch ours off, so the plugin says once which of
 * Sailing's settings to turn off to see only ours (owner, 2026-10-25). Sailing's settings are only read, never changed.
 */
public final class SailingOverlap
{
	/** The name on the Sailing plugin's descriptor, which is how RuneLite lists it. */
	static final String PLUGIN_NAME = "Sailing";
	static final String GROUP = "sailing";
	static final String COUNT = "cargoHoldShowCounts";
	static final String ACTIVE_WRECKS = "salvagingHighlightActiveWrecks";
	static final String INACTIVE_WRECKS = "salvagingHighlightInactiveWrecks";

	private SailingOverlap()
	{
	}

	/** Whether the Sailing plugin is installed and turned on. */
	static boolean sailingOn(PluginManager pluginManager)
	{
		if (pluginManager == null)
		{
			return false;
		}
		for (Plugin plugin : pluginManager.getPlugins())
		{
			if (plugin != null && PLUGIN_NAME.equals(plugin.getName()))
			{
				return pluginManager.isPluginEnabled(plugin);
			}
		}
		return false;
	}

	/** One of Sailing's switches; unset counts as on, its default for all three read here. */
	static boolean on(ConfigManager configManager, String key)
	{
		return !"false".equals(configManager.getConfiguration(GROUP, key));
	}

	/**
	 * The line to say, or null when nothing overlaps: {@code count} when both plugins write a count on the hold,
	 * {@code wrecks} when both outline the wrecks.
	 */
	static String message(boolean count, boolean wrecks)
	{
		if (count && wrecks)
		{
			return "The Sailing plugin is also showing a cargo hold count and wreck outlines. To see only AFK Shipwrecks' "
				+ "(its count also follows Deposit-all), turn these off in the Sailing plugin: Cargo Hold Tracking > Item "
				+ "Count, and Salvaging > Highlight Active Locations and Highlight Inactive Locations.";
		}
		if (count)
		{
			return "The Sailing plugin is also showing a cargo hold count. To see only AFK Shipwrecks' (it also follows "
				+ "Deposit-all), turn off Cargo Hold Tracking > Item Count in the Sailing plugin.";
		}
		if (wrecks)
		{
			return "The Sailing plugin is also outlining the wrecks. To see only AFK Shipwrecks' outlines, turn off "
				+ "Salvaging > Highlight Active Locations and Highlight Inactive Locations in the Sailing plugin.";
		}
		return null;
	}
}
