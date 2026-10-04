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
	public void theLabelNeverTouchesTheTimeAtAnySizeItIsDrawnAt()
	{
		// RuneLite writes the time in its small font with the baseline 3 px above the bottom; its tallest glyph reaches about 13 px up.
		for (int size = HoldInfoBox.SMALLEST_LABELLED; size <= 64; size++)
		{
			BufferedImage picture = HoldInfoBox.label(size);
			assertEquals(size, picture.getWidth());
			assertEquals(size, picture.getHeight());
			int timeTop = size - 13;
			boolean ink = false;
			for (int y = 0; y < size; y++)
			{
				for (int x = 0; x < size; x++)
				{
					if ((picture.getRGB(x, y) >>> 24) != 0)
					{
						ink = true;
						assertTrue("clear of the time at size " + size + ", row " + y, y < timeTop);
						assertTrue("clear of the edge at size " + size, x > 0 && x < size - 1 && y > 0);
					}
				}
			}
			assertTrue("AFK is drawn at " + size, ink);
		}
		for (int size : new int[] {12, HoldInfoBox.SMALLEST_LABELLED - 1})
		{
			BufferedImage blank = HoldInfoBox.label(size);
			assertEquals(size, blank.getWidth());
			for (int y = 0; y < size; y++)
			{
				for (int x = 0; x < size; x++)
				{
					assertEquals("too small for the word: blank at " + size, 0, blank.getRGB(x, y) >>> 24);
				}
			}
		}
	}

	@Test
	public void everyStateHasItsOwnWordAndANumberIsAlwaysATime()
	{
		AfkSession.View view = new AfkSession.View();
		view.countdownMillis = 3 * 60_000L + 40_000L;
		view.estimate = AfkEstimate.of(AfkEstimate.State.INVENTORY_FILLS_FIRST);
		assertEquals("3:40", HoldInfoBox.text(view, CargoHoldMonitor.Level.OK));
		String tip = HoldInfoBox.tooltip(view, CargoHoldMonitor.Level.OK);
		assertTrue(tip, tip.contains("inventory is full in"));
		assertFalse("not the hold", tip.contains("hold full in"));
		view.estimate = AfkEstimate.of(AfkEstimate.State.WRECK_SINKS_FIRST);
		tip = HoldInfoBox.tooltip(view, CargoHoldMonitor.Level.OK);
		assertTrue(tip, tip.contains("wreck sinks"));
		assertFalse(tip.contains("hold full in"));
		String[][] words = {
			{"PLAYER_HOOK_IDLE", "Hook", "click it"},
			{"NOBODY_SALVAGING", "Idle", "no one is on a hook"},
			{"INVENTORY_FULL", "Inv", "deposit"},
			{"LEVEL_TOO_LOW", "Lvl", "level is too low"},
			{"HAZARDOUS", "Stop", "not safe"},
			{"HOLD_DRIFTED", "?", "resync"},
			{"HOLD_UNKNOWN", "?", "open the cargo hold"},
		};
		for (String[] w : words)
		{
			view.estimate = AfkEstimate.of(AfkEstimate.State.valueOf(w[0]));
			assertEquals(w[0], w[1], HoldInfoBox.text(view, CargoHoldMonitor.Level.OK));
			tip = HoldInfoBox.tooltip(view, CargoHoldMonitor.Level.OK);
			assertTrue(w[0] + ": " + tip, tip.contains(w[2]));
		}
		// No state's tooltip is an internal name.
		for (AfkEstimate.State state : AfkEstimate.State.values())
		{
			view.estimate = AfkEstimate.of(state);
			String t = HoldInfoBox.tooltip(view, CargoHoldMonitor.Level.OK);
			assertFalse(state + ": " + t, t.contains(state.name().toLowerCase().replace('_', ' ')) && state.name().contains("_"));
		}
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
