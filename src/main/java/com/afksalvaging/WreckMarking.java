/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

/**
 * How wrecks are marked in the scene: the hull outlined, the water a hook can reach boxed, both or
 * neither (owner, 2026-10-07: "switch between marking the hull of salvage spot and the entire area").
 * One setting in place of 2.0's two switches.
 */
public enum WreckMarking
{
	HULL("Hull", true, false),
	AREA("Area", false, true),
	BOTH("Hull and area", true, true),
	OFF("Off", false, false);

	private final String label;
	private final boolean hull;
	private final boolean area;

	WreckMarking(String label, boolean hull, boolean area)
	{
		this.label = label;
		this.hull = hull;
		this.area = area;
	}

	/** Whether the wreck's own model is outlined. */
	public boolean hull()
	{
		return hull;
	}

	/** Whether the water a hook can reach the wreck from is boxed. */
	public boolean area()
	{
		return area;
	}

	/** The choice that matches 2.0's two switches, for carrying a player's setting over. */
	static WreckMarking of(boolean hull, boolean area)
	{
		for (WreckMarking marking : values())
		{
			if (marking.hull == hull && marking.area == area)
			{
				return marking;
			}
		}
		return BOTH;
	}

	@Override
	public String toString()
	{
		return label;
	}
}
