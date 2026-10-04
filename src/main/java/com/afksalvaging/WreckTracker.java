/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.runelite.api.coords.WorldPoint;

/**
 * Follows the shipwreck sites the client can see and how long the wrecks in reach have left.
 * <p>
 * Each site is one tile that shows either a wreck that can be salvaged or the stump of one that has
 * sunk. A wreck's clock starts when anyone first salvages it. Other players' salvaging cannot be
 * seen, so the clock is anchored to the first moment one of our own hooks was working the wreck;
 * until then all that is known is that it sinks within one lifetime of being worked. A site that
 * drops out of view (the scene reloads as the boat moves) is kept for a while so a wreck seen again
 * is not mistaken for a new one.
 * <p>
 * The tracker also measures how much of the time a usable wreck has been in reach, faded so recent
 * history counts most. Times are milliseconds.
 */
public final class WreckTracker
{
	/** How long a site that dropped out of view is remembered. */
	public static final long FORGET_AFTER_MILLIS = 5 * 60_000L;
	/** Time constant for the wreck availability measurement. */
	public static final long AVAILABILITY_TAU_MILLIS = 20 * 60_000L;

	/** One wreck site. */
	public static final class Site
	{
		private final WorldPoint point;
		private final ShipwreckType type;
		private boolean active;
		private boolean present = true;
		/** Whether one of our hooks has been seen working this wreck, and when that started. */
		private boolean worked;
		private long workedSince;
		private long sunkAt;
		private long lastSeenAt;
		/** When we saw this wreck rise, or 0 when it was already up when first seen. */
		private long roseAt;

		Site(WorldPoint point, ShipwreckType type)
		{
			this.point = point;
			this.type = type;
		}

		public WorldPoint getPoint()
		{
			return point;
		}

		public ShipwreckType getType()
		{
			return type;
		}

		public boolean isActive()
		{
			return active;
		}

		public boolean isPresent()
		{
			return present;
		}

		/** Whether the clock has been anchored to our own salvaging. */
		public boolean isAnchored()
		{
			return active && worked;
		}

		/**
		 * Milliseconds until this wreck sinks at the latest: the lifetime less the time since we
		 * started working it, or a whole lifetime when we have not. Zero once sunk.
		 */
		public long remainingAtMost(long now)
		{
			if (!active)
			{
				return 0;
			}
			long lifetime = type.getLifetimeSeconds() * 1000L;
			if (!worked)
			{
				return lifetime;
			}
			return Math.max(0, workedSince + lifetime - now);
		}

		public long getSunkAt()
		{
			return sunkAt;
		}

		/**
		 * The latest this wreck should sink, as a time, taking its clock from our own salvaging when
		 * we have worked it, else from its rise when the world is one where every wreck is worked at
		 * once, else -1: nothing is known. May lie in the past when the wreck has outlived its clock.
		 */
		long sinksBy(boolean workedFromRise)
		{
			if (!active)
			{
				return -1;
			}
			long lifetime = type.getLifetimeSeconds() * 1000L;
			if (worked)
			{
				return workedSince + lifetime;
			}
			if (workedFromRise && roseAt > 0)
			{
				return roseAt + lifetime;
			}
			return -1;
		}
	}

	/** How long a wreck may outlive its clock before the clock is written off as wrong. */
	public static final long CLOCK_GRACE_MILLIS = 60_000;

	/** When a wreck should next rise in reach, at most, and whether it is bound to be one of ours. */
	public static final class NextRise
	{
		public final long withinMillis;
		public final boolean certain;
		/** The other wreck has outlived its clock: the rise is due any moment, not at a time we can name. */
		public final boolean overdue;

		NextRise(long withinMillis, boolean certain, boolean overdue)
		{
			this.withinMillis = withinMillis;
			this.certain = certain;
			this.overdue = overdue;
		}
	}

	/**
	 * Writes off the clock of any wreck that has outlived it by {@link #CLOCK_GRACE_MILLIS}: the
	 * lifetimes are the wiki's averages and "worked from its rise" is an assumption, so a wreck
	 * can last longer than either says. Such a wreck gives no bound until it is worked again.
	 */
	public void forgetStaleClocks(boolean salvagingWorld, long now)
	{
		for (Site site : sites.values())
		{
			long by = site.sinksBy(salvagingWorld);
			if (by >= 0 && now - by > CLOCK_GRACE_MILLIS)
			{
				site.worked = false;
				site.workedSince = 0;
				site.roseAt = 0;
			}
		}
	}

	private final Map<WorldPoint, Site> sites = new HashMap<>();
	private final DecayingSum uptime = new DecayingSum(AVAILABILITY_TAU_MILLIS);
	private final DecayingSum elapsed = new DecayingSum(AVAILABILITY_TAU_MILLIS);
	private long lastSinkAt;

	/**
	 * Notes a wreck object in view, whether newly spawned, found by scanning the scene, or replayed
	 * by a scene reload.
	 *
	 * @return true when the object was a wreck or stump
	 */
	public boolean observe(WorldPoint point, int objectId, long now)
	{
		ShipwreckType type = ShipwreckType.fromObjectId(objectId);
		if (type == null || point == null)
		{
			return false;
		}
		boolean activeNow = ShipwreckType.isActiveWreck(objectId);
		Site site = sites.get(point);
		if (site == null || site.type != type)
		{
			site = new Site(point, type);
			site.active = activeNow;
			sites.put(point, site);
		}
		else if (activeNow && !site.active)
		{
			// A new wreck rose on this site; whatever we knew about the old one is gone.
			site.worked = false;
			site.workedSince = 0;
			site.sunkAt = 0;
			site.roseAt = now;
		}
		else if (!activeNow && site.active)
		{
			site.sunkAt = now;
			site.worked = false;
			site.workedSince = 0;
			lastSinkAt = now;
		}
		site.active = activeNow;
		site.present = true;
		site.lastSeenAt = now;
		return true;
	}

	/** Notes a wreck object leaving the scene. It may have sunk or the boat may have sailed off. */
	public void despawn(WorldPoint point, int objectId, long now)
	{
		Site site = point == null ? null : sites.get(point);
		// Only the object the site shows now can take it out of view. When a wreck sinks, its stump
		// may be added before the wreck is removed; that late removal must not hide the stump.
		if (site != null && ShipwreckType.fromObjectId(objectId) == site.type
			&& ShipwreckType.isActiveWreck(objectId) == site.active)
		{
			site.present = false;
			site.lastSeenAt = now;
		}
	}

	/** The scene is being reloaded: nothing is in view until it is seen again. */
	public void markAllAbsent(long now)
	{
		for (Site site : sites.values())
		{
			if (site.present)
			{
				site.present = false;
				site.lastSeenAt = now;
			}
		}
	}

	/** Forgets sites that have been out of view for a while. */
	public void prune(long now)
	{
		Iterator<Site> it = sites.values().iterator();
		while (it.hasNext())
		{
			Site site = it.next();
			if (!site.present && now - site.lastSeenAt > FORGET_AFTER_MILLIS)
			{
				it.remove();
			}
		}
	}

	/** Sites in view within range of any of the points, nearest first. Range is in tiles, same plane only. */
	public List<Site> nearby(List<WorldPoint> references, int range)
	{
		List<Site> list = new ArrayList<>();
		if (references == null || references.isEmpty())
		{
			return list;
		}
		Map<Site, Integer> distances = new HashMap<>();
		for (Site site : sites.values())
		{
			if (!site.present)
			{
				continue;
			}
			int nearest = Integer.MAX_VALUE;
			for (WorldPoint reference : references)
			{
				if (reference != null && site.point.getPlane() == reference.getPlane())
				{
					nearest = Math.min(nearest, site.point.distanceTo2D(reference));
				}
			}
			if (nearest <= range)
			{
				list.add(site);
				distances.put(site, nearest);
			}
		}
		list.sort(Comparator.comparingInt((Site s) -> distances.get(s))
			.thenComparingInt(s -> s.point.getX())
			.thenComparingInt(s -> s.point.getY()));
		return list;
	}

	public List<Site> nearby(WorldPoint reference, int range)
	{
		return nearby(Collections.singletonList(reference), range);
	}

	/** Wrecks in reach that are up and that the player has the level to salvage. */
	public List<Site> eligibleActive(List<WorldPoint> references, int range, int sailingLevel)
	{
		List<Site> list = new ArrayList<>();
		for (Site site : nearby(references, range))
		{
			if (site.active && sailingLevel >= site.type.getSailingLevel())
			{
				list.add(site);
			}
		}
		return list;
	}

	/** Whether any wreck in reach is up, whatever its level. */
	public boolean anyActive(List<WorldPoint> references, int range)
	{
		for (Site site : nearby(references, range))
		{
			if (site.active)
			{
				return true;
			}
		}
		return false;
	}

	/** The kind of wreck that is up in reach and usable, nearest first, or null. */
	public ShipwreckType typeInReach(List<WorldPoint> references, int range, int sailingLevel)
	{
		List<Site> wrecks = eligibleActive(references, range, sailingLevel);
		return wrecks.isEmpty() ? null : wrecks.get(0).type;
	}

	/**
	 * One of our hooks is rolling right now, so every usable wreck it can reach has been worked from
	 * at least this moment.
	 */
	public void noteRolling(List<WorldPoint> references, int range, int sailingLevel, long now)
	{
		for (Site site : eligibleActive(references, range, sailingLevel))
		{
			if (!site.worked)
			{
				site.worked = true;
				site.workedSince = now;
			}
		}
	}

	/**
	 * Milliseconds until every usable wreck in reach has sunk, at most. Zero when none is up. The
	 * bound is loose for wrecks whose clock is not anchored to our own salvaging.
	 */
	public long allSunkWithin(List<WorldPoint> references, int range, int sailingLevel, long now)
	{
		long longest = 0;
		for (Site site : eligibleActive(references, range, sailingLevel))
		{
			longest = Math.max(longest, site.remainingAtMost(now));
		}
		return longest;
	}

	/** Whether every usable wreck in reach has an anchored clock, so the bound above is meaningful. */
	public boolean allAnchored(List<WorldPoint> references, int range, int sailingLevel)
	{
		List<Site> wrecks = eligibleActive(references, range, sailingLevel);
		if (wrecks.isEmpty())
		{
			return false;
		}
		for (Site site : wrecks)
		{
			if (!site.isAnchored())
			{
				return false;
			}
		}
		return true;
	}

	/**
	 * When a wreck should next rise at the sites in reach, while none is up there. The wrecks of an
	 * area share one pool and one rises the moment another sinks (wiki, Shipwreck salvaging), so
	 * the answer is the earliest sink among the other wrecks in view. A wreck's clock runs from
	 * when we worked it; on a salvaging world, where every wreck is worked from the moment it
	 * rises, from its rise. Certain when every sunk site in view is in reach, so the next rise can
	 * land nowhere else; otherwise only likely.
	 *
	 * @return null when a wreck is up in reach, no sunk site is in reach, or no other wreck has a
	 * clock to go by
	 */
	public NextRise nextRise(List<WorldPoint> references, int range, int sailingLevel, boolean salvagingWorld, long now)
	{
		List<Site> inReach = nearby(references, range);
		boolean sunkInReach = false;
		for (Site site : inReach)
		{
			if (site.active && site.type.getSailingLevel() <= sailingLevel)
			{
				return null;
			}
			sunkInReach |= !site.active;
		}
		if (!sunkInReach)
		{
			return null;
		}
		long earliest = -1;
		boolean certain = true;
		for (Site site : presentSites())
		{
			if (inReach.contains(site))
			{
				continue;
			}
			if (!site.active)
			{
				// Another empty site in the area: the next wreck may rise there instead.
				certain = false;
				continue;
			}
			long by = site.sinksBy(salvagingWorld);
			if (by >= 0 && (earliest < 0 || by < earliest))
			{
				earliest = by;
			}
		}
		if (earliest < 0)
		{
			return null;
		}
		// Past its clock the bound is spent: the rise is due any moment rather than at a time (the
		// overlay showed "≤ 0:00" before this, owner 2026-10-03).
		return new NextRise(Math.max(0, earliest - now), certain, earliest <= now);
	}

	/** The most recent time a wreck in view sank, or 0. */
	public long getLastSinkAt()
	{
		return lastSinkAt;
	}

	/** Records whether a usable wreck was in reach over the last {@code dtMillis} of time that mattered. */
	public void recordAvailability(boolean available, long dtMillis, long now)
	{
		if (dtMillis <= 0)
		{
			return;
		}
		elapsed.add(dtMillis, now);
		if (available)
		{
			uptime.add(dtMillis, now);
		}
		else
		{
			uptime.decayTo(now);
		}
	}

	/**
	 * The share of recent time a usable wreck was in reach, blended with a prior belief so it is
	 * usable before much has been observed.
	 *
	 * @param prior            availability to assume with no evidence, 0 to 1
	 * @param priorWeightMillis how much observed time it takes to count as much as the prior
	 */
	public double availability(double prior, long priorWeightMillis, long now)
	{
		uptime.decayTo(now);
		elapsed.decayTo(now);
		double p = Math.max(0, Math.min(1, prior));
		double w = Math.max(0, priorWeightMillis);
		double total = elapsed.value() + w;
		if (total <= 0)
		{
			return p;
		}
		return Math.max(0, Math.min(1, (uptime.value() + p * w) / total));
	}

	public int siteCount()
	{
		return sites.size();
	}

	/** The sites in view right now, wrecks up or sunk, in no particular order. */
	public List<Site> presentSites()
	{
		List<Site> list = new ArrayList<>();
		for (Site site : sites.values())
		{
			if (site.present)
			{
				list.add(site);
			}
		}
		return list;
	}

	/** Forgets all sites and history, for example after a world hop. */
	public void clear()
	{
		sites.clear();
		uptime.reset();
		elapsed.reset();
		lastSinkAt = 0;
	}
}
