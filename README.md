# RuneLite plugin workspace

Custom [RuneLite](https://runelite.net) plugins, plus the tooling to build more.

## Requirements

- JDK 11+ (developed on Temurin 17). The Gradle wrapper downloads Gradle itself.
- PowerShell 7 (`pwsh`) for the scripts. Windows PowerShell 5.1 also works.
- Optional: IntelliJ IDEA Community. Open a single `plugins/<id>` folder as the project.

## Quick start

```bash
# one-time: reference checkout of RuneLite's source (git-ignored)
pwsh -File scripts/update-ref.ps1

# new plugin
pwsh -File scripts/new-plugin.ps1 -Name "Helmet Check" -Description "Warns when your head slot is empty"

# build and launch a dev client with it
cd plugins/helmet-check
./gradlew build
./gradlew run
```

Log in to the dev client from its **Jagex Accounts** sidebar panel
([details](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts)).

## Plugins

| Plugin | Description |
|---|---|
| _none yet_ | |

## Publishing

The Plugin Hub builds each plugin from its own public repository. See
[.claude/skills/runelite-plugin-hub/SKILL.md](.claude/skills/runelite-plugin-hub/SKILL.md) for
splitting a plugin out with `git subtree split` and submitting it to
[runelite/plugin-hub](https://github.com/runelite/plugin-hub).

## Working with Claude Code

`CLAUDE.md` and `.claude/skills/` give Claude the RuneLite rules, API notes and workflows
(new plugin, run client, publish).

## License

Plugins are BSD-2-Clause unless their folder says otherwise. `template/` is derived from
[runelite/example-plugin](https://github.com/runelite/example-plugin).
