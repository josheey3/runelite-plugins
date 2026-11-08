# Panel Popout

Pop RuneLite sidebar panels out into their own windows, so several can be open at once, for
example notes, XP tracker and Quest Helper on a second monitor. Save window layouts as presets
and switch between them with one click or a hotkey.

> **Plugin Hub status: not eligible.** The hub doesn't accept plugins that modify RuneLite's own
> UI such as the sidebar ([details](../../docs/plugin-hub-rules.md#precedent)), and moving other
> plugins' panels is exactly what this does. Use it with `./gradlew runClient` from the repo root.
> The policy-compatible route to everyone's client is a RuneLite core change
> ([runelite/runelite#2463](https://github.com/runelite/runelite/issues/2463)).

## Using it

- **Pop out:**
  - right-click any sidebar icon → **Pop out**;
  - **middle-click** an icon (middle-click again to put it back);
  - open a panel and click the pop-out button at the top of the sidebar's icon strip; or
  - use **Pop out...** in the Panel Popout panel.
- **Popped-out panels** are greyed out in the sidebar. Click the icon to bring its window to the
  front.
- **Each window's title bar** has a pin (keep on top) and a pop-in button. Closing the window also
  puts the panel back.
- **Presets:** open the **Panel Popout** panel in the sidebar.
  - Arrange your windows, then click **Save open windows as preset**.
  - **Open** pops out exactly that preset's panels in their saved positions and closes every other
    popout.
  - The **…** menu has *Update from open windows*, *Choose panels…*, *Move up/down*, *Rename…* and
    *Delete*.
  - The highlighted preset is the one currently open.
- **Hotkeys** (Configuration → Panel Popout → Hotkeys): open presets 1–5 (in list order), and pop
  everything back in. They work while the game window has focus.

## Settings

| Setting | Default | Effect |
|---|---|---|
| Remember popouts | on | Reopens windows after a restart, and when a plugin turns back on |
| New windows on top | off | Pin new windows by default |
| Minimise with RuneLite | off | Minimise popouts with RuneLite and restore them with it, without taking focus from the game |
| Pop-out button in sidebar | on | The button at the top of the icon strip |
| Open preset 1–5, Pop all back in | not set | Hotkeys |

## Safety

- **Configuration can't be popped out**, so plugin settings, including turning this plugin off,
  are always in the sidebar.
- If anything unexpected goes wrong, every panel is put back in the sidebar.
- Turning the plugin off puts every panel back. They reopen when it's turned on again.
- Only Panel Popout's own windows are ever moved, minimised or raised. The RuneLite window is
  only listened to, never changed, and popout windows never take focus by themselves.

## How it works

RuneLite has no API for this, so the plugin works with the sidebar's Swing components directly,
using only public Swing APIs (no reflection):

- Popping out moves the tab's component into a new window and leaves a `PopoutPlaceholder` in the
  tab. Tab positions don't change, so RuneLite's index-based mapping of tabs to plugins stays valid.
- The placeholder `equals()` the original component. When the owning plugin removes its panel
  while it's popped out, RuneLite's own `sidebar.remove(panel)` finds and removes the placeholder,
  and the window closes.
- A popped-out tab is disabled and never left selected, so RuneLite never deactivates its panel.
  The plugin calls `onActivate`/`onDeactivate` itself as panels move in and out.

Limits: this relies on the sidebar being a `JTabbedPane`, which RuneLite could change in any
update. Panels are identified by their tab name, so two plugins with the same panel name share
one remembered position.

## Development

From the repo root:

```bash
./gradlew :panel-popout:run                          # dev client with just this plugin
PANELPOPOUT_SELFTEST=1 ./gradlew :panel-popout:run   # plus an automated sidebar self-test
./gradlew runClient                                  # everyday client with all repo plugins
```

The self-test (`src/test/.../PopoutSelfTest.java`) pops out a scroll-wrapped panel (Notes) and an
unwrapped one (Time Tracking). It then:

1. stops and starts the Notes plugin;
2. checks the right-click menu, middle-click and that Configuration is protected;
3. applies a preset and goes through the preset-hotkey path;
4. restarts Panel Popout itself.

It checks the sidebar after each step and logs `SELFTEST PASS` or `SELFTEST FAIL`. It touches
only the sidebar, never the game.
