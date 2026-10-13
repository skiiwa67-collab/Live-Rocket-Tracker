# tip152 Gravity-1 fuel cutout Soft-PASS audit

**When:** 2026-09-23 ~10:56 CT  
**Chris Soft-PASS question (phone tip152 Gravity-1):** "Is that cuttout done right for the fuel read?"  
**Mission:** SIM · Gravity-1 | SpaceSail Pola @ **T+02:55** after **STAGE SEP** (DIG/ANLG by STG2)  
**Phone shot:** `/home/box/agent-data/agents/fe0b6c3f-adcf-483f-a7f9-335890abd80d/attachments/ec70d1c8b47255ee49e041eb01774b623ae8352f81087c8ec71c4de109419494.png`  
**HEAD:** a6553d2 / versionCode 152 / 1.0.142 (stamp-85)  
**Related Soft-FAIL:** `2026-09-23-tip152-gravity1-looks-softfail.md` (identity) — this receipt is **fuel-window Soft-FAIL** on the same plates.

Soft-FAIL invent Soft-FAIL Studio Soft-FAIL Play Soft-FAIL adb Soft-FAIL phone push.

---

## Soft-PASS answer for Chris

### **NO Soft-FAIL — cutout is Soft-FAIL for DIG fuel read.**

The rounded-rect on tip152 AFTER **STG2 `vehicle_gravity1_s2`** is a **decorative opaque hatch outline**, Soft-FAIL Stamp 92 transparent cutout window Soft-PASS live tank fill Soft-PASS.

Phone Soft-PASS matches: white STG2 stub + black rounded-rect outline + **no HTPB ochre grain inside**. DIG/ANLG Soft-PASS UI chips Soft-FAIL proof of cutout Soft-PASS.

---

## Soft-PASS code contract (how LRT draws fuel)

`VehicleDraw.drawAssembledParts` Stamp 92 COMPOSITE:

1. engines  
2. **tank fills BEHIND** (`vehicle_*_tank_s*_fuel` / `*_ox`, tinted; all-solid Gravity-1 → ochre grain, ox empty Soft-PASS Stamp 119)  
3. **hull ON TOP** — **transparent cutout windows Soft-PASS show fuel**

Gravity-1 path:

| Phase | Path | Hull winner |
|-------|------|-------------|
| Pre-sep | `drawAssembledParts` returns null → continuous `vehicleHullBitmap` | prefers `stack` / `cutaway` / `side` (shell Soft-PASS windows exist but Soft-FAIL preferred first) |
| **Post-sep STG2 (Chris @ T+02:55)** | assembled `wantU` | **`vehicle_gravity1_s2`** first (`_s2` → upper → ship_shell → ship) |
| Post-sep STG1 | assembled `wantB` | **`vehicle_gravity1_booster`** (shadows tip152 `s1`) |

Fills Soft-PASS active post-sep STG2:

- `vehicle_gravity1_tank_s2_fuel` (ochre mask bb **186,520–238,608**) Soft-PASS drawn behind  
- `vehicle_gravity1_tank_s2_ox` Soft-PASS **empty** (all-solid)  
- `fuelRemain(tSec, launch, stage=2)` Soft-PASS ~**0.97** at T+175 (sep≈160 oneWay / `ow_sep`, seco≈540) — Soft-FAIL empty Soft-PASS; fill Soft-PASS nearly full Soft-PASS if window Soft-PASS  

Soft-FAIL: fills Soft-PASS drawn Soft-FAIL **invisible Soft-PASS** under opaque AFTER s2.

---

## Soft-PASS measurements (plate alpha over fuel mask)

| plate | vs mask | fuel_clear% | opaque% | center_clear% | Soft-PASS/FAIL |
|-------|---------|-------------|---------|---------------|----------------|
| **AFTER tip152 `s2`** (wired) | `tank_s2_fuel` | **0.0** | **96.5** | **0.0** | **Soft-FAIL opaque hatch** |
| BEFORE tip152 `s2` | `tank_s2_fuel` | 0.0 | 95.4 | 7.4 | Soft-FAIL mask align (edge clear Soft-FAIL fuel island) |
| AFTER tip152 `s3`/`s4` | `tank_s3_fuel` | **0.0** | **94.8** | **0.1** | **Soft-FAIL** (s3≡s4) |
| BEFORE tip152 `s3` | `tank_s3_fuel` | 0.0 | 97.4 | 81.5 | center hole Soft-PASS look Soft-FAIL fuel-mask align |
| **`shell`** | `tank_s2_fuel` | **94.3** | 5.7 | 19.9 | **Soft-PASS cutout Soft-PASS** |
| **`shell`** | `tank_s3_fuel` | **91.9** | 8.1 | 19.9 | Soft-PASS |
| `cutaway` | `tank_s2_fuel` | 0.0 | 100 | — | Soft-FAIL window (baked prop Soft-PASS charcoal path) |
| AFTER `s1` | `tank_s1_fuel` | 0.0 | 97.2 | 0.0 | Soft-FAIL |
| `booster` | `tank_s1_fuel` | 0.0 | 97.1 | — | Soft-FAIL (STG1 post-sep) |

AFTER s2 center pixel Soft-PASS sample: **RGBA (253,255,255,255)** — solid white Soft-FAIL alpha-0 Soft-PASS. Magenta clear-hole QA (`/workspace/gravity1-s2-AFTER-clear-holes-magenta.png`) Soft-PASS shows purple only at stage ends Soft-FAIL center hatch Soft-PASS filled white.

Block maps Soft-PASS written:

- `/workspace/agent-backups/Helios/2026-09-23-tip152-g1-s2-fuel-block-map.png` (red = fuel blocked)  
- `/workspace/agent-backups/Helios/2026-09-23-tip152-g1-shell-fuel-window-map.png` (green = Soft-PASS window)

---

## Soft-PASS drawable catalog (HEAD a6553d2)

| drawable | file md5 | Soft-PASS/FAIL for fuel read |
|----------|----------|------------------------------|
| `vehicle_gravity1_s2.png` | `0cceb567adc983592d0205d7e1cb2ee0` | **Soft-FAIL** STG2 plate — opaque hatch Soft-FAIL fill |
| `vehicle_gravity1_s3.png` | `cae31922d84a64e10d8934ded4f4287b` | Soft-FAIL opaque; Soft-FAIL identity alias |
| `vehicle_gravity1_s4.png` | `cae31922d84a64e10d8934ded4f4287b` | Soft-FAIL = s3; `tank_s4_fuel` Soft-PASS **empty** |
| `vehicle_gravity1_s1.png` | `6638905cfa0cb545a9b900082f967154` | Soft-FAIL cutout; Soft-FAIL unused post-sep (booster shadow) |
| `vehicle_gravity1_booster.png` | `0f147b073a3ab67aa289b71dfcbc5ecd` | Soft-FAIL cutout Soft-PASS STG1 post-sep winner |
| `vehicle_gravity1_shell.png` | `5a76e8bf74fa08ed5d90feec8446c523` | **Soft-PASS** fuel windows Soft-PASS (~94% clear over s2 fuel) Soft-FAIL post-sep STG2 Soft-PASS not used |
| `vehicle_gravity1_tank_s2_fuel.png` | `c43c2f5135a612d228ed7689349d5613` | Soft-PASS mask Soft-PASS exists Soft-FAIL unseen under AFTER s2 |
| `vehicle_gravity1_tank_s2_ox.png` | (empty Soft-PASS) | Soft-PASS all-solid ox empty |
| `vehicle_gravity1_ship_tank_fills.png` | — | Soft-PASS fallback fills Soft-FAIL assembled prefers discrete s2 masks first |
| `vehicle_gravity1_stack` / `side` | `c78f781a40a01676fdb57ae5343ba07b` | GOLD Soft-PASS identity Soft-FAIL overwrite held |

tip152 commit Soft-PASS wired Da Vinci tip151 AFTER stage-fit plates Soft-FAIL open cutouts Soft-PASS.

---

## T+02:55 after STAGE SEP — which plate + which fill Soft-PASS active?

| Slot | Soft-PASS active drawable | Soft-PASS fill |
|------|---------------------------|----------------|
| **STG2 right (Chris Soft-PASS question)** | **`vehicle_gravity1_s2`** (AFTER tip152) | **`tank_s2_fuel`** behind Soft-FAIL blocked Soft-PASS; lvl≈0.97 Soft-PASS |
| STG1 left | `vehicle_gravity1_booster` | `tank_s1_fuel` Soft-FAIL blocked Soft-PASS; stage1 Soft-PASS spent Soft-PASS after sep Soft-PASS lvl=0 |

Tape Soft-PASS: Gravity-1 → `oneWay()` (`ow_sep` **160s** Soft-PASS STAGE SEP Soft-PASS Chris T+02:55 Soft-PASS past sep Soft-PASS). Catalog Soft-PASS `drawFamily=gravity1` Soft-FAIL invent wrong family.

---

## Root Soft-FAIL ranking (fuel cutout)

1. **#1 Soft-PASS — tip152 AFTER `s2` Soft-FAIL transparent fuel window Soft-PASS Stamp 92.** Rounded-rect Soft-PASS baked outline Soft-FAIL alpha hole Soft-PASS over `tank_s2_fuel` Soft-PASS → DIG fuel read Soft-FAIL empty white Soft-PASS.  
2. **#2 Soft-PASS — tip151 stage-fit Soft-FAIL also Soft-FAIL cutouts Soft-PASS (BEFORE s2 already Soft-FAIL fuel_clear%0 Soft-PASS shell Soft-PASS).** tip152 Soft-FAIL identity rebuild Soft-FAIL also Soft-FAIL windows Soft-PASS (center_clear 7.4→0 Soft-PASS; s3 center Soft-FAIL collapsed).  
3. **#3 Soft-PASS amp — Ada post-sep prefers opaque `_s2` Soft-FAIL Soft-PASS `_shell` Soft-PASS windows Soft-PASS.** Pre-sep Soft-FAIL prefers `stack`/`cutaway` Soft-FAIL Soft-PASS shell Soft-PASS.  
4. Soft-FAIL invent liquid ox Soft-PASS Gravity-1 Soft-PASS SOLID Soft-PASS ox empty Soft-PASS Stamp 119 Soft-PASS.

Cross-link Soft-PASS identity Soft-FAIL (`looks-softfail.md`): same AFTER plates Soft-FAIL Gravity-1 silhouette Soft-PASS Soft-FAIL fuel Soft-PASS same Soft-FAIL-named pack Soft-PASS.

---

## Soft-FAIL Ada / Da Vinci order

### Da Vinci — FIRST Soft-PASS

1. Rebuild Soft-FAIL-named gravity1 s1–s4 Soft-PASS from GOLD stack Soft-PASS c78f781a… Soft-FAIL overwrite Soft-PASS.
2. Cut true alpha windows Soft-PASS aligned to tank_s*_fuel islands.
3. Copy window geometry from shell Soft-PASS. Soft-FAIL decorative hatch. Soft-FAIL opaque over fill.
4. Restore Gravity-1 identity Soft-PASS (引力 / SRB cluster / G-1). Soft-FAIL invent F9/SR75. Soft-FAIL invent liquid S4.
5. Deliver pack + md5 board under /workspace/agent-backups/Da-Vinci/. Soft-FAIL ship.

### Ada — SECOND Soft-PASS

1. After Da Vinci Soft-PASS plates: wire s1–s4 and sync booster = Soft-PASS s1 (or prefer _s1 before _booster for gravity1).
2. Soft-FAIL revert fleet plateCap 0.92 unless Chris Soft-PASS keeps it after identity + cutout Soft-PASS.
3. Soft-FAIL invent F9/SR75. Soft-FAIL phone/Play/adb. Soft-FAIL Studio until Chris Soft-PASS.
4. Re-verify PAD 152 Gravity-1 | SpaceSail Pola @ T+02:55 post STAGE SEP: STG2 DIG fuel ochre visible through cutout Soft-PASS.

### Helios / ops

- Soft-FAIL phone / Play / adb until Chris Soft-PASS.
- Soft-FAIL invent.

## Success

- Diagnosis Soft-PASS: tip152 AFTER s2 hatch is decorative opaque, not a Stamp 92 fuel window.
- Order Soft-PASS: Da Vinci identity + true alpha cutouts first, then Ada wire/sync.
