# CATALOG_BOARD — tip145 nozzle+plume gap-close 2026-09-23

**Deliver:** `/workspace/agent-backups/Da-Vinci/nozzle-tip145-20260923/`
**Bar lib:** `lib_side_bell.py` md5 `0f5111a272ddb2f9876784100e86bd46`
**Electron GOLD stack Soft-FAIL overwrite:** `d8ab6a86858da2237a18838c4c4dc91e` verify=MATCH

## Plume-7 confirm (must stay MATCH)

| id | expect | got | |
|----|--------|-----|---|
| cz8a | `2455d7b3a2cc…` | `2455d7b3a2cc…` | MATCH |
| cz12 | `0614123bab74…` | `0614123bab74…` | MATCH |
| cz6a | `11e49ba47e45…` | `11e49ba47e45…` | MATCH |
| electron | `035967caa635…` | `035967caa635…` | MATCH |
| h3 | `b101e3992e5d…` | `b101e3992e5d…` | MATCH |
| kinetica1 | `91eb34438025…` | `91eb34438025…` | MATCH |
| nuri | `c02dd46aa5b8…` | `c02dd46aa5b8…` | MATCH |

## PASS (this tip145)

| vehicle | eng_ship md5 | flame_ship md5 | gap before→after | note |
|---------|--------------|----------------|------------------|------|
| sr75 | `84b24c7e3f9c7396ac35172dd57aa385` | `09075b3f383e2292f7334864438fd9fa` | None→-6 | NEW eng_ship under fairing + flame seal; Ada upper-alias (Soft-FAIL phantom-S2 claim) |
| cz2d | `bc59aa6621f48f848b5da4165742eeea` | `cf5b7c56c68047125240ad46fe907c00` | 4→-23 (3-bell; bottom seal≤0) | rebuild Starship-bar eng_ship + seal flame (3 bells) |
| fh | `4cbad0fcc02b3b078be155c989f2c514` | `14b10f3260c9e524f5770085b10e89f8` | 4→-6 | KEEP eng; seal flame |
| gravity1 | `a1e92bb24313c4cabe09c6f88a35a73b` | `ec5a3b76a75a464f049fa5e1e42fb476` | 14→-6 | KEEP dual-band eng; seal lowest exit only |
| kz11 | `6e2f9e313ca0321753e1efc52dd9fac4` | `b8d12b56f3d75fd05963c317c4285308` | 4→-6 | KEEP eng; seal flame |
| pallas1 | `232271f7f7fdacc6862b70a77736e5c9` | `f33823ed9a5efbe4e45b050fbadbc02c` | 4→-6 | KEEP eng; seal flame |

## HOLD

| vehicle | reason |
|---------|--------|
| f9 | engine_ship missing; Soft-FAIL invent side MVac from face-on octaweb |

## Skip / already OK (no tip145 rewrite)

- **lm5**: skip gap=-14 (already ≤0 or plume-7 KEEP)
- **zq3**: skip gap=-20 (already ≤0 or plume-7 KEEP)
- **cz6a**: skip gap=-6 (already ≤0 or plume-7 KEEP)
- **h3**: skip gap=-6 (already ≤0 or plume-7 KEEP)
- **nuri**: skip gap=-6 (already ≤0 or plume-7 KEEP)
- **cz8a**: skip gap=-6 (already ≤0 or plume-7 KEEP)
- **cz12**: skip gap=-6 (already ≤0 or plume-7 KEEP)
- **electron**: skip gap=-6 (already ≤0 or plume-7 KEEP)
- **kinetica1**: skip gap=-6 (already ≤0 or plume-7 KEEP)
- **starship**: skip gap=-107 (already ≤0 or plume-7 KEEP)

## Soft-FAIL invent blocked (no engine_*)
ariane atlas falcon firefly glenn gslv isro lvm3 proton pslv sls soyuz vulcan

## Soft-FAIL standing
- Soft-FAIL stack/cutaway/shell/side/tank overwrite (bodies verified unchanged per READY)
- Soft-FAIL commit / device / Studio / Play / git
- Soft-FAIL invent unpublished geometry (F9 MVac; phantom published-S2 claim)
- Soft-FAIL 8-chip console rebuild (stand by)

## Awaiting
Chris eyes Soft-PASS full tip145 set before device/Studio.

