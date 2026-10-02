/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

import java.awt.Color;

/**
 * The colours that are fixed rather than settings: the overlay's text, the counter's shading, the
 * sidebar and the chat lines. The colour settings for these were removed to keep the settings panel
 * simple (owner, 2026-10-02); the values are the defaults those settings had. The banner, box and
 * sorting colours are still settings.
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

	// Sidebar.
	static final Color SIDEBAR_IN_LEVEL = new Color(70, 200, 110);
	static final Color SIDEBAR_BELOW_LEVEL = new Color(200, 110, 110);
	static final Color SIDEBAR_MARKED = new Color(255, 200, 60);

	/** The chat lines' colour, as a {@code <col=>} tag value (30, 90, 168). */
	static final String CHAT_TIP = "1e5aa8";

	private Palette()
	{
	}
}
