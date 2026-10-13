# HARD — One app second = one real second (ALL launches)

**Chris voice HARD 2026-09-23 ~09:39 CT**  
**Applies to:** every launch family, live and theater. Not CZ-8A-only.

## Rule
1. Visual clock + vehicle animation track **wall time 1:1**. Never slow. Never speed multipliers on the clock.
2. Data refresh may trail **2–5s** (≤**30s** sparse). That is **data** lag only — must not throttle the sim/live clock.
3. `LIVE_WALL_LAG_SEC` is a **display offset** behind real, not a playback rate brake.

## Root causes (Helios Soft-PASS code read)
1. Wallpaper `tickSim` dt capped at **50ms**/frame while frames often longer → slow theater.
2. `RocketTelemetryModule.tickSim` also `deltaSec.coerceIn(0f, 0.25f)` → cannot catch up 1:1 after a hitch (MCC can pass up to 1s wall dt but only 0.25s advances).

## Fix owner: Ada (tip146)
- Wallpaper dt cap → match MCC `1000L`
- tickSim: advance by full wall dt (sane max ~1s after pause), remove 0.25f brake
- Live wall-clock path stays 1× via `effectiveSecondsFromNet`; scrub theater off live birds
- Studio only; Soft-FAIL phone/Play until Chris verifies 1:1 on device

## Canonical
`/workspace/agent-backups/Helios/HARD-realtime-1x-sim-clock.md`
