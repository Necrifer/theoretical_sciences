"""
1.13.0 block art (Patch C2, the station grows): the rectenna panel and the station modules, in the style of tools/blocks14.py and animated the same way.

Run:  python3 tools/blocks1130.py [preview <png>]
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


def rectenna_panel():
    """Rows of copper dipoles over a wire mesh, a dark diode at each feed."""
    img = canvas('#3a3e44')
    noise(img, 0.03, 41)
    for i in range(0, 16, 3):
        line(img, 0, i, 15, i, '#5a5e66')
        line(img, i, 0, i, 15, '#5a5e66')
    for y in (1, 7, 13):
        for x0 in (1, 9):
            line(img, x0, y, x0 + 5, y, '#d08a50')
            px(img, x0 + 3, y, '#202024')
    frame(img, '#2a2c30')
    return bevel(img)


def storm_shelter_module():
    """Blue water panels framed in white polyethylene, a hatch with a radiation sign."""
    img = canvas('#e8ebe6')
    noise(img, 0.02, 42)
    for x0, y0 in ((1, 1), (9, 1), (1, 9), (9, 9)):
        rect(img, x0, y0, x0 + 5, y0 + 5, '#3a78c0')
        line(img, x0 + 1, y0 + 1, x0 + 3, y0 + 1, '#8ac0f0')
    disc(img, 7.5, 7.5, 2.4, '#f0c020')
    px(img, 7, 7, '#202020'); px(img, 8, 8, '#202020'); px(img, 7, 8, '#202020')
    frame(img, '#a8aca6')
    return bevel(img)


def habitat_ring_module():
    """The hub of a spinning ring: spokes from a bright bearing to a rim with lit windows."""
    img = canvas('#c8ccd4')
    noise(img, 0.03, 43)
    for a in range(48):
        t = a / 48 * TAU
        px(img, int(round(7.5 + 6.5 * math.cos(t))), int(round(7.5 + 6.5 * math.sin(t))), '#7a7e86')
    for a in range(0, 48, 6):
        t = a / 48 * TAU
        px(img, int(round(7.5 + 6.5 * math.cos(t))), int(round(7.5 + 6.5 * math.sin(t))), '#ffe8a0')
    for a in range(4):
        t = a / 4 * TAU + 0.4
        for r in range(2, 6):
            px(img, int(round(7.5 + r * math.cos(t))), int(round(7.5 + r * math.sin(t))), '#8a8e96')
    disc(img, 7.5, 7.5, 1.6, '#80d0ff')
    return bevel(img)


CASINGS = {'rectenna_panel': (rectenna_panel, 1)}
SIMPLE = {}
GLOWING = {'storm_shelter_module': (storm_shelter_module, 1), 'habitat_ring_module': (habitat_ring_module, 2)}


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
    print('wrote the 1.13.0 block textures')
