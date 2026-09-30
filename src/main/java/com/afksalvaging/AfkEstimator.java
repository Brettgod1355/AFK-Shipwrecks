/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

/**
 * Turns everything the plugin knows into the AFK timer's state and countdown.
 * <p>
 * The forecast has two parts. While the wreck that is up now lasts, everyone salvages at full rate:
 * crew fill the hold directly; the player's salvage goes to their inventory first, counts as bound
 * for the hold if the setting says so, and stops when their inventory is full or the wreck sinks
 * (an AFK player does not walk to the next wreck). After that only the crew carry on, at a rate
 * scaled by how much of the time a wreck has been up.
 */
public final class AfkEstimator
{
	/** Milliseconds in a game tick. */
	public static final double TICK_MILLIS = 600;

	/** Everything the estimate depends on, filled in by the plugin each tick. */
	public static final class Inputs
	{
		public boolean sailing;
		public int hookCount;
		public boolean holdKnown;
		/** The hold is known to be full. */
		public boolean holdFull;
		/** Only the running tally says the hold is full. */
		public boolean holdFullUnconfirmed;
		/** Free slots in the hold. */
		public int holdRemaining;
		/** Salvage per tick the published tables expect from crew whose hook has a wreck in reach. */
		public double crewExpectedPerTick;
		/** Salvage per tick the published tables expect from the player while salvaging. */
		public double playerExpectedPerTick;
		/** Whether the player is occupying a hook, rolling or not. */
		public boolean playerAtHook;
		/** Whether the player is actually rolling. */
		public boolean playerRolling;
		/** Unsorted salvage in the player's inventory. */
		public int inventorySalvage;
		public int freeInventorySlots;
		public boolean countInventorySalvage = true;
		/** Whether a wreck the player has the level for is in reach of a manned hook. */
		public boolean wreckInReach;
		/** Whether a wreck is in reach that the player's level is too low for, and none they can use. */
		public boolean onlyHigherWrecksInReach;
		public boolean hazardous;
		public boolean stalled;
		/** How long the wrecks in reach will still be up, at most. */
		public long wreckWindowMillis;
		/** Share of recent time a usable wreck was in reach, 0 to 1. */
		public double availability = 1;
		/** Observed over published rate. */
		public double correction = 1;
		public boolean confident;
		/** Likely wait for the next wreck when none is up, or -1 when there is nothing to go on. */
		public long expectedWaitMillis = -1;
	}

	private AfkEstimator()
	{
	}

	public static AfkEstimate estimate(Inputs in)
	{
		if (!in.sailing)
		{
			return AfkEstimate.of(AfkEstimate.State.NOT_SAILING);
		}
		if (in.hookCount <= 0)
		{
			return AfkEstimate.of(AfkEstimate.State.NO_HOOK);
		}
		if (!in.holdKnown)
		{
			return AfkEstimate.of(AfkEstimate.State.HOLD_UNKNOWN);
		}
		if (in.holdFull)
		{
			return AfkEstimate.of(AfkEstimate.State.HOLD_FULL);
		}
		if (in.holdFullUnconfirmed || in.holdRemaining <= 0)
		{
			return AfkEstimate.of(AfkEstimate.State.HOLD_FULL_UNCONFIRMED);
		}
		boolean crewOnHooks = in.crewExpectedPerTick > 0;
		if (!crewOnHooks && !in.playerAtHook)
		{
			return AfkEstimate.of(AfkEstimate.State.NOBODY_SALVAGING);
		}
		double k = in.correction > 0 ? in.correction : 1;
		double crew = k * in.crewExpectedPerTick * 1000 / TICK_MILLIS;
		double player = in.playerRolling ? k * in.playerExpectedPerTick * 1000 / TICK_MILLIS : 0;
		double availability = Math.max(0, Math.min(1, in.availability));
		double holdPerHour = (crew + player) * 3600;
		boolean approximate = !in.confident;

		int remaining = in.holdRemaining;
		if (in.countInventorySalvage)
		{
			remaining = Math.max(0, remaining - Math.max(0, in.inventorySalvage));
		}
		// Time the crew alone would need with a wreck up the whole time: the figure for the paused states.
		long crewWork = crew > 0 ? millis(remaining / crew) : -1;

		if (in.hazardous)
		{
			return AfkEstimate.of(AfkEstimate.State.HAZARDOUS, -1, crewWork, approximate);
		}
		if (!in.wreckInReach)
		{
			if (in.onlyHigherWrecksInReach)
			{
				return AfkEstimate.of(AfkEstimate.State.LEVEL_TOO_LOW, -1, crewWork, approximate);
			}
			return AfkEstimate.of(AfkEstimate.State.WAITING_FOR_WRECK, in.expectedWaitMillis, crewWork, approximate);
		}
		if (in.stalled)
		{
			return AfkEstimate.of(AfkEstimate.State.STALLED, -1, crewWork, approximate);
		}
		if (remaining <= 0)
		{
			// Everything needed to fill the hold is already in the inventory waiting to be deposited.
			return new AfkEstimate(AfkEstimate.State.COUNTING_DOWN, 0, 0, approximate, holdPerHour);
		}

		double window = Math.max(0, in.wreckWindowMillis) / 1000.0;
		double playerUntil = 0;
		if (player > 0)
		{
			playerUntil = Math.min(Math.max(0, in.freeInventorySlots) / player, window);
		}
		double filledInWindow = crew * window + player * playerUntil;
		// Salvaging time needed if a wreck were up throughout: player until they stop, then crew.
		double together = crew + player;
		long work;
		if (remaining <= together * playerUntil + 1e-9)
		{
			work = millis(remaining / together);
		}
		else
		{
			work = crew > 0 ? millis(playerUntil + (remaining - together * playerUntil) / crew) : -1;
		}
		if (remaining <= filledInWindow + 1e-9)
		{
			return new AfkEstimate(AfkEstimate.State.COUNTING_DOWN, work, work, approximate, holdPerHour);
		}

		double crewAfter = crew * availability;
		if (crewAfter <= 0)
		{
			if (player > 0)
			{
				boolean inventoryFirst = Math.max(0, in.freeInventorySlots) / player <= window;
				return new AfkEstimate(inventoryFirst ? AfkEstimate.State.INVENTORY_FILLS_FIRST : AfkEstimate.State.WRECK_SINKS_FIRST,
					millis(playerUntil), work, approximate, holdPerHour);
			}
			return AfkEstimate.of(AfkEstimate.State.WAITING_FOR_WRECK, in.expectedWaitMillis, work, approximate);
		}
		double eta = window + (remaining - filledInWindow) / crewAfter;
		return new AfkEstimate(AfkEstimate.State.COUNTING_DOWN, millis(eta), work, approximate, holdPerHour);
	}

	private static long millis(double seconds)
	{
		if (Double.isNaN(seconds) || Double.isInfinite(seconds))
		{
			return -1;
		}
		return Math.round(Math.max(0, seconds) * 1000);
	}
}
