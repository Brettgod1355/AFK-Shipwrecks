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
import java.awt.Rectangle;
import java.awt.Shape;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;
import net.runelite.client.util.ColorUtil;

/**
 * The cargo count written on the hold itself, "39/160" or as "Counter shows" sets it, green to red as
 * it fills (owner, 2026-10-08, so the Sailing plugin's own hold count can be switched off). On the
 * player's own boat, once the count is known.
 */
public class HoldCountOverlay extends Overlay
{
	/** How high above the hold the words sit when its outline is not known, in local units. */
	private static final int HEIGHT = 80;

	private final Client client;
	private final AfkSalvagingPlugin plugin;
	private final AfkSalvagingConfig config;

	@Inject
	HoldCountOverlay(Client client, AfkSalvagingPlugin plugin, AfkSalvagingConfig config)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
		setPriority(PRIORITY_LOW);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		CargoHoldMonitor monitor = plugin.getSession().monitor();
		if (!config.countOnHold() || !plugin.isOwnBoat() || !monitor.hasCount())
		{
			return null;
		}
		GameObject hold = plugin.cargoHoldObject();
		if (hold == null)
		{
			return null;
		}
		int used = monitor.getUsed();
		int capacity = monitor.getCapacity();
		String text = config.holdCountFormat().format(used, capacity);
		Color color = color(config.holdColorStyle(), used, capacity, config.holdStepsNearlyFull(), config.holdStepsFull(),
			config.holdEmptyColor(),
			config.holdNearlyFullColor(), config.holdFullColor());
		Font font = graphics.getFont();
		graphics.setFont(font.deriveFont(font.getSize2D() * Math.max(50, config.holdCountSize()) / 100f));
		Point at = centered(graphics, hold.getConvexHull(), text);
		if (at == null)
		{
			at = Perspective.getCanvasTextLocation(client, graphics, hold.getLocalLocation(), text, HEIGHT);
		}
		if (at != null)
		{
			OverlayUtil.renderTextLocation(graphics, at, text, color);
		}
		return null;
	}

	/**
	 * Where the text sits in the middle of the hold as drawn on screen, or null when the hold has no outline (owner,
	 * 2026-10-25: "get the cargo hold count on the cargo hold a bit more centered"; it sat above the hold).
	 */
	private static Point centered(Graphics2D graphics, Shape hull, String text)
	{
		if (hull == null)
		{
			return null;
		}
		Rectangle bounds = hull.getBounds();
		if (bounds.isEmpty())
		{
			return null;
		}
		FontMetrics metrics = graphics.getFontMetrics();
		return textAt(bounds, metrics.stringWidth(text), metrics.getAscent(), metrics.getDescent());
	}

	/** The count's color for the chosen style (owner, 2026-10-25: the Cargo hold settings). */
	static Color color(HoldColorStyle style, int used, int capacity, int nearlyPercent, int fullPercent, Color empty,
		Color nearlyFull, Color full)
	{
		switch (style)
		{
			case ONE_COLOR:
				return empty;
			case STEPS:
				// By percentage of this hold's size (owner, 2026-10-25); the full step wins if the two are set the wrong way round.
				if (capacity <= 0)
				{
					return empty;
				}
				if (used * 100 >= capacity * fullPercent)
				{
					return full;
				}
				return used * 100 >= capacity * nearlyPercent ? nearlyFull : empty;
			default:
				// Three stops: empty, nearly full at half, full (owner, 2026-10-25). A straight two-color mix went through
				// a dull olive in the middle.
				double fill = capacity > 0 ? Math.min(1.0, (double) used / capacity) : 0;
				return fill <= 0.5 ? ColorUtil.colorLerp(empty, nearlyFull, fill * 2)
					: ColorUtil.colorLerp(nearlyFull, full, (fill - 0.5) * 2);
		}
	}

	/** The baseline point that centers text of this width and height on the box. */
	static Point textAt(Rectangle box, int width, int ascent, int descent)
	{
		int x = (int) Math.round(box.getCenterX() - width / 2.0);
		int y = (int) Math.round(box.getCenterY() + (ascent - descent) / 2.0);
		return new Point(x, y);
	}
}
