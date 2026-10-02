/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

import net.runelite.api.gameval.ItemID;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class HoldWhitelistTest
{
	private static final int RUNE = ItemID.AIRRUNE;
	private static final int KIT = ItemID.BOAT_REPAIR_KIT;
	private static final int CRATE = ItemID.FISH_CRATE_EMPTY;

	@Test
	public void theWikiListAnswersBeforeTheHoldIsOpened()
	{
		HoldWhitelist hold = new HoldWhitelist();
		assertTrue(hold.takes(KIT));
		assertTrue(hold.takes(ItemID.MCANNONBALL));
		assertTrue(hold.takes(ItemID.SAILING_LOG));
		assertTrue(hold.takes(ItemID.RAW_SHARK));
		assertFalse(hold.takes(RUNE));
		assertFalse(hold.takes(ItemID.COINS));
	}

	@Test
	public void theSeedListHoldsEveryWikiItem()
	{
		// 76 named items with their versions (85 ids), 22 ship cannonballs (granite and the two dragon
		// specials included), 7 repair kits.
		assertEquals(114, HoldWhitelist.SEED.size());
	}

	@Test
	public void everyShipCannonballIsADeposit()
	{
		HoldWhitelist hold = new HoldWhitelist();
		int[] cannonballs = {
			ItemID.BRONZE_CANNONBALL, ItemID.IRON_CANNONBALL, ItemID.MCANNONBALL, ItemID.MITHRIL_CANNONBALL,
			ItemID.ADAMANT_CANNONBALL, ItemID.RUNE_CANNONBALL, ItemID.DRAGON_CANNONBALL, ItemID.GRANITE_CANNONBALL,
			ItemID.BRONZE_CHAINSHOT_CANNONBALL, ItemID.IRON_CHAINSHOT_CANNONBALL, ItemID.STEEL_CHAINSHOT_CANNONBALL,
			ItemID.MITHRIL_CHAINSHOT_CANNONBALL, ItemID.ADAMANT_CHAINSHOT_CANNONBALL, ItemID.RUNE_CHAINSHOT_CANNONBALL,
			ItemID.DRAGON_CHAINSHOT_CANNONBALL,
			ItemID.BRONZE_INCENDIARY_CANNONBALL, ItemID.IRON_INCENDIARY_CANNONBALL, ItemID.STEEL_INCENDIARY_CANNONBALL,
			ItemID.MITHRIL_INCENDIARY_CANNONBALL, ItemID.ADAMANT_INCENDIARY_CANNONBALL, ItemID.RUNE_INCENDIARY_CANNONBALL,
			ItemID.DRAGON_INCENDIARY_CANNONBALL,
		};
		for (int id : cannonballs)
		{
			assertTrue("cannonball " + id, hold.takes(id));
			assertTrue("cannonball " + id, HoldWhitelist.isCannonball(id));
		}
		assertEquals(cannonballs.length, HoldWhitelist.CANNONBALLS.size());
		assertTrue("every cannonball is also in the seed", HoldWhitelist.SEED.containsAll(HoldWhitelist.CANNONBALLS));
		assertFalse("a repair kit is taken but is no cannonball", HoldWhitelist.isCannonball(KIT));
	}

	@Test
	public void learnsOnlyOnceTheGameHasAnsweredTwiceAlike()
	{
		HoldWhitelist hold = new HoldWhitelist();
		int[] slots = {RUNE, KIT, -1};
		// Slot 0 accepted, slot 1 refused: the opposite of the wiki list.
		assertFalse("one look is not enough", hold.observe(slots, 0b001, true));
		assertFalse(hold.takes(RUNE));
		assertTrue(hold.observe(slots, 0b001, true));
		assertTrue(hold.takes(RUNE));
		assertFalse("the game wins over the wiki", hold.takes(KIT));
		assertFalse("nothing new the third time", hold.observe(slots, 0b001, true));
	}

	@Test
	public void aChangeBetweenLooksStartsOver()
	{
		HoldWhitelist hold = new HoldWhitelist();
		hold.observe(new int[]{RUNE}, 0b0, true);
		assertFalse("the inventory moved: wait", hold.observe(new int[]{CRATE}, 0b0, true));
		assertTrue(hold.takes(CRATE));
		hold.resetObservation();
		assertFalse("closing the hold starts over too", hold.observe(new int[]{CRATE}, 0b0, true));
	}

	@Test
	public void refusalsAreNotLearnedWhileTheHoldMayBeFull()
	{
		HoldWhitelist hold = new HoldWhitelist();
		int[] slots = {KIT};
		hold.observe(slots, 0, false);
		assertFalse(hold.observe(slots, 0, false));
		assertTrue("a full hold refusing it proves nothing", hold.takes(KIT));
	}

	@Test
	public void anAcceptanceOutranksAnEarlierRefusal()
	{
		HoldWhitelist hold = new HoldWhitelist();
		int[] slots = {RUNE};
		hold.observe(slots, 0, true);
		hold.observe(slots, 0, true);
		assertFalse(hold.takes(RUNE));
		hold.observe(slots, 1, true);
		hold.observe(slots, 1, true);
		assertTrue(hold.takes(RUNE));
		assertEquals("", hold.encodeRefused());
	}

	@Test
	public void whatWasLearnedSurvivesStorage()
	{
		HoldWhitelist hold = new HoldWhitelist();
		hold.observe(new int[]{RUNE, KIT}, 0b01, true);
		hold.observe(new int[]{RUNE, KIT}, 0b01, true);
		HoldWhitelist restored = new HoldWhitelist();
		restored.decode(hold.encodeAccepted(), hold.encodeRefused() + ",junk");
		assertTrue(restored.takes(RUNE));
		assertFalse(restored.takes(KIT));
		restored.decode(String.valueOf(KIT), String.valueOf(KIT));
		assertTrue("listed in both: accepted wins", restored.takes(KIT));
	}
}
