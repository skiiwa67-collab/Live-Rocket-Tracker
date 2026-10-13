# LRT Blue Origin catalog audit (read-only), 2026-10-09 ~10:05 AM CT — Helios

Source audited: GitHub skiiwa67-collab/Live-Rocket-Tracker, branch `origin/stamp-85` @ `4be9291` (2026-09-29, versionCode 157 / 1.0.147) — newest app code on GitHub. `main` (fa14ae7) holds docs only for app code since Sep. Clone: /workspace/lrt-audit (local branch audit-stamp85, nothing pushed). Build on Chris's phone: UNVERIFIED (not checked).
LL2 data: https://ll.thespacedevs.com/2.2.0/launch/?lsp__name=Blue%20Origin (70 launches, not throttled): NS-1..NS-38 (38 New Shepard) + 32 New Glenn (3 flown, 29 TBD).

## How a launch resolves (verified, code)
- `VehicleCatalog.blob()` VehicleCatalog.kt:1030-1031 = lowercase "rocketName name missionName provider". For NS-38 that is "new shepard new shepard | ns-38 ns-38 blue origin".
- `spec()` VehicleCatalog.kt:1044-1049: first row whose any token is a substring of blob, else GENERIC (VehicleCatalog.kt:100-122, verified=false, drawFamily "generic").
- Rows (30): starship, fh, f9, glenn, vulcan, sls, soyuz, electron, ariane, lm5, cz2d, cz8a, cz6a, kz11, cz12, lm, lvm3, pslv, gslv, isro, zq3, sr75, nuri, h3, atlas, firefly, kinetica1, pallas1, gravity1, proton. **No New Shepard row; no token "new shepard"/"shepard"/"ns-".**
- Draw: VehicleDraw.kt:83-165 switch on drawFamily; "glenn" -> drawArtThenCoreTanks("glenn") (106-109); else -> drawArtThenCoreTanks("generic") (161-164). No `vehicle_generic*` drawable exists, so it returns false -> `cleanSilhouette` (VehicleDraw.kt:1416-1446: grey rounded tube + triangle) and `bookGap` banner "NEW VEHICLE · DATA UPDATE REQUIRED" (166-168, 1448+).
- Timeline: FlightEvents.kt:55 needsUpdate -> unknownVehicle() (176-185: LIFTOFF + DATA UPDATE REQUIRED).

## Why NS 38 and NS 28 fail (VERIFIED by code + script simulation of spec() on live LL2 data)
Not a name-format issue ("NS 38" vs "NS-38" is irrelevant — nothing in the catalog matches either form, nor "New Shepard"). There is no New Shepard VehicleSpec row and no New Shepard art in the repo (no file matching shepard/ns anywhere in res/, or anywhere on the box). All New Shepard launches fall to GENERIC by design ("do not invent a drawing"). Result on screen per code: placeholder grey tube + DATA UPDATE REQUIRED banner, not real art. (On-device appearance UNVERIFIED by screenshot.)
**Every New Shepard flight fails the same way: NS-1 through NS-38 (all 38, simulated; 0 resolve).** Also "New Shepard" alone, "NS 38", "NS-38" -> GENERIC.
History: PlayListing/CATALOG-AUDIT.txt:347 (commit 7e44f2d, 2026-08-30) already listed New Shepard among vehicles that "land here" (generic). Never added since.

## GOOD
- New Glenn: all 32 LL2 New Glenn launches resolve to `glenn` via token "new glenn" (VehicleCatalog.kt:211-234), incl. Maiden Flight, EscaPADE, BlueBird 7 (failed Apr 2026), BlueBird Block 2, Amazon Leo LN-01..LN-24, Blue Moon Pathfinder 1&2/VIPER, HLS LEO Demo, Mars Telecom Orbiter.
- Glenn art files (11) exist, valid RGBA PNG 424x1200, non-empty, transparent bg (75-97% transparent), no byte-duplicates of any other family. Added f08c08a (Ada, stamp 93, 2026-09-09 15:31 CT). Assembled-parts path loads `vehicle_glenn_booster` + `vehicle_glenn_ship`; tank masks tank_s1/s2_ox/fuel present.
- Agency label "BLUE ORIGIN" (LaunchModels.kt:67-69, 369, 388), language `en` (VehicleCatalog.kt:1033-1042 else branch) — correct for a US agency. Blue Origin TelemetrySkin exists (TelemetrySkin.kt:137-150, 292). Pad glyph GLENN TOWER for LC-36 (PadGlyph.kt:50).

## BROKEN / MISSING
1. New Shepard (NS-1..NS-38): no catalog row, no art -> GENERIC placeholder. Evidence above. (NS 38 / NS 28 already being logged by another worker; same root cause covers all 38.)
2. No "Blue Origin update art" commit found: `git log --all -i --grep` for blue/shepard/glenn on GitHub (all branches) and local RetroCommandCenter returns only ac20863 (stamp 91, tank colours, unrelated) and f08c08a (Glenn art). Newest Glenn art is f08c08a. If Chris expects newer Blue Origin art, it is not in GitHub (UNVERIFIED whether it exists elsewhere, e.g. Da Vinci's folders/Dropbox — not searched beyond /workspace).

## Other findings (Glenn)
- Art accuracy (visual review, my judgment): `vehicle_glenn_booster` is a full stack with sharp ogive nose and large swept delta fins; `vehicle_glenn_ship` is a cut-away ogive nose section. Real New Glenn has a blunt 7 m fairing and small aft fins/strakes. Looks generic-ogive, not clearly New Glenn. Matches the old note in CATALOG-AUDIT.txt:215 ("Hat is a generic ogive, not New Glenn's blunt fairing"). Recommend Da Vinci review; not a broken-render bug.
- No glenn flame/engine/exhaust art (`vehicle_glenn_flame_*`, `_engine_*` absent); uses procedural "raptor" plume (VehicleDraw.kt:945, 957). Fine functionally.
- Upper stage data: glenn row s2Pattern = BE4_2 (VehicleCatalog.kt:233) and methalox=true / fuelName CH4 for the whole vehicle, but New Glenn GS2 uses 2x BE-3U (LH2/LOX). engineName "BE-4 / BE-3U" and ispVac 445 s are BE-3U-ish, so the row is internally inconsistent. S2 tanks/plume will render as methalox (VehicleDraw.kt:107 passes methalox=true). s2Thrust "1.6 MN VAC" — public figure for 2x BE-3U is ~1.4 MN (UNVERIFIED against a source in this audit).
- Timeline: glenn falls to oneWay() (FlightEvents.kt:67-68, 146-154): no booster landing events despite recoverable=true / landingLit=7. Stage count is 2 (correct).
- Dead code: drawGlennVehicle in CommandConsoleView.kt:3593 and RetroCommandWallpaperService.kt:5798, and VehicleDraw.glenn() :2140 have no callers.
- Console skin: Blue Origin launches map to MCC console skin (AppPrefs.kt:88), not a Blue Origin board; Da Vinci HANDOFF-157 notes blue.png "is not one of the ten chips". By design per 157, noted only.
- Demo launch "New Glenn | Demo Mission" (LaunchDataProvider.kt:1203-1211) resolves to glenn (good).
- Blue Moon / Blue Ring: no catalog rows (they are payloads on New Glenn in LL2; launches resolve to glenn — fine).

## Root cause summary
- VERIFIED: New Shepard missing art = no VehicleSpec row + no drawables; spec() falls to GENERIC. Affects all NS-1..NS-38.
- UNVERIFIED: exact on-phone look and which build Chris is running.
