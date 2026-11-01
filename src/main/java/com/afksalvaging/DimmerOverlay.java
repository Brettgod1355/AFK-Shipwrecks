/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Area;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * Darkens the game while {@link Dimmer} says nothing needs the player (2.5). Drawn under the game's windows for
 * "Game view", over them otherwise; the plugin's own banners and boxes are drawn after it, so they stay bright.
 */
public class DimmerOverlay extends Overlay
{
	/** The chat box in each window layout; the first one shown is the one "All but chat" leaves bright. */
	private static final int[] CHAT = {InterfaceID.Toplevel.CHAT_CONTAINER, InterfaceID.ToplevelOsrsStretch.CHAT_CONTAINER,
		InterfaceID.ToplevelPreEoc.CHAT_CONTAINER, InterfaceID.Chatbox.UNIVERSE};

	private final Client client;
	private final AfkSalvagingPlugin plugin;
	private final AfkSalvagingConfig config;

	@Inject
	DimmerOverlay(Client client, AfkSalvagingPlugin plugin, AfkSalvagingConfig config)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.DYNAMIC);
		setPriority(PRIORITY_LOW);
		useLayer();
	}

	/**
	 * The layer for the chosen "Dimmer covers". RuneLite files an overlay under its layer when it is added, so the
	 * plugin removes and adds it again after a change.
	 */
	void useLayer()
	{
		setLayer(config.dimmerCovers() == DimmerCovers.GAME_VIEW ? OverlayLayer.UNDER_WIDGETS : OverlayLayer.ABOVE_WIDGETS);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!plugin.dimmed())
		{
			return null;
		}
		Rectangle canvas = new Rectangle(client.getCanvasWidth(), client.getCanvasHeight());
		Rectangle viewport = new Rectangle(client.getViewportXOffset(), client.getViewportYOffset(),
			client.getViewportWidth(), client.getViewportHeight());
		Area area = Dimmer.area(config.dimmerCovers(), canvas, viewport, chat());
		if (area == null)
		{
			return null;
		}
		Color before = graphics.getColor();
		graphics.setColor(config.dimmerColor());
		graphics.fill(area);
		graphics.setColor(before);
		return null;
	}

	/** Where the shown chat box is, or null when none is. */
	private Rectangle chat()
	{
		for (int id : CHAT)
		{
			Widget widget = client.getWidget(id);
			if (widget != null && !widget.isHidden())
			{
				return widget.getBounds();
			}
		}
		return null;
	}
}
