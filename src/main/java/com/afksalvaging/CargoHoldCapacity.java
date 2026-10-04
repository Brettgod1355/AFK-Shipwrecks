/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.runelite.api.gameval.ObjectID;

/**
 * Maps the cargo hold object built on a boat to the number of slots it holds.
 * <p>
 * Each hold tier is a different object for each boat size (raft, skiff, sloop), and each of
 * those has three variants: idle, showing cargo, and shown empty. Capacity depends on both the
 * tier and the boat size, so the object id alone identifies it. The cargo item container the
 * server sends is always {@link #MAX_SLOTS} long regardless of the hold, which is why the real
 * limit has to come from the hold itself.
 */
public final class CargoHoldCapacity
{
	public static final int UNKNOWN = -1;
	/** Slots in the largest hold, and the fixed length of the cargo item container. */
	public static final int MAX_SLOTS = 240;

	private static final Pattern NUMBER = Pattern.compile("[0-9]+");
	private static final Map<Integer, Integer> CAPACITY_BY_OBJECT = new HashMap<>();

	static
	{
		// Basic
		hold(20, ObjectID.SAILING_BOAT_CARGO_HOLD_REGULAR_RAFT, ObjectID.SAILING_BOAT_CARGO_HOLD_REGULAR_RAFT_CARGO, ObjectID.SAILING_BOAT_CARGO_HOLD_REGULAR_RAFT_NO_CARGO);
		hold(30, ObjectID.SAILING_BOAT_CARGO_HOLD_REGULAR_2X5, ObjectID.SAILING_BOAT_CARGO_HOLD_REGULAR_2X5_CARGO, ObjectID.SAILING_BOAT_CARGO_HOLD_REGULAR_2X5_NO_CARGO);
		hold(40, ObjectID.SAILING_BOAT_CARGO_HOLD_REGULAR_LARGE, ObjectID.SAILING_BOAT_CARGO_HOLD_REGULAR_LARGE_CARGO, ObjectID.SAILING_BOAT_CARGO_HOLD_REGULAR_LARGE_NO_CARGO);
		// Oak
		hold(30, ObjectID.SAILING_BOAT_CARGO_HOLD_OAK_RAFT, ObjectID.SAILING_BOAT_CARGO_HOLD_OAK_RAFT_CARGO, ObjectID.SAILING_BOAT_CARGO_HOLD_OAK_RAFT_NO_CARGO);
		hold(45, ObjectID.SAILING_BOAT_CARGO_HOLD_OAK_2X5, ObjectID.SAILING_BOAT_CARGO_HOLD_OAK_2X5_CARGO, ObjectID.SAILING_BOAT_CARGO_HOLD_OAK_2X5_NO_CARGO);
		hold(60, ObjectID.SAILING_BOAT_CARGO_HOLD_OAK_LARGE, ObjectID.SAILING_BOAT_CARGO_HOLD_OAK_LARGE_CARGO, ObjectID.SAILING_BOAT_CARGO_HOLD_OAK_LARGE_NO_CARGO);
		// Teak
		hold(45, ObjectID.SAILING_BOAT_CARGO_HOLD_TEAK_RAFT, ObjectID.SAILING_BOAT_CARGO_HOLD_TEAK_RAFT_CARGO, ObjectID.SAILING_BOAT_CARGO_HOLD_TEAK_RAFT_NO_CARGO);
		hold(60, ObjectID.SAILING_BOAT_CARGO_HOLD_TEAK_2X5, ObjectID.SAILING_BOAT_CARGO_HOLD_TEAK_2X5_CARGO, ObjectID.SAILING_BOAT_CARGO_HOLD_TEAK_2X5_NO_CARGO);
		hold(90, ObjectID.SAILING_BOAT_CARGO_HOLD_TEAK_LARGE, ObjectID.SAILING_BOAT_CARGO_HOLD_TEAK_LARGE_CARGO, ObjectID.SAILING_BOAT_CARGO_HOLD_TEAK_LARGE_NO_CARGO);
		// Mahogany
		hold(60, ObjectID.SAILING_BOAT_CARGO_HOLD_MAHOGANY_RAFT, ObjectID.SAILING_BOAT_CARGO_HOLD_MAHOGANY_RAFT_CARGO, ObjectID.SAILING_BOAT_CARGO_HOLD_MAHOGANY_RAFT_NO_CARGO);
		hold(90, ObjectID.SAILING_BOAT_CARGO_HOLD_MAHOGANY_2X5, ObjectID.SAILING_BOAT_CARGO_HOLD_MAHOGANY_2X5_CARGO, ObjectID.SAILING_BOAT_CARGO_HOLD_MAHOGANY_2X5_NO_CARGO);
		hold(120, ObjectID.SAILING_BOAT_CARGO_HOLD_MAHOGANY_LARGE, ObjectID.SAILING_BOAT_CARGO_HOLD_MAHOGANY_LARGE_CARGO, ObjectID.SAILING_BOAT_CARGO_HOLD_MAHOGANY_LARGE_NO_CARGO);
		// Camphor
		hold(80, ObjectID.SAILING_BOAT_CARGO_HOLD_CAMPHOR_RAFT, ObjectID.SAILING_BOAT_CARGO_HOLD_CAMPHOR_RAFT_CARGO, ObjectID.SAILING_BOAT_CARGO_HOLD_CAMPHOR_RAFT_NO_CARGO);
		hold(120, ObjectID.SAILING_BOAT_CARGO_HOLD_CAMPHOR_2X5, ObjectID.SAILING_BOAT_CARGO_HOLD_CAMPHOR_2X5_CARGO, ObjectID.SAILING_BOAT_CARGO_HOLD_CAMPHOR_2X5_NO_CARGO);
		hold(160, ObjectID.SAILING_BOAT_CARGO_HOLD_CAMPHOR_LARGE, ObjectID.SAILING_BOAT_CARGO_HOLD_CAMPHOR_LARGE_CARGO, ObjectID.SAILING_BOAT_CARGO_HOLD_CAMPHOR_LARGE_NO_CARGO);
		// Ironwood
		hold(105, ObjectID.SAILING_BOAT_CARGO_HOLD_IRONWOOD_RAFT, ObjectID.SAILING_BOAT_CARGO_HOLD_IRONWOOD_RAFT_CARGO, ObjectID.SAILING_BOAT_CARGO_HOLD_IRONWOOD_RAFT_NO_CARGO);
		hold(150, ObjectID.SAILING_BOAT_CARGO_HOLD_IRONWOOD_2X5, ObjectID.SAILING_BOAT_CARGO_HOLD_IRONWOOD_2X5_CARGO, ObjectID.SAILING_BOAT_CARGO_HOLD_IRONWOOD_2X5_NO_CARGO);
		hold(210, ObjectID.SAILING_BOAT_CARGO_HOLD_IRONWOOD_LARGE, ObjectID.SAILING_BOAT_CARGO_HOLD_IRONWOOD_LARGE_CARGO, ObjectID.SAILING_BOAT_CARGO_HOLD_IRONWOOD_LARGE_NO_CARGO);
		// Rosewood
		hold(120, ObjectID.SAILING_BOAT_CARGO_HOLD_ROSEWOOD_RAFT, ObjectID.SAILING_BOAT_CARGO_HOLD_ROSEWOOD_RAFT_CARGO, ObjectID.SAILING_BOAT_CARGO_HOLD_ROSEWOOD_RAFT_NO_CARGO);
		hold(180, ObjectID.SAILING_BOAT_CARGO_HOLD_ROSEWOOD_2X5, ObjectID.SAILING_BOAT_CARGO_HOLD_ROSEWOOD_2X5_CARGO, ObjectID.SAILING_BOAT_CARGO_HOLD_ROSEWOOD_2X5_NO_CARGO);
		hold(240, ObjectID.SAILING_BOAT_CARGO_HOLD_ROSEWOOD_LARGE, ObjectID.SAILING_BOAT_CARGO_HOLD_ROSEWOOD_LARGE_CARGO, ObjectID.SAILING_BOAT_CARGO_HOLD_ROSEWOOD_LARGE_NO_CARGO);
	}

	private CargoHoldCapacity()
	{
	}

	private static void hold(int capacity, int... objectIds)
	{
		for (int objectId : objectIds)
		{
			CAPACITY_BY_OBJECT.put(objectId, capacity);
		}
	}

	/** Whether this game object is a cargo hold of any tier or size. */
	public static boolean isCargoHold(int objectId)
	{
		return CAPACITY_BY_OBJECT.containsKey(objectId);
	}

	/** Slots in the given cargo hold object, or {@link #UNKNOWN} if it is not a cargo hold. */
	public static int forObjectId(int objectId)
	{
		return CAPACITY_BY_OBJECT.getOrDefault(objectId, UNKNOWN);
	}

	/**
	 * Reads the first whole number from interface text such as {@code "37"} or {@code "37 / 40"}.
	 *
	 * @return the number, or {@link #UNKNOWN} when the text has none
	 */
	public static int firstNumber(String text)
	{
		if (text == null)
		{
			return UNKNOWN;
		}
		Matcher matcher = NUMBER.matcher(text);
		return matcher.find() ? parse(matcher.group()) : UNKNOWN;
	}

	/**
	 * Reads the last whole number from interface text such as {@code "40"} or {@code "37 / 40"}.
	 *
	 * @return the number, or {@link #UNKNOWN} when the text has none
	 */
	public static int lastNumber(String text)
	{
		if (text == null)
		{
			return UNKNOWN;
		}
		int value = UNKNOWN;
		Matcher matcher = NUMBER.matcher(text);
		while (matcher.find())
		{
			value = parse(matcher.group());
		}
		return value;
	}

	private static int parse(String digits)
	{
		try
		{
			return Integer.parseInt(digits);
		}
		catch (NumberFormatException e)
		{
			return UNKNOWN;
		}
	}
}
