"""
Skins for the containment-failure creatures, built by recolouring vanilla's own entity textures (so the UV layout
always matches the vanilla model geometry the renderers use), plus an emissive "glow" layer for each, and
18x18 icons for the new status effects.
"""
import io, os, zipfile, colorsys
import numpy as np
from PIL import Image

ROOT = os.path.dirname(os.path.abspath(__file__))
TEX = os.path.join(ROOT, '..', 'src/main/resources/assets/theoreticalsciences/textures')
JAR = __import__("mcpaths").CLIENT_JAR


def vanilla(path):
    with zipfile.ZipFile(JAR) as z:
        return np.asarray(Image.open(io.BytesIO(z.read('assets/minecraft/textures/' + path))).convert('RGBA')).astype(float) / 255


def save(img, path):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    Image.fromarray((np.clip(img, 0, 1) * 255).astype(np.uint8), 'RGBA').save(path)


def tint(img, dark, light, keep=0.25):
    """Map luminance onto a dark->light colour ramp, keeping a little of the original shading detail."""
    lum = img[..., :3] @ np.array([0.3, 0.59, 0.11])
    lum = np.clip((lum - lum[img[..., 3] > 0].min()) / max(1e-3, np.ptp(lum[img[..., 3] > 0])), 0, 1)
    out = img.copy()
    col = np.asarray(dark)[None, None] * (1 - lum[..., None]) + np.asarray(light)[None, None] * lum[..., None]
    out[..., :3] = col * (1 - keep) + img[..., :3] * keep
    return out


def glow_from(img, mask, rgb):
    g = np.zeros_like(img)
    g[mask, :3] = rgb
    g[mask, 3] = 1
    return g


def hexc(h):
    return np.array([int(h[i:i + 2], 16) / 255 for i in (1, 3, 5)])


def veins(shape, seed, density=0.06):
    rng = np.random.default_rng(seed)
    m = np.zeros(shape[:2], bool)
    h, w = shape[:2]
    for _ in range(int(h * w * density / 6)):
        y, x = rng.integers(0, h), rng.integers(0, w)
        for _ in range(6):
            m[y % h, x % w] = True
            y += rng.integers(-1, 2)
            x += rng.integers(-1, 2)
    return m


def main():
    # Destabilised Husk: sickly green-grey flesh, glowing green veins and eyes
    z = vanilla('entity/zombie/zombie.png')
    husk = tint(z, hexc('#1c2414'), hexc('#8aa070'), 0.3)
    v = veins(z.shape, 3) & (z[..., 3] > 0)
    husk[v, :3] = hexc('#7cff40')
    save(husk, os.path.join(TEX, 'entity/destabilised_husk.png'))
    eyes = np.zeros_like(z)
    eyes[12:13, 9:11] = [0.6, 1, 0.3, 1]
    eyes[12:13, 13:15] = [0.6, 1, 0.3, 1]
    g = glow_from(z, v, hexc('#7cff40'))
    g = np.maximum(g, eyes)
    save(g, os.path.join(TEX, 'entity/destabilised_husk_glow.png'))

    # (Paradox Echo, Void Wraith, Nanite Swarm and Law Wraith are drawn procedurally in code - no skins needed)
    ys, xs = np.mgrid[0:64, 0:64]

    # status effect icons
    for name, col, shape in (('reality_destabilisation', '#80ffe0', 'fractal'), ('nanite_infection', '#40ffe0', 'grid'),
                             ('temporal_dissonance', '#ffc23d', 'clock'), ('decoherence', '#80f0ff', 'scatter'), ('vacuum_decay', '#6080c0', 'ring'),
                             ('infinite_regress', '#ffd060', 'nested'), ('spaghettification', '#ff5020', 'bars'), ('time_dilation', '#8060ff', 'clock'),
                             ('dimensional_bleed', '#a080ff', 'nested'), ('law_flux', '#40ffc0', 'grid'), ('imperfection', '#e0e8ff', 'ring'),
                             ('dissolution', '#ffffff', 'scatter'),
                             # round 41 (1.3.3): the effects added in 1.1
                             ('lithostatic_pressure', '#c09060', 'pressure'), ('seismic_vertigo', '#d0a070', 'quake'),
                             ('magnetized', '#ff5050', 'magnet'), ('frame_drag', '#80a0ff', 'spiral'),
                             ('information_loss', '#c080ff', 'swap'), ('variance', '#f0e0a0', 'dice')):
        n = 18
        ys2, xs2 = np.mgrid[0:n, 0:n] - (n - 1) / 2
        r = np.hypot(xs2, ys2)
        a = np.arctan2(ys2, xs2)
        img = np.zeros((n, n, 4))
        if shape == 'fractal':
            # a square splitting into ever smaller offset copies (Sierpinski carpet-like)
            xi, yi = (xs2 + 9).astype(int), (ys2 + 9).astype(int)
            m = np.ones_like(r, dtype=bool)
            for k in (1, 3):
                m &= ~(((xi // k) % 3 == 1) & ((yi // k) % 3 == 1))
            m &= (np.abs(xs2) < 8) & (np.abs(ys2) < 8)
        elif shape == 'scatter':
            rng = np.random.default_rng(len(name))
            m = np.zeros_like(r, dtype=bool)
            for _ in range(9):
                cx, cy = rng.integers(-6, 7, 2)
                m |= (np.abs(xs2 - cx) < 1.2) & (np.abs(ys2 - cy) < 1.2)
        elif shape == 'ring':
            m = (np.abs(r - 6) < 1.3) | (r < 1.6)
        elif shape == 'nested':
            k = np.maximum(np.abs(xs2), np.abs(ys2))
            m = ((np.abs(k - 7.5) < 0.8) | (np.abs(k - 4.5) < 0.8) | (np.abs(k - 1.5) < 0.8))
        elif shape == 'bars':
            m = ((xs2.astype(int) % 3 == 0) & (np.abs(ys2) < 8 - np.abs(xs2) * 0.4)) & (np.abs(xs2) < 8)
        elif shape == 'pressure':
            # a slab pressed down by three arrows
            m = (ys2 > 5) & (ys2 < 7.5) & (np.abs(xs2) < 8)
            for cx in (-5, 0, 5):
                m |= (np.abs(xs2 - cx) < 0.8) & (ys2 > -7) & (ys2 < 2)
                m |= (ys2 >= 1) & (ys2 < 4) & (np.abs(xs2 - cx) < 3.5 - (ys2 - 1))
        elif shape == 'quake':
            # a seismograph trace
            m = (np.abs(ys2 - 5 * np.sin(xs2 * 1.3) * np.exp(-(xs2 / 5) ** 2)) < 1.1) & (np.abs(xs2) < 8)
        elif shape == 'magnet':
            # a horseshoe magnet
            m = ((np.abs(r - 5.5) < 1.6) & (ys2 > 0)) | ((np.abs(np.abs(xs2) - 5.5) < 1.6) & (ys2 <= 0) & (ys2 > -7))
        elif shape == 'spiral':
            m = (np.abs(np.mod(r - a * 1.6, 3.6 * np.pi / 2) - 1.2) < 0.9) & (r < 8) & (r > 1)
        elif shape == 'swap':
            # two opposed arrows
            m = ((np.abs(ys2 + 3) < 0.8) & (np.abs(xs2) < 6)) | ((np.abs(ys2 - 3) < 0.8) & (np.abs(xs2) < 6))
            m |= (xs2 > 3) & (np.abs(ys2 + 3) < 3 - (xs2 - 3) * 0.9) & (xs2 < 7)
            m |= (xs2 < -3) & (np.abs(ys2 - 3) < 3 - (-xs2 - 3) * 0.9) & (xs2 > -7)
        elif shape == 'dice':
            m = ((np.abs(xs2) < 7) & (np.abs(ys2) < 7)) & ~((np.abs(xs2) < 5.5) & (np.abs(ys2) < 5.5))
            for cx, cy in ((-3, -3), (0, 0), (3, 3)):
                m |= np.hypot(xs2 - cx, ys2 - cy) < 1.3
        elif shape == 'grid':
            m = (((xs2.astype(int) % 3 == 0) | (ys2.astype(int) % 3 == 0)) & (r < 7.5)) & ~(r < 2)
        else:
            m = (np.abs(r - 6.5) < 1) | ((np.abs(xs2) < 0.7) & (ys2 < 0) & (ys2 > -5)) | ((np.abs(ys2) < 0.7) & (xs2 > 0) & (xs2 < 4))
        c = hexc(col)
        img[m, :3] = c
        img[m, 3] = 1
        edge = np.zeros_like(m)
        for dy, dx in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            edge |= np.roll(m, (dy, dx), (0, 1))
        edge &= ~m
        img[edge, :3] = c * 0.3
        img[edge, 3] = 1
        save(img, os.path.join(TEX, 'mob_effect', name + '.png'))
    print('entity textures done')


if __name__ == '__main__':
    main()
