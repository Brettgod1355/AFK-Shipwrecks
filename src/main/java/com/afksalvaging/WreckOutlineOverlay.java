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
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.Skill;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;
import net.runelite.client.ui.overlay.outline.ModelOutlineRenderer;

/**
 * Marks every wreck in view that the player's Sailing level lets them salvage: cyan while it is up, gray
 * once it has sunk (owner, 2026-10-07). Wrecks above the player's level get nothing; the colors are fixed.
 * <ul>
 * <li>Tile (the default since 2.2): a box on the tiles the wreck stands on (owner, 2026-10-14: "a small
 * tile highlight ... so it shows if it's active or inactive"). Far less work per frame than the hull.</li>
 * <li>Hull: the wreck's model outlined, the way the game outlines a wreck under the mouse (owner,
 * 2026-10-03: "a nice indicator without being too much mess of overlays on screen").</li>
 * </ul>
 */
public class WreckOutlineOverlay extends Overlay
{
	private static final int WIDTH = 2;
	private static final int FEATHER = 2;
	private static final Stroke TILE_OUTLINE = new BasicStroke(2);

	private final Client client;
	private final AfkSalvagingPlugin plugin;
	private final AfkSalvagingConfig config;
	private final ModelOutlineRenderer outlines;

	@Inject
	WreckOutlineOverlay(Client client, AfkSalvagingPlugin plugin, AfkSalvagingConfig config, ModelOutlineRenderer outlines)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		this.outlines = outlines;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
		setPriority(PRIORITY_LOW);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		WreckMarking marking = config.wreckMarking();
		if (!(marking.hull() || marking.tile()) || !plugin.onSalvagingBoat())
		{
			return null;
		}
		int level = client.getBoostedSkillLevel(Skill.SAILING);
		for (WreckTracker.Site site : plugin.getSession().wrecks().presentSites())
		{
			if (site.getType().getSailingLevel() > level)
			{
				continue;
			}
			// A sunk site's object is the stump the next wreck rises from; it is outlined the same way.
			GameObject wreck = plugin.wreckObject(site.getPoint());
			if (wreck == null)
			{
				continue;
			}
			Color color = site.isActive() ? Palette.WRECK_OUTLINE : Palette.WRECK_SUNK;
			if (marking.hull())
			{
				outlines.drawOutline(wreck, WIDTH, color, FEATHER);
			}
			else
			{
				drawTiles(graphics, wreck, color);
			}
		}
		return null;
	}

	/** A box on the tiles the wreck stands on, filled faintly when "Fill the boxes" is on. */
	private void drawTiles(Graphics2D graphics, GameObject wreck, Color color)
	{
		Polygon tiles = wreck.getCanvasTilePoly();
		if (tiles == null)
		{
			return;
		}
		Color fill = new Color(color.getRed(), color.getGreen(), color.getBlue(), config.boxFill() ? 40 : 0);
		OverlayUtil.renderPolygon(graphics, tiles, color, fill, TILE_OUTLINE);
	}
}
