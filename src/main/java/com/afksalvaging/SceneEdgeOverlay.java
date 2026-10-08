/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Stroke;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * A faint dashed square round the area the game has loaded, which is everything the plugin can know
 * about (owner, 2026-10-08: "a big outline square that's not filled in ... that shows what area the
 * plugin can see", so a player can tell whether a far set of wrecks is in it). The square is the
 * top-level scene, not a radius round the player, and it jumps when the game loads a new one. A
 * setting, off by default.
 */
public class SceneEdgeOverlay extends Overlay
{
	private static final Stroke DASHED = new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f,
		new float[]{12f, 10f}, 0f);

	private final Client client;
	private final AfkSalvagingPlugin plugin;
	private final AfkSalvagingConfig config;

	@Inject
	SceneEdgeOverlay(Client client, AfkSalvagingPlugin plugin, AfkSalvagingConfig config)
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
		if (!config.showSceneEdge() || !plugin.isOwnBoat())
		{
			return null;
		}
		WorldView top = client.getTopLevelWorldView();
		if (top == null)
		{
			return null;
		}
		int last = Perspective.LOCAL_TILE_SIZE - 1;
		int maxX = top.getSizeX() * Perspective.LOCAL_TILE_SIZE - 1;
		int maxY = top.getSizeY() * Perspective.LOCAL_TILE_SIZE - 1;
		int plane = top.getPlane();
		Stroke before = graphics.getStroke();
		graphics.setStroke(DASHED);
		graphics.setColor(Palette.SCENE_EDGE);
		// Each side one tile at a time: a corner far off or behind the camera has no place on screen, but
		// the stretch of the side that is in view still draws.
		for (int i = 0; i < top.getSizeX(); i++)
		{
			int x0 = i * Perspective.LOCAL_TILE_SIZE;
			int x1 = Math.min(maxX, x0 + last + 1);
			segment(graphics, top, plane, x0, 0, x1, 0);
			segment(graphics, top, plane, x0, maxY, x1, maxY);
		}
		for (int j = 0; j < top.getSizeY(); j++)
		{
			int y0 = j * Perspective.LOCAL_TILE_SIZE;
			int y1 = Math.min(maxY, y0 + last + 1);
			segment(graphics, top, plane, 0, y0, 0, y1);
			segment(graphics, top, plane, maxX, y0, maxX, y1);
		}
		graphics.setStroke(before);
		return null;
	}

	private void segment(Graphics2D graphics, WorldView top, int plane, int x0, int y0, int x1, int y1)
	{
		Point a = Perspective.localToCanvas(client, new LocalPoint(x0, y0, top), plane);
		Point b = Perspective.localToCanvas(client, new LocalPoint(x1, y1, top), plane);
		if (a != null && b != null)
		{
			graphics.drawLine(a.getX(), a.getY(), b.getX(), b.getY());
		}
	}
}
