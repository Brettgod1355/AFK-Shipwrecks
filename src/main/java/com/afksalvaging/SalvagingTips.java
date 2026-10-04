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
			+ "reaches two wrecks at once. Wrecks that are up and within your level also get a soft cyan outline round "
			+ "the hull, like the game's own hover outline (a setting).",
		"Park so every hook on your boat is inside a green box: both on a sloop, the one on a raft or skiff. "
			+ "The box changes to the parked colour and says \"Parked\" when they all are; with only some in, the "
			+ "label counts them. Once parked, the yellow boxes go, to clear the view (a setting, on by default), and "
			+ "the green ones go 20 seconds after the boat stops, at login too, coming back the moment it moves (a "
			+ "setting; 0 keeps them). While you stay parked, \"Parked in double spot\" stays in the middle of your boat (a "
			+ "setting, on by default).",
		"Reach is taken as " + AfkSession.HOOK_RANGE + " tiles, from other plugins' observations rather than the "
			+ "game's code, so a box edge may be a tile off. If a hook sits in a box and only one wreck is worked, "
			+ "report it and the number gets fixed.",
		"While you wait for a wreck, \"Next wreck here in ≤\" is the earliest another wreck in the area can sink, "
			+ "since one rises here the moment another sinks. A wreck's clock starts when a boat is first seen beside "
			+ "it (or your hook rolls on it); lifetimes are averages, so past the clock it says \"any moment\" and soon "
			+ "gives up on that wreck until it is seen worked again.",
		"A double spot is a place, not a moment: the green box shows even while one or both of its wrecks are down, "
			+ "because the next wreck rises on the same tile, so you can park and wait. Sunk wrecks' yellow boxes are "
			+ "dimmer.",
		"Map pans the world map while it is open. With it closed, press Map, then open the map and it jumps there.",
		"Route asks the Shortest Path plugin to draw the way. The line above the list says if it is missing or "
			+ "switched off. A route to a spot, sent by Route or by Auto, is cleared for you once you pull up to one "
			+ "of its wrecks or step off your boat. Shortest Path does not tell plugins when you set a target of your "
			+ "own, so if you replace the plugin's route yourself, that one is cleared then instead.",
		"The star pins a spot to the top of every list and to the Favourites filter. Auto marks one spot to be "
			+ "routed to by itself whenever you board your boat from a dock. Favourites, the Auto spot and the "
			+ "dropdowns are kept per character, under the account's own id, so a name change keeps them.",
		"The distances are sailing distances: the way round the land by sea, worked out over a map of the water "
			+ "taken from the game's own map data, give or take a few tiles. They show once you are on the water or "
			+ "standing at a dock, and Sort: nearest first uses them too. The dock boxes at the top follow you "
			+ "about: the nearest dock you can use, and the nearest dock where your crew bank the hold as you step "
			+ "off (one with a bank deposit box, or the bank boat, which you bank at from your deck), with what a "
			+ "nearer one needs. Afloat, those are sailing distances as well; ashore, a straight line, since you "
			+ "walk to a dock. The dropdowns are remembered between sessions.",
		"Test alert fires the full-hold notification and banner so you can check your set-up without filling a "
			+ "hold. Forget rates throws away what the timer learned about your crew's speed on each wreck.",
		"The timer and counter need one real look at the hold: open the cargo hold once and they follow from there. "
			+ "Opening it again at any time resyncs the tally.",
		"Your crew salvage at your Sailing level, boosted or not. If a boost lapses below the wreck's level they "
			+ "stop, and the plugin says so.",
		"Your own hook does not restart when the wreck sinks; your crew do. The plugin reminds you to click it. If a "
			+ "deckhandier crewmate sits idle while a weaker one works a hook, it says who to swap, once, with its own "
			+ "notification under Reminders, and a flashing SWAP CREW notice sits mid-screen (with a Swap line on the "
			+ "overlay) until you do.",
		"A minute before the game logs you out for idling, while you are on your own boat, the plugin sends a "
			+ "notification (Reminders settings: how long before, or never). It cannot press a key for you.",
		"\"Countdown infobox\" in the Overlay settings puts the time to a full hold in a small box among RuneLite's "
			+ "infoboxes, with the detail on hover, for when the overlay is hidden.",
		"Inventory sorting boxes each item on your boat at a salvaging spot (with the hold open, only the deposits, "
			+ "in the panel beside it): sage keep (if \"Box kept items\" is on), cyan deposit (ship "
			+ "bronze to dragon cannonballs by default, each listed in the Sorting tab with an × that takes it out), green "
			+ "alch (at or "
			+ "above the \"Alch from\" value), red drop (only what you mark; \"Add suggested drops\" in the Sorting "
			+ "tab marks 74 common ones for you, after asking). "
			+ "Anything else gets no box until you mark it. Shift-right-click an item to mark it, or type its name in the "
			+ "Sorting tab. Nothing is dropped or alched for you. Inside the open hold, the salvage stacks get an amber "
			+ "box so they are easy to find and click (a setting, on by default).",
		"The overlay only appears while a wreck site is in view. \"Show overlay\" in the Overlay settings can make it "
			+ "show whenever you are aboard. It steps aside while any game interface fills the middle of the screen, the "
			+ "cargo hold and the world map included, and comes back when that closes."
	));

	private SalvagingTips()
	{
	}

	public static List<String> all()
	{
		return TIPS;
	}

	/** The tips as one HTML block for a Swing label, wrapped to the given width in pixels. */
	public static String html(int width)
	{
		StringBuilder html = new StringBuilder("<html><body style='width:" + width + "pt'><ul style='margin-left:12px;padding-left:0'>");
		for (String tip : TIPS)
		{
			html.append("<li style='margin-bottom:4px'>").append(tip).append("</li>");
		}
		return html.append("</ul></body></html>").toString();
	}
}
