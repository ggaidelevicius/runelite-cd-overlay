package com.cooldownoverlays;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CooldownTrackerTest
{
	private static final double DELTA = 0.000001;
	private static final long START = 1_000_000_000L;
	private final CooldownTracker tracker = new CooldownTracker();

	@Test
	public void readyServerStateHasNoCooldown()
	{
		tracker.tick(0, START);

		assertFalse(tracker.getHeart().isActive());
		assertTrue(tracker.getHeart().isReady());
		assertEquals(0, tracker.getHeart().getFinishedAgeSeconds(START), DELTA);
		tracker.tick(0, atTick(1));
		assertEquals(0.6, tracker.getHeart().getFinishedAgeSeconds(atTick(1)), DELTA);
	}

	@Test
	public void observedImbuedHeartUseStartsSevenMinuteSharedCooldown()
	{
		tracker.initialize(0, START);
		tracker.tick(70, atTick(1));

		assertTrue(tracker.getHeart().isKnown());
		assertEquals(420, tracker.getHeart().getRemainingSeconds(atTick(1)), DELTA);
		assertEquals(700, tracker.getHeart().getTotalTicks());
		assertEquals(1, tracker.getHeart().getRemainingFraction(atTick(1)), DELTA);
	}

	@Test
	public void observedSaturatedHeartUseStartsFiveMinuteSharedCooldown()
	{
		tracker.initialize(0, START);
		tracker.tick(50, atTick(1));

		assertEquals(300, tracker.getHeart().getRemainingSeconds(atTick(1)), DELTA);
		assertEquals(500, tracker.getHeart().getTotalTicks());
		assertEquals(1, tracker.getHeart().getRemainingFraction(atTick(1)), DELTA);
	}

	@Test
	public void unchangedHeartBucketCountsDownBetweenServerUpdates()
	{
		tracker.initialize(0, START);
		tracker.tick(50, atTick(1));
		for (int tick = 2; tick <= 10; tick++)
		{
			tracker.tick(50, atTick(tick));
		}

		assertEquals(294.6, tracker.getHeart().getRemainingSeconds(atTick(10)), DELTA);
		tracker.tick(49, atTick(11));
		assertEquals(294, tracker.getHeart().getRemainingSeconds(atTick(11)), DELTA);
	}

	@Test
	public void heartServerSampleCorrectsAnUnknownBucketPhase()
	{
		tracker.initialize(35, START);
		assertEquals(210, tracker.getHeart().getRemainingSeconds(START), DELTA);

		tracker.tick(34, atTick(1));
		assertEquals(204, tracker.getHeart().getRemainingSeconds(atTick(1)), DELTA);
	}

	@Test
	public void imbuedDurationIsPreservedWhenRemainingTimeDropsBelowFiveMinutes()
	{
		tracker.initialize(0, START);
		tracker.tick(70, atTick(1));
		tracker.tick(49, atTick(2));

		assertEquals(700, tracker.getHeart().getTotalTicks());
		assertEquals(0.7, tracker.getHeart().getRemainingFraction(atTick(2)), DELTA);
	}

	@Test
	public void increasingHeartSampleStartsNewSharedDuration()
	{
		tracker.initialize(20, START);
		tracker.tick(50, atTick(1));

		assertEquals(500, tracker.getHeart().getTotalTicks());
		assertEquals(300, tracker.getHeart().getRemainingSeconds(atTick(1)), DELTA);
	}

	@Test
	public void delayedHeartClearDoesNotDeclareReadyEarly()
	{
		tracker.initialize(1, START);
		for (int tick = 1; tick <= 25; tick++)
		{
			tracker.tick(1, atTick(tick));
		}

		assertTrue(tracker.getHeart().isActive());
		assertTrue(tracker.getHeart().getRemainingSeconds(atTick(26)) > 0);
		tracker.tick(0, atTick(26));
		assertFalse(tracker.getHeart().isActive());
	}

	@Test
	public void pausedHeartCannotCountDownBeyondItsAuthoritativeBucket()
	{
		tracker.initialize(30, START);
		for (int tick = 1; tick <= 100; tick++)
		{
			tracker.tick(30, atTick(tick));
		}

		assertEquals(174.6, tracker.getHeart().getRemainingSeconds(atTick(100)), DELTA);
		assertEquals(174, tracker.getHeart().getRemainingSeconds(atTick(101)), DELTA);
		assertEquals(-1, tracker.getHeart().getFinishedAgeSeconds(atTick(100)), DELTA);
	}

	@Test
	public void serverClearTriggersCompletionOnce()
	{
		tracker.initialize(1, START);
		tracker.tick(0, atTick(1));

		assertFalse(tracker.getHeart().isActive());
		assertEquals(0, tracker.getHeart().getFinishedAgeSeconds(atTick(1)), DELTA);
		tracker.tick(0, atTick(2));
		assertEquals(0.6, tracker.getHeart().getFinishedAgeSeconds(atTick(2)), DELTA);
	}

	@Test
	public void newHeartUseCancelsCompletionAnimation()
	{
		tracker.initialize(1, START);
		tracker.tick(0, atTick(1));
		tracker.tick(50, atTick(2));

		assertTrue(tracker.getHeart().isActive());
		assertEquals(-1, tracker.getHeart().getFinishedAgeSeconds(atTick(2)), DELTA);
	}

	@Test
	public void lifecycleResetWaitsForAConfirmedReadySample()
	{
		tracker.initialize(50, START);
		tracker.clear();

		assertFalse(tracker.getHeart().isActive());
		assertFalse(tracker.getHeart().isReady());
		assertEquals(-1, tracker.getHeart().getFinishedAgeSeconds(atTick(1)), DELTA);
		tracker.initialize(0, atTick(2));
		assertTrue(tracker.getHeart().isReady());
		assertEquals(0, tracker.getHeart().getFinishedAgeSeconds(atTick(2)), DELTA);
	}

	@Test
	public void reconnectDiscardsOldTimingAndRehydratesServerState()
	{
		tracker.initialize(0, START);
		tracker.tick(70, atTick(1));
		tracker.clear();
		assertFalse(tracker.getHeart().isActive());

		tracker.tick(10, atTick(100));
		assertEquals(60, tracker.getHeart().getRemainingSeconds(atTick(100)), DELTA);
		assertEquals(700, tracker.getHeart().getTotalTicks());
	}

	@Test
	public void negativeServerSamplesAreTreatedAsReady()
	{
		tracker.initialize(-1, START);
		assertFalse(tracker.getHeart().isActive());
		assertTrue(tracker.getHeart().isReady());
		assertEquals(0, tracker.getHeart().getFinishedAgeSeconds(START), DELTA);
		tracker.tick(50, atTick(1));
		tracker.tick(-1, atTick(2));

		assertFalse(tracker.getHeart().isActive());
		assertEquals(0, tracker.getHeart().getFinishedAgeSeconds(atTick(2)), DELTA);
	}

	private static long atTick(int tick)
	{
		return START + tick * Cooldown.TICK_NANOS;
	}
}
