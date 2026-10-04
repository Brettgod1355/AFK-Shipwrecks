/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

import java.awt.image.BufferedImage;
import net.runelite.client.util.ImageUtil;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/** The sidebar button's icon is looked up next to the plugin class, so a moved or broken file would only fail at start-up. */
public class SidebarIconTest
{
	@Test
	public void sidebarIconLoadsFromThePluginPackage()
	{
		BufferedImage icon = ImageUtil.loadImageResource(AfkSalvagingPlugin.class, "spots_icon.png");
		assertNotNull(icon);
		assertEquals(16, icon.getWidth());
		assertEquals(16, icon.getHeight());
	}
}
