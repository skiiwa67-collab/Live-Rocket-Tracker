# NURI plume-anchor Soft-PASS — READY

**Family:** nuri
**When:** 2026-09-23 CT
**Lane:** Da Vinci art only. Soft-FAIL Play/device/Ada code.

## Soft-PASS writes
| file | md5 | note |
|------|-----|------|
| `vehicle_nuri_flame_ship.png` | `c02dd46aa5b832bcc9eb7791bdb0eab2` | NEW — opaque top seals into engine exit |
| `vehicle_nuri_flame_s2.png` | `c02dd46aa5b832bcc9eb7791bdb0eab2` | identical alias |
| `vehicle_nuri_exhaust_ship.png` | `c02dd46aa5b832bcc9eb7791bdb0eab2` | identical alias |
| `vehicle_nuri_engine_ship.png` | `ea6f49731b520f793e9137da54846ee2` | KEEP Starship-bar (unchanged) |

## Geometry
- Canvas: 424×1200 transparent
- engine_ship bbox: (201, 550, 222, 575) exit y1=575
- NEW flame bbox: (205, 569, 217, 605)
- **gap = -6** (seal 6px into bell)
- bell centers: [211.5]
- palette: rp1
- Soft-FAIL bake plume into engine_*

## Eyes
- `compare_nuri_postsep.png`
- `_proof_nuri_postsep.png`

## Mirrored to
- `/workspace/hud-assets`
- `/workspace/lrt-nuri-pack`

## Soft-FAIL body proof (unchanged)
- stack: `c4ed89d5cd0d170164f7ce160c19abb8`
- ship: `e39e182485b6dc9dbe50c5c6082ff1fe`
- cutaway: `c1dc333a1caea4050422fc33113fedb8`

## Soft-FAIL
- Soft-FAIL overwrite stack / cutaway / shell / side / tank masks
- Soft-FAIL Play / device / invent unpublished geometry


## Work dir
`/workspace/agent-backups/Da-Vinci/plume-anchor-20260923/nuri`
