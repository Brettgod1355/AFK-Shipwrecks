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
	public void aModalHungOnTheFrameCountsInEveryLayout()
	{
		assertTrue(AfkSalvagingPlugin.isToplevelComponent(InterfaceID.Toplevel.MAINMODAL));
		assertTrue(AfkSalvagingPlugin.isToplevelComponent(InterfaceID.ToplevelOsrsStretch.MAINMODAL));
		assertTrue(AfkSalvagingPlugin.isToplevelComponent(InterfaceID.ToplevelPreEoc.MAINMODAL));
		assertTrue("the floating world map's home", AfkSalvagingPlugin.isToplevelComponent(InterfaceID.ToplevelPreEoc.FLOATER));
	}

	@Test
	public void dialoguesAndSidePanelsHangElsewhere()
	{
		assertFalse("dialogues open inside the chatbox", AfkSalvagingPlugin.isToplevelComponent(InterfaceID.CHATBOX << 16));
		assertFalse(AfkSalvagingPlugin.isToplevelComponent(InterfaceID.SailingBoatCargohold.UNIVERSE));
		assertFalse(AfkSalvagingPlugin.isToplevelComponent(InterfaceID.SkillGuide.WINDOW));
	}

	@Test
	public void theViewportContainersAreKnownInEveryLayout()
	{
		assertTrue(AfkSalvagingPlugin.isMainViewportContainer(InterfaceID.Toplevel.MAINMODAL));
		assertTrue(AfkSalvagingPlugin.isMainViewportContainer(InterfaceID.ToplevelOsrsStretch.FLOATER));
		assertTrue(AfkSalvagingPlugin.isMainViewportContainer(InterfaceID.ToplevelPreEoc.MAINMODAL_BACKGROUNDS));
		assertFalse("side panels", AfkSalvagingPlugin.isMainViewportContainer(InterfaceID.ToplevelOsrsStretch.SIDE3));
		assertFalse("the chatbox", AfkSalvagingPlugin.isMainViewportContainer(InterfaceID.Toplevel.CHAT_CONTAINER));
	}
}
