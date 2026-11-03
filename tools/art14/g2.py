"""Item art group 2 (round 14): transformation loops for the dense ingots and the late-game cores.
Every item tells a short story in a 60..72 frame loop (collapse/rebound, boil/crush, dissolve into a galaxy, shatter and
re-pin, descend/repel/flip, unify/break symmetry, open/close, roll off the hat, unfold to a net, curve spacetime,
annihilate/pair-produce, flicker between universes). All loops are seamless: every phase function is equal at t=0 and t=1.
"""
import math
import numpy as np
from items3d import *
import anim

PI = math.pi


# ------------------------------------------------------------------------------------------- helpers
def _ss(x):
    x = min(max(x, 0.0), 1.0)
    return x * x * (3 - 2 * x)


def _seg(t, a, b):
    return _ss((t - a) / (b - a))


def _bump(t, c, w):
    """Smooth 0..1..0 pulse centred on c with half-width w (not wrapped)."""
    return math.exp(-((t - c) / w) ** 2)


def _hs(a):
    v = np.sin(a * 12.9898 + 4.1) * 43758.5453
    return v - np.floor(v)


def _dot(img, x, y, rgb, a=1.0):
    anim._blend(img, int(round(y)), int(round(x)), np.asarray(rgb, float), min(1.0, max(0.0, a)))


def _dot_empty(img, x, y, rgb, a=1.0):
    yi, xi = int(round(y)), int(round(x))
    if 0 <= yi < img.shape[0] and 0 <= xi < img.shape[1] and img[yi, xi, 3] < 0.5:
        _dot(img, x, y, rgb, a)


def _line(img, x0, y0, x1, y1, rgb, a0=1.0, a1=1.0, empty=False):
    n = int(max(abs(x1 - x0), abs(y1 - y0))) + 1
    for i in range(n + 1):
        u = i / max(n, 1)
        f = _dot_empty if empty else _dot
        f(img, x0 + (x1 - x0) * u, y0 + (y1 - y0) * u, rgb, a0 + (a1 - a0) * u)


def _ring(img, cx, cy, r, rgb, a=1.0, ry=None, empty=False):
    ry = r if ry is None else ry
    n = max(12, int(r * 7))
    for i in range(n):
        ang = i * TAU / n
        f = _dot_empty if empty else _dot
        f(img, cx + math.cos(ang) * r, cy + math.sin(ang) * ry, rgb, a)


def _star(img, x, y, rgb, s):
    """Four-point flare of arm length s (pixels, 32px image)."""
    _dot(img, x, y, WHITE, 1)
    arm = int(round(s))
    for d in range(1, arm + 1):
        a = 1 - d / (arm + 1)
        for dx, dy in ((d, 0), (-d, 0), (0, d), (0, -d)):
            _dot(img, x + dx, y + dy, rgb, a)


def _fade(img, a):
    img[..., 3] *= a


def _whiten(img, k):
    m = img[..., 3] > 0
    img[m, :3] = img[m, :3] * (1 - k) + k


def _proj(pt, view):
    cam = rotx(-view.get('view_pitch', 0.5)) @ roty(view.get('view_yaw', 0.7853))
    c = np.asarray(pt, float) @ cam.T
    k = view.get('zoom', 1.0) / view.get('extent', 1.0)
    return (c[0] * k + 1) / 2 * 32 - 0.5, (-c[1] * k + 1) / 2 * 32 - 0.5


def _ing(p, L=0.85, Hh=0.3, W=0.45):
    return np.maximum(sd_box(p, (L, Hh, W), 0.05), (p[..., 1] * 0.95 + np.abs(p[..., 2]) * 0.6 + np.abs(p[..., 0]) * 0.12 - (Hh + 0.12)))


def _scaled(fn, s):
    """Non-uniform scale (sx, sy, sz) or uniform s of an SDF."""
    if np.isscalar(s):
        return lambda p: fn(p / s) * s
    s = np.asarray(s, float)
    return lambda p: fn(p / s) * float(s.min())


def _zero(p):
    return np.zeros(p.shape[:-1])


def _hue(rgb, deg):
    return np.clip(np.asarray(rgb, float) @ anim.hue_matrix(deg).T, 0, 1)


def _glowmat(col, edge='#ffffff'):
    return Material(col, edge, emissive=col, pattern=lambda p, n: (_zero(p), np.ones(p.shape[:-1])))


ING_VIEW = dict(view_pitch=0.6, view_yaw=0.65, zoom=1.05)
N = ()


# =========================================================================================== neutronium
def neutronium_ingot(c1, c2, t):
    # so dense it cannot keep its shape: the ingot sags, collapses to a spinning neutron-star point whose pulsar beams
    # sweep round, then rebounds (overshoot, wobble) back into an ingot; starlight orbits get dragged in and flung out
    sag = _seg(t, 0.28, 0.42)
    col = _seg(t, 0.40, 0.52)
    reb = _seg(t, 0.64, 0.82)
    if t < 0.58:
        s = 1 - 0.97 * col
        sv = (s * (1 + 0.15 * sag), s * (1 - 0.55 * sag * (1 - col)), s * (1 + 0.1 * sag))
    else:
        r = reb
        s = max(0.03, r + 0.28 * math.sin(r * TAU) * (1 - r) * (r > 0))
        sv = (s, s * (1 - 0.15 * math.sin(r * PI * 3) * (1 - r)), s)
    point = 0.5 < t < 0.68
    heat = max(col * (1 - reb) if t < 0.6 else (1 - reb), 0)

    def sink(p, n):
        rr = np.sqrt(p[..., 0] ** 2 * 0.5 + p[..., 2] ** 2)
        v = np.sin(rr * 22 + t * TAU * 4)
        return -0.3 - 0.12 * (v > 0.6) + 0.5 * heat, ((v > 0.85 - 1.2 * heat) & (p[..., 1] > 0.18 - heat)).astype(float)
    parts, mats = [], [Material(c1, c2, metal=0.7, gloss=0.45, emissive=mix(H(c2), H(c1), 0.3 * (1 - heat)), pattern=sink),
                       _glowmat(c2)]
    if min(sv) > 0.06:
        parts.append(Part(_scaled(lambda p: _ing(p, 0.86, 0.22, 0.46), sv), 0, rot=roty(heat * 1.5)))
    if point:
        parts.append(Part(lambda p: sd_sphere(p, 0.13 + 0.03 * math.sin(t * TAU * 12)), 1))
    view = ING_VIEW

    def ov(img):
        drag = s
        for k in range(2):                               # starlight dragged round (and into) the ingot
            for j in range(7):
                a = TAU * (2 * t + k / 2) - j * 0.12 * (1 + 2 * (col if t < 0.58 else 1 - reb))
                x = 15.5 + math.cos(a) * 14 * (0.25 + 0.75 * drag)
                y = 15.8 + (math.sin(a) * 9 - math.cos(a) * 2) * (0.25 + 0.75 * drag)
                _dot_empty(img, x, y, WHITE if j == 0 else H(c2), 1.0 - j / 6)
        if point:                                        # pulsar: two beams sweeping round twice
            u = (t - 0.5) / 0.18
            env = math.sin(u * PI)
            a = u * TAU * 2 + 0.4
            for sgn in (1, -1):
                _line(img, 15.5, 15.5, 15.5 + sgn * math.cos(a) * 15, 15.5 + sgn * math.sin(a) * 15 * 0.6, H(c2), env, 0.1 * env, empty=True)
            _star(img, 15.5, 15.5, H(c2), 1 + 2 * env)
        fl = _bump(t, 0.68, 0.035)                       # rebound shock ring
        if fl > 0.05:
            _ring(img, 15.5, 15.5, 4 + (t - 0.6) * 120, WHITE, fl, ry=(4 + (t - 0.6) * 120) * 0.6, empty=True)
    return parts, mats, view, ov


# =========================================================================================== metallic hydrogen
_MH_B = [(-0.55, 0.05, -0.15), (-0.2, 0.15, 0.2), (0.2, 0.0, -0.2), (0.55, 0.1, 0.12), (0.0, -0.1, 0.0), (-0.35, -0.12, 0.25), (0.35, -0.1, 0.3)]


def metallic_hydrogen_ingot(c1, c2, t):
    # metastable: the mercury-bright ingot boils back into hydrogen bubbles that drift apart; pressure plates slam in,
    # crush the gas, and a lightning flash marks the instant it becomes a metal again
    e = _seg(t, 0.32, 0.48) if t < 0.6 else 1 - _seg(t, 0.62, 0.8)
    s = 1 - e
    flash = _bump(t, 0.8, 0.03)
    step = int(t * 16) % 16

    def zap(p, n):
        x, z = p[..., 0], p[..., 2]
        k = _hs(np.float64(step) * 3.7)
        path = 0.15 * np.sin(x * 8 + k * 20) + 0.08 * np.sin(x * 23 + k * 40) + (k - 0.5) * 0.3
        arc = (np.abs(z - path) < 0.04 + 0.1 * flash) & (np.abs(x - (k - 0.5)) < 0.45 + flash) & ((step % 2 == 0) | (flash > 0.3))
        return 0.2 * np.sin(x * 6 - t * TAU * 4) + 0.6 * flash, arc.astype(float)
    wob = lambda p: _ing(p) + (0.03 + 0.05 * e) * np.sin(p[..., 0] * 7 - t * TAU * 4) * (p[..., 1] > 0)
    parts = []
    mats = [Material(c1, c2, metal=1.0, gloss=1.7, emissive='#e0f8ff', pattern=zap),
            Material('#a8d8ff', '#ffffff', metal=0.1, gloss=2.0, emissive='#e8f8ff',
                     pattern=lambda p, n: (_zero(p), (p[..., 1] > 0.6 * length(p)).astype(float)))]
    if s > 0.05:
        parts.append(Part(_scaled(wob, (s, s * (0.6 + 0.4 * s), s)), 0))
    if e > 0.04:
        for k, b in enumerate(_MH_B):
            r = 0.05 + 0.12 * e * (0.8 + 0.4 * _hs(k * 1.3))
            w = math.sin(TAU * t * 3 + k * 1.7) * 0.05 * e
            pos = (b[0] * (0.6 + 0.7 * e) + w, b[1] * (0.6 + 1.5 * e) + 0.2 * e * _hs(k * 2.1), b[2] * (0.6 + 0.9 * e) - w)
            parts.append(Part(lambda p, r=r: sd_sphere(p, r), 1, pos=pos))

    def ov(img):
        cr = _seg(t, 0.6, 0.78) * (1 - _seg(t, 0.8, 0.88))        # pressure plates closing in from four sides
        if cr > 0.02:
            g = 15 - 9 * _seg(t, 0.62, 0.79)
            for sx, sy in ((1, 1), (1, -1), (-1, 1), (-1, -1)):
                cx, cy = 15.5 + sx * g, 15.5 + sy * g * 0.8
                _line(img, cx, cy, cx - sx * 4, cy, H(c2), cr, cr * 0.3)
                _line(img, cx, cy, cx, cy - sy * 4, H(c2), cr, cr * 0.3)
        if flash > 0.1:
            _whiten(img, 0.5 * flash)
            for k in range(5):
                a = k * TAU / 5 + 0.3
                x0, y0 = 15.5 + math.cos(a) * 6, 15.5 + math.sin(a) * 5
                x1, y1 = 15.5 + math.cos(a + 0.3) * 13, 15.5 + math.sin(a - 0.3) * 11
                _line(img, x0, y0, x1, y1, H('#c0f0ff'), flash, 0.2 * flash, empty=True)
    return parts, mats, ING_VIEW, ov


# =========================================================================================== cosmic neutronium
_CN_P = [(-0.6, 0.32, -0.2), (-0.3, 0.33, 0.15), (0.0, 0.34, -0.1), (0.35, 0.33, 0.2), (0.62, 0.3, -0.15), (-0.75, 0.1, 0.4),
         (0.75, 0.0, 0.4), (-0.4, 0.0, 0.47), (0.1, -0.05, 0.47), (0.45, 0.1, 0.47), (-0.1, 0.33, 0.3), (0.2, 0.33, -0.3)]


def cosmic_neutronium_ingot(c1, c2, t):
    # the black ingot lets its starlight out: it fades into the dark while its stars swirl into a little spiral galaxy,
    # which then falls back in on itself and condenses into the ingot again (an outline flash as it re-solidifies)
    out = _seg(t, 0.3, 0.45) if t < 0.62 else 1 - _seg(t, 0.66, 0.84)
    a_body = 1 - out
    s = 1 - 0.35 * out

    def stars(p, n):
        x = p[..., 0] * 7 - t * 12
        cx = np.mod(np.floor(x), 6)
        h = _hs(cx * 7.1 + np.floor(p[..., 1] * 7) * 13.7 + np.floor(p[..., 2] * 7) * 3.3)
        fx = x - np.floor(x)
        tw = np.sin(h * 50 + t * TAU * 4) > -0.3
        return np.full(h.shape, -0.4), ((h > 0.78 - 0.3 * out) & tw & (np.abs(fx - 0.5) < 0.4)).astype(float)
    parts = [Part(_scaled(_ing, s), 0)] if a_body > 0.03 else []
    mats = [Material('#0c0c16', '#5060a0', metal=0.5, gloss=0.5, keep_dark=True, emissive=c2, pattern=stars)]

    def ov(img):
        _fade(img, a_body)
        reform = _bump(t, 0.84, 0.03)
        if reform > 0.1:                                  # outline flash as the ingot condenses
            sol = img[..., 3] > 0.3
            edge = sol & ~(np.roll(sol, 1, 0) & np.roll(sol, -1, 0) & np.roll(sol, 1, 1) & np.roll(sol, -1, 1))
            img[edge, :3] = img[edge, :3] * (1 - reform) + H('#a0b0ff') * reform
            img[edge, 3] = np.maximum(img[edge, 3], reform)
        if out < 0.02:
            return
        spin = TAU * 1.5 * _seg(t, 0.3, 0.84)
        for k, pt in enumerate(_CN_P):
            x0, y0 = _proj(np.array(pt) * s, ING_VIEW)
            arm = k % 2
            rr = 2.5 + (k // 2) * 2.2
            a = spin + arm * PI + rr * 0.35
            x1, y1 = 15.5 + math.cos(a) * rr, 15.5 + math.sin(a) * rr * 0.55
            x, y = x0 + (x1 - x0) * out, y0 + (y1 - y0) * out
            tw = 0.6 + 0.4 * math.sin(TAU * (t * 4 + k * 0.37))
            _dot(img, x, y, WHITE if k % 3 == 0 else H('#c8d4ff'), tw * min(1, out * 2))
            if out > 0.6 and k % 3 == 0:
                _star(img, x, y, H('#8090ff'), 1)
            for j in range(1, 3):                         # short trails along the spiral
                aj = a - j * 0.25
                _dot_empty(img, x - (x1 - (15.5 + math.cos(aj) * rr)) * out, y - (y1 - (15.5 + math.sin(aj) * rr * 0.55)) * out,
                           H('#7080e0'), 0.8 * out * (1 - j / 3))
        core = out * (0.7 + 0.3 * math.sin(t * TAU * 6))
        _star(img, 15.5, 15.5, H('#d0d8ff'), 1 + 1.5 * core)
    return parts, mats, ING_VIEW, ov


# =========================================================================================== crystalline matrix
def crystalline_matrix_ingot(c1, c2, t):
    # a hexagonal crystal bar pinned by the light of two dead stars: the two light fronts meet in the middle, the
    # lattice unpins and the bar breaks into six cells that turn freely; the starlight returns and re-pins them
    e = _seg(t, 0.38, 0.5) if t < 0.62 else 1 - _seg(t, 0.64, 0.8)
    pin = _bump(t, 0.8, 0.025) + _bump(t, 0.38, 0.025)
    fa = None
    if t < 0.38:
        fa = -0.9 + 0.9 * (t / 0.38)
    elif 0.62 <= t < 0.8:
        fa = -0.9 + 0.9 * ((t - 0.62) / 0.18)
    n = 6
    L = 0.82
    w = 2 * L / n
    parts = []
    mats = []
    BASE = rotz(PI / 2)
    for k in range(n):
        xk = -L + w * (k + 0.5)

        def light(p, nn, xk=xk):
            x = -p[..., 1] + xk                          # hexprism axis is local y, mapped to world -x
            front = np.zeros(x.shape, bool)
            if fa is not None:
                front = (np.abs(x - fa) < 0.07) | (np.abs(x + fa) < 0.07)
            g = np.abs(np.mod(x * 5, 1) - 0.5) < 0.06
            edge = np.abs(p[..., 1]) > w / 2 - 0.05 - 0.02 * e
            return 0.2 * g + 0.6 * pin, (front | (edge & (e > 0.05)) | (pin > 0.5)).astype(float)
        mats.append(Material(c1, c2, metal=0.6, gloss=1.5, emissive=c2, pattern=light))
        sp = (k - 2.5) * 0.9 * e + e * TAU * 0.5 * (1 if k % 2 else -1) * _seg(t, 0.4, 0.66)
        pos = (xk * (1 + 0.45 * e), 0.18 * e * math.sin(k * 2.1 + 1), 0.22 * e * math.cos(k * 1.7))
        R = rotx(sp) @ rotz(0.5 * e * math.sin(k * 1.3)) @ BASE @ roty(PI / 6)
        parts.append(Part(lambda p: sd_hexprism(p, 0.3, w / 2 - 0.01 - 0.03 * e), k, pos=pos, rot=R))
    view = dict(view_pitch=0.45, view_yaw=0.6, zoom=1.0 - 0.12 * e)

    def ov(img):
        if pin > 0.1:
            for x in (5.5, 25.5):
                _star(img, x, 15.5 + (x - 15.5) * 0.25, H(c2), 2 * pin)
    return parts, mats, view, ov


# =========================================================================================== anti-iron
def antiiron_ingot(c1, c2, t):
    # anti-iron refined in levitation: it sinks toward its mould, the field ring flares and sparks crackle in the
    # shrinking gap (it must never touch), it is flung up with a full barrel roll, glowing inverted, then settles
    down = _seg(t, 0.25, 0.46)
    up = _seg(t, 0.5, 0.64)
    y = 0.16 - 0.3 * down + 0.3 * up + 0.16 * math.sin(PI * _seg(t, 0.5, 0.86)) + 0.025 * math.sin(TAU * t * 2)
    roll = TAU * _seg(t, 0.52, 0.8)
    inv = math.sin(PI * _seg(t, 0.5, 0.82))
    near = _bump(t, 0.48, 0.04)
    base = mix(H(c1), np.clip(1 - H(c1) * 0.9, 0, 1) * np.array([0.7, 0.85, 1.0]), inv)
    parts = [Part(lambda p: _ing(p, 0.8, 0.26, 0.42), 0, pos=(0, y, 0), rot=rotx(roll) @ roty(0.12 * math.sin(t * TAU))),
             Part(lambda p: sd_torus(p, 0.7, 0.035 + 0.02 * near), 1, pos=(0, -0.42, 0), rot=roty(t * TAU)),
             Part(lambda p: np.maximum(sd_box(p, (0.92, 0.06, 0.55), 0.02), -sd_box(p - np.array([0, 0.06, 0]), (0.84, 0.05, 0.47))), 2,
                  pos=(0, -0.5, 0))]
    mats = [Material(base, c2, metal=0.92, emissive=c2, pattern=lambda p, n: (pat_noise_shade(10, 0.12, 3)(p, n)[0],
                                                                            (np.abs(np.mod(p[..., 0] * 3 + t * 6, 1) - 0.5) < 0.08 * inv * 2).astype(float))),
            Material('#80d0ff', '#ffffff', emissive=mix(H('#80d0ff'), WHITE, near), pattern=pat_bands(10, 4, t, 0.25 + 0.25 * near, axis=0)),
            Material('#3a3440', '#8a8090', metal=0.7)]
    view = dict(view_pitch=0.5, view_yaw=0.65, zoom=0.98)

    def ov(img):
        if near > 0.1:
            for k in range(7):
                x = 15.5 + (_hs(k * 3.1 + int(t * 40)) - 0.5) * 22
                yy = 19 + (_hs(k * 5.7 + int(t * 40)) - 0.5) * 4
                _dot(img, x, yy, WHITE if k % 2 else H(c2), near)
                _dot(img, x + 1, yy - 1, H('#ffe0a0'), near * 0.6)
        tr = math.sin(PI * _seg(t, 0.52, 0.64))
        if tr > 0.1:                                     # afterimage streak below while flung up
            for j in range(3):
                _dot_empty(img, 7 + j * 9, 24 - j, H(c2), tr * 0.6)
                _dot_empty(img, 8 + j * 9, 26 - j, H(c2), tr * 0.4)
    return parts, mats, view, ov


# =========================================================================================== theory of everything
_TOE = ['#ffe040', '#40a0ff', '#ff4040', '#c080ff']


def theory_of_everything(c1, c2, t):
    # four force rings precess apart; they swing into one plane and fuse to white (unification), the unified ring
    # shrinks to a single blazing point (the equation), then bursts and breaks symmetry back into four coloured forces
    sep = 1 - _seg(t, 0.22, 0.42) + _seg(t, 0.66, 0.86)
    wht = _seg(t, 0.3, 0.45) * (1 - _seg(t, 0.62, 0.78))
    sc = 1 - _seg(t, 0.47, 0.56) + _seg(t, 0.58, 0.68)
    sc = max(sc, 0.0)
    flash = _bump(t, 0.57, 0.025)
    parts, mats = [], []
    for k, col in enumerate(_TOE):
        cc = mix(H(col), WHITE, wht * 0.92)
        mats.append(Material(cc, '#ffffff', emissive=cc, pattern=pat_bands(8, 2 + k % 2, t, 0.35 + 0.3 * wht, axis=0)))
        if sc < 0.06:
            continue
        tilt = (0.6 + 0.35 * k) * sep
        R = roty(k * 0.785 * sep + t * TAU) @ rotx(tilt) @ rotz(0.3 * k * sep)
        rr = (0.64 - 0.06 * k * sep) * sc
        parts.append(Part(lambda p, rr=rr: sd_torus(p, rr, 0.045 + 0.02 * wht), k, rot=R))
    mats.append(Material('#ffffff', '#ffffff', emissive='#ffffff', pattern=pat_pulse_core(t)))
    parts.append(Part(lambda p: sd_sphere(p, 0.07 + 0.08 * wht + 0.25 * flash), 4))

    def ov(img):
        if flash > 0.08:
            _star(img, 15.5, 15.5, WHITE, 2 + 6 * flash)
        burst = _seg(t, 0.58, 0.72)
        if 0.0 < burst < 1:
            for k, col in enumerate(_TOE):                 # symmetry breaking: four coloured sparks fly apart
                for j in range(3):
                    a = k * TAU / 4 + 0.5 + j * 0.25
                    rr = 3 + 12 * burst - j
                    _dot_empty(img, 15.5 + math.cos(a) * rr, 15.5 + math.sin(a) * rr, H(col), (1 - burst) * (1 - j / 3))
    return parts, mats, dict(view_pitch=0.4, view_yaw=0.4, zoom=1.0), ov


# =========================================================================================== omnilex
def omnilex(c1, c2, t):
    # the book of every law: it lies shut, springs open, two pages turn on their own while law-cubes fall *upward*
    # out of it and a ring of rules spins up around it; then the cubes are recalled and the book snaps shut again
    op = _seg(t, 0.1, 0.26) * (1 - _seg(t, 0.8, 0.94))
    oa = 0.38
    ang_r = (PI - oa - 0.08) * (1 - op) + oa * op
    off = 0.38 * (1 - op)
    T = 0.035
    board = lambda p: sd_box(p - np.array([0.38, 0, 0]), (0.37, T, 0.48), 0.02)
    cover = lambda p: sd_box(p - np.array([0.39, -0.045, 0]), (0.39, 0.018, 0.5), 0.01)
    parts = [Part(board, 0, pos=(off, 0, 0), rot=rotz(PI - oa)), Part(cover, 2, pos=(off, 0, 0), rot=rotz(PI - oa)),
             Part(board, 0, pos=(off, 0, 0), rot=rotz(ang_r)), Part(cover, 2, pos=(off, 0, 0), rot=rotz(ang_r))]
    flips = []
    for c0 in (0.34, 0.52):
        u = _seg(t, c0, c0 + 0.12)
        if 0 < u < 1:
            flips.append(oa + (PI - 2 * oa) * u)
    for a in flips:
        parts.append(Part(lambda p: sd_box(p - np.array([0.36, 0, 0]), (0.35, 0.012, 0.46)), 3, pos=(off, 0.0, 0), rot=rotz(a)))
    ring = op * (1 - _seg(t, 0.7, 0.82))
    if ring > 0.05:
        parts.append(Part(lambda p: sd_torus(p, 0.86 * ring, 0.024), 1, pos=(0, 0.05, 0), rot=rotx(0.3) @ roty(t * TAU * 2)))

    def text(p, n):
        line = (np.abs(np.mod(p[..., 2] * 7, 1) - 0.5) < 0.18) & (np.abs(p[..., 0] - 0.38) < 0.28) & (np.abs(p[..., 1]) > T - 0.01)
        wave = np.sin(p[..., 0] * 10 + p[..., 2] * 3 - t * TAU * 4) > 0.4 - 0.8 * op
        return -0.3 * line, (line & wave & (op > 0.2)).astype(float)
    mats = [Material('#f0ece0', c2, metal=0.2, emissive='#20d0b0', pattern=text),
            Material(c2, '#ffffff', emissive=c2, pattern=pat_bands(10, 2, t, 0.25, axis=0)),
            Material('#1a3048', c2, metal=0.9, keep_dark=True, emissive=c2,
                     pattern=lambda p, n: (_zero(p), ((np.abs(p[..., 2]) + np.abs(p[..., 0] - 0.39) < 0.2) & (np.abs(p[..., 2]) + np.abs(p[..., 0] - 0.39) > 0.08) & (op < 0.5)).astype(float))),
            Material('#fffaf0', c2, metal=0.1, emissive='#a0fff0', pattern=lambda p, n: (_zero(p), (np.abs(np.mod(p[..., 2] * 6, 1) - 0.5) < 0.1).astype(float)))]
    act = _seg(t, 0.24, 0.32) * (1 - _seg(t, 0.72, 0.8))
    for k in range(5):
        life = (t * 2 + k / 5) % 1.0
        s = 0.07 * act * min(1.0, life / 0.15, (1 - life) / 0.2)
        if s < 0.015:
            continue
        a = k * TAU / 5 + 0.6
        parts.append(Part(lambda p, s=s: sd_box(p, (s, s, s), 0.01), 1, pos=(math.cos(a) * 0.3 * (1 + life), 0.1 + life * 0.75, math.sin(a) * 0.3 * (1 + life)),
                          rot=roty(life * 3) @ rotx(life * 2)))
    view = dict(view_pitch=0.6, view_yaw=0.2, zoom=1.0 + 0.25 * (1 - op))

    def ov(img):
        snap = _bump(t, 0.26, 0.025) + _bump(t, 0.94, 0.02)
        if snap > 0.1:
            _star(img, 15.5, 15.5, H(c2), 2 + 3 * snap)
    return parts, mats, view, ov


# =========================================================================================== higgs catalyst
def higgs_catalyst(c1, c2, t):
    # electroweak symmetry breaking on a loop: the potential starts as a plain bowl with a weightless ball at the
    # bottom; the centre rises into the Mexican hat, the ball teeters on the peak, rolls off into the trough and orbits
    # it, gaining mass (it grows and turns gold, with a flash); then the hat relaxes and the ball rolls home
    hat_k = _seg(t, 0.12, 0.3) * (1 - _seg(t, 0.78, 0.94))
    b = -0.3 + 3.8 * hat_k
    a3 = 1.2 + 5.8 * hat_k
    c = -0.25 + (b * b / (4 * a3) if b > 0 else 0.0)
    yf = lambda r2: a3 * r2 * r2 - b * r2 + c
    vtr = math.sqrt(max(b, 0) / (2 * a3))

    def surf(p):
        x, z = p[..., 0], p[..., 2]
        r2 = x * x + z * z
        r = np.sqrt(r2)
        dy = np.abs(4 * a3 * r2 * r - 2 * b * r)
        g = np.sqrt(1 + dy ** 2)
        return np.maximum(np.maximum((p[..., 1] - yf(r2)) / g, -0.42 - p[..., 1]), r - 0.66) * 0.6

    roll = _seg(t, 0.3, 0.42) * (1 - _seg(t, 0.72, 0.86))
    rb = vtr * roll
    wob = 0.03 * math.sin(t * TAU * 10) * _seg(t, 0.2, 0.3) * (1 - roll)
    ang = TAU * 2 * _seg(t, 0.32, 0.84)
    mass = _seg(t, 0.4, 0.5) * (1 - _seg(t, 0.74, 0.86))
    br = 0.09 + 0.06 * mass
    bx, bz = math.cos(ang) * rb + wob, math.sin(ang) * rb
    by = yf(rb * rb) + br + 0.02

    def rings(p, n):
        r = np.sqrt(p[..., 0] ** 2 + p[..., 2] ** 2)
        a = np.arctan2(p[..., 2], p[..., 0])
        lines = (np.abs(np.mod(r * 7, 1) - 0.5) < 0.1) | (np.abs(np.mod(a * 12 / TAU, 1) - 0.5) < 0.06)
        peak = (r < 0.14 + 0.05 * math.sin(t * TAU * 4)) & (hat_k > 0.3)
        trough = np.abs(r - vtr) < 0.08
        return -0.1 * lines, (peak | (lines & trough & (hat_k > 0.5)) | (lines & (r < 0.2) & (hat_k < 0.2))).astype(float)
    parts = [Part(surf, 0), Part(lambda p: sd_sphere(p, br), 1, pos=(bx, by, bz))]
    bcol = mix(H('#c0f8ff'), H('#ff7030'), mass)
    mats = [Material('#8a6c18', c2, metal=0.9, emissive=c2, pattern=rings),
            Material(mix(H('#e0f8ff'), H('#8a1c10'), mass), '#ffffff', metal=0.3 + 0.6 * mass, emissive=bcol,
                     pattern=lambda p, n: (_zero(p), (np.ones(p.shape[:-1]) * (mass < 0.5)) + (p[..., 1] > 0.5 * br)))]
    view = dict(view_pitch=0.62, view_yaw=0.6, zoom=1.1)

    def ov(img):
        f = _bump(t, 0.47, 0.03)
        if f > 0.1:
            x, y = _proj((bx, by, bz), view)
            _star(img, x, y, H(c2), 1 + 3 * f)
            _ring(img, x, y, 3 + 4 * (1 - f), H(c2), f * 0.7, empty=True)
    return parts, mats, view, ov


# =========================================================================================== tesseract core
def _hypercube():
    V = np.array([[(i >> k & 1) * 2 - 1 for k in range(4)] for i in range(16)], float)
    E = [(i, j) for i in range(16) for j in range(i + 1, 16) if bin(i ^ j).count('1') == 1]
    return V, E


_TV, _TE = _hypercube()
_TCELL = np.array([[s if k == a else 0 for k in range(4)] for a in range(4) for s in (1, -1)], float)
_TNET = [(0, 0.54, 0), (0, 0.18, 0), (0, -0.18, 0), (0, -0.54, 0), (0.36, 0.18, 0), (-0.36, 0.18, 0), (0, 0.18, 0.36), (0, 0.18, -0.36)]


def _rot4(i, j, a):
    M = np.eye(4)
    c, s = math.cos(a), math.sin(a)
    M[i, i], M[i, j], M[j, i], M[j, j] = c, -s, s, c
    return M


def _capsules(A, B, r):
    def f(p):
        d = None
        for a, b in zip(A, B):
            pa, ba = p - a, b - a
            hh = np.clip((pa * ba).sum(-1) / max(float((ba * ba).sum()), 1e-9), 0, 1)
            di = length(pa - ba * hh[..., None]) - r
            d = di if d is None else np.minimum(d, di)
        return d
    return f


def tesseract_core(c1, c2, t):
    # the hypercube's shadow turns through the fourth axis (inner cube becomes outer cube); then its eight cells pop
    # out as solid cubes and fly into the Dali cross (its unfolded 3D net), which turns once and folds back inside
    g = _seg(t, 0.0, 0.4) * 0.5 + _seg(t, 0.85, 1.0) * 0.5
    M = _rot4(0, 3, g * PI / 2)
    VIEWR = (roty(0.6) @ rotx(0.45)).T

    def project(X):
        Y = X @ M.T
        s = 0.44 * 2.0 / (2.7 - Y[:, 3])
        return (Y[:, :3] * s[:, None]) @ VIEWR, Y[:, 3]
    P, w = project(_TV)
    cells, _ = project(_TCELL * 1.0)
    net = _seg(t, 0.4, 0.55) * (1 - _seg(t, 0.72, 0.86))
    pop = _seg(t, 0.38, 0.46) * (1 - _seg(t, 0.82, 0.88))
    wr = 0.05 * (1 - _seg(t, 0.4, 0.5) + _seg(t, 0.8, 0.88))
    parts = []
    if wr > 0.008:
        near = [(i, j) for i, j in _TE if w[i] + w[j] > 1e-6]
        far = [(i, j) for i, j in _TE if w[i] + w[j] <= 1e-6]
        if near:
            parts.append(Part(_capsules([P[i] for i, j in near], [P[j] for i, j in near], wr), 0))
        if far:
            parts.append(Part(_capsules([P[i] for i, j in far], [P[j] for i, j in far], wr * 0.8), 1))
    if pop > 0.02:
        yaw = TAU * _seg(t, 0.52, 0.76)
        Rn = roty(yaw + 0.5) @ rotx(0.15)
        cs = 0.17 * pop
        for parity in (0, 1):
            cen = []
            for k in range(8):
                if k % 2 != parity:
                    continue
                a = (cells[k] * 0.9) @ Rn                   # world -> net-local frame
                b = np.array(_TNET[k])
                cen.append(a + (b - a) * net)
            parts.append(Part(lambda p, cen=cen: np.min(np.stack([sd_box(p - c, (cs, cs, cs), 0.02) for c in cen]), 0), 2 + parity, rot=Rn))
    mats = [Material(c1, '#ffffff', metal=0.8, emissive=c1, pattern=pat_bands(8, 2, t, 0.25, axis=1)),
            Material(c2, '#ffffff', metal=0.8, emissive=c2, pattern=pat_all()),
            Material(c1, '#ffffff', metal=0.7, emissive=c1, pattern=pat_bands(6, 2, t, 0.2, axis=1)),
            Material(c2, '#ffffff', metal=0.7, emissive=c2, pattern=pat_bands(6, 2, t, 0.2, axis=1))]
    view = dict(view_pitch=0.3, view_yaw=0.5)

    def ov(img):
        f = _bump(t, 0.42, 0.03) + _bump(t, 0.86, 0.03)
        if f > 0.1:
            _star(img, 15.5, 15.5, H(c2), 1 + 4 * f)
    return parts, mats, view, ov


# =========================================================================================== einstein field core
def einstein_field_core(c1, c2, t):
    # both halves of the field equations: a flat spacetime grid; a bright mass drops onto it and the grid sags into a
    # well, then wraps right round the mass into the lattice sphere (the core), pulses, and unwraps back to flat
    drop = _seg(t, 0.05, 0.28)
    wrap = _seg(t, 0.3, 0.48) * (1 - _seg(t, 0.72, 0.9))
    lift = _seg(t, 0.86, 1.0)
    D = 0.45 * drop * (1 - lift)
    my = 0.75 * (1 - drop) + 0.75 * lift * 0 - D * 0.55 + 0.75 * lift
    my = my * (1 - wrap)
    mr = 0.2 + 0.08 * wrap

    def sheet(p):
        r2 = p[..., 0] ** 2 + p[..., 2] ** 2
        yy = -D * np.exp(-r2 / 0.18) - 0.05
        d = (p[..., 1] - yy) / np.sqrt(1 + (D * 4) ** 2)
        return np.maximum(np.abs(d) - 0.03, np.maximum(np.abs(p[..., 0]), np.abs(p[..., 2])) - 0.72)

    def lattice(p):
        r = np.sqrt((p * p).sum(-1)) + 1e-6
        shell = np.abs(r - 0.6) - 0.035
        phi = np.arctan2(p[..., 2], p[..., 0])
        th = np.arccos(np.clip(p[..., 1] / r, -1, 1))
        dl = np.abs(np.mod(phi * 8 / TAU, 1) - 0.5) * (TAU / 8) * r * np.sin(th)
        dt = np.abs(np.mod(th * 6 / PI, 1) - 0.5) * (PI / 6) * r
        return np.maximum(shell, np.minimum(dl, dt) - 0.035)

    def field(p):
        if wrap <= 0.001:
            return sheet(p)
        if wrap >= 0.999:
            return lattice(p)
        return (1 - wrap) * sheet(p) + wrap * lattice(p)

    def grid(p, n):
        r = np.sqrt((p * p).sum(-1))
        g = (np.abs(np.mod(p[..., 0] * 5, 1) - 0.5) < 0.12) | (np.abs(np.mod(p[..., 2] * 5, 1) - 0.5) < 0.12)
        ripple = np.sin(r * 18 - t * TAU * 6) > 0.5
        return -0.15 * g, ((g & (wrap < 0.5) & (ripple | (r < 0.2 + D))) | ((wrap >= 0.5) & ripple)).astype(float)
    parts = [Part(field, 0, rot=roty(t * TAU / 4 * 0)), Part(lambda p: sd_sphere(p, mr), 1, pos=(0, my, 0))]
    mats = [Material(c1, c2, metal=0.9, emissive=c2, pattern=grid),
            Material('#ffe0a0', '#ffffff', emissive='#fff0d0', pattern=pat_pulse_core(t * 2))]
    view = dict(view_pitch=0.42, view_yaw=0.5, zoom=1.0)

    def ov(img):
        f = _bump(t, 0.5, 0.03) + _bump(t, 0.6, 0.03) * 0.6
        if f > 0.1:
            _ring(img, 15.5, 15.5, 12 + 3 * (1 - f), H(c2), f * 0.8, empty=True)
        if 0.15 < t < 0.3:
            k = (t - 0.15) / 0.15
            _ring(img, 15.5, 18.5, 3 + 10 * k, H(c2), (1 - k) * 0.7, ry=(3 + 10 * k) * 0.4, empty=True)
    return parts, mats, view, ov


# =========================================================================================== annihilation core
def annihilation_core(c1, c2, t):
    # inside a steel cage a matter blob and an antimatter blob spiral together, annihilate in a white flash that
    # makes the cage itself glow white-hot and bulge; gamma rays escape through the bars; then pair production: two
    # new blobs are born from a point at the centre and drift apart as the cage cools back through pink to steel
    ins = _seg(t, 0.0, 0.42)
    flash = _bump(t, 0.45, 0.03)
    heat = max(0.0, 1 - max(0.0, t - 0.45) / 0.3) if t >= 0.43 else 0.0
    born = _seg(t, 0.62, 1.0)
    if t < 0.45:
        rad = 0.42 * (1 - ins) + 0.0
        r = 0.2 - 0.05 * ins
        alive = rad > 0.03 or t < 0.44
    else:
        rad = 0.42 * born
        r = 0.2 * born + 0.15 * (1 - born) * 0 if born > 0 else 0.0
        r = 0.05 + 0.15 * born if born > 0 else 0.0
        alive = born > 0
    ang = TAU * (1.5 * ins) if t < 0.45 else TAU * (0.0 + 0.5 * born) - PI * 0.5 + TAU * 0.5 * 0
    # pair born along the same axis the old pair started on (angle 0) so t=1 matches t=0
    if t >= 0.45:
        ang = 0.0
    bulge = 1 + 0.12 * flash
    frame = lambda p: np.maximum(sd_box(p, (0.62, 0.62, 0.62), 0.04),
                                 -np.minimum(np.minimum(sd_box(p, (0.5, 0.5, 1)), sd_box(p, (1, 0.5, 0.5))), sd_box(p, (0.5, 1, 0.5))))
    parts = [Part(_scaled(frame, bulge), 0)]
    hot = mix(mix(H('#7a8090'), H(c1), min(1, heat * 1.5)), WHITE, max(0, heat * 2 - 1))
    mats = [Material(hot, '#ffffff', metal=1.0 - 0.6 * heat, emissive=mix(H(c1), WHITE, heat),
                     pattern=lambda p, n: (_zero(p), (np.ones(p.shape[:-1]) * (heat > 0.25)))),
            _glowmat('#80c0ff'), _glowmat(c1), _glowmat('#ffffff')]
    if alive and r > 0.02:
        cx, cz = math.cos(ang) * rad, math.sin(ang) * rad
        parts.append(Part(lambda p: sd_sphere(p, r), 1, pos=(-cx, 0, -cz)))
        parts.append(Part(lambda p: sd_sphere(p, r), 2, pos=(cx, 0, cz)))
    if flash > 0.05:
        parts.append(Part(lambda p: sd_sphere(p, 0.1 + 0.35 * flash), 3))
    view = dict(view_pitch=0.45, view_yaw=0.6)

    def ov(img):
        g = _seg(t, 0.44, 0.6)
        if 0 < g < 1:
            for k in range(8):
                a = k * TAU / 8 + 0.2
                for j in range(3):
                    rr = 8 + 12 * g - j * 1.5
                    _dot_empty(img, 15.5 + math.cos(a) * rr, 15.5 + math.sin(a) * rr, WHITE if j == 0 else H(c1), (1 - g) * (1 - j / 3))
        if flash > 0.3:
            _whiten(img, 0.5 * flash)
        sp = _bump(t, 0.63, 0.03)
        if sp > 0.1:
            _star(img, 15.5, 15.5, H(c1), 1 + 2 * sp)
    return parts, mats, view, ov


# =========================================================================================== multiverse shard
_MV = [  # (shape fn, hue degrees) - four versions of the shard from four universes
    lambda p: sd_gem(p, 0.3, 0.9),
    lambda p: sd_octa(p / np.array([0.8, 1.15, 0.8]), 0.72) * 0.8,
    lambda p: sd_box(p, (0.3, 0.55, 0.3), 0.04),
    lambda p: sd_gem(p / np.array([1.3, 0.75, 1.3]), 0.3, 0.9) * 0.75,
]


def multiverse_shard(c1, c2, t):
    # its constants will not settle: the shard holds one form from one universe, then splits into three chromatic
    # ghosts (neighbouring branes) that slide apart and re-merge as a different crystal in a different colour
    n = len(_MV)
    u = t * n
    k = int(u) % n
    f = u - int(u)
    m = _seg(f, 0.7, 0.95)
    split = math.sin(PI * _seg(f, 0.6, 1.0))
    sh_a, sh_b = _MV[k], _MV[(k + 1) % n]
    shape = lambda p: (1 - m) * sh_a(p) + m * sh_b(p)
    hue_deg = 90 * (k + m)
    base = _hue(H(c1), hue_deg)
    acc = _hue(H(c2), hue_deg)
    R = rotz(0.35) @ roty(t * TAU * 2)

    def fac(p, nn):
        v = np.sin(p[..., 1] * 14 - t * TAU * 8)
        return 0.15 * np.tanh(nn[..., 0] * nn[..., 2] * 20), ((v > 0.75) | (length(p) < 0.18)).astype(float)
    parts = [Part(shape, 0, rot=R)]
    mats = [Material(base, acc, metal=0.85, gloss=1.6, emissive=acc, pattern=fac),
            Material('#40ffe0', '#ffffff', metal=0.3, emissive='#40ffe0', pattern=pat_all()),
            Material('#ff40a0', '#ffffff', metal=0.3, emissive='#ff40a0', pattern=pat_all())]
    if split > 0.05:
        d = 0.5 * split
        sc = 0.7
        parts.append(Part(_scaled(shape, sc), 1, pos=(-d, 0.12 * split, -d * 0.6), rot=R))
        parts.append(Part(_scaled(shape, sc), 2, pos=(d, -0.12 * split, d * 0.6), rot=R))
    view = dict(view_pitch=0.35, view_yaw=0.6, zoom=1.0)

    def ov(img):
        if split > 0.3:                                   # glitch: shift a few rows sideways
            for j in range(3):
                y = int(_hs(k * 7.0 + j * 3.1 + int(f * 20)) * 30) + 1
                dx = int((_hs(j * 5.3 + k) - 0.5) * 6 * split)
                if dx:
                    img[y:y + 2] = np.roll(img[y:y + 2], dx, axis=1)
        if 0.93 < f or f < 0.04:
            _star(img, 15.5, 15.5, acc, 2)
    return parts, mats, view, ov


# =========================================================================================== exports
ITEMS = {
    'neutronium_ingot': neutronium_ingot, 'metallic_hydrogen_ingot': metallic_hydrogen_ingot,
    'cosmic_neutronium_ingot': cosmic_neutronium_ingot, 'crystalline_matrix_ingot': crystalline_matrix_ingot,
    'antiiron_ingot': antiiron_ingot, 'theory_of_everything': theory_of_everything, 'omnilex': omnilex,
    'higgs_catalyst': higgs_catalyst, 'tesseract_core': tesseract_core, 'einstein_field_core': einstein_field_core,
    'annihilation_core': annihilation_core, 'multiverse_shard': multiverse_shard,
}
RECIPES = {
    'neutronium_ingot': (N, N), 'metallic_hydrogen_ingot': (N, N), 'cosmic_neutronium_ingot': (N, N),
    'crystalline_matrix_ingot': (N, N), 'antiiron_ingot': (N, N), 'theory_of_everything': (N, N), 'omnilex': (N, N),
    'higgs_catalyst': (N, N), 'tesseract_core': (N, N), 'einstein_field_core': (N, N), 'annihilation_core': (N, N),
    'multiverse_shard': (N, N),
}
NARRATIVES = {}
FRAMES = {
    'neutronium_ingot': 64, 'metallic_hydrogen_ingot': 64, 'cosmic_neutronium_ingot': 72, 'crystalline_matrix_ingot': 64,
    'antiiron_ingot': 60, 'theory_of_everything': 72, 'omnilex': 64, 'higgs_catalyst': 64, 'tesseract_core': 60,
    'einstein_field_core': 64, 'annihilation_core': 64, 'multiverse_shard': 72,
}
