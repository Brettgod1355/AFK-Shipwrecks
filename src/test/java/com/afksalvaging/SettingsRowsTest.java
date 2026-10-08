/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.ConfigDescriptor;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigItemDescriptor;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.ConfigSectionDescriptor;
import net.runelite.client.config.Range;
import net.runelite.client.config.Units;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class SettingsRowsTest
{
	/** The plugin's settings described the way RuneLite's ConfigManager describes them (tests may reflect). */
	static ConfigDescriptor describe() throws Exception
	{
		List<ConfigSectionDescriptor> sections = new ArrayList<>();
		for (Field field : AfkSalvagingConfig.class.getDeclaredFields())
		{
			ConfigSection section = field.getAnnotation(ConfigSection.class);
			if (section != null)
			{
				sections.add(new ConfigSectionDescriptor((String) field.get(null), section));
			}
		}
		List<ConfigItemDescriptor> items = new ArrayList<>();
		for (Method method : AfkSalvagingConfig.class.getDeclaredMethods())
		{
			ConfigItem item = method.getAnnotation(ConfigItem.class);
			if (item != null)
			{
				items.add(new ConfigItemDescriptor(item, method.getGenericReturnType(), method.getAnnotation(Range.class),
					method.getAnnotation(Alpha.class), method.getAnnotation(Units.class)));
			}
		}
		return new ConfigDescriptor(AfkSalvagingConfig.class.getAnnotation(ConfigGroup.class), sections, items);
	}

	static List<SettingsRows.Row> rows() throws Exception
	{
		return SettingsRows.rows(describe());
	}

	@Test
	public void everyVisibleSettingIsOnThePageAndNoHiddenOneIs() throws Exception
	{
		Set<String> onPage = new HashSet<>();
		for (SettingsRows.Row row : rows())
		{
			onPage.add(row.key);
		}
		for (ConfigItemDescriptor item : describe().getItems())
		{
			ConfigItem annotation = item.getItem();
			boolean visible = !annotation.hidden() && !annotation.name().isEmpty();
			assertEquals(annotation.keyName() + (visible ? " is missing from the page (a new kind of setting?)" : " is hidden"),
				visible, onPage.contains(annotation.keyName()));
		}
	}

	@Test
	public void everyDropdownHasItsChoices() throws Exception
	{
		for (SettingsRows.Row row : rows())
		{
			if (row.kind == SettingsRows.Kind.CHOICE)
			{
				assertNotNull(row.key, row.options);
				assertTrue(row.key, row.options.length > 1);
			}
		}
		assertEquals(SettingsRows.Kind.CHOICE, SettingsRows.kindOf(WreckMarking.class));
		assertEquals(SettingsRows.Kind.TOGGLE, SettingsRows.kindOf(boolean.class));
	}

	@Test
	public void numbersKeepTheirRangeAndUnits() throws Exception
	{
		for (SettingsRows.Row row : rows())
		{
			if (row.key.equals("reminderGraceSeconds"))
			{
				assertEquals(SettingsRows.Kind.NUMBER, row.kind);
				assertFalse(row.units.isEmpty());
				assertTrue(row.max < Integer.MAX_VALUE);
				return;
			}
		}
		throw new AssertionError("reminderGraceSeconds not found");
	}

	@Test
	public void theLooseSettingsComeFirstUnderGeneral() throws Exception
	{
		List<SettingsRows.Row> all = rows();
		assertEquals(SettingsRows.GENERAL, all.get(0).section);
		int general = 0;
		for (SettingsRows.Row row : all)
		{
			general += row.section.equals(SettingsRows.GENERAL) ? 1 : 0;
		}
		assertEquals("Default notification and Say when updated", 2, general);
	}

	@Test
	public void searchMatchesEveryWordInNameSectionOrDescription() throws Exception
	{
		List<String> found = new ArrayList<>();
		for (SettingsRows.Row row : rows())
		{
			if (SettingsRows.matches(row, SettingsRows.words("BANNER size")))
			{
				found.add(row.name);
			}
		}
		assertTrue(found.contains("Banner size"));
		assertTrue(SettingsRows.words("   ").isEmpty());
		for (SettingsRows.Row row : rows())
		{
			assertFalse(SettingsRows.matches(row, SettingsRows.words("banner zzzz")));
		}
	}

	@Test
	public void everySectionStartsClosedAndHoldsSomething() throws Exception
	{
		ConfigDescriptor descriptor = describe();
		Set<String> used = new HashSet<>();
		for (ConfigItemDescriptor item : descriptor.getItems())
		{
			if (!item.getItem().hidden())
			{
				used.add(item.getItem().section());
			}
		}
		for (ConfigSectionDescriptor section : descriptor.getSections())
		{
			assertTrue(section.name() + " starts open", section.getSection().closedByDefault());
			assertTrue(section.name() + " is empty", used.contains(section.getKey()));
		}
	}
}
