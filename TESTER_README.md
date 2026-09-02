# Live Rocket Tracker — closed tester notes (stamp 37)

Sideload this APK. This is **not** a Play production upload. Play testers on versionCode 17 stay on 17. Play 17 stays frozen.

Stamp **20** (`stamp-20-debug`) stays the KEEP for units + AUTO pin.
Stamp **21** (`stamp-21-debug`) is the GISAT look + Electron HUD locks.
Stamp **23** (`stamp-23-debug`) is SIGNED usable WITH the Pixel bar. Do not overwrite it.
Stamp **25** (`stamp-25-debug`) is SIGNED. Auto packs THIS page. Do not overwrite it.
Stamp **26** (`stamp-26-debug`) is Owl AUTO / Electron HUD. Do not overwrite it.
Stamp **36** (`stamp-36-debug`) deleted OverlayPiP. Full-screen MCC WebView + sign-in. Do not overwrite it.

This stamp is **37** / **1.0.27**. Built from stamp 36. OverlayPiP stays deleted.

## VID (locked cut)

- OverlayPipActivity stays gone. HUD VID and MCC VID do not wrap it, hop it, or PiP it.
- CommandCenterActivity is the player (`singleTask`). Manifest has `supportsPictureInPicture=true`.
- MCC VID is a full-screen WebView for Google / YouTube sign-in. Not 280x168. Not FloatingVideoWindow overlay chrome. Password Manager can autofill. Sign-in YT overlay bar stays gone.
- MCC VID has an explicit **PIP** chip. One tap calls `enterPictureInPictureMode` on the playing WebView. Do not require Razr recents / taskbar swipe.
- Back and EXIT while video is up also PiP that same WebView. They do not `finish()` or `closeVid()` the player.
- HUD VID reuses the same Command Center session. If a video is already playing, do not load a different historic URL. Same YouTube, same `CookieManager` cookies. Existing player is brought forward and PiP'd if needed.
- PiP is the YouTube frame (16:9 of the WebView). MCC chrome and the PIP chip are hidden in PiP. Source rect is the WebView. If `enterPictureInPictureMode` fails, stay fullscreen MCC with video still playing.
- No AUDIOFOCUS_GAIN. YouTube wins.

## Owl / AUTO (from 26)

Catalog skip of Electron Owl is a bug. AUTO follows HOLD / in-flight / webcast-live. Owl stays in the CMD picker so you can reselect it and re-pin AUTO (double-tap AUTO still pins). Do not hide the current hold/in-flight bird as past.

Published Owl only (no invented NET):
- Vehicle Electron. 9 Rutherford sea-level + 1 Rutherford vacuum. LOX/RP-1 electric-pump-fed. Smithsonian vac ~5800 lbf, Isp 343 s.
- Mission Owl Around The World, 1x StriX (Synspective). 575 km LEO, 38 deg. Pad Launch Complex 1 Māhia, this flight LC-1B.
- Coords 39.26085 S, 177.86586 E. Map label: Rocket Lab LC-1 / Mahia. Tiny lat/lon like GISAT.
- Draw Electron: 9 octaweb bells + 1 vac STG2. Not GSLV. Not Super Heavy.

GISAT Hindi look remains when that ISRO launch is pinned/selected. Do not force GISAT over Owl AUTO.

## HUD

- Clock at the top and T-/T+ under it are SIGNED. Do not treat a change there as a pass.
- Event tape packs above the Razr dock (WindowInsets / packer). Eight plates are not moved.
- ALT/SPD family is a bit smaller. Digital gauges stay.
- VID well is LINKS. LIVE if a watch URL exists, else official vs keep-alive historic/agency. Tap opens Command Center VID. If AUTO is on Electron, no Starlink/SpaceX buttons on that bird.
- STS fills the well under the eight buttons. Text is spread. Agency logo is not covered.

## Also still true from 25

Eight plates: **CMD CDT TEL STS PAD VID MSK AUTO**.
Unknown vehicle = **NEW VEHICLE**. We do not invent drawings or numbers.
No AUDIOFOCUS_GAIN.
