/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CrewSpeechTest
{
	@Test
	public void recognisesAFullHold()
	{
		assertTrue(CrewSpeech.reportsFullHold("The cargo hold is full. I can't salvage anything."));
		assertTrue(CrewSpeech.reportsFullHold("<col=ff0000>The cargo hold is full.</col>"));
		assertFalse(CrewSpeech.reportsFullHold("Managed to hook some salvage! I'll put it in the cargo hold."));
		assertFalse(CrewSpeech.reportsFullHold(null));
	}

	@Test
	public void recognisesCrewSalvage()
	{
		assertTrue(CrewSpeech.reportsCrewSalvage("Managed to hook some salvage! I'll put it in the cargo hold."));
		assertTrue(CrewSpeech.reportsCrewSalvage("Got one! I'll put it in the cargo hold."));
		assertFalse(CrewSpeech.reportsCrewSalvage("The cargo hold is full. I can't salvage anything."));
		assertFalse(CrewSpeech.reportsCrewSalvage("There's somethin' in the drink!"));
		assertFalse(CrewSpeech.reportsCrewSalvage(null));
	}

	@Test
	public void recognisesTheGhostsWordlessSpeech()
	{
		assertTrue(CrewSpeech.isGhostSpeech("Wooo."));
		assertTrue(CrewSpeech.isGhostSpeech("Woooo wooo."));
		assertTrue(CrewSpeech.isGhostSpeech("Woooo wooo wooooo woooo."));
		assertTrue(CrewSpeech.isGhostSpeech("<col=ffffff>Wooo!</col>"));
		assertFalse(CrewSpeech.isGhostSpeech("Woo hoo, salvage!"));
		assertFalse(CrewSpeech.isGhostSpeech("Wish I had a boat."));
		assertFalse(CrewSpeech.isGhostSpeech(""));
		assertFalse(CrewSpeech.isGhostSpeech(null));
	}

	@Test
	public void recognisesThePlayersOwnDeposit()
	{
		assertTrue(CrewSpeech.reportsPlayerDeposit("You deposit some cargo into the cargo hold."));
		assertFalse(CrewSpeech.reportsPlayerDeposit("Your crew pack the cargo they were holding into the cargo hold."));
		assertFalse(CrewSpeech.reportsPlayerDeposit(null));
	}
}
