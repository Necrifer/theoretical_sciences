"""
1.12.0 block art (Patch C1, getting there): the Propulsion Test Stand's casings, the shielding blocks, the Return Beacon
and the Mission Bay, in the style of tools/blocks14.py and animated the same way.

Run:  python3 tools/blocks1120.py [preview <png>]
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


def test_stand_frame():
    """Heavy steel lattice: an X brace in a square frame, rivets at the joints."""
    img = canvas('#2a2c30')
    noise(img, 0.03, 31)
    rect(img, 0, 0, 15, 2, '#6a6e76'); rect(img, 0, 13, 15, 15, '#6a6e76')
    rect(img, 0, 0, 2, 15, '#7a7e86'); rect(img, 13, 0, 15, 15, '#7a7e86')
    for i in range(16):
        for d in (0, 1):
            if 0 <= i + d < 16:
                px(img, i, min(15, i + d), '#8a8e96')
                px(img, 15 - i, min(15, i + d), '#5a5e66')
    for x, y in ((1, 1), (14, 1), (1, 14), (14, 14), (7, 7), (8, 8)):
        px(img, x, y, '#c0c4cc')
    rect(img, 0, 6, 15, 6, '#c8a030')                                       # a painted safety band
    return bevel(img)


def flame_trench_block():
    """Refractory concrete, scorched, with a strip of the curved steel deflector and the water curtain's nozzles."""
    img = canvas('#9a958c')
    noise(img, 0.05, 32)
    speckle(img, ['#7a756c', '#b8b4aa', '#5a554c'], 30, 32)
    for y in range(10, 16):
        for x in range(16):
            if (x * 7 + y * 3) % 5 < 3:
                px(img, x, y, '#3a3632')                                    # soot
    rect(img, 0, 0, 15, 2, '#6a6e76')
    for x in (2, 6, 10, 14):
        px(img, x, 3, '#80c8ff')                                            # the deluge nozzles
    frame(img, '#5a554c')
    return bevel(img)


def propellant_tank_block():
    """A cryogenic tank's insulated skin, frosted at the seams, with a level gauge."""
    img = canvas('#e0dcd0')
    noise(img, 0.03, 33)
    for y in (4, 11):
        line(img, 0, y, 15, y, '#a8a49a')
    speckle(img, ['#ffffff', '#e8f4ff'], 18, 33)                            # frost
    rect(img, 12, 3, 13, 12, '#3a3e46')
    rect(img, 12, 7, 13, 12, '#60d0ff')                                     # the level gauge, two thirds full
    rect(img, 2, 6, 6, 8, '#c87a3a')                                        # a stencilled band
    frame(img, '#8a867c')
    return bevel(img)


def polyethylene_shielding():
    """Slabs of milky white plastic, slightly translucent at the edges, stacked."""
    img = canvas('#e8ebe6')
    noise(img, 0.02, 34)
    for y in (5, 10):
        line(img, 0, y, 15, y, '#c4c8c2')
        line(img, 0, y + 1, 15, y + 1, '#f8faf6')
    speckle(img, ['#f4f6f2', '#d4d8d0'], 14, 34)
    frame(img, '#c0c4be')
    return bevel(img, amt=0.1)


def water_shield_tank():
    """Water behind glass in a steel frame."""
    img = canvas('#3a78c0')
    noise(img, 0.04, 35)
    for y in range(2, 14):
        for x in range(2, 14):
            if (x + y * 2) % 9 == 0:
                px(img, x, y, '#6aa8e8')
    rect(img, 0, 0, 15, 1, '#7a7e86'); rect(img, 0, 14, 15, 15, '#7a7e86')
    rect(img, 0, 0, 1, 15, '#6a6e76'); rect(img, 14, 0, 15, 15, '#6a6e76')
    line(img, 3, 3, 6, 3, '#c8e8ff')                                        # a glint on the glass
    return bevel(img)


def return_beacon():
    """A retroreflector array (corner cubes) in a ring, the radio's lamp at its centre."""
    img = canvas('#4a4e56')
    noise(img, 0.03, 36)
    for cx, cy in ((4, 4), (11, 4), (4, 11), (11, 11)):
        disc(img, cx, cy, 2.6, '#9aa8b8')
        px(img, cx, cy, '#e8f0ff'); px(img, cx - 1, cy, '#6a7888')
    disc(img, 7.5, 7.5, 1.8, '#ff6040')
    frame(img, '#2a2c30')
    return bevel(img)


def mission_bay():
    """A docking berth: hazard-striped edge round a dark hatch, a docking light."""
    img = canvas('#c8ccd4')
    noise(img, 0.03, 37)
    for i in range(16):
        c = '#e0b020' if (i // 2) % 2 == 0 else '#2a2a2a'
        px(img, i, 0, c); px(img, i, 15, c); px(img, 0, i, c); px(img, 15, i, c)
    rect(img, 3, 3, 12, 12, '#2a2e36')
    rect(img, 4, 4, 11, 11, '#3a404a')
    line(img, 4, 7, 11, 7, '#1a1e26'); line(img, 7, 4, 7, 11, '#1a1e26')
    disc(img, 7.5, 7.5, 1.2, '#60ff90')
    return bevel(img)


CASINGS = {'test_stand_frame': (test_stand_frame, None), 'flame_trench_block': (flame_trench_block, None),
           'propellant_tank_block': (propellant_tank_block, 2)}
SIMPLE = {'polyethylene_shielding': polyethylene_shielding, 'water_shield_tank': water_shield_tank}
GLOWING = {'return_beacon': (return_beacon, 2), 'mission_bay': (mission_bay, 1)}


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
    print('wrote the 1.12.0 block textures')
