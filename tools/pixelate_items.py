"""
Item art in the Minecraft style: turns the smooth rendered item textures into rough, hand pixelled looking art.

  * art pixels are doubled: each texture is rebuilt at 16 by 16 (vanilla's item size) and shown at 32 by 32, so
    every art pixel is two by two screen pixels, as chunky as vanilla's; thin lines and highlights are kept;
  * hard edges: every pixel either solid or clear;
  * eight flat shades per item (shared by all its animation frames), so light falls in clear steps;
  * light grain: a small, fixed, per pixel nudge before the shades are picked, giving a slightly hand placed
    surface; it is the same in every frame, so animations do not flicker;
  * a dark outline in each item's own colour.

Usage: python3 tools/pixelate_items.py path/to/item.png [more.png ...]   (edits the files in place)
"""
import random, colorsys
from PIL import Image

def lum(c): return c[0]*0.3+c[1]*0.59+c[2]*0.11

def down16(fr):
    """2x2 cells to one pixel. Flat cells take their average; a cell holding an edge or a thin line keeps the pixel
    that stands out most from its neighbourhood, so lines and highlights survive the halving."""
    w = fr.size[0]; p = fr.load(); n = w // 2
    out = Image.new('RGBA', (n, n), (0, 0, 0, 0)); o = out.load()
    for y in range(n):
        for x in range(n):
            cells = [p[2*x+i, 2*y+j] for i in (0, 1) for j in (0, 1)]
            op = [c for c in cells if c[3] >= 128]
            if len(op) < 2: continue
            ls = [lum(c) for c in op]
            if max(ls) - min(ls) < 40:
                o[x, y] = tuple(sum(c[k] for c in op)//len(op) for k in range(3)) + (255,)
            else:
                nb = []
                for yy in range(max(0, 2*y-2), min(w, 2*y+4)):
                    for xx in range(max(0, 2*x-2), min(w, 2*x+4)):
                        if p[xx, yy][3] >= 128: nb.append(lum(p[xx, yy]))
                m = sum(nb)/len(nb)
                o[x, y] = max(op, key=lambda c: abs(lum(c)-m))[:3] + (255,)
    return out

def stylise(frames, colours, seed, amp, outline=True):
    w = frames[0].size[0]
    rng = random.Random(seed)
    nudge = [[rng.choice((-1, 0, 0, 1)) * amp + rng.randint(-amp//3, amp//3) for _ in range(w)] for _ in range(w)]
    opaque = []
    for f in frames:
        px = f.load()
        for y in range(w):
            for x in range(w):
                c = px[x, y]
                if c[3]:
                    n = nudge[y][x]
                    opaque.append(tuple(max(0, min(255, c[k] + n)) for k in range(3)))
    if not opaque: return frames
    s = Image.new('RGB', (len(opaque), 1)); s.putdata(opaque)
    pal = s.quantize(colors=colours, method=Image.Quantize.MEDIANCUT, dither=Image.Dither.NONE)
    used = sorted(set(pal.getdata()))
    pc = pal.getpalette()
    palette = [tuple(pc[i*3:i*3+3]) for i in used]
    def near(c):
        return min(palette, key=lambda q: (q[0]-c[0])**2*3+(q[1]-c[1])**2*4+(q[2]-c[2])**2*2)
    cache = {}
    out = []
    for f in frames:
        px = f.load()
        o = Image.new('RGBA', (w, w), (0, 0, 0, 0)); op = o.load()
        for y in range(w):
            for x in range(w):
                c = px[x, y]
                if c[3]:
                    n = nudge[y][x]
                    k = tuple(max(0, min(255, c[i] + n)) for i in range(3))
                    if k not in cache: cache[k] = near(k)
                    op[x, y] = cache[k] + (255,)
        if outline:
            edge = []
            for y in range(w):
                for x in range(w):
                    if not op[x, y][3]: continue
                    nb = [(x+dx, y+dy) for dx, dy in ((1,0),(-1,0),(0,1),(0,-1))]
                    if any(not (0 <= a < w and 0 <= b < w) or not op[a, b][3] for a, b in nb):
                        edge.append((x, y))
            for x, y in edge:
                r, g, b = op[x, y][:3]
                h, l, s_ = colorsys.rgb_to_hls(r/255, g/255, b/255)
                r2, g2, b2 = colorsys.hls_to_rgb(h, max(0.07, l*0.38), min(1, s_*1.1))
                op[x, y] = (int(r2*255), int(g2*255), int(b2*255), 255)
        out.append(o)
    return out

def frames_of(im):
    w = im.size[0]
    return [im.crop((0, i*w, w, (i+1)*w)) for i in range(im.size[1]//w)]

def soft(path, im, colours=8):
    """Glows and see through overlays keep their transparency, in four steps instead of a smooth fade: chunky
    pixels and a few shades, no outline."""
    w = im.size[0]
    small = im.resize((w // 2, im.size[1] // 2), Image.BOX) if w == 32 else im.copy()
    px = small.load()
    rgb = Image.new('RGB', small.size)
    for y in range(small.size[1]):
        for x in range(small.size[0]):
            r, g, b, a = px[x, y]
            a = 0 if a < 32 else min(255, (a + 42) // 85 * 85)
            px[x, y] = (r, g, b, a)
            rgb.putpixel((x, y), (r, g, b))
    q = rgb.quantize(colors=colours, method=Image.Quantize.MEDIANCUT, dither=Image.Dither.NONE).convert('RGB')
    qp = q.load()
    for y in range(small.size[1]):
        for x in range(small.size[0]):
            a = px[x, y][3]
            px[x, y] = qp[x, y] + (a,) if a else (0, 0, 0, 0)
    return small.resize(im.size, Image.NEAREST) if w == 32 else small


def process(path, colours=8, amp=6):
    im = Image.open(path).convert('RGBA')
    h = im.getchannel('A').histogram()
    if sum(h[1:]) and sum(h[1:224]) / sum(h[1:]) > 0.25:
        return soft(path, im, colours)
    fr = frames_of(im)
    for f in fr:
        px = f.load()
        for y in range(f.size[1]):
            for x in range(f.size[0]):
                c = px[x, y]; px[x, y] = c[:3] + (255,) if c[3] >= 128 else (0, 0, 0, 0)
    seed = sum(map(ord, path.split('/')[-1]))
    w = fr[0].size[0]
    if w == 32:
        small = stylise([down16(f) for f in fr], colours, seed, amp)
        fr = [f.resize((32, 32), Image.NEAREST) for f in small]
    else:
        fr = stylise(fr, colours, seed, amp)
    out = Image.new('RGBA', im.size)
    for i, f in enumerate(fr): out.paste(f, (0, i*w))
    return out


if __name__ == '__main__':
    import sys
    for p in sys.argv[1:]:
        process(p).save(p, optimize=True)
