"""
1.2.0 (art pass): hand-made 3D builders for the items added since 1.1, so each one reads as what it is rather than a
recoloured 1.1 shape. Same renderer and conventions as items3d.py: a builder takes (c1, c2, t) and returns
(parts, materials, view kwargs[, overlay]). items3d.py merges BUILDERS into ITEMS last, so these win over the
art-kind defaults.
"""
import math
import numpy as np
from render3d import Part, Material, sd_sphere, sd_box, sd_octa, sd_torus, sd_cyl, sd_capsule, sd_hexprism, sd_gem, rotx, roty, rotz, hexc

TAU = math.pi * 2
BUILDERS = {}
# motions and overlays per item (anim.RECIPE_OVERRIDE); anything not listed rocks gently with a glint
MOTIONS = {}


def reg(*ids):
    def deco(fn):
        for i in ids:
            BUILDERS[i] = fn
        return fn
    return deco


# ------------------------------------------------------------------------------- shape helpers
def V(*a):
    return np.array(a, float)


def sd_ellipsoid(p, r):
    r = np.asarray(r, float)
    k0 = np.linalg.norm(p / r, axis=-1)
    k1 = np.linalg.norm(p / (r * r), axis=-1)
    return k0 * (k0 - 1.0) / np.maximum(k1, 1e-6)


def smin(a, b, k=0.12):
    h = np.clip(0.5 + 0.5 * (b - a) / k, 0, 1)
    return b * (1 - h) + a * h - k * h * (1 - h)


def sd_cone_y(p, r0, r1, h):
    """A frustum along y from -h (radius r0) to +h (radius r1)."""
    y = p[..., 1]
    q = np.sqrt(p[..., 0] ** 2 + p[..., 2] ** 2)
    r = r0 + (r1 - r0) * np.clip((y + h) / (2 * h), 0, 1)
    return np.maximum((q - r) * 0.85, np.abs(y) - h)


def rep_xz(p, s):
    q = p.copy()
    q[..., 0] = np.mod(p[..., 0] + s / 2, s) - s / 2
    q[..., 2] = np.mod(p[..., 2] + s / 2, s) - s / 2
    return q


def capsule_chain(pts, r, mat):
    return [Part(lambda p, a=pts[i], b=pts[i + 1]: sd_capsule(p, a, b, r), mat) for i in range(len(pts) - 1)]


def tube(fn, n, r, mat):
    return capsule_chain([fn(i / n) for i in range(n + 1)], r, mat)


def em_all():
    return lambda p, n: (np.zeros(p.shape[:-1]), np.ones(p.shape[:-1]))


def em_bands(freq, t, axis=1, width=0.3, speed=1):
    return lambda p, n: (np.zeros(p.shape[:-1]), (np.sin(p[..., axis] * freq - t * TAU * speed) > 1 - width * 2).astype(float))


def shade_noise(scale=7, amt=0.2, seed=0):
    return lambda p, n: (np.sin(p[..., 0] * scale + seed) * np.sin(p[..., 1] * scale * 1.3 + 2 + seed) * np.sin(p[..., 2] * scale * 0.8 + 1) * amt,
                         np.zeros(p.shape[:-1]))


def em_spots(t, density=0.86, seed=1, scale=8):
    def f(p, n):
        h = np.sin(np.floor(p[..., 0] * scale) * 12.99 + np.floor(p[..., 1] * scale) * 78.23 + np.floor(p[..., 2] * scale) * 37.72 + seed) * 43758.5
        h = h - np.floor(h)
        return np.zeros(h.shape), ((h > density) & (np.sin(h * 40 + t * TAU) > -0.2)).astype(float)
    return f


def glass(col='#c8e0f0'):
    return Material(col, '#ffffff', metal=0.95)


def vial(liquid, cap, t, emblem=None, lasting=False, glow=True):
    """A stoppered ampoule of glowing liquid, an emblem floating in front of it."""
    mats = [Material(liquid, '#ffffff', emissive=liquid if glow else None, pattern=em_bands(10, t, 1, 0.22) if glow else None, metal=0.6),
            glass(), Material(cap, '#ffffff', metal=0.85)]
    parts = [Part(lambda p: sd_cyl(p, 0.24, 0.42, 0.06), 0, pos=(0, -0.18, 0)),
             Part(lambda p: sd_torus(p, 0.27, 0.035), 1, pos=(0, 0.22, 0)),
             Part(lambda p: sd_torus(p, 0.27, 0.035), 1, pos=(0, -0.55, 0)),
             Part(lambda p: sd_cyl(p, 0.12, 0.1, 0.02), 1, pos=(0, 0.33, 0)),
             Part(lambda p: sd_cyl(p, 0.17, 0.12, 0.04), 2, pos=(0, 0.5, 0))]
    if lasting:
        mats.append(Material('#ffd040', '#ffffff', emissive='#ffd040', pattern=em_bands(6, t, 0, 0.3), metal=0.9))
        parts.append(Part(lambda p: sd_torus(p, 0.5, 0.04), len(mats) - 1, pos=(0, 0.05, 0), rot=rotx(1.25) @ rotz(t * TAU)))
    if emblem:
        mats.append(Material(emblem[1], '#ffffff', emissive=emblem[1], pattern=em_all(), metal=0.7))
        k = len(mats) - 1
        parts += emblem[0](k, t)
    return parts, mats, dict(view_pitch=0.3, view_yaw=0.6, zoom=1.05)


# =============================================================================== 1.4 Biology
@reg('sterile_pack')
def b_sterile_pack(c1, c2, t):
    stripes = lambda p, n: (np.zeros(p.shape[:-1]), ((np.sin((p[..., 0] + p[..., 2]) * 14) > 0.55) & (np.abs(p[..., 0]) < 0.5)).astype(float))
    mats = [Material('#f0f2f4', '#ffffff', metal=0.2), Material('#e8e8f0', '#ffffff', emissive='#4090ff', pattern=stripes, metal=0.2),
            Material('#c8ccd4', '#ffffff', metal=0.3)]
    body = lambda p: np.maximum(sd_box(p, (0.62, 0.12, 0.48), 0.05), sd_ellipsoid(p, (0.8, 0.13, 0.66)))
    parts = [Part(body, 0), Part(lambda p: sd_box(p, (0.45, 0.13, 0.08), 0.02), 1, pos=(0, 0.0, 0)),
             Part(lambda p: sd_box(p, (0.66, 0.04, 0.06), 0.01), 2, pos=(0, 0, 0.52)),
             Part(lambda p: sd_box(p, (0.66, 0.04, 0.06), 0.01), 2, pos=(0, 0, -0.52))]
    return parts, mats, dict(view_pitch=0.75, view_yaw=0.5)


def _bilayer(heads_col, tails_col, t, pore=None):
    s = 0.24
    heads = lambda p, y: np.maximum(sd_sphere(rep_xz(p, s) - V(0, y, 0), 0.105), sd_box(p, (0.66, 1, 0.5)))
    tail = lambda p: np.maximum(sd_cyl(rep_xz(p, s), 0.035, 0.19), sd_box(p, (0.66, 1, 0.5)))
    mats = [Material(heads_col, '#ffffff', metal=0.35), Material(tails_col, '#fff0c0', metal=0.15)]
    parts = [Part(lambda p: heads(p, 0.26), 0), Part(lambda p: heads(p, -0.26), 0),
             Part(lambda p: tail(p - V(0.03, 0, 0)), 1), Part(lambda p: tail(p + V(0.03, 0, 0)), 1)]
    if pore:
        mats.append(Material(pore, '#ffffff', metal=0.6))
        mats.append(Material('#80e0ff', '#ffffff', emissive='#80e0ff', pattern=em_all()))
        parts = [Part(lambda p, f=pp.fn: np.maximum(f(p), -sd_cyl(p, 0.2, 1)), pp.mat) for pp in parts]
        parts.append(Part(lambda p: np.maximum(sd_cyl(p, 0.2, 0.36, 0.03), -sd_cyl(p, 0.09, 1)), 2))
        y = math.sin(t * TAU) * 0.55
        parts.append(Part(lambda p: sd_sphere(p - V(0, y, 0), 0.08), 3))
    return parts, mats, dict(view_pitch=0.55, view_yaw=0.6, zoom=0.95)


@reg('lipid_bilayer')
def b_lipid_bilayer(c1, c2, t):
    return _bilayer('#e8b850', '#f8eccc', t)


@reg('ion_channel_membrane')
def b_ion_channel(c1, c2, t):
    return _bilayer('#d0a040', '#f8eccc', t, pore='#a04070')


def _cell_cutaway(membrane, cyto, t, nucleus='#7040a0', extra=True, pos=(0, 0, 0), r=0.62, cut=True, mirror=False):
    pos = V(*pos)
    mx = -1 if mirror else 1
    mats = [Material(membrane, '#ffffff', metal=0.4), Material(cyto, '#ffffff', metal=0.2),
            Material(nucleus, '#e0c0ff', metal=0.3, pattern=shade_noise(14, 0.15)), Material('#e08030', '#ffd080', metal=0.3)]
    wedge = (lambda p: sd_box(p - V(0.5 * mx, 0.5, -0.5) * r, V(0.5, 0.5, 0.5) * r)) if cut else (lambda p: np.full(p.shape[:-1], 9.0))
    shell = lambda p: np.maximum(np.abs(sd_sphere(p, r)) - 0.05, -wedge(p))
    inner = lambda p: np.maximum(sd_sphere(p, r - 0.06), -wedge(p + V(0.04 * mx, -0.04, 0.04)))
    parts = [Part(shell, 0, pos=pos), Part(inner, 1, pos=pos), Part(lambda p: sd_sphere(p, r * 0.36), 2, pos=pos + V(0.12 * mx, 0.1, -0.1) * r / 0.62)]
    if extra:
        a = t * TAU
        parts.append(Part(lambda p: sd_capsule(p, V(-0.12, 0, 0), V(0.12, 0, 0), 0.07 * r / 0.62 + 0.02), 3, pos=pos + V(-0.22 * mx, -0.22, -0.2) * r / 0.62,
                          rot=rotz(0.6 + 0.2 * math.sin(a))))
    return parts, mats


@reg('living_cell')
def b_living_cell(c1, c2, t):
    parts, mats = _cell_cutaway('#80d070', '#d8f0c8', t)
    return parts, mats, dict(view_pitch=0.4, view_yaw=0.75)


@reg('yeast_culture')
def b_yeast(c1, c2, t):
    mats = [Material('#e8d098', '#fff4d0', metal=0.25, pattern=shade_noise(10, 0.12)), Material('#c8a060', '#ffe0a0', metal=0.2)]
    cells = [((-0.22, -0.18, 0.05), (0.3, 0.24, 0.24), 0.5), ((0.24, -0.25, -0.1), (0.27, 0.22, 0.22), -0.4),
             ((0.02, 0.22, -0.05), (0.28, 0.22, 0.22), 1.1)]
    parts = []
    for c, r, a in cells:
        parts.append(Part(lambda p, r=r: sd_ellipsoid(p, r), 0, pos=c, rot=rotz(a)))
    bud = 0.08 + 0.05 * (0.5 + 0.5 * math.sin(t * TAU))
    parts.append(Part(lambda p: sd_sphere(p, bud), 1, pos=(0.3, 0.42, 0.0)))
    parts.append(Part(lambda p: sd_sphere(p, 0.06), 1, pos=(-0.45, -0.38, 0.12)))
    return parts, mats, dict(view_pitch=0.35, view_yaw=0.6)


@reg('algae_culture')
def b_algae(c1, c2, t):
    """Volvox: a hollow green sphere of cells with daughter colonies inside (cut open)."""
    dots = lambda p, n: (np.zeros(p.shape[:-1]), (np.sin(p[..., 0] * 22) * np.sin(p[..., 1] * 22) * np.sin(p[..., 2] * 22) > 0.35).astype(float))
    mats = [Material('#2c7a30', '#a0ff90', emissive='#90ff70', pattern=dots, metal=0.3), Material('#58c040', '#d0ffb0', emissive='#b0ff80',
                                                                                                    pattern=em_bands(16, t, 0, 0.2), metal=0.3)]
    wedge = lambda p: sd_box(p - V(0.32, 0.32, -0.32), V(0.32, 0.32, 0.32))
    parts = [Part(lambda p: np.maximum(np.abs(sd_sphere(p, 0.62)) - 0.06, -wedge(p)), 0)]
    a = t * TAU
    for k, (x, y, z) in enumerate(((0.12, 0.15, -0.1), (-0.2, -0.1, 0.05), (0.1, -0.22, -0.15))):
        parts.append(Part(lambda p, k=k: sd_sphere(p, 0.15 - 0.02 * k), 1, pos=(x + 0.04 * math.cos(a + k), y, z + 0.04 * math.sin(a + k))))
    return parts, mats, dict(view_pitch=0.4, view_yaw=0.75)


@reg('messenger_rna')
def b_mrna(c1, c2, t):
    cols = ['#ff5050', '#50a0ff', '#ffd040', '#50e070']
    mats = [Material('#c04060', '#ff90b0', metal=0.5)] + [Material(c, '#ffffff', emissive=c, pattern=em_all(), metal=0.4) for c in cols] + \
           [Material('#ffe0f0', '#ffffff', metal=0.7)]
    path = lambda u: V(-0.7 + 1.4 * u, 0.32 * math.sin(u * TAU * 1.2 + t * TAU * 0.5), 0.12 * math.cos(u * TAU))
    parts = tube(path, 16, 0.055, 0)
    for k in range(11):
        u = (k + 0.5) / 11
        a = path(u)
        parts.append(Part(lambda p, a=a: sd_capsule(p, a, a + V(0, -0.2, 0.05), 0.045), 1 + k % 4))
    parts.append(Part(lambda p: sd_sphere(p, 0.11), 5, pos=tuple(path(0))))
    return parts, mats, dict(view_pitch=0.35, view_yaw=0.35)


def _petri(dish, agar):
    mats = [glass('#d8e4ec'), Material(agar, '#ffffff', metal=0.15, pattern=shade_noise(12, 0.08))]
    parts = [Part(lambda p: np.maximum(sd_cyl(p, 0.72, 0.1, 0.03), -sd_cyl(p - V(0, 0.08, 0), 0.66, 0.1)), 0, pos=(0, -0.15, 0)),
             Part(lambda p: sd_cyl(p, 0.66, 0.035), 1, pos=(0, -0.12, 0))]
    return parts, mats


@reg('bacterial_culture')
def b_bacteria(c1, c2, t):
    parts, mats = _petri('#d8e4ec', '#e8d8a0')
    mats.append(Material('#d8c060', '#fff8b0', metal=0.3))
    rng = np.random.default_rng(4)
    for k in range(9):
        x, z, a = rng.uniform(-0.45, 0.45), rng.uniform(-0.45, 0.45), rng.uniform(0, math.pi)
        wig = 0.03 * math.sin(t * TAU + k)
        parts.append(Part(lambda p: sd_capsule(p, V(-0.13, 0, 0), V(0.13, 0, 0), 0.065), 2, pos=(x + wig, -0.02, z), rot=roty(a)))
    return parts, mats, dict(view_pitch=0.8, view_yaw=0.6)


@reg('bacteriophage')
def b_phage(c1, c2, t):
    mats = [Material('#8048c8', '#e0b0ff', metal=0.6, pattern=em_spots(t, 0.9), emissive='#e0a0ff'), Material('#a0a0b8', '#ffffff', metal=0.7),
            Material('#606078', '#c0c0d0', metal=0.6)]
    parts = [Part(lambda p: sd_octa(p / V(1, 1.25, 1), 0.3) * 0.8, 0, pos=(0, 0.42, 0), rot=roty(0.4)),
             Part(lambda p: sd_cyl(p, 0.06, 0.26), 1, pos=(0, 0.02, 0)),
             Part(lambda p: sd_cyl(p, 0.1, 0.03), 1, pos=(0, 0.17, 0)),
             Part(lambda p: sd_hexprism(p, 0.14, 0.03), 2, pos=(0, -0.26, 0), rot=rotx(math.pi / 2))]
    flex = 0.08 * math.sin(t * TAU)
    for k in range(6):
        a = k * TAU / 6 + 0.3
        knee = V(math.cos(a) * 0.34, -0.36 + flex, math.sin(a) * 0.34)
        foot = V(math.cos(a) * 0.5, -0.62, math.sin(a) * 0.5)
        parts += capsule_chain([V(0, -0.26, 0), knee, foot], 0.028, 2)
    return parts, mats, dict(view_pitch=0.25, view_yaw=0.5)


@reg('mould_culture')
def b_mould(c1, c2, t):
    parts, mats = _petri('#d8e4ec', '#d8d0b0')
    mats += [Material('#e8e8e0', '#ffffff', metal=0.1), Material('#3a8a70', '#a0f0d0', metal=0.3, pattern=em_spots(t, 0.8, 3, 18), emissive='#a0ffe0')]
    parts.append(Part(lambda p: sd_ellipsoid(p, (0.42, 0.06, 0.42)) + 0.02 * np.sin(p[..., 0] * 30) * np.sin(p[..., 2] * 30), 2, pos=(0, -0.08, 0)))
    for k, (x, z, h) in enumerate(((0, 0, 0.5), (0.22, 0.12, 0.38), (-0.2, 0.15, 0.42), (0.08, -0.25, 0.35), (-0.15, -0.15, 0.3))):
        sway = 0.04 * math.sin(t * TAU + k)
        top = V(x + sway, -0.05 + h, z)
        parts.append(Part(lambda p, a=V(x, -0.05, z), b=top: sd_capsule(p, a, b, 0.025), 2))
        for j in range(4):
            aa = j * TAU / 4 + k
            parts.append(Part(lambda p, c=top + V(math.cos(aa) * 0.06, 0.04, math.sin(aa) * 0.06): sd_sphere(p - c, 0.055), 3))
    return parts, mats, dict(view_pitch=0.5, view_yaw=0.6)


@reg('antibiotic')
def b_antibiotic(c1, c2, t):
    half = lambda s: (lambda p: np.maximum(sd_capsule(p, V(-0.42, 0, 0), V(0.42, 0, 0), 0.24), s * p[..., 0]))
    mats = [Material('#f4f4f8', '#ffffff', metal=0.6), Material('#30b0a0', '#a0fff0', metal=0.6)]
    parts = [Part(half(1), 0, rot=rotz(0.5)), Part(half(-1), 1, rot=rotz(0.5)),
             Part(lambda p: sd_capsule(p, V(-0.2, 0, 0), V(0.2, 0, 0), 0.12), 0, pos=(0.32, -0.46, 0.25), rot=roty(1.2) @ rotz(0.2))]
    return parts, mats, dict(view_pitch=0.4, view_yaw=0.6)


@reg('probiotic_seal')
def b_probiotic(c1, c2, t):
    mats = [Material('#d8a040', '#fff0c0', metal=0.5, pattern=shade_noise(9, 0.1)), Material('#f0d080', '#ffffff', metal=0.6),
            Material('#90c050', '#e0ffb0', emissive='#c0ff80', pattern=em_bands(8, t, 0, 0.25), metal=0.4)]
    blob = lambda p: sd_cyl(p, 0.6 + 0.05 * np.sin(np.arctan2(p[..., 2], p[..., 0]) * 7), 0.1, 0.05)
    parts = [Part(blob, 0), Part(lambda p: sd_torus(p, 0.42, 0.045), 1, pos=(0, 0.1, 0))]
    for a in (0.3, 1.9):
        parts.append(Part(lambda p: sd_capsule(p, V(-0.16, 0, 0), V(0.16, 0, 0), 0.06), 2, pos=(0, 0.12, 0), rot=roty(a)))
    return parts, mats, dict(view_pitch=0.85, view_yaw=0.4)


@reg('physarum_culture')
def b_physarum(c1, c2, t):
    parts, mats = _petri('#d8e4ec', '#3a2a18')
    pulse = lambda p, n: (np.zeros(p.shape[:-1]), (np.sin(np.sqrt(p[..., 0] ** 2 + p[..., 2] ** 2) * 16 - t * TAU * 2) > 0.3).astype(float))
    mats += [Material('#e0c020', '#ffff80', emissive='#ffe040', pattern=pulse, metal=0.3), Material('#c09020', '#fff0a0', metal=0.3)]
    for k in range(6):
        a = k * TAU / 6 + 0.2
        mid = V(math.cos(a) * 0.3, -0.06, math.sin(a) * 0.3)
        end = V(math.cos(a + 0.3) * 0.58, -0.07, math.sin(a + 0.3) * 0.58)
        end2 = V(math.cos(a - 0.35) * 0.55, -0.07, math.sin(a - 0.35) * 0.55)
        parts += capsule_chain([V(0, -0.05, 0), mid, end], 0.045, 2)
        parts.append(Part(lambda p, a=mid, b=end2: sd_capsule(p, a, b, 0.03), 2))
        parts.append(Part(lambda p, c=end: sd_sphere(p - c, 0.07), 3))
    parts.append(Part(lambda p: sd_sphere(p - V(0, -0.05, 0), 0.12), 2))
    return parts, mats, dict(view_pitch=0.85, view_yaw=0.5)


def _y_shape(mat_arm, mat_tip, t, flip=False, scale=1.0):
    s = -1 if flip else 1
    base = V(0, -0.55, 0) * s * scale
    hinge = V(0, -0.05, 0) * s * scale
    wob = 0.05 * math.sin(t * TAU)
    l = V(-0.42 - wob, 0.45, 0) * V(1, s, 1) * scale
    r = V(0.42 + wob, 0.45, 0) * V(1, s, 1) * scale
    parts = [Part(lambda p: sd_capsule(p, base, hinge, 0.09 * scale), mat_arm),
             Part(lambda p: sd_capsule(p, hinge, l, 0.085 * scale), mat_arm), Part(lambda p: sd_capsule(p, hinge, r, 0.085 * scale), mat_arm),
             Part(lambda p: sd_capsule(p, hinge + (l - hinge) * 0.55 + V(-0.1, 0, 0), l + V(-0.1, -0.05 * s, 0), 0.06 * scale), mat_arm),
             Part(lambda p: sd_capsule(p, hinge + (r - hinge) * 0.55 + V(0.1, 0, 0), r + V(0.1, -0.05 * s, 0), 0.06 * scale), mat_arm),
             Part(lambda p: sd_sphere(p - l, 0.1 * scale), mat_tip), Part(lambda p: sd_sphere(p - r, 0.1 * scale), mat_tip)]
    return parts


@reg('antibody')
def b_antibody(c1, c2, t):
    mats = [Material('#e0b0e0', '#fff0ff', metal=0.5), Material('#ff60e0', '#ffffff', emissive='#ff80f0', pattern=em_all())]
    return _y_shape(0, 1, t), mats, dict(view_pitch=0.2, view_yaw=0.35)


def _emblem(kind):
    def mk(k, t):
        c = V(0.38, -0.05, -0.32)
        if kind == 'fracture':
            return [Part(lambda p: np.maximum(sd_octa(p / V(0.6, 1, 0.5), 0.2) * 0.5, -p[..., 1] - 0.12), k, pos=tuple(c), rot=rotz(0.5 + 0.3 * math.sin(t * TAU))),
                    Part(lambda p: sd_octa(p / V(0.6, 1, 0.5), 0.1) * 0.5, k, pos=tuple(c + V(0.1, -0.15, 0)), rot=rotz(-0.6))]
        if kind == 'grey_goo':
            out = []
            for j in range(5):
                a = t * TAU + j * TAU / 5
                out.append(Part(lambda p, d=c + V(math.cos(a) * 0.12, 0.05 * math.sin(a * 2), math.sin(a) * 0.12): sd_box(p - d, (0.045, 0.045, 0.045)), k))
            return out
        if kind == 'rewind':
            return [Part(lambda p: sd_torus(p, 0.13, 0.035), k, pos=tuple(c), rot=rotx(1.2)),
                    Part(lambda p: sd_octa(p, 0.07), k, pos=tuple(c + V(0.13 * math.cos(-t * TAU), 0, 0.13 * math.sin(-t * TAU))))]
        if kind == 'metastasis':
            # a growth budding off seeds that drift away from it
            out = [Part(lambda p: sd_sphere(p - c, 0.08), k)]
            for j in range(4):
                a = j * TAU / 4 + 0.5
                q = (t + j * 0.25) % 1
                d = c + V(math.cos(a), 0.4 * math.sin(a * 2), math.sin(a)) * (0.06 + 0.12 * q)
                out.append(Part(lambda p, d=d, r=0.045 * (1 - 0.5 * q): sd_sphere(p - d, r), k))
            return out
        return [Part(lambda p: np.maximum(sd_torus(p, 0.13, 0.05), -sd_sphere(p, 0.1)), k, pos=tuple(c), rot=rotx(1.4 + 0.2 * math.sin(t * TAU)))]
    return mk


WARD = {'fracture': ('#40e0d0', '#80fff0'), 'grey_goo': ('#9098a8', '#e0e4f0'), 'rewind': ('#e0a030', '#ffd060'), 'hollowing': ('#5030a0', '#b080ff'),
        'metastasis': ('#d04070', '#ff90b0')}
for _w, (_liq, _em) in WARD.items():
    BUILDERS['ward_' + _w] = (lambda w, liq, em: (lambda c1, c2, t: vial(liq, '#a0a8b8', t, (_emblem(w), em))))(_w, _liq, _em)
    BUILDERS['lasting_ward_' + _w] = (lambda w, liq, em: (lambda c1, c2, t: vial(liq, '#e0b040', t, (_emblem(w), em), lasting=True)))(_w, _liq, _em)


@reg('perfused_tissue')
def b_perfused(c1, c2, t):
    flow = lambda p, n: (np.zeros(p.shape[:-1]), (np.sin((p[..., 0] - p[..., 2]) * 10 - t * TAU * 2) > 0.4).astype(float))
    mats = [Material('#d88080', '#ffd0d0', metal=0.2, pattern=shade_noise(10, 0.15)), Material('#b01818', '#ff6060', emissive='#ff3030', pattern=flow, metal=0.5)]
    slab = lambda p: sd_box(p, (0.62, 0.14, 0.5), 0.12) + 0.03 * np.sin(p[..., 0] * 9) * np.sin(p[..., 2] * 8)
    parts = [Part(slab, 0)]
    trunk = [V(-0.62, 0.17, 0.1), V(-0.2, 0.18, 0.05), V(0.15, 0.17, -0.05), V(0.6, 0.16, -0.12)]
    parts += capsule_chain(trunk, 0.06, 1)
    for a, b in ((trunk[1], V(-0.05, 0.17, 0.42)), (trunk[1], V(-0.32, 0.17, -0.4)), (trunk[2], V(0.32, 0.17, 0.38)), (trunk[2], V(0.1, 0.17, -0.42))):
        parts.append(Part(lambda p, a=a, b=b: sd_capsule(p, a, b, 0.035), 1))
    return parts, mats, dict(view_pitch=0.7, view_yaw=0.6)


@reg('grown_organ')
def b_organ(c1, c2, t):
    beat = 1 + 0.06 * max(0, math.sin(t * TAU * 2))
    mats = [Material('#b02838', '#ff8090', metal=0.4, pattern=shade_noise(11, 0.12)), Material('#d05070', '#ffc0d0', metal=0.5),
            Material('#4060c0', '#a0c0ff', metal=0.5)]
    heart = lambda p: smin(smin(sd_sphere((p - V(-0.18, 0.12, 0)) / beat, 0.32) * beat, sd_sphere((p - V(0.18, 0.12, 0)) / beat, 0.32) * beat, 0.15),
                           sd_cone_y(p - V(0, -0.22, 0), 0.05, 0.38, 0.26), 0.12)
    parts = [Part(heart, 0, rot=rotz(-0.25)),
             Part(lambda p: sd_capsule(p, V(0.05, 0.3, 0), V(0.08, 0.62, 0.02), 0.09), 1),
             Part(lambda p: sd_capsule(p, V(0.08, 0.62, 0.02), V(0.32, 0.66, 0.0), 0.08), 1),
             Part(lambda p: sd_capsule(p, V(-0.12, 0.35, 0.05), V(-0.2, 0.62, 0.08), 0.07), 2)]
    return parts, mats, dict(view_pitch=0.2, view_yaw=0.45)


@reg('circadian_culture')
def b_circadian(c1, c2, t):
    parts, mats = _petri('#d8e4ec', '#403060')
    a = t * TAU
    daynight = lambda p, n: (np.where(p[..., 0] > 0, 0.25, -0.15), (p[..., 0] > 0).astype(float))
    mats.append(Material('#5040a0', '#c0b0ff', emissive='#ffd040', pattern=daynight, metal=0.4))
    mats.append(Material('#ffffff', '#ffffff', emissive='#fff0a0', pattern=em_all()))
    parts.append(Part(lambda p: sd_sphere(p, 0.3), 2, pos=(0, 0.25, 0), rot=roty(a)))
    parts.append(Part(lambda p: sd_sphere(p, 0.05), 3, pos=(math.cos(a) * 0.48, 0.25, math.sin(a) * 0.48)))
    return parts, mats, dict(view_pitch=0.5, view_yaw=0.6)


@reg('stable_dose')
def b_stable_dose(c1, c2, t):
    mats = [Material('#c8ccd4', '#ffffff', metal=0.85), Material('#90d8b0', '#e0fff0', metal=0.4), glass('#e8f0f4')]
    parts = [Part(lambda p: sd_box(p, (0.62, 0.03, 0.42), 0.02), 0)]
    for i in range(3):
        for j in range(2):
            x, z = -0.38 + i * 0.38, -0.2 + j * 0.4
            parts.append(Part(lambda p: np.maximum(sd_sphere(p, 0.15), -p[..., 1]), 2, pos=(x, 0.02, z)))
            if not (i == 2 and j == 1):
                parts.append(Part(lambda p: sd_cyl(p, 0.11, 0.05, 0.03), 1, pos=(x, 0.06, z)))
    return parts, mats, dict(view_pitch=0.8, view_yaw=0.5)


@reg('stable_ecosystem')
def b_ecosystem(c1, c2, t):
    mats = [glass('#d0ecf0'), Material('#6a4a28', '#c09060', metal=0.2, pattern=shade_noise(12, 0.12)), Material('#3a9a40', '#a0ff90', metal=0.3),
            Material('#7a5030', '#c08050', metal=0.2), Material('#3070c0', '#a0d0ff', emissive='#80c0ff', pattern=em_bands(14, t, 0, 0.2), metal=0.7)]
    front = lambda p: sd_box(p - V(0, 0, -0.6), V(0.8, 0.8, 0.6))
    parts = [Part(lambda p: np.maximum(np.abs(sd_sphere(p, 0.62)) - 0.03, -front(p)), 0),
             Part(lambda p: sd_torus(p, 0.62, 0.04), 0, pos=(0, -0.2, 0)),
             Part(lambda p: np.maximum(sd_sphere(p, 0.6), p[..., 1] + 0.22), 1),
             Part(lambda p: sd_cyl(p, 0.18, 0.02), 4, pos=(0.22, -0.2, -0.1)),
             Part(lambda p: sd_capsule(p, V(0, -0.22, 0), V(0, 0.08, 0), 0.04), 3, pos=(-0.15, 0, 0.05))]
    sway = 0.03 * math.sin(t * TAU)
    for k, (x, y, z, r) in enumerate(((-0.15, 0.18, 0.05, 0.16), (-0.27, 0.08, 0.0, 0.11), (-0.04, 0.1, 0.12, 0.1))):
        parts.append(Part(lambda p, r=r: sd_sphere(p, r), 2, pos=(x + sway, y, z)))
    return parts, mats, dict(view_pitch=0.35, view_yaw=0.6)


@reg('symbiont_lining')
def b_symbiont(c1, c2, t):
    mats = [Material('#2a6a48', '#90e0b0', metal=0.3, pattern=em_spots(t, 0.8, 7, 14), emissive='#a0ffc0'), Material('#c0c8d0', '#ffffff', metal=0.85)]
    sheet = lambda p: np.maximum(np.abs(sd_sphere(p - V(0, -1.4, 0), 1.75)) - 0.07, sd_box(p, (0.62, 0.6, 0.48)))
    parts = [Part(sheet, 0), Part(lambda p: sd_box(p, (0.64, 0.03, 0.05), 0.01), 1, pos=(0, 0.26, 0.5)),
             Part(lambda p: sd_box(p, (0.64, 0.03, 0.05), 0.01), 1, pos=(0, 0.26, -0.5))]
    return parts, mats, dict(view_pitch=0.55, view_yaw=0.6)


@reg('evolved_strain')
def b_evolved(c1, c2, t):
    lvl_cut = lambda p: -p[..., 2] - 0.02        # the glass is open toward the viewer, so the banded culture shows
    bands = lambda p, n: (np.zeros(p.shape[:-1]), (np.sin(p[..., 1] * 18 + t * TAU) > 0.2).astype(float))
    mats = [glass('#dce8ee'), Material('#a06a30', '#ffd090', emissive='#ffb050', pattern=bands, metal=0.4), Material('#505868', '#a0a8b8', metal=0.8),
            Material('#e04040', '#ffa0a0', metal=0.6)]
    parts = []
    for k, (x, lvl, col) in enumerate(((-0.3, 0.1, 1), (0.0, 0.35, 1), (0.3, -0.05, 1))):
        parts.append(Part(lambda p: np.maximum(np.abs(sd_capsule(p, V(0, -0.45, 0), V(0, 0.4, 0), 0.11)) - 0.015,
                                               np.maximum(p[..., 1] - 0.5, lvl_cut(p))), 0, pos=(x, 0, 0)))
        parts.append(Part(lambda p, lvl=lvl: np.maximum(sd_capsule(p, V(0, -0.45, 0), V(0, 0.4, 0), 0.1), p[..., 1] - lvl), col, pos=(x, 0, 0)))
    parts.append(Part(lambda p: np.maximum(sd_box(p, (0.5, 0.05, 0.18), 0.02), -sd_cyl(rep_xz(p, 0.3) * V(1, 1, 0) + V(0, 0, 0), 0.12, 0.2)), 2, pos=(0, 0.1, 0)))
    parts.append(Part(lambda p: sd_box(p, (0.5, 0.04, 0.18), 0.02), 2, pos=(0, -0.55, 0)))
    parts.append(Part(lambda p: sd_cyl(p, 0.12, 0.04), 3, pos=(0.0, 0.47, 0)))
    return parts, mats, dict(view_pitch=0.25, view_yaw=0.5)


@reg('gene_circuit')
def b_gene_circuit(c1, c2, t):
    mats = [Material('#203048', '#6080c0', metal=0.6), Material('#4080ff', '#c0d8ff', metal=0.5), Material('#ff6080', '#ffd0e0', metal=0.5),
            Material('#80ffd0', '#ffffff', emissive='#80ffe0', pattern=em_all())]
    parts = [Part(lambda p: sd_box(p, (0.62, 0.05, 0.48), 0.02), 0, pos=(0, -0.45, 0))]
    h1 = lambda u: V(0.18 * math.cos(u * TAU * 1.5), -0.38 + u * 0.95, 0.18 * math.sin(u * TAU * 1.5))
    h2 = lambda u: V(0.18 * math.cos(u * TAU * 1.5 + math.pi), -0.38 + u * 0.95, 0.18 * math.sin(u * TAU * 1.5 + math.pi))
    parts += tube(h1, 14, 0.045, 1) + tube(h2, 14, 0.045, 2)
    lit = int(t * 7) % 7
    for k in range(7):
        u = (k + 0.5) / 7
        parts.append(Part(lambda p, a=h1(u), b=h2(u): sd_capsule(p, a, b, 0.025), 3 if k == lit else 0))
    for x in (-0.45, 0.45):
        parts.append(Part(lambda p: sd_box(p, (0.08, 0.04, 0.08), 0.01), 3, pos=(x, -0.37, 0.25)))
    return parts, mats, dict(view_pitch=0.3, view_yaw=0.6)


@reg('living_catalyst')
def b_living_catalyst(c1, c2, t):
    mats = [Material('#2a8a3c', '#a0ffa0', metal=0.4, pattern=shade_noise(8, 0.2, 3)), Material('#ffe060', '#ffffff', emissive='#ffe060', pattern=em_all()),
            Material('#60ff90', '#ffffff', emissive='#80ffa0', pattern=em_bands(8, t, 0, 0.25), metal=0.6)]
    blob = lambda p: np.maximum(sd_sphere(p, 0.48) + 0.06 * np.sin(p[..., 0] * 6) * np.sin(p[..., 1] * 5 + 1) * np.sin(p[..., 2] * 7),
                                -sd_sphere(p - V(0.3, 0.32, -0.3), 0.2))
    a = t * TAU
    s = 0.5 + 0.5 * math.cos(a)
    parts = [Part(blob, 0), Part(lambda p: sd_sphere(p, 0.09), 1, pos=(0.3 + 0.3 * s, 0.32 + 0.25 * s, -0.3 - 0.2 * s)),
             Part(lambda p: sd_torus(p, 0.64, 0.035), 2, rot=rotx(1.1) @ rotz(a))]
    return parts, mats, dict(view_pitch=0.35, view_yaw=0.6)


@reg('mirror_cell')
def b_mirror_cell(c1, c2, t):
    """One cell, cut through by a mirror: the far half is its own reflection, built the other way round."""
    parts, mats = _cell_cutaway('#b8b8d0', '#eeeef8', t, nucleus='#6070b0')
    mats.append(Material('#d0d8f0', '#ffffff', emissive='#e0e8ff', pattern=em_bands(10, t, 1, 0.15), metal=0.95))
    parts.append(Part(lambda p: np.maximum(sd_box(p, (0.015, 0.8, 0.8), 0.004), sd_sphere(p, 0.7)), len(mats) - 1, rot=roty(0.5)))
    return parts, mats, dict(view_pitch=0.4, view_yaw=0.75)


@reg('mirror_antibody')
def b_mirror_antibody(c1, c2, t):
    """A silver antibody above a mirror, and its reflection the other way up beneath it."""
    mats = [Material('#d8dce8', '#ffffff', metal=0.95), Material('#b0c0ff', '#ffffff', emissive='#c0d0ff', pattern=em_all()),
            Material('#a0a8c0', '#ffffff', metal=0.95, pattern=em_bands(10, t, 0, 0.12), emissive='#e0e8ff'), Material('#505870', '#9098b0', metal=0.8)]
    up = [Part(q.fn, q.mat, pos=tuple(q.pos + V(0, 0.36, 0)), rot=q.rot) for q in _y_shape(0, 1, t, scale=0.66)]
    down = [Part(q.fn, 3, pos=tuple(q.pos + V(0, -0.36, 0)), rot=q.rot) for q in _y_shape(0, 1, t, flip=True, scale=0.4)]
    parts = up + down + [Part(lambda p: sd_cyl(p, 0.5, 0.015, 0.005), 2, pos=(0, -0.02, 0))]
    return parts, mats, dict(view_pitch=0.3, view_yaw=0.35)


@reg('tree_of_life')
def b_tree_of_life(c1, c2, t):
    mats = [Material('#6a4020', '#c08850', metal=0.3, pattern=shade_noise(14, 0.12)),
            Material('#40c060', '#d0ffc0', emissive='#a0ff80', pattern=em_spots(t, 0.55, 2, 10), metal=0.4),
            Material('#ffe080', '#ffffff', emissive='#ffe080', pattern=em_all())]
    parts = [Part(lambda p: sd_cone_y(p, 0.12, 0.07, 0.3), 0, pos=(0, -0.3, 0))]
    top = V(0, 0.0, 0)
    for k in range(3):
        a = k * TAU / 3 + 0.4
        b1 = top + V(math.cos(a) * 0.3, 0.3, math.sin(a) * 0.3)
        parts.append(Part(lambda p, b=b1: sd_capsule(p, top, b, 0.055), 0))
        for j in (-0.6, 0.6):
            b2 = b1 + V(math.cos(a + j) * 0.2, 0.18, math.sin(a + j) * 0.2)
            parts.append(Part(lambda p, a_=b1, b=b2: sd_capsule(p, a_, b, 0.03), 0))
            parts.append(Part(lambda p, c=b2: sd_sphere(p - c, 0.17), 1))
    for k in range(4):
        a = k * TAU / 4
        parts += capsule_chain([V(0, -0.58, 0), V(math.cos(a) * 0.28, -0.66, math.sin(a) * 0.28), V(math.cos(a) * 0.48, -0.62, math.sin(a) * 0.48)], 0.035, 0)
    g = 0.04 + 0.02 * math.sin(t * TAU)
    parts.append(Part(lambda p: sd_sphere(p, g + 0.06), 2, pos=(0, 0.42, 0)))
    return parts, mats, dict(view_pitch=0.25, view_yaw=0.6, zoom=0.95)


MOTIONS.update({i: (('spin',), ('glint',)) for i in ('living_cell', 'algae_culture', 'bacteriophage', 'antibody', 'tree_of_life', 'living_catalyst',
                                                     'gene_circuit', 'circadian_culture')})
MOTIONS.update({i: (('rock',), ()) for i in ('bacterial_culture', 'mould_culture', 'physarum_culture', 'perfused_tissue', 'stable_dose', 'sterile_pack',
                                             'lipid_bilayer', 'ion_channel_membrane', 'symbiont_lining', 'mirror_cell', 'mirror_antibody')})
MOTIONS['grown_organ'] = (('rock',), ())


# =============================================================================== 1.2 Chemistry
def _heap(col, acc, t, shape='sphere', n=14, seed=1, r=0.13, spread=0.42, h=0.35, flat=1.0, glow=None):
    """A little heap of pellets, prills, flakes or grains."""
    rng = np.random.default_rng(seed)
    mats = [Material(col, acc, metal=0.35, emissive=glow, pattern=em_spots(t, 0.9, seed) if glow else shade_noise(12, 0.1, seed))]
    parts = []
    for k in range(n):
        a, rr = rng.uniform(0, TAU), spread * math.sqrt(rng.uniform(0, 1))
        y = -0.35 + h * (1 - rr / spread) + rng.uniform(-0.04, 0.04)
        c = V(math.cos(a) * rr, y, math.sin(a) * rr)
        rot = rotx(rng.uniform(-1, 1)) @ rotz(rng.uniform(-1, 1))
        if shape == 'sphere':
            parts.append(Part(lambda p, c=c: sd_sphere(p - c, r), 0))
        elif shape == 'flake':
            parts.append(Part(lambda p: sd_ellipsoid(p, (r * 1.3, r * 0.45 * flat, r * 1.3)), 0, pos=tuple(c), rot=rot))
        elif shape == 'cube':
            parts.append(Part(lambda p: sd_box(p, (r, r, r), 0.02), 0, pos=tuple(c), rot=rot))
    return parts, mats


def _bowl(col='#a0a8b8', r=0.6, depth=0.25, y=-0.38):
    m = Material(col, '#ffffff', metal=0.6)
    f = lambda p: np.maximum(np.abs(sd_sphere(p - V(0, depth + 0.35, 0), 0.7)) - 0.04, p[..., 1] - 0.0)
    return Part(lambda p: np.maximum(f(p), -sd_box(p - V(0, -0.5, 0), (2, 0.0, 2))), 0, pos=(0, y, 0)), m


def _powder_bowl(powder, acc, t, bowl='#a8a0a0', seed=2):
    """A mound of powder in a shallow dish."""
    mats = [Material(bowl, '#ffffff', metal=0.6), Material(powder, acc, metal=0.15, pattern=shade_noise(20, 0.08, seed))]
    dish = lambda p: np.maximum(np.maximum(np.abs(sd_sphere(p - V(0, 0.55, 0), 0.85)) - 0.04, p[..., 1] - 0.05), -0.32 - p[..., 1])
    mound = lambda p: np.maximum(sd_ellipsoid(p - V(0, -0.12, 0), (0.55, 0.3, 0.55)), -0.25 - p[..., 1])
    return [Part(dish, 0, pos=(0, -0.1, 0)), Part(mound, 1, pos=(0, -0.05, 0))], mats


def _bottle(liquid, label, cap, t, glow=False, body=0.3):
    mats = [Material(liquid, '#ffffff', metal=0.85, emissive=liquid if glow else None, pattern=em_bands(9, t, 1, 0.2) if glow else None),
            Material(label, '#ffffff', metal=0.1), Material(cap, '#ffffff', metal=0.7)]
    parts = [Part(lambda p: sd_cyl(p, body, 0.36, 0.08), 0, pos=(0, -0.22, 0)),
             Part(lambda p: sd_cone_y(p, body * 0.95, 0.13, 0.12), 0, pos=(0, 0.25, 0)),
             Part(lambda p: sd_cyl(p, body + 0.012, 0.16, 0.0), 1, pos=(0, -0.24, 0)),
             Part(lambda p: sd_cyl(p, 0.15, 0.1, 0.03), 2, pos=(0, 0.45, 0))]
    return parts, mats


def _card(col, acc, t, deco):
    mats = [Material(col, acc, metal=0.2), Material(acc, '#ffffff', emissive=acc, pattern=em_all(), metal=0.3)]
    parts = [Part(lambda p: sd_box(p, (0.5, 0.03, 0.62), 0.03), 0)] + deco(1, t)
    return parts, mats, dict(view_pitch=0.85, view_yaw=0.55)


@reg('lead_pig')
def b_lead_pig(c1, c2, t):
    trefoil = lambda p, n: (np.zeros(p.shape[:-1]), ((np.abs(p[..., 1] + 0.05) < 0.14) & (np.cos(np.arctan2(p[..., 2], p[..., 0]) * 3) > 0.4)).astype(float))
    mats = [Material('#4c5060', '#a0a8b8', metal=0.55, pattern=shade_noise(8, 0.12)), Material('#e0c020', '#ffffff', emissive='#ffd020', pattern=trefoil, metal=0.4),
            Material('#686c78', '#c0c8d0', metal=0.7)]
    parts = [Part(lambda p: sd_cyl(p, 0.48, 0.34, 0.08), 0, pos=(0, -0.2, 0)), Part(lambda p: sd_cyl(p, 0.495, 0.12, 0.0), 1, pos=(0, -0.2, 0)),
             Part(lambda p: sd_cyl(p, 0.5, 0.08, 0.04), 2, pos=(0, 0.2, 0)), Part(lambda p: sd_capsule(p, V(-0.12, 0, 0), V(0.12, 0, 0), 0.07), 2, pos=(0, 0.33, 0))]
    return parts, mats, dict(view_pitch=0.45, view_yaw=0.6)


@reg('iron_catalyst')
def b_iron_catalyst(c1, c2, t):
    pores = lambda p: sd_sphere(rep_xz(p, 0.2) * V(1, 0, 1) + V(0, np.mod(p[..., 1] + 0.1, 0.2) - 0.1, 0)[None] * 0, 0.06)
    def sponge(p, r):
        q = p.copy()
        for ax in range(3):
            q[..., ax] = np.mod(p[..., ax] + 0.09, 0.18) - 0.09
        return np.maximum(sd_sphere(p, r) + 0.03 * np.sin(p[..., 0] * 13), -sd_sphere(q, 0.07))
    mats = [Material('#5a4a3a', '#c8a888', metal=0.5)]
    parts = [Part(lambda p: sponge(p, 0.36), 0, pos=(-0.15, 0.05, 0)), Part(lambda p: sponge(p, 0.26), 0, pos=(0.32, -0.25, 0.1)),
             Part(lambda p: sponge(p, 0.22), 0, pos=(-0.25, -0.38, -0.2))]
    return parts, mats, dict(view_pitch=0.4, view_yaw=0.6)


@reg('vanadium_bed')
def b_vanadium(c1, c2, t):
    mats = [Material('#a87820', '#ffd060', metal=0.4, pattern=shade_noise(10, 0.1))]
    ring = lambda p: np.maximum(sd_cyl(p, 0.17, 0.17, 0.02), -sd_cyl(p, 0.1, 0.3))
    parts = []
    for k, (x, y, z, a, b) in enumerate(((-0.25, 0.15, 0, 0.3, 0.2), (0.25, 0.1, 0.1, 1.4, 0.5), (0, -0.25, -0.1, 0.8, 1.2), (0.32, -0.35, 0.2, 0.2, 0.9),
                                          (-0.35, -0.3, 0.25, 1.2, 0.1))):
        parts.append(Part(ring, 0, pos=(x, y, z), rot=rotx(a) @ rotz(b)))
    return parts, mats, dict(view_pitch=0.45, view_yaw=0.6)


@reg('platinum_rhodium_gauze')
def b_gauze(c1, c2, t):
    hot = lambda p, n: (np.zeros(p.shape[:-1]), (np.sqrt(p[..., 0] ** 2 + p[..., 2] ** 2) < 0.28 + 0.06 * math.sin(t * TAU)).astype(float))
    def mesh(p):
        q = rep_xz(p, 0.16)
        wx = sd_capsule(q * V(0, 1, 1), V(0, 0, 0), V(0, 0, 0), 0.025)
        a = np.sqrt(q[..., 1] ** 2 + q[..., 2] ** 2) - 0.022
        b = np.sqrt(q[..., 1] ** 2 + q[..., 0] ** 2) - 0.022
        return np.maximum(np.minimum(a, b), sd_cyl(p, 0.62, 0.1))
    mats = [Material('#a0a0a8', '#ffffff', emissive='#ff9030', pattern=hot, metal=0.95), Material('#808088', '#e0e0e8', metal=0.9)]
    parts = [Part(mesh, 0), Part(lambda p: sd_torus(p, 0.63, 0.04), 1)]
    return parts, mats, dict(view_pitch=0.8, view_yaw=0.6)


@reg('halite')
def b_halite(c1, c2, t):
    mats = [Material('#d8dce4', '#ffffff', metal=0.8, pattern=em_spots(t, 0.95), emissive='#ffffff')]
    parts = [Part(lambda p: sd_box(p, (0.28, 0.28, 0.28), 0.015), 0, pos=(-0.1, 0.05, 0), rot=roty(0.3) @ rotx(0.1)),
             Part(lambda p: sd_box(p, (0.2, 0.2, 0.2), 0.015), 0, pos=(0.32, -0.25, 0.05), rot=roty(0.9) @ rotz(0.2)),
             Part(lambda p: sd_box(p, (0.15, 0.15, 0.15), 0.015), 0, pos=(-0.35, -0.35, 0.2), rot=roty(-0.4)),
             Part(lambda p: sd_box(p, (0.12, 0.12, 0.12), 0.015), 0, pos=(0.12, 0.38, -0.1), rot=roty(0.7) @ rotz(0.5))]
    return parts, mats, dict(view_pitch=0.4, view_yaw=0.6)


@reg('gypsum_crystal')
def b_gypsum(c1, c2, t):
    """A desert rose: thin blades of gypsum in a rosette."""
    mats = [Material('#e0d4c0', '#fff8ee', metal=0.5, pattern=shade_noise(9, 0.08))]
    parts = []
    for k in range(7):
        a = k * TAU / 7
        parts.append(Part(lambda p: sd_ellipsoid(p, (0.34, 0.3, 0.04)), 0, pos=(math.cos(a) * 0.22, -0.05 + 0.08 * (k % 2), math.sin(a) * 0.22),
                          rot=roty(-a) @ rotx(0.5)))
    parts.append(Part(lambda p: sd_ellipsoid(p, (0.3, 0.26, 0.04)), 0, pos=(0, 0.2, 0), rot=rotx(1.3)))
    return parts, mats, dict(view_pitch=0.45, view_yaw=0.6)


@reg('lithium_carbonate')
def b_lithium(c1, c2, t):
    parts, mats = _powder_bowl('#e8e8f0', '#ffffff', t, bowl='#6a8090', seed=3)
    return parts, mats, dict(view_pitch=0.55, view_yaw=0.6)


@reg('prussian_blue_pigment')
def b_prussian(c1, c2, t):
    parts, mats = _powder_bowl('#1a3090', '#6080ff', t, bowl='#c8c0b8', seed=4)
    mats.append(Material('#c8c0b8', '#ffffff', metal=0.5))
    parts.append(Part(lambda p: sd_capsule(p, V(0, -0.1, 0), V(0, 0.45, 0), 0.07), 2, pos=(0.15, 0.05, 0.05), rot=rotz(-0.6)))
    return parts, mats, dict(view_pitch=0.5, view_yaw=0.6)


@reg('alumina')
def b_alumina(c1, c2, t):
    mats = [Material('#c0b8a8', '#f0e8d8', metal=0.3), Material('#f0f0f4', '#ffffff', metal=0.15, pattern=shade_noise(20, 0.08, 5))]
    crucible = lambda p: np.maximum(np.maximum(sd_cone_y(p, 0.32, 0.45, 0.32), -sd_cone_y(p - V(0, 0.06, 0), 0.26, 0.39, 0.32)), 0)
    parts = [Part(lambda p: np.maximum(sd_cyl(p, 0.42, 0.3, 0.04), -sd_cyl(p - V(0, 0.08, 0), 0.35, 0.3)), 0, pos=(0, -0.2, 0)),
             Part(lambda p: np.maximum(sd_ellipsoid(p, (0.38, 0.2, 0.38)), -p[..., 1] - 0.02), 1, pos=(0, 0.02, 0))]
    return parts, mats, dict(view_pitch=0.5, view_yaw=0.6)


@reg('thermochemical_data_card')
def b_thermo_card(c1, c2, t):
    def deco(k, t):
        pts = [V(-0.38 + 0.76 * u, 0.045, -0.3 + 0.5 * (u ** 0.5) + 0.04 * math.sin(u * 20)) for u in np.linspace(0, 1, 9)]
        out = capsule_chain(pts, 0.025, k)
        out.append(Part(lambda p: sd_cone_y(p, 0.09, 0.0, 0.12), k, pos=(-0.25, 0.12, 0.4)))
        return out
    return _card('#e8dcc0', '#ff7020', t, deco)


@reg('structure_card')
def b_structure_card(c1, c2, t):
    def deco(k, t):
        out = []
        for i, h in enumerate((0.05, 0.3, 0.08, 0.5, 0.12, 0.22, 0.04, 0.38)):
            x = -0.4 + i * 0.115
            out.append(Part(lambda p, h=h: sd_box(p, (0.035, h * 0.5, 0.035)), k, pos=(x, 0.03 + h * 0.5, 0.1)))
        out.append(Part(lambda p: sd_hexprism(p, 0.12, 0.02), k, pos=(-0.22, 0.04, -0.35)))
        return out
    return _card('#202838', '#80b0ff', t, deco)


@reg('calibration_card')
def b_calibration_card(c1, c2, t):
    def deco(k, t):
        out = []
        rng = np.random.default_rng(9)
        for i in range(6):
            for j in range(8):
                if rng.random() < 0.4:
                    out.append(Part(lambda p: sd_box(p, (0.035, 0.04, 0.025)), k, pos=(-0.36 + i * 0.14, 0.0, -0.5 + j * 0.14)))
        return out
    return _card('#c8b890', '#80e0ff', t, deco)


@reg('ammonium_nitrate')
def b_ammonium_nitrate(c1, c2, t):
    parts, mats = _heap('#ecece4', '#ffffff', t, n=18, seed=6, r=0.11)
    mats.append(Material('#c8b080', '#ffffff', metal=0.1))
    parts.append(Part(lambda p: np.maximum(np.abs(sd_cyl(p, 0.62, 0.12)) - 0.02, -p[..., 1]), 1, pos=(0, -0.4, 0)))
    return parts, mats, dict(view_pitch=0.5, view_yaw=0.6)


@reg('caustic_soda')
def b_caustic(c1, c2, t):
    parts, mats = _heap('#f0f0f8', '#ffffff', t, shape='flake', n=16, seed=7, r=0.12, spread=0.55, h=0.2)
    return parts, mats, dict(view_pitch=0.55, view_yaw=0.6)


@reg('nickel_plated_iron')
def b_nickel_plated(c1, c2, t):
    """A bar of iron sawn through: the thin bright nickel skin shows round the dull core."""
    ing = lambda p: np.maximum(sd_box(p, (0.7, 0.24, 0.36), 0.04), p[..., 0] - 0.55)
    mats = [Material('#c8d0d8', '#ffffff', metal=0.95), Material('#5a5a60', '#a0a0a8', metal=0.6)]
    parts = [Part(lambda p: np.maximum(ing(p), -sd_box(p - V(0.6, 0, 0), (0.08, 0.2, 0.32))), 0, rot=roty(0.0)),
             Part(lambda p: np.maximum(sd_box(p, (0.62, 0.205, 0.325), 0.02), p[..., 0] - 0.55), 1)]
    return parts, mats, dict(view_pitch=0.5, view_yaw=-0.5)


@reg('pah_soot')
def b_pah(c1, c2, t):
    mats = [Material('#282828', '#706860', metal=0.3, keep_dark=True), Material('#403830', '#a09080', metal=0.3)]
    parts = []
    rng = np.random.default_rng(3)
    for k in range(7):
        c = V(*rng.uniform(-0.35, 0.35, 3))
        parts.append(Part(lambda p: np.maximum(sd_hexprism(p, 0.2, 0.03), -sd_hexprism(p, 0.09, 0.1)), k % 2, pos=tuple(c),
                          rot=rotx(rng.uniform(0, 3)) @ rotz(rng.uniform(0, 3))))
    return parts, mats, dict(view_pitch=0.4, view_yaw=0.6)


@reg('amino_acid_mixture')
def b_amino(c1, c2, t):
    cols = ['#ff6060', '#60a0ff', '#ffd040', '#60e070', '#e080ff']
    mats = [glass('#dce8ec')] + [Material(c, '#ffffff', metal=0.5) for c in cols]
    parts = [Part(lambda p: np.maximum(np.maximum(np.abs(sd_cyl(p, 0.42, 0.4, 0.06)) - 0.02, p[..., 1] - 0.3), -p[..., 2] - 0.1), 0)]
    rng = np.random.default_rng(2)
    for k in range(16):
        a, r = rng.uniform(0, TAU), rng.uniform(0, 0.32)
        parts.append(Part(lambda p, c=V(math.cos(a) * r, rng.uniform(-0.35, 0.2), math.sin(a) * r): sd_sphere(p - c, 0.09), 1 + k % 5))
    return parts, mats, dict(view_pitch=0.35, view_yaw=0.6)


@reg('nickel_catalyst')
def b_nickel_catalyst(c1, c2, t):
    def sponge(p):
        q = p.copy()
        for ax in range(3):
            q[..., ax] = np.mod(p[..., ax] + 0.11, 0.22) - 0.11
        return np.maximum(sd_box(p, (0.4, 0.4, 0.4), 0.06), -sd_sphere(q, 0.085))
    mats = [Material('#707a70', '#d0e0d0', metal=0.8)]
    return [Part(sponge, 0, rot=roty(0.3) @ rotx(0.2))], mats, dict(view_pitch=0.45, view_yaw=0.6)


@reg('polymer_sheet')
def b_polymer(c1, c2, t):
    mats = [Material('#d8e0e8', '#ffffff', metal=0.6), Material('#c0ccd8', '#ffffff', metal=0.6)]
    parts = [Part(lambda p: np.maximum(sd_cyl(p, 0.24, 0.55, 0.02), -sd_cyl(p, 0.08, 0.7)), 0, pos=(-0.25, 0.0, 0), rot=rotx(math.pi / 2)),
             Part(lambda p: sd_box(p, (0.42, 0.015, 0.55), 0.005), 1, pos=(0.2, -0.24, 0))]
    return parts, mats, dict(view_pitch=0.45, view_yaw=0.4)


def _spool(thread, t, flange='#8a6038'):
    bands = lambda p, n: (np.sin(p[..., 1] * 60) * 0.06, np.zeros(p.shape[:-1]))
    mats = [Material(flange, '#e0b080', metal=0.3), Material(thread, '#ffffff', metal=0.3, pattern=bands)]
    parts = [Part(lambda p: sd_cyl(p, 0.45, 0.05, 0.02), 0, pos=(0, 0.42, 0)), Part(lambda p: sd_cyl(p, 0.45, 0.05, 0.02), 0, pos=(0, -0.42, 0)),
             Part(lambda p: sd_cyl(p, 0.34, 0.38), 1)]
    a = t * TAU
    parts.append(Part(lambda p: sd_capsule(p, V(0.34, 0.1, 0), V(0.62, -0.4, 0.2), 0.025), 1, rot=roty(a)))
    return parts, mats


@reg('nylon_fibre')
def b_nylon(c1, c2, t):
    parts, mats = _spool('#f0ece0', t)
    return parts, mats, dict(view_pitch=0.35, view_yaw=0.6)


@reg('aramid_fibre')
def b_aramid(c1, c2, t):
    weave = lambda p, n: ((np.sign(np.sin(p[..., 0] * 18) * np.sin(p[..., 2] * 18)) * 0.12), np.zeros(p.shape[:-1]))
    mats = [Material('#d0a820', '#fff080', metal=0.5, pattern=weave)]
    cloth = lambda p: sd_box(p, (0.62, 0.035, 0.55), 0.01) + 0.0
    fold = lambda p: np.maximum(np.abs(sd_cyl(p, 0.2, 0.55)) - 0.035, p[..., 0])
    parts = [Part(lambda p: sd_box(p, (0.5, 0.035, 0.52), 0.01), 0, pos=(0.05, -0.2, 0)),
             Part(lambda p: sd_box(p, (0.5, 0.035, 0.52), 0.01), 0, pos=(0.05, 0.2, 0)),
             Part(fold, 0, pos=(-0.45, 0, 0), rot=rotx(math.pi / 2))]
    return parts, mats, dict(view_pitch=0.5, view_yaw=0.6)


@reg('fine_reagent')
def b_fine_reagent(c1, c2, t):
    parts, mats = _bottle('#7a4010', '#f0ece0', '#303038', t)
    mats.append(Material('#c0a0ff', '#ffffff', emissive='#c0a0ff', pattern=em_all()))
    parts.append(Part(lambda p: sd_box(p, (0.06, 0.06, 0.02)), 3, pos=(0, -0.22, -0.32)))
    return parts, mats, dict(view_pitch=0.25, view_yaw=0.5)


@reg('enantiopure_reagent')
def b_enantiopure(c1, c2, t):
    parts, mats = _bottle('#30a090', '#f0ece0', '#c0c8d0', t, glow=True)
    mats.append(Material('#ffffff', '#ffffff', emissive='#e0fff8', pattern=em_all()))
    hel = lambda u: V(0.42 + 0.1 * math.cos(u * TAU * 2.5 + t * TAU), -0.4 + 0.8 * u, -0.25 + 0.1 * math.sin(u * TAU * 2.5 + t * TAU))
    parts += tube(hel, 14, 0.03, 3)
    return parts, mats, dict(view_pitch=0.25, view_yaw=0.5)


@reg('mof_gas_sorbent')
def b_mof(c1, c2, t):
    mats = [Material('#5a7080', '#c0e0f0', metal=0.7), Material('#60c0e0', '#ffffff', metal=0.7),
            Material('#80e0ff', '#ffffff', emissive='#a0f0ff', pattern=em_all())]
    def frame(p):
        q = np.abs(p)
        e = 0.45
        d = 9.0
        for a, b, c in ((0, 1, 2), (1, 2, 0), (2, 0, 1)):
            d = np.minimum(d, np.sqrt((q[..., a] - e) ** 2 + (q[..., b] - e) ** 2) - 0.04 + np.maximum(q[..., c] - e, 0))
        return d
    parts = [Part(frame, 0, rot=roty(0.4))]
    for sx in (-1, 1):
        for sy in (-1, 1):
            for sz in (-1, 1):
                parts.append(Part(lambda p: sd_sphere(p, 0.1), 1, pos=tuple(rotx(0) @ (roty(0.4).T @ V(sx, sy, sz) * 0.45))))
    s = 0.16 + 0.02 * math.sin(t * TAU)
    parts.append(Part(lambda p: sd_sphere(p, s), 2))
    return parts, mats, dict(view_pitch=0.45, view_yaw=0.6)


@reg('silica_aerogel')
def b_aerogel(c1, c2, t):
    haze = lambda p, n: (np.zeros(p.shape[:-1]), (np.sin(p[..., 0] * 9 + p[..., 1] * 7 - t * TAU) > 0.7).astype(float))
    mats = [Material('#a8c8e8', '#ffffff', emissive='#d8ecff', pattern=haze, metal=0.3), Material('#e060a0', '#ffd0e8', metal=0.3),
            Material('#40a040', '#a0ff90', metal=0.3)]
    parts = [Part(lambda p: sd_box(p, (0.42, 0.24, 0.32), 0.07), 0, pos=(0, -0.2, 0))]
    for k in range(5):
        a = k * TAU / 5
        parts.append(Part(lambda p: sd_ellipsoid(p, (0.1, 0.03, 0.05)), 1, pos=(math.cos(a) * 0.08, 0.15, math.sin(a) * 0.08), rot=roty(-a)))
    parts.append(Part(lambda p: sd_capsule(p, V(0, 0.05, 0), V(0.0, 0.15, 0), 0.025), 2))
    return parts, mats, dict(view_pitch=0.45, view_yaw=0.6)


@reg('quasicrystal_plate')
def b_quasicrystal(c1, c2, t):
    def penrose(p, n):
        v = 0
        for k in range(5):
            a = k * math.pi / 5
            v = v + np.cos((p[..., 0] * math.cos(a) + p[..., 2] * math.sin(a)) * 16)
        return np.zeros(v.shape), (v > 0.8).astype(float)
    decagon = lambda p: np.maximum(np.max(np.stack([p[..., 0] * math.cos(k * math.pi / 5) + p[..., 2] * math.sin(k * math.pi / 5) for k in range(10)], -1), -1) - 0.58,
                                   np.abs(p[..., 1]) - 0.07)
    mats = [Material('#3a2c20', '#ffe0b0', emissive='#ffc040', pattern=penrose, metal=0.85)]
    return [Part(decagon, 0)], mats, dict(view_pitch=0.8, view_yaw=0.6)


@reg('aluminium_ingot')
def b_aluminium(c1, c2, t):
    brushed = lambda p, n: (np.sin(p[..., 0] * 70) * 0.05, np.zeros(p.shape[:-1]))
    ing = lambda p: np.maximum(sd_box(p, (0.7, 0.16, 0.34), 0.03), (p[..., 1] * 0.95 + np.abs(p[..., 2]) * 0.5 - 0.24))
    mats = [Material('#b8c0c8', '#ffffff', metal=0.95, pattern=brushed)]
    parts = [Part(ing, 0, pos=(0, -0.22, 0.12)), Part(ing, 0, pos=(0.05, 0.12, -0.12), rot=roty(0.25))]
    return parts, mats, dict(view_pitch=0.55, view_yaw=0.65)


def _molecule(center, ligand, t, arms, centre_r=0.2, lig_r=0.12, bond='#c0c8d0', glow_centre=False):
    mats = [Material(center, '#ffffff', metal=0.7, emissive=center if glow_centre else None, pattern=em_all() if glow_centre else None),
            Material(ligand, '#ffffff', metal=0.5), Material(bond, '#ffffff', metal=0.6)]
    parts = [Part(lambda p: sd_sphere(p, centre_r), 0)]
    for v in arms:
        v = V(*v)
        parts.append(Part(lambda p, v=v: sd_capsule(p, V(0, 0, 0), v, 0.04), 2))
        parts.append(Part(lambda p, v=v: sd_sphere(p - v, lig_r), 1))
    return parts, mats


@reg('copper_ammine_complex')
def b_copper_ammine(c1, c2, t):
    arms = [(0.5 * math.cos(k * TAU / 4), 0, 0.5 * math.sin(k * TAU / 4)) for k in range(4)] + [(0, 0.42, 0), (0, -0.42, 0)]
    parts, mats = _molecule('#2060d0', '#a0c8ff', t, arms, glow_centre=True)
    return parts, mats, dict(view_pitch=0.4, view_yaw=0.6 + t * TAU)


@reg('edta')
def b_edta(c1, c2, t):
    """A claw of a molecule: six arms closing round a metal ion."""
    mats = [Material('#ffc040', '#ffffff', emissive='#ffd060', pattern=em_all()), Material('#d0d0d8', '#ffffff', metal=0.5), Material('#ff6060', '#ffffff', metal=0.5)]
    parts = [Part(lambda p: sd_sphere(p, 0.15), 0)]
    for k in range(6):
        a = k * TAU / 6
        base = V(math.cos(a) * 0.55, -0.35, math.sin(a) * 0.55)
        mid = V(math.cos(a) * 0.5, 0.1, math.sin(a) * 0.5)
        tip = V(math.cos(a) * 0.22, 0.12 * (1 if k % 2 else -1), math.sin(a) * 0.22)
        parts += capsule_chain([base, mid, tip], 0.045, 1)
        parts.append(Part(lambda p, c=tip: sd_sphere(p - c, 0.07), 2))
    return parts, mats, dict(view_pitch=0.4, view_yaw=0.6)


@reg('porphyrin_oxygen_carrier')
def b_porphyrin(c1, c2, t):
    mats = [Material('#a02020', '#ff8080', metal=0.5), Material('#ff6040', '#ffffff', emissive='#ff7040', pattern=em_all()),
            Material('#e0e0f0', '#ffffff', metal=0.5)]
    parts = [Part(lambda p: sd_torus(p, 0.42, 0.05), 0), Part(lambda p: sd_sphere(p, 0.12), 1)]
    for k in range(4):
        a = k * TAU / 4 + math.pi / 4
        parts.append(Part(lambda p: np.maximum(sd_cyl(p, 0.14, 0.04), -sd_cyl(p, 0.07, 0.1)), 0, pos=(math.cos(a) * 0.42, 0, math.sin(a) * 0.42)))
        parts.append(Part(lambda p, a=a: sd_capsule(p, V(0, 0, 0), V(math.cos(a) * 0.34, 0, math.sin(a) * 0.34), 0.03), 0))
    bob = 0.3 + 0.05 * math.sin(t * TAU)
    parts.append(Part(lambda p: sd_sphere(p - V(-0.06, bob, 0), 0.09), 2))
    parts.append(Part(lambda p: sd_sphere(p - V(0.08, bob + 0.08, 0), 0.09), 2))
    return parts, mats, dict(view_pitch=0.6, view_yaw=0.6)


@reg('doped_wafer')
def b_doped_wafer(c1, c2, t):
    grid = lambda p, n: (np.zeros(p.shape[:-1]), ((np.abs(np.mod(p[..., 0] * 7, 1) - 0.5) < 0.06) | (np.abs(np.mod(p[..., 2] * 7, 1) - 0.5) < 0.06)).astype(float))
    mats = [Material('#384060', '#a0a8e0', emissive='#c0c8ff', pattern=grid, metal=0.9), Material('#a03050', '#ff90b0', metal=0.6)]
    flat = lambda p: np.maximum(sd_cyl(p, 0.62, 0.03), p[..., 2] - 0.55)
    return [Part(flat, 0, pos=(0, 0.02, 0)), Part(lambda p: np.maximum(sd_cyl(p, 0.62, 0.025), p[..., 2] - 0.55), 1, pos=(0, -0.04, 0))], \
        mats, dict(view_pitch=0.7, view_yaw=0.6)


@reg('alumina_ceramic_plate')
def b_alumina_plate(c1, c2, t):
    mats = [Material('#e4e4dc', '#ffffff', metal=0.3)]
    return [Part(lambda p: sd_hexprism(p, 0.55, 0.1), 0),
            Part(lambda p: sd_hexprism(p, 0.4, 0.03), 0, pos=(0, 0.12, 0))], mats, dict(view_pitch=0.7, view_yaw=0.6)


@reg('ceramic_armour_plate')
def b_ceramic_armour(c1, c2, t):
    weave = lambda p, n: ((np.sign(np.sin(p[..., 0] * 18) * np.sin(p[..., 2] * 18)) * 0.12), np.zeros(p.shape[:-1]))
    mats = [Material('#e0e0d8', '#ffffff', metal=0.35), Material('#c8a020', '#fff080', metal=0.4, pattern=weave)]
    parts = [Part(lambda p: sd_box(p, (0.6, 0.05, 0.5), 0.02), 1, pos=(0, -0.12, 0))]
    for i in range(3):
        for j in range(3):
            parts.append(Part(lambda p: sd_hexprism(p, 0.16, 0.06), 0, pos=(-0.38 + i * 0.36 + 0.09 * (j % 2), -0.02, -0.3 + j * 0.3)))
    return parts, mats, dict(view_pitch=0.65, view_yaw=0.6)


@reg('cryogenic_lander')
def b_lander(c1, c2, t):
    mats = [Material('#d8c8a8', '#fff8e8', metal=0.4), Material('#e08030', '#ffd090', metal=0.3), Material('#909090', '#ffffff', metal=0.5)]
    sway = 0.06 * math.sin(t * TAU)
    canopy = lambda p: np.maximum(np.abs(sd_sphere(p, 0.5)) - 0.03, -p[..., 1] + 0.05)
    parts = [Part(lambda p: sd_cone_y(p, 0.22, 0.12, 0.13), 0, pos=(sway, -0.5, 0)), Part(lambda p: sd_sphere(p, 0.13), 0, pos=(sway, -0.37, 0)),
             Part(canopy, 1, pos=(0, 0.05, 0))]
    for k in range(4):
        a = k * TAU / 4 + 0.4
        parts.append(Part(lambda p, a=a: sd_capsule(p, V(math.cos(a) * 0.45, 0.15, math.sin(a) * 0.45), V(sway, -0.35, 0), 0.015), 2))
    return parts, mats, dict(view_pitch=0.25, view_yaw=0.6)


@reg('oxygen_rebreather')
def b_rebreather(c1, c2, t):
    mats = [Material('#405060', '#a0c0d8', metal=0.6), Material('#80e0ff', '#ffffff', emissive='#a0f0ff', pattern=em_all()),
            Material('#202830', '#707880', metal=0.3), Material('#e0e0e0', '#ffffff', metal=0.8)]
    mask = lambda p: np.maximum(sd_ellipsoid(p, (0.3, 0.26, 0.22)), -sd_ellipsoid(p - V(0, 0, 0.08), (0.24, 0.2, 0.2)))
    parts = [Part(mask, 0, pos=(-0.25, 0.2, 0)), Part(lambda p: sd_ellipsoid(p, (0.2, 0.1, 0.02)), 1, pos=(-0.25, 0.25, -0.2)),
             Part(lambda p: sd_capsule(p, V(0, -0.35, 0), V(0, 0.2, 0), 0.15), 3, pos=(0.35, -0.12, 0.1))]
    hose = lambda u: V(-0.25 + 0.55 * u, 0.0 - 0.35 * math.sin(u * math.pi), -0.1 + 0.2 * u)
    parts += tube(hose, 8, 0.045, 2)
    return parts, mats, dict(view_pitch=0.3, view_yaw=0.5)


@reg('hydrophobic_coating')
def b_hydrophobic(c1, c2, t):
    mats = [Material('#9098a8', '#ffffff', metal=0.85), Material('#a0d8ff', '#ffffff', metal=0.95)]
    parts = [Part(lambda p: sd_box(p, (0.6, 0.06, 0.5), 0.02), 0, pos=(0, -0.2, 0))]
    roll = (t * 0.6) % 1
    for k, (x, z, r) in enumerate(((-0.3, -0.2, 0.15), (0.2, 0.15, 0.12), (0.3, -0.25, 0.08), (-0.15, 0.25, 0.1), (-0.45 + 0.9 * roll, 0.0, 0.09))):
        parts.append(Part(lambda p, r=r: sd_ellipsoid(p, (r, r * 0.85, r)), 1, pos=(x, -0.14 + r * 0.85, z)))
    return parts, mats, dict(view_pitch=0.5, view_yaw=0.6)


@reg('tholin')
def b_tholin(c1, c2, t):
    mats = [Material('#7a4018', '#e09040', metal=0.6, pattern=shade_noise(7, 0.25, 3))]
    blob = lambda p: smin(smin(sd_sphere(p - V(-0.1, -0.05, 0), 0.36), sd_sphere(p - V(0.28, -0.25, 0.1), 0.24), 0.18),
                          sd_sphere(p - V(-0.2, -0.38, -0.15), 0.22), 0.18) + 0.03 * np.sin(p[..., 0] * 11) * np.sin(p[..., 2] * 9)
    drip = lambda p: sd_capsule(p, V(0.28, -0.4, 0.1), V(0.3, -0.62 - 0.04 * math.sin(t * TAU), 0.1), 0.05)
    return [Part(lambda p: smin(blob(p), drip(p), 0.08), 0)], mats, dict(view_pitch=0.35, view_yaw=0.6)


@reg('organic_evaporite')
def b_evaporite(c1, c2, t):
    mats = [Material('#a89070', '#e0d0b0', metal=0.2, pattern=shade_noise(12, 0.1)), Material('#e8e0c8', '#ffffff', metal=0.7, pattern=em_spots(t, 0.95), emissive='#ffffff')]
    parts = [Part(lambda p: sd_box(p, (0.55, 0.08, 0.45), 0.05) + 0.02 * np.sin(p[..., 0] * 13) * np.sin(p[..., 2] * 11), 0, pos=(0, -0.3, 0))]
    rng = np.random.default_rng(8)
    for k in range(9):
        x, z, s = rng.uniform(-0.4, 0.4), rng.uniform(-0.32, 0.32), rng.uniform(0.06, 0.13)
        parts.append(Part(lambda p, s=s: sd_box(p, (s * 0.6, s * 1.6, s * 0.6), 0.01), 1, pos=(x, -0.22 + s * 1.2, z), rot=roty(rng.uniform(0, 3)) @ rotz(rng.uniform(-0.4, 0.4))))
    return parts, mats, dict(view_pitch=0.45, view_yaw=0.6)


@reg('kekules_dream')
def b_kekule_dream(c1, c2, t):
    """The snake that seized its own tail, coiled into benzene's hexagon."""
    scales = lambda p, n: (np.sin(p[..., 0] * 30) * np.sin(p[..., 2] * 30) * 0.12, np.zeros(p.shape[:-1]))
    mats = [Material('#3a7a30', '#c0ff80', metal=0.5, pattern=scales), Material('#ffd060', '#ffffff', emissive='#ffd060', pattern=em_all())]
    pts = [V(0.5 * math.cos(k * TAU / 6 + t * TAU / 6), 0.04 * math.sin(k * 2), 0.5 * math.sin(k * TAU / 6 + t * TAU / 6)) for k in range(7)]
    parts = capsule_chain(pts, 0.09, 0)
    head = pts[0] + V(0.06, 0.05, -0.1)
    parts.append(Part(lambda p: sd_ellipsoid(p - head, (0.15, 0.1, 0.12)), 0))
    parts.append(Part(lambda p: sd_sphere(p - head - V(0.06, 0.06, -0.08), 0.035), 1))
    return parts, mats, dict(view_pitch=0.7, view_yaw=0.6)


def _hexring(atom, bond, t, glow, r=0.45):
    mats = [Material(atom, '#ffffff', metal=0.5), Material(bond, '#ffffff', metal=0.5), Material(glow, '#ffffff', emissive=glow, pattern=em_bands(10, t, 0, 0.3))]
    pts = [V(r * math.cos(k * TAU / 6), 0, r * math.sin(k * TAU / 6)) for k in range(6)]
    parts = [Part(lambda p, a=pts[k], b=pts[(k + 1) % 6]: sd_capsule(p, a, b, 0.05), 1) for k in range(6)]
    parts += [Part(lambda p, c=c: sd_sphere(p - c, 0.12), 0) for c in pts]
    parts.append(Part(lambda p: sd_torus(p, r * 0.6, 0.035), 2))
    return parts, mats


@reg('kekule_ring')
def b_kekule_ring(c1, c2, t):
    parts, mats = _hexring('#404040', '#a0a0a8', t, '#ffe080')
    return parts, mats, dict(view_pitch=0.6, view_yaw=0.6 + t * TAU / 6)


@reg('chiral_ligand')
def b_chiral_ligand(c1, c2, t):
    pa, ma = _hexring('#305020', '#a0d080', t, '#c0ff80', r=0.28)
    parts = [Part(q.fn, q.mat, pos=(-0.3, 0.1, 0), rot=rotz(0.9)) for q in pa] + [Part(q.fn, q.mat, pos=(0.3, -0.1, 0), rot=rotx(1.2)) for q in pa]
    parts.append(Part(lambda p: sd_capsule(p, V(-0.08, 0.05, 0), V(0.08, -0.05, 0), 0.06), 1))
    return parts, ma, dict(view_pitch=0.4, view_yaw=0.6)


@reg('chemical_clock')
def b_chemical_clock(c1, c2, t):
    parts, mats = _petri('#d8e4ec', '#202848')
    target = lambda p, n: (np.zeros(p.shape[:-1]), (np.sin(np.sqrt((p[..., 0] - 0.1) ** 2 + (p[..., 2] + 0.05) ** 2) * 26 - t * TAU * 2) > 0.2).astype(float))
    mats.append(Material('#2040a0', '#80a0ff', emissive='#ff5030', pattern=target, metal=0.3))
    parts.append(Part(lambda p: sd_cyl(p, 0.64, 0.02), 2, pos=(0, -0.08, 0)))
    return parts, mats, dict(view_pitch=0.85, view_yaw=0.6)


MOTIONS.update({i: (('rock',), ()) for i in ('lead_pig', 'thermochemical_data_card', 'structure_card', 'calibration_card', 'lithium_carbonate',
                                             'prussian_blue_pigment', 'alumina', 'ammonium_nitrate', 'caustic_soda', 'platinum_rhodium_gauze',
                                             'doped_wafer', 'alumina_ceramic_plate', 'ceramic_armour_plate', 'hydrophobic_coating', 'organic_evaporite',
                                             'chemical_clock', 'kekules_dream', 'aramid_fibre', 'polymer_sheet', 'quasicrystal_plate')})
MOTIONS.update({i: ((), ('glint',)) for i in ('copper_ammine_complex', 'kekule_ring')})
MOTIONS.update({i: (('spin',), ('glint',)) for i in ('mof_gas_sorbent', 'edta', 'porphyrin_oxygen_carrier', 'chiral_ligand', 'vanadium_bed')})


# =============================================================================== 1.3 Mathematics
def _glow(col, t=0.0, bands=False):
    return Material(col, '#ffffff', emissive=col, pattern=em_bands(8, t, 1, 0.3) if bands else em_all(), metal=0.6)


@reg('zero')
def b_zero(c1, c2, t):
    """The empty set: a ring of nothing, struck through."""
    mats = [Material('#181820', '#8080a0', metal=0.9), _glow('#e0e0ff', t, True)]
    ring = lambda p: sd_torus(p * V(1, 1, 1) / V(0.8, 1, 1), 0.42, 0.08)
    return [Part(lambda p: sd_torus(p, 0.42, 0.09), 0, rot=rotx(math.pi / 2)),
            Part(lambda p: sd_capsule(p, V(-0.45, -0.6, 0), V(0.45, 0.6, 0), 0.05), 1)], mats, dict(view_pitch=0.15, view_yaw=0.35)


@reg('unity')
def b_unity(c1, c2, t):
    """The unit circle with its one radius: everything multiplied by it stays itself."""
    mats = [Material('#a07820', '#fff0a0', metal=0.95), _glow('#fff0a0'), Material('#302810', '#a08840', metal=0.7)]
    a = t * TAU
    return [Part(lambda p: sd_torus(p, 0.55, 0.045), 0), Part(lambda p: sd_sphere(p, 0.1), 1),
            Part(lambda p: sd_capsule(p, V(0, 0, 0), V(0.55 * math.cos(a), 0, 0.55 * math.sin(a)), 0.035), 1),
            Part(lambda p: sd_sphere(p - V(0.55 * math.cos(a), 0, 0.55 * math.sin(a)), 0.08), 1),
            Part(lambda p: sd_cyl(p, 0.5, 0.015), 2, pos=(0, -0.05, 0))], mats, dict(view_pitch=0.75, view_yaw=0.6)


@reg('microstate_tally')
def b_microstate_tally(c1, c2, t):
    def deco(k, t):
        out = []
        for g, (gx, gz) in enumerate(((-0.24, -0.3), (0.2, -0.3), (-0.24, 0.15), (0.2, 0.15))):
            for i in range(4):
                out.append(Part(lambda p: sd_capsule(p, V(0, 0.04, -0.13), V(0, 0.04, 0.13), 0.022), k, pos=(gx - 0.12 + i * 0.07, 0, gz)))
            if g < 3:
                out.append(Part(lambda p: sd_capsule(p, V(-0.17, 0.05, 0.12), V(0.1, 0.05, -0.12), 0.022), k, pos=(gx, 0, gz)))
        return out
    return _card('#182030', '#80c0ff', t, deco)


@reg('proof_certificate')
def b_proof(c1, c2, t):
    lines = lambda p, n: (np.zeros(p.shape[:-1]), ((np.abs(np.mod(p[..., 2] * 9, 1) - 0.5) < 0.12) & (np.abs(p[..., 0]) < 0.3)).astype(float) * 0)
    mats = [Material('#e8d8b0', '#fff4d8', metal=0.15, pattern=shade_noise(9, 0.06)), Material('#a01818', '#ff6060', metal=0.6),
            Material('#5a4020', '#a08050', metal=0.1)]
    parts = [Part(lambda p: sd_box(p, (0.42, 0.015, 0.4), 0.005), 0),
             Part(lambda p: sd_cyl(p, 0.09, 0.45, 0.02), 0, pos=(0, 0.06, -0.48), rot=rotz(math.pi / 2)),
             Part(lambda p: sd_cyl(p, 0.09, 0.45, 0.02), 0, pos=(0, 0.06, 0.48), rot=rotz(math.pi / 2)),
             Part(lambda p: sd_cyl(p, 0.13, 0.03, 0.02), 1, pos=(0.18, 0.04, 0.2))]
    for i in range(5):
        parts.append(Part(lambda p: sd_box(p, (0.25 - 0.04 * (i % 2), 0.005, 0.012)), 2, pos=(-0.05, 0.02, -0.3 + i * 0.1)))
    parts.append(Part(lambda p: sd_box(p, (0.04, 0.005, 0.04)), 2, pos=(-0.2, 0.02, 0.25)))   # the closing square
    return parts, mats, dict(view_pitch=0.85, view_yaw=0.6)


@reg('pi_crystal')
def b_pi(c1, c2, t):
    """A crystal wheel that has rolled once: the glowing track behind it is exactly π diameters long."""
    mats = [Material('#2050a0', '#80d0ff', metal=0.9), _glow('#80d0ff'), Material('#102040', '#4080c0', metal=0.6)]
    roll = 0.55 * math.sin(t * TAU) * 0.2
    wheel = lambda p: np.minimum(sd_torus(p, 0.3, 0.05), np.minimum(sd_box(p, (0.3, 0.025, 0.025)), sd_box(p, (0.025, 0.025, 0.3))))
    return [Part(wheel, 0, pos=(0.32 + roll, 0.0, 0), rot=rotx(math.pi / 2) @ roty(roll / 0.35)),
            Part(lambda p: sd_box(p, (0.47, 0.02, 0.04)), 1, pos=(-0.25, -0.35, 0)),
            Part(lambda p: sd_sphere(p, 0.05), 1, pos=(0.32 + roll, -0.3, 0)),
            Part(lambda p: sd_box(p, (0.75, 0.02, 0.15), 0.01), 2, pos=(0, -0.4, 0))], mats, dict(view_pitch=0.25, view_yaw=0.35)


@reg('symmetry_crystal')
def b_symmetry_crystal(c1, c2, t):
    """A six-fold snowflake: every arm the same arm turned."""
    mats = [Material('#c080e0', '#ffd0ff', metal=0.95, emissive='#ffb0ff', pattern=em_spots(t, 0.9)), _glow('#ffb0ff')]
    parts = [Part(lambda p: sd_hexprism(p, 0.14, 0.05), 1, rot=rotx(math.pi / 2))]
    for k in range(6):
        R = rotz(k * TAU / 6)
        def arm(p, R=R):
            q = p @ R
            d = sd_capsule(q, V(0, 0, 0), V(0, 0.62, 0), 0.045)
            for y, L in ((0.3, 0.16), (0.48, 0.1)):
                for s in (-1, 1):
                    d = np.minimum(d, sd_capsule(q, V(0, y, 0), V(s * L, y + L * 0.8, 0), 0.03))
            return d
        parts.append(Part(arm, 0))
    return parts, mats, dict(view_pitch=0.0, view_yaw=0.35)


@reg('lie_group_lattice')
def b_lie(c1, c2, t):
    """A smooth group drawn as a torus, its points the lattice of turns it is made of."""
    grid = lambda p, n: (np.zeros(p.shape[:-1]), ((np.cos(np.arctan2(p[..., 2], p[..., 0]) * 12 + t * TAU) > 0.9) |
                                                   (np.cos(np.arctan2(p[..., 1], np.sqrt(p[..., 0] ** 2 + p[..., 2] ** 2) - 0.4) * 8) > 0.9)).astype(float))
    mats = [Material('#302058', '#c0a0ff', emissive='#c0a0ff', pattern=grid, metal=0.8)]
    return [Part(lambda p: sd_torus(p, 0.42, 0.2), 0)], mats, dict(view_pitch=0.55, view_yaw=0.6)


@reg('symmetry_seed')
def b_symmetry_seed(c1, c2, t):
    mats = [Material('#5a3070', '#e0a0ff', metal=0.7), _glow('#ffd0ff', t, True), Material('#40a050', '#a0ff90', metal=0.4)]
    seed = lambda p: sd_ellipsoid(p, (0.28, 0.42, 0.2))
    return [Part(lambda p: np.maximum(seed(p), -sd_box(p, (0.02, 1, 1))), 0, pos=(0, -0.1, 0)),
            Part(lambda p: np.maximum(seed(p), np.abs(p[..., 0]) - 0.02), 1, pos=(0, -0.1, 0)),
            Part(lambda p: sd_capsule(p, V(0, 0.28, 0), V(0.0, 0.5, 0), 0.03), 2),
            Part(lambda p: sd_ellipsoid(p, (0.12, 0.03, 0.06)), 2, pos=(-0.1, 0.5, 0), rot=rotz(0.4)),
            Part(lambda p: sd_ellipsoid(p, (0.12, 0.03, 0.06)), 2, pos=(0.1, 0.5, 0), rot=rotz(-0.4))], mats, dict(view_pitch=0.25, view_yaw=0.45)


@reg('euler_crystal')
def b_euler_crystal(c1, c2, t):
    """A nautilus of green crystal: a spiral growing in proportion to itself, which is the whole of e."""
    mats = [Material('#3a6018', '#c0ff80', metal=0.85, pattern=em_bands(14, t, 0, 0.2), emissive='#c0ff80')]
    b = 0.2
    sp = lambda u: V(0.05 * math.exp(b * u) * math.cos(u), 0.0, 0.05 * math.exp(b * u) * math.sin(u)) * 1.0
    parts = []
    N = 26
    for k in range(N):
        u = 0.4 + k * 0.42
        c = sp(u)
        r = 0.012 * math.exp(b * u) * 1.6
        parts.append(Part(lambda p, c=c, r=r: sd_sphere(p - c, r), 0))
    return parts, mats, dict(view_pitch=1.0, view_yaw=0.6 + t * TAU * 0.0)


@reg('rate_table')
def b_rate_table(c1, c2, t):
    def deco(k, t):
        out = []
        for i in range(8):
            h = 0.55 * math.exp(-0.35 * i)
            out.append(Part(lambda p, h=h: sd_box(p, (0.035, h * 0.5, 0.035)), k, pos=(-0.38 + i * 0.105, 0.03 + h * 0.5, 0.0)))
        return out
    return _card('#203018', '#a0ff80', t, deco)


@reg('stability_margin')
def b_stability(c1, c2, t):
    """A ball resting in a bowl: nudge it and it comes back."""
    mats = [Material('#283850', '#80c0ff', metal=0.7), _glow('#80c0ff')]
    bowl = lambda p: np.maximum(np.abs(sd_sphere(p - V(0, 0.55, 0), 0.75)) - 0.04, p[..., 1] - 0.15)
    sw = 0.25 * math.sin(t * TAU)
    bx, by = 0.6 * math.sin(sw), 0.25 - 0.6 * math.cos(sw)
    return [Part(bowl, 0, pos=(0, -0.3, 0)), Part(lambda p: sd_sphere(p - V(bx, by + 0.05, 0), 0.17), 1)], mats, dict(view_pitch=0.75, view_yaw=0.6)


@reg('assayed_enzyme')
def b_assayed_enzyme(c1, c2, t):
    """A folded enzyme in a measured vial, a gauge band round the glass."""
    mats = [glass('#e8e0d0'), Material('#e09030', '#ffd080', metal=0.5), _glow('#ffd080', t, True), Material('#5a4020', '#c0a060', metal=0.6)]
    parts = [Part(lambda p: np.maximum(np.abs(sd_cyl(p, 0.3, 0.45, 0.06)) - 0.02, -p[..., 2] - 0.1), 0),
             Part(lambda p: sd_cyl(p, 0.33, 0.06, 0.0), 3, pos=(0, 0.48, 0))]
    pts = [V(0.15 * math.cos(u * 3.1), -0.3 + 0.05 * u, 0.15 * math.sin(u * 3.1)) for u in np.linspace(0, 10, 18)]
    parts += capsule_chain(pts, 0.06, 1)
    for i in range(4):
        parts.append(Part(lambda p: sd_box(p, (0.06, 0.008, 0.01)), 2, pos=(0.0, -0.3 + i * 0.2, -0.32)))
    return parts, mats, dict(view_pitch=0.25, view_yaw=0.45)


@reg('confidence_seal')
def b_confidence(c1, c2, t):
    """A gold seal with a bell curve struck in relief, its middle band lit."""
    def relief(p):
        bell = 0.09 * np.exp(-(p[..., 0] / 0.2) ** 2)
        return np.maximum(sd_cyl(p, 0.5, 0.06, 0.03), p[..., 1] - 0.06 - bell * (np.abs(p[..., 2]) < 0.06))
    lit = lambda p, n: (np.zeros(p.shape[:-1]), ((np.abs(p[..., 0]) < 0.2) & (p[..., 1] > 0.065)).astype(float))
    mats = [Material('#a07820', '#ffe080', emissive='#fff0a0', pattern=lit, metal=0.95), Material('#a02020', '#ff6060', metal=0.3)]
    mats.append(_glow('#fff0a0'))
    bell = [V(x, 0.08 + 0.28 * math.exp(-(x / 0.16) ** 2), 0) for x in np.linspace(-0.4, 0.4, 11)]
    parts = [Part(relief, 0), Part(lambda p: sd_torus(p, 0.5, 0.04), 0)] + capsule_chain(bell, 0.035, 2) + [
             Part(lambda p: sd_box(p, (0.08, 0.01, 0.25), 0.01), 1, pos=(-0.15, -0.05, -0.55), rot=roty(0.3)),
             Part(lambda p: sd_box(p, (0.08, 0.01, 0.25), 0.01), 1, pos=(0.15, -0.05, -0.55), rot=roty(-0.3))]
    return parts, mats, dict(view_pitch=0.35, view_yaw=0.0)


@reg('error_code')
def b_error_code(c1, c2, t):
    """A cube of bits, one flipped and glowing as the code catches it."""
    mats = [Material('#204028', '#80ff90', metal=0.6), _glow('#80ff90'), _glow('#ff6050')]
    parts = []
    rng = np.random.default_rng(4)
    for i in range(3):
        for j in range(3):
            for k in range(3):
                m = 2 if (i, j, k) == (2, 2, 0) else (1 if rng.random() < 0.45 else 0)
                parts.append(Part(lambda p: sd_box(p, (0.12, 0.12, 0.12), 0.02), m, pos=((i - 1) * 0.3, (j - 1) * 0.3, (k - 1) * 0.3)))
    return parts, mats, dict(view_pitch=0.5, view_yaw=0.65)


@reg('one_time_pad')
def b_pad(c1, c2, t):
    cipher = lambda p, n: (np.zeros(p.shape[:-1]), (np.sin(np.floor(p[..., 0] * 14) * 12.9 + np.floor(p[..., 2] * 14) * 78.2) > 0.3).astype(float))
    mats = [Material('#3a1818', '#ff8080', emissive='#ff8080', pattern=cipher, metal=0.2), Material('#e8dcd0', '#ffffff', metal=0.1), Material('#606068', '#c0c0c8', metal=0.8)]
    parts = [Part(lambda p: sd_box(p, (0.42, 0.12, 0.5), 0.02), 1, pos=(0, -0.15, 0)),
             Part(lambda p: sd_box(p, (0.42, 0.01, 0.5), 0.01), 0, pos=(0, -0.02, 0)),
             Part(lambda p: sd_box(p, (0.4, 0.008, 0.25), 0.01), 0, pos=(0.05, 0.15, 0.55), rot=rotx(-0.6) @ rotz(0.2)),
             Part(lambda p: sd_cyl(p, 0.05, 0.44, 0.0), 2, pos=(0, 0.0, -0.5), rot=rotz(math.pi / 2))]
    return parts, mats, dict(view_pitch=0.6, view_yaw=0.6)


@reg('euclidean_shard')
def b_euclid_shard(c1, c2, t):
    """A shard of marble with a right triangle and its squares cut into the face."""
    mats = [Material('#d8d0c0', '#ffffff', metal=0.25, pattern=shade_noise(6, 0.1)), _glow('#ffe8a0')]
    shard = lambda p: np.maximum(sd_box(p, (0.5, 0.5, 0.1), 0.02), (p[..., 0] + p[..., 1]) * 0.7 - 0.45)
    a, b = V(-0.3, -0.3, -0.11), V(0.1, -0.3, -0.11)
    c = V(-0.3, 0.0, -0.11)
    parts = [Part(shard, 0)] + [Part(lambda p, u=u, v=v: sd_capsule(p, u, v, 0.025), 1) for u, v in ((a, b), (b, c), (c, a))]
    parts.append(Part(lambda p: sd_box(p, (0.035, 0.035, 0.01)), 1, pos=(-0.25, -0.25, -0.11)))
    return parts, mats, dict(view_pitch=0.1, view_yaw=-0.3 + 0.0)


def _graph(nodes, edges, node_m, edge_m, lit_edges=(), lit_m=None, r=0.07):
    parts = [Part(lambda p, c=V(*c): sd_sphere(p - c, r), node_m) for c in nodes]
    for e, (a, b) in enumerate(edges):
        parts.append(Part(lambda p, a=V(*nodes[a]), b=V(*nodes[b]): sd_capsule(p, a, b, 0.022 if (a, b) not in lit_edges else 0.03),
                          lit_m if (a, b) in lit_edges else edge_m))
    return parts


@reg('optimised_pathway')
def b_pathway(c1, c2, t):
    mats = [Material('#603010', '#ffa050', metal=0.6), Material('#503828', '#a08070', metal=0.4), _glow('#ffa050', t, True)]
    N = [(-0.6, -0.1, 0), (-0.25, 0.3, -0.2), (-0.25, -0.35, 0.2), (0.1, 0.05, 0.25), (0.15, 0.45, -0.1), (0.1, -0.45, -0.2), (0.55, 0.0, 0)]
    E = [(0, 1), (0, 2), (1, 4), (1, 3), (2, 3), (2, 5), (3, 6), (4, 6), (5, 6)]
    lit = [(0, 2), (2, 3), (3, 6)]
    parts = _graph(N, E, 0, 1)
    for a, b in lit:
        parts.append(Part(lambda p, a=V(*N[a]), b=V(*N[b]): sd_capsule(p, a, b, 0.04), 2))
    return parts, mats, dict(view_pitch=0.35, view_yaw=0.6)


@reg('network_map')
def b_network(c1, c2, t):
    mats = [_glow('#60d0ff'), Material('#205060', '#60d0ff', metal=0.6), Material('#082030', '#3080a0', metal=0.4)]
    rng = np.random.default_rng(11)
    N = [tuple(rng.uniform(-0.5, 0.5, 2).tolist()) for _ in range(9)]
    N = [(x, 0.06, z) for x, z in N]
    E = [(i, j) for i in range(9) for j in range(i + 1, 9) if math.dist(N[i], N[j]) < 0.48]
    parts = _graph(N, E, 0, 1, r=0.06) + [Part(lambda p: sd_box(p, (0.6, 0.02, 0.6), 0.02), 2)]
    return parts, mats, dict(view_pitch=0.8, view_yaw=0.6)


@reg('stable_strategy')
def b_ess(c1, c2, t):
    """Three strategies chasing each other round a cycle, none winning for good."""
    mats = [_glow('#ff80c0'), Material('#ff5050', '#ffffff', metal=0.6), Material('#50a0ff', '#ffffff', metal=0.6), Material('#ffd040', '#ffffff', metal=0.6)]
    parts = []
    for k in range(3):
        a = k * TAU / 3 + t * TAU / 3
        c = V(0.45 * math.cos(a), 0, 0.45 * math.sin(a))
        parts.append(Part(lambda p, c=c: sd_sphere(p - c, 0.15), 1 + k))
        arc = lambda u, a=a: V(0.45 * math.cos(a + 0.5 + u * 1.1), 0, 0.45 * math.sin(a + 0.5 + u * 1.1))
        parts += tube(arc, 5, 0.03, 0)
        tip = arc(1.0)
        parts.append(Part(lambda p, c=tip: sd_sphere(p - c, 0.06), 0))
    return parts, mats, dict(view_pitch=0.8, view_yaw=0.6)


@reg('pattern_template')
def b_pattern_template(c1, c2, t):
    turing = lambda p, n: (np.zeros(p.shape[:-1]), (np.sin(p[..., 0] * 17 + 2 * np.sin(p[..., 2] * 9)) * np.sin(p[..., 2] * 17 + 2 * np.sin(p[..., 0] * 7)) > 0.25).astype(float))
    mats = [Material('#102018', '#80ffb0', emissive='#80ffb0', pattern=turing, metal=0.6), Material('#405048', '#a0c0b0', metal=0.8)]
    return [Part(lambda p: sd_box(p, (0.55, 0.04, 0.55), 0.02), 0), Part(lambda p: np.maximum(sd_box(p, (0.62, 0.07, 0.62), 0.02), -sd_box(p, (0.55, 0.2, 0.55))), 1)], \
        mats, dict(view_pitch=0.85, view_yaw=0.6)


@reg('half_turn')
def b_half_turn(c1, c2, t):
    """A Möbius band: walk it once and you come back upside down."""
    mats = [Material('#8060c0', '#e0d0ff', metal=0.85, emissive='#e0c0ff', pattern=em_bands(6, t, 0, 0.35))]
    parts = []
    N = 40
    for k in range(N):
        u = k / N * TAU
        c = V(0.45 * math.cos(u), 0, 0.45 * math.sin(u))
        R = roty(-u) @ rotz(u / 2)
        parts.append(Part(lambda p: sd_box(p, (0.03, 0.18, 0.045), 0.0), 0, pos=tuple(c), rot=R))
    return parts, mats, dict(view_pitch=0.55, view_yaw=0.6)


@reg('eulers_identity')
def b_eulers_identity(c1, c2, t):
    """Half way round the unit circle: e^iπ lands on minus one, and plus one closes it to nothing."""
    mats = [Material('#5a4018', '#ffe8a0', metal=0.95), _glow('#ffe8a0'), _glow('#ffffff')]
    u = math.pi * (0.5 + 0.5 * math.sin(t * TAU - math.pi / 2))
    arc = lambda s: V(0.5 * math.cos(s * u), 0, -0.5 * math.sin(s * u))
    parts = [Part(lambda p: sd_torus(p, 0.5, 0.025), 0), Part(lambda p: sd_box(p, (0.62, 0.012, 0.012)), 0), Part(lambda p: sd_box(p, (0.012, 0.012, 0.62)), 0)]
    parts += tube(arc, 10, 0.045, 1)
    parts += [Part(lambda p: sd_sphere(p - V(0.5, 0, 0), 0.08), 2), Part(lambda p: sd_sphere(p - V(-0.5, 0, 0), 0.08), 2),
              Part(lambda p: sd_sphere(p - arc(1.0), 0.1), 1)]
    return parts, mats, dict(view_pitch=0.85, view_yaw=0.0)


@reg('tautology_shard')
def b_tautology(c1, c2, t):
    """A white crystal that is true however it is turned, held by a ring that ends where it starts."""
    mats = [Material('#e8e8f0', '#ffffff', metal=0.95, emissive='#ffffff', pattern=em_spots(t, 0.93)), _glow('#ffffff', t, True)]
    return [Part(lambda p: sd_octa(p * V(1, 0.7, 1), 0.42), 0, rot=roty(0.4)),
            Part(lambda p: sd_torus(p, 0.5, 0.03), 1, rot=rotx(1.2) @ rotz(t * TAU))], mats, dict(view_pitch=0.3, view_yaw=0.6)


@reg('geodesic_thread')
def b_geodesic(c1, c2, t):
    """A globe with a thread pulled tight along a great circle, the shortest way across a curved world."""
    mats = [Material('#203850', '#6090b0', metal=0.6, pattern=shade_noise(5, 0.15)), _glow('#80d0ff', t, True), Material('#80d0ff', '#ffffff', metal=0.8)]
    return [Part(lambda p: sd_sphere(p, 0.5), 0), Part(lambda p: sd_torus(p, 0.52, 0.03), 1, rot=rotx(0.9) @ rotz(0.5)),
            Part(lambda p: sd_sphere(p, 0.06), 2, pos=tuple(np.array([0.52, 0, 0]) @ (rotx(0.9) @ rotz(0.5)).T)),
            Part(lambda p: sd_sphere(p, 0.06), 2, pos=tuple(np.array([-0.52, 0, 0]) @ (rotx(0.9) @ rotz(0.5)).T))], mats, dict(view_pitch=0.3, view_yaw=0.6 + t * TAU * 0.0)


@reg('epsilon_dust')
def b_epsilon(c1, c2, t):
    """Specks each smaller than the last, falling into a dish that never quite fills."""
    mats = [_glow('#a0ffe0'), Material('#103028', '#60c0a0', metal=0.7)]
    parts = [Part(lambda p: np.maximum(np.maximum(np.abs(sd_sphere(p - V(0, 0.55, 0), 0.8)) - 0.035, p[..., 1] - 0.0), -0.3 - p[..., 1]), 1, pos=(0, -0.2, 0))]
    for k in range(9):
        r = 0.12 * (0.72 ** k)
        y = 0.55 - 0.1 * k - 0.1 * ((t * 3 + k * 0.3) % 1)
        a = k * 2.4
        parts.append(Part(lambda p, c=V(0.25 * math.cos(a) * (1 - k / 10), y, 0.25 * math.sin(a) * (1 - k / 10)), r=r: sd_sphere(p - c, max(r, 0.025)), 0))
    return parts, mats, dict(view_pitch=0.35, view_yaw=0.6)


MOTIONS.update({i: (('rock',), ()) for i in ('microstate_tally', 'proof_certificate', 'rate_table', 'confidence_seal', 'one_time_pad', 'euclidean_shard',
                                             'pattern_template', 'network_map', 'zero', 'pi_crystal', 'stability_margin', 'eulers_identity', 'epsilon_dust',
                                             'unity')})
MOTIONS.update({i: (('spin',), ('glint',)) for i in ('symmetry_crystal', 'euler_crystal', 'lie_group_lattice', 'half_turn', 'geodesic_thread', 'stable_strategy',
                                                     'error_code', 'tautology_shard')})


# =============================================================================== 1.2.0: Inner Space and Titan's hidden ocean
@reg('miniaturiser')
def b_miniaturiser(c1, c2, t):
    """Three rings of lenses round a capsule, a tiny figure shrinking inside."""
    mats = [Material('#c8d0d8', '#ffffff', metal=0.9), glass('#e0f0ff'), _glow_n('#ff6080', t)]
    parts = [Part(lambda p: np.maximum(np.abs(sd_cyl(p, 0.3, 0.45, 0.06)) - 0.02, -p[..., 2] - 0.05), 1), Part(lambda p: sd_cyl(p, 0.34, 0.05, 0.02), 0, pos=(0, -0.48, 0)),
             Part(lambda p: sd_cyl(p, 0.34, 0.05, 0.02), 0, pos=(0, 0.48, 0))]
    for k in range(3):
        parts.append(Part(lambda p: sd_torus(p, 0.46, 0.035), 0, pos=(0, -0.25 + k * 0.25, 0), rot=rotx(0.3 * math.sin(t * TAU + k))))
    s_ = 0.16 * (0.6 + 0.4 * math.cos(t * TAU))
    parts += [Part(lambda p: sd_sphere(p - V(0, s_ * 0.9, 0), s_ * 0.45), 2), Part(lambda p: sd_capsule(p, V(0, -s_, 0), V(0, s_ * 0.4, 0), s_ * 0.35), 2)]
    return parts, mats, dict(view_pitch=0.3, view_yaw=0.6)


def _glow_n(col, t):
    return Material(col, '#ffffff', emissive=col, pattern=em_all(), metal=0.5)


@reg('matrix_fibre')
def b_matrix_fibre(c1, c2, t):
    """Collagen: three strands wound into a triple helix, frayed at one end."""
    mats = [Material('#e8d4bc', '#fff4e4', metal=0.4), Material('#d8b898', '#fff0d8', metal=0.4), Material('#c8a080', '#ffe0c0', metal=0.4)]
    parts = []
    for k in range(3):
        f = lambda u, k=k: V(0.1 * math.cos(u * 14 + k * TAU / 3), -0.6 + 1.2 * u, 0.1 * math.sin(u * 14 + k * TAU / 3))
        parts += tube(f, 18, 0.06, k)
    return [Part(q.fn, q.mat, rot=rotz(0.7)) for q in parts], mats, dict(view_pitch=0.3, view_yaw=0.6)


@reg('nucleotide')
def b_nucleotide(c1, c2, t):
    """One letter of the code: a phosphate (ball), a sugar (pentagon) and a base (a flat ring), joined."""
    mats = [Material('#ffb040', '#ffffff', metal=0.6), Material('#e0e0f0', '#ffffff', metal=0.5), _glow_n('#80e0ff', t)]
    pent = lambda p: np.maximum(np.max(np.stack([p[..., 0] * math.cos(k * TAU / 5) + p[..., 2] * math.sin(k * TAU / 5) for k in range(5)], -1), -1) - 0.16,
                                np.abs(p[..., 1]) - 0.06)
    return [Part(lambda p: sd_sphere(p - V(-0.42, 0.05, 0), 0.16), 0),
            Part(lambda p: sd_capsule(p, V(-0.3, 0.03, 0), V(-0.12, 0, 0), 0.04), 1),
            Part(pent, 1, rot=rotx(math.pi / 2)),
            Part(lambda p: sd_capsule(p, V(0.12, 0, 0), V(0.25, 0, 0), 0.04), 1),
            Part(lambda p: np.maximum(sd_hexprism(p, 0.2, 0.05), -sd_hexprism(p, 0.1, 0.1)), 2, pos=(0.42, 0, 0), rot=rotx(math.pi / 2))], \
        mats, dict(view_pitch=0.35, view_yaw=0.15)


@reg('memory_cell')
def b_memory_cell(c1, c2, t):
    """A lymphocyte: a pale round cell, nearly all nucleus, with a glowing receptor pattern kept on its surface."""
    mats = [Material('#a8c0f0', '#e0f0ff', metal=0.5), Material('#4050a0', '#90a0ff', metal=0.4), _glow_n('#e0f0ff', t)]
    parts = [Part(lambda p: np.maximum(sd_sphere(p, 0.55), -sd_box(p - V(0.4, 0.4, -0.4), (0.4, 0.4, 0.4))), 0),
             Part(lambda p: sd_sphere(p, 0.42), 1)]
    for k in range(10):
        y = 1 - 2 * (k + 0.5) / 10
        r = math.sqrt(1 - y * y)
        a = k * 2.39996
        c = V(math.cos(a) * r, y, math.sin(a) * r) * 0.56
        if c[0] > 0.05 and c[1] > 0.05 and c[2] < -0.05: continue
        parts.append(Part(lambda p, c=c: sd_sphere(p - c, 0.05), 2))
    return parts, mats, dict(view_pitch=0.4, view_yaw=0.6)


@reg('tumour_specimen')
def b_tumour_specimen(c1, c2, t):
    """A specimen jar with a lumpy growth inside, budding."""
    mats = [glass('#f0e0e8'), Material('#c04060', '#ff90b0', metal=0.4, pattern=shade_noise(9, 0.18)), Material('#606878', '#c0c8d0', metal=0.8)]
    lump = lambda p: smin(smin(sd_sphere(p - V(0, -0.2, 0), 0.24), sd_sphere(p - V(0.14, 0.0, 0.05), 0.15), 0.1),
                          sd_sphere(p - V(-0.1, 0.05, -0.08), 0.12 + 0.02 * math.sin(t * TAU)), 0.1)
    return [Part(lambda p: np.maximum(np.maximum(np.abs(sd_cyl(p, 0.36, 0.42, 0.06)) - 0.02, -p[..., 2] - 0.1), p[..., 1] - 0.38), 0),
            Part(lump, 1), Part(lambda p: sd_cyl(p, 0.39, 0.06, 0.02), 2, pos=(0, 0.42, 0))], mats, dict(view_pitch=0.3, view_yaw=0.45)


@reg('matured_antibody')
def b_matured_antibody(c1, c2, t):
    mats = [Material('#d0d8ff', '#ffffff', metal=0.6), _glow_n('#8090ff', t)]
    parts = _y_shape(0, 1, t)
    mats.append(Material('#ffd040', '#ffffff', emissive='#ffd040', pattern=em_bands(6, t, 0, 0.3), metal=0.9))
    parts.append(Part(lambda p: sd_torus(p, 0.5, 0.03), len(mats) - 1, pos=(0, 0.1, 0), rot=rotx(1.25) @ rotz(t * TAU)))
    return parts, mats, dict(view_pitch=0.2, view_yaw=0.4)


@reg('cryobot')
def b_cryobot(c1, c2, t):
    """A long probe, its tip glowing hot from the radium core, melting into a slab of ice; a tether trails from its top."""
    mats = [Material('#c0c8d0', '#ffffff', metal=0.9), _glow_n('#ff8040', t), Material('#b0d8f0', '#ffffff', metal=0.95), Material('#e0c080', '#ffffff', metal=0.3)]
    parts = [Part(lambda p: sd_capsule(p, V(0, -0.25, 0), V(0, 0.45, 0), 0.14), 0),
             Part(lambda p: sd_cone_y(p, 0.14, 0.02, 0.18), 1, pos=(0, -0.42, 0)),
             Part(lambda p: np.maximum(sd_box(p, (0.5, 0.08, 0.45), 0.02), -sd_cyl(p, 0.2, 0.3)), 2, pos=(0, -0.5, 0))]
    teth = lambda u: V(0.15 * math.sin(u * 4 + t * TAU), 0.55 + 0.4 * u, 0.1 * u)
    parts += tube(teth, 8, 0.025, 3)
    return [Part(q.fn, q.mat, pos=q.pos, rot=q.rot) for q in parts], mats, dict(view_pitch=0.25, view_yaw=0.5)


@reg('vent_biofilm')
def b_vent_biofilm(c1, c2, t):
    """A crust of bacteria on a chip of chimney rock, its filaments swaying in warm water."""
    mats = [Material('#504038', '#a08070', metal=0.3, pattern=shade_noise(8, 0.2)), Material('#ffc080', '#ffffff', metal=0.3),
            Material('#e09060', '#ffd0a0', metal=0.3)]
    parts = [Part(lambda p: sd_box(p, (0.5, 0.12, 0.4), 0.06) + 0.03 * np.sin(p[..., 0] * 12) * np.sin(p[..., 2] * 9), 0, pos=(0, -0.35, 0))]
    rng = np.random.default_rng(4)
    for k in range(14):
        x, z = rng.uniform(-0.42, 0.42), rng.uniform(-0.32, 0.32)
        h = rng.uniform(0.15, 0.4)
        sw = 0.08 * math.sin(t * TAU + k)
        parts += capsule_chain([V(x, -0.22, z), V(x + sw * 0.5, -0.22 + h * 0.5, z), V(x + sw, -0.22 + h, z)], 0.025, 1 + k % 2)
    return parts, mats, dict(view_pitch=0.5, view_yaw=0.6)


@reg('eel_photophore')
def b_photophore(c1, c2, t):
    """A light organ: a glowing bead in a dark cup with a lens over it, pulsing."""
    mats = [Material('#203848', '#60808c', metal=0.5), Material('#80ffe8', '#ffffff', emissive='#a0fff0', pattern=em_bands(4, t, 1, 0.5), metal=0.4),
            glass('#d0fff8')]
    cup = lambda p: np.maximum(np.maximum(sd_sphere(p, 0.45), -sd_sphere(p - V(0, 0.15, 0), 0.38)), p[..., 1] - 0.15)
    return [Part(cup, 0), Part(lambda p: sd_sphere(p - V(0, 0.08, 0), 0.24 + 0.02 * math.sin(t * TAU)), 1),
            Part(lambda p: sd_torus(p - V(0, 0.15, 0), 0.4, 0.04), 2)], mats, dict(view_pitch=0.55, view_yaw=0.6)


MOTIONS.update({'nucleotide': (('spin',), ('glint',)), 'memory_cell': (('spin',), ('glint',)), 'matrix_fibre': (('rock',), ()),
                'vent_biofilm': (('rock',), ()), 'cryobot': (('rock',), ())})


@reg('luminous_mesoglea')
def b_mesoglea(c1, c2, t):
    """A lump of clear jelly from a bell, a glowing ring still inside it."""
    mats = [glass('#c8ecff'), Material('#ff90e0', '#ffffff', emissive='#ffb0f0', pattern=em_bands(4, t, 0, 0.5), metal=0.4)]
    dome = lambda p: np.maximum(sd_ellipsoid(p, (0.55, 0.38, 0.55)), -p[..., 1] - 0.18)
    return [Part(dome, 0), Part(lambda p: sd_torus(p, 0.22, 0.05), 1, pos=(0, 0.02, 0))], mats, dict(view_pitch=0.45, view_yaw=0.6)


@reg('crab_chitin')
def b_chitin(c1, c2, t):
    """A curved plate of shell, ridged, with a few pale bristles."""
    ridge = lambda p, n: (np.sin(p[..., 0] * 22) * 0.08, np.zeros(p.shape[:-1]))
    mats = [Material('#f0e8dc', '#ffffff', metal=0.45, pattern=ridge), Material('#c0b4a0', '#ffffff', metal=0.3)]
    shell = lambda p: np.maximum(np.abs(sd_sphere(p - V(0, -0.6, 0), 0.9)) - 0.06, -p[..., 1] - 0.0)
    plate = lambda p: np.maximum(shell(p), sd_box(p, (0.55, 1, 0.45), 0.1))
    parts = [Part(plate, 0, pos=(0, -0.2, 0))]
    for k in range(6):
        x = -0.35 + k * 0.14
        parts.append(Part(lambda p, x=x: sd_capsule(p, V(x, 0.12, 0.3), V(x + 0.03, 0.3, 0.45), 0.015), 1))
    return parts, mats, dict(view_pitch=0.55, view_yaw=0.6)
