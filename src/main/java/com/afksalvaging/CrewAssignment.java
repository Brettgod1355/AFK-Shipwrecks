/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

/**
 * The job a crewmate has been given, as the crew slot "position" varbits report it.
 * <p>
 * The values come from watching the game (via the Sailing plugin's notes): 0 is unassigned, 4 the
 * sails, 5 repairs, 7 the wind catcher, 10 the hook on a skiff, and 13 and 14 the two hooks on a
 * sloop. Anything else is treated as some other job. A crewmate who is seen salvaging counts as on a
 * hook regardless, in case a boat uses a value not listed here.
 */
public final class CrewAssignment
{
	public static final int NONE = 0;
	public static final int SAILS = 4;
	public static final int REPAIRS = 5;
	public static final int WIND_CATCHER = 7;
	public static final int HOOK_SKIFF = 10;
	public static final int HOOK_SLOOP_1 = 13;
	public static final int HOOK_SLOOP_2 = 14;

	private CrewAssignment()
	{
	}

	public static boolean isHook(int position)
	{
		return position == HOOK_SKIFF || position == HOOK_SLOOP_1 || position == HOOK_SLOOP_2;
	}

	public static boolean isAssigned(int position)
	{
		return position != NONE;
	}

	/** Whether this is a job we know is not a hook, so a salvage line cannot be coming from it. */
	public static boolean isKnownNonHook(int position)
	{
		return position == SAILS || position == REPAIRS || position == WIND_CATCHER;
	}

	/** A short name for the overlay and logs. */
	public static String describe(int position)
	{
		switch (position)
		{
			case NONE:
				return "idle";
			case SAILS:
				return "sails";
			case REPAIRS:
				return "repairs";
			case WIND_CATCHER:
				return "wind catcher";
			case HOOK_SKIFF:
			case HOOK_SLOOP_1:
			case HOOK_SLOOP_2:
				return "hook";
			default:
				return "job " + position;
		}
	}
}
