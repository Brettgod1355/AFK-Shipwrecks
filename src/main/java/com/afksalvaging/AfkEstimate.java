/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

/** What the AFK timer should show right now. */
public final class AfkEstimate
{
	public enum State
	{
		/** Not on a boat: show nothing. */
		NOT_SAILING,
		/** The boat has no salvaging hook. */
		NO_HOOK,
		/** The hold's count or size is not known yet; opening the hold fixes that. */
		HOLD_UNKNOWN,
		/** The hold is full, or the game says so. */
		HOLD_FULL,
		/** The running tally says the hold is full but nothing has confirmed it. */
		HOLD_FULL_UNCONFIRMED,
		/** No crewmate on a hook and the player is not at one. */
		NOBODY_SALVAGING,
		/** The game refused to salvage here. */
		HAZARDOUS,
		/** A wreck is in reach but the player's Sailing level is too low for it. */
		LEVEL_TOO_LOW,
		/** People are on hooks but no wreck they can salvage is in reach. */
		WAITING_FOR_WRECK,
		/** Far more salvage was expected than has arrived; the crew are probably not salvaging. */
		STALLED,
		/** Counting down to a full hold. */
		COUNTING_DOWN,
		/** Only the player is salvaging and their inventory fills before the hold would. */
		INVENTORY_FILLS_FIRST,
		/** Only the player is salvaging and the wreck sinks before either fills. */
		WRECK_SINKS_FIRST
	}

	private final State state;
	private final long etaMillis;
	private final long workMillis;
	private final boolean approximate;
	private final double holdPerHour;

	AfkEstimate(State state, long etaMillis, long workMillis, boolean approximate, double holdPerHour)
	{
		this.state = state;
		this.etaMillis = etaMillis;
		this.workMillis = workMillis;
		this.approximate = approximate;
		this.holdPerHour = holdPerHour;
	}

	static AfkEstimate of(State state)
	{
		return new AfkEstimate(state, -1, -1, false, 0);
	}

	static AfkEstimate of(State state, long etaMillis, long workMillis, boolean approximate)
	{
		return new AfkEstimate(state, etaMillis, workMillis, approximate, 0);
	}

	public State getState()
	{
		return state;
	}

	/** Milliseconds until the state's deadline, or -1 when there is none. */
	public long getEtaMillis()
	{
		return etaMillis;
	}

	public boolean hasEta()
	{
		return etaMillis >= 0;
	}

	/**
	 * Salvaging time needed to fill the hold with everyone who is on a hook working at full rate,
	 * ignoring any wreck downtime, or -1 when it cannot be worked out. In the paused states this
	 * is what is left once a wreck is up again.
	 */
	public long getWorkMillis()
	{
		return workMillis;
	}

	public boolean hasWork()
	{
		return workMillis >= 0;
	}

	/** Whether the rate behind the countdown still rests mostly on the published tables. */
	public boolean isApproximate()
	{
		return approximate;
	}

	/** Expected slots added to the hold per hour while a wreck is up, at the current rate. */
	public double getHoldPerHour()
	{
		return holdPerHour;
	}

	@Override
	public String toString()
	{
		return state + (etaMillis >= 0 ? " " + Durations.countdown(etaMillis) : "") + (approximate ? " ~" : "");
	}
}
