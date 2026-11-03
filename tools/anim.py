"""
Animation layer for item textures (MeatballCraft-style variety on top of the 3D models in items3d.py).

Every effect here is periodic in t over [0, 1): frequencies are whole numbers of cycles per loop and every
moving thing returns exactly to where it started, so the last frame flows straight back into the first.

Three kinds of animation:
  * motions    - change how the model is viewed or lit (spin, rock, bob, pulse, tumble, hue cycle, breathe)
  * overlays   - 2D effects painted on the finished frame (glints, sparkle bursts, orbiting motes, embers,
                 glitches, infalling sparks, eruption rays, fractal growth)
  * narratives - flagship items get a little story in a longer loop (a black hole forming and evaporating,
                 a universe banging and crunching, a tree growing, an infinite Droste zoom...)
"""
import math, hashlib
import numpy as np
from render3d import render, Part, Material, mix, hexc, sd_sphere, sd_torus, sd_capsule, sd_box, sd_octa, rotx, roty, rotz
import items3d as I

TAU = math.tau
WHITE = np.ones(3)


def h(s, k=0):
    return int(hashlib.md5(f'{s}:{k}'.encode()).hexdigest()[:8], 16)


# ------------------------------------------------------------------------------------------- colour helpers
def hue_matrix(deg):
    a = math.radians(deg)
    c, s_ = math.cos(a), math.sin(a)
    k = 1 / 3
    sq = math.sqrt(k)
    return np.array([[c + (1 - c) * k, k * (1 - c) - sq * s_, k * (1 - c) + sq * s_],
                     [k * (1 - c) + sq * s_, c + k * (1 - c), k * (1 - c) - sq * s_],
                     [k * (1 - c) - sq * s_, k * (1 - c) + sq * s_, c + k * (1 - c)]])


def hue_shift_materials(mats, deg):
    M = hue_matrix(deg)
    for m in mats:
        m.ramp = np.clip(m.ramp @ M.T, 0, 1)
        if m.emissive is not None:
            m.emissive = np.clip(np.asarray(m.emissive) @ M.T, 0, 1)


# ------------------------------------------------------------------------------------------- motions
def apply_motions(parts, mats, view, t, motions, seed):
    view = dict(view)
    ph = (seed % 1000) / 1000.0
    for m in motions:
        if m == 'spin':
            view['view_yaw'] = view.get('view_yaw', 0.785) + TAU * t
        elif m == 'spin_back':
            view['view_yaw'] = view.get('view_yaw', 0.785) - TAU * t
        elif m == 'rock':
            view['view_yaw'] = view.get('view_yaw', 0.785) + 0.32 * math.sin(TAU * t)
        elif m == 'tumble':
            view['view_yaw'] = view.get('view_yaw', 0.785) + 0.28 * math.cos(TAU * t)
            view['view_pitch'] = view.get('view_pitch', 0.5) + 0.18 * math.sin(TAU * t)
        elif m == 'bob':
            dy = 0.07 * math.sin(TAU * t)
            for p in parts:
                p.pos = p.pos + np.array([0, dy, 0])
        elif m == 'pulse':
            view['zoom'] = view.get('zoom', 1.0) * (1 + 0.06 * math.sin(TAU * t))
        elif m == 'heartbeat':
            # two beats per loop; measured as the circular distance to the beat so the swell rises and falls smoothly
            # (the old exp(-(t % 0.5)...) jumped from rest to full swell in one frame on the loop seam and mid-loop)
            x = t % 0.5
            beat = math.exp(-(min(x, 0.5 - x) / 0.06) ** 2)
            view['zoom'] = view.get('zoom', 1.0) * (1 + 0.08 * beat)
        elif m == 'hue':
            hue_shift_materials(mats, 360 * t)
        elif m == 'hue_soft':
            hue_shift_materials(mats, 40 * math.sin(TAU * t))
        elif m == 'breathe':
            k = 0.7 + 0.3 * math.sin(TAU * t)
            for mm in mats:
                if mm.emissive is not None:
                    mm.emissive = np.clip(np.asarray(mm.emissive) * k + (1 - k) * 0.1, 0, 1)
    return view


# ------------------------------------------------------------------------------------------- overlays
def _opaque(img):
    return img[..., 3] > 0.5


def _blend(img, y, x, rgb, a):
    n = img.shape[0]
    if 0 <= x < n and 0 <= y < n and a > 0:
        img[y, x, :3] = img[y, x, :3] * (1 - a) * (img[y, x, 3] > 0) + np.asarray(rgb) * a + (img[y, x, :3] * 0 if img[y, x, 3] > 0 else 0)
        img[y, x, 3] = max(img[y, x, 3], a)


def ov_glint(img, t, seed, start=0.0, width=3):
    """A bright diagonal sweep across the item once per loop (the classic 'shiny' pass)."""
    n = img.shape[0]
    local = (t - start) % 1.0
    if local > 0.4:
        return
    pos = -6 + local / 0.4 * (2 * n + 12)
    ys, xs = np.mgrid[0:n, 0:n]
    band = np.clip(1 - np.abs((xs + ys) - pos) / width, 0, 1) * _opaque(img)
    img[..., :3] = img[..., :3] * (1 - band[..., None] * 0.65) + band[..., None] * 0.65


def _edge_points(img, seed, count):
    op = _opaque(img)
    n = img.shape[0]
    pts = [(y, x) for y in range(n) for x in range(n) if op[y, x] and (
        not op[max(0, y - 2), x] or not op[min(n - 1, y + 2), x] or not op[y, max(0, x - 2)] or not op[y, min(n - 1, x + 2)])]
    if not pts:
        return []
    rng = np.random.default_rng(seed)
    idx = rng.choice(len(pts), size=min(count, len(pts)), replace=False)
    return [pts[i] for i in idx]


def ov_sparkles(img, t, seed, rgb, count=4, period=1):
    """Four-point star sparkles that bloom and fade at different spots around the silhouette."""
    pts = _edge_points(img, seed, count)
    for i, (y, x) in enumerate(pts):
        life = (t * period + i / max(1, len(pts))) % 1.0
        if life > 0.3:
            continue
        s = math.sin(life / 0.3 * math.pi)
        arm = int(round(s * 3))
        for d in range(1, arm + 1):
            a = s * (1 - d / (arm + 1))
            for dy, dx in ((d, 0), (-d, 0), (0, d), (0, -d)):
                _blend(img, y + dy, x + dx, rgb, a)
        _blend(img, y, x, WHITE, s)


def ov_orbit(img, t, seed, rgb, count=3, rx=13, ry=5, tilt=0.3, speed=1):
    """Glowing motes orbiting the item; behind it they only show in empty pixels."""
    n = img.shape[0]
    c = (n - 1) / 2
    base = img.copy()
    for i in range(count):
        a = TAU * (t * speed + i / count)
        x = c + math.cos(a) * rx
        y = c + math.sin(a) * ry + math.cos(a) * rx * tilt
        front = math.sin(a) > 0
        for dy in (-1, 0, 1):
            for dx in (-1, 0, 1):
                yy, xx = int(round(y)) + dy, int(round(x)) + dx
                if not (0 <= yy < n and 0 <= xx < n):
                    continue
                core = dy == 0 and dx == 0
                if not front and base[yy, xx, 3] > 0.5:
                    continue
                _blend(img, yy, xx, WHITE if core else rgb, 1.0 if core else 0.55)


def ov_embers(img, t, seed, rgb, count=6, rise=1.0):
    """Particles drifting up off the item and fading - heat, vapour or leaking energy."""
    n = img.shape[0]
    op = _opaque(img)
    cols = np.nonzero(op.any(0))[0]
    if len(cols) == 0:
        return
    rng = np.random.default_rng(seed)
    for i in range(count):
        x0 = int(rng.choice(cols))
        ph = rng.random()
        life = (t + ph) % 1.0
        rows = np.nonzero(op[:, x0])[0]
        y0 = rows.min() if len(rows) else n // 2
        y = y0 - life * n * 0.5 * rise
        x = x0 + math.sin(TAU * (life + ph)) * 1.5
        a = math.sin(life * math.pi)
        _blend(img, int(round(y)), int(round(x)), rgb, a)
        _blend(img, int(round(y)) + 1, int(round(x)), rgb, a * 0.4)


def ov_infall(img, t, seed, rgb, count=8):
    """Sparks spiralling inward to the centre (accretion)."""
    n = img.shape[0]
    c = (n - 1) / 2
    for i in range(count):
        life = (t + i / count) % 1.0
        r = (1 - life) * n * 0.5
        a = TAU * (i / count) + life * 4.0 * 1.0 + TAU * life
        _blend(img, int(round(c + math.sin(a) * r * 0.6)), int(round(c + math.cos(a) * r)), rgb, math.sin(life * math.pi))


def ov_rays(img, t, seed, rgb, bursts=2, fade_in=0.0):
    """Eruptions: rays shoot outward in pulses.  fade_in > 0 ramps each burst up over that fraction of its life,
    so a burst that starts on the loop seam does not pop in at full brightness."""
    n = img.shape[0]
    c = (n - 1) / 2
    for b in range(bursts):
        life = (t * bursts - b) % bursts
        if life > 1:
            continue
        life = (t * bursts) % 1.0
        for k in range(8):
            a = TAU * k / 8 + b * 0.4
            r0, r1 = 7 + life * 6, 9 + life * 9
            for r in np.linspace(r0, r1, 6):
                env = min(1.0, life / fade_in) if fade_in > 0 else 1.0
                _blend(img, int(round(c + math.sin(a) * r)), int(round(c + math.cos(a) * r)), rgb, (1 - life) * 0.9 * env)


def ov_glitch(img, t, seed):
    """Brief reality glitches: rows shear sideways and colour channels split for a couple of frames."""
    for w in (0.18, 0.62):
        if abs(t - w) < 0.035:
            n = img.shape[0]
            rng = np.random.default_rng(seed + int(w * 100))
            for _ in range(4):
                y0 = rng.integers(0, n - 3)
                sh = int(rng.integers(-3, 4))
                img[y0:y0 + 3] = np.roll(img[y0:y0 + 3], sh, axis=1)
            img[..., 0] = np.roll(img[..., 0], 1, axis=1)
            img[..., 2] = np.roll(img[..., 2], -1, axis=1)


def ov_fractal(img, t, seed, rgb):
    """Cantor dust that builds itself level by level *behind* the item, then dissolves (MeatballCraft fractal style)."""
    n = img.shape[0]
    behind = img[..., 3] < 0.5
    level = 1 + int(t * 4) % 4               # 1..4 levels of subdivision
    x = (t * 4) % 1
    fade = min(1.0, max(0.0, min(x, 0.9 - x) / 0.15)) * 0.85   # fully faded out before a level swaps, so no pop
    def dust(x0, y0, size, depth):
        if depth == 0:
            yy, xx = int(y0 + size / 2), int(x0 + size / 2)
            if 0 <= yy < n and 0 <= xx < n and behind[yy, xx]:
                _blend(img, yy, xx, rgb, fade)
            return
        s3 = size / 3
        for (dx, dy) in ((0, 0), (2, 0), (0, 2), (2, 2)):
            dust(x0 + dx * s3, y0 + dy * s3, s3, depth - 1)
    dust(0, 0, n, level)


def ov_tracer(img, t, seed, rgb, path, trail=8):
    """A comet running around a closed path, leaving a fading trail (closed timelike curves, infinity)."""
    for k in range(trail):
        tt = (t - k * 0.02) % 1.0
        x, y = path(tt)
        a = 1.0 - k / trail
        _blend(img, int(round(y)), int(round(x)), WHITE if k == 0 else rgb, a)


# ------------------------------------------------------------------------------------------- narratives
def nar_black_hole(c1, c2, t):
    """Light converges, a horizon forms and grows, the accretion ring flares, Hawking radiation evaporates it."""
    if t < 0.15:
        r = 0.05 + t / 0.15 * 0.2
    elif t < 0.7:
        r = 0.25 + 0.13 * math.sin((t - 0.15) / 0.55 * math.pi / 2)
    else:
        r = 0.33 * (1 - (t - 0.7) / 0.3) ** 0.7 + 0.05      # ends exactly where the loop begins
    ring = 0.4 + 0.75 * r
    parts = [Part(lambda p: sd_sphere(p, r), 0), Part(lambda p: sd_torus(p, ring, 0.04 + r * 0.08), 1, rot=rotx(0.35))]
    mats = [Material('#000000', '#000000', metal=0.0, gloss=0.0), Material(c2, '#ffffff', emissive=c2, pattern=I.pat_bands(12, 2, t, 0.3, axis=0))]
    mats[0].ramp[:] = np.array([0.0, 0.0, 0.02])
    def ov(img):
        ov_infall(img, t, 3, I.H(c2), 10)
        if t > 0.88:
            k = 1 - abs(t - 0.94) / 0.06
            n = img.shape[0]; c = (n - 1) // 2
            for dy in range(-3, 4):
                for dx in range(-3, 4):
                    _blend(img, c + dy, c + dx, WHITE, max(0, k * (1 - math.hypot(dx, dy) / 4)))
    return parts, mats, dict(view_pitch=0.25, view_yaw=0.5 + TAU * t), ov


def nar_big_bang(c1, c2, t):
    """A point flashes, inflates into a spinning galaxy-filled sphere, then crunches back to a point."""
    if t < 0.1:
        r = 0.04 + t / 0.1 * 0.2
    elif t < 0.8:
        r = 0.24 + 0.44 * math.sin(min(1, (t - 0.1) / 0.35) * math.pi / 2)
    else:
        r = 0.68 * (1 - (t - 0.8) / 0.2) + 0.04
    parts, mats, view, _ = I.b_universe(c1, c2, t)
    parts = [Part(lambda p: sd_sphere(p, r), 0)]
    view = dict(view, view_yaw=view.get('view_yaw', 0.3) + TAU * t)
    def ov(img):
        if t < 0.12 or t > 0.95:
            k = 1 - min(t, abs(1 - t)) / 0.12
            n = img.shape[0]; c = (n - 1) // 2
            for dy in range(-5, 6):
                for dx in range(-5, 6):
                    _blend(img, c + dy, c + dx, WHITE, max(0, k * (1 - math.hypot(dx, dy) / 6)))
        ov_sparkles(img, t, 9, I.H(c2), 5, 2)
    return parts, mats, view, ov


def nar_omega(c1, c2, t):
    """Everything converges on a single point, which flares and resets the cycle."""
    parts, mats, view, ov0 = I.b_omega(c1, c2, t)
    s = 1 - 0.5 * ((t * 1.0) % 1.0)
    for p in parts[1:]:
        p.rot = p.rot * 1
    view = dict(view, zoom=view.get('zoom', 1.0) * (0.75 + 0.35 * s), view_yaw=view.get('view_yaw', 0.6) + TAU * t)
    def ov(img):
        if ov0: ov0(img)
        ov_infall(img, t, 5, I.H(c2), 12)
        if t > 0.9:
            ov_rays(img, (t - 0.9) * 10, 1, WHITE, 1)
    return parts, mats, view, ov


def nar_tree(c1, c2, t, base=None):
    """A tree of light grows level by level, holds, then dissolves into its seed."""
    grow = min(1.0, t / 0.6) if t < 0.85 else (1 - (t - 0.85) / 0.15)
    depth = max(0, min(3, int(grow * 4)))
    mats = [Material(c1, c2, metal=0.6, emissive=c1, pattern=I.pat_bands(8, 2, t, 0.25, axis=1)),
            Material(c2, '#ffffff', emissive=c2, pattern=I.pat_pulse_core(t)),
            Material('#20283a', c1, metal=1.0)]
    parts = []
    def br(a, d, ln, lvl, yaw):
        b = a + d * ln
        parts.append(Part(lambda p, a=a, b=b, r=(0.05 + 0.035 * (3 - lvl)): sd_capsule(p, a, b, r), 0))
        if lvl == depth:
            parts.append(Part(lambda p, c=b: sd_sphere(p - c, 0.11 + 0.03 * math.sin(TAU * t * 2)), 1))
            return
        for k in (-1, 1):
            y2 = yaw + (1.57 if lvl % 2 == 0 else 0)
            side = np.array([math.cos(y2), 0, math.sin(y2)])
            nd = d * math.cos(0.62) + side * k * math.sin(0.62)
            br(b, nd / np.linalg.norm(nd), ln * 0.7, lvl + 1, y2)
    br(np.array([0, -0.55, 0.0]), np.array([0, 1.0, 0]), 0.45, 0, 0.4)
    parts.append(Part(lambda p: sd_cyl_(p), 2))
    return parts, mats, dict(view_pitch=0.3, view_yaw=0.6 + TAU * t), (lambda img: ov_embers(img, t, 4, I.H(c2), 5))


def sd_cyl_(p):
    from render3d import sd_cyl
    return sd_cyl(p - np.array([0, -0.66, 0]), 0.55, 0.06, 0.02)


def nar_droste(c1, c2, t):
    """Russell's set contains itself: an endless zoom into nested copies of the same rings."""
    parts, mats = [], [Material(c1, c2, metal=0.8), Material(c2, '#ffffff', emissive=c2, pattern=I.pat_all())]
    # every level is identical, so after one loop each ring has become its outer neighbour: a seamless infinite zoom
    for k in range(-1, 7):
        r = 0.78 * 0.62 ** (k + t)
        if r > 0.95 or r < 0.05:
            continue
        w = 0.05 * r / 0.6 + 0.008
        parts.append(Part(lambda p, r=r, w=w: sd_torus(p, r, w), 0, rot=rotx(0.9)))
        parts.append(Part(lambda p, r=r, w=w: sd_torus(p, r, w), 0, rot=rotz(1.4)))
    parts.append(Part(lambda p: sd_sphere(p, 0.05), 1))
    return parts, mats, dict(view_pitch=0.3, view_yaw=0.5 + TAU * t), None


def nar_infinity(c1, c2, t):
    parts, mats, view, _ = I.b_lemniscate(c1, c2, t)
    def path(tt):
        a = tt * TAU
        d = 1 + math.sin(a) ** 2
        return 15.5 + 12.5 * math.cos(a) / d, 15.5 + 8 * math.sin(a) * math.cos(a) / d
    def ov(img):
        ov_tracer(img, t, 1, I.H(c2), path, 10)
    return parts, mats, view, ov


def nar_white_hole(c1, c2, t):
    parts, mats, view, _ = I.b_whitehole(c1, c2, t)
    def ov(img):
        ov_rays(img, t, 2, I.H(c2), 3, fade_in=0.2)
        ov_sparkles(img, t, 7, WHITE, 4, 2)
    return parts, mats, view, ov


def nar_kugelblitz(c1, c2, t):
    """Light from every direction focuses onto one point until it collapses into a seed of darkness."""
    parts, mats, view, _ = I.b_orb(c1, c2, t, emissive=c1, pattern=I.pat_pulse_core(t), r=0.25 + 0.15 * (t if t < 0.8 else (1 - t) * 4))
    def ov(img):
        ov_infall(img, t, 11, I.H(c2), 16)
    return parts, mats, dict(view, view_yaw=view.get('view_yaw', 0.6) + TAU * t), ov


def nar_fractal_shard(c1, c2, t):
    parts, mats, view, ov0 = I.b_gem(c1, c2, t, cluster=True, glowcore=True, spin=True)
    def ov(img):
        ov_fractal(img, t, 1, I.H(c2))
        if ov0: ov0(img)
    return parts, mats, view, ov


def nar_cosmic_string(c1, c2, t):
    parts, mats, view, _ = I.b_cosmic_string(c1, c2, t)
    hue_shift_materials(mats, 360 * t)
    def ov(img):
        ov_embers(img, t, 2, I.H(c2), 6)
    return parts, mats, view, ov


def nar_time_loop(c1, c2, t):
    parts, mats, view, _ = I.b_lemniscate(c1, c2, t)
    def path(tt):
        a = tt * TAU
        d = 1 + math.sin(a) ** 2
        return 15.5 + 12.5 * math.cos(a) / d, 15.5 + 8 * math.sin(a) * math.cos(a) / d
    def ov(img):
        ov_tracer(img, t, 1, I.H(c2), path, 6)
        ov_tracer(img, (t + 0.5) % 1, 2, I.H(c1), path, 6)      # the same object, half a loop in its own past
    return parts, mats, view, ov


NARRATIVES = {
    'micro_singularity': nar_black_hole, 'pocket_universe': nar_big_bang, 'universe_seed': nar_big_bang,
    'bubble_universe_sample': nar_big_bang, 'omega_point': nar_omega, 'tree_simulacrum': nar_tree, 'omnilex': nar_tree,
    'russell_paradox_set': nar_droste, 'absolute_infinity': nar_infinity, 'white_hole_fragment': nar_white_hole,
    'kugelblitz_seed': nar_kugelblitz, 'aleph_null_shard': nar_fractal_shard, 'cosmic_string_segment': nar_cosmic_string,
    'closed_timelike_curve': nar_time_loop,
}

# ------------------------------------------------------------------------------------------- per-item recipes
# Palettes of motion/overlay combos per broad category. Items that share a model get *different* entries,
# offset by their index, so no two similar items animate the same way.
PALETTES = {
    'crystal': [(('rock',), ('sparkles', 'glint')), (('spin',), ('sparkles',)), (('bob',), ('glint', 'sparkles')),
                (('tumble',), ('sparkles',)), (('rock', 'hue_soft'), ('glint',)), (('bob', 'breathe'), ('sparkles', 'embers'))],
    'metal': [((), ('glint',)), (('rock',), ('glint', 'embers')), (('bob',), ('glint',)), (('tumble',), ('glint', 'sparkles'))],
    'orb': [(('pulse',), ('orbit',)), (('spin', 'breathe'), ('orbit',)), (('bob',), ('embers', 'sparkles')),
            (('heartbeat',), ('orbit', 'glint')), (('spin_back', 'pulse'), ('sparkles',))],
    'device': [(('breathe',), ('glint',)), (('rock',), ('sparkles',)), (('bob', 'breathe'), ('glint',)), (('tumble',), ('embers',)),
               (('rock',), ('glitch',))],
    'chip': [(('breathe',), ('glint',)), (('rock', 'breathe'), ('glint',)), (('bob',), ('sparkles',)), (('pulse',), ('glint',))],
    'flat': [(('rock',), ('glint',)), (('bob',), ('glint',)), (('tumble',), ('glint',)), (('rock', 'breathe'), ('sparkles',))],
    'strange': [(('spin', 'hue'), ('sparkles',)), (('tumble', 'hue'), ('glint', 'sparkles')), (('rock',), ('glitch', 'sparkles')),
                (('bob', 'hue'), ('orbit',))],
}
CATEGORY = {}
for _art_set, _cat in (
        (('crystal', 'shard', 'multishard', 'gem', 'lens'), 'crystal'),
        (('ingot', 'plate', 'casimir', 'sail', 'shell', 'chunk', 'remnant'), 'metal'),
        (('ghostorb', 'pellet', 'monopole', 'quarkstar', 'star', 'seed', 'useed', 'contained', 'bloch', 'foam', 'catom', 'progmatter', 'omega'), 'orb'),
        (('analyzer', 'codex', 'shifter', 'emitter', 'capacitor', 'coil', 'spindle', 'satellite', 'probe', 'anchor', 'artifact', 'damper', 'clock',
          'warp', 'neuron', 'brain', 'realanchor', 'throat', 'negentropy'), 'device'),
        (('chip', 'demonchip', 'aicore', 'code', 'holo', 'hexgrid'), 'chip'),
        (('brane', 'manifold', 'string', 'qstring', 'edgeflow', 'lattice', 'lambda', 'tesseract', 'universe', 'planck', 'loop', 'streak',
          'pair', 'dots', 'swarm', 'singularity', 'whitehole'), 'strange')):
    for _a in _art_set:
        CATEGORY[_a] = _cat
HUE_ITEMS = {'multiverse_shard', 'superposition_crystal', 'psionic_crystal', 'law_crystal', 'calabi_yau_manifold', 'm_brane', 'quantum_string'}
OVERLAY_FN = {'glint': ov_glint, 'sparkles': ov_sparkles, 'orbit': ov_orbit, 'embers': ov_embers, 'glitch': ov_glitch}


RECIPE_OVERRIDE = {}
FRAMES_OVERRIDE = {}


def recipe_for(item, art, index_in_group):
    if item in RECIPE_OVERRIDE:
        return RECIPE_OVERRIDE[item]
    cat = CATEGORY.get(art, 'device')
    pal = PALETTES[cat]
    motions, overlays = pal[(index_in_group + h(item) % 2) % len(pal)]
    if item in HUE_ITEMS and 'hue' not in motions:
        motions = tuple(motions) + ('hue',)
    return motions, overlays


def frames_for(item):
    if item in FRAMES_OVERRIDE:
        return FRAMES_OVERRIDE[item]
    # 20 fps playback (frametime 1): 2 s loops, 4 s for the story animations
    return 80 if item in NARRATIVES else 40


def render_frame(item, art, c1, c2, t, index_in_group=0):
    seed = h(item)
    if item in NARRATIVES:
        parts, mats, view, ov = NARRATIVES[item](c1, c2, t)
        img = render(parts, mats, **view)
        if ov:
            ov(img)
        return img
    res = I.ITEMS[item](c1, c2, t)
    parts, mats, view = res[0], res[1], dict(res[2])
    ov0 = res[3] if len(res) > 3 else None
    motions, overlays = recipe_for(item, art, index_in_group)
    view = apply_motions(parts, mats, view, t, motions, seed)
    img = render(parts, mats, **view)
    if ov0:
        ov0(img)
    accent = I.H(c2)
    for o in overlays:
        if o == 'glint':
            ov_glint(img, t, seed, start=(seed % 7) / 7)
        elif o == 'sparkles':
            ov_sparkles(img, t, seed, accent, 3 + seed % 3)
        elif o == 'orbit':
            ov_orbit(img, t, seed, accent, 2 + seed % 3, rx=12 + seed % 3, ry=4 + seed % 3, speed=1 + seed % 2)
        elif o == 'embers':
            ov_embers(img, t, seed, accent, 5 + seed % 4)
        elif o == 'glitch':
            ov_glitch(img, t, seed)
    return img


try:
    import art11
    art11.install()
except ImportError:
    pass

try:
    import art14
    art14.install()
except ImportError:
    pass
