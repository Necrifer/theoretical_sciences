"""
1.14.0 block art (Patch D1): the Active Magnetic Shield, in the style of tools/blocks14.py and animated the same way.

Run:  python3 tools/blocks1140.py [preview <png>]
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


def active_magnetic_shield():
    """A coil housing: windings round a glowing bore, frost on the cryostat."""
    img = canvas('#5a5e66')
    noise(img, 0.03, 51)
    for r, c in ((7.0, '#c87a3a'), (6.0, '#a8602a'), (5.0, '#c87a3a'), (4.0, '#a8602a')):
        for a in range(64):
            t = a / 64 * TAU
            px(img, int(round(7.5 + r * math.cos(t))), int(round(7.5 + r * math.sin(t))), c)
    disc(img, 7.5, 7.5, 2.6, '#2a2c34')
    disc(img, 7.5, 7.5, 1.4, '#80c8ff')
    for x, y in ((1, 1), (14, 1), (1, 14), (14, 14)):
        px(img, x, y, '#e8f4ff')
    return bevel(img)


CASINGS = {}
SIMPLE = {}
GLOWING = {'active_magnetic_shield': (active_magnetic_shield, 2)}


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
    print('wrote the 1.14.0 block textures')
