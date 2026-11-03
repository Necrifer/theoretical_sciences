"""
Hand-drawn block textures that the generic generators would get wrong (or overwrite): the Interior's rocks.
Runs after ore_textures.py in regenerate.sh, so the ores drawn here are not rebuilt on their host afterwards.

- core_iron: Iron-Nickel rock, warm rust-brown with silvery kamacite grains (a stony-iron, not a metal block)
- core_magnetite_ore: the same rock studded with big blue-black octahedral magnetite crystals with electric-blue
  edges, so it reads as something different from several blocks away
- inner_core_boundary: near-black rock with golden seams
- geode_shell: a pale, rough calcite crust with agate banding, like the rind of a broken geode
"""
import os
import numpy as np
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
TEX = os.path.join(HERE, '..', 'src', 'main', 'resources', 'assets', 'theoreticalsciences', 'textures', 'block')
N = 16


def h(s):
    s = s.lstrip('#')
    return np.array([int(s[i:i + 2], 16) for i in (0, 2, 4)], dtype=float)


def smooth(a, k=1):
    for _ in range(k):
        a = (a + np.roll(a, 1, 0) + np.roll(a, 1, 1) + np.roll(a, -1, 0) + np.roll(a, -1, 1)) / 5
    return a


def rock(seed, base, dark, fleck=None, fleck_p=0.07, vein=None):
    rng = np.random.default_rng(seed)
    sm = smooth(rng.random((N, N)))
    t = np.clip(sm * 1.7 - 0.3, 0, 1)[..., None]
    img = dark + (base - dark) * t
    if vein is not None:
        ys, xs = np.mgrid[0:N, 0:N]
        v = np.abs(np.sin(xs * 0.7 + ys * 0.35 + 1.3) + 0.6 * np.sin(ys * 1.1 - xs * 0.4)) < 0.22
        img[v] = vein
    if fleck is not None:
        f = rng.random((N, N)) < fleck_p
        img[f] = fleck
        f2 = np.roll(f, 1, 1) & (rng.random((N, N)) < 0.5)
        img[f2] = fleck * 0.7
    return img


def save(name, img):
    a = np.clip(img, 0, 255).astype(np.uint8)
    out = np.dstack([a, np.full((N, N), 255, np.uint8)])
    Image.fromarray(out, 'RGBA').save(os.path.join(TEX, name + '.png'))
    mc = os.path.join(TEX, name + '.png.mcmeta')
    if os.path.exists(mc):
        os.remove(mc)


def iron_nickel():
    # warm, rusty stony-iron: brown-grey matrix, orange rust blooms, silvery metal grains
    img = rock(5, h('#6a5444'), h('#2e241e'), fleck=h('#cfc8b8'), fleck_p=0.06)
    rng = np.random.default_rng(11)
    rust = smooth(rng.random((N, N)), 2) > 0.58
    img[rust] = img[rust] * 0.55 + h('#9a4a22') * 0.45
    return img


def magnetite(base):
    """Big octahedral crystals: a diamond outline, black faces split light/dark, electric-blue rims and a spark."""
    img = base.copy() * 0.8
    # (centre x, centre y, half size)
    crystals = [(4, 4, 3), (11, 6, 3), (6, 11, 3), (12, 12, 2), (2, 13, 1), (14, 2, 1)]
    face_l, face_d, rim, spark = h('#2a3040'), h('#0c0e16'), h('#3aa8ff'), h('#c8f0ff')
    for cx, cy, r in crystals:
        # a shadow down and right, so each crystal sits proud of the rock
        for y in range(N):
            for x in range(N):
                d = abs(x - cx - 1) + abs(y - cy - 1)
                if d <= r and img is not None:
                    img[y, x] = img[y, x] * 0.5
        for y in range(N):
            for x in range(N):
                d = abs(x - cx) + abs(y - cy)
                if d > r:
                    continue
                if d == r:
                    img[y, x] = rim if (x <= cx or y <= cy) else rim * 0.55
                else:
                    img[y, x] = face_l if (x - cx) < (y - cy) else face_d   # the two halves catch light differently
        img[cy - r + (1 if r > 1 else 0), cx] = spark
    return img


def boundary():
    return rock(9, h('#2a241c'), h('#120e0a'), fleck=h('#f0c060'), vein=h('#a07020'))


def geode_shell():
    # a rough pale calcite crust with concentric agate bands and small pits
    rng = np.random.default_rng(23)
    ys, xs = np.mgrid[0:N, 0:N]
    wob = smooth(rng.random((N, N)), 2) * 3.0
    r = np.sqrt((xs - 2.5) ** 2 + (ys - 17.0) ** 2) + wob          # bands curve around a point off the bottom-left corner
    band = (np.floor(r / 2.6) % 4).astype(int)
    pal = [h('#e9e4d8'), h('#cfc6b6'), h('#ded8e6'), h('#b8aea0')]  # white, tan-grey, faint lavender, darker tan
    img = np.stack([pal[b] for b in band.ravel()]).reshape(N, N, 3)
    grain = smooth(rng.random((N, N)))[..., None]
    img = img * (0.86 + 0.22 * grain)
    pits = rng.random((N, N)) < 0.06
    img[pits] = img[pits] * 0.68
    img[np.roll(pits, -1, 0) & ~pits] *= 1.08                          # a lit lip above each pit
    return img


def cloud(seed, dark, light, alpha_lo=70, alpha_hi=170):
    """1.10.1: a nebular cloud - soft, translucent puffs (seamless, so neighbouring clouds merge)."""
    rng = np.random.default_rng(seed)
    t = smooth(rng.random((N, N)), 3)
    t = (t - t.min()) / (t.max() - t.min() + 1e-9)
    rgb = dark + (light - dark) * t[..., None]
    a = alpha_lo + (alpha_hi - alpha_lo) * t
    sp = rng.random((N, N)) < 0.03                                      # a few bright grains or ionised knots
    rgb[sp] = light * 1.15
    a[sp] = 220
    out = np.dstack([np.clip(rgb, 0, 255), a]).astype(np.uint8)
    return out


def save_rgba(name, out):
    Image.fromarray(out, 'RGBA').save(os.path.join(TEX, name + '.png'))
    mc = os.path.join(TEX, name + '.png.mcmeta')
    if os.path.exists(mc):
        os.remove(mc)


if __name__ == '__main__':
    base = iron_nickel()
    save('core_iron', base)
    save('core_magnetite_ore', magnetite(base))
    save('inner_core_boundary', boundary())
    save('geode_shell', geode_shell())
    save_rgba('dust_cloud', cloud(31, h('#3a2c24'), h('#a88e74'), 110, 200))
    save_rgba('hydrogen_cloud', cloud(32, h('#701428'), h('#ff6a88')))
    save_rgba('oxygen_cloud', cloud(33, h('#0e4a48'), h('#66ffd8')))
    print('custom textures drawn: 7')
