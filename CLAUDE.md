# RuneLite plugin workspace

One repository for all of our custom [RuneLite](https://runelite.net) plugins, built as a single
Gradle multi-project build. Shared code lives in `core/`. The Plugin Hub needs one repository per
plugin, so `scripts/export-plugin.ps1` produces a standalone copy of a plugin when publishing.

## Priority #1: Plugin Hub acceptance

The Plugin Hub is the only way into a normal RuneLite client, so **every plugin is designed to be
accepted there** unless the user explicitly decides it's personal-only.

- Before designing a plugin or feature, run the feasibility gate in the
  `runelite-hub-acceptance` skill against `docs/plugin-hub-rules.md`. In particular, plugins
  must not modify RuneLite's own UI (sidebar, root frame) or other plugins' UI or config. If an
  idea fails the gate, say so up front and offer: reshape it, a RuneLite core PR, or personal-only.
- Before every commit to a plugin: `pwsh -File scripts/hub-check.ps1 -Id <id>` (add `-Build` before
  publishing). Fix errors; be able to justify warnings.
- Lesson learned: `panel-popout` was built before checking the rejected-features list and turned
  out to be ineligible by policy. Check first.

## Layout

| Path | What |
|---|---|
| `settings.gradle` | Includes `core`, `dev-client` and **every `plugins/*` folder automatically** (project `:<folder>`) |
| `build.gradle` | Shared build: RuneLite deps (`latest.release`), Java 11, `core` dependency, `run` + `shadowJar` tasks, `runClient` |
| `core/` | Shared library (`com.josheey3.core`), bundled into each plugin jar. See the `runelite-shared-core` skill. |
| `plugins/<id>/` | One plugin per folder. Its `build.gradle` sets only `group`, `ext.pluginMainClass` and extra deps. |
| `dev-client/` | `./gradlew runClient`: RuneLite with every plugin loaded (launcher generated from `plugins=` lines) |
| `template/plugin/` | Tokenized plugin skeleton used by `scripts/new-plugin.ps1` |
| `template/standalone/` | Upstream example-plugin build files, used by the export |
| `scripts/new-plugin.ps1` | `pwsh -File scripts/new-plugin.ps1 -Name "My Plugin" -Description "..."` |
| `scripts/hub-check.ps1` | `pwsh -File scripts/hub-check.ps1 -Id <id> [-Build]`: Plugin Hub rules check |
| `scripts/export-plugin.ps1` | `pwsh -File scripts/export-plugin.ps1 -Id <id> -Build` → `build/export/<id>/` |
| `scripts/update-ref.ps1` | Clone or refresh `_ref/` (RuneLite source, wiki, plugin-hub, plugin-hub-tooling) |
| `workspace.properties` | `author`, `basePackage`, `corePackage` |
| `docs/plugin-hub-rules.md` | Hub rules, rejected categories, review process, precedent (dated snapshot plus sources) |
| `docs/runelite-agent-guidelines.md` | RuneLite's official agent rules (from example-plugin's AGENTS.md) |
| `_ref/` | Git-ignored, read-only reference checkouts. Never edit or commit them. |

## Skills

- `runelite-hub-acceptance`: **use first**. Feasibility gate, rules while building, pre-commit check.
- `runelite-new-plugin`: create a plugin from the template.
- `runelite-plugin-api`: API reference: threads, events, overlays, panels, config, where to look in `_ref/`.
- `runelite-shared-core`: what belongs in `core/`, its rules, and how export relocates it.
- `runelite-run-client`: build, run the dev client, `runClient`, log in, logs, self-tests, what you may test.
- `runelite-plugin-hub`: export a plugin and submit it to the Plugin Hub.

## Commands (from the repo root)

```bash
./gradlew build                 # everything: core + all plugins, with tests
./gradlew :<id>:build           # one plugin
./gradlew :<id>:run             # dev client with just that plugin (blocks; run in background)
./gradlew runClient             # RuneLite with every plugin in this repo, for everyday use
```

JDK 17 Temurin is `JAVA_HOME`. There is no global Gradle; always use the root wrapper.

## Git

- Remote: `https://github.com/josheey3/runelite-plugins`. Machine-specific push and account
  instructions are in `CLAUDE.local.md` (git-ignored). Follow them when present. Never change
  global git or gh configuration.
- Plugin Hub publishing repositories are separate, public, and created by the export flow (see
  the `runelite-plugin-hub` skill).

## Rules

@docs/runelite-agent-guidelines.md

Workspace-specific additions:
- Code shared by two or more plugins goes in `core/`. Don't copy it between plugins.
- Don't use reflection, even in plugins not meant for the hub, unless the user explicitly decides a
  plugin is personal-only. Say what it costs: no hub submission, and it can break on any RuneLite update.
- Never automate game input or use screenshots or computer-use on the game client. Only the user
  tests in-game. Automated checks of our own Swing UI from test code are fine (see the
  `runelite-run-client` skill).
- Never stop `RuneLite.exe`; that's the user's own client. Only stop the dev client's `java.exe`, and
  never a dev client the user is using: match the PID you started.
- Keep one commit per logical change, and keep each plugin's changes inside its own folder.
