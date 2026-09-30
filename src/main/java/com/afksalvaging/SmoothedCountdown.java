/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

/**
 * A countdown that glides towards new estimates instead of jumping.
 * <p>
 * The estimate behind the timer moves every time salvage arrives. Shown raw it would lurch by
 * minutes; instead the displayed time runs down at one second per second and closes a quarter of
 * the gap to the latest estimate on every update. When the estimate is frozen (nobody can salvage
 * right now) the display holds still. Times are milliseconds.
 */
public final class SmoothedCountdown
{
	/** Share of the gap to the fresh estimate closed on each update. */
	public static final double CATCH_UP = 0.25;
	/** A gap bigger than this is taken at once; the estimate has changed for a real reason. */
	public static final long SNAP_MILLIS = 15 * 60_000L;

	private long remaining = -1;
	private long updatedAt = Long.MIN_VALUE;

	/**
	 * Feeds the latest estimate.
	 *
	 * @param estimateMillis the fresh remaining time, or -1 for none
	 * @param running        whether time is passing against the countdown right now
	 * @return the remaining time to show, or -1
	 */
	public long update(long estimateMillis, boolean running, long now)
	{
		if (estimateMillis < 0)
		{
			remaining = -1;
			updatedAt = now;
			return -1;
		}
		if (remaining < 0 || updatedAt == Long.MIN_VALUE || Math.abs(estimateMillis - remaining) > SNAP_MILLIS)
		{
			remaining = estimateMillis;
			updatedAt = now;
			return remaining;
		}
		if (running && now > updatedAt)
		{
			remaining = Math.max(0, remaining - (now - updatedAt));
		}
		updatedAt = now;
		remaining = Math.max(0, Math.round(remaining + (estimateMillis - remaining) * CATCH_UP));
		return remaining;
	}

	/** The remaining time as of the last update, or -1. */
	public long remaining()
	{
		return remaining;
	}

	/** The remaining time right now, still running down if it was running. */
	public long remainingAt(long now, boolean running)
	{
		if (remaining < 0)
		{
			return -1;
		}
		if (!running || updatedAt == Long.MIN_VALUE || now <= updatedAt)
		{
			return remaining;
		}
		return Math.max(0, remaining - (now - updatedAt));
	}

	public void reset()
	{
		remaining = -1;
		updatedAt = Long.MIN_VALUE;
	}
}
