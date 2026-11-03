"""Round 36: the mod's own particles, replacing the vanilla ones it used to borrow (end rods, portal motes, electric
sparks, flashes, enchanting glyphs, ink, sonic booms, explosions, smoke, villager emotes, flames and dust).
Frames are drawn white-ish and tinted in code (client/particle/QHParticle), so one sprite set serves many colours.
Run directly to (re)write only these textures: python3 particles36.py"""
import math, os
import numpy as np
import gen_textures as G

KINDS = ('photon', 'phase_mote', 'arc_spark', 'flash', 'glyph', 'void_ink', 'shockwave', 'burst', 'vapour',
         'fault', 'resolve', 'plasma', 'glow')
TAU = 2 * math.pi
W = np.array([1.0, 1.0, 1.0])

# the glyphs: stroke lists in -1..1 (sum, integral, partial, psi, nabla, infinity, aleph, hbar)
GLYPHS = [
    [(0.45, -0.6, -0.45, -0.6), (-0.45, -0.6, 0.1, 0.0), (0.1, 0.0, -0.45, 0.6), (-0.45, 0.6, 0.45, 0.6)],
    [(0.3, -0.7, 0.1, -0.75), (0.1, -0.75, 0.0, -0.5), (0.0, -0.5, 0.0, 0.5), (0.0, 0.5, -0.1, 0.75), (-0.1, 0.75, -0.3, 0.7)],
    [(0.35, -0.55, 0.1, -0.7), (0.1, -0.7, -0.15, -0.5), (0.3, -0.4, 0.3, 0.3), (0.3, 0.3, 0.0, 0.6), (0.0, 0.6, -0.3, 0.35), (-0.3, 0.35, -0.1, 0.05), (-0.1, 0.05, 0.3, 0.1)],
    [(0.0, -0.75, 0.0, 0.75), (-0.5, -0.4, -0.45, 0.0), (-0.45, 0.0, 0.0, 0.3), (0.0, 0.3, 0.45, 0.0), (0.45, 0.0, 0.5, -0.4)],
    [(-0.55, -0.5, 0.55, -0.5), (-0.55, -0.5, 0.0, 0.6), (0.55, -0.5, 0.0, 0.6)],
    [(-0.6, 0.0, -0.3, -0.3), (-0.3, -0.3, 0.0, 0.0), (0.0, 0.0, 0.3, 0.3), (0.3, 0.3, 0.6, 0.0), (0.6, 0.0, 0.3, -0.3), (0.3, -0.3, 0.0, 0.0), (0.0, 0.0, -0.3, 0.3), (-0.3, 0.3, -0.6, 0.0)],
    [(-0.45, -0.6, 0.45, 0.6), (0.4, -0.6, 0.15, -0.05), (-0.15, 0.05, -0.4, 0.6)],
    [(-0.3, -0.7, -0.3, 0.6), (-0.5, -0.4, 0.0, -0.55), (-0.3, -0.05, 0.1, -0.15), (0.1, -0.15, 0.3, 0.05), (0.3, 0.05, 0.3, 0.6)],
]


def frames(kind, count=8):
    U, V, R, A = G.U, G.V, G.R, G.A
    seg, fill = G.seg, G.fill
    out = []
    for i in range(count):
        t = i / count
        cv = G.Canvas()
        if kind == 'photon':           # a bright core with a four-point glint that turns
            cv.add(fill(R - 0.35) * np.clip(1 - R / 0.35, 0, 1) ** 1.2, W)
            a = t * math.pi / 2
            for k in range(2):
                b = a + k * math.pi / 2
                cv.add(fill(seg(U, V, -math.cos(b) * 0.9, -math.sin(b) * 0.9, math.cos(b) * 0.9, math.sin(b) * 0.9, 0.035)) * 0.8, W)
        elif kind == 'phase_mote':     # a small hexagonal shard, rotating
            n = 6
            ang = A - t * TAU / 6
            k = np.cos(math.pi / n) / np.cos((ang % (TAU / n)) - math.pi / n)
            cv.add(fill(R - 0.45 * k) * (0.5 + 0.5 * np.clip(1 - R / 0.45, 0, 1)), W)
            cv.add(fill(np.abs(R - 0.45 * k) - 0.04), W)
        elif kind == 'arc_spark':      # a jagged bolt, different every frame
            rng = np.random.default_rng(100 + i)
            pts = [(-0.85, (rng.random() - 0.5) * 0.4)]
            for s in range(1, 6):
                pts.append((-0.85 + s * 0.34, (rng.random() - 0.5) * 0.9))
            for (x0, y0), (x1, y1) in zip(pts, pts[1:]):
                cv.add(fill(seg(U, V, x0, y0, x1, y1, 0.05)), W)
                cv.add(fill(seg(U, V, x0, y0, x1, y1, 0.16)) * 0.35, W)
        elif kind == 'flash':          # a wide soft bloom with a hot centre
            cv.add(np.clip(1 - R / 0.95, 0, 1) ** 1.1, W)
            cv.add(fill(R - 0.4) * 0.9, W)
        elif kind == 'glyph':
            for x0, y0, x1, y1 in GLYPHS[i % len(GLYPHS)]:
                cv.add(fill(seg(U, V, x0 * 0.85, y0 * 0.85, x1 * 0.85, y1 * 0.85, 0.07)), W)
        elif kind == 'void_ink':       # a ragged blot, curling
            wob = 0.08 * np.sin(A * 5 + t * TAU) + 0.05 * np.sin(A * 9 - t * TAU * 2)
            cv.add(fill(R - (0.6 + wob)) * 0.9, np.array([0.25, 0.2, 0.35]))
        elif kind == 'shockwave':      # a thin ring with a bright leading edge
            cv.add(fill(np.abs(R - 0.8) - 0.06), W)
            cv.add(fill(np.abs(R - 0.72) - 0.1) * 0.3, W)
            for k in range(12):
                b = k * TAU / 12 + t
                cv.add(fill(seg(U, V, math.cos(b) * 0.62, math.sin(b) * 0.62, math.cos(b) * 0.9, math.sin(b) * 0.9, 0.02)) * 0.5, W)
        elif kind == 'burst':          # an expanding fireball shell
            r = 0.25 + 0.65 * t
            cv.add(fill(R - r) * np.clip(1 - (r - R) / 0.35, 0, 1) * (1 - t * 0.6), W)
            cv.add(fill(np.abs(R - r) - 0.05) * (1 - t * 0.5), W)
        elif kind == 'vapour':         # a soft cloud puff
            wob = 0.06 * np.sin(A * 4 + t * TAU) + 0.04 * np.sin(A * 7 + 1)
            cv.add(np.clip(1 - R / (0.85 + wob), 0, 1) ** 0.8, np.array([0.85, 0.88, 0.95]))
        elif kind == 'fault':          # a red-ready warning cross in a broken ring
            cv.add(fill(seg(U, V, -0.45, -0.45, 0.45, 0.45, 0.09)), W)
            cv.add(fill(seg(U, V, 0.45, -0.45, -0.45, 0.45, 0.09)), W)
            cv.add(fill(np.abs(R - 0.78) - 0.05) * ((np.sin(A * 4 + t * TAU) > -0.3) * 1.0), W)
        elif kind == 'resolve':        # a check mark inside a turning ring
            cv.add(fill(seg(U, V, -0.45, 0.0, -0.12, 0.35, 0.09)), W)
            cv.add(fill(seg(U, V, -0.12, 0.35, 0.5, -0.4, 0.09)), W)
            cv.add(fill(np.abs(R - 0.8) - 0.04) * ((np.sin(A * 3 - t * TAU) > -0.6) * 1.0), W)
        elif kind == 'plasma':         # a flickering tongue of plasma
            s = 1 + 0.15 * math.sin(t * TAU * 2)
            body = np.sqrt(U * U + (V * 0.7) ** 2) - 0.55 * s
            cv.add(fill(body) * np.clip(1.3 - R / 0.75, 0, 1), W)
            cv.add(fill(R - 0.22), W)
        elif kind == 'glow':           # plain soft dot (tinted by colour in code)
            cv.add(np.clip(1 - R / 0.7, 0, 1) ** 1.6, W)
            cv.add(fill(R - 0.18) * 0.6, W)
        f = cv.down(outline=False)
        f[..., 3] = np.clip(cv.a.reshape(G.N, G.SS, G.N, G.SS).mean(axis=(1, 3)) * 1.5, 0, 1)
        f[..., :3] = np.clip(cv.rgb.reshape(G.N, G.SS, G.N, G.SS, 3).mean(axis=(1, 3)) * 1.5, 0, 1)
        out.append(f)
    return out


def write():
    G.setres(16)
    for k in KINDS:
        for j, fr in enumerate(frames(k)):
            G.save_single(fr, os.path.join(G.TEX, 'particle', f'{k}_{j}.png'))
    print('particles36:', len(KINDS), 'kinds')


if __name__ == '__main__':
    write()
