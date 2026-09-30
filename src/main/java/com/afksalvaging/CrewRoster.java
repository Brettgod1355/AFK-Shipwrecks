/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The crew slots and who is doing what, kept up to date from the crew varbits and from what the
 * crewmates say.
 * <p>
 * A crewmate is on a hook when their assignment says so, or when they were heard hooking salvage
 * recently and their assignment is not one we know to be something else. Only the first
 * {@code boatCapacity} filled slots are aboard; the game picks crew in slot order when more are
 * assigned than the boat can carry.
 */
public final class CrewRoster
{
	public static final int SLOTS = 5;
	/** How long a crewmate heard salvaging is still assumed to be on a hook, in game ticks. */
	public static final int HEARD_SALVAGING_TICKS = 100;

	private final Crewmate[] crew = new Crewmate[SLOTS];
	private final int[] position = new int[SLOTS];
	private final int[] lastSalvageTick = new int[SLOTS];
	private int boatCapacity = SLOTS;

	public CrewRoster()
	{
		clear();
	}

	/** Slots the current boat can carry: 0 on a raft, 2 on a skiff, 5 on a sloop. */
	public void setBoatCapacity(int capacity)
	{
		boatCapacity = Math.max(0, Math.min(SLOTS, capacity));
	}

	public int getBoatCapacity()
	{
		return boatCapacity;
	}

	/** Records who is in a slot (null for nobody), keeping the slot's assignment. Slots run 0 to 4. */
	public void setCrewmate(int slot, Crewmate crewmate)
	{
		check(slot);
		if (crewmate == null || (crew[slot] != null && crew[slot].getUniqueId() != crewmate.getUniqueId()))
		{
			lastSalvageTick[slot] = Integer.MIN_VALUE;
		}
		crew[slot] = crewmate;
	}

	public void setPosition(int slot, int assignment)
	{
		check(slot);
		if (assignment != position[slot] && !CrewAssignment.isHook(assignment))
		{
			// Moved off the hook: what they said while on it no longer places them there.
			lastSalvageTick[slot] = Integer.MIN_VALUE;
		}
		position[slot] = assignment;
	}

	public Crewmate getCrewmate(int slot)
	{
		check(slot);
		return crew[slot];
	}

	public int getPosition(int slot)
	{
		check(slot);
		return position[slot];
	}

	/** Whether the slot holds a crewmate who is on the boat. */
	public boolean isAboard(int slot)
	{
		check(slot);
		return crew[slot] != null && aboardIndex(slot) < boatCapacity;
	}

	/**
	 * A crewmate in this slot was heard hooking salvage.
	 *
	 * @return false when the slot is empty
	 */
	public boolean noteSalvage(int slot, int tick)
	{
		check(slot);
		if (crew[slot] == null)
		{
			return false;
		}
		lastSalvageTick[slot] = tick;
		return true;
	}

	/** Whether the crewmate in this slot is working a salvaging hook. */
	public boolean isOnHook(int slot, int tick)
	{
		check(slot);
		if (!isAboard(slot))
		{
			return false;
		}
		if (CrewAssignment.isHook(position[slot]))
		{
			return true;
		}
		if (CrewAssignment.isKnownNonHook(position[slot]))
		{
			return false;
		}
		return lastSalvageTick[slot] != Integer.MIN_VALUE && tick - lastSalvageTick[slot] <= HEARD_SALVAGING_TICKS;
	}

	/**
	 * Finds the slot of the crewmate who just spoke, by name. Names are unique per crewmate, and the
	 * NPC standing on the boat carries the crewmate's name.
	 *
	 * @return the slot, or -1 when no aboard crewmate has that name
	 */
	public int slotByName(String name)
	{
		if (name == null)
		{
			return -1;
		}
		String wanted = name.trim().toLowerCase(Locale.ROOT);
		if (wanted.isEmpty())
		{
			return -1;
		}
		for (int slot = 0; slot < SLOTS; slot++)
		{
			if (crew[slot] != null && isAboard(slot) && crew[slot].getName().trim().toLowerCase(Locale.ROOT).equals(wanted))
			{
				return slot;
			}
		}
		return -1;
	}

	/** Crewmates aboard and on a hook, in slot order. */
	public List<Crewmate> onHooks(int tick)
	{
		List<Crewmate> list = new ArrayList<>();
		for (int slot = 0; slot < SLOTS; slot++)
		{
			if (isOnHook(slot, tick))
			{
				list.add(crew[slot]);
			}
		}
		return list;
	}

	/** Slots of the crewmates on hooks, in slot order, so their assignments can be looked up. */
	public List<Integer> hookSlots(int tick)
	{
		List<Integer> list = new ArrayList<>();
		for (int slot = 0; slot < SLOTS; slot++)
		{
			if (isOnHook(slot, tick))
			{
				list.add(slot);
			}
		}
		return list;
	}

	/**
	 * Whether the crewmate in this slot is aboard with no job at all. A crewmate on the sails, a
	 * cannon or any other facility is busy, not spare.
	 */
	public boolean isIdle(int slot, int tick)
	{
		check(slot);
		return isAboard(slot) && position[slot] == CrewAssignment.NONE && !isOnHook(slot, tick);
	}

	/** Crewmates aboard with no job, in slot order. */
	public List<Crewmate> idle(int tick)
	{
		List<Crewmate> list = new ArrayList<>();
		for (int slot = 0; slot < SLOTS; slot++)
		{
			if (isIdle(slot, tick))
			{
				list.add(crew[slot]);
			}
		}
		return list;
	}

	/** Idle crewmates who are deckhand enough to work a hook of this tier. */
	public int countSpareFor(SalvagingHookTier tier, int tick)
	{
		int needed = tier == null ? 1 : tier.getDeckhandiness();
		int count = 0;
		for (Crewmate crewmate : idle(tick))
		{
			if (crewmate.effectiveDeckhandiness() >= needed)
			{
				count++;
			}
		}
		return count;
	}

	public int countAboard()
	{
		int count = 0;
		for (int slot = 0; slot < SLOTS; slot++)
		{
			if (isAboard(slot))
			{
				count++;
			}
		}
		return count;
	}

	public int countOnHooks(int tick)
	{
		return onHooks(tick).size();
	}

	/** Distinct deckhandiness values among the crew on hooks, for reading XP drops. */
	public int[] deckhandinessOnHooks(int tick)
	{
		boolean[] seen = new boolean[5];
		int count = 0;
		for (Crewmate crewmate : onHooks(tick))
		{
			int d = crewmate.effectiveDeckhandiness();
			if (!seen[d])
			{
				seen[d] = true;
				count++;
			}
		}
		int[] values = new int[count];
		int i = 0;
		for (int d = 1; d <= 4; d++)
		{
			if (seen[d])
			{
				values[i++] = d;
			}
		}
		return values;
	}

	public void clear()
	{
		for (int slot = 0; slot < SLOTS; slot++)
		{
			crew[slot] = null;
			position[slot] = CrewAssignment.NONE;
			lastSalvageTick[slot] = Integer.MIN_VALUE;
		}
	}

	/** Position of this slot among the filled slots, which decides who fits on the boat. */
	private int aboardIndex(int slot)
	{
		int index = 0;
		for (int i = 0; i < slot; i++)
		{
			if (crew[i] != null)
			{
				index++;
			}
		}
		return index;
	}

	private static void check(int slot)
	{
		if (slot < 0 || slot >= SLOTS)
		{
			throw new IllegalArgumentException("crew slot out of range: " + slot);
		}
	}
}
