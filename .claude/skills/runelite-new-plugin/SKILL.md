---
name: runelite-new-plugin
description: Scaffold a new RuneLite plugin in this workspace (plugins/<id>) from the template, with the package, classes, config group and plugin properties renamed. Use whenever the user wants to start, create or make a new RuneLite plugin or feature, before writing any plugin code.
---

# Create a new RuneLite plugin

Every plugin is a self-contained Gradle project under `plugins/<id>/`, so it can later be split into
its own repository for the Plugin Hub (see the `runelite-plugin-hub` skill).

## Steps

1. Choose:
   - **Name**: display name, may contain spaces, e.g. `Helmet Check`.
   - **Description**: one line shown in the plugin list and on the Plugin Hub.
   - **Id** (optional): kebab-case, derived from the name by default (`helmet-check`). It becomes the
     folder name, the Gradle project name and the config group, so make it specific.
2. Run from the workspace root (PowerShell 7 `pwsh` is installed; Windows PowerShell also works):

   ```bash
   pwsh -NoProfile -File scripts/new-plugin.ps1 -Name "Helmet Check" -Description "Warns when your head slot is empty"
   ```

   Optional: `-Id`, `-Package com.someone.helmetcheck`, `-Author`. Author and base package default
   to `workspace.properties`. The script refuses to overwrite an existing folder and warns if any
   `__TOKEN__` placeholders are left.
3. Build once to confirm it compiles: `cd plugins/<id> && ./gradlew build`.
4. Replace the greeter demo in `<Class>Plugin.java` / `<Class>Config.java` with the real feature.
   Remove the `greeting` config item if unused (unused config must not be left behind).
5. Update `README.md` and `runelite-plugin.properties` (`tags=` helps Plugin Hub search).
6. Launch it for the user with the `runelite-run-client` skill and tell them what to test.

## What the template contains

| File | Purpose |
|---|---|
| `src/main/java/<pkg>/<Class>Plugin.java` | `@PluginDescriptor` plugin with startUp/shutDown and one event subscriber |
| `src/main/java/<pkg>/<Class>Config.java` | `@ConfigGroup` interface; `GROUP` constant = plugin id |
| `src/test/java/<pkg>/<Class>PluginTest.java` | `main()` that loads the plugin as built-in and starts RuneLite (`./gradlew run`) |
| `src/test/resources/logback-test.xml` | DEBUG logging for the plugin package |
| `build.gradle` | Unmodified upstream example-plugin build: `compileOnly net.runelite:client:latest.release`, Java 11 |
| `runelite-plugin.properties` | Plugin Hub metadata (`plugins=` must list the plugin class) |
| `LICENSE` | BSD-2-Clause (Plugin Hub requires a permissive license) |

Keep `build.gradle` structurally identical to the upstream template (the hub's `build=standard`
replaces it anyway). Don't add dependencies that `runelite-client` already brings in (Guava, Gson,
OkHttp, Guice, Lombok is already declared).

## Template maintenance

`template/` tracks <https://github.com/runelite/example-plugin> with tokens
(`__PACKAGE__`, `__Class__`, `__NAME__`, `__ID__`, `__AUTHOR__`, `__DESCRIPTION__`, `__YEAR__`).
To pick up upstream changes run `pwsh -File scripts/update-ref.ps1`, then diff
`_ref/example-plugin` against `template/` and port changes by hand.
