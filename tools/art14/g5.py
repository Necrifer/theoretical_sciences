"""Item art group 5 (round 14, follow-up): Omnipotence Dust, Apeiron Ingot and Omnipotent Ingot get shape-shifting
stories asked for by the player. Loads after g3 and overrides its builders. Every loop is seamless (t=0 == t=1)."""
import math
import numpy as np
from items3d import *
import anim
from art14.g3 import (PI, cl, sm, win, bump, back, pulse, hsv, lerp3, _hs, hue_map, _ing, _void, scaled, sd_segs,
                      sd_poly, _flat, _upright, _starshape, cam_of, proj, paint, paint_behind, glow, ring, star, dot,
                      rays, spark, opaque, edge, dilate, outline_flash, halo, fade, recolor, run_edge, XX, YY, N)


def _lerp_sd(a, b, k):
    if k <= 0:
        return a
    if k >= 1:
        return b
    return lambda p: a(p) * (1 - k) + b(p) * k


def _timeline(t, keys):
    """keys: [(t_start_of_hold, t_end_of_hold, index)], sorted. Returns (i, j, k): blend from form i to j by k."""
    for n, (a, b, i) in enumerate(keys):
        if a <= t < b:
            return i, i, 0.0
        nxt = keys[n + 1] if n + 1 < len(keys) else None
        if nxt and b <= t < nxt[0]:
            return i, nxt[2], sm((t - b) / (nxt[0] - b))
    return keys[-1][2], keys[-1][2], 0.0


def _invert(img, k):
    if k <= 0:
        return
    op = opaque(img)[..., None]
    img[..., :3] = np.where(op, img[..., :3] * (1 - k) + (1 - img[..., :3]) * k, img[..., :3])


# =========================================================================================== OMNIPOTENCE DUST
DU_BASE = np.array([0.0, -0.62, 0.0])
DU_C = np.array([0.0, 0.02, 0.0])


def _du_heap(s):
    def f(p):
        q = (p - DU_BASE) / max(s, 1e-3) + DU_BASE
        d = sd_sphere((q - DU_BASE) / np.array([1.0, 0.55, 1.0]), 0.78) * 0.55
        d = d + 0.025 * np.sin(q[..., 0] * 17) * np.sin(q[..., 2] * 15)
        return np.maximum(d, -(q[..., 1] + 0.62)) * s
    return f


def _box_frame(p, b, e):
    p = np.abs(p) - b
    q = np.abs(p + e) - e
    L = lambda x, y, z: np.sqrt(np.maximum(x, 0) ** 2 + np.maximum(y, 0) ** 2 + np.maximum(z, 0) ** 2) + np.minimum(np.maximum(x, np.maximum(y, z)), 0)
    return np.minimum(np.minimum(L(p[..., 0], q[..., 1], q[..., 2]), L(q[..., 0], p[..., 1], q[..., 2])), L(q[..., 0], q[..., 1], p[..., 2]))


def _du_forms(t):
    a = TAU * t * 2
    R1, R2 = roty(a), rotx(0.6) @ roty(-a * 1.5)
    Rt = rotx(1.25 + 0.2 * math.sin(TAU * t * 2)) @ rotz(a)
    Rc1, Rc2 = rotx(0.5) @ roty(a * 0.75), roty(-a * 1.5) @ rotx(0.6)
    Rs = roty(a * 0.6) @ rotz(0.3)
    loc = lambda p, R: (p - DU_C) @ R
    return [
        _du_heap(1.0),                                                                             # 0 dust heap
        lambda p: sd_gem(loc(p, R1), 0.3, 0.7, 0.45),                                              # 1 crystal
        lambda p: sd_sphere(p - DU_C, 0.5),                                                        # 2 sphere
        lambda p: sd_torus(loc(p, Rt), 0.52, 0.17),                                                 # 3 ring
        lambda p: np.minimum(_box_frame(loc(p, Rc1), np.array([0.46] * 3), 0.06),
                             sd_box(loc(p, Rc2), (0.2, 0.2, 0.2), 0.02)),                         # 4 nested cubes
        lambda p: _starshape(loc(p, Rs) / 0.95) * 0.95,                                            # 5 star
    ]


DU_TINT = [H('#fff0c0'), H('#d8f4ff'), H('#ffd870'), H('#ffd0ec'), H('#ddd4ff'), H('#ffffff')]
DU_EM = [H('#ffffff'), H('#a0e8ff'), H('#fff0a0'), H('#ff9ad8'), H('#b8a0ff'), H('#fff6c0')]
DU_KEYS = [(0.0, 0.18, 0), (0.24, 0.29, 1), (0.32, 0.41, 2), (0.44, 0.53, 3), (0.56, 0.64, 4), (0.67, 0.75, 5)]
DU_FLASH = [0.21, 0.305, 0.425, 0.545, 0.655]
DU_GRAINS = [(math.cos(TAU * i / 8 + 0.4) * (0.25 + 0.25 * (i % 2)), -0.42 + 0.06 * (i % 3), math.sin(TAU * i / 8 + 0.4) * (0.25 + 0.25 * (i % 2)))
             for i in range(8)]


def omnipotence_dust(c1, c2, t):
    """A heap of white-gold dust. It lifts and becomes, in turn, a crystal, a sphere, a ring, a cube inside a cube and
    an eight-pointed star - a flash at every change, each form with its own faint tint. The star then crumbles: its
    grains rain down and the heap builds up again."""
    forms = _du_forms(t)
    crumble = t >= 0.75
    parts, mats = [], []
    if not crumble:
        i, j, k = _timeline(t, DU_KEYS)
        fn = _lerp_sd(forms[i], forms[j], k)
        tint, em = lerp3(DU_TINT[i], DU_TINT[j], k), lerp3(DU_EM[i], DU_EM[j], k)
        if i == 0 and t > 0.05:
            tint = lerp3(H(c1), tint, k)
        cur = j if k > 0.5 else i
    else:
        sh = 1 - sm(win(t, 0.75, 0.79))          # the star breaks up
        hs = 0.2 + 0.8 * sm(win(t, 0.78, 0.92))  # the heap builds up again
        st = scaled(lambda p: forms[5](p + DU_C) , sh)
        st2 = lambda p: st(p - DU_C)
        heap = _du_heap(hs)
        fn = (lambda p: np.minimum(st2(p), heap(p))) if sh > 0.02 else heap
        tint, em, cur = H(c1), H(c2), 0
    if not crumble and cur == 0:
        tint = H(c1)

    def glit(p, n):
        cc = np.floor(p * 13)
        hh = _hs(cc[..., 0] * 7.1 + cc[..., 1] * 3.3 + cc[..., 2] * 5.7)
        tw = np.sin(TAU * (t * 4 + hh * 5))
        g = ((hh > 0.8) & (tw > 0.3)).astype(float)
        if cur != 0:
            g = np.maximum(g, (np.sin(p[..., 1] * 16 - TAU * t * 6) > 0.8) * 0.8)
        return 0.12 * np.sin(p[..., 0] * 30 + p[..., 2] * 25), g
    parts.append(Part(fn, 0))
    mats.append(Material(tint, '#ffe080', metal=0.6, gloss=1.4, emissive=em, pattern=glit))
    # falling grains
    mats.append(Material('#fff4d0', '#ffffff', emissive='#fffbe8', pattern=pat_all()))
    gpos = []
    if 0.75 <= t < 0.9:
        fall = win(t, 0.76, 0.9)
        for g in range(8):
            a = TAU * g / 8
            top = DU_C + np.array([math.cos(a) * 0.55, 0.15 * math.sin(a * 2) + 0.1, math.sin(a) * 0.55])
            pos = lerp3(top, np.array(DU_GRAINS[g]), fall ** 2) + np.array([0, 0.25 * math.sin(PI * fall) * (g % 2), 0])
            gs = 1 - sm(win(t, 0.86, 0.9))
            gpos.append(pos)
            if gs > 0.05:
                parts.append(Part(lambda p, r=0.07 * gs: sd_sphere(p, r), 1, pos=pos))
    view = dict(view_pitch=0.45, view_yaw=0.6, zoom=1.0)

    def ov(img):
        cx, cy, _ = proj(DU_C, view)
        hx, hy, _ = proj((0, -0.45, 0), view)
        gold = H('#ffe080')
        # dust motes drawn up into the shape while it lifts
        if 0.17 < t < 0.27:
            x = win(t, 0.17, 0.27)
            for i in range(7):
                life = (x * 1.6 + i / 7) % 1.0
                px = hx + (i - 3) * 2.2 * (1 - life)
                py = hy + 2 - life * 9
                dot(img, px, py, gold, math.sin(PI * life) * (1 - x * 0.5), behind=True)
        # flashes at every change of form
        for c in DU_FLASH:
            fl = pulse(t, c, 0.014)
            if fl > 0.02:
                glow(img, cx, cy, 13, WHITE, 0.6 * fl)
                star(img, cx, cy, 11, WHITE, fl, diag=0.5)
                outline_flash(img, WHITE, fl)
        if 0.22 < t < 0.75:
            halo(img, lerp3(em, WHITE, 0.3), 0.6)
            anim.ov_sparkles(img, (t * 3) % 1.0, 17, lerp3(em, WHITE, 0.5), 3)
        # the star crumbling: a flash and grain trails
        fl = pulse(t, 0.755, 0.015)
        glow(img, cx, cy, 12, H('#fff0c0'), 0.6 * fl)
        for pos in gpos:
            if t < 0.88:
                x, y, _ = proj(pos, view)
                dot(img, x, y - 1.3, gold, 0.5, behind=True)
                dot(img, x, y - 2.6, gold, 0.25, behind=True)
        pf = win(t, 0.86, 0.97)                        # a puff where they land
        if 0 < pf < 1:
            for i in range(6):
                a = PI + PI * i / 5
                r = 3 + 7 * pf
                dot(img, hx + math.cos(a) * r, hy + 3 + math.sin(a) * r * 0.35, H(c1), (1 - pf) * 0.8, behind=True)
        if t < 0.17 or t > 0.9:
            anim.ov_sparkles(img, (t * 2) % 1.0, 33, WHITE, 3)
    return parts, mats, view, ov


# =========================================================================================== APEIRON INGOT
def _ing_s(p, L, Hh, W):
    """Ingot (trapezoid bar) with independent length / height / width."""
    r = 0.05 * min(1.0, Hh / 0.3)
    d = sd_box(p, (L, Hh, W), r)
    a, b, c = 0.285 / Hh, 0.27 / W, 0.102 / L
    pl = (p[..., 1] * a + np.abs(p[..., 2]) * b + np.abs(p[..., 0]) * c - 0.42) / math.sqrt(a * a + b * b + c * c)
    return np.maximum(d, pl)


def apeiron_ingot(c1, c2, t):
    """The boundless ingot. It begins to stretch: longer and longer, thinner and thinner, a bar, a rod, a wire running
    off both sides of the frame, a single thread of light - so thin it passes through nothing and turns inside out.
    The thread comes back as its own negative (dark, with gold where it was pale), contracts and thickens from both
    ends, and settles into the ingot again as the colours turn right-side out."""
    up = sm(win(t, 0.12, 0.47))                 # stretching
    dn = sm(win(t, 0.53, 0.86))                 # contracting
    s = up * (1 - dn)                           # 0 = ingot, 1 = thread
    st = math.exp(math.log(9.0) * s)            # length factor 1 .. 9
    thin = st ** -1.15                          # thickness factor 1 .. ~0.08
    inv = cl(win(t, 0.49, 0.51)) * (1 - sm(win(t, 0.84, 0.97)))   # negative palette
    flip = PI * sm(win(t, 0.44, 0.58)) + PI * sm(win(t, 0.84, 0.98))   # turns over at the thinnest point, back at the end
    wob = 0.04 * s * math.sin(TAU * t * 6)
    L, Hh, W = 0.85 * st, 0.3 * thin, 0.45 * thin

    def body(p):
        return _ing_s(p, L, Hh, W)
    pale, gold = H(c1), H(c2)
    base = lerp3(pale, H('#2a1d55'), inv)
    acc = lerp3(gold, H('#ffd060'), inv)
    emis = lerp3(gold, H('#ffe070'), inv)

    def pat(p, n):
        ripple = np.sin(p[..., 0] * 9 / st - TAU * t * 4)
        bands = (np.sin(p[..., 0] * 14 / max(st ** 0.5, 1) - TAU * t * 5 * np.sign(p[..., 0] + 1e-9)) > 0.6).astype(float)
        em = bands * cl(s * 2.5) + cl((s - 0.75) * 4)
        if inv > 0:                               # the negative: edges glow gold
            em = np.maximum(em, (np.abs(n[..., 1]) < 0.6) * inv * 0.9)
        return 0.06 * ripple, em
    parts = [Part(body, 0, pos=(0, wob, 0), rot=rotx(flip))]
    mats = [Material(base, acc, metal=0.9, gloss=1.5, emissive=emis, pattern=pat, keep_dark=True)]
    view = dict(view_pitch=0.55, view_yaw=0.32, zoom=1.0)

    def ov(img):
        cx, cy, _ = proj((0, 0, 0), view)
        # thread glint running out along the wire
        if s > 0.3:
            k = cl((s - 0.3) * 2)
            for sd in (1, -1):
                for j in range(3):
                    u = ((t * 3 + j / 3) % 1.0)
                    x, y, _ = proj((sd * u * 1.4, wob, 0), view)
                    dot(img, x, y, lerp3(gold, WHITE, 0.6), k * (1 - u) * 0.9)
        # the inversion: a white snap, then a negative flash
        fl = pulse(t, 0.5, 0.012)
        if fl > 0.02:
            glow(img, cx, cy, 16, WHITE, 0.8 * fl, power=1.0)
            star(img, cx, cy, 15, WHITE, fl, diag=0.7)
        nf = bump(t, 0.5, 0.56)
        _invert(img, 0.85 * nf)
        if 0.5 < t < 0.62:                          # shockwave of the turn
            x = win(t, 0.5, 0.62)
            ring(img, cx, cy, 2 + x * 13, lerp3(gold, WHITE, 0.4), (1 - x) * 0.8, sy=0.45)
        if 0.86 < t < 0.98:                         # turning right-side out: a sweep along the bar
            x = win(t, 0.86, 0.98)
            outline_flash(img, lerp3(pale, WHITE, 0.5), 0.8 * math.sin(PI * x))
            sweep = np.abs(XX - (4 + x * 24)) < 1.5
            paint(img, sweep * opaque(img) * 0.6 * math.sin(PI * x), WHITE)
        if s < 0.05 and inv < 0.05:
            ex = edge(img)
            shim = (np.sin((XX * 0.7 - YY * 0.5) * 1.6 - TAU * t * 5) > 0.6) & ex
            paint(img, shim * 0.5, lerp3(gold, WHITE, 0.4))
            anim.ov_glint(img, (t * 1.5) % 1.0, 5, start=0.0)
    return parts, mats, view, ov


# =========================================================================================== OMNIPOTENT INGOT
TS_V = np.array([[(i >> b) & 1 for b in range(4)] for i in range(16)], float) * 2 - 1
TS_E = np.array([(a, b) for a in range(16) for b in range(a + 1, 16) if bin(a ^ b).count('1') == 1])


def _tess(a1, a2, D=3.0, sc=0.42):
    c1, s1, c2, s2 = math.cos(a1), math.sin(a1), math.cos(a2), math.sin(a2)
    R = np.eye(4)
    R[0, 0], R[0, 3], R[3, 0], R[3, 3] = c1, -s1, s1, c1
    R2 = np.eye(4)
    R2[1, 1], R2[1, 3], R2[3, 1], R2[3, 3] = c2, -s2, s2, c2
    v = TS_V @ (R2 @ R).T
    P = v[:, :3] * (sc * D / (D - v[:, 3]))[:, None]
    return P, v[:, 3]


def _trefoil(n=72, sc=0.21):
    u = np.linspace(0, TAU, n + 1)
    return np.stack([np.sin(u) + 2 * np.sin(2 * u), np.cos(u) - 2 * np.cos(2 * u), -np.sin(3 * u)], -1) * sc


TK_P = _trefoil()


def omnipotent_ingot(c1, c2, t):
    """The final ingot, which is every law at once. The white-gold bar collapses into a black hole - a black sphere
    with a blazing accretion disk and a lensed photon ring, swallowing light. It inverts with a negative flash into a
    white hole, a blinding sphere throwing out rays and matter. That shrinks to a point and unfolds into a tesseract
    turning through the fourth dimension (its far cube dim violet, its near cube gold), which twists into a glowing
    trefoil knot with a rainbow current running along it; the knot pulls tight, and condenses back into the ingot."""
    ing = lambda p: _ing(p)
    parts, mats = [], []
    view = dict(view_pitch=0.5, view_yaw=0.65, zoom=1.0)
    gold = H(c2)
    st = {}

    # ---- ingot (start and end)
    s_in = 1 - sm(win(t, 0.07, 0.13))
    if t >= 0.7:
        s_in = back(win(t, 0.74, 0.84))
    dark = sm(win(t, 0.06, 0.12))
    if s_in > 0.02:
        def filig(p, n):
            v = np.sin(p[..., 0] * 11 + np.sin(p[..., 2] * 9) * 1.5 - TAU * t * 4)
            return 0.05 * v, (np.abs(v) < 0.16) * (0.7 + 0.3 * math.sin(TAU * t * 2))
        base = lerp3(H('#fffaf0'), H('#302040'), dark if t < 0.5 else 0.0)
        mats.append(Material(base, c2, metal=0.95, gloss=1.8, emissive='#ffe9a0', pattern=filig, keep_dark=True))
        parts.append(Part(scaled(ing, s_in), len(mats) - 1, rot=roty(TAU * 0.5 * sm(win(t, 0.05, 0.13)) if t < 0.5 else 0)))

    # ---- black hole
    bh = sm(win(t, 0.08, 0.14)) * (1 - sm(win(t, 0.285, 0.30)))
    bh_disk = sm(win(t, 0.1, 0.17)) * (1 - sm(win(t, 0.28, 0.30)))
    if bh > 0.02 or bh_disk > 0.02:
        vdir = -cam_of(view)[2]

        def rimp(p, n):
            r = np.abs(n @ vdir) < 0.35
            return np.zeros(r.shape), r.astype(float) * 0.9
        m = Material('#000000', '#ffffff', metal=0.0, gloss=0.2, emissive='#ffb040', pattern=rimp, keep_dark=True)
        m.ramp[:] = 0.0
        m.ramp[4:] = 0.04
        mats.append(m)
        if bh > 0.02:
            parts.append(Part(lambda p, r=0.3 * bh: sd_sphere(p, r), len(mats) - 1))
        spin = TAU * t * 6

        def disk(p, R=0.62 * bh_disk):
            rr = np.sqrt(p[..., 0] ** 2 + p[..., 2] ** 2)
            return np.maximum(np.abs(p[..., 1]) - 0.025, np.abs(rr - R) - 0.2 * bh_disk)

        def dpat(p, n):
            rr = np.sqrt(p[..., 0] ** 2 + p[..., 2] ** 2)
            ang = np.arctan2(p[..., 2], p[..., 0])
            sw = np.sin(ang * 3 + rr * 14 - spin)
            return 0.15 * sw, np.clip(0.75 + 0.35 * sw, 0, 1)
        if bh_disk > 0.02:
            mats.append(Material('#ff9a30', '#fff0c0', metal=0.2, emissive=lerp3(H('#ffc060'), WHITE, 0.25), pattern=dpat))
            parts.append(Part(disk, len(mats) - 1, rot=rotz(0.12) @ rotx(0.18)))

    # ---- white hole
    wh = sm(win(t, 0.295, 0.32)) * (1 - sm(win(t, 0.415, 0.445)))
    if wh > 0.02:
        mats.append(Material('#ffffff', '#ffffff', emissive='#ffffff', pattern=pat_all()))
        parts.append(Part(lambda p, r=(0.36 + 0.03 * math.sin(TAU * t * 12)) * wh: sd_sphere(p, r), len(mats) - 1))

    # ---- tesseract
    ts = sm(win(t, 0.435, 0.475)) * (1 - sm(win(t, 0.565, 0.6)))
    if ts > 0.02:
        th = TAU * win(t, 0.43, 0.6)
        P, w4 = _tess(th, th * 0.5)
        P = P * ts
        ew = (w4[TS_E[:, 0]] + w4[TS_E[:, 1]]) / 2
        near, far = TS_E[ew >= 0], TS_E[ew < 0]
        st['tess'] = (P, w4)
        mats.append(Material('#ffe080', '#ffffff', emissive='#fff0b0', pattern=pat_all()))
        mats.append(Material('#9070e0', '#d0c0ff', metal=0.6, emissive='#a080ff', pattern=_flat(0.6)))
        if len(near):
            parts.append(Part(lambda p, A=P[near[:, 0]], B=P[near[:, 1]]: sd_segs(p, A, B, 0.04 * max(ts, 0.3)), len(mats) - 2, rot=roty(0.4)))
        if len(far):
            parts.append(Part(lambda p, A=P[far[:, 0]], B=P[far[:, 1]]: sd_segs(p, A, B, 0.032 * max(ts, 0.3)), len(mats) - 1, rot=roty(0.4)))

    # ---- trefoil knot, condensing into the ingot
    kn = sm(win(t, 0.585, 0.625)) * (1 - sm(win(t, 0.675, 0.7)))
    mb = sm(win(t, 0.685, 0.71)) * (1 - sm(win(t, 0.765, 0.795)))

    def kpat(p, n):
        ang = np.arctan2(p[..., 1], p[..., 0])
        cur = np.sin(ang * 2 - TAU * t * 8)
        return 0.1 * cur, (cur > 0.3) * 1.0
    if kn > 0.02:
        Pk = TK_P * kn
        Rk = roty(TAU * win(t, 0.58, 0.7) * 0.5) @ rotx(0.35)

        mats.append(Material('#ffe6a0', '#ffffff', metal=0.9, gloss=1.6, emissive='#ffffff', pattern=kpat))
        parts.append(Part(lambda p, P=Pk: sd_poly(p, P, 0.085 * max(kn, 0.4)), len(mats) - 1, rot=Rk))
    if mb > 0.02:                                   # Moebius band: one side, one edge; it shrinks into the ingot
        msc = mb * (1 - 0.35 * sm(win(t, 0.74, 0.795)))
        tw = TAU * t * 2

        def mob(p, s=msc):
            q = p / s
            u = np.arctan2(q[..., 2], q[..., 0])
            rr = np.sqrt(q[..., 0] ** 2 + q[..., 2] ** 2) - 0.58
            z = q[..., 1]
            h = u / 2 + tw
            c, sn = np.cos(h), np.sin(h)
            a, b = c * rr + sn * z, -sn * rr + c * z
            d = np.sqrt(np.maximum(np.abs(a) - 0.15, 0) ** 2 + np.maximum(np.abs(b) - 0.04, 0) ** 2) + \
                np.minimum(np.maximum(np.abs(a) - 0.15, np.abs(b) - 0.04), 0)
            return d * s * 0.7
        mats.append(Material('#c8f0ff', '#ffffff', metal=0.85, gloss=1.6, emissive='#ffffff', pattern=kpat))
        parts.append(Part(mob, len(mats) - 1, rot=rotx(-0.7) @ roty(TAU * win(t, 0.68, 0.8) * 0.5)))

    def ov(img):
        cx, cy, _ = proj((0, 0, 0), view)
        is_ing = s_in > 0.6 and (t < 0.07 or t > 0.8)
        if is_ing:                                  # crown of light behind the ingot
            cr = 1.0 if t < 0.07 else sm(win(t, 0.8, 0.9))
            r = np.hypot(XX - cx, (YY - cy - 1) / 0.8)
            paint_behind(img, 0.4 * cr * np.clip(1 - r / 15, 0, 1) ** 1.3, lerp3(gold, WHITE, 0.25))
            rays(img, cx, cy + 1, 9.5, 12.5 + 2 * math.sin(TAU * t * 3) ** 2, 16, lerp3(gold, WHITE, 0.5), 0.9 * cr, phase=TAU * t, sy=0.8)
        if 0.05 < t < 0.14:                         # collapsing: light pulled in
            anim.ov_infall(img, win(t, 0.05, 0.14), 7, lerp3(gold, WHITE, 0.4), 10)
        if bh > 0.3:                                # photon ring + lensed back of the disk
            k = bh
            ring(img, cx, cy, 5.6, H('#ffe0a0'), 0.85 * k, w=0.7, behind=True)
            ring(img, cx, cy - 0.5, 7.2, H('#ff9a30'), 0.55 * k * bh_disk, w=0.8, sy=0.6, behind=True)
            anim.ov_infall(img, (t * 3) % 1.0, 11, H('#ffc070'), 7)
        # black -> white: negative flash
        nf = bump(t, 0.285, 0.315)
        _invert(img, nf)
        fl = pulse(t, 0.297, 0.012)
        glow(img, cx, cy, 18, WHITE, 0.9 * fl, power=1.0)
        if wh > 0.1:                                # the white hole throws everything out
            glow(img, cx, cy, 16, WHITE, 0.75 * wh, power=1.2)
            rays(img, cx, cy, 7, 15, 12, WHITE, 0.9 * wh, phase=-TAU * t * 2, behind=True)
            for i in range(8):
                life = (t * 4 + i / 8) % 1.0
                a = TAU * (i / 8) + 0.3 * i
                r = 6 + life * 10
                dot(img, cx + math.cos(a) * r, cy + math.sin(a) * r * 0.8, lerp3(H('#c0e8ff'), WHITE, life), (1 - life) * wh, behind=True)
            halo(img, H('#e0f0ff'), 0.8, thr=0.85)
        fl = pulse(t, 0.44, 0.012)                  # shrinks to a point, unfolds into 4D
        glow(img, cx, cy, 6, WHITE, fl)
        star(img, cx, cy, 10, WHITE, fl, diag=0.6)
        if 'tess' in st and ts > 0.5:               # vertices: near ones white, far ones violet
            P, w4 = st['tess']
            for i in range(16):
                x, y, _ = proj(roty(0.4) @ P[i], view)
                kk = cl(0.5 + 0.5 * w4[i])
                dot(img, x, y, lerp3(H('#8060ff'), WHITE, kk), 0.5 + 0.5 * kk)
        fl = pulse(t, 0.59, 0.014)
        glow(img, cx, cy, 10, H('#e0d0ff'), 0.7 * fl)
        outline_flash(img, WHITE, fl)
        fl = pulse(t, 0.69, 0.012)                  # knot unties into a one-sided band
        glow(img, cx, cy, 10, H('#d0f0ff'), 0.7 * fl)
        star(img, cx, cy, 10, WHITE, fl, diag=0.5)
        if kn + mb > 0.3:                           # rainbow current along the knot / band
            rh = (np.arctan2(YY - cy, XX - cx) / TAU) - t * 3
            m = (img[..., :3].min(-1) > 0.8) & opaque(img)
            rc = img.copy()
            recolor(rc, hue_map(rh), 0.75 * cl(kn + mb))
            img[..., :3] = np.where(m[..., None], rc[..., :3], img[..., :3])
            halo(img, lerp3(gold, WHITE, 0.4), 0.6)
        if 0.7 < t < 0.8:                           # condensing
            anim.ov_infall(img, win(t, 0.7, 0.8), 13, hsv(t * 5, 0.4, 1.0), 10)
        fl = pulse(t, 0.775, 0.016)                 # reborn
        glow(img, cx, cy, 18, WHITE, fl)
        star(img, cx, cy, 15, WHITE, fl, diag=0.8)
        if 0.78 < t < 0.92:
            x = win(t, 0.78, 0.92)
            ring(img, cx, cy, 3 + x * 13, gold, (1 - x) * 0.8, sy=0.65)
            outline_flash(img, lerp3(gold, WHITE, 0.5), 0.7 * (1 - x))
        if t < 0.05:
            run_edge(img, cx, cy, win(t, 0.0, 0.05), WHITE, bump(t, 0.0, 0.05))
    return parts, mats, view, ov


# =========================================================================================== exports
ITEMS = {
    'omnipotence_dust': omnipotence_dust,
    'apeiron_ingot': apeiron_ingot,
    'omnipotent_ingot': omnipotent_ingot,
}
RECIPES = {k: ((), ()) for k in ITEMS}
NARRATIVES = {}
FRAMES = {'omnipotence_dust': 80, 'apeiron_ingot': 80, 'omnipotent_ingot': 96}
