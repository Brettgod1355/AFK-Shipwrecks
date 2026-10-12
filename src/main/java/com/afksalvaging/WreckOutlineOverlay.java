/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.Skill;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.outline.ModelOutlineRenderer;

/**
 * Outlines the hull of every wreck in view that the player's Sailing level lets them salvage, the
 * way the game outlines a wreck under the mouse (owner, 2026-10-03: "a nice indicator without being
 * too much mess of overlays on screen"): cyan while it is up, gray once it has sunk (owner,
 * 2026-10-07). Wrecks above the player's level get nothing. Drawn when "Mark wrecks" includes the
 * hull; the colors are fixed.
 */
public class WreckOutlineOverlay extends Overlay
{
	private static final int WIDTH = 2;
	private static final int FEATHER = 2;

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
		if (!config.wreckMarking().hull() || !plugin.onSalvagingBoat())
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
			if (wreck != null)
			{
				outlines.drawOutline(wreck, WIDTH, site.isActive() ? Palette.WRECK_OUTLINE : Palette.WRECK_SUNK, FEATHER);
			}
		}
		return null;
	}
}
