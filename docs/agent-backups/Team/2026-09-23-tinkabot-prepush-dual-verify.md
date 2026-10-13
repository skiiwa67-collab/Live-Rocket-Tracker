# Helios GO GATE 1 — pre-push dual verify (tinkabot)

**When:** 2026-09-23 ~08:15 CDT  
**Auditor:** tinkabot (independent; Soft-FAIL invent)  
**Subject:** Staged LRT fixes tip134 / tip136 / tip139 / tip140 prior to Studio-only push  
**Overall: PASS** (programming Soft-PASS for staged tree + tip140 APK)  
**Standing Soft-FAIL:** Studio push / Play publish / adb / invent / interrupt Chris until Chris explicit go. Soft-FAIL Play forever.

## Surfaces

| Surface | Evidence |
|---------|----------|
| GO | `/workspace/agent-backups/Helios/2026-09-23-tinkabot-prepush-dual-verify-GO.txt` |
| HOLD | `/workspace/agent-backups/Helios/2026-09-23-ada-prepush-HOLD-until-dual-verify.txt` |
| Source HEAD | `/workspace/RetroCommandCenter` @ `cead4511d45a36c5b4296b98dc87869f909ac9d3` (`stamp-85` = tip140) |
| tip139 tiphead (ancestor) | `113ad7f` art+wire ALL FOUR |
| tip139 Soft-PASS APK | `/workspace/LRT-tip139-1.0.129/LiveRocketTracker-tip139-1.0.129-debug.apk` md5 `b0dd6c1069756653ba3c1d70dcafac96` · versionCode 139 / 1.0.129 |
| tip140 Soft-PASS APK | `/workspace/LRT-tip140-1.0.130/LiveRocketTracker-tip140-1.0.130-debug.apk` md5 `2701ff18382ebe975e7cfd09c71fc4ee` · versionCode 140 / 1.0.130 |
| tip134 APK | `/workspace/LRT-tip134-1.0.124/LiveRocketTracker-tip134-1.0.124-debug.apk` md5 `e4f2142f754645be0fb32f3b7d6fe2a9` · 134 / 1.0.124 |
| tip136 APK | `/workspace/LRT-tip136-1.0.126/LiveRocketTracker-tip136-1.0.126-debug.apk` md5 `8950f74cb9c470baae16cd9158eab9cc` · 136 / 1.0.126 |
| Prior tip139 audit | `/workspace/agent-backups/tinkabot/2026-09-23-tip139-programming-audit.md` Soft-PASS PASS |
| Play HARD | `/workspace/agent-backups/Helios/HARD-no-agent-play-publish.md` (+ team mirror) |

Ancestry Soft-PASS on HEAD: `87b86d0` (#48 tip134) → `d404882` (tip136) → `113ad7f` (tip139) → `cead451` (tip140).

---

## Checklist

### 1) tip134 — garbled labels fix — **PASS**

**Docs / commits Soft-PASS:**
- Commit `87b86d0` — `fix #48: clean EN HUD plates (FakeBold off, CASC EN-only, PAD mark top)`
- Stamp `d31c8dd` — versionCode 134 / 1.0.124
- LRT #48 / tip134 Soft-PASS retained through tip140 HEAD

**Source wired Soft-PASS** (`app/src/main/java/com/ccos/retro/wallpaper/RetroCommandWallpaperService.kt`):
- `telBold()` / `telRegular()` Soft-PASS `isFakeBoldText = false` + real `Typeface.BOLD` (~L230–237) — stops MPHH/RANGGE FakeBold double-stroke
- `drawCascBtn` Soft-PASS passes empty local string to `drawFullFaceLabel` (~L2188–2194) — CASC EN-only; `zhFor` kept unused for later
- `drawPadVersionMark` Soft-PASS pins VERSION_CODE to TOP of PAD cell + clears FakeBold (~L2056–2077); tip137 size `(oldSize * 0.78f).coerceIn(20f, 32f)` Soft-PASS retained
- Clears FakeBold at drawButtons / drawFullFaceLabel / end drawTelemetrySurface Soft-PASS (~L2062–2090, ~L2745–2746)

**APK Soft-PASS:** tip134 APK present; tip140 dex Soft-PASS strings `isFakeBoldText` / `setFakeBoldText`; no regression of #48 wiring in HEAD.

**No regression Soft-PASS:** tip137 pad size Soft-PASS still nested inside tip134 PAD mark path.

### 2) tip136 — wallpaper entertainment disclaimer — **PASS**

**Commit Soft-PASS:** `d404882` — `tip136: wallpaper entertainment-only disclaimer Soft-PASS`  
Files: `RetroCommandWallpaperService.kt` + `app/src/main/res/values/strings.xml`

**Source strings Soft-PASS** (`strings.xml` L4, L8–10):
- `wallpaper_description` — entertainment only / not real-time telemetry
- `disclaimer_entertainment_short` — `ENTERTAINMENT ONLY · not real telemetry`
- `disclaimer_entertainment_body` — companies do not share real-time telemetry…

**Wallpaper path Soft-PASS:**
- Event-jump footer Soft-PASS (~L3211–3220): `ENTERTAINMENT ONLY · not real telemetry`
- MISSION page Soft-PASS (~L7147–7160): `ENTERTAINMENT PURPOSES ONLY` + body

**APK Soft-PASS (tip140 aapt):** resources Soft-PASS  
`disclaimer_entertainment_short` / `disclaimer_entertainment_body` / `wallpaper_description`  
Dex Soft-PASS: `ENTERTAINMENT ONLY` / `not real telemetry`

**No regression Soft-PASS:** tip136 Soft-PASS ancestor of tip139/140; docs Soft-PASS `tip136 has no clock fix` (0797703).

### 3) tip139 / tip140 and other pending staged fixes — **PASS**

**tip139 Soft-PASS (still in tip140 tree):**
- Ada READY: `/workspace/agent-backups/Ada/2026-09-23-tip139-allfour-wired-READY.md`
- Helios contents: `/workspace/agent-backups/Helios/2026-09-23-tip139-ALLFOUR-contents.md`
- APK Soft-PASS reconfirm md5 `b0dd6c1069756653ba3c1d70dcafac96` · aapt `versionCode='139'` `versionName='1.0.129'`
- Catalog ids Soft-PASS lines: cz6a:440 sr75:723 nuri:759 h3:798
- tip137 pad font Soft-PASS retained (`0.78f` coerceIn 20–32)
- tip138 fetch queue Soft-PASS (`LaunchDataProvider.refreshIfNeeded` queue-while-fetching) + cold 2s re-kick Soft-PASS in wallpaper drawFrame

**tip140 Soft-PASS staged (present, not Soft-FAIL missing):**
- Ada READY: `/workspace/agent-backups/Ada/2026-09-23-tip140-READY.md`
- Helios verify: `/workspace/agent-backups/Helios/2026-09-23-tip140-READY-helios-verify.json`
- HEAD Soft-PASS `cead451` + stamp `a4db003` · `app/build.gradle.kts` versionCode 140 / versionName 1.0.130
- APK Soft-PASS md5 `2701ff18382ebe975e7cfd09c71fc4ee` · aapt 140 / 1.0.130
- Wires Soft-PASS (`AppPrefs.kt`):
  - getter default `cmd_page` → **2** (~L140)
  - `applyCommandPageDefaultV1()` one-shot `ccos_cmd_page_default_v1` (~L314–320)
  - `ensurePaidAppDefaults` writes `cmd_page` **2** (~L338)
  - SetupActivity Soft-PASS still sets page 2 (`SetupActivity.kt` L38)
  - Settings −/+ override Soft-PASS kept (`MainActivity.kt`)
  - Dex Soft-PASS: `ccos_cmd_page_default_v1` / `cmd_page`

**Other pending in Ada/Helios backups Soft-PASS:** only tip139 + tip140 READY Soft-PASS staged. No extra unnamed tip Soft-PASS claimed beyond tip134–140 chain Soft-PASS verified.

### 4) No missing VehicleCatalog or resource wiring — **PASS** (staged source + tip139/140 APK Soft-PASS)

**VehicleCatalog.kt Soft-PASS:** `id` + `drawFamily` for `cz6a` / `sr75` / `nuri` / `h3` at lines 440 / 723 / 759 / 798; consts `CZ6A_DRAWABLE` / `SR75_DRAWABLE` / `NURI_DRAWABLE` Soft-PASS.

**VehicleDraw.kt Soft-PASS:** when-branches Soft-PASS `cz6a` / `sr75` / `nuri` / `h3` (art-then-tanks / `cz6a()` / `h3()`).

**GOLD art Soft-PASS md5 MATCH** Helios ALLFOUR Soft-PASS + prior tip139 audit — identical in:
- `/workspace/RetroCommandCenter/app/src/main/res/drawable/`
- tip139 APK
- tip140 APK

| Asset | Soft-PASS md5 |
|-------|---------------|
| vehicle_cz6a_stack | `17ad81c62235dd204a535e6f2519e50e` |
| vehicle_cz6a_cutaway | `6dd047856ac7bfb7c538d350d77f8d41` |
| vehicle_cz6a_shell | `037d0d9310b1813b7cf0e806e12b44d5` |
| vehicle_sr75_stack | `4cb9b75bac4ea7b8c92166181460718d` |
| vehicle_sr75_cutaway | `957417a271334d47c873e85e852d6bd2` |
| vehicle_sr75_shell | `e606603e8b384e2dbaf948a20696a11f` |
| vehicle_nuri_stack | `9b055e356a44dd7a70a5ba74f8550693` |
| vehicle_nuri_cutaway | `36adc443daf67c643ed1f00b492cb497` |
| vehicle_nuri_shell | `bebeff2a52a31921d421b6c525107419` |
| vehicle_h3_stack | `0716e244a5ca17668ed908ca4c640673` |
| vehicle_h3_cutaway | `de1c01cbdef94ff56ac9c950d45c243a` |
| vehicle_h3_shell | `38b6a173c2fbd6f8c69d653c95abe36f` |

**Soft-FAIL Ada heed (non-FAIL for staged wiring):** `/workspace/lrt-*-pack-20260923/` and `/workspace/hud-assets/` Soft-FAIL DRIFT on **stack+cutaway** for all four (shells Soft-PASS). READY.md Soft-PASS md5s Soft-PASS still list the Soft-PASS GOLD above; pack disk Soft-FAIL diverged. Soft-FAIL Ada re-copy from pack/hud-assets into drawable before Studio — Soft-PASS push from RetroCommandCenter @ `cead451` / tip140 APK Soft-PASS GOLD only.

### 5) No Play publish path — Soft-FAIL Play forever (Studio-only) — **PASS**

- HARD Soft-PASS: `/workspace/agent-backups/Helios/HARD-no-agent-play-publish.md` (+ team mirror) Soft-FAIL agent Play publish forever; Chris alone publishes.
- Ada Soft-PASS ACK Studio-only: `/workspace/agent-backups/Helios/2026-09-23-ada-studio-only-standing-receipt.json`
- Chris STOP Soft-PASS: `/workspace/agent-backups/Helios/2026-09-23-ada-tip139-PLAY-STAGE-ONLY-STOP.txt`
- Soft-FAIL invent: no agent Play publish automation Soft-PASS found in RetroCommandCenter gradle/scripts (no triplet-play / promote-to-production Soft-PASS path Soft-PASS staged). Docs Soft-PASS Soft-FAIL Play only.
- This verify Soft-FAIL Studio push Soft-PASS (HOLD until dual Soft-PASS + Chris go). Soft-FAIL ship.

---

## Soft-FAIL blockers Ada must fix / heed before Studio

1. **Soft-FAIL Play forever** — Soft-PASS Studio-only after wire Soft-PASS + dual Soft-PASS + Chris go. Soft-FAIL production / rollout / promote forever.
2. **Soft-FAIL re-import drifted packs** — Soft-PASS Studio from `/workspace/RetroCommandCenter` @ `cead451` (or tip140 Soft-PASS APK art Soft-PASS already baked). Soft-FAIL overwrite `vehicle_*_{stack,cutaway}.png` from current `/workspace/lrt-*-pack-20260923/` or `/workspace/hud-assets/` (stack+cutaway Soft-FAIL drift vs Soft-PASS GOLD Soft-PASS above).
3. **Soft-FAIL signed AAB Soft-PASS still Chris-side** (prior receipt Soft-PASS): no agent signingConfigs Soft-PASS; Soft-PASS Chris Generate Signed Bundle Soft-PASS path A Soft-PASS. Soft-FAIL invent passwords.
4. **Soft-FAIL device / adb** until Chris explicit go (HARD_DEVICE_PUSH Soft-PASS Studio-first).

No Soft-FAIL programming break found on tip134 / tip136 / tip139 / tip140 staged wiring for this GATE 1.

## Bottom line

**Overall PASS** — tip134 + tip136 present, wired, no regression in tip140 HEAD; tip139 ALL FOUR retained; tip140 staged Soft-PASS; VehicleCatalog + GOLD Soft-PASS in source + tip139/140 APKs; Soft-FAIL Play forever / Studio-only standing Soft-PASS. Soft-FAIL invent. Soft-FAIL Studio push / Play publish by this agent.
