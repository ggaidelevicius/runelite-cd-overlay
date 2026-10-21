package com.cooldownoverlays;

import java.awt.Graphics2D;
import java.awt.Shape;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.ui.overlay.WidgetItemOverlay;

final class InventoryCooldownOverlay extends WidgetItemOverlay
{
	private final Client client;
	private final CooldownOverlaysConfig config;
	private final CooldownTracker tracker;
	private final PotionTimers potionTimers;
	private final BoostPotionTimers boostPotionTimers;
	private final CooldownRenderer renderer;
	private final ItemOutlineCache itemOutlines;

	@Inject
	InventoryCooldownOverlay(Client client, CooldownOverlaysConfig config, CooldownTracker tracker,
		PotionTimers potionTimers, BoostPotionTimers boostPotionTimers, CooldownRenderer renderer,
		ItemOutlineCache itemOutlines)
	{
		this.client = client;
		this.config = config;
		this.tracker = tracker;
		this.potionTimers = potionTimers;
		this.boostPotionTimers = boostPotionTimers;
		this.renderer = renderer;
		this.itemOutlines = itemOutlines;
		showOnInventory();
	}

	@Override
	public void renderItemOverlay(Graphics2D graphics, int itemId, WidgetItem widgetItem)
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}
		boolean enabled = itemId == ItemID.IMBUED_HEART && config.imbuedHeart()
			|| itemId == ItemID.SATURATED_HEART && config.saturatedHeart();
		if (enabled)
		{
			render(graphics, widgetItem, tracker.getHeart());
			return;
		}
		PotionItems.Family family = PotionItems.getFamily(itemId);
		if (family != null && family.isEnabled(config))
		{
			render(graphics, widgetItem, potionTimers.get(family.getEffect()));
		}
		BoostPotion boostPotion = BoostPotion.forItem(itemId);
		if (boostPotion != null && boostPotion.isEnabled(config))
		{
			render(graphics, widgetItem, boostPotionTimers.get(boostPotion));
		}
	}

	private void render(Graphics2D graphics, WidgetItem widgetItem, Cooldown cooldown)
	{
		long nowNanos = System.nanoTime();
		if (!renderer.isVisible(cooldown, nowNanos))
		{
			return;
		}
		Shape outline = cooldown.isReady() && config.animationBorderStyle() == BorderStyle.ITEM_OUTLINE
			? itemOutlines.getOutline(widgetItem.getId(), widgetItem.getQuantity()) : null;
		renderer.render(graphics, widgetItem.getCanvasBounds(), cooldown, nowNanos, outline);
	}
}
