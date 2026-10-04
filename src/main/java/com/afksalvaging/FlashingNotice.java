/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import net.runelite.api.Client;
import net.runelite.client.ui.FontManager;

/**
 * A two-line notice flashing in the middle of the screen, the shape the salvaging-world alert
 * and the crew swap alert share: a bold title, a plain line under it, the banner's size setting,
 * and a background that dims every half second.
 */
final class FlashingNotice
{
	private static final long FLASH_PERIOD_MS = 500;
	private static final int PADDING = 12;
	private static final int LINE_GAP = 4;

	private FlashingNotice()
	{
	}

	/**
	 * @param scale   the banner size setting as a fraction, 1 for 100%
	 * @param yOffset pixels below the centre of the canvas, so two notices can stand clear of each other
	 */
	static Dimension draw(Graphics2D graphics, Client client, String title, String detail, Color colour, float scale, long now, int yOffset)
	{
		Font bold = FontManager.getRunescapeBoldFont();
		Font titleFont = bold.deriveFont(bold.getSize2D() * scale);
		Font plain = FontManager.getRunescapeFont();
		// Three quarters of the title, never below the font's own size to stay readable, and never above the title (review, 2026-10-04).
		Font detailFont = plain.deriveFont(Math.min(titleFont.getSize2D(), plain.getSize2D() * Math.max(1f, scale * 0.75f)));
		FontMetrics titleMetrics = graphics.getFontMetrics(titleFont);
		FontMetrics detailMetrics = graphics.getFontMetrics(detailFont);

		int width = Math.max(titleMetrics.stringWidth(title), detailMetrics.stringWidth(detail)) + 2 * PADDING;
		int height = titleMetrics.getHeight() + LINE_GAP + detailMetrics.getHeight() + 2 * PADDING;
		int x = (client.getCanvasWidth() - width) / 2;
		int y = (client.getCanvasHeight() - height) / 2 + yOffset;

		Color background = colour;
		if ((now / FLASH_PERIOD_MS) % 2 == 1)
		{
			background = new Color(colour.getRed(), colour.getGreen(), colour.getBlue(), colour.getAlpha() / 3);
		}
		graphics.setColor(background);
		graphics.fillRect(x, y, width, height);

		graphics.setColor(Palette.TEXT);
		graphics.setFont(titleFont);
		int titleY = y + PADDING + titleMetrics.getAscent();
		graphics.drawString(title, x + (width - titleMetrics.stringWidth(title)) / 2, titleY);
		graphics.setFont(detailFont);
		int detailY = titleY + titleMetrics.getDescent() + LINE_GAP + detailMetrics.getAscent();
		graphics.drawString(detail, x + (width - detailMetrics.stringWidth(detail)) / 2, detailY);
		return new Dimension(width, height);
	}
}
