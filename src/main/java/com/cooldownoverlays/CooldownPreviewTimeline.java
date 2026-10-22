package com.cooldownoverlays;

/** A wall-clock demonstration, entirely separate from game cooldown state. */
final class CooldownPreviewTimeline implements CooldownView
{
	static final long DURATION_NANOS = 10_000_000_000L;
	private long startedAtNanos;
	private long readyAtNanos;
	private boolean ready;

	CooldownPreviewTimeline(long nowNanos)
	{
		restart(nowNanos);
	}

	void restart(long nowNanos)
	{
		startedAtNanos = nowNanos;
		readyAtNanos = nowNanos + DURATION_NANOS;
		ready = false;
	}

	void update(long nowNanos)
	{
		if (!ready && nowNanos - startedAtNanos >= DURATION_NANOS)
		{
			ready = true;
		}
	}

	void restartReady(long nowNanos)
	{
		update(nowNanos);
		if (ready)
		{
			readyAtNanos = nowNanos;
		}
	}

	@Override
	public boolean isActive()
	{
		return !ready;
	}

	@Override
	public boolean isKnown()
	{
		return true;
	}

	@Override
	public boolean isReady()
	{
		return ready;
	}

	@Override
	public double getRemainingSeconds(long nowNanos)
	{
		return ready ? 0 : Math.max(0, DURATION_NANOS - Math.max(0, nowNanos - startedAtNanos))
			/ 1_000_000_000.0;
	}

	@Override
	public double getRemainingFraction(long nowNanos)
	{
		return getRemainingSeconds(nowNanos) / 10.0;
	}

	@Override
	public double getFinishedAgeSeconds(long nowNanos)
	{
		return ready ? Math.max(0, nowNanos - readyAtNanos) / 1_000_000_000.0 : -1;
	}
}
