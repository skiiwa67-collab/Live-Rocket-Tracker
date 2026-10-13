# KINETICA1 plume-anchor Soft-PASS — READY

**Family:** kinetica1
**When:** 2026-09-23 CT
**Lane:** Da Vinci art only. Soft-FAIL Play/device/Ada code.

## Soft-PASS writes
| file | md5 | note |
|------|-----|------|
| `vehicle_kinetica1_flame_ship.png` | `91eb3443802512f846b6a3fa2cd964a1` | NEW — opaque top seals into engine exit |
| `vehicle_kinetica1_flame_s2.png` | `91eb3443802512f846b6a3fa2cd964a1` | identical alias |
| `vehicle_kinetica1_exhaust_ship.png` | `91eb3443802512f846b6a3fa2cd964a1` | identical alias |
| `vehicle_kinetica1_engine_ship.png` | `cfca95fce2f0df7a8279340f044797b5` | KEEP Starship-bar (unchanged) |

## Geometry
- Canvas: 424×1200 transparent
- engine_ship bbox: (184, 303, 239, 626) exit y1=626
- NEW flame bbox: (203, 620, 219, 664)
- **gap = -6** (seal 6px into bell)
- bell centers: [211.5]
- palette: methalox
- Soft-FAIL bake plume into engine_*

## Eyes
- `compare_kinetica1_postsep.png`
- `_proof_kinetica1_postsep.png`

## Mirrored to
- `/workspace/hud-assets`
- `/workspace/lrt-kinetica1-pack`

## Soft-FAIL body proof (unchanged)
- stack: `0c919bed7c069b980a6fdc1462a78d10`
- ship: `24eced24ce5347dd5b541fc38e5af6f1`
- cutaway: `0c919bed7c069b980a6fdc1462a78d10`

## Soft-FAIL
- Soft-FAIL overwrite stack / cutaway / shell / side / tank masks
- Soft-FAIL Play / device / invent unpublished geometry


## Work dir
`/workspace/agent-backups/Da-Vinci/plume-anchor-20260923/kinetica1`
