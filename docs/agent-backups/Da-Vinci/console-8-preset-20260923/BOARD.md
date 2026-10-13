# BOARD — Console 8 presets COMPACT (Da Vinci Soft-PASS)

**Date:** 2026-09-23 CT
**Composite:** `BOARD_8_presets_compact.png`
**Also:** `BOARD_8_presets.png` (same Soft-PASS compact overwrite)
**Fit mocks:** `mock_cmd_flyout_8_fit.png` · `mock_cmd_flyout_8_narrow.png`
**Master size:** Soft-PASS `200×56` (Soft-FAIL fat 320×120)

## Locked layout (short Soft-PASS labels)

```
Row 1:  1 MCC  |  2 ROS  |  3 SPACEX  |  4 NASA
Row 2:  5 CNSA  |  6 ARIANE  |  7 R.LAB [PROPOSED]  |  8 ULA [PROPOSED]
```

Roster unchanged Soft-PASS: MCC · Roscosmos · SpaceX · NASA · CNSA · Arianespace · Rocket Lab · ULA

## Soft-PASS dynamic scale (Ada)

- Soft-PASS XML shape drawables in **dp** (Soft-FAIL ship fixed 320px bitmaps as sole Soft-PASS size).
- Suggest 2 LinearLayouts (row1/row2) Soft-PASS each 4 buttons Soft-PASS `0dp` + `weight=1` Soft-PASS height **40–48dp** Soft-PASS textSize **~11–12sp**.
- Soft-PASS proof: fit mock + narrow (~360dp-class) Soft-PASS readable.

## Notes

- MCC amber lit Soft-PASS distinct from NASA blue Soft-PASS.
- Slots 7–8 tiny **PROPOSED** badge Soft-PASS.
- Soft-FAIL long MISSION CONTROL / full Cyrillic crowding on compact.
- Soft-FAIL tip142 / Play / vehicle invent Soft-FAIL ❤️ until Chris Soft-PASS.
- Soft-FAIL overwrite Live-Rocket-Tracker drawable trees Soft-FAIL invent vehicles Soft-FAIL Play.
- Eyes-only pack. Parent pings Helios on READY (Soft-FAIL SendToAgent from executor).

## Chip files

| Slot | Selected | Idle |
|------|----------|------|
| 1 MCC | `chip_01_mcc.png` | `chip_01_mcc_idle.png` |
| 2 ROS | `chip_02_ros.png` | `chip_02_ros_idle.png` |
| 3 SPACEX | `chip_03_spacex.png` | `chip_03_spacex_idle.png` |
| 4 NASA | `chip_04_nasa.png` | `chip_04_nasa_idle.png` |
| 5 CNSA | `chip_05_cnsa.png` | `chip_05_cnsa_idle.png` |
| 6 ARIANE | `chip_06_arianespace.png` | `chip_06_arianespace_idle.png` |
| 7 R.LAB | `chip_07_rocketlab.png` | `chip_07_rocketlab_idle.png` |
| 8 ULA | `chip_08_ula.png` | `chip_08_ula_idle.png` |
