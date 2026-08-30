# Layout contract — Retro Command Center

Cursor cannot see pixels. This file is the eyes.

## Hard rules
1. Do not draw labels, sliders, or gauges with absolute x,y in `RetroCommandWallpaperService.kt` unless you also screenshot the emulator after the change.
2. No text may occupy the same rect as a side button, slider, or lamp. Buttons own their rect. Text is laid out in the remaining content rect.
3. Daylight: contrast at least 7:1. No 1px amber on charcoal. Min type 18sp equivalent. Nation skin = color + type, not a new coordinate soup.
4. Wallpaper is glance: countdown, status, one strip. Full data lives on FOCUS pages (Compose or a real layout, not more canvas).
5. If you cannot screenshot the emulator, you do not ship a UI change.

## Split
- `RetroCommandWallpaperService.kt` must get thinner, not thicker.
- New command pages = layout engine (Compose). Not more `drawText`.

## Fail closed
A change that covers a control with text is a bug, not a style choice. Revert it.
