package com.cooldownoverlays;

import java.lang.reflect.Proxy;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import net.runelite.api.Client;
import net.runelite.api.Skill;
import net.runelite.api.gameval.VarbitID;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BoostPotionClientTest
{
	private final Map<Integer, Integer> varbits = new HashMap<>();
	private final Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
	private final BoostPotionTimers timers = new BoostPotionTimers();
	private final CooldownOverlaysConfig config = new CooldownOverlaysConfig()
	{
		@Override public int attackThreshold() { return 103; }
		@Override public int strengthThreshold() { return 103; }
		@Override public int defenceThreshold() { return 103; }
		@Override public int rangedThreshold() { return 103; }
		@Override public int magicThreshold() { return 103; }
	};
	private int tick;
	private final Client client = (Client) Proxy.newProxyInstance(Client.class.getClassLoader(),
		new Class<?>[]{Client.class}, (proxy, method, args) ->
		{
			switch (method.getName())
			{
				case "getRealSkillLevel": return 99;
				case "getBoostedSkillLevel": return levels.getOrDefault(args[0], 99);
				case "getTickCount": return tick;
				case "getVarbitValue": return varbits.getOrDefault(args[0], 0);
				default: throw new UnsupportedOperationException(method.getName());
			}
		});

	@Test
	public void divineProtectionSuppressesOnlyTheRelevantSkills()
	{
		varbits.put(VarbitID.DIVINEATTACK_POTION_TIME, 500);
		timers.tick(client, config, 0);
		assertFalse(timers.get(BoostPotion.SUPER_ATTACK).isReady());
		assertFalse(timers.get(BoostPotion.SUPER_ATTACK).isActive());
		assertTrue(timers.get(BoostPotion.SUPER_STRENGTH).isReady());
		assertTrue(timers.get(BoostPotion.SUPER_COMBAT).isReady());
		varbits.put(VarbitID.DIVINEATTACK_POTION_TIME, 0);
		tick++;
		timers.tick(client, config, tick * Cooldown.TICK_NANOS);
		assertTrue(timers.get(BoostPotion.SUPER_ATTACK).isReady());
	}

	@Test
	public void compoundDivineProtectionCoversEveryAffectedStat()
	{
		varbits.put(VarbitID.DIVINECOMBAT_POTION_TIME, 500);
		varbits.put(VarbitID.DIVINEBASTION_POTION_TIME, 500);
		varbits.put(VarbitID.DIVINEBATTLEMAGE_POTION_TIME, 500);
		timers.tick(client, config, 0);
		for (BoostPotion potion : BoostPotion.values())
		{
			assertFalse(potion.name(), timers.get(potion).isReady());
			assertFalse(potion.name(), timers.get(potion).isActive());
		}
	}

	@Test
	public void raidOverloadProtectionRequiresBeingInsideChambers()
	{
		varbits.put(VarbitID.RAIDS_OVERLOAD_TIMER, 20);
		varbits.put(VarbitID.RAIDS_CLIENT_INDUNGEON, 1);
		timers.tick(client, config, 0);
		assertAllUnavailable();
		varbits.put(VarbitID.RAIDS_CLIENT_INDUNGEON, 0);
		tick++;
		timers.tick(client, config, tick * Cooldown.TICK_NANOS);
		assertTrue(timers.get(BoostPotion.SUPER_COMBAT).isReady());
	}

	@Test
	public void saltsNightmareZoneAndBlightedOverloadsMaintainCombatBoosts()
	{
		int[] effects = {VarbitID.TOA_MIDRAIDLOOT_STATS_TIMER,
			VarbitID.NZONE_OVERLOAD_POTION_EFFECTS, VarbitID.DEADMAN_OVERLOAD_POTION_EFFECTS};
		for (int effect : effects)
		{
			timers.clear();
			varbits.clear();
			varbits.put(effect, 1);
			timers.tick(client, config, 0);
			assertAllUnavailable();
		}
	}

	@Test
	public void heartSizedMagicBoostDoesNotAdvertiseANormalMagicDose()
	{
		varbits.put(VarbitID.IMBUED_HEART_TIMER, 50);
		levels.put(Skill.MAGIC, 112);
		timers.tick(client, config, 0);
		assertFalse(timers.get(BoostPotion.MAGIC).isReady());
		assertFalse(timers.get(BoostPotion.MAGIC).isActive());
		assertTrue(timers.get(BoostPotion.SUPER_ATTACK).isReady());
	}

	@Test
	public void preserveVarbitExtendsLearnedDecayEstimate()
	{
		varbits.put(VarbitID.PRAYER_PRESERVE, 1);
		levels.put(Skill.ATTACK, 119);
		timers.tick(client, config, 0);
		levels.put(Skill.ATTACK, 118);
		tick = 100;
		timers.tick(client, config, tick * Cooldown.TICK_NANOS);
		assertEquals(1440, timers.get(BoostPotion.SUPER_ATTACK)
			.getRemainingSeconds(tick * Cooldown.TICK_NANOS), 0.00001);
	}

	@Test
	public void unconfiguredTargetsDoNotGeneratePotionPrompts()
	{
		timers.tick(client, new CooldownOverlaysConfig() {}, 0);
		assertAllUnavailable();
	}

	@Test
	public void clientReadsIndependentAbsoluteTargetsForEverySkill()
	{
		CooldownOverlaysConfig targets = new CooldownOverlaysConfig()
		{
			@Override public int attackThreshold() { return 107; }
			@Override public int strengthThreshold() { return 110; }
			@Override public int defenceThreshold() { return 105; }
			@Override public int rangedThreshold() { return 108; }
			@Override public int magicThreshold() { return 102; }
		};
		levels.put(Skill.ATTACK, 107);
		levels.put(Skill.STRENGTH, 110);
		levels.put(Skill.DEFENCE, 105);
		levels.put(Skill.RANGED, 108);
		levels.put(Skill.MAGIC, 102);
		timers.tick(client, targets, 0);
		for (BoostPotion potion : BoostPotion.values())
		{
			assertFalse(potion.name(), timers.get(potion).isReady());
			assertTrue(potion.name(), timers.get(potion).isActive());
		}

		levels.replaceAll((skill, level) -> level - 1);
		tick = 100;
		timers.tick(client, targets, tick * Cooldown.TICK_NANOS);
		for (BoostPotion potion : BoostPotion.values())
		{
			assertTrue(potion.name(), timers.get(potion).isReady());
			assertFalse(potion.name(), timers.get(potion).isActive());
		}
	}

	@Test
	public void moonlightDefenceMarkerDoesNotHideUsefulOrdinaryPotions()
	{
		varbits.put(VarbitID.MOONLIGHT_POTION_TIME, 499);
		varbits.put(VarbitID.DIVINEDEFENCE_POTION_TIME, 500);
		timers.tick(client, config, 0);
		assertTrue(timers.get(BoostPotion.SUPER_DEFENCE).isReady());
		assertTrue(timers.get(BoostPotion.DEFENCE).isReady());

		varbits.put(VarbitID.DIVINEDEFENCE_POTION_TIME, 700);
		timers.tick(client, config, Cooldown.TICK_NANOS);
		assertFalse(timers.get(BoostPotion.SUPER_DEFENCE).isReady());
		assertFalse(timers.get(BoostPotion.SUPER_DEFENCE).isActive());
	}

	@Test
	public void naturalDefenceDecayUnderMoonlightCanTeachTheSharedPhase()
	{
		varbits.put(VarbitID.MOONLIGHT_POTION_TIME, 500);
		varbits.put(VarbitID.DIVINEDEFENCE_POTION_TIME, 500);
		levels.put(Skill.DEFENCE, 119);
		timers.tick(client, config, 0);
		levels.put(Skill.DEFENCE, 118);
		tick = 100;
		timers.tick(client, config, tick * Cooldown.TICK_NANOS);
		assertTrue(timers.get(BoostPotion.SUPER_DEFENCE).isKnown());
		assertEquals(960, timers.get(BoostPotion.SUPER_DEFENCE)
			.getRemainingSeconds(tick * Cooldown.TICK_NANOS), 0.00001);
	}

	private void assertAllUnavailable()
	{
		for (BoostPotion potion : BoostPotion.values())
		{
			assertFalse(potion.name(), timers.get(potion).isReady());
			assertFalse(potion.name(), timers.get(potion).isActive());
		}
	}
}
