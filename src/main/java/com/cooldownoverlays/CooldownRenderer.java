package com.cooldownoverlays;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import javax.inject.Inject;

/** Draws icon overlays, preserving the caller's clipping and graphics state. */
final class CooldownRenderer
{
	private static final Font TIMER_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 12);
	private static final double FLASH_SECONDS = 0.8;
	private final CooldownOverlaysConfig config;

	@Inject
	CooldownRenderer(CooldownOverlaysConfig config)
	{
		this.config = config;
	}

	void render(Graphics2D graphics, Rectangle bounds, CooldownView cooldown, long nowNanos)
	{
		render(graphics, bounds, cooldown, nowNanos, null);
	}

	void render(Graphics2D graphics, Rectangle bounds, CooldownView cooldown, long nowNanos, Shape itemOutline)
	{
		if (!isVisible(cooldown, nowNanos) || bounds == null || bounds.width <= 0 || bounds.height <= 0)
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
			// A silhouette stroke can extend slightly beyond its source sprite. Keep the
			// inherited widget clip, while tile borders and timers stay inside their icon.
			if (cooldown.isActive() || !config.readyStyle().usesBorder()
				|| config.animationBorderStyle() != BorderStyle.ITEM_OUTLINE)
			{
				iconGraphics.clip(bounds);
			}
			iconGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			DisplayMode mode = config.displayMode();
			if (cooldown.isActive() && (!known || mode != DisplayMode.TIMER))
			{
				double fraction = known ? cooldown.getRemainingFraction(nowNanos) : 1;
				if (mode.usesItemFill())
				{
					drawItemFill(iconGraphics, bounds, itemOutline, fraction, known);
				}
				else
				{
					drawCircle(iconGraphics, bounds, fraction, known);
				}
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
				drawReadyIndicator(iconGraphics, bounds, itemOutline,
					nowNanos, cooldown.getFinishedAgeSeconds(nowNanos), opacity);
			}
			// Draw text last so glows never obscure the timer.
			if (cooldown.isActive() && (!known || mode.showsTimer()))
			{
				drawTimer(iconGraphics, bounds, known ? formatSeconds(seconds) : "?");
			}
		}
		finally
		{
			iconGraphics.dispose();
		}
	}

	boolean isVisible(CooldownView cooldown, long nowNanos)
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
		return age >= 0 && (config.readyStyle() != ReadyStyle.SINGLE_FLASH || age < FLASH_SECONDS)
			&& (config.animationMode() == AnimationMode.PERMANENT
			|| age < Math.max(1, config.animationDuration()));
	}

	private void drawReadyIndicator(Graphics2D graphics, Rectangle bounds, Shape itemOutline, long nowNanos,
		double age, double opacity)
	{
		ReadyStyle style = config.readyStyle();
		if (style == ReadyStyle.CORNER_MARKER)
		{
			drawCornerMarker(graphics, bounds, opacity);
			return;
		}
		int width = Math.max(1, Math.min(6, config.animationBorderWidth()));
		Shape border = readyBorder(bounds, itemOutline, width);
		if (border == null)
		{
			return;
		}
		graphics.translate(bounds.x, bounds.y);
		switch (style)
		{
			case STEADY_OUTLINE:
				drawSteadyOutline(graphics, border, width, opacity);
				break;
			case SOFT_GLOW:
				drawGlow(graphics, border, width, opacity);
				break;
			case BREATHING_GLOW:
				double breath = 0.35 + 0.65 * (0.5 + 0.5 * Math.cos(age * Math.PI * 2 / 1.8));
				drawGlow(graphics, border, width, opacity * breath);
				break;
			case SINGLE_FLASH:
				drawFlash(graphics, border, width, opacity, age);
				break;
			case FLASH_THEN_STEADY:
				drawSteadyOutline(graphics, border, width, opacity);
				drawFlash(graphics, border, width, opacity, age);
				break;
			case ANIMATED_BORDER:
			default:
				drawAnimatedBorder(graphics, border, width, nowNanos, opacity);
				break;
		}
	}

	private Shape readyBorder(Rectangle bounds, Shape itemOutline, int width)
	{
		if (config.animationBorderStyle() == BorderStyle.ITEM_OUTLINE)
		{
			return itemOutline;
		}
		double inset = (width + 3) / 2.0 + 0.5;
		if (bounds.width <= inset * 2 || bounds.height <= inset * 2)
		{
			return null;
		}
		return new RoundRectangle2D.Double(inset, inset,
			bounds.width - inset * 2, bounds.height - inset * 2, 5, 5);
	}

	private void drawAnimatedBorder(Graphics2D graphics, Shape border, int width, long nowNanos,
		double opacity)
	{
		double phase = Math.floorMod(nowNanos, 1_200_000_000L) / 1_200_000_000.0;
		Color color = config.animationColor();
		double pulse = 0.65 + 0.35 * Math.sin(phase * Math.PI * 2);
		// Soft edges follow the same contour as the selected-width core.
		graphics.setStroke(new BasicStroke(width + 3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		graphics.setColor(withOpacity(color, opacity * pulse * 0.08));
		graphics.draw(border);
		graphics.setStroke(new BasicStroke(width + 1, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		graphics.setColor(withOpacity(color, opacity * pulse * 0.16));
		graphics.draw(border);
		graphics.setStroke(new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		graphics.setColor(withOpacity(color, opacity * pulse * 0.4));
		graphics.draw(border);
		graphics.setStroke(new BasicStroke(width,
			BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10,
			new float[]{5, 12}, (float) (phase * 17)));
		graphics.setColor(withOpacity(color, opacity * pulse));
		graphics.draw(border);
	}

	private void drawSteadyOutline(Graphics2D graphics, Shape border, int width, double opacity)
	{
		strokeBorder(graphics, border, width, opacity);
	}

	private void drawGlow(Graphics2D graphics, Shape border, int width, double opacity)
	{
		strokeBorder(graphics, border, width + 6, opacity * 0.055);
		strokeBorder(graphics, border, width + 3, opacity * 0.12);
		strokeBorder(graphics, border, width, opacity * 0.38);
	}

	private void drawFlash(Graphics2D graphics, Shape border, int width, double opacity, double age)
	{
		if (age >= FLASH_SECONDS)
		{
			return;
		}
		double fade = 1 - Math.max(0, age) / FLASH_SECONDS;
		double flashOpacity = opacity * fade * fade;
		strokeBorder(graphics, border, width + 8, flashOpacity * 0.1);
		strokeBorder(graphics, border, width + 4, flashOpacity * 0.25);
		strokeBorder(graphics, border, width, flashOpacity);
	}

	private void strokeBorder(Graphics2D graphics, Shape border, int width, double opacity)
	{
		graphics.setStroke(new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		graphics.setColor(withOpacity(config.animationColor(), opacity));
		graphics.draw(border);
	}

	private void drawCornerMarker(Graphics2D graphics, Rectangle bounds, double opacity)
	{
		double diameter = Math.min(5, Math.min(bounds.width, bounds.height) - 2);
		if (diameter <= 0)
		{
			return;
		}
		Shape marker = new Ellipse2D.Double(bounds.x + bounds.width - diameter - 2,
			bounds.y + bounds.height - diameter - 2, diameter, diameter);
		graphics.setStroke(new BasicStroke(2));
		graphics.setColor(withOpacity(Color.BLACK, opacity * 0.85));
		graphics.draw(marker);
		graphics.setColor(withOpacity(config.animationColor(), opacity));
		graphics.fill(marker);
	}

	private static Color withOpacity(Color color, double opacity)
	{
		return new Color(color.getRed(), color.getGreen(), color.getBlue(),
			(int) Math.round(color.getAlpha() * Math.max(0, Math.min(1, opacity))));
	}

	private void drawItemFill(Graphics2D graphics, Rectangle bounds, Shape silhouette,
		double fraction, boolean known)
	{
		if (silhouette == null)
		{
			return;
		}
		Rectangle2D itemBounds = silhouette.getBounds2D();
		double remaining = Math.max(0, Math.min(1, fraction));
		if (itemBounds.isEmpty() || remaining <= 0)
		{
			return;
		}
		Graphics2D fillGraphics = (Graphics2D) graphics.create();
		try
		{
			fillGraphics.translate(bounds.x, bounds.y);
			fillGraphics.clip(silhouette);
			fillGraphics.setColor(progressShade(known));
			// Only shade unrevealed item pixels. The original full-colour icon shows
			// below the rising boundary, including its texture and transparent holes.
			fillGraphics.fill(new Rectangle2D.Double(itemBounds.getX(), itemBounds.getY(),
				itemBounds.getWidth(), itemBounds.getHeight() * remaining));
		}
		finally
		{
			fillGraphics.dispose();
		}
	}

	private Color progressShade(boolean known)
	{
		int opacity = Math.max(0, Math.min(100, config.circleOpacity()));
		int alpha = (int) Math.round(opacity * 255 / 100.0 * (known ? 1 : 0.5));
		return new Color(0, 0, 0, alpha);
	}

	private void drawCircle(Graphics2D graphics, Rectangle bounds, double fraction, boolean known)
	{
		double remaining = Math.max(0, Math.min(1, fraction));
		double diameter = Math.min(bounds.width, bounds.height) - 1;
		if (diameter <= 0 || remaining <= 0)
		{
			return;
		}
		graphics.setColor(progressShade(known));
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
