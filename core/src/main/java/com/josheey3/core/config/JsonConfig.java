package com.josheey3.core.config;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;

/**
 * Stores structured plugin state (lists, maps, small objects) as JSON in a single config key, for
 * state that isn't a user-facing setting. Use the {@link Gson} instance injected by RuneLite.
 * <p>
 * Values are stored per RuneLite profile, like any other config.
 */
@Slf4j
public final class JsonConfig
{
	private JsonConfig()
	{
	}

	/**
	 * @param type e.g. {@code new TypeToken<List<Foo>>(){}}
	 * @return the stored value, or {@code fallback} if missing or unreadable
	 */
	public static <T> T load(ConfigManager configManager, Gson gson, String group, String key, TypeToken<T> type, T fallback)
	{
		String json = configManager.getConfiguration(group, key);
		if (json == null || json.isEmpty())
		{
			return fallback;
		}
		try
		{
			T value = gson.fromJson(json, type.getType());
			return value != null ? value : fallback;
		}
		catch (JsonParseException e)
		{
			log.warn("Ignoring unreadable config {}.{}", group, key, e);
			return fallback;
		}
	}

	public static void save(ConfigManager configManager, Gson gson, String group, String key, Object value)
	{
		configManager.setConfiguration(group, key, gson.toJson(value));
	}
}
