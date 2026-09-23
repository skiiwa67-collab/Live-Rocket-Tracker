# LRT console 8-preset audit — 2026-09-23 (America/Chicago)

**Searcher:** Grok Bot (Helios subagent)  
**Owner:** Chris Olsen  
**Target:** MAIN APP settings + CMD flyout **CONSOLE color preset boxes**  
**Rules:** Soft-FAIL invent · Soft-FAIL dual-write · Soft-FAIL Play

---

## Verdict (one screen)

| Question | Call |
|----------|------|
| Da Vinci / Helios **finished 8-box preset row art** on disk? | **Soft-FAIL — NO** |
| Wire-ready for tip142? | **Soft-FAIL — NO** |
| Wired console rockers today | **3** — MCC · ROS · CLEAR |
| Agency live skins (`TelemetrySkin.forLaunch`) | **10** token packs (auto by bird; **not** settings rockers) |
| Chris 2026-09-19 8-theme roster remembered? | **Soft-PASS** (voice + Ada JSON) |
| Theme dumps for NASA/CNSA/Arianespace? | **HELD** (sheet Soft-FAIL) |

---

## Chris 2026-09-19 8-theme FINAL (memory Soft-PASS)

Sources:

- `/workspace/agent-backups/helios/voice-exports/2026-09-19/02-0555-long-march-launch-graphics-bugs.md` (and `_for_mcp_upload` parts)
- `/workspace/agent-backups/Ada/sessions/2026-09-19/begin.json` + `mid.json` (`theme dumps HELD`, `CLEAR→SpaceX rocker`, `NASA/CNSA/Arianespace rockers`)

| # | Theme | Status |
|---|-------|--------|
| 1 | **MCC** | Keep (favorite / neutral default) |
| 2 | **Roscosmos** | Keep (ROS rocker) |
| 3 | **CLEAR → SpaceX** | Rename / retheme |
| 4 | **NASA** | New rocker |
| 5 | **China / CNSA** | New rocker |
| 6 | **Arianespace** | New rocker |
| 7 | **Rocket Lab** | Proposed for slots 7–8 |
| 8 | **ULA** | Proposed for slots 7–8 |

**Behavior intent:** auto-select provider look on live launch; user can override (e.g. stay on Roscosmos).

---

## What exists — paths + md5

### 1) Soft-FAIL sheet (canonical 2026-09-19)

| Path | md5 |
|------|-----|
| `/workspace/lrt-softfail-review-2026-09-19/VISUAL_SOFT_FAIL_SHEET.md` | `9e16342e3ebf2bc6c600ea6f5e0dd004` |
| `/workspace/lrt-softfail-review-2026-09-19/SOFT_FAIL_REVIEW.md` | (companion one-pager) |

Sheet truth:

- Flyout rockers = **MCC \| ROS \| CLEAR only**
- SpaceX / NASA / CNSA / Arianespace = **`TelemetrySkin.forLaunch` agency bird skins**, **NOT** rockers
- Theme art dumps = **HELD**

Proofs:

| Path | md5 |
|------|-----|
| `/workspace/lrt-softfail-review-2026-09-19/proofs/13-cmd-flyout-ROS-MCC-CLEAR.png` | `c68766ad7b95565c2f46e9cec102661b` |
| `/workspace/lrt-softfail-review-2026-09-19/proofs/14-MCC-tel-analog.png` | `e7788d29314b941df6750dbed57676d6` |

### 2) Chris art-review themes (keepers — not an 8-chip UI mock)

Dir: `/workspace/chris-art-review/themes/`

| File | md5 | Role vs 8-preset goal |
|------|-----|------------------------|
| `01-cmd-flyout-Roscosmos.png` | `c68766ad7b95565c2f46e9cec102661b` | Soft-PASS 3-rocker flyout (same pixels as proof 13) |
| `02-settings-Roscosmos-console.png` | `a8cac450fb3e6a08be6ba52925a3023f` | Settings console Roscosmos look |
| `03-MCC-tel-analog-gauges.png` | `e7788d29314b941df6750dbed57676d6` | MCC tel (same as proof 14) |
| `04-MCC-traj-baikonur.png` | `8eb8dddad26e9f0c53c201e75f878102` | MCC traj keeper |
| `05-bg-console-Roscosmos-wide.png` | `c6b60884b3af2b073dc334f3fe5d465a` | Roscosmos bg |
| `06-HUD-Roscosmos-Soyuz.png` | `b049ce6263164a001fe355cc5ecd55a3` | HUD keeper |
| `07-China-Jiuquan-map.png` | `6f23d91e4ae20d501e73e39981382fb7` | China map — **agency ref, not rocker chip** |
| `08-SpaceX-F9-stg2-plume-keeper.png` | `4146eb9af237f899599f86fa6efd6a62` | SpaceX plume — **agency ref, not rocker chip** |
| `09-stamp20-MCC-shot.png` | `d7b96320d9e3e4575051d7a7781ed2e2` | MCC stamp |
| `10-mcc1.png` | `c87a69bbf5d9c5cce24caa3beab04003` | MCC |

**Not found:** NASA / CNSA / Arianespace / Rocket Lab / ULA **rocker chip** packs; CLEAR→SpaceX retheme pack; any Soft-PASS **8-box preset row** mock PNG.

### 3) RetroCommandCenter code Soft-PASS (now)

**Console rockers (settings + CMD flyout):** **3**

- Prefs: `AppPrefs.CONSOLE_SKIN_MCC` / `CONSOLE_SKIN_ROS` / `CONSOLE_SKIN_CLEAR`  
  → `/workspace/RetroCommandCenter/app/src/main/java/com/ccos/retro/model/AppPrefs.kt`
- Layout: `activity_main.xml` `section_console_skin` → `btn_console_mcc` / `_ros` / `_clear` (one horizontal row)
- Drawables (only these chip XMLs):
  - `res/drawable/chip_console_selected_mcc.xml`
  - `res/drawable/chip_console_selected_ros.xml`
  - `res/drawable/chip_console_selected_clear.xml`
  - `res/drawable/chip_console_idle.xml`
  - `res/drawable/chip_console_ros_idle.xml`
- Wiring: `MainActivity` `applyConsoleSkin` / `wireConsoleSkin`; wallpaper CMD popout uses same 3 IDs (Stamp 88)

**Agency telemetry skins (`TelemetrySkin` tokens):** **10** — auto via `forLaunch`, **not** settings rockers

1. `spacex`  
2. `nasa`  
3. `chinese`  
4. `roscosmos`  
5. `blueOrigin`  
6. `esa`  
7. `rocketLab`  
8. `isro`  
9. `jaxa`  
10. `generic`  

File: `/workspace/RetroCommandCenter/app/src/main/java/com/ccos/retro/skin/TelemetrySkin.kt`  
Also: `SystemSkin.kt` = console chrome palettes for MCC/ROS/CLEAR path.

### 4) Dropbox MCP `/Grok-Agent-Backups`

Voice-export mirrors of the 2026-09-19 Long March / theme-job thread only.  
**No** binary 8-box console-preset PNG pack found.

### 5) GitHub `Live-Rocket-Tracker` code search

No extra `chip_console_*` / 8-preset docs beyond the 3-rocker tree already on the box.

### 6) Notion

No Soft-PASS “8 console preset boxes” art page. Related LRT tips exist; none deliver console 8-box art.

---

## Gauge-board themes — SEPARATE (Soft-FAIL as LRT settings)

`/workspace/ai-live-gauge-board-art-themes-r12/` — Leonardo / Cursor / GitHub board chrome Soft-PASS r12 (`SOFT_PASS_R12.md`).

These are **NOT** LRT settings console presets. Staged for Helios under:

`/workspace/agent-backups/Helios/console-preset-previews/_NOT-LRT-gauge-board-themes/`

---

## Previews copied for Helios attach

Dir: `/workspace/agent-backups/Helios/console-preset-previews/`

| Preview | md5 |
|---------|-----|
| `01-cmd-flyout-MCC-ROS-CLEAR.png` | `c68766ad7b95565c2f46e9cec102661b` |
| `02-settings-console-Roscosmos.png` | `a8cac450fb3e6a08be6ba52925a3023f` |
| `03-MCC-tel-analog-gauges.png` | `e7788d29314b941df6750dbed57676d6` |
| `05-bg-console-Roscosmos-wide.png` | `c6b60884b3af2b073dc334f3fe5d465a` |
| `07-China-Jiuquan-map-agency-ref.png` | `6f23d91e4ae20d501e73e39981382fb7` |
| `08-SpaceX-F9-plume-agency-ref.png` | `4146eb9af237f899599f86fa6efd6a62` |
| `09-stamp20-MCC-shot.png` | `d7b96320d9e3e4575051d7a7781ed2e2` |
| `10-mcc1.png` | `c87a69bbf5d9c5cce24caa3beab04003` |
| `13-cmd-flyout-ROS-MCC-CLEAR-proof.png` | `c68766ad7b95565c2f46e9cec102661b` |
| `14-MCC-tel-analog-proof.png` | `e7788d29314b941df6750dbed57676d6` |

---

## Soft-FAIL tip142 wire-ready? Missing + ETA

**Not wire-ready** for an 8-preset console row on tip142.

### Missing

1. Da Vinci **theme dump pack** for rockers 3–8 (SpaceX retheme of CLEAR, NASA, CNSA, Arianespace, Rocket Lab, ULA) — **HELD** since 2026-09-19  
2. Soft-PASS **8-box / 2-row preset UI mock** file (Chris remembers a layout; no finished 8-chip row PNG found)  
3. Ada prefs + layout + wallpaper Stamp 88 expansion beyond `MCC|ROS|CLEAR`  
4. New `chip_console_*` drawables (selected + idle) for added labels  
5. Chris lock on slots 7–8 if still TBD (Rocket Lab + ULA were proposed)

### Suggested ETA after Helios review / Chris GO (not started; Soft-FAIL invent)

| Wave | Work | ETA |
|------|------|-----|
| A | Da Vinci: 8 rocker chip art + color tokens matching MCC/ROS/CLEAR stamp language | ~0.5–1 day art |
| B | Ada: prefs IDs + 2×4/wrap row + wallpaper CMD rockers + auto-select from `TelemetrySkin.forLaunch` with sticky override | ~0.5–1 day code |
| C | MiniME soak proofs ×8 | ~0.5 day |

---

## Search coverage

- [x] `/workspace/agent-backups/` Da-Vinci / Helios / Ada / team  
- [x] `/workspace/lrt-softfail-review-2026-09-19/`  
- [x] `/workspace/chris-art-review/themes/`  
- [x] Dropbox `/Grok-Agent-Backups`  
- [x] GitHub Live-Rocket-Tracker code search  
- [x] Notion keyword search  
- [x] RetroCommandCenter `chip_console*`, `TelemetrySkin`, `SystemSkin`, `section_console_skin`  
- [x] Gauge-board themes noted separately  

---

## Bottom line for Helios

Chris’s **8-company console plan Soft-PASS in memory** (2026-09-19 FINAL).  
**Shipped UI Soft-PASS:** still **3 rockers**.  
**Agency color Soft-PASS:** **10** `TelemetrySkin` packs already auto-apply by launch — that half exists; **settings/CMD rocker expansion + theme dumps Soft-FAIL / HELD**.  

This agent: Soft-FAIL invent · Soft-FAIL dual-write. Helios dual-writes after review.

## Appendix — live md5 dump (generated 2026-09-23)

### chris-art-review/themes
```
c68766ad7b95565c2f46e9cec102661b  /workspace/chris-art-review/themes/01-cmd-flyout-Roscosmos.png
a8cac450fb3e6a08be6ba52925a3023f  /workspace/chris-art-review/themes/02-settings-Roscosmos-console.png
e7788d29314b941df6750dbed57676d6  /workspace/chris-art-review/themes/03-MCC-tel-analog-gauges.png
8eb8dddad26e9f0c53c201e75f878102  /workspace/chris-art-review/themes/04-MCC-traj-baikonur.png
c6b60884b3af2b073dc334f3fe5d465a  /workspace/chris-art-review/themes/05-bg-console-Roscosmos-wide.png
b049ce6263164a001fe355cc5ecd55a3  /workspace/chris-art-review/themes/06-HUD-Roscosmos-Soyuz.png
6f23d91e4ae20d501e73e39981382fb7  /workspace/chris-art-review/themes/07-China-Jiuquan-map.png
4146eb9af237f899599f86fa6efd6a62  /workspace/chris-art-review/themes/08-SpaceX-F9-stg2-plume-keeper.png
d7b96320d9e3e4575051d7a7781ed2e2  /workspace/chris-art-review/themes/09-stamp20-MCC-shot.png
c87a69bbf5d9c5cce24caa3beab04003  /workspace/chris-art-review/themes/10-mcc1.png
```

### softfail proofs + sheet
```
c68766ad7b95565c2f46e9cec102661b  /workspace/lrt-softfail-review-2026-09-19/proofs/13-cmd-flyout-ROS-MCC-CLEAR.png
e7788d29314b941df6750dbed57676d6  /workspace/lrt-softfail-review-2026-09-19/proofs/14-MCC-tel-analog.png
9e16342e3ebf2bc6c600ea6f5e0dd004  /workspace/lrt-softfail-review-2026-09-19/VISUAL_SOFT_FAIL_SHEET.md
```

### Helios console-preset-previews (curated copies)
```
c68766ad7b95565c2f46e9cec102661b  /workspace/agent-backups/Helios/console-preset-previews/01-cmd-flyout-MCC-ROS-CLEAR.png
a8cac450fb3e6a08be6ba52925a3023f  /workspace/agent-backups/Helios/console-preset-previews/02-settings-console-Roscosmos.png
e7788d29314b941df6750dbed57676d6  /workspace/agent-backups/Helios/console-preset-previews/03-MCC-tel-analog-gauges.png
c6b60884b3af2b073dc334f3fe5d465a  /workspace/agent-backups/Helios/console-preset-previews/05-bg-console-Roscosmos-wide.png
6f23d91e4ae20d501e73e39981382fb7  /workspace/agent-backups/Helios/console-preset-previews/07-China-Jiuquan-map-agency-ref.png
4146eb9af237f899599f86fa6efd6a62  /workspace/agent-backups/Helios/console-preset-previews/08-SpaceX-F9-plume-agency-ref.png
d7b96320d9e3e4575051d7a7781ed2e2  /workspace/agent-backups/Helios/console-preset-previews/09-stamp20-MCC-shot.png
c87a69bbf5d9c5cce24caa3beab04003  /workspace/agent-backups/Helios/console-preset-previews/10-mcc1.png
c68766ad7b95565c2f46e9cec102661b  /workspace/agent-backups/Helios/console-preset-previews/13-cmd-flyout-ROS-MCC-CLEAR-proof.png
e7788d29314b941df6750dbed57676d6  /workspace/agent-backups/Helios/console-preset-previews/14-MCC-tel-analog-proof.png
```

### chip_console drawables present
```
/workspace/RetroCommandCenter/app/src/main/res/drawable/chip_console_idle.xml
/workspace/RetroCommandCenter/app/src/main/res/drawable/chip_console_ros_idle.xml
/workspace/RetroCommandCenter/app/src/main/res/drawable/chip_console_selected_clear.xml
/workspace/RetroCommandCenter/app/src/main/res/drawable/chip_console_selected_mcc.xml
/workspace/RetroCommandCenter/app/src/main/res/drawable/chip_console_selected_ros.xml
```
