/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

import java.awt.Rectangle;
import java.awt.geom.Area;

/**
 * When and where the dimmer darkens the game (2.5): while nothing needs the player, read from the same activity, crew
 * and hold state as the AFK timer. Every case is off until the player turns it on. The idea and the first version are
 * trippy22's (AFK-Shipwrecks#20); the owner asked for it rebuilt in the plugin's own style (2026-11-01).
 */
final class Dimmer
{
	private Dimmer()
	{
	}

	/** Whether the game is dimmed now; the plugin also clears it ashore, off the own boat and over an open window. */
	static boolean dims(AfkSalvagingConfig config, AfkSession.View view, CargoHoldMonitor hold)
	{
		if (view.danger != null || view.hazardous || !view.parked)
		{
			// Never while the boat is moving (owner, 2026-11-01); it dims again once it has stood still a few ticks.
			return false;
		}
		if (config.dimUndimInventoryFull() && view.inventoryFullFromHook && view.activity != PlayerActivity.SORTING)
		{
			// Your own hook filled it: time to sort, even if the crew carry on or the hook's animation still loops.
			// Salvage taken out of the cargo hold fills it on purpose, so that does not count (owner, 2026-11-03).
			return false;
		}
		switch (view.activity)
		{
			case SALVAGING:
				return config.dimWhileSalvaging();
			case SORTING:
				return config.dimWhileSorting() && !sortingNearlyDone(config.dimSortingUndimEarly(), view.sortingLeftMillis);
			case IDLE:
				// A stalled crew clears it: the player has to nudge the boat or reassign them.
				if (config.dimWhileCrewSalvage() && view.crewSalvaging && !holdAt(hold, config.dimCrewUndimAt()))
				{
					return true;
				}
				return waiting(config, view);
			case AT_HOOK_IDLE:
				return waiting(config, view);
			default:
				return false;
		}
	}

	/** Whether any "While" switch is on; with none, the dimmer has nothing to work out. */
	static boolean anyOn(AfkSalvagingConfig config)
	{
		return config.dimWhileCrewSalvage() || config.dimWhileNoWreck() || config.dimWhileSalvaging() || config.dimWhileSorting();
	}

	/** Within the chosen seconds of sorting's estimated end; an unknown end is not "nearly done". */
	static boolean sortingNearlyDone(int earlySeconds, long leftMillis)
	{
		return earlySeconds > 0 && leftMillis >= 0 && leftMillis <= earlySeconds * 1000L;
	}

	/** At a salvaging spot with nothing up, not even a wreck above the player's level, which counts as a respawn. */
	private static boolean waiting(AfkSalvagingConfig config, AfkSession.View view)
	{
		return config.dimWhileNoWreck() && view.atSpot && view.wrecksUp == 0 && view.higherWrecksUp == 0;
	}

	/** The hold is at least this full, by the game's word or the count; an unknown size is not full. */
	static boolean holdAt(CargoHoldMonitor hold, int percent)
	{
		return hold.isConfirmedFull()
			|| (hold.hasCount() && (long) hold.getUsed() * 100 >= (long) hold.getCapacity() * percent);
	}

	/**
	 * The part of the canvas to darken, or null for none: the game view, or the whole canvas less the chat box when
	 * one is shown.
	 */
	static Area area(DimmerCovers covers, Rectangle canvas, Rectangle viewport, Rectangle chat)
	{
		if (covers == DimmerCovers.GAME_VIEW)
		{
			Rectangle view = viewport.intersection(canvas);
			return view.isEmpty() ? null : new Area(view);
		}
		Area area = new Area(canvas);
		if (covers == DimmerCovers.ALL_BUT_CHAT && chat != null)
		{
			area.subtract(new Area(chat));
		}
		return area.isEmpty() ? null : area;
	}
}
