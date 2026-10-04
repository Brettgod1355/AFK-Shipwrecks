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
		description = "How every notification in this plugin is sent, unless its own setting is switched off or set to "
			+ "custom with the cog. Left plain, it is RuneLite's own notification settings; set it to custom once and "
			+ "every category follows it. Switching it off silences every category that is not custom.",
		position = 0
	)
	default Notification defaultNotification()
	{
		return Notification.ON;
	}

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

	@ConfigSection(
		name = "Salvage spots",
		description = "The sidebar list of hotspots and the boxes on the water that show where to park",
		position = 6
	)
	String SPOTS = "spots";

	// ---- Timer ----

	@ConfigItem(
		keyName = "showTimer",
		name = "Show timer",
		description = "Show how long until the cargo hold is full at the current salvaging rate. Status lines such as "
			+ "waiting for a wreck or the crew having stopped are shown either way.",
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
		description = "Show the time the hold will be full as 12-hour (6:25 PM) or 24-hour (18:25).",
		section = TIMER,
		position = 2
	)
	default ClockFormat clockFormat()
	{
		return ClockFormat.TWELVE_HOUR;
	}

	@ConfigItem(
		keyName = "countHookedSalvage",
		name = "Count salvage you hooked",
		description = "Treat salvage you hooked yourself and have not yet deposited as bound for the hold. "
			+ "Salvage you withdrew to sort is never counted.",
		section = TIMER,
		position = 3
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
		position = 4
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
		position = 5
	)
	default boolean worldTip()
	{
		return true;
	}

	@ConfigItem(
		keyName = "worldAlert",
		name = "Salvaging world alert",
		description = "While you sit at a spot with no wreck up for a quarter of a minute on a world that is not a "
			+ "salvaging world, flash a notice in the middle of the screen with the worlds to hop to. It goes when a "
			+ "wreck is up or you hop. The tip above says it once in chat; this keeps saying it.",
		section = TIMER,
		position = 6
	)
	default boolean worldAlert()
	{
		return true;
	}

	// ---- Reminders ----

	@ConfigItem(
		keyName = "hookEmptyNotification",
		name = "Hook empty, crewmate free",
		description = "Notify when a salvaging hook is standing empty and a crewmate aboard could be working it, "
			+ "for example after you step off your hook to sort. Only at a salvaging spot, with a wreck site, up or "
			+ "sunk, within reach of your hooks.",
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
		keyName = "betterCrewNotification",
		name = "Better crewmate free",
		description = "A crewmate with more deckhandiness is sitting idle while a weaker one works a hook: say who to "
			+ "swap, once, after the pair has stood for ten seconds. Only when every hook is manned; an empty hook "
			+ "gets its own reminder first.",
		section = REMINDERS,
		position = 3
	)
	default Notification betterCrewNotification()
	{
		return Notification.ON;
	}

	@ConfigItem(
		keyName = "swapAlert",
		name = "Flash the swap on screen",
		description = "While a better crewmate is free, flash a SWAP CREW notice in the middle of the screen, in the "
			+ "reminder banner colour, until the swap is made.",
		section = REMINDERS,
		position = 4
	)
	default boolean swapAlert()
	{
		return true;
	}

	@ConfigItem(
		keyName = "waitingNotification",
		name = "Waiting for a wreck",
		description = "Notify once every wreck in reach has been down for ten seconds, with the latest the next can rise "
			+ "when that is known. Off unless you want it.",
		section = REMINDERS,
		position = 5
	)
	default Notification waitingNotification()
	{
		return Notification.OFF;
	}

	@ConfigItem(
		keyName = "sortingDoneNotification",
		name = "Done sorting at the station",
		description = "Notify five seconds after the game says you have no more salvage to sort, unless you have clicked "
			+ "anything or started sorting again by then: it is for when you have wandered off. Off unless you want it.",
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
		description = "How long a hook may stand empty before the first reminder. Using the cargo hold buys a "
			+ "little extra; settling in to sort shortens it.",
		section = REMINDERS,
		position = 7
	)
	@Range(min = 3, max = 60)
	@Units(Units.SECONDS)
	default int reminderGraceSeconds()
	{
		return 10;
	}

	@ConfigItem(
		keyName = "reminderRepeatSeconds",
		name = "Repeat every",
		description = "Remind again this often while the hook stays empty. 0 reminds once.",
		section = REMINDERS,
		position = 8
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
		description = "Notify shortly before the game logs you out for idling while you are on your own boat, so a "
			+ "full-hold alert does not arrive after you are gone.",
		section = REMINDERS,
		position = 9
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
		position = 10
	)
	@Range(min = 0, max = 600)
	@Units(Units.SECONDS)
	default int idleLogoutWarnSeconds()
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
		keyName = "warningBannerColor",
		name = "Nearly full colour",
		description = "Background colour of the banner for the early warning, so you can tell it from the full one at a glance.",
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
		name = "Reminder colour",
		description = "Background colour of the banner when a hook needs attention.",
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
		description = "Near wrecks: the timer, counter, hooks and wrecks lines show only while a wreck site is in view, "
			+ "so sailing from port to port stays clear. Always aboard: show them whenever you are on your boat. "
			+ "Alerts and banners show either way.",
		section = OVERLAY,
		position = -2
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
		position = 0
	)
	default boolean showCounter()
	{
		return true;
	}

	@ConfigItem(
		keyName = "counterStyle",
		name = "Counter shows",
		description = "What the counter shows: used out of total (154/160), slots left (6 left), or percent full (96%).",
		section = OVERLAY,
		position = 11
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
		keyName = "counterScale",
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

	@ConfigItem(
		keyName = "showInfoBox",
		name = "Countdown infobox",
		description = "A small box among RuneLite's infoboxes with the time to a full hold, so it stays in view with "
			+ "the overlay hidden or scrolled away. Hover it for the detail.",
		section = OVERLAY,
		position = 10
	)
	default boolean showInfoBox()
	{
		return false;
	}

	// ---- Salvage spots ----

	@ConfigItem(
		keyName = "spotSidebar",
		name = "Show sidebar",
		description = "Add a sidebar panel listing every salvaging hotspot, with buttons to show it on the world map "
			+ "and to route there with the Shortest Path plugin.",
		section = SPOTS,
		position = 0
	)
	default boolean spotSidebar()
	{
		return true;
	}

	@ConfigItem(
		keyName = "wreckReachBoxes",
		name = "Wreck reach boxes",
		description = "Box the water around each wreck in view where a hook can reach it. Sunk wrecks get a dimmer box, "
			+ "since the next wreck rises on the same tile.",
		section = SPOTS,
		position = 2
	)
	default boolean wreckReachBoxes()
	{
		return true;
	}

	@ConfigItem(
		keyName = "hideReachWhenParked",
		name = "Hide reach boxes once parked",
		description = "Take the wreck reach boxes off the water while every hook on your boat is inside a double spot box. "
			+ "They come back the moment you drift out.",
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
		name = "Wreck reach colour",
		description = "Outline colour of the wreck reach boxes. The fill is a fainter version of it.",
		section = SPOTS,
		position = 4
	)
	default Color wreckReachColor()
	{
		return new Color(255, 215, 40, 190);
	}

	@ConfigItem(
		keyName = "outlineWrecks",
		name = "Outline wrecks that are up",
		description = "Outline the hull of every wreck in view that is up and that your Sailing level lets you salvage, "
			+ "the way the game outlines one under the mouse. Sunk wrecks and wrecks above your level get nothing.",
		section = SPOTS,
		position = 5
	)
	default boolean outlineWrecks()
	{
		return true;
	}

	@ConfigItem(
		keyName = "doubleSpotBoxes",
		name = "Double spot boxes",
		description = "Box the water where one hook reaches two wreck sites at once, whether or not both wrecks are up "
			+ "right now. Park so every hook sits in the box and it changes colour and says Parked.",
		section = SPOTS,
		position = 6
	)
	default boolean doubleSpotBoxes()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
		keyName = "doubleSpotColor",
		name = "Double spot colour",
		description = "Outline colour of the double spot boxes while you are not parked in them. The fill is a fainter "
			+ "version of it.",
		section = SPOTS,
		position = 7
	)
	default Color doubleSpotColor()
	{
		return new Color(70, 220, 90, 200);
	}

	@Alpha
	@ConfigItem(
		keyName = "doubleSpotParkedColor",
		name = "Parked colour",
		description = "The colour a double spot box changes to once every hook on your boat is inside it: both hooks on a "
			+ "sloop, the one hook on a raft or skiff.",
		section = SPOTS,
		position = 8
	)
	default Color doubleSpotParkedColor()
	{
		return new Color(51, 255, 255, 67);
	}

	@ConfigItem(
		keyName = "doubleSpotLabels",
		name = "Label double spots",
		description = "Write \"Double spot\" on each box where to park, with how many of your hooks are in it, and "
			+ "\"Parked\" once they all are.",
		section = SPOTS,
		position = 9
	)
	default boolean doubleSpotLabels()
	{
		return true;
	}

	@Range(min = 0, max = 600)
	@Units(Units.SECONDS)
	@ConfigItem(
		keyName = "doubleSpotHideSeconds",
		name = "Hide double spot boxes after",
		description = "How long after the boat stops the double spot boxes disappear, at login too; they come back the "
			+ "moment the boat moves. 0 keeps them on screen.",
		section = SPOTS,
		position = 10
	)
	default int doubleSpotHideSeconds()
	{
		return 20;
	}

	@ConfigItem(
		keyName = "nearestDock",
		name = "Show nearest docks",
		description = "Name the nearest dock you can use and the nearest dock where your crew bank the hold as you step off "
			+ "(a dock with a bank deposit box, or the bank boat) at the top of the sidebar, each with buttons to show "
			+ "it on the world map and to route there with the Shortest Path plugin.",
		section = SPOTS,
		position = 11
	)
	default boolean nearestDock()
	{
		return true;
	}

	@ConfigItem(
		keyName = "dockRequirements",
		name = "Only docks I can use",
		description = "Name only docks whose Sailing level (unboosted) and quest you meet, and say what a nearer one "
			+ "needs when there is one. Off names the nearest whatever they need.",
		section = SPOTS,
		position = 13
	)
	default boolean dockRequirements()
	{
		return true;
	}

	@ConfigItem(
		keyName = "autoRouteOnBoarding",
		name = "Auto route when boarding",
		description = "When you board your boat from a dock, hand the spot marked Auto in the sidebar to the Shortest "
			+ "Path plugin so the route is drawn without pressing anything. Only one spot can be marked. The route is "
			+ "cleared when you reach the spot or leave your boat.",
		section = SPOTS,
		position = 12
	)
	default boolean autoRouteOnBoarding()
	{
		return true;
	}

	@ConfigSection(
		name = "Inventory sorting",
		description = "Boxes round inventory items saying what to keep, put in the hold, alch or drop",
		position = 7
	)
	String SORTING = "sorting";

	// ---- Inventory sorting ----

	@ConfigItem(
		keyName = "inventorySort",
		name = "Box inventory items",
		description = "Draw a coloured box round each item in your inventory: keep (sage), deposit (cyan), alch (green) "
			+ "or drop (red). Unmarked items are sorted by what they are: plain bronze to dragon cannonballs (never noted) to deposit, "
			+ "items that alch for at least the threshold to alch, and everything else gets no box. Drop is only "
			+ "what you mark. Shift-right-click an item to mark it.",
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
		description = "Where the boxes are drawn. A salvaging spot is anywhere a wreck site is in view.",
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
		description = "An unmarked item that alchs for at least this much gets the alch box; below it, no box.",
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
		name = "Keep if GE beats alch by",
		description = "An item that would be alched gets the keep box instead when its Grand Exchange price beats its "
			+ "alch value by at least this much, since it is worth carrying home. 0 turns this off.",
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
		description = "Add one AFK Shipwrecks entry to an inventory item's menu while Shift is held, opening to Keep / "
			+ "Deposit / Alch / Drop and Unmark, "
			+ "only while you are on your own boat (anywhere, if the boxes are set to show everywhere). These send "
			+ "nothing to the game.",
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
		description = "Draw the keep box at all. Off leaves kept items plain so only the others stand out.",
		section = SORTING,
		position = 5
	)
	default boolean boxKeep()
	{
		return true;
	}

	@ConfigItem(
		keyName = "highlightCargoHold",
		name = "Highlight the cargo hold",
		description = "Outline the cargo hold on your boat in the Deposit colour, so where the cyan-boxed items go is marked "
			+ "the same way.",
		section = SORTING,
		position = 10
	)
	default boolean highlightCargoHold()
	{
		return false;
	}

	@ConfigItem(
		keyName = "boxHoldSalvage",
		name = "Box salvage in the hold",
		description = "While the cargo hold is open, box the salvage inside it so it is easy to see and click among the "
			+ "cannonballs and kits. Nothing else in the hold is boxed.",
		section = SORTING,
		position = 11
	)
	default boolean boxHoldSalvage()
	{
		return true;
	}

	@Alpha
	@ConfigItem(keyName = "keepColor", name = "Keep colour", description = "", section = SORTING, position = 6)
	default Color keepColor()
	{
		return new Color(154, 163, 122, 255);
	}

	@Alpha
	@ConfigItem(keyName = "holdColor", name = "Deposit colour", description = "", section = SORTING, position = 7)
	default Color holdColor()
	{
		return new Color(60, 220, 230, 220);
	}

	@Alpha
	@ConfigItem(keyName = "alchColor", name = "Alch colour", description = "", section = SORTING, position = 8)
	default Color alchColor()
	{
		return new Color(70, 220, 90, 220);
	}

	@Alpha
	@ConfigItem(keyName = "dropColor", name = "Drop colour", description = "", section = SORTING, position = 9)
	default Color dropColor()
	{
		return new Color(240, 80, 80, 220);
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
		description = "One line in chat, once, the first time you log in after the Plugin Hub has updated the plugin, "
			+ "saying what the new version brings.",
		position = 100
	)
	default boolean updateMessage()
	{
		return true;
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
