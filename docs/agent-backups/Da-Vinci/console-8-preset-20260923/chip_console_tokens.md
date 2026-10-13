# Console 8-preset art tokens — Soft-PASS compact for Ada

**Pack:** `/workspace/agent-backups/Da-Vinci/console-8-preset-20260923/`
**Date:** 2026-09-23 (CT)
**Agent:** Da Vinci (art executor)
**Scope:** Soft-PASS art chips + hex tokens only. Soft-FAIL wire prefs / tip142 / Play.

Sources: `TelemetrySkin.kt` agency Tokens + MCC `chip_console_selected_mcc.xml`.
ULA: Soft-PASS proposed from published brand language (no Tokens yet).

## Soft-PASS size (compact masters)

- **Art master size:** `200×56` px (Soft-FAIL fat `320×120` for flyout).
- **Short labels (phone Soft-PASS read):** MCC · ROS · SPACEX · NASA · CNSA · ARIANE · R.LAB · ULA
- Soft-FAIL long “MISSION CONTROL” / full Cyrillic crowding on compact Soft-PASS.
- Soft-FAIL ship fixed 320px bitmaps as the sole Soft-PASS size.

## Soft-PASS Ada direction (dynamic scale)

**Critical:** Soft-PASS XML **shape drawables in dp** (not fixed-px bitmaps as sole Soft-PASS size).
Art PNGs here are Soft-PASS eyes-only masters / preview. Runtime Soft-PASS = shape + text Soft-PASS.

Match live rockers (`activity_main.xml` `row_console_skin`): 0dp width + `layout_weight=1`.

Suggest **2 LinearLayouts** (row1 / row2), Soft-PASS each with **4 buttons**:

```xml
<!-- Soft-PASS Ada sketch — Soft-FAIL wire until Helios + Chris ❤️ -->
<LinearLayout
    android:orientation="vertical"
    android:layout_width="match_parent"
    android:layout_height="wrap_content">

  <LinearLayout  <!-- row1: MCC · ROS · SPACEX · NASA -->
      android:orientation="horizontal"
      android:layout_width="match_parent"
      android:layout_height="wrap_content"
      android:layout_marginBottom="6dp">
    <Button
        android:layout_width="0dp"
        android:layout_height="40dp"   <!-- Soft-PASS 40–48dp -->
        android:layout_weight="1"
        android:layout_margin="2dp"
        android:textSize="11sp"        <!-- Soft-PASS ~11–12sp -->
        android:text="MCC"
        android:background="@drawable/chip_console_selected_mcc"/>
    <!-- ×3 more Soft-PASS weight=1 buttons -->
  </LinearLayout>

  <LinearLayout  <!-- row2: CNSA · ARIANE · R.LAB · ULA -->
      android:orientation="horizontal"
      android:layout_width="match_parent"
      android:layout_height="wrap_content">
    <!-- 4 Soft-PASS buttons: 0dp + weight 1 + height 40–48dp + textSize ~11–12sp -->
  </LinearLayout>
</LinearLayout>
```

- Soft-PASS height: **40–48dp** (matches live console skin rocker height class).
- Soft-PASS textSize: **~11–12sp** short company labels Soft-PASS readable on narrow.
- Soft-PASS proof mocks: `mock_cmd_flyout_8_fit.png` + `mock_cmd_flyout_8_narrow.png`.

## Layout (row1 | row2)

| Slot | ID | Label | Company | Status |
|------|----|-------|---------|--------|
| 1 | mcc | MCC | MCC | Soft-PASS favorite amber |
| 2 | ros | ROS | Roscosmos | Soft-PASS |
| 3 | spacex | SPACEX | SpaceX | Soft-PASS retheme |
| 4 | nasa | NASA | NASA | Soft-PASS (distinct from MCC) |
| 5 | cnsa | CNSA | CNSA | Soft-PASS |
| 6 | arianespace | ARIANE | Arianespace | Soft-PASS |
| 7 | rocketlab | R.LAB | Rocket Lab | Soft-PASS **PROPOSED** (tiny badge) |
| 8 | ula | ULA | ULA | Soft-PASS **PROPOSED** (tiny badge) |

## Per-slot hex (selected)

### 1. `mcc` — MCC

- **company:** MCC
- **fill:** `#FFB040`
- **stroke:** `#FFD080`
- **rim / dark:** `#3A2800`
- **text:** `#1A1200`
- **text_sub / accent secondary:** `#3A2800`
- **lamp:** `#FFF0C0`
- **lamp_ring:** `#3A2800`
- **style:** `lit_fill`
- **suggested selected drawable:** `chip_console_selected_mcc.xml`
- **suggested idle drawable:** `chip_console_idle_mcc.xml`
- **art PNG:** `chip_01_mcc.png` / `chip_01_mcc_idle.png` (200×56)

### 2. `ros` — ROS

- **company:** Roscosmos
- **fill:** `#101418`
- **stroke:** `#E8C547`
- **rim / dark:** `#080A0C`
- **text:** `#E8E0D0`
- **text_sub / accent secondary:** `#E8C547`
- **lamp:** `#C41E3A`
- **lamp_ring:** `#E8C547`
- **style:** `dark_accent`
- **suggested selected drawable:** `chip_console_selected_ros.xml`
- **suggested idle drawable:** `chip_console_idle_ros.xml`
- **art PNG:** `chip_02_ros.png` / `chip_02_ros_idle.png` (200×56)

### 3. `spacex` — SPACEX

- **company:** SpaceX
- **fill:** `#0C0C0C`
- **stroke:** `#FFFFFF`
- **rim / dark:** `#050505`
- **text:** `#F0F0F0`
- **text_sub / accent secondary:** `#00D26A`
- **lamp:** `#00D26A`
- **lamp_ring:** `#FFFFFF`
- **style:** `dark_accent`
- **suggested selected drawable:** `chip_console_selected_spacex.xml`
- **suggested idle drawable:** `chip_console_idle_spacex.xml`
- **art PNG:** `chip_03_spacex.png` / `chip_03_spacex_idle.png` (200×56)
- **accent_dim (SpaceX stripe):** `#005288`

### 4. `nasa` — NASA

- **company:** NASA
- **fill:** `#1A2840`
- **stroke:** `#6A9AD4`
- **rim / dark:** `#0A0E18`
- **text:** `#E8EEF8`
- **text_sub / accent secondary:** `#C8102E`
- **lamp:** `#C8102E`
- **lamp_ring:** `#0032A0`
- **style:** `dark_accent`
- **suggested selected drawable:** `chip_console_selected_nasa.xml`
- **suggested idle drawable:** `chip_console_idle_nasa.xml`
- **art PNG:** `chip_04_nasa.png` / `chip_04_nasa_idle.png` (200×56)
- **accent (NASA blue bar):** `#0032A0`

### 5. `cnsa` — CNSA

- **company:** CNSA
- **fill:** `#2A1010`
- **stroke:** `#DE2910`
- **rim / dark:** `#0C0808`
- **text:** `#F5E6C8`
- **text_sub / accent secondary:** `#FFD700`
- **lamp:** `#FFD700`
- **lamp_ring:** `#DE2910`
- **style:** `dark_accent`
- **suggested selected drawable:** `chip_console_selected_cnsa.xml`
- **suggested idle drawable:** `chip_console_idle_cnsa.xml`
- **art PNG:** `chip_05_cnsa.png` / `chip_05_cnsa_idle.png` (200×56)

### 6. `arianespace` — ARIANE

- **company:** Arianespace
- **fill:** `#183060`
- **stroke:** `#FFD100`
- **rim / dark:** `#070B18`
- **text:** `#FFFFFF`
- **text_sub / accent secondary:** `#FFD100`
- **lamp:** `#FFD100`
- **lamp_ring:** `#003399`
- **style:** `dark_accent`
- **suggested selected drawable:** `chip_console_selected_arianespace.xml`
- **suggested idle drawable:** `chip_console_idle_arianespace.xml`
- **art PNG:** `chip_06_arianespace.png` / `chip_06_arianespace_idle.png` (200×56)
- **stroke_secondary:** `#003399`

### 7. `rocketlab` — R.LAB

- **company:** Rocket Lab
- **fill:** `#1C1C1C`
- **stroke:** `#FF5A1F`
- **rim / dark:** `#080808`
- **text:** `#F2F2F2`
- **text_sub / accent secondary:** `#FF5A1F`
- **lamp:** `#FF5A1F`
- **lamp_ring:** `#FF5A1F`
- **style:** `dark_accent`
- **suggested selected drawable:** `chip_console_selected_rocketlab.xml`
- **suggested idle drawable:** `chip_console_idle_rocketlab.xml`
- **art PNG:** `chip_07_rocketlab.png` / `chip_07_rocketlab_idle.png` (200×56)
- **badge:** tiny `PROPOSED` Soft-PASS

### 8. `ula` — ULA

- **company:** ULA
- **fill:** `#0B1F44`
- **stroke:** `#F6A800`
- **rim / dark:** `#061028`
- **text:** `#FFFFFF`
- **text_sub / accent secondary:** `#F6A800`
- **lamp:** `#F6A800`
- **lamp_ring:** `#F6A800`
- **style:** `dark_accent`
- **suggested selected drawable:** `chip_console_selected_ula.xml`
- **suggested idle drawable:** `chip_console_idle_ula.xml`
- **art PNG:** `chip_08_ula.png` / `chip_08_ula_idle.png` (200×56)
- **note:** No TelemetrySkin Tokens yet — Soft-PASS proposed from published brand language only
- **badge:** tiny `PROPOSED` Soft-PASS

## Idle guidance (all slots)

- Dim fill ~45–70% of selected fill (MCC idle uses olive-dark `#2A2010`, not lit amber).
- Stroke muted ~35% of selected stroke brightness.
- Lamp = empty ring / `#222222` core (off).
- Text muted ~45% of selected text.

## XML shape direction (Ada — Soft-FAIL wire)

Mirror existing Stamp 69 layer-list pattern — Soft-PASS **dp**, Soft-FAIL sole fixed-px bitmap ship:

```xml
<!-- example: chip_console_selected_<id>.xml -->
<layer-list>
  <item>  <!-- rim + stroke -->
    <shape android:shape="rectangle">
      <solid android:color="{rim}"/>
      <stroke android:width="2dp" android:color="{stroke}"/>
      <corners android:radius="6dp"/>
    </shape>
  </item>
  <item android:left="3dp" android:top="3dp" android:right="3dp" android:bottom="3dp">
    <shape android:shape="rectangle">
      <solid android:color="{fill}"/>
      <corners android:radius="4dp"/>
    </shape>
  </item>
</layer-list>
```

MCC keeps lit-fill amber language (`#FFB040` / `#FFD080` / `#3A2800`) — Soft-FAIL absorb into NASA.
NASA stays blue/meatball language (`#0032A0` / `#C8102E` / `#0A0E18`).

## Soft-FAIL reminders

- Soft-FAIL invent vehicle stacks / rocket hulls in these boxes.
- Soft-FAIL tip142 ship / Play / device — Soft-FAIL ❤️ until Chris Soft-PASS.
- Soft-FAIL overwrite Live-Rocket-Tracker drawable trees from this pack.
- Soft-PASS Studio-only after Helios Soft-PASS + Chris ❤️.
- Soft-FAIL ship fixed 320px bitmaps as sole Soft-PASS size — Soft-PASS dp shape + weight.
- Auto live-provider Soft-PASS remains Ada (`TelemetrySkin.forLaunch`); these are settings/CMD override chips.
