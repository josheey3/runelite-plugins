# RuneLite plugin workspace

Workspace for building custom [RuneLite](https://runelite.net) plugins. Each plugin is a
self-contained Gradle project, so any one of them can later be split into its own repository for the
Plugin Hub.

## Layout

| Path | What |
|---|---|
| `plugins/<id>/` | One plugin per folder, a standalone Gradle build with its own wrapper |
| `template/` | Tokenized copy of `runelite/example-plugin`, used by `scripts/new-plugin.ps1` |
| `scripts/new-plugin.ps1` | Scaffold a plugin: `pwsh -File scripts/new-plugin.ps1 -Name "My Plugin" -Description "..."` |
| `scripts/update-ref.ps1` | Clone or refresh `_ref/` (RuneLite source and example-plugin, read-only reference) |
| `workspace.properties` | Default `author` and `basePackage` for new plugins |
| `docs/runelite-agent-guidelines.md` | RuneLite's official agent rules for plugin code (from example-plugin's AGENTS.md) |
| `_ref/` | Git-ignored. RuneLite source for API lookups. Never edit it or commit it. |

## Skills

- `runelite-new-plugin`: create a plugin from the template.
- `runelite-plugin-api`: API reference: threads, events, overlays, panels, config, where to look in `_ref/`.
- `runelite-run-client`: build, run the dev client, log in, read logs, what you may and may not test.
- `runelite-plugin-hub`: split a plugin into its own repository and submit it to the Plugin Hub.

## Commands (run inside `plugins/<id>/`)

```bash
./gradlew build   # compile (Java 11 target) + tests
./gradlew run     # launch RuneLite dev client with the plugin loaded (blocks; run in background)
```

JDK 17 Temurin is `JAVA_HOME`. There is no global Gradle; always use the wrapper.

## Rules

@docs/runelite-agent-guidelines.md

Workspace-specific additions:
- Don't use reflection, even in plugins not meant for the hub, unless the user explicitly decides a
  plugin is personal-only. Say what it costs: no hub submission, and it can break on any RuneLite update.
- Never automate game input or use screenshots or computer-use on the game client. Only the user
  tests in-game.
- Never stop `RuneLite.exe`; that's the user's own client. Only stop the dev client's `java.exe`.
- Keep one commit per logical change, and keep each plugin's changes inside its own folder.
