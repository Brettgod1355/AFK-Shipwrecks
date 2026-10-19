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
import static org.junit.Assert.assertTrue;

public class SettingsSearchTest
{
	/** The plugin's settings described the way RuneLite's ConfigManager describes them. */
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

	private static List<SettingsSearch.Entry> entries() throws Exception
	{
		return SettingsSearch.entries(describe());
	}

	@Test
	public void hiddenSettingsAreNeverFound() throws Exception
	{
		for (SettingsSearch.Entry entry : entries())
		{
			assertFalse(entry.getName().isEmpty());
		}
		assertTrue(SettingsSearch.find(entries(), "wreckMarkingCarried").isEmpty());
		assertTrue(SettingsSearch.find(entries(), "lastVersion").isEmpty());
	}

	@Test
	public void hoverTextIsWrappedSoItStaysOnScreen() throws Exception
	{
		// Owner, 2026-10-19: one long line ran off the screen.
		List<String> descriptions = new ArrayList<>();
		for (Method method : AfkSalvagingConfig.class.getDeclaredMethods())
		{
			ConfigItem item = method.getAnnotation(ConfigItem.class);
			if (item != null && !item.hidden())
			{
				descriptions.add(item.description());
			}
		}
		for (Field field : AfkSalvagingConfig.class.getDeclaredFields())
		{
			ConfigSection section = field.getAnnotation(ConfigSection.class);
			if (section != null)
			{
				descriptions.add(section.description());
			}
		}
		for (String description : descriptions)
		{
			for (String line : description.split(SettingsSearch.TOOLTIP_BREAK))
			{
				assertTrue(line, line.length() <= SettingsSearch.TOOLTIP_LINE);
			}
		}
		for (SettingsSearch.Entry entry : entries())
		{
			assertFalse("searched and shown as plain text", entry.getDescription().contains("<"));
		}
	}

	@Test
	public void aWordFindsTheSettingAndSaysWhereItIs() throws Exception
	{
		List<SettingsSearch.Entry> found = SettingsSearch.find(entries(), "banner size");
		assertEquals("Banner size", found.get(0).getName());
		assertEquals("AFK Shipwrecks settings > Banner", found.get(0).where());

		found = SettingsSearch.find(entries(), "MARK WRECKS");
		assertEquals("Mark wrecks", found.get(0).getName());
		assertEquals("Wrecks", found.get(0).getSection());
	}

	@Test
	public void nameMatchesComeBeforeDescriptionMatches() throws Exception
	{
		List<SettingsSearch.Entry> found = SettingsSearch.find(entries(), "grace");
		assertEquals("Grace period", found.get(0).getName());
	}

	@Test
	public void everyWordMustMatchAndEmptyFindsNothing() throws Exception
	{
		assertTrue(SettingsSearch.find(entries(), "banner zzzz").isEmpty());
		assertTrue(SettingsSearch.find(entries(), "   ").isEmpty());
		assertTrue(SettingsSearch.find(entries(), null).isEmpty());
	}

	@Test
	public void resultsAreCapped() throws Exception
	{
		// "e" is in nearly every setting.
		assertEquals(SettingsSearch.MAX_RESULTS, SettingsSearch.find(entries(), "e").size());
	}

	@Test
	public void looseSettingsKeepThePanelsOrder() throws Exception
	{
		List<SettingsSearch.Entry> all = entries();
		assertEquals("Default notification", all.get(0).getName());
		assertEquals("Say when updated", all.get(all.size() - 1).getName());
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

	@Test
	public void onlyTwoSettingsSitOutsideTheSections() throws Exception
	{
		List<String> loose = new ArrayList<>();
		for (SettingsSearch.Entry entry : entries())
		{
			if (entry.getSection().isEmpty())
			{
				loose.add(entry.getName());
			}
		}
		assertEquals(2, loose.size());
	}

	@Test
	public void everyVisibleSettingHasItsOwnName() throws Exception
	{
		// Two "Repeat every" settings read alike in search results and on the wiki (2.3).
		Set<String> names = new HashSet<>();
		for (SettingsSearch.Entry entry : entries())
		{
			assertTrue(entry.getName() + " is used twice", names.add(entry.getName()));
		}
	}

	@Test
	public void highlightFindsTheNextStepHighlights() throws Exception
	{
		// Issue #14: a player could not find how to turn the station, hold and hook outlines off.
		List<String> found = new ArrayList<>();
		for (SettingsSearch.Entry entry : SettingsSearch.find(entries(), "highlight"))
		{
			if (entry.getSection().equals("Next step highlights"))
			{
				found.add(entry.getName());
			}
		}
		assertTrue(found.contains("Salvage highlight"));
		assertTrue(found.contains("Hook highlight"));
		assertTrue(found.contains("Empty hook crew highlight"));
	}

	/** The names found for a search, best first. */
	private static List<String> names(String query) throws Exception
	{
		List<String> names = new ArrayList<>();
		for (SettingsSearch.Entry entry : SettingsSearch.find(entries(), query))
		{
			names.add(entry.getName());
		}
		return names;
	}

	@Test
	public void theWaysPlayersAskForTheHighlightsAllFindThem() throws Exception
	{
		// Issue #14 asked for "highlighting"; on 2.2 that and the rest of these found nothing (2026-10-19).
		for (String query : new String[]{"highlight", "highlights", "highlighting", "turn off highlight",
			"disable highlight", "remove highlight", "how to turn off the highlighting", "Highlights off"})
		{
			List<String> found = names(query);
			assertTrue(query + ": " + found, found.contains("Salvage highlight"));
			assertTrue(query + ": " + found, found.contains("Hook highlight"));
		}
		assertEquals("Hook highlight", names("hook highlight").get(0));
		assertTrue(names("outline").contains("Salvage highlight"));
	}

	@Test
	public void endingsAreForgivenButEveryWordStillCounts() throws Exception
	{
		assertEquals("Fill the boxes", names("filling boxes").get(0));
		assertEquals("Show nearest docks", names("nearest dock").get(0));
		assertEquals("Banner size", names("banners size").get(0));
		assertTrue(names("highlighting zzzz").isEmpty());
		assertEquals("hook", SettingsSearch.stem("hooks"));
		assertEquals("box", SettingsSearch.stem("boxes"));
		assertEquals("need", SettingsSearch.stem("need"));
		assertEquals("class", SettingsSearch.stem("class"));
	}

	@Test
	public void onlyFillerTypedIsSearchedAsTyped() throws Exception
	{
		assertFalse(SettingsSearch.find(entries(), "off").isEmpty());
	}
}
