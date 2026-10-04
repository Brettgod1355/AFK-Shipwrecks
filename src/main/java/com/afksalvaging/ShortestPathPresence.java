/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginManager;

/**
 * Whether the Shortest Path plugin is there to receive a route.
 * <p>
 * Routes travel over the event bus, which has no reply, so the only way to tell the player why
 * nothing happened is to look at the client's plugin list first. That list is RuneLite's own;
 * nothing of Shortest Path is touched, and the plugin is found by the name on its descriptor.
 */
public enum ShortestPathPresence
{
	/** Installed and turned on. */
	READY,
	/** Installed but turned off in the plugin list. */
	DISABLED,
	/** Not installed. */
	MISSING;

	/** The name on Shortest Path's plugin descriptor, which is how RuneLite lists it. */
	public static final String PLUGIN_NAME = "Shortest Path";

	public static ShortestPathPresence check(PluginManager pluginManager)
	{
		if (pluginManager == null)
		{
			return MISSING;
		}
		for (Plugin plugin : pluginManager.getPlugins())
		{
			if (plugin != null && PLUGIN_NAME.equals(plugin.getName()))
			{
				return pluginManager.isPluginEnabled(plugin) ? READY : DISABLED;
			}
		}
		return MISSING;
	}

	/** What to tell the player, or null when a route can be sent. */
	public String problem()
	{
		switch (this)
		{
			case DISABLED:
				return "Shortest Path is installed but turned off. Turn it on in the plugin list to use Route.";
			case MISSING:
				return "Route needs the Shortest Path plugin. Install it from the Plugin Hub (search \"Shortest Path\").";
			default:
				return null;
		}
	}
}
