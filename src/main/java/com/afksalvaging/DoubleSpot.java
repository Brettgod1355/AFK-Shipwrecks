/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import net.runelite.api.coords.WorldPoint;

/**
 * A place on the water where one salvaging hook reaches two wrecks at once.
 * <p>
 * A hook can work any wreck within its reach, measured as the larger of the east-west and
 * north-south distances, so the tiles it can reach a wreck from form a square around that wreck.
 * Where two of those squares overlap, a hook parked in the overlap works both wrecks: that is a
 * double salvage spot, and the overlap is always a rectangle. The game adds its own such spots to
 * every hotspot; this works them out from the wreck sites actually in view, including sites whose
 * wreck has sunk, since the next one rises on the same tile.
 */
public final class DoubleSpot
{
	/** A rectangle of tiles, inclusive on all sides, on one plane. */
	public static final class Box
	{
		private final int minX;
		private final int minY;
		private final int maxX;
		private final int maxY;
		private final int plane;

		Box(int minX, int minY, int maxX, int maxY, int plane)
		{
			this.minX = minX;
			this.minY = minY;
			this.maxX = maxX;
			this.maxY = maxY;
			this.plane = plane;
		}

		public int getMinX()
		{
			return minX;
		}

		public int getMinY()
		{
			return minY;
		}

		public int getMaxX()
		{
			return maxX;
		}

		public int getMaxY()
		{
			return maxY;
		}

		public int getPlane()
		{
			return plane;
		}

		/** Tiles east to west. */
		public int width()
		{
			return maxX - minX + 1;
		}

		/** Tiles north to south. */
		public int height()
		{
			return maxY - minY + 1;
		}

		/** Whether a hook on this tile is inside the box. */
		public boolean contains(WorldPoint point)
		{
			return point != null && point.getPlane() == plane
				&& point.getX() >= minX && point.getX() <= maxX
				&& point.getY() >= minY && point.getY() <= maxY;
		}

		@Override
		public String toString()
		{
			return "(" + minX + "," + minY + ")-(" + maxX + "," + maxY + ")";
		}
	}

	private final WreckTracker.Site first;
	private final WreckTracker.Site second;
	private final Box box;

	private DoubleSpot(WreckTracker.Site first, WreckTracker.Site second, Box box)
	{
		this.first = first;
		this.second = second;
		this.box = box;
	}

	public WreckTracker.Site getFirst()
	{
		return first;
	}

	public WreckTracker.Site getSecond()
	{
		return second;
	}

	public Box getBox()
	{
		return box;
	}

	/** How many of the two wrecks are up right now: 0, 1 or 2. */
	public int activeCount()
	{
		return (first.isActive() ? 1 : 0) + (second.isActive() ? 1 : 0);
	}

	/** The two wrecks by name, for example {@code Barracuda + Small}. */
	public String label()
	{
		String a = first.getType().getDisplayName();
		String b = second.getType().getDisplayName();
		return a.equals(b) ? "2 x " + a : a + " + " + b;
	}

	/**
	 * The tiles from which a hook with this reach works both wrecks, or null when no tile does.
	 * Wrecks on different planes never share a spot.
	 */
	public static Box reachOverlap(WorldPoint a, WorldPoint b, int reach)
	{
		if (a == null || b == null || reach < 0 || a.getPlane() != b.getPlane())
		{
			return null;
		}
		int minX = Math.max(a.getX(), b.getX()) - reach;
		int maxX = Math.min(a.getX(), b.getX()) + reach;
		int minY = Math.max(a.getY(), b.getY()) - reach;
		int maxY = Math.min(a.getY(), b.getY()) + reach;
		if (minX > maxX || minY > maxY)
		{
			return null;
		}
		return new Box(minX, minY, maxX, maxY, a.getPlane());
	}

	/**
	 * Every double spot among these wreck sites, west to east then south to north, so the drawing
	 * order is stable from tick to tick.
	 *
	 * @param sites the wreck sites to pair up; sites that are not in view are skipped
	 * @param reach the hook's reach in tiles
	 * @param mode  which pairs count: both wrecks up, or any two sites
	 */
	public static List<DoubleSpot> find(List<WreckTracker.Site> sites, int reach, DoubleSpotMode mode)
	{
		if (mode == null || mode == DoubleSpotMode.OFF || sites == null || sites.size() < 2)
		{
			return Collections.emptyList();
		}
		List<DoubleSpot> spots = new ArrayList<>();
		for (int i = 0; i < sites.size(); i++)
		{
			WreckTracker.Site a = sites.get(i);
			if (a == null || !a.isPresent())
			{
				continue;
			}
			for (int j = i + 1; j < sites.size(); j++)
			{
				WreckTracker.Site b = sites.get(j);
				if (b == null || !b.isPresent())
				{
					continue;
				}
				if (mode == DoubleSpotMode.ACTIVE_WRECKS && !(a.isActive() && b.isActive()))
				{
					continue;
				}
				Box box = reachOverlap(a.getPoint(), b.getPoint(), reach);
				if (box != null)
				{
					spots.add(new DoubleSpot(a, b, box));
				}
			}
		}
		spots.sort(Comparator.comparingInt((DoubleSpot s) -> s.box.minX)
			.thenComparingInt(s -> s.box.minY)
			.thenComparingInt(s -> s.box.maxX)
			.thenComparingInt(s -> s.box.maxY));
		return spots;
	}
}
