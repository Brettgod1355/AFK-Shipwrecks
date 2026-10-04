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
 * A flashing notice in the middle of the screen while a deckhandier crewmate sits idle and a
 * weaker one works a hook (owner, 2026-10-03: "a different overlay thats flashing to notify us
 * when we should swap npc's on the hook"). It stands, in the reminder banner's colour, from the
 * moment the swap is announced until it is made or the pair breaks up; a setting, on by default.
 * It sits a little below the salvaging-world notice, so the two never cover each other.
 */
public class CrewSwapOverlay extends Overlay
{
	/** Pixels below the centre, clear of the salvaging-world notice. */
	private static final int BELOW_CENTRE = 90;

	private final Client client;
	private final AfkSalvagingPlugin plugin;
	private final AfkSalvagingConfig config;

	@Inject
	CrewSwapOverlay(Client client, AfkSalvagingPlugin plugin, AfkSalvagingConfig config)
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
		CrewSwapWatch.Suggestion swap = plugin.getSession().view().betterCrew;
		if (!config.swapAlert() || swap == null || !plugin.isOwnBoat())
		{
			return null;
		}
		return FlashingNotice.draw(graphics, client, "SWAP CREW",
			swap.in.shortName() + " for " + swap.out.shortName() + " (" + swap.inDeckhandiness + " vs " + swap.outDeckhandiness + ")",
			config.reminderBannerColor(), config.bannerScale() / 100f, System.currentTimeMillis(), BELOW_CENTRE);
	}
}
