package com.josheey3.panelpopout;

import com.google.inject.Provides;
import com.josheey3.core.ui.Icons;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.NavigationButton;

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
	private PopoutManager popouts;

	@Inject
	private PresetStore presets;

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

		popouts.start();
	}

	@Override
	protected void shutDown()
	{
		popouts.stop();
		popouts.removeChangeListener(refreshPanel);
		refreshPanel = null;
		clientToolbar.removeNavigation(navButton);
		panel = null;
		navButton = null;
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!PanelPopoutConfig.GROUP.equals(event.getGroup()))
		{
			return;
		}
		if ("showPopoutButton".equals(event.getKey()))
		{
			SwingUtilities.invokeLater(popouts::updateSidebarButton);
		}
	}

	@Provides
	PanelPopoutConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(PanelPopoutConfig.class);
	}
}
