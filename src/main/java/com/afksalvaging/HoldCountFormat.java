/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

/** What the count on the cargo hold reads: the overlay's choice, or its own (owner, 2026-10-25). */
public enum HoldCountFormat
{
	SAME_AS_OVERLAY("Same as overlay", null),
	USED_OF_TOTAL("Used of total", CounterStyle.USED_OF_TOTAL),
	SLOTS_LEFT("Slots left", CounterStyle.SLOTS_LEFT),
	PERCENT_FULL("Percent full", CounterStyle.PERCENT_FULL);

	private final String label;
	private final CounterStyle style;

	HoldCountFormat(String label, CounterStyle style)
	{
		this.label = label;
		this.style = style;
	}

	/** The style to write the count in, given the overlay's "Counter shows". */
	CounterStyle style(CounterStyle overlay)
	{
		return style == null ? overlay : style;
	}

	@Override
	public String toString()
	{
		return label;
	}
}
