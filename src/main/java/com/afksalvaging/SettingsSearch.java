/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
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
	/** The line break in a setting's description; RuneLite shows the description as HTML hover text. */
	static final String TOOLTIP_BREAK = "<br>";
	/** The longest line of hover text, in characters, before it needs a {@link #TOOLTIP_BREAK}. */
	static final int TOOLTIP_LINE = 64;

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
			// Descriptions break their lines with <br> so RuneLite's hover text stays on screen; here they are text.
			entries.add(new Entry(annotation.name(),
				section == null ? "" : section.name(),
				annotation.description().replace(TOOLTIP_BREAK, " "),
				section == null ? annotation.position() : section.position(),
				section == null ? Integer.MIN_VALUE : annotation.position()));
		}
		entries.sort(Comparator.comparingInt(Entry::getSectionPosition).thenComparingInt(Entry::getPosition));
		return entries;
	}

	/**
	 * Words that say what to do with a setting rather than which one, skipped so "turn off highlight" finds what
	 * "highlight" finds (2.3, issue #14: a player searching for how to turn the highlights off found nothing).
	 */
	private static final Set<String> FILLER = new HashSet<>(Arrays.asList(
		"turn", "off", "on", "disable", "enable", "remove", "how", "to", "the", "a", "my", "i", "can"));

	/** Words players use for the same outline; a match through another of them ranks below any direct match. */
	private static final Set<String> OUTLINE_WORDS = new HashSet<>(Arrays.asList("highlight", "outline", "box"));

	/**
	 * The settings matching every word typed, best first: a word in the name beats one only in the
	 * section, which beats one only in the description. An empty query matches nothing. Endings are
	 * forgiven ("highlighting" finds "highlight"), filler words are skipped unless nothing else was
	 * typed, and highlight, outline and box find each other.
	 */
	static List<Entry> find(List<Entry> entries, String query)
	{
		String[] typed = query == null ? new String[0] : query.toLowerCase(Locale.ROOT).trim().split("\\s+");
		if (typed.length == 0 || typed[0].isEmpty())
		{
			return Collections.emptyList();
		}
		List<String> words = new ArrayList<>();
		for (String word : typed)
		{
			if (!FILLER.contains(word))
			{
				words.add(stem(word));
			}
		}
		if (words.isEmpty())
		{
			// Only filler typed ("off", say): search for that as typed.
			for (String word : typed)
			{
				words.add(stem(word));
			}
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
				int best = score(word, name, section, description);
				if (best == 0 && OUTLINE_WORDS.contains(word))
				{
					for (String other : OUTLINE_WORDS)
					{
						if (score(other, name, section, description) > 0)
						{
							best = 1;
						}
					}
				}
				if (best == 0)
				{
					all = false;
					break;
				}
				score += best;
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

	/** 100 for a word in the name, 10 in the section, 2 in the description, 0 nowhere (1 is a match by synonym). */
	private static int score(String word, String name, String section, String description)
	{
		if (name.contains(word))
		{
			return 100;
		}
		if (section.contains(word))
		{
			return 10;
		}
		return description.contains(word) ? 2 : 0;
	}

	/**
	 * The word without a plural or -ing/-ed ending, so "highlighting", "highlights" and "boxes" match "highlight"
	 * and "box". Only ever shortens, and the match is by contains, so a stemmed word finds everything the typed
	 * one did. Short words are left alone.
	 */
	static String stem(String word)
	{
		String stem = word;
		if (stem.endsWith("ings"))
		{
			stem = stem.substring(0, stem.length() - 4);
		}
		else if (stem.endsWith("ing") || stem.endsWith("ies"))
		{
			stem = stem.substring(0, stem.length() - 3);
		}
		else if (stem.endsWith("xes") || stem.endsWith("ches") || stem.endsWith("shes") || stem.endsWith("sses"))
		{
			stem = stem.substring(0, stem.length() - 2);
		}
		else if (stem.endsWith("ed"))
		{
			stem = stem.substring(0, stem.length() - 2);
		}
		else if (stem.endsWith("s") && !stem.endsWith("ss"))
		{
			stem = stem.substring(0, stem.length() - 1);
		}
		return stem.length() >= 3 ? stem : word;
	}
}
