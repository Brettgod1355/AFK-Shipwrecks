/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
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
	/** How high above the hold the words sit, in local units. */
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
		String text = config.counterStyle().format(used, capacity);
		double fill = capacity > 0 ? Math.min(1.0, (double) used / capacity) : 0;
		Color color = ColorUtil.colorLerp(Palette.COUNTER_EMPTY, Palette.COUNTER_FULL, fill);
		Point at = Perspective.getCanvasTextLocation(client, graphics, hold.getLocalLocation(), text, HEIGHT);
		if (at != null)
		{
			OverlayUtil.renderTextLocation(graphics, at, text, color);
		}
		return null;
	}
}
