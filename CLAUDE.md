# RuneLite plugin workspace

One repository for all of our custom [RuneLite](https://runelite.net) plugins, built as a single
Gradle multi-project build. Shared code lives in `core/`. The Plugin Hub needs one repository per
plugin, so `scripts/export-plugin.ps1` produces a standalone copy of a plugin when publishing.

## Layout

| Path | What |
|---|---|
| `settings.gradle` | Includes `core` and **every `plugins/*` folder automatically** (project `:<folder>`) |
| `build.gradle` | Shared build: RuneLite deps (`latest.release`), Java 11, `core` dependency, `run` + `shadowJar` tasks |
| `core/` | Shared library (`com.josheey3.core`). Bundled into each plugin jar. See the `runelite-shared-core` skill. |
| `plugins/<id>/` | One plugin per folder. Its `build.gradle` sets only `group`, `ext.pluginMainClass`, extra deps. |
| `template/plugin/` | Tokenized plugin skeleton used by `scripts/new-plugin.ps1` |
| `template/standalone/` | Upstream example-plugin build files, used by the export |
| `scripts/new-plugin.ps1` | `pwsh -File scripts/new-plugin.ps1 -Name "My Plugin" -Description "..."` |
| `scripts/export-plugin.ps1` | `pwsh -File scripts/export-plugin.ps1 -Id <id> -Build` → `build/export/<id>/` |
| `scripts/update-ref.ps1` | Clone or refresh `_ref/` (RuneLite source, read-only reference) |
| `workspace.properties` | `author`, `basePackage`, `corePackage` |
| `docs/runelite-agent-guidelines.md` | RuneLite's official agent rules (from example-plugin's AGENTS.md) |
| `_ref/` | Git-ignored. RuneLite source for API lookups. Never edit or commit it. |

## Skills

- `runelite-new-plugin`: create a plugin from the template.
- `runelite-plugin-api`: API reference: threads, events, overlays, panels, config, where to look in `_ref/`.
- `runelite-shared-core`: what belongs in `core/`, its rules, and how export relocates it.
- `runelite-run-client`: build, run the dev client, log in, read logs, self-tests, what you may test.
- `runelite-plugin-hub`: export a plugin and submit it to the Plugin Hub.

## Commands (from the repo root)

```bash
./gradlew build                 # everything: core + all plugins, with tests
./gradlew :<id>:build           # one plugin
./gradlew :<id>:run             # dev client with that plugin (blocks; run in background)
```

JDK 17 Temurin is `JAVA_HOME`. There is no global Gradle; always use the root wrapper.

## Git

- Remote: `https://github.com/josheey3/runelite-plugins` (private), owned by the user's
  **personal** GitHub account `josheey3`. The machine's default gh/git account is a work account,
  so never push with the default credentials. Push with a one-off credential helper:
  ```bash
  export JTOKEN=$(gh auth token -u josheey3)
  git -c credential.helper= -c 'credential.helper=!f() { echo username=josheey3; echo "password=$JTOKEN"; }; f' push
  ```
- Repo-local identity is already set (`17991401+josheey3@users.noreply.github.com`). Don't change
  the global git config.

## Rules

@docs/runelite-agent-guidelines.md

Workspace-specific additions:
- Code shared by two or more plugins goes in `core/`. Don't copy it between plugins.
- Don't use reflection, even in plugins not meant for the hub, unless the user explicitly decides a
  plugin is personal-only. Say what it costs: no hub submission, and it can break on any RuneLite update.
- Never automate game input or use screenshots or computer-use on the game client. Only the user
  tests in-game. Automated checks of our own Swing UI from test code are fine (see the
  `runelite-run-client` skill).
- Never stop `RuneLite.exe`; that's the user's own client. Only stop the dev client's `java.exe`.
- Keep one commit per logical change, and keep each plugin's changes inside its own folder.
