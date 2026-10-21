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

public class AdditionalPotionTimersTest
{
	private static final double DELTA = 0.000001;
	private static final long START = 1_000_000_000L;
	private final PotionTimers timers = new PotionTimers();

	@Test
	public void newServerCountersUseTheirOwnRatesAndDurations()
	{
		Map<PotionEffect, Integer> samples = new EnumMap<>(PotionEffect.class);
		samples.put(PotionEffect.GOADING, 100);
		samples.put(PotionEffect.MENAPHITE_REMEDY, 20);
		samples.put(PotionEffect.MOONLIGHT, 500);
		samples.put(PotionEffect.PRAYER_REGENERATION, 66);
		timers.initialize(samples, START);

		assertEquals(360, remaining(PotionEffect.GOADING, 0), DELTA);
		assertEquals(300, remaining(PotionEffect.MENAPHITE_REMEDY, 0), DELTA);
		assertEquals(300, remaining(PotionEffect.MOONLIGHT, 0), DELTA);
		assertEquals(475.2, remaining(PotionEffect.PRAYER_REGENERATION, 0), DELTA);
		assertEquals(600, timers.get(PotionEffect.GOADING).getTotalTicks());
		assertEquals(500, timers.get(PotionEffect.MENAPHITE_REMEDY).getTotalTicks());
		assertEquals(500, timers.get(PotionEffect.MOONLIGHT).getTotalTicks());
		assertEquals(800, timers.get(PotionEffect.PRAYER_REGENERATION).getTotalTicks());
	}

	@Test
	public void newCountersRemainAuthoritativeWhenPausedAndClearToReady()
	{
		Map<PotionEffect, Integer> samples = new EnumMap<>(PotionEffect.class);
		samples.put(PotionEffect.GOADING, 1);
		samples.put(PotionEffect.MENAPHITE_REMEDY, 1);
		samples.put(PotionEffect.PRAYER_REGENERATION, 1);
		timers.initialize(samples, START);
		for (int tick = 1; tick <= 50; tick++)
		{
			timers.tick(samples, atTick(tick));
		}
		for (PotionEffect effect : samples.keySet())
		{
			assertTrue(effect.name(), timers.get(effect).isActive());
			assertFalse(effect.name(), timers.get(effect).isReady());
			assertEquals(0.6, remaining(effect, 50), DELTA);
		}
		timers.tick(Map.of(), atTick(51));
		for (PotionEffect effect : samples.keySet())
		{
			assertFalse(effect.name(), timers.get(effect).isActive());
			assertTrue(effect.name(), timers.get(effect).isReady());
		}
	}

	@Test
	public void overloadSourcesHaveIndependentTimersAndRefreshPhases()
	{
		Map<PotionEffect, Integer> samples = new EnumMap<>(PotionEffect.class);
		samples.put(PotionEffect.OVERLOAD, 20);
		samples.put(PotionEffect.NMZ_OVERLOAD, 10);
		samples.put(PotionEffect.BLIGHTED_OVERLOAD, 5);
		timers.initialize(samples, START);
		for (int tick = 1; tick < 10; tick++)
		{
			timers.tick(samples, atTick(tick));
		}
		samples.put(PotionEffect.NMZ_OVERLOAD, 20);
		timers.tick(samples, atTick(10));

		assertEquals(294, remaining(PotionEffect.NMZ_OVERLOAD, 10), DELTA);
		assertEquals(294, remaining(PotionEffect.OVERLOAD, 10), DELTA);
		assertEquals(69, remaining(PotionEffect.BLIGHTED_OVERLOAD, 10), DELTA);
		samples.put(PotionEffect.OVERLOAD, 0);
		timers.tick(samples, atTick(11));
		assertTrue(timers.get(PotionEffect.OVERLOAD).isReady());
		assertTrue(timers.get(PotionEffect.NMZ_OVERLOAD).isActive());
		assertTrue(timers.get(PotionEffect.BLIGHTED_OVERLOAD).isActive());
	}

	@Test
	public void poisonImmunityCoversTheFullCounterWhileVenomTracksOnlyItsStage()
	{
		Map<PotionEffect, Integer> samples = poison(-39);
		timers.initialize(samples, START);
		assertEquals(18, remaining(PotionEffect.ANTIVENOM, 0), DELTA);
		assertEquals(702, remaining(PotionEffect.ANTIPOISON, 0), DELTA);
		for (int tick = 1; tick < 30; tick++)
		{
			timers.tick(samples, atTick(tick));
		}
		timers.tick(poison(-38), atTick(30));

		assertTrue(timers.get(PotionEffect.ANTIVENOM).isReady());
		assertTrue(timers.get(PotionEffect.ANTIPOISON).isActive());
		assertEquals(684, remaining(PotionEffect.ANTIPOISON, 30), DELTA);
	}

	@Test
	public void poisonImmunityBoundariesHandleTheFinalBucketAndNoProtection()
	{
		timers.initialize(poison(-1), START);
		assertEquals(18, remaining(PotionEffect.ANTIPOISON, 0), DELTA);
		assertTrue(timers.get(PotionEffect.ANTIVENOM).isReady());
		timers.tick(poison(0), atTick(1));
		assertTrue(timers.get(PotionEffect.ANTIPOISON).isReady());
		timers.tick(poison(10), atTick(2));
		assertTrue(timers.get(PotionEffect.ANTIPOISON).isReady());
		assertTrue(timers.get(PotionEffect.ANTIVENOM).isReady());
	}

	@Test
	public void poisonRedosingRetainsItsCadenceAcrossTheVenomBoundary()
	{
		timers.initialize(poison(-38), START);
		for (int tick = 1; tick < 10; tick++)
		{
			timers.tick(poison(-38), atTick(tick));
		}
		timers.tick(poison(-48), atTick(10));
		assertEquals(174, remaining(PotionEffect.ANTIVENOM, 10), DELTA);
		assertEquals(858, remaining(PotionEffect.ANTIPOISON, 10), DELTA);
		timers.tick(poison(0), atTick(11));
		timers.tick(poison(-48), atTick(12));
		assertEquals(180, remaining(PotionEffect.ANTIVENOM, 12), DELTA);
		assertEquals(864, remaining(PotionEffect.ANTIPOISON, 12), DELTA);
	}

	@Test
	public void clientReadsNewCountersAndKeepsAllOverloadSourcesSeparate()
	{
		Map<Integer, Integer> varbits = new HashMap<>();
		varbits.put(VarbitID.GOADING_POTION_TIMER, 100);
		varbits.put(VarbitID.STATRENEWAL_POTION_TIMER, 20);
		varbits.put(VarbitID.MOONLIGHT_POTION_TIME, 499);
		varbits.put(VarbitID.DIVINEDEFENCE_POTION_TIME, 500);
		varbits.put(VarbitID.PRAYER_REGENERATION_POTION_TIMER, 66);
		varbits.put(VarbitID.RAIDS_CLIENT_INDUNGEON, 1);
		varbits.put(VarbitID.RAIDS_OVERLOAD_TIMER, 20);
		varbits.put(VarbitID.NZONE_OVERLOAD_POTION_EFFECTS, 10);
		varbits.put(VarbitID.DEADMAN_OVERLOAD_POTION_EFFECTS, 5);
		Client client = client(varbits, Map.of(VarPlayerID.POISON, -48));
		timers.initialize(client, START);

		assertEquals(360, remaining(PotionEffect.GOADING, 0), DELTA);
		assertEquals(300, remaining(PotionEffect.MENAPHITE_REMEDY, 0), DELTA);
		assertEquals(300, remaining(PotionEffect.MOONLIGHT, 0), DELTA);
		assertEquals(475.2, remaining(PotionEffect.PRAYER_REGENERATION, 0), DELTA);
		assertEquals(300, remaining(PotionEffect.OVERLOAD, 0), DELTA);
		assertEquals(150, remaining(PotionEffect.NMZ_OVERLOAD, 0), DELTA);
		assertEquals(75, remaining(PotionEffect.BLIGHTED_OVERLOAD, 0), DELTA);
		assertEquals(864, remaining(PotionEffect.ANTIPOISON, 0), DELTA);
		assertEquals(180, remaining(PotionEffect.ANTIVENOM, 0), DELTA);
		varbits.put(VarbitID.RAIDS_CLIENT_INDUNGEON, 0);
		timers.tick(client, atTick(1));
		assertFalse(timers.get(PotionEffect.OVERLOAD).isActive());
		assertFalse(timers.get(PotionEffect.OVERLOAD).isReady());
		assertTrue(timers.get(PotionEffect.NMZ_OVERLOAD).isActive());
		assertTrue(timers.get(PotionEffect.BLIGHTED_OVERLOAD).isActive());
	}

	@Test
	public void divineDefenceAloneCannotInventAMoonlightEffect()
	{
		Map<Integer, Integer> varbits = new HashMap<>();
		varbits.put(VarbitID.DIVINEDEFENCE_POTION_TIME, 1);
		timers.initialize(client(varbits, Map.of()), START);
		assertTrue(timers.get(PotionEffect.MOONLIGHT).isReady());
		assertFalse(timers.get(PotionEffect.MOONLIGHT).isActive());
	}

	@Test
	public void anActualDivineDefenceDoseOverridesTheMoonlightTimer()
	{
		Map<Integer, Integer> varbits = new HashMap<>();
		varbits.put(VarbitID.MOONLIGHT_POTION_TIME, 300);
		varbits.put(VarbitID.DIVINEDEFENCE_POTION_TIME, 301);
		Client client = client(varbits, Map.of());
		timers.initialize(client, START);
		assertEquals(180.6, remaining(PotionEffect.MOONLIGHT, 0), DELTA);
		varbits.put(VarbitID.DIVINEDEFENCE_POTION_TIME, 500);
		timers.tick(client, atTick(1));

		assertFalse(timers.get(PotionEffect.MOONLIGHT).isActive());
		assertTrue(timers.get(PotionEffect.MOONLIGHT).isReady());
		assertTrue(timers.get(PotionEffect.DIVINE_DEFENCE).isActive());
		assertEquals(300, remaining(PotionEffect.DIVINE_DEFENCE, 1), DELTA);
	}

	@Test
	public void chatOnlyEffectsStayHiddenUntilConfirmed()
	{
		timers.initialize(Map.of(), START);
		timers.tick(Map.of(), atTick(1));
		assertUnavailable(PotionEffect.SILK_DRESSING);
		assertUnavailable(PotionEffect.BLESSED_CRYSTAL_SCARAB);
	}

	@Test
	public void confirmationBeforeFirstSampleSurvivesInitializationAndLastsFullDuration()
	{
		timers.onConfirmedUse(PotionEffect.SILK_DRESSING, START);
		timers.onConfirmedUse(PotionEffect.BLESSED_CRYSTAL_SCARAB, START);
		timers.tick(Map.of(), START);
		assertEquals(60, remaining(PotionEffect.SILK_DRESSING, 0), DELTA);
		assertEquals(24, remaining(PotionEffect.BLESSED_CRYSTAL_SCARAB, 0), DELTA);
		for (int tick = 1; tick < 40; tick++)
		{
			timers.tick(Map.of(), atTick(tick));
		}
		assertEquals(0.6, remaining(PotionEffect.BLESSED_CRYSTAL_SCARAB, 39), DELTA);
		timers.tick(Map.of(), atTick(40));
		assertTrue(timers.get(PotionEffect.BLESSED_CRYSTAL_SCARAB).isReady());
		assertFalse(timers.get(PotionEffect.BLESSED_CRYSTAL_SCARAB).isActive());
		assertTrue(timers.get(PotionEffect.SILK_DRESSING).isActive());
		for (int tick = 41; tick <= 100; tick++)
		{
			timers.tick(Map.of(), atTick(tick));
		}
		assertTrue(timers.get(PotionEffect.SILK_DRESSING).isReady());
		assertEquals(0, timers.get(PotionEffect.SILK_DRESSING).getFinishedAgeSeconds(atTick(100)), DELTA);
	}

	@Test
	public void explicitInitializationAlsoRetainsPendingConfirmation()
	{
		timers.onConfirmedUse(PotionEffect.SILK_DRESSING, START);
		timers.initialize(Map.of(), START);
		assertEquals(60, remaining(PotionEffect.SILK_DRESSING, 0), DELTA);
	}

	@Test
	public void redosingChatOnlyEffectRestartsOnlyItsOwnDuration()
	{
		timers.onConfirmedUse(PotionEffect.SILK_DRESSING, START);
		timers.onConfirmedUse(PotionEffect.BLESSED_CRYSTAL_SCARAB, START);
		timers.tick(Map.of(), START);
		for (int tick = 1; tick < 20; tick++)
		{
			timers.tick(Map.of(), atTick(tick));
		}
		timers.onConfirmedUse(PotionEffect.BLESSED_CRYSTAL_SCARAB, atTick(20));
		timers.tick(Map.of(), atTick(20));
		assertEquals(24, remaining(PotionEffect.BLESSED_CRYSTAL_SCARAB, 20), DELTA);
		assertEquals(48, remaining(PotionEffect.SILK_DRESSING, 20), DELTA);
		for (int tick = 21; tick <= 60; tick++)
		{
			timers.tick(Map.of(), atTick(tick));
		}
		assertTrue(timers.get(PotionEffect.BLESSED_CRYSTAL_SCARAB).isReady());
		assertTrue(timers.get(PotionEffect.SILK_DRESSING).isActive());
	}

	@Test
	public void deathClearsConfirmedEffectsAndTheirPendingMessages()
	{
		timers.onConfirmedUse(PotionEffect.SILK_DRESSING, START);
		timers.onConfirmedUse(PotionEffect.BLESSED_CRYSTAL_SCARAB, START);
		timers.clearOnDeath();
		timers.tick(Map.of(PotionEffect.SURGE, 50), START);
		assertUnavailable(PotionEffect.SILK_DRESSING);
		assertUnavailable(PotionEffect.BLESSED_CRYSTAL_SCARAB);
		assertTrue(timers.get(PotionEffect.SURGE).isActive());
		timers.onConfirmedUse(PotionEffect.SILK_DRESSING, atTick(1));
		timers.tick(Map.of(PotionEffect.SURGE, 50), atTick(1));
		timers.clearOnDeath();
		assertUnavailable(PotionEffect.SILK_DRESSING);
		assertTrue(timers.get(PotionEffect.SURGE).isActive());
	}

	@Test
	public void lifecycleClearDoesNotResurrectAConfirmedEffectOnTheNextSample()
	{
		timers.onConfirmedUse(PotionEffect.SILK_DRESSING, START);
		timers.clear();
		timers.tick(Map.of(), atTick(1));
		assertUnavailable(PotionEffect.SILK_DRESSING);
		assertUnavailable(PotionEffect.BLESSED_CRYSTAL_SCARAB);
	}

	private void assertUnavailable(PotionEffect effect)
	{
		assertFalse(effect.name(), timers.get(effect).isActive());
		assertFalse(effect.name(), timers.get(effect).isReady());
	}

	private double remaining(PotionEffect effect, int tick)
	{
		return timers.get(effect).getRemainingSeconds(atTick(tick));
	}

	private static Map<PotionEffect, Integer> poison(int raw)
	{
		return Map.of(PotionEffect.ANTIPOISON, raw, PotionEffect.ANTIVENOM, raw);
	}

	private static long atTick(int tick)
	{
		return START + tick * Cooldown.TICK_NANOS;
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
}
