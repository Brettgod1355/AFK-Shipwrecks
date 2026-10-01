/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

import net.runelite.api.coords.WorldPoint;

/**
 * Every dock a boat can moor at, so the sidebar can name the nearest one and route to it.
 * <p>
 * The names and points are the ones RuneLite's own World Map plugin draws its mooring icons from
 * ({@code MooringLocation}, BSD 2-Clause, see THIRD_PARTY_NOTICES.md), so they agree with the
 * icons already on the map.
 */
public enum Mooring
{
	PORT_SARIM("Port Sarim", 3050, 3192),
	THE_PANDEMONIUM("The Pandemonium", 3069, 2986),
	LANDS_END("Land's End", 1506, 3402),
	HOSIDIUS("Hosidius", 1726, 3452),
	MUSA_POINT("Musa Point", 2960, 3147),
	PORT_PISCARILLIUS("Port Piscarillius", 1845, 3687),
	RIMMINGTON("Rimmington", 2905, 3226),
	CATHERBY("Catherby", 2796, 3412),
	BRIMHAVEN("Brimhaven", 2757, 3229),
	ARDOUGNE("Ardougne", 2671, 3265),
	PORT_KHAZARD("Port Khazard", 2685, 3161),
	WITCHAVEN("Witchaven", 2746, 3304),
	ENTRANA("Entrana", 2878, 3335),
	CIVITAS_ILLA_FORTIS("Civitas illa Fortis", 1774, 3141),
	CORSAIR_COVE("Corsair Cove", 2579, 2843),
	DOGNOSE_ISLAND("Dognose Island", 3061, 2639),
	CAIRN_ISLE("Cairn Isle", 2749, 2951),
	CHINCHOMPA_ISLAND("Chinchompa Island", 1892, 3429),
	SUNSET_COAST("Sunset Coast", 1511, 2975),
	REMOTE_ISLAND("Remote Island", 2971, 2603),
	THE_SUMMER_SHORE("The Summer Shore", 3174, 2367),
	THE_LITTLE_PEARL("The Little Pearl", 3354, 2216),
	ALDARIN("Aldarin", 1452, 2970),
	VATRACHOS_ISLAND("Vatrachos Island", 1872, 2985),
	THE_ONYX_CREST("The Onyx Crest", 2997, 2288),
	RUINS_OF_UNKAH("Ruins of Unkah", 3143, 2824),
	SHIMMERING_ATOLL("Shimmering Atoll", 1557, 2771),
	VOID_KNIGHTS_OUTPOST("Void Knights' Outpost", 2651, 2678),
	PORT_ROBERTS("Port Roberts", 1860, 3306),
	ANGLERS_RETREAT("Anglers' Retreat", 2467, 2721),
	LAST_LIGHT("Last Light", 2848, 2327),
	RED_ROCK("Red Rock", 2808, 2510),
	MINOTAURS_REST("Minotaurs' Rest", 1958, 3117),
	ISLE_OF_SOULS("Isle of Souls", 2282, 2823),
	ISLE_OF_BONES("Isle of Bones", 2532, 2531),
	LAGUNA_AURORAE("Laguna Aurorae", 1202, 2733),
	CHARRED_ISLAND("Charred Island", 2660, 2395),
	TEAR_OF_THE_SOUL("Tear of the Soul", 2318, 2774),
	RELLEKKA("Rellekka", 2630, 3705),
	WYRMSCRAIG("Wyrmscraig", 2567, 2297),
	WYRMSCRAIG_CAVERN("Wyrmscraig Cavern", 2773, 8607),
	WINTUMBER_ISLAND("Wintumber Island", 2058, 2606),
	THE_CROWN_JEWEL("The Crown Jewel", 1765, 2659),
	ETCETERIA("Etceteria", 2611, 3840),
	PORT_TYRAS("Port Tyras", 2144, 3120),
	LLEDRITH_ISLAND("Lledrith Island", 2097, 3188),
	DEEPFIN_POINT("Deepfin Point", 1923, 2758),
	JATIZSO("Jatizso", 2412, 3780),
	NEITIZNOT("Neitiznot", 2308, 3783),
	RAINBOWS_END("Rainbow's End", 2344, 2270),
	PRIFDDINAS("Prifddinas", 2158, 3324),
	SUNBLEAK_ISLAND("Sunbleak Island", 2189, 2327),
	YNYSDAIL("Ynysdail", 2222, 3466),
	WATERBIRTH_ISLAND("Waterbirth Island", 2543, 3765),
	PISCATORIS("Piscatoris", 2303, 3690),
	LUNAR_ISLE("Lunar Isle", 2151, 3880),
	BUCCANEERS_HAVEN("Buccaneers' Haven", 2080, 3690),
	DRUMSTICK_ISLE("Drumstick Isle", 2150, 3530),
	WEISS("Weiss", 2860, 3972),
	BRITTLE_ISLE("Brittle Isle", 1954, 4056),
	GRIMSTONE("Grimstone", 2927, 4056);

	private final String displayName;
	private final WorldPoint point;

	Mooring(String displayName, int x, int y)
	{
		this.displayName = displayName;
		this.point = new WorldPoint(x, y, 0);
	}

	public String getDisplayName()
	{
		return displayName;
	}

	/** The mooring in the top-level world. */
	public WorldPoint getPoint()
	{
		return point;
	}

	/**
	 * Tiles from a point to this dock as the crow flies (the longer of the two axes, which is how
	 * the game counts tiles). The plane is ignored: the docks are all at sea level.
	 */
	public int tilesFrom(WorldPoint from)
	{
		return distance(from, point);
	}

	/** The dock nearest a point, or null when there is no point. */
	public static Mooring nearest(WorldPoint from)
	{
		if (from == null)
		{
			return null;
		}
		Mooring best = null;
		int bestDistance = Integer.MAX_VALUE;
		for (Mooring mooring : values())
		{
			int d = mooring.tilesFrom(from);
			if (d < bestDistance)
			{
				best = mooring;
				bestDistance = d;
			}
		}
		return best;
	}

	/** Tiles between two points in the top-level world, planes ignored. */
	public static int distance(WorldPoint a, WorldPoint b)
	{
		return Math.max(Math.abs(a.getX() - b.getX()), Math.abs(a.getY() - b.getY()));
	}
}
