# CZ6A plume-anchor Soft-PASS — READY

**Family:** cz6a
**When:** 2026-09-23 CT
**Lane:** Da Vinci art only. Soft-FAIL Play/device/Ada code.

## Soft-PASS writes
| file | md5 | note |
|------|-----|------|
| `vehicle_cz6a_flame_ship.png` | `11e49ba47e45457a6e1da88effe5aad9` | NEW — opaque top seals into engine exit |
| `vehicle_cz6a_flame_s2.png` | `11e49ba47e45457a6e1da88effe5aad9` | identical alias |
| `vehicle_cz6a_exhaust_ship.png` | `11e49ba47e45457a6e1da88effe5aad9` | identical alias |
| `vehicle_cz6a_engine_ship.png` | `f1b8ed16263c977302f996d6ba1d9ac9` | KEEP Starship-bar (unchanged) |

## Geometry
- Canvas: 424×1200 transparent
- engine_ship bbox: (200, 429, 223, 469) exit y1=469
- NEW flame bbox: (205, 463, 218, 499)
- **gap = -6** (seal 6px into bell)
- bell centers: [211.5]
- palette: rp1
- Soft-FAIL bake plume into engine_*

## Eyes
- `compare_cz6a_postsep.png`
- `_proof_cz6a_postsep.png`

## Mirrored to
- `/workspace/hud-assets`
- `/workspace/lrt-cz6a-pack`

## Soft-FAIL body proof (unchanged)
- stack: `0290f8c22f3d409d2e768d55ddc3a8e9`
- ship: `20c417d8163f6c9877c3f26255d18a10`
- cutaway: `956c5c87fb94f3b56380a2189657a3c9`

## Soft-FAIL
- Soft-FAIL overwrite stack / cutaway / shell / side / tank masks
- Soft-FAIL Play / device / invent unpublished geometry


## Work dir
`/workspace/agent-backups/Da-Vinci/plume-anchor-20260923/cz6a`
