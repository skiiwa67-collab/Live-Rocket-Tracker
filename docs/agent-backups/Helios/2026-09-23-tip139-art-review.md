# tip139 Helios art review — 2026-09-23

**Reviewed:** 2026-09-23 07:25 CT (Nuri Soft-PASS + ALL FOUR Soft-PASS)
**Bar:** Starship / CZ-8A GOLD
**Chris HARD:** tip139 requires ALL FOUR PASS — Soft-PASS now.
**Device/Studio HOLD:** Soft-PASS held until Chris go.

## Gate status

- tip139_eligible: `['cz6a', 'sr75', 'nuri', 'h3']`
- HOLD: `[]`
- **all_four_gate: Soft-PASS**
- Ada Soft-PASS GO: rebuild tip139 / 1.0.129 WITH all four GOLD packs + catalog wires. Soft-FAIL phone/Studio push. Stage APK + report. Dual-write contents Soft-PASS.

## Vehicles

### Long March 6A (`cz6a`) — **PASS**

- stack md5 `17ad81c62235dd204a535e6f2519e50e`
- cutaway md5 `6dd047856ac7bfb7c538d350d77f8d41`
- shell md5 `037d0d9310b1813b7cf0e806e12b44d5`
- proof md5 `6fdd88e99b2a278d07a76a214444d327`
- stack: `/workspace/hud-assets/vehicle_cz6a_stack.png`
- cutaway: `/workspace/hud-assets/vehicle_cz6a_cutaway.png`
- shell: `/workspace/hud-assets/vehicle_cz6a_shell.png`
- proof: `/workspace/hud-assets/_proof_cz6a.png`
- ready: `/workspace/lrt-cz6a-pack-20260923/READY.md`
- Findings:
  - Proof sheet matches CZ-8A GOLD layout: shell/cutaway/s1/ship/stack+flame/SRB + phone-slot mocks.
  - Orange LM hull + 4 solid SRBs + kerolox fills (cyan/amber) coherent with MANIFEST published facts.
  - Measured md5s match READY.md claims for stack/cutaway/shell/proof.
  - Vector SS×6 + LANCZOS feel; Soft-FAIL propane-pill not observed on proof.
  - Catalog wire still Ada-owned (cz6a MISSING) — art PASS only, not catalog ship.

### HyImpulse SR75 Flight 2 (`sr75`) — **PASS**

- stack md5 `4cb9b75bac4ea7b8c92166181460718d`
- cutaway md5 `957417a271334d47c873e85e852d6bd2`
- shell md5 `e606603e8b384e2dbaf948a20696a11f`
- proof md5 `b584d56069e25cf65e2172451578f624`
- stack: `/workspace/hud-assets/vehicle_sr75_stack.png`
- cutaway: `/workspace/hud-assets/vehicle_sr75_cutaway.png`
- shell: `/workspace/hud-assets/vehicle_sr75_shell.png`
- proof: `/workspace/hud-assets/_proof_sr75.png`
- ready: `/workspace/lrt-sr75-pack-20260923/READY.md`
- Findings:
  - Proof sheet matches Starship/CZ-8A GOLD layout with white composite hull, aft fins, hybrid paraffin/LOX fills.
  - Single HyPLOX-75 plume + shell/cutaway/stack trio present; phone-slot mocks Soft-PASS.
  - Measured md5s match Da Vinci READY claims (stack 4cb9b75b…).
  - Single-stage suborbital proportions coherent with MANIFEST (~12.5 m / Ø0.64 m).
  - Catalog wire still Ada-owned (sr75 MISSING) — art PASS only.

### Nuri / KSLV-2 (KARI) (`nuri`) — **PASS**

- stack md5 `9b055e356a44dd7a70a5ba74f8550693`
- cutaway md5 `36adc443daf67c643ed1f00b492cb497`
- shell md5 `bebeff2a52a31921d421b6c525107419`
- proof md5 `304015dc7fad2d43b7e49764ede92b6d`
- stack: `/workspace/hud-assets/vehicle_nuri_stack.png`
- cutaway: `/workspace/hud-assets/vehicle_nuri_cutaway.png`
- shell: `/workspace/hud-assets/vehicle_nuri_shell.png`
- proof: `/workspace/hud-assets/_proof_nuri.png`
- ready: `/workspace/lrt-nuri-pack-20260923/READY.md`
- vehiclespec: `/workspace/lrt-nuri-pack-20260923/ADA_VEHICLESPEC.md`
- Findings:
  - Proof sheet matches Starship/CZ-8A GOLD layout: shell/cutaway/s1/ship/stack+flame/upper + phone-slot mocks.
  - 3-stage kerolox white hull Soft-PASS; LOX cyan + fuel amber fills Soft-PASS; 4×KRE-075 plume Soft-PASS; no SRBs Soft-PASS.
  - Measured md5s match Da Vinci READY claims.
  - Proportions coherent with READY published facts (47.2 m / S1 Ø3.5 m).
  - Catalog id=nuri MISSING — Ada wire after all-four Soft-PASS.

### H3-24 Martian Moons eXploration (MMX) (`h3`) — **PASS**

- stack md5 `0716e244a5ca17668ed908ca4c640673`
- cutaway md5 `de1c01cbdef94ff56ac9c950d45c243a`
- shell md5 `38b6a173c2fbd6f8c69d653c95abe36f`
- proof md5 `32131fd2cdf5b3d54b4c5714aa73caa8`
- stack: `/workspace/hud-assets/vehicle_h3_stack.png`
- cutaway: `/workspace/hud-assets/vehicle_h3_cutaway.png`
- shell: `/workspace/hud-assets/vehicle_h3_shell.png`
- proof: `/workspace/hud-assets/_proof_h3.png`
- ready: `/workspace/lrt-h3-pack-20260923/READY.md`
- Findings:
  - Proof sheet matches Starship/CZ-8A GOLD layout: shell/cutaway/s1/ship/stack+flame/SRB + phone-slot mocks.
  - White H3 hull + 4× orange SRB-3 + hydrolox fills (LOX cyan + LH2 pale) coherent with READY published facts (63 m / Ø5.2 m).
  - Measured md5s match Da Vinci READY claims for stack/cutaway/shell/proof.
  - Catalog id=h3 already Soft-PASS in VehicleCatalog — art PASS confirms Ada refresh path.
  - Vector SS×6 + LANCZOS feel Soft-PASS; Soft-FAIL propane-pill not observed on proof.

