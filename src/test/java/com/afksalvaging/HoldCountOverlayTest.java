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
		// Fade: the look before 2.3.2, empty to full as the hold fills.
		assertEquals(empty, HoldCountOverlay.color(HoldColorStyle.FADE, 0, 160, 5, empty, nearly, full));
		assertEquals(full, HoldCountOverlay.color(HoldColorStyle.FADE, 160, 160, 5, empty, nearly, full));
		// Steps: empty, then nearly full at "Warn with slots left", then full.
		assertEquals(empty, HoldCountOverlay.color(HoldColorStyle.STEPS, 154, 160, 5, empty, nearly, full));
		assertEquals(nearly, HoldCountOverlay.color(HoldColorStyle.STEPS, 155, 160, 5, empty, nearly, full));
		assertEquals(full, HoldCountOverlay.color(HoldColorStyle.STEPS, 160, 160, 5, empty, nearly, full));
		assertEquals("no warning set: no nearly-full step", empty, HoldCountOverlay.color(HoldColorStyle.STEPS, 159, 160, 0, empty, nearly, full));
		// One color: always the empty color.
		assertEquals(empty, HoldCountOverlay.color(HoldColorStyle.ONE_COLOR, 160, 160, 5, empty, nearly, full));
	}

	@Test
	public void theCountReadsLikeTheOverlayUnlessItHasItsOwnFormat()
	{
		assertEquals(CounterStyle.SLOTS_LEFT, HoldCountFormat.SAME_AS_OVERLAY.style(CounterStyle.SLOTS_LEFT));
		assertEquals(CounterStyle.PERCENT_FULL, HoldCountFormat.PERCENT_FULL.style(CounterStyle.SLOTS_LEFT));
	}
}
