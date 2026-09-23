# tip139 — BUILD ONLY ship-lock (1.0.129)

**Date:** 2026-09-23 CT  
**Stamp:** 139 / 1.0.129 on `stamp-85`  
**Base:** tip138 (pad font + fetch-queue / cold retry)  
**Play:** HOLD  
**Device / Studio push:** HARD HOLD (HARD_DEVICE_PUSH_GO)

## Contents
- Version bump only on tip138 code tiphead.
- **No** new VehicleCatalog / VehicleDraw wires in this tip.
- **No** gold art PNG commit in this tip (Helios review PASS required first).

## Art — Helios PASS vs HELD

| Family | Da Vinci status | Helios PASS? | Pack in tip139? |
|--------|-----------------|--------------|-----------------|
| CZ-6A | READY pack Soft-PASS prep; md5s measured; catalog draft | **NO (review first)** | **HELD** — do not wire until Helios PASS |
| SR-75 Flight 2 | PNGs mirrored; READY/ADA_VEHICLESPEC Soft-FAIL incomplete | **NO** | **HELD** |
| Nuri / KSLV-2 | Soft-FAIL missing (empty pack) | **NO** | **HELD** |
| H3-24 MMX | Soft-FAIL partial (no stack/cutaway/shell GOLD) | **NO** | **HELD** |
| Other 30-day GOLD | none Soft-PASS’d with Helios PASS at cutoff | **NO** | **HELD** |

**Art passed into tip139:** none.  
**Art held back:** CZ-6A, SR-75, Nuri, H3-24 (and any other 30-day until Helios PASS).

## Staged APK
`/workspace/LRT-tip139-1.0.129/LiveRocketTracker-tip139-1.0.129-debug.apk`  
(build-only — Soft-FAIL adb / Soft-FAIL Studio→phone)

## Next
Helios PASS gold → Ada tip140+ wire+pack. Chris go before any device install.
