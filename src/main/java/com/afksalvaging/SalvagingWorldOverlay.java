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
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * A flashing notice in the middle of the screen while the player sits at a salvaging spot with
 * no wreck up on a world that is not a salvaging world (owner, 2026-10-03): on those worlds every
 * site is worked and the next wreck comes sooner. The chat tip says it once; this keeps saying it
 * until a wreck is up or the player hops. A setting, on by default.
 */
public class SalvagingWorldOverlay extends Overlay
{
	private static final long FLASH_PERIOD_MS = 500;
	/** How long the spot has to have been bare before the notice starts: a wreck often rises again within seconds. */
	static final long AFTER_MS = 15_000;
	private static final int PADDING = 12;
	private static final int LINE_GAP = 4;

	private final Client client;
	private final AfkSalvagingPlugin plugin;
	private final AfkSalvagingConfig config;
	/** When the spot was first seen bare on this world, or -1 while it is not. */
	private long bareSince = -1;

	@Inject
	SalvagingWorldOverlay(Client client, AfkSalvagingPlugin plugin, AfkSalvagingConfig config)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
		setPriority(PRIORITY_HIGH);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		// At a spot means a wreck site is in view; bare means no wreck the player can work is up.
		// Nothing else is asked, so it shows with or without crew and before the hold was ever opened.
		AfkSession.View view = plugin.getSession().view();
		boolean bare = config.worldAlert() && !plugin.isSalvagingWorld() && plugin.isOwnBoat()
			&& !plugin.getSession().wrecks().presentSites().isEmpty() && view.wrecksUp == 0;
		long now = System.currentTimeMillis();
		if (!bare)
		{
			bareSince = -1;
			return null;
		}
		if (bareSince < 0)
		{
			bareSince = now;
		}
		if (now - bareSince < AFTER_MS)
		{
			return null;
		}
		String title = "NOT A SALVAGING WORLD";
		String detail = "Wrecks come back sooner on worlds " + plugin.salvagingWorldsText();

		// The same size setting as the banner, so one knob sizes every alert.
		float scale = config.bannerScale() / 100f;
		Font bold = FontManager.getRunescapeBoldFont();
		Font titleFont = bold.deriveFont(bold.getSize2D() * scale);
		Font plain = FontManager.getRunescapeFont();
		Font detailFont = plain.deriveFont(plain.getSize2D() * Math.max(1f, scale * 0.75f));
		FontMetrics titleMetrics = graphics.getFontMetrics(titleFont);
		FontMetrics detailMetrics = graphics.getFontMetrics(detailFont);

		int width = Math.max(titleMetrics.stringWidth(title), detailMetrics.stringWidth(detail)) + 2 * PADDING;
		int height = titleMetrics.getHeight() + LINE_GAP + detailMetrics.getHeight() + 2 * PADDING;
		int x = (client.getCanvasWidth() - width) / 2;
		int y = (client.getCanvasHeight() - height) / 2;

		Color background = config.warningBannerColor();
		if ((now / FLASH_PERIOD_MS) % 2 == 1)
		{
			background = new Color(background.getRed(), background.getGreen(), background.getBlue(), background.getAlpha() / 3);
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
