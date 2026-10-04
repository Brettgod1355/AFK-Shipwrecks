/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ObjectID;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class WreckTrackerTest
{
	private static final WorldPoint HOOK = new WorldPoint(1800, 4000, 0);
	private static final List<WorldPoint> HOOKS = Collections.singletonList(HOOK);
	private static final WorldPoint NEAR = new WorldPoint(1806, 4004, 0);
	private static final WorldPoint FAR = new WorldPoint(1840, 4000, 0);
	private static final WorldPoint UPSTAIRS = new WorldPoint(1806, 4004, 1);
	private static final int RANGE = 8;
	private static final int LEVEL = 99;

	private final WreckTracker tracker = new WreckTracker();

	@Test
	public void findsWrecksInRangeOfAnyHookOnTheSamePlane()
	{
		tracker.observe(NEAR, ObjectID.SAILING_MERCHANT_SHIPWRECK, 0);
		tracker.observe(FAR, ObjectID.SAILING_MERCHANT_SHIPWRECK, 0);
		tracker.observe(UPSTAIRS, ObjectID.SAILING_MERCHANT_SHIPWRECK, 0);
		assertFalse(tracker.observe(NEAR, ObjectID.SAILING_BOAT_HULL_KANDARIN_3X8_WOOD, 0));

		List<WreckTracker.Site> nearby = tracker.nearby(HOOKS, RANGE);
		assertEquals(1, nearby.size());
		assertEquals(NEAR, nearby.get(0).getPoint());
		assertEquals(ShipwreckType.MERCHANT, nearby.get(0).getType());
		assertTrue(tracker.anyActive(HOOKS, RANGE));
		assertEquals(1, tracker.eligibleActive(HOOKS, RANGE, LEVEL).size());
		assertEquals(0, tracker.eligibleActive(HOOKS, RANGE, 86).size());
		assertEquals(ShipwreckType.MERCHANT, tracker.typeInReach(HOOKS, RANGE, LEVEL));
		assertNull(tracker.typeInReach(HOOKS, RANGE, 86));
		assertTrue(tracker.nearby((WorldPoint) null, RANGE).isEmpty());
		assertTrue(tracker.nearby(Collections.emptyList(), RANGE).isEmpty());

		// A second hook further along the boat brings the far wreck into reach.
		WorldPoint secondHook = new WorldPoint(1834, 4000, 0);
		assertEquals(2, tracker.nearby(Arrays.asList(HOOK, secondHook), RANGE).size());
	}

	@Test
	public void stumpsAreNotActive()
	{
		tracker.observe(NEAR, ObjectID.SAILING_LARGE_SHIPWRECK_STUMP, 0);
		assertEquals(1, tracker.nearby(HOOKS, RANGE).size());
		assertFalse(tracker.anyActive(HOOKS, RANGE));
		assertEquals(0, tracker.allSunkWithin(HOOKS, RANGE, LEVEL, 0));
		assertFalse(tracker.allAnchored(HOOKS, RANGE, LEVEL));
	}

	@Test
	public void aWreckIsOnlyAnchoredOnceWeWorkIt()
	{
		tracker.observe(NEAR, ObjectID.SAILING_MERCHANT_SHIPWRECK, 10_000);
		// Until then all we know is that it lasts at most one lifetime.
		assertEquals(240_000, tracker.allSunkWithin(HOOKS, RANGE, LEVEL, 20_000));
		assertEquals(240_000, tracker.allSunkWithin(HOOKS, RANGE, LEVEL, 500_000));
		assertFalse(tracker.allAnchored(HOOKS, RANGE, LEVEL));

		tracker.noteRolling(HOOKS, RANGE, LEVEL, 30_000);
		assertTrue(tracker.allAnchored(HOOKS, RANGE, LEVEL));
		assertEquals(240_000, tracker.allSunkWithin(HOOKS, RANGE, LEVEL, 30_000));
		assertEquals(180_000, tracker.allSunkWithin(HOOKS, RANGE, LEVEL, 90_000));
		assertEquals(0, tracker.allSunkWithin(HOOKS, RANGE, LEVEL, 500_000));
		// Rolling on does not move the anchor.
		tracker.noteRolling(HOOKS, RANGE, LEVEL, 100_000);
		assertEquals(170_000, tracker.allSunkWithin(HOOKS, RANGE, LEVEL, 100_000));
	}

	@Test
	public void seeingAWreckRiseDoesNotAnchorItByItself()
	{
		tracker.observe(NEAR, ObjectID.SAILING_SMALL_SHIPWRECK_STUMP, 0);
		tracker.observe(NEAR, ObjectID.SAILING_SMALL_SHIPWRECK, 50_000);
		assertFalse(tracker.allAnchored(HOOKS, RANGE, LEVEL));
		assertEquals(60_000, tracker.allSunkWithin(HOOKS, RANGE, LEVEL, 80_000));

		tracker.observe(NEAR, ObjectID.SAILING_SMALL_SHIPWRECK_STUMP, 110_000);
		assertEquals(110_000, tracker.getLastSinkAt());
		assertEquals(110_000, tracker.nearby(HOOKS, RANGE).get(0).getSunkAt());
		assertFalse(tracker.anyActive(HOOKS, RANGE));

		// The new wreck on the site starts with nothing known.
		tracker.noteRolling(HOOKS, RANGE, LEVEL, 100_000);
		tracker.observe(NEAR, ObjectID.SAILING_SMALL_SHIPWRECK, 200_000);
		assertFalse(tracker.allAnchored(HOOKS, RANGE, LEVEL));
		assertEquals(60_000, tracker.allSunkWithin(HOOKS, RANGE, LEVEL, 200_000));
	}

	@Test
	public void theLongestLivedWreckDecidesWhenAllAreGone()
	{
		WorldPoint other = new WorldPoint(1795, 3996, 0);
		tracker.observe(NEAR, ObjectID.SAILING_PIRATE_SHIPWRECK, 10_000);
		tracker.noteRolling(HOOKS, RANGE, LEVEL, 10_000);
		tracker.observe(other, ObjectID.SAILING_PIRATE_SHIPWRECK, 70_000);
		tracker.noteRolling(HOOKS, RANGE, LEVEL, 70_000);
		assertEquals(180_000, tracker.allSunkWithin(HOOKS, RANGE, LEVEL, 70_000));
		assertEquals(150_000, tracker.allSunkWithin(HOOKS, RANGE, LEVEL, 100_000));
		assertTrue(tracker.allAnchored(HOOKS, RANGE, LEVEL));

		// A third, unanchored wreck loosens the bound to a whole lifetime.
		WorldPoint third = new WorldPoint(1808, 3998, 0);
		tracker.observe(third, ObjectID.SAILING_PIRATE_SHIPWRECK, 100_000);
		assertEquals(180_000, tracker.allSunkWithin(HOOKS, RANGE, LEVEL, 100_000));
		assertFalse(tracker.allAnchored(HOOKS, RANGE, LEVEL));
	}

	@Test
	public void aSceneReloadDoesNotResetAnything()
	{
		tracker.observe(NEAR, ObjectID.SAILING_MERCHANT_SHIPWRECK, 0);
		tracker.noteRolling(HOOKS, RANGE, LEVEL, 0);
		tracker.markAllAbsent(50_000);
		assertTrue(tracker.nearby(HOOKS, RANGE).isEmpty());
		// The replayed spawn is the same wreck with the same clock.
		tracker.observe(NEAR, ObjectID.SAILING_MERCHANT_SHIPWRECK, 51_000);
		assertTrue(tracker.allAnchored(HOOKS, RANGE, LEVEL));
		assertEquals(189_000, tracker.allSunkWithin(HOOKS, RANGE, LEVEL, 51_000));
	}

	@Test
	public void sitesThatLeaveTheSceneAreRememberedThenForgotten()
	{
		tracker.observe(NEAR, ObjectID.SAILING_MERCHANT_SHIPWRECK, 0);
		tracker.despawn(NEAR, ObjectID.SAILING_MERCHANT_SHIPWRECK, 1_000);
		assertTrue(tracker.nearby(HOOKS, RANGE).isEmpty());
		assertEquals(1, tracker.siteCount());
		tracker.prune(1_000 + WreckTracker.FORGET_AFTER_MILLIS);
		assertEquals(1, tracker.siteCount());
		tracker.prune(1_001 + WreckTracker.FORGET_AFTER_MILLIS);
		assertEquals(0, tracker.siteCount());

		// Despawn of a different object at the site is ignored.
		tracker.observe(NEAR, ObjectID.SAILING_MERCHANT_SHIPWRECK, 2_000);
		tracker.despawn(NEAR, ObjectID.SAILING_BOAT_HULL_KANDARIN_3X8_WOOD, 3_000);
		assertEquals(1, tracker.nearby(HOOKS, RANGE).size());
	}

	@Test
	public void availabilityStartsAtThePriorAndMovesWithEvidence()
	{
		long prior = 600_000;
		assertEquals(0.7, tracker.availability(0.7, prior, 0), 1e-9);
		for (long now = 600; now <= 600_000; now += 600)
		{
			tracker.recordAvailability(true, 600, now);
		}
		double after = tracker.availability(0.7, prior, 600_000);
		assertTrue(after > 0.8 && after < 0.9);
		for (long now = 600_600; now <= 1_800_000; now += 600)
		{
			tracker.recordAvailability(false, 600, now);
		}
		double later = tracker.availability(0.7, prior, 1_800_000);
		assertTrue(later < after);
		assertTrue(later > 0.2);
		tracker.recordAvailability(true, 0, 1_800_000);
		assertEquals(later, tracker.availability(0.7, prior, 1_800_000), 1e-9);
		WreckTracker fresh = new WreckTracker();
		assertEquals(1.0, fresh.availability(1.0, 0, 0), 1e-9);
	}

	@Test
	public void theNextWreckHereIsTheEarliestOtherSinkOnASalvagingWorld()
	{
		WreckTracker tracker = new WreckTracker();
		long lifetime = ShipwreckType.MERCHANT.getLifetimeSeconds() * 1000L;
		// Ours has sunk; the far one rose at 10 s and, on a salvaging world, was worked from then.
		tracker.observe(NEAR, ObjectID.SAILING_MERCHANT_SHIPWRECK_STUMP, 0);
		tracker.observe(FAR, ObjectID.SAILING_MERCHANT_SHIPWRECK_STUMP, 0);
		tracker.observe(FAR, ObjectID.SAILING_MERCHANT_SHIPWRECK, 10_000);
		WreckTracker.NextRise next = tracker.nextRise(HOOKS, RANGE, LEVEL, true, 20_000);
		assertNotNull(next);
		assertEquals(10_000 + lifetime - 20_000, next.withinMillis);
		assertTrue("ours is the only empty site in view", next.certain);
		// Off a salvaging world nobody may be working the far wreck: nothing to go by.
		assertNull(tracker.nextRise(HOOKS, RANGE, LEVEL, false, 20_000));
		// Another empty site out of reach: the next wreck may rise there instead.
		WorldPoint other = new WorldPoint(1760, 4000, 0);
		tracker.observe(other, ObjectID.SAILING_MERCHANT_SHIPWRECK_STUMP, 20_000);
		next = tracker.nextRise(HOOKS, RANGE, LEVEL, true, 20_000);
		assertNotNull(next);
		assertFalse(next.certain);
		// A wreck up in reach: we are not waiting.
		tracker.observe(NEAR, ObjectID.SAILING_MERCHANT_SHIPWRECK, 30_000);
		assertNull(tracker.nextRise(HOOKS, RANGE, LEVEL, true, 30_000));
		// A far wreck first seen already up has no clock until we work it.
		WreckTracker fresh = new WreckTracker();
		fresh.observe(NEAR, ObjectID.SAILING_MERCHANT_SHIPWRECK_STUMP, 0);
		fresh.observe(FAR, ObjectID.SAILING_MERCHANT_SHIPWRECK, 0);
		assertNull(fresh.nextRise(HOOKS, RANGE, LEVEL, true, 5_000));
	}

	@Test
	public void clearForgetsEverything()
	{
		tracker.observe(NEAR, ObjectID.SAILING_MERCHANT_SHIPWRECK, 0);
		tracker.observe(NEAR, ObjectID.SAILING_MERCHANT_SHIPWRECK_STUMP, 5);
		tracker.recordAvailability(true, 1000, 1000);
		tracker.clear();
		assertEquals(0, tracker.siteCount());
		assertEquals(0, tracker.getLastSinkAt());
		assertEquals(0.5, tracker.availability(0.5, 1000, 2000), 1e-9);
	}
}
