# HARD: device push requires Chris verbal GO + Studio-first

Effective 2026-09-23 CT (Helios voice Soft-PASS). Updated same morning after Chris correction.

## Soft-PASS flow (mandatory)
1. **Announce** the push out loud (tip / versionCode / versionName / what it contains)
2. **Wait** for Chris's explicit **go** or **okay**
3. **Push to Android Studio first** (sync Studio tree so Studio matches the tip)
4. Chris **confirms Studio shows the new version**
5. **Studio** handles device install/sync

## Soft-FAIL
- Soft-FAIL direct `adb install` to Motorola (or any device) from Helios/box/Ada scripts
- Soft-FAIL silent or automatic pushes
- Soft-FAIL leaving Studio on an older tip than the phone (adb bypass)

## Why
Fast adb pushes blanked LRT HUD during live-fetch (tip136/137 race). Also Studio sat on 1.0.126 while phone was on 1.0.127 because adb bypassed Studio.

## Owners
Helios (fleet enforcement) + Ada (LRT Git / Studio). Soft-FAIL Play still separate.