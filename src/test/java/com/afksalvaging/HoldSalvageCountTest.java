/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/** The salvage count in the hold that picks between lighting the hold and a hook. */
public class HoldSalvageCountTest
{
	private final HoldSalvageCount hold = new HoldSalvageCount();

	@Test
	public void unknownUntilTheHoldIsRead()
	{
		assertEquals(HoldSalvageCount.UNKNOWN, hold.get());
		hold.crewSalvage(3);
		hold.inventorySalvageChanged(5, 10, false, true, false);
		assertEquals(HoldSalvageCount.UNKNOWN, hold.get());
		hold.read(18, 11);
		assertEquals(18, hold.get());
	}

	@Test
	public void crewSalvageAddsWhileTheHoldIsShut()
	{
		hold.read(10, 1);
		hold.crewSalvage(1);
		hold.crewSalvage(2);
		assertEquals(13, hold.get());
	}

	@Test
	public void aWithdrawalWithTheWindowShutTakesWhatTheInventoryGained()
	{
		// In game (2026-10-13, Original RS2, tick 449): Withdraw-All, the window closed in the same tick, and the
		// inventory filled with no hold read after it.
		hold.read(105, 447);
		hold.inventorySalvageChanged(18, 449, false, true, false);
		assertEquals(87, hold.get());
		// Never below empty.
		hold.inventorySalvageChanged(200, 450, false, true, false);
		assertEquals(0, hold.get());
	}

	@Test
	public void aDepositWithTheWindowShutAddsWhatTheInventoryLost()
	{
		hold.read(4, 1);
		hold.inventorySalvageChanged(-6, 5, false, false, true);
		assertEquals(10, hold.get());
	}

	@Test
	public void theReadIsTheTruthWhileTheWindowIsOpenOrWasReadThisTick()
	{
		hold.read(30, 100);
		hold.inventorySalvageChanged(12, 101, true, true, false);
		assertEquals(30, hold.get());
		// A read and the inventory change in the same tick: the read already holds the change.
		hold.read(18, 102);
		hold.inventorySalvageChanged(12, 102, false, true, false);
		assertEquals(18, hold.get());
	}

	@Test
	public void hookingAndSortingAreNotTheHolds()
	{
		hold.read(20, 1);
		hold.inventorySalvageChanged(1, 5, false, false, false);
		hold.inventorySalvageChanged(-1, 6, false, false, false);
		assertEquals(20, hold.get());
	}

	@Test
	public void theCountRememberedAtLogoutStandsUntilTheNextRead()
	{
		hold.restore(37);
		assertEquals(37, hold.get());
		hold.crewSalvage(2);
		assertEquals("followed like a read count", 39, hold.get());
		hold.restore(10);
		assertEquals("a known count is never overwritten", 39, hold.get());
		hold.read(41, 5);
		assertEquals(41, hold.get());
		hold.forget();
		hold.restore(-1);
		assertEquals("nothing saved: still unknown", HoldSalvageCount.UNKNOWN, hold.get());
	}

	@Test
	public void bankingEmptiesItAndAnotherBoatForgetsIt()
	{
		hold.read(40, 1);
		hold.banked();
		assertEquals(0, hold.get());
		hold.forget();
		assertEquals(HoldSalvageCount.UNKNOWN, hold.get());
	}
}
