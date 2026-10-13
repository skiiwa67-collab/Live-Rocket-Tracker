# Wave 1 nozzle + SRB burn — Soft-PASS READY summary
**Date:** 2026-09-23 CT · Da Vinci art executor

## Status
| Lane | Vehicle | Status |
|------|---------|--------|
| A | CZ-6A | **READY** — Starship-bar nozzles + FG-112 burn @ 114.5 s NSF |
| B | H3-24 | **READY** — Starship-bar nozzles + SRB-3 burn @ 110 s JAXA |
| C1 | Nuri | **READY** — liquid nozzles only (Soft-FAIL solid burn) |
| C2 | SR-75 | **READY** — hybrid nozzle only (Soft-FAIL solid burn) |

## Soft-FAIL held
- Soft-FAIL commit / publish / thread / catalog ship / device / Studio / Play
- Soft-FAIL RetroCommandCenter git
- Soft-FAIL invent geometry or burn times (reused GOLD/measured placement; Helios research times)
- Soft-FAIL solid burn bar on CZ-8A / Nuri / SR-75
- Soft-FAIL CZ-8A liquid nozzles this wave (optional only after A–C READY — **not started**)
- Soft-FAIL full-fleet sprawl

## Lib
`/workspace/agent-backups/Da-Vinci/nozzle-wave1-20260923/lib_side_bell.py` md5 `0f5111a272ddb2f9876784100e86bd46` (matches Soft-PASS preview)

## Mirrors updated (art only, no git)
- `/workspace/agent-backups/Da-Vinci/nozzle-wave1-20260923/{cz6a,h3,nuri,sr75}/`
- Packs: `lrt-cz6a-pack-20260923`, `lrt-h3-pack-20260923`, `lrt-nuri-pack-20260923`, `lrt-sr75-pack-20260923` (+ cz6a alias pack)
- `/workspace/hud-assets/` matching vehicle_* + burn + proof

See per-vehicle `READY.md` for absolute paths + md5s.
