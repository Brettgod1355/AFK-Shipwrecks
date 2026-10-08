/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.List;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

/**
 * Writes a timer in the middle of every wreck in view, ours and the ones nearby (owner, 2026-10-08):
 * cyan while the wreck is up, grey once it has sunk. The colour only says up or sunk; it does not
 * change as the time runs down (owner: "It just needs to change colors if it's active versus
 * inactive"). The words come from {@link WreckTimers}. A setting, on by default.
 */
public class WreckTimerOverlay extends Overlay
{
	/** How high above the water the words sit, in local units: inside the hull rather than on the sea. */
	private static final int HEIGHT = 120;

	private final Client client;
	private final AfkSalvagingPlugin plugin;
	private final AfkSalvagingConfig config;

	@Inject
	WreckTimerOverlay(Client client, AfkSalvagingPlugin plugin, AfkSalvagingConfig config)
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
		if (!config.wreckTimers() || !plugin.isOwnBoat())
		{
			return null;
		}
		List<WreckTracker.Site> present = plugin.getSession().wrecks().presentSites();
		long now = AfkSalvagingPlugin.clock();
		for (WreckTracker.Site site : present)
		{
			GameObject wreck = plugin.wreckObject(site.getPoint());
			if (wreck == null)
			{
				continue;
			}
			WreckTimers.Timer timer = WreckTimers.of(site, present, now);
			Point at = Perspective.getCanvasTextLocation(client, graphics, wreck.getLocalLocation(), timer.text, HEIGHT);
			if (at != null)
			{
				Color colour = timer.up ? Palette.WRECK_OUTLINE : Palette.WRECK_SUNK_TEXT;
				OverlayUtil.renderTextLocation(graphics, at, timer.text, colour);
			}
		}
		return null;
	}
}
