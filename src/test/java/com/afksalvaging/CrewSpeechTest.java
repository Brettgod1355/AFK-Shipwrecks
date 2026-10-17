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
	public void recognizesAFullHold()
	{
		assertTrue(CrewSpeech.reportsFullHold("The cargo hold is full. I can't salvage anything."));
		assertTrue(CrewSpeech.reportsFullHold("<col=ff0000>The cargo hold is full.</col>"));
		assertFalse(CrewSpeech.reportsFullHold("Managed to hook some salvage! I'll put it in the cargo hold."));
		assertFalse(CrewSpeech.reportsFullHold(null));
	}

	@Test
	public void recognizesCrewSalvage()
	{
		assertTrue(CrewSpeech.reportsCrewSalvage("Managed to hook some salvage! I'll put it in the cargo hold."));
		assertTrue(CrewSpeech.reportsCrewSalvage("Got one! I'll put it in the cargo hold."));
		assertFalse(CrewSpeech.reportsCrewSalvage("The cargo hold is full. I can't salvage anything."));
		assertFalse(CrewSpeech.reportsCrewSalvage("There's somethin' in the drink!"));
		assertFalse(CrewSpeech.reportsCrewSalvage(null));
	}

	@Test
	public void recognizesTheGhostsWordlessSpeech()
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
	public void recognizesTheOrdersPuttingACrewmateOnAHookOrToRest()
	{
		assertTrue(CrewSpeech.isCrewOrder("Ex-Captain Siad, take control of the starboard side rune salvaging hook!"));
		assertTrue(CrewSpeech.isCrewOrder("<col=00ffff>Jenkins, take control of the port side salvaging hook!</col>"));
		assertTrue(CrewSpeech.isCrewOrder("Ex-Captain Siad, take a rest!"));
		assertFalse(CrewSpeech.isCrewOrder("Jenkins, take control of the sails!"));
		assertFalse(CrewSpeech.isCrewOrder("You start operating the salvaging hook..."));
		assertFalse(CrewSpeech.isCrewOrder("I could take a rest!"));
		assertFalse(CrewSpeech.isCrewOrder(null));
	}

	@Test
	public void recognizesThePlayersOwnDeposit()
	{
		assertTrue(CrewSpeech.reportsPlayerDeposit("You deposit some cargo into the cargo hold."));
		assertFalse(CrewSpeech.reportsPlayerDeposit("Your crew pack the cargo they were holding into the cargo hold."));
		assertFalse(CrewSpeech.reportsPlayerDeposit(null));
	}

	@Test
	public void anyOrderToACrewmateIsCrewText()
	{
		// Owner, 2026-10-17: another player's order to steer still showed over their head.
		assertTrue(CrewSpeech.isAnyCrewOrder("Ex-Captain Siad, take control of the helm!"));
		assertTrue(CrewSpeech.isAnyCrewOrder("Jenkins, take control of the sails!"));
		assertTrue(CrewSpeech.isAnyCrewOrder("Ex-Captain Siad, take control of the starboard side rune salvaging hook!"));
		assertTrue(CrewSpeech.isAnyCrewOrder("<col=ffff00>Jenkins, take a rest!</col>"));
		// Ordinary chat stays, even with a comma or the same words.
		assertFalse(CrewSpeech.isAnyCrewOrder("Can someone take control of the helm?"));
		assertFalse(CrewSpeech.isAnyCrewOrder("Well, I could take a rest! Then, take control of the sails!"));
		assertFalse(CrewSpeech.isAnyCrewOrder("hi, how are you"));
		assertFalse(CrewSpeech.isAnyCrewOrder(null));
	}
}
