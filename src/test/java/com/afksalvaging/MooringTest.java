/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

import java.util.HashSet;
import java.util.function.Function;
import java.util.Set;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.coords.WorldPoint;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class MooringTest
{
	@Test
	public void everyDockOnTheMapIsListedOnce()
	{
		// RuneLite's World Map plugin draws 61 mooring icons (1.13.1); the bank boat is the one entry that is no dock.
		int docks = 0;
		for (Mooring dock : Mooring.values())
		{
			if (dock.isDock())
			{
				docks++;
			}
		}
		assertEquals(61, docks);
		assertEquals(62, Mooring.values().length);
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
		assertNull(Mooring.nearest((WorldPoint) null));
		// Alongside the bank boat it is still never the nearest dock: there is nowhere to step off.
		WorldPoint besideBankBoat = new WorldPoint(2250, 2537, 0);
		assertNotEquals(Mooring.BANK_BOAT, Mooring.nearest(besideBankBoat));
		assertNotEquals(Mooring.BANK_BOAT, Mooring.nearestUsable(besideBankBoat, 99, quest -> QuestState.FINISHED));
	}

	@Test
	public void everyDockHasALevelAndTheQuestedOnesNameTheirQuest()
	{
		for (Mooring dock : Mooring.values())
		{
			assertTrue(dock.name(), dock.getSailingLevel() >= 1 && dock.getSailingLevel() <= 99);
			assertTrue(dock.name(), dock.requirementText().startsWith("level " + dock.getSailingLevel() + " Sailing"));
		}
		assertEquals(1, Mooring.PORT_SARIM.getSailingLevel());
		assertEquals(Quest.SONG_OF_THE_ELVES, Mooring.PRIFDDINAS.getQuest());
		assertEquals("level 70 Sailing, Song of the Elves", Mooring.PRIFDDINAS.requirementText());
		assertEquals("level 45 Sailing, Troubled Tortugans started", Mooring.THE_SUMMER_SHORE.requirementText());
		assertEquals("level 62 Sailing, Fallen From Grace started, raft or skiff only", Mooring.WYRMSCRAIG_CAVERN.requirementText());
		assertEquals("level 5 Sailing, must have visited Kourend", Mooring.LANDS_END.requirementText());
	}

	@Test
	public void usableMeansTheUnboostedLevelAndTheQuestState()
	{
		Function<Quest, QuestState> nothingDone = quest -> QuestState.NOT_STARTED;
		Function<Quest, QuestState> allStarted = quest -> QuestState.IN_PROGRESS;
		Function<Quest, QuestState> allDone = quest -> QuestState.FINISHED;
		assertTrue(Mooring.PORT_SARIM.usable(1, nothingDone));
		assertFalse("level 70 needed", Mooring.PRIFDDINAS.usable(69, allDone));
		assertFalse("quest must be finished", Mooring.PRIFDDINAS.usable(70, allStarted));
		assertTrue(Mooring.PRIFDDINAS.usable(70, allDone));
		assertFalse(Mooring.THE_SUMMER_SHORE.usable(45, nothingDone));
		assertTrue("partial completion: started is enough", Mooring.THE_SUMMER_SHORE.usable(45, allStarted));
		// A condition the client cannot see is taken as met.
		assertTrue(Mooring.LANDS_END.usable(5, nothingDone));
	}

	@Test
	public void theNearestUsableDockSkipsWhatThePlayerCannotUse()
	{
		WorldPoint nearPrifddinas = new WorldPoint(2160, 3330, 0);
		Function<Quest, QuestState> nothingDone = quest -> QuestState.NOT_STARTED;
		assertEquals(Mooring.PRIFDDINAS, Mooring.nearest(nearPrifddinas));
		Mooring usable = Mooring.nearestUsable(nearPrifddinas, 99, nothingDone);
		assertTrue(usable != Mooring.PRIFDDINAS);
		assertTrue(usable.usable(99, nothingDone));
		assertNull("nothing at level 0", Mooring.nearestUsable(nearPrifddinas, 0, nothingDone));
		assertNull(Mooring.nearestUsable((WorldPoint) null, 99, nothingDone));
	}

	@Test
	public void thePlaneDoesNotCount()
	{
		assertEquals(0, Mooring.PORT_SARIM.tilesFrom(new WorldPoint(3050, 3192, 1)));
	}

	@Test
	public void portsAreTheDocksWhereTheCrewBankTheHold()
	{
		int ports = 0;
		for (Mooring mooring : Mooring.values())
		{
			if (mooring.banksCargo())
			{
				ports++;
			}
		}
		assertEquals("the wiki's Bank deposit box column, 2026-10-03, plus the bank boat", 22, ports);
		assertTrue(Mooring.PORT_SARIM.banksCargo());
		assertTrue(Mooring.ETCETERIA.banksCargo());
		assertTrue("an island with a bank is still not a port", !Mooring.RELLEKKA.banksCargo());
		assertTrue(!Mooring.WYRMSCRAIG_CAVERN.banksCargo());
		// The bank boat banks the hold but is no dock: never the nearest dock you can use.
		assertTrue(Mooring.BANK_BOAT.banksCargo());
		assertTrue(!Mooring.BANK_BOAT.isDock());
		assertTrue(Mooring.RELLEKKA.isDock());
	}
}
