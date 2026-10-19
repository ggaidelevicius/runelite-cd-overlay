package com.cooldownoverlays;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import net.runelite.api.Skill;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BoostPotionTimersTest
{
	private static final double DELTA = 0.000001;
	private static final long START = 1_000_000_000L;
	private final BoostPotionTimers timers = new BoostPotionTimers();
	private final Map<Skill, Integer> base = levels(99, 99, 99, 99, 99);

	@Test
	public void unboostedSkillIsReadyOnInitialSample()
	{
		tick(0, levels(99, 99, 99, 99, 99), Set.of(), false, 103);

		assertTrue(timers.get(BoostPotion.SUPER_ATTACK).isReady());
		assertFalse(timers.get(BoostPotion.SUPER_ATTACK).isActive());
		assertEquals(0, timers.get(BoostPotion.SUPER_ATTACK).getFinishedAgeSeconds(atTick(0)), DELTA);
	}

	@Test
	public void boostedInitialSampleWaitsForObservedDecayPhase()
	{
		tick(0, levels(118, 99, 99, 99, 99), Set.of(), false, 103);

		assertTrue(timers.get(BoostPotion.SUPER_ATTACK).isActive());
		assertFalse(timers.get(BoostPotion.SUPER_ATTACK).isKnown());
		assertFalse(timers.get(BoostPotion.SUPER_ATTACK).isReady());
	}

	@Test
	public void observedSingleLevelDecayPredictsTimeToThreshold()
	{
		tick(99, levels(119, 99, 99, 99, 99), Set.of(), false, 103);
		tick(100, levels(118, 99, 99, 99, 99), Set.of(), false, 103);

		Cooldown cooldown = timers.get(BoostPotion.SUPER_ATTACK);
		assertTrue(cooldown.isKnown());
		assertFalse(cooldown.isReady());
		// Level 118 drops below 103 after sixteen further one-minute decays.
		assertEquals(960, cooldown.getRemainingSeconds(atTick(100)), DELTA);
	}

	@Test
	public void drinkingAgainRetainsTheSharedDecayPhase()
	{
		tick(99, levels(109, 99, 99, 99, 99), Set.of(), false, 103);
		tick(100, levels(108, 99, 99, 99, 99), Set.of(), false, 103);
		tick(110, levels(118, 99, 99, 99, 99), Set.of(), false, 103);

		assertTrue(timers.get(BoostPotion.SUPER_ATTACK).isKnown());
		// Next decay is tick 200, rather than one minute after drinking at tick 110.
		assertEquals(954, timers.get(BoostPotion.SUPER_ATTACK).getRemainingSeconds(atTick(110)), DELTA);
	}

	@Test
	public void preserveUsesNinetySecondDecayIntervals()
	{
		tick(99, levels(119, 99, 99, 99, 99), Set.of(), true, 103);
		tick(100, levels(118, 99, 99, 99, 99), Set.of(), true, 103);

		assertTrue(timers.get(BoostPotion.SUPER_ATTACK).isKnown());
		assertEquals(1440, timers.get(BoostPotion.SUPER_ATTACK).getRemainingSeconds(atTick(100)), DELTA);
	}

	@Test
	public void thresholdChangesRecalculateTimerAndReadiness()
	{
		tick(99, levels(109, 99, 99, 99, 99), Set.of(), false, 103);
		tick(100, levels(108, 99, 99, 99, 99), Set.of(), false, 103);
		assertEquals(360, timers.get(BoostPotion.SUPER_ATTACK).getRemainingSeconds(atTick(100)), DELTA);

		tick(101, levels(108, 99, 99, 99, 99), Set.of(), false, 108);
		assertEquals(59.4, timers.get(BoostPotion.SUPER_ATTACK).getRemainingSeconds(atTick(101)), DELTA);
		assertFalse(timers.get(BoostPotion.SUPER_ATTACK).isReady());

		tick(102, levels(108, 99, 99, 99, 99), Set.of(), false, 109);
		assertTrue(timers.get(BoostPotion.SUPER_ATTACK).isReady());
		assertFalse(timers.get(BoostPotion.SUPER_ATTACK).isActive());
	}

	@Test
	public void combinedPotionUsesFirstBeneficialSkillToReachThreshold()
	{
		tick(99, levels(104, 118, 118, 99, 99), Set.of(), false, 103);
		tick(100, levels(103, 118, 118, 99, 99), Set.of(), false, 103);

		assertEquals(60, timers.get(BoostPotion.SUPER_COMBAT).getRemainingSeconds(atTick(100)), DELTA);
		assertFalse(timers.get(BoostPotion.SUPER_COMBAT).isReady());
		tick(199, levels(103, 118, 118, 99, 99), Set.of(), false, 103);
		assertFalse(timers.get(BoostPotion.SUPER_COMBAT).isReady());

		tick(200, levels(102, 117, 117, 99, 99), Set.of(), false, 103);
		assertTrue(timers.get(BoostPotion.SUPER_COMBAT).isReady());
	}

	@Test
	public void protectedStatsCannotTriggerReadyAnimation()
	{
		tick(0, levels(99, 118, 118, 99, 99), Set.of(Skill.ATTACK), false, 103);

		assertFalse(timers.get(BoostPotion.SUPER_ATTACK).isReady());
		assertFalse(timers.get(BoostPotion.SUPER_ATTACK).isActive());
		assertFalse(timers.get(BoostPotion.SUPER_COMBAT).isReady());
		assertTrue(timers.get(BoostPotion.SUPER_COMBAT).isActive());

		tick(1, levels(99, 99, 99, 99, 99), Set.of(Skill.ATTACK, Skill.STRENGTH, Skill.DEFENCE), false, 103);
		assertFalse(timers.get(BoostPotion.SUPER_COMBAT).isReady());
		assertFalse(timers.get(BoostPotion.SUPER_COMBAT).isActive());
	}

	@Test
	public void protectedSingleLevelDropDoesNotTeachNaturalDecayPhase()
	{
		tick(99, levels(119, 118, 99, 99, 99), Set.of(Skill.ATTACK), false, 103);
		tick(100, levels(118, 118, 99, 99, 99), Set.of(Skill.ATTACK), false, 103);

		assertFalse(timers.get(BoostPotion.SUPER_STRENGTH).isKnown());
		assertTrue(timers.get(BoostPotion.SUPER_STRENGTH).isActive());
	}

	@Test
	public void bulkDrainAndDoseIncreasesDoNotTeachDecayPhase()
	{
		tick(99, levels(118, 99, 99, 99, 99), Set.of(), false, 103);
		tick(100, levels(110, 99, 99, 99, 99), Set.of(), false, 103);
		assertFalse(timers.get(BoostPotion.SUPER_ATTACK).isKnown());

		tick(101, levels(118, 99, 99, 99, 99), Set.of(), false, 103);
		assertFalse(timers.get(BoostPotion.SUPER_ATTACK).isKnown());
		assertFalse(timers.get(BoostPotion.SUPER_ATTACK).isReady());
	}

	@Test
	public void realLevelChangeDoesNotTeachDecayPhase()
	{
		base.put(Skill.ATTACK, 98);
		tick(99, levels(118, 99, 99, 99, 99), Set.of(), false, 103);
		base.put(Skill.ATTACK, 99);
		tick(100, levels(117, 99, 99, 99, 99), Set.of(), false, 103);

		assertFalse(timers.get(BoostPotion.SUPER_ATTACK).isKnown());
		assertFalse(timers.get(BoostPotion.SUPER_ATTACK).isReady());
	}

	@Test
	public void countdownCannotDeclareReadyBeforeObservedThresholdCrossing()
	{
		tick(99, levels(104, 99, 99, 99, 99), Set.of(), false, 103);
		tick(100, levels(103, 99, 99, 99, 99), Set.of(), false, 103);
		tick(201, levels(103, 99, 99, 99, 99), Set.of(), false, 103);

		assertFalse(timers.get(BoostPotion.SUPER_ATTACK).isReady());
		assertTrue(timers.get(BoostPotion.SUPER_ATTACK).isActive());

		tick(202, levels(102, 99, 99, 99, 99), Set.of(), false, 103);
		assertTrue(timers.get(BoostPotion.SUPER_ATTACK).isReady());
	}

	@Test
	public void highThresholdCannotAdvertiseAUsefulDoseAtFullPotionCap()
	{
		tick(0, levels(118, 99, 99, 99, 99), Set.of(), false, 130);
		assertFalse(timers.get(BoostPotion.SUPER_ATTACK).isReady());

		tick(1, levels(117, 99, 99, 99, 99), Set.of(), false, 130);
		assertTrue(timers.get(BoostPotion.SUPER_ATTACK).isReady());

		tick(2, levels(118, 99, 99, 99, 99), Set.of(), false, 130);
		assertFalse(timers.get(BoostPotion.SUPER_ATTACK).isReady());
		assertEquals(-1, timers.get(BoostPotion.SUPER_ATTACK).getFinishedAgeSeconds(atTick(2)), DELTA);
	}

	@Test
	public void missingSkillValuesAreUnavailableRatherThanReady()
	{
		timers.tick(0, Map.of(), Map.of(), Set.of(), false, thresholds(103), atTick(0));
		for (BoostPotion potion : BoostPotion.values())
		{
			assertFalse(potion.name(), timers.get(potion).isReady());
			assertFalse(potion.name(), timers.get(potion).isActive());
		}
	}

	@Test
	public void missingBoostedSampleCannotAnimateAsAnUnboostedSkill()
	{
		timers.tick(0, base, Map.of(), Set.of(), false, thresholds(103), atTick(0));
		for (BoostPotion potion : BoostPotion.values())
		{
			assertFalse(potion.name(), timers.get(potion).isReady());
			assertFalse(potion.name(), timers.get(potion).isActive());
		}
	}

	@Test
	public void clearDiscardsReadinessAndLearnedPhase()
	{
		tick(99, levels(119, 99, 99, 99, 99), Set.of(), false, 103);
		tick(100, levels(118, 99, 99, 99, 99), Set.of(), false, 103);
		assertTrue(timers.get(BoostPotion.SUPER_ATTACK).isKnown());
		assertTrue(timers.get(BoostPotion.MAGIC).isReady());

		timers.clear();
		for (BoostPotion potion : BoostPotion.values())
		{
			assertFalse(potion.name(), timers.get(potion).isReady());
			assertFalse(potion.name(), timers.get(potion).isActive());
		}

		tick(101, levels(118, 99, 99, 99, 99), Set.of(), false, 103);
		assertTrue(timers.get(BoostPotion.SUPER_ATTACK).isActive());
		assertFalse(timers.get(BoostPotion.SUPER_ATTACK).isKnown());
	}

	@Test
	public void strengthBelow110WaitsAt110AndAnimatesAt109()
	{
		Map<Skill, Integer> targets = Map.of(Skill.STRENGTH, 110);
		timers.tick(99, base, levels(99, 111, 99, 99, 99), Set.of(), false, targets, atTick(99));
		timers.tick(100, base, levels(99, 110, 99, 99, 99), Set.of(), false, targets, atTick(100));
		Cooldown cooldown = timers.get(BoostPotion.SUPER_STRENGTH);
		assertFalse(cooldown.isReady());
		assertEquals(60, cooldown.getRemainingSeconds(atTick(100)), DELTA);

		timers.tick(200, base, levels(99, 109, 99, 99, 99), Set.of(), false, targets, atTick(200));
		assertTrue(cooldown.isReady());
		assertFalse(cooldown.isActive());
		assertTrue(timers.get(BoostPotion.SUPER_COMBAT).isReady());
		assertFalse(timers.get(BoostPotion.SUPER_ATTACK).isReady());
		assertFalse(timers.get(BoostPotion.SUPER_ATTACK).isActive());
	}

	@Test
	public void eachSkillUsesItsOwnAbsoluteTarget()
	{
		Map<Skill, Integer> targets = levels(0, 110, 0, 105, 102);
		timers.tick(99, base, levels(99, 111, 99, 104, 102), Set.of(), false, targets, atTick(99));
		timers.tick(100, base, levels(99, 110, 99, 104, 102), Set.of(), false, targets, atTick(100));

		assertEquals(60, timers.get(BoostPotion.SUPER_COMBAT).getRemainingSeconds(atTick(100)), DELTA);
		assertTrue(timers.get(BoostPotion.RANGING).isReady());
		assertTrue(timers.get(BoostPotion.BASTION).isReady());
		assertFalse(timers.get(BoostPotion.MAGIC).isReady());
		assertEquals(60, timers.get(BoostPotion.BATTLEMAGE).getRemainingSeconds(atTick(100)), DELTA);
		assertFalse(timers.get(BoostPotion.DEFENCE).isActive());
		assertFalse(timers.get(BoostPotion.DEFENCE).isReady());
	}

	@Test
	public void disablingTargetClearsBothCountdownAndReadiness()
	{
		timers.tick(0, base, levels(99, 109, 99, 99, 99), Set.of(), false,
			Map.of(Skill.STRENGTH, 110), atTick(0));
		assertTrue(timers.get(BoostPotion.SUPER_STRENGTH).isReady());
		timers.tick(1, base, levels(99, 109, 99, 99, 99), Set.of(), false,
			Map.of(Skill.STRENGTH, 109), atTick(1));
		assertTrue(timers.get(BoostPotion.SUPER_STRENGTH).isActive());
		assertFalse(timers.get(BoostPotion.SUPER_STRENGTH).isReady());

		timers.tick(2, base, levels(99, 109, 99, 99, 99), Set.of(), false,
			Map.of(Skill.STRENGTH, 0), atTick(2));
		for (BoostPotion potion : BoostPotion.values())
		{
			assertFalse(potion.name(), timers.get(potion).isReady());
			assertFalse(potion.name(), timers.get(potion).isActive());
		}
	}

	@Test
	public void belowBaseTargetWaitsForAnActualDrainInsteadOfPredictingOne()
	{
		Map<Skill, Integer> targets = Map.of(Skill.STRENGTH, 90);
		// Attack teaches the shared phase even though its target is disabled.
		timers.tick(99, base, levels(111, 95, 99, 99, 99), Set.of(), false, targets, atTick(99));
		timers.tick(100, base, levels(110, 95, 99, 99, 99), Set.of(), false, targets, atTick(100));
		assertFalse(timers.get(BoostPotion.SUPER_STRENGTH).isActive());
		assertFalse(timers.get(BoostPotion.SUPER_STRENGTH).isReady());

		timers.tick(101, base, levels(110, 89, 99, 99, 99), Set.of(), false, targets, atTick(101));
		assertTrue(timers.get(BoostPotion.SUPER_STRENGTH).isReady());
		timers.tick(102, base, levels(110, 90, 99, 99, 99), Set.of(), false, targets, atTick(102));
		assertFalse(timers.get(BoostPotion.SUPER_STRENGTH).isReady());
		assertFalse(timers.get(BoostPotion.SUPER_STRENGTH).isActive());
	}

	@Test
	public void thresholdAtBaseCannotNaturallyCrossButOneAboveCan()
	{
		Map<Skill, Integer> targets = levels(99, 100, 0, 0, 0);
		timers.tick(99, base, levels(101, 101, 99, 99, 99), Set.of(), false, targets, atTick(99));
		timers.tick(100, base, levels(100, 100, 99, 99, 99), Set.of(), false, targets, atTick(100));
		assertFalse(timers.get(BoostPotion.SUPER_ATTACK).isActive());
		assertFalse(timers.get(BoostPotion.SUPER_ATTACK).isReady());
		assertEquals(60, timers.get(BoostPotion.SUPER_STRENGTH).getRemainingSeconds(atTick(100)), DELTA);
		timers.tick(200, base, levels(99, 99, 99, 99, 99), Set.of(), false, targets, atTick(200));
		assertFalse(timers.get(BoostPotion.SUPER_ATTACK).isReady());
		assertTrue(timers.get(BoostPotion.SUPER_STRENGTH).isReady());
	}

	@Test
	public void absoluteTargetDoesNotMoveWhenBaseLevelChanges()
	{
		base.put(Skill.STRENGTH, 90);
		Map<Skill, Integer> targets = Map.of(Skill.STRENGTH, 105);
		timers.tick(0, base, levels(99, 104, 99, 99, 99), Set.of(), false, targets, atTick(0));
		assertTrue(timers.get(BoostPotion.SUPER_STRENGTH).isReady());
		base.put(Skill.STRENGTH, 91);
		timers.tick(1, base, levels(99, 104, 99, 99, 99), Set.of(), false, targets, atTick(1));
		assertTrue(timers.get(BoostPotion.SUPER_STRENGTH).isReady());
		timers.tick(2, base, levels(99, 105, 99, 99, 99), Set.of(), false, targets, atTick(2));
		assertFalse(timers.get(BoostPotion.SUPER_STRENGTH).isReady());
	}

	@Test
	public void absoluteTargetStillRequiresAPotionThatCanImproveTheSkill()
	{
		Map<Skill, Integer> targets = Map.of(Skill.STRENGTH, 110);
		timers.tick(0, base, levels(99, 112, 99, 99, 99), Set.of(), false, targets, atTick(0));
		assertFalse(timers.get(BoostPotion.STRENGTH).isReady());
		timers.tick(1, base, levels(99, 110, 99, 99, 99), Set.of(), false, targets, atTick(1));
		assertFalse(timers.get(BoostPotion.STRENGTH).isReady());
		timers.tick(2, base, levels(99, 109, 99, 99, 99), Set.of(), false, targets, atTick(2));
		assertTrue(timers.get(BoostPotion.STRENGTH).isReady());
		assertTrue(timers.get(BoostPotion.SUPER_STRENGTH).isReady());
	}

	private void tick(int tick, Map<Skill, Integer> boosted, Set<Skill> protectedSkills, boolean preserve, int threshold)
	{
		timers.tick(tick, base, boosted, protectedSkills, preserve, thresholds(threshold), atTick(tick));
	}

	private static Map<Skill, Integer> thresholds(int level)
	{
		return levels(level, level, level, level, level);
	}

	private static Map<Skill, Integer> levels(int attack, int strength, int defence, int ranged, int magic)
	{
		Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
		levels.put(Skill.ATTACK, attack);
		levels.put(Skill.STRENGTH, strength);
		levels.put(Skill.DEFENCE, defence);
		levels.put(Skill.RANGED, ranged);
		levels.put(Skill.MAGIC, magic);
		return levels;
	}

	private static long atTick(int tick)
	{
		return START + tick * Cooldown.TICK_NANOS;
	}
}
