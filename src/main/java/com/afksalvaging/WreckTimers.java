/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

import java.util.List;

/**
 * The words drawn on each wreck in view (owner, 2026-10-08: "add timers like inside the hull of
 * individual shipwrecks, the ones that we're on, as well as ones that are nearby"). A wreck that is up
 * shows how long it has left at most; a sunk one shows when the next wreck can rise in the area.
 * <p>
 * Never a number without an observed start (owner, 2026-10-03): a wreck's clock starts only once a
 * boat is seen beside it or our own hook rolls on it, and a wreck nobody has been seen working says so
 * instead of guessing.
 */
final class WreckTimers
{
	/** What to draw on one wreck, and whether it is the up colour or the sunk colour. */
	static final class Timer
	{
		final String text;
		final boolean up;

		Timer(String text, boolean up)
		{
			this.text = text;
			this.up = up;
		}
	}

	private WreckTimers()
	{
	}

	/**
	 * The timer for one site.
	 *
	 * @param site    the wreck or stump to label
	 * @param present every site in view, this one included
	 * @param now     the steady clock, milliseconds
	 */
	static Timer of(WreckTracker.Site site, List<WreckTracker.Site> present, long now)
	{
		if (site.isActive())
		{
			long by = site.sinksBy();
			if (by >= 0)
			{
				return new Timer(by > now ? "Sinks in ≤ " + Durations.countdown(by - now) : "Sinks any moment", true);
			}
			return new Timer(site.clockWrittenOff() ? "Up - outlived its clock" : "Up - no clock yet", true);
		}
		// Sunk: the area's wrecks share one pool and one rises the moment another sinks (wiki), so the
		// next rise is bounded by the earliest known sink among the wrecks up in view. With more than one
		// sunk site in view it may rise at another of them.
		long earliest = -1;
		int sunk = 0;
		for (WreckTracker.Site other : present)
		{
			if (!other.isActive())
			{
				sunk++;
				continue;
			}
			long by = other.sinksBy();
			if (by >= 0 && (earliest < 0 || by < earliest))
			{
				earliest = by;
			}
		}
		if (earliest < 0)
		{
			return new Timer("Sunk", false);
		}
		String next = sunk > 1 ? "Sunk - maybe next " : "Sunk - next ";
		return new Timer(earliest > now ? next + "≤ " + Durations.countdown(earliest - now) : next + "any moment", false);
	}
}
