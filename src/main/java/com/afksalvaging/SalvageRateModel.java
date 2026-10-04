/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

import java.util.Locale;

/**
 * Learns how the real salvage rate compares with the published one.
 * <p>
 * Every game tick the plugin works out how much salvage the published tables say the people on the
 * hooks should produce, and feeds that in as "expected". Salvage that actually turns up is fed in as
 * "observed". The ratio of the two is a correction factor: 1.0 means the tables are spot on, 0.8
 * means the crew are producing a fifth less than published. The factor starts at a prior worth
 * {@link #PRIOR_WEIGHT} expected salvage, so there is an estimate from the first tick, and it fades
 * old evidence with a 30 minute time constant measured in ticks spent actually salvaging, so a long
 * break does not erase what was learned. The factor can be remembered between sessions.
 */
public final class SalvageRateModel
{
	/** Ticks between the player's rolls. */
	public static final int PLAYER_ROLL_TICKS = 4;
	/** Ticks between a crewmate's rolls. */
	public static final int CREW_ROLL_TICKS = 5;
	/** Deckhandiness assumed for a crewmate whose stats could not be read. */
	public static final int DEFAULT_DECKHANDINESS = 2;
	/** Salvaging ticks over which evidence fades to about a third: thirty minutes. */
	public static final long DEFAULT_TAU_TICKS = 3000;
	/** The prior counts as this much expected salvage: about ten minutes of one good crewmate. */
	public static final double PRIOR_WEIGHT = 25;
	/** Observed salvage before the estimate is called confident rather than approximate. */
	public static final int CONFIDENT_EVENTS = 25;
	/**
	 * Expected salvage without any arriving before the occupants are presumed not to be salvaging.
	 * Ten keeps a run of plain bad luck (about one in twenty thousand gaps) from tripping it.
	 */
	public static final double STALL_EXPECTED = 10;
	private static final double MIN_CORRECTION = 0.1;
	private static final double MAX_CORRECTION = 4.0;

	private final DecayingSum observed;
	private final DecayingSum expected;
	private long activeTicks;
	private long lifetimeEvents;
	private double prior = 1.0;
	private double expectedSinceLastEvent;

	public SalvageRateModel()
	{
		this(DEFAULT_TAU_TICKS);
	}

	public SalvageRateModel(long tauTicks)
	{
		observed = new DecayingSum(tauTicks);
		expected = new DecayingSum(tauTicks);
	}

	/**
	 * Records one tick in which the occupants were expected to produce this much salvage between them.
	 * Ticks with nothing expected (nobody rolling) do not count as salvaging time.
	 */
	public void addExpected(double expectedThisTick)
	{
		if (!(expectedThisTick > 0))
		{
			return;
		}
		activeTicks++;
		observed.decayTo(activeTicks);
		expected.add(expectedThisTick, activeTicks);
		expectedSinceLastEvent += expectedThisTick;
	}

	/** Records one piece of salvage arriving, from anyone on a hook. */
	public void recordSalvage()
	{
		observed.add(1, activeTicks);
		expected.decayTo(activeTicks);
		lifetimeEvents++;
		expectedSinceLastEvent = 0;
	}

	/** Observed divided by expected, shrunk towards the prior, kept within sane bounds. */
	public double correction()
	{
		double k = (observed.value() + PRIOR_WEIGHT * prior) / (expected.value() + PRIOR_WEIGHT);
		return Math.max(MIN_CORRECTION, Math.min(MAX_CORRECTION, k));
	}

	/** Whether enough salvage has been seen for the estimate to be more than the published tables. */
	public boolean isConfident()
	{
		return lifetimeEvents >= CONFIDENT_EVENTS;
	}

	/**
	 * Whether far more salvage was expected than has arrived since the last piece, which means the
	 * people counted as salvaging are probably not (out of range, level dropped, hazardous water).
	 */
	public boolean isStalled()
	{
		return expectedSinceLastEvent >= STALL_EXPECTED;
	}

	public double expectedSinceLastEvent()
	{
		return expectedSinceLastEvent;
	}

	public long lifetimeEvents()
	{
		return lifetimeEvents;
	}

	public double observed()
	{
		return observed.value();
	}

	public double expected()
	{
		return expected.value();
	}

	/** The correction to remember, as text, or null when nothing has been learned. */
	public String encodeMemory()
	{
		if (lifetimeEvents == 0)
		{
			return null;
		}
		return String.format(Locale.ROOT, "%.4f", correction());
	}

	/**
	 * Starts from a remembered correction, forgetting current evidence.
	 *
	 * @return false when the text is not a usable number, in which case the prior is 1.0
	 */
	public boolean restoreMemory(String memory)
	{
		reset();
		if (memory == null)
		{
			return false;
		}
		try
		{
			double k = Double.parseDouble(memory.trim());
			if (Double.isNaN(k) || Double.isInfinite(k) || k <= 0)
			{
				return false;
			}
			prior = Math.max(MIN_CORRECTION, Math.min(MAX_CORRECTION, k));
			return true;
		}
		catch (NumberFormatException e)
		{
			return false;
		}
	}

	/** Forgets all evidence and returns to a prior of 1.0. */
	public void reset()
	{
		observed.reset();
		expected.reset();
		activeTicks = 0;
		lifetimeEvents = 0;
		prior = 1.0;
		expectedSinceLastEvent = 0;
	}
}
