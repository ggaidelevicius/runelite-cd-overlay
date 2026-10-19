package com.cooldownoverlays;

public enum DisplayMode
{
	TIMER("Timer"),
	CIRCLE("Circular sweep"),
	BOTH("Timer and circular sweep");

	private final String label;

	DisplayMode(String label)
	{
		this.label = label;
	}

	@Override
	public String toString()
	{
		return label;
	}
}
