/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Works out who salvaged what from the signals the client gets, without counting anything twice.
 * <p>
 * Three things reveal a piece of salvage: a crewmate's overhead line, a Sailing XP drop, and the
 * player's own inventory gaining salvage. A crewmate earns the player 10% of the wreck's salvaging
 * XP per point of deckhandiness, so when the wreck is known the size of an XP drop says whether it
 * was the player, a level 4 deckhand, a level 3 one and so on; several drops in one tick add up.
 * The ghost crewmate never says he salvaged anything, so his XP is the only sign of it.
 * <p>
 * Lines and XP terms wait a couple of ticks for their counterpart. A matched pair is one event; an
 * unmatched line or XP term becomes an event on its own when its time is up. The player's salvage
 * is counted from their inventory alone, which is exact.
 */
public final class SalvageEvents
{
	/** Ticks a line or XP term waits for its counterpart. */
	public static final int WINDOW_TICKS = 2;
	/** Allowance for the XP boost of a keg of Horizon's lure. */
	private static final double BOOST_TOLERANCE = 0.03;
	/** Harvesting the crystal extractor gives 250 Sailing XP, a little more with Horizon's lure. */
	public static final int EXTRACTOR_XP_MIN = 245;
	public static final int EXTRACTOR_XP_MAX = 257;

	public enum Source
	{
		PLAYER,
		CREW
	}

	/** One piece of salvage and who hooked it. */
	public static final class Event
	{
		private final Source source;
		private final int deckhandiness;
		private final int tick;

		Event(Source source, int deckhandiness, int tick)
		{
			this.source = source;
			this.deckhandiness = deckhandiness;
			this.tick = tick;
		}

		public Source getSource()
		{
			return source;
		}

		/** The crewmate's deckhandiness, or 0 when it is not known. */
		public int getDeckhandiness()
		{
			return deckhandiness;
		}

		public int getTick()
		{
			return tick;
		}

		@Override
		public String toString()
		{
			return source + (source == Source.CREW ? "(D" + deckhandiness + ")" : "") + "@" + tick;
		}
	}

	/** What was going on when an XP drop arrived, so it can be read. */
	public static final class XpContext
	{
		/** The wreck being salvaged, or null when it is not known. */
		public ShipwreckType wreck;
		public boolean playerSalvaging;
		public boolean playerSorting;
		/** Distinct deckhandiness values of the crew on hooks. */
		public int[] crewDeckhandiness = new int[0];
		/** Sorting XP values that could be arriving, one per kind of salvage being sorted. */
		public double[] sortingXp = new double[0];
	}

	private static final class Pending
	{
		final int deckhandiness;
		final int tick;

		Pending(int deckhandiness, int tick)
		{
			this.deckhandiness = deckhandiness;
			this.tick = tick;
		}
	}

	private final List<Pending> lines = new ArrayList<>();
	private final List<Pending> xpTerms = new ArrayList<>();
	private final List<Integer> ghostLines = new ArrayList<>();
	private final List<Event> ready = new ArrayList<>();
	private int ignoreXpUntilTick = Integer.MIN_VALUE;
	private int unmatchedGhostLines;

	/** A crewmate said they hooked salvage. Deckhandiness 0 when the speaker could not be placed. */
	public void crewLine(int deckhandiness, int tick)
	{
		Pending term = takeMatching(xpTerms, deckhandiness, tick);
		if (term != null)
		{
			ready.add(new Event(Source.CREW, term.deckhandiness > 0 ? term.deckhandiness : deckhandiness, tick));
			return;
		}
		lines.add(new Pending(deckhandiness, tick));
	}

	/** The ghost crewmate wailed, which he does when he hooks something, and also when he does not. */
	public void ghostLine(int tick)
	{
		ghostLines.add(tick);
	}

	/**
	 * Sailing XP for the next couple of ticks is from something else, such as harvesting the
	 * crystal extractor, and must not be read as salvage.
	 */
	public void ignoreXpUntil(int tick)
	{
		ignoreXpUntilTick = Math.max(ignoreXpUntilTick, tick);
	}

	/**
	 * Wails from the ghost that no Sailing XP followed, in a row. He wails whether or not he is
	 * salvaging, so a run of these while he should be salvaging means he is not: the hold is full or
	 * something else has stopped him. Reset by a wail that XP did follow.
	 */
	public int unmatchedGhostLines()
	{
		return unmatchedGhostLines;
	}

	/** Forgets the run of unanswered wails, for example when the crew have plainly started again. */
	public void resetUnmatchedGhostLines()
	{
		unmatchedGhostLines = 0;
	}

	/** The player's inventory gained this much salvage while they were salvaging. Counted at once. */
	public void playerGain(int count, int tick)
	{
		for (int i = 0; i < count; i++)
		{
			ready.add(new Event(Source.PLAYER, 0, tick));
		}
	}

	/**
	 * Sailing XP arrived. When the wreck is known the drop is split into whoever earned it; when it
	 * is not, it can only confirm the ghost's wail.
	 */
	public void sailingXp(int delta, int tick, XpContext context)
	{
		if (delta <= 0 || tick <= ignoreXpUntilTick)
		{
			return;
		}
		if (delta >= EXTRACTOR_XP_MIN && delta <= EXTRACTOR_XP_MAX)
		{
			// The crystal extractor. No mix of salvage adds up to this without a player salvage, and
			// a player salvage shows in the inventory anyway.
			return;
		}
		if (context == null || context.wreck == null)
		{
			if (!ghostLines.isEmpty() && tick - ghostLines.get(0) <= WINDOW_TICKS)
			{
				ghostLines.remove(0);
				unmatchedGhostLines = 0;
				ready.add(new Event(Source.CREW, 4, tick));
			}
			return;
		}
		if (context.playerSorting && explainedBySortingAlone(delta, context) && !crewEvidenceWithin(tick))
		{
			// Sorting XP can look exactly like a crew salvage (pirate salvage sorted for 31.5, a level 4
			// deckhand's pirate salvage for 30.4). Without a crew line or the ghost's wail, it is sorting.
			return;
		}
		List<Double> amounts = new ArrayList<>();
		List<Integer> kinds = new ArrayList<>();
		double base = context.wreck.getSalvagingXp();
		if (context.playerSalvaging)
		{
			amounts.add(base);
			kinds.add(0);
		}
		for (int d : context.crewDeckhandiness)
		{
			if (d >= 1 && d <= 4)
			{
				amounts.add(base * d / 10.0);
				kinds.add(d);
			}
		}
		if (context.playerSorting)
		{
			for (double xp : context.sortingXp)
			{
				amounts.add(xp);
				kinds.add(-1);
			}
		}
		int[] best = explain(delta, amounts);
		if (best == null)
		{
			return;
		}
		for (int i = 0; i < best.length; i++)
		{
			int kind = kinds.get(i);
			if (kind <= 0)
			{
				continue;
			}
			for (int n = 0; n < best[i]; n++)
			{
				creditCrewXp(kind, tick);
			}
		}
	}

	/**
	 * Moves time on: anything that waited long enough for a counterpart becomes an event of its own.
	 *
	 * @return the events ready since the last call, oldest first
	 */
	public List<Event> drain(int tick)
	{
		Iterator<Pending> it = lines.iterator();
		while (it.hasNext())
		{
			Pending line = it.next();
			if (tick - line.tick > WINDOW_TICKS)
			{
				ready.add(new Event(Source.CREW, line.deckhandiness, line.tick));
				it.remove();
			}
		}
		it = xpTerms.iterator();
		while (it.hasNext())
		{
			Pending term = it.next();
			if (tick - term.tick > WINDOW_TICKS)
			{
				ready.add(new Event(Source.CREW, term.deckhandiness, term.tick));
				it.remove();
			}
		}
		Iterator<Integer> ghosts = ghostLines.iterator();
		while (ghosts.hasNext())
		{
			if (tick - ghosts.next() > WINDOW_TICKS)
			{
				ghosts.remove();
				unmatchedGhostLines++;
			}
		}
		List<Event> out = new ArrayList<>(ready);
		ready.clear();
		return out;
	}

	public void reset()
	{
		lines.clear();
		xpTerms.clear();
		ghostLines.clear();
		ready.clear();
		ignoreXpUntilTick = Integer.MIN_VALUE;
		unmatchedGhostLines = 0;
	}

	private static boolean explainedBySortingAlone(int delta, XpContext context)
	{
		List<Double> sorting = new ArrayList<>();
		for (double xp : context.sortingXp)
		{
			sorting.add(xp);
		}
		return explain(delta, sorting) != null;
	}

	private boolean crewEvidenceWithin(int tick)
	{
		for (Pending line : lines)
		{
			if (tick - line.tick <= WINDOW_TICKS)
			{
				return true;
			}
		}
		for (int ghost : ghostLines)
		{
			if (tick - ghost <= WINDOW_TICKS)
			{
				return true;
			}
		}
		return false;
	}

	private void creditCrewXp(int deckhandiness, int tick)
	{
		Pending line = takeMatching(lines, deckhandiness, tick);
		if (line != null)
		{
			ready.add(new Event(Source.CREW, deckhandiness, tick));
			return;
		}
		if (deckhandiness == 4 && !ghostLines.isEmpty() && tick - ghostLines.get(0) <= WINDOW_TICKS)
		{
			ghostLines.remove(0);
			unmatchedGhostLines = 0;
		}
		xpTerms.add(new Pending(deckhandiness, tick));
	}

	/** Removes and returns the oldest pending entry within the window whose deckhandiness agrees. */
	private static Pending takeMatching(List<Pending> pending, int deckhandiness, int tick)
	{
		Iterator<Pending> it = pending.iterator();
		while (it.hasNext())
		{
			Pending p = it.next();
			if (tick - p.tick > WINDOW_TICKS)
			{
				continue;
			}
			if (deckhandiness == 0 || p.deckhandiness == 0 || p.deckhandiness == deckhandiness)
			{
				it.remove();
				return p;
			}
		}
		return null;
	}

	/**
	 * Finds how many of each amount add up to the delta, allowing a point of rounding per term and a
	 * small boost. Prefers the fewest terms. Returns null when nothing fits.
	 */
	static int[] explain(int delta, List<Double> amounts)
	{
		if (amounts.isEmpty())
		{
			return null;
		}
		int[] counts = new int[amounts.size()];
		int[] best = null;
		int bestTerms = Integer.MAX_VALUE;
		int[] limit = new int[amounts.size()];
		for (int i = 0; i < amounts.size(); i++)
		{
			limit[i] = amounts.get(i) <= 0 ? 0 : Math.min(4, (int) Math.floor((delta + 1) / amounts.get(i)) + 1);
		}
		int i = 0;
		while (true)
		{
			// Evaluate the current combination.
			double sum = 0;
			int terms = 0;
			for (int j = 0; j < counts.length; j++)
			{
				sum += counts[j] * amounts.get(j);
				terms += counts[j];
			}
			if (terms > 0 && terms < bestTerms)
			{
				double tolerance = terms + sum * BOOST_TOLERANCE;
				if (Math.abs(sum - delta) <= tolerance + 1e-9)
				{
					best = counts.clone();
					bestTerms = terms;
				}
			}
			// Advance the odometer.
			i = 0;
			while (i < counts.length)
			{
				counts[i]++;
				if (counts[i] <= limit[i])
				{
					break;
				}
				counts[i] = 0;
				i++;
			}
			if (i == counts.length)
			{
				return best;
			}
		}
	}
}
