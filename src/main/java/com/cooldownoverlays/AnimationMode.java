package com.cooldownoverlays;

public enum AnimationMode
{
	PERMANENT("While ready"),
	TIMED("For a duration");

	private final String label;

	AnimationMode(String label)
	{
		this.label = label;
	}

	@Override
	public String toString()
	{
		return label;
	}
}
