# H3 plume-anchor Soft-PASS — READY

**Family:** h3
**When:** 2026-09-23 CT
**Lane:** Da Vinci art only. Soft-FAIL Play/device/Ada code.

## Soft-PASS writes
| file | md5 | note |
|------|-----|------|
| `vehicle_h3_flame_ship.png` | `b101e3992e5d76e2014c40c70e735389` | NEW — opaque top seals into engine exit |
| `vehicle_h3_flame_s2.png` | `b101e3992e5d76e2014c40c70e735389` | identical alias |
| `vehicle_h3_exhaust_ship.png` | `b101e3992e5d76e2014c40c70e735389` | identical alias |
| `vehicle_h3_engine_ship.png` | `968d76117f5821a362d8caa98a4da4c6` | KEEP Starship-bar (unchanged) |

## Geometry
- Canvas: 424×1200 transparent
- engine_ship bbox: (200, 486, 223, 526) exit y1=526
- NEW flame bbox: (205, 520, 218, 558)
- **gap = -6** (seal 6px into bell)
- bell centers: [211.5]
- palette: hydrolox
- Soft-FAIL bake plume into engine_*

## Eyes
- `compare_h3_postsep.png`
- `_proof_h3_postsep.png`

## Mirrored to
- `/workspace/hud-assets`
- `/workspace/lrt-h3-pack`

## Soft-FAIL body proof (unchanged)
- stack: `e68bf1bfd260db5d9dec103d01771fb4`
- ship: `a04c0a65f5c62de7b236f5f8f9f56265`
- cutaway: `3f08b74d897c1e74995c3083c2b6177f`

## Soft-FAIL
- Soft-FAIL overwrite stack / cutaway / shell / side / tank masks
- Soft-FAIL Play / device / invent unpublished geometry


## Work dir
`/workspace/agent-backups/Da-Vinci/plume-anchor-20260923/h3`
