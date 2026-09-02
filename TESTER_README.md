# Live Rocket Tracker — closed tester notes (stamp 42)

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

This stamp is **42** / **1.0.32**. Built from stamp 39 only. OverlayPiP stays deleted. SizedVidWindow is not in the tree. Do not invent OverlayPipActivity, FloatingVideoWindow, or SizedVidWindow.

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

## Stamp 42 PiP chrome (system only)

- **REAL Android Picture-in-Picture.** Drag the picture to the bottom: Android's dump-X appears. Dump it. That is the close. OEM PiP X if present can stay. Still draggable.
- **MIN** (stock YouTube / Android PiP size — not 280x168 postage): only system grow-arrows + X. No LRT CC, no LRT gear, no LRT mute overlay, no invented buttons. Fake `ic_pip_cc` RemoteAction is deleted.
- **MEDIUM** (grow arrows): leftover HUD on THIS screen, packer — not magic 1120x630. Tap shows YouTube's own CC / gear / mute / arrows.
- **MAX**: fullscreen MCC square path. Full YouTube icons including CC and gear.
- Mute is YouTube mute (native chrome on MEDIUM / MAX tap). Not an LRT overlay at MIN.
- Captions render on the video frame (`textTracks` / YouTube caption windows). We do not draw an LRT CC button.
- If the OEM ignores size hints, live with Android's size plus the dump-X. Do not invent a second overlay window to fake resize.

## HUD (issue 11, from 39)

- Wallpaper **grid / design art underneath** is a notch brighter so a dark space webcast reads against the wallpaper (Path of Exile / black-on-black case).
- Do not blow out plates / clock / chrome. Clock and eight plates are not restacked. No catalog rewrite.

## Owl / AUTO (from 26)

Catalog skip of Electron Owl is a bug. AUTO follows HOLD / in-flight / webcast-live. Owl stays in the CMD picker so you can reselect it and re-pin AUTO (double-tap AUTO still pins). Do not hide the current hold/in-flight bird as past.

## Also still true from 25

Eight plates: **CMD CDT TEL STS PAD VID MSK AUTO**.
Unknown vehicle = **NEW VEHICLE**. We do not invent drawings or numbers.
No AUDIOFOCUS_GAIN.
