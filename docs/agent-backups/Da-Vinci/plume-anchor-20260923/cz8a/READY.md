# CZ-8A plume-anchor Soft-PASS — READY

**Family:** cz8a (FIRST)
**When:** 2026-09-23 ~08:45 CT
**Lane:** Da Vinci art only. Soft-FAIL Play/device/Ada code.

## Soft-PASS writes
| file | md5 | note |
|------|-----|------|
| `vehicle_cz8a_flame_ship.png` | `2455d7b3a2ccca1096130921c1eef253` | NEW — opaque top seals into engine exit |
| `vehicle_cz8a_flame_s2.png` | `2455d7b3a2ccca1096130921c1eef253` | identical alias |
| `vehicle_cz8a_exhaust_ship.png` | `2455d7b3a2ccca1096130921c1eef253` | identical alias |
| `vehicle_cz8a_engine_ship.png` | `6f9b1caf943987f14856de61fdba5db7` | KEEP Starship-bar catalog (unchanged) |

## Geometry
- Canvas: 424×1200 transparent
- engine_ship bbox (α>8): (180,506)–(243,546) — twin Starship-bar bells, exit y1=546
- NEW flame bbox (α>8): (184,540)–(239,578)
- **gap = flame_top − eng_bottom = 540 − 546 = −6** (seal 6px into bell; Soft-PASS ≤0; prefer 2..8)
- Plume length ≈ 38px short/modest twin needles under bell centers 191.5 / 231.5
- Soft-FAIL bake plume into engine_*

## Eyes
- `compare_cz8a_postsep.png` — LEFT OLD flame (gap≈−14 soft) / RIGHT NEW (gap=−6 flush)
- `_proof_cz8a_postsep.png` — NEW alone (ship+engine+flame crop 2×)

## Mirrored to
- `/workspace/hud-assets/` — flame_ship, flame_s2, exhaust_ship, engine_ship (KEEP)
- `/workspace/lrt-cz8a-pack/` — same Soft-PASS set

## Soft-FAIL body proof (unchanged)
| layer | md5 before = after |
|-------|--------------------|
| stack | `a38272674c951621882f086e7dd85b4d` |
| ship  | `144a453138f0cae79ad86107eb77a39f` |
| cutaway | `559a33d75ec9c9d761045c73385e2a72` |

## Soft-FAIL
- Soft-FAIL overwrite stack / cutaway / shell / side / tank masks
- Soft-FAIL Play / device / invent unpublished geometry
- CODE post-sep hullDest = Ada lane (not this pack)

## lib
- `lib_side_bell.py` md5 `0f5111a272ddb2f9876784100e86bd46` (matches nozzle-catalog-20260923)

## Work dir
`/workspace/agent-backups/Da-Vinci/plume-anchor-20260923/cz8a/`
