"""Round 22: the Simulated Universe item - a glass sphere with a turning galaxy inside (3 layers, 40 frames).
layer0: the sphere's body (tinted dark), layer1: the galaxy (tinted the mod's colour), layer2: stars and glass (untinted)."""
import math, os, random, sys
from PIL import Image

S, N = 32, 40
OUT = sys.argv[1] if len(sys.argv) > 1 else '.'
cx = cy = 15.5
R = 13.2


def clamp(x):
    return max(0, min(255, int(round(x))))


rnd = random.Random(22)
stars = [(rnd.uniform(-1, 1), rnd.uniform(-1, 1), rnd.random(), rnd.random() * math.tau) for _ in range(40)]
stars = [s for s in stars if s[0] ** 2 + s[1] ** 2 < 0.8]
L0, L1, L2 = (Image.new('RGBA', (S, S * N)) for _ in range(3))
for f in range(N):
    t = f / N
    rot = t * math.tau
    for y in range(S):
        for x in range(S):
            dx, dy = (x - cx) / R, (y - cy) / R
            r2 = dx * dx + dy * dy
            if r2 > 1.0:
                continue
            r = math.sqrt(r2)
            # body: darker at the rim, a faint lit side top-left
            lit = 0.55 + 0.45 * max(0, -(dx * 0.6 + dy * 0.8)) * (1 - r)
            v = 120 + 110 * (1 - r) * lit
            L0.putpixel((x, y + f * S), (clamp(v), clamp(v), clamp(v), 255))
            # galaxy: two log-spiral arms, tilted into an ellipse
            gx, gy = dx, dy / 0.62
            gr = math.hypot(gx, gy)
            if gr < 0.95:
                th = math.atan2(gy, gx) - rot
                arm = 0
                for k in range(2):
                    ph = th - 2.6 * math.log(gr + 0.05) + k * math.pi
                    arm = max(arm, math.cos(ph) ** 8)
                b = arm * (1 - gr) ** 1.2 * 1.4 + math.exp(-gr * gr * 30) * 1.3
                b = min(1, b)
                if b > 0.08:
                    L1.putpixel((x, y + f * S), (clamp(255 * (0.6 + 0.4 * b)), clamp(255 * (0.6 + 0.4 * b)), clamp(255 * (0.6 + 0.4 * b)), clamp(255 * b)))
            # glass: a rim and a highlight
            a = 0
            if r > 0.86:
                a = (r - 0.86) / 0.14 * 150
            hx, hy = dx + 0.42, dy + 0.48
            hl = math.exp(-(hx * hx + hy * hy) * 28) * 230
            a = max(a, hl)
            if a > 8:
                L2.putpixel((x, y + f * S), (255, 255, 255, clamp(a)))
    for sx, sy, ph0, sp in stars:
        tw = 0.5 + 0.5 * math.sin(t * math.tau * 2 + sp)
        px, py = int(cx + sx * R * 0.95), int(cy + sy * R * 0.95)
        if tw > 0.55:
            old = L2.getpixel((px, py + f * S))
            L2.putpixel((px, py + f * S), (255, 255, 255, max(old[3], clamp(140 + 115 * tw))))
for im, n in ((L0, ''), (L1, '_galaxy'), (L2, '_glass')):
    p = os.path.join(OUT, 'simulated_universe' + n + '.png')
    im.save(p)
    open(p + '.mcmeta', 'w').write('{"animation": {"frametime": 2, "interpolate": true}}')
print('universe textures written')
