"""
Procedural, animated textures for Theoretical Sciences.

Every texture is drawn with signed-distance shapes on a 4x supersampled canvas, then box-filtered down to
16x16 and given a dark outline so it reads as Minecraft pixel art. Animations are vertical frame strips
with a .mcmeta file (interpolated), so every item, block, fluid and particle moves.
"""
import json, math, os
import numpy as np
from PIL import Image

ROOT = os.path.dirname(os.path.abspath(__file__))
C = json.load(open(os.path.join(ROOT, 'content.json')))
ASSETS = os.path.join(ROOT, '..', 'src/main/resources/assets/theoreticalsciences')
TEX = os.path.join(ASSETS, 'textures')
SS = 4            # supersampling
FRAMES = 16
from scipy.ndimage import gaussian_filter, distance_transform_edt


def setres(n):
    """Switch output resolution (items are drawn at 32x32, blocks at 16x16)."""
    global N, S, x_, y_, U, V, R, A
    N = n
    S = N * SS
    y_, x_ = np.mgrid[0:S, 0:S]
    U = (x_ + 0.5) / S * 2 - 1       # -1..1 left->right
    V = (y_ + 0.5) / S * 2 - 1       # -1..1 top->bottom
    R = np.sqrt(U * U + V * V)
    A = np.arctan2(V, U)


setres(16)
TAU = math.pi * 2

def hexc(h):
    h = h.lstrip('#')
    return np.array([int(h[i:i + 2], 16) / 255 for i in (0, 2, 4)])


def mix(a, b, t):
    t = np.clip(t, 0, 1)[..., None] if np.ndim(t) else np.clip(t, 0, 1)
    return a * (1 - t) + b * t


def smooth(e0, e1, x):
    t = np.clip((x - e0) / (e1 - e0), 0, 1)
    return t * t * (3 - 2 * t)


def fill(sdf, aa=0.04):
    """Coverage from a signed distance (negative inside)."""
    return np.clip(0.5 - sdf / aa, 0, 1)


def box(u, v, w, h):
    du, dv = np.abs(u) - w, np.abs(v) - h
    return np.sqrt(np.maximum(du, 0) ** 2 + np.maximum(dv, 0) ** 2) + np.minimum(np.maximum(du, dv), 0)


def circle(u, v, r):
    return np.sqrt(u * u + v * v) - r


def seg(u, v, ax, ay, bx, by, r):
    pax, pay, bax, bay = u - ax, v - ay, bx - ax, by - ay
    h = np.clip((pax * bax + pay * bay) / (bax * bax + bay * bay + 1e-9), 0, 1)
    return np.sqrt((pax - bax * h) ** 2 + (pay - bay * h) ** 2) - r


def rot(u, v, a):
    c, s = math.cos(a), math.sin(a)
    return u * c - v * s, u * s + v * c


def noise(u, v, seed=0, scale=4.0):
    rng = np.random.default_rng(seed)
    g = rng.random((34, 34))
    x = (u + 1) / 2 * scale + 8
    y = (v + 1) / 2 * scale + 8
    # wraps every `scale` cells = exactly once across the texture, so it tiles and loops seamlessly
    wr = max(1, int(round(scale)))
    x0, y0 = np.floor(x).astype(int) % wr, np.floor(y).astype(int) % wr
    fx, fy = x - np.floor(x), y - np.floor(y)
    fx, fy = fx * fx * (3 - 2 * fx), fy * fy * (3 - 2 * fy)
    a, b = g[y0, x0], g[y0, (x0 + 1) % wr]
    c, d = g[(y0 + 1) % wr, x0], g[(y0 + 1) % wr, (x0 + 1) % wr]
    return (a * (1 - fx) + b * fx) * (1 - fy) + (c * (1 - fx) + d * fx) * fy


LIGHT = np.array([-0.55, -0.7, 0.85])
LIGHT = LIGHT / np.linalg.norm(LIGHT)
HALF = LIGHT + np.array([0, 0, 1.0])
HALF = HALF / np.linalg.norm(HALF)


class Canvas:
    """
    Layered painter with a height field and an emissive mask.
    Every opaque layer is "pillow embossed" (domed by its distance to its own edge) and stacked on the
    layers below, so the final image can be lit like a relief: top-left key light, specular highlights,
    cavity occlusion and a tinted outline. Emissive (glowing) paint is never shaded.
    """

    def __init__(self):
        self.rgb = np.zeros((S, S, 3))
        self.a = np.zeros((S, S))
        self.h = np.zeros((S, S))
        self.e = np.zeros((S, S))
        self.level = 0.0

    def over(self, cov, color, dome=1.0, emissive=0.0, raise_=0.18):
        cov = np.clip(cov, 0, 1)
        col = color if np.ndim(color) == 3 else np.broadcast_to(color, (S, S, 3))
        mask = cov > 0.5
        if dome > 0 and mask.any() and not mask.all():
            d = distance_transform_edt(mask)
            pillow = np.sqrt(np.clip(d / (S * 0.16), 0, 1))
        else:
            pillow = np.zeros((S, S))
        layer_h = self.level + raise_ + 0.55 * dome * pillow
        self.h = self.h * (1 - cov) + layer_h * cov
        self.e = self.e * (1 - cov) + emissive * cov
        self.rgb = self.rgb * (1 - cov[..., None]) + col * cov[..., None]
        self.a = self.a + cov * (1 - self.a)
        self.level += 0.08

    def add(self, cov, color):
        """Emissive light, added on top and immune to shading."""
        cov = np.clip(cov, 0, 1)
        col = color if np.ndim(color) == 3 else np.broadcast_to(color, (S, S, 3))
        self.rgb = np.clip(self.rgb + col * cov[..., None], 0, 1)
        self.a = np.clip(self.a + cov * 0.6, 0, 1)
        self.e = np.clip(np.maximum(self.e, cov * 0.95), 0, 1)

    def lit(self):
        hs = gaussian_filter(self.h, S * 0.012) * (S * 0.09)
        gy, gx = np.gradient(hs)
        n = np.stack([-gx, -gy, np.ones_like(hs)], -1)
        n /= np.linalg.norm(n, axis=-1, keepdims=True)
        diff = np.clip(n @ LIGHT, 0, 1)
        spec = np.clip(n @ HALF, 0, 1) ** 28
        cavity = np.clip((gaussian_filter(hs, S * 0.05) - hs) * 0.35, 0, 0.35)
        grain = (np.random.default_rng(3).random((S, S)) - 0.5) * 0.06
        shade = 0.42 + 0.78 * diff - cavity + grain
        lit = self.rgb * shade[..., None] + (spec * 0.6)[..., None] * (0.6 + 0.4 * self.rgb)
        e = self.e[..., None]
        return np.clip(lit * (1 - e) + self.rgb * e * 1.08, 0, 1)

    def down(self, outline=True, opaque=False, shade=True):
        src = self.lit() if shade else self.rgb
        rgb = src.reshape(N, SS, N, SS, 3)
        a = self.a.reshape(N, SS, N, SS)
        aw = a.mean(axis=(1, 3))
        col = (rgb * a[..., None]).sum(axis=(1, 3)) / np.maximum(a.sum(axis=(1, 3)), 1e-6)[..., None]
        out = np.zeros((N, N, 4))
        out[..., :3] = col
        out[..., 3] = np.where(aw > 0.35, 1.0, 0.0) if not opaque else 1.0
        if opaque:
            out[..., :3] = col * aw[..., None]
        if outline and not opaque:
            solid = out[..., 3] > 0
            edge = np.zeros_like(solid)
            for dy, dx in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                edge |= np.roll(solid, (dy, dx), axis=(0, 1))
            edge &= ~solid
            # tinted outline: darkened colour of the neighbouring pixels
            nb = np.zeros((N, N, 3))
            cnt = np.zeros((N, N, 1))
            for dy, dx in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                m = np.roll(solid, (dy, dx), axis=(0, 1))[..., None]
                nb += np.roll(out[..., :3], (dy, dx), axis=(0, 1)) * m
                cnt += m
            tint = nb / np.maximum(cnt, 1) * 0.28 + np.array([0.02, 0.015, 0.04])
            out[edge, :3] = tint[edge]
            out[edge, 3] = 1.0
            # 1px darker rim on the lower-right inside edge for extra depth
            inner_br = solid & ~(np.roll(solid, (-1, -1), axis=(0, 1)))
            out[inner_br, :3] *= 0.72
        return out


def save_strip(frames, path, frametime=2, interpolate=True):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    h, w = frames[0].shape[:2]
    img = np.concatenate(frames, axis=0)
    Image.fromarray((np.clip(img, 0, 1) * 255).astype(np.uint8), 'RGBA').save(path)
    if len(frames) > 1:
        json.dump({"animation": {"frametime": frametime, "interpolate": interpolate}}, open(path + '.mcmeta', 'w'))
        import trim_animations            # round 36: no interpolation, frametime >= 2, at most 32 frames
        trim_animations.trim(path)


def save_single(frame, path):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    Image.fromarray((np.clip(frame, 0, 1) * 255).astype(np.uint8), 'RGBA').save(path)


# =====================================================================================================
# ITEM ART
# =====================================================================================================

def shimmer(t, k=1.0):
    return 0.5 + 0.5 * np.sin(TAU * t * k)


def art_crystal(cv, t, c1, c2, seed=0):
    u, v = rot(U, V, 0.0)
    d = np.maximum(np.abs(u) * 1.6 + np.abs(v) * 0.9 - 0.9, np.abs(u) - 0.42)
    d2 = np.maximum(np.abs(U + 0.45) * 2.2 + np.abs(V - 0.25) * 1.2 - 0.55, -1)
    base = mix(c1, c2, smooth(-0.6, 0.8, -V + U * 0.3))
    cv.over(fill(d), base)
    cv.over(fill(d2), mix(c1, c2, 0.3))
    facet = fill(np.abs(U - V * 0.2) - 0.03) * fill(d)
    cv.add(facet * 0.35, c2)
    sweep = np.exp(-((U + V * 0.6 - (t * 3.2 - 1.6)) ** 2) / 0.02) * fill(d)
    cv.add(sweep, np.array([1, 1, 1.0]))


def art_chunk(cv, t, c1, c2, seed=1):
    n = noise(U, V, seed, 5)
    d = R - 0.62 - (n - 0.5) * 0.35
    cv.over(fill(d), mix(c1, c1 * 1.6, n))
    for i in range(5):
        a = i * 1.3 + seed
        px, py = math.cos(a) * 0.35, math.sin(a * 1.7) * 0.3
        tw = 0.5 + 0.5 * math.sin(TAU * t + i * 2.1)
        cv.add(fill(circle(U - px, V - py, 0.07 * tw)) * fill(d), c2)


def art_ingot(cv, t, c1, c2, seed=2):
    u, v = rot(U, V, -0.45)
    d = box(u, v, 0.75, 0.3) - 0.05
    top = box(u, v + 0.1, 0.6, 0.18)
    cv.over(fill(d), c1 * 0.75)
    cv.over(fill(top), mix(c1, c2, 0.4))
    sheen = np.exp(-((u - (t * 3 - 1.5)) ** 2) / 0.03) * fill(d)
    cv.add(sheen, c2)


def art_plate(cv, t, c1, c2, seed=3):
    u, v = rot(U, V, 0.15)
    d = box(u, v, 0.7, 0.7) - 0.05
    cv.over(fill(d), mix(c1, c2, smooth(-1, 1, -u - v) * 0.6))
    cv.over(fill(np.abs(box(u, v, 0.55, 0.55)) - 0.02), c1 * 0.6)
    sheen = np.exp(-((u + v - (t * 4 - 2)) ** 2) / 0.05) * fill(d)
    cv.add(sheen * 0.8, c2)


def art_shard(cv, t, c1, c2, seed=4):
    u, v = rot(U, V, 0.6)
    d = np.maximum(np.abs(u) * 3.0 + v * 0.8 - 0.75, np.abs(v) - 0.85)
    cv.over(fill(d), mix(c1, c2, smooth(0.8, -0.8, v)))
    rng = np.random.default_rng(seed)
    for i in range(6):
        px, py = rng.uniform(-0.3, 0.3), rng.uniform(-0.7, 0.7)
        tw = max(0, math.sin(TAU * t * 2 + i * 1.9))
        cv.add(fill(circle(U - px, V - py, 0.05 + 0.05 * tw)) * fill(d), np.array([1, 1, 1.0]) * tw)


def art_bloch(cv, t, c1, c2, seed=5):
    cv.over(fill(R - 0.78), c1 * 0.25)
    cv.over(fill(np.abs(R - 0.74) - 0.04), c1)
    cv.add(fill(np.abs(V / 0.3 * 0.3) - 0.015) * fill(R - 0.74) * 0.6, c1)  # equator
    cv.add(fill(np.abs(U) - 0.015) * fill(R - 0.74) * 0.4, c1)
    a = TAU * t
    tx, ty = math.cos(a) * 0.5, -0.45 + math.sin(a) * 0.12
    cv.over(fill(seg(U, V, 0, 0, tx, ty, 0.05)), c2)
    cv.add(fill(circle(U - tx, V - ty, 0.13)), c2)


def art_pair(cv, t, c1, c2, seed=6):
    a = TAU * t
    p1 = (math.cos(a) * 0.45, math.sin(a) * 0.45)
    p2 = (-p1[0], -p1[1])
    cv.add(fill(seg(U, V, p1[0], p1[1], p2[0], p2[1], 0.02)) * (0.5 + 0.5 * np.sin(R * 20 - a * 4)), np.array([1, 1, 1.0]))
    cv.over(fill(circle(U - p1[0], V - p1[1], 0.22)), c1)
    cv.over(fill(circle(U - p2[0], V - p2[1], 0.22)), c2)
    cv.add(fill(circle(U - p1[0], V - p1[1], 0.1)), np.array([1, 1, 1.0]))
    cv.add(fill(circle(U - p2[0], V - p2[1], 0.1)), np.array([1, 1, 1.0]))


def art_dots(cv, t, c1, c2, seed=7):
    for i, (px, py) in enumerate([(-0.4, -0.4), (0.4, -0.4), (-0.4, 0.4), (0.4, 0.4), (0, 0)]):
        h = (t + i * 0.2) % 1
        col = mix(c1, c2, 0.5 + 0.5 * math.sin(TAU * h))
        r = 0.18 + 0.06 * math.sin(TAU * h)
        cv.over(fill(box(U - px, V - py, r, r) - 0.04), col)
        cv.add(fill(circle(U - px, V - py, r * 0.4)), np.array([1, 1, 1.0]) * 0.6)


def art_chip(cv, t, c1, c2, seed=8):
    d = box(U, V, 0.62, 0.62) - 0.06
    for i in range(-2, 3):
        for sgn in (-1, 1):
            cv.over(fill(box(U - i * 0.25, V - sgn * 0.78, 0.05, 0.12)), np.array([0.7, 0.7, 0.75]))
            cv.over(fill(box(U - sgn * 0.78, V - i * 0.25, 0.12, 0.05)), np.array([0.7, 0.7, 0.75]))
    cv.over(fill(d), c1)
    grid = (np.abs(np.sin(U * 9)) < 0.15) | (np.abs(np.sin(V * 9)) < 0.15)
    pulse = np.exp(-((R - t * 1.2) ** 2) / 0.01)
    cv.add(grid * fill(d + 0.08) * (0.25 + pulse), c2)


def art_casimir(cv, t, c1, c2, seed=9):
    for sx in (-1, 1):
        cv.over(fill(box(U - sx * 0.42, V, 0.13, 0.78) - 0.03), mix(c1, c1 * 1.4, smooth(-1, 1, -V)))
    gap = fill(box(U, V, 0.27, 0.7))
    wave = 0.5 + 0.5 * np.sin(V * 18 - TAU * t * 2)
    cv.add(gap * (0.35 + 0.5 * wave * fill(np.abs(U) - 0.12)), c2)


def art_hexgrid(cv, t, c1, c2, seed=10):
    d = box(U, V, 0.72, 0.72) - 0.05
    cv.over(fill(d), c1)
    q = U * 4.0
    r = V * 4.0 + q * 0.5
    hx = np.abs(((q + 0.5) % 1) - 0.5) + np.abs(((r + 0.5) % 1) - 0.5)
    lines = fill(np.abs(hx - 0.48) - 0.07)
    hue = 0.5 + 0.5 * np.sin(TAU * t + U * 3 + V * 2)
    cv.add(lines * fill(d + 0.04), mix(c2, np.array([0.8, 0.5, 1.0]), hue))


def art_edgeflow(cv, t, c1, c2, seed=11):
    d = box(U, V, 0.66, 0.66) - 0.06
    cv.over(fill(d), c1)
    edge = fill(np.abs(d + 0.06) - 0.07)
    ang = (A / TAU + 0.5 - t) % 1
    cv.add(edge * (0.3 + 0.7 * (ang < 0.25)), c2)
    cv.add(edge * 0.3, c2)


def art_coil(cv, t, c1, c2, seed=12):
    cv.over(fill(box(U, V, 0.18, 0.8) - 0.03), np.array([0.25, 0.25, 0.3]))
    for i in range(7):
        y = -0.66 + i * 0.22
        cv.over(fill(seg(U, V, -0.6, y - 0.06, 0.6, y + 0.06, 0.07)), c1)
        flow = max(0, math.sin(TAU * (t * 2 - i / 7)))
        cv.add(fill(seg(U, V, -0.6, y - 0.06, 0.6, y + 0.06, 0.03)) * flow, c2)


def art_capacitor(cv, t, c1, c2, seed=13):
    d = box(U, V + 0.05, 0.45, 0.65) - 0.1
    cv.over(fill(d), mix(c1, c1 * 2, smooth(-1, 1, U)))
    level = 0.65 - 1.3 * (0.5 + 0.5 * math.sin(TAU * t))
    cv.add(fill(box(U, V + 0.05, 0.3, 0.5)) * (V + 0.05 > level), c2)
    cv.over(fill(box(U, V + 0.8, 0.15, 0.1)), np.array([0.8, 0.8, 0.85]))


def art_streak(cv, t, c1, c2, seed=14):
    u, v = rot(U, V, -0.7)
    for k in range(3):
        off = ((t + k / 3) % 1) * 2.4 - 1.2
        d = box(u - off, v - (k - 1) * 0.25, 0.35, 0.05)
        cv.add(fill(d) * (1 - k * 0.25), mix(c2, c1, k / 2))
    d = np.maximum(np.abs(u) * 0.8 + np.abs(v) * 2.4 - 0.6, -1)
    cv.over(fill(d) * 0.9, mix(c2, c1, 0.5 + 0.5 * np.sin(u * 6 - TAU * t * 2)))


def art_clock(cv, t, c1, c2, seed=15):
    cv.over(fill(R - 0.8), c1)
    cv.over(fill(np.abs(R - 0.72) - 0.05), c2)
    for i in range(12):
        a = i * TAU / 12
        cv.over(fill(circle(U - math.cos(a) * 0.58, V - math.sin(a) * 0.58, 0.04)), c2 * 0.8)
    a1 = TAU * t - math.pi / 2
    a2 = -TAU * t * 3 - math.pi / 2   # runs backwards - it's a chronon capacitor
    cv.over(fill(seg(U, V, 0, 0, math.cos(a1) * 0.45, math.sin(a1) * 0.45, 0.05)), np.array([1, 1, 1.0]))
    cv.over(fill(seg(U, V, 0, 0, math.cos(a2) * 0.3, math.sin(a2) * 0.3, 0.06)), c2)


def art_contained(cv, t, c1, c2, seed=16):
    cv.over(fill(np.abs(R - 0.7) - 0.08), np.array([0.5, 0.5, 0.6]))
    a = TAU * t
    u, v = rot(U, V, a)
    cv.add(fill(np.abs(R - 0.55) - 0.03) * (0.5 + 0.5 * np.sin(A * 6 + a * 3)), c1)
    core = 0.18 + 0.04 * math.sin(a * 4)
    cv.over(fill(R - core), c1)
    cv.add(fill(R - core * 0.5), c2)


def art_pellet(cv, t, c1, c2, seed=17):
    cv.over(fill(R - 0.55), mix(c1, c1 * 0.4, smooth(-0.5, 0.6, U + V)))
    q = np.sin((U + V) * 8 + TAU * t) * np.sin((U - V) * 8 - TAU * t)
    cv.add(fill(R - 0.5) * np.clip(q, 0, 1) * 0.6, c2)
    cv.add(fill(circle(U + 0.2, V + 0.2, 0.12)), np.array([1, 1, 1.0]) * 0.7)


def art_monopole(cv, t, c1, c2, seed=18):
    for i in range(8):
        a = i * TAU / 8
        r0 = 0.35 + ((t + i / 8) % 1) * 0.5
        cv.add(fill(seg(U, V, math.cos(a) * 0.3, math.sin(a) * 0.3, math.cos(a) * r0, math.sin(a) * r0, 0.03)) * (1 - (r0 - 0.35) / 0.5), c2)
    cv.over(fill(R - 0.32), c1)
    cv.over(fill(box(U, V, 0.12, 0.03)), np.array([1, 1, 1.0]))
    cv.over(fill(box(U, V, 0.03, 0.12)), np.array([1, 1, 1.0]))


def art_string(cv, t, c1, c2, seed=19):
    for k in range(2):
        yy = 0.55 * np.sin(U * 3.2 + TAU * t * (1 + k) + k) * (1 - U * U * 0.6)
        cv.add(fill(np.abs(V - yy) - (0.06 if k == 0 else 0.03)) * fill(np.abs(U) - 0.85), c2 if k else c1)


def art_lens(cv, t, c1, c2, seed=20):
    d = (U / 0.8) ** 2 + (V / 0.55) ** 2 - 1
    cv.over(fill(d * 0.5), c1)
    u, v = rot(U, V, TAU * t)
    swirl = 0.5 + 0.5 * np.sin(A * 3 + R * 10 - TAU * t * 2)
    cv.add(fill(d * 0.5) * swirl * 0.6, c2)
    cv.over(fill(np.abs(d) * 0.4 - 0.04), c2 * 0.8)


def art_manifold(cv, t, c1, c2, seed=21):
    for k in range(5):
        a = TAU * (t + k / 5)
        rr = 0.35 + 0.25 * np.sin(A * 5 + a)
        cv.add(fill(np.abs(R - rr) - 0.03), mix(c1, c2, k / 4))
    cv.add(fill(R - 0.12), np.array([1, 1, 1.0]))


def art_planck(cv, t, c1, c2, seed=22):
    s = 0.15 + 0.08 * math.sin(TAU * t)
    for i in range(4):
        a = i * math.pi / 2 + TAU * t * 0.25
        cv.add(fill(seg(U, V, 0, 0, math.cos(a) * 0.8, math.sin(a) * 0.8, 0.02)), c2)
    cv.over(fill(box(*rot(U, V, math.pi / 4), s, s)), c1)
    cv.add(fill(R - 0.06), np.array([1, 1, 1.0]))


def art_emitter(cv, t, c1, c2, seed=23):
    cv.over(fill(box(U, V + 0.3, 0.45, 0.35) - 0.05), np.array([0.25, 0.25, 0.3]))
    cv.over(fill(R - 0.38), c1)
    for k in range(3):
        rr = ((t + k / 3) % 1) * 0.9
        cv.add(fill(np.abs(R - rr) - 0.03) * (V < 0.1) * (1 - rr / 0.9), c2)


def art_seed(cv, t, c1, c2, seed=24):
    for k in range(8):
        a = k * TAU / 8 + TAU * t * 0.5
        rr = 0.75 - ((t * 2 + k / 8) % 1) * 0.6
        cv.add(fill(circle(U - math.cos(a) * rr, V - math.sin(a) * rr, 0.06)), c1)
    cv.over(fill(R - 0.25 - 0.05 * math.sin(TAU * t * 2)), c1)
    cv.add(fill(R - 0.12), np.array([1, 1, 1.0]))


def art_warp(cv, t, c1, c2, seed=25):
    for k in range(4):
        rx = 0.75 - k * 0.15
        d = np.abs((U / rx) ** 2 + (V / (rx * 0.45)) ** 2 - 1) * rx * 0.5 - 0.03
        flow = 0.5 + 0.5 * np.sin(A * 2 - TAU * t * 2 + k)
        cv.over(fill(d), mix(c1, c2, flow))


def art_spindle(cv, t, c1, c2, seed=26):
    cv.over(fill(box(U, V, 0.08, 0.85)), np.array([0.6, 0.6, 0.65]))
    a = TAU * t
    for k in range(3):
        y = -0.4 + k * 0.4
        w = 0.5 * abs(math.cos(a + k))
        cv.over(fill(box(U, V - y, w + 0.05, 0.08)), mix(c1, c2, 0.5 + 0.5 * math.cos(a + k)))


def art_singularity(cv, t, c1, c2, seed=27):
    disk = (U / 0.85) ** 2 + (V / 0.28) ** 2 - 1
    swirl = 0.5 + 0.5 * np.sin(A * 4 - R * 8 + TAU * t * 2)
    cv.add(fill(disk * 0.3) * (0.4 + 0.6 * swirl) * fill(0.2 - R), mix(c2, np.array([1, 1, 0.9]), 1 - R))
    cv.over(fill(np.abs(R - 0.3) - 0.04), c2)
    cv.over(fill(R - 0.26), np.array([0, 0, 0.0]))


def art_whitehole(cv, t, c1, c2, seed=28):
    for i in range(10):
        a = i * TAU / 10 + TAU * t * 0.3
        L = 0.6 + 0.3 * math.sin(TAU * t * 2 + i)
        cv.add(fill(seg(U, V, 0, 0, math.cos(a) * L, math.sin(a) * L, 0.035)), c2)
    cv.add(fill(R - 0.45) * (1 - R / 0.45), c2)
    cv.over(fill(R - 0.22), c1)


def art_tesseract(cv, t, c1, c2, seed=29):
    s1 = 0.75
    s2 = 0.3 + 0.12 * math.sin(TAU * t)
    a = TAU * t * 0.25
    u, v = rot(U, V, a)
    outer = np.abs(box(u, v, s1, s1)) - 0.04
    inner = np.abs(box(u, v, s2, s2)) - 0.04
    cv.over(fill(outer), c1)
    cv.over(fill(inner), c2)
    for sx in (-1, 1):
        for sy in (-1, 1):
            x0, y0 = rot(np.array(sx * s1), np.array(sy * s1), -a)
            x1, y1 = rot(np.array(sx * s2), np.array(sy * s2), -a)
            cv.over(fill(seg(U, V, float(x0), float(y0), float(x1), float(y1), 0.035)), mix(c1, c2, 0.5))


def art_analyzer(cv, t, c1, c2, seed=30):
    cv.over(fill(box(U + 0.1, V + 0.1, 0.45, 0.6) - 0.08), c1 * 1.5)
    cv.over(fill(box(U + 0.1, V - 0.05, 0.32, 0.3)), np.array([0.02, 0.05, 0.06]))
    wave = 0.12 * np.sin((U + 0.1) * 14 - TAU * t * 2)
    cv.add(fill(np.abs(V - 0.0 - wave) - 0.035) * fill(box(U + 0.1, V - 0.05, 0.3, 0.28)), c2)
    cv.over(fill(seg(U, V, 0.35, -0.6, 0.75, -0.9, 0.06)), np.array([0.6, 0.6, 0.65]))
    cv.add(fill(circle(U - 0.75, V + 0.9, 0.08 + 0.04 * math.sin(TAU * t * 2))), c2)


def art_codex(cv, t, c1, c2, seed=31):
    cv.over(fill(box(U, V, 0.62, 0.78) - 0.04), c1 * 2)
    cv.over(fill(box(U + 0.62, V, 0.06, 0.78)), c2 * 0.5)
    a = TAU * t
    for k in range(3):
        rr = 0.18 + k * 0.12
        cv.add(fill(np.abs(np.sqrt((U * 1.0) ** 2 + (V / (0.4 + 0.3 * abs(math.cos(a + k)))) ** 2) - rr) - 0.02), c2)
    cv.add(fill(R - 0.07), np.array([1, 1, 1.0]))


def art_shifter(cv, t, c1, c2, seed=32):
    cv.over(fill(seg(U, V, -0.5, 0.7, 0.2, -0.1, 0.1)), np.array([0.3, 0.28, 0.35]))
    cv.over(fill(R - 0.0 + 0 - 0.0 + circle(U - 0.35, V + 0.35, 0.33) * 1), c1 * 2)
    for k in range(3):
        rr = ((t + k / 3) % 1) * 0.35
        cv.add(fill(np.abs(circle(U - 0.35, V + 0.35, rr)) - 0.03), c2)


def art_qstring(cv, t, c1, c2, seed=33):
    # closed string loop vibrating in its 3rd harmonic
    rr = 0.5 + 0.15 * np.sin(A * 3 + TAU * t * 2)
    cv.add(fill(np.abs(R - rr) - 0.05), c1)
    cv.add(fill(np.abs(R - rr) - 0.02), c2)


def art_brane(cv, t, c1, c2, seed=34):
    wave = 0.12 * np.sin(U * 4 + TAU * t) * np.cos(V * 3 - TAU * t)
    d = box(*rot(U, V + wave, 0.3), 0.75, 0.45)
    cv.over(fill(d), mix(c1, c2, 0.5 + 0.5 * np.sin(U * 5 - TAU * t)))
    cv.add(fill(np.abs(d) - 0.03), c2)


def art_foam(cv, t, c1, c2, seed=35):
    rng = np.random.default_rng(seed)
    pts = rng.uniform(-0.7, 0.7, (9, 2))
    for i, (px, py) in enumerate(pts):
        for j in range(i + 1, len(pts)):
            qx, qy = pts[j]
            if (px - qx) ** 2 + (py - qy) ** 2 < 0.35:
                cv.add(fill(seg(U, V, px, py, qx, qy, 0.02)), c1 * 1.5)
    for i, (px, py) in enumerate(pts):
        tw = 0.5 + 0.5 * math.sin(TAU * t * 2 + i)
        cv.over(fill(circle(U - px, V - py, 0.06 + 0.05 * tw)), mix(c1, c2, tw))


def art_multishard(cv, t, c1, c2, seed=36):
    d = np.maximum(np.abs(U) * 1.3 + np.abs(V) * 1.0 - 0.85, -1)
    hue = (t + (U + V) * 0.3) % 1
    col = np.stack([0.5 + 0.5 * np.sin(TAU * (hue + 0)), 0.5 + 0.5 * np.sin(TAU * (hue + 0.33)), 0.5 + 0.5 * np.sin(TAU * (hue + 0.66))], -1)
    cv.over(fill(d), col)
    cv.add(fill(np.abs(U - V * 0.3) - 0.02) * fill(d), np.array([1, 1, 1.0]) * 0.5)


def art_anchor(cv, t, c1, c2, seed=37):
    cv.over(fill(box(U, V, 0.12, 0.8)), np.array([0.35, 0.3, 0.45]))
    cv.over(fill(box(U, V + 0.55, 0.5, 0.1)), np.array([0.35, 0.3, 0.45]))
    tear = fill(np.abs(U - 0.08 * np.sin(V * 10 + TAU * t * 2)) - 0.04 * (1 - V * V)) * fill(np.abs(V) - 0.7)
    cv.add(tear, c2)


def art_useed(cv, t, c1, c2, seed=38):
    rr = 0.3 + 0.08 * math.sin(TAU * t)
    cv.add(fill(R - rr * 2) * (1 - R / (rr * 2)), c1)
    cv.over(fill(R - rr), mix(c1, c2, 0.5 + 0.5 * np.sin(A * 6 + TAU * t * 3)))


def art_universe(cv, t, c1, c2, seed=39):
    cv.over(fill(R - 0.8), c1)
    arms = 0.5 + 0.5 * np.sin(2 * A - np.log(R + 0.05) * 3.5 - TAU * t)
    cv.add(fill(R - 0.75) * arms ** 3 * (1 - R), np.array([0.6, 0.7, 1.0]))
    cv.add(fill(R - 0.2) * (1 - R / 0.2), c2)
    cv.over(fill(np.abs(R - 0.8) - 0.04), c2 * 0.8)


def art_remnant(cv, t, c1, c2, seed=40):
    cv.over(fill(box(U, V, 0.6, 0.6) - 0.1), mix(c1, c2, 0.5 + 0.2 * noise(U, V, seed, 3)))
    cv.add(fill(R - 0.08) * (0.5 + 0.5 * math.sin(TAU * t)) * 0.3, c2)


def art_neuron(cv, t, c1, c2, seed=41):
    for i in range(6):
        a = i * TAU / 6 + 0.3
        cv.over(fill(seg(U, V, 0, 0, math.cos(a) * 0.8, math.sin(a) * 0.8, 0.04)), c1 * 2)
        p = (t * 2 + i / 6) % 1
        cv.add(fill(circle(U - math.cos(a) * 0.8 * p, V - math.sin(a) * 0.8 * p, 0.07)), c2)
    cv.over(fill(R - 0.25), c2)


def art_lattice(cv, t, c1, c2, seed=42):
    cv.over(fill(box(U, V, 0.72, 0.72) - 0.04), c1)
    for i in range(-2, 3):
        cv.add(fill(np.abs(U - i * 0.3) - 0.015) * fill(box(U, V, 0.7, 0.7)), c2 * 0.4)
        cv.add(fill(np.abs(V - i * 0.3) - 0.015) * fill(box(U, V, 0.7, 0.7)), c2 * 0.4)
        for j in range(-2, 3):
            on = math.sin(TAU * t * 2 + i * 1.3 + j * 2.1) > 0.2
            cv.add(fill(circle(U - i * 0.3, V - j * 0.3, 0.07)) * on, c2)


def art_aicore(cv, t, c1, c2, seed=43):
    d = box(*rot(U, V, math.pi / 4), 0.55, 0.55) - 0.05
    cv.over(fill(d), c1 * 1.5)
    cv.over(fill(np.abs(d) - 0.03), c2 * 0.7)
    eye = 0.22 + 0.04 * math.sin(TAU * t * 2)
    cv.add(fill(R - eye) * (1 - R / eye * 0.5), c2)
    cv.add(fill(np.abs(R - 0.38) - 0.02) * (((A / TAU + t) % 0.25) < 0.12), c2)


def art_sail(cv, t, c1, c2, seed=44):
    d = np.maximum(np.abs(U) + np.abs(V) - 0.8, -1)
    cv.over(fill(d), mix(c1, c2, 0.3 + 0.5 * np.exp(-((U - V - (t * 3 - 1.5)) ** 2) / 0.05)))
    cv.over(fill(np.abs(d) - 0.03), np.array([0.8, 0.8, 0.85]))


def art_satellite(cv, t, c1, c2, seed=45):
    for sx in (-1, 1):
        cv.over(fill(box(U - sx * 0.55, V, 0.28, 0.2)), mix(np.array([0.15, 0.2, 0.45]), c2, 0.3 + 0.3 * math.sin(TAU * t + sx)))
    cv.over(fill(box(U, V, 0.2, 0.25) - 0.04), c1 * 1.6)
    cv.add(fill(circle(U, V + 0.32, 0.06)) * (math.sin(TAU * t * 2) > 0), c2)


def art_shell(cv, t, c1, c2, seed=46):
    d = np.maximum(np.abs(R - 1.2) - 0.35, np.abs(A + math.pi / 2) - 0.6)
    vv = V + 0.2
    d = np.maximum(np.abs(np.sqrt(U * U + (vv + 1.0) ** 2) - 1.3) - 0.35, np.abs(U) - 0.75)
    cv.over(fill(d), c1 * 1.5)
    seams = fill(np.abs(np.sin(U * 6)) - 0.12) * fill(d)
    cv.add(seams * (0.4 + 0.6 * shimmer(t)), c2)


def art_star(cv, t, c1, c2, seed=47):
    cv.add(fill(R - 0.9) * (1 - R / 0.9) ** 2, c1)
    gran = noise(U * 2 + t, V * 2, seed, 6)
    cv.over(fill(R - 0.48), mix(c1, c2, gran * 0.8 + 0.2))
    cv.add(fill(R - 0.2), c2)




# ---------------------------------------------------------------- volumetric helpers
def poly(pts):
    """Coverage of a convex polygon (points in order, any winding)."""
    inside_pos = np.ones((S, S), bool)
    inside_neg = np.ones((S, S), bool)
    n = len(pts)
    for i in range(n):
        (x0, y0), (x1, y1) = pts[i], pts[(i + 1) % n]
        c = (x1 - x0) * (V - y0) - (y1 - y0) * (U - x0)
        inside_pos &= c >= 0
        inside_neg &= c <= 0
    return (inside_pos | inside_neg).astype(float)


def orb(cv, cx, cy, r, col, hi=None, glow=0.0, rim=None):
    """Shaded sphere: radial falloff toward lower-right, specular dot top-left, optional glow halo and rim light."""
    d = np.sqrt((U - cx) ** 2 + (V - cy) ** 2)
    if glow > 0:
        cv.add(fill(d - r * 1.7) * np.clip(1 - d / (r * 1.7), 0, 1) ** 2 * glow, col)
    shade = np.clip(1.15 - np.sqrt((U - cx + r * 0.35) ** 2 + (V - cy + r * 0.35) ** 2) / (r * 1.6), 0.25, 1.1)
    base = col * shade[..., None]
    cv.over(fill(d - r), base, dome=1.4)
    if rim is not None:
        cv.add(fill(np.abs(d - r * 0.92) - r * 0.06) * smooth(-0.2, 0.6, (U - cx + V - cy) / r), rim)
    cv.add(fill(circle(U - cx + r * 0.38, V - cy + r * 0.4, r * 0.18)) * 0.8, hi if hi is not None else np.array([1, 1, 1.0]))


def iso_cube(cv, cx, cy, s, top, left, right):
    """Isometric cube; returns coverages (top, left, right) for decorating faces."""
    h = s * 0.58
    t = poly([(cx, cy - s), (cx + s, cy - s + h), (cx, cy - s + 2 * h), (cx - s, cy - s + h)])
    l = poly([(cx - s, cy - s + h), (cx, cy - s + 2 * h), (cx, cy + s), (cx - s, cy + s - h)])
    r = poly([(cx, cy - s + 2 * h), (cx + s, cy - s + h), (cx + s, cy + s - h), (cx, cy + s)])
    cv.over(l, left, dome=0.2)
    cv.over(r, right, dome=0.2)
    cv.over(t, top, dome=0.2)
    return t, l, r


def art_pair(cv, t, c1, c2, seed=6):
    a = TAU * t
    p1 = (math.cos(a) * 0.42, math.sin(a) * 0.3)
    p2 = (-p1[0], -p1[1])
    # entanglement link: a standing wave between the photons
    u = np.linspace(0, 1, 1)
    for k in range(9):
        f = k / 8
        x = p1[0] + (p2[0] - p1[0]) * f
        y = p1[1] + (p2[1] - p1[1]) * f + 0.08 * math.sin(f * math.pi * 3 + a * 4)
        cv.add(fill(circle(U - x, V - y, 0.05)) * 0.8, mix(c1, c2, f))
    orb(cv, p1[0], p1[1], 0.27, c1, glow=0.7)
    orb(cv, p2[0], p2[1], 0.27, c2, glow=0.7)


def art_seed(cv, t, c1, c2, seed=24):
    for k in range(10):
        a = k * TAU / 10 + TAU * t * 0.3
        rr = 0.95 - ((t * 2 + k / 10) % 1) * 0.5
        cv.add(fill(seg(U, V, math.cos(a) * rr, math.sin(a) * rr, math.cos(a) * (rr - 0.18), math.sin(a) * (rr - 0.18), 0.035)), c1)
    orb(cv, 0, 0, 0.42 + 0.04 * math.sin(TAU * t * 2), c1, glow=0.8, rim=np.array([1, 1, 1.0]))
    cv.add(fill(R - 0.16), np.array([1, 1, 1.0]))


def art_planck(cv, t, c1, c2, seed=22):
    a = TAU * t * 0.25
    pts = [(math.cos(a + k * math.pi / 2) * 0.8, math.sin(a + k * math.pi / 2) * 0.8) for k in range(4)]
    cv.over(poly(pts), mix(c2, c1, 0.4), dome=1.2)
    grid = (np.abs(((U * 5) % 1) - 0.5) < 0.06) | (np.abs(((V * 5) % 1) - 0.5) < 0.06)
    cv.add(poly(pts) * grid * (0.4 + 0.4 * shimmer(t)), c2)
    cv.add(fill(R - 0.12) * (1 - R / 0.12), np.array([1, 1, 1.0]))


def art_useed(cv, t, c1, c2, seed=38):
    orb(cv, 0, 0, 0.6, c1 * 0.8, glow=0.6, rim=c2)
    sw = 0.5 + 0.5 * np.sin(A * 3 + R * 12 - TAU * t * 2)
    cv.add(fill(R - 0.48) * sw * 0.6, c2)
    cv.add(fill(R - 0.12), np.array([1, 1, 1.0]))


def art_foam(cv, t, c1, c2, seed=35):
    rng = np.random.default_rng(seed)
    pts = rng.uniform(-0.65, 0.65, (8, 2))
    for i, (px, py) in enumerate(pts):
        for j in range(i + 1, len(pts)):
            qx, qy = pts[j]
            if (px - qx) ** 2 + (py - qy) ** 2 < 0.45:
                cv.over(fill(seg(U, V, px, py, qx, qy, 0.045)), c1 * 1.4, dome=0.6)
    for i, (px, py) in enumerate(pts):
        tw = 0.5 + 0.5 * math.sin(TAU * t * 2 + i)
        orb(cv, px, py, 0.11 + 0.04 * tw, mix(c1, c2, tw), glow=0.4)


def art_monopole(cv, t, c1, c2, seed=18):
    for i in range(8):
        a = i * TAU / 8 + 0.2
        r0 = 0.5 + ((t + i / 8) % 1) * 0.42
        cv.add(fill(seg(U, V, math.cos(a) * 0.45, math.sin(a) * 0.45, math.cos(a) * r0, math.sin(a) * r0, 0.045)) * (1 - (r0 - 0.5) / 0.42), c2)
    orb(cv, 0, 0, 0.5, c1, rim=c2)
    # "N" glyph
    cv.add(fill(seg(U, V, -0.16, 0.2, -0.16, -0.2, 0.05)) + fill(seg(U, V, -0.16, -0.2, 0.16, 0.2, 0.05)) + fill(seg(U, V, 0.16, 0.2, 0.16, -0.2, 0.05)), np.array([1, 1, 1.0]))


def art_qstring(cv, t, c1, c2, seed=33):
    rr = 0.55 + 0.16 * np.sin(A * 3 + TAU * t * 2)
    cv.add(fill(np.abs(R - rr) - 0.16) * 0.35, c1)
    cv.over(fill(np.abs(R - rr) - 0.08), c1, dome=0.8)
    cv.add(fill(np.abs(R - rr) - 0.03), c2)


def art_string(cv, t, c1, c2, seed=19):
    for k in range(2):
        yy = 0.5 * np.sin(U * 3.2 + TAU * t + k * math.pi) * (1 - U * U * 0.5)
        m = fill(np.abs(U) - 0.88)
        cv.add(fill(np.abs(V - yy) - 0.15) * m * 0.3, c1)
        cv.over(fill(np.abs(V - yy) - 0.075) * m, c1 if k == 0 else c1 * 0.8, dome=0.7)
        cv.add(fill(np.abs(V - yy) - 0.025) * m, c2)


def art_remnant(cv, t, c1, c2, seed=40):
    top, left, right = iso_cube(cv, 0, 0.02, 0.78, c2 * 0.9, c2 * 0.6, c2 * 0.4)
    n = noise(U, V, seed, 9)
    cracks = fill(np.abs(n - 0.5) - 0.02)
    cv.over(cracks * (top + left + right), c1 * 0.8, dome=0)
    ember = max(0, math.sin(TAU * t)) * 0.4
    cv.add(cracks * left * ember, np.array([1.0, 0.4, 0.15]))


def art_edgeflow(cv, t, c1, c2, seed=11):
    top, left, right = iso_cube(cv, 0, 0.02, 0.78, c1 * 1.8, c1 * 1.2, c1 * 0.8)
    edges = fill(np.abs(box(U, V, 0.0, 0.0)) - 0.0)  # placeholder
    s_ = 0.78
    h = s_ * 0.58
    lines = [((0, -s_), (s_, -s_ + h)), ((s_, -s_ + h), (0, -s_ + 2 * h)), ((0, -s_ + 2 * h), (-s_, -s_ + h)), ((-s_, -s_ + h), (0, -s_)),
             ((-s_, -s_ + h), (-s_, s_ - h)), ((s_, -s_ + h), (s_, s_ - h)), ((0, -s_ + 2 * h), (0, s_)), ((-s_, s_ - h), (0, s_)), ((0, s_), (s_, s_ - h))]
    for i, ((x0, y0), (x1, y1)) in enumerate(lines):
        flow = 0.4 + 0.6 * max(0, math.sin(TAU * t * 2 - i * 0.7))
        cv.add(fill(seg(U, V, x0, y0 + 0.02, x1, y1 + 0.02, 0.035)) * flow, c2)


def art_manifold(cv, t, c1, c2, seed=21):
    for k in range(3):
        a = TAU * t * (0.5 + k * 0.2)
        rr = 0.3 + 0.42 * np.abs(np.cos((A + a) * (3 + k) / 2))
        col = mix(c1, c2, k / 2)
        cv.over(fill(R - rr) * fill(R - 0.95), col * (0.7 + 0.15 * k), dome=0.9)
    orb(cv, 0, 0, 0.18, np.array([1, 1, 1.0]), glow=0.6)


def art_streak(cv, t, c1, c2, seed=14):
    u, v = rot(U, V, -0.75)
    for k in range(3):
        off = ((t + k / 3) % 1) * 2.6 - 1.3
        cv.add(fill(box(u - off, v - (k - 1) * 0.32, 0.4, 0.04)) * (1 - k * 0.25), mix(c2, c1, k / 2))
    d = np.maximum(np.abs(u) * 0.75 + np.abs(v) * 2.2 - 0.62, -1)
    cv.over(fill(d), mix(c2 * 0.85, c1, 0.5 + 0.5 * np.sin(u * 5 - TAU * t * 2)), dome=1.2)
    cv.add(fill(seg(u, v, -0.6, 0, 0.6, 0, 0.03)) * fill(d), np.array([1, 1, 1.0]) * 0.8)


def art_contained(cv, t, c1, c2, seed=16):
    # magnetic bottle: glass capsule, coils, levitating antimatter core
    cap = box(U, V, 0.42, 0.72) - 0.2
    cv.over(fill(cap), np.array([0.16, 0.18, 0.26]), dome=1.0)
    for y in (-0.55, 0.0, 0.55):
        cv.over(fill(box(U, V - y, 0.62, 0.07) - 0.03), np.array([0.75, 0.42, 0.2]), dome=0.6)
    a = TAU * t
    orb(cv, 0.06 * math.sin(a * 2), 0.08 * math.cos(a), 0.22, c1, glow=0.9, hi=c2)


def art_emitter(cv, t, c1, c2, seed=23):
    cv.over(fill(box(U, V + 0.45, 0.35, 0.32) - 0.05), np.array([0.28, 0.28, 0.33]), dome=0.8)
    dish = fill(R - 0.62) * (V < 0.12)
    cv.over(dish, np.array([0.55, 0.55, 0.62]), dome=1.0)
    cv.over(fill(R - 0.48) * (V < 0.05), np.array([0.12, 0.1, 0.16]), dome=0.5)
    orb(cv, 0, -0.1, 0.15, c2, glow=0.8)
    for k in range(3):
        rr = 0.62 + ((t + k / 3) % 1) * 0.35
        cv.add(fill(np.abs(R - rr) - 0.03) * (V < -0.1) * (1 - (rr - 0.62) / 0.35), c2)


def art_whitehole(cv, t, c1, c2, seed=28):
    for i in range(12):
        a = i * TAU / 12 + TAU * t * 0.3
        L = 0.75 + 0.2 * math.sin(TAU * t * 2 + i)
        cv.add(fill(seg(U, V, 0, 0, math.cos(a) * L, math.sin(a) * L, 0.05)) * 0.9, c2)
    cv.add(fill(R - 0.7) * np.clip(1 - R / 0.7, 0, 1) ** 1.5, c2)
    orb(cv, 0, 0, 0.32, c1, glow=0.6)


def art_dots(cv, t, c1, c2, seed=7):
    for i, (px, py) in enumerate([(-0.42, -0.42), (0.42, -0.42), (-0.42, 0.42), (0.42, 0.42), (0, 0)]):
        h = (t + i * 0.2) % 1
        col = mix(c1, c2, 0.5 + 0.5 * math.sin(TAU * h))
        orb(cv, px, py, 0.22 + 0.04 * math.sin(TAU * h), col, glow=0.35)




# ---------------------------------------------------------------- expansion art
def art_negentropy(cv, t, c1, c2, seed=60):
    cap = box(U, V, 0.5, 0.75) - 0.15
    cv.over(fill(cap), np.array([0.14, 0.16, 0.22]), dome=1.0)
    inner = fill(box(U, V, 0.36, 0.6))
    # left: ordered lattice (cold), right: chaos (hot) - a demon keeps them apart
    lat = ((np.abs(((U * 6) % 1) - 0.5) < 0.15) & (np.abs(((V * 6 + t * 2) % 1) - 0.5) < 0.15)) * (U < 0)
    rng = np.random.default_rng(int(t * 16))
    hot = np.zeros((S, S))
    for _ in range(10):
        hot += fill(circle(U - rng.uniform(0.05, 0.33), V - rng.uniform(-0.55, 0.55), 0.05))
    cv.add(inner * lat, c2)
    cv.add(inner * np.clip(hot, 0, 1), c1)
    cv.over(fill(box(U, V, 0.025, 0.6)), np.array([0.8, 0.8, 0.9]), dome=0.3)


def art_demonchip(cv, t, c1, c2, seed=61):
    art_chip(cv, t, c1, c2, seed)
    blink = 1.0 if (t * 4) % 1 > 0.15 else 0.1
    cv.add(fill(((U / 0.3) ** 2 + (V / 0.14) ** 2) - 1) * blink, np.array([1.0, 0.3, 0.2]))
    cv.add(fill(R - 0.07) * blink, np.array([0.1, 0.0, 0.0]))


def art_holo(cv, t, c1, c2, seed=62):
    top, left, right = iso_cube(cv, 0, 0.02, 0.75, c1 * 0.5 + 0.1, c1 * 0.35, c1 * 0.25)
    fr = 0.5 + 0.5 * np.sin((U * 0.7 + V) * 22 - TAU * t * 2)
    hue = (U + V) * 0.5 + t
    rainbow = np.stack([0.5 + 0.5 * np.sin(TAU * hue), 0.5 + 0.5 * np.sin(TAU * (hue + 0.33)), 0.5 + 0.5 * np.sin(TAU * (hue + 0.66))], -1)
    cv.add((top + left + right) * fr * 0.45, rainbow)


def art_code(cv, t, c1, c2, seed=63):
    cv.over(fill(box(U, V, 0.62, 0.8) - 0.06), np.array([0.04, 0.06, 0.05]), dome=0.8)
    rng = np.random.default_rng(seed)
    for col in range(7):
        x = -0.48 + col * 0.16
        speed = rng.uniform(0.6, 1.6)
        for k in range(6):
            y = ((k / 6 + t * speed + rng.random()) % 1) * 1.5 - 0.75
            on = rng.random() > 0.25
            cv.add(fill(box(U - x, V - y, 0.045, 0.035)) * on * (0.4 + 0.6 * (k == 5)), c2)


def art_realanchor(cv, t, c1, c2, seed=64):
    art_anchor(cv, t, c1, c2, seed)
    g = (t * 8) % 1 < 0.25
    if g:
        band = np.abs(V - (t * 2 % 1 - 0.5)) < 0.08
        cv.rgb = np.where(band[..., None], np.roll(cv.rgb, int(S * 0.04), axis=1), cv.rgb)


def art_omega(cv, t, c1, c2, seed=65):
    for k in range(4):
        rr = 0.95 - ((t + k / 4) % 1) * 0.55
        cv.add(fill(np.abs(R - rr) - 0.025) * (1 - (0.95 - rr) / 0.55), c2)
    orb(cv, 0, 0, 0.42, c1, glow=0.7, rim=c2)
    om = fill(np.abs(np.sqrt(U ** 2 + (V + 0.03) ** 2) - 0.2) - 0.04) * (V < 0.12)
    om += fill(box(U + 0.15, V - 0.17, 0.08, 0.035)) + fill(box(U - 0.15, V - 0.17, 0.08, 0.035))
    cv.add(np.clip(om, 0, 1), np.array([1, 1, 1.0]))


def art_swarm(cv, t, c1, c2, seed=66):
    rng = np.random.default_rng(seed)
    for i in range(40):
        a0, r0 = rng.uniform(0, TAU), rng.uniform(0.1, 0.75)
        sp = rng.uniform(0.5, 1.5)
        a = a0 + TAU * t * sp
        x, y = math.cos(a) * r0, math.sin(a * 1.3) * r0 * 0.8
        orb(cv, x, y, 0.055, mix(c1, c2, rng.random()), hi=c2)


def art_progmatter(cv, t, c1, c2, seed=67):
    m = 0.5 + 0.5 * math.sin(TAU * t)
    p = 2 + m * 6
    d = (np.abs(U) ** p + np.abs(V) ** p) ** (1 / p) - 0.62
    cv.over(fill(d), mix(c1, c2, 0.3), dome=1.4)
    grid = (np.abs(((U * 6) % 1) - 0.5) < 0.07) | (np.abs(((V * 6) % 1) - 0.5) < 0.07)
    cv.add(fill(d) * grid * 0.4, c2)


def art_catom(cv, t, c1, c2, seed=68):
    orb(cv, 0, 0, 0.62, c1, rim=c2)
    q = U * 4.0
    r = V * 4.0 + q * 0.5
    hx = np.abs(((q + 0.5) % 1) - 0.5) + np.abs(((r + 0.5) % 1) - 0.5)
    cv.add(fill(R - 0.58) * fill(np.abs(hx - 0.48) - 0.05) * (0.3 + 0.4 * shimmer(t)), c2)


def art_probe(cv, t, c1, c2, seed=69):
    for sx in (-1, 1):
        cv.over(poly([(sx * 0.2, -0.12), (sx * 0.9, -0.3), (sx * 0.9, 0.3), (sx * 0.2, 0.12)]), np.array([0.15, 0.22, 0.5]), dome=0.3)
        cv.add(poly([(sx * 0.2, -0.12), (sx * 0.9, -0.3), (sx * 0.9, 0.3), (sx * 0.2, 0.12)]) * (np.abs(((U * 8) % 1) - 0.5) < 0.08) * 0.4, c2)
    cv.over(fill(box(U, V, 0.2, 0.32) - 0.06), c1 * 1.5, dome=1.0)
    cv.over(fill(circle(U, V - 0.4, 0.13)), np.array([0.5, 0.5, 0.55]), dome=1.0)
    cv.add(fill(circle(U, V + 0.05, 0.08 + 0.03 * math.sin(TAU * t * 2))), c2)


def art_damper(cv, t, c1, c2, seed=70):
    a = TAU * t
    for k, (rx, ry) in enumerate(((0.8, 0.8 * abs(math.cos(a))), (0.8 * abs(math.cos(a * 0.7 + 1)), 0.8), (0.6, 0.6 * abs(math.sin(a)) + 0.05))):
        d = np.abs((U / max(rx, 0.05)) ** 2 + (V / max(ry, 0.05)) ** 2 - 1) * min(rx, ry, 0.4) * 0.5 - 0.03
        cv.over(fill(d), mix(c1, c2, k / 2), dome=0.7)
    orb(cv, 0, 0, 0.2, c2, glow=0.5)


def art_ghostorb(cv, t, c1, c2, seed=71):
    fade = 0.35 + 0.35 * math.sin(TAU * t)
    cv.add(fill(R - 0.7) * np.clip(1 - R / 0.7, 0, 1) * (0.5 + fade), c1)
    cv.add(fill(np.abs(R - 0.45) - 0.03) * (0.5 + 0.5 * np.sin(A * 3 - TAU * t * 3)), c2)
    cv.add(fill(R - 0.12), np.array([1, 1, 1.0]) * (0.5 + fade))


def art_quarkstar(cv, t, c1, c2, seed=72):
    orb(cv, 0, 0, 0.6, c1 * 0.7, glow=0.5, rim=c2)
    for k, col in enumerate((np.array([1, 0.25, 0.25]), np.array([0.25, 1, 0.3]), np.array([0.3, 0.45, 1]))):
        a = TAU * (t + k / 3)
        orb(cv, math.cos(a) * 0.28, math.sin(a) * 0.28, 0.14, col, glow=0.6)
    cv.add(fill(R - 0.3) * 0.2, c2)


def art_throat(cv, t, c1, c2, seed=73):
    cv.over(fill(R - 0.85), c1 * 0.4, dome=1.0)
    for k in range(6):
        f = ((k / 6 + t) % 1)
        rr = 0.82 * (1 - f) ** 1.6 + 0.04
        cv.add(fill(np.abs(R - rr) - 0.02 * (1 - f) - 0.008) * (1 - f), mix(c2, c1, f))
    cv.over(fill(np.abs(R - 0.8) - 0.07), mix(c1, c2, 0.5), dome=0.8)
    cv.add(fill(R - 0.1), np.array([1, 1, 1.0]))


def art_loop(cv, t, c1, c2, seed=74):
    # closed timelike curve: a lemniscate that something keeps travelling around
    tt = np.linspace(0, TAU, 80)
    for i, s_ in enumerate(tt):
        x, y = 0.75 * math.cos(s_) / (1 + math.sin(s_) ** 2), 0.75 * math.sin(s_) * math.cos(s_) / (1 + math.sin(s_) ** 2)
        cv.over(fill(circle(U - x, V - y, 0.08)), mix(c1, c2, 0.5 + 0.5 * math.sin(s_)), dome=0.6, raise_=0.02)
    p = TAU * t
    x, y = 0.75 * math.cos(p) / (1 + math.sin(p) ** 2), 0.75 * math.sin(p) * math.cos(p) / (1 + math.sin(p) ** 2)
    orb(cv, x, y, 0.12, np.array([1, 1, 1.0]), glow=0.8)


def art_artifact(cv, t, c1, c2, seed=75):
    cv.over(poly([(0, -0.85), (0.6, -0.2), (0.35, 0.8), (-0.35, 0.8), (-0.6, -0.2)]), c1 * 1.3, dome=1.0)
    cv.over(poly([(0, -0.55), (0.35, -0.12), (0.2, 0.5), (-0.2, 0.5), (-0.35, -0.12)]), c1 * 0.6, dome=0.6)
    for k in range(3):
        a = TAU * (t + k / 3)
        cv.add(fill(circle(U - math.cos(a) * 0.15, V - 0.05 - math.sin(a) * 0.15, 0.06)), c2)


def art_lambda(cv, t, c1, c2, seed=76):
    cv.over(fill(box(U, V, 0.7, 0.7) - 0.08), c1, dome=0.9)
    g = 0.6 + 0.4 * shimmer(t)
    cv.add((fill(seg(U, V, 0, -0.45, -0.38, 0.45, 0.07)) + fill(seg(U, V, 0, -0.45, 0.38, 0.45, 0.07))) * g, c2)
    for k in range(3):
        rr = ((t + k / 3) % 1) * 0.95
        cv.add(fill(np.abs(box(U, V, rr, rr)) - 0.015) * (1 - rr) * 0.6, c2)


def art_brain(cv, t, c1, c2, seed=77):
    d = (U / 0.78) ** 2 + ((V + 0.05) / 0.6) ** 2 - 1
    cv.over(fill(d * 0.4), c1, dome=1.3)
    folds = 0.5 + 0.5 * np.sin(U * 14 + np.sin(V * 9) * 2) * np.sin(V * 12 + np.cos(U * 7))
    cv.over(fill(d * 0.4 + 0.05) * (folds > 0.72), c1 * 0.7, dome=0.2, raise_=0.03)
    cv.over(fill(box(U, V, 0.02, 0.5)) * fill(d * 0.4), c1 * 0.5, dome=0)
    spark = 0.5 + 0.5 * np.sin(A * 7 + R * 15 - TAU * t * 3)
    cv.add(fill(d * 0.4) * (spark > 0.93), c2)


ITEM_ART = {k[4:]: v for k, v in globals().items() if k.startswith('art_')}

# =====================================================================================================
# BLOCK ART (opaque 16x16)
# =====================================================================================================


def blk_base(name):
    return {'deepslate': (np.array([0.24, 0.24, 0.27]), np.array([0.15, 0.15, 0.17])),
            'end_stone': (np.array([0.86, 0.87, 0.62]), np.array([0.7, 0.71, 0.48])),
            'bulk_stone': (np.array([0.06, 0.04, 0.10]), np.array([0.18, 0.12, 0.28])),
            'netherrack': (np.array([0.45, 0.16, 0.16]), np.array([0.28, 0.08, 0.09])),
            'axiom_stone': (np.array([0.86, 0.87, 0.92]), np.array([0.70, 0.72, 0.80])),
            'foam_substrate': (np.array([0.62, 0.80, 0.92]), np.array([0.36, 0.50, 0.66])),
            'ordinal_stone': (np.array([0.16, 0.15, 0.30]), np.array([0.08, 0.07, 0.18])),
            'infall_rock': (np.array([0.18, 0.05, 0.04]), np.array([0.08, 0.02, 0.02])),
            'ejecta_rock': (np.array([0.94, 0.92, 0.86]), np.array([0.80, 0.78, 0.72])),
            'antistone': (np.array([0.24, 0.15, 0.10]), np.array([0.14, 0.08, 0.05]))}.get(name, (np.array([0.4, 0.4, 0.4]), np.array([0.25, 0.25, 0.25])))


FORMED_MODE = [False]


def formed_frame(t, art, c1, c2, seed=0, glass=False):
    """
    The 'transformed' look of a casing inside a complete multiblock: no frame or bolts (so neighbouring blocks merge
    into one surface), brighter, with energy conduits on the centre lines. The conduits run edge to edge, so across
    the whole structure they join into a single flowing network; the pulse travels a whole tile per loop, so it
    continues seamlessly from one block into the next and loops cleanly.
    """
    FORMED_MODE[0] = True
    try:
        img = block_frame(t, art, c1, c2, None, seed)
    finally:
        FORMED_MODE[0] = False
    n = img.shape[0]
    glow = np.clip(c2 * 1.1 + 0.1, 0, 1)
    img[..., :3] = np.clip(img[..., :3] * 1.12 + 0.02, 0, 1)
    mid = n // 2
    for i in range(n):
        pulse = 0.35 + 0.65 * max(0.0, math.cos(TAU * (i / n - t))) ** 6
        for (y, x) in ((mid, i), (mid - 1, i), (i, mid), (i, mid - 1)):
            a = pulse * (0.55 if glass else 0.85)
            img[y, x, :3] = img[y, x, :3] * (1 - a) + glow * a
            if glass:
                img[y, x, 3] = max(img[y, x, 3], 0.45 + 0.4 * pulse)
    return img


def block_frame(t, art, c1, c2, base=None, seed=0):
    cv = Canvas()
    n = noise(U, V, seed + 11, 6)
    if art == 'ore':
        b1, b2 = blk_base(base)
        cv.over(np.ones((S, S)), mix(b2, b1, n))
        rng = np.random.default_rng(seed)
        for i in range(5):
            px, py = rng.uniform(-0.65, 0.65, 2)
            tw = 0.6 + 0.4 * math.sin(TAU * t + i * 1.3)
            d = np.maximum(np.abs(U - px) * 1.3 + np.abs(V - py) - 0.22, -1)
            cv.over(fill(d), mix(c1, c2, tw))
            cv.add(fill(circle(U - px, V - py, 0.05)) * tw, np.array([1, 1, 1.0]))
    elif art == 'axiomstone':
        # pale marble of pure form, faintly ruled with a coordinate grid that pulses along its axes
        cv.over(np.ones((S, S)), mix(np.array([0.72, 0.74, 0.82]), np.array([0.90, 0.91, 0.95]), n))
        vein = fill(np.abs(noise(U, V, seed + 7, 3) - 0.5) - 0.025)
        cv.over(vein * 0.5, np.array([0.62, 0.64, 0.74]))
        grid = (fill(np.abs(((U + 1) * 2) % 1 - 0.5) - 0.47) + fill(np.abs(((V + 1) * 2) % 1 - 0.5) - 0.47)).clip(0, 1)
        cv.add(grid * (0.08 + 0.08 * math.sin(TAU * t)), c2)
    elif art == 'bulkstone':
        cv.over(np.ones((S, S)), mix(c1, c2, n * 0.6))
        vein = fill(np.abs(noise(U, V, seed + 3, 3) - 0.5) - 0.03)
        cv.add(vein * (0.3 + 0.3 * math.sin(TAU * t)), np.array([0.5, 0.3, 0.9]))
    elif art in ('bulkcrystal', 'crystalblock'):
        cv.over(np.ones((S, S)), mix(c1, c2, n))
        facets = fill(np.abs(np.sin((U + V) * 5)) - 0.1) + fill(np.abs(np.sin((U - V) * 4)) - 0.08)
        cv.add(np.clip(facets, 0, 1) * 0.5, c2)
        sweep = np.exp(-((U + V - (t * 6 - 3)) ** 2) / 0.04)
        cv.add(sweep * 0.7, np.array([1, 1, 1.0]))
    elif art == 'metalblock':
        cv.over(np.ones((S, S)), mix(c1 * 0.8, c1, n))
        cv.over(fill(np.abs(box(U, V, 0.9, 0.9)) - 0.04), c1 * 0.5)
        sheen = np.exp(-((U - V - (t * 6 - 3)) ** 2) / 0.06)
        cv.add(sheen * 0.6, c2)
    elif art in ('casing', 'casing2', 'riftframe', 'cosmic', 'stellarframe', 'hyperframe', 'circuit', 'coilblock', 'emitterblock',
                 'magcasing', 'chrono', 'wormframe', 'reality', 'dipole', 'quadrupole', 'rfcavity', 'beampipe', 'cryo', 'vacuum',
                 'detector', 'injector', 'lensarray'):
        cv.over(np.ones((S, S)), mix(c1 * 0.8, c1 * 1.2, n))
        if not FORMED_MODE[0]:
            rim = fill(np.abs(box(U, V, 0.93, 0.93)) - 0.05)
            cv.over(rim, c1 * 1.8 + 0.05)
            bolt = sum(fill(circle(U - sx * 0.78, V - sy * 0.78, 0.07)) for sx in (-1, 1) for sy in (-1, 1))
            cv.over(np.clip(bolt, 0, 1), np.array([0.55, 0.57, 0.62]))
        p = shimmer(t)
        if art == 'casing':
            cv.add(fill(np.abs(box(U, V, 0.55, 0.55)) - 0.03) * (0.4 + 0.6 * p), c2)
        elif art == 'casing2':
            ring = fill(np.abs(R - 0.5) - 0.06) * (0.5 + 0.5 * np.sin(A * 2 - TAU * t * 2))
            cv.add(ring, c2)
            cv.over(fill(R - 0.35), np.array([0, 0, 0.0]))
        elif art == 'hyperframe':
            s2 = 0.3 + 0.1 * math.sin(TAU * t)
            cv.add(fill(np.abs(box(U, V, s2, s2)) - 0.03), c2)
            for sx in (-1, 1):
                for sy in (-1, 1):
                    cv.add(fill(seg(U, V, sx * 0.62, sy * 0.62, sx * s2, sy * s2, 0.025)), c2 * 0.8)
            cv.add(fill(np.abs(box(U, V, 0.62, 0.62)) - 0.03), c2 * 0.7)
        elif art == 'circuit':
            for i in range(-3, 4):
                trace = fill(np.abs(U - i * 0.25) - 0.02) * (np.sin(V * 3 + i) > 0)
                trace2 = fill(np.abs(V - i * 0.25) - 0.02) * (np.sin(U * 3 + i * 2) > 0.3)
                cv.add(np.clip(trace + trace2, 0, 1) * fill(box(U, V, 0.8, 0.8)) * (0.3 + 0.7 * max(0, math.sin(TAU * t + i))), c2)
        elif art == 'coilblock':
            for i in range(-3, 4):
                cv.over(fill(np.abs(V - i * 0.22) - 0.07) * fill(box(U, V, 0.8, 0.8)), np.array([0.75, 0.38, 0.18]))
                cv.add(fill(np.abs(V - i * 0.22) - 0.025) * fill(box(U, V, 0.8, 0.8)) * max(0, math.sin(TAU * (t * 2 - i / 7))), c2)
        elif art == 'emitterblock':
            cv.over(fill(R - 0.55), np.array([0.08, 0.1, 0.12]))
            for k in range(3):
                rr = ((t + k / 3) % 1) * 0.55
                cv.add(fill(np.abs(R - rr) - 0.03), c2)
            cv.add(fill(R - 0.12), np.array([1, 1, 1.0]))
        elif art == 'riftframe':
            tear = fill(np.abs(U - 0.1 * np.sin(V * 9 + TAU * t * 2)) - 0.05 * (1 - V * V)) * fill(np.abs(V) - 0.75)
            cv.over(fill(box(U, V, 0.55, 0.75)), np.array([0.03, 0.0, 0.06]))
            cv.add(tear, c2)
        elif art == 'cosmic':
            cv.over(fill(box(U, V, 0.7, 0.7)), np.array([0.01, 0.01, 0.03]))
            rng = np.random.default_rng(5)
            for i in range(14):
                px, py = rng.uniform(-0.65, 0.65, 2)
                tw = max(0, math.sin(TAU * t * 2 + i * 1.7))
                cv.add(fill(circle(U - px, V - py, 0.035)) * (0.3 + 0.7 * tw), mix(c2, np.array([0.6, 0.7, 1.0]), i % 2))
        elif art == 'magcasing':
            for k in range(3):
                rr = 0.25 + k * 0.2
                cv.over(fill(np.abs(R - rr) - 0.05), np.array([0.7, 0.4, 0.22]), dome=0.5)
                cv.add(fill(np.abs(R - rr) - 0.02) * max(0, math.sin(TAU * t - k)), c2)
        elif art == 'chrono':
            cv.over(fill(R - 0.62), np.array([0.1, 0.08, 0.04]), dome=0.6)
            for i in range(12):
                a = i * TAU / 12
                cv.over(fill(circle(U - math.cos(a) * 0.5, V - math.sin(a) * 0.5, 0.04)), c2 * 0.8, dome=0.3)
            a1 = -TAU * t
            cv.add(fill(seg(U, V, 0, 0, math.cos(a1) * 0.42, math.sin(a1) * 0.42, 0.04)), c2)
        elif art == 'wormframe':
            cv.over(fill(R - 0.7), np.array([0.02, 0.02, 0.05]), dome=0.4)
            for k in range(4):
                f = (k / 4 + t) % 1
                rr = 0.68 * (1 - f) ** 1.5 + 0.03
                cv.add(fill(np.abs(R - rr) - 0.02) * (1 - f), c2)
        elif art == 'reality':
            cv.over(fill(box(U, V, 0.75, 0.75)), np.array([0.01, 0.03, 0.02]), dome=0.2)
            rng = np.random.default_rng(9)
            for col in range(6):
                x = -0.6 + col * 0.24
                y = ((t * rng.choice([1, 2]) + rng.random()) % 1) * 1.5 - 0.75
                cv.add(fill(box(U - x, V - y, 0.05, 0.2)) * np.clip(1 - np.abs(V - y) * 3, 0, 1), c2)
            if (t * 6) % 1 < 0.2:
                cv.rgb = np.roll(cv.rgb, int(S * 0.06), axis=1)
        elif art == 'dipole':
            # C-shaped iron yoke (red/blue poles) around the beam gap; field lines pulse across the gap
            cv.over(fill(box(U, V, 0.72, 0.62)), c1 * 0.6 + 0.04)
            cv.over(fill(box(U, V - 0.36, 0.6, 0.16)), np.array([0.75, 0.18, 0.16]), dome=0.4)
            cv.over(fill(box(U, V + 0.36, 0.6, 0.16)), np.array([0.18, 0.3, 0.8]), dome=0.4)
            cv.over(fill(box(U, V, 0.6, 0.12)), np.array([0.02, 0.02, 0.04]))
            for i in range(5):
                x = -0.48 + i * 0.24
                cv.add(fill(np.abs(U - x) - 0.015) * fill(np.abs(V) - 0.18) * (0.3 + 0.7 * max(0, math.sin(TAU * t * 2 - i))), c2)
            cv.add(fill(np.abs(V) - 0.025) * fill(np.abs(U) - 0.6), np.array([1, 1, 1.0]) * (0.6 + 0.4 * p))
        elif art == 'quadrupole':
            # four hyperbolic pole tips around the beam; alternating N/S polarity
            cv.over(fill(R - 0.78), c1 * 0.7 + 0.03, dome=0.3)
            for k in range(4):
                a = k * math.pi / 2 + math.pi / 4
                px, py = math.cos(a) * 0.45, math.sin(a) * 0.45
                col = np.array([0.75, 0.2, 0.16]) if k % 2 == 0 else np.array([0.18, 0.32, 0.82])
                cv.over(fill(circle(U - px, V - py, 0.2)), col, dome=0.6)
                cv.add(fill(np.abs(circle(U - px, V - py, 0.12)) - 0.02) * max(0, math.sin(TAU * t + k * math.pi / 2)), c2)
            cv.over(fill(R - 0.14), np.array([0.02, 0.02, 0.04]))
            cv.add(fill(R - 0.05) * (0.6 + 0.4 * p), np.array([1, 1, 1.0]))
        elif art == 'rfcavity':
            # a chain of elliptical pill-box cells; a bright bunch rides the RF wave from cell to cell
            cv.over(fill(box(U, V, 0.8, 0.5)), c1 * 0.5 + 0.03)
            for i in range(4):
                x = -0.6 + i * 0.4
                cv.over(fill(np.sqrt(((U - x) / 0.18) ** 2 + (V / 0.42) ** 2) - 1), mix(c1 * 1.4, c2, 0.15), dome=0.7)
                field = max(0.0, math.cos(TAU * (t - i / 4))) ** 3
                cv.add(fill(np.sqrt(((U - x) / 0.1) ** 2 + (V / 0.28) ** 2) - 1) * field, c2)
            cv.add(fill(np.abs(V) - 0.02) * fill(np.abs(U) - 0.8) * 0.5, c2)
        elif art == 'beampipe':
            # round vacuum pipe cross-section with flanges; particle bunches stream through it
            cv.over(fill(R - 0.72), np.array([0.55, 0.58, 0.64]), dome=0.6)
            cv.over(fill(R - 0.52), np.array([0.03, 0.04, 0.07]))
            for k in range(8):
                a = k * TAU / 8
                cv.over(fill(circle(U - math.cos(a) * 0.62, V - math.sin(a) * 0.62, 0.04)), np.array([0.3, 0.32, 0.36]))
            for k in range(3):
                f = (t * 3 + k / 3) % 1
                cv.add(fill(circle(U, V, 0.08 + 0.35 * f)) * (1 - f) * 0.35, c2)
            cv.add(fill(R - 0.07), np.array([1, 1, 1.0]))
        elif art == 'cryo':
            # frosted coolant manifold: serpentine pipes with frost creeping along them
            cv.over(fill(box(U, V, 0.8, 0.8)), np.array([0.7, 0.8, 0.88]) * 0.35 + c1 * 0.3)
            for i in range(4):
                y = -0.54 + i * 0.36
                cv.over(fill(np.abs(V - y) - 0.08) * fill(np.abs(U) - 0.72), np.array([0.62, 0.7, 0.78]), dome=0.5)
                frost = 0.5 + 0.5 * np.sin(U * 9 + TAU * t + i * 1.7)
                cv.add(fill(np.abs(V - y) - 0.035) * fill(np.abs(U) - 0.72) * frost * 0.8, c2)
            for sx in (-1, 1):
                cv.over(fill(np.abs(U - sx * 0.72) - 0.07) * fill(np.abs(V) - 0.62), np.array([0.62, 0.7, 0.78]), dome=0.5)
        elif art == 'vacuum':
            # turbomolecular pump: a spinning bladed rotor behind a grille
            cv.over(fill(R - 0.72), np.array([0.2, 0.22, 0.26]), dome=0.4)
            for k in range(9):
                a = k * TAU / 9 + TAU * t
                cv.over(fill(seg(U, V, math.cos(a) * 0.15, math.sin(a) * 0.15, math.cos(a + 0.5) * 0.66, math.sin(a + 0.5) * 0.66, 0.05)),
                        np.array([0.62, 0.66, 0.72]), dome=0.3)
            cv.over(fill(R - 0.16), c1 * 1.5 + 0.1, dome=0.6)
            cv.add(fill(np.abs(R - 0.72) - 0.03) * (0.4 + 0.6 * p), c2)
        elif art == 'detector':
            # hexagonal scintillator array: hits light up cells, then fade (like an event display)
            cv.over(fill(box(U, V, 0.8, 0.8)), np.array([0.04, 0.05, 0.07]))
            rng = np.random.default_rng(seed + 21)
            for row in range(5):
                for col in range(5):
                    cx = -0.64 + col * 0.32 + (0.16 if row % 2 else 0)
                    cy = -0.64 + row * 0.32
                    if abs(cx) > 0.8:
                        continue
                    ph = rng.random()
                    hit = max(0.0, 1 - ((t * 2 + ph) % 1) * 4)
                    hexd = np.maximum(np.abs(U - cx) * 0.866 + np.abs(V - cy) * 0.5, np.abs(V - cy)) - 0.13
                    cv.over(fill(hexd), mix(c1 * 0.8, c2, 0.15 + 0.85 * hit), dome=0.3)
        elif art == 'injector':
            # converging nozzle feeding plasma into the core; plasma glows hottest at the throat
            cv.over(fill(box(U, V, 0.78, 0.78)), c1 * 0.55 + 0.03)
            cone = fill(np.abs(U) - (0.18 + 0.4 * (V + 0.8) / 1.6)) * fill(np.abs(V) - 0.7)
            cv.over(cone, np.array([0.5, 0.5, 0.55]), dome=0.5)
            inner = fill(np.abs(U) - (0.08 + 0.3 * (V + 0.8) / 1.6)) * fill(np.abs(V) - 0.66)
            flick = 0.7 + 0.3 * math.sin(TAU * t * 3)
            cv.add(inner * flick * np.clip(1.2 - (V + 0.8) / 1.6, 0, 1), c2)
            cv.add(fill(circle(U, V + 0.7, 0.12)) * flick, np.array([1, 1, 1.0]))
        elif art == 'lensarray':
            # a 3x3 array of gravitational lenses, each bending a point of light into an Einstein ring
            cv.over(fill(box(U, V, 0.8, 0.8)), np.array([0.02, 0.02, 0.05]))
            for i in range(3):
                for j in range(3):
                    cx, cy = -0.5 + i * 0.5, -0.5 + j * 0.5
                    rr = np.hypot(U - cx, V - cy)
                    ph = (i * 3 + j) / 9
                    cv.add(fill(np.abs(rr - 0.14) - 0.025) * (0.4 + 0.6 * max(0, math.sin(TAU * (t + ph)))), c2)
                    cv.over(fill(rr - 0.07), np.array([0, 0, 0.0]))
        elif art == 'stellarframe':
            cv.over(fill(box(U, V, 0.6, 0.6)), np.array([0.12, 0.08, 0.05]))
            cv.add(fill(R - 0.45) * (1 - R / 0.45) * (0.6 + 0.4 * p), c2)
            for i in range(4):
                a = i * math.pi / 2 + math.pi / 4
                cv.over(fill(seg(U, V, math.cos(a) * 0.3, math.sin(a) * 0.3, math.cos(a) * 0.85, math.sin(a) * 0.85, 0.06)), c1 * 2)
    elif art in ('plating', 'tiles', 'bricks', 'polished', 'lamp', 'hazard', 'vent', 'stars', 'data', 'planks', 'pillar_side', 'pillar_top'):
        # ---- decorative surfaces: clean bevelled panels with a little animated life
        lo, hi = c1 * 0.75, mix(c1, c2, 0.25) * 1.1
        cv.over(np.ones((S, S)), mix(lo, hi, n * 0.5 + 0.25))
        bevel = lambda d: (fill(d) - fill(d + 0.06))
        if art == 'plating':
            cv.over(fill(np.abs(box(U, V, 0.93, 0.93)) - 0.04), c1 * 1.45)
            cv.over(fill(np.abs(box(U, V, 0.93, 0.93)) - 0.04) * (V > 0) * (U > 0), c1 * 0.55)
            for sx in (-1, 1):
                for sy in (-1, 1):
                    cv.over(fill(circle(U - sx * 0.74, V - sy * 0.74, 0.06)), mix(c1, np.ones(3), 0.5))
            cv.add(np.exp(-((U - V - (t * 6 - 3)) ** 2) / 0.02) * 0.25, c2)
        elif art == 'tiles':
            g = (np.abs(U) < 0.04) | (np.abs(V) < 0.04) | (np.abs(np.abs(U) - 0.96) < 0.04) | (np.abs(np.abs(V) - 0.96) < 0.04)
            cv.over(g.astype(float), c1 * 0.5)
            for qx in (-0.5, 0.5):
                for qy in (-0.5, 0.5):
                    cv.add(fill(np.abs(box(U - qx, V - qy, 0.4, 0.4)) - 0.025) * (0.25 + 0.25 * math.sin(TAU * t + qx * 3 + qy * 5)), c2)
        elif art == 'bricks':
            row = np.floor((V + 1) * 2)
            off = (row % 2) * 0.5
            mortar = (np.abs(((V + 1) * 2) % 1 - 0.0) < 0.06) | (np.abs(((U + 1 + off) * 1.0) % 1) < 0.05)
            shade = np.sin(row * 3.1 + np.floor(U + 1 + off) * 5.7) * 0.12
            cv.over(np.ones((S, S)), mix(lo, hi, 0.5 + shade + n * 0.2))
            cv.over(mortar.astype(float), c1 * 0.35)
            cv.add(mortar * 0.25 * (0.5 + 0.5 * math.sin(TAU * t)), c2)
        elif art == 'polished':
            cv.over(np.ones((S, S)), mix(c1 * 1.1, mix(c1, c2, 0.4), np.clip(0.6 - V * 0.3 + n * 0.2, 0, 1)))
            cv.over(fill(np.abs(box(U, V, 0.93, 0.93)) - 0.03), mix(c1, c2, 0.5))
            cv.add(np.exp(-((U + V * 0.5 - (t * 6 - 3)) ** 2) / 0.05) * 0.35, np.ones(3))
        elif art == 'lamp':
            cv.over(fill(np.abs(box(U, V, 0.93, 0.93)) - 0.08), c1 * 1.3)
            core = fill(box(U, V, 0.78, 0.78))
            glowv = 0.75 + 0.25 * math.sin(TAU * t)
            cv.over(core, mix(c2, np.ones(3), 0.25 * glowv))
            cv.add(core * np.clip(1 - R * 0.8, 0, 1) * 0.5, np.ones(3))
            cv.over(fill(np.abs(U) - 0.03) * core + fill(np.abs(V) - 0.03) * core, mix(c2, c1, 0.5))
        elif art == 'hazard':
            stripes = ((U + V + t * 0.8) * 1.25) % 1 < 0.5
            cv.over(fill(box(U, V, 0.8, 0.8)), np.array([0.08, 0.08, 0.08]))
            cv.over((stripes * fill(box(U, V, 0.8, 0.8))).astype(float), c2 * 0.95)
            cv.over(fill(np.abs(box(U, V, 0.93, 0.93)) - 0.06), c1 * 1.2)
        elif art == 'vent':
            cv.over(fill(np.abs(box(U, V, 0.93, 0.93)) - 0.06), c1 * 1.3)
            for i in range(-3, 4):
                slat = fill(np.abs(V - i * 0.22) - 0.05) * fill(box(U, V, 0.78, 0.8))
                cv.over(slat, c1 * 0.35)
                cv.add(slat * (0.3 + 0.3 * math.sin(TAU * t * 2 - i)), c2 * 0.6)
        elif art == 'stars':
            cv.over(np.ones((S, S)), mix(c1 * 0.6, c1 * 1.2, n))
            rng = np.random.default_rng(seed + 77)
            for i in range(18):
                px, py = rng.uniform(-0.9, 0.9, 2)
                tw = 0.4 + 0.6 * max(0, math.sin(TAU * t * (1 + i % 3) + i))
                cv.add(fill(circle(U - px, V - py, 0.03 + 0.02 * (i % 3 == 0))) * tw, mix(c2, np.array([0.6, 0.7, 1.0]), i % 2))
            cv.over(fill(np.abs(box(U, V, 0.95, 0.95)) - 0.03), c1 * 1.6 + 0.05)
        elif art == 'data':
            cv.over(fill(np.abs(box(U, V, 0.93, 0.93)) - 0.05), c1 * 2.0)
            cell = np.floor((U + 1) * 4) + np.floor((V + 1) * 4) * 8
            on = (np.sin(cell * 12.9898 + (math.floor(t * 8) % 8) * 4.1) * 43758.5) % 1 > 0.6
            pad = (np.abs(((U + 1) * 4) % 1 - 0.5) < 0.32) & (np.abs(((V + 1) * 4) % 1 - 0.5) < 0.32) & (fill(box(U, V, 0.85, 0.85)) > 0)
            cv.over(pad.astype(float), c1 * 1.6)
            cv.add((pad & on).astype(float), c2)
        elif art == 'planks':
            row = np.floor((V + 1) * 2)
            grain = 0.5 + 0.5 * np.sin(U * 14 + row * 3 + noise(U * 0.5, V * 3, seed + 3, 3) * 4)
            cv.over(np.ones((S, S)), mix(c1 * 0.85, mix(c1, c2, 0.25), grain * 0.6 + n * 0.3))
            cv.over((np.abs(((V + 1) * 2) % 1) < 0.06).astype(float), c1 * 0.45)
            cv.add(np.exp(-(((V + 1) * 2 % 1 - 0.5) ** 2) / 0.01) * fill(np.abs(U - math.sin(TAU * t) * 0.6) - 0.08) * 0.6, c2)
        elif art == 'pillar_side':
            cv.over(np.ones((S, S)), mix(lo, hi, 0.5 + 0.35 * np.cos(U * 1.5) + n * 0.1))
            cv.over(fill(np.abs(np.abs(U) - 0.82) - 0.06), c1 * 1.5)
            cv.add(fill(np.abs(U) - 0.06) * (0.4 + 0.4 * np.sin(V * 6 - TAU * t * 2)), c2)
        else:  # pillar_top
            cv.over(fill(R - 0.88), mix(lo, hi, 0.6))
            cv.over(fill(np.abs(R - 0.7) - 0.06), c1 * 1.5)
            cv.add(fill(R - 0.25) * (0.6 + 0.4 * math.sin(TAU * t)), c2)
    elif art == 'fracture':
        # reality coming apart: a dark field of nested, offset squares (a fractal) whose edges glitch in and out
        cv.over(np.ones((S, S)), mix(c1 * 0.7, c1 * 1.2, n))
        rng = np.random.default_rng(seed + 5)
        for lvl, (size, count) in enumerate(((0.55, 2), (0.28, 4), (0.14, 8))):
            for i in range(count):
                px, py = rng.uniform(-0.7, 0.7, 2)
                ph = rng.uniform(0, 1)
                vis = 0.5 + 0.5 * math.sin(TAU * (t * (lvl + 1) + ph))      # fades in and out, whole cycles per loop
                dx = 0.06 * math.sin(TAU * (t * 2 + ph)) * (lvl + 1)
                edge = fill(np.abs(box(U - px - dx, V - py, size, size)) - 0.02)
                cv.add(edge * vis * (0.9 - lvl * 0.2), c2)
                cv.over(fill(box(U - px - dx, V - py, size * 0.92, size * 0.92)) * 0.25 * vis, mix(c1, c2, 0.3))
        # glitch bands: a few rows shifted with split colour
        band = np.floor((V + 1) * 8)
        on = ((band * 7 + math.floor(t * 8) * 3) % 5 == 0).astype(float)
        cv.add(on * 0.35 * (0.5 + 0.5 * np.sin(U * 9 + TAU * t)), c2)
        return cv.down(outline=False)
    elif art == 'cloak':
        # metamaterial cloaking glass: nearly invisible, with light bending around hexagonal cells
        cv.over(np.ones((S, S)) * 0.08, c1)
        hexes = fill(np.abs(np.abs(((U * 2.5 + 0.5) % 1) - 0.5) + np.abs(((V * 2.5 + U * 1.25 + 0.5) % 1) - 0.5) - 0.48) - 0.03)
        ripple = 0.5 + 0.5 * np.sin((U + V) * 6 - TAU * t * 2)
        cv.add(hexes * 0.35 * ripple, c2)
        rgba = cv.down(outline=False)
        rgba[..., 3] = np.clip(cv.a.reshape(N, SS, N, SS).mean(axis=(1, 3)) * 0.6, 0.06, 0.5)
        return rgba
    elif art in ('transfinite', 'omnilex'):
        cv.over(np.ones((S, S)), mix(c1 * 0.8, c1 * 1.4, n))
        if not FORMED_MODE[0]:
            cv.over(fill(np.abs(box(U, V, 0.93, 0.93)) - 0.05), c2 * 0.35 + 0.05)
        if art == 'transfinite':
            # Cantor dust: the middle third removed forever, glowing level by level
            for lvl in range(4):
                k = 3 ** lvl
                cu = np.floor((U + 1) / 2 * k * 3) % 3 != 1
                cvv = np.floor((V + 1) / 2 * k * 3) % 3 != 1
                lit = max(0, math.sin(TAU * t - lvl * 0.9))
                cv.add((cu & cvv) * fill(box(U, V, 0.84, 0.84)) * (0.08 + 0.22 * lit) / (1 + lvl * 0.3), c2)
        else:
            # a branching tree glyph above a wave line: the two things the Omnilex governs
            def br(x0, y0, ang, ln, d):
                if d == 0: return
                x1, y1 = x0 + math.cos(ang) * ln, y0 + math.sin(ang) * ln
                cv.add(fill(seg(U, V, x0, y0, x1, y1, 0.035 * (d / 4 + 0.4))) * (0.5 + 0.5 * math.sin(TAU * t - d)), c2)
                br(x1, y1, ang - 0.5, ln * 0.68, d - 1)
                br(x1, y1, ang + 0.5, ln * 0.68, d - 1)
            br(0, 0.45, -math.pi / 2, 0.35, 4)
            cv.add(fill(np.abs(V - 0.62 - 0.06 * np.sin(U * 9 + TAU * t * 2)) - 0.03) * fill(box(U, V, 0.84, 0.84)), np.array([0.6, 0.4, 1.0]))
    elif art == 'arbor':
        # impossible wood: grain flows both up and down at once and its colour drifts through phases
        hue = mix(c1, c2 * 0.5, 0.5 + 0.5 * math.sin(TAU * t + seed))
        grain = 0.5 + 0.5 * np.sin(U * 9 + noise(U, V * 0.3, seed + 5, 3) * 6)
        cv.over(np.ones((S, S)), mix(hue * 0.6, hue * 1.5, grain * 0.7 + n * 0.3))
        for i, (x0, sp) in enumerate(((-0.45, 1), (0.1, -1), (0.55, 1))):
            yy = ((V + 1) / 2 + sp * t + i * 0.3) % 1
            cv.add(fill(np.abs(U - x0 - 0.06 * np.sin(V * 7 + i)) - 0.03) * np.exp(-((yy - 0.5) ** 2) / 0.03), c2)
    elif art == 'bubble':
        cv.over(fill(R - 0.85), np.array([0.02, 0.02, 0.06]))
        arm = 0.5 + 0.5 * np.sin(A * 2 - np.log(R + 0.05) * 4 - TAU * t)
        cv.add(fill(R - 0.75) * (np.exp(-R * 5) * 1.4 + arm * (1 - R) * 0.6), c2)
        cv.add(fill(np.abs(R - 0.82) - 0.04) * (0.5 + 0.3 * np.sin(A * 3 + TAU * t)), np.array([0.7, 0.85, 1.0]))
        cv.add(fill(circle(U + 0.35, V + 0.35, 0.08)), np.array([1, 1, 1.0]))
        return cv.down(outline=False)
    elif art == 'fractal':
        # Sierpinski carpet in axiom white and gold, its holes shimmering
        cv.over(np.ones((S, S)), mix(c1 * 0.85, c1 * 1.1, n))
        gx, gy = ((U + 1) / 2 * 27).astype(int), ((V + 1) / 2 * 27).astype(int)
        hole = np.zeros_like(U, dtype=bool)
        x, y = gx.copy(), gy.copy()
        for _ in range(3):
            hole |= (x % 3 == 1) & (y % 3 == 1)
            x //= 3
            y //= 3
        cv.over(hole.astype(float), c2 * (0.55 + 0.25 * math.sin(TAU * t)))
        cv.over(fill(np.abs(box(U, V, 0.95, 0.95)) - 0.03), c2)
    elif art == 'koch':
        # a crystal pane with a Koch-snowflake ghost turning inside it
        cv.over(np.ones((S, S)) * 0.25, c1)
        ang = A + TAU * t / 6
        r_k = 0.55 + 0.12 * np.abs(np.cos(ang * 3)) + 0.05 * np.abs(np.cos(ang * 12)) + 0.02 * np.abs(np.cos(ang * 48))
        cv.add(fill(np.abs(R - r_k) - 0.04), c2)
        cv.add(fill(R - 0.12) * (0.6 + 0.4 * math.sin(TAU * t)), np.ones(3))
        rgba = cv.down(outline=False)
        rgba[..., 3] = np.clip(cv.a.reshape(N, SS, N, SS).mean(axis=(1, 3)), 0.3, 0.9)
        return rgba
    elif art == 'foam':
        # membranes of foam: Voronoi cell walls that breathe
        rng = np.random.default_rng(seed + 5)
        pts = rng.uniform(-1.3, 1.3, (9, 2))
        d = np.stack([np.hypot(U - px - 0.08 * math.sin(TAU * t + i), V - py - 0.08 * math.cos(TAU * t + i)) for i, (px, py) in enumerate(pts)])
        d.sort(axis=0)
        wall = fill(d[1] - d[0] - 0.08)
        cv.over(np.ones((S, S)), mix(c1 * 0.5, c1 * 0.8, n))
        cv.over(wall, mix(c1, c2, 0.6))
        cv.add(wall * np.exp(-(d[1] - d[0]) * 20) * 0.6, np.ones(3))
    elif art == 'foambubble':
        cv.over(fill(R - 0.95) * 0.15, c1)
        cv.add(fill(np.abs(R - 0.86) - 0.05) * (0.6 + 0.4 * np.sin(A * 2 + TAU * t)), c2)
        cv.add(fill(circle(U + 0.35, V + 0.35, 0.12)) * 0.9, np.ones(3))
        rgba = cv.down(outline=False)
        rgba[..., 3] = np.clip(cv.a.reshape(N, SS, N, SS).mean(axis=(1, 3)), 0.1, 0.8)
        return rgba
    elif art == 'transcendent':
        # white-gold marble with slowly flowing veins of light
        veins = np.abs(np.sin(U * 3 + noise(U * 1.5, V * 1.5, seed + 9, 3) * 5 + TAU * t * 0.5))
        cv.over(np.ones((S, S)), mix(c1, c1 * 0.88, n))
        cv.add(np.exp(-veins * 10) * 0.8, c2)
    elif art == 'omnium':
        # iridescent metal: the hue drifts across the surface
        hue = (U * 0.6 + V * 0.4 + n * 0.5 + t) % 1.0
        rgb = np.stack([0.6 + 0.4 * np.cos(TAU * (hue + k / 3)) for k in range(3)], -1)
        cv.over(np.ones((S, S)), rgb * 0.9)
        cv.over(fill(np.abs(box(U, V, 0.93, 0.93)) - 0.04), np.ones(3))
        cv.add(np.exp(-((U - V - (t * 6 - 3)) ** 2) / 0.03) * 0.5, np.ones(3))
    elif art == 'omnicasing':
        cv.over(np.ones((S, S)), mix(c1 * 0.9, c1 * 1.05, n))
        cv.over(fill(np.abs(box(U, V, 0.9, 0.9)) - 0.05), c2)
        cv.over(fill(np.abs(box(U, V, 0.5, 0.5)) - 0.03), c2 * 0.8)
        cv.add(fill(circle(U, V, 0.18)) * (0.6 + 0.4 * math.sin(TAU * t)), np.ones(3))
    elif art == 'rift':
        cv.over(np.ones((S, S)), np.array([0.02, 0.0, 0.05]))
        arm = 0.5 + 0.5 * np.sin(A * 3 - np.log(R + 0.05) * 5 - TAU * t * 2)
        cv.add(arm * np.clip(1.2 - R, 0, 1), c2)
        cv.add(fill(R - 0.15), np.ones(3))
    elif art == 'planck':
        cv.over(np.ones((S, S)), mix(c1 * 0.6, c1, n))
        cv.over(fill(np.abs(box(U, V, 0.93, 0.93)) - 0.06), c1 * 1.6)
        for i in range(6):
            a = TAU * (i / 6 + t / 3)
            cv.add(fill(circle(U - math.cos(a) * 0.45, V - math.sin(a) * 0.45, 0.09)) * (0.5 + 0.5 * math.sin(TAU * t * 3 + i)), c2)
        cv.add(fill(circle(U, V, 0.12)), np.ones(3) * 0.7)
    elif art == 'fieldglass':
        cv.over(np.ones((S, S)) * 0.18, c1)
        if not FORMED_MODE[0]:
            cv.over(fill(np.abs(box(U, V, 0.93, 0.93)) - 0.05), c1 * 0.7)
        hexes = fill(np.abs(np.abs(((U * 3 + 0.5) % 1) - 0.5) + np.abs(((V * 3 + U * 1.5 + 0.5) % 1) - 0.5) - 0.48) - 0.04)
        ripple = 0.5 + 0.5 * np.sin(R * 8 - TAU * t * 2)
        cv.add(hexes * (0.15 + 0.35 * ripple), c2)
        rgba = cv.down(outline=False)
        rgba[..., 3] = np.clip(cv.a.reshape(N, SS, N, SS).mean(axis=(1, 3)), 0.2, 0.85)
        return rgba
    return cv.down(outline=False, opaque=True)


def machine_faces(idx, color, t, multiblock=False):
    """Returns dict of face textures for a machine at time t."""
    c = hexc('#%06x' % color)
    dark = np.array([0.10, 0.11, 0.15])
    out = {}
    # side
    cv = Canvas()
    n = noise(U, V, idx, 6)
    cv.over(np.ones((S, S)), mix(dark * 0.8, dark * 1.3, n))
    cv.over(fill(np.abs(box(U, V, 0.93, 0.93)) - 0.05), dark * 2.2)
    cv.over(fill(box(U, V, 0.6, 0.12)), np.array([0.03, 0.03, 0.05]))
    cv.add(fill(box(U, V, 0.55, 0.06)) * (0.4 + 0.6 * shimmer(t)), c)
    for sx in (-1, 1):
        for sy in (-1, 1):
            cv.over(fill(circle(U - sx * 0.78, V - sy * 0.78, 0.07)), np.array([0.5, 0.52, 0.58]))
    out['side'] = cv.down(outline=False, opaque=True)
    # top
    cv = Canvas()
    cv.over(np.ones((S, S)), mix(dark * 0.8, dark * 1.3, n))
    cv.over(fill(np.abs(box(U, V, 0.93, 0.93)) - 0.05), dark * 2.2)
    cv.over(fill(R - 0.5), np.array([0.04, 0.04, 0.06]))
    cv.add(fill(np.abs(R - 0.42) - 0.04) * (0.5 + 0.5 * np.sin(A * 3 + TAU * t * 2)), c)
    out['top'] = cv.down(outline=False, opaque=True)
    # front (off / on)
    for state in ('off', 'on'):
        cv = Canvas()
        cv.over(np.ones((S, S)), mix(dark * 0.8, dark * 1.3, n))
        cv.over(fill(np.abs(box(U, V, 0.93, 0.93)) - 0.05), dark * 2.2 + (c * 0.3 if multiblock else 0))
        cv.over(fill(box(U, V - 0.05, 0.68, 0.62)), np.array([0.02, 0.02, 0.03]))
        if state == 'on':
            pattern = idx % 6
            sc = fill(box(U, V - 0.05, 0.62, 0.56))
            if pattern == 0:
                g = 0.5 + 0.5 * np.sin(U * 10 - TAU * t * 2) * np.sin(V * 10 + TAU * t)
            elif pattern == 1:
                g = np.exp(-((R - (t % 1) * 0.8) ** 2) / 0.01) + 0.3 * np.exp(-R * 3)
            elif pattern == 2:
                g = (np.abs(V - 0.05 - 0.3 * np.sin(U * 6 + TAU * t * 2)) < 0.07) * 1.0 + 0.15
            elif pattern == 3:
                g = 0.5 + 0.5 * np.sin(A * 4 + R * 12 - TAU * t * 2)
                g = g * np.exp(-R * 1.5)
            elif pattern == 4:
                g = ((np.floor((U + 1) * 4) + np.floor((V + 1) * 4) + int(t * 9)) % 3 == 0) * 0.9 + 0.1
            else:
                g = np.clip(1 - np.abs(U * 1.5) - np.abs(V - 0.05) * 1.5 + 0.3 * np.sin(TAU * t * 3), 0, 1)
            cv.add(sc * g, c)
            cv.add(sc * 0.12, c)
        else:
            cv.add(fill(box(U, V - 0.05, 0.62, 0.56)) * 0.06, c)
        cv.over(fill(box(U, V + 0.78, 0.4, 0.06)), np.array([0.3, 0.3, 0.35]))
        out['front_' + state] = cv.down(outline=False, opaque=True)
    return out


def fluid_frame(t, art, c1, c2, flow=False, seed=0):
    cv = Canvas()
    vv = V - (t * 2 if flow else 0)
    # the churn moves around a circle in noise-space, so frame N flows straight back into frame 0
    n1 = noise(U + 0.4 * math.cos(TAU * t), vv + 0.4 * math.sin(TAU * t), seed, 4)
    n2 = noise(U * 2 - 0.3 * math.sin(TAU * t), vv * 2 + 0.3 * math.cos(TAU * t), seed + 7, 6)
    base = mix(c1, c2, n1 * 0.6 + n2 * 0.4)
    if art in ('plasma', 'stellar'):
        base = mix(c1, c2, np.clip(n1 * n2 * 2.2, 0, 1))
    elif art == 'superfluid':
        base = mix(c1, c2, 0.3 + 0.4 * np.sin(U * 6 + n1 * 4 + TAU * t))
    elif art in ('darkgas',):
        base = mix(c1, c2, (n2 > 0.72) * 0.8 + n1 * 0.2)
    elif art == 'tachyongas':
        base = mix(c1 * 0.6, c2, ((V * 6 - U * 2 + t * 12) % 1 < 0.2) * 0.8 + n1 * 0.3)
    elif art in ('exotic', 'falsevacuum'):
        base = mix(c1, c2, 0.5 + 0.5 * np.sin(n1 * 8 - TAU * t))
    elif art == 'inflaton':
        base = mix(c1, c2, 0.5 + 0.5 * np.sin(R * 10 - TAU * t * 2))
    elif art == 'law':
        # a void that keeps bubbling bright new laws into existence; each bubble grows, then pops
        base = mix(c1 * 0.5, c1 * 1.6, n1)
        rng = np.random.default_rng(seed + 31)
        for i in range(7):
            px, py, ph = rng.uniform(-0.8, 0.8), rng.uniform(-0.8, 0.8), rng.random()
            life = (t + ph) % 1
            rr = 0.04 + 0.16 * life
            ring = np.exp(-((np.sqrt((U - px) ** 2 + (vv % 2 - 1 - py) ** 2) - rr) ** 2) / 0.0012) * (1 - life)
            base = base + ring[..., None] * mix(c2, np.array([1.0, 0.9, 0.5]), i % 2) * 1.2
        base = np.clip(base, 0, 1)
    cv.over(np.ones((S, S)), base)
    out = cv.down(outline=False, opaque=True)
    return out


def particle_frames(kind, count=8):
    frames = []
    for i in range(count):
        t = i / count
        cv = Canvas()
        if kind == 'tachyon':
            cv.add(fill(R - 0.5) * (1 - R / 0.5), np.array([1, 1, 0.8]))
            cv.add(fill(seg(U, V, -0.8, 0, 0.8, 0, 0.06)) * (0.6 + 0.4 * math.sin(TAU * t)), np.array([1, 0.95, 0.6]))
        elif kind == 'quantum_foam':
            s = 0.5 * math.sin(math.pi * t) + 0.1
            cv.add(fill(np.abs(R - s) - 0.08), np.array([0.7, 0.9, 1.0]))
        elif kind == 'hawking':
            cv.add(fill(R - 0.45) * (1 - R / 0.45), np.array([1, 1, 1.0]))
            for k in range(4):
                a = k * math.pi / 2 + t
                cv.add(fill(seg(U, V, 0, 0, math.cos(a) * 0.8, math.sin(a) * 0.8, 0.04)), np.array([0.9, 0.8, 1.0]))
        elif kind == 'infall':
            cv.add(fill(R - 0.6) * np.clip(1 - R / 0.6, 0, 1) ** 1.5, np.array([1, 0.85, 0.6]))
        elif kind == 'dark_mote':
            cv.add(fill(np.abs(R - 0.4 - 0.1 * math.sin(TAU * t)) - 0.12) * 0.8, np.array([0.5, 0.45, 1.0]))
        f = cv.down(outline=False)
        f[..., 3] = np.clip(cv.a.reshape(N, SS, N, SS).mean(axis=(1, 3)) * 1.5, 0, 1)
        f[..., :3] = np.clip(cv.rgb.reshape(N, SS, N, SS, 3).mean(axis=(1, 3)) * 1.5, 0, 1)
        frames.append(f)
    return frames


def cell_frame(t, c1, c2):
    cv = Canvas()
    glass = box(U, V + 0.05, 0.42, 0.68) - 0.12
    cv.over(fill(glass), np.array([0.12, 0.14, 0.2]))
    level = -0.55
    inner = box(U, V + 0.05, 0.32, 0.58) - 0.06
    wave = 0.04 * np.sin(U * 10 + TAU * t * 2)
    liquid = fill(inner) * (V > level + wave)
    cv.over(liquid, mix(c1, c2, 0.5 + 0.5 * np.sin(V * 8 - TAU * t)))
    cv.add(fill(seg(U, V, -0.22, -0.4, -0.22, 0.45, 0.04)) * 0.35, np.array([1, 1, 1.0]))
    cv.over(fill(box(U, V + 0.8, 0.3, 0.1)), np.array([0.55, 0.57, 0.62]))
    cv.over(fill(box(U, V - 0.78, 0.36, 0.08)), np.array([0.55, 0.57, 0.62]))
    return cv.down()


def port_frame(t, linked):
    cv = Canvas()
    dark = np.array([0.12, 0.13, 0.17])
    cv.over(np.ones((S, S)), dark)
    cv.over(fill(np.abs(box(U, V, 0.93, 0.93)) - 0.05), dark * 2.2)
    col = np.array([0.25, 1.0, 0.6]) if linked else np.array([1.0, 0.35, 0.3])
    for k in range(4):
        a = k * math.pi / 2
        x0, y0 = math.cos(a) * 0.6, math.sin(a) * 0.6
        cv.over(fill(seg(U, V, x0, y0, x0 * 0.4, y0 * 0.4, 0.07)), np.array([0.4, 0.42, 0.48]))
    cv.over(fill(R - 0.3), np.array([0.02, 0.02, 0.03]))
    cv.add(fill(R - 0.22) * ((0.5 + 0.5 * math.sin(TAU * t)) if linked else 0.35), col)
    return cv.down(outline=False, opaque=True)


def terminal_frames():
    frames = []
    for i in range(FRAMES):
        t = i / FRAMES
        cv = Canvas()
        cv.over(np.ones((S, S)), np.array([0.03, 0.05, 0.08]))
        cv.over(fill(np.abs(box(U, V, 0.93, 0.93)) - 0.05), np.array([0.2, 0.25, 0.32]))
        for k in range(6):
            a = k * TAU / 6 + TAU * t / 6
            cv.add(fill(seg(U, V, 0, 0, math.cos(a) * 0.55, math.sin(a) * 0.55, 0.02)) * 0.5, np.array([0.25, 0.9, 1.0]))
            cv.add(fill(circle(U - math.cos(a) * 0.55, V - math.sin(a) * 0.55, 0.09)), np.array([0.25, 0.9, 1.0]) * (0.5 + 0.5 * math.sin(TAU * t + k)))
        cv.add(fill(R - 0.15), np.array([1, 0.85, 0.4]))
        frames.append(cv.down(outline=False, opaque=True))
    return frames


def logo():
    size = 256
    img = np.zeros((size, size, 4))
    yy, xx = np.mgrid[0:size, 0:size]
    u, v = (xx / size) * 2 - 1, (yy / size) * 2 - 1
    r = np.sqrt(u * u + v * v)
    a = np.arctan2(v, u)
    disk = np.exp(-((v / (0.18 + 0.0 * u)) ** 2)) * np.clip(1 - np.abs(u), 0, 1) * (0.6 + 0.4 * np.sin(a * 6 + r * 20))
    glow = np.exp(-((r - 0.3) ** 2) / 0.004)
    col = np.zeros((size, size, 3))
    col += disk[..., None] * np.array([1.0, 0.6, 0.2])
    col += glow[..., None] * np.array([1.0, 0.85, 0.6])
    col *= (r > 0.27)[..., None]
    img[..., :3] = np.clip(col, 0, 1)
    img[..., 3] = np.clip(disk + glow + (r < 0.27) * 1.0, 0, 1) * (r < 0.98)
    pass  # the logo is now drawn by tools/compose.py (python3 compose.py logo)
    if False: Image.fromarray((img * 255).astype(np.uint8), 'RGBA').save(os.path.join(ROOT, '..', 'src/main/resources/theoreticalsciences_logo.png'))


def _render3d_job(job):
    """Renders one item's full animation loop (anim.py decides motion, overlays and loop length)."""
    iid, art, c1, c2, group_index = job
    import anim, items3d
    out = []
    if iid.startswith('@cell:'):
        # fluid cells: each fluid sloshes differently (rock / bob / tumble), gases leak vapour
        fid = iid[6:]
        k = anim.h(fid) % 3
        n = 40
        for f in range(n):
            t = f / n
            res = items3d.b_cell(c1, c2, t, fluid_cell=True)
            view = anim.apply_motions(res[0], res[1], res[2], t, (('rock',), ('bob',), ('tumble',))[k], anim.h(fid))
            img = items3d.render(res[0], res[1], **view)
            anim.ov_glint(img, t, anim.h(fid), start=(anim.h(fid) % 5) / 5)
            if art == 'gas':
                anim.ov_embers(img, t, anim.h(fid), items3d.H(c2), 5)
            out.append(img)
        return out
    if iid not in items3d.ITEMS:
        return None
    n = anim.frames_for(iid)
    for f in range(n):
        out.append(anim.render_frame(iid, art, c1, c2, f / n, group_index))
    return out


def extreme_block_textures():
    """Extreme Assembly Table (a glowing 9x9 grid on top) and Singularity Compactor (a swallowing vortex on top)."""
    setres(16)
    purple = hexc('#b060ff')
    dark = np.array([0.08, 0.07, 0.12])
    n = noise(U, V, 77, 6)
    for bid, col in (('extreme_assembly_table', 0xB060FF), ('singularity_compactor', 0x8040FF)):
        side = [machine_faces(200 + len(bid), col, f / FRAMES, True)['side'] for f in range(FRAMES)]
        save_strip(side, os.path.join(TEX, 'block', bid + '_side.png'), frametime=2)
        cv = Canvas()
        cv.over(np.ones((S, S)), mix(dark * 0.7, dark * 1.1, n))
        cv.over(fill(np.abs(box(U, V, 0.93, 0.93)) - 0.05), dark * 2.0)
        save_single(cv.down(outline=False, opaque=True), os.path.join(TEX, 'block', bid + '_bottom.png'))
    frames = []
    for f in range(FRAMES):
        t = f / FRAMES
        cv = Canvas()
        cv.over(np.ones((S, S)), mix(dark * 0.8, dark * 1.2, n))
        cv.over(fill(np.abs(box(U, V, 0.93, 0.93)) - 0.05), dark * 2.2)
        g = (U + 0.82) / 1.64 * 9
        h = (V + 0.82) / 1.64 * 9
        inside = fill(box(U, V, 0.82, 0.82))
        lines = np.maximum(fill(np.abs(g - np.round(g)) * 0.18 - 0.02), fill(np.abs(h - np.round(h)) * 0.18 - 0.02)) * inside
        cv.over(inside, np.array([0.03, 0.02, 0.06]))
        wave = 0.35 + 0.65 * np.clip(np.sin((np.floor(g) + np.floor(h)) * 0.7 - TAU * t), 0, 1)
        cv.add(lines * wave, purple)
        frames.append(cv.down(outline=False, opaque=True))
    save_strip(frames, os.path.join(TEX, 'block', 'extreme_assembly_table_top.png'), frametime=2)
    frames = []
    for f in range(FRAMES):
        t = f / FRAMES
        cv = Canvas()
        cv.over(np.ones((S, S)), mix(dark * 0.8, dark * 1.2, n))
        cv.over(fill(np.abs(box(U, V, 0.93, 0.93)) - 0.05), dark * 2.2)
        cv.over(fill(R - 0.72), np.array([0.01, 0.0, 0.02]))
        spiral = 0.5 + 0.5 * np.sin(A * 4 + np.log(R + 0.05) * 6 + TAU * t * 2)
        cv.add(fill(R - 0.7) * spiral * np.clip(R * 1.6, 0, 1), hexc('#a060ff'))
        cv.add(fill(np.abs(R - 0.72) - 0.04), hexc('#e0c0ff'))
        cv.over(fill(R - 0.12), np.array([0.0, 0.0, 0.0]))
        frames.append(cv.down(outline=False, opaque=True))
    save_strip(frames, os.path.join(TEX, 'block', 'singularity_compactor_top.png'), frametime=2)


def round3_block_textures():
    """Display pedestal, Pleroma Confluence and the Omnipotence Core (blocks registered in Java, not in content.json)."""
    setres(16)
    dark = np.array([0.07, 0.07, 0.10])
    n = noise(U, V, 91, 6)
    cv = Canvas()
    cv.over(np.ones((S, S)), mix(dark * 0.8, dark * 1.4, n))
    cv.over(fill(np.abs(box(U, V, 0.93, 0.93)) - 0.05), np.array([0.55, 0.5, 0.35]))
    cv.over(fill(np.abs(R - 0.55) - 0.04), np.array([0.8, 0.7, 0.4]))
    save_single(cv.down(outline=False, opaque=True), os.path.join(TEX, 'block', 'display_pedestal.png'))
    side = [machine_faces(311, 0xC080FF, f / FRAMES, True)['side'] for f in range(FRAMES)]
    save_strip(side, os.path.join(TEX, 'block', 'pleroma_confluence_side.png'), frametime=2)
    cv = Canvas()
    cv.over(np.ones((S, S)), mix(dark * 0.7, dark * 1.1, n))
    cv.over(fill(np.abs(box(U, V, 0.93, 0.93)) - 0.05), dark * 2.0)
    save_single(cv.down(outline=False, opaque=True), os.path.join(TEX, 'block', 'pleroma_confluence_bottom.png'))
    frames = []
    for f in range(FRAMES):
        t = f / FRAMES
        cv = Canvas()
        cv.over(np.ones((S, S)), mix(dark * 0.8, dark * 1.2, n))
        cv.over(fill(np.abs(box(U, V, 0.93, 0.93)) - 0.05), dark * 2.2)
        for k in range(6):          # six streams of colour spiralling into a white centre
            a = TAU * (k / 6 + t / 2)
            col = np.array([0.5 + 0.5 * math.cos(TAU * k / 6), 0.5 + 0.5 * math.cos(TAU * (k / 6 + 1 / 3)), 0.5 + 0.5 * math.cos(TAU * (k / 6 + 2 / 3))])
            arm = np.exp(-((A - a + np.log(R + 0.05) * 1.5 + math.pi) % TAU - math.pi) ** 2 / 0.05) * fill(R - 0.8)
            cv.add(arm * 0.8, col)
        cv.add(fill(R - 0.2), np.ones(3))
        frames.append(cv.down(outline=False, opaque=True))
    save_strip(frames, os.path.join(TEX, 'block', 'pleroma_confluence_top.png'), frametime=2)
    frames = [block_frame(f / FRAMES, 'omnicasing', hexc('#fffaf0'), hexc('#ffd060'), None, 5) for f in range(FRAMES)]
    for fr in frames:
        fr[..., :3] = np.clip(fr[..., :3] * 1.15, 0, 1)
    save_strip(frames, os.path.join(TEX, 'block', 'omnipotence_core.png'), frametime=2)


def main():
    machines = json.load(open(os.path.join(ROOT, 'machines.json')))
    # items: true-3D pixel-art (ray-marched at 4x, quantised to 16px hue-shifted ramps)
    from multiprocessing import Pool
    import anim
    groups = {}
    jobs = []
    for it in C['items']:
        cat = anim.CATEGORY.get(it['art'], 'device')
        jobs.append((it['id'], it['art'], it['c1'], it['c2'], groups.get(cat, 0)))
        groups[cat] = groups.get(cat, 0) + 1
    jobs += [('@cell:' + fl['id'], 'gas' if fl['gas'] else 'liquid', fl['c1'], fl['c2'], 0) for fl in C['fluids']]
    with Pool() as pool:
        results = pool.map(_render3d_job, jobs, chunksize=1)
    done3d = set()
    for (iid, *_), frames in zip(jobs, results):
        if frames is None:
            continue
        name = iid[6:] + '_cell' if iid.startswith('@cell:') else iid
        save_strip(frames, os.path.join(TEX, 'item', name + '.png'), frametime=1, interpolate=False)
        done3d.add(name)
    setres(32)
    for i, it in enumerate(C['items']):
        if it['id'] in done3d:
            continue
        fn = ITEM_ART.get(it['art'])
        if fn is None:
            raise SystemExit('missing art ' + it['art'])
        c1, c2 = hexc(it['c1']), hexc(it['c2'])
        frames = []
        for f in range(FRAMES):
            cv = Canvas()
            fn(cv, f / FRAMES, c1, c2, i)
            frames.append(cv.down())
        save_strip(frames, os.path.join(TEX, 'item', it['id'] + '.png'))
    # blocks (16x16)
    setres(16)
    for i, b in enumerate(C['blocks']):
        if not b.get('art'):
            continue                      # stairs / slabs / walls reuse their base block's texture
        c1, c2 = hexc(b['c1']), hexc(b['c2'])
        if b['kind'] == 'deco_pillar':
            for part in ('side', 'top'):
                frames = [block_frame(f / FRAMES, 'pillar_' + part, c1, c2, None, i) for f in range(FRAMES)]
                save_strip(frames, os.path.join(TEX, 'block', b['id'] + '_' + part + '.png'), frametime=3)
            continue
        frames = [block_frame(f / FRAMES, b['art'], c1, c2, b.get('base') if b['kind'] == 'ore' else None, i) for f in range(FRAMES)]
        save_strip(frames, os.path.join(TEX, 'block', b['id'] + '.png'), frametime=3)
        if b['kind'] in ('casing', 'glass'):
            frames = [formed_frame(f / FRAMES, b['art'], c1, c2, i, b['kind'] == 'glass') for f in range(FRAMES)]
            save_strip(frames, os.path.join(TEX, 'block', b['id'] + '_formed.png'), frametime=2)
    # the World Tree's heartwood shifts between four abnormal phases
    arb = next(b for b in C['blocks'] if b['id'] == 'arbor_heartwood')
    phase_cols = [('#2a1a40', '#ffb0ff'), ('#0a2a30', '#80ffe0'), ('#3a2a08', '#ffe080'), ('#d0d0e0', '#4020a0')]
    for ph, (a, b) in enumerate(phase_cols):
        frames = [block_frame(f / FRAMES, 'arbor', hexc(a), hexc(b), None, ph) for f in range(FRAMES)]
        save_strip(frames, os.path.join(TEX, 'block', f'arbor_heartwood_{ph}.png'), frametime=3)
    # machines
    for i, m in enumerate(machines):
        faces = [machine_faces(i, m['color'], f / FRAMES, m['multiblock']) for f in range(FRAMES)]
        for key in ('side', 'top', 'front_off', 'front_on'):
            fr = [fc[key] for fc in faces]
            if key == 'front_off':
                fr = [fr[0]]
            save_strip(fr, os.path.join(TEX, 'block', 'machine', m['id'] + '_' + key + '.png'), frametime=2)
    # port + terminal
    save_strip([port_frame(f / FRAMES, False) for f in range(FRAMES)], os.path.join(TEX, 'block', 'interface_port.png'))
    save_strip([port_frame(f / FRAMES, True) for f in range(FRAMES)], os.path.join(TEX, 'block', 'interface_port_linked.png'))
    save_strip(terminal_frames(), os.path.join(TEX, 'block', 'research_terminal_top.png'))
    side = machine_faces(99, 0x40E0FF, 0)['side']
    save_single(side, os.path.join(TEX, 'block', 'research_terminal_side.png'))
    extreme_block_textures()
    round3_block_textures()
    setres(16)
    # fluids + cells
    for i, fl in enumerate(C['fluids']):
        c1, c2 = hexc(fl['c1']), hexc(fl['c2'])
        save_strip([fluid_frame(f / 32, fl['art'], c1, c2, False, i) for f in range(32)], os.path.join(TEX, 'block', 'fluid', fl['id'] + '_still.png'), frametime=2)
        flow = [fluid_frame(f / 32, fl['art'], c1, c2, True, i) for f in range(32)]
        # flowing textures are 32x32 per frame in vanilla; upscale 2x
        flow = [np.repeat(np.repeat(fr, 2, axis=0), 2, axis=1) for fr in flow]
        save_strip(flow, os.path.join(TEX, 'block', 'fluid', fl['id'] + '_flow.png'), frametime=2)

    setres(32)
    for fl in C['fluids']:
        if fl['id'] + '_cell' in done3d:
            continue
        c1, c2 = hexc(fl['c1']), hexc(fl['c2'])
        save_strip([cell_frame(f / FRAMES, c1, c2) for f in range(FRAMES)], os.path.join(TEX, 'item', fl['id'] + '_cell.png'))
    setres(16)
    # particles
    for k in ('tachyon', 'quantum_foam', 'hawking', 'infall', 'dark_mote'):
        for j, fr in enumerate(particle_frames(k)):
            save_single(fr, os.path.join(TEX, 'particle', f'{k}_{j}.png'))
    import particles36           # round 36: the mod's own replacements for the vanilla particles
    particles36.write()
    # misc
    white = np.ones((16, 16, 4))
    save_single(white, os.path.join(TEX, 'misc', 'white.png'))
    logo()
    import empty_cell             # round 37: the empty cell is the same canister as the filled ones
    setres(32)
    save_strip(empty_cell.frames(), os.path.join(TEX, 'item', 'containment_cell.png'), frametime=1, interpolate=False)
    print('textures done')


if __name__ == '__main__':
    main()
