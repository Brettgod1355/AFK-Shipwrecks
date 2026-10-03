/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import net.runelite.api.coords.WorldPoint;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class SeaMapTest
{
	/**
	 * A ten-by-ten grid of 4-tile cells from (1000, 2000): sea everywhere except a wall of land
	 * down column 5 with a gap at row 9, and an island cell at (2, 7).
	 */
	private static SeaMap sample()
	{
		BitSet sea = new BitSet(100);
		for (int cy = 0; cy < 10; cy++)
		{
			for (int cx = 0; cx < 10; cx++)
			{
				boolean wall = cx == 5 && cy != 9;
				boolean island = cx == 2 && cy == 7;
				if (!wall && !island)
				{
					sea.set(cy * 10 + cx);
				}
			}
		}
		return new SeaMap(1000, 2000, 4, 10, 10, sea);
	}

	private static WorldPoint cell(int cx, int cy)
	{
		return new WorldPoint(1000 + cx * 4 + 1, 2000 + cy * 4 + 1, 0);
	}

	@Test
	public void aStraightRunCostsItsTilesAndADiagonalABitMore()
	{
		int[] d = sample().distances(cell(0, 0), Arrays.asList(cell(4, 0), cell(4, 4), cell(0, 0)));
		assertArrayEquals(new int[]{16, 22, 0}, d);
	}

	@Test
	public void landInTheWayIsSailedRound()
	{
		// Straight across the wall it would be 8 cells; the gap at row 9 makes it a long detour.
		int[] d = sample().distances(cell(1, 0), Arrays.asList(cell(9, 0)));
		assertTrue(d[0] > 8 * 4);
		assertEquals(9 * 14 * 4 / 10 + 9 * 14 * 4 / 10 - 8, d[0], 8);
	}

	@Test
	public void theIslandIsNotSeaButADockOnItMeasuresFromTheWaterBesideIt()
	{
		SeaMap map = sample();
		assertFalse(map.isSea(cell(2, 7)));
		assertTrue(map.isSea(cell(2, 8)));
		int[] d = map.distances(cell(2, 7), Arrays.asList(cell(2, 9)));
		assertTrue("from a cell beside the island: " + d[0], d[0] >= 4 && d[0] <= 16);
	}

	@Test
	public void farOutsideTheMapNothingIsReachable()
	{
		int[] d = sample().distances(new WorldPoint(3000, 3000, 0), Arrays.asList(cell(0, 0)));
		assertEquals(SeaMap.UNREACHABLE, d[0]);
	}

	@Test
	public void thePackagedMapLoadsAndKnowsTheOceanFromTheLand() throws IOException
	{
		SeaMap map = SeaMap.load();
		assertTrue(map.seaCells() > 100_000);
		assertTrue("Rellekka harbour", map.isSea(new WorldPoint(2630, 3708, 0)));
		assertFalse("Lumbridge castle", map.isSea(new WorldPoint(3222, 3218, 0)));
		for (SalvagingSpot spot : SalvagingSpot.values())
		{
			assertTrue(spot.name(), map.nearestCell(spot.getPoint()) >= 0);
		}
	}

	@Test
	public void rellekkaToWitchavenIsALongWayRoundByBoat() throws IOException
	{
		SeaMap map = SeaMap.load();
		List<WorldPoint> targets = new ArrayList<>();
		targets.add(SalvagingSpot.FISHERMAN_WITCHAVEN.getPoint());
		targets.add(SalvagingSpot.FREMENNIK_ETCETERIA.getPoint());
		long start = System.nanoTime();
		int[] d = map.distances(Mooring.RELLEKKA.getPoint(), targets);
		long millis = (System.nanoTime() - start) / 1_000_000;
		assertTrue("Witchaven " + d[0], d[0] > 1500 && d[0] < 3000);
		assertTrue("Etceteria " + d[1], d[1] > 350 && d[1] < 600);
		assertTrue("took " + millis + " ms", millis < 500);
	}
}
