/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

import java.util.ArrayList;
import java.util.List;
import net.runelite.api.coords.WorldPoint;

/**
 * Every shipwreck salvaging hotspot on the world map: the wreck it yields and where it is.
 * <p>
 * The hotspot coordinates and the salvage names are the ones RuneLite's own World Map plugin uses
 * for its salvaging spot icons ({@code SalvagingSpotLocation}, BSD 2-Clause, see
 * THIRD_PARTY_NOTICES.md). The "where" of each spot was worked out from the same plugin's list of
 * moorings, nearest port first, so a spot can be told from the others of its kind. A hotspot is an
 * area holding several wreck sites, so the point is the middle of the area rather than one wreck.
 */
public enum SalvagingSpot
{
	SMALL_PANDEMONIUM(ShipwreckType.SMALL, 3110, 2948, 55, "south-east", "The Pandemonium"),
	SMALL_LANDS_END(ShipwreckType.SMALL, 1589, 3323, 114, "south-east", "Land's End"),

	FISHERMAN_WITCHAVEN(ShipwreckType.FISHERMAN, 2782, 3319, 39, "north-east", "Witchaven"),
	FISHERMAN_CAIRN_ISLE(ShipwreckType.FISHERMAN, 2698, 3047, 108, "north-west", "Cairn Isle"),
	FISHERMAN_PISCARILLIUS(ShipwreckType.FISHERMAN, 1903, 3835, 158, "north", "Port Piscarillius"),

	BARRACUDA_PANDEMONIUM(ShipwreckType.BARRACUDA, 2996, 2917, 100, "south-west", "The Pandemonium"),
	BARRACUDA_UNKAH(ShipwreckType.BARRACUDA, 3123, 2808, 25, "south-west", "Ruins of Unkah"),
	BARRACUDA_CORSAIR_COVE(ShipwreckType.BARRACUDA, 2447, 3021, 221, "north-west", "Corsair Cove"),
	BARRACUDA_ANGLERS_RETREAT(ShipwreckType.BARRACUDA, 2478, 2823, 102, "north", "Anglers' Retreat"),
	BARRACUDA_PRIFDDINAS(ShipwreckType.BARRACUDA, 2142, 3350, 30, "north-west", "Prifddinas"),
	BARRACUDA_YNYSDAIL(ShipwreckType.BARRACUDA, 2222, 3444, 22, "south", "Ynysdail"),

	LARGE_SHIMMERING_ATOLL(ShipwreckType.LARGE, 1563, 2679, 92, "south", "Shimmering Atoll"),
	LARGE_CROWN_JEWEL(ShipwreckType.LARGE, 1886, 2508, 193, "south-east", "The Crown Jewel"),
	LARGE_RAINBOWS_END(ShipwreckType.LARGE, 2459, 2144, 170, "south-east", "Rainbow's End"),
	LARGE_WEISS(ShipwreckType.LARGE, 2845, 4030, 59, "north", "Weiss"),
	LARGE_LITTLE_PEARL(ShipwreckType.LARGE, 3394, 2138, 87, "south-east", "The Little Pearl"),

	PIRATE_ISLE_OF_BONES(ShipwreckType.PIRATE, 2663, 2544, 131, "east", "Isle of Bones"),
	PIRATE_DEEPFIN_POINT(ShipwreckType.PIRATE, 1826, 2842, 128, "north-west", "Deepfin Point"),
	PIRATE_BUCCANEERS_HAVEN(ShipwreckType.PIRATE, 2116, 3655, 50, "south-east", "Buccaneers' Haven"),
	PIRATE_LUNAR_ISLE(ShipwreckType.PIRATE, 2193, 3843, 55, "south-east", "Lunar Isle"),

	MERCENARY_CROWN_JEWEL_NEAR(ShipwreckType.MERCENARY, 1688, 2399, 271, "south", "The Crown Jewel"),
	MERCENARY_CROWN_JEWEL_FAR(ShipwreckType.MERCENARY, 1747, 2209, 450, "south", "The Crown Jewel"),
	MERCENARY_SUNBLEAK_ISLAND(ShipwreckType.MERCENARY, 2140, 2212, 125, "south-west", "Sunbleak Island"),

	FREMENNIK_BRITTLE_ISLE(ShipwreckType.FREMENNIK, 1769, 4124, 197, "west", "Brittle Isle"),
	FREMENNIK_LUNAR_ISLE(ShipwreckType.FREMENNIK, 2272, 4129, 276, "north-east", "Lunar Isle"),
	FREMENNIK_ETCETERIA(ShipwreckType.FREMENNIK, 2591, 4128, 288, "north", "Etceteria"),

	MERCHANT_LAGUNA_AURORAE(ShipwreckType.MERCHANT, 1069, 2962, 264, "north-west", "Laguna Aurorae"),
	MERCHANT_BRITTLE_ISLE(ShipwreckType.MERCHANT, 1820, 3989, 149, "south-west", "Brittle Isle"),
	MERCHANT_SUNBLEAK_ISLAND(ShipwreckType.MERCHANT, 1888, 2150, 349, "south-west", "Sunbleak Island");

	private final ShipwreckType wreck;
	private final WorldPoint point;
	private final int tilesFromPort;
	private final String bearing;
	private final String port;

	SalvagingSpot(ShipwreckType wreck, int x, int y, int tilesFromPort, String bearing, String port)
	{
		this.wreck = wreck;
		this.point = new WorldPoint(x, y, 0);
		this.tilesFromPort = tilesFromPort;
		this.bearing = bearing;
		this.port = port;
	}

	public ShipwreckType getWreck()
	{
		return wreck;
	}

	/** The middle of the hotspot in the top-level world. */
	public WorldPoint getPoint()
	{
		return point;
	}

	/** Sailing level needed to salvage here. */
	public int getSailingLevel()
	{
		return wreck.getSailingLevel();
	}

	/** The name the game gives the salvage and the map icon, for example {@code Barracuda salvage}. */
	public String getSalvageName()
	{
		return salvageName(wreck);
	}

	/** The nearest port by sailing distance. */
	public String getPort()
	{
		return port;
	}

	/** Where the spot is from its nearest port, for example {@code 25 tiles south-west of Ruins of Unkah}. */
	public String getWhere()
	{
		return tilesFromPort + " tiles " + bearing + " of " + port;
	}

	/** Two lines for the world map: what and what level, then where. */
	public String getTooltip()
	{
		return getSalvageName() + " - Level " + getSailingLevel() + "<br>" + getWhere();
	}

	/** What the game calls a wreck's salvage, which is also how its map icon is named. */
	public static String salvageName(ShipwreckType type)
	{
		switch (type)
		{
			case SMALL:
				return "Small salvage";
			case FISHERMAN:
				return "Fishy salvage";
			case BARRACUDA:
				return "Barracuda salvage";
			case LARGE:
				return "Large salvage";
			case PIRATE:
				return "Plundered salvage";
			case MERCENARY:
				return "Martial salvage";
			case FREMENNIK:
				return "Fremennik salvage";
			case MERCHANT:
				return "Opulent salvage";
			default:
				return type.getDisplayName() + " salvage";
		}
	}

	/** The spots that yield this wreck, in map order; every spot when the type is null. */
	public static List<SalvagingSpot> forWreck(ShipwreckType type)
	{
		List<SalvagingSpot> list = new ArrayList<>();
		for (SalvagingSpot spot : values())
		{
			if (type == null || spot.wreck == type)
			{
				list.add(spot);
			}
		}
		return list;
	}

	/** The spot whose middle is nearest to the point, or null without a point. */
	public static SalvagingSpot nearest(WorldPoint from)
	{
		if (from == null)
		{
			return null;
		}
		SalvagingSpot best = null;
		long bestDistance = Long.MAX_VALUE;
		for (SalvagingSpot spot : values())
		{
			long dx = spot.point.getX() - from.getX();
			long dy = spot.point.getY() - from.getY();
			long distance = dx * dx + dy * dy;
			if (distance < bestDistance)
			{
				bestDistance = distance;
				best = spot;
			}
		}
		return best;
	}
}
