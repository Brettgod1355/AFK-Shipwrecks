/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

/** What the dimmer darkens (2.5). The constant names are what RuneLite saves. */
public enum DimmerCovers
{
	/** The game world only: the chat box, the sidebar, the minimap and open windows stay bright. */
	GAME_VIEW("Game view"),
	/** The whole game except the chat box. */
	ALL_BUT_CHAT("All but chat"),
	EVERYTHING("Everything");

	private final String label;

	DimmerCovers(String label)
	{
		this.label = label;
	}

	@Override
	public String toString()
	{
		return label;
	}
}
