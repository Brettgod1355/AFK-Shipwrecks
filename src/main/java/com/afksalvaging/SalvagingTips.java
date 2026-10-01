/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** The things worth knowing that the sidebar shows under "Tips": how the plugin works and how to use it. */
public final class SalvagingTips
{
	private static final List<String> TIPS = Collections.unmodifiableList(Arrays.asList(
		"The boxes on the water are for your hooks, not the whole boat. The game measures salvage reach from the "
			+ "hook, so a hook has to sit inside a box. Yellow: a hook there reaches that wreck. Green: a hook there "
			+ "reaches two wrecks at once.",
		"Park so every hook on your boat is inside a green box: both on a sloop, the one on a raft or skiff. "
			+ "The box changes to the parked colour and says \"Parked\" when they all are; with only some in, the "
			+ "label counts them.",
		"Reach is taken as " + AfkSession.HOOK_RANGE + " tiles, from other plugins' observations rather than the "
			+ "game's code, so a box edge may be a tile off. If a hook sits in a box and only one wreck is worked, "
			+ "report it and the number gets fixed.",
		"Sunk wrecks keep a dimmer box because the next wreck rises on the same tile; with \"All sites\" the green "
			+ "boxes show for them too, so you can park before it does.",
		"Map pans the world map while it is open. With it closed, press Map, then open the map and it jumps there.",
		"Route asks the Shortest Path plugin to draw the way. The line above the list says if it is missing or "
			+ "switched off.",
		"The timer and counter need one real look at the hold: open the cargo hold once and they follow from there. "
			+ "Opening it again at any time resyncs the tally.",
		"Your crew salvage at your Sailing level, boosted or not. If a boost lapses below the wreck's level they "
			+ "stop, and the plugin says so.",
		"Your own hook does not restart when the wreck sinks; your crew do. The plugin reminds you to click it.",
		"The overlay only appears while a wreck site is in view. \"Show overlay\" in the Overlay settings can make it "
			+ "show whenever you are aboard."
	));

	private SalvagingTips()
	{
	}

	public static List<String> all()
	{
		return TIPS;
	}

	/** The tips as one HTML block for a Swing label. */
	public static String html()
	{
		StringBuilder html = new StringBuilder("<html><ul style='margin-left:12px;padding-left:0'>");
		for (String tip : TIPS)
		{
			html.append("<li style='margin-bottom:4px'>").append(tip).append("</li>");
		}
		return html.append("</ul></html>").toString();
	}
}
