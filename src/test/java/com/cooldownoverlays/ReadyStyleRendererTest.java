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

public class ReadyStyleRendererTest
{
	private static final long SECOND = 1_000_000_000L;
	private static final Rectangle ICON = new Rectangle(16, 16, 36, 32);
	private static final Shape BOTTLE = bottleOutline();

	@Test
	public void existingAnimatedBorderRemainsTheDefault()
	{
		assertEquals(ReadyStyle.ANIMATED_BORDER, new CooldownOverlaysConfig() { }.readyStyle());
		assertEquals(7, ReadyStyle.values().length);
	}

	@Test
	public void steadyOutlineDoesNotMoveAndLeavesItemTextureAndTileCornersClear()
	{
		BufferedImage first = render(ReadyStyle.STEADY_OUTLINE, ready(0), 0);
		assertTrue(alphaSum(first) > 0);
		assertSameImage(first, render(ReadyStyle.STEADY_OUTLINE, ready(0), 7 * SECOND));
		assertTrue(alpha(first, 31, 24) > 0);
		assertEquals(0, alpha(first, 34, 38));
		assertEquals(0, alpha(first, ICON.x, ICON.y));
	}

	@Test
	public void softGlowIsStaticAndSofterAndWiderThanASteadyOutline()
	{
		BufferedImage glow = render(ReadyStyle.SOFT_GLOW, ready(0), 0);
		assertSameImage(glow, render(ReadyStyle.SOFT_GLOW, ready(0), 7 * SECOND));
		assertTrue(alpha(glow, 28, 24) > 0);
		assertEquals(0, alpha(render(ReadyStyle.STEADY_OUTLINE, ready(0), 0), 28, 24));
		assertTrue(alpha(glow, 31, 24)
			< alpha(render(ReadyStyle.STEADY_OUTLINE, ready(0), 0), 31, 24));
		assertEquals(0, alpha(glow, 34, 38));
	}

	@Test
	public void breathingGlowChangesBrightnessWithoutMovingTheContour()
	{
		BufferedImage bright = render(ReadyStyle.BREATHING_GLOW, ready(0), 0);
		BufferedImage dim = render(ReadyStyle.BREATHING_GLOW, ready(0), 900_000_000L);
		assertTrue(alphaSum(dim) > 0);
		assertTrue(alphaSum(bright) > alphaSum(dim));
		for (int x = 0; x < bright.getWidth(); x++)
		{
			for (int y = 0; y < bright.getHeight(); y++)
			{
				assertTrue("Breathing only changes alpha at the same contour pixels",
					alpha(dim, x, y) <= alpha(bright, x, y));
			}
		}
	}

	@Test
	public void newMotionStylesStartFromTheReadyTransitionInsteadOfTheWallClock()
	{
		for (ReadyStyle style : new ReadyStyle[]{ReadyStyle.BREATHING_GLOW,
			ReadyStyle.SINGLE_FLASH, ReadyStyle.FLASH_THEN_STEADY})
		{
			assertSameImage(render(style, ready(0), SECOND / 4),
				render(style, ready(17 * SECOND), 17 * SECOND + SECOND / 4));
		}
	}

	@Test
	public void singleFlashFadesOnceAndStopsEvenInPermanentMode()
	{
		Cooldown cooldown = ready(0);
		CooldownRenderer renderer = new CooldownRenderer(config(ReadyStyle.SINGLE_FLASH));
		assertTrue(renderer.isVisible(cooldown, 0));
		assertTrue(alphaSum(render(ReadyStyle.SINGLE_FLASH, cooldown, 0))
			> alphaSum(render(ReadyStyle.SINGLE_FLASH, cooldown, 400_000_000L)));
		assertFalse(renderer.isVisible(cooldown, 800_000_000L));
		assertEquals(0, alphaSum(render(ReadyStyle.SINGLE_FLASH, cooldown, 800_000_000L)));
		assertFalse(renderer.isVisible(cooldown, 180 * SECOND));
		cooldown.setReady(180 * SECOND);
		assertFalse("Repeated ready samples do not retrigger a flash",
			renderer.isVisible(cooldown, 180 * SECOND));
		cooldown.restartReadyAnimation(180 * SECOND);
		assertTrue(renderer.isVisible(cooldown, 180 * SECOND));
	}

	@Test
	public void flashThenSteadySettlesIntoTheSameFixedOutline()
	{
		BufferedImage first = render(ReadyStyle.FLASH_THEN_STEADY, ready(0), 0);
		BufferedImage settled = render(ReadyStyle.FLASH_THEN_STEADY, ready(0), SECOND);
		assertFalse(Arrays.equals(pixels(first), pixels(settled)));
		assertSameImage(settled, render(ReadyStyle.STEADY_OUTLINE, ready(0), SECOND));
		assertSameImage(settled, render(ReadyStyle.FLASH_THEN_STEADY, ready(0), 90 * SECOND));
	}

	@Test
	public void cornerMarkerNeedsNoSpriteAndOnlyUsesACompactBottomRightArea()
	{
		BufferedImage marker = render(config(ReadyStyle.CORNER_MARKER), ready(0), 0, ICON, null);
		assertTrue(alphaSum(marker) > 0);
		assertSameImage(marker, render(config(ReadyStyle.CORNER_MARKER), ready(0), 70 * SECOND, ICON, null));
		for (int x = 0; x < marker.getWidth(); x++)
		{
			for (int y = 0; y < marker.getHeight(); y++)
			{
				if (alpha(marker, x, y) != 0)
				{
					assertTrue(x >= ICON.x + ICON.width - 8 && x < ICON.x + ICON.width);
					assertTrue(y >= ICON.y + ICON.height - 8 && y < ICON.y + ICON.height);
				}
			}
		}
		assertEquals(0, alphaSum(render(config(ReadyStyle.CORNER_MARKER), ready(0), 0,
			new Rectangle(16, 16, 1, 1), null)));
	}

	@Test
	public void everyStyleHonorsDisabledUninitializedAndFutureReadyStates()
	{
		for (ReadyStyle style : ReadyStyle.values())
		{
			CooldownOverlaysConfig disabled = config(style, BorderStyle.ITEM_OUTLINE,
				AnimationMode.PERMANENT, 3, false);
			assertEquals(0, alphaSum(render(disabled, ready(0), 0, ICON, BOTTLE)));
			assertEquals(0, alphaSum(render(style, new Cooldown(), 0)));
			assertEquals(0, alphaSum(render(style, ready(SECOND), 0)));
		}
	}

	@Test
	public void persistentStylesHonorTimedFadeAndExpiry()
	{
		for (ReadyStyle style : ReadyStyle.values())
		{
			if (style == ReadyStyle.SINGLE_FLASH)
			{
				continue;
			}
			CooldownOverlaysConfig timed = config(style, BorderStyle.ITEM_OUTLINE, AnimationMode.TIMED, 3, true);
			CooldownOverlaysConfig permanent = config(style);
			BufferedImage faded = render(timed, ready(0), 2_750_000_000L, ICON, BOTTLE);
			BufferedImage full = render(permanent, ready(0), 2_750_000_000L, ICON, BOTTLE);
			assertTrue(alphaSum(faded) > 0);
			assertTrue(alphaSum(faded) < alphaSum(full));
			assertEquals(0, alphaSum(render(timed, ready(0), 3 * SECOND, ICON, BOTTLE)));
			assertTrue(alphaSum(render(permanent, ready(0), 180 * SECOND, ICON, BOTTLE)) > 0);
		}
	}

	@Test
	public void outlineStylesWaitForTheSpriteAndSlotStylesCanRenderWithoutOne()
	{
		for (ReadyStyle style : ReadyStyle.values())
		{
			if (!style.usesBorder())
			{
				continue;
			}
			assertEquals(0, alphaSum(render(config(style), ready(0), 0, ICON, null)));
			CooldownOverlaysConfig slot = config(style, BorderStyle.INVENTORY_SLOT,
				AnimationMode.PERMANENT, 3, true);
			assertTrue(alphaSum(render(slot, ready(0), 0, ICON, null)) > 0);
		}
	}

	@Test
	public void activeTimersAreIndependentOfEveryReadyStyle()
	{
		for (boolean known : new boolean[]{true, false})
		{
			Cooldown cooldown = new Cooldown();
			if (known)
			{
				cooldown.start(10, 20, 0);
			}
			else
			{
				cooldown.startUnknown(0);
			}
			BufferedImage baseline = render(ReadyStyle.ANIMATED_BORDER, cooldown, 0);
			assertTrue(alphaSum(baseline) > 0);
			for (ReadyStyle style : ReadyStyle.values())
			{
				assertSameImage(baseline, render(style, cooldown, 0));
			}
		}
	}

	@Test
	public void everyStylePreservesCallerGraphicsAndInheritedClip()
	{
		for (ReadyStyle style : ReadyStyle.values())
		{
			BufferedImage image = new BufferedImage(120, 120, BufferedImage.TYPE_INT_ARGB);
			Graphics2D graphics = image.createGraphics();
			AffineTransform transform = AffineTransform.getTranslateInstance(4, 3);
			transform.scale(1.5, 1.5);
			Rectangle clip = new Rectangle(0, 0, 33, 70);
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
				new CooldownRenderer(config(style)).render(graphics, ICON, ready(0), SECOND / 4, BOTTLE);
				assertEquals(transform, graphics.getTransform());
				assertEquals(clip, graphics.getClipBounds());
				assertEquals(stroke, graphics.getStroke());
				assertEquals(composite, graphics.getComposite());
				assertEquals(Color.MAGENTA, graphics.getColor());
				assertEquals(font, graphics.getFont());
				assertEquals(RenderingHints.VALUE_ANTIALIAS_OFF,
					graphics.getRenderingHint(RenderingHints.KEY_ANTIALIASING));
				for (int x = 54; x < image.getWidth(); x++)
				{
					for (int y = 0; y < image.getHeight(); y++)
					{
						assertEquals(0, alpha(image, x, y));
					}
				}
			}
			finally
			{
				graphics.dispose();
			}
		}
	}

	private static CooldownOverlaysConfig config(ReadyStyle style)
	{
		return config(style, BorderStyle.ITEM_OUTLINE, AnimationMode.PERMANENT, 3, true);
	}

	private static CooldownOverlaysConfig config(ReadyStyle style, BorderStyle borderStyle,
		AnimationMode mode, int duration, boolean enabled)
	{
		return new CooldownOverlaysConfig()
		{
			@Override public ReadyStyle readyStyle() { return style; }
			@Override public boolean animateWhenReady() { return enabled; }
			@Override public BorderStyle animationBorderStyle() { return borderStyle; }
			@Override public AnimationMode animationMode() { return mode; }
			@Override public int animationDuration() { return duration; }
		};
	}

	private static Cooldown ready(long at)
	{
		Cooldown cooldown = new Cooldown();
		cooldown.setReady(at);
		return cooldown;
	}

	private static Shape bottleOutline()
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
		return ItemOutlineCache.trace(sprite);
	}

	private static BufferedImage render(ReadyStyle style, Cooldown cooldown, long now)
	{
		return render(config(style), cooldown, now, ICON, BOTTLE);
	}

	private static BufferedImage render(CooldownOverlaysConfig config, Cooldown cooldown, long now,
		Rectangle bounds, Shape outline)
	{
		BufferedImage image = new BufferedImage(96, 96, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		try
		{
			new CooldownRenderer(config).render(graphics, bounds, cooldown, now, outline);
		}
		finally
		{
			graphics.dispose();
		}
		return image;
	}

	private static int[] pixels(BufferedImage image)
	{
		return image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
	}

	private static int alpha(BufferedImage image, int x, int y)
	{
		return image.getRGB(x, y) >>> 24;
	}

	private static long alphaSum(BufferedImage image)
	{
		long sum = 0;
		for (int pixel : pixels(image))
		{
			sum += pixel >>> 24;
		}
		return sum;
	}

	private static void assertSameImage(BufferedImage expected, BufferedImage actual)
	{
		assertArrayEquals(pixels(expected), pixels(actual));
	}
}
