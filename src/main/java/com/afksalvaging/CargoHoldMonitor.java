/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

import net.runelite.api.Item;

/**
 * Tracks how full the current cargo hold is and decides when an alert is due.
 * <p>
 * The count is exact right after an item container update and becomes an estimate as
 * salvage and deposits are added to it between updates. Holds plain state with no client
 * access so the rules can be unit tested. All times are milliseconds from the same clock.
 */
public final class CargoHoldMonitor
{
	public enum Level
	{
		OK,
		NEARLY_FULL,
		FULL
	}

	/** Crew salvage arriving while the tally says full before the tally is pulled back by that many. */
	public static final int OVERCOUNT_EVIDENCE = 2;

	private int used = CargoHoldCapacity.UNKNOWN;
	private int capacity = CargoHoldCapacity.UNKNOWN;
	private boolean estimate;
	private boolean reportedFullByGame;
	private int overcountEvidence;
	private Level alerted = Level.OK;
	private long lastAlertAt;

	/** Counts occupied slots in an item container. Empty slots have no item or id -1. */
	public static int countUsed(Item[] items)
	{
		if (items == null)
		{
			return 0;
		}
		int count = 0;
		for (Item item : items)
		{
			if (item != null && item.getId() > -1 && item.getQuantity() > 0)
			{
				count++;
			}
		}
		return count;
	}

	/** Records an exact count from the item container. This supersedes any estimate or full message. */
	public void setUsed(int used)
	{
		this.used = used < 0 ? CargoHoldCapacity.UNKNOWN : used;
		estimate = false;
		reportedFullByGame = false;
		overcountEvidence = 0;
	}

	/** Restores a remembered count that may be out of date, for example after logging in. */
	public void setEstimatedUsed(int used)
	{
		this.used = used < 0 ? CargoHoldCapacity.UNKNOWN : used;
		estimate = true;
		reportedFullByGame = false;
		overcountEvidence = 0;
	}

	/**
	 * Adds cargo that was seen going in (positive) or out (negative) without a container update.
	 * The result stays between empty and the capacity, or the largest hold if the capacity is unknown.
	 *
	 * @return false when there is no count yet to adjust
	 */
	public boolean adjust(int delta)
	{
		if (used == CargoHoldCapacity.UNKNOWN)
		{
			return false;
		}
		int limit = capacity == CargoHoldCapacity.UNKNOWN ? CargoHoldCapacity.MAX_SLOTS : Math.max(capacity, used);
		used = Math.max(0, Math.min(limit, used + delta));
		estimate = true;
		if (delta != 0)
		{
			reportedFullByGame = false;
		}
		if (delta < 0)
		{
			overcountEvidence = 0;
		}
		return true;
	}

	/**
	 * A crewmate hooked salvage while the tally already said the hold was full. Crew stop when the
	 * hold is really full, so the tally was too high. One such event might be a race with the count;
	 * after {@link #OVERCOUNT_EVIDENCE} of them the tally is pulled back by that many and the alert
	 * re-armed, so a drifted count cannot leave the timer stuck at "full" for the rest of the trip.
	 *
	 * @return true when the tally was corrected
	 */
	public boolean crewSalvagedWhileFull()
	{
		if (!isFullByEstimate())
		{
			return false;
		}
		overcountEvidence++;
		if (overcountEvidence < OVERCOUNT_EVIDENCE)
		{
			return false;
		}
		used = Math.max(0, capacity - overcountEvidence);
		estimate = true;
		overcountEvidence = 0;
		return true;
	}

	/** Whether the hold reads full only because of the running tally, not a real count or the game saying so. */
	public boolean isFullByEstimate()
	{
		return estimate && !reportedFullByGame && hasCount() && used >= capacity;
	}

	/** Whether the hold is known full: the game said so, or an exact count reached the capacity. */
	public boolean isConfirmedFull()
	{
		if (reportedFullByGame)
		{
			return true;
		}
		return !estimate && hasCount() && used >= capacity;
	}

	public void setCapacity(int capacity)
	{
		this.capacity = capacity <= 0 ? CargoHoldCapacity.UNKNOWN : capacity;
	}

	/**
	 * The game itself said the hold is full, through a crewmate or a chat message.
	 * Treated as full until the next item container update, and the count moves up to the
	 * capacity so the counter agrees.
	 */
	public void markFullByGame()
	{
		reportedFullByGame = true;
		if (capacity != CargoHoldCapacity.UNKNOWN && (used == CargoHoldCapacity.UNKNOWN || used < capacity))
		{
			used = capacity;
			estimate = true;
		}
	}

	/** Forgets a full message from the game, for example once the player has left the boat. */
	public void clearFullByGame()
	{
		reportedFullByGame = false;
	}

	public boolean isReportedFullByGame()
	{
		return reportedFullByGame;
	}

	/** Whether the count has drifted from the last exact container update. */
	public boolean isEstimate()
	{
		return estimate;
	}

	public int getUsed()
	{
		return used;
	}

	/**
	 * Slots in the hold. If more items were counted than the expected capacity, the count wins,
	 * so the display never shows more used than total.
	 */
	public int getCapacity()
	{
		if (capacity == CargoHoldCapacity.UNKNOWN)
		{
			return CargoHoldCapacity.UNKNOWN;
		}
		return used > capacity ? used : capacity;
	}

	public boolean hasCount()
	{
		return used != CargoHoldCapacity.UNKNOWN && capacity != CargoHoldCapacity.UNKNOWN;
	}

	/** Free slots, or {@link CargoHoldCapacity#UNKNOWN} when either number is missing. */
	public int remaining()
	{
		return hasCount() ? Math.max(0, capacity - used) : CargoHoldCapacity.UNKNOWN;
	}

	/**
	 * The current fill level.
	 *
	 * @param warnSlots free slots at which the hold counts as nearly full; 0 disables that level
	 */
	public Level level(int warnSlots)
	{
		if (reportedFullByGame)
		{
			return Level.FULL;
		}
		if (!hasCount())
		{
			return Level.OK;
		}
		if (used >= capacity)
		{
			return Level.FULL;
		}
		if (warnSlots > 0 && capacity - used <= warnSlots)
		{
			return Level.NEARLY_FULL;
		}
		return Level.OK;
	}

	/**
	 * Decides whether an alert should be sent right now.
	 * <p>
	 * An alert fires the first time the hold reaches a level, and again when it climbs from nearly
	 * full to full. While it stays at that level, it fires again every {@code repeatMillis} if that
	 * is positive. Dropping back to OK re-arms everything.
	 *
	 * @return the level to announce, or {@code null} for nothing
	 */
	public Level poll(long now, int warnSlots, long repeatMillis)
	{
		Level current = level(warnSlots);
		if (current == Level.OK)
		{
			alerted = Level.OK;
			return null;
		}
		if (current.compareTo(alerted) > 0)
		{
			alerted = current;
			lastAlertAt = now;
			return current;
		}
		if (current.compareTo(alerted) < 0)
		{
			alerted = current;
			return null;
		}
		if (repeatMillis > 0 && now - lastAlertAt >= repeatMillis)
		{
			lastAlertAt = now;
			return current;
		}
		return null;
	}

	/** Forgets everything, for example when switching boats or logging out. */
	public void reset()
	{
		used = CargoHoldCapacity.UNKNOWN;
		capacity = CargoHoldCapacity.UNKNOWN;
		estimate = false;
		reportedFullByGame = false;
		overcountEvidence = 0;
		alerted = Level.OK;
		lastAlertAt = 0;
	}
}
