/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

/**
 * What the cargo counter shows. The enum names double as the labels in the settings panel,
 * which RuneLite renders in title case.
 */
public enum CounterStyle
{
	/** {@code 154/160}. */
	USED_OF_TOTAL,
	/** {@code 6 left}. */
	SLOTS_LEFT,
	/** {@code 96%}. */
	PERCENT_FULL;

	/** The counter's number for this style. */
	public String format(int used, int capacity)
	{
		switch (this)
		{
			case SLOTS_LEFT:
				return Math.max(0, capacity - used) + " left";
			case PERCENT_FULL:
				return percentFull(used, capacity) + "%";
			default:
				return used + "/" + capacity;
		}
	}

	/**
	 * How full the hold is, rounded down so it only reads 100 when nothing more fits.
	 * A count above the capacity, which happens when the capacity was guessed too low,
	 * also reads 100.
	 */
	static int percentFull(int used, int capacity)
	{
		if (capacity <= 0)
		{
			return 0;
		}
		return (int) Math.min(100, Math.floor(100.0 * used / capacity));
	}
}
