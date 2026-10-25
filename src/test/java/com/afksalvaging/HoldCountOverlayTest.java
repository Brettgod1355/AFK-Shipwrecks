/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

import java.awt.Rectangle;
import net.runelite.api.Point;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class HoldCountOverlayTest
{
	@Test
	public void theCountSitsInTheMiddleOfTheHold()
	{
		// Owner, 2026-10-25: the count sat above the hold; now in its middle as drawn on screen.
		Point at = HoldCountOverlay.textAt(new Rectangle(100, 200, 60, 40), 30, 12, 2);
		assertEquals("half the text left of the middle", 115, at.getX());
		assertEquals("baseline puts the letters' middle on the box's middle", 225, at.getY());
	}

	@Test
	public void theCountIsColoredByTheChosenStyle()
	{
		java.awt.Color empty = java.awt.Color.GREEN, nearly = java.awt.Color.ORANGE, full = java.awt.Color.RED;
		// Fade: three stops, empty, nearly full at half, full.
		assertEquals(empty, HoldCountOverlay.color(HoldColorStyle.FADE, 0, 160, 75, 100, empty, nearly, full));
		assertEquals(nearly, HoldCountOverlay.color(HoldColorStyle.FADE, 80, 160, 75, 100, empty, nearly, full));
		assertEquals(full, HoldCountOverlay.color(HoldColorStyle.FADE, 160, 160, 75, 100, empty, nearly, full));
		// Steps: by how full this hold is, 75% and 100% by default, on any hold size.
		assertEquals(empty, HoldCountOverlay.color(HoldColorStyle.STEPS, 119, 160, 75, 100, empty, nearly, full));
		assertEquals(nearly, HoldCountOverlay.color(HoldColorStyle.STEPS, 120, 160, 75, 100, empty, nearly, full));
		assertEquals(nearly, HoldCountOverlay.color(HoldColorStyle.STEPS, 159, 160, 75, 100, empty, nearly, full));
		assertEquals(full, HoldCountOverlay.color(HoldColorStyle.STEPS, 160, 160, 75, 100, empty, nearly, full));
		assertEquals(empty, HoldCountOverlay.color(HoldColorStyle.STEPS, 14, 20, 75, 100, empty, nearly, full));
		assertEquals(nearly, HoldCountOverlay.color(HoldColorStyle.STEPS, 15, 20, 75, 100, empty, nearly, full));
		assertEquals("own percentages", full, HoldCountOverlay.color(HoldColorStyle.STEPS, 216, 240, 50, 90, empty, nearly, full));
		assertEquals("set the wrong way round: full wins", full,
			HoldCountOverlay.color(HoldColorStyle.STEPS, 150, 160, 95, 90, empty, nearly, full));
		assertEquals("size not known yet", empty, HoldCountOverlay.color(HoldColorStyle.STEPS, 50, -1, 75, 100, empty, nearly, full));
		// One color: always the empty color.
		assertEquals(empty, HoldCountOverlay.color(HoldColorStyle.ONE_COLOR, 160, 160, 75, 100, empty, nearly, full));
	}
}
