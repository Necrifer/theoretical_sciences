"""Item art group 2 (round 11): gems, ghost orbs, hex grids, holo dossiers, ingots, lattices and lenses.
Every item has its own model and/or animation that says what it is."""
import math
import numpy as np
from items3d import *
import anim

PI = math.pi


# ------------------------------------------------------------------------------------------- helpers
def _hs(a):
    v = np.sin(a * 12.9898 + 4.1) * 43758.5453
    return v - np.floor(v)


def _dot(img, x, y, rgb, a=1.0):
    anim._blend(img, int(round(y)), int(round(x)), np.asarray(rgb, float), a)


def _proj(pt, view):
    """World point -> (x, y) on the 32px image for a static view."""
    cam = rotx(-view.get('view_pitch', 0.5)) @ roty(view.get('view_yaw', 0.7853))
    c = np.asarray(pt, float) @ cam.T
    k = view.get('zoom', 1.0) / view.get('extent', 1.0)
    return (c[0] * k + 1) / 2 * 32 - 0.5, (-c[1] * k + 1) / 2 * 32 - 0.5


def _segs(p, segs, r):
    d = None
    for a, b in segs:
        di = sd_capsule(p, a, b, r)
        d = di if d is None else np.minimum(d, di)
    return d


def _balls(p, pts, r):
    d = None
    for q in pts:
        di = sd_sphere(p - np.asarray(q, float), r)
        d = di if d is None else np.minimum(d, di)
    return d


def sd_boxframe(p, b, e):
    p = np.abs(p) - np.asarray(b, float)
    q = np.abs(p + e) - e
    def f(a, b_, c):
        v = np.stack([a, b_, c], -1)
        return length(np.maximum(v, 0)) + np.minimum(np.maximum(a, np.maximum(b_, c)), 0)
    px, py, pz = p[..., 0], p[..., 1], p[..., 2]
    qx, qy, qz = q[..., 0], q[..., 1], q[..., 2]
    return np.minimum(np.minimum(f(px, qy, qz), f(qx, py, qz)), f(qx, qy, pz))


def _wedge(p, a0, a1, gap=0.0):
    """Half-plane intersection in the local xz plane keeping angles a0..a1 (span <= pi). <=0 inside."""
    x, z = p[..., 0], p[..., 2]
    d0 = -(x * -math.sin(a0) + z * math.cos(a0)) + gap
    d1 = (x * -math.sin(a1) + z * math.cos(a1)) + gap
    return np.maximum(d0, d1)


def _ing(p, L=0.85, Hh=0.3, W=0.45):
    return np.maximum(sd_box(p, (L, Hh, W), 0.05), (p[..., 1] * 0.95 + np.abs(p[..., 2]) * 0.6 + np.abs(p[..., 0]) * 0.12 - (Hh + 0.12)))


def _none(p):
    return np.zeros(p.shape[:-1])


ING_VIEW = dict(view_pitch=0.6, view_yaw=0.65, zoom=1.05)


# =========================================================================================== gems
def r_process_cluster(c1, c2, t):
    # dark supernova slag with three freshly made heavy elements stuck in it: a gold octahedron, a platinum cube and a
    # glowing uranium needle; a rain of neutrons pours in and every captured one flashes
    slag = lambda p: sd_sphere(p, 0.42) + 0.07 * np.sin(p[..., 0] * 9 + 1) * np.sin(p[..., 1] * 8) * np.sin(p[..., 2] * 7 + 2)
    def u_glow(p, n):
        return np.zeros(p.shape[:-1]), (np.sin(p[..., 1] * 14 - t * TAU * 3) > -0.2).astype(float)
    parts = [Part(slag, 0, pos=(0.02, -0.18, 0)),
             Part(lambda p: sd_octa(p, 0.34), 1, pos=(-0.32, 0.1, 0.1), rot=rotz(0.35) @ roty(0.4)),
             Part(lambda p: sd_box(p, (0.17, 0.17, 0.17), 0.02), 2, pos=(0.36, 0.05, -0.05), rot=rotx(0.6) @ rotz(0.5)),
             Part(lambda p: sd_gem(p, 0.11, 0.42), 3, pos=(0.05, 0.3, -0.12), rot=rotz(-0.35))]
    mats = [Material(c1, c2, metal=0.25, pattern=pat_noise_shade(9, 0.3, 4)),
            Material('#e8a818', '#fff0a0', metal=1.0), Material('#b0bac4', '#ffffff', metal=1.0),
            Material('#3a8a30', '#c0ff80', emissive='#90ff60', pattern=u_glow)]
    def ov(img):
        for i in range(5):
            life = (t * 2 + i / 5) % 1.0
            x0, y0 = 3 + i * 6.5, -2
            x, y = x0 + life * 5, y0 + life * 17
            if life < 0.75:
                _dot(img, x, y, H('#a0d8ff'), 0.9)
                _dot(img, x - 0.3, y - 1.2, H('#a0d8ff'), 0.4)
            elif life < 0.9:
                sparkle(img, int(x / 2), int(y / 2), H(c2))
    return parts, mats, dict(view_pitch=0.35, view_yaw=0.5), ov


def string_coupling_crystal(c1, c2, t):
    # a dark magenta crystal with a closed string around its waist that splits into two loops and joins up again
    s = 0.5 - 0.5 * math.cos(t * TAU * 2)                    # 0 = one loop, 1 = split into two
    R = 0.5 - 0.14 * s
    parts = [Part(lambda p: sd_gem(p, 0.28, 0.92), 0, rot=rotz(0.12) @ roty(0.3)),
             Part(lambda p: sd_torus(p, R, 0.05), 1, pos=(0, 0.42 * s, 0), rot=rotx(0.35)),
             Part(lambda p: sd_torus(p, R, 0.05), 1, pos=(0, -0.42 * s, 0), rot=rotx(0.35))]
    mats = [Material(c1, c2, metal=0.85, pattern=pat_spots(t, 0.85, 3), emissive='#c060c0'),
            Material(c2, '#ffffff', emissive=c2, pattern=pat_bands(8, 2, t, 0.3, axis=0))]
    return parts, mats, dict(view_pitch=0.3, view_yaw=0.6), None


# =========================================================================================== ghost orbs
def banach_tarski_sphere(c1, c2, t):
    # one sphere of five wedges flies apart and the wedges widen until they are two whole spheres - and back again
    s = 0.5 - 0.5 * math.cos(t * TAU)
    s = s * s * (3 - 2 * s)
    r = 0.44 - 0.12 * s
    parts, mats = [], [Material(c1, c2, metal=0.9), Material('#8898b0', c2, metal=0.9)]
    groups = [(0, 0, 2), (1, 0, 2), (2, 1, 3), (3, 1, 3), (4, 1, 3)]   # (piece, sphere, pieces in that sphere)
    idx_in = {0: 0, 1: 1, 2: 0, 3: 1, 4: 2}
    for k, g, cnt in groups:
        span0, span1 = TAU / 5, TAU / cnt
        a0 = (1 - s) * (k * TAU / 5) + s * (idx_in[k] * span1 + 0.3)
        span = (1 - s) * span0 + s * span1 - 0.0001
        cx = (g * 2 - 1) * 0.46 * s
        mid = a0 + span / 2
        gap = 0.07 * math.sin(PI * s)                     # pieces drift apart mid-flight
        off = np.array([cx + math.cos(mid) * gap, 0.04 * math.sin(PI * s) * (1 if g else -1), math.sin(mid) * gap])
        def wedge(p, a0=a0, a1=a0 + span, r=r):
            return np.maximum(sd_sphere(p, r), _wedge(p, a0, a1))
        def seam(p, n, a0=a0, a1=a0 + span):
            x, z = p[..., 0], p[..., 2]
            e0 = np.abs(x * -math.sin(a0) + z * math.cos(a0)) < 0.035
            e1 = np.abs(x * -math.sin(a1) + z * math.cos(a1)) < 0.035
            return np.zeros(x.shape), (e0 | e1).astype(float)
        mats.append(Material(c1 if k % 2 == 0 else '#8898b0', c2, metal=0.9, emissive=c2, pattern=seam))
        parts.append(Part(wedge, len(mats) - 1, pos=off, rot=rotx(0.15)))
    return parts, mats, dict(view_pitch=0.55, view_yaw=0.0), None


def hyperdimensional_pearl(c1, c2, t):
    # an iridescent pearl that blinks between four places; ghost rings mark everywhere else it also is
    spots = [(-0.38, 0.36), (0.4, 0.3), (0.36, -0.36), (-0.4, -0.34)]
    k = int(t * 4) % 4
    ph = (t * 4) % 1
    x, y = spots[k]
    def irid(p, n):
        v = np.sin(p[..., 0] * 9 + p[..., 1] * 7 - t * TAU * 2)
        return 0.25 * v, (v > 0.75).astype(float)
    parts = [Part(lambda p: sd_sphere(p, 0.3), 0, pos=(x, y, 0))]
    mats = [Material('#1c6a64', c2, metal=0.95, gloss=1.6, emissive=c2, pattern=irid)]
    view = dict(view_pitch=0.0, view_yaw=0.0)
    def ov(img):
        n = 32
        ys, xs = np.mgrid[0:n, 0:n]
        for j, (sx, sy) in enumerate(spots):
            if j == k:
                continue
            px, py = _proj((sx, sy, 0), view)
            d = np.sqrt((xs - px) ** 2 + (ys - py) ** 2)
            ring = (np.abs(d - 4.5) < 0.6) & (img[..., 3] < 0.5)
            a = 0.35 + 0.25 * math.sin(TAU * (t * 2 + j / 4))
            img[ring, :3] = H(c2) * a + img[ring, :3] * (1 - a)
            img[ring, 3] = np.maximum(img[ring, 3], a)
        if ph < 0.18:                                    # teleport flash where it just arrived
            px, py = _proj((x, y, 0), view)
            sparkle(img, int(px / 2), int(py / 2), H(c2), 2 if ph < 0.09 else 1)
    return parts, mats, view, ov


def ocean_simulacrum(c1, c2, t):
    # a lattice cage holding a sphere of the Ocean of Laws; bubbles rise through it and pop at the top
    def bubbles(p, n):
        q = p.copy()
        q[..., 1] = q[..., 1] - t * (1 / 3.0) * 3            # rises by exactly one cell-height (1/3 * 3 cells) per loop
        cell = np.floor(q * 3 * 2)
        h = _hs(cell[..., 0] * 1.7 + np.mod(cell[..., 1], 6) * 7.3 + cell[..., 2] * 3.1)
        f = q * 6 - cell - 0.5
        rr = (f ** 2).sum(-1)
        wave = 0.12 * np.sin(np.arctan2(p[..., 2], p[..., 0]) * 4 + p[..., 1] * 6 - t * TAU)
        return wave, ((h > 0.55) & (rr < 0.16)).astype(float)
    liquid = lambda p: sd_sphere(p, 0.5) + 0.025 * np.sin(np.arctan2(p[..., 2], p[..., 0]) * 5 + t * TAU) * np.sin(p[..., 1] * 7)
    def nodes(p, n):
        corner = (np.abs(p) > 0.5).sum(-1) >= 3
        return np.zeros(p.shape[:-1]), (corner & (math.sin(t * TAU * 2) > -0.3)).astype(float)
    parts = [Part(liquid, 0), Part(lambda p: sd_boxframe(p, (0.62, 0.62, 0.62), 0.035), 1)]
    mats = [Material('#5a2aa0', c2, metal=0.4, emissive=c2, pattern=bubbles), Material('#8090a8', c2, metal=1.0, emissive=c2, pattern=nodes)]
    return parts, mats, dict(view_pitch=0.45, view_yaw=0.6), None


def sterile_neutrino(c1, c2, t):
    # a containment ring tries to catch a ghost particle; it slips straight through and is gone
    a = -1.5 + 3.8 * t
    pos = (a, -a * 0.75, 0.0)
    near = math.exp(-(a / 0.35) ** 2)
    def catch(p, n):
        return np.zeros(p.shape[:-1]), (np.sin(np.arctan2(p[..., 2], p[..., 0]) * 6 + t * TAU * 3) > 1 - 2 * near).astype(float)
    parts = [Part(lambda p: sd_torus(p, 0.52, 0.075), 0, rot=rotx(PI / 2) @ rotz(0.0) @ rotx(0.25)),
             Part(lambda p: sd_sphere(p, 0.15), 1, pos=pos)]
    mats = [Material(c1, '#ffffff', metal=0.95, emissive=c2, pattern=catch), Material(c2, '#ffffff', emissive=c2, pattern=pat_all())]
    view = dict(view_pitch=0.0, view_yaw=0.0)
    def ov(img):
        for k in range(1, 7):
            b = a - k * 0.11
            px, py = _proj((b, -b * 0.75, 0), view)
            _dot(img, px, py, H(c1), 0.6 * (1 - k / 7))
    return parts, mats, view, ov


def virtual_graviton(c1, c2, t):
    # a spin-2 blob flexing between plus-shapes, flickering in and out of existence; gravitational ripples spread out
    ex = 0.5 + 0.5 * math.cos(t * TAU * 2)                 # how much it currently exists
    st = 0.35 * math.sin(t * TAU * 4)
    def blob(p):
        az = np.arctan2(p[..., 1], p[..., 0])
        rr = 0.3 * (0.55 + 0.45 * ex) * (1 + st * np.cos(2 * az))
        return (length(p) - rr) * 0.7
    def flick(p, n):
        return np.zeros(p.shape[:-1]), (np.sin(p[..., 0] * 13 + p[..., 1] * 11 - t * TAU * 4) > 1.2 - 1.6 * ex).astype(float)
    # the ripple is born inside the blob and spreads out past the edge of the picture (it used to shrink to a thin
    # ring in view and snap back to the centre); a new one starts as the old one leaves, twice per loop
    R = 0.12 + 1.45 * ((t * 2) % 1)
    parts = [Part(blob, 0), Part(lambda p: sd_torus(p, R, 0.045), 1, rot=rotx(PI / 2 - 0.35))]
    mats = [Material('#3a4890', c2, metal=0.3, emissive=c2, pattern=flick),
            Material(c2, '#ffffff', emissive=c2, pattern=lambda p, n: (np.zeros(p.shape[:-1]), (np.sin(np.arctan2(p[..., 2], p[..., 0]) * 8) > -0.2).astype(float)))]
    return parts, mats, dict(view_pitch=0.15, view_yaw=0.0), None


def virtual_monopole(c1, c2, t):
    # a hedgehog: every field line points outward from one pole; it is lent by the vacuum, grows and is taken back
    life = math.sin(PI * t) ** 2
    sc = 0.35 + 0.65 * life
    dirs = []
    for i in range(8):
        a = i * TAU / 8 + 0.3
        e = 0.6 if i % 2 else -0.4
        v = np.array([math.cos(a), e, math.sin(a)])
        dirs.append(v / np.linalg.norm(v))
    dirs += [np.array([0, 1.0, 0]), np.array([0, -1.0, 0])]
    spikes = lambda p: _segs(p / sc, [(d * 0.2, d * 0.78) for d in dirs], 0.045) * sc
    def outward(p, n):
        r = length(p) / sc
        return np.zeros(r.shape), (np.sin(r * 16 - t * TAU * 4) > 0.2).astype(float)
    parts = [Part(lambda p: sd_sphere(p, 0.3 * sc), 0), Part(spikes, 1, rot=roty(t * TAU))]
    mats = [Material(c1, c2, metal=0.6, emissive=c2, pattern=pat_pulse_core(t)), Material(c2, '#ffffff', emissive=c2, pattern=outward)]
    return parts, mats, dict(view_pitch=0.3, view_yaw=0.5), None


def virtual_photon(c1, c2, t):
    # a Feynman wavy line strung between two vertices; the bright pulse never makes it from one end to the other
    A, k, L = 0.15, 2 * PI / 0.42, 0.62
    ph = t * TAU * 2
    def wave(p):
        x, y, z = p[..., 0], p[..., 1], p[..., 2]
        yy = A * np.sin(k * x - ph)
        slope = A * k * np.cos(k * x - ph)
        d = np.sqrt(((y - yy) / np.sqrt(1 + slope ** 2)) ** 2 + z ** 2) - 0.055
        return np.maximum(d, np.abs(x) - L) * 0.85
    def pulse(p, n):
        life = (t * 2) % 1
        xc = -L + life * 1.6 * L
        a = np.exp(-((p[..., 0] - xc) / 0.16) ** 2) * math.sin(PI * life)
        return np.zeros(a.shape), (a > 0.35).astype(float) + 0.0 * a
    vflash = lambda p, n: (np.zeros(p.shape[:-1]), np.full(p.shape[:-1], 1.0 if (t * 2) % 1 < 0.15 else 0.0))
    parts = [Part(wave, 0, rot=rotz(0.35)),
             Part(lambda p: sd_sphere(p, 0.11), 1, pos=rotz(0.35) @ np.array([-L - 0.04, 0, 0])),
             Part(lambda p: sd_sphere(p, 0.11), 2, pos=rotz(0.35) @ np.array([L + 0.04, 0, 0]))]
    mats = [Material('#c0a030', c2, metal=0.4, emissive='#ffffff', pattern=pulse),
            Material('#a09040', c2, emissive=c2, pattern=vflash), Material('#605020', c2, metal=0.8)]
    return parts, mats, dict(view_pitch=0.2, view_yaw=0.0), None


def virtual_quark(c1, c2, t):
    # a red quark tugs out of a dark hole in the vacuum on a gluon string, which stretches and snaps it back
    life = (t * 2) % 1
    d = 0.12 + 0.62 * math.sin(PI * min(life / 0.8, 1)) ** 2 if life < 0.8 else 0.12
    hole = np.array([-0.32, -0.28, 0])
    dirv = np.array([0.75, 0.66, 0]) / np.linalg.norm([0.75, 0.66, 0])
    q = hole + dirv * (0.22 + d)
    rr = 0.07 - 0.03 * (d / 0.74)
    def coil(p, n):
        s = (p - hole) @ dirv
        return np.zeros(s.shape), (np.sin(s * 30 - t * TAU * 6) > 0.0).astype(float)
    parts = [Part(lambda p: sd_sphere(p, 0.3), 0, pos=hole),
             Part(lambda p: sd_capsule(p, hole, q, rr), 1),
             Part(lambda p: sd_sphere(p, 0.17), 2, pos=q)]
    mats = [Material('#0c0408', c2, metal=0.9, keep_dark=True, pattern=lambda p, n: (np.zeros(p.shape[:-1]), (np.abs(length(p) - 0.3) < 0.0).astype(float))),
            Material('#ffa0c0', '#ffffff', emissive='#ffe0f0', pattern=coil), Material(c2, '#ffffff', emissive=c2, pattern=pat_pulse_core(t))]
    def ov(img):
        if 0.8 <= life < 0.9:                                # the snap back
            px, py = _proj(hole + dirv * 0.3, dict(view_pitch=0.0, view_yaw=0.0))
            sparkle(img, int(px / 2), int(py / 2), H(c2), 2)
    return parts, mats, dict(view_pitch=0.0, view_yaw=0.0), ov


def void_essence(c1, c2, t):
    # a pocket of perfect black: only a violet rim shows where it is; wisps fall toward it and are pushed back out
    view = dict(view_pitch=0.3, view_yaw=0.5)
    vdir = np.array([0, 0, 1.0]) @ (rotx(-view['view_pitch']) @ roty(view['view_yaw']))
    def rim(p, n):
        f = np.abs(n @ vdir)
        flick = 0.04 * np.sin(np.arctan2(p[..., 1], p[..., 0]) * 7 + t * TAU * 3)
        return np.zeros(f.shape) - 1, (f < 0.55 + flick).astype(float)
    parts = [Part(lambda p: sd_sphere(p, 0.46), 0)]
    mats = [Material('#000000', c2, metal=0.0, gloss=0.0, keep_dark=True, emissive=c2, pattern=rim)]
    def ov(img):
        c = 15.5
        for i in range(7):
            life = (t + i / 7) % 1
            a = TAU * i / 7 + life * 2.5
            r = 15 - 6 * math.sin(PI * life)                 # in, bounce off the edge, out again
            x, y = c + math.cos(a) * r, c + math.sin(a) * r
            _dot(img, x, y, H(c2), 0.75 * math.sin(PI * life))
    return parts, mats, view, ov


# =========================================================================================== hex grids
def e8_lattice(c1, c2, t):
    # a dark disc with the E8 Petrie projection: two nested octagrams and rings of root points, slowly turning
    rot = t * TAU / 8                                     # one symmetry step per loop
    def petrie(p, n):
        x, z = p[..., 0], p[..., 2]
        r = np.sqrt(x * x + z * z)
        a = np.arctan2(z, x) - rot
        lines = np.zeros(r.shape, bool)
        for j in range(8):
            th = j * TAU / 8 + rot
            d = np.abs(x * math.cos(th) + z * math.sin(th) - 0.62 * math.cos(3 * PI / 8))
            lines |= d < 0.035
        pts = np.zeros(r.shape, bool)
        for R, cnt, off in ((0.62, 8, 0.0), (0.36, 8, PI / 8)):
            sec = np.mod(a - off + PI / cnt, TAU / cnt) - PI / cnt
            pts |= ((r - R) ** 2 + (sec * R) ** 2) < 0.085 ** 2
        tw = np.sin(r * 10 - t * TAU * 2) > -0.5
        top = p[..., 1] > 0.02
        return -0.6 - 0.1 * lines, (((pts & tw) | lines | (r < 0.09)) & (r < 0.72) & top).astype(float)
    parts = [Part(lambda p: sd_cyl(p, 0.78, 0.05, 0.02), 0)]
    mats = [Material('#120c24', c2, metal=0.6, gloss=0.5, emissive=c2, pattern=petrie, keep_dark=True)]
    return parts, mats, dict(view_pitch=1.0, view_yaw=0.3, zoom=1.05), None


def error_correction_lattice(c1, c2, t):
    # a surface code: a grid of qubits; every beat one flips (red bead), its neighbours light up, vote it out
    step = int(t * 6) % 6
    ph = (t * 6) % 1
    rng = np.random.default_rng(5 + step)
    i, j = int(rng.integers(0, 5)), int(rng.integers(0, 5))
    if (i, j) == (2, 2):
        i = 3
    gx, gz = (i - 2) * 0.27, (j - 2) * 0.27
    def grid(p, n):
        x, z = p[..., 0], p[..., 2]
        fx, fz = x / 0.27 + 2, z / 0.27 + 2
        q = (np.abs(fx - np.round(fx)) < 0.3) & (np.abs(fz - np.round(fz)) < 0.3) & (np.abs(fx - 2) < 2.5) & (np.abs(fz - 2) < 2.5)
        topf = p[..., 1] > 0.04
        vote = (np.abs(np.round(fx) - i - 2 + 2) + np.abs(np.round(fz) - j - 2 + 2) == 1) & (0.25 < ph < 0.75)
        lines = ((np.abs(fx - np.round(fx) - 0.5) < 0.07) | (np.abs(fz - np.round(fz) - 0.5) < 0.07)) & (np.abs(fx - 2) < 2.2) & (np.abs(fz - 2) < 2.2)
        return -0.1 * lines, (q & topf & (vote | (np.sin(fx + fz - t * TAU * 2) > -0.2))).astype(float)
    parts = [Part(lambda p: sd_box(p, (0.72, 0.06, 0.72), 0.03), 0),
             Part(lambda p: sd_sphere(p, 0.15), 1, pos=(0, 0.13, 0))]
    mats = [Material(c1, c2, metal=0.6, emissive=c2, pattern=grid),
            Material('#ffffff', c2, emissive='#e0f8ff', pattern=pat_pulse_core(t)),
            Material('#ff3030', '#ffffff', emissive='#ff4040', pattern=pat_all())]
    if ph < 0.6:
        parts.append(Part(lambda p: sd_sphere(p, 0.1 * (1 - max(0, ph - 0.35) / 0.25)), 2, pos=(gx, 0.1, gz)))
    return parts, mats, dict(view_pitch=0.75, view_yaw=0.6, zoom=1.0), None


def metamaterial_sheet(c1, c2, t):
    # a sheet of split-ring resonators; a light beam hits it and refracts the *wrong* way
    def srr(p, n):
        x, z = p[..., 0] * 2.2 + 0.5, p[..., 2] * 2.2 + 0.5
        fx, fz = x - np.round(x), z - np.round(z)
        r = np.sqrt(fx * fx + fz * fz)
        ang = np.arctan2(fz, fx)
        ring = (np.abs(r - 0.3) < 0.1) & (np.abs(ang - PI / 2) > 0.6)
        wave = np.sin(np.round(x) * 1.3 + np.round(z) * 0.7 - t * TAU * 2) > -0.2
        return -0.06 * ring, (ring & wave).astype(float)
    parts = [Part(lambda p: sd_box(p, (0.72, 0.045, 0.72), 0.02), 0, pos=(0, -0.05, 0))]
    mats = [Material(c1, c2, metal=0.8, emissive=c2, pattern=srr)]
    view = dict(view_pitch=0.6, view_yaw=0.785)
    def ov(img):
        hx, hy = 15.5, 16.5
        a_in = (1, 1)
        for k in range(14):                              # incoming beam, from the upper left
            u = k / 13
            x, y = 1 + (hx - 1) * u, 0 + (hy - 0) * u
            _dot(img, x, y, H('#fff0a0'), 0.55)
        for k in range(12):                              # the refracted beam leaves on the *same* side of the normal
            u = k / 11
            x, y = hx - 9 * u, hy + 14 * u
            _dot(img, x, y, H('#fff0a0'), 0.55)
        for s in range(2):
            u = (t * 2 + s * 0.5) % 1
            if u < 0.5:
                v = u / 0.5
                x, y = 1 + (hx - 1) * v, (hy) * v
            else:
                v = (u - 0.5) / 0.5
                x, y = hx - 9 * v, hy + 14 * v
            _dot(img, x, y, np.ones(3), 1.0)
    return parts, mats, view, ov


def molecular_orbital_lattice(c1, c2, t):
    # a 3x3 grid of atoms joined by dumbbell bonding orbitals; the electron density sloshes back and forth in them
    def rep(p, s=0.5):
        q = p.copy()
        q[..., 0] = p[..., 0] - np.clip(np.round(p[..., 0] / s), -1, 1) * s
        q[..., 2] = p[..., 2] - np.clip(np.round(p[..., 2] / s), -1, 1) * s
        return q
    atoms = lambda p: sd_sphere(rep(p), 0.12)
    def lobes(p):
        # bonds along x at half-cells, and along z
        qx = p.copy()
        qx[..., 0] = p[..., 0] - (np.clip(np.floor(p[..., 0] / 0.5), -1, 0) + 0.5) * 0.5
        qx[..., 2] = p[..., 2] - np.clip(np.round(p[..., 2] / 0.5), -1, 1) * 0.5
        dx = sd_sphere(qx / np.array([1.0, 0.55, 0.55]), 0.17) * 0.55
        qz = p.copy()
        qz[..., 2] = p[..., 2] - (np.clip(np.floor(p[..., 2] / 0.5), -1, 0) + 0.5) * 0.5
        qz[..., 0] = p[..., 0] - np.clip(np.round(p[..., 0] / 0.5), -1, 1) * 0.5
        dz = sd_sphere(qz / np.array([0.55, 0.55, 1.0]), 0.17) * 0.55
        return np.minimum(dx, dz)
    def slosh(p, n):
        v = np.sin(p[..., 0] * 2 * PI / 0.5 * 0.5 + p[..., 2] * 2 * PI - t * TAU * 2)
        return 0.15 * v, (v > 0.1).astype(float)
    parts = [Part(atoms, 0), Part(lobes, 1)]
    mats = [Material('#e0e4f0', '#ffffff', metal=0.7), Material('#3060a0', c2, metal=0.3, emissive=c2, pattern=slosh)]
    return parts, mats, dict(view_pitch=0.7, view_yaw=0.5, zoom=1.1), None


def quantum_field_lattice(c1, c2, t):
    # a glowing field mesh; a particle is just a wave-packet rippling across it
    x0 = -1.6 + 3.2 * t
    def h(x, z):
        r2 = (x - x0) ** 2 + z ** 2
        return 0.3 * np.exp(-r2 / 0.06) * np.cos((x - x0) * 11) + 0.015 * np.sin(x * 9 - t * TAU * 3) * np.sin(z * 9)
    def sheet(p):
        return np.maximum((np.abs(p[..., 1] - h(p[..., 0], p[..., 2])) - 0.035) * 0.4, sd_box(p, (0.72, 0.5, 0.72)))
    def mesh(p, n):
        x, z = p[..., 0] * 3, p[..., 2] * 3
        g = (np.abs(x - np.round(x)) < 0.12) | (np.abs(z - np.round(z)) < 0.12)
        hh = h(p[..., 0], p[..., 2])
        bump = np.abs(hh) > 0.07
        return 0.25 * g + 0.8 * hh, bump.astype(float)
    parts = [Part(sheet, 0)]
    mats = [Material(c1, c2, metal=0.5, emissive='#e0f0ff', pattern=mesh)]
    return parts, mats, dict(view_pitch=0.55, view_yaw=0.6, zoom=1.05), None


def standard_model_matrix(c1, c2, t):
    # the particle table as tiles: quarks (violet), leptons (green), gauge bosons (red), the Higgs (gold),
    # and one empty slot where gravity should go
    P = 0.3
    def tiles(p, cols, rows, s=0.11):
        q = p.copy()
        q[..., 0] = p[..., 0] - np.clip(np.round(p[..., 0] / P), cols[0], cols[1]) * P
        q[..., 2] = p[..., 2] - (np.clip(np.round(p[..., 2] / P - 0.5), rows[0], rows[1]) + 0.5) * P
        return sd_box(q, (s, 0.06, s), 0.02)
    def scan(p, n):
        cx = np.round(p[..., 0] / P)
        cz = np.round(p[..., 2] / P - 0.5)
        k = int(t * 16) % 16
        on = (cx + 2) * 4 + (cz + 2) == k
        return np.zeros(cx.shape), ((on | (np.sin(cx * 1.7 + cz * 2.3 + t * TAU * 2) > 0.6)) & (p[..., 1] > 0.03)).astype(float)
    parts = [Part(lambda p: sd_box(p, (0.82, 0.04, 0.66), 0.02), 0, pos=(0.08, -0.06, 0)),
             Part(lambda p: tiles(p, (-2, 0), (-2, -1)), 1, pos=(0, 0.04, 0)),
             Part(lambda p: tiles(p, (-2, 0), (0, 1)), 2, pos=(0, 0.04, 0)),
             Part(lambda p: tiles(p, (1, 1), (-2, 1)), 3, pos=(0, 0.04, 0)),
             Part(lambda p: sd_sphere(p, 0.12), 4, pos=(0.68, 0.1, -0.15)),
             Part(lambda p: sd_box(p, (0.11, 0.02, 0.11), 0.02), 5, pos=(0.68, 0.0, 0.3))]
    mats = [Material(c1, c2, metal=0.8),
            Material('#7040c0', '#e0c0ff', emissive='#e0c0ff', pattern=scan), Material('#309050', '#c0ffd0', emissive='#c0ffd0', pattern=scan),
            Material('#b03040', '#ffc0c0', emissive='#ffd0d0', pattern=scan),
            Material('#e0b030', '#fff0a0', emissive='#fff0a0', pattern=pat_pulse_core(t)),
            Material('#101018', c2, keep_dark=True, emissive=c2, pattern=lambda p, n: (np.zeros(p.shape[:-1]), np.full(p.shape[:-1], 1.0 if (t * 4) % 1 < 0.2 else 0.0)))]
    return parts, mats, dict(view_pitch=0.85, view_yaw=0.35, zoom=1.0), None


# =========================================================================================== holo: research notes
TIER_NOTE = {'empirical': 0, 'theoretical': 1, 'unified': 2, 'absolute': 3}


def _notes(tier, idx):
    """A dossier split in three: each note is one third, with the outline of the whole dossier glowing round it."""
    def b(c1, c2, t):
        ghost = Material(c2, '#ffffff', emissive=mix(H(c2), H(c1), 0.45), pattern=lambda p, n: (np.zeros(p.shape[:-1]), (np.sin(length(p) * 10 + np.arctan2(p[..., 1], p[..., 0]) * 3 - t * TAU * (idx + 1)) > -0.6).astype(float)))
        if tier == 'empirical':
            # a data slate in three vertical strips; the piece shows a live bar chart and a scan line
            x0 = (idx - 1) * 0.47
            def chart(p, n):
                x, y = p[..., 0], p[..., 1]
                front = p[..., 2] < -0.03
                col = np.floor((x + 0.21) / 0.105)
                hgt = 0.12 + 0.5 * (0.5 + 0.5 * np.sin(t * TAU * (idx + 1) + col * 1.9 + idx))
                bar = (np.abs((x + 0.21) / 0.105 - col - 0.5) < 0.3) & (y < -0.34 + hgt) & (y > -0.36)
                scan = np.abs(y - (0.38 - ((t * 2) % 1) * 0.76)) < 0.035
                return np.zeros(x.shape), ((bar | scan) & front).astype(float)
            parts = [Part(lambda p: sd_box(p, (0.21, 0.42, 0.05), 0.02), 0, pos=(x0, 0, 0)),
                     Part(lambda p: sd_boxframe(p, (0.72, 0.46, 0.04), 0.035), 1)]
            mats = [Material(c1, c2, metal=0.6, emissive=c2, pattern=chart), ghost]
            return parts, mats, dict(view_pitch=0.12, view_yaw=0.3 + 0.08 * (idx - 1)), None
        if tier == 'theoretical':
            # a disc in three 120-degree slices; the slice carries a running wavefunction
            a0 = PI / 2 + idx * TAU / 3 - PI / 3
            def wavef(p, n):
                x, z = p[..., 0], p[..., 2]
                c, s = math.cos(a0 + PI / 3), math.sin(a0 + PI / 3)
                u, v = x * c + z * s, -x * s + z * c
                w = np.abs(v - 0.1 * np.sin(u * 10 - t * TAU * (2 + idx))) < 0.07
                dots = (np.abs(np.sqrt(x * x + z * z) - 0.58) < 0.04)
                face = np.abs(p[..., 1]) > 0.03
                return np.zeros(x.shape), ((w | dots) & face).astype(float)
            parts = [Part(lambda p: np.maximum(sd_cyl(p, 0.66, 0.05, 0.02), _wedge(p, a0, a0 + TAU / 3, 0.035)), 0, rot=rotx(PI / 2)),
                     Part(lambda p: sd_torus(p, 0.72, 0.04), 1, rot=rotx(PI / 2))]
            mats = [Material('#3a2858', c2, metal=0.7, emissive='#e0c0ff', pattern=wavef), ghost]
            return parts, mats, dict(view_pitch=0.15, view_yaw=0.25), None
        if tier == 'unified':
            # a thick golden ring in three arcs around one bright point; glyphs march round the arc
            a0 = PI / 2 + idx * TAU / 3 - PI / 3
            def glyph(p, n):
                ang = np.arctan2(p[..., 2], p[..., 0])
                g = np.sin(ang * 18 - t * TAU * (1 + idx) * (1 if idx != 1 else -1)) > 0.5
                return 0.15 * np.sin(ang * 36), (g & (p[..., 1] < -0.02)).astype(float)
            parts = [Part(lambda p: np.maximum(sd_torus(p, 0.52, 0.14), _wedge(p, a0, a0 + TAU / 3, 0.03)), 0, rot=rotx(PI / 2)),
                     Part(lambda p: sd_torus(p, 0.52, 0.04), 1, rot=rotx(PI / 2)),
                     Part(lambda p: sd_sphere(p, 0.07 + 0.03 * math.sin(t * TAU * 2)), 2)]
            mats = [Material(c1, c2, metal=0.95, emissive=c2, pattern=glyph), ghost, Material(c2, '#ffffff', emissive='#ffffff', pattern=pat_all())]
            return parts, mats, dict(view_pitch=0.2, view_yaw=0.2), None
        # absolute: a black monolith in three stacked blocks; the piece scrolls white text, the rest is a white outline
        y0 = (idx - 1) * 0.47
        def text(p, n):
            y = p[..., 1] + t * 0.48 * (1 if idx != 1 else -1)
            row = np.round(y / 0.08)
            ln = 0.08 + 0.18 * _hs(np.mod(row, 6) * 3.3 + idx * 7)
            on = (np.abs(y / 0.08 - row) < 0.22) & (p[..., 0] < -0.2 + ln * 1.6) & (p[..., 0] > -0.24)
            front = p[..., 2] < -0.08
            edge = (np.abs(p[..., 0]) > 0.26) & front
            return np.zeros(y.shape), ((on & front) | edge).astype(float)
        parts = [Part(lambda p: sd_box(p, (0.3, 0.21, 0.11), 0.01), 0, pos=(0, y0, 0)),
                 Part(lambda p: sd_boxframe(p, (0.34, 0.7, 0.13), 0.035), 1)]
        mats = [Material('#060606', '#ffffff', metal=0.9, keep_dark=True, emissive='#ffffff', pattern=text),
                Material('#ffffff', '#ffffff', emissive='#c8c8c8', pattern=ghost.pattern)]
        return parts, mats, dict(view_pitch=0.2, view_yaw=0.45), None
    return b


def holographic_matrix(c1, c2, t):
    # a hollow sphere whose shell is covered in flickering bits, cut open to show there is nothing inside but a
    # projected cube - the 3D object stored on its 2D boundary
    def bits(p, n):
        a = np.arctan2(p[..., 2], p[..., 0])
        b = np.arcsin(np.clip(p[..., 1] / np.maximum(length(p), 1e-6), -1, 1))
        ca, cb = np.floor(a * 4), np.floor(b * 5)
        h = _hs(ca * 3.1 + cb * 17.3 + (int(t * 8) % 8) * 5.7)
        outer = length(p) > 0.6
        return np.where(outer, -0.2, -0.7), ((h > 0.62) & outer).astype(float)
    view = dict(view_pitch=0.35, view_yaw=0.6)
    vdir = np.array([0, 0, 1.0]) @ (rotx(-view['view_pitch']) @ roty(view['view_yaw']))
    cut = -vdir * 0.62 + np.array([0.25, 0.3, 0])
    parts = [Part(lambda p: np.abs(sd_sphere(p, 0.6)) - 0.05, 0),
             Part(lambda p: sd_sphere(p, 0.5), 0, pos=cut, sub=True),
             Part(lambda p: sd_box(p, (0.16, 0.16, 0.16), 0.02), 1, rot=roty(t * TAU) @ rotx(0.6) @ rotz(0.6))]
    mats = [Material(c1, c2, metal=0.7, emissive=c2, pattern=bits), Material(c2, c1, emissive=c2, pattern=pat_bands(10, 2, t, 0.3, axis=1))]
    return parts, mats, view, None


# =========================================================================================== ingots
def chronosteel_ingot(c1, c2, t):
    # a brass-gold ingot with a clock medallion set in its top; time is stopped around it except for the ticking hand
    tick = int(t * 12) % 12
    hand = -tick * TAU / 12 + PI / 2
    def face(p, n):
        x, z = p[..., 0], p[..., 2]
        r = np.sqrt(x * x + z * z)
        a = np.arctan2(z, x)
        top = p[..., 1] > 0.02
        ring = np.abs(r - 0.2) < 0.04
        ticks = (np.abs(np.mod(a, TAU / 4) - TAU / 8) > TAU / 8 - 0.3) & (np.abs(r - 0.13) < 0.04)
        hx, hz = math.cos(hand), math.sin(hand)
        along = x * hx + z * hz
        hnd = (np.abs(-x * hz + z * hx) < 0.04) & (along > -0.03) & (along < 0.17)
        return np.full(x.shape, -0.5), (top & (ring | ticks | hnd)).astype(float)
    parts = [Part(_ing, 0), Part(lambda p: sd_cyl(p, 0.26, 0.04, 0.015), 1, pos=(0, 0.31, 0))]
    mats = [Material(c1, c2, metal=0.85, gloss=0.8), Material('#3a2a10', c2, metal=0.9, keep_dark=True, emissive='#fff4d0', pattern=face)]
    motes = [(3, 3), (27, 6), (5, 26), (28, 24), (14, 2)]
    def ov(img):
        for i, (x, y) in enumerate(motes):              # dust that does not fall: frozen in mid-air
            _dot(img, x, y, H(c2), 0.7)
    return parts, mats, ING_VIEW, ov


def computronium_ingot(c1, c2, t):
    # a dark-green ingot with a processor die on top and circuit traces flowing with data over every face
    parts = [Part(_ing, 0), Part(lambda p: sd_box(p, (0.28, 0.05, 0.16), 0.02), 1, pos=(0, 0.33, 0))]
    mats = [Material(c1, c2, metal=0.85, emissive=c2, pattern=pat_combine(pat_traces(t, 2), pat_bands(9, 2, t, 0.08, axis=1))),
            Material('#101810', c2, metal=0.95, keep_dark=True, emissive=c2, pattern=pat_grid(10, t, 0.1))]
    return parts, mats, ING_VIEW, None


def cosmic_neutronium_ingot(c1, c2, t):
    # an almost-black ingot with starlight drifting through it
    def stars(p, n):
        x = p[..., 0] * 7 - t * 6
        cx = np.mod(np.floor(x), 6)
        h = _hs(cx * 7.1 + np.floor(p[..., 1] * 7) * 13.7 + np.floor(p[..., 2] * 7) * 3.3)
        fx = x - np.floor(x)
        tw = np.sin(h * 50 + t * TAU * 2) > -0.3
        return np.full(h.shape, -0.4), ((h > 0.78) & tw & (np.abs(fx - 0.5) < 0.4)).astype(float)
    parts = [Part(_ing, 0)]
    mats = [Material('#0c0c16', '#5060a0', metal=0.5, gloss=0.5, keep_dark=True, emissive=c2, pattern=stars)]
    return parts, mats, ING_VIEW, None


def crystalline_matrix_ingot(c1, c2, t):
    # a hexagonal crystal bar; two light fronts (from two dead stars) sweep through it from opposite ends
    def light(p, n):
        x = p[..., 0]
        a = np.abs(x - (-0.9 + 1.8 * t)) < 0.07
        b = np.abs(x - (0.9 - 1.8 * ((t + 0.5) % 1))) < 0.07
        g = np.abs(np.mod(x * 5, 1) - 0.5) < 0.06
        return 0.2 * g, (a | b).astype(float)
    parts = [Part(lambda p: sd_hexprism(p, 0.3, 0.82), 0, rot=rotz(PI / 2) @ roty(PI / 6))]
    mats = [Material(c1, c2, metal=0.6, gloss=1.5, emissive=c2, pattern=light)]
    return parts, mats, dict(view_pitch=0.45, view_yaw=0.6, zoom=1.0), None


def flerovium_ingot(c1, c2, t):
    # a steel-grey ingot held by two closed nuclear shells (magic numbers): rings orbiting it in opposite directions
    beads = lambda p, n: (np.zeros(p.shape[:-1]), (np.sin(np.arctan2(p[..., 2], p[..., 0]) * 7) > 0.55).astype(float))
    parts = [Part(lambda p: _ing(p, 0.62, 0.24, 0.36), 0),
             Part(lambda p: sd_torus(p, 0.82, 0.04), 1, rot=rotx(0.35) @ roty(t * TAU)),
             Part(lambda p: sd_torus(p, 0.7, 0.04), 1, rot=rotz(1.1) @ rotx(-0.3) @ roty(-t * TAU))]
    mats = [Material(c1, c2, metal=0.95), Material('#8098c0', '#ffffff', metal=0.9, emissive=c2, pattern=beads)]
    return parts, mats, ING_VIEW, None


def hypernova_alloy_ingot(c1, c2, t):
    # a pink ingot still glowing from the forge: plasma veins boil across its surface, its edges slightly molten
    def plasma(p, n):
        a = np.sin(p[..., 0] * 7 + np.sin(p[..., 2] * 9 + t * TAU) * 1.5 + t * TAU * 2)
        b = np.sin(p[..., 2] * 8 - p[..., 1] * 6 + np.sin(p[..., 0] * 5 - t * TAU * 2))
        v = a * b
        return 0.2 * v, (v > 0.15).astype(float)
    melt = lambda p: _ing(p) + 0.025 * np.sin(p[..., 0] * 11 + t * TAU) * np.sin(p[..., 2] * 9)
    parts = [Part(melt, 0)]
    mats = [Material(c1, c2, metal=0.7, emissive='#fff0d0', pattern=plasma)]
    return parts, mats, ING_VIEW, None


def massless_alloy(c1, c2, t):
    # a pale glassy ingot drifting high above a grid of Higgs-field dots that it never touches; it turns weightlessly
    def higgs(p, n):
        x, z = p[..., 0] * 4, p[..., 2] * 4
        d = (x - np.round(x)) ** 2 + (z - np.round(z)) ** 2 < 0.06
        return np.zeros(x.shape), (d & (np.sin(np.round(x) + np.round(z) * 2 + t * TAU) > -0.4)).astype(float)
    parts = [Part(lambda p: _ing(p, 0.62, 0.22, 0.34), 0, pos=(0, 0.3 + 0.08 * math.sin(t * TAU), 0),
                  rot=roty(t * TAU) @ rotz(0.15 * math.sin(t * TAU * 2))),
             Part(lambda p: sd_box(p, (0.78, 0.02, 0.78)), 1, pos=(0, -0.55, 0))]
    mats = [Material(c1, c2, metal=0.4, gloss=1.8, emissive='#ffffff', pattern=lambda p, n: (np.zeros(p.shape[:-1]), (np.abs(p[..., 0]) > 0.58).astype(float))),
            Material('#20283a', '#c0d0ff', keep_dark=True, emissive='#b0c0ff', pattern=higgs)]
    return parts, mats, dict(view_pitch=0.55, view_yaw=0.65, zoom=1.0), None


def metallic_hydrogen_ingot(c1, c2, t):
    # a liquid-silver ingot whose top ripples like mercury; little arcs of current crackle across it
    step = int(t * 10) % 10
    def zap(p, n):
        x, z = p[..., 0], p[..., 2]
        k = _hs(np.float64(step) * 3.7)
        path = 0.15 * np.sin(x * 8 + k * 20) + 0.08 * np.sin(x * 23 + k * 40) + (k - 0.5) * 0.3
        arc = (np.abs(z - path) < 0.04) & (np.abs(x - (k - 0.5)) < 0.45) & (step % 2 == 0)
        return 0.2 * np.sin(x * 6 - t * TAU * 2), arc.astype(float)
    wob = lambda p: _ing(p) + 0.03 * np.sin(p[..., 0] * 7 - t * TAU * 2) * (p[..., 1] > 0)
    parts = [Part(wob, 0)]
    mats = [Material(c1, c2, metal=1.0, gloss=1.7, emissive='#e0f8ff', pattern=zap)]
    return parts, mats, ING_VIEW, None


def negative_mass_ingot(c1, c2, t):
    # an upside-down ingot that hangs *above* its resting pad, pushed up by a pink anti-gravity ring
    def spots(p, n):
        v = np.sin(p[..., 0] * 10 + t * TAU * 2) * np.sin(p[..., 2] * 10)
        return np.zeros(v.shape), (v > 0.6).astype(float)
    parts = [Part(_ing, 0, pos=(0, 0.25 + 0.07 * math.sin(t * TAU), 0), rot=rotz(PI)),
             Part(lambda p: sd_torus(p, 0.5 + 0.04 * math.sin(t * TAU * 2), 0.05), 1, pos=(0, -0.48, 0))]
    mats = [Material(c1, c2, metal=0.9, emissive=c2, pattern=spots), Material(c2, '#ffffff', emissive=c2, pattern=pat_bands(8, -2, t, 0.25, axis=0))]
    return parts, mats, ING_VIEW, None


def neutronium_ingot(c1, c2, t):
    # a squat, impossibly dense ingot: gravity ripples sink into its top and light bends round its silhouette
    def sink(p, n):
        r = np.sqrt(p[..., 0] ** 2 * 0.5 + p[..., 2] ** 2)
        v = np.sin(r * 22 + t * TAU * 2)
        return -0.3 - 0.12 * (v > 0.6), ((v > 0.85) & (p[..., 1] > 0.18)).astype(float)
    parts = [Part(lambda p: _ing(p, 0.86, 0.22, 0.46), 0)]
    mats = [Material(c1, c2, metal=0.7, gloss=0.45, emissive=mix(H(c2), H(c1), 0.3), pattern=sink)]
    def ov(img):
        for k in range(2):                               # starlight dragged round the ingot on a tight path
            for j in range(7):
                a = TAU * (t + k / 2) - j * 0.1
                x = 15.3 + math.cos(a) * 14
                y = 16.3 + math.sin(a) * 9 - math.cos(a) * 2
                _dot(img, x, y, WHITE if j == 0 else H(c2), 1.0 - j / 6)
    return parts, mats, ING_VIEW, ov


def omnium_ingot(c1, c2, t):
    # every concept at once: each face a different colour, diagonal rainbow seams, cycling through the spectrum
    def facets(p, n):
        off = 0.25 * np.sin(p[..., 0] * 3 + p[..., 2] * 4)
        seam = np.abs(np.mod(p[..., 0] * 3 + p[..., 2] * 2 + p[..., 1] * 2 - t * 3, 1) - 0.5) < 0.08
        return off, seam.astype(float)
    parts = [Part(_ing, 0)]
    mats = [Material('#c06080', '#80ffff', metal=0.9, emissive='#ffffff', pattern=facets)]
    return parts, mats, ING_VIEW, None


def spacetime_alloy_ingot(c1, c2, t):
    # an ingot sagging into its own gravity well; its polished top reflects a turning spiral galaxy and stars
    def sag(p):
        q = p.copy()
        q[..., 1] = q[..., 1] + 0.14 * np.exp(-(p[..., 0] / 0.45) ** 2)
        return _ing(q) * 0.85
    def sky(p, n):
        x, z = p[..., 0], p[..., 2]
        r = np.sqrt(x * x + z * z * 2)
        a = np.arctan2(z, x)
        arm = (np.sin(a * 2 - r * 12 + t * TAU) > 0.7) & (r < 0.35)
        h = _hs(np.floor(x * 9) * 5.1 + np.floor(z * 9) * 9.7)
        star = (h > 0.86) & (np.sin(h * 30 + t * TAU * 2) > 0)
        up = n[..., 1] > 0.6
        return -0.2 * up, ((arm | star | (r < 0.06)) & up).astype(float)
    parts = [Part(sag, 0)]
    mats = [Material(c1, c2, metal=0.95, emissive=c2, pattern=sky)]
    return parts, mats, ING_VIEW, None


def stellar_iron_ingot(c1, c2, t):
    # rusty star-iron, glowing hot underneath; fusion flashes try to start on top and fizzle out (iron is the end)
    def heat(p, n):
        hot = p[..., 1] < -0.12 + 0.05 * np.sin(p[..., 0] * 9 + t * TAU * 2)
        lat = (np.abs(np.mod(p[..., 0] * 6, 1) - 0.5) < 0.12) & (np.abs(np.mod(p[..., 2] * 6, 1) - 0.5) < 0.12)
        return 0.12 * lat, hot.astype(float)
    parts = [Part(_ing, 0)]
    mats = [Material(c1, c2, metal=0.75, emissive='#ff8040', pattern=pat_combine(heat, pat_noise_shade(14, 0.15, 3)))]
    spots = [(9, 10), (20, 9), (14, 13), (24, 13)]
    def ov(img):
        k = int(t * 4) % 4
        ph = (t * 4) % 1
        x, y = spots[k]
        if ph < 0.5:
            _dot(img, x, y, WHITE, 1 - ph * 2)
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                _dot(img, x + dx * (1 + ph * 4), y + dy * (1 + ph * 4), H(c2), (1 - ph * 2) * 0.7)
    return parts, mats, ING_VIEW, ov


def transcendent_metal_ingot(c1, c2, t):
    # a white metal that cannot decide its shape: it flows from ingot to sphere to octahedron to twisted bar
    w = [max(0.0, math.cos(TAU * t - k * TAU / 4)) ** 2 for k in range(4)]
    s = sum(w)
    w = [x / s for x in w]
    def morph(p):
        q = p.copy()
        tw = 1.2 * p[..., 0]
        c, sn = np.cos(tw), np.sin(tw)
        q[..., 1], q[..., 2] = c * p[..., 1] - sn * p[..., 2], sn * p[..., 1] + c * p[..., 2]
        return (w[0] * _ing(p) + w[1] * sd_sphere(p, 0.5) + w[2] * sd_octa(p, 0.72) + w[3] * sd_box(q, (0.8, 0.14, 0.2), 0.04) * 0.8)
    parts = [Part(morph, 0)]
    mats = [Material(c1, c2, metal=0.9, gloss=1.5, emissive='#e0e0ff', pattern=pat_bands(10, 2, t, 0.12, axis=1))]
    return parts, mats, dict(view_pitch=0.45, view_yaw=0.65, zoom=1.05), None


def unbihexium_ingot(c1, c2, t):
    # a heavy violet ingot with a glowing island of stability on top: contour rings around a pulsing core bead
    def contours(p, n):
        x, z = p[..., 0], p[..., 2]
        r = np.sqrt(np.maximum((x / 1.6) ** 2 + z ** 2 + 0.01 * np.sin(np.arctan2(z, x) * 3), 0))
        ring = np.abs(np.mod(r * 9 - t * 2, 1) - 0.5) < 0.12
        return -0.35 + 0.0 * r, (ring & (p[..., 1] > 0.26) & (r < 0.3)).astype(float)
    parts = [Part(_ing, 0), Part(lambda p: sd_sphere(p, 0.13), 1, pos=(0, 0.31, 0))]
    mats = [Material(c1, c2, metal=0.6, gloss=0.45, emissive=c2, pattern=contours), Material(c2, '#ffffff', emissive=c2, pattern=pat_pulse_core(t))]
    return parts, mats, ING_VIEW, None


# =========================================================================================== lattices
def causal_shield_lattice(c1, c2, t):
    # a gold hexagonal shield of triangular chronosteel lattice; a red paradox strikes it and is shunted aside
    hitp = 0.4
    def lat(p, n):
        x, z = p[..., 0], p[..., 2]
        lines = np.zeros(x.shape, bool)
        for k in range(3):
            a = k * PI / 3
            u = (x * math.cos(a) + z * math.sin(a)) * 3
            lines |= np.abs(u - np.round(u)) < 0.1
        flash = math.exp(-((t - hitp) / 0.05) ** 2)
        chron = np.sin(x * 5 + z * 3 - t * TAU * 2) > -0.6 - 0.5 * flash
        face = np.abs(p[..., 1]) > 0.04
        return -0.25 + 0.3 * lines, (lines & chron & face).astype(float)
    parts = [Part(lambda p: sd_hexprism(p, 0.66, 0.06), 0, rot=rotx(PI / 2) @ roty(PI / 6))]
    mats = [Material('#6a5020', c2, metal=0.95, emissive=c2, pattern=lat)]
    def ov(img):
        if t < hitp:
            u = t / hitp
            path = [(34 - 18 * u + k * 1.1, -2 + 17 * u - k * 1.0) for k in range(4)]
        elif t < hitp + 0.3:
            u = (t - hitp) / 0.3
            path = [(16 - 16 * u + k * 1.2, 15 + 4 * u - k * 0.3) for k in range(4)]
        else:
            return
        for k, (x, y) in enumerate(path):
            _dot(img, x, y, WHITE if k == 0 else H('#ff4040'), 1 - k / 4)
        if hitp <= t < hitp + 0.08:
            sparkle(img, 8, 7, H('#ff8040'), 2)
    return parts, mats, dict(view_pitch=0.2, view_yaw=0.3), ov


def cherenkov_lattice(c1, c2, t):
    # a diamond octahedral wire frame around a black netherite core; blue Cherenkov light races along the struts
    v = [np.array(a, float) * 0.72 for a in ((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1))]
    edges = [(v[i], v[j]) for i in range(6) for j in range(i + 1, 6) if abs(v[i] @ v[j]) < 1e-6]
    def blue(p, n):
        s = p[..., 0] * 1.3 + p[..., 1] * 2.1 - p[..., 2]
        return np.zeros(s.shape), (np.sin(s * 6 - t * TAU * 3) > 0.3).astype(float)
    parts = [Part(lambda p: _segs(p, edges, 0.055), 0, rot=roty(0.3)),
             Part(lambda p: sd_box(p, (0.2, 0.2, 0.2), 0.03), 1, rot=roty(t * TAU) @ rotx(0.6) @ rotz(0.6))]
    mats = [Material('#a8d8e8', c2, metal=0.9, emissive=c2, pattern=blue),
            Material('#201c22', '#80f0ff', metal=0.6, keep_dark=True, emissive='#40a0ff', pattern=lambda p, n: (np.zeros(p.shape[:-1]), (np.abs(p).max(-1) < 0.0).astype(float)))]
    return parts, mats, dict(view_pitch=0.3, view_yaw=0.5), None


def eigenstate_lattice(c1, c2, t):
    # energy levels as plates on a rail (spaced like n^2); a glowing state jumps between them, never in between
    ys = [-0.6, -0.36, 0.04, 0.6]
    seq = [0, 2, 1, 3, 0, 1, 2, 0]
    k = int(t * 8) % 8
    lv = seq[k]
    def lit(p, n, y=0.0):
        return np.zeros(p.shape[:-1]), (np.abs(length(p * np.array([1, 0, 1])) - 0.38) < 0.06).astype(float)
    parts = [Part(lambda p: sd_cyl(p, 0.035, 0.7), 0, pos=(-0.5, -0.1, 0))]
    mats = [Material('#607080', '#ffffff', metal=0.9), Material(c1, c2, metal=0.8, emissive=c2, pattern=lit), Material(c1, c2, metal=0.8),
            Material(c2, '#ffffff', emissive='#ffffff', pattern=pat_all())]
    for i, y in enumerate(ys):
        parts.append(Part(lambda p, i=i: sd_cyl(p, 0.48 - i * 0.03, 0.035, 0.01), 1 if i == lv else 2, pos=(0, y, 0)))
    parts.append(Part(lambda p: sd_sphere(p, 0.1), 3, pos=(0.22, ys[lv] + 0.12, -0.1)))
    def ov(img):
        ph = (t * 8) % 1
        prev = seq[(k - 1) % 8]
        if prev > lv and ph < 0.35:                      # dropping a level emits a photon
            yy = 16 - ys[lv] * 15
            for j in range(4):
                _dot(img, 21 + ph * 26 + j, yy - 2 + (j % 2) * 2, H(c2), 0.9)
    return parts, mats, dict(view_pitch=0.5, view_yaw=0.5), ov


def gravitational_wave_crystal(c1, c2, t):
    # a ring of test masses round a crystal, squeezed and stretched by a passing gravitational wave (+ then x)
    h = 0.28 * math.sin(t * TAU * 2)
    cross = (t % 0.5) >= 0.25
    pts = []
    for i in range(8):
        a = i * TAU / 8
        x, y = math.cos(a) * 0.66, math.sin(a) * 0.66
        if cross:
            x, y = x + h * y * 0.0, y
            u, v = (x + y) / math.sqrt(2), (x - y) / math.sqrt(2)
            u, v = u * (1 + h), v * (1 - h)
            x, y = (u + v) / math.sqrt(2), (u - v) / math.sqrt(2)
        else:
            x, y = x * (1 + h), y * (1 - h)
        pts.append((x, y, 0))
    def ripple(p, n):
        r = length(p)
        return np.zeros(r.shape), (np.sin(r * 20 - t * TAU * 4) > 0.5).astype(float)
    sx, sy = (1 + h * 0.5), (1 - h * 0.5)
    parts = [Part(lambda p: sd_gem(p / np.array([sx, sy, 1]), 0.24, 0.42) * min(sx, sy), 0, rot=rotz(0.0)),
             Part(lambda p: _balls(p, pts, 0.09), 1)]
    mats = [Material(c1, c2, metal=0.8, emissive=c2, pattern=ripple), Material('#c0e0ff', '#ffffff', metal=0.9, emissive=c2, pattern=pat_all())]
    return parts, mats, dict(view_pitch=0.15, view_yaw=0.15), None


def metal_organic_framework(c1, c2, t):
    # a cube of orange metal nodes and dark organic struts; small guest molecules wander in and out of the pores
    s = 0.5
    corners = [np.array([x, y, z]) * s for x in (-1, 1) for y in (-1, 1) for z in (-1, 1)]
    edges = [(a, b) for i, a in enumerate(corners) for b in corners[i + 1:] if np.sum(np.abs(a - b) > 1e-6) == 1]
    guests = []
    for k in range(3):
        a = TAU * (t + k / 3)
        guests.append((0.75 * math.cos(a) * math.cos(a * 0 + k), 0.3 * math.sin(a * 2 + k), 0.75 * math.sin(a) * 0.6))
    parts = [Part(lambda p: _segs(p, edges, 0.05), 0), Part(lambda p: _balls(p, corners, 0.13), 1),
             Part(lambda p: _balls(p, guests, 0.07), 2)]
    mats = [Material(c1, '#80a0c0', metal=0.4), Material(c2, '#ffe0a0', metal=0.9, emissive=c2, pattern=pat_pulse_core(t)),
            Material('#a0ffff', '#ffffff', emissive='#a0ffff', pattern=pat_all())]
    return parts, mats, dict(view_pitch=0.4, view_yaw=0.55), None


def neural_lattice(c1, c2, t):
    # a 3-4-3 network of photonic neurons; signals fire layer by layer, left to right
    L = [[(-0.6, y, z) for y, z in ((0.35, 0.1), (0.0, -0.1), (-0.35, 0.1))],
         [(0.0, y, z) for y, z in ((0.5, 0.0), (0.17, 0.15), (-0.17, -0.15), (-0.5, 0.0))],
         [(0.6, y, z) for y, z in ((0.35, -0.1), (0.0, 0.1), (-0.35, -0.1))]]
    edges = [(np.array(a), np.array(b)) for i in range(2) for a in L[i] for b in L[i + 1]]
    nodes = [q for l in L for q in l]
    def fire(p, n):
        x = p[..., 0]
        front = -0.8 + ((t * 2) % 1) * 1.8
        return np.zeros(x.shape), (np.abs(x - front) < 0.12).astype(float)
    parts = [Part(lambda p: _segs(p, edges, 0.03), 0), Part(lambda p: _balls(p, nodes, 0.11), 1)]
    mats = [Material(c1, c2, metal=0.5, emissive=c2, pattern=fire),
            Material('#40a070', '#ffffff', metal=0.5, emissive=c2, pattern=lambda p, n: (np.zeros(p.shape[:-1]), (np.abs(p[..., 0] - (-0.8 + ((t * 2) % 1) * 1.8)) < 0.3).astype(float)))]
    return parts, mats, dict(view_pitch=0.2, view_yaw=0.25), None


def russell_paradox_set(c1, c2, t):
    # (the item plays anim.nar_droste; this builder is the still model) nested rings, each the same set again
    parts, mats = [], [Material(c1, c2, metal=0.8), Material(c2, '#ffffff', emissive=c2, pattern=pat_all())]
    for k, r in enumerate((0.72, 0.46, 0.28)):
        a = t * TAU * (1 if k % 2 == 0 else -1)
        parts.append(Part(lambda p, r=r: sd_torus(p, r, 0.06), 0, rot=rotx(0.9) @ roty(a)))
        parts.append(Part(lambda p, r=r: sd_torus(p, r, 0.06), 0, rot=rotz(1.4) @ rotx(-a)))
    parts.append(Part(lambda p: sd_sphere(p, 0.12), 1))
    return parts, mats, dict(view_pitch=0.3, view_yaw=0.5), None


def spinor_lattice(c1, c2, t):
    # a Moebius band: a light marker runs round it twice per loop and only then is back on the side it started
    R, w, th = 0.52, 0.2, 0.04
    Th = TAU * 2 * t
    def mob(p):
        x, y, z = p[..., 0], p[..., 1], p[..., 2]
        a = np.arctan2(z, x)
        u = np.sqrt(x * x + z * z) - R
        c, s = np.cos(a / 2), np.sin(a / 2)
        u2, v2 = u * c + y * s, -u * s + y * c
        return sd_box(np.stack([u2, v2, np.zeros_like(u2)], -1), (w, th, 1.0), 0.01) * 0.7
    def mark(p, n):
        x, y, z = p[..., 0], p[..., 1], p[..., 2]
        a = np.arctan2(z, x)
        u = np.sqrt(x * x + z * z) - R
        v2 = -u * np.sin(a / 2) + y * np.cos(a / 2)
        k = np.round((Th - a) / TAU)
        d = np.abs(Th - a - k * TAU)
        side = v2 * np.where(np.mod(k, 2) == 0, 1, -1) > 0
        edge = np.abs(u * np.cos(a / 2) + y * np.sin(a / 2)) > w - 0.05
        return -0.1 * edge, (((d < 0.45) & side) | edge).astype(float)
    parts = [Part(mob, 0, rot=rotx(0.2))]
    mats = [Material(c1, c2, metal=0.8, emissive=c2, pattern=mark)]
    return parts, mats, dict(view_pitch=0.75, view_yaw=0.5, zoom=1.05), None


def thought_matrix(c1, c2, t):
    # eight identical glass cells each holding the same pink thought; a copy-pulse jumps from cell to cell
    k = int(t * 8) % 8
    order = [0, 1, 3, 2, 6, 7, 5, 4]
    def cells(p):
        q = np.abs(p) - 0.3
        return sd_box(q, (0.24, 0.24, 0.24), 0.04)
    def thought(p, n):
        q = p - np.sign(p) * 0.3
        idx = (p[..., 0] > 0) * 1 + (p[..., 1] > 0) * 2 + (p[..., 2] > 0) * 4
        v = np.sin(q[..., 0] * 11 + np.sin(q[..., 1] * 9) * 2) * np.sin(q[..., 2] * 10 + q[..., 1] * 6)
        bright = idx == order[k]
        return 0.1 * v, ((v > 0.35) | (bright & (v > -0.3))).astype(float)
    parts = [Part(cells, 0, rot=roty(0.0))]
    mats = [Material(c1, c2, metal=0.6, gloss=1.4, emissive=c2, pattern=thought)]
    return parts, mats, dict(view_pitch=0.45, view_yaw=0.6), None


def zeolite_catalyst(c1, c2, t):
    # a porous pale crystal riddled with molecule-sized cages; pores light up as reactions happen inside them
    def porous(p):
        s = 0.28
        q = p - np.clip(np.round(p / s), -2, 2) * s
        return np.maximum(sd_box(p, (0.56, 0.56, 0.56), 0.04), -sd_sphere(q, 0.115))
    def react(p, n):
        s = 0.28
        cell = np.clip(np.round(p / s), -2, 2)
        inner = sd_box(p, (0.56, 0.56, 0.56), 0.04) < -0.015
        h = _hs(cell[..., 0] * 3 + cell[..., 1] * 7 + cell[..., 2] * 13)
        on = np.mod(h - t * 2, 1) < 0.25
        return np.zeros(h.shape), (inner & on).astype(float)
    parts = [Part(porous, 0, rot=roty(0.0))]
    mats = [Material(c1, c2, metal=0.4, emissive='#ffe080', pattern=react)]
    return parts, mats, dict(view_pitch=0.5, view_yaw=0.6), None


# =========================================================================================== lenses
def containment_cell(c1, c2, t):
    # an empty horizontal glass flask in steel collars; three magnetic coils pulse in turn to keep the field sealed
    def coil(p, n, i=0):
        on = int(t * 6) % 3 == i
        return np.zeros(p.shape[:-1]), np.full(p.shape[:-1], 1.0 if on else 0.0)
    def glass(p, n):
        return 0.3 * np.sin(p[..., 0] * 4 - t * TAU) - 0.15, np.zeros(p.shape[:-1])
    parts = [Part(lambda p: sd_capsule(p, (-0.5, 0, 0), (0.5, 0, 0), 0.3), 0),
             Part(lambda p: sd_cyl(p, 0.36, 0.08, 0.02), 1, pos=(-0.62, 0, 0), rot=rotz(PI / 2)),
             Part(lambda p: sd_cyl(p, 0.36, 0.08, 0.02), 1, pos=(0.62, 0, 0), rot=rotz(PI / 2))]
    mats = [Material('#283848', c2, metal=1.0, gloss=1.8, pattern=glass), Material(c1, '#ffffff', metal=0.95)]
    for i, x in enumerate((-0.3, 0.0, 0.3)):
        mats.append(Material('#a06030', c2, metal=0.9, emissive=c2, pattern=lambda p, n, i=i: coil(p, n, i)))
        parts.append(Part(lambda p: sd_torus(p, 0.32, 0.045), len(mats) - 1, pos=(x, 0, 0), rot=rotz(PI / 2)))
    return parts, mats, dict(view_pitch=0.35, view_yaw=0.45), None


def cosmic_lens(c1, c2, t):
    # a wobbling loop of cosmic string round a clear lens; parallel rays enter and converge to a flaring focus
    def loop(p):
        a = np.arctan2(p[..., 2], p[..., 0])
        rr = 0.58 + 0.035 * np.sin(a * 7 + t * TAU * 2)
        q = np.stack([np.sqrt(p[..., 0] ** 2 + p[..., 2] ** 2) - rr, p[..., 1]], -1)
        return (length(q) - 0.06) * 0.8
    lensf = lambda p: np.maximum(sd_sphere(p - np.array([0, -1.2, 0]), 1.3), sd_sphere(p - np.array([0, 1.2, 0]), 1.3))
    parts = [Part(loop, 0, rot=rotz(PI / 2) @ roty(0)), Part(lensf, 1, rot=rotz(PI / 2))]
    mats = [Material(c1, c2, emissive='#fff8e0', pattern=pat_bands(7, 2, t, 0.35, axis=2)),
            Material('#60708a', '#ffffff', metal=0.3, gloss=1.8, pattern=lambda p, n: (0.3 * np.sin(p[..., 2] * 5 - t * TAU), np.zeros(p.shape[:-1])))]
    def ov(img):
        fx, fy = 29, 15.5
        for j, y0 in enumerate((8, 15.5, 23)):
            for k in range(0, 16, 2):
                u = (k + t * 2 * 2) % 16 / 16
                x = 0 + u * 14 if True else 0
                _dot(img, x, y0, H(c1), 0.35)
            for k in range(6):
                u = k / 6
                _dot(img, 17 + (fx - 17) * u, y0 + (fy - y0) * u, H(c2), 0.6)
        if (t * 2) % 1 < 0.5:
            sparkle(img, 14, 7, H(c1), 2 if (t * 2) % 1 < 0.25 else 1)
    return parts, mats, dict(view_pitch=0.05, view_yaw=0.35), ov


def cryptochrome_magnetoreceptor(c1, c2, t):
    # a robin's eye: violet striated iris, dark pupil; an entangled radical pair circles it, colours flipping together
    def iris(p, n):
        r = np.sqrt(p[..., 0] ** 2 + p[..., 1] ** 2)
        a = np.arctan2(p[..., 1], p[..., 0])
        front = p[..., 2] < -0.25
        ir = (r < 0.28) & front
        pupil = (r < 0.11) & front
        stri = np.sin(a * 14 + np.sin(t * TAU) * 0.5) > 0.0
        return np.where(pupil, -2.0, np.where(ir, 0.1 * stri, 0.0)), (ir & ~pupil & (stri | (r > 0.22))).astype(float)
    a = t * TAU
    up = (t % 0.5) < 0.25
    p1 = (0.66 * math.cos(a), 0.3 * math.sin(a), 0.5 * math.sin(a))
    p2 = (-p1[0], -p1[1], -p1[2])
    parts = [Part(lambda p: sd_sphere(p, 0.46), 0, rot=roty(0.15 * math.sin(t * TAU)) @ rotx(0.1 * math.sin(t * TAU * 2))),
             Part(lambda p: sd_sphere(p, 0.08), 1, pos=p1), Part(lambda p: sd_sphere(p, 0.08), 2, pos=p2)]
    mats = [Material('#d8d0e8', c2, metal=0.3, gloss=1.4, emissive=c2, pattern=iris),
            Material('#ffd060', '#ffffff', emissive='#ffd060' if up else '#60c0ff', pattern=pat_all()),
            Material('#60c0ff', '#ffffff', emissive='#60c0ff' if up else '#ffd060', pattern=pat_all())]
    return parts, mats, dict(view_pitch=0.0, view_yaw=0.0), None


# =========================================================================================== registry
ITEMS = {
    'r_process_cluster': r_process_cluster, 'string_coupling_crystal': string_coupling_crystal,
    'banach_tarski_sphere': banach_tarski_sphere, 'hyperdimensional_pearl': hyperdimensional_pearl,
    'ocean_simulacrum': ocean_simulacrum, 'sterile_neutrino': sterile_neutrino, 'virtual_graviton': virtual_graviton,
    'virtual_monopole': virtual_monopole, 'virtual_photon': virtual_photon, 'virtual_quark': virtual_quark,
    'void_essence': void_essence,
    'e8_lattice': e8_lattice, 'error_correction_lattice': error_correction_lattice, 'metamaterial_sheet': metamaterial_sheet,
    'molecular_orbital_lattice': molecular_orbital_lattice, 'quantum_field_lattice': quantum_field_lattice,
    'standard_model_matrix': standard_model_matrix,
    'holographic_matrix': holographic_matrix,
    'chronosteel_ingot': chronosteel_ingot, 'computronium_ingot': computronium_ingot,
    'cosmic_neutronium_ingot': cosmic_neutronium_ingot, 'crystalline_matrix_ingot': crystalline_matrix_ingot,
    'flerovium_ingot': flerovium_ingot, 'hypernova_alloy_ingot': hypernova_alloy_ingot, 'massless_alloy': massless_alloy,
    'metallic_hydrogen_ingot': metallic_hydrogen_ingot, 'negative_mass_ingot': negative_mass_ingot,
    'neutronium_ingot': neutronium_ingot, 'omnium_ingot': omnium_ingot, 'spacetime_alloy_ingot': spacetime_alloy_ingot,
    'stellar_iron_ingot': stellar_iron_ingot, 'transcendent_metal_ingot': transcendent_metal_ingot,
    'unbihexium_ingot': unbihexium_ingot,
    'causal_shield_lattice': causal_shield_lattice, 'cherenkov_lattice': cherenkov_lattice,
    'eigenstate_lattice': eigenstate_lattice, 'gravitational_wave_crystal': gravitational_wave_crystal,
    'metal_organic_framework': metal_organic_framework, 'neural_lattice': neural_lattice,
    'russell_paradox_set': russell_paradox_set, 'spinor_lattice': spinor_lattice, 'thought_matrix': thought_matrix,
    'zeolite_catalyst': zeolite_catalyst,
    'containment_cell': containment_cell, 'cosmic_lens': cosmic_lens, 'cryptochrome_magnetoreceptor': cryptochrome_magnetoreceptor,
}
for _tier in TIER_NOTE:
    for _i in range(3):
        ITEMS[f'{_tier}_notes_{_i + 1}'] = _notes(_tier, _i)

N = ()
RECIPES = {
    'r_process_cluster': (('rock',), N), 'string_coupling_crystal': (('rock',), ('sparkles',)),
    'banach_tarski_sphere': (N, N), 'hyperdimensional_pearl': (N, N), 'ocean_simulacrum': (('rock',), N),
    'sterile_neutrino': (N, N), 'virtual_graviton': (N, N), 'virtual_monopole': (N, N), 'virtual_photon': (N, N),
    'virtual_quark': (N, N), 'void_essence': (N, N),
    'e8_lattice': (N, ('sparkles',)), 'error_correction_lattice': (N, N), 'metamaterial_sheet': (N, N),
    'molecular_orbital_lattice': (('rock',), N), 'quantum_field_lattice': (N, N), 'standard_model_matrix': (N, ('glint',)),
    'holographic_matrix': (('breathe',), N),
    'empirical_notes_1': (('bob',), N), 'empirical_notes_2': (('breathe',), ('glint',)), 'empirical_notes_3': (('pulse',), N),
    'theoretical_notes_1': (('rock',), N), 'theoretical_notes_2': (('bob',), ('sparkles',)), 'theoretical_notes_3': (('breathe',), N),
    'unified_notes_1': (('pulse',), ('glint',)), 'unified_notes_2': (('rock',), N), 'unified_notes_3': (('bob', 'breathe'), N),
    'absolute_notes_1': (('breathe',), N), 'absolute_notes_2': (('bob',), ('glint',)), 'absolute_notes_3': (('rock',), ('sparkles',)),
    'chronosteel_ingot': (N, N), 'computronium_ingot': (('breathe',), ('glint',)), 'cosmic_neutronium_ingot': (('rock',), ('sparkles',)),
    'crystalline_matrix_ingot': (('tumble',), ('sparkles',)), 'flerovium_ingot': (N, N), 'hypernova_alloy_ingot': (('breathe',), ('embers',)),
    'massless_alloy': (N, ('sparkles',)), 'metallic_hydrogen_ingot': (('rock',), N), 'negative_mass_ingot': (N, ('embers',)),
    'neutronium_ingot': (N, N), 'omnium_ingot': (('hue',), ('glint', 'sparkles')), 'spacetime_alloy_ingot': (('rock',), ('glint',)),
    'stellar_iron_ingot': (N, ('glint',)), 'transcendent_metal_ingot': (('spin',), ('sparkles',)), 'unbihexium_ingot': (('heartbeat',), N),
    'causal_shield_lattice': (N, N), 'cherenkov_lattice': (('spin',), ('sparkles',)), 'eigenstate_lattice': (N, N),
    'gravitational_wave_crystal': (N, N), 'metal_organic_framework': (('rock',), N), 'neural_lattice': (('rock',), N),
    'russell_paradox_set': (N, N), 'spinor_lattice': (('spin',), N), 'thought_matrix': (('spin',), N),
    'zeolite_catalyst': (('tumble',), ('sparkles',)),
    'containment_cell': (('rock',), ('glint',)), 'cosmic_lens': (N, N), 'cryptochrome_magnetoreceptor': (N, N),
}
NARRATIVES = {}
FRAMES = {}
