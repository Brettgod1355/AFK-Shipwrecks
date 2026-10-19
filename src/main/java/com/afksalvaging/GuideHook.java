/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

/** Which salvaging hook the salvage highlight may point at (owner, 2026-10-12). */
public enum GuideHook
{
	WEAKEST_CREW("Free or weakest"),
	FREE_ONLY("Free only"),
	OFF("Off");

	private final String label;

	GuideHook(String label)
	{
		this.label = label;
	}

	@Override
	public String toString()
	{
		return label;
	}
}
