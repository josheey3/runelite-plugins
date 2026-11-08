# RuneLite plugins

Custom [RuneLite](https://runelite.net) plugins in one repository, with a shared `core` library
and tooling to create, run and publish them.

## Plugins

| Plugin | Description |
|---|---|
| [Panel Popout](plugins/panel-popout) | Pop sidebar panels into their own windows (e.g. a second monitor) and save layouts as presets |

## Requirements

- JDK 11+ (developed on Temurin 17). The Gradle wrapper downloads Gradle itself.
- PowerShell 7 (`pwsh`) for the scripts. Windows PowerShell 5.1 also works.
- Optional: IntelliJ IDEA Community. Open the repo root; every plugin shows up as a module.

## Quick start

```bash
# one-time: reference checkout of RuneLite's source (git-ignored)
pwsh -File scripts/update-ref.ps1

# build everything
./gradlew build

# launch a dev client with one plugin
./gradlew :panel-popout:run

# new plugin
pwsh -File scripts/new-plugin.ps1 -Name "Helmet Check" -Description "Warns when your head slot is empty"
./gradlew :helmet-check:run
```

To log in to the dev client, open its **Jagex Accounts** sidebar panel → **Add Jagex Account**.
The fallback is the `--insecure-write-credentials` method on the
[RuneLite wiki](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts).

## Layout

```
build.gradle, settings.gradle   shared build; every plugins/* folder is included automatically
core/                           shared library (com.josheey3.core), bundled into each plugin
plugins/<id>/                   one plugin per folder
template/plugin/                skeleton used by scripts/new-plugin.ps1
template/standalone/            upstream example-plugin build files, used for hub export
scripts/                        new-plugin, export-plugin, update-ref
.claude/skills/, CLAUDE.md      instructions for Claude Code
```

## Publishing to the Plugin Hub

The hub builds each plugin from its own repository, so publishing exports one plugin with the
`core` classes it uses copied in and relocated:

```bash
pwsh -File scripts/export-plugin.ps1 -Id panel-popout -Build   # -> build/export/panel-popout
```

See [.claude/skills/runelite-plugin-hub/SKILL.md](.claude/skills/runelite-plugin-hub/SKILL.md) for
pushing that folder and opening the [runelite/plugin-hub](https://github.com/runelite/plugin-hub)
PR.

## License

BSD-2-Clause. `template/` is derived from
[runelite/example-plugin](https://github.com/runelite/example-plugin).
