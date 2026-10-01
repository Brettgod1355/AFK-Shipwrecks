/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import net.runelite.api.coords.WorldPoint;

/**
 * Which spots the sidebar lists and in what order: the filter from the dropdown, favourites first,
 * then either the catalogue order (by wreck) or nearest the player first. Pure, so it can be tested
 * without Swing.
 */
public final class SpotList
{
	/** Dropdown choices that are not a wreck kind. Stored by name, so they must not be renamed. */
	public static final String FILTER_ALL = "ALL";
	public static final String FILTER_MY_LEVEL = "MY_LEVEL";
	public static final String FILTER_FAVOURITES = "FAVOURITES";

	private SpotList()
	{
	}

	/** The wreck a filter key names, or null for the choices that are not a wreck. */
	public static ShipwreckType wreckOf(String filterKey)
	{
		for (ShipwreckType type : ShipwreckType.values())
		{
			if (type.name().equals(filterKey))
			{
				return type;
			}
		}
		return null;
	}

	/** A filter key that is one of the known choices, or ALL for anything else (an old or mistyped setting). */
	public static String validFilter(String filterKey)
	{
		if (FILTER_MY_LEVEL.equals(filterKey) || FILTER_FAVOURITES.equals(filterKey) || wreckOf(filterKey) != null)
		{
			return filterKey;
		}
		return FILTER_ALL;
	}

	/**
	 * The spots to list, in order.
	 *
	 * @param filterKey    ALL, MY_LEVEL, FAVOURITES or a {@link ShipwreckType} name
	 * @param sailingLevel the player's level, or 0 when unknown (MY_LEVEL then lists nothing)
	 * @param favourites   spots pinned to the top
	 * @param nearestFirst sort by distance from {@code from} rather than by wreck
	 * @param from         where the player is, or null when unknown (then the order is by wreck)
	 */
	public static List<SalvagingSpot> arrange(String filterKey, int sailingLevel, Set<SalvagingSpot> favourites,
		boolean nearestFirst, WorldPoint from)
	{
		List<SalvagingSpot> spots = new ArrayList<>();
		ShipwreckType wreck = wreckOf(filterKey);
		for (SalvagingSpot spot : SalvagingSpot.values())
		{
			if (wreck != null && spot.getWreck() != wreck)
			{
				continue;
			}
			if (FILTER_MY_LEVEL.equals(filterKey) && sailingLevel < spot.getSailingLevel())
			{
				continue;
			}
			if (FILTER_FAVOURITES.equals(filterKey) && !favourites.contains(spot))
			{
				continue;
			}
			spots.add(spot);
		}
		Comparator<SalvagingSpot> order = Comparator.comparing((SalvagingSpot s) -> favourites.contains(s) ? 0 : 1);
		if (nearestFirst && from != null)
		{
			order = order.thenComparingInt(s -> Mooring.distance(from, s.getPoint()));
		}
		spots.sort(order.thenComparingInt(Enum::ordinal));
		return spots;
	}

	/** Favourites as stored: enum names joined with commas. */
	public static String encodeFavourites(Set<SalvagingSpot> favourites)
	{
		StringBuilder out = new StringBuilder();
		for (SalvagingSpot spot : SalvagingSpot.values())
		{
			if (favourites.contains(spot))
			{
				out.append(out.length() == 0 ? "" : ",").append(spot.name());
			}
		}
		return out.toString();
	}

	/** Favourites from storage; unknown names (a renamed spot) are dropped rather than failing. */
	public static Set<SalvagingSpot> decodeFavourites(String stored)
	{
		Set<SalvagingSpot> favourites = EnumSet.noneOf(SalvagingSpot.class);
		if (stored == null || stored.isEmpty())
		{
			return favourites;
		}
		for (String name : stored.split(","))
		{
			SalvagingSpot spot = spotNamed(name.trim());
			if (spot != null)
			{
				favourites.add(spot);
			}
		}
		return favourites;
	}

	/** The spot with this enum name, or null. */
	public static SalvagingSpot spotNamed(String name)
	{
		if (name == null || name.isEmpty())
		{
			return null;
		}
		for (SalvagingSpot spot : SalvagingSpot.values())
		{
			if (spot.name().equals(name))
			{
				return spot;
			}
		}
		return null;
	}
}
