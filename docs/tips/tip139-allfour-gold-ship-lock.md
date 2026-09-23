# tip139 — ALL FOUR GOLD ship-lock (1.0.129)

**Date:** 2026-09-23 CT  
**Stamp:** 139 / 1.0.129 on `stamp-85`  
**Play:** HOLD  
**Device / Studio push:** Soft-FAIL (HARD_DEVICE_PUSH_GO / Soft-FAIL interrupt Chris)

## Contents
- Helios-PASS GOLD art packed + wired for **cz6a + sr75 + nuri + h3**.
- VehicleCatalog VehicleSpec rows + drawable consts.
- VehicleDraw `when(fam)` + continuous-stack / plateCap / overlay allowlists.
- Version unchanged: `versionCode = 139` / `versionName = "1.0.129"` (do not reverse).

## Soft-PASS MD5 prove (drawable)

| Asset | MD5 |
|-------|-----|
| vehicle_cz6a_stack.png | 17ad81c62235dd204a535e6f2519e50e |
| vehicle_cz6a_cutaway.png | 6dd047856ac7bfb7c538d350d77f8d41 |
| vehicle_cz6a_shell.png | 037d0d9310b1813b7cf0e806e12b44d5 |
| vehicle_sr75_stack.png | 4cb9b75bac4ea7b8c92166181460718d |
| vehicle_sr75_cutaway.png | 957417a271334d47c873e85e852d6bd2 |
| vehicle_sr75_shell.png | e606603e8b384e2dbaf948a20696a11f |
| vehicle_nuri_stack.png | 9b055e356a44dd7a70a5ba74f8550693 |
| vehicle_nuri_cutaway.png | 36adc443daf67c643ed1f00b492cb497 |
| vehicle_nuri_shell.png | bebeff2a52a31921d421b6c525107419 |
| vehicle_h3_stack.png | 0716e244a5ca17668ed908ca4c640673 |
| vehicle_h3_cutaway.png | de1c01cbdef94ff56ac9c950d45c243a |
| vehicle_h3_shell.png | 38b6a173c2fbd6f8c69d653c95abe36f |

## Wires
- `cz6a`: s1Engines=2 liquid + SRBs in nerdNote; CHAMBER4 / VACUUM1; drawArtThenLmTanks
- `sr75`: s2Engines=0; VACUUM1 / VACUUM1; drawArtThenCoreTanks
- `nuri`: CHAMBER4 / VACUUM1; drawArtThenCoreTanks
- `h3`: tokens mmx / h3-24 / h3-24l / martian moons; fuelName LH2; oxName LOX; drawFamily=h3; drawArtThenCoreTanks (pre-existing)

## Soft-FAIL
- Soft-FAIL phone / Studio install
- Soft-FAIL interrupt Chris
- Soft-FAIL invent unpublished numbers (use — / draft)
- Soft-FAIL force push

## Staged APK
`/workspace/LRT-tip139-1.0.129/LiveRocketTracker-tip139-1.0.129-debug.apk`
