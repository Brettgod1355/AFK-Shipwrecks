/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * The things worth knowing that the sidebar shows under "Tips": how the plugin works and how to use
 * it, in short groups under a heading each (tightened 2026-10-08: the 18 loose tips overlapped and ran
 * to about thirteen screens of scrolling).
 */
public final class SalvagingTips
{
	/** One group of tips under its heading. */
	static final class Group
	{
		final String heading;
		final List<String> tips;

		Group(String heading, String... tips)
		{
			this.heading = heading;
			this.tips = Collections.unmodifiableList(Arrays.asList(tips));
		}
	}

	private static final List<Group> GROUPS = Collections.unmodifiableList(Arrays.asList(
		new Group("Parking",
			"The boxes on the water are for your hooks, not the boat: the game measures reach from the hook, so a "
				+ "hook has to sit inside a box. Yellow: a hook there reaches that wreck. Green: it reaches two at once.",
			"Park so every hook is in a green box (both on a sloop, the one on a raft or skiff). It turns the parked "
				+ "color and says \"Parked\"; with only some hooks in, it counts them.",
			"A double spot is a place, not a moment: the green box stays while its wrecks are down, since the next "
				+ "one rises on the same tile. Park and wait.",
			"Once you are parked the yellow boxes go, the green ones go 20 seconds after the boat stops, and "
				+ "\"Parked in double spot\" stays on your boat. Each of these is a setting.",
			"Reach is taken as " + AfkSession.HOOK_RANGE + " tiles, from other plugins' observations rather than the "
				+ "game's code, so an edge may be a tile off. If a hook sits in a box and only one wreck is worked, "
				+ "tell me."),
		new Group("Wrecks and timers",
			"Each wreck in view carries a timer: cyan \"Sinks in ≤\" while it is up, gray on a sunk one for when the "
				+ "next can rise (\"maybe next\" when another sunk site could take it).",
			"A wreck's clock starts only once a boat is seen beside it or your hook rolls on it. Lifetimes are "
				+ "averages, so past the clock it says \"any moment\".",
			"While you wait, \"Next wreck here in ≤\" is the earliest another wreck in the area can sink: one rises "
				+ "here the moment another sinks.",
			"Wrecks within your level get a box on the tiles they stand on, cyan while up and gray once sunk. \"Mark "
				+ "wrecks\" picks the tile or the hull outline, the area, both or neither.",
			"The plugin only knows the wrecks in the area the game has loaded, a square of about 104 tiles that "
				+ "moves as you sail. \"Show what the plugin sees\" draws it; sail toward wrecks outside it."),
		new Group("Crew and hook",
			"Your crew salvage at your Sailing level, boosted or not. If a boost lapses below the wreck's level they "
				+ "stop, and the plugin says so.",
			"Your own hook does not restart when the wreck sinks; your crew's do. The plugin reminds you to click it.",
			"If a deckhandier crewmate sits idle while a weaker one works a hook, it says who to swap; turn on "
				+ "\"Flash the swap on screen\" for a flashing SWAP CREW until you do."),
		new Group("Timer and alerts",
			"The timer and counter need one real look at the hold: open it once. Opening it again resyncs the tally.",
			"The overlay shows while a wreck site is in view (\"Show overlay\" can make it always aboard) and steps "
				+ "aside while a game interface fills the screen. \"Countdown infobox\" keeps the time in view.",
			"A minute before the idle logout you get a notification. It cannot press a key for you.",
			"Parked at a spot, you are told if the crew stop while a wreck is in reach, before a boost wears off, "
				+ "and, with a flashing red DANGER and a sound, if the boat is damaged or you are hit or poisoned.",
			"Test alert, under Tools on the Spots tab, fires the full-hold notification and banner so you can check "
				+ "your set-up."),
		new Group("Sidebar",
			"Map pans the world map while it is open; with it closed, press Map, then open the map. Route asks the "
				+ "Shortest Path plugin to draw the way and clears itself when you arrive or step off your boat.",
			"The star pins a spot to the top and to Favorites. Auto routes to one spot whenever you board from a "
				+ "dock. These and the dropdowns are kept per character.",
			"Distances are sailing distances round the land, give or take a few tiles. The dock boxes name the "
				+ "nearest dock you can use and the nearest where your crew bank the hold.",
			"Click Tools, Docks or Session to fold it away. Forget rates throws away what the timer learned about "
				+ "your crew's speed; Clear route takes the line down.",
			"Find a setting, above, searches every setting and says which section it is in. Ideas or bugs? The "
				+ "Feedback tab has GitHub and Discord."),
		new Group("Inventory sorting",
			"On your boat at a spot, items get a box: cyan deposit (bronze to dragon cannonballs by default), green "
				+ "alch (from the \"Alch from\" value), red drop (only what you mark), sage keep (if \"Box kept "
				+ "items\" is on). Anything else gets none.",
			"Shift-right-click an item to mark it, or type its name in the Sorting tab. \"Add suggested drops\" marks "
				+ "74 common junk items, after asking. Nothing is ever dropped or alched for you.",
			"With the hold open only the deposit boxes show, and the salvage in the hold gets an amber box.",
			"Your next step lights up on deck: the station (amber) while you carry salvage, then, once drop and alch "
				+ "items are gone, the hold for deposits. After that, the hold if it holds enough salvage to fill your "
				+ "free inventory, otherwise a hook (green) you could take yourself.",
			"Open the hold and take nothing out, and only the hook stays lit, unless the hold could fill your inventory. \"Hook highlight\" picks a free one, or "
				+ "the weakest crewmate's: clicking a crewed hook sends them to rest.")
	));

	private SalvagingTips()
	{
	}

	/** Every tip, in order, without the headings. */
	public static List<String> all()
	{
		List<String> all = new ArrayList<>();
		for (Group group : GROUPS)
		{
			all.addAll(group.tips);
		}
		return Collections.unmodifiableList(all);
	}

	static List<Group> groups()
	{
		return GROUPS;
	}

	/** The tips as one HTML block for a Swing label, wrapped to the given width in pixels, a heading per group. */
	public static String html(int width)
	{
		StringBuilder html = new StringBuilder("<html><body style='width:" + width + "pt'>");
		for (Group group : GROUPS)
		{
			html.append("<p style='margin-top:6px;color:#ffffff'><b>").append(group.heading).append("</b></p>")
				.append("<ul style='margin-left:12px;padding-left:0;margin-top:2px'>");
			for (String tip : group.tips)
			{
				html.append("<li style='margin-bottom:3px'>").append(tip).append("</li>");
			}
			html.append("</ul>");
		}
		return html.append("</body></html>").toString();
	}
}
