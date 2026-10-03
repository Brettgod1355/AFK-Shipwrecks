/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

/**
 * Says, once, when a stronger crewmate sits idle while a weaker one works a hook (owner,
 * 2026-10-03). The pair has to stand for {@link #GRACE_MILLIS} first, since crew change jobs for
 * a moment as the player shuffles them, and a new pair is a new suggestion.
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

	private Suggestion standing;
	private long since = -1;
	private boolean told;

	/**
	 * Feeds this tick's best swap, or null when the hooks are as well crewed as they can be.
	 *
	 * @return the suggestion to announce now, or null
	 */
	public Suggestion update(long now, Suggestion current)
	{
		if (current == null)
		{
			reset();
			return null;
		}
		if (standing == null || !standing.key().equals(current.key()))
		{
			standing = current;
			since = now;
			told = false;
		}
		else
		{
			standing = current;
		}
		if (!told && now - since >= GRACE_MILLIS)
		{
			told = true;
			return current;
		}
		return null;
	}

	/** The swap already announced and still worth making, for the overlay; null otherwise. */
	public Suggestion standing()
	{
		return told ? standing : null;
	}

	public void reset()
	{
		standing = null;
		since = -1;
		told = false;
	}
}
