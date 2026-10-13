# SR-75 plume-anchor — Soft-FAIL HOLD (EMPTY ship engine/flame)

**Family:** sr75
**When:** 2026-09-23 CT
**Verdict:** Soft-FAIL HOLD — Soft-FAIL invent phantom S2 nozzle+plume

## Why HOLD
Published HyImpulse SR-75 is a **single-stage** hybrid sounding rocket with **one HyPLOX-75** (paraffin+LOX). Sources: HyImpulse.de/sr75 · Southern Launch · `lrt-sr75-pack/ADA_VEHICLESPEC.md`.

- `vehicle_sr75_ship.png` bbox ≈ (165,48)–(258,172) is the **fairing/nose**, not an upper stage with a nozzle.
- `vehicle_sr75_engine_ship.png` / `flame_ship` / `flame_s2` / `exhaust_ship` are empty placeholders (0 opaque px, shared blank md5).
- Real nozzle already Soft-PASS on **booster**: `vehicle_sr75_engine_booster.png` md5 `e36d5fd9c3f7e5d5b80179f6c0d5b8d1` (WAVE1 Starship-bar).

Inventing a short S2 nozzle+plume under the fairing would be **unpublished geometry** (Soft-FAIL invent). Soft-FAIL stack/cutaway overwrite.

## Soft-FAIL body (untouched this run)
| layer | md5 |
|-------|-----|
| stack | `6c61b6a0ed3626bede65b20974a99663` |
| ship (fairing) | `a2b098717cdf…` (hud) |
| engine_booster | `e36d5fd9c3f7e5d5b80179f6c0d5b8d1` KEEP |

## Next (needs Chris/Helios GO)
- Option A: Soft-PASS leave ship engine/flame empty; Ada Soft-FAIL draw ship plume for single-stage.
- Option B: Chris Soft-PASS exception to paint decorative ship plume (still Soft-FAIL if treated as real S2).

## Soft-FAIL
Play / device / invent S2 / stack overwrite.
