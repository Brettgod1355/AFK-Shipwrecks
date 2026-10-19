/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import net.runelite.api.gameval.AnimationID;

/**
 * What the player is doing at their own facilities, read from their animation.
 * <p>
 * The salvaging animations loop with short gaps, so the detector keeps the last activity for a few
 * ticks after the animation stops rather than flickering back to idle between cycles.
 */
public enum PlayerActivity
{
	/** Not at a hook or station. */
	IDLE,
	/** At a salvaging hook and rolling for salvage. */
	SALVAGING,
	/** Standing at a salvaging hook with nothing to salvage. */
	AT_HOOK_IDLE,
	/** Sorting salvage at a salvaging station. */
	SORTING;

	/** Whether the player is occupying a hook, salvaging or not. */
	public boolean isAtHook()
	{
		return this == SALVAGING || this == AT_HOOK_IDLE;
	}

	/** Animations played while operating a salvaging hook, for each boat size. */
	static final Set<Integer> SALVAGING_ANIMATIONS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
		AnimationID.SAILING_HUMAN_SALVAGE_HOOK_KANDARIN_1X3_DROP01,
		AnimationID.SAILING_HUMAN_SALVAGE_HOOK_KANDARIN_1X3_IDLE01,
		AnimationID.SAILING_HUMAN_SALVAGE_HOOK_KANDARIN_1X3_SALVAGING01,
		AnimationID.SAILING_HUMAN_SALVAGE_HOOK_KANDARIN_1X3_PULL01,
		AnimationID.SAILING_HUMAN_SALVAGE_HOOK_KANDARIN_2X5_DROP01,
		AnimationID.SAILING_HUMAN_SALVAGE_HOOK_KANDARIN_2X5_IDLE01,
		AnimationID.SAILING_HUMAN_SALVAGE_HOOK_KANDARIN_3X8_DROP01,
		AnimationID.SAILING_HUMAN_SALVAGE_HOOK_KANDARIN_3X8_IDLE01,
		AnimationID.HUMAN_SAILING_SALVAGE01_LARGE01_DROP01,
		AnimationID.HUMAN_SAILING_SALVAGE01_LARGE01_IDLE01
	)));

	/**
	 * Standing at a hook with no wreck to salvage: the small hook's resting loop, and every hook's reset once its wreck
	 * sinks under the player. After a reset the bigger hooks show no animation at all (seen on a 3x8 hook, Original
	 * RS2, 2026-10-19), so the detector keeps the player at the hook until they move.
	 */
	static final Set<Integer> AT_HOOK_IDLE_ANIMATIONS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
		AnimationID.SAILING_HUMAN_SALVAGE_HOOK_KANDARIN_1X3_INACTIVE01,
		AnimationID.SAILING_HUMAN_SALVAGE_HOOK_KANDARIN_1X3_RESET01,
		AnimationID.SAILING_HUMAN_SALVAGE_HOOK_KANDARIN_2X5_RESET01,
		AnimationID.SAILING_HUMAN_SALVAGE_HOOK_KANDARIN_3X8_RESET01,
		AnimationID.HUMAN_SAILING_SALVAGE01_LARGE01_RESET01
	)));

	/** Animations played while sorting at a salvaging station. */
	static final Set<Integer> SORTING_ANIMATIONS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
		AnimationID.SAILING_HUMAN_SALVAGE_HOOK_KANDARIN_1X3_INTERACT01,
		AnimationID.HUMAN_SAILING_SALVAGE01_LARGE01_INTERACT01
	)));

	/** The activity an animation id shows, or IDLE for anything else including no animation. */
	public static PlayerActivity fromAnimation(int animationId)
	{
		if (SALVAGING_ANIMATIONS.contains(animationId))
		{
			return SALVAGING;
		}
		if (AT_HOOK_IDLE_ANIMATIONS.contains(animationId))
		{
			return AT_HOOK_IDLE;
		}
		if (SORTING_ANIMATIONS.contains(animationId))
		{
			return SORTING;
		}
		return IDLE;
	}

	/**
	 * Tracks the player's activity over time with a short hold so looping animations do not flicker.
	 * Ticks are game ticks.
	 */
	public static final class Detector
	{
		/** Ticks an activity survives without its animation before counting as idle. */
		public static final int HOLD_TICKS = 3;

		private PlayerActivity current = IDLE;
		private int lastSeenTick = Integer.MIN_VALUE;
		/** Where the player stood when last seen idle at a hook, or null. */
		private Object idleAt;

		/** Feeds the player's current animation for this tick, with no position to hold a hook by. */
		public PlayerActivity update(int animationId, int tick)
		{
			return update(animationId, tick, null);
		}

		/**
		 * Feeds the player's current animation for this tick.
		 *
		 * @param where where the player stands on deck (any value with equals), or null when unknown: standing on
		 *              the same spot with no animation keeps them idle at the hook
		 * @return the activity after this tick
		 */
		public PlayerActivity update(int animationId, int tick, Object where)
		{
			PlayerActivity seen = fromAnimation(animationId);
			if (seen != IDLE)
			{
				current = seen;
				lastSeenTick = tick;
				idleAt = seen == AT_HOOK_IDLE ? where : null;
			}
			else if (current == AT_HOOK_IDLE && animationId == -1 && idleAt != null && idleAt.equals(where))
			{
				lastSeenTick = tick;
			}
			else if (current != IDLE && tick - lastSeenTick > HOLD_TICKS)
			{
				current = IDLE;
				idleAt = null;
			}
			return current;
		}

		/** Forces idle at once, for example when the player leaves the boat. */
		public void reset()
		{
			current = IDLE;
			lastSeenTick = Integer.MIN_VALUE;
			idleAt = null;
		}

		public PlayerActivity current()
		{
			return current;
		}
	}
}
