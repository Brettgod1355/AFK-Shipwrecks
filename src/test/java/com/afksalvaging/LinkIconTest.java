/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

import java.awt.image.BufferedImage;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class LinkIconTest
{
	@Test
	public void eachBrandLogoLoadsAndFitsItsSquareWithoutStretching()
	{
		for (String file : new String[] {"discord_white.png", "discord_blurple.png", "github_white.png", "github_green.png"})
		{
			BufferedImage logo = SalvagingSpotPanel.logo(file);
			assertTrue(file, logo.getWidth() <= SalvagingSpotPanel.LINK_ICON_SIZE && logo.getHeight() <= SalvagingSpotPanel.LINK_ICON_SIZE);
			assertEquals(file + " fills the square one way", SalvagingSpotPanel.LINK_ICON_SIZE, Math.max(logo.getWidth(), logo.getHeight()), 1);
		}
		// Discord's symbol is wider than tall (528 x 400); it keeps that shape.
		BufferedImage discord = SalvagingSpotPanel.logo("discord_white.png");
		assertEquals(12, discord.getHeight());
		// Each coloured version is drawn at its white version's size, so hovering does not shift anything.
		BufferedImage white = SalvagingSpotPanel.logo("github_white.png");
		BufferedImage green = SalvagingSpotPanel.logoLike("github_green.png", white);
		assertEquals(white.getWidth(), green.getWidth());
		assertEquals(white.getHeight(), green.getHeight());
	}
}
