# FLEET_AUDIT — plume anchor (α>8 bbox)

Generated 2026-09-23 CT from `/workspace/hud-assets/` (post-cz8a mirror).
gap = flame_top_y − engine_bottom_y. Soft-FAIL if gap > 0 (floating). PASS if gap ≤ 0 (overlap).
has_starship_bar_engine = hud engine_ship md5 matches nozzle-catalog-20260923 when present.

| id | eng_bottom | flame_top | gap | verdict | has_starship_bar_engine | flame_file | notes |
|----|------------|-----------|-----|---------|--------------------------|------------|-------|
| ariane | None | 1112 | None | **FLAME_NO_ENGINE** | False | `vehicle_ariane_flame_ship.png` | engine_ship missing |
| atlas | None | 1112 | None | **FLAME_NO_ENGINE** | False | `vehicle_atlas_flame_ship.png` | engine_ship missing |
| cz12 | 467 | 471 | 4 | **SOFT_FAIL** | True | `vehicle_cz12_flame_ship.png` |  |
| cz2d | 383 | 387 | 4 | **SOFT_FAIL** | True | `vehicle_cz2d_flame_ship.png` |  |
| cz6a | 469 | 451 | -18 | **PASS** | True | `vehicle_cz6a_flame_ship.png` |  |
| cz8a | 546 | 540 | -6 | **PASS** | True | `vehicle_cz8a_flame_ship.png` |  |
| electron | 422 | 426 | 4 | **SOFT_FAIL** | True | `vehicle_electron_flame_ship.png` |  |
| f9 | None | 402 | None | **FLAME_NO_ENGINE** | False | `vehicle_f9_flame_ship.png` | engine_ship missing |
| fh | 481 | 485 | 4 | **SOFT_FAIL** | True | `vehicle_fh_flame_ship.png` |  |
| firefly | None | 1117 | None | **FLAME_NO_ENGINE** | False | `vehicle_firefly_flame_ship.png` | engine_ship missing |
| glenn | None | 1107 | None | **FLAME_NO_ENGINE** | False | `vehicle_glenn_flame_ship.png` | engine_ship missing |
| gravity1 | 686 | 700 | 14 | **SOFT_FAIL** | True | `vehicle_gravity1_flame_ship.png` |  |
| gslv | None | 1112 | None | **FLAME_NO_ENGINE** | False | `vehicle_gslv_flame_ship.png` | engine_ship missing |
| h3 | 526 | 508 | -18 | **PASS** | True | `vehicle_h3_flame_ship.png` |  |
| isro | None | 1112 | None | **FLAME_NO_ENGINE** | False | `vehicle_isro_flame_ship.png` | engine_ship missing |
| kinetica1 | 626 | 629 | 3 | **SOFT_FAIL** | True | `vehicle_kinetica1_flame_ship.png` |  |
| kz11 | 286 | 290 | 4 | **SOFT_FAIL** | True | `vehicle_kz11_flame_ship.png` |  |
| lm5 | 490 | 476 | -14 | **PASS** | True | `vehicle_lm5_flame_ship.png` |  |
| lvm3 | None | 1112 | None | **FLAME_NO_ENGINE** | False | `vehicle_lvm3_flame_ship.png` | engine_ship missing |
| nuri | 575 | 570 | -5 | **PASS** | True | `vehicle_nuri_flame_ship.png` |  |
| pallas1 | 504 | 508 | 4 | **SOFT_FAIL** | True | `vehicle_pallas1_flame_ship.png` |  |
| proton | None | 1112 | None | **FLAME_NO_ENGINE** | False | `vehicle_proton_flame_ship.png` | engine_ship missing |
| pslv | None | 1117 | None | **FLAME_NO_ENGINE** | False | `vehicle_pslv_flame_ship.png` | engine_ship missing |
| sls | None | 1100 | None | **FLAME_NO_ENGINE** | False | `vehicle_sls_flame_ship.png` | engine_ship missing |
| soyuz | None | 1102 | None | **FLAME_NO_ENGINE** | False | `vehicle_soyuz_flame_ship.png` | engine_ship missing |
| sr75 | None | None | None | **EMPTY** | True | `vehicle_sr75_flame_ship.png` | opaque_px eng=0 flame=0 |
| starship | 1191 | 1084 | -107 | **PASS** | False | `vehicle_starship_flame_ship.png` |  |
| vulcan | None | 1107 | None | **FLAME_NO_ENGINE** | False | `vehicle_vulcan_flame_ship.png` | engine_ship missing |
| zq3 | 475 | 455 | -20 | **PASS** | True | `vehicle_zq3_flame_ship.png` |  |

## Soft-FAIL floating (gap>0): count=8
cz12, cz2d, electron, fh, gravity1, kinetica1, kz11, pallas1

## Soft-FAIL EMPTY / FLAME_NO_ENGINE
- **ariane**: FLAME_NO_ENGINE (eng_px=0, flame_px=2095)
- **atlas**: FLAME_NO_ENGINE (eng_px=0, flame_px=2095)
- **f9**: FLAME_NO_ENGINE (eng_px=0, flame_px=7976)
- **firefly**: FLAME_NO_ENGINE (eng_px=0, flame_px=1944)
- **glenn**: FLAME_NO_ENGINE (eng_px=0, flame_px=2248)
- **gslv**: FLAME_NO_ENGINE (eng_px=0, flame_px=2095)
- **isro**: FLAME_NO_ENGINE (eng_px=0, flame_px=2095)
- **lvm3**: FLAME_NO_ENGINE (eng_px=0, flame_px=2095)
- **proton**: FLAME_NO_ENGINE (eng_px=0, flame_px=2095)
- **pslv**: FLAME_NO_ENGINE (eng_px=0, flame_px=1944)
- **sls**: FLAME_NO_ENGINE (eng_px=0, flame_px=2777)
- **soyuz**: FLAME_NO_ENGINE (eng_px=0, flame_px=2395)
- **sr75**: EMPTY (eng_px=0, flame_px=0)
- **vulcan**: FLAME_NO_ENGINE (eng_px=0, flame_px=2248)

## Next-wave Soft-FAIL ids
cz12, cz2d, electron, fh, gravity1, kinetica1, kz11, pallas1, ariane, atlas, f9, firefly, glenn, gslv, isro, lvm3, proton, pslv, sls, soyuz, sr75, vulcan

## Notes
- cz8a rebuilt this run → PASS gap=−6 (flame md5 `2455d7b3a2ccca1096130921c1eef253`).
- Helios queue owns rebuild order; this audit Soft-FAIL invent full-fleet fix.
- Soft-FAIL stack/cutaway/tank overwrite on all fixes.

## Post-fix update (same run — Helios queue)
After Soft-PASS flame rebuild + mirror, remeasure α>8 gap:
| id | gap | verdict |
|----|-----|---------|
| cz8a | −6 | PASS |
| cz12 | −6 | PASS (was +4 Soft-FAIL) |
| cz6a | −6 | PASS |
| electron | −6 | PASS (was +4 Soft-FAIL) |
| h3 | −6 | PASS |
| kinetica1 | −6 | PASS (was +3 Soft-FAIL) |
| nuri | −6 | PASS |

Remaining Soft-FAIL float (not in this Helios queue): cz2d, fh, gravity1, kz11, pallas1.
HOLD: sr75 EMPTY invent, f9 FLAME_NO_ENGINE invent.
