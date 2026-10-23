/*
 * Copyright (c) 2016-2017, Adam <Adam@sigterm.info>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.cooldownoverlays;

/**
 * Estimates the shared combat-boost decay phase from observed natural decreases.
 * Preserve timing follows RuneLite's BoostsPlugin. Call once per game tick, after
 * reporting that tick's observed decay, so prayer changes update qualification.
 */
final class BoostDecayClock
{
	private int lastDecayTick = -1;
	private int expectedDecayTick = -1;
	private boolean preserveBeenActive;

	void clear()
	{
		lastDecayTick = -1;
		expectedDecayTick = -1;
		preserveBeenActive = false;
	}

	void observeDecay(int tick)
	{
		// Several boosted skills may decrease in the same server batch.
		if (tick <= lastDecayTick)
		{
			return;
		}
		lastDecayTick = tick;
		expectedDecayTick = tick + 100;
		preserveBeenActive = false;
	}

	/** Check an observation before ticksToNextDecay expires a cycle at its deadline. */
	boolean isExpectedDecay(int tick, boolean preserveActive)
	{
		if (lastDecayTick == -1 || tick < lastDecayTick || tick > expectedDecayTick)
		{
			return false;
		}
		return tick == lastDecayTick + intervalAt(tick, preserveActive);
	}

	int ticksToNextDecay(int tick, boolean preserveActive)
	{
		if (lastDecayTick == -1)
		{
			return -1;
		}
		int elapsed = tick - lastDecayTick;
		if (elapsed < 0 || tick >= expectedDecayTick)
		{
			// A missed decrease can mean protected stats or an incorrect observation.
			// Never roll an unconfirmed prediction into another decay cycle.
			clear();
			return -1;
		}

		int interval = intervalAt(tick, preserveActive);
		preserveBeenActive = interval == 150;

		if (interval <= elapsed)
		{
			clear();
			return -1;
		}
		expectedDecayTick = lastDecayTick + interval;
		return interval - elapsed;
	}

	private int intervalAt(int tick, boolean preserveActive)
	{
		int elapsed = tick - lastDecayTick;
		if ((preserveActive && (elapsed < 75 || preserveBeenActive)) || elapsed > 125)
		{
			return 150;
		}
		return elapsed > 100 ? 125 : 100;
	}
}
