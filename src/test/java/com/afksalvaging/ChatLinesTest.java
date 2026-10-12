/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicReference;
import net.runelite.api.MessageNode;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class ChatLinesTest
{
	/** A message node that only remembers its value. */
	private static MessageNode node(AtomicReference<String> value)
	{
		return (MessageNode) Proxy.newProxyInstance(MessageNode.class.getClassLoader(), new Class<?>[]{MessageNode.class},
			(proxy, method, args) ->
			{
				if (method.getName().equals("setValue"))
				{
					value.set((String) args[0]);
				}
				return method.getName().equals("getValue") ? value.get() : null;
			});
	}

	@Test
	public void eachChatboxGetsItsOwnBlue()
	{
		assertEquals("<col=" + Palette.CHAT_TIP + ">Hi</col>", ChatLines.format("Hi", false));
		assertEquals("<col=" + Palette.CHAT_TIP_TRANSPARENT + ">Hi</col>", ChatLines.format("Hi", true));
		assertNotEquals(Palette.CHAT_TIP, Palette.CHAT_TIP_TRANSPARENT);
	}

	@Test
	public void switchingTransparencyRecolorsTheLinesAlreadyShown()
	{
		ChatLines lines = new ChatLines();
		AtomicReference<String> value = new AtomicReference<>(ChatLines.format("Auto route", false));
		lines.remember(node(value), "Auto route");

		lines.recolor(true);
		assertEquals(ChatLines.format("Auto route", true), value.get());
		lines.recolor(false);
		assertEquals(ChatLines.format("Auto route", false), value.get());
	}

	@Test
	public void onlyTheRecentLinesAreKept()
	{
		ChatLines lines = new ChatLines();
		lines.remember(null, "lost");
		assertEquals(0, lines.size());
		AtomicReference<String> first = new AtomicReference<>("first");
		lines.remember(node(first), "first");
		for (int i = 0; i < ChatLines.KEPT; i++)
		{
			lines.remember(node(new AtomicReference<>()), "line " + i);
		}
		assertEquals(ChatLines.KEPT, lines.size());
		lines.recolor(true);
		// The oldest line has been dropped, so it is left as it was.
		assertEquals("first", first.get());
	}
}
