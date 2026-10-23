package com.cooldownoverlays;

import java.lang.reflect.Proxy;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import net.runelite.api.Client;
import net.runelite.api.Skill;
import net.runelite.api.gameval.ItemID;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RepotProtectionPolicyTest
{
	private static final long NOW = 1_000_000_000L;
	private final Map<String, Object> settings = new HashMap<>();
	private final Map<Skill, Integer> base = new EnumMap<>(Skill.class);
	private final Map<Skill, Integer> boosted = new EnumMap<>(Skill.class);
	private int energy = 10_000;
	private int tick;
	private final CooldownOverlaysConfig defaults = new CooldownOverlaysConfig() {};
	private final CooldownOverlaysConfig config = (CooldownOverlaysConfig) Proxy.newProxyInstance(
		CooldownOverlaysConfig.class.getClassLoader(), new Class<?>[]{CooldownOverlaysConfig.class},
		(proxy, method, args) -> settings.containsKey(method.getName())
			? settings.get(method.getName()) : method.invoke(defaults, args));
	private final Client client = (Client) Proxy.newProxyInstance(Client.class.getClassLoader(),
		new Class<?>[]{Client.class}, (proxy, method, args) ->
		{
			switch (method.getName())
			{
				case "getRealSkillLevel": return base.getOrDefault(args[0], 99);
				case "getBoostedSkillLevel": return boosted.getOrDefault(args[0], 200);
				case "getEnergy": return energy;
				case "getTickCount": return tick;
				case "getVarbitValue": return 0;
				default: throw new UnsupportedOperationException(method.getName());
			}
		});
	private final CooldownTracker tracker = new CooldownTracker();
	private final PotionTimers potions = new PotionTimers();
	private final BoostPotionTimers boosts = new BoostPotionTimers();
	private final RepotProtectionPolicy policy = new RepotProtectionPolicy(client, config, tracker, potions, boosts);

	@Before
	public void enableProtection()
	{
		settings.put("preventAccidentalRedosing", true);
		settings.put("strengthThreshold", 110);
	}

	@Test
	public void disabledByDefaultAndIndependentOfReadyAnimation()
	{
		tracker.getHeart().start(100, 100, NOW);
		settings.clear();
		assertFalse(config.preventAccidentalRedosing());
		assertFalse(policy.shouldProtect(ItemID.IMBUED_HEART, NOW));
		settings.put("preventAccidentalRedosing", true);
		settings.put("animateWhenReady", false);
		assertTrue(policy.shouldProtect(ItemID.IMBUED_HEART, NOW));
		settings.put("animateWhenReady", true);
		assertTrue(policy.shouldProtect(ItemID.IMBUED_HEART, NOW));
	}

	@Test
	public void heartsShareTheirActualCooldownButRespectIndependentSettings()
	{
		tracker.initialize(50, NOW);
		assertTrue(policy.shouldProtect(ItemID.IMBUED_HEART, NOW));
		assertTrue(policy.shouldProtect(ItemID.SATURATED_HEART, NOW));
		settings.put("imbuedHeart", false);
		assertFalse(policy.shouldProtect(ItemID.IMBUED_HEART, NOW));
		assertTrue(policy.shouldProtect(ItemID.SATURATED_HEART, NOW));
		settings.put("saturatedHeart", false);
		assertFalse(policy.shouldProtect(ItemID.SATURATED_HEART, NOW));
	}

	@Test
	public void onlyActiveKnownTimersBlockDoses()
	{
		Cooldown cooldown = potions.get(PotionEffect.SURGE);
		assertFalse(policy.shouldProtect(ItemID._4DOSESURGE, NOW));
		cooldown.startUnknown(NOW);
		assertFalse(policy.shouldProtect(ItemID._4DOSESURGE, NOW));
		cooldown.start(100, 100, NOW);
		assertTrue(policy.shouldProtect(ItemID._4DOSESURGE, NOW));
		cooldown.setReady(NOW);
		assertFalse(policy.shouldProtect(ItemID._4DOSESURGE, NOW));
		cooldown.clear();
		assertFalse(policy.shouldProtect(ItemID._4DOSESURGE, NOW));
	}

	@Test
	public void graceWindowIncludesTheExactBoundaryAndSmoothRemainder()
	{
		Cooldown cooldown = potions.get(PotionEffect.SURGE);
		cooldown.start(26, 500, NOW);
		assertTrue(policy.shouldProtect(ItemID._4DOSESURGE, NOW));
		assertFalse(policy.shouldProtect(ItemID._4DOSESURGE, NOW + Cooldown.TICK_NANOS));
		cooldown.start(25, 500, NOW);
		assertFalse(policy.shouldProtect(ItemID._4DOSESURGE, NOW));
		settings.put("repotGraceSeconds", 0);
		cooldown.start(1, 500, NOW);
		assertTrue(policy.shouldProtect(ItemID._4DOSESURGE, NOW));
	}

	@Test
	public void changingGraceWindowAppliesImmediately()
	{
		potions.get(PotionEffect.GOADING).start(50, 600, NOW);
		assertTrue(policy.shouldProtect(ItemID._4DOSEGOADING, NOW));
		settings.put("repotGraceSeconds", 30);
		assertFalse(policy.shouldProtect(ItemID._4DOSEGOADING, NOW));
		settings.put("repotGraceSeconds", 29);
		assertTrue(policy.shouldProtect(ItemID._4DOSEGOADING, NOW));
	}

	@Test
	public void enabledTimedFamiliesUseTheirEffectAndOwnCheckbox()
	{
		activateTimers();
		Object[][] families = {
			{ItemID._4DOSESURGE, "surgePotion"},
			{ItemID.NZONE4DOSEOVERLOADPOTION, "nmzOverload"},
			{ItemID.DEADMAN4DOSEOVERLOAD, "blightedOverload"},
			{ItemID.RAIDS_VIAL_OVERLOAD_WEAK_4, "weakOverload"},
			{ItemID.RAIDS_VIAL_OVERLOAD_4, "overload"},
			{ItemID.RAIDS_VIAL_OVERLOAD_STRONG_4, "strongOverload"},
			{ItemID.RAIDS_VIAL_PRAYER_WEAK_4, "weakPrayerEnhance"},
			{ItemID.RAIDS_VIAL_PRAYER_4, "prayerEnhance"},
			{ItemID.RAIDS_VIAL_PRAYER_STRONG_4, "strongPrayerEnhance"},
			{ItemID.RAIDS_VIAL_ANTIPOISON_WEAK_4, "weakRaidAntipoison"},
			{ItemID.RAIDS_VIAL_ANTIPOISON_4, "raidAntipoison"},
			{ItemID.RAIDS_VIAL_ANTIPOISON_STRONG_4, "strongRaidAntipoison"},
			{ItemID.TOA_SUPPLY_STATS_2, "smellingSalts"},
			{ItemID.TOA_SUPPLY_ENERGY_2, "liquidAdrenaline"},
			{ItemID._4DOSESTAMINA, "staminaPotion"},
			{ItemID._4DOSE2STAMINA, "extendedStaminaPotion"},
			{ItemID._4DOSE1ANTIDRAGON, "antifirePotion"},
			{ItemID._4DOSE2ANTIDRAGON, "extendedAntifirePotion"},
			{ItemID._4DOSE3ANTIDRAGON, "superAntifirePotion"},
			{ItemID._4DOSE4ANTIDRAGON, "extendedSuperAntifirePotion"},
			{ItemID._4DOSEANTIPOISON, "antipoison"},
			{ItemID._4DOSE2ANTIPOISON, "superAntipoison"},
			{ItemID.ANTIDOTE_4, "antidotePlus"},
			{ItemID.ANTIDOTE__4, "antidotePlusPlus"},
			{ItemID.ANTIVENOM4, "antiVenom"},
			{ItemID.ANTIVENOM_4, "antiVenomPlus"},
			{ItemID.EXTENDED_ANTIVENOM_4, "extendedAntiVenomPlus"},
			{ItemID._4DOSEGOADING, "goadingPotion"},
			{ItemID._4DOSESTATRENEWAL, "menaphiteRemedy"},
			{ItemID._4DOSE1PRAYER_REGENERATION, "prayerRegenerationPotion"},
			{ItemID._4DOSEDIVINEATTACK, "divineSuperAttack"},
			{ItemID._4DOSEDIVINESTRENGTH, "divineSuperStrength"},
			{ItemID._4DOSEDIVINEDEFENCE, "divineSuperDefence"},
			{ItemID._4DOSEDIVINERANGE, "divineRanging"},
			{ItemID._4DOSEDIVINEMAGIC, "divineMagic"},
			{ItemID._4DOSEDIVINECOMBAT, "divineSuperCombat"},
			{ItemID._4DOSEDIVINEBASTION, "divineBastion"},
			{ItemID._4DOSEDIVINEBATTLEMAGE, "divineBattlemage"}
		};
		for (Object[] family : families)
		{
			int item = (int) family[0];
			String setting = (String) family[1];
			assertTrue(setting, policy.shouldProtect(item, NOW));
			settings.put(setting, false);
			assertFalse(setting, policy.shouldProtect(item, NOW));
			settings.remove(setting);
		}
	}

	@Test
	public void strongerProtectionPotionsDoNotBorrowWeakerEffectTimers()
	{
		potions.get(PotionEffect.ANTIFIRE).start(300, 600, NOW);
		potions.get(PotionEffect.ANTIPOISON).start(300, 600, NOW);
		assertTrue(policy.shouldProtect(ItemID._4DOSE1ANTIDRAGON, NOW));
		assertFalse(policy.shouldProtect(ItemID._4DOSE3ANTIDRAGON, NOW));
		assertTrue(policy.shouldProtect(ItemID._4DOSEANTIPOISON, NOW));
		assertFalse(policy.shouldProtect(ItemID.ANTIVENOM_4, NOW));
		potions.get(PotionEffect.SUPER_ANTIFIRE).start(100, 300, NOW);
		potions.get(PotionEffect.ANTIVENOM).start(100, 300, NOW);
		assertTrue(policy.shouldProtect(ItemID._4DOSE3ANTIDRAGON, NOW));
		assertTrue(policy.shouldProtect(ItemID.ANTIVENOM_4, NOW));
	}

	@Test
	public void healingAndRestorationSuppliesRemainUsableWithActiveEffectTimers()
	{
		activateTimers();
		int[] restorativeItems = {
			ItemID.SANFEW_SALVE_4_DOSE, ItemID.SANFEW_SALVE_3_DOSE,
			ItemID.SANFEW_SALVE_2_DOSE, ItemID.SANFEW_SALVE_1_DOSE,
			ItemID.BR_SANFEW_SALVE_4_DOSE, ItemID.BR_SANFEW_SALVE_3_DOSE,
			ItemID.BR_SANFEW_SALVE_2_DOSE, ItemID.BR_SANFEW_SALVE_1_DOSE,
			ItemID._4DOSEMOONLIGHTPOTION, ItemID._3DOSEMOONLIGHTPOTION,
			ItemID._2DOSEMOONLIGHTPOTION, ItemID._1DOSEMOONLIGHTPOTION,
			ItemID.TOA_SUPPLY_HEAL_OVERTIME_2, ItemID.TOA_SUPPLY_HEAL_OVERTIME_1,
			ItemID.TOA_SUPPLY_PRAYER_OVERTIME_2, ItemID.TOA_SUPPLY_PRAYER_OVERTIME_1,
			ItemID.DT2_SCAR_MAZE_STAMINA
		};
		for (int item : restorativeItems)
		{
			assertFalse("Restorative item " + item, policy.shouldProtect(item, NOW));
		}
		assertFalse(policy.shouldProtect(-1, NOW));
		assertFalse(policy.shouldProtect(385, NOW)); // Shark: unrelated food is never intercepted.
	}

	@Test
	public void allSupportedBarbarianMixDosesRemainUsableForHealing()
	{
		activateTimers();
		int[] mixes = {
			ItemID.BRUTAL_2DOSE1ATTACK, ItemID.BRUTAL_1DOSE1ATTACK,
			ItemID.BRUTAL_2DOSE1STRENGTH, ItemID.BRUTAL_1DOSE1STRENGTH,
			ItemID.BRUTAL_2DOSE1DEFENSE, ItemID.BRUTAL_1DOSE1DEFENSE,
			ItemID.BRUTAL_2DOSE2ATTACK, ItemID.BRUTAL_1DOSE2ATTACK,
			ItemID.BRUTAL_2DOSE2STRENGTH, ItemID.BRUTAL_1DOSE2STRENGTH,
			ItemID.BRUTAL_2DOSE2DEFENSE, ItemID.BRUTAL_1DOSE2DEFENSE,
			ItemID.BRUTAL_2DOSECOMBAT, ItemID.BRUTAL_1DOSECOMBAT,
			ItemID.BRUTAL_2DOSERANGERSPOTION, ItemID.BRUTAL_1DOSERANGERSPOTION,
			ItemID.BRUTAL_2DOSE1MAGIC, ItemID.BRUTAL_1DOSE1MAGIC,
			ItemID.BRUTAL_2DOSESTAMINA, ItemID.BRUTAL_1DOSESTAMINA,
			ItemID.BRUTAL_2DOSE1ANTIDRAGON, ItemID.BRUTAL_1DOSE1ANTIDRAGON,
			ItemID.BRUTAL_2DOSE2ANTIDRAGON, ItemID.BRUTAL_1DOSE2ANTIDRAGON,
			ItemID.BRUTAL_2DOSE3ANTIDRAGON, ItemID.BRUTAL_1DOSE3ANTIDRAGON,
			ItemID.BRUTAL_2DOSE4ANTIDRAGON, ItemID.BRUTAL_1DOSE4ANTIDRAGON,
			ItemID.BRUTAL_2DOSEANTIPOISON, ItemID.BRUTAL_1DOSEANTIPOISON,
			ItemID.BRUTAL_2DOSE2ANTIPOISON, ItemID.BRUTAL_1DOSE2ANTIPOISON,
			ItemID.BRUTAL_ANTIDOTE_2, ItemID.BRUTAL_ANTIDOTE_1
		};
		for (int item : mixes)
		{
			assertFalse("Healing mix " + item, policy.shouldProtect(item, NOW));
		}
	}

	@Test
	public void staminaRemainsUsableWheneverItCanRestoreRunEnergy()
	{
		potions.get(PotionEffect.STAMINA).start(200, 200, NOW);
		int[] items = {ItemID._4DOSESTAMINA, ItemID._1DOSESTAMINA,
			ItemID._4DOSE2STAMINA, ItemID._1DOSE2STAMINA, ItemID.BR_4DOSESTAMINA};
		for (int item : items)
		{
			energy = 10_000;
			assertTrue(policy.shouldProtect(item, NOW));
			energy = 9_999;
			assertFalse(policy.shouldProtect(item, NOW));
			energy = 0;
			assertFalse(policy.shouldProtect(item, NOW));
		}
	}

	@Test
	public void ordinaryPotionsRequireConfiguredTargetsAndKnownEstimates()
	{
		Cooldown timer = boosts.get(BoostPotion.SUPER_STRENGTH);
		timer.start(100, 100, NOW);
		assertTrue(policy.shouldProtect(ItemID._4DOSE2STRENGTH, NOW));
		settings.put("strengthThreshold", 0);
		assertFalse(policy.shouldProtect(ItemID._4DOSE2STRENGTH, NOW));
		settings.put("strengthThreshold", 110);
		timer.startUnknown(NOW);
		assertFalse(policy.shouldProtect(ItemID._4DOSE2STRENGTH, NOW));
		timer.setReady(NOW);
		assertFalse(policy.shouldProtect(ItemID._4DOSE2STRENGTH, NOW));
	}

	@Test
	public void liveOrdinarySkillDropImmediatelyAllowsAnOtherwiseProtectedDose()
	{
		boosts.get(BoostPotion.SUPER_STRENGTH).start(100, 100, NOW);
		boosted.put(Skill.STRENGTH, 110);
		assertTrue(policy.shouldProtect(ItemID._4DOSE2STRENGTH, NOW));
		boosted.put(Skill.STRENGTH, 109);
		assertFalse(policy.shouldProtect(ItemID._4DOSE2STRENGTH, NOW));
		assertTrue(boosts.get(BoostPotion.SUPER_STRENGTH).isActive());
		assertTrue(boosts.get(BoostPotion.SUPER_STRENGTH).isKnown());
	}

	@Test
	public void protectionUsesTheExistingDecayEstimateAndAbsoluteTarget()
	{
		tick = 99;
		boosted.put(Skill.STRENGTH, 112);
		boosts.tick(client, config, NOW + tick * Cooldown.TICK_NANOS);
		assertFalse(policy.shouldProtect(ItemID._4DOSE2STRENGTH, NOW + tick * Cooldown.TICK_NANOS));
		tick = 100;
		boosted.put(Skill.STRENGTH, 111);
		boosts.tick(client, config, NOW + tick * Cooldown.TICK_NANOS);
		assertFalse(policy.shouldProtect(ItemID._4DOSE2STRENGTH, NOW + tick * Cooldown.TICK_NANOS));
		tick = 199;
		boosts.tick(client, config, NOW + tick * Cooldown.TICK_NANOS);
		tick = 200;
		boosted.put(Skill.STRENGTH, 110);
		long confirmedNow = NOW + tick * Cooldown.TICK_NANOS;
		boosts.tick(client, config, confirmedNow);
		assertTrue(policy.shouldProtect(ItemID._4DOSE2STRENGTH, confirmedNow));
		settings.put("repotGraceSeconds", 60);
		assertFalse(policy.shouldProtect(ItemID._4DOSE2STRENGTH, confirmedNow));
		settings.put("repotGraceSeconds", 15);
		boosted.put(Skill.STRENGTH, 109);
		assertFalse(policy.shouldProtect(ItemID._4DOSE2STRENGTH, confirmedNow));
	}

	@Test
	public void everyOrdinaryFamilyUsesItsOwnEnabledCheckbox()
	{
		activateTimers();
		settings.put("attackThreshold", 110);
		settings.put("defenceThreshold", 110);
		settings.put("rangedThreshold", 110);
		settings.put("magicThreshold", 103);
		Object[][] families = {
			{ItemID._4DOSE1ATTACK, "attackPotion"},
			{ItemID.STRENGTH4, "strengthPotion"},
			{ItemID._4DOSE1DEFENSE, "defencePotion"},
			{ItemID._4DOSE2ATTACK, "superAttackPotion"},
			{ItemID._4DOSE2STRENGTH, "superStrengthPotion"},
			{ItemID._4DOSE2DEFENSE, "superDefencePotion"},
			{ItemID._4DOSECOMBAT, "combatPotion"},
			{ItemID._4DOSE2COMBAT, "superCombatPotion"},
			{ItemID._4DOSERANGERSPOTION, "rangingPotion"},
			{ItemID._4DOSE1MAGIC, "magicPotion"},
			{ItemID._4DOSEBASTION, "bastionPotion"},
			{ItemID._4DOSEBATTLEMAGE, "battlemagePotion"}
		};
		for (Object[] family : families)
		{
			int item = (int) family[0];
			String setting = (String) family[1];
			assertTrue(setting, policy.shouldProtect(item, NOW));
			settings.put(setting, false);
			assertFalse(setting, policy.shouldProtect(item, NOW));
			settings.remove(setting);
		}
	}

	@Test
	public void combinedOrdinaryPotionAllowsAnyUsefulConfiguredSkill()
	{
		boosts.get(BoostPotion.SUPER_COMBAT).start(100, 100, NOW);
		boosted.put(Skill.STRENGTH, 110);
		boosted.put(Skill.ATTACK, 70);
		assertTrue(policy.shouldProtect(ItemID._4DOSE2COMBAT, NOW));
		settings.put("attackThreshold", 110);
		assertFalse(policy.shouldProtect(ItemID._4DOSE2COMBAT, NOW));
		boosted.put(Skill.ATTACK, 110);
		assertTrue(policy.shouldProtect(ItemID._4DOSE2COMBAT, NOW));
		boosted.put(Skill.MAGIC, 1);
		settings.put("magicThreshold", 103);
		assertTrue(policy.shouldProtect(ItemID._4DOSE2COMBAT, NOW));
	}

	@Test
	public void ordinaryPotionMustActuallyImproveTheStatEvenWithHighTarget()
	{
		boosts.get(BoostPotion.STRENGTH).start(100, 100, NOW);
		boosts.get(BoostPotion.SUPER_STRENGTH).start(100, 100, NOW);
		settings.put("strengthThreshold", 130);
		boosted.put(Skill.STRENGTH, 118);
		assertTrue(policy.shouldProtect(ItemID.STRENGTH4, NOW));
		assertTrue(policy.shouldProtect(ItemID._4DOSE2STRENGTH, NOW));
		boosted.put(Skill.STRENGTH, 117);
		assertTrue(policy.shouldProtect(ItemID.STRENGTH4, NOW));
		assertFalse(policy.shouldProtect(ItemID._4DOSE2STRENGTH, NOW));
	}

	@Test
	public void everyDivineFamilyAllowsImmediateRecoveryOfItsAffectedStats()
	{
		Object[][] cases = {
			{ItemID._4DOSEDIVINEATTACK, Skill.ATTACK, 118},
			{ItemID._4DOSEDIVINESTRENGTH, Skill.STRENGTH, 118},
			{ItemID._4DOSEDIVINEDEFENCE, Skill.DEFENCE, 118},
			{ItemID._4DOSEDIVINERANGE, Skill.RANGED, 112},
			{ItemID._4DOSEDIVINEMAGIC, Skill.MAGIC, 103},
			{ItemID._4DOSEDIVINECOMBAT, Skill.ATTACK, 118},
			{ItemID._4DOSEDIVINECOMBAT, Skill.STRENGTH, 118},
			{ItemID._4DOSEDIVINECOMBAT, Skill.DEFENCE, 118},
			{ItemID._4DOSEDIVINEBASTION, Skill.RANGED, 112},
			{ItemID._4DOSEDIVINEBASTION, Skill.DEFENCE, 118},
			{ItemID._4DOSEDIVINEBATTLEMAGE, Skill.MAGIC, 103},
			{ItemID._4DOSEDIVINEBATTLEMAGE, Skill.DEFENCE, 118}
		};
		activateTimers();
		for (Object[] dose : cases)
		{
			int item = (int) dose[0];
			Skill skill = (Skill) dose[1];
			int cap = (int) dose[2];
			boosted.clear();
			boosted.put(skill, cap);
			assertTrue("Full " + skill + " on " + item, policy.shouldProtect(item, NOW));
			boosted.put(skill, cap - 1);
			assertFalse("Drained " + skill + " on " + item, policy.shouldProtect(item, NOW));
		}
	}

	@Test
	public void divineRecoveryUsesActualBaseLevelAndIgnoresOrdinaryTargets()
	{
		potions.get(PotionEffect.DIVINE_COMBAT).start(500, 500, NOW);
		settings.put("strengthThreshold", 0);
		base.put(Skill.STRENGTH, 70);
		boosted.put(Skill.STRENGTH, 85);
		assertTrue(policy.shouldProtect(ItemID._4DOSEDIVINECOMBAT, NOW));
		boosted.put(Skill.STRENGTH, 84);
		assertFalse(policy.shouldProtect(ItemID._4DOSEDIVINECOMBAT, NOW));
	}

	@Test
	public void missingLiveSkillDataLeavesUsefulBoostDosesAvailable()
	{
		boosts.get(BoostPotion.SUPER_STRENGTH).start(100, 100, NOW);
		potions.get(PotionEffect.DIVINE_STRENGTH).start(500, 500, NOW);
		base.put(Skill.STRENGTH, 0);
		assertFalse(policy.shouldProtect(ItemID._4DOSE2STRENGTH, NOW));
		assertFalse(policy.shouldProtect(ItemID._4DOSEDIVINESTRENGTH, NOW));
	}

	private void activateTimers()
	{
		for (PotionEffect effect : PotionEffect.values())
		{
			potions.get(effect).start(2000, 2000, NOW);
		}
		for (BoostPotion potion : BoostPotion.values())
		{
			boosts.get(potion).start(2000, 2000, NOW);
		}
	}
}
