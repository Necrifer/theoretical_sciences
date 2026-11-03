"""Item art group 3 (round 14): the brand-new endgame materials of the Transcendental / Omnipotence chain.
Every item has its own 60-80 frame transformation story and lands back exactly where it started (t=0 == t=1)."""
import colorsys
import math
import numpy as np
from items3d import *
import anim

PI = math.pi


# =========================================================================================== timing helpers
def cl(x, a=0.0, b=1.0):
    return max(a, min(b, x))


def sm(x):
    x = cl(x)
    return x * x * (3 - 2 * x)


def win(t, a, b):
    return cl((t - a) / (b - a))


def bump(t, a, b):
    x = win(t, a, b)
    return 0.0 if x <= 0 or x >= 1 else math.sin(PI * x)


def back(x):
    x = cl(x)
    if x <= 0:
        return 0.0
    c1 = 1.70158
    return 1 + (c1 + 1) * (x - 1) ** 3 + c1 * (x - 1) ** 2


def pulse(t, c, w):
    """Gaussian flash centred on c (periodic in t)."""
    d = (t - c + 0.5) % 1.0 - 0.5
    return math.exp(-(d / w) ** 2)


def hsv(h, s=1.0, v=1.0):
    return np.array(colorsys.hsv_to_rgb(h % 1.0, s, v))


def lerp3(a, b, k):
    return np.asarray(a, float) * (1 - k) + np.asarray(b, float) * k


def _hs(a):
    v = np.sin(a * 12.9898 + 4.1) * 43758.5453
    return v - np.floor(v)


HUES = np.stack([hsv(h, 0.6, 1.0) for h in np.linspace(0, 1, 64, endpoint=False)])


def hue_map(h):
    return HUES[((np.asarray(h) % 1.0) * 64).astype(int) % 64]


# =========================================================================================== SDF helpers
def _ing(p, L=0.85, Hh=0.3, W=0.45):
    return np.maximum(sd_box(p, (L, Hh, W), 0.05), (p[..., 1] * 0.95 + np.abs(p[..., 2]) * 0.6 + np.abs(p[..., 0]) * 0.12 - (Hh + 0.12)))


def _void(p):
    return np.full(p.shape[:-1], 9.0)


def scaled(fn, s):
    if s < 0.015:
        return _void
    return lambda p: fn(p / s) * s


def sd_segs(p, A, B, r):
    """Distance to the capsules A[i]-B[i] (radius r). Only points near the bounding sphere get the exact (costly)
    evaluation; the rest get the (smaller, hence safe) distance to that sphere."""
    c = (np.vstack([A, B]).max(0) + np.vstack([A, B]).min(0)) / 2
    rb = np.sqrt(((np.vstack([A, B]) - c) ** 2).sum(-1)).max() + r
    flat = p.reshape(-1, 3)
    dc = np.sqrt(((flat - c) ** 2).sum(-1)) - rb
    out = dc.copy()
    near = dc < 0.12
    if near.any():
        q = flat[near]
        pa = q[:, None, :] - A
        ba = B - A
        h = np.clip((pa * ba).sum(-1) / np.maximum((ba * ba).sum(-1), 1e-9), 0, 1)
        d = np.sqrt(((pa - ba * h[..., None]) ** 2).sum(-1))
        out[near] = (d - r).min(-1)
    return out.reshape(p.shape[:-1])


def sd_poly(p, P, r):
    """Distance to a polyline P (M,3) of capsules with radius r."""
    if len(P) < 2:
        return _void(p)
    return sd_segs(p, P[:-1], P[1:], r)


def _planes(tt, tb, waist):
    """Planes (K,4) of the convex bipyramid tip-top / waist polygon / tip-bottom."""
    tt, tb = np.asarray(tt, float), np.asarray(tb, float)
    W = [np.asarray(w, float) for w in waist]
    cen = (tt + tb + sum(W)) / (len(W) + 2)
    pl = []
    for i in range(len(W)):
        a, b = W[i], W[(i + 1) % len(W)]
        for tri in ((tt, a, b), (tb, b, a)):
            n = np.cross(tri[1] - tri[0], tri[2] - tri[0])
            n /= np.linalg.norm(n)
            if n @ (tri[0] - cen) < 0:
                n = -n
            pl.append(np.append(n, n @ tri[0]))
    return np.array(pl)


def _poly(p, PL):
    return (p @ PL[:, :3].T - PL[:, 3]).max(-1)


def _flat(v):
    return lambda p, n: (np.zeros(p.shape[:-1]), np.full(p.shape[:-1], float(v)))


def _upright(p):
    """Swap y/z so that sd_torus draws a ring standing up, facing the viewer."""
    return p[..., [0, 2, 1]]


# =========================================================================================== 2D painting helpers
N = 32
YY, XX = np.mgrid[0:N, 0:N].astype(float)


def cam_of(view):
    return rotx(-view.get('view_pitch', 0.5)) @ roty(view.get('view_yaw', 0.7853))


def proj(pt, view):
    cam = cam_of(view)
    c = np.asarray(pt, float) @ cam.T
    k = view.get('zoom', 1.0) / view.get('extent', 1.0)
    return (c[0] * k + 1) / 2 * N - 0.5, (-c[1] * k + 1) / 2 * N - 0.5, c[2]


def paint(img, A, rgb, add=False):
    A = np.clip(np.asarray(A, float), 0, 1)
    if not A.any():
        return
    rgb = np.asarray(rgb, float)
    if rgb.ndim == 1:
        rgb = np.broadcast_to(rgb, img.shape[:2] + (3,))
    a0 = img[..., 3].copy()
    out_a = A + a0 * (1 - A)
    out_c = (rgb * A[..., None] + img[..., :3] * (a0 * (1 - A))[..., None]) / np.maximum(out_a, 1e-6)[..., None]
    if add:
        lit = np.clip(img[..., :3] + rgb * A[..., None], 0, 1)
        out_c = np.where((a0 > 0.5)[..., None], lit, out_c)
    img[..., :3] = np.where((out_a > 0)[..., None], out_c, img[..., :3])
    img[..., 3] = out_a


def paint_behind(img, A, rgb):
    """Paint only into (partly) empty pixels, under what is there."""
    A = np.clip(np.asarray(A, float), 0, 1) * (1 - img[..., 3])
    paint(img, A, rgb)


def glow(img, cx, cy, R, rgb, k, add=True, power=1.5):
    if k <= 0 or R <= 0:
        return
    r = np.hypot(XX - cx, YY - cy)
    paint(img, k * np.clip(1 - r / R, 0, 1) ** power, rgb, add)


def ring(img, cx, cy, r, rgb, k, w=0.9, sy=1.0, behind=False):
    if k <= 0:
        return
    d = np.hypot(XX - cx, (YY - cy) / sy)
    A = k * np.clip(1 - np.abs(d - r) / w, 0, 1)
    if behind:
        paint_behind(img, A, rgb)
    else:
        paint(img, A, rgb, True)


def star(img, cx, cy, L, rgb, k, diag=0.0):
    if k <= 0:
        return
    dx, dy = XX - cx, YY - cy
    A = np.clip(1 - np.abs(dx) / L, 0, 1) * (np.abs(dy) < 0.6) + np.clip(1 - np.abs(dy) / L, 0, 1) * (np.abs(dx) < 0.6)
    if diag:
        A = A + diag * np.clip(1 - np.abs(dx) / (L * 0.6), 0, 1) * (np.abs(np.abs(dx) - np.abs(dy)) < 0.6)
    paint(img, k * np.clip(A, 0, 1), rgb, True)


def dot(img, x, y, rgb, a=1.0, behind=False):
    xi, yi = int(round(x)), int(round(y))
    if 0 <= xi < N and 0 <= yi < N:
        if behind and img[yi, xi, 3] > 0.5:
            return
        anim._blend(img, yi, xi, np.asarray(rgb, float), cl(a))


def rays(img, cx, cy, r0, r1, count, rgb, k, phase=0.0, behind=True, sy=1.0, cols=None):
    if k <= 0:
        return
    for i in range(count):
        a = TAU * i / count + phase
        c = rgb if cols is None else cols[i % len(cols)]
        steps = int(max(2, (r1 - r0) * 1.5))
        for r in np.linspace(r0, r1, steps):
            dot(img, cx + math.cos(a) * r, cy + math.sin(a) * r * sy, c, k * (1 - (r - r0) / max(1e-6, r1 - r0) * 0.75), behind=behind)


def spark(img, x, y, rgb, s):
    if s <= 0:
        return
    arm = int(round(s * 3))
    for d in range(1, arm + 1):
        for ddx, ddy in ((d, 0), (-d, 0), (0, d), (0, -d)):
            dot(img, x + ddx, y + ddy, rgb, s * (1 - d / (arm + 1)))
    dot(img, x, y, WHITE, s)


def opaque(img):
    return img[..., 3] > 0.5


def edge(img):
    op = opaque(img)
    inner = op.copy()
    for dy, dx in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        inner &= np.roll(np.roll(op, dy, 0), dx, 1)
    return op & ~inner


def dilate(m):
    out = m.copy()
    for dy, dx in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        out |= np.roll(np.roll(m, dy, 0), dx, 1)
    return out


def outline_flash(img, rgb, k):
    if k > 0:
        paint(img, edge(img) * k, rgb)


def halo(img, rgb, k, thr=0.93):
    """1px light bleeding out of the brightest pixels into empty space."""
    if k <= 0:
        return
    lum = img[..., :3].min(-1) * opaque(img)
    h = dilate(lum > thr) & ~opaque(img)
    paint(img, h * k, rgb)


def fade(img, k):
    img[..., 3] *= cl(k)


def recolor(img, tint, k, lift=1.15):
    if k <= 0:
        return
    rgb = img[..., :3]
    lum = rgb.mean(-1)
    tint = np.asarray(tint, float)
    if tint.ndim == 1:
        tint = np.broadcast_to(tint, img.shape[:2] + (3,))
    out = np.clip(tint * (0.25 + lum[..., None] * lift) + np.maximum(lum - 0.78, 0)[..., None] * 1.6, 0, 1)
    m = (opaque(img) & (lum > 0.1))[..., None] * k
    img[..., :3] = rgb * (1 - m) + out * m


def run_edge(img, cx, cy, x, rgb, s):
    """A sparkle that runs along the outline (x in [0,1) = angle round the centre)."""
    ys, xs = np.nonzero(edge(img))
    if len(xs) and s > 0:
        order = np.argsort(np.arctan2(ys - cy, xs - cx))
        i = order[int((x % 1.0) * (len(order) - 1))]
        spark(img, xs[i], ys[i], rgb, s)


# =========================================================================================== SHARD OF OMNIPOTENCE
SH_MAIN = np.vstack([_planes((0.05, 0.95, -0.02), (-0.1, -0.86, 0.05),
                             [(0.31, 0.10, 0.02), (0.12, 0.16, 0.27), (-0.22, 0.04, 0.21), (-0.27, -0.04, -0.12), (0.06, 0.06, -0.28)]),
                     [[0.5246, 0.8186, -0.2338, 0.62]]])          # the fracture face that breaks the top tip off
SH_CHIP = _planes((0.02, 0.42, 0.0), (-0.03, -0.38, 0.02), [(0.15, 0.0, 0.02), (0.0, 0.03, 0.15), (-0.14, 0.0, 0.05), (0.02, -0.02, -0.15)])
CHIP_R = rotz(0.55)
SH_GHOSTS = [  # (window start, window end, shape, tint)
    (0.24, 0.37, lambda p: _ing(p / 0.62) * 0.62, '#ffb860'),
    (0.47, 0.60, lambda p: sd_sphere(p, 0.5), '#80e0ff'),
    (0.72, 0.86, lambda p: sd_torus(_upright(p), 0.48, 0.13), '#ff90e0'),
]


def _shard(p):
    a = _poly(p, SH_MAIN)
    b = _poly((p - np.array([0.3, -0.42, 0.08])) @ CHIP_R, SH_CHIP)
    return np.minimum(a, b)


def omnipotence_shard(c1, c2, t):
    """A white-gold splinter of the shattered Core with light leaking from its cracks. It keeps trying to remember what
    it was for: three times a loop it flickers into a translucent ghost of something else - an ingot, an orb, a ring -
    holds it for a moment, and snaps back to a splinter with a crack of light."""
    m, tint, snap = 0.0, None, 0.0
    for a, b, fn, col in SH_GHOSTS:
        if a <= t < b:
            m = sm(win(t, a, a + 0.06))
            tint, target = col, fn
        snap = max(snap, math.exp(-(((t - b) % 1.0) / 0.018)))
    gfn = target if tint else None

    def shape(p):
        s = _shard(p)
        if m <= 0:
            return s
        return s * (1 - m) + gfn(p) * m

    def cracks(p, n):
        c_a = np.abs(p[..., 1] - 0.4 * p[..., 0] - 0.1 * np.sin(p[..., 2] * 11) - 0.22) < 0.05
        c_b = np.abs(p[..., 1] + 0.55 * p[..., 0] + 0.28 + 0.08 * np.sin(p[..., 2] * 9 + 1)) < 0.05
        c_c = (np.abs(p[..., 0] * 0.9 + p[..., 2] * 0.4 - 0.05 + 0.06 * np.sin(p[..., 1] * 13)) < 0.03) & (p[..., 1] > 0.3)
        k = 0.75 + 0.25 * math.sin(TAU * t * 4) + snap
        return -0.06 * (c_a | c_b | c_c), (c_a | c_b | c_c).astype(float) * k * (1 - m)
    parts = [Part(scaled(shape, 1.15), 0, rot=roty(TAU * t) @ rotz(0.12))]
    mats = [Material(c1, '#ffe8a0', metal=0.7, gloss=1.6, emissive='#ffd040', pattern=cracks)]
    view = dict(view_pitch=0.25, view_yaw=0.6, zoom=1.0)

    def ov(img):
        cx, cy, _ = proj((0, 0, 0), view)
        if m > 0:                                      # the ghost of another form: translucent, tinted, scanlined
            recolor(img, lerp3(H(tint), WHITE, 0.35), 0.9 * m, lift=1.5)
            glow(img, cx, cy, 12, H(tint), 0.35 * m)
            outline_flash(img, H(tint), 0.6 * m)
            scan = ((YY + int(t * 64)) % 3 == 0)
            img[..., 3] *= 1 - m * (0.35 + 0.35 * scan)
        halo(img, H('#ffe9a8'), 0.55 * (1 - m) + 0.4 * snap)
        if snap > 0.05:                                # snapping back: a crack of light and splinters of light
            outline_flash(img, WHITE, snap)
            glow(img, cx, cy, 13, H('#fff0c0'), 0.5 * snap)
            star(img, cx, cy, 11, WHITE, snap, diag=0.5)
        if m <= 0 and snap < 0.3:                      # idle: light seeps from one crack end
            k = 0.5 + 0.5 * math.sin(TAU * t * 3)
            spark(img, cx + 3 + 2 * math.sin(TAU * t), cy - 7, H('#ffe9a8'), 0.6 * k)
    return parts, mats, view, ov


# =========================================================================================== ECHO OF THE TRANSCENDENTAL
EC_Y = np.array([-0.5, 0.5])
GOLD_ANG = PI * (3 - math.sqrt(5))


def transcendental_echo(c1, c2, t):
    """A hollow ringing sphere - dark glass bound by two glowing parallels and two pole caps - holding the faint image of the
    destroyed Transcendental disc (a golden-angle spiral with its halo). It is struck: the ribs flash and quiver, echo
    rings spread out and fade, and the image inside fades away like a memory, is gone for a moment, and comes back
    seed by seed from the centre."""
    strike = 0.28
    ts = (t - strike) % 1.0
    quiver = math.exp(-ts * 9) * math.sin(ts * TAU * 7) * (ts < 0.6)
    R = 0.7 * (1 + 0.05 * quiver)
    mem = 1 - sm(win(t, 0.33, 0.55)) + sm(win(t, 0.72, 0.97))
    grow = sm(win(t, 0.72, 0.97)) if t >= 0.6 else 1.0

    def ribs(p):
        rho = np.sqrt(p[..., 0] ** 2 + p[..., 2] ** 2)
        rk = np.sqrt(np.maximum(R * R - (EC_Y * R / 0.7) ** 2, 0.01))
        dq = np.sqrt((rho[..., None] - rk) ** 2 + (p[..., 1:2] - EC_Y * R / 0.7) ** 2) - 0.035
        cap = np.minimum(sd_sphere(p - np.array([0, R, 0]), 0.07), sd_sphere(p + np.array([0, R, 0]), 0.07))
        return np.minimum(dq.min(-1), cap)
    flash = math.exp(-(ts / 0.03) ** 2) if ts < 0.3 else 0.0

    def ribpat(p, n):
        b = np.sin(np.arctan2(p[..., 2], p[..., 0]) * 3 + TAU * t * 2 + p[..., 1] * 4) > 0.2
        return np.zeros(b.shape), np.maximum(b * 0.5 + 0.1, flash + 0.6 * abs(quiver))
    rot = roty(TAU * t)
    parts = [Part(ribs, 0, rot=rot)]
    mats = [Material('#b090c0', c2, metal=0.8, gloss=1.4, emissive=lerp3(H(c2), WHITE, 0.3), pattern=ribpat)]
    view = dict(view_pitch=0.22, view_yaw=0.5, zoom=1.0)

    def ov(img):
        cx, cy, _ = proj((0, 0, 0), view)
        rr = np.hypot(XX - cx, YY - cy)
        body = np.clip(11.8 - rr, 0, 1)
        empty = ~opaque(img)
        # dark glass body seen through the ribs (radial gradient, lighter rim)
        glass = lerp3(H(c1), H('#5a3070'), np.clip(rr / 11.5, 0, 1)[..., None] ** 2)
        paint_behind(img, body * 0.92, glass)
        ring(img, cx, cy, 11.0, H('#c8a0e0'), 0.45 + 0.4 * abs(quiver), w=0.7)
        # the remembered disc: golden-angle seeds + halo ring, drawn inside, under the ribs
        if mem > 0.02:
            spin = TAU * t
            nseed = int(30 * grow) if t >= 0.6 else 30
            for i in range(1, nseed + 1):
                r = 1.55 * math.sqrt(i) * (0.6 + 0.4 * grow)
                a = i * GOLD_ANG + spin
                x, y = cx + math.cos(a) * r, cy + math.sin(a) * r * 0.8
                out = r / 8.5                          # the outer seeds are the first to be forgotten
                k = cl(mem * 1.6 - out * 0.8) * (0.75 + 0.25 * math.sin(i * 1.7 + TAU * t * 2))
                xi, yi = int(round(x)), int(round(y))
                if 0 <= xi < N and 0 <= yi < N and empty[yi, xi]:
                    dot(img, x, y, lerp3(H('#ffd870'), H(c2), (i % 3) / 2), k)
            hl = np.clip(1 - np.abs(np.hypot(XX - cx, (YY - cy) / 0.8) - 9.6) / 0.8, 0, 1) * empty * cl(mem * 1.5 - 0.5)
            paint(img, hl * 0.8, H('#ffe0a0'), True)
            glow(img, cx, cy, 3.5, WHITE, 0.6 * mem)
        # echo rings ringing out after the strike
        for j in range(3):
            x = (ts - j * 0.07) / 0.45
            if 0 < x < 1:
                ring(img, cx, cy, 12 + x * 6, H(c2), (1 - x) * (0.9 - 0.25 * j), w=0.8, sy=0.85, behind=True)
        if flash > 0.05:
            outline_flash(img, WHITE, flash)
            glow(img, cx, cy, 15, H(c2), 0.4 * flash)
        if 0.6 < t < 0.72:                             # the empty sphere: one faint ghost of the image flickers
            k = bump(t, 0.6, 0.72) * (0.5 + 0.5 * math.sin(TAU * t * 30))
            ring(img, cx, cy, 6, H('#ffe0a0'), 0.35 * k, w=0.6, sy=0.8)
    return parts, mats, view, ov


# =========================================================================================== OMNIPOTENCE DUST
DU_GRAIN0 = [(math.cos(TAU * i / 8 + 0.3) * (0.32 + 0.2 * (i % 2)), -0.36 + 0.1 * (i % 3), math.sin(TAU * i / 8 + 0.3) * (0.32 + 0.2 * (i % 2)))
             for i in range(8)]
DU_CORE = np.array([0.0, 0.3, 0.0])
DU_TGT = [(0.3, 0, 0), (-0.3, 0, 0), (0, 0.3, 0), (0, -0.3, 0), (0, 0, 0.3), (0, 0, -0.3), (0.22, 0.0, 0.22), (-0.22, 0.0, -0.22)]


def omnipotence_dust(c1, c2, t):
    """A small heap of glittering white-gold dust. Eight grains lift out of it and swirl up in a spiral; they meet in the
    air and click together into a tiny Omnipotence Core (a glowing octahedron in a ring) which flashes and turns - and
    then crumbles; the grains rain back down into the heap with a puff."""
    lift = sm(win(t, 0.18, 0.44))
    form = back(win(t, 0.44, 0.54)) * (1 - sm(win(t, 0.72, 0.78)))
    fall = win(t, 0.78, 0.92)
    parts, mats = [], []
    heap_s = 1 - 0.12 * lift * (1 - fall) - 0.12 * (lift > 0.99) * (1 - fall) * 0
    if t >= 0.78:
        heap_s = 0.88 + 0.12 * min(1.0, fall ** 2 * 1.0)

    def heap(p):
        q = p / heap_s
        d = sd_sphere((q - np.array([0, -0.62, 0])) / np.array([1.0, 0.55, 1.0]), 0.78) * 0.55
        d = d + 0.025 * np.sin(q[..., 0] * 17) * np.sin(q[..., 2] * 15)
        return np.maximum(d, -(q[..., 1] + 0.62)) * heap_s

    def glit(p, n):
        cc = np.floor(p * 13)
        hh = _hs(cc[..., 0] * 7.1 + cc[..., 1] * 3.3 + cc[..., 2] * 5.7)
        tw = np.sin(TAU * (t * 3 + hh * 5))
        return 0.12 * np.sin(p[..., 0] * 30 + p[..., 2] * 25), ((hh > 0.82) & (tw > 0.3)).astype(float)
    parts.append(Part(heap, 0))
    mats.append(Material(c1, '#ffd870', metal=0.55, gloss=1.3, emissive=c2, pattern=glit))
    mats.append(Material('#ffffff', '#ffe080', metal=0.8, emissive='#fff6d0', pattern=pat_combine(pat_bands(9, 2, t, 0.25, axis=1), _flat(0.6 * form))))
    mats.append(Material('#ffd860', '#ffffff', metal=1.0, emissive='#ffe890', pattern=pat_bands(10, 3, t, 0.2, axis=0)))
    mats.append(Material('#fff4d0', '#ffffff', emissive='#fffbe8', pattern=pat_all()))
    cspin = TAU * 2 * t
    if form > 0.02:
        parts.append(Part(scaled(lambda p: sd_octa(p, 0.3), form), 1, pos=DU_CORE, rot=roty(cspin)))
        parts.append(Part(scaled(lambda p: sd_torus(p, 0.42, 0.035), form), 2, pos=DU_CORE, rot=rotx(0.5 + 0.3 * math.sin(TAU * t)) @ rotz(0.3) @ roty(-cspin)))
    gpos = []
    for i in range(8):
        g0 = np.array(DU_GRAIN0[i]) * np.array([1.0, 1.0, 1.0])
        tgt = DU_CORE + np.array(DU_TGT[i]) * 0.9
        if t < 0.44:
            k = lift
            a = TAU * 1.25 * k + i
            sw = math.sin(PI * k) * 0.35
            pos = lerp3(g0, tgt, k) + np.array([math.cos(a) * sw, 0.0, math.sin(a) * sw])
            gs = 1.0 if k > 0.01 else 0.0
        elif t < 0.78:
            pos, gs = tgt, 1 - sm(win(t, 0.44, 0.5)) + sm(win(t, 0.72, 0.77))
        else:
            pos = lerp3(tgt, g0, fall ** 2)
            gs = 1.0 - sm(win(t, 0.9, 0.94))
        gpos.append((pos, gs))
        if gs > 0.05:
            parts.append(Part(lambda p, r=0.065 * gs: sd_sphere(p, r), 3, pos=pos))
    view = dict(view_pitch=0.45, view_yaw=0.6, zoom=1.0)

    def ov(img):
        cx, cy, _ = proj(DU_CORE, view)
        hx, hy, _ = proj((0, -0.45, 0), view)
        gold = H('#ffe080')
        for pos, gs in gpos:                           # swirl trails behind the rising / falling grains
            if gs > 0.05 and (0.18 < t < 0.44 or 0.78 < t < 0.92):
                x, y, _ = proj(pos, view)
                dy = 1.2 if t < 0.5 else -1.5
                dot(img, x, y + dy, gold, 0.5, behind=True)
                dot(img, x, y + 2 * dy, gold, 0.25, behind=True)
        fl = math.exp(-((t - 0.47) / 0.025) ** 2)
        glow(img, cx, cy, 13, H('#fff0c0'), 0.7 * fl)
        star(img, cx, cy, 10, WHITE, fl, diag=0.5)
        if form > 0.5:
            rays(img, cx, cy, 7, 10, 8, gold, 0.5 * form, phase=TAU * t, behind=True)
        if 0.72 < t < 0.8:                             # it crumbles
            outline_flash(img, gold, bump(t, 0.72, 0.8))
        pf = win(t, 0.9, 1.0)                          # dust puff where the grains land
        if 0 < pf < 1:
            for i in range(6):
                a = PI + PI * i / 5
                r = 3 + 6 * pf
                dot(img, hx + math.cos(a) * r, hy + 2 + math.sin(a) * r * 0.35, H(c1), (1 - pf) * 0.8, behind=True)
        anim.ov_sparkles(img, (t * 2) % 1.0, 33, WHITE, 3)
    return parts, mats, view, ov


# =========================================================================================== APEIRON INGOT
AP_R = 0.4           # each copy is this much smaller than the one before
AP_S = 0.85          # size of the main ingot
AP_XI = AP_S * 0.85 * (1 + 2 * AP_R / (1 - AP_R)) * 1.04        # where the copies converge (the boundary that is never reached)


def apeiron_ingot(c1, c2, t):
    """The boundless ingot: a pale lilac-gold bar whose outline shimmers. Its ends unfold like a telescope into smaller
    and smaller copies of itself that converge on a vanishing point at each side; the copies stream outward and shrink
    into infinity (a seamless Droste flow - new ones keep sliding out of the ends), then the infinity folds back into one
    ingot."""
    g = sm(win(t, 0.22, 0.36)) * (1 - sm(win(t, 0.82, 0.95)))
    z = 2 * sm(win(t, 0.34, 0.84))
    parts, mats = [], []
    main = lambda p: _ing(p / AP_S) * AP_S
    mats.append(Material(c1, c2, metal=0.9, gloss=1.5, emissive=c2,
                         pattern=lambda p, n: (0.06 * np.sin(p[..., 0] * 9 - TAU * t * 3),
                                               ((np.abs(p[..., 0]) > 0.5) & (np.sin(p[..., 0] * 18 - TAU * t * 6) > 0.2)).astype(float) * g)))
    parts.append(Part(main, 0))
    for side in (1, -1):
        for k in range(1, 5):
            u = k - 1 + (z % 1.0)                  # u in [k-1, k): copy emerges at u=0 from inside the main ingot
            s = AP_R ** u
            if s * AP_S < 0.04:
                continue
            L = AP_S * 0.85
            x = side * L * (1 + s + 2 * AP_R * (1 - s / AP_R) / (1 - AP_R)) * 1.04 * g
            far = cl(u / 3)
            gold = (k + int(z)) % 2 == 0
            base = H('#ffcc50') if gold else H('#b0a0ff')
            mi = len(mats)
            mats.append(Material(lerp3(base, WHITE, 0.15 * far), '#ffffff', metal=0.9, gloss=1.4,
                                 emissive=lerp3(base, WHITE, 0.5), pattern=_flat(0.9 * cl(far * 1.5 - 0.3) * g)))
            parts.append(Part(scaled(main, s), mi, pos=(x, -0.02 * (1 - s), 0)))
    view = dict(view_pitch=0.58, view_yaw=0.3 + 0.1 * math.sin(TAU * t), zoom=1.0 - 0.38 * g)

    def ov(img):
        cx, cy, _ = proj((0, 0, 0), view)
        gold = H(c2)
        # the edges exist only where you look: a shimmer runs round the outline
        ex = edge(img)
        shim = (np.sin((XX * 0.7 - YY * 0.5) * 1.6 - TAU * t * 5) > 0.55) & ex
        paint(img, shim * (0.65 - 0.25 * g), lerp3(gold, WHITE, 0.4))
        for side in (1, -1):                        # the vanishing points at the ends of infinity
            x, y, _ = proj((side * AP_XI, 0, 0), view)
            if g > 0.2:
                spark(img, x + side, y, gold, g * (0.55 + 0.45 * math.sin(TAU * t * 6 + side)))
        unf = math.exp(-((t - 0.3) / 0.03) ** 2) + math.exp(-((t - 0.9) / 0.03) ** 2)
        outline_flash(img, lerp3(H(c1), WHITE, 0.5), 0.7 * unf)
        if g < 0.05:
            anim.ov_glint(img, (t * 1.5) % 1.0, 5, start=0.0)
    return parts, mats, view, ov


# =========================================================================================== PLEROMA CRYSTAL
PL_MOTES = [(i / 10, 0.1 + 0.12 * (i % 3), -0.55 + 0.11 * i, 1 + (i % 3) * 0.5) for i in range(10)]


def pleroma_crystal(c1, c2, t):
    """Fullness: a pale crystal with ten tiny glyph-motes of every colour circling inside it - every law of the
    Transcendental. Light pours into it from the bottom until it is full to the tip, trembles, and overflows in a
    flash: the motes are flung out and circle outside as a rainbow ring while the crystal drains dark; then they return
    inside and it begins to fill again."""
    if t < 0.56:
        lvl = -0.95 + 1.95 * sm(win(t, 0.04, 0.56))
    else:
        lvl = 1.0 - 1.95 * sm(win(t, 0.62, 0.72))
    full = bump(t, 0.5, 0.64)
    out = sm(win(t, 0.6, 0.66)) * (1 - sm(win(t, 0.76, 0.94)))
    shake = 0.02 * full * math.sin(TAU * t * 40)

    def fillp(p, n):
        y = p[..., 1]
        filled = y < lvl
        surf = np.abs(y - lvl - 0.03 * np.sin(p[..., 0] * 12 + TAU * t * 4)) < 0.06
        facets = np.sin(np.arctan2(p[..., 2], p[..., 0]) * 3) > 0.92
        return 0.12 * filled, np.maximum(filled * (0.75 + 0.25 * full), surf * 1.0) + facets * 0.4
    parts = [Part(lambda p: sd_gem(p, 0.4, 0.92, 0.32), 0, pos=(shake, 0, 0), rot=roty(TAU * t))]
    em = lerp3(H(c2), WHITE, 0.35 + 0.5 * full)
    mats = [Material(c1, c2, metal=0.6, gloss=1.7, emissive=em, pattern=fillp)]
    view = dict(view_pitch=0.25, view_yaw=0.5, zoom=1.0)

    def ov(img):
        cx, cy, _ = proj((0, 0, 0), view)
        ins = opaque(img)
        for i, (h0, rad, y, sp) in enumerate(PL_MOTES):
            a = TAU * (t * (2 if i % 2 else -2) + h0)
            r_in = rad * 16
            r_out = 13 + 1.5 * math.sin(i)
            r = r_in * (1 - out) + r_out * out
            x = cx + math.cos(a) * r
            yy = cy - y * 13 * (1 - out) + math.sin(a) * r * (0.35 + 0.15 * out) + (out * (i % 3 - 1) * 2)
            col = hsv(i / 10 + t, 0.85, 1.0)
            if out < 0.5 and lvl > y:                  # submerged in the light: paler
                col = lerp3(col, WHITE, 0.45)
            front = math.sin(a) > 0
            if out < 0.1 and not front:
                col = col * 0.7
            dot(img, x, yy, col, 1.0)
            # each mote is a tiny glyph: a second stroke pixel in a direction of its own
            ddx, ddy = ((1, 0), (0, 1), (1, 1), (-1, 1))[i % 4]
            dot(img, x + ddx, yy + ddy, col, 0.6)
        fl = pulse(t, 0.61, 0.025)
        if fl > 0.02:
            glow(img, cx, cy, 16, WHITE, fl)
            star(img, cx, cy - 2, 15, WHITE, fl, diag=0.6)
            outline_flash(img, WHITE, fl)
        if full > 0.3:                                 # brimming: light trembles on the rim
            outline_flash(img, lerp3(H(c2), WHITE, 0.5), 0.6 * full * (0.6 + 0.4 * math.sin(TAU * t * 24)))
        if 0.6 < t < 0.75:                             # overflow spilling down the sides
            x = win(t, 0.6, 0.75)
            for j in range(4):
                sx = cx + (-5, -2, 2, 5)[j]
                dot(img, sx, cy - 10 + x * 22 + j % 2 * 2, lerp3(H(c2), WHITE, 0.5), (1 - x), behind=True)
        if 0.7 < t < 0.9:
            ring(img, cx, cy, 13, H(c2), 0.25 * bump(t, 0.7, 0.9), w=0.7, sy=0.45, behind=True)
    return parts, mats, view, ov


# =========================================================================================== LOGOS THREAD
LG_M = 26
_s = np.linspace(0, 1, LG_M)
_ph = _s * 3 * TAU + 0.4
LG_CURSIVE = np.stack([-0.78 + 1.5 * _s - 0.2 * np.sin(_ph), 0.24 * np.cos(_ph) + 0.25 * (_s - 0.5) - 0.02,
                       0.08 * np.sin(_s * TAU)], -1)
_u = _s * TAU
LG_KNOT = np.stack([np.sin(_u) + 2 * np.sin(2 * _u), np.cos(_u) - 2 * np.cos(2 * _u), -np.sin(3 * _u)], -1) * 0.23


def logos_thread(c1, c2, t):
    """The Word that orders the world: a glowing golden thread written in a looping cursive hand. It pulls itself tight
    into a trefoil knot with a flash and turns, showing the knot in three dimensions; then it unties back into the
    script, is erased back into the pen-point, and writes itself out again with a bright nib."""
    tie = sm(win(t, 0.2, 0.34)) * (1 - sm(win(t, 0.6, 0.72)))
    if t < 0.72:
        f = 1.0
    elif t < 0.82:
        f = 1 - 0.7 * sm(win(t, 0.72, 0.82))
    else:
        f = 0.3 + 0.7 * sm(win(t, 0.82, 0.99))
    spin = TAU * sm(win(t, 0.34, 0.6))
    P = lerp3(LG_CURSIVE, LG_KNOT, tie)
    kf = f * (LG_M - 1)
    k0 = int(kf)
    pts = P[:k0 + 1]
    if k0 < LG_M - 1 and kf - k0 > 1e-3:
        pts = np.vstack([pts, P[k0] + (P[k0 + 1] - P[k0]) * (kf - k0)])
    head = pts[-1]
    R = roty(spin) @ rotx(0.25 * math.sin(spin))
    parts = [Part(lambda p: sd_poly(p, pts, 0.058), 0, rot=R)]
    pat = lambda p, n: (0.0 * p[..., 0], ((np.sin(p[..., 0] * 7 + p[..., 1] * 5 - TAU * t * 6) > 0.6) | (tie > 0.97)).astype(float) * (0.6 + 0.4 * tie))
    mats = [Material(c1, '#ffffff', metal=0.9, gloss=1.4, emissive='#fff4c0', pattern=pat)]
    view = dict(view_pitch=0.2, view_yaw=0.0, zoom=1.0)

    def ov(img):
        cx, cy, _ = proj((0, 0, 0), view)
        gold = H(c1)
        tied = pulse(t, 0.34, 0.025)
        if tied > 0.03:
            glow(img, cx, cy, 15, H('#fff0b0'), 0.3 * tied)
            outline_flash(img, WHITE, tied)
            for i in range(8):
                a = TAU * i / 8
                dot(img, cx + math.cos(a) * 13, cy + math.sin(a) * 13, gold, tied)
        if 0.36 < t < 0.6:                             # the knot radiates order
            k = bump(t, 0.36, 0.6)
            rays(img, cx, cy, 10, 13, 6, gold, 0.6 * k, phase=spin)
        if t >= 0.72:                                  # the nib: erasing / writing
            x, y, _ = proj(R @ head, view)
            nk = bump(t, 0.72, 1.0)
            spark(img, x, y, H('#fff8e0'), 0.9 * nk)
            glow(img, x, y, 4, WHITE, 0.5 * nk)
        elif tie < 0.02:                               # idle: a glint reads along the line
            i = int(win(t, 0.0, 0.2) * (LG_M - 1)) if t < 0.2 else 0
            x, y, _ = proj(R @ P[i], view)
            spark(img, x, y, WHITE, bump(t, 0.0, 0.2))
        halo(img, gold, 0.4)
    return parts, mats, view, ov


# =========================================================================================== REKINDLED SPARK
def rekindled_spark(c1, c2, t):
    """The first light of a second creation: a tiny twinkling spark swells white-hot and bangs - an open shell of colour
    expands, cooling from white through gold and rose to violet, with newborn galaxies glinting on its inside - then
    it all falls back together into the spark."""
    if t < 0.36:
        R = 0.0
    elif t < 0.72:
        R = 0.2 + 0.68 * (1 - (1 - win(t, 0.36, 0.72)) ** 2.2)
    else:
        R = 0.88 * (1 - sm(win(t, 0.72, 0.9)))
    cool = win(t, 0.36, 0.72) if t < 0.72 else 1 - sm(win(t, 0.72, 0.9))
    swell = bump(t, 0.26, 0.4)
    rs = 0.16 + 0.03 * math.sin(TAU * t * 6) + 0.2 * swell
    if 0.4 < t < 0.86:
        rs *= 0.45
    view = dict(view_pitch=0.35, view_yaw=0.5, zoom=1.0)
    tc = -cam_of(view)[2]                              # world direction toward the viewer
    hue = (0.14 - 0.45 * cool) % 1.0                   # white-gold -> rose -> violet
    shell_col = lerp3(hsv(hue, 0.75, 1.0), WHITE, max(0.0, 0.6 - cool * 1.4))
    parts = [Part(lambda p: sd_sphere(p, rs), 0)]
    mats = [Material(c1, '#ffffff', emissive=lerp3(H(c1), WHITE, 0.4 + 0.6 * swell), pattern=pat_all()),
            Material(shell_col * 0.6, shell_col, emissive=shell_col,
                     pattern=lambda p, n: (np.zeros(p.shape[:-1]), np.maximum(0.55 + 0.45 * (np.sin(p[..., 0] * 14 + p[..., 1] * 11 + TAU * t * 3) > 0.3) - cool * 0.3,
                                                                          (_hs(np.floor(p[..., 0] * 9) * 3.7 + np.floor(p[..., 1] * 9) * 1.3 + np.floor(p[..., 2] * 9) * 7.9) > 0.86) * 1.0)))]
    if R > 0.05:
        th = 0.05 + 0.05 * (1 - cool)
        parts.append(Part(lambda p: np.maximum(np.abs(sd_sphere(p, R)) - th, np.abs(p @ tc) - 0.42 * R), 1))

    def ov(img):
        cx, cy, _ = proj((0, 0, 0), view)
        bang = pulse(t, 0.37, 0.03)
        glow(img, cx, cy, 16, WHITE, bang)
        star(img, cx, cy, 15, WHITE, bang, diag=0.7)
        if R > 0.05:                                   # the edge of the expanding universe
            rp = R * 16
            ring(img, cx, cy, rp + 1.0, shell_col, 0.6 * (1 - cool * 0.5), w=0.8, behind=True)
            for i in range(5):                         # newborn galaxies inside
                a = TAU * (i * 0.29 + 0.1) + t
                r = rp * (0.25 + 0.12 * i)
                dot(img, cx + math.cos(a) * r, cy + math.sin(a) * r * 0.8, lerp3(shell_col, WHITE, 0.6), cool * 0.9)
        if 0.72 < t < 0.9:                             # condensing: light falls back in
            anim.ov_infall(img, win(t, 0.72, 0.9), 12, lerp3(shell_col, WHITE, 0.5), 10)
        cond = pulse(t, 0.9, 0.02)
        glow(img, cx, cy, 9, H(c1), cond)
        if t < 0.3 or t > 0.9:                          # the idle spark twinkles
            tw = 0.6 + 0.4 * math.sin(TAU * t * 6)
            star(img, cx, cy, 6 + 3 * tw, H('#fff4c0'), 0.8 * tw, diag=0.4)
            glow(img, cx, cy, 6, H(c1), 0.5)
            for i in range(3):
                life = (t * 4 + i / 3) % 1.0
                a = TAU * (i / 3 + 0.2)
                dot(img, cx + math.cos(a) * (4 + 5 * life), cy + math.sin(a) * (4 + 5 * life), H(c1), math.sin(PI * life) * 0.8)
    return parts, mats, view, ov


# =========================================================================================== OMNIPOTENT INGOT
OP_METALS = [H(c) for c in ('#d8d8ff', '#ffd060', '#60ff9a', '#ff6080', '#60b0ff', '#c080ff', '#ff9040', '#40ffe0')]
OP_PROP = [(0.85, 0.3, 0.45), (0.75, 0.34, 0.45), (0.9, 0.26, 0.42), (0.8, 0.3, 0.5), (0.85, 0.33, 0.4), (0.78, 0.28, 0.48),
           (0.88, 0.3, 0.44), (0.82, 0.32, 0.46)]


def _starshape(p):
    d = sd_octa(p / np.array([1.0, 0.32, 0.32]), 0.85) * 0.32
    d = np.minimum(d, sd_octa(p / np.array([0.32, 1.0, 0.32]), 0.85) * 0.32)
    d = np.minimum(d, sd_octa(p / np.array([0.32, 0.32, 1.0]), 0.85) * 0.32)
    q = p @ rotz(PI / 4).T
    d = np.minimum(d, sd_octa(q / np.array([0.62, 0.28, 0.28]), 0.85) * 0.28)
    d = np.minimum(d, sd_octa(q / np.array([0.28, 0.62, 0.28]), 0.85) * 0.28)
    return np.minimum(d, sd_sphere(p, 0.28))


def omnipotent_ingot(c1, c2, t):
    """The final ingot. A white-gold bar crowned by turning rays of light. It flickers through every metal of the mod in
    turn (each a different ingot), rises and melts into a golden sphere which throws out a halo of eight tiny ingots of
    every colour; they dive back in and the sphere bursts into a spinning star; the star collapses to a white point,
    goes supernova with a double rainbow shockwave - and the ingot is reborn from the light, overshooting, sheened in
    rainbow, its crown blazing, and settles back to white-gold."""
    # ---- phases
    flick = 0.15 <= t < 0.29
    a_sph = sm(win(t, 0.29, 0.37))                    # ingot -> sphere
    a_star = sm(win(t, 0.49, 0.56))                   # sphere -> star
    col = sm(win(t, 0.57, 0.645))                     # star -> white point
    reborn = t >= 0.66
    if reborn:
        a_sph = a_star = 0.0
    sg = back(win(t, 0.66, 0.78)) if reborn else 1.0
    halo_e = sm(win(t, 0.33, 0.40)) * (1 - sm(win(t, 0.46, 0.53)))
    lift = 0.12 * a_sph * (1 - col) if not reborn else 0.0
    k_met = int(win(t, 0.15, 0.29) * 8) % 8 if flick else 0
    L, Hh, W = OP_PROP[k_met] if flick else OP_PROP[0]
    s_main = (1 - col) if not reborn else sg
    s_main = max(s_main, 0.0)

    def body(p):
        d = _ing(p, L, Hh, W)
        if a_sph > 0 and not reborn:
            d = d * (1 - a_sph) + sd_sphere(p, 0.5 + 0.03 * math.sin(TAU * t * 8) * (1 - a_star)) * a_sph
            if a_star > 0:
                d = d * (1 - a_star) + _starshape(p) * a_star
        return d
    if reborn:
        spin = 0.0
    else:
        spin = TAU * 1.0 * sm(win(t, 0.29, 0.645)) + TAU * 2 * a_star * (1 - col) * 0
    rot = roty(spin) @ rotz(0.6 * a_star * math.sin(TAU * win(t, 0.49, 0.645)))
    gold_em = '#ffe9a0'

    def filig(p, n):
        v = np.sin(p[..., 0] * 11 + np.sin(p[..., 2] * 9) * 1.5 - TAU * t * 4)
        line = np.abs(v) < 0.16
        return 0.05 * v, line * (0.7 + 0.3 * math.sin(TAU * t * 2)) + a_sph * 0.35 * (np.sin(p[..., 1] * 14 - TAU * t * 6) > 0) + 0.9 * a_star
    parts = [Part(scaled(body, s_main), 0, pos=(0, lift, 0), rot=rot)]
    mats = [Material(lerp3(H('#fffaf0'), H('#ffd050'), 0.8 * a_sph * (1 - a_star)), c2, metal=0.95, gloss=1.8, emissive=lerp3(H(gold_em), WHITE, a_star), pattern=filig)]
    # the halo of every ingot
    hpos = []
    if halo_e > 0.02:
        orb = TAU * 1.5 * win(t, 0.33, 0.53)
        Rr = 0.95 * halo_e
        for k in range(8):
            a = TAU * k / 8 + orb
            pt = rotx(-0.55) @ np.array([math.cos(a) * Rr, 0.0, math.sin(a) * Rr]) + np.array([0, lift, 0])
            hpos.append((pt, k))
            mats.append(Material(OP_METALS[k], '#ffffff', metal=0.9, gloss=1.4, emissive=OP_METALS[k], pattern=_flat(0.5)))
            parts.append(Part(scaled(_ing, 0.3 * cl(halo_e * 1.4)), len(mats) - 1, pos=pt, rot=roty(-a + orb * 2)))
    # the white point
    if 0.6 < t < 0.665:
        mats.append(Material('#ffffff', '#ffffff', emissive='#ffffff', pattern=pat_all()))
        parts.append(Part(lambda p: sd_sphere(p, 0.07 + 0.05 * col), len(mats) - 1, pos=(0, lift, 0)))
    view = dict(view_pitch=0.6 - 0.3 * a_sph * (1 - col) if not reborn else 0.6, view_yaw=0.65, zoom=1.05 - 0.1 * halo_e)

    def ov(img):
        cx, cy, _ = proj((0, lift, 0), view)
        gold = H(c2)
        # ---- the crown of light (behind the item)
        if reborn:
            crown = 1.0 + 0.8 * (1 - win(t, 0.66, 0.92))
        else:
            crown = (1.0 - a_sph * 0.5) * (1 - col)
        if crown > 0.02:
            r = np.hypot(XX - cx, (YY - cy - 1) / 0.8)
            paint_behind(img, 0.45 * min(1.0, crown) * np.clip(1 - r / 15, 0, 1) ** 1.3, lerp3(gold, WHITE, 0.25))
            ln = 3 + 2.5 * math.sin(TAU * t * 3) ** 2 + (7 * (1 - win(t, 0.66, 0.92)) if reborn else 0)
            rays(img, cx, cy + 1, 9.5, 9.5 + ln, 16, lerp3(gold, WHITE, 0.5), 0.95 * min(1.0, crown), phase=TAU * t, sy=0.8)
        # ---- every metal: the strobe tint
        if flick:
            recolor(img, OP_METALS[k_met], 0.8)
            outline_flash(img, lerp3(OP_METALS[k_met], WHITE, 0.3), 0.8)
        # ---- trails of the ingot halo
        if halo_e > 0.3:
            orb = TAU * 1.5 * win(t, 0.33, 0.53)
            for k in range(8):
                for j in range(1, 4):
                    a = TAU * k / 8 + orb - 0.2 * j
                    pt = rotx(-0.55) @ np.array([math.cos(a), 0.0, math.sin(a)]) * 0.95 * halo_e + np.array([0, lift, 0])
                    x, y, z = proj(pt, view)
                    dot(img, x, y, OP_METALS[k], (halo_e - 0.3) * (0.9 - 0.25 * j), behind=z > 0)
        if 0.46 < t < 0.53:                            # they dive in
            outline_flash(img, hsv(t * 8, 0.5, 1.0), bump(t, 0.46, 0.53))
        if 0.5 < t < 0.6:                              # the star throws prismatic rays
            k = bump(t, 0.5, 0.6)
            rays(img, cx, cy, 10, 15, 8, None, k, phase=-TAU * t * 2, behind=True, cols=[hsv(i / 8 + t, 0.6, 1.0) for i in range(8)])
        if 0.56 < t < 0.665:                           # collapse: prismatic infall to the white point
            anim.ov_infall(img, win(t, 0.56, 0.665), 3, hsv(t * 4, 0.4, 1.0), 12)
            glow(img, cx, cy, 3 + 4 * col, WHITE, 0.5 + 0.5 * col)
        # ---- supernova and rebirth
        fl = math.exp(-((t - 0.665) / 0.025) ** 2)
        glow(img, cx, cy, 20, WHITE, fl)
        star(img, cx, cy, 16, WHITE, fl, diag=0.8)
        for j, (a0, a1) in enumerate(((0.66, 0.8), (0.69, 0.86))):
            if a0 < t < a1:
                x = win(t, a0, a1)
                for q in range(16):
                    a = TAU * q / 16 + j * 0.2
                    rr = 3 + x * 14
                    dot(img, cx + math.cos(a) * rr, cy + math.sin(a) * rr * (0.75 if j == 0 else 0.55), hsv(q / 16 + t + j * 0.5, 0.65, 1.0), (1 - x))
        if reborn and t < 0.95:                        # rainbow sheen sweeping the newborn ingot, then fading to white-gold
            w_r = sm(win(t, 0.67, 0.7)) * (1 - sm(win(t, 0.84, 0.95)))
            rh = (XX + YY) / 30.0 - t * 4
            recolor(img, hue_map(rh), 0.8 * w_r)
            outline_flash(img, hsv(t * 6, 0.6, 1.0), 0.9 * w_r)
        if reborn and 0.74 < t < 1.0:
            x = win(t, 0.74, 1.0)
            for i in range(4):
                life = (x * 2 + i / 4) % 1.0
                a = TAU * (i * 0.37 + 0.1)
                spark(img, cx + math.cos(a) * 11, cy + math.sin(a) * 5, hsv(i / 4 + t * 2, 0.4, 1.0), math.sin(PI * life) * (1 - x) * 0.9)
        if t < 0.15:                                   # idle: a prismatic sparkle runs the outline
            run_edge(img, cx, cy, win(t, 0.0, 0.15), hsv(t * 6, 0.45, 1.0), bump(t, 0.0, 0.15))
            outline_flash(img, lerp3(gold, WHITE, 0.6), 0.3 * bump(t, 0.0, 0.15))
    return parts, mats, view, ov


# =========================================================================================== CRYSTAL OF TRANSCENDENCE
TC_V4 = np.array([[s * (i == j) for j in range(4)] for i in range(4) for s in (1, -1)], float)      # the 16-cell
TC_E = np.array([(a, b) for a in range(8) for b in range(a + 1, 8) if b != (a ^ 1)])
TC_D = 2.6
TC_K = 0.74 * TC_D
TC_SY = 1.3


def _tc_proj(theta):
    c, s = math.cos(theta), math.sin(theta)
    R = np.eye(4)
    R[0, 0], R[0, 3], R[3, 0], R[3, 3] = c, -s, s, c
    v = TC_V4 @ R.T
    P = v[:, :3] * (TC_K / (TC_D - v[:, 3]))[:, None]
    P[:, 1] *= TC_SY
    return P, v[:, 3]


def transcendence_crystal(c1, c2, t):
    """A crystal that steps outside every system that could describe it. It turns as an ordinary lilac octahedral
    crystal - then its faces thin away to a skeleton of light and it rotates through the fourth dimension: the tips run
    in through the centre point and out the other side while the hidden inner vertices come out to be tips, turning
    the crystal inside out (twice); it solidifies again with a flash."""
    wire = sm(win(t, 0.24, 0.36)) * (1 - sm(win(t, 0.78, 0.9)))
    th = PI * sm(win(t, 0.34, 0.8))
    P, w4 = _tc_proj(th)
    A, B = P[TC_E[:, 0]], P[TC_E[:, 1]]

    def shape(p):
        solid = sd_octa(p / np.array([1.0, TC_SY, 1.0]), 0.74) * 0.95
        if wire <= 0:
            return solid
        sk = np.minimum(sd_segs(p, A, B, 0.035), (np.sqrt(((p[..., None, :] - P) ** 2).sum(-1)) - 0.07).min(-1))
        return solid * (1 - wire) + sk * wire

    def facets(p, n):
        q = np.abs(p / np.array([1.0, TC_SY, 1.0]))
        e = (np.abs(q[..., 0] - q[..., 2]) < 0.04) | (q[..., 1] < 0.04 * TC_SY) | (np.abs(q[..., 0] - q[..., 1] / 1) < 0.0)
        sheen = np.sin((p[..., 0] + p[..., 1] - p[..., 2]) * 6 + TAU * t * 2) > 0.85
        return 0.1 * np.sign(n[..., 1]), np.maximum((e | sheen).astype(float) * 0.9, wire * 1.0)
    rot = roty(TAU * t)
    parts = [Part(shape, 0, rot=rot)]
    mats = [Material(c1, '#c8a0ff', metal=0.8, gloss=1.6, emissive=lerp3(H('#e8d0ff'), H('#c8a8ff'), wire), pattern=facets)]
    view = dict(view_pitch=0.3, view_yaw=0.5, zoom=1.0)

    def ov(img):
        cx, cy, _ = proj((0, 0, 0), view)
        if wire > 0.5:                                # vertex glints: near (w>0) bright, far (w<0) dim violet
            for i in range(8):
                x, y, z = proj(rot @ P[i], view)
                k = cl(0.5 + 0.5 * w4[i])
                dot(img, x, y, lerp3(H('#a070ff'), WHITE, k), (wire - 0.5) * 2 * (0.5 + 0.5 * k))
            # inside-out moments: a vertex passes the centre
            through = abs(math.sin(th * 2))
            glow(img, cx, cy, 5, H('#e0c8ff'), (1 - through) * 0.8 * wire)
            # a ghost of the previous rotation (afterimage)
            Pg, _ = _tc_proj(max(0.0, th - 0.35))
            for a, b in TC_E[::2]:
                for f in (0.33, 0.66):
                    x, y, _ = proj(rot @ (Pg[a] * (1 - f) + Pg[b] * f), view)
                    dot(img, x, y, H('#b080ff'), 0.35 * wire, behind=True)
        fl = pulse(t, 0.87, 0.025) + 0.7 * pulse(t, 0.3, 0.025)
        outline_flash(img, WHITE, min(1.0, fl))
        star(img, cx, cy, 12, WHITE, fl, diag=0.5)
        if wire < 0.05:
            anim.ov_sparkles(img, (t * 2) % 1.0, 9, H('#ffffff'), 3)
    return parts, mats, view, ov


# =========================================================================================== ABSOLUTE SINGULARITY
AS_SHAPES = [(0, 0.0), (1, 0.25), (2, 0.5), (0, 0.75)]


def absolute_singularity(c1, c2, t):
    """A point where there are no laws: a black sphere with a white rim inside a faint coordinate lattice, little
    geometric solids orbiting it. It does not pull or lens anything - instead space is erased: a flat white void spreads
    out from it, the lattice vanishes and the solids lose their shading and drop away; the void flips black-white,
    white-black, like a negative; then space is drawn back in from the edges and geometry returns."""
    E = 13.5 * sm(win(t, 0.3, 0.42)) * (1 - sm(win(t, 0.68, 0.84)))
    sc = 1 - sm(win(t, 0.32, 0.42)) + sm(win(t, 0.74, 0.88))
    flips = [(0.47, 0.53), (0.58, 0.63)]
    neg = any(a <= t < b for a, b in flips)
    view = dict(view_pitch=0.35, view_yaw=0.5, zoom=1.0)
    vdir = -cam_of(view)[2]

    def rimp(p, n):
        r = np.abs(n @ vdir) < 0.45
        return np.zeros(r.shape), r.astype(float)
    m0 = Material('#000000', '#ffffff', metal=0.0, gloss=0.4, emissive='#ffffff', pattern=rimp, keep_dark=True)
    m0.ramp[:] = np.array([[0.0, 0.0, 0.0], [0.0, 0.0, 0.0], [0.01, 0.01, 0.01], [0.03, 0.03, 0.03], [0.06, 0.06, 0.06], [0.15, 0.15, 0.15]])
    parts = [Part(lambda p: sd_sphere(p, 0.36), 0)]
    mats = [m0, Material('#9098a8', '#ffffff', metal=0.6, gloss=1.2)]
    spos = []
    for i, (kind, ph) in enumerate(AS_SHAPES):
        a = TAU * (t + ph)
        dy = -0.6 * (1 - sc)
        pos = (math.cos(a) * 0.72, 0.18 * math.sin(a) + dy + 0.05 * (i % 2), math.sin(a) * 0.72)
        spos.append(pos)
        if sc > 0.05:
            fn = [lambda p: sd_box(p, (0.1, 0.1, 0.1), 0.01), lambda p: sd_octa(p, 0.16), lambda p: sd_torus(p, 0.1, 0.035)][kind]
            parts.append(Part(scaled(fn, sc), 1, pos=pos, rot=rotx(TAU * t * 2 + i) @ rotz(TAU * t + i)))

    def ov(img):
        cx, cy, _ = proj((0, 0, 0), view)
        rr = np.hypot(XX - cx, YY - cy)
        # the faint lattice of space (laws), behind everything, erased inside the void
        lat = ((np.abs((XX - cx) % 5 - 2.5) > 2.0) | (np.abs((YY - cy) % 5 - 2.5) > 2.0)) & (rr < 14.5) & (rr > E + 0.5)
        A = lat * 0.42 * np.clip((14.5 - rr) / 4, 0, 1)
        paint_behind(img, A, H('#a0a0b8'))
        if E > 0.5:
            void = rr < E
            sing = opaque(img) & (rr < 6.6)              # the singularity itself stays (flat)
            obj = opaque(img) & ~sing & void
            fg, bgc = (WHITE, np.zeros(3)) if neg else (np.zeros(3), WHITE)
            # everything inside loses its shading: flat silhouettes
            img[..., :3] = np.where((void & ~obj & ~sing)[..., None], bgc, img[..., :3])
            img[..., 3] = np.where(void, 1.0, img[..., 3])
            img[..., :3] = np.where((obj | (sing & (rr < 4.0)))[..., None], fg, img[..., :3])
            img[..., :3] = np.where((sing & (rr >= 4.0))[..., None], bgc * 0 + (WHITE if not neg else np.zeros(3)) * 0 + fg, img[..., :3])
            # crisp rim of the void
            rim = (np.abs(rr - E) < 0.7)
            img[..., :3] = np.where(rim[..., None], WHITE * 0.55 if not neg else WHITE, img[..., :3])
            img[..., 3] = np.where(rim, 1.0, img[..., 3])
            # the point itself always has its white rim (on white: a black dot with a grey ring)
            core = rr < 2.6
            img[..., :3] = np.where(core[..., None], fg, img[..., :3])
            ringp = np.abs(rr - 3.2) < 0.6
            img[..., :3] = np.where(ringp[..., None], bgc * 0.4 + fg * 0.6 if not neg else lerp3(bgc, fg, 0.5), img[..., :3])
        else:
            k = 0.35 + 0.25 * math.sin(TAU * t * 3)
            ring(img, cx, cy, 7.0, WHITE, k, w=0.7, behind=True)
        fl = pulse(t, 0.3, 0.02)
        glow(img, cx, cy, 15, WHITE, fl)
    return parts, mats, view, ov


# =========================================================================================== ESCHATON CORE
def _clock(t, a=0.5, b=0.6):
    """Time that stops: advances 0->1 over the loop but stands still between a and b."""
    if t < a:
        return t / (1 - (b - a))
    if t < b:
        return a / (1 - (b - a))
    return (t - (b - a)) / (1 - (b - a))


def _sq_torus(p, R, w, h):
    q0 = np.sqrt(p[..., 0] ** 2 + p[..., 2] ** 2) - R
    q = np.stack([np.abs(q0) - w, np.abs(p[..., 1]) - h], -1)
    return length(np.maximum(q, 0)) + np.minimum(np.maximum(q[..., 0], q[..., 1]), 0) - 0.01


def eschaton_core(c1, c2, t):
    """The end of all things, held still. Inside a gimbal cage of three white-gold bands a little universe ages: its
    stars go out one by one and it reddens; it collapses to a point while the bands clamp in tight; time stops - the cage
    freezes, glowing - then it bangs back to a young universe full of pink-white stars and the cage swings free."""
    age = win(t, 0.0, 0.36)
    crunch = sm(win(t, 0.36, 0.5))
    bang = t >= 0.6
    if bang:
        rs = 0.36 * back(win(t, 0.6, 0.72))
        age = 0.0
    else:
        rs = 0.36 * (1 - crunch) + 0.03 * crunch
    clamp = crunch * (1 - sm(win(t, 0.6, 0.72))) if t >= 0.36 else 0.0
    held = 0.5 <= t < 0.6
    ck = _clock(t)
    hot = float(crunch > 0.6 and t < 0.6) + (1 - win(t, 0.6, 0.7)) * (t >= 0.6)
    young = lerp3(H('#5a2470'), H('#200830'), 0)
    old = H('#3a0a0a')

    def stars(p, n):
        cc = np.floor(p * 11)
        hh = _hs(cc[..., 0] * 7.1 + cc[..., 1] * 13.3 + cc[..., 2] * 3.7)
        thr = 0.55 + 0.43 * age ** 0.8
        tw = np.sin(TAU * (t * 4 + hh * 7)) > -0.3
        neb = np.sin(p[..., 0] * 7 + p[..., 1] * 5 + TAU * ck) * (1 - age)
        return 0.15 * neb, ((hh > thr) & tw).astype(float) + hot
    core_em = lerp3(lerp3(WHITE, H(c2), 0.4), H('#ff5030'), age)
    if bang:
        core_em = lerp3(WHITE, lerp3(WHITE, H(c2), 0.4), win(t, 0.6, 0.75))
    m0 = Material(lerp3(H('#7a3c90'), old, age), '#ff80ff', metal=0.1, gloss=0.6, emissive=core_em, pattern=stars, keep_dark=True)
    cage_em = lerp3(H('#ffe8a0'), H(c2), 0.6 if held else 0.0)
    m1 = Material(c1, '#ffd860', metal=1.0, gloss=1.6, emissive=cage_em,
                  pattern=lambda p, n: (np.zeros(p.shape[:-1]), np.maximum((np.sin(np.arctan2(p[..., 2], p[..., 0]) * 8 - TAU * ck * 3) > 0.7) * 0.8, 0.9 * held + 0.6 * clamp)))
    parts = [Part(lambda p: sd_sphere(p, max(rs, 0.02)), 0, rot=roty(TAU * ck))]
    Rb = 0.78 - 0.14 * clamp
    for k in range(3):
        R = (rotx(TAU * ck * (1 + k)) if k == 0 else rotz(TAU * ck * (k + 1) * (-1) ** k)) @ [np.eye(3), rotx(PI / 2), rotz(PI / 2) @ rotx(PI / 2)][k]
        R = [rotx(0.5) @ rotz(TAU * ck), rotz(0.6) @ rotx(PI / 2 + TAU * ck * 2), roty(TAU * ck) @ rotz(PI / 2) @ rotx(-TAU * ck)][k]
        parts.append(Part(lambda p, r=Rb - 0.08 * k: _sq_torus(p, r, 0.025, 0.05), 1, rot=R))
    view = dict(view_pitch=0.3, view_yaw=0.5, zoom=1.0)

    def ov(img):
        cx, cy, _ = proj((0, 0, 0), view)
        pink = H(c2)
        if 0.36 < t < 0.5:                             # the stars fall in
            anim.ov_infall(img, win(t, 0.36, 0.5), 5, H('#ffb0a0'), 10)
        if held:                                       # time stands still: the cage glows, the point burns
            k = bump(t, 0.5, 0.6)
            outline_flash(img, pink, 0.7 * k)
            glow(img, cx, cy, 4, WHITE, 0.9)
            dot(img, cx, cy, WHITE, 1.0)
        fl = pulse(t, 0.6, 0.025)
        glow(img, cx, cy, 16, lerp3(WHITE, pink, 0.3), fl)
        star(img, cx, cy, 14, WHITE, fl, diag=0.6)
        if 0.6 < t < 0.78:
            x = win(t, 0.6, 0.78)
            ring(img, cx, cy, 4 + x * 12, pink, (1 - x) * 0.9, sy=0.7)
        if t > 0.7 or t < 0.08:                        # young: new stars glint around it
            anim.ov_sparkles(img, (t * 3) % 1.0, 29, lerp3(pink, WHITE, 0.5), 3)
    return parts, [m0, m1], view, ov


# =========================================================================================== exports
ITEMS = {
    'omnipotence_shard': omnipotence_shard,
    'transcendental_echo': transcendental_echo,
    'omnipotence_dust': omnipotence_dust,
    'apeiron_ingot': apeiron_ingot,
    'pleroma_crystal': pleroma_crystal,
    'logos_thread': logos_thread,
    'rekindled_spark': rekindled_spark,
    'omnipotent_ingot': omnipotent_ingot,
    'transcendence_crystal': transcendence_crystal,
    'absolute_singularity': absolute_singularity,
    'eschaton_core': eschaton_core,
}
RECIPES = {k: ((), ()) for k in ITEMS}
NARRATIVES = {}
FRAMES = {
    'omnipotence_shard': 64, 'transcendental_echo': 64, 'omnipotence_dust': 64, 'apeiron_ingot': 72,
    'pleroma_crystal': 64, 'logos_thread': 72, 'rekindled_spark': 64, 'omnipotent_ingot': 80,
    'transcendence_crystal': 72, 'absolute_singularity': 64, 'eschaton_core': 72,
}
