# LRT Play Store full description — corrected draft (single video pane)
Date: 2026-10-09, about 10:10 AM CT. Written by Helios (executor). DRAFT ONLY.
Nothing was published, submitted, or staged in Play Console. Nothing was committed or pushed to any repo. Chris alone publishes.

## Status
- Language: en-US (default). Full description is 1,150 characters before the fix and 1,167 after. The limit is 4,000.
- Short description (live): "Command Center HUD on a second home screen page. Rockets from every country." (76/80). It does not mention video, so it is unchanged.
- Changed: one paragraph (paragraph 3). Nothing else changed. The live text has a comma ("live wallpaper, and") that the repo copy lacks. This draft keeps the live text exactly.

## Diff (live en-US -> corrected)
```diff
--- before-live-en-US
+++ after-corrected
@@ -2,7 +2,7 @@
 
 Watch a launch without opening anything. Countdown, vehicle, pad, analog or digital gauges, stage stacks, engine lights, the ground track, and the event tape sit right on the wallpaper. Skip through historic launches with + and -. Flip to digital when you want the big map.
 
-Tap in for the full command center: telemetry, trajectory, engines, propellant, and two webcast panes. Sign in once inside a pane, or jump to the real YouTube app if you already have Premium.
+Tap in for the full command center: telemetry, trajectory, engines, propellant, and one minimized YouTube video pane. Sign in once inside the pane, or jump to the real YouTube app if you already have Premium.
 
 Numbers you see are labeled EST. We do not pretend we have a secret live downlink. If a rocket is not in the catalog, you get an honest “data update required,” not a made-up drawing.
 
```

## Corrected full description (paste-ready; also in the .txt next to this file)
```
Set Live Rocket Tracker as your live wallpaper, and a second home screen page turns into a mission-control HUD. Choose that page number in Settings. Android only lets a live wallpaper live on one page, so keep your icons on page 1 and put the console on page 2.

Watch a launch without opening anything. Countdown, vehicle, pad, analog or digital gauges, stage stacks, engine lights, the ground track, and the event tape sit right on the wallpaper. Skip through historic launches with + and -. Flip to digital when you want the big map.

Tap in for the full command center: telemetry, trajectory, engines, propellant, and one minimized YouTube video pane. Sign in once inside the pane, or jump to the real YouTube app if you already have Premium.

Numbers you see are labeled EST. We do not pretend we have a secret live downlink. If a rocket is not in the catalog, you get an honest “data update required,” not a made-up drawing.

Built around Starship and Falcon. The same HUD also covers published vehicles from all countries, with real public facts in one catalog. Built for a phone first.

If you want your phone to feel like a console on launch day, this is it.
```

## Sources
- LIVE public listing (read-only fetch, 2026-10-09 ~10:00 AM CT): https://play.google.com/store/apps/details?id=com.liverockettracker . Page shows "Updated on Sep 29, 2026". Full description matches PlayListing/UPLOAD.txt (one comma differs). Short description = og:description above.
- Repo source of the live text: skiiwa67-collab/Live-Rocket-Tracker `PlayListing/UPLOAD.txt` lines 46-58 ("PASTE INTO PLAY — full description"). Added in commit 7e44f2d (2026-08-30, "versionCode 16 on Razr"). It has not changed since, on either main (fa14ae7) or stamp-85 (4be9291). Box copies are identical: /workspace/play-listing/UPLOAD.txt, /workspace/lrt-play-fix/UPLOAD.txt, /workspace/{Live-Rocket-Tracker,RetroCommandCenter,lrt-audit}/PlayListing/UPLOAD.txt.
- Other drafts that were NOT live (none says two panes):
  - docs/PLAY_LISTING.md (main, 7482c84/17fbf76, 2026-09-01), with the same text in Notion "LRT Play listing / money package" https://app.notion.com/p/3cea94933faa81a19fd6c2b2d7532944. It says "YouTube picture-in-picture next to the HUD".
  - docs/play-store-media/PASTE-PACK-tip132.md (stamp-85, 6b2e40d/3edf1e7, 2026-09-17). It says "VID keeps a webcast/PiP alive".
  - /workspace/lrt-play-store-media/TOMORROW-PLAY-PACK/PASTE.txt. It says "YouTube picture-in-picture next to the HUD".

## Code check (origin/stamp-85 4be9291, versionCode 157 / 1.0.147 — newest LRT code on GitHub)
- ui/CommandCenterActivity.kt:43 doc comment: "VID is one YouTube WebView filling the console well - no dual popout panes".
- CommandCenterActivity.kt:126-134: the VID button opens only `feeds.official.url` in that single WebView (openVid, 211-223). ensureVid at 240-243 has the comment "Stamp 53: always load into the existing single well".
- res/layout/activity_command_center.xml:292-309: one `vid_web` WebView inside `vid_well`.
- wallpaper/RetroCommandWallpaperService.kt:885-905 openWebcast(): opens ONE URL (official, else NSF) via ACTION_VIEW.
- Dead code: ui/VideoFeedOverlay.kt:32-44 and 87-95 still open two windows (official + NSF). Nothing references it any more, because CommandCenterActivity no longer uses it.
- "Minimized": the GitHub code does NOT clearly show a minimized pane. In the Command Center the WebView fills the console well. The live phone screenshots show a small PiP-style video box on the HUD. The word "minimized" comes from Chris's statement. Ada's newer MiniME/Studio code (tips after 157, if any) was not visible to me.

## Other places that still say two panes / two feeds
- Localized descriptions on the live listing (de, es, fr, ja, pt_BR checked) all say two panes in paragraph 3, e.g. de "zwei Webcast-Fenster", es "dos paneles", fr "deux fenêtres", ja "2つのウェブキャストパネル", pt_BR "dois painéis". I can't tell from outside Play Console whether these are Google auto-translations, which follow the en-US text, or manual translations, which must be edited one by one.
- Tablet screenshots in the repo, PlayListing/tablet-7in/01-mcc-tel-vid.jpg and 02-mcc-analog-vid.jpg, show TWO YouTube panes (SPACEX + NASASPACEFLIGHT). UPLOAD.txt lists them as the optional 7-inch tablet set. The public page only served phone shots, so I could not confirm whether they are live. Check Play Console's tablet screenshot slots.
- The 17 phone screenshots on the live listing show a single small video box or none. OK.
- UPLOAD.txt line 52 itself (the repo paste source) should be updated in the repo by Ada/Chris. Not done (read-only).
- Old Notion notes describe the two-pane history: https://app.notion.com/p/3cfa94933faa8133aa26e9fa648c1af0 and https://app.notion.com/p/3cda94933faa8113ae73fdab81da9655. They are history, not listing copy.

## Uncertain / for Chris
- "jump to the real YouTube app if you already have Premium": per the CommandCenterActivity.kt:44 comment, the in-pane YT button chrome was removed. The wallpaper VID path still opens the URL externally (ACTION_VIEW), which normally means the YouTube app. Left unchanged, as instructed. Chris may want to reword it.
- "Sign in once inside a pane" became "inside the pane". Only grammar changed.
