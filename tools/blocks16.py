"""
1.6.0 block art (Archaeology): the Buried Tell's soils, ash and mud brick (the suspicious ones in the four brushing
stages the game shows, the find appearing as the soil is dusted away), and the casings of the Bloomery Shaft and the
Photogrammetry Gantry, in the style of tools/blocks14.py and animated the same way.

Run:  python3 tools/blocks16.py [preview <png>]
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


def tell_soil(seed=1):
    img = canvas('#6a5038')
    noise(img, 0.05, seed)
    speckle(img, ['#4a3828', '#86684a', '#2a2018'], 30, seed)              # crumbs of charcoal and brick
    speckle(img, ['#a05a3a'], 5, seed + 9)
    return img


def ash_layer(seed=2):
    img = canvas('#6e6a66')
    noise(img, 0.05, seed)
    for y in (3, 9, 13):
        img[y, :, :3] = img[y, :, :3] * 0.6                               # fine laminae
    speckle(img, ['#2a2626', '#9a9490', '#1a1616'], 34, seed)
    return img


def mud_brick():
    img = canvas('#9a7a58')
    noise(img, 0.04, 5)
    for y in (0, 4, 8, 12):
        img[y, :, :3] = hexc('#6a5038')[:3]                               # mortar courses
    for row, off in enumerate((0, 4, 2, 6)):
        for x in range(off, 16, 8):
            img[row * 4 + 1:row * 4 + 4, x, :3] = hexc('#6a5038')[:3]
    speckle(img, ['#c8a878', '#7a5a3a'], 18, 5)                            # straw and grit
    return img


def find_icon(img, kind, reveal):
    """The find, showing more of itself at each brushing stage."""
    if reveal <= 0:
        return img
    m = np.zeros((16, 16), bool)
    y, x = np.mgrid[0:16, 0:16]
    if kind == 'sherd':
        shape = ((x - 8) ** 2 / 20 + (y - 8) ** 2 / 12 < 1)
        col, edge = '#b0603a', '#e8a070'
    else:
        shape = (abs((x - 8) - (y - 8)) < 3) & (abs(x - 8) < 6)
        col, edge = '#2a1608', '#8a5a30'
    m = shape & ((x + y * 0.7) < 6 + reveal * 7)                           # uncovered from one corner
    img[m] = hexc(col)
    img[m & (y < 8)] = hexc(edge)
    return img


def suspicious(base_fn, kind):
    out = []
    for k in range(4):
        img = base_fn(7 + k)
        if k == 0:
            speckle(img, ['#e0d0b0', '#3a2a1a'], 6, 40)                   # a faint hint something is there
        find_icon(img, kind, k / 3)
        out.append(img)
    return out


def fired_clay_shaft():
    img = canvas('#a8603a')
    noise(img, 0.05, 11)
    for y in (2, 7, 12):
        line(img, 0, y, 15, y + (1 if y == 7 else 0), '#7a4028')           # courses of daub
    speckle(img, ['#c87a50', '#5a2a18', '#d89060'], 22, 11)
    for x in (4, 11):
        line(img, x, 3, x + 1, 6, '#3a1a10')                              # firing cracks
    frame(img, '#5a2a18')
    return img


def tuyere_block():
    img = fired_clay_shaft()
    disc(img, 7.5, 7.5, 4.2, '#6a3420')
    disc(img, 7.5, 7.5, 3.0, '#3a1a10')
    disc(img, 7.5, 7.5, 1.8, '#ffb040')                                    # the glowing nozzle mouth
    disc(img, 7.5, 7.5, 0.9, '#fff0a0')
    return img


def bellows_block():
    img = canvas('#7a5030')
    for x in range(16):
        img[:, x, :3] *= 0.85 + 0.15 * math.cos(x / 15 * math.pi * 3) ** 2      # leather folds
    for y in (1, 14):
        rect(img, 0, y, 15, y, '#4a3018')
    rect(img, 0, 0, 15, 0, '#a08060'); rect(img, 0, 15, 15, 15, '#a08060')     # wooden boards
    rect(img, 6, 6, 9, 9, '#a08060'); px(img, 7, 7, '#2a1a10'); px(img, 8, 8, '#2a1a10')
    return bevel(img)


def survey_gantry_frame():
    img = canvas('#3a3e44')
    rect(img, 1, 1, 14, 14, '#2a2c30')
    line(img, 1, 1, 14, 14, '#c8b040'); line(img, 1, 14, 14, 1, '#c8b040')   # cross bracing
    for (x, y) in ((1, 1), (14, 1), (1, 14), (14, 14)):
        px(img, x, y, '#e0e0e0')
    frame(img, '#7a7e86', '#5a5e66')
    return img


def camera_rig():
    img = canvas('#2a2c30')
    rect(img, 1, 6, 14, 9, '#5a5e66')                                      # the slide bar
    for cx in (4, 11):
        disc(img, cx, 7.5, 2.6, '#101214')
        disc(img, cx, 7.5, 1.6, '#3060a0')
        px(img, cx - 1, 6, '#c0e0ff')                                      # lens glint
    px(img, 7, 3, '#ff4040')                                               # the recording lamp
    frame(img, '#1a1c20', '#3a3e44')
    return img


def datum_plinth():
    img = canvas('#a8a8a4')
    noise(img, 0.04, 21)
    speckle(img, ['#8a8a86', '#c0c0bc'], 20, 21)
    disc(img, 7.5, 7.5, 2.5, '#c8a040')                                    # the brass survey point
    disc(img, 7.5, 7.5, 1.0, '#6a5020')
    line(img, 7, 2, 8, 2, '#5a5a56'); line(img, 7, 13, 8, 13, '#5a5a56')
    frame(img, '#6a6a66')
    return bevel(img)


CASINGS = {'fired_clay_shaft': (fired_clay_shaft, None), 'tuyere_block': (tuyere_block, 3), 'bellows_block': (bellows_block, None),
           'survey_gantry_frame': (survey_gantry_frame, None), 'camera_rig': (camera_rig, 2), 'datum_plinth': (datum_plinth, None)}


def all_textures():
    out = {}
    save(tell_soil(), 'tell_soil'); out['tell_soil'] = tell_soil()
    save(ash_layer(), 'ash_layer'); out['ash_layer'] = ash_layer()
    save(mud_brick(), 'ancient_mud_brick'); out['ancient_mud_brick'] = mud_brick()
    for bid, fn, kind in (('suspicious_tell_soil', tell_soil, 'sherd'), ('suspicious_ash_layer', ash_layer, 'beam')):
        for k, img in enumerate(suspicious(fn, kind)):
            save(img, '%s_%d' % (bid, k))
            out['%s_%d' % (bid, k)] = img
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
    print('wrote the 1.6.0 block textures')
