"""Round-11 item art, group 1: clocks, code, codices, coils, containment, crystals, dampers, quarks, emitters, gems.
Every item has its own model and its own motion, built from its name and lore."""
import math
import numpy as np
from items3d import *
import anim

# ------------------------------------------------------------------------------------------------ helpers


def per(t, k=1):
    """0 -> 1 -> 0 once per loop (k times)."""
    return 0.5 - 0.5 * math.cos(TAU * t * k)


def sstep(x):
    x = min(1.0, max(0.0, x))
    return x * x * (3 - 2 * x)


def wrap(a):
    return (a + math.pi) % TAU - math.pi


def smin(a, b, k):
    h = np.clip(0.5 + 0.5 * (b - a) / k, 0, 1)
    return b * (1 - h) + a * h - k * h * (1 - h)


def ring_xy(p, R, r):
    """Torus lying in the xy plane (facing the camera)."""
    return np.sqrt((np.sqrt(p[..., 0] ** 2 + p[..., 1] ** 2) - R) ** 2 + p[..., 2] ** 2) - r


def hsh(*a):
    v = 0
    for i, x in enumerate(a):
        v = v + x * (12.9898, 78.233, 37.719, 19.19)[i % 4]
    v = np.sin(v) * 43758.5453
    return v - np.floor(v)


def black():
    m = Material('#000000', '#000000', metal=0.0, gloss=0.0)
    m.ramp[:] = np.array([0.0, 0.0, 0.02])
    return m


def glow(col):
    return Material(col, '#ffffff', emissive=col, pattern=pat_all())


def C(c):
    return H(c) if isinstance(c, str) else np.asarray(c, float)


def proj(P, view):
    """World point -> 32px image coordinates for a builder's view (no view motions applied)."""
    cam = rotx(-view.get('view_pitch', 0.5)) @ roty(view.get('view_yaw', 0.7853))
    q = np.asarray(P, float) @ cam.T
    k = view.get('zoom', 1.0) / view.get('extent', 1.0)
    return (q[0] * k + 1) / 2 * 32 - 0.5, (1 - q[1] * k) / 2 * 32 - 0.5


def px(img, x, y, rgb, a=1.0):
    anim._blend(img, int(round(y)), int(round(x)), np.asarray(rgb, float), float(min(1.0, max(0.0, a))))


def line(img, x0, y0, x1, y1, rgb, a=1.0):
    n = int(max(abs(x1 - x0), abs(y1 - y0))) + 1
    for i in range(n + 1):
        s = i / max(n, 1)
        px(img, x0 + (x1 - x0) * s, y0 + (y1 - y0) * s, rgb, a)


def only_empty(img, x, y, rgb, a):
    xi, yi = int(round(x)), int(round(y))
    if 0 <= xi < 32 and 0 <= yi < 32 and img[yi, xi, 3] < 0.5:
        px(img, xi, yi, rgb, a)


def helix(p, R, pitch, r, h):
    """Helix around the local y axis."""
    a = np.arctan2(p[..., 2], p[..., 0])
    rho = np.sqrt(p[..., 0] ** 2 + p[..., 2] ** 2)
    u = p[..., 1] - pitch * a / TAU
    u = np.mod(u + pitch / 2, pitch) - pitch / 2
    d = np.sqrt((rho - R) ** 2 + u ** 2) * 0.8 - r
    return np.maximum(d, np.abs(p[..., 1]) - h)


def gem_s(p, s, r=0.36, h=0.98, waist=0.35):
    return sd_gem(p / s, r, h, waist) * s


# ================================================================================================ clocks
def chronal_anchor(c1, c2, t):
    # a ship's anchor with a pocket watch for a crown: its hands never move, while dust races round it
    def arc(p):
        q = p - np.array([0, -0.12, 0])
        return np.maximum(ring_xy(q, 0.46, 0.07), q[..., 1] + 0.05)
    def hands(p, n):
        x, y = p[..., 0], -p[..., 2]
        a = np.arctan2(y, x)
        r = np.sqrt(x * x + y * y)
        h1 = (np.abs(wrap_np(a - 2.6)) < 0.3) & (r < 0.16)
        h2 = (np.abs(wrap_np(a - 1.05)) < 0.22) & (r < 0.24)
        ticks = (r > 0.22) & (r < 0.27) & (np.abs(wrap_np(a * 4)) < 0.5)
        return np.zeros(r.shape), (h1 | h2 | ticks | (r < 0.04)).astype(float)
    parts = [Part(lambda p: sd_capsule(p, (0, -0.62, 0), (0, 0.32, 0), 0.075), 0),
             Part(lambda p: sd_capsule(p, (-0.32, 0.16, 0), (0.32, 0.16, 0), 0.06), 0),
             Part(arc, 0),
             Part(lambda p: sd_octa(p, 0.15), 0, pos=(-0.47, -0.08, 0), rot=rotz(0.5)),
             Part(lambda p: sd_octa(p, 0.15), 0, pos=(0.47, -0.08, 0), rot=rotz(-0.5)),
             Part(lambda p: sd_cyl(p, 0.31, 0.05, 0.02), 2, pos=(0, 0.6, -0.03), rot=rotx(TAU / 4)),
             Part(lambda p: sd_torus(p, 0.32, 0.05), 1, pos=(0, 0.6, -0.03), rot=rotx(TAU / 4)),
             Part(lambda p: sd_torus(p, 0.07, 0.035), 1, pos=(0, 0.97, 0), rot=rotx(TAU / 4))]
    mats = [Material('#5a5048', c2, metal=0.8), Material(c2, '#ffffff', metal=1.0),
            Material('#f0e0b8', c2, metal=0.2, emissive='#3a2008', pattern=hands)]
    view = dict(view_pitch=0.15, view_yaw=0.15, zoom=0.95)

    def ov(img):
        # outside the anchor time runs at three different speeds
        for k, (spd, rx, ry, col) in enumerate(((3, 14, 5, C(c2)), (-2, 12, 9, WHITE), (5, 10, 3, C(c2)))):
            for i in range(2):
                a = TAU * (t * spd + i / 2 + k / 5)
                x, y = 15.5 + math.cos(a) * rx, 16 + math.sin(a) * ry
                for j in range(3):
                    aa = a - j * 0.18 * (1 if spd > 0 else -1)
                    only_empty(img, 15.5 + math.cos(aa) * rx, 16 + math.sin(aa) * ry, col, 0.9 - j * 0.3)
    return parts, mats, view, ov


def wrap_np(a):
    return np.mod(a + math.pi, TAU) - math.pi


def chronon_capacitor(c1, c2, t):
    # a glass cell filling with discrete quanta of time, one click at a time, with a carrying handle.
    # Loop: eight clicks fill it (t < 0.75), it holds full, then discharges click by click (t > 0.8) back to empty.
    filling = t < 0.75
    if filling:
        n_q = int(t / 0.75 * 8)
    elif t < 0.8:
        n_q = 8
    else:
        n_q = 7 - int((t - 0.8) / 0.2 * 8)

    def quanta(p, n):
        y = p[..., 1] + 0.5
        k = np.floor(y / 0.14)
        fr = y / 0.14 - k
        side = np.abs(n[..., 1]) < 0.7
        lit = (k < n_q) & (fr > 0.18) & (fr < 0.82) & (k >= 0)
        rim = (k == n_q) & (fr > 0.3) & (fr < 0.7) & ((t / 0.75 * 8) % 1 < 0.5) & filling
        return np.where(side, -0.15, 0.0), ((lit | rim) & side).astype(float)
    parts = [Part(lambda p: sd_cyl(p, 0.34, 0.5, 0.03), 0),
             Part(lambda p: sd_cyl(p, 0.42, 0.08, 0.03), 1, pos=(0, 0.56, 0)),
             Part(lambda p: sd_cyl(p, 0.42, 0.08, 0.03), 1, pos=(0, -0.56, 0)),
             Part(lambda p: np.maximum(ring_xy(p, 0.3, 0.05), -p[..., 1]), 1, pos=(0, 0.64, 0)),
             Part(lambda p: sd_cyl(p, 0.07, 0.07), 2, pos=(-0.2, -0.7, 0.0)),
             Part(lambda p: sd_cyl(p, 0.07, 0.07), 2, pos=(0.2, -0.7, 0.0))]
    mats = [Material('#3a3020', c2, metal=0.9, gloss=1.5, emissive=c2, pattern=quanta),
            Material('#a07828', c2, metal=1.0), Material('#d0d4dc', '#ffffff', metal=1.0)]
    view = dict(view_pitch=0.3, view_yaw=0.6, zoom=0.98)

    def ov(img):
        if filling and (t / 0.75 * 8) % 1 < 0.2:
            x, y = proj((0.34, -0.5 + n_q * 0.14 + 0.07, 0), view)
            sparkle(img, int(x / 2), int(y / 2), C(c2))
    return parts, mats, view, ov


# ================================================================================================ code
def feynman_diagram(c1, c2, t):
    # ink on paper: two particles meet, a wavy photon carries the interaction, two particles leave
    A, B = np.array([-0.26, 0.0, -0.06]), np.array([0.26, 0.0, -0.06])
    ends_in = [np.array([-0.68, -0.55, -0.06]), np.array([-0.68, 0.55, -0.06])]
    ends_out = [np.array([0.68, -0.55, -0.06]), np.array([0.68, 0.55, -0.06])]

    def photon(p):
        x = p[..., 0]
        xc = np.clip(x, -0.26, 0.26)
        w = 0.09 * np.sin(xc * 30 - t * TAU * 2)
        return np.sqrt((x - xc) ** 2 + ((p[..., 1] - w) * 0.55) ** 2 + (p[..., 2] + 0.06) ** 2) - 0.035

    def pulse(lo, hi, x0, x1):
        def f(p, n):
            s = (t - lo) / (hi - lo)
            if s < 0 or s > 1:
                return np.zeros(p.shape[:-1]), np.zeros(p.shape[:-1])
            u = (p[..., 0] - x0) / (x1 - x0)
            return np.zeros(u.shape), (np.abs(u - s) < 0.22).astype(float)
        return f
    ph_on = 0.3 <= t < 0.62
    parts = [Part(lambda p: sd_box(p, (0.82, 0.74, 0.03), 0.02), 0, pos=(0, 0, 0.06))]
    for e in ends_in:
        parts.append(Part(lambda p, e=e: sd_capsule(p, e, A, 0.04), 1))
    for e in ends_out:
        parts.append(Part(lambda p, e=e: sd_capsule(p, B, e, 0.04), 2))
    parts.append(Part(photon, 3))
    parts.append(Part(lambda p: sd_sphere(p - A, 0.08), 4 if abs(t - 0.3) < 0.08 else 5))
    parts.append(Part(lambda p: sd_sphere(p - B, 0.08), 4 if abs(t - 0.62) < 0.08 else 5))
    pulse_col = '#80d0ff'
    mats = [Material('#e8e4d8', '#ffffff', metal=0.1, gloss=0.3),
            Material('#202020', pulse_col, metal=0.2, keep_dark=True, emissive=pulse_col, pattern=pulse(0.0, 0.3, -0.68, -0.26)),
            Material('#202020', pulse_col, metal=0.2, keep_dark=True, emissive=pulse_col, pattern=pulse(0.62, 0.95, 0.26, 0.68)),
            Material('#202020', pulse_col, metal=0.2, keep_dark=True, emissive='#ffe080' if ph_on else None,
                     pattern=pat_all() if ph_on else None),
            glow('#ffffff'),
            Material('#202020', '#606060', metal=0.2, keep_dark=True)]
    return parts, mats, dict(view_pitch=0.12, view_yaw=0.18, zoom=1.0), None


def formal_proof_tree(c1, c2, t):
    # axioms at the top, inference bars below them; each step lights only once the steps above are checked.
    # Loop: middle lights at 0.25, root at 0.5, tick 0.62-0.85, then the proof is cleared root-first so the loop
    # ends in the same state it starts in (it used to snap from fully proven to blank).
    lv_mid, lv_root = 0.25 <= t < 0.94, 0.5 <= t < 0.88

    def node(w):
        return lambda p: sd_box(p, (w, 0.085, 0.05), 0.02)

    def textl(p, n):
        y = p[..., 1]
        x = p[..., 0]
        return np.zeros(y.shape), ((np.abs(y) < 0.03) | (np.abs(x) > 0.5)).astype(float)
    lit = Material(c1, c2, metal=0.3, emissive=c2, pattern=pat_all())
    dim = Material('#30404c', c2, metal=0.4)
    mats = [lit, dim, Material('#c0c8d0', '#ffffff', metal=1.0)]
    parts = []
    for x in (-0.6, -0.2, 0.2, 0.6):
        parts.append(Part(node(0.15), 0, pos=(x, 0.62, 0)))
    for x in (-0.4, 0.4):
        parts.append(Part(lambda p: sd_box(p, (0.32, 0.018, 0.03)), 0 if lv_mid else 2, pos=(x, 0.42, 0)))
        parts.append(Part(node(0.2), 0 if lv_mid else 1, pos=(x, 0.2, 0)))
    parts.append(Part(lambda p: sd_box(p, (0.62, 0.018, 0.03)), 0 if lv_root else 2, pos=(0, -0.01, 0)))
    parts.append(Part(node(0.36), 0 if lv_root else 1, pos=(0, -0.24, 0)))
    view = dict(view_pitch=0.12, view_yaw=0.12, zoom=1.0)

    def ov(img):
        # the machine's tick of approval under the theorem
        if 0.62 <= t < 0.85:
            on = (t * 10) % 1 < 0.7
            if on:
                x, y = proj((0, -0.55, 0), view)
                pts = [(-3, 0), (-2, 1), (-1, 2), (0, 1), (1, 0), (2, -1), (3, -2)]
                for dx, dy in pts:
                    px(img, x + dx, y + dy, C(c2), 1.0)
                    px(img, x + dx, y + dy + 1, C(c2) * 0.6, 0.8)
        # a scan cursor checking the current level
        lvl_y = 0.62 if t < 0.25 else (0.2 if t < 0.5 else -0.24)
        x0, y0 = proj((-0.85, lvl_y, -0.1), view)
        px(img, x0, y0, WHITE, 0.5 + 0.5 * math.sin(t * TAU * 8))
    return parts, mats, view, ov


def godel_sentence(c1, c2, t):
    # a slate that quotes itself: the same tablet nested inside itself forever, zooming without end
    parts = [Part(lambda p: sd_box(p, (0.74, 0.74, 0.08), 0.06), 0),
             Part(lambda p: np.maximum(sd_box(p, (0.7, 0.7, 0.1), 0.03), -sd_box(p, (0.62, 0.62, 0.5))), 1)]
    mats = [Material(c1, c2, metal=0.4), Material('#c09030', c2, metal=1.0)]
    view = dict(view_pitch=0.0, view_yaw=0.0, zoom=1.0)

    def ov(img):
        O = np.array([20.5, 19.5])           # the fixed point of the regress
        C0 = np.array([15.5, 15.5])
        col = C(c2)
        for k in range(-1, 8):
            s = 0.55 ** (k + t)
            r = 9.5 * s
            if r < 0.8 or r > 10.5:
                continue
            cx, cy = O + (C0 - O) * s
            a = max(0.0, min(1.0, r / 2.5, (10.5 - r) / 1.5))
            x0, x1, y0, y1 = cx - r, cx + r, cy - r, cy + r
            line(img, x0, y0, x1, y0, col, a)
            line(img, x0, y1, x1, y1, col, a)
            line(img, x0, y0, x0, y1, col, a)
            line(img, x1, y0, x1, y1, col, a)
            if r > 3:   # the sentence: "this statement..." as two dashes of text
                line(img, x0 + r * 0.25, y0 + r * 0.45, x0 + r * 1.1, y0 + r * 0.45, col * 0.8 + 0.2, a * 0.8)
                line(img, x0 + r * 0.25, y0 + r * 0.8, x0 + r * 0.75, y0 + r * 0.8, col * 0.8 + 0.2, a * 0.8)
    return parts, mats, view, ov


def source_code_fragment(c1, c2, t):
    # a torn-off piece of the universe's terminal, scrolling; most lines are dim comments, few are code
    def shape(p):
        x = p[..., 0]
        tri = lambda v: np.abs(np.mod(v, 1.0) - 0.5)
        top = p[..., 1] - (0.52 + 0.16 * tri(x * 3.1 + 0.2))
        bot = -p[..., 1] - (0.6 + 0.14 * tri(x * 2.3 + 0.7))
        rgt = p[..., 0] - (0.55 + 0.1 * tri(p[..., 1] * 2.7))
        return np.maximum(np.maximum(sd_box(p, (0.8, 0.9, 0.05), 0.02), np.maximum(top, bot) * 0.7), rgt * 0.7)

    def code(p, n):
        y = p[..., 1] - t * 1.0
        row = np.floor(y * 9)
        fr = y * 9 - row
        rr = np.mod(row, 9)
        h = hsh(rr, 3.0)
        indent = -0.48 + np.floor(hsh(rr, 7.0) * 3) * 0.1
        ln = indent + 0.25 + h * 0.6
        on_row = (fr > 0.25) & (fr < 0.75) & (p[..., 0] > indent) & (p[..., 0] < ln) & (n[..., 2] < -0.5)
        comment = hsh(rr, 11.0) > 0.35
        return np.where(on_row & comment, 0.35, 0.0), (on_row & ~comment).astype(float)
    parts = [Part(shape, 0)]
    mats = [Material(c1, '#406050', metal=0.3, emissive=c2, pattern=code)]
    view = dict(view_pitch=0.1, view_yaw=0.15, zoom=1.0)

    def ov(img):
        if (t * 4) % 1 < 0.5:
            x, y = proj((0.25, -0.38, -0.06), view)
            for dy in range(3):
                px(img, x, y + dy, C(c2), 1.0)
                px(img, x + 1, y + dy, C(c2), 1.0)
    return parts, mats, view, ov


# ================================================================================================ codices
def primordial_charter(c1, c2, t):
    # a scroll between two golden rods; the first laws write themselves rune by rune, then fade to start again
    nr, nc = 5, 4
    total = nr * nc
    written = int(min(1.0, t / 0.7) * total + 0.001)
    fade = t > 0.88

    def runes(p, n):
        x, y = p[..., 0], p[..., 1]
        c = np.floor((x + 0.42) / 0.21)
        r = np.floor((0.5 - y) / 0.2)
        fx = (x + 0.42) / 0.21 - c
        fy = (0.5 - y) / 0.2 - r
        idx = r * nc + c
        sx, sy = np.floor(fx * 3), np.floor(fy * 2.6)
        bit = hsh(idx, sx * 3 + sy) > 0.45
        inside = (c >= 0) & (c < nc) & (r >= 0) & (r < nr) & (fx > 0.12) & (fx < 0.88) & (fy > 0.12) & (fy < 0.78)
        on = inside & bit & (idx < written) & (n[..., 2] < -0.3)
        if fade:
            on = on & (hsh(idx, 2.0) > (t - 0.88) / 0.12)
        newest = inside & (idx == written - 1) & ((t * 20) % 1 < 0.5)
        return np.zeros(x.shape), (on | (newest & bit)).astype(float)
    parts = [Part(lambda p: sd_box(p, (0.5, 0.56, 0.02), 0.01), 0),
             Part(lambda p: sd_cyl(p, 0.08, 0.6, 0.02), 1, pos=(0, 0.6, 0), rot=rotz(TAU / 4)),
             Part(lambda p: sd_cyl(p, 0.08, 0.6, 0.02), 1, pos=(0, -0.6, 0), rot=rotz(TAU / 4))]
    for s in (-1, 1):
        for y in (0.6, -0.6):
            parts.append(Part(lambda p: sd_sphere(p, 0.1), 2, pos=(s * 0.66, y, 0)))
    mats = [Material(c1, c2, metal=0.2, emissive=c2, pattern=runes), Material('#c8a040', '#fff0c0', metal=1.0),
            Material(c2, '#ffffff', metal=0.6, emissive=c2, pattern=pat_pulse_core(t))]
    return parts, mats, dict(view_pitch=0.1, view_yaw=0.25, zoom=1.0), None


def quantum_codex(c1, c2, t):
    # the field guide lies open; a page turns, and an atom glows on the left-hand page
    ang = -0.22 + (math.pi + 0.44) * sstep((t - 0.15) / 0.55)
    if t >= 0.85:
        ang = -0.22   # the turned page has settled on the left; a fresh one lies on the right
    lift = 0.0

    def text(p, n):
        z = p[..., 2]
        x = p[..., 0]
        row = np.mod(z * 9, 1.0)
        on = (row > 0.35) & (row < 0.6) & (np.abs(x) < 0.3) & (n[..., 1] > 0.3)
        return np.where(on, -0.35, 0.0), np.zeros(z.shape)

    def atom(p, n):
        x, z = p[..., 0] + 0.42, p[..., 2]
        orb = np.zeros(x.shape, bool)
        for k in range(3):
            a = k * math.pi / 3 + t * TAU
            u = x * math.cos(a) + z * math.sin(a)
            v = -x * math.sin(a) + z * math.cos(a)
            orb |= np.abs(np.sqrt((u / 0.3) ** 2 + (v / 0.1) ** 2) - 1) < 0.22
        orb |= (x * x + z * z) < 0.004
        return np.zeros(x.shape), (orb & (n[..., 1] > 0.3)).astype(float)
    parts = [Part(lambda p: sd_box(p - np.array([-0.44, 0, 0]), (0.46, 0.04, 0.6), 0.02), 0, rot=rotz(-0.2)),
             Part(lambda p: sd_box(p - np.array([0.44, 0, 0]), (0.46, 0.04, 0.6), 0.02), 0, rot=rotz(0.2)),
             Part(lambda p: sd_box(p - np.array([-0.42, 0.07, 0]), (0.4, 0.05, 0.55), 0.01), 1, rot=rotz(-0.2)),
             Part(lambda p: sd_box(p - np.array([0.42, 0.07, 0]), (0.4, 0.05, 0.55), 0.01), 2, rot=rotz(0.2)),
             Part(lambda p: sd_cyl(p, 0.06, 0.6), 3, pos=(0, -0.02, 0), rot=rotx(TAU / 4)),
             Part(lambda p: sd_box(p - np.array([0.4, 0, 0]), (0.38, 0.008, 0.53)), 2, pos=(0, 0.13 + lift, 0), rot=rotz(ang))]
    mats = [Material(c1, c2, metal=0.5), Material('#e8ecf4', c2, metal=0.1, emissive=c2, pattern=atom),
            Material('#e8ecf4', c2, metal=0.1, pattern=text), Material('#c8a040', '#fff0c0', metal=1.0)]
    return parts, mats, dict(view_pitch=0.75, view_yaw=0.0, zoom=1.05), None


# ================================================================================================ coils
def atto_inductor(c1, c2, t):
    # a monopole at the centre; its field lines run straight out and are wound into a ring
    dirs = []
    for k in range(8):
        a = k * TAU / 8
        e = 0.55 if k % 2 else -0.45
        d = np.array([math.cos(a), e, math.sin(a)])
        dirs.append(d / np.linalg.norm(d))

    def lines(p):
        d = 1e9
        for v in dirs:
            d = np.minimum(d, sd_capsule(p, v * 0.15, v * 0.72, 0.035))
        return d

    def outflow(p, n):
        r = np.sqrt((p ** 2).sum(-1))
        return np.zeros(r.shape), (np.sin(r * 16 - t * TAU * 3) > 0.4).astype(float)
    parts = [Part(lambda p: sd_sphere(p, 0.2), 0), Part(lines, 1),
             Part(lambda p: sd_torus(p, 0.68, 0.06), 2, rot=rotx(0.25))]
    mats = [Material(c2, '#ffffff', emissive=c2, pattern=pat_pulse_core(t)),
            Material(c1, c2, metal=0.6, emissive=c2, pattern=outflow),
            Material(c1, c2, metal=0.9, emissive=c2, pattern=pat_bands(30, 0, t, 0.25, axis=0))]
    mats[2].pattern = lambda p, n: (0.3 * np.sign(np.sin(np.arctan2(p[..., 2], p[..., 0]) * 18)),
                                    (np.abs(wrap_np(np.arctan2(p[..., 2], p[..., 0]) - t * TAU * 2)) < 0.35).astype(float))
    return parts, mats, dict(view_pitch=0.35, view_yaw=0.5), None


def dipole_winding(c1, c2, t):
    # two saddle coils clamped over a beam pipe; bunches of beam shoot through it, current circulates in the coils
    def saddle(sign):
        def f(p):
            q = p.copy()
            q[..., 1] = sign * p[..., 1] - 0.16 - 0.9 * p[..., 2] ** 2
            x = np.clip(q[..., 0], -0.42, 0.42)
            d2 = np.sqrt((q[..., 0] - x) ** 2 + q[..., 2] ** 2) - 0.24
            return np.sqrt(d2 ** 2 + q[..., 1] ** 2) * 0.8 - 0.065
        return f

    def current(p, n):
        a = np.arctan2(p[..., 2], np.clip(np.abs(p[..., 0]) - 0.42, 0, None) * np.sign(p[..., 0]) + 1e-6)
        s = np.where(np.abs(p[..., 0]) < 0.42, p[..., 0] * np.sign(p[..., 2]) * 2.2, np.sign(p[..., 0]) * 0.92 + a * 0.3)
        return np.zeros(s.shape), (np.sin(s * 9 - t * TAU * 2) > 0.55).astype(float)

    def beam(p, n):
        return np.zeros(p.shape[:-1]), (np.mod(p[..., 1] * 2.5 + t * 4, 1.0) < 0.22).astype(float)
    parts = [Part(lambda p: sd_cyl(p, 0.11, 0.85), 1, rot=rotz(TAU / 4)),
             Part(saddle(1), 0), Part(saddle(-1), 0),
             Part(lambda p: sd_cyl(p, 0.2, 0.04, 0.01), 2, pos=(-0.8, 0, 0), rot=rotz(TAU / 4)),
             Part(lambda p: sd_cyl(p, 0.2, 0.04, 0.01), 2, pos=(0.8, 0, 0), rot=rotz(TAU / 4))]
    mats = [Material(c1, c2, metal=0.95, emissive=c2, pattern=current),
            Material('#9aa4b4', '#ffffff', metal=1.0, emissive='#ffffff', pattern=beam),
            Material('#d0d4dc', '#ffffff', metal=1.0)]
    return parts, mats, dict(view_pitch=0.45, view_yaw=0.45, zoom=1.0), None


def femto_inductor(c1, c2, t):
    # a coreless helix: copper wound around empty space, the current spiralling along it
    def em(p, n):
        a = np.arctan2(p[..., 2], p[..., 0])
        s = p[..., 1] / 0.3
        return np.zeros(s.shape), (np.mod(s - t * 4, 2.0) < 0.45).astype(float)
    rot = rotz(0.45) @ rotx(-0.25)
    parts = [Part(lambda p: helix(p, 0.36, 0.36, 0.055, 0.6), 0, rot=rot),
             Part(lambda p: sd_capsule(p, (0.36, 0.6, 0), (0.36, 0.88, 0), 0.05), 0, rot=rot),
             Part(lambda p: sd_capsule(p, (0.36, -0.6, 0), (0.36, -0.88, 0), 0.05), 0, rot=rot)]
    mats = [Material(c1, c2, metal=1.0, emissive=c2, pattern=em)]
    view = dict(view_pitch=0.3, view_yaw=0.3)

    def ov(img):
        # nothing in the middle - just an occasional quantum flicker of the vacuum
        if (t * 5) % 1 < 0.25:
            x, y = proj((0, 0, 0), view)
            px(img, x + (int(t * 5) % 3) - 1, y, WHITE, 0.7)
    return parts, mats, view, ov


def superconducting_coil(c1, c2, t):
    # a frosted toroidal winding; one bright packet of current circles it forever, never slowing
    def wind(p, n):
        a = np.arctan2(p[..., 2], p[..., 0])
        stripe = np.sin(a * 22) > 0
        head = np.abs(wrap_np(a - t * TAU)) < 0.35
        tail = np.abs(wrap_np(a - t * TAU + 0.55)) < 0.25
        frost = hsh(np.floor(p[..., 0] * 18), np.floor(p[..., 1] * 18), np.floor(p[..., 2] * 18)) > 0.9
        return np.where(stripe, -0.12, 0.08) + 0.25 * frost, (head | (tail & stripe)).astype(float)
    parts = [Part(lambda p: sd_torus(p, 0.5, 0.19), 0, rot=rotx(-1.15)),
             Part(lambda p: sd_cyl(p, 0.12, 0.05, 0.02), 1, rot=rotx(-1.15), pos=(0, 0, 0))]
    mats = [Material(c1, c2, metal=0.9, emissive=c2, pattern=wind), Material('#c0d0e0', '#ffffff', metal=1.0)]
    return parts, mats, dict(view_pitch=0.3, view_yaw=0.5), None


# ================================================================================================ contained
def annihilation_core(c1, c2, t):
    # inside a steel cage a blob of matter and a blob of antimatter drift together, annihilate, and re-form
    s = sstep(t / 0.45)
    flash = max(0.0, 1 - abs(t - 0.5) / 0.1) if 0.4 < t < 0.6 else 0.0
    grow = sstep((t - 0.65) / 0.3) if t >= 0.6 else 1.0
    alive = t < 0.48 or t >= 0.65
    r = 0.21 * (grow if t >= 0.6 else 1.0)
    x = 0.38 * (1 - s) + 0.1 if t < 0.6 else 0.48
    frame = lambda p: np.maximum(sd_box(p, (0.62, 0.62, 0.62), 0.04),
                                 -np.minimum(np.minimum(sd_box(p, (0.5, 0.5, 1)), sd_box(p, (1, 0.5, 0.5))), sd_box(p, (0.5, 1, 0.5))))
    parts = [Part(frame, 0)]
    mats = [Material('#7a8090', '#ffffff', metal=1.0), glow('#80c0ff'), glow(c1), glow('#ffffff')]
    if alive and r > 0.01:
        parts.append(Part(lambda p: sd_sphere(p, r), 1, pos=(-x, 0, 0)))
        parts.append(Part(lambda p: sd_sphere(p, r), 2, pos=(x, 0, 0)))
    if flash > 0:
        parts.append(Part(lambda p: sd_sphere(p, 0.1 + 0.3 * flash), 3))
    view = dict(view_pitch=0.45, view_yaw=0.6)

    def ov(img):
        if flash > 0.2:
            for k in range(8):
                a = k * TAU / 8
                for rr in range(int(6 + 8 * flash), int(10 + 8 * flash)):
                    only_empty(img, 15.5 + math.cos(a) * rr, 15.5 + math.sin(a) * rr, C(c1), flash)
    return parts, mats, view, ov


def antimatter_pellet(c1, c2, t):
    # a Penning trap: two end caps and a ring electrode; the pellet hovers in the middle, circling, never touching
    a = TAU * t * 2
    pos = (0.07 * math.cos(a), 0.05 * math.sin(TAU * t * 3), 0.07 * math.sin(a))
    cap = lambda p: np.maximum(sd_cyl(p, 0.3, 0.12, 0.04), -sd_sphere(p - np.array([0, 0, 0]) * 0, 0.0) + 0 * p[..., 0] - 1)
    parts = [Part(lambda p: sd_cyl(p, 0.3, 0.1, 0.04), 0, pos=(0, 0.62, 0)),
             Part(lambda p: sd_cyl(p, 0.3, 0.1, 0.04), 0, pos=(0, -0.62, 0)),
             Part(lambda p: sd_cyl(p, 0.1, 0.1, 0.02), 0, pos=(0, 0.45, 0)),
             Part(lambda p: sd_cyl(p, 0.1, 0.1, 0.02), 0, pos=(0, -0.45, 0)),
             Part(lambda p: sd_torus(p, 0.55, 0.07), 1),
             Part(lambda p: sd_sphere(p, 0.16), 2, pos=pos)]
    mats = [Material('#c8a040', '#fff0c0', metal=1.0), Material('#8a90a0', c2, metal=1.0, emissive=c1,
                                                                 pattern=pat_bands(8, 2, t, 0.1, axis=0)),
            Material(c1, '#ffffff', emissive=c1, pattern=pat_pulse_core(t))]
    view = dict(view_pitch=0.25, view_yaw=0.5)

    def ov(img):
        x, y = proj(pos, view)
        for k in range(4):     # the magnetron orbit trail
            aa = a - (k + 1) * 0.5
            qx, qy = proj((0.07 * math.cos(aa) * 2.5, pos[1], 0.07 * math.sin(aa) * 2.5), view)
            px(img, qx, qy, C(c1), 0.7 - k * 0.15)
    return parts, mats, view, ov


def coherence_stabiliser(c1, c2, t):
    # a hexagonal lantern; inside, one clean standing wave goes round and round without breaking up
    def wave(p, n):
        a = np.arctan2(p[..., 2], p[..., 0])
        y = p[..., 1]
        w = 0.22 * np.sin(a * 3 - t * TAU) * np.cos(a * 0 + 0)
        band = np.abs(y - w) < 0.06
        env = np.abs(y + w) < 0.035
        return -0.1 + np.zeros(y.shape), (band | env).astype(float)
    parts = [Part(lambda p: sd_cyl(p, 0.32, 0.46), 1),
             Part(lambda p: sd_hexprism(p, 0.42, 0.07), 0, pos=(0, 0.55, 0)),
             Part(lambda p: sd_hexprism(p, 0.42, 0.07), 0, pos=(0, -0.55, 0)),
             Part(lambda p: sd_cyl(p, 0.1, 0.08), 0, pos=(0, 0.68, 0))]
    for k in range(3):
        a = k * TAU / 3 + 0.5
        parts.append(Part(lambda p, a=a: sd_capsule(p, (0.4 * math.cos(a), -0.5, 0.4 * math.sin(a)), (0.4 * math.cos(a), 0.5, 0.4 * math.sin(a)), 0.035), 0))
    mats = [Material('#7a8698', '#ffffff', metal=1.0), Material(c1, c2, metal=1.0, gloss=1.6, emissive=c2, pattern=wave)]
    return parts, mats, dict(view_pitch=0.25, view_yaw=0.5), None


def genome_sample(c1, c2, t):
    # a corked test tube; a double helix turns slowly in the green liquid
    def dna(p, n):
        a = np.arctan2(p[..., 2], p[..., 0])
        y = p[..., 1]
        ph = a - y * 9 - t * TAU
        s1 = np.abs(wrap_np(ph)) < 0.45
        s2 = np.abs(wrap_np(ph + math.pi)) < 0.45
        rung = (np.abs(np.mod(y * 9 / TAU * 4, 1) - 0.5) < 0.1) & (np.abs(wrap_np(ph + math.pi / 2)) < 1.2)
        liquid = y < 0.32
        return np.where(liquid, 0.0, -0.25), ((s1 | s2 | rung) & liquid & (y > -0.6)).astype(float)
    rot = rotz(-0.35)
    parts = [Part(lambda p: sd_capsule(p, (0, -0.52, 0), (0, 0.5, 0), 0.22), 0, rot=rot),
             Part(lambda p: sd_torus(p - np.array([0, 0.52, 0]), 0.22, 0.04), 1, rot=rot),
             Part(lambda p: sd_cyl(p - np.array([0, 0.62, 0]), 0.17, 0.12, 0.03), 2, rot=rot)]
    mats = [Material('#2c4430', c2, metal=0.9, gloss=1.6, emissive=c2, pattern=dna),
            Material('#c0c8d0', '#ffffff', metal=1.0), Material('#8a6038', '#d0a070', metal=0.1)]
    return parts, mats, dict(view_pitch=0.25, view_yaw=0.5), None


def noble_gas_clathrate(c1, c2, t):
    # an icy geodesic cage with one xenon atom rattling inside it
    norms = [np.array(v, float) / np.linalg.norm(v) for v in ((1, 0, 0), (0, 1, 0), (0, 0, 1), (1, 1, 1), (1, -1, 1), (-1, 1, 1), (1, 1, -1))]

    def cage(p):
        shell = np.abs(sd_sphere(p, 0.6)) - 0.05
        ribs = 1e9
        for v in norms:
            ribs = np.minimum(ribs, np.abs(p @ v))
        return np.maximum(shell, ribs - 0.055)
    jit = (0.07 * math.sin(TAU * t * 3), 0.05 * math.sin(TAU * t * 2 + 1), 0.06 * math.cos(TAU * t * 5))
    parts = [Part(cage, 0, rot=roty(0.3) @ rotx(0.2)), Part(lambda p: sd_sphere(p, 0.26), 1, pos=jit)]
    mats = [Material('#b8d8f0', '#ffffff', metal=0.7, gloss=1.8, pattern=pat_noise_shade(14, 0.12, 2)),
            Material('#8070e0', '#ffffff', emissive='#b0a0ff', pattern=pat_pulse_core(t))]
    return parts, mats, dict(view_pitch=0.35, view_yaw=0.5), None


def stem_cell_culture(c1, c2, t):
    # a petri dish of cells that keep starting to divide and changing their minds - and their colour
    def dish(p):
        base = sd_cyl(p - np.array([0, -0.12, 0]), 0.78, 0.05, 0.02)
        rim = np.maximum(np.abs(np.sqrt(p[..., 0] ** 2 + p[..., 2] ** 2) - 0.78) - 0.03, np.abs(p[..., 1] + 0.04) - 0.12)
        return np.minimum(base, rim)

    def cell(ph, cx, cz, r, ang):
        s = per(t + ph) * 0.13
        d = np.array([math.cos(ang), 0, math.sin(ang)])
        a = np.array([cx, -0.02, cz]) + d * s
        b = np.array([cx, -0.02, cz]) - d * s
        return lambda p: smin(sd_sphere(p - a, r), sd_sphere(p - b, r), 0.09) * 1.0
    col = np.clip(anim.hue_matrix(70 * math.sin(TAU * t)) @ C(c2), 0, 1)
    col2 = np.clip(anim.hue_matrix(-70 * math.sin(TAU * t) + 30) @ C(c2), 0, 1)

    def nucleus(p, n):
        r = np.sqrt((p ** 2).sum(-1))
        return np.zeros(r.shape), (np.sin(p[..., 0] * 30) * np.sin(p[..., 2] * 30) > 0.6).astype(float)
    parts = [Part(dish, 0), Part(cell(0.0, -0.3, -0.1, 0.17, 0.3), 1), Part(cell(0.33, 0.28, -0.2, 0.15, 1.8), 2),
             Part(cell(0.66, 0.05, 0.32, 0.16, 2.6), 1)]
    mats = [Material('#a8b8c8', '#ffffff', metal=0.9, gloss=1.5),
            Material(col, '#ffffff', metal=0.2, emissive=col * 0.6 + 0.4, pattern=nucleus),
            Material(col2, '#ffffff', metal=0.2, emissive=col2 * 0.6 + 0.4, pattern=nucleus)]
    return parts, mats, dict(view_pitch=0.75, view_yaw=0.5, zoom=1.05), None


def tardigrade_culture(c1, c2, t):
    # a water bear walks on its eight stubby legs, then curls into a dry tun and simply pauses - then walks on
    s = sstep((t - 0.35) / 0.12) - sstep((t - 0.85) / 0.12)
    L = 0.5 * (1 - 0.55 * s)

    def body(p):
        d = 1e9
        for k in range(4):
            cx = L * (-0.78 + k * 0.48)
            r = 0.2 + (0.03 if k in (1, 2) else 0) + 0.1 * s
            d = smin(d, sd_sphere(p - np.array([cx, 0.02, 0]), r), 0.1)
        hx = L * 1.05 + 0.04
        d = smin(d, sd_sphere(p - np.array([hx, 0.0, 0]), 0.14 + 0.06 * s), 0.08)
        return d
    walk = TAU * t * 4

    def legs(p):
        d = 1e9
        for k in range(4):
            x = L * (-0.8 + k * 0.5)
            for side in (-1, 1):
                ph = walk + k * 1.6 + (0 if side > 0 else math.pi)
                a = np.array([x, -0.1, side * 0.13])
                ln = 0.24 * (1 - s) + 0.01
                b = a + np.array([0.06 * math.sin(ph) * (1 - s), -ln, side * 0.05 * (1 - s)])
                d = np.minimum(d, sd_capsule(p, a, b, 0.055))
        return d

    def seg(p, n):
        return 0.12 * np.cos(p[..., 0] / max(L, 0.1) * 6.5), np.zeros(p.shape[:-1])
    skin = mix(C(c2), np.array([0.45, 0.35, 0.22]), s)
    parts = [Part(body, 0), Part(legs, 1)]
    mats = [Material(skin, '#ffffff', metal=0.1, pattern=seg), Material(skin * 0.85, '#ffffff', metal=0.1)]
    view = dict(view_pitch=0.3, view_yaw=0.35, zoom=1.08)

    def ov(img):
        if s < 0.5:
            x, y = proj((L * 1.05 + 0.12, 0.06, -0.1), view)
            px(img, x, y, np.array([0.05, 0.05, 0.08]), 1.0)
        if s > 0.8:   # paused
            for dx in (0, 3):
                for dy in range(4):
                    px(img, 23 + dx, 3 + dy, C(c2), 0.95)
                    px(img, 24 + dx, 3 + dy, C(c2), 0.95)
    return parts, mats, view, ov


def xenogenome_sample(c1, c2, t):
    # a round-bottomed flask; the genome inside is a three-armed spiral that is not DNA and does not hold still
    def code(p, n):
        a = np.arctan2(p[..., 2], p[..., 0])
        y = p[..., 1]
        ph = a * 3 + y * 7 - t * TAU * 2
        arm = np.abs(wrap_np(ph)) < 0.55
        dots = hsh(np.floor(p[..., 0] * 10), np.floor(p[..., 1] * 10 + t * 20), np.floor(p[..., 2] * 10)) > 0.93
        return np.zeros(y.shape), (arm | dots).astype(float)
    parts = [Part(lambda p: sd_sphere(p, 0.48), 0, pos=(0, -0.2, 0)),
             Part(lambda p: sd_cyl(p, 0.13, 0.3, 0.02), 1, pos=(0, 0.42, 0)),
             Part(lambda p: sd_torus(p, 0.14, 0.04), 2, pos=(0, 0.72, 0)),
             Part(lambda p: sd_cyl(p, 0.1, 0.08, 0.03), 3, pos=(0, 0.8, 0))]
    mats = [Material(c1, c2, metal=0.9, gloss=1.6, emissive=c2, pattern=code),
            Material('#382848', c2, metal=1.0, gloss=1.6), Material('#c0c8d0', '#ffffff', metal=1.0),
            Material('#203040', c2, metal=0.5, emissive=c2, pattern=pat_bands(4, 1, t, 0.2, axis=0))]
    return parts, mats, dict(view_pitch=0.3, view_yaw=0.5, zoom=1.02), None


# ================================================================================================ crystals
def aleph_null_shard(c1, c2, t):
    # an endless row of identical shards, each smaller than the last, receding into a vanishing point forever
    O = np.array([0.5, 0.48, 0.2])
    C0 = np.array([-0.42, -0.4, 0])
    parts = []
    for k in range(-3, 9):
        s = 0.62 ** (k + t)
        if s < 0.03:
            continue
        c = O + (C0 - O) * s
        parts.append(Part(lambda p, s=s: gem_s(p, s * 0.55, 0.3, 0.75), 0, pos=c, rot=rotz(0.6) @ roty(0.5 + k * 0.7)))
    mats = [Material(c1, c2, metal=0.9, gloss=1.4, emissive=c2,
                     pattern=lambda p, n: (np.zeros(p.shape[:-1]), (np.abs(p[..., 1]) < 0.02).astype(float)))]

    def ov(img):
        anim.ov_sparkles(img, t, 21, C(c2), 4, 2)
    return parts, mats, dict(view_pitch=0.3, view_yaw=0.4), ov


def bekenstein_crystal(c1, c2, t):
    # a cube balanced on its corner; every bit it holds lives on its surface, blinking, while its inside is dark
    def bits(p, n):
        g = np.floor(p * 5)
        h = hsh(g[..., 0], g[..., 1], g[..., 2])
        on = np.sin(h * 60 + t * TAU * (1 + np.floor(h * 2))) > 0.45
        q = np.abs(np.mod(p * 5, 1) - 0.5) < 0.34
        cell = (q.sum(-1) >= 2)
        edge = (np.abs(p) > 0.45).sum(-1) >= 2
        return np.where(cell, 0.0, -0.3), ((on & cell) | edge).astype(float)
    parts = [Part(lambda p: sd_box(p, (0.48, 0.48, 0.48), 0.03), 0, rot=roty(t * TAU / 4 * 0 + 0.0) @ rotz(math.pi / 4) @ rotx(0.6155))]
    mats = [Material(c1, c2, metal=0.8, emissive=c2, pattern=bits)]
    return parts, mats, dict(view_pitch=0.3, view_yaw=0.5), None


def cherenkov_boule(c1, c2, t):
    # a lying boule with its seed neck, a slicing laser plane sweeping along marking where the wafers will be cut
    def body(p):
        return smin(sd_capsule(p, (-0.5, 0, 0), (0.42, 0, 0), 0.32), sd_capsule(p, (0.5, 0, 0), (0.78, 0, 0), 0.08), 0.15)

    def slices(p, n):
        x = p[..., 0]
        cut = np.abs(np.mod(x * 5, 1) - 0.5) < 0.06
        xs = -0.8 + t * 1.6
        laser = np.abs(x - xs) < 0.04
        done = cut & (x < xs)
        facet = 0.18 * np.sign(np.sin(np.arctan2(p[..., 2], p[..., 1]) * 6))
        return facet - 0.15 * done, (laser | (done & (np.sin(x * 40 + t * TAU * 2) > 0.3))).astype(float)
    parts = [Part(body, 0, rot=rotz(0.15) @ roty(0.0))]
    mats = [Material(c1, c2, metal=0.6, gloss=1.6, emissive=c2, pattern=slices)]
    return parts, mats, dict(view_pitch=0.35, view_yaw=0.35), None


def cherenkov_quartz(c1, c2, t):
    # a quartz point; a particle outruns light through it, trailing a blue Mach cone
    parts = [Part(lambda p: sd_gem(p, 0.3, 0.92, 0.15), 0, rot=rotz(0.2) @ roty(0.4)),
             Part(lambda p: sd_gem(p, 0.17, 0.5, 0.15), 0, pos=(0.42, -0.42, 0.1), rot=rotz(-0.7)),
             Part(lambda p: sd_gem(p, 0.15, 0.42, 0.15), 0, pos=(-0.4, -0.5, -0.1), rot=rotz(0.75))]
    mats = [Material(c1, c2, metal=0.85, gloss=1.4, emissive=c2,
                     pattern=lambda p, n: (np.zeros(p.shape[:-1]), (np.sin(p[..., 1] * 20 + t * TAU * 2) > 0.85).astype(float)))]

    def ov(img):
        for k in range(2):
            s = (t * 2 + k * 0.0) % 1.0
            if k == 1:
                continue
            x, y = -2 + s * 36, 30 - s * 30
            for j in range(1, 9):
                a = 1 - j / 9
                for side in (-1, 1):
                    for w in (0, 1):
                        px(img, x - j * 0.9 + side * j * 0.5 + w, y + j * 0.9 + side * j * 0.5, mix(C(c2), WHITE, 0.3), a)
            px(img, x, y, WHITE, 1.0)
            px(img, x - 1, y + 1, WHITE, 0.8)
    return parts, mats, dict(view_pitch=0.3, view_yaw=0.5), ov


def chiral_crystal(c1, c2, t):
    # a hexagonal crystal twisted into a left-handed screw
    def twisted(p):
        a = p[..., 1] * 3.0
        c, s = np.cos(a), np.sin(a)
        q = np.stack([p[..., 0] * c - p[..., 2] * s, p[..., 1], p[..., 0] * s + p[..., 2] * c], -1)
        return sd_gem(q, 0.34, 0.95, 0.3) * 0.55

    def stripe(p, n):
        a = np.arctan2(p[..., 2], p[..., 0]) + p[..., 1] * 3.0
        edge = np.abs(np.mod(a + math.pi / 6, math.pi / 3) - math.pi / 6) < 0.09
        run = np.sin(p[..., 1] * 6 - t * TAU * 2) > 0.2
        return np.zeros(a.shape), (edge & run).astype(float)
    parts = [Part(twisted, 0, rot=rotz(0.25))]
    mats = [Material(mix(C(c1), C(c2), 0.55), c2, metal=0.9, gloss=1.4, emissive='#fff0c0', pattern=stripe)]
    return parts, mats, dict(view_pitch=0.25, view_yaw=0.5), None


def dilithium_crystal(c1, c2, t):
    # a rough cluster of porous crystals; antimatter specks seep through the pores to a regulated heartbeat
    long = lambda p: sd_octa(p / np.array([0.45, 1.0, 0.45]), 0.8) * 0.45

    def pores(p, n):
        g = np.floor(p * 9)
        h = hsh(g[..., 0], g[..., 1], g[..., 2])
        pore = h > 0.78
        flow = np.mod(h * 7 + t * 2, 1.0) < 0.3
        return np.where(pore, -0.35, 0.0), (pore & flow).astype(float)
    parts = [Part(long, 0, rot=rotz(0.25), pos=(0, 0.08, 0)),
             Part(lambda p: long(p / 0.6) * 0.6, 0, pos=(0.42, -0.36, 0.05), rot=rotz(-0.8)),
             Part(lambda p: long(p / 0.5) * 0.5, 0, pos=(-0.4, -0.42, -0.05), rot=rotz(0.9)),
             Part(lambda p: sd_box(p, (0.55, 0.08, 0.3), 0.05), 1, pos=(0, -0.68, 0))]
    mats = [Material(c1, c2, metal=0.7, gloss=1.2, emissive=c2, pattern=pores), Material('#5a4048', '#a08088', metal=0.2,
                                                                                       pattern=pat_noise_shade(12, 0.3))]
    return parts, mats, dict(view_pitch=0.3, view_yaw=0.6), None


def electride_crystal(c1, c2, t):
    # a cubic salt lattice of dark ions; its anions are bare electrons that hop from hole to hole between the ions
    def ions(p):
        q = p - 0.32 * np.clip(np.round(p / 0.32), -1, 1)
        return sd_sphere(q, 0.13)
    holes = [np.array(v) * 0.16 for v in ((1, 2.6, -1), (-1, 2.6, -1), (-1, 2.6, 1), (1, 2.6, 1), (1, 1, -2.6), (-1, 1, -2.6), (-1, -1, -2.6), (1, -1, -2.6))]
    k = t * 8
    i = int(k) % 8
    s_ = sstep((k % 1) / 0.35)
    e = holes[i] * (1 - s_) + holes[(i + 1) % 8] * s_
    j = (i + 4) % 8
    e2 = holes[j] * (1 - s_) + holes[(j + 1) % 8] * s_
    R = roty(0.3)
    parts = [Part(ions, 0, rot=R), Part(lambda p: sd_sphere(p, 0.12), 1, pos=R @ e), Part(lambda p: sd_sphere(p, 0.12), 1, pos=R @ e2)]
    mats = [Material('#2a3660', '#80a0e0', metal=0.8, gloss=1.2), glow(mix(C(c2), WHITE, 0.6))]
    return parts, mats, dict(view_pitch=0.35, view_yaw=0.5, zoom=1.05), None


def exotic_matter_crystal(c1, c2, t):
    # a crystal hanging point-down, straining upwards; droplets of it break off and fall up and away
    def drop(p):
        q = p / np.array([0.5, 1.0, 0.5])
        return np.maximum(sd_octa(q, 0.9) * 0.5, p[..., 1] - 0.3)
    parts = [Part(drop, 0, pos=(0, 0.0 + 0.06 * math.sin(TAU * t), 0), rot=roty(0.6)),
             Part(lambda p: sd_box(p, (0.3, 0.05, 0.3), 0.02), 2, pos=(0, 0.36 + 0.06 * math.sin(TAU * t), 0), rot=roty(0.6))]
    for k in range(3):
        life = (t + k / 3) % 1.0
        y = 0.25 + life * 0.9
        sz = 0.11 * math.sin(life * math.pi) + 0.005
        x = (-0.25, 0.08, 0.3)[k]
        parts.append(Part(lambda p, sz=sz: sd_octa(p, sz), 1, pos=(x, y, 0), rot=rotz(life * TAU)))
    mats = [Material(c1, c2, metal=0.8, gloss=1.4, emissive=c2,
                     pattern=lambda p, n: (np.zeros(p.shape[:-1]), (np.abs(np.mod(p[..., 1] * 3 - t * 2 + np.floor(np.arctan2(p[..., 2], p[..., 0]) * 2) * 0.37, 1) - 0.5) < 0.1).astype(float))),
            glow(c2), Material('#a0a8b8', '#ffffff', metal=1.0)]
    return parts, mats, dict(view_pitch=0.25, view_yaw=0.5, zoom=1.0), None


def folded_protein_crystal(c1, c2, t):
    # one amino-acid chain folded into its unique shape: a helix and a pleated sheet; a signal runs along the chain
    pts = []
    for i in range(15):
        u = 0.15 + i / 14 * (TAU - 0.45)
        pts.append(np.array([math.sin(u) + 2 * math.sin(2 * u), math.cos(u) - 2 * math.cos(2 * u), -math.sin(3 * u) * 1.3]) * 0.21)
    segs = list(zip(pts[:-1], pts[1:]))
    n = len(segs)

    def chain(p):
        d = 1e9
        for a, b in segs:
            d = np.minimum(d, sd_capsule(p, a, b, 0.07))
        for c in pts:
            d = smin(d, sd_sphere(p - c, 0.095), 0.04)
        return d

    def signal(p, nrm):
        best = np.full(p.shape[:-1], 1e9)
        par = np.zeros(p.shape[:-1])
        for i, (a, b) in enumerate(segs):
            ba = b - a
            h = np.clip(((p - a) * ba).sum(-1) / (ba @ ba), 0, 1)
            d = np.sqrt(((p - a - ba * h[..., None]) ** 2).sum(-1))
            closer = d < best
            best = np.where(closer, d, best)
            par = np.where(closer, i + h, par)
        u = np.mod(par - t * n, n)
        return np.zeros(par.shape), ((u < 1.6) | (np.abs(np.mod(par, 1) - 0.0) < 0.08)).astype(float)
    parts = [Part(chain, 0)]
    for i in (0, len(pts) - 1):
        parts.append(Part(lambda p, c=pts[i]: sd_sphere(p - c, 0.11), 1))
    mats = [Material(c1, c2, metal=0.5, emissive=c2, pattern=signal), glow(c2)]
    return parts, mats, dict(view_pitch=0.3, view_yaw=0.4, zoom=1.0), None


def fractal_core(c1, c2, t):
    # a Sierpinski tetrahedron; light bands pour inward from scale to scale, so the largest copy becomes the smallest
    def sier(p):
        z = p / 0.62
        for _ in range(3):
            x, y, w = z[..., 0], z[..., 1], z[..., 2]
            m = x + y < 0
            x, y = np.where(m, -y, x), np.where(m, -x, y)
            m = x + w < 0
            x, w = np.where(m, -w, x), np.where(m, -x, w)
            m = y + w < 0
            y, w = np.where(m, -w, y), np.where(m, -y, w)
            z = np.stack([x, y, w], -1) * 2 - 1
        d = (np.maximum(np.maximum(-z[..., 0] - z[..., 1] - z[..., 2], z[..., 0] + z[..., 1] - z[..., 2]),
                        np.maximum(z[..., 0] - z[..., 1] + z[..., 2], -z[..., 0] + z[..., 1] + z[..., 2])) - 1) / math.sqrt(3)
        return d / 8 * 0.62

    def zoom(p, n):
        r = np.sqrt((p ** 2).sum(-1)) + 1e-4
        return np.zeros(r.shape), (np.sin(np.log(r) / math.log(2) * TAU + t * TAU * 2) > 0.8).astype(float)
    parts = [Part(sier, 0, rot=rotx(-0.6155) @ roty(math.pi / 4) @ roty(0.0))]
    mats = [Material(c1, c2, metal=0.9, emissive=c2, pattern=zoom)]
    return parts, mats, dict(view_pitch=0.35, view_yaw=0.3, zoom=1.0), None


def frozen_photon(c1, c2, t):
    # a wave packet of light stuck on the edge of a black horizon, frozen solid, slowly reddening into nothing
    def wave(p):
        x = p[..., 0]
        xc = np.clip(x, -0.62, 0.62)
        env = np.exp(-(xc / 0.4) ** 2)
        w = 0.2 * env * np.sin(xc * 14)
        return np.sqrt((x - xc) ** 2 + ((p[..., 1] - w) * 0.5) ** 2 + (p[..., 2] * 1.0) ** 2) - 0.05 - 0.03 * env
    k = per(t)
    col = mix(C(c2), np.array([0.55, 0.06, 0.04]), k * 0.8)
    parts = [Part(lambda p: sd_sphere(p, 1.0), 0, pos=(0, -1.32, 0.3)), Part(wave, 1, pos=(0, 0.05, 0)),
             Part(lambda p: sd_torus(p, 1.0, 0.025), 2, pos=(0, -1.32, 0.3))]
    mats = [black(), Material(col, '#ffffff', emissive=col, pattern=pat_all()), glow(mix(C(c2), np.array([0.6, 0.1, 0.05]), 0.5))]
    view = dict(view_pitch=0.2, view_yaw=0.0, zoom=1.0)

    def ov(img):
        if (t * 2) % 1 < 0.15:
            x, y = proj((0.0, 0.25, 0), view)
            px(img, x, y - 1, WHITE, 0.8)
    return parts, mats, view, ov


def graviton_crystal(c1, c2, t):
    # a small heavy crystal sitting in the funnel it presses into a sheet of spacetime; ripples run out of the well
    def sheet(p):
        r2 = p[..., 0] ** 2 + p[..., 2] ** 2
        f = -0.5 * np.exp(-r2 / 0.12) + 0.015 * np.sin(np.sqrt(r2) * 18 - t * TAU * 2)
        d = (np.abs(p[..., 1] - f) - 0.025) * 0.5
        return np.maximum(d, np.sqrt(r2) - 0.86)

    def grid(p, n):
        g = (np.abs(np.mod(p[..., 0] * 3.5, 1) - 0.5) < 0.07) | (np.abs(np.mod(p[..., 2] * 3.5, 1) - 0.5) < 0.07)
        r = np.sqrt(p[..., 0] ** 2 + p[..., 2] ** 2)
        ring = np.sin(r * 18 - t * TAU * 2) > 0.85
        return np.zeros(r.shape), (g | (ring & (r > 0.3))).astype(float)
    parts = [Part(sheet, 0, pos=(0, 0.05, 0)), Part(lambda p: sd_gem(p, 0.22, 0.46, 0.3), 1, pos=(0, -0.1, 0), rot=rotz(0.15) @ roty(t * TAU / 3 * 0))]
    mats = [Material('#182040', c2, metal=0.5, emissive=c2, pattern=grid),
            Material(c1, c2, metal=0.95, gloss=1.4, emissive=c2, pattern=pat_pulse_core(t))]
    return parts, mats, dict(view_pitch=0.5, view_yaw=0.5, zoom=1.0), None


def hyperdimensional_boule(c1, c2, t):
    # a 3D slice of a 4D boule turning through the fourth dimension, so its cross-section keeps changing shape;
    # the part of it that leans into +w glows magenta
    a, b, c = 0.7 + 0.45 * math.sin(TAU * t), 0.6 + 0.4 * math.sin(TAU * t * 2 + 1), TAU * t
    hb = np.array([0.36, 0.6, 0.36, 0.36])
    d = 0.45 * math.sin(TAU * t)

    def coords(p):
        x, y, z = p[..., 0], p[..., 1], p[..., 2]
        w = np.zeros(x.shape)
        x, w = x * math.cos(a) - w * math.sin(a), x * math.sin(a) + w * math.cos(a)
        z, w = z * math.cos(b) - w * math.sin(b), z * math.sin(b) + w * math.cos(b)
        y, w = y * math.cos(d) - w * math.sin(d), y * math.sin(d) + w * math.cos(d)
        x, z = x * math.cos(c) - z * math.sin(c), x * math.sin(c) + z * math.cos(c)
        return np.stack([x, y, z, w], -1)

    def shape(p):
        q = np.abs(coords(p)) - (hb - 0.06)
        return (np.sqrt((np.maximum(q, 0) ** 2).sum(-1)) + np.minimum(q.max(-1), 0) - 0.06) * 0.8

    def wcol(p, n):
        w = coords(p)[..., 3]
        return np.zeros(w.shape), (w > 0.1).astype(float)
    parts = [Part(shape, 0), Part(lambda p: sd_cyl(p, 0.08, 0.12, 0.02), 1, pos=(0, 0.8, 0))]
    mats = [Material(c1, c2, metal=0.8, gloss=1.4, emissive=c2, pattern=wcol), Material('#c0c8d0', '#ffffff', metal=1.0)]
    return parts, mats, dict(view_pitch=0.3, view_yaw=0.6, zoom=1.05), None


def hyperon_crystal(c1, c2, t):
    # a squat cuboctahedron, far too heavy: it thuds down twice per loop; every lattice site holds a three-quark triplet
    h = abs(math.sin(TAU * t))
    y = -0.12 + 0.12 * (1 - (1 - h) ** 2) ** 3

    def shape(p):
        return np.maximum(sd_box(p, (0.46, 0.46, 0.46)), sd_octa(p, 0.88))

    def triplets(p, n):
        g = np.floor(p * 2.5)
        f = p * 2.5 - g
        k = int(t * 6) % 3
        lit = np.zeros(p.shape[:-1], bool)
        for j in range(3):
            a = j * TAU / 3
            c = np.array([0.5 + 0.25 * math.cos(a), 0.5 + 0.25 * math.sin(a), 0.5])
            d = np.sqrt(((f[..., :2] - c[:2]) ** 2).sum(-1))
            d2 = np.sqrt(((f[..., 1:] - c[:2]) ** 2).sum(-1))
            dd = np.minimum(d, d2)
            lit |= (dd < (0.16 if j == k else 0.09))
        return np.zeros(p.shape[:-1]), lit.astype(float)
    parts = [Part(shape, 0, pos=(0, y, 0), rot=roty(0.4))]
    mats = [Material(c1, c2, metal=0.8, emissive=c2, pattern=triplets)]
    view = dict(view_pitch=0.4, view_yaw=0.5)

    def ov(img):
        if h < 0.12:   # dust kicked up when it lands
            for dx in (-12, -10, 10, 12):
                px(img, 15.5 + dx, 27 - (1 if abs(dx) == 12 else 0), C(c2), 0.6)
    return parts, mats, view, ov


def lattice_seed(c1, c2, t):
    # a tiny perfect nucleus; one by one atoms fly in and lock into the next layer, then the layer is absorbed
    dirs = [np.array(v, float) for v in ((1, 1, 1), (-1, 1, 1), (1, -1, 1), (-1, -1, 1), (1, 1, -1), (-1, 1, -1), (1, -1, -1), (-1, -1, -1))]
    order = [0, 7, 3, 4, 1, 6, 2, 5]
    parts = [Part(lambda p: sd_box(p, (0.17, 0.17, 0.17), 0.02), 0, rot=roty(0.4))]
    absorb = sstep((t - 0.85) / 0.13)
    for i, k in enumerate(order):
        st = 0.04 + i * 0.09
        prog = sstep((t - st) / 0.1)
        if prog <= 0:
            continue
        d = roty(0.4) @ (dirs[k] * 0.27)
        pos = d * (1 + (1 - prog) * 2.5)
        r = 0.12 * min(1, prog * 2) * (1 - absorb)
        if r > 0.005:
            parts.append(Part(lambda p, r=r: sd_sphere(p, r), 1, pos=pos))
    mats = [Material(c1, c2, metal=0.95, gloss=1.5, emissive=c2, pattern=pat_pulse_core(t)),
            Material(c2, c1, metal=0.6, emissive=c1, pattern=lambda p, n: (np.zeros(p.shape[:-1]), (n[..., 1] > 0.5).astype(float)))]
    view = dict(view_pitch=0.35, view_yaw=0.5, zoom=1.15)

    def ov(img):
        for i, k in enumerate(order):
            st = 0.04 + i * 0.09 + 0.1
            if 0 <= t - st < 0.04:
                x, y = proj(roty(0.4) @ (dirs[k] * 0.27), view)
                sparkle(img, int(x / 2), int(y / 2), C(c2))
    return parts, mats, view, ov


def law_crystal(c1, c2, t):
    # a bubble of new law, freezing from the bottom up into a faceted crystal, thawing, and freezing again
    yf = -0.65 + 1.3 * per(t)

    def shape(p):
        sph = sd_sphere(p, 0.55)
        fac = np.maximum(sd_octa(p, 0.82), sd_box(p, (0.5, 0.5, 0.5)))
        k = np.clip((yf - p[..., 1]) * 6, 0, 1)
        return sph * (1 - k) + fac * k

    def film(p, n):
        frozen = p[..., 1] < yf
        front = np.abs(p[..., 1] - yf) < 0.04
        iri = np.sin(p[..., 1] * 10 + p[..., 0] * 4 + t * TAU * 2) > 0.5
        frost = hsh(np.floor(p[..., 0] * 12), np.floor(p[..., 1] * 12), np.floor(p[..., 2] * 12)) > 0.85
        return np.where(frozen, 0.15, -0.1), (front | (~frozen & iri) | (frozen & frost)).astype(float)
    parts = [Part(shape, 0, rot=roty(0.5))]
    mats = [Material(c1, c2, metal=0.9, gloss=1.6, emissive=c2, pattern=film)]
    return parts, mats, dict(view_pitch=0.3, view_yaw=0.5), None


def moduli_crystal(c1, c2, t):
    # a crystal threaded with three tiny curled-up dimensions; their sizes (the moduli) breathe in and out
    parts = [Part(lambda p: sd_gem(p, 0.27, 0.9, 0.3), 0, rot=rotz(0.2))]
    for k, y in enumerate((-0.42, 0.0, 0.42)):
        R = 0.32 + 0.1 * math.sin(TAU * t + k * TAU / 3) - abs(y) * 0.25
        parts.append(Part(lambda p, R=R: sd_torus(p, R, 0.045), 1, pos=(rotz(0.2) @ np.array([0, y, 0])),
                          rot=rotz(0.2) @ rotx(0.35 * (k - 1)) @ roty(TAU * t * (1 if k % 2 else -1))))
    mats = [Material(mix(C(c1), C(c2), 0.35), c2, metal=0.9, gloss=1.4, emissive=c2, pattern=pat_bands(6, 1, t, 0.12, axis=1)),
            Material(c2, '#ffffff', metal=0.8, emissive=c2, pattern=lambda p, n: (np.zeros(p.shape[:-1]),
                     (np.sin(np.arctan2(p[..., 2], p[..., 0]) * 3 + t * TAU * 2) > 0.0).astype(float)))]
    return parts, mats, dict(view_pitch=0.3, view_yaw=0.5), None


def psionic_crystal(c1, c2, t):
    # a floating double-pointed crystal with a glowing eye; thought ripples spread out from it in rings
    def eye(p, n):
        r = np.sqrt(p[..., 0] ** 2 + p[..., 1] ** 2 * 1.5)
        front = n[..., 2] < -0.3
        iris = (r < 0.13) & front
        rings = (np.sin(r * 28 - t * TAU * 2) > 0.6) & front & (r < 0.35)
        return np.zeros(r.shape), (iris | rings).astype(float)
    parts = [Part(lambda p: sd_gem(p, 0.36, 0.85, 0.5), 0, rot=roty(0.0))]
    mats = [Material(c1, c2, metal=0.8, gloss=1.4, emissive=c2, pattern=eye)]
    view = dict(view_pitch=0.15, view_yaw=0.0, zoom=1.05)

    def ov(img):
        for k in range(2):
            life = (t * 2 + k * 0.5) % 1.0
            r = 7 + life * 10
            a = 1 - life
            for j in range(40):
                ang = j * TAU / 40
                only_empty(img, 15.5 + math.cos(ang) * r, 15.5 + math.sin(ang) * r * 0.7, C(c2), a)
    return parts, mats, view, ov


def pulsar_timing_crystal(c1, c2, t):
    # a tiny crystal star with two lighthouse beams sweeping round it; each time a beam faces you, it ticks
    a = TAU * t * 2
    R = roty(a) @ rotz(0.55)

    def beam(p):
        y = np.abs(p[..., 1])
        r = 0.02 + y * 0.14
        return np.maximum(np.sqrt(p[..., 0] ** 2 + p[..., 2] ** 2) - r, y - 0.95) * 0.9

    def tick(p, n):
        return np.zeros(p.shape[:-1]), (np.mod(np.abs(p[..., 1]) * 4 - t * 8, 1) < 0.5).astype(float)
    parts = [Part(lambda p: sd_octa(p, 0.3), 0, rot=R), Part(beam, 1, rot=R),
             Part(lambda p: sd_torus(p, 0.42, 0.025), 2, rot=R @ rotx(0.0))]
    mats = [Material(c1, c2, metal=0.9, gloss=1.5, emissive=c2, pattern=pat_pulse_core(t)),
            Material(c2, '#ffffff', emissive=c2, pattern=tick), glow(c1 if False else '#4080a0')]
    view = dict(view_pitch=0.3, view_yaw=0.0)

    def ov(img):
        facing = math.cos(a + math.pi / 2)
        for ph in (0.0, 0.5):
            local = (t * 2 + ph) % 1.0
            d = min(local, 1 - local)
            if d < 0.06:
                k = 1 - d / 0.06
                for rr in range(1, 5):
                    for dx, dy in ((rr, 0), (-rr, 0), (0, rr), (0, -rr)):
                        px(img, 15.5 + dx, 15.5 + dy, WHITE, k * (1 - rr / 5))
    return parts, mats, view, ov


def quantum_chlorophyll(c1, c2, t):
    # a crystalline leaf; captured photons fall on it and their energy runs down every vein at once to the centre
    def leaf(p):
        x, z = p[..., 0], p[..., 2]
        bend = 0.25 * x * x
        ves = np.maximum(np.sqrt((x - 0.62) ** 2 + z ** 2) - 0.95, np.sqrt((x + 0.62) ** 2 + z ** 2) - 0.95)
        return np.maximum(ves, np.abs(p[..., 1] - bend) - 0.035) * 0.9

    def veins(p, n):
        x, z = p[..., 0], p[..., 2]
        mid = np.abs(x) < 0.03
        side = np.abs(np.mod(z - np.abs(x) * 1.1, 0.24) - 0.12) < 0.025
        dist = np.abs(x) + np.abs(z - 0.0) * 0.4
        wave = np.mod(dist * 3 + t * 2, 1.0) < 0.35
        return np.where(mid | side, 0.1, 0.0), ((mid | side) & wave).astype(float)
    parts = [Part(leaf, 0, rot=roty(0.5) @ rotx(-0.1)),
             Part(lambda p: sd_capsule(p, (0, 0.0, -0.75), (0.05, -0.08, -0.98), 0.03), 1, rot=roty(0.5) @ rotx(-0.1))]
    mats = [Material(c1, c2, metal=0.7, gloss=1.4, emissive=c2, pattern=veins), Material('#406020', '#a0c060', metal=0.2)]
    view = dict(view_pitch=0.7, view_yaw=0.6, zoom=1.12)

    def ov(img):
        anim.ov_infall(img, t, 5, np.array([1.0, 0.95, 0.5]), 6)
    return parts, mats, view, ov


def retrocausal_crystal(c1, c2, t):
    # shards fly together into a crystal and apart again - the wrong way round; dust falls *into* it, glint runs backwards
    s = 1 - per(t)
    dirs = [(0.0, 1.0, 0.0), (0.9, -0.3, 0.2), (-0.9, -0.3, 0.2), (0.3, -0.4, -0.9), (-0.2, 0.1, 0.95)]
    parts = []
    for k, d in enumerate(dirs):
        d = np.array(d) / np.linalg.norm(d)
        off = d * (0.18 + 0.45 * s)
        parts.append(Part(lambda p: sd_gem(p, 0.18, 0.42, 0.3), 0, pos=off, rot=rotz(0.4 * k + s * 1.5) @ rotx(0.6 * k)))
    parts.append(Part(lambda p: sd_octa(p, 0.24), 1, rot=roty(0.5)))
    mats = [Material(c1, c2, metal=0.85, gloss=1.4, emissive=c2, pattern=pat_spots(1 - t, 0.9, 3)), glow(c2)]
    view = dict(view_pitch=0.3, view_yaw=0.5)

    def ov(img):
        rng = np.random.default_rng(7)
        for i in range(7):
            ph = rng.random()
            x0 = rng.uniform(4, 28)
            life = (t + ph) % 1.0
            y = 1 + life * 14
            x = x0 + (15.5 - x0) * life
            only_empty(img, x, y, C(c2), math.sin(life * math.pi))
        anim.ov_glint(img, (1 - t) % 1.0, 0, start=0.3)
    return parts, mats, view, ov


def scintillator_crystal(c1, c2, t):
    # a scintillator bar on a photomultiplier; each passing particle makes it flash, and a tally counts the flashes
    hits = (0.1, 0.42, 0.7)
    fl = max([max(0.0, 1 - abs(t - h0 - 0.03) / 0.06) for h0 in hits])
    col = mix(C(c1), np.array([0.85, 0.95, 1.0]), fl)
    parts = [Part(lambda p: sd_box(p, (0.6, 0.26, 0.26), 0.03), 0, pos=(-0.18, 0.05, 0)),
             Part(lambda p: sd_cyl(p, 0.22, 0.22, 0.04), 1, pos=(0.62, 0.05, 0), rot=rotz(TAU / 4)),
             Part(lambda p: sd_cyl(p, 0.12, 0.08, 0.02), 2, pos=(0.9, 0.05, 0), rot=rotz(TAU / 4))]
    mats = [Material(c1, c2, metal=0.7, gloss=1.5, emissive=col if fl > 0.1 else None, pattern=pat_all() if fl > 0.1 else
                     (lambda p, n: (0.2 * np.sin(p[..., 0] * 6 + p[..., 1] * 4), np.zeros(p.shape[:-1])))),
            Material('#404858', '#a0a8b8', metal=0.9), Material('#c8a040', '#fff0c0', metal=1.0)]
    view = dict(view_pitch=0.35, view_yaw=0.45, zoom=1.0)

    def ov(img):
        count = sum(1 for h0 in hits if t >= h0)
        for i, h0 in enumerate(hits):
            if 0 <= t - h0 < 0.06:   # the particle track crossing the bar
                s = (t - h0) / 0.06
                x0 = 6 + i * 6
                line(img, x0 + 4 - s * 2, 0 + s * 30, x0 + 4 - s * 2 - 2, 0 + s * 30 - 4, WHITE, 1.0)
        for i in range(count):
            for dy in range(3):
                px(img, 4 + i * 2, 26 + dy, C(c2), 1.0)
    return parts, mats, view, ov


def superposition_crystal(c1, c2, t):
    # two lattice configurations of one crystal occupying the same place; which one you see keeps changing
    s = math.sin(TAU * t)
    sa, sb = 1 + 0.1 * s, 1 - 0.1 * s
    parts = [Part(lambda p: gem_s(p, sa, 0.34, 0.9), 0, rot=rotz(0.45) @ roty(0.3)),
             Part(lambda p: gem_s(p, sb, 0.34, 0.9), 1, rot=rotz(-0.45) @ roty(-0.3)),
             Part(lambda p: sd_octa(p, 0.3), 2, rot=roty(TAU * t))]
    mats = [Material(c1, c2, metal=0.85, gloss=1.4, emissive=c1, pattern=pat_bands(8, 1, t, 0.12, axis=1)),
            Material(c2, c1, metal=0.85, gloss=1.4, emissive=c2, pattern=pat_bands(8, -1, t, 0.12, axis=1)),
            glow('#ffffff')]
    return parts, mats, dict(view_pitch=0.3, view_yaw=0.5), None


def topological_boule(c1, c2, t):
    # a boule sliced open at an angle: its interior is dead dark, while currents race round its surface in helices
    nrm = np.array([0.35, 1.0, -0.5])
    nrm = nrm / np.linalg.norm(nrm)

    def shape(p):
        body = sd_cyl(p, 0.36, 0.7, 0.18)
        cut = p @ nrm - 0.28
        return np.maximum(body, cut)

    def edge(p, n):
        on_cut = (n @ nrm) > 0.95
        a = np.arctan2(p[..., 2], p[..., 0])
        rho = np.sqrt(p[..., 0] ** 2 + p[..., 2] ** 2)
        hel = np.abs(wrap_np(a * 2 + p[..., 1] * 7 - t * TAU * 2)) < 0.5
        rim = on_cut & (rho > 0.29)
        return np.where(on_cut, -0.65, 0.05), ((hel & ~on_cut) | rim).astype(float)
    parts = [Part(shape, 0, rot=rotz(0.2))]
    mats = [Material('#7a5418', c2, metal=0.8, gloss=1.3, emissive='#fff4d0', pattern=edge)]
    return parts, mats, dict(view_pitch=0.4, view_yaw=0.5), None


# ================================================================================================ dampers
def graviton_harness(c1, c2, t):
    # a belt and two crossing straps around a dense dark weight; crushing gravity flows evenly down every strap
    sag = 0.03 * per(t, 2)

    def strap_em(p, n):
        return np.zeros(p.shape[:-1]), (np.mod(p[..., 1] * 4 + t * 4, 1.0) < 0.25).astype(float)
    parts = [Part(lambda p: sd_torus(p, 0.6, 0.075), 0, pos=(0, -0.2 - sag, 0)),
             Part(lambda p: np.maximum(ring_xy(p, 0.6, 0.06), -p[..., 1] - 0.0), 1, pos=(0, -0.2 - sag, 0)),
             Part(lambda p: np.maximum(ring_xy(p, 0.6, 0.06), -p[..., 1] - 0.0), 1, pos=(0, -0.2 - sag, 0), rot=roty(TAU / 4)),
             Part(lambda p: sd_box(p, (0.14, 0.12, 0.05), 0.03), 2, pos=(0, -0.2 - sag, -0.62)),
             Part(lambda p: sd_sphere(p, 0.27), 3, pos=(0, -0.12 - sag * 2, 0))]
    mats = [Material('#7880b0', c2, metal=0.5, pattern=pat_bands(16, 0, t, 0.1, axis=0)),
            Material('#5a60a0', c2, metal=0.6, emissive=c2, pattern=strap_em),
            Material('#c0c8d8', c2, metal=1.0, emissive=c2, pattern=pat_pulse_core(t)),
            Material('#101018', c2, metal=0.9, keep_dark=True)]
    return parts, mats, dict(view_pitch=0.35, view_yaw=0.6), None


def inertia_damper(c1, c2, t):
    # the housing shakes violently while the core inside, held on springs, stays perfectly still
    sx = 0.1 * math.sin(TAU * t * 3) + 0.04 * math.sin(TAU * t * 7)
    sy = 0.05 * math.sin(TAU * t * 5)
    hp = np.array([sx, sy, 0])
    parts = [Part(lambda p: np.maximum(np.abs(sd_cyl(p, 0.7, 0.16, 0.04)) - 0.06, -sd_cyl(p, 0.62, 1.0) * 0 - 1 + 0 * p[..., 0] + 0.0) if False else
                  np.maximum(sd_cyl(p, 0.74, 0.16, 0.04), -sd_cyl(p, 0.6, 1.0)), 0, pos=hp, rot=rotx(TAU / 4)),
             Part(lambda p: sd_sphere(p, 0.22), 1)]
    for k in range(4):
        a = k * TAU / 4 + math.pi / 4
        e = hp + np.array([math.cos(a) * 0.6, math.sin(a) * 0.6, 0])
        s = np.array([math.cos(a) * 0.2, math.sin(a) * 0.2, 0])
        parts.append(Part(lambda p, e=e, s=s: sd_capsule(p, s, e, 0.035), 2))
    mats = [Material(c1, c2, metal=0.9, pattern=pat_bands(12, 0, t, 0.1, axis=0)),
            Material(c2, '#ffffff', emissive=c2, pattern=pat_pulse_core(t)),
            Material('#c0c8d8', c2, metal=1.0, emissive=c2,
                     pattern=lambda p, n: (np.zeros(p.shape[:-1]), (np.sin(np.sqrt(p[..., 0] ** 2 + p[..., 1] ** 2) * 30) > 0.6).astype(float)))]
    view = dict(view_pitch=0.15, view_yaw=0.25)

    def ov(img):
        # motion lines on the side the housing is moving away from
        v = math.cos(TAU * t * 3)
        if abs(v) > 0.6:
            side = -1 if v > 0 else 1
            for y in (11, 16, 21):
                for j in range(3):
                    only_empty(img, 15.5 + side * (13 + j), y, C(c2), 0.7 - j * 0.2)
    return parts, mats, view, ov


# ================================================================================================ dots / quarks
def bottom_top_quarks(c1, c2, t):
    # a heavy gold top quark that decays before it can bind - bursting into three fragments - while a bottom quark circles
    a = TAU * t
    bpos = (0.55 * math.cos(a), 0.1 * math.sin(a * 2), 0.55 * math.sin(a))
    parts = [Part(lambda p: sd_sphere(p, 0.15), 1, pos=bpos)]
    if t < 0.5:
        r = 0.35 * sstep(t / 0.12) if t < 0.12 else 0.35 + 0.02 * math.sin(TAU * t * 6)
        parts.append(Part(lambda p: sd_sphere(p, r), 0))
    elif t < 0.56:
        parts.append(Part(lambda p: sd_sphere(p, 0.35 + (t - 0.5) * 4), 2))
    else:
        f = (t - 0.56) / 0.44
        for k in range(3):
            d = np.array([math.cos(k * TAU / 3 + 0.4), math.sin(k * TAU / 3 + 0.4), 0.3])
            parts.append(Part(lambda p, rr=0.12 * (1 - f) + 0.005: sd_sphere(p, rr), 3 if k else 1, pos=d * (0.1 + f * 0.8)))
    mats = [Material(c1, '#ffffff', metal=0.9, emissive=c1, pattern=pat_pulse_core(t)), glow('#8090ff'), glow('#ffffff'), glow(c1)]
    return parts, mats, dict(view_pitch=0.3, view_yaw=0.4), None


def fractal_dust(c1, c2, t):
    # a heap of Cantor dust: eight grains, each made of eight smaller grains, each made of eight smaller...
    def dust(p):
        z = p / 0.5
        s = 1.0
        for _ in range(3):
            z = np.abs(z)
            z = z * 3 - 2
            s *= 3
        return sd_box(z, (1, 1, 1), 0.15) / s * 0.5

    def em(p, n):
        g = np.floor(p * 13)
        h = hsh(g[..., 0], g[..., 1], g[..., 2])
        return np.zeros(h.shape), (np.sin(h * 50 + t * TAU) > 0.75).astype(float)
    parts = [Part(dust, 0, rot=rotx(0.5) @ roty(TAU * t / 4 * 0 + 0.6))]
    mats = [Material(c1, c2, metal=0.6, emissive=c2, pattern=em)]
    view = dict(view_pitch=0.35, view_yaw=0.5)

    return parts, mats, view, None


def quantum_dot(c1, c2, t):
    # three nanocrystals on a wafer: the smallest blue, a mid green one, and one that grows and shrinks, its colour following its size
    s = per(t)
    r = 0.16 + 0.16 * s
    cols = [np.array([0.35, 0.5, 1.0]), C(c2), np.array([1.0, 0.9, 0.3]), C(c1)]
    f = s * 3
    i = min(2, int(f))
    col = mix(cols[i], cols[i + 1], f - i)
    facet = lambda p, r: np.maximum(sd_sphere(p, r), sd_octa(p, r * 1.45))
    parts = [Part(lambda p: sd_box(p, (0.8, 0.06, 0.8), 0.02), 0, pos=(0, -0.3, 0)),
             Part(lambda p: facet(p, r), 1, pos=(0.12, -0.24 + r * 0.8, 0.1)),
             Part(lambda p: facet(p, 0.12), 2, pos=(-0.5, -0.16, -0.3)),
             Part(lambda p: facet(p, 0.18), 3, pos=(-0.42, -0.12, 0.42))]
    mats = [Material('#606878', '#a0a8b8', metal=0.9, pattern=pat_grid(5, 0, 0.05)),
            Material(col, '#ffffff', metal=0.6, emissive=col, pattern=pat_pulse_core(t)),
            Material(cols[0], '#ffffff', emissive=cols[0], pattern=pat_all()),
            Material(cols[1], '#ffffff', emissive=cols[1], pattern=pat_all())]
    return parts, mats, dict(view_pitch=0.55, view_yaw=0.6), None


def strange_charm_quarks(c1, c2, t):
    # a strange and a charm quark flicker into existence, whirl round each other for an instant on a gluon string, and vanish
    life = (t * 2) % 1.0
    ep = int(t * 2)
    e = math.sin(life * math.pi) ** 0.6
    a = TAU * life * 1.5 + ep * 2.0
    tilt = rotx(0.5 + ep * 0.6)
    p1 = tilt @ np.array([math.cos(a) * 0.5, 0, math.sin(a) * 0.5])
    parts = []
    if e > 0.05:
        parts = [Part(lambda p: sd_sphere(p, 0.26 * e), 0, pos=p1), Part(lambda p: sd_sphere(p, 0.26 * e), 1, pos=-p1),
                 Part(lambda p: sd_capsule(p, p1, -p1, 0.04 * e), 2)]
    else:
        parts = [Part(lambda p: sd_sphere(p, 0.03), 2)]
    mats = [Material(c1, '#ffffff', emissive=c1, pattern=pat_pulse_core(t)), Material(c2, '#ffffff', emissive=c2, pattern=pat_pulse_core(t)),
            Material('#ffffff', '#ffffff', emissive='#ffffff', pattern=pat_bands(18, 4, t, 0.3, axis=0))]
    view = dict(view_pitch=0.3, view_yaw=0.4)

    def ov(img):
        if life < 0.12 or life > 0.9:
            d = min(life, 1 - life)
            sparkle(img, 7, 7, C(c1) if ep == 0 else C(c2), 2 if d < 0.05 else 1)
    return parts, mats, view, ov


def turing_morphogen(c1, c2, t):
    # a pebble whose skin paints itself: leopard spots melt into stripes and back, driven by two morphogens, one fast and one slow
    m = per(t)

    def paint(p, n):
        x, y, z = p[..., 0] * 11, p[..., 1] * 11, p[..., 2] * 11
        spots = np.cos(x) + np.cos(0.5 * x + 0.866 * z) + np.cos(0.5 * x - 0.866 * z) + 0.6 * np.cos(y)
        stripes = 2.2 * np.cos(0.7 * x + 0.7 * z + 0.4 * y)
        f = spots * (1 - m) + stripes * m
        on = f > 1.1
        return np.where(on, 0.1, -0.05), on.astype(float)
    parts = [Part(lambda p: sd_sphere(p / np.array([0.75, 0.45, 0.62]), 1.0) * 0.45, 0, rot=rotz(0.15))]
    mats = [Material(c1, c2, metal=0.3, emissive=c2, pattern=paint)]
    view = dict(view_pitch=0.45, view_yaw=0.5)

    def ov(img):
        anim.ov_orbit(img, t, 1, C(c2), 1, rx=14, ry=6, speed=3)
        anim.ov_orbit(img, t, 2, np.array([0.9, 0.4, 0.2]), 1, rx=12, ry=9, speed=1)
    return parts, mats, view, ov


def up_down_quarks(c1, c2, t):
    # a proton: two red up quarks and a blue down quark, tied together by three wriggling gluon strings
    R = rotx(0.5) @ rotz(TAU * t)
    pos = [R @ np.array([math.cos(k * TAU / 3) * 0.48, math.sin(k * TAU / 3) * 0.48, 0]) for k in range(3)]
    parts = []
    for k in range(3):
        parts.append(Part(lambda p, c=pos[k]: sd_sphere(p - c, 0.19 if k < 2 else 0.2), 0 if k < 2 else 1))
        a = np.zeros(3)
        b = pos[k]
        parts.append(Part(lambda p, b=b: sd_capsule(p, np.zeros(3), b, 0.045), 2))
    parts.append(Part(lambda p: sd_sphere(p, 0.08), 3))

    def gluon(p, n):
        r = np.sqrt((p ** 2).sum(-1))
        return np.zeros(r.shape), (np.sin(r * 30 - t * TAU * 4) > 0.0).astype(float)
    mats = [Material(c1, '#ffffff', metal=0.5, emissive=c1, pattern=pat_pulse_core(t)),
            Material(c2, '#ffffff', metal=0.5, emissive=c2, pattern=pat_pulse_core(t + 0.5)),
            Material('#e0e060', '#ffffff', metal=0.5, emissive='#fff080', pattern=gluon), glow('#ffffff')]
    return parts, mats, dict(view_pitch=0.3, view_yaw=0.4), None


# ================================================================================================ emitters
def hawking_emitter(c1, c2, t):
    # a dish holding a tiny black horizon; virtual pairs pop up at its edge - one falls in, its partner escapes as glow
    def bowl(p):
        q = p - np.array([0, 0.35, 0])
        return np.maximum(np.abs(sd_sphere(q, 0.72)) - 0.045, q[..., 1] + 0.32)
    rr = 0.27 - 0.03 * per(t)
    parts = [Part(bowl, 0),
             Part(lambda p: sd_cyl(p, 0.1, 0.18), 1, pos=(0, -0.55, 0)),
             Part(lambda p: sd_cyl(p, 0.32, 0.05, 0.02), 1, pos=(0, -0.72, 0)),
             Part(lambda p: sd_sphere(p, rr), 2, pos=(0, 0.42, 0)),
             Part(lambda p: sd_torus(p, rr + 0.07, 0.03), 3, pos=(0, 0.42, 0), rot=rotx(1.25))]
    mats = [Material('#585868', c2, metal=0.9, emissive=c2, pattern=pat_bands(10, 1, t, 0.08, axis=1)),
            Material('#606878', '#a0a8b8', metal=0.9), black(), glow(c2)]
    view = dict(view_pitch=0.35, view_yaw=0.6)

    def ov(img):
        cx, cy = proj((0, 0.42, 0), view)
        for i in range(4):
            life = (t * 2 + i / 4) % 1.0
            ang = i * 1.7 + 0.4
            r_out = 5 + life * 10
            r_in = max(0.0, 5 - life * 6)
            only_empty(img, cx + math.cos(ang) * r_out, cy + math.sin(ang) * r_out * 0.8, C(c2), 1 - life)
            if r_in > 1:
                px(img, cx + math.cos(ang) * r_in, cy + math.sin(ang) * r_in * 0.8, np.array([0.3, 0.2, 0.4]), 0.8)
    return parts, mats, view, ov


def tachyon_klystron(c1, c2, t):
    # a klystron tube with resonant cavities; its beam bunches travel backwards, from the collector to the gun
    rot = rotz(0.3)

    def beam(p, n):
        return np.zeros(p.shape[:-1]), (np.mod(p[..., 1] * 3 + t * 4, 1.0) < 0.3).astype(float)
    parts = [Part(lambda p: sd_cyl(p, 0.13, 0.62), 0, rot=rot @ rotz(TAU / 4) @ np.eye(3)),
             Part(lambda p: sd_cyl(p, 0.24, 0.12, 0.04), 1, pos=rot @ np.array([-0.72, 0, 0]), rot=rot @ rotz(TAU / 4)),
             Part(lambda p: sd_sphere(p, 0.22), 1, pos=rot @ np.array([0.72, 0, 0])),
             Part(lambda p: sd_box(p, (0.1, 0.22, 0.14), 0.02), 1, pos=rot @ np.array([0.18, 0.32, 0]), rot=rot)]
    for x in (-0.38, -0.08, 0.22):
        parts.append(Part(lambda p: sd_torus(p, 0.25, 0.07), 2, pos=rot @ np.array([x, 0, 0]), rot=rot @ rotz(TAU / 4)))
    parts[0] = Part(lambda p: sd_cyl(p, 0.13, 0.62), 0, rot=rot @ rotz(TAU / 4))
    mats = [Material(c1, c2, metal=0.8, emissive=c2, pattern=beam), Material('#8a90a0', '#ffffff', metal=1.0),
            Material('#c08040', c2, metal=1.0, emissive=c2, pattern=lambda p, n: (np.zeros(p.shape[:-1]),
                     (np.sin(np.arctan2(p[..., 2], p[..., 0]) * 2 + t * TAU * 4) > 0.7).astype(float)))]
    view = dict(view_pitch=0.3, view_yaw=0.3)

    def ov(img):
        # an afterimage of the next bunch appearing *before* it gets there
        x0, y0 = proj(rot @ np.array([-0.9, 0, 0]), view)
        if (t * 4) % 1 < 0.3:
            only_empty(img, x0 - 2, y0, C(c2), 0.8)
            only_empty(img, x0 - 3, y0, C(c2), 0.4)
    return parts, mats, view, ov


# ================================================================================================ gems
def dilaton_crystal(c1, c2, t):
    # a gem grown on a reef, wrapped by one vibrating closed string; how hard it shakes is the coupling, frozen in
    def string(p):
        a = np.arctan2(p[..., 2], p[..., 0])
        rho = np.sqrt(p[..., 0] ** 2 + p[..., 2] ** 2)
        R = 0.5 + 0.07 * np.sin(a * 5 + t * TAU * 2)
        y = 0.08 * np.cos(a * 5 + t * TAU * 2)
        return np.sqrt((rho - R) ** 2 + (p[..., 1] - y) ** 2) * 0.7 - 0.035

    def reef(p):
        return sd_sphere(p / np.array([1, 0.5, 1]), 0.42) * 0.5 + 0.04 * np.sin(p[..., 0] * 15) * np.sin(p[..., 2] * 13)
    parts = [Part(lambda p: sd_gem(p, 0.3, 0.75, 0.3), 0, pos=(0, 0.1, 0), rot=rotz(0.12)),
             Part(string, 1, pos=(0, 0.08, 0), rot=rotx(0.35)),
             Part(reef, 2, pos=(0, -0.62, 0))]
    mats = [Material(c1, c2, metal=0.9, gloss=1.4, emissive=c2, pattern=pat_spots(t, 0.9, 6)), glow(c2),
            Material('#403050', c2, metal=0.2, emissive=c2, pattern=pat_spots(t, 0.85, 2))]
    return parts, mats, dict(view_pitch=0.3, view_yaw=0.5), None


def higgs_boson_crystal(c1, c2, t):
    # a frozen excitation rolling round the trough of the Higgs 'Mexican hat' potential
    a0 = 0.4

    def f(r):
        return np.minimum(6.0 * (r * r - a0 * a0) ** 2, 0.24) - 0.3

    def hat(p):
        r = np.sqrt(p[..., 0] ** 2 + p[..., 2] ** 2)
        d = (np.abs(p[..., 1] - f(r)) - 0.035) * 0.3
        return np.maximum(d, r - 0.8)

    def contours(p, n):
        r = np.sqrt(p[..., 0] ** 2 + p[..., 2] ** 2)
        return 0.25 * np.cos(r * 12), ((np.abs(np.mod(r * 4, 1) - 0.5) < 0.07) | (np.abs(np.mod(np.arctan2(p[..., 2], p[..., 0]) * 8 / TAU, 1) - 0.5) < 0.05)).astype(float)
    a = TAU * t
    ball = (a0 * math.cos(a), -0.3 + 0.22, a0 * math.sin(a))
    parts = [Part(hat, 0, pos=(0, -0.05, 0)), Part(lambda p: sd_octa(p, 0.27), 1, pos=(ball[0], ball[1] - 0.05, ball[2]), rot=roty(-a * 2))]
    mats = [Material('#2a4038', c1, metal=0.6, emissive=mix(C(c1), np.zeros(3), 0.45), pattern=contours),
            Material(mix(C(c1), C(c2), 0.5), c2, metal=0.95, gloss=1.5, emissive=c2, pattern=pat_pulse_core(t))]
    return parts, mats, dict(view_pitch=0.42, view_yaw=0.5, zoom=1.05), None


def lorentz_transform_crystal(c1, c2, t):
    # a crystal flattened by length contraction, flying at 0.99c: blue-shifted ahead, red-shifted behind, the world streaking past
    def shape(p):
        return sd_gem(p / np.array([0.42, 1.0, 1.0]), 0.62, 0.9, 0.3) * 0.42

    def doppler(p, n):
        x = p[..., 0]
        front = x < -0.1
        back = x > 0.12
        return np.where(back, -0.25, 0.0), (front & (np.sin(p[..., 1] * 14 + t * TAU * 4) > -0.3)).astype(float)
    parts = [Part(lambda p: np.maximum(shape(p), p[..., 0]), 0), Part(lambda p: np.maximum(shape(p), -p[..., 0]), 1)]
    mats = [Material(mix(C(c1), C(c2), 0.4), c2, metal=0.9, gloss=1.4, emissive=mix(C(c2), WHITE, 0.4), pattern=doppler),
            Material('#803040', '#ff9080', metal=0.9, gloss=1.4)]
    view = dict(view_pitch=0.15, view_yaw=0.0)

    def ov(img):
        rng = np.random.default_rng(3)
        for i in range(9):
            y = int(rng.integers(2, 30))
            ph = rng.random()
            ln = int(rng.integers(3, 7))
            x = 34 - ((t * 2 + ph) % 1.0) * 44
            for j in range(ln):
                col = np.array([1.0, 0.45, 0.35]) if j > ln // 2 else C(c2)
                only_empty(img, x + j, y, col, 0.9 - j / ln * 0.6)
    return parts, mats, view, ov


def novikov_crystal(c1, c2, t):
    # a gem held in a faceted ring-loop; a light goes round the loop and meets its own past self, consistently, forever
    def ring(p):
        rho = np.sqrt(p[..., 0] ** 2 + p[..., 2] ** 2)
        q = np.stack([rho - 0.58, p[..., 1]], -1)
        q = np.abs(q)
        hexd = np.maximum(q[..., 0] * 0.866 + q[..., 1] * 0.5, q[..., 1]) - 0.08
        return hexd

    def light(p, n):
        a = np.arctan2(p[..., 2], p[..., 0])
        h1 = np.abs(wrap_np(a - t * TAU)) < 0.4
        h2 = np.abs(wrap_np(a - t * TAU + math.pi)) < 0.25
        trail = np.abs(wrap_np(a - t * TAU + 0.6)) < 0.25
        return np.zeros(a.shape), (h1 | h2 | trail).astype(float)
    parts = [Part(ring, 0, rot=rotx(0.9) @ rotz(0.2)), Part(lambda p: sd_gem(p, 0.2, 0.6, 0.3), 1, rot=rotz(-0.15))]
    mats = [Material('#6a4818', c2, metal=0.9, emissive='#fff4d0', pattern=light),
            Material(c2, '#ffffff', metal=0.9, gloss=1.4, emissive='#ffb040', pattern=pat_pulse_core(t))]
    return parts, mats, dict(view_pitch=0.3, view_yaw=0.5), None


# ================================================================================================ registry
ITEMS = {
    'chronal_anchor': chronal_anchor, 'chronon_capacitor': chronon_capacitor,
    'feynman_diagram': feynman_diagram, 'formal_proof_tree': formal_proof_tree, 'godel_sentence': godel_sentence,
    'source_code_fragment': source_code_fragment,
    'primordial_charter': primordial_charter, 'quantum_codex': quantum_codex,
    'atto_inductor': atto_inductor, 'dipole_winding': dipole_winding, 'femto_inductor': femto_inductor,
    'superconducting_coil': superconducting_coil,
    'annihilation_core': annihilation_core, 'antimatter_pellet': antimatter_pellet, 'coherence_stabiliser': coherence_stabiliser,
    'genome_sample': genome_sample, 'noble_gas_clathrate': noble_gas_clathrate, 'stem_cell_culture': stem_cell_culture,
    'tardigrade_culture': tardigrade_culture, 'xenogenome_sample': xenogenome_sample,
    'aleph_null_shard': aleph_null_shard, 'bekenstein_crystal': bekenstein_crystal, 'cherenkov_boule': cherenkov_boule,
    'cherenkov_quartz': cherenkov_quartz, 'chiral_crystal': chiral_crystal, 'dilithium_crystal': dilithium_crystal,
    'electride_crystal': electride_crystal, 'exotic_matter_crystal': exotic_matter_crystal,
    'folded_protein_crystal': folded_protein_crystal, 'fractal_core': fractal_core, 'frozen_photon': frozen_photon,
    'graviton_crystal': graviton_crystal, 'hyperdimensional_boule': hyperdimensional_boule, 'hyperon_crystal': hyperon_crystal,
    'lattice_seed': lattice_seed, 'law_crystal': law_crystal, 'moduli_crystal': moduli_crystal, 'psionic_crystal': psionic_crystal,
    'pulsar_timing_crystal': pulsar_timing_crystal, 'quantum_chlorophyll': quantum_chlorophyll,
    'retrocausal_crystal': retrocausal_crystal, 'scintillator_crystal': scintillator_crystal,
    'superposition_crystal': superposition_crystal, 'topological_boule': topological_boule,
    'graviton_harness': graviton_harness, 'inertia_damper': inertia_damper,
    'bottom_top_quarks': bottom_top_quarks, 'fractal_dust': fractal_dust, 'quantum_dot': quantum_dot,
    'strange_charm_quarks': strange_charm_quarks, 'turing_morphogen': turing_morphogen, 'up_down_quarks': up_down_quarks,
    'hawking_emitter': hawking_emitter, 'tachyon_klystron': tachyon_klystron,
    'dilaton_crystal': dilaton_crystal, 'higgs_boson_crystal': higgs_boson_crystal,
    'lorentz_transform_crystal': lorentz_transform_crystal, 'novikov_crystal': novikov_crystal,
}

# motions + overlays. Items whose builder overlay projects 3D points use no view-changing motion.
RECIPES = {
    'chronal_anchor': ((), ()),
    'chronon_capacitor': ((), ()),
    'feynman_diagram': (('rock',), ()),
    'formal_proof_tree': ((), ()),
    'godel_sentence': ((), ()),
    'source_code_fragment': ((), ('glitch',)),
    'primordial_charter': (('rock',), ('sparkles',)),
    'quantum_codex': ((), ('glint',)),
    'atto_inductor': (('spin',), ()),
    'dipole_winding': (('rock',), ('glint',)),
    'femto_inductor': ((), ()),
    'superconducting_coil': ((), ('sparkles',)),
    'annihilation_core': ((), ()),
    'antimatter_pellet': ((), ()),
    'coherence_stabiliser': (('spin',), ()),
    'genome_sample': (('rock',), ('glint',)),
    'noble_gas_clathrate': (('spin',), ('sparkles',)),
    'stem_cell_culture': (('rock',), ()),
    'tardigrade_culture': ((), ()),
    'xenogenome_sample': (('hue_soft',), ('glitch',)),
        'bekenstein_crystal': (('spin',), ()),
    'cherenkov_boule': (('rock',), ()),
    'cherenkov_quartz': ((), ()),
    'chiral_crystal': (('spin',), ('glint',)),
    'dilithium_crystal': (('heartbeat',), ('sparkles',)),
    'electride_crystal': (('rock',), ('orbit',)),
    'exotic_matter_crystal': ((), ('embers',)),
    'folded_protein_crystal': (('rock',), ('sparkles',)),
    'fractal_core': (('spin', 'breathe'), ()),
    'frozen_photon': ((), ()),
    'graviton_crystal': (('rock',), ()),
    'hyperdimensional_boule': ((), ('sparkles',)),
    'hyperon_crystal': ((), ()),
    'lattice_seed': ((), ()),
    'law_crystal': (('rock',), ('embers',)),
    'moduli_crystal': (('rock',), ()),
    'psionic_crystal': (('breathe',), ()),
    'pulsar_timing_crystal': ((), ()),
    'quantum_chlorophyll': ((), ()),
    'retrocausal_crystal': ((), ()),
    'scintillator_crystal': ((), ()),
    'superposition_crystal': (('tumble', 'hue'), ('glitch',)),
    'topological_boule': (('spin',), ()),
    'graviton_harness': (('rock',), ()),
    'inertia_damper': ((), ()),
    'bottom_top_quarks': (('tumble',), ()),
    'fractal_dust': (('spin',), ('sparkles',)),
    'quantum_dot': (('rock',), ()),
    'strange_charm_quarks': ((), ()),
    'turing_morphogen': ((), ()),
    'up_down_quarks': ((), ('orbit',)),
    'hawking_emitter': ((), ()),
    'tachyon_klystron': ((), ()),
    'dilaton_crystal': (('rock',), ('sparkles',)),
    'higgs_boson_crystal': ((), ('glint',)),
    'lorentz_transform_crystal': ((), ()),
    'novikov_crystal': (('rock',), ()),
}

# replaces the old shared fractal-shard story: an endless receding row of shards (80-frame loop)
NARRATIVES = {'aleph_null_shard': aleph_null_shard}

# slower loops for the items that tell a little sequence
FRAMES = {
    'tardigrade_culture': 80, 'lattice_seed': 80, 'annihilation_core': 60, 'formal_proof_tree': 80,
    'primordial_charter': 80, 'quantum_codex': 60, 'bottom_top_quarks': 60, 'scintillator_crystal': 60,
    'chronon_capacitor': 64, 'retrocausal_crystal': 60,
}
