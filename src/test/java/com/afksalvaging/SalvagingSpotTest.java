/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.runelite.api.coords.WorldPoint;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class SalvagingSpotTest
{
	@Test
	public void everyHotspotOnTheMapIsListedOnce()
	{
		assertEquals(29, SalvagingSpot.values().length);
		Set<WorldPoint> points = new HashSet<>();
		Set<String> names = new HashSet<>();
		for (SalvagingSpot spot : SalvagingSpot.values())
		{
			assertTrue("duplicate point " + spot, points.add(spot.getPoint()));
			assertTrue("two spots read the same: " + spot.getWhere(), names.add(spot.getSalvageName() + spot.getWhere()));
			assertEquals(0, spot.getPoint().getPlane());
		}
	}

	@Test
	public void levelsComeFromTheWreck()
	{
		assertEquals(35, SalvagingSpot.BARRACUDA_UNKAH.getSailingLevel());
		assertEquals(87, SalvagingSpot.MERCHANT_LAGUNA_AURORAE.getSailingLevel());
		for (SalvagingSpot spot : SalvagingSpot.values())
		{
			assertEquals(spot.getWreck().getSailingLevel(), spot.getSailingLevel());
		}
	}

	@Test
	public void spotsPerWreckMatchTheMap()
	{
		assertEquals(2, SalvagingSpot.forWreck(ShipwreckType.SMALL).size());
		assertEquals(3, SalvagingSpot.forWreck(ShipwreckType.FISHERMAN).size());
		assertEquals(6, SalvagingSpot.forWreck(ShipwreckType.BARRACUDA).size());
		assertEquals(5, SalvagingSpot.forWreck(ShipwreckType.LARGE).size());
		assertEquals(4, SalvagingSpot.forWreck(ShipwreckType.PIRATE).size());
		assertEquals(3, SalvagingSpot.forWreck(ShipwreckType.MERCENARY).size());
		assertEquals(3, SalvagingSpot.forWreck(ShipwreckType.FREMENNIK).size());
		assertEquals(3, SalvagingSpot.forWreck(ShipwreckType.MERCHANT).size());
		assertEquals(29, SalvagingSpot.forWreck(null).size());
	}

	@Test
	public void namesReadLikeTheGameAndTheMap()
	{
		assertEquals("Fishy salvage", SalvagingSpot.salvageName(ShipwreckType.FISHERMAN));
		assertEquals("Opulent salvage", SalvagingSpot.salvageName(ShipwreckType.MERCHANT));
		assertEquals("25 tiles south-west of Ruins of Unkah", SalvagingSpot.BARRACUDA_UNKAH.getWhere());
	}

	@Test
	public void nearestSpotIsFoundFromAnywhere()
	{
		assertEquals(SalvagingSpot.BARRACUDA_UNKAH, SalvagingSpot.nearest(new WorldPoint(3140, 2820, 0)));
		assertEquals(SalvagingSpot.SMALL_LANDS_END, SalvagingSpot.nearest(new WorldPoint(1506, 3402, 0)));
		assertNull(SalvagingSpot.nearest(null));
		List<SalvagingSpot> all = SalvagingSpot.forWreck(null);
		for (SalvagingSpot spot : all)
		{
			assertEquals(spot, SalvagingSpot.nearest(spot.getPoint()));
		}
	}
}
