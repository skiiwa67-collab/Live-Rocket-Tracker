# PROPOSAL: Bug-Out disaster-type decision logic (Skippy, 2026-10-13), design only
Sources: bugout-sec/DESIGN-disaster-type-inventory.md (inv) §0, §1, §6.1–6.5; DESIGN-disaster-data-priority.md (dp) §1, "REVISED §2 weather promotion", "Weather WATCH area", "GRID-DOWN MODE"; PROGRAM-LOGIC.md core principles + "Authority route default"; SECURITY-NOTES.md §3 + Five-tier §8. Code: one-to-many 5e221be/e82062b (0.6.14). Builds on Perseus's notes (bugout-sec/PROPOSAL-decision-logic-perseus-draft.md) and does not replace Ada's §6 defaults.

## 1. What the code does today (why this is needed)
- Scenario is a fixed enum with the cause baked in: GRID, WATER and EMP are all typed `WAR_ATTACK` (Data.kt:102-104). There is no natural or cyber grid path and no solar-storm path.
- `NUCLEAR("Nuclear (worst case)")` is typed `POWER_PLANT_FAILURE` with radius 10.0 (Data.kt:100), which merges attack and accident, the very pair inv §6.4 says to keep apart.
- Radius choice: `IncidentRadius.pick` (Incident.kt:103-110) puts a cached value ahead of a fresh authority radius, and puts the quake formula ahead of `i.radiusMi`, even though the comment says "authority feed wins over formulas" (Incident.kt:107).
- No weather motion logic exists in the code (no storm speed or heading fields in Incident.kt:34). The dp weather rules are design only.

## 2. Architecture
1. **EFFECT model** (what's broken), per hazard area: `power, comms, roads, air, water, structures, ground`, each with {state OK/DEGRADED/DOWN/UNKNOWN, source, age}. This drives routing, power mode and grid-down (dp GRID-DOWN).
2. **CAUSE model**: a probability vector over {NATURAL, ACCIDENT, CYBER, ATTACK, UNREST, UNKNOWN} plus `causeSource` and an audit trail (inv §6.1, extended with CYBER and UNREST from inv §6.4 rows). The UI keeps Ada's plain words: "Cause: not confirmed".
3. **Playbooks**: one record per (displayGroup × cause), kept in a versioned rules file (`assets/rules/playbooks-vN.json`, signed like any update, SECURITY-NOTES §3 "Unsigned upgrades"). No if/else per type. Every number is a `Param` {default, unit, min, max, adjustBy, label "est."}. Per Chris's rule, min/max *warn*, they don't clamp. (Note: Tunables.kt:14-26 currently clamps with coerceIn, which is a wall.)
4. **Evidence scorer** (inv §6.2 ladder): official declaration (sets the cause; weighted by verified tier per SECURITY-NOTES Five-tier §8, so unverified = "claimed", weight 0) > feed type (SWPC G-scale → NATURAL; NWS/GDACS → NATURAL; NRC/operator → ACCIDENT; CISA/utility cyber notice → CYBER) > co-occurrence (window Param, default 2 h; can raise ATTACK but never set it alone above `attackInferThreshold`, default 0.6) > corroborated ground reports. It outputs P(cause) with hysteresis (Param) and logs "cause updated" in place.
5. **Ambiguity**: `causeUnknownMode` default SHOW_BOTH_PROTECTIVE_FIRST (inv §6.2). It computes a "Do now" set that is safe for both paths (Perseus §3). When paths conflict on *direction*, it shows both, with "follow official orders first".
6. **Person adapter**: location + terrain, nearby hazards (EFFECT), mobility tier, family + pets (Tunables.petsAsTargets), battery and power mode, vehicle + fuel, planned stay-put hours (dp WATCH "Planned stay"). It scales the Params: watch radius, time-to-safety and refresh cadence.
7. **Weather (no fixed numbers)**, from dp:
   - Time-to-route = projected polygon arrival along heading × speed. Promote when it's under the "Weather warning time" Param (start 45 min, est.).
   - Unknown speed → distance fallback Param.
   - Watch radius = stay-put hours × observed storm speed (fallback Param) + travel buffer, displayed out to 75 mi+ where practical. The max *warns*, it doesn't clip. The circle label states the math.
   - Corridor widens by speed × data age.

## 3. Same-effect, different-cause pairs (doc-supported only)
| Pair (inv §6.4) | Shared effect | Natural / accident path | Attack path (attack modifiers inv §6.3) | Cause tips (evidence) | Source status |
|---|---|---|---|---|---|
| Space weather vs EMP | power + electronics down | Stay put, keep usage low, forecastable, recovery focus | Follow-on strikes possible; avoid targets; long outage, no comms; offline maps | SWPC alert + no attack signal → NATURAL; national alert or sudden total loss with no SWPC → ATTACK | EMP UNSOURCED |
| Grid outage: weather vs cyber vs attack | power down | Tied to weather polygon; local restoration | Cyber: regional, water/comms may follow, no route change. Attack: avoid substations | Inside a weather warning → NATURAL; CISA/utility notice → CYBER; DHS/NTAS → ATTACK; else UNKNOWN | Sourced |
| Radiation: plant vs dirty bomb vs detonation | air (radiation) | Plant: EPZ, plant-centered, no hold | RDD: local, crosswind. Detonation: blast first, then go deep; largest radius | NRC/operator → ACCIDENT; blast/flash report → detonation-first | Sourced (Ready.gov) |
| Dam failure | water, roads | Uphill, out of the valley | Same physics + hold + avoid targets; cascade UNSOURCED | NWS dam-failure warning → structural unless DHS/FEMA wording | Sourced |
| Chemical leak | air (heavy gas) | Up and upwind, upper floor | Multiple release points → several plume cones; longer hold | ERG/operator ID → ACCIDENT; terrorism declaration or venue mass casualty → ATTACK | Attack steps UNSOURCED |
| Explosion | structures, fire/air | Single site, smoke/chem branch | Secondary devices; follow law enforcement direction | Official declaration | Secondary-device wording UNSOURCED |
| Water contamination | water | Follow utility orders | Don't use tap at all until told | — | UNSOURCED both |
| Structural collapse | structures, roads | — | Hold + avoid targets | — | UNSOURCED both |
| Pandemic vs bioterror | health | Jurisdiction-wide, no route change | Point source → move away; cover nose/mouth | WHO/CDC → NATURAL; release notice → ATTACK | Sourced |
| Wildfire vs arson | fire, air, roads | Same escape | Multiple fronts; hold only if declared | Official only | Sourced |
| Riot vs coordinated attack | roads, crowds | Avoid crowds | Run / cover per ready.gov/public-spaces | Official / multi-site | Sourced |
| Aircraft crash | structures, fire | — | Hold + avoid targets | Declaration | UNSOURCED both |
**Not supported by the docs (left out, unknown):** earthquake vs blast, and comms down from jamming vs congestion. Neither appears in inv, dp, PROGRAM-LOGIC or SECURITY-NOTES. Add them only after a sourced inventory entry.

**What changes between causes** (inv §6.3): shelter vs move (attack → shelter longer, `attackHoldHours` 24 h start), target avoidance (suggested layer, never auto-routed, list UNSOURCED), comms discipline (expect outage, mesh, pre-cache), trust in authority (verified-only, SECURITY-NOTES §3; an unverified "attack" claim is shown as claimed and can't clear hazards), recovery time (natural = forecast or restoration; attack = hold until official all-clear).

## 4. Schema sketch
```json
{ "rulesVersion": "2026-10-13.1", "schema": 1,
  "params": { "weatherWarnMin": {"default":45,"unit":"min","min":5,"max":180,"adjustBy":["user","situation"],"est":true},
              "assumedStormMph": {"default":30,"unit":"mph","est":true},
              "plannedStayH": {"default":6,"unit":"h","adjustBy":["user","destination"]},
              "attackInferThreshold": {"default":0.6}, "attackHoldHours": {"default":24,"unit":"h"},
              "coOccurWindowH": {"default":2,"unit":"h"} },
  "playbooks": [ {
    "id": "grid.cyber", "displayGroup": "grid_outage", "cause": "CYBER",
    "triggers": [{"input":"effect.power","op":"==","value":"DOWN"}],
    "evidence": [{"signal":"feed.cisa_notice","weight":"p:w.official"},
                 {"signal":"cooccur.multi_utility","weight":"p:w.cooccur","window":"p:coOccurWindowH"}],
    "dataPriority": ["utility_status","cisa","nws_alerts"],
    "routing": {"avoidLayers": [], "changeRoute": false},
    "messaging": {"posture": "conserve", "meshFirst": true},
    "timers": {"hold": null, "refresh": "p:refresh.grid"},
    "steps": {"source": "https://www.ready.gov/cybersecurity", "status": "SOURCED"} } ],
  "unknownMode": "SHOW_BOTH_PROTECTIVE_FIRST" }
```

## 5. Unknowns
Cutoff and hysteresis defaults; whether the safe-for-both set is computed or curated per pair; regional spread of attack priors; sourcing for every UNSOURCED row; jamming and earthquake-vs-blast pairs (not in the docs).
