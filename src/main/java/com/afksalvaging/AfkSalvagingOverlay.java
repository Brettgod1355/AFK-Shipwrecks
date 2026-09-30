/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.ComponentConstants;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;
import net.runelite.client.util.ColorUtil;

/**
 * Draws the banner, the countdown and the status lines at the top of the game.
 */
public class AfkSalvagingOverlay extends OverlayPanel
{
	/** Panel width at 100% banner size. */
	private static final int BASE_WIDTH = 200;
	/** Breathing room either side of text that is wider than the panel. */
	private static final int TEXT_PADDING = 8;
	/** Minimum space between a line's label and its value. */
	private static final int LINE_GAP = 12;
	private static final long FLASH_PERIOD_MS = 500;
	// The colours, read from the settings at the start of every frame.
	private Color counterEmpty;
	private Color counterFull;
	private Color amber;
	private Color dim;
	private Color good;
	private Color bad;
	private Color textColour;

	private final AfkSalvagingPlugin plugin;
	private final AfkSalvagingConfig config;

	/** One row of the overlay. */
	private static final class Line
	{
		final String left;
		final String right;
		final Color rightColor;

		Line(String left, String right, Color rightColor)
		{
			this.left = left;
			this.right = right;
			this.rightColor = rightColor;
		}
	}

	@Inject
	AfkSalvagingOverlay(AfkSalvagingPlugin plugin, AfkSalvagingConfig config)
	{
		super(plugin);
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.TOP_CENTER);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
		setPriority(PRIORITY_HIGH);
		panelComponent.setPreferredSize(new Dimension(BASE_WIDTH, 0));
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		long now = AfkSalvagingPlugin.clock();
		counterEmpty = config.counterEmptyColor();
		counterFull = config.counterFullColor();
		amber = config.attentionColor();
		dim = config.overlayDimColor();
		good = config.goodColor();
		bad = config.badColor();
		textColour = config.overlayTextColor();
		AfkSession session = plugin.getSession();
		AfkSession.View view = session.view();
		CargoHoldMonitor monitor = session.monitor();

		CargoHoldMonitor.Level holdBanner = plugin.bannerLevel(now);
		HookWatch.Reason reminder = holdBanner == CargoHoldMonitor.Level.OK ? plugin.reminderBanner(now) : null;
		boolean aboardOwnBoat = view.estimate.getState() != AfkEstimate.State.NOT_SAILING;
		if (aboardOwnBoat && config.overlayWhen() == OverlayWhen.NEAR_WRECKS)
		{
			// A salvaging spot, as far as the client can tell, is anywhere a wreck site is in view.
			aboardOwnBoat = view.wrecksUp > 0 || view.higherWrecksUp > 0 || !session.wrecks().presentSites().isEmpty();
		}
		List<Line> lines = new ArrayList<>();
		if (aboardOwnBoat)
		{
			addTimerLines(lines, view, holdBanner);
		}
		if (config.showCounter() && aboardOwnBoat && monitor.hasCount())
		{
			int used = monitor.getUsed();
			int capacity = monitor.getCapacity();
			double fill = capacity > 0 ? Math.min(1.0, (double) used / capacity) : 0;
			lines.add(new Line("Cargo hold", config.counterStyle().format(used, capacity),
				ColorUtil.colorLerp(counterEmpty, counterFull, fill)));
		}
		if (aboardOwnBoat && config.showHooks() && view.hookCount > 0)
		{
			lines.add(hooksLine(view));
		}
		if (aboardOwnBoat && config.showWrecks() && view.hookCount > 0)
		{
			lines.add(wrecksLine(view));
		}

		boolean banner = holdBanner != CargoHoldMonitor.Level.OK || reminder != null;
		if (!banner && lines.isEmpty())
		{
			return null;
		}

		Color background = ComponentConstants.STANDARD_BACKGROUND_COLOR;
		int width = 0;
		if (banner)
		{
			if (holdBanner == CargoHoldMonitor.Level.FULL)
			{
				background = config.bannerColor();
			}
			else if (holdBanner == CargoHoldMonitor.Level.NEARLY_FULL)
			{
				background = config.warningBannerColor();
			}
			else
			{
				background = config.reminderBannerColor();
			}
			if (config.flashBanner() && (now / FLASH_PERIOD_MS) % 2 == 1)
			{
				background = new Color(background.getRed(), background.getGreen(), background.getBlue(),
					background.getAlpha() / 3);
			}
			String text;
			if (holdBanner == CargoHoldMonitor.Level.FULL)
			{
				text = "CARGO HOLD FULL";
			}
			else if (holdBanner == CargoHoldMonitor.Level.NEARLY_FULL)
			{
				text = "CARGO HOLD NEARLY FULL";
			}
			else
			{
				text = reminder == HookWatch.Reason.PLAYER_NEEDED ? "YOUR HOOK IS IDLE" : "HOOK EMPTY";
			}
			// The title takes its font from the graphics, so scaling the font scales the banner.
			float scale = config.bannerScale() / 100f;
			Font bold = FontManager.getRunescapeBoldFont();
			Font bannerFont = bold.deriveFont(bold.getSize2D() * scale);
			graphics.setFont(bannerFont);
			int textWidth = graphics.getFontMetrics(bannerFont).stringWidth(text);
			width = Math.max(Math.round(BASE_WIDTH * scale), textWidth + 2 * ComponentConstants.STANDARD_BORDER + TEXT_PADDING);
			panelComponent.getChildren().add(TitleComponent.builder()
				.text(text)
				.color(textColour)
				.build());
		}
		panelComponent.setBackgroundColor(background);

		float scale = config.overlayScale() / 100f;
		Font normal = FontManager.getRunescapeFont();
		Font lineFont = normal.deriveFont(normal.getSize2D() * scale);
		FontMetrics metrics = graphics.getFontMetrics(lineFont);
		for (Line line : lines)
		{
			int lineWidth = metrics.stringWidth(line.left) + metrics.stringWidth(line.right) + LINE_GAP;
			width = Math.max(width, lineWidth + 2 * ComponentConstants.STANDARD_BORDER + TEXT_PADDING);
			panelComponent.getChildren().add(LineComponent.builder()
				.left(line.left)
				.right(line.right)
				.leftColor(banner ? textColour : dim)
				.rightColor(banner ? textColour : line.rightColor)
				.leftFont(lineFont)
				.rightFont(lineFont)
				.build());
		}

		if (getPreferredSize() == null)
		{
			// Follow the chosen sizes unless the player has resized the overlay themselves.
			panelComponent.setPreferredSize(new Dimension(width, 0));
		}
		return super.render(graphics);
	}

	private void addTimerLines(List<Line> lines, AfkSession.View view, CargoHoldMonitor.Level holdBanner)
	{
		AfkEstimate estimate = view.estimate;
		String approx = estimate.isApproximate() ? "~" : "";
		boolean timer = config.showTimer();
		boolean crew = !view.crewOnHooks.isEmpty();
		switch (estimate.getState())
		{
			case NO_HOOK:
				lines.add(new Line("Timer", "no salvaging hook on this boat", dim));
				break;
			case HOLD_UNKNOWN:
				lines.add(new Line("Timer", "open the cargo hold once to start", amber));
				break;
			case HOLD_FULL:
				if (holdBanner == CargoHoldMonitor.Level.OK)
				{
					lines.add(new Line("Hold", "full", counterFull));
				}
				break;
			case HOLD_FULL_UNCONFIRMED:
				lines.add(new Line("Hold", "full by the tally; open it to check", amber));
				break;
			case HOLD_DRIFTED:
				lines.add(new Line("Hold", "tally has drifted; open it to resync", amber));
				break;
			case NOBODY_SALVAGING:
				lines.add(new Line("Timer", "no one is on a hook", dim));
				break;
			case HAZARDOUS:
				lines.add(new Line("Timer", "not safe to salvage here", bad));
				break;
			case LEVEL_TOO_LOW:
				lines.add(new Line(crew ? "Crew stopped" : "Timer",
					"Sailing level too low" + (view.levelNeeded > 0 ? " (needs " + view.levelNeeded + ")" : ""), bad));
				break;
			case WAITING_FOR_WRECK:
				lines.add(new Line("Waiting for a wreck", view.waitingMillis >= 0 ? Durations.countdown(view.waitingMillis) + " so far" : "", amber));
				if (timer && estimate.hasWork())
				{
					lines.add(new Line("Left to salvage", approx + Durations.coarse(estimate.getWorkMillis()), dim));
				}
				if (view.showWorldTip && config.worldTip())
				{
					lines.add(new Line("Tip", "wrecks refill faster on worlds " + plugin.salvagingWorldsText(), dim));
				}
				break;
			case STALLED:
				lines.add(new Line("Timer", crew ? "crew do not seem to be salvaging" : "no salvage is arriving; is the hook in reach?", bad));
				break;
			case PLAYER_HOOK_IDLE:
				lines.add(new Line("Your hook", "idle; click it to salvage", amber));
				break;
			case INVENTORY_FULL:
				lines.add(new Line("Inventory", "full; deposit to carry on", amber));
				break;
			case INVENTORY_FILLS_FIRST:
				if (timer)
				{
					lines.add(new Line("Inventory full in", Durations.countdown(view.countdownMillis), amber));
					lines.add(new Line("", "deposit to keep the hold filling", dim));
				}
				break;
			case WRECK_SINKS_FIRST:
				if (timer)
				{
					lines.add(new Line("Wreck sinks in", "≤ " + Durations.countdown(view.countdownMillis), amber));
					lines.add(new Line("", "you stop then; nobody else is on a hook", dim));
				}
				break;
			case COUNTING_DOWN:
			{
				if (!timer)
				{
					break;
				}
				long remaining = view.countdownMillis;
				if (remaining == 0 && estimate.getWorkMillis() == 0)
				{
					lines.add(new Line("Hold", "full once you deposit your salvage", amber));
					break;
				}
				lines.add(new Line("Hold full in", approx + Durations.coarse(remaining), textColour));
				if (config.showClockTime() && remaining >= 0)
				{
					boolean twelveHour = config.clockFormat() == AfkSalvagingConfig.ClockFormat.TWELVE_HOUR;
					lines.add(new Line("", "at " + Durations.clockAfter(System.currentTimeMillis(), remaining,
						ZoneId.systemDefault(), twelveHour), dim));
				}
				if (view.idleLogoutMillis >= 0 && remaining > view.idleLogoutMillis && !view.idleWarning)
				{
					lines.add(new Line("Idle logout in", Durations.countdown(view.idleLogoutMillis), amber));
				}
				break;
			}
			default:
				break;
		}
		if (view.idleWarning)
		{
			lines.add(new Line("Idle logout in", Durations.countdown(view.idleLogoutMillis) + "; move the mouse", bad));
		}

	}

	private Line hooksLine(AfkSession.View view)
	{
		StringBuilder text = new StringBuilder();
		for (Crewmate crewmate : view.crewOnHooks)
		{
			if (text.length() > 0)
			{
				text.append(", ");
			}
			text.append(crewmate.shortName());
		}
		if (view.playerAtHook)
		{
			text.append(text.length() > 0 ? " + you" : "you");
		}
		Color color = good;
		if (view.emptyHooks > 0)
		{
			if (text.length() > 0)
			{
				text.append(" · ");
			}
			text.append(view.emptyHooks).append(view.emptyHooks == 1 ? " empty" : " empty");
			if (view.spareCrewCannotUseHook)
			{
				text.append(" (no crewmate can use it)");
			}
			color = amber;
		}
		else if (text.length() == 0)
		{
			text.append("none");
			color = dim;
		}
		return new Line("Hooks", text.toString(), color);
	}

	private Line wrecksLine(AfkSession.View view)
	{
		if (view.wrecksUp <= 0)
		{
			if (view.higherWrecksUp > 0)
			{
				return new Line("Wrecks", view.higherWrecksUp + " up, need level " + view.levelNeeded, bad);
			}
			return new Line("Wrecks", "none in reach", dim);
		}
		StringBuilder text = new StringBuilder();
		text.append(view.wrecksUp).append(" up");
		if (view.wreckType != null)
		{
			text.append(" (").append(view.wreckType.getDisplayName()).append(")");
		}
		if (view.wreckWindowAnchored && view.wreckWindowMillis >= 0)
		{
			text.append(" · ").append(view.wrecksUp == 1 ? "sinks in ≤ " : "last sinks in ≤ ")
				.append(Durations.countdown(view.wreckWindowMillis));
		}
		return new Line("Wrecks", text.toString(), good);
	}

}
