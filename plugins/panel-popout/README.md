# Panel Popout

Pop RuneLite sidebar panels out into their own windows, so several can be open at once, for
example notes, XP tracker and Quest Helper on a second monitor. Save window layouts as presets
and switch between them with one click.

## Using it

- **Pop out:** right-click any sidebar icon → **Pop out**, or open a panel and click the pop-out
  button at the top of the sidebar's icon strip.
- **Popped-out panels** are greyed out in the sidebar. Click the icon to bring its window to the
  front.
- **Each window's title bar** has a pin (keep on top) and a pop-in button. Closing the window also
  puts the panel back.
- **Presets:** open the **Panel Popout** panel in the sidebar. Arrange your windows, then click
  **Save open windows as preset**. **Open** on a preset pops out exactly those panels in their
  saved positions and closes every other popout. The **…** menu has *Update from open windows*,
  *Choose panels…*, *Rename…* and *Delete*.
- **Settings** (Configuration → Panel Popout):

  | Setting | Default | Effect |
  |---|---|---|
  | Remember popouts | on | Reopens windows after a restart, and when a plugin turns back on |
  | Pop-out button | on | The button at the top of the icon strip |
  | New windows on top | off | Pin new windows by default |

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
./gradlew :panel-popout:run                          # dev client with the plugin
PANELPOPOUT_SELFTEST=1 ./gradlew :panel-popout:run   # plus an automated sidebar self-test
```

The self-test (`src/test/.../PopoutSelfTest.java`) pops out a scroll-wrapped panel (Notes) and an
unwrapped one (Time Tracking). It then stops and starts the Notes plugin, opens the right-click
menu, applies a preset and restarts Panel Popout itself, checking the sidebar after each step. It
logs `SELFTEST PASS` or `SELFTEST FAIL`. It touches only the sidebar, never the game.
