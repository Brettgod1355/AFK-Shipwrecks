/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.function.Supplier;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.infobox.InfoBox;

/**
 * The time to a full hold as one of RuneLite's infoboxes, so it stays in view with the overlay
 * hidden. Shown only aboard the player's own boat with something to say. In place of a picture the
 * box carries the word AFK across its top, above the time RuneLite writes along the bottom (owner,
 * 2026-10-04: "think we can fit AFK above the time").
 */
public class HoldInfoBox extends InfoBox
{
	/** The word drawn across the top of the picture. */
	static final String LABEL = "AFK";
	/** Pixels left clear above the word. */
	private static final int LABEL_MARGIN = 1;
	/** The smallest picture worth drawing the word on; below it the box is text only. */
	static final int SMALLEST_LABELLED = 20;

	private final Supplier<AfkSession.View> view;
	private final Supplier<CargoHoldMonitor.Level> holdLevel;

	/**
	 * @param boxSize RuneLite's infobox size setting; the picture is drawn at this size so RuneLite
	 *                never scales it, which would thin or drop the word's strokes
	 */
	public HoldInfoBox(Plugin plugin, int boxSize, Supplier<AfkSession.View> view, Supplier<CargoHoldMonitor.Level> holdLevel)
	{
		super(label(boxSize), plugin);
		this.view = view;
		this.holdLevel = holdLevel;
		setTooltip("AFK Shipwrecks");
	}

	/**
	 * The box's picture: AFK in the game's small font along the top, with the shadow the game gives
	 * its text, and nothing below, so the time RuneLite writes along the bottom has the lower half
	 * to itself. Square, of the given side; a very small box gets a blank picture.
	 */
	static BufferedImage label(int size)
	{
		size = Math.max(2, size);
		BufferedImage picture = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		if (size < SMALLEST_LABELLED)
		{
			return picture;
		}
		Graphics2D g = picture.createGraphics();
		try
		{
			g.setFont(FontManager.getRunescapeSmallFont());
			FontMetrics metrics = g.getFontMetrics();
			int x = (size - metrics.stringWidth(LABEL)) / 2;
			int baseline = LABEL_MARGIN + metrics.getAscent();
			g.setColor(Color.BLACK);
			g.drawString(LABEL, x + 1, baseline + 1);
			g.setColor(Palette.TEXT);
			g.drawString(LABEL, x, baseline);
		}
		finally
		{
			g.dispose();
		}
		return picture;
	}

	@Override
	public boolean render()
	{
		return text(view.get(), holdLevel.get()) != null;
	}

	@Override
	public String getText()
	{
		AfkSession.View v = view.get();
		String text = text(v, holdLevel.get());
		setTooltip(tooltip(v, holdLevel.get()));
		return text == null ? "" : text;
	}

	@Override
	public Color getTextColor()
	{
		AfkSession.View v = view.get();
		CargoHoldMonitor.Level level = holdLevel.get();
		if (level == CargoHoldMonitor.Level.FULL || v.estimate.getState() == AfkEstimate.State.HOLD_FULL)
		{
			return Palette.BAD;
		}
		if (level == CargoHoldMonitor.Level.NEARLY_FULL || v.idleWarning
			|| v.estimate.getState() == AfkEstimate.State.WAITING_FOR_WRECK)
		{
			return Palette.ATTENTION;
		}
		return Palette.TEXT;
	}

	/** The short text, or null when the box should not be drawn. Package-private for tests. */
	static String text(AfkSession.View v, CargoHoldMonitor.Level level)
	{
		if (level == CargoHoldMonitor.Level.FULL)
		{
			return "Full";
		}
		switch (v.estimate.getState())
		{
			case HOLD_FULL:
			case HOLD_FULL_UNCONFIRMED:
				return "Full";
			case COUNTING_DOWN:
			case INVENTORY_FILLS_FIRST:
			case WRECK_SINKS_FIRST:
			case STALLED:
				return Durations.tiny(v.countdownMillis);
			case WAITING_FOR_WRECK:
				// Only at a spot; on the way there the box has nothing to say (owner, 2026-10-04).
				return v.atSpot ? "Wait" : null;
			case NOT_SAILING:
			case NO_HOOK:
				return null;
			default:
				return "?";
		}
	}

	static String tooltip(AfkSession.View v, CargoHoldMonitor.Level level)
	{
		if (level == CargoHoldMonitor.Level.FULL || v.estimate.getState() == AfkEstimate.State.HOLD_FULL)
		{
			return "AFK Shipwrecks: the cargo hold is full";
		}
		switch (v.estimate.getState())
		{
			case HOLD_FULL_UNCONFIRMED:
				return "AFK Shipwrecks: hold full by the tally; open it to check";
			case COUNTING_DOWN:
			case INVENTORY_FILLS_FIRST:
			case WRECK_SINKS_FIRST:
			case STALLED:
				return "AFK Shipwrecks: hold full in " + (v.estimate.isApproximate() ? "~" : "")
					+ Durations.coarse(v.countdownMillis)
					+ (v.idleWarning ? "; idle logout in " + Durations.countdown(v.idleLogoutMillis) : "");
			case WAITING_FOR_WRECK:
				return "AFK Shipwrecks: waiting for a wreck";
			case HOLD_UNKNOWN:
				return "AFK Shipwrecks: open the cargo hold once to start the timer";
			default:
				return "AFK Shipwrecks: " + v.estimate.getState().name().toLowerCase().replace('_', ' ');
		}
	}
}
