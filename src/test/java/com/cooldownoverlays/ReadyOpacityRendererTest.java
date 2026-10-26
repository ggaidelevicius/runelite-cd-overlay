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
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ReadyOpacityRendererTest
{
	private static final long SECOND = 1_000_000_000L;
	private static final Rectangle ICON = new Rectangle(16, 16, 36, 32);
	private static final Shape ITEM = silhouette();

	@Test
	public void defaultOpacityKeepsTheExistingOutlineAppearance()
	{
		CooldownOverlaysConfig defaults = new CooldownOverlaysConfig()
		{
			@Override public boolean animateWhenReady() { return true; }
		};
		assertEquals(100, defaults.animationOpacity());
		CooldownOverlaysConfig explicit = new CooldownOverlaysConfig()
		{
			@Override public boolean animateWhenReady() { return true; }
			@Override public int animationOpacity() { return 100; }
		};

		BufferedImage image = render(defaults, ready(), SECOND / 4, ITEM);
		assertTrue(alphaSum(image) > 0);
		assertEquals("The item interior remains untouched", 0, alpha(image, ICON.x + 10, ICON.y + 15));
		assertSameImage(image, render(explicit, ready(), SECOND / 4, ITEM));
	}

	@Test
	public void everyReadyStyleSupportsTransparentPartialAndClampedOpacity()
	{
		Settings opaque = settings(100);
		Settings translucent = settings(35);
		for (ReadyStyle style : ReadyStyle.values())
		{
			opaque.style = style;
			translucent.style = style;
			translucent.opacity = 35;
			BufferedImage full = render(opaque, ready(), 0, ITEM);
			BufferedImage soft = render(translucent, ready(), 0, ITEM);
			assertTrue(style + " still has a visible signal", alphaSum(soft) > 0);
			assertTrue(style + " is measurably less opaque", alphaSum(soft) < alphaSum(full));
			if (style == ReadyStyle.CORNER_MARKER)
			{
				assertTrue("The opaque marker has a black contrast rim", blackAlphaSum(full) > 0);
				assertTrue("The black rim also becomes translucent", blackAlphaSum(soft) < blackAlphaSum(full));
			}
			translucent.opacity = 0;
			assertEquals(style + " is completely hidden, including any black rim", 0,
				alphaSum(render(translucent, ready(), 0, ITEM)));
			translucent.opacity = -50;
			assertEquals(style + " clamps negative opacity", 0,
				alphaSum(render(translucent, ready(), 0, ITEM)));
			translucent.opacity = 150;
			assertSameImage(full, render(translucent, ready(), 0, ITEM));
		}
	}

	@Test
	public void opacityMultipliesExistingIndicatorColourAlpha()
	{
		Settings translucentColour = settings(50);
		translucentColour.color = new Color(180, 80, 210, 128);
		Settings expected = settings(100);
		expected.color = new Color(180, 80, 210, 64);
		BufferedImage image = render(translucentColour, ready(), 0, ITEM);

		assertTrue(alphaSum(image) > 0);
		assertSameImage(render(expected, ready(), 0, ITEM), image);
		translucentColour.color = new Color(180, 80, 210, 0);
		assertEquals(0, alphaSum(render(translucentColour, ready(), 0, ITEM)));
	}

	@Test
	public void outlineOpacityMultipliesTheExistingTimedFade()
	{
		Settings timed = settings(50);
		timed.mode = AnimationMode.TIMED;
		Settings permanent = settings(25);
		assertSameImage(render(permanent, ready(), 2_750_000_000L, ITEM),
			render(timed, ready(), 2_750_000_000L, ITEM));
		assertEquals(0, alphaSum(render(timed, ready(), 3 * SECOND, ITEM)));
	}

	@Test
	public void progressOpacityAndKnownOrUnknownCooldownsRemainIndependent()
	{
		Settings full = settings(100);
		Settings transparent = settings(0);
		full.progressOpacity = 0;
		BufferedImage readyWithTransparentProgress = render(full, ready(), 0, ITEM);
		full.progressOpacity = 100;
		assertSameImage(readyWithTransparentProgress, render(full, ready(), 0, ITEM));
		full.progressOpacity = 60;
		for (DisplayMode mode : DisplayMode.values())
		{
			full.display = mode;
			transparent.display = mode;
			for (boolean known : new boolean[]{true, false})
			{
				Cooldown cooldown = new Cooldown();
				if (known)
				{
					cooldown.start(50, 100, 0);
				}
				else
				{
					cooldown.startUnknown(0);
				}
				assertSameImage(render(full, cooldown, 0, ITEM), render(transparent, cooldown, 0, ITEM));
			}
		}
	}

	@Test
	public void translucentReadyIndicatorsPreserveTheCallerClipAndGraphicsState()
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
			new CooldownRenderer(settings(40)).render(graphics, ICON, ready(), 0, ITEM);

			assertEquals(transform, graphics.getTransform());
			assertEquals(clip, graphics.getClipBounds());
			assertEquals(stroke, graphics.getStroke());
			assertEquals(composite, graphics.getComposite());
			assertEquals(Color.MAGENTA, graphics.getColor());
			assertEquals(font, graphics.getFont());
			assertEquals(RenderingHints.VALUE_ANTIALIAS_OFF,
				graphics.getRenderingHint(RenderingHints.KEY_ANTIALIASING));
			assertTrue(alphaSum(image) > 0);
			for (int x = 43; x < image.getWidth(); x++)
			{
				for (int y = 0; y < image.getHeight(); y++)
				{
					assertEquals("The inherited widget clip remains authoritative", 0, alpha(image, x, y));
				}
			}
		}
		finally
		{
			graphics.dispose();
		}
	}

	private static Settings settings(int opacity)
	{
		Settings settings = new Settings();
		settings.opacity = opacity;
		return settings;
	}

	private static Cooldown ready()
	{
		Cooldown cooldown = new Cooldown();
		cooldown.setReady(0);
		return cooldown;
	}

	private static Shape silhouette()
	{
		Area shape = new Area(new Rectangle2D.Double(4, 8, 28, 22));
		shape.subtract(new Area(new Rectangle2D.Double(15, 17, 5, 5)));
		shape.add(new Area(new Rectangle2D.Double(1, 2, 3, 3)));
		return shape;
	}

	private static BufferedImage render(CooldownOverlaysConfig config, Cooldown cooldown, long nowNanos,
		Shape silhouette)
	{
		BufferedImage image = new BufferedImage(96, 96, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		try
		{
			new CooldownRenderer(config).render(graphics, ICON, cooldown, nowNanos, silhouette);
		}
		finally
		{
			graphics.dispose();
		}
		return image;
	}

	private static int alpha(BufferedImage image, int x, int y)
	{
		return image.getRGB(x, y) >>> 24;
	}

	private static int alphaSum(BufferedImage image)
	{
		int sum = 0;
		for (int pixel : pixels(image))
		{
			sum += pixel >>> 24;
		}
		return sum;
	}

	private static int blackAlphaSum(BufferedImage image)
	{
		int sum = 0;
		for (int pixel : pixels(image))
		{
			if ((pixel & 0xffffff) == 0)
			{
				sum += pixel >>> 24;
			}
		}
		return sum;
	}

	private static int[] pixels(BufferedImage image)
	{
		return image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
	}

	private static void assertSameImage(BufferedImage expected, BufferedImage actual)
	{
		assertArrayEquals(pixels(expected), pixels(actual));
	}

	private static final class Settings implements CooldownOverlaysConfig
	{
		private int opacity = 100;
		private int progressOpacity = 60;
		private Color color = new Color(255, 205, 64);
		private ReadyStyle style = ReadyStyle.STEADY_OUTLINE;
		private BorderStyle border = BorderStyle.ITEM_OUTLINE;
		private AnimationMode mode = AnimationMode.PERMANENT;
		private DisplayMode display = DisplayMode.BOTH;

		@Override public boolean animateWhenReady() { return true; }
		@Override public int animationOpacity() { return opacity; }
		@Override public int circleOpacity() { return progressOpacity; }
		@Override public Color animationColor() { return color; }
		@Override public ReadyStyle readyStyle() { return style; }
		@Override public BorderStyle animationBorderStyle() { return border; }
		@Override public AnimationMode animationMode() { return mode; }
		@Override public int animationDuration() { return 3; }
		@Override public DisplayMode displayMode() { return display; }
	}
}
