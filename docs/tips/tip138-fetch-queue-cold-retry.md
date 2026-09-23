# tip138 — live-fetch race on fast adb push

**Date:** 2026-09-23  
**Stamp:** 138 / 1.0.128 on `stamp-85`  
**SHAs:** stamp `fce7faf` · fix `fb562e0`  
**Play:** HOLD

## Bug
Fast Motorola adb install (tip136/137) → HUD painted 8 buttons but live launch never refreshed (blank/NO LOCK) because in-flight `refreshIfNeeded` dropped later kicks and `lastLaunchRefresh` advanced anyway.

## Fix
1. `LaunchDataProvider.refreshIfNeeded`: queue force+onDone while `sharedFetching`; drain queue in `finally`.
2. Wallpaper drawFrame: while `isFetchingData()` cold, re-kick every 2s (forceRefresh).
3. Top HUD null-launch line uses `noTrackLabel` → shows FETCHING instead of silent NO LAUNCH TRACKED.

## Untouched
tip137 pad version size; tip136 disclaimer; tip135 CZ-8A gold; #48 plates.
