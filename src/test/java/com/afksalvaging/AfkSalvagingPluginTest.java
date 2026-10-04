/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

/**
 * Starts a development RuneLite client with this plugin loaded.
 * Run with {@code ./gradlew run} or from IntelliJ.
 */
public final class AfkSalvagingPluginTest
{
	@SuppressWarnings("unchecked") // RuneLite's loadBuiltin uses generic varargs.
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(AfkSalvagingPlugin.class);
		RuneLite.main(args);
	}
}
