# READY — tip151 stage-fit Soft-FAIL-named rebuild

**When:** 2026-09-23 CT
**Lane:** Da Vinci art — electron s2 + gravity1 s1/s2/s3/s4
**Soft-FAIL ship / git / device / Play** — pack deliverable only (+ hud/pack mirror for Ada wire).

## Soft-FAIL held
- Soft-FAIL overwrite electron GOLD stack/side/cutaway/shell (stack md5 still `d8ab6a86858da2237a18838c4c4dc91e`)
- Soft-FAIL overwrite gravity1 stack/side
- Soft-FAIL flame/engine overwrite beyond Soft-FAIL-named stage plates
- Soft-FAIL invent Electron fairing geometry / Gravity-1 liquid S4 (s4 remains s3 alias)
- Soft-FAIL F9 MVac / phantom SR75 S2 / kz11+cz2d flame

## Paths
- Pack: `/workspace/agent-backups/Da-Vinci/tip151-stage-fit-20260923`
- Mirrored: `/workspace/hud-assets/` + `lrt-electron-pack` / `lrt-gravity1-pack` (stage plates only)

## old → new md5 + opaque bb/aspect

| asset | old md5 | new md5 | before | after |
|-------|---------|---------|--------|-------|
| `vehicle_electron_s2.png` | `7cc527e09709164065069b722750653b` | `033df399dced71b089c0c44157269fe8` | bb 61×185 asp 3.033 (top_pad=238 bot_pad=777) | bb 158×245 asp 1.551 (top_pad=156 bot_pad=799) |
| `vehicle_gravity1_s2.png` | `faed4d90ba372c844fd14515e5cd5d18` | `0cceb567adc983592d0205d7e1cb2ee0` | bb 297×244 asp 0.822 (top_pad=443 bot_pad=513) | bb 151×258 asp 1.709 (top_pad=438 bot_pad=504) |
| `vehicle_gravity1_s1.png` | `0f147b073a3ab67aa289b71dfcbc5ecd` | `6638905cfa0cb545a9b900082f967154` | bb 297×365 asp 1.229 (top_pad=669 bot_pad=166) | bb 221×320 asp 1.448 (top_pad=688 bot_pad=192) |
| `vehicle_gravity1_s3.png` | `f265f0ada5eb1ec8bb6c5bec9424cfbc` | `cae31922d84a64e10d8934ded4f4287b` | bb 165×351 asp 2.127 (top_pad=336 bot_pad=513) | bb 158×238 asp 1.506 (top_pad=286 bot_pad=676) |
| `vehicle_gravity1_s4.png` | `cae1ac2a7964548cb7d8c5f1a457d051` | `cae31922d84a64e10d8934ded4f4287b` | bb 165×351 asp 2.127 (top_pad=336 bot_pad=513) | bb 158×238 asp 1.506 (top_pad=286 bot_pad=676) |

## Notes
- **electron s2:** anisotropic scale toward peer STG2 median asp (~1.55) + taller bb (fixes TALL≈3.0 / LOW_BH). Published carbon Rutherford look preserved (resample of existing plate). Soft-FAIL ship alias / stack.
- **gravity1 s2:** stripped SRB-top bleed (core-only M2) that caused SHORT asp 0.822; scaled to peer-ish plate fill.
- **gravity1 s3/s4:** corrected prior mega-band duplicate; s3 = M3 core-only; s4 = alias of s3 (baseline Soft-FAIL invent liquid S4).
- **gravity1 s1:** M1+SRBs republished plate with useful fill scale; published Orienspace look kept.
- Preview board: `/workspace/agent-backups/Da-Vinci/tip151-stage-fit-20260923/preview_stage_fit_board.png`
- Electron GOLD stack md5 (unchanged): `d8ab6a86858da2237a18838c4c4dc91e`

## Success criteria
- electron s2 asp 3.033 → **1.551** (no longer ~3.0 TALL outlier): PASS
- gravity1 s2 asp 0.822 → **1.709** (no longer SHORT 0.822): PASS
- GOLD electron stack untouched: **PASS** `d8ab6a86858da2237a18838c4c4dc91e`
- Soft-FAIL ship: **HELD**
