/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SmoothedCountdownTest
{
	@Test
	public void aHeldFigureDoesNotLoseTheHoldWhenItRunsAgain()
	{
		SmoothedCountdown countdown = new SmoothedCountdown();
		countdown.update(12 * 60_000L, true, 0);
		// Held for two and a half minutes, as during a stall.
		for (long t = 600; t <= 150_000; t += 600)
		{
			assertEquals(12 * 60_000L, countdown.hold(t));
		}
		// Running again with the same estimate: one tick off, not the whole stall.
		long shown = countdown.update(12 * 60_000L, true, 150_600);
		assertEquals(12 * 60_000L - 600 + 150, shown, 200);
	}

	private final SmoothedCountdown countdown = new SmoothedCountdown();

	@Test
	public void takesTheFirstEstimateAsIs()
	{
		assertEquals(600_000, countdown.update(600_000, true, 0));
		assertEquals(600_000, countdown.remaining());
	}

	@Test
	public void runsDownInRealTimeWhenTheEstimateHoldsSteady()
	{
		countdown.update(600_000, true, 0);
		assertEquals(599_400, countdown.update(599_400, true, 600));
		assertEquals(598_800, countdown.remainingAt(1_200, true));
		assertEquals(599_400, countdown.remainingAt(1_200, false));
	}

	@Test
	public void glidesTowardsANewEstimate()
	{
		countdown.update(600_000, true, 0);
		// The estimate jumps to 700 s; the display closes a quarter of the gap each tick.
		long shown = countdown.update(700_000, true, 600);
		assertEquals(599_400 + Math.round(100_600 * 0.25), shown);
		long next = countdown.update(699_400, true, 1_200);
		assertTrue(next > shown && next < 699_400);
		long after = shown;
		for (int i = 2; i < 40; i++)
		{
			after = countdown.update(700_000 - i * 600L, true, i * 600L);
		}
		assertEquals(700_000 - 39 * 600L, after, 200);
	}

	@Test
	public void holdsStillWhilePaused()
	{
		countdown.update(600_000, true, 0);
		assertEquals(600_000, countdown.update(600_000, false, 10_000));
		assertEquals(600_000, countdown.update(600_000, false, 20_000));
	}

	@Test
	public void snapsWhenTheEstimateChangesByALot()
	{
		countdown.update(600_000, true, 0);
		assertEquals(3_600_000, countdown.update(3_600_000, true, 600));
		assertEquals(60_000, countdown.update(60_000, true, 1_200));
	}

	@Test
	public void forgetsWhenThereIsNoEstimate()
	{
		countdown.update(600_000, true, 0);
		assertEquals(-1, countdown.update(-1, true, 600));
		assertEquals(-1, countdown.remainingAt(5_000, true));
		assertEquals(120_000, countdown.update(120_000, true, 1_200));
		countdown.reset();
		assertEquals(-1, countdown.remaining());
	}

	@Test
	public void neverGoesBelowZero()
	{
		countdown.update(1_000, true, 0);
		assertEquals(0, countdown.update(0, true, 5_000));
		assertEquals(0, countdown.remainingAt(10_000, true));
	}
}
