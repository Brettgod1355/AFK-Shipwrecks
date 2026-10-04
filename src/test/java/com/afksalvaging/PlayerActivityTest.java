/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

import net.runelite.api.gameval.AnimationID;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PlayerActivityTest
{
	@Test
	public void classifiesAnimations()
	{
		assertEquals(PlayerActivity.SALVAGING, PlayerActivity.fromAnimation(AnimationID.SAILING_HUMAN_SALVAGE_HOOK_KANDARIN_3X8_DROP01));
		assertEquals(PlayerActivity.SALVAGING, PlayerActivity.fromAnimation(AnimationID.HUMAN_SAILING_SALVAGE01_LARGE01_IDLE01));
		assertEquals(PlayerActivity.SORTING, PlayerActivity.fromAnimation(AnimationID.HUMAN_SAILING_SALVAGE01_LARGE01_INTERACT01));
		assertEquals(PlayerActivity.SORTING, PlayerActivity.fromAnimation(AnimationID.SAILING_HUMAN_SALVAGE_HOOK_KANDARIN_1X3_INTERACT01));
		assertEquals(PlayerActivity.AT_HOOK_IDLE, PlayerActivity.fromAnimation(AnimationID.SAILING_HUMAN_SALVAGE_HOOK_KANDARIN_1X3_INACTIVE01));
		assertTrue(PlayerActivity.AT_HOOK_IDLE.isAtHook());
		assertTrue(PlayerActivity.SALVAGING.isAtHook());
		assertFalse(PlayerActivity.SORTING.isAtHook());
		assertFalse(PlayerActivity.IDLE.isAtHook());
		assertEquals(PlayerActivity.IDLE, PlayerActivity.fromAnimation(-1));
		assertEquals(PlayerActivity.IDLE, PlayerActivity.fromAnimation(AnimationID.HUMAN_SAILING_HELM_3X8_ACTIVE01));
	}

	@Test
	public void holdsTheActivityAcrossShortGaps()
	{
		PlayerActivity.Detector detector = new PlayerActivity.Detector();
		assertEquals(PlayerActivity.IDLE, detector.update(-1, 0));
		assertEquals(PlayerActivity.SALVAGING, detector.update(AnimationID.SAILING_HUMAN_SALVAGE_HOOK_KANDARIN_2X5_DROP01, 1));
		assertEquals(PlayerActivity.SALVAGING, detector.update(-1, 2));
		assertEquals(PlayerActivity.SALVAGING, detector.update(-1, 4));
		assertEquals(PlayerActivity.IDLE, detector.update(-1, 5));
		assertEquals(PlayerActivity.IDLE, detector.current());
	}

	@Test
	public void switchesStraightFromSalvagingToSorting()
	{
		PlayerActivity.Detector detector = new PlayerActivity.Detector();
		detector.update(AnimationID.SAILING_HUMAN_SALVAGE_HOOK_KANDARIN_3X8_IDLE01, 10);
		assertEquals(PlayerActivity.SORTING, detector.update(AnimationID.HUMAN_SAILING_SALVAGE01_LARGE01_INTERACT01, 11));
		detector.reset();
		assertEquals(PlayerActivity.IDLE, detector.current());
	}
}
