"""Item art for the Antiverse / antimatter content (round 11). Each item has its own model and motion."""
import math
import numpy as np
from items3d import *
import anim


def _flash(t, seed, every=0.25):
    """Annihilation sparks: now and then a stray atom of matter meets the surface - a white pixel and a cross."""
    every = 1.0 / max(1, round(1.0 / every))      # a whole number of sparks per loop, so the loop is seamless
    def ov(img):
        k = int(t / every)
        rnd = np.random.default_rng(seed * 101 + k)
        ph = (t % every) / every
        if ph < 0.45:
            x, y = int(rnd.integers(3, 13)), int(rnd.integers(3, 13))
            sparkle(img, x, y, H('#ffe0c0'), 1 if ph < 0.25 else 0)
    return ov


def _ingot_shape(p):
    return np.maximum(sd_box(p, (0.85, 0.3, 0.45), 0.05), (p[..., 1] * 0.95 + np.abs(p[..., 2]) * 0.6 + np.abs(p[..., 0]) * 0.12 - 0.42))


def antiiron_ingot(c1, c2, t):
    # a levitating ingot that never touches anything: a ring of field lines under it, a gap of air
    parts = [Part(_ingot_shape, 0, pos=(0, 0.12 + 0.04 * math.sin(t * TAU), 0), rot=roty(0.15 * math.sin(t * TAU))),
             Part(lambda p: sd_torus(p, 0.62, 0.035), 1, pos=(0, -0.4, 0), rot=rotx(0.0) @ roty(t * TAU))]
    mats = [Material(c1, c2, metal=0.92, pattern=pat_noise_shade(10, 0.12, 3)),
            Material('#80d0ff', '#ffffff', emissive='#80d0ff', pattern=pat_bands(10, 2.0, t, 0.25, axis=0))]
    return parts, mats, dict(view_pitch=0.55, view_yaw=0.65, zoom=1.0), _flash(t, 11)


def antigold_ingot(c1, c2, t):
    # cold blue gold whose gloss runs the "wrong" way: an emissive band sweeping from right to left
    parts = [Part(_ingot_shape, 0, rot=roty(0.1))]
    mats = [Material(c1, c2, metal=0.97, pattern=pat_bands(4, -1.0, t, 0.15, axis=0), emissive=c2)]
    return parts, mats, dict(view_pitch=0.6, view_yaw=0.7, zoom=1.05), _flash(t, 12, 0.5)


def mirror_alloy_ingot(c1, c2, t):
    # chequerboard of matter and antimatter grains, each pair's boundary glowing with its own Penning field
    def chk(p, n):
        c = (np.floor(p[..., 0] * 5) + np.floor(p[..., 2] * 5) + np.floor(p[..., 1] * 5)) % 2
        edge = (np.abs(((p[..., 0] * 5) % 1) - 0.5) > 0.42) | (np.abs(((p[..., 2] * 5) % 1) - 0.5) > 0.42)
        hum = 0.5 + 0.5 * np.sin(p[..., 0] * 6 - t * TAU * 2)
        return (c - 0.5) * 0.5, (edge & (hum > 0.3)).astype(float)
    parts = [Part(_ingot_shape, 0, pos=(0, 0.02 * math.sin(t * TAU * 2), 0))]
    mats = [Material('#5a6070', '#f0a0ff', metal=0.9, pattern=chk, emissive='#f0a0ff')]
    return parts, mats, dict(view_pitch=0.6, view_yaw=0.62, zoom=1.05), None


def raw_antiiron(c1, c2, t):
    def f(p):
        return sd_sphere(p, 0.55) + 0.11 * np.sin(p[..., 0] * 6 + 1) * np.sin(p[..., 1] * 7) * np.sin(p[..., 2] * 5 + 2)
    parts = [Part(f, 0, pos=(0, 0.06 * math.sin(t * TAU), 0)), Part(lambda p: sd_torus(p, 0.7, 0.03), 1, rot=rotx(1.2) @ rotz(t * TAU))]
    mats = [Material(c1, c2, metal=0.5, pattern=pat_combine(pat_noise_shade(8, 0.3, 2), pat_spots(t, 0.9, 4)), emissive=c2),
            Material('#80d0ff', '#ffffff', emissive='#80d0ff', pattern=pat_all())]
    return parts, mats, dict(view_pitch=0.3, view_yaw=0.5), _flash(t, 13, 0.33)


def raw_antigold(c1, c2, t):
    def f(p):
        return sd_sphere(p, 0.52) + 0.08 * np.sin(p[..., 0] * 9) * np.sin(p[..., 1] * 8 + 1) * np.sin(p[..., 2] * 7)
    nug = [(0.38, 0.3, 0.1), (-0.3, 0.35, -0.2), (0.1, -0.4, 0.3)]
    parts = [Part(f, 0)] + [Part(lambda p: sd_sphere(p, 0.14), 1, pos=q) for q in nug]
    mats = [Material('#2a3048', '#80b0ff', metal=0.3, pattern=pat_noise_shade(7, 0.3, 5)),
            Material(c2, '#ffffff', metal=0.97, pattern=pat_spots(t, 0.6, 9), emissive='#e0f0ff')]
    return parts, mats, dict(view_pitch=0.35, view_yaw=0.4 + 0.3 * math.sin(t * TAU)), None


def anticarbon_crystal(c1, c2, t):
    # an antidiamond: a dark octahedron whose facets flare orange where matter strikes them
    def flare(p, n):
        lit = np.sin((p[..., 0] + p[..., 1] * 1.3 - p[..., 2]) * 9 + t * TAU * 3) > 0.85
        return np.zeros(lit.shape), lit.astype(float)
    parts = [Part(lambda p: sd_octa(p, 0.72), 0, rot=roty(t * TAU) @ rotx(0.2))]   # a whole turn per loop (half did not loop)
    mats = [Material('#202020', c2, metal=0.95, pattern=flare, emissive=c2, keep_dark=True)]
    return parts, mats, dict(view_pitch=0.25, view_yaw=0.5), _flash(t, 14, 0.2)


def antisilicon_shard(c1, c2, t):
    # a semiconductor wafer splinter with a "hole" current of positrons crawling the wrong way along it
    shard = lambda p: np.maximum(sd_box(p, (0.8, 0.1, 0.55)), p[..., 0] * 0.6 + p[..., 2] - 0.45)
    def holes(p, n):
        lane = np.abs(((p[..., 2] * 6) % 1) - 0.5) < 0.12
        run = np.sin(p[..., 0] * 9 + t * TAU * 2) > 0.6
        return np.zeros(lane.shape), (lane & run).astype(float)
    parts = [Part(shard, 0, rot=rotz(0.25))]
    mats = [Material('#6a5040', c2, metal=0.85, pattern=holes, emissive='#ff70b0')]
    return parts, mats, dict(view_pitch=0.95, view_yaw=0.6), None


def antihydrogen_crystal(c1, c2, t):
    # frozen antihydrogen: a cluster of icy hexagonal needles, faint spectral lines (Balmer colours) pulsing in turn
    lines = ['#ff3040', '#40d0ff', '#6060ff', '#a040ff']
    k = int(t * 4) % 4
    parts = [Part(lambda p: sd_hexprism(p, 0.18, 0.62), 0, rot=rotz(0.25)),
             Part(lambda p: sd_hexprism(p, 0.13, 0.42), 0, pos=(0.32, -0.2, 0.1), rot=rotz(-0.5)),
             Part(lambda p: sd_hexprism(p, 0.12, 0.38), 0, pos=(-0.3, -0.25, -0.05), rot=rotz(0.7))]
    mats = [Material(c1, c2, metal=0.9, pattern=pat_bands(6, 1.0, t, 0.12), emissive=lines[k])]
    return parts, mats, dict(view_pitch=0.25, view_yaw=0.6), None


def antihelium_nucleus(c1, c2, t):
    # two antiprotons (pink) and two antineutrons (grey) jostling in a tight cluster, a glow shell round them
    a = t * TAU
    q = [(0.2 * math.cos(a), 0.18, 0.2 * math.sin(a)), (-0.2 * math.cos(a), 0.18, -0.2 * math.sin(a)),
         (0.2 * math.sin(a), -0.15, -0.2 * math.cos(a)), (-0.2 * math.sin(a), -0.15, 0.2 * math.cos(a))]
    parts = [Part(lambda p: sd_sphere(p, 0.26), 0 if i < 2 else 1, pos=q[i]) for i in range(4)]
    mats = [Material('#ff70b0', '#ffffff', emissive='#ff70b0', pattern=pat_pulse_core(t)), Material('#606878', '#ffffff', metal=0.4)]
    return parts, mats, dict(view_pitch=0.3, view_yaw=0.5), _flash(t, 15, 0.5)


def antiquark_shard(c1, c2, t):
    # three anti-colour shards (anti-red cyan, anti-green magenta, anti-blue yellow) bound by flux tubes, rotating
    cols = ['#40ffff', '#ff40ff', '#ffff40']
    parts, mats = [], []
    for i in range(3):
        ang = t * TAU + i * TAU / 3
        pos = (0.38 * math.cos(ang), 0.1 * math.sin(ang * 2), 0.38 * math.sin(ang))
        mats.append(Material(cols[i], '#ffffff', emissive=cols[i], pattern=pat_spots(t, 0.5, i)))
        parts.append(Part(lambda p: sd_octa(p / np.array([0.6, 1, 0.6]), 0.4) * 0.6, i, pos=pos, rot=roty(ang)))
    mats.append(Material('#ffffff', '#ffffff', emissive='#ffd0e0', pattern=pat_all()))
    parts.append(Part(lambda p: sd_torus(p, 0.38, 0.03), 3, rot=rotx(0.08)))
    return parts, mats, dict(view_pitch=0.45, view_yaw=0.6), None


def antiproton_bundle(c1, c2, t):
    # a glass magnetic bottle with a pink knot of antiprotons circling inside, coils at both ends
    a = t * TAU
    parts = [Part(lambda p: sd_torus(p, 0.36, 0.07), 1, pos=(0, 0.48, 0)), Part(lambda p: sd_torus(p, 0.36, 0.07), 1, pos=(0, -0.48, 0))]
    for k in range(4):
        b = k * TAU / 4 + 0.4
        parts.append(Part(lambda p, b=b: sd_capsule(p, (0.36 * math.cos(b), -0.48, 0.36 * math.sin(b)), (0.36 * math.cos(b), 0.48, 0.36 * math.sin(b)), 0.035), 0))
    for k in range(3):
        b = a * 2 + k * TAU / 3
        parts.append(Part(lambda p: sd_sphere(p, 0.11), 2, pos=(0.17 * math.cos(b), 0.2 * math.sin(a + k), 0.17 * math.sin(b))))
    mats = [Material('#405060', '#c0e0ff', metal=0.95, gloss=1.0), Material('#a07040', '#ffd0a0', metal=0.9),
            Material('#ff70b0', '#ffffff', emissive='#ff70b0', pattern=pat_all())]
    return parts, mats, dict(view_pitch=0.25, view_yaw=0.6), None


def mirror_processor(c1, c2, t):
    # a chip whose traces glow, but the signal pulses run backwards (positron current)
    def tr(p, n):
        grid = (np.abs(((p[..., 0] * 4) % 1) - 0.5) < 0.08) | (np.abs(((p[..., 2] * 4) % 1) - 0.5) < 0.08)
        run = np.sin(p[..., 0] * 6 + p[..., 2] * 4 + t * TAU * 2) > 0.5
        return np.zeros(grid.shape), (grid & run).astype(float)
    parts = [Part(lambda p: sd_box(p, (0.62, 0.1, 0.62), 0.03), 0),
             Part(lambda p: sd_box(p, (0.28, 0.06, 0.28), 0.02), 1, pos=(0, 0.13, 0))]
    mats = [Material('#2a1810', c2, metal=0.6, pattern=tr, emissive=c2), Material('#101010', '#ff9050', metal=0.95, keep_dark=True)]
    return parts, mats, dict(view_pitch=0.75, view_yaw=0.785), None


def baryogenesis_seed(c1, c2, t):
    # a glowing seed with a pair of spirals (matter gold, antimatter blue) wound round it - one is always a little longer
    parts = [Part(lambda p: sd_sphere(p / np.array([0.8, 1.0, 0.8]), 0.38) * 0.8, 0)]
    for k in range(6):
        a = t * TAU + k * 0.9
        h = -0.45 + k * 0.18
        parts.append(Part(lambda p: sd_sphere(p, 0.06), 1, pos=(0.45 * math.cos(a), h, 0.45 * math.sin(a))))
        if k < 5:
            parts.append(Part(lambda p: sd_sphere(p, 0.05), 2, pos=(0.45 * math.cos(a + math.pi), h, 0.45 * math.sin(a + math.pi))))
    mats = [Material('#fff0c0', '#ffffff', emissive='#fff0c0', pattern=pat_pulse_core(t)),
            Material('#ffd060', '#ffffff', emissive='#ffd060', pattern=pat_all()), Material('#60a0ff', '#ffffff', emissive='#60a0ff', pattern=pat_all())]
    return parts, mats, dict(view_pitch=0.3, view_yaw=0.5), None


def cpt_mirror(c1, c2, t):
    # an anti-gold ring around nothing; the "glass" is a slowly turning inversion of the scene - a hue-flipped swirl
    def swirl(p, n):
        r = np.sqrt(p[..., 0] ** 2 + p[..., 1] ** 2)
        a = np.arctan2(p[..., 1], p[..., 0])
        v = np.sin(a * 3 + r * 10 - t * TAU * 2)
        return v * 0.3, (v > 0.4).astype(float)
    parts = [Part(lambda p: sd_torus(p, 0.58, 0.1), 0, rot=rotx(math.pi / 2)),
             Part(lambda p: sd_cyl(p, 0.52, 0.02), 1, rot=rotx(math.pi / 2)),
             Part(lambda p: sd_box(p, (0.07, 0.3, 0.07), 0.02), 0, pos=(0, -0.85, 0))]
    mats = [Material('#30406a', '#a0d0ff', metal=0.97), Material('#2a1408', '#ffb070', pattern=swirl, emissive='#ffb070')]
    return parts, mats, dict(view_pitch=0.15, view_yaw=0.35 + 0.25 * math.sin(t * TAU)), None


def penning_harness(c1, c2, t):
    # a harness: a pair of coil rings joined by a spine, a blue field shell pulsing round the whole thing
    parts = [Part(lambda p: sd_torus(p, 0.42, 0.07), 0, pos=(0, 0.3, 0), rot=rotx(0.3)),
             Part(lambda p: sd_torus(p, 0.42, 0.07), 0, pos=(0, -0.3, 0), rot=rotx(0.3)),
             Part(lambda p: sd_box(p, (0.08, 0.38, 0.08), 0.03), 1, pos=(0, 0, -0.36)),
             Part(lambda p: sd_sphere(p, 0.12), 2, pos=(0, 0, 0))]
    mats = [Material('#a07040', '#ffd0a0', metal=0.9, pattern=pat_bands(10, 2.0, t, 0.2, axis=0), emissive='#ffb070'),
            Material('#304050', '#c0d0e0', metal=0.8), Material('#80d0ff', '#ffffff', emissive='#80d0ff', pattern=pat_spots(t, 0.7, 2))]
    return parts, mats, dict(view_pitch=0.3, view_yaw=0.6), None


ITEMS = {
    'antiiron_ingot': antiiron_ingot, 'antigold_ingot': antigold_ingot, 'mirror_alloy_ingot': mirror_alloy_ingot,
    'raw_antiiron': raw_antiiron, 'raw_antigold': raw_antigold, 'anticarbon_crystal': anticarbon_crystal,
    'antisilicon_shard': antisilicon_shard, 'antihydrogen_crystal': antihydrogen_crystal, 'antihelium_nucleus': antihelium_nucleus,
    'antiquark_shard': antiquark_shard, 'antiproton_bundle': antiproton_bundle, 'mirror_processor': mirror_processor,
    'baryogenesis_seed': baryogenesis_seed, 'cpt_mirror': cpt_mirror, 'penning_harness': penning_harness,
}
RECIPES = {
    'antiiron_ingot': ((), ('glint',)), 'antigold_ingot': (('rock',), ()), 'mirror_alloy_ingot': (('breathe',), ('glint',)),
    'raw_antiiron': ((), ()), 'raw_antigold': ((), ('sparkles',)), 'anticarbon_crystal': ((), ('glint',)),
    'antisilicon_shard': (('rock',), ()), 'antihydrogen_crystal': (('bob',), ('sparkles',)), 'antihelium_nucleus': (('pulse',), ()),
    'antiquark_shard': ((), ()), 'antiproton_bundle': (('rock',), ('glint',)), 'mirror_processor': (('breathe',), ()),
    'baryogenesis_seed': (('bob',), ('embers',)), 'cpt_mirror': ((), ('glint',)), 'penning_harness': (('rock',), ('orbit',)),
}
