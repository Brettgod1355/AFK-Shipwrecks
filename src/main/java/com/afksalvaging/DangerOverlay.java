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
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * A flashing red DANGER notice above the middle of the screen while the boat is being damaged, the
 * player hit, or poisoned at a salvaging spot (owner, 2026-10-08). The sound comes with the "Danger"
 * notification. Above the centre, clear of the salvaging-world notice (centre) and the swap (below).
 */
public class DangerOverlay extends Overlay
{
	private static final int ABOVE_CENTRE = -90;

	private final Client client;
	private final AfkSalvagingPlugin plugin;
	private final AfkSalvagingConfig config;

	@Inject
	DangerOverlay(Client client, AfkSalvagingPlugin plugin, AfkSalvagingConfig config)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
		setPriority(PRIORITY_HIGH);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		String danger = plugin.getSession().view().danger;
		if (!config.dangerAlert() || danger == null || !plugin.isOwnBoat())
		{
			return null;
		}
		// Shown even over an open interface: danger matters more than tidiness.
		return FlashingNotice.draw(graphics, client, "DANGER", danger, config.bannerColor(),
			config.bannerScale() / 100f, System.currentTimeMillis(), ABOVE_CENTRE);
	}
}
