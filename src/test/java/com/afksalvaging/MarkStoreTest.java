/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
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

	private static final String DUKE = "rsprofile.duke";
	private static final String LUKE = "rsprofile.luke";
	private static final int SWORD = 1277;
	private static final int SHIELD = 1171;
	private static final int BALL = 31906;

	/** RuneLite's settings, in memory: two characters, one logged in. */
	private static final class FakeSettings implements MarkStore.Settings
	{
		final Map<String, String> plugin = new HashMap<>();
		final Map<String, String> perCharacter = new HashMap<>();
		final List<String> characters = new ArrayList<>(Arrays.asList(DUKE, LUKE));
		String own = DUKE;

		@Override
		public String ownProfile()
		{
			return own;
		}

		@Override
		public List<String> profiles()
		{
			return characters;
		}

		@Override
		public String get(String key)
		{
			return plugin.get(key);
		}

		@Override
		public String get(String profile, String key)
		{
			return perCharacter.get(profile + "." + key);
		}

		@Override
		public void set(String profile, String key, String value)
		{
			perCharacter.put(profile + "." + key, value);
		}

		@Override
		public void unset(String key)
		{
			plugin.remove(key);
		}

		@Override
		public void unset(String profile, String key)
		{
			perCharacter.remove(profile + "." + key);
		}

		String list(String profile, SortRule rule)
		{
			return get(profile, MarkStore.keyFor(rule));
		}
	}

	@Test
	public void aMarkIsFiledUnderTheCharacterLoggedInAndSeenByEveryone()
	{
		FakeSettings settings = new FakeSettings();
		MarkStore store = new MarkStore(settings);
		assertTrue(store.mark(SWORD, SortRule.KEEP));
		assertEquals(String.valueOf(SWORD), settings.list(DUKE, SortRule.KEEP));
		assertNull(settings.list(LUKE, SortRule.KEEP));
		// Logged in as the other character, the mark is still there.
		settings.own = LUKE;
		assertEquals(SortRule.KEEP, new MarkStore(settings).load().markOf(SWORD));
	}

	@Test
	public void markingAnItemTakesItOutOfEveryOtherListOfEveryCharacter()
	{
		FakeSettings settings = new FakeSettings();
		settings.set(LUKE, MarkStore.keyFor(SortRule.DROP), SWORD + "," + SHIELD);
		settings.set(DUKE, MarkStore.keyFor(SortRule.ALCH), String.valueOf(SWORD));
		MarkStore store = new MarkStore(settings);
		store.mark(SWORD, SortRule.KEEP);
		assertEquals(String.valueOf(SHIELD), settings.list(LUKE, SortRule.DROP));
		assertNull("an emptied list is unset, not saved empty", settings.list(DUKE, SortRule.ALCH));
		assertEquals(String.valueOf(SWORD), settings.list(DUKE, SortRule.KEEP));
		SalvageSorter.Lists lists = store.load();
		assertEquals(SortRule.KEEP, lists.markOf(SWORD));
		assertEquals(SortRule.DROP, lists.markOf(SHIELD));
	}

	@Test
	public void unmarkingTakesTheItemOutEverywhere()
	{
		FakeSettings settings = new FakeSettings();
		settings.set(LUKE, MarkStore.keyFor(SortRule.KEEP), String.valueOf(SWORD));
		MarkStore store = new MarkStore(settings);
		assertTrue(store.mark(SWORD, null));
		assertNull(settings.list(LUKE, SortRule.KEEP));
		assertNull(store.load().markOf(SWORD));
	}

	@Test
	public void excludingADefaultUnmarksItAndKeepsItOutOfTheDefaults()
	{
		FakeSettings settings = new FakeSettings();
		settings.set(LUKE, MarkStore.keyFor(SortRule.HOLD), String.valueOf(BALL));
		MarkStore store = new MarkStore(settings);
		assertTrue(store.exclude(BALL));
		assertNull(settings.list(LUKE, SortRule.HOLD));
		assertEquals(String.valueOf(BALL), settings.get(DUKE, MarkStore.EXCLUDED_KEY));
		SalvageSorter.Lists lists = store.load();
		assertTrue(lists.isExcluded(BALL));
		assertNull(lists.markOf(BALL));
		// Marking it again brings it back and lifts the exclusion.
		store.mark(BALL, SortRule.HOLD);
		assertNull(settings.get(DUKE, MarkStore.EXCLUDED_KEY));
		assertFalse(store.load().isExcluded(BALL));
	}

	@Test
	public void loggedOutNothingIsFiledAndTheCallSaysSo()
	{
		FakeSettings settings = new FakeSettings();
		settings.own = null;
		MarkStore store = new MarkStore(settings);
		assertFalse(store.mark(SWORD, SortRule.KEEP));
		assertFalse(store.exclude(BALL));
		assertFalse(store.migrate());
		assertTrue(settings.perCharacter.isEmpty());
	}

	@Test
	public void whereTwoCharactersDisagreeTheOneLoggedInWins()
	{
		// Only stale copies can disagree, since filing takes an item out of everyone else's lists.
		FakeSettings settings = new FakeSettings();
		settings.set(DUKE, MarkStore.keyFor(SortRule.KEEP), String.valueOf(SWORD));
		settings.set(LUKE, MarkStore.keyFor(SortRule.DROP), String.valueOf(SWORD));
		assertEquals(SortRule.KEEP, new MarkStore(settings).load().markOf(SWORD));
		settings.own = LUKE;
		assertEquals(SortRule.DROP, new MarkStore(settings).load().markOf(SWORD));
	}

	@Test
	public void theOldSharedListsMoveUnderTheCharacterLoggedInOnce()
	{
		FakeSettings settings = new FakeSettings();
		settings.plugin.put(MarkStore.keyFor(SortRule.KEEP), SWORD + "," + SHIELD);
		// Luke already has the shield as Drop: that stays his.
		settings.set(LUKE, MarkStore.keyFor(SortRule.DROP), String.valueOf(SHIELD));
		MarkStore store = new MarkStore(settings);
		assertTrue(store.migrate());
		assertEquals(String.valueOf(SWORD), settings.list(DUKE, SortRule.KEEP));
		assertEquals(String.valueOf(SHIELD), settings.list(LUKE, SortRule.DROP));
		assertNull("the old setting is gone", settings.get(MarkStore.keyFor(SortRule.KEEP)));
		assertFalse("nothing left to move", store.migrate());
	}
}
