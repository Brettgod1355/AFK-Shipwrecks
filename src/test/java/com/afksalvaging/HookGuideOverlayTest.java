/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

import java.awt.Rectangle;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

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
	public void boxesAButtonThatMostlyShows()
	{
		Rectangle view = new Rectangle(0, 0, 100, 100);
		Rectangle inside = new Rectangle(10, 20, 30, 30);
		assertEquals(inside, HookGuideOverlay.shownPart(inside, view));
		// 25 of 30 rows show at the bottom edge: boxed where it shows.
		assertEquals(new Rectangle(10, 75, 30, 25), HookGuideOverlay.shownPart(new Rectangle(10, 75, 30, 30), view));
		// Exactly two thirds still counts; less does not.
		assertEquals(new Rectangle(10, 0, 30, 20), HookGuideOverlay.shownPart(new Rectangle(10, -10, 30, 30), view));
		assertNull(HookGuideOverlay.shownPart(new Rectangle(10, 81, 30, 30), view));
		assertNull(HookGuideOverlay.shownPart(new Rectangle(10, 150, 30, 30), view));
		assertEquals(inside, HookGuideOverlay.shownPart(inside, null));
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
