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
	/** A tradeable, unnoted item the cargo hold does not take. */
	private static SalvageSorter.ItemFacts item(int id, int alch, long ge)
	{
		return new SalvageSorter.ItemFacts(id, false, true, false, false, alch, ge);
	}

	/** A tradeable ship cannonball, noted or not. */
	private static SalvageSorter.ItemFacts cannonball(int id, boolean noted)
	{
		return new SalvageSorter.ItemFacts(id, noted, true, true, true, 5, 100);
	}

	/** A tradeable item the hold takes that is not a cannonball, such as a repair kit. */
	private static SalvageSorter.ItemFacts holdItem(int id, int alch)
	{
		return new SalvageSorter.ItemFacts(id, false, true, true, false, alch, 0);
	}

	@Test
	public void unmarkedItemsSortThemselves()
	{
		SalvageSorter.Lists none = new SalvageSorter.Lists();
		assertEquals("alchs for the threshold", SortRule.ALCH, SalvageSorter.rule(item(2, 1000, 100), none, 1000, 0));
		assertNull("alchs for less: no box, nothing is a drop by default", SalvageSorter.rule(item(3, 999, 100), none, 1000, 0));
		assertEquals("GE rule off: still alch", SortRule.ALCH, SalvageSorter.rule(item(4, 1000, 5000), none, 1000, 0));
		assertEquals("GE beats alch by 50%: keep", SortRule.KEEP, SalvageSorter.rule(item(4, 1000, 1500), none, 1000, 50));
		assertEquals("GE beats alch by less than 50%: alch", SortRule.ALCH, SalvageSorter.rule(item(4, 1000, 1499), none, 1000, 50));
	}

	@Test
	public void onlyCannonballsAreADepositByDefault()
	{
		SalvageSorter.Lists none = new SalvageSorter.Lists();
		assertEquals(SortRule.HOLD, SalvageSorter.rule(cannonball(1, false), none, 1000, 0));
		assertNull("a noted cannonball is refused by the hold: no box", SalvageSorter.rule(cannonball(1, true), none, 1000, 0));
		assertNull("a cheap repair kit is not called a drop", SalvageSorter.rule(holdItem(5, 10), none, 1000, 0));
		assertNull("nor an alch, nor a deposit", SalvageSorter.rule(holdItem(6, 5000), none, 1000, 0));
	}

	@Test
	public void theHoldTakesWhatThePlayerMarks()
	{
		SalvageSorter.Lists lists = new SalvageSorter.Lists();
		lists.mark(5, SortRule.HOLD);
		assertEquals(SortRule.HOLD, SalvageSorter.rule(holdItem(5, 10), lists, 1000, 0));
	}

	@Test
	public void aStackableTheHoldRefusesIsNotADeposit()
	{
		SalvageSorter.Lists none = new SalvageSorter.Lists();
		// Runes or nails: stackable, but not on the hold's list.
		assertNull(SalvageSorter.rule(item(8, 5, 10), none, 1000, 0));
	}

	@Test
	public void untradeablesGetNoBoxUnlessMarked()
	{
		SalvageSorter.Lists lists = new SalvageSorter.Lists();
		SalvageSorter.ItemFacts cheap = new SalvageSorter.ItemFacts(9, false, false, false, false, 0, 0);
		SalvageSorter.ItemFacts heldAndAlchable = new SalvageSorter.ItemFacts(10, false, false, true, false, 5000, 0);
		assertNull("never dropped by default", SalvageSorter.rule(cheap, lists, 1000, 0));
		assertNull("nor deposited or alched by default", SalvageSorter.rule(heldAndAlchable, lists, 1000, 0));
		lists.mark(9, SortRule.DROP);
		assertEquals("the player's own mark still applies", SortRule.DROP, SalvageSorter.rule(cheap, lists, 1000, 0));
	}

	@Test
	public void aMarkWinsAndAnItemIsInOneListOnly()
	{
		SalvageSorter.Lists lists = new SalvageSorter.Lists();
		lists.mark(7, SortRule.KEEP);
		assertEquals(SortRule.KEEP, SalvageSorter.rule(cannonball(7, false), lists, 1000, 0));
		lists.mark(7, SortRule.DROP);
		assertEquals(SortRule.DROP, lists.markOf(7));
		assertTrue(lists.ids(SortRule.KEEP).isEmpty());
		lists.mark(7, null);
		assertNull(lists.markOf(7));
		assertEquals(SortRule.HOLD, SalvageSorter.rule(cannonball(7, false), lists, 1000, 0));
	}

	@Test
	public void listsSurviveStorageAndBadEntries()
	{
		SalvageSorter.Lists lists = new SalvageSorter.Lists();
		lists.mark(10, SortRule.ALCH);
		lists.mark(11, SortRule.ALCH);
		assertEquals("10,11", SalvageSorter.Lists.join(lists.ids(SortRule.ALCH)));
		assertEquals("", SalvageSorter.Lists.join(lists.ids(SortRule.DROP)));
		assertEquals(Arrays.asList(10, 11), SalvageSorter.Lists.parse("10, 11,junk,,11"));
		assertTrue(SalvageSorter.Lists.parse(null).isEmpty());
		assertTrue(SalvageSorter.Lists.parse("").isEmpty());
	}

	@Test
	public void listsReadTogetherKeepTheFirstMarkOfAnItem()
	{
		// One character's lists are read first, then another's: the first mark seen wins a disagreement.
		SalvageSorter.Lists lists = new SalvageSorter.Lists();
		lists.add(SortRule.ALCH, Arrays.asList(10, 11));
		lists.add(SortRule.DROP, Arrays.asList(11, 12));
		assertEquals(Arrays.asList(10, 11), lists.ids(SortRule.ALCH));
		assertEquals(Arrays.asList(12), lists.ids(SortRule.DROP));
		SalvageSorter.Lists copy = new SalvageSorter.Lists();
		copy.mark(99, SortRule.KEEP);
		copy.replaceWith(lists);
		assertNull(copy.markOf(99));
		assertEquals(SortRule.DROP, copy.markOf(12));
	}

	@Test
	public void coinsAndUnsortedSalvageAreNeverBoxed()
	{
		assertTrue(SalvageSorter.excluded(ItemID.COINS));
		assertTrue(SalvageSorter.excluded(ShipwreckType.BARRACUDA.getSalvageItemId()));
		assertTrue(!SalvageSorter.excluded(ItemID.LOBSTER));
	}
}
