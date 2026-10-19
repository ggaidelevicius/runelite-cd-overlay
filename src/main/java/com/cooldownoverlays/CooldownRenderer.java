package com.cooldownoverlays;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.geom.RoundRectangle2D;
import javax.inject.Inject;

/** Draws only within an icon, preserving the caller's clipping and graphics state. */
final class CooldownRenderer
{
	private static final Font TIMER_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 12);
	private final CooldownOverlaysConfig config;

	@Inject
	CooldownRenderer(CooldownOverlaysConfig config)
	{
		this.config = config;
	}

	void render(Graphics2D graphics, Rectangle bounds, Cooldown cooldown, long nowNanos)
	{
		if (!isVisible(cooldown, nowNanos) || bounds.width <= 0 || bounds.height <= 0)
		{
			return;
		}

		boolean known = cooldown.isKnown();
		double seconds = known ? cooldown.getRemainingSeconds(nowNanos) : 0;
		if (cooldown.isActive() && known && seconds <= 0)
		{
			return;
		}

		Graphics2D iconGraphics = (Graphics2D) graphics.create();
		try
		{
			iconGraphics.clip(bounds);
			iconGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			DisplayMode mode = config.displayMode();
			if (cooldown.isActive() && (!known || mode != DisplayMode.TIMER))
			{
				double fraction = known ? cooldown.getRemainingFraction(nowNanos) : 1;
				drawCircle(iconGraphics, bounds, fraction, known);
			}
			if (cooldown.isReady())
			{
				double opacity = 1;
				if (config.animationMode() == AnimationMode.TIMED)
				{
					double duration = Math.max(1, config.animationDuration());
					double age = cooldown.getFinishedAgeSeconds(nowNanos);
					opacity = Math.min(1, (duration - age) / Math.min(0.5, duration));
				}
				drawAnimatedBorder(iconGraphics, bounds, nowNanos, opacity);
			}
			// Draw text last so glows never obscure the timer.
			if (cooldown.isActive() && (!known || mode != DisplayMode.CIRCLE))
			{
				drawTimer(iconGraphics, bounds, known ? formatSeconds(seconds) : "?");
			}
		}
		finally
		{
			iconGraphics.dispose();
		}
	}

	boolean isVisible(Cooldown cooldown, long nowNanos)
	{
		if (cooldown.isActive())
		{
			return true;
		}
		if (!config.animateWhenReady() || !cooldown.isReady())
		{
			return false;
		}
		double age = cooldown.getFinishedAgeSeconds(nowNanos);
		return age >= 0 && (config.animationMode() == AnimationMode.PERMANENT
			|| age < Math.max(1, config.animationDuration()));
	}

	private void drawAnimatedBorder(Graphics2D graphics, Rectangle bounds, long nowNanos,
		double opacity)
	{
		if (bounds.width < 6 || bounds.height < 6)
		{
			return;
		}
		double phase = Math.floorMod(nowNanos, 1_200_000_000L) / 1_200_000_000.0;
		Color color = config.animationColor();
		double pulse = 0.65 + 0.35 * Math.sin(phase * Math.PI * 2);
		RoundRectangle2D border = new RoundRectangle2D.Double(
			bounds.x + 2.5, bounds.y + 2.5, bounds.width - 5, bounds.height - 5, 5, 5);
		// Layer soft edges under a bright core, contained inside the item slot.
		for (int width = 5; width >= 1; width -= 2)
		{
			graphics.setStroke(new BasicStroke(width));
			graphics.setColor(withOpacity(color, opacity * pulse * (0.5 / width)));
			graphics.draw(border);
		}
		graphics.setStroke(new BasicStroke(2,
			BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10,
			new float[]{5, 12}, (float) (phase * 17)));
		graphics.setColor(withOpacity(color, opacity * pulse));
		graphics.draw(border);
	}

	private static Color withOpacity(Color color, double opacity)
	{
		return new Color(color.getRed(), color.getGreen(), color.getBlue(),
			(int) Math.round(color.getAlpha() * Math.max(0, Math.min(1, opacity))));
	}

	private void drawCircle(Graphics2D graphics, Rectangle bounds, double fraction, boolean known)
	{
		double remaining = Math.max(0, Math.min(1, fraction));
		double diameter = Math.min(bounds.width, bounds.height) - 1;
		if (diameter <= 0 || remaining <= 0)
		{
			return;
		}
		int opacity = Math.max(0, Math.min(100, config.circleOpacity()));
		int alpha = (int) Math.round(opacity * 255 / 100.0 * (known ? 1 : 0.5));
		graphics.setColor(new Color(0, 0, 0, alpha));
		// Start fully shaded, then reveal clockwise from twelve o'clock.
		double startAngle = 90 - (1 - remaining) * 360;
		graphics.fill(new Arc2D.Double(
			bounds.getCenterX() - diameter / 2,
			bounds.getCenterY() - diameter / 2,
			diameter, diameter, startAngle, -360 * remaining, Arc2D.PIE));
	}

	private void drawTimer(Graphics2D graphics, Rectangle bounds, String text)
	{
		Font font = TIMER_FONT;
		graphics.setFont(font);
		FontMetrics metrics = graphics.getFontMetrics();
		// Keep long minute timers legible on small or resized spell icons.
		if (metrics.stringWidth(text) > bounds.width - 2)
		{
			float size = Math.max(8, font.getSize2D() * (bounds.width - 2) / metrics.stringWidth(text));
			graphics.setFont(font.deriveFont(size));
			metrics = graphics.getFontMetrics();
		}
		int x = bounds.x + (bounds.width - metrics.stringWidth(text)) / 2;
		int y = bounds.y + (bounds.height - metrics.getAscent() - metrics.getDescent()) / 2 + metrics.getAscent();
		graphics.setColor(Color.BLACK);
		graphics.drawString(text, x - 1, y);
		graphics.drawString(text, x + 1, y);
		graphics.drawString(text, x, y - 1);
		graphics.drawString(text, x, y + 1);
		graphics.setColor(config.timerColor());
		graphics.drawString(text, x, y);
	}

	static String formatSeconds(double seconds)
	{
		long roundedSeconds = (long) Math.ceil(Math.max(0, seconds));
		if (roundedSeconds < 60)
		{
			return Long.toString(roundedSeconds);
		}
		long remainder = roundedSeconds % 60;
		return roundedSeconds / 60 + ":" + (remainder < 10 ? "0" : "") + remainder;
	}
}
