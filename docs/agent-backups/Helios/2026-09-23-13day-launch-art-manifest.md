# LRT art queue — 13-day launch manifest

**Owner:** Chris Olsen  
**Window:** 2026-09-23 00:00 CT through 2026-10-06 00:00 CT (13 days)  
**Built:** 2026-09-23 ~07:12 CT  
**Priority rule:** CZ-6A (~20h) first, then other launches inside 48h, then remainder of 13d.

## Sources (PASS only — no invented NETs)

| Source | Result |
|--------|--------|
| Next Spaceflight upcoming / detail pages | Confirmed CZ-8A, CZ-6A, USSF-385, Starship Flight 14, Crew-13; several month/year NET cards Soft FAIL (excluded from table) |
| Launch Library 2 upcoming API (`net` 2026-09-23 → 2026-10-06) | 8 launches with published NET in window |
| Local `/workspace` LRT / ll2 caches | No fresh launch-cache JSON found; `ll2-agent-plugin` skill docs only |
| VehicleCatalog — RetroCommandCenter (primary for art ids) | Used for catalog yes/no |
| VehicleCatalog — Live Rocket Tracker | Leaner set; noted where it differs |

## FLAG — CZ-6A NET

| Field | Value |
|-------|--------|
| Vehicle | Long March 6A (CZ-6A) |
| Mission | Unknown Payload |
| Provider | CASC |
| NET | **2026-09-24 03:45 CT** (2026-09-24 08:45 UTC) |
| Window | 08:38–09:04 UTC |
| Status | Go for Launch |
| Precision | Hour (LL2 + NSF) |
| ~T-0 from build | ~20.5 h |
| Priority bucket | **20h** |
| Catalog | yes — `lm` only (generic Long March catch-all). **No dedicated `cz6a` art id** in RCC or LRT. |

## Main table

| Date CT | Vehicle | Mission | Provider | Already in catalog (RCC art id) | Priority bucket |
|---------|---------|---------|----------|----------------------------------|-----------------|
| 2026-09-23 08:30 CT | Long March 8A | Unknown Payload | CASC | yes — `cz8a` (LRT falls back to `lm`) | 20h |
| 2026-09-24 03:45 CT | Long March 6A | Unknown Payload | CASC | yes — `lm` only (no dedicated `cz6a`) **FLAG** | 20h |
| 2026-09-25 19:15 CT | Electron | Owlright, Owlright, Owlright (StriX Launch 13) | Rocket Lab | yes — `electron` | 13d |
| 2026-09-26 06:56 CT | Falcon 9 Block 5 | USSF-385 | SpaceX | yes — `f9` | 13d |
| 2026-09-28 07:15 CT | Starship V3 | Starlink Group 31-1 (Starship Flight 14) | SpaceX | yes — `starship` | 13d |
| 2026-10-01 10:10 CT | Falcon 9 Block 5 | Crew-13 | SpaceX | yes — `f9` | 13d |
| 2026-10-01 13:08 CT | Falcon 9 Block 5 | Transporter 18 (Dedicated SSO Rideshare) | SpaceX | yes — `f9` | 13d |
| 2026-10-04 (day) CT | SR75 | Flight 2 | HyImpulse | no | 13d |

### Bucket notes

- **20h:** Long March 8A (~1.3 h); Long March 6A (~20.5 h, priority FLAG).
- **48h:** no additional launches with published NET between end of 20h and +48 h from build time.
- **13d:** Electron StriX-13, USSF-385, Starship Flight 14, Crew-13, Transporter 18, SR75 Flight 2.

### Soft FAIL rows (excluded from dated priority work)

Do not invent clock times. NSF listed these without a day inside the window:

| Vehicle | Mission | Provider | Published NET quality | Why Soft FAIL |
|---------|---------|----------|----------------------|---------------|
| Vulcan VC6L | Amazon Leo (LV-01) | ULA | NET October 2026 | month-only |
| Electron/Curie | BlackSky Gen-3 Mission 6 | Rocket Lab | NET 2026 | year-only |
| Long March 12A | Flight 2 | CASC | NET October 2026 | month-only |
| Electron/Photon | LOXSAT 1 | Rocket Lab | NET 2026 | year-only |
| Falcon 9 Block 5 | CRS SpX-35 | SpaceX | NET October 2026 | month-only |
| Falcon 9 Block 5 | Tranche 1 Transport Layer A | SpaceX | NET October 2026 | month-only |

**SR75 Flight 2:** LL2 publishes NET day 2026-10-04 with precision **Day**, status TBD. Included in main table by day only; Soft FAIL any guessed clock (19:00 CT in LL2 is day-precision filler, not a confirmed T-0).

### Outside window (reference only)

| Date CT | Vehicle | Mission | Note |
|---------|---------|---------|------|
| 2026-10-07 03:23 CT | KSLV-2 | NeonSat-2 to 6 & Others | day 14+ |
| NET 2026-10-07 | Ariane 64 | Amazon Leo (LE-04) | NSF NET day; outside 13d |
| 2026-10-19 14:41 CT | H3-24L | MMX | outside 13d |

## Art-queue priority order

1. **CZ-6A** — dedicated art gap (`lm` only). NET ~20.5 h.
2. **CZ-8A** — already `cz8a` in RCC (verify live art pack / LRT sync; LRT catalog still maps to `lm`).
3. No other 48h NETs.
4. Rest of 13d in table order: Electron → F9 USSF-385 → Starship F14 → F9 Crew-13 → F9 Transporter 18 → SR75 (no catalog id).

## Catalog id index used (RCC)

`starship`, `fh`, `f9`, `glenn`, `vulcan`, `sls`, `soyuz`, `electron`, `ariane`, `lm5`, `cz2d`, `cz8a`, `kz11`, `cz12`, `lm`, `lvm3`, `pslv`, `gslv`, `isro`, `zq3`, `h3`, `atlas`, `firefly`, `kinetica1`, `pallas1`, `gravity1`, `proton`, `generic`

RCC-only vs LRT: `cz8a`, `cz2d`, `cz12`, `kinetica1`, `kz11`, `pallas1`, `gravity1`, `pslv`, `gslv` (LRT lacks these dedicated ids).

## Verify stamps

- LL2 query count in window: 8  
- NSF detail cross-check: CZ-8A `/launches/details/8367/`, CZ-6A `/launches/details/8397/`, USSF-385 `/launches/details/8389/`, Starship F14 `/launches/details/8346/`, Crew-13 `/launches/details/6973/`  
- No guessed NETs in main table clock columns except SR75 day-only Soft FAIL note above  
