---
name: runelite-run-client
description: Build, test and launch the RuneLite developer client for a plugin in this workspace, log in with a Jagex account, run automated UI self-tests, and read client logs. Use when asked to run, start, launch, test or debug a RuneLite plugin, or to check that a change works.
---

# Build, run and debug a RuneLite plugin

Run everything from the repository root with the root Gradle wrapper (`./gradlew` in Git Bash,
`.\gradlew.bat` in PowerShell). There is no global Gradle install. JDK 17 (Temurin) is
`JAVA_HOME`; everything compiles with `--release 11`.

| Goal | Command |
|---|---|
| Compile + unit tests, everything | `./gradlew build` |
| One plugin | `./gradlew :<id>:build` |
| Launch dev client with a plugin | `./gradlew :<id>:run` (runs `<Class>PluginTest.main` with `--developer-mode --debug`, assertions on) |
| Everyday client with **all** plugins | `./gradlew runClient` (`dev-client/`: launcher generated from every `plugins=` line; developer mode, no `--debug`) |
| Plugin jar for sideloading (includes core) | `./gradlew :<id>:jar` → `plugins/<id>/build/libs/<id>.jar` |
| Fat jar that runs RuneLite + plugin | `./gradlew :<id>:shadowJar` → `plugins/<id>/build/libs/<id>-all.jar` |
| Refresh the RuneLite version | `./gradlew build --refresh-dependencies` (`latest.release` is cached for 24h) |

## Launching for the user

`./gradlew :<id>:run` blocks until the client closes. Start it in the background, redirect output
to a log in the scratchpad, and wait for the lines you need (your plugin's `log.debug` output, or
`Plugin <Class>Plugin is now running`). The first launch downloads the client jars and game pack
and can take a minute or two.

The dev client:
- opens a real RuneLite window on the user's desktop (they have two 1920x1080 monitors);
- uses the user's normal `~/.runelite` folder: same settings and profiles, and their installed
  Plugin Hub plugins load too, which is good for testing against other plugins' panels;
- can run next to their normal RuneLite. Expect harmless noise: `BindException: Address already in
  use` from plugins, and a `ReflectUtil` `InaccessibleObjectException` at DEBUG level on JDK 17.

To stop it, close the window, or kill only the dev client's `java.exe` by matching its main class
(`devclient.DevClient` for `runClient`). The user may have their own dev client open with the same
main class, so record the PID you started and kill only that. **Never** kill `RuneLite.exe`: that
is the user's own client.

```powershell
Get-CimInstance Win32_Process -Filter "Name='java.exe'" | Where-Object { $_.CommandLine -match 'HelmetCheckPluginTest' } | ForEach-Object { Stop-Process -Id $_.ProcessId -Force }
```

## Logging in (the user does this, never you)

Normally the user starts RuneLite from the Jagex Launcher, which hands it their session. The dev
client isn't started by the launcher, so use one of these:

1. **Jagex Accounts panel (preferred):** in the dev client open the **Jagex Accounts** sidebar
   panel (a developer-only built-in plugin) → **Add Jagex Account** → sign in in the browser →
   click a character. Accounts stay listed for next time.
2. **Credentials file (fallback, from the RuneLite wiki):** run **RuneLite (configure)** from the
   Start menu and add `--insecure-write-credentials` to *Client arguments*. Then launch RuneLite
   once from the Jagex Launcher. That writes `~/.runelite/credentials.properties`, and the dev
   client uses it. The file is effectively a password: never read, print or commit it. Delete it
   (and remove the argument) to revoke.
   <https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts>

## Testing rules

- You can't verify in-game behaviour. **Never** use screenshots, computer-use or input automation on
  the game: automating game input breaks Jagex's rules and can get the account banned.
- Automated checks of our **own Swing UI** are fine and encouraged. Put a self-test class in the
  plugin's `src/test/java` that runs from `<Class>PluginTest.main` only when an environment
  variable is set, e.g. `PANELPOPOUT_SELFTEST=1 ./gradlew :panel-popout:run`. It drives the UI
  through the plugin's own APIs and Swing components, and logs `SELFTEST ok/FAIL` lines and a final
  `SELFTEST PASS`/`FAIL`. See `plugins/panel-popout/src/test/.../PopoutSelfTest.java`. Don't change
  persisted user settings: stop and start plugins with `PluginManager.stopPlugin/startPlugin`,
  never `setPluginEnabled`, and clean up any state you create.
- Test sources can't use Lombok (same as the hub build). Use `LoggerFactory.getLogger`.
- After a change, tell the user exactly what to test: the main path, edge cases, and what "working"
  looks like. A clean start or a self-test pass doesn't replace their check; wait for them to confirm.

## Logs and files

- Console output: wherever you redirected `./gradlew :<id>:run`.
- Client log: `~/.runelite/logs/client.log` (rolls daily).
- Settings: `~/.runelite/profiles2/`. Sideloaded jars: `~/.runelite/sideloaded-plugins/`, which
  only load in developer mode, i.e. a client not started by the launcher.
