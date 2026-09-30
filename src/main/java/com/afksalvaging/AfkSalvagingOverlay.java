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
	private static final Color COUNTER_EMPTY = new Color(70, 200, 70);
	private static final Color COUNTER_FULL = new Color(230, 60, 60);
	private static final Color AMBER = new Color(255, 190, 70);
	private static final Color DIM = new Color(190, 190, 190);
	private static final Color GOOD = new Color(120, 220, 120);
	private static final Color BAD = new Color(240, 90, 90);

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
		AfkSession session = plugin.getSession();
		AfkSession.View view = session.view();
		CargoHoldMonitor monitor = session.monitor();

		CargoHoldMonitor.Level holdBanner = plugin.bannerLevel(now);
		HookWatch.Reason reminder = holdBanner == CargoHoldMonitor.Level.OK ? plugin.reminderBanner(now) : null;
		boolean aboardOwnBoat = view.estimate.getState() != AfkEstimate.State.NOT_SAILING;
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
			lines.add(new Line("Cargo hold", used + "/" + capacity,
				ColorUtil.colorLerp(COUNTER_EMPTY, COUNTER_FULL, fill)));
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
			background = holdBanner != CargoHoldMonitor.Level.OK ? config.bannerColor() : config.reminderBannerColor();
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
				.color(Color.WHITE)
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
				.leftColor(banner ? Color.WHITE : DIM)
				.rightColor(banner ? Color.WHITE : line.rightColor)
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
				lines.add(new Line("Timer", "no salvaging hook on this boat", DIM));
				break;
			case HOLD_UNKNOWN:
				lines.add(new Line("Timer", "open the cargo hold once to start", AMBER));
				break;
			case HOLD_FULL:
				if (holdBanner == CargoHoldMonitor.Level.OK)
				{
					lines.add(new Line("Hold", "full", COUNTER_FULL));
				}
				break;
			case HOLD_FULL_UNCONFIRMED:
				lines.add(new Line("Hold", "full by the tally; open it to check", AMBER));
				break;
			case HOLD_DRIFTED:
				lines.add(new Line("Hold", "tally has drifted; open it to resync", AMBER));
				break;
			case NOBODY_SALVAGING:
				lines.add(new Line("Timer", "no one is on a hook", DIM));
				break;
			case HAZARDOUS:
				lines.add(new Line("Timer", "not safe to salvage here", BAD));
				break;
			case LEVEL_TOO_LOW:
				lines.add(new Line(crew ? "Crew stopped" : "Timer",
					"Sailing level too low" + (view.levelNeeded > 0 ? " (needs " + view.levelNeeded + ")" : ""), BAD));
				break;
			case WAITING_FOR_WRECK:
				lines.add(new Line("Waiting for a wreck", view.waitingMillis >= 0 ? Durations.countdown(view.waitingMillis) + " so far" : "", AMBER));
				if (timer && estimate.hasWork())
				{
					lines.add(new Line("Left to salvage", approx + Durations.coarse(estimate.getWorkMillis()), DIM));
				}
				if (view.showWorldTip && config.worldTip())
				{
					lines.add(new Line("Tip", "wrecks refill faster on worlds " + plugin.salvagingWorldsText(), DIM));
				}
				break;
			case STALLED:
				lines.add(new Line("Timer", crew ? "crew do not seem to be salvaging" : "no salvage is arriving; is the hook in reach?", BAD));
				break;
			case PLAYER_HOOK_IDLE:
				lines.add(new Line("Your hook", "idle; click it to salvage", AMBER));
				break;
			case INVENTORY_FULL:
				lines.add(new Line("Inventory", "full; deposit to carry on", AMBER));
				break;
			case INVENTORY_FILLS_FIRST:
				if (timer)
				{
					lines.add(new Line("Inventory full in", Durations.countdown(view.countdownMillis), AMBER));
					lines.add(new Line("", "deposit to keep the hold filling", DIM));
				}
				break;
			case WRECK_SINKS_FIRST:
				if (timer)
				{
					lines.add(new Line("Wreck sinks in", "≤ " + Durations.countdown(view.countdownMillis), AMBER));
					lines.add(new Line("", "you stop then; nobody else is on a hook", DIM));
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
					lines.add(new Line("Hold", "full once you deposit your salvage", AMBER));
					break;
				}
				lines.add(new Line("Hold full in", approx + Durations.coarse(remaining), Color.WHITE));
				if (config.showClockTime() && remaining >= 0)
				{
					lines.add(new Line("", "at " + Durations.clockAfter(System.currentTimeMillis(), remaining, ZoneId.systemDefault()), DIM));
				}
				if (view.idleLogoutMillis >= 0 && remaining > view.idleLogoutMillis)
				{
					lines.add(new Line("Idle logout in", Durations.countdown(view.idleLogoutMillis), AMBER));
				}
				break;
			}
			default:
				break;
		}
		if (view.sortingLeftMillis >= 0)
		{
			lines.add(new Line("Sorting", Durations.countdown(view.sortingLeftMillis) + " left", DIM));
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
		Color color = GOOD;
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
			color = AMBER;
		}
		else if (text.length() == 0)
		{
			text.append("none");
			color = DIM;
		}
		return new Line("Hooks", text.toString(), color);
	}

	private Line wrecksLine(AfkSession.View view)
	{
		if (view.wrecksUp <= 0)
		{
			if (view.higherWrecksUp > 0)
			{
				return new Line("Wrecks", view.higherWrecksUp + " up, need level " + view.levelNeeded, BAD);
			}
			return new Line("Wrecks", "none in reach", DIM);
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
		return new Line("Wrecks", text.toString(), GOOD);
	}

}
