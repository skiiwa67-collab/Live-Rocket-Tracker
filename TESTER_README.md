# Live Rocket Tracker — closed tester notes (stamp 28)

Sideload this APK. This is **not** a Play production upload. Play testers on versionCode 17 stay on 17. Play 17 stays frozen.

Stamp **20** (`stamp-20-debug`) stays the KEEP for units + AUTO pin.
Stamp **21** (`stamp-21-debug`) is the GISAT look + Electron HUD locks.
Stamp **23** (`stamp-23-debug`) is SIGNED usable WITH the Pixel bar. Do not overwrite it.
Stamp **25** (`stamp-25-debug`) is SIGNED. Auto packs THIS page. Do not overwrite it.
Stamp **26** (`stamp-26-debug`) is SIGNED. Owl AUTO one-mission hack. Do not overwrite it.
Stamp **27** (`stamp-27-debug`) is SIGNED. Live LL2 AUTO/picker window. Do not overwrite it.

This stamp is **28** / **1.0.18**. Overlay PiP player well. Not a rebuild of 20–27.

## HUD look (queued after PiP)

- VID LIVE / LINKS are real targets (bigger type, Click Me). Official watch if LL2 has one, else related historic Electron replay. Taps switch the **clean overlay PiP**, not a fat YouTube pane.
- STS / MSK spread big in the well. Published pages only (LL2 info/wiki, or the official Rocket Lab Owl sheet). Taps are ACTION_VIEW like PAD geo.
- Tape takes a packer share above `dockFloor()` so L/O MAXQ SECO DEPLOY stay readable. Now = wall-clock. After the book, now sits at the right. No invented past marks.
- ACCEL / ENG labels sit under the analog cans, not at the band floor toward the dock.
- Clock is still the biggest. T-/T+ is next. A glance word is reserved: **LIVE / HOLD / GO / IN FLIGHT / PAST / SIM**. A Success hours after NET is PAST, not live GO. Every launch — not an Owl AUTO/layout key.

## Overlay PiP (the Razr bug)

Stamp 27 HUD VID was a collapsed title bar (SignIN YT − + X, no player) or a full-screen YouTube pane that stole every plate tap.

- HUD overlay is a **small floating window**: embed player + thin chrome (drag, mute, −, +, X).
- The activity window **is** the PiP rect. Touches outside go to CMD CDT TEL STS PAD VID MSK AUTO.
- Not Android system Picture-in-Picture (that cropped the well to zero height).
- Not the MCC YouTube home/search pane glued to the wallpaper.
- MCC VID stays one official pane first. Sign-in there. Cookies stay in-process.
- Minimize / back / exit from MCC leaves this overlay playing on the HUD. No second dead SignIN.

## Still true

- Clock at the top and T-/T+ under it are SIGNED.
- Eight plates: **CMD CDT TEL STS PAD VID MSK AUTO**.
- No AUDIOFOCUS_GAIN. YouTube keeps the speaker. Overlay mute is JS only.
- AUTO / picker is the stamp 27 live LL2 window.
- LL2: cache upcoming+previous. No refetch on every plate/frame. Throttle backs off minutes and keeps last good cache. lldev only after that backoff.
