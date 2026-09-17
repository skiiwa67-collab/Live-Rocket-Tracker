# LRT tip 132 — Play PRODUCTION paste pack (F13 MAP icon fix)
**Chris: copy-paste only. Do NOT merge GitHub PRs yourself — Ada owns git.**

Version: **132 / 1.0.122** (tip `7fcb12d` on `stamp-85`)
App name: **Live Rocket Tracker** (unchanged)

---

## YOUR LOOP (only)
1. Android Studio → **Generate Signed Bundle / APK** → release **AAB** (use your Play upload keystore)
2. Play Console → upload that AAB
3. Play Console → Main store listing → App icon = file below
4. Paste short / full / release notes from this pack
5. Upload screenshots / feature graphic in the order below
6. Resubmit for review

---

## APP ICON (must match launcher — Play rejection fix)
**Upload this exact file as Store listing → App icon (512×512 PNG):**

```
D:\Downloads\play-app-icon-512-F13-MAP.png
```

Also in tree:
- `PlayListing\play-app-icon-512-F13-MAP.png`
- `docs\play-store-media\icon\play-app-icon-512-F13-MAP.png`

SHA256:
```
2EF08978110BF20A4C11B9D1A3B3263462F22D3AEB89706A09E86274CDEC472B
```

Same F13 ground-track MAP art is now the **on-device launcher** (`@mipmap/ic_launcher`). Do **not** upload the old teal rocket vector or A-MAP Earth+arc packs.

---

## Release name (internal, 50 max)
```
Starship 14 is a GO
```

---

## Short description (80 max)
```
Live rocket tracker wallpaper - real countdowns, fleet art, webcast + telemetry.
```

---

## Full description (paste)
```
Live Rocket Tracker turns your phone into mission control.

AUTO follows the next real launch with published countdown times - not stale guesses. Watch Starship, Falcon, Soyuz, Long March, Electron, and more with paper-doll vehicle art that matches the bird you're tracking.

Set it as live wallpaper for a always-on HUD, or open Command Center for deeper telemetry, historic flights, and agency skins. VID keeps a webcast/PiP alive while you watch the clock. Other video options help when YouTube isn't available in your region - the YouTube path you already love stays the same.

Built for people who sleep through a launch and still want the real story afterward.
```

---

## What's new / en-US release notes
```
<en-US>
Store icon and launcher now match (Starship F13 ground-track map). Live launch times hardened. Fleet art (Long March 12, Kinetica). VID: Other video options when YouTube isn't available - YouTube path unchanged.
</en-US>
```

---

## Appeal / review reply (if Console asks about icon)
```
Launcher and high-res store icon are now the same Starship Flight 13 ground-track map artwork. The previous listing used a different map graphic while the APK used a teal rocket vector. Version 1.0.122 (versionCode 132) ships matching mipmap launcher assets; the 512 PNG uploaded to the listing is that same F13 map.
```

---

## Feature graphic (1024×500)
Prefer:
```
docs\play-store-media\beauty\01-starship\f14-starship-feature-1024x500.png
```
Fallback:
```
PlayListing\feature-graphic-1024x500.jpg
```

---

## Phone screenshots — upload order (pick 4–8)
**Path base:** `docs\play-store-media\beauty\`

1. `01-starship\f14-starship-phone-hero-1080x1920-a.png` — Starship F14 hero
2. `01-hud-ros-soyuz-star-pip.png` — wallpaper HUD + PiP
3. `03-mcc-tel-analog-gauges-pip.png` — Command Center
4. `05-vid-youtube-overlay-miss.png` — VID / YouTube
5. `01-starship\starship-silver-cutaway-chris-pick-stamp74.png` — cutaway art
6. `04-china\hud-cz8a-stamp73-sync-look.png` — China / near-term (optional)
7. `08-geo-open-with-chooser.png` — world / open-with (optional)

Skip anything named FAIL.

**Alt phone set (PlayListing\phone\)** if you prefer the F13 beauty JPGs:
1. `phone\01-liftoff.jpg`
2. `phone\05-plasma-map.jpg`
3. `phone\04-digital-track.jpg`
4. `phone\03-stage-sep.jpg`
5. `phone\02-booster-entry.jpg`
6. `phone\06-ship-reentry.jpg` (Soyuz — all-countries proof, not lead)
7. `phone\07-landspace.jpg` (China — all-countries proof, not lead)

---

## Promo video
```
https://www.youtube.com/watch?v=sW30xia3gnw
```

---

## Debug soak APK (optional before signed AAB)
```
D:\Downloads\LiveRocketTracker-vc132-debug.apk
```

## Signed AAB
You generate in Studio (Ada does not have your Play upload keystore). After signed AAB is built, upload it + the F13 MAP 512 icon + paste pack above, then resubmit.
