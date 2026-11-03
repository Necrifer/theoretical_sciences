"""Seamless space backgrounds for the Modonomicon Codex: a nebula base and three star layers for parallax."""
import os
import numpy as np
from PIL import Image

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'src/main/resources/assets/theoreticalsciences/textures/gui/codex')
N = 512
rng = np.random.default_rng(1729)


def periodic_noise(beta, seed):
    """Fractal noise that tiles perfectly (built in frequency space)."""
    r = np.random.default_rng(seed)
    f = np.fft.fftfreq(N)
    k = np.sqrt(f[:, None] ** 2 + f[None, :] ** 2)
    k[0, 0] = 1
    spec = (r.normal(size=(N, N)) + 1j * r.normal(size=(N, N))) / k ** beta
    spec[0, 0] = 0
    n = np.real(np.fft.ifft2(spec))
    return (n - n.min()) / (n.max() - n.min())


def stars(count, size, bright, seed):
    r = np.random.default_rng(seed)
    a = np.zeros((N, N, 4))
    ys, xs = np.mgrid[-4:5, -4:5]
    for _ in range(count):
        x, y = r.integers(0, N, 2)
        b = bright * (0.4 + 0.6 * r.random())
        tint = np.array([0.75 + 0.25 * r.random(), 0.8 + 0.2 * r.random(), 1.0])
        if r.random() < 0.15: tint = np.array([1.0, 0.85, 0.6])
        rad = size * (0.6 + r.random())
        g = np.exp(-(xs ** 2 + ys ** 2) / (2 * rad ** 2)) * b
        for dy in range(-4, 5):
            for dx in range(-4, 5):
                v = g[dy + 4, dx + 4]
                if v < 0.02: continue
                px, py = (x + dx) % N, (y + dy) % N
                a[py, px, :3] = np.maximum(a[py, px, :3], tint * v)
                a[py, px, 3] = max(a[py, px, 3], min(1, v * 1.3))
    return a


def save(a, name):
    Image.fromarray((np.clip(a, 0, 1) * 255).astype(np.uint8), 'RGBA').save(os.path.join(OUT, name))


# base: deep blue-black with violet and teal nebulae and a dust lane
n1, n2, n3 = periodic_noise(1.6, 1), periodic_noise(1.9, 2), periodic_noise(1.3, 3)
neb_v = np.clip((n1 - 0.45) * 2.2, 0, 1) ** 1.6
neb_t = np.clip((n2 - 0.5) * 2.4, 0, 1) ** 1.8
dust = np.clip((n3 - 0.35) * 1.6, 0, 1)
base = np.zeros((N, N, 4))
base[..., 0] = 0.02 + 0.30 * neb_v + 0.05 * neb_t
base[..., 1] = 0.02 + 0.06 * neb_v + 0.22 * neb_t
base[..., 2] = 0.06 + 0.38 * neb_v + 0.30 * neb_t
base[..., :3] *= (0.55 + 0.45 * dust[..., None])
base[..., 3] = 1
faint = stars(900, 0.45, 0.55, 11)
base[..., :3] = np.maximum(base[..., :3], faint[..., :3])
save(base, 'space_0.png')
save(stars(260, 0.55, 0.85, 12), 'space_1.png')
save(stars(90, 0.8, 1.0, 13), 'space_2.png')
# the nearest layer: a few big glowing stars and thin wisps of gas
s3 = stars(22, 1.6, 1.0, 14)
w = np.clip((periodic_noise(2.2, 5) - 0.62) * 3, 0, 1) * 0.25
s3[..., 0] = np.maximum(s3[..., 0], w * 0.7)
s3[..., 1] = np.maximum(s3[..., 1], w * 0.8)
s3[..., 2] = np.maximum(s3[..., 2], w)
s3[..., 3] = np.maximum(s3[..., 3], w)
save(s3, 'space_3.png')
print('codex space done')
