# DEFECTS_ART_DRAFT — tip151 FULL catalog (Da Vinci art side)

**When:** 2026-09-23 CT (Chris HARD: NO partial Soft-PASS)  
**Lane:** Da Vinci art audit only → joint list with Ada  
**Sources measured:** `/workspace/hud-assets/` (canonical), cross-check `lrt-electron-pack`, `lrt-gravity1-pack`, `lrt-kz11-pack`, `lrt-cz2d-pack`, tip145 `/workspace/agent-backups/Da-Vinci/nozzle-tip145-20260923/`  
**α threshold:** >8 opaque bbox  
**gap:** `flame_top_y − engine_bottom_y` (≤0 = seal/overlap PASS; >0 = float Soft-FAIL candidate)  
**STG2 peer set:** content-cropped plates (`fill<0.4`), n=15; median aspect **1.528**, median bb **201×259**

## HARD Soft-FAIL standing (this draft)

| rule | status |
|------|--------|
| Soft-FAIL invent unpublished geometry | HELD |
| Soft-FAIL overwrite stack/cutaway/shell/side/tank on nozzle-only (`HARD-nozzle-only-no-stack-overwrite.md`) | HELD |
| Soft-FAIL F9 invent side MVac | HELD |
| Soft-FAIL claim phantom SR75 S2 | HELD |
| Soft-FAIL actually rebuilding PNGs this tip | **HELD — draft only** |
| Soft-FAIL ship | **HELD** |

## Seed defects (include — do not invent more geometry)

| family | asset | issue | evidence (md5/size/gap) | Ada-first? | rebuild? |
|--------|-------|-------|-------------------------|------------|----------|
| electron | `vehicle_electron_s2.png` | STG2 nose clipped by HUD box top — stage too tall / wrong aspect for HUD STG2 plate | canvas 424×1200; opaque bb **61×185**; aspect **3.033** (peer median 1.528, **TALL** + **LOW_BH**); top_pad=238 bot_pad=777; md5 `7cc527e09709164065069b722750653b` (hud=pack MATCH); side/stack GOLD md5 `d8ab6a86858da2237a18838c4c4dc91e` | No (art plate aspect) | **HOLD** until Ada Soft-FAIL-names; Soft-FAIL rebuild now |
| gravity1 | `vehicle_gravity1_s1/s2/s3/s4.png` (+ stage plate family) | Stage plates cropped/underscaled for HUD | **s2** bb **297×244** asp **0.822** (**SHORT** vs median 1.528); md5 `faed4d90ba372c844fd14515e5cd5d18`. **s1** bb 297×365 asp 1.229 md5 `0f147b073a3a…`. s3/s4 bb 165×351 asp 2.127. side bb 297×994 asp 3.347 md5 `c78f781a40a0…`. pack=hud MATCH | No (art scale/crop) | **HOLD** until Ada Soft-FAIL-names; Soft-FAIL rebuild now |
| kz11 | `vehicle_kz11_s2.png` + `flame_s2` | STG2 plate fit + STG2 plume inside body | **STG2 plate:** bb 201×353 asp **1.756** (~median; not art-tall outlier) md5 `804cbd86d0ebc4d7a6043cdadfec8ea7`. **flame_s2 vs engine_ship gap = −6 PASS** (eng_bot=286 flame_top=280); flame/ship md5 `b8d12b56f3d75fd05963c317c4285308` tip145=hud MATCH. Art flame_s2 bbox **not** Soft-FAIL | **Yes — Ada hullDest first**; flag art `flame_s2` only if bbox Soft-FAIL (currently PASS) | Art flame: **no**. Plate fit: Ada-first then art if Ada Soft-FAIL-names |
| cz2d | `vehicle_cz2d_flame_s2.png` / ship plume | STG2 plume inside body | **flame_s2/ship vs engine_ship gap = −23 PASS** (eng_bot=383 flame_top=360); eng_ship md5 `bc59aa6621f48f848b5da4165742eeea`; flame md5 `cf5b7c56c68047125240ad46fe907c00` tip145=hud MATCH. Art nozzle–plume seal OK → body intrusion is layout | **Yes — Ada code first** | Art flame: **no** (gap PASS). Soft-FAIL invent geometry |

## Measured flags (audit — not new invented geometry)

| family | asset | issue | evidence (md5/size/gap) | Ada-first? | rebuild? |
|--------|-------|-------|-------------------------|------------|----------|
| f9 | `vehicle_f9_s2.png` | STG2 aspect TALL peer (same class as Electron) | bb 73×249 asp **3.411** TALL; md5 `fa4605fb3a52632915cde2954d7d8919` | Joint (may be intentional thin S2) | HOLD — Soft-FAIL invent |
| f9 | `engine_ship` / `flame_ship` | No side `engine_ship`; octaweb vs flame_ship gap **+203** Soft-FAIL candidate | only `vehicle_f9_engine_octaweb.png` md5 `3cba0c97febd…`; flame_ship md5 `bdf2ecdef4c6…`; gap octaweb_bot=199 → flame_top=402 = **+203** | N/A | **Soft-FAIL invent side MVac** — HOLD / no art invent |
| nuri | `vehicle_nuri_s2.png` | STG2 aspect TALL vs content median | bb 152×356 asp **2.342** TALL; md5 `784b32ecc2b8bde9930b8100e322a2bf` | Joint eyes | HOLD pending Ada Soft-FAIL-names |
| cz2d | `vehicle_cz2d_s2.png` | STG2 SHORT / LOW_BH (measured; may relate to HUD plate) | bb 210×192 asp **0.914** SHORT+LOW_BH; md5 `36ecbab9aeb350253fe9561abbcc0894` | Possibly Ada fit | HOLD — do not invent; Ada Soft-FAIL-names first |
| cz8a | `vehicle_cz8a_s2.png` | STG2 SHORT vs median | bb 332×293 asp **0.883**; md5 `c7c1c0576e948165891f95cece3a8301` | Joint | HOLD |
| cz12 | `vehicle_cz12_s2.png` | STG2 SHORT vs median | bb 238×227 asp **0.954**; md5 `e38293c0a33a01e108fbd6b0f60ae369` | Joint | HOLD |
| sr75 | `vehicle_sr75_s2.png` | EMPTY opaque plate (not published S2) | canvas 424×1200; opaque bb **empty**; md5 `577f9382ed993ca2056b1346233ae1f4` (= tip145 empty OLD sentinel) | Ada upper-alias aware | **Soft-FAIL claim phantom SR75 S2** — do not treat as real S2 art debt |
| sr75 | `engine_ship` + `flame_ship` / `flame_s2` | tip145 upper-alias seal on disk (gap PASS) — not published S2 | eng_ship `84b24c7e3f9c7396ac35172dd57aa385`; flame_ship/s2/exhaust `09075b3f383e2292f7334864438fd9fa`; gap **−6 PASS**; tip145=hud MATCH | Ada alias | Soft-FAIL phantom-S2 claim; no body overwrite |

## tip145 / plume-7 still on disk (verify)

### Plume-7 `flame_ship` (hud = expect MATCH)

| id | hud md5 | status |
|----|---------|--------|
| cz8a | `2455d7b3a2ccca1096130921c1eef253` | MATCH |
| cz12 | `0614123bab749ce85bd6db1ccabf0329` | MATCH |
| cz6a | `11e49ba47e45457a6e1da88effe5aad9` | MATCH |
| electron | `035967caa63531320f4bb3ff601eea65` | MATCH |
| h3 | `b101e3992e5d76e2014c40c70e735389` | MATCH |
| kinetica1 | `91eb3443802512f846b6a3fa2cd964a1` | MATCH |
| nuri | `c02dd46aa5b832bcc9eb7791bdb0eab2` | MATCH |

### tip145 family packs under `nozzle-tip145-20260923/` (MD5SUMS present; hud MATCH)

| family | eng_ship | flame_ship / flame_s2 | gap (hud α>8) |
|--------|----------|----------------------|---------------|
| cz2d | `bc59aa6621f48f848b5da4165742eeea` | `cf5b7c56c68047125240ad46fe907c00` | **−23 PASS** |
| kz11 | `6e2f9e313ca0321753e1efc52dd9fac4` | `b8d12b56f3d75fd05963c317c4285308` | **−6 PASS** |
| sr75 | `84b24c7e3f9c7396ac35172dd57aa385` | `09075b3f383e2292f7334864438fd9fa` | **−6 PASS** |
| gravity1 | `a1e92bb24313c4cabe09c6f88a35a73b` | `ec5a3b76a75a464f049fa5e1e42fb476` | **−6 PASS** |
| fh | `4cbad0fcc02b3b078be155c989f2c514` | `14b10f3260c9e524f5770085b10e89f8` | **−6 PASS** |
| pallas1 | `232271f7f7fdacc6862b70a77736e5c9` | `f33823ed9a5efbe4e45b050fbadbc02c` | **−6 PASS** |

## Fleet flame_ship vs engine_ship gap snapshot (hud, α>8)

All catalog families with `engine_ship` (or starship ship eng): **PASS gap≤0** except:

| family | gap | note |
|--------|-----|------|
| f9 | **+203** vs octaweb | Soft-FAIL invent side MVac — no fix this draft |
| *(no eng_ship)* ariane atlas firefly glenn gslv isro lvm3 proton pslv sls soyuz vulcan | n/a | Soft-FAIL invent unpublished `engine_*` |

## Art Soft-FAIL-name candidates for Ada joint (rebuild later)

1. **electron** — STG2 plate aspect/tall fit (seed + measured TALL/LOW_BH)  
2. **gravity1** — stage plates underscaled/cropped (seed + measured SHORT s2)  
3. **kz11** — Ada hullDest first for “plume in body”; art `flame_s2` **not** Soft-FAIL on bbox/gap  
4. **cz2d** — Ada code first for “plume in body”; art gap PASS; optional later plate SHORT flag if Ada Soft-FAIL-names  

## Soft-FAIL this tip

- Soft-FAIL PNG rebuilds  
- Soft-FAIL stack/cutaway/shell/side/tank overwrite  
- Soft-FAIL ship / device / Studio / Play / commit  
- Soft-FAIL invent F9 side MVac / phantom SR75 published S2 / unpublished fleet engines  

**Deliverable:** this file only.  
**Path:** `/workspace/agent-backups/Da-Vinci/tip151-joint-defects-20260923/DEFECTS_ART_DRAFT.md`
