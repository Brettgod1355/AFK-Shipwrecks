/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

/**
 * A running total that fades out exponentially, so recent history counts for more than old history.
 * <p>
 * With a time constant of {@code tau}, anything added {@code tau} ago is worth about 37% of what it
 * was, and anything added {@code 3 tau} ago about 5%. Times are milliseconds from one clock.
 */
public final class DecayingSum
{
	private final double tauMillis;
	private double value;
	private long lastAt = Long.MIN_VALUE;

	public DecayingSum(long tauMillis)
	{
		if (tauMillis <= 0)
		{
			throw new IllegalArgumentException("tau must be positive");
		}
		this.tauMillis = tauMillis;
	}

	/** Brings the total up to date with the time that has passed. Time never runs backwards here. */
	public void decayTo(long now)
	{
		if (lastAt != Long.MIN_VALUE && now > lastAt)
		{
			value *= Math.exp(-(now - lastAt) / tauMillis);
		}
		if (lastAt == Long.MIN_VALUE || now > lastAt)
		{
			lastAt = now;
		}
	}

	public void add(double amount, long now)
	{
		decayTo(now);
		value += amount;
	}

	/** The total as of the last update; call {@link #decayTo} first for the current figure. */
	public double value()
	{
		return value;
	}

	/** Replaces the total outright, for example when restoring remembered history. */
	public void set(double amount, long now)
	{
		value = amount;
		lastAt = now;
	}

	public void reset()
	{
		value = 0;
		lastAt = Long.MIN_VALUE;
	}
}
