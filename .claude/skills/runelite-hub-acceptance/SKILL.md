---
name: runelite-hub-acceptance
description: Make sure a RuneLite plugin idea or change can be accepted on the Plugin Hub - check it against the hub's rules and rejected-feature list BEFORE designing or writing code, keep it inside the rules while building, and run scripts/hub-check.ps1 before every commit. Use at the start of any new plugin or feature, when choosing how to implement something, and whenever the user asks whether something is allowed or would be accepted.
---

# Plugin Hub acceptance comes first

Getting onto the Plugin Hub is the **default goal for every plugin in this repo**. It's the only
way into the user's normal (Jagex Launcher) RuneLite client. A plugin the hub won't accept can
only be used through `./gradlew runClient`, so treat "would the hub accept this?" as a design
constraint, not a final check.

The rules, sources and precedent are in `docs/plugin-hub-rules.md`. Read it at the start of any
new plugin or significant feature.

## 1. Before designing (feasibility gate)

Answer these before writing code. Any "yes" is a stop sign: tell the user and offer the options
below rather than building it anyway.

1. Does it change **RuneLite's UI or another plugin's UI**: the sidebar, root frame, overlays or
   infoboxes it doesn't own, other plugins' panels? (Rejected category.)
2. Does it act on the **client window**: minimise, move, resize or focus it?
3. Does it change **other plugins' config**?
4. Does it hit a game rule (boss/PvP helpers, menu changes that send actions, input automation,
   autotyping)? See `docs/runelite-agent-guidelines.md`.
5. Does it need forbidden code (reflection, files via `java.io`, processes, JNI) or a new
   third-party library?
6. Is it on the rejected list or a `wontfix` issue? Search `_ref/wiki/Rejected-or-Rolled-Back-Features.md`
   and `gh search issues --repo runelite/runelite "<idea>"`.
7. Does a hub plugin already do it? `ls _ref/plugin-hub/plugins | grep -i <word>`, then read the
   manifest: `disabled=` means it was pulled, so find out why. The hub prefers contributing to an
   existing plugin over a duplicate.

If the idea fails the gate, present the honest options:
- **Reshape it** to stay inside the rules (own panel or window instead of changing RuneLite's;
  opt-in; drop the risky part).
- **RuneLite core PR** for features that belong in the client itself.
- **Personal-only plugin**, usable via `./gradlew runClient`. Only if the user explicitly
  chooses this; then mark it in the plugin README and the root README table.

## 2. While building

- Follow `docs/runelite-agent-guidelines.md` exactly (threads, config, logging, cleanup).
- Prefer the simplest readable approach. Reviewers (human and bot) must be able to read every line.
- Inject `Gson`/`OkHttpClient`; use `Filepath`; use `LinkBrowser.browse`; use gameval IDs.
- Make surprising behaviour opt-in and network features off by default, with the standard warning.
- Keep dev-only helpers in test sources named `*SelfTest*.java`. Export strips them.
- Write the README for the hub page: what it does, how to use it, screenshots. Add "How it works"
  for anything unusual.

## 3. Before every commit to a plugin

```bash
pwsh -NoProfile -File scripts/hub-check.ps1 -Id <id>          # quick source/manifest scan
pwsh -NoProfile -File scripts/hub-check.ps1 -Id <id> -Build   # + standalone Java 11 hub build
```

Fix every ERROR. Read every WARN and either fix it or be able to justify it to a reviewer. The
script can't judge game rules or intent, so go through the gate questions again for the change.

## Status of plugins in this repo

| Plugin | Hub status |
|---|---|
| `panel-popout` | **Not eligible**: it moves other plugins' panels out of the sidebar (core UI). Personal use via `runClient`; the policy-compatible route is a RuneLite core PR (runelite/runelite#2463). |

Update this table when adding plugins.
