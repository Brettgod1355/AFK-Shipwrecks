/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

import java.util.regex.Pattern;
import net.runelite.client.util.Text;

/**
 * Recognises the crewmate speech and game messages that reveal cargo moving into the hold.
 * <p>
 * The game only transmits the hold's contents while the cargo hold interface is open, so
 * these lines are the only live signal while sailing. Matching is case-insensitive and
 * ignores colour tags.
 */
public final class CrewSpeech
{
	/** Said by a crewmate, and by the game, when nothing more fits in the hold. */
	private static final String HOLD_FULL = "cargo hold is full";
	/** Fragments of the crewmate line announcing a successful salvage that goes into the hold. */
	private static final String[] CREW_SALVAGE = {"hook some salvage", "put it in the cargo hold"};
	/** Game message when the player deposits carried cargo without opening the hold. */
	private static final String PLAYER_DEPOSIT = "you deposit some cargo into the cargo hold";
	/** Game message when the salvaging station has nothing left to sort. */
	private static final String SORTING_DONE = "no more salvage to sort";
	/** Game message when the boat is somewhere it may not salvage. */
	private static final String HAZARDOUS = "not safe to salvage while in hazardous waters";
	/** The ghostly cabin boy only ever says variations of "Wooo wooo." */
	private static final Pattern GHOST_SPEECH = Pattern.compile("(?i)^(?:w+o+[ ,]*)+[.!]*$");

	private CrewSpeech()
	{
	}

	/** Whether this line says the cargo hold is full. */
	public static boolean reportsFullHold(String text)
	{
		return plain(text).contains(HOLD_FULL);
	}

	/** Whether a crewmate says they salvaged something and put it in the hold. */
	public static boolean reportsCrewSalvage(String text)
	{
		String line = plain(text);
		if (line.contains(HOLD_FULL))
		{
			return false;
		}
		for (String fragment : CREW_SALVAGE)
		{
			if (line.contains(fragment))
			{
				return true;
			}
		}
		return false;
	}

	/** Whether the game says the player just deposited one piece of carried cargo. */
	public static boolean reportsPlayerDeposit(String text)
	{
		return plain(text).contains(PLAYER_DEPOSIT);
	}

	/** Whether the game says the player has sorted everything at the station. */
	public static boolean reportsSortingDone(String text)
	{
		return plain(text).contains(SORTING_DONE);
	}

	/** Whether the game refused to salvage because the boat is in hazardous waters. */
	public static boolean reportsHazardousWaters(String text)
	{
		return plain(text).contains(HAZARDOUS);
	}

	/**
	 * Whether this is the ghost crewmate's wordless speech. He cannot say what he did, so a
	 * Sailing XP drop in the same tick is what tells us he salvaged something.
	 */
	public static boolean isGhostSpeech(String text)
	{
		String line = text == null ? "" : Text.removeTags(text).trim();
		return !line.isEmpty() && GHOST_SPEECH.matcher(line).matches();
	}

	private static String plain(String text)
	{
		return text == null ? "" : Text.removeTags(text).toLowerCase();
	}
}
