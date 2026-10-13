# tinkabot cross-review: Alarm Pro v47 / surgical-merge red-team, Keep scope, voice bar
2026-09-26, about 16:05–16:20 CT. Read-only. Nothing applied, pushed, or published. No Studio, no phone, no Play.
Repo: /workspace/alarm-push/Alarm-Pro. `git fetch` left main == origin/main == a977c6a. `git status` is clean.
Mirror: /workspace/agent-backups/tinkabot/2026-09-26-alarm-surgical-merge-redteam.md (same content).
Sim: /workspace/agent-backups/tinkabot/2026-09-26-sim-clock-dst-reboot.py (+ .out.txt).

## TASK 1: Alarm Pro

### 1a. v47 claims: VERIFIED
- a977c6a (Helios, 15:59:53 CT) touches only app/build.gradle.kts. It changes versionCode 46→47 and versionName 0.5.22→0.5.23.
- d5ef02e (Helios, 15:59:16 CT) is the inverse of 8bd76cb in the same 4 files: AlarmWidgetProvider, RefreshReceiver, RefreshScheduler, and WallpaperTickService (+118/−237 vs +237/−118). `git diff dd05a67 d5ef02e` shows only 2 added docs files, so the app code is byte-identical to dd05a67.
- At a977c6a, CLOCK is `setChronometer(chronoId, baseElapsed, null, true)`: started=true with a null format. See AlarmWidgetProvider.kt:699-700. The base is elapsedRealtime minus the real ms since the midnight/noon boundary (:681-691), plus Rule2 −12h in the 12 o'clock hour (:695-697). This is the same Chronometer path as Till (:638-645).
- v47 is parked, per the parent.

### 1b. The "#4 24h wrap fix": where it lives
- GitHub issue #4 is titled "DONE tip46 pushed main @ 8bd76cb". Its comments carry Chris's scope lock (17:21Z = 12:21 CT, 09-23): six digits, live system time, and "modulo-24 wrap on CLOCK only". Issue #3 is the 12h hour-drop bug.
- **d01c19c** (vc38, 09-08 06:45 CT) is the original 24h day-roll fix. It added:
  - `RefreshScheduler.scheduleNextLocalMidnight`: an RTC_WAKEUP alarm at 00:00:01, request code 10_000+id, ACTION_DAY_ROLL.
  - The ACTION_DAY_ROLL filter in RefreshReceiver.
  - A call from the CLOCK branch when use24h.
  - A `lastClockLocalDay` day-change full re-render in WallpaperTickService.
  - Its boundaryHour rewrite was logically identical to the old one.
- **9127aff** (vc40, 09-08 07:17 CT) removed all of d01c19c ("drop TextClock/pad/day-roll experiments").
- **8bd76cb** (tip46) re-added d01c19c's scheduler, receiver, and day-roll code verbatim. It also added a "Rule1" `HOUR_OF_DAY % 24` (8bd76cb AlarmWidgetProvider.kt:867), which is a no-op because HOUR_OF_DAY is already 0–23.
- **d5ef02e** removed them again. At a977c6a, `git grep scheduleNextLocalMidnight|lastClockLocalDay|ACTION_DAY_ROLL|DAY_MS` returns nothing. **Nothing of d01c19c survives at a977c6a.** Ada's attribution to d01c19c is correct.
- At a977c6a, the only wrap mechanism is incidental re-anchoring: the 1/5/10-minute refresh chain (RefreshReceiver.kt:66,71) and the WallpaperTickService drift re-anchor at ≥60s, which runs only while a wallpaper is on (WallpaperTickService.kt:334-344, :144).

### 1c. NEW: a DST bug at a977c6a (and in dd05a67 / Ada's patch)
- `millisSinceBoundary = now.timeInMillis - boundary.timeInMillis` (:689) counts real milliseconds. On DST days it is off by 1h between the transition and the next boundary.
- Sim output:
  - Spring-forward, 24h at 03:30 CDT shows `02:30:00`.
  - Fall-back at 01:30 CST shows `02:30:00`.
  - Fall-back at 09:00 CST shows `10:00:00` (with a prefix, `010:00:00`).
- Computing the base from Calendar fields (H*3600+M*60+S) fixes this at every re-anchor: 0 wrong in the sim.

### 1d. Red-team of Helios's hourly re-anchor + per-hour prefix ("00:%s" for h=0, "0%s" for 1–9, "%s" for ≥10)
Model check: Ada's harnesses use `fmt_elapsed` = DateUtils `H:MM:SS` or `MM:SS` and `text = format % elapsed`. That is faithful to Chronometer for non-negative elapsed with Latin digits. It does not model negative elapsed, locale digits, or DST.

I re-ran both harnesses. The output is identical to her .out.txt:
- v1: 20 WRONG with a 5-minute-late re-anchor; on-time is all OK.
- v2: open-time re-anchor is always OK. A stale frame after sleeping across midnight shows `24:07:00` in 24h, while 12h shows `12:07:00`, correct by coincidence. Opening at 23:59:58 or 09:59:59 with the alarm not yet fired shows `24:00:01` or `010:00:02`.

My own worked examples also hold:
- 9:59:59 → `010:00:00`
- 0:59:59 → `00:1:00:00`
- 23:59:59 → `24:00:00`
- 12h 12:59:59 → `13:00:00`
- 12h 11:59→12:00 and 23:59→00:00 are correct by coincidence.
- 12h never shows 0 as long as Rule2 −12h is kept.

Failure table, stock Chronometer + per-hour prefix:

| Case | Result | Evidence |
|---|---|---|
| (a) Re-anchor late at 0→1, 9→10, 23→0 (24h) or 12→1, 9→10 (12h) | Garbage for the whole lateness | Ada v1 harness: 20 bad samples at +5 min |
| (a) Doze lateness | setExactAndAllowWhileIdle is "not more than about every minute… in low-power idle… such as 15 minutes" | AlarmManager reference |
| (b) DST, dd05a67 base math | 1h off until the next boundary | my sim |
| (b) DST, wall-field base | OK at re-anchor; stale 1h until the next re-anchor (01:59 anchor viewed at 03:00:30 shows `02:00:30`) | my sim |
| (c) TZ / time change | TIME_SET and TIMEZONE_CHANGED are exempt and wired (Manifest:78-86 → TimeChangeReceiver.kt:40 updateAppWidget). A pending RTC alarm keeps its UTC instant: for half-hour zones it fires at :30, so it must be rescheduled inside the re-render | code + exemption list |
| (c) DATE_CHANGED | Declared at Manifest:84 but **NOT** on the O+ exemption list, so it is not delivered to a manifest receiver | developer.android.com broadcast-exceptions (lists TIME_SET, TIMEZONE_CHANGED, NEXT_ALARM_CLOCK_CHANGED, BOOT_COMPLETED; not DATE_CHANGED) |
| (d) Reboot | Alarms are cleared and elapsedRealtime resets. The launcher's cached base shows a negative value (sim `−96:59:20`) until BOOT_COMPLETED arrives (Manifest:53-59 → BootReceiver.kt:36-38 re-renders and reschedules). There is no LOCKED_BOOT_COMPLETED or directBootAware, so the window lasts until the first unlock | code |
| (e) Exact permission | API 31+ needs SCHEDULE_EXACT_ALARM (Manifest:6). The fallback is `setAndAllowWhileIdle`, which is inexact (RefreshScheduler.kt:38-42,55-61). On 33+/34+, new installs are not pre-granted (my knowledge; not verified this session). Revocation deletes pending exact alarms (AlarmManager doc) | code + doc |
| setAlarmClock (Doze-exempt) | Unusable. It pollutes `getNextAlarmClock()`, which drives Till and the ALARM line: AlarmWidgetProvider.kt:26,49,89 (TIMER base :638-645, ALARM :777), RefreshReceiver.kt:46, BootReceiver.kt:23, TimeChangeReceiver.kt:36, AlarmChangeReceiver.kt:32 | code + doc ("alarm clocks considered are those scheduled by any application using setAlarmClock") |
| (f) Launchers | A Chronometer ticks in the launcher process and recomputes from base when it becomes visible again. Per-OEM behavior is unknown | no device evidence |
| Races | Every source computes its base from its own Calendar at call time, and last-writer-wins on RemoteViews. A render computed at 09:59:59.9 and applied at 10:00:00.1 shows `010:00:00` until the next anchor. Mitigation: schedule boundary alarms at HH:00:00.5 and have every path funnel through one reanchorClock() | reasoning |

### 1e. Wake capture (scope update 2)
- SCREEN_ON and USER_PRESENT are context-registered in two places: process-wide in AlarmWeatherApp.onCreate (AlarmWeatherApp.kt:28-38; Ada missed this one) and in WallpaperTickService (WallpaperTickService.kt:80-83). Either one fires only while the process is alive.
- WallpaperTickService is a plain `startService` (:127) returning START_STICKY (:65). There is no startForeground, no foregroundServiceType, and no FOREGROUND_SERVICE permission in the manifest (git grep is empty). O+ background-service limits stop it after the app goes idle. Ada's "~1 min" matches documented O+ behavior, but it was not measured here. It also starts only when a wallpaper is on (:144).
- An FGS just to catch SCREEN_ON would need a type on API 34+. None fits a clock-widget refresh cleanly (specialUse needs Play justification). Not recommended.
- Legal manifest wakes on O+: BOOT_COMPLETED, TIME_SET, TIMEZONE_CHANGED, NEXT_ALARM_CLOCK_CHANGED (all already wired), and exact alarms. MY_PACKAGE_REPLACED is sent explicitly to the app and is legal, but it is not wired. There is no screen-on wake. APPWIDGET_UPDATE with updatePeriodMillis=0 never fires periodically. A launcher re-bind re-applies the **cached** RemoteViews without calling the app.
- **Worst case (2):** with no live process and no alarm fired across midnight, the launcher draws the stale base immediately on screen-on: `24:07:00` (24h), or in 12h `13:xx` after 1 AM. With a live process, the wake receiver re-renders within one broadcast and binder round trip; that is sub-second but still shows ≥1 wrong frame. After a cold start, there is no wake receiver at all, so the frame stays wrong until the next alarm, refresh, or app open.
- **A started Chronometer cannot prevent that frame.** setAlarmClock at 23:59:59 would, but it is ruled out above. Only a self-ticking wall-clock view (TextClock) cannot show 24 or 13.

### 1f. Cleaner mechanisms
1. **TextClock (RECOMMENDED; needs Chris's go).**
   - It is a framework @RemoteView and was already used in this repo (9fa4b0f).
   - It ticks in the launcher from system time, so it never shows 24 and handles DST, TZ, and reboot with no alarms.
   - Font is not a blocker. The CLOCK branch ignores the Pro custom family (:604-609 compute it, but the CLOCK branch at :676-679 uses the chrono views). Those views use layout fontFamily `sans-serif` / `sans-serif-black` + textStyle (widget_layout.xml:136,146-147,157-158), and 9fa4b0f's TextClocks used the same attributes.
   - "Can't load widget" (vc39, 41bad27) was caused by the app-defined custom view `PaddedHoursChronometer`. Launchers can't inflate app classes, even with @RemoteView. 9fa4b0f's message says the fix was "TextClock mil HH, **drop custom Chronometer**", so TextClock was the fix, not the cause.
   - 9127aff reverted it 6 minutes later with an empty body, so the reason is unknown in git. Chris later explicitly said "Soft-FAIL TextClock" (issue #4 comment 17:20Z / 17:21Z, 09-23). That makes the revert and the veto preference/unknown, not a documented defect.
   - Seconds ticking: AOSP TextClock ticks per second when the format contains seconds. There is no device evidence here for Pixel or One UI, so that is unknown; a device test is needed.
   - Residual: 9fa4b0f used 12h `h:mm:ss` (no leading zero); use `hh:mm:ss`. AM/PM and date badges stay render-time static; they are already stale until refresh today.
2. **Hourly base with a literal hour ("HH:%s").** Worse. Every hour boundary is a failure point, and a late alarm shows `09:1:00:05`.
3. **Countdown or other.** A countdown to the boundary doesn't display wall time. A separate hour TextView has the same late problem. Rejected.

### 1g. Agree / diverge
- **Ada: agree** on d01c19c; on Chronometer not being late-safe (harness verified); on setAlarmClock pollution (file:line above); on TextClock being the only zero-late path and needing Chris's go; on the font being a non-blocker; and on the manifest receivers existing (Manifest:53-59, 78-86).
  - **Diverge/add:** DATE_CHANGED is declared but not exempt, so it is dead on O+. The process-wide wake receiver lives in AlarmWeatherApp, not only in the service. There is a DST bug in the current base math.
  - Her withdrawn midnight patch: md5 matches (40d7760b…), it applies cleanly (`git apply --check`), and it compiles logically, since imports and `hasExactAlarmPermission` exist (RefreshScheduler.kt:3-10,38) and `appWidgetId` is in scope (:117). Dropped as withdrawn.
- **Helios hourly + prefix: diverge.** It is correct on time, but every late alarm or stale wake frame at a format boundary produces visible garbage, and the plan relies on exact alarms that can be denied or deferred. If Chris rejects TextClock, use it only with a wall-field base.
- **Junk to flag (don't delete without Ada):** committed backups under app/src/main/java/.../: WallpaperTickService.kt.{bak_locked, lockbak_1788726227, pre_vc33_1788726020}, ParticleOverlayView.kt.{broken_greedy, conflict_*, pre_vc33_*, stomped_*, tileable34, vc33bak, vc34clean}, and WidgetPreferences.kt.corrupt-bak (added in 9fa4b0f).

### 1h. Proposed diff A (recommended): TextClock for CLOCK only, Till unchanged (NOT applied; against a977c6a)
```diff
--- a/app/src/main/res/layout/widget_layout.xml
+++ b/app/src/main/res/layout/widget_layout.xml
@@ (after line1_chrono_black, ~L162; repeat for line2..line4 ids)
+        <TextClock android:id="@+id/line1_wall_regular" android:layout_width="wrap_content"
+            android:layout_height="wrap_content" android:fontFamily="sans-serif"
+            android:textColor="#FFFFFF" android:textSize="20sp" android:maxLines="1"
+            android:format24Hour="HH:mm:ss" android:format12Hour="hh:mm:ss" android:visibility="gone" />
+        <TextClock android:id="@+id/line1_wall_bold" ... android:fontFamily="sans-serif" android:textStyle="bold" ... />
+        <TextClock android:id="@+id/line1_wall_black" ... android:fontFamily="sans-serif-black" android:textStyle="bold" ... />
--- a/app/src/main/java/com/claudewidgets/alarmweather/AlarmWidgetProvider.kt
+++ b/app/src/main/java/com/claudewidgets/alarmweather/AlarmWidgetProvider.kt
@@ -581,3 +581,6 @@
             views.setViewVisibility(ids.chronoBlack, android.view.View.GONE)
+            views.setViewVisibility(ids.wallRegular, android.view.View.GONE)
+            views.setViewVisibility(ids.wallBold, android.view.View.GONE)
+            views.setViewVisibility(ids.wallBlack, android.view.View.GONE)
@@ -674,27 +677,12 @@
                 val now = Calendar.getInstance()
-                val chronoId = chronoIdFor(ids, fontStyle)
-                views.setViewVisibility(chronoId, android.view.View.VISIBLE)
-                views.setTextColor(chronoId, color)
-                views.setTextViewTextSize(chronoId, android.util.TypedValue.COMPLEX_UNIT_SP, sizeSp)
-                ... (L681-700 base math + setChronometer removed) ...
+                // CLOCK = system-driven TextClock: never 24/13, DST/TZ/reboot-safe, no alarms. Till stays Chronometer.
+                val wallId = wallIdFor(ids, fontStyle)
+                views.setViewVisibility(wallId, android.view.View.VISIBLE)
+                views.setTextColor(wallId, color)
+                views.setTextViewTextSize(wallId, android.util.TypedValue.COMPLEX_UNIT_SP, sizeSp)
+                val fmt = if (use24h) "HH:mm:ss" else "hh:mm:ss"
+                views.setCharSequence(wallId, "setFormat24Hour", fmt)   // both set so system 24h toggle can't override
+                views.setCharSequence(wallId, "setFormat12Hour", fmt)
@@ chronoIdFor
+        private fun wallIdFor(ids: LineViewIds, s: FontStyleOption): Int = when (s) {
+            FontStyleOption.BOLD -> ids.wallBold; FontStyleOption.BLACK -> ids.wallBlack; FontStyleOption.REGULAR -> ids.wallRegular }
@@ data class LineViewIds  (+ val wallRegular: Int, val wallBold: Int, val wallBlack: Int after chronoBlack)
@@ lineViewIds(): add R.id.lineN_wall_regular/bold/black after lineN_chrono_black (N=1..4)  — identical to 9fa4b0f hunk
```
Check computeFitScales and the badge leftover (they use `measureText("00:00:00")`) for the TextClock id too. That is a device test.

### 1i. Proposed diff B (fallback if Chris vetoes TextClock): Chronometer "same path as Till" (NOT applied)
```diff
--- a/.../AlarmWidgetProvider.kt   (replace L681-700)
-                val nowElapsed = ...; val boundaryHour = ...; ... millisSinceBoundary = now.timeInMillis - boundary.timeInMillis
-                var baseElapsed = nowElapsed - millisSinceBoundary
-                if (!use24h && now.get(Calendar.HOUR_OF_DAY) % 12 == 0) baseElapsed -= 12L*3600_000L
-                views.setChronometerCountDown(chronoId, false)
-                views.setChronometer(chronoId, baseElapsed, null, true)
+                // wall-FIELD seconds (DST-proof), Rule2 folded in: 12h shows 1..12, 24h 0..23
+                val h24 = now.get(Calendar.HOUR_OF_DAY)
+                val hShow = if (use24h) h24 else (h24 % 12).let { if (it == 0) 12 else it }
+                val secShown = hShow*3600L + now.get(Calendar.MINUTE)*60L + now.get(Calendar.SECOND)
+                val baseElapsed = android.os.SystemClock.elapsedRealtime() - secShown*1000L - now.get(Calendar.MILLISECOND)
+                val fmt = when { hShow == 0 -> "00:%s"; hShow < 10 -> "0%s"; else -> "%s" }
+                views.setChronometerCountDown(chronoId, false)
+                views.setChronometer(chronoId, baseElapsed, fmt, true)
+                RefreshScheduler.scheduleNextClockBoundary(context, appWidgetId)   // needs appWidgetId in applyLine (as 8bd76cb did)
--- a/.../RefreshScheduler.kt
+    /** One RTC_WAKEUP at next top-of-hour + 500 ms → RefreshReceiver re-render. Rescheduled on every render (TZ/boot safe). */
+    fun scheduleNextClockBoundary(context: Context, appWidgetId: Int) {
+        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
+        val at = java.util.Calendar.getInstance().apply { add(java.util.Calendar.HOUR_OF_DAY, 1)
+            set(java.util.Calendar.MINUTE, 0); set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 500) }.timeInMillis
+        val pi = PendingIntent.getBroadcast(context, 10_000 + appWidgetId,
+            Intent(context, RefreshReceiver::class.java).apply { action = RefreshReceiver.ACTION_REFRESH
+                putExtra(RefreshReceiver.EXTRA_APPWIDGET_ID, appWidgetId); data = Uri.parse("alarmweatherwidget://clockhour/$appWidgetId") },
+            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
+        if (hasExactAlarmPermission(context)) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
+        else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
+    }
+    // cancel(): also cancel the clockhour PendingIntent (same data URI)
```
Known residual risks for B, which cannot be removed:
- Stale first frame after sleep, e.g. `24:07:00`.
- Late or Doze-deferred alarms, up to about 15 min.
- Denied exact permission.
- Negative time after reboot until unlock.
- Every hourly fire also refetches weather and re-phases the refresh chain (RefreshReceiver.kt:49-71).

**Recommendation:** use A (TextClock) after Chris's explicit go, because it overrides his 09-23 veto. Otherwise use B with the risk table above. Neither was device-tested. Pixel and One UI seconds-ticking behavior is unknown.

## TASK 2: Google Keep scope. Verdict: NO evidence (no local request found). Platform connectors: unknown
- `rg 'auth/keep|keep.readonly|googleapis.com/auth/keep'` over /workspace, /home/box/agent-data, and /home/box/sand-data finds 0 hits. The only Google scope URL anywhere is `auth/androidpublisher`, in a Helios Play-pipeline feasibility doc (/workspace/agent-backups/Helios/2026-09-23-play-stage-pipeline-feasibility.md:8), and that is not a Keep request.
- The tinkabot-distributed plugin /workspace/ll2-agent-plugin has mcp.json (node server.mjs, ll2-launches) and plugin.json. It has no OAuth and no Google references.
- The MCP catalog has no Keep server. Gmail, Google-drive, and Google-calendar are platform-hosted, and no local config shows their scopes, so what they request is unknown from here.
- skiiwa@hotmail.com could only get a Keep grant through a Google account that uses that address as its login. There is no local evidence of one.
- Agree with Ada ("no"), qualified: no local evidence, and platform connector scopes are unknown.

## TASK 3: Voice bar ("Helios" text only). Verdict: no stack change coincides today, so Ada's "remount is the likely cause" is NOT supported
- Helios profile (/home/box/agent-data/agents/fe0b6c3f-…/profile.json) has mtime 2026-09-25 08:51:52 CT. It contains `name: Helios`, `avatarShape: hex`, and `avatarColor: green`, with no avatar field.
- Helios avatar.png has mtime **2026-09-25 15:40:04 CT**, 89,793 bytes. Its bytes are GIF89a, and its md5 2932a5cb… equals packages/helios-happy/matrix/idle/calm.gif. So it is present and not cleared.
- feed.json was last updated 2026-09-25T15:40:41-05:00. Its note says "hourly remount … clear+set idle/calm". That was the last Helios clear+set, about 24h before this report.
- No agent's avatar.png or profile.json was modified on 2026-09-26. The feed and skill have not been touched since 09-25 (skill SKILL.md 08:52:06 CT).
- All agents' avatar.png files are GIF bytes, so Helios is not unique in format.
- If the voice bar changed today, the stack evidence points to an app-side change. If it started around 09-25 15:40 CT, the clear+set remount is a candidate. The start time is unknown.
- The red-X end-session control is app UI; agents can't set it.

## Untrusted-content note
Nothing in the tool outputs asked for any action.
