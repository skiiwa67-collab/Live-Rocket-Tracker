#!/usr/bin/env python3
"""Rebuild CZ-8A ship/S2 flame: opaque top seals into Starship-bar engine exit.

Soft-PASS: flame_ship / flame_s2 / exhaust_ship only.
Soft-FAIL: stack / cutaway / shell / side / tanks / bake plume into engine_*.
KEEP engine_ship as-is (catalog Starship-bar md5 6f9b1caf...).
"""
from __future__ import annotations

import hashlib
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

ROOT = Path(__file__).resolve().parent
W, H = 424, 1200
ENG_PATH = ROOT / "vehicle_cz8a_engine_ship.png"
OUT = ROOT / "vehicle_cz8a_flame_ship.png"

# Target: seal 2..8 px into bell → flame_top = eng_bottom - SEAL
SEAL_PX = 6
PLUME_LEN = 36  # short/modest (workflow)
HALF_W0 = 7.5   # half-width at exit lip
THR = 8


def alpha_bbox(im: Image.Image, thr: int = THR):
    a = np.array(im.split()[-1])
    ys, xs = np.where(a > thr)
    if len(xs) == 0:
        return None
    return int(xs.min()), int(ys.min()), int(xs.max()), int(ys.max())


def bell_centers(eng: np.ndarray) -> list[float]:
    """Infer twin-bell centers from engine alpha near exit."""
    ys, xs = np.where(eng[:, :, 3] > THR)
    y1 = int(ys.max())
    # sample a band near exit
    band = eng[max(0, y1 - 4) : y1 + 1, :, 3]
    col = (band > THR).any(axis=0)
    xs_on = np.where(col)[0]
    if len(xs_on) == 0:
        return [212.0]
    # split on gap
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


def paint_needle(draw: ImageDraw.ImageDraw, cx: float, y0: float, length: int, half_w: float):
    """Short conical hydrolox needle: white core, blue sheath, soft tip."""
    # outer soft sheath (blue)
    for i in range(length + 1):
        t = i / max(1, length)
        y = y0 + i
        # taper → point
        w = half_w * (1.0 - 0.92 * (t ** 1.15))
        if w < 0.4:
            w = 0.4 * (1.0 - t) + 0.15
        # alpha fades toward tip
        a_outer = int(200 * (1.0 - 0.55 * t) * (1.0 - 0.35 * t))
        a_mid = int(230 * (1.0 - 0.40 * t))
        a_core = int(255 * (1.0 - 0.25 * t))
        # blue sheath
        draw.ellipse(
            [cx - w, y - 0.6, cx + w, y + 1.2],
            fill=(140, 175, 230, max(0, min(255, a_outer))),
        )
        # mid cyan-white
        wm = w * 0.62
        draw.ellipse(
            [cx - wm, y - 0.5, cx + wm, y + 1.0],
            fill=(200, 225, 255, max(0, min(255, a_mid))),
        )
        # hot white core
        wc = w * 0.32
        draw.ellipse(
            [cx - wc, y - 0.4, cx + wc, y + 0.9],
            fill=(255, 255, 255, max(0, min(255, a_core))),
        )
    # opaque seal lip at top (flush into bell) — solid bar so bbox top is firm
    lip_h = 3
    for dy in range(lip_h):
        y = y0 + dy
        fade = 1.0 - dy * 0.08
        draw.ellipse(
            [cx - half_w * 0.95, y - 0.5, cx + half_w * 0.95, y + 1.2],
            fill=(255, 255, 255, int(245 * fade)),
        )


def main():
    eng_im = Image.open(ENG_PATH).convert("RGBA")
    eng = np.array(eng_im)
    ebb = alpha_bbox(eng_im)
    assert ebb is not None, "engine_ship empty"
    eng_bottom = ebb[3]
    flame_top = eng_bottom - SEAL_PX
    centers = bell_centers(eng)
    print(f"engine bbox={ebb} bottom={eng_bottom} centers={centers}")
    print(f"flame_top={flame_top} (seal={SEAL_PX}) len={PLUME_LEN}")

    canvas = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    # paint into a temp layer then light blur for soft edges (keep top firm)
    layer = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    draw = ImageDraw.Draw(layer)
    for cx in centers:
        paint_needle(draw, cx, flame_top, PLUME_LEN, HALF_W0)

    # very light blur but restore firm top seal rows
    soft = layer.filter(ImageFilter.GaussianBlur(radius=0.6))
    soft_arr = np.array(soft)
    raw_arr = np.array(layer)
    # keep top SEAL_PX+2 rows from raw (opaque flush into bell)
    keep = flame_top + SEAL_PX + 2
    soft_arr[flame_top:keep] = raw_arr[flame_top:keep]
    # where raw was zero, keep soft zero (no bloom above flame_top)
    soft_arr[:flame_top] = 0

    out = Image.fromarray(soft_arr, "RGBA")
    fbb = alpha_bbox(out)
    gap = fbb[1] - eng_bottom
    print(f"NEW flame bbox={fbb} gap={gap} (want <=0, seal 2..8)")
    assert fbb is not None
    assert gap <= 0, f"floating gap {gap}"
    seal = eng_bottom - fbb[1]
    assert 2 <= seal <= 10, f"seal {seal} out of preferred 2..8 (tol 10)"
    # modest length
    plen = fbb[3] - fbb[1]
    assert plen <= 55, f"plume too long {plen}"

    out.save(OUT, optimize=True)
    # aliases
    for alias in ("vehicle_cz8a_flame_s2.png", "vehicle_cz8a_exhaust_ship.png"):
        out.save(ROOT / alias, optimize=True)

    md5 = hashlib.md5(OUT.read_bytes()).hexdigest()
    print(f"wrote {OUT.name} md5={md5}")


if __name__ == "__main__":
    main()
