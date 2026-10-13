import sys, re, hashlib
from androguard.core.apk import APK
from androguard.core.axml import AXMLPrinter
from loguru import logger; logger.remove()
p=sys.argv[1] if len(sys.argv)>1 else "/workspace/agent-backups/Helios/v47-local-review/app-debug.apk"
a=APK(p)
print("sha256", hashlib.sha256(open(p,'rb').read()).hexdigest())
print("package", a.get_package(), "versionCode", a.get_androidversion_code(), "versionName", a.get_androidversion_name())
print("minSdk", a.get_min_sdk_version(), "targetSdk", a.get_target_sdk_version())
man=a.get_android_manifest_axml().get_xml().decode()
print("debuggable_attr", re.findall(r'debuggable="[^"]*"', man))
print("permissions", sorted(a.get_permissions()))
for tag in ("receiver","service"):
    for m in re.finditer(r'<%s[^>]*>'%tag, man): print(" ", m.group(0)[:220])
print("signature certs:", [c.subject.human_friendly for c in a.get_certificates()])
lay="res/layout/widget_layout.xml"
x=AXMLPrinter(a.get_file(lay)).get_xml().decode()
open("widget_layout.decoded.xml","w").write(x)
tags=re.findall(r'<([A-Za-z0-9_.]+)[\s>]', x)
from collections import Counter
print("widget_layout tags:", dict(Counter(tags)))
tcs=re.findall(r'<TextClock[^>]*>', x)
print("TextClock count", len(tcs))
for t in tcs: print("  ", re.sub(r'\s+',' ',t)[:400])
custom=[t for t in set(tags) if '.' in t]
print("custom (dotted) view classes in widget_layout:", custom)
print("format12Hour/24Hour attrs in layout:", re.findall(r'format(?:12|24)Hour="[^"]*"', x))
names=a.get_files()
print("layout files:", [n for n in names if n.startswith("res/layout")])
# Dex check: ClockFormat and PaddedHoursChronometer, AgentDebugLog strings
from androguard.core.dex import DEX
cls=set(); strs=set()
for d in a.get_all_dex():
    dx=DEX(d)
    for c in dx.get_classes(): cls.add(c.get_name())
    for s in dx.get_strings(): strs.add(str(s))
for k in ["ClockFormat","PaddedHours","AgentDebugLog","ClockAnchor"]:
    print(k, sorted(c for c in cls if k in c))
for s in ["HH:mm:ss","hh:mm:ss","h:mm:ss","setFormat12Hour","setFormat24Hour","127.0.0.1:7685","DEBUG_E25390","ACTION_DAY_ROLL","com.claudewidgets.alarmweather.ACTION_DAY_ROLL","e25390","/ingest/"]:
    print(f"string {s!r}:", any(s in x for x in strs))
