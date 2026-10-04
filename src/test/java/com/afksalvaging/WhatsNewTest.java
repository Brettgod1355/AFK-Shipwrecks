/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class WhatsNewTest
{
	@Test
	public void onlyARememberedCargoCountShowsCargoHoldAlertWasUsed()
	{
		// RuneLite writes every default before start-up, so only the counts 1.0 saved itself say it was used.
		assertFalse("no characters", WhatsNew.usedBefore(Collections.emptyList()));
		assertFalse("characters with nothing remembered", WhatsNew.usedBefore(Arrays.asList(Collections.<String>emptyList(), Collections.<String>emptyList())));
		assertTrue(WhatsNew.usedBefore(Arrays.asList(Collections.<String>emptyList(), Collections.singletonList("used.2"))));
	}

	@Test
	public void theVersionInCodeMatchesTheBuild() throws IOException
	{
		assertEquals("build.gradle", WhatsNew.VERSION, versionIn("build.gradle", "version\\s*=\\s*'([^']+)'"));
		assertEquals("runelite-plugin.properties", WhatsNew.VERSION,
			versionIn("runelite-plugin.properties", "(?m)^version=(.+)$"));
	}

	@Test
	public void aFreshInstallSaysNothingAndTheSameVersionSaysNothing()
	{
		assertNull(WhatsNew.message(null, false));
		assertNull(WhatsNew.message("", false));
		assertNull(WhatsNew.message(WhatsNew.VERSION, true));
	}

	@Test
	public void anUpdateSaysOneLineWithTheVersionAndTheNote()
	{
		String line = WhatsNew.message("1.9", true);
		assertTrue(line, line.startsWith("AFK Shipwrecks updated to " + WhatsNew.VERSION + ": "));
		assertTrue(line, line.endsWith(WhatsNew.NOTE + "."));
		assertFalse("no links in chat", line.contains("http"));
	}

	@Test
	public void comingFromTheVersionThatKeptNoVersionIsTheRename()
	{
		String line = WhatsNew.message(null, true);
		assertTrue(line, line.startsWith("Cargo Hold Alert is now AFK Shipwrecks " + WhatsNew.VERSION + ": "));
	}

	@Test
	public void theNoteIsOneLineWithoutLinks()
	{
		assertFalse(WhatsNew.NOTE.contains("http"));
		assertFalse(WhatsNew.NOTE.contains("\n"));
		assertFalse("the full stop is added when it is said", WhatsNew.NOTE.endsWith("."));
	}

	private static String versionIn(String file, String regex) throws IOException
	{
		String text = new String(Files.readAllBytes(Paths.get(file)), StandardCharsets.UTF_8);
		Matcher m = Pattern.compile(regex).matcher(text);
		assertTrue(file + " has a version", m.find());
		return m.group(1).trim();
	}
}
