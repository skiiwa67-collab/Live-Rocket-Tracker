#!/usr/bin/env python3
"""Falcon Heavy front and side. Three equal liquid cores. Side is not a copy of the front."""
import math, os
from PIL import Image, ImageDraw

OUT = "/workspace/agent-backups/Da-Vinci/fh-three-core-20260929"
W, H = 424, 1200
WHITE = (236, 237, 239, 255)
SHADE = (176, 178, 182, 255)
BLACK = (16, 16, 18, 255)
SKIRT = (20, 20, 22, 255)
BELL = (58, 60, 64, 255)
RIM = (150, 152, 156, 255)
FIN = (36, 38, 42, 255)

def canvas():
    return Image.new("RGBA", (W, H), (0, 0, 0, 0))

def ogive_points(cx, top, bot, half, steps=28):
    L = float(bot - top)
    r = float(half)
    R = (L * L + r * r) / (2.0 * r)
    right = []
    for i in range(steps + 1):
        dy = L * i / steps
        inside = R * R - (L - dy) ** 2
        x = math.sqrt(max(0.0, inside)) - (R - r)
        right.append((cx + x, top + dy))
    left = [(2 * cx - x, y) for x, y in reversed(right[:-1])]
    return right + left

def ogive(d, cx, top, bot, half, fill):
    d.polygon(ogive_points(cx, top, bot, half), fill=fill)

def column(d, x, y0, y1, w, fill):
    d.rectangle((x, y0, x + w, y1), fill=fill)

def bells_front(d, cx, y, half):
    """Nine Merlin bells in an octaweb, hanging under the skirt. y is the skirt bottom."""
    r = max(8.0, half * 0.30)
    ring = half * 0.58
    spots = [(0.0, r * 0.15)]
    for i in range(8):
        a = math.radians(-90 + i * 45)
        spots.append((math.cos(a) * ring, math.sin(a) * ring * 0.78 + r * 0.35))
    for dx, dy in spots:
        x, yy = cx + dx, y + dy
        d.ellipse((x - r, yy - r * 0.82, x + r, yy + r * 0.82), fill=BELL, outline=RIM)

def bells_side(d, cx, y, half, n=4):
    span = half * 1.55
    bw = span / n
    for i in range(n):
        x = cx - span / 2 + i * bw
        throat = bw * 0.22
        top = y + 1
        bot = y + 34
        d.polygon([
            (x + bw * 0.5 - throat, top),
            (x + bw * 0.5 + throat, top),
            (x + bw * 0.9, bot),
            (x + bw * 0.1, bot),
        ], fill=BELL)
        d.line([(x + bw * 0.5 - throat, top), (x + bw * 0.1, bot)], fill=RIM)
        d.line([(x + bw * 0.5 + throat, top), (x + bw * 0.9, bot)], fill=RIM)

def fins(d, x, w, y):
    d.rounded_rectangle((x - 16, y, x - 2, y + 14), radius=2, fill=FIN)
    d.rounded_rectangle((x + w + 2, y, x + w + 16, y + 14), radius=2, fill=FIN)

def side_core(d, x, w, fill, nose_top, body_top, base, show_fins=True):
    column(d, x, body_top, base - 30, w, fill)
    ogive(d, x + w / 2, nose_top, body_top + 2, w / 2, fill)
    d.rectangle((x, body_top, x + w, body_top + 8), fill=BLACK)
    d.rectangle((x, base - 34, x + w, base), fill=SKIRT)
    if show_fins:
        fins(d, x, w, body_top + 18)
    d.rectangle((x + w - 7, body_top + 34, x + w - 4, base - 48), fill=(70, 72, 76, 255))

def draw_front():
    im = canvas()
    d = ImageDraw.Draw(im)
    w = 104
    gap = 10
    total = w * 3 + gap * 2
    x0 = (W - total) // 2
    xs = [x0, x0 + w + gap, x0 + 2 * (w + gap)]
    base = 980
    body_top = 430
    nose_top = 330
    # sides first
    for x in (xs[0], xs[2]):
        side_core(d, x, w, WHITE, nose_top, body_top, base)
        bells_front(d, x + w / 2, base, w / 2 - 2)
    # center, same diameter, taller
    cx = xs[1]
    column(d, cx, 250, base, w, WHITE)
    d.rectangle((cx, 292, cx + w, 368), fill=BLACK)
    column(d, cx, 176, 292, w, WHITE)
    fw = int(w * 0.58)
    mid = cx + w / 2
    ogive(d, mid, 28, 188, fw, WHITE)
    # short boat-tail from the wider fairing down to the core
    d.polygon([
        (mid - fw, 168), (mid + fw, 168),
        (mid + w / 2, 210), (mid - w / 2, 210),
    ], fill=WHITE)
    bells_front(d, mid, base, w / 2 - 2)
    return im

def draw_side():
    im = canvas()
    d = ImageDraw.Draw(im)
    w = 108
    base = 980
    # far booster sticks out to the left so its nose and its own bells stay visible
    side_core(d, 36, w, SHADE, 348, 450, base, show_fins=False)
    bells_side(d, 36 + w / 2 - 6, base, w / 2 - 10, n=3)
    column(d, 118, 210, base - 30, w, WHITE)
    d.rectangle((118, base - 34, 118 + w, base), fill=SKIRT)
    d.rectangle((118, 292, 118 + w, 368), fill=BLACK)
    column(d, 118, 176, 292, w, WHITE)
    mid = 118 + w / 2
    fw = int(w * 0.58)
    ogive(d, mid, 28, 188, fw, WHITE)
    d.polygon([(mid - fw, 168), (mid + fw, 168), (mid + w / 2, 214), (mid - w / 2, 214)], fill=WHITE)
    bells_side(d, mid, base, w / 2 - 14, n=3)
    side_core(d, 210, w, WHITE, 336, 438, base, show_fins=False)
    d.polygon([(210 + w - 2, 468), (210 + w + 42, 456), (210 + w + 42, 484), (210 + w - 2, 494)], fill=FIN)
    bells_side(d, 210 + w / 2 + 4, base, w / 2 - 8, n=4)
    return im

def draw_one():
    im = canvas()
    d = ImageDraw.Draw(im)
    w = 130
    x = (W - w) // 2
    base = 1000
    side_core(d, x, w, WHITE, 250, 360, base)
    bells_front(d, x + w / 2, base, w / 2 - 4)
    return im

if __name__ == "__main__":
    front, side, one = draw_front(), draw_side(), draw_one()
    front.save(os.path.join(OUT, "vehicle_fh_stack.png"))
    front.save(os.path.join(OUT, "vehicle_fh.png"))
    front.save(os.path.join(OUT, "vehicle_fh_full.png"))
    side.save(os.path.join(OUT, "vehicle_fh_side.png"))
    one.save(os.path.join(OUT, "vehicle_fh_booster.png"))
    one.save(os.path.join(OUT, "vehicle_fh_s1.png"))
    one.save(os.path.join(OUT, "vehicle_fh_srb.png"))
    print("ok", os.path.getsize(os.path.join(OUT, "vehicle_fh_stack.png")))
