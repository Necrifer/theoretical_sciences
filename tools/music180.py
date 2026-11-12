"""
1.8.0: the music of Omnipotency. A slow hymn: a pad of voices holding chords that move by one note at a time, a bell
that says the same four notes in different orders over them, and nothing below the middle of the keyboard.

Run:  python3 tools/music180.py
"""
import math, os, subprocess, tempfile
import numpy as np
from scipy.io import wavfile

SR = 44100
OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'src/main/resources/assets/theoreticalsciences/sounds/music/omnipotency.ogg')
rng = np.random.RandomState(33550337 % (2 ** 31))


def hz(n):                      # midi note
    return 440.0 * 2 ** ((n - 69) / 12.0)


def voice(f, d, vib=0.004, bright=0.5):
    t = np.arange(int(d * SR)) / SR
    ph = 2 * np.pi * f * t + vib * f / 5.0 * np.sin(2 * np.pi * (4.6 + rng.rand() * 0.8) * t + rng.rand() * 6)
    x = np.sin(ph) + bright * 0.5 * np.sin(2 * ph) + bright * 0.25 * np.sin(3 * ph) + bright * 0.1 * np.sin(5 * ph)
    a = min(2.5, d / 3)
    env = np.minimum(1, t / a) * np.minimum(1, (d - t) / a)
    return x * np.sin(env * np.pi / 2) ** 2


def bell(f, d=6.0):
    t = np.arange(int(d * SR)) / SR
    x = sum(a * np.sin(2 * np.pi * f * r * t) * np.exp(-t * k) for r, a, k in [(1, 1, 0.7), (2.01, 0.45, 1.1), (3.02, 0.22, 1.8), (4.2, 0.12, 2.6), (5.43, 0.06, 3.5)])
    return x * np.minimum(1, t / 0.004)


# D major, moving one voice at a time: D, Bm, G, A sus, D...
CHORDS = [[62, 66, 69, 74], [62, 66, 71, 74], [62, 67, 71, 74], [64, 67, 71, 76], [64, 69, 73, 76], [62, 69, 74, 78], [62, 67, 71, 79], [62, 66, 69, 78],
          [61, 66, 69, 76], [62, 66, 71, 74], [62, 67, 71, 74], [62, 66, 69, 74]]
BAR = 9.0
total = int((len(CHORDS) * BAR + 8) * SR)
mix = np.zeros(total)
for i, ch in enumerate(CHORDS):
    for n in ch:
        for det in (-0.06, 0.05):
            v = voice(hz(n + det), BAR + 3.0, bright=0.35 if n < 70 else 0.2) * 0.11
            s = int(i * BAR * SR)
            mix[s:s + len(v)] += v
    notes = [ch[3] + 12, ch[1] + 12, ch[2] + 12, ch[3] + 12]
    order = rng.permutation(4)
    for k in range(3 if i % 2 else 4):
        b = bell(hz(notes[order[k]])) * 0.085
        s = int((i * BAR + 1.0 + k * 2.0 + rng.rand() * 0.15) * SR)
        mix[s:s + len(b)] += b[:max(0, total - s)]
# a little air: a slow echo
for delay, g in ((0.37, 0.35), (0.71, 0.22), (1.13, 0.12)):
    d = int(delay * SR)
    mix[d:] += mix[:-d] * g
fade = int(4 * SR)
mix[:fade] *= np.linspace(0, 1, fade)
mix[-fade:] *= np.linspace(1, 0, fade)
mix = np.tanh(mix / (np.max(np.abs(mix)) + 1e-9) * 1.2) * 0.8
with tempfile.NamedTemporaryFile(suffix='.wav', delete=False) as tf:
    wavfile.write(tf.name, SR, (mix * 32767).astype(np.int16))
    subprocess.run(['ffmpeg', '-v', 'error', '-y', '-i', tf.name, '-ac', '1', '-c:a', 'libvorbis', '-q:a', '4', OUT], check=True)
os.unlink(tf.name)
print('written', OUT, os.path.getsize(OUT), 'bytes', round(total / SR), 's')
