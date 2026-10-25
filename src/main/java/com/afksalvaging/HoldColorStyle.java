/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

/** How the count on the cargo hold is colored (owner, 2026-10-25). The constant names are what RuneLite saves. */
public enum HoldColorStyle
{
	/** From the empty color to the full color as the hold fills: the look before 2.3.2. */
	FADE("Fade"),
	/** The empty color, the nearly-full color once "Warn with slots left" is reached, the full color when full. */
	STEPS("Steps"),
	/** Always the empty color. */
	ONE_COLOR("One color");

	private final String label;

	HoldColorStyle(String label)
	{
		this.label = label;
	}

	@Override
	public String toString()
	{
		return label;
	}
}
