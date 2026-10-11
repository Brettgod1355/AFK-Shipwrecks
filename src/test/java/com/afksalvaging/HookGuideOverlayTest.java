/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class HookGuideOverlayTest
{
	@Test
	public void readsTheCrewRowsName()
	{
		assertEquals("Ex-Captain Siad", HookGuideOverlay.rowName("Ex-Captain Siad<br>H3 P2 D3"));
		assertEquals("Cabin Boy Jenkins", HookGuideOverlay.rowName("<col=ffffff>Cabin Boy Jenkins</col><br>H2 P2 D4"));
		assertEquals("Unassign", HookGuideOverlay.rowName("Unassign"));
		assertEquals("", HookGuideOverlay.rowName(null));
	}

	@Test
	public void readsTheHooksRequirementFromTheTitle()
	{
		assertEquals(3, HookGuideOverlay.needed("Rune salvaging hook (D3)"));
		assertEquals(1, HookGuideOverlay.needed("<col=ff981f>Bronze salvaging hook (D1)</col>"));
		assertEquals(1, HookGuideOverlay.needed("Salvaging hook"));
	}

	@Test
	public void picturesComeFromTheSailingSprites()
	{
		// Read from the client log on 2026-10-07: the hook icon and the empty place on an Assign button.
		assertEquals(7073, HookGuideOverlay.HOOK_ICON);
		assertEquals(7102, HookGuideOverlay.EMPTY_PLACE);
	}
}
