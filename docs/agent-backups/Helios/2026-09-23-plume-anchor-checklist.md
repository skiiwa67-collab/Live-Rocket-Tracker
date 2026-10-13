# Plume-anchor fleet checklist — Helios 2026-09-23

## Root Soft-PASS (Ada)
Post-sep `drawAssembledParts` → `paintArtPathFlames` WITHOUT hullDest/hullSrc (VehicleDraw ~L422–424) → baseY stub Soft-FAIL dest under nozzle.
Stamp 100 path Soft-PASS only when hullDest+hullSrc passed. CZ-8A PNG flame/engine Soft-PASS overlap (-15px) tiphead.

## CODE Soft-FAIL (Ada owns)
- Fix: pass hullDest/hullSrc into paintArtPathFlames on post-sep ship path (same Stamp 100 map).
- Soft-FAIL tip142 Studio until Helios Soft-PASS Da Vinci art + Ada code Soft-PASS.
- Soft-FAIL Play Soft-FAIL adb Soft-FAIL invent Soft-FAIL Chris push until go.

## ART Soft-PASS targets (Da Vinci) — Starship-bar S2 nozzles + flame flush
Families WITH engine_ship + flame_ship Soft-PASS PNG overlap tiphead:
- cz8a (FIRST) gap=-15
- cz12 gap=+3
- cz6a gap=-21
- electron gap=-5
- h3 gap=-21
- kinetica1 gap=-3
- nuri gap=-9

Soft-FAIL / need work:
- f9: flame_ship Soft-PASS, engine_ship MISSING (FLAME_NO_ENGINE)
- sr75: engine_ship + flame_ship exist but 0 opaque px Soft-FAIL EMPTY
- starship: engine_ship Soft-PASS, no flame_ship (ENGINE_ONLY — may use procedural)

NO_FLAME / no ship engine on tiphead (skip or queue later): ariane atlas cz2d fh firefly glenn lm5 lvm3 proton soyuz vulcan zq3

Inventory JSON: /workspace/agent-backups/Helios/2026-09-23-plume-anchor-quick-inventory.json

Ada audit: /workspace/agent-backups/Ada/2026-09-23-cz8a-postsep-plume-audit.md
