# HOLD + FOCUS (do not "simplify" this back into AUTO)

## The bug
`isInFlight()` used to return false on Launch Successful, and the T+ window was 30 minutes. After Electron deploy (~56 min) + webcast_live false, AUTO dropped the bird and `getNextAny()` locked the soonest future NET (GISAT). A one-mission Owl name hack is not a catalog.

AUTO = browse. HOLD = watch. AUTO double-tap = hard pin. Never let AUTO steal a HOLD or a pin.
There is no LCK plate. The eight plates stay CMD CDT TEL STS PAD VID MSK AUTO.

## Live window
- Fetch LL2 upcoming **and** previous.
- Picker: upcoming (min 14 days) + previous 48 hours + HOLD / in-flight / webcast-live / Go / T+ 6h.
- AUTO first bucket: HOLD / Go / In Flight / webcast_live / T+ 6h, closest to now. Else soonest future NET.
- Success is not a delete during the T+ watch window.

## Pin (AUTO double-tap)
- Sticky launch id. Does not expire.
- Lock light on the AUTO plate while pinned. Browse lamp stays off.
- Catalog refresh / missed `findById` keeps the pinned snapshot. Do not fall through to `getNextAny`.
- Plus/minus still walk events on the pinned launch. Launch pick stays CMD / MCC.

## HOLD
- Sticky launch id + `holdUntilMs`.
- Presets: 2h (default), 6h, 2d, until cancel.
- If the user is watching a live / in-flight vehicle, auto-HOLD for the default duration past T-0. Do not make them find a button during ascent.
- In-flight beats next NET.
- Stage split does not end HOLD. Success/failure + timer ends it.
- ISS / long missions: user picks 2d or until cancel.

## FOCUS
A live wallpaper cannot hide other Android launcher widgets. Delete that path.

FOCUS spawns extra command pages for THIS mission (you already have `launcherPageCount` up to 12):
TRAJ, PROP, ENG, then STG1 / STG2 after staging.
Leave HOLD → pages go away.

## Cursor rules
See LAYOUT_CONTRACT.md. No new absolute-coordinate text in the 4600-line wallpaper file.
