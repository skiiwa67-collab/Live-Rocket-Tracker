# Studio git-fetch root cause — standing
**Where we are:** 2026-09-23 ~12:51 CT · Helios HARD standing item

## Root-cause family (kill this class — do not re-patch tip-by-tip)

Studio PCs whose `git fetch` fails with **"Repository not found"** (usually private-repo auth) **silently stay on old commits** while GitHub already has newer tips.

**Gradle Sync does not pull git.** Syncing Gradle only reloads local sources; it never fetches `origin`.

### Proof today — Alarm Pro tip46
| Surface | State |
|---------|--------|
| GitHub `skiiwa67-collab/Alarm-Pro` `main` | `8bd76cb` · versionCode **46** / 0.5.22 |
| Studio clone `D:\Downloads\Claude\AlarmWeatherWidgetMK2\AlarmWeatherWidget` | stuck on `2f453a6` · versionCode **45** / 0.5.21 |
| `git fetch origin` on MiniME | FAIL: `Repository not found` |
| Chris action | "Synced" in Studio → still saw 45 |

Helios dual-wrote tip46 files into the Studio folder as a **one-off unblock**. That is **not** the standing fix.

### Standing fix class
1. Repair **git fetch / auth** on that Studio PC for the private repo so `git fetch` + `git pull` work.
2. Until fetch works, Ada must dual-push **GitHub + Studio disk** for every tip (same HARD pattern as LRT dual-push).
3. Never treat Gradle Sync as proof the PC has the new commit.

### Why this burned tokens today
Repeated tip46 Studio unblock loops (voice, Ada, Helios dual-write, BootReceiver name mismatch) all stem from this class: GitHub had 46; Studio could not fetch it. Chris named this the root-cause family behind today's ~44% usage burn.

### Dual-write targets
- Local: this file under `/workspace/agent-backups/Helios/`
- Notion Focus Tasks
- GitHub `docs/agent-backups/` (Alarm-Pro and/or LRT)
- Dropbox `/Grok-Agent-Backups/Helios/`
