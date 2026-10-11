/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

import java.util.Collections;
import java.util.List;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ObjectID;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class WreckTimersTest
{
	private static final WorldPoint A = new WorldPoint(1800, 4000, 0);
	private static final WorldPoint B = new WorldPoint(1806, 4004, 0);
	private static final WorldPoint C = new WorldPoint(1830, 4000, 0);
	/** Merchant wrecks last 240 s. */
	private static final long LIFE = ShipwreckType.MERCHANT.getLifetimeSeconds() * 1000L;

	private final WreckTracker tracker = new WreckTracker();

	private WreckTracker.Site site(WorldPoint point)
	{
		for (WreckTracker.Site site : tracker.presentSites())
		{
			if (site.getPoint().equals(point))
			{
				return site;
			}
		}
		throw new AssertionError("no site at " + point);
	}

	private WreckTimers.Timer timer(WorldPoint point, long now)
	{
		List<WreckTracker.Site> present = tracker.presentSites();
		return WreckTimers.of(site(point), present, now);
	}

	@Test
	public void aWreckNobodyHasWorkedHasNoNumber()
	{
		tracker.observe(A, ObjectID.SAILING_MERCHANT_SHIPWRECK, 0);
		WreckTimers.Timer timer = timer(A, 1_000);
		assertEquals("Up - no clock yet", timer.text);
		assertTrue(timer.up);
	}

	@Test
	public void aWorkedWreckCountsDownThenSaysAnyMoment()
	{
		tracker.observe(A, ObjectID.SAILING_MERCHANT_SHIPWRECK, 0);
		tracker.noteBoatsNear(Collections.singletonList(A), AfkSession.OTHER_BOAT_REACH, 10_000);
		assertEquals("Sinks in ≤ 3:50", timer(A, 20_000).text);
		assertEquals("Sinks any moment", timer(A, 10_000 + LIFE + 1).text);
		tracker.forgetStaleClocks(10_000 + LIFE + WreckTracker.CLOCK_GRACE_MILLIS + 1);
		assertEquals("Up - outlived its clock", timer(A, 10_000 + LIFE + WreckTracker.CLOCK_GRACE_MILLIS + 2).text);
	}

	@Test
	public void aSunkSiteSaysWhenTheNextWreckCanRise()
	{
		tracker.observe(A, ObjectID.SAILING_MERCHANT_SHIPWRECK_STUMP, 0);
		tracker.observe(B, ObjectID.SAILING_MERCHANT_SHIPWRECK, 0);
		WreckTimers.Timer timer = timer(A, 1_000);
		assertEquals("Sunk", timer.text);
		assertFalse(timer.up);

		// The tracker's times are a steady clock that is never 0 in play; 0 means "not seen".
		tracker.noteBoatsNear(Collections.singletonList(B), AfkSession.OTHER_BOAT_REACH, 1_000);
		assertEquals("Sunk - next ≤ 3:59", timer(A, 2_000).text);
		assertEquals("Sunk - next any moment", timer(A, 1_000 + LIFE + 1).text);
	}

	@Test
	public void withTwoSunkSitesTheRiseMayLandElsewhere()
	{
		tracker.observe(A, ObjectID.SAILING_MERCHANT_SHIPWRECK_STUMP, 0);
		tracker.observe(C, ObjectID.SAILING_MERCHANT_SHIPWRECK_STUMP, 0);
		tracker.observe(B, ObjectID.SAILING_MERCHANT_SHIPWRECK, 0);
		tracker.noteBoatsNear(Collections.singletonList(B), AfkSession.OTHER_BOAT_REACH, 1_000);
		assertEquals("Sunk - maybe next ≤ 3:59", timer(A, 2_000).text);
	}
}
