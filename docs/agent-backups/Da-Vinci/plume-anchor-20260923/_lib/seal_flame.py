#!/usr/bin/env python3
"""Shared Soft-PASS flame sealer: opaque top mates eng_bottom - SEAL_PX.

Soft-FAIL stack/body. Soft-FAIL bake into engine_*. KEEP engine_ship.
"""
from __future__ import annotations

import hashlib
import shutil
from pathlib import Path
from typing import Sequence, Tuple

import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageFont

THR = 8
SEAL_PX = 6
W, H = 424, 1200
BG = (28, 30, 38, 255)

# palette presets
PALETTES = {
    "hydrolox": {  # blue-white
        "outer": (140, 175, 230),
        "mid": (200, 225, 255),
        "core": (255, 255, 255),
    },
    "rp1": {  # orange-yellow
        "outer": (255, 140, 40),
        "mid": (255, 200, 70),
        "core": (255, 248, 210),
    },
    "methalox": {  # pale yellow-white
        "outer": (255, 170, 60),
        "mid": (255, 220, 120),
        "core": (255, 250, 220),
    },
}


def md5(p: Path) -> str:
    return hashlib.md5(p.read_bytes()).hexdigest()


def alpha_bbox(im: Image.Image, thr: int = THR):
    a = np.array(im.split()[-1])
    ys, xs = np.where(a > thr)
    if len(xs) == 0:
        return None
    return int(xs.min()), int(ys.min()), int(xs.max()), int(ys.max())


def bell_centers(eng: np.ndarray) -> list[float]:
    ys, xs = np.where(eng[:, :, 3] > THR)
    if len(ys) == 0:
        return [W / 2]
    y1 = int(ys.max())
    band = eng[max(0, y1 - 4) : y1 + 1, :, 3]
    col = (band > THR).any(axis=0)
    xs_on = np.where(col)[0]
    if len(xs_on) == 0:
        return [W / 2]
    centers = []
    start = int(xs_on[0])
    prev = int(xs_on[0])
    for x in xs_on[1:]:
        x = int(x)
        if x - prev > 3:
            centers.append((start + prev) / 2.0)
            start = x
        prev = x
    centers.append((start + prev) / 2.0)
    return centers


def paint_needle(draw, cx, y0, length, half_w, pal):
    o, m, c = pal["outer"], pal["mid"], pal["core"]
    for i in range(length + 1):
        t = i / max(1, length)
        y = y0 + i
        w = half_w * (1.0 - 0.92 * (t ** 1.15))
        if w < 0.4:
            w = 0.4 * (1.0 - t) + 0.15
        a_outer = int(200 * (1.0 - 0.55 * t) * (1.0 - 0.35 * t))
        a_mid = int(230 * (1.0 - 0.40 * t))
        a_core = int(255 * (1.0 - 0.25 * t))
        draw.ellipse([cx - w, y - 0.6, cx + w, y + 1.2], fill=(*o, max(0, min(255, a_outer))))
        wm = w * 0.62
        draw.ellipse([cx - wm, y - 0.5, cx + wm, y + 1.0], fill=(*m, max(0, min(255, a_mid))))
        wc = w * 0.32
        draw.ellipse([cx - wc, y - 0.4, cx + wc, y + 0.9], fill=(*c, max(0, min(255, a_core))))
    for dy in range(3):
        y = y0 + dy
        fade = 1.0 - dy * 0.08
        draw.ellipse(
            [cx - half_w * 0.95, y - 0.5, cx + half_w * 0.95, y + 1.2],
            fill=(*c, int(245 * fade)),
        )


def rebuild_flame(
    work: Path,
    vid: str,
    *,
    palette: str = "rp1",
    plume_len: int = 36,
    half_w: float = 7.5,
    seal_px: int = SEAL_PX,
    aliases: Sequence[str] = ("flame_s2", "exhaust_ship"),
) -> dict:
    work.mkdir(parents=True, exist_ok=True)
    hud = Path("/workspace/hud-assets")
    eng_src = hud / f"vehicle_{vid}_engine_ship.png"
    flame_old = hud / f"vehicle_{vid}_flame_ship.png"
    ship = hud / f"vehicle_{vid}_ship.png"
    stack = hud / f"vehicle_{vid}_stack.png"

    # snapshot body md5s
    before = {}
    for label, p in [("stack", stack), ("ship", ship), ("engine", eng_src)]:
        if p.exists():
            before[label] = md5(p)

    shutil.copy2(eng_src, work / f"vehicle_{vid}_engine_ship.png")
    if flame_old.exists():
        shutil.copy2(flame_old, work / f"vehicle_{vid}_flame_ship_OLD.png")
    if ship.exists():
        shutil.copy2(ship, work / f"vehicle_{vid}_ship.png")
    lib_src = Path("/workspace/agent-backups/Da-Vinci/nozzle-catalog-20260923/lib_side_bell.py")
    shutil.copy2(lib_src, work / "lib_side_bell.py")

    eng_im = Image.open(work / f"vehicle_{vid}_engine_ship.png").convert("RGBA")
    assert eng_im.size == (W, H), f"{vid} engine size {eng_im.size}"
    eng = np.array(eng_im)
    ebb = alpha_bbox(eng_im)
    assert ebb is not None and (eng[:, :, 3] > THR).sum() > 0, f"{vid} engine empty"
    eng_bottom = ebb[3]
    flame_top = eng_bottom - seal_px
    centers = bell_centers(eng)
    pal = PALETTES[palette]

    layer = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    draw = ImageDraw.Draw(layer)
    for cx in centers:
        paint_needle(draw, cx, flame_top, plume_len, half_w, pal)
    soft = layer.filter(ImageFilter.GaussianBlur(radius=0.6))
    soft_arr = np.array(soft)
    raw_arr = np.array(layer)
    keep = flame_top + seal_px + 2
    soft_arr[flame_top:keep] = raw_arr[flame_top:keep]
    soft_arr[:flame_top] = 0
    out = Image.fromarray(soft_arr, "RGBA")
    fbb = alpha_bbox(out)
    gap = fbb[1] - eng_bottom
    assert gap <= 0, f"{vid} floating gap {gap}"
    seal = eng_bottom - fbb[1]
    assert 2 <= seal <= 10, f"{vid} seal {seal}"
    plen = fbb[3] - fbb[1]
    assert plen <= 60, f"{vid} plume too long {plen}"

    out_path = work / f"vehicle_{vid}_flame_ship.png"
    out.save(out_path, optimize=True)
    for a in aliases:
        out.save(work / f"vehicle_{vid}_{a}.png", optimize=True)

    # compare + proof
    def comp(flame):
        canvas = Image.new("RGBA", (W, H), BG)
        if ship.exists():
            canvas.alpha_composite(Image.open(ship).convert("RGBA"))
        canvas.alpha_composite(eng_im)
        canvas.alpha_composite(flame)
        return canvas

    # crop around engine+flame
    x0 = max(0, ebb[0] - 30)
    x1 = min(W, ebb[2] + 30)
    y0 = max(0, ebb[1] - 40)
    y1 = min(H, max(fbb[3], ebb[3]) + 50)
    old_f = Image.open(work / f"vehicle_{vid}_flame_ship_OLD.png").convert("RGBA") if (work / f"vehicle_{vid}_flame_ship_OLD.png").exists() else Image.new("RGBA", (W, H))
    left = comp(old_f).crop((x0, y0, x1, y1))
    right = comp(out).crop((x0, y0, x1, y1))
    gap_w = 12
    label_h = 28
    board = Image.new("RGBA", (left.width * 2 + gap_w + 24, left.height + label_h + 24), (18, 20, 26, 255))
    drawb = ImageDraw.Draw(board)
    try:
        font = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf", 13)
        font_sm = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf", 10)
    except Exception:
        font = ImageFont.load_default()
        font_sm = font
    ox, oy = 12, label_h + 8
    board.paste(left, (ox, oy))
    board.paste(right, (ox + left.width + gap_w, oy))
    old_gap = None
    ob = alpha_bbox(old_f)
    if ob:
        old_gap = ob[1] - eng_bottom
    drawb.text((ox, 6), f"CURRENT gap={old_gap}", fill=(220, 180, 120, 255), font=font)
    drawb.text((ox + left.width + gap_w, 6), f"NEW seal gap={gap}", fill=(140, 220, 160, 255), font=font)
    drawb.text((ox, board.height - 16), f"{vid} post-sep Soft-FAIL stack overwrite", fill=(160, 160, 170, 255), font=font_sm)
    board.save(work / f"compare_{vid}_postsep.png", optimize=True)
    proof = right.resize((right.width * 2, right.height * 2), Image.Resampling.NEAREST)
    proof.save(work / f"_proof_{vid}_postsep.png", optimize=True)

    flame_md5 = md5(out_path)
    eng_md5 = md5(work / f"vehicle_{vid}_engine_ship.png")
    return {
        "vid": vid,
        "eng_bottom": eng_bottom,
        "flame_top": fbb[1],
        "gap": gap,
        "centers": centers,
        "flame_md5": flame_md5,
        "eng_md5": eng_md5,
        "before": before,
        "fbb": fbb,
        "ebb": ebb,
        "aliases": list(aliases),
        "palette": palette,
    }


def mirror_and_ready(work: Path, info: dict, pack_dirs: Sequence[Path], extra_notes: str = ""):
    vid = info["vid"]
    hud = Path("/workspace/hud-assets")
    # body Soft-FAIL check before
    stack = hud / f"vehicle_{vid}_stack.png"
    ship = hud / f"vehicle_{vid}_ship.png"
    cut = hud / f"vehicle_{vid}_cutaway.png"
    before_stack = md5(stack) if stack.exists() else None
    before_ship = md5(ship) if ship.exists() else None
    before_cut = md5(cut) if cut.exists() else None

    files = [f"vehicle_{vid}_flame_ship.png", f"vehicle_{vid}_engine_ship.png"]
    for a in info["aliases"]:
        files.append(f"vehicle_{vid}_{a}.png")

    for dest in pack_dirs:
        dest = Path(dest)
        dest.mkdir(parents=True, exist_ok=True)
        for f in files:
            src = work / f
            if src.exists():
                shutil.copy2(src, dest / f)

    after_stack = md5(stack) if stack.exists() else None
    after_ship = md5(ship) if ship.exists() else None
    after_cut = md5(cut) if cut.exists() else None
    assert before_stack == after_stack, "STACK OVERWRITTEN Soft-FAIL"
    assert before_ship == after_ship, "SHIP OVERWRITTEN Soft-FAIL"
    assert before_cut == after_cut, "CUTAWAY OVERWRITTEN Soft-FAIL"

    # MD5SUMS + READY
    sums = work / "MD5SUMS.txt"
    lines = [f"# {vid} plume-anchor Soft-PASS 2026-09-23", "# Soft-FAIL stack/cutaway/shell/side/tank"]
    for f in files + [f"compare_{vid}_postsep.png", f"_proof_{vid}_postsep.png", "lib_side_bell.py"]:
        p = work / f
        if p.exists():
            lines.append(f"{md5(p)}  {f}")
    if before_stack:
        lines.append(f"# stack Soft-FAIL unchanged {before_stack}")
    if before_ship:
        lines.append(f"# ship Soft-FAIL unchanged {before_ship}")
    sums.write_text("\n".join(lines) + "\n")

    ready = work / "READY.md"
    alias_rows = "\n".join(
        f"| `vehicle_{vid}_{a}.png` | `{info['flame_md5']}` | identical alias |"
        for a in info["aliases"]
    )
    ready.write_text(
        f"""# {vid.upper()} plume-anchor Soft-PASS — READY

**Family:** {vid}
**When:** 2026-09-23 CT
**Lane:** Da Vinci art only. Soft-FAIL Play/device/Ada code.

## Soft-PASS writes
| file | md5 | note |
|------|-----|------|
| `vehicle_{vid}_flame_ship.png` | `{info['flame_md5']}` | NEW — opaque top seals into engine exit |
{alias_rows}
| `vehicle_{vid}_engine_ship.png` | `{info['eng_md5']}` | KEEP Starship-bar (unchanged) |

## Geometry
- Canvas: 424×1200 transparent
- engine_ship bbox: {info['ebb']} exit y1={info['eng_bottom']}
- NEW flame bbox: {info['fbb']}
- **gap = {info['gap']}** (seal {info['eng_bottom']-info['flame_top']}px into bell)
- bell centers: {info['centers']}
- palette: {info['palette']}
- Soft-FAIL bake plume into engine_*

## Eyes
- `compare_{vid}_postsep.png`
- `_proof_{vid}_postsep.png`

## Mirrored to
{chr(10).join('- `' + str(d) + '`' for d in pack_dirs)}

## Soft-FAIL body proof (unchanged)
- stack: `{before_stack}`
- ship: `{before_ship}`
- cutaway: `{before_cut}`

## Soft-FAIL
- Soft-FAIL overwrite stack / cutaway / shell / side / tank masks
- Soft-FAIL Play / device / invent unpublished geometry
{extra_notes}

## Work dir
`{work}`
"""
    )
    return {
        "stack": before_stack,
        "ship": before_ship,
        "cutaway": before_cut,
    }


if __name__ == "__main__":
    import sys
    print("import seal_flame as library")
