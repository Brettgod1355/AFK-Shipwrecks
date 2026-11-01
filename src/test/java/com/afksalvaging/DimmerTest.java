/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

import java.awt.Rectangle;
import java.awt.geom.Area;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class DimmerTest
{
	/** The Dimmer settings, each switch off as on a new install until a test turns it on. */
	private static final class Settings implements AfkSalvagingConfig
	{
		boolean crew, noWreck, salvaging, sorting;
		boolean undimInventoryFull = true;
		int undimAt = 100;
		int early;

		@Override
		public boolean dimWhileCrewSalvage()
		{
			return crew;
		}

		@Override
		public int dimCrewUndimAt()
		{
			return undimAt;
		}

		@Override
		public boolean dimWhileNoWreck()
		{
			return noWreck;
		}

		@Override
		public boolean dimWhileSalvaging()
		{
			return salvaging;
		}

		@Override
		public boolean dimWhileSorting()
		{
			return sorting;
		}

		@Override
		public int dimSortingUndimEarly()
		{
			return early;
		}

		@Override
		public boolean dimUndimInventoryFull()
		{
			return undimInventoryFull;
		}
	}

	private final Settings settings = new Settings();
	private final AfkSession.View view = new AfkSession.View();
	private final CargoHoldMonitor hold = new CargoHoldMonitor();

	@Before
	public void crewAtWork()
	{
		view.parked = true;
		view.atSpot = true;
		view.wrecksUp = 1;
		view.crewSalvaging = true;
		hold.setCapacity(240);
		hold.setUsed(200);
	}

	private boolean dims()
	{
		return Dimmer.dims(settings, view, hold);
	}

	@Test
	public void nothingIsDimmedUntilTurnedOn()
	{
		AfkSalvagingConfig defaults = new AfkSalvagingConfig()
		{
		};
		assertFalse(defaults.dimWhileCrewSalvage() || defaults.dimWhileNoWreck() || defaults.dimWhileSalvaging()
			|| defaults.dimWhileSorting());
		assertEquals(DimmerCovers.GAME_VIEW, defaults.dimmerCovers());
		assertTrue(defaults.dimUndimOnAlerts());
		assertFalse(defaults.dimUndimInventoryFull());
		for (PlayerActivity activity : PlayerActivity.values())
		{
			view.activity = activity;
			view.wrecksUp = 0;
			assertFalse(activity.name(), dims());
		}
	}

	@Test
	public void anySwitchTurnsItOn()
	{
		assertFalse(Dimmer.anyOn(settings));
		settings.sorting = true;
		assertTrue(Dimmer.anyOn(settings));
	}

	@Test
	public void theCrewDimUntilTheyStopOrTheHoldIsFullEnough()
	{
		settings.crew = true;
		assertTrue(dims());
		view.crewSalvaging = false;
		assertFalse("stopped or stalled", dims());
		view.crewSalvaging = true;

		settings.undimAt = 90;
		hold.setUsed(215);
		assertTrue(dims());
		hold.setUsed(216);
		assertFalse("216/240 is exactly 90%", dims());
		hold.setUsed(200);
		assertTrue("dims again after a withdraw", dims());

		settings.undimAt = 100;
		hold.setUsed(239);
		assertTrue(dims());
		hold.setUsed(240);
		assertFalse(dims());
	}

	@Test
	public void anUnknownHoldIsNotFull()
	{
		settings.crew = true;
		settings.undimAt = 50;
		hold.reset();
		hold.setUsed(100);
		assertTrue(dims());
	}

	@Test
	public void waitingNeedsASpotAndClearsWhenAnyWreckRises()
	{
		settings.noWreck = true;
		view.crewSalvaging = false;
		assertFalse("a wreck is up", dims());
		view.wrecksUp = 0;
		assertTrue(dims());
		view.activity = PlayerActivity.AT_HOOK_IDLE;
		assertTrue("waiting at an empty hook", dims());
		view.higherWrecksUp = 1;
		assertFalse("a wreck above your level rose too", dims());
		view.higherWrecksUp = 0;
		view.atSpot = false;
		assertFalse("open water", dims());
	}

	@Test
	public void waitingDoesNotCareHowFullTheHoldIs()
	{
		settings.crew = true;
		settings.noWreck = true;
		view.crewSalvaging = false;
		view.wrecksUp = 0;
		hold.setUsed(240);
		assertTrue(dims());
	}

	@Test
	public void yourOwnSalvagingAndSortingHaveTheirOwnSwitches()
	{
		settings.crew = true;
		settings.noWreck = true;
		view.activity = PlayerActivity.SALVAGING;
		assertFalse("the crew and waiting switches are for when you stand idle", dims());
		settings.salvaging = true;
		assertTrue(dims());

		view.activity = PlayerActivity.SORTING;
		assertFalse(dims());
		settings.sorting = true;
		view.sortingLeftMillis = 1000;
		assertTrue("0 seconds waits for the end", dims());
		settings.early = 3;
		view.sortingLeftMillis = 3001;
		assertTrue(dims());
		view.sortingLeftMillis = 3000;
		assertFalse(dims());
		view.sortingLeftMillis = -1;
		assertTrue("an unknown end is not nearly done", dims());

		view.activity = PlayerActivity.AT_HOOK_IDLE;
		assertFalse("stopped at a hook with a wreck up needs you", dims());
	}

	@Test
	public void aFullInventoryClearsItUnlessYouAreSorting()
	{
		settings.crew = settings.salvaging = settings.sorting = true;
		settings.undimInventoryFull = true;
		view.inventoryFull = true;
		assertFalse("idle with the crew still salvaging", dims());
		view.activity = PlayerActivity.SALVAGING;
		assertFalse("stopped at the hook, its loop still playing", dims());
		view.activity = PlayerActivity.SORTING;
		assertTrue("sorting starts from a full inventory", dims());

		settings.undimInventoryFull = false;
		view.activity = PlayerActivity.IDLE;
		assertTrue(dims());
	}

	@Test
	public void aMovingBoatIsNeverDimmed()
	{
		settings.crew = settings.noWreck = settings.salvaging = settings.sorting = true;
		view.wrecksUp = 0;
		for (PlayerActivity activity : PlayerActivity.values())
		{
			view.activity = activity;
			view.parked = true;
			assertTrue(activity.name(), dims());
			view.parked = false;
			assertFalse(activity.name(), dims());
		}
	}

	@Test
	public void dangerClearsEverything()
	{
		settings.crew = settings.noWreck = settings.salvaging = settings.sorting = true;
		view.wrecksUp = 0;
		for (PlayerActivity activity : PlayerActivity.values())
		{
			view.activity = activity;
			view.danger = "Your boat is taking damage";
			assertFalse(activity.name(), dims());
			view.danger = null;
			view.hazardous = true;
			assertFalse(activity.name(), dims());
			view.hazardous = false;
		}
	}

	@Test
	public void theDimmedAreaFollowsDimmerCovers()
	{
		Rectangle canvas = new Rectangle(0, 0, 100, 100);
		Rectangle viewport = new Rectangle(10, 10, 60, 40);
		Rectangle chat = new Rectangle(0, 60, 60, 40);

		Area view = Dimmer.area(DimmerCovers.GAME_VIEW, canvas, viewport, chat);
		assertEquals(viewport, view.getBounds());
		assertNull("no game view", Dimmer.area(DimmerCovers.GAME_VIEW, canvas, new Rectangle(), chat));
		assertEquals("cut to the canvas", new Rectangle(0, 0, 20, 20),
			Dimmer.area(DimmerCovers.GAME_VIEW, canvas, new Rectangle(-10, -10, 30, 30), chat).getBounds());

		Area allButChat = Dimmer.area(DimmerCovers.ALL_BUT_CHAT, canvas, viewport, chat);
		assertTrue(allButChat.contains(80, 80));
		assertFalse(allButChat.contains(20, 80));
		assertTrue("chat box hidden", Dimmer.area(DimmerCovers.ALL_BUT_CHAT, canvas, viewport, null).contains(20, 80));

		Area everything = Dimmer.area(DimmerCovers.EVERYTHING, canvas, viewport, chat);
		assertTrue(everything.contains(20, 80));
		assertEquals(canvas, everything.getBounds());
	}
}
