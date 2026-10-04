/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ShortestPathPresenceTest
{
	@Test
	public void onlyAMissingOrSwitchedOffPluginIsAProblem()
	{
		assertNull(ShortestPathPresence.READY.problem());
		assertTrue(ShortestPathPresence.MISSING.problem().contains("Install it from the Plugin Hub"));
		assertTrue(ShortestPathPresence.DISABLED.problem().contains("turned off"));
	}

	@Test
	public void noPluginManagerMeansNoShortestPath()
	{
		assertEquals(ShortestPathPresence.MISSING, ShortestPathPresence.check(null));
	}

	@Test
	public void looksForTheDescriptorName()
	{
		assertEquals("Shortest Path", ShortestPathPresence.PLUGIN_NAME);
	}
}
