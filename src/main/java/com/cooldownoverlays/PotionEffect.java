package com.cooldownoverlays;

import net.runelite.api.Client;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.gameval.VarPlayerID;

/** Server counters for item cooldowns and potion effects. */
enum PotionEffect
{
	SURGE(VarbitID.SURGE_POTION_TIMER, 10, 500),
	OVERLOAD(VarbitID.RAIDS_OVERLOAD_TIMER, 25, 500),
	PRAYER_ENHANCE(VarbitID.RAIDS_PRAYERENHANCE_TIMER, 1, 0),
	SMELLING_SALTS(VarbitID.TOA_MIDRAIDLOOT_STATS_TIMER, 25, 800),
	LIQUID_ADRENALINE(VarbitID.TOA_MIDRAIDLOOT_ENERGY_ACTIVE, 1, 250),
	STAMINA(VarbitID.STAMINA_DURATION, 10, 200),
	ANTIFIRE(VarbitID.ANTIFIRE_POTION, 30, 600),
	SUPER_ANTIFIRE(VarbitID.SUPER_ANTIFIRE_POTION, 20, 300),
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
		if ((this == OVERLOAD || this == PRAYER_ENHANCE)
			&& client.getVarbitValue(VarbitID.RAIDS_CLIENT_INDUNGEON) != 1)
		{
			return 0;
		}
		if (this == ANTIVENOM)
		{
			return client.getVarpValue(variableId);
		}
		if (this == STAMINA && client.getVarbitValue(VarbitID.STAMINA_ACTIVE) != 1)
		{
			return 0;
		}
		return client.getVarbitValue(variableId);
	}

	int remainingBuckets(int rawValue)
	{
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
		return this == OVERLOAD || this == PRAYER_ENHANCE
			|| this == ANTIFIRE || this == SUPER_ANTIFIRE || this == ANTIVENOM;
	}

	boolean hasBucketPhase(int rawValue)
	{
		// Re-dosing antivenom shares the poison counter's existing 30-tick cadence.
		return this == ANTIVENOM ? rawValue != 0 : rawValue > 0;
	}
}
