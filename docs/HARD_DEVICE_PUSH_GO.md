# HARD: device push requires Chris verbal GO

Effective 2026-09-23 CT (Helios voice Soft-PASS).

**NEVER** push an APK or patch to Chris's Motorola (or any device) without:
1. Announcing the push out loud (what tip / version / what it contains)
2. Waiting for his explicit **go** or **okay**

No silent pushes. No automatic pushes. Ready build = announce + HOLD until approval.

Why: fast adb pushes can blank the LRT HUD during live-fetch / paint (tip136/137 race).

Owners: Helios (fleet) + Ada (LRT Git / Studio / adb). Soft-FAIL Play still separate.