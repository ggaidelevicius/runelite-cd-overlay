package com.cooldownoverlays;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.KeyCode;
import net.runelite.api.Menu;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.PostMenuSort;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class RepotProtectionTest
{
	private static final long NOW = 10_000_000_000L;
	private final CooldownTracker tracker = new CooldownTracker();
	private final PotionTimers potions = new PotionTimers();
	private final BoostPotionTimers boosts = new BoostPotionTimers();
	private final List<MenuEntry> entries = new ArrayList<>();
	private final Map<Integer, Item> inventory = new HashMap<>();
	private GameState gameState = GameState.LOGGED_IN;
	private boolean enabled = true;
	private boolean menuOpen;
	private boolean shift;
	private boolean dragging;
	private Widget tentativeDragWidget;
	private int menuReads;
	private int nativeClicks;
	private boolean containerAvailable = true;
	private boolean menuAvailable = true;
	private final CooldownOverlaysConfig config = new CooldownOverlaysConfig()
	{
		@Override public boolean preventAccidentalRedosing() { return enabled; }
	};
	private final ItemContainer container = proxy(ItemContainer.class, (method, args) ->
	{
		if (method.equals("getItem"))
		{
			return inventory.get(args[0]);
		}
		throw new UnsupportedOperationException(method);
	});
	private final Menu menu = proxy(Menu.class, (method, args) ->
	{
		switch (method)
		{
			case "getMenuEntries": return entries.toArray(new MenuEntry[0]);
			case "createMenuEntry":
				assertEquals(-1, args[0]);
				MenuEntry entry = entry();
				entries.add(entry);
				return entry;
			default: throw new UnsupportedOperationException(method);
		}
	});
	private final Client client = proxy(Client.class, (method, args) ->
	{
		switch (method)
		{
			case "getGameState": return gameState;
			case "isMenuOpen": return menuOpen;
			case "isKeyPressed":
				assertEquals(KeyCode.KC_SHIFT, args[0]);
				return shift;
			case "isDraggingWidget": return dragging;
			case "getDraggedWidget": return tentativeDragWidget;
			case "getMenu":
				menuReads++;
				return menuAvailable ? menu : null;
			case "getItemContainer":
				assertEquals(InventoryID.INV, args[0]);
				return containerAvailable ? container : null;
			case "getVarbitValue": return (int) args[0] == VarbitID.IMBUED_HEART_TIMER ? 10 : 0;
			case "getVarpValue": return 0;
			case "getRealSkillLevel":
			case "getBoostedSkillLevel": return 99;
			case "getTickCount": return 1;
			default: throw new UnsupportedOperationException(method);
		}
	});
	private final RepotProtection protection = new RepotProtection(client, config,
		new RepotProtectionPolicy(client, config, tracker, potions, boosts));

	@Before
	public void prepareActiveHeart()
	{
		tracker.getHeart().start(100, 100, NOW);
		setItem(ItemID.SATURATED_HEART, "Invigorate");
		protection.sample(NOW);
	}

	@Test
	public void waitIsLocalAndLeavesEveryOriginalActionAvailable()
	{
		MenuEntry original = entries.get(0).onClick(ignored -> nativeClicks++);
		MenuEntry examine = entry().setOption("Examine").setType(MenuAction.EXAMINE_ITEM);
		entries.add(0, examine);
		protection.updateMenu(NOW);
		assertEquals(3, entries.size());
		assertSame(examine, entries.get(0));
		assertSame(original, entries.get(1));
		assertEquals(MenuAction.CC_OP, original.getType());
		assertEquals("Invigorate", original.getOption());
		MenuEntry wait = entries.get(2);
		assertEquals("Wait", wait.getOption());
		assertEquals(original.getTarget(), wait.getTarget());
		assertEquals(MenuAction.RUNELITE, wait.getType());
		assertEquals(InterfaceID.Inventory.ITEMS, wait.getParam1());
		assertEquals(0, wait.getParam0());
		assertEquals(ItemID.SATURATED_HEART, wait.getItemId());
		assertEquals(original.getWorldViewId(), wait.getWorldViewId());
		assertTrue(wait.isForceLeftClick());
		wait.onClick().accept(wait);
		assertEquals(0, nativeClicks);
		original.onClick().accept(original);
		assertEquals(1, nativeClicks);
	}

	@Test
	public void shiftOpenMenusAndDraggingPreserveTheNativeMenu()
	{
		shift = true;
		assertUntouched();
		shift = false;
		menuOpen = true;
		assertUntouched();
		menuOpen = false;
		dragging = true;
		assertUntouched();
		assertEquals(0, menuReads);
	}

	@Test
	public void anOrdinaryPressBeforeTheDragThresholdStillUsesWait()
	{
		// RuneLite sets a tentative widget before sorting the menu even on a
		// normal click. Only an actual drag should bypass left-click protection.
		tentativeDragWidget = proxy(Widget.class, (method, args) -> null);
		protection.updateMenu(NOW);
		assertEquals(2, entries.size());
		assertEquals("Wait", entries.get(1).getOption());
	}

	@Test
	public void disabledUninitialisedAndStaleStateNeverReadsTheMenu()
	{
		enabled = false;
		assertUntouched();
		enabled = true;
		protection.clear();
		assertUntouched();
		protection.sample(NOW - 3 * Cooldown.TICK_NANOS - 1);
		assertUntouched();
		protection.sample(NOW + 1);
		assertUntouched();
		assertEquals(0, menuReads);
	}

	@Test
	public void otherGameStatesCannotChangeInventoryClicks()
	{
		for (GameState state : GameState.values())
		{
			if (state != GameState.LOGGED_IN)
			{
				gameState = state;
				assertUntouched();
			}
		}
		assertEquals(0, menuReads);
	}

	@Test
	public void nativeDropUseEmptyAndExamineStayOnTop()
	{
		for (String option : new String[]{"Drop", "Use", "Empty", "Examine", "Check", "Eat", "Wait"})
		{
			entries.get(0).setOption(option);
			assertUntouched();
			assertEquals(option, entries.get(0).getOption());
		}
	}

	@Test
	public void unrelatedWidgetActionsAndInvalidSlotsAreIgnored()
	{
		MenuEntry original = entries.get(0);
		original.setParam1(InterfaceID.Bankside.ITEMS);
		assertUntouched();
		original.setParam1(InterfaceID.Inventory.ITEMS).setParam0(-1);
		assertUntouched();
		original.setParam0(28);
		assertUntouched();
		original.setParam0(0).setType(MenuAction.RUNELITE);
		assertUntouched();
		original.setType(MenuAction.WALK);
		assertUntouched();
	}

	@Test
	public void currentInventoryMustMatchTheHoveredDose()
	{
		containerAvailable = false;
		assertUntouched();
		containerAvailable = true;
		inventory.clear();
		assertUntouched();
		inventory.put(0, new Item(ItemID.IMBUED_HEART, 1));
		assertUntouched();
		inventory.put(0, new Item(ItemID.SATURATED_HEART, 0));
		assertUntouched();
	}

	@Test
	public void unknownReadyAndGraceWindowLeaveNativeUseAvailable()
	{
		tracker.getHeart().startUnknown(NOW);
		assertUntouched();
		tracker.getHeart().setReady(NOW);
		assertUntouched();
		tracker.getHeart().start(25, 100, NOW);
		assertUntouched();
		tracker.getHeart().start(26, 100, NOW);
		protection.updateMenu(NOW);
		assertEquals(2, entries.size());
	}

	@Test
	public void drinkSmellAndLowPriorityNativeConsumptionAreSupported()
	{
		setItem(ItemID._4DOSEGOADING, "Drink");
		potions.get(PotionEffect.GOADING).start(100, 100, NOW);
		protection.updateMenu(NOW);
		assertEquals("Wait", entries.get(1).getOption());
		setItem(ItemID.TOA_SUPPLY_STATS_2, "Smell");
		entries.get(0).setType(MenuAction.CC_OP_LOW_PRIORITY);
		potions.get(PotionEffect.SMELLING_SALTS).start(100, 100, NOW);
		protection.updateMenu(NOW);
		assertEquals("Wait", entries.get(1).getOption());
	}

	@Test
	public void anExistingWaitOrAnotherPluginsTopOptionIsNotDuplicated()
	{
		protection.updateMenu(NOW);
		protection.updateMenu(NOW);
		assertEquals(2, entries.size());
		entries.get(1).setOption("Custom action");
		protection.updateMenu(NOW);
		assertEquals(2, entries.size());
	}

	@Test
	public void missingOrEmptyMenuIsHarmless()
	{
		menuAvailable = false;
		assertUntouched();
		menuAvailable = true;
		entries.clear();
		protection.updateMenu(NOW);
		assertTrue(entries.isEmpty());
	}

	@Test
	public void pluginSamplesClearsAndStopsItsProtection() throws Exception
	{
		CooldownOverlaysPlugin plugin = plugin();
		protection.clear();
		plugin.onPostMenuSort(new PostMenuSort());
		assertEquals(1, entries.size());
		plugin.onGameTick(new GameTick());
		plugin.onPostMenuSort(new PostMenuSort());
		assertEquals(2, entries.size());
		setItem(ItemID.SATURATED_HEART, "Invigorate");
		GameStateChanged event = new GameStateChanged();
		event.setGameState(GameState.HOPPING);
		plugin.onGameStateChanged(event);
		plugin.onPostMenuSort(new PostMenuSort());
		assertEquals(1, entries.size());
		plugin.onGameTick(new GameTick());
		inject(plugin, "running", false);
		plugin.onPostMenuSort(new PostMenuSort());
		assertEquals(1, entries.size());
	}

	@Test
	public void protectionRunsAfterOrdinaryMenuSwappers() throws Exception
	{
		CooldownOverlaysPlugin plugin = plugin();
		protection.sample(System.nanoTime());
		EventBus eventBus = new EventBus();
		eventBus.register(plugin);
		eventBus.register(new OrdinarySwapper());
		eventBus.post(new PostMenuSort());
		assertEquals(1, entries.size());
		assertEquals("Drop", entries.get(0).getOption());
	}

	public class OrdinarySwapper
	{
		@Subscribe
		public void onPostMenuSort(PostMenuSort event)
		{
			entries.get(0).setOption("Drop");
		}
	}

	private CooldownOverlaysPlugin plugin() throws Exception
	{
		CooldownOverlaysPlugin plugin = new CooldownOverlaysPlugin();
		inject(plugin, "client", client);
		inject(plugin, "config", config);
		inject(plugin, "tracker", tracker);
		inject(plugin, "potionTimers", potions);
		inject(plugin, "boostPotionTimers", boosts);
		inject(plugin, "repotProtection", protection);
		inject(plugin, "running", true);
		return plugin;
	}

	private static void inject(Object target, String name, Object value) throws Exception
	{
		Field field = target.getClass().getDeclaredField(name);
		field.setAccessible(true);
		field.set(target, value);
	}

	private void assertUntouched()
	{
		MenuEntry original = entries.get(0);
		protection.updateMenu(NOW);
		assertEquals(1, entries.size());
		assertSame(original, entries.get(0));
	}

	private void setItem(int id, String option)
	{
		entries.clear();
		inventory.put(0, new Item(id, 1));
		entries.add(entry().setOption(option).setTarget("<col=ff9040>Item</col>")
			.setType(MenuAction.CC_OP).setIdentifier(1).setParam0(0)
			.setParam1(InterfaceID.Inventory.ITEMS).setItemId(id).setWorldViewId(7));
	}

	private static MenuEntry entry()
	{
		Map<String, Object> values = new HashMap<>();
		return (MenuEntry) Proxy.newProxyInstance(MenuEntry.class.getClassLoader(),
			new Class<?>[]{MenuEntry.class}, (proxy, method, args) ->
			{
				String name = method.getName();
				if (name.startsWith("set"))
				{
					values.put(name.substring(3), args[0]);
					return proxy;
				}
				if (name.startsWith("get"))
				{
					return values.get(name.substring(3));
				}
				switch (name)
				{
					case "isForceLeftClick": return values.getOrDefault("ForceLeftClick", false);
					case "isItemOp": return values.get("Type") == MenuAction.CC_OP
						|| values.get("Type") == MenuAction.CC_OP_LOW_PRIORITY;
					case "onClick":
						if (args == null) { return values.get("OnClick"); }
						values.put("OnClick", args[0]);
						return proxy;
					default: throw new UnsupportedOperationException(name);
				}
			});
	}

	private interface Call
	{
		Object invoke(String method, Object[] args);
	}

	private static <T> T proxy(Class<T> type, Call call)
	{
		return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
			(proxy, method, args) -> call.invoke(method.getName(), args)));
	}
}
