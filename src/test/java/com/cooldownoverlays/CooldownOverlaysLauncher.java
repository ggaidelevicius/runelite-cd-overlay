package com.cooldownoverlays;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

/** Development client entry point. Starting it is always a user action. */
public final class CooldownOverlaysLauncher
{
	private CooldownOverlaysLauncher()
	{
	}

	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(CooldownOverlaysPlugin.class);
		RuneLite.main(args);
	}
}
