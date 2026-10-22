package com.cooldownoverlays;

/** Read-only presentation state shared by game timers and the settings preview. */
interface CooldownView
{
	boolean isActive();

	boolean isKnown();

	boolean isReady();

	double getRemainingSeconds(long nowNanos);

	double getRemainingFraction(long nowNanos);

	double getFinishedAgeSeconds(long nowNanos);
}
