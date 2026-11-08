# Plugin Hub rules: what gets a plugin accepted or rejected

A working summary for this repo. **Snapshot: 2026-11-08.** The rules change, so re-check the
sources before relying on a detail. `pwsh -File scripts/update-ref.ps1` refreshes local copies in
`_ref/` (`wiki/`, `plugin-hub/`, `plugin-hub-tooling/`).

| Source | What it is |
|---|---|
| [Rejected or Rolled-Back Features](https://github.com/runelite/runelite/wiki/Rejected-or-Rolled-Back-Features) (`_ref/wiki/Rejected-or-Rolled-Back-Features.md`) | Feature categories RuneLite won't accept, forbidden language features and APIs |
| [Plugin Hub Review](https://github.com/runelite/runelite/wiki/Plugin-Hub-Review) (`_ref/wiki/Plugin-Hub-Review.md`) | How review works |
| [Jagex third-party client guidelines](https://secure.runescape.com/m=news/third-party-client-guidelines?oldschool=1) | Game rules (bosses, PvP, menus, input) |
| `docs/runelite-agent-guidelines.md` | RuneLite's AGENTS.md: code rules and game-rule summary |
| `_ref/plugin-hub-tooling/package/` | The packager the hub CI runs (hard build checks) |
| [`wontfix` issues/PRs](https://github.com/runelite/runelite/issues?q=label%3Awontfix) | Ideas maintainers have already declined |

## How review works (2026)

- Every submission **and every update** is reviewed. Since April 2026 an **automated review bot**
  can approve simple plugins and updates. Humans configure which permissions and APIs a plugin is
  allowed, and review anything unusual.
- The hub CI records **every API the plugin calls** (`apirecorder`) and fails on disallowed ones.
- Review checks **security** and **game-rule compliance** only. It does not check that the plugin
  works, performs well, or plays nicely with other plugins. Maintainers can still **disable** a
  plugin later if it destabilises the client.
- The guiding rule: *"if we cannot review every single line of source code that your plugin will
  execute, we will not accept it."* Simple, readable, dependency-free code is the fastest path.
- Some old plugins are grandfathered. An existing plugin doing X does **not** mean X is allowed.

## Hard build checks (packager)

- Java 11 bytecode (`options.release.set(11)`). Java only: no Kotlin, Scala, or anything else.
- No classes in the `net.runelite` package.
- `icon.png`, if present, must be a real PNG of at most 5000 px total. 48x72 is recommended.
- `runelite-plugin.properties` must list a class extending `Plugin` in `plugins=`.
- Disallowed APIs (`disallowed-apis.txt`): `new Gson()`/`new GsonBuilder()`,
  `new OkHttpClient()`/`.Builder()`, `WidgetInfo`/`WidgetID`, `ChatMessageManager.update`,
  the old `ItemManager.getItemStats(int, boolean)`, and the account/session internals.
- New third-party dependencies need hub-side hash verification and slow review a lot.
  `build=standard` (no extra dependencies) is the fast lane.

## Forbidden code

- Reflection; JNI/JNA; executing external programs; downloading or loading code at runtime;
  Java (de)serialization.
- `Desktop.open`, `Desktop.browse`, `LinkBrowser.open` (`LinkBrowser.browse` is fine).
- `Thread.sleep`, `Thread.interrupt`.
- `java.io` file APIs. Use `Filepath` and set `internalName` in `@PluginDescriptor`.
- `java.lang.Runtime`; `java.awt.KeyboardFocusManager`.
- Any HTTP client other than the injected `OkHttpClient`; creating your own `Gson` or `OkHttpClient`.
- Using `net.runelite` as your package.
- `Client.menuAction` except the one collection-log use. **Sensitive** (extra review): network IO,
  `Client.runScript`. **Restricted** (forces manual review): `Client.hopToWorld`,
  `Filepath.Unchecked`, `ScriptEventBuilder.setCanSendPackets`.

## Rejected feature categories (excerpt; read the full page)

Most relevant to UI plugins like the ones in this repo:

- **Plugins that manipulate the core RuneLite UI**: overlays or infoboxes, overlay/interface
  anchors, and *Swing components such as the root Frame or sidebar*. A maintainer explained why
  when disabling `sidebar-manager` (2026-09): *"they can break the client in ways that prevent
  them from being uninstalled or configured without safe mode."*
- **Plugins that manipulate the client's window**, including minimising it or changing which window
  is focused (removed at Jagex's request).
- **Plugins that modify other plugins' configs.**
- Plugins whose main features are locked behind paid external services (new plugins since 2026-10-14).
- Plugins that rely on code outside the hub's review, such as a companion app or script users run themselves.
- Also: content simulation, crowdsourcing data about other players, exposing player info over
  HTTP, credential managers, autotyping or changing outgoing chat, player-provided-ID plugins,
  big alterations of in-game assets, adult content, and all the boss/PvP/menu/input
  restrictions in `docs/runelite-agent-guidelines.md`.

## Precedent

| Plugin | What it does | Outcome |
|---|---|---|
| `sidebar-manager` | Re-orders, resizes and hides sidebar icons (changes the core sidebar) | Merged, then **disabled** ("modify the core RuneLite UI") |
| `claude-sidebar` | Sidebar panel that needs a companion app users download and run themselves | **Disabled** a day after merge: code outside hub review is a security concern (plugin-hub#17946) |
| `chat-panel`, `model-popout` | Pop the plugin's **own** content into its own window | Accepted |
| `sidebar-favorites`, `hotkey-toggle-sidepanels` | Their own panel or hotkeys that open existing panels | Accepted |
| `panel-popout` (this repo) | Moves **other plugins'** panels out of the sidebar | **Not eligible.** Personal use via `./gradlew runClient`. The real fix is a RuneLite core PR ([runelite/runelite#2463](https://github.com/runelite/runelite/issues/2463)). |

Rule of thumb: a plugin may create **its own** panels, windows, overlays and hotkeys freely. It
must not rearrange, wrap, hide, move or restyle **RuneLite's** UI or **other plugins'** UI. If an
idea needs that, it's a RuneLite core PR, not a hub plugin.

## What helps acceptance

- Small, focused scope; one clear feature. Plain code, no clever tricks.
- Opt-in (default off) for anything a reviewer might find surprising, and the standard
  third-party-server `warning` on network features.
- A README that explains what the plugin does, ideally with a screenshot. For anything unusual,
  a short "How it works" so reviewers don't have to reverse-engineer it.
- Clean shutDown: everything registered in startUp is removed again.
- `pwsh -File scripts/hub-check.ps1 -Id <id> -Build` passes with no errors.
