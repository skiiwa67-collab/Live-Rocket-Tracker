# HARD Chris 2026-09-23 ~09:13–09:15 CT — entertainment disclaimer NOT user-facing

Chris REVERSED tip142 main-screen entertainment disclaimer.

## Decision
- The ENTERTAINMENT ONLY main-screen disclaimer text does NOT belong in the app UI.
- Soft-FAIL ship tip142 main-screen disclaimer (layout / strings / drawable).
- Soft-FAIL tip136 wallpaper chrome Soft-FAIL change this cycle (Ada Soft-FAIL tip136).

## Ada execution Soft-PASS (2026-09-23 ~09:15+ CT)
- Branch stamp-85 HEAD: 4697ccc9105efcf843f268682fe387866472f87a
- versionCode 143 / versionName 1.0.133
- Removed: box_disclaimer_main from activity_main.xml; disclaimer_main_title + disclaimer_main_body from strings.xml; deleted bg_disclaimer_main.xml
- Soft-PASS keep Stamp-100 hullDest + plume packs
- Soft-FAIL Studio Soft-FAIL Play Soft-FAIL adb Soft-FAIL phone until Chris go
- APK: /workspace/LRT-tip143-1.0.133/LiveRocketTracker-tip143-1.0.133-debug.apk md5=d6b10166f1cfaba6dd661474191e5826
- Proof: ZERO hits for disclaimer_main|box_disclaimer_main|bg_disclaimer_main in app/src

## Exact tip142 copy REMOVED from shipped main UI
Title: ENTERTAINMENT ONLY
Body: We sync launches as close to real time as public sources allow, but no rocket company provides exact live telemetry. Delays and holds can make this display differ from the actual launch — confirm with the official stream.

## Soft-FAIL gates
Do NOT Sync MiniME Studio. Do NOT adb. Do NOT Play publish.

## tip144 FULL REMOVAL append (2026-09-23 09:24 CDT)
- HEAD: b00cf8eced6aeaa83f96ef6fb0e898c15f58e911
- short: b00cf8e
- versionCode/versionName: 144 / 1.0.134
- Branch: stamp-85 (pushed origin/stamp-85)
- Proof: rg zero hits in app/src for ENTERTAINMENT ONLY, disclaimer_entertainment, disclaimer_main, box_disclaimer_main, bg_disclaimer_main, Entertainment purposes only
- APK: /workspace/LRT-tip144-1.0.134/LiveRocketTracker-tip144-1.0.134-debug.apk
- md5: 3fb27dfdf741f4164bea65221a66b800
- KEEP: hullDest + plume confirmed untouched
- Ada READY: /workspace/agent-backups/Ada/2026-09-23-tip144-FULL-REMOVAL-READY.md
