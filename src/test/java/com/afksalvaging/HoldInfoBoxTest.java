/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class HoldInfoBoxTest
{
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
