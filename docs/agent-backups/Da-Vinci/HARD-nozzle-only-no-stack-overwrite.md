# HARD — nozzle-only pass Soft-FAIL stack overwrite

**Effective:** 2026-09-23 (Helios heed + Chris Electron tiphead)
**Owner:** Da Vinci

## Soft-FAIL
On nozzle-only / Starship-bar engine rebuilds:
- Soft-FAIL overwrite `stack` / `cutaway` / `shell` / `side` / tank masks (`tank_*`, `*_tank_fills`)
- Soft-PASS write NEW `engine_*` (and optional eyes-only proofs) only
- Soft-FAIL mirror stack/cutaway composites into hud-assets or packs unless Helios/Chris Soft-PASS GO for that body layer

## Electron restore 2026-09-23
Restored LRT GOLD body to hud-assets + lrt-electron-pack from tiphead stack md5 `d8ab6a86858da2237a18838c4c4dc91e`.
Starship-bar `engine_*` Soft-PASS kept as eyes-only until Chris Soft-PASS full-catalog.
