package com.cooldownoverlays;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BoostDecayClockTest
{
	private final BoostDecayClock clock = new BoostDecayClock();

	@Test
	public void phaseIsUnknownUntilANaturalDecayIsObserved()
	{
		assertEquals(-1, clock.ticksToNextDecay(50, false));
		assertEquals(-1, clock.ticksToNextDecay(75, true));
	}

	@Test
	public void ordinaryBoostsDecayAfterOneHundredTicks()
	{
		clock.observeDecay(20);
		assertEquals(100, clock.ticksToNextDecay(20, false));
		assertEquals(75, clock.ticksToNextDecay(45, false));
		assertEquals(1, clock.ticksToNextDecay(119, false));
		clock.observeDecay(120);
		assertEquals(100, clock.ticksToNextDecay(120, false));
	}

	@Test
	public void aMissedDecreaseInvalidatesThePhaseRatherThanExtendingIt()
	{
		clock.observeDecay(20);
		assertEquals(1, clock.ticksToNextDecay(119, false));
		assertEquals(-1, clock.ticksToNextDecay(120, false));
		assertEquals(-1, clock.ticksToNextDecay(121, false));
		assertEquals(-1, clock.ticksToNextDecay(146, true));
		clock.observeDecay(150);
		assertEquals(100, clock.ticksToNextDecay(150, false));
	}

	@Test
	public void skippingTheFirstCycleDoesNotFabricatePreserveExtensions()
	{
		clock.observeDecay(20);
		assertEquals(-1, clock.ticksToNextDecay(146, true));
	}

	@Test
	public void preserveActiveEarlyExtendsTheCycleToOneHundredFiftyTicks()
	{
		clock.observeDecay(20);
		assertEquals(150, clock.ticksToNextDecay(20, true));
		assertEquals(75, clock.ticksToNextDecay(95, true));
		assertEquals(50, clock.ticksToNextDecay(120, true));
		assertEquals(1, clock.ticksToNextDecay(169, true));
		assertEquals(-1, clock.ticksToNextDecay(170, true));
	}

	@Test
	public void preserveCanQualifyBeforeTheSeventyFifthTick()
	{
		clock.observeDecay(0);
		assertEquals(77, clock.ticksToNextDecay(23, false));
		assertEquals(76, clock.ticksToNextDecay(74, true));
		assertEquals(50, clock.ticksToNextDecay(100, true));
	}

	@Test
	public void preserveActivatedTooLateDoesNotExtendTheCurrentCycle()
	{
		clock.observeDecay(0);
		assertEquals(26, clock.ticksToNextDecay(74, false));
		assertEquals(25, clock.ticksToNextDecay(75, true));
		assertEquals(1, clock.ticksToNextDecay(99, true));
		assertEquals(-1, clock.ticksToNextDecay(100, true));
	}

	@Test
	public void turningPreserveOffBeforeTheFirstExtensionRestoresTheNormalDeadline()
	{
		clock.observeDecay(0);
		assertEquals(150, clock.ticksToNextDecay(0, true));
		assertEquals(20, clock.ticksToNextDecay(80, false));
		assertEquals(19, clock.ticksToNextDecay(81, true));
		assertEquals(-1, clock.ticksToNextDecay(100, true));
	}

	@Test
	public void turningPreserveOffAfterOneHundredTicksKeepsTheFirstExtension()
	{
		clock.observeDecay(0);
		assertEquals(150, clock.ticksToNextDecay(0, true));
		assertEquals(50, clock.ticksToNextDecay(100, true));
		assertEquals(24, clock.ticksToNextDecay(101, false));
		assertEquals(15, clock.ticksToNextDecay(110, true));
		assertEquals(1, clock.ticksToNextDecay(124, false));
		assertEquals(-1, clock.ticksToNextDecay(125, false));
	}

	@Test
	public void turningPreserveOffAfterTheSecondExtensionKeepsItsDeadline()
	{
		clock.observeDecay(0);
		assertEquals(150, clock.ticksToNextDecay(0, true));
		assertEquals(25, clock.ticksToNextDecay(125, true));
		assertEquals(24, clock.ticksToNextDecay(126, false));
		assertEquals(1, clock.ticksToNextDecay(149, false));
		assertEquals(-1, clock.ticksToNextDecay(150, false));
	}

	@Test
	public void duplicateDecayEventsDoNotResetPreserveQualification()
	{
		clock.observeDecay(0);
		assertEquals(150, clock.ticksToNextDecay(0, true));
		clock.observeDecay(0);
		assertEquals(75, clock.ticksToNextDecay(75, true));
	}

	@Test
	public void aNewDecayStartsNewPreserveQualification()
	{
		clock.observeDecay(0);
		assertEquals(150, clock.ticksToNextDecay(0, true));
		clock.observeDecay(150);
		assertEquals(100, clock.ticksToNextDecay(150, false));
		assertEquals(25, clock.ticksToNextDecay(225, true));
	}

	@Test
	public void clearingForLogoutOrHopForgetsThePhase()
	{
		clock.observeDecay(20);
		assertEquals(150, clock.ticksToNextDecay(20, true));
		clock.clear();
		assertEquals(-1, clock.ticksToNextDecay(21, true));
		clock.observeDecay(1);
		assertEquals(100, clock.ticksToNextDecay(1, false));
	}

	@Test
	public void backwardsTicksInvalidateThePhase()
	{
		clock.observeDecay(20);
		assertEquals(-1, clock.ticksToNextDecay(19, false));
		assertEquals(-1, clock.ticksToNextDecay(20, true));
	}

	@Test
	public void expectedDecayCanBeMatchedBeforeTheDeadlineExpires()
	{
		assertFalse(clock.isExpectedDecay(100, false));
		clock.observeDecay(20);
		assertFalse(clock.isExpectedDecay(119, false));
		assertTrue(clock.isExpectedDecay(120, false));
		assertFalse(clock.isExpectedDecay(121, false));
		assertEquals(-1, clock.ticksToNextDecay(120, false));
		assertFalse(clock.isExpectedDecay(120, false));
	}

	@Test
	public void preserveHistoryDeterminesWhichDeadlineMatches()
	{
		clock.observeDecay(0);
		assertEquals(150, clock.ticksToNextDecay(0, true));
		assertFalse(clock.isExpectedDecay(100, true));
		assertTrue(clock.isExpectedDecay(100, false));
		assertEquals(50, clock.ticksToNextDecay(100, true));
		assertFalse(clock.isExpectedDecay(124, false));
		assertTrue(clock.isExpectedDecay(125, false));
		assertTrue(clock.isExpectedDecay(150, true));
	}

	@Test
	public void matchingDoesNotInventPreserveQualificationOrExtendAnExpiredCycle()
	{
		clock.observeDecay(0);
		assertFalse(clock.isExpectedDecay(74, true));
		// A query alone cannot qualify Preserve; it must be observed on a game tick.
		assertTrue(clock.isExpectedDecay(100, true));
		assertFalse(clock.isExpectedDecay(125, true));
		assertFalse(clock.isExpectedDecay(150, true));
		assertFalse(clock.isExpectedDecay(-1, false));
	}
}
