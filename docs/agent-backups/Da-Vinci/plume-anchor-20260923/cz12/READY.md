# CZ12 plume-anchor Soft-PASS — READY

**Family:** cz12
**When:** 2026-09-23 CT
**Lane:** Da Vinci art only. Soft-FAIL Play/device/Ada code.

## Soft-PASS writes
| file | md5 | note |
|------|-----|------|
| `vehicle_cz12_flame_ship.png` | `0614123bab749ce85bd6db1ccabf0329` | NEW — opaque top seals into engine exit |
| `vehicle_cz12_flame_s2.png` | `0614123bab749ce85bd6db1ccabf0329` | identical alias |
| `vehicle_cz12_exhaust_ship.png` | `0614123bab749ce85bd6db1ccabf0329` | identical alias |
| `vehicle_cz12_engine_ship.png` | `09ad9bfed96b05d22d7a239fd633ab7c` | KEEP Starship-bar (unchanged) |

## Geometry
- Canvas: 424×1200 transparent
- engine_ship bbox: (152, 423, 270, 467) exit y1=467
- NEW flame bbox: (159, 461, 262, 503)
- **gap = -6** (seal 6px into bell)
- bell centers: [168.5, 253.5]
- palette: rp1
- Soft-FAIL bake plume into engine_*

## Eyes
- `compare_cz12_postsep.png`
- `_proof_cz12_postsep.png`

## Mirrored to
- `/workspace/hud-assets`

## Soft-FAIL body proof (unchanged)
- stack: `9dc0795d2bc4c8df8f91099871240d4a`
- ship: `e38293c0a33a01e108fbd6b0f60ae369`
- cutaway: `9dc0795d2bc4c8df8f91099871240d4a`

## Soft-FAIL
- Soft-FAIL overwrite stack / cutaway / shell / side / tank masks
- Soft-FAIL Play / device / invent unpublished geometry


## Work dir
`/workspace/agent-backups/Da-Vinci/plume-anchor-20260923/cz12`
