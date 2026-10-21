package com.cooldownoverlays;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.gameval.VarbitID;

/** Tracks item cooldowns and potion protection independently of display settings. */
@Singleton
final class PotionTimers
{
	private final Map<PotionEffect, State> states = new EnumMap<>(PotionEffect.class);
	private final Set<PotionEffect> pendingConfirmedUses = EnumSet.noneOf(PotionEffect.class);
	private boolean initialized;
	private int tickCount;
	private boolean liquidAdrenalineDose;

	PotionTimers()
	{
		for (PotionEffect effect : PotionEffect.values())
		{
			states.put(effect, new State());
		}
	}

	void initialize(Client client, long nowNanos)
	{
		initialize(read(client), client.getVarbitValue(VarbitID.RAIDS_PRAYERENHANCE_RATE), nowNanos);
		suppressUnavailableRaidEffects(client);
	}

	void tick(Client client, long nowNanos)
	{
		tick(read(client), client.getVarbitValue(VarbitID.RAIDS_PRAYERENHANCE_RATE), nowNanos);
		suppressUnavailableRaidEffects(client);
	}

	void onLiquidAdrenalineDose()
	{
		// A confirmed drink can refresh the effect without changing its active flag.
		liquidAdrenalineDose = true;
	}

	void onConfirmedUse(PotionEffect effect, long nowNanos)
	{
		if (!effect.isConfirmedUseEffect())
		{
			throw new IllegalArgumentException("Effect does not use a confirmed-use timer: " + effect);
		}
		State state = states.get(effect);
		state.available = true;
		state.cooldown.start(effect.getStandardTicks(), effect.getStandardTicks(), nowNanos);
		state.knownUntilTick = tickCount + effect.getStandardTicks();
		// The next sample belongs to this message's server batch. Start that sample
		// at full duration instead of prematurely deducting one game tick.
		pendingConfirmedUses.add(effect);
	}

	void initialize(Map<PotionEffect, Integer> samples, long nowNanos)
	{
		initialize(samples, 1, nowNanos);
	}

	void initialize(Map<PotionEffect, Integer> samples, int prayerEnhanceRate, long nowNanos)
	{
		Set<PotionEffect> pendingUses = EnumSet.noneOf(PotionEffect.class);
		pendingUses.addAll(pendingConfirmedUses);
		clear();
		pendingConfirmedUses.addAll(pendingUses);
		initialized = true;
		for (PotionEffect effect : PotionEffect.values())
		{
			if (effect.isConfirmedUseEffect())
			{
				updateConfirmedUse(effect, nowNanos);
			}
			else
			{
				update(effect, samples.getOrDefault(effect, 0), bucketTicks(effect, prayerEnhanceRate), nowNanos, true);
			}
		}
	}

	void tick(Map<PotionEffect, Integer> samples, long nowNanos)
	{
		tick(samples, 1, nowNanos);
	}

	void tick(Map<PotionEffect, Integer> samples, int prayerEnhanceRate, long nowNanos)
	{
		if (!initialized)
		{
			boolean confirmedDose = liquidAdrenalineDose;
			initialize(samples, prayerEnhanceRate, nowNanos);
			if (confirmedDose)
			{
				liquidAdrenalineDose = true;
				update(PotionEffect.LIQUID_ADRENALINE,
					samples.getOrDefault(PotionEffect.LIQUID_ADRENALINE, 0), 1, nowNanos, false);
				liquidAdrenalineDose = false;
			}
			return;
		}
		tickCount++;
		for (PotionEffect effect : PotionEffect.values())
		{
			if (effect.isConfirmedUseEffect())
			{
				updateConfirmedUse(effect, nowNanos);
				continue;
			}
			int rawValue = samples.getOrDefault(effect, 0);
			int buckets = effect.remainingBuckets(rawValue);
			int bucketTicks = bucketTicks(effect, prayerEnhanceRate);
			int minimumTicks = Math.max(1, (buckets - 1) * bucketTicks + 1);
			// Some cooldowns pause in encounters. Never interpolate into another server bucket.
			states.get(effect).cooldown.tick(nowNanos, minimumTicks);
			update(effect, rawValue, bucketTicks, nowNanos, false);
		}
		liquidAdrenalineDose = false;
	}

	private void updateConfirmedUse(PotionEffect effect, long nowNanos)
	{
		State state = states.get(effect);
		if (pendingConfirmedUses.remove(effect))
		{
			state.available = true;
			state.cooldown.start(effect.getStandardTicks(), effect.getStandardTicks(), nowNanos);
			state.knownUntilTick = tickCount + effect.getStandardTicks();
		}
		else if (state.cooldown.isActive())
		{
			if (tickCount >= state.knownUntilTick)
			{
				state.cooldown.setReady(nowNanos);
			}
			else
			{
				state.cooldown.tick(nowNanos);
			}
		}
	}

	private void update(PotionEffect effect, int rawValue, int bucketTicks, long nowNanos, boolean initial)
	{
		State state = states.get(effect);
		initial |= !state.available;
		state.available = true;
		int buckets = bucketTicks == 0 ? 0 : effect.remainingBuckets(rawValue);
		boolean rateChanged = bucketTicks != state.previousBucketTicks;
		boolean changed = rawValue != state.previousRaw || rateChanged;
		if (effect == PotionEffect.LIQUID_ADRENALINE)
		{
			if (buckets == 0)
			{
				state.knownUntilTick = -1;
				state.cooldown.setReady(nowNanos);
			}
			else if (!initial && (liquidAdrenalineDose || state.previousRaw <= 0))
			{
				state.cooldown.start(effect.getStandardTicks(), effect.getStandardTicks(), nowNanos);
				state.knownUntilTick = tickCount + effect.getStandardTicks();
			}
			else if (initial || !state.cooldown.isActive()
				|| (state.cooldown.isKnown() && tickCount >= state.knownUntilTick))
			{
				// The flag cannot recover an existing duration or a missed refresh.
				state.cooldown.startUnknown(nowNanos);
				state.knownUntilTick = -1;
			}
			state.previousRaw = rawValue;
			state.previousBucketTicks = bucketTicks;
			return;
		}
		if (effect.preservesBucketPhase() && (initial || changed))
		{
			if (!effect.hasBucketPhase(rawValue))
			{
				state.nextBucketTick = -1;
			}
			else if (rateChanged || state.nextBucketTick <= tickCount)
			{
				state.nextBucketTick = tickCount + bucketTicks;
			}
		}
		if (buckets == 0)
		{
			state.cooldown.setReady(nowNanos);
		}
		else if (initial || changed || !state.cooldown.isActive())
		{
			int ticks = buckets * bucketTicks;
			if (effect.preservesBucketPhase())
			{
				// Drinking again adds counter units without restarting the underlying cadence.
				ticks = (buckets - 1) * bucketTicks
					+ Math.max(1, state.nextBucketTick - tickCount);
			}
			int total = state.cooldown.getTotalTicks();
			if (!state.cooldown.isActive() || rateChanged || buckets > effect.remainingBuckets(state.previousRaw))
			{
				// Extended and stacked effects can exceed their standard potion duration.
				// Antivenom's initial counter also distinguishes its short and extended variants.
				total = Math.max(effect.getStandardTicks(), ticks);
			}
			state.cooldown.start(ticks, total, nowNanos);
		}
		state.previousRaw = rawValue;
		state.previousBucketTicks = bucketTicks;
	}

	void clear()
	{
		for (State state : states.values())
		{
			clear(state);
		}
		tickCount = 0;
		initialized = false;
		liquidAdrenalineDose = false;
		pendingConfirmedUses.clear();
	}

	void clearOnDeath()
	{
		for (PotionEffect effect : PotionEffect.values())
		{
			if (effect.isConfirmedUseEffect())
			{
				clear(states.get(effect));
				pendingConfirmedUses.remove(effect);
			}
		}
	}

	Cooldown get(PotionEffect effect)
	{
		return states.get(effect).cooldown;
	}

	private void suppressUnavailableRaidEffects(Client client)
	{
		if (client.getVarbitValue(VarbitID.RAIDS_CLIENT_INDUNGEON) != 1)
		{
			// Inactive Chambers counters outside the raid do not indicate usable supplies.
			clear(states.get(PotionEffect.OVERLOAD));
			clear(states.get(PotionEffect.PRAYER_ENHANCE));
		}
	}

	private static void clear(State state)
	{
		state.cooldown.clear();
		state.available = false;
		state.previousRaw = 0;
		state.previousBucketTicks = 0;
		state.nextBucketTick = -1;
		state.knownUntilTick = -1;
	}

	private static Map<PotionEffect, Integer> read(Client client)
	{
		Map<PotionEffect, Integer> samples = new EnumMap<>(PotionEffect.class);
		for (PotionEffect effect : PotionEffect.values())
		{
			samples.put(effect, effect.read(client));
		}
		return samples;
	}

	private static int bucketTicks(PotionEffect effect, int prayerEnhanceRate)
	{
		// The game's buff bar multiplies this counter by its tier-dependent refresh rate.
		return effect == PotionEffect.PRAYER_ENHANCE ? Math.max(0, prayerEnhanceRate) : effect.getBucketTicks();
	}

	private static final class State
	{
		private final Cooldown cooldown = new Cooldown();
		private boolean available;
		private int previousRaw;
		private int previousBucketTicks;
		private int nextBucketTick = -1;
		private int knownUntilTick = -1;
	}
}
