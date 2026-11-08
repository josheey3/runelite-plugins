package com.josheey3.panelpopout;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.josheey3.core.config.JsonConfig;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.config.ConfigManager;

/**
 * Presets, persisted as JSON in the plugin's config group (so they follow the RuneLite profile).
 */
@Singleton
class PresetStore
{
	static final String KEY = "presets";
	private static final TypeToken<List<Preset>> TYPE = new TypeToken<List<Preset>>()
	{
	};

	private final ConfigManager configManager;
	private final Gson gson;
	private final List<Preset> presets = new ArrayList<>();

	@Inject
	PresetStore(ConfigManager configManager, Gson gson)
	{
		this.configManager = configManager;
		this.gson = gson;
	}

	void load()
	{
		presets.clear();
		for (Preset p : JsonConfig.load(configManager, gson, PanelPopoutConfig.GROUP, KEY, TYPE, Collections.emptyList()))
		{
			if (p != null && p.getName() != null)
			{
				if (p.getPanels() == null)
				{
					p.setPanels(new ArrayList<>());
				}
				presets.add(p);
			}
		}
	}

	List<Preset> all()
	{
		return Collections.unmodifiableList(presets);
	}

	@Nullable
	Preset byName(String name)
	{
		for (Preset p : presets)
		{
			if (p.getName().equalsIgnoreCase(name))
			{
				return p;
			}
		}
		return null;
	}

	void add(Preset preset)
	{
		presets.add(preset);
		save();
	}

	void remove(Preset preset)
	{
		presets.remove(preset);
		save();
	}

	/**
	 * Moves a preset up (-1) or down (+1). Order matters: preset hotkeys follow it.
	 */
	void move(Preset preset, int delta)
	{
		int from = presets.indexOf(preset);
		int to = from + delta;
		if (from < 0 || to < 0 || to >= presets.size())
		{
			return;
		}
		Collections.swap(presets, from, to);
		save();
	}

	int indexOf(Preset preset)
	{
		return presets.indexOf(preset);
	}

	/**
	 * Persist after changing a preset in place.
	 */
	void save()
	{
		JsonConfig.save(configManager, gson, PanelPopoutConfig.GROUP, KEY, presets);
	}
}
