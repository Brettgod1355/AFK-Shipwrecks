/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

import java.util.Collections;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.events.PluginMessage;

/**
 * Builds the messages the Shortest Path plugin listens for on RuneLite's event bus.
 * <p>
 * This is the only link between the two plugins: no compile dependency, no reflection. If Shortest
 * Path is not installed the messages go nowhere and nothing breaks. The contract was read from
 * Skretzo/shortest-path {@code ShortestPathPlugin.onPluginMessage} on 2026-10-01: namespace
 * {@code shortestpath}; name {@code path} with a {@code target} that is a WorldPoint, a packed
 * point or a set of either, and an optional {@code start} defaulting to the player; name
 * {@code clear} to remove the path.
 */
public final class ShortestPathMessages
{
	public static final String NAMESPACE = "shortestpath";
	public static final String PATH = "path";
	public static final String CLEAR = "clear";
	public static final String TARGET = "target";

	private ShortestPathMessages()
	{
	}

	/** Asks Shortest Path to draw a route from the player to this point. */
	public static PluginMessage routeTo(WorldPoint target)
	{
		return new PluginMessage(NAMESPACE, PATH, Collections.singletonMap(TARGET, target));
	}

	/** Asks Shortest Path to take its route down. */
	public static PluginMessage clearRoute()
	{
		return new PluginMessage(NAMESPACE, CLEAR);
	}
}
