/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

import java.util.List;
import net.runelite.api.coords.WorldPoint;

/**
 * A route this plugin sent to a salvaging spot, watched until it has done its job and should be
 * taken off the screen. Shortest Path only clears a route at its exact target, which here is the
 * middle of the hotspot, and a boat parks at a wreck some way from that.
 */
public final class SpotRoute
{
	/** One of the spot's wreck sites this near the player counts as having arrived. */
	public static final int ARRIVAL_TILES = 15;
	/** How far from a hotspot's middle a wreck site is still taken to be part of it. */
	public static final int SPOT_TILES = 48;
	/** Ticks off the boat before the player is taken to have left it, so one odd tick does not count. */
	public static final int LEAVE_TICKS = 3;

	/** Why a route is over, with what the sidebar says about it. */
	public enum End
	{
		NONE(null),
		ARRIVED("Route cleared: you are at the spot."),
		LEFT_BOAT("Route cleared: you left your boat.");

		private final String status;

		End(String status)
		{
			this.status = status;
		}

		public String getStatus()
		{
			return status;
		}
	}

	private final SalvagingSpot spot;
	/** A route sent from land is not over until the player has boarded and left again. */
	private boolean beenAboard;
	private int ticksAshore;

	public SpotRoute(SalvagingSpot spot)
	{
		this.spot = spot;
	}

	public SalvagingSpot getSpot()
	{
		return spot;
	}

	/**
	 * One game tick.
	 *
	 * @param aboard whether the player is on their own boat
	 * @param player where the player is on the world map, or null if unknown
	 * @param sites  the wreck sites in view
	 */
	public End tick(boolean aboard, WorldPoint player, List<WreckTracker.Site> sites)
	{
		if (aboard)
		{
			beenAboard = true;
			ticksAshore = 0;
		}
		else if (beenAboard && ++ticksAshore >= LEAVE_TICKS)
		{
			return End.LEFT_BOAT;
		}
		if (player == null)
		{
			return End.NONE;
		}
		if (Mooring.distance(player, spot.getPoint()) <= ARRIVAL_TILES)
		{
			return End.ARRIVED;
		}
		for (WreckTracker.Site site : sites)
		{
			if (Mooring.distance(site.getPoint(), spot.getPoint()) <= SPOT_TILES
				&& Mooring.distance(site.getPoint(), player) <= ARRIVAL_TILES)
			{
				return End.ARRIVED;
			}
		}
		return End.NONE;
	}
}
