"""
Tiny SDF ray-marcher that renders items as real 3D objects, then shades them as pixel art.

Pipeline per frame:
  1. Orthographic rays (4x4 per output pixel) are marched against a scene of signed-distance primitives.
  2. Per hit: normal, material id, object-space position.  Key light top-left-front, cool rim light behind.
  3. Lighting is snapped onto a 6-tone hue-shifted ramp for the material (deep shadow -> specular white),
     with 2x2 ordered dithering between neighbouring tones - the hand-shaded look of GregTech / Avaritia / UFO.
  4. Emissive materials keep their own bright colour; small glow pixels are added around them.
Output: native 16x16 RGBA frames (crisp, no anti-aliasing), plus optional 2D pixel overlays.
"""
import math
import numpy as np

OUT = 32
SS = 3
RES = OUT * SS

BAYER = np.array([[0, 2], [3, 1]]) / 4.0 - 0.375


def hexc(h):
    h = h.lstrip('#')
    return np.array([int(h[i:i + 2], 16) / 255 for i in (0, 2, 4)])


def mix(a, b, t):
    return a * (1 - t) + b * t


def ramp(base, accent=None):
    """6-tone pixel-art ramp: shadows drift toward blue/purple, highlights toward the accent and white."""
    base = np.asarray(base, float)
    accent = base if accent is None else np.asarray(accent, float)
    cool = np.array([0.10, 0.07, 0.22])
    return np.array([
        mix(base * 0.18, cool, 0.55),
        mix(base * 0.42, cool, 0.25),
        base * 0.72,
        base,
        mix(base, mix(accent, np.ones(3), 0.35), 0.55),
        mix(accent, np.ones(3), 0.82),
    ])


# ----------------------------------------------------------------------------- SDF primitives (p: (...,3))
def length(p):
    return np.sqrt((p * p).sum(-1))


def sd_sphere(p, r):
    return length(p) - r


def sd_box(p, b, r=0.0):
    q = np.abs(p) - (np.asarray(b) - r)
    return length(np.maximum(q, 0)) + np.minimum(np.maximum(q[..., 0], np.maximum(q[..., 1], q[..., 2])), 0) - r


def sd_octa(p, s):
    p = np.abs(p)
    return (p[..., 0] + p[..., 1] + p[..., 2] - s) * 0.57735027


def sd_torus(p, R, r):
    q = np.stack([np.sqrt(p[..., 0] ** 2 + p[..., 2] ** 2) - R, p[..., 1]], -1)
    return length(q) - r


def sd_cyl(p, r, h, round_=0.0):
    d = np.stack([np.sqrt(p[..., 0] ** 2 + p[..., 2] ** 2) - r + round_, np.abs(p[..., 1]) - h + round_], -1)
    return np.minimum(np.maximum(d[..., 0], d[..., 1]), 0) + length(np.maximum(d, 0)) - round_


def sd_capsule(p, a, b, r):
    a, b = np.asarray(a, float), np.asarray(b, float)
    pa, ba = p - a, b - a
    h = np.clip((pa * ba).sum(-1) / (ba * ba).sum(), 0, 1)
    return length(pa - ba * h[..., None]) - r


def sd_hexprism(p, r, h):
    k = np.array([-0.8660254, 0.5, 0.57735])
    q = np.abs(p)
    xy = q[..., [0, 2]].copy()
    dot = np.minimum(k[0] * xy[..., 0] + k[1] * xy[..., 1], 0)
    xy[..., 0] -= 2 * dot * k[0]
    xy[..., 1] -= 2 * dot * k[1]
    d0 = length(np.stack([xy[..., 0] - np.clip(xy[..., 0], -k[2] * r, k[2] * r), xy[..., 1] - r], -1)) * np.sign(xy[..., 1] - r)
    d1 = q[..., 1] - h
    return np.minimum(np.maximum(d0, d1), 0) + length(np.maximum(np.stack([d0, d1], -1), 0))


def sd_gem(p, r, h, waist=0.35):
    """Hexagonal crystal: a prism body with pointed ends."""
    ang = np.arctan2(p[..., 2], p[..., 0])
    sector = np.mod(ang + math.pi / 6, math.pi / 3) - math.pi / 6
    rad = np.sqrt(p[..., 0] ** 2 + p[..., 2] ** 2) * np.cos(sector)
    body = rad - r
    k = h * (1 - waist) / r
    tips = (rad * k + np.abs(p[..., 1]) - h) / math.sqrt(k * k + 1)
    return np.maximum(body, tips)


def rotx(a):
    c, s = math.cos(a), math.sin(a)
    return np.array([[1, 0, 0], [0, c, -s], [0, s, c]])


def roty(a):
    c, s = math.cos(a), math.sin(a)
    return np.array([[c, 0, s], [0, 1, 0], [-s, 0, c]])


def rotz(a):
    c, s = math.cos(a), math.sin(a)
    return np.array([[c, -s, 0], [s, c, 0], [0, 0, 1]])


class Part:
    """A primitive with a transform (object -> world: R then translate) and a material index."""

    def __init__(self, fn, mat, pos=(0, 0, 0), rot=None, smooth=0.0, sub=False):
        self.fn = fn
        self.mat = mat
        self.pos = np.asarray(pos, float)
        self.rot = np.eye(3) if rot is None else rot
        self.sub = sub

    def dist(self, p):
        local = (p - self.pos) @ self.rot  # inverse rotation (orthonormal)
        return self.fn(local)


class Material:
    def __init__(self, base, accent=None, emissive=None, pattern=None, metal=0.5, gloss=1.0, keep_dark=False):
        b = hexc(base) if isinstance(base, str) else np.asarray(base, float)
        if not keep_dark and b.max() < 0.22:
            b = mix(b, np.array([0.42, 0.45, 0.52]), 0.42)   # very dark casings would vanish at 16px
        self.ramp = ramp(b, hexc(accent) if isinstance(accent, str) else accent)
        self.emissive = None if emissive is None else (hexc(emissive) if isinstance(emissive, str) else np.asarray(emissive))
        self.pattern = pattern   # f(local_pos, normal) -> (shade offset, emissive mask) arrays
        self.metal = metal
        self.gloss = gloss


def scene_dist(parts, p):
    d = np.full(p.shape[:-1], 1e9)
    m = np.full(p.shape[:-1], -1)
    for i, part in enumerate(parts):
        di = part.dist(p)
        if part.sub:
            d = np.maximum(d, -di)
            continue
        closer = di < d
        d = np.where(closer, di, d)
        m = np.where(closer, i, m)
    return d, m


LIGHT_CAM = np.array([-0.5, 0.62, -0.6])     # camera space: up-left, toward the viewer
LIGHT_CAM /= np.linalg.norm(LIGHT_CAM)
RIM_CAM = np.array([0.7, -0.1, 0.7])         # from behind, lower right
RIM_CAM /= np.linalg.norm(RIM_CAM)


def render(parts, materials, view_pitch=0.5, view_yaw=0.7853, zoom=1.0, glow=True, steps=56, outline=True, extent=1.0):
    """Returns a 16x16 RGBA float image."""
    n = RES
    ys, xs = np.mgrid[0:n, 0:n]
    u = ((xs + 0.5) / n * 2 - 1) * extent / zoom
    v = -((ys + 0.5) / n * 2 - 1) * extent / zoom
    cam = rotx(-view_pitch) @ roty(view_yaw)    # world -> camera (looking down at the object)
    inv = cam.T
    origin = np.stack([u, v, np.full_like(u, -4.0)], -1) @ cam   # camera space -> world (row vectors)
    direction = np.array([0, 0, 1.0]) @ cam
    LIGHT = LIGHT_CAM @ cam
    RIM = RIM_CAM @ cam
    t = np.zeros((n, n))
    hit = np.zeros((n, n), bool)
    for _ in range(steps):
        p = origin + direction * t[..., None]
        d, _ = scene_dist(parts, p)
        hit |= d < 0.002
        t = np.where(hit, t, t + np.maximum(d, 0.004))
        if (t > 9).all():
            break
    p = origin + direction * t[..., None]
    d, mid = scene_dist(parts, p)
    hit = hit & (d < 0.01)
    e = 0.004
    nx = scene_dist(parts, p + [e, 0, 0])[0] - scene_dist(parts, p - [e, 0, 0])[0]
    ny = scene_dist(parts, p + [0, e, 0])[0] - scene_dist(parts, p - [0, e, 0])[0]
    nz = scene_dist(parts, p + [0, 0, e])[0] - scene_dist(parts, p - [0, 0, e])[0]
    nrm = np.stack([nx, ny, nz], -1)
    nrm /= np.maximum(np.linalg.norm(nrm, axis=-1, keepdims=True), 1e-9)
    view = -direction
    half = (LIGHT + view)
    half /= np.linalg.norm(half)
    diff = np.clip(nrm @ LIGHT, 0, 1)
    spec = np.clip(nrm @ half, 0, 1)
    rim = np.clip(nrm @ RIM, 0, 1) * (1 - np.clip(nrm @ view, 0, 1)) ** 2
    # soft self-shadow / occlusion from a short march toward the light
    occ = np.zeros((n, n))
    for k in range(1, 6):
        dd, _ = scene_dist(parts, p + nrm * 0.01 + LIGHT * (0.06 * k))
        occ = np.maximum(occ, np.clip((0.06 * k - dd) / (0.06 * k), 0, 1) * (1 - k / 7))
    # downsample: per output pixel use the centre-most hit sample (crisp), coverage from all samples
    img = np.zeros((OUT, OUT, 4))
    idx_map = np.full((OUT, OUT), -1)
    part_map = np.full((OUT, OUT), -1)
    emis_map = np.zeros((OUT, OUT), bool)
    level = np.zeros((n, n))
    rgb_full = np.zeros((n, n, 3))
    emis_full = np.zeros((n, n))
    for i, part in enumerate(parts):
        sel = hit & (mid == i)
        if not sel.any():
            continue
        mat = materials[part.mat]
        up = np.clip(nrm @ (np.array([0, 1.0, 0]) @ cam), -1, 1)
        lv = 0.46 + 0.55 * diff + 0.12 * up - 0.35 * occ + mat.gloss * (spec ** (30 + 60 * mat.metal)) * 0.75 + 0.3 * rim
        if mat.pattern is not None:
            local = (p - part.pos) @ part.rot
            off, em = mat.pattern(local, nrm)
            lv = lv + off
        else:
            em = np.zeros((n, n))
        level = np.where(sel, lv, level)
        if mat.emissive is not None:
            emis_full = np.where(sel, np.maximum(em, 0) if mat.pattern is not None else 1.0, emis_full)
            rgb_full = np.where((sel & (emis_full > 0))[..., None], mat.emissive, rgb_full)
        else:
            emis_full = np.where(sel, np.maximum(em, 0), emis_full)
            rgb_full = np.where((sel & (em > 0))[..., None], mat.ramp[5], rgb_full)
    # ---- downsample with smooth, continuous shading (no hard 6-step banding, no dither noise)
    cov = hit.reshape(OUT, SS, OUT, SS).mean(axis=(1, 3))
    def avg(a2):
        num = (a2 * hit).reshape(OUT, SS, OUT, SS).sum(axis=(1, 3))
        return num / np.maximum(hit.reshape(OUT, SS, OUT, SS).sum(axis=(1, 3)), 1)
    lv_px = avg(level)
    em_px = avg(emis_full)
    for oy in range(OUT):
        for ox in range(OUT):
            if cov[oy, ox] < 0.34:
                continue
            block = hit[oy * SS:(oy + 1) * SS, ox * SS:(ox + 1) * SS]
            yy, xx = np.nonzero(block)
            cy = cx = (SS - 1) / 2
            k = np.argmin((yy - cy) ** 2 + (xx - cx) ** 2)
            sy, sx = oy * SS + yy[k], ox * SS + xx[k]
            m_i = mid[sy, sx]
            mat = materials[parts[m_i].mat]
            lv = lv_px[oy, ox]
            f = float(np.clip((lv - 0.15) * 4.4, 0, 5))
            i0 = int(f)
            i1 = min(5, i0 + 1)
            fr = f - i0
            # snap to quarter steps between ramp tones: smooth but still clean pixel-art bands
            fr = round(fr * 4) / 4
            col = mix(mat.ramp[i0], mat.ramp[i1], fr)
            part_map[oy, ox] = m_i
            em = em_px[oy, ox]
            if em > 0.3:
                kk = float(np.clip(0.66 + 0.4 * (lv - 0.55), 0.55, 1.15))
                ecol = np.clip(rgb_full[sy, sx] * kk, 0, 1)
                if lv > 1.15:
                    ecol = mix(ecol, np.ones(3), 0.5)
                col = mix(col, ecol, min(1.0, em * 1.3))
                emis_map[oy, ox] = True
            img[oy, ox, :3] = col
            img[oy, ox, 3] = 1
    if outline:
        solid = part_map >= 0
        def shifted(arr, dy, dx, fill):
            sh = np.full(arr.shape, fill, dtype=arr.dtype)
            ys0, ys1 = max(0, -dy), OUT - max(0, dy)
            xs0, xs1 = max(0, -dx), OUT - max(0, dx)
            sh[ys0:ys1, xs0:xs1] = arr[ys0 + dy:ys1 + dy, xs0 + dx:xs1 + dx]
            return sh
        # 1px dark outline just outside the silhouette (GregTech-style crisp edge)
        ring = np.zeros_like(solid)
        for dy, dx in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            ring |= shifted(solid, dy, dx, False)
        ring &= ~solid
        for oy, ox in zip(*np.nonzero(ring)):
            nb = [part_map[y, x] for y, x in ((oy + 1, ox), (oy - 1, ox), (oy, ox + 1), (oy, ox - 1)) if 0 <= y < OUT and 0 <= x < OUT and part_map[y, x] >= 0]
            r = materials[parts[nb[0]].mat].ramp
            img[oy, ox, :3] = mix(r[0], np.zeros(3), 0.35)
            img[oy, ox, 3] = 1
        # soft seams between different parts: the lower/right part darkens slightly along the seam
        seam = (shifted(part_map, -1, 0, -1) != part_map) & (shifted(part_map, -1, 0, -1) >= 0)
        seam |= (shifted(part_map, 0, -1, -1) != part_map) & (shifted(part_map, 0, -1, -1) >= 0)
        seam &= solid & ~emis_map
        img[seam, :3] *= 0.82
        # gentle highlight on the upper-left silhouette edge
        top = solid & ~shifted(solid, -1, 0, False) & ~emis_map
        img[top, :3] = np.clip(img[top, :3] * 1.12 + 0.03, 0, 1)
    return img


def add_glow(img, rgb, strength=0.55):
    """Soft 1-px halo of emissive colour around bright pixels (drawn into empty pixels only)."""
    lum = img[..., :3].max(-1) * img[..., 3]
    bright = lum > 0.93
    halo = np.zeros_like(bright)
    for dy, dx in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        halo |= np.roll(bright, (dy, dx), (0, 1))
    halo &= img[..., 3] == 0
    img[halo, :3] = np.asarray(rgb) * strength
    img[halo, 3] = 0.55
    return img
