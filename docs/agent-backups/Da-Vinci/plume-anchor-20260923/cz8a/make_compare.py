#!/usr/bin/env python3
"""Compare board: LEFT=CURRENT (OLD flame) RIGHT=NEW flame, post-sep crop."""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont
import numpy as np

ROOT = Path(__file__).resolve().parent
W, H = 424, 1200
BG = (28, 30, 38, 255)

def load(p):
    return Image.open(p).convert("RGBA")

def composite(ship, eng, flame):
    canvas = Image.new("RGBA", (W, H), BG)
    canvas.alpha_composite(ship)
    canvas.alpha_composite(eng)
    canvas.alpha_composite(flame)
    return canvas

def tight_crop(im, pad=18):
    a = np.array(im.split()[-1])
    # also consider non-bg roughly via luminance difference — use alpha of layers only
    # crop around engine+flame region known
    return im.crop((150, 470, 280, 620))

ship = load(ROOT / "vehicle_cz8a_ship.png")
eng = load(ROOT / "vehicle_cz8a_engine_ship.png")
old_f = load(ROOT / "vehicle_cz8a_flame_ship_OLD.png")
new_f = load(ROOT / "vehicle_cz8a_flame_ship.png")

left = tight_crop(composite(ship, eng, old_f))
right = tight_crop(composite(ship, eng, new_f))

# side-by-side with labels
gap = 12
label_h = 28
board_w = left.width * 2 + gap + 24
board_h = left.height + label_h + 24
board = Image.new("RGBA", (board_w, board_h), (18, 20, 26, 255))
draw = ImageDraw.Draw(board)
try:
    font = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf", 14)
    font_sm = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf", 11)
except Exception:
    font = ImageFont.load_default()
    font_sm = font

ox, oy = 12, label_h + 8
board.paste(left, (ox, oy))
board.paste(right, (ox + left.width + gap, oy))
draw.text((ox, 6), "CURRENT (OLD flame) gap=-14", fill=(220, 180, 120, 255), font=font)
draw.text((ox + left.width + gap, 6), "NEW flame seal gap=-6", fill=(140, 220, 160, 255), font=font)
draw.text((ox, board_h - 16), "cz8a post-sep ship+engine+flame  Soft-FAIL stack overwrite", fill=(160, 160, 170, 255), font=font_sm)

board.save(ROOT / "compare_cz8a_postsep.png", optimize=True)

# proof NEW alone (ship+eng+new flame crop, larger)
proof = tight_crop(composite(ship, eng, new_f))
# upscale 2x for eyes
proof2 = proof.resize((proof.width * 2, proof.height * 2), Image.Resampling.NEAREST)
proof2.save(ROOT / "_proof_cz8a_postsep.png", optimize=True)
print("wrote compare + proof")
