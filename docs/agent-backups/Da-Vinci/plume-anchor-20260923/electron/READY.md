# ELECTRON plume-anchor Soft-PASS — READY

**Family:** electron
**When:** 2026-09-23 CT
**Lane:** Da Vinci art only. Soft-FAIL Play/device/Ada code.

## Soft-PASS writes
| file | md5 | note |
|------|-----|------|
| `vehicle_electron_flame_ship.png` | `035967caa63531320f4bb3ff601eea65` | NEW — opaque top seals into engine exit |
| `vehicle_electron_flame_s2.png` | `035967caa63531320f4bb3ff601eea65` | identical alias |
| `vehicle_electron_exhaust_ship.png` | `035967caa63531320f4bb3ff601eea65` | identical alias |
| `vehicle_electron_engine_ship.png` | `4020879c91ebdf03202f01d2bd729659` | KEEP Starship-bar (unchanged) |

## Geometry
- Canvas: 424×1200 transparent
- engine_ship bbox: (200, 370, 223, 422) exit y1=422
- NEW flame bbox: (204, 416, 218, 458)
- **gap = -6** (seal 6px into bell)
- bell centers: [211.5]
- palette: rp1
- Soft-FAIL bake plume into engine_*

## Eyes
- `compare_electron_postsep.png`
- `_proof_electron_postsep.png`

## Mirrored to
- `/workspace/hud-assets`
- `/workspace/lrt-electron-pack`

## Soft-FAIL body proof (unchanged)
- stack: `d8ab6a86858da2237a18838c4c4dc91e`
- ship: `fcbf6ed710e2240eb78de82c2f3f537e`
- cutaway: `bf0be9bb431a5915ce5e91617923c9d2`

## Soft-FAIL
- Soft-FAIL overwrite stack / cutaway / shell / side / tank masks
- Soft-FAIL Play / device / invent unpublished geometry


## Work dir
`/workspace/agent-backups/Da-Vinci/plume-anchor-20260923/electron`
