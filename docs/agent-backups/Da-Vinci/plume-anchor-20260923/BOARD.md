# Plume-anchor BOARD — Da Vinci 2026-09-23

Helios checklist: `/workspace/agent-backups/Helios/2026-09-23-plume-anchor-checklist.md`
FLEET_AUDIT: `/workspace/agent-backups/Da-Vinci/plume-anchor-20260923/FLEET_AUDIT.md`

## Soft-PASS READY (flame sealed gap=−6; engine KEEP Starship-bar; stack Soft-FAIL untouched)

| family | pack | flame md5 | eng md5 (KEEP) | stack Soft-FAIL unchanged | mirrored |
|--------|------|-----------|----------------|---------------------------|----------|
| **cz8a** | `plume-anchor-20260923/cz8a/` | `2455d7b3a2ccca1096130921c1eef253` | `6f9b1caf943987f14856de61fdba5db7` | `a38272674c951621882f086e7dd85b4d` | hud-assets + lrt-cz8a-pack |
| **cz12** | `plume-anchor-20260923/cz12/` | `0614123bab749ce85bd6db1ccabf0329` | `09ad9bfed96b05d22d7a239fd633ab7c` | `9dc0795d2bc4c8df8f91099871240d4a` | hud-assets only (no lrt-cz12-pack) |
| **cz6a** | `plume-anchor-20260923/cz6a/` | `11e49ba47e45457a6e1da88effe5aad9` | `f1b8ed16263c977302f996d6ba1d9ac9` | `0290f8c22f3d409d2e768d55ddc3a8e9` | hud-assets + lrt-cz6a-pack |
| **electron** | `plume-anchor-20260923/electron/` | `035967caa63531320f4bb3ff601eea65` | `4020879c91ebdf03202f01d2bd729659` | `d8ab6a86858da2237a18838c4c4dc91e` GOLD tiphead | hud-assets + lrt-electron-pack |
| **h3** | `plume-anchor-20260923/h3/` | `b101e3992e5d76e2014c40c70e735389` | `968d76117f5821a362d8caa98a4da4c6` | `e68bf1bfd260db5d9dec103d01771fb4` | hud-assets + lrt-h3-pack |
| **kinetica1** | `plume-anchor-20260923/kinetica1/` | `91eb3443802512f846b6a3fa2cd964a1` | `cfca95fce2f0df7a8279340f044797b5` | `0c919bed7c069b980a6fdc1462a78d10` | hud-assets + lrt-kinetica1-pack |
| **nuri** | `plume-anchor-20260923/nuri/` | `c02dd46aa5b832bcc9eb7791bdb0eab2` | `ea6f49731b520f793e9137da54846ee2` | `c4ed89d5cd0d170164f7ce160c19abb8` | hud-assets + lrt-nuri-pack |

Each pack has: `READY.md`, `MD5SUMS.txt`, `compare_<id>_postsep.png`, `_proof_<id>_postsep.png`, flame_ship (+ flame_s2 + exhaust_ship aliases), engine_ship KEEP.

## Soft-FAIL HOLD
| family | reason | pack |
|--------|--------|------|
| **sr75** | Published single-stage HyPLOX-75 only; ship layer = fairing; Soft-FAIL invent phantom S2. engine_booster KEEP WAVE1. | `plume-anchor-20260923/sr75/READY.md` |
| **f9** | engine_ship MISSING; Soft-FAIL invent side-profile MVac from face-on octaweb without Chris GO. | `plume-anchor-20260923/f9/READY.md` |

## Skip NO_FLAME (tiphead) — unchanged
ariane atlas cz2d fh firefly glenn lm5 lvm3 proton soyuz vulcan zq3

## Next-wave Soft-FAIL float (from FLEET_AUDIT, not in Helios queue this run)
cz2d, fh, gravity1, kz11, pallas1

## Standing Soft-FAIL
- Soft-FAIL Play / device / Studio / Ada ping / SendToAgent
- Soft-FAIL stack / cutaway / shell / side / tank overwrite always
- Soft-FAIL bake plume into engine_*
- Electron GOLD body md5 `d8ab6a86858da2237a18838c4c4dc91e` Soft-FAIL overwrite (verified)
