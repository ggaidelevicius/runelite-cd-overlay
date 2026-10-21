package com.cooldownoverlays;

public enum DisplayMode
{
	TIMER("Timer"),
	CIRCLE("Circular sweep"),
	BOTH("Timer and circular sweep"),
	ITEM_FILL("Item fill"),
	TIMER_AND_ITEM_FILL("Timer and item fill");

	private final String label;

	DisplayMode(String label)
	{
		this.label = label;
	}

	boolean usesItemFill()
	{
		return this == ITEM_FILL || this == TIMER_AND_ITEM_FILL;
	}

	boolean showsTimer()
	{
		return this == TIMER || this == BOTH || this == TIMER_AND_ITEM_FILL;
	}

	@Override
	public String toString()
	{
		return label;
	}
}
