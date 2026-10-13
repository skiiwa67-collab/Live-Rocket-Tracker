# tinkabot independent sim: Chronometer = fmt % DateUtils.formatElapsedTime((now-base)//1000)
# Models REAL elapsed (UTC instants) vs wall fields, America/Chicago DST.
from datetime import datetime, timedelta, timezone
from zoneinfo import ZoneInfo
TZ = ZoneInfo("America/Chicago")
def fe(s):
    neg = s < 0; s = abs(s)
    h, m, x = s//3600, (s%3600)//60, s%60
    t = f"{h}:{m:02d}:{x:02d}" if h > 0 else f"{m:02d}:{x:02d}"
    return ("\u2212" if neg else "") + t
def prefix(eh): return "00:%s" if eh == 0 else ("0%s" if eh < 10 else "%s")
def base_dd05a67(now_utc, u24):
    # AlarmWidgetProvider.kt:681-697 @a977c6a: millisSinceBoundary = now.timeInMillis - boundary.timeInMillis (REAL ms)
    l = now_utc.astimezone(TZ); bh = 0 if (u24 or l.hour < 12) else 12
    b = l.replace(hour=bh, minute=0, second=0, microsecond=0)
    ms = (now_utc - b.astimezone(timezone.utc)).total_seconds()
    shift = 12*3600 if (not u24 and l.hour % 12 == 0) else 0
    return ms + shift, (12 if shift else (l.hour - bh))   # elapsed-at-anchor, eh
def base_wallfields(now_utc, u24):
    # proposed: seconds-of-day from Calendar fields (DST-proof at anchor time)
    l = now_utc.astimezone(TZ); sod = l.hour*3600 + l.minute*60 + l.second
    if u24: return sod, l.hour
    h12 = l.hour % 12
    return (h12 or 12)*3600 + l.minute*60 + l.second, (h12 or 12)
def show(anchor_utc, now_utc, u24, fn):
    e0, eh = fn(anchor_utc, u24)
    el = int(e0 + (now_utc - anchor_utc).total_seconds())   # elapsedRealtime advances in REAL seconds
    return prefix(eh).replace("%s", fe(el))
def exp(now_utc, u24):
    l = now_utc.astimezone(TZ); h = l.hour if u24 else (l.hour % 12 or 12)
    return f"{h:02d}:{l.minute:02d}:{l.second:02d}"
def L(y,mo,d,h,mi,s=0,fold=0): return datetime(y,mo,d,h,mi,s,tzinfo=TZ,fold=fold).astimezone(timezone.utc)
bad = 0
print("== DST spring-forward 2026-03-08 (02:00->03:00 CT); anchor AT now (fresh re-anchor)")
for u24 in (True, False):
    for fn in (base_dd05a67, base_wallfields):
        n = L(2026,3,8,3,30); s = show(n, n, u24, fn); e = exp(n, u24); ok = s == e; bad += (not ok and fn is base_wallfields)
        print(f"  {'24h' if u24 else '12h'} {fn.__name__:16s} 03:30 CDT shows {s:>10s} expected {e} {'OK' if ok else 'WRONG'}")
print("== DST fall-back 2026-11-01 (02:00 CDT->01:00 CST); fresh anchor at 01:30 CST (2nd pass) and 09:00")
for u24 in (True, False):
    for fn in (base_dd05a67, base_wallfields):
        for n in (L(2026,11,1,1,30,fold=1), L(2026,11,1,9,0)):
            s = show(n, n, u24, fn); e = exp(n, u24); ok = s == e; bad += (not ok and fn is base_wallfields)
            print(f"  {'24h' if u24 else '12h'} {fn.__name__:16s} {n.astimezone(TZ).strftime('%H:%M %Z')} shows {s:>10s} expected {e} {'OK' if ok else 'WRONG'}")
print("== DST spring-forward, anchored 01:59:00 CST, NO re-anchor, view at 03:00:30 CDT (wall-field base)")
a = L(2026,3,8,1,59); n = L(2026,3,8,3,0,30)
print(f"  24h shows {show(a,n,True,base_wallfields)} expected {exp(n,True)}  (stale by 1h until next re-anchor)")
print("== Reboot: last RemoteViews base from old uptime; elapsedRealtime restarts near 0")
old_uptime = 5*86400; anchor_elapsed = old_uptime; base_er = anchor_elapsed - (23*3600)  # anchored at 23:00 with uptime 5d
new_er = 40  # 40 s after boot
print(f"  launcher re-inflates cached Chronometer: shows {fe(new_er - base_er)} (negative) until BOOT_COMPLETED re-render")
print(f"TOTAL WRONG for wall-field base with fresh anchor: {bad}")
