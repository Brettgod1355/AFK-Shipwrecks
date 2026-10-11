/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

import java.awt.Color;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class WreckMarkingTest
{
	@Test
	public void twoPointOhSwitchesCarryOver()
	{
		// Outline wrecks, Wreck reach boxes: both on is the default and stays the default.
		assertEquals(WreckMarking.BOTH, WreckMarking.of(true, true));
		assertEquals(WreckMarking.HULL, WreckMarking.of(true, false));
		assertEquals(WreckMarking.AREA, WreckMarking.of(false, true));
		assertEquals(WreckMarking.OFF, WreckMarking.of(false, false));
	}

	@Test
	public void eachChoiceDrawsWhatItSays()
	{
		assertTrue(WreckMarking.HULL.hull());
		assertFalse(WreckMarking.HULL.area());
		assertFalse(WreckMarking.AREA.hull());
		assertTrue(WreckMarking.AREA.area());
		assertTrue(WreckMarking.BOTH.hull() && WreckMarking.BOTH.area());
		assertFalse(WreckMarking.OFF.hull() || WreckMarking.OFF.area());
	}

	@Test
	public void theDropdownReadsAsWords()
	{
		assertEquals("Hull and area", WreckMarking.BOTH.toString());
	}

	@Test
	public void sunkBoxesAreGreyAsStrongAsThePickedColour()
	{
		Color sunk = SalvageBoxOverlay.sunk(new Color(255, 215, 40, 190));
		assertEquals(Palette.WRECK_SUNK.getRed(), sunk.getRed());
		assertEquals(Palette.WRECK_SUNK.getGreen(), sunk.getGreen());
		assertEquals(Palette.WRECK_SUNK.getBlue(), sunk.getBlue());
		assertEquals(190, sunk.getAlpha());
	}
}
