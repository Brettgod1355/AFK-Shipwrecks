/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class HoldItemsTest
{
	@Test
	public void theRememberedTypesRoundTrip()
	{
		Set<Integer> ids = new HashSet<>(Arrays.asList(ItemID.RUNE_CANNONBALL, ItemID.MITHRIL_CANNONBALL, ItemID.MCANNONBALL));
		String text = HoldItems.encode(ids);
		Set<Integer> back = new HashSet<>();
		HoldItems.decode(text, back);
		assertEquals(ids, back);
	}

	@Test
	public void aDamagedValueKeepsWhatItCanRead()
	{
		Set<Integer> back = new HashSet<>();
		HoldItems.decode("12, x, ,-4,34", back);
		assertEquals(new HashSet<>(Arrays.asList(12, 34)), back);
		HoldItems.decode(null, back);
		assertEquals(2, back.size());
		assertEquals("", HoldItems.encode(new HashSet<>()));
	}

	@Test
	public void theTypesADepositTookAreTheOnesThatWentDown()
	{
		Map<Integer, Integer> before = new HashMap<>();
		before.put(ItemID.DRAGON_CANNONBALL, 40);
		before.put(ItemID.RUNE_CANNONBALL, 10);
		before.put(ItemID.LOBSTER, 3);
		Map<Integer, Integer> after = new HashMap<>();
		// Every dragon cannonball deposited, five of the rune ones, the lobsters kept.
		after.put(ItemID.RUNE_CANNONBALL, 5);
		after.put(ItemID.LOBSTER, 3);
		assertEquals(new HashSet<>(Arrays.asList(ItemID.DRAGON_CANNONBALL, ItemID.RUNE_CANNONBALL)), HoldItems.gone(before, after));
		assertTrue(HoldItems.gone(after, after).isEmpty());
	}
}
