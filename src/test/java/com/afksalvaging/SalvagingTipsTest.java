/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SalvagingTipsTest
{
	@Test
	public void tipsExplainTheBoxesAndQuoteTheReachInUse()
	{
		String all = String.join(" ", SalvagingTips.all());
		assertTrue(all.contains("hook, so a hook has to sit inside a box"));
		assertTrue(all.contains("Reach is taken as " + AfkSession.HOOK_RANGE + " tiles"));
		assertTrue(all.contains("Shortest Path"));
	}

	@Test
	public void htmlListsEveryTip()
	{
		String html = SalvagingTips.html(195);
		assertTrue(html.startsWith("<html>"));
		assertEquals(SalvagingTips.all().size(), html.split("<li").length - 1);
	}
}
