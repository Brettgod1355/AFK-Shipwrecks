/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

import java.util.ArrayDeque;
import java.util.Deque;
import net.runelite.api.MessageNode;

/**
 * The plugin's own chat lines, in a blue that suits the chatbox: dark on the parchment, light on the
 * transparent chatbox, where the dark blue was hard to read (owner, 2026-10-07). The recent lines are
 * kept so they can be re-colored when the player switches transparency or between fixed and
 * resizable.
 */
final class ChatLines
{
	/** How many recent lines are re-colored; older ones have long scrolled out of view. */
	static final int KEPT = 30;

	private final Deque<Line> lines = new ArrayDeque<>();

	static String format(String text, boolean transparent)
	{
		return "<col=" + (transparent ? Palette.CHAT_TIP_TRANSPARENT : Palette.CHAT_TIP) + ">" + text + "</col>";
	}

	void remember(MessageNode node, String text)
	{
		if (node == null)
		{
			return;
		}
		lines.addLast(new Line(node, text));
		while (lines.size() > KEPT)
		{
			lines.removeFirst();
		}
	}

	void recolor(boolean transparent)
	{
		for (Line line : lines)
		{
			line.node.setValue(format(line.text, transparent));
		}
	}

	int size()
	{
		return lines.size();
	}

	void clear()
	{
		lines.clear();
	}

	private static final class Line
	{
		final MessageNode node;
		final String text;

		Line(MessageNode node, String text)
		{
			this.node = node;
			this.text = text;
		}
	}
}
