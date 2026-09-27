/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.cargofull;

import java.awt.Color;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Notification;
import net.runelite.client.config.Range;
import net.runelite.client.config.Units;

@ConfigGroup(CargoFullConfig.GROUP)
public interface CargoFullConfig extends Config
{
	String GROUP = "cargofull";

	@ConfigSection(
		name = "Alerts",
		description = "When and how you are alerted",
		position = 0
	)
	String ALERTS = "alerts";

	@ConfigSection(
		name = "Early warning",
		description = "A heads-up before the hold is completely full",
		position = 1
	)
	String EARLY_WARNING = "earlyWarning";

	@ConfigSection(
		name = "On screen",
		description = "What is drawn over the game",
		position = 2
	)
	String ON_SCREEN = "onScreen";

	@ConfigItem(
		keyName = "notification",
		name = "Cargo alert",
		description = "The RuneLite notification sent when the hold fills up. "
			+ "Use the gear to choose sound, tray popup, screen flash and focus behaviour.",
		section = ALERTS,
		position = 0
	)
	default Notification notification()
	{
		return Notification.ON;
	}

	@ConfigItem(
		keyName = "repeatSeconds",
		name = "Repeat alert every",
		description = "Send the alert again this often while the hold stays full. 0 alerts once each time it fills.",
		section = ALERTS,
		position = 1
	)
	@Range(min = 0, max = 600)
	@Units(Units.SECONDS)
	default int repeatSeconds()
	{
		return 0;
	}

	@ConfigItem(
		keyName = "alertOnGameMessage",
		name = "Alert on crew message",
		description = "Also alert when a crewmate or the game says the cargo hold is full, "
			+ "even if the plugin could not count the hold itself.",
		section = ALERTS,
		position = 2
	)
	default boolean alertOnGameMessage()
	{
		return true;
	}

	@ConfigItem(
		keyName = "liveEstimate",
		name = "Track between hold openings",
		description = "The game only sends the hold's contents while it is open. Keep counting in between from "
			+ "crewmate salvage messages and your own deposits and withdrawals. The real contents take over "
			+ "whenever you open the hold.",
		section = ALERTS,
		position = 3
	)
	default boolean liveEstimate()
	{
		return true;
	}

	@ConfigItem(
		keyName = "warnSlotsRemaining",
		name = "Warn with slots left",
		description = "Alert once this many free slots remain, before the hold is completely full. 0 turns the early warning off.",
		section = EARLY_WARNING,
		position = 0
	)
	@Range(min = 0, max = 239)
	default int warnSlotsRemaining()
	{
		return 0;
	}

	@ConfigItem(
		keyName = "earlyWarningSound",
		name = "Play sound",
		description = "Play the alert sound for the early warning. Off keeps the early warning silent while the "
			+ "tray popup, screen flash and banner still show; the full alert always uses the Cargo alert settings.",
		section = EARLY_WARNING,
		position = 1
	)
	default boolean earlyWarningSound()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showBanner",
		name = "Show banner",
		description = "Draw a large CARGO HOLD FULL banner at the top of the game while the hold is full.",
		section = ON_SCREEN,
		position = 0
	)
	default boolean showBanner()
	{
		return true;
	}

	@ConfigItem(
		keyName = "flashBanner",
		name = "Flash banner",
		description = "Pulse the banner so it catches your eye.",
		section = ON_SCREEN,
		position = 1
	)
	default boolean flashBanner()
	{
		return true;
	}

	@ConfigItem(
		keyName = "bannerScale",
		name = "Banner size",
		description = "Size of the banner text and box. 100% is the normal RuneLite overlay size.",
		section = ON_SCREEN,
		position = 2
	)
	@Range(min = 50, max = 300)
	@Units(Units.PERCENT)
	default int bannerScale()
	{
		return 100;
	}

	@ConfigItem(
		keyName = "bannerSeconds",
		name = "Hide banner after",
		description = "Hide the banner this long after the alert. 0 keeps it up until the hold has space again.",
		section = ON_SCREEN,
		position = 3
	)
	@Range(min = 0, max = 600)
	@Units(Units.SECONDS)
	default int bannerSeconds()
	{
		return 0;
	}

	@Alpha
	@ConfigItem(
		keyName = "bannerColor",
		name = "Banner colour",
		description = "Background colour of the banner.",
		section = ON_SCREEN,
		position = 4
	)
	default Color bannerColor()
	{
		return new Color(190, 30, 30, 210);
	}

	@ConfigItem(
		keyName = "showCounter",
		name = "Show cargo counter",
		description = "Show used and total cargo slots while you are on your boat.",
		section = ON_SCREEN,
		position = 5
	)
	default boolean showCounter()
	{
		return true;
	}

	@ConfigItem(
		keyName = "counterScale",
		name = "Counter size",
		description = "Size of the cargo counter shown while you sail. 100% is the normal RuneLite overlay size.",
		section = ON_SCREEN,
		position = 6
	)
	@Range(min = 50, max = 300)
	@Units(Units.PERCENT)
	default int counterScale()
	{
		return 100;
	}
}
