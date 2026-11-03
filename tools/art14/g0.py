"""Item art group 0 (round 14): transformation animations for the top ingots and the endgame catalysts / cores.
Every item tells a little story in a 60-80 frame loop: it shatters and re-forms, collapses and is reborn, ages to dust
and is restored, opens and closes, freezes or unfurls - and always lands back exactly where it started (t=0 == t=1)."""
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
    """0 at a, 1 in the middle, 0 at b."""
    x = win(t, a, b)
    return 0.0 if x <= 0 or x >= 1 else math.sin(PI * x)


def back(x):
    """ease-out with overshoot (grows a little too big, settles)."""
    x = cl(x)
    if x <= 0:
        return 0.0
    c1 = 1.70158
    return 1 + (c1 + 1) * (x - 1) ** 3 + c1 * (x - 1) ** 2


def hsv(h, s=1.0, v=1.0):
    return np.array(colorsys.hsv_to_rgb(h % 1.0, s, v))


def lerp3(a, b, k):
    return np.asarray(a, float) * (1 - k) + np.asarray(b, float) * k


def _hs(a):
    v = np.sin(a * 12.9898 + 4.1) * 43758.5453
    return v - np.floor(v)


# =========================================================================================== SDF helpers
def _ing(p, L=0.85, Hh=0.3, W=0.45):
    return np.maximum(sd_box(p, (L, Hh, W), 0.05), (p[..., 1] * 0.95 + np.abs(p[..., 2]) * 0.6 + np.abs(p[..., 0]) * 0.12 - (Hh + 0.12)))


def _void(p):
    return np.full(p.shape[:-1], 9.0)


def scaled(fn, s):
    if s < 0.015:
        return _void
    return lambda p: fn(p / s) * s


def _segs(p, segs, r):
    d = None
    for a, b in segs:
        di = sd_capsule(p, a, b, r)
        d = di if d is None else np.minimum(d, di)
    return d


def _flat(v):
    return lambda p, n: (np.zeros(p.shape[:-1]), np.full(p.shape[:-1], float(v)))


ING_VIEW = dict(view_pitch=0.6, view_yaw=0.65, zoom=1.05)

# the ingot cut into 4 x 2 slices (half sizes 0.2125 x 0.225)
SLICES = [(x, z) for z in (-0.225, 0.225) for x in (-0.6375, -0.2125, 0.2125, 0.6375)]


def _slice_centered(cx, cz, gap=0.0):
    """One slice of the ingot, moved so that its centre is the local origin."""
    def f(p):
        q = p.copy()
        q[..., 0] += cx
        q[..., 2] += cz
        reg = np.maximum(np.abs(p[..., 0]) - 0.2125 + gap, np.abs(p[..., 2]) - 0.225 + gap)
        return np.maximum(_ing(q), reg)
    return f


# =========================================================================================== 2D painting helpers
N = 32
YY, XX = np.mgrid[0:N, 0:N].astype(float)


def proj(pt, view):
    cam = rotx(-view.get('view_pitch', 0.5)) @ roty(view.get('view_yaw', 0.7853))
    c = np.asarray(pt, float) @ cam.T
    k = view.get('zoom', 1.0) / view.get('extent', 1.0)
    return (c[0] * k + 1) / 2 * N - 0.5, (-c[1] * k + 1) / 2 * N - 0.5, c[2]


def paint(img, A, rgb, add=False):
    """Composite colour rgb (3,) or (N,N,3) with alpha map A over the image."""
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


def glow(img, cx, cy, R, rgb, k, add=True, power=1.5):
    if k <= 0 or R <= 0:
        return
    r = np.hypot(XX - cx, YY - cy)
    paint(img, k * np.clip(1 - r / R, 0, 1) ** power, rgb, add)


def ring(img, cx, cy, r, rgb, k, w=0.9, sy=1.0):
    if k <= 0:
        return
    d = np.hypot(XX - cx, (YY - cy) / sy)
    paint(img, k * np.clip(1 - np.abs(d - r) / w, 0, 1), rgb, True)


def star(img, cx, cy, L, rgb, k, diag=0.0):
    """Lens-flare cross (and optionally a fainter diagonal cross)."""
    if k <= 0:
        return
    dx, dy = XX - cx, YY - cy
    A = np.clip(1 - np.abs(dx) / L, 0, 1) * (np.abs(dy) < 0.6) + np.clip(1 - np.abs(dy) / L, 0, 1) * (np.abs(dx) < 0.6)
    if diag:
        A = A + diag * np.clip(1 - np.abs(dx) / (L * 0.6), 0, 1) * (np.abs(np.abs(dx) - np.abs(dy)) < 0.6)
    paint(img, k * np.clip(A, 0, 1), rgb, True)


def rays(img, cx, cy, r0, r1, count, rgb, k, phase=0.0):
    if k <= 0:
        return
    for i in range(count):
        a = TAU * i / count + phase
        for r in np.linspace(r0, r1, int(max(2, (r1 - r0) * 1.5))):
            dot(img, cx + math.cos(a) * r, cy + math.sin(a) * r, rgb, k * (1 - (r - r0) / max(1e-6, r1 - r0) * 0.6))


def dot(img, x, y, rgb, a=1.0, behind=False):
    xi, yi = int(round(x)), int(round(y))
    if 0 <= xi < N and 0 <= yi < N:
        if behind and img[yi, xi, 3] > 0.5:
            return
        anim._blend(img, yi, xi, np.asarray(rgb, float), cl(a))


def spark(img, x, y, rgb, s):
    """Four-point star sparkle of size s in [0,1]."""
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


def outline_flash(img, rgb, k):
    if k > 0:
        paint(img, edge(img) * k, rgb)


def fade(img, k):
    img[..., 3] *= cl(k)


def recolor(img, tint, k, lift=1.15):
    """Repaint the lit body through a colour map (keeps the shading, dark outline stays dark)."""
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


def invert(img, k):
    op = opaque(img)[..., None]
    img[..., :3] = np.where(op, img[..., :3] * (1 - k) + (1 - img[..., :3]) * k, img[..., :3])


# =========================================================================================== OMNIUM INGOT
OMNI_INGR = [H(c) for c in ('#d8d8ff', '#ffd060', '#60ff9a', '#ff6080', '#60b0ff', '#c080ff', '#ff9040', '#40ffe0')]


def omnium_ingot(c1, c2, t):
    """The showpiece. Every ingredient metal flows through it as colour bands; it cracks with white light, shatters into a
    prismatic halo of eight orbiting shards, the halo collapses to a single white point, and a perfect ingot is reborn
    from the flash with a rainbow sheen and a lens flare."""
    if t < 0.745:
        e = sm(win(t, 0.34, 0.46))                    # exploded into a halo
        col = sm(win(t, 0.60, 0.71))                  # halo collapses to the point
        sg = 1.0
        gap = 0.025 * sm(win(t, 0.26, 0.34))
        rc = 0.27 * sm(win(t, 0.24, 0.34)) * (1 - col) + 0.07 * col
        rc += 0.03 * math.sin(TAU * t * 8) * e * (1 - col)
    else:
        e = col = gap = 0.0
        sg = back(win(t, 0.745, 0.86))                # reborn, overshooting a little
        rc = 0.0
    shake = 0.018 * bump(t, 0.27, 0.37)
    spin = TAU * 2 * t
    Rr = 0.92 * (1 - col)
    parts, mats = [], []
    for k, (cx, cz) in enumerate(SLICES):
        a = TAU * k / 8 + spin
        ringp = rotx(-0.75) @ np.array([math.cos(a) * Rr, 0.0, math.sin(a) * Rr])
        jit = shake * np.array([math.sin(TAU * t * 31 + k * 1.7), math.sin(TAU * t * 23 + k * 2.9), math.sin(TAU * t * 29 + k)])
        pos = sg * lerp3((cx, 0, cz), ringp, e) + jit
        ps = sg * (1 - 0.35 * e) * (1 - col)
        rot = roty(-a * e) @ rotz(e * (1.2 + TAU * t)) @ rotx(e * 0.8 * k)
        hue = k / 8 + 2 * t
        mats.append(Material(lerp3(H('#c4c4f0'), hsv(hue, 0.7, 1.0), e), '#ffffff', metal=0.9, gloss=1.4,
                             emissive=hsv(hue, 0.55, 1.0), pattern=_flat(0.95 * e)))
        parts.append(Part(scaled(_slice_centered(cx, cz, gap), ps), k, pos=pos, rot=rot))
    mats.append(Material('#ffffff', '#ffffff', emissive='#ffffff', pattern=pat_all()))
    parts.append(Part(scaled(lambda p: sd_sphere(p, 1.0), rc), 8))
    view = dict(view_pitch=0.6, view_yaw=0.65 + 0.3 * math.sin(TAU * t), zoom=1.05 - 0.12 * e)

    def ov(img):
        cx, cy, _ = proj((0, 0, 0), view)
        # ingredient bands flowing through the metal / rainbow sheen after the rebirth
        u = (XX * 0.55 + YY * 0.45) / 3.2 - t * 8
        i0 = np.floor(u).astype(int) % 8
        f = (u - np.floor(u))[..., None]
        P = np.array(OMNI_INGR)
        silver = H('#e4e4ff')
        sel = np.clip((f - 0.38) / 0.1, 0, 1) * np.clip((0.98 - f) / 0.1, 0, 1)
        bands = silver * (1 - sel) + P[i0] * sel
        w_rain = sm(win(t, 0.745, 0.78)) * (1 - sm(win(t, 0.86, 1.0)))
        rh = (XX + YY) / 34.0 - t * 3
        rain = np.stack([hsv(h, 0.6, 1.0) for h in np.linspace(0, 1, 64, endpoint=False)])[((rh % 1.0) * 64).astype(int) % 64]
        tint = bands * (1 - w_rain) + rain * w_rain
        recolor(img, tint, (1 - e) * 0.85)
        # light leaking through the cracks
        if 0.27 < t < 0.4:
            k = bump(t, 0.27, 0.4)
            for i in range(5):
                a = TAU * (i / 5 + 0.13)
                spark(img, cx + math.cos(a) * 9, cy + math.sin(a) * 4, WHITE, k * (0.5 + 0.5 * math.sin(TAU * t * 12 + i)))
        # shatter shockwave
        if 0.34 < t < 0.47:
            x = win(t, 0.34, 0.47)
            ring(img, cx, cy, 3 + x * 13, hsv(x + t, 0.4, 1.0), (1 - x) * 0.9, sy=0.8)
        # prismatic trails behind the orbiting shards
        if e > 0.3 and col < 0.95:
            for k in range(8):
                for j in range(1, 4):
                    a = TAU * k / 8 + spin - 0.18 * j
                    pt = rotx(-0.75) @ np.array([math.cos(a) * Rr, 0.0, math.sin(a) * Rr])
                    x, y, z = proj(pt, view)
                    dot(img, x, y, hsv(k / 8 + 2 * t, 0.6, 1.0), (e - 0.3) * (1 - col) * (0.8 - 0.22 * j), behind=z > 0)
        # infall and the white point
        if 0.58 < t < 0.745:
            anim.ov_infall(img, (t - 0.58) / 0.165, 7, hsv(t * 3, 0.4, 1.0), 10)
            glow(img, cx, cy, 2.5 + 4 * col, WHITE, 0.6 + 0.4 * col)
        # the flash, flare and rainbow shockwave of the rebirth
        fl = math.exp(-((t - 0.75) / 0.025) ** 2)
        glow(img, cx, cy, 15, WHITE, fl)
        if 0.74 < t < 0.9:
            x = win(t, 0.745, 0.9)
            star(img, cx, cy, 6 + 10 * (1 - x), WHITE, (1 - x), diag=0.5)
            for k in range(12):
                a = TAU * k / 12
                rr = 4 + x * 13
                dot(img, cx + math.cos(a) * rr, cy + math.sin(a) * rr * 0.7, hsv(k / 12 + t, 0.6, 1.0), (1 - x))
        if 0.8 < t < 1.0:
            x = win(t, 0.8, 1.0)
            for i in range(4):
                life = (x * 2 + i / 4) % 1.0
                a = TAU * (i * 0.37 + 0.1)
                spark(img, cx + math.cos(a) * 11, cy + math.sin(a) * 5, hsv(i / 4 + t * 2, 0.5, 1.0), math.sin(PI * life) * (1 - x))
        # idle: a prismatic sparkle runs the outline once
        if t < 0.25:
            ex = edge(img)
            ys, xs = np.nonzero(ex)
            if len(xs):
                order = np.argsort(np.arctan2(ys - cy, xs - cx))
                i = order[int(win(t, 0.0, 0.25) * (len(order) - 1))]
                spark(img, xs[i], ys[i], hsv(t * 4, 0.5, 1.0), bump(t, 0.0, 0.25))
    return parts, mats, view, ov


# =========================================================================================== TRANSCENDENT METAL INGOT
TM_TINTS = ['#d8d8ff', '#c8f0ff', '#fff0c8', '#ffd8f4']


def transcendent_metal_ingot(c1, c2, t):
    """A white metal that cannot decide its shape: it holds a form, shudders, and flows into the next one - ingot,
    sphere, octahedron, twisted bar - with a flash and a ring of sparks at every change, tinting as it goes."""
    u = 4 * t
    k = int(u) % 4
    f = u - math.floor(u)
    m = sm((f - 0.55) / 0.45)
    w = [0.0] * 4
    w[k] += 1 - m
    w[(k + 1) % 4] += m
    surge = math.sin(PI * m)
    sq = 1 - 0.14 * surge

    def morph(p):
        p = p / sq
        q = p.copy()
        tw = 1.2 * p[..., 0]
        c, sn = np.cos(tw), np.sin(tw)
        q[..., 1], q[..., 2] = c * p[..., 1] - sn * p[..., 2], sn * p[..., 1] + c * p[..., 2]
        d = 0.0
        if w[0]:
            d = d + w[0] * _ing(p)
        if w[1]:
            d = d + w[1] * sd_sphere(p, 0.52)
        if w[2]:
            d = d + w[2] * sd_octa(p, 0.74)
        if w[3]:
            d = d + w[3] * sd_box(q, (0.8, 0.14, 0.2), 0.04) * 0.8
        return d * sq

    def flow(p, n):
        v = np.sin(p[..., 1] * 10 + p[..., 0] * 3 - t * TAU * 8)
        return 0.08 * v, ((v > 0.55) * (0.15 + surge)).astype(float)
    tint = lerp3(H(TM_TINTS[k]), H(TM_TINTS[(k + 1) % 4]), m)
    parts = [Part(morph, 0)]
    mats = [Material(tint, c2, metal=0.9, gloss=1.5, emissive=mix(tint, WHITE, 0.6), pattern=flow)]
    view = dict(view_pitch=0.45, view_yaw=0.65 + TAU * t, zoom=1.05)

    def ov(img):
        cx, cy, _ = proj((0, 0, 0), view)
        outline_flash(img, mix(tint, WHITE, 0.5), 0.9 * surge)
        if f > 0.7:
            x = (f - 0.7) / 0.3
            for i in range(8):
                a = TAU * i / 8 + k * 0.4
                dot(img, cx + math.cos(a) * (6 + 9 * x), cy + math.sin(a) * (6 + 9 * x) * 0.75, mix(tint, WHITE, 0.3), 1 - x)
        glow(img, cx, cy, 7, WHITE, 0.45 * surge)
        if f < 0.5:                                    # calm: a single sparkle drifts over the form
            spark(img, cx - 5 + f * 18, cy - 4 + f * 4, tint, math.sin(PI * f * 2) * 0.8)
    return parts, mats, view, ov


# =========================================================================================== COMPUTRONIUM INGOT
CP_SLOT = [((i % 4 - 1.5) * 0.47, 0.0, (i // 4 - 0.5) * 0.66) for i in range(8)]


def computronium_ingot(c1, c2, t):
    """Matter arranged for computation: the circuit-traced ingot decompiles into eight glowing bit-cubes laid out as a
    register, which clocks four times (the rows shift past each other, bits flip, a wrapping cube hops over the top),
    then it compiles back into the ingot with a green flash."""
    e = sm(win(t, 0.28, 0.40)) * (1 - sm(win(t, 0.76, 0.88)))
    T0, TL = 0.41, 0.085
    j = int(cl((t - T0) / TL, 0, 3.999)) if t > T0 else 0
    tick = cl((t - T0) / TL, 0, 4)
    jt = int(math.floor(tick)) if tick < 4 else 4
    ft = sm(((tick - jt) / 0.55)) if tick < 4 else 0.0
    clk = 0.0 < tick < 4
    parts, mats = [], []
    mats.append(Material(c1, c2, metal=0.85, emissive=c2, pattern=pat_combine(pat_traces(t, 2), pat_bands(9, 2, t, 0.08, axis=1))))
    mats.append(Material('#60ff9a', '#ffffff', metal=0.6, emissive='#80ffb0', pattern=pat_all()))
    mats.append(Material('#0c2418', c2, metal=0.95, keep_dark=True, emissive=c2, pattern=pat_grid(9, t, 0.1)))
    for k, (cx, cz) in enumerate(SLICES):
        row, col0 = k // 4, k % 4
        d = 1 if row == 0 else -1
        a_col = (col0 + d * jt) % 4
        b_col = (col0 + d * (jt + 1)) % 4
        if clk:
            if abs(b_col - a_col) == 3:                  # wrapping cube hops over the row
                xc = (a_col + (b_col - a_col) * ft - 1.5) * 0.47
                lift = 0.45 * math.sin(PI * ft)
            else:
                xc = (a_col + (b_col - a_col) * ft - 1.5) * 0.47
                lift = 0.08 * math.sin(PI * ft)
        else:
            xc, lift = (a_col - 1.5) * 0.47, 0.0
        tgt = np.array([xc, lift + 0.05 * math.sin(TAU * t * 4 + k), (row - 0.5) * 0.66])
        c = np.array([cx, 0.0, cz])

        def fn(p, c=c, e=e):
            sl = np.maximum(_ing(p), np.maximum(np.abs(p[..., 0] - c[0]) - 0.2125, np.abs(p[..., 2] - c[2]) - 0.225))
            if e <= 0:
                return sl
            cube = sd_box(p - c, (0.15, 0.15, 0.15), 0.03)
            return sl * (1 - e) + cube * e
        if e > 0.5:
            bit = _hs(np.float64(k * 7.3 + jt * 3.1 + 1)) > 0.45
            mi = 1 if bit else 2
        else:
            mi = 0
        parts.append(Part(fn, mi, pos=(tgt - c) * e))
    view = dict(view_pitch=0.6 + 0.1 * e, view_yaw=0.65 + 0.15 * math.sin(TAU * t), zoom=1.05 - 0.05 * e)

    def ov(img):
        cx, cy, _ = proj((0, 0, 0), view)
        g = H(c2)
        if clk and ft < 0.3:                             # clock edge: every bit pulses
            outline_flash(img, g, 0.6 * (1 - ft / 0.3))
        if e > 0.6:                                      # data packets running between the cubes
            for i in range(4):
                life = (t * 6 + i / 4) % 1.0
                a = CP_SLOT[(i * 3) % 8]
                b = CP_SLOT[(i * 3 + 5) % 8]
                pt = lerp3(a, b, life)
                x, y, _ = proj(pt + np.array([0, 0.2, 0]), view)
                dot(img, x, y, WHITE, math.sin(PI * life))
        snap = math.exp(-((t - 0.875) / 0.02) ** 2) + math.exp(-((t - 0.395) / 0.02) ** 2)
        outline_flash(img, g, snap)
        glow(img, cx, cy, 14, g, 0.35 * snap)
        if e < 0.05:                                     # idle: a scan line sweeps the circuitry
            s = (t * 2) % 1.0
            band = np.abs((XX - YY * 0.3) - (-11 + s * 44)) < 1.0
            paint(img, band * opaque(img) * 0.4, g, True)
    return parts, mats, view, ov


# =========================================================================================== CHRONOSTEEL INGOT
_rng = np.random.default_rng(14)
CH_DUST = [(float(_rng.uniform(7, 25)), float(_rng.uniform(11, 20)), float(_rng.uniform(-2.5, 2.5)), float(_rng.uniform(0.7, 1.0)))
           for _ in range(16)]


def _chrono_hand(t):
    # 6 ticks, forward whirl of 3 turns, backward whirl of 3 turns, 6 ticks: exactly one turn per loop
    if t < 0.25:
        n = math.floor(t * 24)
    elif t < 0.5:
        n = 6 + 36 * sm(win(t, 0.27, 0.48))
    elif t < 0.75:
        n = 42 - 36 * sm(win(t, 0.52, 0.73))
    else:
        n = 6 + math.floor((t - 0.75) * 24)
    return PI / 2 - n * TAU / 12


def chronosteel_ingot(c1, c2, t):
    """Steel forged in stopped time: the clock on its face ticks, then races ahead - the ingot rusts, pits and crumbles
    to dust in seconds - time freezes with a golden flash, the hand spins backwards, the dust flies back up and the
    ingot stands pristine again. It never ages."""
    hand = _chrono_hand(t)
    if t < 0.5:
        age = sm(win(t, 0.27, 0.46))
        s = 1 - sm(win(t, 0.40, 0.50))
    else:
        age = 1 - sm(win(t, 0.58, 0.72))
        s = sm(win(t, 0.55, 0.70))
    s = max(s, 0.0)
    wob = age * 0.07

    def body(p):
        nz = np.sin(p[..., 0] * 9 + 1) * np.sin(p[..., 1] * 11 + 2) * np.sin(p[..., 2] * 8 + 3) + 0.5 * np.sin(p[..., 0] * 23) * np.sin(p[..., 2] * 19 + 1)
        return _ing(p) + wob * nz + age * 0.03

    def rust(p, n):
        v = np.sin(p[..., 0] * 13 + 2) * np.sin(p[..., 2] * 11) * np.sin(p[..., 1] * 9 + 1)
        r = v > 0.35 - age * 0.9
        return -0.32 * r * age, np.zeros(v.shape)

    def face(p, n):
        x, z = p[..., 0], p[..., 2]
        r = np.sqrt(x * x + z * z)
        a = np.arctan2(z, x)
        top = p[..., 1] > 0.02
        rg = np.abs(r - 0.2) < 0.04
        ticks = (np.abs(np.mod(a, TAU / 4) - TAU / 8) > TAU / 8 - 0.3) & (np.abs(r - 0.13) < 0.04)
        hx, hz = math.cos(hand), math.sin(hand)
        along = x * hx + z * hz
        hnd = (np.abs(-x * hz + z * hx) < 0.04) & (along > -0.03) & (along < 0.17)
        return np.full(x.shape, -0.5), (top & (rg | ticks | hnd)).astype(float)
    base = lerp3(H(c1), H('#7a3a1a'), age * 0.85)
    parts = [Part(scaled(body, s), 0), Part(scaled(lambda p: sd_cyl(p - np.array([0, 0.31, 0]), 0.26, 0.04, 0.015), s), 1)]
    mats = [Material(base, lerp3(H(c2), H('#c06030'), age), metal=0.85 - 0.6 * age, gloss=0.8 - 0.5 * age, pattern=rust),
            Material(lerp3(H('#3a2a10'), H('#4a2010'), age), c2, metal=0.9, keep_dark=True,
                     emissive=lerp3(H('#fff4d0'), H('#ff9050'), age), pattern=face)]
    view = dict(ING_VIEW)

    def ov(img):
        cx, cy, _ = proj((0, 0, 0), view)
        gold = H(c2)
        frozen = 0.48 < t < 0.56
        # the dust: falls while time races, hangs while it is stopped, flies back up while it reverses
        if 0.38 < t < 0.74:
            if t < 0.5:
                dd = sm(win(t, 0.40, 0.52))
            elif t < 0.55:
                dd = 1.0
            else:
                dd = 1 - sm(win(t, 0.55, 0.71))
            for i, (x0, y0, vx, sp) in enumerate(CH_DUST):
                y = y0 + dd * sp * (29 - y0)
                x = x0 + dd * vx * 2
                colr = WHITE if frozen else (lerp3(H('#b06030'), gold, 0.3) if t < 0.5 else gold)
                dot(img, x, y, colr, 0.9 * min(1, dd * 4 + (0 if t < 0.5 else 0.3)))
        # time freezes: golden flash and outline
        fz = bump(t, 0.47, 0.57)
        outline_flash(img, gold, fz * 0.9)
        glow(img, cx, cy, 15, gold, 0.4 * math.exp(-((t - 0.505) / 0.02) ** 2))
        if frozen:
            ring(img, cx, cy, 8 + 6 * win(t, 0.48, 0.56), gold, 0.6, sy=0.6)
        # the restored ingot gleams
        rg = bump(t, 0.68, 0.8)
        if rg > 0:
            pos = -6 + win(t, 0.68, 0.8) * 50
            band = np.clip(1 - np.abs((XX + YY) - pos) / 2.5, 0, 1) * opaque(img)
            paint(img, band * 0.7 * rg, WHITE, True)
        # idle: dust motes frozen in mid-air
        if t < 0.3 or t > 0.74:
            for (x, y) in ((3, 3), (27, 6), (5, 26), (28, 24), (14, 2)):
                dot(img, x, y, gold, 0.7)
    return parts, mats, view, ov


# =========================================================================================== HYPERNOVA ALLOY INGOT
def hypernova_alloy_ingot(c1, c2, t):
    """Still glowing from the forge: the boiling plasma ingot implodes to a white-hot bead, rebounds as a hypernova
    (flash, shock ring, rays), and the expanding plasma ball cools - white, gold, pink - and condenses back into the ingot."""
    if t < 0.52:
        s = 1 - 0.86 * sm(win(t, 0.34, 0.50))
        ws = sm(win(t, 0.36, 0.50))                      # how much of a sphere it is
        heat = sm(win(t, 0.36, 0.50))
    else:
        grow = sm(win(t, 0.52, 0.66))
        s = 0.14 + 0.98 * grow - 0.12 * sm(win(t, 0.66, 0.84))
        ws = 1 - sm(win(t, 0.66, 0.84))
        heat = 1 - sm(win(t, 0.58, 0.88))
    wob = 0.03 + 0.05 * ws

    def shape(p):
        sph = sd_sphere(p, 0.5) + wob * np.sin(p[..., 0] * 9 + t * TAU * 3) * np.sin(p[..., 1] * 8 - t * TAU * 2)
        ing = _ing(p) + 0.025 * np.sin(p[..., 0] * 11 + t * TAU) * np.sin(p[..., 2] * 9)
        return ing * (1 - ws) + sph * ws

    def plasma(p, n):
        a = np.sin(p[..., 0] * 7 + np.sin(p[..., 2] * 9 + t * TAU) * 1.5 + t * TAU * 2)
        b = np.sin(p[..., 2] * 8 - p[..., 1] * 6 + np.sin(p[..., 0] * 5 - t * TAU * 2))
        v = a * b
        return 0.2 * v, np.maximum((v > 0.15 - heat).astype(float), heat)
    if heat > 0.66:
        em = lerp3(H('#ffd040'), WHITE, (heat - 0.66) / 0.34)
    elif heat > 0.33:
        em = lerp3(H('#ff8050'), H('#ffd040'), (heat - 0.33) / 0.33)
    else:
        em = lerp3(H('#fff0d0'), H('#ff8050'), heat / 0.33)
    parts = [Part(scaled(shape, s), 0)]
    mats = [Material(lerp3(H(c1), em, heat), c2, metal=0.7, emissive=em, pattern=plasma)]
    view = dict(view_pitch=0.6, view_yaw=0.65 + 0.2 * math.sin(TAU * t), zoom=1.05)

    def ov(img):
        cx, cy, _ = proj((0, 0, 0), view)
        pink, warm = H(c1), H(c2)
        if 0.3 < t < 0.52:
            anim.ov_infall(img, win(t, 0.3, 0.52), 5, pink, 12)
            glow(img, cx, cy, 4, WHITE, sm(win(t, 0.4, 0.5)))
        fl = math.exp(-((t - 0.52) / 0.02) ** 2)
        glow(img, cx, cy, 16, WHITE, fl)
        if 0.51 < t < 0.68:
            x = win(t, 0.51, 0.68)
            ring(img, cx, cy, 3 + 14 * x, lerp3(WHITE, pink, x), 1 - x, w=1.2)
            rays(img, cx, cy, 5 + 6 * x, 9 + 9 * x, 10, warm, 1 - x, phase=0.3)
        if t > 0.56 and t < 0.92:
            x = win(t, 0.56, 0.92)
            anim.ov_embers(img, x, 11, warm, 6, 1.0)
        if t < 0.3 or t > 0.85:                          # idle: molten droplets lift off the top
            anim.ov_embers(img, (t * 2) % 1.0, 3, pink, 3, 0.6)
    return parts, mats, view, ov


# =========================================================================================== FLEROVIUM INGOT
FL_NUC = [(0, 0, 0), (0.21, 0, 0), (-0.21, 0, 0), (0, 0.21, 0), (0, -0.21, 0), (0, 0, 0.21), (0, 0, -0.21)]


def flerovium_ingot(c1, c2, t):
    """Element 114 on the island of stability: the steel ingot's two orbiting shells tighten, the ingot opens into its
    nucleus - a cluster of protons and neutrons jostling inside two closed shells that flash at the magic number - and
    the nucleons fold back into the ingot."""
    e = sm(win(t, 0.28, 0.42)) * (1 - sm(win(t, 0.70, 0.84)))
    s = 1 - e
    jig = e * 0.035
    extra = 2 * TAU * sm(win(t, 0.36, 0.76))           # shells whirl two extra turns while closed around the nucleus
    R1 = 0.82 - 0.18 * e
    R2 = 0.7 - 0.08 * e
    parts = [Part(scaled(lambda p: _ing(p, 0.62, 0.24, 0.36), s), 0),
             Part(lambda p: sd_torus(p, R1, 0.04), 1, rot=rotx(0.35 - 0.2 * e) @ roty(t * TAU + extra)),
             Part(lambda p: sd_torus(p, R2, 0.04), 1, rot=rotz(1.1 - 0.2 * e) @ rotx(-0.3 + 0.9 * e) @ roty(-t * TAU - extra))]
    mats = [Material(c1, c2, metal=0.95),
            Material('#8098c0', '#ffffff', metal=0.9, emissive=c2,
                     pattern=lambda p, n: (np.zeros(p.shape[:-1]), np.maximum((np.sin(np.arctan2(p[..., 2], p[..., 0]) * 7) > 0.55).astype(float), magic))),
            Material('#ff9070', '#ffe0d0', metal=0.3, emissive='#ffb090', pattern=_flat(0.0)),
            Material('#a8b8d8', '#ffffff', metal=0.5)]
    magic = bump(t, 0.52, 0.62)
    rot_cl = roty(TAU * t * 2) @ rotx(0.5)
    for i, q in enumerate(FL_NUC):
        q = rot_cl @ np.array(q) * (0.4 + 0.9 * e)
        j = jig * np.array([math.sin(TAU * t * 17 + i * 2.1), math.sin(TAU * t * 13 + i * 1.3), math.sin(TAU * t * 19 + i)])
        r = 0.04 + 0.15 * e
        parts.append(Part(lambda p, r=r: sd_sphere(p, r), 2 if i % 2 == 0 else 3, pos=q + j))
    view = dict(ING_VIEW, zoom=1.05 + 0.05 * e, view_pitch=0.6 - 0.25 * e)

    def ov(img):
        cx, cy, _ = proj((0, 0, 0), view)
        if magic > 0:
            outline_flash(img, H(c2), magic)
            glow(img, cx, cy, 12, H(c2), 0.4 * magic)
            ring(img, cx, cy, 6 + 8 * win(t, 0.52, 0.62), WHITE, magic * 0.8)
        if 0.3 < t < 0.42 or 0.72 < t < 0.84:
            k = bump(t, 0.3, 0.42) + bump(t, 0.72, 0.84)
            for i in range(6):
                a = TAU * i / 6 + t * 5
                spark(img, cx + math.cos(a) * 7, cy + math.sin(a) * 5, H(c2), 0.6 * k * (i % 2))
        if e < 0.05:                                     # idle: "114" decays nowhere - a faint shell shimmer
            anim.ov_sparkles(img, (t * 2) % 1.0, 114, H(c2), 3)
    return parts, mats, view, ov


# =========================================================================================== ANTI-GOLD INGOT
def antigold_ingot(c1, c2, t):
    """Cold blue anti-gold whose gloss runs the wrong way. Its matter twin - a gold ingot - fades in above it, mirrored,
    and descends; they touch and annihilate in a white flash and gamma rays; pair production spits out a gold and a blue
    bead; the gold one escapes, the blue one grows back into the anti-gold ingot (a flicker of inverted colour)."""
    # matter twin
    tw_s = sm(win(t, 0.18, 0.30)) * (1 - sm(win(t, 0.44, 0.50)))
    tw_y = 1.0 - 0.36 * sm(win(t, 0.24, 0.45))
    # anti-gold: annihilated at 0.5, reborn from a bead
    if t < 0.5:
        s = 1 - sm(win(t, 0.44, 0.50))
        ws = 0.0
    else:
        s = sm(win(t, 0.60, 0.80)) * 0.7 + 0.3 * sm(win(t, 0.54, 0.60))
        ws = 1 - sm(win(t, 0.62, 0.80))
    hot = sm(win(t, 0.40, 0.48)) if t < 0.5 else 0.0
    zoomk = sm(win(t, 0.16, 0.26)) * (1 - sm(win(t, 0.46, 0.56)))

    def shape(p):
        ing = _ing(p)
        if ws <= 0:
            return ing
        return ing * (1 - ws) + sd_sphere(p, 0.3) * ws

    def gloss_b(p, n):
        o, e = pat_bands(4, -1, t, 0.15, axis=0)(p, n)
        return o, np.maximum(e, hot)
    parts = [Part(scaled(shape, s), 0, pos=(0, -0.15 * zoomk, 0), rot=roty(0.1)),
             Part(scaled(_ing, tw_s * 0.9), 1, pos=(0, tw_y * 0.9 - 0.15 * zoomk + 0.0, 0), rot=rotz(PI) @ roty(0.1))]
    mats = [Material(c1, c2, metal=0.97, pattern=gloss_b, emissive=lerp3(H(c2), WHITE, hot)),
            Material('#e8a818', '#fff0a0', metal=1.0, emissive=lerp3(H('#ffe080'), WHITE, hot), pattern=_flat(hot))]
    # pair production beads
    if 0.5 < t < 0.72:
        x = win(t, 0.5, 0.72)
        a = 2.5 * x * TAU
        rr = 0.12 + 0.5 * x
        gold_pos = (math.cos(a) * rr, 0.15 + 0.35 * x, math.sin(a) * rr)
        parts.append(Part(scaled(lambda p: sd_sphere(p, 1.0), 0.13 * (1 - x) + 0.02), 1, pos=gold_pos))
    view = dict(view_pitch=0.6, view_yaw=0.7 + 0.15 * math.sin(TAU * t), zoom=1.05 - 0.3 * zoomk)

    def ov(img):
        cx, cy, _ = proj((0, -0.0, 0), view)
        fl = math.exp(-((t - 0.5) / 0.025) ** 2)
        glow(img, cx, cy, 16, WHITE, fl)
        if 0.48 < t < 0.62:
            x = win(t, 0.48, 0.62)
            rays(img, cx, cy, 3 + 10 * x, 7 + 12 * x, 8, H('#e0f0ff'), 1 - x, phase=0.2)
            ring(img, cx, cy, 2 + 14 * x, WHITE, (1 - x) * 0.8)
        if 0.5 < t < 0.72:                              # the escaping gold bead's trail
            x = win(t, 0.5, 0.72)
            for j in range(5):
                xx = max(0.0, x - j * 0.03)
                a = 2.5 * xx * TAU
                rr = 0.12 + 0.5 * xx
                px, py, pz = proj((math.cos(a) * rr, 0.15 + 0.35 * xx, math.sin(a) * rr), view)
                dot(img, px, py, H('#ffd060'), (1 - j / 5) * (1 - x), behind=pz > 0)
        # reborn: inverted-colour flicker (it *is* the negative of gold)
        if 0.8 < t < 0.86 and int(round(t * 60)) % 2 == 1:
            invert(img, 0.85)
        if t < 0.18 or t > 0.86:
            anim.ov_sparkles(img, (t * 2) % 1.0, 31, H(c2), 3)
    return parts, mats, view, ov


# =========================================================================================== OMNISCIENCE CATALYST
def omniscience_catalyst(c1, c2, t):
    """All nine singularities fused: nine coloured singularities orbit a closed eye, spiral in and fuse in a flash; the eye
    opens on an iris of every colour and looks around, knowing everything - briefly - then closes, and the nine fly out."""
    if t < 0.5:
        R = 0.88 - 0.8 * sm(win(t, 0.04, 0.30))
        rs = 0.095 * (1 - sm(win(t, 0.22, 0.31)))
    else:
        R = 0.08 + 0.8 * sm(win(t, 0.86, 1.0))
        rs = 0.095 * sm(win(t, 0.86, 0.93))
    o = 0.3 + 0.7 * sm(win(t, 0.31, 0.39)) * (1 - sm(win(t, 0.80, 0.86)))
    spin = TAU * 2 * t + 2 * TAU * sm(win(t, 0.04, 0.30))
    almond = lambda p: np.maximum(sd_sphere((p - np.array([0, -0.5, 0])) / np.array([1, 1, 0.5]), 0.88),
                                  sd_sphere((p + np.array([0, -0.5, 0])) / np.array([1, 1, 0.5]), 0.88)) * 0.5
    sc = np.array([1.0, o, 1.0])
    parts = [Part(lambda p: almond(p / sc) * min(1.0, o + 0.2), 0),
             Part(lambda p: almond(p / (sc * np.array([1.12, 1.25, 1.0]))) * 0.9, 1, pos=(0, 0, 0.08))]
    mats = [Material('#f0f0f8', '#ffffff', metal=0.2, gloss=1.4),
            Material('#0a0a10', '#806080', metal=0.3, keep_dark=True)]
    for k in range(9):
        a = TAU * k / 9 + spin
        mats.append(Material(hsv(k / 9, 0.85, 1.0), '#ffffff', emissive=hsv(k / 9, 0.7, 1.0), pattern=pat_pulse_core(t)))
        if rs > 0.01:
            parts.append(Part(lambda p, r=rs: sd_sphere(p, r), 2 + k, pos=(math.cos(a) * R, math.sin(a) * R * 0.3 + 0.05, math.sin(a) * R)))
    view = dict(view_pitch=0.0, view_yaw=0.0, zoom=1.0)
    look = (0.0, 0.0)
    if 0.42 < t < 0.78:
        x = win(t, 0.42, 0.78)
        look = (2.2 * math.sin(TAU * x), -1.0 * math.sin(TAU * x * 2))

    def ov(img):
        c = 15.5
        lum = img[..., :3].mean(-1)
        white = opaque(img) & (lum > 0.45)
        ix, iy = c + look[0], c + look[1]
        r = np.hypot(XX - ix, YY - iy)
        a = np.arctan2(YY - iy, XX - ix)
        iris = white & (r < 6.6)
        hue = (a / TAU + t * 3 + r * 0.04) % 1.0
        hues = np.stack([hsv(h, 0.85, 1.0) for h in np.linspace(0, 1, 48, endpoint=False)])
        col = hues[(hue * 48).astype(int) % 48]
        img[..., :3] = np.where(iris[..., None], col, img[..., :3])
        pupil = iris & (r < 2.4 + 0.6 * math.sin(TAU * t * 4))
        img[..., :3] = np.where(pupil[..., None], 0.02, img[..., :3])
        if o > 0.9:
            dot(img, ix - 1.5, iy - 2, WHITE, 1.0)
        # orbit trails of the nine
        if rs > 0.02:
            for k in range(9):
                for j in range(1, 3):
                    aa = TAU * k / 9 + spin - 0.22 * j
                    x, y, z = proj((math.cos(aa) * R, math.sin(aa) * R * 0.3 + 0.05, math.sin(aa) * R), view)
                    dot(img, x, y, hsv(k / 9, 0.8, 1.0), 0.5 - 0.18 * j, behind=z > 0)
        fl = math.exp(-((t - 0.315) / 0.025) ** 2)
        glow(img, c, c, 15, WHITE, fl)
        star(img, c, c, 14, WHITE, fl, diag=0.6)
        if 0.4 < t < 0.8:                              # knowing everything: rays of every colour
            k = bump(t, 0.4, 0.8)
            for i in range(12):
                aa = TAU * i / 12 + t * 2
                for rr in (11, 12.5, 14):
                    dot(img, c + math.cos(aa) * rr, c + math.sin(aa) * rr * 0.8, hsv(i / 12 + t, 0.7, 1.0), k * 0.75 * (1 - (rr - 11) / 5), behind=True)
        if 0.8 < t < 0.86:                              # the blink of forgetting
            outline_flash(img, hsv(t * 5, 0.5, 1.0), bump(t, 0.8, 0.86))
    return parts, mats, view, ov


# =========================================================================================== ETERNITY CATALYST
def eternity_catalyst(c1, c2, t):
    """Time flows through it without passing: the hourglass runs, flips over (and nothing changes - neither bulb empties),
    then melts into a spinning golden ring of eternity with a grain of sand racing round it, and pours back into the glass."""
    phi = PI * sm(win(t, 0.18, 0.34))
    w = sm(win(t, 0.50, 0.62)) * (1 - sm(win(t, 0.82, 0.94)))
    R = rotz(phi)
    fs = 1 - w

    def glass(p):
        bul = np.minimum(sd_sphere((p - np.array([0, 0.36, 0])) / np.array([1, 1.05, 1]), 0.34),
                         sd_sphere((p + np.array([0, 0.36, 0])) / np.array([1, 1.05, 1]), 0.34))
        hg = np.minimum(bul, sd_cyl(p, 0.07, 0.12))
        if w <= 0:
            return hg
        tor = sd_torus(rotx(PI / 2) @ np.zeros(3) + p @ rotx(PI / 2), 0.56, 0.09)
        return hg * (1 - w) + tor * w

    def sand(p, n):
        y = p @ R[1]                                   # sand obeys world gravity, whichever way up the glass is
        ly = p[..., 1]
        top = (ly * np.sign(math.cos(phi) + 1e-9)) > 0
        filled = np.where(top, (y > 0.06) & (y < 0.3), y < -0.5) if abs(math.cos(phi)) > 0.5 else np.zeros(y.shape, bool)
        band = np.sin(np.arctan2(p[..., 1], p[..., 0]) * 6 - t * TAU * 6) > 0.3
        return 0.15 * filled, np.maximum(filled.astype(float) * (1 - w), (band * w * 0.9 + w * 0.5) * (w > 0.4))
    pill = [(np.array([math.cos(a) * 0.4, -0.76, math.sin(a) * 0.4]), np.array([math.cos(a) * 0.4, 0.76, math.sin(a) * 0.4]))
            for a in (PI / 4, 3 * PI / 4, 5 * PI / 4, 7 * PI / 4)]
    frame = lambda p: np.minimum(np.minimum(sd_cyl(p - np.array([0, 0.78, 0]), 0.44, 0.045, 0.02),
                                            sd_cyl(p + np.array([0, 0.78, 0]), 0.44, 0.045, 0.02)), _segs(p, pill, 0.04))
    parts = [Part(glass, 0, rot=R), Part(scaled(frame, fs), 1, rot=R @ roty(TAU * t))]
    mats = [Material(lerp3(H('#d8e0f0'), H('#e0b040'), w), '#ffffff', metal=0.3 + 0.6 * w, gloss=1.8, emissive=c2, pattern=sand),
            Material('#c0a050', c2, metal=1.0, emissive=c2, pattern=pat_bands(6, 1, t, 0.08, axis=2))]
    view = dict(view_pitch=0.2, view_yaw=0.5, zoom=1.0)

    def ov(img):
        cx, cy, _ = proj((0, 0, 0), view)
        gold = H(c2)
        if w < 0.05 and abs(math.cos(phi)) > 0.99:     # the endless stream through the neck
            for y in range(16, 26):
                on = ((y - t * 60) % 3) < 1.5
                dot(img, 15.5, y, gold if on else WHITE, 0.9 if on else 0.5)
        if 0.18 < t < 0.42:                            # the flip sends out a ring of time
            x = win(t, 0.22, 0.42)
            ring(img, cx, cy, 4 + 13 * x, gold, (1 - x) * bump(t, 0.18, 0.42), sy=0.45)
            outline_flash(img, gold, 0.6 * bump(t, 0.2, 0.32))
        if w > 0.5:                                    # a grain racing round the ring of eternity
            for j in range(8):
                a = TAU * (t * 4) - j * 0.22
                dot(img, cx + math.cos(a) * 10, cy + math.sin(a) * 10, WHITE if j == 0 else gold, (1 - j / 8) * (w - 0.5) * 2)
        mt = math.exp(-((t - 0.56) / 0.02) ** 2) + math.exp(-((t - 0.88) / 0.02) ** 2)
        glow(img, cx, cy, 13, gold, 0.5 * mt)
        if t > 0.94 or t < 0.16:
            anim.ov_sparkles(img, (t * 2) % 1.0, 21, gold, 3)
    return parts, mats, view, ov


# =========================================================================================== TRANSCENDENTAL CRYSTAL
_GLY = {}


def _glyph(ch, size):
    key = (ch, size)
    if key not in _GLY:
        from PIL import Image, ImageDraw, ImageFont
        big = N * 4
        im = Image.new('L', (big, big), 0)
        d = ImageDraw.Draw(im)
        font = ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf', size * 4)
        l, tp, r, b = d.textbbox((0, 0), ch, font=font)
        d.text(((big - (r - l)) / 2 - l, (big - (b - tp)) / 2 - tp), ch, font=font, fill=255)
        _GLY[key] = np.asarray(im.resize((N, N), Image.BOX)) / 255.0
    return _GLY[key]


def transcendental_crystal(c1, c2, t):
    """pi and e, which no polynomial can reach: a dark-gold crystal shows a burning pi while motes run a perfect circle;
    a polynomial curve reaches for it and falls short; the crystal flips like a coin and becomes e, its motes now
    streaming out along a logarithmic spiral; another polynomial fails; it flips back to pi."""
    fl1 = win(t, 0.40, 0.50)
    fl2 = win(t, 0.90, 1.0)
    flip = fl1 if 0.40 < t < 0.5 else fl2
    sx = abs(math.cos(PI * flip))
    sx = max(sx, 0.06)
    ch = 'π' if (t < 0.45 or t >= 0.95) else 'e'
    ph = 1.0 if ch == 'π' else 0.0

    def gem(p):
        q = p / np.array([sx, 1, sx])
        return sd_gem(q, 0.36, 0.86) * sx

    def edges(p, n):
        ang = np.arctan2(p[..., 2], p[..., 0])
        sec = np.abs(np.mod(ang + PI / 6, PI / 3) - PI / 6)
        e = (sec > PI / 6 - 0.09) | (np.abs(np.abs(p[..., 1]) - 0.86 * 0.35 * 0 - 0.3) < 0.02)
        return 0.1 * np.sin(p[..., 1] * 20), e.astype(float) * (0.7 + 0.3 * math.sin(TAU * t * 4))
    parts = [Part(gem, 0, rot=rotz(0.1))]
    mats = [Material(c1, c2, metal=0.7, gloss=1.6, keep_dark=True, emissive=c2, pattern=edges)]
    mats[0].ramp[0:3] = np.array([[0.05, 0.04, 0.03], [0.12, 0.09, 0.04], [0.22, 0.16, 0.07]])
    view = dict(view_pitch=0.15, view_yaw=0.5 + TAU * t * 1, zoom=1.0)

    def ov(img):
        cx, cy = 15.5, 15.5
        gold = H(c2)
        # the glyph burns on the crystal, squashing with the flip
        m = _glyph(ch, 13)
        if sx < 0.999:
            cols = np.clip(np.round(cx + (np.arange(N) - cx) / sx).astype(int), 0, N - 1)
            inside = np.abs(np.arange(N) - cx) / sx < N / 2
            m = m[:, cols] * inside[None, :]
        m = m * opaque(img)
        k = 0.85 + 0.15 * math.sin(TAU * t * 2)
        paint(img, m * k, lerp3(gold, WHITE, 0.25), False)
        # pi: motes on a perfect circle / e: motes streaming out along a log spiral
        for i in range(6):
            if ch == 'π':
                a = TAU * (t * 2 + i / 6)
                x, y = cx + math.cos(a) * 13, cy + math.sin(a) * 4.5
                dot(img, x, y, gold, 0.9, behind=math.sin(a) < 0)
            else:
                life = (t * 4 + i / 6) % 1.0
                th = life * 2.5 * TAU
                r = 1.5 * math.exp(0.16 * th)
                dot(img, cx + math.cos(th + i) * r, cy + math.sin(th + i) * r * 0.6, gold, math.sin(PI * life), behind=True)
        # a polynomial reaches for it and falls short
        for (a0, a1, side) in ((0.12, 0.34, 1), (0.62, 0.84, -1)):
            if a0 < t < a1:
                pr = win(t, a0, a1 - 0.06)
                fade_k = 1 - win(t, a1 - 0.06, a1)
                for j in range(int(pr * 14)):
                    xx = j / 13.0
                    x = cx + side * (15 - xx * 9)
                    y = 29 - 22 * xx * xx
                    dot(img, x, y, H('#a0a0b0'), 0.6 * fade_k, behind=True)
                if pr >= 1:
                    dot(img, cx + side * 6, 7, H('#ff6040'), fade_k)
        flash = bump(t, 0.42, 0.48) + bump(t, 0.92, 0.98)
        star(img, cx, cy, 12, gold, flash, diag=0.4)
        outline_flash(img, gold, flash * 0.8)
    return parts, mats, view, ov


# =========================================================================================== GRAND UNIFIED CORE
GU_COLS = ['#ff4848', '#48ff70', '#4878ff']


def grand_unified_core(c1, c2, t):
    """Raise the energy and the forces unify: red, green and blue force rings spin up, tilt into line and fuse into one
    white ring, which contracts into the core; the core flares - then symmetry breaks and three coloured rings burst
    back out of it and settle."""
    if t < 0.62:
        c = sm(win(t, 0.22, 0.42))
        q = sm(win(t, 0.46, 0.57))
    else:
        c = 1 - sm(win(t, 0.66, 0.86))
        q = 1 - sm(win(t, 0.63, 0.82))
    rc = 0.22 + 0.1 * q + 0.04 * math.sin(TAU * t * 6) * q
    parts = [Part(lambda p: sd_sphere(p, rc), 0)]
    mats = [Material(lerp3(H(c1), H(c2), max(q, 0.6 * c)), c2, emissive=lerp3(H(c2), WHITE, q), pattern=pat_pulse_core(t))]
    extra = 3 * TAU * sm(win(t, 0.2, 0.5))
    for k, col in enumerate(GU_COLS):
        cc = lerp3(H(col), H('#ff90ff'), c)
        bands = pat_bands(10, 2, t, 0.3, axis=0)
        mats.append(Material(cc, '#ffffff', emissive=cc, pattern=lambda p, n, b=bands, c=c: (b(p, n)[0], np.maximum(b(p, n)[1], float(c > 0.7)))))
        Rk = (0.6 + 0.08 * k) * (1 - c) + 0.64 * c
        Rk = Rk * (1 - q) + 0.12 * q
        sp = (TAU * t * 2 + extra) * (1 if k % 2 else -1) * (1 - c) + (TAU * t * 2 + extra) * c
        R = rotx(0.35 + (1 - c) * (k - 1) * 0.9 + 0.75 * c) @ rotz((1 - c) * k * 1.0 + 0.3 * c) @ roty(sp)
        if q < 0.97:
            parts.append(Part(lambda p, rr=Rk, th=0.045 + 0.035 * c: sd_torus(p, rr, th), k + 1, rot=R))
    view = dict(view_pitch=0.3, view_yaw=0.5)

    def ov(img):
        cx, cy, _ = proj((0, 0, 0), view)
        if 0.2 < t < 0.48:                             # energy spirals in as the rings align
            anim.ov_infall(img, win(t, 0.2, 0.48), 9, lerp3(H(c2), WHITE, 0.4), 9)
        fl = math.exp(-((t - 0.6) / 0.03) ** 2)
        glow(img, cx, cy, 15, lerp3(H(c2), WHITE, 0.6), fl)
        if 0.56 < t < 0.7:
            x = win(t, 0.56, 0.7)
            rays(img, cx, cy, 6 + 4 * x, 10 + 6 * x, 12, H(c2), (1 - x), phase=t)
        if 0.63 < t < 0.8:                             # symmetry breaking: three coloured shock fronts
            x = win(t, 0.63, 0.8)
            for k, col in enumerate(GU_COLS):
                ring(img, cx, cy, 3 + 12 * x + k * 1.5, H(col), (1 - x) * 0.8, sy=0.6 + 0.2 * k)
        outline_flash(img, H(c2), 0.7 * c * (1 - q))
        if q > 0.9:
            outline_flash(img, WHITE, 0.6)
        if t < 0.18 or t > 0.88:
            anim.ov_sparkles(img, (t * 3) % 1.0, 17, H(c2), 3)
    return parts, mats, view, ov


# =========================================================================================== STELLAR CORE
def stellar_core(c1, c2, t):
    """A captive star: it boils inside two crossed containment hoops; the hoops swing open into one wide ring and the
    star swells, flaring and throwing prominences; then the hoops snap shut with a blue containment flash, squeezing the
    star, which wobbles back to size."""
    o = sm(win(t, 0.28, 0.44)) * (1 - sm(win(t, 0.56, 0.61)))
    if t < 0.56:
        r = 0.42 + 0.13 * sm(win(t, 0.3, 0.5))
    else:
        x = win(t, 0.56, 0.84)
        r = 0.42 + 0.13 * (1 - sm(win(t, 0.56, 0.6))) - 0.07 * math.exp(-x * 4) * math.cos(x * 14) * sm(win(t, 0.58, 0.6)) * (1 - x)
    r = r + 0.01 * math.sin(TAU * t * 4)
    hot = bump(t, 0.3, 0.6)

    def gran(p, n):
        cc = np.floor(p * 5)
        hh = _hs(cc[..., 0] * 7.1 + cc[..., 1] * 13.3 + cc[..., 2] * 3.7)
        boil = np.sin(TAU * (t * 3 + hh))
        f = p * 5 - cc - 0.5
        lane = (np.abs(f) > 0.42).sum(-1) >= 1
        return 0.3 * boil - 0.25 * lane, np.maximum(((~lane) | (boil > 0.5)).astype(float), hot)
    spin = TAU * t * 2 + 2 * TAU * sm(win(t, 0.3, 0.6))
    RA = 0.62 + 0.24 * o
    parts = [Part(lambda p: sd_sphere(p, r), 0),
             Part(lambda p: sd_torus(p, RA, 0.04), 1, rot=rotx(1.25 * (1 - o) + 0.3 * o) @ rotz(0.4 * (1 - o)) @ roty(spin)),
             Part(lambda p: sd_torus(p, RA - 0.06 * o, 0.04), 1, rot=rotz(1.25 * (1 - o)) @ rotx(0.3) @ roty(-spin))]
    snap = math.exp(-((t - 0.6) / 0.025) ** 2)
    mats = [Material(c1, c2, emissive=lerp3(mix(H(c1), H(c2), 0.55), H(c2), hot), pattern=gran),
            Material('#707888', '#ffffff', metal=1.0, emissive=lerp3(H('#80c0ff'), WHITE, snap), pattern=pat_bands(8, 1, t, 0.1 + 0.4 * snap, axis=0))]
    view = dict(view_pitch=0.3, view_yaw=0.5)

    def ov(img):
        cx, cy, _ = proj((0, 0, 0), view)
        n_prom = 2 if o < 0.5 else 4
        for k in range(n_prom):
            life = (t * 2 + k / n_prom) % 1.0
            a = TAU * (0.15 + k / n_prom + 0.1 * k)
            hh = math.sin(life * PI) * (3 + 5 * o)
            rr0 = r * 16 / 1.0
            for sgn in np.linspace(-0.5, 0.5, 7):
                rr = rr0 + hh * math.cos(sgn * PI)
                dot(img, cx + math.cos(a + sgn * 0.6) * rr, cy + math.sin(a + sgn * 0.6) * rr, H(c1), 0.85 * math.sin(life * PI), behind=True)
        if o > 0.2:
            glow(img, cx, cy, 15, H(c2), 0.3 * o)
            rays(img, cx, cy, 10, 13 + 2 * math.sin(TAU * t * 6), 8, H(c2), 0.6 * o, phase=t * 2)
        if snap > 0.05:
            outline_flash(img, H('#a0d8ff'), snap)
            ring(img, cx, cy, 9 + 6 * win(t, 0.58, 0.66), H('#a0d8ff'), snap, sy=0.5)
            glow(img, cx, cy, 13, H('#c0e8ff'), 0.5 * snap)
    return parts, mats, view, ov


# =========================================================================================== HORIZON SHARD
_rng2 = np.random.default_rng(41)
HZ_MEM = [tuple(v) for v in _rng2.normal(size=(9, 3))]


def horizon_shard(c1, c2, t):
    """A sliver of event horizon: sparks fall in and stay; then the sliver unfurls into the whole black horizon - a full
    sphere with a burning photon ring, every remembered spark glowing on its surface - and folds back into a shard."""
    g = sm(win(t, 0.28, 0.46)) * (1 - sm(win(t, 0.72, 0.88)))
    Rs = 0.5
    vrot = rotz(0.45) @ roty(0.25 * math.sin(TAU * t))
    view = dict(view_pitch=0.15, view_yaw=0.35)
    cam = rotx(-view['view_pitch']) @ roty(view['view_yaw'])
    vdir = np.array([0, 0, 1.0]) @ cam

    def msk(p, gg):
        sc = 1 + 6 * gg
        return (np.abs(p[..., 0]) / (0.26 * sc) + np.abs(p[..., 1]) / (0.66 * sc) - 1) * 0.22 * sc

    def shape(p):
        front = p[..., 2] - 1.5 * g
        shell = np.abs(sd_sphere(p - np.array([0, 0, 0.55]), 0.82 - 0.32 * g)) - 0.05
        shard = np.maximum(np.maximum(shell, msk(p, g)), front - 0.5)
        if g <= 0:
            return shard
        return shard * (1 - g) + sd_sphere(p, Rs) * g

    def rim(p, n):
        m = msk(p, g)
        a = np.arctan2(p[..., 1], p[..., 0])
        e = (m > -0.07) & (np.sin(a * 4 + TAU * t * 2) > -0.4) & (g < 0.6)
        limb = (np.abs(n @ vdir) < 0.35) & (g > 0.5)
        mem = np.zeros(m.shape, bool)
        for i, v in enumerate(HZ_MEM):
            v = np.asarray(v) / np.linalg.norm(v)
            mem |= ((p / np.maximum(length(p), 1e-6)[..., None]) @ v > 0.985) & (g > 0.7) & (math.sin(TAU * t * 3 + i) > -0.5)
        memo = (_hs(np.floor(p[..., 0] * 8) * 3.1 + np.floor(p[..., 1] * 8) * 7.3) > 0.9) & (m < -0.12) & (g < 0.4)
        return np.zeros(m.shape), (e | limb | mem | memo).astype(float)
    m0 = Material('#000000', c2, metal=0.0, gloss=0.6, emissive=c2, pattern=rim, keep_dark=True)
    m0.ramp[:] = np.array([[0.0, 0.0, 0.0], [0.01, 0.01, 0.02], [0.03, 0.03, 0.05], [0.06, 0.06, 0.09], [0.12, 0.1, 0.12], [0.3, 0.22, 0.15]])
    m1 = Material(c2, '#ffffff', emissive=c2, pattern=pat_bands(12, 2, t, 0.3, axis=0))
    parts = [Part(shape, 0, rot=vrot if g < 0.5 else rotz(0.45 * (1 - g)) @ roty(0.25 * math.sin(TAU * t) * (1 - g))),
             Part(scaled(lambda p: sd_torus(p, 1.0, 0.045), 0.78 * sm(win(g, 0.5, 1.0))), 1, rot=rotx(0.35) @ rotz(0.15))]

    def ov(img):
        cx, cy, _ = proj((0, 0, 0), view)
        anim.ov_infall(img, (t * 2) % 1.0, 31, H(c2), 7 + int(6 * g))
        fl = math.exp(-((t - 0.46) / 0.03) ** 2)
        outline_flash(img, H(c2), fl)
        glow(img, cx, cy, 15, H(c2), 0.35 * fl)
        if g > 0.6:                                     # light bent round the horizon
            for j in range(6):
                a = TAU * t * 2 - j * 0.15
                dot(img, cx + math.cos(a) * 11, cy + math.sin(a) * 11, WHITE if j == 0 else H(c2), (1 - j / 6) * (g - 0.6) * 2.5, behind=math.sin(a) < 0)
    return parts, [m0, m1], view, ov


# =========================================================================================== CRYSTALLISED LAW
def law_crystal(c1, c2, t):
    """A bubble of new law rises from the Primordial Ocean, swells and wobbles on the edge of popping - and freezes, from
    the bottom up, into a faceted crystal with a burst of frost; the crystal turns, shimmering in shifting hues, then
    thaws back into a little bubble that sinks into the ocean, and rises again."""
    th = sm(win(t, 0.80, 0.95))                        # thaw back into a bubble
    if t < 0.8:
        s = 0.32 + 0.38 * sm(win(t, 0.0, 0.16)) + 0.16 * sm(win(t, 0.16, 0.34))
        y0 = -0.5 * (1 - sm(win(t, 0.0, 0.18)))
    else:
        s = 0.86 - 0.54 * th
        y0 = -0.5 * sm(win(t, 0.84, 1.0))
    yf = -0.8 + 1.6 * sm(win(t, 0.36, 0.45))           # freeze front (object space, before scaling)
    frozen_all = 0.45 <= t < 0.8
    wob = 0.05 * bump(t, 0.12, 0.36) * (1 + math.sin(TAU * t * 9)) + 0.03 * bump(t, 0.8, 1.0)

    def shape(p):
        sph = sd_sphere(p, 0.62) + wob * np.sin(p[..., 0] * 7 + t * TAU * 5) * np.sin(p[..., 1] * 6)
        fac = np.maximum(sd_octa(p, 0.92), sd_box(p, (0.56, 0.56, 0.56)))
        if t < 0.36:
            return sph
        if t >= 0.8:
            return fac * (1 - th) + sph * th
        k = np.clip((yf - p[..., 1]) * 6, 0, 1)
        return sph * (1 - k) + fac * k

    def film(p, n):
        if t >= 0.8:
            frozen = np.full(p.shape[:-1], th < 0.5)
        else:
            frozen = (p[..., 1] < yf) if not frozen_all else np.ones(p.shape[:-1], bool)
        front = (np.abs(p[..., 1] - yf) < 0.05) & (0.36 < t < 0.45)
        iri = np.sin(p[..., 1] * 10 + p[..., 0] * 4 + t * TAU * 4) > 0.5
        frost = _hs(np.floor(p[..., 0] * 12) * 1.7 + np.floor(p[..., 1] * 12) * 5.3 + np.floor(p[..., 2] * 12) * 9.1) > 0.85
        return np.where(frozen, 0.15, -0.05), (front | (~frozen & iri) | (frozen & frost)).astype(float)
    parts = [Part(scaled(shape, s), 0, pos=(0, y0, 0), rot=roty(0.5 + TAU * 2 * sm(win(t, 0.45, 0.84))))]
    mats = [Material(c1, c2, metal=0.9, gloss=1.6, emissive=c2, pattern=film)]
    if 0.45 <= t < 0.8:
        anim.hue_shift_materials(mats, 70 * math.sin(TAU * win(t, 0.45, 0.8)))
    view = dict(view_pitch=0.3, view_yaw=0.5)

    def ov(img):
        cx, cy, _ = proj((0, y0, 0), view)
        iri_k = 0.55 * (1 - win(t, 0.33, 0.4)) if t < 0.5 else 0.55 * th
        if iri_k > 0:                                  # soap-film iridescence on the bubble
            hues = np.stack([hsv(h, 0.55, 1.0) for h in np.linspace(0, 1, 32, endpoint=False)])
            hm = ((XX * 0.04 + YY * 0.07 + t * 3) % 1.0 * 32).astype(int) % 32
            recolor(img, hues[hm], iri_k)
        ocean = 1 - win(t, 0.12, 0.25) if t < 0.5 else win(t, 0.86, 1.0)
        if ocean > 0:                                  # the surface of the ocean it rises from / sinks into
            for i in range(3):
                life = (t * 3 + i / 3) % 1.0
                dot(img, cx + (i - 1) * 4, cy + 6 + life * 8, H(c1), (1 - life) * ocean)
            ring(img, 15.5, 27, 9, H(c1), 0.6 * ocean, w=0.7, sy=0.22)
        fz = math.exp(-((t - 0.455) / 0.02) ** 2)
        outline_flash(img, WHITE, fz)
        star(img, cx, cy, 14, WHITE, fz, diag=0.6)
        if 0.45 < t < 0.6:
            x = win(t, 0.45, 0.6)
            for i in range(8):
                a = TAU * i / 8
                dot(img, cx + math.cos(a) * (7 + 8 * x), cy + math.sin(a) * (7 + 8 * x), H('#e0fff8'), 1 - x)
        if 0.5 < t < 0.8:
            anim.ov_sparkles(img, win(t, 0.5, 0.8) * 2 % 1.0, 77, H(c2), 4)
        if 0.8 < t < 0.92:                             # thaw: meltwater drips away
            x = win(t, 0.8, 0.92)
            for i in range(5):
                dot(img, cx - 6 + i * 3, cy + 5 + x * 8 + (i % 2) * 2, H(c1), (1 - x))
    return parts, mats, view, ov


# =========================================================================================== exports
ITEMS = {
    'omnium_ingot': omnium_ingot,
    'transcendent_metal_ingot': transcendent_metal_ingot,
    'computronium_ingot': computronium_ingot,
    'chronosteel_ingot': chronosteel_ingot,
    'hypernova_alloy_ingot': hypernova_alloy_ingot,
    'flerovium_ingot': flerovium_ingot,
    'antigold_ingot': antigold_ingot,
    'omniscience_catalyst': omniscience_catalyst,
    'eternity_catalyst': eternity_catalyst,
    'transcendental_crystal': transcendental_crystal,
    'grand_unified_core': grand_unified_core,
    'stellar_core': stellar_core,
    'horizon_shard': horizon_shard,
    'law_crystal': law_crystal,
}
RECIPES = {k: ((), ()) for k in ITEMS}
NARRATIVES = {}
FRAMES = {
    'omnium_ingot': 80, 'transcendent_metal_ingot': 80, 'computronium_ingot': 64, 'chronosteel_ingot': 72,
    'hypernova_alloy_ingot': 64, 'flerovium_ingot': 60, 'antigold_ingot': 60, 'omniscience_catalyst': 80,
    'eternity_catalyst': 64, 'transcendental_crystal': 64, 'grand_unified_core': 72, 'stellar_core': 72,
    'horizon_shard': 64, 'law_crystal': 64,
}
