/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.runelite.client.config.ConfigDescriptor;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigItemDescriptor;
import net.runelite.client.config.ConfigSectionDescriptor;

/**
 * "Find a setting" in the sidebar's Tips tab (owner, 2026-10-07: "Can we have a search bar in our
 * settings to search for a particular setting?"). RuneLite's settings panel is core UI that a plugin
 * may not change, so the search lives in our own sidebar and says where each match sits. The list
 * is read from the plugin's own settings as RuneLite describes them, so it never goes out of date.
 */
final class SettingsSearch
{
	/** The most matches listed at once; past that the words should be narrowed. */
	static final int MAX_RESULTS = 8;

	/** One setting as the search sees it. */
	static final class Entry
	{
		private final String name;
		/** The section it sits under, or empty for the settings at the top and bottom of the panel. */
		private final String section;
		private final String description;
		private final int sectionPosition;
		private final int position;

		Entry(String name, String section, String description, int sectionPosition, int position)
		{
			this.name = name;
			this.section = section;
			this.description = description;
			this.sectionPosition = sectionPosition;
			this.position = position;
		}

		String getName()
		{
			return name;
		}

		String getSection()
		{
			return section;
		}

		String getDescription()
		{
			return description;
		}

		int getSectionPosition()
		{
			return sectionPosition;
		}

		int getPosition()
		{
			return position;
		}

		/** Where to find it, worded for the sidebar. */
		String where()
		{
			return section.isEmpty() ? "AFK Shipwrecks settings" : "AFK Shipwrecks settings > " + section;
		}
	}

	private SettingsSearch()
	{
	}

	/** Every setting a player can see, in the order the settings panel lists them. */
	static List<Entry> entries(ConfigDescriptor descriptor)
	{
		Map<String, ConfigSectionDescriptor> sections = new HashMap<>();
		for (ConfigSectionDescriptor section : descriptor.getSections())
		{
			sections.put(section.getKey(), section);
		}
		List<Entry> entries = new ArrayList<>();
		for (ConfigItemDescriptor item : descriptor.getItems())
		{
			ConfigItem annotation = item.getItem();
			if (annotation.hidden() || annotation.name().isEmpty())
			{
				continue;
			}
			ConfigSectionDescriptor section = sections.get(annotation.section());
			// The panel lists sections and loose settings together by position; a loose setting comes
			// before a section that shares its position.
			entries.add(new Entry(annotation.name(),
				section == null ? "" : section.name(),
				annotation.description(),
				section == null ? annotation.position() : section.position(),
				section == null ? Integer.MIN_VALUE : annotation.position()));
		}
		entries.sort(Comparator.comparingInt(Entry::getSectionPosition).thenComparingInt(Entry::getPosition));
		return entries;
	}

	/**
	 * The settings matching every word typed, best first: a word in the name beats one only in the
	 * section, which beats one only in the description. An empty query matches nothing.
	 */
	static List<Entry> find(List<Entry> entries, String query)
	{
		String[] words = query == null ? new String[0] : query.toLowerCase(Locale.ROOT).trim().split("\\s+");
		if (words.length == 0 || words[0].isEmpty())
		{
			return Collections.emptyList();
		}
		List<Entry> found = new ArrayList<>();
		Map<Entry, Integer> scores = new IdentityHashMap<>();
		for (Entry entry : entries)
		{
			String name = entry.getName().toLowerCase(Locale.ROOT);
			String section = entry.getSection().toLowerCase(Locale.ROOT);
			String description = entry.getDescription().toLowerCase(Locale.ROOT);
			int score = 0;
			boolean all = true;
			for (String word : words)
			{
				if (name.contains(word))
				{
					score += 100;
				}
				else if (section.contains(word))
				{
					score += 10;
				}
				else if (description.contains(word))
				{
					score += 1;
				}
				else
				{
					all = false;
					break;
				}
			}
			if (all)
			{
				found.add(entry);
				scores.put(entry, score);
			}
		}
		// Stable: equal scores keep the settings panel's own order.
		found.sort(Comparator.comparingInt((Entry e) -> scores.get(e)).reversed());
		return found.size() > MAX_RESULTS ? new ArrayList<>(found.subList(0, MAX_RESULTS)) : found;
	}
}
