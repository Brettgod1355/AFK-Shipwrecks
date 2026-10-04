/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DecayingSumTest
{
	private static final long TAU = 10_000;

	@Test
	public void fadesToAboutAThirdAfterOneTimeConstant()
	{
		DecayingSum sum = new DecayingSum(TAU);
		sum.add(100, 0);
		sum.decayTo(TAU);
		assertEquals(100 * Math.exp(-1), sum.value(), 1e-9);
		sum.decayTo(3 * TAU);
		assertEquals(100 * Math.exp(-3), sum.value(), 1e-9);
	}

	@Test
	public void additionsAreFadedFromWhenTheyHappened()
	{
		DecayingSum sum = new DecayingSum(TAU);
		sum.add(10, 0);
		sum.add(10, TAU);
		assertEquals(10 + 10 * Math.exp(-1), sum.value(), 1e-9);
	}

	@Test
	public void timeNeverRunsBackwards()
	{
		DecayingSum sum = new DecayingSum(TAU);
		sum.add(10, 5_000);
		sum.decayTo(1_000);
		assertEquals(10, sum.value(), 1e-9);
		sum.add(5, 1_000);
		assertEquals(15, sum.value(), 1e-9);
		sum.decayTo(5_000);
		assertEquals(15, sum.value(), 1e-9);
	}

	@Test
	public void setAndResetReplaceHistory()
	{
		DecayingSum sum = new DecayingSum(TAU);
		sum.add(10, 0);
		sum.set(3, 1_000);
		assertEquals(3, sum.value(), 1e-9);
		sum.decayTo(1_000 + TAU);
		assertEquals(3 * Math.exp(-1), sum.value(), 1e-9);
		sum.reset();
		assertEquals(0, sum.value(), 1e-9);
		sum.add(1, 0);
		assertEquals(1, sum.value(), 1e-9);
	}

	@Test(expected = IllegalArgumentException.class)
	public void rejectsANonPositiveTimeConstant()
	{
		new DecayingSum(0);
	}
}
