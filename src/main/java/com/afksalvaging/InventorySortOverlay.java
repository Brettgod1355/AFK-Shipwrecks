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
 * A coloured box round each inventory item saying what to do with it: keep, hold, alch or drop.
 * Drawn only where the settings say, by default on the player's own boat at a salvaging spot.
 */
public class InventorySortOverlay extends WidgetItemOverlay
{
	private static final Stroke OUTLINE = new BasicStroke(2);

	private final AfkSalvagingPlugin plugin;
	private final AfkSalvagingConfig config;

	@Inject
	InventorySortOverlay(AfkSalvagingPlugin plugin, AfkSalvagingConfig config)
	{
		this.plugin = plugin;
		this.config = config;
		showOnInventory();
		// The cargo hold's own inventory panel too, so what to deposit stands out while the hold is open (owner, 2026-10-03).
		showOnInterfaces(InterfaceID.SAILING_BOAT_CARGOHOLD_SIDE);
	}

	@Override
	public void renderItemOverlay(Graphics2D graphics, int itemId, WidgetItem item)
	{
		if (!plugin.inventorySortActive() || SalvageSorter.excluded(itemId))
		{
			return;
		}
		SortRule rule = plugin.sortRule(itemId);
		if (rule == null || (rule == SortRule.KEEP && !config.boxKeep()))
		{
			return;
		}
		// With the hold open only the deposits are boxed: that is what the panel beside it is for (owner, 2026-10-03).
		if (rule != SortRule.HOLD && plugin.isCargoInterfaceOpen())
		{
			return;
		}
		Rectangle bounds = item.getCanvasBounds();
		if (bounds == null)
		{
			return;
		}
		Color colour = colour(rule);
		if (rule != SortRule.KEEP)
		{
			// Keep is an outline only (owner, 2026-10-03): what stays should not be tinted like what goes.
			graphics.setColor(new Color(colour.getRed(), colour.getGreen(), colour.getBlue(), Math.min(colour.getAlpha(), 50)));
			graphics.fill(bounds);
		}
		Stroke old = graphics.getStroke();
		graphics.setStroke(OUTLINE);
		graphics.setColor(colour);
		graphics.draw(bounds);
		graphics.setStroke(old);
	}

	private Color colour(SortRule rule)
	{
		switch (rule)
		{
			case KEEP:
				return config.keepColor();
			case HOLD:
				return config.holdColor();
			case ALCH:
				return config.alchColor();
			default:
				return config.dropColor();
		}
	}
}
