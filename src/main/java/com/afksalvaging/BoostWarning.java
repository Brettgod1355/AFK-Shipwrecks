/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

/**
 * When to warn about a Sailing boost wearing off below the wreck's level (owner, 2026-10-08: "option
 * for before, after and both"). Before: as the boost reaches the last level the wreck allows. After:
 * once the crew have stopped, which is what 2.0 did.
 */
public enum BoostWarning
{
	BEFORE("Before it ends", true, false),
	AFTER("After crew stop", false, true),
	BOTH("Both", true, true);

	private final String label;
	private final boolean before;
	private final boolean after;

	BoostWarning(String label, boolean before, boolean after)
	{
		this.label = label;
		this.before = before;
		this.after = after;
	}

	public boolean before()
	{
		return before;
	}

	public boolean after()
	{
		return after;
	}

	@Override
	public String toString()
	{
		return label;
	}
}
