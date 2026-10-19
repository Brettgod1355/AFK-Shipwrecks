/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

/**
 * Notices when a salvaging hook is standing empty and somebody could be working it.
 * <p>
 * Two stories. The player steps off their hook to sort salvage and forgets to hand it to the spare
 * crewmate: the reminder says to assign one. Or the wreck sank while the player was on the hook and
 * a new one is up, but unlike a crewmate the player does not start again by themselves: the reminder
 * says to click the hook. The watch waits out a grace period, so stepping off to deposit and coming
 * straight back does not trigger it, reminds once, and optionally keeps reminding. It re-arms when
 * every hook is manned again. Times are milliseconds.
 */
public final class HookWatch
{
	public enum Signal
	{
		NONE,
		REMIND,
		REPEAT
	}

	/** Why a hook needs attention. */
	public enum Reason
	{
		/** A crewmate aboard could take the hook. */
		SPARE_CREW,
		/** Nobody can take it but the player. */
		PLAYER_NEEDED
	}

	/** This tick's situation, filled in by the plugin. */
	public static final class Situation
	{
		/** Hooks built on the boat. */
		public int hookCount;
		/** Crewmates working hooks. */
		public int crewOnHooks;
		/** Whether the player is at a hook, salvaging or not. */
		public boolean playerAtHook;
		/**
		 * Whether the player stands at a hook doing nothing (its wreck sank under them). With a wreck in reach they
		 * still count as off it, since they do not restart by themselves, but the word is to click it.
		 */
		public boolean playerIdleAtHook;
		/** Whether the player is sorting at a station. */
		public boolean playerSorting;
		/** Idle crewmates aboard who are deckhand enough for an empty hook. */
		public int spareCrew;
		/** Whether the hold is full, in which case nobody could salvage anyway. */
		public boolean holdFull;
		/** Whether a wreck the player can salvage is in reach. */
		public boolean wreckInReach;
		/** Whether the boat is standing still on the player's own boat. */
		public boolean parked = true;
		/**
		 * Whether a wreck site, up or sunk, is within reach of a hook: only there is an empty hook worth a
		 * word (owner, 2026-10-03; within reach rather than in view, 2026-10-04).
		 */
		public boolean atSpot = true;
	}

	private long emptySince = -1;
	private long lastRemindedAt = -1;
	private long extraGrace;
	/** The player started sorting while a hook stood empty: the assign guide may show before the reminder. */
	private boolean sortingWhileEmpty;
	private boolean reminded;
	private Reason reason;

	/**
	 * Feeds this tick's situation.
	 *
	 * @param graceMillis  how long the hook must stay empty before the first reminder
	 * @param repeatMillis how often to remind again while it stays empty; 0 or less for never
	 */
	public Signal update(long now, Situation s, long graceMillis, long repeatMillis)
	{
		Reason current = reasonFor(s);
		if (current == null)
		{
			reset();
			return Signal.NONE;
		}
		reason = current;
		if (emptySince < 0)
		{
			emptySince = now;
		}
		if (!reminded)
		{
			// Always the player's grace (owner, 2026-10-17: "make it follow the grace period"); sorting no longer
			// cuts it short, it only brings the assign guide forward (see showAssignGuide).
			long grace = graceMillis + extraGrace;
			if (now - emptySince >= Math.max(0, grace))
			{
				reminded = true;
				lastRemindedAt = now;
				return Signal.REMIND;
			}
			return Signal.NONE;
		}
		if (repeatMillis > 0 && now - lastRemindedAt >= repeatMillis)
		{
			lastRemindedAt = now;
			return Signal.REPEAT;
		}
		return Signal.NONE;
	}

	/** Grace granted when the player is clearly only depositing or withdrawing. */
	public static final long DEPOSIT_GRACE_MILLIS = 10_000;

	/** The player clicked the hold: give them longer to come back before reminding. */
	public void noteHoldUse()
	{
		if (!reminded)
		{
			extraGrace += DEPOSIT_GRACE_MILLIS;
		}
	}

	/**
	 * The player has settled in to sort, so they are not coming back to the hook soon: the guide to assign a
	 * crewmate may show at once, while the reminder itself still waits the grace (owner, 2026-10-17: "an overlay
	 * isnt as annoying as an alert imo but the alert is still nice incase we forget").
	 */
	public void noteSortingStarted()
	{
		sortingWhileEmpty = true;
	}

	/**
	 * Whether to show the guide to assign a crewmate to the empty hook: once the reminder is up, or earlier when the
	 * player started sorting with the hook empty. Only when a crewmate aboard could take it.
	 */
	public boolean showAssignGuide()
	{
		return reason == Reason.SPARE_CREW && (reminded || sortingWhileEmpty);
	}

	/** Why the standing reminder was raised, or null when none is standing. */
	public Reason getReason()
	{
		return reminded ? reason : null;
	}

	/** Whether a reminder is currently standing, for the banner. */
	public boolean isReminding()
	{
		return reminded;
	}

	/** How many hooks are unmanned. */
	public static int emptyHooks(int hookCount, int crewOnHooks, boolean playerAtHook)
	{
		int manned = Math.min(hookCount, Math.max(0, crewOnHooks) + (playerAtHook ? 1 : 0));
		return Math.max(0, hookCount - manned);
	}

	/** What, if anything, is wrong right now. */
	static Reason reasonFor(Situation s)
	{
		if (!s.parked || !s.atSpot || s.holdFull || s.hookCount <= 0)
		{
			return null;
		}
		int empty = emptyHooks(s.hookCount, s.crewOnHooks, s.playerAtHook);
		if (empty == 0)
		{
			return null;
		}
		// Standing at their own hook after the wreck sank, the player wants to click it, not to hand it to a
		// crewmate (owner, 2026-10-19: an "empty hook" alarm while salvaging on the hook themselves).
		boolean standing = s.playerIdleAtHook && !s.playerAtHook;
		if ((standing ? empty - 1 : empty) > 0 && s.spareCrew > 0)
		{
			return Reason.SPARE_CREW;
		}
		if (standing)
		{
			return s.wreckInReach ? Reason.PLAYER_NEEDED : null;
		}
		if (!s.playerAtHook && !s.playerSorting && s.wreckInReach)
		{
			return Reason.PLAYER_NEEDED;
		}
		return null;
	}

	public void reset()
	{
		emptySince = -1;
		lastRemindedAt = -1;
		extraGrace = 0;
		sortingWhileEmpty = false;
		reminded = false;
		reason = null;
	}
}
