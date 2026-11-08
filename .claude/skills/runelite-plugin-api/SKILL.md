---
name: runelite-plugin-api
description: Reference for writing RuneLite plugin code - plugin lifecycle, the client thread vs Swing EDT, events, overlays, infoboxes, sidebar panels, config, game IDs, and how to look things up in the RuneLite source checked out in _ref/. Use when writing or changing any RuneLite plugin code, or when unsure which RuneLite API, event or class to use.
---

# RuneLite plugin API reference

Read `docs/runelite-agent-guidelines.md` too: RuneLite's own rules for plugin code.

## Source of truth: `_ref/runelite`

APIs change. Before using one you're unsure of, check the source instead of relying on memory.
`_ref/` is git-ignored. Create or update it with `pwsh -File scripts/update-ref.ps1`, a shallow
clone of `runelite/runelite` master and `runelite/example-plugin`. `latest.release` (what plugins
compile against) may lag master by a few days.

| Area | Path under `_ref/runelite/` |
|---|---|
| Game API (`Client`, actors, widgets) | `runelite-api/src/main/java/net/runelite/api/` |
| Game events | `runelite-api/src/main/java/net/runelite/api/events/` |
| Game IDs (`ItemID`, `NpcID`, `ObjectID`, `InterfaceID`, `VarbitID`, `VarPlayerID`, `AnimationID`, `SpriteID`, `InventoryID`) | `runelite-api/src/main/java/net/runelite/api/gameval/` |
| Client services (config, overlays, UI, managers) | `runelite-client/src/main/java/net/runelite/client/` |
| Client events (`ConfigChanged`, `PluginChanged`, `ProfileChanged`, `ClientShutdown`, loot events) | `runelite-client/src/main/java/net/runelite/client/events/` |
| 100+ built-in plugins to copy patterns from | `runelite-client/src/main/java/net/runelite/client/plugins/` |
| Sidebar, windows, theme | `runelite-client/src/main/java/net/runelite/client/ui/` (`ClientUI`, `ClientToolbar`, `NavigationButton`, `PluginPanel`, `ColorScheme`) |

When you need a pattern ("how do built-in plugins draw a tile outline?"), grep the built-in plugins
first. They are the best examples of accepted style.

## Lifecycle

```java
@PluginDescriptor(name = "Helmet Check", description = "...", tags = {"gear"})
public class HelmetCheckPlugin extends Plugin
{
	@Inject private Client client;
	@Inject private ClientThread clientThread;
	@Inject private HelmetCheckConfig config;

	@Override protected void startUp() { /* register overlays, nav buttons, listeners */ }
	@Override protected void shutDown() { /* undo EVERYTHING from startUp */ }

	@Provides HelmetCheckConfig provideConfig(ConfigManager cm) { return cm.getConfig(HelmetCheckConfig.class); }
}
```

- Guice injects anything the client binds (`Client`, `ClientThread`, `ConfigManager`, `OverlayManager`,
  `ClientToolbar`, `ItemManager`, `InfoBoxManager`, `ChatMessageManager`, `Notifier`, `KeyManager`,
  `MouseManager`, `SpriteManager`, `EventBus`, `OkHttpClient`, `Gson`, `ScheduledExecutorService`).
- `@Subscribe` methods on the plugin class are registered automatically. Other objects need
  `eventBus.register(obj)` in startUp and `eventBus.unregister(obj)` in shutDown.
- Plugins can be toggled at runtime and on profile switch, so shutDown must leave no trace.
- `@PluginDescriptor` also has `enabledByDefault`, `hidden`, `developerPlugin`, `configName`.
  `@PluginDependency(OtherPlugin.class)` declares a dependency on another plugin.

## Threads: the rule that matters most

| Thread | What runs there | Rule |
|---|---|---|
| **Client thread** | Game events, `@Subscribe` for game events, overlays' `render` | The only thread allowed to read or write game state (`client.*`). Never block it. |
| **Swing EDT** | Panels, `NavigationButton` clicks, `startUp`/`shutDown` | All Swing work. Don't touch `client` here; hop with `clientThread.invoke(...)`. |
| OkHttp pool / executor | Network, disk IO | Hop back with `clientThread.invoke` (game) or `SwingUtilities.invokeLater` (UI). |

- `clientThread.invoke(r)` runs now if already on the client thread, else next cycle.
  `invokeLater(r)` always defers. The `BooleanSupplier` forms retry until they return true.
- Updating a panel from an event: `SwingUtilities.invokeLater(() -> panel.update(data))`.
  Copy the data first; don't pass live game objects to the EDT.

## Common events (`net.runelite.api.events`)

`GameTick` (every 0.6s), `GameStateChanged` (`LOGIN_SCREEN`, `LOGGING_IN`, `LOADING`, `LOGGED_IN`, `HOPPING`),
`ChatMessage`, `MenuEntryAdded`, `MenuOptionClicked`, `NpcSpawned`/`NpcDespawned`, `PlayerSpawned`,
`GameObjectSpawned`/`Despawned` (plus Wall/Decorative/Ground variants), `ItemSpawned`, `ItemContainerChanged`,
`StatChanged`, `VarbitChanged`, `WidgetLoaded`/`WidgetClosed`, `AnimationChanged`, `HitsplatApplied`,
`ActorDeath`, `InteractingChanged`, `ScriptPostFired`, `CommandExecuted` (`::command` in dev mode).
Client events: `ConfigChanged` (check `event.getGroup()`), `ProfileChanged`, `NpcLootReceived`,
`PlayerLootReceived`, `PluginChanged`, `ClientShutdown`.

Track spawned objects with spawn/despawn events into your own collection. Don't scan the scene each tick.

## Drawing on the game: overlays

```java
class MyOverlay extends Overlay
{
	@Inject MyOverlay() { setPosition(OverlayPosition.DYNAMIC); setLayer(OverlayLayer.ABOVE_SCENE); }
	@Override public Dimension render(Graphics2D g) { /* OverlayUtil.renderPolygon(...) */ return null; }
}
// startUp: overlayManager.add(overlay); shutDown: overlayManager.remove(overlay);
```

- Boxed text panels: extend `OverlayPanel` and add `LineComponent`/`TitleComponent` to
  `panelComponent.getChildren()` (see `ui/overlay/components`).
- `render` runs every frame on the client thread. Keep it cheap and precompute in event handlers.
- Item icons in inventories: `WidgetItemOverlay`. InfoBoxes (icons with counters or timers under
  the minimap): `InfoBoxManager.addInfoBox(new Counter(...))` or `Timer`.

## Sidebar panels

```java
panel = injector.getInstance(MyPanel.class);          // MyPanel extends PluginPanel
navButton = NavigationButton.builder()
	.tooltip("Helmet Check").icon(ImageUtil.loadImageResource(getClass(), "icon.png"))
	.priority(5).panel(panel).build();
clientToolbar.addNavigation(navButton);               // shutDown: clientToolbar.removeNavigation(navButton)
```

- `PluginPanel` is 225 px wide. `super()` wraps it in a scroll pane, and `super(false)` doesn't (you
  manage layout and scrolling yourself). Override `onActivate`/`onDeactivate` to pause work while
  hidden.
- Use `ColorScheme` colours and `FontManager` fonts so the panel matches the client theme.
- `NavigationButton.popup(Map<String, Runnable>)` adds right-click menu items to the sidebar tab.
- To work with *other* plugins' sidebar tabs (there's no API), use `core`'s `ClientSidebar`.
  See `plugins/panel-popout` for the safe way to change tabs without breaking RuneLite's
  index-based tab-to-plugin mapping.
- Load images with `ImageUtil.loadImageResource(getClass(), "file.png")`, and put files in
  `src/main/resources/<package path>/`.

## Config

```java
@ConfigGroup(MyConfig.GROUP)
public interface MyConfig extends Config
{
	String GROUP = "helmet-check";
	@ConfigSection(name = "Alerts", description = "...", position = 0) String alerts = "alerts";
	@ConfigItem(keyName = "notify", name = "Notify", description = "...", section = alerts, position = 1)
	default boolean notify() { return true; }
	@Range(min = 1, max = 100) @ConfigItem(keyName = "size", name = "Size", description = "...")
	default int size() { return 10; }
}
```

Types: `boolean`, `int`, `double`, `String`, enums, `Color` (`@Alpha` for transparency), `Keybind`,
`ModifierlessKeybind`, `Notification`. Hidden state: `configManager.setConfiguration(GROUP, key, value)`
plus a `hidden = true` item, or `getRSProfileConfiguration` for per-account values. Never rename
groups or keys without a migration.

## Other handy services

- Chat: `client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", msg, null)` (client thread), or
  `chatMessageManager.queue(QueuedMessage.builder()...)`.
- Notifications: `notifier.notify(config.myNotification(), "text")`.
- Item prices and names: `itemManager.getItemPrice(id)`, `itemManager.getItemComposition(id)` (client thread).
- Hotkeys: `keyManager.registerKeyListener(new HotkeyListener(config::hotkey) { ... })`.
- Opening URLs: `LinkBrowser.browse(url)`. File IO: `Filepath` (set `internalName` in the descriptor).
- Developer tools (dev client only): the "Developer Tools" panel has a widget inspector, var
  inspector, and tile/NPC/object info. Use it to find IDs.
