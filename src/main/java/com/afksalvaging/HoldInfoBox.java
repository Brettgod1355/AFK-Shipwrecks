/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.function.Supplier;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.ui.overlay.infobox.InfoBox;

/**
 * The time to a full hold as one of RuneLite's infoboxes, so it stays in view with the overlay
 * hidden. Shown only aboard the player's own boat with something to say.
 */
public class HoldInfoBox extends InfoBox
{
	private final Supplier<AfkSession.View> view;
	private final Supplier<CargoHoldMonitor.Level> holdLevel;

	public HoldInfoBox(BufferedImage image, Plugin plugin, Supplier<AfkSession.View> view,
		Supplier<CargoHoldMonitor.Level> holdLevel)
	{
		super(image, plugin);
		this.view = view;
		this.holdLevel = holdLevel;
		setTooltip("AFK Salvaging");
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
				return "Wait";
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
			return "AFK Salvaging: the cargo hold is full";
		}
		switch (v.estimate.getState())
		{
			case HOLD_FULL_UNCONFIRMED:
				return "AFK Salvaging: hold full by the tally; open it to check";
			case COUNTING_DOWN:
			case INVENTORY_FILLS_FIRST:
			case WRECK_SINKS_FIRST:
			case STALLED:
				return "AFK Salvaging: hold full in " + (v.estimate.isApproximate() ? "~" : "")
					+ Durations.coarse(v.countdownMillis)
					+ (v.idleWarning ? "; idle logout in " + Durations.countdown(v.idleLogoutMillis) : "");
			case WAITING_FOR_WRECK:
				return "AFK Salvaging: waiting for a wreck";
			case HOLD_UNKNOWN:
				return "AFK Salvaging: open the cargo hold once to start the timer";
			default:
				return "AFK Salvaging: " + v.estimate.getState().name().toLowerCase().replace('_', ' ');
		}
	}
}
