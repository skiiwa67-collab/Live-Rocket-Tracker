# tip139 programming audit — tinkabot Soft-PASS PASS

**When:** 2026-09-23 ~07:30 CT  
**Auditor:** tinkabot  
**Subject:** Ada tip139 / 1.0.129 wiring (BUILD ONLY — Soft-FAIL device/Studio)  
**Verdict: PASS**

## Evidence surfaces
| Surface | Soft-PASS |
|---------|-----------|
| Staged APK | `/workspace/LRT-tip139-1.0.129/LiveRocketTracker-tip139-1.0.129-debug.apk` |
| APK md5 | `b0dd6c1069756653ba3c1d70dcafac96` (matches Ada READY) |
| Tiphead | `113ad7f` on `stamp-85` — `art+wire: tip139 Helios-PASS GOLD cz6a+sr75+nuri+h3` |
| Tools | APK binary audit (drawable md5 + dex/manifest strings) + `gh` raw tiphead sources. pstack plugin present on box; no `pstack` CLI — used equivalent Soft-PASS prove-it surfaces. Soft-FAIL invent. Soft-FAIL interrupt Chris. Soft-FAIL device/Studio. |

## Checklist

### 1) VehicleCatalog + VehicleDraw Soft-PASS
- **PASS** tiphead `VehicleCatalog.kt`: `id/drawFamily` Soft-PASS for `cz6a`, `sr75`, `nuri`, `h3`; consts `CZ6A_DRAWABLE` / `SR75_DRAWABLE` / `NURI_DRAWABLE`.
- **PASS** tiphead `VehicleDraw.kt`: when-branches Soft-PASS `cz6a` / `sr75` / `nuri` / `h3` (art-then-tanks / `cz6a()` / `h3()`).
- Note (non-blocking): no `H3_DRAWABLE` const string Soft-PASS — draw uses `drawFamily="h3"` + `vehicle_h3_*` Soft-PASS.

### 2) Drawable assets Soft-PASS (no blank HUD for tip139 fours)
- **PASS** APK contains stack/cutaway/shell for all four; counts: cz6a 30, sr75 29, nuri 32, h3 31 vehicle_* drawables.
- **PASS** md5 MATCH Helios GOLD packs (`lrt-*-pack-20260923`):

| Asset | md5 |
|-------|-----|
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

### 3) tip137 pad font Soft-PASS in tip139
- **PASS** `drawPadVersionMark`: `(oldSize * 0.78f).coerceIn(20f, 32f)` Soft-PASS on tiphead wallpaper (matches `docs/tips/tip137-pad-version-size.md`).

### 4) tip138 fetch / cold-retry Soft-PASS in tip139
- **PASS** `LaunchDataProvider.refreshIfNeeded`: tip138 queue-while-fetching Soft-PASS.
- **PASS** wallpaper drawFrame: cold `isFetchingData()` re-kick every 2s Soft-PASS (`docs/tips/tip138-fetch-queue-cold-retry.md`).
- Soft-FAIL blank HUD from dropped refresh: mitigated Soft-PASS in source (runtime device Soft-FAIL until Chris go).

### 5) Version Soft-PASS
- **PASS** tiphead `app/build.gradle.kts`: `versionCode = 139`, `versionName = "1.0.129"`.
- **PASS** APK manifest string pool Soft-PASS `versionName=1.0.129`; dex Soft-PASS `1.0.129`.

### 6) Incomplete integrations
- **PASS** for tip139 scope (all four wired + art Soft-PASS + tip137/138 retained).
- Residual non-blocking: `H3_DRAWABLE` const absent (draw path Soft-PASS anyway).

## Soft-FAIL standing
- Soft-FAIL phone / Studio / adb push until Helios Soft-PASS + Chris explicit go.
- Soft-FAIL interrupt Chris during CZ-8A window.

## Bottom line
**PASS** — tip139 programming Soft-PASS for Helios independent audit / Chris WHEN-to-go gate.

## Ada SOURCE Soft-PASS reconfirm (2026-09-23 ~07:30 CT)
- Tree: `/workspace/RetroCommandCenter` @ `113ad7f6a2bcf3f13fab6ee52093745b817b48a7`
- Catalog line Soft-PASS: cz6a:440 sr75:723 nuri:759 h3:798
- APK md5 reconfirm Soft-PASS `b0dd6c1069756653ba3c1d70dcafac96`
- Verdict unchanged: **PASS**
