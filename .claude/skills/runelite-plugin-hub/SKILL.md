---
name: runelite-plugin-hub
description: Prepare a plugin from this workspace for the RuneLite Plugin Hub, split it into its own public repository and open the runelite/plugin-hub pull request. Use when the user wants to publish, release, submit or update a plugin on the Plugin Hub, or asks whether a plugin would be accepted.
---

# Publish to the Plugin Hub

The hub builds each plugin from **its own public GitHub repository** at a fixed commit. This
workspace keeps plugins under `plugins/<id>/`, so publishing means splitting one out.

## 1. Pre-flight review

Check the plugin against `docs/runelite-agent-guidelines.md`: rules, forbidden features, threading,
config. Specifically:
- Nothing left from the template: `com.example`, `Example*`, the greeter config item.
- No reflection, JNI/JNA, `Process`/`ProcessBuilder`, dynamic class loading, Java serialization.
- Java 11 compatible: `./gradlew build` with the template's `options.release.set(11)`.
- Third-party server features are opt-in, with the standard `warning`.
- `runelite-plugin.properties`: `displayName`, `author`, `description`, `tags`,
  `plugins=<fully.qualified.PluginClass>`, `build=standard` (unless custom dependencies are needed).
- Optional `icon.png` at the repository root, at most 48x72 px, and a real PNG.
- `README.md` that explains the feature (shown on the hub page) and a BSD-2 `LICENSE`.
- New third-party dependencies need hub dependency verification and slow review down a lot, so
  avoid them.

Reviewers reject what they can't easily confirm is within the rules: "If it is difficult for us to
ensure the plugin isn't against the rules we will not merge it". Plugins that change core client UI
or reach into other plugins get extra scrutiny, so flag that risk to the user honestly.

## 2. Split into its own repository

From the workspace root, with history:

```bash
git subtree split --prefix=plugins/<id> -b publish/<id>
gh repo create <owner>/<id> --public --description "<description>"
git push https://github.com/<owner>/<id>.git publish/<id>:master
```

Creating a public repository and pushing to it publishes code, so confirm with the user first.
Copy `docs/runelite-agent-guidelines.md` into the new repository as `AGENTS.md` if they want it.

The `gradlew` file must keep LF line endings and its executable bit, which `.gitattributes`
handles. Check with `git ls-files -s plugins/<id>/gradlew`; the mode should be `100755`.

## 3. Submit

1. Fork `runelite/plugin-hub` (`gh repo fork runelite/plugin-hub --clone=false`).
2. On a branch named after the plugin, add `plugins/<id>` containing:
   ```
   repository=https://github.com/<owner>/<id>.git
   commit=<full 40-char sha of the commit to publish>
   ```
3. Open a PR to `runelite/plugin-hub` with a short description. End the description with:
   `Generated-by: Claude Code`
4. Watch the `build` check and the "RuneLite Plugin Hub Checks" comment, fix, push to the plugin
   repo, and update `commit=` in the **same** PR. Don't open new PRs.

Opening the PR is outward-facing. Show the user the manifest and PR text and get a yes first.

## Updating a published plugin

Push the new commits to the plugin repository, then update `commit=` in `plugins/<id>` on a fresh
branch from `upstream/master` in the plugin-hub fork and open a PR.
