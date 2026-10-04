/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

import java.util.Collections;
import java.util.Set;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SalvagingWorldsTest
{
	@Test
	public void parsesTheDefaultList()
	{
		Set<Integer> worlds = SalvagingWorlds.parse(SalvagingWorlds.DEFAULT_WORLDS);
		assertEquals(2, worlds.size());
		assertTrue(worlds.contains(596));
		assertTrue(worlds.contains(597));
	}

	@Test
	public void toleratesMessyInput()
	{
		Set<Integer> worlds = SalvagingWorlds.parse(" 486;597 , w301, -4, 0,,");
		assertEquals(2, worlds.size());
		assertTrue(worlds.contains(486));
		assertTrue(worlds.contains(597));
		assertTrue(SalvagingWorlds.parse(null).isEmpty());
		assertTrue(SalvagingWorlds.parse("   ").isEmpty());
	}

	@Test
	public void recognisesTheActivityLabelOrTheList()
	{
		assertTrue(SalvagingWorlds.isSalvagingActivity("Salvaging"));
		assertTrue(SalvagingWorlds.isSalvagingActivity("Sailing - salvaging"));
		assertFalse(SalvagingWorlds.isSalvagingActivity("Trade - Free"));
		assertFalse(SalvagingWorlds.isSalvagingActivity(null));

		Set<Integer> extra = SalvagingWorlds.parse("486");
		assertTrue(SalvagingWorlds.isSalvagingWorld(596, "Salvaging", Collections.emptySet()));
		assertTrue(SalvagingWorlds.isSalvagingWorld(486, null, extra));
		assertFalse(SalvagingWorlds.isSalvagingWorld(302, "Trade - Free", extra));
		assertFalse(SalvagingWorlds.isSalvagingWorld(302, null, null));
	}
}
