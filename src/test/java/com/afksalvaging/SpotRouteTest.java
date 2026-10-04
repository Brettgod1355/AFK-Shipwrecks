/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

import java.util.Collections;
import java.util.List;
import net.runelite.api.coords.WorldPoint;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class SpotRouteTest
{
	private static final SalvagingSpot SPOT = SalvagingSpot.FISHERMAN_WITCHAVEN;
	private static final List<WreckTracker.Site> NO_SITES = Collections.emptyList();
	private static final WorldPoint FAR = fromSpot(-300, 200);

	private static WorldPoint fromSpot(int dx, int dy)
	{
		return new WorldPoint(SPOT.getPoint().getX() + dx, SPOT.getPoint().getY() + dy, 0);
	}

	private static List<WreckTracker.Site> siteAt(WorldPoint point)
	{
		return Collections.singletonList(new WreckTracker.Site(point, SPOT.getWreck()));
	}

	@Test
	public void aRouteStaysWhileSailingTowardsTheSpot()
	{
		SpotRoute route = new SpotRoute(SPOT);
		for (int tick = 0; tick < 10; tick++)
		{
			assertEquals(SpotRoute.End.NONE, route.tick(true, FAR, NO_SITES));
		}
	}

	@Test
	public void leavingTheBoatEndsItAfterAFewTicks()
	{
		SpotRoute route = new SpotRoute(SPOT);
		route.tick(true, FAR, NO_SITES);
		for (int tick = 1; tick < SpotRoute.LEAVE_TICKS; tick++)
		{
			assertEquals(SpotRoute.End.NONE, route.tick(false, FAR, NO_SITES));
		}
		assertEquals(SpotRoute.End.LEFT_BOAT, route.tick(false, FAR, NO_SITES));
	}

	@Test
	public void oneOddTickOffTheBoatDoesNotEndIt()
	{
		SpotRoute route = new SpotRoute(SPOT);
		for (int tick = 0; tick < 10; tick++)
		{
			route.tick(true, FAR, NO_SITES);
			assertEquals(SpotRoute.End.NONE, route.tick(false, FAR, NO_SITES));
		}
	}

	@Test
	public void aRouteSentFromLandStaysUntilTheBoatIsBoardedAndLeft()
	{
		SpotRoute route = new SpotRoute(SPOT);
		for (int tick = 0; tick < 10; tick++)
		{
			assertEquals(SpotRoute.End.NONE, route.tick(false, FAR, NO_SITES));
		}
		assertEquals(SpotRoute.End.NONE, route.tick(true, FAR, NO_SITES));
		for (int tick = 1; tick < SpotRoute.LEAVE_TICKS; tick++)
		{
			route.tick(false, FAR, NO_SITES);
		}
		assertEquals(SpotRoute.End.LEFT_BOAT, route.tick(false, FAR, NO_SITES));
	}

	@Test
	public void reachingTheMiddleOfTheSpotEndsIt()
	{
		assertEquals(SpotRoute.End.ARRIVED, new SpotRoute(SPOT).tick(true, fromSpot(SpotRoute.ARRIVAL_TILES, 0), NO_SITES));
		assertEquals(SpotRoute.End.NONE, new SpotRoute(SPOT).tick(true, fromSpot(SpotRoute.ARRIVAL_TILES + 1, 0), NO_SITES));
	}

	@Test
	public void pullingUpToOneOfTheSpotsWrecksEndsIt()
	{
		WorldPoint wreck = fromSpot(30, 0);
		assertEquals(SpotRoute.End.ARRIVED, new SpotRoute(SPOT).tick(true, fromSpot(30 + SpotRoute.ARRIVAL_TILES, 0), siteAt(wreck)));
		assertEquals(SpotRoute.End.NONE, new SpotRoute(SPOT).tick(true, fromSpot(31 + SpotRoute.ARRIVAL_TILES, 0), siteAt(wreck)));
	}

	@Test
	public void aWreckOfAnotherSpotOnTheWayDoesNotEndIt()
	{
		WorldPoint wreck = fromSpot(SpotRoute.SPOT_TILES + 1, 0);
		assertEquals(SpotRoute.End.NONE, new SpotRoute(SPOT).tick(true, fromSpot(SpotRoute.SPOT_TILES + 5, 0), siteAt(wreck)));
	}

	@Test
	public void anUnknownPositionEndsNothing()
	{
		assertEquals(SpotRoute.End.NONE, new SpotRoute(SPOT).tick(true, null, NO_SITES));
	}
}
