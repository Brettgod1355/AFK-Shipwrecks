/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

import org.junit.Test;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class SailingOverlapTest
{
	@Test
	public void theLineNamesOnlyWhatOverlaps()
	{
		// Owner, 2026-10-25: a player coming from the Sailing plugin sees two counts and two sets of outlines.
		String both = SailingOverlap.message(true, true);
		assertTrue(both.contains("Item Count") && both.contains("Highlight Active Locations"));
		String count = SailingOverlap.message(true, false);
		assertTrue(count.contains("Item Count") && !count.contains("Highlight"));
		String wrecks = SailingOverlap.message(false, true);
		assertTrue(wrecks.contains("Highlight Inactive Locations") && !wrecks.contains("Item Count"));
		assertNull("nothing overlaps: nothing said", SailingOverlap.message(false, false));
	}
}
