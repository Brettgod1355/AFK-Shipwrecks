/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

import java.util.Locale;
import java.util.Objects;

/**
 * A crewmate the player has recruited: who they are and how good a deckhand they are.
 * <p>
 * Deckhandiness normally comes from the game's crew table. When that cannot be read, the name is
 * looked up in a table copied from the wiki, and failing that a middling value is assumed.
 */
public final class Crewmate
{
	private final int uniqueId;
	private final String name;
	private final int deckhandiness;

	public Crewmate(int uniqueId, String name, int deckhandiness)
	{
		this.uniqueId = uniqueId;
		this.name = name == null ? "" : name;
		this.deckhandiness = deckhandiness;
	}

	/** The crewmate's id in the game's crew table, as the crew slot varbits report it. */
	public int getUniqueId()
	{
		return uniqueId;
	}

	public String getName()
	{
		return name;
	}

	/** 1 to 4, or 0 when unknown. */
	public int getDeckhandiness()
	{
		return deckhandiness;
	}

	/** The deckhandiness to calculate with: the known value, else the wiki's, else the default. */
	public int effectiveDeckhandiness()
	{
		if (deckhandiness >= 1 && deckhandiness <= 4)
		{
			return deckhandiness;
		}
		int known = knownDeckhandiness(name);
		return known > 0 ? known : SalvageRateModel.DEFAULT_DECKHANDINESS;
	}

	/** Deckhandiness of the named crewmates on the wiki, or 0 for anyone else. */
	public static int knownDeckhandiness(String name)
	{
		if (name == null)
		{
			return 0;
		}
		switch (name.trim().toLowerCase(Locale.ROOT))
		{
			case "cabin boy jenkins":
			case "jolly jim":
				return 4;
			case "jobless jim":
			case "ex-captain siad":
				return 3;
			case "oarswoman olga":
			case "bosun zarah":
			case "spotter virginia":
				return 2;
			case "adventurer ada":
			case "jittery jim":
			case "sailor jakob":
				return 1;
			default:
				return 0;
		}
	}

	/** A first name for tight overlay lines: "Cabin Boy Jenkins" becomes "Jenkins", "Jolly Jim" stays. */
	public String shortName()
	{
		String trimmed = name.trim();
		if (trimmed.isEmpty())
		{
			return "Crewmate";
		}
		String lower = trimmed.toLowerCase(Locale.ROOT);
		if (lower.startsWith("cabin boy ") || lower.startsWith("ex-captain ") || lower.startsWith("oarswoman ")
			|| lower.startsWith("bosun ") || lower.startsWith("spotter ") || lower.startsWith("sailor ")
			|| lower.startsWith("adventurer "))
		{
			return trimmed.substring(trimmed.lastIndexOf(' ') + 1);
		}
		return trimmed;
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o)
		{
			return true;
		}
		if (!(o instanceof Crewmate))
		{
			return false;
		}
		Crewmate other = (Crewmate) o;
		return uniqueId == other.uniqueId && deckhandiness == other.deckhandiness && name.equals(other.name);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(uniqueId, name, deckhandiness);
	}

	@Override
	public String toString()
	{
		return name + "(D" + effectiveDeckhandiness() + ")";
	}
}
