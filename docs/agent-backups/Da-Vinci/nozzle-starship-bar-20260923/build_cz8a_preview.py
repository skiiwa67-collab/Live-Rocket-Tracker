#!/usr/bin/env python3
"""CZ-8A nozzle preview ONLY — Starship side-profile vs OLD ellipse blobs.

Soft-FAIL Electron/F9/fleet/commit/device. Preview for Chris eyes.
"""
from __future__ import annotations

import hashlib
import sys
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path("/workspace/agent-backups/Da-Vinci/nozzle-starship-bar-20260923")
sys.path.insert(0, str(ROOT))
from lib_side_bell import paint_bells_on_layer  # noqa: E402

W, H = 424, 1200
PACK = Path("/workspace/lrt-cz8a-pack")
CROPS = ROOT / "crops"

# Measured from existing vehicle_cz8a_engine_booster.png
ENGINE_CY = 973
OFFSETS = [-142, -28, 28, 142]
BELL_W = 14
BELL_H = 48


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


def bottom_composite(shell_or_stack: Image.Image, engine: Image.Image) -> Image.Image:
    """Composite engine onto stack/shell for bottom proof (full canvas)."""
    base = shell_or_stack.copy().convert("RGBA")
    # Remove old nozzles from stack bottom by zeroing alpha where old eng was,
    # but for preview we composite NEW eng over a shell+stack without old eng:
    # Prefer shell (no engines) + NEW eng; if using stack, erase eng bay first.
    return Image.alpha_composite(base, engine)


def erase_bottom_engines(stack: Image.Image, y0: int = 960) -> Image.Image:
    out = stack.copy().convert("RGBA")
    px = out.load()
    for y in range(y0, H):
        for x in range(W):
            r, g, b, a = px[x, y]
            # Keep stage body (lighter / higher up solid) roughly; clear ovalish bells
            # Safer: clear anything below measured engine bay start on stack.
            if y >= y0:
                # Only clear if near bell vertical band (engines hang below body)
                if y >= ENGINE_CY - 2:
                    px[x, y] = (r, g, b, 0)
    return out


def label_bar(draw: ImageDraw.ImageDraw, box, text: str, font):
    x0, y0, x1, y1 = box
    draw.rectangle([x0, y0, x1, y1], fill=(0, 0, 0, 170))
    # center text
    bbox = draw.textbbox((0, 0), text, font=font)
    tw, th = bbox[2] - bbox[0], bbox[3] - bbox[1]
    tx = x0 + (x1 - x0 - tw) // 2
    ty = y0 + (y1 - y0 - th) // 2 - 1
    draw.text((tx, ty), text, fill=(255, 255, 255, 255), font=font)


def make_panel(img: Image.Image, crop_h: int = 220) -> Image.Image:
    """Bottom crop on dark bg for phone-readable compare cell."""
    ys = []
    a = img.split()[-1]
    # find content bottom
    import numpy as np

    arr = np.array(a)
    rows = (arr > 10).any(axis=1)
    if rows.any():
        y_max = int(rows.nonzero()[0].max())
    else:
        y_max = H - 1
    y0 = max(0, y_max - crop_h + 1)
    crop = img.crop((0, y0, W, min(H, y0 + crop_h)))
    # paste on black
    panel = Image.new("RGBA", (W, crop_h), (0, 0, 0, 255))
    panel.alpha_composite(crop, (0, 0))
    return panel.convert("RGB")


def main():
    ROOT.mkdir(parents=True, exist_ok=True)

    old_eng = Image.open(PACK / "vehicle_cz8a_engine_booster.png").convert("RGBA")
    stack = Image.open(PACK / "vehicle_cz8a_stack.png").convert("RGBA")
    shell = Image.open(PACK / "vehicle_cz8a_shell.png").convert("RGBA")

    # NEW engine layer — Starship side-profile bells, same placement/scale
    new_eng = paint_bells_on_layer(
        (W, H),
        ENGINE_CY,
        OFFSETS,
        BELL_W,
        BELL_H,
        color_outer=(128, 134, 142, 250),
        color_inner=(28, 30, 34, 230),
    )
    new_path = ROOT / "cz8a_engine_booster_NEW.png"
    new_eng.save(new_path)
    print("wrote", new_path, new_eng.size)

    # Bottom proofs: erase old eng from stack, composite OLD/NEW
    stack_clean = erase_bottom_engines(stack, y0=ENGINE_CY - 4)
    # Also try shell body bottom + eng (cleaner). Prefer stack body above engines.
    # Rebuild: body from stack above ENGINE_CY, engines from layer.
    body = stack.copy()
    bp = body.load()
    for y in range(ENGINE_CY - 2, H):
        for x in range(W):
            bp[x, y] = (0, 0, 0, 0)

    old_comp = Image.alpha_composite(body, old_eng)
    new_comp = Image.alpha_composite(body, new_eng)
    old_comp_path = ROOT / "cz8a_stack_bottom_OLD.png"
    new_comp_path = ROOT / "cz8a_stack_bottom_NEW.png"
    # save full composites too for inspection
    old_comp.save(ROOT / "cz8a_stack_with_OLD_eng.png")
    new_comp.save(ROOT / "cz8a_stack_with_NEW_eng.png")

    old_panel = make_panel(old_comp, 220)
    new_panel = make_panel(new_comp, 220)

    # Starship ref crop (already prepared)
    star_path = CROPS / "starship_side_bottom.png"
    if star_path.exists():
        star = Image.open(star_path).convert("RGB")
        # fit to panel height
        star = star.resize((W, 220), Image.Resampling.LANCZOS)
    else:
        star = Image.new("RGB", (W, 220), (20, 20, 20))

    # Side-by-side preview: STARSHIP REF | OLD | NEW
    gap = 8
    label_h = 36
    panel_h = 220
    cols = 3
    board_w = W * cols + gap * (cols + 1)
    board_h = label_h + panel_h + gap * 2 + 28
    board = Image.new("RGB", (board_w, board_h), (18, 18, 22))
    draw = ImageDraw.Draw(board, "RGBA")
    font = load_font(18)
    title_font = load_font(14)

    panels = [star, old_panel, new_panel]
    labels = ["STARSHIP REF", "OLD", "NEW"]
    for i, (panel, lab) in enumerate(zip(panels, labels)):
        x = gap + i * (W + gap)
        y = gap + label_h
        # label bar
        label_bar(draw, (x, gap, x + W, gap + label_h - 4), lab, font)
        board.paste(panel, (x, y))

    # footer note
    foot = "CZ-8A nozzle preview — HOLD fleet pending Chris Soft-PASS"
    fb = draw.textbbox((0, 0), foot, font=title_font)
    draw.text(
        ((board_w - (fb[2] - fb[0])) // 2, board_h - 22),
        foot,
        fill=(180, 185, 190),
        font=title_font,
    )

    preview_path = ROOT / "compare_cz8a_preview.png"
    board.save(preview_path, optimize=True)
    print("wrote", preview_path, board.size)

    # Also save bottom crop panels
    old_panel.save(old_comp_path)
    new_panel.save(new_comp_path)

    deliverables = [
        new_path,
        preview_path,
        old_comp_path,
        new_comp_path,
        ROOT / "lib_side_bell.py",
    ]

    # READY_PREVIEW.md + MD5SUMS
    sums = []
    lines = [
        "# CZ-8A Nozzle Preview — READY for Chris eyes",
        "",
        "**Scope:** preview ONLY (Helios GO UPDATE). Soft-FAIL Electron / F9 / fleet / commit / device / Studio / Play.",
        "",
        "**What changed:** `draw_side_bells` ellipse blobs → Starship-bar side-profile de Laval bells (narrow throat → expanding flare → foreshortened exit rim; thin cavity crescent; horizontal cooling ribs).",
        "",
        "**HOLD:** full nozzle rebuild across all rockets until Chris Soft-PASS / ❤️ on this direction.",
        "",
        "## Deliverables",
        "",
    ]
    md5_lines = []
    for p in deliverables:
        if not p.exists():
            continue
        digest = md5(p)
        sums.append((p, digest))
        rel = str(p)
        lines.append(f"- `{rel}`  md5 `{digest}`")
        md5_lines.append(f"{digest}  {p.name}")

    # include all PNGs in dir for checksum file
    pngs = sorted(ROOT.glob("*.png"))
    md5_all = []
    for p in pngs:
        md5_all.append(f"{md5(p)}  {p.name}")

    ready = ROOT / "READY_PREVIEW.md"
    ready.write_text("\n".join(lines) + "\n", encoding="utf-8")
    md5sums = ROOT / "MD5SUMS.txt"
    md5sums.write_text("\n".join(md5_all) + "\n", encoding="utf-8")

    notes = ROOT / "NOTES.md"
    notes.write_text(
        "CZ-8A preview: replaced ellipse/oval `draw_side_bells` (face-on propane-pill look) "
        "with `lib_side_bell.draw_side_profile_bell` — true side-profile de Laval silhouette "
        "(throat → flare → rim), metallic ribs, and a thin dark crescent at the exit only. "
        "Same placement/scale as old layer (offsets −142/−28/+28/+142, h=48). "
        "HOLD full fleet nozzle rebuild until Chris Soft-PASS on this direction. "
        "Soft-FAIL Electron/F9/commit/device.\n",
        encoding="utf-8",
    )

    print("READY", ready)
    print("MD5SUMS", md5sums)
    for p, d in sums:
        print(f"MD5 {d}  {p}")


if __name__ == "__main__":
    main()
