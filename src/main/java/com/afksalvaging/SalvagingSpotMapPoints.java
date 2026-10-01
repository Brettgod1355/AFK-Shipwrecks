/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.EnumMap;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.ui.overlay.worldmap.WorldMapPoint;
import net.runelite.client.ui.overlay.worldmap.WorldMapPointManager;

/**
 * Puts a named, levelled marker for every salvaging hotspot on the world map.
 * <p>
 * Hovering one reads, for example, "Barracuda salvage - Level 35" and where it is. The spot picked
 * in the sidebar gets a bigger marker that sticks to the edge of the map when it is off screen, so
 * it can be found from anywhere. The markers are drawn here rather than loaded, so the plugin
 * bundles no images.
 */
@Singleton
public class SalvagingSpotMapPoints
{
	private static final int SIZE = 14;
	private static final int PICKED_SIZE = 20;
	private static final Color RING = new Color(20, 20, 20, 220);
	private static final Color BELOW_LEVEL = new Color(140, 140, 140);
	private static final Color IN_LEVEL = new Color(70, 200, 110);
	private static final Color PICKED = new Color(255, 190, 40);

	private final WorldMapPointManager manager;
	private final Map<SalvagingSpot, WorldMapPoint> points = new EnumMap<>(SalvagingSpot.class);
	private boolean shown;
	private int sailingLevel;
	private SalvagingSpot picked;

	@Inject
	SalvagingSpotMapPoints(WorldMapPointManager manager)
	{
		this.manager = manager;
	}

	/** Puts the markers up or takes them down. */
	public void setShown(boolean show)
	{
		if (show == shown)
		{
			return;
		}
		shown = show;
		if (show)
		{
			for (SalvagingSpot spot : SalvagingSpot.values())
			{
				WorldMapPoint point = WorldMapPoint.builder()
					.worldPoint(spot.getPoint())
					.target(spot.getPoint())
					.image(image(spot))
					.name(spot.getSalvageName())
					.tooltip(spot.getTooltip())
					.jumpOnClick(true)
					.snapToEdge(spot == picked)
					.build();
				points.put(spot, point);
				manager.add(point);
			}
		}
		else
		{
			for (WorldMapPoint point : points.values())
			{
				manager.remove(point);
			}
			points.clear();
		}
	}

	/** Recolours the markers for the player's level: green within reach, grey above it. */
	public void setSailingLevel(int level)
	{
		if (level == sailingLevel)
		{
			return;
		}
		sailingLevel = level;
		redraw();
	}

	/** Marks the spot the sidebar picked, or none, and points at it from the map's edge. */
	public void setPicked(SalvagingSpot spot)
	{
		if (spot == picked)
		{
			return;
		}
		picked = spot;
		redraw();
	}

	public SalvagingSpot getPicked()
	{
		return picked;
	}

	private void redraw()
	{
		for (Map.Entry<SalvagingSpot, WorldMapPoint> entry : points.entrySet())
		{
			WorldMapPoint point = entry.getValue();
			point.setImage(image(entry.getKey()));
			point.setSnapToEdge(entry.getKey() == picked);
		}
	}

	private BufferedImage image(SalvagingSpot spot)
	{
		boolean isPicked = spot == picked;
		int size = isPicked ? PICKED_SIZE : SIZE;
		Color colour = isPicked ? PICKED : (sailingLevel >= spot.getSailingLevel() ? IN_LEVEL : BELOW_LEVEL);
		BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		try
		{
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setColor(colour);
			g.fillOval(1, 1, size - 2, size - 2);
			g.setColor(RING);
			g.setStroke(new BasicStroke(isPicked ? 2 : 1));
			g.drawOval(1, 1, size - 3, size - 3);
			// A small anchor-ring in the middle so the dot reads as a mooring, not a player.
			g.setColor(Color.WHITE);
			int hole = size / 4;
			g.fillOval((size - hole) / 2, (size - hole) / 2, hole, hole);
		}
		finally
		{
			g.dispose();
		}
		return image;
	}
}
