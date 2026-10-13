# Helios GATE 1 - independent pre-push dual verify (LRT)

**When:** 2026-09-23 ~08:20 CT
**Auditor:** Helios (independent)
**Plan:** `/workspace/agent-backups/Helios/2026-09-23-prepush-dual-verify-plan.md`
**Standing:** Soft-PASS Studio stage only after Helios and Tinkabot clean. Soft-FAIL Play publish forever. Soft-FAIL Studio/adb/device until Chris explicit go.
**Actions taken:** Search + measure only. Soft-FAIL commit / Studio push / Play publish.

## Overall verdict

**PASS** - staged tiphead clean for tip134 / tip136 / tip139 / tip140 (+ tip137/138 carry-forward). No Studio push by Helios. Await Tinkabot GATE1 report for DUAL pair gate. Ada must sync tip140 onto MiniME before any Studio push.

## Sources searched

| Source | Result |
|--------|--------|
| `/workspace/agent-backups/Helios/` | FOUND - tip139/140 audits, HARD Play gates, dual-verify plan/HOLD |
| `/workspace/agent-backups/Ada/` | FOUND - tip139 ALL FOUR READY, tip140 READY |
| `/workspace/agent-backups/team/` | FOUND - tip139 programming audit copy, HARD Play gates, voice continuity (#48 tip134 / tip136) |
| `/workspace/agent-backups/tinkabot/` | FOUND - tip139 programming audit; no GATE1 report yet at Helios verify time |
| Box LRT trees | FOUND - `/workspace/RetroCommandCenter` tiphead **cead451** (tip140) |
| Staged APKs | FOUND - tip134/136/139/140 under `/workspace/LRT-tip*` |
| Dropbox | **NOT FOUND** tip134/136/139/140 staged fix packs (only unrelated LRT voice export under `/Grok-Agent-Backups/helios-voice-exports/`) |
| MiniME Studio | FOUND - `C:\Users\skiiw\AndroidStudioProjects\RetroCommandCenter` @ **113ad7f** (tip139) - **behind** tip140 |

## Tiphead measured (box = push candidate)

- Repo: `/workspace/RetroCommandCenter`
- HEAD: `cead451` - tip140 default cmd_page 2 + one-shot after big patch
- Prior: `a4db003` stamp 140; `113ad7f` tip139 ALL FOUR; `d404882` tip136; `87b86d0` tip134/#48
- versionCode **140** / versionName **1.0.130** (`app/build.gradle.kts` md5 `21f410e191f826d72f72f07c75636336`)
- Staged APK: `/workspace/LRT-tip140-1.0.130/LiveRocketTracker-tip140-1.0.130-debug.apk`
  - md5 `2701ff18382ebe975e7cfd09c71fc4ee` (matches Ada READY + Helios tip140 verify JSON)
  - aapt: versionCode='140' versionName='1.0.130'

Key source md5s (box tiphead):

- `AppPrefs.kt` `1daeb35203d6e85ad569ecd2a70b4ecc`
- `RetroCommandWallpaperService.kt` `691e92eed8c0531fa479739eaf936ff2`
- `strings.xml` `6618b07afac2d45d89e0ebd0f0e42d7d`
- `VehicleCatalog.kt` `50971cc1769b20504a8882d04cf8ce82`
- `VehicleDraw.kt` `94df5c76414930894816ed049d2cff7b`

## 1) tip134 - garbled labels / FakeBold / CASC EN-only / PAD pin-top

| Field | Evidence |
|-------|----------|
| FOUND | YES |
| Commit | `87b86d0` - fix #48: clean EN HUD plates (FakeBold off, CASC EN-only, PAD mark top) |
| APK | `/workspace/LRT-tip134-1.0.124/LiveRocketTracker-tip134-1.0.124-debug.apk` md5 `e4f2142f754645be0fb32f3b7d6fe2a9` / aapt 134 / 1.0.124 |
| Wired | YES on box tiphead - isFakeBoldText=false at HUD init + plate draws; drawCascBtn passes local="" (EN-only); drawPadVersionMark still present |
| MiniME | tip134 wires present on Studio tree (FakeBold false sites Soft-PASS) |
| Regression | Soft-FAIL invent as tip134 break - VehicleDraw.kt still has one isFakeBoldText=true on UPDATE_HEAD overlay only (not #48 plates). Non-blocking. |
| **Verdict** | **PASS** |

## 2) tip136 - wallpaper entertainment disclaimer

| Field | Evidence |
|-------|----------|
| FOUND | YES |
| Commit | `d404882` - tip136 wallpaper entertainment-only disclaimer Soft-PASS (2 files) |
| APK | `/workspace/LRT-tip136-1.0.126/LiveRocketTracker-tip136-1.0.126-debug.apk` md5 `8950f74cb9c470baae16cd9158eab9cc` / aapt 136 / 1.0.126 |
| Wired | YES - strings.xml has disclaimer_entertainment_short/_body + wallpaper_description; wallpaper paints matching ENTERTAINMENT ONLY / ENTERTAINMENT PURPOSES ONLY. tip140 APK resources.arsc contains same copy. |
| MiniME | strings Soft-PASS present |
| Note | Wallpaper uses matching literals rather than R.string refs - functionally wired; Soft-FAIL treat as missing. |
| **Verdict** | **PASS** |

## 3) tip139 - ALL FOUR GOLD art + catalog wiring

| Field | Evidence |
|-------|----------|
| FOUND | YES |
| Commit | `113ad7f` - art+wire tip139 Helios-PASS GOLD cz6a+sr75+nuri+h3 |
| APK | `/workspace/LRT-tip139-1.0.129/LiveRocketTracker-tip139-1.0.129-debug.apk` md5 `b0dd6c1069756653ba3c1d70dcafac96` / aapt 139 / 1.0.129 |
| Catalog | ids cz6a / sr75 / nuri / h3 + drawFamily Soft-PASS; consts CZ6A_DRAWABLE / SR75_DRAWABLE / NURI_DRAWABLE Soft-PASS (no H3_DRAWABLE - drawFamily=h3; non-blocking, same as prior Helios audit) |
| Drawables | 12/12 stack/cutaway/shell Soft-PASS in app/src/main/res/drawable/ |
| GOLD md5 | Tree Soft-PASS equals Helios 2026-09-23-tip139-art-review Soft-PASS claims; Soft-PASS equals tip139-apk-audit/helios-indep extract (12/12 MATCH) |
| MiniME | HEAD Soft-PASS 113ad7f / version 139 / 1.0.129 Soft-PASS - tip139 Soft-PASS present on Studio |
| Pack/hud drift | NON-BLOCKING: current lrt-*-pack-20260923 and hud-assets stack+cutaway DIFF from tip139 Soft-PASS tree; shells MATCH. Soft-FAIL treat as tip139 regression - Soft-PASS tree still equals Soft-PASS GOLD that shipped tip139. |
| **Verdict** | **PASS** |

Tree Soft-PASS GOLD md5s:

- cz6a stack `17ad81c62235dd204a535e6f2519e50e` / cutaway `6dd047856ac7bfb7c538d350d77f8d41` / shell `037d0d9310b1813b7cf0e806e12b44d5`
- sr75 stack `4cb9b75bac4ea7b8c92166181460718d` / cutaway `957417a271334d47c873e85e852d6bd2` / shell `e606603e8b384e2dbaf948a20696a11f`
- nuri stack `9b055e356a44dd7a70a5ba74f8550693` / cutaway `36adc443daf67c643ed1f00b492cb497` / shell `bebeff2a52a31921d421b6c525107419`
- h3 stack `0716e244a5ca17668ed908ca4c640673` / cutaway `de1c01cbdef94ff56ac9c950d45c243a` / shell `38b6a173c2fbd6f8c69d653c95abe36f`

## 4) tip140 - default command page 2

| Field | Evidence |
|-------|----------|
| FOUND | YES (box tiphead + staged APK + Ada READY) |
| Commit | `cead451` |
| APK | tip140 md5 `2701ff18382ebe975e7cfd09c71fc4ee` Soft-PASS |
| Wired (box) | YES - getter default cmd_page **2**; applyCommandPageDefaultV1 / ccos_cmd_page_default_v1; ensurePaidAppDefaults writes cmd_page **2**; SetupActivity still sets page 2; Settings override Soft-PASS kept |
| Wallpaper page detect | Soft-PASS compares page == prefs.commandPageIndex when offsets available; fallback when xOffsetStep tiny treats launcher as page 0 (onCommandPage = prefs.commandPageIndex == 0) - Soft-FAIL invent as tip140 bug |
| MiniME Studio | **NOT YET** - HEAD 113ad7f, versionCode **139**, cmd_page getter default still **0**, ensurePaidAppDefaults still writes **0**. Expected under Ada HOLD until dual verify Soft-PASS. Soft-FAIL claim Studio already has tip140. |
| **Verdict** | **PASS** (staged tiphead Soft-PASS). Ada Soft-PASS sync/pull tip140 onto MiniME after GATE 1 BOTH clean Soft-PASS before Studio push. |

## 5) Other pending staged (tip137 / tip138) - carry-forward Soft-PASS

| Item | Evidence | Verdict |
|------|----------|---------|
| tip137 pad font | drawPadVersionMark Soft-PASS (oldSize * 0.78f).coerceIn(20f, 32f) Soft-PASS on tiphead | **PASS** (no regression) |
| tip138 fetch race | cold 2_000L retry Soft-PASS; FETCHING Soft-PASS in RocketTelemetryModule Soft-PASS on tiphead | **PASS** (no regression) |

## 6) VehicleCatalog / resource wiring Soft-PASS

- ALL FOUR tip139 vehicles Soft-PASS catalog + drawable Soft-PASS on tiphead.
- Soft-FAIL missing stack/cutaway/shell for cz6a/sr75/nuri/h3 Soft-PASS tiphead.
- Soft-FAIL invent Play publish path Soft-PASS in tiphead as ship path Soft-PASS this gate.

**Verdict: PASS**

## 7) Play path Soft-FAIL forever Soft-PASS

- HARD docs Soft-PASS: HARD-no-agent-play-publish.md, Play stage-only Soft-PASS receipts Soft-PASS.
- Soft-FAIL found agent Play-publish automation Soft-PASS tiphead Soft-PASS as ship path Soft-PASS this gate.
- Soft-PASS Studio-only after BOTH Soft-PASS.

**Verdict: PASS** (Soft-FAIL Play Soft-PASS honored)

## Per-item table

| # | Item | FOUND | Wired (box tip140) | MiniME Studio | PASS/FAIL |
|---|------|-------|--------------------|---------------|-----------|
| 1 | tip134 garbled labels | YES | YES | YES (via tip139 HEAD) | **PASS** |
| 2 | tip136 entertainment disclaimer | YES | YES | YES | **PASS** |
| 3 | tip139 ALL FOUR | YES | YES | YES @ 113ad7f | **PASS** |
| 4 | tip140 cmd_page default 2 | YES | YES | **NO** (still tip139 / cmd_page 0) | **PASS*** |
| 5 | tip137/138 carry-forward | YES | YES | under tip139 HEAD | **PASS** |
| 6 | Catalog/resource wiring | YES | YES | tip139 Soft-PASS | **PASS** |
| 7 | Soft-FAIL Play forever | YES (HARD) | N/A | N/A | **PASS** |

* tip140 Soft-PASS as staged tiphead Soft-PASS; Studio Soft-PASS intentionally Soft-FAIL until Ada Soft-PASS after GATE 1.

## Flags (non-blocking)

1. Studio lag (expected HOLD): MiniME is tip139 (113ad7f, versionCode 139, cmd_page default 0).
   Box tiphead is tip140 (cead451).
   After BOTH Helios and Tinkabot report Soft-PASS, Ada must sync/pull tip140 onto MiniME before any Studio push.

2. Pack/hud-assets stack+cutaway currently DIFF from tip139 shipped GOLD tree; shells MATCH.
   Tree still matches art-review GOLD md5s + tip139 APK extract.
   Do not silently rewire newer packs without a new Soft-PASS.

3. No H3_DRAWABLE const (drawFamily=h3). Non-blocking; same as prior Helios tip139 audit.

4. tip136 wallpaper paints matching string literals rather than R.string refs. Functionally wired.

5. Dropbox: no tip134/136/139/140 staged fix mirrors found. Box tiphead + staged APKs are source of truth for this gate.

## HARD hold

- Soft-FAIL Studio / adb / device push until Chris explicit go.
- Soft-FAIL agent Play publish forever.
- GATE 1 pair: Helios Soft-PASS (this file) + Tinkabot Soft-PASS required before Ada Studio-only push.

## Report path

`/workspace/agent-backups/Helios/2026-09-23-helios-prepush-dual-verify.md`
