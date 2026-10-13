/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

import com.afksalvaging.replay.Recording;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Assume;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * Replays Black Box recordings of real play through the hold count and fails on drift: a hold opened with the
 * count saying something else. Local only (owner, 2026-10-13): recordings live in {@code recordings/}, which
 * git ignores, one folder per character holding Black Box's recording folders. Without it, as on GitHub and
 * in the Plugin Hub's build, the test is skipped. Every hold read replayed is written to
 * {@code build/replay/hold-reads.txt}.
 */
public class HoldCountReplayTest
{
	private static final Path RECORDINGS = Paths.get("recordings");
	private static final Path REPORT = Paths.get("build", "replay", "hold-reads.txt");

	@Test
	public void theCountMatchesTheHoldEveryTimeItIsOpened() throws IOException
	{
		Assume.assumeTrue("no recordings on this machine", Files.isDirectory(RECORDINGS));
		List<String> problems = new ArrayList<>();
		List<String> report = new ArrayList<>();
		int played = 0;
		for (Path character : list(RECORDINGS))
		{
			List<Recording> recordings = new ArrayList<>();
			List<String> names = new ArrayList<>();
			for (Path folder : list(character))
			{
				Path events = folder.resolve("events.jsonl");
				if (Files.isRegularFile(events))
				{
					recordings.add(Recording.read(events));
					names.add(character.getFileName() + "/" + folder.getFileName());
				}
			}
			if (recordings.isEmpty())
			{
				continue;
			}
			played += recordings.size();
			HoldCountReplay.Result result = new HoldCountReplay().play(recordings, names);
			for (HoldCountReplay.Check check : result.checks)
			{
				report.add((check.drift() ? "DRIFT " : "      ") + check);
			}
			for (HoldCountReplay.Check check : result.drift())
			{
				problems.add("drift: " + check);
			}
			for (int id : result.unknownCrew)
			{
				problems.add(character.getFileName() + ": crewmate id " + id + " has no name in HoldCountReplay.CREW");
			}
		}
		Assume.assumeTrue("no recordings in " + RECORDINGS, played > 0);
		Files.createDirectories(REPORT.getParent());
		Files.write(REPORT, report, StandardCharsets.UTF_8);
		assertTrue(played + " recordings replayed:\n" + String.join("\n", problems), problems.isEmpty());
	}

	private static List<Path> list(Path dir) throws IOException
	{
		try (Stream<Path> entries = Files.list(dir))
		{
			return entries.filter(Files::isDirectory).sorted().collect(Collectors.toList());
		}
	}
}
