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
		// Outline wrecks, Wreck reach boxes: both on was the default; 2.2 then moves it to the tile.
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
		assertEquals("Tile and area", WreckMarking.TILE_AND_AREA.toString());
	}

	@Test
	public void aTileIsNeverAHull()
	{
		assertTrue(WreckMarking.TILE_AND_AREA.tile() && WreckMarking.TILE_AND_AREA.area());
		assertTrue(WreckMarking.TILE.tile());
		assertFalse(WreckMarking.TILE.area());
		for (WreckMarking marking : WreckMarking.values())
		{
			assertFalse(marking.name(), marking.tile() && marking.hull());
		}
		assertFalse(WreckMarking.BOTH.tile() || WreckMarking.AREA.tile() || WreckMarking.OFF.tile());
	}

	@Test
	public void onlyTheOldDefaultMovesToTheTile()
	{
		// Hull and area was the default through 2.1 (owner, 2026-10-14: a small tile instead).
		assertEquals(WreckMarking.TILE_AND_AREA, WreckMarking.tileDefault(WreckMarking.BOTH));
		assertEquals(WreckMarking.HULL, WreckMarking.tileDefault(WreckMarking.HULL));
		assertEquals(WreckMarking.AREA, WreckMarking.tileDefault(WreckMarking.AREA));
		assertEquals(WreckMarking.OFF, WreckMarking.tileDefault(WreckMarking.OFF));
		assertEquals(WreckMarking.TILE, WreckMarking.tileDefault(WreckMarking.TILE));
	}

	@Test
	public void theSavedNamesOf21AreKept()
	{
		// RuneLite saves the constant's name: renaming one would lose a player's choice.
		assertEquals(WreckMarking.BOTH, WreckMarking.valueOf("BOTH"));
		assertEquals(WreckMarking.HULL, WreckMarking.valueOf("HULL"));
		assertEquals(WreckMarking.AREA, WreckMarking.valueOf("AREA"));
		assertEquals(WreckMarking.OFF, WreckMarking.valueOf("OFF"));
	}

	@Test
	public void sunkBoxesAreGrayAsStrongAsThePickedColor()
	{
		Color sunk = SalvageBoxOverlay.sunk(new Color(255, 215, 40, 190));
		assertEquals(Palette.WRECK_SUNK.getRed(), sunk.getRed());
		assertEquals(Palette.WRECK_SUNK.getGreen(), sunk.getGreen());
		assertEquals(Palette.WRECK_SUNK.getBlue(), sunk.getBlue());
		assertEquals(190, sunk.getAlpha());
	}
}
