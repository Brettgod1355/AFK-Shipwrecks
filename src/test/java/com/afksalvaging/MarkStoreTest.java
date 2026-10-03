/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Test;

public class MarkStoreTest
{
	@Test
	public void aCharactersStoredKeySplitsIntoProfileAndSetting()
	{
		assertArrayEquals(new String[]{"rsprofile.eLGRJxqN", "sortDropIds"},
			MarkStore.profileAndKey("cargofull.rsprofile.eLGRJxqN.sortDropIds"));
		assertNull("a plugin-wide key", MarkStore.profileAndKey("cargofull.sortDropIds"));
		assertNull("another plugin's key", MarkStore.profileAndKey("sailing.rsprofile.eLGRJxqN.sortDropIds"));
		assertNull("no setting after the profile", MarkStore.profileAndKey("cargofull.rsprofile.eLGRJxqN."));
		assertNull("no setting at all", MarkStore.profileAndKey("cargofull.rsprofile.eLGRJxqN"));
	}

	@Test
	public void everyListHasItsOwnSettingAndBack()
	{
		for (SortRule rule : SortRule.values())
		{
			assertEquals(rule, MarkStore.ruleFor(MarkStore.keyFor(rule)));
		}
		assertNull(MarkStore.ruleFor("spots.favourites"));
	}
}
