"""Starship-bar side-profile de Laval bell drawer (Pillow).

Replaces ellipse/blob draw_side_bells with true SIDE-PROFILE bells:
- Narrow throat under stage base
- Smooth expanding de Laval outer silhouette (polyline, not fat oval)
- Exit rim foreshortened ellipse at bottom
- Subtle metallic bands / cooling channels (horizontal ribs)
- Dark inner cavity only as a thin crescent inside the rim
"""
from __future__ import annotations

from typing import Iterable, Sequence, Tuple

from PIL import Image, ImageDraw

RGBA = Tuple[int, int, int, int]


def _clamp(v: int, lo: int, hi: int) -> int:
    return max(lo, min(hi, v))


def _de_laval_half_widths(bell_w: int, bell_h: int, n: int = 48) -> list[float]:
    """Half-width samples from throat (t=0) to exit (t=1).

    Starship-language taper: narrow throat, gentle flare, slight lip at exit.
    bell_w is half-width at exit (matches prior draw_side_bells convention).
    """
    throat = max(1.6, bell_w * 0.38)
    exit_w = float(bell_w)
    widths: list[float] = []
    for i in range(n + 1):
        t = i / n
        # Smoothstep + slight concave-to-convex de Laval feel
        s = t * t * (3.0 - 2.0 * t)
        # Early throat hold, then accelerate flare mid-bell
        flare = s ** 0.85
        w = throat + (exit_w - throat) * flare
        # Soft lip near exit (last ~12%)
        if t > 0.88:
            lip = (t - 0.88) / 0.12
            w += exit_w * 0.04 * lip
        widths.append(w)
    return widths


def draw_side_profile_bell(
    draw: ImageDraw.ImageDraw,
    cx: float,
    cy: float,
    bell_w: float,
    bell_h: float,
    *,
    color_outer: RGBA = (132, 138, 146, 250),
    color_mid: RGBA = (108, 114, 122, 248),
    color_shade: RGBA = (78, 84, 92, 245),
    color_highlight: RGBA = (188, 194, 202, 160),
    color_rib: RGBA = (155, 160, 168, 90),
    color_rim: RGBA = (170, 176, 184, 230),
    color_cavity: RGBA = (28, 30, 34, 230),
    color_mount: RGBA = (145, 150, 155, 240),
    ribs: bool = True,
) -> None:
    """Paint one side-profile de Laval bell.

    (cx, cy) = throat center top. bell_w = exit half-width. bell_h = full height.
    """
    bw = max(4, int(round(bell_w)))
    bh = max(10, int(round(bell_h)))
    n = max(24, bh)
    half_ws = _de_laval_half_widths(bw, bh, n=n)

    # Outer silhouette polygon (left edge down, right edge up)
    left = [(cx - half_ws[i], cy + (bh * i / n)) for i in range(n + 1)]
    right = [(cx + half_ws[i], cy + (bh * i / n)) for i in range(n, -1, -1)]
    poly = left + right
    draw.polygon(poly, fill=color_outer)

    # Right-side shade band (cylindrical volume)
    shade_pts = []
    for i in range(n + 1):
        y = cy + (bh * i / n)
        w = half_ws[i]
        shade_pts.append((cx + w * 0.18, y))
    for i in range(n, -1, -1):
        y = cy + (bh * i / n)
        w = half_ws[i]
        shade_pts.append((cx + w, y))
    if len(shade_pts) >= 3:
        draw.polygon(shade_pts, fill=color_shade)

    # Center highlight stripe
    hi_pts = []
    for i in range(n + 1):
        y = cy + (bh * i / n)
        w = half_ws[i]
        hi_pts.append((cx - max(0.8, w * 0.12), y))
    for i in range(n, -1, -1):
        y = cy + (bh * i / n)
        w = half_ws[i]
        hi_pts.append((cx + max(0.8, w * 0.10), y))
    if len(hi_pts) >= 3:
        draw.polygon(hi_pts, fill=color_highlight)

    # Mid tone body fill slightly inset (metallic body, hides any hole look)
    inset = []
    for i in range(2, n - 1):
        y = cy + (bh * i / n)
        w = max(1.0, half_ws[i] * 0.82)
        inset.append((cx - w, y))
    for i in range(n - 2, 1, -1):
        y = cy + (bh * i / n)
        w = max(1.0, half_ws[i] * 0.82)
        inset.append((cx + w, y))
    if len(inset) >= 3:
        draw.polygon(inset, fill=color_mid)

    # Horizontal cooling ribs / bands
    if ribs and bh >= 18:
        rib_n = max(3, bh // 7)
        for k in range(1, rib_n + 1):
            t = k / (rib_n + 1)
            i = int(round(t * n))
            i = _clamp(i, 2, n - 2)
            y = cy + (bh * i / n)
            w = half_ws[i] * 0.92
            y0 = int(round(y))
            draw.line(
                [(cx - w, y0), (cx + w, y0)],
                fill=color_rib,
                width=1,
            )

    # Exit rim: foreshortened thin ellipse (side view of circular exit)
    rim_y = cy + bh
    rim_half_h = max(1.5, bw * 0.18)
    rim_box = [
        cx - bw,
        rim_y - rim_half_h,
        cx + bw,
        rim_y + rim_half_h * 0.55,
    ]
    draw.ellipse(rim_box, outline=color_rim, width=1)
    # Soft rim face (thin metallic lip)
    draw.arc(
        [rim_box[0] + 1, rim_box[1], rim_box[2] - 1, rim_box[3]],
        200,
        340,
        fill=color_highlight,
        width=1,
    )

    # Dark inner cavity ONLY as thin crescent inside the rim — not a big oval blob
    cav_inset = max(1.2, bw * 0.22)
    cav_box = [
        cx - bw + cav_inset,
        rim_y - rim_half_h * 0.35,
        cx + bw - cav_inset,
        rim_y + rim_half_h * 0.35,
    ]
    # Lower crescent only (mask upper half conceptually by drawing flat chord fill)
    draw.chord(cav_box, 10, 170, fill=color_cavity)

    # Throat mount stub above bell
    tw = max(2, int(round(bw * 0.42)))
    draw.rectangle(
        [cx - tw, cy - 3, cx + tw, cy + 2],
        fill=color_mount,
    )
    # Tiny throat neck
    nw = max(1, int(round(bw * 0.30)))
    draw.polygon(
        [
            (cx - nw, cy),
            (cx + nw, cy),
            (cx + half_ws[max(1, n // 8)], cy + bh * 0.12),
            (cx - half_ws[max(1, n // 8)], cy + bh * 0.12),
        ],
        fill=color_outer,
    )


def draw_side_bells(
    img: Image.Image,
    cy: int,
    offsets: Sequence[float],
    bell_w: float,
    bell_h: float,
    color_outer: RGBA = (132, 138, 146, 250),
    color_inner: RGBA = (28, 30, 34, 230),
) -> Image.Image:
    """Drop-in replacement for blob ellipse draw_side_bells.

    color_inner is remapped to the thin rim crescent (not a filled oval).
    """
    out = img.copy()
    d = ImageDraw.Draw(out)
    cx = out.size[0] // 2
    for dx in offsets:
        draw_side_profile_bell(
            d,
            cx + dx,
            cy,
            bell_w,
            bell_h,
            color_outer=color_outer,
            color_cavity=color_inner,
        )
    return out


def paint_bells_on_layer(
    size: Tuple[int, int],
    cy: int,
    offsets: Iterable[float],
    bell_w: float,
    bell_h: float,
    **kwargs,
) -> Image.Image:
    layer = Image.new("RGBA", size, (0, 0, 0, 0))
    return draw_side_bells(layer, cy, list(offsets), bell_w, bell_h, **kwargs)
