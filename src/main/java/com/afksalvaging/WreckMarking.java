/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

/**
 * How wrecks are marked in the scene: the wreck itself (a box on the tiles it stands on, or its hull
 * outlined), the water a hook can reach boxed, both or neither (owner, 2026-10-07: "switch between marking
 * the hull of salvage spot and the entire area"). One setting in place of 2.0's two switches.
 * <p>
 * The tile box joined in 2.2 and became the default (owner, 2026-10-14: "instead of being a whole highlight,
 * it should just be a small tile highlight ... so it shows if it's active or inactive with ... the timer").
 * The constant names are what RuneLite saves, so the 2.1 ones keep their names.
 */
public enum WreckMarking
{
	TILE_AND_AREA("Tile and area", Marker.TILE, true),
	TILE("Tile", Marker.TILE, false),
	BOTH("Hull and area", Marker.HULL, true),
	HULL("Hull", Marker.HULL, false),
	AREA("Area", Marker.NONE, true),
	OFF("Off", Marker.NONE, false);

	/** What marks the wreck itself. */
	private enum Marker
	{
		NONE,
		TILE,
		HULL
	}

	private final String label;
	private final Marker marker;
	private final boolean area;

	WreckMarking(String label, Marker marker, boolean area)
	{
		this.label = label;
		this.marker = marker;
		this.area = area;
	}

	/** Whether the wreck's own model is outlined. */
	public boolean hull()
	{
		return marker == Marker.HULL;
	}

	/** Whether the tiles the wreck stands on are boxed. */
	public boolean tile()
	{
		return marker == Marker.TILE;
	}

	/** Whether the water a hook can reach the wreck from is boxed. */
	public boolean area()
	{
		return area;
	}

	/** The choice that matches 2.0's two switches, for carrying a player's setting over. */
	static WreckMarking of(boolean hull, boolean area)
	{
		if (hull)
		{
			return area ? BOTH : HULL;
		}
		return area ? AREA : OFF;
	}

	/**
	 * The choice after 2.2's change of default. Hull and area was the default from 2.0 to 2.1 and RuneLite
	 * saves a default as a value, so it moves to the new default; a player who chose anything else keeps it.
	 */
	static WreckMarking tileDefault(WreckMarking current)
	{
		return current == BOTH ? TILE_AND_AREA : current;
	}

	@Override
	public String toString()
	{
		return label;
	}
}
