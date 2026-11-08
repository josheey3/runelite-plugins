---
name: runelite-run-client
description: Build, test and launch the RuneLite developer client for a plugin in this workspace, log in with a Jagex account, and read client logs. Use when asked to run, start, launch, test or debug a RuneLite plugin, or to check that a change works.
---

# Build, run and debug a RuneLite plugin

All commands run from the plugin directory, `plugins/<id>/`. Use the Gradle wrapper (`./gradlew`
in Git Bash, `.\gradlew.bat` in PowerShell); there is no global Gradle install. JDK 17 (Temurin) is
`JAVA_HOME`; plugins compile with `--release 11`.

| Goal | Command |
|---|---|
| Compile + unit tests | `./gradlew build` |
| Launch dev client with plugin | `./gradlew run` (runs `<Class>PluginTest.main` with `--developer-mode --debug`, assertions on) |
| Fat jar for sideloading | `./gradlew shadowJar` → `build/libs/<id>-all.jar` |
| Refresh RuneLite version | `./gradlew build --refresh-dependencies` (`latest.release` is cached for 24h) |

## Launching for the user

`./gradlew run` blocks until the client closes, so start it in the background and redirect output
to a log in the scratchpad, then wait for the line you need, e.g. your plugin's `log.debug` output
or `Client initialization took`. The first launch downloads the RuneLite client jars and the game
pack and can take a minute or two.

The dev client:
- opens a real RuneLite window on the user's desktop;
- uses the user's normal `~/.runelite` folder: same settings and profiles, and their installed
  Plugin Hub plugins load too, which is useful for testing against other plugins' panels;
- can run next to their normal RuneLite. If both run, port-binding errors from plugins such as
  `BindException: Address already in use` are expected and harmless.

To stop it, close the window, or kill only the dev client's `java.exe` (match `<Class>PluginTest`
in the command line). Never kill `RuneLite.exe`: that is the user's own client.

```powershell
Get-CimInstance Win32_Process -Filter "Name='java.exe'" | Where-Object { $_.CommandLine -match 'HelmetCheckPluginTest' } | ForEach-Object { Stop-Process -Id $_.ProcessId -Force }
```

## Logging in

The dev client isn't started by the Jagex Launcher, so the user signs in from the dev client itself:
open the **Jagex Accounts** sidebar panel (a developer-only built-in plugin), click
**Add Jagex Account**, finish sign-in in the browser, then click a character. The fallback is the
`--insecure-write-credentials` method in
<https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts>. The user always does this
themselves; never type, read or handle their credentials.

## Testing rules

- You can't verify in-game behaviour yourself. **Never** use screenshots, computer-use or input
  automation on the game: automating game input breaks Jagex's rules and can get the account banned.
- UI and Swing behaviour (the sidebar, panels, windows) can be checked from logs: add temporary
  `log.debug`/`log.warn` lines, launch, and grep the output. Remove them afterwards.
- After a change, tell the user exactly what to test: the main path, edge cases, and what "working"
  looks like. A clean start isn't a pass; wait for the user to confirm.

## Logs and files

- Console output: wherever you redirected `./gradlew run`.
- Client log: `~/.runelite/logs/client.log` (rolls daily).
- Settings: `~/.runelite/profiles2/`. Sideloaded jars: `~/.runelite/sideloaded-plugins/`, which
  only load in developer mode, i.e. a client not started by the launcher.
