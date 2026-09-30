/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class SalvageRateModelTest
{
	/** A good crewmate on a good wreck: about one salvage every 15 seconds, so 0.04 per tick. */
	private static final double PER_TICK = 0.04;

	private static final long NO_FADE = 100_000_000L;

	private final SalvageRateModel model = new SalvageRateModel();
	private final SalvageRateModel steady = new SalvageRateModel(NO_FADE);

	@Test
	public void startsFromThePublishedTables()
	{
		assertEquals(1.0, model.correction(), 1e-12);
		assertFalse(model.isConfident());
		assertFalse(model.isStalled());
		assertNull(model.encodeMemory());
	}

	@Test
	public void learnsThatTheCrewAreSlowerThanPublished()
	{
		// Published: 1 per 25 ticks. Actual: 1 per 50 ticks, i.e. half.
		for (int tick = 1; tick <= 5000; tick++)
		{
			steady.addExpected(PER_TICK);
			if (tick % 50 == 0)
			{
				steady.recordSalvage();
			}
		}
		// 200 expected, 100 observed, prior 25 at 1.0: (100 + 25) / (200 + 25) = 0.556.
		assertEquals(0.556, steady.correction(), 0.002);
		assertTrue(steady.isConfident());
		assertEquals(100, steady.lifetimeEvents());
		// With fading, the last half hour weighs most but the answer is the same kind of number.
		for (int tick = 1; tick <= 5000; tick++)
		{
			model.addExpected(PER_TICK);
			if (tick % 50 == 0)
			{
				model.recordSalvage();
			}
		}
		assertEquals(0.6, model.correction(), 0.05);
	}

	@Test
	public void thePriorGivesWayGraduallyToEvidence()
	{
		// Published 1 per 25 ticks; actual matches published exactly.
		for (int tick = 1; tick <= 250; tick++)
		{
			steady.addExpected(PER_TICK);
			if (tick % 25 == 0)
			{
				steady.recordSalvage();
			}
		}
		assertEquals(1.0, steady.correction(), 1e-6);
		assertFalse(steady.isConfident());
		// Twice as fast as published for the same stretch: (20 + 25) / (10 + 25) = 1.29, not 2.
		SalvageRateModel fast = new SalvageRateModel(NO_FADE);
		for (int tick = 1; tick <= 250; tick++)
		{
			fast.addExpected(PER_TICK);
			if (tick % 12 == 0)
			{
				fast.recordSalvage();
			}
		}
		assertEquals(1.29, fast.correction(), 0.03);
	}

	@Test
	public void ticksWithNobodyRollingDoNotAgeTheEvidence()
	{
		for (int tick = 1; tick <= 2500; tick++)
		{
			model.addExpected(PER_TICK);
			if (tick % 50 == 0)
			{
				model.recordSalvage();
			}
		}
		double before = model.correction();
		for (int tick = 0; tick < 100_000; tick++)
		{
			model.addExpected(0);
		}
		assertEquals(before, model.correction(), 1e-12);
		assertEquals(0, model.expectedSinceLastEvent(), 1e-12);
	}

	@Test
	public void oldEvidenceFadesOverHalfAnHourOfSalvaging()
	{
		for (int tick = 1; tick <= 3000; tick++)
		{
			model.addExpected(PER_TICK);
			if (tick % 50 == 0)
			{
				model.recordSalvage();
			}
		}
		assertTrue(model.correction() < 0.65);
		// Now the crew produce exactly as published for an hour and a half.
		for (int tick = 1; tick <= 9000; tick++)
		{
			model.addExpected(PER_TICK);
			if (tick % 25 == 0)
			{
				model.recordSalvage();
			}
		}
		assertEquals(1.0, model.correction(), 0.03);
	}

	@Test
	public void stallsWhenFarMoreWasExpectedThanArrived()
	{
		for (int tick = 1; tick <= 149; tick++)
		{
			model.addExpected(PER_TICK);
		}
		assertFalse(model.isStalled());
		model.addExpected(PER_TICK);
		assertTrue(model.isStalled());
		model.recordSalvage();
		assertFalse(model.isStalled());
	}

	@Test
	public void theCorrectionStaysWithinReason()
	{
		for (int i = 0; i < 1000; i++)
		{
			model.recordSalvage();
		}
		assertEquals(4.0, model.correction(), 1e-12);
		SalvageRateModel slow = new SalvageRateModel();
		for (int tick = 0; tick < 100_000; tick++)
		{
			slow.addExpected(1);
		}
		assertEquals(0.1, slow.correction(), 1e-12);
	}

	@Test
	public void memoryRoundTrips()
	{
		for (int tick = 1; tick <= 5000; tick++)
		{
			steady.addExpected(PER_TICK);
			if (tick % 50 == 0)
			{
				steady.recordSalvage();
			}
		}
		String memory = steady.encodeMemory();
		assertEquals("0.5556", memory);

		SalvageRateModel restored = new SalvageRateModel();
		assertTrue(restored.restoreMemory(memory));
		assertEquals(0.5556, restored.correction(), 1e-9);
		assertFalse(restored.isConfident());
		assertEquals(0, restored.lifetimeEvents());
	}

	@Test
	public void memoryIgnoresRubbish()
	{
		assertFalse(model.restoreMemory(null));
		assertFalse(model.restoreMemory(""));
		assertFalse(model.restoreMemory("abc"));
		assertFalse(model.restoreMemory("-1"));
		assertFalse(model.restoreMemory("0"));
		assertFalse(model.restoreMemory("NaN"));
		assertEquals(1.0, model.correction(), 1e-12);
		assertTrue(model.restoreMemory("9"));
		assertEquals(4.0, model.correction(), 1e-12);
	}

	@Test
	public void resetForgetsEverything()
	{
		model.restoreMemory("0.5");
		model.addExpected(1);
		model.recordSalvage();
		model.reset();
		assertEquals(1.0, model.correction(), 1e-12);
		assertEquals(0, model.lifetimeEvents());
		assertEquals(0, model.observed(), 1e-12);
		assertEquals(0, model.expected(), 1e-12);
	}
}
