/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CounterStyleTest
{
	@Test
	public void usedOfTotalIsTheOriginalCounter()
	{
		assertEquals("154/160", CounterStyle.USED_OF_TOTAL.format(154, 160));
		assertEquals("0/20", CounterStyle.USED_OF_TOTAL.format(0, 20));
	}

	@Test
	public void slotsLeftNeverGoesNegative()
	{
		assertEquals("6 left", CounterStyle.SLOTS_LEFT.format(154, 160));
		assertEquals("0 left", CounterStyle.SLOTS_LEFT.format(160, 160));
		assertEquals("0 left", CounterStyle.SLOTS_LEFT.format(45, 40));
	}

	@Test
	public void percentOnlyReadsOneHundredWhenFull()
	{
		assertEquals("0%", CounterStyle.PERCENT_FULL.format(0, 160));
		assertEquals("96%", CounterStyle.PERCENT_FULL.format(154, 160));
		assertEquals("99%", CounterStyle.PERCENT_FULL.format(239, 240));
		assertEquals("100%", CounterStyle.PERCENT_FULL.format(160, 160));
		assertEquals("100%", CounterStyle.PERCENT_FULL.format(45, 40));
	}

	@Test
	public void percentWithoutACapacityIsZero()
	{
		assertEquals(0, CounterStyle.percentFull(5, 0));
		assertEquals(0, CounterStyle.percentFull(5, CargoHoldCapacity.UNKNOWN));
	}
}
