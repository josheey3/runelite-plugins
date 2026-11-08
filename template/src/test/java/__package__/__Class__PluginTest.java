package __PACKAGE__;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

/**
 * Launches a RuneLite developer client with this plugin loaded as if it were built in.
 * Run with {@code ./gradlew run} from the plugin directory.
 */
public class __Class__PluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(__Class__Plugin.class);
		RuneLite.main(args);
	}
}
