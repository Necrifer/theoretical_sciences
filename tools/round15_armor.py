"""
Round 15 - armour textures for the Raiment of Transcendence.

Writes assets/theoreticalsciences/textures/models/armor/transcendence_layer_1.png (helmet, chestplate + arms, boots) and
transcendence_layer_2.png (leggings), 64x32, following the vanilla humanoid armour UV layout:

    head  box at (0,0)   8x8x8      body box at (16,16) 8x12x4
    arm   box at (40,16) 4x12x4     leg  box at (0,16)  4x12x4

A box at (u,v) with size (w,h,d) has: top (u+d, v, w x d), bottom (u+d+w, v, w x d),
right (u, v+d, d x h), front (u+d, v+d, w x h), left (u+d+w, v+d, d x h), back (u+2d+w, v+d, w x h).

Style: white-gold plates, a darker gold trim around every plate edge, and soft prismatic (rainbow) accents - a gem on
the crown and the chest, and a thin prismatic seam on the limbs.

Run:  python tools/round15_armor.py
"""
import colorsys
import os
import random

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, '..', 'src', 'main', 'resources', 'assets', 'theoreticalsciences', 'textures', 'models', 'armor')

PLATE_HI = (255, 251, 236)
PLATE_LO = (236, 222, 186)
TRIM = (214, 168, 64)
TRIM_DARK = (150, 108, 34)
GOLD_BRIGHT = (255, 214, 102)

rng = random.Random(15)


def mix(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3))


def prism(t, light=0.78, sat=0.55):
    r, g, b = colorsys.hls_to_rgb(t % 1.0, light, sat)
    return int(r * 255), int(g * 255), int(b * 255)


def put(img, x, y, c, a=255):
    img.putpixel((x, y), (c[0], c[1], c[2], a))


def plate(img, x0, y0, w, h, rows=None, trim=True, seam=None, hue=0.0):
    """Paint one face. rows=(first,last) limits which rows (relative to the face) are covered."""
    r0, r1 = rows if rows else (0, h)
    for yy in range(r0, r1):
        for xx in range(w):
            t = yy / max(1, h - 1)
            c = mix(PLATE_HI, PLATE_LO, t * 0.8)
            n = rng.randint(-6, 6)
            c = tuple(max(0, min(255, v + n)) for v in c)
            edge = xx == 0 or xx == w - 1 or yy == r0 or yy == r1 - 1
            if trim and edge:
                c = TRIM_DARK if (yy == r1 - 1 or xx == w - 1) else TRIM
            put(img, x0 + xx, y0 + yy, c)
    if seam is not None and w >= 3:
        # a thin prismatic seam running down the middle of the plate
        sx = x0 + seam
        for yy in range(r0 + 1, r1 - 1):
            put(img, sx, y0 + yy, prism(hue + yy * 0.07, 0.8, 0.5))


def box(img, u, v, w, h, d, rows=None, faces=('top', 'bottom', 'right', 'front', 'left', 'back'), **kw):
    """Paint the side faces of a box; rows limits the side faces' rows (top/bottom faces are always whole)."""
    if 'top' in faces:
        plate(img, u + d, v, w, d)
    if 'bottom' in faces:
        plate(img, u + d + w, v, w, d)
    sides = {'right': (u, v + d, d), 'front': (u + d, v + d, w), 'left': (u + d + w, v + d, d), 'back': (u + 2 * d + w, v + d, w)}
    for f, (x, y, fw) in sides.items():
        if f in faces:
            plate(img, x, y, fw, h, rows=rows, **kw)


def gem(img, cx, cy, hue, size=1):
    """A small prismatic gem: a bright core with a ring of shifting colour."""
    for dy in range(-size, size + 1):
        for dx in range(-size, size + 1):
            if abs(dx) + abs(dy) > size:
                continue
            core = dx == 0 and dy == 0
            c = (255, 255, 255) if core else prism(hue + (dx * 0.13 + dy * 0.21), 0.72, 0.7)
            put(img, cx + dx, cy + dy, c)


def layer1():
    img = Image.new('RGBA', (64, 32), (0, 0, 0, 0))
    # ---- crown (head box 8x8x8 at 0,0)
    box(img, 0, 0, 8, 8, 8)
    # front: open face below the brow, a gold brow band with a gem
    fx, fy = 8, 8
    for yy in range(3, 8):
        for xx in range(2, 6):
            img.putpixel((fx + xx, fy + yy), (0, 0, 0, 0))
    for xx in range(8):
        put(img, fx + xx, fy + 1, GOLD_BRIGHT)
        put(img, fx + xx, fy + 2, TRIM)
    gem(img, fx + 4, fy + 1, 0.55)
    # crown points along the top edge of every side face
    for x0 in (0, 8, 16, 24):
        for xx in range(0, 8, 2):
            put(img, x0 + xx, 8, GOLD_BRIGHT)
    # halo ring on the top face
    for xx in range(8, 16):
        put(img, xx, 0, TRIM)
        put(img, xx, 7, TRIM)
    for yy in range(8):
        put(img, 8, yy, TRIM)
        put(img, 15, yy, TRIM)
    for i, (xx, yy) in enumerate(((10, 2), (13, 2), (10, 5), (13, 5))):
        put(img, xx, yy, prism(i * 0.25, 0.8, 0.6))

    # ---- mantle (body box 8x12x4 at 16,16)
    box(img, 16, 16, 8, 12, 4)
    bx, by = 20, 20                                   # body front face
    for yy in range(12):
        put(img, bx + 3, by + yy, TRIM)               # central gold rib
        put(img, bx + 4, by + yy, TRIM)
    for xx in range(8):
        put(img, bx + xx, by + 7, TRIM_DARK)          # belt line
    gem(img, bx + 4, by + 3, 0.0, size=2)             # the prismatic heart
    # back: two gold spines
    for yy in range(12):
        put(img, 32 + 2, 20 + yy, TRIM)
        put(img, 32 + 5, 20 + yy, TRIM)

    # ---- arms (arm box 4x12x4 at 40,16): pauldrons and a prismatic seam
    box(img, 40, 16, 4, 12, 4, seam=2, hue=0.6)
    for x0, fw in ((40, 4), (44, 4), (48, 4), (52, 4)):
        for xx in range(fw):
            put(img, x0 + xx, 20, GOLD_BRIGHT)
            put(img, x0 + xx, 21, GOLD_BRIGHT if xx % 2 == 0 else TRIM)
            put(img, x0 + xx, 22, TRIM_DARK)

    # ---- treads (leg box 4x12x4 at 0,16, bottom 5 rows)
    box(img, 0, 16, 4, 12, 4, rows=(7, 12), faces=('bottom', 'right', 'front', 'left', 'back'))
    for x0 in (0, 4, 8, 12):
        for xx in range(4):
            put(img, x0 + xx, 27, GOLD_BRIGHT)
    put(img, 5, 29, prism(0.3, 0.8, 0.6))
    put(img, 6, 29, prism(0.4, 0.8, 0.6))
    return img


def layer2():
    img = Image.new('RGBA', (64, 32), (0, 0, 0, 0))
    # ---- greaves on the legs (leg box at 0,16, top 9 rows) with a prismatic seam on the front
    box(img, 0, 16, 4, 12, 4, rows=(0, 9), faces=('top', 'right', 'front', 'left', 'back'))
    for yy in range(1, 8):
        put(img, 5, 20 + yy, prism(0.1 + yy * 0.08, 0.8, 0.5))
    for x0 in (0, 4, 8, 12):                          # knee guards
        for xx in range(4):
            put(img, x0 + xx, 24, GOLD_BRIGHT)
            put(img, x0 + xx, 25, TRIM)
    # ---- girdle on the body (body box at 16,16, lower 5 rows)
    box(img, 16, 16, 8, 12, 4, rows=(7, 12), faces=('right', 'front', 'left', 'back'))
    for x0, fw in ((16, 4), (20, 8), (28, 4), (32, 8)):
        for xx in range(fw):
            put(img, x0 + xx, 27, GOLD_BRIGHT)
    gem(img, 24, 29, 0.75)
    return img


def main():
    os.makedirs(OUT, exist_ok=True)
    layer1().save(os.path.join(OUT, 'transcendence_layer_1.png'))
    layer2().save(os.path.join(OUT, 'transcendence_layer_2.png'))
    print('wrote', os.path.normpath(OUT))


if __name__ == '__main__':
    main()
