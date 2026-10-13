# READY — tip153 Stamp 92 fleet fuel cutouts (eyes-only)

**When:** 2026-09-23 CT
**Lane:** Da Vinci art — Stamp 92 TRUE alpha fuel windows Soft-PASS fill-under Soft-PASS
**Chris Soft-PASS:** Gravity-1 identity Soft-PASS ("looks much better") Soft-FAIL cutouts Soft-PASS → fill-under board Soft-PASS ochre through windows

## Soft-FAIL held
- Soft-FAIL copy into `hud-assets` Soft-FAIL Ada wire Soft-FAIL Play Soft-FAIL phone Soft-FAIL adb until Helios Soft-PASS + Chris Soft-PASS
- Soft-FAIL overwrite Gravity-1 side/stack (`c78f781a40a01676fdb57ae5343ba07b` KEEP)
- Soft-FAIL overwrite Electron stack (`d8ab6a86858da2237a18838c4c4dc91e` KEEP) Soft-PASS Electron GOLD body
- Soft-FAIL overwrite any stack/side/cutaway/engines/flames
- Soft-FAIL invent F9 side MVac Soft-FAIL phantom SR75 S2 Soft-FAIL invent liquid G1 S4 (s4=s3 alias)
- Soft-FAIL invent windows on EMPTY fuel masks Soft-FAIL paint fills onto hull

## Method Soft-PASS
1. Prefer GOLD `vehicle_{id}_shell.png` window ∩ stage plate Soft-PASS shell-match residual blocked fuel.
2. Else / also: fuel+ox+fills mask island + modest inset Soft-PASS keep hull rim.
3. Gravity-1: tip152 identity KEEP Soft-PASS; punch Soft-PASS match shell fuel_clear Soft-PASS.
4. Soft-FAIL stack/side Soft-FAIL hud mirror Soft-PASS eyes-only pack.

## Paths
- Pack: `/workspace/agent-backups/Da-Vinci/tip153-fuel-cutouts-20260923`
- Plates: `/workspace/agent-backups/Da-Vinci/tip153-fuel-cutouts-20260923/plates`
- Preview fill-under board: `/workspace/agent-backups/Da-Vinci/tip153-fuel-cutouts-20260923/preview_fillunder_board.png` (md5 `2008e1146bb26627e801939d84267798`)
- G1 fill-under: `/workspace/agent-backups/Da-Vinci/tip153-fuel-cutouts-20260923/preview_gravity1_fillunder.png` (md5 `bda9b1cf5d4c598ae6a9436566d3ce26`)
- Fleet audit: `/workspace/agent-backups/Da-Vinci/tip153-fuel-cutouts-20260923/FLEET_CUTOUT_AUDIT.md`

## Gravity-1 (identity KEEP + windows obvious)

| asset | old (tip152) md5 | new md5 | fuel_clear before→after | note |
|-------|------------------|---------|-------------------------|------|
| `vehicle_gravity1_s1.png` | `6962af66b28c6a41fbbe571145e4d6ed` | `6962af66b28c6a41fbbe571145e4d6ed` | 91.8→**91.8** | identity KEEP Soft-PASS (already ≥90) |
| `vehicle_gravity1_s2.png` | `4e246859c58a2b6e36ecfa4d164599fd` | `aac9bf5dc8245f04e2f6e02883c73acb` | 89.9→**94.3** | shell-match punch Soft-PASS wide squat KEEP |
| `vehicle_gravity1_s3.png` | `dc532102efb67377fbf47ec80477ccf8` | `db0011c43b8207707cdaab8af6e04528` | 89.4→**91.9** | shell-match punch Soft-PASS |
| `vehicle_gravity1_booster.png` | `0f147b073a3ab67aa289b71dfcbc5ecd` | `d23e8d6f341221e4369077091792e6d1` | 0.0→**92.4** | post-sep STG1 Soft-PASS punch |
| `vehicle_gravity1_ship.png` | `ca62ae67e330c15e191e56139e06223f` | `ec6002351608c9c67ae7e439ea5cb8fa` | 0.0→**93.4** | eyes-only Soft-FAIL Ada/hud ship until Soft-PASS |

## Batch punch table (one batch — next-30d FIRST then remaining catalog)

| family | asset | old md5 | new md5 | fuel_clear before→after | Soft-FAIL invent note |
|--------|-------|---------|---------|-------------------------|----------------------|
| `gravity1` | `vehicle_gravity1_s1.png` | `6962af66b28c6a41fbbe571145e4d6ed` | `6962af66b28c6a41fbbe571145e4d6ed` | 91.8→**91.8** | Soft-FAIL invent Soft-FAIL stack overwrite Soft-FAIL hud copy |
| `gravity1` | `vehicle_gravity1_s2.png` | `4e246859c58a2b6e36ecfa4d164599fd` | `aac9bf5dc8245f04e2f6e02883c73acb` | 89.9→**94.3** | Soft-FAIL invent Soft-FAIL stack overwrite Soft-FAIL hud copy |
| `gravity1` | `vehicle_gravity1_s3.png` | `dc532102efb67377fbf47ec80477ccf8` | `db0011c43b8207707cdaab8af6e04528` | 89.4→**91.9** | Soft-FAIL invent Soft-FAIL stack overwrite Soft-FAIL hud copy |
| `gravity1` | `vehicle_gravity1_booster.png` | `0f147b073a3ab67aa289b71dfcbc5ecd` | `d23e8d6f341221e4369077091792e6d1` | 0.0→**92.4** | Soft-FAIL invent Soft-FAIL stack overwrite Soft-FAIL hud copy |
| `gravity1` | `vehicle_gravity1_ship.png` | `ca62ae67e330c15e191e56139e06223f` | `ec6002351608c9c67ae7e439ea5cb8fa` | 0.0→**93.4** | Soft-FAIL invent Soft-FAIL stack overwrite Soft-FAIL hud copy |
| `cz8a` | `vehicle_cz8a_s1.png` | `6096d0fe0472e17da5d601222fc4434e` | `ef01b87368fdcd8d3439fa0f01e2f139` | 0.0→**94.5** | Soft-FAIL invent Soft-FAIL stack overwrite Soft-FAIL hud copy |
| `cz8a` | `vehicle_cz8a_s2.png` | `c7c1c0576e948165891f95cece3a8301` | `12bef3896c929805a4ad69a042a975be` | 0.0→**93.7** | Soft-FAIL invent Soft-FAIL stack overwrite Soft-FAIL hud copy |
| `cz8a` | `vehicle_cz8a_booster.png` | `6096d0fe0472e17da5d601222fc4434e` | `ef01b87368fdcd8d3439fa0f01e2f139` | 0.0→**94.6** | Soft-FAIL invent Soft-FAIL stack overwrite Soft-FAIL hud copy |
| `cz8a` | `vehicle_cz8a_ship.png` | `144a453138f0cae79ad86107eb77a39f` | `20cf4dc441a509b5f461b2290e406535` | 0.0→**93.6** | Soft-FAIL invent Soft-FAIL stack overwrite Soft-FAIL hud copy |
| `cz6a` | `vehicle_cz6a_s1.png` | `1a2af3c14afbfd9d7a7fd6d1dfa6a654` | `08b06dcb62fa8ece1a581c559c6e72ed` | 0.0→**94.7** | Soft-FAIL invent Soft-FAIL stack overwrite Soft-FAIL hud copy |
| `cz6a` | `vehicle_cz6a_s2.png` | `68499f7f5632127ccdbbea1feac12dd2` | `621b739698e515b55bfad51f13149fd8` | 0.0→**94.4** | Soft-FAIL invent Soft-FAIL stack overwrite Soft-FAIL hud copy |
| `cz6a` | `vehicle_cz6a_booster.png` | `1a2af3c14afbfd9d7a7fd6d1dfa6a654` | `08b06dcb62fa8ece1a581c559c6e72ed` | 0.0→**94.7** | Soft-FAIL invent Soft-FAIL stack overwrite Soft-FAIL hud copy |
| `cz6a` | `vehicle_cz6a_ship.png` | `20c417d8163f6c9877c3f26255d18a10` | `be05333a1fc2582d6f1d26685537705f` | 0.0→**94.1** | Soft-FAIL invent Soft-FAIL stack overwrite Soft-FAIL hud copy |
| `electron` | `vehicle_electron_s1.png` | `6f5d983731827a6f1d745b30b2792847` | `c58406d8a0dd4ef0f9febd6432f630e5` | 0.0→**94.2** | Soft-FAIL invent Soft-FAIL stack overwrite Soft-FAIL hud copy |
| `electron` | `vehicle_electron_s2.png` | `033df399dced71b089c0c44157269fe8` | `b7e0e397525778c4ddadbaf49866c292` | 14.3→**94.6** | Soft-FAIL invent Soft-FAIL stack overwrite Soft-FAIL hud copy |
| `electron` | `vehicle_electron_booster.png` | `6f5d983731827a6f1d745b30b2792847` | `c58406d8a0dd4ef0f9febd6432f630e5` | 0.0→**94.2** | Soft-FAIL invent Soft-FAIL stack overwrite Soft-FAIL hud copy |
| `electron` | `vehicle_electron_ship.png` | `fcbf6ed710e2240eb78de82c2f3f537e` | `7c356d1396ca9196f8aa6ea7903cba48` | 0.0→**93.1** | Soft-FAIL invent Soft-FAIL stack overwrite Soft-FAIL hud copy |
| `f9` | `vehicle_f9_s1.png` | `e7151edade115132d3a6e740b00ee3ce` | `114106c22d6b843292aa4886b69e441d` | 0.0→**93.5** | Soft-FAIL invent Soft-FAIL stack overwrite Soft-FAIL hud copy |
| `f9` | `vehicle_f9_s2.png` | `fa4605fb3a52632915cde2954d7d8919` | `008270e03a386bf519d6afcc885bcbaf` | 0.0→**91.9** | Soft-FAIL invent Soft-FAIL stack overwrite Soft-FAIL hud copy |
| `sr75` | `vehicle_sr75_s1.png` | `400ec0bd59f69a6065218f08cfb98955` | `f91bf889b0c2000ee7d08e28a77169c6` | 0.0→**94.3** | Soft-FAIL invent Soft-FAIL stack overwrite Soft-FAIL hud copy |
| `sr75` | `vehicle_sr75_booster.png` | `400ec0bd59f69a6065218f08cfb98955` | `f91bf889b0c2000ee7d08e28a77169c6` | 0.0→**94.1** | Soft-FAIL invent Soft-FAIL stack overwrite Soft-FAIL hud copy |
| `nuri` | `vehicle_nuri_s1.png` | `43056d15d6375e8d297c3e22a32946b8` | `e4acf963db57dbe6b5241046903b1948` | 0.0→**97.2** | Soft-FAIL invent Soft-FAIL stack overwrite Soft-FAIL hud copy |
| `nuri` | `vehicle_nuri_s2.png` | `784b32ecc2b8bde9930b8100e322a2bf` | `6f49ca8e722b92b3d2824ae180c09ce0` | 0.0→**97.2** | Soft-FAIL invent Soft-FAIL stack overwrite Soft-FAIL hud copy |
| `nuri` | `vehicle_nuri_booster.png` | `43056d15d6375e8d297c3e22a32946b8` | `e4acf963db57dbe6b5241046903b1948` | 0.0→**97.1** | Soft-FAIL invent Soft-FAIL stack overwrite Soft-FAIL hud copy |
| `nuri` | `vehicle_nuri_ship.png` | `e39e182485b6dc9dbe50c5c6082ff1fe` | `fda2c10e3a9e9716e8d9f96ed7b877bf` | 0.0→**94.4** | Soft-FAIL invent Soft-FAIL stack overwrite Soft-FAIL hud copy |
| `h3` | `vehicle_h3_s1.png` | `3b2af1906952e531f7b680abf6280490` | `f7254791e6db81ce7d001f3cab1bccd9` | 0.0→**94.7** | Soft-FAIL invent Soft-FAIL stack overwrite Soft-FAIL hud copy |
| `h3` | `vehicle_h3_s2.png` | `600f87fbc71085d00e686ad402f23af6` | `ee0bb90ebed6a29fb03e999bcf855e6d` | 0.0→**93.2** | Soft-FAIL invent Soft-FAIL stack overwrite Soft-FAIL hud copy |
| `h3` | `vehicle_h3_booster.png` | `3b2af1906952e531f7b680abf6280490` | `f7254791e6db81ce7d001f3cab1bccd9` | 0.0→**94.8** | Soft-FAIL invent Soft-FAIL stack overwrite Soft-FAIL hud copy |
| `h3` | `vehicle_h3_ship.png` | `a04c0a65f5c62de7b236f5f8f9f56265` | `eed7bfea494b96027770ef596f99081f` | 0.0→**92.9** | Soft-FAIL invent Soft-FAIL stack overwrite Soft-FAIL hud copy |
| `cz2d` | `vehicle_cz2d_s1.png` | `82073a60c8d250a100226587cfb12658` | `aa9e8623fc3cbff92a7cdd896e6418c8` | 0.0→**96.7** | Soft-FAIL invent Soft-FAIL stack overwrite Soft-FAIL hud copy |
| `cz2d` | `vehicle_cz2d_s2.png` | `36ecbab9aeb350253fe9561abbcc0894` | `72244d25aae77632fb7c5479d1540312` | 0.0→**94.6** | Soft-FAIL invent Soft-FAIL stack overwrite Soft-FAIL hud copy |
| `cz2d` | `vehicle_cz2d_booster.png` | `82073a60c8d250a100226587cfb12658` | `2ec21ec7db4bc5d49f66bd2b464c31f9` | 0.0→**96.6** | Soft-FAIL invent Soft-FAIL stack overwrite Soft-FAIL hud copy |
| `cz2d` | `vehicle_cz2d_ship.png` | `929c5f8a72fd2d150a7310f4a7eec390` | `3e2721beb456d1d7b36bb9a953c2baee` | 0.0→**94.2** | Soft-FAIL invent Soft-FAIL stack overwrite Soft-FAIL hud copy |
| `kz11` | `vehicle_kz11_s1.png` | `172d7d53f4f4d49ee4666078ea146d73` | `a76aec499e7ed49367b81b0278efe1b0` | 0.0→**96.0** | Soft-FAIL invent Soft-FAIL stack overwrite Soft-FAIL hud copy |
| `kz11` | `vehicle_kz11_s2.png` | `804cbd86d0ebc4d7a6043cdadfec8ea7` | `89c0e001bf267eaa296ce5b2352b31f2` | 0.0→**95.2** | Soft-FAIL invent Soft-FAIL stack overwrite Soft-FAIL hud copy |
| `kz11` | `vehicle_kz11_s3.png` | `3e7afb3b9b27db5c11741c0cf5d0db2c` | `c80d979a9067b028a11f754655f99e3c` | 0.0→**95.0** | Soft-FAIL invent Soft-FAIL stack overwrite Soft-FAIL hud copy |
| `kz11` | `vehicle_kz11_s4.png` | `bccec4f5f7a04abe1e06416ef825dd2b` | `1da88c018b45bbe3f5ed0afaf6ee59ce` | 0.5→**92.3** | Soft-FAIL invent Soft-FAIL stack overwrite Soft-FAIL hud copy |
| `kz11` | `vehicle_kz11_booster.png` | `172d7d53f4f4d49ee4666078ea146d73` | `a76aec499e7ed49367b81b0278efe1b0` | 0.0→**96.0** | Soft-FAIL invent Soft-FAIL stack overwrite Soft-FAIL hud copy |
| `kz11` | `vehicle_kz11_ship.png` | `618b40995466cc49bb583458cc293f0a` | `0fd47fc819ecbe8cf3b50fb08f9ad0be` | 0.1→**94.0** | Soft-FAIL invent Soft-FAIL stack overwrite Soft-FAIL hud copy |
| `cz12` | `vehicle_cz12_s1.png` | `ea636e6236077c59eb954dbd439f46a8` | `8eb573210a72f7d968b71d9430033135` | 0.0→**98.1** | Soft-FAIL invent Soft-FAIL stack overwrite Soft-FAIL hud copy |
| `cz12` | `vehicle_cz12_s2.png` | `e38293c0a33a01e108fbd6b0f60ae369` | `2b504e9f635de6336bf1e2663ff6529b` | 0.1→**95.0** | Soft-FAIL invent Soft-FAIL stack overwrite Soft-FAIL hud copy |
| `cz12` | `vehicle_cz12_booster.png` | `ea636e6236077c59eb954dbd439f46a8` | `ac226db16542d0abdb6cd847f6c020ff` | 0.8→**98.1** | Soft-FAIL invent Soft-FAIL stack overwrite Soft-FAIL hud copy |
| `cz12` | `vehicle_cz12_ship.png` | `e38293c0a33a01e108fbd6b0f60ae369` | `6ae4e447e2be3d6f72a0c520c2e579a2` | 0.0→**94.9** | Soft-FAIL invent Soft-FAIL stack overwrite Soft-FAIL hud copy |
| `kinetica1` | `vehicle_kinetica1_s1.png` | `37d2a9aa878687b3db6035424472f33c` | `76c2e6e6dcb590a5020699d8a7aa84ad` | 0.0→**97.7** | Soft-FAIL invent Soft-FAIL stack overwrite Soft-FAIL hud copy |
| `kinetica1` | `vehicle_kinetica1_s2.png` | `a57c175769804da51b692fe6f337abe7` | `fa71b4d31cabce19ccce7b3159c43249` | 3.9→**95.8** | Soft-FAIL invent Soft-FAIL stack overwrite Soft-FAIL hud copy |
| `kinetica1` | `vehicle_kinetica1_s3.png` | `9b523809eec3cb77e4edb00ad74b6cf7` | `8d3dc282224db7fb8fbf673ffce199aa` | 4.9→**95.5** | Soft-FAIL invent Soft-FAIL stack overwrite Soft-FAIL hud copy |
| `kinetica1` | `vehicle_kinetica1_s4.png` | `a381f6c4f37a657f1a7031b52cb377af` | `f61614d0384dfc7fb5ba8bfdbad8cd3e` | 1.1→**92.5** | Soft-FAIL invent Soft-FAIL stack overwrite Soft-FAIL hud copy |
| `kinetica1` | `vehicle_kinetica1_booster.png` | `37d2a9aa878687b3db6035424472f33c` | `76c2e6e6dcb590a5020699d8a7aa84ad` | 0.0→**97.7** | Soft-FAIL invent Soft-FAIL stack overwrite Soft-FAIL hud copy |
| `kinetica1` | `vehicle_kinetica1_ship.png` | `24eced24ce5347dd5b541fc38e5af6f1` | `89bdc8e6e0ed2a931332fb9913ff1f4f` | 3.7→**95.1** | Soft-FAIL invent Soft-FAIL stack overwrite Soft-FAIL hud copy |
| `lm5` | `vehicle_lm5_s1.png` | `36f4ab1587aa42d095b4895e053a9c85` | `ae5e5bf06c79106a32c0145a499cc9b2` | 0.0→**95.1** | Soft-FAIL invent Soft-FAIL stack overwrite Soft-FAIL hud copy |
| `lm5` | `vehicle_lm5_s2.png` | `82f9871f29aa5fa2c419b80d35ff46c1` | `1b521e9c1a71e4cf63e2f9b49c1e508d` | 0.0→**95.5** | Soft-FAIL invent Soft-FAIL stack overwrite Soft-FAIL hud copy |
| `lm5` | `vehicle_lm5_booster.png` | `36f4ab1587aa42d095b4895e053a9c85` | `ae5e5bf06c79106a32c0145a499cc9b2` | 0.0→**93.1** | Soft-FAIL invent Soft-FAIL stack overwrite Soft-FAIL hud copy |
| `lm5` | `vehicle_lm5_ship.png` | `30444dbc0e0b503afc72c9f5736a2684` | `4b37cc78c2c4b500ccc123a1b2be92ad` | 0.0→**95.3** | Soft-FAIL invent Soft-FAIL stack overwrite Soft-FAIL hud copy |
| `pallas1` | `vehicle_pallas1_s1.png` | `5cc9d63c18e71cf922ecd76e0189377d` | `b7006818ba2d53af0af7b554bbfd2544` | 0.0→**92.1** | Soft-FAIL invent Soft-FAIL stack overwrite Soft-FAIL hud copy |
| `pallas1` | `vehicle_pallas1_s2.png` | `e4eaaf898acff155cfd03626d6b5570e` | `4f9ae048a3b1c92c9dbcc8387dbd7c76` | 0.0→**92.2** | Soft-FAIL invent Soft-FAIL stack overwrite Soft-FAIL hud copy |
| `pallas1` | `vehicle_pallas1_booster.png` | `5cc9d63c18e71cf922ecd76e0189377d` | `b7006818ba2d53af0af7b554bbfd2544` | 0.0→**92.0** | Soft-FAIL invent Soft-FAIL stack overwrite Soft-FAIL hud copy |
| `pallas1` | `vehicle_pallas1_ship.png` | `971f34e0134ddc4a5553d9ff6a780fc8` | `0bdb4f25429f0f575eb205c999695ee8` | 0.0→**91.8** | Soft-FAIL invent Soft-FAIL stack overwrite Soft-FAIL hud copy |
| `zq3` | `vehicle_zq3_s1.png` | `4a31becb4745ee01399e03e7d6d8ea8d` | `1d40e57508c8028c2c65ccebbcc07fe1` | 2.9→**96.4** | Soft-FAIL invent Soft-FAIL stack overwrite Soft-FAIL hud copy |
| `zq3` | `vehicle_zq3_s2.png` | `85cba4102187e3056446d8773714b6db` | `01238e32c74d6c6472219df3f5cbce2c` | 3.8→**96.4** | Soft-FAIL invent Soft-FAIL stack overwrite Soft-FAIL hud copy |
| `zq3` | `vehicle_zq3_booster.png` | `4a31becb4745ee01399e03e7d6d8ea8d` | `1d40e57508c8028c2c65ccebbcc07fe1` | 3.0→**96.5** | Soft-FAIL invent Soft-FAIL stack overwrite Soft-FAIL hud copy |
| `zq3` | `vehicle_zq3_ship.png` | `b76e2530b1c2dbe0c493af17b9286fd0` | `84dd1021ca07b5800345de30ec258349` | 3.2→**96.2** | Soft-FAIL invent Soft-FAIL stack overwrite Soft-FAIL hud copy |

## Success criteria
- G1 fill-under Soft-PASS shows ochre HTPB through windows Soft-FAIL black fake Soft-PASS
- Fleet table Soft-PASS complete Soft-PASS
- Soft-FAIL hud/Ada/ship Soft-PASS held Soft-FAIL stack overwrite Soft-PASS held

**FAIL remaining families:** none
**Punched plates:** 61

