package com.cooldownoverlays;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.lang.reflect.Proxy;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Skill;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.widgets.WidgetItem;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Exercises the item mapping through the same timer, overlay, and re-dose paths used in game. */
public class LmsSuperCombatIntegrationTest
{
	private static final int[] DOSES = {
		ItemID.BR_4DOSE2COMBAT, ItemID.BR_3DOSE2COMBAT, ItemID.BR_2DOSE2COMBAT, ItemID.BR_1DOSE2COMBAT
	};
	private final Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
	private final Map<String, Object> settings = new HashMap<>();
	private final CooldownOverlaysConfig defaults = new CooldownOverlaysConfig() {};
	private final CooldownOverlaysConfig config = (CooldownOverlaysConfig) Proxy.newProxyInstance(
		CooldownOverlaysConfig.class.getClassLoader(), new Class<?>[]{CooldownOverlaysConfig.class},
		(proxy, method, args) -> settings.containsKey(method.getName())
			? settings.get(method.getName()) : method.invoke(defaults, args));
	private final Client client = (Client) Proxy.newProxyInstance(Client.class.getClassLoader(),
		new Class<?>[]{Client.class}, (proxy, method, args) ->
		{
			switch (method.getName())
			{
				case "getGameState": return GameState.LOGGED_IN;
				case "getRealSkillLevel": return 99;
				case "getBoostedSkillLevel": return levels.getOrDefault(args[0], 99);
				case "getTickCount": return 0;
				case "getVarbitValue": return 0;
				default: throw new UnsupportedOperationException(method.getName());
			}
		});
	private final CooldownTracker tracker = new CooldownTracker();
	private final PotionTimers potions = new PotionTimers();
	private final BoostPotionTimers boosts = new BoostPotionTimers();
	private final RepotProtectionPolicy policy = new RepotProtectionPolicy(client, config, tracker, potions, boosts);
	private final InventoryCooldownOverlay overlay = new InventoryCooldownOverlay(client, config,
		tracker, potions, boosts, new CooldownRenderer(config), new ItemOutlineCache(client));

	@Test
	public void lowDefenceDoesNotMakeLmsSuperCombatReady()
	{
		configureTargets();
		levels.put(Skill.ATTACK, 118);
		levels.put(Skill.STRENGTH, 118);
		boosts.tick(client, config, 0);
		assertTrue(boosts.get(BoostPotion.LMS_SUPER_COMBAT).isActive());
		assertFalse(boosts.get(BoostPotion.LMS_SUPER_COMBAT).isReady());
		assertTrue("Ordinary super combat still helps Defence", boosts.get(BoostPotion.SUPER_COMBAT).isReady());

		levels.put(Skill.STRENGTH, 109);
		boosts.tick(client, config, Cooldown.TICK_NANOS);
		assertTrue(boosts.get(BoostPotion.LMS_SUPER_COMBAT).isReady());
		assertFalse(boosts.get(BoostPotion.LMS_SUPER_COMBAT).isActive());
	}

	@Test
	public void defenceOnlyTargetDoesNotCreateAnLmsTimerOrProtectADose()
	{
		settings.put("defenceThreshold", 110);
		settings.put("preventAccidentalRedosing", true);
		boosts.tick(client, config, 0);
		assertFalse(boosts.get(BoostPotion.LMS_SUPER_COMBAT).isActive());
		assertFalse(boosts.get(BoostPotion.LMS_SUPER_COMBAT).isReady());
		assertTrue(boosts.get(BoostPotion.SUPER_COMBAT).isReady());
		boosts.get(BoostPotion.LMS_SUPER_COMBAT).start(100, 100, 0);
		for (int itemId : DOSES)
		{
			assertFalse("No useful configured target for " + itemId, policy.shouldProtect(itemId, 0));
		}
	}

	@Test
	public void everyDoseUsesTheLmsTimerForOverlayAndProtection()
	{
		configureTargets();
		settings.put("preventAccidentalRedosing", true);
		settings.put("displayMode", DisplayMode.CIRCLE);
		levels.put(Skill.ATTACK, 118);
		levels.put(Skill.STRENGTH, 118);
		long nowNanos = System.nanoTime();
		boosts.get(BoostPotion.LMS_SUPER_COMBAT).start(100, 200, nowNanos);
		for (int itemId : DOSES)
		{
			assertTrue("Active overlay for " + itemId, hasOverlay(itemId));
			assertTrue("Protect unnecessary dose " + itemId, policy.shouldProtect(itemId, nowNanos));
		}
		assertFalse("A separate normal timer must not be borrowed", hasOverlay(ItemID._4DOSE2COMBAT));
		assertFalse(policy.shouldProtect(ItemID._4DOSE2COMBAT, nowNanos));

		levels.put(Skill.ATTACK, 109);
		for (int itemId : DOSES)
		{
			assertFalse("A useful Attack dose stays available " + itemId, policy.shouldProtect(itemId, nowNanos));
		}
	}

	@Test
	public void superCombatCheckboxHidesEveryLmsDoseAndLeavesRangingEnabled()
	{
		configureTargets();
		settings.put("preventAccidentalRedosing", true);
		settings.put("displayMode", DisplayMode.CIRCLE);
		settings.put("superCombatPotion", false);
		levels.put(Skill.ATTACK, 118);
		levels.put(Skill.STRENGTH, 118);
		long nowNanos = System.nanoTime();
		boosts.get(BoostPotion.LMS_SUPER_COMBAT).start(100, 200, nowNanos);
		boosts.get(BoostPotion.RANGING).start(100, 200, nowNanos);
		for (int itemId : DOSES)
		{
			assertFalse("Hidden LMS dose " + itemId, hasOverlay(itemId));
			assertFalse("Disabled protection for " + itemId, policy.shouldProtect(itemId, nowNanos));
		}
		assertTrue(hasOverlay(ItemID.BR_4DOSERANGERSPOTION));
		assertEquals(BoostPotion.RANGING, BoostPotion.forItem(ItemID.BR_4DOSERANGERSPOTION));
	}

	private void configureTargets()
	{
		settings.put("attackThreshold", 110);
		settings.put("strengthThreshold", 110);
		settings.put("defenceThreshold", 110);
	}

	private boolean hasOverlay(int itemId)
	{
		BufferedImage image = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		try
		{
			overlay.renderItemOverlay(graphics, itemId,
				new WidgetItem(itemId, 1, new Rectangle(8, 8, 36, 32), null, null));
		}
		finally
		{
			graphics.dispose();
		}
		for (int y = 0; y < image.getHeight(); y++)
		{
			for (int x = 0; x < image.getWidth(); x++)
			{
				if ((image.getRGB(x, y) >>> 24) != 0)
				{
					return true;
				}
			}
		}
		return false;
	}
}
