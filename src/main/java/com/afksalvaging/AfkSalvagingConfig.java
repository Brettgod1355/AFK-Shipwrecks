/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

import java.awt.Color;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Notification;
import net.runelite.client.config.Range;
import net.runelite.client.config.Units;

@ConfigGroup(AfkSalvagingConfig.GROUP)
public interface AfkSalvagingConfig extends Config
{
	/** Kept from the plugin's Cargo Hold Alert days so settings and remembered counts carry over. */
	String GROUP = "cargofull";

	@ConfigSection(
		name = "Timer",
		description = "The countdown to a full cargo hold",
		position = 0
	)
	String TIMER = "timer";

	@ConfigSection(
		name = "Reminders",
		description = "Nudges when a hook is standing empty or the crew have stopped",
		position = 1
	)
	String REMINDERS = "reminders";

	@ConfigSection(
		name = "Cargo full",
		description = "The alert when the hold is completely full",
		position = 2
	)
	String CARGO_FULL = "cargoFull";

	@ConfigSection(
		name = "Early warning",
		description = "A heads-up before the hold is completely full",
		position = 3
	)
	String EARLY_WARNING = "earlyWarning";

	@ConfigSection(
		name = "Banner",
		description = "The banner drawn over the game while an alert is active",
		position = 4
	)
	String BANNER = "banner";

	@ConfigSection(
		name = "Overlay",
		description = "The lines shown while you are on your boat",
		position = 5
	)
	String OVERLAY = "overlay";

	// ---- Timer ----

	@ConfigItem(
		keyName = "showTimer",
		name = "Show timer",
		description = "Show how long until the cargo hold is full at the current salvaging rate.",
		section = TIMER,
		position = 0
	)
	default boolean showTimer()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showClockTime",
		name = "Show clock time",
		description = "Also show the time of day the hold is expected to be full.",
		section = TIMER,
		position = 1
	)
	default boolean showClockTime()
	{
		return true;
	}

	@ConfigItem(
		keyName = "countHookedSalvage",
		name = "Count salvage you hooked",
		description = "Treat salvage you hooked yourself and have not yet deposited as bound for the hold. "
			+ "Salvage you withdrew to sort is never counted.",
		section = TIMER,
		position = 2
	)
	default boolean countHookedSalvage()
	{
		return true;
	}

	@ConfigItem(
		keyName = "salvagingWorlds",
		name = "Salvaging worlds",
		description = "World numbers to treat as salvaging worlds, where wrecks come back quickly because every site "
			+ "is being worked. Worlds the world list labels as salvaging are always included.",
		section = TIMER,
		position = 3
	)
	default String salvagingWorlds()
	{
		return SalvagingWorlds.DEFAULT_WORLDS;
	}

	@ConfigItem(
		keyName = "worldTip",
		name = "Salvaging world tip",
		description = "When the crew have been waiting a while for a wreck on an ordinary world, mention that "
			+ "salvaging worlds keep wrecks coming.",
		section = TIMER,
		position = 4
	)
	default boolean worldTip()
	{
		return true;
	}

	// ---- Reminders ----

	@ConfigItem(
		keyName = "hookEmptyNotification",
		name = "Hook empty, crewmate free",
		description = "Notify when a salvaging hook is standing empty and a crewmate aboard could be working it, "
			+ "for example after you step off your hook to sort.",
		section = REMINDERS,
		position = 0
	)
	default Notification hookEmptyNotification()
	{
		return Notification.ON;
	}

	@ConfigItem(
		keyName = "hookIdleNotification",
		name = "Your hook is idle",
		description = "Notify when a wreck is up, a hook is empty and only you can take it, for example after the "
			+ "wreck you were salvaging sank. Unlike your crew, you do not start again by yourself.",
		section = REMINDERS,
		position = 1
	)
	default Notification hookIdleNotification()
	{
		return Notification.ON;
	}

	@ConfigItem(
		keyName = "boostDroppedNotification",
		name = "Crew stopped: level too low",
		description = "Notify when the crew stop because your Sailing level, boosted or not, has dropped below "
			+ "what the wreck in reach needs.",
		section = REMINDERS,
		position = 2
	)
	default Notification boostDroppedNotification()
	{
		return Notification.ON;
	}

	@ConfigItem(
		keyName = "reminderGraceSeconds",
		name = "Grace period",
		description = "How long a hook may stand empty before the first reminder. Using the cargo hold buys a "
			+ "little extra; settling in to sort shortens it.",
		section = REMINDERS,
		position = 3
	)
	@Range(min = 3, max = 60)
	@Units(Units.SECONDS)
	default int reminderGraceSeconds()
	{
		return 15;
	}

	@ConfigItem(
		keyName = "reminderRepeatSeconds",
		name = "Repeat every",
		description = "Remind again this often while the hook stays empty. 0 reminds once.",
		section = REMINDERS,
		position = 4
	)
	@Range(min = 0, max = 600)
	@Units(Units.SECONDS)
	default int reminderRepeatSeconds()
	{
		return 60;
	}

	// ---- Cargo full ----

	@ConfigItem(
		keyName = "notification",
		name = "Notification",
		description = "The RuneLite notification sent when the hold is completely full. "
			+ "Use the gear to choose sound, tray popup, screen flash and focus behaviour.",
		section = CARGO_FULL,
		position = 0
	)
	default Notification notification()
	{
		return Notification.ON;
	}

	@ConfigItem(
		keyName = "repeatSeconds",
		name = "Repeat every",
		description = "Send the alert again this often while the hold stays full. 0 alerts once each time it fills.",
		section = CARGO_FULL,
		position = 1
	)
	@Range(min = 0, max = 600)
	@Units(Units.SECONDS)
	default int repeatSeconds()
	{
		return 0;
	}

	// ---- Early warning ----

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
			+ "tray popup, screen flash and banner still show; the full alert always uses the Cargo full settings.",
		section = EARLY_WARNING,
		position = 1
	)
	default boolean earlyWarningSound()
	{
		return true;
	}

	// ---- Banner ----

	@ConfigItem(
		keyName = "showBanner",
		name = "Show banner",
		description = "Draw a large banner at the top of the game while the hold is full or a hook needs you.",
		section = BANNER,
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
		section = BANNER,
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
		section = BANNER,
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
		section = BANNER,
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
		name = "Full colour",
		description = "Background colour of the banner when the hold is full.",
		section = BANNER,
		position = 4
	)
	default Color bannerColor()
	{
		return new Color(190, 30, 30, 210);
	}

	@Alpha
	@ConfigItem(
		keyName = "reminderBannerColor",
		name = "Reminder colour",
		description = "Background colour of the banner when a hook needs attention.",
		section = BANNER,
		position = 5
	)
	default Color reminderBannerColor()
	{
		return new Color(200, 130, 20, 210);
	}

	// ---- Overlay ----

	@ConfigItem(
		keyName = "showCounter",
		name = "Show cargo counter",
		description = "Show used and total cargo slots while you are on your boat.",
		section = OVERLAY,
		position = 0
	)
	default boolean showCounter()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showHooks",
		name = "Show hooks line",
		description = "Show who is on each salvaging hook.",
		section = OVERLAY,
		position = 1
	)
	default boolean showHooks()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showWrecks",
		name = "Show wrecks line",
		description = "Show how many wrecks are in reach and how long the current one has left at most.",
		section = OVERLAY,
		position = 2
	)
	default boolean showWrecks()
	{
		return true;
	}

	@ConfigItem(
		keyName = "overlayScale",
		name = "Text size",
		description = "Size of the overlay lines. 100% is the normal RuneLite overlay size.",
		section = OVERLAY,
		position = 3
	)
	@Range(min = 50, max = 300)
	@Units(Units.PERCENT)
	default int overlayScale()
	{
		return 100;
	}
}
