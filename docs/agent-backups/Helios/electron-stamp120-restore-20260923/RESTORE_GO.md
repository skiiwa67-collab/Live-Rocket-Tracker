# Electron Stamp 120 GOLD restore — Helios GO for Ada

**When:** 2026-09-23 ~08:25 CT
**Bug:** Chris phone tip140 PAD 140 shows Electron Owlright with cyan/orange blob tanks + chewed stack; LIFTOFF/STAGE SEP labels overlap (secondary).
**Root cause:** RetroCommandCenter tiphead @ cead451 has corrupt vehicle_electron_* (15/15 DIFF vs Stamp 120 GOLD). Tank masks are solid yellow/cyan blobs; stack/cutaway PNGs are chewed.
**GOLD source:** `/workspace/Live-Rocket-Tracker/app/src/main/res/drawable/vehicle_electron_*`
**Stack GOLD md5:** `d8ab6a86858da2237a18838c4c4dc91e` (catalog Stamp 120 D8AB6A)
**Staged here:** `/workspace/agent-backups/Helios/electron-stamp120-restore-20260923`

## Ada DO NOW (Studio ONLY)
1. Copy ALL `vehicle_electron*` from this restore dir into MiniME Studio RetroCommandCenter `app/src/main/res/drawable/` AND box tiphead `/workspace/RetroCommandCenter/app/src/main/res/drawable/` (same set).
2. Verify `vehicle_electron_stack.png` md5 == `d8ab6a86858da2237a18838c4c4dc91e`.
3. Bump tip (tip141 / 1.0.131 suggested) — Electron art restore only; Soft-FAIL invent other changes.
4. Studio sync ONLY. Soft-FAIL Play / adb until Chris go.
5. Report when Studio loadable so Helios can tell Chris Studio is ready to sync and load onto phone.

## Heeds
- Soft-FAIL re-import Da Vinci nozzle-catalog electron stack/cutaway/tanks (those overwrote hud/pack at 08:14 CT; GOLD tanks/stack live in Live-Rocket-Tracker + this restore).
- Engine_* Starship-bar nozzles stay eyes-only until Chris hearts full-catalog.
- Soft-FAIL Play forever.
