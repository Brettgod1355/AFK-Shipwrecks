/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

/** Which double salvage spots to box on the water. The names are the labels in the settings panel. */
public enum DoubleSpotMode
{
	/** Draw nothing. */
	OFF,
	/** Only where two wrecks that are up right now can both be reached. */
	ACTIVE_WRECKS,
	/** Every pair of wreck sites in view, sunk ones included, so you can park before they rise. */
	ALL_SITES
}
