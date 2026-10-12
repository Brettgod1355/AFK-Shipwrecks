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
import java.awt.Shape;
import javax.inject.Inject;
import net.runelite.api.GameObject;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

/**
 * Outlines where the salvage goes next on the player's own boat: the cargo hold in the Deposit color, the
 * salvaging station and a hook in fixed colors (owner, 2026-10-12). What to outline is worked out once a
 * tick by the plugin ({@link SalvageGuide}); this only draws it. Replaces 2.1's always-on hold outline.
 */
public class SalvageGuideOverlay extends Overlay
{
	private static final BasicStroke NORMAL = new BasicStroke(2);
	/** A full inventory: hooked salvage is being lost, so the outline is thicker and filled brighter. */
	private static final BasicStroke STRONG = new BasicStroke(4);
	private static final int NORMAL_FILL_ALPHA = 25;
	private static final int STRONG_FILL_ALPHA = 70;

	private final AfkSalvagingPlugin plugin;
	private final AfkSalvagingConfig config;

	@Inject
	SalvageGuideOverlay(AfkSalvagingPlugin plugin, AfkSalvagingConfig config)
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
		SalvageGuide.Result guide = plugin.guide();
		if (guide.isEmpty())
		{
			return null;
		}
		boolean strong = guide.isStrong();
		if (guide.shows(SalvageGuide.Target.HOLD))
		{
			outline(graphics, plugin.cargoHoldObject(), config.holdColor(), strong);
		}
		if (guide.shows(SalvageGuide.Target.STATION))
		{
			for (GameObject station : plugin.stationObjects())
			{
				outline(graphics, station, Palette.GUIDE_STATION, strong);
			}
		}
		if (guide.shows(SalvageGuide.Target.HOOK))
		{
			outline(graphics, plugin.hookObject(guide.getHookHash()), Palette.GUIDE_HOOK, false);
		}
		return null;
	}

	private static void outline(Graphics2D graphics, GameObject object, Color color, boolean strong)
	{
		Shape hull = object == null ? null : object.getConvexHull();
		if (hull == null)
		{
			return;
		}
		Color fill = new Color(color.getRed(), color.getGreen(), color.getBlue(), strong ? STRONG_FILL_ALPHA : NORMAL_FILL_ALPHA);
		OverlayUtil.renderPolygon(graphics, hull, color, fill, strong ? STRONG : NORMAL);
	}
}
