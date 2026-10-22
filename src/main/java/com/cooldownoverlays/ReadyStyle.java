package com.cooldownoverlays;

public enum ReadyStyle
{
	ANIMATED_BORDER("Animated border"),
	STEADY_OUTLINE("Steady outline"),
	SOFT_GLOW("Soft glow"),
	CORNER_MARKER("Corner marker"),
	BREATHING_GLOW("Breathing glow"),
	SINGLE_FLASH("Single flash"),
	FLASH_THEN_STEADY("Flash, then steady");

	private final String label;

	ReadyStyle(String label)
	{
		this.label = label;
	}

	boolean usesBorder()
	{
		return this != CORNER_MARKER;
	}

	@Override
	public String toString()
	{
		return label;
	}
}
