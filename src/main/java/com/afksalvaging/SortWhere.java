/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

/** Where the inventory boxes are drawn. */
public enum SortWhere
{
	AT_WRECKS("On my boat at a salvaging spot"),
	ABOARD("Whenever I am on my boat"),
	EVERYWHERE("Everywhere");

	private final String label;

	SortWhere(String label)
	{
		this.label = label;
	}

	@Override
	public String toString()
	{
		return label;
	}
}
