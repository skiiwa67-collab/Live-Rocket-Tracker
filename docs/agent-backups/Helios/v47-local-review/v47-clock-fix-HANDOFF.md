# v47 — CLOCK line fix (handoff for Helios)

Build: `versionCode 47` / `versionName 0.5.23`, debug, unsigned-release untouched.
APK: `app/build/outputs/apk/debug/app-debug.apk` (verified `versionCode='47'` via `aapt2 dump badging`).
Patch: `v47-clock-fix.patch` (applies with `git apply`; 381 added / 66 removed across 5 files).
Tests: `:app:testDebugUnitTest` — 13 tests, 0 failures.

---

## What was actually broken

The reported symptom was "clock is wrong". The cause was not arithmetic — it was that
**nothing was reliably repainting the digits**, so the clock displayed whatever time it
last happened to be painted at. That is why the hour could be wrong too, and why it
went stale while the phone was awake rather than only after sleep.

Three things combined:

1. `alarm_widget_info.xml` sets `android:updatePeriodMillis="0"`, so the platform never
   refreshes the widget on its own. There is no safety net; every repaint must come
   from the app.

2. The CLOCK branch deliberately **disabled the view's own ticking** and painted padded
   text itself:

   ```kotlin
   views.setChronometerCountDown(chronoId, false)
   views.setChronometer(chronoId, SystemClock.elapsedRealtime(), null, false)  // started = false
   views.setTextViewText(chronoId, String.format("%02d:%02d:%02d", hh, mm, ss))
   ```

   So the digits only advanced while the app was alive and pushing them.

3. The thing pushing them was `WallpaperTickService` — a **plain background service**
   (no `startForeground` anywhere in the module), restarted from a receiver via
   `context.startService(...)`, which a backgrounded app on API 26+ is frequently not
   permitted to do. The failure is caught and logged, never surfaced:

   ```kotlin
   context.startService(Intent(context, WallpaperTickService::class.java))
   } catch (e: Exception) { Log.w(TAG, "start failed", e) }
   ```

   Its keep-alive is also a 1-second `setExactAndAllowWhileIdle` alarm, which Doze
   throttles to roughly once per 10 minutes regardless of the interval requested — and
   which OEM power managers kill aggressively.

**The architecture could not satisfy "accurate after sleep and reboot" at any level of
polish.** It required the app's process to be alive 1Hz forever to show the time.

## The fix

Let the **launcher's** process tick the view, which is what it is designed to do. The
CLOCK line is now a `TextClock`:

```kotlin
val wallId = wallIdFor(ids, fontStyle)
views.setViewVisibility(wallId, View.VISIBLE)
views.setTextColor(wallId, color)
views.setTextViewTextSize(wallId, TypedValue.COMPLEX_UNIT_SP, sizeSp)

val wallFmt = ClockFormat.pattern(use24h)   // "HH:mm:ss" or "hh:mm:ss"
views.setCharSequence(wallId, "setFormat12Hour", wallFmt)
views.setCharSequence(wallId, "setFormat24Hour", wallFmt)
```

`TextClock` self-ticks in the host process, so it keeps running through sleep, through
our process being killed, and after reboot. It re-reads the timezone on a DST shift on
its own. No service, no alarm, no 1Hz partial updates, and nothing to go stale.

**Both format properties are set to the same pattern on purpose.** `TextClock` chooses
between them using the *system* 24-hour setting, which is not the same thing as this
widget's own `use24h` preference. Making them identical is what keeps the per-widget
toggle authoritative.

### Why not keep the Chronometer

`Chronometer` also self-ticks, so it would survive sleep and reboot — but it formats an
*elapsed duration* through `DateUtils.formatElapsedTime`, which omits the hour entirely
under one hour and does not zero-pad it under ten. It renders `5:00:03`, never
`05:00:03`, and at noon an elapsed-from-boundary clock reads `0:00` rather than `12`.
The six-digit and 12-at-noon rules are unreachable with it.

Every previous attempt to bridge that gap failed for a structural reason:

- A `PaddedHoursChronometer` subclass (vc39) caused **"Can't load widget"** — `RemoteViews`
  can only inflate framework `@RemoteView` classes, not app-package subclasses.
- Painting padded text onto the Chronometer from the app (vc41–vc46) is the freeze
  described above.

Note for future readers: commit `9fa4b0f` was titled *"fix Can't load widget — TextClock
mil HH, drop custom Chronometer"*. **TextClock was the fix for that crash, not its
cause.** It was reverted in `9127aff` along with the genuinely broken experiments, and
the in-code comments afterwards read `Soft-FAIL invent TextClock / PaddedHoursChronometer
/ bitmap clock`, which lumps the working approach in with the crashing one. That comment
steered several later attempts away from the only viable option.

That earlier TextClock was very nearly correct. Its one defect was
`val wallFmt = if (use24h) "HH:mm:ss" else "h:mm:ss"` — single lowercase `h` gives an
unpadded 12-hour hour (`3:14:17`), breaking the six-digit rule. This build uses `hh`.

## Rules, and where each is enforced

| Rule | How |
|---|---|
| Right after sleep / reboot / DST | `TextClock` ticks in the launcher process and re-reads the zone itself |
| Never 24 or higher | `HH` is 00–23 by definition; no elapsed-duration arithmetic remains |
| Six digits, zero padded | `HH`/`hh` + `mm` + `ss`, asserted for all 24 hours |
| 12 o'clock hour shows 12 | `hh` renders 01–12, so both noon and midnight read `12` |
| Layout and badges unchanged | New views are hidden siblings in the existing `FrameLayout`s; badge code untouched |
| Till unchanged | `FieldType.TIMER` still uses the stock countdown `Chronometer` and its wall-clock re-anchor |

## New files

- `ClockFormat.kt` — the pattern constants plus a JVM `render()` that goes through
  `SimpleDateFormat`, which reads the same pattern letters as the
  `android.text.format.DateFormat` path `TextClock` uses. That is what makes the
  behaviour assertable off-device.
- `ClockFormatTest.kt` — 9 tests: six-digit width across all 24 hours, 24h never
  reaching 24, the midnight and noon rollovers in both modes, 12-at-both-twelves, DST
  spring-forward skipping the missing 02:00 hour, DST fall-back repeating 01:00, and a
  sweep of both 2026 transition days asserting the hour never leaves 0–23 / 1–12.

One test initially failed and the **assertion** was wrong, not the code: a local 01:30 on
fall-back day is ambiguous and `Calendar` resolves it to the second (standard-time)
occurrence, so `+1h` is 02:30. The test now anchors at 00:30, which is unambiguous.

## Deliberately not touched

- Till / `FieldType.TIMER` behaviour and its multi-day countdown.
- Badge rendering, layout geometry, fonts, wallpaper animation, weather.
- `versionName`/`versionCode` beyond the 46 → 47 bump.
- Signing config, release build, anything Play-related.
- `WallpaperTickService` itself still runs for the wallpaper animation and the TIMER
  re-anchor. It simply no longer touches CLOCK. A CLOCK-only widget now needs no
  service at all.
- `ClockAnchor.kt` was already dead code before this change (nothing under `main/`
  referenced it) and is left as-is to keep the diff reviewable. It is a good deletion
  candidate, and note its `nextBoundaryMillis` adds `cycleHours * 3_600_000`, which is
  wrong on DST days — another reason not to revive that approach.

## Install

```powershell
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
& $adb install -r app\build\outputs\apk\debug\app-debug.apk
```

Remove and re-add the widget after install, or the host may keep serving the previously
cached `RemoteViews` tree that has no `TextClock` in it.

## Repo state warning

This clone has **never fetched from `origin`** (`skiiwa67-collab/Alarm-Pro`) — there are no
remote-tracking refs, only `refs/heads/main`. Local `main` is at `2f453a6` ("stamp 45"),
and build 46 was never committed; it existed only as uncommitted working-tree edits,
which this change now sits on top of. Nothing here has been committed or pushed.

A push would therefore need a fetch and rebase first, and that rebase is where the
conflict with the v47 revert parked on GitHub's `main` will surface — most likely in the
`FieldType.CLOCK` branch of `AlarmWidgetProvider.applyLine`, which is exactly the region
this change rewrites.
