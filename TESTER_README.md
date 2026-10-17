# Live Rocket Tracker — closed tester notes (stamp 40)

Sideload this APK. This is **not** a Play production upload. Play testers on versionCode 17 stay on 17. Play 17 stays frozen.

Stamp **20** (`stamp-20-debug`) stays the KEEP for units + AUTO pin.
Stamp **21** (`stamp-21-debug`) is the GISAT look + Electron HUD locks.
Stamp **23** (`stamp-23-debug`) is SIGNED usable WITH the Pixel bar. Do not overwrite it.
Stamp **25** (`stamp-25-debug`) is SIGNED. Auto packs THIS page. Do not overwrite it.
Stamp **26** (`stamp-26-debug`) is Owl AUTO / Electron HUD. Do not overwrite it.
Stamp **36** (`stamp-36-debug`) deleted OverlayPiP. Full-screen MCC WebView + sign-in. Do not overwrite it.
Stamp **37** (`stamp-37-debug`) explicit MCC WebView PiP + HUD session reuse. Do not overwrite it.
Stamp **38** (`stamp-38-debug`) inset PIP chip + video-only 16:9 system PiP. Chris SIGNED as nice. Do not overwrite it.
Stamp **39** (`stamp-39-debug`) mute/speaker + brighter HUD grid. Chris SIGNED as not bad. Do not overwrite it.

This stamp is **40** / **1.0.30**. Built from stamp 39 only. OverlayPipActivity / FloatingVideoWindow / SignIN YT − + X bar stay deleted. Do not invent them back.

## Overlay permission (required for the sized window)

Stamp 40 sizes a **chrome-less video window we control** via `WindowManager.LayoutParams`. Motorola system PiP cannot shrink this Razr (pins ~1120x630). First PIP hop will ask for **Display over other apps** (`SYSTEM_ALERT_WINDOW`). Grant it, then PIP again. Without that permission the sized window cannot attach.

## VID (locked cut)

- OverlayPipActivity stays gone. No FloatingVideoWindow title bar. No SPACEX SEARCH SignIN YT − + X chrome. If the window needs a grab, drag the **video surface** itself.
- CommandCenterActivity is the player (`singleTask`). Same WebView session for HUD VID + MCC VID + sized window.
- MCC VID is a full-screen WebView for Google / YouTube sign-in. Password Manager can autofill.
- MCC VID has an explicit **PIP** chip **below** statusBars / cutout. One tap hops the same WebView into the sized window.
- **FULL** = MCC fullscreen (the square) to browse videos. PIP chip hops back to the last sized window.
- Back and EXIT while video is up hop to the sized window.
- HUD VID reuses the same Command Center session.
- No AUDIOFOCUS_GAIN. YouTube wins.

## Sized window (we control size)

- **SMALLEST** ~280×168 (stamp 26 emu). Chrome: **grow-arrows + X only**. No gear, no CC, no mute (too small).
- **MEDIUM** ~1120×630 (current Razr system-PiP size). Chrome: **Mute + CC + gear + shrink/grow + expand-to-full**.
- **FULL** = MCC. Expand/square opens CommandCenterActivity fullscreen.
- Mute keeps stamp 39 behavior (`video.muted` / `yt.player` mute). Speaker works.
- **CC is a real toggle** at medium/full only: injects JS to `yt.player` / `#movie_player` captions **and** clicks the YouTube CC control. Not a no-op RemoteAction.
- Gear clicks the YouTube settings control in the same WebView.
- Drag the video surface to move. No title-bar handle.

## HUD (kept from 39)

- Wallpaper **grid / design art underneath** is a notch brighter so a dark space webcast reads. Plates / clock / chrome not blown out. No catalog rewrite. No clock restack.

## Owl / AUTO (from 26)

Catalog skip of Electron Owl is a bug. AUTO follows HOLD / in-flight / webcast-live. Owl stays in the CMD picker so you can reselect it and re-pin AUTO (double-tap AUTO still pins). Do not hide the current hold/in-flight bird as past.

## Also still true from 25

Eight plates: **CMD CDT TEL STS PAD VID MSK AUTO**.
Unknown vehicle = **NEW VEHICLE**. We do not invent drawings or numbers.
No AUDIOFOCUS_GAIN.
