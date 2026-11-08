"""Item definitions for the 3D pixel-art renderer. Each builder returns (parts, materials, kwargs, overlay)."""
import math
import numpy as np
from render3d import *

TAU = math.pi * 2
WHITE = np.ones(3)


def H(h):
    return hexc(h)


def lerp(a, b, t):
    return a + (b - a) * t


# ------------------------------------------------------------------------------- emissive patterns
def pat_bands(freq, speed, t, width=0.25, axis=1):
    def f(p, n):
        sp = int(round(speed)) or (1 if speed >= 0 else -1)
        v = np.sin(p[..., axis] * freq - t * TAU * sp)
        return np.zeros(v.shape), (v > 1 - width * 2).astype(float)
    return f


def pat_grid(freq, t, width=0.12):
    def f(p, n):
        g = (np.abs(((p[..., 0] * freq) % 1) - 0.5) < width) | (np.abs(((p[..., 2] * freq) % 1) - 0.5) < width) | (np.abs(((p[..., 1] * freq) % 1) - 0.5) < width)
        pulse = 0.5 + 0.5 * np.sin(p[..., 0] * 3 + p[..., 2] * 2 - t * TAU)
        return np.zeros(g.shape), (g & (pulse > 0.35)).astype(float)
    return f


def pat_spots(t, density=0.9, seed=1):
    def f(p, n):
        h = np.sin(np.floor(p[..., 0] * 7) * 12.9898 + np.floor(p[..., 1] * 7) * 78.233 + np.floor(p[..., 2] * 7) * 37.719 + seed) * 43758.5453
        h = h - np.floor(h)
        tw = 0.5 + 0.5 * np.sin(h * 40 + t * TAU)
        return np.zeros(h.shape), ((h > density) & (tw > 0.4)).astype(float)
    return f


def pat_noise_shade(scale=6, amt=0.25, seed=0):
    def f(p, n):
        v = np.sin(p[..., 0] * scale + seed) * np.sin(p[..., 1] * scale * 1.3 + 2) * np.sin(p[..., 2] * scale * 0.9 + 1)
        return v * amt, np.zeros(v.shape)
    return f


def pat_combine(*fs):
    def f(p, n):
        off = 0
        em = 0
        for g in fs:
            o, e = g(p, n)
            off = off + o
            em = np.maximum(em, e)
        return off, em
    return f


def pat_all():
    def f(p, n):
        return np.zeros(p.shape[:-1]), np.ones(p.shape[:-1])
    return f


def pat_pulse_core(t, r0=0.25):
    def f(p, n):
        r = np.sqrt((p ** 2).sum(-1))
        return np.zeros(r.shape), (np.sin(r * 18 - t * TAU * 2) > 0.2).astype(float)
    return f


# ------------------------------------------------------------------------------- overlays (16x16 pixel ops)
def put(img, pts, rgb, a=1.0):
    # overlays are authored on a 16x16 grid; scale them to the render resolution
    k = img.shape[0] // 16
    for (x, y) in pts:
        if 0 <= x < 16 and 0 <= y < 16:
            img[y * k:(y + 1) * k, x * k:(x + 1) * k, :3] = rgb
            img[y * k:(y + 1) * k, x * k:(x + 1) * k, 3] = np.maximum(img[y * k:(y + 1) * k, x * k:(x + 1) * k, 3], a)


def sparkle(img, x, y, rgb, size=1):
    put(img, [(x, y)], WHITE)
    for d in range(1, size + 1):
        put(img, [(x + d, y), (x - d, y), (x, y + d), (x, y - d)], rgb, 0.85)


# ------------------------------------------------------------------------------- builders
def b_gem(c1, c2, t, tilt=0.3, r=0.36, h=0.98, cluster=False, glowcore=False, spin=False):
    parts = [Part(lambda p: sd_gem(p, r, h), 0, rot=rotz(tilt) @ roty(t * TAU if spin else 0.4))]
    mats = [Material(c1, c2, metal=0.85, pattern=pat_spots(t, 0.94) if glowcore else None, emissive=c2 if glowcore else None)]
    if cluster:
        parts.append(Part(lambda p: sd_gem(p, r * 0.55, h * 0.5), 0, pos=(0.42, -0.42, 0.15), rot=rotz(-0.6)))
        parts.append(Part(lambda p: sd_gem(p, r * 0.5, h * 0.42), 0, pos=(-0.45, -0.5, -0.1), rot=rotz(0.8)))
    def ov(img):
        if (t * 4) % 1 < 0.5:
            sparkle(img, 5 + (0, 1, 2, 1)[int(t * 4) % 4], 4, H(c2))
    return parts, mats, dict(view_pitch=0.35, view_yaw=0.6), ov


def b_shard(c1, c2, t, tilt=0.6):
    shard = lambda p: np.maximum(sd_octa(p / np.array([0.6, 1.0, 0.48]), 1.0) * 0.48, -p[..., 1] - 0.8)
    small = lambda p: sd_octa(p / np.array([0.6, 1.0, 0.48]), 0.5) * 0.48
    parts = [Part(shard, 0, rot=rotz(tilt) @ roty(0.5)),
             Part(small, 0, pos=(0.45 if tilt > 0 else -0.45, -0.45, 0.1), rot=rotz(-tilt * 1.4))]
    mats = [Material(c1, c2, metal=0.7, pattern=pat_spots(t, 0.9), emissive=c2)]
    return parts, mats, dict(view_pitch=0.3, view_yaw=0.5), None


def b_orb(c1, c2, t, rings=0, ring_col='#c0c8d8', pattern=None, emissive=None, r=0.6, satellites=0, sat_col=None, metal=0.4, ring_glow=False):
    parts = [Part(lambda p: sd_sphere(p, r), 0)]
    mats = [Material(c1, c2, metal=metal, pattern=pattern, emissive=emissive)]
    for k in range(rings):
        a = 0.45 + k * 1.1
        parts.append(Part(lambda p, k=k: sd_torus(p, r + 0.2 + k * 0.08, 0.05), 1, rot=rotx(a) @ rotz(0.3 * k + t * TAU * (1 if k % 2 == 0 else -1))))
    if rings:
        mats.append(Material(ring_col, '#ffffff', metal=0.9, pattern=pat_bands(6, 1.0, t, 0.2, axis=0) if ring_glow else None,
                             emissive=ring_col if ring_glow else None))
    for k in range(satellites):
        a = TAU * (t + k / max(satellites, 1))
        col = sat_col[k % len(sat_col)] if sat_col else c2
        mats.append(Material(col, '#ffffff', emissive=col, pattern=pat_all()))
        parts.append(Part(lambda p: sd_sphere(p, 0.13), len(mats) - 1, pos=(math.cos(a) * (r + 0.22), math.sin(a * 2) * 0.15, math.sin(a) * (r + 0.22))))
    return parts, mats, dict(view_pitch=0.3, view_yaw=0.6), None


def b_ingot(c1, c2, t, pattern=None, emissive=None, float_glow=False):
    ing = lambda p: np.maximum(sd_box(p, (0.85, 0.3, 0.45), 0.05), (p[..., 1] * 0.95 + np.abs(p[..., 2]) * 0.6 + np.abs(p[..., 0]) * 0.12 - 0.42))
    parts = [Part(ing, 0, pos=(0, 0.05 * math.sin(t * TAU) if float_glow else 0, 0))]
    mats = [Material(c1, c2, metal=0.9, pattern=pattern, emissive=emissive)]
    return parts, mats, dict(view_pitch=0.6, view_yaw=0.65, zoom=1.05), None


def b_plate(c1, c2, t, pattern=None, emissive=None, thick=0.08, curved=False):
    if curved:
        f = lambda p: np.maximum(np.abs(sd_sphere(p - np.array([0, -1.6, 0]), 2.0)) - thick, sd_box(p, (0.75, 0.6, 0.75)))
    else:
        f = lambda p: sd_box(p, (0.75, thick, 0.75), 0.04)
    parts = [Part(f, 0)]
    mats = [Material(c1, c2, metal=0.8, pattern=pattern, emissive=emissive)]
    if not curved:
        # raised inner panel + accent trim so plates read as machined parts, not flat diamonds
        parts.append(Part(lambda p: sd_box(p, (0.5, thick + 0.07, 0.5), 0.02), 1))
        mats.append(Material(mix(H(c1), H(c2), 0.35), c2, metal=0.9, pattern=pattern or pat_bands(4, 1.0, t, 0.08, axis=0), emissive=emissive or c2))
        for sx in (-1, 1):
            for sz in (-1, 1):
                parts.append(Part(lambda p: sd_sphere(p, 0.08), 0, pos=(sx * 0.62, thick + 0.03, sz * 0.62)))
    return parts, mats, dict(view_pitch=0.6, view_yaw=0.785), None


def b_casimir(c1, c2, t):
    parts = [Part(lambda p: sd_box(p, (0.75, 0.07, 0.75), 0.03), 0, pos=(0, 0.25, 0)),
             Part(lambda p: sd_box(p, (0.75, 0.07, 0.75), 0.03), 0, pos=(0, -0.25, 0)),
             Part(lambda p: sd_box(p, (0.62, 0.15, 0.62)), 1)]
    mats = [Material(c1, '#ffffff', metal=0.9), Material(c2, '#ffffff', emissive=c2, pattern=pat_bands(10, 1.0, t, 0.3, axis=0))]
    return parts, mats, dict(view_pitch=0.55, view_yaw=0.785), None


def b_cell(c1, c2, t, frame='#7a8090', level=0.55, coils=False, core=None, fluid_cell=False):
    fr = H(frame)
    R = 0.36
    if fluid_cell:
        R, level = 0.48, 0.72
        a, b = H(c1), H(c2)
        liq = a if a.max() > 0.3 else mix(b, a, 0.35)
        if liq.max() < 0.45:
            liq = liq / max(liq.max(), 1e-3) * 0.55   # keep near-black fluids readable
        c1 = liq
    parts = [Part(lambda p: sd_cyl(p, R, 0.62, 0.08), 1),
             Part(lambda p: sd_cyl(p - np.array([0, 0.74, 0]), R + 0.06, 0.1, 0.03), 0),
             Part(lambda p: sd_cyl(p + np.array([0, 0.74, 0]), R + 0.06, 0.1, 0.03), 0),
             Part(lambda p: sd_cyl(p - np.array([0, 0.92, 0]), 0.16, 0.08, 0.02), 0)]
    mats = [Material(frame, '#ffffff', metal=0.9)]
    wave = lambda p, n: (np.zeros(p.shape[:-1]), (p[..., 1] < (level * 1.2 - 0.6) + 0.06 * np.sin(np.arctan2(p[..., 2], p[..., 0]) * 3 + t * TAU)).astype(float))
    mats.append(Material(c1, c2, emissive=None, pattern=None, metal=0.1))
    # liquid: emissive below the level line, dark glass above
    mats[1] = Material('#20283a', '#a0c0ff', metal=1.0, emissive=c1, pattern=lambda p, n: (np.zeros(p.shape[:-1]), np.where(wave(p, n)[1] > 0, 1.0, 0.0)))
    for x in (-1, 1):
        for z in (-1, 1):
            parts.append(Part(lambda p, x=x, z=z: sd_cyl(p - np.array([x * R * 0.83, 0, z * R * 0.83]), 0.06, 0.66), 0))
    if coils:
        for y in (-0.35, 0.0, 0.35):
            parts.append(Part(lambda p, y=y: sd_torus(p - np.array([0, y, 0]), 0.42, 0.06), 2))
        mats.append(Material('#c06030', '#ffd0a0', metal=0.9))
    if core:
        parts.append(Part(lambda p: sd_sphere(p - np.array([0, 0.05 * math.sin(t * TAU * 2), 0]), 0.2), len(mats)))
        mats.append(Material(core, '#ffffff', emissive=core, pattern=pat_all()))
    return parts, mats, dict(view_pitch=0.3, view_yaw=0.6, zoom=0.92 if fluid_cell else 1.0), None


def b_cube(c1, c2, t, pattern=None, emissive=None, edges=False, size=0.6):
    parts = [Part(lambda p: sd_box(p, (size, size, size), 0.06), 0)]
    mats = [Material(c1, c2, metal=0.6, pattern=pattern, emissive=emissive)]
    if edges:
        def edge_pat(p, n):
            q = np.abs(p) > size * 0.8
            e = (q.sum(-1) >= 2).astype(float) * (0.5 + 0.5 * np.sin(p.sum(-1) * 6 - t * TAU * 2) > 0.3)
            return np.zeros(e.shape), e
        mats[0] = Material(c1, c2, metal=0.6, pattern=edge_pat, emissive=c2)
    return parts, mats, dict(view_pitch=0.6, view_yaw=0.785), None


def b_chip(c1, c2, t, eye=None):
    parts = [Part(lambda p: sd_box(p, (0.62, 0.09, 0.62), 0.03), 0),
             Part(lambda p: sd_box(p - np.array([0, 0.12, 0]), (0.3, 0.05, 0.3), 0.02), 1)]
    mats = [Material(c1, '#80a0b0', metal=0.4, pattern=pat_grid(5, t, 0.08), emissive=c2),
            Material('#202430', c2, emissive=eye or c2, pattern=pat_pulse_core(t))]
    for i in range(-2, 3):
        for s in (-1, 1):
            parts.append(Part(lambda p, i=i, s=s: sd_box(p - np.array([i * 0.24, -0.02, s * 0.74]), (0.06, 0.04, 0.12)), 2))
            parts.append(Part(lambda p, i=i, s=s: sd_box(p - np.array([s * 0.74, -0.02, i * 0.24]), (0.12, 0.04, 0.06)), 2))
    mats.append(Material('#c8a040', '#fff0c0', metal=1.0))
    return parts, mats, dict(view_pitch=0.75, view_yaw=0.785, zoom=0.95), None


def b_aicore(c1, c2, t):
    body = lambda p: sd_octa(p / np.array([1.0, 0.7, 1.0]), 0.8) * 0.7
    parts = [Part(body, 0, rot=roty(0.785)), Part(lambda p: sd_sphere(p, 0.3), 1)]
    mats = [Material(c1, c2, metal=0.8, pattern=pat_grid(4, t, 0.06), emissive=c2),
            Material(c2, '#ffffff', emissive=c2, pattern=pat_pulse_core(t))]
    parts[1] = Part(lambda p: sd_sphere(p - np.array([0, 0, -0.3]), 0.26), 1)
    return parts, mats, dict(view_pitch=0.25, view_yaw=0.0), None


def b_torus(c1, c2, t, R=0.62, r=0.16, core=None, tilt=0.6, glow=True, count=1):
    parts, mats = [], [Material(c1, c2, metal=0.8, pattern=pat_bands(8, 1.0, t, 0.18, axis=0) if glow else None, emissive=c2 if glow else None)]
    for k in range(count):
        parts.append(Part(lambda p: sd_torus(p, R - k * 0.12, r), 0, rot=rotx(tilt + k * 1.0) @ rotz(k * 0.9 + t * TAU * (k + 1))))
    if core:
        mats.append(Material(core, '#ffffff', emissive=core, pattern=pat_all()))
        parts.append(Part(lambda p: sd_sphere(p, 0.2), 1))
    return parts, mats, dict(view_pitch=0.35, view_yaw=0.5), None


def b_tube_curve(c1, c2, t, fn, n=28, r=0.09):
    pts = [fn(i / n) for i in range(n + 1)]
    parts = []
    for i in range(n):
        a, b = pts[i], pts[i + 1]
        parts.append(Part(lambda p, a=a, b=b: sd_capsule(p, a, b, r), 0))
    phase = t
    def em(p, nrm):
        return np.zeros(p.shape[:-1]), (np.sin(p[..., 0] * 5 + p[..., 1] * 3 - phase * TAU * 2) > 0.5).astype(float)
    mats = [Material(c1, c2, metal=0.6, pattern=em, emissive=c2)]
    return parts, mats, dict(view_pitch=0.4, view_yaw=0.3), None


def b_chunk(c1, c2, t, seed=0, spots=True):
    def f(p):
        d = sd_sphere(p, 0.62)
        return d + 0.09 * np.sin(p[..., 0] * 7 + seed) * np.sin(p[..., 1] * 6 + seed * 2) * np.sin(p[..., 2] * 8 + 1)
    parts = [Part(f, 0)]
    mats = [Material(c1, c2, metal=0.3, pattern=pat_combine(pat_noise_shade(9, 0.25, seed), pat_spots(t, 0.88, seed)) if spots else pat_noise_shade(9, 0.25, seed),
                     emissive=c2 if spots else None)]
    return parts, mats, dict(view_pitch=0.3, view_yaw=0.5), None


def b_calibrator(c1, c2, t):
    """A tuning fork whose prongs ring with a standing wave."""
    hum = 0.015 * math.sin(t * TAU * 4)
    parts = [Part(lambda p: sd_capsule(p, np.array([0, -0.85, 0]), np.array([0, -0.15, 0]), 0.09), 0),
             Part(lambda p: sd_torus(p, 0.22, 0.07), 2, pos=(0, -0.05, 0), rot=rotx(TAU / 4)),
             Part(lambda p: sd_capsule(p, np.array([-0.22 - hum, -0.05, 0]), np.array([-0.22 - hum, 0.82, 0]), 0.07), 2),
             Part(lambda p: sd_capsule(p, np.array([0.22 + hum, -0.05, 0]), np.array([0.22 + hum, 0.82, 0]), 0.07), 2),
             Part(lambda p: sd_sphere(p - np.array([0, 0.55, 0]), 0.11), 1)]
    mats = [Material(c1, '#a0a8b8', metal=0.5), Material(c2, '#ffffff', emissive=c2, pattern=pat_pulse_core(t)),
            Material('#b0b8c8', c2, metal=1.0, emissive=c2, pattern=pat_bands(10, 2, t, 0.08, axis=1))]
    return parts, mats, dict(view_pitch=0.25, view_yaw=0.5, zoom=1.05), None


def b_autocalibrator(c1, c2, t):
    parts = [Part(lambda p: sd_sphere(p, 0.36), 1)]
    mats = [Material('#b0b8c8', c2, metal=1.0, emissive=c2, pattern=pat_bands(8, 2, t, 0.1, axis=1)),
            Material(c1, c2, metal=0.6, emissive=c2, pattern=pat_grid(6, t, 0.08))]
    for k in range(3):
        parts.append(Part(lambda p: sd_torus(p, 0.62, 0.05), 0, rot=rotx(0.5 + k * 1.05) @ rotz(k * 2.1 + t * TAU * (1 if k % 2 else -1))))
    return parts, mats, dict(view_pitch=0.3, view_yaw=0.5), None


def b_assembler(c1, c2, t):
    """A builder's wand projecting a holographic block that assembles itself."""
    parts = [Part(lambda p: sd_capsule(p, np.array([-0.62, -0.75, 0]), np.array([0.0, 0.0, 0]), 0.09), 0),
             Part(lambda p: sd_box(p, (0.14, 0.14, 0.14), 0.03), 2, pos=(0.02, 0.02, 0), rot=rotz(0.785)),
             Part(lambda p: np.maximum(sd_box(p, (0.24, 0.24, 0.24)), -sd_box(p, (0.2, 0.2, 0.5))), 1, pos=(0.42, 0.46, 0), rot=roty(t * TAU) @ rotx(0.5))]
    def holo(p, n):
        scan = np.abs(np.mod(p[..., 1] * 3 - t * 2, 1) - 0.5) < 0.12
        return np.zeros(p.shape[:-1]), (scan | (np.abs(np.abs(p).max(-1) - 0.24) < 0.03)).astype(float)
    mats = [Material(c1, '#a0a8b8', metal=0.6), Material('#203040', c2, emissive=c2, pattern=holo), Material('#c0c8d8', c2, metal=1.0)]
    return parts, mats, dict(view_pitch=0.3, view_yaw=0.6), None


def b_device(c1, c2, t, kind):
    mats = [Material(c1, '#a0a8b8', metal=0.6), Material('#101418', c2, emissive=c2, pattern=None), Material('#8a90a0', '#ffffff', metal=0.9)]
    parts = []
    view = dict(view_pitch=0.35, view_yaw=0.6)
    if kind == 'analyzer':
        parts += [Part(lambda p: sd_box(p, (0.42, 0.62, 0.14), 0.06), 0, pos=(-0.1, -0.05, 0)),
                  Part(lambda p: sd_box(p, (0.3, 0.26, 0.04), 0.02), 1, pos=(-0.1, 0.12, -0.14)),
                  Part(lambda p: sd_capsule(p, (0.25, 0.5, 0), (0.62, 0.9, 0), 0.05), 2)]
        mats[1] = Material('#101418', c2, emissive=c2, pattern=lambda p, n: (np.zeros(p.shape[:-1]), (np.abs(p[..., 1] - 0.08 * np.sin(p[..., 0] * 25 - t * TAU * 2)) < 0.05).astype(float)))
    elif kind == 'codex':
        parts += [Part(lambda p: sd_box(p, (0.55, 0.7, 0.15), 0.03), 0), Part(lambda p: sd_box(p, (0.5, 0.65, 0.16)), 2, pos=(0.04, 0, 0.02))]
        mats[0] = Material(c1, c2, metal=0.3, emissive=c2, pattern=lambda p, n: (np.zeros(p.shape[:-1]), ((np.abs(np.sqrt(p[..., 0] ** 2 + p[..., 1] ** 2) - 0.25 - 0.03 * math.sin(t * TAU)) < 0.04) & (p[..., 2] < -0.1)).astype(float)))
        view = dict(view_pitch=0.2, view_yaw=0.35)
    elif kind == 'shifter':
        parts += [Part(lambda p: sd_capsule(p, (-0.55, -0.7, 0), (0.25, 0.3, 0), 0.08), 2),
                  Part(lambda p: sd_sphere(p - np.array([0.38, 0.48, 0]), 0.24), 1),
                  Part(lambda p: sd_torus(p - np.array([0.38, 0.48, 0]), 0.36, 0.04), 0, rot=rotx(1.2 + t * TAU))]
        mats[1] = Material(c2, '#ffffff', emissive=c2, pattern=pat_pulse_core(t))
    elif kind == 'emitter':
        parts += [Part(lambda p: sd_box(p, (0.3, 0.2, 0.3), 0.04), 2, pos=(0, -0.6, 0)),
                  Part(lambda p: np.maximum(np.abs(sd_sphere(p - np.array([0, 0.5, 0]), 0.8)) - 0.05, p[..., 1] - 0.0), 0),
                  Part(lambda p: sd_sphere(p - np.array([0, -0.05, 0]), 0.14), 1)]
        mats[1] = Material(c2, '#ffffff', emissive=c2, pattern=pat_all())
        view = dict(view_pitch=0.55, view_yaw=0.6)
    elif kind == 'capacitor':
        parts += [Part(lambda p: sd_cyl(p, 0.42, 0.6, 0.08), 0), Part(lambda p: sd_cyl(p - np.array([0, 0.68, 0]), 0.15, 0.1), 2)]
        lvl = 0.5 + 0.5 * math.sin(t * TAU)
        mats[0] = Material(c1, c2, metal=0.5, emissive=c2, pattern=lambda p, n: (np.zeros(p.shape[:-1]), ((p[..., 1] < lvl * 1.1 - 0.55) & (np.abs(np.arctan2(p[..., 2], p[..., 0]) + 2.2) < 0.5)).astype(float)))
    elif kind == 'spindle':
        parts += [Part(lambda p: sd_cyl(p, 0.08, 0.9), 2)]
        for k, y in enumerate((-0.45, 0.0, 0.45)):
            parts.append(Part(lambda p, y=y: sd_cyl(p - np.array([0, y, 0]), 0.5 - abs(y) * 0.4, 0.06, 0.02), 0, rot=roty(t * TAU)))
        mats[0] = Material(c1, c2, metal=0.8, emissive=c2, pattern=pat_bands(16, 1, t, 0.15, axis=0))
    elif kind == 'satellite':
        parts += [Part(lambda p: sd_box(p, (0.24, 0.28, 0.24), 0.05), 0), Part(lambda p: sd_cyl(p - np.array([0, 0.38, 0]), 0.05, 0.14), 2)]
        for s in (-1, 1):
            parts.append(Part(lambda p, s=s: sd_box(p - np.array([s * 0.66, 0, 0]), (0.4, 0.03, 0.34), 0.01), 1))
        mats[1] = Material('#1a2a60', c2, metal=1.0, emissive=c2, pattern=pat_grid(6, t, 0.06))
        view = dict(view_pitch=0.55, view_yaw=0.5)
    elif kind == 'probe':
        parts += [Part(lambda p: sd_cyl(p, 0.22, 0.4, 0.06), 0), Part(lambda p: sd_cyl(p - np.array([0, 0.5, 0]), 0.1, 0.12), 2),
                  Part(lambda p: sd_sphere(p + np.array([0, 0.45, 0]), 0.16), 1)]
        for s in (-1, 1):
            parts.append(Part(lambda p, s=s: sd_box(p - np.array([s * 0.6, 0.05, 0]), (0.34, 0.25, 0.02), 0.01), 3))
        mats[1] = Material(c2, '#ffffff', emissive=c2, pattern=pat_pulse_core(t))
        mats.append(Material('#1a2a60', '#80c0ff', metal=1.0, emissive='#80c0ff', pattern=pat_grid(6, t, 0.06)))
    elif kind == 'anchor':
        parts += [Part(lambda p: sd_capsule(p, (0, -0.6, 0), (0, 0.7, 0), 0.09), 2),
                  Part(lambda p: sd_torus(p - np.array([0, 0.82, 0]), 0.14, 0.05), 2, rot=rotx(1.57)),
                  Part(lambda p: np.maximum(sd_torus(p + np.array([0, 0.25, 0]), 0.5, 0.08), p[..., 1] + 0.25), 2, rot=rotx(1.57)),
                  Part(lambda p: sd_box(p - np.array([0, 0.35, 0]), (0.38, 0.06, 0.06)), 2)]
        parts.append(Part(lambda p: sd_capsule(p, (0.0, -0.4, -0.12), (0.0, 0.5, -0.12), 0.03 + 0.01 * math.sin(t * TAU * 3)), 1))
        mats[1] = Material(c2, '#ffffff', emissive=c2, pattern=pat_all())
    elif kind == 'artifact':
        parts += [Part(lambda p: sd_hexprism(p, 0.55, 0.22), 0, rot=rotx(1.57)), Part(lambda p: sd_sphere(p - np.array([0, 0, -0.22]), 0.2), 1)]
        mats[0] = Material(c1, c2, metal=0.9, emissive=c2, pattern=pat_grid(5, t, 0.05))
        mats[1] = Material(c2, '#ffffff', emissive=c2, pattern=pat_pulse_core(t))
        view = dict(view_pitch=0.25, view_yaw=0.3)
    elif kind == 'damper':
        for k in range(3):
            parts.append(Part(lambda p, k=k: sd_torus(p, 0.7 - k * 0.14, 0.05), 0, rot=rotx(0.5 + k * 1.0 + t * TAU * (1 if k % 2 else -1)) @ rotz(k * 0.8)))
        parts.append(Part(lambda p: sd_sphere(p, 0.2), 1))
        mats[0] = Material(c1, c2, metal=0.9)
        mats[1] = Material(c2, '#ffffff', emissive=c2, pattern=pat_all())
    elif kind == 'clock':
        parts += [Part(lambda p: sd_cyl(p, 0.72, 0.1, 0.04), 2, rot=rotx(-1.3)), Part(lambda p: sd_cyl(p, 0.6, 0.13), 1, rot=rotx(-1.3))]
        a1, a2 = -t * TAU, t * TAU * 3
        def hands(p, n):
            x, y = p[..., 0], p[..., 2]
            ang = np.arctan2(y, x)
            r = np.sqrt(x * x + y * y)
            h1 = (np.abs(np.angle(np.exp(1j * (ang - a1)))) < 0.18) & (r < 0.45)
            h2 = (np.abs(np.angle(np.exp(1j * (ang - a2)))) < 0.25) & (r < 0.3)
            ticks = (r > 0.45) & (np.abs(np.angle(np.exp(1j * ang * 6))) < 0.3)
            return np.zeros(r.shape), (h1 | h2 | ticks).astype(float)
        mats[1] = Material('#2a2008', c2, emissive=c2, pattern=hands)
        view = dict(view_pitch=0.2, view_yaw=0.2)
    elif kind == 'lens':
        parts += [Part(lambda p: sd_sphere(p / np.array([0.8, 0.28, 0.8]), 1.0) * 0.28, 1),
                  Part(lambda p: sd_torus(p, 0.8, 0.07), 2)]
        mats[1] = Material(c1, c2, emissive=c2, pattern=lambda p, n: (np.zeros(p.shape[:-1]), (np.sin(np.arctan2(p[..., 2], p[..., 0]) * 3 + np.sqrt(p[..., 0] ** 2 + p[..., 2] ** 2) * 9 - t * TAU * 2) > 0.6).astype(float)))
        view = dict(view_pitch=0.55, view_yaw=0.6)
    elif kind == 'coil':
        parts += [Part(lambda p: sd_cyl(p, 0.18, 0.8), 2)]
        for k in range(6):
            y = -0.62 + k * 0.25
            parts.append(Part(lambda p, y=y: sd_torus(p - np.array([0, y, 0]), 0.36, 0.08), 0))
        mats[0] = Material(c1, c2, metal=0.95, emissive=c2, pattern=pat_bands(4, 1.5, t, 0.12, axis=1))
        view = dict(view_pitch=0.35, view_yaw=0.6)
    elif kind == 'warp':
        parts += [Part(lambda p: sd_torus(p / np.array([1.0, 1.0, 0.5]), 0.6, 0.12) * 0.5, 0, rot=rotx(1.2)),
                  Part(lambda p: sd_torus(p / np.array([1.0, 1.0, 0.5]), 0.4, 0.09) * 0.5, 0, rot=rotx(1.2))]
        mats[0] = Material(c1, c2, metal=0.9, emissive=c2, pattern=pat_bands(10, 2, t, 0.2, axis=0))
    elif kind == 'neuron':
        parts += [Part(lambda p: sd_sphere(p, 0.28), 1)]
        for k in range(6):
            a = k * TAU / 6 + 0.3
            parts.append(Part(lambda p, a=a: sd_capsule(p, (0, 0, 0), (math.cos(a) * 0.85, math.sin(a) * 0.6, math.sin(a * 2) * 0.3), 0.05), 0))
        mats[0] = Material(c1, c2, metal=0.4, emissive=c2, pattern=pat_bands(12, 2, t, 0.15, axis=0))
        mats[1] = Material(c2, '#ffffff', emissive=c2, pattern=pat_pulse_core(t))
    elif kind == 'brain':
        f = lambda p: sd_sphere(p / np.array([0.8, 0.6, 0.65]), 1.0) * 0.6 + 0.03 * np.sin(p[..., 0] * 18 + np.sin(p[..., 1] * 12) * 2) * np.sin(p[..., 2] * 14)
        parts += [Part(f, 0)]
        mats[0] = Material(c1, c2, metal=0.2, emissive=c2, pattern=pat_combine(pat_noise_shade(14, 0.3), pat_spots(t, 0.93)))
    return parts, mats, view, None


def b_tesseract(c1, c2, t):
    a = t * TAU
    outer = lambda p: np.maximum(sd_box(p, (0.72, 0.72, 0.72)), -np.minimum(np.minimum(sd_box(p, (0.62, 0.62, 2)), sd_box(p, (2, 0.62, 0.62))), sd_box(p, (0.62, 2, 0.62))))
    s = 0.3 + 0.08 * math.sin(t * TAU)
    inner = lambda p: np.maximum(sd_box(p, (s, s, s)), -np.minimum(np.minimum(sd_box(p, (s * 0.7, s * 0.7, 2)), sd_box(p, (2, s * 0.7, s * 0.7))), sd_box(p, (s * 0.7, 2, s * 0.7))))
    parts = [Part(outer, 0, rot=roty(a)), Part(inner, 1, rot=roty(-a * 2) @ rotx(a))]
    mats = [Material(c1, c2, metal=0.9, emissive=c1, pattern=pat_bands(6, 1, t, 0.12, axis=1)), Material(c2, '#ffffff', emissive=c2, pattern=pat_all())]
    return parts, mats, dict(view_pitch=0.5, view_yaw=0.785), None


def b_pair(c1, c2, t):
    a = t * TAU
    p1 = np.array([math.cos(a) * 0.5, 0.1, math.sin(a) * 0.5])
    parts = [Part(lambda p: sd_sphere(p - p1, 0.28), 0), Part(lambda p: sd_sphere(p + p1, 0.28), 1),
             Part(lambda p: sd_capsule(p, p1, -p1, 0.05), 2)]
    mats = [Material(c1, '#ffffff', emissive=c1, pattern=pat_pulse_core(t)), Material(c2, '#ffffff', emissive=c2, pattern=pat_pulse_core(t + 0.5)),
            Material('#ffffff', '#ffffff', emissive='#ffffff', pattern=pat_bands(20, 3, t, 0.25, axis=0))]
    return parts, mats, dict(view_pitch=0.4, view_yaw=0.3), None


def b_network(c1, c2, t, n=7, seed=3, node_r=0.17):
    rng = np.random.default_rng(seed)
    pts = rng.uniform(-0.62, 0.62, (n, 3))
    parts, mats = [], [Material(c1, c2, metal=0.5), Material(c2, '#ffffff', emissive=c2, pattern=pat_pulse_core(t))]
    for i in range(n):
        for j in range(i + 1, n):
            if np.linalg.norm(pts[i] - pts[j]) < 0.8:
                parts.append(Part(lambda p, a=pts[i], b=pts[j]: sd_capsule(p, a, b, 0.065), 0))
        parts.append(Part(lambda p, c=pts[i]: sd_sphere(p - c, node_r), 1))
    return parts, mats, dict(view_pitch=0.35, view_yaw=0.4 + t * TAU), None


def b_swarm(c1, c2, t):
    rng = np.random.default_rng(5)
    parts, mats = [], [Material(c1, c2, metal=0.9), Material(c2, '#ffffff', emissive=c2, pattern=pat_all())]
    for i in range(34):
        a0, r0, h0 = rng.uniform(0, TAU), rng.uniform(0.2, 0.75), rng.uniform(-0.5, 0.5)
        a = a0 + t * TAU * rng.choice([1, 2, -1])
        c = np.array([math.cos(a) * r0, h0, math.sin(a) * r0])
        parts.append(Part(lambda p, c=c: sd_box(p - c, (0.1, 0.1, 0.1), 0.02), 1 if i % 4 == 0 else 0))
    return parts, mats, dict(view_pitch=0.3, view_yaw=0.5), None


def b_quantum_dots(c1, c2, t):
    parts, mats = [], []
    cols = [c1, c2, '#ffd040', '#40a0ff']
    for k, (x, z) in enumerate(((-0.4, -0.4), (0.4, -0.4), (-0.4, 0.4), (0.4, 0.4))):
        mats.append(Material(cols[k], '#ffffff', metal=0.6, emissive=cols[k], pattern=pat_pulse_core(t + k / 4)))
        parts.append(Part(lambda p, x=x, z=z: sd_box(p - np.array([x, 0, z]), (0.24, 0.24, 0.24), 0.08), k))
    return parts, mats, dict(view_pitch=0.6, view_yaw=0.785), None


def b_lemniscate(c1, c2, t):
    def fn(s):
        a = s * TAU
        d = 1 + math.sin(a) ** 2
        return np.array([0.8 * math.cos(a) / d, 0.15 * math.sin(a * 2), 0.8 * math.sin(a) * math.cos(a) / d])
    parts, mats, view, _ = b_tube_curve(c1, c2, t, fn, 30, 0.09)
    p0 = fn(t)
    mats.append(Material('#ffffff', '#ffffff', emissive='#ffffff', pattern=pat_all()))
    parts.append(Part(lambda p: sd_sphere(p - p0, 0.16), 1))
    return parts, mats, dict(view_pitch=0.6, view_yaw=0.2), None


def b_qstring(c1, c2, t):
    def fn(s):
        a = s * TAU
        r = 0.55 + 0.15 * math.sin(a * 3 + t * TAU * 2)
        return np.array([r * math.cos(a), 0.15 * math.sin(a * 3), r * math.sin(a)])
    return b_tube_curve(c1, c2, t, fn, 36, 0.11)[:3] + (None,)


def b_cosmic_string(c1, c2, t):
    def fn(s):
        x = s * 1.7 - 0.85
        return np.array([x, 0.35 * math.sin(s * TAU * 1.5 + t * TAU), 0.25 * math.cos(s * TAU * 1.5 + t * TAU)])
    return b_tube_curve(c1, c2, t, fn, 30, 0.13)


def b_manifold(c1, c2, t):
    parts, mats = [], [Material(c1, c2, metal=0.7, emissive=c2, pattern=pat_bands(8, 1, t, 0.15, axis=0))]
    for k in range(3):
        parts.append(Part(lambda p, k=k: sd_torus(p, 0.5, 0.13), 0, rot=rotx(k * 1.05 + t * TAU) @ rotz(k * 2.1)))
    mats.append(Material('#ffffff', '#ffffff', emissive='#ffffff', pattern=pat_all()))
    parts.append(Part(lambda p: sd_sphere(p, 0.14), 1))
    return parts, mats, dict(view_pitch=0.3, view_yaw=0.4), None


def b_planck(c1, c2, t):
    parts = [Part(lambda p: sd_octa(p, 0.85), 0, rot=roty(t * TAU) @ rotx(0.3))]
    mats = [Material(c1, c2, metal=1.0, emissive=c2, pattern=pat_grid(6, t, 0.06))]
    return parts, mats, dict(view_pitch=0.35, view_yaw=0.5), None


def b_progmatter(c1, c2, t):
    m = 0.5 + 0.5 * math.sin(t * TAU)
    f = lambda p: lerp(sd_box(p, (0.58, 0.58, 0.58), 0.06), sd_sphere(p, 0.7), m)
    parts = [Part(f, 0)]
    mats = [Material(c1, c2, metal=0.7, emissive=c2, pattern=pat_grid(6, t, 0.07))]
    return parts, mats, dict(view_pitch=0.55, view_yaw=0.785), None


def b_whitehole(c1, c2, t):
    parts = [Part(lambda p: sd_sphere(p, 0.42), 0)]
    mats = [Material(c1, c2, emissive=c1, pattern=pat_all())]
    def ov(img):
        cx, cy = 8, 8
        for k in range(8):
            a = k * TAU / 8 + t * TAU
            for r in range(5, 8):
                put(img, [(int(round(cx - 0.5 + math.cos(a) * r)), int(round(cy - 0.5 + math.sin(a) * r)))], H(c2), 1.0 - (r - 5) * 0.3)
    return parts, mats, dict(view_pitch=0.3, view_yaw=0.5), ov


def b_singularity(c1, c2, t):
    parts = [Part(lambda p: sd_sphere(p, 0.36), 0), Part(lambda p: sd_torus(p / np.array([1, 1, 1]), 0.68, 0.06), 1, rot=rotx(0.35))]
    mats = [Material('#000000', '#000000', metal=0.0, gloss=0.0),
            Material(c2, '#ffffff', emissive=c2, pattern=pat_bands(12, 2, t, 0.3, axis=0))]
    mats[0].ramp[:] = np.array([0.0, 0.0, 0.02]) # pure shadow
    return parts, mats, dict(view_pitch=0.25, view_yaw=0.5), None


def b_universe(c1, c2, t):
    def gal(p, n):
        x, z = p[..., 0], p[..., 2]
        r = np.sqrt(x * x + z * z) + 1e-6
        arms = np.sin(2 * np.arctan2(z, x) - np.log(r) * 4 - t * TAU) > 0.45
        return np.zeros(r.shape), ((arms & (r < 0.5)) | (r < 0.14)).astype(float)
    parts = [Part(lambda p: sd_sphere(p, 0.68), 0)]
    mats = [Material(c1, c2, metal=1.0, emissive=c2, pattern=gal)]
    return parts, mats, dict(view_pitch=0.9, view_yaw=0.3), None


def b_star(c1, c2, t):
    parts = [Part(lambda p: sd_sphere(p, 0.5), 0)]
    mats = [Material(c1, c2, emissive=c2, pattern=pat_combine(pat_spots(t, 0.55), pat_noise_shade(10, 0.3)))]
    def ov(img):
        for k in range(8):
            a = k * TAU / 8 + t * TAU
            r = 7 + (k % 2)
            put(img, [(int(round(7.5 + math.cos(a) * r + 1e-3)), int(round(7.5 + math.sin(a) * r + 1e-3)))], H(c1), 0.8)
    return parts, mats, dict(view_pitch=0.3, view_yaw=0.5), ov


def b_omega(c1, c2, t):
    parts, mats, view, _ = b_orb(c1, c2, t, rings=2, ring_col=c2, ring_glow=True, emissive=c1, pattern=pat_pulse_core(t), r=0.5)
    def ov(img):
        put(img, [(6, 6), (7, 5), (8, 5), (9, 6), (6, 7), (9, 7), (6, 8), (9, 8), (7, 9), (8, 9), (6, 10), (9, 10), (5, 10), (10, 10)], WHITE)
    return parts, mats, view, ov


def b_text_tile(c1, c2, t, glyph):
    parts = [Part(lambda p: sd_box(p, (0.7, 0.7, 0.12), 0.06), 0)]
    mats = [Material(c1, c2, metal=0.6)]
    def ov(img):
        on = 0.6 + 0.4 * math.sin(t * TAU)
        put(img, glyph, H(c2) * on + (1 - on) * H(c1))
    return parts, mats, dict(view_pitch=0.2, view_yaw=0.35), ov


def b_code(c1, c2, t):
    parts = [Part(lambda p: sd_box(p, (0.6, 0.78, 0.1), 0.05), 0)]
    def rain(p, n):
        col = np.floor((p[..., 0] + 0.6) * 6)
        head = np.mod(t * 4 + np.sin(col * 7.1) * 3, 2.0) - 1.0
        dy = (head - p[..., 1])
        return np.zeros(col.shape), ((dy > 0) & (dy < 0.6) & (np.mod(np.floor(p[..., 1] * 7), 2) == 0) & (np.mod(col, 1) == 0)).astype(float)
    mats = [Material(c1, c2, metal=0.3, emissive=c2, pattern=rain)]
    return parts, mats, dict(view_pitch=0.15, view_yaw=0.3), None



def b_banach(c1, c2, t):
    """One ball cut into pieces that drift apart and reassemble as two."""
    s = 0.5 - 0.5 * math.cos(t * TAU)
    parts, mats = [], [Material(c1, c2, metal=0.8, emissive=c2, pattern=pat_bands(6, 1, t, 0.1, axis=1))]
    for k in range(2):
        cx = (k * 2 - 1) * 0.42 * s
        for q in range(4):
            a = q * TAU / 4 + 0.785
            off = np.array([cx + math.cos(a) * 0.08 * s, 0, math.sin(a) * 0.08 * s])
            wedge = lambda p, a=a, off=off, r=0.42 + 0.06 * (1 - s): np.maximum(sd_sphere(p - off, r),
                     -((p[..., 0] - off[0]) * math.cos(a) + (p[..., 2] - off[2]) * math.sin(a)) + 0.0)
            parts.append(Part(wedge, 0))
    return parts, mats, dict(view_pitch=0.35, view_yaw=0.3), None


def b_russell(c1, c2, t):
    """A set containing itself: nested shells, each one the same set again."""
    parts, mats = [], [Material(c1, c2, metal=0.8), Material(c2, '#ffffff', emissive=c2, pattern=pat_all())]
    for k, r in enumerate((0.72, 0.5, 0.32)):
        a = t * TAU * (1 if k % 2 == 0 else -1)
        parts.append(Part(lambda p, r=r: sd_torus(p, r, 0.06), 0, rot=rotx(0.9 + k * 0.5) @ roty(a)))
        parts.append(Part(lambda p, r=r: sd_torus(p, r, 0.06), 0, rot=rotz(1.4 + k * 0.4) @ rotx(-a)))
    parts.append(Part(lambda p: sd_sphere(p, 0.14), 1))
    return parts, mats, dict(view_pitch=0.3, view_yaw=0.5), None


def b_key(c1, c2, t):
    parts = [Part(lambda p: sd_torus(p, 0.32, 0.12), 0, pos=(-0.4, 0.4, 0), rot=rotx(TAU / 4)),
             Part(lambda p: sd_capsule(p, np.array([-0.15, 0.15, 0]), np.array([0.62, -0.62, 0]), 0.12), 0),
             Part(lambda p: sd_box(p, (0.1, 0.2, 0.08), 0.02), 0, pos=(0.42, -0.5, 0), rot=rotz(-0.78)),
             Part(lambda p: sd_box(p, (0.1, 0.15, 0.08), 0.02), 0, pos=(0.2, -0.28, 0), rot=rotz(-0.78)),
             Part(lambda p: sd_octa(p, 0.2), 1, pos=(-0.4, 0.4, 0), rot=roty(t * TAU))]
    mats = [Material(c1, c2, metal=1.0, emissive=c2, pattern=pat_bands(10, 2, t, 0.12, axis=0)),
            Material(c2, '#ffffff', emissive=c2, pattern=pat_pulse_core(t))]
    return parts, mats, dict(view_pitch=0.25, view_yaw=0.2), None


def b_log(c1, c2, t):
    def rings(p, n):
        r = np.sqrt(p[..., 0] ** 2 + p[..., 2] ** 2)
        top = n[..., 1] > 0.6
        g = (np.sin(r * 40) > 0.3) & top
        side = (~top) & (np.sin(p[..., 1] * 14 + np.sin(np.arctan2(p[..., 2], p[..., 0]) * 5) * 2 + t * TAU) > 0.75)
        return np.zeros(r.shape), (g | side).astype(float)
    parts = [Part(lambda p: sd_cyl(p, 0.48, 0.42, 0.05), 0, rot=rotz(0.25))]
    mats = [Material(c1, c2, metal=0.2, emissive=c2, pattern=rings)]
    return parts, mats, dict(view_pitch=0.55, view_yaw=0.6), None


def _tree_parts(mat_wood, mat_leaf, t, scale=1.0, base=(0, -0.55, 0)):
    parts = []
    def br(a, d, ln, depth, yaw):
        b = a + d * ln
        parts.append(Part(lambda p, a=a, b=b, r=(0.05 + 0.035 * depth) * scale: sd_capsule(p, a, b, r), mat_wood))
        if depth == 0:
            parts.append(Part(lambda p, c=b: sd_sphere(p - c, 0.11 * scale), mat_leaf))
            return
        for k in (-1, 1):
            ang = 0.62 + 0.08 * math.sin(t * TAU + depth + k)
            y2 = yaw + k * 0.0 + (1.57 if depth % 2 == 0 else 0)
            side = np.array([math.cos(y2), 0, math.sin(y2)])
            nd = d * math.cos(ang) + side * k * math.sin(ang)
            br(b, nd / np.linalg.norm(nd), ln * 0.7, depth - 1, y2)
    br(np.array(base, float), np.array([0, 1.0, 0]), 0.45 * scale, 3, 0.4)
    return parts


def b_tree_sim(c1, c2, t):
    mats = [Material(c1, c2, metal=0.6, emissive=c1, pattern=pat_bands(8, 2, t, 0.25, axis=1)),
            Material(c2, '#ffffff', emissive=c2, pattern=pat_pulse_core(t)),
            Material('#20283a', c1, metal=1.0)]
    parts = _tree_parts(0, 1, t)
    parts.append(Part(lambda p: sd_cyl(p - np.array([0, -0.66, 0]), 0.55, 0.06, 0.02), 2))
    return parts, mats, dict(view_pitch=0.3, view_yaw=0.6), None


def b_ocean_sim(c1, c2, t):
    def bubbles(p, n):
        cell = np.floor(p * 6)
        h = np.mod(np.sin(cell[..., 0] * 12.9 + cell[..., 1] * 78.2 + cell[..., 2] * 37.7) * 43758.5, 1.0)
        life = np.mod(h + t, 1.0)
        return np.zeros(h.shape), ((h > 0.72) & (life < 0.6)).astype(float)
    parts = [Part(lambda p: sd_sphere(p, 0.6), 0), Part(lambda p: sd_box(p, (0.78, 0.04, 0.78), 0.02), 1, rot=roty(t * TAU))]
    mats = [Material(c1, c2, metal=0.9, emissive=c2, pattern=bubbles), Material('#8090a8', c2, metal=1.0, emissive=c2, pattern=pat_grid(5, t, 0.08))]
    return parts, mats, dict(view_pitch=0.35, view_yaw=0.5), None


def b_omnilex(c1, c2, t):
    mats = [Material('#e0d8ff', c2, metal=0.8, emissive='#ffffff', pattern=pat_bands(8, 2, t, 0.25, axis=1)),
            Material(c2, '#ffffff', emissive=c2, pattern=pat_pulse_core(t)),
            Material('#2a0a50', c2, metal=0.9, emissive=c2, pattern=pat_bands(10, 1, t, 0.18, axis=0))]
    parts = _tree_parts(0, 1, t, scale=0.9, base=(0, -0.35, 0))
    parts.append(Part(lambda p: sd_cyl(p - np.array([0, -0.5, 0]), 0.62, 0.1, 0.05), 2))
    parts.append(Part(lambda p: sd_torus(p, 0.8, 0.04), 1, pos=(0, -0.1, 0), rot=rotx(0.35) @ roty(t * TAU)))
    return parts, mats, dict(view_pitch=0.3, view_yaw=0.5), None



# ---- GT-style chain components
def pat_traces(t, seed=0):
    """Circuit traces running across a flat surface, with a pulse travelling along them."""
    def f(p, n):
        x, z = p[..., 0], p[..., 2]
        cell = np.floor(x * 6 + seed) + np.floor(z * 6) * 7
        horiz = np.mod(np.sin(cell * 12.9898) * 43758.5, 1.0) > 0.5
        lx = np.abs(np.mod(z * 6, 1.0) - 0.5) < 0.12
        lz = np.abs(np.mod(x * 6, 1.0) - 0.5) < 0.12
        tr = np.where(horiz, lx, lz) & (n[..., 1] > 0.5)
        pulse = np.mod(x * 1.5 + z - t * 2, 1.0) < 0.25
        return np.zeros(x.shape) - 0.05 * tr, (tr & pulse).astype(float)
    return f


def b_wafer(c1, c2, t, engraved=False):
    def disk(p):
        return np.maximum(sd_cyl(p, 0.78, 0.05, 0.02), -(p[..., 2] + 0.74))   # the flat "notch" edge
    parts = [Part(disk, 0)]
    if engraved:
        def dies(p, n):
            g = (np.abs(np.mod(p[..., 0] * 5, 1) - 0.5) < 0.42) & (np.abs(np.mod(p[..., 2] * 5, 1) - 0.5) < 0.42) & (n[..., 1] > 0.5)
            r = np.sqrt(p[..., 0] ** 2 + p[..., 2] ** 2)
            sweep = np.mod(r * 3 - t * 2, 1) < 0.3
            return -0.12 * (~g), (g & sweep & (r < 0.65)).astype(float)
        mats = [Material(c1, c2, metal=0.9, gloss=1.4, emissive=c2, pattern=dies)]
    else:
        def sheen(p, n):
            return 0.25 * np.sin(p[..., 0] * 4 + p[..., 2] * 3 - t * TAU) * (n[..., 1] > 0.5), np.zeros(p.shape[:-1])
        mats = [Material(c1, c2, metal=1.0, gloss=1.6, pattern=sheen)]
    return parts, mats, dict(view_pitch=0.85, view_yaw=0.5, zoom=1.0), None


def b_die(c1, c2, t):
    parts = [Part(lambda p: sd_box(p, (0.42, 0.08, 0.42), 0.02), 0), Part(lambda p: sd_box(p - np.array([0, 0.05, 0]), (0.3, 0.05, 0.3), 0.01), 1)]
    mats = [Material('#8090a0', '#e0e8f0', metal=1.0), Material(c1, c2, metal=0.8, emissive=c2, pattern=pat_traces(t))]
    return parts, mats, dict(view_pitch=0.75, view_yaw=0.785, zoom=1.15), None


def b_lens(c1, c2, t):
    lensf = lambda p: np.maximum(sd_sphere(p - np.array([0, -1.1, 0]), 1.3), sd_sphere(p - np.array([0, 1.1, 0]), 1.3))
    parts = [Part(lambda p: sd_torus(p, 0.62, 0.11), 0), Part(lensf, 1)]
    def glint(p, n):
        return 0.35 * np.sin(p[..., 0] * 5 - t * TAU * 2), (np.abs(p[..., 0] + p[..., 2] - (np.mod(t * 2, 1) * 2.4 - 1.2)) < 0.12).astype(float)
    mats = [Material('#606878', c2, metal=1.0), Material(c1, c2, metal=0.2, gloss=1.8, emissive='#ffffff', pattern=glint)]
    return parts, mats, dict(view_pitch=0.6, view_yaw=0.4, zoom=1.0), None


def b_boule(c1, c2, t):
    body = lambda p: np.maximum(sd_cyl(p, 0.36, 0.72, 0.18), -sd_box(p - np.array([0, 0.9, 0]), (1, 0.12, 1)))
    cone = lambda p: sd_capsule(p, np.array([0, 0.62, 0]), np.array([0, 0.9, 0]), 0.08)
    def facet(p, n):
        a = np.arctan2(p[..., 2], p[..., 0])
        return 0.2 * np.sign(np.sin(a * 6)), (np.abs(np.mod(p[..., 1] * 2 - t, 1) - 0.5) < 0.05).astype(float)
    parts = [Part(body, 0, rot=rotz(0.35)), Part(cone, 1, rot=rotz(0.35))]
    mats = [Material(c1, c2, metal=0.5, gloss=1.5, emissive=c2, pattern=facet), Material('#c0c8d0', '#ffffff', metal=1.0)]
    return parts, mats, dict(view_pitch=0.3, view_yaw=0.5), None


def b_board(c1, c2, t, populated=True):
    parts = [Part(lambda p: sd_box(p, (0.75, 0.05, 0.6), 0.02), 0)]
    mats = [Material(c1, c2, metal=0.3, emissive=c2, pattern=pat_traces(t, 3)), Material('#202430', '#808898', metal=0.8), Material('#c8a040', '#fff0c0', metal=1.0)]
    if populated:
        for (x, z, w, d) in ((-0.35, -0.2, 0.22, 0.22), (0.3, 0.15, 0.16, 0.12), (0.35, -0.3, 0.1, 0.1), (-0.3, 0.3, 0.12, 0.08)):
            parts.append(Part(lambda p, x=x, z=z, w=w, d=d: sd_box(p - np.array([x, 0.09, z]), (w, 0.05, d), 0.01), 1))
        for i in range(-4, 5):
            parts.append(Part(lambda p, i=i: sd_box(p - np.array([i * 0.15, 0.02, 0.62]), (0.04, 0.03, 0.05)), 2))
    return parts, mats, dict(view_pitch=0.8, view_yaw=0.6, zoom=1.0), None


def b_processor(c1, c2, t, tier):
    parts, mats, view, ov = b_chip(c1, c2, t)
    # higher tiers grow extra stacked dies and orbiting rings
    for k in range(min(tier, 4) // 2):
        parts.append(Part(lambda p, k=k: sd_box(p - np.array([0, 0.22 + k * 0.1, 0]), (0.2 - k * 0.05, 0.04, 0.2 - k * 0.05), 0.01), 1))
    if tier >= 5:
        mats.append(Material(c2, '#ffffff', emissive=c2, pattern=pat_bands(10, 2, t, 0.2, axis=0)))
        parts.append(Part(lambda p: sd_torus(p - np.array([0, 0.3, 0]), 0.5, 0.035), len(mats) - 1, rot=rotx(0.3) @ roty(t * TAU)))
    return parts, mats, view, ov


def b_component(c1, c2, t, kind):
    if kind == 'capacitor':
        parts = [Part(lambda p: sd_cyl(p, 0.3, 0.5, 0.08), 0), Part(lambda p: sd_cyl(p - np.array([0, 0.52, 0]), 0.24, 0.05, 0.02), 1),
                 Part(lambda p: sd_capsule(p, np.array([-0.12, -0.5, 0]), np.array([-0.12, -0.85, 0]), 0.04), 2),
                 Part(lambda p: sd_capsule(p, np.array([0.12, -0.5, 0]), np.array([0.12, -0.75, 0]), 0.04), 2)]
        mats = [Material(c1, c2, metal=0.6, emissive=c2, pattern=pat_bands(6, 1, t, 0.1, axis=1)), Material('#c0c8d0', '#ffffff', metal=1.0), Material('#c8a040', '#fff0c0', metal=1.0)]
        return parts, mats, dict(view_pitch=0.35, view_yaw=0.5, zoom=1.1), None
    if kind == 'inductor':
        parts = [Part(lambda p: sd_cyl(p, 0.16, 0.66, 0.03), 1, rot=rotz(TAU / 4))]
        for k in range(-3, 4):
            parts.append(Part(lambda p: sd_torus(p, 0.25, 0.075), 0, pos=(k * 0.17, 0, 0), rot=rotz(TAU / 4)))
        mats = [Material(c1, c2, metal=1.0, emissive=c2, pattern=pat_bands(8, 2, t, 0.12, axis=0)), Material('#303440', '#8090a0', metal=0.8)]
        return parts, mats, dict(view_pitch=0.4, view_yaw=0.5, zoom=1.05), None
    parts = [Part(lambda p: sd_box(p, (0.32, 0.14, 0.32), 0.04), 0)]
    for s in (-1, 0, 1):
        parts.append(Part(lambda p, s=s: sd_box(p - np.array([s * 0.2, -0.28, 0.0]), (0.04, 0.16, 0.04)), 2))
    mats = [Material(c1, c2, metal=0.5, emissive=c2, pattern=pat_pulse_core(t)), Material('#ffffff', '#ffffff'), Material('#c8a040', '#fff0c0', metal=1.0)]
    return parts, mats, dict(view_pitch=0.45, view_yaw=0.6, zoom=1.25), None


def b_catalyst(c1, c2, t):
    parts = [Part(lambda p: sd_octa(p, 0.55), 1, rot=roty(t * TAU) @ rotx(0.4))]
    mats = [Material(mix(H(c1), np.array([0.75, 0.78, 0.85]), 0.5), c2, metal=1.0, emissive=c2, pattern=pat_bands(10, 1, t, 0.1, axis=0)),
            Material(c1, c2, emissive=c2, pattern=pat_pulse_core(t))]
    for k in range(3):
        parts.append(Part(lambda p: sd_torus(p, 0.72, 0.055), 0, rot=rotx(0.6 + k * 1.05) @ rotz(k * 2.1 + t * TAU)))
    return parts, mats, dict(view_pitch=0.3, view_yaw=0.5), None


ITEMS = {
    'cherenkov_quartz': lambda c1, c2, t: b_gem(c1, c2, t, cluster=True, glowcore=True),
    'degenerate_matter_chunk': lambda c1, c2, t: b_chunk(c1, c2, t, 1),
    'neutronium_ingot': lambda c1, c2, t: b_ingot(c1, c2, t),
    'neutronium_plate': lambda c1, c2, t: b_plate(c1, c2, t),
    'void_lattice_shard': lambda c1, c2, t: b_shard(c1, c2, t),
    'exotic_matter_crystal': lambda c1, c2, t: b_gem(c1, c2, t, tilt=-0.25, glowcore=True, spin=True),
    'qubit': lambda c1, c2, t: b_orb('#103040', c2, t, rings=2, ring_col=c1, ring_glow=True, emissive=c1, pattern=pat_pulse_core(t)),
    'entangled_photon_pair': b_pair,
    'quantum_dot': b_quantum_dots,
    'superposition_crystal': lambda c1, c2, t: b_gem(c1, c2, t, cluster=True, glowcore=True, tilt=-0.2),
    'quantum_processor': lambda c1, c2, t: b_chip(c1, c2, t),
    'casimir_plate': b_casimir,
    'metamaterial_sheet': lambda c1, c2, t: b_plate(c1, c2, t, pattern=pat_grid(7, t, 0.07), emissive=c2),
    'topological_insulator': lambda c1, c2, t: b_cube(c1, c2, t, edges=True),
    'superconducting_coil': lambda c1, c2, t: b_device(c1, c2, t, 'coil'),
    'higgs_field_capacitor': lambda c1, c2, t: b_device(c1, c2, t, 'capacitor'),
    'tachyon_crystal': lambda c1, c2, t: b_gem(c1, c2, t, tilt=-1.0, r=0.28, h=1.05, glowcore=True),
    'chronon_capacitor': lambda c1, c2, t: b_device(c1, c2, t, 'clock'),
    'antimatter_pellet': lambda c1, c2, t: b_cell('#30344a', c2, t, frame='#9098a8', level=0, coils=True, core=c1),
    'strange_matter_pellet': lambda c1, c2, t: b_orb(c1, c2, t, pattern=pat_spots(t, 0.8), emissive=c2, r=0.55),
    'magnetic_monopole': lambda c1, c2, t: b_orb(c1, c2, t, rings=3, ring_col=c2, ring_glow=True, r=0.42),
    'cosmic_string_segment': b_cosmic_string,
    'dark_matter_lens': lambda c1, c2, t: b_device(c1, c2, t, 'lens'),
    'calabi_yau_manifold': b_manifold,
    'planck_shard': b_planck,
    'hawking_emitter': lambda c1, c2, t: b_device(c1, c2, t, 'emitter'),
    'kugelblitz_seed': lambda c1, c2, t: b_orb(c1, c2, t, emissive=c1, pattern=pat_pulse_core(t), r=0.45, rings=1, ring_col=c2, ring_glow=True),
    'alcubierre_coil': lambda c1, c2, t: b_device(c1, c2, t, 'warp'),
    'penrose_spindle': lambda c1, c2, t: b_device(c1, c2, t, 'spindle'),
    'micro_singularity': b_singularity,
    'white_hole_fragment': b_whitehole,
    'tesseract_core': b_tesseract,
    'spectral_analyzer': lambda c1, c2, t: b_device(c1, c2, t, 'analyzer'),
    'quantum_codex': lambda c1, c2, t: b_device(c1, c2, t, 'codex'),
    'brane_shifter': lambda c1, c2, t: b_device(c1, c2, t, 'shifter'),
    'quantum_string': b_qstring,
    'm_brane': lambda c1, c2, t: b_plate(c1, c2, t, pattern=pat_bands(6, 1, t, 0.25, axis=0), emissive=c2, curved=True),
    'graviton_crystal': lambda c1, c2, t: b_gem(c1, c2, t, cluster=True, glowcore=True, tilt=0.15),
    'spin_foam_lattice': lambda c1, c2, t: b_network(c1, c2, t),
    'multiverse_shard': lambda c1, c2, t: b_gem(np.array([0.5 + 0.5 * math.sin(t * TAU), 0.5 + 0.5 * math.sin(t * TAU + 2.1), 0.5 + 0.5 * math.sin(t * TAU + 4.2)]), c2, t, glowcore=True, tilt=0.5),
    'rift_anchor': lambda c1, c2, t: b_device(c1, c2, t, 'anchor'),
    'universe_seed': lambda c1, c2, t: b_orb(c1, c2, t, emissive=c2, pattern=pat_combine(pat_noise_shade(), pat_pulse_core(t)), r=0.48, rings=1, ring_col=c2, ring_glow=True, satellites=3, sat_col=['#ffffff']),
    'pocket_universe': b_universe,
    'heat_death_remnant': lambda c1, c2, t: b_cube(c2, c1, t, pattern=pat_noise_shade(12, 0.35)),
    'photonic_neuron': lambda c1, c2, t: b_device(c1, c2, t, 'neuron'),
    'neural_lattice': lambda c1, c2, t: b_cube(c1, c2, t, pattern=pat_grid(5, t, 0.1), emissive=c2),
    'narrow_ai_core': b_aicore,
    'general_ai_core': b_aicore,
    'superintelligence_core': b_aicore,
    'computronium_ingot': lambda c1, c2, t: b_ingot(c1, c2, t, pattern=pat_grid(8, t, 0.07), emissive=c2),
    'solar_statite': lambda c1, c2, t: b_plate(c1, c2, t, thick=0.03, pattern=pat_grid(5, t, 0.05), emissive=c2),
    'dyson_swarm_satellite': lambda c1, c2, t: b_device(c1, c2, t, 'satellite'),
    'dyson_shell_segment': lambda c1, c2, t: b_plate(c1, c2, t, curved=True, pattern=pat_bands(10, 0.5, t, 0.1, axis=0), emissive=c2),
    'stellar_core': b_star,
    'negentropy_cell': lambda c1, c2, t: b_cell(c2, c1, t, frame='#8090a0', level=0.7),
    'maxwell_demon_chip': lambda c1, c2, t: b_chip('#302020', c2, t, eye='#ff3020'),
    'holographic_matrix': lambda c1, c2, t: b_cube(c1, c2, t, pattern=pat_bands(14, 1, t, 0.3, axis=0), emissive=np.array([0.6 + 0.4 * math.sin(t * TAU), 0.6 + 0.4 * math.sin(t * TAU + 2), 1.0])),
    'bekenstein_crystal': lambda c1, c2, t: b_gem(c1, c2, t, glowcore=True, tilt=0.0, r=0.42),
    'source_code_fragment': b_code,
    'reality_anchor': lambda c1, c2, t: b_device('#404858', c2, t, 'anchor'),
    'omega_point': b_omega,
    'nanite_swarm': b_swarm,
    'programmable_matter': b_progmatter,
    'claytronic_catom': lambda c1, c2, t: b_orb(c1, c2, t, pattern=pat_grid(6, t, 0.06), emissive=c2, r=0.6),
    'von_neumann_probe': lambda c1, c2, t: b_device(c1, c2, t, 'probe'),
    'massless_alloy': lambda c1, c2, t: b_ingot(c1, c2, t, float_glow=True),
    'negative_mass_ingot': lambda c1, c2, t: b_ingot(c1, c2, t, pattern=pat_spots(t, 0.9), emissive=c2),
    'inertia_damper': lambda c1, c2, t: b_device(c1, c2, t, 'damper'),
    'metallic_hydrogen_ingot': lambda c1, c2, t: b_ingot(c1, c2, t),
    'sterile_neutrino': lambda c1, c2, t: b_orb(c1, c2, t, emissive=c2, pattern=pat_bands(10, 1, t, 0.3, axis=1), r=0.45, rings=1, ring_col=c1),
    'quark_star_core': lambda c1, c2, t: b_orb(c1, c2, t, satellites=3, sat_col=['#ff4040', '#40ff60', '#4060ff'], r=0.48, pattern=pat_spots(t, 0.85), emissive=c2),
    'magnetar_shard': lambda c1, c2, t: b_shard(c1, c2, t, tilt=-0.5),
    'pulsar_timing_crystal': lambda c1, c2, t: b_gem(c1, c2, t, glowcore=True, tilt=0.4, spin=True),
    'dilithium_crystal': lambda c1, c2, t: b_gem(c1, c2, t, cluster=True, glowcore=True, tilt=-0.35),
    'annihilation_core': lambda c1, c2, t: b_cell('#30344a', c2, t, frame='#b0b8c8', level=0, coils=True, core=c1),
    'wormhole_throat': lambda c1, c2, t: b_torus(c1, c2, t, R=0.62, r=0.18, core=c2, tilt=1.1),
    'closed_timelike_curve': b_lemniscate,
    'paradox_residue': lambda c1, c2, t: b_chunk(c1, c2, t, 4),
    'bootstrap_artifact': lambda c1, c2, t: b_device(c1, c2, t, 'artifact'),
    'cosmological_constant': lambda c1, c2, t: b_text_tile(c1, c2, t, [(7, 4), (8, 4), (6, 6), (9, 6), (6, 7), (9, 7), (5, 9), (10, 9), (5, 10), (10, 10), (7, 5), (8, 5), (6, 8), (9, 8)]),
    'psionic_crystal': lambda c1, c2, t: b_gem(c1, c2, t, cluster=True, glowcore=True, tilt=0.3),
    'thought_matrix': lambda c1, c2, t: b_cube(c1, c2, t, pattern=pat_grid(5, t, 0.1), emissive=c2),
    'boltzmann_brain': lambda c1, c2, t: b_device(c1, c2, t, 'brain'),
    # ---- endgame: beyond mathematics
    'aleph_null_shard': lambda c1, c2, t: b_gem(c1, c2, t, cluster=True, glowcore=True, spin=True),
    'godel_sentence': lambda c1, c2, t: b_text_tile(c1, c2, t, [(6, 4), (7, 4), (8, 4), (9, 4), (5, 5), (5, 6), (5, 7), (5, 8), (5, 9), (6, 10), (7, 10), (8, 10), (9, 10), (10, 9), (10, 8), (9, 7), (8, 7)]),
    'banach_tarski_sphere': b_banach,
    'russell_paradox_set': b_russell,
    'absolute_infinity': b_lemniscate,
    'axiom_fragment': lambda c1, c2, t: b_shard(c1, c2, t, tilt=0.4),
    'axiom_key': b_key,
    'arbor_heartwood_sample': b_log,
    'bubble_universe_sample': lambda c1, c2, t: (lambda r: (r[0] + [Part(lambda p: sd_torus(p, 0.74, 0.035), 1, rot=rotx(1.2))],
                                                            r[1] + [Material('#c0d8ff', '#ffffff', emissive='#a0c8ff', pattern=pat_all())], r[2], None))(b_universe(c1, c2, t)),
    'law_crystal': lambda c1, c2, t: b_gem(c1, c2, t, glowcore=True, spin=True, tilt=0.15),
    'tree_simulacrum': b_tree_sim,
    'ocean_simulacrum': b_ocean_sim,
    'omnilex': b_omnilex,
    'lattice_seed': lambda c1, c2, t: b_gem(c1, c2, t, glowcore=True, spin=True, r=0.28, h=0.7),
    'cherenkov_boule': b_boule,
    'topological_boule': b_boule,
    'hyperdimensional_boule': b_boule,
    'cherenkov_wafer': b_wafer,
    'topological_wafer': b_wafer,
    'hyperdimensional_wafer': b_wafer,
    'photonic_logic_wafer': lambda c1, c2, t: b_wafer(c1, c2, t, True),
    'qubit_array_wafer': lambda c1, c2, t: b_wafer(c1, c2, t, True),
    'topological_logic_wafer': lambda c1, c2, t: b_wafer(c1, c2, t, True),
    'hyperdimensional_logic_wafer': lambda c1, c2, t: b_wafer(c1, c2, t, True),
    'cosmic_logic_wafer': lambda c1, c2, t: b_wafer(c1, c2, t, True),
    'photonic_die': b_die,
    'qubit_die': b_die,
    'topological_die': b_die,
    'hyperdimensional_die': b_die,
    'cosmic_die': b_die,
    'photonic_lens': b_lens,
    'tesseract_lens': b_lens,
    'cosmic_lens': b_lens,
    'femto_capacitor': lambda c1, c2, t: b_component(c1, c2, t, 'capacitor'),
    'atto_capacitor': lambda c1, c2, t: b_component(c1, c2, t, 'capacitor'),
    'femto_inductor': lambda c1, c2, t: b_component(c1, c2, t, 'inductor'),
    'atto_inductor': lambda c1, c2, t: b_component(c1, c2, t, 'inductor'),
    'femto_transistor': lambda c1, c2, t: b_component(c1, c2, t, 'transistor'),
    'atto_transistor': lambda c1, c2, t: b_component(c1, c2, t, 'transistor'),
    'circuit_substrate': lambda c1, c2, t: b_board(c1, c2, t, False),
    'photonic_board': b_board,
    'topological_board': b_board,
    'cosmic_board': b_board,
    'photonic_processor': lambda c1, c2, t: b_processor(c1, c2, t, 1),
    'topological_processor': lambda c1, c2, t: b_processor(c1, c2, t, 3),
    'hyperdimensional_processor': lambda c1, c2, t: b_processor(c1, c2, t, 4),
    'cosmic_processor': lambda c1, c2, t: b_processor(c1, c2, t, 5),
    'ontological_processor': lambda c1, c2, t: b_processor(c1, c2, t, 6),
    'transfinite_processor': lambda c1, c2, t: b_processor(c1, c2, t, 7),
    'higgs_catalyst': b_catalyst,
    'spacetime_catalyst': b_catalyst,
    'eternity_catalyst': b_catalyst,
    'chronosteel_ingot': lambda c1, c2, t: b_ingot(c1, c2, t, pattern=pat_bands(6, 0.5, t, 0.08, axis=0), emissive=c2),
    'spacetime_alloy_ingot': lambda c1, c2, t: b_ingot(c1, c2, t, pattern=pat_spots(t, 0.93), emissive=c2),
    'cosmic_neutronium_ingot': lambda c1, c2, t: b_ingot(c1, c2, t, pattern=pat_spots(t, 0.95, 4), emissive='#ffffff'),
    'spacetime_slag': lambda c1, c2, t: b_chunk(c1, c2, t, 7),
    'fractured_essence': lambda c1, c2, t: b_chunk(c1, c2, t, 11),
    'void_essence': lambda c1, c2, t: b_orb(c1, c2, t, emissive=c2, pattern=pat_combine(pat_noise_shade(), pat_spots(t, 0.8)), r=0.5, satellites=2, sat_col=['#c060ff']),
    'field_calibrator': b_calibrator,
    'autonomous_calibrator': b_autocalibrator,
    'structure_assembler': b_assembler,
}


def render_item(item_id, c1, c2, t):
    fn = ITEMS.get(item_id)
    if fn is None:
        return None
    res = fn(c1, c2, t)
    parts, mats, kw = res[0], res[1], res[2]
    ov = res[3] if len(res) > 3 else None
    img = render(parts, mats, **kw)
    if ov:
        ov(img)
    return img


# ---- multiblock-complexity components and particles
ITEMS.update({
    'dipole_winding': lambda c1, c2, t: b_torus(c1, c2, t, R=0.55, r=0.17, tilt=0.9, count=2),
    'quadrupole_pole_tip': lambda c1, c2, t: b_plate(c1, c2, t, curved=True, pattern=pat_bands(4, 1, t, 0.2, axis=0), emissive=c2),
    'rf_resonator_cell': lambda c1, c2, t: b_device(c1, c2, t, 'capacitor'),
    'tachyon_klystron': lambda c1, c2, t: b_device(c1, c2, t, 'emitter'),
    'cryocooler_unit': lambda c1, c2, t: b_cell(c2, c1, t, frame='#a0b0c0', level=0.8, coils=True),
    'turbomolecular_rotor': lambda c1, c2, t: b_device(c1, c2, t, 'spindle'),
    'scintillator_crystal': lambda c1, c2, t: b_gem(c1, c2, t, cluster=True, glowcore=True, tilt=0.4),
    'pixel_tracker_module': lambda c1, c2, t: b_chip(c1, c2, t),
    'plasma_nozzle_tip': lambda c1, c2, t: b_device(c1, c2, t, 'warp'),
    'gravitic_lens_element': lambda c1, c2, t: b_device(c1, c2, t, 'lens'),
    'hawking_absorber_plate': lambda c1, c2, t: b_plate(c1, c2, t, pattern=pat_grid(6, t, 0.06), emissive=c2),
    'error_correction_lattice': lambda c1, c2, t: b_network(c1, c2, t, n=9, seed=5),
    'anchor_core': lambda c1, c2, t: b_device(c1, c2, t, 'anchor'),
    'stabiliser_matrix': b_manifold,
    'causal_shield_lattice': lambda c1, c2, t: b_cube(c1, c2, t, pattern=pat_grid(4, t, 0.1), emissive=c2, edges=True),
    'degenerate_armour_plate': lambda c1, c2, t: b_plate(c1, c2, t, thick=0.14),
    'pentaquark_shard': lambda c1, c2, t: b_shard(c1, c2, t),
    'glueball_core': lambda c1, c2, t: b_orb(c1, c2, t, emissive=c2, pattern=pat_pulse_core(t), r=0.5, rings=2, ring_col=c2, ring_glow=True),
    'hyperon_crystal': lambda c1, c2, t: b_gem(c1, c2, t, glowcore=True, spin=True, tilt=-0.3),
    'hypernova_alloy_ingot': lambda c1, c2, t: b_ingot(c1, c2, t, float_glow=True),
    'transcendent_metal_ingot': lambda c1, c2, t: b_ingot(c1, c2, t, pattern=pat_bands(8, 1, t, 0.2, axis=0), emissive=c2, float_glow=True),
    'horizon_shard': lambda c1, c2, t: b_shard(c1, c2, t, tilt=-0.4),
})


# ---- concept fields: glyph tablets for mathematics, and art-based defaults for everything else
_GLYPH_FONT = '/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf'


def _glyph_mask(ch, n):
    from PIL import Image, ImageDraw, ImageFont
    big = n * 4
    im = Image.new('L', (big, big), 0)
    d = ImageDraw.Draw(im)
    size = int(big * 0.62)
    font = ImageFont.truetype(_GLYPH_FONT, size)
    l, tp, r, b = d.textbbox((0, 0), ch, font=font)
    d.text(((big - (r - l)) / 2 - l, (big - (b - tp)) / 2 - tp), ch, font=font, fill=255)
    return np.asarray(im.resize((n, n), Image.LANCZOS)) / 255.0


def b_glyph(c1, c2, t, ch):
    """A floating tablet of polished stone with a mathematical symbol burning into its face."""
    parts = [Part(lambda p: sd_box(p, (0.62, 0.62, 0.1), 0.12), 0),
             Part(lambda p: sd_torus(p, 0.86, 0.035), 1, rot=rotx(1.5708) @ rotz(t * TAU))]
    mats = [Material(c1, c2, metal=0.55), Material(c2, '#ffffff', emissive=c2, pattern=pat_bands(8, 1, t, 0.25, axis=0))]

    def ov(img):
        n = img.shape[0]
        m = _glyph_mask(ch, n)
        on = 0.75 + 0.25 * math.sin(t * TAU)
        col = H(c2) * on + (1 - on) * np.array([1, 1, 1.0])
        halo = np.clip(_blur(m) * 1.4, 0, 1) * (1 - m)
        for k in range(3):
            img[..., k] = img[..., k] * (1 - m) + col[k] * m
            img[..., k] = np.clip(img[..., k] + halo * H(c2)[k] * 0.5, 0, 1)
        img[..., 3] = np.maximum(img[..., 3], np.maximum(m, halo * 0.6))
    return parts, mats, dict(view_pitch=0.15, view_yaw=0.3), ov


def b_concept_singularity(c1, c2, t):
    """A black point wrapped in two tilted accretion rings of its field's colour."""
    parts = [Part(lambda p: sd_sphere(p, 0.34), 0),
             Part(lambda p: sd_torus(p, 0.62, 0.07), 1, rot=rotx(1.05) @ rotz(t * TAU)),
             Part(lambda p: sd_torus(p, 0.78, 0.04), 2, rot=rotx(-0.6) @ roty(0.9) @ rotz(-t * TAU))]
    mats = [Material('#000000', '#000000', metal=0.0, gloss=0.0),
            Material(c2, '#ffffff', emissive=c2, pattern=pat_bands(10, 2, t, 0.35, axis=0)),
            Material(c1, c2, emissive=c2, pattern=pat_bands(6, -1, t, 0.25, axis=0))]
    mats[0].ramp[:] = np.array([0.0, 0.0, 0.02])
    return parts, mats, dict(view_pitch=0.35, view_yaw=0.5), None


def _blur(m):
    out = m.copy()
    for dy, dx in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        out = np.maximum(out, np.roll(np.roll(m, dy, 0), dx, 1) * 0.7)
    return out


AUTO_ART = {
    'shard': lambda c1, c2, t: b_shard(c1, c2, t),
    'dots': b_quantum_dots,
    'manifold': b_manifold,
    'brane': lambda c1, c2, t: b_plate(c1, c2, t, pattern=pat_bands(6, 1, t, 0.25, axis=0), emissive=c2, curved=True),
    'code': b_code,
    'network': lambda c1, c2, t: b_network(c1, c2, t, n=8, seed=11),
    'pair': b_pair,
    'bloch': lambda c1, c2, t: b_orb(c1, c2, t, rings=3, ring_col=c2, ring_glow=True, emissive=c2, pattern=pat_pulse_core(t), r=0.42),
    'gem': lambda c1, c2, t: b_gem(c1, c2, t, glowcore=True, tilt=0.2),
    'hexgrid': lambda c1, c2, t: b_cube(c1, c2, t, pattern=pat_grid(6, t, 0.08), emissive=c2, edges=True),
    'star': b_star,
    'tesseract': b_tesseract,
    'orb': lambda c1, c2, t: b_orb(c1, c2, t, emissive=c2, pattern=pat_pulse_core(t), r=0.55, rings=1, ring_col=c2, ring_glow=True),
    'lattice': lambda c1, c2, t: b_cube(c1, c2, t, pattern=pat_grid(4, t, 0.1), emissive=c2, edges=True),
    'ingot': lambda c1, c2, t: b_ingot(c1, c2, t, float_glow=True),
    'chunk': lambda c1, c2, t: b_chunk(c1, c2, t, 7),
    'string': b_qstring,
    'loop': b_lemniscate,
    'streak': b_cosmic_string,
    'singularity': lambda c1, c2, t: b_concept_singularity(c1, c2, t),
    'omega': b_omega,
    'analyzer': lambda c1, c2, t: b_device(c1, c2, t, 'analyzer'),
    'crystal': lambda c1, c2, t: b_gem(c1, c2, t, cluster=True, glowcore=True, tilt=0.3),
}


def auto_register(content_items):
    """Gives every item without a hand-made 3D builder one chosen by its art kind (or a glyph tablet)."""
    for it in content_items:
        if it['id'] in ITEMS:
            continue
        if it.get('glyph'):
            ITEMS[it['id']] = (lambda ch: (lambda c1, c2, t: b_glyph(c1, c2, t, ch)))(it['glyph'])
        elif it['art'] in AUTO_ART:
            ITEMS[it['id']] = AUTO_ART[it['art']]


def _auto_from_content():
    import json, os
    p = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'content.json')
    if os.path.exists(p):
        auto_register(json.load(open(p))['items'])


_auto_from_content()


# round 37 (1.1): the first materials and instruments of the new sciences (rendered static: see the 1.1 design's texture rule)
ITEMS.update({
    'telescope_mirror_blank': b_lens,
    'theodolite_lens': b_lens,
    'spectral_plate': lambda c1, c2, t: b_plate(c1, c2, t, emissive=c2, pattern=pat_bands(12, 0, t)),
    'survey_map': lambda c1, c2, t: b_device(c1, c2, t, 'codex'),
    'geoid_model': lambda c1, c2, t: b_orb(c1, c2, t, r=0.6, metal=0.2),
    'sediment_core': b_log,
    'raw_pitchblende': lambda c1, c2, t: b_chunk(c1, c2, t, seed=3),
    'radium_salt': lambda c1, c2, t: b_gem(c1, c2, t, cluster=True, glowcore=True),
    'geiger_tube': lambda c1, c2, t: b_device(c1, c2, t, 'capacitor'),
    'prussian_blue_tablet': lambda c1, c2, t: b_orb(c1, c2, t, r=0.5, metal=0.1),
    'magnetite': lambda c1, c2, t: b_gem(c1, c2, t, tilt=0.5),
    'ferrite_magnet': lambda c1, c2, t: b_plate(c1, c2, t, thick=0.22),
    'electrical_steel_lamination': lambda c1, c2, t: b_plate(c1, c2, t, thick=0.06),
    'dice_set': lambda c1, c2, t: b_cube(c1, c2, t, size=0.5),
    'random_number_chip': lambda c1, c2, t: b_chip(c1, c2, t),
    'geodetic_surveyor': lambda c1, c2, t: b_device(c1, c2, t, 'analyzer'),
    'gauss_meter': lambda c1, c2, t: b_device(c1, c2, t, 'analyzer'),
    'geiger_counter': lambda c1, c2, t: b_device(c1, c2, t, 'analyzer'),
})


# round 38 (1.2): Tier II materials and instruments (static)
ITEMS.update({
    'seismic_recorder_drum': b_log,
    'radiogenic_lead': lambda c1, c2, t: b_chunk(c1, c2, t, seed=5, spots=False),
    'lead_shielding_plate': lambda c1, c2, t: b_plate(c1, c2, t, thick=0.2),
    'monazite_sand': b_quantum_dots,
    'rare_earth_oxide': b_quantum_dots,
    'neodymium_blank': b_ingot,
    'samarium_cobalt_blank': b_ingot,
    'neodymium_magnet': lambda c1, c2, t: b_plate(c1, c2, t, thick=0.25),
    'samarium_cobalt_magnet': lambda c1, c2, t: b_plate(c1, c2, t, thick=0.25),
    'proof_mass': lambda c1, c2, t: b_cube(c1, c2, t, size=0.55),
    'time_dilated_sample': lambda c1, c2, t: b_gem(c1, c2, t, cluster=True),
    'normal_distribution_crystal': lambda c1, c2, t: b_gem(c1, c2, t, tilt=0.1, glowcore=True),
    'prior_tablet': lambda c1, c2, t: b_device(c1, c2, t, 'codex'),
    'posterior_tablet': lambda c1, c2, t: b_device(c1, c2, t, 'codex'),
})


# round 40 (1.2.0): the Planetary Interior (static)
ITEMS.update({
    'core_drill': lambda c1, c2, t: b_device(c1, c2, t, 'probe'),
    'ringwoodite': lambda c1, c2, t: b_gem(c1, c2, t, tilt=0.4),
    'bridgmanite': lambda c1, c2, t: b_gem(c1, c2, t, cluster=True),
    'fission_fragment_mix': b_quantum_dots,
})


# round 41 (1.3.3): Deep Riches (static)
ITEMS.update({
    'corundum': lambda c1, c2, t: b_gem(c1, c2, t, tilt=0.2),
    'garnet': lambda c1, c2, t: b_gem(c1, c2, t, tilt=0.5, r=0.4),
    'peridot': lambda c1, c2, t: b_gem(c1, c2, t, tilt=0.35),
    'moissanite': lambda c1, c2, t: b_gem(c1, c2, t, cluster=True, glowcore=True),
    'rich_iron_chunk': lambda c1, c2, t: b_chunk(c1, c2, t, seed=11),
    'rich_copper_chunk': lambda c1, c2, t: b_chunk(c1, c2, t, seed=12),
    'rich_gold_chunk': lambda c1, c2, t: b_chunk(c1, c2, t, seed=13),
    'moissanite_drill': lambda c1, c2, t: b_device(c1, c2, t, 'probe'),
})


# round 42 (1.3.5): the Interior's machines - fuel cycle and press products (static)
ITEMS.update({
    'yellowcake': lambda c1, c2, t: b_chunk(c1, c2, t, seed=21, spots=False),
    'fluorspar': lambda c1, c2, t: b_gem(c1, c2, t, cluster=True, tilt=0.25),
    'uranium_235_pellet': lambda c1, c2, t: b_cube(c1, c2, t, size=0.45),
    'uranium_238_pellet': lambda c1, c2, t: b_cube(c1, c2, t, size=0.45),
    'plutonium_239_pellet': lambda c1, c2, t: b_orb(c1, c2, t, r=0.45, metal=0.6),
    'graphite_moderator': lambda c1, c2, t: b_plate(c1, c2, t, thick=0.3),
    'boron_control_rod': b_ingot,
    'metamorphic_schist': lambda c1, c2, t: b_chunk(c1, c2, t, seed=23),
})


# 1.2.0 (art pass): hand-made builders for everything added since 1.1 (items3d_new.py), over the art-kind defaults
from items3d_new import BUILDERS as _NEW_BUILDERS
ITEMS.update(_NEW_BUILDERS)
