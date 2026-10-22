/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

/**
 * The SWAP CREW notice in the middle of the screen: shown steady, flashing, or not at all (owner, 2026-10-22: "change
 * the overlay for swap crew so its not flashing (at least not by default) but actually just have it display"). The
 * constant names are what RuneLite saves.
 */
public enum SwapNotice
{
	ON("On"),
	FLASH("Flash"),
	OFF("Off");

	private final String label;

	SwapNotice(String label)
	{
		this.label = label;
	}

	/**
	 * The choice carried over from 2.3's switch ("Flash the swap on screen"): off stays off; on becomes the steady
	 * notice, the new default, since a player who never touched it got it on by default.
	 */
	static SwapNotice carried(boolean switchOn)
	{
		return switchOn ? ON : OFF;
	}

	@Override
	public String toString()
	{
		return label;
	}
}
