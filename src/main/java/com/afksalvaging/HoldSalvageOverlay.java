/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;
import javax.inject.Inject;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.ui.overlay.WidgetItemOverlay;

/**
 * Boxes the salvage inside the open cargo hold, so it is easy to see and click among the
 * cannonballs and kits (owner, 2026-10-03). Only the hold's own contents: the inventory beside it
 * is the sorting overlay's business.
 */
public class HoldSalvageOverlay extends WidgetItemOverlay
{
	private static final Stroke OUTLINE = new BasicStroke(2);

	private final AfkSalvagingConfig config;

	@Inject
	HoldSalvageOverlay(AfkSalvagingConfig config)
	{
		this.config = config;
		showOnInterfaces(InterfaceID.SAILING_BOAT_CARGOHOLD);
	}

	@Override
	public void renderItemOverlay(Graphics2D graphics, int itemId, WidgetItem item)
	{
		if (!config.boxHoldSalvage() || ShipwreckType.fromSalvageItemId(itemId) == null)
		{
			return;
		}
		Rectangle bounds = item.getCanvasBounds();
		if (bounds == null)
		{
			return;
		}
		Color colour = Palette.ATTENTION;
		graphics.setColor(new Color(colour.getRed(), colour.getGreen(), colour.getBlue(), 50));
		graphics.fill(bounds);
		Stroke old = graphics.getStroke();
		graphics.setStroke(OUTLINE);
		graphics.setColor(colour);
		graphics.draw(bounds);
		graphics.setStroke(old);
	}
}
