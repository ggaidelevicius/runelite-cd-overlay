package com.cooldownoverlays;

import java.util.Set;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Skill;
import net.runelite.api.gameval.ItemID;

/** Decides whether a known item timer makes another left-click dose unnecessary. */
final class RepotProtectionPolicy
{
	// Every supported barbarian mix also heals, regardless of its potion effect.
	private static final Set<Integer> HEALING_MIXES = Set.of(
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
		ItemID.BRUTAL_ANTIDOTE_2, ItemID.BRUTAL_ANTIDOTE_1);

	private final Client client;
	private final CooldownOverlaysConfig config;
	private final CooldownTracker tracker;
	private final PotionTimers potionTimers;
	private final BoostPotionTimers boostPotionTimers;

	@Inject
	RepotProtectionPolicy(Client client, CooldownOverlaysConfig config, CooldownTracker tracker,
		PotionTimers potionTimers, BoostPotionTimers boostPotionTimers)
	{
		this.client = client;
		this.config = config;
		this.tracker = tracker;
		this.potionTimers = potionTimers;
		this.boostPotionTimers = boostPotionTimers;
	}

	boolean shouldProtect(int itemId, long nowNanos)
	{
		if (!config.preventAccidentalRedosing() || HEALING_MIXES.contains(itemId))
		{
			return false;
		}
		if (itemId == ItemID.IMBUED_HEART || itemId == ItemID.SATURATED_HEART)
		{
			boolean enabled = itemId == ItemID.IMBUED_HEART ? config.imbuedHeart() : config.saturatedHeart();
			return enabled && hasProtectedTime(tracker.getHeart(), nowNanos);
		}
		PotionItems.Family family = PotionItems.getFamily(itemId);
		if (family != null)
		{
			if (!family.isEnabled(config) || isRestorative(family))
			{
				return false;
			}
			if ((family == PotionItems.Family.STAMINA || family == PotionItems.Family.EXTENDED_STAMINA)
				&& client.getEnergy() < 10_000)
			{
				return false;
			}
			BoostPotion divineBoost = divineBoost(family);
			return (divineBoost == null || !canImproveStats(divineBoost, false))
				&& hasProtectedTime(potionTimers.get(family.getEffect()), nowNanos);
		}
		BoostPotion potion = BoostPotion.forItem(itemId);
		return potion != null && potion.isEnabled(config) && hasConfiguredTarget(potion)
			&& !canImproveStats(potion, true)
			&& hasProtectedTime(boostPotionTimers.get(potion), nowNanos);
	}

	private boolean hasProtectedTime(Cooldown cooldown, long nowNanos)
	{
		int graceSeconds = Math.max(0, Math.min(60, config.repotGraceSeconds()));
		return cooldown.isActive() && cooldown.isKnown()
			&& cooldown.getRemainingSeconds(nowNanos) > graceSeconds;
	}

	private boolean hasConfiguredTarget(BoostPotion potion)
	{
		for (Skill skill : potion.getSkills())
		{
			if (threshold(skill) > 0)
			{
				return true;
			}
		}
		return false;
	}

	private boolean canImproveStats(BoostPotion potion, boolean requireThreshold)
	{
		for (Skill skill : potion.getSkills())
		{
			int threshold = requireThreshold ? threshold(skill) : Integer.MAX_VALUE;
			if (threshold <= 0)
			{
				continue;
			}
			int base = client.getRealSkillLevel(skill);
			int current = client.getBoostedSkillLevel(skill);
			// Missing live skill data must not turn an old timer into a blocked dose.
			if (base <= 0 || current < 0 || current < threshold && current < potion.getBoostedLevel(skill, base))
			{
				return true;
			}
		}
		return false;
	}

	private int threshold(Skill skill)
	{
		switch (skill)
		{
			case ATTACK: return config.attackThreshold();
			case STRENGTH: return config.strengthThreshold();
			case DEFENCE: return config.defenceThreshold();
			case RANGED: return config.rangedThreshold();
			case MAGIC: return config.magicThreshold();
			default: return 0;
		}
	}

	private static boolean isRestorative(PotionItems.Family family)
	{
		return family == PotionItems.Family.SANFEW_SERUM || family == PotionItems.Family.MOONLIGHT
			|| family == PotionItems.Family.SILK_DRESSING || family == PotionItems.Family.BLESSED_CRYSTAL_SCARAB
			|| family == PotionItems.Family.SCARRED_SCRAPS;
	}

	private static BoostPotion divineBoost(PotionItems.Family family)
	{
		switch (family)
		{
			case DIVINE_ATTACK: return BoostPotion.SUPER_ATTACK;
			case DIVINE_STRENGTH: return BoostPotion.SUPER_STRENGTH;
			case DIVINE_DEFENCE: return BoostPotion.SUPER_DEFENCE;
			case DIVINE_RANGING: return BoostPotion.RANGING;
			case DIVINE_MAGIC: return BoostPotion.MAGIC;
			case DIVINE_COMBAT: return BoostPotion.SUPER_COMBAT;
			case DIVINE_BASTION: return BoostPotion.BASTION;
			case DIVINE_BATTLEMAGE: return BoostPotion.BATTLEMAGE;
			default: return null;
		}
	}
}
