# Live Rocket Tracker — closed tester notes (stamp 39)

Sideload this APK. This is **not** a Play production upload. Play testers on versionCode 17 stay on 17. Play 17 stays frozen.

Stamp **20** (`stamp-20-debug`) stays the KEEP for units + AUTO pin.
Stamp **21** (`stamp-21-debug`) is the GISAT look + Electron HUD locks.
Stamp **23** (`stamp-23-debug`) is SIGNED usable WITH the Pixel bar. Do not overwrite it.
Stamp **25** (`stamp-25-debug`) is SIGNED. Auto packs THIS page. Do not overwrite it.
Stamp **26** (`stamp-26-debug`) is Owl AUTO / Electron HUD. Do not overwrite it.
Stamp **36** (`stamp-36-debug`) deleted OverlayPiP. Full-screen MCC WebView + sign-in. Do not overwrite it.
Stamp **37** (`stamp-37-debug`) explicit MCC WebView PiP + HUD session reuse. Do not overwrite it.
Stamp **38** (`stamp-38-debug`) inset PIP chip + video-only 16:9 system PiP. Chris SIGNED as nice. Do not overwrite it.

This stamp is **39** / **1.0.29**. Built from stamp 38 only. OverlayPiP stays deleted. Do not invent OverlayPipActivity or FloatingVideoWindow.

## VID (locked cut)

- OverlayPipActivity stays gone. HUD VID and MCC VID do not wrap it, hop it, or PiP it.
- CommandCenterActivity is the player (`singleTask`). Manifest has `supportsPictureInPicture=true`.
- MCC VID is a full-screen WebView for Google / YouTube sign-in. Not OverlayPip. Password Manager can autofill.
- MCC VID has an explicit **PIP** chip. One tap calls `enterPictureInPictureMode` on the playing WebView.
- The PIP chip sits **below** the status bar and display cutout. Hidden while in system PiP. After center-expand back to fullscreen MCC, the PIP chip is still there to hop back to PiP.
- In system PiP the window is a **16:9 video-only** surface: MCC chrome, the PIP chip, and YouTube page chrome (title / comments / related) are hidden. Video fills the WebView.
- **Center expand** on the system PiP chrome = fullscreen MCC to browse videos. That is kept. It is not a second player.
- Back and EXIT while video is up also PiP that same WebView.
- HUD VID reuses the same Command Center session.
- No AUDIOFOCUS_GAIN. YouTube wins.

## Stamp 39 PiP chrome (system only)

- Tap the PiP chrome: **MUTE** (speaker) and **CC** (closed captions) sit next to the system gear / X via `PictureInPictureParams.setActions` `RemoteAction`. Not a new overlay window.
- CC stays enabled / tappable. Mute toggles the playing WebView `video.muted` (JS on the video element). Broadcasts are wired in `CommandCenterActivity`.
- Size toggle (system bottom-right arrows, plus a size RemoteAction): inward = compact like stamp-26 emu ~280x168; outward = current big Razr ~1120x630. Implemented with `setPictureInPictureParams` / `sourceRectHint` / 16:9 `aspectRatio`.
- **OEM note:** Motorola currently pins ~1120x630 (`Rect 52,1606-1172,2236`). If the OEM ignores the compact hint, this stamp does **not** invent OverlayPip to fake a small window. Report that — the mute/CC actions still work on the system chrome.

## HUD (issue 11)

- Wallpaper **grid / design art underneath** is a notch brighter so a dark space webcast reads against the wallpaper (Path of Exile / black-on-black case).
- Do not blow out plates / clock / chrome. Clock and eight plates are not restacked. No catalog rewrite.

## Owl / AUTO (from 26)

Catalog skip of Electron Owl is a bug. AUTO follows HOLD / in-flight / webcast-live. Owl stays in the CMD picker so you can reselect it and re-pin AUTO (double-tap AUTO still pins). Do not hide the current hold/in-flight bird as past.

## Also still true from 25

Eight plates: **CMD CDT TEL STS PAD VID MSK AUTO**.
Unknown vehicle = **NEW VEHICLE**. We do not invent drawings or numbers.
No AUDIOFOCUS_GAIN.
