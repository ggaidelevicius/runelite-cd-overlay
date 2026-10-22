package com.cooldownoverlays;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.HierarchyEvent;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.function.LongSupplier;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import net.runelite.client.ui.ColorScheme;

/** EDT-owned, replayable preview of the same renderer used by inventory items. */
final class CooldownPreviewPanel extends JPanel
{
	private static final int PADDING = 10;
	private final CooldownOverlaysConfig config;
	private final CooldownRenderer renderer;
	private final LongSupplier clock;
	private final CooldownPreviewTimeline timeline;
	private final Timer timer;
	private final JLabel state = label("Loading potion icon");
	private final JLabel hint = label("Simulated 10-second cooldown; no supplies used.");
	private final JButton restart = new JButton("Restart preview");
	private final JPanel icons;
	private BufferedImage sprite;
	private Shape silhouette;
	private Object[] previousReadySettings;
	private boolean closed;

	CooldownPreviewPanel(CooldownOverlaysConfig config, BufferedImage sprite, Shape silhouette,
		LongSupplier clock)
	{
		this.config = config;
		this.renderer = new CooldownRenderer(config);
		this.clock = clock;
		this.timeline = new CooldownPreviewTimeline(clock.getAsLong());
		this.previousReadySettings = readySettings();
		setLayout(new BorderLayout(0, 8));
		setBackground(ColorScheme.DARK_GRAY_COLOR);
		setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));

		JLabel title = label("Super combat potion");
		title.setFont(title.getFont().deriveFont(Font.BOLD));
		add(title, BorderLayout.NORTH);
		icons = new JPanel()
		{
			@Override
			protected void paintComponent(Graphics graphics)
			{
				super.paintComponent(graphics);
				paintIcons((Graphics2D) graphics);
			}
		};
		icons.setOpaque(false);
		icons.setPreferredSize(new Dimension(330, 138));
		add(icons, BorderLayout.CENTER);

		JPanel controls = new JPanel(new BorderLayout(0, 7));
		controls.setOpaque(false);
		JPanel captions = new JPanel(new BorderLayout(0, 4));
		captions.setOpaque(false);
		captions.add(state, BorderLayout.NORTH);
		hint.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		captions.add(hint, BorderLayout.SOUTH);
		controls.add(captions, BorderLayout.NORTH);
		restart.addActionListener(event -> restart());
		controls.add(restart, BorderLayout.SOUTH);
		add(controls, BorderLayout.SOUTH);

		timer = new Timer(33, event -> refresh());
		timer.setCoalesce(true);
		addHierarchyListener(event ->
		{
			if ((event.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0)
			{
				updateTimer();
			}
		});
		setSprite(sprite, silhouette);
	}

	void setSprite(BufferedImage image, Shape outline)
	{
		boolean firstImage = sprite == null;
		sprite = image == null ? null : copy(image);
		silhouette = outline == null ? null : new Path2D.Double(outline);
		restart.setEnabled(sprite != null);
		if (sprite != null && firstImage)
		{
			timeline.restart(clock.getAsLong());
		}
		refresh();
		updateTimer();
	}

	void refreshSettings()
	{
		long nowNanos = clock.getAsLong();
		timeline.update(nowNanos);
		Object[] settings = readySettings();
		if (!Arrays.equals(settings, previousReadySettings))
		{
			timeline.restartReady(nowNanos);
			previousReadySettings = settings;
		}
		refresh();
	}

	private Object[] readySettings()
	{
		return new Object[]{config.animateWhenReady(), config.readyStyle(), config.animationMode(),
			config.animationDuration(), config.animationColor(), config.animationBorderStyle(),
			config.animationBorderWidth()};
	}

	void restart()
	{
		timeline.restart(clock.getAsLong());
		refresh();
	}

	void close()
	{
		closed = true;
		timer.stop();
	}

	@Override
	public void removeNotify()
	{
		timer.stop();
		super.removeNotify();
	}

	private void updateTimer()
	{
		long nowNanos = clock.getAsLong();
		if (!closed && sprite != null && isShowing() && needsAnimation(nowNanos))
		{
			timer.start();
		}
		else
		{
			timer.stop();
		}
	}

	private boolean needsAnimation(long nowNanos)
	{
		if (timeline.isActive())
		{
			return true;
		}
		if (!renderer.isVisible(timeline, nowNanos))
		{
			return false;
		}
		if (config.animationMode() == AnimationMode.TIMED)
		{
			return true;
		}
		ReadyStyle style = config.readyStyle();
		return style == ReadyStyle.ANIMATED_BORDER || style == ReadyStyle.BREATHING_GLOW
			|| style == ReadyStyle.SINGLE_FLASH
			|| (style == ReadyStyle.FLASH_THEN_STEADY && timeline.getFinishedAgeSeconds(nowNanos) < 0.8);
	}

	private void refresh()
	{
		long nowNanos = clock.getAsLong();
		timeline.update(nowNanos);
		if (sprite == null)
		{
			state.setText("Loading potion icon");
		}
		else if (timeline.isActive())
		{
			state.setText("Cooldown: " + CooldownRenderer.formatSeconds(timeline.getRemainingSeconds(nowNanos)) + "s");
		}
		else if (!config.animateWhenReady())
		{
			state.setText("Ready: enable Show ready indicator.");
		}
		else if (!renderer.isVisible(timeline, nowNanos))
		{
			state.setText("Ready: indicator finished");
		}
		else
		{
			state.setText("Ready");
		}
		icons.repaint();
		updateTimer();
	}

	private void paintIcons(Graphics2D graphics)
	{
		if (sprite == null)
		{
			return;
		}
		long nowNanos = clock.getAsLong();
		timeline.update(nowNanos);
		int nativeX = 40;
		int nativeY = 42;
		paintIcon(graphics, nativeX, nativeY, 1, nowNanos);
		paintIcon(graphics, 164, 10, 3, nowNanos);
		graphics.setColor(ColorScheme.LIGHT_GRAY_COLOR);
		graphics.setFont(getFont().deriveFont(11f));
		drawCentered(graphics, "Actual size", nativeX + sprite.getWidth() / 2, 130);
		drawCentered(graphics, "3× detail", 164 + sprite.getWidth() * 3 / 2, 130);
	}

	private void paintIcon(Graphics2D graphics, int x, int y, int scale, long nowNanos)
	{
		Graphics2D iconGraphics = (Graphics2D) graphics.create();
		try
		{
			iconGraphics.translate(x, y);
			iconGraphics.scale(scale, scale);
			iconGraphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
				RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
			iconGraphics.setColor(new Color(62, 53, 41));
			iconGraphics.fillRect(-PADDING, -PADDING,
				sprite.getWidth() + PADDING * 2, sprite.getHeight() + PADDING * 2);
			iconGraphics.drawImage(sprite, 0, 0, null);
			renderer.render(iconGraphics, new Rectangle(0, 0, sprite.getWidth(), sprite.getHeight()),
				timeline, nowNanos, silhouette);
		}
		finally
		{
			iconGraphics.dispose();
		}
	}

	private static void drawCentered(Graphics2D graphics, String text, int centerX, int baseline)
	{
		graphics.drawString(text, centerX - graphics.getFontMetrics().stringWidth(text) / 2, baseline);
	}

	private static JLabel label(String text)
	{
		JLabel label = new JLabel(text, SwingConstants.CENTER);
		label.setForeground(Color.WHITE);
		return label;
	}

	private static BufferedImage copy(BufferedImage source)
	{
		BufferedImage result = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = result.createGraphics();
		try
		{
			graphics.drawImage(source, 0, 0, null);
		}
		finally
		{
			graphics.dispose();
		}
		return result;
	}

	// Package-private seams keep tests headless and deterministic.
	CooldownPreviewTimeline timeline()
	{
		return timeline;
	}

	boolean isTimerRunning()
	{
		return timer.isRunning();
	}

	String stateText()
	{
		return state.getText();
	}
}
