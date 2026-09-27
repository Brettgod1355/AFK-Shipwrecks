/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.cargofull;

import com.cargofull.CargoHoldMonitor.Level;
import net.runelite.api.Item;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class CargoHoldMonitorTest
{
	private static final long NO_REPEAT = 0;

	private final CargoHoldMonitor monitor = new CargoHoldMonitor();

	@Test
	public void countsOnlyOccupiedSlots()
	{
		Item[] items = {new Item(995, 1), new Item(-1, 0), null, new Item(1511, 0), new Item(1513, 5)};
		assertEquals(2, CargoHoldMonitor.countUsed(items));
		assertEquals(0, CargoHoldMonitor.countUsed(null));
		assertEquals(0, CargoHoldMonitor.countUsed(new Item[0]));
	}

	@Test
	public void alertsOnceWhenTheHoldFills()
	{
		monitor.setCapacity(40);
		monitor.setUsed(39);
		assertNull(monitor.poll(0, 0, NO_REPEAT));

		monitor.setUsed(40);
		assertEquals(Level.FULL, monitor.poll(1, 0, NO_REPEAT));
		assertNull(monitor.poll(2, 0, NO_REPEAT));
		assertNull(monitor.poll(60_000, 0, NO_REPEAT));
	}

	@Test
	public void reArmsOnceSpaceIsMade()
	{
		monitor.setCapacity(40);
		monitor.setUsed(40);
		assertEquals(Level.FULL, monitor.poll(0, 0, NO_REPEAT));

		monitor.setUsed(12);
		assertNull(monitor.poll(1, 0, NO_REPEAT));
		assertEquals(Level.OK, monitor.level(0));

		monitor.setUsed(40);
		assertEquals(Level.FULL, monitor.poll(2, 0, NO_REPEAT));
	}

	@Test
	public void warnsEarlyThenEscalatesToFull()
	{
		int warnAt = 3;
		monitor.setCapacity(40);
		monitor.setUsed(36);
		assertNull(monitor.poll(0, warnAt, NO_REPEAT));

		monitor.setUsed(37);
		assertEquals(Level.NEARLY_FULL, monitor.poll(1, warnAt, NO_REPEAT));
		monitor.setUsed(38);
		assertNull(monitor.poll(2, warnAt, NO_REPEAT));

		monitor.setUsed(40);
		assertEquals(Level.FULL, monitor.poll(3, warnAt, NO_REPEAT));
		assertNull(monitor.poll(4, warnAt, NO_REPEAT));
	}

	@Test
	public void takingOneItemOutDoesNotReAlertUntilFullAgain()
	{
		monitor.setCapacity(40);
		monitor.setUsed(40);
		assertEquals(Level.FULL, monitor.poll(0, 3, NO_REPEAT));

		monitor.setUsed(39);
		assertNull(monitor.poll(1, 3, NO_REPEAT));
		assertEquals(Level.NEARLY_FULL, monitor.level(3));

		monitor.setUsed(40);
		assertEquals(Level.FULL, monitor.poll(2, 3, NO_REPEAT));
	}

	@Test
	public void repeatsWhileItStaysFull()
	{
		long every = 10_000;
		monitor.setCapacity(40);
		monitor.setUsed(40);
		assertEquals(Level.FULL, monitor.poll(0, 0, every));
		assertNull(monitor.poll(5_000, 0, every));
		assertEquals(Level.FULL, monitor.poll(10_000, 0, every));
		assertNull(monitor.poll(15_000, 0, every));
		assertEquals(Level.FULL, monitor.poll(20_000, 0, every));
	}

	@Test
	public void gameMessageCountsAsFullUntilTheNextCount()
	{
		monitor.markFullByGame();
		assertTrue(monitor.isReportedFullByGame());
		assertEquals(Level.FULL, monitor.level(0));
		assertEquals(Level.FULL, monitor.poll(0, 0, NO_REPEAT));
		assertNull(monitor.poll(1, 0, NO_REPEAT));

		monitor.setCapacity(40);
		monitor.setUsed(10);
		assertFalse(monitor.isReportedFullByGame());
		assertEquals(Level.OK, monitor.level(0));
	}

	@Test
	public void gameMessageCanBeForgotten()
	{
		monitor.markFullByGame();
		monitor.clearFullByGame();
		assertEquals(Level.OK, monitor.level(0));
	}

	@Test
	public void nothingFiresWithoutBothNumbers()
	{
		monitor.setUsed(240);
		assertFalse(monitor.hasCount());
		assertNull(monitor.poll(0, 0, NO_REPEAT));
		assertEquals(CargoHoldCapacity.UNKNOWN, monitor.remaining());

		monitor.reset();
		monitor.setCapacity(40);
		assertFalse(monitor.hasCount());
		assertNull(monitor.poll(1, 0, NO_REPEAT));
	}

	@Test
	public void capacityNeverShowsLessThanTheCount()
	{
		monitor.setCapacity(40);
		monitor.setUsed(45);
		assertEquals(45, monitor.getCapacity());
		assertEquals(0, monitor.remaining());
		assertEquals(Level.FULL, monitor.level(0));
	}

	@Test
	public void resetForgetsEverything()
	{
		monitor.setCapacity(40);
		monitor.setUsed(40);
		monitor.markFullByGame();
		assertEquals(Level.FULL, monitor.poll(0, 0, NO_REPEAT));

		monitor.reset();
		assertEquals(CargoHoldCapacity.UNKNOWN, monitor.getUsed());
		assertEquals(CargoHoldCapacity.UNKNOWN, monitor.getCapacity());
		assertFalse(monitor.isReportedFullByGame());
		assertEquals(Level.OK, monitor.level(0));

		monitor.setCapacity(40);
		monitor.setUsed(40);
		assertEquals(Level.FULL, monitor.poll(1, 0, NO_REPEAT));
	}
}
