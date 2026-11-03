"""Item art group 1 (round 14): transformation animations.

Every item here tells a little story in a 60-80 frame loop (form -> transform -> reform), in the spirit of
Meatballcraft's showpiece materials. All timelines return exactly to their start state at t = 1.
"""
import math
import numpy as np
from items3d import *
import anim

PI = math.pi


# ------------------------------------------------------------------------------------------- helpers
def ss(e0, e1, x):
    u = min(1.0, max(0.0, (x - e0) / (e1 - e0)))
    return u * u * (3 - 2 * u)


def env(t, a, b, c, d):
    """0 before a, ramps to 1 over a..b, holds, ramps back to 0 over c..d."""
    return ss(a, b, t) * (1 - ss(c, d, t))


def lin(e0, e1, x):
    return min(1.0, max(0.0, (x - e0) / (e1 - e0)))


def _hs(a):
    v = np.sin(a * 12.9898 + 4.1) * 43758.5453
    return v - np.floor(v)


def _ing(p, L=0.85, Hh=0.3, W=0.45):
    return np.maximum(sd_box(p, (L, Hh, W), 0.05), (p[..., 1] * 0.95 + np.abs(p[..., 2]) * 0.6 + np.abs(p[..., 0]) * 0.12 - (Hh + 0.12)))


def scaled(fn, s):
    s = max(float(s), 1e-3)
    return lambda p: fn(p / s) * s


def glow(col, accent='#ffffff', pattern=None):
    return Material(col, accent, emissive=col, pattern=pattern or pat_all())


def _dot(img, x, y, rgb, a=1.0):
    anim._blend(img, int(math.floor(y + 0.5)), int(math.floor(x + 0.5)), np.asarray(rgb, float), min(1.0, max(0.0, a)))


def _proj(pt, view):
    """World point -> (x, y) on the 32px image for a static view."""
    cam = rotx(-view.get('view_pitch', 0.5)) @ roty(view.get('view_yaw', 0.7853))
    c = np.asarray(pt, float) @ cam.T
    k = view.get('zoom', 1.0) / view.get('extent', 1.0)
    return (c[0] * k + 1) / 2 * 32 - 0.5, (-c[1] * k + 1) / 2 * 32 - 0.5


def _flash(img, x, y, r, rgb, k):
    """Soft round glow blob (drawn over everything)."""
    if k <= 0.01:
        return
    ri = int(math.ceil(r))
    for dy in range(-ri, ri + 1):
        for dx in range(-ri, ri + 1):
            d = math.hypot(dx, dy) / r
            if d < 1:
                _dot(img, x + dx, y + dy, mix(np.asarray(rgb, float), WHITE, max(0.0, 1 - d * 1.6)), k * (1 - d) ** 0.8)


def _burst(img, x, y, life, rgb, n=8, r0=3, r1=15, rot=0.0, length=3):
    """Rays shooting outward from (x, y) as life runs 0 -> 1."""
    if not 0 < life < 1:
        return
    a0 = 1 - life
    for k in range(n):
        a = TAU * k / n + rot
        r = r0 + (r1 - r0) * life
        for j in range(length):
            rr = r - j * 0.9
            _dot(img, x + math.cos(a) * rr, y + math.sin(a) * rr, WHITE if j == 0 else rgb, a0 * (1 - j / (length + 1)))


def _ring(img, x, y, r, rgb, a, sy=1.0):
    """1px circle (shockwave)."""
    if a <= 0.01 or r <= 0.3:
        return
    n = max(8, int(r * 7))
    for k in range(n):
        ang = TAU * k / n
        _dot(img, x + math.cos(ang) * r, y + math.sin(ang) * r * sy, rgb, a)


def _ghost(img, g, a, tint=None):
    """Composite a second render g *behind* img (afterimages, superposed states)."""
    m = (g[..., 3] > 0.5) & (img[..., 3] < 0.5)
    col = g[..., :3] if tint is None else mix(g[..., :3], np.asarray(tint, float), 0.5)
    img[m, :3] = col[m]
    img[m, 3] = a * g[m, 3]


def _fade(img, k):
    img[..., 3] *= max(0.0, min(1.0, k))


def _invert(img):
    m = img[..., 3] > 0.5
    img[m, :3] = 1 - img[m, :3]


def _pixelate(img, b):
    """Quantise the image into b x b blocks (a Planck-scale mosaic)."""
    n = img.shape[0]
    for y in range(0, n, b):
        for x in range(0, n, b):
            blk = img[y:y + b, x:x + b]
            m = blk[..., 3] > 0.5
            if m.mean() < 0.5:
                blk[...] = 0
                continue
            col = blk[..., :3][m].mean(0)
            blk[..., :3] = col
            blk[..., 3] = 1


ING_VIEW = dict(view_pitch=0.6, view_yaw=0.65, zoom=1.05)


# =========================================================================================== ingots
def negative_mass_ingot(c1, c2, t):
    """Hovers upside-down over its anti-gravity ring; the ring tightens and *pushes*, and the ingot falls UP out of
    sight trailing afterimages (the image inverts at the kick); pink motes then rise from below and condense into a
    new ingot that snaps back onto its hover point."""
    def spots(p, n):
        v = np.sin(p[..., 0] * 10 + t * TAU * 2) * np.sin(p[..., 2] * 10)
        return np.zeros(v.shape), (v > 0.6).astype(float)

    def state(tt):
        charge = env(tt, 0.2, 0.34, 0.36, 0.41)
        u = lin(0.4, 0.58, tt)
        rise = 2.3 * u * u if tt < 0.58 else 0.0
        if tt < 0.58:
            s = 1.0
        else:
            s = ss(0.66, 0.84, tt)
        y = 0.22 + 0.06 * math.sin(TAU * tt * 2) - 0.12 * charge + rise
        return y, s, charge

    y, s, charge = state(t)
    shock = env(t, 0.4, 0.43, 0.43, 0.56)
    R = 0.52 - 0.2 * charge + 0.3 * shock
    ring_col = mix(H(c2), WHITE, 0.6 * charge)
    mats = [Material(c1, c2, metal=0.9, emissive=c2, pattern=spots),
            Material(c2, '#ffffff', emissive=ring_col, pattern=pat_bands(8, -2, t, 0.25 + 0.2 * charge, axis=0))]
    ing = Part(scaled(_ing, s), 0, pos=(0, y, 0), rot=rotz(PI))
    ring = Part(lambda p: sd_torus(p, R, 0.05 + 0.02 * charge), 1, pos=(0, -0.5, 0))
    parts = [ring] + ([ing] if s > 0.02 else [])
    view = dict(ING_VIEW, zoom=0.95)

    def ov(img):
        if 0.4 < t < 0.66:                                       # afterimages of the upward fall
            for k, dt in enumerate((0.02, 0.04, 0.06)):
                yy, s2, _ = state(t - dt)
                if t - dt < 0.4 or s2 < 0.5:
                    continue
                g = render([Part(_ing, 0, pos=(0, yy, 0), rot=rotz(PI))], mats, **view)
                _ghost(img, g, 0.55 - 0.15 * k, tint=H(c2))
        if abs(t - 0.41) < 0.022:
            _invert(img)                                         # the kick: a negative image for a frame or two
        cx, cy = _proj((0, 0.22, 0), view)
        if 0.56 < t < 0.8:                                       # motes rise from below and converge on the hover point
            for i in range(12):
                life = lin(0.56 + 0.012 * i, 0.78, t)
                if life <= 0 or life >= 1:
                    continue
                x0 = 4 + _hs(i * 3.7) * 24
                x = x0 + (cx - x0) * life ** 1.5
                yy = 33 + (cy - 33) * life ** 0.8
                _dot(img, x, yy, WHITE if i % 3 == 0 else H(c2), 0.4 + 0.6 * life)
        _flash(img, cx, cy, 6, H(c2), env(t, 0.76, 0.8, 0.8, 0.88) * 0.9)
        if 0.8 < t < 0.92:
            _ring(img, cx, cy, 3 + (t - 0.8) * 90, H(c2), 1 - (t - 0.8) / 0.12, 0.45)
    return parts, mats, view, ov


def spacetime_alloy_ingot(c1, c2, t):
    """An ingot sagging into its own gravity well with a galaxy in its reflection; the well deepens until the ingot
    sinks into it and vanishes behind an Einstein ring of lensed starlight, which then gives the ingot back."""
    sag_a = 0.14 + 0.45 * env(t, 0.28, 0.45, 0.78, 0.92)
    s = 1 - ss(0.4, 0.56, t) * (1 - ss(0.74, 0.92, t))
    drop = -0.35 * ss(0.4, 0.56, t) * (1 - ss(0.74, 0.92, t))
    ring = env(t, 0.47, 0.6, 0.72, 0.86)

    def sag(p):
        q = p.copy()
        q[..., 1] = q[..., 1] + sag_a * np.exp(-(p[..., 0] / 0.45) ** 2)
        return _ing(q) / (1 + 1.9 * sag_a)

    def sky(p, n):
        x, z = p[..., 0], p[..., 2]
        r = np.sqrt(x * x + z * z * 2)
        a = np.arctan2(z, x)
        arm = (np.sin(a * 2 - r * 12 + t * TAU * 2) > 0.7) & (r < 0.35)
        hh = _hs(np.floor(x * 9) * 5.1 + np.floor(z * 9) * 9.7)
        star = (hh > 0.86 - 0.3 * (sag_a - 0.14)) & (np.sin(hh * 30 + t * TAU * 3) > 0)
        up = n[..., 1] > 0.6 - 0.8 * (sag_a - 0.14)
        return -0.2 * up, ((arm | star | (r < 0.06)) & up).astype(float)

    mats = [Material(c1, c2, metal=0.95, emissive=c2, pattern=sky),
            Material(c2, '#ffffff', emissive=mix(H(c2), WHITE, 0.4), pattern=pat_bands(10, 3, t, 0.3, axis=0)),
            Material('#05060c', c1, metal=0.2, gloss=0.3, keep_dark=True)]
    parts = []
    if s > 0.02:
        parts.append(Part(scaled(sag, s), 0, pos=(0, drop, 0)))
    if ring > 0.02:
        parts.append(Part(lambda p: sd_torus(p, 0.62 * ring, 0.07 * ring), 1, rot=rotx(1.25) @ rotz(0.25)))
        parts.append(Part(lambda p: sd_sphere(p, 0.2 * ring), 2))
    view = dict(ING_VIEW)

    def ov(img):
        if ring > 0.3:                                           # lensed stars stream round the ring
            for k in range(5):
                a = TAU * (t * 3 + k / 5)
                for j in range(4):
                    aa = a - j * 0.18
                    _dot(img, 15.5 + math.cos(aa) * 13 * ring, 15.5 + math.sin(aa) * 13 * ring, WHITE if j == 0 else H(c2),
                         ring * (1 - j / 4))
        _flash(img, 15.5, 16.5, 7, H(c2), env(t, 0.52, 0.56, 0.56, 0.62) * 0.8)
        _flash(img, 15.5, 16, 7, H(c2), env(t, 0.82, 0.86, 0.86, 0.94) * 0.7)
        if t < 0.3 or t > 0.92:
            anim.ov_glint(img, (t + 0.1) % 1, 0, start=0.0)
    return parts, mats, view, ov


def stellar_iron_ingot(c1, c2, t):
    """Rusty star-iron heats from below until it swells into a little star; the star burns, then its iron core
    collapses to a point (fusion stops at iron), flashes as a supernova, and the debris falls back in and cools
    into a new ingot."""
    heat = ss(0.08, 0.28, t) * (1 - ss(0.8, 0.98, t))
    star = env(t, 0.24, 0.4, 0.62, 0.7)                       # ingot -> sphere morph weight
    collapse = ss(0.55, 0.64, t)
    reborn = ss(0.68, 0.84, t)
    if t < 0.64:
        s = 1 + 0.15 * star - 0.95 * collapse
    else:
        s = max(0.02, reborn)
    w = env(t, 0.24, 0.4, 0.64, 0.7)

    def shape(p):
        return (1 - w) * _ing(p) + w * sd_sphere(p, 0.55)

    def hot(p, n):
        lvl = -0.3 + 0.9 * heat + 0.05 * np.sin(p[..., 0] * 9 + t * TAU * 4)
        h = (p[..., 1] < lvl).astype(float)
        conv = np.sin(p[..., 0] * 11 + t * TAU * 3) * np.sin(p[..., 2] * 11 - t * TAU * 2) * np.sin(p[..., 1] * 9)
        lat = (np.abs(np.mod(p[..., 0] * 6, 1) - 0.5) < 0.12) & (np.abs(np.mod(p[..., 2] * 6, 1) - 0.5) < 0.12)
        em = ((h > 0) & (star < 0.4)) | ((conv > 0.05) & (star >= 0.4))
        return 0.12 * lat * (1 - heat) + 0.25 * star, em.astype(float)

    ecol = mix(H('#ffa050'), H('#fff4c0'), star)
    mats = [Material(mix(H(c1), H('#ff8030'), 0.4 * heat + 0.5 * star), c2, metal=0.75, emissive=ecol,
                     pattern=pat_combine(hot, pat_noise_shade(14, 0.15 * (1 - star), 3)))]
    parts = [Part(scaled(shape, s), 0)] if s > 0.03 else []
    view = dict(ING_VIEW, zoom=1.0)
    spots = [(9, 10), (20, 9), (14, 13), (24, 13)]

    def ov(img):
        if t < 0.2 or t > 0.92:                                  # fusion sparks that fizzle (iron cannot fuse)
            k = int(t * 20) % 4
            ph = (t * 20) % 1
            x, y = spots[k]
            _dot(img, x, y, WHITE, math.sin(ph * PI))
            _dot(img, x + 1, y, H(c2), 0.6 * math.sin(ph * PI))
        if 0.36 < t < 0.6:
            anim.ov_rays(img, (t - 0.36) / 0.24 * 2 % 1, 0, H('#ffc060'), 1)
        _flash(img, 15.5, 15.5, 9, H('#fff0c0'), env(t, 0.62, 0.645, 0.65, 0.72))
        _burst(img, 15.5, 15.5, lin(0.64, 0.8, t), H('#ffa040'), 12, 3, 17, 0.2, 4)
        if 0.66 < t < 0.84:                                      # debris falls back in and condenses
            for i in range(14):
                life = lin(0.66 + 0.006 * i, 0.84, t)
                if 0 < life < 1:
                    a = TAU * i / 14 + 0.6 * life
                    r = 15 * (1 - life) ** 1.3 + 2
                    _dot(img, 15.5 + math.cos(a) * r, 15.5 + math.sin(a) * r * 0.75, H('#ff9050') if i % 2 else H(c2), 0.4 + 0.6 * life)
    return parts, mats, view, ov


def unbihexium_ingot(c1, c2, t):
    """A heavy violet ingot that tries to fission: it cracks in half and the two halves drift apart, exposing a
    spinning nucleus wrapped in magic-number shells - then the island of stability wins, the halves slam back and a
    shell-shaped shockwave rings out."""
    g = 0.44 * ss(0.22, 0.38, t) * (1 - lin(0.62, 0.69, t) ** 2)
    core = env(t, 0.26, 0.38, 0.6, 0.69)
    ph = t % 0.25
    beat = math.exp(-(min(ph, 0.25 - ph) / 0.05) ** 2) * (1 - core)

    def contours(p, n):
        x, z = p[..., 0], p[..., 2]
        r = np.sqrt(np.maximum((x / 1.6) ** 2 + z ** 2, 0))
        ring = np.abs(np.mod(r * 9 - t * 4, 1) - 0.5) < 0.12 + 0.08 * beat
        cut = np.abs(x) < 0.025
        face = (np.abs(x) < 0.02) & (core > 0.05)
        return -0.35 - 0.4 * face, ((ring & (p[..., 1] > 0.26) & (r < 0.3)) | (cut & ~face)).astype(float)

    def half(sign):
        return lambda p: np.maximum(_ing(p), -sign * p[..., 0])

    mats = [Material(c1, c2, metal=0.6, gloss=0.45, emissive=c2, pattern=contours),
            Material(c2, '#ffffff', emissive=c2, pattern=pat_pulse_core(t)),
            glow('#ff60b0', pattern=pat_combine(pat_all())),
            Material('#b0a8e0', '#ffffff', metal=0.5, emissive='#e0d8ff', pattern=pat_spots(t, 0.6, 4)),
            Material(c2, '#ffffff', emissive=mix(H(c2), WHITE, 0.3), pattern=pat_bands(10, 2, t, 0.3, axis=0))]
    parts = [Part(half(1), 0, pos=(g, 0, 0), rot=rotz(-0.5 * g)),
             Part(half(-1), 0, pos=(-g, 0, 0), rot=rotz(0.5 * g))]
    if core < 0.6:
        parts.append(Part(lambda p: sd_sphere(p, 0.13 + 0.03 * beat), 1, pos=(0, 0.31 * (1 - core), 0)))
    if core > 0.02:
        sc = 0.3 * core
        rc = roty(TAU * t * 2) @ rotx(0.5)
        prot = [np.array(v) * sc for v in ((1, 0, 0), (-0.5, 0.8, 0.3), (-0.4, -0.7, 0.5), (0, 0, -1))]
        neut = [np.array(v) * sc for v in ((-1, 0, 0), (0.5, -0.8, -0.3), (0.4, 0.7, -0.5), (0, 0, 1), (0, 0, 0))]
        parts.append(Part(lambda p: np.min([sd_sphere(p - q, 0.14 * core) for q in prot], 0), 2, pos=(0, 0.22, 0), rot=rc))
        parts.append(Part(lambda p: np.min([sd_sphere(p - q, 0.14 * core) for q in neut], 0), 3, pos=(0, 0.2, 0), rot=rc))
        parts.append(Part(lambda p: sd_torus(p, 0.44 * core, 0.03), 4, pos=(0, 0.2, 0), rot=rotx(1.2) @ rotz(TAU * t)))
        parts.append(Part(lambda p: sd_torus(p, 0.38 * core, 0.03), 4, pos=(0, 0.2, 0), rot=rotz(1.2) @ rotx(-TAU * t)))
    view = dict(ING_VIEW, zoom=1.0 - 0.15 * ss(0.2, 0.36, t) * (1 - ss(0.6, 0.7, t)))

    def ov(img):
        cx, cy = _proj((0, 0.2, 0), view)
        if 0.38 < t < 0.62:                                      # alpha particles try to escape and are pulled back
            for i in range(4):
                ph = ((t - 0.38) * 6 + i / 4) % 1
                a = TAU * i / 4 + 0.4
                r = 3 + 6 * math.sin(ph * PI)
                _dot(img, cx + math.cos(a) * r, cy + math.sin(a) * r * 0.7, WHITE, math.sin(ph * PI))
        _flash(img, cx, cy, 6, H(c2), env(t, 0.67, 0.69, 0.7, 0.76))
        for k in range(2):
            life = lin(0.69 + 0.05 * k, 0.86 + 0.05 * k, t)
            if 0 < life < 1:
                _ring(img, cx, cy, 3 + life * 14, H(c2), (1 - life) * 0.9, 0.5)
    return parts, mats, view, ov


def mirror_alloy_ingot(c1, c2, t):
    """A chequered matter/antimatter ingot lifts off a Penning-field mirror that unfolds beneath it; its antimatter
    reflection grows out of the mirror upside-down. The two hum in counter-phase, slam toward each other, the field
    flashes to keep them apart, and the reflection sinks back into the mirror."""
    L = ss(0.18, 0.36, t) * (1 - lin(0.6, 0.68, t) ** 2 * 0.85) * (1 - ss(0.68, 0.84, t))
    hum = 0.04 * math.sin(TAU * t * 6) * env(t, 0.34, 0.4, 0.56, 0.6)
    up_y = 0.46 * L + hum
    plane = 0.3 * L - 0.3
    flash = env(t, 0.64, 0.67, 0.68, 0.74)

    def chk(anti):
        def f(p, n):
            c = (np.floor(p[..., 0] * 5) + np.floor(p[..., 2] * 5) + np.floor(p[..., 1] * 5)) % 2
            if anti:
                c = 1 - c
            edge = (np.abs(((p[..., 0] * 5) % 1) - 0.5) > 0.42) | (np.abs(((p[..., 2] * 5) % 1) - 0.5) > 0.42)
            hm = 0.5 + 0.5 * np.sin(p[..., 0] * 6 - t * TAU * 4)
            return (c - 0.5) * 0.5, (edge & (hm > 0.3 - 0.8 * flash)).astype(float)
        return f

    def refl(p):
        sy = max(L, 1e-3)
        q = p.copy()
        q[..., 1] = (plane + (plane - p[..., 1]) / sy) - up_y
        return _ing(q) * min(1.0, sy)

    def grid(p, n):
        g = (np.abs(((p[..., 0] * 6) % 1) - 0.5) < 0.12) | (np.abs(((p[..., 2] * 6) % 1) - 0.5) < 0.12)
        return np.zeros(g.shape), (g | (flash > 0.3)).astype(float)

    mats = [Material('#5a6070', c2, metal=0.9, pattern=chk(False), emissive=c2),
            Material(mix(H(c2), H('#401838'), 0.6), c2, metal=0.9, pattern=chk(True), emissive=WHITE),
            Material(c2, '#ffffff', emissive=mix(H(c2), WHITE, 0.3 + 0.5 * flash), pattern=grid)]
    parts = [Part(_ing, 0, pos=(0, up_y, 0))]
    if L > 0.03:
        parts.append(Part(refl, 1))
        parts.append(Part(lambda p: sd_box(p, (0.98 * L, 0.012, 0.62 * L), 0.0), 2, pos=(0, plane, 0)))
    sp = ss(0.15, 0.36, t) * (1 - ss(0.68, 0.84, t))
    view = dict(ING_VIEW, zoom=1.05 - 0.27 * sp, view_pitch=0.6 - 0.28 * sp)

    def ov(img):
        cx, cy = _proj((0, plane, 0), view)
        _flash(img, cx, cy, 8, H(c2), flash)
        if flash > 0.1:
            _burst(img, cx, cy, lin(0.64, 0.78, t), H(c2), 6, 4, 15, 0.0, 3)
        if t < 0.15 or t > 0.9:
            anim.ov_glint(img, (t + 0.12) % 1, 0, start=0.0)
    return parts, mats, view, ov


# =========================================================================================== endgame items
def proof_of_everything(c1, c2, t):
    """The end-of-proof tombstone (a black block on a plinth) is written line by line, gold scan-line at the work
    front; complete, it flares Q.E.D. with gold rays; then it is taken back down, line by line, into golden embers -
    and the proof begins again."""
    tt = (t + 0.45) % 1.0                                      # t=0 shows the finished proof
    build = lin(0.0, 0.32, tt)
    unb = lin(0.72, 0.98, tt)
    steps = 8
    hb = math.floor(build * steps) / steps + (build * steps % 1) ** 3 / steps if build < 1 else 1
    hu = math.floor(unb * steps) / steps if unb > 0 else 0
    frac = max(0.0, min(1.0, hb - hu))
    ytop = -0.4 + 0.96 * frac
    qed = env(tt, 0.32, 0.36, 0.4, 0.62)

    def edges(p, n):
        q = np.abs(p) / np.array([0.36, 0.48, 0.2])
        e = ((q > 0.86).sum(-1) >= 2)
        sweep = np.sin((p[..., 0] + p[..., 1]) * 6 - t * TAU * 3) > -0.3 - 1.5 * qed
        inset = (np.abs(np.abs(p[..., 0]) - 0.24) < 0.03) & (np.abs(p[..., 1]) < 0.36) | (np.abs(np.abs(p[..., 1]) - 0.36) < 0.03) & (np.abs(p[..., 0]) < 0.24)
        inset = inset & (n[..., 2] < -0.5)
        front = np.abs(p[..., 1] + 0.08 - ytop) < 0.06
        lines = (np.abs(np.mod(p[..., 1] * 8.3, 1) - 0.5) < 0.07) & (np.abs(p[..., 0]) < 0.2) & (qed > 0.3) & (n[..., 2] < -0.5)
        return -0.1 * inset, ((e & sweep) | inset | front | lines).astype(float)

    mats = [Material('#14101c', c2, metal=0.9, gloss=1.5, emissive=mix(H(c2), WHITE, 0.4 * qed), pattern=edges, keep_dark=True),
            Material('#3a3040', c2, metal=0.8, emissive=c2, pattern=pat_bands(6, 2, t, 0.08 + 0.2 * qed, axis=0))]
    parts = [Part(lambda p: sd_box(p, (0.5, 0.07, 0.3), 0.02), 1, pos=(0, -0.48, 0)),
             Part(lambda p: sd_box(p, (0.62, 0.06, 0.38), 0.02), 1, pos=(0, -0.6, 0))]
    if frac > 0.01:
        parts.insert(0, Part(lambda p: np.maximum(sd_box(p, (0.36, 0.48, 0.2), 0.02), p[..., 1] + 0.08 - ytop), 0, pos=(0, 0.08, 0)))
    view = dict(view_pitch=0.2, view_yaw=0.45, zoom=1.05)

    def ov(img):
        cx, cy = _proj((0, 0.1, 0), view)
        if qed > 0.05:
            anim.ov_rays(img, (tt - 0.32) / 0.3 * 2 % 1, 0, H(c2), 1)
            _flash(img, cx, cy - 1, 5, H(c2), env(tt, 0.32, 0.345, 0.35, 0.42) * 0.9)
        fx, fy = _proj((0.0, ytop, 0), view)
        if 0 < build < 1:                                       # quill sparks at the writing front
            sx = fx + 6 * math.sin(tt * TAU * 9)
            _dot(img, sx, fy, WHITE, 1)
            _dot(img, sx + 1, fy - 1, H(c2), 0.7)
        if 0 < unb < 1:                                         # the block crumbles into rising gold dust
            for i in range(8):
                ph = (tt * 6 + i / 8) % 1
                x = fx - 6 + _hs(i * 2.3 + int(tt * 6)) * 12
                _dot(img, x, fy - ph * 8, H(c2), 1 - ph)
        if 0.4 < tt < 0.72:
            anim.ov_embers(img, t, 55, H(c2), 5, 0.7)
    return parts, mats, view, ov


def _trefoil(p, sc=0.1, r=0.05, n=20):
    db = np.sqrt((p * p).sum(-1)) - sc * 3.3 - r       # bounding sphere: only evaluate the tube near the knot
    m = db < 0.08
    if not m.any():
        return db
    out = db.copy()
    out[m] = _trefoil_tube(p[m], sc, r, n)
    return out


def _trefoil_tube(p, sc, r, n):
    pts = []
    for k in range(n + 1):
        u = TAU * k / n
        pts.append(np.array([math.sin(u) + 2 * math.sin(2 * u), math.cos(u) - 2 * math.cos(2 * u), -math.sin(3 * u)]) * sc)
    d = None
    for k in range(n):
        di = sd_capsule(p, pts[k], pts[k + 1], r)
        d = di if d is None else np.minimum(d, di)
    return d


def spacetime_catalyst(c1, c2, t):
    """A trefoil knot of curved spacetime spins above a gravity well. The well deepens, the knot is drawn down the
    throat and vanishes; the throat flares, ripples race out across the grid (the process it catalyses) - and the
    knot climbs back out, unconsumed."""
    D = 0.8 + 0.25 * env(t, 0.25, 0.4, 0.62, 0.8)
    sink = env(t, 0.28, 0.44, 0.62, 0.82)
    ks = 1 - 0.95 * sink
    ky = 0.5 - 0.95 * sink
    fl = env(t, 0.44, 0.48, 0.56, 0.66)

    def well(p):
        x, z = p[..., 0], p[..., 2]
        r = np.sqrt(x * x + z * z)
        hgt = 0.3 - D / (r / 0.15 + 1)
        g = np.sqrt(1 + (D / 0.15 / (r / 0.15 + 1) ** 2) ** 2)
        return np.maximum(np.abs(p[..., 1] - hgt) / g - 0.03, r - 0.8)

    def grid(p, n):
        r = np.sqrt(p[..., 0] ** 2 + p[..., 2] ** 2)
        a = np.arctan2(p[..., 2], p[..., 0])
        circ = np.abs(np.mod(r * 4 + t * (2 + 6 * fl * 0), 1) - 0.5) < 0.12
        spoke = (np.abs(np.mod((a - t * TAU / 6) * 6 / TAU, 1) - 0.5) < 0.06) & (r > 0.2)
        wave = np.abs(r - 0.9 * lin(0.46, 0.7, t)) < 0.08
        return 0.0 * r, (circ | spoke | (wave & (fl > 0.02))).astype(float)

    mats = [Material('#203060', c2, metal=0.7, gloss=0.6, emissive=c2, pattern=grid),
            Material('#ffffff', c2, emissive='#ffffff', pattern=pat_pulse_core(t)),
            Material(c1, c2, metal=0.8, emissive=mix(H(c2), WHITE, 0.5), pattern=pat_bands(10, 3, t, 0.3, axis=0))]
    parts = [Part(well, 0)]
    if fl > 0.02:
        parts.append(Part(lambda p: sd_sphere(p, 0.06 + 0.16 * fl), 1, pos=(0, 0.35 - D, 0)))
    if ks > 0.08:
        parts.append(Part(scaled(lambda p: _trefoil(p, 0.115, 0.055), ks), 2, pos=(0, ky, 0), rot=roty(TAU * t) @ rotx(0.5)))
    view = dict(view_pitch=0.55, view_yaw=0.5, zoom=1.0)

    def ov(img):
        cx, cy = _proj((0, 0.35 - D, 0), view)
        _flash(img, cx, cy, 7, H(c2), fl * 0.9)
        _burst(img, cx, cy, lin(0.46, 0.62, t), H(c2), 8, 2, 14, 0.3, 3)
        if 0.66 < t < 0.85:
            anim.ov_sparkles(img, (t - 0.66) / 0.19 * 0.3, 7, H(c2), 3)
    return parts, mats, view, ov


def fractal_core(c1, c2, t):
    """A gold-lit Sierpinski tetrahedron turns once on its axis, then the camera falls into its top corner: the corner
    swells until the smallest copy has become the whole, flashes - and it is the same object again."""
    rot0 = np.array([[1, -1, 0] / np.sqrt(2), [1, 1, 1] / np.sqrt(3), [1, 1, -2] / np.sqrt(6)])
    V = np.array([0, 0.62 * math.sqrt(3) - 0.36, 0])
    turn = ss(0.0, 0.36, t) * TAU / 3
    s = 2 ** ss(0.4, 0.78, t) if t < 0.81 else 1.0          # x2 is the same fractal: swap back under the flash
    hue = ss(0.4, 0.78, t)
    M = roty(turn) @ rot0

    def sier(p):
        z = (p - np.array([0, -0.36, 0])) @ M / 0.62
        for _ in range(4):
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
        return d / 16 * 0.62

    def zoomed(p):
        return np.maximum(sier(V + (p - V) / s) * s, sd_sphere(p, 1.35))

    def bands(p, n):
        r = np.sqrt(((p - V) ** 2).sum(-1)) / s + 1e-4          # log-radial from the zoom corner: scale-invariant x2
        v = np.sin(np.log(r) / math.log(2) * TAU - t * TAU * 3)
        return np.zeros(r.shape), (v > 0.75).astype(float)

    col = mix(H(c2), H('#ff9060'), math.sin(hue * PI) * 0.6)
    flash = env(t, 0.76, 0.8, 0.82, 0.92)
    mats = [Material(c1, col, metal=0.9, emissive=mix(col, WHITE, 0.5 * flash), pattern=bands)]
    parts = [Part(zoomed, 0)]
    view = dict(view_pitch=0.3, view_yaw=0.5, zoom=1.0)

    def ov(img):
        vx, vy = _proj(V, view)
        _flash(img, vx, vy + 2, 7, col, flash)
        if 0.4 < t < 0.78:
            anim.ov_infall(img, (t - 0.4) / 0.38, 5, col, 6)
        _ring(img, 15.5, 17, 4 + lin(0.8, 0.98, t) * 13, col, (1 - lin(0.8, 0.98, t)) * (t > 0.8))
    return parts, mats, view, ov


def m_theory_core(c1, c2, t):
    """Five coloured string loops - the five string theories - orbit a dark core on their own tilted planes; they
    swing into one plane and fuse into a single white membrane, which inflates into an eleven-dimensional bubble,
    flashes, and splits back into five."""
    cols = ('#ff6060', '#60ff80', '#6080ff', '#ffd040', '#ff60ff')
    S = 1 - env(t, 0.24, 0.42, 0.7, 0.88)                      # 1 = five strings, 0 = one membrane
    inflate = env(t, 0.44, 0.54, 0.6, 0.7)
    parts, mats = [], []
    for k, col in enumerate(cols):
        tilt = mix(np.array(0.75), np.array(0.35 + k * 0.55), S)
        yaw = S * (k * TAU / 5 + TAU * t * (1 if k % 2 == 0 else -1))
        R = (0.42 + 0.06 * k) * S + 0.48 * (1 - S) - 0.2 * inflate
        r = 0.045 * S + 0.14 * (1 - S) + 0.3 * inflate
        cc = mix(H(col), mix(H(c2), WHITE, 0.4), (1 - S) ** 0.7)
        mats.append(Material(cc, '#ffffff', metal=0.6, emissive=cc, pattern=pat_bands(8, 2, t, 0.25 + 0.3 * (1 - S), axis=1)))
        parts.append(Part(lambda p, R=R, r=r: sd_torus(p, R, r), k, rot=roty(float(yaw)) @ rotx(float(tilt))))
    mats.append(Material(c1, c2, metal=0.8, emissive=c2, pattern=pat_spots(t, 0.6, 11)))
    parts.append(Part(lambda p: sd_sphere(p, 0.17 * S + 0.02), 5))
    view = dict(view_pitch=0.35, view_yaw=0.5, zoom=1.0)

    def ov(img):
        _flash(img, 15.5, 15.5, 9, mix(H(c2), WHITE, 0.5), env(t, 0.55, 0.58, 0.6, 0.68) * 0.85)
        _burst(img, 15.5, 15.5, lin(0.58, 0.72, t), H(c2), 11, 6, 16, 0.1, 3)
    return parts, mats, view, ov


def quark_star_core(c1, c2, t):
    """A dense neutron crust that splits into four segments and opens like a fruit, revealing three colour-charged
    quarks bound by flickering gluon strings, swapping colours; the crust snaps shut with a flash and beats."""
    o = 0.42 * env(t, 0.22, 0.36, 0.62, 0.72) ** 1.2
    core = env(t, 0.2, 0.34, 0.66, 0.74)
    yaw0 = TAU * t
    d0 = min(t, 1 - t)
    beat = math.exp(-((t - 0.85) / 0.04) ** 2) + math.exp(-(d0 / 0.04) ** 2) * 0.6

    def wedge(a0, a1):
        def f(p):
            x, y = p[..., 0], p[..., 1]
            d0 = -(x * -math.sin(a0) + y * math.cos(a0))
            d1 = (x * -math.sin(a1) + y * math.cos(a1))
            return np.maximum(sd_sphere(p, 0.52 + 0.03 * beat), np.maximum(d0, d1) + 0.012)
        return f

    def crust(p, n):
        hh = _hs(np.floor(p[..., 0] * 9) * 3.1 + np.floor(p[..., 1] * 9) * 7.3 + np.floor(p[..., 2] * 9) * 1.7)
        sp = (hh > 0.82) & (np.sin(hh * 40 + t * TAU * 2) > 0)
        r = np.sqrt((p ** 2).sum(-1))
        inner = r < 0.49
        rim = inner & (r > 0.42)
        return 0.1 * (hh - 0.5) - 0.35 * inner, ((sp & ~inner) | rim).astype(float)

    mats = [Material(c1, c2, metal=0.6, emissive=mix(H(c2), H('#ff40c0'), 0.5), pattern=crust)]
    parts = []
    for k in range(4):
        a0, a1 = k * PI / 2, (k + 1) * PI / 2
        am = (a0 + a1) / 2 + yaw0
        parts.append(Part(wedge(a0, a1), 0, pos=(math.cos(am) * o, math.sin(am) * o, 0), rot=rotz(yaw0)))
    qcols = ('#ff4040', '#40ff60', '#4070ff')
    shift = int(t * 12) % 3
    if core > 0.02:
        rq = roty(TAU * t * 2) @ rotx(0.4)
        qp = [np.array([math.cos(TAU * k / 3), 0, math.sin(TAU * k / 3)]) * 0.24 * core for k in range(3)]
        for k in range(3):
            mats.append(glow(qcols[(k + shift) % 3]))
            parts.append(Part(lambda p, q=qp[k]: sd_sphere(p - q, 0.13 * core), len(mats) - 1, rot=rq))
        mats.append(Material('#ffffff', c2, emissive=mix(H(c2), WHITE, 0.6), pattern=pat_bands(30, 4, t, 0.35, axis=0)))
        parts.append(Part(lambda p: np.min([sd_capsule(p, (0, 0, 0), q, 0.035 * core) for q in qp], 0), len(mats) - 1, rot=rq))
    view = dict(view_pitch=0.15, view_yaw=0.0, zoom=1.0 - 0.12 * env(t, 0.2, 0.36, 0.62, 0.72))

    def ov(img):
        _flash(img, 15.5, 15.5, 7, H(c2), env(t, 0.68, 0.71, 0.72, 0.8) * 0.8)
        life = lin(0.71, 0.86, t)
        if 0 < life < 1:
            _ring(img, 15.5, 15.5, 6 + life * 10, H(c2), 1 - life, 0.8)
        if core > 0.5:
            anim.ov_infall(img, t * 2 % 1, 3, H(qcols[shift]), 6)
    return parts, mats, view, ov


def planck_shard(c1, c2, t):
    """A white octahedral shard turns; a beam slices through it and the halves fly apart - but at the Planck length
    there is nothing smaller: the image breaks into a coarse mosaic, the halves are dragged back together and fuse
    with a flash."""
    if t < 0.3:
        spin = t / 0.3 * 0.5
    elif t < 0.75:
        spin = 0.5
    else:
        spin = 0.5 + (t - 0.75) / 0.25 * 0.5
    th = TAU * spin
    o = 0.22 * ss(0.33, 0.44, t) * (1 - lin(0.62, 0.7, t) ** 2)
    jit = 0.03 * math.sin(TAU * t * 16) * env(t, 0.44, 0.48, 0.6, 0.64)
    nrm = np.array([0.55, 0.75, -0.35])
    nrm = nrm / np.linalg.norm(nrm)

    def half(sgn):
        return lambda p: np.maximum(sd_octa(p, 0.85), sgn * (p @ nrm) + 0.015)

    rot = rotx(0.3) @ roty(th)
    nw = rot @ nrm
    mats = [Material(c1, c2, metal=1.0, emissive=c2, pattern=pat_grid(6, t, 0.06 + 0.06 * env(t, 0.33, 0.4, 0.6, 0.7))),
            Material(c2, '#ffffff', emissive='#ffffff', pattern=pat_all())]
    if o + abs(jit) > 1e-3:
        parts = [Part(half(1), 0, pos=nw * (o + jit), rot=rot), Part(half(-1), 0, pos=-nw * (o + jit), rot=rot)]
    else:
        parts = [Part(lambda p: sd_octa(p, 0.85), 0, rot=rot)]
    view = dict(view_pitch=0.35, view_yaw=0.5, zoom=0.95)

    def ov(img):
        u = lin(0.28, 0.36, t)                                  # the cutting beam sweeps across
        if 0 < u < 1:
            for x in range(32):
                y = 30 - u * 36 + x * 0.55
                _dot(img, x, y, WHITE, 1.0)
                _dot(img, x, y + 1, H(c2), 0.6)
        b = 4 if 0.5 < t < 0.6 else (2 if 0.46 < t < 0.64 else 0)
        if b:
            _pixelate(img, b)
        cx, cy = 15.5, 15.5
        _flash(img, cx, cy, 6, H(c2), env(t, 0.68, 0.7, 0.71, 0.78))
        _burst(img, cx, cy, lin(0.69, 0.82, t), H(c2), 4, 4, 15, PI / 4, 4)
    return parts, mats, view, ov


def superposition_crystal(c1, c2, t):
    """Two lattice configurations of one crystal: a violet hexagonal prism and a teal cubic cluster. The crystal flickers
    ever faster between them, then holds both at once as overlapping ghosts, is 'measured' with a flash into the
    cubic form, and slowly decoheres back into the hexagonal one."""
    def gemA(p):
        return sd_gem(p, 0.34, 0.9)

    def gemB(p):
        a = sd_box(p, (0.3, 0.3, 0.3), 0.03)
        b = sd_box(p - np.array([0.28, -0.42, 0.1]), (0.17, 0.17, 0.17), 0.02)
        c = sd_box(p - np.array([-0.3, 0.4, -0.05]), (0.15, 0.15, 0.15), 0.02)
        return np.minimum(a, np.minimum(b, c))

    fr = int(round(t * 64))
    if t < 0.2:
        wa = 1.0
    elif t < 0.42:
        per = max(1, int(round(6 - (t - 0.2) / 0.22 * 5)))      # flicker period shrinks 6 -> 1 frames
        wa = 1.0 if (fr // per) % 2 == 0 else 0.0
    elif t < 0.8:
        wa = 0.0
    else:
        wa = ss(0.8, 0.98, t)
    super_ = env(t, 0.42, 0.44, 0.56, 0.58)
    rA = rotz(0.4) @ roty(0.3 + 0.3 * math.sin(TAU * t))
    rB = rotz(-0.3) @ roty(0.8) @ rotx(0.6 + 0.3 * math.sin(TAU * t))

    def morph(p):
        return wa * gemA(p @ rA) + (1 - wa) * gemB(p @ rB)

    col = mix(H(c2), H(c1), wa)
    acc = mix(H(c1), H(c2), wa)
    mats = [Material(col, acc, metal=0.85, gloss=1.4, emissive=mix(col, WHITE, 0.3), pattern=pat_bands(8, 2 if wa > 0.5 else -2, t, 0.12, axis=1)),
            Material(c1, c2, metal=0.85, gloss=1.4, emissive=c1, pattern=pat_bands(8, 2, t, 0.12, axis=1))]
    parts = [Part(morph, 0)]
    view = dict(view_pitch=0.3, view_yaw=0.5)

    def ov(img):
        if super_ > 0.05:                                       # both configurations at once
            g = render([Part(lambda p: gemA(p @ rA), 1, pos=(0.12, 0.04, 0))], mats, **view)
            _ghost(img, g, 0.6, tint=H(c1))
            img[..., 0] = np.roll(img[..., 0], 1, axis=1)
            img[..., 2] = np.roll(img[..., 2], -1, axis=1)
        _flash(img, 15.5, 15.5, 8, H(c2), env(t, 0.56, 0.585, 0.6, 0.66))
        _burst(img, 15.5, 15.5, lin(0.585, 0.72, t), H(c2), 6, 5, 15, 0.3, 3)
        if t < 0.2 or 0.66 < t < 0.8:
            anim.ov_sparkles(img, t, 12, H(c2) if t > 0.5 else H(c1), 3)
    return parts, mats, view, ov


# =========================================================================================== registry
ITEMS = {
    'negative_mass_ingot': negative_mass_ingot, 'spacetime_alloy_ingot': spacetime_alloy_ingot,
    'stellar_iron_ingot': stellar_iron_ingot, 'unbihexium_ingot': unbihexium_ingot,
    'mirror_alloy_ingot': mirror_alloy_ingot, 'proof_of_everything': proof_of_everything,
    'spacetime_catalyst': spacetime_catalyst, 'fractal_core': fractal_core, 'm_theory_core': m_theory_core,
    'quark_star_core': quark_star_core, 'planck_shard': planck_shard, 'superposition_crystal': superposition_crystal,
}

# every story animation is fully authored in its builder: no extra motions or stock overlays
RECIPES = {k: ((), ()) for k in ITEMS}

NARRATIVES = {}

FRAMES = {
    'negative_mass_ingot': 64, 'spacetime_alloy_ingot': 72, 'stellar_iron_ingot': 72, 'unbihexium_ingot': 64,
    'mirror_alloy_ingot': 64, 'proof_of_everything': 72, 'spacetime_catalyst': 64, 'fractal_core': 72,
    'm_theory_core': 72, 'quark_star_core': 64, 'planck_shard': 60, 'superposition_crystal': 64,
}
