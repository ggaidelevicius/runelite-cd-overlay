package com.cooldownoverlays;

import net.runelite.api.gameval.ItemID;
import org.junit.Test;

import static com.cooldownoverlays.PotionItems.Family;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class PotionItemsTest
{
	@Test
	public void surgeAndStaminaKeepOverlaysAfterEveryDose()
	{
		assertFamily(Family.SURGE, PotionEffect.SURGE,
			ItemID._4DOSESURGE, ItemID._3DOSESURGE, ItemID._2DOSESURGE, ItemID._1DOSESURGE);
		assertFamily(Family.STAMINA, PotionEffect.STAMINA,
			ItemID._4DOSESTAMINA, ItemID._3DOSESTAMINA, ItemID._2DOSESTAMINA, ItemID._1DOSESTAMINA,
			ItemID.BRUTAL_2DOSESTAMINA, ItemID.BRUTAL_1DOSESTAMINA,
			ItemID.BR_4DOSESTAMINA, ItemID.BR_3DOSESTAMINA, ItemID.BR_2DOSESTAMINA, ItemID.BR_1DOSESTAMINA);
		assertFamily(Family.EXTENDED_STAMINA, PotionEffect.STAMINA,
			ItemID._4DOSE2STAMINA, ItemID._3DOSE2STAMINA, ItemID._2DOSE2STAMINA, ItemID._1DOSE2STAMINA);
	}

	@Test
	public void antifireIncludesEveryDoseAndMixWithoutChangingProtectionType()
	{
		assertFamily(Family.ANTIFIRE, PotionEffect.ANTIFIRE,
			ItemID._4DOSE1ANTIDRAGON, ItemID._3DOSE1ANTIDRAGON, ItemID._2DOSE1ANTIDRAGON, ItemID._1DOSE1ANTIDRAGON,
			ItemID.BRUTAL_2DOSE1ANTIDRAGON, ItemID.BRUTAL_1DOSE1ANTIDRAGON);
		assertFamily(Family.EXTENDED_ANTIFIRE, PotionEffect.ANTIFIRE,
			ItemID._4DOSE2ANTIDRAGON, ItemID._3DOSE2ANTIDRAGON, ItemID._2DOSE2ANTIDRAGON, ItemID._1DOSE2ANTIDRAGON,
			ItemID.BRUTAL_2DOSE2ANTIDRAGON, ItemID.BRUTAL_1DOSE2ANTIDRAGON);
		assertFamily(Family.SUPER_ANTIFIRE, PotionEffect.SUPER_ANTIFIRE,
			ItemID._4DOSE3ANTIDRAGON, ItemID._3DOSE3ANTIDRAGON, ItemID._2DOSE3ANTIDRAGON, ItemID._1DOSE3ANTIDRAGON,
			ItemID.BRUTAL_2DOSE3ANTIDRAGON, ItemID.BRUTAL_1DOSE3ANTIDRAGON);
		assertFamily(Family.EXTENDED_SUPER_ANTIFIRE, PotionEffect.SUPER_ANTIFIRE,
			ItemID._4DOSE4ANTIDRAGON, ItemID._3DOSE4ANTIDRAGON, ItemID._2DOSE4ANTIDRAGON, ItemID._1DOSE4ANTIDRAGON,
			ItemID.BRUTAL_2DOSE4ANTIDRAGON, ItemID.BRUTAL_1DOSE4ANTIDRAGON);
	}

	@Test
	public void raidOverloadTiersShareOneEffectForEveryDose()
	{
		assertFamily(Family.WEAK_OVERLOAD, PotionEffect.OVERLOAD,
			ItemID.RAIDS_VIAL_OVERLOAD_WEAK_4, ItemID.RAIDS_VIAL_OVERLOAD_WEAK_3,
			ItemID.RAIDS_VIAL_OVERLOAD_WEAK_2, ItemID.RAIDS_VIAL_OVERLOAD_WEAK_1);
		assertFamily(Family.OVERLOAD, PotionEffect.OVERLOAD,
			ItemID.RAIDS_VIAL_OVERLOAD_4, ItemID.RAIDS_VIAL_OVERLOAD_3,
			ItemID.RAIDS_VIAL_OVERLOAD_2, ItemID.RAIDS_VIAL_OVERLOAD_1);
		assertFamily(Family.STRONG_OVERLOAD, PotionEffect.OVERLOAD,
			ItemID.RAIDS_VIAL_OVERLOAD_STRONG_4, ItemID.RAIDS_VIAL_OVERLOAD_STRONG_3,
			ItemID.RAIDS_VIAL_OVERLOAD_STRONG_2, ItemID.RAIDS_VIAL_OVERLOAD_STRONG_1);
	}

	@Test
	public void raidPrayerEnhanceTiersShareOneEffectForEveryDose()
	{
		assertFamily(Family.WEAK_PRAYER_ENHANCE, PotionEffect.PRAYER_ENHANCE,
			ItemID.RAIDS_VIAL_PRAYER_WEAK_4, ItemID.RAIDS_VIAL_PRAYER_WEAK_3,
			ItemID.RAIDS_VIAL_PRAYER_WEAK_2, ItemID.RAIDS_VIAL_PRAYER_WEAK_1);
		assertFamily(Family.PRAYER_ENHANCE, PotionEffect.PRAYER_ENHANCE,
			ItemID.RAIDS_VIAL_PRAYER_4, ItemID.RAIDS_VIAL_PRAYER_3,
			ItemID.RAIDS_VIAL_PRAYER_2, ItemID.RAIDS_VIAL_PRAYER_1);
		assertFamily(Family.STRONG_PRAYER_ENHANCE, PotionEffect.PRAYER_ENHANCE,
			ItemID.RAIDS_VIAL_PRAYER_STRONG_4, ItemID.RAIDS_VIAL_PRAYER_STRONG_3,
			ItemID.RAIDS_VIAL_PRAYER_STRONG_2, ItemID.RAIDS_VIAL_PRAYER_STRONG_1);
	}

	@Test
	public void tombsOfAmascutSuppliesKeepOverlaysAfterTheirFirstDose()
	{
		assertFamily(Family.SMELLING_SALTS, PotionEffect.SMELLING_SALTS,
			ItemID.TOA_SUPPLY_STATS_2, ItemID.TOA_SUPPLY_STATS_1);
		assertFamily(Family.LIQUID_ADRENALINE, PotionEffect.LIQUID_ADRENALINE,
			ItemID.TOA_SUPPLY_ENERGY_2, ItemID.TOA_SUPPLY_ENERGY_1);
	}

	@Test
	public void allAntiVenomFamiliesShareVenomProtectionEffect()
	{
		assertFamily(Family.ANTIVENOM, PotionEffect.ANTIVENOM,
			ItemID.ANTIVENOM4, ItemID.ANTIVENOM3, ItemID.ANTIVENOM2, ItemID.ANTIVENOM1);
		assertFamily(Family.ANTIVENOM_PLUS, PotionEffect.ANTIVENOM,
			ItemID.ANTIVENOM_4, ItemID.ANTIVENOM_3, ItemID.ANTIVENOM_2, ItemID.ANTIVENOM_1);
		assertFamily(Family.EXTENDED_ANTIVENOM_PLUS, PotionEffect.ANTIVENOM,
			ItemID.EXTENDED_ANTIVENOM_4, ItemID.EXTENDED_ANTIVENOM_3,
			ItemID.EXTENDED_ANTIVENOM_2, ItemID.EXTENDED_ANTIVENOM_1);
	}

	@Test
	public void divinePotionsUseTheirOwnEffectForEveryDose()
	{
		assertFamily(Family.DIVINE_ATTACK, PotionEffect.DIVINE_ATTACK,
			ItemID._4DOSEDIVINEATTACK, ItemID._3DOSEDIVINEATTACK, ItemID._2DOSEDIVINEATTACK, ItemID._1DOSEDIVINEATTACK);
		assertFamily(Family.DIVINE_STRENGTH, PotionEffect.DIVINE_STRENGTH,
			ItemID._4DOSEDIVINESTRENGTH, ItemID._3DOSEDIVINESTRENGTH,
			ItemID._2DOSEDIVINESTRENGTH, ItemID._1DOSEDIVINESTRENGTH);
		assertFamily(Family.DIVINE_DEFENCE, PotionEffect.DIVINE_DEFENCE,
			ItemID._4DOSEDIVINEDEFENCE, ItemID._3DOSEDIVINEDEFENCE, ItemID._2DOSEDIVINEDEFENCE, ItemID._1DOSEDIVINEDEFENCE);
		assertFamily(Family.DIVINE_RANGING, PotionEffect.DIVINE_RANGING,
			ItemID._4DOSEDIVINERANGE, ItemID._3DOSEDIVINERANGE, ItemID._2DOSEDIVINERANGE, ItemID._1DOSEDIVINERANGE);
		assertFamily(Family.DIVINE_MAGIC, PotionEffect.DIVINE_MAGIC,
			ItemID._4DOSEDIVINEMAGIC, ItemID._3DOSEDIVINEMAGIC, ItemID._2DOSEDIVINEMAGIC, ItemID._1DOSEDIVINEMAGIC);
		assertFamily(Family.DIVINE_COMBAT, PotionEffect.DIVINE_COMBAT,
			ItemID._4DOSEDIVINECOMBAT, ItemID._3DOSEDIVINECOMBAT, ItemID._2DOSEDIVINECOMBAT, ItemID._1DOSEDIVINECOMBAT);
		assertFamily(Family.DIVINE_BASTION, PotionEffect.DIVINE_BASTION,
			ItemID._4DOSEDIVINEBASTION, ItemID._3DOSEDIVINEBASTION, ItemID._2DOSEDIVINEBASTION, ItemID._1DOSEDIVINEBASTION);
		assertFamily(Family.DIVINE_BATTLEMAGE, PotionEffect.DIVINE_BATTLEMAGE,
			ItemID._4DOSEDIVINEBATTLEMAGE, ItemID._3DOSEDIVINEBATTLEMAGE,
			ItemID._2DOSEDIVINEBATTLEMAGE, ItemID._1DOSEDIVINEBATTLEMAGE);
	}

	@Test
	public void notesPlaceholdersAndUnrelatedItemsHaveNoOverlay()
	{
		int[] unrelated = {
			-1, ItemID.IMBUED_HEART, ItemID.BLIGHTED_SACK_SURGE,
			ItemID._4DOSE2ATTACK, ItemID._4DOSEANTIPOISON,
			ItemID.Cert._4DOSESURGE, ItemID.Placeholder._4DOSESURGE,
			ItemID.Cert._4DOSESTAMINA, ItemID.Placeholder._4DOSESTAMINA,
			ItemID.Cert._4DOSEDIVINECOMBAT, ItemID.Placeholder._4DOSEDIVINECOMBAT,
			ItemID.NZONE4DOSEOVERLOADPOTION, ItemID.DEADMAN4DOSEOVERLOAD,
			ItemID.TOA_SUPPLY_PANICHEAL_2, ItemID.TOA_SUPPLY_PANICHEAL_1,
			ItemID.Cert.TOA_SUPPLY_STATS_2, ItemID.Cert.TOA_SUPPLY_ENERGY_2
		};
		for (int itemId : unrelated)
		{
			assertNull("family for " + itemId, PotionItems.getFamily(itemId));
			assertNull("effect for " + itemId, PotionItems.getEffect(itemId));
		}
	}

	@Test
	public void everyPotionFamilyIsEnabledByDefault()
	{
		CooldownOverlaysConfig config = new CooldownOverlaysConfig() {};
		for (Family family : Family.values())
		{
			assertTrue(family.name(), family.isEnabled(config));
		}
	}

	@Test
	public void sharedEffectsKeepIndependentItemSettings()
	{
		CooldownOverlaysConfig config = new CooldownOverlaysConfig()
		{
			@Override
			public boolean staminaPotion()
			{
				return false;
			}

			@Override
			public boolean extendedAntifirePotion()
			{
				return false;
			}

			@Override
			public boolean antiVenomPlus()
			{
				return false;
			}

			@Override
			public boolean weakOverload()
			{
				return false;
			}

			@Override
			public boolean strongPrayerEnhance()
			{
				return false;
			}
		};

		for (Family family : Family.values())
		{
			if (family == Family.STAMINA || family == Family.EXTENDED_ANTIFIRE || family == Family.ANTIVENOM_PLUS
				|| family == Family.WEAK_OVERLOAD || family == Family.STRONG_PRAYER_ENHANCE)
			{
				assertFalse(family.name(), family.isEnabled(config));
			}
			else
			{
				assertTrue(family.name(), family.isEnabled(config));
			}
		}
	}

	private static void assertFamily(Family family, PotionEffect effect, int... itemIds)
	{
		for (int itemId : itemIds)
		{
			assertEquals("family for " + itemId, family, PotionItems.getFamily(itemId));
			assertEquals("effect for " + itemId, effect, PotionItems.getEffect(itemId));
		}
	}
}
