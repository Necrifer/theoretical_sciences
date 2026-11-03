"""Round 30: the glow layers of Singularity Clusters (level 1), Hyperclusters (2) and Ultraclusters (3).
Each is a 40-frame loop of bright singularities orbiting a common centre - 3, 5 or 8 of them, with trails and, for the
higher levels, extra rings - drawn white so the item's tint colours them. Loops exactly: every orbit makes a whole
number of turns in 40 frames."""
import math, os
from PIL import Image, ImageDraw, ImageFilter

OUT = os.path.join(os.path.dirname(__file__), '..', '..', 'src/main/resources/assets/theoreticalsciences/textures/item')
S, N, SS = 32, 40, 4                     # size, frames, supersampling


def frame(level, f):
    big = Image.new('RGBA', (S * SS, S * SS), (0, 0, 0, 0))
    d = ImageDraw.Draw(big)
    c = S * SS / 2
    count = {1: 3, 2: 5, 3: 8}[level]
    rings = {1: [10], 2: [8, 12.5], 3: [6.5, 10, 13.5]}[level]
    for ri, r in enumerate(rings):                       # faint orbit rings, slowly breathing
        rr = (r + 0.4 * math.sin(2 * math.pi * (f / N + ri / 3))) * SS
        a = 70 + 30 * ri
        d.ellipse((c - rr, c - rr * 0.55, c + rr, c + rr * 0.55), outline=(255, 255, 255, a), width=SS)
    for k in range(count):
        ring = rings[k % len(rings)]
        turns = 1 + (k % len(rings))                     # whole turns per loop: seamless
        for t in range(6, -1, -1):                       # the trail behind, then the body
            ph = 2 * math.pi * (turns * (f - t * 0.6) / N + k / count)
            x, y = c + math.cos(ph) * ring * SS, c + math.sin(ph) * ring * SS * 0.55
            rad = (1.6 - t * 0.18) * SS
            a = int(255 * (1 - t / 7) ** 1.5)
            d.ellipse((x - rad, y - rad, x + rad, y + rad), fill=(255, 255, 255, a))
    # the common centre, pulsing
    p = 1.2 + 0.6 * (0.5 + 0.5 * math.sin(2 * math.pi * f / N)) + 0.4 * level
    d.ellipse((c - p * SS, c - p * SS, c + p * SS, c + p * SS), fill=(255, 255, 255, 200))
    im = big.resize((S, S), Image.LANCZOS)
    glow = im.filter(ImageFilter.GaussianBlur(0.8))
    return Image.alpha_composite(glow, im)


for level in (1, 2, 3):
    strip = Image.new('RGBA', (S, S * N))
    for f in range(N):
        strip.paste(frame(level, f), (0, f * S))
    name = 'singularity_cluster_glow' + ('' if level == 1 else f'_{level}')
    strip.save(os.path.join(OUT, name + '.png'))
    open(os.path.join(OUT, name + '.png.mcmeta'), 'w').write('{"animation": {"frametime": 2, "interpolate": false}}')
print('cluster glow layers written')
