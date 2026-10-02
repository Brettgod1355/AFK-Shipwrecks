/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

import java.util.function.Function;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.coords.WorldPoint;

/**
 * Every dock a boat can moor at, so the sidebar can name the nearest one and route to it.
 * <p>
 * The names and points are the ones RuneLite's own World Map plugin draws its mooring icons from
 * ({@code MooringLocation}, BSD 2-Clause, see THIRD_PARTY_NOTICES.md), so they agree with the
 * icons already on the map. The Sailing level and quest each dock needs are the figures on the
 * OSRS Wiki's Mooring point page (read 2026-10-02; Last Light from its own page), credited in the
 * same notices.
 */
public enum Mooring
{
	PORT_SARIM("Port Sarim", 3050, 3192, 1, null, false, null),
	THE_PANDEMONIUM("The Pandemonium", 3069, 2986, 1, null, false, null),
	LANDS_END("Land's End", 1506, 3402, 5, null, false, "must have visited Kourend"),
	HOSIDIUS("Hosidius", 1726, 3452, 5, null, false, "must have visited Kourend"),
	MUSA_POINT("Musa Point", 2960, 3147, 10, null, false, null),
	PORT_PISCARILLIUS("Port Piscarillius", 1845, 3687, 15, null, false, "must have visited Kourend"),
	RIMMINGTON("Rimmington", 2905, 3226, 18, null, false, null),
	CATHERBY("Catherby", 2796, 3412, 20, null, false, null),
	BRIMHAVEN("Brimhaven", 2757, 3229, 25, null, false, null),
	ARDOUGNE("Ardougne", 2671, 3265, 28, null, false, null),
	PORT_KHAZARD("Port Khazard", 2685, 3161, 30, null, false, null),
	WITCHAVEN("Witchaven", 2746, 3304, 34, null, false, null),
	ENTRANA("Entrana", 2878, 3335, 36, null, false, "no prohibited items worn"),
	CIVITAS_ILLA_FORTIS("Civitas illa Fortis", 1774, 3141, 38, Quest.CHILDREN_OF_THE_SUN, true, "must have visited Varlamore"),
	CORSAIR_COVE("Corsair Cove", 2579, 2843, 40, Quest.THE_CORSAIR_CURSE, true, null),
	DOGNOSE_ISLAND("Dognose Island", 3061, 2639, 40, null, false, null),
	CAIRN_ISLE("Cairn Isle", 2749, 2951, 42, null, false, null),
	CHINCHOMPA_ISLAND("Chinchompa Island", 1892, 3429, 42, null, false, null),
	SUNSET_COAST("Sunset Coast", 1511, 2975, 44, Quest.CHILDREN_OF_THE_SUN, true, "must have visited Varlamore"),
	REMOTE_ISLAND("Remote Island", 2971, 2603, 45, null, false, null),
	THE_SUMMER_SHORE("The Summer Shore", 3174, 2367, 45, Quest.TROUBLED_TORTUGANS, false, null),
	THE_LITTLE_PEARL("The Little Pearl", 3354, 2216, 45, Quest.TROUBLED_TORTUGANS, false, null),
	ALDARIN("Aldarin", 1452, 2970, 46, Quest.CHILDREN_OF_THE_SUN, true, "must have visited Varlamore"),
	VATRACHOS_ISLAND("Vatrachos Island", 1872, 2985, 46, Quest.CHILDREN_OF_THE_SUN, true, null),
	THE_ONYX_CREST("The Onyx Crest", 2997, 2288, 47, null, false, null),
	RUINS_OF_UNKAH("Ruins of Unkah", 3143, 2824, 48, null, false, null),
	SHIMMERING_ATOLL("Shimmering Atoll", 1557, 2771, 49, null, false, null),
	VOID_KNIGHTS_OUTPOST("Void Knights' Outpost", 2651, 2678, 50, null, false, null),
	PORT_ROBERTS("Port Roberts", 1860, 3306, 50, null, false, null),
	ANGLERS_RETREAT("Anglers' Retreat", 2467, 2721, 51, null, false, null),
	LAST_LIGHT("Last Light", 2848, 2327, 52, Quest.THE_RED_REEF, false, null),
	RED_ROCK("Red Rock", 2808, 2510, 52, Quest.THE_RED_REEF, false, null),
	MINOTAURS_REST("Minotaurs' Rest", 1958, 3117, 54, Quest.CHILDREN_OF_THE_SUN, true, null),
	ISLE_OF_SOULS("Isle of Souls", 2282, 2823, 55, null, false, null),
	ISLE_OF_BONES("Isle of Bones", 2532, 2531, 56, null, false, null),
	LAGUNA_AURORAE("Laguna Aurorae", 1202, 2733, 58, null, false, null),
	CHARRED_ISLAND("Charred Island", 2660, 2395, 60, null, false, null),
	TEAR_OF_THE_SOUL("Tear of the Soul", 2318, 2774, 61, null, false, null),
	RELLEKKA("Rellekka", 2630, 3705, 62, Quest.THE_FREMENNIK_TRIALS, true, null),
	WYRMSCRAIG("Wyrmscraig", 2567, 2297, 62, null, false, null),
	WYRMSCRAIG_CAVERN("Wyrmscraig Cavern", 2773, 8607, 62, Quest.FALLEN_FROM_GRACE, false, "raft or skiff only"),
	WINTUMBER_ISLAND("Wintumber Island", 2058, 2606, 63, null, false, null),
	THE_CROWN_JEWEL("The Crown Jewel", 1765, 2659, 64, null, false, null),
	ETCETERIA("Etceteria", 2611, 3840, 65, Quest.ROYAL_TROUBLE, true, null),
	PORT_TYRAS("Port Tyras", 2144, 3120, 66, Quest.REGICIDE, true, null),
	LLEDRITH_ISLAND("Lledrith Island", 2097, 3188, 66, null, false, null),
	DEEPFIN_POINT("Deepfin Point", 1923, 2758, 67, null, false, null),
	JATIZSO("Jatizso", 2412, 3780, 68, Quest.THE_FREMENNIK_ISLES, true, null),
	NEITIZNOT("Neitiznot", 2308, 3783, 68, Quest.THE_FREMENNIK_ISLES, true, null),
	RAINBOWS_END("Rainbow's End", 2344, 2270, 69, null, false, null),
	PRIFDDINAS("Prifddinas", 2158, 3324, 70, Quest.SONG_OF_THE_ELVES, true, null),
	SUNBLEAK_ISLAND("Sunbleak Island", 2189, 2327, 72, null, false, null),
	YNYSDAIL("Ynysdail", 2222, 3466, 73, null, false, null),
	WATERBIRTH_ISLAND("Waterbirth Island", 2543, 3765, 74, Quest.THE_FREMENNIK_TRIALS, true, null),
	PISCATORIS("Piscatoris", 2303, 3690, 75, Quest.SWAN_SONG, true, null),
	LUNAR_ISLE("Lunar Isle", 2151, 3880, 76, Quest.LUNAR_DIPLOMACY, true, null),
	BUCCANEERS_HAVEN("Buccaneers' Haven", 2080, 3690, 76, null, false, null),
	DRUMSTICK_ISLE("Drumstick Isle", 2150, 3530, 79, null, false, null),
	WEISS("Weiss", 2860, 3972, 80, Quest.MAKING_FRIENDS_WITH_MY_ARM, true, null),
	BRITTLE_ISLE("Brittle Isle", 1954, 4056, 81, null, false, null),
	GRIMSTONE("Grimstone", 2927, 4056, 87, null, false, null);

	private final String displayName;
	private final WorldPoint point;
	private final int sailingLevel;
	private final Quest quest;
	private final boolean questComplete;
	private final String note;

	/**
	 * @param sailingLevel  Sailing level to disembark here, not boostable (wiki: Mooring point)
	 * @param quest         the quest that gates the dock, or null
	 * @param questComplete whether the quest must be finished (true) or only started (false)
	 * @param note          a condition the client cannot check, shown to the player, or null
	 */
	Mooring(String displayName, int x, int y, int sailingLevel, Quest quest, boolean questComplete, String note)
	{
		this.displayName = displayName;
		this.point = new WorldPoint(x, y, 0);
		this.sailingLevel = sailingLevel;
		this.quest = quest;
		this.questComplete = questComplete;
		this.note = note;
	}

	public int getSailingLevel()
	{
		return sailingLevel;
	}

	public Quest getQuest()
	{
		return quest;
	}

	/** What the dock needs, for the player: {@code level 70 Sailing, Song of the Elves}. */
	public String requirementText()
	{
		StringBuilder text = new StringBuilder("level " + sailingLevel + " Sailing");
		if (quest != null)
		{
			text.append(", ").append(quest.getName()).append(questComplete ? "" : " started");
		}
		if (note != null)
		{
			text.append(", ").append(note);
		}
		return text.toString();
	}

	/**
	 * Whether the player can disembark here, as far as the client can tell: the unboosted level,
	 * and the quest's state. Conditions the client cannot check (a first visit, items worn, the
	 * boat type) are taken as met and shown in {@link #requirementText()} instead.
	 */
	public boolean usable(int realSailingLevel, Function<Quest, QuestState> questState)
	{
		if (realSailingLevel < sailingLevel)
		{
			return false;
		}
		if (quest == null)
		{
			return true;
		}
		QuestState state = questState.apply(quest);
		return questComplete ? state == QuestState.FINISHED : state != QuestState.NOT_STARTED;
	}

	/** The nearest dock the player can disembark at, or null when none is. */
	public static Mooring nearestUsable(WorldPoint from, int realSailingLevel, Function<Quest, QuestState> questState)
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
			if (d < bestDistance && mooring.usable(realSailingLevel, questState))
			{
				best = mooring;
				bestDistance = d;
			}
		}
		return best;
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
