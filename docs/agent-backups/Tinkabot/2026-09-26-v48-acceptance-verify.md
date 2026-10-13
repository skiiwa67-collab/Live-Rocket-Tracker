# Alarm Pro v48 acceptance verification (tinkabot, 2026-09-26 ~5:25 PM CT)

**Target:** `skiiwa67-collab/Alarm-Pro` main @ **870b657**. `git ls-remote` confirms main = 870b6573…; the commit is dated 17:10 CT. Its parent d93a9fe is the vc48 stamp, whose parent is a977c6a.
**How it was checked:** read-only clone at `/workspace/tmp/Alarm-Pro-v48`. No commits, pushes, GitHub/D: writes, Studio, phone, Play or agent messages.
**Checklist used:** `/workspace/agent-backups/tinkabot/v48-acceptance-checklist.md`, run in full.
**Outputs:** everything is in `/workspace/agent-backups/tinkabot/v48-verify/` (written below as `v48-verify/`).

## OVERALL: **PASS**
One item is only partly verified: a full Android compile was not possible on this box (details in the Build item). No APK is available for v48, so the APK checks could not be run.

## Per-item results
| # | Item | Result | Evidence |
|---|---|---|---|
| A1 | ClockFormat.kt matches the v47 reference | **PASS (justified line-ending diff)** | `cmp` fails at byte 39, line 1, because the v47 reference came from Helios's Windows zip with CRLF line endings, while the repo stores LF (`git ls-files --eol`: i/lf w/lf). After CRLF→LF normalisation the files are identical, sha256 prefix 1c15b3ac… on both sides. `v48-verify/byte-compare.out.txt` |
| A2 | ClockFormat.kt tracked | **PASS** | `git ls-files --error-unmatch` succeeds (`v48-verify/checklist-run.out.txt` §A2) |
| A3 | widget_layout.xml matches v47; 12 TextClock / 12 Chronometer | **PASS (CRLF only)** | Identical after normalisation (e5c27e22…). 12 `<TextClock` and 12 `<Chronometer`. No dotted custom view tags (§B7) |
| A4 | Provider CLOCK branch sets both slots | **PASS** | The extracted branch is byte-identical to `clock-branch.v47.txt`: `setCharSequence(wallId,"setFormat12Hour"/"setFormat24Hour", ClockFormat.pattern(use24h))`, found near AlarmWidgetProvider.kt:652-669 |
| A5 | wallIdFor + wall ids | **PASS** | Identical (wallIdFor at AlarmWidgetProvider.kt:809; `val wallRegular` at :834). All 12 `lineN_wall_*` ids appear once in the layout and once in the provider (`v48-verify/static-compile-proof.out.txt`) |
| A6 | 12h default + 24h toggle | **PASS** | WidgetPreferences.kt:414 `getBoolean("use_24_hour_format", false)`. WidgetConfigureActivity.kt:346 loads the switch and :768 commits it. Both files are unchanged vs a977c6a |
| A7 | Save updates the widget | **PASS** | WidgetConfigureActivity.kt:536 `AlarmWidgetProvider.updateAppWidget` (also :556, :874) |
| A8 | Till unchanged | **PASS** | The TIMER branch (AlarmWidgetProvider.kt:636 through the CLOCK `else if`) is identical to a977c6a: it uses `chronoIdFor` :638 and `setChronometerCountDown(chronoId,true)` :649. TimeChange/AlarmChange/Boot receivers, WakeRefreshHelper, WallpaperTickService, AnimationScheduler and the manifest are all unchanged vs a977c6a (`v48-verify/till-and-branch.out.txt`) |
| A9 | ClockFormatTest.kt matches v47 | **PASS (CRLF only)** | Identical after normalisation (e7e9c3db…). It is the only tracked test file |
| A10 | versionCode 48 / versionName 0.5.24 | **PASS** | app/build.gradle.kts:13-14 (plus `testImplementation junit 4.13.2` :47) |
| B1 | AgentDebugLog / e25390 / 127.0.0.1:7685 / ingest absent | **PASS** | `rg` found no matches (rc=1) in app/ |
| B2 | ClockAnchor(+Test) absent; no debug-e25390.log | **PASS** | No files, no references |
| B3 | No tests that write files | **PASS** | No `appendText`/`writeText`/`FileOutputStream`/`File(` in app/src/test |
| B4 | No 1Hz loop for CLOCK-only or idle widgets | **PASS** | No `anyClockOrTimer`/`reanchorChronometersFromWallClock`/`lastClockLocalDay`. WallpaperTickService.kt is unchanged vs a977c6a. The service starts only when a wallpaper is on (`startIfNeeded` :142-144), and so does the keep-alive (:190, :240). Inside the loop, Off / non-Pro widgets `continue` (:266-267). CLOCK/TIMER re-anchor happens only on drift ≥60 s (:339). A CLOCK-only widget with wallpaper Off never starts the service. `v48-verify/wts-and-apk.out.txt` |
| B5 | Build-46 dead prefs absent | **PASS** | No `clock_anchor_boundary_ms`/`hasClockLine`/`widgetId()`. WidgetPreferences diff vs the v47 reference is exactly those 11 removed lines (`byte-compare.out.txt`) |
| B6 | Junk backups removed | **PASS** | All 11 deleted in 870b657 (7 ParticleOverlayView.*, 3 WallpaperTickService.*, 1 WidgetPreferences.corrupt-bak). None left in git or on disk |
| B7 | No PaddedHours / `%02d:%02d:%02d` / `"h:mm:ss"` / custom view class | **PASS** | No matches |
| B8 | Nothing from `cursor/clock-textclock-fix-c075` (5a47eae) | **PASS** | `WidgetClockFormat` is absent, 5a47eae is **not** an ancestor of 870b657, and v48 leaves WakeRefreshHelper and WallpaperTickService untouched (5a47eae changed both). The v48 layout and CLOCK branch are v47-identical |
| B9 | No signing secrets / keystores | **PASS** | No `signingConfigs`/passwords, no *.jks/*.keystore/local.properties tracked |
| B10 | agent-build-init gradle files | **STATUS: still tracked (not removed)** | `agent-build-init.gradle` and `-vc33/-vc35/-vc36/-vc37.gradle` are at the repo root. Flagged earlier as needing Chris's decision, not a blocker. They sit outside `app/` and are not applied unless passed with `--init-script` |
| C1 | onDeleted cancels the day-roll alarm | **PASS** | AlarmWidgetProvider.kt:70 calls `RefreshScheduler.cancelDayRoll`. It is defined at RefreshScheduler.kt:86-89 and uses the same PendingIntent (:91-99: requestCode `10_000+id`, action ACTION_DAY_ROLL, data `dayroll/<id>`) as `scheduleNextLocalMidnight` :68-84. Belt-and-braces: RefreshReceiver.kt:49 returns early when `getAppWidgetInfo(id) == null`, inside try/finally, so `pendingResult.finish()` still runs |
| D | APK inspect | **N/A: no APK available** | *.apk is gitignored and none is tracked. No v48 APK exists on the box (only the v47 debug APK in Helios's bundle). Once Ada has an APK, run `/workspace/tmp/venv/bin/python /workspace/agent-backups/tinkabot/v47-redteam/apk_inspect.py <apk>` |
| E1 | Per-second proof against **v48's compiled ClockFormat** | **PASS, 0 fails** | The proof calls `com.claudewidgets.alarmweather.ClockFormat.INSTANCE.pattern/render`, compiled by kotlinc 2.2.20 from the tracked v48 file. Covers midnight, 1:00, 10:00, noon, 13:00, both DST changes, 3 full days + New Year, and all 4 app/system 12/24 combos: **1,065,916 checks, 0 fails**. `v48-verify/textclock-proof-v48.out.txt`, source `v48-verify/java/TextClockProofV48.java` |
| E2 | Sims / harnesses | **PASS** | The adapted TextClock sim reports `TOTAL … FAILS: 0` (DST, reboot, Ada hourly 300 s late, asleep across midnight, 72 h without updates) in `v48-verify/sim-v48-textclock-vs-chronometer.out.txt`. The original tinkabot sim and both Ada harnesses re-ran byte-identical to their stored outputs (`v48-verify/rerun-*.out.txt`) |
| E3 | v48 unit tests (ClockFormatTest) | **PASS: 9/9** | Compiled with kotlinc 2.2.20 on JDK 17 and run with JUnit 4.13.2: `OK (9 tests)`. `v48-verify/clockformat-kotlinc-junit.out.txt` |
| Build | Clean compile from tracked files | **PARTIAL: static proof + JVM compile of the clock files** | A full Gradle/AGP build was **not feasible**: `dl.google.com` (Android SDK and Google Maven for AGP 9.1.1 / compileSdk 37) fails with a TLS EOF from this box. What was done instead: (1) kotlinc compile of the tracked ClockFormat.kt + ClockFormatTest.kt, which succeeds (rc=0); (2) static proof: every top-level type referenced in main is declared in a tracked file or imported from platform/libraries, there are no package-internal imports, and no non-platform imports; the heuristic's remaining names are all platform, nested or local (ALL, AdapterView, AlarmClock, BuildConfig, Effect, Gravity, Math, String, System, SharedPreferences, and PeriodicWorkRequest, which appears only in a comment). The new symbols (`ClockFormat`, `cancelDayRoll`, `scheduleNextLocalMidnight`, `dayRollPendingIntent`, `ACTION_DAY_ROLL`, `wallIdFor`, the 12 R.id wall ids) are all defined. Removed symbols have 0 remaining references. The `applyLine` call passes `appWidgetId` to match the new parameter. `git status --ignored` shows no untracked files. `v48-verify/static-compile-proof.out.txt`. Ada's Studio build is the authoritative full compile |

## Justified differences vs the v47 reference
1. **Line endings.** The v47 reference is CRLF (Windows zip); the v48 repo is LF. Content is identical for ClockFormat.kt, ClockFormatTest.kt, widget_layout.xml and WidgetConfigureActivity.kt.
2. **AlarmWidgetProvider.kt**, which differs only by planned changes:
   - `+RefreshScheduler.cancelDayRoll` in onDeleted (the C1 fix).
   - The TIMER branch keeps the a977c6a inline Chronometer code instead of v47's `applyTimerChronometerBase` helper, so Till is literally unchanged.
   - v47's `reanchorChronometersFromWallClock` / `applyTimerChronometerBase` were removed; they existed only for the build-46 1Hz loop.
   - CLOCK branch, wall ids and wallIdFor are identical.
3. **WidgetPreferences.kt**: identical to a977c6a. It differs from the v47 reference only by the 11 removed build-46 dead-pref lines.

## Known accepted gaps (not failures)
- **Noon AM/PM badge lag:** intentionally not in v48. In 12h mode (the default) the app-drawn AM/PM badge (drawn in the CLOCK badge code) is refreshed only by the 1/5/10-minute refresh chain, and the midnight day-roll is scheduled only for 24h (`if (use24h)` in the CLOCK branch). The TextClock digits themselves are correct at 12:00:00.
- **Carried over from the v47 proof caveats:**
  - On Android 8-11, TextClock ticks on uptime-second boundaries, so the seconds digit can change up to <1 s late; the format is never wrong.
  - Non-Latin-digit locales (ar/fa/bn) render native digits.
  - The Android formatting path is emulated by hand port on the JVM, not run on a device.
  - Release builds keep info logs (no `Log` stripping rule), e.g. location lat/lon at LocationHelper.kt:45. This predates v48.
- **agent-build-init*.gradle** (5 files) are still tracked. That's Chris's call.

## Output files (`/workspace/agent-backups/tinkabot/v48-verify/`)
- **Checklist and code checks:**
  - `checklist-run.out.txt` (full A/B/C run)
  - `byte-compare.out.txt` (v47-reference vs v48, CRLF-normalised)
  - `v48-vs-a977c6a-code.diff`
  - `till-and-branch.out.txt`
  - `wts-and-apk.out.txt`
  - `static-compile-proof.out.txt`
- **Tests and proofs:**
  - `clockformat-kotlinc-junit.out.txt`
  - `java/TextClockProofV48.java` → `textclock-proof-v48.out.txt`
  - `sim-v47-textclock-vs-chronometer.py` (copy) → `sim-v48-textclock-vs-chronometer.out.txt`
  - `rerun-2026-09-26-sim-clock-dst-reboot.out.txt`, `rerun-ada-hourly.out.txt`, `rerun-ada-wake.out.txt`
