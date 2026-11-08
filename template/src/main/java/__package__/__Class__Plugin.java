package __PACKAGE__;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@Slf4j
@PluginDescriptor(
	name = "__NAME__",
	description = "__DESCRIPTION__",
	tags = {}
)
public class __Class__Plugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private __Class__Config config;

	@Override
	protected void startUp() throws Exception
	{
		log.debug("__NAME__ started");
	}

	@Override
	protected void shutDown() throws Exception
	{
		log.debug("__NAME__ stopped");
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged)
	{
		if (gameStateChanged.getGameState() == GameState.LOGGED_IN)
		{
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "__NAME__ says " + config.greeting(), null);
		}
	}

	@Provides
	__Class__Config provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(__Class__Config.class);
	}
}
