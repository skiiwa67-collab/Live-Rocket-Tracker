# LRT tip 155 event-tape sanity (read-only)

**Overall: FAIL**

Checked 2026-09-29 ~06:55 CT. No push, no commit, no Studio, no phone, no Play, no Gradle build.

The prompt says "version 1.5.5". That string is not in the tree. The tip155 stamp on branch `stamp-85` is **versionName `1.0.145`**, **versionCode `155`**. Same scheme as the phone's build 154 (`1.0.144` / `154`). Review below is of that stamp, not of a literal `1.5.5` versionName.

## SHA pair

| Role | SHA | When (CT) |
|---|---|---|
| Baseline: parent of the tip155 bump. `app/build.gradle.kts` still `versionCode = 154` / `versionName = "1.0.144"` (proven by the stamp patch). | `b0badf53a2473cebde2f31c3e0d3c92c23fdfd34` | 2026-09-24 08:53 CT (voice-docs commit; app version still 154) |
| tip155 version stamp | `320953582417a14de4cd904b81ef80abcd5b26bb` | authored 2026-09-29 06:49 CT, committer 06:49 CT |
| tip155 event-tape change (branch `stamp-85` HEAD) | `351e364cfd1409746efae2ce35115323645f58c4` | authored 2026-09-29 06:49 CT, committer 06:49 CT |

Compare `b0badf53a2473cebde2f31c3e0d3c92c23fdfd34...351e364cfd1409746efae2ce35115323645f58c4`: **ahead by 2, 3 files, +22 / -10**. Nothing else.

Historical versionCode 154 stamp (not the immediate parent; later commits kept vc 154): `84830f52c1479f03aac17a0c8c69415f93c5bd1e` "tip154: stamp versionCode 154 / versionName 1.0.144" (2026-09-23 11:37 CT).

`main` is **not** this code. `main` HEAD `8297fb4208b098f6a31ae4d7246e2618f47ced84` still has `app/build.gradle.kts` `versionCode = 19` / `versionName = "1.0.9"`. Ada must sync **`stamp-85`**, not `main`.

Local `/workspace/Live-Rocket-Tracker` is not this commit. Its `.git` is a broken shallow checkout (`git` rejects it: missing `refs/`, HEAD is raw SHA `77931f6ce04fefc69e0db80923bf64545ea624ec` tag `stamp-43-debug`). Working tree gradle is `versionCode = 43` / `versionName = "1.0.33"`. Bundles `/workspace/lrt-tip155.bundle` and `lrt-tip155-full.bundle` both point at `351e364cfd1409746efae2ce35115323645f58c4` but are not cloneable here (missing prerequisites `b0badf53…` and `225c3c3d…`). Sources below were read from GitHub at `351e364…`.

## 1. Unrelated files — PASS

Every file in the two-commit diff:

| File | Belongs to the event-tape fix? |
|---|---|
| `app/build.gradle.kts` | Yes. Version stamp only: `154`/`"1.0.144"` → `155`/`"1.0.145"`. |
| `app/src/main/java/com/ccos/retro/ui/CommandCenterActivity.kt` | Yes. The actual behavior change (`onResume` / `onPause` handler). |
| `app/src/main/java/com/ccos/retro/module/RocketTelemetryModule.kt` | Mixed, same commit. New `isLiveWallClockFlight` (`RocketTelemetryModule.kt:234-236`) is the gate the activity calls. Three other hunks are **comment-only** chip-label edits, not tape behavior: `RocketTelemetryModule.kt:545`, `:579`, `:905` (`1H\|2H\|48H` → `30m\|1h\|2h\|24h\|48h`). No logic change on those lines. |

No other paths. No asset, manifest, ProGuard, or wallpaper edits.

## 2. Debug flags — PASS

Diff adds no `BuildConfig.DEBUG` gate, no hardcoded debug boolean, no localhost/HTTP post, no TODO/FIXME, no agent logger. New comments are the tip155 note at `CommandCenterActivity.kt:268-270` and the public-wrapper kdoc at `RocketTelemetryModule.kt:234`.

Pre-existing, **not in the diff**: empty `catch (_: Exception)` in `closeVid` at `CommandCenterActivity.kt:233-234` (pause YouTube JS). Left alone.

## 3. Silent swallows — PASS for this diff

The tip155 hunks add no `Log.w`, no `catch`, no log-and-continue. Tape position is not hidden behind a new swallow.

Pre-existing and **outside this diff** (not charged to tip155, do not treat as a new defect): wallpaper telemetry draw swallows all exceptions at `RetroCommandWallpaperService.kt` inside `drawFrame` (`catch (_: Exception)` around the `eventMonitor.poll` block, file not modified by these commits). `RocketTelemetryModule.kt:517` and `:846` are `Log.i`, not in the diff.

## 4. Event-tape mechanism — FAIL

It does **not** keep the tape advancing for a whole flight on API 26+. It only keeps an Activity `Handler` posted while that process stays scheduled. There is no foreground service, no `startForeground`, no notification, no `AlarmManager`, no `WorkManager`, no wake lock, no battery-exemption. Manifest (`AndroidManifest.xml`) has INTERNET, ACCESS_NETWORK_STATE, VIBRATE, and a wallpaper service with `BIND_WALLPAPER` only. No `FOREGROUND_SERVICE` permission. No service class other than `RetroCommandWallpaperService`.

What the patch actually does:

- `onResume` (`CommandCenterActivity.kt:259-263`): if `running` is already true, do not repost `tick`.
- `onPause` (`CommandCenterActivity.kt:271-275`): if `telemetryModule.isLiveWallClockFlight(tracked)` then **leave `running == true` and do not `removeCallbacks(tick)`**. Otherwise stop the handler (sim/replay), as before.
- `tick` (`CommandCenterActivity.kt:68-82`): main-looper `Handler`, `postDelayed(this, 100L)`. For a live bird `simSecondsFromNet == null`, so it does **not** call `tickSim`. It calls `pushFlightEvents()` and `console.invalidate()`.

`isLiveWallClockFlight` (`RocketTelemetryModule.kt:235-242`) is a public alias of `isLiveWallClockBird`: false for `demo-*` and for `telemetryListMode == "historical"`; otherwise `launch.isActiveWatch(now) || !launch.isReplayable(now)`. `isActiveWatch` is the near-NET / in-flight window (`LaunchModels.kt:177-187`). `WATCH_AFTER_NET_SEC` is **60 minutes** (`LaunchModels.kt:244`). `isReplayable` is false for anything not yet 60 minutes past NET (`LaunchModels.kt:229-232`), so this gate is also true for an upcoming bird days before liftoff, not only during the flight. The handler does not re-check the gate after `onPause`, so once backgrounded "live" the 10 Hz loop never stops itself when the flight ends.

Why this is not "the whole flight":

- Tape **contents** are already a function of wall clock, not of tick count. `effectiveSecondsFromNet` for a live bird is `(now - pinnedNet) / 1000 - trail` (`RocketTelemetryModule.kt:259-276`). `FlightEventMonitor.occurred` filters the catalog with `it.tSec <= tSec` (`FlightEvents.kt:289-297`). Pausing the handler does not freeze T+. The next `tick` after resume paints the right tape. The handler only drives `poll()` side effects (banner + kinetic, `CommandCenterActivity.kt:410-428`) and invalidates a console that is not on screen.
- API 26+ background limits are not addressed. A started-from-background service would be illegal without a foreground promotion; this patch starts nothing. A cached activity process is free to be killed, and from Android 12 the cached-app freezer suspends the main looper (targetSdk is 36, `app/build.gradle.kts:8` and `:13`). Doze (screen off, unplugged, still) stops this Handler too. `FLAG_KEEP_SCREEN_ON` is set in `onCreate` (`CommandCenterActivity.kt:88`) and only applies while that window is visible. It is not a partial wake lock.
- The surface you actually see after leaving the app (home wallpaper) was not changed. Its engine stops drawing when not visible (`RetroCommandWallpaperService.kt:516-523`, `drawRunnable` returns immediately if `!visible` at `:408`) and, when visible, already computes T from `effectiveSecondsFromNet(now)` (`:1346`). Tip155 does not make the wallpaper tick while the launcher is not showing it.
- `onDestroy` (`CommandCenterActivity.kt:283-293`) does not clear `running` or remove `tick`. Back/EXIT calls `finish()` (`:323`, `:102`). `onPause` runs first; if the bird is "live", the handler stays posted and retains the destroyed Activity for the rest of the process lifetime, still calling `pushFlightEvents` / `console.invalidate` at 10 Hz.

Qualified-pass bar from the request ("depends on the process staying alive") is missed on purpose: this is that case, and it does not even keep the visible tape moving. **FAIL**, not a qualified pass.

## 5. Build cleanliness — PASS on static identity, compile not run

- `versionCode = 155`, `versionName = "1.0.145"` at `app/build.gradle.kts:14-15` on `351e364…`. Not the literal string `1.5.5`. Matches the existing tip scheme (tip154 = `1.0.144` / `154`). If Chris required the characters `1.5.5` in the manifest, this build does not have them.
- New symbol `isLiveWallClockFlight` is defined and called. No new resource, permission, or class. Diff does not add a file the build would need, and does not add a backup, keystore, `storePassword`, or `local.properties`.
- **Gradle was not run.** Do not treat this as a clean compile. The stamp-85 tree is not a working checkout on this box (local clone is stamp-43 and its `.git` is broken; the tip155 bundles are missing prerequisite commits). `/workspace/android-sdk` has platforms `android-34`, `android-36`, `android-37.0`, but this box often cannot fetch Google Maven/SDK pieces, and no assemble was attempted against `351e364…`.

## File-by-file issues

- `app/build.gradle.kts` — no functional issue. Identity is `1.0.145` / `155`, not `1.5.5`.
- `CommandCenterActivity.kt` — **issue.** Background "fix" is a 100 ms main-looper Handler left running across `onPause` (`:267-275`, tick `:68-82`). No FGS. Not stopped in `onDestroy` (`:283-293`). Does not advance the tape for a full flight once the process is cached, frozen, or killed. Live gate is not rechecked, so the 10 Hz loop can outlive the flight.
- `RocketTelemetryModule.kt` — gate itself (`:235-242`) matches the existing wall-clock rule and is true for upcoming birds, not only in-flight. Comment-only chip text at `:545`, `:579`, `:905`. No behavior bug in those hunks. Live T is already wall-clock at `:259-276`, which is why the handler is not what moves the tape.

