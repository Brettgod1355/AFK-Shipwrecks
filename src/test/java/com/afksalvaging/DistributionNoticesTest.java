/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Scanner;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * The Plugin Hub builds its own jar from the resources output, so a `jar {}` block in this
 * build never reaches the distributed artifact. These copies are what BSD 2-Clause requires
 * a binary redistribution to carry, so they must stay identical to the originals.
 */
public class DistributionNoticesTest
{
	@Test
	public void distributedNoticesMatchTheRepositoryRoot() throws Exception
	{
		for (String name : new String[]{"LICENSE", "THIRD_PARTY_NOTICES.md"})
		{
			File root = new File(name);
			assertNotNull(name + " is missing from the repository root", root.exists() ? root : null);
			String expected = new String(Files.readAllBytes(root.toPath()), StandardCharsets.UTF_8);

			try (InputStream packaged = getClass().getResourceAsStream("/META-INF/" + name))
			{
				assertNotNull("META-INF/" + name + " is not on the classpath, so the Plugin Hub jar "
					+ "would ship without it", packaged);
				try (Scanner scanner = new Scanner(packaged, StandardCharsets.UTF_8.name()))
				{
					scanner.useDelimiter("\\A");
					assertEquals("src/main/resources/META-INF/" + name + " has drifted from the root "
						+ name, expected, scanner.hasNext() ? scanner.next() : "");
				}
			}
		}
	}
}
