/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

import java.awt.image.BufferedImage;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class HoldInfoBoxTest
{
	@Test
	public void theLabelSitsInTheTopHalfAndLeavesTheBottomToTheTime()
	{
		for (int size : new int[] {35, 32, 28, 50})
		{
			BufferedImage picture = HoldInfoBox.label(size);
			assertEquals(size, picture.getWidth());
			assertEquals(size, picture.getHeight());
			boolean inkAbove = false;
			boolean inkBelow = false;
			boolean inkOnEdge = false;
			for (int y = 0; y < picture.getHeight(); y++)
			{
				for (int x = 0; x < picture.getWidth(); x++)
				{
					if ((picture.getRGB(x, y) >>> 24) != 0)
					{
						if (y < picture.getHeight() / 2)
						{
							inkAbove = true;
						}
						else
						{
							inkBelow = true;
						}
						if (x == 0 || x == picture.getWidth() - 1 || y == 0)
						{
							inkOnEdge = true;
						}
					}
				}
			}
			assertTrue("AFK is drawn at " + size, inkAbove);
			assertFalse("the time's half is clear at " + size, inkBelow);
			assertFalse("nothing touches the edge at " + size, inkOnEdge);
		}
		BufferedImage tiny = HoldInfoBox.label(12);
		assertEquals(12, tiny.getWidth());
		assertEquals("too small for the word: blank", 0, tiny.getRGB(6, 3) >>> 24);
	}

	@Test
	public void nothingIsDrawnOffTheBoatOrWithoutAHook()
	{
		AfkSession.View view = new AfkSession.View();
		view.estimate = AfkEstimate.of(AfkEstimate.State.NOT_SAILING);
		assertNull(HoldInfoBox.text(view, CargoHoldMonitor.Level.OK));
		view.estimate = AfkEstimate.of(AfkEstimate.State.NO_HOOK);
		assertNull(HoldInfoBox.text(view, CargoHoldMonitor.Level.OK));
	}

	@Test
	public void theCountdownIsTinyAndFullWinsOverEverything()
	{
		AfkSession.View view = new AfkSession.View();
		view.estimate = AfkEstimate.of(AfkEstimate.State.COUNTING_DOWN);
		view.countdownMillis = 52 * 60_000L;
		assertEquals("52m", HoldInfoBox.text(view, CargoHoldMonitor.Level.OK));
		assertTrue(HoldInfoBox.tooltip(view, CargoHoldMonitor.Level.OK).contains("52 min"));
		assertEquals("Full", HoldInfoBox.text(view, CargoHoldMonitor.Level.FULL));
		view.estimate = AfkEstimate.of(AfkEstimate.State.WAITING_FOR_WRECK);
		assertNull("on the way to a spot the box says nothing", HoldInfoBox.text(view, CargoHoldMonitor.Level.OK));
		view.atSpot = true;
		assertEquals("Wait", HoldInfoBox.text(view, CargoHoldMonitor.Level.OK));
		view.estimate = AfkEstimate.of(AfkEstimate.State.HOLD_UNKNOWN);
		assertEquals("?", HoldInfoBox.text(view, CargoHoldMonitor.Level.OK));
	}
}
