/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.cargofull;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import javax.inject.Inject;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.ComponentConstants;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;
import net.runelite.client.util.ColorUtil;

/**
 * Draws the CARGO HOLD FULL banner and the used/total cargo counter at the top of the game.
 */
public class CargoFullOverlay extends OverlayPanel
{
	/** Panel width at 100% banner size. */
	private static final int BASE_WIDTH = 200;
	/** Breathing room either side of text that is wider than the panel. */
	private static final int TEXT_PADDING = 8;
	/** Minimum space between the counter's label and its number. */
	private static final int COUNTER_GAP = 12;
	private static final long FLASH_PERIOD_MS = 500;
	private static final Color COUNTER_EMPTY = new Color(70, 200, 70);
	private static final Color COUNTER_FULL = new Color(230, 60, 60);

	private final CargoFullPlugin plugin;
	private final CargoFullConfig config;

	@Inject
	CargoFullOverlay(CargoFullPlugin plugin, CargoFullConfig config)
	{
		super(plugin);
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.TOP_CENTER);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
		setPriority(PRIORITY_HIGH);
		panelComponent.setPreferredSize(new Dimension(BASE_WIDTH, 0));
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		long now = System.currentTimeMillis();
		CargoHoldMonitor.Level banner = plugin.bannerLevel(now);
		boolean showCounter = config.showCounter() && plugin.isSailing() && plugin.hasCount();
		if (banner == CargoHoldMonitor.Level.OK && !showCounter)
		{
			return null;
		}

		Color background = ComponentConstants.STANDARD_BACKGROUND_COLOR;
		// The panel only grows as wide as what it shows: the counter alone hugs its text,
		// while the banner has a comfortable minimum width.
		int width = 0;
		if (banner != CargoHoldMonitor.Level.OK)
		{
			background = config.bannerColor();
			if (config.flashBanner() && (now / FLASH_PERIOD_MS) % 2 == 1)
			{
				background = new Color(background.getRed(), background.getGreen(), background.getBlue(),
					background.getAlpha() / 3);
			}

			// The title takes its font from the graphics, so scaling the font scales the banner.
			String text = banner == CargoHoldMonitor.Level.FULL ? "CARGO HOLD FULL" : "CARGO HOLD NEARLY FULL";
			float scale = config.bannerScale() / 100f;
			Font bold = FontManager.getRunescapeBoldFont();
			Font bannerFont = bold.deriveFont(bold.getSize2D() * scale);
			graphics.setFont(bannerFont);
			int textWidth = graphics.getFontMetrics(bannerFont).stringWidth(text);
			width = Math.max(Math.round(BASE_WIDTH * scale),
				textWidth + 2 * ComponentConstants.STANDARD_BORDER + TEXT_PADDING);

			panelComponent.getChildren().add(TitleComponent.builder()
				.text(text)
				.color(Color.WHITE)
				.build());
		}
		panelComponent.setBackgroundColor(background);

		if (showCounter)
		{
			int used = plugin.getUsed();
			int capacity = plugin.getCapacity();
			double fill = capacity > 0 ? Math.min(1.0, (double) used / capacity) : 0;
			Color color = banner != CargoHoldMonitor.Level.OK
				? Color.WHITE
				: ColorUtil.colorLerp(COUNTER_EMPTY, COUNTER_FULL, fill);

			// The counter has its own size, independent of the banner's.
			String left = "Cargo hold";
			String right = used + "/" + capacity;
			float counterScale = config.counterScale() / 100f;
			Font normal = FontManager.getRunescapeFont();
			Font counterFont = normal.deriveFont(normal.getSize2D() * counterScale);
			FontMetrics metrics = graphics.getFontMetrics(counterFont);
			int lineWidth = metrics.stringWidth(left) + metrics.stringWidth(right) + COUNTER_GAP;
			width = Math.max(width, lineWidth + 2 * ComponentConstants.STANDARD_BORDER + TEXT_PADDING);

			panelComponent.getChildren().add(LineComponent.builder()
				.left(left)
				.right(right)
				.rightColor(color)
				.leftFont(counterFont)
				.rightFont(counterFont)
				.build());
		}

		if (getPreferredSize() == null)
		{
			// Follow the chosen sizes unless the player has resized the overlay themselves.
			panelComponent.setPreferredSize(new Dimension(width, 0));
		}

		return super.render(graphics);
	}
}
