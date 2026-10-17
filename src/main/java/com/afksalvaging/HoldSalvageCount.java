/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

/**
 * How many salvage the cargo hold holds, for the salvage highlight's choice between the hold and a hook (owner,
 * 2026-10-17: "perfect if it can just know how much salvage is in the hold and know how much free inventory you
 * have"). Exact at every hold read; between reads it follows the crew's counted salvage and the player's
 * withdrawals and deposits made with the hold window shut. Unknown until the first read on this boat. Pure, so it
 * is tested without a client.
 */
final class HoldSalvageCount
{
	static final int UNKNOWN = -1;

	private int count = UNKNOWN;
	private int lastReadTick = Integer.MIN_VALUE;

	/** The salvage in the hold, or {@link #UNKNOWN}. */
	int get()
	{
		return count;
	}

	/** The hold was read: its salvage is known exactly. */
	void read(int salvage, int tick)
	{
		count = Math.max(0, salvage);
		lastReadTick = tick;
	}

	/** Crew salvage the tally counted while the hold was shut: it went into the hold. */
	void crewSalvage(int counted)
	{
		if (count != UNKNOWN && counted > 0)
		{
			count += counted;
		}
	}

	/**
	 * The salvage in the inventory changed by {@code delta}. With the hold window open, or read this tick, the
	 * read holds the truth and this is left alone. Otherwise a gain during a withdrawal came out of the hold, and
	 * a loss during a deposit went into it; anything else (hooking, sorting) is not the hold's business.
	 */
	void inventorySalvageChanged(int delta, int tick, boolean holdOpen, boolean withdrawing, boolean depositing)
	{
		if (count == UNKNOWN || delta == 0 || holdOpen || tick == lastReadTick)
		{
			return;
		}
		if (delta > 0 && withdrawing)
		{
			count = Math.max(0, count - delta);
		}
		else if (delta < 0 && depositing)
		{
			count -= delta;
		}
	}

	/**
	 * The count remembered for this boat at the end of the last session (owner, 2026-10-17: "remember the hold
	 * salvage count at logout"), used until the hold is read again. A count already known is kept.
	 */
	void restore(int saved)
	{
		if (count == UNKNOWN && saved >= 0)
		{
			count = saved;
		}
	}

	/** The crew banked the hold: it is empty. */
	void banked()
	{
		count = 0;
	}

	/** Another boat, or logged out: not known until the hold is read again. */
	void forget()
	{
		count = UNKNOWN;
		lastReadTick = Integer.MIN_VALUE;
	}
}
