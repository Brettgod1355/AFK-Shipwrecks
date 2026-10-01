/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Fails the build if the plugin's own sources use anything the Plugin Hub rejects.
 * <p>
 * The patterns come from the Hub wiki, the packager's disallowed-API file, the Hub's own agent
 * rulebook and what its reviewers have written on pull requests. Comments and string literals are
 * stripped before matching, so a word in a sentence does not count; only code does. This test
 * itself uses {@code java.nio} to read the sources, which is fine: the rules bind {@code src/main},
 * and nothing under {@code src/test} ships.
 */
public class HubRulesTest
{
	private static final Path MAIN_SOURCES = Paths.get("src", "main", "java");
	private static final Path MAIN_RESOURCES = Paths.get("src", "main", "resources");

	/** Pattern, then the rule and where it comes from. */
	static final String[][] FORBIDDEN = {
		{"java\\.lang\\.reflect", "reflection is forbidden (Hub wiki, Rejected features)"},
		{"Class\\.forName\\(", "reflection is forbidden (Hub wiki)"},
		{"\\.getDeclared(Method|Field|Constructor)s?\\(", "reflection is forbidden (Hub wiki)"},
		{"\\.getMethod\\(", "reflection is forbidden (Hub wiki)"},
		{"\\bTypeToken\\b", "Gson TypeToken is reflection (reviewer, plugin-hub#16712)"},
		{"Thread\\.sleep\\(", "Thread.sleep is not allowed; use a scheduled executor (reviewer, plugin-hub#16712)"},
		{"\\.interrupt\\(\\)", "Thread.interrupt is not allowed (reviewer, plugin-hub#16712)"},
		{"awaitTermination\\(", "never block in shutDown; use shutdownNow (Hub AGENTS.md)"},
		{"java\\.awt\\.Desktop", "use LinkBrowser, not Desktop (reviewer, plugin-hub#16712)"},
		{"KeyboardFocusManager", "KeyboardFocusManager is not allowed (reviewer, plugin-hub#17279)"},
		{"addAWTEventListener", "JVM-wide input hooks are not allowed; use KeyManager (shared rules)"},
		{"System\\.(out|err)\\.", "system i/o, use a logger instead (review bot, plugin-hub#17265)"},
		{"System\\.getenv\\(", "environment access is not allowed (Hub review list)"},
		{"javax\\.sound", "javax.sound is not allowed; use the Notifier (Hub review list)"},
		{"Runtime\\.getRuntime\\(\\)", "no external processes (Hub wiki)"},
		{"ProcessBuilder", "no external processes (Hub wiki)"},
		{"\\.menuAction\\(", "client.menuAction is not allowed (reviewer, plugin-hub#15925)"},
		{"invokeMenuAction", "menu invocation is not allowed (Hub)"},
		{"hopToWorld\\(", "client.hopToWorld needs manual review and is not for this plugin (reviewer, plugin-hub#17120)"},
		{"\\.runScript\\(", "client scripts that act as input are automation (plugin-hub#12507); none are needed here"},
		{"java\\.io\\.(File|FileInputStream|FileOutputStream|FileReader|FileWriter|RandomAccessFile)\\b", "file i/o only through Filepath (reviewers, many PRs)"},
		{"java\\.nio\\.file", "file i/o only through Filepath (reviewers, many PRs)"},
		{"JFileChooser", "use Filepath.Chooser (Hub AGENTS.md)"},
		{"RuneLite\\.(CACHE_DIR|RUNELITE_DIR)", "no files in RuneLite's directories directly (reviewer, plugin-hub#15978)"},
		{"HttpURLConnection", "use the injected OkHttpClient (Hub AGENTS.md)"},
		{"\\.openConnection\\(", "URL.openConnection is not allowed (review bot, plugin-hub#17265)"},
		{"java\\.net\\.http", "use the injected OkHttpClient (Hub AGENTS.md)"},
		{"new OkHttpClient", "never construct an OkHttpClient; inject it (packager disallowed-apis)"},
		{"new Gson(Builder)?\\(", "never construct a Gson; inject it (packager disallowed-apis)"},
		{"\\bWidgetInfo\\b", "use InterfaceID, not WidgetInfo (packager disallowed-apis)"},
		{"\\bWidgetID\\b", "use InterfaceID, not WidgetID (packager disallowed-apis)"},
		{"\\.getVar\\(", "use getVarbitValue, not getVar (packager disallowed-apis)"},
		{"\\.getResource\\(", "use getResourceAsStream, not getResource (reviewer, plugin-hub#15816)"},
		{"sun\\.misc\\.Unsafe", "no native memory access (Hub AGENTS.md)"},
		{"Object(Input|Output)Stream", "no Java serialization (Hub AGENTS.md)"},
		{"enabledByDefault\\s*=\\s*false", "do not disable the plugin by default (review bot, plugin-hub#17200)"},
		{"net\\.runelite\\.client\\.account", "account internals are disallowed (packager disallowed-apis)"},
		{"^\\s*package\\s+net\\.runelite", "the net.runelite namespace is not allowed (packager)"},
	};

	@Test
	public void mainSourcesUseNothingTheHubRejects() throws IOException
	{
		List<String> findings = new ArrayList<>();
		for (Path file : javaFiles(MAIN_SOURCES))
		{
			String source = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
			for (String violation : violations(source))
			{
				findings.add(file + ": " + violation);
			}
		}
		assertTrue("Plugin Hub rule violations:\n" + String.join("\n", findings), findings.isEmpty());
	}

	@Test
	public void noServicesFileAndNoBuildArtifactsAreShipped() throws IOException
	{
		Path services = MAIN_RESOURCES.resolve("META-INF").resolve("services");
		assertFalse("META-INF/services must not exist (Hub AGENTS.md)", Files.exists(services));
		try (Stream<Path> all = Files.walk(Paths.get("src")))
		{
			List<String> artifacts = new ArrayList<>();
			all.filter(p -> p.toString().endsWith(".class") || p.toString().endsWith(".tmp"))
				.forEach(p -> artifacts.add(p.toString()));
			assertTrue("build artifacts under src: " + artifacts, artifacts.isEmpty());
		}
	}

	@Test
	public void iconIsARealPngWithinTheHubLimits() throws IOException
	{
		Path icon = Paths.get("icon.png");
		assertTrue("icon.png missing", Files.exists(icon));
		byte[] bytes = Files.readAllBytes(icon);
		assertTrue("icon.png is above the 256 KiB packager limit", bytes.length <= 256 * 1024);
		byte[] signature = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};
		for (int i = 0; i < signature.length; i++)
		{
			assertEquals("icon.png is not a real PNG (byte " + i + ")", signature[i], bytes[i]);
		}
		int width = readInt(bytes, 16);
		int height = readInt(bytes, 20);
		assertTrue("icon.png is " + width + "x" + height + "; the Hub allows at most 48x72", width <= 48 && height <= 72);
	}

	@Test
	public void licenseIsTheHubTemplateShape() throws IOException
	{
		String license = new String(Files.readAllBytes(Paths.get("LICENSE")), StandardCharsets.UTF_8);
		assertTrue("LICENSE must be the BSD 2-Clause text the Hub uses", license.contains("BSD 2-Clause License"));
		assertTrue("LICENSE must keep 'All rights reserved.' (reviewer, plugin-hub#16590)", license.contains("All rights reserved."));
		assertTrue("LICENSE must say COPYRIGHT HOLDERS, plural (reviewer, plugin-hub#16590)", license.contains("COPYRIGHT HOLDERS"));
	}

	@Test
	public void theGuardItselfCatchesEachRule()
	{
		// Prove the scanner fails on a violation, so a green run means something.
		assertEquals(1, violations("import java.lang.reflect.Method;\nclass X {}").size());
		assertEquals(1, violations("class X { void f() throws Exception { Thread.sleep(10); } }").size());
		assertEquals(1, violations("class X { Object g = new Gson(); }").size());
		// ...and ignores the same words in comments and strings.
		assertTrue(violations("// Thread.sleep( is forbidden\nclass X { String s = \"new Gson(\"; }").isEmpty());
	}

	/** Every forbidden pattern found in code, with the rule it breaks. */
	static List<String> violations(String source)
	{
		String code = stripCommentsAndStrings(source);
		List<String> found = new ArrayList<>();
		for (String[] rule : FORBIDDEN)
		{
			if (Pattern.compile(rule[0], Pattern.MULTILINE).matcher(code).find())
			{
				found.add(rule[1] + " [" + rule[0] + "]");
			}
		}
		return found;
	}

	private static String stripCommentsAndStrings(String source)
	{
		String noBlockComments = source.replaceAll("(?s)/\\*.*?\\*/", " ");
		String noLineComments = noBlockComments.replaceAll("//[^\\n]*", "");
		return noLineComments.replaceAll("\"(?:\\\\.|[^\"\\\\])*\"", "\"\"");
	}

	private static List<Path> javaFiles(Path root) throws IOException
	{
		List<Path> files = new ArrayList<>();
		if (!Files.exists(root))
		{
			return files;
		}
		try (Stream<Path> walk = Files.walk(root))
		{
			walk.filter(p -> p.toString().endsWith(".java")).forEach(files::add);
		}
		return files;
	}

	private static int readInt(byte[] bytes, int offset)
	{
		return ((bytes[offset] & 0xFF) << 24) | ((bytes[offset + 1] & 0xFF) << 16)
			| ((bytes[offset + 2] & 0xFF) << 8) | (bytes[offset + 3] & 0xFF);
	}
}
