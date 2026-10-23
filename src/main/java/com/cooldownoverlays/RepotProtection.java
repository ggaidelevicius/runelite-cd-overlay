package com.cooldownoverlays;

import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.KeyCode;
import net.runelite.api.Menu;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.client.util.Text;

/** Adds an inert left-click choice without changing the native consumption action. */
@Singleton
final class RepotProtection
{
	private static final long MAX_SAMPLE_AGE_NANOS = 3 * Cooldown.TICK_NANOS;
	private final Client client;
	private final CooldownOverlaysConfig config;
	private final RepotProtectionPolicy policy;
	private boolean sampled;
	private long sampledAtNanos;

	@Inject
	RepotProtection(Client client, CooldownOverlaysConfig config, RepotProtectionPolicy policy)
	{
		this.client = client;
		this.config = config;
		this.policy = policy;
	}

	void sample(long nowNanos)
	{
		sampled = true;
		sampledAtNanos = nowNanos;
	}

	void clear()
	{
		sampled = false;
	}

	void updateMenu(long nowNanos)
	{
		if (!config.preventAccidentalRedosing() || !sampled || nowNanos < sampledAtNanos
			|| nowNanos - sampledAtNanos > MAX_SAMPLE_AGE_NANOS
			|| client.getGameState() != GameState.LOGGED_IN || client.isMenuOpen()
			|| client.isKeyPressed(KeyCode.KC_SHIFT) || client.isDraggingWidget())
		{
			return;
		}

		Menu menu = client.getMenu();
		if (menu == null)
		{
			return;
		}
		MenuEntry[] entries = menu.getMenuEntries();
		if (entries.length == 0)
		{
			return;
		}
		MenuEntry top = entries[entries.length - 1];
		if (!isInventoryConsumption(top))
		{
			return;
		}
		ItemContainer inventory = client.getItemContainer(InventoryID.INV);
		Item item = inventory == null ? null : inventory.getItem(top.getParam0());
		if (item == null || item.getId() != top.getItemId() || item.getQuantity() <= 0
			|| !policy.shouldProtect(item.getId(), nowNanos))
		{
			return;
		}

		// RUNELITE actions are handled locally. Keep every original menu entry so
		// right-click use remains available, including with single-button mode.
		menu.createMenuEntry(-1)
			.setOption("Wait")
			.setTarget(top.getTarget())
			.setType(MenuAction.RUNELITE)
			.setIdentifier(top.getIdentifier())
			.setParam0(top.getParam0())
			.setParam1(top.getParam1())
			.setItemId(item.getId())
			.setWorldViewId(top.getWorldViewId())
			.setForceLeftClick(true)
			.onClick(ignored -> { });
	}

	private static boolean isInventoryConsumption(MenuEntry entry)
	{
		if (entry == null || entry.getParam1() != InterfaceID.Inventory.ITEMS
			|| entry.getParam0() < 0 || entry.getParam0() >= 28
			|| (entry.getType() != MenuAction.CC_OP && entry.getType() != MenuAction.CC_OP_LOW_PRIORITY)
			|| !entry.isItemOp() || entry.getOption() == null)
		{
			return false;
		}
		String option = Text.removeTags(entry.getOption());
		return option.equals("Drink") || option.equals("Invigorate") || option.equals("Smell");
	}
}
