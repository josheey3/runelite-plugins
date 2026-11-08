package com.josheey3.panelpopout;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

/**
 * Launches a RuneLite developer client with this plugin loaded as if it were built in.
 * Run with {@code ./gradlew run} from the plugin directory.
 */
public class PanelPopoutPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(PanelPopoutPlugin.class);
		if ("1".equals(System.getenv("PANELPOPOUT_SELFTEST")))
		{
			PopoutSelfTest.schedule();
		}
		RuneLite.main(args);
	}
}
