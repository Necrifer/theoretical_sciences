"""
Ore textures that sit in their host rock: each ore is the host block's own texture (frame for frame, if it is
animated) with small mineral clusters set into it, vanilla-style - so a Fractal Ore looks like Axiom Stone with
crystals in it, not like a different stone. Run after the main texture generator.
"""
import io
import json
import os
import random
import zipfile

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
TEX = os.path.join(HERE, '..', 'src/main/resources/assets/theoreticalsciences/textures/block')
VANILLA_JAR = __import__("mcpaths").CLIENT_JAR
FRAMES = 16


def hexc(h):
    h = h.lstrip('#')
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def host_frames(base):
    p = os.path.join(TEX, base + '.png')
    if os.path.exists(p):
        im = Image.open(p).convert('RGBA')
    else:
        with zipfile.ZipFile(VANILLA_JAR) as z:
            im = Image.open(io.BytesIO(z.read(f'assets/minecraft/textures/block/{base}.png'))).convert('RGBA')
    w = im.width
    n = max(1, im.height // w)
    frames = [im.crop((0, k * w, w, (k + 1) * w)) for k in range(n)]
    if w != 16: frames = [f.resize((16, 16), Image.NEAREST) for f in frames]
    return frames


def clusters(seed):
    """Five or six little mineral clusters, each a short random walk of pixels."""
    rnd = random.Random(seed)
    cells = {}
    centres = []
    for _ in range(rnd.randint(5, 6)):
        for _try in range(30):
            cx, cy = rnd.randint(1, 14), rnd.randint(1, 14)
            if all(abs(cx - a) + abs(cy - b) > 4 for a, b in centres): break
        centres.append((cx, cy))
        x, y = cx, cy
        n = rnd.randint(3, 6)
        for k in range(n):
            cells[(x, y)] = 1.0 if k == 0 else 0.55 + 0.35 * rnd.random()
            dx, dy = rnd.choice(((1, 0), (-1, 0), (0, 1), (0, -1), (1, 1), (-1, 1)))
            x, y = max(0, min(15, x + dx)), max(0, min(15, y + dy))
    return cells, centres


def lerp(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def make(ore, base, c1, c2):
    hosts = host_frames(base)
    import zlib
    cells, centres = clusters(zlib.crc32(ore.encode()))
    dark, bright = hexc(c1), hexc(c2)
    out = Image.new('RGBA', (16, 16 * FRAMES))
    for f in range(FRAMES):
        img = hosts[f % len(hosts)].copy()
        px = img.load()
        # a shadow under each cluster, from the host rock itself, so the crystals look set into it
        for (x, y), _ in cells.items():
            sx, sy = x + 1, y + 1
            if sx < 16 and sy < 16 and (sx, sy) not in cells:
                r, g, b, a = px[sx, sy]
                px[sx, sy] = (int(r * 0.7), int(g * 0.7), int(b * 0.7), a)
        for (x, y), v in cells.items():
            col = lerp(dark, bright, v)
            px[x, y] = col + (255,)
        # one cluster at a time catches the light
        cx, cy = centres[(f // 3) % len(centres)]
        tw = 1 - abs((f % 3) - 1) * 0.5
        if (cx, cy) in cells:
            px[cx, cy] = lerp(bright, (255, 255, 255), 0.6 * tw) + (255,)
        out.paste(img, (0, f * 16))
    out.save(os.path.join(TEX, ore + '.png'))
    json.dump({'animation': {'frametime': 4, 'interpolate': True}}, open(os.path.join(TEX, ore + '.png.mcmeta'), 'w'))


if __name__ == '__main__':
    C = json.load(open(os.path.join(HERE, 'content.json')))
    n = 0
    for b in C['blocks']:
        if b['kind'] == 'ore':
            make(b['id'], b.get('base') or 'stone', b['c1'], b['c2'])
            n += 1
    print('ore textures rebuilt on their host rock:', n)
