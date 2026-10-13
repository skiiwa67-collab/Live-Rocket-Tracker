# Alarm Pro CLOCK freeze dig — tinkabot Soft-PASS (2026-09-26 ~8:08–8:11 CT)

**Tree:** `/workspace/alarm-push/Alarm-Pro`  
**Git:** `main` == `origin/main` @ `8bd76cb` (`fix(tip46): CLOCK six-digit HH:MM:SS via padded setTextViewText`)  
**Parent:** `dd05a67` (pre-tip46 CLOCK still used `setChronometer(..., started=true)`)  
**Scope:** Soft-PASS dig only. Soft-FAIL invent Soft-PASS. Soft-FAIL Play. Soft-FAIL apply patch. Soft-FAIL tip46 re-audit (cancelled). Soft-FAIL message Helios/Ada/Chris. Soft-FAIL push / compile-fix.

Cross-check: Helios same-day softpass note exists at `/workspace/agent-backups/Helios/2026-09-26-alarm-pro-clock-freeze-softpass.md`. This dig independently verified the CURRENT tree; conclusions agree; cites below are from live sources.

---

## Symptom (Chris + Helios screenshots)

- Status-bar time **advances**.
- Big orange HUD **CLOCK freezes** (examples: stuck `04:00:0` / `08:00:07`).
- Yellow **"Till alarm"** countdown **keeps ticking**.
- Chris: CLOCK is supposed to be locked to that same countdown timer path; it is not.

---

## A) Git verify (done)

```
## main...origin/main
8bd76cb fix(tip46): CLOCK six-digit HH:MM:SS via padded setTextViewText
dd05a67 fix(#3): Soft-PASS 12h widget clock hour digit in 12 o'clock hour
a8ea739 Alarm Pro stamp 46 bump: versionCode 46 / versionName 0.5.22
```

tip46 touched: `AlarmWidgetProvider.kt`, `WallpaperTickService.kt`, `RefreshScheduler.kt`, `RefreshReceiver.kt`.

---

## B–C) CLOCK vs TIMER update paths (current HEAD)

### TIMER / Till — launcher self-tick (started=true)

`AlarmWidgetProvider.applyTimerChronometerBase` L827–842:

```kotlin
views.setChronometerCountDown(chronoId, true)
views.setChronometer(chronoId, baseElapsed, null, true)  // started=true
```

Launcher Chronometer process ticks forever after one RemoteViews apply. No 1Hz process wake required for second digits.

### CLOCK — tip46 snapshot only (started=false + setTextViewText)

`AlarmWidgetProvider.applyClockChronometerBase` L856–880:

```kotlin
views.setChronometerCountDown(chronoId, false)
views.setChronometer(chronoId, android.os.SystemClock.elapsedRealtime(), null, false) // started=false
views.setTextViewText(chronoId, String.format(Locale.US, "%02d:%02d:%02d", hh, mm, ss))
```

Comments L846–851 / L862–863 explicitly Soft-FAIL Chronometer auto-tick so DateUtils cannot overwrite padded HH:MM:SS. Result: visible CLOCK digits are a **static TextView snapshot** until the next process-side re-paint.

### Shared lean re-anchor (1Hz)

`AlarmWidgetProvider.reanchorChronometersFromWallClock` L793–818:

- Builds empty `RemoteViews(widget_layout)`
- For each enabled CLOCK/TIMER line → `applyTimerChronometerBase` / `applyClockChronometerBase`
- `appWidgetManager.partiallyUpdateAppWidget`
- `WallpaperTickService.markPaintedWallSec`

`WallpaperTickService.maybeRefreshCustomFontClock` L333–375:

- Every wall second (`wallSec != lastPaintedWallSec`) → `reanchorChronometersFromWallClock`
- Civil day change → `pushFullClockTimer` → full `updateAppWidget` (vc38 day-roll)

Called from `WallpaperTickService.push` L317–318 **even when wallpaper Off** (wallpaper bitmap block alone is gated; Chronometer re-anchor is not). `startIfNeeded` L145–148 keeps the service for `anyClockOrTimer`, not only wallpaper.

### Pre-tip46 CLOCK (regression baseline @ dd05a67)

CLOCK used the **same Chronometer family as Till**:

```kotlin
views.setChronometerCountDown(chronoId, false)
views.setChronometer(chronoId, baseElapsed, null, true)  // started=true, wall-anchored base
```

with midnight/noon boundary + Rule2 −12h shift for 12 o'clock hour. Comment then: *"ticking to the second via the exact same Chronometer mechanism as the alarm countdown"*. tip46 removed that lock to force padded six digits.

---

## Call graph

```
WallpaperTickService.onStartCommand
  └─ Handler tick (TICK_MS 250 / FLUID 90 / RAIN 60)
       └─ push(t)
            ├─ [if wallpaper != Off] partial wallpaper bitmap update
            └─ maybeRefreshCustomFontClock          // tip46: even wallpaper Off
                 ├─ day change → updateAppWidget (full)
                 └─ wallSec != last → reanchorChronometersFromWallClock
                      ├─ TIMER → applyTimerChronometerBase  (started=true)  → launcher ticks
                      └─ CLOCK → applyClockChronometerBase  (started=false + setTextViewText)
                           └─ partialUpdateAppWidget

WallpaperKeepAliveReceiver (~1s AlarmManager)
  └─ startIfNeeded + sampleIfSamplerStale → push (same path)

WakeRefreshHelper / SCREEN_ON / Boot / RefreshReceiver / TimeChange / AlarmChange
  └─ updateAppWidget → applyLine → applyClock/TimerChronometerBase (same started true/false split)

AppWidget updatePeriodMillis = 0  (alarm_widget_info.xml) — no OS 1Hz help
RefreshScheduler.scheduleNext = user interval (1/5/10 min) — weather/alarm, not CLOCK seconds
scheduleNextLocalMidnight = day-roll only
```

---

## Why Till ticks and CLOCK freezes

| | Till (TIMER) | CLOCK (tip46) |
|--|--|--|
| View | Chronometer id | Same Chronometer id family |
| `setChronometer(..., started=)` | **true** L838 | **false** L865 |
| Digit driver | Launcher Chronometer auto-tick | `setTextViewText` snapshot L877–880 |
| Needs 1Hz process wake for seconds? | **No** | **Yes** (`maybeRefreshCustomFontClock` → reanchor) |
| Survives WallpaperTickService / process death / Doze gap? | **Yes** (already started in launcher) | **No** — freezes at last painted `"HH:MM:SS"` |

Status bar advances because device wall clock is fine. Till keeps counting because it is on the launcher Chronometer path. CLOCK freezes whenever the 1Hz sampler path misses CLOCK-only paints (`lastPaintedWallSec` stale, service killed, keep-alive AlarmManager delayed under Doze, process death between keep-alives). Wallpaper Off alone does **not** skip re-anchor in current code (L317–318 Soft-PASS); freeze is the **started=false dependency**, not an Off-skip bug.

Chris "fix made it worse": tip46 intentionally swapped CLOCK off the never-stop Chronometer class Till still uses.

---

## Secondary observation — truncated last digit (`04:00:0`)

- tip46 always paints 8-char `"%02d:%02d:%02d"` (L877–880).
- `widget_layout.xml` L17–23: `maxLines="1"` → overflow **clips** (does not wrap). Historical note mentions `"9:12:34"` wrap bugs.
- Side badges share `line*_row` horizontal space; `computeFitScales` deliberately **excludes** badge width from CLOCK natural width (L452–459); leftover calc uses `measureText("00:00:00")` then may still show side badges when `leftover >= 50dp` (L700–722).
- Soft-PASS as likely layout clip of the last second digit under tight width + large sizeSp. Soft-FAIL invent clip patch without device measure. Primary freeze root cause stands without this.

---

## D–F) Cadence checklist

| Check | Result |
|--|--|
| D wallpaper Off skip of CLOCK re-anchor? | **No** on current tree — `maybeRefreshCustomFontClock` always after wallpaper block; `startIfNeeded` keeps service for CLOCK/TIMER |
| E Chronometer vs TextView ids | CLOCK+TIMER both use Chronometer view ids (`chronoIdFor`); tip46 paints CLOCK via `setTextViewText` on that Chronometer (extends TextView). Not a separate TextView id. |
| F AppWidget / AlarmManager cadence | `updatePeriodMillis=0`; weather refresh minutes; day-roll midnight+1s; CLOCK seconds depend entirely on WallpaperTickService 1Hz + keep-alive |

---

## Root cause (3–6 sentences)

tip46 (`8bd76cb`) fixed DateUtils unpadded/omitted hours by turning CLOCK into a **static padded snapshot**: `applyClockChronometerBase` L864–880 calls `setChronometer(..., started=false)` then `setTextViewText("%02d:%02d:%02d")`. TIMER/Till stayed on `applyTimerChronometerBase` L837–838 `setChronometer(..., started=true)`, so the launcher keeps ticking Till forever. CLOCK seconds now depend on `WallpaperTickService.maybeRefreshCustomFontClock` → `reanchorChronometersFromWallClock` (L333–374 / L793–818) every wall second; when that process-side 1Hz path stalls, CLOCK freezes while Till and the status bar continue. Pre-tip46 (`dd05a67`) CLOCK used `setChronometer(..., started=true)` with a wall-anchored base — the same never-stop class as Till — which matches Chris HARD ("locked to that same countdown timer"). Wallpaper Off is not the skip; the tip46 started=false design is.

---

## Minimal Ada patch (stage only — Soft-FAIL apply here / Soft-FAIL Play)

**Evidence-supported direction (matches Helios Soft-PASS + pre-tip46 code):**

1. **Restore CLOCK to Chronometer self-tick** inside `applyClockChronometerBase` — restore wall-anchored `baseElapsed` from `dd05a67` (midnight/noon boundary + Rule2 −12h when `!use24h && HOUR_OF_DAY % 12 == 0`) and call:
   ```kotlin
   views.setChronometerCountDown(chronoId, false)
   views.setChronometer(chronoId, baseElapsed, null, true)  // started=true
   ```
   Remove (or Soft-FAIL as sole tick driver) the `setTextViewText` padded snapshot path that requires 1Hz for every second.

2. **Keep** `reanchorChronometersFromWallClock` / `maybeRefreshCustomFontClock` as **resync** on wake / drift / day-roll — Soft-FAIL as the only second source.

3. **Six-digit HH:MM:SS on the ticking path** remains an open Soft-PASS for Ada’s stage branch. Stock Chronometer `format` only wraps `DateUtils.formatElapsedTime` (`%s`) and **cannot** zero-pad hours or show hour under 1h. Soft-FAIL invent TextClock / `@RemoteView` subclass / bitmap clock in this dig (tip46 Soft-FAIL invent still stands until Ada Soft-PASS a concrete format/view on the *started=true* path). Do not ship another started=false snapshot as the second driver.

4. Soft-FAIL agent Play publish; Ada stages for Chris.

**Concrete restore skeleton (from `dd05a67` CLOCK body → current `applyClockChronometerBase`):**

```kotlin
private fun applyClockChronometerBase(views: RemoteViews, chronoId: Int, use24h: Boolean) {
    val now = Calendar.getInstance()
    val nowElapsed = android.os.SystemClock.elapsedRealtime()
    val boundaryHour = if (use24h || now.get(Calendar.HOUR_OF_DAY) < 12) 0 else 12
    val boundary = (now.clone() as Calendar).apply {
        set(Calendar.HOUR_OF_DAY, boundaryHour)
        set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }
    var baseElapsed = nowElapsed - (now.timeInMillis - boundary.timeInMillis)
    // tip46 Rule1: optional %= DAY_MS if Ada re-carries defensive clamp for 24h
    if (!use24h && now.get(Calendar.HOUR_OF_DAY) % 12 == 0) {
        baseElapsed -= 12L * 60L * 60L * 1000L  // Rule2 KEEP
    }
    views.setChronometerCountDown(chronoId, false)
    views.setChronometer(chronoId, baseElapsed, null, true) // NEVER-STOP with Till
    // Soft-FAIL setTextViewText snapshot as sole tick driver
}
```

Clipping (`04:00:0`): Soft-FAIL invent layout change here; treat as follow-up Soft-PASS once freeze is gone (or Ada measures leftover vs Chronometer paint width on device).

---

## Soft-FAIL gaps

- No device logcat proving WallpaperTickService gap during a freeze window this session.
- No Soft-PASS Chronometer format / custom view for padded HH:MM:SS while `started=true`.
- Soft-FAIL invent Soft-PASS. Soft-FAIL Play forever (agent). Soft-FAIL apply/push/compile from tinkabot.

---

## Absolute report path

`/workspace/agent-backups/tinkabot/2026-09-26-alarm-pro-clock-freeze-dig.md`  
Mirror: `/workspace/agent-backups/team/2026-09-26-alarm-pro-clock-freeze-dig.md`
