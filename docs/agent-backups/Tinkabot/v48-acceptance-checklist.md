# Alarm Pro v48 acceptance checklist (tinkabot, 2026-09-26 ~5:05 PM CT)

**What v48 is:** the v47 clock code kept verbatim, plus removal of the residue. It is not a rewrite.
**Where the v47 reference lives:** `/workspace/agent-backups/tinkabot/v47-redteam/v47-reference/`
- That folder holds copies of v47 files extracted from `Helios/v47-local-review/alarm-v47-src.zip`, plus a `SHA256SUMS` file.
**Proof basis:** `/workspace/agent-backups/tinkabot/2026-09-26-v47-local-redteam.md`

Set these variables before running the checks:
```
V48=<path to v48 repo root>          # e.g. clone of the v48 branch
REF=/workspace/agent-backups/tinkabot/v47-redteam/v47-reference
PKG=app/src/main/java/com/claudewidgets/alarmweather
APK=<path to v48 apk>
```

## A. MUST BE PRESENT and byte-identical to v47 (the clock fix)
| # | Item | Check | Pass if |
|---|---|---|---|
| A1 | `ClockFormat.kt` (patterns `HH:mm:ss` / `hh:mm:ss`) | `cmp $REF/ClockFormat.kt $V48/$PKG/ClockFormat.kt` | no output |
| A2 | `ClockFormat.kt` is tracked in git (it was untracked in v47, so a clean GitHub build fails without it) | `git -C $V48 ls-files --error-unmatch $PKG/ClockFormat.kt` | exit 0 |
| A3 | `widget_layout.xml`: 12 framework `<TextClock>` views (`lineN_wall_regular/bold/black`) with the same fonts as the Chronometers; Till Chronometers untouched | `cmp $REF/widget_layout.xml $V48/app/src/main/res/layout/widget_layout.xml` and `rg -c '<TextClock' ...` = 12, `rg -c '<Chronometer' ...` = 12 | identical, 12 and 12 |
| A4 | Provider CLOCK branch: sets both slots through `setCharSequence("setFormat12Hour"/"setFormat24Hour", ClockFormat.pattern(use24h))` | `sed -n '/} else if (field == FieldType.CLOCK) {/,/RefreshScheduler.scheduleNextLocalMidnight/p' $V48/$PKG/AlarmWidgetProvider.kt \| diff $REF/clock-branch.v47.txt -` | no diff. If v48 drops the `scheduleNextLocalMidnight` call, the only allowed diff is losing that call and its `if (use24h) {` wrapper. |
| A5 | `wallIdFor` + `LineViewIds` wall ids | `sed -n '/private fun wallIdFor/,/^        }/p' $V48/$PKG/AlarmWidgetProvider.kt \| diff $REF/wallIdFor.v47.txt -` and `rg -n 'wall(Regular\|Bold\|Black)\|_wall_' $V48/$PKG/AlarmWidgetProvider.kt \| sed 's/^[0-9]*://' \| diff <(sed 's/^[0-9]*://' $REF/wall-ids.v47.txt) -` | no diff |
| A6 | 12h is the default, 24h is a toggle | `rg -n 'fun is24HourFormat\(\): Boolean = prefs.getBoolean\("use_24_hour_format", false\)' $V48/$PKG/WidgetPreferences.kt` and `rg -n 'use24hSwitch.isChecked = prefs.is24HourFormat\(\)\|putBoolean\("use_24_hour_format", use24\)' $V48/$PKG/WidgetConfigureActivity.kt` | 1 hit, then 2 hits |
| A7 | Save pushes a widget update, so the toggle takes effect | `rg -n 'AlarmWidgetProvider.updateAppWidget' $V48/$PKG/WidgetConfigureActivity.kt` | hit in completeAndClose (v47 line 536) |
| A8 | TIMER (Till) is still the stock countdown Chronometer | `rg -n 'setChronometerCountDown\(chronoId, true\)' $V48/$PKG/AlarmWidgetProvider.kt` | 1 or more hits, and the TIMER branch uses `chronoIdFor` |
| A9 | `ClockFormatTest.kt` (optional, but if present it must be identical) | `cmp $REF/ClockFormatTest.kt $V48/app/src/test/java/com/claudewidgets/alarmweather/ClockFormatTest.kt` | no output |
| A10 | Version bump | `rg -n 'versionCode = 48' $V48/app/build.gradle.kts` | 1 hit |

## B. MUST BE ABSENT (residue)
Run every command from `$V48`. **Pass = no output.**
```
# B1 debug logger + HTTP ingest (was in v47 APK: AgentDebugLog.kt, TimeChangeReceiver.kt:33-40)
rg -n 'AgentDebugLog|e25390|DEBUG_E25390|127\.0\.0\.1:7685|/ingest/|#region agent log' app/
# B2 dead ClockAnchor code + its file-writing test (ClockAnchorTest.kt:19-25 appends debug-e25390.log)
rg -ln 'ClockAnchor' app/ ; ls debug-e25390.log app/debug-e25390.log 2>/dev/null
# B3 any test that writes files
rg -n 'appendText|writeText|FileOutputStream|File\(' app/src/test
# B4 build-46 1Hz loop in WallpaperTickService (anyClockOrTimer, every-wall-second reanchor)
rg -n 'anyClockOrTimer|every wall second|every wall-clock second|reanchorChronometersFromWallClock|lastClockLocalDay' app/src/main
# B5 build-46 dead prefs in WidgetPreferences
rg -n 'clock_anchor_boundary_ms|getClockAnchorBoundaryMillis|setClockAnchorBoundaryMillis|fun hasClockLine|fun widgetId' app/src/main
# B6 junk backups tracked in git (11 files present at a977c6a)
git ls-files | rg -i '\.(bak|bak_locked|lockbak_\d+|pre_vc33_\d+|corrupt-bak|conflict_\d+|stomped_\d+|vc34clean|vc33bak|tileable34|broken_greedy|orig|rej)$|~$'
# B7 old pad/Chronometer CLOCK paths and custom view classes
rg -n 'PaddedHoursChronometer|%02d:%02d:%02d|"h:mm:ss"' app/src/main
rg -n '<[a-z]+\.[A-Za-z.]+' app/src/main/res/layout/widget_layout.xml
# B8 mixing with the other TextClock branch (origin/cursor/clock-textclock-fix-c075, 5a47eae)
rg -ln 'WidgetClockFormat' app/
# B9 signing/secrets
rg -n 'storePassword|keyPassword|keyAlias|storeFile|signingConfigs' app/build.gradle.kts build.gradle.kts gradle.properties
git ls-files | rg -i '\.(jks|keystore|p12)$|local\.properties'
# B10 Cursor/agent scaffolding
git ls-files | rg -i '_patch_.*\.py|_fix_.*\.py|agent-build-init|\.cursor/'   # agent-build-init*.gradle ARE tracked at a977c6a: flag them, decide with Chris
```
Expected WallpaperTickService state: the v45 behavior, or Ada's TIMER-only narrowing. That means `startIfNeeded`, `scheduleKeepAlive`, and `sampleIfSamplerStale` are gated on wallpaper only (plus TIMER if she keeps a 60 s drift re-anchor). No `partiallyUpdateAppWidget` call runs every second for CLOCK. `git diff a977c6a -- $PKG/WallpaperTickService.kt` should be small and reviewed line by line.

## C. MUST BE FIXED IF the day-roll alarm is kept (found in v47)
- C1: `RefreshScheduler.scheduleNextLocalMidnight` creates PendingIntent requestCode `10_000+id` with data `alarmweatherwidget://dayroll/<id>` (v47 RefreshScheduler.kt:70-91). `AlarmWidgetProvider.onDeleted` (v47 :67-77) only cancels the refresh PendingIntent, so the day-roll alarm is never cancelled.
  - After a widget is deleted, it still fires `RefreshReceiver` at midnight. `RefreshReceiver.kt:74` then calls `RefreshScheduler.scheduleNext(deletedId)`, which brings the refresh chain back to life for a widget that no longer exists (a 1/5/10-min wakeup plus a weather fetch, forever).
  - Check: `rg -n 'dayroll|10_000' $V48/$PKG/AlarmWidgetProvider.kt $V48/$PKG/RefreshScheduler.kt`. Pass if either there is no day-roll at all, or there is a `cancelDayRoll` called from `onDeleted`.
  - Also acceptable: guard `RefreshReceiver` with `appWidgetManager.getAppWidgetInfo(id) == null -> return`.
- C2 (known gap, not a blocker for "clock fix only"): 12h is now the default, and the AM/PM badge is an app-drawn bitmap (v47 AlarmWidgetProvider.kt:704). No noon alarm exists, and the day-roll only runs for 24h (v47 :667-669). At 12:00:00 the digits are right but the badge can read AM until the next refresh (up to 10 min, longer under Doze). Tell Chris.

## D. APK checks (box, no device)
```
/workspace/tmp/venv/bin/python /workspace/agent-backups/tinkabot/v47-redteam/apk_inspect.py $APK
```
Pass if all of these hold:
- versionCode is 48.
- `widget_layout tags` shows `TextClock: 12` and `Chronometer: 12`.
- `custom (dotted) view classes` is `[]`.
- `ClockFormat` class is present.
- `AgentDebugLog []`, `ClockAnchor []`, `PaddedHours []`.
- The strings `127.0.0.1:7685`, `DEBUG_E25390`, `e25390`, `/ingest/` are all False.
- `HH:mm:ss`, `hh:mm:ss`, `setFormat12Hour`, and `setFormat24Hour` are all True. The `'h:mm:ss'` line is a substring match and always True when `hh:mm:ss` is present, so ignore it. In v47 the exact-match check was False.

For a release APK, also check that `debuggable_attr` is `[]` and that the signature cert is not "Android Debug".

## E. Behaviour proofs (re-run, no device)
```
export PATH=/opt/jdk17/bin:$PATH
cp $V48/$PKG/ClockFormat.kt /tmp/ && cmp /tmp/ClockFormat.kt $REF/ClockFormat.kt   # proof applies only if identical
cd /workspace/agent-backups/tinkabot/v47-redteam/java && javac TextClockProof.java && java -cp . TextClockProof | tail -20
python3 /workspace/agent-backups/tinkabot/v47-redteam/sim-v47-textclock-vs-chronometer.py | tail -3
```
Pass if both of these hold:
- `TOTAL checks=1065916 fails=0`, with all 11 table rows PASS and all 4 app/system combos PASS.
- `TOTAL v47 TextClock FAILS: 0`.

Ada also runs `:app:testDebugUnitTest`, which must be green with ClockAnchorTest gone. Expect 9 tests from ClockFormatTest.

## F. On-device smoke (Chris / Ada, after install)
1. Remove and re-add the widget.
2. Default: shows `hh:mm:ss` with a leading zero, e.g. `09:05:09`.
3. Toggle 24h and save: shows `HH:mm:ss`.
4. Flip the system 24h setting both ways: the widget does not change.
5. Force-stop the app: the clock keeps ticking.
6. Reboot: the clock is correct before the app is opened.
7. Till still counts down.
