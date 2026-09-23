# tip140 — default command page 2 (1.0.130)

**Date:** 2026-09-23 CT
**Stamp:** 140 / 1.0.130 on `stamp-85`
**Play / device / Studio push:** Soft-FAIL until Chris go. Soft-FAIL agent Play publish forever.

## Contents
- `AppPrefs.commandPageIndex` getter default Soft-PASS `cmd_page` **2**.
- `ensurePaidAppDefaults()` Soft-PASS writes `cmd_page` **2** (was 0).
- One-shot `ccos_cmd_page_default_v1`: Soft-PASS set `cmd_page=2` and Soft-PASS `launcher_pages >= 3` after first install / big patch. Soft-FAIL wipe other prefs.
- SetupActivity Yes Soft-PASS still sets command page 2.
- Settings COMMAND PAGE −/+ Soft-PASS kept — user Soft-PASS can set 0 and it persists after the one-shot.

## Soft-FAIL invent
LRT is a live wallpaper. Public Android Soft-FAIL: wallpaper Soft-PASS cannot force Nova/Pixel/etc to scroll/open on a given page. Soft-PASS swipe to command page 2. Leave pages 0–1 blank for icons if desired.

## Soft-FAIL
- Soft-FAIL tip139 reopen
- Soft-FAIL adb / Studio install / Play Publish/Rollout until Chris go
- Soft-FAIL agent Play publish (HARD ABSOLUTE)
