/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

/** Which object to highlight for the next step with salvage (owner, 2026-10-12). */
public enum GuideTarget
{
	AUTOMATIC("Automatic"),
	STATION("Station"),
	HOLD("Cargo hold"),
	OFF("Off");

	private final String label;

	GuideTarget(String label)
	{
		this.label = label;
	}

	@Override
	public String toString()
	{
		return label;
	}
}
