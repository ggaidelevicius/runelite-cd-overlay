package com.cooldownoverlays;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CooldownAnimationTest
{
	private static final long SECOND = 1_000_000_000L;
	private static final Rectangle ICON = new Rectangle(4, 4, 32, 32);

	@Test
	public void readyBorderMovesWithoutObscuringTheCenterOfAnIcon()
	{
		Cooldown cooldown = new Cooldown();
		cooldown.setReady(0);
		BufferedImage first = render(cooldown, 0, true, AnimationMode.PERMANENT);
		BufferedImage later = render(cooldown, SECOND / 3, true, AnimationMode.PERMANENT);
		assertTrue(visiblePixels(first) > 0);
		assertTrue(imagesDiffer(first, later));
		assertEquals(0, first.getRGB(20, 20));
		assertEquals(0, later.getRGB(20, 20));
	}

	@Test
	public void animationsAreOptionalAndDisabledByDefault()
	{
		CooldownOverlaysConfig defaults = new CooldownOverlaysConfig() { };
		assertFalse(defaults.animateWhenReady());
		Cooldown cooldown = new Cooldown();
		cooldown.start(100, 100, 0);
		assertEquals(0, visiblePixels(render(cooldown, 0, false, AnimationMode.TIMED)));
		cooldown.finish(0);
		assertEquals(0, visiblePixels(render(cooldown, 0, false, AnimationMode.TIMED)));
		assertEquals(0, visiblePixels(render(cooldown, 0, false, AnimationMode.PERMANENT)));
	}

	@Test
	public void activeTimersNeverAnimateEvenWhenTheReadyAnimationIsEnabled()
	{
		Cooldown cooldown = new Cooldown();
		for (AnimationMode mode : AnimationMode.values())
		{
			cooldown.start(100, 100, 0);
			assertEquals(0, visiblePixels(render(cooldown, 0, true, mode)));
			cooldown.startUnknown(0);
			BufferedImage enabled = render(cooldown, SECOND, true, mode);
			BufferedImage disabled = render(cooldown, SECOND, false, mode);
			assertFalse(imagesDiffer(enabled, disabled));
		}
	}

	@Test
	public void timedReadyAnimationMovesThenExpiresAtTheConfiguredDuration()
	{
		Cooldown cooldown = new Cooldown();
		cooldown.start(1, 1, 0);
		cooldown.finish(0);
		BufferedImage first = render(cooldown, 0, true, AnimationMode.TIMED);
		BufferedImage later = render(cooldown, SECOND / 3, true, AnimationMode.TIMED);
		assertTrue(visiblePixels(first) > 0);
		assertTrue(imagesDiffer(first, later));
		assertEquals(0, first.getRGB(20, 20));
		assertEquals(0, visiblePixels(render(cooldown, 3 * SECOND, true, AnimationMode.TIMED)));
		assertTrue(visiblePixels(render(cooldown, 4 * SECOND, config(true, AnimationMode.TIMED, 5))) > 0);
		assertEquals(0, visiblePixels(render(cooldown, 5 * SECOND, config(true, AnimationMode.TIMED, 5))));
	}

	@Test
	public void permanentAnimationRemainsVisibleUntilTheItemIsUsed()
	{
		Cooldown cooldown = new Cooldown();
		cooldown.setReady(0);
		assertTrue(visiblePixels(render(cooldown, 60 * SECOND, true, AnimationMode.PERMANENT)) > 0);
		cooldown.start(100, 100, 60 * SECOND);
		assertEquals(0, visiblePixels(render(cooldown, 60 * SECOND, true, AnimationMode.PERMANENT)));
	}

	@Test
	public void initialConfirmedReadySampleCanAnimateButUninitializedStateCannot()
	{
		Cooldown cooldown = new Cooldown();
		assertEquals(0, visiblePixels(render(cooldown, 0, true, AnimationMode.PERMANENT)));
		cooldown.finish(SECOND);
		assertTrue(cooldown.isReady());
		assertTrue(visiblePixels(render(cooldown, SECOND, true, AnimationMode.TIMED)) > 0);
	}

	@Test
	public void readySamplesDoNotRetriggerOrExtendTheAnimation()
	{
		Cooldown cooldown = new Cooldown();
		cooldown.start(1, 1, 0);
		cooldown.finish(0);
		cooldown.finish(2 * SECOND);
		assertEquals(2, cooldown.getFinishedAgeSeconds(2 * SECOND), 0.00001);
		assertEquals(0, visiblePixels(render(cooldown, 3 * SECOND, true, AnimationMode.TIMED)));
	}

	@Test
	public void lifecycleClearAndANewDoseCancelTheReadyAnimation()
	{
		Cooldown cooldown = new Cooldown();
		cooldown.finish(0);
		cooldown.clear();
		assertFalse(cooldown.isReady());
		assertEquals(0, visiblePixels(render(cooldown, SECOND, true, AnimationMode.TIMED)));
		cooldown.start(1, 1, SECOND);
		cooldown.finish(SECOND);
		cooldown.start(100, 100, 2 * SECOND);
		assertFalse(cooldown.isReady());
		assertEquals(-1, cooldown.getFinishedAgeSeconds(2 * SECOND), 0);
		assertEquals(0, visiblePixels(render(cooldown, 2 * SECOND, true, AnimationMode.TIMED)));
		cooldown.finish(3 * SECOND);
		assertTrue(visiblePixels(render(cooldown, 3 * SECOND, true, AnimationMode.TIMED)) > 0);
	}

	@Test
	public void readyAnimationRespectsTheParentClipAndGraphicsState()
	{
		BufferedImage image = new BufferedImage(40, 40, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		Rectangle clip = new Rectangle(4, 4, 16, 32);
		Cooldown cooldown = new Cooldown();
		cooldown.start(1, 1, 0);
		cooldown.finish(0);
		try
		{
			graphics.setClip(clip);
			graphics.setColor(Color.MAGENTA);
			new CooldownRenderer(config(true, AnimationMode.PERMANENT, 3)).render(graphics, ICON, cooldown, 0);
			assertEquals(clip, graphics.getClipBounds());
			assertEquals(Color.MAGENTA, graphics.getColor());
			assertTrue(visiblePixels(image) > 0);
			for (int x = 20; x < 40; x++)
			{
				for (int y = 0; y < 40; y++)
				{
					assertEquals(0, image.getRGB(x, y));
				}
			}
		}
		finally
		{
			graphics.dispose();
		}
	}

	private static BufferedImage render(Cooldown cooldown, long nowNanos, boolean enabled, AnimationMode mode)
	{
		return render(cooldown, nowNanos, config(enabled, mode, 3));
	}

	private static BufferedImage render(Cooldown cooldown, long nowNanos, CooldownOverlaysConfig config)
	{
		BufferedImage image = new BufferedImage(40, 40, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		try
		{
			new CooldownRenderer(config).render(graphics, ICON, cooldown, nowNanos);
		}
		finally
		{
			graphics.dispose();
		}
		return image;
	}

	private static CooldownOverlaysConfig config(boolean enabled, AnimationMode mode, int duration)
	{
		return new CooldownOverlaysConfig()
		{
			@Override
			public BorderStyle animationBorderStyle()
			{
				return BorderStyle.INVENTORY_SLOT;
			}

			@Override
			public DisplayMode displayMode()
			{
				return DisplayMode.CIRCLE;
			}

			@Override
			public int circleOpacity()
			{
				return 0;
			}

			@Override
			public boolean animateWhenReady()
			{
				return enabled;
			}

			@Override
			public AnimationMode animationMode()
			{
				return mode;
			}

			@Override
			public int animationDuration()
			{
				return duration;
			}
		};
	}

	private static int visiblePixels(BufferedImage image)
	{
		int count = 0;
		for (int x = 0; x < image.getWidth(); x++)
		{
			for (int y = 0; y < image.getHeight(); y++)
			{
				if ((image.getRGB(x, y) >>> 24) > 0)
				{
					count++;
				}
			}
		}
		return count;
	}

	private static boolean imagesDiffer(BufferedImage first, BufferedImage second)
	{
		for (int x = 0; x < first.getWidth(); x++)
		{
			for (int y = 0; y < first.getHeight(); y++)
			{
				if (first.getRGB(x, y) != second.getRGB(x, y))
				{
					return true;
				}
			}
		}
		return false;
	}
}
