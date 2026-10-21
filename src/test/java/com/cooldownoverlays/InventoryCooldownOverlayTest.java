package com.cooldownoverlays;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.lang.reflect.Proxy;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.SpritePixels;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.widgets.WidgetItem;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class InventoryCooldownOverlayTest
{
	private final CooldownTracker tracker = new CooldownTracker();
	private final PotionTimers potions = new PotionTimers();
	private final BoostPotionTimers boosts = new BoostPotionTimers();
	private GameState gameState = GameState.LOGGED_IN;
	private boolean enabled = true;
	private boolean itemsEnabled = true;
	private DisplayMode displayMode = DisplayMode.BOTH;
	private BorderStyle style = BorderStyle.ITEM_OUTLINE;
	private boolean spriteAvailable = true;
	private int spriteRequests;
	private int requestedId;
	private int requestedQuantity;
	private final Client client = (Client) Proxy.newProxyInstance(Client.class.getClassLoader(),
		new Class<?>[]{Client.class}, (proxy, method, args) ->
		{
			if (method.getName().equals("getGameState"))
			{
				return gameState;
			}
			if (method.getName().equals("createItemSprite"))
			{
				spriteRequests++;
				requestedId = (int) args[0];
				requestedQuantity = (int) args[1];
				return spriteAvailable ? sprite() : null;
			}
			throw new UnsupportedOperationException(method.getName());
		});
	private final CooldownOverlaysConfig config = new CooldownOverlaysConfig()
	{
		@Override public DisplayMode displayMode() { return displayMode; }
		@Override public boolean animateWhenReady() { return enabled; }
		@Override public AnimationMode animationMode() { return AnimationMode.PERMANENT; }
		@Override public BorderStyle animationBorderStyle() { return style; }
		@Override public boolean saturatedHeart() { return itemsEnabled; }
		@Override public boolean staminaPotion() { return itemsEnabled; }
		@Override public boolean superStrengthPotion() { return itemsEnabled; }
	};
	private final InventoryCooldownOverlay overlay = new InventoryCooldownOverlay(client, config,
		tracker, potions, boosts, new CooldownRenderer(config), new ItemOutlineCache(client));

	@Test
	public void readyHeartsEffectPotionsAndBoostPotionsUseTheirOwnSprite()
	{
		tracker.getHeart().setReady(0);
		potions.get(PotionEffect.STAMINA).setReady(0);
		boosts.get(BoostPotion.SUPER_STRENGTH).setReady(0);
		int[] items = {ItemID.SATURATED_HEART, ItemID._4DOSESTAMINA, ItemID._4DOSE2STRENGTH};
		for (int item : items)
		{
			assertTrue(visiblePixels(render(item, 2, null)) > 0);
			assertEquals(item, requestedId);
			assertEquals(2, requestedQuantity);
		}
		assertEquals(3, spriteRequests);
	}

	@Test
	public void inactiveHiddenOrRunningIconsDoNotRequestOutlineSprites()
	{
		render(ItemID.SATURATED_HEART, 1, null);
		tracker.getHeart().start(100, 100, System.nanoTime());
		render(ItemID.SATURATED_HEART, 1, null);
		tracker.getHeart().setReady(0);
		enabled = false;
		assertEquals(0, visiblePixels(render(ItemID.SATURATED_HEART, 1, null)));
		enabled = true;
		gameState = GameState.LOGIN_SCREEN;
		assertEquals(0, visiblePixels(render(ItemID.SATURATED_HEART, 1, null)));
		assertEquals(0, spriteRequests);
	}

	@Test
	public void slotStyleDrawsWithoutRequestingASpriteAndMissingOutlineHasNoFallback()
	{
		tracker.getHeart().setReady(0);
		style = BorderStyle.INVENTORY_SLOT;
		assertTrue(visiblePixels(render(ItemID.SATURATED_HEART, 1, null)) > 0);
		assertEquals(0, spriteRequests);
		style = BorderStyle.ITEM_OUTLINE;
		spriteAvailable = false;
		assertEquals(0, visiblePixels(render(ItemID.SATURATED_HEART, 1, null)));
		assertEquals(1, spriteRequests);
		spriteAvailable = true;
		assertTrue(visiblePixels(render(ItemID.SATURATED_HEART, 1, null)) > 0);
		assertEquals(2, spriteRequests);
	}

	@Test
	public void outlineFollowsTheDraggedItemInsteadOfTheOriginalSlot()
	{
		tracker.getHeart().setReady(0);
		BufferedImage image = render(ItemID.SATURATED_HEART, 1, new Rectangle(48, 40, 36, 32));
		assertTrue(visiblePixels(image) > 0);
		for (int x = 0; x < 45; x++)
		{
			for (int y = 0; y < image.getHeight(); y++)
			{
				assertEquals("The original slot must not glow during a drag", 0, image.getRGB(x, y));
			}
		}
	}

	@Test
	public void bothItemFillModesUseTheActiveItemSpriteWithoutReadyAnimations()
	{
		startActiveCooldowns();
		enabled = false;
		style = BorderStyle.INVENTORY_SLOT;
		displayMode = DisplayMode.ITEM_FILL;
		int[] items = {ItemID.SATURATED_HEART, ItemID._4DOSESTAMINA, ItemID._4DOSE2STRENGTH};
		for (int item : items)
		{
			assertTrue(visiblePixels(render(item, 2, null)) > 0);
			assertEquals(item, requestedId);
			assertEquals(2, requestedQuantity);
		}
		assertEquals(3, spriteRequests);

		displayMode = DisplayMode.TIMER_AND_ITEM_FILL;
		for (int item : items)
		{
			assertTrue(visiblePixels(render(item, 2, null)) > 0);
		}
		assertEquals("Switching fill modes must reuse the same item silhouettes", 3, spriteRequests);
	}

	@Test
	public void activeItemFillCachesSilhouettesByItemAndQuantityAcrossFrames()
	{
		startActiveCooldowns();
		displayMode = DisplayMode.ITEM_FILL;
		render(ItemID.SATURATED_HEART, 1, null);
		render(ItemID.SATURATED_HEART, 1, null);
		assertEquals(1, spriteRequests);
		render(ItemID.SATURATED_HEART, 3, null);
		assertEquals(2, spriteRequests);
		assertEquals(ItemID.SATURATED_HEART, requestedId);
		assertEquals(3, requestedQuantity);
		render(ItemID.SATURATED_HEART, 3, null);
		assertEquals(2, spriteRequests);
	}

	@Test
	public void activeItemFillFollowsTheDraggedItem()
	{
		startActiveCooldowns();
		displayMode = DisplayMode.ITEM_FILL;
		BufferedImage image = render(ItemID.SATURATED_HEART, 1, new Rectangle(48, 40, 36, 32));
		assertTrue(visiblePixels(image) > 0);
		for (int x = 0; x < 45; x++)
		{
			for (int y = 0; y < image.getHeight(); y++)
			{
				assertEquals("The cooldown mask must follow the dragged icon", 0, image.getRGB(x, y));
			}
		}
	}

	@Test
	public void disabledLoggedOutAndUnrelatedItemsDoNotRequestFillSprites()
	{
		startActiveCooldowns();
		displayMode = DisplayMode.ITEM_FILL;
		int[] items = {ItemID.SATURATED_HEART, ItemID._4DOSESTAMINA, ItemID._4DOSE2STRENGTH};
		itemsEnabled = false;
		for (int item : items)
		{
			assertEquals(0, visiblePixels(render(item, 1, null)));
		}
		itemsEnabled = true;
		gameState = GameState.LOGIN_SCREEN;
		for (int item : items)
		{
			assertEquals(0, visiblePixels(render(item, 1, null)));
		}
		gameState = GameState.LOGGED_IN;
		assertEquals(0, visiblePixels(render(ItemID.COINS, 1, null)));
		assertEquals(0, spriteRequests);
	}

	@Test
	public void missingActiveFillSpriteRetriesWithoutDrawingAnInventoryTile()
	{
		startActiveCooldowns();
		displayMode = DisplayMode.ITEM_FILL;
		spriteAvailable = false;
		assertEquals(0, visiblePixels(render(ItemID.SATURATED_HEART, 1, null)));
		assertEquals(1, spriteRequests);
		spriteAvailable = true;
		BufferedImage image = render(ItemID.SATURATED_HEART, 1, null);
		assertTrue(visiblePixels(image) > 0);
		assertEquals(2, spriteRequests);
		assertEquals("The inventory tile outside the item silhouette must stay clear", 0, image.getRGB(9, 9));
	}

	private void startActiveCooldowns()
	{
		long nowNanos = System.nanoTime();
		tracker.getHeart().start(100, 200, nowNanos);
		potions.get(PotionEffect.STAMINA).start(100, 200, nowNanos);
		boosts.get(BoostPotion.SUPER_STRENGTH).start(100, 200, nowNanos);
	}

	private BufferedImage render(int item, int quantity, Rectangle dragging)
	{
		BufferedImage image = new BufferedImage(96, 80, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		try
		{
			overlay.renderItemOverlay(graphics, item,
				new WidgetItem(item, quantity, new Rectangle(8, 8, 36, 32), null, dragging));
		}
		finally
		{
			graphics.dispose();
		}
		return image;
	}

	private static SpritePixels sprite()
	{
		BufferedImage image = new BufferedImage(36, 32, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		graphics.setColor(Color.WHITE);
		graphics.fillRect(10, 6, 16, 20);
		graphics.dispose();
		return (SpritePixels) Proxy.newProxyInstance(SpritePixels.class.getClassLoader(),
			new Class<?>[]{SpritePixels.class}, (proxy, method, args) ->
			{
				if (method.getName().equals("toBufferedImage") && method.getParameterCount() == 0)
				{
					return image;
				}
				throw new UnsupportedOperationException(method.getName());
			});
	}

	private static int visiblePixels(BufferedImage image)
	{
		int count = 0;
		for (int y = 0; y < image.getHeight(); y++)
		{
			for (int x = 0; x < image.getWidth(); x++)
			{
				if ((image.getRGB(x, y) >>> 24) != 0)
				{
					count++;
				}
			}
		}
		return count;
	}
}
