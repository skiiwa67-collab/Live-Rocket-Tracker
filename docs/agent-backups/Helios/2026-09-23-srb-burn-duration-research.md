# SRB burn duration research — LRT fleet
**Date:** 2026-09-23 (America/Chicago)  
**Scope:** Published solid rocket booster (SRB) burn durations for rockets in Chris Olsen's Live Rocket Tracker fleet that use solid boosters. Public sources only. Soft-FAIL inventing times.

---

## Per rocket

### 1) Long March 8 / Long March 8A (CZ-8 / CZ-8A) — CASC / CALT

- **Has SRBs?:** **NO**
- **Booster designation / count:** 0 or 2 × **K2** liquid strap-ons (each 1× YF-100, RP-1/LOX). CZ-8A keeps the same liquid first stage + optional liquid K2 boosters; upgrades are hydrolox second stage (YF-75H) and larger fairing option.
- **Burn duration (s):** **NOT APPLICABLE — not solid.** Liquid K2 burn published as **174 s** (Chinese Wikipedia encyclopedia table; not an SRB).
- **Source:**
  - EN Wikipedia Long March 8: boosters powered by YF-100, propellant RP-1/LOX — https://en.wikipedia.org/wiki/Long_March_8  
    Quote (booster table): "Powered by 1 × YF-100" / "Propellant RP-1 / LOX"
  - ZH Wikipedia 长征八号: K2 推进时间 **174秒**, 燃料 煤油/液氧 — https://zh.wikipedia.org/wiki/长征八号  
    Quote: "推进时间 174秒" / "燃料 煤油 / 液氧"
- **Confidence:** **high** (not solid — well established)
- **Proxy if missing:** For a *Chinese solid strap-on* burn bar in LRT, use **CZ-6A FG-112 ≈ 114.5 s**. Do **not** drive an SRB progress bar from the 174 s liquid number.

---

### 2) Long March 6A (CZ-6A) — SAST / CASC (tip139 art pack)

- **Has SRBs?:** **YES**
- **Booster designation / count:** **4 × FG-112** solid strap-ons (~2.0 m dia × ~15.1 m; HTPB solid). Core remains liquid (2× YF-100). Distinguish: strap-on solids only — core is **not** solid.
- **Burn duration (s):** **114.5** (booster burn); separation **117.5** (maiden-flight timeline)
- **Source:** NASASpaceflight, Adrian Beil, 2022-03-29 — https://www.nasaspaceflight.com/2022/03/cz-6a-debut/  
  Quote: *"The boosters burned for 114.5 seconds and separated from the rocket at 117.5 seconds into the flight."*  
  Type: **inferred from launch / flight timeline** (NSF report of debut ascent profile; not a stamped CASC datasheet).  
  EN Wikipedia lists FG-112 thrust/size/propellant but **does not** publish burn seconds — https://en.wikipedia.org/wiki/Long_March_6A  
  Official CASC/SAST FG-112 burn-time datasheet: **NOT PUBLICLY FOUND**.
- **Confidence:** **high** for flight-observed ~115 s class; **medium** if a stamped agency spec is required
- **Proxy if missing:** N/A — published flight number exists. Closest class peer if needed: JAXA **SRB-3 ~105–110 s** (similar ~2.5 m solid strap-on class).

---

### 3) Nuri / KSLV-2 (Korea — KARI)

- **Has SRBs?:** **NO** — confirmed no solid strap-ons
- **Booster designation / count:** None. Three liquid stages: S1 4× KRE-075, S2 1× KRE-075 vac, S3 1× KRE-007 (Jet A-1 / LOX).
- **Burn duration (s):** **NOT APPLICABLE — no solids.** (Liquid S1 burn commonly cited **127 s** — not SRB.)
- **Source:**
  - Everyday Astronaut Nuri Test Flight: https://everydayastronaut.com/test-flight-nuri/  
    Quote: *"The rocket’s first stage uses four KRE-075 sea level engines… The stage burns for 127 seconds and uses Jet A-1 and Liquid Oxygen (LOx) propellant."*
  - Wikipedia / Gunter / SpaceNews coverage consistently describe all-liquid stack with no strap-ons.
- **Confidence:** **high**
- **Proxy if missing:** None for SRB bar. Soft-FAIL solid burn UI on Nuri. (Future GEO-KSLV concepts with liquid side boosters are not current Nuri / KSLV-2.)

---

### 4) H3 / H3-24 (Japan JAXA / MHI) — SRB-3 (tip139)

- **Has SRBs?:** **YES** (config-dependent: 0, 2, or 4)
- **Booster designation / count:** **SRB-3**; H3-24 = 2 LE-9 + **4 SRB-3**; H3-22 = 2 LE-9 + 2 SRB-3; H3-30 = 3 LE-9 + 0 SRB-3
- **Burn duration (s):**
  - **~105** — JAXA design/plan table (engine page)
  - **~110** — JAXA SRB-3 product page table (全燃焼時間 約110s)
  - **110.1** — JAXA ground firing test (design validation), 2018-08-26
  - **105** — Everyday Astronaut encyclopedia for flight articles
  - Qualification ground test reported **107.5 s** (Tokyo Tobimono / JAXA qual coverage)
- **Sources:**
  - JAXA engine page: https://www.rocket.jaxa.jp/rocket/engine/srb3/ — table "全燃焼時間 **約105s**" (SRB-3 計画)
  - JAXA H3 SRB-3 page: https://www.rocket.jaxa.jp/rocket/h3/srb3.html — table "全燃焼時間 **約110s**"
  - JAXA press (ground test): https://global.jaxa.jp/press/2018/08/20180826_srb3.html — *"Firing Duration: **110.1 sec**"*
  - Everyday Astronaut VEP 4 / H3-22: https://everydayastronaut.com/vep-4-h3-22-2/ — *"burns for **105 seconds**"*
- **Type:** **official agency spec** (≈105–110 s band) + **measured ground-test** (110.1 s). Flight separation often called slightly after burnout (mission timelines ~T+1:56 / ~116 s) — separation ≠ burn.
- **Confidence:** **high**
- **Proxy if missing:** N/A. Recommended LRT calibration: **110 s** (later JAXA table + ground test) or **105 s** (plan / Everyday Astronaut). Soft-FAIL inventing outside ~105–110 band.

---

### 5) Other Chinese Long March variants with solids commonly in LRT — brief notes

#### Long March 11 (CZ-11 / CZ-11H)
- **Has SRBs (strap-ons)?:** **NO**
- **Solids?:** **YES — entire vehicle is 4-stage all-solid** (core motors, not strap-ons). Stage 1 = **P35**.
- **Burn duration (s):** Stage 1 P35 **71** (Wikipedia encyclopedia). Stages 2–4 burn times: **NOT PUBLICLY FOUND** in the same table.
- **Source:** https://en.wikipedia.org/wiki/Long_March_11 — First stage P35: *"Burn time **71 seconds**"* / *"Maximum thrust 1,200 kN"*
- **Confidence:** **medium** (encyclopedia; not stamped CASC datasheet for all stages)
- **Note for LRT:** Do not treat as strap-on SRB bar; if modeling stage burn, use P35 **71 s** as S1 only.

#### Long March 5 (CZ-5 / CZ-5B)
- **Has SRBs?:** **NO**
- **Boosters:** 4 × liquid **CZ-5-300** (each 2× YF-100, RP-1/LOX)
- **Burn duration (s):** Liquid booster burn commonly **~173 s** (Wikipedia / mission timelines). **Not solid.**
- Soft-FAIL SRB bar.

#### Long March 6 / 6C, Long March 7 / 7A, Long March 2/3/4 families
- **CZ-6 / 6C:** liquid core only (no solids on 6C; 6A is the solid-strap-on variant).
- **CZ-7 / 7A:** liquid YF-100 strap-ons (not solid).
- **CZ-2/3/4 classic:** hypergolic liquid strap-ons where used (not solid).
- Among current Long March orbital fleet, **CZ-6A is the primary (and essentially only) hybrid with solid strap-on boosters.** All-solid small lifters (CZ-11, and commercial solids like Kinetica/Ceres/etc. if in LRT) are different UI cases.

---

### 6) SR-75 Flight 2 (HyImpulse)

- **Has SRBs?:** **NO**
- **Propulsion:** Single-stage **hybrid** — solid **paraffin** fuel + liquid **LOX** (HyPLOX-75), **not** a solid rocket booster / SRB.
- **Burn duration (s):** Flight-2 published SRB time: **N/A**. Hybrid motor planned / paper burn class often **~70–72 s** (HyImpulse / SmallSat / EUCASS literature for HyPLOX75 / SL1 stage motors) — **not** an SRB spec; treat as hybrid motor duration if ever needed.
- **Source:** HyImpulse SR75 page — https://hyimpulse.de/sr75 — *"hybrid rocket motor uses paraffin and liquid oxygen"* / *"thrust of 75kN"*
- **Confidence:** **high** that it is **not** an SRB
- **Proxy if missing:** Soft-FAIL solid burn bar. Closest *solid* peer for UI testing only: small solid stages (e.g. CZ-11 P35 **71 s**) are coincidentally similar length — do not claim they are the same motor class.

---

## Summary table

| rocket | burn_s | source_quality | proxy_if_missing |
| --- | --- | --- | --- |
| CZ-8 / CZ-8A | N/A (no SRBs; liquid K2 **174**) | high | Use CZ-6A **114.5** for Chinese solid strap-on bar |
| CZ-6A (FG-112 ×4) | **114.5** (sep 117.5) | high (NSF flight timeline); medium for agency datasheet | SRB-3 ~105–110 if needed |
| Nuri / KSLV-2 | N/A (no SRBs) | high | none — Soft-FAIL SRB UI |
| H3 SRB-3 (0/2/4) | **~105–110** (plan/table); **110.1** ground test | high (JAXA official) | n/a |
| CZ-11 (all-solid S1 P35) | **71** (S1 only; not strap-on) | medium (Wikipedia) | S2–S4 NOT PUBLICLY FOUND |
| CZ-5 | N/A (liquid boosters ~173) | high | none for SRB |
| SR-75 F2 | N/A (hybrid paraffin/LOX) | high | Soft-FAIL SRB; hybrid ~70–72 class if needed |

---

## Critical caveats

1. **Soft-FAIL inventing.** Where no solid burn is published, the table says N/A or NOT PUBLICLY FOUND — do not fabricate.
2. **Strap-on vs core solid:** CZ-6A / H3 use strap-on solids on a liquid core. CZ-11 is all-solid stages (no strap-ons). CZ-8/8A/5/7 and Nuri liquid strap-ons/cores must not drive SRB bars.
3. **Number type differs:** H3 = official JAXA spec + ground test; CZ-6A = NSF-reported **flight timeline** (best public number; CASC datasheet not found); CZ-11 S1 = encyclopedia.
4. **Burn ≠ separation:** CZ-6A burn 114.5 vs sep 117.5; H3 SRB-3 burnout ~105–110 vs separation often ~T+116. Prefer **burn** for progress-bar end state.
5. **Recommended Da Vinci / LRT SRB calibrations:** **CZ-6A = 114.5 s** (or round 115); **H3 SRB-3 = 110 s** (or 105). Soft-FAIL CZ-8A / Nuri / SR-75 / CZ-5 as solid.

---

*Helios research — 2026-09-23 CT — public sources only*
