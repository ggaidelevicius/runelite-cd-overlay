package com.cooldownoverlays;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.event.HierarchyEvent;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicLong;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class CooldownPreviewPanelTest
{
	private static final long SECOND = 1_000_000_000L;

	@Test
	public void restartButtonReplaysTheTenSecondCountdown() throws Exception
	{
		SwingUtilities.invokeAndWait(() ->
		{
			AtomicLong now = new AtomicLong();
			CooldownPreviewPanel panel = panel(new PreviewConfig(), now);
			now.set(12 * SECOND);
			panel.refreshSettings();
			assertTrue(panel.timeline().isReady());
			assertTrue(panel.stateText().contains("enable Show ready indicator"));
			button(panel).doClick(0);
			assertTrue(panel.timeline().isActive());
			assertEquals(10, panel.timeline().getRemainingSeconds(now.get()), 0);
			assertEquals("Cooldown: 10s", panel.stateText());
			panel.close();
		});
	}

	@Test
	public void readySettingsReplayReadyPhaseButPreserveTheCooldown() throws Exception
	{
		SwingUtilities.invokeAndWait(() ->
		{
			AtomicLong now = new AtomicLong();
			PreviewConfig config = new PreviewConfig();
			CooldownPreviewPanel panel = panel(config, now);
			now.set(3 * SECOND);
			config.style = ReadyStyle.SINGLE_FLASH;
			panel.refreshSettings();
			assertEquals(7, panel.timeline().getRemainingSeconds(now.get()), 0);
			now.set(12 * SECOND);
			panel.refreshSettings();
			config.enabled = true;
			panel.refreshSettings();
			assertTrue(panel.timeline().isReady());
			assertEquals(0, panel.timeline().getFinishedAgeSeconds(now.get()), 0);
			assertEquals("Ready", panel.stateText());
			now.set(14 * SECOND);
			panel.refreshSettings();
			assertEquals("Ready: indicator finished", panel.stateText());
			config.color = Color.CYAN;
			panel.refreshSettings();
			assertEquals(0, panel.timeline().getFinishedAgeSeconds(now.get()), 0);
			assertEquals("Ready", panel.stateText());
			panel.close();
		});
	}

	@Test
	public void displaySettingsRepaintWithoutReplayingReadyPhase() throws Exception
	{
		SwingUtilities.invokeAndWait(() ->
		{
			AtomicLong now = new AtomicLong();
			PreviewConfig config = new PreviewConfig();
			CooldownPreviewPanel panel = panel(config, now);
			now.set(14 * SECOND);
			panel.refreshSettings();
			config.mode = DisplayMode.ITEM_FILL;
			config.opacity = 30;
			config.timerColor = Color.CYAN;
			panel.refreshSettings();
			assertTrue(panel.timeline().isReady());
			assertEquals(4, panel.timeline().getFinishedAgeSeconds(now.get()), 0);
			panel.close();
		});
	}

	@Test
	public void assetArrivalStartsAFullPreviewAndUsesAnImmutableImageSnapshot() throws Exception
	{
		SwingUtilities.invokeAndWait(() ->
		{
			AtomicLong now = new AtomicLong();
			CooldownPreviewPanel panel = new CooldownPreviewPanel(new PreviewConfig(), null, null, now::get);
			assertFalse(button(panel).isEnabled());
			now.set(20 * SECOND);
			BufferedImage source = sprite();
			panel.setSprite(source, ItemOutlineCache.trace(source));
			assertTrue(button(panel).isEnabled());
			assertEquals(10, panel.timeline().getRemainingSeconds(now.get()), 0);
			BufferedImage before = paint(panel);
			source.setRGB(18, 16, Color.GREEN.getRGB());
			BufferedImage after = paint(panel);
			assertTrue(java.util.Arrays.equals(
				before.getRGB(0, 0, before.getWidth(), before.getHeight(), null, 0, before.getWidth()),
				after.getRGB(0, 0, after.getWidth(), after.getHeight(), null, 0, after.getWidth())));
			panel.close();
		});
	}

	@Test
	public void liveDisplayModeChangesUseTheActualRenderer() throws Exception
	{
		SwingUtilities.invokeAndWait(() ->
		{
			AtomicLong now = new AtomicLong(0);
			PreviewConfig config = new PreviewConfig();
			config.mode = DisplayMode.ITEM_FILL;
			CooldownPreviewPanel panel = panel(config, now);
			now.set(5 * SECOND);
			panel.refreshSettings();
			BufferedImage fill = paint(panel);
			config.mode = DisplayMode.TIMER;
			panel.refreshSettings();
			BufferedImage timer = paint(panel);
			assertNotEquals("A live mode change must change the item preview", checksum(fill), checksum(timer));
			assertEquals(5, panel.timeline().getRemainingSeconds(now.get()), 0);
			panel.close();
		});
	}

	@Test
	public void repaintTimerRunsOnlyWhileShowingAndStopsOnClose() throws Exception
	{
		SwingUtilities.invokeAndWait(() ->
		{
			CooldownPreviewPanel panel = panel(new PreviewConfig(), new AtomicLong());
			assertFalse(panel.isTimerRunning());
			JPanel showingParent = new JPanel()
			{
				@Override
				public boolean isShowing()
				{
					return true;
				}
			};
			showingParent.add(panel);
			panel.addNotify();
			panel.dispatchEvent(new HierarchyEvent(panel, HierarchyEvent.HIERARCHY_CHANGED,
				panel, showingParent, HierarchyEvent.SHOWING_CHANGED));
			assertTrue(panel.isTimerRunning());
			panel.setVisible(false);
			assertFalse(panel.isTimerRunning());
			panel.setVisible(true);
			assertTrue(panel.isTimerRunning());
			panel.close();
			assertFalse(panel.isTimerRunning());
			showingParent.remove(panel);
			panel.removeNotify();
		});
	}

	@Test
	public void visiblePreviewStopsWhenFinishedAndRestartsForConfigChanges() throws Exception
	{
		SwingUtilities.invokeAndWait(() ->
		{
			AtomicLong now = new AtomicLong();
			PreviewConfig config = new PreviewConfig();
			CooldownPreviewPanel panel = panel(config, now);
			JPanel showingParent = new JPanel()
			{
				@Override
				public boolean isShowing()
				{
					return true;
				}
			};
			showingParent.add(panel);
			panel.addNotify();
			panel.dispatchEvent(new HierarchyEvent(panel, HierarchyEvent.HIERARCHY_CHANGED,
				panel, showingParent, HierarchyEvent.SHOWING_CHANGED));
			assertTrue(panel.isTimerRunning());
			now.set(10 * SECOND);
			panel.refreshSettings();
			assertFalse("Disabled ready signals need no continuing repaint loop", panel.isTimerRunning());
			config.enabled = true;
			panel.refreshSettings();
			assertTrue("Changing ready settings replays the timed indicator", panel.isTimerRunning());
			now.set(14 * SECOND);
			panel.refreshSettings();
			assertFalse("An expired timed indicator must release its repaint timer", panel.isTimerRunning());
			config.length = AnimationMode.PERMANENT;
			panel.refreshSettings();
			assertFalse("A steady permanent outline requires no repaint loop", panel.isTimerRunning());
			config.style = ReadyStyle.BREATHING_GLOW;
			panel.refreshSettings();
			assertTrue(panel.isTimerRunning());
			config.style = ReadyStyle.FLASH_THEN_STEADY;
			panel.refreshSettings();
			assertTrue(panel.isTimerRunning());
			now.set(15 * SECOND);
			panel.refreshSettings();
			assertFalse("The settled outline needs no repaint loop", panel.isTimerRunning());
			button(panel).doClick(0);
			assertTrue("Restarting a completed preview resumes the countdown", panel.isTimerRunning());
			panel.close();
			showingParent.remove(panel);
			panel.removeNotify();
		});
	}

	private static CooldownPreviewPanel panel(PreviewConfig config, AtomicLong now)
	{
		BufferedImage image = sprite();
		return new CooldownPreviewPanel(config, image, ItemOutlineCache.trace(image), now::get);
	}

	private static BufferedImage sprite()
	{
		BufferedImage result = new BufferedImage(36, 32, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = result.createGraphics();
		graphics.setColor(Color.RED);
		graphics.fill(new Rectangle2D.Double(10, 6, 16, 24));
		graphics.dispose();
		return result;
	}

	private static BufferedImage paint(CooldownPreviewPanel panel)
	{
		panel.setSize(panel.getPreferredSize());
		layout(panel);
		BufferedImage result = new BufferedImage(panel.getWidth(), panel.getHeight(), BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = result.createGraphics();
		panel.paint(graphics);
		graphics.dispose();
		return result;
	}

	private static void layout(Container container)
	{
		container.doLayout();
		for (Component component : container.getComponents())
		{
			if (component instanceof Container)
			{
				layout((Container) component);
			}
		}
	}

	private static JButton button(Container container)
	{
		for (Component component : container.getComponents())
		{
			if (component instanceof JButton)
			{
				return (JButton) component;
			}
			if (component instanceof Container)
			{
				JButton result = button((Container) component);
				if (result != null)
				{
					return result;
				}
			}
		}
		return null;
	}

	private static long checksum(BufferedImage image)
	{
		long checksum = 1;
		for (int pixel : image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth()))
		{
			checksum = checksum * 31 + pixel;
		}
		return checksum;
	}

	private static final class PreviewConfig implements CooldownOverlaysConfig
	{
		private boolean enabled;
		private ReadyStyle style = ReadyStyle.STEADY_OUTLINE;
		private Color color = Color.YELLOW;
		private DisplayMode mode = DisplayMode.BOTH;
		private int opacity = 60;
		private Color timerColor = Color.WHITE;
		private AnimationMode length = AnimationMode.TIMED;

		@Override public boolean animateWhenReady() { return enabled; }
		@Override public ReadyStyle readyStyle() { return style; }
		@Override public Color animationColor() { return color; }
		@Override public DisplayMode displayMode() { return mode; }
		@Override public int circleOpacity() { return opacity; }
		@Override public Color timerColor() { return timerColor; }
		@Override public AnimationMode animationMode() { return length; }
	}
}
