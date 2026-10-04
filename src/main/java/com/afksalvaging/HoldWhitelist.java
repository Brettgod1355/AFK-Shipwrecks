/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;
import net.runelite.api.gameval.ItemID;

/**
 * Which items the cargo hold accepts.
 * <p>
 * The game decides on the server and tells the client one inventory slot at a time: while the hold
 * is open, bit N of the varp {@code SAILING_BOAT_CARGOHOLD_SIDE_WHITELIST} is set when the item in
 * slot N can be deposited (the game's own side-panel script greys out the rest). This class learns
 * from that, and starts from the wiki's list of storable items so it is right before the hold has
 * ever been opened. What the game says always wins over the wiki list.
 * <p>
 * Noted items are never accepted (OSRS Wiki, "Cargo hold": "The cargo hold will not accept noted
 * items"); callers ask about the unnoted id and say no for a noted one. Courier crates, bounty items
 * and the unsorted salvage are storable too but are not seeded: they are long lists that do not come
 * from sorting salvage, and the game's answer covers them once the hold is opened with one aboard.
 */
public final class HoldWhitelist
{
	/**
	 * The OSRS Wiki's "Storable items" for the cargo hold, plus the ship cannonballs and repair kits
	 * its table links to (pages "Cargo hold", "Cannonball" and "Repair kits", read 2026-10-02, item
	 * ids from the wiki's infobox_item bucket and checked against RuneLite 1.13.1's ItemID). OSRS
	 * Wiki, CC BY-NC-SA 3.0.
	 */
	static final Set<Integer> SEED = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
		// Sailing kit. The log, spyglass, duck, crowbar and diving gear take no space in the hold.
		ItemID.SKILLCAPE_SAILING, ItemID.SKILLCAPE_SAILING_TRIMMED, ItemID.SKILLCAPE_SAILING_HOOD,
		ItemID.SAILING_LOG_INITIAL, ItemID.SAILING_LOG, ItemID.SAILING_CHARTING_SPYGLASS,
		ItemID.SAILING_CHARTING_CURRENT_DUCK, ItemID.SAILING_CHARTING_CROWBAR,
		ItemID.HUNDRED_PIRATE_DIVING_HELMET, ItemID.HUNDRED_PIRATE_DIVING_BACKPACK,
		// Repair kits.
		ItemID.BOAT_REPAIR_KIT, ItemID.BOAT_REPAIR_KIT_OAK, ItemID.BOAT_REPAIR_KIT_TEAK,
		ItemID.BOAT_REPAIR_KIT_MAHOGANY, ItemID.BOAT_REPAIR_KIT_CAMPHOR, ItemID.BOAT_REPAIR_KIT_IRONWOOD,
		ItemID.BOAT_REPAIR_KIT_ROSEWOOD,
		// Ship cannonballs: regular (steel is the multicannon's MCANNONBALL), granite, chainshot, incendiary.
		// Kept in step with CANNONBALLS below; HoldWhitelistTest checks that every one is here.
		ItemID.BRONZE_CANNONBALL, ItemID.IRON_CANNONBALL, ItemID.MCANNONBALL, ItemID.MITHRIL_CANNONBALL,
		ItemID.ADAMANT_CANNONBALL, ItemID.RUNE_CANNONBALL, ItemID.DRAGON_CANNONBALL, ItemID.GRANITE_CANNONBALL,
		ItemID.BRONZE_CHAINSHOT_CANNONBALL, ItemID.IRON_CHAINSHOT_CANNONBALL, ItemID.STEEL_CHAINSHOT_CANNONBALL,
		ItemID.MITHRIL_CHAINSHOT_CANNONBALL, ItemID.ADAMANT_CHAINSHOT_CANNONBALL, ItemID.RUNE_CHAINSHOT_CANNONBALL,
		ItemID.BRONZE_INCENDIARY_CANNONBALL, ItemID.IRON_INCENDIARY_CANNONBALL, ItemID.STEEL_INCENDIARY_CANNONBALL,
		ItemID.MITHRIL_INCENDIARY_CANNONBALL, ItemID.ADAMANT_INCENDIARY_CANNONBALL, ItemID.RUNE_INCENDIARY_CANNONBALL,
		// The dragon chainshot and incendiary are in the game's item list but not on the wiki's table.
		ItemID.DRAGON_CHAINSHOT_CANNONBALL, ItemID.DRAGON_INCENDIARY_CANNONBALL,
		// Drinks.
		ItemID.BEER_GLASS, ItemID.GROG, ItemID.CIDER, ItemID.WHIRLPOOL_SURPRISE, ItemID.KRAKEN_INK_STOUT,
		ItemID.PERILDANCE_BITTER, ItemID.TRAWLERS_TRUST, ItemID.HORIZONS_LURE,
		// Fishing from the boat.
		ItemID.BRUT_FISH_CUTS, ItemID.SAILING_FINE_FISH_OFFCUTS, ItemID.CAMPHOR_CRATE, ItemID.SLAYER_ICY_WATER,
		ItemID.FISH_CRATE_EMPTY,
		ItemID.RAW_GIANT_KRILL, ItemID.POH_TROPHYDROP_GIANT_KRILL, ItemID.FISH_CRATE_GIANT_KRILL, ItemID.FISH_CRATE_GIANT_KRILL_VAR,
		ItemID.RAW_HADDOCK, ItemID.POH_TROPHYDROP_HADDOCK, ItemID.FISH_CRATE_HADDOCK, ItemID.FISH_CRATE_HADDOCK_VAR,
		ItemID.RAW_YELLOWFIN, ItemID.POH_TROPHYDROP_YELLOWFIN, ItemID.FISH_CRATE_YELLOWFIN, ItemID.FISH_CRATE_YELLOWFIN_VAR,
		ItemID.RAW_HALIBUT, ItemID.POH_TROPHYDROP_HALIBUT, ItemID.FISH_CRATE_HALIBUT, ItemID.FISH_CRATE_HALIBUT_VAR,
		ItemID.RAW_BLUEFIN, ItemID.POH_TROPHYDROP_BLUEFIN, ItemID.FISH_CRATE_BLUEFIN, ItemID.FISH_CRATE_BLUEFIN_VAR,
		ItemID.RAW_MARLIN, ItemID.POH_TROPHYDROP_MARLIN, ItemID.FISH_CRATE_MARLIN, ItemID.FISH_CRATE_MARLIN_VAR,
		// Fishing gear and fish (game update of 2026-08-05).
		ItemID.NET, ItemID.BIG_NET, ItemID.FISHING_ROD, ItemID.FISHING_BAIT, ItemID.LOBSTER_POT, ItemID.HARPOON,
		ItemID.TBWT_KARAMBWAN_VESSEL, ItemID.TBWT_KARAMBWAN_VESSEL_LOADED_WITH_KARAMBWANJI,
		ItemID.TBWT_RAW_KARAMBWANJI, ItemID.PISCARILIUS_SANDWORMS,
		ItemID.RAW_SHRIMP, ItemID.SHRIMP, ItemID.RAW_ANCHOVIES, ItemID.ANCHOVIES, ItemID.RAW_SARDINE, ItemID.SARDINE,
		ItemID.RAW_HERRING, ItemID.HERRING, ItemID.RAW_MACKEREL, ItemID.MACKEREL, ItemID.RAW_COD, ItemID.COD,
		ItemID.RAW_BASS, ItemID.BASS, ItemID.RAW_TUNA, ItemID.TUNA, ItemID.RAW_SWORDFISH, ItemID.SWORDFISH,
		ItemID.RAW_LOBSTER, ItemID.LOBSTER, ItemID.TBWT_RAW_KARAMBWAN, ItemID.TBWT_COOKED_KARAMBWAN,
		ItemID.RAW_ANGLERFISH, ItemID.ANGLERFISH, ItemID.RAW_MONKFISH, ItemID.MONKFISH, ItemID.RAW_SHARK, ItemID.SHARK
	)));

	/**
	 * Every ship cannonball: the only items the Deposit box goes on by default (owner, 2026-10-02:
	 * "just all the cballs by default"). The rest of {@link #SEED} keeps its items from being
	 * called drops, but they are only deposits when the player marks them.
	 */
	static final Set<Integer> CANNONBALLS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
		ItemID.BRONZE_CANNONBALL, ItemID.IRON_CANNONBALL, ItemID.MCANNONBALL, ItemID.MITHRIL_CANNONBALL,
		ItemID.ADAMANT_CANNONBALL, ItemID.RUNE_CANNONBALL, ItemID.DRAGON_CANNONBALL, ItemID.GRANITE_CANNONBALL,
		ItemID.BRONZE_CHAINSHOT_CANNONBALL, ItemID.IRON_CHAINSHOT_CANNONBALL, ItemID.STEEL_CHAINSHOT_CANNONBALL,
		ItemID.MITHRIL_CHAINSHOT_CANNONBALL, ItemID.ADAMANT_CHAINSHOT_CANNONBALL, ItemID.RUNE_CHAINSHOT_CANNONBALL,
		ItemID.DRAGON_CHAINSHOT_CANNONBALL,
		ItemID.BRONZE_INCENDIARY_CANNONBALL, ItemID.IRON_INCENDIARY_CANNONBALL, ItemID.STEEL_INCENDIARY_CANNONBALL,
		ItemID.MITHRIL_INCENDIARY_CANNONBALL, ItemID.ADAMANT_INCENDIARY_CANNONBALL, ItemID.RUNE_INCENDIARY_CANNONBALL,
		ItemID.DRAGON_INCENDIARY_CANNONBALL
	)));

	/**
	 * The cannonballs salvage actually turns up (owner, 2026-10-03: "we only get normal
	 * steel-dragon from salvage, not any incendiary or chainshot"): the plain ones from steel (the
	 * multicannon's MCANNONBALL) to dragon. These are Deposit by default; the hold takes the rest
	 * of {@link #CANNONBALLS} too, but they get no box unless marked.
	 */
	static final Set<Integer> DEFAULT_DEPOSITS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
		ItemID.MCANNONBALL, ItemID.MITHRIL_CANNONBALL, ItemID.ADAMANT_CANNONBALL, ItemID.RUNE_CANNONBALL, ItemID.DRAGON_CANNONBALL
	)));

	/** Whether this unnoted item is a Deposit by default: a plain steel to dragon cannonball. */
	public static boolean isDefaultDeposit(int itemId)
	{
		return DEFAULT_DEPOSITS.contains(itemId);
	}

	private final Set<Integer> accepted = new LinkedHashSet<>();
	private final Set<Integer> refused = new LinkedHashSet<>();
	private int[] lastSlots;
	private int lastBits;

	/**
	 * Whether the hold takes this item, by its unnoted id: what the game last said, else the wiki
	 * list.
	 */
	public boolean takes(int itemId)
	{
		if (accepted.contains(itemId))
		{
			return true;
		}
		if (refused.contains(itemId))
		{
			return false;
		}
		return SEED.contains(itemId);
	}

	/**
	 * One look at the inventory while the hold is open. A pair is learned only when it has held for
	 * two looks in a row, so an inventory that changed a tick before the game's answer does not
	 * teach the wrong thing. Refusals are only learned while the hold has room, in case a full hold
	 * refuses everything.
	 *
	 * @param slotItemIds the unnoted item id in each inventory slot, or -1 for an empty slot or a
	 *                    noted item (a noted item says nothing about the item itself)
	 * @param bits        the whitelist varp
	 * @param holdHasRoom whether the hold is known to have a free slot
	 * @return whether anything new was learned
	 */
	public boolean observe(int[] slotItemIds, int bits, boolean holdHasRoom)
	{
		boolean stable = lastSlots != null && lastBits == bits && Arrays.equals(lastSlots, slotItemIds);
		lastSlots = slotItemIds.clone();
		lastBits = bits;
		if (!stable)
		{
			return false;
		}
		boolean changed = false;
		for (int slot = 0; slot < slotItemIds.length && slot < Integer.SIZE; slot++)
		{
			int id = slotItemIds[slot];
			if (id < 0)
			{
				continue;
			}
			if ((bits & (1 << slot)) != 0)
			{
				changed |= accepted.add(id);
				changed |= refused.remove(id);
			}
			else if (holdHasRoom && !accepted.contains(id))
			{
				changed |= refused.add(id);
			}
		}
		return changed;
	}

	/** Forgets the half-seen look, for example when the hold closes. */
	public void resetObservation()
	{
		lastSlots = null;
	}

	public String encodeAccepted()
	{
		return encode(accepted);
	}

	public String encodeRefused()
	{
		return encode(refused);
	}

	/** Restores what was learned; anything unreadable is dropped rather than failing. */
	public void decode(String acceptedIds, String refusedIds)
	{
		accepted.clear();
		refused.clear();
		decodeInto(acceptedIds, accepted);
		decodeInto(refusedIds, refused);
		refused.removeAll(accepted);
	}

	private static String encode(Set<Integer> ids)
	{
		StringBuilder out = new StringBuilder();
		for (int id : new TreeSet<>(ids))
		{
			out.append(out.length() == 0 ? "" : ",").append(id);
		}
		return out.toString();
	}

	private static void decodeInto(String stored, Set<Integer> into)
	{
		if (stored == null || stored.isEmpty())
		{
			return;
		}
		for (String part : stored.split(","))
		{
			try
			{
				into.add(Integer.parseInt(part.trim()));
			}
			catch (NumberFormatException ignored)
			{
				// A hand-edited setting; skip the bad entry.
			}
		}
	}
}
