package com.josheey3.panelpopout;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(PanelPopoutConfig.GROUP)
public interface PanelPopoutConfig extends Config
{
	String GROUP = "panel-popout";

	/**
	 * Hidden key holding the open popouts (name, bounds, always-on-top) as JSON.
	 */
	String STATE_KEY = "popouts";

	@ConfigItem(
		keyName = "rememberPopouts",
		name = "Remember popouts",
		description = "Reopen popped-out panels in the same place and size after restarting RuneLite",
		position = 0
	)
	default boolean rememberPopouts()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showPopoutButton",
		name = "Pop-out button",
		description = "Show a pop-out button at the top of the sidebar's icon strip that pops out the open panel",
		position = 1
	)
	default boolean showPopoutButton()
	{
		return true;
	}

	@ConfigItem(
		keyName = "alwaysOnTopByDefault",
		name = "New windows on top",
		description = "Keep newly popped-out windows above other windows. Each window also has its own pin button.",
		position = 2
	)
	default boolean alwaysOnTopByDefault()
	{
		return false;
	}
}
