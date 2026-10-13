# Saxon independent review — 2026-09-23 ~07:19 CT
Chris GO via Helios. HOLD device pushes Soft-PASS held.

## CODE (Ada) — Soft-PASS compile / Soft-FAIL wire gaps

| Item | Verdict | Cite |
|------|---------|------|
| tip138 assembleDebug | Soft-PASS BUILD SUCCESSFUL | `/workspace/LRT-tip138-1.0.128/assemble.log` · APK md5 `e59c8522a65a00af63fb38a5d8d70f5f` |
| Phone | Soft-PASS on 138 / 1.0.128 | Ada audit + Helios jobs |
| tip139+ | Soft-FAIL none queued | `docs/audits/2026-09-23-gold-art-pipeline-audit.md` |
| Kotlin | Soft-PASS warnings only (deprecated insets/scaledDensity) | tip138 assemble.log |
| Device push | HOLD Soft-PASS | HARD — no adb / Studio→phone |
| Untracked art in RCC | Soft-FAIL commit until tip GO | `vehicle_cz6a*` (30) + `vehicle_sr75*` (29) untracked |

## ART vs Starship GOLD bar

| Vehicle | Art on disk | Catalog wire | Saxon Soft-PASS call |
|---------|-------------|--------------|----------------------|
| **CZ-6A** | GOLD Soft-PASS READY stack md5 `17ad81c62235dd204a535e6f2519e50e` | Soft-FAIL `cz6a` MISSING (falls back `lm`) | **Art Soft-PASS** vs GOLD (tanks/SRBs/flames/cutaway Soft-PASS glance `_proof_cz6a.png`). **Live Soft-FAIL** until Ada wires `cz6a` before NET ~Sep 24 03:45 CT |
| **SR-75 F2** | Pack Soft-PASS `/workspace/lrt-sr75-pack/` + hud-assets stack md5 `4cb9b75bac4ea7b8c92166181460718d` · proof Soft-PASS glance | Soft-FAIL no `sr75` VehicleSpec | **Art Soft-PASS prep** (hybrid LOX cyan + paraffin, flame, shell). **Wire Soft-FAIL**. Manifest “MISSING” is Soft-FAIL stale vs disk Soft-PASS |
| **H3-24 MMX** | Partial `vehicle_h3_*` (18) · Soft-FAIL no stack/cutaway/shell GOLD trio | Soft-PASS `id=h3` wired | **Soft-FAIL GOLD** — FLOOR/partial only; Ada Soft-FAIL no GOLD READY pack. Soft-PASS catalog wire only |
| **Nuri / KSLV-2** | Soft-FAIL empty pack (only `build_nuri_gold.py` mid) · no hud-assets | Soft-FAIL no catalog | **Soft-FAIL MISSING** art + wire |

## 30-day coverage Soft-PASS (manifest + disk delta)

Manifest Soft-PASS (Helios 30-day): 13 launches · covered 11 · missing 2 (SR75, Nuri).  
Saxon disk Soft-PASS update: SR75 art now Soft-PASS on disk → **missing art = 1 (Nuri only)** if counts art files; catalog Soft-FAIL still 2 (sr75+nuri) + cz6a wire Soft-FAIL for 48h bird.

48h Soft-PASS risk: **CZ-6A NET tomorrow** — art Soft-PASS, wire Soft-FAIL = GAME-STOPPER if tip not Soft-PASS’d before launch.

## GAME-STOPPERS vs parked

| # | Item | Severity |
|---|------|----------|
| 1 | CZ-6A catalog Soft-FAIL before NET | **GAME-STOPPER (48h)** |
| 2 | Nuri GOLD Soft-FAIL MISSING | delay (~Oct 6) |
| 3 | H3 GOLD Soft-FAIL partial | delay (~Oct 19) |
| 4 | SR-75 catalog Soft-FAIL | delay (~Oct 4) — art Soft-PASS |
| — | FH #46 / CZ-2D #42/#43 | parked Soft-FAIL (prior) |

## Recommended next (no device push)

1. Ada tip Soft-PASS GO: wire `cz6a` + bake drawables → tip139 HOLD push until Chris clears.
2. Da Vinci finish Nuri GOLD Soft-PASS pack; H3 GOLD stack/cutaway/shell Soft-PASS.
3. Ada wire `sr75` after Soft-PASS art md5s locked.
4. Refresh 30-day manifest Soft-PASS counts (SR75 art Soft-PASS landed).

HOLD Soft-PASS: no adb, no Play, no phone install.


---
## Soft-PASS align ~07:20 CT (executor + Ada FYI)

- tip139 **BUILD ONLY Soft-PASS staged**: vc139 / 1.0.129 @ `bd4b4ef` (stamp+docs). Art **HELD** pending Helios PASS of all four GOLD packs. Soft-FAIL my earlier "tip139+ none queued."
- Chris HARD 2026-09-23: tip139 ship Soft-FAIL until **all four** GOLD Soft-PASS (H3, Nuri, SR-75, CZ-6A). Year sweep = tip140+ QUEUE only.
- SR-75 stack Soft-PASS md5 confirm `4cb9b75bac4ea7b8c92166181460718d` (Ada Soft-PASS match).
- tip137–139 assemble Soft-PASS. Phone Soft-PASS remains tip138 under HOLD.
- H3 Soft-FAIL detail: mid-body ~90% transparent → black HUD "propane pill" voids; no GOLD stack/cutaway/shell.
- Wire HOLD. Device HOLD.
