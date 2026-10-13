# FLEET_CUTOUT_AUDIT — tip153 Stamp 92 fuel windows

**When:** 2026-09-23 CT
**Pack:** `/workspace/agent-backups/Da-Vinci/tip153-fuel-cutouts-20260923/`
**Metric:** `fuel_clear%` = % of fuel-mask pixels where hull alpha ≤ 8
**PASS bar:** ≥ 90.0%

## Soft-FAIL held
- Soft-FAIL invent unpublished geometry / empty-mask windows
- Soft-FAIL overwrite stack/side/cutaway/engines/flames
- KEEP Gravity-1 side/stack `c78f781a40a01676fdb57ae5343ba07b` Soft-PASS
- KEEP Electron stack `d8ab6a86858da2237a18838c4c4dc91e` Soft-PASS
- Soft-FAIL invent F9 side MVac Soft-FAIL phantom SR75 S2 Soft-FAIL invent liquid G1 S4
- Soft-FAIL Ada wire Soft-FAIL hud-assets copy Soft-FAIL Play/phone/adb Soft-FAIL ship until Helios Soft-PASS + Chris Soft-PASS
- Soft-FAIL paint fills onto hull (TRUE alpha holes only)

## Family rollup (ALL)

| # | family | bucket | status | punched stages | FAIL remaining |
|--:|--------|--------|--------|----------------|----------------|
| 1 | `gravity1` | next-30d | **PASS** | s1, s2, s3, booster, ship | — |
| 2 | `cz8a` | next-30d | **PASS** | s1, s2, booster, ship | — |
| 3 | `cz6a` | next-30d | **PASS** | s1, s2, booster, ship | — |
| 4 | `electron` | next-30d | **PASS** | s1, s2, booster, ship | — |
| 5 | `f9` | next-30d | **PASS** | s1, s2 | — |
| 6 | `starship` | next-30d | **PASS** | — | — |
| 7 | `ariane` | next-30d | **PASS** | — | — |
| 8 | `sr75` | next-30d | **PASS** | s1, booster | — |
| 9 | `nuri` | next-30d | **PASS** | s1, s2, booster, ship | — |
| 10 | `gslv` | next-30d | **PASS** | — | — |
| 11 | `h3` | next-30d | **PASS** | s1, s2, booster, ship | — |
| 12 | `cz2d` | next-30d | **PASS** | s1, s2, booster, ship | — |
| 13 | `kz11` | next-30d | **PASS** | s1, s2, s3, s4, booster, ship | — |
| 14 | `atlas` | catalog | **PASS** | — | — |
| 15 | `cz12` | catalog | **PASS** | s1, s2, booster, ship | — |
| 16 | `fh` | catalog | **PASS** | — | — |
| 17 | `firefly` | catalog | **PASS** | — | — |
| 18 | `glenn` | catalog | **PASS** | — | — |
| 19 | `isro` | catalog | **PASS** | — | — |
| 20 | `kinetica1` | catalog | **PASS** | s1, s2, s3, s4, booster, ship | — |
| 21 | `lm5` | catalog | **PASS** | s1, s2, booster, ship | — |
| 22 | `lvm3` | catalog | **PASS** | — | — |
| 23 | `pallas1` | catalog | **PASS** | s1, s2, booster, ship | — |
| 24 | `proton` | catalog | **PASS** | — | — |
| 25 | `pslv` | catalog | **PASS** | — | — |
| 26 | `sls` | catalog | **PASS** | — | — |
| 27 | `soyuz` | catalog | **PASS** | — | — |
| 28 | `vulcan` | catalog | **PASS** | — | — |
| 29 | `zq3` | catalog | **PASS** | s1, s2, booster, ship | — |

## Per-stage detail

| family | stage | punched? | fuel_clear before→after | status | new md5 |
|--------|-------|----------|-------------------------|--------|---------|
| `gravity1` | `s1` | YES | 91.8→91.8 | PASS | `6962af66b28c` |
| `gravity1` | `s2` | YES | 89.9→94.3 | PASS | `aac9bf5dc824` |
| `gravity1` | `s3` | YES | 89.4→91.9 | PASS | `db0011c43b82` |
| `gravity1` | `s4` | no | — | EMPTY_MASK | `—` |
| `gravity1` | `booster` | YES | 0.0→92.4 | PASS | `d23e8d6f3412` |
| `gravity1` | `ship` | YES | 0.0→93.4 | PASS | `ec6002351608` |
| `cz8a` | `s1` | YES | 0.0→94.5 | PASS | `ef01b87368fd` |
| `cz8a` | `s2` | YES | 0.0→93.7 | PASS | `12bef3896c92` |
| `cz8a` | `booster` | YES | 0.0→94.6 | PASS | `ef01b87368fd` |
| `cz8a` | `ship` | YES | 0.0→93.6 | PASS | `20cf4dc441a5` |
| `cz6a` | `s1` | YES | 0.0→94.7 | PASS | `08b06dcb62fa` |
| `cz6a` | `s2` | YES | 0.0→94.4 | PASS | `621b739698e5` |
| `cz6a` | `booster` | YES | 0.0→94.7 | PASS | `08b06dcb62fa` |
| `cz6a` | `ship` | YES | 0.0→94.1 | PASS | `be05333a1fc2` |
| `electron` | `s1` | YES | 0.0→94.2 | PASS | `c58406d8a0dd` |
| `electron` | `s2` | YES | 14.3→94.6 | PASS | `b7e0e3975257` |
| `electron` | `booster` | YES | 0.0→94.2 | PASS | `c58406d8a0dd` |
| `electron` | `ship` | YES | 0.0→93.1 | PASS | `7c356d1396ca` |
| `f9` | `s1` | YES | 0.0→93.5 | PASS | `114106c22d6b` |
| `f9` | `s2` | YES | 0.0→91.9 | PASS | `008270e03a38` |
| `starship` | `s1` | no | 97.8→97.8 | PASS | `ca3797b7acd6` |
| `starship` | `s2` | no | 97.5→97.5 | PASS | `ae14b4c2f6e2` |
| `starship` | `booster` | no | 94.7→94.7 | PASS | `ca3797b7acd6` |
| `starship` | `ship` | no | 95.0→95.0 | PASS | `ae14b4c2f6e2` |
| `ariane` | `s1` | no | 100.0→100.0 | PASS | `50e492d5d539` |
| `ariane` | `s2` | no | 100.0→100.0 | PASS | `30767d4880bd` |
| `ariane` | `booster` | no | 100.0→100.0 | PASS | `50e492d5d539` |
| `ariane` | `ship` | no | 100.0→100.0 | PASS | `30767d4880bd` |
| `sr75` | `s1` | YES | 0.0→94.3 | PASS | `f91bf889b0c2` |
| `sr75` | `s2` | no | — | EMPTY_MASK | `—` |
| `sr75` | `booster` | YES | 0.0→94.1 | PASS | `f91bf889b0c2` |
| `nuri` | `s1` | YES | 0.0→97.2 | PASS | `e4acf963db57` |
| `nuri` | `s2` | YES | 0.0→97.2 | PASS | `6f49ca8e722b` |
| `nuri` | `booster` | YES | 0.0→97.1 | PASS | `e4acf963db57` |
| `nuri` | `ship` | YES | 0.0→94.4 | PASS | `fda2c10e3a9e` |
| `gslv` | `s1` | no | 100.0→100.0 | PASS | `769a12c27127` |
| `gslv` | `s2` | no | — | EMPTY_MASK | `—` |
| `gslv` | `booster` | no | 100.0→100.0 | PASS | `769a12c27127` |
| `h3` | `s1` | YES | 0.0→94.7 | PASS | `f7254791e6db` |
| `h3` | `s2` | YES | 0.0→93.2 | PASS | `ee0bb90ebed6` |
| `h3` | `booster` | YES | 0.0→94.8 | PASS | `f7254791e6db` |
| `h3` | `ship` | YES | 0.0→92.9 | PASS | `eed7bfea494b` |
| `cz2d` | `s1` | YES | 0.0→96.7 | PASS | `aa9e8623fc3c` |
| `cz2d` | `s2` | YES | 0.0→94.6 | PASS | `72244d25aae7` |
| `cz2d` | `booster` | YES | 0.0→96.6 | PASS | `2ec21ec7db4b` |
| `cz2d` | `ship` | YES | 0.0→94.2 | PASS | `3e2721beb456` |
| `kz11` | `s1` | YES | 0.0→96.0 | PASS | `a76aec499e7e` |
| `kz11` | `s2` | YES | 0.0→95.2 | PASS | `89c0e001bf26` |
| `kz11` | `s3` | YES | 0.0→95.0 | PASS | `c80d979a9067` |
| `kz11` | `s4` | YES | 0.5→92.3 | PASS | `1da88c018b45` |
| `kz11` | `booster` | YES | 0.0→96.0 | PASS | `a76aec499e7e` |
| `kz11` | `ship` | YES | 0.1→94.0 | PASS | `0fd47fc819ec` |
| `atlas` | `s1` | no | 100.0→100.0 | PASS | `746df3ad3718` |
| `atlas` | `s2` | no | 100.0→100.0 | PASS | `ba897ddcde2f` |
| `atlas` | `booster` | no | 100.0→100.0 | PASS | `746df3ad3718` |
| `atlas` | `ship` | no | 100.0→100.0 | PASS | `ba897ddcde2f` |
| `cz12` | `s1` | YES | 0.0→98.1 | PASS | `8eb573210a72` |
| `cz12` | `s2` | YES | 0.1→95.0 | PASS | `2b504e9f635d` |
| `cz12` | `booster` | YES | 0.8→98.1 | PASS | `ac226db16542` |
| `cz12` | `ship` | YES | 0.0→94.9 | PASS | `6ae4e447e2be` |
| `fh` | `s1` | no | 96.0→96.0 | PASS | `080c9f8dca37` |
| `fh` | `s2` | no | 95.7→95.7 | PASS | `329b4b6cb5c2` |
| `fh` | `booster` | no | 96.0→96.0 | PASS | `080c9f8dca37` |
| `fh` | `ship` | no | 95.5→95.5 | PASS | `c980b58c79ef` |
| `firefly` | `s1` | no | 100.0→100.0 | PASS | `777f19fc11df` |
| `firefly` | `s2` | no | 100.0→100.0 | PASS | `558bce759752` |
| `firefly` | `booster` | no | 100.0→100.0 | PASS | `777f19fc11df` |
| `firefly` | `ship` | no | 100.0→100.0 | PASS | `558bce759752` |
| `glenn` | `s1` | no | 100.0→100.0 | PASS | `fe360310d3c0` |
| `glenn` | `s2` | no | 100.0→100.0 | PASS | `2c54259a2853` |
| `glenn` | `booster` | no | 100.0→100.0 | PASS | `fe360310d3c0` |
| `glenn` | `ship` | no | 100.0→100.0 | PASS | `2c54259a2853` |
| `isro` | `s1` | no | 100.0→100.0 | PASS | `cde76f8fe753` |
| `isro` | `s2` | no | — | EMPTY_MASK | `—` |
| `isro` | `booster` | no | 100.0→100.0 | PASS | `cde76f8fe753` |
| `kinetica1` | `s1` | YES | 0.0→97.7 | PASS | `76c2e6e6dcb5` |
| `kinetica1` | `s2` | YES | 3.9→95.8 | PASS | `fa71b4d31cab` |
| `kinetica1` | `s3` | YES | 4.9→95.5 | PASS | `8d3dc282224d` |
| `kinetica1` | `s4` | YES | 1.1→92.5 | PASS | `f61614d0384d` |
| `kinetica1` | `booster` | YES | 0.0→97.7 | PASS | `76c2e6e6dcb5` |
| `kinetica1` | `ship` | YES | 3.7→95.1 | PASS | `89bdc8e6e0ed` |
| `lm5` | `s1` | YES | 0.0→95.1 | PASS | `ae5e5bf06c79` |
| `lm5` | `s2` | YES | 0.0→95.5 | PASS | `1b521e9c1a71` |
| `lm5` | `booster` | YES | 0.0→93.1 | PASS | `ae5e5bf06c79` |
| `lm5` | `ship` | YES | 0.0→95.3 | PASS | `4b37cc78c2c4` |
| `lvm3` | `s1` | no | 100.0→100.0 | PASS | `cde76f8fe753` |
| `lvm3` | `s2` | no | 100.0→100.0 | PASS | `414cfe661804` |
| `lvm3` | `booster` | no | 100.0→100.0 | PASS | `cde76f8fe753` |
| `lvm3` | `ship` | no | 100.0→100.0 | PASS | `414cfe661804` |
| `pallas1` | `s1` | YES | 0.0→92.1 | PASS | `b7006818ba2d` |
| `pallas1` | `s2` | YES | 0.0→92.2 | PASS | `4f9ae048a3b1` |
| `pallas1` | `booster` | YES | 0.0→92.0 | PASS | `b7006818ba2d` |
| `pallas1` | `ship` | YES | 0.0→91.8 | PASS | `0bdb4f25429f` |
| `proton` | `s1` | no | 100.0→100.0 | PASS | `5d408be362c2` |
| `proton` | `s2` | no | 100.0→100.0 | PASS | `e3e103299317` |
| `proton` | `booster` | no | 100.0→100.0 | PASS | `5d408be362c2` |
| `proton` | `ship` | no | 100.0→100.0 | PASS | `e3e103299317` |
| `pslv` | `s1` | no | 100.0→100.0 | PASS | `cde76f8fe753` |
| `pslv` | `s2` | no | — | EMPTY_MASK | `—` |
| `pslv` | `booster` | no | 100.0→100.0 | PASS | `cde76f8fe753` |
| `sls` | `s1` | no | 100.0→100.0 | PASS | `9fdfd2d17f22` |
| `sls` | `s2` | no | — | EMPTY_MASK | `—` |
| `sls` | `booster` | no | 100.0→100.0 | PASS | `9fdfd2d17f22` |
| `soyuz` | `s1` | no | 100.0→100.0 | PASS | `4969b501f544` |
| `soyuz` | `s2` | no | 100.0→100.0 | PASS | `d5d90bbe9a9b` |
| `soyuz` | `booster` | no | 100.0→100.0 | PASS | `4969b501f544` |
| `soyuz` | `ship` | no | 100.0→100.0 | PASS | `d5d90bbe9a9b` |
| `vulcan` | `s1` | no | 100.0→100.0 | PASS | `0f046273b0fd` |
| `vulcan` | `s2` | no | 100.0→100.0 | PASS | `135be0c7d81a` |
| `vulcan` | `booster` | no | 100.0→100.0 | PASS | `0f046273b0fd` |
| `vulcan` | `ship` | no | 100.0→100.0 | PASS | `135be0c7d81a` |
| `zq3` | `s1` | YES | 2.9→96.4 | PASS | `1d40e57508c8` |
| `zq3` | `s2` | YES | 3.8→96.4 | PASS | `01238e32c74d` |
| `zq3` | `booster` | YES | 3.0→96.5 | PASS | `1d40e57508c8` |
| `zq3` | `ship` | YES | 3.2→96.2 | PASS | `84dd1021ca07` |

**PASS families:** 29 — `gravity1`, `cz8a`, `cz6a`, `electron`, `f9`, `starship`, `ariane`, `sr75`, `nuri`, `gslv`, `h3`, `cz2d`, `kz11`, `atlas`, `cz12`, `fh`, `firefly`, `glenn`, `isro`, `kinetica1`, `lm5`, `lvm3`, `pallas1`, `proton`, `pslv`, `sls`, `soyuz`, `vulcan`, `zq3`
**FAIL remaining:** 0 — none

