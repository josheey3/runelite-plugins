package com.josheey3.panelpopout;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Keybind;

@ConfigGroup(PanelPopoutConfig.GROUP)
public interface PanelPopoutConfig extends Config
{
	String GROUP = "panel-popout";

	/**
	 * Hidden key holding the open popouts (name, bounds, always-on-top) as JSON.
	 */
	String STATE_KEY = "popouts";

	/**
	 * Number of preset hotkeys; they map to the presets in list order.
	 */
	int PRESET_HOTKEYS = 5;

	@ConfigSection(
		name = "Windows",
		description = "How popped-out windows behave",
		position = 0
	)
	String windows = "windows";

	@ConfigSection(
		name = "Hotkeys",
		description = "Hotkeys work while the RuneLite game window is focused",
		position = 1
	)
	String hotkeys = "hotkeys";

	@ConfigItem(
		keyName = "rememberPopouts",
		name = "Remember popouts",
		description = "Reopen popped-out panels in the same place and size after restarting RuneLite",
		section = windows,
		position = 0
	)
	default boolean rememberPopouts()
	{
		return true;
	}

	@ConfigItem(
		keyName = "alwaysOnTopByDefault",
		name = "New windows on top",
		description = "Keep newly popped-out windows above other windows. Each window also has its own pin button.",
		section = windows,
		position = 1
	)
	default boolean alwaysOnTopByDefault()
	{
		return false;
	}

	@ConfigItem(
		keyName = "minimizeWithClient",
		name = "Minimise with RuneLite",
		description = "Minimise popped-out windows when RuneLite is minimised, and restore them with it",
		section = windows,
		position = 2
	)
	default boolean minimizeWithClient()
	{
		return false;
	}

	@ConfigItem(
		keyName = "showPopoutButton",
		name = "Pop-out button in sidebar",
		description = "Show a pop-out button at the top of the sidebar's icon strip that pops out the open panel",
		section = windows,
		position = 3
	)
	default boolean showPopoutButton()
	{
		return true;
	}

	@ConfigItem(keyName = "presetHotkey1", name = "Open preset 1", description = "Opens the 1st preset in the Panel Popout list", section = hotkeys, position = 0)
	default Keybind presetHotkey1()
	{
		return Keybind.NOT_SET;
	}

	@ConfigItem(keyName = "presetHotkey2", name = "Open preset 2", description = "Opens the 2nd preset in the Panel Popout list", section = hotkeys, position = 1)
	default Keybind presetHotkey2()
	{
		return Keybind.NOT_SET;
	}

	@ConfigItem(keyName = "presetHotkey3", name = "Open preset 3", description = "Opens the 3rd preset in the Panel Popout list", section = hotkeys, position = 2)
	default Keybind presetHotkey3()
	{
		return Keybind.NOT_SET;
	}

	@ConfigItem(keyName = "presetHotkey4", name = "Open preset 4", description = "Opens the 4th preset in the Panel Popout list", section = hotkeys, position = 3)
	default Keybind presetHotkey4()
	{
		return Keybind.NOT_SET;
	}

	@ConfigItem(keyName = "presetHotkey5", name = "Open preset 5", description = "Opens the 5th preset in the Panel Popout list", section = hotkeys, position = 4)
	default Keybind presetHotkey5()
	{
		return Keybind.NOT_SET;
	}

	@ConfigItem(keyName = "popInAllHotkey", name = "Pop all back in", description = "Puts every popped-out panel back in the sidebar", section = hotkeys, position = 5)
	default Keybind popInAllHotkey()
	{
		return Keybind.NOT_SET;
	}
}
