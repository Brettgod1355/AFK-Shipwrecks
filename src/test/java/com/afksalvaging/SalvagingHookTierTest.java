/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

import net.runelite.api.gameval.ObjectID;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class SalvagingHookTierTest
{
	@Test
	public void everyBoatSizeMapsToTheSameTier()
	{
		assertEquals(SalvagingHookTier.DRAGON, SalvagingHookTier.fromObjectId(ObjectID.SALVAGING_HOOK_RAFT_DRAGON));
		assertEquals(SalvagingHookTier.DRAGON, SalvagingHookTier.fromObjectId(ObjectID.SALVAGING_HOOK_DRAGON));
		assertEquals(SalvagingHookTier.DRAGON, SalvagingHookTier.fromObjectId(ObjectID.SALVAGING_HOOK_LARGE_DRAGON));
		assertEquals(SalvagingHookTier.DRAGON, SalvagingHookTier.fromObjectId(ObjectID.SALVAGING_HOOK_LARGE_DRAGON_B));
		assertEquals(SalvagingHookTier.BRONZE, SalvagingHookTier.fromObjectId(ObjectID.SALVAGING_HOOK_LARGE_BRONZE_B));
	}

	@Test
	public void otherObjectsAreNotHooks()
	{
		assertNull(SalvagingHookTier.fromObjectId(ObjectID.SAILING_SALVAGING_STATION_3X8));
		assertFalse(SalvagingHookTier.isHook(ObjectID.SAILING_SMALL_SHIPWRECK));
		assertFalse(SalvagingHookTier.isHook(-1));
		assertTrue(SalvagingHookTier.isHook(ObjectID.SALVAGING_HOOK_MITHRIL));
	}

	@Test
	public void deckhandinessRequirementsMatchTheWiki()
	{
		assertEquals(1, SalvagingHookTier.STEEL.getDeckhandiness());
		assertEquals(2, SalvagingHookTier.ADAMANT.getDeckhandiness());
		assertEquals(3, SalvagingHookTier.RUNE.getDeckhandiness());
		assertEquals(4, SalvagingHookTier.DRAGON.getDeckhandiness());
		assertEquals(86, SalvagingHookTier.DRAGON.getSailingLevel());
	}
}
