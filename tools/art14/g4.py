"""Item art group 4 (round 14): the endgame relic set - Omnipotent Edge, Scepter of Singularities, the white-gold
armour of transcendence (crown, mantle, greaves, treads) and the Lawgiver's Codex.
Weapons are drawn diagonally like Minecraft tools (handle bottom-left, tip top-right); the armour pieces are front-view
Minecraft-style silhouettes in one white-gold set, each with its own accent light. Every loop has a 'power' moment and
lands back exactly where it started (t=0 == t=1)."""
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
    k = 1.70158
    return 1 + (k + 1) * (x - 1) ** 3 + k * (x - 1) ** 2


def hsv(h, s=1.0, v=1.0):
    return np.array(colorsys.hsv_to_rgb(h % 1.0, s, v))


def lerp3(a, b, k):
    return np.asarray(a, float) * (1 - k) + np.asarray(b, float) * k


def _void(p):
    return np.full(p.shape[:-1], 9.0)


def scaled(fn, s):
    if s < 0.015:
        return _void
    return lambda p: fn(p / s) * s


def _flat(v):
    return lambda p, n: (np.zeros(p.shape[:-1]), np.full(p.shape[:-1], float(v)))


def sd_boxframe(p, b, e):
    p = np.abs(p) - np.asarray(b)
    q = np.abs(p + e) - e
    def comb(a, bb, c):
        v = np.stack([a, bb, c], -1)
        return length(np.maximum(v, 0)) + np.minimum(np.maximum(a, np.maximum(bb, c)), 0)
    return np.minimum(np.minimum(comb(p[..., 0], q[..., 1], q[..., 2]), comb(q[..., 0], p[..., 1], q[..., 2])),
                      comb(q[..., 0], q[..., 1], p[..., 2]))


# white-gold set palette
WG = '#fff4d0'
GOLD = '#f0b838'
GOLD_HI = '#ffd860'


# =========================================================================================== 2D painting helpers
N = 32
YY, XX = np.mgrid[0:N, 0:N].astype(float)


def proj(pt, view):
    cam = rotx(-view.get('view_pitch', 0.5)) @ roty(view.get('view_yaw', 0.7853))
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


def opaque(img):
    return img[..., 3] > 0.5


def behind(img, A, rgb):
    """Paint only into empty pixels (things drawn behind the object)."""
    paint(img, np.asarray(A, float) * (img[..., 3] < 0.5), rgb)


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
    if k <= 0:
        return
    dx, dy = XX - cx, YY - cy
    A = np.clip(1 - np.abs(dx) / L, 0, 1) * (np.abs(dy) < 0.6) + np.clip(1 - np.abs(dy) / L, 0, 1) * (np.abs(dx) < 0.6)
    if diag:
        A = A + diag * np.clip(1 - np.abs(dx) / (L * 0.6), 0, 1) * (np.abs(np.abs(dx) - np.abs(dy)) < 0.6)
    paint(img, k * np.clip(A, 0, 1), rgb, True)


def dot(img, x, y, rgb, a=1.0, back_only=False):
    xi, yi = int(round(x)), int(round(y))
    if 0 <= xi < N and 0 <= yi < N:
        if back_only and img[yi, xi, 3] > 0.5:
            return
        anim._blend(img, yi, xi, np.asarray(rgb, float), cl(a))


def spark(img, x, y, rgb, s):
    if s <= 0:
        return
    arm = int(round(s * 3))
    for d in range(1, arm + 1):
        for ddx, ddy in ((d, 0), (-d, 0), (0, d), (0, -d)):
            dot(img, x + ddx, y + ddy, rgb, s * (1 - d / (arm + 1)))
    dot(img, x, y, WHITE, s)


def edge(img):
    op = opaque(img)
    inner = op.copy()
    for dy, dx in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        inner &= np.roll(np.roll(op, dy, 0), dx, 1)
    return op & ~inner


def outline_flash(img, rgb, k):
    if k > 0:
        paint(img, edge(img) * k, rgb)


def seg_dist(ax, ay, bx, by):
    """Pixel distance to a segment, and the position u in [0,1] along it."""
    vx, vy = bx - ax, by - ay
    u = np.clip(((XX - ax) * vx + (YY - ay) * vy) / max(vx * vx + vy * vy, 1e-6), 0, 1)
    return np.hypot(XX - ax - u * vx, YY - ay - u * vy), u


# =========================================================================================== OMNIPOTENT EDGE
SW_R = rotz(-PI / 4)
SW_C = np.array([0.0, 0.0, 0.0])
SW_L = 1.66            # blade length (local y from the guard)
SW_Y0 = -0.5           # local y of the guard


def _sw(fn, mat, ly, rot=None):
    return Part(fn, mat, pos=SW_R @ np.array([0.0, ly, 0.0]) + SW_C, rot=SW_R if rot is None else SW_R @ rot)


def _blade_sdf(p):
    x, y, z = np.abs(p[..., 0]), p[..., 1], p[..., 2]
    w, ht = 0.14, 0.36
    side = x - w * (1 - 0.12 * y / SW_L)
    tip = (x * ht + (y - SW_L) * w) / math.hypot(ht, w)
    base = -y
    thick = np.abs(z) + x * 0.25 - 0.045
    return np.maximum(np.maximum(side, tip), np.maximum(base, thick))


def omnipotent_edge(c1, c2, t):
    """A white-gold hilt holding a sliver of absolute light whose edges are a hairline of void. Idle, it hums; then a
    bead of light runs up both edges from the guard to the point, the point flares, and for a moment the blade is in
    several places at once (golden afterimages fan out around the hilt and fold back into the one true blade)."""
    run = win(t, 0.28, 0.50)                            # light running along the edge
    s_run = run * (SW_L + 0.2) - 0.1
    fan = bump(t, 0.50, 0.78)                           # afterimages
    hum = 0.5 + 0.5 * math.sin(TAU * t * 2)

    def blade_pat(p, n):
        x, y = np.abs(p[..., 0]), p[..., 1]
        wy = 0.14 * (1 - 0.12 * y / SW_L)
        rim = (x > wy - 0.05) | (x * 0.36 + (y - SW_L) * 0.14 > -0.05 * 0.386)
        bead = np.exp(-((y - s_run) / 0.13) ** 2) * (0 < run < 1)
        off = np.where(rim, -3.0 + 4.2 * bead, 0.45 + 0.2 * hum + 0.9 * bead + 0.4 * fan - 0.5 * x)
        em = np.where(rim, bead > 0.35, 1.0).astype(float)
        return off, em

    def grip_pat(p, n):
        v = np.sin(p[..., 1] * 60 + p[..., 0] * 30)
        return 0.12 * v, np.zeros(p.shape[:-1])

    mats = [
        Material('#fff8e0', c2, metal=0.9, gloss=1.4, emissive='#fff2c0', pattern=blade_pat),
        Material(GOLD_HI, '#ffffff', metal=0.95, gloss=1.4),
        Material('#f8f0dc', GOLD_HI, metal=0.4, pattern=grip_pat),
        Material('#ffffff', c2, emissive=lerp3(H('#ffe890'), WHITE, bump(t, 0.24, 0.34)), pattern=pat_all()),
    ]

    def guard(p):
        bar = sd_box(p, (0.30, 0.05, 0.06), 0.025)
        caps = sd_sphere(np.stack([np.abs(p[..., 0]) - 0.32, p[..., 1] - 0.02, p[..., 2]], -1), 0.075)
        mid = sd_box(p - np.array([0, -0.02, 0]), (0.1, 0.1, 0.075), 0.04)
        return np.minimum(np.minimum(bar, caps), mid)

    parts = [
        _sw(_blade_sdf, 0, SW_Y0 + 0.03),
        _sw(guard, 1, SW_Y0),
        _sw(lambda p: sd_cyl(p, 0.058, 0.2, 0.02), 2, SW_Y0 - 0.27),
        _sw(lambda p: sd_octa(p, 0.13), 1, SW_Y0 - 0.55),
        _sw(lambda p: sd_sphere(p, 0.065), 3, SW_Y0, rot=None),
    ]
    parts[-1].pos = parts[-1].pos + np.array([0.0, 0.0, -0.06])
    view = dict(view_pitch=0.0, view_yaw=0.0, zoom=1.0)

    def at(ly, lx=0.0):
        x, y, _ = proj(SW_R @ np.array([lx, ly, 0.0]) + SW_C, view)
        return x, y

    def ov(img):
        gx, gy = at(SW_Y0)
        tx, ty = at(SW_Y0 + SW_L + 0.03)
        # afterimages: the blade is in several places at once (ghost blades slide out sideways and fold back)
        if fan > 0:
            for d, kk in ((-4.5, 0.9), (4.5, 0.9), (-9.0, 0.5), (9.0, 0.5)):
                o = d * sm(cl(fan * 1.5)) / math.sqrt(2)
                d2, u = seg_dist(gx + o, gy + o, tx + o, ty + o)
                wpx = 1.25 * np.clip((1.04 - u) / 0.2, 0, 1) ** 0.6
                A = np.clip(wpx - d2, 0, 1) * (u > 0.12)
                core = np.clip(0.8 - d2, 0, 1) * (u > 0.12) * np.clip((1.0 - u) / 0.1, 0, 1)
                behind(img, A * kk * fan * 0.6, H(c2))
                paint(img, core * kk * fan * (img[..., 3] < 0.95), lerp3(H(c2), WHITE, 0.7))
        # the bead of light on both edges
        if 0 < run < 1:
            for side in (-1, 1):
                ly = SW_Y0 + 0.03 + s_run
                w = 0.125 if s_run < SW_L - 0.34 else max(0.0, 0.125 * (SW_L - s_run) / 0.34)
                x, y = at(ly, side * (w + 0.03))
                spark(img, x, y, H(c2), 0.55)
        # the point flares as the light leaves it
        fl = math.exp(-((t - 0.50) / 0.035) ** 2)
        star(img, tx, ty, 3 + 6 * fl, WHITE, fl, diag=0.5)
        glow(img, tx, ty, 6, H(c2), 0.8 * fl)
        outline_flash(img, H(GOLD_HI), 0.55 * fan)
        # guard gem wakes before the run
        g = bump(t, 0.20, 0.36)
        glow(img, gx, gy, 4, WHITE, 0.7 * g)
        # idle glint drifting down the blade
        if t > 0.8 or t < 0.08:
            x = ((t - 0.8) % 1.0) / 0.28
            px, py = at(SW_Y0 + SW_L * (0.9 - 0.7 * x), 0.0)
            spark(img, px, py, WHITE, 0.6 * math.sin(PI * cl(x)))
    return parts, mats, view, ov


# =========================================================================================== SCEPTER OF SINGULARITIES
SC_R = rotz(-PI / 4)
SC_C = np.array([-0.02, -0.02, 0.0])
SC_HEAD = 0.80          # local y of the held singularity
MODES = ['black hole', 'white hole', 'tesseract', 'entanglement', 'big bang']


def _sc(fn, mat, ly, lx=0.0, lz=0.0, rot=None):
    return Part(fn, mat, pos=SC_R @ np.array([lx, ly, lz]) + SC_C, rot=SC_R if rot is None else SC_R @ rot)


def singularity_scepter(c1, c2, t):
    """A violet-black scepter bound in gold whose three-pronged head holds a singularity that keeps deciding what it
    has always been: a black hole with a pink accretion ring, a white hole that bursts with light, a wireframe
    tesseract turning inside out, an entangled pair of motes, and a big bang - each born from a point, shown off,
    and folded back into a point for the next."""
    u = 5 * t + 0.5                    # t=0 (the still icon) sits in the middle of the black-hole mode
    k = int(u) % 5
    f = u - math.floor(u)
    grow = back(win(f, 0.0, 0.16))
    shrink = 1 - sm(win(f, 0.84, 1.0))
    s = grow * shrink
    power = bump(f, 0.30, 0.75)
    pink = H(c2)

    def shaft_pat(p, n):
        v = np.sin(p[..., 1] * 18 + np.arctan2(p[..., 2], p[..., 0]) * 2)
        return 0.06 * v, (np.abs(np.sin(p[..., 1] * 7 - t * TAU * 5)) > 0.985).astype(float) * 0.0

    mats = [
        Material('#3a2052', c2, metal=0.6, pattern=shaft_pat),
        Material(GOLD_HI, '#ffffff', metal=0.95, gloss=1.4),
        None,   # the singularity, per mode
        Material('#ffffff', c2, emissive=pink, pattern=pat_all()),
    ]

    def bands(p):
        d = sd_torus(p - np.array([0, 0.22, 0]), 0.066, 0.03)
        d = np.minimum(d, sd_torus(p - np.array([0, -0.25, 0]), 0.066, 0.03))
        d = np.minimum(d, sd_torus(p - np.array([0, -0.72, 0]), 0.066, 0.03))
        return d

    def prongs(p):
        d = sd_cyl(p - np.array([0, 0.47, 0]), 0.09, 0.07, 0.02)
        for i in range(3):
            a = TAU * i / 3 + 0.5
            ca, sa = math.cos(a), math.sin(a)
            d = np.minimum(d, sd_capsule(p, (0.05 * ca, 0.5, 0.05 * sa), (0.27 * ca, 0.74, 0.27 * sa), 0.035))
            d = np.minimum(d, sd_capsule(p, (0.27 * ca, 0.74, 0.27 * sa), (0.16 * ca, 1.02, 0.16 * sa), 0.03))
        return d

    parts = [
        _sc(lambda p: sd_cyl(p, 0.052, 0.8, 0.02), 0, -0.3),
        _sc(bands, 1, 0.0),
        _sc(prongs, 1, 0.0),
        _sc(lambda p: sd_octa(p, 0.14), 1, -1.13),
    ]
    head_ly = SC_HEAD
    hp = SC_R @ np.array([0.0, head_ly, 0.0]) + SC_C
    spin = TAU * t * 3

    if k == 0:      # black hole
        mats[2] = Material('#05020a', '#402060', metal=0.9, keep_dark=True)
        parts.append(Part(scaled(lambda p: sd_sphere(p, 0.2), s), 2, pos=hp))
        def disk_pat(p, n):
            a = np.arctan2(p[..., 2], p[..., 0])
            return 0.2 * np.sin(a * 3 - t * TAU * 15), np.ones(p.shape[:-1])
        mats.append(Material('#ff90c0', c2, emissive=lerp3(H('#ff9ad0'), WHITE, 0.4 * power), pattern=disk_pat))
        parts.append(Part(scaled(lambda p: sd_torus(p, 0.33, 0.03), s * (1 + 0.12 * power)), 4, pos=hp,
                          rot=rotz(0.75) @ rotx(-0.55)))
    elif k == 1:    # white hole
        mats[2] = Material('#ffffff', c2, emissive=lerp3(H('#fff0ff'), WHITE, power), pattern=pat_all())
        parts.append(Part(scaled(lambda p: sd_sphere(p, 0.24), s * (1 + 0.2 * power)), 2, pos=hp))
    elif k == 2:    # tesseract
        mats[2] = Material('#b070ff', c2, emissive=lerp3(H('#b080ff'), H('#ffd0ff'), power), pattern=pat_all())
        w = 0.5 + 0.5 * math.cos(TAU * win(f, 0.2, 0.8))           # inside-out: outer and inner cubes swap
        bo, bi = 0.09 + 0.17 * w, 0.26 - 0.17 * w

        def tess(p):
            return sd_octa(p, 0.09)
        parts.append(Part(scaled(tess, s * (0.6 + 0.6 * power)), 2, pos=hp, rot=roty(spin)))
    elif k == 3:    # entanglement
        mats[2] = Material('#ffffff', c2, emissive=WHITE, pattern=pat_all())
        mats.append(Material(pink, c2, emissive=pink, pattern=pat_all()))
        a = TAU * f * 2
        r = 0.22 * s
        off = np.array([math.cos(a) * r, math.sin(a) * r, -0.3])
        parts.append(Part(lambda p: sd_sphere(p, 0.11 * s + 1e-3), 2, pos=hp + off))
        parts.append(Part(lambda p: sd_sphere(p, 0.11 * s + 1e-3), 4, pos=hp - off + np.array([0, 0, -0.6])))
    else:           # big bang: a point that becomes a universe
        mats[2] = Material('#ffffff', c2, emissive=WHITE, pattern=pat_all())
        pr = 0.08 * (1 - sm(win(f, 0.25, 0.35))) * grow + 0.0
        parts.append(Part(scaled(lambda p: sd_sphere(p, 1.0), pr), 2, pos=hp))

    view = dict(view_pitch=0.0, view_yaw=0.0, zoom=1.0)

    def ov(img):
        hx, hy, _ = proj(hp, view)
        # every birth and every collapse is a little flash in the claws
        birth = math.exp(-((f - 0.02) / 0.04) ** 2) + math.exp(-((f - 1.02) / 0.04) ** 2)
        glow(img, hx, hy, 4, WHITE, 0.7 * birth)
        if k == 0:
            for i in range(6):
                life = (f * 2 + i / 6) % 1.0
                r = (1 - life) * 9 + 1
                a = TAU * i / 6 + life * 3
                dot(img, hx + math.cos(a) * r, hy + math.sin(a) * r * 0.8, pink, math.sin(PI * life) * s)
            glow(img, hx, hy, 6, H('#ff60c0'), 0.35 * power)
        elif k == 1:
            glow(img, hx, hy, 9, WHITE, 0.7 * power * s)
            for i in range(8):
                life = (f * 2.5 + i / 8) % 1.0
                a = TAU * i / 8 + 0.2
                r = 3 + life * 9
                dot(img, hx + math.cos(a) * r, hy + math.sin(a) * r, lerp3(WHITE, pink, life), (1 - life) * s)
            star(img, hx, hy, 8, WHITE, power * s, diag=0.4)
        elif k == 2:
            # a true 4D hypercube, rotating in the xw plane (it turns inside out), projected to 3D then to the icon
            ang = TAU * win(f, 0.1, 0.9) * 0.5 + 0.35
            ca, sa = math.cos(ang), math.sin(ang)
            V = np.array([[(i >> b & 1) * 2 - 1 for b in range(4)] for i in range(16)], float)
            x4 = V[:, 0] * ca - V[:, 3] * sa
            w4 = V[:, 0] * sa + V[:, 3] * ca
            V3 = np.stack([x4, V[:, 1], V[:, 2]], -1) * (1.0 / (2.6 - w4))[:, None] * 0.3 * s
            V3 = V3 @ (roty(spin * 0.25 + 0.5) @ rotx(0.5)).T
            P = [proj(hp + v, view) for v in V3]
            A = np.zeros((N, N))
            for i in range(16):
                for b in range(4):
                    j = i ^ (1 << b)
                    if j > i:
                        d, _ = seg_dist(P[i][0], P[i][1], P[j][0], P[j][1])
                        A = np.maximum(A, np.clip(1.0 - d, 0, 1) * (0.65 if b == 3 else 1.0))
            behind(img, A * 0.55, H('#2a0a40'))
            paint(img, A, lerp3(H('#c090ff'), H('#ffe0ff'), power), True)
            glow(img, hx, hy, 5, H('#d0a0ff'), 0.4 * power)
        elif k == 3:
            a = TAU * f * 2
            r = 0.22 * s
            p1 = proj(hp + np.array([math.cos(a) * r, math.sin(a) * r, 0]), view)
            p2 = proj(hp - np.array([math.cos(a) * r, math.sin(a) * r, 0]), view)
            d, uu = seg_dist(p1[0], p1[1], p2[0], p2[1])
            wav = 0.5 + 0.5 * np.sin(uu * 12 - f * TAU * 6)
            paint(img, np.clip(1.0 - d, 0, 1) * (0.4 + 0.6 * wav) * s, lerp3(pink, WHITE, 0.5), True)
            if power > 0.6:
                spark(img, p1[0], p1[1], WHITE, (power - 0.6) * 2)
                spark(img, p2[0], p2[1], pink, (power - 0.6) * 2)
        else:
            x = win(f, 0.25, 0.85)
            if 0 < x < 1:
                fl = math.exp(-((f - 0.3) / 0.06) ** 2)
                glow(img, hx, hy, 4 + 14 * x, WHITE, fl)
                star(img, hx, hy, 4 + 9 * x, WHITE, (1 - x), diag=0.6)
                ring(img, hx, hy, 2 + x * 12, hsv(x * 0.8 + 0.8, 0.4, 1.0), (1 - x) * 0.9)
                for i in range(10):
                    a = TAU * i / 10 + 0.3
                    r = 2 + x * 11 * (0.7 + 0.3 * math.sin(i * 2.3))
                    dot(img, hx + math.cos(a) * r, hy + math.sin(a) * r, hsv(i / 10, 0.45, 1.0), (1 - x) * 1.2)
            if f < 0.3:             # the seed point twinkles before it bangs
                spark(img, hx, hy, pink, 0.4 + 0.4 * math.sin(f * TAU * 6))
            if f > 0.8:             # the newborn universe collapses into the next black hole
                x = win(f, 0.8, 1.0)
                for i in range(6):
                    a = TAU * i / 6 + x * 2
                    r = (1 - x) * 8
                    dot(img, hx + math.cos(a) * r, hy + math.sin(a) * r, pink, 0.8)
        # a mote of each mode's colour climbs the shaft during the power moment
        if power > 0:
            ly = -1.0 + 1.8 * win(f, 0.30, 0.75)
            x, y, _ = proj(SC_R @ np.array([0, ly, 0]) + SC_C, view)
            dot(img, x, y, pink, power * 0.9)
    return parts, mats, view, ov


# =========================================================================================== shared armour bits
ARM_VIEW = dict(view_pitch=0.16, view_yaw=0.28, zoom=1.0)


def wg_mat(c2, pattern=None, emissive=None):
    return Material(WG, c2, metal=0.85, gloss=1.3, pattern=pattern, emissive=emissive)


def gold_mat(pattern=None, emissive=None):
    return Material(GOLD, '#fff0b0', metal=0.95, gloss=1.4, pattern=pattern, emissive=emissive)


def shine_sweep(img, t0, t1, t, rgb=WHITE, k=0.8, width=2.5):
    """A diagonal sheen that crosses the whole piece once between t0 and t1."""
    x = win(t, t0, t1)
    if not 0 < x < 1:
        return
    pos = -8 + x * 56
    d = np.abs((XX + YY) * 0.7 - pos)
    paint(img, opaque(img) * np.clip(1 - d / width, 0, 1) * k * math.sin(PI * x), rgb, True)


# =========================================================================================== CROWN OF TRANSCENDENCE
def crown_of_transcendence(c1, c2, t):
    """A white-gold helm crowned with a ring of gold points and a single eye-gem at the brow, under a floating halo
    that slowly turns (a bright arc runs around it). Once per loop the eye opens wide and blazes: the halo flares,
    light rays fall from it and the whole helm is outlined in light; then the eye half-closes again."""
    HC = np.array([0.0, -0.22, 0.0])
    power = bump(t, 0.45, 0.75)
    blink = 1 - 0.85 * bump(t, 0.12, 0.2)                 # one slow blink before it opens
    eye_open = (0.55 + 0.45 * sm(win(t, 0.40, 0.5)) * (1 - sm(win(t, 0.72, 0.85)))) * blink

    def shell(p):
        q = p / np.array([1.15, 0.8, 1.0])
        q = np.stack([q[..., 0], np.maximum(q[..., 1], 0), q[..., 2]], -1)
        d = (np.abs(length(q) - 0.6) - 0.075) * 0.8
        d = np.maximum(d, -(p[..., 1] + 0.3))
        d = np.maximum(d, -sd_box(p - np.array([0, -0.36, -0.5]), (0.36, 0.38, 0.5), 0.04))
        return d

    def helm_pat(p, n):
        q = p / np.array([1.15, 0.8, 1.0])
        q = np.stack([q[..., 0], np.maximum(q[..., 1], 0), q[..., 2]], -1)
        inner = length(q) < 0.6
        v = (np.abs(p[..., 1] + 0.02) < 0.035) | (np.abs(p[..., 0]) < 0.03) & (p[..., 1] > 0.1)
        return np.where(inner, -1.6, np.where(v, -0.25, 0.0)), np.zeros(p.shape[:-1])

    def crown(p):
        r = np.sqrt(p[..., 0] ** 2 + p[..., 2] ** 2)
        a = np.arctan2(p[..., 2], p[..., 0])
        tri = np.abs(((a * 7 / TAU + 0.25) % 1.0) - 0.5) * 2         # 0 at a point, 1 between points
        top = 0.08 + 0.16 * (1 - tri) ** 1.5
        band = np.maximum(np.abs(r - 0.6) - 0.05, np.maximum(-0.02 - p[..., 1], p[..., 1] - top))
        return band * 0.8

    def crown_pat(p, n):
        tips = (p[..., 1] > 0.17)
        return np.where(tips, 0.3, 0.0), tips * (0.3 + 0.7 * power)

    def halo_pat(p, n):
        a = np.arctan2(p[..., 2], p[..., 0])
        arc = np.cos(a - TAU * t * 2) * 0.5 + 0.5
        return 0.5 * arc ** 6 + 0.4 * power, np.ones(p.shape[:-1])

    def eye_pat(p, n):
        x, y = p[..., 0], p[..., 1] / max(eye_open, 0.05)
        r = np.hypot(x, y)
        pupil = r < 0.04 + 0.015 * power
        iris = (r < 0.09) & ~pupil
        off = np.where(pupil, -2.5, np.where(iris, -0.1, 0.6))
        return off, np.where(pupil, 0.0, 1.0)

    mats = [
        wg_mat(c2, pattern=helm_pat),
        gold_mat(pattern=crown_pat, emissive=H('#fff0a0')),
        Material('#fff6d8', GOLD_HI, emissive=lerp3(H('#fff2b8'), WHITE, power), pattern=halo_pat),
        Material('#ffffff', GOLD_HI, emissive=lerp3(H('#ffe9a0'), WHITE, 0.5 + 0.5 * power), pattern=eye_pat),
    ]
    hy = 0.66 + 0.03 * math.sin(TAU * t)
    tilt = 0.55 + 0.08 * math.sin(TAU * t)
    eh = 0.095 * eye_open + 1e-3
    parts = [
        Part(shell, 0, pos=HC),
        Part(crown, 1, pos=HC + np.array([0, 0.22, 0])),
        Part(lambda p: sd_torus(p, 0.38, 0.05 + 0.012 * power), 2, pos=(0, hy, 0), rot=rotx(tilt)),
        Part(lambda p: sd_box(p, (0.15, eh, 0.05), min(eh * 0.9, 0.09)), 3, pos=HC + np.array([0, 0.27, -0.7]), rot=rotx(-0.15)),
    ]
    view = dict(ARM_VIEW)

    def ov(img):
        ex, ey, _ = proj(HC + np.array([0, 0.27, -0.74]), view)
        hx, hy2, _ = proj((0, hy, 0), view)
        # the halo's light falls on the crown
        if power > 0:
            for i in range(7):
                a = (i - 3) * 0.32
                x0 = hx + math.sin(a) * 9
                for j in range(5):
                    yy = hy2 + 2 + j * 2.2 + (t * 40 % 2.2)
                    dot(img, x0 + math.sin(a) * j * 0.8, yy, H('#fff0b0'), power * 0.45 * (1 - j / 5), back_only=True)
        glow(img, ex, ey, 5 + 3 * power, WHITE, 0.25 + 0.6 * power)
        star(img, ex, ey, 3 + 8 * power, WHITE, power ** 1.5, diag=0.5)
        outline_flash(img, H('#fff4c0'), 0.7 * bump(t, 0.5, 0.7))
        # a spark rides the halo where its bright arc is
        a = TAU * t * 2
        px, py, pz = proj(np.array([0, hy, 0]) + rotx(tilt) @ np.array([math.cos(a) * 0.38, 0, math.sin(a) * 0.38]), view)
        spark(img, px, py, H('#fff0b0'), 0.35 + 0.4 * power if pz < 0 else 0.0)
        shine_sweep(img, 0.82, 0.98, t, k=0.5)
    return parts, mats, view, ov


# =========================================================================================== MANTLE OF OMNIPOTENCE
def mantle_of_omnipotence(c1, c2, t):
    """A radiant white-gold breastplate with gold pauldrons and belt and a sun emblem on the chest, standing in a soft
    prismatic cape-glow that ripples behind it. The sun turns slowly; once per loop it rises to full power - the
    emblem blazes, rays burst outward and the cape-light surges into a full rainbow - then settles."""
    power = bump(t, 0.40, 0.72)
    spin = TAU * t

    def body(p):
        torso = sd_box(p - np.array([0, -0.14, 0]), (0.40, 0.58, 0.19), 0.1)
        yoke = sd_box(p - np.array([0, 0.46, 0]), (0.80, 0.2, 0.21), 0.08)
        arms = sd_box(np.stack([np.abs(p[..., 0]) - 0.68, p[..., 1] - 0.16, p[..., 2]], -1), (0.15, 0.40, 0.19), 0.06)
        d = np.minimum(np.minimum(torso, yoke), arms)
        neck = sd_box((p - np.array([0, 0.74, 0])) @ rotz(PI / 4), (0.24, 0.24, 0.5))
        return np.maximum(d, -neck)

    def body_pat(p, n):
        ridge = np.abs(p[..., 0]) < 0.025
        rib = (np.abs(np.abs(p[..., 1] + 0.14) - 0.3) < 0.025) & (np.abs(p[..., 0]) < 0.38)
        return np.where(ridge | rib, -0.22, 0.0) + 0.1 * np.cos(p[..., 0] * 3), np.zeros(p.shape[:-1])

    def trims(p):
        paul = sd_box(np.stack([np.abs(p[..., 0]) - 0.68, p[..., 1] - 0.5, p[..., 2]], -1), (0.19, 0.17, 0.235), 0.08)
        belt = sd_box(p - np.array([0, -0.67, 0]), (0.43, 0.07, 0.225), 0.03)
        collar = np.maximum(sd_box((p - np.array([0, 0.74, 0])) @ rotz(PI / 4), (0.29, 0.29, 0.235), 0.02),
                            -sd_box((p - np.array([0, 0.74, 0])) @ rotz(PI / 4), (0.24, 0.24, 0.6)))
        collar = np.maximum(collar, 0.42 - p[..., 1])
        return np.minimum(np.minimum(paul, belt), collar)

    def sun_pat(p, n):
        r = np.hypot(p[..., 0], p[..., 2])
        a = np.arctan2(p[..., 2], p[..., 0])
        rays = np.cos(a * 8 + spin) > 0.3
        core = r < 0.09
        return np.where(core, 0.6, np.where(rays, 0.3, -0.6)), np.where(core | rays, 1.0, 0.0)

    sun_col = lerp3(H(GOLD_HI), WHITE, 0.6 * power)
    mats = [
        wg_mat(c2, pattern=body_pat),
        gold_mat(),
        Material(GOLD_HI, '#ffffff', emissive=sun_col, pattern=sun_pat, metal=0.9),
    ]
    sr = 0.16 + 0.03 * power
    parts = [
        Part(body, 0),
        Part(trims, 1),
        Part(lambda p: sd_cyl(p, sr, 0.03, 0.015), 2, pos=(0, 0.08, -0.2), rot=rotx(PI / 2)),
    ]
    view = dict(ARM_VIEW)
    view['view_yaw'] = 0.22

    def ov(img):
        sx, sy, _ = proj((0, 0.08, -0.23), view)
        # prismatic cape-glow rippling behind the mantle
        top = 6.0
        rip = np.sin(YY * 0.55 - t * TAU * 3 + XX * 0.2)
        cx = 15.5
        half = 12.5 + (YY - top) * 0.16 + rip * (0.9 + 1.2 * power)
        inside = (YY > top) & (np.abs(XX - cx) < half) & (YY < 31)
        hue = (YY / 30.0 + XX / 60.0 - t * 2) % 1.0
        rgb = np.stack([hsv(h, 0.45, 1.0) for h in np.linspace(0, 1, 32, endpoint=False)])[(hue * 32).astype(int) % 32]
        rgb = lerp3(H('#fff0c8'), rgb, 0.35 + 0.55 * power)
        k = (0.35 + 0.35 * power) * (0.6 + 0.4 * (rip > 0)) * np.clip((half - np.abs(XX - cx)) / 2.0, 0, 1)
        behind(img, inside * k, rgb)
        # the sun at full power
        glow(img, sx, sy, 5 + 4 * power, H('#fff4b0'), 0.15 + 0.6 * power)
        if power > 0:
            for i in range(8):
                a = TAU * i / 8 + spin * 0.5
                for r in (4.5, 6.0, 7.5):
                    dot(img, sx + math.cos(a) * r * (0.6 + 0.6 * power), sy + math.sin(a) * r * (0.6 + 0.6 * power),
                        H('#fff2b0'), power * (1.2 - r / 8))
        star(img, sx, sy, 3 + 7 * power, WHITE, power ** 1.5)
        outline_flash(img, H('#fff4c0'), 0.6 * bump(t, 0.48, 0.66))
        shine_sweep(img, 0.80, 0.98, t, k=0.55)
    return parts, mats, view, ov


# =========================================================================================== GREAVES OF THE ABSOLUTE
def greaves_of_the_absolute(c1, c2, t):
    """White-gold greaves with a gold belt and knee-guards; cyan light-lines run down each leg like distance being
    folded. Once per loop the distance folds: the legs telescope shut (the lower legs slide up into the knees in a
    cyan flash, leaving afterimages of their full length) and snap back out."""
    fold = sm(win(t, 0.40, 0.50)) * (1 - sm(win(t, 0.58, 0.70)))
    power = bump(t, 0.38, 0.72)
    cyan = H(c2)
    L = 0.6 * (1 - 0.32 * fold)       # half length of a leg

    def leg_pat(p, n):
        x, y = p[..., 0], p[..., 1]
        lane = (np.abs(np.abs(x) - 0.09) < 0.038)
        flow = np.sin(y * 9 + t * TAU * 6)
        lines = lane & (flow > 0.2 - 0.9 * power)
        seam = np.abs(y - 0.0) < 0.02
        return np.where(lines, 0.5, np.where(seam, -0.2, 0.0)), lines.astype(float)

    mats = [
        wg_mat(c2, pattern=leg_pat, emissive=lerp3(cyan, WHITE, 0.4 * power)),
        gold_mat(),
        wg_mat(c2),
    ]
    top = 0.36

    def hips(p):
        d = sd_box(p - np.array([0, 0.40, 0]), (0.53, 0.17, 0.21), 0.06)
        return d

    def belt(p):
        d = sd_box(p - np.array([0, 0.62, 0]), (0.56, 0.075, 0.23), 0.03)
        knees = sd_box(np.stack([np.abs(p[..., 0]) - 0.31, p[..., 1] - (top - 2 * L * 0.42), p[..., 2] + 0.03], -1),
                       (0.2, 0.075, 0.2), 0.04)
        cuffs = sd_box(np.stack([np.abs(p[..., 0]) - 0.31, p[..., 1] - (top - 2 * L + 0.05), p[..., 2]], -1),
                       (0.235, 0.05, 0.225), 0.02)
        return np.minimum(np.minimum(d, knees), cuffs)

    parts = [
        Part(hips, 2),
        Part(belt, 1),
        Part(lambda p: sd_box(p, (0.21, L, 0.2), 0.05), 0, pos=(-0.31, top - L, 0)),
        Part(lambda p: sd_box(p, (0.21, L, 0.2), 0.05), 0, pos=(0.31, top - L, 0)),
    ]
    view = dict(ARM_VIEW)
    view['view_yaw'] = 0.22

    def ov(img):
        # afterimages of the full-length legs while the distance is folded
        if fold > 0.02:
            for sx in (-0.31, 0.31):
                a0 = proj((sx, top - 2 * L, -0.2), view)
                a1 = proj((sx, top - 1.2, -0.2), view)
                for j in range(int(round(a0[1])), int(round(a1[1])) + 1):
                    for dx in (-3, 3):
                        dot(img, a0[0] + dx, j, cyan, fold * 0.7 * (1 - (j - a0[1]) / max(1, a1[1] - a0[1]) * 0.5),
                            back_only=True)
                    if (j + int(t * 64)) % 3 == 0:
                        dot(img, a0[0], j, WHITE, fold * 0.8, back_only=True)
        # horizontal fold-lines flash across the legs
        k = bump(t, 0.40, 0.56)
        if k > 0:
            for sx in (-0.31, 0.31):
                for yy in (0.0, -0.3):
                    x, y, _ = proj((sx, top + yy - 0.2, -0.2), view)
                    d, _ = seg_dist(x - 5, y, x + 5, y)
                    paint(img, np.clip(1 - d, 0, 1) * k, lerp3(cyan, WHITE, 0.5), True)
        outline_flash(img, cyan, 0.6 * bump(t, 0.45, 0.7))
        # sparks at the cuffs when the legs snap back out
        sn = bump(t, 0.6, 0.76)
        for sx in (-0.31, 0.31):
            x, y, _ = proj((sx, top - 2 * L, -0.2), view)
            spark(img, x, y + 1, cyan, sn)
        shine_sweep(img, 0.80, 0.98, t, k=0.5)
    return parts, mats, view, ov


# =========================================================================================== TREADS OF INFINITY
def _lem(p, a=0.15):
    """Distance-ish to a lemniscate of Bernoulli in the local xy plane."""
    x, y = p[..., 0], p[..., 1]
    f = (x * x + y * y) ** 2 - a * a * (x * x - y * y)
    gx = 4 * x * (x * x + y * y) - 2 * a * a * x
    gy = 4 * y * (x * x + y * y) + 2 * a * a * y
    return np.abs(f) / np.maximum(np.hypot(gx, gy), 1e-3)


LEM_PIX = [(3, 1), (4, 0), (5, 0), (6, 1), (5, 2), (4, 2), (3, 1), (2, 0), (1, 0), (0, 1), (1, 2), (2, 2)]


def treads_of_infinity(c1, c2, t):
    """A pair of white-gold boots, toes turned outward, gold-soled and gold-cuffed, each with a pink infinity sign
    glowing on its shaft. They walk in place on nothing: each step lands on a little ring of pink light that appears
    out of the air. Once per loop a light runs around both infinity signs, they blaze, and a lemniscate of motes
    loops between the boots."""
    power = bump(t, 0.42, 0.78)
    pink = H(c2)
    st = [0.05 * max(0.0, math.sin(TAU * 2 * t + ph)) for ph in (0.0, PI)]   # alternate steps

    def lem_pat(p, n):
        q = p - np.array([0.0, 0.05, 0])
        d = _lem(q, 0.2)
        ln = (d < 0.05) & (np.abs(q[..., 0]) < 0.25)
        return np.where(ln, -0.35, 0.0), np.zeros(p.shape[:-1])

    mats = [
        wg_mat(c2, pattern=lem_pat, emissive=lerp3(pink, WHITE, 0.5 * power)),
        gold_mat(),
    ]
    parts = []
    for side, s in ((-1, 0), (1, 1)):
        def boot(p, side=side):
            q = p.copy()
            q[..., 0] *= side
            shaft = sd_box(q, (0.22, 0.36, 0.18), 0.05)
            foot = sd_box(q - np.array([0.17, -0.36, 0]), (0.36, 0.13, 0.19), 0.06)
            return np.minimum(shaft, foot)

        def trim(p, side=side):
            q = p.copy()
            q[..., 0] *= side
            cuff = sd_box(q - np.array([0, 0.35, 0]), (0.25, 0.07, 0.205), 0.03)
            sole = sd_box(q - np.array([0.17, -0.5, 0]), (0.375, 0.045, 0.205), 0.02)
            toe = sd_box(q - np.array([0.47, -0.37, 0]), (0.07, 0.1, 0.2), 0.03)
            return np.minimum(np.minimum(cuff, sole), toe)
        pos = np.array([side * 0.36, 0.12 + st[s], 0.0])
        parts.append(Part(boot, 0, pos=pos))
        parts.append(Part(trim, 1, pos=pos))
    view = dict(ARM_VIEW)
    view['view_yaw'] = 0.2

    def ov(img):
        # steps landing on nothing: a ring of light appears under each boot as it comes down
        for s, side in ((0, -1), (1, 1)):
            ph = (2 * t + (0.0 if s == 0 else 0.5)) % 1.0
            land = bump(ph, 0.45, 0.85)
            x, y, _ = proj((side * 0.52, 0.12 - 0.56, -0.05), view)
            ring(img, x, y + 1.5, 3.5 + 2.5 * win(ph, 0.45, 0.85), pink, land * 0.8, w=0.7, sy=0.35)
            for i in range(2):
                life = (ph + i / 2) % 1.0
                dot(img, x - 2 + i * 4, y + 3 + life * 3, lerp3(pink, WHITE, 0.3), math.sin(PI * life) * 0.7, back_only=True)
        # the glowing infinity signs (crisp pixel sprites on each shaft)
        lit = 0.75 + 0.25 * math.sin(TAU * t * 2) + 0.4 * power
        u = win(t, 0.42, 0.66)
        for s, side in ((0, -1), (1, 1)):
            x, y, _ = proj((side * 0.36, 0.12 + st[s] + 0.05, -0.2), view)
            x0, y0 = int(round(x)) - 3, int(round(y)) - 1
            for j, (dx, dy) in enumerate(LEM_PIX):
                head = 0 < u < 1 and abs((u * 2 * len(LEM_PIX)) % len(LEM_PIX) - j) < 1.5
                col = WHITE if head else lerp3(H('#ff6ad8'), WHITE, cl(0.1 + 0.5 * power))
                dot(img, x0 + dx, y0 + dy, col, cl(lit))
        # a light runs round both infinity signs, then they blaze
        if power > 0:
            u = win(t, 0.42, 0.66)
            for s, side in ((0, -1), (1, 1)):
                a = TAU * u * 2
                lx = 0.15 * math.cos(a) / (1 + math.sin(a) ** 2)
                ly = 0.15 * math.sin(a) * math.cos(a) / (1 + math.sin(a) ** 2)
                x, y, _ = proj((side * 0.36 + lx * 1.33, 0.12 + st[s] + 0.05 + ly * 1.33, -0.2), view)
                spark(img, x, y, pink, 0.7 * power)
            fl = bump(t, 0.62, 0.78)
            for s, side in ((0, -1), (1, 1)):
                x, y, _ = proj((side * 0.36, 0.12 + st[s] + 0.07, -0.2), view)
                glow(img, x, y, 5, pink, 0.7 * fl)
            # a lemniscate of motes loops between the boots
            for i in range(10):
                a = TAU * (i / 10 + t * 2)
                lx = 0.75 * math.cos(a) / (1 + math.sin(a) ** 2)
                ly = 0.75 * math.sin(a) * math.cos(a) / (1 + math.sin(a) ** 2)
                x, y, _ = proj((lx, -0.62 + ly * 0.35, -0.3), view)
                dot(img, x, y, lerp3(pink, WHITE, 0.4 * (i % 2)), power * 0.9)
        shine_sweep(img, 0.84, 0.99, t, k=0.5)
    return parts, mats, view, ov


# =========================================================================================== LAWGIVER'S CODEX
BK_W, BK_H, BK_ZC, BK_ZP = 0.74, 0.54, 0.13, 0.095


def lawgivers_codex(c1, c2, t):
    """A thick leather tome bound in gold with a golden sigil on its cover. It opens by itself; golden law-lines
    write themselves across the blank pages (a quill-spark at the head of each line), the law blazes as it takes
    effect, a page turns, a single seal-mark is struck on the fresh page, and the book closes again."""
    op = sm(win(t, 0.08, 0.22)) * (1 - sm(win(t, 0.82, 0.94)))
    th = (PI - 0.42) * op
    sx = -BK_W / 2 * (1 - op)
    wl = win(t, 0.22, 0.36)                     # writing the left page
    wr = win(t, 0.38, 0.52) * (t < 0.62)        # writing the right page (blank again after the turn)
    power = bump(t, 0.52, 0.64)
    turn = sm(win(t, 0.62, 0.74))
    seal = bump(t, 0.74, 0.84)
    gold = H(c2)
    leather = '#6a3a16'
    rows = [0.34 - i * 0.17 for i in range(5)]

    def cover_box(p, z0):
        return sd_box(p - np.array([BK_W / 2, 0, z0]), (BK_W / 2, BK_H, 0.03), 0.02)

    def cover_pat(p, n):
        x, y = p[..., 0] - BK_W / 2, p[..., 1]
        border = (np.abs(x) > BK_W / 2 - 0.07) | (np.abs(y) > BK_H - 0.07)
        corner = (np.abs(x) > BK_W / 2 - 0.13) & (np.abs(y) > BK_H - 0.13)
        r = np.hypot(x, y - 0.05)
        sig = (np.abs(r - 0.15) < 0.035) | ((np.abs(x) < 0.03) & (np.abs(y) < 0.3)) | ((np.abs(y + 0.22) < 0.03) & (np.abs(x) < 0.15))
        g = border | corner | sig
        return np.where(g, 0.35, 0.0), g * (0.55 + 0.45 * power)

    def lines_mask(p, prog, rev):
        x, y = p[..., 0], p[..., 1]
        u = np.abs(x)
        m = np.zeros(p.shape[:-1], bool)
        for i, ry in enumerate(rows):
            pr = cl(prog * 5 - i)
            if pr <= 0:
                continue
            x0, x1 = 0.1, 0.64 - (0.2 if i == 4 else 0.0)
            reach = x0 + (x1 - x0) * pr
            if rev:
                hit = (u < x1) & (u > x1 - (x1 - x0) * pr)
            else:
                hit = (u > x0) & (u < reach)
            wave = 0.012 * np.sin(u * 70 + i)
            m |= hit & (np.abs(y - ry + wave) < 0.045)
        return m

    def page_pat_factory(prog, rev, mark=0.0):
        def f(p, n):
            front = n[..., 2] < -0.6
            m = lines_mask(p, prog, rev) & front
            if mark > 0:
                q = np.hypot(p[..., 0] - 0.37, p[..., 1] + 0.06)
                m |= front & (np.abs(q - 0.09 * (0.6 + 0.4 * mark)) < 0.025 * (mark > 0.2))
            edges = (np.abs(n[..., 2]) < 0.6) & (np.sin(p[..., 2] * 160) > 0.3)
            return np.where(m, 0.6, np.where(edges, -0.18, 0.28)), m * 1.0
        return f

    mats = [
        Material(leather, '#c08040', metal=0.15, gloss=0.6, emissive=gold, pattern=cover_pat),
        Material('#f4e8c8', '#ffffff', metal=0.0, gloss=0.5, emissive=lerp3(gold, WHITE, 0.5 * power),
                 pattern=page_pat_factory(wl, True)),
        Material('#f4e8c8', '#ffffff', metal=0.0, gloss=0.5, emissive=lerp3(gold, WHITE, 0.5 * power),
                 pattern=page_pat_factory(wr, False, seal)),
        Material('#f8eed4', '#ffffff', metal=0.0, gloss=0.5, emissive=gold, pattern=page_pat_factory(1.0, False)),
        Material(GOLD, '#fff0b0', metal=0.95, gloss=1.4),
    ]
    hinge = np.array([sx, 0.0, 0.0])
    Rf = roty(th)
    Rt = roty(PI * turn)
    parts = [
        Part(lambda p: cover_box(p, BK_ZC - 0.03), 0, pos=hinge),                        # back cover
        Part(lambda p: sd_box(p - np.array([BK_W / 2 - 0.01, 0, BK_ZP / 2]), (BK_W / 2 - 0.035, BK_H - 0.03, BK_ZP / 2), 0.01),
             2, pos=hinge),                                                              # right page block
        Part(lambda p: cover_box(p, -BK_ZC + 0.03), 0, pos=hinge, rot=Rf),               # front cover
        Part(lambda p: sd_box(p - np.array([BK_W / 2 - 0.01, 0, -BK_ZP / 2]), (BK_W / 2 - 0.035, BK_H - 0.03, BK_ZP / 2), 0.01),
             1, pos=hinge, rot=Rf @ roty(PI) @ rotz(PI)),                                 # left page block (text faces out)
        Part(lambda p: sd_cyl(p, BK_ZC, BK_H + 0.01, 0.02), 0, pos=hinge),                # spine
        Part(lambda p: np.minimum(sd_torus(p - np.array([0, 0.3, 0]), BK_ZC, 0.022),
                                  sd_torus(p - np.array([0, -0.3, 0]), BK_ZC, 0.022)), 4, pos=hinge),   # spine bands
    ]
    if 0.0 < turn < 1.0:
        parts.append(Part(lambda p: sd_box(p - np.array([BK_W / 2 - 0.01, 0, 0]), (BK_W / 2 - 0.04, BK_H - 0.04, 0.008)),
                          3, pos=hinge + np.array([0, 0, -0.012]), rot=Rt))
    view = dict(view_pitch=0.12 + 0.1 * op, view_yaw=0.38 * (1 - op) + 0.06, zoom=1.0)

    def ov(img):
        # light spills from between the pages as it opens
        k = bump(t, 0.1, 0.26)
        hx, hy, _ = proj(hinge, view)
        glow(img, hx, hy, 9, H('#ffe8a0'), 0.55 * k)
        # quill spark at the head of the line being written
        for prog, rev, side in ((wl, True, -1), (wr, False, 1)):
            if 0 < prog < 1:
                i = min(4, int(prog * 5))
                pr = prog * 5 - i
                x0, x1 = 0.1, 0.64 - (0.2 if i == 4 else 0.0)
                u = x1 - (x1 - x0) * pr if rev else x0 + (x1 - x0) * pr
                x, y, _ = proj(hinge + np.array([side * u, rows[i], -0.01]), view)
                spark(img, x, y, gold, 0.75)
        # the law takes effect: the text blazes and light rises off the pages
        if power > 0:
            glow(img, hx, hy, 14, H('#fff0b0'), 0.45 * power)
            for i in range(7):
                life = (t * 6 + i / 7) % 1.0
                x = hx - 9 + i * 3
                dot(img, x, hy - 2 - life * 12, gold, power * math.sin(PI * life))
            outline_flash(img, gold, 0.6 * power)
        # the seal-mark is struck
        if seal > 0:
            x, y, _ = proj(hinge + np.array([0.37, -0.06, -0.01]), view)
            star(img, x, y, 2 + 4 * seal, WHITE, seal, diag=0.4)
        # idle closed: the cover sigil glints, a faint pulse
        if op < 0.05:
            u = (t + 0.12) % 1.0
            spark(img, *proj(hinge + np.array([BK_W / 2, 0.15, -BK_ZC - 0.01]), view)[:2], gold, 0.5 * bump(u, 0.0, 0.2))
    return parts, mats, view, ov


# =========================================================================================== exports
ITEMS = {
    'omnipotent_edge': omnipotent_edge,
    'singularity_scepter': singularity_scepter,
    'crown_of_transcendence': crown_of_transcendence,
    'mantle_of_omnipotence': mantle_of_omnipotence,
    'greaves_of_the_absolute': greaves_of_the_absolute,
    'treads_of_infinity': treads_of_infinity,
    'lawgivers_codex': lawgivers_codex,
}
RECIPES = {k: ((), ()) for k in ITEMS}
NARRATIVES = {}
FRAMES = {
    'omnipotent_edge': 64, 'singularity_scepter': 80, 'crown_of_transcendence': 64, 'mantle_of_omnipotence': 64,
    'greaves_of_the_absolute': 64, 'treads_of_infinity': 64, 'lawgivers_codex': 80,
}
