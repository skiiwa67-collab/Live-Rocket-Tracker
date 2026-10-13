#!/usr/bin/env python3
"""CZ-6A nozzle + SRB burn-bar preview ONLY.

Helios Soft-PASS research correction:
- Soft-FAIL solid burn bar on CZ-8A (liquid YF-100 strap-ons)
- Preview vehicle = CZ-6A (4× FG-112 solids); side view shows L/R pair
- Burn bar calibrated to 114.5 s (NSF maiden); preview shows mid-burn (~50%)
- Soft-FAIL commit/publish/device/Studio/Play
"""
from __future__ import annotations

import hashlib
import sys
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageFont

ROOT = Path("/workspace/agent-backups/Da-Vinci/nozzle-starship-bar-20260923")
sys.path.insert(0, str(ROOT))
from lib_side_bell import draw_side_profile_bell, paint_bells_on_layer  # noqa: E402

W, H = 424, 1200
PACK = Path("/workspace/lrt-cz6a-pack-20260923")
CROPS = ROOT / "crops"

# Measured from existing vehicle_cz6a_engine_booster.png / srb.png
ENGINE_CY = 973
# outer = visible SRB nozzles; inner = core 2×YF-100
OFFSETS_ALL = [-137, -28, 28, 137]
BELL_W = 13
BELL_H = 44

# Side-view SRB bodies (L/R pair; fore/aft occluded)
SRB_L = dict(x0=24, x1=121, y0=727, y1=974, cx=72)
SRB_R = dict(x0=303, x1=399, y0=727, y1=974, cx=351)

# FG-112 burn calibration (NSF maiden): 114.5 s
FG112_BURN_S = 114.5
# Preview demo state: mid-burn
PREVIEW_T_S = 57.0  # ~50% expended


def md5(path: Path) -> str:
    h = hashlib.md5()
    with path.open("rb") as f:
        for chunk in iter(lambda: f.read(1 << 20), b""):
            h.update(chunk)
    return h.hexdigest()


def load_font(size: int):
    for p in (
        "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf",
        "/usr/share/fonts/truetype/liberation/LiberationSans-Bold.ttf",
        "/usr/share/fonts/truetype/freefont/FreeSansBold.ttf",
    ):
        try:
            return ImageFont.truetype(p, size)
        except OSError:
            continue
    return ImageFont.load_default()


def draw_srb_burn_bar(
    img: Image.Image,
    srb: dict,
    *,
    t_s: float = PREVIEW_T_S,
    burn_s: float = FG112_BURN_S,
    bar_w: int = 10,
) -> Image.Image:
    """Simple vertical burn tell along SRB length.

    Soft-FAIL invent propellant color science — light expended vs burning only.
    Expended (from nose/top): dim cool gray. Remaining/burning: warm light amber.
    Fraction expended = clamp(t/burn, 0..1). Solids burn tell progresses top→down
    as a UI progress strip (not physics grain model).
    """
    out = img.copy()
    d = ImageDraw.Draw(out)
    frac = max(0.0, min(1.0, t_s / burn_s))
    y0, y1 = srb["y0"] + 8, srb["y1"] - 6
    h = max(1, y1 - y0)
    split = y0 + int(round(h * frac))  # above = expended
    cx = srb["cx"]
    # place bar slightly inward of outer skin so it reads on the booster face
    if cx < W // 2:
        bx = cx + 18  # left SRB: bar toward core
    else:
        bx = cx - 18  # right SRB: bar toward core
    x0, x1 = bx - bar_w // 2, bx + bar_w // 2

    # track outline
    d.rectangle([x0 - 1, y0 - 1, x1 + 1, y1 + 1], outline=(40, 42, 48, 220), width=1)
    # expended (top)
    if split > y0:
        d.rectangle([x0, y0, x1, split], fill=(70, 74, 82, 230))
    # remaining / burning (bottom)
    if split < y1:
        d.rectangle([x0, split, x1, y1], fill=(230, 170, 70, 235))
    # burn front tick
    d.rectangle([x0 - 2, split - 1, x1 + 2, split + 1], fill=(255, 220, 140, 250))
    return out


def erase_engines(stack: Image.Image, y0: int = ENGINE_CY - 2) -> Image.Image:
    out = stack.copy().convert("RGBA")
    px = out.load()
    for y in range(y0, H):
        for x in range(W):
            r, g, b, a = px[x, y]
            px[x, y] = (0, 0, 0, 0)
    return out


def make_panel(img: Image.Image, y0: int, y1: int) -> Image.Image:
    crop = img.crop((0, y0, W, y1))
    panel = Image.new("RGBA", (W, y1 - y0), (0, 0, 0, 255))
    panel.alpha_composite(crop, (0, 0))
    return panel.convert("RGB")


def label_bar(draw, box, text, font):
    x0, y0, x1, y1 = box
    draw.rectangle([x0, y0, x1, y1], fill=(0, 0, 0, 180))
    bbox = draw.textbbox((0, 0), text, font=font)
    tw, th = bbox[2] - bbox[0], bbox[3] - bbox[1]
    draw.text(
        (x0 + (x1 - x0 - tw) // 2, y0 + (y1 - y0 - th) // 2 - 1),
        text,
        fill=(255, 255, 255, 255),
        font=font,
    )


def main():
    ROOT.mkdir(parents=True, exist_ok=True)

    old_eng = Image.open(PACK / "vehicle_cz6a_engine_booster.png").convert("RGBA")
    stack = Image.open(PACK / "vehicle_cz6a_stack.png").convert("RGBA")

    # NEW main+SRB nozzles — Starship side-profile (same placement/scale)
    new_eng = paint_bells_on_layer(
        (W, H),
        ENGINE_CY,
        OFFSETS_ALL,
        BELL_W,
        BELL_H,
        color_outer=(122, 128, 136, 250),
        color_inner=(28, 30, 34, 230),
    )
    new_eng_path = ROOT / "cz6a_engine_booster_NEW.png"
    new_eng.save(new_eng_path)

    # Burn bars layer (SRB length tell only)
    burn = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    burn = draw_srb_burn_bar(burn, SRB_L)
    burn = draw_srb_burn_bar(burn, SRB_R)
    burn_path = ROOT / "cz6a_srb_burn_bar_NEW.png"
    burn.save(burn_path)

    # Overlay engines+burn for Ada-style optional layers
    eng_and_burn = Image.alpha_composite(new_eng, burn)
    eng_burn_path = ROOT / "cz6a_engine_and_srb_burn_NEW.png"
    eng_and_burn.save(eng_burn_path)

    # Body without old nozzles
    body = erase_engines(stack, ENGINE_CY - 2)

    old_comp = Image.alpha_composite(body, old_eng)
    new_comp = Image.alpha_composite(body, new_eng)
    new_comp = Image.alpha_composite(new_comp, burn)

    old_full = ROOT / "cz6a_stack_with_OLD_eng.png"
    new_full = ROOT / "cz6a_stack_with_NEW_eng_burn.png"
    old_comp.save(old_full)
    new_comp.save(new_full)

    # Crop tall enough to show SRB burn bars + nozzles
    crop_y0, crop_y1 = 700, 1040
    old_panel = make_panel(old_comp, crop_y0, crop_y1)
    new_panel = make_panel(new_comp, crop_y0, crop_y1)
    old_panel.save(ROOT / "cz6a_stack_bottom_OLD.png")
    new_panel.save(ROOT / "cz6a_stack_bottom_NEW.png")

    # Starship ref — stretch/pad to same panel height
    star_path = CROPS / "starship_side_bottom.png"
    ph = crop_y1 - crop_y0
    if star_path.exists():
        star = Image.open(star_path).convert("RGB")
        star = star.resize((W, ph), Image.Resampling.LANCZOS)
    else:
        star = Image.new("RGB", (W, ph), (20, 20, 20))

    gap = 8
    label_h = 36
    cols = 3
    board_w = W * cols + gap * (cols + 1)
    foot_h = 48
    board_h = label_h + ph + gap * 2 + foot_h
    board = Image.new("RGB", (board_w, board_h), (18, 18, 22))
    draw = ImageDraw.Draw(board, "RGBA")
    font = load_font(18)
    small = load_font(13)

    panels = [star, old_panel, new_panel]
    labels = ["STARSHIP REF", "OLD", "NEW"]
    for i, (panel, lab) in enumerate(zip(panels, labels)):
        x = gap + i * (W + gap)
        label_bar(draw, (x, gap, x + W, gap + label_h - 4), lab, font)
        board.paste(panel, (x, gap + label_h))

    foot1 = "CZ-6A preview — main YF-100 + FG-112 SRB nozzles (Starship side-profile)"
    foot2 = (
        f"SRB burn bar: FG-112 = {FG112_BURN_S:g}s (NSF) · demo t≈{PREVIEW_T_S:g}s "
        f"(~{int(round(100 * PREVIEW_T_S / FG112_BURN_S))}% expended) · HOLD fleet"
    )
    for i, (txt, fnt, col) in enumerate(
        [
            (foot1, small, (200, 205, 210)),
            (foot2, small, (180, 185, 190)),
        ]
    ):
        bb = draw.textbbox((0, 0), txt, font=fnt)
        draw.text(
            ((board_w - (bb[2] - bb[0])) // 2, board_h - foot_h + 6 + i * 18),
            txt,
            fill=col,
            font=fnt,
        )

    # Legend chip on NEW panel: dim=expended / amber=burning
    nx = gap + 2 * (W + gap)
    ny = gap + label_h + 8
    draw.rectangle([nx + 8, ny, nx + 20, ny + 12], fill=(70, 74, 82))
    draw.text((nx + 24, ny - 1), "expended", fill=(200, 200, 200), font=small)
    draw.rectangle([nx + 110, ny, nx + 122, ny + 12], fill=(230, 170, 70))
    draw.text((nx + 126, ny - 1), "burning", fill=(200, 200, 200), font=small)

    preview_path = ROOT / "compare_cz6a_preview.png"
    board.save(preview_path, optimize=True)

    # READY_PREVIEW.md + MD5SUMS + NOTES
    deliverable_names = [
        "compare_cz6a_preview.png",
        "cz6a_engine_booster_NEW.png",
        "cz6a_srb_burn_bar_NEW.png",
        "cz6a_engine_and_srb_burn_NEW.png",
        "cz6a_stack_bottom_OLD.png",
        "cz6a_stack_bottom_NEW.png",
        "lib_side_bell.py",
        "READY_PREVIEW.md",
        "NOTES.md",
    ]

    lines = [
        "# CZ-6A Nozzle + SRB Burn-Bar Preview — READY for Chris eyes",
        "",
        "**Scope:** preview ONLY. Soft-FAIL commit / publish / thread / catalog / device / Studio / Play.",
        "",
        "**Vehicle:** Long March 6A (real solids: 4× FG-112). Side orthographic shows L/R SRB pair.",
        "",
        "**Helios research correction applied:**",
        "- Soft-FAIL solid burn bar on CZ-8A (side boosters are liquid YF-100 kerolox).",
        "- Soft-FAIL Nuri / SR-75 solid bar.",
        "- Soft-PASS CZ-6A FG-112 burn calibration **114.5 s** (NSF maiden; sep ~117.5).",
        "",
        "**What changed (NEW):**",
        "1. Core + SRB nozzles: ellipse blobs → Starship-bar side-profile de Laval (`lib_side_bell.py`).",
        "2. Each visible SRB: simple vertical burn tell (dim expended / warm burning) along booster length.",
        "3. Preview demo at t≈57 s (~50% of 114.5 s). Soft-FAIL invent propellant color science.",
        "",
        "**HOLD:** full fleet nozzle/SRB-bar rebuild until Chris Soft-PASS / ❤️ on this direction.",
        "",
        "## Deliverables",
        "",
    ]

    md5_all = []
    for name in deliverable_names:
        p = ROOT / name
        if not p.exists():
            continue
        digest = md5(p) if p.suffix != ".md" or name.endswith(".md") else md5(p)
        if p.is_file() and p.suffix in {".png", ".py", ".txt", ".md"}:
            digest = md5(p)
            lines.append(f"- `{p}`  md5 `{digest}`")
            md5_all.append(f"{digest}  {p.name}")

    # checksum every png in dir
    for p in sorted(ROOT.glob("*.png")):
        digest = md5(p)
        entry = f"{digest}  {p.name}"
        if entry not in md5_all:
            md5_all.append(entry)

    ready = ROOT / "READY_PREVIEW.md"
    ready.write_text("\n".join(lines) + "\n", encoding="utf-8")
    # refresh md5 after writing READY
    md5_all = [f"{md5(p)}  {p.name}" for p in sorted(ROOT.glob("*")) if p.suffix in {".png", ".py", ".md", ".txt"} and p.name != "MD5SUMS.txt"]
    (ROOT / "MD5SUMS.txt").write_text("\n".join(md5_all) + "\n", encoding="utf-8")

    (ROOT / "NOTES.md").write_text(
        "CZ-6A preview (supersedes CZ-8A solid-bar attempt): Starship side-profile de Laval "
        "nozzles on core YF-100 + FG-112 SRBs; simple burn progress bar along each visible "
        f"SRB calibrated to FG-112 = {FG112_BURN_S:g}s (NSF). Soft-FAIL solid bar on CZ-8A "
        "(liquid strap-ons), Nuri, SR-75. HOLD full fleet pending Chris Soft-PASS. "
        "Soft-FAIL commit/device/Studio/Play.\n",
        encoding="utf-8",
    )

    # final READY with accurate md5s including NOTES refresh
    lines = [
        "# CZ-6A Nozzle + SRB Burn-Bar Preview — READY for Chris eyes",
        "",
        "**Scope:** preview ONLY. Soft-FAIL commit / publish / thread / catalog / device / Studio / Play.",
        "",
        "**Vehicle:** Long March 6A (real solids: 4× FG-112). Side orthographic shows L/R SRB pair.",
        "",
        "**Helios research correction applied:**",
        "- Soft-FAIL solid burn bar on CZ-8A (side boosters are liquid YF-100 kerolox).",
        "- Soft-FAIL Nuri / SR-75 solid bar.",
        "- Soft-PASS CZ-6A FG-112 burn calibration **114.5 s** (NSF maiden; sep ~117.5).",
        "",
        "**What changed (NEW):**",
        "1. Core + SRB nozzles: ellipse blobs → Starship-bar side-profile de Laval (`lib_side_bell.py`).",
        "2. Each visible SRB: simple vertical burn tell (dim expended / warm burning) along booster length.",
        "3. Preview demo at t≈57 s (~50% of 114.5 s). Soft-FAIL invent propellant color science.",
        "",
        "**HOLD:** full fleet nozzle/SRB-bar rebuild until Chris Soft-PASS / ❤️ on this direction.",
        "",
        "## Key paths + md5s",
        "",
    ]
    key = [
        preview_path,
        new_eng_path,
        burn_path,
        eng_burn_path,
        ROOT / "cz6a_stack_bottom_NEW.png",
        ROOT / "lib_side_bell.py",
    ]
    for p in key:
        lines.append(f"- `{p}`  md5 `{md5(p)}`")
    ready.write_text("\n".join(lines) + "\n", encoding="utf-8")
    md5_all = [
        f"{md5(p)}  {p.name}"
        for p in sorted(ROOT.iterdir())
        if p.is_file() and p.name != "MD5SUMS.txt"
    ]
    (ROOT / "MD5SUMS.txt").write_text("\n".join(md5_all) + "\n", encoding="utf-8")

    print("PREVIEW", preview_path, md5(preview_path))
    print("ENG", new_eng_path, md5(new_eng_path))
    print("BURN", burn_path, md5(burn_path))
    print("READY", ready)


if __name__ == "__main__":
    main()
