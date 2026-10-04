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
 * A flashing notice in the middle of the screen while the player sits at a salvaging spot with
 * no wreck up on a world that is not a salvaging world (owner, 2026-10-03): on those worlds every
 * site is worked and the next wreck comes sooner. The chat tip says it once; this keeps saying it
 * until a wreck is up or the player hops. A setting, on by default.
 */
public class SalvagingWorldOverlay extends Overlay
{
	/** How long the spot has to have been bare before the notice starts: a wreck often rises again within seconds. */
	static final long AFTER_MS = 15_000;

	private final Client client;
	private final AfkSalvagingPlugin plugin;
	private final AfkSalvagingConfig config;
	/** When the spot was first seen bare on this world, or -1 while it is not. */
	private long bareSince = -1;

	@Inject
	SalvagingWorldOverlay(Client client, AfkSalvagingPlugin plugin, AfkSalvagingConfig config)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
		setPriority(PRIORITY_HIGH);
	}

	/** Starts the wait afresh, as when the plugin is switched back on (review, 2026-10-04). */
	void reset()
	{
		bareSince = -1;
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		// At a spot means a wreck site, up or sunk, is within reach of a hook (a dock beside the spots
		// has sites in view but none in reach); bare means no wreck the player can work is up.
		// Nothing else is asked, so it shows with or without crew and before the hold was ever opened.
		AfkSession.View view = plugin.getSession().view();
		boolean bare = config.worldAlert() && !plugin.isSalvagingWorld() && plugin.isOwnBoat()
			&& view.atSpot && view.wrecksUp == 0;
		long now = System.currentTimeMillis();
		if (!bare)
		{
			bareSince = -1;
			return null;
		}
		if (bareSince < 0)
		{
			bareSince = now;
		}
		if (now - bareSince < AFTER_MS || plugin.mainInterfaceOpen())
		{
			return null;
		}
		return FlashingNotice.draw(graphics, client, "NOT A SALVAGING WORLD",
			"Wrecks come back sooner on worlds " + plugin.salvagingWorldsText(),
			config.warningBannerColor(), config.bannerScale() / 100f, now, 0);
	}
}
