# F9 tip145 — HOLD

**When:** 2026-09-23 CT
**Verdict:** HOLD Soft-FAIL invent side-profile MVac

## Why
- `engine_ship` **MISSING** (only face-on `engine_octaweb` 424×200 md5 `3cba0c97febd31227bdfbdef894a4fe3`).
- Soft-FAIL invent side MVac from face-on octaweb without published side refs / Chris GO.
- `flame_ship` exists: opaque_px=7976 bbox=(190, 402, 244, 599) md5 `bdf2ecdef4c683ad66f9cc214bb7139b`.
- gap vs ship bottom: 1 (ship bb=(175, 153, 247, 401)). No eng exit to seal against → Soft-FAIL rebuild invent.

## Soft-FAIL
invent side MVac · stack overwrite · commit/device

## Work
`/workspace/agent-backups/Da-Vinci/nozzle-tip145-20260923/f9`
