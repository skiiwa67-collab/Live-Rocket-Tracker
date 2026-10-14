# Cyber phone plan: defensive verification lane (Skippy, 2026-10-14)
Synthetic inputs for our own drill harness only. Specs: bug-out-toolset/docs/SPEC-cyber-attack-v1-perseus.md + PLAN-cyber-phone-side.md (in the repo and /workspace/bugout-sec; both present). Code checked **statically** at one-to-many **b43f53d** (0.6.29). No Gradle run. Cases: `2026-10-14-cyber-verify-drills.json` (same schema as `app/src/test/resources/drills/*.json`; drills that need harness fields not in drills/README carry `knownMismatch` or new keys `gps`, `sos`, `cyberEvidence`, flagged NEW-FIELD).

## Checklist + static result
| # | Check (spec) | Drill | Static result @ b43f53d |
|---|---|---|---|
| 1 | SKIPPY-CYB-1: officials up + crowd-only → max WATCH (PLAN §1c) | SKV-01/02 | **PASS**: LaneConfig.kt:34-38 caps at `crowdOnlyMaxLevelOfficialsUp` (Cyber.kt:16) |
| 2 | Crowd-only never pushes while officials up | SKV-01 | **PASS**: PushGate.mayPush needs official, grid-down, or the tree override (PushGate.kt:31-32) |
| 3 | Corroboration lifts the cap (utility/official/device probe) | SKV-03 | **PASS**: crowdOnly false when another origin is present (LaneConfig.kt:35; Cyber.kt:33) |
| 4 | Grid-down when ALL alerting feeds are unreachable | SKV-04 | **PASS**: FeedHealth.kt:47-50 (after `gridDownEnterMin` 10) |
| 5 | Grid-down when ≥2 feeds are SUSPECT (PLAN §1 `officialsSuspectMinFeeds`=2) | SKV-06 | **FAIL**: no counter; `GridDownArea.markSuspect` has no caller (V0627.kt:16); `isGridDown` flips on any one mark (V0627.kt:23) |
| 6 | One suspect feed doesn't flip | SKV-05 | PASS today, only because nothing marks suspect (see 5) |
| 7 | Flood → burst collapse | SKV-07 | **PASS**: AbuseGuard.independentGroups (AbuseGuard.kt:73-75; params :28-29) |
| 8 | Spread bar ≥3 cells / 1 mi / 10 min (Skippy S1) | SKV-08 | **FAIL**: `cyber.confirmMinCells/CellMi/MinSpanMin` not in code |
| 9 | Spoofed attack all-clear | SKV-09 | **PASS**: only top-level official ends it (AlertThresholds.kt:128) |
| 10 | Symmetric service all-clear (`cyber.allClearRequires`) | SKV-10 | **FAIL**: not found |
| 11 | Bad feed (garbage/503) → "unreachable", never all-clear | SKV-11 | **PASS**: OfficialFeeds.kt:103 labels it; stale rule FeedHealth.kt:31 |
| 12 | GPS spoof detection (`gps.jumpMi` 0.5/5 s, `mismatchMi` 3) + GPS tile | SKV-12 | **FAIL**: no detector; no GPS in `Service` (LaneConfig.kt:24) |
| 13 | Location vs reporter movement | SKV-13 | **PASS** (logic): AbuseGuard.locationPlausible (:66-69), used at V0627.kt:46 |
| 14 | SOS with suspect GPS still sends, marked | SKV-14 | **PASS** (function): Cyber.kt:87-88. **Unknown** whether any caller passes `gpsSuspect=true` (no detector, see 12) |
| 15 | Hospital/911 crowd floor (C6) | SKV-20 | **PASS**: LaneConfig.kt:37 |
| 16 | Privacy kFloor + time buckets | SKV-19 | **PASS**: Cyber.kt:28-29, 49-50; V0627.kt rollUp/timeBucket |

## My earlier 12 cyber-review items (2026-10-14-cyber-design-review.md)
1 panic seeding / spread: **FAIL** (row 8); burst part PASS. · 2 warning-at-2 crowd-only: **PASS** officials-up (row 1). Grid-down still allows crowd WARNING: Chris question stays open. · 3 substation root event: **PARTIAL**: parent→child within lag is handled (V0627.kt:96-107) but child-before-parent counts as cyber evidence (SKV-16). · 4 false all-clear: attack **PASS**, service **FAIL** (rows 9-10). · 5 carrier outage = unknown: **FAIL** (SKV-17, not found). · 6 tracking/privacy: **PASS** (row 16); rotating keys unknown. · 7 mesh flood as counts: **FAIL** (SKV-18, `relayCyberAsCounts` not found). · 8 absolutes/units: cell shown as ~0.6 mi (Cyber.kt:86) PASS; SWPC G4-G5 Param not checked (unknown). · 9 GPS tile: **FAIL** (row 12). · 10 readiness card: not found (unknown/not built). · 11 passive device signals: **PASS** partial: DEVICE_SELF_TEST origin (Cyber.kt:11, 33). · 12 IC3 sourcing: doc item, not code (n/a).

## Summary
- PASS: 11 of 16 checklist rows (one by default only).
- FAIL: 5 (≥2-suspect grid-down, spread bar, symmetric all-clear, GPS detection, mesh-as-counts).
- Life-safety check: no FAIL above makes a real warning go silent. All five are missing guards or missing grid-down entry paths. Row 5 is the most important: today a cyber attack that corrupts feeds without making them unreachable never enters grid-down, so crowd reports stay capped at WATCH with no push.
