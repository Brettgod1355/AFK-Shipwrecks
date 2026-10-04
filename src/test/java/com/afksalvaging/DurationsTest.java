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
	public void tinyIsForAnInfobox()
	{
		assertEquals("?", Durations.tiny(-1));
		assertEquals("4:31", Durations.tiny(271_000));
		assertEquals("52m", Durations.tiny(52 * 60_000L));
		assertEquals("1h30", Durations.tiny(90 * 60_000L));
		assertEquals("2h", Durations.tiny(120 * 60_000L));
	}

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
	public void coarseFormIsSecondsWhenCloseAndMinutesWhenNot()
	{
		assertEquals("9:00", Durations.coarse(9 * 60_000L));
		assertEquals("9:59", Durations.coarse(Durations.FINE_MILLIS - 1_000));
		assertEquals("10 min", Durations.coarse(Durations.FINE_MILLIS));
		assertEquals("52 min", Durations.coarse(52 * 60_000L + 10_000));
		assertEquals("53 min", Durations.coarse(52 * 60_000L + 30_000));
		assertEquals("1 h", Durations.coarse(3_600_000));
		assertEquals("1 h 30 min", Durations.coarse(90 * 60_000L));
		assertEquals("?", Durations.coarse(-1));
	}

	@Test
	public void clockTimeIsWhenTheCountdownEnds()
	{
		long noon = 12 * 3_600_000L;
		assertEquals("12:42", Durations.clockAfter(noon, 42 * 60_000L, ZoneOffset.UTC));
		assertEquals("13:00", Durations.clockAfter(noon, 3_600_000, ZoneOffset.UTC));
		assertEquals("12:00", Durations.clockAfter(noon, -1, ZoneOffset.UTC));
	}

	@Test
	public void clockTimeCanBeTwelveHour()
	{
		long midnight = 0;
		long noon = 12 * 3_600_000L;
		assertEquals("6:25 PM", Durations.clockAfter(noon, (6 * 60 + 25) * 60_000L, ZoneOffset.UTC, true));
		assertEquals("18:25", Durations.clockAfter(noon, (6 * 60 + 25) * 60_000L, ZoneOffset.UTC, false));
		assertEquals("12:00 PM", Durations.clockAfter(noon, 0, ZoneOffset.UTC, true));
		assertEquals("12:30 AM", Durations.clockAfter(midnight, 30 * 60_000L, ZoneOffset.UTC, true));
		assertEquals("9:05 AM", Durations.clockAfter(midnight, (9 * 60 + 5) * 60_000L, ZoneOffset.UTC, true));
	}
}
