package com.cooldownoverlays;

import net.runelite.api.Client;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.gameval.VarPlayerID;

/** Server counters and confirmed-use durations for item cooldowns and potion effects. */
enum PotionEffect
{
	SURGE(VarbitID.SURGE_POTION_TIMER, 10, 500),
	OVERLOAD(VarbitID.RAIDS_OVERLOAD_TIMER, 25, 500),
	NMZ_OVERLOAD(VarbitID.NZONE_OVERLOAD_POTION_EFFECTS, 25, 500),
	BLIGHTED_OVERLOAD(VarbitID.DEADMAN_OVERLOAD_POTION_EFFECTS, 25, 500),
	PRAYER_ENHANCE(VarbitID.RAIDS_PRAYERENHANCE_TIMER, 1, 0),
	SMELLING_SALTS(VarbitID.TOA_MIDRAIDLOOT_STATS_TIMER, 25, 800),
	LIQUID_ADRENALINE(VarbitID.TOA_MIDRAIDLOOT_ENERGY_ACTIVE, 1, 250),
	SILK_DRESSING(-1, 1, 100),
	BLESSED_CRYSTAL_SCARAB(-1, 1, 40),
	STAMINA(VarbitID.STAMINA_DURATION, 10, 200),
	GOADING(VarbitID.GOADING_POTION_TIMER, 6, 600),
	MENAPHITE_REMEDY(VarbitID.STATRENEWAL_POTION_TIMER, 25, 500),
	MOONLIGHT(VarbitID.MOONLIGHT_POTION_TIME, 1, 500),
	PRAYER_REGENERATION(VarbitID.PRAYER_REGENERATION_POTION_TIMER, 12, 800),
	ANTIFIRE(VarbitID.ANTIFIRE_POTION, 30, 600),
	SUPER_ANTIFIRE(VarbitID.SUPER_ANTIFIRE_POTION, 20, 300),
	ANTIPOISON(VarPlayerID.POISON, 30, 0),
	ANTIVENOM(VarPlayerID.POISON, 30, 0),
	DIVINE_ATTACK(VarbitID.DIVINEATTACK_POTION_TIME, 1, 500),
	DIVINE_STRENGTH(VarbitID.DIVINESTRENGTH_POTION_TIME, 1, 500),
	DIVINE_DEFENCE(VarbitID.DIVINEDEFENCE_POTION_TIME, 1, 500),
	DIVINE_RANGING(VarbitID.DIVINERANGE_POTION_TIME, 1, 500),
	DIVINE_MAGIC(VarbitID.DIVINEMAGIC_POTION_TIME, 1, 500),
	DIVINE_COMBAT(VarbitID.DIVINECOMBAT_POTION_TIME, 1, 500),
	DIVINE_BASTION(VarbitID.DIVINEBASTION_POTION_TIME, 1, 500),
	DIVINE_BATTLEMAGE(VarbitID.DIVINEBATTLEMAGE_POTION_TIME, 1, 500);

	private static final int VENOM_IMMUNITY_CUTOFF = -38;
	private final int variableId;
	private final int bucketTicks;
	private final int standardTicks;

	PotionEffect(int variableId, int bucketTicks, int standardTicks)
	{
		this.variableId = variableId;
		this.bucketTicks = bucketTicks;
		this.standardTicks = standardTicks;
	}

	int read(Client client)
	{
		if (isConfirmedUseEffect())
		{
			return 0;
		}
		if ((this == OVERLOAD || this == PRAYER_ENHANCE)
			&& client.getVarbitValue(VarbitID.RAIDS_CLIENT_INDUNGEON) != 1)
		{
			return 0;
		}
		if (this == ANTIVENOM || this == ANTIPOISON)
		{
			return client.getVarpValue(variableId);
		}
		if (this == STAMINA && client.getVarbitValue(VarbitID.STAMINA_ACTIVE) != 1)
		{
			return 0;
		}
		if (this == MOONLIGHT)
		{
			int ticks = client.getVarbitValue(variableId);
			int defenceTicks = client.getVarbitValue(VarbitID.DIVINEDEFENCE_POTION_TIME);
			// Re-dosing can leave the Moonlight timer one tick behind its linked
			// Defence timer. Do not infer Moonlight from an unrelated divine dose.
			if (ticks > 0 && defenceTicks > ticks + 1)
			{
				// A longer actual divine effect supersedes the Moonlight Defence timer.
				return 0;
			}
			return ticks > 0 && defenceTicks == ticks + 1 ? ticks + 1 : ticks;
		}
		return client.getVarbitValue(variableId);
	}

	int remainingBuckets(int rawValue)
	{
		if (this == ANTIPOISON)
		{
			// Venom protection also prevents poison, including its eventual poison-only tail.
			return rawValue < 0 ? -rawValue : 0;
		}
		if (this == ANTIVENOM)
		{
			// Values -38 through -1 are the poison-immunity tail, not venom protection.
			return rawValue >= VENOM_IMMUNITY_CUTOFF ? 0 : -(rawValue - VENOM_IMMUNITY_CUTOFF);
		}
		return Math.max(0, rawValue);
	}

	int getBucketTicks()
	{
		return bucketTicks;
	}

	int getStandardTicks()
	{
		return standardTicks;
	}

	boolean preservesBucketPhase()
	{
		return this == OVERLOAD || this == NMZ_OVERLOAD || this == BLIGHTED_OVERLOAD
			|| this == PRAYER_ENHANCE || this == ANTIFIRE || this == SUPER_ANTIFIRE
			|| this == ANTIPOISON || this == ANTIVENOM;
	}

	boolean hasBucketPhase(int rawValue)
	{
		// Re-dosing antivenom shares the poison counter's existing 30-tick cadence.
		return this == ANTIVENOM || this == ANTIPOISON ? rawValue != 0 : rawValue > 0;
	}

	boolean isConfirmedUseEffect()
	{
		return this == SILK_DRESSING || this == BLESSED_CRYSTAL_SCARAB;
	}
}
