/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

/**
 * Where crew text is hidden: the text over crewmates' heads, players' orders to their crew over their heads, and the
 * player's own orders in the chat box (owner, 2026-10-18: "add an option or dropdown to toggle between apply
 * everywhere and apply at salvage spots"). The constant names are what RuneLite saves.
 */
public enum CrewTextHiding
{
	OFF("Off"),
	SALVAGING_SPOTS("At salvaging spots"),
	EVERYWHERE("Everywhere");

	private final String label;

	CrewTextHiding(String label)
	{
		this.label = label;
	}

	/** Whether crew text is hidden right now, given whether the player is at a salvaging spot. */
	boolean applies(boolean atSpot)
	{
		return this == EVERYWHERE || (this == SALVAGING_SPOTS && atSpot);
	}

	/**
	 * The choice carried over from 2.2's on/off switch ("Hide crew orders in chat"): off stays off, and on becomes
	 * "At salvaging spots", the only place the switch ever applied.
	 */
	static CrewTextHiding carried(boolean switchOn)
	{
		return switchOn ? SALVAGING_SPOTS : OFF;
	}

	@Override
	public String toString()
	{
		return label;
	}
}
