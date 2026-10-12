/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import net.runelite.api.coords.WorldPoint;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class SpotListTest
{
	private static final Set<SalvagingSpot> NONE = EnumSet.noneOf(SalvagingSpot.class);

	@Test
	public void allListsEverySpotInCatalogOrder()
	{
		List<SalvagingSpot> spots = SpotList.arrange(SpotList.FILTER_ALL, 0, NONE, false, (WorldPoint) null);
		assertEquals(SalvagingSpot.values().length, spots.size());
		assertEquals(SalvagingSpot.SMALL_PANDEMONIUM, spots.get(0));
	}

	@Test
	public void aWreckFilterKeepsOnlyThatWreck()
	{
		List<SalvagingSpot> spots = SpotList.arrange("BARRACUDA", 0, NONE, false, (WorldPoint) null);
		assertEquals(6, spots.size());
		for (SalvagingSpot spot : spots)
		{
			assertEquals(ShipwreckType.BARRACUDA, spot.getWreck());
		}
	}

	@Test
	public void myLevelKeepsWhatTheLevelAllowsAndNothingWhenLoggedOut()
	{
		for (SalvagingSpot spot : SpotList.arrange(SpotList.FILTER_MY_LEVEL, 35, NONE, false, (WorldPoint) null))
		{
			assertTrue(spot.name(), spot.getSailingLevel() <= 35);
		}
		assertEquals(11, SpotList.arrange(SpotList.FILTER_MY_LEVEL, 35, NONE, false, (WorldPoint) null).size());
		assertTrue(SpotList.arrange(SpotList.FILTER_MY_LEVEL, 0, NONE, false, (WorldPoint) null).isEmpty());
	}

	@Test
	public void favoritesComeFirstAndCanBeTheWholeList()
	{
		Set<SalvagingSpot> favorites = EnumSet.of(SalvagingSpot.MERCHANT_SUNBLEAK_ISLAND, SalvagingSpot.LARGE_WEISS);
		List<SalvagingSpot> all = SpotList.arrange(SpotList.FILTER_ALL, 0, favorites, false, (WorldPoint) null);
		assertEquals(SalvagingSpot.LARGE_WEISS, all.get(0));
		assertEquals(SalvagingSpot.MERCHANT_SUNBLEAK_ISLAND, all.get(1));
		assertEquals(SalvagingSpot.SMALL_PANDEMONIUM, all.get(2));
		assertEquals(2, SpotList.arrange(SpotList.FILTER_FAVORITES, 0, favorites, false, (WorldPoint) null).size());
	}

	@Test
	public void nearestFirstSortsByDistanceButFavoritesStayOnTop()
	{
		WorldPoint atUnkah = Mooring.RUINS_OF_UNKAH.getPoint();
		List<SalvagingSpot> spots = SpotList.arrange(SpotList.FILTER_ALL, 0, NONE, true, atUnkah);
		assertEquals(SalvagingSpot.BARRACUDA_UNKAH, spots.get(0));
		for (int i = 1; i < spots.size(); i++)
		{
			assertTrue(Mooring.distance(atUnkah, spots.get(i - 1).getPoint()) <= Mooring.distance(atUnkah, spots.get(i).getPoint()));
		}
		Set<SalvagingSpot> favorite = EnumSet.of(SalvagingSpot.LARGE_WEISS);
		assertEquals(SalvagingSpot.LARGE_WEISS, SpotList.arrange(SpotList.FILTER_ALL, 0, favorite, true, atUnkah).get(0));
		// Without a position, nearest first falls back to the catalog order.
		assertEquals(SalvagingSpot.SMALL_PANDEMONIUM, SpotList.arrange(SpotList.FILTER_ALL, 0, NONE, true, (WorldPoint) null).get(0));
	}

	@Test
	public void storedChoicesSurviveARoundTripAndBadValues()
	{
		Set<SalvagingSpot> favorites = EnumSet.of(SalvagingSpot.BARRACUDA_UNKAH, SalvagingSpot.SMALL_LANDS_END);
		assertEquals("SMALL_LANDS_END,BARRACUDA_UNKAH", SpotList.encodeFavorites(favorites));
		assertEquals(favorites, SpotList.decodeFavorites("SMALL_LANDS_END,BARRACUDA_UNKAH"));
		assertEquals(EnumSet.of(SalvagingSpot.BARRACUDA_UNKAH), SpotList.decodeFavorites("GONE_SPOT, BARRACUDA_UNKAH"));
		assertTrue(SpotList.decodeFavorites(null).isEmpty());
		assertTrue(SpotList.decodeFavorites("").isEmpty());
		assertEquals(SpotList.FILTER_ALL, SpotList.validFilter("whatever"));
		assertEquals("LARGE", SpotList.validFilter("LARGE"));
		assertEquals(SpotList.FILTER_FAVORITES, SpotList.validFilter(SpotList.FILTER_FAVORITES));
		assertNull(SpotList.spotNamed("nope"));
		assertEquals(SalvagingSpot.LARGE_WEISS, SpotList.spotNamed("LARGE_WEISS"));
	}
}
