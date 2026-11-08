package com.josheey3.panelpopout;

import com.google.inject.Provides;
import com.josheey3.core.ui.Icons;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.config.Keybind;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ClientShutdown;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.HotkeyListener;

@Slf4j
@PluginDescriptor(
	name = PanelPopoutPlugin.NAME,
	description = "Pop sidebar panels out into their own windows, e.g. onto a second monitor, and save window layouts as presets",
	tags = {"popout", "pop out", "window", "detach", "sidebar", "panel", "monitor", "preset", "layout"}
)
public class PanelPopoutPlugin extends Plugin
{
	static final String NAME = "Panel Popout";

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private KeyManager keyManager;

	@Inject
	private PanelPopoutConfig config;

	@Inject
	private PopoutManager popouts;

	@Inject
	private PresetStore presets;

	private final List<HotkeyListener> hotkeys = new ArrayList<>();
	private PanelPopoutPanel panel;
	private Runnable refreshPanel;
	private NavigationButton navButton;

	@Override
	protected void startUp()
	{
		presets.load();
		panel = injector.getInstance(PanelPopoutPanel.class);
		refreshPanel = panel::refresh;
		popouts.addChangeListener(refreshPanel);

		navButton = NavigationButton.builder()
			.tooltip(NAME)
			.icon(Icons.popOutImage(16, ColorScheme.BRAND_ORANGE))
			.priority(9)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navButton);

		for (int i = 0; i < PanelPopoutConfig.PRESET_HOTKEYS; i++)
		{
			int index = i;
			addHotkey(() -> presetHotkey(config, index), () -> openPreset(index));
		}
		addHotkey(config::popInAllHotkey, popouts::popInAll);

		popouts.start();
	}

	@Override
	protected void shutDown()
	{
		hotkeys.forEach(keyManager::unregisterKeyListener);
		hotkeys.clear();
		popouts.stop();
		popouts.removeChangeListener(refreshPanel);
		refreshPanel = null;
		clientToolbar.removeNavigation(navButton);
		panel = null;
		navButton = null;
	}

	/**
	 * Opens the preset at {@code index} in list order (used by hotkeys).
	 */
	void openPreset(int index)
	{
		List<Preset> all = presets.all();
		if (index < all.size())
		{
			popouts.applyLayout(all.get(index).getPanels());
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!PanelPopoutConfig.GROUP.equals(event.getGroup()))
		{
			return;
		}
		SwingUtilities.invokeLater(() ->
		{
			if ("showPopoutButton".equals(event.getKey()))
			{
				popouts.updateSidebarButton();
			}
			if (panel != null)
			{
				// Hotkey labels on presets.
				panel.refresh();
			}
		});
	}

	@Subscribe
	public void onClientShutdown(ClientShutdown event)
	{
		// Capture final window positions; the normal save is debounced.
		if (SwingUtilities.isEventDispatchThread())
		{
			popouts.saveNow();
			return;
		}
		CompletableFuture<Void> saved = new CompletableFuture<>();
		SwingUtilities.invokeLater(() ->
		{
			popouts.saveNow();
			saved.complete(null);
		});
		event.waitFor(saved);
	}

	private void addHotkey(Supplier<Keybind> keybind, Runnable action)
	{
		HotkeyListener listener = new HotkeyListener(keybind)
		{
			@Override
			public void hotkeyPressed()
			{
				SwingUtilities.invokeLater(action);
			}
		};
		keyManager.registerKeyListener(listener);
		hotkeys.add(listener);
	}

	/**
	 * The hotkey for the preset at {@code index} (list order).
	 */
	static Keybind presetHotkey(PanelPopoutConfig config, int index)
	{
		switch (index)
		{
			case 0:
				return config.presetHotkey1();
			case 1:
				return config.presetHotkey2();
			case 2:
				return config.presetHotkey3();
			case 3:
				return config.presetHotkey4();
			case 4:
				return config.presetHotkey5();
			default:
				return Keybind.NOT_SET;
		}
	}

	@Provides
	PanelPopoutConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(PanelPopoutConfig.class);
	}
}
