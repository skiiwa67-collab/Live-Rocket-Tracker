# Live Rocket Tracker — closed tester notes (stamp 27)

Sideload this APK. This is **not** a Play production upload. Play testers on versionCode 17 stay on 17. Play 17 stays frozen.

Stamp **20** (`stamp-20-debug`) stays the KEEP for units + AUTO pin.
Stamp **21** (`stamp-21-debug`) is the GISAT look + Electron HUD locks.
Stamp **23** (`stamp-23-debug`) is SIGNED usable WITH the Pixel bar. Do not overwrite it.
Stamp **25** (`stamp-25-debug`) is SIGNED. Auto packs THIS page. Do not overwrite it.
Stamp **26** (`stamp-26-debug`) is SIGNED. Owl AUTO one-mission hack. Do not overwrite it.

This stamp is **27** / **1.0.17**. Live LL2 window. Not a rebuild of 26.

## AUTO / catalog (the customer-losing bug)

Catalog is a live window over Launch Library 2, not a delete list.

- Always fetch LL2 **upcoming and previous**.
- CMD picker: all upcoming (minimum next 14 days) + recent previous (last 48 hours), including just-flew / HOLD / in-flight / webcast-live / Go. Never hide a bird AUTO just left.
- AUTO pick: HOLD or Go or In Flight or webcast_live or T+ inside a 6-hour watch window, **closest to now**. Only if none of those exist, soonest future NET.
- T+ watch window is **6 hours** after NET (or still webcast_live). 30 minutes was the skip-to-GISAT bug. Success does not eject the bird during that window.
- AUTO never keys off the word Owl. Pad facts for Electron / Mahia may still enrich missing coords.

GISAT Hindi look remains when that ISRO launch is pinned/selected. Do not force GISAT over a just-flew bird still inside the watch window.

## HUD

- Clock at the top and T-/T+ under it are SIGNED. Do not treat a change there as a pass.
- Event tape packs above the Razr dock (WindowInsets / packer). Eight plates are not moved.
- No AUDIOFOCUS_GAIN. Overlay PiP stays signed.

## Also still true from 25 / 26

Eight plates: **CMD CDT TEL STS PAD VID MSK AUTO**.
VID well is LINKS. STS fills the well under the eight buttons.
