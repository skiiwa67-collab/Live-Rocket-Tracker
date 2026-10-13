#!/usr/bin/env python3
"""tip152 Gravity-1 identity Soft-PASS + Stamp 92 fuel cutouts Soft-PASS.

Soft-FAIL invent liquid S4 / stack overwrite / Ada wire / ship / aspect squash /
opaque decorative hatch / hud copy until Helios Soft-PASS + Chris Soft-PASS.
"""
from __future__ import annotations
from pathlib import Path
from PIL import Image, ImageFilter
import hashlib, json, numpy as np

BEFORE = Path("/workspace/agent-backups/Da-Vinci/tip151-stage-fit-20260923/_before")
HUD = Path("/workspace/hud-assets")
PACK = Path("/workspace/agent-backups/Da-Vinci/tip152-gravity1-identity-20260923")
WORK = PACK / "_work"
CW, CH = 424, 1200
GOLD_SIDE = "c78f781a40a01676fdb57ae5343ba07b"
EXPECTED_BEFORE = {
    "s1": "0f147b073a3ab67aa289b71dfcbc5ecd",
    "s2": "faed4d90ba372c844fd14515e5cd5d18",
    "s3": "f265f0ada5eb1ec8bb6c5bec9424cfbc",
    "s4": "cae1ac2a7964548cb7d8c5f1a457d051",
}

def md5(p: Path) -> str:
    return hashlib.md5(Path(p).read_bytes()).hexdigest()

def punch_shell_windows(plate: Image.Image, shell: Image.Image, side: Image.Image) -> Image.Image:
    p = np.array(plate.convert("RGBA")).copy()
    s = np.array(shell.convert("RGBA"))
    sd = np.array(side.convert("RGBA"))
    win = (s[:, :, 3] < 32) & (sd[:, :, 3] > 200)
    plate_op = p[:, :, 3] > 40
    near = np.array(Image.fromarray((plate_op.astype(np.uint8) * 255)).filter(ImageFilter.MaxFilter(3))) > 0
    punch = win & near
    p[punch, 3] = 0
    shell_edge = (s[:, :, 3] >= 32) & (s[:, :, 3] < 220)
    near_punch = np.array(Image.fromarray((punch.astype(np.uint8) * 255)).filter(ImageFilter.MaxFilter(3))) > 0
    border = shell_edge & near_punch & near & (~punch)
    if border.any():
        for c in range(3):
            p[border, c] = s[border, c]
        p[border, 3] = np.maximum(p[border, 3], np.minimum(s[border, 3].astype(np.int16) + 40, 255).astype(np.uint8))
    return Image.fromarray(p, "RGBA")

def main() -> None:
    WORK.mkdir(parents=True, exist_ok=True)
    assert md5(HUD / "vehicle_gravity1_side.png") == GOLD_SIDE
    shell = Image.open(HUD / "vehicle_gravity1_shell.png").convert("RGBA")
    side = Image.open(HUD / "vehicle_gravity1_side.png").convert("RGBA")
    for s in ["s1", "s2", "s3"]:
        src = BEFORE / f"vehicle_gravity1_{s}.png"
        assert md5(src) == EXPECTED_BEFORE[s]
        plate = punch_shell_windows(Image.open(src).convert("RGBA"), shell, side)
        out = Image.new("RGBA", (CW, CH), (0, 0, 0, 0))
        out.paste(plate, (0, 0), plate)
        out.save(WORK / f"vehicle_gravity1_{s}.png", optimize=True)
        (PACK / f"vehicle_gravity1_{s}.png").write_bytes((WORK / f"vehicle_gravity1_{s}.png").read_bytes())
    # s4 alias of s3 Soft-FAIL invent liquid S4
    data = (PACK / "vehicle_gravity1_s3.png").read_bytes()
    (PACK / "vehicle_gravity1_s4.png").write_bytes(data)
    (WORK / "vehicle_gravity1_s4.png").write_bytes(data)
    lines = [f"{md5(PACK / f'vehicle_gravity1_{s}.png')}  vehicle_gravity1_{s}.png" for s in ["s1","s2","s3","s4"]]
    (PACK / "MD5SUMS").write_text("\n".join(lines) + "\n")
    assert md5(HUD / "vehicle_gravity1_side.png") == GOLD_SIDE
    print((PACK / "MD5SUMS").read_text())

if __name__ == "__main__":
    main()
