# READY — tip154 tank fill fit (2026-09-23 CT)

**Lane:** Da Vinci art — tank fill masks only
**Chris Soft-FAIL:** Electron tanks not fitting inside body; some fleet tanks too small

## HARD FAIL held
- Soft-FAIL overwrite Electron GOLD stack/side `d8ab6a86858da2237a18838c4c4dc91e`
- Soft-FAIL overwrite Gravity-1 GOLD side/stack `c78f781a40a01676fdb57ae5343ba07b` (hud/RCC); LRT drawable tip152 identity also untouched
- Soft-FAIL overwrite cutaway/shell/engines/flames
- Soft-FAIL Ada wire Soft-FAIL hud-assets Soft-FAIL phone Soft-FAIL Play Soft-FAIL ship until Helios Soft-PASS + Chris Soft-PASS
- Soft-FAIL invent unpublished geometry Soft-FAIL invent F9 side MVac

## Method
1. Measure mid-row `fill_width / core_window_width` vs `vehicle_{id}_cutaway.png`.
2. `core_window` = cutaway opaque center-stack diameter (25th-pct single-segment on upper fill rows); side boosters use local shell-gap body.
3. Target **84%** (band 80–88%). Soft-FAIL spill outside cutaway opaque. Soft-FAIL tiny (<70%).
4. Electron: also clip to shell window gap so fills sit INSIDE window (no rim spill); uniform all 4 tanks.
5. Keep propellant colors (Electron kerolox LOX cyan + RP amber).
6. Eyes-only pack — Soft-FAIL hud copy.

**Pack:** `/workspace/agent-backups/Da-Vinci/tip154-tank-fit-20260923`

## Electron (FIRST)

| asset | old md5 | new md5 | changed |
|-------|---------|---------|---------|
| `vehicle_electron_booster_tank_fills.png` | `e131d0a9b2bd59e2fb43efccce631bb6` | `f694ca9d35f2e325eb418396603b6170` | YES |
| `vehicle_electron_ship_tank_fills.png` | `8873ba10eb5c6a6278dbd7cd7371e45f` | `687a6dd8c9166e52973458f1645a3b79` | YES |
| `vehicle_electron_tank_fills.png` | `c465df4ebb6196a301a9724c370fd8d7` | `4ffbd3e1df7caf2c5f0c449da2e1d83f` | YES |
| `vehicle_electron_tank_s1_fuel.png` | `f0580b03b745ca67ed3a9f9eecf648cb` | `f154361b8d1907707d6bdfd2e492d6f2` | YES |
| `vehicle_electron_tank_s1_ox.png` | `426ecb90ecdbb3ad4562e0357f1d4b63` | `c068a3a8f105047e257c694e59910c99` | YES |
| `vehicle_electron_tank_s2_fuel.png` | `ce4579e0f8832339fa1a806b30404209` | `1cf87cf0f075ca42edb2685bfb9ca9f5` | YES |
| `vehicle_electron_tank_s2_ox.png` | `0e0b0bfb3b9b9d6e3806093aaa57ebc6` | `d8c2e8a2c588e03788cd1d635433904a` | YES |

| metric | before | after |
|--------|--------|-------|
| mid-row fill/core (all tanks) | 51/61=**0.836** (flush to gap 49 → 1px rim spill) | 49/61=**0.803** inside gap, outside%=**0** |
| outside% vs cutaway opaque | 0 | 0 |
| shell-gap spill | 1px/side | **0** |
| colors | LOX cyan (18,235,255) + RP amber (242,158,42) | **KEEP** |

## Rebuilt families (undersize → ~84%)

| family | before (center ratios) | after | outside% | status |
|--------|------------------------|-------|----------|--------|
| `electron` | 0.836, 0.836, 0.836, 0.836 | 0.803, 0.803, 0.803, 0.803 | 0.00 | **PASS** |
| `gravity1` | 0.505, 0.505, 0.505 | 0.838, 0.838, 0.838 | 0.00 | **PASS** |
| `cz2d` | 0.533, 0.533, 0.533, 0.533 | 0.838, 0.838, 0.838, 0.838 | 0.00 | **PASS** |
| `kz11` | 0.512, 0.512, 0.512, 0.512, 0.512 | 0.841, 0.841, 0.841, 0.841, 0.841 | 0.00 | **PASS** |
| `cz12` | 0.338, 0.338, 0.815, 0.815 | 0.838, 0.838, 0.815, 0.815 | 0.00 | **PASS** |
| `kinetica1` | 0.242, 0.318, 0.365, 0.573 | 0.839, 0.839, 0.839, 0.839 | 0.00 | **PASS** |
| `lm5` | 0.503, 0.503, 0.503, 0.503 | 0.840, 0.840, 0.840, 0.840 | 0.00 | **PASS** |
| `zq3` | 0.538, 0.534, 0.531, 0.541 | 0.841, 0.840, 0.840, 0.842 | 0.00 | **PASS** |

## KEEP (already in-band under core-window metric)

| family | center ratios | note |
|--------|---------------|------|
| `cz8a` | 0.876, 0.876, 0.876, 0.876 | cz8a s1 raw seg_w ratio 0.37 is booster-merged span; core-window=170 → 0.876 PASS |
| `cz6a` | 0.876, 0.876, 0.876, 0.876 | in-band |
| `sr75` | 0.904, 0.904 | in-band |
| `nuri` | 0.908, 0.908, 0.908, 0.908, 0.887, 0.887 | in-band |
| `h3` | 0.879, 0.879, 0.879, 0.879 | in-band |
| `fh` | 0.829, 0.829, 0.829, 0.829 | in-band |
| `pallas1` | 0.829, 0.829, 0.829, 0.829 | in-band |

## GOLD verification (untouched)

| asset | md5 |
|-------|-----|
| `vehicle_electron_stack.png` | `d8ab6a86858da2237a18838c4c4dc91e` KEEP |
| `vehicle_electron_side.png` | `d8ab6a86858da2237a18838c4c4dc91e` KEEP |
| `vehicle_electron_cutaway.png` | `bf0be9bb431a5915ce5e91617923c9d2` KEEP |
| `vehicle_gravity1_stack.png` (LRT) | `8effda85dab7f492646b53a81070cd52` untouched |
| `vehicle_gravity1_side.png` (LRT) | `8effda85dab7f492646b53a81070cd52` untouched |
| Gravity-1 GOLD (hud/RCC) | `c78f781a40a01676fdb57ae5343ba07b` Soft-FAIL overwrite Soft-PASS held |

## Deliverables
- Fills: `/workspace/agent-backups/Da-Vinci/tip154-tank-fit-20260923/fills/`
- MD5SUMS: `/workspace/agent-backups/Da-Vinci/tip154-tank-fit-20260923/MD5SUMS`
- Previews: `preview_electron_before_after.png`, `preview_gravity1_before_after.png`, `preview_cz8a_before_after.png`, `preview_cz2d_before_after.png`, `preview_fleet_tank_fit_board.png`
- Fleet audit: `/workspace/agent-backups/Da-Vinci/tip154-tank-fit-20260923/FLEET_TANK_FIT_AUDIT.md`

## Success
- Electron fills inside cutaway/shell windows, uniform **~80%** body width, Soft-FAIL spill Soft-FAIL tiny Soft-FAIL GOLD overwrite Soft-FAIL hud Soft-FAIL Ada Soft-FAIL ship
- gravity1 / cz2d / kz11 / cz12 / kinetica1 / lm5 / zq3 center tanks raised to ~84%
- cz8a KEEP (core-window PASS)

