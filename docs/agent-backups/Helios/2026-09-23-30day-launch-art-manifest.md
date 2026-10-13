# LRT art queue — 30-day launch manifest

**Owner:** Chris Olsen  
**Window:** 2026-09-23 00:00 CT through 2026-10-23 00:00 CT (30 days)  
**Built:** 2026-09-23 ~07:16 CT  
**Rule:** Soft PASS published NETs only. Soft FAIL invent NETs / invent clocks on Day-precision rows.  
**Priority buckets:** 48h / 7d / 30d from build time (~07:16 CT).

## Summary numbers (Soft PASS)

| Metric | Count |
|--------|------:|
| Total launches (LL2 published NET in window) | **13** |
| Covered (hud-assets vehicle art present) | **11** |
| Missing (no vehicle_* art / no catalog id) | **2** |
| Soft FAIL excluded (month/year-only NET) | see Soft FAIL table (not in totals) |

### Covered / missing roll-up

| Class | Count | Vehicles / missions |
|-------|------:|---------------------|
| Covered | 11 | CZ-8A; CZ-6A (GOLD READY); Electron StriX-13; F9 USSF-385; Starship Flight 14; F9 Crew-13; F9 Transporter 18; Ariane 64 LE-04; F9 Starlink 15-25; GSLV Mk II IRNSS-1L; H3-24 MMX |
| Missing | 2 | **HyImpulse SR75 Flight 2**; **Nuri / KSLV-2 NeonSat-2 to 6** (Chris alias: Niri) |

### Per-missing ETA

| Missing vehicle | Mission | NET (published) | ETA to GOLD pack | Notes |
|-----------------|---------|-----------------|------------------|-------|
| SR75 | Flight 2 | 2026-10-04 (day) CT | ~2–3 h after kick | No `vehicle_sr75*` in hud-assets / RCC / LRT. No catalog id. Soft FAIL invent clock (LL2 Day filler). |
| Nuri / KSLV-2 | NeonSat-2 to 6 | 2026-10-06 22:23 CT | ~3–4 h GOLD | No `vehicle_nuri*` / `vehicle_kslv*`. No catalog id. Chris said **Niri**. |

### Priority buckets (from build ~07:16 CT)

| Bucket | Window end (approx) | Launches in bucket | Missing in bucket |
|--------|---------------------|-------------------:|------------------:|
| **48h** | 2026-09-25 ~07:16 CT | 2 | **0** |
| **7d** | 2026-09-30 ~07:16 CT | 3 | **0** |
| **30d** | 2026-10-23 00:00 CT | 8 | **2** (SR75, Nuri) |

## Sources (PASS only — no invented NETs)

| Source | Result |
|--------|--------|
| Launch Library 2 upcoming API (`net` 2026-09-23T05:00Z → 2026-10-23T05:00Z) | **13** launches with published NET in window |
| Next Spaceflight / prior Helios 13-day + LRT HORIZON | Cross-check; month/year-only cards Soft FAIL (excluded from dated totals) |
| VehicleCatalog — RetroCommandCenter (`app/.../VehicleCatalog.kt`) | Primary art-id map |
| VehicleCatalog — Live Rocket Tracker | Leaner set; noted where it differs |
| hud-assets `/workspace/hud-assets/` | Primary art coverage (mirrored into RCC / LRT drawables where present) |
| CZ-6A pack | **GOLD Soft-PASS READY** (`READY_cz6a.md`, stack md5 `17ad81c62235dd204a535e6f2519e50e`) |

## FLAG — CZ-6A (48h, art DONE)

| Field | Value |
|-------|--------|
| Vehicle | Long March 6A (CZ-6A) |
| Mission | Unknown Payload |
| Provider | CASC |
| NET | **2026-09-24 03:45 CT** (2026-09-24 08:45 UTC) |
| Status | Go for Launch |
| Precision | Hour (LL2) |
| Art | **COVERED — GOLD Soft-PASS READY** |
| Catalog | Ada-pending: dedicated `cz6a` still **MISSING** from VehicleCatalog.kt (RCC falls back to `lm`). Do not invent the wire. |

## Main table (30-day Soft PASS)

| Date CT | Vehicle | Mission | Provider | Catalog (RCC) | hud-assets | Priority bucket | Art status |
|---------|---------|---------|----------|---------------|------------|-----------------|------------|
| 2026-09-23 08:30 CT | Long March 8A | Unknown Payload | CASC | yes — `cz8a` | `vehicle_cz8a_*` | **48h** | COVERED — GOLD Soft-PASS (hands-off) |
| 2026-09-24 03:45 CT | Long March 6A | Unknown Payload | CASC | `lm` only (no `cz6a` yet) | `vehicle_cz6a_*` | **48h** | COVERED — **GOLD READY** |
| 2026-09-25 19:15 CT | Electron | Owlright… (StriX Launch 13) | Rocket Lab | yes — `electron` | `vehicle_electron_*` | **7d** | COVERED |
| 2026-09-26 06:56 CT | Falcon 9 Block 5 | USSF-385 | SpaceX | yes — `f9` | `vehicle_f9_*` | **7d** | COVERED Soft-PASS F9 |
| 2026-09-28 07:15 CT | Starship V3 | Starlink Group 31-1 (Flight 14) | SpaceX | yes — `starship` | `vehicle_starship_*` | **7d** | COVERED — GOLD reference (hands-off) |
| 2026-10-01 10:10 CT | Falcon 9 Block 5 | Crew-13 | SpaceX | yes — `f9` | `vehicle_f9_*` | **30d** | COVERED Soft-PASS F9 |
| 2026-10-01 13:08 CT | Falcon 9 Block 5 | Transporter 18 | SpaceX | yes — `f9` | `vehicle_f9_*` | **30d** | COVERED Soft-PASS F9 |
| 2026-10-04 (day) CT | **SR75** | **Flight 2** | **HyImpulse** | **no** | **none** | **30d** | **MISSING — NEEDS BUILD** |
| NET 2026-10-07 (day) | Ariane 64 Block 2 | Amazon Leo (LE-04) | Arianespace | yes — `ariane` | `vehicle_ariane_*` | **30d** | COVERED (PARTIAL pack; Soft FAIL invent clock — Day precision) |
| 2026-10-06 22:23 CT | **Nuri / KSLV-2** | **NeonSat-2 to 6** | **KARI** | **no** | **none** | **30d** | **MISSING — NEEDS BUILD** (Chris: Niri) |
| 2026-10-10 11:00 CT | Falcon 9 Block 5 | Starlink Group 15-25 | SpaceX | yes — `f9` | `vehicle_f9_*` | **30d** | COVERED Soft-PASS F9 |
| 2026-10-14 (day) CT | GSLV Mk II | IRNSS-1L (NVS-03) | ISRO | yes — `gslv` (LRT: `gslv2`) | `vehicle_gslv_*` | **30d** | COVERED (Soft FAIL invent clock — Day precision) |
| 2026-10-19 14:41 CT | H3-24 | Martian Moon eXplorer (MMX) | MHI / JAXA mission | yes — `h3` | `vehicle_h3_*` | **30d** | COVERED |

### Explicit required rows (confirmed)

1. **SR75 Flight 2 (HyImpulse)** — in main table; Day-precision NET; **MISSING** art + catalog.  
2. **Nuri / KSLV-2 (KARI)** — NeonSat-2 to 6; Minute-precision Go; **MISSING** art + catalog. Chris alias **Niri**.

### Bucket detail

**48h (2 launches, 0 missing)**  
- Long March 8A — COVERED GOLD.  
- Long March 6A — COVERED GOLD READY (catalog wire still Ada).

**7d (3 launches, 0 missing)**  
- Electron StriX-13; F9 USSF-385; Starship Flight 14.

**30d (8 launches, 2 missing)**  
- Covered: Crew-13, Transporter 18, Ariane LE-04, Starlink 15-25, GSLV IRNSS-1L, H3 MMX.  
- Missing: SR75 Flight 2; Nuri / KSLV-2.

## Soft FAIL rows (excluded from dated Soft PASS totals)

Do not invent day/clock. NSF / prior Helios listed these without a firm day inside the Soft PASS table:

| Vehicle | Mission | Provider | Published NET quality | Why Soft FAIL |
|---------|---------|----------|----------------------|---------------|
| Vulcan VC6L | Amazon Leo (LV-01) | ULA | NET October 2026 | month-only |
| Electron/Curie | BlackSky Gen-3 Mission 6 | Rocket Lab | NET 2026 | year-only |
| Long March 12A | Flight 2 | CASC | NET October 2026 | month-only |
| Electron/Photon | LOXSAT 1 | Rocket Lab | NET 2026 / TBD cards | no firm day in LL2 30d query |
| Falcon 9 Block 5 | CRS SpX-35 | SpaceX | NET October 2026 | month-only |
| Falcon 9 Block 5 | Tranche 1 Transport Layer A | SpaceX | NET October 2026 | month-only |
| Chang'e 7 / LM-5 | (TBD cards) | CASC | NET TBD | Soft FAIL invent NET |
| Firefly Alpha | QuickSounder (TBD) | Firefly | NET TBD | Soft FAIL invent NET |

**Day-precision Soft FAIL clocks (still counted in Soft PASS totals by day):** SR75 Flight 2; Ariane 64 LE-04; GSLV IRNSS-1L — include by published day only; Soft FAIL any guessed T-0 clock.

## Art-queue priority order (30d)

1. **CZ-6A** — GOLD READY; Ada VehicleSpec `cz6a` still pending (not an art rebuild).  
2. No other 48h / 7d MISSING vehicles.  
3. **SR75 Flight 2** — first MISSING by NET (~Oct 4 day); ETA ~2–3 h.  
4. **Nuri / KSLV-2 (Niri)** — MISSING; firm Minute NET Oct 6 22:23 CT; ETA ~3–4 h.  
5. Optional GOLD upgrades (not MISSING): Electron / Ariane / GSLV / H3 PARTIAL or FLOOR-era packs if Helios Soft-FAILS quality — only after MISSING builds.

## Catalog × assets index (horizon-relevant)

| Vehicle | RCC catalog id | LRT catalog id | hud-assets prefix | Notes |
|---------|----------------|----------------|-------------------|-------|
| CZ-8A | `cz8a` | falls back `lm` | `vehicle_cz8a_*` | GOLD Soft-PASS |
| CZ-6A | MISSING → `lm` | MISSING → `lm` | `vehicle_cz6a_*` | GOLD READY; Ada wire pending |
| Electron | `electron` | `electron` | `vehicle_electron_*` | COVERED |
| Falcon 9 | `f9` | `f9` | `vehicle_f9_*` | Soft-PASS family (LRT drawable copy may lag; hud-assets + RCC present) |
| Starship | `starship` | `starship` | `vehicle_starship_*` | GOLD reference |
| SR75 | — | — | — | **MISSING** |
| Ariane 64 | `ariane` | `ariane` | `vehicle_ariane_*` | PARTIAL pack |
| Nuri / KSLV-2 | — | — | — | **MISSING** (Chris: Niri) |
| GSLV Mk II | `gslv` | `gslv2` | `vehicle_gslv_*` | COVERED |
| H3-24 | `h3` | `h3` | `vehicle_h3_*` | COVERED |

RCC catalog ids present: `starship`, `fh`, `f9`, `glenn`, `vulcan`, `sls`, `soyuz`, `electron`, `ariane`, `lm5`, `cz2d`, `cz8a`, `kz11`, `cz12`, `lm`, `lvm3`, `pslv`, `gslv`, `isro`, `zq3`, `h3`, `atlas`, `firefly`, `kinetica1`, `pallas1`, `gravity1`, `proton`, `generic`.  
**Not present:** `cz6a`, `sr75`, `nuri` / `kslv2`.

## Providers in Soft PASS window

SpaceX, CASC/CNSA family, Rocket Lab, HyImpulse, Arianespace, KARI, ISRO, MHI/JAXA (H3 MMX).  
ULA / CNSA month-only cards Soft FAIL (see Soft FAIL table). No published LL2 NET in this window for ULA Vulcan or additional CNSA beyond CZ-8A / CZ-6A.

## Verify stamps

- LL2 query count in window: **13**  
- Soft PASS totals: **13 total / 11 covered / 2 missing**  
- CZ-6A GOLD READY proof: `/workspace/hud-assets/READY_cz6a.md`, `_proof_cz6a.png`  
- Prior refs: `/workspace/agent-backups/Helios/2026-09-23-13day-launch-art-manifest.md`, `/workspace/lrt-horizon-20260923/HORIZON.md`  
- No invented NETs in Soft PASS clock columns; Day rows Soft FAIL guessed T-0  
- Local only — not pushed to device; no git commit required for this report  

