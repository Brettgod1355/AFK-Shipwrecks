/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Decides what on the player's own boat to highlight next, so a glance says where the salvage goes
 * (owner, 2026-10-12). The loop:
 * <ol>
 * <li>Salvage in the inventory: the salvaging station, to sort it (the cargo hold, to deposit it, on a boat
 * with no station). Not while the player is still hooking with room left; a full inventory is the strong
 * look, since hooked salvage is being lost.</li>
 * <li>No salvage in the inventory, items left with a Drop box: nothing; drop them first.</li>
 * <li>Then items left with an Alch box: nothing; alch them first.</li>
 * <li>Then items left with a Deposit box: the cargo hold.</li>
 * <li>Then: the cargo hold if it holds salvage (withdraw more to sort), and a hook (hook by hand), side by
 * side; the player picks. After the hold was opened and closed with nothing taken, only the hook.</li>
 * </ol>
 * Only Keep boxes never hold a step back. Pure, so it is tested without a client.
 */
public final class SalvageGuide
{
	/** What to point at. */
	public enum Target
	{
		HOLD,
		STATION,
		HOOK
	}

	/** One hook on the boat, as the guide needs it. */
	public static final class HookFacts
	{
		public final long hash;
		public final SalvagingHookTier tier;
		/** The deckhandiness of the crewmate working it, or -1 when no crewmate is on it. */
		public final int crewDeckhandiness;
		/** How far the player stands from it, in local units; smaller is nearer. */
		public final int distance;

		public HookFacts(long hash, SalvagingHookTier tier, int crewDeckhandiness, int distance)
		{
			this.hash = hash;
			this.tier = tier;
			this.crewDeckhandiness = crewDeckhandiness;
			this.distance = distance;
		}

		boolean isFree()
		{
			return crewDeckhandiness < 0;
		}
	}

	/** Everything the guide looks at, read once a tick. */
	public static final class Inputs
	{
		public GuideTarget mode = GuideTarget.AUTOMATIC;
		public GuideHook hookChoice = GuideHook.WEAKEST_CREW;
		/** On the player's own boat. */
		public boolean ownBoat;
		public boolean hasStation;
		public int inventorySalvage;
		public boolean inventoryFull;
		/** Inventory items carrying each box; all 0 while the boxes are not showing. */
		public int dropBoxed;
		public int alchBoxed;
		public int depositBoxed;
		/** Whether the hold is known to hold salvage. */
		public boolean holdHasSalvage;
		/** How many salvage the hold holds, or {@link HoldSalvageCount#UNKNOWN}. */
		public int holdSalvage = HoldSalvageCount.UNKNOWN;
		/** Free inventory slots, or -1 when unknown. */
		public int freeSlots = -1;
		/** The hold was opened and closed with nothing taken and no salvage in the inventory. */
		public boolean holdTurnedDown;
		/** The player is at a hook: hooking, or standing at one. */
		public boolean playerAtHook;
		/** A wreck the player could hook is in reach. */
		public boolean wreckInReach;
		public List<HookFacts> hooks = Collections.emptyList();
	}

	/** What to draw this tick. */
	public static final class Result
	{
		public static final Result NONE = new Result(Collections.emptyList(), 0, false);

		private final List<Target> targets;
		private final long hookHash;
		private final boolean strong;

		Result(List<Target> targets, long hookHash, boolean strong)
		{
			this.targets = targets;
			this.hookHash = hookHash;
			this.strong = strong;
		}

		public boolean shows(Target target)
		{
			return targets.contains(target);
		}

		public List<Target> getTargets()
		{
			return targets;
		}

		/** The hook to highlight, when {@link #shows(Target)} says HOOK. */
		public long getHookHash()
		{
			return hookHash;
		}

		/** The full-inventory look: brighter and thicker. */
		public boolean isStrong()
		{
			return strong;
		}

		public boolean isEmpty()
		{
			return targets.isEmpty();
		}

		/**
		 * The station alone, or nothing: what shows while the cargo hold window is open, since the station can be
		 * clicked to sort straight from it (owner, 2026-10-13). The hold and hook wait until the window closes.
		 */
		public Result stationOnly()
		{
			return shows(Target.STATION) ? new Result(Collections.singletonList(Target.STATION), 0, strong) : NONE;
		}
	}

	private SalvageGuide()
	{
	}

	public static Result decide(Inputs in)
	{
		if (!in.ownBoat || in.mode == GuideTarget.OFF)
		{
			return Result.NONE;
		}
		if (in.inventorySalvage > 0)
		{
			if (in.playerAtHook && !in.inventoryFull)
			{
				// Still hooking with room left: nothing to fetch yet.
				return Result.NONE;
			}
			// The station sorts it; with no station, or for a player who chose the hold, the hold takes it.
			Target where = in.mode == GuideTarget.HOLD || !in.hasStation ? Target.HOLD : Target.STATION;
			return allowed(in.mode, where) ? only(where, in.inventoryFull) : Result.NONE;
		}
		if (in.dropBoxed > 0 || in.alchBoxed > 0)
		{
			return Result.NONE;
		}
		if (in.depositBoxed > 0)
		{
			return allowed(in.mode, Target.HOLD) ? only(Target.HOLD, false) : Result.NONE;
		}
		if (in.playerAtHook && in.wreckInReach)
		{
			// Already hooking: the hold is not the next step, and lighting it until the first catch lands was
			// a flash of the wrong thing (owner, 2026-10-17). Waiting at a hook with no wreck up, the hold stays.
			return Result.NONE;
		}
		boolean hold = in.holdHasSalvage && !in.holdTurnedDown && allowed(in.mode, Target.HOLD);
		HookFacts hook = in.playerAtHook || !in.wreckInReach ? null : pickHook(in.hooks, in.hookChoice);
		if (hold && hook != null && in.holdSalvage != HoldSalvageCount.UNKNOWN && in.freeSlots > 0)
		{
			// Both would do: the hold once it has a full inventory's worth to take out, else the hook (owner,
			// 2026-10-17). Unknown counts keep both lit, as before.
			return in.holdSalvage >= in.freeSlots ? only(Target.HOLD, false)
				: new Result(Collections.singletonList(Target.HOOK), hook.hash, false);
		}
		List<Target> targets = new ArrayList<>();
		if (hold)
		{
			targets.add(Target.HOLD);
		}
		if (hook != null)
		{
			targets.add(Target.HOOK);
		}
		return targets.isEmpty() ? Result.NONE : new Result(targets, hook == null ? 0 : hook.hash, false);
	}

	/**
	 * The hook to point the player at: a free one if there is one (nearest first), otherwise, when the setting
	 * allows it, the one whose crewmate has the least deckhandiness, since replacing them costs least. Ties go
	 * to the weaker hook, then the nearer one. Null when the setting is off or no hook qualifies.
	 */
	public static HookFacts pickHook(List<HookFacts> hooks, GuideHook choice)
	{
		if (choice == GuideHook.OFF || hooks == null)
		{
			return null;
		}
		HookFacts best = null;
		for (HookFacts hook : hooks)
		{
			if (hook.isFree() && (best == null || hook.distance < best.distance))
			{
				best = hook;
			}
		}
		if (best != null || choice == GuideHook.FREE_ONLY)
		{
			return best;
		}
		for (HookFacts hook : hooks)
		{
			if (best == null || weaker(hook, best))
			{
				best = hook;
			}
		}
		return best;
	}

	private static boolean weaker(HookFacts a, HookFacts b)
	{
		if (a.crewDeckhandiness != b.crewDeckhandiness)
		{
			return a.crewDeckhandiness < b.crewDeckhandiness;
		}
		int tierA = a.tier == null ? -1 : a.tier.ordinal();
		int tierB = b.tier == null ? -1 : b.tier.ordinal();
		if (tierA != tierB)
		{
			return tierA < tierB;
		}
		return a.distance < b.distance;
	}

	/** Whether the setting lets this object be highlighted; the hook has its own setting. */
	private static boolean allowed(GuideTarget mode, Target target)
	{
		switch (mode)
		{
			case AUTOMATIC:
				return true;
			case STATION:
				return target == Target.STATION;
			case HOLD:
				return target == Target.HOLD;
			default:
				return false;
		}
	}

	private static Result only(Target target, boolean strong)
	{
		return new Result(Collections.singletonList(target), 0, strong);
	}
}
