# Live Rocket Tracker — closed tester notes (stamp 43)

Sideload this APK. This is **not** a Play production upload. Play testers on versionCode 17 stay on 17. Play 17 stays frozen.

Stamp **20** (`stamp-20-debug`) stays the KEEP for units + AUTO pin.
Stamp **21** (`stamp-21-debug`) is the GISAT look + Electron HUD locks.
Stamp **23** (`stamp-23-debug`) is SIGNED usable WITH the Pixel bar. Do not overwrite it.
Stamp **25** (`stamp-25-debug`) is SIGNED. Auto packs THIS page. Do not overwrite it.
Stamp **26** (`stamp-26-debug`) is Owl AUTO / Electron HUD. Do not overwrite it.
Stamp **36** (`stamp-36-debug`) deleted OverlayPiP. Full-screen MCC WebView + sign-in. Do not overwrite it.
Stamp **37** (`stamp-37-debug`) explicit MCC WebView PiP + HUD session reuse. Do not overwrite it.
Stamp **38** (`stamp-38-debug`) inset PIP chip + video-only 16:9 system PiP. Chris SIGNED as nice. Do not overwrite it.
Stamp **39** (`stamp-39-debug`) mute + brighter HUD grid + MCC square expand. Do not overwrite it.
Leave **40** and **41** tags if they exist. This stamp is **not** those overlay cuts.

This stamp is **43** / **1.0.33**. Built from **stamp-42-debug** only. OverlayPiP stays deleted. SizedVidWindow is not in the tree. Do not invent OverlayPipActivity, FloatingVideoWindow, or SizedVidWindow. Do not clobber stamp-20 through stamp-42.

Stamp **43** three sizes, YouTube tools only: **MIN** = stock YouTube/Android PiP (`sourceRectHint` that size, not the full WebView). Tap shows YouTube's own X + grow only. **MEDIUM** = the 16:30 clean globe window (~85% wide, 16:9, lower third, dump-X room below) — untapped is that clean shot; tap shows YouTube mute left, CC, shrink/grow, full square, X top-right, then chrome goes away again. **MAX** = normal full YouTube / MCC; PIP chip stays so you can hop back. Dump-X KEEP. Overlay-permission gear DELETED. No RemoteActions, no fake `ic_pip_cc`, no invented circles. `PIP_VIDEO_FILL` 100vh no longer covers YouTube chrome on medium/max. The 16:24 gear shot was FAIL.

## VID (locked cut)

- OverlayPipActivity stays gone. HUD VID and MCC VID do not wrap it, hop it, or PiP it.
- CommandCenterActivity is the player (`singleTask`). Manifest has `supportsPictureInPicture=true`.
- MCC VID is a full-screen WebView for Google / YouTube sign-in. Not OverlayPip. Password Manager can autofill.
- MCC VID has an explicit **PIP** chip. One tap calls `enterPictureInPictureMode` on the playing WebView. Home / `onUserLeaveHint` / `setAutoEnterEnabled` hop to **system** PiP.
- The PIP chip sits **below** the status bar and display cutout. Hidden while in system PiP. After center-expand back to fullscreen MCC, the PIP chip is still there to hop back to PiP.
- **Center expand** on the system PiP chrome = fullscreen MCC to browse videos. That is kept. It is not a second player.
- Back and EXIT while video is up also PiP that same WebView.
- HUD VID reuses the same Command Center session.
- No AUDIOFOCUS_GAIN. YouTube wins.
- SignIN YT − + X bar stays dead.

## Stamp 43 PiP chrome (YouTube tools, three sizes)

- **REAL Android Picture-in-Picture.** Drag the picture to the bottom: Android's dump-X appears. Dump it. That is the close. Still draggable.
- **MIN** = stock YouTube / Android PiP size (not 42's full-WebView hop). Tap: YouTube X + grow only. No LRT CC, no overlay gear, no mute overlay, no invented buttons.
- **MEDIUM** = Chris 16:30 clean globe window. Untapped = clean video. Tap: YouTube mute, CC, shrink/grow, full square, X. Then chrome hides again.
- **MAX** = fullscreen MCC. Normal YouTube. PIP chip KEEP.
- Overlay-permission gear is gone. Fake `ic_pip_cc` RemoteAction stays deleted. No 100vh video fill covering YouTube chrome on medium/max.

## HUD (issue 11, from 39)

- Wallpaper **grid / design art underneath** is a notch brighter so a dark space webcast reads against the wallpaper (Path of Exile / black-on-black case).
- Do not blow out plates / clock / chrome. Clock and eight plates are not restacked. No catalog rewrite.

## Owl / AUTO (from 26)

Catalog skip of Electron Owl is a bug. AUTO follows HOLD / in-flight / webcast-live. Owl stays in the CMD picker so you can reselect it and re-pin AUTO (double-tap AUTO still pins). Do not hide the current hold/in-flight bird as past.

## Also still true from 25

Eight plates: **CMD CDT TEL STS PAD VID MSK AUTO**.
Unknown vehicle = **NEW VEHICLE**. We do not invent drawings or numbers.
No AUDIOFOCUS_GAIN.
