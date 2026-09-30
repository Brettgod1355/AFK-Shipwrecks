/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

import java.time.ZoneOffset;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DurationsTest
{
	@Test
	public void countdownRoundsUpAndSwitchesToHours()
	{
		assertEquals("0:00", Durations.countdown(0));
		assertEquals("0:00", Durations.countdown(-5_000));
		assertEquals("0:01", Durations.countdown(1));
		assertEquals("0:59", Durations.countdown(59_000));
		assertEquals("1:00", Durations.countdown(59_001));
		assertEquals("42:10", Durations.countdown(42 * 60_000L + 10_000));
		assertEquals("1:00:00", Durations.countdown(3_600_000));
		assertEquals("2:05:09", Durations.countdown(2 * 3_600_000L + 5 * 60_000L + 8_001));
	}

	@Test
	public void spokenFormRoundsToMinutes()
	{
		assertEquals("under a minute", Durations.spoken(20_000));
		assertEquals("about 1 minute", Durations.spoken(60_000));
		assertEquals("about 42 minutes", Durations.spoken(42 * 60_000L + 10_000));
		assertEquals("about 1 hour", Durations.spoken(3_600_000));
		assertEquals("about 1 hour 5 minutes", Durations.spoken(65 * 60_000L));
		assertEquals("about 2 hours 1 minute", Durations.spoken(121 * 60_000L));
	}

	@Test
	public void clockTimeIsWhenTheCountdownEnds()
	{
		long noon = 12 * 3_600_000L;
		assertEquals("12:42", Durations.clockAfter(noon, 42 * 60_000L, ZoneOffset.UTC));
		assertEquals("13:00", Durations.clockAfter(noon, 3_600_000, ZoneOffset.UTC));
		assertEquals("12:00", Durations.clockAfter(noon, -1, ZoneOffset.UTC));
	}
}
