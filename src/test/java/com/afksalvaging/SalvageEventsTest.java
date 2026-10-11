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
	public void lateXpIsPayForALineAlreadyCounted()
	{
		// In game (2026-10-10): a line at tick 28 with no XP, then at tick 34 one drop of two salvages' XP
		// (that line's and an earlier one's). The line's XP must not count a second time.
		events.crewLine(4, 28);
		assertEquals(1, crewEvents(events.drain(31), 4));
		events.sailingXp(60, 34, pirate(false, 4));
		assertEquals(1, crewEvents(events.drain(37), 4));

		// Past the late window the XP is a salvage of its own.
		events.crewLine(4, 100);
		assertEquals(1, crewEvents(events.drain(103), 4));
		events.sailingXp(30, 100 + SalvageEvents.LATE_XP_TICKS + 1, pirate(false, 4));
		assertEquals(1, crewEvents(events.drain(130), 4));
	}

	@Test
	public void twoCrewSalvagingTogetherAreTwoWhenTheirXpIsOnTimeOrLate()
	{
		// Both say their line and the XP comes in one drop the same tick.
		events.crewLine(4, 10);
		events.crewLine(4, 10);
		events.sailingXp(60, 10, pirate(false, 4, 4));
		assertEquals(2, crewEvents(events.drain(13), 4));

		// Both say their line; the XP for both comes late in one drop.
		events.crewLine(4, 30);
		events.crewLine(4, 30);
		assertEquals(2, crewEvents(events.drain(33), 4));
		events.sailingXp(60, 36, pirate(false, 4, 4));
		assertTrue(events.drain(40).isEmpty());

		// Only one says a line (the ghost never does); the XP for both comes at once.
		events.crewLine(4, 60);
		events.sailingXp(60, 60, pirate(false, 4, 4));
		assertEquals(2, crewEvents(events.drain(63), 4));

		// A level 3 and a level 4 at once: the late XP only pays the line of the same level.
		events.crewLine(3, 90);
		assertEquals(1, crewEvents(events.drain(93), 3));
		events.sailingXp(30, 95, pirate(false, 3, 4));
		assertEquals(1, crewEvents(events.drain(99), 4));
	}

	@Test
	public void aPairIsDatedByItsEarlierHalf()
	{
		// In game (2026-10-10): the line at tick 958 and the XP at 959 are one salvage that landed at 958.
		events.crewLine(3, 958);
		events.sailingXp(23, 959, pirate(false, 3));
		List<Event> out = events.drain(959);
		assertEquals(1, out.size());
		assertEquals(958, out.get(0).getTick());
		events.sailingXp(23, 970, pirate(false, 3));
		events.crewLine(3, 971);
		out = events.drain(971);
		assertEquals(1, out.size());
		assertEquals(970, out.get(0).getTick());
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
	public void ofSplitsWithAsFewTermsTheClosestWins()
	{
		// In game (2026-10-10): 124 XP while sorting, with a level 3 and a level 4 crewmate. A level 3
		// salvage and a sort (123.6) beats two level 4 salvages (129.6), which also fit the tolerance.
		List<Double> amounts = Arrays.asList(48.6, 64.8, 75.0);
		assertArrayEquals(new int[]{1, 0, 1}, SalvageEvents.explain(124, amounts));
		assertArrayEquals(new int[]{0, 2, 0}, SalvageEvents.explain(130, amounts));
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
