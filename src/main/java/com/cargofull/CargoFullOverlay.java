/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.cargofull;

import java.awt.Color;
import java.awt.Dimension;
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
	private static final int WIDTH = 200;
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
		panelComponent.setPreferredSize(new Dimension(WIDTH, 0));
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
		if (banner != CargoHoldMonitor.Level.OK)
		{
			background = config.bannerColor();
			if (config.flashBanner() && (now / FLASH_PERIOD_MS) % 2 == 1)
			{
				background = new Color(background.getRed(), background.getGreen(), background.getBlue(),
					background.getAlpha() / 3);
			}
			graphics.setFont(FontManager.getRunescapeBoldFont());
			panelComponent.getChildren().add(TitleComponent.builder()
				.text(banner == CargoHoldMonitor.Level.FULL ? "CARGO HOLD FULL" : "CARGO HOLD NEARLY FULL")
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
			panelComponent.getChildren().add(LineComponent.builder()
				.left("Cargo hold")
				.right(used + "/" + capacity)
				.rightColor(color)
				.build());
		}

		return super.render(graphics);
	}
}
