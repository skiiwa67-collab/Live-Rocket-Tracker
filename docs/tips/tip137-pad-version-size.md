# tip137 — PAD button version mark size

**Date:** 2026-09-23  
**Stamp:** 137 / 1.0.127 on `stamp-85`  
**SHAs:** stamp `d72cb0e` · fix `c849dbe`  
**Play:** HOLD

## Problem
PAD plate `VERSION_CODE` text was unreadable on a large monitor (`textSize` was `oldSize * 0.55f` coerced to 11–14px).

## Fix
`drawPadVersionMark` in `RetroCommandWallpaperService.kt`:
- was: `(oldSize * 0.55f).coerceIn(11f, 14f)`
- now: `(oldSize * 0.78f).coerceIn(20f, 32f)`
- still pinned to top of PAD cell (#48 — no stacking on "PAD" baseline)

## Untouched
#48 FakeBold / CASC EN-only / PAD pin-top; tip135 CZ-8A gold; tip136 entertainment disclaimer.

## APK
`/workspace/LRT-tip137-1.0.127/LiveRocketTracker-tip137-1.0.127-debug.apk`  
md5 `cf5f8c6d5805dee3bd0a4385f0db0422`
