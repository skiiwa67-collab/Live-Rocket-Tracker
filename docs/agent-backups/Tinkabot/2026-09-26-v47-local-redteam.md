# v47 local build: clock proofs, extra findings, v48 acceptance (tinkabot, 2026-09-26 ~5:05 PM CT)

**Scope:** after Chris dropped v47 and Ada took ownership of the code review, PR #6, and the build/unit tests, this report covers three things:
- (a) proof results for the v47 clock logic, which v48 keeps verbatim
- (b) problems beyond Ada's list (`/workspace/agent-backups/Ada/2026-09-26-v47-local-review.md`)
- (c) the v48 acceptance checklist

**Inputs:**
- `/workspace/agent-backups/Helios/v47-local-review/`: the patch, the full diff, `alarm-v47-src.zip` (extracted read-only to `/workspace/tmp/v47src`, directory permissions fixed locally), and `app-debug.apk` (sha256 bbd2620c…2d58)
- Repo `skiiwa67-collab/Alarm-Pro` @ a977c6a, read-only clone at `/workspace/tmp/Alarm-Pro-ro`

**What I did not do:** no commit, push, Studio, device, Play, or agent messages. I did not re-run the 13 unit tests (Ada owns them; there is no Android SDK or Gradle Android plugin on this box). I did not write a patch of my own: task 3 was dropped.

## Task 1: tool search (short)
- **Cursor plugin marketplace:** there is no GetDynamicTools/CallDynamicTool in my toolset, and `GetMcpTools` pattern `plugin|dynamic|marketplace` returned 0 matches. **Not searched. Unavailable.**
- **Skill catalog** `/home/box/agent-data/workflows`: 20 skills. Nothing is clock- or widget-clock-related; the only hit on "clock|widget" is `lrt-emulator-extra-hud-page`, which is unrelated.
- **pstack / GitHub libraries:** not searched after the redirect. RemoteViews can only inflate framework `@RemoteView` classes, so a third-party view library cannot run in a home-screen widget anyway. That leaves the answer as the platform `android.widget.TextClock`:
  - The AOSP source I fetched is annotated `@RemoteView` (TextClock.java.aosp-mirror-main:98), and `setFormat12Hour`/`setFormat24Hour` are `@RemotableViewMethod` (:358-359, :417-418).
  - Docs: https://developer.android.com/reference/android/widget/TextClock and https://developer.android.com/develop/ui/views/appwidgets/overview (supported widget views).
  - Source copies: `/workspace/agent-backups/tinkabot/v47-redteam/TextClock.java.*`

## (a) Proof results: v47 ClockFormat logic as reference

**Method:** `v47-redteam/java/TextClockProof.java` on Temurin JDK 17.0.20.1 with tzdata 2026b. Every second, three independent renderers must agree:
1. A verbatim port of v47 `ClockFormat.kt`: `SimpleDateFormat(pattern(use24h), Locale.US)`.
2. A port of AOSP `android.text.format.DateFormat.format` for the H/h/m/s cases (DateFormat.java.android-13.0.0_r1:530-560, zeroPad :701-703). It is fed the pattern picked by a port of `TextClock.chooseFormat()` (TextClock.java main :551-560), with both slots set to `pattern(use24h)` exactly as v47 AlarmWidgetProvider.kt:661-663 does.
3. `java.time` expected fields.

Each check also asserts length 8, the `\d\d:\d\d:\d\d` shape, and the hour range (0-23 for 24h, 1-12 for 12h). All of it runs for app 12h/24h × system 12h/24h.

**Output:** `/workspace/agent-backups/tinkabot/v47-redteam/textclock-proof.out.txt`

| Window (America/Chicago) | Samples | Result |
|---|---|---|
| 23:59:55→00:00:05 midnight | 11 s ×2 app ×2 system | PASS |
| 00:59:55→01:00:05 | same | PASS |
| 09:59:55→10:00:05 | same | PASS |
| 11:59:55→12:00:05 noon (12h shows 12:00:00) | same | PASS |
| 12:59:55→13:00:05 (12h 12→01, 24h 13) | same | PASS |
| DST spring-forward 2026-03-08 01:59:59 CST→03:00:00 CDT | same | PASS |
| DST fall-back 2026-11-01 01:59:59 CDT→01:00:00 CST | same | PASS |
| Full day 2026-03-08 (23 h), every second | 82,800 s | PASS |
| Full day 2026-06-16, every second | 86,400 s | PASS |
| Full day 2026-11-01 (25 h), every second | 90,000 s | PASS |
| 2026-12-31 23:00→2027-01-01 01:00, every second | 7,201 s | PASS |
| **4 combos** (app 12h default / 24h × system 12h / 24h) at 00:00:00, 00:05:09, 09:05:09, 12:00:00, 13:05:09, 23:59:59 | 4 rows | PASS: app 12h → `12:00:00 12:05:09 09:05:09 12:00:00 01:05:09 11:59:59` under either system setting; app 24h → `00:00:00 00:05:09 09:05:09 12:00:00 13:05:09 23:59:59` under either |
| **Total** | 1,065,916 checks | **0 fails** |

**Chris's 12h-default + 24h-toggle requirement: MET in v47 code.**
- Default: `is24HourFormat()` defaults to false (WidgetPreferences.kt:414).
- Toggle: switch at WidgetConfigureActivity.kt:346, committed at :768, widget updated on save at :536.
- Both slots are set, so the system 24h setting cannot flip it (AlarmWidgetProvider.kt:661-663).
- Counterfactual: the old 9fa4b0f pattern `h:mm:ss` renders `9:05:09` (7 characters, dropped zero).

**Anchor-dependent scenarios** (the tinkabot sim and both Ada harnesses, re-run and adapted):
- The originals re-ran with byte-identical output: `v47-redteam/rerun-*.out.txt` diffed against the stored outputs with no differences.
- Adapted run: `v47-redteam/sim-v47-textclock-vs-chronometer.py` → `…out.txt`. TextClock text is a pure function of `now` (TextClock.java onTimeChanged :659-662); the last RemoteViews refresh is not an input.

| Scenario | Old Chronometer | v47 TextClock |
|---|---|---|
| A: DST fresh anchor and no-re-anchor (both modes) | 10 WRONG (02:30, 010:00, …) | 0 fail |
| B: reboot, cached RemoteViews re-inflated 40 s after boot | −96:59:20 | 23:00:40 / 11:00:40 OK |
| C: Ada hourly cases, re-anchor 300 s late | 20 WRONG (24:00, 00:1:00, 010:00, 13:00), same as Ada's recorded 20 | 0 fail |
| D: asleep across midnight, first frame 00:07 | 24:07:00 | 00:07:00 / 12:07:00 OK |
| E: no app updates for 72 h across the fall-back, sampled every 7 s | n/a | 74,058 samples, 0 fail |

**Total v47 TextClock fails: 0.**

**Caveats on the proof (honest limits):**
1. **Emulated, not run on Android.** The AOSP DateFormat and chooseFormat logic are hand ports running on the JVM; no Android runtime executed them.
2. **The v47 unit tests check `render()`, which production never calls.** Production calls only `ClockFormat.pattern()` (AlarmWidgetProvider.kt:661). `ClockFormatTest` covers `ClockFormat.render()`, which goes through SimpleDateFormat. My AOSP port is what closes that gap.
3. **Sub-second lag on Android 8-11.** On API 26-30 TextClock ticks on `uptimeMillis` second boundaries (android-8.0.0_r1:178-181, android-11.0.0_r1:193-198), so the seconds digit can change up to <1 s late: a fixed per-boot phase. The format is never wrong. API 31+ ticks on wall-clock seconds (android-12.0.0_r1:197-211).
4. **Non-Latin digits in some locales.** AOSP zeroPad uses `Locale.getDefault()`, so ar-EG, fa-IR and bn-BD render native digits (e.g. `٠٩:٠٥:٠٩`). Still 8 characters and correct, but not ASCII. en-US and hi-IN are ASCII.
5. **Host behavior.** A launcher that doesn't honor TextClock, or an OEM quirk, can't be tested off-device. TextClock stops ticking while not visible (onVisibilityAggregated :598-607), which is good for battery.

**APK static check:** `v47-redteam/apk_inspect.py` → `apk_inspect.out.txt` (androguard 4.1.4). It found:
- `widget_layout.xml` has 12 `TextClock` and 12 `Chronometer`, with no custom (dotted) view classes and no PaddedHours class.
- Dex contains `HH:mm:ss`, `hh:mm:ss`, `setFormat12Hour`, `setFormat24Hour`. The exact string `h:mm:ss` is absent.
- versionCode 47, `debuggable="true"`, signed with the Android Debug cert (expected for a debug build).
- **Also in the dex:** `AgentDebugLog` plus the `127.0.0.1:7685` and `/ingest/` strings, and `ClockAnchor` classes.

## (b) Problems beyond Ada's list
1. **NEW: the day-roll alarm is never cancelled, which brings refresh chains back to life for deleted widgets.**
   - The build-46 `RefreshScheduler.scheduleNextLocalMidnight` creates PendingIntent requestCode `10_000+id` with data `dayroll/<id>` (RefreshScheduler.kt:70-91).
   - `AlarmWidgetProvider.onDeleted` (:67-77) cancels only the refresh PendingIntent.
   - After a 24h-mode widget is deleted, the alarm still fires at midnight. `RefreshReceiver` (:36-81) runs, and `RefreshReceiver.kt:74` calls `scheduleNext(deletedId)`, so a 1/5/10-min wakeup plus weather-fetch chain restarts for a widget that no longer exists.
   - Ada only rated the `ACTION_DAY_ROLL` filter correct; she did not flag this.
2. **A partial commit breaks a clean GitHub build.**
   - `ClockFormat.kt`, `ClockFormatTest.kt`, `ClockAnchor.kt`, `ClockAnchorTest.kt` and `AgentDebugLog.kt` are not in the repo at a977c6a (checked with `git ls-files`).
   - Local `TimeChangeReceiver.kt:33-40` calls `AgentDebugLog`, and `AlarmWidgetProvider.kt:661` calls `ClockFormat`. Committing those two without the helper files fails compilation.
3. **The debug logger is called from production.** `TimeChangeReceiver.onReceive` fires on TIME_SET, TIMEZONE_CHANGED and DATE_CHANGED (receiver is exported, manifest :78-86).
   - Every call logs at level `i` and spawns a thread that POSTs to `http://127.0.0.1:7685/ingest/30904556-…` (AgentDebugLog.kt:29-45).
   - With targetSdk 36 and no networkSecurityConfig, cleartext traffic is most likely blocked, so it fails silently, but the call and its thread still happen.
   - Ada has this. What I add is that it's the only production call site.
4. **The release build doesn't strip logs.** `proguard-rules.pro` has no `-assumenosideeffects` rule for `android.util.Log`, so `Log.i` survives into release. That includes location lat/lon at LocationHelper.kt:45 (pre-existing at a977c6a) and spoken weather text in WeatherTts.kt:36/50/213.
   - This is a privacy nit, not a v48 blocker.
5. **Signing and release config:** OK. The release build has `isMinifyEnabled`/`isShrinkResources` true. There is no `signingConfigs` block, no keystore or passwords in the gradle files, and `.gitignore` covers `*.keystore` and `local.properties`. The v47 APK is a debug build (debuggable, debug cert) and is not a release candidate.
6. **Tracked agent scaffolding in the repo** (not v47-specific): `agent-build-init*.gradle` (5 files) plus the 11 junk backups (`git ls-files` at a977c6a). Ada lists the backups; the gradle init scripts are extra. Chris should decide on them.
7. **There's a second TextClock branch on the remote:** `origin/cursor/clock-textclock-fix-c075` @ 5a47eae (21:22 UTC = 4:22 PM CT), which uses `WidgetClockFormat.kt`. v48 must not mix it with `ClockFormat.kt`.
8. **The handoff overstates things.** It says "a CLOCK-only widget now needs no service", which is false (`anyClockOrTimer`, WallpaperTickService.kt:357-368; Ada has this). It also says TextClock "re-reads the zone on a DST shift": in fact DST is handled by the Calendar/TimeZone rules, and the zone is only re-read on TIMEZONE_CHANGED (TextClock.java :182-184). The behavior is still correct either way.
9. **The 12h AM/PM badge lags at noon.** Ada listed this as a known gap. It now matters more because 12h is the default: AlarmWidgetProvider.kt:704 draws the badge, and the day-roll alarm is 24h-only (:667-669).

**Clean areas:**
- The wallpaper/Pro gating refactor is equivalent.
- WidgetPreferences only adds dead methods; no keys were renamed.
- No `setAlarmClock` anywhere, so `getNextAlarmClock` is unaffected.
- The new alarms use `setExactAndAllowWhileIdle`/`setAndAllowWhileIdle` behind `canScheduleExactAlarms`.
- The manifest is unchanged from stamp45.

## (c) v48 acceptance checklist
Full checklist with exact commands: `/workspace/agent-backups/tinkabot/v48-acceptance-checklist.md`. In short:
- **A. Must match v47 byte for byte** (reference copies and SHA256SUMS in `v47-redteam/v47-reference/`):
  - `ClockFormat.kt` (and it must be git-tracked)
  - `widget_layout.xml` (12 TextClock / 12 Chronometer)
  - the provider CLOCK branch and wall-id hunks
  - the 12h-default pref and the use24hSwitch/save path
  - TIMER countdown unchanged; versionCode 48
- **B. Must be absent:** AgentDebugLog/e25390/ingest, ClockAnchor(+Test), tests that write files, the anyClockOrTimer/1Hz reanchor loop, the dead clock-anchor prefs, junk backups, PaddedHours/`%02d` padding/`h:mm:ss`, WidgetClockFormat mixing, signing secrets, agent scaffolding.
- **C. Required fix:** if the day-roll alarm stays, cancel it in onDeleted.
- **D/E.** APK script plus a re-run of both proofs, which must report 0 fails.
- **F.** On-device smoke steps.

## Files
- **This report:** `/workspace/agent-backups/tinkabot/2026-09-26-v47-local-redteam.md`
- **Checklist:** `/workspace/agent-backups/tinkabot/v48-acceptance-checklist.md`
- **Proofs:**
  - `v47-redteam/java/TextClockProof.java` → `v47-redteam/textclock-proof.out.txt`
  - `v47-redteam/sim-v47-textclock-vs-chronometer.py` → `.out.txt`
  - `v47-redteam/rerun-tinkabot-sim-clock-dst-reboot.out.txt`, `rerun-ada-hourly.out.txt`, `rerun-ada-wake.out.txt`
- **APK:** `v47-redteam/apk_inspect.py` (takes an APK path argument) → `apk_inspect.out.txt`, plus `widget_layout.decoded.xml`
- **AOSP sources:** `v47-redteam/TextClock.java.{aosp-mirror-main,android-8.0.0_r1,android-10.0.0_r1,android-11.0.0_r1,android-12.0.0_r1,android-13.0.0_r1}` and `DateFormat.java.android-13.0.0_r1`
- **v47 reference:** `v47-redteam/v47-reference/` (+ SHA256SUMS)

(`v47-redteam/` = `/workspace/agent-backups/tinkabot/v47-redteam/`)

## Verdict per area
| Area | Verdict |
|---|---|
| v47 clock logic (format, rollovers, DST, reboot/Doze independence, 4 combos) | **PASS** (emulated; device smoke still needed) |
| 12h default + 24h toggle overrides system | **PASS / requirement met** |
| APK layout (framework TextClock, no custom view) | **PASS** |
| v47 build as a whole | **FAIL**: Ada's blockers plus (b)1 day-roll orphan and (b)2 untracked files |
| Signing/release config | **PASS** (no secrets). Log stripping is a nit. |
