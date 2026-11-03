"""Round 32: textures of the Dodecaract Excavator and the Eschaton Cell (32x32, 40-frame loops).

* The Excavator: a pickaxe whose head is the shadow of a twelve-dimensional cube - all 4096 corners and 24576 edges
  projected onto the picture, so it shows as a 24-sided zonogon full of rhombi. Each of the twelve axes slowly grows
  and shrinks (as if turning through the hidden dimensions), and the edges are tinted by axis, cyan to violet.
* The Cell: a glass capsule with gold caps; inside, a core of charge pulses and sparks climb.
Both loop exactly (every motion makes whole turns in 40 frames).
Usage: python3 art32/gear.py"""
import math, os
import numpy as np
from PIL import Image, ImageDraw, ImageFilter

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', '..', 'src/main/resources/assets/theoreticalsciences/textures/item')
S, N, SS = 32, 40, 4                     # size, frames, supersampling
B = S * SS
TAU = 2 * math.pi


def hsv(h, s, v):
    i = int(h * 6) % 6
    f = h * 6 - int(h * 6)
    p, q, t = v * (1 - s), v * (1 - s * f), v * (1 - s * (1 - f))
    return [(v, t, p), (q, v, p), (p, v, t), (p, q, v), (t, p, v), (v, p, q)][i]


def to_img(rgb, alpha):
    a = np.clip(alpha, 0, 1)
    arr = np.dstack([np.clip(rgb, 0, 1) * 255, a * 255]).astype(np.uint8)
    return Image.fromarray(arr, 'RGBA')


def zonogon(axes, rot, radius, cx, cy):
    """The outline of a cube's shadow from these axes (directions in half-turns), turned by rot, scaled to radius."""
    vec = np.array([[math.cos(math.pi * a + rot), math.sin(math.pi * a + rot)] for a in axes])
    bits = ((np.arange(1 << len(axes))[:, None] >> np.arange(len(axes))[None, :]) & 1).astype(float)
    pts = bits @ vec
    pts -= pts.mean(0)
    hull = np.array(convex_hull(pts))
    hull = hull * (radius / np.abs(np.hypot(hull[:, 0], hull[:, 1])).max())
    # drawn as a pick's head: stretched across the haft, thin along it (so its turning looks like a change of shape)
    c, s_ = math.cos(math.pi / 4), math.sin(math.pi / 4)
    rot = np.array([[c, -s_], [s_, c]])
    m = rot @ np.diag([1.6, 0.5]) @ rot.T
    return hull @ m.T + np.array([cx, cy])


def dodecaract(img, f, cx, cy, radius):
    """The 12-cube's shadow: its 24-sided outline, with the shadows of the 6- and 3-cubes inside it (the cube seen
    through fewer of its dimensions), each turning its own way, joined corner to corner like a tesseract's drawing."""
    t = f / N
    breathe = 0.06 * math.sin(TAU * t)
    outer = zonogon([k / 12 for k in range(12)], TAU * t / 2, radius, cx, cy)                     # 24-gon
    mid = zonogon([k / 6 for k in range(6)], -TAU * t / 2, radius * (0.66 + breathe), cx, cy)       # 12-gon
    inner = zonogon([k / 3 for k in range(3)], TAU * t, radius * (0.36 - breathe), cx, cy)          # hexagon
    lay = Image.new('RGBA', img.size, (0, 0, 0, 0))
    d = ImageDraw.Draw(lay)
    d.polygon([tuple(p) for p in outer], fill=(24, 10, 48, 200))
    def near(p, poly):
        return poly[np.argmin(np.hypot(*(poly - p).T))]
    # struts: every corner to the nearest corner of the next shadow in
    for p in outer:
        q = near(p, mid)
        d.line([tuple(p), tuple(q)], fill=(120, 200, 255, 150), width=SS // 2)
    for p in mid:
        q = near(p, inner)
        d.line([tuple(p), tuple(q)], fill=(200, 140, 255, 170), width=SS // 2)
    for poly, col, wd in ((inner, (255, 120, 255, 255), SS), (mid, (110, 240, 255, 255), SS), (outer, (255, 244, 208, 255), SS + SS // 2)):
        pts = [tuple(p) for p in poly]
        d.line(pts + [pts[0]], fill=col, width=wd, joint='curve')
    # corners of the outline catch the light in turn
    for i, p in enumerate(outer):
        a = 0.5 + 0.5 * math.cos(TAU * (t * 2 - i / len(outer)))
        r = SS * (0.6 + 0.9 * a)
        d.ellipse((p[0] - r, p[1] - r, p[0] + r, p[1] + r), fill=(255, 255, 255, int(120 + 135 * a)))
    k = 0.5 + 0.5 * math.sin(TAU * t * 2)
    r = SS * (1.4 + 0.8 * k)
    d.ellipse((cx - r, cy - r, cx + r, cy + r), fill=(255, 255, 255, 255))
    glow = lay.filter(ImageFilter.GaussianBlur(1.5 * SS))
    img = Image.alpha_composite(img, glow)
    return Image.alpha_composite(img, lay)


def convex_hull(p):
    pts = sorted(set(map(lambda q: (round(q[0], 9), round(q[1], 9)), p)))
    def cross(o, a, b):
        return (a[0] - o[0]) * (b[1] - o[1]) - (a[1] - o[1]) * (b[0] - o[0])
    lo, up = [], []
    for q in pts:
        while len(lo) >= 2 and cross(lo[-2], lo[-1], q) <= 0: lo.pop()
        lo.append(q)
    for q in reversed(pts):
        while len(up) >= 2 and cross(up[-2], up[-1], q) <= 0: up.pop()
        up.append(q)
    return lo[:-1] + up[:-1]


def excavator(f):
    img = Image.new('RGBA', (B, B), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    # the haft, bottom-left to the head: white-gold with a dark outline, and bands of light running up it
    x0, y0, x1, y1 = 3 * SS, 29 * SS, 19 * SS, 13 * SS
    d.line([(x0, y0), (x1, y1)], fill=(40, 28, 10, 255), width=int(3.6 * SS))
    d.line([(x0, y0), (x1, y1)], fill=(240, 214, 150, 255), width=int(2.2 * SS))
    for k in range(4):
        u = ((f / N) + k / 4) % 1
        bx, by = x0 + (x1 - x0) * u, y0 + (y1 - y0) * u
        d.ellipse((bx - 1.2 * SS, by - 1.2 * SS, bx + 1.2 * SS, by + 1.2 * SS), fill=(255, 250, 230, int(255 * math.sin(math.pi * u))))
    # the head: the 12-cube's shadow
    img = dodecaract(img, f, 19 * SS, 13 * SS, 9 * SS)
    im = img.resize((S, S), Image.LANCZOS)
    return im


def cell(f):
    t = f / N
    img = Image.new('RGBA', (B, B), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    cx = 16 * SS
    top, bot, rw = 6 * SS, 26 * SS, 6 * SS
    # the core of charge, pulsing magenta to gold
    k = 0.5 + 0.5 * math.sin(TAU * t)
    core = tuple(int(255 * v) for v in hsv(0.86 + 0.16 * k, 0.55, 1.0))
    halo = Image.new('RGBA', (B, B), (0, 0, 0, 0))
    ImageDraw.Draw(halo).rounded_rectangle((cx - rw - 2 * SS, top - SS, cx + rw + 2 * SS, bot + SS), radius=rw, fill=core + (150,))
    img = Image.alpha_composite(img, halo.filter(ImageFilter.GaussianBlur(2.5 * SS)))
    d = ImageDraw.Draw(img)
    d.rounded_rectangle((cx - rw, top, cx + rw, bot), radius=rw, fill=(30, 10, 40, 230), outline=(255, 230, 255, 255), width=SS)
    cr = (2.6 + 0.8 * k) * SS
    for r, a in ((cr * 1.9, 90), (cr * 1.3, 170), (cr, 255)):
        d.ellipse((cx - r, 16 * SS - r * 1.6, cx + r, 16 * SS + r * 1.6), fill=core + (a,))
    d.ellipse((cx - cr * 0.45, 16 * SS - cr * 0.7, cx + cr * 0.45, 16 * SS + cr * 0.7), fill=(255, 255, 255, 255))
    # sparks climbing
    for j in range(5):
        u = (t * (1 + j % 2) + j / 5) % 1
        y = bot - 2 * SS - u * (bot - top - 4 * SS)
        x = cx + math.sin(TAU * (u * 2 + j / 5)) * (rw - 2.5 * SS)
        a = int(255 * math.sin(math.pi * u))
        d.ellipse((x - 0.8 * SS, y - 0.8 * SS, x + 0.8 * SS, y + 0.8 * SS), fill=(255, 250, 220, a))
    # gold caps and a glint on the glass
    for yc in (top, bot):
        d.rounded_rectangle((cx - rw - SS, yc - 1.6 * SS, cx + rw + SS, yc + 1.6 * SS), radius=SS, fill=(240, 184, 56, 255), outline=(110, 70, 10, 255), width=SS // 2)
    g = (t * 1.0) % 1
    gy = top + 2 * SS + g * (bot - top - 4 * SS)
    d.line([(cx - rw + 1.5 * SS, gy), (cx - rw + 1.5 * SS, gy + 3 * SS)], fill=(255, 255, 255, int(200 * math.sin(math.pi * g))), width=SS)
    return img.resize((S, S), Image.LANCZOS)


def save(name, fn):
    strip = Image.new('RGBA', (S, S * N))
    for f in range(N):
        strip.paste(fn(f), (0, f * S))
    strip.save(os.path.join(OUT, name + '.png'))
    open(os.path.join(OUT, name + '.png.mcmeta'), 'w').write('{"animation": {"frametime": 2, "interpolate": false}}')


if __name__ == '__main__':
    save('dodecaract_excavator', excavator)
    save('eschaton_cell', cell)
    print('round 32 gear textures written')
