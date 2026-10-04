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
	/** The smallest picture the word fits on above the time RuneLite writes (its top sits about 13 px above the bottom); below it the box is text only. */
	static final int SMALLEST_LABELLED = 24;

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
		return colour(view.get(), holdLevel.get());
	}

	/** The text's colour: red when it is over or blocked, amber when it wants a look, dim when nothing is happening. */
	static Color colour(AfkSession.View v, CargoHoldMonitor.Level level)
	{
		if (level == CargoHoldMonitor.Level.FULL)
		{
			return Palette.BAD;
		}
		switch (v.estimate.getState())
		{
			case HOLD_FULL:
			case LEVEL_TOO_LOW:
			case HAZARDOUS:
			case STALLED:
				return Palette.BAD;
			case NOBODY_SALVAGING:
				return Palette.DIM;
			case WAITING_FOR_WRECK:
			case PLAYER_HOOK_IDLE:
			case INVENTORY_FULL:
			case INVENTORY_FILLS_FIRST:
			case WRECK_SINKS_FIRST:
			case HOLD_FULL_UNCONFIRMED:
			case HOLD_DRIFTED:
			case HOLD_UNKNOWN:
				return Palette.ATTENTION;
			default:
				return level == CargoHoldMonitor.Level.NEARLY_FULL || v.idleWarning ? Palette.ATTENTION : Palette.TEXT;
		}
	}

	/**
	 * The short text, or null when the box should not be drawn. Every state has a word of its own,
	 * so a number is only ever a time and "?" only ever means the count is not known (review,
	 * 2026-10-04). Package-private for tests.
	 */
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
			case STALLED:
			case INVENTORY_FILLS_FIRST:
			case WRECK_SINKS_FIRST:
				// The last two are when you stop, not when the hold fills; the colour and tooltip say so.
				return Durations.tiny(v.countdownMillis);
			case WAITING_FOR_WRECK:
				// Only at a spot; on the way there the box has nothing to say (owner, 2026-10-04).
				return v.atSpot ? "Wait" : null;
			case PLAYER_HOOK_IDLE:
				return "Hook";
			case NOBODY_SALVAGING:
				return "Idle";
			case INVENTORY_FULL:
				return "Inv";
			case LEVEL_TOO_LOW:
				return "Lvl";
			case HAZARDOUS:
				return "Stop";
			case NOT_SAILING:
			case NO_HOOK:
				return null;
			case HOLD_UNKNOWN:
			case HOLD_DRIFTED:
			default:
				return "?";
		}
	}

	static String tooltip(AfkSession.View v, CargoHoldMonitor.Level level)
	{
		String idle = v.idleWarning ? "; idle logout in " + Durations.countdown(v.idleLogoutMillis) : "";
		if (level == CargoHoldMonitor.Level.FULL || v.estimate.getState() == AfkEstimate.State.HOLD_FULL)
		{
			return "AFK Shipwrecks: the cargo hold is full" + idle;
		}
		String approx = v.estimate.isApproximate() ? "~" : "";
		switch (v.estimate.getState())
		{
			case HOLD_FULL_UNCONFIRMED:
				return "AFK Shipwrecks: hold full by the tally; open it to check" + idle;
			case COUNTING_DOWN:
				return "AFK Shipwrecks: hold full in " + approx + Durations.coarse(v.countdownMillis) + idle;
			case STALLED:
				return "AFK Shipwrecks: no salvage is arriving; the last estimate was hold full in "
					+ approx + Durations.coarse(v.countdownMillis) + idle;
			case INVENTORY_FILLS_FIRST:
				return "AFK Shipwrecks: your inventory is full in " + Durations.countdown(v.countdownMillis)
					+ "; deposit to keep the hold filling" + idle;
			case WRECK_SINKS_FIRST:
				return "AFK Shipwrecks: the wreck sinks in at most " + Durations.countdown(v.countdownMillis)
					+ ", and you stop then; nobody else is on a hook" + idle;
			case WAITING_FOR_WRECK:
				return "AFK Shipwrecks: waiting for a wreck" + idle;
			case PLAYER_HOOK_IDLE:
				return "AFK Shipwrecks: your hook is idle; click it to salvage" + idle;
			case NOBODY_SALVAGING:
				return "AFK Shipwrecks: no one is on a hook" + idle;
			case INVENTORY_FULL:
				return "AFK Shipwrecks: your inventory is full; deposit to carry on" + idle;
			case LEVEL_TOO_LOW:
				return "AFK Shipwrecks: your Sailing level is too low for this wreck"
					+ (v.levelNeeded > 0 ? " (needs " + v.levelNeeded + ")" : "") + idle;
			case HAZARDOUS:
				return "AFK Shipwrecks: not safe to salvage here" + idle;
			case HOLD_DRIFTED:
				return "AFK Shipwrecks: the tally has drifted; open the hold to resync" + idle;
			case HOLD_UNKNOWN:
				return "AFK Shipwrecks: open the cargo hold once to start the timer" + idle;
			default:
				return "AFK Shipwrecks" + idle;
		}
	}
}
