# Alarm Pro home-screen widget — orange CLOCK freeze Soft-PASS
**Date:** 2026-09-26 (America/Chicago)  
**Dig agent:** Helios / Grok Bot executor  
**Chris HARD:** do NOT invent a cosmetic lag patch; Soft-PASS cause first; lock CLOCK to the same never-stop Chronometer path as Till. Soft-FAIL agent Play publish.

---

## 1. Symptom (Chris screenshots 2026-09-26)

| Shot | Status bar | Orange CLOCK | Yellow Till alarm | Next alarm |
|------|------------|--------------|-------------------|------------|
| 1 | 08:02 | frozen ~08:00:07 | still ticking | 05:00 |
| 2 | 04:30 | stuck `"04:00:0"` (last digit clipped) | still ticking | 05:00 |
| 3 | 04:13 | `"04:00:0"` | `24:46:47` | 05:00 |

**Shot 3 math Soft-PASS:** next 05:00 minus Till `24:46:47` ⇒ wall now ≈ **04:13:13**. Countdown uses correct live wall time; orange CLOCK does not.

Ghost grey overlay behind clock still shows live launch/T- style text in some shots (wallpaper `animation_overlay` path — Soft-FAIL which glyph layer until on-device confirm).

Unstick historically: Settings→Save (full `updateAppWidget`) — same family as ISSUE1.md.

---

## 2. Answers to focus questions

### Q1 — How is orange wall clock rendered?
**tip46 (shipped Soft-PASS tip46 day receipt vc46 / `8bd76cb`):** Chronometer **view id**, but **not** Chronometer auto-tick.

- `applyLine` CLOCK branch calls `applyClockChronometerBase`.
- That function sets `started=false`, then paints padded `"HH:MM:SS"` via `setTextViewText` on the Chronometer id (TextView subclass).
- Dig / mk2 / pre-tip46: Chronometer counting **UP** from midnight/noon boundary with `started=true` (launcher self-ticks).

**Not** a live `TextClock`. **Not** outline bitmap for tip46 CLOCK digits (badges are bitmaps). Custom-font aww path can freeze CLOCK via `putTextBitmap` — separate Soft-FAIL if Pro font family is on.

### Q2 — How is Till alarm rendered?
**Native Chronometer countdown** (`FieldType.TIMER`, label `"Till alarm"`):

- `setChronometerCountDown(chronoId, true)`
- `setChronometer(chronoId, baseElapsed, null, true)` → **`started=true`**
- Launcher process ticks forever; no 1Hz app wake required between full refreshes.

### Q3 — Why Till ticks while CLOCK freezes / truncates to `"04:00:0"`?
**Split tick sources Soft-PASS:**

1. Till = launcher Chronometer (`started=true`) → keeps ticking after app process / `WallpaperTickService` dies.
2. CLOCK tip46 = static `setTextViewText` snapshot → only advances when `WallpaperTickService` 1Hz `reanchorChronometersFromWallClock` runs.
3. When that wake dies (Doze / OEM kill / sampler stop Soft-FAIL exact OEM path), CLOCK freezes on last paint; Till continues.

**Truncation `"04:00:0"` Soft-PASS secondary:** tip46 paints full `"%02d:%02d:%02d"` (8 chars). `widget_layout.xml` documents `maxLines="1"` so over-wide digits **clip** instead of wrap (`"9:12:34"` → `"9:12:"` / `"34"` was the old wrap bug). Frozen paint near `04:00:0x` + width clip ⇒ visible `"04:00:0"`. Not Chronometer DateUtils omitting a digit mid-tick.

### Q4 — tip46 “fix” that made lagging worse?
**Commit intent (Helios tip46 day receipt):**  
`fix(tip46): CLOCK six-digit HH:MM:SS via padded setTextViewText`

Pre-tip46 CLOCK used the **same** Chronometer self-tick family as Till (mk2 ~532–577; dig vc37 “always Chronometer”). tip46 Soft-FAIL’d Chronometer `DateUtils.formatElapsedTime` (omits hour under 1h / unpads under 10h) and replaced CLOCK tick with **1Hz re-paint**. That made CLOCK dependent on a fragile wake path while Till stayed robust → lagging/freeze **worse** exactly as Chris saw.

### Q5 — Soft-PASS root cause + minimal patch draft
See §§3–6.

---

## 3. Call graph (tip46)

```
WallpaperTickService.onStartCommand
  └─ Handler tick Runnable (TICK_MS 250 / rain 60 / fluid 90)
       └─ push(context, t)
            ├─ [optional] ParticleOverlayView.renderWallpaper → animation_overlay bitmap
            └─ maybeRefreshCustomFontClock(...)
                 ├─ civil day change → pushFullClockTimer → updateAppWidget (full)
                 └─ wallSec != lastPaintedWallSec
                      └─ AlarmWidgetProvider.reanchorChronometersFromWallClock
                           ├─ TIMER → applyTimerChronometerBase  (started=true countdown)
                           └─ CLOCK → applyClockChronometerBase  (started=false + setTextViewText)
                                └─ AppWidgetManager.partiallyUpdateAppWidget

RefreshReceiver (minutes) → updateAppWidget (full plate; also re-arms CLOCK snapshot)
SCREEN_ON / USER_PRESENT (context-registered in WallpaperTickService) → bonus full rebuild
```

**Failure mode Soft-PASS:** any stop of `WallpaperTickService` / Handler tick / process death leaves Till Chronometer alive in launcher and CLOCK stuck on last `setTextViewText`.

---

## 4. File:line evidence (quotes)

Sources on box: decoded tip46 from `/workspace/alarm-push/tip46-studio-b64/*.b64` → `/tmp/tip46_*.kt`; contrasts `/workspace/mk2src/`, `/workspace/alarm-pro-dig/`, `/workspace/aww/`; ISSUE1 `/workspace/Alarm-Pro/ISSUE1.md`; receipt `/workspace/agent-backups/Helios/2026-09-23-tip46-alarm-pro-day-receipt.md`.

### 4.1 tip46 CLOCK — started=false + setTextViewText (root)

`AlarmWidgetProvider.kt` (tip46) `applyClockChronometerBase`:

```
// Soft-FAIL Chronometer auto-tick (DateUtils path). Started=false so 1Hz
// setTextViewText owns the visible digits.
views.setChronometerCountDown(chronoId, false)
views.setChronometer(chronoId, android.os.SystemClock.elapsedRealtime(), null, false)
...
views.setTextViewText(
    chronoId,
    String.format(Locale.US, "%02d:%02d:%02d", hh, mm, ss)
)
```
**Lines ~862–880** (`/tmp/tip46_AlarmWidgetProvider.kt`).

Kdoc ~846–851 Soft-FAIL start Chronometer so DateUtils does not overwrite padded text — **intentional** decoupling from never-stop timer.

### 4.2 tip46 TIMER — started=true (why Till lives)

```
views.setChronometerCountDown(chronoId, true)
views.setChronometer(chronoId, baseElapsed, null, true)
```
**Lines ~837–838** `applyTimerChronometerBase`.

### 4.3 tip46 1Hz re-paint gate

`reanchorChronometersFromWallClock` ~793–818: partial update; CLOCK → `applyClockChronometerBase`, TIMER → `applyTimerChronometerBase`.

`WallpaperTickService.maybeRefreshCustomFontClock` ~363–374:
```
if (wallSec != last) {
    ...
    AlarmWidgetProvider.reanchorChronometersFromWallClock(context, mgr, id, nextAlarm)
}
```
Service kept alive for Chronometer re-anchor even if wallpaper Off (~147–148). Still a **process+Handler** dependency CLOCK should not need.

### 4.4 Pre-tip46 / dig never-stop CLOCK (target class)

mk2 `AlarmWidgetProvider.kt` ~532–577:
```
views.setChronometerCountDown(chronoId, false)
views.setChronometer(chronoId, baseElapsed, null, true)  // started=true, wall-anchored UP
```

dig Soft-PASS #3 (−12h in 12 o'clock hour so `"12:MM:SS"` not `"M:SS"`) ~692–700:
```
if (!use24h && now.get(Calendar.HOUR_OF_DAY) % 12 == 0) {
    baseElapsed -= 12L * 60L * 60L * 1000L
}
views.setChronometer(..., baseElapsed, null, true)
```

### 4.5 Layout clip (truncation)

`widget_layout.xml` header (~17–23): without `maxLines="1"`, over-wide time wraps; with it, **clips**. Explains `"04:00:0"` without inventing a separate digit bug.

### 4.6 ISSUE1 family

`/workspace/Alarm-Pro/ISSUE1.md`: painted clock was already known fragile vs live Chronometer; RefreshReceiver chain can die; Settings→Save was recovery. tip46 made CLOCK **more** dependent on wake, not less.

---

## 5. Root cause confidence

| Claim | Confidence |
|-------|------------|
| tip46 CLOCK is snapshot (`started=false` + `setTextViewText`), not Chronometer tick | **HIGH Soft-PASS** (source + receipt) |
| Till TIMER is `started=true` Chronometer | **HIGH Soft-PASS** |
| Freeze-while-Till-ticks = wake/reanchor died, launcher Chronometer lived | **HIGH Soft-PASS** (matches all 3 shots + math) |
| tip46 padding fix made lagging worse vs pre-tip46 Chronometer CLOCK | **HIGH Soft-PASS** (intent + behavior split) |
| `"04:00:0"` = last paint + `maxLines` clip | **MED Soft-PASS** (layout comment; no on-device measure) |
| Ghost grey live T- = `animation_overlay` still updating or stale layer | **LOW Soft-FAIL** (needs shot→view id map) |
| Exact OEM kill path of WallpaperTickService on Chris Razr/Nova | **Soft-FAIL** (not required once Chronometer lock removes dependency) |

**Soft-PASS verdict:** root cause locked. Safe to draft minimal Chronometer-lock patch for Ada (stage only).

---

## 6. Proposed minimal patch draft (Ada — stage / Studio only)

**Goal (Chris HARD):** CLOCK locked to same never-stop Chronometer timer class as Till. Soft-FAIL keeping `started=false` + `setTextViewText` as the sole second driver. Soft-FAIL invent TextClock / custom Chronometer subclass / outline bitmap clock / Play publish by agent.

### 6.1 `AlarmWidgetProvider.applyClockChronometerBase` (tip46)

Replace snapshot body with wall-anchored **UP** Chronometer (`started=true`), same structure as dig/mk2:

```kotlin
private fun applyClockChronometerBase(
    views: RemoteViews,
    chronoId: Int,
    use24h: Boolean
) {
    val nowElapsed = android.os.SystemClock.elapsedRealtime()
    val now = Calendar.getInstance()
    val boundaryHour = if (use24h || now.get(Calendar.HOUR_OF_DAY) < 12) 0 else 12
    val boundary = (now.clone() as Calendar).apply {
        set(Calendar.HOUR_OF_DAY, boundaryHour)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    var baseElapsed = nowElapsed - (now.timeInMillis - boundary.timeInMillis)
    // dig Soft-PASS #3: 12h mode at HOUR_OF_DAY 0 or 12 → show 12:MM:SS not M:SS
    if (!use24h && now.get(Calendar.HOUR_OF_DAY) % 12 == 0) {
        baseElapsed -= 12L * 60L * 60L * 1000L
    }
    views.setChronometerCountDown(chronoId, false)
    // Soft-PASS never-stop: same started=true class as TIMER
    views.setChronometer(chronoId, baseElapsed, null, true)
    // Soft-FAIL setTextViewText as tick source here
}
```

Optional RemoteViews `setChronometer(..., format, ...)` / `setFormat` only if it still auto-ticks — Soft-FAIL formats that require stopping Chronometer.

### 6.2 `reanchorChronometersFromWallClock` / `WallpaperTickService`

- Keep lean re-anchor as **resync** (wake, day-roll, SCREEN_ON, drift correct) — Soft-FAIL as the only CLOCK tick source.
- Day-roll full `updateAppWidget` (vc38) KEEP for badges 23→00.
- Do not remove Till Chronometer path.

### 6.3 Out of scope this patch

- Soft-FAIL Play publish / Submit / Rollout (Chris alone).
- Soft-FAIL invent padded DateUtils replacement that reintroduces `started=false`.
- Soft-FAIL custom font `putTextBitmap` CLOCK freeze (aww) unless Chris confirms Pro font on — if yes, same Chronometer lock, Soft-FAIL bitmap tick.
- Cosmetic “call update more often” without Chronometer lock = Soft-FAIL (Chris HARD).

### 6.4 Acceptance (Ada / Chris)

1. Kill/force-stop app or let Doze kill sampler → orange CLOCK **keeps ticking** with Till for ≥30+ min screen-off then screen-on (ISSUE1 bar).
2. Status bar vs CLOCK within ~1s after SCREEN_ON without Settings→Save.
3. 12h Soft-PASS: 12:xx shows hour digit `12` (dig −12h), not `M:SS`.
4. 24h Soft-PASS: never hours ≥24 between day-roll full updates; day-roll still snaps badges.
5. Till multi-day (e.g. 24h+) unchanged Soft-PASS (Soft-FAIL `% DAY_MS`).

---

## 7. Soft-FAIL gaps

1. On-device confirm WallpaperTickService dead at freeze time (logcat `WallpaperTick`) — logical Soft-PASS does not need it for Chronometer lock.
2. Exact truncate pixel measure for `"04:00:0"` (Nova + fitScale) — MED only.
3. Ghost grey launch T- view-id Soft-FAIL until screenshot annotation.
4. Whether Chris tip46 device has Pro custom font (aww bitmap CLOCK branch) Soft-FAIL — tip46 studio source uses Chronometer id path for CLOCK digits.
5. Unpadded Chronometer hour under 10h (`9:05:03` vs `09:05:03`) returns if DateUtils path restored — Chris HARD prioritizes never-stop over tip46 six-digit padding; document as known Chronometer Soft-FAIL or follow-up format-only Soft-PASS on ticking path.
6. Product source on this dig was **not** modified (Chris HARD dig-only).

---

## 8. Soft-PASS summary (one paragraph)

tip46 intentionally stopped CLOCK Chronometer auto-tick (`started=false`) and painted padded HH:MM:SS via `setTextViewText`, advancing only when `WallpaperTickService` 1Hz `reanchorChronometersFromWallClock` runs; Till kept `started=true` Chronometer and therefore keeps ticking when that wake dies — matching Chris’s 2026-09-26 screenshots (frozen orange clock, live Till, Till math proving correct now, clipped `"04:00:0"`). Minimal Ada patch: restore CLOCK to wall-anchored Chronometer `started=true` (mk2/dig + dig −12h Soft-PASS), keep reanchor as resync only; Soft-FAIL cosmetic 1Hz-only fixes and Soft-FAIL agent Play publish.
