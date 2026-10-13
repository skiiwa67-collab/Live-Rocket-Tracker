# F9 plume-anchor — Soft-FAIL HOLD (no engine_ship)

**Family:** f9
**When:** 2026-09-23 CT
**Verdict:** Soft-FAIL HOLD — Soft-FAIL invent side-profile MVac / octaweb conversion

## Why HOLD
- `vehicle_f9_flame_ship.png` Soft-PASS exists (bbox y 402–599) under ship bottom y≈401.
- `vehicle_f9_engine_ship.png` **MISSING**.
- Catalog already Soft-FAIL blocked: face-on `vehicle_f9_engine_octaweb.png` is 424×200 — Soft-FAIL invent side-profile Merlin/MVac without measured side engine layer (`nozzle-catalog-20260923/f9/READY.md`).

Cannot Soft-PASS true side-profile MVac without invent or Chris Soft-PASS invent exception.

## Soft-FAIL body (untouched)
| layer | md5 |
|-------|-----|
| stack | `73004957061f…` (hud) |
| ship / s2 | `fa4605fb3a52…` |
| flame_ship | `bdf2ecdef4c6…` KEEP (unchanged this run) |

## Next (needs Chris GO)
- Measured side-profile MVac engine layer, OR
- Chris Soft-PASS invent exception for side-profile MVac under ship.

## Soft-FAIL
Play / device / invent octaweb→side / stack overwrite / invent engine_ship.
