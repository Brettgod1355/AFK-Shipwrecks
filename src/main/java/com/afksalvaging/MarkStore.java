/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.config.RuneScapeProfile;

/**
 * Where the Keep / Deposit / Alch / Drop marks live. Each character's marks are kept under their
 * own id (RuneLite's RS profile, which follows the account's Jagex id, not its name), and the lists
 * the plugin works from are every character's folded together. So the marks are shared, as the
 * owner wants, without two clients open at once overwriting each other, which one shared setting
 * did: RuneLite only downloads settings at start-up and the last client to save won (2026-10-03).
 * An item is in at most one list across all characters: marking it anywhere takes it out of
 * everyone else's lists.
 */
final class MarkStore
{
	private final ConfigManager configManager;

	MarkStore(ConfigManager configManager)
	{
		this.configManager = configManager;
	}

	/** The setting one list is kept under, the same name whichever character it belongs to. */
	static String keyFor(SortRule rule)
	{
		switch (rule)
		{
			case KEEP:
				return "sortKeepIds";
			case HOLD:
				return "sortHoldIds";
			case ALCH:
				return "sortAlchIds";
			default:
				return "sortDropIds";
		}
	}

	/** The setting the items taken out of the defaults are kept under. */
	static final String EXCLUDED_KEY = "sortNoneIds";

	/** Every key an item can be filed under: the four lists and the excluded set. */
	static List<String> allKeys()
	{
		List<String> keys = new ArrayList<>();
		for (SortRule rule : SortRule.values())
		{
			keys.add(keyFor(rule));
		}
		keys.add(EXCLUDED_KEY);
		return keys;
	}

	/** Every character's lists folded into one; where two characters disagree, the one logged in wins. */
	SalvageSorter.Lists load()
	{
		SalvageSorter.Lists lists = new SalvageSorter.Lists();
		String own = configManager.getRSProfileKey();
		if (own != null)
		{
			read(lists, own);
		}
		for (String profile : profiles())
		{
			if (!profile.equals(own))
			{
				read(lists, profile);
			}
		}
		return lists;
	}

	private void read(SalvageSorter.Lists into, String profile)
	{
		for (SortRule rule : SortRule.values())
		{
			into.add(rule, SalvageSorter.Lists.parse(configManager.getConfiguration(AfkSalvagingConfig.GROUP, profile, keyFor(rule))));
		}
		into.addExcluded(SalvageSorter.Lists.parse(configManager.getConfiguration(AfkSalvagingConfig.GROUP, profile, EXCLUDED_KEY)));
	}

	/**
	 * Marks an item for the character logged in (null unmarks it), taking it out of every
	 * character's other lists and off their excluded sets. Does nothing when nobody is logged in,
	 * since there is no one to file the mark under.
	 *
	 * @return whether anything was saved
	 */
	boolean mark(int itemId, SortRule rule)
	{
		return file(itemId, rule == null ? null : keyFor(rule));
	}

	/**
	 * Takes an item out of the defaults for the character logged in: no box until it is marked
	 * again. Same rules as {@link #mark}.
	 */
	boolean exclude(int itemId)
	{
		return file(itemId, EXCLUDED_KEY);
	}

	/** Puts the item under one key of the character logged in (null: nowhere) and nowhere else, for anyone. */
	private boolean file(int itemId, String targetKey)
	{
		String own = configManager.getRSProfileKey();
		if (own == null)
		{
			return false;
		}
		Set<String> profiles = new LinkedHashSet<>(profiles());
		profiles.add(own);
		for (String profile : profiles)
		{
			for (String key : allKeys())
			{
				List<Integer> ids = new ArrayList<>(SalvageSorter.Lists.parse(configManager.getConfiguration(AfkSalvagingConfig.GROUP, profile, key)));
				boolean changed = ids.remove(Integer.valueOf(itemId));
				if (profile.equals(own) && key.equals(targetKey))
				{
					ids.add(itemId);
					changed = true;
				}
				if (changed)
				{
					save(profile, key, ids);
				}
			}
		}
		return true;
	}

	/**
	 * Moves the lists the plugin kept plugin-wide before 2026-10-03 under the character logged
	 * in, so nothing already marked is lost. Items another character already has stay theirs.
	 *
	 * @return whether there was anything to move
	 */
	boolean migrate()
	{
		String own = configManager.getRSProfileKey();
		if (own == null)
		{
			return false;
		}
		boolean moved = false;
		SalvageSorter.Lists everyone = load();
		for (SortRule rule : SortRule.values())
		{
			String key = keyFor(rule);
			String old = configManager.getConfiguration(AfkSalvagingConfig.GROUP, key);
			if (old == null)
			{
				continue;
			}
			List<Integer> ids = new ArrayList<>(SalvageSorter.Lists.parse(configManager.getConfiguration(AfkSalvagingConfig.GROUP, own, key)));
			for (int id : SalvageSorter.Lists.parse(old))
			{
				if (everyone.markOf(id) == null && !ids.contains(id))
				{
					ids.add(id);
				}
			}
			save(own, key, ids);
			configManager.unsetConfiguration(AfkSalvagingConfig.GROUP, key);
			moved = true;
		}
		return moved;
	}

	private void save(String profile, String key, List<Integer> ids)
	{
		if (ids.isEmpty())
		{
			configManager.unsetConfiguration(AfkSalvagingConfig.GROUP, profile, key);
		}
		else
		{
			configManager.setConfiguration(AfkSalvagingConfig.GROUP, profile, key, SalvageSorter.Lists.join(ids));
		}
	}

	/**
	 * Every character RuneLite knows, by the key its settings are filed under. The per-character
	 * settings live in RuneLite's own store, not the plugin's, so {@code getConfigurationKeys}
	 * never sees them (an adversarial review caught the first version using it, 2026-10-03);
	 * {@code getRSProfiles} is the list to ask.
	 */
	private List<String> profiles()
	{
		List<String> profiles = new ArrayList<>();
		for (RuneScapeProfile profile : configManager.getRSProfiles())
		{
			if (profile.getKey() != null && !profiles.contains(profile.getKey()))
			{
				profiles.add(profile.getKey());
			}
		}
		return profiles;
	}
}
