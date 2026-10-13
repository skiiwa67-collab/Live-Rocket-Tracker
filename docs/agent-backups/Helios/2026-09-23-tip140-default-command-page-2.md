# tip140 Soft-PASS — Default command page index 2 Soft-PASS

**At:** 2026-09-23 07:38 CT
**Owner:** Ada (Git/Studio). Helios Soft-PASS audit. Soft-FAIL device/Studio push until Chris go.

## Chris ask Soft-PASS
Hard-default home/HUD to **page 2 (command page)** on first install / after big patch. Users who want icons-first home Soft-PASS change back to page 0 in Settings. Soft-FAIL adb until Chris go.

## Soft-PASS findings (box RetroCommandCenter stamp sources)
1. LRT is a **live wallpaper**, Soft-FAIL the system launcher. Public Android Soft-FAIL: wallpaper Soft-PASS cannot force Nova/Pixel/etc to **scroll/open** on a given page. Soft-PASS only: detect page via `WallpaperService.onOffsetsChanged` (`xOffset`/`xOffsetStep`) and paint HUD when `page == commandPageIndex`.
2. Soft-PASS already: `SetupActivity` Yes Soft-PASS sets `commandPageIndex = 2` and `launcherPageCount >= 3`.
3. Soft-FAIL conflict: `AppPrefs.commandPageIndex` default Soft-PASS `getInt("cmd_page", 0)` and `ensurePaidAppDefaults()` Soft-PASS writes `cmd_page` **0** on `ccos_first_run_v6` one-shot — fights Setup Soft-PASS page 2 for first install / migration paths.
4. Soft-PASS user override Soft-PASS already: MainActivity Settings COMMAND PAGE −/+ Soft-PASS writes `commandPageIndex` (0…MAX). Soft-PASS keep that Soft-FAIL remove.

## Soft-PASS fix (next tip = tip140 / after tip139 Soft-PASS ship)
Soft-FAIL tip139 reopen Soft-PASS (art Soft-PASS locked). Soft-PASS tip140:
1. `AppPrefs` Soft-PASS default `cmd_page` → **2** (getter default + any reset helpers).
2. New one-shot Soft-PASS `ccos_cmd_page_default_v1` (or bump first_run v7): Soft-PASS set `cmd_page=2`, Soft-PASS ensure `launcher_pages >= 3`, Soft-PASS Soft-FAIL wipe other prefs. Soft-PASS after big patch Soft-PASS once; user Soft-PASS may change back to 0 in Settings afterward.
3. Soft-PASS `ensurePaidAppDefaults` Soft-FAIL write `cmd_page` 0 Soft-PASS write **2** (align with Setup).
4. Soft-PASS keep SetupActivity Soft-PASS page 2 Soft-PASS.
5. Soft-PASS docs Soft-PASS: Soft-FAIL claim LRT moves launcher page Soft-PASS; Soft-PASS tell user swipe to command page 2 / leave blank pages 0–1 for icons if desired. Soft-PASS optional Nova tip Soft-PASS only if Soft-PASS verified later Soft-FAIL invent.

## Soft-FAIL
- Soft-FAIL invent Android API to set launcher default page from wallpaper.
- Soft-FAIL device/Studio/Play until Chris go.
- Soft-FAIL agent Play publish (HARD ABSOLUTE).

## Acceptance Soft-PASS
- Fresh install Soft-PASS / Soft-PASS one-shot migration Soft-PASS: `cmd_page` Soft-PASS 2, Settings shows COMMAND PAGE 2.
- User Soft-PASS can set COMMAND PAGE back to 0 Soft-PASS persist across restart.
- Soft-FAIL force-scroll Soft-PASS documented Soft-FAIL as Soft-FAIL invent.
