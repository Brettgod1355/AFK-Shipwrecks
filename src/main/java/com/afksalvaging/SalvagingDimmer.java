package com.afksalvaging;

/** Dimming decisions using the same activity, crew and hold state as the AFK timer. */
final class SalvagingDimmer
{
	private SalvagingDimmer()
	{
	}

	static boolean shouldDim(AfkSalvagingConfig config, AfkSession.View view, CargoHoldMonitor hold)
	{
		if (view.danger != null || view.hazardous)
		{
			return false;
		}
		if (view.activity == PlayerActivity.SALVAGING)
		{
			return config.dimWhenSalvaging();
		}
		if (view.activity == PlayerActivity.SORTING)
		{
			return config.dimWhenSorting() && !(config.undimSoonerWhenSorting()
				&& view.sortingLeftMillis >= 0
				&& view.sortingLeftMillis <= Math.max(0, config.sortingUndimSeconds()) * 1000L);
		}
		if (view.activity != PlayerActivity.IDLE && view.activity != PlayerActivity.AT_HOOK_IDLE)
		{
			return false;
		}
		if (view.activity == PlayerActivity.IDLE && config.dimWhenCrewSalvaging() && view.crewSalvaging
			&& !atCargoThreshold(hold, config.cargoHoldUndimPercent()))
		{
			return true;
		}
		// A known site must be in reach. A visible wreck above our level still counts as a respawn.
		return config.dimWhenNoWrecks() && view.atSpot && view.wrecksUp == 0 && view.higherWrecksUp == 0;
	}

	private static boolean atCargoThreshold(CargoHoldMonitor hold, int percent)
	{
		return hold.isConfirmedFull() || (hold.hasCount()
			&& (long) hold.getUsed() * 100 >= (long) hold.getCapacity() * Math.max(1, Math.min(100, percent)));
	}
}
