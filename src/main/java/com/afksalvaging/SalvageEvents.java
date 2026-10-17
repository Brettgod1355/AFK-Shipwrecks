/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	private static final Logger log = LoggerFactory.getLogger(SalvageEvents.class);

	/** Ticks a line or XP term waits for its counterpart. */
	public static final int WINDOW_TICKS = 2;
	/**
	 * Ticks a line already counted on its own still claims XP that turns up late. The game can pay the
	 * crew's XP well after their line (in game, 2026-10-10: two salvages' XP arrived together 6 and 16
	 * ticks after the salvage), and counting that XP again made the hold run high.
	 */
	public static final int LATE_XP_TICKS = 20;
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
	/** Lines counted on their own, still waiting to claim XP that arrives late. */
	private final List<Pending> loneLines = new ArrayList<>();
	/**
	 * Every line counted on its own, however long ago, until a held-back XP lump pays it. A window open long
	 * enough (a level-up message left up for minutes) holds the XP past {@link #LATE_XP_TICKS}; the lump that
	 * comes when it closes holds these lines' shares, which must not be counted again (in game, 2026-10-17,
	 * Original RS2: four lines counted, then a lump of seven shares; all seven were counted and the hold ran 4 high).
	 */
	private final List<Pending> heldLines = new ArrayList<>();
	/** Lines older than this are let go: no window stays open that long on a salvaging trip. */
	private static final int HELD_LINE_MAX_TICKS = 1000;
	/** The tick the game said the crew's held-back XP was being paid. */
	private int heldPayTick = Integer.MIN_VALUE;
	/**
	 * The ticks of the last Sailing XP drop and the one before it. While a window holds the XP back, none arrives
	 * at all, so only lines counted since the drop before the lump can be what it pays: the same message also
	 * comes with ordinary crew XP (in game, 2026-10-13, Air Marshal hooking: the message, a line and its 65 XP all
	 * at tick 1258, two ticks after the player's own XP).
	 */
	private int lastXpTick = Integer.MIN_VALUE;
	private int previousXpTick = Integer.MIN_VALUE;
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
			log.debug("Salvage: crew line (deckhandiness {}) at tick {} matched XP from tick {}", deckhandiness, tick, term.tick);
			// Dated by the earlier of the two, which is when the salvage landed.
			ready.add(new Event(Source.CREW, term.deckhandiness > 0 ? term.deckhandiness : deckhandiness,
				Math.min(term.tick, tick)));
			return;
		}
		log.debug("Salvage: crew line (deckhandiness {}) at tick {} waits for XP", deckhandiness, tick);
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
		// The drop before this one bounds what a held-back lump can pay (see lastXpTick).
		int since = lastXpTick == tick ? previousXpTick : lastXpTick;
		noteXpDrop(tick);
		if (delta >= EXTRACTOR_XP_MIN && delta <= EXTRACTOR_XP_MAX)
		{
			// The crystal extractor. No mix of salvage adds up to this without a player salvage, and
			// a player salvage shows in the inventory anyway.
			return;
		}
		if (context == null || context.wreck == null)
		{
			if (context != null && context.playerSorting && explainedBySortingAlone(delta, context))
			{
				// Sorting, whatever the ghost says: he wails whether or not he hooks anything, and with no wreck known
				// a sort that fell on a wail was counted as his salvage (in game: Air Marshal 2026-10-15 tick 927 and
				// 2026-10-17 tick 850, Original RS2 2026-10-13 tick 4776; each left the hold one high).
				return;
			}
			if (!ghostLines.isEmpty() && tick - ghostLines.get(0) <= WINDOW_TICKS)
			{
				int wail = ghostLines.remove(0);
				unmatchedGhostLines = 0;
				ready.add(new Event(Source.CREW, 4, Math.min(wail, tick)));
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
		log.debug("Salvage: {} Sailing XP at tick {} read as {} against {}", delta, tick,
			best == null ? "nothing" : Arrays.toString(best), amounts);
		if (best == null)
		{
			return;
		}
		boolean heldPay = heldPayTick != Integer.MIN_VALUE && tick >= heldPayTick && tick - heldPayTick <= WINDOW_TICKS;
		for (int i = 0; i < best.length; i++)
		{
			int kind = kinds.get(i);
			if (kind <= 0)
			{
				continue;
			}
			for (int n = 0; n < best[i]; n++)
			{
				if (heldPay && absorbHeld(kind, tick, since))
				{
					continue;
				}
				creditCrewXp(kind, tick);
			}
		}
		if (heldPay)
		{
			// The lump pays everything held back at once: no line counted before it still waits for pay.
			heldLines.clear();
			loneLines.clear();
			heldPayTick = Integer.MIN_VALUE;
		}
	}

	/** A Sailing XP drop arrived (any, crew or not): held-back pay only ever covers lines since the one before. */
	private void noteXpDrop(int tick)
	{
		if (tick != lastXpTick)
		{
			previousXpTick = lastXpTick;
			lastXpTick = tick;
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
				log.debug("Salvage: crew line from tick {} had no XP; counted on its own at tick {}", line.tick, tick);
				ready.add(new Event(Source.CREW, line.deckhandiness, line.tick));
				if (line.tick > ignoreXpUntilTick)
				{
					// Its XP may still come. A line whose XP fell in an ignored stretch has had it already.
					loneLines.add(line);
					heldLines.add(line);
				}
				it.remove();
			}
		}
		loneLines.removeIf(line -> tick - line.tick > LATE_XP_TICKS);
		heldLines.removeIf(line -> tick - line.tick > HELD_LINE_MAX_TICKS);
		it = xpTerms.iterator();
		while (it.hasNext())
		{
			Pending term = it.next();
			if (tick - term.tick > WINDOW_TICKS)
			{
				it.remove();
				Pending counted = takeMatching(loneLines, term.deckhandiness, term.tick, LATE_XP_TICKS);
				if (counted != null)
				{
					heldLines.remove(counted);
					log.debug("Salvage: XP from tick {} (deckhandiness {}) is late pay for the line counted from tick {}",
						term.tick, term.deckhandiness, counted.tick);
					continue;
				}
				log.debug("Salvage: XP from tick {} (deckhandiness {}) had no crew line; counted on its own at tick {}",
					term.tick, term.deckhandiness, tick);
				ready.add(new Event(Source.CREW, term.deckhandiness, term.tick));
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

	/** Crew lines and XP waiting for their counterpart: salvage that has landed but is not counted yet. */
	int pending()
	{
		return lines.size() + xpTerms.size();
	}

	/** The XP held back while the hold was open was paid and set aside: no line still waits for its pay. */
	public void heldBackXpPaid()
	{
		loneLines.clear();
		heldLines.clear();
	}

	/**
	 * The game said "You gain some experience by watching your crew work.": the Sailing XP this tick is the lump
	 * held back while a window was open. Its shares for lines already counted are not new salvage. The XP usually
	 * comes just after the message in the same tick; shares that came first are settled here.
	 */
	public void heldBackXpLine(int tick)
	{
		heldPayTick = tick;
		Iterator<Pending> it = xpTerms.iterator();
		while (it.hasNext())
		{
			Pending term = it.next();
			// The drop these terms came in is already the last one: the lump pays lines since the one before.
			int since = lastXpTick == term.tick ? previousXpTick : lastXpTick;
			if (Math.abs(tick - term.tick) <= WINDOW_TICKS && absorbHeld(term.deckhandiness, term.tick, since))
			{
				it.remove();
			}
		}
	}

	/**
	 * One share of a held-back lump: pay for a line counted on its own since the XP drop before the lump, if there
	 * is one of that kind.
	 */
	private boolean absorbHeld(int deckhandiness, int tick, int since)
	{
		Pending line = null;
		Iterator<Pending> it = heldLines.iterator();
		while (it.hasNext())
		{
			Pending p = it.next();
			if (p.tick > since && tick - p.tick <= HELD_LINE_MAX_TICKS
				&& (deckhandiness == 0 || p.deckhandiness == 0 || p.deckhandiness == deckhandiness))
			{
				it.remove();
				line = p;
				break;
			}
		}
		if (line == null)
		{
			return false;
		}
		loneLines.remove(line);
		log.debug("Salvage: held-back XP at tick {} (deckhandiness {}) is pay for the line counted from tick {}",
			tick, deckhandiness, line.tick);
		return true;
	}

	public void reset()
	{
		lines.clear();
		loneLines.clear();
		heldLines.clear();
		heldPayTick = Integer.MIN_VALUE;
		lastXpTick = Integer.MIN_VALUE;
		previousXpTick = Integer.MIN_VALUE;
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
			log.debug("Salvage: XP (deckhandiness {}) at tick {} matched a crew line from tick {}", deckhandiness, tick, line.tick);
			// Dated by the line when it came first (in game, 2026-10-10: the line at tick 958, the hold read
			// 119 at 958, the XP at 959; dated 959, the salvage was counted again on top of the read).
			ready.add(new Event(Source.CREW, deckhandiness, Math.min(line.tick, tick)));
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
		return takeMatching(pending, deckhandiness, tick, WINDOW_TICKS);
	}

	/** Removes and returns the oldest entry at most {@code window} ticks old whose deckhandiness agrees. */
	private static Pending takeMatching(List<Pending> pending, int deckhandiness, int tick, int window)
	{
		Iterator<Pending> it = pending.iterator();
		while (it.hasNext())
		{
			Pending p = it.next();
			if (tick - p.tick > window)
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
	 * small boost. Prefers the fewest terms, then a sum closer by more than a point of rounding: 124 XP
	 * while sorting is a level 3 crewmate and a sort (123.6), not two level 4 salvages (129.6), which
	 * also fit the tolerance and were taken first (in game, 2026-10-10: one salvage counted as three).
	 * Within a point of each other the first found stands. Returns null when nothing fits.
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
		double bestError = Double.MAX_VALUE;
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
			if (terms > 0 && terms <= bestTerms)
			{
				double tolerance = terms + sum * BOOST_TOLERANCE;
				double error = Math.abs(sum - delta);
				if (error <= tolerance + 1e-9 && (terms < bestTerms || error < bestError - 1))
				{
					best = counts.clone();
					bestTerms = terms;
					bestError = error;
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
