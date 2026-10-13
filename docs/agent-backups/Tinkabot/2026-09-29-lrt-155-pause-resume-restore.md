# LRT tip155 pause/resume restore check

**Overall: PASS** (read-only, 2026-09-29 ~07:13 CT). No Studio, no phone, no Play.

Regression commit: `351e364cfd1409746efae2ce35115323645f58c4` on `stamp-85`.
Restore commit: `b367256505bfce1660c82f4976d147832561f438` (2026-09-29 07:12 CT), parent `9b2f8558d3b361431f3c03ea4b0910d4f2c4d70e` (historic Past/replay).

## Checks

- onResume always posts the tick again. `CommandCenterActivity.kt` sets `running = true`, `removeCallbacks(tick)`, `post(tick)` with no `if (!running)` guard.
- onPause always removes it. Same file sets `running = false` and `removeCallbacks(tick)` for every flight, including live. The tip155 "leave it running" branch is gone.
- Wallpaper file is not in this commit. Only two files changed.
- Historic Past/replay `9b2f855` is the parent, so it is still on `stamp-85`.

## Extra, still in scope

The commit also deletes `isLiveWallClockFlight` from `RocketTelemetryModule.kt` (4 lines). That function existed only to serve the pause branch this commit removes. The pause patch was its only call site. Chip-label comments from `351e364` are left as they were. No foreground service was added.

Version stamp stays `155` / `1.0.145` from `32095358`. Phone stays on 154.
