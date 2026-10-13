# LRT 1.5.6.2 review list
Source: Helios, 2026-09-29 ~08:14 CT, from Chris.
Status: HOLD until Ada sends a finished build.
Loop: find ALL issues before she rebuilds. Do not drip. Flag the full set, work out fixes with Ada, she rebuilds, repeat until clean.
After clean pass: Da Vinci glances, then Helios, then Chris pushes Studio as 1.5.6.2.
No Studio push from tinkabot. No Play.

1. Approved Falcon Heavy stack md5 61fc77c120a7b91e4797691759797ec5. Bells and nose untouched. Old cartoon md5 2647e9cdc46477c42953e1275b2eb9c0 stays out.
2. Chinese, Russian, Japanese, Hindi, French, English beside each.
3. Tape pauses on leave and starts on return. Commit 351e364 must not be the behavior.
4. Catalog entry owns the language and it flows into buttons, art, and text.
5. Jokes localized per country, including the unknown-flight joke.
6. Extra panel if the chip row overflows past eight.
7. Chinese line is on the Chinese plates.
8. Plate text is restored. English stays beside languages Chris cannot read. Country language is there for the others. FakeBold stays off.
9. Tape center-lock, past left/bottom, upcoming right/bottom and shrunk, text grows and pops to the top, nothing hits center until it happens. Not pixel-based.
10. All of the above scale with the screen. No fixed pixels.

Also still in force from earlier this morning:
- Japanese throughout for H3. Hindi for ISRO (PSLV, GSLV, LVM3). Russian for Roscosmos. Chinese for the Chinese companies. French for ESA.
- No H-2A row in the catalog.
- Catalog language picks the art type. Inserted text matches that language. English stays beside it.
- Not a skin-only overlay.
- Do not pass a version bump or a change that breaks the tape.
