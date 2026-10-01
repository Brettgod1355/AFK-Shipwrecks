/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

import java.util.HashSet;
import java.util.Set;
import net.runelite.api.coords.WorldPoint;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class MooringTest
{
	@Test
	public void everyDockOnTheMapIsListedOnce()
	{
		// RuneLite's World Map plugin draws 61 mooring icons (1.13.1).
		assertEquals(61, Mooring.values().length);
		Set<String> names = new HashSet<>();
		Set<WorldPoint> points = new HashSet<>();
		for (Mooring dock : Mooring.values())
		{
			assertTrue(dock.name(), names.add(dock.getDisplayName()));
			assertTrue(dock.name(), points.add(dock.getPoint()));
		}
	}

	@Test
	public void theNearestDockIsByTheLongerAxis()
	{
		// Just off Port Sarim's dock: 10 east, 3 north.
		assertEquals(Mooring.PORT_SARIM, Mooring.nearest(new WorldPoint(3060, 3195, 0)));
		assertEquals(10, Mooring.PORT_SARIM.tilesFrom(new WorldPoint(3060, 3195, 0)));
		// Out at the Unkah Barracuda spot, the ruins are the nearest dock.
		assertEquals(Mooring.RUINS_OF_UNKAH, Mooring.nearest(SalvagingSpot.BARRACUDA_UNKAH.getPoint()));
		assertNull(Mooring.nearest(null));
	}

	@Test
	public void thePlaneDoesNotCount()
	{
		assertEquals(0, Mooring.PORT_SARIM.tilesFrom(new WorldPoint(3050, 3192, 1)));
	}
}
