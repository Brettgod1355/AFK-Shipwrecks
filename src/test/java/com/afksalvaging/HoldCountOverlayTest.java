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
}
