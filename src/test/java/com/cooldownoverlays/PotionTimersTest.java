package com.cooldownoverlays;

import java.lang.reflect.Proxy;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import net.runelite.api.Client;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.gameval.VarPlayerID;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PotionTimersTest
{
	private static final double DELTA = 0.000001;
	private static final long START = 1_000_000_000L;
	private final PotionTimers timers = new PotionTimers();

	@Test
	public void initialReadyStateIsAvailableImmediatelyAndRepeatedSamplesPreserveItsAge()
	{
		timers.tick(new EnumMap<>(PotionEffect.class), START);

		for (PotionEffect effect : PotionEffect.values())
		{
			assertFalse(timers.get(effect).isActive());
			assertTrue(timers.get(effect).isReady());
			assertEquals(0, timers.get(effect).getFinishedAgeSeconds(START), DELTA);
		}
		timers.tick(new EnumMap<>(PotionEffect.class), atTick(1));
		for (PotionEffect effect : PotionEffect.values())
		{
			assertEquals(0.6, timers.get(effect).getFinishedAgeSeconds(atTick(1)), DELTA);
		}
	}

	@Test
	public void surgeAndStaminaConvertTheirTenTickCounters()
	{
		Map<PotionEffect, Integer> samples = sample(PotionEffect.SURGE, 50);
		samples.put(PotionEffect.STAMINA, 20);
		timers.initialize(samples, START);

		assertEquals(300, remaining(PotionEffect.SURGE, 0), DELTA);
		assertEquals(120, remaining(PotionEffect.STAMINA, 0), DELTA);
		assertEquals(500, timers.get(PotionEffect.SURGE).getTotalTicks());
		assertEquals(200, timers.get(PotionEffect.STAMINA).getTotalTicks());
	}

	@Test
	public void extendedStaminaUsesTheLongerReportedDuration()
	{
		timers.initialize(sample(PotionEffect.STAMINA, 40), START);

		assertEquals(240, remaining(PotionEffect.STAMINA, 0), DELTA);
		assertEquals(400, timers.get(PotionEffect.STAMINA).getTotalTicks());
		assertEquals(1, timers.get(PotionEffect.STAMINA).getRemainingFraction(START), DELTA);
	}

	@Test
	public void raidBoostsConvertTheirTwentyFiveTickCounters()
	{
		Map<PotionEffect, Integer> samples = sample(PotionEffect.OVERLOAD, 20);
		samples.put(PotionEffect.SMELLING_SALTS, 32);
		timers.initialize(samples, START);

		assertEquals(300, remaining(PotionEffect.OVERLOAD, 0), DELTA);
		assertEquals(480, remaining(PotionEffect.SMELLING_SALTS, 0), DELTA);
		assertEquals(500, timers.get(PotionEffect.OVERLOAD).getTotalTicks());
		assertEquals(800, timers.get(PotionEffect.SMELLING_SALTS).getTotalTicks());
		for (int tick = 1; tick < 25; tick++)
		{
			timers.tick(samples, atTick(tick));
		}
		samples.put(PotionEffect.OVERLOAD, 19);
		samples.put(PotionEffect.SMELLING_SALTS, 31);
		timers.tick(samples, atTick(25));
		assertEquals(285, remaining(PotionEffect.OVERLOAD, 25), DELTA);
		assertEquals(465, remaining(PotionEffect.SMELLING_SALTS, 25), DELTA);
	}

	@Test
	public void overloadRefreshPreservesItsCurrentBucketPhase()
	{
		timers.initialize(sample(PotionEffect.OVERLOAD, 10), START);
		for (int tick = 1; tick < 10; tick++)
		{
			timers.tick(sample(PotionEffect.OVERLOAD, 10), atTick(tick));
		}
		timers.tick(sample(PotionEffect.OVERLOAD, 20), atTick(10));

		assertEquals(294, remaining(PotionEffect.OVERLOAD, 10), DELTA);
		assertEquals(500, timers.get(PotionEffect.OVERLOAD).getTotalTicks());
	}

	@Test
	public void pausedRaidCountersCannotInterpolateBeyondTheirReportedBucket()
	{
		Map<PotionEffect, Integer> samples = sample(PotionEffect.OVERLOAD, 20);
		samples.put(PotionEffect.SMELLING_SALTS, 32);
		timers.initialize(samples, START);
		for (int tick = 1; tick <= 100; tick++)
		{
			timers.tick(samples, atTick(tick));
		}

		assertEquals(285.6, remaining(PotionEffect.OVERLOAD, 100), DELTA);
		assertEquals(465.6, remaining(PotionEffect.SMELLING_SALTS, 100), DELTA);
	}

	@Test
	public void prayerEnhanceUsesItsReportedRateAndPreservesTheBucketPhase()
	{
		timers.initialize(sample(PotionEffect.PRAYER_ENHANCE, 40), 12, START);
		assertEquals(288, remaining(PotionEffect.PRAYER_ENHANCE, 0), DELTA);
		assertEquals(480, timers.get(PotionEffect.PRAYER_ENHANCE).getTotalTicks());
		for (int tick = 1; tick < 5; tick++)
		{
			timers.tick(sample(PotionEffect.PRAYER_ENHANCE, 40), 12, atTick(tick));
		}
		timers.tick(sample(PotionEffect.PRAYER_ENHANCE, 50), 12, atTick(5));
		assertEquals(357, remaining(PotionEffect.PRAYER_ENHANCE, 5), DELTA);
		assertEquals(595, timers.get(PotionEffect.PRAYER_ENHANCE).getTotalTicks());
		for (int tick = 6; tick < 12; tick++)
		{
			timers.tick(sample(PotionEffect.PRAYER_ENHANCE, 50), 12, atTick(tick));
		}
		timers.tick(sample(PotionEffect.PRAYER_ENHANCE, 49), 12, atTick(12));
		assertEquals(352.8, remaining(PotionEffect.PRAYER_ENHANCE, 12), DELTA);
	}

	@Test
	public void prayerEnhanceRateChangesResynchronizeAndZeroRateMeansInactive()
	{
		timers.initialize(sample(PotionEffect.PRAYER_ENHANCE, 40), 12, START);
		timers.tick(sample(PotionEffect.PRAYER_ENHANCE, 40), 8, atTick(1));
		assertEquals(192, remaining(PotionEffect.PRAYER_ENHANCE, 1), DELTA);
		assertEquals(320, timers.get(PotionEffect.PRAYER_ENHANCE).getTotalTicks());
		timers.tick(sample(PotionEffect.PRAYER_ENHANCE, 40), 0, atTick(2));
		assertFalse(timers.get(PotionEffect.PRAYER_ENHANCE).isActive());
		timers.initialize(sample(PotionEffect.PRAYER_ENHANCE, 40), 0, atTick(3));
		assertFalse(timers.get(PotionEffect.PRAYER_ENHANCE).isActive());
		assertTrue(timers.get(PotionEffect.PRAYER_ENHANCE).isReady());
		assertEquals(0, timers.get(PotionEffect.PRAYER_ENHANCE).getFinishedAgeSeconds(atTick(3)), DELTA);
	}

	@Test
	public void joiningActiveLiquidAdrenalineDoesNotInventItsRemainingDuration()
	{
		timers.initialize(sample(PotionEffect.LIQUID_ADRENALINE, 1), START);
		timers.tick(sample(PotionEffect.LIQUID_ADRENALINE, 1), atTick(1));

		assertTrue(timers.get(PotionEffect.LIQUID_ADRENALINE).isActive());
		assertFalse(timers.get(PotionEffect.LIQUID_ADRENALINE).isKnown());
		assertEquals(0, timers.get(PotionEffect.LIQUID_ADRENALINE).getTotalTicks());
		assertEquals(-1, timers.get(PotionEffect.LIQUID_ADRENALINE).getFinishedAgeSeconds(atTick(1)), DELTA);
	}

	@Test
	public void liquidAdrenalineTransitionStartsKnownDurationAndServerControlsExpiry()
	{
		timers.initialize(sample(PotionEffect.LIQUID_ADRENALINE, 0), START);
		timers.tick(sample(PotionEffect.LIQUID_ADRENALINE, 1), atTick(1));

		assertTrue(timers.get(PotionEffect.LIQUID_ADRENALINE).isKnown());
		assertEquals(150, remaining(PotionEffect.LIQUID_ADRENALINE, 1), DELTA);
		assertEquals(250, timers.get(PotionEffect.LIQUID_ADRENALINE).getTotalTicks());
		for (int tick = 2; tick <= 250; tick++)
		{
			timers.tick(sample(PotionEffect.LIQUID_ADRENALINE, 1), atTick(tick));
		}
		assertTrue(timers.get(PotionEffect.LIQUID_ADRENALINE).isActive());
		assertEquals(0.6, remaining(PotionEffect.LIQUID_ADRENALINE, 250), DELTA);
		timers.tick(sample(PotionEffect.LIQUID_ADRENALINE, 1), atTick(251));
		assertTrue(timers.get(PotionEffect.LIQUID_ADRENALINE).isActive());
		assertFalse(timers.get(PotionEffect.LIQUID_ADRENALINE).isKnown());
		assertEquals(-1, timers.get(PotionEffect.LIQUID_ADRENALINE).getFinishedAgeSeconds(atTick(251)), DELTA);
		timers.tick(sample(PotionEffect.LIQUID_ADRENALINE, 0), atTick(252));
		assertFalse(timers.get(PotionEffect.LIQUID_ADRENALINE).isActive());
		assertEquals(0, timers.get(PotionEffect.LIQUID_ADRENALINE).getFinishedAgeSeconds(atTick(252)), DELTA);
	}

	@Test
	public void confirmedLiquidAdrenalineDoseRefreshesAnUnchangedActiveFlag()
	{
		timers.initialize(sample(PotionEffect.LIQUID_ADRENALINE, 1), START);
		timers.onLiquidAdrenalineDose();
		timers.tick(sample(PotionEffect.LIQUID_ADRENALINE, 1), atTick(1));
		assertEquals(150, remaining(PotionEffect.LIQUID_ADRENALINE, 1), DELTA);
		timers.tick(sample(PotionEffect.LIQUID_ADRENALINE, 1), atTick(2));
		assertEquals(149.4, remaining(PotionEffect.LIQUID_ADRENALINE, 2), DELTA);
		timers.onLiquidAdrenalineDose();
		timers.tick(sample(PotionEffect.LIQUID_ADRENALINE, 1), atTick(3));
		assertEquals(150, remaining(PotionEffect.LIQUID_ADRENALINE, 3), DELTA);
	}

	@Test
	public void confirmedDoseBeforeFirstTickStillStartsAKnownLiquidAdrenalineDuration()
	{
		timers.onLiquidAdrenalineDose();
		timers.tick(sample(PotionEffect.LIQUID_ADRENALINE, 1), START);

		assertTrue(timers.get(PotionEffect.LIQUID_ADRENALINE).isKnown());
		assertEquals(150, remaining(PotionEffect.LIQUID_ADRENALINE, 0), DELTA);
	}

	@Test
	public void liquidAdrenalineDoseRequiresActiveFlagAndLifecycleClearDiscardsPendingDose()
	{
		timers.initialize(sample(PotionEffect.LIQUID_ADRENALINE, 0), START);
		timers.onLiquidAdrenalineDose();
		timers.tick(sample(PotionEffect.LIQUID_ADRENALINE, 0), atTick(1));
		assertFalse(timers.get(PotionEffect.LIQUID_ADRENALINE).isActive());
		assertTrue(timers.get(PotionEffect.LIQUID_ADRENALINE).isReady());
		assertEquals(0.6, timers.get(PotionEffect.LIQUID_ADRENALINE).getFinishedAgeSeconds(atTick(1)), DELTA);
		timers.onLiquidAdrenalineDose();
		timers.clear();
		timers.tick(sample(PotionEffect.LIQUID_ADRENALINE, 1), atTick(2));
		assertTrue(timers.get(PotionEffect.LIQUID_ADRENALINE).isActive());
		assertFalse(timers.get(PotionEffect.LIQUID_ADRENALINE).isKnown());
		timers.clear();
		assertFalse(timers.get(PotionEffect.LIQUID_ADRENALINE).isActive());
		assertEquals(-1, timers.get(PotionEffect.LIQUID_ADRENALINE).getFinishedAgeSeconds(atTick(3)), DELTA);
	}

	@Test
	public void unchangedBucketsCountDownLocallyAndServerUpdatesResynchronize()
	{
		timers.initialize(sample(PotionEffect.SURGE, 50), START);
		for (int tick = 1; tick < 10; tick++)
		{
			timers.tick(sample(PotionEffect.SURGE, 50), atTick(tick));
		}
		assertEquals(294.6, remaining(PotionEffect.SURGE, 9), DELTA);
		timers.tick(sample(PotionEffect.SURGE, 49), atTick(10));

		assertEquals(294, remaining(PotionEffect.SURGE, 10), DELTA);
		assertEquals(500, timers.get(PotionEffect.SURGE).getTotalTicks());
	}

	@Test
	public void antifireRefreshPreservesTheCurrentThirtyTickBucketPhase()
	{
		timers.initialize(sample(PotionEffect.ANTIFIRE, 20), START);
		for (int tick = 1; tick < 10; tick++)
		{
			timers.tick(sample(PotionEffect.ANTIFIRE, 20), atTick(tick));
		}
		timers.tick(sample(PotionEffect.ANTIFIRE, 40), atTick(10));

		// The second dose adds 600 ticks to the existing 590, without a fresh bucket.
		assertEquals(714, remaining(PotionEffect.ANTIFIRE, 10), DELTA);
		assertEquals(1190, timers.get(PotionEffect.ANTIFIRE).getTotalTicks());
		for (int tick = 11; tick < 30; tick++)
		{
			timers.tick(sample(PotionEffect.ANTIFIRE, 40), atTick(tick));
		}
		timers.tick(sample(PotionEffect.ANTIFIRE, 39), atTick(30));
		assertEquals(702, remaining(PotionEffect.ANTIFIRE, 30), DELTA);
		assertEquals(1190, timers.get(PotionEffect.ANTIFIRE).getTotalTicks());
	}

	@Test
	public void superAntifireUsesTwentyTickBucketsAndPreservesRefreshPhase()
	{
		timers.initialize(sample(PotionEffect.SUPER_ANTIFIRE, 15), START);
		assertEquals(180, remaining(PotionEffect.SUPER_ANTIFIRE, 0), DELTA);
		for (int tick = 1; tick < 5; tick++)
		{
			timers.tick(sample(PotionEffect.SUPER_ANTIFIRE, 15), atTick(tick));
		}
		timers.tick(sample(PotionEffect.SUPER_ANTIFIRE, 30), atTick(5));

		assertEquals(357, remaining(PotionEffect.SUPER_ANTIFIRE, 5), DELTA);
		assertEquals(595, timers.get(PotionEffect.SUPER_ANTIFIRE).getTotalTicks());
	}

	@Test
	public void eachAntivenomVariantUsesItsActualVenomProtectionCounter()
	{
		timers.initialize(sample(PotionEffect.ANTIVENOM, -39), START);
		assertEquals(18, remaining(PotionEffect.ANTIVENOM, 0), DELTA);
		assertEquals(30, timers.get(PotionEffect.ANTIVENOM).getTotalTicks());
		timers.initialize(sample(PotionEffect.ANTIVENOM, -48), START);
		assertEquals(180, remaining(PotionEffect.ANTIVENOM, 0), DELTA);
		assertEquals(300, timers.get(PotionEffect.ANTIVENOM).getTotalTicks());
		timers.initialize(sample(PotionEffect.ANTIVENOM, -58), START);
		assertEquals(360, remaining(PotionEffect.ANTIVENOM, 0), DELTA);
		assertEquals(600, timers.get(PotionEffect.ANTIVENOM).getTotalTicks());
	}

	@Test
	public void venomProtectionEndsAtThePoisonImmunityTail()
	{
		timers.initialize(sample(PotionEffect.ANTIVENOM, -39), START);
		timers.tick(sample(PotionEffect.ANTIVENOM, -38), atTick(1));

		assertFalse(timers.get(PotionEffect.ANTIVENOM).isActive());
		assertEquals(0, timers.get(PotionEffect.ANTIVENOM).getFinishedAgeSeconds(atTick(1)), DELTA);
		timers.tick(sample(PotionEffect.ANTIVENOM, -37), atTick(2));
		assertFalse(timers.get(PotionEffect.ANTIVENOM).isActive());
		assertEquals(0.6, timers.get(PotionEffect.ANTIVENOM).getFinishedAgeSeconds(atTick(2)), DELTA);
	}

	@Test
	public void antivenomRefreshPreservesThePoisonCountersExistingPhase()
	{
		timers.initialize(sample(PotionEffect.ANTIVENOM, -38), START);
		for (int tick = 1; tick < 10; tick++)
		{
			timers.tick(sample(PotionEffect.ANTIVENOM, -38), atTick(tick));
		}
		timers.tick(sample(PotionEffect.ANTIVENOM, -48), atTick(10));

		assertEquals(174, remaining(PotionEffect.ANTIVENOM, 10), DELTA);
		timers.tick(sample(PotionEffect.ANTIVENOM, 0), atTick(11));
		timers.tick(sample(PotionEffect.ANTIVENOM, -48), atTick(12));
		assertEquals(180, remaining(PotionEffect.ANTIVENOM, 12), DELTA);
	}

	@Test
	public void divineCountersUseGameTicksAndRemainIndependent()
	{
		Map<PotionEffect, Integer> samples = new EnumMap<>(PotionEffect.class);
		for (PotionEffect effect : PotionEffect.values())
		{
			if (effect.name().startsWith("DIVINE_"))
			{
				samples.put(effect, 500);
			}
		}
		timers.initialize(samples, START);
		for (PotionEffect effect : samples.keySet())
		{
			assertEquals(300, remaining(effect, 0), DELTA);
		}
		samples.put(PotionEffect.DIVINE_ATTACK, 0);
		samples.put(PotionEffect.DIVINE_COMBAT, 499);
		timers.tick(samples, atTick(1));

		assertFalse(timers.get(PotionEffect.DIVINE_ATTACK).isActive());
		assertTrue(timers.get(PotionEffect.DIVINE_COMBAT).isActive());
		assertEquals(299.4, remaining(PotionEffect.DIVINE_COMBAT, 1), DELTA);
		assertTrue(timers.get(PotionEffect.DIVINE_MAGIC).isActive());
	}

	@Test
	public void positiveServerStateDoesNotExpireBeforeTheServerClearsIt()
	{
		timers.initialize(sample(PotionEffect.SURGE, 1), START);
		for (int tick = 1; tick < 30; tick++)
		{
			timers.tick(sample(PotionEffect.SURGE, 1), atTick(tick));
		}
		assertTrue(timers.get(PotionEffect.SURGE).isActive());
		assertEquals(-1, timers.get(PotionEffect.SURGE).getFinishedAgeSeconds(atTick(29)), DELTA);
		timers.tick(sample(PotionEffect.SURGE, 0), atTick(30));

		assertFalse(timers.get(PotionEffect.SURGE).isActive());
		assertEquals(0, timers.get(PotionEffect.SURGE).getFinishedAgeSeconds(atTick(30)), DELTA);
	}

	@Test
	public void pausedSurgeCannotCountDownBeyondItsAuthoritativeBucket()
	{
		timers.initialize(sample(PotionEffect.SURGE, 50), START);
		for (int tick = 1; tick <= 100; tick++)
		{
			timers.tick(sample(PotionEffect.SURGE, 50), atTick(tick));
		}

		assertEquals(294.6, remaining(PotionEffect.SURGE, 100), DELTA);
		timers.tick(sample(PotionEffect.SURGE, 49), atTick(101));
		assertEquals(294, remaining(PotionEffect.SURGE, 101), DELTA);
	}

	@Test
	public void lifecycleClearWaitsForFreshSamplesBeforeDeclaringReadiness()
	{
		timers.initialize(sample(PotionEffect.SURGE, 50), START);
		timers.clear();
		assertFalse(timers.get(PotionEffect.SURGE).isActive());
		assertFalse(timers.get(PotionEffect.SURGE).isReady());
		assertEquals(-1, timers.get(PotionEffect.SURGE).getFinishedAgeSeconds(atTick(1)), DELTA);
		timers.initialize(sample(PotionEffect.SURGE, 0), atTick(2));
		assertTrue(timers.get(PotionEffect.SURGE).isReady());
		assertEquals(0, timers.get(PotionEffect.SURGE).getFinishedAgeSeconds(atTick(2)), DELTA);
		timers.initialize(sample(PotionEffect.SURGE, 25), atTick(3));
		assertEquals(150, remaining(PotionEffect.SURGE, 3), DELTA);
		assertEquals(-1, timers.get(PotionEffect.SURGE).getFinishedAgeSeconds(atTick(3)), DELTA);
	}

	@Test
	public void clientSamplingUsesPoisonVarpAndRequiresActiveStaminaFlag()
	{
		Map<Integer, Integer> varbits = new HashMap<>();
		varbits.put(VarbitID.SURGE_POTION_TIMER, 50);
		varbits.put(VarbitID.STAMINA_DURATION, 40);
		varbits.put(VarbitID.STAMINA_ACTIVE, 0);
		varbits.put(VarbitID.ANTIFIRE_POTION, 20);
		varbits.put(VarbitID.SUPER_ANTIFIRE_POTION, 15);
		varbits.put(VarbitID.DIVINEATTACK_POTION_TIME, 500);
		Map<Integer, Integer> varps = new HashMap<>();
		varps.put(VarPlayerID.POISON, -48);
		Client client = client(varbits, varps);
		timers.initialize(client, START);

		assertFalse(timers.get(PotionEffect.STAMINA).isActive());
		assertEquals(300, remaining(PotionEffect.SURGE, 0), DELTA);
		assertEquals(360, remaining(PotionEffect.ANTIFIRE, 0), DELTA);
		assertEquals(180, remaining(PotionEffect.SUPER_ANTIFIRE, 0), DELTA);
		assertEquals(180, remaining(PotionEffect.ANTIVENOM, 0), DELTA);
		assertEquals(300, remaining(PotionEffect.DIVINE_ATTACK, 0), DELTA);
		varbits.put(VarbitID.STAMINA_ACTIVE, 1);
		timers.tick(client, atTick(1));
		assertEquals(240, remaining(PotionEffect.STAMINA, 1), DELTA);
	}

	@Test
	public void clientSamplingReadsRaidCountersAndClearsChambersEffectsOutsideTheRaid()
	{
		Map<Integer, Integer> varbits = new HashMap<>();
		varbits.put(VarbitID.RAIDS_CLIENT_INDUNGEON, 1);
		varbits.put(VarbitID.RAIDS_OVERLOAD_TIMER, 20);
		varbits.put(VarbitID.RAIDS_PRAYERENHANCE_TIMER, 40);
		varbits.put(VarbitID.RAIDS_PRAYERENHANCE_RATE, 12);
		varbits.put(VarbitID.TOA_MIDRAIDLOOT_STATS_TIMER, 32);
		varbits.put(VarbitID.TOA_MIDRAIDLOOT_ENERGY_ACTIVE, 1);
		Client client = client(varbits, new HashMap<>());
		timers.initialize(client, START);

		assertEquals(300, remaining(PotionEffect.OVERLOAD, 0), DELTA);
		assertEquals(288, remaining(PotionEffect.PRAYER_ENHANCE, 0), DELTA);
		assertEquals(480, remaining(PotionEffect.SMELLING_SALTS, 0), DELTA);
		assertTrue(timers.get(PotionEffect.LIQUID_ADRENALINE).isActive());
		assertFalse(timers.get(PotionEffect.LIQUID_ADRENALINE).isKnown());
		varbits.put(VarbitID.RAIDS_PRAYERENHANCE_RATE, 8);
		timers.tick(client, atTick(1));
		assertEquals(192, remaining(PotionEffect.PRAYER_ENHANCE, 1), DELTA);
		varbits.put(VarbitID.RAIDS_CLIENT_INDUNGEON, 0);
		timers.tick(client, atTick(2));
		assertFalse(timers.get(PotionEffect.OVERLOAD).isActive());
		assertFalse(timers.get(PotionEffect.PRAYER_ENHANCE).isActive());
		assertFalse(timers.get(PotionEffect.OVERLOAD).isReady());
		assertFalse(timers.get(PotionEffect.PRAYER_ENHANCE).isReady());
		assertTrue(timers.get(PotionEffect.SMELLING_SALTS).isActive());
	}

	@Test
	public void chambersSuppliesBecomeReadyOnlyWhenTheyAreUsableInsideTheRaid()
	{
		Map<Integer, Integer> varbits = new HashMap<>();
		Client client = client(varbits, new HashMap<>());
		timers.initialize(client, START);
		assertFalse(timers.get(PotionEffect.OVERLOAD).isReady());
		assertFalse(timers.get(PotionEffect.PRAYER_ENHANCE).isReady());
		timers.tick(client, atTick(1));
		assertFalse(timers.get(PotionEffect.OVERLOAD).isReady());
		assertFalse(timers.get(PotionEffect.PRAYER_ENHANCE).isReady());

		varbits.put(VarbitID.RAIDS_CLIENT_INDUNGEON, 1);
		timers.tick(client, atTick(2));
		assertTrue(timers.get(PotionEffect.OVERLOAD).isReady());
		assertTrue(timers.get(PotionEffect.PRAYER_ENHANCE).isReady());
		assertEquals(0, timers.get(PotionEffect.OVERLOAD).getFinishedAgeSeconds(atTick(2)), DELTA);
		timers.tick(client, atTick(3));
		assertEquals(0.6, timers.get(PotionEffect.OVERLOAD).getFinishedAgeSeconds(atTick(3)), DELTA);

		varbits.put(VarbitID.RAIDS_CLIENT_INDUNGEON, 0);
		timers.tick(client, atTick(4));
		assertFalse(timers.get(PotionEffect.OVERLOAD).isReady());
		assertFalse(timers.get(PotionEffect.PRAYER_ENHANCE).isReady());
		varbits.put(VarbitID.RAIDS_CLIENT_INDUNGEON, 1);
		varbits.put(VarbitID.RAIDS_OVERLOAD_TIMER, 20);
		timers.tick(client, atTick(5));
		assertFalse(timers.get(PotionEffect.OVERLOAD).isReady());
		assertEquals(300, remaining(PotionEffect.OVERLOAD, 5), DELTA);
		assertTrue(timers.get(PotionEffect.PRAYER_ENHANCE).isReady());
	}

	private static Client client(Map<Integer, Integer> varbits, Map<Integer, Integer> varps)
	{
		return (Client) Proxy.newProxyInstance(Client.class.getClassLoader(), new Class<?>[]{Client.class},
			(proxy, method, args) ->
			{
				if (method.getName().equals("getVarbitValue") && args.length == 1)
				{
					return varbits.getOrDefault(args[0], 0);
				}
				if (method.getName().equals("getVarpValue"))
				{
					return varps.getOrDefault(args[0], 0);
				}
				throw new UnsupportedOperationException(method.getName());
			});
	}

	private double remaining(PotionEffect effect, int tick)
	{
		return timers.get(effect).getRemainingSeconds(atTick(tick));
	}

	private static Map<PotionEffect, Integer> sample(PotionEffect effect, int rawValue)
	{
		Map<PotionEffect, Integer> samples = new EnumMap<>(PotionEffect.class);
		samples.put(effect, rawValue);
		return samples;
	}

	private static long atTick(int tick)
	{
		return START + tick * Cooldown.TICK_NANOS;
	}
}
