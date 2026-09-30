/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

/**
 * The game's published chance of hooking salvage on one roll, by wreck, hook and Sailing level.
 * <p>
 * The wiki gives, for every wreck and hook, the success value out of 256 at level 1 and at level 99;
 * the game interpolates linearly between them. Crewmates roll at 10% of this per point of
 * deckhandiness. Boosts above 99 do nothing. These figures are the starting point for the rate
 * estimate; what is actually observed corrects them.
 */
public final class SalvageChance
{
	/** Successes out of 256 at level 1, by wreck (enum order) then hook tier (enum order). */
	private static final int[][] LOW = {
		{50, 52, 55, 57, 60, 62, 67}, // Small
		{48, 50, 52, 55, 57, 60, 64}, // Fisherman's
		{45, 47, 49, 51, 54, 56, 60}, // Barracuda
		{42, 44, 46, 48, 50, 52, 56}, // Large
		{40, 42, 44, 46, 48, 50, 54}, // Pirate
		{32, 33, 35, 36, 38, 40, 43}, // Mercenary
		{28, 29, 30, 32, 33, 35, 37}, // Fremennik
		{24, 25, 26, 27, 28, 30, 32}, // Merchant
	};

	/** Successes out of 256 at level 99. */
	private static final int[][] HIGH = {
		{100, 105, 110, 115, 120, 125, 135},
		{96, 100, 105, 110, 115, 120, 129},
		{90, 94, 99, 103, 108, 112, 121},
		{84, 88, 92, 96, 100, 105, 113},
		{80, 84, 88, 92, 96, 100, 108},
		{64, 67, 70, 73, 76, 80, 86},
		{58, 60, 63, 66, 69, 72, 78},
		{50, 52, 55, 57, 60, 62, 67},
	};

	private SalvageChance()
	{
	}

	/**
	 * Chance, between 0 and 1, that one of the player's rolls at this wreck with this hook succeeds.
	 * Levels outside 1 to 99 are clamped; a null hook is treated as bronze.
	 */
	public static double player(ShipwreckType wreck, SalvagingHookTier hook, int sailingLevel)
	{
		if (wreck == null)
		{
			return Double.NaN;
		}
		int tier = hook == null ? 0 : hook.ordinal();
		int level = Math.max(1, Math.min(99, sailingLevel));
		int low = LOW[wreck.ordinal()][tier];
		int high = HIGH[wreck.ordinal()][tier];
		int value = (low * (99 - level) + high * (level - 1)) / 98;
		return (1 + value) / 256.0;
	}

	/** Chance that one roll by a crewmate of this deckhandiness succeeds at this wreck with this hook. */
	public static double crew(ShipwreckType wreck, SalvagingHookTier hook, int sailingLevel, int deckhandiness)
	{
		int d = deckhandiness <= 0 ? SalvageRateModel.DEFAULT_DECKHANDINESS : Math.min(4, deckhandiness);
		return player(wreck, hook, sailingLevel) * d / 10.0;
	}
}
