/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CrewRosterTest
{
	private static final Crewmate JENKINS = new Crewmate(7, "Cabin Boy Jenkins", 4);
	private static final Crewmate JOLLY = new Crewmate(9, "Jolly Jim", 4);
	private static final Crewmate ADA = new Crewmate(3, "Adventurer Ada", 0);

	private final CrewRoster roster = new CrewRoster();

	@Test
	public void assignmentsDecideWhoIsOnAHook()
	{
		roster.setCrewmate(0, JENKINS);
		roster.setPosition(0, CrewAssignment.HOOK_SLOOP_1);
		roster.setCrewmate(1, JOLLY);
		roster.setPosition(1, CrewAssignment.SAILS);
		roster.setCrewmate(2, ADA);

		assertEquals(3, roster.countAboard());
		assertEquals(1, roster.countOnHooks(0));
		assertEquals(JENKINS, roster.onHooks(0).get(0));
		assertEquals(1, roster.hookSlots(0).size());
		assertEquals(0, (int) roster.hookSlots(0).get(0));
		// Jolly Jim is busy on the sails; only Ada is spare.
		assertEquals(1, roster.idle(0).size());
		assertEquals(ADA, roster.idle(0).get(0));
		assertArrayEquals(new int[]{4}, roster.deckhandinessOnHooks(0));
	}

	@Test
	public void onlySpareCrewWhoCanUseTheHookCount()
	{
		roster.setCrewmate(0, ADA);
		roster.setCrewmate(1, JOLLY);
		assertEquals(2, roster.countSpareFor(SalvagingHookTier.STEEL, 0));
		assertEquals(1, roster.countSpareFor(SalvagingHookTier.DRAGON, 0));
		assertEquals(2, roster.countSpareFor(null, 0));
		// A keg of whirlpool surprise lifts everyone to deckhandiness 2, so Ada can take a steel hook.
		assertEquals(2, roster.countSpareFor(SalvagingHookTier.STEEL, 0, 2));
		assertEquals(2, roster.countSpareFor(SalvagingHookTier.ADAMANT, 0, 2));
		assertEquals(1, roster.countSpareFor(SalvagingHookTier.RUNE, 0, 2));
		roster.setPosition(1, CrewAssignment.HOOK_SLOOP_1);
		assertEquals(0, roster.countSpareFor(SalvagingHookTier.DRAGON, 0));
		roster.setPosition(0, 22);
		assertEquals(0, roster.countSpareFor(SalvagingHookTier.STEEL, 0));
		assertFalse(roster.isIdle(0, 0));
	}

	@Test
	public void distinctDeckhandinessOnHooksIsSorted()
	{
		roster.setCrewmate(0, JOLLY);
		roster.setPosition(0, CrewAssignment.HOOK_SLOOP_2);
		roster.setCrewmate(1, new Crewmate(5, "Jobless Jim", 3));
		roster.setPosition(1, CrewAssignment.HOOK_SLOOP_1);
		roster.setCrewmate(2, JENKINS);
		roster.noteSalvage(2, 0);
		assertArrayEquals(new int[]{3, 4}, roster.deckhandinessOnHooks(0));
		assertEquals(0, roster.deckhandinessOnHooks(0).length == 0 ? 0 : roster.idle(0).size());
	}

	@Test
	public void aCrewmateHeardSalvagingCountsAsOnAHook()
	{
		roster.setCrewmate(1, JOLLY);
		assertFalse(roster.isOnHook(1, 50));
		assertEquals(1, roster.slotByName("jolly jim"));
		assertTrue(roster.noteSalvage(1, 50));
		assertTrue(roster.isOnHook(1, 50));
		assertTrue(roster.isOnHook(1, 50 + CrewRoster.HEARD_SALVAGING_TICKS));
		assertFalse(roster.isOnHook(1, 51 + CrewRoster.HEARD_SALVAGING_TICKS));
		// An assignment we do not recognise could be a hook on some boat, so the line still counts.
		roster.setPosition(1, 22);
		roster.noteSalvage(1, 60);
		assertTrue(roster.isOnHook(1, 60));
	}

	@Test
	public void beingMovedToAnotherJobOverridesWhatWasHeard()
	{
		roster.setCrewmate(1, JOLLY);
		roster.setPosition(1, CrewAssignment.HOOK_SLOOP_1);
		roster.noteSalvage(1, 50);
		roster.setPosition(1, CrewAssignment.SAILS);
		assertFalse(roster.isOnHook(1, 51));
		roster.setPosition(1, CrewAssignment.NONE);
		assertFalse(roster.isOnHook(1, 52));
		assertTrue(roster.isIdle(1, 52));
		// Keeping the same hook assignment keeps everything.
		roster.setPosition(1, CrewAssignment.HOOK_SLOOP_2);
		roster.noteSalvage(1, 53);
		roster.setPosition(1, CrewAssignment.HOOK_SLOOP_2);
		roster.setPosition(1, CrewAssignment.NONE);
		assertFalse(roster.isOnHook(1, 54));
	}

	@Test
	public void onlyTheFirstSlotsFitOnASmallBoat()
	{
		roster.setCrewmate(0, JENKINS);
		roster.setCrewmate(2, JOLLY);
		roster.setCrewmate(4, ADA);
		roster.setPosition(4, CrewAssignment.HOOK_SLOOP_2);
		roster.setBoatCapacity(2);

		assertTrue(roster.isAboard(0));
		assertTrue(roster.isAboard(2));
		assertFalse(roster.isAboard(4));
		assertEquals(2, roster.countAboard());
		assertEquals(0, roster.countOnHooks(0));
		assertEquals(-1, roster.slotByName("Adventurer Ada"));

		roster.setBoatCapacity(0);
		assertEquals(0, roster.countAboard());
	}

	@Test
	public void replacingACrewmateForgetsWhatTheOldOneSaid()
	{
		roster.setCrewmate(0, JENKINS);
		roster.noteSalvage(0, 10);
		roster.setCrewmate(0, JOLLY);
		assertFalse(roster.isOnHook(0, 11));
		roster.setCrewmate(0, JOLLY);
		roster.noteSalvage(0, 12);
		roster.setCrewmate(0, JOLLY);
		assertTrue(roster.isOnHook(0, 13));
		roster.setCrewmate(0, null);
		assertFalse(roster.noteSalvage(0, 14));
		assertEquals(0, roster.countAboard());
	}

	@Test
	public void unknownDeckhandinessFallsBackToTheWikiThenTheDefault()
	{
		assertEquals(1, ADA.effectiveDeckhandiness());
		assertEquals(4, new Crewmate(1, "cabin boy jenkins", 0).effectiveDeckhandiness());
		assertEquals(SalvageRateModel.DEFAULT_DECKHANDINESS, new Crewmate(1, "Someone New", 0).effectiveDeckhandiness());
		assertEquals(3, new Crewmate(1, "Someone New", 3).effectiveDeckhandiness());
		assertEquals("Jenkins", JENKINS.shortName());
		assertEquals("Jolly Jim", JOLLY.shortName());
		assertEquals("Ada", ADA.shortName());
		assertEquals("Siad", new Crewmate(2, "Ex-Captain Siad", 3).shortName());
		assertEquals("Crewmate", new Crewmate(2, " ", 3).shortName());
	}

	@Test(expected = IllegalArgumentException.class)
	public void rejectsSlotsThatDoNotExist()
	{
		roster.setPosition(5, 0);
	}
}
