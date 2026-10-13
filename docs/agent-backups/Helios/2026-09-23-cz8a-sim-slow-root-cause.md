# CZ-8A sim "way too slow" — root cause 2026-09-23 ~09:38 CT

## How speed works
1. **Live CURRENT / active watch:** `effectiveSecondsFromNet` = `(now - NET)/1000 - LIVE_WALL_LAG_SEC` (5s). Rate = **1× wall clock**. Hold caps at T−0.5. No speed multiplier. Not CZ-8A-specific.
2. **Historic / demo theater:** `tickSim(deltaSec)` advances memory at 1× *if* delta equals wall delta.

## Bug (theater / leftover sim on wallpaper)
`RetroCommandWallpaperService` caps `dt` at **50 ms** per frame while `postDelayed(55)` runs **after** draw. Slow frames (heavy paint + tip138 2s cold refresh) → sim advances 50ms per 150–300ms wall → **3–6× too slow**.
MCC `CommandCenterActivity` caps at **1000 ms** — fine.

## Not the cause
- tip138 retry / fetch queue: no rate change; can worsen frame time
- 5s lag: offset only, not slowdown
- No CZ-8A-only speed config
- Art: N/A for clock rate (Da Vinci not owner)

## Fix owner: Ada
Wallpaper `coerceIn(0L, 50L)` → `1000L` (match MCC); optional frame schedule compensate. Studio only; Soft-FAIL phone/Play.
