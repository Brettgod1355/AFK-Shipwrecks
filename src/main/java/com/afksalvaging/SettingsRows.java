/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.runelite.client.config.ConfigDescriptor;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigItemDescriptor;
import net.runelite.client.config.ConfigSectionDescriptor;
import net.runelite.client.config.Notification;

/**
 * The settings as the sidebar's own settings page shows them (owner, 2026-10-08: "build our own
 * settings menu ... mirrored of the plugin settings in the runelite settings ... with a search bar").
 * Read from the description RuneLite builds of the plugin's settings, so the page always matches the
 * real ones. Nothing here inspects classes: each setting's kind is told from its declared type by
 * plain comparison, and the dropdowns' choices come from an explicit list of the plugin's own enums
 * (RuneFolio's settings page was refused for reflecting, plugin-hub review 2026-09-18).
 */
final class SettingsRows
{
	/** What sort of control a setting gets. */
	enum Kind
	{
		TOGGLE,
		NUMBER,
		TEXT,
		CHOICE,
		COLOUR,
		NOTIFICATION
	}

	/** One setting on the page. */
	static final class Row
	{
		final String key;
		final String name;
		final String description;
		/** The section's name, or "General" for the settings at the top and bottom of RuneLite's panel. */
		final String section;
		final int sectionPosition;
		final int position;
		final Kind kind;
		/** The declared type, for telling the kind of control. */
		final Object type;
		/** RuneLite's description of the setting, whose type it needs to read the value back. */
		final ConfigItemDescriptor item;
		final int min;
		final int max;
		final String units;
		final Object[] options;
		final boolean alpha;

		Row(String key, String name, String description, String section, int sectionPosition, int position, Kind kind,
			Object type, ConfigItemDescriptor item, int min, int max, String units, Object[] options, boolean alpha)
		{
			this.item = item;
			this.key = key;
			this.name = name;
			this.description = description;
			this.section = section;
			this.sectionPosition = sectionPosition;
			this.position = position;
			this.kind = kind;
			this.type = type;
			this.min = min;
			this.max = max;
			this.units = units;
			this.options = options;
			this.alpha = alpha;
		}
	}

	/** The heading for settings outside any section. */
	static final String GENERAL = "General";

	/**
	 * Every dropdown setting's choices, by its type. A new enum setting must be added here;
	 * SettingsRowsTest fails until it is.
	 */
	private static final Map<Object, Object[]> CHOICES = new HashMap<>();

	static
	{
		CHOICES.put(BoostWarning.class, BoostWarning.values());
		CHOICES.put(AfkSalvagingConfig.ClockFormat.class, AfkSalvagingConfig.ClockFormat.values());
		CHOICES.put(CounterStyle.class, CounterStyle.values());
		CHOICES.put(OverlayWhen.class, OverlayWhen.values());
		CHOICES.put(SortWhere.class, SortWhere.values());
		CHOICES.put(WreckMarking.class, WreckMarking.values());
	}

	private SettingsRows()
	{
	}

	/** The kind of control for a declared type, or null for a type the page cannot show. */
	static Kind kindOf(Object type)
	{
		if (type == boolean.class || type == Boolean.class)
		{
			return Kind.TOGGLE;
		}
		if (type == int.class || type == Integer.class)
		{
			return Kind.NUMBER;
		}
		if (type == String.class)
		{
			return Kind.TEXT;
		}
		if (type == Color.class)
		{
			return Kind.COLOUR;
		}
		if (type == Notification.class)
		{
			return Kind.NOTIFICATION;
		}
		return CHOICES.containsKey(type) ? Kind.CHOICE : null;
	}

	/** Every visible setting, in the order RuneLite's panel lists them, sections in order. */
	static List<Row> rows(ConfigDescriptor descriptor)
	{
		Map<String, ConfigSectionDescriptor> sections = new HashMap<>();
		for (ConfigSectionDescriptor section : descriptor.getSections())
		{
			sections.put(section.getKey(), section);
		}
		List<Row> rows = new ArrayList<>();
		for (ConfigItemDescriptor item : descriptor.getItems())
		{
			ConfigItem annotation = item.getItem();
			Object type = item.getType();
			Kind kind = kindOf(type);
			if (annotation.hidden() || annotation.name().isEmpty() || kind == null)
			{
				continue;
			}
			ConfigSectionDescriptor section = sections.get(annotation.section());
			int min = item.getRange() == null ? 0 : item.getRange().min();
			int max = item.getRange() == null ? Integer.MAX_VALUE : item.getRange().max();
			rows.add(new Row(annotation.keyName(), annotation.name(), annotation.description(),
				section == null ? GENERAL : section.name(),
				// The loose settings sit together first, under General.
				section == null ? Integer.MIN_VALUE : section.position(),
				annotation.position(), kind, type, item, min, max,
				item.getUnits() == null ? "" : item.getUnits().value(),
				CHOICES.get(type), item.getAlpha() != null));
		}
		rows.sort(Comparator.comparingInt((Row r) -> r.sectionPosition).thenComparingInt(r -> r.position));
		return rows;
	}

	/** The rows grouped under their section names, sections in order. */
	static Map<String, List<Row>> bySection(List<Row> rows)
	{
		Map<String, List<Row>> grouped = new LinkedHashMap<>();
		for (Row row : rows)
		{
			grouped.computeIfAbsent(row.section, s -> new ArrayList<>()).add(row);
		}
		return grouped;
	}

	/** The words typed in the search box, lower case; none for a blank box. */
	static List<String> words(String query)
	{
		String trimmed = query == null ? "" : query.toLowerCase(Locale.ROOT).trim();
		if (trimmed.isEmpty())
		{
			return Collections.emptyList();
		}
		List<String> words = new ArrayList<>();
		Collections.addAll(words, trimmed.split("\\s+"));
		return words;
	}

	/** Whether every word appears in the setting's name, section or description. */
	static boolean matches(Row row, List<String> words)
	{
		String text = (row.name + " " + row.section + " " + row.description).toLowerCase(Locale.ROOT);
		for (String word : words)
		{
			if (!text.contains(word))
			{
				return false;
			}
		}
		return true;
	}
}
