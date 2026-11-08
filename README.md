# RuneLite plugins

Custom [RuneLite](https://runelite.net) plugins in one repository, with a shared `core` library
and tooling to create, run and publish them.

## Plugins

| Plugin | Description | Plugin Hub |
|---|---|---|
| [Panel Popout](plugins/panel-popout) | Pop sidebar panels into their own windows (e.g. a second monitor) and save layouts as presets | Not eligible ([why](docs/plugin-hub-rules.md#precedent)); use `runClient` |

Plugins aim for the [Plugin Hub](https://runelite.net/plugin-hub) by default. See
[docs/plugin-hub-rules.md](docs/plugin-hub-rules.md) and run `scripts/hub-check.ps1` before submitting.

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

# everyday client: RuneLite with every plugin in this repo loaded
./gradlew runClient

# new plugin
pwsh -File scripts/new-plugin.ps1 -Name "Helmet Check" -Description "Warns when your head slot is empty"
./gradlew :helmet-check:run
```

To log in to a dev client, open its **Jagex Accounts** sidebar panel → **Add Jagex Account**.
The fallback is the `--insecure-write-credentials` method on the
[RuneLite wiki](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts).

## Layout

```
build.gradle, settings.gradle   shared build; every plugins/* folder is included automatically
core/                           shared library (com.josheey3.core), bundled into each plugin
dev-client/                     ./gradlew runClient: RuneLite with every plugin loaded
plugins/<id>/                   one plugin per folder
template/plugin/                skeleton used by scripts/new-plugin.ps1
template/standalone/            upstream example-plugin build files, used for hub export
scripts/                        new-plugin, hub-check, export-plugin, update-ref
docs/                           Plugin Hub rules, RuneLite agent guidelines
.claude/skills/, CLAUDE.md      instructions for Claude Code
```

## Publishing to the Plugin Hub

The hub builds each plugin from its own repository, so publishing exports one plugin with the
`core` classes it uses copied in and relocated:

```bash
pwsh -File scripts/hub-check.ps1 -Id <id> -Build         # rules check + standalone hub build
pwsh -File scripts/export-plugin.ps1 -Id <id> -Build     # -> build/export/<id>
```

See [.claude/skills/runelite-plugin-hub/SKILL.md](.claude/skills/runelite-plugin-hub/SKILL.md) for
pushing that folder and opening the [runelite/plugin-hub](https://github.com/runelite/plugin-hub)
PR.

## License

BSD-2-Clause. `template/` is derived from
[runelite/example-plugin](https://github.com/runelite/example-plugin).
