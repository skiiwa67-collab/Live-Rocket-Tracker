# Live Rocket Tracker — closed tester notes (stamp 25)

Sideload this APK. This is **not** a Play production upload. Play testers on versionCode 17 stay on 17. Play 17 stays frozen.

Stamp **20** (`stamp-20-debug`) stays the KEEP for units + AUTO pin.
Stamp **21** (`stamp-21-debug`) is the GISAT look + Electron HUD locks.
Stamp **23** (`stamp-23-debug`) is SIGNED usable WITH the Pixel bar. Do not overwrite it.

This stamp is **25**. Pixel glues the Google search bar to the **first** home page. You cannot drag it off on stock. The HUD lives on the **extra** page — that page has no Google bar. Auto measures the page it is on: search reserve only on first home, full well on the extra HUD page. Do not assume Auto always clears dock+search.

Drag the bar if the launcher lets you. Nova can hide it. Nova is optional, not the default. Pixel emu stays stock.

CMD double-tap → Settings → HOME SCREENS → **BOTTOM GAP**:
- **AUTO** (default) — this page. Search row on page 0 only. Extra HUD page takes the well.
- **DOCK** — dock + nav override.
- **DOCK+SEARCH** — force search + dock + nav (page-0 look).

TEL tape: letters get bigger as the event slides toward center. Far events stay small. Readable when it matters.

21 GISAT look stays. No AUDIOFOCUS_GAIN. Overlay PiP stays signed. Eight plates are not moved.

Chris is on a Razr. PrimeTestLab used a Galaxy Note 10+. Any phone or tablet is fine.

**Do not test Alarm.** Alarm is a separate product.

## VID / picture-in-picture (heavy test)

This is what testers hunted.

1. Pick a historic Electron (Command Center / historic list). Owl Around The World is the stamp-20 recapture case.
2. Open Command Center → tap VID **once**. One screen only (official). Sign in there (`SignIN`). Do not expect two panes on first click.
3. Tap VID a second time for NASASpaceflight. Third tap closes.
4. Close MCC and open VID again. The Google / YouTube session must still be there. Getting kicked out is a fail.
5. Historic Electron used to 404 on YouTube channel `/search`. Must **load** a results page or a real watch URL. YouTube 404 is a fail.

**Wallpaper PiP is unchanged.** HUD first, webcast ~60–90s later. Do not regress live-wallpaper PiP.

**MCC panes require a Google / YouTube account signed in ON THAT DEVICE.** Sign in inside the VID pane (`SignIN`). Chrome login does not carry over to the app WebView.

Also test a **live** upcoming webcast the same way (VID on a flight that has a watch URL).

## Also test

- **Clock is king.** Actual time stays at the top and is bigger than T-/T+. Countdown is secondary and still has units (`1d 8h` / `T+52m`), never a naked H:MM:SS that could be minutes.
- **One countdown, with units.** A long T- must read `33h` or `1d 9h`, never a naked `T-02:04` / `T-33:45:01`. Top glance and CDT must not disagree.
- **Live T.** If Launch Library 2 says they already lifted, the HUD shows `T+` or `HOLDING`. A leftover T-minus after liftoff is a fail. Do not invent a T0.
- **AUTO double-tap** pins the flight. Lock light on AUTO when pinned. Double-tap again unpins. Single tap AUTO still browses. `+` / `-` walk events on the pinned flight. Launch pick is still CMD / MCC. There is no LCK plate.
- **AUTO showing Electron T+ is not a bug** when Owl (or another live flight) lifted and AUTO ON followed it. GISAT look (Hindi, GSLV Mk II stack, map pad) is for the pinned/selected ISRO launch. Do not force GISAT over live AUTO.
- **Agency / space-org display icon** stays visible. Mission text uses empty space (under the plates / below the icon) and **wraps the full name** (not “Owl Aroun”). It must not sit on the icon. Do not move home-screen widgets.
- **Eight plates stay visible:** CMD CDT TEL STS PAD VID MSK AUTO. They are the buttons. A giant CDT slab must not sit on top of them.
- **STACK silhouettes** stay inside the plate. No overflow.
- **TEL event tape:** the selected launch (not an 80s now-window). Acronyms above and below (`L/O`, `MECO`, `SEP`, `SECO`, `FAIR`, `DEPLOY`), readable, not a smashed `L/OMECO` pile. Past marks stay after they fly; `+`/`-` walks them. After last payload deploy the tape does not go blank.
- **HUD TEXT SIZE** default is MD (not SM) so the clock can be king. Clock and T- glance stay unit-labeled (`23m 25s` / `1d 9h`).
- **GISAT / ISRO (when that launch is selected):** Hindi dual-label on the eight plates (acronyms stay CMD CDT TEL STS PAD VID MSK AUTO). Agency **इसरो**. Map shows published pad name + real lat/lon and short `GSLV Mk II` (not the GISAT-1A mission string). Stack is solid S139 core + 4 L40 Vikas liquid strap-ons + GS2 + CUS + ogive. Solid core is not a liquid bell. No DATA UPDATE REQUIRED on that stack.
- **Bottom gap:** Auto is THIS page. Pixel glues Google search to first home — Auto reserves search+dock+nav there. Extra HUD page has no bar; Auto takes the well (dock+nav). Dock / Dock+search stay as overrides. Drag the bar if the launcher lets you. Nova can hide it. Packer uses WindowInsetsCompat plus the HOME launcher's own hotseat/QSB dimens and the published search-widget minHeight. Not nav-only. Not a 48dp guess. Eight plates are not moved.
- **Wallpaper audio:** launch cues must not pause YouTube / overlay PiP. No AUDIOFOCUS_GAIN. Live feed never pauses. If cues cannot mix, they stay quiet.

## Hard locks

Eight HUD plates stay: **CMD CDT TEL STS PAD VID MSK AUTO**.
Unknown vehicle = **NEW VEHICLE** outline. We do not invent drawings or numbers.
Do not draw under the Android dock.
Analog TEL ENG, TRAJ ground track, and STG2 flap glow stay.
