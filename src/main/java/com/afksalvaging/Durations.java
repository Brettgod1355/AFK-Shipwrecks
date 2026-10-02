/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Formats the times the overlay shows. */
public final class Durations
{
	private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT);

	private Durations()
	{
	}

	/**
	 * {@code m:ss} under an hour, {@code h:mm:ss} from an hour up. Negative values read as zero
	 * and the value is rounded up so a countdown only shows 0:00 when it has really run out.
	 */
	public static String countdown(long millis)
	{
		long seconds = (Math.max(0, millis) + 999) / 1000;
		long hours = seconds / 3600;
		long minutes = (seconds % 3600) / 60;
		long rest = seconds % 60;
		if (hours > 0)
		{
			return String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, rest);
		}
		return String.format(Locale.ROOT, "%d:%02d", minutes, rest);
	}

	/** A short rounded form for messages: "about 42 minutes", "about 1 hour 5 minutes", "under a minute". */
	public static String spoken(long millis)
	{
		long minutes = (Math.max(0, millis) + 30_000) / 60_000;
		if (minutes < 1)
		{
			return "under a minute";
		}
		long hours = minutes / 60;
		long rest = minutes % 60;
		if (hours == 0)
		{
			return "about " + minutes + (minutes == 1 ? " minute" : " minutes");
		}
		String text = "about " + hours + (hours == 1 ? " hour" : " hours");
		if (rest > 0)
		{
			text += " " + rest + (rest == 1 ? " minute" : " minutes");
		}
		return text;
	}

	/** Under this the countdown shows seconds; above it, minutes. */
	public static final long FINE_MILLIS = 10 * 60_000L;

	/**
	 * Seconds when it is close, whole minutes when it is not, because the estimate is not that
	 * precise: {@code 4:31}, {@code 52 min}, {@code 1 h}, {@code 1 h 30 min}. A negative value is "?".
	 */
	public static String coarse(long millis)
	{
		if (millis < 0)
		{
			return "?";
		}
		if (millis < FINE_MILLIS)
		{
			return countdown(millis);
		}
		long minutes = (millis + 30_000) / 60_000;
		long hours = minutes / 60;
		long rest = minutes % 60;
		if (hours == 0)
		{
			return minutes + " min";
		}
		return rest > 0 ? hours + " h " + rest + " min" : hours + " h";
	}

	/**
	 * The shortest form, for an infobox: {@code 4:31} under ten minutes, then {@code 52m},
	 * {@code 1h30}, {@code 2h}. A negative value is "?".
	 */
	public static String tiny(long millis)
	{
		if (millis < 0)
		{
			return "?";
		}
		if (millis < FINE_MILLIS)
		{
			return countdown(millis);
		}
		long minutes = (millis + 30_000) / 60_000;
		long hours = minutes / 60;
		long rest = minutes % 60;
		if (hours == 0)
		{
			return minutes + "m";
		}
		return rest > 0 ? String.format(Locale.ROOT, "%dh%02d", hours, rest) : hours + "h";
	}

	/** The local wall-clock time this many milliseconds after {@code now}, as {@code HH:mm}. */
	public static String clockAfter(long now, long millis, ZoneId zone)
	{
		return CLOCK.format(Instant.ofEpochMilli(now + Math.max(0, millis)).atZone(zone));
	}
}
