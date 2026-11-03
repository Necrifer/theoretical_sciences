"""Round-11 item art, group 4: wafers, probes, keys, seeds, shards, shifters, spindles, cores, strings, swarms,
tesseracts, throats, universes and warp parts. Every item has its own model, surface and motion."""
import math
import numpy as np
from items3d import *      # builders, patterns, Part, Material, sd_*, rot*, H, TAU ...
import anim


# =============================================================================================== helpers
def _hash(a, b=0.0, c=0.0, s=0.0):
    v = np.sin(a * 12.9898 + b * 78.233 + c * 37.719 + s * 4.581) * 43758.5453
    return v - np.floor(v)


def _wrap(a):
    return np.mod(a + math.pi, TAU) - math.pi


def _smin(a, b, k):
    h = np.clip(0.5 + 0.5 * (b - a) / k, 0, 1)
    return b * (1 - h) + a * h - k * h * (1 - h)


def _ell(r):
    """Ellipsoid of radii r (approximate distance)."""
    r = np.asarray(r, float)
    return lambda p: sd_sphere(p / r, 1.0) * r.min()


def _segs(A, B, r):
    """Union of capsules A[i]-B[i] (one Part, many segments)."""
    A = [np.asarray(a, float) for a in A]
    B = [np.asarray(b, float) for b in B]
    rs = list(r) if hasattr(r, '__len__') else [r] * len(A)

    def f(p):
        d = None
        for a, b, rr in zip(A, B, rs):
            pa, ba = p - a, b - a
            hh = np.clip((pa * ba).sum(-1) / max(float((ba * ba).sum()), 1e-9), 0, 1)
            di = length(pa - ba * hh[..., None]) - rr
            d = di if d is None else np.minimum(d, di)
        return d
    return f


def _curve(fn, n, r):
    pts = [np.asarray(fn(i / n), float) for i in range(n + 1)]
    return _segs(pts[:-1], pts[1:], r)


def _swap_xy(p):
    """Torus helper: sd_torus(_swap_xy(p)) lies in the xy plane (faces the camera)."""
    return p[..., [0, 2, 1]]


def _wafer_disk(p):
    return np.maximum(sd_cyl(p, 0.78, 0.05, 0.02), -(p[..., 2] + 0.74))


def _proj(pt, view):
    """World point -> (x, y, depth) on the 32x32 image, matching render()."""
    cam = rotx(-view.get('view_pitch', 0.5)) @ roty(view.get('view_yaw', 0.785))
    c = np.asarray(pt, float) @ cam.T
    k = view.get('zoom', 1.0) / view.get('extent', 1.0)
    return (c[0] * k + 1) * 16 - 0.5, (-c[1] * k + 1) * 16 - 0.5, c[2]


def _px(img, x, y, rgb, a):
    anim._blend(img, int(round(y)), int(round(x)), rgb, float(np.clip(a, 0, 1)))


def _px_empty(img, x, y, rgb, a):
    xi, yi = int(round(x)), int(round(y))
    if 0 <= xi < img.shape[1] and 0 <= yi < img.shape[0] and img[yi, xi, 3] < 0.5:
        anim._blend(img, yi, xi, rgb, float(np.clip(a, 0, 1)))


def _line(img, x0, y0, x1, y1, rgb, a=1.0, empty_only=False):
    n = int(max(abs(x1 - x0), abs(y1 - y0))) + 1
    for i in range(n + 1):
        s = i / n
        (_px_empty if empty_only else _px)(img, x0 + (x1 - x0) * s, y0 + (y1 - y0) * s, rgb, a)


def _dark(m):
    """Make a material read as near-black (horizons, voids)."""
    m.ramp[:] = np.array([[0.0, 0.0, 0.02], [0.02, 0.02, 0.05], [0.04, 0.04, 0.08], [0.07, 0.06, 0.11], [0.12, 0.1, 0.16], [0.3, 0.28, 0.35]])
    return m


def _smooth(x):
    x = min(1.0, max(0.0, x))
    return x * x * (3 - 2 * x)


# =============================================================================================== plates / wafers
def qubit_array_wafer(c1, c2, t):
    """A wafer tiled with qubit dots that flip state at random; a metamaterial lens circles above and engraves."""
    la = TAU * t
    lp = np.array([0.36 * math.cos(la), 0.0, 0.36 * math.sin(la)])

    def pat(p, n):
        x, z = p[..., 0], p[..., 2]
        cx, cz = np.floor(x * 6), np.floor(z * 6)
        fx, fz = x * 6 - cx - 0.5, z * 6 - cz - 0.5
        topf = n[..., 1] > 0.5
        dot = (fx * fx + fz * fz < 0.1) & topf & (x * x + z * z < 0.5)
        on = np.sin(TAU * (2 * t + _hash(cx, cz))) > 0.25
        near = (x - lp[0]) ** 2 + (z - lp[2]) ** 2 < 0.06
        grid = ((np.abs(fx) > 0.44) | (np.abs(fz) > 0.44)) & topf
        return -0.18 * grid + 0.1 * dot, (dot & (on | near)).astype(float)
    parts = [Part(_wafer_disk, 0),
             Part(_ell((0.26, 0.05, 0.26)), 1, pos=(lp[0], 0.42, lp[2])),
             Part(lambda p: sd_torus(p, 0.27, 0.04), 2, pos=(lp[0], 0.42, lp[2])),
             Part(lambda p: sd_capsule(p, (0, 0, 0), (0, 0.38, 0), 0.035), 3, pos=(lp[0], 0.03, lp[2]))]
    mats = [Material(c1, c2, metal=0.9, gloss=1.4, emissive=c2, pattern=pat),
            Material('#a0e8ff', '#ffffff', metal=0.2, gloss=1.8),
            Material('#707888', '#ffffff', metal=1.0),
            Material(c2, '#ffffff', emissive=c2, pattern=pat_all())]
    return parts, mats, dict(view_pitch=0.8, view_yaw=0.5), None


def room_temperature_superconductor(c1, c2, t):
    """A dark tile with a magnet levitating over it (Meissner effect); screening currents ring out beneath."""
    y = 0.42 + 0.05 * math.sin(TAU * t)

    def rings(p, n):
        r = np.sqrt(p[..., 0] ** 2 + p[..., 2] ** 2)
        e = (np.sin(r * 16 - TAU * t * 2) > 0.75) & (n[..., 1] > 0.5) & (r < 0.62)
        return -0.1 * (r > 0.62), e.astype(float)
    rot = roty(TAU * t)
    parts = [Part(lambda p: sd_box(p, (0.72, 0.09, 0.72), 0.03), 0),
             Part(lambda p: sd_box(p, (0.2, 0.08, 0.2), 0.03), 1, pos=(0, y + 0.08, 0), rot=rot),
             Part(lambda p: sd_box(p, (0.2, 0.08, 0.2), 0.03), 2, pos=(0, y - 0.08, 0), rot=rot),
             Part(lambda p: sd_torus(p, 0.36, 0.025), 3, pos=(0, y, 0), rot=rotx(0.15 * math.sin(TAU * t)))]
    mats = [Material(c1, c2, metal=0.95, gloss=1.5, emissive=c2, pattern=rings),
            Material('#d04848', '#ffd0d0', metal=0.8), Material('#4870d8', '#d0e0ff', metal=0.8),
            Material(c2, '#ffffff', emissive=c2, pattern=pat_bands(10, 2, t, 0.3, axis=0))]
    return parts, mats, dict(view_pitch=0.55, view_yaw=0.785), None


def topological_board(c1, c2, t):
    """A circuit board whose current runs only around its protected edge - a chase light circling the rim."""
    def pat(p, n):
        x, z = p[..., 0], p[..., 2]
        m = np.maximum(np.abs(x) / 0.78, np.abs(z) / 0.6)
        rim = (m > 0.84) & (m < 0.93) & (n[..., 1] > 0.5)
        ang = np.arctan2(z / 0.6, x / 0.78)
        chase = np.sin(ang * 4 - TAU * t * 2) > 0.2
        inner = (np.abs(np.mod(z * 5, 1) - 0.5) < 0.08) & (m < 0.78) & (n[..., 1] > 0.5)
        return -0.12 * inner + 0.05 * rim, (rim & chase).astype(float)
    parts = [Part(lambda p: sd_box(p, (0.78, 0.05, 0.6), 0.02), 0),
             Part(lambda p: sd_box(p, (0.22, 0.06, 0.22), 0.02), 1, pos=(-0.22, 0.09, 0.0)),
             Part(lambda p: sd_box(p, (0.14, 0.05, 0.12), 0.02), 1, pos=(0.36, 0.08, -0.14)),
             Part(lambda p: sd_torus(p, 0.12, 0.03), 2, pos=(0.34, 0.07, 0.24)),
             Part(lambda p: sd_box(p, (0.5, 0.025, 0.04)), 3, pos=(0.05, 0.05, 0.5))]
    mats = [Material(c1, c2, metal=0.4, emissive=c2, pattern=pat),
            Material('#20202a', c2, metal=0.8, emissive=c2, pattern=pat_pulse_core(t)),
            Material(c2, '#ffffff', emissive=c2, pattern=pat_bands(10, -2, t, 0.3, axis=0)),
            Material('#d0a040', '#fff0c0', metal=1.0, pattern=lambda p, n: (0.3 * np.sign(np.sin(p[..., 0] * 40)), np.zeros(p.shape[:-1])))]
    return parts, mats, dict(view_pitch=0.8, view_yaw=0.6), None


def topological_logic_wafer(c1, c2, t):
    """Three anyon worldlines braiding across a wafer; the braid scrolls, the crossings flare."""
    def pat(p, n):
        x, z = p[..., 0], p[..., 2]
        topf = (n[..., 1] > 0.5) & (x * x + z * z < 0.55)
        zs = [0.34 * np.sin(x * 4.2 - TAU * t + TAU * k / 3) for k in range(3)]
        strand = np.zeros(x.shape, bool)
        for zk in zs:
            strand |= np.abs(z - zk) < 0.055
        cross = (np.abs(zs[0] - zs[1]) < 0.07) | (np.abs(zs[1] - zs[2]) < 0.07) | (np.abs(zs[0] - zs[2]) < 0.07)
        dies = (np.abs(np.mod(x * 4, 1) - 0.5) > 0.45) | (np.abs(np.mod(z * 4, 1) - 0.5) > 0.45)
        return -0.14 * (dies & topf) + 0.25 * (cross & strand), (strand & topf).astype(float)
    parts = [Part(_wafer_disk, 0, rot=roty(0.3))]
    mats = [Material(c1, c2, metal=0.9, gloss=1.4, emissive=c2, pattern=pat)]
    return parts, mats, dict(view_pitch=0.85, view_yaw=0.5), None


def topological_wafer(c1, c2, t):
    """A polished, inert wafer: its bulk is dark, only the edge carries current - segments chase round the rim."""
    def pat(p, n):
        x, z = p[..., 0], p[..., 2]
        r = np.sqrt(x * x + z * z)
        rim = (r > 0.6) | (n[..., 1] < 0.5) | (z < -0.64)
        chase = np.sin(np.arctan2(z, x) * 4 - TAU * t * 2) > -0.4
        sheen = 0.22 * np.sin(x * 3 + z * 2 - TAU * t) * (n[..., 1] > 0.5)
        return sheen - 0.25 * (~rim), (rim & chase).astype(float)
    parts = [Part(_wafer_disk, 0)]
    mats = [Material(c1, c2, metal=1.0, gloss=1.6, emissive='#ffffff', pattern=pat)]
    return parts, mats, dict(view_pitch=0.75, view_yaw=0.5), None


# =============================================================================================== probes
def entanglement_tuner(c1, c2, t):
    """A tuning wand: two spins in its head always point opposite ways, flipping together; a dial ticks round."""
    a = TAU * t * 2
    head = np.array([0.22, 0.32, 0.0])
    d = np.array([math.cos(a), math.sin(a), 0.0]) * 0.17
    big = 0.5 + 0.5 * math.cos(a)
    parts = [Part(lambda p: sd_capsule(p, (-0.6, -0.75, 0), (0.0, 0.05, 0), 0.1), 0),
             Part(lambda p: sd_torus(_swap_xy(p), 0.34, 0.06), 1, pos=head),
             Part(lambda p: sd_sphere(p, 0.09 + 0.04 * big), 2, pos=head + d),
             Part(lambda p: sd_sphere(p, 0.13 - 0.04 * big), 3, pos=head - d),
             Part(lambda p: sd_capsule(p, -d, d, 0.025), 4, pos=head),
             Part(lambda p: sd_cyl(p, 0.17, 0.06, 0.02), 1, pos=(-0.38, -0.47, 0), rot=rotz(0.9) @ roty(TAU * t))]
    mats = [Material(c1, c2, metal=0.6, emissive=c2, pattern=pat_bands(10, 1, t, 0.1, axis=1)),
            Material('#a0a0c0', '#ffffff', metal=1.0, pattern=lambda p, n: (0.25 * np.sign(np.sin(np.arctan2(p[..., 2], p[..., 0]) * 6)), np.zeros(p.shape[:-1]))),
            Material(c2, '#ffffff', emissive=c2, pattern=pat_all()),
            Material('#60f0ff', '#ffffff', emissive='#60f0ff', pattern=pat_all()),
            Material('#ffffff', '#ffffff', emissive='#ffffff', pattern=pat_bands(30, 3, t, 0.3, axis=0))]
    return parts, mats, dict(view_pitch=0.2, view_yaw=0.15), None


def gene_sampler(c1, c2, t):
    """A syringe: the plunger draws back and the barrel fills with glowing green genome, then empties again."""
    fill = 0.5 - 0.5 * math.cos(TAU * t)
    lv = -0.38 + 0.74 * fill
    R = rotz(-0.785)

    def barrel(p, n):
        y = p[..., 1]
        liquid = y < lv + 0.03 * np.sin(np.arctan2(p[..., 2], p[..., 0]) * 2 + TAU * t)
        ticks = (np.abs(np.mod(y * 8, 1) - 0.5) < 0.08) & (p[..., 0] < -0.05)
        bub = liquid & (_hash(np.floor(y * 9 - t * 9 * 1), np.floor(np.arctan2(p[..., 2], p[..., 0]) * 2)) > 0.85)
        return -0.2 * ticks + 0.3 * bub, liquid.astype(float)
    parts = [Part(lambda p: sd_cyl(p, 0.2, 0.4, 0.04), 0, rot=R),
             Part(lambda p: sd_cyl(p - np.array([0, -0.45, 0]), 0.09, 0.07, 0.02), 1, rot=R),
             Part(lambda p: sd_capsule(p, (0, -0.5, 0), (0, -0.95, 0), 0.025), 2, rot=R),
             Part(lambda p: sd_capsule(p, (0, lv, 0), (0, lv + 0.55, 0), 0.05), 1, rot=R),
             Part(lambda p: sd_cyl(p - np.array([0, lv + 0.58, 0]), 0.16, 0.035, 0.01), 3, rot=R),
             Part(lambda p: sd_box(p - np.array([0, 0.4, 0]), (0.32, 0.035, 0.1), 0.02), 1, rot=R)]
    mats = [Material('#405848', c2, metal=0.3, gloss=1.8, emissive=c2, pattern=barrel),
            Material('#b0b8c0', '#ffffff', metal=0.9), Material('#e0e8f0', '#ffffff', metal=1.0),
            Material(c1, c2, metal=0.5)]
    return parts, mats, dict(view_pitch=0.3, view_yaw=0.3, zoom=0.95), None


def _probe_parts(mb, md, s, pos):
    """Body (hex hull), dish and antenna of a Von Neumann probe at scale s."""
    pos = np.asarray(pos, float)
    dish = lambda p: np.maximum(np.abs(sd_sphere(p - np.array([0, 0.42, 0]), 0.42)) - 0.03, p[..., 1] - 0.2)
    return [Part(lambda p: sd_hexprism(p / s, 0.26, 0.18) * s, mb, pos=pos),
            Part(lambda p: dish(p / s) * s, md, pos=pos + np.array([0, 0.17, 0]) * s, rot=rotz(-0.35)),
            Part(lambda p: sd_capsule(p / s, (0, 0, 0), (0.08, 0.32, 0), 0.03) * s, md, pos=pos + np.array([0, 0.2, 0]) * s)]


def von_neumann_probe(c1, c2, t):
    """A probe welding a smaller copy of itself; the finished copy flies off to the right and a new one begins."""
    main = np.array([-0.3, 0.12, 0.0])

    def hull(p, n):
        port = (np.abs(p[..., 1]) < 0.06) & (np.sin(np.arctan2(p[..., 2], p[..., 0]) * 3 + TAU * t) > 0.6)
        return 0.15 * np.sign(np.sin(np.arctan2(p[..., 2], p[..., 0]) * 3)), port.astype(float)
    mats = [Material(c1, c2, metal=0.8, emissive=c2, pattern=hull), Material(c2, '#ffffff', metal=0.9),
            Material('#909aa8', '#ffffff', metal=1.0)]
    parts = _probe_parts(0, 1, 1.0, main)
    build = t < 0.6
    if build:
        s, q = 0.55 * _smooth(t / 0.6), np.array([0.38, -0.42, 0.0])
    else:
        u = (t - 0.6) / 0.4
        s, q = 0.55, np.array([0.38 + 1.7 * u * u, -0.42 - 0.25 * u, 0.0])
    if s > 0.03:
        parts += _probe_parts(0, 1, s, q)
    # the welding arm stays out (it used to pop in on the loop seam); it always points at the build spot
    qb = np.array([0.38, -0.42, 0.0])
    parts.append(Part(_segs([main + np.array([0.12, -0.12, 0]), np.array([0.2, -0.05, 0])],
                            [np.array([0.2, -0.05, 0]), qb + np.array([-0.05, 0.12, 0])], 0.035), 2))
    view = dict(view_pitch=0.35, view_yaw=0.55)

    def ov(img):
        if build and int(t * 40) % 3 != 0:
            x, y, _ = _proj(q + np.array([-0.05, 0.12, 0]), view)
            _px(img, x, y, WHITE, 1.0)
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                _px(img, x + dx, y + dy, H(c2), 0.8)
    return parts, mats, view, ov


# =============================================================================================== real anchors / keys
def axiom_key(c1, c2, t):
    """A pale-gold key; past its bit a tear in the laws opens and closes, pouring light."""
    parts = [Part(lambda p: sd_torus(_swap_xy(p), 0.25, 0.075), 0, pos=(-0.48, 0.48, 0)),
             Part(lambda p: sd_octa(p, 0.17), 1, pos=(-0.48, 0.48, 0), rot=roty(TAU * t)),
             Part(lambda p: sd_capsule(p, (-0.3, 0.3, 0), (0.45, -0.45, 0), 0.075), 0),
             Part(lambda p: sd_torus(_swap_xy(p), 0.09, 0.04), 0, pos=(-0.22, 0.22, 0), rot=rotz(0.785)),
             Part(lambda p: sd_box(p, (0.07, 0.2, 0.06), 0.02), 0, pos=(0.36, -0.2, 0), rot=rotz(0.785)),
             Part(lambda p: sd_box(p, (0.06, 0.13, 0.06), 0.02), 0, pos=(0.2, -0.08, 0), rot=rotz(0.785))]
    mats = [Material(c1, c2, metal=1.0, emissive=c2, pattern=pat_bands(8, 1, t, 0.1, axis=0)),
            Material(c2, '#ffffff', emissive=c2, pattern=pat_pulse_core(t))]
    o = math.sin(math.pi * t) ** 2

    def ov(img):
        cx, cy = 25.5, 24.0
        L = 2 + int(round(6 * o))
        for k in range(-L, L + 1):
            jx = cx + (1 if (k * 7) % 3 == 0 else (-1 if (k * 5) % 4 == 0 else 0))
            w = 1 - abs(k) / (L + 1)
            _px(img, jx, cy + k, WHITE, w)
            if o > 0.25:
                _px(img, jx - 1, cy + k, H(c2), 0.7 * w * o)
                _px(img, jx + 1, cy + k, H(c2), 0.7 * w * o)
        if o > 0.5:
            for k in range(3):
                a = TAU * (k / 3 + t)
                _px(img, cx + math.cos(a) * (3 + 3 * o), cy + math.sin(a) * (3 + 4 * o), H(c2), o - 0.3)
    return parts, mats, dict(view_pitch=0.2, view_yaw=0.2), ov


def reality_anchor(c1, c2, t):
    """A ship's anchor sunk in a locked grid tile; around it the rest of reality glitches and is rewritten."""
    crown = lambda p: np.maximum(sd_torus(_swap_xy(p - np.array([0, -0.18, 0])), 0.45, 0.075), p[..., 1] + 0.18)
    parts = [Part(lambda p: sd_capsule(p, (0, -0.6, 0), (0, 0.55, 0), 0.08), 0),
             Part(lambda p: sd_torus(_swap_xy(p), 0.14, 0.05), 0, pos=(0, 0.68, 0)),
             Part(lambda p: sd_box(p, (0.34, 0.055, 0.055), 0.02), 0, pos=(0, 0.38, 0)),
             Part(crown, 0),
             Part(lambda p: sd_octa(p / np.array([1, 1.4, 0.6]), 0.14) * 0.6, 0, pos=(-0.45, -0.12, 0), rot=rotz(0.5)),
             Part(lambda p: sd_octa(p / np.array([1, 1.4, 0.6]), 0.14) * 0.6, 0, pos=(0.45, -0.12, 0), rot=rotz(-0.5)),
             Part(lambda p: sd_box(p, (0.6, 0.04, 0.42), 0.02), 1, pos=(0, -0.72, 0))]

    def code(p, n):
        y = p[..., 1]
        e = (np.abs(p[..., 0]) < 0.04) & (np.mod(y * 6 - t * 2, 1) < 0.35)
        return np.zeros(y.shape), e.astype(float)

    def grid(p, n):
        g = (np.abs(np.mod(p[..., 0] * 5, 1) - 0.5) > 0.4) | (np.abs(np.mod(p[..., 2] * 5, 1) - 0.5) > 0.4)
        return np.zeros(g.shape), (g & (n[..., 1] > 0.5)).astype(float)
    mats = [Material(c1, c2, metal=0.9, emissive=c2, pattern=code), Material('#182028', c2, metal=0.6, emissive=c2, pattern=grid)]

    def ov(img):
        k = int(t * 20)
        rng = np.random.default_rng(1000 + k)
        for i in range(9):
            x, y = int(rng.integers(0, 31)), int(rng.integers(0, 31))
            col = H(c2) if i % 3 else np.array([1.0, 0.3, 0.8])
            for dx in range(int(rng.integers(1, 4))):
                _px_empty(img, x + dx, y, col, 0.75)
    return parts, mats, dict(view_pitch=0.25, view_yaw=0.35), ov


# =============================================================================================== seeds
def kugelblitz_story(c1, c2, t):
    """Beams of light converge on one point, it brightens, collapses into a black hole ringed by light, then evaporates."""
    parts, mats = [], [Material(c1, '#ffffff', emissive=c1, pattern=pat_pulse_core(t)),
                       Material(c2, '#ffffff', emissive=c1, pattern=pat_bands(8, -3, t, 0.45, axis=1)),
                       _dark(Material('#000000', '#000000', metal=0.0, gloss=0.0, keep_dark=True)),
                       Material(c1, '#ffffff', emissive=c1, pattern=pat_bands(12, 2, t, 0.35, axis=0))]
    dirs = [(1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0.7, 0.7, 0.3), (-0.7, -0.7, -0.3), (0.7, -0.7, 0.3), (-0.7, 0.7, -0.3)]
    phase = ''
    if t < 0.5:
        u = t / 0.5
        # the beams fly in from outside the picture, thickening as they come, and the point of light grows from
        # nothing - so the loop starts from the same empty sky the evaporation ends in
        rin = 0.12 + 1.5 * (1 - u) ** 1.5
        for d in dirs:
            dv = np.asarray(d, float) / np.linalg.norm(d)
            parts.append(Part(lambda p, a=dv * rin, b=dv * (rin + 0.38): sd_capsule(p, a, b, (0.04 + 0.02 * u) * min(1.0, u / 0.2)), 1))
        if u > 0.05:
            parts.append(Part(lambda p, r=0.25 * u * u: sd_sphere(p, r), 0))
    elif t < 0.9:
        r = 0.26 - 0.08 * _smooth((t - 0.5) / 0.08)
        parts.append(Part(lambda p: sd_sphere(p, r), 2))
        parts.append(Part(lambda p: sd_torus(p, r + 0.2, 0.035), 3, rot=rotx(0.35) @ roty(TAU * t * 2)))
        phase = 'hole'
    else:
        u = (t - 0.9) / 0.1
        r = 0.18 * (1 - u)
        if r > 0.02:
            parts.append(Part(lambda p: sd_sphere(p, r), 2))
            parts.append(Part(lambda p: sd_torus(p, r + 0.2 * (1 - u), 0.03), 3, rot=rotx(0.35)))
        phase = 'evap'

    def ov(img):
        if t < 0.08 or t > 0.48 and t < 0.56:
            k = 1 - abs(t - 0.52) / 0.04 if t > 0.4 else 0
            n = 32
            for dy in range(-4, 5):
                for dx in range(-4, 5):
                    _px(img, 15.5 + dx, 15.5 + dy, WHITE, max(0, k * (1 - math.hypot(dx, dy) / 5)))
        if phase == 'hole':
            anim.ov_infall(img, t, 7, H(c1), 6)
        if phase == 'evap':
            u = (t - 0.9) / 0.1
            for k in range(12):
                a = TAU * k / 12
                for r in (4 + u * 10, 5 + u * 10):
                    _px(img, 15.5 + math.cos(a) * r, 15.5 + math.sin(a) * r, WHITE, 1 - u)
    return parts, mats, dict(view_pitch=0.3, view_yaw=0.5 + TAU * t), ov


def primordial_harvest(c1, c2, t):
    """A ribbed seed with a swaying sprout, circled by a kernel of every kind of crop."""
    sway = 0.07 * math.sin(TAU * t)
    tip = np.array([0.06 + sway, 0.62, 0.0])

    def ribs(p, n):
        a = np.arctan2(p[..., 2], p[..., 0])
        rib = np.sin(a * 8) > 0.6
        sp = (_hash(np.floor(p[..., 0] * 9), np.floor(p[..., 1] * 9), np.floor(p[..., 2] * 9)) > 0.86) & (np.sin(TAU * t + p[..., 1] * 4) > 0)
        return -0.2 * rib, sp.astype(float)
    parts = [Part(_ell((0.3, 0.42, 0.3)), 0, pos=(0, -0.18, 0), rot=rotz(0.15)),
             Part(_segs([(0, 0.18, 0), (0.0, 0.4, 0)], [(0.0, 0.4, 0), tip], 0.045), 1),
             Part(_ell((0.2, 0.05, 0.09)), 1, pos=tip + np.array([0.15, -0.03, 0]), rot=rotz(0.5 + sway * 3)),
             Part(_ell((0.2, 0.05, 0.09)), 1, pos=tip + np.array([-0.15, -0.06, 0]), rot=rotz(-0.5 + sway * 3))]
    mats = [Material(c1, c2, metal=0.3, emissive=c2, pattern=ribs), Material(c2, '#ffffff', metal=0.2)]
    for k, col in enumerate(('#f0c040', '#ff7a20', '#c02050', '#6070ff')):
        a = TAU * (t + k / 4)
        mats.append(Material(col, '#ffffff', metal=0.4, emissive=col, pattern=pat_pulse_core(t + k / 4)))
        parts.append(Part(lambda p: sd_sphere(p, 0.1), len(mats) - 1, pos=(0.68 * math.cos(a), -0.15 + 0.22 * math.sin(a), 0.68 * math.sin(a))))
    return parts, mats, dict(view_pitch=0.3, view_yaw=0.5), None


def spark_of_life(c1, c2, t):
    """A primordial droplet with lightning crackling inside it; a chain of three amino beads circles it."""
    drop = lambda p: _smin(sd_sphere(p - np.array([0, -0.15, 0]), 0.42), sd_capsule(p, (0, -0.05, 0), (0, 0.48, 0), 0.05), 0.3)

    def veins(p, n):
        v = np.abs(np.sin(p[..., 0] * 9 + 2 * np.sin(p[..., 1] * 7)) * np.sin(p[..., 2] * 8 + p[..., 1] * 5)) < 0.12
        fl = (int(t * 15) % 3) != 1          # 15 steps = 5 whole flicker cycles per loop
        return 0.15 * np.sin(p[..., 1] * 12), (v & fl).astype(float)
    a = TAU * t
    beads = [np.array([0.66 * math.cos(a - k * 0.35), 0.25 * math.sin(a - k * 0.35) - 0.1, 0.66 * math.sin(a - k * 0.35)]) for k in range(3)]
    parts = [Part(drop, 0), Part(lambda p: sd_sphere(p, 0.08), 1, pos=beads[0]), Part(lambda p: sd_sphere(p, 0.08), 1, pos=beads[1]),
             Part(lambda p: sd_sphere(p, 0.08), 1, pos=beads[2]), Part(_segs(beads[:2], beads[1:], 0.025), 2)]
    mats = [Material(c1, c2, metal=0.5, gloss=1.5, emissive=c2, pattern=veins),
            Material(c2, '#ffffff', emissive=c2, pattern=pat_pulse_core(t)), Material('#c0c0a0', '#ffffff', metal=0.6)]

    def ov(img):
        k = int(t * 8)
        if (t * 8) % 1 > 0.55:
            return
        rng = np.random.default_rng(77 + k)
        x, y = 15.5 + rng.uniform(-2, 2), 9.0
        for _ in range(6):
            nx, ny = x + rng.uniform(-3, 3), y + rng.uniform(2, 3)
            _line(img, x, y, nx, ny, WHITE if _ % 2 == 0 else H(c2), 1.0)
            x, y = nx, ny
    return parts, mats, dict(view_pitch=0.25, view_yaw=0.5), ov


# =============================================================================================== shards
def arrow_of_time_shard(c1, c2, t):
    """An arrowhead pointing backwards; its light flows tip-to-tail and scattered dust un-scatters into it."""
    R = rotz(2.356)
    head = lambda p: np.maximum(sd_octa((p - np.array([0, 0.3, 0])) / np.array([0.42, 0.6, 0.14]), 1.0) * 0.14, -(p[..., 1] - 0.12))
    fl = lambda p: sd_octa((p - np.array([0, -0.6, 0])) / np.array([0.2, 0.16, 0.06]), 1.0) * 0.06
    parts = [Part(head, 0, rot=R), Part(lambda p: sd_hexprism(p - np.array([0, -0.18, 0]), 0.07, 0.42), 1, rot=R), Part(fl, 0, rot=R)]
    mats = [Material(c1, c2, metal=0.8, emissive=c2, pattern=pat_bands(10, 2, t, 0.22, axis=1)),
            Material('#8a7a5a', c2, metal=0.9, emissive=c2, pattern=pat_bands(14, 2, t, 0.18, axis=1))]

    def ov(img):
        for i in range(9):
            life = (t + i / 9) % 1.0
            a = TAU * _hash(i, 3)
            sx, sy = 15.5 + math.cos(a) * 15, 15.5 + math.sin(a) * 15
            k = life * life
            _px_empty(img, sx + (15.5 - sx) * k, sy + (15.5 - sy) * k, H(c2), math.sin(life * math.pi))
    return parts, mats, dict(view_pitch=0.2, view_yaw=0.3), ov


_TET = [np.array(v, float) / math.sqrt(3) for v in ((1, 1, 1), (1, -1, -1), (-1, 1, -1), (-1, -1, 1))]


def _rot_to_y(v):
    v = np.asarray(v, float) / np.linalg.norm(v)
    y = np.array([0, 1.0, 0])
    ax = np.cross(v, y)
    s, c = np.linalg.norm(ax), float(v @ y)
    if s < 1e-9:
        return np.eye(3)
    ax /= s
    K = np.array([[0, -ax[2], ax[1]], [ax[2], 0, -ax[0]], [-ax[1], ax[0], 0]])
    return np.eye(3) + K * s + K @ K * (1 - c)


def axiom_fragment(c1, c2, t):
    """A white tetrahedron - the simplest solid - with violet light flowing down its edges from one source vertex."""
    s = 0.28
    def tet(p):
        return np.max(np.stack([-(p @ v) for v in _TET], -1), -1) - s

    def edges(p, n):
        ds = np.sort(np.stack([-(p @ v) for v in _TET], -1), -1)
        e = ds[..., 3] - ds[..., 2] < 0.05
        top = 3 * s * _TET[0]
        dist = np.sqrt(((p - top) ** 2).sum(-1))
        flow = np.sin(dist * 10 - TAU * t * 2) > -0.2
        return np.zeros(e.shape), (e & flow).astype(float)
    # orient the source vertex (opposite face 0) upward
    Rr = _rot_to_y(_TET[0])
    rot = roty(TAU * t / 3) @ Rr
    top = rot @ (3 * s * _TET[0])
    parts = [Part(tet, 0, rot=rot), Part(lambda p: sd_sphere(p, 0.08), 1, pos=top)]
    mats = [Material(c1, c2, metal=0.6, gloss=1.3, emissive=c2, pattern=edges), Material('#ffffff', '#ffffff', emissive='#ffffff', pattern=pat_all())]

    def ov(img):
        x, y, _ = _proj(top, dict(view_pitch=0.3, view_yaw=0.5))
        if (t * 4) % 1 < 0.5:
            sparkle(img, int(round((x - 0.5) / 2)), int(round((y - 0.5) / 2)), H(c2))
    return parts, mats, dict(view_pitch=0.3, view_yaw=0.5, zoom=1.1), ov


_OCT_V = [np.array(v, float) for v in ((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1))]
_OCT_E = [(i, j) for i in range(6) for j in range(i + 1, 6) if abs(_OCT_V[i] @ _OCT_V[j]) < 0.5]


def cardinal_shard(c1, c2, t):
    """Octahedral frames nested inside each other without end; each grows into the next, counting up forever."""
    parts, mats = [], [Material(c2, '#ffffff', emissive=c2, pattern=pat_bands(10, 2, t, 0.25, axis=1)),
                       Material(c1, c2, metal=0.9, emissive=c2, pattern=pat_pulse_core(t))]
    for k in range(5):
        sc = 0.95 * 0.5 ** (k + 1 - t)
        r = 0.05 * min(1.0, sc / 0.4) * min(1.0, max(0.0, (0.95 - sc) / 0.25))
        if r < 0.006:
            continue
        ang = math.log2(sc) * 0.785
        V = [v * sc * np.array([0.75, 1.0, 0.75]) for v in _OCT_V]
        R = roty(ang)
        parts.append(Part(_segs([V[i] for i, j in _OCT_E], [V[j] for i, j in _OCT_E], r), 0, rot=R))
    parts.append(Part(lambda p: sd_octa(p / np.array([0.75, 1, 0.75]), 0.07) * 0.75, 1))
    return parts, mats, dict(view_pitch=0.3, view_yaw=0.6, zoom=1.05), None


def cosmic_record_fragment(c1, c2, t):
    """A broken wedge of a black record; magenta sound rings play outward along its grooves from the label."""
    def wedge(p):
        x, z = p[..., 0], p[..., 2]
        r = np.sqrt(x * x + z * z)
        a = np.arctan2(z, x)
        outer = r - (0.88 + 0.04 * np.sin(a * 23))
        side = (np.abs(a) - 0.5 - 0.06 * np.sin(r * 17)) * np.maximum(r, 0.2)
        return np.maximum(np.maximum(np.maximum(outer, side), np.abs(p[..., 1]) - 0.035), 0.06 - r)

    def grooves(p, n):
        r = np.sqrt(p[..., 0] ** 2 + p[..., 2] ** 2)
        topf = n[..., 1] > 0.4
        label = (r < 0.3) & topf
        g = np.sin(r * 50) > 0
        wave = (np.sin(r * 22 - TAU * t * 3) > 0.82) & (r > 0.3) & topf
        return -0.15 * g + 0.4 * label, (wave | (label & (np.sin(r * 30) > -0.5))).astype(float)
    parts = [Part(wedge, 0, pos=(-0.45, 0, 0.0))]
    mats = [Material(c1, c2, metal=0.9, gloss=1.7, emissive=c2, pattern=grooves)]
    return parts, mats, dict(view_pitch=0.8, view_yaw=-0.5, zoom=1.55), None


def horizon_shard(c1, c2, t):
    """A curved sliver of black event horizon with a burning orange rim; sparks spiral in and stay."""
    msk = lambda p: sd_octa(p / np.array([0.45, 0.82, 2.0]), 1.0) * 0.45
    shell = lambda p: np.maximum(np.abs(sd_sphere(p - np.array([0, 0, 1.0]), 1.0)) - 0.05, msk(p))

    def rim(p, n):
        m = msk(p)
        a = np.arctan2(p[..., 1], p[..., 0])
        e = (m > -0.07) & (np.sin(a * 4 + TAU * t * 2) > -0.4)
        mem = (_hash(np.floor(p[..., 0] * 8), np.floor(p[..., 1] * 8)) > 0.9) & (m < -0.12)
        return np.zeros(m.shape), (e | mem).astype(float)
    m0 = _dark(Material('#000000', c2, metal=0.0, gloss=0.6, emissive=c2, pattern=rim, keep_dark=True))
    parts = [Part(shell, 0, rot=rotz(0.45) @ roty(0.25 * math.sin(TAU * t)))]

    def ov(img):
        anim.ov_infall(img, t, 31, H(c2), 7)
    return parts, [m0], dict(view_pitch=0.15, view_yaw=0.35), ov


def kaluza_klein_shard(c1, c2, t):
    """A long blue crystal with a tiny curled-up dimension - a ring - sliding along it, a bead running round the ring."""
    R = rotz(0.4)
    yk = 0.42 * math.sin(TAU * t)
    b = TAU * t * 3
    bead = R @ np.array([0.27 * math.cos(b), yk, 0.27 * math.sin(b)])

    def helix(p, n):
        a = np.arctan2(p[..., 2], p[..., 0])
        return np.zeros(a.shape), (np.sin(a + p[..., 1] * 9 - TAU * t * 2) > 0.8).astype(float)
    parts = [Part(lambda p: sd_gem(p, 0.2, 0.92, 0.25), 0, rot=R @ roty(0.3)),
             Part(lambda p: sd_torus(p - np.array([0, yk, 0]), 0.27, 0.035), 1, rot=R),
             Part(lambda p: sd_sphere(p, 0.07), 2, pos=bead)]
    mats = [Material(c1, c2, metal=0.85, emissive=c2, pattern=helix),
            Material('#c0e0ff', '#ffffff', emissive=c2, pattern=pat_bands(16, 3, t, 0.25, axis=0)),
            Material('#ffffff', '#ffffff', emissive='#ffffff', pattern=pat_all())]
    return parts, mats, dict(view_pitch=0.3, view_yaw=0.5), None


def kerr_metric_shard(c1, c2, t):
    """A dark spindle crystal pierced through an oblate ergosphere whose orange swirl is dragged round with it."""
    def swirl(p, n):
        r = np.sqrt(p[..., 0] ** 2 + p[..., 2] ** 2)
        a = np.arctan2(p[..., 2], p[..., 0])
        return np.zeros(r.shape), (np.sin(a * 3 + r * 9 - TAU * t * 2) > 0.1).astype(float)
    parts = [Part(lambda p: sd_gem(p, 0.18, 0.85, 0.3), 0, rot=rotz(0.2)),
             Part(lambda p: sd_torus(p / np.array([1, 0.45, 1]), 0.48, 0.16) * 0.45, 1, rot=rotz(0.2) @ rotx(0.3))]
    m0 = Material('#20202a', c2, metal=0.95, emissive=c2, pattern=pat_spots(t, 0.93, 3))
    mats = [m0, Material('#402010', c2, metal=0.6, emissive=c2, pattern=swirl)]
    return parts, mats, dict(view_pitch=0.35, view_yaw=0.5), None


def magnetar_shard(c1, c2, t):
    """A violet shard caged by three dipole field loops; pulses race round the loops and the field crackles."""
    shard = lambda p: sd_octa(p / np.array([0.38, 0.75, 0.3]), 1.0) * 0.3
    parts = [Part(shard, 0, rot=roty(0.4))]
    for k in range(3):
        a = k * TAU / 3 + 0.3
        parts.append(Part(lambda p: sd_torus(_swap_xy(p - np.array([0.36, 0, 0])), 0.36, 0.032), 1, rot=roty(a)))
    mats = [Material(c1, c2, metal=0.8, emissive=c2, pattern=pat_spots(t, 0.85, 5)),
            Material(c2, '#ffffff', emissive=c2, pattern=pat_bands(9, 2, t, 0.3, axis=1))]

    def ov(img):
        k = int(t * 10)
        rng = np.random.default_rng(500 + k)
        if (t * 10) % 1 < 0.5:
            x, y = rng.uniform(5, 26), rng.uniform(5, 26)
            for _ in range(3):
                nx, ny = x + rng.uniform(-3, 3), y + rng.uniform(-3, 3)
                _line(img, x, y, nx, ny, WHITE, 0.9)
                x, y = nx, ny
    return parts, mats, dict(view_pitch=0.3, view_yaw=0.5), ov


def ordinal_shard(c1, c2, t):
    """A climbing row of shards, each larger than the last; gold light steps up them one by one to the crowned omega."""
    parts, mats = [], []
    n = 5
    for k in range(n):
        f = k / (n - 1)
        pos = (-0.68 + 1.3 * f, -0.58 + 0.95 * f, 0)
        sz = 0.22 + 0.26 * f
        lit = ((t * n - k) % n) < 1.3
        mats.append(Material(mix(H(c1), H(c2), 0.25), c2, metal=0.85, emissive=c2,
                             pattern=pat_all() if lit else pat_bands(8, 1, t, 0.12, axis=1)))
        parts.append(Part(lambda p, s=sz: sd_octa(p / np.array([0.5, 1.0, 0.4]), s) * 0.4, k, pos=pos, rot=roty(0.5 + k * 0.3)))
    mats.append(Material(c2, '#ffffff', emissive=c2, pattern=pat_bands(10, 2, t, 0.3, axis=0)))
    parts.append(Part(lambda p: sd_torus(p, 0.3, 0.04), n, pos=(0.62, 0.37, 0), rot=rotx(0.3) @ roty(TAU * t)))
    return parts, mats, dict(view_pitch=0.25, view_yaw=0.2), None


def pentaquark_shard(c1, c2, t):
    """A small pink crystal holding five coloured quarks in a jostling pentagon, bound by a gluon ring."""
    cols = ('#ff3040', '#40e060', '#4070ff', '#ffd040', '#40e0ff')
    parts = [Part(lambda p: sd_gem(p, 0.22, 0.5, 0.3), 0, rot=rotx(-0.5) @ roty(0.3)),
             Part(lambda p: sd_torus(p, 0.46, 0.025), 1, rot=rotx(0.5))]
    mats = [Material(c1, c2, metal=0.8, emissive=c2, pattern=pat_spots(t, 0.8, 2)),
            Material(c2, '#ffffff', emissive=c2, pattern=pat_bands(12, 3, t, 0.3, axis=0))]
    R = rotx(0.5)
    for k, col in enumerate(cols):
        a = TAU * (k / 5 + t)
        rr = 0.46 + 0.06 * math.sin(TAU * (2 * t + k * 0.37))
        pos = R @ np.array([rr * math.cos(a), 0.05 * math.sin(TAU * (3 * t + k / 5)), rr * math.sin(a)])
        mats.append(Material(col, '#ffffff', emissive=col, pattern=pat_all()))
        parts.append(Part(lambda p: sd_sphere(p, 0.125), len(mats) - 1, pos=pos))
    return parts, mats, dict(view_pitch=0.75, view_yaw=0.5), None


_PRIME = np.array([0, 0, 1, 1, 0, 1, 0, 1, 0, 0, 0, 1, 0, 1, 0, 0], float)


def prime_shard(c1, c2, t):
    """A square obelisk ruled in rows: only the prime-numbered rows glow; a sieve line climbs and strikes out the rest."""
    def ob(p):
        body = sd_box(p, (0.17, 0.55, 0.17))
        cap = np.maximum(sd_octa((p - np.array([0, 0.55, 0])) / np.array([1, 1.6, 1]), 0.24) * 0.62, 0.55 - p[..., 1])
        return np.minimum(body, cap)
    scan = int(t * 12)

    def rows(p, n):
        row = np.clip(np.floor((p[..., 1] + 0.55) * 11.0), 0, 15).astype(int)
        pr = _PRIME[row] > 0
        gap = np.abs(np.mod((p[..., 1] + 0.55) * 11.0, 1) - 0.5) > 0.38
        hit = (row == scan)
        return -0.25 * (~pr) - 0.2 * gap, ((pr & ~gap) | (hit & ~gap)).astype(float)
    parts = [Part(ob, 0, rot=rotz(-0.25) @ roty(0.6)),
             Part(lambda p: sd_box(p, (0.07, 0.07, 0.07), 0.01), 1, pos=(0.42, -0.6, 0.1), rot=roty(0.7)),
             Part(lambda p: sd_box(p, (0.05, 0.05, 0.05), 0.01), 1, pos=(-0.35, -0.7, -0.1), rot=roty(0.3))]
    mats = [Material(c1, c2, metal=0.8, emissive=c2, pattern=rows), Material('#40485a', '#8090a0', metal=0.6)]

    def ov(img):
        for i in range(3):
            life = (t * 2 + i / 3) % 1.0
            _px_empty(img, 13 + i * 3, 27 + life * 6, H(c1) * 1.6, 1 - life)
    return parts, mats, dict(view_pitch=0.2, view_yaw=0.4), ov


def proof_fragment(c1, c2, t):
    """A torn page on which a proof writes itself line by line, ends in a blinking QED box, then is wiped."""
    def page(p):
        edge = 0.42 + 0.06 * np.sin(p[..., 1] * 23) + 0.04 * np.sin(p[..., 1] * 41)
        return np.maximum(sd_box(p, (0.55, 0.7, 0.025), 0.01), p[..., 0] - edge)
    wh = min(1.0, t / 0.75)
    eh = max(0.0, (t - 0.8) / 0.2)
    rows = 6

    def text(p, n):
        x, y = p[..., 0], p[..., 1]
        i = np.round((0.5 - y) / 0.19)
        yi = 0.5 - i * 0.19
        line = (np.abs(y - yi) < 0.035) & (i >= 0) & (i < rows) & (x > -0.45) & (x < 0.38 - 0.25 * (i == rows - 1))
        word = _hash(np.floor(x * 9 + 0.5), i) > 0.22
        s = (i + (x + 0.45) / 0.85) / rows
        vis = (s < wh) & (s > eh)
        qed = (np.abs(x - 0.25) < 0.06) & (np.abs(y - (0.5 - (rows - 1) * 0.19)) < 0.06) & (wh >= 1.0) & (eh <= 0) & (int(t * 16) % 2 == 0)
        front = p[..., 2] < -0.01
        return -0.15 * (line & word & vis & front), (((line & word & vis) | qed) & front).astype(float)
    parts = [Part(page, 0, rot=rotz(0.12) @ roty(0.2))]
    mats = [Material(c1, '#ffffff', metal=0.1, gloss=0.4, emissive=c2, pattern=text)]
    return parts, mats, dict(view_pitch=0.1, view_yaw=0.15), None


def silicon_life_spore(c1, c2, t):
    """A core studded with six silicate spikes that grow and retract in turn, like breathing."""
    parts = [Part(lambda p: sd_sphere(p, 0.34), 0)]
    dirs = [(1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1)]
    for k, d in enumerate(dirs):
        ln = 0.42 + 0.14 * math.sin(TAU * t + k * TAU / 6)
        dv = np.asarray(d, float)
        Rk = _rot_to_y(dv).T
        parts.append(Part(lambda p, ln=ln: sd_octa((p - np.array([0, 0.3, 0])) / np.array([0.15, ln, 0.15]), 1.0) * 0.15, 1, rot=rotx(0.5) @ roty(0.6) @ Rk))
    mats = [Material(mix(H(c1), H(c2), 0.35), c2, metal=0.5, emissive=c2, pattern=pat_combine(pat_noise_shade(12, 0.3, 2), pat_pulse_core(t))),
            Material(mix(H(c2), H(c1), 0.3), '#ffffff', metal=0.9, emissive='#ffffff', pattern=pat_bands(14, 2, t, 0.15, axis=1))]
    return parts, mats, dict(view_pitch=0.3, view_yaw=0.5), None


def superpartner_shard(c1, c2, t):
    """Two mirror-twin shards circling each other: one solid violet, its superpartner a dark ghost seen only by its edges."""
    a = TAU * t
    p1 = np.array([0.36 * math.cos(a), 0.05 * math.sin(2 * a), 0.36 * math.sin(a)])
    shard = lambda p: sd_octa(p / np.array([0.33, 0.7, 0.26]), 1.0) * 0.26
    view = dict(view_pitch=0.3, view_yaw=0.5)
    cam = rotx(-view['view_pitch']) @ roty(view['view_yaw'])
    vdir = np.array([0, 0, 1.0]) @ cam

    def ghost(p, n):
        f = np.abs(n @ vdir)
        return np.zeros(f.shape), (f < 0.45).astype(float)
    parts = [Part(shard, 0, pos=p1, rot=rotz(0.35)), Part(shard, 1, pos=-p1, rot=rotz(-0.35))]
    mats = [Material(c2, '#ffffff', metal=0.85, emissive='#ffffff', pattern=pat_spots(t, 0.93, 1)),
            _dark(Material('#000000', c2, metal=0.3, emissive=c2, pattern=ghost, keep_dark=True))]
    return parts, mats, view, None


def void_lattice_shard(c1, c2, t):
    """A shard that is only scaffolding: a cubic lattice of bars carved to a crystal outline, nodes pulsing violet."""
    cell = 0.26
    def lat(p):
        q = np.mod(p + cell / 2, cell) - cell / 2
        bx = np.sqrt(q[..., 1] ** 2 + q[..., 2] ** 2)
        by = np.sqrt(q[..., 0] ** 2 + q[..., 2] ** 2)
        bz = np.sqrt(q[..., 0] ** 2 + q[..., 1] ** 2)
        bars = np.minimum(np.minimum(bx, by), bz) - 0.045
        hull = sd_octa(p / np.array([0.6, 1.0, 0.6]), 0.95) * 0.6
        return np.maximum(bars, hull)

    def nodes(p, n):
        q = np.mod(p + cell / 2, cell) - cell / 2
        nd = (np.abs(q) < 0.07).all(-1)
        return np.zeros(nd.shape), (nd & (np.sin(p[..., 1] * 6 - TAU * t) > -0.2)).astype(float)
    parts = [Part(lat, 0, rot=rotz(0.35) @ roty(0.5) @ rotx(0.2))]
    mats = [Material(c1, c2, metal=0.8, emissive=c2, pattern=nodes)]
    return parts, mats, dict(view_pitch=0.3, view_yaw=0.5), None


# =============================================================================================== shifters
def brane_shifter(c1, c2, t):
    """A wand projecting two rippling membranes; a bead of you slips sideways out from between them and back."""
    def mem(dy, ph):
        def f(p):
            q = p.copy()
            q[..., 1] = q[..., 1] - dy - 0.04 * np.sin(p[..., 0] * 7 + p[..., 2] * 4 - TAU * t * 2 + ph)
            return sd_box(q, (0.42, 0.02, 0.32)) * 0.6
        return f
    head = np.array([0.18, 0.32, 0.0])
    bx = 0.5 * math.sin(TAU * t)
    parts = [Part(lambda p: sd_capsule(p, (-0.6, -0.8, 0), (-0.1, -0.12, 0), 0.09), 0),
             Part(lambda p: sd_cyl(p, 0.13, 0.1, 0.03), 1, pos=(-0.05, -0.05, 0), rot=rotz(-0.63)),
             Part(mem(0.0, 0.0), 2, pos=head + np.array([0, -0.12, 0]), rot=rotx(0.35)),
             Part(mem(0.0, 1.5), 2, pos=head + np.array([0, 0.16, 0]), rot=rotx(0.35)),
             Part(lambda p: sd_sphere(p, 0.08), 3, pos=head + np.array([bx, 0.02, 0]))]
    mats = [Material(c1, c2, metal=0.7, emissive=c2, pattern=pat_bands(10, 1, t, 0.1, axis=1)),
            Material('#9090b0', '#ffffff', metal=1.0),
            Material(c2, '#ffffff', metal=0.5, emissive=c2, pattern=lambda p, n: (np.zeros(p.shape[:-1]), (np.sin(np.sqrt(p[..., 0] ** 2 + p[..., 2] ** 2) * 18 - TAU * t * 2) > 0.4).astype(float))),
            Material('#ffffff', '#ffffff', emissive='#ffffff', pattern=pat_all())]
    return parts, mats, dict(view_pitch=0.3, view_yaw=0.25), None


def cantor_key(c1, c2, t):
    """A gold key whose bit is the Cantor set - each tier of teeth the last with its middle thirds removed."""
    x0, L = 0.12, 0.62
    boxes = []
    for lvl in range(3):
        segs = [(0.0, 1.0)]
        for _ in range(lvl):
            segs = [s for a, b in segs for s in ((a, a + (b - a) / 3), (b - (b - a) / 3, b))]
        for a, b in segs:
            cx = x0 + (a + b) / 2 * L
            boxes.append((cx, -0.12 - lvl * 0.12, (b - a) / 2 * L))

    def teeth(p):
        d = None
        for cx, cy, hw in boxes:
            di = sd_box(p - np.array([cx, cy, 0]), (max(hw - 0.006, 0.012), 0.045, 0.05))
            d = di if d is None else np.minimum(d, di)
        return d
    R = rotz(0.45)
    parts = [Part(lambda p: sd_torus(_swap_xy(p - np.array([-0.55, 0.0, 0])), 0.22, 0.07), 0, rot=R),
             Part(lambda p: sd_capsule(p, (-0.33, 0.0, 0), (0.76, 0.0, 0), 0.065), 0, rot=R),
             Part(teeth, 1, rot=R),
             Part(lambda p: sd_octa(p - np.array([-0.55, 0, 0]), 0.12), 2, rot=R)]
    mats = [Material(c2, '#ffffff', metal=1.0, pattern=pat_noise_shade(8, 0.1)),
            Material(c2, '#ffffff', metal=1.0, emissive='#ffffff', pattern=pat_spots(t, 0.85, 7)),
            Material(c1, c2, emissive=c2, pattern=pat_pulse_core(t))]

    def ov(img):
        anim.ov_fractal(img, t, 3, H(c2) * 0.8)
    return parts, mats, dict(view_pitch=0.2, view_yaw=0.15, zoom=1.05), ov


def horizon_diver(c1, c2, t):
    """A diving helmet for falling into a black hole: the dark visor reflects a turning orange accretion ring."""
    def visor(p, n):
        x, y = p[..., 0], p[..., 2]
        r = np.sqrt(x * x + y * y)
        a = np.arctan2(y, x)
        ring = (np.abs(r - 0.19 - 0.03 * np.cos(2 * a)) < 0.04) & (np.sin(a * 2 + TAU * t * 2) > -0.5)
        return np.zeros(r.shape), ring.astype(float)
    parts = [Part(lambda p: sd_sphere(p, 0.56), 0, pos=(0, 0.05, 0)),
             Part(lambda p: sd_cyl(p, 0.33, 0.06, 0.03), 1, pos=(0, 0.08, -0.5), rot=rotx(-1.5708)),
             Part(lambda p: sd_torus(_swap_xy(p), 0.36, 0.05), 2, pos=(0, 0.08, -0.52)),
             Part(lambda p: sd_torus(p, 0.44, 0.08), 2, pos=(0, -0.5, 0)),
             Part(lambda p: sd_cyl(p, 0.07, 0.1), 2, pos=(0.0, 0.66, 0))]
    mats = [Material(c1, c2, metal=0.85, pattern=pat_noise_shade(9, 0.15)),
            _dark(Material('#000000', c2, emissive=c2, pattern=visor, metal=1.0, keep_dark=True)),
            Material('#a08070', '#ffffff', metal=1.0, emissive=c2, pattern=pat_spots(t, 0.9, 2))]
    return parts, mats, dict(view_pitch=0.15, view_yaw=0.35), None


def planck_diver(c1, c2, t):
    """A slim emitter whose tip dissolves into quantum foam - bubbles that swell and pop at random."""
    tip = np.array([0.4, 0.4, 0.0])

    def rings(p, n):
        return np.zeros(p.shape[:-1]), (np.sin(p[..., 1] * 18 + TAU * t * 3) > 0.55).astype(float)
    R = rotz(-0.785)
    parts = [Part(lambda p: sd_capsule(p, (-0.62, -0.62, 0), (-0.22, -0.22, 0), 0.1), 0),
             Part(lambda p: sd_cyl(p, 0.12, 0.28, 0.03), 1, pos=(0.05, 0.05, 0), rot=R),
             Part(lambda p: sd_cyl(p, 0.17, 0.04, 0.02), 2, pos=(-0.2, -0.2, 0), rot=R)]
    mats = [Material(c1, c2, metal=0.6), Material('#40607a', c2, metal=0.9, emissive=c2, pattern=rings),
            Material('#b0c0d0', '#ffffff', metal=1.0), Material(c2, '#ffffff', metal=0.3, emissive=c2, pattern=pat_pulse_core(t))]
    for k in range(5):
        a = TAU * _hash(k, 1)
        off = np.array([math.cos(a), math.sin(a), _hash(k, 2) - 0.5]) * (0.1 + 0.12 * _hash(k, 3))
        r = 0.035 + 0.1 * math.sin(math.pi * ((2 * t + k / 5) % 1.0)) ** 2
        parts.append(Part(lambda p, r=r: sd_sphere(p, r), 3, pos=tip + off))

    def ov(img):
        rng = np.random.default_rng(int(t * 40) + 900)
        x, y, _ = _proj(tip, dict(view_pitch=0.3, view_yaw=0.2))
        for _ in range(5):
            _px_empty(img, x + rng.integers(-6, 7), y + rng.integers(-6, 7), H(c2), 0.8)
    return parts, mats, dict(view_pitch=0.3, view_yaw=0.2), ov


# =============================================================================================== spindles
def carbon_nanotube_bundle(c1, c2, t):
    """Three nanotubes twisted into a rope; the twist rolls along it, ends fraying apart."""
    def rope(p):
        a = p[..., 1] * 4.0 + TAU * t
        c, s = np.cos(a), np.sin(a)
        x = p[..., 0] * c - p[..., 2] * s
        z = p[..., 0] * s + p[..., 2] * c
        d = None
        for k in range(3):
            ox, oz = 0.12 * math.cos(k * TAU / 3), 0.12 * math.sin(k * TAU / 3)
            dk = np.sqrt((x - ox) ** 2 + (z - oz) ** 2) - 0.1
            d = dk if d is None else np.minimum(d, dk)
        return np.maximum(d, np.abs(p[..., 1]) - 0.72) * 0.7

    def hexs(p, n):
        h = (np.sin(p[..., 1] * 30) * np.sin(np.arctan2(p[..., 2], p[..., 0]) * 6) > 0.3)
        return -0.2 * h, np.zeros(h.shape)
    R = rotz(-0.78)
    fray = [R @ np.array(v) for v in ((0, 0.72, 0), (0.05, 0.92, 0.04), (0, 0.72, 0), (-0.1, 0.9, -0.03), (0, 0.72, 0), (0.0, 0.95, 0.1))]
    parts = [Part(rope, 0, rot=R), Part(_segs(fray[0::2], fray[1::2], 0.04), 0)]
    mats = [Material('#606468', '#d0d8e0', metal=0.95, gloss=1.4, pattern=hexs)]
    return parts, mats, dict(view_pitch=0.3, view_yaw=0.5), None


def penrose_spindle(c1, c2, t):
    """An axle through a spinning black hole's orange ergosphere; energy bursts run out along it to the collector cups."""
    R = rotz(0.5)
    Rx = R @ rotz(math.pi / 2)

    def axle(p, n):
        return np.zeros(p.shape[:-1]), (np.sin(np.abs(p[..., 1]) * 12 - TAU * t * 2) > 0.6).astype(float)

    def swirl(p, n):
        a = np.arctan2(p[..., 2], p[..., 0])
        r = np.sqrt(p[..., 0] ** 2 + p[..., 2] ** 2)
        return np.zeros(a.shape), (np.sin(a * 3 - r * 10 + TAU * t * 2) > 0.0).astype(float)
    cup = lambda p: np.maximum(np.abs(sd_sphere(p - np.array([0, 0.2, 0]), 0.2)) - 0.03, p[..., 1] - 0.12)
    parts = [Part(lambda p: sd_cyl(p, 0.05, 0.8), 0, rot=Rx),
             Part(lambda p: sd_sphere(p, 0.17), 1),
             Part(lambda p: sd_torus(p / np.array([1, 0.5, 1]), 0.3, 0.07) * 0.5, 2, rot=Rx),
             Part(cup, 3, pos=R @ np.array([0.72, 0, 0]), rot=R @ rotz(math.pi / 2)),
             Part(cup, 3, pos=R @ np.array([-0.72, 0, 0]), rot=R @ rotz(-math.pi / 2))]
    mats = [Material(c1, c2, metal=0.95, emissive=c2, pattern=axle),
            _dark(Material('#000000', '#000000', metal=0, gloss=0, keep_dark=True)),
            Material('#503018', c2, metal=0.5, emissive=c2, pattern=swirl),
            Material('#8890a0', c2, metal=1.0, emissive=c2, pattern=lambda p, n: (np.zeros(p.shape[:-1]), np.full(p.shape[:-1], float(math.sin(TAU * t * 2 - 2.5) > 0.6))))]
    return parts, mats, dict(view_pitch=0.3, view_yaw=0.25, zoom=1.05), None


def telomerase_complex(c1, c2, t):
    """A double helix scrolling upward into a red enzyme that keeps rebuilding its glowing end-cap."""
    def helix(p):
        a = p[..., 1] * 5.0 - TAU * t
        c, s = np.cos(a), np.sin(a)
        x = p[..., 0] * c - p[..., 2] * s
        z = p[..., 0] * s + p[..., 2] * c
        st = np.minimum(np.sqrt((x - 0.2) ** 2 + z * z), np.sqrt((x + 0.2) ** 2 + z * z)) - 0.06
        qy = np.mod(p[..., 1] - t * 0.25 * 0 + 0.0, 0.16) - 0.08
        rung = sd_box(np.stack([x, qy, z], -1), (0.2, 0.025, 0.03))
        return np.maximum(np.minimum(st, rung), np.abs(p[..., 1] + 0.2) - 0.62) * 0.75

    def code(p, n):
        cap = (p[..., 1] > 0.25) & (math.sin(TAU * t * 2) > -0.2)
        rung = np.abs(np.mod(p[..., 1], 0.16) - 0.08) < 0.03
        return np.zeros(cap.shape), (cap | (rung & (np.sin(p[..., 1] * 8 - TAU * t) > 0.5))).astype(float)
    lump = lambda p: sd_sphere(p, 0.27) + 0.04 * np.sin(p[..., 0] * 14) * np.sin(p[..., 1] * 12 + 1) * np.sin(p[..., 2] * 13)
    R = rotz(0.3)
    parts = [Part(helix, 0, rot=R), Part(lump, 1, pos=R @ np.array([0, 0.55, 0]), rot=roty(TAU * t))]
    mats = [Material('#a0a8b8', c2, metal=0.6, emissive=c2, pattern=code),
            Material('#a03030', c2, metal=0.3, emissive=c2, pattern=pat_spots(t, 0.8, 6))]
    return parts, mats, dict(view_pitch=0.25, view_yaw=0.5), None


def turbomolecular_rotor(c1, c2, t):
    """Stacked bladed discs spinning so fast the blades strobe; stray gas molecules get batted out sideways."""
    def blades(p, n):
        a = np.arctan2(p[..., 2], p[..., 0]) + TAU * t * 3
        r = np.sqrt(p[..., 0] ** 2 + p[..., 2] ** 2)
        b = np.sin(a * 10 + p[..., 1] * 30) > 0
        return 0.3 * b - 0.15, ((r > 0.38) & b & (np.sin(a * 2) > 0.6)).astype(float)
    parts = [Part(lambda p: sd_cyl(p, 0.08, 0.72), 1)]
    for k in range(5):
        y = -0.52 + k * 0.26
        rad = 0.58 - k * 0.04
        parts.append(Part(lambda p, rad=rad: sd_cyl(p, rad, 0.045, 0.015), 0 if k % 2 == 0 else 2, pos=(0, y, 0)))
    parts.append(Part(lambda p: sd_cyl(p, 0.15, 0.06, 0.02), 1, pos=(0, 0.72, 0)))
    mats = [Material(c1, c2, metal=0.95, emissive=c2, pattern=blades), Material('#606878', '#ffffff', metal=1.0),
            Material(mix(H(c1), H(c2), 0.3), c2, metal=0.9, pattern=lambda p, n: (0.25 * np.sign(np.sin(np.arctan2(p[..., 2], p[..., 0]) * 10)), np.zeros(p.shape[:-1])))]

    def ov(img):
        for i in range(4):
            life = (t * 2 + i / 4) % 1.0
            side = 1 if i % 2 == 0 else -1
            y = 9 + i * 4
            _px_empty(img, 15.5 + side * (12 + life * 6), y - life * 2, H(c2), 1 - life)
    return parts, mats, dict(view_pitch=0.4, view_yaw=0.5), ov


# =============================================================================================== cores
def grand_unified_core(c1, c2, t):
    """Red, green and blue force rings tilt together, fuse into one bright ring at high energy, then split again."""
    sep = 0.5 + 0.5 * math.cos(TAU * t)
    parts = [Part(lambda p: sd_sphere(p, 0.24), 0)]
    mats = [Material(c1, c2, emissive=c2, pattern=pat_pulse_core(t))]
    if sep > 0.1:
        for k, col in enumerate(('#ff4848', '#48ff70', '#4878ff')):
            mats.append(Material(col, '#ffffff', emissive=col, pattern=pat_bands(10, 2, t, 0.3, axis=0)))
            R = rotx(0.35 + sep * (k - 1) * 0.9) @ rotz(sep * k * 1.0) @ roty(TAU * t * (1 if k % 2 else -1))
            parts.append(Part(lambda p, rr=0.6 + 0.08 * k * sep: sd_torus(p, rr, 0.045), len(mats) - 1, rot=R))
    else:
        mats.append(Material(c2, '#ffffff', emissive=c2, pattern=pat_all()))
        parts.append(Part(lambda p: sd_torus(p, 0.66, 0.07), 1, rot=rotx(0.35)))

    def ov(img):
        if sep < 0.25:
            anim.ov_rays(img, (t - 0.5) * 4 % 1.0, 1, H(c2), 1)
    return parts, mats, dict(view_pitch=0.3, view_yaw=0.5), ov


def m_theory_core(c1, c2, t):
    """A membrane torus striped in five string-theory colours; the colours flow into one, then apart again."""
    cols = ('#ff6060', '#60ff80', '#6080ff', '#ffd040', '#ff60ff')
    w = 0.5 - 0.5 * math.cos(TAU * t)
    parts, mats = [], []
    spin = TAU * t
    for k, col in enumerate(cols):
        ak = k * TAU / 5 + spin
        def sec(p, ak=ak):
            a = np.arctan2(p[..., 2], p[..., 0])
            rxz = np.sqrt(p[..., 0] ** 2 + p[..., 2] ** 2)
            return np.maximum(sd_torus(p, 0.5, 0.15 + 0.03 * math.sin(TAU * t * 2)), (np.abs(_wrap(a - ak)) - math.pi / 5 + 0.02) * rxz)
        cc = mix(H(col), H(c2), w)
        mats.append(Material(cc, '#ffffff', metal=0.6, emissive=cc, pattern=pat_bands(8, 1, t, 0.35, axis=1)))
        parts.append(Part(sec, k, rot=rotx(-0.75)))
    mats.append(Material(c1, c2, metal=0.8, emissive=c2, pattern=pat_spots(t, 0.6, 11)))
    parts.append(Part(lambda p: sd_sphere(p, 0.2), 5))
    return parts, mats, dict(view_pitch=0.35, view_yaw=0.5), None


def stellar_core(c1, c2, t):
    """A fist-sized star with boiling granulation, held between two crossed containment hoops."""
    def gran(p, n):
        c = np.floor(p * 5)
        h = _hash(c[..., 0], c[..., 1], c[..., 2])
        boil = np.sin(TAU * (t * 2 + h))
        f = p * 5 - c - 0.5
        lane = (np.abs(f) > 0.42).sum(-1) >= 1
        return 0.3 * boil - 0.25 * lane, ((~lane) | (boil > 0.5)).astype(float)
    parts = [Part(lambda p: sd_sphere(p, 0.46), 0),
             Part(lambda p: sd_torus(p, 0.62, 0.04), 1, rot=rotx(1.25) @ rotz(0.4)),
             Part(lambda p: sd_torus(p, 0.62, 0.04), 1, rot=rotz(1.25) @ rotx(0.3))]
    mats = [Material(c1, c2, emissive=mix(H(c1), H(c2), 0.55), pattern=gran),
            Material('#707888', '#ffffff', metal=1.0, emissive='#80c0ff', pattern=pat_bands(8, 1, t, 0.1, axis=0))]

    def ov(img):
        for k in range(2):
            life = (t * 2 + k * 0.5) % 1.0
            a = TAU * (0.15 + 0.45 * k)
            h = math.sin(life * math.pi) * 3
            for s in np.linspace(-0.5, 0.5, 7):
                r = 8 + h * math.cos(s * math.pi)
                _px_empty(img, 15.5 + math.cos(a + s * 0.6) * r, 15.5 + math.sin(a + s * 0.6) * r, H(c1), 0.85 * math.sin(life * math.pi))
    return parts, mats, dict(view_pitch=0.3, view_yaw=0.5), ov


# =============================================================================================== streaks
def tachyon_crystal(c1, c2, t):
    """A needle crystal whose afterimages appear *ahead* of it and slide back into it; its glint runs backwards."""
    parts = [Part(lambda p: sd_gem(p, 0.2, 0.72, 0.15), 0, pos=(-0.18, -0.18, 0), rot=rotz(-0.785) @ roty(0.4))]
    mats = [Material(c1, c2, metal=0.9, gloss=1.5, emissive=c2, pattern=pat_bands(12, -3, t, 0.15, axis=1))]

    def ov(img):
        op = img[..., 3] > 0.5
        rim = op & ~(np.roll(op, 1, 0) & np.roll(op, -1, 0) & np.roll(op, 1, 1) & np.roll(op, -1, 1))
        for k in range(3):
            ph = (2 * t + k / 3) % 1.0
            d = int(round(11 * (1 - ph)))
            if d <= 0:
                continue
            a = 0.85 * ph
            sh = np.zeros_like(rim)
            sh[:-d, d:] = rim[d:, :-d]
            sh &= ~op
            img[sh, :3] = img[sh, :3] * (1 - a) + H(c2) * a
            img[sh, 3] = np.maximum(img[sh, 3], a)
        n = img.shape[0]
        lt = (t * 2) % 1.0
        if lt < 0.5:
            pos = (2 * n + 6) - lt / 0.5 * (2 * n + 12)
            ys, xs = np.mgrid[0:n, 0:n]
            band = np.clip(1 - np.abs((xs + ys) - pos) / 3, 0, 1) * op
            img[..., :3] = img[..., :3] * (1 - band[..., None] * 0.7) + band[..., None] * 0.7
    return parts, mats, dict(view_pitch=0.25, view_yaw=0.4), ov


def wave_packet(c1, c2, t):
    """A helical wave inside a Gaussian envelope: the carrier spins forward through a packet that stays put."""
    def fn(s):
        x = -0.92 + 1.84 * s
        env = math.exp(-(x / 0.42) ** 2)
        ph = 9 * x - TAU * t * 2
        return (x, 0.5 * env * math.cos(ph), 0.5 * env * math.sin(ph))

    def glow(p, n):
        env = np.exp(-(p[..., 0] / 0.42) ** 2)
        return np.zeros(env.shape), (env > 0.35).astype(float)
    parts = [Part(_curve(fn, 34, 0.075), 0), Part(lambda p: sd_capsule(p, (-0.95, 0, 0), (0.95, 0, 0), 0.02), 1)]
    mats = [Material(c1, c2, metal=0.6, emissive=c2, pattern=glow), Material('#305070', c2, metal=0.8)]
    view = dict(view_pitch=0.25, view_yaw=0.1)

    def ov(img):
        for i in range(-10, 11):
            x = i * 0.09
            env = math.exp(-(x / 0.42) ** 2)
            for sgn in (1, -1):
                X, Y, _ = _proj((x, sgn * (0.52 * env + 0.08), 0), view)
                if i % 2 == 0:
                    _px_empty(img, X, Y, H(c2), 0.35)
    return parts, mats, view, ov


# =============================================================================================== strings
def brane_tension_fiber(c1, c2, t):
    """A membrane tented up at one point by a taut fiber that hums; ripples run out across the sheet."""
    def sheet(p):
        r2 = p[..., 0] ** 2 + p[..., 2] ** 2
        hgt = 0.38 * np.exp(-r2 / 0.05) + 0.025 * np.sin(np.sqrt(r2) * 16 - TAU * t * 2)
        return np.maximum((np.abs(p[..., 1] - hgt) - 0.03) * 0.5, sd_box(p, (0.72, 1, 0.72)))

    def rip(p, n):
        r = np.sqrt(p[..., 0] ** 2 + p[..., 2] ** 2)
        return np.zeros(r.shape), ((np.sin(r * 16 - TAU * t * 2) > 0.7) & (r > 0.15)).astype(float)
    base = np.array([0, -0.08, 0])
    top = np.array([0.12, 0.88, 0])

    def fib(s):
        q = base + (top - base) * s
        return q + np.array([1, 0, 0.5]) * 0.08 * math.sin(math.pi * s * 2) * math.sin(TAU * t * 2)
    parts = [Part(sheet, 0, pos=(0, -0.45, 0)), Part(_curve(fib, 10, 0.045), 1),
             Part(lambda p: sd_sphere(p, 0.07), 2, pos=top)]
    mats = [Material(c1, c2, metal=0.5, emissive=c2, pattern=rip), Material(c2, '#ffffff', emissive=c2, pattern=pat_bands(20, 3, t, 0.35, axis=1)),
            Material('#ffffff', '#ffffff', emissive='#ffffff', pattern=pat_all())]
    return parts, mats, dict(view_pitch=0.45, view_yaw=0.6), None


def cosmic_string_story(c1, c2, t):
    """A taut, hair-thin defect: two kinks race inward at light speed, meet in a cusp flare, and pass through.
    Background stars on either side show double (the string's conical deficit)."""
    A = np.array([-0.9, -0.85, 0.0])
    B = np.array([0.9, 0.85, 0.0])
    perp = np.array([-0.69, 0.72, 0.0])
    k = abs((t * 2) % 2 - 1)        # 1 at the ends (t=0,1), 0 in the middle (t=0.5)
    c1p, c2p = 0.5 - 0.5 * k, 0.5 + 0.5 * k

    def fn(s):
        b = 0.16 * (math.exp(-((s - c1p) / 0.07) ** 2) - math.exp(-((s - c2p) / 0.07) ** 2))
        return A + (B - A) * s + perp * b + np.array([0, 0, 0.1 * math.sin(s * TAU)])
    parts = [Part(_curve(fn, 26, 0.05), 0)]
    mats = [Material(c1, c2, emissive=c2, pattern=pat_bands(16, 4, t, 0.3, axis=0))]

    def ov(img):
        for i, (sx, sy) in enumerate(((7, 20), (11, 26), (20, 6), (25, 12))):
            tw = 0.5 + 0.5 * math.sin(TAU * (t * 2 + i / 4))
            _px_empty(img, sx, sy, H(c2), 0.4 + 0.5 * tw)
            _px_empty(img, sx + (3 if sx < 16 else -3), sy + (-3 if sx < 16 else 3), H(c2), 0.25 + 0.3 * tw)
        if k < 0.2:
            f = 1 - k / 0.2
            for kk in range(8):
                a = TAU * kk / 8
                for r in range(1, 2 + int(5 * f)):
                    _px(img, 15.5 + math.cos(a) * r, 15.5 + math.sin(a) * r, WHITE if r < 3 else H(c1), f)
    return parts, mats, dict(view_pitch=0.15, view_yaw=0.1), ov


def geodesic_thread(c1, c2, t):
    """A gridded sphere of curved spacetime wound with one golden thread along a great circle; a bead runs it."""
    def grid(p, n):
        lat = np.arcsin(np.clip(p[..., 1] / 0.5, -1, 1))
        lon = np.arctan2(p[..., 2], p[..., 0])
        g = (np.abs(np.sin(lat * 4)) < 0.15) | (np.abs(np.sin(lon * 4)) < 0.15)
        return -0.2 * (~g), (g & (np.sin(lon + TAU * t) > 0.7)).astype(float)
    R = rotx(1.0) @ rotz(0.5)
    a = TAU * t * 2
    bead = R @ np.array([0.54 * math.cos(a), 0, 0.54 * math.sin(a)])

    def dash(p, n):
        ang = np.arctan2(p[..., 2], p[..., 0])
        return np.zeros(ang.shape), (np.sin(ang * 6 - TAU * t * 4) > -0.3).astype(float)
    parts = [Part(lambda p: sd_sphere(p, 0.5), 0), Part(lambda p: sd_torus(p, 0.54, 0.04), 1, rot=R),
             Part(lambda p: sd_sphere(p, 0.085), 2, pos=bead)]
    mats = [Material(c1, '#6070a0', metal=0.6, emissive='#806040', pattern=grid),
            Material(c2, '#ffffff', emissive=c2, pattern=dash), Material('#ffffff', '#ffffff', emissive='#ffffff', pattern=pat_all())]
    return parts, mats, dict(view_pitch=0.3, view_yaw=0.5), None


# =============================================================================================== swarms
def nanite_swarm(c1, c2, t):
    """A metal block with a bite taken out; a stream of nanites pours from the wound and circles back to feed."""
    blk = lambda p: np.maximum(sd_box(p, (0.3, 0.3, 0.3), 0.03), -sd_sphere(p - np.array([0.3, 0.3, -0.3]), 0.27))

    def edge(p, n):
        d = np.sqrt(((p - np.array([0.3, 0.3, -0.3])) ** 2).sum(-1))
        return np.zeros(d.shape), ((d < 0.32) & (np.sin(d * 40 + TAU * t * 2) > 0.3)).astype(float)
    parts = [Part(blk, 0, pos=(0, -0.15, 0))]
    mats = [Material(c1, c2, metal=0.9, emissive=c2, pattern=edge), Material(c2, '#ffffff', emissive=c2, pattern=pat_all()),
            Material('#b0b8c8', '#ffffff', metal=1.0)]
    for i in range(10):
        u = (i / 10 + t) % 1.0
        a = TAU * u
        pos = (0.66 * math.cos(a), 0.22 + 0.3 * math.sin(2 * a), 0.55 * math.sin(a))
        parts.append(Part(lambda p: sd_octa(p, 0.1), 1 if i % 3 == 0 else 2, pos=pos, rot=roty(a * 3)))
    return parts, mats, dict(view_pitch=0.35, view_yaw=0.6), None


def xenobot_swarm(c1, c2, t):
    """Three C-shaped clumps of living cells circling a pile of pellets they are herding together."""
    def bot(p):
        b = np.maximum(sd_sphere(p, 0.21), -sd_sphere(p - np.array([0.17, 0, 0]), 0.11))
        return b + 0.015 * np.sin(p[..., 0] * 40) * np.sin(p[..., 1] * 37) * np.sin(p[..., 2] * 41)

    def cells(p, n):
        c = np.floor(p * 14)
        h = _hash(c[..., 0], c[..., 1], c[..., 2])
        beat = (h > 0.82) & (np.sin(TAU * (t * 2 + h)) > 0.3)
        return 0.25 * (h - 0.5), beat.astype(float)
    parts = []
    for k in range(3):
        a = TAU * (t + k / 3)
        pos = (0.5 * math.cos(a), 0.06 * math.sin(TAU * t * 2 + k), 0.5 * math.sin(a))
        parts.append(Part(bot, 0, pos=pos, rot=roty(-a - math.pi / 2)))
    for k, q in enumerate(((0, -0.05, 0), (0.1, -0.08, 0.06), (-0.08, -0.07, 0.08), (0.02, 0.04, -0.03))):
        parts.append(Part(lambda p: sd_sphere(p, 0.07), 1, pos=q))
    mats = [Material(mix(H(c1), H(c2), 0.55), c2, metal=0.2, emissive='#ffe0d0', pattern=cells), Material('#8a8070', '#ffffff', metal=0.3)]
    return parts, mats, dict(view_pitch=0.45, view_yaw=0.5), None


# =============================================================================================== tesseracts
def hyperlattice_frame(c1, c2, t):
    """A 3x3 frame whose every window holds a turning cube - each cell bigger inside than out."""
    step = 0.46

    def cellq(p):
        q = p.copy()
        q[..., 0] -= np.clip(np.round(p[..., 0] / step), -1, 1) * step
        q[..., 1] -= np.clip(np.round(p[..., 1] / step), -1, 1) * step
        return q

    def frame(p):
        win = sd_box(cellq(p), (0.17, 0.17, 1.0))
        return np.maximum(sd_box(p, (0.7, 0.7, 0.07), 0.02), -win)
    Rc = roty(TAU * t) @ rotx(0.6)

    def cubes(p):
        q = cellq(p) @ Rc
        return sd_box(q, (0.1, 0.1, 0.1), 0.01)

    def cedge(p, n):
        q = cellq(p) @ Rc
        e = (np.abs(q) > 0.075).sum(-1) >= 2
        return np.zeros(e.shape), e.astype(float)

    def trim(p, n):
        m = np.maximum(np.abs(p[..., 0]), np.abs(p[..., 1]))
        return np.zeros(m.shape), ((m > 0.63) & (np.sin((p[..., 0] + p[..., 1]) * 6 - TAU * t) > 0.3)).astype(float)
    parts = [Part(frame, 0), Part(cubes, 1)]
    mats = [Material(c1, c2, metal=0.8, emissive=c2, pattern=trim), Material(mix(H(c1), H(c2), 0.5), c2, metal=0.7, emissive='#ffffff', pattern=cedge)]
    return parts, mats, dict(view_pitch=0.3, view_yaw=0.45), None


def minkowski_lattice(c1, c2, t):
    """A gridded plane of spacetime with light cones standing at its points, light rings racing up and down them."""
    def cone(h):
        return lambda p: np.maximum((np.sqrt(p[..., 0] ** 2 + p[..., 2] ** 2) - np.abs(p[..., 1]) * 0.75) * 0.8, np.abs(p[..., 1]) - h)

    def lightr(p, n):
        return np.zeros(p.shape[:-1]), (np.sin(np.abs(p[..., 1]) * 26 - TAU * t * 2) > 0.45).astype(float)

    def grid(p, n):
        g = (np.abs(np.mod(p[..., 0] * 2.5 + 0.5, 1) - 0.5) < 0.06) | (np.abs(np.mod(p[..., 2] * 2.5 + 0.5, 1) - 0.5) < 0.06)
        return -0.1, (g & (n[..., 1] > 0.5)).astype(float)
    parts = [Part(lambda p: sd_box(p, (0.82, 0.03, 0.82)), 0, pos=(0, -0.3, 0)),
             Part(cone(0.36), 1, pos=(0, 0.06, 0))]
    for x, z in ((-0.4, -0.4), (0.4, -0.4), (-0.4, 0.4), (0.4, 0.4)):
        parts.append(Part(cone(0.2), 1, pos=(x, -0.1, z)))
    mats = [Material(c1, c2, metal=0.8, emissive=c2, pattern=grid), Material('#a0c0ff', c2, metal=0.3, gloss=1.5, emissive='#ffffff', pattern=lightr)]
    return parts, mats, dict(view_pitch=0.45, view_yaw=0.6), None


def _hypercube(dim):
    V = np.array([[1 if (i >> k) & 1 else -1 for k in range(dim)] for i in range(2 ** dim)], float)
    E = [(i, j) for i in range(2 ** dim) for j in range(i + 1, 2 ** dim) if bin(i ^ j).count('1') == 1]
    return V, E


_P5V, _P5E = _hypercube(5)
_P4V, _P4E = _hypercube(4)


def _rot_nd(dim, i, j, a):
    M = np.eye(dim)
    c, s = math.cos(a), math.sin(a)
    M[i, i], M[i, j], M[j, i], M[j, j] = c, -s, s, c
    return M


def penteract_lattice(c1, c2, t):
    """The 3D-printed shadow is a small glowing node; around it the full 5-cube - 32 corners, 80 edges - turns in 5D."""
    parts = [Part(lambda p: sd_sphere(p, 0.13), 0)]
    mats = [Material(c1, c2, emissive=c2, pattern=pat_pulse_core(t))]
    M = _rot_nd(5, 0, 2, TAU * t) @ _rot_nd(5, 1, 3, -TAU * t) @ _rot_nd(5, 2, 4, TAU * t)
    V = _P5V @ M.T
    ang = np.arange(5) * TAU / 5
    P = np.stack([np.cos(ang), np.sin(ang)], 1) * 4.0
    D = np.cos(ang * 2)
    XY = V @ P + 15.5
    Z = V @ D

    def ov(img):
        zmin, zmax = Z.min(), Z.max()
        order = sorted(_P5E, key=lambda e: -(Z[e[0]] + Z[e[1]]))
        for i, j in order:
            dep = ((Z[i] + Z[j]) / 2 - zmin) / max(zmax - zmin, 1e-6)
            _line(img, XY[i, 0], XY[i, 1], XY[j, 0], XY[j, 1], H(c2), 0.25 + 0.55 * (1 - dep))
        for i in range(32):
            dep = (Z[i] - zmin) / max(zmax - zmin, 1e-6)
            _px(img, XY[i, 0], XY[i, 1], mix(WHITE, H(c2), dep), 0.6 + 0.4 * (1 - dep))
    return parts, mats, dict(view_pitch=0.3, view_yaw=0.5), ov


def tesseract_core(c1, c2, t):
    """A true tesseract projection: the inner cube turns outward through the fourth axis and becomes the outer one."""
    M = _rot_nd(4, 0, 3, t * math.pi / 2)
    V = _P4V @ M.T
    w = V[:, 3]
    s = 0.44 * 2.0 / (2.7 - w)
    P = V[:, :3] * s[:, None]
    P = P @ (roty(0.6) @ rotx(0.45)).T
    near = [(i, j) for i, j in _P4E if w[i] + w[j] > 1e-6]
    far = [(i, j) for i, j in _P4E if w[i] + w[j] <= 1e-6]
    parts = []
    if near:
        parts.append(Part(_segs([P[i] for i, j in near], [P[j] for i, j in near], 0.05), 0))
    if far:
        parts.append(Part(_segs([P[i] for i, j in far], [P[j] for i, j in far], 0.04), 1))
    mats = [Material(c1, '#ffffff', metal=0.8, emissive=c1, pattern=pat_bands(8, 2, t, 0.25, axis=1)),
            Material(c2, '#ffffff', metal=0.8, emissive=c2, pattern=pat_all())]
    return parts, mats, dict(view_pitch=0.3, view_yaw=0.5), None


# =============================================================================================== throats
def einstein_rosen_core(c1, c2, t):
    """An hourglass bridge: the top mouth is a black hole swallowing light, the bottom a white hole pouring it out."""
    def shell(p):
        rxz = np.sqrt(p[..., 0] ** 2 + p[..., 2] ** 2)
        r = np.sqrt(0.13 ** 2 + (0.72 * p[..., 1]) ** 2)
        return np.maximum((np.abs(rxz - r) - 0.04) * 0.6, np.abs(p[..., 1]) - 0.68)

    def flow(p, n):
        return np.zeros(p.shape[:-1]), (np.sin(p[..., 1] * 14 + TAU * t * 2) > 0.65).astype(float)
    R = rotz(0.15)
    parts = [Part(lambda p: np.maximum(shell(p), -p[..., 1]), 0, rot=R),
             Part(lambda p: np.maximum(shell(p), p[..., 1]), 1, rot=R),
             Part(lambda p: sd_torus(p - np.array([0, 0.68, 0]), 0.51, 0.045), 2, rot=R),
             Part(lambda p: sd_torus(p + np.array([0, 0.68, 0]), 0.51, 0.045), 1, rot=R)]
    mats = [_dark(Material('#000000', c2, emissive='#8070a0', pattern=flow, keep_dark=True)),
            Material(c2, '#ffffff', emissive=c2, pattern=lambda p, n: (np.zeros(p.shape[:-1]), np.ones(p.shape[:-1]) * (1 - 0.0))),
            Material('#40304a', '#a090c0', metal=1.0)]

    def ov(img):
        for i in range(4):
            life = (t * 2 + i / 4) % 1.0
            a = TAU * i / 4 + 0.6
            if life < 0.5:
                u = life / 0.5
                r = 9 * (1 - u)
                _px(img, 15.5 + math.cos(a) * r, 5 + u * 6, H(c2), 1 - u * 0.5)
            else:
                u = (life - 0.5) / 0.5
                r = 2 + 12 * u
                _px_empty(img, 15.5 + math.cos(a) * r, 26 + u * 5, WHITE, 1 - u)
    return parts, mats, dict(view_pitch=0.42, view_yaw=0.5), ov


def wormhole_throat(c1, c2, t):
    """Looking straight down a wormhole: rings recede forever into a blue-white light at the far end."""
    parts = [Part(lambda p: sd_torus(_swap_xy(p), 0.62, 0.13), 0)]
    mats = [Material(c1, c2, metal=0.8, emissive='#c080ff', pattern=pat_bands(10, 1, t, 0.12, axis=0)),
            Material('#203060', c2, emissive=c2, pattern=pat_bands(8, 2, t, 0.35, axis=0)),
            Material('#ffffff', '#ffffff', emissive=mix(H(c2), WHITE, 0.6), pattern=pat_all())]
    for k in range(5):
        u = k + t
        r = 0.56 * 0.6 ** u
        th = 0.045 * r / 0.56 + 0.012
        parts.append(Part(lambda p, r=r, th=th: sd_torus(_swap_xy(p), r, th), 1, pos=(0, 0, 0.25 * u)))
    parts.append(Part(lambda p: sd_sphere(p, 0.085), 2, pos=(0, 0, 1.2)))
    return parts, mats, dict(view_pitch=0.1, view_yaw=0.12), None


# =============================================================================================== universes
def _galaxy(t, rcut=0.6, arms_freq=2):
    def gal(p, n):
        x, z = p[..., 0], p[..., 2]
        r = np.sqrt(x * x + z * z) + 1e-6
        arms = np.sin(arms_freq * np.arctan2(z, x) - np.log(r) * 4 - t * TAU) > 0.45
        stars = _hash(np.floor(p[..., 0] * 14), np.floor(p[..., 1] * 14), np.floor(p[..., 2] * 14)) > 0.93
        return np.zeros(r.shape), ((arms & (r < rcut)) | (r < 0.12) | stars).astype(float)
    return gal


def bubble_universe_story(c1, c2, t):
    """A bud on a branch of the World Tree swells into a bubble with a galaxy inside, swings ripe, then is plucked back to a bud."""
    if t < 0.55:
        R = 0.08 + 0.38 * _smooth(t / 0.55)
    elif t < 0.88:
        R = 0.46
    else:
        R = 0.46 - 0.38 * _smooth((t - 0.88) / 0.12)
    sw = 0.08 * math.sin(TAU * t * 2) * (R / 0.46)
    knot = np.array([0.1, 0.4, 0.0])
    ctr = knot + np.array([sw, -0.1 - R, 0])
    branch = [np.array(v) for v in ((-0.95, 0.7, 0), (-0.4, 0.48, 0), (0.1, 0.4, 0), (0.6, 0.48, 0), (0.95, 0.7, 0))]
    parts = [Part(_segs(branch[:-1], branch[1:], [0.08, 0.07, 0.06, 0.05]), 0),
             Part(lambda p: sd_capsule(p, knot, ctr + np.array([0, R * 0.9, 0]), 0.03), 0),
             Part(_ell((0.14, 0.04, 0.08)), 2, pos=(-0.45, 0.58, 0), rot=rotz(0.4)),
             Part(lambda p: sd_sphere(p, R), 1, pos=ctr, rot=rotx(-0.6))]
    mats = [Material('#6a4a28', c2, metal=0.2, pattern=pat_noise_shade(14, 0.25)),
            Material(c1, c2, metal=1.0, emissive=c2, pattern=_galaxy(t * 2)),
            Material('#408040', '#c0ffa0', metal=0.2)]

    def ov(img):
        if t > 0.86 or t < 0.04:
            anim.ov_sparkles(img, t, 13, H(c2), 4, 4)
    return parts, mats, dict(view_pitch=0.15, view_yaw=0.2), ov


def pocket_universe_story(c1, c2, t):
    """Inside a glass bubble a universe bangs from a point, fills the bubble with turning galaxies, and crunches back."""
    if t < 0.08:
        r = 0.04 + 0.1 * t / 0.08
    elif t < 0.3:
        r = 0.14 + 0.38 * _smooth((t - 0.08) / 0.22)
    elif t < 0.8:
        r = 0.52
    else:
        r = 0.52 - 0.48 * _smooth((t - 0.8) / 0.2)
    wob = 0.04 * math.sin(TAU * t * 3)
    parts = [Part(lambda p: sd_sphere(p, r), 0, pos=(0, wob, 0), rot=rotx(-0.5))]
    mats = [Material(c1, c2, metal=1.0, emissive=c2, pattern=_galaxy(t * 3, 0.55, 3))]

    def ov(img):
        cy = 15.5 - wob * 16
        for k in range(48):
            a = TAU * k / 48
            x, y = 15.5 + math.cos(a) * 12.5, cy + math.sin(a) * 12.5
            hi = math.cos(a + 2.4)
            _px_empty(img, x, y, mix(H('#a0c0ff'), WHITE, max(0, hi)), 0.35 + 0.45 * max(0, hi))
        for k in range(5):
            a = 3.6 + k * 0.12
            _px(img, 15.5 + math.cos(a) * 10, cy + math.sin(a) * 10, WHITE, 0.6)
        if t < 0.06 or t > 0.97:
            f = 1 - min(t, 1 - t) / 0.06
            for dy in range(-3, 4):
                for dx in range(-3, 4):
                    _px(img, 15.5 + dx, cy + dy, WHITE, max(0, f * (1 - math.hypot(dx, dy) / 4)))
    return parts, mats, dict(view_pitch=0.6, view_yaw=0.3), ov


def universe_seed_story(c1, c2, t):
    """A trembling grain of false vacuum; a bubble of true vacuum nucleates and inflates out of the frame, then a new grain condenses."""
    if t < 0.55:
        s, jit = 1.0, 0.012 + 0.03 * t / 0.55
    elif t < 0.7:
        s, jit = 1.0 - _smooth((t - 0.55) / 0.15), 0.0
    else:
        s, jit = _smooth((t - 0.7) / 0.3), 0.0
    j = np.array([math.sin(TAU * t * 17), math.sin(TAU * t * 13 + 1), math.sin(TAU * t * 11 + 2)]) * jit
    parts, mats = [], [Material(c1, c2, metal=0.9, emissive=c2, pattern=pat_bands(10, 4, t, 0.1 + 0.25 * min(1, t / 0.55), axis=1)),
                       Material(c1, '#ffffff', metal=1.0, emissive=c1, pattern=pat_bands(10, 2, t, 0.25, axis=0))]
    if s > 0.03:
        parts.append(Part(lambda p: sd_octa(p / s, 0.3) * s, 0, pos=j, rot=roty(TAU * t) @ rotx(0.3)))
        parts.append(Part(lambda p: sd_torus(p / s, 0.44, 0.03) * s, 1, pos=j, rot=rotx(1.1) @ rotz(-TAU * t)))

    def ov(img):
        if 0.5 < t < 0.82:
            u = (t - 0.5) / 0.32
            r = 1.0 * math.exp(u * 3.6)
            for k in range(64):
                a = TAU * k / 64
                _px(img, 15.5 + math.cos(a) * r, 15.5 + math.sin(a) * r, WHITE, 1 - u * 0.7)
                _px(img, 15.5 + math.cos(a) * (r - 1), 15.5 + math.sin(a) * (r - 1), H(c1), (1 - u) * 0.6)
        if t > 0.7:
            anim.ov_infall(img, t, 41, H(c1), 6)
    return parts, mats, dict(view_pitch=0.3, view_yaw=0.5), ov


def panspermia_seed(c1, c2, t):
    """A tumbling meteoroid husk cracked with glowing green life, shedding spores into a short comet tail."""
    def rock(p):
        return sd_sphere(p / np.array([0.5, 0.4, 0.42]), 1.0) * 0.4 + 0.05 * np.sin(p[..., 0] * 9 + 1) * np.sin(p[..., 1] * 8) * np.sin(p[..., 2] * 7 + 2)

    def cracks(p, n):
        v = np.abs(np.sin(p[..., 0] * 7 + np.sin(p[..., 2] * 6) * 2) + np.sin(p[..., 1] * 6 + p[..., 2] * 3)) < 0.18
        pulse = np.sin(TAU * t * 2 + p[..., 1] * 3) > -0.3
        return 0.2 * np.sin(p[..., 0] * 13) * np.sin(p[..., 2] * 11), (v & pulse).astype(float)
    parts = [Part(rock, 0, rot=roty(TAU * t) @ rotz(0.4))]
    mats = [Material(c1, c2, metal=0.3, emissive=c2, pattern=cracks)]

    def ov(img):
        for i in range(8):
            life = (t * 2 + i / 8) % 1.0
            d = 7 + life * 12
            x = 15.5 + d * 0.8 + math.sin(i * 2.3) * 1.5
            y = 15.5 - d * 0.6 + math.cos(i * 1.7) * 1.5
            _px_empty(img, x, y, H(c2), (1 - life) * 0.9)
    return parts, mats, dict(view_pitch=0.3, view_yaw=0.5), ov


# =============================================================================================== warp
def alcubierre_coil(c1, c2, t):
    """A warp ring around a tiny craft: space dips ahead and bulges behind, and the grid streams past."""
    def hf(x, z):
        return 0.34 * (np.exp(-((x - 0.45) ** 2 + z * z) / 0.06) - np.exp(-((x + 0.45) ** 2 + z * z) / 0.06))

    def sheet(p):
        return np.maximum((np.abs(p[..., 1] + hf(p[..., 0], p[..., 2])) - 0.025) * 0.55, sd_box(p, (0.85, 1, 0.75)))

    def grid(p, n):
        g = (np.abs(np.mod((p[..., 0] + t * 0.6) * 3.333, 1) - 0.5) > 0.42) | (np.abs(np.mod(p[..., 2] * 3.333, 1) - 0.5) > 0.42)
        return 0.0, g.astype(float)

    def coil(p, n):
        a = np.arctan2(p[..., 2], p[..., 0])
        return 0.2 * np.sign(np.sin(a * 14)), (np.sin(a * 2 - TAU * t * 2) > 0.5).astype(float)
    parts = [Part(sheet, 0, pos=(0, -0.32, 0)),
             Part(lambda p: sd_torus(p / np.array([1, 0.6, 1]), 0.36, 0.09) * 0.6, 1, pos=(0, 0.08, 0)),
             Part(lambda p: sd_capsule(p, (-0.16, 0, 0), (0.16, 0, 0), 0.07), 2, pos=(0, 0.08, 0))]
    mats = [Material('#182040', c2, metal=0.6, emissive=mix(H(c1), H(c2), 0.5), pattern=grid),
            Material(c1, c2, metal=1.0, emissive=c2, pattern=coil), Material('#c0c8d8', '#ffffff', metal=1.0)]
    return parts, mats, dict(view_pitch=0.55, view_yaw=0.45), None


def plasma_nozzle_tip(c1, c2, t):
    """A converging-diverging bell ringed by magnet coils, a flickering orange plasma jet roaring out of it."""
    def bell(p):
        rxz = np.sqrt(p[..., 0] ** 2 + p[..., 2] ** 2)
        r = 0.1 + 0.55 * (p[..., 1] - 0.25) ** 2
        return np.maximum((np.abs(rxz - r) - 0.035) * 0.6, np.abs(p[..., 1] - 0.05) - 0.45)
    fl = 0.08 * math.sin(TAU * t * 4) + 0.04 * math.sin(TAU * t * 7)
    R = rotz(0.6)
    parts = [Part(bell, 0, pos=R @ np.array([0, 0.3, 0]), rot=R @ roty(0.0)),
             Part(lambda p: sd_torus(p, 0.17, 0.045), 1, pos=R @ np.array([0, 0.55, 0]), rot=R),
             Part(lambda p: sd_torus(p, 0.25, 0.045), 1, pos=R @ np.array([0, 0.22, 0]), rot=R),
             Part(lambda p: sd_capsule(p, (0, -0.1, 0), (0, -0.85 - fl, 0), 0.11 + 0.02 * math.sin(TAU * t * 6)), 2, rot=R),
             Part(lambda p: sd_capsule(p, (0, 0.0, 0), (0, -0.55 - fl * 0.5, 0), 0.05), 3, rot=R)]
    mats = [Material(c1, c2, metal=0.9, emissive=c2, pattern=pat_bands(10, 1, t, 0.08, axis=1)),
            Material('#c07040', '#ffd0a0', metal=1.0, emissive='#80c0ff', pattern=pat_bands(20, 2, t, 0.12, axis=0)),
            Material(mix(H(c2), WHITE, 0.3), '#ffffff', emissive=np.clip(mix(H(c2), WHITE, 0.45) * 1.25, 0, 1), pattern=pat_all()),
            Material('#ffffff', '#ffffff', emissive=mix(H(c2), WHITE, 0.75), pattern=pat_all())]

    view = dict(view_pitch=0.3, view_yaw=0.2)

    def ov(img):
        x0, y0, _ = _proj(R @ np.array([0, -0.12, 0]), view)
        x1, y1, _ = _proj(R @ np.array([0, -0.8 - fl, 0]), view)
        for k in range(14):
            u = k / 13
            col = mix(WHITE, H(c2), min(1, u * 1.6))
            _px(img, x0 + (x1 - x0) * u, y0 + (y1 - y0) * u, col, 1.0 - 0.3 * u)
            _px(img, x0 + (x1 - x0) * u + 1, y0 + (y1 - y0) * u, mix(H(c2), WHITE, 0.3 * (1 - u)), 0.9 - 0.3 * u)
        for i in range(5):
            life = (t * 2 + i / 5) % 1.0
            d = 10 + life * 7
            _px_empty(img, 15.5 + d * 0.56 + math.sin(i * 2.1) * 2, 15.5 + d * 0.83, H(c2), 1 - life)
    return parts, mats, view, ov


# =============================================================================================== registry
ITEMS = {
    'qubit_array_wafer': qubit_array_wafer,
    'room_temperature_superconductor': room_temperature_superconductor,
    'topological_board': topological_board,
    'topological_logic_wafer': topological_logic_wafer,
    'topological_wafer': topological_wafer,
    'entanglement_tuner': entanglement_tuner,
    'gene_sampler': gene_sampler,
    'von_neumann_probe': von_neumann_probe,
    'axiom_key': axiom_key,
    'reality_anchor': reality_anchor,
    'kugelblitz_seed': kugelblitz_story,
    'primordial_harvest': primordial_harvest,
    'spark_of_life': spark_of_life,
    'arrow_of_time_shard': arrow_of_time_shard,
    'axiom_fragment': axiom_fragment,
    'cardinal_shard': cardinal_shard,
    'cosmic_record_fragment': cosmic_record_fragment,
    'horizon_shard': horizon_shard,
    'kaluza_klein_shard': kaluza_klein_shard,
    'kerr_metric_shard': kerr_metric_shard,
    'magnetar_shard': magnetar_shard,
    'ordinal_shard': ordinal_shard,
    'pentaquark_shard': pentaquark_shard,
    'prime_shard': prime_shard,
    'proof_fragment': proof_fragment,
    'silicon_life_spore': silicon_life_spore,
    'superpartner_shard': superpartner_shard,
    'void_lattice_shard': void_lattice_shard,
    'brane_shifter': brane_shifter,
    'cantor_key': cantor_key,
    'horizon_diver': horizon_diver,
    'planck_diver': planck_diver,
    'carbon_nanotube_bundle': carbon_nanotube_bundle,
    'penrose_spindle': penrose_spindle,
    'telomerase_complex': telomerase_complex,
    'turbomolecular_rotor': turbomolecular_rotor,
    'grand_unified_core': grand_unified_core,
    'm_theory_core': m_theory_core,
    'stellar_core': stellar_core,
    'tachyon_crystal': tachyon_crystal,
    'wave_packet': wave_packet,
    'brane_tension_fiber': brane_tension_fiber,
    'cosmic_string_segment': cosmic_string_story,
    'geodesic_thread': geodesic_thread,
    'nanite_swarm': nanite_swarm,
    'xenobot_swarm': xenobot_swarm,
    'hyperlattice_frame': hyperlattice_frame,
    'minkowski_lattice': minkowski_lattice,
    'penteract_lattice': penteract_lattice,
    'tesseract_core': tesseract_core,
    'einstein_rosen_core': einstein_rosen_core,
    'wormhole_throat': wormhole_throat,
    'bubble_universe_sample': bubble_universe_story,
    'pocket_universe': pocket_universe_story,
    'panspermia_seed': panspermia_seed,
    'universe_seed': universe_seed_story,
    'alcubierre_coil': alcubierre_coil,
    'plasma_nozzle_tip': plasma_nozzle_tip,
}

# motions / stock overlays per item (most of the character lives in the builders themselves)
RECIPES = {
    'qubit_array_wafer': ((), ()),
    'room_temperature_superconductor': (('rock',), ()),
    'topological_board': ((), ('glint',)),
    'topological_logic_wafer': (('rock',), ()),
    'topological_wafer': ((), ('glint',)),
    'entanglement_tuner': (('rock',), ()),
    'gene_sampler': ((), ()),
    'von_neumann_probe': ((), ()),
    'axiom_key': (('bob',), ()),
    'reality_anchor': ((), ()),
    'primordial_harvest': (('bob',), ()),
    'spark_of_life': (('heartbeat',), ()),
    'arrow_of_time_shard': (('rock',), ()),
    'axiom_fragment': ((), ()),
    'cardinal_shard': ((), ()),
    'cosmic_record_fragment': ((), ('sparkles',)),
    'horizon_shard': ((), ()),
    'kaluza_klein_shard': (('rock',), ()),
    'kerr_metric_shard': (('tumble',), ()),
    'magnetar_shard': (('spin',), ()),
    'ordinal_shard': ((), ('sparkles',)),
    'pentaquark_shard': ((), ()),
    'prime_shard': (('rock',), ()),
    'proof_fragment': ((), ()),
    'silicon_life_spore': (('tumble', 'breathe'), ()),
    'superpartner_shard': ((), ()),
    'void_lattice_shard': (('spin',), ()),
    'brane_shifter': ((), ()),
    'cantor_key': (('rock',), ()),
    'horizon_diver': (('rock',), ('glint',)),
    'planck_diver': ((), ()),
    'carbon_nanotube_bundle': ((), ('glint',)),
    'penrose_spindle': (('rock',), ()),
    'telomerase_complex': ((), ()),
    'turbomolecular_rotor': ((), ()),
    'grand_unified_core': ((), ()),
    'm_theory_core': (('tumble',), ()),
    'stellar_core': (('pulse',), ()),
    'tachyon_crystal': ((), ()),
    'wave_packet': ((), ()),
    'brane_tension_fiber': (('rock',), ()),
    'geodesic_thread': (('spin_back',), ()),
    'nanite_swarm': ((), ()),
    'xenobot_swarm': ((), ()),
    'hyperlattice_frame': (('rock',), ()),
    'minkowski_lattice': (('spin',), ()),
    'penteract_lattice': ((), ()),
    'tesseract_core': ((), ()),
    'einstein_rosen_core': ((), ()),
    'wormhole_throat': ((), ()),
    'panspermia_seed': ((), ()),
    'alcubierre_coil': ((), ()),
    'plasma_nozzle_tip': ((), ()),
}

# These five ids already had (shared) story animations upstream; each now gets its own.
# Only three are real 80-frame stories; the other two are ordinary 40-frame loops routed through NARRATIVES
# because a NARRATIVES entry always wins over ITEMS in anim.render_frame.
NARRATIVES = {
    'kugelblitz_seed': kugelblitz_story,
    'pocket_universe': pocket_universe_story,
    'universe_seed': universe_seed_story,
    'cosmic_string_segment': cosmic_string_story,
    'bubble_universe_sample': bubble_universe_story,
}
FRAMES = {'cosmic_string_segment': 40}
