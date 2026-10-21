package com.cooldownoverlays;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CooldownBorderTest
{
	private static final long SECOND = 1_000_000_000L;
	private static final Rectangle ICON = new Rectangle(16, 16, 36, 32);
	private static final Shape BOTTLE = ItemOutlineCache.trace(bottleSprite());

	@Test
	public void itemBorderFollowsBottleNeckAndBodyWithoutFillingItsCenterOrCorners()
	{
		BufferedImage image = render(ready(), 0, ICON, BOTTLE,
			config(BorderStyle.ITEM_OUTLINE, 2, AnimationMode.PERMANENT, 3, DisplayMode.BOTH));

		assertTrue("The narrow neck has its own contour", alpha(image, 16 + 15, 16 + 8) > 0);
		assertTrue("The wider body has its own contour", alpha(image, 16 + 7, 16 + 22) > 0);
		assertEquals("No bounding-box edge beside the neck", 0, alpha(image, 16 + 7, 16 + 8));
		assertEquals("The bottle center remains unobscured", 0, alpha(image, 16 + 18, 16 + 22));
		assertEquals(0, alpha(image, 16 + 1, 16 + 1));
		assertEquals(0, alpha(image, 16 + 34, 16 + 1));
		assertEquals(0, alpha(image, 16 + 1, 16 + 31));
		assertEquals(0, alpha(image, 16 + 34, 16 + 31));
	}

	@Test
	public void widerSettingDrawsAMeasurablyThickerContour()
	{
		BufferedImage thin = render(ready(), 0, ICON, BOTTLE,
			config(BorderStyle.ITEM_OUTLINE, 1, AnimationMode.PERMANENT, 3, DisplayMode.BOTH));
		BufferedImage thick = render(ready(), 0, ICON, BOTTLE,
			config(BorderStyle.ITEM_OUTLINE, 6, AnimationMode.PERMANENT, 3, DisplayMode.BOTH));

		assertTrue(visiblePixels(thin) > 0);
		assertTrue("Six-pixel strokes cover more pixels than one-pixel strokes",
			visiblePixels(thick) > visiblePixels(thin));
		assertEquals("One-pixel border leaves this distant neck pixel clear", 0,
			alpha(thin, 16 + 11, 16 + 8));
		assertTrue("Six-pixel border reaches farther out from the same neck",
			alpha(thick, 16 + 11, 16 + 8) > 0);
		assertEquals("Width changes do not fill the bottle", 0,
			alpha(thick, 16 + 18, 16 + 22));
	}

	@Test
	public void nativeItemGeometryIsTranslatedWithoutResizingToWidgetBounds()
	{
		CooldownOverlaysConfig config = config(BorderStyle.ITEM_OUTLINE, 2,
			AnimationMode.PERMANENT, 3, DisplayMode.BOTH);
		BufferedImage original = render(ready(), 0, ICON, BOTTLE, config);
		BufferedImage larger = render(ready(), 0, new Rectangle(16, 16, 60, 48), BOTTLE, config);
		BufferedImage smaller = render(ready(), 0, new Rectangle(16, 16, 10, 8), BOTTLE, config);
		assertImagesEqual(original, larger);
		assertImagesEqual(original, smaller);

		BufferedImage moved = render(ready(), 0, new Rectangle(23, 27, 36, 32), BOTTLE, config);
		for (int x = 0; x < 80; x++)
		{
			for (int y = 0; y < 80; y++)
			{
				assertEquals("The same local contour moves with the item's origin",
					original.getRGB(x, y), moved.getRGB(x + 7, y + 11));
			}
		}
	}

	@Test
	public void outlineRespectsInheritedClipAndPreservesCallerGraphicsState()
	{
		BufferedImage image = new BufferedImage(120, 120, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		AffineTransform transform = new AffineTransform();
		transform.translate(4, 3);
		transform.scale(1.5, 1.5);
		Rectangle clip = new Rectangle(0, 0, 26, 70);
		BasicStroke stroke = new BasicStroke(4);
		AlphaComposite composite = AlphaComposite.SrcOver.derive(0.7f);
		Font font = new Font(Font.MONOSPACED, Font.PLAIN, 9);
		try
		{
			graphics.setTransform(transform);
			graphics.setClip(clip);
			graphics.setStroke(stroke);
			graphics.setComposite(composite);
			graphics.setColor(Color.MAGENTA);
			graphics.setFont(font);
			graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
			new CooldownRenderer(config(BorderStyle.ITEM_OUTLINE, 6,
				AnimationMode.PERMANENT, 3, DisplayMode.BOTH)).render(graphics, ICON, ready(), 0, BOTTLE);

			assertEquals(transform, graphics.getTransform());
			assertEquals(clip, graphics.getClipBounds());
			assertEquals(stroke, graphics.getStroke());
			assertEquals(composite, graphics.getComposite());
			assertEquals(Color.MAGENTA, graphics.getColor());
			assertEquals(font, graphics.getFont());
			assertEquals(RenderingHints.VALUE_ANTIALIAS_OFF,
				graphics.getRenderingHint(RenderingHints.KEY_ANTIALIASING));
			assertTrue(visiblePixels(image) > 0);
			// The clip's transformed right edge is 4 + 26 * 1.5 = 43.
			for (int x = 43; x < image.getWidth(); x++)
			{
				for (int y = 0; y < image.getHeight(); y++)
				{
					assertEquals("The parent clip remains authoritative", 0, alpha(image, x, y));
				}
			}
		}
		finally
		{
			graphics.dispose();
		}
	}

	@Test
	public void silhouetteHaloMayExtendOutsideSpriteBoundsWithinTheParentClip()
	{
		BufferedImage sprite = new BufferedImage(20, 24, BufferedImage.TYPE_INT_ARGB);
		Graphics2D spriteGraphics = sprite.createGraphics();
		try
		{
			spriteGraphics.setColor(Color.WHITE);
			spriteGraphics.fillRect(0, 5, 10, 14);
		}
		finally
		{
			spriteGraphics.dispose();
		}
		BufferedImage image = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		try
		{
			graphics.setClip(new Rectangle(21, 20, 30, 35));
			new CooldownRenderer(config(BorderStyle.ITEM_OUTLINE, 6,
				AnimationMode.PERMANENT, 3, DisplayMode.BOTH)).render(graphics,
				new Rectangle(24, 24, 20, 24), ready(), 0, ItemOutlineCache.trace(sprite));
		}
		finally
		{
			graphics.dispose();
		}
		assertTrue("The contour stroke is not cut off by its source pixel box", alpha(image, 22, 35) > 0);
		assertEquals("The same halo remains clipped by the parent widget", 0, alpha(image, 20, 35));
	}

	@Test
	public void outlineMovesWhileReadyAndHonorsPermanentAndTimedModes()
	{
		Cooldown cooldown = ready();
		CooldownOverlaysConfig timed = config(BorderStyle.ITEM_OUTLINE, 2,
			AnimationMode.TIMED, 3, DisplayMode.BOTH);
		BufferedImage first = render(cooldown, 0, ICON, BOTTLE, timed);
		BufferedImage later = render(cooldown, SECOND / 3, ICON, BOTTLE, timed);
		assertTrue(visiblePixels(first) > 0);
		assertTrue(visiblePixels(later) > 0);
		assertFalse("The activation border advances along the contour",
			Arrays.equals(pixels(first), pixels(later)));
		assertEquals(0, visiblePixels(render(cooldown, 3 * SECOND, ICON, BOTTLE, timed)));
		assertTrue(visiblePixels(render(cooldown, 4 * SECOND, ICON, BOTTLE,
			config(BorderStyle.ITEM_OUTLINE, 2, AnimationMode.TIMED, 5, DisplayMode.BOTH))) > 0);
		assertEquals(0, visiblePixels(render(cooldown, 5 * SECOND, ICON, BOTTLE,
			config(BorderStyle.ITEM_OUTLINE, 2, AnimationMode.TIMED, 5, DisplayMode.BOTH))));
		assertTrue(visiblePixels(render(cooldown, 60 * SECOND, ICON, BOTTLE,
			config(BorderStyle.ITEM_OUTLINE, 2, AnimationMode.PERMANENT, 3, DisplayMode.BOTH))) > 0);
	}

	@Test
	public void unavailableOutlineDoesNotSubstituteAnInventoryRectangle()
	{
		CooldownOverlaysConfig defaults = new CooldownOverlaysConfig()
		{
			@Override
			public boolean animateWhenReady()
			{
				return true;
			}
		};
		assertEquals(0, visiblePixels(render(ready(), 0, ICON, null, defaults)));
		BufferedImage image = new BufferedImage(96, 96, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		try
		{
			new CooldownRenderer(defaults).render(graphics, ICON, ready(), 0);
		}
		finally
		{
			graphics.dispose();
		}
		assertEquals("The legacy render overload also waits for an outline", 0, visiblePixels(image));
		assertTrue("An explicit inventory-slot choice needs no sprite",
			visiblePixels(render(ready(), 0, ICON, null,
				config(BorderStyle.INVENTORY_SLOT, 2, AnimationMode.PERMANENT, 3, DisplayMode.BOTH))) > 0);
	}

	@Test
	public void activeKnownAndUnknownTimersIgnoreReadyBorderStyleWidthAndShape()
	{
		for (DisplayMode mode : DisplayMode.values())
		{
			for (boolean known : new boolean[]{true, false})
			{
				Cooldown cooldown = new Cooldown();
				if (known)
				{
					cooldown.start(30, 60, 0);
				}
				else
				{
					cooldown.startUnknown(0);
				}
				BufferedImage outline = render(cooldown, 0, ICON, BOTTLE,
					config(BorderStyle.ITEM_OUTLINE, 1, AnimationMode.PERMANENT, 3, mode));
				BufferedImage noOutline = render(cooldown, 0, ICON, null,
					config(BorderStyle.ITEM_OUTLINE, 6, AnimationMode.TIMED, 3, mode));
				BufferedImage tile = render(cooldown, 0, ICON, BOTTLE,
					config(BorderStyle.INVENTORY_SLOT, 6, AnimationMode.PERMANENT, 3, mode));
				assertTrue("The active timer still renders", visiblePixels(outline) > 0);
				assertImagesEqual(outline, noOutline);
				assertImagesEqual(outline, tile);
			}
		}
	}

	private static Cooldown ready()
	{
		Cooldown cooldown = new Cooldown();
		cooldown.setReady(0);
		return cooldown;
	}

	private static BufferedImage bottleSprite()
	{
		BufferedImage sprite = new BufferedImage(36, 32, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = sprite.createGraphics();
		try
		{
			graphics.setColor(Color.WHITE);
			graphics.fillRect(15, 3, 6, 11);
			graphics.fillRect(11, 13, 14, 3);
			graphics.fillRect(7, 16, 22, 12);
			graphics.fillRect(10, 28, 16, 2);
		}
		finally
		{
			graphics.dispose();
		}
		return sprite;
	}

	private static BufferedImage render(Cooldown cooldown, long nowNanos, Rectangle bounds,
		Shape outline, CooldownOverlaysConfig config)
	{
		BufferedImage image = new BufferedImage(96, 96, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		try
		{
			new CooldownRenderer(config).render(graphics, bounds, cooldown, nowNanos, outline);
		}
		finally
		{
			graphics.dispose();
		}
		return image;
	}

	private static CooldownOverlaysConfig config(BorderStyle style, int width, AnimationMode animationMode,
		int duration, DisplayMode displayMode)
	{
		return new CooldownOverlaysConfig()
		{
			@Override
			public boolean animateWhenReady()
			{
				return true;
			}

			@Override
			public BorderStyle animationBorderStyle()
			{
				return style;
			}

			@Override
			public int animationBorderWidth()
			{
				return width;
			}

			@Override
			public AnimationMode animationMode()
			{
				return animationMode;
			}

			@Override
			public int animationDuration()
			{
				return duration;
			}

			@Override
			public DisplayMode displayMode()
			{
				return displayMode;
			}
		};
	}

	private static int alpha(BufferedImage image, int x, int y)
	{
		return image.getRGB(x, y) >>> 24;
	}

	private static int visiblePixels(BufferedImage image)
	{
		int count = 0;
		for (int pixel : pixels(image))
		{
			if ((pixel >>> 24) != 0)
			{
				count++;
			}
		}
		return count;
	}

	private static int[] pixels(BufferedImage image)
	{
		return image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
	}

	private static void assertImagesEqual(BufferedImage expected, BufferedImage actual)
	{
		assertArrayEquals(pixels(expected), pixels(actual));
	}
}
