---
name: runelite-plugin-hub
description: Prepare a plugin from this workspace for the RuneLite Plugin Hub - review it against the rules, export it as a standalone repository with core/ folded in, and open the runelite/plugin-hub pull request. Use when the user wants to publish, release, submit or update a plugin on the Plugin Hub, or asks whether a plugin would be accepted.
---

# Publish to the Plugin Hub

The hub builds each plugin from **the root of its own public GitHub repository** at a fixed
commit, using the standard example-plugin build. This workspace keeps every plugin in one repo and
shares `core/`, so publishing goes through an export step. Development always stays here; the
exported repository is only a publishing target.

## 1. Pre-flight review

First confirm the plugin passed the feasibility gate in the `runelite-hub-acceptance` skill. A plugin
marked "Not eligible" there (for example `panel-popout`) must not be submitted. Then run:

```bash
pwsh -NoProfile -File scripts/hub-check.ps1 -Id <id> -Build
```

It must report 0 errors. Then check the plugin by hand against `docs/plugin-hub-rules.md` and
`docs/runelite-agent-guidelines.md`. Specifically:
- Nothing left from the template: `com.example`, `Example*`, the greeter config item.
- No reflection, JNI/JNA, `Process`/`ProcessBuilder`, dynamic class loading, Java serialization.
- Third-party server features are opt-in, with the standard `warning`.
- `runelite-plugin.properties`: `displayName`, `author`, `description`, `tags`,
  `plugins=<fully.qualified.PluginClass>`, `build=standard` (unless custom dependencies are needed).
- Optional `icon.png` in `plugins/<id>/`, at most 48x72 px, and a real PNG.
- `README.md` that explains the feature (it's the hub page) and a BSD-2 `LICENSE`.
- New third-party dependencies need hub dependency verification and slow review down a lot, so
  avoid them.

Reviewers (humans plus an automated review bot since April 2026) reject what they can't easily
confirm is within the rules: "if we cannot review every single line of source code that your plugin
will execute, we will not accept it". Plugins that change RuneLite's own UI or reach into other
plugins are a rejected category (see `docs/plugin-hub-rules.md`, Precedent). Accepted plugins can
still be disabled later.

## 2. Export

```bash
pwsh -NoProfile -File scripts/export-plugin.ps1 -Id <id> -Build
```

This writes `build/export/<id>/` (git-ignored): the plugin's files, the upstream
`build.gradle`/`settings.gradle`, the Gradle wrapper, `.gitattributes`, and the core classes the
plugin uses, relocated to `<pluginPackage>.core`. `-Build` runs the standalone build to prove it
compiles the way the hub will build it. Dev-only `*SelfTest*.java` files are dropped and
`<Class>PluginTest` is regenerated from the template, so reviewers only see plugin code. Fix any warning about plugin-specific dependencies by
adding them to the exported `build.gradle`.

## 3. Push the standalone repository

Pushing creates public code, so confirm with the user first. The publishing repository is
`josheey3/<id>` and must be **public**. Push as described in `CLAUDE.local.md`,
never with the machine's default account.

```bash
cd build/export/<id>
git init -b master && git add -A && git update-index --chmod=+x gradlew
git -c user.name="Josh Burtsal" -c user.email="17991401+josheey3@users.noreply.github.com" commit -m "<id> <version or monorepo sha>"
GH_TOKEN=$JTOKEN gh repo create josheey3/<id> --public --description "<description>" --source . --remote origin
git -c credential.helper= -c 'credential.helper=!f() { echo username=josheey3; echo "password=$JTOKEN"; }; f' push -u origin master
```

For updates, clone the existing standalone repo, re-export over it (keep its `.git`), commit and
push. Put the monorepo commit sha in the commit message so each hub release can be traced back.

## 4. Submit

1. Fork `runelite/plugin-hub` to `josheey3`.
2. On a branch named after the plugin, add `plugins/<id>` containing:
   ```
   repository=https://github.com/josheey3/<id>.git
   commit=<full 40-char sha in the standalone repo>
   ```
3. Open a PR to `runelite/plugin-hub` with a short description. End the description with:
   `Generated-by: Claude Code`
4. Watch the `build` check and the "RuneLite Plugin Hub Checks" comment, fix things here, re-export,
   push, and update `commit=` in the **same** PR. Don't open new PRs.

Opening the PR is outward-facing. Show the user the manifest and PR text and get a yes first.

## Updating a published plugin

Re-export, push to the standalone repo, then update `commit=` in `plugins/<id>` on a fresh branch
from `upstream/master` in the plugin-hub fork and open a PR.
