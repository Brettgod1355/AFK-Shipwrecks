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
	/**
	 * The few things the marks need from RuneLite's settings, so the rules here can be tested
	 * without a running client (review, 2026-10-04: none of this was tested).
	 */
	interface Settings
	{
		/** The character logged in, by the key their settings are filed under, or null. */
		String ownProfile();

		/** Every character RuneLite knows, by that key. */
		List<String> profiles();

		/** A plugin-wide setting, from before the marks were kept per character. */
		String get(String key);

		String get(String profile, String key);

		void set(String profile, String key, String value);

		void unset(String key);

		void unset(String profile, String key);
	}

	private final Settings settings;

	MarkStore(ConfigManager configManager)
	{
		this(new Settings()
		{
			@Override
			public String ownProfile()
			{
				return configManager.getRSProfileKey();
			}

			/**
			 * The per-character settings live in RuneLite's own store, not the plugin's, so
			 * {@code getConfigurationKeys} never sees them (an adversarial review caught the first
			 * version using it, 2026-10-03); {@code getRSProfiles} is the list to ask.
			 */
			@Override
			public List<String> profiles()
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

			@Override
			public String get(String key)
			{
				return configManager.getConfiguration(AfkSalvagingConfig.GROUP, key);
			}

			@Override
			public String get(String profile, String key)
			{
				return configManager.getConfiguration(AfkSalvagingConfig.GROUP, profile, key);
			}

			@Override
			public void set(String profile, String key, String value)
			{
				configManager.setConfiguration(AfkSalvagingConfig.GROUP, profile, key, value);
			}

			@Override
			public void unset(String key)
			{
				configManager.unsetConfiguration(AfkSalvagingConfig.GROUP, key);
			}

			@Override
			public void unset(String profile, String key)
			{
				configManager.unsetConfiguration(AfkSalvagingConfig.GROUP, profile, key);
			}
		});
	}

	MarkStore(Settings settings)
	{
		this.settings = settings;
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
		String own = settings.ownProfile();
		if (own != null)
		{
			read(lists, own);
		}
		for (String profile : settings.profiles())
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
			into.add(rule, SalvageSorter.Lists.parse(settings.get(profile, keyFor(rule))));
		}
		into.addExcluded(SalvageSorter.Lists.parse(settings.get(profile, EXCLUDED_KEY)));
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
		String own = settings.ownProfile();
		if (own == null)
		{
			return false;
		}
		Set<String> profiles = new LinkedHashSet<>(settings.profiles());
		profiles.add(own);
		for (String profile : profiles)
		{
			for (String key : allKeys())
			{
				List<Integer> ids = new ArrayList<>(SalvageSorter.Lists.parse(settings.get(profile, key)));
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
		String own = settings.ownProfile();
		if (own == null)
		{
			return false;
		}
		boolean moved = false;
		SalvageSorter.Lists everyone = load();
		for (SortRule rule : SortRule.values())
		{
			String key = keyFor(rule);
			String old = settings.get(key);
			if (old == null)
			{
				continue;
			}
			List<Integer> ids = new ArrayList<>(SalvageSorter.Lists.parse(settings.get(own, key)));
			for (int id : SalvageSorter.Lists.parse(old))
			{
				if (everyone.markOf(id) == null && !ids.contains(id))
				{
					ids.add(id);
				}
			}
			save(own, key, ids);
			settings.unset(key);
			moved = true;
		}
		return moved;
	}

	private void save(String profile, String key, List<Integer> ids)
	{
		if (ids.isEmpty())
		{
			settings.unset(profile, key);
		}
		else
		{
			settings.set(profile, key, SalvageSorter.Lists.join(ids));
		}
	}
}
