/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

/** What to do with an item that salvaging put in the inventory. */
public enum SortRule
{
	/** Stays in the inventory: nails, runes, dart tips, the things you came aboard with. */
	KEEP("Keep"),
	/** Goes in the cargo hold, where a stack takes one slot. */
	HOLD("Hold"),
	/** Worth casting High Level Alchemy on. */
	ALCH("Alch"),
	/** Not worth the slot. */
	DROP("Drop");

	private final String label;

	SortRule(String label)
	{
		this.label = label;
	}

	public String getLabel()
	{
		return label;
	}
}
