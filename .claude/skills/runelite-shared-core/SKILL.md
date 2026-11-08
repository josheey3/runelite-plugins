---
name: runelite-shared-core
description: Rules and workflow for the shared core/ library that every plugin in this workspace can use - what belongs there, what it may not do, how it's bundled, and how export relocates it for the Plugin Hub. Use when writing code that two or more plugins could use, when a plugin imports com.josheey3.core, or when changing anything under core/.
---

# Shared `core/` library

`core/` is a normal Gradle subproject (`:core`), compiled against `runelite-client` exactly like a
plugin. The root `build.gradle` makes every plugin depend on it and bundles its classes into the
plugin's jar, so `./gradlew :<id>:run`, sideloading and `shadowJar` just work.

## What belongs in core

Code that's useful to more than one plugin and has nothing plugin-specific in it, for example:

| Package | Contents today |
|---|---|
| `com.josheey3.core.ui` | `WindowBounds` (persistable, monitor-safe window bounds), `Icons` (Java2D-drawn toolbar icons), `ClientSidebar` (find the RuneLite sidebar and its tabs' `PluginPanel`s; **personal-only plugins**, since using it to change the sidebar is a rejected hub category) |
| `com.josheey3.core.config` | `JsonConfig` (store structured state as JSON in a config key) |

Before adding a helper to a plugin, check whether core already has it. When a second plugin needs
something that lives in a plugin, move it to core in its own commit, then use it.

## Rules

- **Same rules as plugin code**: all of `docs/runelite-agent-guidelines.md` and
  `docs/plugin-hub-rules.md` apply, so no reflection, Java 11, no blocking the client thread, and
  so on. Anything in core ships inside hub plugins and gets reviewed with them.
  `scripts/hub-check.ps1` scans the core classes each plugin ships.
- **No `Plugin` classes, no `@ConfigGroup` interfaces, no `@Subscribe` handlers** in core. Core is
  helpers that plugins call.
- **No static mutable state.** Several plugins can load core at once, and in the dev client they
  share one copy of the classes.
- **No new third-party dependencies.** Each one would land in every exported plugin and slow down
  hub review.
- Keep classes small and independent. Export copies only the core classes a plugin references,
  following references transitively, so a big tangled core ships to every plugin.
- Put unit tests in `core/src/test/java` (JUnit 4). They run with `./gradlew :core:test`.
- Resources go in `core/src/main/resources/com/josheey3/core/...`. Load them with
  `ImageUtil.loadImageResource(SomeCoreClass.class, "name.png")` so the path relocates with the class.

## How export handles core

`scripts/export-plugin.ps1 -Id <id>` copies only the core classes the plugin needs into the
standalone project and moves them from `com.josheey3.core.*` to `<pluginPackage>.core.*`,
rewriting the imports. Each hub plugin then owns a private copy and two hub plugins can never clash.
Because it's a text rewrite:
- Always refer to core classes through imports or fully-qualified `com.josheey3.core.` names.
  Don't build class names as strings.
- After changing core, run the export with `-Build` for each plugin that uses it before
  publishing.
