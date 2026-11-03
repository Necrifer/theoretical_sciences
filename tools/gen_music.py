"""
Original music for Theoretical Sciences, synthesised from scratch (no samples): one piece per dimension, one for the
Living Proof's chambers and one for the fight with Alpha Omega.

Writes stereo Ogg Vorbis to assets/theoreticalsciences/sounds/music/<track>.ogg and adds streamed music events to
sounds.json (run after gen_sounds.py, which rewrites that file).

    python3 gen_music.py [track ...]
"""
import json
import os
import subprocess
import sys
import tempfile
from multiprocessing import Pool

import numpy as np
from scipy import signal
from scipy.io import wavfile

SR = 32000
ROOT = os.path.dirname(os.path.abspath(__file__))
A = os.path.join(ROOT, '..', 'src/main/resources/assets/theoreticalsciences')
OUT = os.path.join(A, 'sounds', 'music')
PHI = (1 + 5 ** 0.5) / 2


# ============================================================ instruments
def hz(m):
    return 440.0 * 2 ** ((m - 69) / 12.0)


def tt(dur):
    return np.arange(int(dur * SR)) / SR


def adsr(n, a, d, s, r):
    e = np.full(n, s, dtype=np.float64)
    na, nd, nr = int(a * SR), int(d * SR), int(r * SR)
    na, nd = min(na, n), min(nd, max(0, n - na))
    e[:na] = np.linspace(0, 1, na, endpoint=False) if na else e[:na]
    e[na:na + nd] = np.linspace(1, s, nd, endpoint=False) if nd else e[na:na + nd]
    if nr and n > 0:
        nr = min(nr, n)
        e[n - nr:] *= np.linspace(1, 0, nr)
    return e


def saw(f, t, harm=14):
    out = np.zeros_like(t)
    for k in range(1, harm + 1):
        if f * k > SR * 0.45: break
        out += ((-1) ** (k + 1)) * np.sin(2 * np.pi * f * k * t) / k
    return out * 0.6


def tri(f, t):
    return 2 / np.pi * np.arcsin(np.sin(2 * np.pi * f * t))


def lowpass(x, fc, order=2):
    b, a = signal.butter(order, min(fc, SR * 0.45) / (SR / 2), 'low')
    return signal.lfilter(b, a, x)


def highpass(x, fc, order=2):
    b, a = signal.butter(order, fc / (SR / 2), 'high')
    return signal.lfilter(b, a, x)


def bandpass(x, lo, hi, order=2):
    b, a = signal.butter(order, [lo / (SR / 2), min(hi, SR * 0.45) / (SR / 2)], 'band')
    return signal.lfilter(b, a, x)


def pad(midis, dur, bright=1500, attack=2.0, release=2.5, detune=0.006, rng=None):
    t = tt(dur)
    x = np.zeros_like(t)
    rng = rng or np.random.default_rng(0)
    for m in midis:
        f = hz(m)
        for d in (-detune, 0, detune):
            x += saw(f * (1 + d), t + rng.random(), 10)
    x = lowpass(x, bright, 2)
    return x * adsr(len(t), attack, 0.5, 0.85, release) / max(1, len(midis)) * 0.5


def choir(midis, dur, vowel='a', attack=1.5, release=2.0):
    """Saw voices through vowel formants, with a slow vibrato."""
    F = {'a': (730, 1090, 2440), 'o': (570, 840, 2410), 'u': (300, 870, 2240), 'e': (530, 1840, 2480)}[vowel]
    t = tt(dur)
    x = np.zeros_like(t)
    for m in midis:
        f = hz(m)
        vib = 1 + 0.004 * np.sin(2 * np.pi * 5.2 * t + m)
        ph = 2 * np.pi * np.cumsum(f * vib) / SR
        v = np.zeros_like(t)
        for k in range(1, 24):
            if f * k > SR * 0.45: break
            v += np.sin(ph * k) / k
        x += v
    y = sum(bandpass(x, fc * 0.85, fc * 1.15) * g for fc, g in zip(F, (1.0, 0.6, 0.25)))
    return y * adsr(len(t), attack, 0.4, 0.9, release) / max(1, len(midis)) * 0.9


def bell(m, dur=3.0, index=2.5, ratio=3.5):
    f = hz(m)
    t = tt(dur)
    env = np.exp(-t * 2.2)
    mod = index * np.exp(-t * 3.0) * np.sin(2 * np.pi * f * ratio * t)
    return np.sin(2 * np.pi * f * t + mod) * env * 0.5


def musicbox(m, dur=2.0):
    f = hz(m)
    t = tt(dur)
    return (np.sin(2 * np.pi * f * t) + 0.3 * np.sin(2 * np.pi * f * 4 * t) * np.exp(-t * 9)) * np.exp(-t * 3.5) * 0.45


def pluck(m, dur=1.5, damp=0.996, bright=0.5):
    f = hz(m)
    n = int(dur * SR)
    N = max(2, int(SR / f))
    rng = np.random.default_rng(int(m * 7))
    exc = np.zeros(n)
    exc[:N] = lowpass(rng.uniform(-1, 1, N), 2000 + 6000 * bright, 1)
    a = np.zeros(N + 2)
    a[0] = 1
    a[N] = -damp * 0.5
    a[N + 1] = -damp * 0.5
    y = signal.lfilter([1.0], a, exc)
    return y * 0.5 * adsr(n, 0.001, 0.05, 1, 0.05)


def sine(m, dur, a=0.01, r=0.3):
    t = tt(dur)
    return np.sin(2 * np.pi * hz(m) * t) * adsr(len(t), a, 0.1, 0.9, r) * 0.4


def sub(m, dur, a=1.0, r=1.5):
    t = tt(dur)
    f = hz(m)
    return (np.sin(2 * np.pi * f * t) + 0.25 * np.sin(4 * np.pi * f * t)) * adsr(len(t), a, 0.2, 1, r) * 0.6


def bass(m, dur, cutoff=900):
    t = tt(dur)
    x = saw(hz(m), t, 18) + 0.5 * np.sin(2 * np.pi * hz(m - 12) * t)
    env = adsr(len(t), 0.005, 0.12, 0.6, 0.06)
    return lowpass(x, cutoff, 2) * env * 0.55


def kick(dur=0.45):
    t = tt(dur)
    f = 45 + 110 * np.exp(-t * 28)
    return np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-t * 7) * 1.0


def snare(dur=0.3, rng=None):
    rng = rng or np.random.default_rng(3)
    t = tt(dur)
    n = bandpass(rng.uniform(-1, 1, len(t)), 1200, 7000) * np.exp(-t * 18)
    tone = np.sin(2 * np.pi * 185 * t) * np.exp(-t * 25)
    return (n * 0.9 + tone * 0.5) * 0.7


def hat(dur=0.08, rng=None, open_=False):
    rng = rng or np.random.default_rng(4)
    t = tt(dur * (4 if open_ else 1))
    return highpass(rng.uniform(-1, 1, len(t)), 7000) * np.exp(-t * (12 if open_ else 60)) * 0.25


def heartbeat(dur=0.5):
    t = tt(dur)
    lub = np.sin(2 * np.pi * (40 + 30 * np.exp(-t * 20)) * t) * np.exp(-t * 14)
    return lowpass(lub, 300) * 0.9


def swell(m, dur, reverse=True):
    """A sound that grows instead of fading - a reversed bell."""
    x = bell(m, dur, index=1.5)
    return x[::-1] if reverse else x


def shepard(dur, base=24, rate=0.04, octaves=8):
    """An endlessly rising tone: octave-spaced partials sliding up under a fixed bell-shaped loudness."""
    t = tt(dur)
    x = np.zeros_like(t)
    for k in range(octaves):
        pos = (k + rate * t) % octaves
        f = hz(base) * 2 ** pos
        amp = np.exp(-((pos - octaves / 2) ** 2) / 3.0)
        x += np.sin(2 * np.pi * np.cumsum(f) / SR) * amp
    return x * 0.15


# ============================================================ mixing
class Mix:
    def __init__(self, dur):
        self.n = int(dur * SR)
        self.L = np.zeros(self.n)
        self.R = np.zeros(self.n)
        self.dur = dur

    def add(self, at, x, pan=0.0, gain=1.0):
        i = int(at * SR)
        if i >= self.n or i < 0: return
        x = x[:self.n - i] * gain
        lg, rg = np.cos((pan + 1) * np.pi / 4), np.sin((pan + 1) * np.pi / 4)
        self.L[i:i + len(x)] += x * lg * 1.414
        self.R[i:i + len(x)] += x * rg * 1.414

    def reverb(self, seconds=3.0, wet=0.35, bright=4000, seed=7):
        rng = np.random.default_rng(seed)
        n = int(seconds * SR)
        t = np.arange(n) / SR
        decay = np.exp(-t * 6.9 / seconds)
        irL = lowpass(rng.normal(size=n), bright, 1) * decay
        irR = lowpass(rng.normal(size=n), bright, 1) * decay
        irL /= np.sqrt(np.sum(irL ** 2))
        irR /= np.sqrt(np.sum(irR ** 2))
        L = signal.fftconvolve(self.L, irL)[:self.n]
        R = signal.fftconvolve(self.R, irR)[:self.n]
        self.L = self.L * (1 - wet) + L * wet * 1.6
        self.R = self.R * (1 - wet) + R * wet * 1.6

    def master(self, fade_in=3.0, fade_out=5.0):
        st = np.stack([self.L, self.R], 1)
        st = np.tanh(st / (np.max(np.abs(st)) + 1e-9) * 1.4) / np.tanh(1.4)
        fi, fo = int(fade_in * SR), int(fade_out * SR)
        st[:fi] *= np.linspace(0, 1, fi)[:, None]
        st[-fo:] *= np.linspace(1, 0, fo)[:, None]
        return (st * 0.89 * 32767).astype(np.int16)


def chord(root, kind):
    return [root + i for i in {'m': (0, 3, 7), 'M': (0, 4, 7), 'm7': (0, 3, 7, 10), 'M7': (0, 4, 7, 11), 'm9': (0, 3, 7, 10, 14),
                               'M9': (0, 4, 7, 11, 14), 'sus2': (0, 2, 7), 'sus4': (0, 5, 7), 'dim': (0, 3, 6), 'add9': (0, 4, 7, 14),
                               'cl': (0, 1, 7), 'aug': (0, 4, 8), '5': (0, 7, 12)}[kind]]


# ============================================================ the pieces
def the_bulk(rng):
    """Vast and drifting: a low D drone, slow minor-ninth pads, plucks falling between the branes, graviton swells."""
    dur = 132
    m = Mix(dur)
    m.add(0, sub(38, dur, 4, 6), 0, 0.5)
    m.add(0, sub(45, dur, 6, 6), 0, 0.25)
    prog = [(50, 'm9'), (46, 'M7'), (41, 'M9'), (48, 'sus2'), (50, 'm9'), (43, 'm7'), (46, 'M7'), (45, 'sus4')]
    span = 16
    for i in range(int(dur / span) + 1):
        r, k = prog[i % len(prog)]
        m.add(i * span, pad(chord(r, k), span + 3, 1100, 3.5, 3.5, rng=rng), rng.uniform(-0.3, 0.3), 0.9)
        notes = chord(r + 12, k) + chord(r + 24, k)
        t = i * span + rng.uniform(0, 1)
        while t < (i + 1) * span:
            if rng.random() < 0.55:
                m.add(t, pluck(int(rng.choice(notes)), 2.5, 0.997, 0.3), rng.uniform(-0.8, 0.8), 0.35)
            t += rng.choice([0.5, 0.75, 1.0, 1.5])
    for t in np.arange(10, dur - 10, 22):
        x = tt(6)
        sweep = np.sin(2 * np.pi * np.cumsum(60 + 40 * np.sin(np.pi * x / 6)) / SR) * np.sin(np.pi * x / 6) ** 2 * 0.3
        m.add(t + rng.uniform(0, 4), sweep, rng.uniform(-0.5, 0.5))
    m.reverb(5.0, 0.45, 3000)
    return m


def hyperdimensional_reality(rng):
    """Majestic, E Lydian: a choir over the World Tree, bell arpeggios climbing out of the Ocean of Law."""
    dur = 136
    m = Mix(dur)
    prog = [(52, 'M9'), (54, 'M'), (49, 'm7'), (57, 'M7'), (52, 'add9'), (54, 'M'), (56, 'm7'), (54, 'sus2')]
    span = 12
    for i in range(int(dur / span) + 1):
        r, k = prog[i % len(prog)]
        m.add(i * span, choir(chord(r, k), span + 2, 'a' if i % 2 == 0 else 'o', 2.5, 2.5), 0, 0.9)
        m.add(i * span, pad(chord(r - 12, '5'), span + 2, 900, 3, 3, rng=rng), 0, 0.5)
        tones = chord(r + 12, k)
        for j in range(28):
            if i < 1: break
            t = i * span + j * span / 28
            m.add(t, bell(tones[j % len(tones)] + 12 * ((j // len(tones)) % 2), 2.5, 1.8, 2.0), np.sin(j * 0.7) * 0.7, 0.18)
    m.add(0, sine(88, dur, 8, 8) * (0.5 + 0.5 * np.sin(2 * np.pi * 0.07 * tt(dur))), 0.4, 0.15)
    m.reverb(6.0, 0.5, 5000)
    return m


def platonic_realm(rng):
    """Crystalline and exact: pure fifths, and a music box whose notes fall on golden-ratio times."""
    dur = 118
    m = Mix(dur)
    scale = [60, 62, 64, 67, 69, 72, 74, 76, 79, 81]
    for i, (a, b) in enumerate([(48, 55), (53, 60), (45, 52), (50, 57)] * 4):
        m.add(i * 8, sine(a, 9, 2, 2) + sine(b, 9, 2, 2) + 0.5 * sine(a + 12, 9, 3, 2), 0, 0.6)
    k = 1
    while True:
        t = (k * PHI) % 1 * 2 + (k * 0.62)          # golden-ratio offsets on a slow grid
        if t > dur - 4: break
        note = scale[int(k * PHI * 7) % len(scale)]
        m.add(t, musicbox(note, 2.5), np.sin(k * PHI) * 0.6, 0.55)
        if k % 5 == 0: m.add(t, musicbox(note + 12, 2.5), -np.sin(k * PHI) * 0.6, 0.3)
        k += 1
    for t in np.arange(16, dur - 8, 16):
        for j, n in enumerate([72, 76, 79, 84]):
            m.add(t + j * 0.618, bell(n, 3, 1.2, 1.0), (j - 1.5) * 0.3, 0.2)
    m.reverb(4.0, 0.4, 7000)
    return m


def quantum_foam(rng):
    """Bubbling: clouds of tiny notes appearing and vanishing, pitched blips, a pulse that never settles."""
    dur = 112
    m = Mix(dur)
    scale = [54, 57, 59, 61, 64, 66, 69, 71, 73, 76, 78, 81]
    density = lambda t: 2 + 7 * (0.5 + 0.5 * np.sin(2 * np.pi * t / 23)) * (0.5 + 0.5 * np.sin(2 * np.pi * t / 7.3))
    t = 1.0
    while t < dur - 2:
        n = int(rng.choice(scale))
        if rng.random() < 0.6:
            m.add(t, pluck(n, 0.6, 0.98, 0.8), rng.uniform(-1, 1), 0.3)
        else:
            m.add(t, bell(n + 12, 0.8, 3.5, 1.41), rng.uniform(-1, 1), 0.12)
        t += rng.exponential(1.0 / density(t))
    for t in np.arange(0, dur, 0.37 * 8):
        x = tt(0.3)
        f = 300 + 1200 * rng.random()
        blip = np.sin(2 * np.pi * np.cumsum(f * (1 + 2 * x)) / SR) * np.exp(-x * 20) * 0.15
        m.add(t + rng.uniform(0, 2), blip, rng.uniform(-1, 1))
    for i in range(int(dur / 8)):
        m.add(i * 8, pad(chord(42 + [0, 3, -2, 5][i % 4], 'm'), 9, 700 + 400 * np.sin(i), 2, 2, rng=rng), 0, 0.6)
    m.reverb(3.0, 0.4, 6000)
    return m


def transfinite_hierarchy(rng):
    """Counting past infinity: a tone that rises forever, arpeggios that double their speed and start again."""
    dur = 124
    m = Mix(dur)
    m.add(0, shepard(dur, 24, 0.035), 0, 0.9)
    whole = [60, 62, 64, 66, 68, 70]
    bar = 4.0
    for b in range(int(dur / bar)):
        level = b % 4                                  # 1, 2, 4, 8 notes a beat... then omega: begin again, an octave up
        per = 2 ** level
        octave = 12 * ((b // 4) % 2)
        for j in range(4 * per):
            t = b * bar + j * bar / (4 * per)
            n = whole[(j + b) % len(whole)] + octave
            m.add(t, bell(n, 1.2 / per + 0.3, 1.5, 2.0), np.sin(j * 0.9) * 0.6, 0.22 / (1 + level * 0.3))
        if level == 0:
            m.add(b * bar, pad([48 + (b // 4) % 6 * 2, 52 + (b // 4) % 6 * 2, 56 + (b // 4) % 6 * 2], bar * 4 + 2, 1200, 2, 2, rng=rng), 0, 0.5)
    m.reverb(4.5, 0.42, 5000)
    return m


def black_hole_interior(rng):
    """Falling: a sub-bass that never ends, a pulse whose beats stretch further apart, metal groaning under tides."""
    dur = 124
    m = Mix(dur)
    m.add(0, sub(24, dur, 6, 8), 0, 0.35)
    m.add(0, pad([36, 37, 43, 48], dur, 800, 10, 10, rng=rng), 0, 1.0)
    for i in range(int(dur / 16)):                      # a slow, falling minor line high above the drone
        for j, n in enumerate([67, 63, 62, 60]):
            m.add(i * 16 + j * 4 + 2, sine(n - (i % 3), 4.5, 0.8, 2.0), np.sin(i + j) * 0.5, 0.35)
    t, gap = 2.0, 0.6
    while t < dur - 3:                                  # time dilation: every beat a little later than the last
        m.add(t, heartbeat(0.8), 0, 0.8)
        t += gap
        gap *= 1.035
        if gap > 4.5: gap = 0.6
    for t in np.arange(8, dur - 6, 13):
        x = bell(rng.choice([36, 39, 41]), 6, 4.0, 1.37)
        m.add(t + rng.uniform(0, 3), lowpass(x, 900), rng.uniform(-0.7, 0.7), 0.6)
    for t in np.arange(20, dur - 10, 20):
        m.add(t, swell(rng.choice([48, 51, 55]), 5), rng.uniform(-0.5, 0.5), 0.35)
    m.reverb(6.0, 0.5, 1800)
    return m


def white_hole_interior(rng):
    """Blinding: sounds that swell out of nothing and stop, bright major chords, arpeggios thrown outward."""
    dur = 112
    m = Mix(dur)
    prog = [(57, 'M9'), (62, 'M7'), (64, 'sus4'), (61, 'm7')]
    for i in range(int(dur / 8) + 1):
        r, k = prog[i % len(prog)]
        m.add(i * 8, pad(chord(r, k), 9, 4500, 1.0, 2, rng=rng), 0, 0.5)
        for j, n in enumerate(chord(r + 12, k)):
            m.add(i * 8 + 4 - 3.5, swell(n + 12, 3.5), (j - 1.5) * 0.4, 0.25)
        tones = sorted(chord(r + 12, k) + chord(r + 24, k), reverse=True)
        for j in range(16):
            m.add(i * 8 + 4 + j * 0.25, pluck(tones[j % len(tones)], 1.0, 0.99, 0.9), (j / 8.0 - 1) * 0.9, 0.22)
    m.add(0, sine(93, dur, 5, 5) * (0.6 + 0.4 * np.sin(2 * np.pi * 0.31 * tt(dur))), 0, 0.12)
    m.reverb(3.5, 0.45, 9000)
    return m


def transcendental(rng):
    """Everything, at rest: warm major sevenths, a slow bell melody, voices far away."""
    dur = 136
    m = Mix(dur)
    prog = [(49, 'M9'), (54, 'M7'), (46, 'm9'), (51, 'add9'), (49, 'M7'), (56, 'sus2'), (54, 'M9'), (51, 'sus4')]
    span = 10
    melody = [73, 75, 77, 80, 77, 75, 73, 72, 70, 72, 73, 68]
    for i in range(int(dur / span) + 1):
        r, k = prog[i % len(prog)]
        m.add(i * span, pad(chord(r, k), span + 3, 1600, 3, 3, rng=rng), 0, 0.7)
        if i % 2 == 1: m.add(i * span, choir(chord(r + 12, k)[:3], span + 2, 'o', 3, 3), 0, 0.5)
        if i > 0:
            for j in range(4):
                m.add(i * span + j * 2.5, bell(melody[(i * 4 + j) % len(melody)], 4, 1.2, 1.0), np.sin(i + j) * 0.4, 0.3)
    m.reverb(7.0, 0.55, 6000)
    return m


def living_proof(rng):
    """Inside a mind that is testing you: a heartbeat, a clock, a worrying ostinato, and something listening."""
    dur = 104
    bpm = 90
    beat = 60 / bpm
    m = Mix(dur)
    prog = [(50, 'm'), (46, 'M'), (43, 'm'), (45, 'M')]
    osti = [0, 3, 7, 10, 7, 3, 1, 3]
    for b in range(int(dur / beat)):
        t = b * beat
        m.add(t, heartbeat(0.5), 0, 0.7)
        m.add(t + beat * 0.28, heartbeat(0.4) * 0.6, 0, 0.5)
        if b % 2 == 1: m.add(t + beat * 0.5, hat(0.04, rng), 0.4, 0.22)
        bar = b // 4
        r, k = prog[bar % 4]
        if bar >= 2:
            for j in range(2):
                m.add(t + j * beat / 2, pluck(r + 12 + osti[(b * 2 + j) % 8], 0.5, 0.985, 0.6), -0.3, 0.4)
        if b % 16 == 0:
            m.add(t, pad(chord(r, k) + [r + 13], beat * 16 + 2, 900, 2, 2, rng=rng), 0, 0.6)
        if b % 32 == 24:
            m.add(t, choir([r + 24, r + 25], beat * 8, 'u', 3, 2), 0.2, 0.35)
    m.reverb(3.0, 0.3, 2500)
    return m


def alpha_omega(rng):
    """The fight: 140 bpm, drums, a driving bass, arpeggios, a choir - everything the Omnilex was made of, at once."""
    bpm = 140
    beat = 60 / bpm
    bars = 64
    dur = bars * 4 * beat + 4
    m = Mix(dur)
    prog = [(50, 'm'), (46, 'M'), (43, 'm'), (45, 'M'), (50, 'm'), (41, 'M'), (43, 'm'), (45, 'M')]
    melody = [74, 77, 76, 74, 73, 74, 77, 81, 79, 77, 76, 74, 73, 69, 73, 76]
    for bar in range(bars):
        t0 = bar * 4 * beat
        r, k = prog[bar % len(prog)]
        sec = 'intro' if bar < 8 else 'build' if bar < 16 else 'a' if bar < 32 else 'break' if bar < 40 else 'b' if bar < 60 else 'outro'
        if bar % 2 == 0:
            m.add(t0, choir(chord(r + 12, k), 8 * beat + 1, 'a' if sec != 'break' else 'o', 0.5 if sec in ('a', 'b') else 2, 1.5), 0, 0.55)
            m.add(t0, pad(chord(r, k), 8 * beat + 1, 1800 if sec in ('a', 'b') else 900, 0.3, 1.5, rng=rng), 0, 0.45)
        if sec == 'intro':
            for b in range(4): m.add(t0 + b * beat, heartbeat(0.5), 0, 0.8)
        if sec in ('build', 'a', 'b', 'outro'):
            for e in range(8):
                n = r - 12 + (0 if e % 2 == 0 else 12) + (7 if e in (3, 7) else 0)
                m.add(t0 + e * beat / 2, bass(n, beat / 2 * 0.95, 700 if sec == 'build' else 1100), 0, 0.7)
        if sec in ('a', 'b', 'break') or (sec == 'build' and bar >= 12):
            tones = chord(r + 24, k) + [r + 36]
            for s16 in range(16):
                m.add(t0 + s16 * beat / 4, pluck(tones[(s16 * 3) % len(tones)], 0.4, 0.98, 0.9), 0.5 if s16 % 2 else -0.5, 0.22)
        if sec in ('a', 'b'):
            for b in range(4):
                m.add(t0 + b * beat, kick(), 0, 0.9)
                if b in (1, 3): m.add(t0 + b * beat, snare(0.3, rng), 0.1, 0.6)
                for h in range(2): m.add(t0 + b * beat + h * beat / 2, hat(0.06, rng, open_=(h == 1 and b == 3)), 0.35, 0.5)
            if bar % 4 == 3:
                for f in range(4): m.add(t0 + 3 * beat + f * beat / 4, snare(0.2, rng), -0.2 + f * 0.1, 0.4 + f * 0.1)
        if sec == 'build' and bar >= 12:
            for s16 in range(16): m.add(t0 + s16 * beat / 4, snare(0.15, rng), 0, 0.15 + 0.05 * (bar - 12) + s16 * 0.01)
        if sec == 'b':
            for j in range(4):
                n = melody[((bar - 40) * 4 + j) % len(melody)]
                lead = saw(hz(n), tt(beat * 0.95), 10) * adsr(int(beat * 0.95 * SR), 0.01, 0.1, 0.7, 0.1)
                m.add(t0 + j * beat, lowpass(lead, 3500) * 0.35, 0.1, 0.8)
                m.add(t0 + j * beat, bell(n + 12, 1.0, 2.0, 2.0), -0.3, 0.12)
        if sec == 'break':
            m.add(t0, swell(r + 24, 4 * beat), 0, 0.3)
            for b in range(4): m.add(t0 + b * beat, heartbeat(0.5), 0, 0.6)
    m.reverb(2.2, 0.28, 6000)
    return m


def antiverse(rng):
    """CPT in music: a phrase, then its mirror (C/P: every interval inverted), then the phrase run backwards (T) with
    reversed bells that swell in instead of dying away - over a warm, slightly wrong drone in a copper key."""
    dur = 120
    m = Mix(dur)
    for i, ch in enumerate([(45, 52, 57), (43, 50, 55), (41, 48, 53), (40, 47, 52)] * 3):
        m.add(i * 10, pad(list(ch), 11, bright=1100, attack=3, release=3, rng=rng), 0, 0.5)
    phrase = [69, 72, 76, 74, 72, 71, 69, 64]
    times = [0, 0.5, 1.0, 2.0, 2.5, 3.0, 3.5, 4.5]
    axis = 69
    for k, t0 in enumerate(np.arange(4, dur - 14, 18)):
        mode = k % 3
        for n, t in zip(phrase, times):
            if mode == 0:
                m.add(t0 + t, bell(n, 2.2, 1.8, 2.0), -0.4, 0.35)
            elif mode == 1:                                   # mirrored: intervals inverted about the axis
                m.add(t0 + t, bell(2 * axis - n, 2.2, 1.8, 2.0), 0.4, 0.35)
            else:                                             # time-reversed: notes swell in, phrase runs backwards
                b = np.flip(bell(n, 2.2, 1.8, 2.0))
                m.add(t0 + (times[-1] - t), b, 0.0, 0.3)
    for t in np.arange(0, dur - 2, 9):
        m.add(t + rng.uniform(0, 3), sine(rng.choice([81, 84, 88]), 1.5, 0.3, 1.0), rng.uniform(-1, 1), 0.08)
    m.reverb(3.5, 0.45, 6000)
    return m


def planetary_interior(rng):
    """Deep and slow, E Phrygian: a sub-bass you feel more than hear, the irregular thud of rock settling (the gaps
    between them follow the Gutenberg-Richter law: many small, few large), low dark marimba lines, and magma blipping."""
    dur = 128
    m = Mix(dur)
    m.add(0, sub(28, dur, 6, 8), 0, 0.45)
    prog = [(40, 'm'), (41, 'M'), (40, 'sus4'), (38, 'm7'), (40, 'm'), (36, 'M7'), (41, 'M'), (40, 'm')]
    span = 16
    for i in range(int(dur / span) + 1):
        r, k = prog[i % len(prog)]
        m.add(i * span, pad(chord(r, k), span + 3, 700, 4.0, 4.0, rng=rng), rng.uniform(-0.25, 0.25), 0.8)
        scale = [r + d for d in (0, 1, 3, 5, 7, 8, 10)]
        t = i * span + 2
        while t < (i + 1) * span:
            if rng.random() < 0.45:
                n = int(rng.choice(scale)) + 12
                m.add(t, lowpass(pluck(n, 2.2, 0.994, 0.2), 1400), rng.uniform(-0.6, 0.6), 0.45)
            t += rng.choice([0.75, 1.0, 1.5])
    t = 3.0
    while t < dur - 4:                                   # quakes: magnitude from an exponential tail
        mag = min(1.0, 0.25 + rng.exponential(0.2))
        m.add(t, lowpass(kick(0.9), 160) * mag, rng.uniform(-0.4, 0.4), 0.9)
        if mag > 0.6: m.add(t + 0.05, lowpass(rng.normal(size=int(1.5 * SR)) * np.exp(-tt(1.5) * 3), 300) * 0.3 * mag, 0, 1)
        t += rng.uniform(1.5, 5.0)
    for t in np.arange(5, dur - 3, 1.7):                 # bubbles of magma bursting at the surface of a melt
        if rng.random() < 0.4:
            f0 = rng.uniform(180, 420)
            x = tt(0.25)
            blip = np.sin(2 * np.pi * np.cumsum(f0 * (1 + 2.5 * x)) / SR) * np.exp(-x * 18) * 0.12
            m.add(t + rng.uniform(0, 1), blip, rng.uniform(-0.8, 0.8))
    m.reverb(4.5, 0.4, 1500)
    return m


def accretion_frontier(rng):
    """Cold and wide, A Lydian: a drone like the hole's own rotation, bell arpeggios that orbit (each pattern a little
    faster as it falls inward), a pulsar ticking far off, pads that bend in pitch (the disk's Doppler shift), and now and
    then the rising chirp of two black holes merging."""
    dur = 140
    m = Mix(dur)
    m.add(0, sub(33, dur, 6, 8), 0, 0.4)
    x = tt(dur)
    m.add(0, np.sin(2 * np.pi * 55.0 * x + 0.6 * np.sin(2 * np.pi * x / 9.0)) * 0.08, 0, 1)
    prog = [(57, 'M9'), (59, 'M'), (57, 'M7'), (52, 'sus2'), (57, 'M9'), (62, 'M7'), (59, 'add9'), (57, 'sus2')]
    span = 17.5
    for i in range(int(dur / span) + 1):
        r, k = prog[i % len(prog)]
        pd = pad(chord(r - 12, k), span + 3, 1600, 3.0, 4.0, rng=rng)
        n = len(pd)
        warp = np.clip(np.arange(n) * (1 + 0.004 * np.sin(2 * np.pi * np.arange(n) / SR / 6.0)), 0, n - 1)   # Doppler: rate bends
        m.add(i * span, np.interp(warp, np.arange(n), pd), np.sin(i) * 0.4, 0.85)
        notes = [r + d for d in (0, 4, 6, 7, 11, 14)]
        t, step = i * span + 1.0, 0.6
        while t < (i + 1) * span - 1:
            m.add(t, bell(int(rng.choice(notes)) + 12, 2.6, 1.8, 2.0), np.sin(t * 1.3) * 0.8, 0.22)
            t += step
            step = max(0.25, step * 0.93)               # falling inward: the orbit tightens
    t = 1.0
    while t < dur - 2:                                   # a pulsar, regular as a clock, far away
        m.add(t, highpass(hat(0.04, rng), 3000) * 0.5, 0.7, 0.4)
        t += 1.337
    for t0 in (30.0, 78.0, 118.0):                       # gravitational-wave chirps: frequency and loudness rise to the merger
        T = 4.0
        x = tt(T)
        tau = np.clip(T + 0.02 - x, 0.02, None)
        f = 40 * (tau / T) ** (-3 / 8)
        chirp = np.sin(2 * np.pi * np.cumsum(np.minimum(f, 900)) / SR) * (tau / T) ** (-1 / 4) * 0.05
        chirp[-int(0.05 * SR):] *= np.linspace(1, 0, int(0.05 * SR))
        m.add(t0, chirp, rng.uniform(-0.3, 0.3))
    m.reverb(7.0, 0.5, 3500)
    return m


TRACKS = {f.__name__: f for f in (antiverse, the_bulk, hyperdimensional_reality, platonic_realm, quantum_foam, transfinite_hierarchy,
                                  black_hole_interior, white_hole_interior, transcendental, living_proof, alpha_omega,
                                  planetary_interior, accretion_frontier)}


def render(name):
    import zlib
    rng = np.random.default_rng(zlib.crc32(name.encode()))
    mix = TRACKS[name](rng)
    data = mix.master()
    os.makedirs(OUT, exist_ok=True)
    with tempfile.TemporaryDirectory() as d:
        wav = os.path.join(d, name + '.wav')
        wavfile.write(wav, SR, data)
        subprocess.run(['ffmpeg', '-y', '-loglevel', 'error', '-i', wav, '-c:a', 'libvorbis', '-q:a', '2', os.path.join(OUT, name + '.ogg')], check=True)
    return name, round(mix.dur, 1)


def register():
    p = os.path.join(A, 'sounds.json')
    sj = json.load(open(p)) if os.path.exists(p) else {}
    for name in TRACKS:
        sj[f'music.{name}'] = {'sounds': [{'name': f'theoreticalsciences:music/{name}', 'stream': True}]}
    json.dump(sj, open(p, 'w'), indent=1)


if __name__ == '__main__':
    names = sys.argv[1:] or list(TRACKS)
    if names != ['--register']:
        os.environ.setdefault('PYTHONHASHSEED', '0')
        with Pool(2) as pool:
            for n, d in pool.imap_unordered(render, names):
                print(f'{n}: {d}s')
    register()
    print('music registered:', len(TRACKS))
