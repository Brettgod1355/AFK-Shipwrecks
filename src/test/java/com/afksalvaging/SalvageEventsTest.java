/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

import com.afksalvaging.SalvageEvents.Event;
import com.afksalvaging.SalvageEvents.Source;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class SalvageEventsTest
{
	private final SalvageEvents events = new SalvageEvents();

	private static SalvageEvents.XpContext pirate(boolean playerSalvaging, int... crew)
	{
		SalvageEvents.XpContext context = new SalvageEvents.XpContext();
		context.wreck = ShipwreckType.PIRATE;
		context.playerSalvaging = playerSalvaging;
		context.crewDeckhandiness = crew;
		return context;
	}

	private static int crewEvents(List<Event> list, int deckhandiness)
	{
		int count = 0;
		for (Event event : list)
		{
			if (event.getSource() == Source.CREW && (deckhandiness == 0 || event.getDeckhandiness() == deckhandiness))
			{
				count++;
			}
		}
		return count;
	}

	@Test
	public void aLineAndItsXpAreOneEvent()
	{
		events.crewLine(4, 10);
		events.sailingXp(30, 10, pirate(false, 4));
		List<Event> out = events.drain(10);
		assertEquals(1, out.size());
		assertEquals(Source.CREW, out.get(0).getSource());
		assertEquals(4, out.get(0).getDeckhandiness());
		assertTrue(events.drain(20).isEmpty());
	}

	@Test
	public void theOrderWithinTheTickDoesNotMatter()
	{
		events.sailingXp(31, 10, pirate(false, 4));
		events.crewLine(4, 10);
		assertEquals(1, events.drain(10).size());
		events.sailingXp(30, 15, pirate(false, 4));
		events.crewLine(4, 16);
		assertEquals(1, events.drain(16).size());
		assertTrue(events.drain(30).isEmpty());
	}

	@Test
	public void theGhostIsCountedFromHisXpAlone()
	{
		events.ghostLine(10);
		events.sailingXp(30, 10, pirate(false, 4));
		List<Event> out = events.drain(13);
		assertEquals(1, crewEvents(out, 4));
		// Without a matching XP drop the wail means nothing.
		events.ghostLine(20);
		assertTrue(events.drain(30).isEmpty());
	}

	@Test
	public void ghostXpWithoutHisWailStillCounts()
	{
		events.sailingXp(30, 10, pirate(false, 4));
		assertTrue(events.drain(11).isEmpty());
		List<Event> out = events.drain(13);
		assertEquals(1, crewEvents(out, 4));
	}

	@Test
	public void unknownWreckFallsBackToWailPlusAnyXp()
	{
		SalvageEvents.XpContext unknown = new SalvageEvents.XpContext();
		events.ghostLine(10);
		events.sailingXp(7, 11, unknown);
		assertEquals(1, crewEvents(events.drain(11), 4));
		events.sailingXp(7, 20, unknown);
		assertTrue(events.drain(25).isEmpty());
	}

	@Test
	public void twoCrewInOneTickAddUp()
	{
		// Jenkins (D4, 30.4) and Jobless Jim (D3, 22.8) both hook something: 53 XP.
		events.sailingXp(53, 10, pirate(false, 3, 4));
		events.crewLine(4, 10);
		events.crewLine(3, 10);
		List<Event> out = events.drain(13);
		assertEquals(2, out.size());
		assertEquals(1, crewEvents(out, 4));
		assertEquals(1, crewEvents(out, 3));
	}

	@Test
	public void twoOfTheSameCrewmateClassAreTwoEvents()
	{
		events.sailingXp(61, 10, pirate(false, 4));
		events.crewLine(4, 10);
		events.crewLine(4, 10);
		assertEquals(2, crewEvents(events.drain(13), 4));
	}

	@Test
	public void thePlayersOwnSalvageComesFromTheInventory()
	{
		events.playerGain(2, 10);
		events.sailingXp(76, 10, pirate(true, 4));
		List<Event> out = events.drain(13);
		assertEquals(2, out.size());
		assertEquals(Source.PLAYER, out.get(0).getSource());
		assertEquals(Source.PLAYER, out.get(1).getSource());
	}

	@Test
	public void playerAndCrewInTheSameTickAreSplit()
	{
		events.playerGain(1, 10);
		events.sailingXp(106, 10, pirate(true, 4));
		events.crewLine(4, 10);
		List<Event> out = events.drain(13);
		assertEquals(2, out.size());
		assertEquals(1, crewEvents(out, 4));
	}

	@Test
	public void sortingXpIsNotSalvage()
	{
		SalvageEvents.XpContext context = pirate(false, 4);
		context.playerSorting = true;
		context.sortingXp = new double[]{31.5};
		events.sailingXp(31, 10, context);
		events.sailingXp(32, 13, context);
		assertTrue(events.drain(20).isEmpty());
		// A wail says the ghost did it, not the sorting.
		events.ghostLine(30);
		events.sailingXp(31, 30, context);
		assertEquals(1, crewEvents(events.drain(33), 4));
	}

	@Test
	public void aLineNobodyCanPlaceIsStillCounted()
	{
		events.crewLine(0, 10);
		assertTrue(events.drain(12).isEmpty());
		List<Event> out = events.drain(13);
		assertEquals(1, out.size());
		assertEquals(0, out.get(0).getDeckhandiness());
		// And it takes the deckhandiness from the XP when that arrives first.
		events.sailingXp(23, 20, pirate(false, 3));
		events.crewLine(0, 20);
		out = events.drain(20);
		assertEquals(1, out.size());
		assertEquals(3, out.get(0).getDeckhandiness());
	}

	@Test
	public void unexplainedXpIsIgnored()
	{
		events.sailingXp(500, 10, pirate(true, 4));
		events.sailingXp(1, 11, pirate(true, 4));
		assertTrue(events.drain(20).isEmpty());
		events.sailingXp(0, 21, pirate(true, 4));
		events.sailingXp(-5, 22, pirate(true, 4));
		assertTrue(events.drain(30).isEmpty());
	}

	@Test
	public void explainsSumsAllowingRoundingAndBoosts()
	{
		List<Double> amounts = Arrays.asList(76.0, 30.4, 22.8);
		assertArrayEquals(new int[]{0, 1, 0}, SalvageEvents.explain(30, amounts));
		assertArrayEquals(new int[]{0, 1, 0}, SalvageEvents.explain(31, amounts));
		assertArrayEquals(new int[]{1, 0, 0}, SalvageEvents.explain(78, amounts));
		assertArrayEquals(new int[]{1, 1, 0}, SalvageEvents.explain(106, amounts));
		assertArrayEquals(new int[]{0, 1, 1}, SalvageEvents.explain(53, amounts));
		assertArrayEquals(new int[]{0, 2, 0}, SalvageEvents.explain(61, amounts));
		assertNull(SalvageEvents.explain(15, amounts));
		assertNull(SalvageEvents.explain(40, Arrays.asList(0.0)));
	}

	@Test
	public void resetDropsEverythingPending()
	{
		events.crewLine(4, 10);
		events.ghostLine(10);
		events.playerGain(1, 10);
		events.reset();
		assertTrue(events.drain(20).isEmpty());
	}

	@Test
	public void extractorXpIsNeverSalvage()
	{
		events.ghostLine(10);
		events.sailingXp(250, 10, pirate(false, 4));
		assertTrue(events.drain(13).isEmpty());
		events.ghostLine(20);
		events.sailingXp(256, 20, new SalvageEvents.XpContext());
		assertTrue(events.drain(23).isEmpty());
		// Told to ignore XP for a couple of ticks, it does.
		events.ignoreXpUntil(32);
		events.crewLine(4, 31);
		events.sailingXp(30, 31, pirate(false, 4));
		events.sailingXp(30, 32, pirate(false, 4));
		assertEquals(1, crewEvents(events.drain(35), 4));
		events.sailingXp(30, 33, pirate(false, 4));
		assertEquals(1, crewEvents(events.drain(36), 4));
	}

	@Test
	public void countsTheGhostsUnansweredWails()
	{
		events.ghostLine(10);
		events.ghostLine(15);
		assertEquals(0, events.unmatchedGhostLines());
		events.drain(18);
		assertEquals(2, events.unmatchedGhostLines());
		events.ghostLine(20);
		events.sailingXp(30, 20, pirate(false, 4));
		assertEquals(0, events.unmatchedGhostLines());
		events.ghostLine(30);
		events.drain(40);
		assertEquals(1, events.unmatchedGhostLines());
		events.resetUnmatchedGhostLines();
		assertEquals(0, events.unmatchedGhostLines());
	}
}
