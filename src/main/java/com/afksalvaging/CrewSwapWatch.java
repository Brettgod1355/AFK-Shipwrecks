/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

/**
 * Says, once, when a stronger crewmate sits idle while a weaker one works a hook (owner,
 * 2026-10-03). The pair has to stand for {@link #GRACE_MILLIS} first, since crew change jobs for
 * a moment as the player shuffles them. Once said, the same pair is never said again, however
 * often the player steps off their hook to sort and comes back; a different pair is news.
 */
public final class CrewSwapWatch
{
	public static final long GRACE_MILLIS = 10_000;

	/** Put {@code in} on the hook instead of {@code out}. */
	public static final class Suggestion
	{
		public final Crewmate out;
		public final Crewmate in;
		public final int outDeckhandiness;
		public final int inDeckhandiness;

		public Suggestion(Crewmate out, Crewmate in, int outDeckhandiness, int inDeckhandiness)
		{
			this.out = out;
			this.in = in;
			this.outDeckhandiness = outDeckhandiness;
			this.inDeckhandiness = inDeckhandiness;
		}

		String key()
		{
			return out.getUniqueId() + ">" + in.getUniqueId();
		}
	}

	/** This tick's swap, or null. */
	private Suggestion current;
	/** The pair being timed towards an announcement, and since when. */
	private String candidateKey;
	private long since = -1;
	/** The pair last announced; it is not announced again. */
	private String toldKey;

	/**
	 * Feeds this tick's best swap, or null when the hooks are as well crewed as they can be.
	 *
	 * @return the suggestion to announce now, or null
	 */
	public Suggestion update(long now, Suggestion swap)
	{
		current = swap;
		if (swap == null)
		{
			candidateKey = null;
			since = -1;
			return null;
		}
		String key = swap.key();
		if (!key.equals(candidateKey))
		{
			candidateKey = key;
			since = now;
		}
		if (key.equals(toldKey) || now - since < GRACE_MILLIS)
		{
			return null;
		}
		toldKey = key;
		return swap;
	}

	/** The swap already announced and still worth making, for the overlay; null otherwise. */
	public Suggestion standing()
	{
		return current != null && current.key().equals(toldKey) ? current : null;
	}

	public void reset()
	{
		current = null;
		candidateKey = null;
		since = -1;
		toldKey = null;
	}
}
