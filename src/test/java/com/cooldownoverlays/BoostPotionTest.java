package com.cooldownoverlays;

import net.runelite.api.Skill;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class BoostPotionTest
{
	@Test
	public void meleePotionsKeepOneFamilyAcrossEveryDoseAndMix()
	{
		assertFamily(BoostPotion.ATTACK,
			ItemID._4DOSE1ATTACK, ItemID._3DOSE1ATTACK, ItemID._2DOSE1ATTACK, ItemID._1DOSE1ATTACK,
			ItemID.BRUTAL_2DOSE1ATTACK, ItemID.BRUTAL_1DOSE1ATTACK);
		assertFamily(BoostPotion.STRENGTH,
			ItemID.STRENGTH4, ItemID._3DOSE1STRENGTH, ItemID._2DOSE1STRENGTH, ItemID._1DOSE1STRENGTH,
			ItemID.BRUTAL_2DOSE1STRENGTH, ItemID.BRUTAL_1DOSE1STRENGTH);
		assertFamily(BoostPotion.DEFENCE,
			ItemID._4DOSE1DEFENSE, ItemID._3DOSE1DEFENSE, ItemID._2DOSE1DEFENSE, ItemID._1DOSE1DEFENSE,
			ItemID.BRUTAL_2DOSE1DEFENSE, ItemID.BRUTAL_1DOSE1DEFENSE);
		assertFamily(BoostPotion.SUPER_ATTACK,
			ItemID._4DOSE2ATTACK, ItemID._3DOSE2ATTACK, ItemID._2DOSE2ATTACK, ItemID._1DOSE2ATTACK,
			ItemID.BRUTAL_2DOSE2ATTACK, ItemID.BRUTAL_1DOSE2ATTACK);
		assertFamily(BoostPotion.SUPER_STRENGTH,
			ItemID._4DOSE2STRENGTH, ItemID._3DOSE2STRENGTH, ItemID._2DOSE2STRENGTH, ItemID._1DOSE2STRENGTH,
			ItemID.BRUTAL_2DOSE2STRENGTH, ItemID.BRUTAL_1DOSE2STRENGTH);
		assertFamily(BoostPotion.SUPER_DEFENCE,
			ItemID._4DOSE2DEFENSE, ItemID._3DOSE2DEFENSE, ItemID._2DOSE2DEFENSE, ItemID._1DOSE2DEFENSE,
			ItemID.BRUTAL_2DOSE2DEFENSE, ItemID.BRUTAL_1DOSE2DEFENSE);
	}

	@Test
	public void combinedPotionsRetainAllBeneficialSkills()
	{
		assertFamily(BoostPotion.COMBAT,
			ItemID._4DOSECOMBAT, ItemID._3DOSECOMBAT, ItemID._2DOSECOMBAT, ItemID._1DOSECOMBAT,
			ItemID.BRUTAL_2DOSECOMBAT, ItemID.BRUTAL_1DOSECOMBAT,
			ItemID._4DOSECOMBAT_DEADMAN_STARTER, ItemID._3DOSECOMBAT_DEADMAN_STARTER,
			ItemID._2DOSECOMBAT_DEADMAN_STARTER, ItemID._1DOSECOMBAT_DEADMAN_STARTER);
		assertFamily(BoostPotion.SUPER_COMBAT,
			ItemID._4DOSE2COMBAT, ItemID._3DOSE2COMBAT, ItemID._2DOSE2COMBAT, ItemID._1DOSE2COMBAT);
		assertFamily(BoostPotion.BASTION,
			ItemID._4DOSEBASTION, ItemID._3DOSEBASTION, ItemID._2DOSEBASTION, ItemID._1DOSEBASTION);
		assertFamily(BoostPotion.BATTLEMAGE,
			ItemID._4DOSEBATTLEMAGE, ItemID._3DOSEBATTLEMAGE, ItemID._2DOSEBATTLEMAGE, ItemID._1DOSEBATTLEMAGE);
		assertArrayEquals(new Skill[]{Skill.ATTACK, Skill.STRENGTH}, BoostPotion.COMBAT.getSkills());
		assertArrayEquals(new Skill[]{Skill.ATTACK, Skill.STRENGTH, Skill.DEFENCE}, BoostPotion.SUPER_COMBAT.getSkills());
		assertArrayEquals(new Skill[]{Skill.RANGED, Skill.DEFENCE}, BoostPotion.BASTION.getSkills());
		assertArrayEquals(new Skill[]{Skill.MAGIC, Skill.DEFENCE}, BoostPotion.BATTLEMAGE.getSkills());
	}

	@Test
	public void rangedAndMagicIncludeMixesAndRangedBountySupplies()
	{
		assertFamily(BoostPotion.RANGING,
			ItemID._4DOSERANGERSPOTION, ItemID._3DOSERANGERSPOTION,
			ItemID._2DOSERANGERSPOTION, ItemID._1DOSERANGERSPOTION,
			ItemID.BRUTAL_2DOSERANGERSPOTION, ItemID.BRUTAL_1DOSERANGERSPOTION,
			ItemID.BR_4DOSERANGERSPOTION, ItemID.BR_3DOSERANGERSPOTION,
			ItemID.BR_2DOSERANGERSPOTION, ItemID.BR_1DOSERANGERSPOTION);
		assertFamily(BoostPotion.MAGIC,
			ItemID._4DOSE1MAGIC, ItemID._3DOSE1MAGIC, ItemID._2DOSE1MAGIC, ItemID._1DOSE1MAGIC,
			ItemID.BRUTAL_2DOSE1MAGIC, ItemID.BRUTAL_1DOSE1MAGIC);
	}

	@Test
	public void meleeBoostCapsUseFlooredBaseLevelAndPotionStrength()
	{
		assertEquals(111, BoostPotion.ATTACK.getBoostedLevel(Skill.ATTACK, 99));
		assertEquals(111, BoostPotion.STRENGTH.getBoostedLevel(Skill.STRENGTH, 99));
		assertEquals(111, BoostPotion.DEFENCE.getBoostedLevel(Skill.DEFENCE, 99));
		assertEquals(118, BoostPotion.SUPER_ATTACK.getBoostedLevel(Skill.ATTACK, 99));
		assertEquals(118, BoostPotion.SUPER_STRENGTH.getBoostedLevel(Skill.STRENGTH, 99));
		assertEquals(118, BoostPotion.SUPER_DEFENCE.getBoostedLevel(Skill.DEFENCE, 99));
		assertEquals(23, BoostPotion.ATTACK.getBoostedLevel(Skill.ATTACK, 19));
		assertEquals(25, BoostPotion.ATTACK.getBoostedLevel(Skill.ATTACK, 20));
		assertEquals(26, BoostPotion.SUPER_ATTACK.getBoostedLevel(Skill.ATTACK, 19));
		assertEquals(28, BoostPotion.SUPER_ATTACK.getBoostedLevel(Skill.ATTACK, 20));
	}

	@Test
	public void combinedPotionsApplyTheirDifferentStatCaps()
	{
		assertEquals(111, BoostPotion.COMBAT.getBoostedLevel(Skill.ATTACK, 99));
		assertEquals(111, BoostPotion.COMBAT.getBoostedLevel(Skill.STRENGTH, 99));
		assertEquals(99, BoostPotion.COMBAT.getBoostedLevel(Skill.DEFENCE, 99));
		for (Skill skill : BoostPotion.SUPER_COMBAT.getSkills())
		{
			assertEquals(118, BoostPotion.SUPER_COMBAT.getBoostedLevel(skill, 99));
		}
		assertEquals(112, BoostPotion.BASTION.getBoostedLevel(Skill.RANGED, 99));
		assertEquals(118, BoostPotion.BASTION.getBoostedLevel(Skill.DEFENCE, 99));
		assertEquals(103, BoostPotion.BATTLEMAGE.getBoostedLevel(Skill.MAGIC, 99));
		assertEquals(118, BoostPotion.BATTLEMAGE.getBoostedLevel(Skill.DEFENCE, 99));
	}

	@Test
	public void magicIsFlatWhileRangingDependsOnBaseLevel()
	{
		assertEquals(5, BoostPotion.MAGIC.getBoostedLevel(Skill.MAGIC, 1));
		assertEquals(103, BoostPotion.MAGIC.getBoostedLevel(Skill.MAGIC, 99));
		assertEquals(5, BoostPotion.RANGING.getBoostedLevel(Skill.RANGED, 1));
		assertEquals(112, BoostPotion.RANGING.getBoostedLevel(Skill.RANGED, 99));
		assertEquals(90, BoostPotion.RANGING.getBoostedLevel(Skill.MAGIC, 90));
	}

	@Test
	public void skillArrayCannotMutatePotionDefinitions()
	{
		Skill[] skills = BoostPotion.ATTACK.getSkills();
		skills[0] = Skill.DEFENCE;
		assertArrayEquals(new Skill[]{Skill.ATTACK}, BoostPotion.ATTACK.getSkills());
	}

	@Test
	public void notesPlaceholdersDivinesRaidsAndRestoresHaveNoOrdinaryBoostOverlay()
	{
		int[] unsupported = {
			-1, ItemID.Cert._4DOSE2ATTACK, ItemID.Placeholder._4DOSE2ATTACK,
			ItemID.Cert.STRENGTH4, ItemID.Placeholder.STRENGTH4,
			ItemID._4DOSEDIVINECOMBAT, ItemID._4DOSEDIVINEBATTLEMAGE,
			ItemID.RAIDS_VIAL_OVERLOAD_STRONG_4, ItemID.TOA_SUPPLY_STATS_2,
			ItemID._4DOSESTAMINA, ItemID._4DOSESTATRESTORE, ItemID._4DOSEPOTIONOFSARADOMIN,
			ItemID._4DOSEPOTIONOFZAMORAK
		};
		for (int itemId : unsupported)
		{
			assertNull("ordinary boost for " + itemId, BoostPotion.forItem(itemId));
		}
	}

	@Test
	public void everyFamilyIsEnabledByDefaultAndHasItsOwnSetting()
	{
		CooldownOverlaysConfig defaults = new CooldownOverlaysConfig() {};
		CooldownOverlaysConfig selected = new CooldownOverlaysConfig()
		{
			@Override
			public boolean attackPotion()
			{
				return false;
			}

			@Override
			public boolean superDefencePotion()
			{
				return false;
			}

			@Override
			public boolean bastionPotion()
			{
				return false;
			}
		};
		for (BoostPotion potion : BoostPotion.values())
		{
			assertTrue(potion.name(), potion.isEnabled(defaults));
			if (potion == BoostPotion.ATTACK || potion == BoostPotion.SUPER_DEFENCE || potion == BoostPotion.BASTION)
			{
				assertFalse(potion.name(), potion.isEnabled(selected));
			}
			else
			{
				assertTrue(potion.name(), potion.isEnabled(selected));
			}
		}
	}

	private static void assertFamily(BoostPotion expected, int... itemIds)
	{
		for (int itemId : itemIds)
		{
			assertEquals("boost family for " + itemId, expected, BoostPotion.forItem(itemId));
			assertNull("separate fixed effect for " + itemId, PotionItems.getFamily(itemId));
		}
	}
}
