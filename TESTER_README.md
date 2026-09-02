# Live Rocket Tracker — closed tester notes (stamp 31)

Sideload this APK. This is **not** a Play production upload. Play testers on versionCode 17 stay on 17. Play 17 stays frozen.

Stamp **20** (`stamp-20-debug`) stays the KEEP for units + AUTO pin.
Stamp **21** (`stamp-21-debug`) is the GISAT look + Electron HUD locks.
Stamp **23** (`stamp-23-debug`) is SIGNED usable WITH the Pixel bar. Do not overwrite it.
Stamp **25** (`stamp-25-debug`) is SIGNED. Auto packs THIS page. Do not overwrite it.
Stamp **26** (`stamp-26-debug`) is SIGNED. Last working HUD overlay PiP. Do not overwrite it.
Stamp **27** (`stamp-27-debug`) is SIGNED. Live LL2 AUTO/picker window. Do not overwrite it.
Stamp **28** (`stamp-28-debug`) is SIGNED. Do not overwrite it.
Stamp **29** (`stamp-29-debug`) is SIGNED. Do not overwrite it.
Stamp **30** (`stamp-30-debug`) is SIGNED. Do not overwrite it.

This stamp is **31** / **1.0.21**. HUD overlay player files are byte-for-byte from `stamp-26-debug`. Not a rebuild of 20–30.

## Header (SIGNED — do not restack)

- Current time at the top. Full gap. Biggest. Dynamic for phone/desk.
- T-/T+ is **one line** under the clock. Units stay (`4h 11m`, not a naked clock). Smaller than the type that ate SIM.
- Status word is on the **side** of that countdown line: **SIM / LIVE / HOLD / GO / IN FLIGHT / PAST**. Historic / demo / just-flew that is not a live GO reads SIM or PAST on the side. Not a third row underneath.

## Overlay PiP (stamp 26 player, copied)

Stamp 30 left MCC SignIN chrome (`SignIN YT − + X`) on the HUD with no player. Stamp 31 copies the stamp 26 player files and the stamp 26 SHOW/HIDE/switch call. MCC SignIN stays in MCC. It is not glued onto the HUD.

- `FloatingVideoWindow.kt`, `OverlayPipActivity.kt`, `VideoFeedOverlay.kt` match tag `stamp-26-debug`.
- ONE `FloatingVideoWindow` on the HUD. Watch URL. Volume lowered in the WebView. No AUDIOFOCUS_GAIN.
- Enter Android PiP **once**. Do not re-enter. Do not spawn a second chrome window. −/+ resize this same player.
- VID REPLAY / LINKS Click Me switch **this** overlay. Same window.
- MCC VID stays one official pane. Sign-in there. Cookies stay in-process. Exit MCC does **not** hand a SignIN strip to the HUD.
- Close leaves the HUD. No leftover SignIN YT bar from a second invented window.

## Still true (later stamps, not overlay)

- Clock at the top and T-/T+ under it are SIGNED.
- Eight plates: **CMD CDT TEL STS PAD VID MSK AUTO**.
- AUTO / picker is the stamp 27 live LL2 window.
- Glance word reserved: **LIVE / HOLD / GO / IN FLIGHT / PAST / SIM**. Every launch — not an Owl AUTO/layout key.
- LL2: cache upcoming+previous. No refetch on every plate/frame.
- VID Click Me, packer, catalog stay from stamp 30.
