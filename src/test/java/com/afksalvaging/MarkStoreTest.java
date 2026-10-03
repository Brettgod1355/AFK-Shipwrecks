/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class MarkStoreTest
{
	@Test
	public void everyListAndTheExcludedSetHaveTheirOwnSetting()
	{
		List<String> keys = MarkStore.allKeys();
		assertEquals(SortRule.values().length + 1, keys.size());
		assertEquals(new HashSet<>(keys).size(), keys.size());
		for (SortRule rule : SortRule.values())
		{
			assertTrue(keys.contains(MarkStore.keyFor(rule)));
		}
		assertTrue(keys.contains(MarkStore.EXCLUDED_KEY));
		// The names the 1.0 and early 2.0 settings used, so the migration finds them.
		Set<String> expected = new HashSet<>();
		expected.add("sortKeepIds");
		expected.add("sortHoldIds");
		expected.add("sortAlchIds");
		expected.add("sortDropIds");
		expected.add("sortNoneIds");
		assertEquals(expected, new HashSet<>(keys));
	}
}
