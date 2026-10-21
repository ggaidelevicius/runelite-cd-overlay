package com.cooldownoverlays;

import java.util.EnumSet;
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
	public void poisonProtectionIncludesEveryOrdinaryDoseAndAvailableMix()
	{
		assertFamily(Family.ANTIPOISON, PotionEffect.ANTIPOISON,
			ItemID._4DOSEANTIPOISON, ItemID._3DOSEANTIPOISON, ItemID._2DOSEANTIPOISON, ItemID._1DOSEANTIPOISON,
			ItemID.BRUTAL_2DOSEANTIPOISON, ItemID.BRUTAL_1DOSEANTIPOISON);
		assertFamily(Family.SUPER_ANTIPOISON, PotionEffect.ANTIPOISON,
			ItemID._4DOSE2ANTIPOISON, ItemID._3DOSE2ANTIPOISON, ItemID._2DOSE2ANTIPOISON, ItemID._1DOSE2ANTIPOISON,
			ItemID.BRUTAL_2DOSE2ANTIPOISON, ItemID.BRUTAL_1DOSE2ANTIPOISON);
		assertFamily(Family.ANTIDOTE_PLUS, PotionEffect.ANTIPOISON,
			ItemID.ANTIDOTE_4, ItemID.ANTIDOTE_3, ItemID.ANTIDOTE_2, ItemID.ANTIDOTE_1,
			ItemID.BRUTAL_ANTIDOTE_2, ItemID.BRUTAL_ANTIDOTE_1);
		assertFamily(Family.ANTIDOTE_PLUS_PLUS, PotionEffect.ANTIPOISON,
			ItemID.ANTIDOTE__4, ItemID.ANTIDOTE__3, ItemID.ANTIDOTE__2, ItemID.ANTIDOTE__1);
	}

	@Test
	public void sanfewSerumIncludesStandardAndBountyHunterSupplies()
	{
		assertFamily(Family.SANFEW_SERUM, PotionEffect.ANTIPOISON,
			ItemID.SANFEW_SALVE_4_DOSE, ItemID.SANFEW_SALVE_3_DOSE,
			ItemID.SANFEW_SALVE_2_DOSE, ItemID.SANFEW_SALVE_1_DOSE,
			ItemID.BR_SANFEW_SALVE_4_DOSE, ItemID.BR_SANFEW_SALVE_3_DOSE,
			ItemID.BR_SANFEW_SALVE_2_DOSE, ItemID.BR_SANFEW_SALVE_1_DOSE);
	}

	@Test
	public void chambersAntipoisonTiersSharePoisonProtectionAcrossEveryDose()
	{
		assertFamily(Family.WEAK_RAID_ANTIPOISON, PotionEffect.ANTIPOISON,
			ItemID.RAIDS_VIAL_ANTIPOISON_WEAK_4, ItemID.RAIDS_VIAL_ANTIPOISON_WEAK_3,
			ItemID.RAIDS_VIAL_ANTIPOISON_WEAK_2, ItemID.RAIDS_VIAL_ANTIPOISON_WEAK_1);
		assertFamily(Family.RAID_ANTIPOISON, PotionEffect.ANTIPOISON,
			ItemID.RAIDS_VIAL_ANTIPOISON_4, ItemID.RAIDS_VIAL_ANTIPOISON_3,
			ItemID.RAIDS_VIAL_ANTIPOISON_2, ItemID.RAIDS_VIAL_ANTIPOISON_1);
		assertFamily(Family.STRONG_RAID_ANTIPOISON, PotionEffect.ANTIPOISON,
			ItemID.RAIDS_VIAL_ANTIPOISON_STRONG_4, ItemID.RAIDS_VIAL_ANTIPOISON_STRONG_3,
			ItemID.RAIDS_VIAL_ANTIPOISON_STRONG_2, ItemID.RAIDS_VIAL_ANTIPOISON_STRONG_1);
	}

	@Test
	public void newlySupportedTimedPotionsKeepOverlaysAcrossAllDoses()
	{
		assertFamily(Family.GOADING, PotionEffect.GOADING,
			ItemID._4DOSEGOADING, ItemID._3DOSEGOADING, ItemID._2DOSEGOADING, ItemID._1DOSEGOADING);
		assertFamily(Family.MENAPHITE_REMEDY, PotionEffect.MENAPHITE_REMEDY,
			ItemID._4DOSESTATRENEWAL, ItemID._3DOSESTATRENEWAL, ItemID._2DOSESTATRENEWAL, ItemID._1DOSESTATRENEWAL);
		assertFamily(Family.MOONLIGHT, PotionEffect.MOONLIGHT,
			ItemID._4DOSEMOONLIGHTPOTION, ItemID._3DOSEMOONLIGHTPOTION,
			ItemID._2DOSEMOONLIGHTPOTION, ItemID._1DOSEMOONLIGHTPOTION);
		assertFamily(Family.PRAYER_REGENERATION, PotionEffect.PRAYER_REGENERATION,
			ItemID._4DOSE1PRAYER_REGENERATION, ItemID._3DOSE1PRAYER_REGENERATION,
			ItemID._2DOSE1PRAYER_REGENERATION, ItemID._1DOSE1PRAYER_REGENERATION);
	}

	@Test
	public void nightmareZoneAndBlightedOverloadsUseTheirOwnEffects()
	{
		assertFamily(Family.NMZ_OVERLOAD, PotionEffect.NMZ_OVERLOAD,
			ItemID.NZONE4DOSEOVERLOADPOTION, ItemID.NZONE3DOSEOVERLOADPOTION,
			ItemID.NZONE2DOSEOVERLOADPOTION, ItemID.NZONE1DOSEOVERLOADPOTION);
		assertFamily(Family.BLIGHTED_OVERLOAD, PotionEffect.BLIGHTED_OVERLOAD,
			ItemID.DEADMAN4DOSEOVERLOAD, ItemID.DEADMAN3DOSEOVERLOAD,
			ItemID.DEADMAN2DOSEOVERLOAD, ItemID.DEADMAN1DOSEOVERLOAD);
	}

	@Test
	public void bothUsesOfTimedTombsSuppliesShareTheirMatchingEffect()
	{
		assertFamily(Family.SILK_DRESSING, PotionEffect.SILK_DRESSING,
			ItemID.TOA_SUPPLY_HEAL_OVERTIME_2, ItemID.TOA_SUPPLY_HEAL_OVERTIME_1);
		assertFamily(Family.BLESSED_CRYSTAL_SCARAB, PotionEffect.BLESSED_CRYSTAL_SCARAB,
			ItemID.TOA_SUPPLY_PRAYER_OVERTIME_2, ItemID.TOA_SUPPLY_PRAYER_OVERTIME_1);
	}

	@Test
	public void scarredScrapsShareTheStaminaEffectWithTheirOwnSetting()
	{
		assertFamily(Family.SCARRED_SCRAPS, PotionEffect.STAMINA, ItemID.DT2_SCAR_MAZE_STAMINA);
		CooldownOverlaysConfig config = new CooldownOverlaysConfig()
		{
			@Override
			public boolean staminaPotion()
			{
				return false;
			}
		};
		assertFalse(Family.STAMINA.isEnabled(config));
		assertTrue(Family.SCARRED_SCRAPS.isEnabled(config));
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
			ItemID._4DOSE2ATTACK, ItemID.RATCATCHERS_CAT_ANTIPOISON,
			ItemID.Cert._4DOSESURGE, ItemID.Placeholder._4DOSESURGE,
			ItemID.Cert._4DOSESTAMINA, ItemID.Placeholder._4DOSESTAMINA,
			ItemID.Cert._4DOSEDIVINECOMBAT, ItemID.Placeholder._4DOSEDIVINECOMBAT,
			ItemID.UNFINISHED_ANTIDOTE_, ItemID.UNFINISHED_ANTIDOTE__,
			ItemID.SANFEW_SALVE_STEP_1_4_DOSE, ItemID.SANFEW_SALVE_STEP_2_4_DOSE,
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
	public void newEffectPotionNotesAndPlaceholdersDoNotReceiveOverlays()
	{
		int[] nonUsable = {
			ItemID.Cert._4DOSEANTIPOISON, ItemID.Placeholder._4DOSEANTIPOISON,
			ItemID.Cert._4DOSE2ANTIPOISON, ItemID.Placeholder._4DOSE2ANTIPOISON,
			ItemID.Cert.ANTIDOTE_4, ItemID.Placeholder.ANTIDOTE_4,
			ItemID.Cert.ANTIDOTE__4, ItemID.Placeholder.ANTIDOTE__4,
			ItemID.Cert.SANFEW_SALVE_4_DOSE, ItemID.Placeholder.SANFEW_SALVE_4_DOSE,
			ItemID.Cert._4DOSEGOADING, ItemID.Placeholder._4DOSEGOADING,
			ItemID.Cert._4DOSE1PRAYER_REGENERATION, ItemID.Placeholder._4DOSE1PRAYER_REGENERATION,
			ItemID.Cert._4DOSESTATRENEWAL, ItemID.Placeholder._4DOSESTATRENEWAL,
			ItemID.Cert.DEADMAN4DOSEOVERLOAD, ItemID.Placeholder.DEADMAN4DOSEOVERLOAD,
			ItemID.Placeholder.NZONE4DOSEOVERLOADPOTION,
			ItemID.Cert.TOA_SUPPLY_HEAL_OVERTIME_2, ItemID.Cert.TOA_SUPPLY_PRAYER_OVERTIME_2
		};
		for (int itemId : nonUsable)
		{
			assertNull("family for " + itemId, PotionItems.getFamily(itemId));
			assertNull("effect for " + itemId, PotionItems.getEffect(itemId));
		}
	}

	@Test
	public void everyNewFamilyHasAnIndependentToggleEvenWhenItSharesAnEffect()
	{
		for (Family disabled : EnumSet.of(Family.ANTIPOISON, Family.SUPER_ANTIPOISON,
			Family.ANTIDOTE_PLUS, Family.ANTIDOTE_PLUS_PLUS, Family.SANFEW_SERUM,
			Family.WEAK_RAID_ANTIPOISON, Family.RAID_ANTIPOISON, Family.STRONG_RAID_ANTIPOISON,
			Family.GOADING, Family.MENAPHITE_REMEDY, Family.MOONLIGHT, Family.PRAYER_REGENERATION,
			Family.NMZ_OVERLOAD, Family.BLIGHTED_OVERLOAD, Family.SILK_DRESSING,
			Family.BLESSED_CRYSTAL_SCARAB, Family.SCARRED_SCRAPS))
		{
			CooldownOverlaysConfig config = newFamilyConfig(disabled);
			for (Family family : Family.values())
			{
				assertEquals("Disabling " + disabled + " must only affect that family: " + family,
					family != disabled, family.isEnabled(config));
			}
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

	private static CooldownOverlaysConfig newFamilyConfig(Family disabled)
	{
		return new CooldownOverlaysConfig()
		{
			@Override
			public boolean antipoison()
			{
				return disabled != Family.ANTIPOISON;
			}

			@Override
			public boolean superAntipoison()
			{
				return disabled != Family.SUPER_ANTIPOISON;
			}

			@Override
			public boolean antidotePlus()
			{
				return disabled != Family.ANTIDOTE_PLUS;
			}

			@Override
			public boolean antidotePlusPlus()
			{
				return disabled != Family.ANTIDOTE_PLUS_PLUS;
			}

			@Override
			public boolean sanfewSerum()
			{
				return disabled != Family.SANFEW_SERUM;
			}

			@Override
			public boolean weakRaidAntipoison()
			{
				return disabled != Family.WEAK_RAID_ANTIPOISON;
			}

			@Override
			public boolean raidAntipoison()
			{
				return disabled != Family.RAID_ANTIPOISON;
			}

			@Override
			public boolean strongRaidAntipoison()
			{
				return disabled != Family.STRONG_RAID_ANTIPOISON;
			}

			@Override
			public boolean goadingPotion()
			{
				return disabled != Family.GOADING;
			}

			@Override
			public boolean menaphiteRemedy()
			{
				return disabled != Family.MENAPHITE_REMEDY;
			}

			@Override
			public boolean moonlightPotion()
			{
				return disabled != Family.MOONLIGHT;
			}

			@Override
			public boolean prayerRegenerationPotion()
			{
				return disabled != Family.PRAYER_REGENERATION;
			}

			@Override
			public boolean nmzOverload()
			{
				return disabled != Family.NMZ_OVERLOAD;
			}

			@Override
			public boolean blightedOverload()
			{
				return disabled != Family.BLIGHTED_OVERLOAD;
			}

			@Override
			public boolean silkDressing()
			{
				return disabled != Family.SILK_DRESSING;
			}

			@Override
			public boolean blessedCrystalScarab()
			{
				return disabled != Family.BLESSED_CRYSTAL_SCARAB;
			}

			@Override
			public boolean scarredScraps()
			{
				return disabled != Family.SCARRED_SCRAPS;
			}
		};
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
