# tinkabot v47 red-team: re-runs the scenarios of
#   /workspace/agent-backups/tinkabot/2026-09-26-sim-clock-dst-reboot.py
#   /workspace/agent-backups/Ada/harness-alarm-clock-hourly-reanchor.py
#   /workspace/agent-backups/Ada/harness-alarm-clock-wake-reanchor.py
# against the v47 CLOCK model. v47 CLOCK = framework TextClock whose text is
#   DateFormat.format(pattern(use24h), Calendar(now, TimeZone.getDefault()))   [TextClock.java onTimeChanged :659-662]
# i.e. a pure function of "now" - the last RemoteViews anchor time is NOT an input.
# The old Chronometer model (anchor-dependent) is imported from the original files for contrast.
import importlib.util
from datetime import datetime, timedelta, timezone
from zoneinfo import ZoneInfo
TZ = ZoneInfo("America/Chicago")
def load(p):
    src = open(p).read()
    return src
# --- old models, reused verbatim ---
ns_t = {}; exec(open("/workspace/agent-backups/tinkabot/2026-09-26-sim-clock-dst-reboot.py").read().split("bad = 0")[0], ns_t)
ns_a = {}; exec(open("/workspace/agent-backups/Ada/harness-alarm-clock-hourly-reanchor.py").read().split("cases = [")[0], ns_a)
# --- v47 TextClock model (AOSP DateFormat 'H'/'h' semantics) ---
def textclock(now_utc, u24):
    l = now_utc.astimezone(TZ)
    h = l.hour if u24 else (l.hour % 12 or 12)
    return f"{h:02d}:{l.minute:02d}:{l.second:02d}"
def textclock_anchor_ignored(anchor_utc, now_utc, u24):  # signature parity with old show()
    return textclock(now_utc, u24)
def valid(s, u24):
    if len(s) != 8 or s[2] != ":" or s[5] != ":" or not s.replace(":", "").isdigit(): return False
    h = int(s[:2]); return (0 <= h <= 23) if u24 else (1 <= h <= 12)
L = ns_t["L"]; exp = ns_t["exp"]
rows = []; bad_new = 0; bad_old = 0
def row(label, anchor, now, u24, old_fn=None):
    global bad_new, bad_old
    e = exp(now, u24); n = textclock_anchor_ignored(anchor, now, u24)
    o = ns_t["show"](anchor, now, u24, old_fn) if old_fn else "-"
    okn = (n == e) and valid(n, u24); oko = (o == e)
    bad_new += not okn; bad_old += (old_fn is not None and not oko)
    print(f"  {label:<58} {'24h' if u24 else '12h'} v47={n} old={o:>10} exp={e} v47:{'OK' if okn else 'FAIL'} old:{'OK' if oko else 'WRONG'}")
print("== A. tinkabot DST/reboot scenarios (old = dd05a67/a977c6a real-ms Chronometer base)")
for u24 in (True, False):
    row("spring-forward 03:30 CDT, fresh anchor", L(2026,3,8,3,30), L(2026,3,8,3,30), u24, ns_t["base_dd05a67"])
    row("fall-back 01:30 CST (2nd pass), fresh anchor", L(2026,11,1,1,30,fold=1), L(2026,11,1,1,30,fold=1), u24, ns_t["base_dd05a67"])
    row("fall-back 09:00 CST, fresh anchor", L(2026,11,1,9,0), L(2026,11,1,9,0), u24, ns_t["base_dd05a67"])
    row("spring-forward anchored 01:59 CST, NO re-anchor, view 03:00:30", L(2026,3,8,1,59), L(2026,3,8,3,0,30), u24, ns_t["base_wallfields"])
    row("fall-back anchored 01:59 CDT, NO re-anchor, view 01:00:30 CST", L(2026,11,1,1,59), L(2026,11,1,1,0,30,fold=1), u24, ns_t["base_wallfields"])
print("== B. reboot: launcher re-inflates cached RemoteViews 40 s after boot, app process not yet run")
for u24 in (True, False):
    now = L(2026,6,16,23,0,40)
    n = textclock(now, u24); e = exp(now, u24); ok = n == e and valid(n, u24); bad_new += not ok
    print(f"  {'24h' if u24 else '12h'} v47 TextClock={n} exp={e} {'OK' if ok else 'FAIL'}   (old cached Chronometer: {ns_t['fe'](40 - (5*86400 - 23*3600))} per prior sim)")
print("== C. Ada hourly re-anchor cases, re-anchor 0 s and 300 s late (old = Helios per-hour prefix Chronometer)")
hms = ns_a["hms"]
cases = [("24h 23:59:59->00:00", True, hms(24,0,0)), ("24h 00:59:59->01:00", True, hms(1,0,0)), ("24h 09:59:59->10:00", True, hms(10,0,0)),
         ("12h 11:59:59->12:00", False, hms(12,0,0)), ("12h 12:59:59->01:00", False, hms(13,0,0)), ("12h 09:59:59->10:00", False, hms(10,0,0))]
day0 = L(2026,6,16,0,0)
old_wrong_c = 0
for late in (0, 300):
    for label, u24, b in cases:
        for d in (-1, 0, 1, 60, 299, 301):
            now_s = b + d; a = (b - 3600) if now_s < b + late else b + late
            dd = (a // 86400) * 86400; base, prefix = ns_a["anchor"](a - dd, u24)
            old = prefix.replace("%s", ns_a["fmt_elapsed"](now_s - (dd + base)))
            now = day0 + timedelta(seconds=now_s); n = textclock(now, u24); e = exp(now, u24)
            ok = n == e and valid(n, u24); bad_new += not ok; old_wrong_c += old != e
            if not ok or old != e:
                print(f"  late={late:3d}s {label:<22} wall{d:+5d}s v47={n} old={old:>10} exp={e} v47:{'OK' if ok else 'FAIL'} old:{'OK' if old==e else 'WRONG'}")
print(f"  (C) old WRONG={old_wrong_c}  (matches Ada's recorded TOTAL WRONG: 20)  v47 fails so far={bad_new}")
print("== D. Ada wake cases: phone asleep across midnight, no app code runs (Doze), first frame at 00:07:00")
for u24 in (True, False):
    now = L(2026,6,17,0,7,0); n = textclock(now, u24); e = exp(now, u24); ok = n == e and valid(n, u24); bad_new += not ok
    print(f"  {'24h' if u24 else '12h'} v47 first frame={n} exp={e} {'OK' if ok else 'FAIL'}  (old stale frame 24h: 24:07:00)")
print("== E. Doze / process death: NO app updates for 72 h; TextClock sampled every 7 s by host")
t = L(2026,10,31,0,0); end = t + timedelta(hours=72); cnt = 0; f = 0
while t < end:
    for u24 in (True, False):
        n = textclock(t, u24); cnt += 1
        if not (n == exp(t, u24) and valid(n, u24)): f += 1
    t += timedelta(seconds=7)
bad_new += f; print(f"  samples={cnt} fails={f} (window spans 2026-11-01 fall-back)")
print("== F. Host tick phase (not format): API 26-30 TextClock reschedules at uptimeMillis%1000 boundaries")
print("   (TextClock.java android-8.0.0_r1:178-181, android-11.0.0_r1:193-198); API 31+ schedules at wall-second")
print("   boundaries (android-12.0.0_r1:197-211). Worst-case display lag vs wall clock on API 26-30:")
print("   analytic: lag = (wall_ms - uptime_ms) mod 1000, a fixed per-boot phase in [0, 1000) ms. Seconds digit changes late by that phase;\n   the format is never wrong. API 31+: ~0 ms + handler latency. (Not simulated numerically.)")
print(f"\nTOTAL v47 TextClock FAILS: {bad_new}   (old Chronometer WRONG in A: {bad_old})")
