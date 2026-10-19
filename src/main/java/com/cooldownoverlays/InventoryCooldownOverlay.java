package com.cooldownoverlays;

import java.awt.Graphics2D;
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

	@Inject
	InventoryCooldownOverlay(Client client, CooldownOverlaysConfig config, CooldownTracker tracker,
		PotionTimers potionTimers, BoostPotionTimers boostPotionTimers, CooldownRenderer renderer)
	{
		this.client = client;
		this.config = config;
		this.tracker = tracker;
		this.potionTimers = potionTimers;
		this.boostPotionTimers = boostPotionTimers;
		this.renderer = renderer;
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
			renderer.render(graphics, widgetItem.getCanvasBounds(), tracker.getHeart(), System.nanoTime());
			return;
		}
		PotionItems.Family family = PotionItems.getFamily(itemId);
		if (family != null && family.isEnabled(config))
		{
			renderer.render(graphics, widgetItem.getCanvasBounds(),
				potionTimers.get(family.getEffect()), System.nanoTime());
		}
		BoostPotion boostPotion = BoostPotion.forItem(itemId);
		if (boostPotion != null && boostPotion.isEnabled(config))
		{
			renderer.render(graphics, widgetItem.getCanvasBounds(),
				boostPotionTimers.get(boostPotion), System.nanoTime());
		}
	}
}
