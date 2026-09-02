# Live Rocket Tracker — closed tester notes (stamp 34)

Sideload this APK. This is **not** a Play production upload. Play testers on versionCode 17 stay on 17. Play 17 stays frozen.

This stamp is **34** / **1.0.24**. Same HUD as stamp 26. Overlay PiP enters Android system PiP from the laid-out 280×168 window rect (16:9). If enter fails, OverlayPipActivity finishes — it must not stay fullscreen over plates. Do not overwrite stamp-20 through stamp-33.

---

# Stamp 26 notes (this APK is 26 + PiP enter only)

Stamp **20** (`stamp-20-debug`) stays the KEEP for units + AUTO pin.
Stamp **21** (`stamp-21-debug`) is the GISAT look + Electron HUD locks.
Stamp **23** (`stamp-23-debug`) is SIGNED usable WITH the Pixel bar. Do not overwrite it.
Stamp **25** (`stamp-25-debug`) is SIGNED. Auto packs THIS page. Do not overwrite it.

Stamp 26 was **26** / **1.0.16**. Owl AUTO. Not a rebuild of 25. HUD/catalog/tape/packer unchanged in 34.

## Owl / AUTO

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
- VID well is LINKS. LIVE if a watch URL exists, else official vs keep-alive historic/agency. Tap switches overlay PiP. No extra MCC YouTube panes. PiP volume down without mute. Never AUDIOFOCUS_GAIN. Overlay PiP drag stays. If AUTO is on Electron, no Starlink/SpaceX buttons on that bird.
- STS fills the well under the eight buttons. Text is spread. Agency logo is not covered.
- Background grid is a little brighter so a dark overlay PiP reads.

## Also still true from 25

Eight plates: **CMD CDT TEL STS PAD VID MSK AUTO**.
Unknown vehicle = **NEW VEHICLE**. We do not invent drawings or numbers.
No AUDIOFOCUS_GAIN. Overlay PiP stays signed.
