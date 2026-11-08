---
name: runelite-new-plugin
description: Scaffold a new RuneLite plugin in this workspace (plugins/<id>) from the template, with the package, classes, config group and plugin properties renamed. Use whenever the user wants to start, create or make a new RuneLite plugin or feature, before writing any plugin code.
---

# Create a new RuneLite plugin

All plugins live in this one repository as Gradle subprojects under `plugins/<id>/`. The root
`settings.gradle` picks up any folder there that has a `build.gradle`, so nothing else needs
registering.

## Steps

0. **Feasibility gate first.** Run the `runelite-hub-acceptance` checklist on the idea
   (rules, rejected categories, existing hub plugins). Don't scaffold anything that the Plugin Hub
   won't accept unless the user explicitly chooses a personal-only plugin.
1. Choose:
   - **Name**: display name, may contain spaces, e.g. `Helmet Check`.
   - **Description**: one line shown in the plugin list and on the Plugin Hub.
   - **Id** (optional): kebab-case, derived from the name by default (`helmet-check`). It becomes the
     folder name, the Gradle project (`:helmet-check`) and the config group, so make it specific.
2. Run from the repository root:

   ```bash
   pwsh -NoProfile -File scripts/new-plugin.ps1 -Name "Helmet Check" -Description "Warns when your head slot is empty"
   ```

   Optional: `-Id`, `-Package com.someone.helmetcheck`, `-Author`. Author and base package default
   to `workspace.properties`. The script refuses to overwrite an existing folder and warns if any
   `__TOKEN__` placeholders are left.
3. Build once: `./gradlew :<id>:build`.
4. Replace the greeter demo in `<Class>Plugin.java` / `<Class>Config.java` with the real feature.
   Remove the `greeting` config item if unused (unused config must not be left behind).
5. Before writing a helper, check `core/` (see the `runelite-shared-core` skill). Shared code goes
   there, not in the plugin.
6. Update `README.md`, `runelite-plugin.properties` (`description=`, `tags=`) and the plugins table
   in the root `README.md` (including its Hub status), and the status table in the
   `runelite-hub-acceptance` skill.
7. Run `pwsh -File scripts/hub-check.ps1 -Id <id>` and fix every error before committing.
8. Launch it for the user with the `runelite-run-client` skill and tell them what to test.

## What the template contains

| File | Purpose |
|---|---|
| `src/main/java/<pkg>/<Class>Plugin.java` | `@PluginDescriptor` plugin with startUp/shutDown and one event subscriber |
| `src/main/java/<pkg>/<Class>Config.java` | `@ConfigGroup` interface; `GROUP` constant = plugin id |
| `src/test/java/<pkg>/<Class>PluginTest.java` | `main()` that loads the plugin as built-in and starts RuneLite (`run` task) |
| `src/test/resources/logback-test.xml` | DEBUG logging for the plugin package |
| `build.gradle` | Only `group`, `ext.pluginMainClass` and plugin-specific dependencies; the rest is in the root build |
| `runelite-plugin.properties` | Plugin Hub metadata (`plugins=` must list the plugin class) |
| `LICENSE` | BSD-2-Clause (Plugin Hub requires a permissive license) |

Keep dependencies the same as the upstream template's. The root build mirrors
`template/standalone/build.gradle`, so a plugin that builds here also builds after export. Don't
add dependencies that `runelite-client` already brings in (Guava, Gson, OkHttp, Guice). Lombok
is available in main code only, not tests.

## Template maintenance

`template/plugin/` and `template/standalone/` track <https://github.com/runelite/example-plugin>
with tokens (`__PACKAGE__`, `__Class__`, `__NAME__`, `__ID__`, `__AUTHOR__`, `__DESCRIPTION__`,
`__YEAR__`). To pick up upstream changes run `pwsh -File scripts/update-ref.ps1`, diff
`_ref/example-plugin/build.gradle` against `template/standalone/build.gradle` and the root
`build.gradle`, and port changes by hand.
