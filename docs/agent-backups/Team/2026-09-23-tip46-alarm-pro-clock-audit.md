# tip46 Alarm Pro CLOCK audit — tinkabot (independent)

**When:** 2026-09-23 ~12:19 CT  
**Tree:** `/workspace/alarm-push/Alarm-Pro` main @ `dd05a67` + unpushed tip46 working tree  
**version:** versionCode 46 / versionName 0.5.22  
**Diffstat match:** 4 files, +233/-94  
**Scope (Chris/Helios):** CLOCK only. Soft-FAIL Till/TIMER wrap.  
**Overall: FAIL**  
**Standing:** Soft-FAIL Ada push until tinkabot + Helios both clear. Soft-FAIL Play forever.  

## Surfaces
- AlarmWidgetProvider.kt (applyClockChronometerBase, reanchorChronometersFromWallClock)
- WallpaperTickService.kt (maybeRefreshCustomFontClock, lastClockLocalDay)
- RefreshScheduler.kt (scheduleNextLocalMidnight)
- RefreshReceiver.kt (ACTION_DAY_ROLL)
- vc38 ancestor: d01c19c
- vc40 revert: 9127aff

## Checklist (CLOCK only)

### 1) CLOCK wrap 24:00:00 to 00:00:00 — PASS
- 24h base = elapsed since local midnight (applyClockChronometerBase ~L878-893).
- Defensive millisSinceBoundary %= DAY_MS when use24h (~L892-893).

### 2) noon/midnight AM/PM + 12h hour digit (Rule2) — PASS (logic; six-digit pad FAIL below)
- AM/PM badge from Calendar.AM_PM (~L712).
- Rule2 KEEP: when !use24h and HOUR_OF_DAY % 12 == 0, baseElapsed -= TWELVE_HOUR_MS (~L896-898)
  so 12:MM:SS shows hour digit (stops 12:44 painting 44:00).

### 3) live System.currentTimeMillis() re-anchor — PASS
- applyClockChronometerBase uses Calendar.getInstance() every call.
- reanchorChronometersFromWallClock (~L812).
- WallpaperTickService re-anchors each wall second (~L362-373); day change full update (~L352-358).
- Stock Chronometer ticks between 1Hz lean re-anchors; no TextClock/outline time bitmaps.

### 4) vc38 day-roll restore d01c19c intent — PASS
- RefreshScheduler.scheduleNextLocalMidnight matches d01c19c (second 1 after local midnight, ACTION_DAY_ROLL).
- RefreshReceiver keeps ACTION_DAY_ROLL.
- WallpaperTickService lastClockLocalDay 23->00 full re-anchor + date badges.
- AlarmWidgetProvider calls scheduleNextLocalMidnight at CLOCK wire (~L676).

### 5) six-digit HH MM SS both modes — FAIL
- All CLOCK setChronometer calls pass format null (L858, L860, L901).
- Stock Chronometer / DateUtils.formatElapsedTime:
  - 00:15:30 paints "15:30" (no hour digit)
  - 09:xx:xx paints "9:xx:xx" (unpadded hour)
  - 12:44:00 paints "12:44:00" (Rule2 base shift fixes hour digit only at 12 oclock)
- Chris hard display: six-digit HH MM SS both modes. Stock null format cannot do that.

## Ada fix before re-audit
- Clock digits must always paint HH:MM:SS with zero-padded hour in 12h and 24h.
  Stock Chronometer null format cannot pad hours or show a zero hour.
- Do not rework Till % DAY_MS (out of scope).
- Do not Publish/Rollout/Play.

Soft-FAIL invent. Soft-FAIL Play forever.
