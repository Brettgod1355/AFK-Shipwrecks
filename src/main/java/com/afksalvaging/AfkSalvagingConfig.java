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

	@ConfigItem(
		keyName = "defaultNotification",
		name = "Default notification",
		description = "How every notification in this plugin is sent, unless its<br>"
			+ "own setting is switched off or set to custom with the cog.<br>"
			+ "Left plain, it is RuneLite's own notification settings; set<br>"
			+ "it to custom once and every category follows it. Switching<br>"
			+ "it off silences every category that is not custom.",
		position = 0
	)
	default Notification defaultNotification()
	{
		return Notification.ON;
	}

	// Every section starts closed so the panel opens as a short list of headings (owner, 2026-10-07:
	// "tidy up the options menu ... to de clutter"). Only the sections moved; every key is unchanged, so
	// saved settings carry over.

	@ConfigSection(
		name = "Overlay",
		description = "The countdown and the lines shown while you are on your boat",
		position = 0,
		closedByDefault = true
	)
	String OVERLAY = "overlay";

	@ConfigSection(
		name = "Full hold",
		description = "The alert when the hold is full, and the heads-up before it",
		position = 2,
		closedByDefault = true
	)
	String CARGO_FULL = "cargoFull";

	@ConfigSection(
		name = "Banner",
		description = "The banner drawn over the game while an alert is active",
		position = 3,
		closedByDefault = true
	)
	String BANNER = "banner";

	@ConfigSection(
		name = "Crew and hooks",
		description = "Nudges when a hook is standing empty, the crew have stopped<br>"
			+ "or a better crewmate is free",
		position = 4,
		closedByDefault = true
	)
	String CREW = "crew";

	@ConfigSection(
		name = "Reminders",
		description = "The idle logout, danger, and when a wreck rises or you are<br>"
			+ "done sorting",
		position = 5,
		closedByDefault = true
	)
	String REMINDERS = "reminders";

	@ConfigSection(
		name = "Wrecks",
		description = "How wrecks are marked, their timers, and the boxes on the<br>"
			+ "water that show where to park",
		position = 6,
		closedByDefault = true
	)
	String SPOTS = "spots";

	@ConfigSection(
		name = "Salvaging worlds",
		description = "The worlds where every wreck site is worked, and the tip and<br>"
			+ "alert off them",
		position = 7,
		closedByDefault = true
	)
	String WORLDS = "worlds";

	@ConfigSection(
		name = "Sidebar",
		description = "The sidebar of salvaging spots and docks",
		position = 8,
		closedByDefault = true
	)
	String SIDEBAR = "sidebar";

	// ---- Overlay: the timer (its own section until 2.1) ----

	@ConfigItem(
		keyName = "showTimer",
		name = "Show timer",
		description = "Show how long until the cargo hold is full at the current<br>"
			+ "salvaging rate. Status lines such as waiting for a wreck or<br>"
			+ "the crew having stopped are shown either way.",
		section = OVERLAY,
		position = 1
	)
	default boolean showTimer()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showClockTime",
		name = "Show clock time",
		description = "Also show the time of day the hold is expected to be full.",
		section = OVERLAY,
		position = 2
	)
	default boolean showClockTime()
	{
		// Off for new installs since 2.3 (owner, 2026-10-19); RuneLite saved the old default for everyone else.
		return false;
	}

	enum ClockFormat
	{
		TWELVE_HOUR("12-hour"),
		TWENTY_FOUR_HOUR("24-hour");

		private final String label;

		ClockFormat(String label)
		{
			this.label = label;
		}

		@Override
		public String toString()
		{
			return label;
		}
	}

	@ConfigItem(
		keyName = "clockFormat",
		name = "Clock format",
		description = "Show the time the hold will be full as 12-hour (6:25 PM) or<br>"
			+ "24-hour (18:25).",
		section = OVERLAY,
		position = 3
	)
	default ClockFormat clockFormat()
	{
		return ClockFormat.TWELVE_HOUR;
	}

	@ConfigItem(
		keyName = "salvagingWorlds",
		name = "Salvaging worlds",
		description = "World numbers to treat as salvaging worlds, where wrecks<br>"
			+ "come back quickly because every site is being worked. Worlds<br>"
			+ "the world list labels as salvaging are always included.",
		section = WORLDS,
		position = 0
	)
	default String salvagingWorlds()
	{
		return SalvagingWorlds.DEFAULT_WORLDS;
	}

	@ConfigItem(
		keyName = "worldTip",
		name = "Salvaging world tip",
		description = "When the crew have been waiting a while for a wreck on an<br>"
			+ "ordinary world, mention that salvaging worlds keep wrecks<br>"
			+ "coming.",
		section = WORLDS,
		position = 1
	)
	default boolean worldTip()
	{
		return true;
	}

	@ConfigItem(
		keyName = "worldAlert",
		name = "Salvaging world alert",
		description = "While you sit at a spot with no wreck up for a quarter of a<br>"
			+ "minute on a world that is not a salvaging world, flash a<br>"
			+ "notice in the middle of the screen with the worlds to hop<br>"
			+ "to. It goes when a wreck is up or you hop. The tip above<br>"
			+ "says it once in chat; this keeps saying it.",
		section = WORLDS,
		position = 2
	)
	default boolean worldAlert()
	{
		// Off for new installs since 2.3 (owner, 2026-10-19: a flashing notice is a lot for someone trying the plugin);
		// RuneLite saved the old default for everyone else.
		return false;
	}

	// ---- Crew and hooks, then Reminders (one "Reminders" section until 2.3) ----

	@ConfigItem(
		keyName = "hookEmptyNotification",
		name = "Hook empty, crewmate free",
		description = "Notify when a salvaging hook is standing empty and a<br>"
			+ "crewmate aboard could be working it, for example after you<br>"
			+ "step off your hook to sort. Only at a salvaging spot, with a<br>"
			+ "wreck site, up or sunk, within reach of your hooks.",
		section = CREW,
		position = 0
	)
	default Notification hookEmptyNotification()
	{
		return Notification.ON;
	}

	@ConfigItem(
		keyName = "hookIdleNotification",
		name = "Your hook is idle",
		description = "Notify when a wreck is up, a hook is empty and only you can<br>"
			+ "take it, for example after the wreck you were salvaging<br>"
			+ "sank. Unlike your crew, you do not start again by yourself.",
		section = CREW,
		position = 1
	)
	default Notification hookIdleNotification()
	{
		return Notification.ON;
	}

	@ConfigItem(
		keyName = "boostDroppedNotification",
		name = "Crew stopped: level too low",
		description = "Notify when the crew stop because your Sailing level,<br>"
			+ "boosted or not, has dropped below what the wreck in reach<br>"
			+ "needs.",
		section = CREW,
		position = 5
	)
	default Notification boostDroppedNotification()
	{
		return Notification.ON;
	}

	@ConfigItem(
		keyName = "betterCrewNotification",
		name = "Better crewmate free",
		description = "A crewmate with more deckhandiness is sitting idle while a<br>"
			+ "weaker one works a hook: say who to swap, once, after the<br>"
			+ "pair has stood for ten seconds. Only when every hook is<br>"
			+ "manned; an empty hook gets its own reminder first.",
		section = CREW,
		position = 7
	)
	default Notification betterCrewNotification()
	{
		return Notification.ON;
	}

	@ConfigItem(
		keyName = "swapNotice",
		name = "Swap on screen",
		description = "While a better crewmate is free, a SWAP CREW notice in the<br>"
			+ "middle of the screen, in the reminder banner color, until the<br>"
			+ "swap is made. On shows it steady, Flash makes it flash, Off<br>"
			+ "leaves only the notification and the overlay line.",
		section = CREW,
		position = 8
	)
	default SwapNotice swapNotice()
	{
		// Steady for new installs (owner, 2026-10-22); 2.3's "Flash the swap on screen" switch carries over once.
		return SwapNotice.ON;
	}

	@ConfigItem(
		keyName = "crewTextHiding",
		name = "Hide crew text",
		description = "Hide the text over crewmates' heads, on your boat and on<br>"
			+ "others' (\"Managed to hook some salvage! I'll put it in the<br>"
			+ "cargo hold.\", \"Wooo wooo.\"), players' orders to their crew<br>"
			+ "over their heads (\"Jenkins, take a rest!\"), and your own<br>"
			+ "orders in the chat box. At salvage spots only, everywhere<br>"
			+ "you sail, or off. The hold count still reads every line.",
		section = CREW,
		position = 9
	)
	default CrewTextHiding crewTextHiding()
	{
		return CrewTextHiding.SALVAGING_SPOTS;
	}

	@ConfigItem(
		keyName = "hookGuide",
		name = "Empty hook crew highlight",
		description = "While a hook is empty and a crewmate could take it, flash a<br>"
			+ "green box on the next click: the Sailing tab, the empty<br>"
			+ "hook's Assign button, then the free crewmate with the most<br>"
			+ "deckhandiness for that hook. It starts with the reminder, or<br>"
			+ "as soon as you start sorting after leaving the hook.",
		section = NEXT_STEP,
		position = 2
	)
	default boolean hookGuide()
	{
		return true;
	}

	@ConfigItem(
		keyName = "waitingNotification",
		name = "Waiting for a wreck",
		description = "Notify once every wreck in reach has been down for ten<br>"
			+ "seconds, with the latest the next can rise when that is<br>"
			+ "known. Off unless you want it.",
		section = REMINDERS,
		position = 4
	)
	default Notification waitingNotification()
	{
		return Notification.OFF;
	}

	@ConfigItem(
		keyName = "sortingDoneNotification",
		name = "Done sorting at the station",
		description = "Notify five seconds after the game says you have no more<br>"
			+ "salvage to sort, unless you have clicked anything or started<br>"
			+ "sorting again by then: it is for when you have wandered off.<br>"
			+ "Off unless you want it.",
		section = REMINDERS,
		position = 6
	)
	default Notification sortingDoneNotification()
	{
		return Notification.OFF;
	}

	@ConfigItem(
		keyName = "reminderGraceSeconds",
		name = "Grace period",
		description = "How long a hook may stand empty before the first reminder,<br>"
			+ "sorting or not. Using the cargo hold buys a little extra.",
		section = CREW,
		position = 2
	)
	@Range(min = 3, max = 60)
	@Units(Units.SECONDS)
	default int reminderGraceSeconds()
	{
		return 10;
	}

	@ConfigItem(
		keyName = "reminderRepeatSeconds",
		name = "Remind again every",
		description = "Remind again this often while the hook stays empty. 0<br>"
			+ "reminds once.",
		section = CREW,
		position = 3
	)
	@Range(min = 0, max = 600)
	@Units(Units.SECONDS)
	default int reminderRepeatSeconds()
	{
		return 60;
	}

	@ConfigItem(
		keyName = "idleLogoutNotification",
		name = "Idle logout coming",
		description = "Notify shortly before the game logs you out for idling while<br>"
			+ "you are on your own boat, so a full-hold alert does not<br>"
			+ "arrive after you are gone.",
		section = REMINDERS,
		position = 0
	)
	default Notification idleLogoutNotification()
	{
		return Notification.ON;
	}

	@ConfigItem(
		keyName = "idleLogoutWarnSeconds",
		name = "Warn before logout",
		description = "How long before the idle logout to warn. 0 never warns.",
		section = REMINDERS,
		position = 1
	)
	@Range(min = 0, max = 600)
	@Units(Units.SECONDS)
	default int idleLogoutWarnSeconds()
	{
		return 60;
	}

	@ConfigItem(
		keyName = "crewStalledNotification",
		name = "Crew stopped at a wreck",
		description = "Notify when the crew stop bringing salvage in although a<br>"
			+ "wreck they can work is in reach. The game shows nothing for<br>"
			+ "it; nudging the boat or assigning them again usually starts<br>"
			+ "them.",
		section = CREW,
		position = 4
	)
	default Notification crewStalledNotification()
	{
		return Notification.ON;
	}

	@ConfigItem(
		keyName = "wreckRoseNotification",
		name = "A wreck rose",
		description = "Notify when a wreck rises in reach after you have waited for<br>"
			+ "one. Your crew start on it by themselves; your own hook does<br>"
			+ "not.",
		section = REMINDERS,
		position = 5
	)
	default Notification wreckRoseNotification()
	{
		return Notification.OFF;
	}

	@ConfigItem(
		keyName = "boostWarnWhen",
		name = "Boost warning",
		description = "When to warn about a Sailing boost wearing off below the<br>"
			+ "level of the wreck in reach: just before it drops (the crew<br>"
			+ "still salvaging), after the crew have stopped, or both. Sent<br>"
			+ "the way \"Crew stopped: level too low\" is set.",
		section = CREW,
		position = 6
	)
	default BoostWarning boostWarnWhen()
	{
		return BoostWarning.BOTH;
	}

	@ConfigItem(
		keyName = "dangerNotification",
		name = "Danger",
		description = "While you are parked at a salvaging spot on your own boat:<br>"
			+ "notify, with a sound, when the boat takes damage, you are<br>"
			+ "hit, or you are poisoned. At most once every 30 seconds.",
		section = REMINDERS,
		position = 2
	)
	default Notification dangerNotification()
	{
		return Notification.ON;
	}

	@ConfigItem(
		keyName = "dangerAlert",
		name = "Flash danger on screen",
		description = "Also flash a red DANGER notice in the middle of the screen<br>"
			+ "while it lasts: ten seconds after the last hit, or until a<br>"
			+ "poison is cured.",
		section = REMINDERS,
		position = 3
	)
	default boolean dangerAlert()
	{
		return true;
	}

	// ---- Full hold ----

	@ConfigItem(
		keyName = "notification",
		name = "Full-hold notification",
		description = "The RuneLite notification sent when the hold is completely<br>"
			+ "full. Use the gear to choose sound, tray popup, screen flash<br>"
			+ "and focus behavior.",
		section = CARGO_FULL,
		position = 0
	)
	default Notification notification()
	{
		return Notification.ON;
	}

	@ConfigItem(
		keyName = "repeatSeconds",
		name = "Repeat alert every",
		description = "Send the alert again this often while the hold stays full. 0<br>"
			+ "alerts once each time it fills.",
		section = CARGO_FULL,
		position = 1
	)
	@Range(min = 0, max = 600)
	@Units(Units.SECONDS)
	default int repeatSeconds()
	{
		return 0;
	}

	// ---- Full hold: the early warning (its own section until 2.1) ----

	@ConfigItem(
		keyName = "warnSlotsRemaining",
		name = "Warn with slots left",
		description = "Alert once this many free slots remain, before the hold is<br>"
			+ "completely full: 5 by default. 0 turns the early warning<br>"
			+ "off.",
		section = CARGO_FULL,
		position = 2
	)
	@Range(min = 0, max = 239)
	default int warnSlotsRemaining()
	{
		// 5 slots by default (owner, 2026-10-04).
		return 5;
	}

	@ConfigItem(
		keyName = "earlyWarningSound",
		name = "Play sound",
		description = "Play the alert sound for the early warning. Off keeps the<br>"
			+ "early warning silent while the tray popup, screen flash and<br>"
			+ "banner still show; the full alert always uses the Cargo full<br>"
			+ "settings.",
		section = CARGO_FULL,
		position = 3
	)
	default boolean earlyWarningSound()
	{
		return true;
	}

	// ---- Banner ----

	@ConfigItem(
		keyName = "showBanner",
		name = "Show banner",
		description = "Draw a large banner at the top of the game while the hold is<br>"
			+ "full or a hook needs you.",
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
		description = "Size of the banner text and box. 100% is the normal RuneLite<br>"
			+ "overlay size; 150% by default.",
		section = BANNER,
		position = 2
	)
	@Range(min = 50, max = 300)
	@Units(Units.PERCENT)
	default int bannerScale()
	{
		// 150% by default (owner, 2026-10-04).
		return 150;
	}

	@ConfigItem(
		keyName = "bannerSeconds",
		name = "Hide banner after",
		description = "Hide the banner this long after the alert. 0 keeps it up<br>"
			+ "until the hold has space again.",
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
		name = "Full color",
		description = "Background color of the banner when the hold is full.",
		section = BANNER,
		position = 4
	)
	default Color bannerColor()
	{
		return new Color(190, 30, 30, 210);
	}

	@Alpha
	@ConfigItem(
		keyName = "warningBannerColor",
		name = "Nearly full color",
		description = "Background color of the banner for the early warning, so you<br>"
			+ "can tell it from the full one at a glance.",
		section = BANNER,
		position = 5
	)
	default Color warningBannerColor()
	{
		return new Color(200, 110, 20, 210);
	}

	@Alpha
	@ConfigItem(
		keyName = "reminderBannerColor",
		name = "Reminder color",
		description = "Background color of the banner when a hook needs attention.",
		section = BANNER,
		position = 6
	)
	default Color reminderBannerColor()
	{
		return new Color(200, 130, 20, 210);
	}

	// ---- Overlay ----

	@ConfigItem(
		keyName = "overlayWhen",
		name = "Show overlay",
		description = "Near wrecks: the timer, counter, hooks and wrecks lines show<br>"
			+ "only while a wreck site is in view, so sailing from port to<br>"
			+ "port stays clear. Always aboard: show them whenever you are<br>"
			+ "on your boat. Alerts and banners show either way.",
		section = OVERLAY,
		position = 0
	)
	default OverlayWhen overlayWhen()
	{
		return OverlayWhen.NEAR_WRECKS;
	}

	@ConfigItem(
		keyName = "showCounter",
		name = "Show cargo counter",
		description = "Show used and total cargo slots while you are on your boat.",
		section = OVERLAY,
		position = 5
	)
	default boolean showCounter()
	{
		return true;
	}

	@ConfigItem(
		keyName = "counterStyle",
		name = "Counter shows",
		description = "What the counter shows: used out of total (154/160), slots<br>"
			+ "left (6 left), or percent full (96%).",
		section = OVERLAY,
		position = 6
	)
	default CounterStyle counterStyle()
	{
		return CounterStyle.USED_OF_TOTAL;
	}

	@ConfigItem(
		keyName = "showHooks",
		name = "Show hooks line",
		description = "Show who is on each salvaging hook.",
		section = OVERLAY,
		position = 8
	)
	default boolean showHooks()
	{
		// Off for new installs since 2.3 (owner, 2026-10-19); RuneLite saved the old default for everyone else.
		return false;
	}

	@ConfigItem(
		keyName = "showWrecks",
		name = "Show wrecks line",
		description = "Show how many wrecks are in reach and how long the current<br>"
			+ "one has left at most.",
		section = OVERLAY,
		position = 9
	)
	default boolean showWrecks()
	{
		return true;
	}

	@ConfigItem(
		keyName = "counterScale",
		name = "Text size",
		description = "Size of the overlay lines. 100% is the normal RuneLite<br>"
			+ "overlay size.",
		section = OVERLAY,
		position = 10
	)
	@Range(min = 50, max = 300)
	@Units(Units.PERCENT)
	default int overlayScale()
	{
		// 100% for new installs since 2.3, was 150% (owner, 2026-10-04, then 2026-10-19); RuneLite saved the old default
		// for everyone else.
		return 100;
	}

	@ConfigItem(
		keyName = "countOnHold",
		name = "Count on the hold",
		description = "Write the cargo count on the cargo hold itself, in the middle<br>"
			+ "of the hold (for example 39/160), whenever you are on your<br>"
			+ "own boat.",
		section = CARGO_HOLD,
		position = 0
	)
	default boolean countOnHold()
	{
		return true;
	}

	@ConfigItem(
		keyName = "holdCountFormat",
		name = "Count shows",
		description = "What the count on the hold reads: the same as the overlay's<br>"
			+ "\"Counter shows\", or its own: 154/160, 6 left or 96%.",
		section = CARGO_HOLD,
		position = 1
	)
	default HoldCountFormat holdCountFormat()
	{
		return HoldCountFormat.SAME_AS_OVERLAY;
	}

	@ConfigItem(
		keyName = "holdCountSize",
		name = "Count size",
		description = "How big the count on the hold is drawn. 100% is RuneLite's<br>"
			+ "normal overlay text size.",
		section = CARGO_HOLD,
		position = 2
	)
	@Range(min = 50, max = 300)
	@Units(Units.PERCENT)
	default int holdCountSize()
	{
		return 100;
	}

	@ConfigItem(
		keyName = "holdColorStyle",
		name = "Color style",
		description = "Fade goes from the empty color through the nearly-full color<br>"
			+ "(at half full) to the full color as the hold fills. Steps<br>"
			+ "jumps from one color to the next at the two percentages<br>"
			+ "below, with no fade in between. One color always shows the<br>"
			+ "empty color.",
		section = CARGO_HOLD,
		position = 3
	)
	default HoldColorStyle holdColorStyle()
	{
		return HoldColorStyle.FADE;
	}

	// Steps by how full the hold is, so every hold size changes at the same point (owner, 2026-10-25: "yellow could be
	// 75% and red could be 100% with the option to change the %", "just for the steps").
	@ConfigItem(
		keyName = "holdStepsNearlyFull",
		name = "Steps: nearly full at",
		description = "With Steps, the count turns the nearly-full color once the<br>"
			+ "hold is this full.",
		section = CARGO_HOLD,
		position = 4
	)
	@Range(min = 1, max = 100)
	@Units(Units.PERCENT)
	default int holdStepsNearlyFull()
	{
		return 75;
	}

	@ConfigItem(
		keyName = "holdStepsFull",
		name = "Steps: full at",
		description = "With Steps, the count turns the full color once the hold is<br>"
			+ "this full.",
		section = CARGO_HOLD,
		position = 5
	)
	@Range(min = 1, max = 100)
	@Units(Units.PERCENT)
	default int holdStepsFull()
	{
		return 100;
	}

	@ConfigItem(
		keyName = "holdEmptyColor",
		name = "When empty",
		description = "The count's color while the hold has room (One color: always).",
		section = CARGO_HOLD,
		position = 6
	)
	default Color holdEmptyColor()
	{
		return Palette.HOLD_EMPTY;
	}

	@ConfigItem(
		keyName = "holdNearlyFullColor",
		name = "When nearly full",
		description = "The count's color at half full (Fade), or from \"Steps: nearly<br>"
			+ "full at\" (Steps).",
		section = CARGO_HOLD,
		position = 7
	)
	default Color holdNearlyFullColor()
	{
		return Palette.HOLD_NEARLY_FULL;
	}

	@ConfigItem(
		keyName = "holdFullColor",
		name = "When full",
		description = "The count's color when the hold is full (Fade and Steps).",
		section = CARGO_HOLD,
		position = 8
	)
	default Color holdFullColor()
	{
		return Palette.HOLD_FULL;
	}

	@ConfigItem(
		keyName = "showInfoBox",
		name = "Countdown infobox",
		description = "A small box among RuneLite's infoboxes with the time to a<br>"
			+ "full hold, so it stays in view with the overlay hidden or<br>"
			+ "scrolled away. Hover it for the detail.",
		section = OVERLAY,
		position = 11
	)
	default boolean showInfoBox()
	{
		return false;
	}

	// ---- Sidebar, then Wrecks ("Salvage spots" until 2.1, "Wrecks and parking" until 2.3) ----

	@ConfigItem(
		keyName = "spotSidebar",
		name = "Show sidebar",
		description = "Add a sidebar panel listing every salvaging hotspot, with<br>"
			+ "buttons to show it on the world map and to route there with<br>"
			+ "the Shortest Path plugin.",
		section = SIDEBAR,
		position = 0
	)
	default boolean spotSidebar()
	{
		return true;
	}

	@ConfigItem(
		keyName = "wreckMarking",
		name = "Mark wrecks",
		description = "How each wreck in view is marked. Tile boxes the tiles the<br>"
			+ "wreck stands on; Hull outlines the wreck itself, the way the<br>"
			+ "game outlines one under the mouse. Area boxes the water<br>"
			+ "round it where a hook can reach it. Wrecks that are up are<br>"
			+ "drawn in color (the tile or hull only when your Sailing<br>"
			+ "level lets you salvage it); sunk wrecks are drawn in gray,<br>"
			+ "since the next wreck rises on the same tile.",
		section = SPOTS,
		position = 0
	)
	default WreckMarking wreckMarking()
	{
		// A small tile box rather than the hull outline (owner, 2026-10-14).
		return WreckMarking.TILE_AND_AREA;
	}

	@ConfigItem(
		keyName = "wreckTimers",
		name = "Timers on wrecks",
		description = "Write on each wreck in view how long it has left: \"Sinks in<br>"
			+ "≤ 1:20\" while it is up (in cyan), and on a sunk one when the<br>"
			+ "next wreck can rise, \"Sunk - next ≤ 0:45\" (in gray; \"maybe<br>"
			+ "next\" when another sunk site could take it). A wreck's clock<br>"
			+ "starts only once a boat is seen beside it or your hook rolls<br>"
			+ "on it; until then it says \"no clock yet\".",
		section = SPOTS,
		position = 1
	)
	default boolean wreckTimers()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showSceneEdge",
		name = "Show what the plugin sees",
		description = "Draw a faint dashed square on the water round the area the<br>"
			+ "game has loaded. The plugin only knows about wrecks inside<br>"
			+ "it, so if a set of wrecks is outside, sail a little toward<br>"
			+ "it until the square moves past it. The game redraws this<br>"
			+ "area as you sail, so the square jumps now and then.",
		section = SPOTS,
		position = 11
	)
	default boolean showSceneEdge()
	{
		return false;
	}

	@ConfigItem(
		keyName = "boxFill",
		name = "Fill the boxes",
		description = "Shade the inside of the reach and double spot boxes. Off<br>"
			+ "draws the outlines only, so nothing on the water is tinted.",
		section = SPOTS,
		position = 10
	)
	default boolean boxFill()
	{
		return true;
	}

	@ConfigItem(
		keyName = "hideReachWhenParked",
		name = "Hide reach boxes once parked",
		description = "Take the boxes round wrecks off the water while every hook<br>"
			+ "on your boat is inside a double spot box. They come back the<br>"
			+ "moment you drift out.",
		section = SPOTS,
		position = 3
	)
	default boolean hideReachWhenParked()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
		keyName = "wreckReachColor",
		name = "Wreck reach color",
		description = "Outline color of the boxes round wrecks that are up (Mark<br>"
			+ "wrecks: Area). The fill is a fainter version of it. Sunk<br>"
			+ "wrecks' boxes are gray.",
		section = SPOTS,
		position = 2
	)
	default Color wreckReachColor()
	{
		return new Color(255, 215, 40, 190);
	}

	@ConfigItem(
		keyName = "doubleSpotBoxes",
		name = "Double spot boxes",
		description = "Box the water where one hook reaches two wreck sites at<br>"
			+ "once, whether or not both wrecks are up right now. Park so<br>"
			+ "every hook sits in the box and it changes color and says<br>"
			+ "Parked.",
		section = SPOTS,
		position = 4
	)
	default boolean doubleSpotBoxes()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
		keyName = "doubleSpotColor",
		name = "Double spot color",
		description = "Outline color of the double spot boxes while you are not<br>"
			+ "parked in them. The fill is a fainter version of it.",
		section = SPOTS,
		position = 5
	)
	default Color doubleSpotColor()
	{
		return new Color(70, 220, 90, 200);
	}

	@Alpha
	@ConfigItem(
		keyName = "doubleSpotParkedColor",
		name = "Parked color",
		description = "The color a double spot box changes to once every hook on<br>"
			+ "your boat is inside it: both hooks on a sloop, the one hook<br>"
			+ "on a raft or skiff.",
		section = SPOTS,
		position = 6
	)
	default Color doubleSpotParkedColor()
	{
		return new Color(51, 255, 255, 67);
	}

	@ConfigItem(
		keyName = "doubleSpotLabels",
		name = "Label double spots",
		description = "Write \"Double spot\" on each box where to park, with how many<br>"
			+ "of your hooks are in it, and \"Parked\" once they all are.",
		section = SPOTS,
		position = 7
	)
	default boolean doubleSpotLabels()
	{
		return true;
	}

	@Range(min = 0, max = 600)
	@Units(Units.SECONDS)
	@ConfigItem(
		keyName = "doubleSpotHideSeconds",
		name = "Hide doubles after",
		description = "How long after the boat stops the double spot boxes<br>"
			+ "disappear, at login too; they come back the moment the boat<br>"
			+ "moves. 0 keeps them on screen.",
		section = SPOTS,
		position = 8
	)
	default int doubleSpotHideSeconds()
	{
		// 10 since 2.3, was 20 (owner, 2026-10-18). RuneLite saves a setting's default the first time the plugin
		// runs, so players who already had it keep their 20; only new installs start at 10.
		return 10;
	}

	@ConfigItem(
		keyName = "parkedTextAfterHide",
		name = "Say \"Parked\" after hiding",
		description = "Once the double spot boxes have hidden, keep the words<br>"
			+ "\"Parked in double spot\" in the middle of your boat while<br>"
			+ "every hook is in it. Only the text, no box.",
		section = SPOTS,
		position = 9
	)
	default boolean parkedTextAfterHide()
	{
		// On by default (owner, 2026-10-04).
		return true;
	}

	@ConfigItem(
		keyName = "nearestDock",
		name = "Show nearest docks",
		description = "Name the nearest dock you can use and the nearest dock where<br>"
			+ "your crew bank the hold as you step off (a dock with a bank<br>"
			+ "deposit box, or the bank boat) at the top of the sidebar,<br>"
			+ "each with buttons to show it on the world map and to route<br>"
			+ "there with the Shortest Path plugin.",
		section = SIDEBAR,
		position = 1
	)
	default boolean nearestDock()
	{
		return true;
	}

	@ConfigItem(
		keyName = "dockRequirements",
		name = "Only docks I can use",
		description = "Name only docks whose Sailing level (unboosted) and quest<br>"
			+ "you meet, and say what a nearer one needs when there is one.<br>"
			+ "Off names the nearest whatever they need.",
		section = SIDEBAR,
		position = 2
	)
	default boolean dockRequirements()
	{
		return true;
	}

	@ConfigItem(
		keyName = "autoRouteOnBoarding",
		name = "Auto route when boarding",
		description = "When you board your boat from a dock, hand the spot marked<br>"
			+ "Auto in the sidebar to the Shortest Path plugin so the route<br>"
			+ "is drawn without pressing anything. Only one spot can be<br>"
			+ "marked. The route is cleared when you reach the spot or<br>"
			+ "leave your boat.",
		section = SIDEBAR,
		position = 3
	)
	default boolean autoRouteOnBoarding()
	{
		return true;
	}

	@ConfigSection(
		name = "Inventory sorting",
		description = "Boxes round inventory items saying what to keep, put in the<br>"
			+ "hold, alch or drop",
		position = 9,
		closedByDefault = true
	)
	String SORTING = "sorting";

	// Their own section from 2.3 (issue #14, 2026-10-19: a player could not find how to turn them off under
	// Inventory sorting and Reminders). Keys unchanged, so saved choices carry over.
	@ConfigSection(
		name = "Next step highlights",
		description = "Outlines on your boat for where to go next: the salvaging<br>"
			+ "station, the cargo hold or a hook",
		position = 10,
		closedByDefault = true
	)
	String NEXT_STEP = "nextStep";

	// The count written on the cargo hold, and how it looks (owner, 2026-10-25: "make it its own setting group called
	// Cargo Hold"). "Count on the hold" moved here from Overlay; its key is unchanged, so saved choices carry over.
	@ConfigSection(
		name = "Cargo hold",
		description = "The count written on the cargo hold: what it reads, its size<br>"
			+ "and its colors",
		position = 1,
		closedByDefault = true
	)
	String CARGO_HOLD = "cargoHold";

	// The plugin's own chat lines (owner, 2026-10-25: "add a setting category called chat messages? Move the Say when
	// updated into that as well"). Keys unchanged, so saved choices carry over.
	@ConfigSection(
		name = "Chat messages",
		description = "The plugin's own lines in the chat box",
		position = 11,
		closedByDefault = true
	)
	String CHAT = "chat";

	// ---- Inventory sorting ----

	@ConfigItem(
		keyName = "inventorySort",
		name = "Box inventory items",
		description = "Draw a colored box round each item in your inventory: keep<br>"
			+ "(sage), deposit (cyan), alch (green) or drop (red). Unmarked<br>"
			+ "items are sorted by what they are: plain bronze to dragon<br>"
			+ "cannonballs (never noted) to deposit, items that alch for at<br>"
			+ "least the threshold to alch, and everything else gets no<br>"
			+ "box. Drop is only what you mark. Shift-right-click an item<br>"
			+ "to mark it.",
		section = SORTING,
		position = 0
	)
	default boolean inventorySort()
	{
		return true;
	}

	@ConfigItem(
		keyName = "sortWhere",
		name = "Show boxes",
		description = "Where the boxes are drawn. A salvaging spot is anywhere a<br>"
			+ "wreck site is in view.",
		section = SORTING,
		position = 1
	)
	default SortWhere sortWhere()
	{
		return SortWhere.AT_WRECKS;
	}

	@ConfigItem(
		keyName = "alchThreshold",
		name = "Alch from",
		description = "An unmarked item that alchs for at least this much gets the<br>"
			+ "alch box; below it, no box.",
		section = SORTING,
		position = 2
	)
	@Range(min = 0, max = 10_000_000)
	default int alchThreshold()
	{
		return 1000;
	}

	@ConfigItem(
		keyName = "geOverAlchPercent",
		name = "GE beats alch by",
		description = "An item that would be alched gets the keep box instead when<br>"
			+ "its Grand Exchange price beats its alch value by at least<br>"
			+ "this much, since it is worth carrying home. 0 turns this<br>"
			+ "off.",
		section = SORTING,
		position = 3
	)
	@Range(min = 0, max = 1000)
	@Units(Units.PERCENT)
	default int geOverAlchPercent()
	{
		return 0;
	}

	@ConfigItem(
		keyName = "sortMenu",
		name = "Shift-right-click to mark",
		description = "Add one AFK Shipwrecks entry to an inventory item's menu<br>"
			+ "while Shift is held, opening to Keep / Deposit / Alch / Drop<br>"
			+ "and Unmark, only while you are on your own boat (anywhere,<br>"
			+ "if the boxes are set to show everywhere). These send nothing<br>"
			+ "to the game.",
		section = SORTING,
		position = 4
	)
	default boolean sortMenu()
	{
		return true;
	}

	@ConfigItem(
		keyName = "boxKeep",
		name = "Box kept items",
		description = "Draw the keep box at all. Off, the default, leaves kept<br>"
			+ "items plain so only the others stand out.",
		section = SORTING,
		position = 5
	)
	default boolean boxKeep()
	{
		// Off by default (owner, 2026-10-04): what stays needs no box.
		return false;
	}

	@ConfigItem(
		keyName = "salvageGuide",
		name = "Salvage highlight",
		description = "On your boat, outline the next step for your salvage.<br>"
			+ "Automatic: the salvaging station while you carry salvage<br>"
			+ "(the cargo hold if your boat has no station), brighter once<br>"
			+ "your inventory is full; after sorting, drop the Drop items<br>"
			+ "and alch the Alch items first, then the cargo hold lights up<br>"
			+ "for Deposit items, or for more salvage. Station or Cargo<br>"
			+ "hold light only that one.",
		section = NEXT_STEP,
		position = 0
	)
	default GuideTarget salvageGuide()
	{
		return GuideTarget.AUTOMATIC;
	}

	@ConfigItem(
		keyName = "guideHook",
		name = "Hook highlight",
		description = "When there is nothing left to sort or deposit and a wreck is<br>"
			+ "in reach, outline a hook you could hook from yourself: a<br>"
			+ "free one, or else the one with your weakest crewmate<br>"
			+ "(clicking it sends them to rest). If the cargo hold holds<br>"
			+ "enough salvage to fill your free inventory, the hold lights<br>"
			+ "instead, even after a look inside. Opening the hold and<br>"
			+ "taking nothing out otherwise leaves only the hook lit.",
		section = NEXT_STEP,
		position = 1
	)
	default GuideHook guideHook()
	{
		return GuideHook.WEAKEST_CREW;
	}

	@ConfigItem(
		keyName = "boxHoldSalvage",
		name = "Box salvage in the hold",
		description = "While the cargo hold is open, box the salvage inside it so<br>"
			+ "it is easy to see and click among the cannonballs and kits.<br>"
			+ "Nothing else in the hold is boxed.",
		section = SORTING,
		position = 12
	)
	default boolean boxHoldSalvage()
	{
		return true;
	}

	@Alpha
	@ConfigItem(keyName = "keepColor", name = "Keep color", description = "", section = SORTING, position = 6)
	default Color keepColor()
	{
		return new Color(154, 163, 122, 255);
	}

	@Alpha
	@ConfigItem(keyName = "holdColor", name = "Deposit color", description = "", section = SORTING, position = 7)
	default Color holdColor()
	{
		return new Color(60, 220, 230, 220);
	}

	@Alpha
	@ConfigItem(keyName = "alchColor", name = "Alch color", description = "", section = SORTING, position = 8)
	default Color alchColor()
	{
		return new Color(70, 220, 90, 220);
	}

	@Alpha
	@ConfigItem(keyName = "dropColor", name = "Drop color", description = "", section = SORTING, position = 9)
	default Color dropColor()
	{
		return new Color(240, 80, 80, 220);
	}

	/**
	 * 2.1's "Highlight the cargo hold" (off by default), folded into "Highlight where salvage goes" (now "Salvage highlight") in 2.2, which
	 * lights the hold whenever Deposit items are carried. Kept hidden and unread so the key is never reused.
	 */
	@ConfigItem(keyName = "highlightCargoHold", name = "", description = "", hidden = true)
	default boolean highlightCargoHold()
	{
		return false;
	}

	/** Item ids the game has said the cargo hold accepts, learned while it is open. */
	@ConfigItem(keyName = "holdAcceptedIds", name = "", description = "", hidden = true)
	default String holdAcceptedIds()
	{
		return "";
	}

	/** Item ids the game has said the cargo hold refuses, learned while it is open. */
	@ConfigItem(keyName = "holdRefusedIds", name = "", description = "", hidden = true)
	default String holdRefusedIds()
	{
		return "";
	}

	@ConfigItem(
		keyName = "updateMessage",
		name = "Say when updated",
		description = "One line in chat, once, the first time you log in after the<br>"
			+ "Plugin Hub has updated the plugin, saying what the new<br>"
			+ "version brings.",
		section = CHAT,
		position = 0
	)
	default boolean updateMessage()
	{
		return true;
	}

	@ConfigItem(
		keyName = "pluginConflicts",
		name = "Plugin conflicts",
		description = "Once each login, the first time you are on your boat at a<br>"
			+ "salvaging spot: when another plugin shows the same things<br>"
			+ "(the Sailing plugin's cargo hold count or wreck outlines), a<br>"
			+ "line in chat says which of its settings to turn off to see<br>"
			+ "only these.",
		section = CHAT,
		position = 1
	)
	default boolean pluginConflicts()
	{
		return true;
	}

	// ---- 2.0's two wreck switches, replaced by "Mark wrecks" in 2.1; read once to carry a choice over ----

	@ConfigItem(keyName = "wreckReachBoxes", name = "", description = "", hidden = true)
	default boolean wreckReachBoxes()
	{
		return true;
	}

	@ConfigItem(keyName = "outlineWrecks", name = "", description = "", hidden = true)
	default boolean outlineWrecks()
	{
		return true;
	}

	/** 2.2's "Hide crew orders in chat" switch, read once by the first start on 2.3 for "Hide crew text". */
	@ConfigItem(keyName = "hideCrewOrders", name = "", description = "", hidden = true)
	default boolean hideCrewOrders()
	{
		return true;
	}

	/**
	 * 2.3's "Flash the swap on screen" switch, read once by the first start on 2.3.1 for "Swap on screen". Its default is
	 * on again, so a fresh install (whose default RuneLite stores before the plugin starts) carries over to the steady
	 * notice; a 2.3 install that was given "off" keeps it off.
	 */
	@ConfigItem(keyName = "swapAlert", name = "", description = "", hidden = true)
	default boolean swapAlert()
	{
		return true;
	}

	@ConfigItem(keyName = "swapNoticeCarried", name = "", description = "", hidden = true)
	default boolean swapNoticeCarried()
	{
		return false;
	}

	@ConfigItem(keyName = "crewTextHidingCarried", name = "", description = "", hidden = true)
	default boolean crewTextHidingCarried()
	{
		return false;
	}

	@ConfigItem(keyName = "wreckMarkingCarried", name = "", description = "", hidden = true)
	default boolean wreckMarkingCarried()
	{
		return false;
	}

	/** Set once 2.2 has moved the old default, Hull and area, to Tile and area. */
	@ConfigItem(keyName = "wreckTileCarried", name = "", description = "", hidden = true)
	default boolean wreckTileCarried()
	{
		return false;
	}

	// ---- Remembered sidebar choices; set from the sidebar, not shown in the settings ----

	@ConfigItem(
		keyName = "lastVersion",
		name = "",
		description = "",
		hidden = true
	)
	default String lastVersion()
	{
		return "";
	}

}
