/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import net.runelite.api.gameval.ItemID;

/**
 * Salvage most players drop, picked by the owner from the Old School RuneScape Wiki's shipwreck
 * salvage tables (2026-10-04). Never applied by itself: a player who wants it presses "Add
 * suggested drops" in the Sorting tab, and the items become ordinary Drop marks they can change
 * one by one (owner: "some people might not want everything to be on by default that i think is
 * junk"). Each id was matched by its exact name in the game cache, plain item over quest copies.
 */
final class SuggestedDrops
{
	/** In the order of the wiki tables the owner took them from; each item once (a test checks). */
	static final Integer[] LISTED = {
		// Materials.
		ItemID.BRONZE_BAR, ItemID.NAILS_BRONZE, ItemID.LOGS, ItemID.NAILS_IRON, ItemID.WOODPLANK, ItemID.OAK_LOGS,
		ItemID.SAWMILL_COUPON, ItemID.IRON_BAR, ItemID.PLANK_OAK, ItemID.SAWMILL_COUPON_OAK, ItemID.STEEL_BAR,
		ItemID.NAILS, ItemID.SWAMPPASTE, ItemID.TEAK_LOGS, ItemID.BOAT_REPAIR_KIT_TEAK, ItemID.ROPE,
		ItemID.COPPER_ORE, ItemID.PLANK_TEAK, ItemID.LEAD_ORE, ItemID.MAHOGANY_LOGS, ItemID.CAMPHOR_LOGS,
		ItemID.IRONWOOD_LOGS,
		// Runes and ammunition.
		ItemID.AIRRUNE, ItemID.WATERRUNE, ItemID.STEEL_ARROW,
		// Fishing.
		ItemID.FEATHER, ItemID.FISHING_BAIT, ItemID.RAW_LOBSTER, ItemID.RAW_MACKEREL, ItemID.RAW_SWORDFISH,
		ItemID.FISHING_ROD, ItemID.HARPOON, ItemID.LOBSTER_POT, ItemID.RAW_SHARK, ItemID.RAW_SALMON,
		ItemID.RAW_TUNA, ItemID.RAW_MONKFISH,
		// The sea floor.
		ItemID.SEAWEED, ItemID.GIANT_SEAWEED, ItemID.FLAX_SEED, ItemID.CORAL_ELKHORN_FRAG,
		ItemID.SMALLOYSTERPEARLS, ItemID.BIGOYSTERPEARLS, ItemID.CASKET,
		// Food and drink.
		ItemID.RUM, ItemID.BANANA,
		// Jewellery and weapons.
		ItemID.GOLD_RING, ItemID.SAPPHIRE_RING, ItemID.EMERALD_RING, ItemID.JEWL_EMERALD_BRACELET,
		ItemID.MITHRIL_SCIMITAR, ItemID.MITHRIL_LONGSWORD
	};

	static final Set<Integer> ITEMS = Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList(LISTED)));

	private SuggestedDrops()
	{
	}
}
