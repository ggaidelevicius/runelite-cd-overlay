package com.cooldownoverlays;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CooldownRendererTest
{
	private static final Rectangle ICON = new Rectangle(4, 4, 32, 32);

	@Test
	public void timerRoundsUpAndFormatsMinuteBoundaries()
	{
		assertEquals("1", CooldownRenderer.formatSeconds(0.001));
		assertEquals("10", CooldownRenderer.formatSeconds(9.01));
		assertEquals("59", CooldownRenderer.formatSeconds(59));
		assertEquals("1:00", CooldownRenderer.formatSeconds(59.001));
		assertEquals("1:01", CooldownRenderer.formatSeconds(60.01));
		assertEquals("7:00", CooldownRenderer.formatSeconds(420));
	}

	@Test
	public void circleRevealsClockwiseFromTheTop()
	{
		Cooldown cooldown = new Cooldown();
		cooldown.start(15, 20, 0);
		BufferedImage image = render(cooldown, DisplayMode.CIRCLE);

		assertEquals("The first quarter should already be clear", 0, alpha(image, 27, 12));
		assertEquals("The remaining three quarters stay shaded", 153, alpha(image, 12, 12));
		assertEquals(153, alpha(image, 27, 27));
		assertEquals(153, alpha(image, 12, 27));
		assertEquals("The circular sweep must leave the icon corners clear", 0, alpha(image, 4, 4));
	}

	@Test
	public void timerAndCircleCanBeSelectedIndependentlyOrTogether()
	{
		Cooldown cooldown = new Cooldown();
		cooldown.start(15, 20, 0);
		BufferedImage timer = render(cooldown, DisplayMode.TIMER);
		BufferedImage circle = render(cooldown, DisplayMode.CIRCLE);
		BufferedImage both = render(cooldown, DisplayMode.BOTH);

		assertEquals(0, alpha(timer, 12, 12));
		assertEquals(153, alpha(circle, 12, 12));
		assertEquals(153, alpha(both, 12, 12));
		assertTrue("Timer mode should draw legible foreground text", whitePixels(timer) > 0);
		assertEquals("Circle mode should omit timer text", 0, whitePixels(circle));
		assertTrue("Both mode should retain timer text", whitePixels(both) > 0);
	}

	@Test
	public void unknownCooldownShowsADimStaticCircleAndQuestionMarkInLegacyModes()
	{
		Cooldown unknown = new Cooldown();
		unknown.startUnknown(0);
		for (DisplayMode mode : new DisplayMode[]{DisplayMode.TIMER, DisplayMode.CIRCLE, DisplayMode.BOTH})
		{
			BufferedImage image = render(unknown, mode);
			assertEquals(77, alpha(image, 12, 12));
			assertEquals(77, alpha(image, 27, 12));
			assertTrue(whitePixels(image) > 0);
		}
	}

	@Test
	public void clearedCooldownDrawsNothing()
	{
		Cooldown cooldown = new Cooldown();
		cooldown.start(15, 20, 0);
		cooldown.clear();
		BufferedImage image = render(cooldown, DisplayMode.BOTH);
		assertEquals(0, visiblePixels(image));
	}

	@Test
	public void renderingRespectsExistingClipAndPreservesGraphicsState()
	{
		BufferedImage image = new BufferedImage(40, 40, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		Rectangle clip = new Rectangle(4, 4, 16, 32);
		Font font = new Font(Font.MONOSPACED, Font.PLAIN, 9);
		graphics.setClip(clip);
		graphics.setColor(Color.MAGENTA);
		graphics.setFont(font);
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
		Cooldown cooldown = new Cooldown();
		cooldown.start(20, 20, 0);
		try
		{
			new CooldownRenderer(config(DisplayMode.BOTH)).render(graphics, ICON, cooldown, 0);
			assertEquals(clip, graphics.getClipBounds());
			assertEquals(Color.MAGENTA, graphics.getColor());
			assertEquals(font, graphics.getFont());
			assertEquals(RenderingHints.VALUE_ANTIALIAS_OFF,
				graphics.getRenderingHint(RenderingHints.KEY_ANTIALIASING));
			assertTrue(alpha(image, 12, 12) > 0);
			for (int x = 20; x < image.getWidth(); x++)
			{
				for (int y = 0; y < image.getHeight(); y++)
				{
					assertEquals("No overlay pixel may escape the existing clip", 0, alpha(image, x, y));
				}
			}
		}
		finally
		{
			graphics.dispose();
		}
	}

	@Test
	public void invalidOrEmptyBoundsDrawNothing()
	{
		BufferedImage image = new BufferedImage(40, 40, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		Cooldown cooldown = new Cooldown();
		cooldown.start(20, 20, 0);
		try
		{
			new CooldownRenderer(config(DisplayMode.BOTH)).render(
				graphics, new Rectangle(4, 4, 0, 32), cooldown, 0);
		}
		finally
		{
			graphics.dispose();
		}
		assertEquals(0, visiblePixels(image));
	}

	private static BufferedImage render(Cooldown cooldown, DisplayMode mode)
	{
		BufferedImage image = new BufferedImage(40, 40, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		try
		{
			new CooldownRenderer(config(mode)).render(graphics, ICON, cooldown, 0);
		}
		finally
		{
			graphics.dispose();
		}
		return image;
	}

	private static CooldownOverlaysConfig config(DisplayMode mode)
	{
		return new CooldownOverlaysConfig()
		{
			@Override
			public DisplayMode displayMode()
			{
				return mode;
			}
		};
	}

	private static int alpha(BufferedImage image, int x, int y)
	{
		return image.getRGB(x, y) >>> 24;
	}

	private static int whitePixels(BufferedImage image)
	{
		int count = 0;
		for (int x = 0; x < image.getWidth(); x++)
		{
			for (int y = 0; y < image.getHeight(); y++)
			{
				if (image.getRGB(x, y) == Color.WHITE.getRGB())
				{
					count++;
				}
			}
		}
		return count;
	}

	private static int visiblePixels(BufferedImage image)
	{
		int count = 0;
		for (int x = 0; x < image.getWidth(); x++)
		{
			for (int y = 0; y < image.getHeight(); y++)
			{
				if (alpha(image, x, y) > 0)
				{
					count++;
				}
			}
		}
		return count;
	}
}
