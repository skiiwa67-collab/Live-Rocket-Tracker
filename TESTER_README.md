# Live Rocket Tracker — closed tester notes (stamp 32)

Sideload this APK. This is **not** a Play production upload. Play testers on versionCode 17 stay on 17. Play 17 stays frozen.

Stamp **20** (`stamp-20-debug`) stays the KEEP for units + AUTO pin.
Stamp **21** (`stamp-21-debug`) is the GISAT look + Electron HUD locks.
Stamp **23** (`stamp-23-debug`) is SIGNED usable WITH the Pixel bar. Do not overwrite it.
Stamp **25** (`stamp-25-debug`) is SIGNED. Auto packs THIS page. Do not overwrite it.
Stamp **26** (`stamp-26-debug`) is SIGNED. Last working HUD overlay layout params. Do not overwrite it.
Stamp **27** (`stamp-27-debug`) is SIGNED. Live LL2 AUTO/picker window. Do not overwrite it.
Stamp **28** (`stamp-28-debug`) is SIGNED. Do not overwrite it.
Stamp **29** (`stamp-29-debug`) is SIGNED. Do not overwrite it.
Stamp **30** (`stamp-30-debug`) is SIGNED. Do not overwrite it.
Stamp **31** (`stamp-31-debug`) is SIGNED. Do not overwrite it.

This stamp is **32** / **1.0.22**. HUD overlay is a 280×168 dp FloatingVideoWindow. Not a rebuild of 20–31.

## Header (SIGNED — do not restack)

- Current time at the top. Full gap. Biggest. Dynamic for phone/desk.
- T-/T+ is **one line** under the clock. Units stay (`4h 11m`, not a naked clock). Smaller than the type that ate SIM.
- Status word is on the **side** of that countdown line: **SIM / LIVE / HOLD / GO / IN FLIGHT / PAST**. Historic / demo / just-flew that is not a live GO reads SIM or PAST on the side. Not a third row underneath.

## Overlay player (stamp 32)

Stamp 31 still called `enterPictureInPictureMode` with `setSourceRectHint(Rect(0, 0, 16, 9))` — a 16×9 **pixel** hint. When the WebView did not paint (Error 153 / YouTube cache), Android system PiP collapsed to a SignIN YT − + X sliver. Copying stamp 26 files did not help because stamp 26 did this `enterPip()`.

- **Do not** call `enterPictureInPictureMode`. The player **is** the 280×168 dp `FloatingVideoWindow`.
- Activity window is that rect. `FLAG_NOT_TOUCH_MODAL` — taps outside hit CMD CDT TEL STS PAD VID MSK AUTO.
- − / + resize **that** window. X finishes the activity. No second window. No system PiP sliver.
- Load `youtube.com/watch?v=` (not `/embed`). Error 153 → desktop UA on watch, then `watch?v=…&app=desktop`.
- MCC one-pane SignIN stays in MCC. HUD overlay is a player well, not SignIN YT as the product.
- VID REPLAY / LINKS Click Me still `OverlayPip.switch` the same activity.
- Always dynamic. No Owl special-case.

## Still true (later stamps, not overlay)

- Clock at the top and T-/T+ under it are SIGNED.
- Eight plates: **CMD CDT TEL STS PAD VID MSK AUTO**.
- AUTO / picker is the stamp 27 live LL2 window.
- Glance word reserved: **LIVE / HOLD / GO / IN FLIGHT / PAST / SIM**. Every launch — not an Owl AUTO/layout key.
- LL2: cache upcoming+previous. No refetch on every plate/frame.
- VID Click Me, packer, catalog stay from stamp 30.
