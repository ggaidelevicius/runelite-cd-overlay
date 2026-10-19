package com.cooldownoverlays;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.Skill;
import net.runelite.api.gameval.VarbitID;

/** Estimates time to a useful re-dose from actual combat stats and their shared decay phase. */
@Singleton
final class BoostPotionTimers
{
	private static final Skill[] SKILLS = {
		Skill.ATTACK, Skill.STRENGTH, Skill.DEFENCE, Skill.RANGED, Skill.MAGIC
	};
	private final Map<BoostPotion, Cooldown> timers = new EnumMap<>(BoostPotion.class);
	private final Map<Skill, Integer> previousBase = new EnumMap<>(Skill.class);
	private final Map<Skill, Integer> previousBoosted = new EnumMap<>(Skill.class);
	private final Set<Skill> previousProtected = EnumSet.noneOf(Skill.class);
	private final BoostDecayClock decayClock = new BoostDecayClock();
	private final Map<Skill, Integer> previousThresholds = new EnumMap<>(Skill.class);
	private boolean previousPreserve;

	BoostPotionTimers()
	{
		for (BoostPotion potion : BoostPotion.values())
		{
			timers.put(potion, new Cooldown());
		}
	}

	void tick(Client client, CooldownOverlaysConfig config, long nowNanos)
	{
		Map<Skill, Integer> base = new EnumMap<>(Skill.class);
		Map<Skill, Integer> boosted = new EnumMap<>(Skill.class);
		Map<Skill, Integer> thresholds = new EnumMap<>(Skill.class);
		thresholds.put(Skill.ATTACK, config.attackThreshold());
		thresholds.put(Skill.STRENGTH, config.strengthThreshold());
		thresholds.put(Skill.DEFENCE, config.defenceThreshold());
		thresholds.put(Skill.RANGED, config.rangedThreshold());
		thresholds.put(Skill.MAGIC, config.magicThreshold());
		for (Skill skill : SKILLS)
		{
			base.put(skill, client.getRealSkillLevel(skill));
			boosted.put(skill, client.getBoostedSkillLevel(skill));
		}
		tick(client.getTickCount(), base, boosted, protectedSkills(client, base, boosted),
			client.getVarbitValue(VarbitID.PRAYER_PRESERVE) > 0, thresholds, nowNanos);
	}

	void tick(int tick, Map<Skill, Integer> base, Map<Skill, Integer> boosted,
		Set<Skill> protectedSkills, boolean preserve, Map<Skill, Integer> thresholds, long nowNanos)
	{
		int nextDecay = decayClock.ticksToNextDecay(tick, preserve);
		for (Skill skill : SKILLS)
		{
			int level = base.getOrDefault(skill, 0);
			int current = boosted.getOrDefault(skill, level);
			Integer previous = previousBoosted.get(skill);
			if (level > 0 && boosted.containsKey(skill)
				&& !protectedSkills.contains(skill) && !previousProtected.contains(skill)
				&& previous != null && previousBase.getOrDefault(skill, 0) == level
				&& previous > level && current >= level && current == previous - 1
				&& (nextDecay == -1 || nextDecay <= 2))
			{
				// Restrict later observations to the expected window, reducing false phase
				// changes from one-level enemy drains. The first observation is an estimate.
				decayClock.observeDecay(tick);
			}
		}
		nextDecay = decayClock.ticksToNextDecay(tick, preserve);
		for (BoostPotion potion : BoostPotion.values())
		{
			Cooldown timer = timers.get(potion);
			boolean eligible = false;
			boolean ready = false;
			boolean refreshed = !thresholds.equals(previousThresholds) || preserve != previousPreserve;
			int earliestTicks = Integer.MAX_VALUE;
			for (Skill skill : potion.getSkills())
			{
				int level = base.getOrDefault(skill, 0);
				int threshold = thresholds.getOrDefault(skill, 0);
				if (threshold <= 0 || level <= 0 || !boosted.containsKey(skill)
					|| protectedSkills.contains(skill))
				{
					continue;
				}
				int current = boosted.getOrDefault(skill, level);
				int cap = potion.getBoostedLevel(skill, level);
				boolean skillReady = current < threshold && current < cap;
				int targetLevel = Math.min(threshold - 1, cap - 1);
				// Natural decay stops at base. A lower target can only be crossed by a
				// stat drain, so show no invented countdown while waiting for one.
				if (!skillReady && targetLevel < level)
				{
					continue;
				}
				eligible = true;
				if (skillReady)
				{
					ready = true;
				}
				else if (nextDecay > 0)
				{
					int levelsToThreshold = current - targetLevel;
					int ticks = nextDecay + (levelsToThreshold - 1) * (preserve ? 150 : 100);
					earliestTicks = Math.min(earliestTicks, ticks);
				}
				refreshed |= current > previousBoosted.getOrDefault(skill, current)
					|| level != previousBase.getOrDefault(skill, level);
			}
			if (!eligible)
			{
				timer.clear();
			}
			else if (ready)
			{
				timer.setReady(nowNanos);
			}
			else if (earliestTicks == Integer.MAX_VALUE)
			{
				timer.startUnknown(nowNanos);
			}
			else
			{
				int total = refreshed ? earliestTicks : Math.max(earliestTicks, timer.getTotalTicks());
				timer.start(earliestTicks, total, nowNanos);
			}
		}
		previousBase.clear();
		previousBase.putAll(base);
		previousBoosted.clear();
		previousBoosted.putAll(boosted);
		previousProtected.clear();
		previousProtected.addAll(protectedSkills);
		previousThresholds.clear();
		previousThresholds.putAll(thresholds);
		previousPreserve = preserve;
	}

	Cooldown get(BoostPotion potion)
	{
		return timers.get(potion);
	}

	void clear()
	{
		for (Cooldown timer : timers.values())
		{
			timer.clear();
		}
		previousBase.clear();
		previousBoosted.clear();
		previousProtected.clear();
		decayClock.clear();
		previousThresholds.clear();
		previousPreserve = false;
	}

	private static Set<Skill> protectedSkills(Client client, Map<Skill, Integer> base, Map<Skill, Integer> boosted)
	{
		Set<Skill> protectedSkills = EnumSet.noneOf(Skill.class);
		if ((client.getVarbitValue(VarbitID.RAIDS_CLIENT_INDUNGEON) == 1
			&& client.getVarbitValue(VarbitID.RAIDS_OVERLOAD_TIMER) > 0)
			|| client.getVarbitValue(VarbitID.NZONE_OVERLOAD_POTION_EFFECTS) > 0
			|| client.getVarbitValue(VarbitID.DEADMAN_OVERLOAD_POTION_EFFECTS) > 0
			|| client.getVarbitValue(VarbitID.TOA_MIDRAIDLOOT_STATS_TIMER) > 0)
		{
			for (Skill skill : SKILLS)
			{
				protectedSkills.add(skill);
			}
			return protectedSkills;
		}
		protect(client, protectedSkills, VarbitID.DIVINEATTACK_POTION_TIME, Skill.ATTACK);
		protect(client, protectedSkills, VarbitID.DIVINESTRENGTH_POTION_TIME, Skill.STRENGTH);
		protect(client, protectedSkills, VarbitID.DIVINEDEFENCE_POTION_TIME, Skill.DEFENCE);
		protect(client, protectedSkills, VarbitID.DIVINERANGE_POTION_TIME, Skill.RANGED);
		protect(client, protectedSkills, VarbitID.DIVINEMAGIC_POTION_TIME, Skill.MAGIC);
		protect(client, protectedSkills, VarbitID.DIVINECOMBAT_POTION_TIME, Skill.ATTACK, Skill.STRENGTH, Skill.DEFENCE);
		protect(client, protectedSkills, VarbitID.DIVINEBASTION_POTION_TIME, Skill.RANGED, Skill.DEFENCE);
		protect(client, protectedSkills, VarbitID.DIVINEBATTLEMAGE_POTION_TIME, Skill.MAGIC, Skill.DEFENCE);
		// The shared heart counter cannot identify which heart was used. A heart-sized
		// Magic boost cannot usefully be topped up by an ordinary magic potion yet.
		if (client.getVarbitValue(VarbitID.IMBUED_HEART_TIMER) > 0
			&& boosted.get(Skill.MAGIC) > base.get(Skill.MAGIC) + 4)
		{
			protectedSkills.add(Skill.MAGIC);
		}
		return protectedSkills;
	}

	private static void protect(Client client, Set<Skill> skills, int varbit, Skill... affected)
	{
		if (client.getVarbitValue(varbit) > 0)
		{
			for (Skill skill : affected)
			{
				skills.add(skill);
			}
		}
	}
}
