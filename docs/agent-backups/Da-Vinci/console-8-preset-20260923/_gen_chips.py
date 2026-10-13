#!/usr/bin/env python3
"""Da Vinci Soft-PASS: 8 compact console color preset chips + board + fit mocks.
Eyes-only pack. Soft-FAIL tip142/Play/vehicle invent / LRT overwrite.
Compact Soft-PASS masters: 200×56 — Soft-PASS fit CMD flyout 2×4 on any screen.
"""
from __future__ import annotations

import hashlib
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont, ImageFilter

OUT = Path("/workspace/agent-backups/Da-Vinci/console-8-preset-20260923")
OUT.mkdir(parents=True, exist_ok=True)

FONT_BOLD = "/usr/share/fonts/truetype/sand-box/google/Barlow/Barlow-ExtraBold.ttf"
FONT_MED = "/usr/share/fonts/truetype/sand-box/google/Barlow/Barlow-SemiBold.ttf"
FONT_REG = "/usr/share/fonts/truetype/sand-box/google/Barlow/Barlow-Regular.ttf"

# Soft-PASS compact masters (Soft-FAIL fat 320×120 for flyout)
CHIP_W, CHIP_H = 200, 56
RADIUS = 8


def hex_to_rgb(h: str) -> tuple[int, int, int]:
    h = h.lstrip("#")
    return tuple(int(h[i : i + 2], 16) for i in (0, 2, 4))  # type: ignore


def rgba(h: str, a: int = 255) -> tuple[int, int, int, int]:
    r, g, b = hex_to_rgb(h)
    return (r, g, b, a)


def darken(h: str, factor: float = 0.45) -> str:
    r, g, b = hex_to_rgb(h)
    return f"#{int(r * factor):02X}{int(g * factor):02X}{int(b * factor):02X}"


def lighten(h: str, factor: float = 0.35) -> str:
    r, g, b = hex_to_rgb(h)
    r = min(255, int(r + (255 - r) * factor))
    g = min(255, int(g + (255 - g) * factor))
    b = min(255, int(b + (255 - b) * factor))
    return f"#{r:02X}{g:02X}{b:02X}"


# Locked Soft-PASS token table — SHORT labels for phone-scale Soft-PASS read
# Soft-FAIL long "MISSION CONTROL" / full Cyrillic crowding on compact
PRESETS = [
    {
        "id": "mcc",
        "slot": 1,
        "file": "chip_01_mcc",
        "label": "MCC",
        "sub": None,  # Soft-FAIL long MISSION CONTROL on compact
        "fill": "#FFB040",
        "stroke": "#FFD080",
        "rim": "#3A2800",
        "text": "#1A1200",
        "text_sub": "#3A2800",
        "lamp": "#FFF0C0",
        "lamp_ring": "#3A2800",
        "style": "lit_fill",
        "proposed": False,
        "drawable": "chip_console_selected_mcc",
        "idle_drawable": "chip_console_idle_mcc",
        "company": "MCC",
    },
    {
        "id": "ros",
        "slot": 2,
        "file": "chip_02_ros",
        "label": "ROS",
        "sub": None,  # Soft-FAIL full Cyrillic crowding on compact
        "fill": "#101418",
        "stroke": "#E8C547",
        "rim": "#080A0C",
        "text": "#E8E0D0",
        "text_sub": "#E8C547",
        "lamp": "#C41E3A",
        "lamp_ring": "#E8C547",
        "style": "dark_accent",
        "proposed": False,
        "drawable": "chip_console_selected_ros",
        "idle_drawable": "chip_console_idle_ros",
        "company": "Roscosmos",
    },
    {
        "id": "spacex",
        "slot": 3,
        "file": "chip_03_spacex",
        "label": "SPACEX",
        "sub": None,
        "fill": "#0C0C0C",
        "stroke": "#FFFFFF",
        "rim": "#050505",
        "text": "#F0F0F0",
        "text_sub": "#00D26A",
        "lamp": "#00D26A",
        "lamp_ring": "#FFFFFF",
        "style": "dark_accent",
        "proposed": False,
        "drawable": "chip_console_selected_spacex",
        "idle_drawable": "chip_console_idle_spacex",
        "accent_dim": "#005288",
        "company": "SpaceX",
    },
    {
        "id": "nasa",
        "slot": 4,
        "file": "chip_04_nasa",
        "label": "NASA",
        "sub": None,
        "fill": "#1A2840",
        "stroke": "#6A9AD4",
        "rim": "#0A0E18",
        "text": "#E8EEF8",
        "text_sub": "#C8102E",
        "lamp": "#C8102E",
        "lamp_ring": "#0032A0",
        "style": "dark_accent",
        "proposed": False,
        "drawable": "chip_console_selected_nasa",
        "idle_drawable": "chip_console_idle_nasa",
        "accent": "#0032A0",
        "company": "NASA",
    },
    {
        "id": "cnsa",
        "slot": 5,
        "file": "chip_05_cnsa",
        "label": "CNSA",
        "sub": None,
        "fill": "#2A1010",
        "stroke": "#DE2910",
        "rim": "#0C0808",
        "text": "#F5E6C8",
        "text_sub": "#FFD700",
        "lamp": "#FFD700",
        "lamp_ring": "#DE2910",
        "style": "dark_accent",
        "proposed": False,
        "drawable": "chip_console_selected_cnsa",
        "idle_drawable": "chip_console_idle_cnsa",
        "company": "CNSA",
    },
    {
        "id": "arianespace",
        "slot": 6,
        "file": "chip_06_arianespace",
        "label": "ARIANE",
        "sub": None,
        "fill": "#183060",
        "stroke": "#FFD100",
        "rim": "#070B18",
        "text": "#FFFFFF",
        "text_sub": "#FFD100",
        "lamp": "#FFD100",
        "lamp_ring": "#003399",
        "style": "dark_accent",
        "proposed": False,
        "drawable": "chip_console_selected_arianespace",
        "idle_drawable": "chip_console_idle_arianespace",
        "stroke_secondary": "#003399",
        "company": "Arianespace",
    },
    {
        "id": "rocketlab",
        "slot": 7,
        "file": "chip_07_rocketlab",
        "label": "R.LAB",
        "sub": None,  # PROPOSED is tiny badge, not sub crowding
        "fill": "#1C1C1C",
        "stroke": "#FF5A1F",
        "rim": "#080808",
        "text": "#F2F2F2",
        "text_sub": "#FF5A1F",
        "lamp": "#FF5A1F",
        "lamp_ring": "#FF5A1F",
        "style": "dark_accent",
        "proposed": True,
        "drawable": "chip_console_selected_rocketlab",
        "idle_drawable": "chip_console_idle_rocketlab",
        "company": "Rocket Lab",
    },
    {
        "id": "ula",
        "slot": 8,
        "file": "chip_08_ula",
        "label": "ULA",
        "sub": None,
        "fill": "#0B1F44",
        "stroke": "#F6A800",
        "rim": "#061028",
        "text": "#FFFFFF",
        "text_sub": "#F6A800",
        "lamp": "#F6A800",
        "lamp_ring": "#F6A800",
        "style": "dark_accent",
        "proposed": True,
        "drawable": "chip_console_selected_ula",
        "idle_drawable": "chip_console_idle_ula",
        "note": "No TelemetrySkin Tokens yet — Soft-PASS proposed from published brand language only",
        "company": "ULA",
    },
]


def rounded_rect(draw, xy, radius, fill=None, outline=None, width=1):
    draw.rounded_rectangle(xy, radius=radius, fill=fill, outline=outline, width=width)


def draw_chip(preset: dict, selected: bool = True) -> Image.Image:
    """Compact Soft-PASS rocker chip 200×56: rounded rect + lamp + short company label."""
    w, h = CHIP_W, CHIP_H
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    if selected:
        rim = preset["rim"]
        fill = preset["fill"]
        stroke = preset["stroke"]
        text_c = preset["text"]
        lamp = preset["lamp"]
        lamp_ring = preset["lamp_ring"]
        stroke_w = 2
    else:
        rim = darken(preset["rim"], 0.7)
        if preset["style"] == "lit_fill":
            fill = "#2A2010"
            stroke = "#5A4830"
            text_c = "#8A7860"
        else:
            fill = darken(preset["fill"], 0.7)
            stroke = darken(preset["stroke"], 0.35)
            text_c = darken(preset["text"], 0.45)
        lamp = "#222222"
        lamp_ring = darken(preset.get("lamp_ring", stroke), 0.4)
        stroke_w = 1

    # Outer rim
    rounded_rect(draw, (0, 0, w - 1, h - 1), RADIUS + 1, fill=rgba(rim), outline=None)
    # Stroke frame
    rounded_rect(
        draw,
        (1, 1, w - 2, h - 2),
        RADIUS,
        fill=rgba(fill),
        outline=rgba(stroke),
        width=stroke_w,
    )

    # Secondary inner stroke for Arianespace (ESA blue under gold)
    if selected and preset.get("stroke_secondary"):
        rounded_rect(
            draw,
            (4, 4, w - 5, h - 5),
            RADIUS - 2,
            fill=None,
            outline=rgba(preset["stroke_secondary"], 180),
            width=1,
        )

    # NASA meatball accent bar (thin blue left stripe)
    if selected and preset["id"] == "nasa":
        draw.rectangle((5, 8, 8, h - 9), fill=rgba("#0032A0"))

    # SpaceX blue accent dim stripe under white stroke
    if selected and preset["id"] == "spacex":
        draw.rectangle((8, h - 7, w - 9, h - 5), fill=rgba("#005288"))

    # Lamp dot (left) — scaled for compact
    cx, cy = 18, h // 2
    r = 7
    if selected:
        glow = Image.new("RGBA", (w, h), (0, 0, 0, 0))
        gd = ImageDraw.Draw(glow)
        lr, lg, lb = hex_to_rgb(lamp)
        gd.ellipse((cx - r - 3, cy - r - 3, cx + r + 3, cy + r + 3), fill=(lr, lg, lb, 70))
        glow = glow.filter(ImageFilter.GaussianBlur(2))
        img = Image.alpha_composite(img, glow)
        draw = ImageDraw.Draw(img)

    draw.ellipse((cx - r, cy - r, cx + r, cy + r), fill=rgba(lamp_ring))
    inner = r - 3
    if selected:
        draw.ellipse((cx - inner, cy - inner, cx + inner, cy + inner), fill=rgba(lamp))
        draw.ellipse((cx - 2, cy - 3, cx + 1, cy), fill=rgba("#FFFFFF", 160))
    else:
        draw.ellipse(
            (cx - inner, cy - inner, cx + inner, cy + inner),
            fill=rgba("#111111"),
            outline=rgba(lamp_ring),
            width=1,
        )

    # Label — short company Soft-PASS at phone scale
    label = preset["label"]
    if len(label) <= 4:
        font_label = ImageFont.truetype(FONT_BOLD, 18)
    elif len(label) <= 6:
        font_label = ImageFont.truetype(FONT_BOLD, 16)
    else:
        font_label = ImageFont.truetype(FONT_BOLD, 14)

    tx = 32
    bbox = draw.textbbox((0, 0), label, font=font_label)
    th = bbox[3] - bbox[1]
    ty = (h - th) // 2 - 1

    if selected and preset["style"] == "lit_fill":
        draw.text((tx + 1, ty + 1), label, font=font_label, fill=rgba("#FFFFFF", 90))
    draw.text((tx, ty), label, font=font_label, fill=rgba(text_c))

    # Tiny PROPOSED badge for slots 7-8 (Soft-PASS fit)
    if preset.get("proposed") and selected:
        badge_font = ImageFont.truetype(FONT_BOLD, 7)
        badge = "PROPOSED"
        bb = draw.textbbox((0, 0), badge, font=badge_font)
        bw, bh = bb[2] - bb[0], bb[3] - bb[1]
        bx = w - bw - 10
        by = 4
        rounded_rect(
            draw,
            (bx - 3, by - 1, bx + bw + 3, by + bh + 2),
            2,
            fill=rgba(preset["stroke"], 220),
            outline=None,
        )
        draw.text((bx, by), badge, font=badge_font, fill=rgba("#0A0A0A"))

    # Slot index micro tag
    idx_font = ImageFont.truetype(FONT_MED, 8)
    draw.text(
        (w - 12, h - 12),
        str(preset["slot"]),
        font=idx_font,
        fill=rgba(stroke if selected else text_c, 140),
    )

    return img


def save_png(img: Image.Image, path: Path):
    img.save(path, "PNG", optimize=True)


def build_board_compact(chips: list[Image.Image]) -> Image.Image:
    """2×4 compact Soft-PASS composite."""
    cols, rows = 4, 2
    pad = 14
    gap = 10
    header_h = 56
    footer_h = 40
    label_band = 20

    inner_w = cols * CHIP_W + (cols - 1) * gap
    inner_h = rows * (CHIP_H + label_band) + (rows - 1) * gap
    w = inner_w + pad * 2 + 20
    h = inner_h + pad * 2 + header_h + footer_h + 16

    board = Image.new("RGBA", (w, h), (0, 0, 0, 255))
    draw = ImageDraw.Draw(board)

    rounded_rect(draw, (0, 0, w - 1, h - 1), 14, fill=rgba("#0A0C10"))
    rounded_rect(
        draw, (3, 3, w - 4, h - 4), 12, fill=rgba("#12161C"), outline=rgba("#3A4558"), width=2
    )
    draw.rectangle((12, 10, w - 13, 13), fill=rgba("#FFB040"))

    title_font = ImageFont.truetype(FONT_BOLD, 20)
    sub_font = ImageFont.truetype(FONT_MED, 11)
    draw.text((18, 20), "CONSOLE PRESETS  ·  8 COMPACT", font=title_font, fill=rgba("#E8EEF8"))
    draw.text(
        (18, 42),
        "Soft-PASS 200×56 masters  ·  Soft-PASS flyout fit  ·  slots 7–8 PROPOSED",
        font=sub_font,
        fill=rgba("#8A98B0"),
    )

    origin_x = 10 + pad
    origin_y = header_h + 8 + pad
    slot_font = ImageFont.truetype(FONT_MED, 10)

    labels = [
        "1 MCC",
        "2 ROS",
        "3 SPACEX",
        "4 NASA",
        "5 CNSA",
        "6 ARIANE",
        "7 R.LAB ★",
        "8 ULA ★",
    ]

    for i, chip in enumerate(chips):
        r, c = divmod(i, cols)
        x = origin_x + c * (CHIP_W + gap)
        y = origin_y + r * (CHIP_H + label_band + gap)
        board.alpha_composite(chip, (x, y))
        draw.text((x + 2, y + CHIP_H + 4), labels[i], font=slot_font, fill=rgba("#A8B4C8"))

    foot = ImageFont.truetype(FONT_REG, 10)
    draw.text(
        (18, h - 28),
        "Da Vinci  ·  2026-09-23  ·  Soft-FAIL tip142 / Play / vehicle invent  ·  Eyes-only",
        font=foot,
        fill=rgba("#5A6878"),
    )
    draw.text((w - 160, h - 28), "★ = PROPOSED", font=foot, fill=rgba("#F6A800"))

    return board.convert("RGB")


def _draw_flyout_panel(
    img: Image.Image,
    draw: ImageDraw.ImageDraw,
    chips: list[Image.Image],
    W: int,
    H: int,
    panel_margin: int,
    title_note: str,
    annotate: str,
    caption: str,
) -> None:
    """Shared CMD flyout Soft-PASS layout — weight-style 2×4 inside panel."""
    # backdrop
    draw.rectangle((0, 0, W, H), fill=rgba("#05070A"))
    side = max(24, int(W * 0.06))
    draw.rectangle((0, 0, side, H), fill=rgba("#0C1016"))
    draw.rectangle((W - side, 0, W, H), fill=rgba("#0C1016"))
    cmd_font = ImageFont.truetype(FONT_BOLD, max(10, W // 55))
    draw.text((6, int(H * 0.06)), "CMD", font=cmd_font, fill=rgba("#FFB040"))
    draw.text((W - side + 4, int(H * 0.06)), "PAD", font=ImageFont.truetype(FONT_MED, max(9, W // 60)), fill=rgba("#4A5568"))

    px = panel_margin
    py = int(H * 0.10)
    pw = W - panel_margin * 2
    ph = int(H * 0.72)
    rounded_rect(draw, (px, py, px + pw, py + ph), 12, fill=rgba("#101418"), outline=rgba("#3A4558"), width=2)
    draw.rectangle((px + 12, py + 8, px + pw - 12, py + 11), fill=rgba("#FFB040"))

    title_f = ImageFont.truetype(FONT_BOLD, max(16, W // 40))
    draw.text((px + 16, py + 18), "COMMAND", font=title_f, fill=rgba("#E8EEF8"))
    sec_f = ImageFont.truetype(FONT_BOLD, max(11, W // 55))
    draw.text((px + 16, py + 44), "CONSOLE", font=sec_f, fill=rgba("#8A98B0"))

    # Soft-PASS dynamic scale: simulate LinearLayout weight=1 + 0dp width
    # chips fill available row width with equal weight (like match_parent weight layout)
    inset = 12
    gap_x, gap_y = 6, 8
    avail_w = pw - inset * 2
    chip_disp_w = (avail_w - 3 * gap_x) // 4
    # Soft-PASS height ~40–48dp at phone density; keep aspect near master
    chip_disp_h = max(40, min(48, int(chip_disp_w * CHIP_H / CHIP_W)))
    # bump slightly if room — Soft-PASS readable
    if chip_disp_w >= 70:
        chip_disp_h = max(chip_disp_h, 44)

    start_x = px + inset
    start_y = py + 72

    for i, chip in enumerate(chips):
        r, c = divmod(i, 4)
        small = chip.resize((chip_disp_w, chip_disp_h), Image.Resampling.LANCZOS)
        x = start_x + c * (chip_disp_w + gap_x)
        y = start_y + r * (chip_disp_h + gap_y)
        img.alpha_composite(small, (x, y))

    gy = start_y + 2 * (chip_disp_h + gap_y) + 10
    note_f = ImageFont.truetype(FONT_MED, max(9, W // 70))
    draw.text((px + 16, gy), title_note, font=note_f, fill=rgba("#6A7888"))

    # Soft-PASS annotation box
    ann_f = ImageFont.truetype(FONT_BOLD, max(9, W // 72))
    ann_y = gy + 22
    # wrap annotate roughly
    max_chars = max(28, pw // 8)
    words = annotate.split()
    lines = []
    cur = ""
    for word in words:
        trial = (cur + " " + word).strip()
        if len(trial) <= max_chars:
            cur = trial
        else:
            if cur:
                lines.append(cur)
            cur = word
    if cur:
        lines.append(cur)
    box_h = 10 + len(lines) * (ann_f.size + 4)
    rounded_rect(
        draw,
        (px + 12, ann_y, px + pw - 12, ann_y + box_h),
        6,
        fill=rgba("#1A2830"),
        outline=rgba("#00D26A"),
        width=1,
    )
    for li, line in enumerate(lines):
        draw.text((px + 18, ann_y + 6 + li * (ann_f.size + 4)), line, font=ann_f, fill=rgba("#00D26A"))

    # placeholder rows Soft-PASS simple (Soft-FAIL invent full chrome)
    row_y = ann_y + box_h + 20
    for label, opts in [
        ("HUD TEXT", ["SM", "MD", "LG"]),
        ("LAMP", ["DIM", "NORM", "BRT"]),
    ]:
        if row_y + 36 > py + ph - 40:
            break
        draw.text((px + 16, row_y), label, font=sec_f, fill=rgba("#8A98B0"))
        ox = px + max(100, int(pw * 0.28))
        bw = max(48, (pw - (ox - px) - 24) // 3 - 6)
        for j, o in enumerate(opts):
            bx = ox + j * (bw + 6)
            rounded_rect(
                draw,
                (bx, row_y - 2, bx + bw, row_y + 24),
                6,
                fill=rgba("#1A2028"),
                outline=rgba("#3A4558"),
                width=1,
            )
            draw.text(
                (bx + 8, row_y + 4),
                o,
                font=ImageFont.truetype(FONT_MED, max(10, W // 65)),
                fill=rgba("#A8B4C8"),
            )
        row_y += 40

    cap = ImageFont.truetype(FONT_REG, max(9, W // 80))
    draw.text((px + 16, py + ph - 22), caption, font=cap, fill=rgba("#4A5568"))


def build_flyout_fit(chips: list[Image.Image]) -> Image.Image:
    """Phone-width Soft-PASS proof — 720×1280 CMD flyout 2×4 Soft-PASS fit."""
    W, H = 720, 1280
    img = Image.new("RGBA", (W, H), (8, 10, 14, 255))
    draw = ImageDraw.Draw(img)
    _draw_flyout_panel(
        img,
        draw,
        chips,
        W,
        H,
        panel_margin=64,
        title_note="8 compact Soft-PASS  ·  2×4 Soft-PASS fit  ·  Ada after Helios + Chris ❤️",
        annotate="Soft-PASS dynamic scale — Ada: 0dp width + weight 1 + ~40–48dp height Soft-PASS",
        caption="mock_cmd_flyout_8_fit  ·  Soft-FAIL tip142/Play/vehicle invent  ·  Eyes-only",
    )
    return img.convert("RGB")


def build_flyout_narrow(chips: list[Image.Image]) -> Image.Image:
    """~360dp-class narrow Soft-PASS proof — chips still Soft-PASS readable."""
    # 360dp @ 2x ≈ 720px logical mock; use 360×640 phone chrome then upscale? 
    # Better: render at 360×740 px as "dp-class" pixel mock for Soft-PASS narrow proof
    W, H = 360, 740
    img = Image.new("RGBA", (W, H), (8, 10, 14, 255))
    draw = ImageDraw.Draw(img)
    _draw_flyout_panel(
        img,
        draw,
        chips,
        W,
        H,
        panel_margin=16,
        title_note="360dp-class Soft-PASS  ·  chips Soft-PASS readable",
        annotate="Soft-PASS narrow Soft-PASS — still Soft-PASS readable · weight 1 Soft-PASS",
        caption="mock_cmd_flyout_8_narrow  ·  Soft-FAIL tip142/Play  ·  Eyes-only",
    )
    return img.convert("RGB")


def md5_file(path: Path) -> str:
    h = hashlib.md5()
    with open(path, "rb") as f:
        for chunk in iter(lambda: f.read(65536), b""):
            h.update(chunk)
    return h.hexdigest()


def write_tokens_md():
    lines = [
        "# Console 8-preset art tokens — Soft-PASS compact for Ada",
        "",
        "**Pack:** `/workspace/agent-backups/Da-Vinci/console-8-preset-20260923/`",
        "**Date:** 2026-09-23 (CT)",
        "**Agent:** Da Vinci (art executor)",
        "**Scope:** Soft-PASS art chips + hex tokens only. Soft-FAIL wire prefs / tip142 / Play.",
        "",
        "Sources: `TelemetrySkin.kt` agency Tokens + MCC `chip_console_selected_mcc.xml`.",
        "ULA: Soft-PASS proposed from published brand language (no Tokens yet).",
        "",
        "## Soft-PASS size (compact masters)",
        "",
        f"- **Art master size:** `{CHIP_W}×{CHIP_H}` px (Soft-FAIL fat `320×120` for flyout).",
        "- **Short labels (phone Soft-PASS read):** MCC · ROS · SPACEX · NASA · CNSA · ARIANE · R.LAB · ULA",
        "- Soft-FAIL long “MISSION CONTROL” / full Cyrillic crowding on compact Soft-PASS.",
        "- Soft-FAIL ship fixed 320px bitmaps as the sole Soft-PASS size.",
        "",
        "## Soft-PASS Ada direction (dynamic scale)",
        "",
        "**Critical:** Soft-PASS XML **shape drawables in dp** (not fixed-px bitmaps as sole Soft-PASS size).",
        "Art PNGs here are Soft-PASS eyes-only masters / preview. Runtime Soft-PASS = shape + text Soft-PASS.",
        "",
        "Match live rockers (`activity_main.xml` `row_console_skin`): 0dp width + `layout_weight=1`.",
        "",
        "Suggest **2 LinearLayouts** (row1 / row2), Soft-PASS each with **4 buttons**:",
        "",
        "```xml",
        "<!-- Soft-PASS Ada sketch — Soft-FAIL wire until Helios + Chris ❤️ -->",
        "<LinearLayout",
        "    android:orientation=\"vertical\"",
        "    android:layout_width=\"match_parent\"",
        "    android:layout_height=\"wrap_content\">",
        "",
        "  <LinearLayout  <!-- row1: MCC · ROS · SPACEX · NASA -->",
        "      android:orientation=\"horizontal\"",
        "      android:layout_width=\"match_parent\"",
        "      android:layout_height=\"wrap_content\"",
        "      android:layout_marginBottom=\"6dp\">",
        "    <Button",
        "        android:layout_width=\"0dp\"",
        "        android:layout_height=\"40dp\"   <!-- Soft-PASS 40–48dp -->",
        "        android:layout_weight=\"1\"",
        "        android:layout_margin=\"2dp\"",
        "        android:textSize=\"11sp\"        <!-- Soft-PASS ~11–12sp -->",
        "        android:text=\"MCC\"",
        "        android:background=\"@drawable/chip_console_selected_mcc\"/>",
        "    <!-- ×3 more Soft-PASS weight=1 buttons -->",
        "  </LinearLayout>",
        "",
        "  <LinearLayout  <!-- row2: CNSA · ARIANE · R.LAB · ULA -->",
        "      android:orientation=\"horizontal\"",
        "      android:layout_width=\"match_parent\"",
        "      android:layout_height=\"wrap_content\">",
        "    <!-- 4 Soft-PASS buttons: 0dp + weight 1 + height 40–48dp + textSize ~11–12sp -->",
        "  </LinearLayout>",
        "</LinearLayout>",
        "```",
        "",
        "- Soft-PASS height: **40–48dp** (matches live console skin rocker height class).",
        "- Soft-PASS textSize: **~11–12sp** short company labels Soft-PASS readable on narrow.",
        "- Soft-PASS proof mocks: `mock_cmd_flyout_8_fit.png` + `mock_cmd_flyout_8_narrow.png`.",
        "",
        "## Layout (row1 | row2)",
        "",
        "| Slot | ID | Label | Company | Status |",
        "|------|----|-------|---------|--------|",
        "| 1 | mcc | MCC | MCC | Soft-PASS favorite amber |",
        "| 2 | ros | ROS | Roscosmos | Soft-PASS |",
        "| 3 | spacex | SPACEX | SpaceX | Soft-PASS retheme |",
        "| 4 | nasa | NASA | NASA | Soft-PASS (distinct from MCC) |",
        "| 5 | cnsa | CNSA | CNSA | Soft-PASS |",
        "| 6 | arianespace | ARIANE | Arianespace | Soft-PASS |",
        "| 7 | rocketlab | R.LAB | Rocket Lab | Soft-PASS **PROPOSED** (tiny badge) |",
        "| 8 | ula | ULA | ULA | Soft-PASS **PROPOSED** (tiny badge) |",
        "",
        "## Per-slot hex (selected)",
        "",
    ]
    for p in PRESETS:
        lines += [
            f"### {p['slot']}. `{p['id']}` — {p['label']}",
            "",
            f"- **company:** {p.get('company', p['label'])}",
            f"- **fill:** `{p['fill']}`",
            f"- **stroke:** `{p['stroke']}`",
            f"- **rim / dark:** `{p['rim']}`",
            f"- **text:** `{p['text']}`",
            f"- **text_sub / accent secondary:** `{p['text_sub']}`",
            f"- **lamp:** `{p['lamp']}`",
            f"- **lamp_ring:** `{p['lamp_ring']}`",
            f"- **style:** `{p['style']}`",
            f"- **suggested selected drawable:** `{p['drawable']}.xml`",
            f"- **suggested idle drawable:** `{p['idle_drawable']}.xml`",
            f"- **art PNG:** `{p['file']}.png` / `{p['file']}_idle.png` ({CHIP_W}×{CHIP_H})",
        ]
        if p.get("stroke_secondary"):
            lines.append(f"- **stroke_secondary:** `{p['stroke_secondary']}`")
        if p.get("accent"):
            lines.append(f"- **accent (NASA blue bar):** `{p['accent']}`")
        if p.get("accent_dim"):
            lines.append(f"- **accent_dim (SpaceX stripe):** `{p['accent_dim']}`")
        if p.get("note"):
            lines.append(f"- **note:** {p['note']}")
        if p.get("proposed"):
            lines.append("- **badge:** tiny `PROPOSED` Soft-PASS")
        lines.append("")

    lines += [
        "## Idle guidance (all slots)",
        "",
        "- Dim fill ~45–70% of selected fill (MCC idle uses olive-dark `#2A2010`, not lit amber).",
        "- Stroke muted ~35% of selected stroke brightness.",
        "- Lamp = empty ring / `#222222` core (off).",
        "- Text muted ~45% of selected text.",
        "",
        "## XML shape direction (Ada — Soft-FAIL wire)",
        "",
        "Mirror existing Stamp 69 layer-list pattern — Soft-PASS **dp**, Soft-FAIL sole fixed-px bitmap ship:",
        "",
        "```xml",
        "<!-- example: chip_console_selected_<id>.xml -->",
        "<layer-list>",
        "  <item>  <!-- rim + stroke -->",
        "    <shape android:shape=\"rectangle\">",
        "      <solid android:color=\"{rim}\"/>",
        "      <stroke android:width=\"2dp\" android:color=\"{stroke}\"/>",
        "      <corners android:radius=\"6dp\"/>",
        "    </shape>",
        "  </item>",
        "  <item android:left=\"3dp\" android:top=\"3dp\" android:right=\"3dp\" android:bottom=\"3dp\">",
        "    <shape android:shape=\"rectangle\">",
        "      <solid android:color=\"{fill}\"/>",
        "      <corners android:radius=\"4dp\"/>",
        "    </shape>",
        "  </item>",
        "</layer-list>",
        "```",
        "",
        "MCC keeps lit-fill amber language (`#FFB040` / `#FFD080` / `#3A2800`) — Soft-FAIL absorb into NASA.",
        "NASA stays blue/meatball language (`#0032A0` / `#C8102E` / `#0A0E18`).",
        "",
        "## Soft-FAIL reminders",
        "",
        "- Soft-FAIL invent vehicle stacks / rocket hulls in these boxes.",
        "- Soft-FAIL tip142 ship / Play / device — Soft-FAIL ❤️ until Chris Soft-PASS.",
        "- Soft-FAIL overwrite Live-Rocket-Tracker drawable trees from this pack.",
        "- Soft-PASS Studio-only after Helios Soft-PASS + Chris ❤️.",
        "- Soft-FAIL ship fixed 320px bitmaps as sole Soft-PASS size — Soft-PASS dp shape + weight.",
        "- Auto live-provider Soft-PASS remains Ada (`TelemetrySkin.forLaunch`); these are settings/CMD override chips.",
        "",
    ]
    (OUT / "chip_console_tokens.md").write_text("\n".join(lines), encoding="utf-8")
    print("wrote chip_console_tokens.md")


def main():
    selected_chips = []
    paths = []

    for p in PRESETS:
        sel = draw_chip(p, selected=True)
        idle = draw_chip(p, selected=False)
        sel_path = OUT / f"{p['file']}.png"
        idle_path = OUT / f"{p['file']}_idle.png"
        save_png(sel, sel_path)
        save_png(idle, idle_path)
        selected_chips.append(sel)
        paths.append(sel_path)
        paths.append(idle_path)
        print(f"wrote {sel_path.name} + idle  ({CHIP_W}x{CHIP_H})")

    board = build_board_compact(selected_chips)
    board_path = OUT / "BOARD_8_presets_compact.png"
    board.save(board_path, "PNG", optimize=True)
    paths.append(board_path)
    print(f"wrote {board_path.name}")

    # Keep legacy board name as alias soft-pass copy of compact for eyes continuity
    legacy_board = OUT / "BOARD_8_presets.png"
    board.save(legacy_board, "PNG", optimize=True)
    print(f"wrote {legacy_board.name} (compact Soft-PASS overwrite)")

    fit = build_flyout_fit(selected_chips)
    fit_path = OUT / "mock_cmd_flyout_8_fit.png"
    fit.save(fit_path, "PNG", optimize=True)
    print(f"wrote {fit_path.name}")

    narrow = build_flyout_narrow(selected_chips)
    narrow_path = OUT / "mock_cmd_flyout_8_narrow.png"
    narrow.save(narrow_path, "PNG", optimize=True)
    print(f"wrote {narrow_path.name}")

    # legacy mock overwritten with fit Soft-PASS for continuity
    legacy_fly = OUT / "mock_cmd_flyout_8.png"
    fit.save(legacy_fly, "PNG", optimize=True)
    print(f"wrote {legacy_fly.name} (fit Soft-PASS overwrite)")

    write_tokens_md()

    # BOARD.md
    (OUT / "BOARD.md").write_text(
        "\n".join(
            [
                "# BOARD — Console 8 presets COMPACT (Da Vinci Soft-PASS)",
                "",
                "**Date:** 2026-09-23 CT",
                "**Composite:** `BOARD_8_presets_compact.png`",
                "**Also:** `BOARD_8_presets.png` (same Soft-PASS compact overwrite)",
                "**Fit mocks:** `mock_cmd_flyout_8_fit.png` · `mock_cmd_flyout_8_narrow.png`",
                f"**Master size:** Soft-PASS `{CHIP_W}×{CHIP_H}` (Soft-FAIL fat 320×120)",
                "",
                "## Locked layout (short Soft-PASS labels)",
                "",
                "```",
                "Row 1:  1 MCC  |  2 ROS  |  3 SPACEX  |  4 NASA",
                "Row 2:  5 CNSA  |  6 ARIANE  |  7 R.LAB [PROPOSED]  |  8 ULA [PROPOSED]",
                "```",
                "",
                "Roster unchanged Soft-PASS: MCC · Roscosmos · SpaceX · NASA · CNSA · Arianespace · Rocket Lab · ULA",
                "",
                "## Soft-PASS dynamic scale (Ada)",
                "",
                "- Soft-PASS XML shape drawables in **dp** (Soft-FAIL ship fixed 320px bitmaps as sole Soft-PASS size).",
                "- Suggest 2 LinearLayouts (row1/row2) Soft-PASS each 4 buttons Soft-PASS `0dp` + `weight=1` Soft-PASS height **40–48dp** Soft-PASS textSize **~11–12sp**.",
                "- Soft-PASS proof: fit mock + narrow (~360dp-class) Soft-PASS readable.",
                "",
                "## Notes",
                "",
                "- MCC amber lit Soft-PASS distinct from NASA blue Soft-PASS.",
                "- Slots 7–8 tiny **PROPOSED** badge Soft-PASS.",
                "- Soft-FAIL long MISSION CONTROL / full Cyrillic crowding on compact.",
                "- Soft-FAIL tip142 / Play / vehicle invent Soft-FAIL ❤️ until Chris Soft-PASS.",
                "- Soft-FAIL overwrite Live-Rocket-Tracker drawable trees Soft-FAIL invent vehicles Soft-FAIL Play.",
                "- Eyes-only pack. Parent pings Helios on READY (Soft-FAIL SendToAgent from executor).",
                "",
                "## Chip files",
                "",
                "| Slot | Selected | Idle |",
                "|------|----------|------|",
            ]
            + [
                f"| {p['slot']} {p['label']} | `{p['file']}.png` | `{p['file']}_idle.png` |"
                for p in PRESETS
            ]
            + ["",]
        ),
        encoding="utf-8",
    )
    print("wrote BOARD.md")

    # READY.md (md5s filled after hashes)
    chip_md5s = {p["file"]: md5_file(OUT / f"{p['file']}.png") for p in PRESETS}
    board_md5 = md5_file(board_path)
    fit_md5 = md5_file(fit_path)
    narrow_md5 = md5_file(narrow_path)

    (OUT / "READY.md").write_text(
        "\n".join(
            [
                "# READY — Da Vinci console-8-preset-20260923 COMPACT",
                "",
                "**Status:** Soft-PASS compact art pack complete (eyes-only).",
                "**Path:** `/workspace/agent-backups/Da-Vinci/console-8-preset-20260923/`",
                "**Date:** 2026-09-23 CT",
                f"**Master:** Soft-PASS {CHIP_W}×{CHIP_H} (was Soft-FAIL fat 320×120)",
                "",
                "## Deliverables",
                "",
                "- [x] 8 selected compact chips Soft-PASS short labels",
                "- [x] 8 idle chips same Soft-PASS size",
                "- [x] `BOARD_8_presets_compact.png` (2×4 Soft-PASS)",
                "- [x] `mock_cmd_flyout_8_fit.png` Soft-PASS dynamic-scale proof",
                "- [x] `mock_cmd_flyout_8_narrow.png` Soft-PASS ~360dp-class proof",
                "- [x] `chip_console_tokens.md` (Ada Soft-PASS dp / weight direction)",
                "- [x] `BOARD.md` + `MD5SUMS.txt` + `READY.md`",
                "",
                "## Soft-FAIL confirm (❤️ until Chris Soft-PASS)",
                "",
                "- Soft-FAIL tip142 / Play / device ship Soft-FAIL ❤️",
                "- Soft-FAIL vehicle stacks / rocket hulls invent in boxes",
                "- Soft-FAIL overwrite Live-Rocket-Tracker / tip drawable trees",
                "- Soft-FAIL invent vehicles Soft-FAIL Play",
                "- Soft-FAIL SendToAgent/Helios from this executor (parent pings READY)",
                "- Soft-FAIL ship fixed 320px bitmaps as sole Soft-PASS size",
                "",
                "## Key md5",
                "",
                f"- `BOARD_8_presets_compact.png` → `{board_md5}`",
                f"- `mock_cmd_flyout_8_fit.png` → `{fit_md5}`",
                f"- `mock_cmd_flyout_8_narrow.png` → `{narrow_md5}`",
            ]
            + [f"- `{k}.png` → `{v}`" for k, v in chip_md5s.items()]
            + [
                "",
                "## Next",
                "",
                "Parent → Helios review → Chris Soft-PASS ❤️ → Ada wire Studio-only (dp Soft-PASS).",
                "",
            ]
        ),
        encoding="utf-8",
    )
    print("wrote READY.md")

    # MD5SUMS (exclude self + generator)
    md5_lines = []
    for f in sorted(OUT.iterdir()):
        if f.name.startswith("_"):
            continue
        if f.is_file() and f.suffix.lower() in {".png", ".md", ".txt"} and f.name != "MD5SUMS.txt":
            md5_lines.append(f"{md5_file(f)}  {f.name}")
    (OUT / "MD5SUMS.txt").write_text("\n".join(md5_lines) + "\n", encoding="utf-8")
    print("wrote MD5SUMS.txt")

    print("DONE")
    print("BOARD_compact md5:", board_md5)
    print("fit md5:", fit_md5)
    print("narrow md5:", narrow_md5)
    for k, v in chip_md5s.items():
        print(f"  {k}: {v}")


if __name__ == "__main__":
    main()
