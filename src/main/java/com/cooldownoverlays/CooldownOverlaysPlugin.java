package com.cooldownoverlays;

import com.google.inject.Provides;
import javax.inject.Inject;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.ActorDeath;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.Text;

@PluginDescriptor(
	name = "Cooldown Overlays",
	description = "Cooldowns, potion effect timers, and animated glows on inventory items",
	tags = {"cooldown", "timer", "heart", "potion", "boost", "raid", "inventory"}
)
public class CooldownOverlaysPlugin extends Plugin
{
	private static final String LIQUID_ADRENALINE_MESSAGE =
		"You drink some of the potion, reducing the energy cost of your special attacks.";
	private static final String SILK_DRESSING_MESSAGE = "You quickly apply the dressing to your wounds.";
	private static final String BLESSED_CRYSTAL_SCARAB_MESSAGE = "You crack the crystal in your hand.";

	@Inject
	private Client client;
	@Inject
	private ClientThread clientThread;
	@Inject
	private OverlayManager overlayManager;
	@Inject
	private InventoryCooldownOverlay inventoryOverlay;
	@Inject
	private CooldownTracker tracker;
	@Inject
	private PotionTimers potionTimers;
	@Inject
	private BoostPotionTimers boostPotionTimers;
	@Inject
	private CooldownOverlaysConfig config;
	@Inject
	private ConfigManager configManager;
	@Inject
	private ItemOutlineCache itemOutlines;
	@Inject
	private CooldownPreview preview;
	private volatile boolean running;
	// Client-thread state: a queued startup callback must not replace a newer server sample.
	private boolean sampled;

	@Provides
	CooldownOverlaysConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(CooldownOverlaysConfig.class);
	}

	@Override
	protected void startUp()
	{
		migrateAnimations();
		running = true;
		preview.start();
		overlayManager.add(inventoryOverlay);
		clientThread.invoke(() ->
		{
			if (running && !sampled && client.getGameState() == GameState.LOGGED_IN)
			{
				sampleTimers();
			}
		});
	}

	@Override
	protected void shutDown()
	{
		running = false;
		preview.stop();
		overlayManager.remove(inventoryOverlay);
		clientThread.invoke(() ->
		{
			clearTimers();
			itemOutlines.clear();
		});
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (!running)
		{
			return;
		}
		GameState state = event.getGameState();
		if (state == GameState.LOGIN_SCREEN || state == GameState.HOPPING
			|| state == GameState.CONNECTION_LOST || state == GameState.LOGGING_IN)
		{
			clearTimers();
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (running && client.getGameState() == GameState.LOGGED_IN)
		{
			// GameTick runs after this server batch's var updates. Sampling once here
			// avoids subtracting a tick from a cooldown started in VarbitChanged.
			sampleTimers();
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (!running || client.getGameState() != GameState.LOGGED_IN
			|| (event.getType() != ChatMessageType.GAMEMESSAGE && event.getType() != ChatMessageType.SPAM))
		{
			return;
		}
		String message = Text.removeTags(event.getMessage());
		if (LIQUID_ADRENALINE_MESSAGE.equals(message))
		{
			// A successful second dose can refresh adrenaline without toggling its active flag.
			potionTimers.onLiquidAdrenalineDose();
		}
		else if (SILK_DRESSING_MESSAGE.equals(message))
		{
			potionTimers.onConfirmedUse(PotionEffect.SILK_DRESSING, System.nanoTime());
		}
		else if (BLESSED_CRYSTAL_SCARAB_MESSAGE.equals(message))
		{
			potionTimers.onConfirmedUse(PotionEffect.BLESSED_CRYSTAL_SCARAB, System.nanoTime());
		}
	}

	@Subscribe
	public void onActorDeath(ActorDeath event)
	{
		if (running && client.getLocalPlayer() != null && event.getActor() == client.getLocalPlayer())
		{
			potionTimers.clearOnDeath();
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!running || !CooldownOverlaysConfig.GROUP.equals(event.getGroup()))
		{
			return;
		}
		preview.onConfigChanged();
		if (event.getKey().equals("animateActivation") || event.getKey().equals("animationMode")
			|| event.getKey().equals("animationDuration") || event.getKey().equals("animationBorderStyle")
			|| event.getKey().equals("animationBorderWidth") || event.getKey().equals("readyStyle"))
		{
			clientThread.invoke(() ->
			{
				if (running && config.animateWhenReady())
				{
					long nowNanos = System.nanoTime();
					tracker.getHeart().restartReadyAnimation(nowNanos);
					for (PotionEffect effect : PotionEffect.values())
					{
						potionTimers.get(effect).restartReadyAnimation(nowNanos);
					}
					for (BoostPotion potion : BoostPotion.values())
					{
						boostPotionTimers.get(potion).restartReadyAnimation(nowNanos);
					}
				}
			});
		}
	}

	private void migrateAnimations()
	{
		// Keep the existing activation key, duration, and color. Fold the removed
		// running-border toggle into the single ready-animation preference once.
		String oldBorder = configManager.getConfiguration(CooldownOverlaysConfig.GROUP, "animateBorder");
		if (oldBorder != null)
		{
			if (Boolean.parseBoolean(oldBorder))
			{
				configManager.setConfiguration(CooldownOverlaysConfig.GROUP, "animateActivation", true);
			}
			configManager.unsetConfiguration(CooldownOverlaysConfig.GROUP, "animateBorder");
		}
	}

	private void sampleTimers()
	{
		long nowNanos = System.nanoTime();
		tracker.tick(heartValue(), nowNanos);
		potionTimers.tick(client, nowNanos);
		boostPotionTimers.tick(client, config, nowNanos);
		sampled = true;
	}

	private void clearTimers()
	{
		tracker.clear();
		potionTimers.clear();
		boostPotionTimers.clear();
		sampled = false;
	}

	private int heartValue()
	{
		return client.getVarbitValue(VarbitID.IMBUED_HEART_TIMER);
	}
}

