package com.cooldownoverlays;

import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.Constants;
import net.runelite.api.SpritePixels;
import net.runelite.api.widgets.ItemQuantityMode;

/** Client-thread cache of item silhouettes in native sprite coordinates. */
@Singleton
final class ItemOutlineCache
{
	private static final int MAX_ENTRIES = 128;
	private final Client client;
	private final Map<Long, Shape> outlines = new LinkedHashMap<Long, Shape>(MAX_ENTRIES, 0.75f, true)
	{
		@Override
		protected boolean removeEldestEntry(Map.Entry<Long, Shape> eldest)
		{
			return size() > MAX_ENTRIES;
		}
	};

	@Inject
	ItemOutlineCache(Client client)
	{
		this.client = client;
	}

	Shape getOutline(int itemId, int quantity)
	{
		long key = ((long) itemId << 32) | (quantity & 0xffffffffL);
		Shape outline = outlines.get(key);
		if (outline != null)
		{
			return outline;
		}
		// Counts, the game's black border, and the drop shadow are not item pixels.
		SpritePixels sprite = client.createItemSprite(itemId, quantity, 0, 0,
			ItemQuantityMode.NEVER, false, Constants.CLIENT_DEFAULT_ZOOM);
		if (sprite == null)
		{
			return null;
		}
		outline = trace(sprite.toBufferedImage());
		if (outline != null)
		{
			outlines.put(key, outline);
		}
		return outline;
	}

	void clear()
	{
		outlines.clear();
	}

	static Shape trace(BufferedImage sprite)
	{
		if (sprite == null)
		{
			return null;
		}
		Path2D.Double runs = new Path2D.Double();
		boolean visible = false;
		for (int y = 0; y < sprite.getHeight(); y++)
		{
			int x = 0;
			while (x < sprite.getWidth())
			{
				while (x < sprite.getWidth() && (sprite.getRGB(x, y) >>> 24) == 0)
				{
					x++;
				}
				int start = x;
				while (x < sprite.getWidth() && (sprite.getRGB(x, y) >>> 24) != 0)
				{
					x++;
				}
				if (start < x)
				{
					visible = true;
					runs.moveTo(start, y);
					runs.lineTo(x, y);
					runs.lineTo(x, y + 1);
					runs.lineTo(start, y + 1);
					runs.closePath();
				}
			}
		}
		// Area removes shared row-run edges and retains holes and disconnected parts.
		return visible ? new Area(runs) : null;
	}
}
