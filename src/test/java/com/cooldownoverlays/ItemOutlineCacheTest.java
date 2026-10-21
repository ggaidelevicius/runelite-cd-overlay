package com.cooldownoverlays;

import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.image.BufferedImage;
import java.lang.reflect.Proxy;
import net.runelite.api.Client;
import net.runelite.api.Constants;
import net.runelite.api.SpritePixels;
import net.runelite.api.widgets.ItemQuantityMode;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class ItemOutlineCacheTest
{
	private BufferedImage image = visibleSprite();
	private int requests;
	private Object[] lastArguments;
	private final Client client = (Client) Proxy.newProxyInstance(Client.class.getClassLoader(),
		new Class<?>[]{Client.class}, (proxy, method, args) ->
		{
			if (!method.getName().equals("createItemSprite"))
			{
				throw new UnsupportedOperationException(method.getName());
			}
			requests++;
			lastArguments = args.clone();
			return image == null ? null : sprite(image);
		});
	private final ItemOutlineCache cache = new ItemOutlineCache(client);

	@Test
	public void tracesTheActualIrregularSilhouetteAndDisconnectedPixels()
	{
		BufferedImage sprite = blankSprite();
		sprite.setRGB(2, 2, 0xffffffff);
		sprite.setRGB(3, 2, 0xffffffff);
		sprite.setRGB(2, 3, 0xffffffff);
		sprite.setRGB(7, 6, 0xffffffff);
		Shape outline = ItemOutlineCache.trace(sprite);

		assertTrue(outline.contains(2.5, 2.5));
		assertTrue(outline.contains(3.5, 2.5));
		assertTrue(outline.contains(2.5, 3.5));
		assertTrue(outline.contains(7.5, 6.5));
		assertFalse(outline.contains(3.5, 3.5));
		assertFalse(outline.contains(5.5, 4.5));
		assertEquals(new Rectangle(2, 2, 6, 5), outline.getBounds());
	}

	@Test
	public void retainsTransparentHolesInsteadOfFillingTheBoundingBox()
	{
		BufferedImage sprite = blankSprite();
		for (int y = 1; y <= 5; y++)
		{
			for (int x = 1; x <= 5; x++)
			{
				if (x == 1 || x == 5 || y == 1 || y == 5)
				{
					sprite.setRGB(x, y, 0xffffffff);
				}
			}
		}
		Shape outline = ItemOutlineCache.trace(sprite);

		assertTrue(outline.contains(1.5, 3.5));
		assertTrue(outline.contains(5.5, 3.5));
		assertFalse(outline.contains(3.5, 3.5));
		assertFalse(outline.contains(0.5, 3.5));
	}

	@Test
	public void alphaDefinesVisibilityIncludingBlackAndPartiallyTransparentPixels()
	{
		BufferedImage sprite = blankSprite();
		sprite.setRGB(1, 1, 0xff000000);
		sprite.setRGB(2, 1, 0x01ffffff);
		sprite.setRGB(3, 1, 0x00ffffff);
		Shape outline = ItemOutlineCache.trace(sprite);

		assertTrue(outline.contains(1.5, 1.5));
		assertTrue(outline.contains(2.5, 1.5));
		assertFalse(outline.contains(3.5, 1.5));
		assertNull(ItemOutlineCache.trace(blankSprite()));
		assertNull(ItemOutlineCache.trace(null));
	}

	@Test
	public void requestsRawNativeSpriteWithoutCountBorderOrShadow()
	{
		cache.getOutline(123, 17);

		assertArrayEquals(new Object[]{123, 17, 0, 0, ItemQuantityMode.NEVER, false,
			Constants.CLIENT_DEFAULT_ZOOM}, lastArguments);
	}

	@Test
	public void cachesEachExactItemAndQuantityPair()
	{
		Shape first = cache.getOutline(10, 1);
		assertSame(first, cache.getOutline(10, 1));
		assertEquals(1, requests);
		assertNotSame(first, cache.getOutline(11, 1));
		assertNotSame(first, cache.getOutline(10, 2));
		Shape negativeQuantity = cache.getOutline(10, -1);
		assertNotSame(negativeQuantity, cache.getOutline(11, -1));
		assertSame(negativeQuantity, cache.getOutline(10, -1));
		assertEquals(5, requests);
	}

	@Test
	public void missingSpritesAreRetriedUntilAvailable()
	{
		image = null;
		assertNull(cache.getOutline(10, 1));
		assertNull(cache.getOutline(10, 1));
		image = visibleSprite();
		Shape loaded = cache.getOutline(10, 1);

		assertTrue(loaded.contains(2.5, 2.5));
		assertSame(loaded, cache.getOutline(10, 1));
		assertEquals(3, requests);
	}

	@Test
	public void blankSpritesAreRetriedInsteadOfCachingAnEmptyContour()
	{
		image = blankSprite();
		assertNull(cache.getOutline(10, 1));
		assertNull(cache.getOutline(10, 1));
		image.setRGB(4, 5, 0xffffffff);
		Shape loaded = cache.getOutline(10, 1);

		assertTrue(loaded.contains(4.5, 5.5));
		assertSame(loaded, cache.getOutline(10, 1));
		assertEquals(3, requests);
	}

	@Test
	public void boundedCacheEvictsTheLeastRecentlyUsedShape()
	{
		for (int item = 0; item < 128; item++)
		{
			cache.getOutline(item, 1);
		}
		Shape retained = cache.getOutline(0, 1);
		cache.getOutline(128, 1);
		assertSame(retained, cache.getOutline(0, 1));
		assertEquals(129, requests);
		cache.getOutline(1, 1);
		assertEquals(130, requests);
	}

	@Test
	public void clearDiscardsCachedGeometry()
	{
		Shape first = cache.getOutline(10, 1);
		cache.clear();
		assertNotSame(first, cache.getOutline(10, 1));
		assertEquals(2, requests);
	}

	private static BufferedImage blankSprite()
	{
		return new BufferedImage(10, 8, BufferedImage.TYPE_INT_ARGB);
	}

	private static BufferedImage visibleSprite()
	{
		BufferedImage sprite = blankSprite();
		sprite.setRGB(2, 2, 0xffffffff);
		return sprite;
	}

	private static SpritePixels sprite(BufferedImage image)
	{
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
}
