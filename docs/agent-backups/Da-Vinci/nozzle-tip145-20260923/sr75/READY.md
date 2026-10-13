# SR-75 tip145 — Soft-PASS upper-alias eng+flame READY

**When:** 2026-09-23 CT
**Lane:** Da Vinci art only. Soft-FAIL Play/device/stack overwrite.

## Note (published geometry)
Published SR-75 is **single-stage HyPLOX**. `ship` layer remains fairing-only (untouched).
`engine_ship` + `flame_ship` are Soft-PASS **Ada post-sep / upper aliases** per Helios tip145 steering —
Soft-FAIL treat as real published S2.

## Soft-PASS writes
| file | md5 | note |
|------|-----|------|
| `vehicle_sr75_engine_ship.png` | `84b24c7e3f9c7396ac35172dd57aa385` | NEW Starship-bar under fairing base |
| `vehicle_sr75_flame_ship.png` | `09075b3f383e2292f7334864438fd9fa` | NEW seal gap=-6 |
| `vehicle_sr75_flame_s2.png` | `191c718f374ed72eed1e0464f1672a77` | alias |
| `vehicle_sr75_exhaust_ship.png` | `191c718f374ed72eed1e0464f1672a77` | alias |
| `vehicle_sr75_flame_booster.png` | `4e401a3509c4206a61cfee9ff540ed0b` | resealed gap=-6 (was +46) |
| `vehicle_sr75_flame_s1.png` | `4e401a3509c4206a61cfee9ff540ed0b` | alias of booster flame |
| `vehicle_sr75_engine_booster.png` | `e36d5fd9c3f7e5d5b80179f6c0d5b8d1` | KEEP WAVE1 |

## Geometry
- ship_groups: `[(168, [0], 14, 34)]`
- eng_ship bbox: (197, 165, 226, 203) gap=-6
- flame_booster gap=-6
- lib_side_bell.md5 `0f5111a272ddb2f9876784100e86bd46`

## Soft-FAIL body (unchanged)
- stack: `6c61b6a0ed3626bede65b20974a99663`
- side: `6c61b6a0ed3626bede65b20974a99663`
- cutaway: `99f6f2a58e67d21e473387239d65c38e`
- shell: `e606603e8b384e2dbaf948a20696a11f`
- ship: `a2b098717cdf05272430bd8373d8107a`
- booster: `400ec0bd59f69a6065218f08cfb98955`

## Soft-FAIL
- Soft-FAIL invent claim of published S2
- Soft-FAIL stack/cutaway/shell/side/tank overwrite
- Soft-FAIL commit/device/Studio/Play

## Work
`/workspace/agent-backups/Da-Vinci/nozzle-tip145-20260923/sr75`
