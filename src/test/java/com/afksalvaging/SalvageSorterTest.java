/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

import java.util.Arrays;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class SalvageSorterTest
{
	/** A tradeable, unnoted item; {@code holdTakes} says whether the cargo hold accepts it. */
	private static SalvageSorter.ItemFacts item(int id, boolean holdTakes, int alch, long ge)
	{
		return new SalvageSorter.ItemFacts(id, false, true, holdTakes, alch, ge);
	}

	@Test
	public void unmarkedItemsSortThemselves()
	{
		SalvageSorter.Lists none = new SalvageSorter.Lists();
		assertEquals("the hold takes it", SortRule.HOLD, SalvageSorter.rule(item(1, true, 5, 100), none, 1000, 0));
		assertEquals("alchs for the threshold", SortRule.ALCH, SalvageSorter.rule(item(2, false, 1000, 100), none, 1000, 0));
		assertEquals("alchs for less", SortRule.DROP, SalvageSorter.rule(item(3, false, 999, 100), none, 1000, 0));
		assertEquals("GE rule off: still alch", SortRule.ALCH, SalvageSorter.rule(item(4, false, 1000, 5000), none, 1000, 0));
		assertEquals("GE beats alch by 50%: keep", SortRule.KEEP, SalvageSorter.rule(item(4, false, 1000, 1500), none, 1000, 50));
		assertEquals("GE beats alch by less than 50%: alch", SortRule.ALCH, SalvageSorter.rule(item(4, false, 1000, 1499), none, 1000, 50));
	}

	@Test
	public void theHoldNeverGetsANotedItem()
	{
		SalvageSorter.Lists none = new SalvageSorter.Lists();
		SalvageSorter.ItemFacts notedKits = new SalvageSorter.ItemFacts(5, true, true, true, 10, 50);
		assertEquals("noted, so not hold; cheap, so drop", SortRule.DROP, SalvageSorter.rule(notedKits, none, 1000, 0));
		SalvageSorter.ItemFacts notedValuable = new SalvageSorter.ItemFacts(6, true, true, true, 5000, 100);
		assertEquals(SortRule.ALCH, SalvageSorter.rule(notedValuable, none, 1000, 0));
	}

	@Test
	public void aStackableTheHoldRefusesIsNotHold()
	{
		SalvageSorter.Lists none = new SalvageSorter.Lists();
		// Runes or nails: stackable, but not on the hold's list.
		assertEquals(SortRule.DROP, SalvageSorter.rule(item(8, false, 5, 10), none, 1000, 0));
	}

	@Test
	public void untradeablesGetNoBoxUnlessMarked()
	{
		SalvageSorter.Lists lists = new SalvageSorter.Lists();
		SalvageSorter.ItemFacts cheap = new SalvageSorter.ItemFacts(9, false, false, false, 0, 0);
		SalvageSorter.ItemFacts heldAndAlchable = new SalvageSorter.ItemFacts(10, false, false, true, 5000, 0);
		assertNull("never dropped by default", SalvageSorter.rule(cheap, lists, 1000, 0));
		assertNull("nor held or alched by default", SalvageSorter.rule(heldAndAlchable, lists, 1000, 0));
		lists.mark(9, SortRule.DROP);
		assertEquals("the player's own mark still applies", SortRule.DROP, SalvageSorter.rule(cheap, lists, 1000, 0));
	}

	@Test
	public void aMarkWinsAndAnItemIsInOneListOnly()
	{
		SalvageSorter.Lists lists = new SalvageSorter.Lists();
		lists.mark(7, SortRule.KEEP);
		assertEquals(SortRule.KEEP, SalvageSorter.rule(item(7, true, 0, 0), lists, 1000, 0));
		lists.mark(7, SortRule.DROP);
		assertEquals(SortRule.DROP, lists.markOf(7));
		assertTrue(lists.ids(SortRule.KEEP).isEmpty());
		lists.mark(7, null);
		assertNull(lists.markOf(7));
		assertEquals(SortRule.HOLD, SalvageSorter.rule(item(7, true, 0, 0), lists, 1000, 0));
	}

	@Test
	public void listsSurviveStorageAndBadEntries()
	{
		SalvageSorter.Lists lists = new SalvageSorter.Lists();
		lists.mark(10, SortRule.ALCH);
		lists.mark(11, SortRule.ALCH);
		assertEquals("10,11", lists.encode(SortRule.ALCH));
		assertEquals("", lists.encode(SortRule.DROP));
		SalvageSorter.Lists restored = new SalvageSorter.Lists();
		restored.decode(SortRule.ALCH, "10, 11,junk,");
		assertEquals(Arrays.asList(10, 11), restored.ids(SortRule.ALCH));
		// Decoding a list pulls an id out of any other list, so a hand-edited setting cannot double-mark.
		restored.decode(SortRule.DROP, "11");
		assertEquals(Arrays.asList(10), restored.ids(SortRule.ALCH));
		assertEquals(SortRule.DROP, restored.markOf(11));
		restored.decode(SortRule.ALCH, null);
		assertTrue(restored.ids(SortRule.ALCH).isEmpty());
	}

	@Test
	public void coinsAndUnsortedSalvageAreNeverBoxed()
	{
		assertTrue(SalvageSorter.excluded(ItemID.COINS));
		assertTrue(SalvageSorter.excluded(ShipwreckType.BARRACUDA.getSalvageItemId()));
		assertTrue(!SalvageSorter.excluded(ItemID.LOBSTER));
	}
}
