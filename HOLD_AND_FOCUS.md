# HOLD + FOCUS (do not "simplify" this back into AUTO)

## The bug
`RocketTelemetryModule.resolveTracked` in AUTO calls `getNextAny()`, which keeps only `isUpcoming` (T+5 min). An in-flight Falcon is not upcoming. The wallpaper then locks the soonest NET on Earth (Arianespace, etc.).

AUTO = browse. HOLD = watch. Never let AUTO steal a HOLD.

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
