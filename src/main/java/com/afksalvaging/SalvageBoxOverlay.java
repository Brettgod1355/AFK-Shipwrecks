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
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

/**
 * Boxes the water so you know where to park: a yellow box around each wreck for where a hook can
 * reach it, and a green box where a hook reaches two wrecks at once.
 * <p>
 * The game measures reach from the hook, not the boat, so a box is where a hook has to sit. Your
 * own hooks are known, so a double spot box changes colour and says "Parked" once every hook on the
 * boat is inside it; with only some of them in, the label counts them and the colour stays.
 * Wrecks live in the top-level world even while the player stands on a boat, so the boxes are placed
 * with the top-level world view's coordinates and drawn above the scene.
 */
public class SalvageBoxOverlay extends Overlay
{
	private static final Stroke OUTLINE = new BasicStroke(2);
	private static final Stroke PARKED_OUTLINE = new BasicStroke(3);
	/** How much dimmer a box is when its wreck, or one of its pair, has sunk. */
	private static final float DIM = 0.45f;

	private final Client client;
	private final AfkSalvagingPlugin plugin;
	private final AfkSalvagingConfig config;

	@Inject
	SalvageBoxOverlay(Client client, AfkSalvagingPlugin plugin, AfkSalvagingConfig config)
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
		boolean reachBoxes = config.wreckReachBoxes();
		if (mode == DoubleSpotMode.OFF && !reachBoxes)
		{
			return null;
		}
		WorldView top = client.getTopLevelWorldView();
		if (top == null)
		{
			return null;
		}
		List<WreckTracker.Site> sites = plugin.getSession().wrecks().presentSites();
		if (sites.isEmpty())
		{
			return null;
		}
		int reach = AfkSession.HOOK_RANGE;

		if (reachBoxes)
		{
			Color colour = config.wreckReachColor();
			for (WreckTracker.Site site : sites)
			{
				DoubleSpot.Box box = DoubleSpot.reachOverlap(site.getPoint(), site.getPoint(), reach);
				drawBox(graphics, top, box, site.isActive() ? colour : dim(colour), OUTLINE, null);
			}
		}

		if (mode != DoubleSpotMode.OFF)
		{
			Color colour = config.doubleSpotColor();
			List<WorldPoint> hooks = plugin.hookPoints();
			for (DoubleSpot spot : DoubleSpot.find(sites, reach, mode))
			{
				DoubleSpot.Box box = spot.getBox();
				int inside = 0;
				for (WorldPoint hook : hooks)
				{
					if (box.contains(hook))
					{
						inside++;
					}
				}
				// Parked means every hook the boat has is in the box: both on a sloop, the one on a raft or skiff.
				boolean parked = !hooks.isEmpty() && inside == hooks.size();
				Color drawn = parked ? config.doubleSpotParkedColor() : (spot.activeCount() == 2 ? colour : dim(colour));
				String label = null;
				if (config.doubleSpotLabels())
				{
					if (parked)
					{
						label = "Parked: " + spot.label();
					}
					else if (inside > 0)
					{
						label = spot.label() + " · " + inside + " of " + hooks.size() + " hooks in";
					}
					else
					{
						label = spot.label();
					}
				}
				drawBox(graphics, top, box, drawn, parked ? PARKED_OUTLINE : OUTLINE, label);
			}
		}
		return null;
	}

	private void drawBox(Graphics2D graphics, WorldView top, DoubleSpot.Box box, Color colour, Stroke stroke, String label)
	{
		if (box == null)
		{
			return;
		}
		LocalPoint low = LocalPoint.fromWorld(top, box.getMinX(), box.getMinY());
		LocalPoint high = LocalPoint.fromWorld(top, box.getMaxX(), box.getMaxY());
		if (low == null || high == null)
		{
			// Part of the box is outside the loaded scene; it shows once the boat is closer.
			return;
		}
		LocalPoint centre = new LocalPoint((low.getX() + high.getX()) / 2, (low.getY() + high.getY()) / 2, top);
		Polygon area = Perspective.getCanvasTileAreaPoly(client, centre, box.width(), box.height(), box.getPlane(), 0);
		if (area == null)
		{
			return;
		}
		Color fill = new Color(colour.getRed(), colour.getGreen(), colour.getBlue(),
			stroke == PARKED_OUTLINE ? colour.getAlpha() / 2 : colour.getAlpha() / 5);
		OverlayUtil.renderPolygon(graphics, area, colour, fill, stroke);
		if (label != null)
		{
			Point at = Perspective.getCanvasTextLocation(client, graphics, centre, label, 0);
			if (at != null)
			{
				OverlayUtil.renderTextLocation(graphics, at, label, colour);
			}
		}
	}

	private static Color dim(Color colour)
	{
		return new Color(colour.getRed(), colour.getGreen(), colour.getBlue(), Math.round(colour.getAlpha() * DIM));
	}
}
