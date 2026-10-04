/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import org.junit.Test;

public class CrewSwapWatchTest
{
	private static final Crewmate ADA = new Crewmate(3, "Adventurer Ada", 1);
	private static final Crewmate JENKINS = new Crewmate(7, "Cabin Boy Jenkins", 4);
	private static final Crewmate JOLLY = new Crewmate(9, "Jolly Jim", 4);
	private static final CrewSwapWatch.Suggestion JENKINS_FOR_ADA = new CrewSwapWatch.Suggestion(ADA, JENKINS, 1, 4);
	private static final CrewSwapWatch.Suggestion JOLLY_FOR_ADA = new CrewSwapWatch.Suggestion(ADA, JOLLY, 1, 4);

	@Test
	public void aPairIsAnnouncedOnceAfterTheGraceAndThenStands()
	{
		CrewSwapWatch watch = new CrewSwapWatch();
		assertNull(watch.update(0, JENKINS_FOR_ADA));
		assertNull(watch.standing());
		assertNull(watch.update(CrewSwapWatch.GRACE_MILLIS - 1, JENKINS_FOR_ADA));
		assertSame(JENKINS_FOR_ADA, watch.update(CrewSwapWatch.GRACE_MILLIS, JENKINS_FOR_ADA));
		assertNotNull(watch.standing());
		assertNull(watch.update(CrewSwapWatch.GRACE_MILLIS + 60_000, JENKINS_FOR_ADA));
		assertNotNull(watch.standing());
	}

	@Test
	public void steppingOffTheHookAndBackDoesNotRepeatIt()
	{
		CrewSwapWatch watch = new CrewSwapWatch();
		watch.update(0, JENKINS_FOR_ADA);
		assertSame(JENKINS_FOR_ADA, watch.update(CrewSwapWatch.GRACE_MILLIS, JENKINS_FOR_ADA));
		// The player steps off their hook to sort: no swap to suggest while a hook is empty.
		assertNull(watch.update(20_000, null));
		assertNull(watch.standing());
		// Back on the hook, the same pair stands again: shown, not announced again.
		assertNull(watch.update(30_000, JENKINS_FOR_ADA));
		assertNull(watch.update(30_000 + CrewSwapWatch.GRACE_MILLIS, JENKINS_FOR_ADA));
		assertNotNull(watch.standing());
	}

	@Test
	public void aDifferentPairIsNewsAndTheGraceStartsAgain()
	{
		CrewSwapWatch watch = new CrewSwapWatch();
		watch.update(0, JENKINS_FOR_ADA);
		assertSame(JENKINS_FOR_ADA, watch.update(CrewSwapWatch.GRACE_MILLIS, JENKINS_FOR_ADA));
		assertNull(watch.update(20_000, JOLLY_FOR_ADA));
		assertNull(watch.standing());
		assertSame(JOLLY_FOR_ADA, watch.update(20_000 + CrewSwapWatch.GRACE_MILLIS, JOLLY_FOR_ADA));
	}

	@Test
	public void aWorldHopDoesNotRepeatWhatWasSaid()
	{
		CrewSwapWatch watch = new CrewSwapWatch();
		watch.update(0, JENKINS_FOR_ADA);
		assertNotNull(watch.update(CrewSwapWatch.GRACE_MILLIS, JENKINS_FOR_ADA));
		watch.forgetPending();
		assertNull(watch.update(CrewSwapWatch.GRACE_MILLIS + 1_000, JENKINS_FOR_ADA));
		assertNull("same crew after the hop: not said again", watch.update(3 * CrewSwapWatch.GRACE_MILLIS, JENKINS_FOR_ADA));
		assertNotNull("but it still stands", watch.standing());
	}

	@Test
	public void aResetForgetsWhatWasSaid()
	{
		CrewSwapWatch watch = new CrewSwapWatch();
		watch.update(0, JENKINS_FOR_ADA);
		watch.update(CrewSwapWatch.GRACE_MILLIS, JENKINS_FOR_ADA);
		watch.reset();
		assertNull(watch.standing());
		watch.update(50_000, JENKINS_FOR_ADA);
		assertSame(JENKINS_FOR_ADA, watch.update(50_000 + CrewSwapWatch.GRACE_MILLIS, JENKINS_FOR_ADA));
	}
}
