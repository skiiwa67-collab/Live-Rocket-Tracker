# 2026-09-23 — Pipeline + gold-art audit (Ada)

**Phone:** 138 / 1.0.128 (HOLD device pushes — HARD_DEVICE_PUSH_GO)  
**Git tiphead:** `stamp-85` @ `92911c5` (docs) · code tip138 `bbe7959`/`dd591e1`/`e603435`  
**Compile:** `compileDebugKotlin` Soft-PASS (box)

## 1) Pipeline after tip138

| Tip | Status | One-line |
|-----|--------|----------|
| 134 | shipped | #48 HUD plates FakeBold / CASC EN / PAD mark |
| 135 | shipped | CZ-8A gold pack |
| 136 | shipped | entertainment-only wallpaper disclaimer |
| 137 | shipped | PAD version mark larger (20–32px) |
| 138 | shipped + on phone | fetch queue while in-flight + cold 2s retry + FETCHING label |
| **139+** | **none queued** | Ada not building/bumping anything |

**Staged (no tip number yet):**
- **CZ-6A GOLD** — Da Vinci Soft-PASS prep READY. Drawables mirrored into RCC tree as **untracked** files. **VehicleCatalog `cz6a` MISSING** — wire HOLD until Chris/Helios Soft-PASS tip GO. Launch NET ~Sep 24 08:45 UTC Taiyuan.
- Device push HOLD until Chris clears.

## 2) Gold-art path audit

### CZ-6A
- Pack: `/workspace/lrt-cz6a-pack-20260923/` READY.md Soft-PASS prep
- md5 stack `17ad81c6…` cutaway `6dd04785…` shell `037d0d93…` — match hud-assets + RCC drawable
- Catalog: **MISSING** `id=cz6a` (ADA_VEHICLESPEC.md draft only — Ada owns Git, not committed)
- VehicleDraw: no `cz6a` branch found
- Untouched Soft-PASS families listed in READY (cz8a etc.)

### H3-24 (MMX)
- Catalog: **wired** `id=h3` + VehicleDraw `"h3"`
- Drawables: partial set (booster/core/s1/s2/ship/tanks/upper) — **no `vehicle_h3_stack.png`** in RCC drawable
- No dedicated `/workspace/lrt-h3*` / MMX gold READY pack found this morning
- Soft-FAIL gold-stack Soft-PASS for H3-24 until Da Vinci drops GOLD pack + md5s

### Nuri / KARI
- Catalog: **no** `nuri` / `kslv` VehicleSpec
- Drawables/hud-assets: **none** named nuri/kslv
- `/workspace/lrt-nuri-pack-20260923/` exists but **empty**
- Soft-FAIL — pack not Soft-PASS’d / not present

### SR-75 Flight 2
- Assets present: `/workspace/lrt-sr75-pack/` + hud-assets + RCC drawable (`vehicle_sr75_stack.png` md5 `2b3a4a14…`)
- Catalog: **no** `sr75` VehicleSpec / VehicleDraw branch
- No READY.md in pack this morning
- Soft-FAIL wire Soft-PASS — art on disk, Git catalog not wired

## 3) Recent-tip regression check
- tip138 compile Soft-PASS
- Untracked `vehicle_cz6a*` in working tree (art mirror) — do **not** commit without tip GO
- HARD: no adb / no Studio→phone until Chris clears

## Peers
Da Vinci + Saxon pinged for Soft-PASS path/md5 confirmation on H3-24 / Nuri / SR-75 / CZ-6A.
