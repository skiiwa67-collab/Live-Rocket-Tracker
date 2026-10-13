# LRT plume-anchor Soft-PASS audit — post-sep upper-stage detach

**Where we are:** 2026-09-23 08:43 CDT
**Owner audit:** Helios executor (Grok) for Chris Olsen / Live Rocket Tracker
**Tiphead:** `/workspace/RetroCommandCenter` drawables under `app/src/main/res/drawable/`
**Trigger:** Chris live CZ-8A post-sep screenshot — upper-stage flame floating away from nozzle (regression; said fixed 8–9 versions ago).

## Rules (this audit)
- Soft-PASS stage Studio only
- Soft-FAIL Play publish / invent / device push / claim Studio already has fixes until Ada confirms
- Soft-FAIL overwrite stack/cutaway/tank (HARD `/workspace/agent-backups/Helios/HARD-nozzle-only-no-stack-overwrite.md`)
- Da Vinci / Ada own art+wire fixes; this deliverable is AUDIT + dual-write + Soft-FAIL list for Da Vinci

## Method
1. Inventory every `vehicle_*` with `flame_ship` / `flame_s2` / `exhaust_ship` / `engine_ship` (and booster aliases).
2. PNG geometry: measure first opaque Y of flame vs `engine_ship` nozzle exit (bottom opaque of engine). Same 424×1200 canvas. Soft-PASS if `flame_top - eng_bottom <= 3px` (mated/overlap); Soft-FAIL if gap > 3px (floating).
3. Code: `VehicleDraw.kt` `drawAssembledParts` (post-sep) vs continuous `stageHullSrcRect` + `paintArtPathFlames(..., hullDest, hullSrc)` (Stamp 100).

## Root cause (CZ-8A + fleet assembled path)

**PNG for CZ-8A ship is Soft-PASS** (`engine_ship` bot=547, `flame_ship` top=532, delta=−15px overlap).

**Code Soft-FAIL post-sep:** For CZ-8A (and friends listed in `VehicleDraw.kt` ~L508), pre-sep forces continuous stack (`drawAssembledParts` returns false). **Post-sep** `drawAssembledParts` returns true and:
1. Blits `ship`/`engine_ship` with **full 424×1200 frame** (`src=null`) into slot — opaque content sits mid-frame, so visual nozzle is mid-slot, not at `baseY`.
2. Calls `paintArtPathFlames` **without** `hullDest`/`hullSrc` (`VehicleDraw.kt` ~L422–424).
3. Flame fallback anchors content bbox near `baseY` (`~L860–864`) → **flame at slot bottom while hull/nozzle float mid-slot** = Chris’s detach.

Continuous path (pre-sep / F9) Soft-PASS: Stamp 100 maps flame top through `hullSrc` (`~L829–853`).

## CZ-8A first (detail)

| asset | size | opaque top | opaque bot | md5-12 |
|-------|------|------------|------------|--------|
| vehicle_cz8a_engine_ship.png | 424×1200 | 509 | 547 | c81eb0249eae |
| vehicle_cz8a_flame_ship.png (= flame_s2 = exhaust_ship) | 424×1200 | 532 | 576 | 0e0a72f83972 |
| vehicle_cz8a_engine_booster.png | 424×1200 | 973 | 1021 | a74282fe432d |
| vehicle_cz8a_flame_booster.png (= flame_s1 = exhaust_booster) | 424×1200 | 1037 | 1097 | f1e85d206d66 |
| vehicle_cz8a_ship.png | 424×1200 | 40 | 547 | c6994772b12c |

**One-sentence CZ-8A:** Ship PNG is nozzle-mated (Soft-PASS), but post-sep assembled draw Soft-FAILs by full-frame blit + flame-without-hullDest, so the upper plume detaches/floats under the slot while the nozzle sits mid-frame.

## Fleet audit table

| vehicle | stage | assets | PNG_anchor | code_offset | notes |
|---------|-------|--------|------------|-------------|-------|
| cz8a | ship/s2 | `vehicle_cz8a_flame_ship.png, vehicle_cz8a_flame_s2.png, vehicle_cz8a_exhaust_ship.png, vehicle_cz8a_engine_ship.png` | Soft-PASS | Soft-FAIL | PNG mated delta=-15; post-sep assembled Soft-FAIL (Chris live bug) |
| cz8a | booster/s1 | `vehicle_cz8a_flame_booster.png, vehicle_cz8a_flame_s1.png, vehicle_cz8a_exhaust_booster.png, vehicle_cz8a_engine_booster.png` | Soft-FAIL | Soft-FAIL | PNG FLOAT gap +16px under engine_booster; plus assembled Soft-FAIL |
| cz12 | ship/s2 | `vehicle_cz12_flame_ship.png, vehicle_cz12_flame_s2.png, vehicle_cz12_exhaust_ship.png, vehicle_cz12_engine_ship.png` | Soft-PASS | Soft-FAIL | PNG mated delta=+3; code Soft-FAIL same assembled path |
| cz12 | booster/s1 | `vehicle_cz12_flame_booster.png, vehicle_cz12_flame_s1.png, vehicle_cz12_exhaust_booster.png, vehicle_cz12_engine_booster.png` | Soft-PASS | Soft-FAIL | PNG Soft-PASS delta=-14; code Soft-FAIL post-sep |
| cz6a | ship/s2 | `vehicle_cz6a_flame_ship.png, vehicle_cz6a_flame_s2.png, vehicle_cz6a_exhaust_ship.png, vehicle_cz6a_engine_ship.png` | Soft-PASS | Soft-FAIL | PNG Soft-PASS delta=-21; code Soft-FAIL |
| cz6a | booster/s1 | `vehicle_cz6a_flame_booster.png, vehicle_cz6a_flame_s1.png, vehicle_cz6a_exhaust_booster.png, vehicle_cz6a_engine_booster.png` | Soft-FAIL | Soft-FAIL | PNG FLOAT +16; code Soft-FAIL |
| electron | ship/s2 | `vehicle_electron_flame_ship.png, vehicle_electron_flame_s2.png, vehicle_electron_exhaust_ship.png, vehicle_electron_engine_ship.png` | Soft-PASS | Soft-FAIL | PNG Soft-PASS delta=-5; code Soft-FAIL |
| electron | booster/s1 | `vehicle_electron_flame_booster.png, vehicle_electron_flame_s1.png, vehicle_electron_exhaust_booster.png, vehicle_electron_engine_booster.png` | Soft-PASS | Soft-FAIL | PNG Soft-PASS delta=-3; code Soft-FAIL |
| f9 | ship/s2 | `vehicle_f9_flame_ship.png, vehicle_f9_flame_s2.png, vehicle_f9_exhaust_ship.png` | Soft-PASS | Soft-PASS | PNG Soft-PASS vs ship bot delta=+1 (no engine_ship); code Soft-PASS continuous |
| f9 | booster/s1 | `vehicle_f9_flame_booster.png, vehicle_f9_flame_s1.png, vehicle_f9_exhaust_booster.png, vehicle_f9_engine_octaweb.png` | Soft-PASS | Soft-PASS | PNG Soft-PASS vs booster bot delta=0; code Soft-PASS |
| h3 | ship/s2 | `vehicle_h3_flame_ship.png, vehicle_h3_flame_s2.png, vehicle_h3_exhaust_ship.png, vehicle_h3_engine_ship.png` | Soft-PASS | Soft-FAIL | PNG Soft-PASS delta=-21; code Soft-FAIL |
| h3 | booster/s1 | `vehicle_h3_flame_booster.png, vehicle_h3_flame_s1.png, vehicle_h3_exhaust_booster.png, vehicle_h3_engine_booster.png` | Soft-FAIL | Soft-FAIL | PNG FLOAT +18; code Soft-FAIL |
| kinetica1 | ship/s2 | `vehicle_kinetica1_flame_ship.png, vehicle_kinetica1_flame_s2.png, vehicle_kinetica1_exhaust_ship.png, vehicle_kinetica1_engine_ship.png` | Soft-PASS | Soft-FAIL | PNG Soft-PASS delta=-3; code Soft-FAIL |
| kinetica1 | booster/s1 | `vehicle_kinetica1_flame_booster.png, vehicle_kinetica1_flame_s1.png, vehicle_kinetica1_exhaust_booster.png, vehicle_kinetica1_engine_booster.png` | Soft-PASS | Soft-FAIL | PNG Soft-PASS delta=-11; code Soft-FAIL |
| nuri | ship/s2 | `vehicle_nuri_flame_ship.png, vehicle_nuri_flame_s2.png, vehicle_nuri_exhaust_ship.png, vehicle_nuri_engine_ship.png` | Soft-PASS | Soft-FAIL | PNG Soft-PASS delta=-9; code Soft-FAIL |
| nuri | booster/s1 | `vehicle_nuri_flame_booster.png, vehicle_nuri_flame_s1.png, vehicle_nuri_exhaust_booster.png, vehicle_nuri_engine_booster.png` | Soft-PASS | Soft-FAIL | PNG Soft-PASS delta=-23; code Soft-FAIL |
| sr75 | ship/s2 | `vehicle_sr75_flame_ship.png, vehicle_sr75_flame_s2.png, vehicle_sr75_exhaust_ship.png, vehicle_sr75_engine_ship.png` | Soft-FAIL | Soft-FAIL | PNG Soft-FAIL EMPTY flame_ship/s2/exhaust_ship; code Soft-FAIL; engine_ship also EMPTY |
| sr75 | booster/s1 | `vehicle_sr75_flame_booster.png, vehicle_sr75_flame_s1.png, vehicle_sr75_exhaust_booster.png, vehicle_sr75_engine_booster.png` | Soft-FAIL | Soft-FAIL | PNG FLOAT +43; code Soft-FAIL |
| starship | ship/s2 | `vehicle_starship_engine_ship.png, vehicle_starship_engine_ship.png, vehicle_starship_engine_raptors.png, vehicle_starship_engine_ring.png` | Soft-FAIL | UNKNOWN | PNG Soft-FAIL no flame_* pack (procedural only); engine_ship present |
| starship | booster/s1 | `vehicle_starship_engine_ship.png, vehicle_starship_engine_raptors.png, vehicle_starship_engine_ring.png` | Soft-FAIL | UNKNOWN | PNG Soft-FAIL no flame_booster |

## Counts

- Multi-stage families inventoried with flame/engine packs: **10** (cz8a, cz12, cz6a, electron, f9, h3, kinetica1, nuri, sr75, starship)
- Ship/s2 PNG: Soft-PASS **8** / Soft-FAIL **2**
- Ship/s2 code_offset: Soft-PASS **1** / Soft-FAIL **8** / UNKNOWN **1**
- Booster/s1 PNG: Soft-PASS **5** / Soft-FAIL **5**
- Booster/s1 code_offset Soft-FAIL (assembled post-sep): **8**

### Combined Soft-FAIL vs Soft-PASS (vehicle×stage cells, PNG ∪ code)
- Cells with any Soft-FAIL (PNG or code): **18**
- Cells Soft-PASS on both PNG and code: **2** (F9 ship + F9 booster only)

## Soft-FAIL list for Da Vinci (do NOT claim fixed)

1. **CZ-8A ship post-sep detach (HIGH)** — PNG OK; Ada/code must pass hullDest after assembled OR alpha-crop ship/engine on blit. Optional: tighten ship flame if Ada asks eyes-only.
2. **Fleet assembled post-sep same bug:** cz12, cz6a, electron, h3, kinetica1, nuri, sr75 (+ any other L508 artIds).
3. **PNG FLOAT gaps (booster):** cz8a (+16), cz6a (+16), h3 (+18), sr75 (+43) — re-anchor `flame_booster` top to `engine_booster` exit.
4. **SR-75 ship flames EMPTY** — fill `flame_ship`/`flame_s2`/`exhaust_ship` (and empty `engine_ship`).
5. **Starship** — no PNG flame pack (procedural only); out of scope unless GO’d.
6. **Targeted nozzle pass (separate GO):** ALL second/upper-stage nozzles → Starship side-profile de Laval (catalog `/workspace/agent-backups/Da-Vinci/nozzle-catalog-20260923/`). Soft-FAIL stack/cutaway/tank overwrite.

## Soft-FAIL claims
- Soft-FAIL claim art fixed
- Soft-FAIL claim Studio tip142 loadable
- Soft-FAIL Play / invent / device push
- Ada owns wire + Studio confirm; Da Vinci owns PNG nozzle/flame re-anchors

## Code pointers
- `app/src/main/java/com/ccos/retro/event/VehicleDraw.kt`
  - `drawArtWithInsideTankBurns` ~L421–424 (assembled → flames w/o hullDest)
  - `drawAssembledParts` ~L491–724 (full-frame blit; engShip at `uBase + eH*0.12`)
  - `paintArtPathFlames` ~L793–894 (Stamp 100 hullSrc map vs baseY fallback)
  - `stageHullSrcRect` ~L931–994 (continuous Soft-PASS path)

## Dual-write
- GitHub issue: https://github.com/skiiwa67-collab/Live-Rocket-Tracker/issues/64
- Notion Focus note: https://app.notion.com/p/3e4a94933faa8109a5f6ed5e537d3cf0
- Receipt: `/workspace/agent-backups/Helios/2026-09-23-plume-anchor-dual-write-receipt.json`

