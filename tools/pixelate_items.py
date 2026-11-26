"""
Item art revamp: take the smooth rendered item textures toward Minecraft's hand pixelled look.

  * hard edges: every pixel either opaque or clear (no soft, blended rims);
  * a few flat shades: each item's colours are cut to a small palette shared by all its animation frames, so smooth
    gradients become bands of a few shades, as a pixel artist would paint them;
  * slight flaws: a fixed, per pixel nudge before the cut makes the band edges uneven, like hand placed pixels (the
    nudge is the same in every frame, so nothing flickers);
  * a crisp outline: the outermost pixels are redrawn as a darker shade of the colour inside them;
  * mode 16: everything is first rebuilt at 16 by 16 (vanilla's size) and doubled back, so each art pixel is two.

Usage: python3 tools/pixelate_items.py 32 path/to/item.png [more.png ...]   (edits the files in place)
"""
import glob, os, random, sys
from PIL import Image

def frames_of(im):
    w = im.size[0]
    return [im.crop((0, i * w, w, (i + 1) * w)) for i in range(im.size[1] // w)]


def down16(fr):
    """32 to 16: each 2 by 2 cell takes its median brightness opaque pixel (a real colour, so thin lines and
    highlights survive, instead of a muddy average); a cell is opaque if at least two of four are."""
    out = Image.new('RGBA', (fr.size[0] // 2, fr.size[1] // 2), (0, 0, 0, 0))
    p = fr.load()
    for y in range(out.size[1]):
        for x in range(out.size[0]):
            cells = [p[2 * x + i, 2 * y + j] for i in (0, 1) for j in (0, 1)]
            op = [c for c in cells if c[3] >= 128]
            if len(op) >= 2:
                op.sort(key=lambda c: c[0] * 3 + c[1] * 6 + c[2])
                out.putpixel((x, y), op[(len(op) - 1) // 2][:3] + (255,))
    return out


def stylise(frames, colours, seed, amp=9, block=1):
    w = frames[0].size[0]
    rng = random.Random(seed)
    cells = [[rng.randint(-amp, amp) for _ in range(w)] for _ in range(w)]
    nudge = [[cells[y // block][x // block] for x in range(w)] for y in range(w)]
    # hard edges
    hard = []
    for fr in frames:
        f = fr.copy()
        px = f.load()
        for y in range(w):
            for x in range(w):
                r, g, b, a = px[x, y]
                px[x, y] = (r, g, b, 255) if a >= 128 else (0, 0, 0, 0)
        hard.append(f)
    # one palette for all frames, from the nudged colours
    strip = Image.new('RGB', (w, w * len(hard)))
    opaque = []
    for i, f in enumerate(hard):
        px = f.load()
        for y in range(w):
            for x in range(w):
                r, g, b, a = px[x, y]
                if a:
                    n = nudge[y][x]
                    c = (max(0, min(255, r + n)), max(0, min(255, g + n)), max(0, min(255, b + n)))
                    strip.putpixel((x, y + i * w), c)
                    opaque.append(c)
    if not opaque:
        return frames
    sample = Image.new('RGB', (len(opaque), 1))
    sample.putdata(opaque)
    pal_img = sample.quantize(colors=colours, method=Image.Quantize.MEDIANCUT, dither=Image.Dither.NONE)
    q = strip.quantize(palette=pal_img, dither=Image.Dither.NONE).convert('RGB')
    out = []
    for i, f in enumerate(hard):
        o = Image.new('RGBA', (w, w), (0, 0, 0, 0))
        src = f.load()
        qp = q.load()
        op = o.load()
        for y in range(w):
            for x in range(w):
                if src[x, y][3]:
                    op[x, y] = qp[x, y + i * w] + (255,)
        # crisp outline: an edge pixel becomes a dark shade of its inside neighbour
        edge = []
        for y in range(w):
            for x in range(w):
                if not op[x, y][3]:
                    continue
                nb = [(x + dx, y + dy) for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))]
                if any(not (0 <= a < w and 0 <= b < w) or not op[a, b][3] for a, b in nb):
                    inner = [op[a, b] for a, b in nb if 0 <= a < w and 0 <= b < w and op[a, b][3]]
                    edge.append((x, y, inner))
        for x, y, inner in edge:
            base = max(inner, key=lambda c: sum(c[:3])) if inner else op[x, y]
            r, g, b = base[:3]
            # darker and a little more saturated, never pure black
            m = (r + g + b) / 3
            op[x, y] = (int(max(18, (r + (r - m) * 0.3) * 0.42)), int(max(16, (g + (g - m) * 0.3) * 0.42)),
                        int(max(20, (b + (b - m) * 0.3) * 0.42)), 255)
        out.append(o)
    return out


def process(path, mode):
    im = Image.open(path).convert('RGBA')
    frames = frames_of(im)
    name = os.path.basename(path)
    seed = sum(map(ord, name))
    if mode == 16 and frames[0].size[0] == 32:
        small = [down16(f) for f in frames]
        small = stylise(small, 10, seed, amp=5)
        frames = [f.resize((32, 32), Image.NEAREST) for f in small]
    else:
        frames = stylise(frames, 7 if frames[0].size[0] == 32 else 6, seed, amp=8, block=2)
    out = Image.new('RGBA', im.size)
    w = im.size[0]
    for i, f in enumerate(frames):
        out.paste(f, (0, i * w))
    return out


if __name__ == '__main__':
    mode = int(sys.argv[1])
    for p in sys.argv[2:]:
        process(p, mode).save(p)
