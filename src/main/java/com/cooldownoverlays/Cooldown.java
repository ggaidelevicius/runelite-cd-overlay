package com.cooldownoverlays;

/** Client-thread state for a server-confirmed cooldown, measured in game ticks. */
final class Cooldown implements CooldownView
{
	static final long TICK_NANOS = 600_000_000L;
	private boolean active;
	private boolean known;
	private int remainingTicks;
	private int totalTicks;
	private long sampledAtNanos;
	private boolean ready;
	private long readyAtNanos;

	void start(int ticks, int durationTicks, long nowNanos)
	{
		ready = false;
		active = true;
		known = true;
		remainingTicks = ticks;
		totalTicks = Math.max(ticks, durationTicks);
		sampledAtNanos = nowNanos;
	}

	void startUnknown(long nowNanos)
	{
		ready = false;
		active = true;
		known = false;
		remainingTicks = 0;
		totalTicks = 0;
		sampledAtNanos = nowNanos;
	}

	void tick(long nowNanos)
	{
		tick(nowNanos, 1);
	}

	void tick(long nowNanos, int minimumTicks)
	{
		if (active && known)
		{
			// The server flag remains authoritative, even if a tick arrives late.
			remainingTicks = Math.max(Math.max(1, minimumTicks), remainingTicks - 1);
		}
		sampledAtNanos = nowNanos;
	}

	void clear()
	{
		ready = false;
		active = false;
		known = false;
		remainingTicks = 0;
		totalTicks = 0;
	}

	void finish(long nowNanos)
	{
		setReady(nowNanos);
	}

	void setReady(long nowNanos)
	{
		if (!ready)
		{
			clear();
			ready = true;
			readyAtNanos = nowNanos;
		}
	}

	@Override
	public double getFinishedAgeSeconds(long nowNanos)
	{
		return ready && nowNanos >= readyAtNanos
			? (nowNanos - readyAtNanos) / 1_000_000_000.0 : -1;
	}

	@Override
	public boolean isReady()
	{
		return ready;
	}

	void restartReadyAnimation(long nowNanos)
	{
		if (ready)
		{
			readyAtNanos = nowNanos;
		}
	}

	@Override
	public boolean isActive()
	{
		return active;
	}

	@Override
	public boolean isKnown()
	{
		return known;
	}

	int getTotalTicks()
	{
		return totalTicks;
	}

	@Override
	public double getRemainingSeconds(long nowNanos)
	{
		if (!active || !known)
		{
			return 0;
		}
		// Smooth within one game tick only; stalled clients cannot imply readiness.
		long elapsed = Math.min(TICK_NANOS, Math.max(0, nowNanos - sampledAtNanos));
		long remaining = remainingTicks * TICK_NANOS - elapsed;
		return Math.max(0.001, remaining / 1_000_000_000.0);
	}

	@Override
	public double getRemainingFraction(long nowNanos)
	{
		if (!active)
		{
			return 0;
		}
		if (!known)
		{
			return 1;
		}
		return Math.min(1, getRemainingSeconds(nowNanos) / (totalTicks * 0.6));
	}
}

