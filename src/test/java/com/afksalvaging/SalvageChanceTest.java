/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SalvageChanceTest
{
	@Test
	public void matchesTheWikiAtTheEndsOfTheScale()
	{
		// Pirate wreck, bronze hook: 40/256 at level 1, 80/256 at level 99 (plus the game's +1).
		assertEquals(41 / 256.0, SalvageChance.player(ShipwreckType.PIRATE, SalvagingHookTier.BRONZE, 1), 1e-12);
		assertEquals(81 / 256.0, SalvageChance.player(ShipwreckType.PIRATE, SalvagingHookTier.BRONZE, 99), 1e-12);
		// Merchant wreck, dragon hook: 32 and 67.
		assertEquals(33 / 256.0, SalvageChance.player(ShipwreckType.MERCHANT, SalvagingHookTier.DRAGON, 1), 1e-12);
		assertEquals(68 / 256.0, SalvageChance.player(ShipwreckType.MERCHANT, SalvagingHookTier.DRAGON, 99), 1e-12);
		// Small wreck, bronze hook at 99: 100.
		assertEquals(101 / 256.0, SalvageChance.player(ShipwreckType.SMALL, SalvagingHookTier.BRONZE, 99), 1e-12);
	}

	@Test
	public void interpolatesBetweenLevels()
	{
		// Pirate/dragon: 54 at 1, 108 at 99. At 64: (54*35 + 108*63)/98 = 88.7 -> 88.
		assertEquals(89 / 256.0, SalvageChance.player(ShipwreckType.PIRATE, SalvagingHookTier.DRAGON, 64), 1e-12);
		// Level 50 is exactly halfway: (54*49 + 108*49)/98 = 81.
		assertEquals(82 / 256.0, SalvageChance.player(ShipwreckType.PIRATE, SalvagingHookTier.DRAGON, 50), 1e-12);
	}

	@Test
	public void clampsLevelsAndDefaultsTheHook()
	{
		assertEquals(SalvageChance.player(ShipwreckType.LARGE, SalvagingHookTier.RUNE, 99),
			SalvageChance.player(ShipwreckType.LARGE, SalvagingHookTier.RUNE, 120), 1e-12);
		assertEquals(SalvageChance.player(ShipwreckType.LARGE, SalvagingHookTier.RUNE, 1),
			SalvageChance.player(ShipwreckType.LARGE, SalvagingHookTier.RUNE, -3), 1e-12);
		assertEquals(SalvageChance.player(ShipwreckType.LARGE, SalvagingHookTier.BRONZE, 60),
			SalvageChance.player(ShipwreckType.LARGE, null, 60), 1e-12);
		assertTrue(Double.isNaN(SalvageChance.player(null, SalvagingHookTier.BRONZE, 60)));
	}

	@Test
	public void crewGetTenPercentPerPointOfDeckhandiness()
	{
		double player = SalvageChance.player(ShipwreckType.MERCENARY, SalvagingHookTier.RUNE, 90);
		assertEquals(player * 0.4, SalvageChance.crew(ShipwreckType.MERCENARY, SalvagingHookTier.RUNE, 90, 4), 1e-12);
		assertEquals(player * 0.1, SalvageChance.crew(ShipwreckType.MERCENARY, SalvagingHookTier.RUNE, 90, 1), 1e-12);
		assertEquals(player * 0.2, SalvageChance.crew(ShipwreckType.MERCENARY, SalvagingHookTier.RUNE, 90, 0), 1e-12);
		assertEquals(player * 0.4, SalvageChance.crew(ShipwreckType.MERCENARY, SalvagingHookTier.RUNE, 90, 7), 1e-12);
	}

	@Test
	public void betterHooksAndBiggerWrecksBehaveAsExpected()
	{
		for (ShipwreckType wreck : ShipwreckType.values())
		{
			double last = 0;
			for (SalvagingHookTier hook : SalvagingHookTier.values())
			{
				double chance = SalvageChance.player(wreck, hook, 99);
				assertTrue(wreck + " " + hook, chance > last);
				assertTrue(chance < 0.6);
				last = chance;
			}
		}
		assertTrue(SalvageChance.player(ShipwreckType.SMALL, SalvagingHookTier.DRAGON, 99)
			> SalvageChance.player(ShipwreckType.MERCHANT, SalvagingHookTier.DRAGON, 99));
	}
}
