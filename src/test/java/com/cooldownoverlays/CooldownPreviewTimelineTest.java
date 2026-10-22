package com.cooldownoverlays;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CooldownPreviewTimelineTest
{
	private static final long SECOND = 1_000_000_000L;

	@Test
	public void demonstrationStartsFullAndCountsDownSmoothlyWithoutGameTicks()
	{
		CooldownPreviewTimeline timeline = new CooldownPreviewTimeline(SECOND);
		assertTrue(timeline.isActive());
		assertTrue(timeline.isKnown());
		assertFalse(timeline.isReady());
		assertEquals(10, timeline.getRemainingSeconds(SECOND), 0);
		assertEquals(1, timeline.getRemainingFraction(SECOND), 0);
		timeline.update(SECOND + 125_000_000L);
		assertEquals(9.875, timeline.getRemainingSeconds(SECOND + 125_000_000L), 0);
		assertEquals(0.9875, timeline.getRemainingFraction(SECOND + 125_000_000L), 0);
		assertEquals(-1, timeline.getFinishedAgeSeconds(SECOND), 0);
	}

	@Test
	public void readinessBeginsAtExactlyTenSeconds()
	{
		CooldownPreviewTimeline timeline = new CooldownPreviewTimeline(0);
		timeline.update(10 * SECOND - 1);
		assertTrue(timeline.isActive());
		assertTrue(timeline.getRemainingSeconds(10 * SECOND - 1) > 0);
		timeline.update(10 * SECOND);
		assertFalse(timeline.isActive());
		assertTrue(timeline.isReady());
		assertEquals(0, timeline.getRemainingFraction(10 * SECOND), 0);
		assertEquals(0, timeline.getFinishedAgeSeconds(10 * SECOND), 0);
	}

	@Test
	public void delayedFramesRetainTheOriginalReadyDeadline()
	{
		CooldownPreviewTimeline timeline = new CooldownPreviewTimeline(-5 * SECOND);
		timeline.update(15 * SECOND);
		assertTrue(timeline.isReady());
		assertEquals(10, timeline.getFinishedAgeSeconds(15 * SECOND), 0);
	}

	@Test
	public void restartResetsBothCooldownAndReadyAge()
	{
		CooldownPreviewTimeline timeline = new CooldownPreviewTimeline(0);
		timeline.update(15 * SECOND);
		timeline.restart(20 * SECOND);
		assertTrue(timeline.isActive());
		assertFalse(timeline.isReady());
		assertEquals(10, timeline.getRemainingSeconds(20 * SECOND), 0);
		timeline.update(30 * SECOND);
		assertEquals(0, timeline.getFinishedAgeSeconds(30 * SECOND), 0);
	}

	@Test
	public void styleReplayResetsReadyAgeWithoutStartingAnotherCooldown()
	{
		CooldownPreviewTimeline timeline = new CooldownPreviewTimeline(0);
		timeline.restartReady(3 * SECOND);
		assertTrue(timeline.isActive());
		assertEquals(7, timeline.getRemainingSeconds(3 * SECOND), 0);
		timeline.restartReady(13 * SECOND);
		assertTrue(timeline.isReady());
		assertEquals(0, timeline.getFinishedAgeSeconds(13 * SECOND), 0);
		assertEquals(0.125, timeline.getFinishedAgeSeconds(13 * SECOND + 125_000_000L), 0);
	}
}
