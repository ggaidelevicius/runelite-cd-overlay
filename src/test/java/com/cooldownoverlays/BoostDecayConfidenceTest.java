package com.cooldownoverlays;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import net.runelite.api.Skill;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BoostDecayConfidenceTest
{
	private static final double DELTA = 0.000001;
	private final Scenario scenario = new Scenario();

	@Test
	public void isolatedEnemyDrainDoesNotExposeTheWrongInitialPhase()
	{
		scenario.strengthAt(20, 117);
		scenario.assertUnknown();
		scenario.holdUnknownUntil(99);

		scenario.strengthAt(100, 116);
		scenario.assertUnknown();
		scenario.holdUnknownUntil(199);

		scenario.strengthAt(200, 115);
		// The natural phase is 100/200/300, not 20/120/220. No numerical
		// estimate, including the former incorrect 372 seconds, was exposed.
		scenario.assertRemaining(360);
	}

	@Test
	public void twoSeparateOneMinuteDecaysConfirmThePhase()
	{
		scenario.strengthAt(100, 117);
		scenario.assertUnknown();
		scenario.holdUnknownUntil(199);

		scenario.strengthAt(200, 116);
		scenario.assertRemaining(420);
	}

	@Test
	public void offCadenceEnemyDrainCannotMoveAConfirmedPhase()
	{
		scenario.strengthAt(100, 117);
		scenario.strengthAt(200, 116);
		scenario.assertRemaining(420);

		scenario.strengthAt(250, 115);
		// Five further full cycles follow the next decay at 300.
		scenario.assertRemaining(330);
		scenario.strengthAt(300, 114);
		scenario.assertRemaining(300);
	}

	@Test
	public void aMissedExpectedDecayRequiresTwoFreshObservations()
	{
		scenario.strengthAt(100, 117);
		scenario.strengthAt(200, 116);
		scenario.assertRemaining(420);
		scenario.holdUntil(300);
		scenario.assertUnknown();

		scenario.strengthAt(310, 115);
		scenario.assertUnknown();
		scenario.holdUnknownUntil(409);
		scenario.strengthAt(410, 114);
		scenario.assertRemaining(300);
	}

	@Test
	public void simultaneousSkillDecreasesAreOnlyOneObservation()
	{
		scenario.boosted.put(Skill.ATTACK, 118);
		scenario.boosted.put(Skill.DEFENCE, 118);
		scenario.holdUntil(99);
		scenario.boosted.put(Skill.ATTACK, 117);
		scenario.boosted.put(Skill.DEFENCE, 117);
		scenario.boosted.put(Skill.STRENGTH, 117);
		scenario.holdUntil(100);
		scenario.assertUnknown();

		scenario.strengthAt(200, 116);
		scenario.assertRemaining(420);
	}

	@Test
	public void protectedDecreasesCannotSeedThePhase()
	{
		scenario.protectedSkills.add(Skill.STRENGTH);
		scenario.strengthAt(100, 117);
		scenario.protectedSkills.clear();
		scenario.holdUntil(101);

		scenario.strengthAt(200, 116);
		scenario.assertUnknown();
		scenario.strengthAt(300, 115);
		scenario.assertRemaining(360);
	}

	@Test
	public void leavingProtectionOnTheDecreaseCannotSeedThePhase()
	{
		scenario.protectedSkills.add(Skill.STRENGTH);
		scenario.holdUntil(99);
		scenario.protectedSkills.clear();
		scenario.boosted.put(Skill.STRENGTH, 117);
		scenario.holdUntil(100);
		scenario.assertUnknown();

		scenario.strengthAt(200, 116);
		scenario.assertUnknown();
		scenario.strengthAt(300, 115);
		scenario.assertRemaining(360);
	}

	@Test
	public void baseLevelChangesCannotSeedThePhase()
	{
		scenario.holdUntil(99);
		scenario.base.put(Skill.STRENGTH, 100);
		scenario.boosted.put(Skill.STRENGTH, 117);
		scenario.holdUntil(100);
		scenario.assertUnknown();
		scenario.base.put(Skill.STRENGTH, 99);
		scenario.holdUntil(101);

		scenario.strengthAt(200, 116);
		scenario.assertUnknown();
		scenario.strengthAt(300, 115);
		scenario.assertRemaining(360);
	}

	@Test
	public void bulkDrainsCannotSeedThePhase()
	{
		scenario.strengthAt(100, 116);
		scenario.assertUnknown();
		scenario.strengthAt(200, 115);
		scenario.assertUnknown();
		scenario.strengthAt(300, 114);
		scenario.assertRemaining(300);
	}

	@Test
	public void aMissingSampleCannotTurnItsReturnIntoADecayObservation()
	{
		scenario.holdUntil(99);
		scenario.boosted.remove(Skill.STRENGTH);
		scenario.holdUntil(100);
		assertFalse(scenario.cooldown().isActive());
		scenario.boosted.put(Skill.STRENGTH, 117);
		scenario.holdUntil(101);
		scenario.assertUnknown();

		scenario.strengthAt(200, 116);
		scenario.assertUnknown();
		scenario.strengthAt(300, 115);
		scenario.assertRemaining(360);
	}

	@Test
	public void crossingTheActualThresholdIsReadyEvenWithoutALearnedPhase()
	{
		scenario.boosted.put(Skill.STRENGTH, 110);
		scenario.holdUntil(19);
		scenario.assertUnknown();
		scenario.strengthAt(20, 109);

		assertTrue(scenario.cooldown().isReady());
		assertFalse(scenario.cooldown().isActive());
	}

	@Test
	public void aRedoseBetweenObservationsKeepsTheExistingCandidatePhase()
	{
		scenario.strengthAt(100, 117);
		scenario.assertUnknown();
		scenario.strengthAt(150, 118);
		scenario.assertUnknown();
		scenario.strengthAt(200, 117);

		scenario.assertRemaining(480);
	}

	@Test
	public void continuousPreserveConfirmsAtTheNinetySecondCadence()
	{
		scenario.preserve = true;
		scenario.strengthAt(100, 117);
		scenario.assertUnknown();
		scenario.holdUnknownUntil(249);
		scenario.strengthAt(250, 116);

		scenario.assertRemaining(630);
	}

	@Test
	public void turningPreserveOffRetainsItsEarnedFirstExtension()
	{
		scenario.preserve = true;
		scenario.strengthAt(100, 117);
		scenario.holdUnknownUntil(200);
		scenario.preserve = false;
		scenario.holdUnknownUntil(224);
		scenario.strengthAt(225, 116);

		// This observation is 125 ticks after the candidate. Preserve is now
		// off, so future complete cycles are ordinary 100-tick intervals.
		scenario.assertRemaining(420);
	}

	@Test
	public void hitpointsDecreasesCannotTeachCombatBoostTiming()
	{
		scenario.base.put(Skill.HITPOINTS, 99);
		scenario.boosted.put(Skill.HITPOINTS, 118);
		scenario.holdUntil(99);
		scenario.boosted.put(Skill.HITPOINTS, 117);
		scenario.holdUnknownUntil(199);
		scenario.boosted.put(Skill.HITPOINTS, 116);
		scenario.holdUnknownUntil(200);
	}

	private static final class Scenario
	{
		private final BoostPotionTimers timers = new BoostPotionTimers();
		private final Map<Skill, Integer> base = new EnumMap<>(Skill.class);
		private final Map<Skill, Integer> boosted = new EnumMap<>(Skill.class);
		private final Set<Skill> protectedSkills = EnumSet.noneOf(Skill.class);
		private final Map<Skill, Integer> thresholds = Map.of(Skill.STRENGTH, 110);
		private boolean preserve;
		private int tick = -1;

		private Scenario()
		{
			for (Skill skill : new Skill[]{Skill.ATTACK, Skill.STRENGTH, Skill.DEFENCE, Skill.RANGED, Skill.MAGIC})
			{
				base.put(skill, 99);
				boosted.put(skill, 99);
			}
			boosted.put(Skill.STRENGTH, 118);
		}

		private void strengthAt(int atTick, int strength)
		{
			holdUntil(atTick - 1);
			boosted.put(Skill.STRENGTH, strength);
			holdUntil(atTick);
		}

		private void holdUntil(int untilTick)
		{
			while (tick < untilTick)
			{
				tick++;
				timers.tick(tick, base, boosted, protectedSkills, preserve, thresholds, now());
			}
		}

		private void holdUnknownUntil(int untilTick)
		{
			while (tick < untilTick)
			{
				holdUntil(tick + 1);
				assertUnknown();
			}
		}

		private Cooldown cooldown()
		{
			return timers.get(BoostPotion.SUPER_STRENGTH);
		}

		private void assertUnknown()
		{
			assertTrue("active at tick " + tick, cooldown().isActive());
			assertFalse("unknown phase at tick " + tick, cooldown().isKnown());
			assertFalse("not ready at tick " + tick, cooldown().isReady());
		}

		private void assertRemaining(double seconds)
		{
			assertTrue("known phase at tick " + tick, cooldown().isKnown());
			assertFalse("not ready at tick " + tick, cooldown().isReady());
			assertEquals(seconds, cooldown().getRemainingSeconds(now()), DELTA);
		}

		private long now()
		{
			return 1_000_000_000L + tick * Cooldown.TICK_NANOS;
		}
	}
}
