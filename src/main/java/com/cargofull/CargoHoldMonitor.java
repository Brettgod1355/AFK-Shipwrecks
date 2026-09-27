/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.cargofull;

import net.runelite.api.Item;

/**
 * Tracks how full the current cargo hold is and decides when an alert is due.
 * <p>
 * Holds plain state with no client access so the alert rules can be unit tested.
 * All times are milliseconds from the same clock.
 */
public final class CargoHoldMonitor
{
	public enum Level
	{
		OK,
		NEARLY_FULL,
		FULL
	}

	private int used = CargoHoldCapacity.UNKNOWN;
	private int capacity = CargoHoldCapacity.UNKNOWN;
	private boolean reportedFullByGame;
	private Level alerted = Level.OK;
	private long lastAlertAt;

	/** Counts occupied slots in a cargo item container. Empty slots have no item or id -1. */
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

	/** Records a fresh count from the item container. This supersedes any full message from the game. */
	public void setUsed(int used)
	{
		this.used = used < 0 ? CargoHoldCapacity.UNKNOWN : used;
		reportedFullByGame = false;
	}

	public void setCapacity(int capacity)
	{
		this.capacity = capacity <= 0 ? CargoHoldCapacity.UNKNOWN : capacity;
	}

	/**
	 * The game itself said the hold is full, through a crewmate or a chat message.
	 * Treated as full until the next item container update.
	 */
	public void markFullByGame()
	{
		reportedFullByGame = true;
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
		reportedFullByGame = false;
		alerted = Level.OK;
		lastAlertAt = 0;
	}
}
