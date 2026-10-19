package com.cooldownoverlays;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CooldownTest
{
	private static final double DELTA = 0.000001;
	private static final long START = 1_000_000_000L;

	@Test
	public void countdownInterpolatesWithinOneTick()
	{
		Cooldown cooldown = new Cooldown();
		cooldown.start(10, 20, START);

		assertEquals(6, cooldown.getRemainingSeconds(START), DELTA);
		assertEquals(5.7, cooldown.getRemainingSeconds(START + 300_000_000L), DELTA);
		assertEquals(0.475, cooldown.getRemainingFraction(START + 300_000_000L), DELTA);
	}

	@Test
	public void stalledClientCannotConsumeMoreThanOneTick()
	{
		Cooldown cooldown = new Cooldown();
		cooldown.start(10, 10, START);

		assertEquals(5.4, cooldown.getRemainingSeconds(START + 60_000_000_000L), DELTA);
		assertTrue(cooldown.isActive());
	}

	@Test
	public void finalTickStaysActiveUntilServerClearsIt()
	{
		Cooldown cooldown = new Cooldown();
		cooldown.start(1, 17, START);
		for (int tick = 1; tick <= 20; tick++)
		{
			cooldown.tick(START + tick * Cooldown.TICK_NANOS);
		}

		assertTrue(cooldown.isActive());
		assertTrue(cooldown.getRemainingSeconds(START + 100 * Cooldown.TICK_NANOS) > 0);
		cooldown.clear();
		assertFalse(cooldown.isActive());
		assertEquals(0, cooldown.getRemainingSeconds(START), DELTA);
		assertEquals(0, cooldown.getRemainingFraction(START), DELTA);
	}

	@Test
	public void unknownCountdownDoesNotInventTimeRemaining()
	{
		Cooldown cooldown = new Cooldown();
		cooldown.startUnknown(START);
		cooldown.tick(START + Cooldown.TICK_NANOS);

		assertTrue(cooldown.isActive());
		assertFalse(cooldown.isKnown());
		assertEquals(0, cooldown.getRemainingSeconds(START), DELTA);
		assertEquals(1, cooldown.getRemainingFraction(START), DELTA);
	}

	@Test
	public void newServerSampleReplacesInterpolatedTime()
	{
		Cooldown cooldown = new Cooldown();
		cooldown.start(10, 20, START);
		cooldown.start(8, 20, START + 3 * Cooldown.TICK_NANOS);

		assertEquals(4.8, cooldown.getRemainingSeconds(START + 3 * Cooldown.TICK_NANOS), DELTA);
		assertEquals(0.4, cooldown.getRemainingFraction(START + 3 * Cooldown.TICK_NANOS), DELTA);
	}

	@Test
	public void clockEarlierThanSampleDoesNotIncreaseCountdown()
	{
		Cooldown cooldown = new Cooldown();
		cooldown.start(10, 10, START);

		assertEquals(6, cooldown.getRemainingSeconds(START - 1), DELTA);
		assertEquals(1, cooldown.getRemainingFraction(START - 1), DELTA);
	}

	@Test
	public void readyStateRequiresAConfirmedSampleAndRepeatedSamplesPreserveItsAge()
	{
		Cooldown cooldown = new Cooldown();
		assertFalse(cooldown.isActive());
		assertFalse(cooldown.isReady());
		assertEquals(-1, cooldown.getFinishedAgeSeconds(START), DELTA);
		cooldown.setReady(START);
		assertFalse(cooldown.isActive());
		assertTrue(cooldown.isReady());
		cooldown.finish(START + 2_000_000_000L);
		assertEquals(2, cooldown.getFinishedAgeSeconds(START + 2_000_000_000L), DELTA);
	}

	@Test
	public void activeUnknownAndClearedStatesAllResetReadiness()
	{
		Cooldown cooldown = new Cooldown();
		cooldown.finish(START);
		cooldown.start(10, 10, START);
		assertFalse(cooldown.isReady());
		assertEquals(-1, cooldown.getFinishedAgeSeconds(START), DELTA);
		cooldown.finish(START);
		cooldown.startUnknown(START);
		assertFalse(cooldown.isReady());
		cooldown.finish(START);
		cooldown.clear();
		assertFalse(cooldown.isReady());
		assertEquals(-1, cooldown.getFinishedAgeSeconds(START), DELTA);
	}
}
