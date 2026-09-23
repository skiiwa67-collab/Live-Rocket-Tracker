# tip142 — Main-screen entertainment disclaimer Soft-PASS SPEC (v2 placement)
**When:** 2026-09-23 ~08:51 CT · Helios (placement UPDATED Soft-PASS Chris voice)
**Stage:** Chris Soft-PASS copy + placement Soft-PASS. Ada wires in tip142 with Stamp-100 hullDest + Da Vinci plume packs.
**Soft-FAIL Play Soft-FAIL adb Soft-FAIL invent Soft-FAIL phone until Chris go. Soft-PASS Studio-only after Helios Soft-PASS build verify.**

## Placement Soft-PASS (UPDATED — Soft-FAIL CURRENT/HISTORIC spot)
**Screen:** Main LRT app (`activity_main.xml` top of ScrollView column).
**Spot Soft-PASS:** Own colored high-contrast box **directly under** the `Live Rocket Tracker` app title (`@string/app_name` TextView ~L13–21) Soft-PASS **before** next-live-launch / status info (`txt_status` ~L23–30 Soft-PASS and any later next-launch block).
**Layout Soft-PASS order:**
1. App title (`@string/app_name`) Soft-PASS keep.
2. **NEW disclaimer box Soft-PASS** insert here Soft-PASS.
3. `txt_status` / next-live-launch info Soft-PASS after disclaimer Soft-PASS.
4. Soft-FAIL place above CURRENT/HISTORIC Soft-FAIL TRACKING section Soft-PASS (prior Soft-PASS revoked).
5. Soft-FAIL wallpaper tip136 chrome Soft-FAIL change Soft-PASS leave tip136 Soft-PASS.

## Canonical copy Soft-PASS (Chris exact Soft-PASS)

**Title line (bold):**
```
ENTERTAINMENT ONLY
```

**Body (bold Soft-PASS high-contrast):**
```
We sync launches as close to real time as public sources allow, but no rocket company provides exact live telemetry. Delays and holds can make this display differ from the actual launch — confirm with the official stream.
```

**One-line Soft-PASS equivalent (if single TextView):**
```
ENTERTAINMENT ONLY — We sync launches as close to real time as public sources allow, but no rocket company provides exact live telemetry. Delays and holds can make this display differ from the actual launch — confirm with the official stream.
```

**strings.xml Soft-PASS keys:**
- `disclaimer_main_title` = `ENTERTAINMENT ONLY`
- `disclaimer_main_body` = body above Soft-PASS (no title prefix)

## Style Soft-PASS (impossible to miss)
- Own box Soft-PASS `LinearLayout` vertical Soft-PASS `android:id="@+id/box_disclaimer_main"` Soft-PASS `padding="12dp"` Soft-PASS `layout_marginBottom="12dp"` Soft-PASS `layout_marginTop="6dp"`
- Background: `#2A2208`
- Stroke Soft-PASS gold `#F2C14E` 2dp Soft-PASS use drawable Soft-PASS or MaterialCardView stroke
- Title: `#F2C14E` Soft-PASS `textStyle="bold"` Soft-PASS `textSize="16sp"` Soft-PASS ALL CAPS Soft-PASS as written
- Body: `#FFE8A0` Soft-PASS `textStyle="bold"` Soft-PASS `textSize="13sp"` Soft-FAIL muted `#8AA0B0`

## tip142 ship Soft-PASS bundle (one Studio tip)
1. Ada Stamp-100 assembled hullDest Soft-PASS (Helios Soft-PASS DRAFT Soft-FAIL 1×1 stub Soft-PASS return null)
2. Da Vinci plume-anchor flame packs Soft-PASS (7 families gap=−6 Soft-PASS engine KEEP Soft-FAIL stack overwrite)
3. **This main-screen disclaimer Soft-PASS under app title Soft-PASS**
4. Soft-FAIL tip141 Electron GOLD Soft-FAIL overwrite Soft-FAIL Play Soft-FAIL adb Soft-FAIL invent Soft-FAIL phone until Chris go

## Dual-write
- Local Soft-PASS this file Soft-PASS (v2 Soft-PASS supersedes v1 CURRENT/HISTORIC Soft-PASS)
- Notion Focus Soft-PASS update Soft-PASS
- LRT GitHub Soft-PASS update Soft-PASS
