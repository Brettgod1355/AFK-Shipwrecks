/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Stroke;
import java.util.List;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

/**
 * Boxes the water where a hook reaches two wrecks at once, so you know where to park.
 * <p>
 * Wrecks live in the top-level world even while the player stands on a boat, so the boxes are
 * placed with the top-level world view's coordinates and drawn above the scene.
 */
public class DoubleSpotOverlay extends Overlay
{
	private static final Stroke OUTLINE = new BasicStroke(2);
	/** How much dimmer a box is when fewer than two of its wrecks are up. */
	private static final float DIM = 0.45f;

	private final Client client;
	private final AfkSalvagingPlugin plugin;
	private final AfkSalvagingConfig config;

	@Inject
	DoubleSpotOverlay(Client client, AfkSalvagingPlugin plugin, AfkSalvagingConfig config)
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
		DoubleSpotMode mode = config.doubleSpotMode();
		if (mode == DoubleSpotMode.OFF)
		{
			return null;
		}
		WorldView top = client.getTopLevelWorldView();
		if (top == null)
		{
			return null;
		}
		List<WreckTracker.Site> sites = plugin.getSession().wrecks().presentSites();
		List<DoubleSpot> spots = DoubleSpot.find(sites, AfkSession.HOOK_RANGE, mode);
		Color full = config.doubleSpotColor();
		Color dim = new Color(full.getRed(), full.getGreen(), full.getBlue(), Math.round(full.getAlpha() * DIM));
		for (DoubleSpot spot : spots)
		{
			DoubleSpot.Box box = spot.getBox();
			LocalPoint low = LocalPoint.fromWorld(top, box.getMinX(), box.getMinY());
			LocalPoint high = LocalPoint.fromWorld(top, box.getMaxX(), box.getMaxY());
			if (low == null || high == null)
			{
				// Part of the box is outside the loaded scene; it will show once the boat is closer.
				continue;
			}
			LocalPoint centre = new LocalPoint((low.getX() + high.getX()) / 2, (low.getY() + high.getY()) / 2, top);
			Polygon area = Perspective.getCanvasTileAreaPoly(client, centre, box.width(), box.height(), box.getPlane(), 0);
			if (area == null)
			{
				continue;
			}
			Color colour = spot.activeCount() == 2 ? full : dim;
			Color fill = new Color(colour.getRed(), colour.getGreen(), colour.getBlue(), colour.getAlpha() / 4);
			OverlayUtil.renderPolygon(graphics, area, colour, fill, OUTLINE);
			if (config.doubleSpotLabels())
			{
				String text = spot.label();
				Point at = Perspective.getCanvasTextLocation(client, graphics, centre, text, 0);
				if (at != null)
				{
					OverlayUtil.renderTextLocation(graphics, at, text, colour);
				}
			}
		}
		return null;
	}
}
