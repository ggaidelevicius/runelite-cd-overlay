package com.cooldownoverlays;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;
import net.runelite.api.gameval.ItemID;

final class PotionItems
{
	// Dose and mix variants share a setting, while extended potions keep their own setting.
	enum Family
	{
		SURGE(PotionEffect.SURGE, CooldownOverlaysConfig::surgePotion,
			ItemID._4DOSESURGE, ItemID._3DOSESURGE, ItemID._2DOSESURGE, ItemID._1DOSESURGE),
		WEAK_OVERLOAD(PotionEffect.OVERLOAD, CooldownOverlaysConfig::weakOverload,
			ItemID.RAIDS_VIAL_OVERLOAD_WEAK_4, ItemID.RAIDS_VIAL_OVERLOAD_WEAK_3,
			ItemID.RAIDS_VIAL_OVERLOAD_WEAK_2, ItemID.RAIDS_VIAL_OVERLOAD_WEAK_1),
		OVERLOAD(PotionEffect.OVERLOAD, CooldownOverlaysConfig::overload,
			ItemID.RAIDS_VIAL_OVERLOAD_4, ItemID.RAIDS_VIAL_OVERLOAD_3,
			ItemID.RAIDS_VIAL_OVERLOAD_2, ItemID.RAIDS_VIAL_OVERLOAD_1),
		STRONG_OVERLOAD(PotionEffect.OVERLOAD, CooldownOverlaysConfig::strongOverload,
			ItemID.RAIDS_VIAL_OVERLOAD_STRONG_4, ItemID.RAIDS_VIAL_OVERLOAD_STRONG_3,
			ItemID.RAIDS_VIAL_OVERLOAD_STRONG_2, ItemID.RAIDS_VIAL_OVERLOAD_STRONG_1),
		WEAK_PRAYER_ENHANCE(PotionEffect.PRAYER_ENHANCE, CooldownOverlaysConfig::weakPrayerEnhance,
			ItemID.RAIDS_VIAL_PRAYER_WEAK_4, ItemID.RAIDS_VIAL_PRAYER_WEAK_3,
			ItemID.RAIDS_VIAL_PRAYER_WEAK_2, ItemID.RAIDS_VIAL_PRAYER_WEAK_1),
		PRAYER_ENHANCE(PotionEffect.PRAYER_ENHANCE, CooldownOverlaysConfig::prayerEnhance,
			ItemID.RAIDS_VIAL_PRAYER_4, ItemID.RAIDS_VIAL_PRAYER_3,
			ItemID.RAIDS_VIAL_PRAYER_2, ItemID.RAIDS_VIAL_PRAYER_1),
		STRONG_PRAYER_ENHANCE(PotionEffect.PRAYER_ENHANCE, CooldownOverlaysConfig::strongPrayerEnhance,
			ItemID.RAIDS_VIAL_PRAYER_STRONG_4, ItemID.RAIDS_VIAL_PRAYER_STRONG_3,
			ItemID.RAIDS_VIAL_PRAYER_STRONG_2, ItemID.RAIDS_VIAL_PRAYER_STRONG_1),
		SMELLING_SALTS(PotionEffect.SMELLING_SALTS, CooldownOverlaysConfig::smellingSalts,
			ItemID.TOA_SUPPLY_STATS_2, ItemID.TOA_SUPPLY_STATS_1),
		LIQUID_ADRENALINE(PotionEffect.LIQUID_ADRENALINE, CooldownOverlaysConfig::liquidAdrenaline,
			ItemID.TOA_SUPPLY_ENERGY_2, ItemID.TOA_SUPPLY_ENERGY_1),
		STAMINA(PotionEffect.STAMINA, CooldownOverlaysConfig::staminaPotion,
			ItemID._4DOSESTAMINA, ItemID._3DOSESTAMINA, ItemID._2DOSESTAMINA, ItemID._1DOSESTAMINA,
			ItemID.BRUTAL_2DOSESTAMINA, ItemID.BRUTAL_1DOSESTAMINA,
			ItemID.BR_4DOSESTAMINA, ItemID.BR_3DOSESTAMINA, ItemID.BR_2DOSESTAMINA, ItemID.BR_1DOSESTAMINA),
		EXTENDED_STAMINA(PotionEffect.STAMINA, CooldownOverlaysConfig::extendedStaminaPotion,
			ItemID._4DOSE2STAMINA, ItemID._3DOSE2STAMINA, ItemID._2DOSE2STAMINA, ItemID._1DOSE2STAMINA),
		ANTIFIRE(PotionEffect.ANTIFIRE, CooldownOverlaysConfig::antifirePotion,
			ItemID._4DOSE1ANTIDRAGON, ItemID._3DOSE1ANTIDRAGON, ItemID._2DOSE1ANTIDRAGON, ItemID._1DOSE1ANTIDRAGON,
			ItemID.BRUTAL_2DOSE1ANTIDRAGON, ItemID.BRUTAL_1DOSE1ANTIDRAGON),
		EXTENDED_ANTIFIRE(PotionEffect.ANTIFIRE, CooldownOverlaysConfig::extendedAntifirePotion,
			ItemID._4DOSE2ANTIDRAGON, ItemID._3DOSE2ANTIDRAGON, ItemID._2DOSE2ANTIDRAGON, ItemID._1DOSE2ANTIDRAGON,
			ItemID.BRUTAL_2DOSE2ANTIDRAGON, ItemID.BRUTAL_1DOSE2ANTIDRAGON),
		SUPER_ANTIFIRE(PotionEffect.SUPER_ANTIFIRE, CooldownOverlaysConfig::superAntifirePotion,
			ItemID._4DOSE3ANTIDRAGON, ItemID._3DOSE3ANTIDRAGON, ItemID._2DOSE3ANTIDRAGON, ItemID._1DOSE3ANTIDRAGON,
			ItemID.BRUTAL_2DOSE3ANTIDRAGON, ItemID.BRUTAL_1DOSE3ANTIDRAGON),
		EXTENDED_SUPER_ANTIFIRE(PotionEffect.SUPER_ANTIFIRE, CooldownOverlaysConfig::extendedSuperAntifirePotion,
			ItemID._4DOSE4ANTIDRAGON, ItemID._3DOSE4ANTIDRAGON, ItemID._2DOSE4ANTIDRAGON, ItemID._1DOSE4ANTIDRAGON,
			ItemID.BRUTAL_2DOSE4ANTIDRAGON, ItemID.BRUTAL_1DOSE4ANTIDRAGON),
		ANTIVENOM(PotionEffect.ANTIVENOM, CooldownOverlaysConfig::antiVenom,
			ItemID.ANTIVENOM4, ItemID.ANTIVENOM3, ItemID.ANTIVENOM2, ItemID.ANTIVENOM1),
		ANTIVENOM_PLUS(PotionEffect.ANTIVENOM, CooldownOverlaysConfig::antiVenomPlus,
			ItemID.ANTIVENOM_4, ItemID.ANTIVENOM_3, ItemID.ANTIVENOM_2, ItemID.ANTIVENOM_1),
		EXTENDED_ANTIVENOM_PLUS(PotionEffect.ANTIVENOM, CooldownOverlaysConfig::extendedAntiVenomPlus,
			ItemID.EXTENDED_ANTIVENOM_4, ItemID.EXTENDED_ANTIVENOM_3,
			ItemID.EXTENDED_ANTIVENOM_2, ItemID.EXTENDED_ANTIVENOM_1),
		DIVINE_ATTACK(PotionEffect.DIVINE_ATTACK, CooldownOverlaysConfig::divineSuperAttack,
			ItemID._4DOSEDIVINEATTACK, ItemID._3DOSEDIVINEATTACK, ItemID._2DOSEDIVINEATTACK, ItemID._1DOSEDIVINEATTACK),
		DIVINE_STRENGTH(PotionEffect.DIVINE_STRENGTH, CooldownOverlaysConfig::divineSuperStrength,
			ItemID._4DOSEDIVINESTRENGTH, ItemID._3DOSEDIVINESTRENGTH,
			ItemID._2DOSEDIVINESTRENGTH, ItemID._1DOSEDIVINESTRENGTH),
		DIVINE_DEFENCE(PotionEffect.DIVINE_DEFENCE, CooldownOverlaysConfig::divineSuperDefence,
			ItemID._4DOSEDIVINEDEFENCE, ItemID._3DOSEDIVINEDEFENCE, ItemID._2DOSEDIVINEDEFENCE, ItemID._1DOSEDIVINEDEFENCE),
		DIVINE_RANGING(PotionEffect.DIVINE_RANGING, CooldownOverlaysConfig::divineRanging,
			ItemID._4DOSEDIVINERANGE, ItemID._3DOSEDIVINERANGE, ItemID._2DOSEDIVINERANGE, ItemID._1DOSEDIVINERANGE),
		DIVINE_MAGIC(PotionEffect.DIVINE_MAGIC, CooldownOverlaysConfig::divineMagic,
			ItemID._4DOSEDIVINEMAGIC, ItemID._3DOSEDIVINEMAGIC, ItemID._2DOSEDIVINEMAGIC, ItemID._1DOSEDIVINEMAGIC),
		DIVINE_COMBAT(PotionEffect.DIVINE_COMBAT, CooldownOverlaysConfig::divineSuperCombat,
			ItemID._4DOSEDIVINECOMBAT, ItemID._3DOSEDIVINECOMBAT, ItemID._2DOSEDIVINECOMBAT, ItemID._1DOSEDIVINECOMBAT),
		DIVINE_BASTION(PotionEffect.DIVINE_BASTION, CooldownOverlaysConfig::divineBastion,
			ItemID._4DOSEDIVINEBASTION, ItemID._3DOSEDIVINEBASTION, ItemID._2DOSEDIVINEBASTION, ItemID._1DOSEDIVINEBASTION),
		DIVINE_BATTLEMAGE(PotionEffect.DIVINE_BATTLEMAGE, CooldownOverlaysConfig::divineBattlemage,
			ItemID._4DOSEDIVINEBATTLEMAGE, ItemID._3DOSEDIVINEBATTLEMAGE,
			ItemID._2DOSEDIVINEBATTLEMAGE, ItemID._1DOSEDIVINEBATTLEMAGE);

		private final PotionEffect effect;
		private final Predicate<CooldownOverlaysConfig> enabled;
		private final int[] itemIds;

		Family(PotionEffect effect, Predicate<CooldownOverlaysConfig> enabled, int... itemIds)
		{
			this.effect = effect;
			this.enabled = enabled;
			this.itemIds = itemIds;
		}

		PotionEffect getEffect()
		{
			return effect;
		}

		boolean isEnabled(CooldownOverlaysConfig config)
		{
			return enabled.test(config);
		}
	}

	private static final Map<Integer, Family> FAMILIES;

	static
	{
		Map<Integer, Family> families = new HashMap<>();
		for (Family family : Family.values())
		{
			for (int itemId : family.itemIds)
			{
				if (families.put(itemId, family) != null)
				{
					throw new IllegalStateException("Potion item belongs to multiple families: " + itemId);
				}
			}
		}
		FAMILIES = Collections.unmodifiableMap(families);
	}

	private PotionItems()
	{
	}

	static Family getFamily(int itemId)
	{
		return FAMILIES.get(itemId);
	}

	static PotionEffect getEffect(int itemId)
	{
		Family family = getFamily(itemId);
		return family == null ? null : family.getEffect();
	}
}
