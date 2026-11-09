"""
1.5.0 block art (Small Wonders): the casings of the Quantum Computer and the CT Scanner, and the faces of the nine new
machines, in the same style as tools/blocks14.py (whose helpers this uses) and animated the same way.

Run:  python3 tools/blocks15.py [preview <png>]
"""
import math, os, sys
import numpy as np
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import blocks14 as B
import blocks13_anim as BA
from blocks14 import canvas, noise, rect, px, bevel, frame, line, disc, hexc, save, animated, front, side, top, glyph_rows, SCREEN

TAU = math.tau


def dilution_stage():
    img = canvas('#c89a40')
    y, x = np.mgrid[0:16, 0:16]
    img[..., :3] *= (0.85 + 0.25 * (1 - (x + y) / 30))[..., None]
    disc(img, 7.5, 7.5, 6.0, '#e0b860')
    disc(img, 7.5, 7.5, 2.0, '#8a6020')
    for a in range(6):
        t = a / 6 * TAU
        px(img, int(round(7.5 + math.cos(t) * 4.5)), int(round(7.5 + math.sin(t) * 4.5)), '#6a4810')     # the heat-exchange lines
    frame(img, '#5a4010', '#9a7428')
    return bevel(img)


def qubit_chandelier():
    img = canvas('#a07830')
    rect(img, 2, 2, 13, 13, '#2a2c34')
    for gx in range(3):
        for gy in range(3):
            x0, y0 = 3 + gx * 4, 3 + gy * 4
            rect(img, x0, y0, x0 + 1, y0 + 1, '#80e0ff')                                   # qubits
            px(img, x0 + 2, y0, '#c8a050'); px(img, x0, y0 + 2, '#c8a050')                    # couplers
    frame(img, '#5a4010', '#c8a050')
    return img


def cryostat_shell():
    img = canvas('#b8bcc4')
    y, x = np.mgrid[0:16, 0:16]
    img[..., :3] *= (0.8 + 0.3 * np.cos(x / 15 * math.pi - 0.6) ** 2)[..., None]                # polished, a highlight down it
    img[3, 1:15] = hexc('#8a8e96'); img[12, 1:15] = hexc('#8a8e96')
    for xx in (2, 6, 9, 13):
        px(img, xx, 3, '#e8ecf0'); px(img, xx, 12, '#e8ecf0')
    frame(img, '#5a5e66')
    return img


def gantry_ring():
    img = canvas('#e4e6ea')
    rect(img, 1, 6, 14, 9, '#9aa2ae')
    img[7, 1:15] = hexc('#ffd040')                                                            # the slip ring's track
    rect(img, 5, 2, 10, 4, '#3a3e48')                                                         # the tube housing
    px(img, 7, 3, '#ff8040'); px(img, 8, 3, '#ff8040')
    frame(img, '#7a808a', '#c4c8d0')
    return bevel(img)


def detector_array():
    img = canvas('#3a3e48')
    for gx in range(6):
        for gy in range(3):
            rect(img, 2 + gx * 2, 3 + gy * 4, 2 + gx * 2, 5 + gy * 4, '#a8d8f0')                # scintillator crystals
    frame(img, '#1e2028', '#2c3038')
    return img


def scanner_couch():
    img = canvas('#d8dade')
    rect(img, 1, 5, 14, 10, '#6a90b8')                                                        # the pad
    img[5, 1:15] = hexc('#8ab0d8')
    rect(img, 1, 11, 14, 14, '#9ca2aa')
    frame(img, '#6a6e76')
    return bevel(img)


CASINGS = {'dilution_stage': (dilution_stage, 1), 'qubit_chandelier': (qubit_chandelier, 3), 'cryostat_shell': (cryostat_shell, None),
           'gantry_ring': (gantry_ring, 2), 'detector_array': (detector_array, 4), 'scanner_couch': (scanner_couch, None)}


# ---------------------------------------------------------------- screens
def scr_wave(img, acc, on):
    for x in range(2, 14):
        y = 7 + int(round(3 * math.sin((x - 2) / 11 * TAU)))
        px(img, x, y, acc if on else '#405060')
        px(img, x, 14 - y, '#304050')
    line(img, 2, 7, 13, 7, '#203040')


def scr_sun(img, acc, on):
    disc(img, 4, 7.5, 2.6, '#ffd060' if on else '#605030')
    for t in np.linspace(-1.2, 1.2, 9):
        px(img, int(round(11 + math.cos(t + math.pi) * 3)), int(round(7.5 + math.sin(t + math.pi) * 4)), acc if on else '#405060')
    disc(img, 12, 7.5, 1.2, '#4080ff' if on else '#304050')


def scr_lorenz(img, acc, on):
    for i in range(80):
        t = i / 80 * TAU * 2
        x = 7.5 + 3.4 * math.sin(t) * (1 if i < 40 else -1) + (2 if i < 40 else -2)
        y = 7.5 + 3.2 * math.cos(t * 1.03)
        px(img, int(round(x)), int(round(y)), acc if on else '#405060')


def scr_knot(img, acc, on):
    for i in range(90):
        t = i / 90 * TAU
        x = 7.5 + 2.4 * (math.sin(t) + 2 * math.sin(2 * t)) / 1.4
        y = 7.5 + 2.4 * (math.cos(t) - 2 * math.cos(2 * t)) / 1.4
        px(img, int(round(x)), int(round(y)), acc if on else '#405060')


def scr_life(img, acc, on):
    glider = [(1, 0), (2, 1), (0, 2), (1, 2), (2, 2)]
    for ox, oy in ((3, 3), (8, 7), (4, 9)):
        for gx, gy in glider:
            px(img, ox + gx, oy + gy, acc if on else '#405060')
    for x in range(2, 14):
        for y in range(2, 13):
            if (x + y) % 4 == 0 and img[y, x, 1] < 0.2:
                px(img, x, y, '#14202a')


def scr_check(img, acc, on):
    for y in (4, 7, 10):
        line(img, 3, y, 9, y, '#405060')
        px(img, 11, y, acc if on else '#405060'); px(img, 12, y - 1, acc if on else '#405060'); px(img, 10, y - 1, acc if on else '#405060')


def scr_hill(img, acc, on):
    line(img, 3, 12, 3, 3, '#405060'); line(img, 3, 12, 13, 12, '#405060')
    for x in range(3, 14):
        d = (x - 3) / 10 * 3
        e = d ** 3 / (1 + d ** 3)
        px(img, x, int(round(12 - e * 8)), acc if on else '#405060')
    rect(img, 7, 3, 9, 11, '#283a2a' if on else SCREEN)
    for x in range(3, 14):
        d = (x - 3) / 10 * 3
        e = d ** 3 / (1 + d ** 3)
        px(img, x, int(round(12 - e * 8)), acc if on else '#405060')


def scr_qubits(img, acc, on):
    disc(img, 7.5, 7.5, 4.5, '#203040')
    disc(img, 7.5, 7.5, 3.6, SCREEN)
    line(img, 7.5, 7.5, 10, 4, acc if on else '#405060')
    px(img, 10, 4, '#ffffff' if on else '#506070')
    line(img, 3, 7, 12, 7, '#304050')


def scr_slice(img, acc, on):
    disc(img, 7.5, 7.5, 4.8, '#c0c8d0' if on else '#303840')
    disc(img, 7.5, 7.5, 3.8, '#6a7480' if on else '#20262c')
    disc(img, 6, 6.5, 1.2, '#e8eef4' if on else '#3a4048')
    disc(img, 9.5, 8.5, 0.9, acc if on else '#3a4048')


MACHINES = {'resonance_chamber': ('#90d0f0', scr_wave), 'heliograph': ('#70f0a0', scr_sun), 'chaos_controller': ('#ff7070', scr_lorenz),
            'knot_loom': ('#f0c060', scr_knot), 'automaton_lattice': ('#70f070', scr_life), 'certificate_verifier': ('#9090ff', scr_check),
            'dose_response_assay': ('#ff9090', scr_hill), 'quantum_computer': ('#80e0ff', scr_qubits), 'ct_scanner': ('#e0e8ff', scr_slice)}


def all_textures():
    out = {}
    for cid, (fn, glow) in CASINGS.items():
        base = fn()
        out[cid] = base
        animated(cid, base, (lambda b, t, g=glow: BA.pulse(BA.sheen(b, t), t, g)) if glow else (lambda b, t: BA.sheen(b, t)))
        animated(cid + '_formed', base, lambda b, t, g=glow: BA.conduits(BA.pulse(b, t, g) if g else b, t))
    # machine faces, sides and tops now come from tools/machine_art.py (the 1.5.1 art revamp)
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
    print('wrote the 1.5.0 block textures')
