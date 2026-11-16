"""
1.15.0 block art (Patch D2): the Bolometer Focal Plane, in the style of tools/blocks14.py and animated the same way.

Run:  python3 tools/blocks1150.py [preview <png>]
"""
import math, os, sys
import numpy as np
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import blocks13_anim as BA
from blocks14 import canvas, noise, rect, px, bevel, frame, line, disc, hexc, save, animated

TAU = math.tau


def speckle(img, cols, n, seed):
    rng = np.random.default_rng(seed)
    for _ in range(n):
        px(img, int(rng.integers(0, 16)), int(rng.integers(0, 16)), cols[int(rng.integers(0, len(cols)))])


def bolometer_focal_plane():
    """A hex-packed array of gold feedhorns on a silicon wafer, a sensor glinting in each."""
    img = canvas('#3a3e48')
    noise(img, 0.03, 61)
    for row in range(4):
        for col in range(4):
            cx = 2 + col * 4 + (2 if row % 2 else 0)
            cy = 2 + row * 4
            if cx > 14:
                continue
            disc(img, cx, cy, 1.6, '#c8a040')
            px(img, cx, cy, '#80d0ff')
    frame(img, '#202228')
    return bevel(img)


CASINGS = {'bolometer_focal_plane': (bolometer_focal_plane, 2)}
SIMPLE = {}
GLOWING = {}


def all_textures():
    out = {}
    for bid, fn in SIMPLE.items():
        out[bid] = fn()
        save(out[bid], bid)
    for bid, (fn, g) in GLOWING.items():
        out[bid] = fn()
        animated(bid, out[bid], lambda b, t, g=g: BA.pulse(b, t, g))
    for cid, (fn, glow) in CASINGS.items():
        base = fn()
        out[cid] = base
        animated(cid, base, (lambda b, t, g=glow: BA.pulse(BA.sheen(b, t), t, g)) if glow else (lambda b, t: BA.sheen(b, t)))
        animated(cid + '_formed', base, lambda b, t, g=glow: BA.conduits(BA.pulse(b, t, g) if g else b, t))
    return out


if __name__ == '__main__':
    out = all_textures()
    if len(sys.argv) > 2 and sys.argv[1] == 'preview':
        names = list(out)
        W = Image.new('RGBA', (8 * 72, ((len(names) + 7) // 8) * 72), (50, 50, 56, 255))
        for i, n in enumerate(names):
            im = Image.fromarray((np.clip(out[n], 0, 1) * 255).astype(np.uint8), 'RGBA').resize((64, 64), Image.NEAREST)
            W.alpha_composite(im, ((i % 8) * 72 + 4, (i // 8) * 72 + 4))
        W.save(sys.argv[2])
    print('wrote the 1.15.0 block textures')
