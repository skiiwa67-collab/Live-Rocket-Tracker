# Live Rocket Tracker — closed tester notes (stamp 29)

Sideload this APK. This is **not** a Play production upload. Play testers on versionCode 17 stay on 17. Play 17 stays frozen.

Stamp **20** (`stamp-20-debug`) stays the KEEP for units + AUTO pin.
Stamp **21** (`stamp-21-debug`) is the GISAT look + Electron HUD locks.
Stamp **23** (`stamp-23-debug`) is SIGNED usable WITH the Pixel bar. Do not overwrite it.
Stamp **25** (`stamp-25-debug`) is SIGNED. Auto packs THIS page. Do not overwrite it.
Stamp **26** (`stamp-26-debug`) is SIGNED. Owl AUTO one-mission hack. Do not overwrite it.
Stamp **27** (`stamp-27-debug`) is SIGNED. Live LL2 AUTO/picker window. Do not overwrite it.
Stamp **28** (`stamp-28-debug`) is SIGNED. Do not overwrite it.

This stamp is **29** / **1.0.19**. Restores the stamp 25/26 HUD overlay PiP. Signed two-row header (clock + T-/T+ with status on the side). Not a rebuild of 20–28.

## Header (SIGNED — do not restack)

- Current time at the top. Full gap. Biggest. Dynamic for phone/desk.
- T-/T+ is **one line** under the clock. Units stay (`4h 11m`, not a naked clock). Smaller than the type that ate SIM.
- Status word is on the **side** of that countdown line: **SIM / LIVE / HOLD / GO / IN FLIGHT / PAST**. Historic / demo / just-flew that is not a live GO reads SIM or PAST on the side. Not a third row underneath.

## Overlay PiP (Razr tap-sink + Error 153)

Stamp 27/28 glued a fat YouTube window or a dead `/embed` (Error 153) onto the wallpaper and stole every plate tap.

- HUD overlay is the **stamp 25/26 player**: system Picture-in-Picture over everything, watch URL (not embed), volume lowered in the WebView. Draggable. Mute is JS volume — no AUDIOFOCUS_GAIN.
- Hit-test is **only the visible PiP / player rect**. Touches outside go to CMD CDT TEL STS PAD VID MSK AUTO.
- If system PiP does not take, the activity window **is** the player rect (`FLAG_NOT_TOUCH_MODAL`). Never a full-screen touch interceptor. Never a title-bar-only strip.
- Not the MCC SignIN / YT / search chrome. MCC VID stays one official pane. Sign-in there. Cookies stay in-process.
- Minimize / back / exit from MCC leaves this overlay playing on the HUD.
- VID LIVE / LINKS Click Me switch **this** overlay. Same window.

## Still true

- Clock at the top and T-/T+ under it are SIGNED.
- Eight plates: **CMD CDT TEL STS PAD VID MSK AUTO**.
- No AUDIOFOCUS_GAIN. YouTube keeps the speaker.
- AUTO / picker is the stamp 27 live LL2 window.
- Glance word reserved: **LIVE / HOLD / GO / IN FLIGHT / PAST / SIM**. Every launch — not an Owl AUTO/layout key.
- LL2: cache upcoming+previous. No refetch on every plate/frame.
