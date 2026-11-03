"""Round 24: the Specimen Bundle - a sealed glass capsule (layer 0, untinted) with swirling contents (layer 1,
tinted in its mod's colour). 32 frames, loops (all motion is periodic in the frame number)."""
import math, os, sys
from PIL import Image

S, N = 32, 32
OUT = sys.argv[1] if len(sys.argv) > 1 else '.'


def c(x):
    return max(0, min(255, int(round(x))))


L0, L1 = Image.new('RGBA', (S, S * N)), Image.new('RGBA', (S, S * N))
for f in range(N):
    t = f / N
    for y in range(S):
        for x in range(S):
            # capsule: a rounded vertical pill from y 5..27, x 9..22, with caps
            cx, top, bot, r = 15.5, 9.0, 23.0, 6.6
            yy = min(max(y + 0.5, top), bot)
            d = math.hypot(x + 0.5 - cx, y + 0.5 - yy)
            if d <= r:
                # contents: a churning cloud of specimens, lower 3/4 of the capsule
                fill = top - 3 + 4 + 2 * math.sin(2 * math.pi * (t + x / 13))
                if y + 0.5 > fill:
                    n = 0.5 + 0.25 * math.sin(2 * math.pi * (t * 2 + x / 5 + y / 7)) + 0.25 * math.sin(2 * math.pi * (-t + y / 4 - x / 9))
                    v = 120 + 120 * n * (1 - d / r * 0.5)
                    L1.putpixel((x, y + f * S), (c(v), c(v), c(v), 235))
                # glass
                edge = d / r
                a = 40 + (190 if edge > 0.82 else 0)
                hl = math.exp(-((x + 0.5 - cx + 2.8) ** 2 / 2 + (y + 0.5 - 12) ** 2 / 20)) * 200
                g = 230 if edge > 0.82 else 255
                L0.putpixel((x, y + f * S), (g, g, 255, c(max(a, hl))))
            # caps (metal) above and below
            if (y in (2, 3, 28, 29) and 10 <= x <= 21) or (y in (1, 30) and 12 <= x <= 19):
                sh = 150 + 70 * math.cos((x - 12) / 9 * math.pi) * 0.5
                L0.putpixel((x, y + f * S), (c(sh), c(sh * 0.95), c(sh * 0.8), 255))
    # sparkles drifting up
    for k in range(4):
        ph = (t + k / 4) % 1
        px, py = int(12 + 7 * ((k * 0.37) % 1)), int(26 - 16 * ph)
        a = int(255 * math.sin(math.pi * ph))
        if a > 30:
            L1.putpixel((px, py + f * S), (255, 255, 255, a))
for im, n in ((L0, ''), (L1, '_contents')):
    p = os.path.join(OUT, 'specimen_bundle' + n + '.png')
    im.save(p)
    open(p + '.mcmeta', 'w').write('{"animation": {"frametime": 2, "interpolate": true}}')
print('bundle textures written')
