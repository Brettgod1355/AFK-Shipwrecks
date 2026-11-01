package com.afksalvaging;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SalvagingDimmerTest
{
	private final Options config = new Options();
	private final AfkSession.View view = new AfkSession.View();
	private final CargoHoldMonitor hold = new CargoHoldMonitor();

	private static final class Options implements AfkSalvagingConfig
	{
		boolean crew, waiting, salvaging, sorting, early;
		int percent = 100;

		@Override public boolean dimWhenCrewSalvaging() { return crew; }
		@Override public boolean dimWhenNoWrecks() { return waiting; }
		@Override public boolean dimWhenSalvaging() { return salvaging; }
		@Override public boolean dimWhenSorting() { return sorting; }
		@Override public boolean undimSoonerWhenSorting() { return early; }
		@Override public int cargoHoldUndimPercent() { return percent; }
	}

	@Before
	public void workingCrew()
	{
		view.atSpot = true;
		view.wrecksUp = 1;
		view.crewSalvaging = true;
		hold.setCapacity(240);
		hold.setUsed(200);
	}

	private boolean dimmed()
	{
		return SalvagingDimmer.shouldDim(config, view, hold);
	}

	@Test
	public void crewDimmingIsOptInAndEndsWhenTheCrewStop()
	{
		assertFalse(dimmed());
		config.crew = true;
		assertTrue(dimmed());
		view.crewSalvaging = false;
		assertFalse(dimmed());
	}

	@Test
	public void cargoThresholdUsesSlotsWithoutRoundingAndResumesAfterWithdrawal()
	{
		config.crew = true;
		config.percent = 90;
		hold.setEstimatedUsed(215);
		assertTrue(dimmed());
		hold.adjust(1);
		assertFalse("216/240 is exactly 90%", dimmed());
		hold.adjust(-1);
		assertTrue(dimmed());
		config.percent = 100;
		hold.setUsed(239);
		assertTrue(dimmed());
		hold.setUsed(240);
		assertFalse(dimmed());
	}

	@Test
	public void unknownHoldDoesNotInventAFullCount()
	{
		config.crew = true;
		hold.reset();
		assertTrue(dimmed());
		hold.setUsed(100);
		assertTrue("unknown capacity", dimmed());
	}

	@Test
	public void waitingNeedsAKnownSiteAndUndimsAtAnyRespawn()
	{
		config.waiting = true;
		view.crewSalvaging = false;
		assertFalse(dimmed());
		view.wrecksUp = 0;
		assertTrue(dimmed());
		view.activity = PlayerActivity.AT_HOOK_IDLE;
		assertTrue("waiting at an empty hook", dimmed());
		view.higherWrecksUp = 1;
		assertFalse("a wreck rose even if its level is too high", dimmed());
		view.higherWrecksUp = 0;
		view.atSpot = false;
		assertFalse("open water", dimmed());
	}

	@Test
	public void waitingIsIndependentOfTheCrewCargoThreshold()
	{
		config.crew = true;
		view.crewSalvaging = false;
		view.wrecksUp = 0;
		hold.setUsed(240);
		assertFalse(dimmed());
		config.waiting = true;
		assertTrue(dimmed());
		view.wrecksUp = 1;
		assertFalse(dimmed());
	}

	@Test
	public void activeModesDoNotFallThroughToIdleCrewOrWaitingModes()
	{
		config.crew = config.waiting = true;
		view.activity = PlayerActivity.SALVAGING;
		assertFalse(dimmed());
		config.salvaging = true;
		assertTrue(dimmed());
		view.activity = PlayerActivity.SORTING;
		assertFalse(dimmed());
		config.sorting = true;
		assertTrue(dimmed());
		config.early = true;
		view.sortingLeftMillis = 3001;
		assertTrue(dimmed());
		view.sortingLeftMillis = 3000;
		assertFalse(dimmed());
		view.sortingLeftMillis = -1;
		assertTrue("unknown ETA does not mean finished", dimmed());
		view.activity = PlayerActivity.AT_HOOK_IDLE;
		assertFalse("a player stopped at a hook with a wreck up needs attention", dimmed());
	}

	@Test
	public void dangerClearsEveryDimmingMode()
	{
		config.crew = config.waiting = config.salvaging = config.sorting = true;
		view.wrecksUp = 0;
		for (PlayerActivity activity : PlayerActivity.values())
		{
			view.activity = activity;
			view.danger = "Boat is taking damage";
			assertFalse(dimmed());
			view.danger = null;
			view.hazardous = true;
			assertFalse(dimmed());
			view.hazardous = false;
		}
	}
}
