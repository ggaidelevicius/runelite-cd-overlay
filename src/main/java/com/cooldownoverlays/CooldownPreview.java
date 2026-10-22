package com.cooldownoverlays;

import java.awt.Canvas;
import java.awt.Dialog;
import java.awt.GraphicsConfiguration;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicLong;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.JDialog;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import net.runelite.api.Client;
import net.runelite.api.Constants;
import net.runelite.api.GameState;
import net.runelite.api.SpritePixels;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.widgets.ItemQuantityMode;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;

/** Opens a session-only settings preview; real inventory timers are never touched. */
@Singleton
final class CooldownPreview
{
	private final Client client;
	private final ClientThread clientThread;
	private final ConfigManager configManager;
	private final CooldownOverlaysConfig config;
	private final AtomicLong generation = new AtomicLong();
	private volatile boolean running;
	// The window and panel are accessed only on the EDT.
	private JDialog window;
	private CooldownPreviewPanel panel;

	@Inject
	CooldownPreview(Client client, ClientThread clientThread, ConfigManager configManager,
		CooldownOverlaysConfig config)
	{
		this.client = client;
		this.clientThread = clientThread;
		this.configManager = configManager;
		this.config = config;
	}

	void start()
	{
		running = true;
		long token = generation.incrementAndGet();
		SwingUtilities.invokeLater(() ->
		{
			if (isCurrent(token))
			{
				disposeWindow();
				synchronizeSettings();
			}
		});
	}

	void stop()
	{
		running = false;
		long token = generation.incrementAndGet();
		SwingUtilities.invokeLater(() ->
		{
			if (generation.get() == token && !running)
			{
				disposeWindow();
				if (config.showPreview())
				{
					configManager.setConfiguration(CooldownOverlaysConfig.GROUP, "showPreview", false);
				}
			}
		});
	}

	void onConfigChanged()
	{
		if (!running)
		{
			return;
		}
		long token = generation.get();
		SwingUtilities.invokeLater(() ->
		{
			if (isCurrent(token))
			{
				synchronizeSettings();
			}
		});
	}

	private boolean isCurrent(long token)
	{
		return running && generation.get() == token;
	}

	private void synchronizeSettings()
	{
		if (!config.showPreview())
		{
			generation.incrementAndGet();
			disposeWindow();
			return;
		}
		if (window == null)
		{
			openWindow();
		}
		else
		{
			panel.refreshSettings();
		}
	}

	private void openWindow()
	{
		long token = generation.get();
		Canvas canvas = client.getCanvas();
		Window owner = canvas == null ? null : SwingUtilities.getWindowAncestor(canvas);
		CooldownPreviewPanel openedPanel = new CooldownPreviewPanel(config, null, null, System::nanoTime);
		JDialog openedWindow = new JDialog(owner, "Cooldown Overlays preview", Dialog.ModalityType.MODELESS);
		openedWindow.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		openedWindow.setResizable(false);
		openedWindow.setContentPane(openedPanel);
		openedWindow.addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowClosing(WindowEvent event)
			{
				if (window == openedWindow)
				{
					configManager.setConfiguration(CooldownOverlaysConfig.GROUP, "showPreview", false);
				}
			}

			@Override
			public void windowClosed(WindowEvent event)
			{
				openedPanel.close();
				if (window == openedWindow)
				{
					window = null;
					panel = null;
					generation.incrementAndGet();
				}
			}
		});
		window = openedWindow;
		panel = openedPanel;
		openedWindow.pack();
		positionWindow(openedWindow, owner);
		openedWindow.setVisible(true);

		clientThread.invokeLater(() ->
		{
			if (!isCurrent(token))
			{
				return true;
			}
			if (client.getGameState().getState() < GameState.LOGIN_SCREEN.getState())
			{
				return false;
			}
			SpritePixels icon = client.createItemSprite(ItemID._4DOSE2COMBAT, 1, 1,
				SpritePixels.DEFAULT_SHADOW_COLOR, ItemQuantityMode.NEVER, false, Constants.CLIENT_DEFAULT_ZOOM);
			SpritePixels clean = client.createItemSprite(ItemID._4DOSE2COMBAT, 1, 0,
				0, ItemQuantityMode.NEVER, false, Constants.CLIENT_DEFAULT_ZOOM);
			if (icon == null || clean == null)
			{
				return false;
			}
			// Copy pixels and trace the unadorned item on the client thread. Swing only
			// receives snapshots, and a close/reopen invalidates this delivery.
			BufferedImage snapshot = icon.toBufferedImage();
			Shape silhouette = ItemOutlineCache.trace(clean.toBufferedImage());
			if (silhouette == null)
			{
				return false;
			}
			SwingUtilities.invokeLater(() ->
			{
				if (isCurrent(token) && panel == openedPanel)
				{
					openedPanel.setSprite(snapshot, silhouette);
				}
			});
			return true;
		});
	}

	private void disposeWindow()
	{
		if (panel != null)
		{
			panel.close();
			panel = null;
		}
		if (window != null)
		{
			JDialog disposed = window;
			window = null;
			disposed.dispose();
		}
	}

	private static void positionWindow(JDialog dialog, Window owner)
	{
		if (owner == null)
		{
			dialog.setLocationRelativeTo(null);
			return;
		}
		GraphicsConfiguration configuration = owner.getGraphicsConfiguration();
		Rectangle available = new Rectangle(configuration.getBounds());
		Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(configuration);
		available.x += insets.left;
		available.y += insets.top;
		available.width -= insets.left + insets.right;
		available.height -= insets.top + insets.bottom;
		int x = owner.getX() + owner.getWidth() + 8;
		if (x + dialog.getWidth() > available.x + available.width)
		{
			x = owner.getX() - dialog.getWidth() - 8;
		}
		x = Math.max(available.x, Math.min(x, available.x + available.width - dialog.getWidth()));
		int y = Math.max(available.y,
			Math.min(owner.getY() + 64, available.y + available.height - dialog.getHeight()));
		dialog.setLocation(x, y);
	}
}
