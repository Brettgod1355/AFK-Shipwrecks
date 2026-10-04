/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Shape;
import javax.inject.Inject;
import net.runelite.api.GameObject;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

/**
 * Outlines the cargo hold on the player's own boat in the Deposit colour, so the place the
 * cyan-boxed items go is marked the same way (owner, 2026-10-03). A setting, off by default.
 */
public class CargoHoldOverlay extends Overlay
{
	private final AfkSalvagingPlugin plugin;
	private final AfkSalvagingConfig config;

	@Inject
	CargoHoldOverlay(AfkSalvagingPlugin plugin, AfkSalvagingConfig config)
	{
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
		setPriority(PRIORITY_LOW);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.highlightCargoHold() || !plugin.isOwnBoat())
		{
			return null;
		}
		GameObject hold = plugin.cargoHoldObject();
		Shape hull = hold == null ? null : hold.getConvexHull();
		if (hull != null)
		{
			OverlayUtil.renderPolygon(graphics, hull, config.holdColor());
		}
		return null;
	}
}
