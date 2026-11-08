"""
1.4.0 block art: the casings, consoles, world blocks and decorative blocks of Mind and Language and Engineering of the
Large. Each is drawn here pixel by pixel at 16 x 16 in the mod's style (a dark bevelled frame, flat shaded panels, lit
parts in the machine's colour) and animated the mod's way with tools/blocks13_anim.py: casings catch a passing sheen and
their lit parts pulse, formed casings carry the energy conduits, running consoles flicker with their readouts, and the
glowing world blocks breathe. 16 frames, two ticks each.

Run:  python3 tools/blocks14.py            (writes the textures)
      python3 tools/blocks14.py preview    (a contact sheet in the scratch folder only)
"""
import json, math, os, sys
import numpy as np
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import blocks13_anim as BA

TEX = os.path.join(HERE, '..', 'src/main/resources/assets/theoreticalsciences/textures/block')
N = 16
TAU = math.tau
rng = np.random.default_rng(1400)


def hexc(h, a=1.0):
    h = h.lstrip('#')
    return np.array([int(h[i:i + 2], 16) / 255 for i in (0, 2, 4)] + [a])


def canvas(col):
    img = np.zeros((N, N, 4))
    img[:] = hexc(col)
    return img


def noise(img, amt=0.05, seed=0):
    r = np.random.default_rng(seed).normal(0, amt, (N, N, 1))
    img[..., :3] = np.clip(img[..., :3] + r, 0, 1)
    return img


def rect(img, x0, y0, x1, y1, col):
    img[y0:y1 + 1, x0:x1 + 1] = hexc(col) if isinstance(col, str) else col
    return img


def px(img, x, y, col):
    if 0 <= x < N and 0 <= y < N:
        img[y, x] = hexc(col) if isinstance(col, str) else col


def bevel(img, light='#ffffff', dark='#000000', amt=0.18, width=1):
    """Lighter top and left edges, darker bottom and right: the mod's raised panel."""
    for k in range(width):
        img[k, :, :3] = img[k, :, :3] * (1 - amt) + hexc(light)[:3] * amt
        img[:, k, :3] = img[:, k, :3] * (1 - amt) + hexc(light)[:3] * amt
        img[N - 1 - k, :, :3] = img[N - 1 - k, :, :3] * (1 - amt) + hexc(dark)[:3] * amt
        img[:, N - 1 - k, :3] = img[:, N - 1 - k, :3] * (1 - amt) + hexc(dark)[:3] * amt
    return img


def frame(img, col, inner=None):
    """A one-pixel dark frame, and optionally a second ring inside it."""
    img[0, :] = img[-1, :] = img[:, 0] = img[:, -1] = hexc(col)
    if inner:
        img[1, 1:-1] = img[-2, 1:-1] = img[1:-1, 1] = img[1:-1, -2] = hexc(inner)
    return img


def line(img, x0, y0, x1, y1, col):
    n = int(max(abs(x1 - x0), abs(y1 - y0))) + 1
    for i in range(n):
        t = i / max(1, n - 1)
        px(img, int(round(x0 + (x1 - x0) * t)), int(round(y0 + (y1 - y0) * t)), col)


def disc(img, cx, cy, r, col, soft=False):
    y, x = np.mgrid[0:N, 0:N]
    d = np.hypot(x - cx, y - cy)
    if soft:
        a = np.clip(1 - d / r, 0, 1)[..., None]
        c = hexc(col)
        img[..., :3] = img[..., :3] * (1 - a) + c[:3] * a
    else:
        img[d <= r] = hexc(col)
    return img


def save(img, name):
    path = os.path.join(TEX, name + '.png')
    os.makedirs(os.path.dirname(path), exist_ok=True)
    for f in (path, path + '.mcmeta'):
        if os.path.exists(f):
            os.remove(f)
    Image.fromarray((np.clip(img, 0, 1) * 255).astype(np.uint8), 'RGBA').save(path)
    return path


def animated(name, base, fn, frames=16):
    path = save(base, name)
    BA.write([fn(base, f / frames) for f in range(frames)], path)


# ====================================================================== casings
def glial_scaffold():
    img = canvas('#8c809e')
    noise(img, 0.03, 1)
    for k in range(2, 14, 4):                                   # the polymer lattice
        img[k, 2:14] = hexc('#b8acc8')
        img[2:14, k] = hexc('#b8acc8')
    for x, y in ((4, 4), (8, 8), (12, 4), (4, 12), (12, 12), (8, 4), (4, 8)):
        px(img, x, y, '#efe2f6')                                  # glial nuclei where the struts cross
    frame(img, '#3c3448', '#5c5070')
    return bevel(img)


def axon_bundle():
    img = canvas('#5a4c62')
    for x in range(2, 14, 3):                                     # the tracts, white with myelin
        rect(img, x, 2, x + 1, 13, '#ece4dc')
        img[2:14, x + 1, :3] *= 0.85
        for y in range(4, 14, 5):                                  # nodes of Ranvier
            img[y, x:x + 2] = hexc('#e070c0')
    frame(img, '#2c2232', '#463a4e')
    return bevel(img)


def synapse_relay():
    img = canvas('#3a2a4c')
    noise(img, 0.02, 3)
    c = 7.5
    for a in range(8):                                             # dendrites to the edges
        t = a / 8 * TAU + 0.3
        line(img, c + math.cos(t) * 2, c + math.sin(t) * 2, c + math.cos(t) * 7, c + math.sin(t) * 7, '#b070e0')
    disc(img, c, c, 3.2, '#d080ff')
    disc(img, c, c, 1.8, '#ffe8ff')
    frame(img, '#1c1424', '#2c2038')
    return bevel(img)


def elevator_anchor():
    img = canvas('#7e8288')
    noise(img, 0.025, 4)
    rect(img, 1, 1, 14, 3, '#6a6e74')
    for x in range(1, 15):                                         # hazard band
        for y in (12, 13):
            px(img, x, y, '#e8c020' if ((x + y) // 2) % 2 == 0 else '#202020')
    for x, y in ((3, 6), (12, 6), (3, 9), (12, 9)):                  # bolts
        px(img, x, y, '#c8ccd0')
        px(img, x + 1, y + 1, '#4a4e54')
    rect(img, 6, 5, 9, 10, '#666a70')
    frame(img, '#34363a', '#50545a')
    return bevel(img, amt=0.25)


def tether_ribbon():
    img = canvas('#2a2c30')
    for x in range(4, 12):
        sh = 0.55 + 0.45 * math.sin((x - 4) / 7 * math.pi)
        img[1:15, x, :3] = hexc('#8a96a8')[:3] * sh + 0.05
    for y in range(2, 15, 3):
        img[y, 4:12, :3] *= 0.8
    img[1:15, 7:9] = hexc('#c8d8f0')
    frame(img, '#14161a')
    return img


def climber_car():
    img = canvas('#3a3c40')
    rect(img, 1, 1, 14, 2, '#d0a830'); rect(img, 1, 13, 14, 14, '#d0a830')     # I-beam cage
    rect(img, 1, 1, 2, 14, '#b89020'); rect(img, 13, 1, 14, 14, '#b89020')
    rect(img, 4, 4, 11, 8, '#203040')
    rect(img, 5, 5, 10, 7, '#60a0d8')                                         # the cabin window
    for x in range(3, 13, 2):                                                 # treads
        px(img, x, 10, '#1a1a1a'); px(img, x, 11, '#5a5a5a')
    return bevel(img, amt=0.22)


def habitat_hull():
    img = canvas('#7a8678')
    noise(img, 0.03, 5)
    img[7, 1:15] = hexc('#5a6458')
    img[1:15, 7] = hexc('#5a6458')
    for x, y in ((3, 3), (11, 3), (3, 11), (11, 11)):
        px(img, x, y, '#b0bcae')
    for x, y in ((5, 9), (9, 5), (12, 12)):
        px(img, x, y, '#6a5a44')                                                # grime
    frame(img, '#3a4238', '#56604f')
    return bevel(img)


def habitat_window():
    img = canvas('#5e6a5c')
    rect(img, 2, 2, 13, 13, '#9cd4f0')
    for y in range(2, 14):
        img[y, 2:14, :3] = img[y, 2:14, :3] * (0.75 + 0.25 * y / 13)
    line(img, 3, 12, 8, 3, '#e8f8ff')                                           # a highlight
    rect(img, 2, 11, 13, 13, '#7ab060')                                         # the land beyond, through the glass
    img[7, 2:14] = hexc('#4a5448')
    frame(img, '#2e362c', '#465044')
    return img


def spin_bearing():
    img = canvas('#5c6066')
    disc(img, 7.5, 7.5, 6.6, '#9ca2aa')
    disc(img, 7.5, 7.5, 5.0, '#40444a')
    for a in range(8):
        t = a / 8 * TAU
        px(img, int(round(7.5 + math.cos(t) * 5.8)), int(round(7.5 + math.sin(t) * 5.8)), '#e8ecf0')
    disc(img, 7.5, 7.5, 2.6, '#c8d0d8')
    disc(img, 7.5, 7.5, 1.0, '#ffd060')
    frame(img, '#2a2c30')
    return bevel(img)


def shkadov_mirror():
    img = canvas('#d8c890')
    y, x = np.mgrid[0:N, 0:N]
    g = (x + y) / 30
    img[..., :3] = np.stack([0.75 + 0.2 * g, 0.72 + 0.2 * g, 0.55 + 0.25 * g], -1)
    for k in range(-16, 16):
        line(img, k, 15, k + 15, 0, '#fff4c8') if k % 6 == 0 else None
    frame(img, '#6a5a30', '#a08848')
    return img


def caplan_jet():
    img = canvas('#2c2a2c')
    disc(img, 7.5, 7.5, 6.8, '#504a50')
    disc(img, 7.5, 7.5, 5.2, '#202022')
    disc(img, 7.5, 7.5, 4.2, '#ff7a20')
    disc(img, 7.5, 7.5, 2.8, '#ffc060')
    disc(img, 7.5, 7.5, 1.4, '#ffffe0')
    frame(img, '#141214')
    return img


CASINGS = {'glial_scaffold': (glial_scaffold, None), 'axon_bundle': (axon_bundle, 2), 'synapse_relay': (synapse_relay, 3),
           'elevator_anchor': (elevator_anchor, None), 'tether_ribbon': (tether_ribbon, 1), 'climber_car': (climber_car, 2),
           'habitat_hull': (habitat_hull, None), 'habitat_window': (habitat_window, 1), 'spin_bearing': (spin_bearing, 2),
           'shkadov_mirror': (shkadov_mirror, 1), 'caplan_jet': (caplan_jet, 3)}


# ====================================================================== world and decorative blocks
def grey_matter():
    img = canvas('#a08c94')
    noise(img, 0.04, 6)
    y, x = np.mgrid[0:N, 0:N]
    s = np.sin(x * 0.9 + np.sin(y * 0.7) * 2.2) + np.sin(y * 0.8 + 1.3)
    img[s > 1.35, :3] *= 0.72                                                   # the folds (sulci)
    for i in range(6):
        px(img, *rng.integers(0, N, 2), '#e0c8d8')
    return img


def white_matter():
    img = canvas('#e6ded2')
    for y in range(N):
        img[y, :, :3] *= 0.92 + 0.08 * math.sin(y * 1.7)
    noise(img, 0.025, 7)
    for i in range(5):
        y0 = int(rng.integers(0, N))
        img[y0, :, :3] *= 0.86
    return img


def neuron_soma():
    img = canvas('#7a4c90')
    disc(img, 7.5, 7.5, 9.0, '#c890e8', soft=True)
    disc(img, 7.5, 7.5, 5.0, '#f0c8ff', soft=True)
    disc(img, 8.5, 7.0, 2.4, '#5a3070')                                           # the nucleus
    px(img, 9, 6, '#e8a0ff')                                                      # its nucleolus
    return img


def glyph_rows(img, col, seed, rows=(3, 7, 11), bright=False):
    r = np.random.default_rng(seed)
    for y0 in rows:
        x = 2
        while x < 13:
            w = int(r.integers(2, 4))
            shape = r.integers(0, 2, (3, w))
            shape[r.integers(0, 3), r.integers(0, w)] = 1
            for dy in range(3):
                for dx in range(w):
                    if shape[dy, dx] and x + dx < 14:
                        px(img, x + dx, y0 + dy - 1, col)
            x += w + 1
    return img


def inscribed_glyph_stone():
    img = canvas('#30323a')
    noise(img, 0.025, 8)
    glyph_rows(img, '#5ae0d0', 14)
    frame(img, '#1c1d22', '#262830')
    return bevel(img, amt=0.12)


def glyph_stone_bricks():
    img = canvas('#3a3c44')
    noise(img, 0.03, 9)
    for y in (0, 4, 8, 12):
        img[y, :] = hexc('#22232a')
    for row, y in enumerate((0, 4, 8, 12)):
        off = 0 if row % 2 == 0 else 4
        for x in range(off, N, 8):
            img[y:y + 4, x % N] = hexc('#22232a')
    for cx, cy in ((3, 2), (11, 6), (7, 10), (14, 14), (2, 14)):                   # a glyph stamped on each brick
        px(img, cx, cy, '#5c6070'); px(img, cx + 1, cy, '#5c6070'); px(img, cx, cy + 1, '#5c6070')
    return img


def archive_shelf():
    img = canvas('#5a4a3a')
    rect(img, 0, 0, 15, 1, '#3a2e24'); rect(img, 0, 7, 15, 8, '#3a2e24'); rect(img, 0, 14, 15, 15, '#3a2e24')
    rect(img, 0, 0, 1, 15, '#3a2e24'); rect(img, 14, 0, 15, 15, '#3a2e24')
    r = np.random.default_rng(11)
    for y0, y1 in ((2, 6), (9, 13)):
        x = 2
        while x < 14:
            w = int(r.integers(1, 3))
            top = y0 + int(r.integers(0, 2))
            c = ['#8e8a80', '#a49c8c', '#6e6c66', '#b8ae96'][int(r.integers(0, 4))]
            rect(img, x, top, min(13, x + w - 1), y1, c)
            if r.random() < 0.4:
                px(img, x, top + 1, '#5ae0d0')                                       # a glyph glints on a spine
            x += w
    return img


def anode(stage):
    base = {'': '#b8c0c8', '_pitted': '#a0a4a8', '_spent': '#7c746c'}[stage]
    img = canvas(base)
    noise(img, 0.03 if stage == '' else 0.06, 12)
    y, x = np.mgrid[0:N, 0:N]
    img[..., :3] *= (0.92 + 0.12 * (1 - (x + y) / 30))[..., None]
    rect(img, 6, 0, 9, 2, '#d08040')                                              # the cable lug
    px(img, 7, 1, '#f0b070'); px(img, 8, 1, '#a05828')
    if stage:
        r = np.random.default_rng(13 if stage == '_pitted' else 14)
        n = 10 if stage == '_pitted' else 26
        for i in range(n):
            x0, y0 = r.integers(1, 15, 2)
            px(img, x0, y0, '#3e3a36')
            px(img, x0 + 1, y0, '#f0f0ea' if stage == '_pitted' else '#a0522a')       # white oxide, or rust once spent
    frame(img, '#5a6066' if stage != '_spent' else '#4a3e34')
    return bevel(img, amt=0.2)


# ====================================================================== consoles
SCREEN = '#0a0e14'


def console_frame(accent):
    img = canvas('#3a3e48')
    frame(img, '#1e2028', '#2c3038')
    rect(img, 2, 2, 13, 12, SCREEN)
    rect(img, 2, 13, 13, 13, '#4a4e58')
    for x in range(3, 13, 3):
        px(img, x, 14, accent)                                                     # status lamps
    return img


def front(mid, accent, draw):
    off = console_frame(accent)
    on = console_frame(accent)
    draw(on, accent, True)
    draw(off, accent, False)
    off[2:13, 2:14, :3] = off[2:13, 2:14, :3] * 0.45 + hexc(SCREEN)[:3] * 0.55   # dark and still when off
    return off, on


def scr_spikes(img, acc, on):
    for row, y in enumerate((4, 8, 11)):
        for x in range(2, 14):
            px(img, x, y, '#304050')
        for x in (4 + row * 2, 9 + row, 12 - row):
            line(img, x, y, x, y - 2, acc if on else '#405060')
    disc(img, 3.5, 3.5, 0.8, '#ffe0ff' if on else '#506070')


def scr_weights(img, acc, on):
    r = np.random.default_rng(21)
    for gy in range(4):
        for gx in range(4):
            v = r.random()
            c = hexc(acc)[:3] * (0.3 + 0.7 * v) if on else np.array([0.2, 0.25, 0.3]) * (0.5 + 0.5 * v)
            img[3 + gy * 2:4 + gy * 2 + 1, 3 + gx * 3:5 + gx * 3, :3] = c


def scr_roc(img, acc, on):
    line(img, 3, 12, 3, 3, '#405060'); line(img, 3, 12, 13, 12, '#405060')
    pts = [(3, 12), (4, 7), (5, 5), (7, 4), (10, 3), (13, 3)]
    for (a, b), (c, d) in zip(pts, pts[1:]):
        line(img, a, b, c, d, acc if on else '#405060')
    line(img, 3, 12, 12, 3, '#283440')


def scr_glyphs(img, acc, on):
    rect(img, 2, 2, 13, 12, '#2a2418' if on else SCREEN)
    glyph_rows(img, acc if on else '#405060', 31, rows=(4, 8, 12 - 1))


def scr_brain(img, acc, on):
    disc(img, 7.5, 7.5, 4.8, '#3a3058' if on else '#20242e')
    disc(img, 7.5, 7.5, 3.8, '#241e38' if on else '#181c24')
    line(img, 7, 3, 8, 12, '#3a3058' if on else '#20242e')
    for x, y in ((5, 6), (8, 5), (10, 7), (6, 9), (9, 10), (7, 7)):
        px(img, x, y, acc if on else '#405060')
    for (a, b), (c, d) in (((5, 6), (8, 5)), ((8, 5), (10, 7)), ((6, 9), (9, 10)), ((7, 7), (9, 10)), ((5, 6), (6, 9))):
        line(img, a, b, c, d, '#5a4a80' if on else '#304050')


def scr_beam(img, acc, on):
    for x in range(3, 13):
        y = 6 + int(round(1.8 * math.sin((x - 3) / 9 * math.pi)))
        px(img, x, y, acc if on else '#405060')
        px(img, x, y + 1, '#304050')
    rect(img, 2, 4, 2, 9, '#6a6e78'); rect(img, 13, 4, 13, 9, '#6a6e78')
    line(img, 4, 11, 11, 11, '#304050')
    px(img, 4 + (5 if on else 1), 11, acc)


def scr_weave(img, acc, on):
    for y in range(3, 12):
        for x in range(3, 13):
            if (x + y) % 2 == 0:
                px(img, x, y, acc if on else '#304050')
            elif (x - y) % 4 == 0:
                px(img, x, y, '#506070')


def scr_disc(img, acc, on):
    disc(img, 7.5, 7.5, 4.5, '#304050')
    disc(img, 7.5, 7.5, 3.0, '#203040')
    line(img, 7, 3, 11, 8, '#a0a8b8')
    px(img, 11, 8, acc if on else '#606878')


def scr_pourbaix(img, acc, on):
    rect(img, 2, 2, 13, 12, '#183048' if on else SCREEN)
    for x in range(2, 14):
        y = int(round(9 - (x - 2) * 0.35))
        img[y + 1:13, x, :3] = hexc('#305830' if on else '#1a2420')[:3]            # immunity, below
    rect(img, 8, 2, 11, 8, '#585030' if on else '#20201a')                         # the passive window
    line(img, 2, 5, 13, 9, acc if on else '#405060')


def scr_tether(img, acc, on):
    line(img, 7, 3, 7, 12, '#6a7a90'); line(img, 8, 3, 8, 12, '#6a7a90')
    rect(img, 5, 12, 10, 12, '#808890')
    rect(img, 6, 6, 9, 7, acc if on else '#405060')
    disc(img, 7.5, 2.8, 0.9, '#d0e0ff' if on else '#506070')


def scr_cylinder(img, acc, on):
    rect(img, 3, 5, 12, 10, '#507050')
    for y in (6, 9):
        img[y, 3:13] = hexc(acc if on else '#405060')
    rect(img, 2, 5, 2, 10, '#909890'); rect(img, 13, 5, 13, 10, '#909890')


def scr_star(img, acc, on):
    disc(img, 6, 7.5, 2.4, '#ffd070' if on else '#605030')
    for t in np.linspace(-1.1, 1.1, 9):
        px(img, int(round(6 + math.cos(t) * 5.5)), int(round(7.5 + math.sin(t) * 5.5)), '#e8e0b0' if on else '#504a38')
    line(img, 1, 7, 3, 7, acc if on else '#405060')


MACHINES = {'neural_culture_array': ('#e870c0', scr_spikes), 'synapse_trainer': ('#c080ff', scr_weights), 'psychophysics_booth': ('#f0d050', scr_roc),
            'decipherment_desk': ('#60e0d0', scr_glyphs), 'connectome_array': ('#c890ff', scr_brain), 'load_test_rig': ('#ff9040', scr_beam),
            'composite_layup_press': ('#8890a0', scr_weave), 'tribometer': ('#60c0ff', scr_disc), 'corrosion_cell': ('#e07040', scr_pourbaix),
            'space_elevator': ('#a0d0ff', scr_tether), 'oneill_cylinder': ('#80e0a0', scr_cylinder), 'stellar_engine': ('#ffb040', scr_star)}


def side(accent, seed):
    img = canvas('#4a4e58')
    noise(img, 0.02, seed)
    rect(img, 2, 6, 13, 9, '#3a3e48')
    img[7, 2:14] = hexc(accent)
    for x, y in ((3, 3), (12, 3), (3, 12), (12, 12)):
        px(img, x, y, '#8a8e98')
    frame(img, '#1e2028', '#2c3038')
    return bevel(img)


def top(accent, seed):
    img = canvas('#42464f')
    for y in range(3, 13, 2):
        img[y, 3:13] = hexc('#22252c')                                               # a vent grille
    px(img, 2, 2, accent); px(img, 13, 13, accent)
    frame(img, '#1e2028', '#2c3038')
    return bevel(img)


# ====================================================================== write
def all_textures():
    out = {}
    for cid, (fn, glow) in CASINGS.items():
        base = fn()
        out[cid] = base
        BA_g = glow
        animated(cid, base, (lambda b, t, g=BA_g: BA.pulse(BA.sheen(b, t), t, g)) if glow else (lambda b, t: BA.sheen(b, t)))
        animated(cid + '_formed', base, lambda b, t, g=BA_g: BA.conduits(BA.pulse(b, t, g) if g else b, t))
    save(grey_matter(), 'grey_matter')
    save(white_matter(), 'white_matter')
    animated('neuron_soma', neuron_soma(), lambda b, t: BA.pulse(b, t, 1, 0.5))
    animated('inscribed_glyph_stone', inscribed_glyph_stone(), lambda b, t: BA.pulse(b, t, 2, 0.6))
    save(glyph_stone_bricks(), 'glyph_stone_bricks')
    save(archive_shelf(), 'archive_shelf')
    for st in ('', '_pitted', '_spent'):
        save(anode(st), 'sacrificial_anode' + st)
    for i, (mid, (acc, draw)) in enumerate(MACHINES.items()):
        off, on = front(mid, acc, draw)
        save(off, 'machine/%s_front_off' % mid)
        animated('machine/%s_front_on' % mid, on, BA.readout)
        animated('machine/%s_side' % mid, side(acc, 40 + i), lambda b, t: BA.sheen(b, t, 0.15))
        animated('machine/%s_top' % mid, top(acc, 60 + i), lambda b, t: BA.sheen(b, t, 0.15))
        out[mid] = on
    return out


if __name__ == '__main__':
    out = all_textures()
    if len(sys.argv) > 1 and sys.argv[1] == 'preview':
        names = list(out)
        W = Image.new('RGBA', (8 * 72, ((len(names) + 7) // 8) * 72), (50, 50, 56, 255))
        for i, n in enumerate(names):
            im = Image.fromarray((np.clip(out[n], 0, 1) * 255).astype(np.uint8), 'RGBA').resize((64, 64), Image.NEAREST)
            W.alpha_composite(im, ((i % 8) * 72 + 4, (i // 8) * 72 + 4))
        W.save(sys.argv[2])
    print('wrote the 1.4.0 block textures')
