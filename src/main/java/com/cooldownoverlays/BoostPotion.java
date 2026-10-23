package com.cooldownoverlays;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;
import net.runelite.api.Skill;
import net.runelite.api.gameval.ItemID;

/** Ordinary combat boosts, whose remaining duration depends on the player's stat decay. */
enum BoostPotion
{
	ATTACK(CooldownOverlaysConfig::attackPotion, new Boost[]{Boost.ATTACK},
		ItemID._4DOSE1ATTACK, ItemID._3DOSE1ATTACK, ItemID._2DOSE1ATTACK, ItemID._1DOSE1ATTACK,
		ItemID.BRUTAL_2DOSE1ATTACK, ItemID.BRUTAL_1DOSE1ATTACK),
	STRENGTH(CooldownOverlaysConfig::strengthPotion, new Boost[]{Boost.STRENGTH},
		ItemID.STRENGTH4, ItemID._3DOSE1STRENGTH, ItemID._2DOSE1STRENGTH, ItemID._1DOSE1STRENGTH,
		ItemID.BRUTAL_2DOSE1STRENGTH, ItemID.BRUTAL_1DOSE1STRENGTH),
	DEFENCE(CooldownOverlaysConfig::defencePotion, new Boost[]{Boost.DEFENCE},
		ItemID._4DOSE1DEFENSE, ItemID._3DOSE1DEFENSE, ItemID._2DOSE1DEFENSE, ItemID._1DOSE1DEFENSE,
		ItemID.BRUTAL_2DOSE1DEFENSE, ItemID.BRUTAL_1DOSE1DEFENSE),
	SUPER_ATTACK(CooldownOverlaysConfig::superAttackPotion, new Boost[]{Boost.SUPER_ATTACK},
		ItemID._4DOSE2ATTACK, ItemID._3DOSE2ATTACK, ItemID._2DOSE2ATTACK, ItemID._1DOSE2ATTACK,
		ItemID.BRUTAL_2DOSE2ATTACK, ItemID.BRUTAL_1DOSE2ATTACK),
	SUPER_STRENGTH(CooldownOverlaysConfig::superStrengthPotion, new Boost[]{Boost.SUPER_STRENGTH},
		ItemID._4DOSE2STRENGTH, ItemID._3DOSE2STRENGTH, ItemID._2DOSE2STRENGTH, ItemID._1DOSE2STRENGTH,
		ItemID.BRUTAL_2DOSE2STRENGTH, ItemID.BRUTAL_1DOSE2STRENGTH),
	SUPER_DEFENCE(CooldownOverlaysConfig::superDefencePotion, new Boost[]{Boost.SUPER_DEFENCE},
		ItemID._4DOSE2DEFENSE, ItemID._3DOSE2DEFENSE, ItemID._2DOSE2DEFENSE, ItemID._1DOSE2DEFENSE,
		ItemID.BRUTAL_2DOSE2DEFENSE, ItemID.BRUTAL_1DOSE2DEFENSE),
	COMBAT(CooldownOverlaysConfig::combatPotion, new Boost[]{Boost.ATTACK, Boost.STRENGTH},
		ItemID._4DOSECOMBAT, ItemID._3DOSECOMBAT, ItemID._2DOSECOMBAT, ItemID._1DOSECOMBAT,
		ItemID.BRUTAL_2DOSECOMBAT, ItemID.BRUTAL_1DOSECOMBAT,
		ItemID._4DOSECOMBAT_DEADMAN_STARTER, ItemID._3DOSECOMBAT_DEADMAN_STARTER,
		ItemID._2DOSECOMBAT_DEADMAN_STARTER, ItemID._1DOSECOMBAT_DEADMAN_STARTER),
	SUPER_COMBAT(CooldownOverlaysConfig::superCombatPotion,
		new Boost[]{Boost.SUPER_ATTACK, Boost.SUPER_STRENGTH, Boost.SUPER_DEFENCE},
		ItemID._4DOSE2COMBAT, ItemID._3DOSE2COMBAT, ItemID._2DOSE2COMBAT, ItemID._1DOSE2COMBAT),
	// LMS super combat has no Defence boost, so that threshold cannot make it useful.
	LMS_SUPER_COMBAT(CooldownOverlaysConfig::superCombatPotion,
		new Boost[]{Boost.SUPER_ATTACK, Boost.SUPER_STRENGTH},
		ItemID.BR_4DOSE2COMBAT, ItemID.BR_3DOSE2COMBAT, ItemID.BR_2DOSE2COMBAT, ItemID.BR_1DOSE2COMBAT),
	RANGING(CooldownOverlaysConfig::rangingPotion, new Boost[]{Boost.RANGED},
		ItemID._4DOSERANGERSPOTION, ItemID._3DOSERANGERSPOTION,
		ItemID._2DOSERANGERSPOTION, ItemID._1DOSERANGERSPOTION,
		ItemID.BRUTAL_2DOSERANGERSPOTION, ItemID.BRUTAL_1DOSERANGERSPOTION,
		ItemID.BR_4DOSERANGERSPOTION, ItemID.BR_3DOSERANGERSPOTION,
		ItemID.BR_2DOSERANGERSPOTION, ItemID.BR_1DOSERANGERSPOTION),
	MAGIC(CooldownOverlaysConfig::magicPotion, new Boost[]{Boost.MAGIC},
		ItemID._4DOSE1MAGIC, ItemID._3DOSE1MAGIC, ItemID._2DOSE1MAGIC, ItemID._1DOSE1MAGIC,
		ItemID.BRUTAL_2DOSE1MAGIC, ItemID.BRUTAL_1DOSE1MAGIC),
	BASTION(CooldownOverlaysConfig::bastionPotion, new Boost[]{Boost.RANGED, Boost.SUPER_DEFENCE},
		ItemID._4DOSEBASTION, ItemID._3DOSEBASTION, ItemID._2DOSEBASTION, ItemID._1DOSEBASTION),
	BATTLEMAGE(CooldownOverlaysConfig::battlemagePotion, new Boost[]{Boost.MAGIC, Boost.SUPER_DEFENCE},
		ItemID._4DOSEBATTLEMAGE, ItemID._3DOSEBATTLEMAGE, ItemID._2DOSEBATTLEMAGE, ItemID._1DOSEBATTLEMAGE);

	private enum Boost
	{
		ATTACK(Skill.ATTACK, 10, 3),
		STRENGTH(Skill.STRENGTH, 10, 3),
		DEFENCE(Skill.DEFENCE, 10, 3),
		SUPER_ATTACK(Skill.ATTACK, 15, 5),
		SUPER_STRENGTH(Skill.STRENGTH, 15, 5),
		SUPER_DEFENCE(Skill.DEFENCE, 15, 5),
		RANGED(Skill.RANGED, 10, 4),
		MAGIC(Skill.MAGIC, 0, 4);

		private final Skill skill;
		private final int percentage;
		private final int flat;

		Boost(Skill skill, int percentage, int flat)
		{
			this.skill = skill;
			this.percentage = percentage;
			this.flat = flat;
		}
	}

	private static final Map<Integer, BoostPotion> POTIONS;

	static
	{
		Map<Integer, BoostPotion> potions = new HashMap<>();
		for (BoostPotion potion : values())
		{
			for (int itemId : potion.itemIds)
			{
				if (potions.put(itemId, potion) != null)
				{
					throw new IllegalStateException("Boost potion belongs to multiple families: " + itemId);
				}
			}
		}
		POTIONS = Collections.unmodifiableMap(potions);
	}

	private final Predicate<CooldownOverlaysConfig> enabled;
	private final Boost[] boosts;
	private final int[] itemIds;

	BoostPotion(Predicate<CooldownOverlaysConfig> enabled, Boost[] boosts, int... itemIds)
	{
		this.enabled = enabled;
		this.boosts = boosts;
		this.itemIds = itemIds;
	}

	static BoostPotion forItem(int itemId)
	{
		return POTIONS.get(itemId);
	}

	boolean isEnabled(CooldownOverlaysConfig config)
	{
		return enabled.test(config);
	}

	Skill[] getSkills()
	{
		Skill[] skills = new Skill[boosts.length];
		for (int i = 0; i < boosts.length; i++)
		{
			skills[i] = boosts[i].skill;
		}
		return skills;
	}

	/** The stat cap of a dose, using the same floor-and-add formulas as RuneLite's item stats. */
	int getBoostedLevel(Skill skill, int baseLevel)
	{
		for (Boost boost : boosts)
		{
			if (boost.skill == skill)
			{
				return baseLevel + baseLevel * boost.percentage / 100 + boost.flat;
			}
		}
		return baseLevel;
	}
}
