/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

/** When the information lines of the overlay are shown. Alerts and banners show regardless. */
public enum OverlayWhen
{
	/** Only while a wreck site is in view, which is what being at a salvaging spot looks like to the client. */
	NEAR_WRECKS,
	/** Whenever the player is aboard their own boat. */
	ALWAYS_ABOARD
}
