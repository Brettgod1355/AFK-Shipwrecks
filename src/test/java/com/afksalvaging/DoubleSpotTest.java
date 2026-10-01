/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

import java.util.List;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ObjectID;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class DoubleSpotTest
{
	private static final int REACH = 8;

	@Test
	public void overlapIsWhereBothSquaresMeet()
	{
		DoubleSpot.Box box = DoubleSpot.reachOverlap(new WorldPoint(100, 100, 0), new WorldPoint(110, 100, 0), REACH);
		assertNotNull(box);
		assertEquals(102, box.getMinX());
		assertEquals(108, box.getMaxX());
		assertEquals(92, box.getMinY());
		assertEquals(108, box.getMaxY());
		assertEquals(7, box.width());
		assertEquals(17, box.height());
		assertTrue(box.contains(new WorldPoint(105, 100, 0)));
		assertFalse(box.contains(new WorldPoint(101, 100, 0)));
		assertFalse(box.contains(new WorldPoint(105, 100, 1)));
	}

	@Test
	public void aWreckWithItselfGivesItsWholeReachSquare()
	{
		DoubleSpot.Box box = DoubleSpot.reachOverlap(new WorldPoint(100, 100, 0), new WorldPoint(100, 100, 0), REACH);
		assertNotNull(box);
		assertEquals("(92,92)-(108,108)", box.toString());
		assertEquals(17, box.width());
		assertEquals(17, box.height());
		assertTrue(box.contains(new WorldPoint(108, 92, 0)));
		assertFalse(box.contains(new WorldPoint(109, 100, 0)));
	}

	@Test
	public void wrecksTooFarApartShareNoSpot()
	{
		assertNull(DoubleSpot.reachOverlap(new WorldPoint(100, 100, 0), new WorldPoint(117, 100, 0), REACH));
		assertNotNull(DoubleSpot.reachOverlap(new WorldPoint(100, 100, 0), new WorldPoint(116, 100, 0), REACH));
		assertNull(DoubleSpot.reachOverlap(new WorldPoint(100, 100, 0), new WorldPoint(100, 100, 1), REACH));
		assertNull(DoubleSpot.reachOverlap(null, new WorldPoint(100, 100, 0), REACH));
	}

	@Test
	public void diagonalNeighboursGiveASquareishBox()
	{
		DoubleSpot.Box box = DoubleSpot.reachOverlap(new WorldPoint(100, 100, 0), new WorldPoint(106, 94, 0), REACH);
		assertNotNull(box);
		assertEquals("(98,92)-(108,102)", box.toString());
		assertEquals(11, box.width());
		assertEquals(11, box.height());
	}

	@Test
	public void findsPairsAmongSitesAndRespectsTheMode()
	{
		WreckTracker tracker = new WreckTracker();
		long now = 1_000;
		tracker.observe(new WorldPoint(100, 100, 0), ObjectID.SAILING_BARRACUDA_SHIPWRECK, now);
		tracker.observe(new WorldPoint(110, 100, 0), ObjectID.SAILING_SMALL_SHIPWRECK_STUMP, now);
		tracker.observe(new WorldPoint(105, 110, 0), ObjectID.SAILING_BARRACUDA_SHIPWRECK, now);
		tracker.observe(new WorldPoint(300, 300, 0), ObjectID.SAILING_BARRACUDA_SHIPWRECK, now);
		List<WreckTracker.Site> sites = tracker.presentSites();
		assertEquals(4, sites.size());

		assertTrue(DoubleSpot.find(sites, REACH, DoubleSpotMode.OFF).isEmpty());

		List<DoubleSpot> active = DoubleSpot.find(sites, REACH, DoubleSpotMode.ACTIVE_WRECKS);
		assertEquals(1, active.size());
		assertEquals(2, active.get(0).activeCount());
		assertEquals("2 x Barracuda", active.get(0).label());

		List<DoubleSpot> all = DoubleSpot.find(sites, REACH, DoubleSpotMode.ALL_SITES);
		assertEquals(3, all.size());
		int withStump = 0;
		for (DoubleSpot spot : all)
		{
			if (spot.activeCount() == 1)
			{
				withStump++;
				assertTrue(spot.label().contains("Small"));
			}
		}
		assertEquals(2, withStump);
	}

	@Test
	public void sitesOutOfViewAreSkipped()
	{
		WreckTracker tracker = new WreckTracker();
		tracker.observe(new WorldPoint(100, 100, 0), ObjectID.SAILING_BARRACUDA_SHIPWRECK, 0);
		tracker.observe(new WorldPoint(110, 100, 0), ObjectID.SAILING_BARRACUDA_SHIPWRECK, 0);
		assertEquals(1, DoubleSpot.find(tracker.presentSites(), REACH, DoubleSpotMode.ACTIVE_WRECKS).size());
		tracker.despawn(new WorldPoint(110, 100, 0), ObjectID.SAILING_BARRACUDA_SHIPWRECK, 1);
		assertEquals(1, tracker.presentSites().size());
		assertTrue(DoubleSpot.find(tracker.presentSites(), REACH, DoubleSpotMode.ACTIVE_WRECKS).isEmpty());
	}
}
