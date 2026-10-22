/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

/**
 * Says when a stronger crewmate sits idle while a weaker one works a hook (owner, 2026-10-03). The
 * pair has to stand for {@link #GRACE_MILLIS} first, since crew change jobs for a moment as the
 * player shuffles them. It is said once each time it happens: once the swap has been gone for
 * {@link #FORGET_MILLIS} (made, or the player stepped off to sort), the same pair coming back is
 * said again (owner, 2026-10-22: "if someone ignores it once they might not realize they are
 * losing out on xp"). A different pair is always news.
 */
public final class CrewSwapWatch
{
	public static final long GRACE_MILLIS = 10_000;
	/** How long a swap must be gone before the same pair counts as happening again. */
	public static final long FORGET_MILLIS = 15_000;

	/** Put {@code in} on the hook instead of {@code out}. */
	public static final class Suggestion
	{
		public final Crewmate out;
		public final Crewmate in;
		public final int outDeckhandiness;
		public final int inDeckhandiness;
		/** The player is on the hook {@code in} was working: they took it from them. */
		public final boolean playerTookTheirHook;

		public Suggestion(Crewmate out, Crewmate in, int outDeckhandiness, int inDeckhandiness)
		{
			this(out, in, outDeckhandiness, inDeckhandiness, false);
		}

		public Suggestion(Crewmate out, Crewmate in, int outDeckhandiness, int inDeckhandiness, boolean playerTookTheirHook)
		{
			this.out = out;
			this.in = in;
			this.outDeckhandiness = outDeckhandiness;
			this.inDeckhandiness = inDeckhandiness;
			this.playerTookTheirHook = playerTookTheirHook;
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
	/** The pair last announced; it is not announced again while it stands. */
	private String toldKey;
	/** When the last swap went away, or -1 while one stands. */
	private long goneSince = -1;

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
			if (goneSince < 0)
			{
				goneSince = now;
			}
			else if (now - goneSince >= FORGET_MILLIS)
			{
				toldKey = null;
			}
			return null;
		}
		goneSince = -1;
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

	/**
	 * Forgets the swap in the making but remembers the one already pointed out: after a world hop
	 * the crew are the same, so the same advice would only repeat itself (review, 2026-10-04).
	 */
	public void forgetPending()
	{
		current = null;
		candidateKey = null;
		since = -1;
	}

	public void reset()
	{
		current = null;
		candidateKey = null;
		since = -1;
		toldKey = null;
		goneSince = -1;
	}
}
