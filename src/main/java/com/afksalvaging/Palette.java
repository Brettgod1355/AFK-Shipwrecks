/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

import java.awt.Color;

/**
 * The colors that are fixed rather than settings: the overlay's text, the counter's shading, the
 * sidebar and the chat lines. The color settings for these were removed to keep the settings panel
 * simple (owner, 2026-10-02); the values are the defaults those settings had. The banner, box and
 * sorting colors are still settings.
 */
final class Palette
{
	// Overlay text and the infobox.
	static final Color TEXT = new Color(255, 255, 255);
	static final Color DIM = new Color(190, 190, 190);
	static final Color ATTENTION = new Color(255, 190, 70);
	static final Color GOOD = new Color(120, 220, 120);
	static final Color BAD = new Color(240, 90, 90);
	static final Color COUNTER_EMPTY = new Color(70, 200, 70);
	static final Color COUNTER_FULL = new Color(230, 60, 60);
	/** The outline round a wreck that is up and salvageable: the game's own hover cyan, a little softer. */
	static final Color WRECK_OUTLINE = new Color(90, 220, 230);
	/**
	 * Sunk wrecks, hull and box alike: gray, so a site waiting for its next wreck reads as inactive at a
	 * glance (owner, 2026-10-07: "shows an overlay in gray when a spot is inactive").
	 */
	static final Color WRECK_SUNK = new Color(150, 150, 150);
	/** A sunk wreck's timer: gray like its outline, lighter so the words read on dark water. */
	static final Color WRECK_SUNK_TEXT = new Color(190, 190, 190);
	/** The faint square round what the game has loaded (owner, 2026-10-08: "a subtle color"). */
	static final Color SCENE_EDGE = new Color(255, 255, 255, 90);

	// Sidebar.
	static final Color SIDEBAR_IN_LEVEL = new Color(70, 200, 110);
	static final Color SIDEBAR_BELOW_LEVEL = new Color(200, 110, 110);
	static final Color SIDEBAR_MARKED = new Color(255, 200, 60);
	/** The Tips tab's wiki link: the chat lines' blue on the transparent chatbox, readable on the dark sidebar. */
	static final Color SIDEBAR_LINK = new Color(0x80, 0xC0, 0xFF);

	/** The empty-hook guide's flashing boxes: bright neon green (owner, 2026-10-07). */
	static final Color HOOK_GUIDE = new Color(57, 255, 20);
	/** The salvage highlight on the salvaging station: where carried salvage gets sorted (owner, 2026-10-12). */
	static final Color GUIDE_STATION = new Color(255, 170, 40);
	/** The salvage highlight on a hook: the same green as the empty-hook guide, since both mean "a hook". */
	static final Color GUIDE_HOOK = HOOK_GUIDE;
	/** The chat lines' color on the parchment chatbox, as a {@code <col=>} tag value (30, 90, 168). */
	static final String CHAT_TIP = "1e5aa8";
	/** The chat lines' color on the transparent chatbox, where the dark blue is hard to read (128, 192, 255). */
	static final String CHAT_TIP_TRANSPARENT = "80c0ff";

	private Palette()
	{
	}
}
