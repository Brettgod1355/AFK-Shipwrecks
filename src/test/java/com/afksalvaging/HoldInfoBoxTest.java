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
		BufferedImage picture = HoldInfoBox.label();
		assertEquals(HoldInfoBox.PICTURE_SIZE, picture.getWidth());
		assertEquals(HoldInfoBox.PICTURE_SIZE, picture.getHeight());
		boolean inkAbove = false;
		boolean inkBelow = false;
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
				}
			}
		}
		assertTrue("AFK is drawn", inkAbove);
		assertFalse("the time's half is clear", inkBelow);
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
		assertEquals("Wait", HoldInfoBox.text(view, CargoHoldMonitor.Level.OK));
		view.estimate = AfkEstimate.of(AfkEstimate.State.HOLD_UNKNOWN);
		assertEquals("?", HoldInfoBox.text(view, CargoHoldMonitor.Level.OK));
	}
}
