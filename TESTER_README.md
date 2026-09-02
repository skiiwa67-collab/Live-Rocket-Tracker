# Live Rocket Tracker — closed tester notes (stamp 20)

Sideload this APK. This is **not** a Play production upload. Play testers on versionCode 17 stay on 17. Play 17 stays frozen.

Chris is on a Razr. PrimeTestLab used a Galaxy Note 10+. Any phone or tablet is fine.

**Do not test Alarm.** Alarm is a separate product.

## VID / picture-in-picture (heavy test)

This is what testers hunted.

1. Pick a historic Electron (Command Center / historic list). Owl Around The World is the stamp-20 recapture case.
2. Open Command Center → tap VID.
3. Two panes: official (Rocket Lab) and NASASpaceflight.
4. Historic Electron used to 404 on YouTube channel `/search`. Stamp 20 must **load** a results page or a real watch URL. YouTube 404 is a fail.

**PiP / webcast panes require a Google / YouTube account signed in ON THAT DEVICE** (the phone or tablet you are testing). Sign in inside the VID pane (`SignIN`). Chrome login does not carry over to the app WebView.

Also test a **live** upcoming webcast the same way (VID on a flight that has a watch URL).

## Also test

- **One countdown, with units.** A long T- must read `33h` or `1d 9h`, never a naked `T-02:04` / `T-33:45:01` that could be minutes or hours. Top glance and CDT must not disagree.
- **Live T.** If Launch Library 2 says they already lifted, the HUD shows `T+` or `HOLDING`. A leftover T-minus after liftoff is a fail. Do not invent a T0.
- **AUTO double-tap** pins the flight. Lock light on AUTO when pinned. Double-tap again unpins. Single tap AUTO still browses. `+` / `-` walk events on the pinned flight. Launch pick is still CMD / MCC. There is no LCK plate.
- **Agency / space-org display icon** stays visible. Mission text uses empty space (under the plates / below the icon) and **wraps the full name** (not “Owl Aroun”). It must not sit on the icon. Do not move home-screen widgets.
- **Eight plates stay visible:** CMD CDT TEL STS PAD VID MSK AUTO. They are the buttons. A giant CDT slab must not sit on top of them.
- **TEL event tape:** the selected launch (not an 80s now-window). Acronyms above and below (`L/O`, `MECO`, `SEP`, `SECO`, `FAIR`, `DEPLOY`). Past marks stay after they fly; `+`/`-` walks them. Upcoming marks get fully readable ~2 minutes out. Far-future marks stay compact. After last payload deploy the tape does not go blank.
- **Dock gap:** nothing draws under Phone / Messages / app drawer / system nav.

## Hard locks

Eight HUD plates stay: **CMD CDT TEL STS PAD VID MSK AUTO**.
Unknown vehicle = **NEW VEHICLE** outline. We do not invent drawings or numbers.
Do not draw under the Android dock.
Analog TEL ENG, TRAJ ground track, and STG2 flap glow stay.
