# Alarm Pro bug — font setting skips main clock time
Date: 2026-09-23
Tags: font, UI, clock, settings
Status: open

## Bug
When the user changes the font in Alarm Pro settings, the font updates on all UI elements except the main time display — the clock face stays on the old font.

## Repro
1. Change font in Alarm Pro settings.
2. Labels/buttons update.
3. Main HH:MM:SS time does not — stays on old typeface.

## Likely cause
Clock TextView / RemoteViews chronometer path hard-coded to a typeface, or bypasses the shared font applicator.

## Links
- GitHub: https://github.com/skiiwa67-collab/Alarm-Pro/issues/5
- Notion Focus: https://app.notion.com/p/3e4a94933faa816fb947e824bbe360b6

## Scope
Separate from tip46 six-digit HH:MM:SS / live re-anchor. Typeface application only.
