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
 * reach it (gray once the wreck has sunk), and a green box where a hook reaches two wrecks at once.
 * <p>
 * The game measures reach from the hook, not the boat, so a box is where a hook has to sit. Your
 * own hooks are known, so a double spot box changes color and says "Parked" once every hook on the
 * boat is inside it; with only some of them in, the label counts them and the color stays.
 * Wrecks live in the top-level world even while the player stands on a boat, so the boxes are placed
 * with the top-level world view's coordinates and drawn above the scene.
 */
public class SalvageBoxOverlay extends Overlay
{
	/** What the parked double spot says once its box has hidden. */
	static final String PARKED_TEXT = "Parked in double spot";
	private static final Stroke OUTLINE = new BasicStroke(2);
	private static final Stroke PARKED_OUTLINE = new BasicStroke(3);

	private final Client client;
	private final AfkSalvagingPlugin plugin;
	private final AfkSalvagingConfig config;

	/** The double spots last found, and the wreck list they were found from. */
	private List<WreckTracker.Site> doublesFor;
	private List<DoubleSpot> cachedDoubles;

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
		boolean doubleSpots = config.doubleSpotBoxes();
		boolean reachBoxes = config.wreckMarking().area();
		if ((!doubleSpots && !reachBoxes) || !plugin.onSalvagingBoat())
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
		List<WorldPoint> hooks = plugin.hookPoints();
		// Found whether or not the green boxes are drawn: parked is parked either way. Worked out again only
		// when the wrecks in view change: the tracker hands back the same list until then.
		if (sites != doublesFor)
		{
			doublesFor = sites;
			cachedDoubles = DoubleSpot.find(sites, reach);
		}
		List<DoubleSpot> doubles = cachedDoubles;
		int[] hooksInside = new int[doubles.size()];
		boolean parkedSomewhere = false;
		for (int i = 0; i < doubles.size(); i++)
		{
			DoubleSpot.Box box = doubles.get(i).getBox();
			for (WorldPoint hook : hooks)
			{
				if (box.contains(hook))
				{
					hooksInside[i]++;
				}
			}
			// Parked means every hook the boat has is in the box: both on a sloop, the one on a raft or skiff.
			parkedSomewhere |= !hooks.isEmpty() && hooksInside[i] == hooks.size();
		}

		// Once parked the yellow boxes have done their job and only clutter the view (owner, 2026-10-03).
		if (reachBoxes && !(parkedSomewhere && config.hideReachWhenParked()))
		{
			Color color = config.wreckReachColor();
			for (WreckTracker.Site site : sites)
			{
				DoubleSpot.Box box = DoubleSpot.reachOverlap(site.getPoint(), site.getPoint(), reach);
				drawBox(graphics, top, box, site.isActive() ? color : sunk(color), OUTLINE, null);
			}
		}

		// Once the boat has stood still a while the green boxes have done their job (owner, 2026-10-03):
		// they go after the set time, at login too, and come back the moment the boat moves.
		int hideAfter = config.doubleSpotHideSeconds();
		boolean restingLongEnough = hideAfter > 0 && plugin.getSession().view().boatStillMillis >= hideAfter * 1000L;
		if (doubleSpots && !restingLongEnough)
		{
			Color color = config.doubleSpotColor();
			for (int i = 0; i < doubles.size(); i++)
			{
				DoubleSpot.Box box = doubles.get(i).getBox();
				int inside = hooksInside[i];
				boolean parked = !hooks.isEmpty() && inside == hooks.size();
				// The spot is a place: full color whether or not its wrecks are up right now.
				Color drawn = parked ? config.doubleSpotParkedColor() : color;
				String label = null;
				if (config.doubleSpotLabels())
				{
					// Owner, 2026-10-02: label the box where to park simply "Double spot".
					if (parked)
					{
						label = "Double spot - Parked";
					}
					else if (inside > 0)
					{
						label = "Double spot - " + inside + " of " + hooks.size() + " hooks in";
					}
					else
					{
						label = "Double spot";
					}
				}
				// A label about the boat (hooks in, parked) sits in the middle of the boat; "Double spot" for a
				// box the boat is not in sits in the middle of the box (owner, 2026-10-04).
				LocalPoint boat = inside > 0 ? plugin.ownBoatLocation() : null;
				drawBox(graphics, top, box, drawn, parked ? PARKED_OUTLINE : OUTLINE, boat == null ? label : null);
				if (boat != null && label != null)
				{
					drawText(graphics, boat, label, opaque(drawn));
				}
			}
		}
		else if (doubleSpots && config.parkedTextAfterHide() && !hooks.isEmpty())
		{
			// The boxes have hidden, but the boat is still parked in one: say so where its label was,
			// with no box (owner, 2026-10-04). Full strength, as a faint parked color is a box fill.
			Color text = opaque(config.doubleSpotParkedColor());
			LocalPoint boat = plugin.ownBoatLocation();
			for (int i = 0; i < doubles.size(); i++)
			{
				if (hooksInside[i] == hooks.size())
				{
					if (boat != null)
					{
						drawText(graphics, boat, PARKED_TEXT, text);
					}
					else
					{
						drawLabel(graphics, top, doubles.get(i).getBox(), PARKED_TEXT, text);
					}
					break;
				}
			}
		}
		return null;
	}

	/** Words at a point in the scene, such as the middle of the boat. */
	private void drawText(Graphics2D graphics, LocalPoint at, String text, Color color)
	{
		Point where = Perspective.getCanvasTextLocation(client, graphics, at, text, 0);
		if (where != null)
		{
			OverlayUtil.renderTextLocation(graphics, where, text, color);
		}
	}

	/** The color with no transparency: a box fill may be faint, its words must not be. */
	private static Color opaque(Color color)
	{
		return new Color(color.getRed(), color.getGreen(), color.getBlue());
	}

	/** Words where a double spot box's label goes, at its middle, with no box. */
	private void drawLabel(Graphics2D graphics, WorldView top, DoubleSpot.Box box, String text, Color color)
	{
		if (box == null)
		{
			return;
		}
		LocalPoint low = LocalPoint.fromWorld(top, box.getMinX(), box.getMinY());
		LocalPoint high = LocalPoint.fromWorld(top, box.getMaxX(), box.getMaxY());
		if (low == null || high == null)
		{
			return;
		}
		LocalPoint center = new LocalPoint((low.getX() + high.getX()) / 2, (low.getY() + high.getY()) / 2, top);
		Point at = Perspective.getCanvasTextLocation(client, graphics, center, text, 0);
		if (at != null)
		{
			OverlayUtil.renderTextLocation(graphics, at, text, color);
		}
	}

	private void drawBox(Graphics2D graphics, WorldView top, DoubleSpot.Box box, Color color, Stroke stroke, String label)
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
		LocalPoint center = new LocalPoint((low.getX() + high.getX()) / 2, (low.getY() + high.getY()) / 2, top);
		Polygon area = Perspective.getCanvasTileAreaPoly(client, center, box.width(), box.height(), box.getPlane(), 0);
		if (area == null)
		{
			return;
		}
		// "Fill the boxes" off draws outlines only (owner, 2026-10-08; the Sailing plugin's most asked-for option).
		int fillAlpha = !config.boxFill() ? 0 : stroke == PARKED_OUTLINE ? color.getAlpha() / 2 : color.getAlpha() / 5;
		Color fill = new Color(color.getRed(), color.getGreen(), color.getBlue(), fillAlpha);
		OverlayUtil.renderPolygon(graphics, area, color, fill, stroke);
		if (label != null)
		{
			Point at = Perspective.getCanvasTextLocation(client, graphics, center, label, 0);
			if (at != null)
			{
				OverlayUtil.renderTextLocation(graphics, at, label, color);
			}
		}
	}

	/** A sunk wreck's box: gray, as strong as the reach color the player picked (owner, 2026-10-07). */
	static Color sunk(Color color)
	{
		Color gray = Palette.WRECK_SUNK;
		return new Color(gray.getRed(), gray.getGreen(), gray.getBlue(), color.getAlpha());
	}
}
