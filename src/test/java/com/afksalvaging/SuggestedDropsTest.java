/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

import net.runelite.api.gameval.ItemID;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SuggestedDropsTest
{
	@Test
	public void everyItemIsListedOnce()
	{
		// The owner's screenshots repeated some items (oak logs, raw lobster, emerald ring...): each goes in once.
		assertEquals(SuggestedDrops.LISTED.length, SuggestedDrops.ITEMS.size());
		assertEquals(74, SuggestedDrops.ITEMS.size());
	}

	@Test
	public void nothingTheSorterTreatsSpeciallyIsSuggested()
	{
		for (int id : SuggestedDrops.ITEMS)
		{
			assertFalse("a default deposit " + id, HoldWhitelist.isDefaultDeposit(id));
			assertFalse("unsorted salvage or coins " + id, SalvageSorter.excluded(id));
		}
		assertTrue(SuggestedDrops.ITEMS.contains(ItemID.OAK_LOGS));
	}

	@Test
	public void addingTheSuggestionsLeavesThePlayersOwnMarksAlone()
	{
		MarkStoreTest.FakeSettings settings = new MarkStoreTest.FakeSettings();
		settings.set("rsprofile.luke", MarkStore.keyFor(SortRule.KEEP), String.valueOf(ItemID.CASKET));
		MarkStore store = new MarkStore(settings);
		assertEquals(SuggestedDrops.ITEMS.size() - 1, store.markAllUnmarked(SuggestedDrops.ITEMS, SortRule.DROP));
		SalvageSorter.Lists lists = store.load();
		assertEquals("the casket stays the player's Keep", SortRule.KEEP, lists.markOf(ItemID.CASKET));
		assertEquals(SortRule.DROP, lists.markOf(ItemID.OAK_LOGS));
		assertEquals("pressing it again adds nothing", 0, store.markAllUnmarked(SuggestedDrops.ITEMS, SortRule.DROP));
		settings.own = null;
		assertEquals("logged out", -1, store.markAllUnmarked(SuggestedDrops.ITEMS, SortRule.DROP));
	}
}
