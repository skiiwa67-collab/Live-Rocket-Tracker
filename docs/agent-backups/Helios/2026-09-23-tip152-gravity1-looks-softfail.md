# tip152 Gravity-1 HUD identity Soft-FAIL — Soft-PASS diagnosis

**When:** 2026-09-23 ~10:51 CT  
**Reporter:** Chris Soft-FAIL phone tip152 PAD 152 (visible)  
**Mission:** SIM · Gravity-1 | SpaceSail Pola @ T+02:55 after STAGE SEP  
**Quote:** rocket "no longer looks like the gravity 1"  
**AUTO Soft-PASS:** leave-sim → live works (not this defect)  
**Phone shot:** `/home/box/agent-data/agents/fe0b6c3f-adcf-483f-a7f9-335890abd80d/attachments/ec70d1c8b47255ee49e041eb01774b623ae8352f81087c8ec71c4de109419494.png`  
**HEAD:** a6553d2 / versionCode 152 / 1.0.142 (stamp-85)

Soft-FAIL Studio / adb / Play / device until Chris Soft-PASS again. Soft-FAIL invent F9 / SR75.

---

## What Chris is seeing (phone)

| Slot | Label | Content |
|------|-------|---------|
| Left | STG1 | Spent white stub, "G-1" mark, bottom nozzles, no plume |
| Right | STG2 | Lit white stub + central hatch outline + **plume** (active) |
| Tape | STAGE SEP | Highlighted after MECO |
| Map | East Asia / Yellow–East China Sea track | Correct mission geography Soft-PASS |

Catalog Soft-PASS: `VehicleCatalog` id/family/drawFamily = `gravity1` (Orienspace all-solid M1+SRBs / M2 / M3). Soft-FAIL invent wrong family.

---

## Exact drawable md5s on device tree (gravity1)

Path: `/workspace/RetroCommandCenter/app/src/main/res/drawable/`

| file | md5 | notes |
|------|-----|-------|
| `vehicle_gravity1_s1.png` | `6638905cfa0cb545a9b900082f967154` | tip151 AFTER / tip152 wire; bb 221×320 asp 1.448 |
| `vehicle_gravity1_s2.png` | `0cceb567adc983592d0205d7e1cb2ee0` | tip151 AFTER / tip152 wire; bb 151×258 asp 1.709 |
| `vehicle_gravity1_s3.png` | `cae31922d84a64e10d8934ded4f4287b` | tip151 AFTER; = s4 alias |
| `vehicle_gravity1_s4.png` | `cae31922d84a64e10d8934ded4f4287b` | alias of s3 (Soft-FAIL invent liquid S4 held) |
| `vehicle_gravity1_booster.png` | `0f147b073a3ab67aa289b71dfcbc5ecd` | **= tip151 BEFORE s1** (unchanged by tip152) |
| `vehicle_gravity1_ship.png` | `ca62ae67e330c15e191e56139e06223f` | not preferred post-sep |
| `vehicle_gravity1_upper.png` | `f1acc81d7f70ed25a643a03398833e99` | not preferred post-sep |
| `vehicle_gravity1_side.png` | `c78f781a40a01676fdb57ae5343ba07b` | GOLD Soft-PASS identity ref (= stack) |
| `vehicle_gravity1_stack.png` | `c78f781a40a01676fdb57ae5343ba07b` | same bytes as side; Soft-FAIL overwrite held |
| `vehicle_electron_stack.png` | `d8ab6a86858da2237a18838c4c4dc91e` | electron GOLD Soft-PASS untouched (tip152 Soft-FAIL overwrite held) |

Da Vinci pack `/workspace/agent-backups/Da-Vinci/tip151-stage-fit-20260923/` s1–s4 md5s **MATCH** drawable tree (tip152 wire Soft-PASS).

tip151 BEFORE (pre-wire Soft-PASS stage plates / nozzle-catalog):

| file | before md5 |
|------|------------|
| s1 | `0f147b073a3ab67aa289b71dfcbc5ecd` |
| s2 | `faed4d90ba372c844fd14515e5cd5d18` |
| s3 | `f265f0ada5eb1ec8bb6c5bec9424cfbc` |
| s4 | `cae1ac2a7964548cb7d8c5f1a457d051` |

---

## Post-SEP binding (AssembledVehicle) — Soft-FAIL invent wrong family Soft-PASS shadow

`VehicleDraw.drawAssembledParts` (`gravity1` post-sep; pre-sep returns null → continuous stack):

| HUD slot | Prefer order | **Winner on tree** | Bytes |
|----------|--------------|--------------------|-------|
| STG1 left | booster_shell → **booster** → s1 → core | **`vehicle_gravity1_booster`** | OLD s1 `0f147b07…` |
| STG2 right | **s2** → upper → ship_shell → ship | **`vehicle_gravity1_s2`** | NEW tip152 `0cceb567…` |

**Consequence:** tip152 `s1` (`6638905c…`) **never appears** post-SEP (booster shadows it). Chris Soft-FAIL identity is driven by **NEW tip152 STG2 s2** (+ plateCap fill), while STG1 still shows stale booster.

Catalog names Soft-PASS: s1Name=`GRAVITY-1 CORE + STRAPONS`, s2Name=`UPPER (M2/M3)` — binding ids are gravity1 Soft-PASS; Soft-FAIL Ada invent F9/SR75 ids.

---

## GOLD / Soft-PASS Gravity-1 identity (pre-tip152)

**Canonical Soft-PASS art:** `vehicle_gravity1_side` / `_stack` md5 `c78f781a40a01676fdb57ae5343ba07b`

Distinctive features present on GOLD side (paper-doll Soft-PASS, not photo):

- PRC flag + fairing orange/brown stripe
- Vertical **引力** + **G-1** marks
- Core + **side SRB cluster** silhouette (side view shows 2 SRBs; catalog: 4× M1 strap-ons + core)
- Segmented all-solid HTPB ochre paper-doll stack (Stamp 119)
- Soft-FAIL claim grid fins as required identity — real Gravity-1 has no grid fins; GOLD side also Soft-FAIL grid fins (none drawn). Soft-PASS identity = **SRB cluster + 引力/G-1 + fairing flag**, not fins.

Pre-tip152 stage Soft-PASS (tip151 `_before` / nozzle-catalog): s2 retained **wide SRB-shoulder silhouette** (bb 297×244 asp 0.822 SHORT — aspect Soft-FAIL, identity Soft-PASS). tip151 AFTER stripped that into a **generic dual-nozzle white stub** (see pack `preview_stage_fit_board.png`).

---

## Hypothesis Soft-PASS / Soft-FAIL

| # | Hypothesis | Verdict | Evidence |
|---|------------|---------|----------|
| H1 | **Da Vinci tip151 stage-fit rebuild destroyed Gravity-1 identity** | **Soft-PASS ROOT** | Rebuild: `core_only_mask` stripped SRB wings from s2; anisotropic peer-asp scale; s3/s4 collapsed to same stub. Preview board AFTER s2/s3/s4 = interchangeable white blocks. tip152 wired those 4 plates. Chris Soft-FAIL after STAGE SEP looking at STG2. |
| H2 | **Ada wrong drawable id / wrong family** | **Soft-FAIL invent family**; **Soft-PASS shadow defect** | Family Soft-PASS `gravity1`. STG2 Soft-PASS binds `_s2` (intended). Soft-PASS Ada defect: **booster shadows tip152 s1** so STG1≠wired s1; mixed-generation Soft-FAIL look. Soft-FAIL invent F9/SR75 binding. |
| H3 | **tip151 plateCap 0.92 / content-bbox crops distinctive silhouette into stub** | **Soft-PASS AMPLIFIER**; Soft-FAIL sole root | `plateCap = h * 0.92f` + `partContentSrc` content-bbox (cbead70) fills STG box from opaque bb. Fat/stubby plates → fat HUD stubs. Helios already Soft-PASS'd aspect; Soft-FAIL revert fleet plateCap as first fix. |
| H4 | tip152 Soft-FAIL overwrite electron GOLD stack | **Soft-FAIL** (held) | electron stack still `d8ab6a86858da2237a18838c4c4dc91e` |
| H5 | Leave-sim / AUTO broken | **Soft-FAIL** | Chris: AUTO Soft-PASS leave-sim→live |

---

## Root cause ranking

1. **#1 Soft-PASS — Da Vinci tip151 Soft-FAIL-named stage plates (esp. s2) lost Orienspace Gravity-1 silhouette** when fitting peer asp; tip152 shipped them. Named cause: **identity Soft-FAIL from stage-fit rebuild (core_only + anisotropic), not wrong catalog family.**
2. **#2 Soft-PASS — Ada booster→s1 shadow** — tip152 s1 never drawn post-SEP; STG1 stuck on pre-tip151 booster bytes.
3. **#3 Soft-PASS amp — tip151 plateCap 0.92 + content-bbox** magnifies stub plates into Chris's fat generic STG panes.

---

## Defect list

1. `vehicle_gravity1_s2` Soft-FAIL identity: SRB shoulders removed → generic stub (`0cceb567…`).
2. `vehicle_gravity1_s3`/`s4` Soft-FAIL identity: both `cae31922…` interchangeable stubs (s4 alias Soft-FAIL invent liquid S4 held by design).
3. `vehicle_gravity1_s1` tip152 Soft-PASS bytes unused post-SEP (booster shadow).
4. `vehicle_gravity1_booster` stale vs tip152 s1 (desync Soft-FAIL).
5. plateCap/content-bbox Soft-PASS amp of stub geometry.
6. Soft-FAIL: invent F9/SR75 art or binding as remedy.
7. Soft-FAIL: phone / Play / adb / Studio push from agents until Chris Soft-PASS.

---

## Concrete next order

### Da Vinci — FIRST (identity Soft-PASS)

1. Rebuild Soft-FAIL-named gravity1 **s1–s4** from GOLD side `c78f781a…` (read-only stack Soft-FAIL overwrite).
2. Soft-PASS restore Gravity-1 identity:
   - s1: M1 core + SRB cluster silhouette Soft-PASS (published strap-on look)
   - s2: M2 mid-band Soft-PASS — Soft-FAIL `core_only` wipe of distinctive shoulders; keep Orienspace paper-doll cues (引力 / hatch / proportions)
   - Soft-PASS Starship-bar nozzle flush quality on stage exits (same bar Soft-PASS used elsewhere)
3. Soft-FAIL invent engines beyond published eng_booster/eng_ship bake.
4. Soft-FAIL invent F9 / SR75 / Electron GOLD overwrite / gravity1 stack-side overwrite.
5. Soft-FAIL invent distinct liquid S4 unless Chris Soft-PASS names it; s4 alias Soft-FAIL invent remains until Soft-PASS.
6. Deliver pack + md5 board under `/workspace/agent-backups/Da-Vinci/` — Soft-FAIL ship.

### Ada — SECOND (wire + binding Soft-PASS)

1. After Da Vinci Soft-PASS plates: wire s1–s4 (and **sync `booster` = Soft-PASS s1** OR prefer `_s1` before `_booster` for gravity1 so tip plates actually show).
2. Soft-FAIL revert fleet plateCap 0.92 unless Chris Soft-PASS keeps it after identity Soft-PASS.
3. Soft-FAIL invent F9/SR75; Soft-FAIL phone/Play/adb; Soft-FAIL Studio until Chris Soft-PASS.
4. Re-verify PAD 152 Gravity-1 | SpaceSail Pola @ post STAGE SEP: STG1 left / STG2+plume right Soft-PASS "looks like Gravity-1".

### Helios / ops

- Soft-FAIL phone / Play / adb until Chris Soft-PASS.
- Prior Helios Soft-PASS on tip151 aspect stands; this ticket is **identity Soft-FAIL** — Soft-PASS diagnosis above.

---

## Success

- Diagnosis Soft-PASS with named root: **Da Vinci tip151 stage-fit identity loss on gravity1 s2 (wired tip152), amplified by Ada booster-shadow + plateCap fill.**
- Fix order Soft-PASS: **Da Vinci identity rebuild → Ada sync booster/s1 + wire → Chris Soft-PASS phone** (agents Soft-FAIL phone/Play/adb).

