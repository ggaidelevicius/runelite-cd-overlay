package com.cooldownoverlays;

import javax.inject.Singleton;

/** Tracks the shared heart timer independently of display settings. */
@Singleton
final class CooldownTracker
{
	static final int HEART_BUCKET_TICKS = 10;
	static final int IMBUED_HEART_TICKS = 700;
	static final int SATURATED_HEART_TICKS = 500;
	private final Cooldown heart = new Cooldown();
	private int previousHeart;
	private boolean initialized;

	void initialize(int heartValue, long nowNanos)
	{
		clear();
		initialized = true;
		previousHeart = Math.max(0, heartValue);
		if (previousHeart > 0)
		{
			// Mid-cooldown login cannot identify which heart started the shared timer.
			heart.start(previousHeart * HEART_BUCKET_TICKS, IMBUED_HEART_TICKS, nowNanos);
		}
		else
		{
			heart.setReady(nowNanos);
		}
	}

	void tick(int heartValue, long nowNanos)
	{
		if (!initialized)
		{
			initialize(heartValue, nowNanos);
			return;
		}
		// An unchanged server bucket can also mean the cooldown is paused.
		heart.tick(nowNanos, (previousHeart - 1) * HEART_BUCKET_TICKS + 1);
		update(heartValue, nowNanos);
	}

	private void update(int heartValue, long nowNanos)
	{
		heartValue = Math.max(0, heartValue);
		if (heartValue == 0)
		{
			heart.finish(nowNanos);
		}
		else if (heartValue != previousHeart)
		{
			int ticks = heartValue * HEART_BUCKET_TICKS;
			int total = heart.getTotalTicks();
			if (heartValue > previousHeart || !heart.isActive())
			{
				total = ticks > SATURATED_HEART_TICKS ? IMBUED_HEART_TICKS : SATURATED_HEART_TICKS;
			}
			heart.start(ticks, total, nowNanos);
		}
		previousHeart = heartValue;
	}

	void clear()
	{
		heart.clear();
		previousHeart = 0;
		initialized = false;
	}

	Cooldown getHeart()
	{
		return heart;
	}
}

