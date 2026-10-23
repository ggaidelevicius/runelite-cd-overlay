package com.cooldownoverlays;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.events.ActorDeath;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Exercises the plugin's event boundaries without a RuneLite client or event bus. */
public class ConsumableIntegrationTest
{
	private static final String SILK_MESSAGE = "You quickly apply the dressing to your wounds.";
	private static final String SCARAB_MESSAGE = "You crack the crystal in your hand.";
	private final CooldownOverlaysPlugin plugin = new CooldownOverlaysPlugin();
	private final PotionTimers timers = new PotionTimers();
	private GameState gameState = GameState.LOGGED_IN;
	private Player localPlayer = player();
	private final Client client = (Client) Proxy.newProxyInstance(Client.class.getClassLoader(),
		new Class<?>[]{Client.class}, (proxy, method, args) ->
		{
			switch (method.getName())
			{
				case "getGameState": return gameState;
				case "getLocalPlayer": return localPlayer;
				default: throw new UnsupportedOperationException(method.getName());
			}
		});

	@Before
	public void injectEventDependencies() throws Exception
	{
		inject("client", client);
		inject("potionTimers", timers);
		inject("tracker", new CooldownTracker());
		inject("boostPotionTimers", new BoostPotionTimers());
		CooldownOverlaysConfig config = new CooldownOverlaysConfig() {};
		inject("repotProtection", new RepotProtection(client, config,
			new RepotProtectionPolicy(client, config, new CooldownTracker(), timers, new BoostPotionTimers())));
		inject("running", true);
	}

	@Test
	public void exactGameAndSpamMessagesStartOnlyTheMatchingConsumableTimer()
	{
		for (ChatMessageType type : new ChatMessageType[]{ChatMessageType.GAMEMESSAGE, ChatMessageType.SPAM})
		{
			timers.clear();
			plugin.onChatMessage(chat(type, SILK_MESSAGE));
			assertRunning(PotionEffect.SILK_DRESSING, 100);
			assertFalse(timers.get(PotionEffect.BLESSED_CRYSTAL_SCARAB).isActive());
			timers.clear();
			plugin.onChatMessage(chat(type, SCARAB_MESSAGE));
			assertRunning(PotionEffect.BLESSED_CRYSTAL_SCARAB, 40);
			assertFalse(timers.get(PotionEffect.SILK_DRESSING).isActive());
		}
	}

	@Test
	public void gameTextTagsDoNotPreventConfirmedUses()
	{
		plugin.onChatMessage(chat(ChatMessageType.GAMEMESSAGE, "<col=ff0000>" + SILK_MESSAGE + "</col>"));
		plugin.onChatMessage(chat(ChatMessageType.SPAM, "<col=00ff00>" + SCARAB_MESSAGE + "</col>"));
		assertRunning(PotionEffect.SILK_DRESSING, 100);
		assertRunning(PotionEffect.BLESSED_CRYSTAL_SCARAB, 40);
	}

	@Test
	public void otherChatChannelsCannotSpoofConsumableUses()
	{
		for (ChatMessageType type : ChatMessageType.values())
		{
			if (type == ChatMessageType.GAMEMESSAGE || type == ChatMessageType.SPAM)
			{
				continue;
			}
			plugin.onChatMessage(chat(type, SILK_MESSAGE));
			plugin.onChatMessage(chat(type, SCARAB_MESSAGE));
			assertBothCleared();
		}
	}

	@Test
	public void similarOrUnsuccessfulMessagesDoNotStartTimers()
	{
		String[] messages = {
			"You cannot apply the dressing to your wounds.",
			"You cannot crack the crystal in your hand.",
			"Someone says: " + SILK_MESSAGE,
			SCARAB_MESSAGE + " Again.",
			"You quickly apply the dressing to your wounds",
			"You crack the crystal in your hand",
			"you quickly apply the dressing to your wounds.",
			"you crack the crystal in your hand."
		};
		for (String message : messages)
		{
			plugin.onChatMessage(chat(ChatMessageType.GAMEMESSAGE, message));
			assertBothCleared();
		}
	}

	@Test
	public void messagesOutsideLoggedInStateCannotStartTimers()
	{
		for (GameState state : GameState.values())
		{
			if (state == GameState.LOGGED_IN)
			{
				continue;
			}
			gameState = state;
			plugin.onChatMessage(chat(ChatMessageType.GAMEMESSAGE, SILK_MESSAGE));
			plugin.onChatMessage(chat(ChatMessageType.SPAM, SCARAB_MESSAGE));
			assertBothCleared();
		}
	}

	@Test
	public void stoppedPluginIgnoresConfirmedUseMessages() throws Exception
	{
		inject("running", false);
		plugin.onChatMessage(chat(ChatMessageType.GAMEMESSAGE, SILK_MESSAGE));
		plugin.onChatMessage(chat(ChatMessageType.SPAM, SCARAB_MESSAGE));
		assertBothCleared();
	}

	@Test
	public void localPlayerDeathClearsBothChatTrackedEffects()
	{
		startBoth();
		plugin.onActorDeath(new ActorDeath(localPlayer));
		assertBothCleared();
	}

	@Test
	public void anotherPlayerDeathPreservesBothChatTrackedEffects()
	{
		startBoth();
		plugin.onActorDeath(new ActorDeath(player()));
		assertRunning(PotionEffect.SILK_DRESSING, 100);
		assertRunning(PotionEffect.BLESSED_CRYSTAL_SCARAB, 40);
	}

	@Test
	public void missingLocalPlayerDoesNotMistakeNullActorForLocalDeath()
	{
		startBoth();
		localPlayer = null;
		plugin.onActorDeath(new ActorDeath(null));
		assertRunning(PotionEffect.SILK_DRESSING, 100);
		assertRunning(PotionEffect.BLESSED_CRYSTAL_SCARAB, 40);
	}

	@Test
	public void stoppedPluginIgnoresDeathEvents() throws Exception
	{
		startBoth();
		inject("running", false);
		plugin.onActorDeath(new ActorDeath(localPlayer));
		assertRunning(PotionEffect.SILK_DRESSING, 100);
		assertRunning(PotionEffect.BLESSED_CRYSTAL_SCARAB, 40);
	}

	@Test
	public void sessionTransitionsDiscardDurationsThatCannotBeRehydrated()
	{
		GameState[] transitions = {GameState.LOGIN_SCREEN, GameState.HOPPING,
			GameState.CONNECTION_LOST, GameState.LOGGING_IN};
		for (GameState transition : transitions)
		{
			startBoth();
			GameStateChanged event = new GameStateChanged();
			event.setGameState(transition);
			plugin.onGameStateChanged(event);
			assertBothCleared();
		}
	}

	private void startBoth()
	{
		plugin.onChatMessage(chat(ChatMessageType.GAMEMESSAGE, SILK_MESSAGE));
		plugin.onChatMessage(chat(ChatMessageType.GAMEMESSAGE, SCARAB_MESSAGE));
		assertRunning(PotionEffect.SILK_DRESSING, 100);
		assertRunning(PotionEffect.BLESSED_CRYSTAL_SCARAB, 40);
	}

	private void assertRunning(PotionEffect effect, int ticks)
	{
		Cooldown cooldown = timers.get(effect);
		assertTrue(effect.name(), cooldown.isActive());
		assertTrue(effect.name(), cooldown.isKnown());
		assertFalse(effect.name(), cooldown.isReady());
		assertEquals(effect.name(), ticks, cooldown.getTotalTicks());
	}

	private void assertBothCleared()
	{
		for (PotionEffect effect : new PotionEffect[]{PotionEffect.SILK_DRESSING, PotionEffect.BLESSED_CRYSTAL_SCARAB})
		{
			assertFalse(effect.name(), timers.get(effect).isActive());
			assertFalse(effect.name(), timers.get(effect).isReady());
		}
	}

	private static ChatMessage chat(ChatMessageType type, String message)
	{
		ChatMessage event = new ChatMessage();
		event.setType(type);
		event.setMessage(message);
		return event;
	}

	private void inject(String name, Object value) throws Exception
	{
		Field field = CooldownOverlaysPlugin.class.getDeclaredField(name);
		field.setAccessible(true);
		field.set(plugin, value);
	}

	private static Player player()
	{
		return (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[]{Player.class},
			(proxy, method, args) ->
			{
				throw new UnsupportedOperationException(method.getName());
			});
	}
}
