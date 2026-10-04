/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

import net.runelite.api.gameval.InterfaceID;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MainInterfaceTest
{
	@Test
	public void theViewportContainersAreKnownInEveryLayout()
	{
		assertTrue(AfkSalvagingPlugin.isMainViewportContainer(InterfaceID.Toplevel.MAINMODAL));
		assertTrue(AfkSalvagingPlugin.isMainViewportContainer(InterfaceID.ToplevelOsrsStretch.FLOATER));
		assertTrue(AfkSalvagingPlugin.isMainViewportContainer(InterfaceID.ToplevelPreEoc.MAINMODAL_BACKGROUNDS));
		assertFalse("side panels", AfkSalvagingPlugin.isMainViewportContainer(InterfaceID.ToplevelOsrsStretch.SIDE3));
		assertFalse("the chatbox", AfkSalvagingPlugin.isMainViewportContainer(InterfaceID.Toplevel.CHAT_CONTAINER));
		assertFalse("a skill guide's own window", AfkSalvagingPlugin.isMainViewportContainer(InterfaceID.SkillGuide.WINDOW));
	}
}
