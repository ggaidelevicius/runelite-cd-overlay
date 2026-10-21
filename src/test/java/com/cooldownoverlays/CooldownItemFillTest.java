package com.cooldownoverlays;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CooldownItemFillTest
{
	private static final Rectangle ICON = new Rectangle(4, 4, 36, 32);
	private static final BufferedImage SPRITE = sprite();
	private static final Shape SILHOUETTE = ItemOutlineCache.trace(SPRITE);

	@Test
	public void revealRisesThroughTheActualItemHeightAndLeavesHolesAndMarginsClear()
	{
		for (int ticks : new int[]{20, 15, 10, 5})
		{
			Cooldown cooldown = new Cooldown();
			cooldown.start(ticks, 20, 0);
			BufferedImage image = render(cooldown, DisplayMode.ITEM_FILL, SILHOUETTE, 60, 0);
			// The item occupies rows 4 through 23; tile padding is not progress.
			int boundary = 4 + ticks;
			for (int y = 0; y < SPRITE.getHeight(); y++)
			{
				for (int x = 0; x < SPRITE.getWidth(); x++)
				{
					boolean shaded = (SPRITE.getRGB(x, y) >>> 24) != 0 && y < boundary;
					assertEquals("Only unrevealed sprite pixels are shaded at " + ticks + " ticks",
						shaded ? 153 : 0, alpha(image, ICON.x + x, ICON.y + y));
				}
			}
		}
	}

	@Test
	public void revealedPixelsRetainTheirOriginalColoursAndBackgroundIsUntouched()
	{
		BufferedImage image = new BufferedImage(48, 44, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		Color background = new Color(72, 62, 49);
		Cooldown cooldown = new Cooldown();
		cooldown.start(10, 20, 0);
		try
		{
			graphics.setColor(background);
			graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
			graphics.drawImage(SPRITE, ICON.x, ICON.y, null);
			new CooldownRenderer(config(DisplayMode.ITEM_FILL, 60))
				.render(graphics, ICON, cooldown, 0, SILHOUETTE);
		}
		finally
		{
			graphics.dispose();
		}
		assertEquals(SPRITE.getRGB(12, 21), image.getRGB(ICON.x + 12, ICON.y + 21));
		assertEquals(SPRITE.getRGB(25, 20), image.getRGB(ICON.x + 25, ICON.y + 20));
		assertTrue("The unrevealed top remains dimmed",
			image.getRGB(ICON.x + 15, ICON.y + 5) != SPRITE.getRGB(15, 5));
		assertEquals("Transparent holes keep their background", background.getRGB(),
			image.getRGB(ICON.x + 17, ICON.y + 13));
		assertEquals(background.getRGB(), image.getRGB(ICON.x + 2, ICON.y + 2));
	}

	@Test
	public void optionalTimerKeepsItsOriginalPositionOverTheFill()
	{
		Cooldown cooldown = new Cooldown();
		cooldown.start(10, 20, 0);
		BufferedImage fill = render(cooldown, DisplayMode.ITEM_FILL, SILHOUETTE, 60, 0);
		BufferedImage combined = render(cooldown, DisplayMode.TIMER_AND_ITEM_FILL, SILHOUETTE, 60, 0);
		BufferedImage timer = render(cooldown, DisplayMode.TIMER, null, 60, 0);
		assertEquals(0, whitePixels(fill));
		assertTrue(whitePixels(combined) > 0);
		for (int y = 0; y < timer.getHeight(); y++)
		{
			for (int x = 0; x < timer.getWidth(); x++)
			{
				assertEquals("The fill's local translation must not move the text",
					timer.getRGB(x, y) == Color.WHITE.getRGB(),
					combined.getRGB(x, y) == Color.WHITE.getRGB());
			}
		}
	}

	@Test
	public void unknownFillRemainsStaticAndShowsAQuestionMarkInBothModes()
	{
		Cooldown cooldown = new Cooldown();
		cooldown.startUnknown(0);
		for (DisplayMode mode : new DisplayMode[]{DisplayMode.ITEM_FILL, DisplayMode.TIMER_AND_ITEM_FILL})
		{
			BufferedImage initial = render(cooldown, mode, SILHOUETTE, 60, 0);
			BufferedImage later = render(cooldown, mode, SILHOUETTE, 60, 10_000_000_000L);
			assertTrue(whitePixels(initial) > 0);
			assertEquals(77, alpha(initial, ICON.x + 15, ICON.y + 5));
			assertEquals(77, alpha(initial, ICON.x + 9, ICON.y + 22));
			assertEquals(0, alpha(initial, ICON.x + 2, ICON.y + 2));
			assertImagesEqual(initial, later);
		}
	}

	@Test
	public void missingOrEmptySilhouettesDoNotFallBackToATileOrCircle()
	{
		Cooldown cooldown = new Cooldown();
		cooldown.start(20, 20, 0);
		for (Shape shape : new Shape[]{null, new Rectangle()})
		{
			assertEquals(0, visiblePixels(render(cooldown, DisplayMode.ITEM_FILL, shape, 60, 0)));
			assertTrue(whitePixels(render(cooldown, DisplayMode.TIMER_AND_ITEM_FILL, shape, 60, 0)) > 0);
		}
		cooldown.startUnknown(0);
		assertTrue(whitePixels(render(cooldown, DisplayMode.ITEM_FILL, null, 60, 0)) > 0);
	}

	@Test
	public void opacityControlsOnlyTheUnrevealedPixels()
	{
		Cooldown cooldown = new Cooldown();
		cooldown.start(10, 20, 0);
		for (int opacity : new int[]{-10, 0, 60, 100, 110})
		{
			BufferedImage image = render(cooldown, DisplayMode.ITEM_FILL, SILHOUETTE, opacity, 0);
			int alpha = (int) Math.round(Math.max(0, Math.min(100, opacity)) * 255 / 100.0);
			assertEquals(alpha, alpha(image, ICON.x + 15, ICON.y + 5));
			assertEquals(0, alpha(image, ICON.x + 12, ICON.y + 21));
		}
	}

	@Test
	public void readyAndClearedCooldownsHaveNoRemainingMask()
	{
		Cooldown cooldown = new Cooldown();
		cooldown.start(20, 20, 0);
		cooldown.setReady(0);
		assertEquals(0, visiblePixels(render(cooldown, DisplayMode.ITEM_FILL, SILHOUETTE, 60, 0)));
		cooldown.clear();
		assertEquals(0, visiblePixels(render(cooldown, DisplayMode.ITEM_FILL, SILHOUETTE, 60, 0)));
	}

	@Test
	public void fillPreservesGraphicsStateAndInheritedClipping()
	{
		BufferedImage image = new BufferedImage(48, 44, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		Rectangle clip = new Rectangle(4, 4, 16, 32);
		Font font = new Font(Font.MONOSPACED, Font.PLAIN, 9);
		AffineTransform transform = graphics.getTransform();
		Cooldown cooldown = new Cooldown();
		cooldown.start(20, 20, 0);
		try
		{
			graphics.setClip(clip);
			graphics.setFont(font);
			graphics.setColor(Color.MAGENTA);
			graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
			new CooldownRenderer(config(DisplayMode.TIMER_AND_ITEM_FILL, 60))
				.render(graphics, ICON, cooldown, 0, SILHOUETTE);
			assertEquals(transform, graphics.getTransform());
			assertEquals(clip, graphics.getClipBounds());
			assertEquals(font, graphics.getFont());
			assertEquals(Color.MAGENTA, graphics.getColor());
			assertEquals(RenderingHints.VALUE_ANTIALIAS_OFF,
				graphics.getRenderingHint(RenderingHints.KEY_ANTIALIASING));
			assertTrue(visiblePixels(image) > 0);
			for (int y = 0; y < image.getHeight(); y++)
			{
				for (int x = 0; x < image.getWidth(); x++)
				{
					if (!clip.contains(x, y))
					{
						assertEquals(0, alpha(image, x, y));
					}
				}
			}
		}
		finally
		{
			graphics.dispose();
		}
	}

	private static BufferedImage render(Cooldown cooldown, DisplayMode mode, Shape silhouette, int opacity, long now)
	{
		BufferedImage image = new BufferedImage(48, 44, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		try
		{
			new CooldownRenderer(config(mode, opacity)).render(graphics, ICON, cooldown, now, silhouette);
		}
		finally
		{
			graphics.dispose();
		}
		return image;
	}

	private static CooldownOverlaysConfig config(DisplayMode mode, int opacity)
	{
		return new CooldownOverlaysConfig()
		{
			@Override public DisplayMode displayMode() { return mode; }
			@Override public int circleOpacity() { return opacity; }
		};
	}

	private static BufferedImage sprite()
	{
		BufferedImage image = new BufferedImage(36, 32, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		graphics.setColor(new Color(180, 120, 60));
		graphics.fillRect(12, 4, 10, 8);
		graphics.setColor(new Color(60, 190, 100));
		graphics.fillRect(8, 12, 20, 12);
		graphics.setColor(new Color(200, 230, 220));
		graphics.fillRect(12, 19, 3, 4);
		graphics.dispose();
		for (int y = 12; y < 16; y++)
		{
			for (int x = 16; x < 20; x++)
			{
				image.setRGB(x, y, 0);
			}
		}
		image.setRGB(30, 6, new Color(80, 160, 240).getRGB());
		return image;
	}

	private static int alpha(BufferedImage image, int x, int y)
	{
		return image.getRGB(x, y) >>> 24;
	}

	private static int whitePixels(BufferedImage image)
	{
		int count = 0;
		for (int pixel : image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth()))
		{
			if (pixel == Color.WHITE.getRGB()) count++;
		}
		return count;
	}

	private static int visiblePixels(BufferedImage image)
	{
		int count = 0;
		for (int pixel : image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth()))
		{
			if ((pixel >>> 24) != 0) count++;
		}
		return count;
	}

	private static void assertImagesEqual(BufferedImage expected, BufferedImage actual)
	{
		for (int y = 0; y < expected.getHeight(); y++)
		{
			for (int x = 0; x < expected.getWidth(); x++)
			{
				assertEquals(expected.getRGB(x, y), actual.getRGB(x, y));
			}
		}
	}
}
