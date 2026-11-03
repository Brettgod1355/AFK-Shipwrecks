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
import java.util.Objects;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * Darkens the game while {@link Dimmer} says nothing needs the player (2.5). Drawn under the game's windows for
 * "Game view", over them otherwise. The plugin's banners, info box and inventory boxes are drawn after it and stay
 * bright; its drawings on the deck (wreck boxes and timers, the hold count, the next-step outlines) are drawn with the
 * game world, before it, so they dim with the game.
 * With every "While" switch off it does nothing at all, and the area it darkens is kept until the canvas, the game
 * view, the chat box or "Dimmer covers" changes (the PR audit on AFK-Shipwrecks#22, 2026-11-01).
 */
public class DimmerOverlay extends Overlay
{
	/** The chat box in each window layout; the first one shown is the one "All but chat" leaves bright. */
	private static final int[] CHAT = {InterfaceID.Toplevel.CHAT_CONTAINER, InterfaceID.ToplevelOsrsStretch.CHAT_CONTAINER,
		InterfaceID.ToplevelPreEoc.CHAT_CONTAINER, InterfaceID.Chatbox.UNIVERSE};

	private final Client client;
	private final AfkSalvagingPlugin plugin;
	private final AfkSalvagingConfig config;

	/** Whether any "While" switch is on, and what the dimmer covers: read when a Dimmer setting changes. */
	private boolean on;
	private DimmerCovers covers = DimmerCovers.GAME_VIEW;

	/** The area last worked out, and what it was worked out from. */
	private Area area;
	private DimmerCovers areaCovers;
	private final Rectangle areaCanvas = new Rectangle();
	private final Rectangle areaViewport = new Rectangle();
	private Rectangle areaChat;

	@Inject
	DimmerOverlay(Client client, AfkSalvagingPlugin plugin, AfkSalvagingConfig config)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.DYNAMIC);
		setPriority(PRIORITY_LOW);
		configure();
	}

	/**
	 * Reads the Dimmer settings and picks the layer for "Dimmer covers". RuneLite files an overlay under its layer
	 * when it is added, so the plugin removes and adds it again after that one changes.
	 */
	void configure()
	{
		on = Dimmer.anyOn(config);
		covers = config.dimmerCovers();
		setLayer(covers == DimmerCovers.GAME_VIEW ? OverlayLayer.UNDER_WIDGETS : OverlayLayer.ABOVE_WIDGETS);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		// The cargo hold or another window opening clears it at once, not at the next tick, and so does anything but
		// being logged in (a dropped connection runs no ticks).
		if (!on || !plugin.dimmed() || plugin.mainInterfaceOpen() || client.getGameState() != GameState.LOGGED_IN)
		{
			return null;
		}
		Area dark = area();
		if (dark == null)
		{
			return null;
		}
		Color before = graphics.getColor();
		graphics.setColor(config.dimmerColor());
		graphics.fill(dark);
		graphics.setColor(before);
		return null;
	}

	/** The area to darken, worked out again only when what it depends on has changed. */
	private Area area()
	{
		int width = client.getCanvasWidth();
		int height = client.getCanvasHeight();
		int x = client.getViewportXOffset();
		int y = client.getViewportYOffset();
		int viewWidth = client.getViewportWidth();
		int viewHeight = client.getViewportHeight();
		Rectangle chat = covers == DimmerCovers.ALL_BUT_CHAT ? chat() : null;
		if (covers != areaCovers || width != areaCanvas.width || height != areaCanvas.height || x != areaViewport.x
			|| y != areaViewport.y || viewWidth != areaViewport.width || viewHeight != areaViewport.height
			|| !Objects.equals(chat, areaChat))
		{
			areaCovers = covers;
			areaCanvas.setBounds(0, 0, width, height);
			areaViewport.setBounds(x, y, viewWidth, viewHeight);
			areaChat = chat;
			area = Dimmer.area(covers, areaCanvas, areaViewport, chat);
		}
		return area;
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
