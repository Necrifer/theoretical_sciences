"""
Procedurally synthesised creature sounds (no samples - everything is generated here, so there are no licensing questions).
Writes mono Ogg Vorbis files to assets/theoreticalsciences/sounds/entity/<mob>/ and the matching sounds.json.
Mono matters: Minecraft only positions (pans / attenuates) mono sounds in 3D.
"""
import json, os, subprocess, tempfile
import numpy as np
from scipy import signal
from scipy.io import wavfile

ROOT = os.path.dirname(os.path.abspath(__file__))
A = os.path.join(ROOT, '..', 'src/main/resources/assets/theoreticalsciences')
OUT = os.path.join(A, 'sounds', 'entity')
SR = 44100
rng = np.random.default_rng(1234)


def T(d):
    return np.arange(int(SR * d)) / SR


def env(n, a=0.01, r=0.2, curve=2.0):
    """Attack / exponential-ish release envelope over n samples."""
    t = np.arange(n) / SR
    d = n / SR
    e = np.minimum(1, t / max(a, 1e-4))
    rel = np.clip((d - t) / max(r, 1e-4), 0, 1) ** curve
    return e * rel


def tone(freq, d, kind='sine'):
    """freq may be a number or an array (sweep); phase is integrated so sweeps are smooth."""
    n = int(SR * d)
    f = np.full(n, float(freq)) if np.isscalar(freq) else np.interp(np.linspace(0, 1, n), np.linspace(0, 1, len(freq)), freq)
    ph = 2 * np.pi * np.cumsum(f) / SR
    if kind == 'sine':
        return np.sin(ph)
    if kind == 'saw':
        return 2 * ((ph / (2 * np.pi)) % 1) - 1
    if kind == 'square':
        return np.sign(np.sin(ph))
    if kind == 'tri':
        return 2 * np.abs(2 * ((ph / (2 * np.pi)) % 1) - 1) - 1
    raise ValueError(kind)


def noise(d):
    return rng.uniform(-1, 1, int(SR * d))


def bp(x, lo, hi, order=2):
    b, a = signal.butter(order, [lo / (SR / 2), min(hi / (SR / 2), 0.99)], btype='band')
    return signal.lfilter(b, a, x)


def lp(x, f, order=2):
    b, a = signal.butter(order, min(f / (SR / 2), 0.99), btype='low')
    return signal.lfilter(b, a, x)


def hp(x, f, order=2):
    b, a = signal.butter(order, f / (SR / 2), btype='high')
    return signal.lfilter(b, a, x)


def crush(x, bits=6, hold=4):
    """Bit-crush and sample-rate reduce: the 'digital' sound of reality glitching."""
    q = 2 ** bits
    y = np.round(x * q) / q
    idx = (np.arange(len(y)) // hold) * hold
    return y[np.minimum(idx, len(y) - 1)]


def glitch(x, grain=0.04, p_repeat=0.25, p_reverse=0.15, p_drop=0.1):
    """Chops the sound into grains and randomly stutters, reverses or drops them."""
    g = int(SR * grain)
    out = []
    i = 0
    while i < len(x):
        chunk = x[i:i + g]
        r = rng.random()
        if r < p_drop:
            out.append(np.zeros_like(chunk))
        elif r < p_drop + p_reverse:
            out.append(chunk[::-1])
        elif r < p_drop + p_reverse + p_repeat:
            out.append(chunk)
            out.append(chunk * 0.8)
        else:
            out.append(chunk)
        i += g
    return np.concatenate(out)


def mix(*parts):
    n = max(len(p) for p in parts)
    y = np.zeros(n)
    for p in parts:
        y[:len(p)] += p
    return y


def place(buf, x, at):
    s = int(SR * at)
    e = min(len(buf), s + len(x))
    if s < len(buf):
        buf[s:e] += x[:e - s]


def clink(f0, d=0.25, partials=(1, 2.76, 5.4, 8.9), decay=12.0):
    """A small metal impact: inharmonic partials with fast exponential decay."""
    t = T(d)
    y = sum(np.sin(2 * np.pi * f0 * k * t + rng.random() * 6) * np.exp(-t * decay * (1 + k * 0.4)) / (1 + i)
            for i, k in enumerate(partials))
    y += hp(noise(d), 3000) * np.exp(-t * 80) * 0.6
    return y


def bell(f0, d=1.2):
    t = T(d)
    return sum(np.sin(2 * np.pi * f0 * k * t) * np.exp(-t * (1.5 + k)) * a for k, a in ((1, 1), (2.0, 0.5), (2.76, 0.4), (4.07, 0.25), (5.4, 0.15)))


def groan(d, f0=95, vib=5.0):
    """A vocal groan: sawtooth with vibrato through two formant filters."""
    t = T(d)
    f = f0 * (1 + 0.04 * np.sin(2 * np.pi * vib * t)) * (1 - 0.15 * t / d)
    src = tone(f, d, 'saw')
    y = bp(src, 350, 750) * 1.0 + bp(src, 900, 1400) * 0.6 + bp(src, 2200, 2800) * 0.2
    return y * env(len(y), 0.15, d * 0.5)


def norm(x, peak=0.85):
    x = x - np.mean(x)
    m = np.max(np.abs(x)) or 1
    fade = min(len(x), int(SR * 0.01))
    x = x / m * peak
    x[-fade:] *= np.linspace(1, 0, fade)
    x[:min(64, len(x))] *= np.linspace(0, 1, min(64, len(x)))
    return x


# ============================================================================= the creatures
def husk_ambient(v):
    d = 1.3 + v * 0.2
    y = groan(d, 85 + v * 12)
    y = glitch(y, 0.035 + v * 0.01)
    y = 0.6 * y + 0.5 * crush(y, 5, 6)
    return y


def husk_hurt(v):
    g = groan(0.35, 130 + v * 20, 9)
    burst = crush(bp(noise(0.25), 400, 5000), 4, 8) * env(int(SR * 0.25), 0.002, 0.2)
    return glitch(mix(g, burst * 0.7), 0.03, 0.4, 0.2, 0.05)


def husk_death(v):
    d = 1.8
    t = T(d)
    g = groan(1.0, 110, 3)
    g = g * np.linspace(1, 0.2, len(g))
    y = np.zeros(len(t))
    place(y, crush(g, 6, 3), 0)
    # disintegration: crackles that thin out and rise in pitch as the creature comes apart
    for i in range(140):
        at = (i / 140) ** 0.8 * (d - 0.1)
        f = 800 + 6000 * at / d
        n = int(SR * 0.02)
        place(y, bp(noise(0.02), f * 0.8, f * 1.2) * np.exp(-np.arange(n) / SR * 200) * (1 - at / d) * 1.5, at)
    return y


def husk_step(v):
    d = 0.14
    th = lp(noise(d), 300) * env(int(SR * d), 0.002, 0.12) * 2
    click = crush(hp(noise(0.03), 2000), 3, 6) * 0.4
    return mix(th, click)


def wraith_ambient(v):
    d = 2.2
    t = T(d)
    sub = (np.sin(2 * np.pi * (42 + v * 6) * t) + 0.6 * np.sin(2 * np.pi * (43.3 + v * 6) * t)) * 0.8
    n = noise(d)
    # a swirling band of noise (matter orbiting the horizon): three fixed bands cross-faded, so there are no clicks
    sw = np.zeros(len(t))
    for k, c in enumerate((180, 340, 620)):
        g = 0.5 + 0.5 * np.sin(2 * np.pi * 0.45 * t + v + k * 2.094)
        sw += bp(n, c * 0.7, c * 1.3) * g
    y = sub + sw * 1.5
    return y * env(len(y), 0.5, 0.8)


def wraith_hurt(v):
    d = 0.45
    t = T(d)
    f = np.linspace(260 + v * 40, 70, len(t)) * (1 + 0.12 * np.sin(2 * np.pi * 28 * t))
    return (tone(f, d) + 0.3 * bp(noise(d), 100, 900)) * env(len(t), 0.005, 0.35)


def wraith_death(v):
    d = 1.1
    t = T(d)
    # implosion: everything sweeps down and accelerates inward, then stops dead (the blast follows in-game)
    f = 700 * (1 - t / d) ** 2.5 + 28
    body = tone(f, d, 'saw') * 0.6
    suck = bp(noise(d), 200, 4000) * (t / d) ** 2
    y = (body + suck * 1.2) * np.minimum(1, (t / d) ** 0.5 * 1.4)
    y[-int(SR * 0.03):] *= np.linspace(1, 0, int(SR * 0.03))
    return lp(y, 6000)


def wraith_charge(v):
    d = 0.7
    t = T(d)
    swell = bp(noise(d), 150, 2500) * (t / d) ** 3
    tonal = tone(np.linspace(60, 240, len(t)), d) * (t / d) ** 2 * 0.6
    return swell + tonal


def wraith_blink(v):
    d = 0.35
    t = T(d)
    f = 1800 * np.exp(-t * 14) + 90
    return tone(f, d) * env(len(t), 0.002, 0.3) + hp(noise(d), 4000) * np.exp(-t * 40) * 0.3


def swarm_ambient(v):
    d = 1.6
    t = T(d)
    y = np.zeros(len(t))
    for i in range(28):                       # many tiny detuned motors
        f = rng.uniform(320, 900)
        flutter = 1 + 0.06 * np.sin(2 * np.pi * rng.uniform(4, 11) * t + rng.random() * 6)
        y += tone(f * flutter, d, 'square') * (0.5 + 0.5 * np.sin(2 * np.pi * rng.uniform(0.5, 3) * t + rng.random() * 6)) / 28
    y = bp(y, 250, 3500)
    for i in range(18):                       # crackles of discharge
        place(y, hp(noise(0.01), 3000) * 0.5, rng.uniform(0, d - 0.02))
    return y * env(len(t), 0.2, 0.4)


def swarm_hurt(v):
    d = 0.32
    t = T(d)
    y = np.zeros(len(t))
    for i in range(30):
        place(y, hp(noise(0.006), 2500) * rng.uniform(0.3, 1), rng.uniform(0, 0.25))
    return y + tone(np.linspace(1500, 400, len(t)), d, 'square') * env(len(t), 0.002, 0.25) * 0.15


def swarm_death(v):
    d = 0.9
    t = T(d)
    y = np.zeros(len(t))
    for i in range(70):                       # sparks fizzling out
        at = rng.uniform(0, d - 0.05) ** 1.2
        place(y, bp(noise(0.015), 1500, 9000) * (1 - at / d) * rng.uniform(0.4, 1), at)
    return y + lp(noise(d), 600) * np.exp(-t * 6) * 0.4


def swarm_split(v):
    d = 0.55
    t = T(d)
    zap = tone(np.linspace(3000, 300, int(SR * 0.12)), 0.12, 'square') * env(int(SR * 0.12), 0.001, 0.1) * 0.5
    y = np.zeros(len(t))
    place(y, zap, 0)
    for i in range(6):                        # the pieces chirping away from each other
        f0 = rng.uniform(900, 2000)
        place(y, tone(np.linspace(f0, f0 * 1.8, int(SR * 0.08)), 0.08) * env(int(SR * 0.08), 0.002, 0.07) * 0.4, 0.1 + i * 0.06)
    return y


def echo_ambient(v):
    d = 2.0
    y = np.zeros(int(SR * d))
    for k in range(4):                        # tick ... tock ...
        place(y, clink(2600 if k % 2 == 0 else 2100, 0.12, decay=40), 0.1 + k * 0.42)
    b = bell(523.25 if v == 0 else 659.25, 1.3)[::-1] * 0.35   # a reversed chime: time running the wrong way
    place(y, b, d - 1.35)
    return y


def echo_hurt(v):
    d = 0.6
    y = np.zeros(int(SR * d))
    place(y, clink(420 + v * 90, 0.5, decay=7), 0)
    t = T(0.45)
    boing = np.sin(2 * np.pi * (180 + 40 * np.sin(2 * np.pi * 9 * t)) * t) * np.exp(-t * 6) * 0.35   # a spring let go
    place(y, boing, 0.03)
    return y


def echo_death(v):
    d = 1.8
    y = np.zeros(int(SR * d))
    for i in range(36):                       # the mechanism shattering: gears and screws raining down
        at = rng.uniform(0, 1.2) ** 1.4
        place(y, clink(rng.uniform(700, 4200), 0.25, decay=rng.uniform(10, 25)) * rng.uniform(0.2, 0.7) * (1 - at / 1.6), at)
    t = T(0.9)
    place(y, np.sin(2 * np.pi * (220 * (1 + 0.3 * np.exp(-t * 3)) + 30 * np.sin(2 * np.pi * 14 * t)) * t) * np.exp(-t * 3.5) * 0.4, 0.05)
    place(y, bell(392, 1.0) * np.linspace(0.4, 0, int(SR * 1.0)), 0.7)
    return y


def echo_step(v):
    return clink(1700 + v * 300, 0.09, (1, 2.3, 4.1), decay=45) * 0.7


def echo_rewind(v):
    d = 1.1
    t = T(d)
    y = bp(noise(d), 300, 5000) * (t / d) ** 2 * 0.5   # reversed whoosh
    at, gap = 0.0, 0.16
    while at < d - 0.05:                      # ticking that speeds up as it runs backwards
        place(y, clink(2400, 0.06, decay=60)[::-1] * 0.6, at)
        at += gap
        gap = max(0.03, gap * 0.82)
    return y


CHORDS = [(220, 275, 330, 440), (196, 294, 392, 494)]


def law_ambient(v):
    d = 2.4
    t = T(d)
    y = np.zeros(len(t))
    for i, f in enumerate(CHORDS[v % 2]):     # a pure just-intonation chord, slowly shimmering
        y += np.sin(2 * np.pi * f * t * (1 + 0.002 * np.sin(2 * np.pi * 0.7 * t + i))) * (0.5 + 0.5 * np.sin(2 * np.pi * (0.3 + i * 0.17) * t + i)) / 4
        y += np.sin(2 * np.pi * f * 2.0 * t) * 0.08
    return y * env(len(t), 0.6, 0.8, 1.5)


def law_hurt(v):
    d = 0.5
    t = T(d)
    y = sum(np.sin(2 * np.pi * f * t) for f in (311, 330, 466 + v * 20, 494)) / 4     # a law broken: dissonance
    return y * env(len(t), 0.003, 0.4) + bp(noise(d), 2000, 6000) * np.exp(-t * 30) * 0.2


def law_death(v):
    d = 1.6
    t = T(d)
    y = np.zeros(len(t))
    for f in (220, 275, 330, 440, 550):       # every partial collapses onto one tone, then nothing
        fr = f + (180 - f) * np.clip(t / (d * 0.7), 0, 1) ** 2
        y += tone(fr, d) / 5
    return y * env(len(t), 0.01, d * 0.6, 1.2)


def law_shoot(v):
    d = 0.45
    y = np.zeros(int(SR * d))
    for i, f in enumerate((440, 554, 659, 880)):
        n = int(SR * 0.15)
        place(y, tone(f, 0.15) * env(n, 0.003, 0.14) * 0.5, i * 0.07)
    return y


def wisp_ambient(v):
    d = 1.8
    t = T(d)
    f = 90 + 25 * np.sin(2 * np.pi * (0.8 + v * 0.3) * t)
    hum = tone(f, d) * 0.6 + tone(f * 2.01, d) * 0.2
    swirl = bp(noise(d), 300, 1400) * (0.5 + 0.5 * np.sin(2 * np.pi * 1.5 * t + v)) * 0.5
    return (hum + swirl) * env(len(t), 0.3, 0.6)


def wisp_hurt(v):
    d = 0.4
    t = T(d)
    return tone(np.linspace(500 + v * 80, 120, len(t)) * (1 + 0.2 * np.sin(2 * np.pi * 35 * t)), d) * env(len(t), 0.003, 0.35)


def wisp_death(v):
    d = 0.9
    t = T(d)
    f = 400 * (1 - t / d) ** 2 + 40
    return (tone(f, d) * 0.7 + bp(noise(d), 200, 3000) * (t / d) ** 2 * 0.6) * np.minimum(1, (1 - t / d) * 8)


def wisp_pull(v):
    d = 1.0
    t = T(d)
    whomp = tone(np.linspace(120, 35, len(t)), d) * np.exp(-t * 3)
    suck = bp(noise(d), 150, 2500)[::-1] * np.exp(-(1 - t / d) * 4) * 0.5
    return whomp + suck


def strider_ambient(v):
    d = 1.6
    t = T(d)
    y = np.zeros(len(t))
    for f0, a in ((310 + v * 40, 1), (466 + v * 30, 0.6), (622, 0.4)):    # a bowed membrane, heard twice: once from each brane
        for det in (1.0, 1.013):
            y += np.sin(2 * np.pi * f0 * det * t + 3 * np.sin(2 * np.pi * 5 * t)) * a * 0.3
    return y * env(len(t), 0.4, 0.7) + bp(noise(d), 1500, 4000) * env(len(t), 0.4, 0.7) * 0.15


def strider_hurt(v):
    d = 0.45
    t = T(d)
    snap = hp(noise(0.05), 1000) * np.exp(-T(0.05) * 60)
    y = mix(snap, tone(np.linspace(700 + v * 60, 300, len(t)), d) * env(len(t), 0.005, 0.4) * 0.6)
    return y


def strider_death(v):
    d = 1.6
    t = T(d)
    tear = bp(noise(d), 800, 6000) * np.exp(-t * 2) * (0.6 + 0.4 * np.sin(2 * np.pi * 23 * t))
    groan = tone(np.linspace(300, 60, len(t)), d) * env(len(t), 0.05, 1.0) * 0.6
    return tear + groan


def strider_step(v):
    d = 0.18
    return lp(noise(d), 400) * env(int(SR * d), 0.002, 0.15) * 1.5 + clink(1200 + v * 200, d, (1, 2.4), decay=30) * 0.25


def strider_phase(v):
    d = 0.8
    t = T(d)
    sw = bp(noise(d), 300, 5000) * (t / d) ** 2
    ch = sum(tone(np.linspace(200, 900, len(t)) * k, d) for k in (1, 1.01, 0.99)) / 3 * (t / d) ** 3
    y = sw + ch * 0.6
    y[-int(SR * 0.02):] *= np.linspace(1, 0, int(SR * 0.02))
    return y


def crawler_ambient(v):
    d = 1.2
    y = np.zeros(int(SR * d))
    at = 0.05
    while at < d - 0.05:
        f = rng.uniform(1800, 4000)
        place(y, bp(noise(0.012), f * 0.8, f * 1.2) * np.exp(-T(0.012) * 300) * rng.uniform(0.5, 1), at)
        at += rng.uniform(0.02, 0.09)
    return y


def crawler_hurt(v):
    d = 0.25
    t = T(d)
    return tone(np.linspace(1800 + v * 200, 900, len(t)), d, 'square') * env(len(t), 0.002, 0.2) * 0.3 + bp(noise(d), 1000, 5000) * np.exp(-t * 30) * 0.5


def crawler_death(v):
    d = 0.7
    y = np.zeros(int(SR * d))
    for i in range(25):
        place(y, bp(noise(0.02), 600, 4000) * rng.uniform(0.3, 1) * np.exp(-T(0.02) * 150), rng.uniform(0, 0.6) ** 1.3)
    return y


def crawler_step(v):
    return bp(noise(0.03), 2000, 6000) * np.exp(-T(0.03) * 200)


# ------------------------------------------------------------------ round 3: machines, foam, collapse, Platonic natives
def machine_startup(v):
    """Relays clunk, a turbine whine climbs through the octaves, the hum swells and locks in."""
    d = 3.4
    t = T(d)
    f = 60 * (1 + 14 * (1 - np.exp(-t / 1.1)))
    whine = tone(f, d, 'saw') * 0.25 + tone(f * 2.01, d) * 0.3 + tone(f * 3.02, d) * 0.12
    whine = lp(whine, 4000) * np.clip(t / 0.6, 0, 1) * (0.6 + 0.4 * np.clip((t - 2.4) / 0.6, 0, 1))
    hum = (tone(55, d) + tone(110, d) * 0.5 + tone(165, d) * 0.2) * np.clip((t - 0.8) / 2.0, 0, 1) * 0.7
    buf = mix(whine, hum)
    for at, f0 in ((0.0, 180), (0.12, 140), (0.9, 220), (2.9, 260)):
        place(buf, clink(f0, 0.3, decay=20) * 1.2, at)
    place(buf, lp(noise(0.5), 600) * np.exp(-T(0.5) * 8) * 0.8, 0.0)
    return buf * env(len(buf), 0.02, 0.5)


def machine_shutdown(v):
    """The whine falls away, the hum sags and stutters, a few sparks, a final thunk."""
    d = 2.8
    t = T(d)
    f = 900 * np.exp(-t / 0.7) + 40
    whine = (tone(f, d, 'saw') * 0.25 + tone(f * 2.0, d) * 0.25) * np.exp(-t / 1.4)
    stutter = 1 - 0.7 * (np.sin(2 * np.pi * (6 + 10 * t) * t) > 0.6) * np.clip((t - 0.8) / 1.0, 0, 1)
    hum = (tone(55 * (1 - 0.3 * t / d), d) + tone(110 * (1 - 0.3 * t / d), d) * 0.5) * np.exp(-t / 1.0) * 0.8 * stutter
    buf = mix(lp(whine, 3000), hum)
    for k in range(7):
        place(buf, hp(noise(0.03), 2500) * np.exp(-T(0.03) * 120) * 0.7, 0.4 + rng.random() * 1.6)
    place(buf, clink(120, 0.4, decay=10) * 1.4, d - 0.45)
    return buf * env(len(buf), 0.005, 0.3)


def foam_pop(v):
    """A virtual particle blinking out: a tiny bubble pop with a reversed glassy shimmer."""
    d = 0.35
    t = T(d)
    pop = np.sin(2 * np.pi * (900 + 1400 * np.exp(-t * 30)) * t) * np.exp(-t * 40)
    shimmer = tone(2400 + 300 * v, d) * np.exp(-(d - t) * 18)[::-1] * 0.25
    return mix(pop, shimmer[::-1]) + hp(noise(d), 4000) * np.exp(-t * 60) * 0.3


def omnilex_warning(v):
    """A slow two-tone alarm under a choir of detuned sines."""
    d = 3.0
    t = T(d)
    alarm = tone(np.where((t * 1.5) % 1 < 0.5, 440, 330), d, 'square') * 0.15
    choir = sum(tone(f * (1 + 0.004 * k), d) for k, f in enumerate((110, 165, 220, 277, 330))) * 0.12
    return lp(alarm, 2000) + choir * env(int(SR * d), 0.6, 0.8)


def omnilex_collapse(v):
    """Everything falls inward: a sub-bass drop, reversed cymbal swell, then a vast implosion and ringing silence."""
    d = 7.0
    t = T(d)
    swell = hp(noise(d), 3000) * np.clip(t / 3.5, 0, 1) ** 3 * (t < 3.6)
    drop = tone(80 * np.exp(-np.clip(t - 3.5, 0, None) / 1.2) + 18, d) * np.clip((t - 3.5) / 0.05, 0, 1) * np.exp(-np.clip(t - 3.5, 0, None) / 2.0)
    boom = lp(noise(d), 200) * np.exp(-np.clip(t - 3.6, 0, None) * 1.5) * (t > 3.6) * 2
    ring = bell(330, d) * 0.4
    buf = mix(swell * 0.6, drop * 1.2, boom)
    place(buf, ring[:int(SR * 3)], 3.7)
    return buf


def hyperobject_hum(v):
    d = 4.0
    t = T(d)
    y = sum(tone(f * (1 + 0.01 * np.sin(2 * np.pi * 0.3 * t * (k + 1))), d) / (k + 1) for k, f in enumerate((73.4, 110, 146.8, 220, 293.6)))
    return y * (0.7 + 0.3 * np.sin(2 * np.pi * 0.5 * t)) * env(len(y), 0.5, 0.5)


def omnipotence_grant(v):
    d = 1.2
    return mix(bell(523.25, d), bell(659.25, d) * 0.7, bell(783.99, d) * 0.5) + tone(1046.5, d) * np.exp(-T(d) * 3) * 0.2


def wyrm_ambient(v):
    """A recursive hiss: the same phrase repeating ever smaller and higher."""
    d = 1.6
    buf = np.zeros(int(SR * d))
    for k in range(5):
        seg = bp(noise(0.35 / (k + 1) + 0.05), 600 * 1.6 ** k, 1400 * 1.6 ** k) * np.exp(-T(0.35 / (k + 1) + 0.05) * 6)
        place(buf, seg * (0.9 ** k), 0.25 * k)
    return buf


def wyrm_hurt(v):
    d = 0.4
    return bp(noise(d), 800, 3000) * np.exp(-T(d) * 9) + tone(np.linspace(700, 300, 10), d, 'saw') * 0.3 * np.exp(-T(d) * 8)


def wyrm_death(v):
    d = 1.8
    buf = np.zeros(int(SR * d))
    for k in range(8):
        place(buf, clink(400 * 1.3 ** k, 0.3, decay=14) * 0.7, k * 0.17)
    return buf + bp(noise(d), 300, 1200) * np.exp(-T(d) * 2) * 0.5


def sentinel_ambient(v):
    d = 2.0
    t = T(d)
    return (tone(196, d) + tone(196 * 1.5, d) * 0.6 + tone(196 * 2, d) * 0.3) * (0.6 + 0.4 * np.sin(2 * np.pi * 0.7 * t)) * env(int(SR * d), 0.4, 0.6)


def sentinel_hurt(v):
    return clink(600 + 80 * v, 0.4, decay=10)


def sentinel_death(v):
    d = 1.5
    buf = np.zeros(int(SR * d))
    for k, f0 in enumerate((880, 660, 440, 330, 220)):
        place(buf, bell(f0, 0.8) * 0.5, k * 0.15)
    return buf


def sentinel_charge(v):
    d = 1.8
    t = T(d)
    return tone(np.linspace(200, 1600, 64), d) * (t / d) ** 2 * 0.6 + hp(noise(d), 5000) * (t / d) ** 3 * 0.3


def sentinel_fire(v):
    d = 0.6
    t = T(d)
    return tone(np.linspace(1800, 300, 32), d, 'square') * np.exp(-t * 8) * 0.3 + lp(noise(d), 1500) * np.exp(-t * 10)



# ------------------------------------------------------------------ round 4 creatures
def r4_pair(kind):
    def f(v):
        d = {'ambient': 1.2, 'hurt': 0.4, 'death': 1.0}[kind]
        t = T(d)
        if kind == 'death':     # the pair annihilates: two tones collide into a flash
            a = tone(np.linspace(600, 1200, 16), d) + tone(np.linspace(1800, 1200, 16), d)
            return a * np.exp(-t * 3) + hp(noise(d), 3000) * np.exp(-(t - 0.5) ** 2 * 200) * 0.6
        if kind == 'hurt':
            return tone(np.linspace(1500, 900, 8), d) * np.exp(-t * 10)
        return (tone(880 + 40 * np.sin(2 * np.pi * 3 * t), d) + tone(1320 - 40 * np.sin(2 * np.pi * 3 * t), d)) * 0.3 * env(len(t), 0.2, 0.4)
    return f


def r4_creak(f0, kind, rough=0.5):
    """Generic voice: a filtered, vibrato'd buzz; hurt = short and higher, death = falling and long."""
    def f(v):
        d = {'ambient': 0.9, 'hurt': 0.35, 'death': 1.3}[kind]
        t = T(d)
        base = f0 * (1.3 if kind == 'hurt' else 1.0) * (1 + 0.05 * v)
        fr = base * (1 - 0.5 * t / d) if kind == 'death' else base * (1 + 0.04 * np.sin(2 * np.pi * 6 * t))
        src = tone(fr, d, 'saw') * (1 - rough) + noise(d) * rough * 0.5
        y = bp(src, base * 0.8, base * 4)
        return y * env(len(y), 0.02, d * 0.5)
    return f


def r4_bell(f0, kind):
    def f(v):
        if kind == 'hurt':
            return clink(f0 * 2, 0.4, decay=12)
        if kind == 'death':
            buf = np.zeros(int(SR * 1.4))
            for k in range(5):
                place(buf, bell(f0 * (1 + k * 0.25), 0.8) * 0.5, k * 0.12)
            return buf
        d = 1.6
        return (tone(f0, d) + tone(f0 * 1.5, d) * 0.5) * (0.6 + 0.4 * np.sin(2 * np.pi * 0.8 * T(d))) * env(int(SR * d), 0.3, 0.6)
    return f


def r4_bellhop(kind):
    def f(v):
        if kind == 'ambient':      # a front-desk bell: room for one more
            return bell(1568, 0.9) * 0.8
        return groan(0.4 if kind == 'hurt' else 1.0, 160 if kind == 'hurt' else 130)
    return f


def r4_moth(kind):
    def f(v):
        d = {'ambient': 0.8, 'hurt': 0.3, 'death': 0.9}[kind]
        t = T(d)
        flutter = (np.sin(2 * np.pi * 28 * t) > 0) * 1.0
        y = bp(noise(d), 2000, 7000) * flutter * 0.4 + tone(2400 + 200 * v, d) * 0.15
        if kind == 'death':
            y *= np.exp(-t * 3)
        return y * env(len(y), 0.05, 0.2)
    return f


def r4_horror(kind):
    def f(v):
        d = {'ambient': 1.6, 'hurt': 0.5, 'death': 2.0}[kind]
        t = T(d)
        stretch = tone(np.exp(np.linspace(np.log(220), np.log(40), 32)), d, 'saw') if kind != 'hurt' else tone(np.linspace(300, 120, 8), d, 'saw')
        return lp(stretch, 1200) * env(int(SR * d), 0.1, d * 0.5) + lp(noise(d), 300) * 0.3
    return f



SOUNDS = {
    'graviton_wisp.ambient': (wisp_ambient, 2, 'Graviton Wisp hums'),
    'graviton_wisp.hurt': (wisp_hurt, 2, 'Graviton Wisp hurts'),
    'graviton_wisp.death': (wisp_death, 1, 'Graviton Wisp collapses'),
    'graviton_wisp.pull': (wisp_pull, 1, 'Gravity pulses'),
    'brane_strider.ambient': (strider_ambient, 2, 'Brane Strider resonates'),
    'brane_strider.hurt': (strider_hurt, 2, 'Brane Strider hurts'),
    'brane_strider.death': (strider_death, 1, 'Brane Strider tears apart'),
    'brane_strider.step': (strider_step, 2, 'Stilt footsteps'),
    'brane_strider.phase': (strider_phase, 1, 'Brane Strider slips between branes'),
    'moduli_crawler.ambient': (crawler_ambient, 2, 'Moduli Crawler chitters'),
    'moduli_crawler.hurt': (crawler_hurt, 2, 'Moduli Crawler hurts'),
    'moduli_crawler.death': (crawler_death, 1, 'Moduli Crawler crumples'),
    'moduli_crawler.step': (crawler_step, 2, 'Skittering'),
    # event: (generator, variants, subtitle)
    'destabilised_husk.ambient': (husk_ambient, 3, 'Destabilised Husk glitches'),
    'destabilised_husk.hurt': (husk_hurt, 2, 'Destabilised Husk hurts'),
    'destabilised_husk.death': (husk_death, 1, 'Destabilised Husk disintegrates'),
    'destabilised_husk.step': (husk_step, 2, 'Footsteps'),
    'void_wraith.ambient': (wraith_ambient, 2, 'Void Wraith warps space'),
    'void_wraith.hurt': (wraith_hurt, 2, 'Void Wraith hurts'),
    'void_wraith.death': (wraith_death, 1, 'Void Wraith collapses'),
    'void_wraith.charge': (wraith_charge, 1, 'Void Wraith lunges'),
    'void_wraith.blink': (wraith_blink, 1, 'Void Wraith blinks through space'),
    'nanite_swarm.ambient': (swarm_ambient, 2, 'Nanite Swarm buzzes'),
    'nanite_swarm.hurt': (swarm_hurt, 2, 'Nanite Swarm crackles'),
    'nanite_swarm.death': (swarm_death, 1, 'Nanite Swarm fizzles out'),
    'nanite_swarm.split': (swarm_split, 1, 'Nanite Swarm splits'),
    'paradox_echo.ambient': (echo_ambient, 2, 'Paradox Echo ticks'),
    'paradox_echo.hurt': (echo_hurt, 2, 'Paradox Echo clanks'),
    'paradox_echo.death': (echo_death, 1, 'Paradox Echo shatters'),
    'paradox_echo.step': (echo_step, 2, 'Clockwork footsteps'),
    'paradox_echo.rewind': (echo_rewind, 1, 'Paradox Echo rewinds'),
    'law_wraith.ambient': (law_ambient, 2, 'Law Wraith resonates'),
    'law_wraith.hurt': (law_hurt, 2, 'Law Wraith hurts'),
    'law_wraith.death': (law_death, 1, 'Law Wraith collapses'),
    'law_wraith.shoot': (law_shoot, 1, 'Law Wraith casts'),
    'machine.startup': (machine_startup, 1, 'Multiblock powers up'),
    'machine.shutdown': (machine_shutdown, 1, 'Multiblock powers down'),
    'foam.pop': (foam_pop, 3, 'Virtual particle pops'),
    'omnilex.warning': (omnilex_warning, 1, 'The Omnilex Engine warns'),
    'omnilex.collapse': (omnilex_collapse, 1, 'Reality collapses'),
    'hyperobject.hum': (hyperobject_hum, 1, 'Hyperobject hums'),
    'omnipotence.grant': (omnipotence_grant, 1, 'Something is created'),
    'fractal_wyrm.ambient': (wyrm_ambient, 2, 'Fractal Wyrm hisses'),
    'fractal_wyrm.hurt': (wyrm_hurt, 2, 'Fractal Wyrm hurts'),
    'fractal_wyrm.death': (wyrm_death, 1, 'Fractal Wyrm unravels'),
    'axiom_sentinel.ambient': (sentinel_ambient, 2, 'Axiom Sentinel resonates'),
    'axiom_sentinel.hurt': (sentinel_hurt, 2, 'Axiom Sentinel rings'),
    'axiom_sentinel.death': (sentinel_death, 1, 'Axiom Sentinel shatters'),
    'axiom_sentinel.charge': (sentinel_charge, 1, 'Axiom Sentinel charges a proof'),
    'axiom_sentinel.fire': (sentinel_fire, 1, 'Axiom Sentinel fires'),
}


# ------------------------------------------------------------------ 1.3.8: the Interior's creatures, each with a voice of its own
def _click(f, d=0.012, decay=420):
    n = int(SR * d)
    return bp(noise(d), f * 0.7, f * 1.4) * np.exp(-np.arange(n) / SR * decay)


def crawler_voice(kind):
    """A big centipede: dry, rapid mandible clicks and leg-scratching - a critter, not a monster."""
    def f(v):
        d = {'ambient': 1.1, 'hurt': 0.4, 'death': 1.4}[kind]
        buf = np.zeros(int(SR * d))
        n = {'ambient': 26, 'hurt': 14, 'death': 40}[kind]
        for i in range(n):
            if kind == 'death':
                at = (i / n) ** 1.6 * (d - 0.05)                     # the clicks slow and stop
            else:
                at = rng.uniform(0, d - 0.05)
            fr = rng.uniform(2600, 5200) * (1.25 if kind == 'hurt' else 1.0) * (1 + 0.05 * v)
            place(buf, _click(fr) * (0.6 + 0.4 * rng.random()) * (1 - 0.6 * at / d if kind == 'death' else 1), at)
        scratch = hp(noise(d), 4000) * 0.12 * (0.5 + 0.5 * np.sin(2 * np.pi * 18 * T(d)) ** 2)
        if kind == 'hurt':
            squeal = bp(tone(np.linspace(3200, 2400, 50), 0.25, 'saw'), 2000, 5000) * env(int(SR * 0.25), 0.005, 0.2) * 0.5
            place(buf, squeal, 0)
        return buf + scratch[:len(buf)] * (0.4 if kind == 'hurt' else 1.0)
    return f


def plume_voice(kind):
    """Burning rock: a roar of flame that swells and falls, with the crackle and spit of a fire."""
    def f(v):
        d = {'ambient': 1.6, 'hurt': 0.5, 'death': 1.8}[kind]
        t = T(d)
        swell = np.sin(np.pi * np.clip(t / d, 0, 1)) ** (0.7 if kind != 'death' else 0.4)
        roar = lp(noise(d), 700 + 300 * v) * 0.9 + bp(noise(d), 120, 400) * 0.6
        roar = roar * swell * (1 + 0.3 * np.sin(2 * np.pi * 3.2 * t))
        if kind == 'death':
            roar = roar * np.linspace(1, 0, len(t)) ** 1.5           # the fire gutters out
        buf = roar
        crackles = {'ambient': 22, 'hurt': 18, 'death': 30}[kind]
        for i in range(crackles):
            at = rng.uniform(0, d - 0.03)
            place(buf, _click(rng.uniform(1500, 6000), 0.008, 600) * (1.4 if kind == 'hurt' else 0.9), at)
        if kind == 'hurt':
            place(buf, hp(noise(0.3), 2500) * env(int(SR * 0.3), 0.003, 0.25) * 0.8, 0)   # a hiss of steam
        if kind == 'death':
            place(buf, hp(noise(1.0), 3000) * env(int(SR * 1.0), 0.05, 0.9) * 0.5, 0.6)  # it cools with a long hiss
        return buf
    return f


def fission_voice(kind):
    """A husk with something splitting inside it: a low dead groan over the dry tick of a Geiger counter."""
    def f(v):
        d = {'ambient': 1.4, 'hurt': 0.45, 'death': 1.6}[kind]
        buf = np.zeros(int(SR * d))
        g = groan(min(d, 1.1), {'ambient': 70, 'hurt': 120, 'death': 85}[kind] + v * 10, 4)
        place(buf, g * 0.7, 0)
        rate = {'ambient': 25, 'hurt': 90, 'death': 60}[kind]
        tt = 0.0
        while tt < d - 0.01:                                         # Geiger ticks: random, Poisson-spaced
            place(buf, _click(rng.uniform(2500, 3500), 0.004, 1500) * 1.2, tt)
            tt += rng.exponential(1.0 / rate)
        if kind == 'death':                                          # the split: a deep thump and a flash of noise
            thump = tone(np.linspace(90, 30, 40), 0.5) * env(int(SR * 0.5), 0.002, 0.45) * 1.4
            place(buf, thump, 0.5)
            place(buf, hp(noise(0.25), 1500) * env(int(SR * 0.25), 0.001, 0.22), 0.5)
        return buf
    return f


def wraith_voice(kind):
    """A loop of current: mains-like hum with buzzing harmonics, zaps when struck, a discharge when it dies."""
    def f(v):
        d = {'ambient': 1.6, 'hurt': 0.35, 'death': 1.6}[kind]
        t = T(d)
        f0 = 55 * (1 + 0.04 * v)
        if kind == 'death':
            fr = f0 * (1 - 0.7 * t / d)
            hum = tone(fr, d, 'saw') * 0.4 + tone(fr * 2, d) * 0.3
            hum *= np.linspace(1, 0, len(t))
        else:
            hum = tone(f0, d) * 0.5 + tone(f0 * 2, d, 'saw') * 0.25 + tone(f0 * 3, d) * 0.15
            hum *= 0.7 + 0.3 * np.sin(2 * np.pi * 0.8 * t)
        buzz = bp(crush(hum, 5, 3), 300, 3000) * 0.4
        buf = hum + buzz
        zaps = {'ambient': 3, 'hurt': 6, 'death': 10}[kind]
        for i in range(zaps):
            at = rng.uniform(0, d - 0.08)
            place(buf, hp(noise(0.06), 2000) * env(int(SR * 0.06), 0.001, 0.05) * (1.4 if kind != 'ambient' else 0.6), at)
        return buf * env(len(buf), 0.08, 0.3)
    return f


def walker_voice(kind):
    """A random walk you can hear: dice rattling in a cup - clicks at random moments, of random pitch - over a soft swish of noise."""
    def f(v):
        d = {'ambient': 1.2, 'hurt': 0.4, 'death': 1.3}[kind]
        buf = lp(noise(d), 1800) * 0.12 * env(int(SR * d), 0.1, 0.4)
        rate = {'ambient': 18, 'hurt': 60, 'death': 35}[kind]
        tt = 0.0
        while tt < d - 0.02:
            place(buf, _click(rng.uniform(900, 2600) * (1 + 0.05 * v), 0.02, 260) * rng.uniform(0.4, 1.0), tt)
            tt += rng.exponential(1.0 / rate)
        if kind == 'death':                                          # the last die settles: a few slowing knocks
            for i, at in enumerate((0.8, 0.98, 1.1, 1.17)):
                place(buf, _click(1200, 0.03, 200) * (1.0 - 0.2 * i), at)
        return buf
    return f


# 1.10.1: the Accretion Frontier's creatures, each with a voice of its own (none shares a generator with anything else)
def _ks(f0, d, damp=0.995, bright=0.6):
    """Karplus-Strong plucked string at f0 (a scalar or a start..end pair for a string under changing tension)."""
    n = int(SR * d)
    f = np.linspace(*f0, n) if not np.isscalar(f0) else np.full(n, float(f0))
    period = int(SR / f[0])
    buf = list(noise(period / SR) * bright)
    out = np.zeros(n)
    pos = 0.0
    for i in range(n):
        L = len(buf)
        j = int(pos) % L
        out[i] = buf[j]
        buf[j] = damp * 0.5 * (buf[j] + buf[(j + 1) % L])
        pos += f[i] / f[0]
    return out


def _formant(src, fs, q=8.0):
    return sum(bp(src, f * (1 - 1 / q), f * (1 + 1 / q)) * g for f, g in fs)


def nebuloid_voice(kind):
    """Gas and dust: a breathy whoosh whose resonance drifts slowly up and down, like wind through a canyon of cloud."""
    def f(v):
        d = {'ambient': 1.8, 'hurt': 0.45, 'death': 2.2}[kind]
        t = T(d)
        n = lp(noise(d), 2500)
        centre = 380 + 260 * np.sin(2 * np.pi * (0.35 + 0.1 * v) * t) if kind == 'ambient' else (np.linspace(900, 300, len(t)) if kind == 'death'
                                                                                               else np.full(len(t), 700.0))
        out = np.zeros(len(t))
        blk = 2048
        for i in range(0, len(t), blk):                       # a slowly moving band, block by block
            c = float(centre[i])
            out[i:i + blk] = bp(n[max(0, i - 400):i + blk], c * 0.75, c * 1.33)[-len(out[i:i + blk]):]
        if kind == 'hurt':
            out = out * np.exp(-t * 9) * 2
        if kind == 'death':                                   # it disperses: the band widens into a thin hiss
            out = out * np.linspace(1, 0.2, len(t)) + hp(noise(d), 3000) * np.linspace(0, 0.25, len(t)) * np.linspace(1, 0, len(t))
        return out * env(len(out), 0.25 if kind != 'hurt' else 0.01, d * 0.5)
    return f


def wisp_pulsar_voice(kind):
    """A pulsar as a radio telescope hears it: sharp pulses on a strict beat, each a falling chirp (the interstellar
    medium delays the low frequencies - dispersion). Hurt: the beat glitches; death: it spins down, slower and lower."""
    def f(v):
        d = {'ambient': 1.5, 'hurt': 0.5, 'death': 2.4}[kind]
        buf = np.zeros(int(SR * d))
        at, period, f0 = 0.0, 0.18 + 0.02 * v, 2600.0
        while at < d - 0.06:
            pulse = tone(np.geomspace(f0, f0 * 0.25, 20), 0.045) * np.exp(-T(0.045) * 70)
            place(buf, pulse, at)
            if kind == 'hurt':
                at += period * rng.choice([0.3, 0.5, 1.0])
            elif kind == 'death':
                at += period
                period *= 1.18
                f0 *= 0.93
            else:
                at += period
        return buf
    return f


def stretcher_voice(kind):
    """Something under tidal tension: a plucked string whose pitch climbs as it is pulled tighter (strains), a sharp
    crack and a rebounding twang (snaps), and a long rising stretch that ends in a tear and a wobbling slack string."""
    def f(v):
        if kind == 'ambient':
            s_ = _ks((110 + 10 * v, 190 + 10 * v), 1.4, 0.998)
            return lp(s_, 2500) * env(len(s_), 0.01, 0.6) + lp(noise(1.4), 200) * 0.15
        if kind == 'hurt':
            buf = np.zeros(int(SR * 0.6))
            place(buf, hp(noise(0.03), 1500) * np.exp(-T(0.03) * 120) * 1.4, 0)
            place(buf, _ks((330, 220), 0.55, 0.993) * 0.8, 0.01)
            return buf
        buf = np.zeros(int(SR * 2.4))
        place(buf, _ks((90, 420), 1.5, 0.999) * env(int(SR * 1.5), 0.05, 0.1), 0)
        place(buf, hp(noise(0.05), 1200) * np.exp(-T(0.05) * 60) * 1.6, 1.5)
        wob = tone(70 * (1 + 0.15 * np.sin(2 * np.pi * 7 * T(0.85))), 0.85, 'tri') * np.exp(-T(0.85) * 4)
        place(buf, wob * 0.6, 1.52)
        return buf
    return f


def phantom_voice(kind):
    """Bent light: a glass-harmonica shimmer - pure tones a few hertz apart so they beat, swept by a slow phaser.
    Hurt: a glassy ting; death: the tones slide down and apart until they smear into nothing."""
    def f(v):
        if kind == 'hurt':
            return clink(1760 + 80 * v, 0.5, partials=(1, 2.32, 4.25, 6.63), decay=9)
        d = 1.8 if kind == 'ambient' else 2.0
        t = T(d)
        base = 660 + 30 * v
        if kind == 'ambient':
            y = tone(base, d) + tone(base + 3.1, d) + 0.5 * tone(base * 1.5, d) + 0.5 * tone(base * 1.5 + 2.3, d)
        else:
            y = tone(np.linspace(base, base * 0.5, 32), d) + tone(np.linspace(base + 3, base * 0.62, 32), d)
        y = y * (0.6 + 0.4 * np.sin(2 * np.pi * 0.7 * t))
        # a phaser: mix with a short, wobbling delay of itself
        dl = (0.002 + 0.0015 * np.sin(2 * np.pi * 0.4 * t)) * SR
        idx = np.clip(np.arange(len(y)) - dl, 0, len(y) - 1)
        y = y + np.interp(idx, np.arange(len(y)), y)
        return y * env(len(y), 0.35, d * 0.55)
    return f


def imp_voice(kind):
    """A tiny evaporating black hole: a hiss with a thermal (blackbody-like) tilt and crackles whose rate rises as it
    shrinks - Hawking radiation gets hotter as the hole gets smaller. Death: the crackle races to a final bright pop."""
    def f(v):
        d = {'ambient': 1.2, 'hurt': 0.4, 'death': 1.6}[kind]
        t = T(d)
        hiss = bp(noise(d), 1200, 9000) * 0.18 * (0.6 + 0.4 * np.sin(2 * np.pi * 1.3 * t))
        buf = hiss.copy()
        at = 0.0
        while at < d - 0.02:
            q = at / d
            rate = {'ambient': 12, 'hurt': 70, 'death': 8 + 220 * q ** 2}[kind]
            place(buf, _click(rng.uniform(3000, 9000) * (1 + q * (kind == 'death')), 0.006, 900) * rng.uniform(0.5, 1.1), at)
            at += rng.exponential(1.0 / rate)
        if kind == 'death':
            flash = (hp(noise(0.25), 2000) + tone(np.geomspace(6000, 800, 12), 0.25) * 0.5) * np.exp(-T(0.25) * 18) * 1.5
            place(buf, flash, d - 0.27)
        return buf * env(len(buf), 0.02, 0.15)
    return f


def brain_voice(kind):
    """A mind that fluctuated out of the void: muttering - a buzz pushed through vowel formants that change at random,
    like speech heard through a wall. Hurt: a sharp startled vowel; death: the mutter thermalises into noise."""
    VOW = [(730, 1090), (270, 2290), (300, 870), (530, 1840), (640, 1190)]
    def f(v):
        d = {'ambient': 1.5, 'hurt': 0.35, 'death': 1.8}[kind]
        n = int(SR * d)
        t = T(d)
        f0 = (125 + 10 * v) * (1 + 0.08 * np.sin(2 * np.pi * 2.3 * t)) * (1.5 if kind == 'hurt' else 1)
        src = tone(f0, d, 'saw')
        out = np.zeros(n)
        seg = int(SR * 0.12)
        for i in range(0, n, seg):
            f1, f2 = VOW[rng.integers(len(VOW))]
            out[i:i + seg] = _formant(src[max(0, i - 600):i + seg], [(f1, 1.0), (f2, 0.6)], 6)[-len(out[i:i + seg]):]
        syll = 0.55 + 0.45 * (np.sin(2 * np.pi * 4.5 * t) > -0.3)
        out = out * syll
        if kind == 'death':
            k = np.linspace(0, 1, n)
            out = out * (1 - k) + bp(noise(d), 300, 3000) * 0.4 * k * (1 - k) * 4
        return out * env(n, 0.03, d * 0.4)
    return f


def swan_voice(kind):
    """A black swan: a soft, bugling honk (a nasal buzz through a long neck - formants near 600 and 1100 Hz), the hiss
    a swan really makes when threatened, and a falling, breaking honk."""
    def f(v):
        if kind == 'hurt':
            d = 0.6
            return bp(noise(d), 1500, 7000) * env(int(SR * d), 0.02, 0.4) * (1 + 0.3 * np.sin(2 * np.pi * 11 * T(d)))
        d = 0.7 if kind == 'ambient' else 1.4
        t = T(d)
        f0 = (330 + 15 * v) * (np.interp(t, [0, 0.08, d], [0.85, 1.0, 0.97]) if kind == 'ambient' else np.interp(t, [0, d], [1.0, 0.6]))
        src = tone(f0, d, 'saw') + 0.3 * tone(f0 * 1.005, d, 'square')
        y = _formant(src, [(620, 1.0), (1100, 0.7), (2400, 0.25)], 5)
        if kind == 'death':
            y = y * (1 + 0.6 * (np.sin(2 * np.pi * 9 * t) > 0.6))   # the honk breaks
        return y * env(len(y), 0.03, d * 0.35)
    return f


def binary_voice(kind):
    """Two black holes in orbit: two tones a few hertz apart, beating, circling (ambient); a short chirp (hurt); and the
    whole inspiral - frequency and loudness rising to the merger - then the ringdown of the single hole left."""
    def f(v):
        if kind == 'ambient':
            d = 2.0
            t = T(d)
            y = tone(82 + 3 * v, d, 'tri') + tone(85.5 + 3 * v, d, 'tri') + 0.4 * tone(164, d)
            return lp(y, 900) * (0.7 + 0.3 * np.sin(2 * np.pi * 0.5 * t)) * env(len(t), 0.4, 0.8)
        if kind == 'hurt':
            d = 0.5
            t = T(d)
            tau = np.clip(d + 0.01 - t, 0.01, None)
            return tone(np.minimum(120 * (tau / d) ** (-3 / 8), 1200), d) * (tau / d) ** (-0.25) * 0.4 * env(len(t), 0.005, 0.05)
        d, T0 = 3.2, 2.4
        t = T(d)
        tau = np.clip(T0 + 0.005 - t, 0.005, None)
        fq = np.where(t < T0, np.minimum(35 * (tau / T0) ** (-3 / 8), 700), 0)
        amp = np.where(t < T0, (tau / T0) ** (-0.25), 0)
        inspiral = np.sin(2 * np.pi * np.cumsum(fq) / SR) * amp * 0.25
        tr = np.clip(t - T0, 0, None)
        ring = np.where(t >= T0, np.sin(2 * np.pi * 520 * tr) * np.exp(-tr * 6), 0) * 0.9
        return inspiral + ring
    return f


def annihilator_voice(kind):
    """Matter meeting antimatter: crackling discharges over a pure tone, with a flash of noise at each annihilation."""
    def f(v):
        d = {'ambient': 1.3, 'hurt': 0.45, 'death': 1.6}[kind]
        t = T(d)
        buf = tone(511 + 20 * v, d) * 0.15 * (0.5 + 0.5 * np.sin(2 * np.pi * 1.7 * t) ** 2)
        rate = {'ambient': 6, 'hurt': 25, 'death': 18}[kind]
        at = 0.0
        while at < d - 0.1:
            place(buf, (hp(noise(0.08), 3000) + tone(1022, 0.08) * 0.4) * np.exp(-T(0.08) * 40), at)
            at += rng.exponential(1.0 / rate)
        if kind == 'death':
            buf = buf * np.linspace(1, 0.3, len(buf))
            place(buf, hp(noise(0.5), 800) * np.exp(-T(0.5) * 6) * 1.3, d - 0.55)
        return buf * env(len(buf), 0.02, 0.2)
    return f


def fx_pulsar_beat(v):
    """The Pulsar Field's beat, overhead: one big dispersed pulse - a falling whistle over a deep thud."""
    buf = tone(np.geomspace(90, 40, 10), 0.4) * np.exp(-T(0.4) * 8)
    place(buf, tone(np.geomspace(3200, 300, 30), 0.35) * np.exp(-T(0.35) * 9) * 0.8, 0)
    return buf


def fx_micrometeorite(v):
    """A grain of dust at kilometres a second: a tiny, sharp crack and a spray of fragments."""
    buf = np.zeros(int(SR * 0.4))
    place(buf, hp(noise(0.02), 4000) * np.exp(-T(0.02) * 200) * 1.5, 0)
    for i in range(8):
        place(buf, _click(rng.uniform(4000, 9000), 0.004, 1200) * 0.6, 0.02 + rng.exponential(0.05))
    return buf


def fx_imp_burst(v):
    """A Hawking Imp's last flash, heard from afar."""
    return (hp(noise(0.6), 600) * 0.8 + tone(np.geomspace(4000, 120, 20), 0.6) * 0.6) * np.exp(-T(0.6) * 6)


def fx_binary_summon(v):
    """The Binary answering a gravitational-wave echo: a long, rising inspiral swelling out of a low rumble."""
    d = 3.0
    t = T(d)
    tau = np.clip(d + 0.02 - t, 0.02, None)
    return (np.sin(2 * np.pi * np.cumsum(np.minimum(25 * (tau / d) ** (-3 / 8), 500)) / SR) * (tau / d) ** (-0.25) * 0.3
            + lp(noise(d), 120) * np.linspace(0.2, 0.6, len(t)))


def fx_binary_wave(v):
    """The Binary's gravitational wave: space itself squeezed and stretched - a deep, wobbling boom."""
    d = 1.4
    t = T(d)
    return lp(tone(55 * (1 + 0.3 * np.sin(2 * np.pi * 3 * t)), d, 'saw'), 400) * np.exp(-t * 2.5) + lp(noise(d), 200) * np.exp(-t * 4) * 0.6


_FRONTIER = {
    'nebuloid': (nebuloid_voice, 'Nebuloid', ('sighs', 'billows', 'disperses')),
    'pulsar_wisp': (wisp_pulsar_voice, 'Pulsar Wisp', ('ticks', 'flares', 'spins down')),
    'tidal_stretcher': (stretcher_voice, 'Tidal Stretcher', ('strains', 'snaps', 'tears')),
    'lens_phantom': (phantom_voice, 'Lens Phantom', ('shimmers', 'refracts', 'fades')),
    'hawking_imp': (imp_voice, 'Hawking Imp', ('fizzes', 'sputters', 'pops')),
    'boltzmann_brain': (brain_voice, 'Boltzmann Brain', ('mutters', 'flinches', 'fluctuates away')),
    'black_swan': (swan_voice, 'Black Swan', ('honks', 'hisses', 'dies')),
    'binary_serpent': (binary_voice, 'The Binary', ('chirps', 'rings', 'rings down')),
}
SOUNDS['frontier.pulsar_beat'] = (fx_pulsar_beat, 1, 'Pulsar beat')
SOUNDS['frontier.micrometeorite'] = (fx_micrometeorite, 2, 'Micrometeorite strikes')
SOUNDS['hawking_imp.burst'] = (fx_imp_burst, 1, 'Hawking Imp evaporates')
SOUNDS['binary_serpent.summon'] = (fx_binary_summon, 1, 'Something answers the echo')
SOUNDS['binary_serpent.wave'] = (fx_binary_wave, 1, 'Spacetime ripples')
for _id, (_fn, _nm, _words) in _FRONTIER.items():
    for _i, _k in enumerate(('ambient', 'hurt', 'death')):
        SOUNDS[f'{_id}.{_k}'] = (_fn(_k), 2 if _k != 'death' else 1, f'{_nm} {_words[_i]}')


for _k in ('ambient', 'hurt', 'death'):
    SOUNDS[f'random_walker.{_k}'] = (walker_voice(_k), 2 if _k != 'death' else 1, 'Random Walker ' + {'ambient': 'rattles', 'hurt': 'scatters',
                                                                                                    'death': 'settles'}[_k])
    SOUNDS[f'virtual_pair.{_k}'] = (r4_pair(_k), 2 if _k != 'death' else 1, 'Virtual Pair ' + {'ambient': 'hums', 'hurt': 'flickers', 'death': 'annihilates'}[_k])
    SOUNDS[f'planck_mite.{_k}'] = (r4_creak(900, _k, 0.6), 2 if _k != 'death' else 1, 'Planck Mite ' + {'ambient': 'chitters', 'hurt': 'hurts', 'death': 'dissolves'}[_k])
    SOUNDS[f'vacuum_drifter.{_k}'] = (r4_bell(110, _k), 2 if _k != 'death' else 1, 'Vacuum Drifter ' + {'ambient': 'resonates', 'hurt': 'rings', 'death': 'collapses'}[_k])
    SOUNDS[f'hilbert_bellhop.{_k}'] = (r4_bellhop(_k), 2 if _k != 'death' else 1, 'Hilbert Bellhop ' + {'ambient': 'rings the bell', 'hurt': 'hurts', 'death': 'checks out'}[_k])
    SOUNDS[f'ordinal_hound.{_k}'] = (r4_creak(220, _k, 0.3), 2 if _k != 'death' else 1, 'Ordinal Hound ' + {'ambient': 'growls', 'hurt': 'yelps', 'death': 'dies'}[_k])
    SOUNDS[f'hypercube_drifter.{_k}'] = (r4_bell(330, _k), 2 if _k != 'death' else 1, 'Hypercube Drifter ' + {'ambient': 'turns', 'hurt': 'rings', 'death': 'unfolds'}[_k])
    SOUNDS[f'tidal_horror.{_k}'] = (r4_horror(_k), 2 if _k != 'death' else 1, 'Tidal Horror ' + {'ambient': 'stretches', 'hurt': 'hurts', 'death': 'snaps'}[_k])
    SOUNDS[f'photon_moth.{_k}'] = (r4_moth(_k), 2 if _k != 'death' else 1, 'Photon Moth ' + {'ambient': 'flutters', 'hurt': 'hurts', 'death': 'fades'}[_k])
    SOUNDS[f'annihilator.{_k}'] = (annihilator_voice(_k), 2 if _k != 'death' else 1, 'Annihilator ' + {'ambient': 'crackles', 'hurt': 'flares', 'death': 'annihilates'}[_k])
    SOUNDS[f'positronium_skitterer.{_k}'] = (r4_creak(1300, _k, 0.5), 2 if _k != 'death' else 1, 'Positronium Skitterer ' + {'ambient': 'skitters', 'hurt': 'hurts', 'death': 'decays'}[_k])
    SOUNDS[f'magma_crawler.{_k}'] = (crawler_voice(_k), 2 if _k != 'death' else 1, 'Magma Crawler ' + {'ambient': 'chitters', 'hurt': 'squeals', 'death': 'dies'}[_k])
    SOUNDS[f'plume_elemental.{_k}'] = (plume_voice(_k), 2 if _k != 'death' else 1, 'Plume Elemental ' + {'ambient': 'roars', 'hurt': 'hisses', 'death': 'gutters out'}[_k])
    SOUNDS[f'fission_husk.{_k}'] = (fission_voice(_k), 2 if _k != 'death' else 1, 'Fission Husk ' + {'ambient': 'groans', 'hurt': 'crackles', 'death': 'splits'}[_k])
    SOUNDS[f'geodynamo_wraith.{_k}'] = (wraith_voice(_k), 2 if _k != 'death' else 1, 'Geodynamo Wraith ' + {'ambient': 'hums', 'hurt': 'sparks', 'death': 'discharges'}[_k])
    SOUNDS[f'antihydrogen_wisp.{_k}'] = (r4_bell(440, _k), 2 if _k != 'death' else 1, 'Antihydrogen Wisp ' + {'ambient': 'chimes', 'hurt': 'flickers', 'death': 'annihilates'}[_k])


# ------------------------------------------------------------------ round 36: the mod's own effect sounds
# (every vanilla sound the mod used to borrow - beacons, amethyst, wardens, portals, buckets - has its own voice now)
def _sweep(f0, f1, d, kind='sine', curve='exp'):
    t = np.linspace(0, 1, 64)
    fr = f0 * (f1 / f0) ** t if curve == 'exp' else f0 + (f1 - f0) * t
    return tone(fr, d, kind)


def fx_ui_click(v):
    """A clean digital tick: a short square blip and its octave, with a glassy tail."""
    d = 0.12
    t = T(d)
    f = 1900 + 120 * v
    y = tone(f, d, 'square') * 0.25 * np.exp(-t * 90) + tone(f * 2, d) * 0.4 * np.exp(-t * 60) + tone(f * 0.5, d) * 0.3 * np.exp(-t * 40)
    return lp(y, 9000)


def fx_power_up(v):
    """Capacitors charging: a rising sweep under a major chord that swells and locks in."""
    d = 1.6
    t = T(d)
    sweep = _sweep(110, 880, d, 'saw') * 0.25 * np.clip(1 - t / d, 0, 1) ** 0.5
    chord = sum(tone(f, d) * a for f, a in ((220, 0.6), (277.2, 0.4), (329.6, 0.4), (440, 0.3))) * np.clip((t - 0.5) / 0.6, 0, 1)
    buf = mix(lp(sweep, 3000), chord * 0.6)
    place(buf, clink(660, 0.3, decay=14) * 0.5, 1.05)
    return buf * env(len(buf), 0.02, 0.4)


def fx_power_down(v):
    """The field collapses: a falling sweep, the chord sagging out of tune, a soft thunk."""
    d = 1.5
    t = T(d)
    sweep = _sweep(880, 70, d, 'saw') * 0.25 * np.exp(-t * 1.2)
    sag = 1 - 0.25 * t / d
    chord = sum(tone(f * sag, d) * a for f, a in ((220, 0.6), (261.6, 0.4), (329.6, 0.3))) * np.exp(-t * 2.2)
    buf = mix(lp(sweep, 2500), chord * 0.6)
    place(buf, lp(noise(0.25), 300) * np.exp(-T(0.25) * 18) * 0.9, d - 0.3)
    return buf * env(len(buf), 0.005, 0.3)


def fx_confirm(v):
    """Two rising tones, a fifth apart: accepted."""
    d = 0.45
    buf = np.zeros(int(SR * d))
    for at, f in ((0.0, 880 + 40 * v), (0.11, 1318 + 60 * v)):
        x = (tone(f, 0.3) + tone(f * 2, 0.3) * 0.25) * np.exp(-T(0.3) * 12)
        place(buf, x, at)
    return buf


def fx_resonate(v):
    """A lattice ringing: clustered inharmonic partials beating slowly against each other."""
    d = 1.8
    t = T(d)
    f0 = 520 + 60 * v
    y = sum(tone(f0 * k * (1 + 0.003 * j), d) * np.exp(-t * (1.0 + k * 0.6)) / (1 + k) for j, k in enumerate((1, 1.505, 2.02, 2.97, 4.13)))
    return y * (0.7 + 0.3 * np.sin(2 * np.pi * 3.3 * t)) * env(len(y), 0.03, 0.8)


def fx_chime(v):
    """A single bright crystal ping."""
    return bell(1320 + 180 * v, 1.0) * 0.8 + hp(noise(1.0), 6000) * np.exp(-T(1.0) * 50) * 0.2


def fx_shatter(v):
    """A lattice breaking: a sharp crack, glassy shards tinkling down."""
    d = 0.9
    t = T(d)
    crack = hp(noise(d), 2500) * np.exp(-t * 35)
    buf = crack.copy()
    for k in range(9):
        place(buf, clink(1800 + rng.random() * 2600, 0.25, decay=18) * (0.5 - k * 0.04), 0.02 + k * 0.07 + rng.random() * 0.03)
    return buf


def fx_phase_shift(v):
    """Folded through a dimension: a reversed swell snaps into a bent, falling tone."""
    d = 0.9
    t = T(d)
    swell = (hp(noise(0.45), 1500) * np.linspace(0, 1, int(SR * 0.45)) ** 3)
    bend = (_sweep(1600 + 200 * v, 180, 0.5) + _sweep(2400, 270, 0.5) * 0.4) * np.exp(-T(0.5) * 5)
    buf = np.zeros(int(SR * d))
    place(buf, swell * 0.5, 0)
    place(buf, bend, 0.42)
    return crush(buf, 9, 2)


def fx_rift_hum(v):
    """An open rift: a low drone, slowly phasing, with a hiss of something passing through."""
    d = 3.0
    t = T(d)
    drone = sum(tone(f * (1 + 0.006 * np.sin(2 * np.pi * 0.4 * t * (k + 1))), d) / (k + 1) for k, f in enumerate((55, 82.4, 110, 164.8)))
    hiss = bp(noise(d), 2000, 6000) * (0.3 + 0.3 * np.sin(2 * np.pi * 0.7 * t))
    return (drone + hiss * 0.4) * env(len(drone), 0.6, 0.6)


def fx_charge(v):
    """Energy gathering: a whine climbing to a tight trill."""
    d = 1.4
    t = T(d)
    f = 200 * (12 ** (t / d))
    y = tone(f, d, 'saw') * 0.2 + tone(f * 1.5, d) * 0.3
    trill = 1 + 0.5 * np.sin(2 * np.pi * (8 + 30 * t / d) * t)
    return lp(y * trill, 6000) * (t / d) ** 1.5 * env(len(y), 0.05, 0.05)


def fx_pulse(v):
    """A low, double heartbeat of the field."""
    d = 0.9
    buf = np.zeros(int(SR * d))
    for at, a in ((0.0, 1.0), (0.22, 0.7)):
        x = tone(_f := 48, 0.35) * np.exp(-T(0.35) * 9) + lp(noise(0.35), 120) * np.exp(-T(0.35) * 14) * 0.6
        place(buf, x * a, at)
    return buf


def fx_emergence(v):
    """Something vast arriving: sub rumble, a formant roar rising out of it, ringing metal."""
    d = 3.2
    t = T(d)
    rumble = lp(noise(d), 90) * 2.0 * env(int(SR * d), 0.3, 1.2)
    f = 70 * (1 + 0.6 * np.clip(t / 1.6, 0, 1)) * (1 + 0.03 * np.sin(2 * np.pi * 5 * t))
    src = tone(f, d, 'saw')
    roar = (bp(src, 300, 700) + bp(src, 1000, 1600) * 0.6 + bp(noise(d), 1800, 3200) * 0.3) * env(int(SR * d), 0.5, 1.4)
    buf = mix(rumble, roar)
    place(buf, bell(110, 2.0) * 0.4, 0.6)
    return buf


def fx_shockwave(v):
    """A pressure front: a hard crack, a low boom, and air rushing back in."""
    d = 1.8
    t = T(d)
    crack = hp(noise(d), 1800) * np.exp(-t * 28) * 1.2
    boom = (tone(55 * np.exp(-t * 0.8) + 25, d) + lp(noise(d), 160) * 1.5) * np.exp(-t * 2.4)
    rush = bp(noise(d), 400, 2500) * np.clip((t - 0.25) / 0.5, 0, 1) * np.exp(-np.clip(t - 0.4, 0, None) * 3) * 0.4
    return mix(crack, boom, rush)


def fx_ascend(v):
    """Understanding arrives: a quick rising arpeggio of bells."""
    d = 1.4
    buf = np.zeros(int(SR * d))
    for k, f in enumerate((523.3, 659.3, 784.0, 1046.5, 1318.5)):
        place(buf, bell(f, 0.9) * (0.5 + 0.1 * k), k * 0.08)
    return buf


def fx_gateway(v):
    """A gateway forming: a wide chord swells from nothing, with a sparkle on top."""
    d = 3.5
    t = T(d)
    chord = sum(tone(f * (1 + 0.002 * k), d) * a for k, (f, a) in enumerate(((65.4, 1), (98, 0.7), (130.8, 0.6), (196, 0.5), (261.6, 0.4), (392, 0.3))))
    spark = hp(noise(d), 7000) * (0.5 + 0.5 * np.sin(2 * np.pi * 9 * t)) * 0.15
    return (chord * 0.4 + spark) * np.clip(t / 2.0, 0, 1) ** 2 * env(len(chord), 0.01, 0.8)


def fx_vent(v):
    """Coolant venting: a pressurised hiss that falls away."""
    d = 1.0
    t = T(d)
    return bp(noise(d), 1500 + 300 * v, 9000) * np.exp(-t * 3) * env(int(SR * d), 0.01, 0.4) + lp(noise(d), 400) * np.exp(-t * 8) * 0.4


def fx_fluid_fill(v):
    """A dense fluid drawn up a cell: a gulp rising in pitch."""
    d = 0.5
    t = T(d)
    y = sum(tone(np.linspace(200 + 120 * k, 500 + 180 * k, 16), 0.12) * np.exp(-T(0.12) * 25) for k in range(1))
    buf = np.zeros(int(SR * d))
    for k in range(4):
        place(buf, tone(np.linspace(220 + 70 * k, 520 + 90 * k, 16), 0.12) * np.exp(-T(0.12) * 25), k * 0.09)
    return buf + lp(noise(d), 900) * np.exp(-t * 6) * 0.3


def fx_fluid_empty(v):
    """Poured out: falling gulps over a wash."""
    d = 0.6
    t = T(d)
    buf = np.zeros(int(SR * d))
    for k in range(4):
        place(buf, tone(np.linspace(520 - 70 * k, 240 - 40 * k, 16), 0.12) * np.exp(-T(0.12) * 25), k * 0.09)
    return buf + bp(noise(d), 300, 2500) * np.exp(-t * 4) * 0.4


def fx_mutate(v):
    """Matter rewritten: a wet, glitching warble."""
    d = 1.2
    t = T(d)
    y = tone(180 * (1 + 0.5 * np.sin(2 * np.pi * 7 * t)), d, 'saw')
    y = bp(y, 300, 1800) * env(int(SR * d), 0.05, 0.4)
    return glitch(crush(y, 6, 3), 0.03, 0.3, 0.2, 0.05)[:int(SR * d)]


def fx_hatch_open(v):
    """A servo whirs, a magnetic latch releases."""
    d = 0.6
    t = T(d)
    servo = lp(tone(np.linspace(300, 600, 16), 0.4, 'saw'), 1500) * 0.3 * env(int(SR * 0.4), 0.02, 0.1)
    buf = np.zeros(int(SR * d))
    place(buf, servo, 0)
    place(buf, clink(220, 0.25, decay=18), 0.38)
    return buf


def fx_hatch_close(v):
    """A servo whirs down, the latch seats with a thunk."""
    d = 0.6
    servo = lp(tone(np.linspace(600, 300, 16), 0.4, 'saw'), 1500) * 0.3 * env(int(SR * 0.4), 0.02, 0.1)
    buf = np.zeros(int(SR * d))
    place(buf, servo, 0)
    place(buf, clink(140, 0.3, decay=14) * 1.2 + lp(noise(0.3), 300) * np.exp(-T(0.3) * 20) * 0.6, 0.36)
    return buf


def fx_equip(v):
    """Armour locking on: magnetic seals clicking shut in sequence, a brief hum."""
    d = 0.8
    buf = np.zeros(int(SR * d))
    for k in range(4):
        place(buf, clink(400 + 90 * k, 0.15, decay=30) * 0.6, k * 0.06)
    place(buf, (tone(220, 0.5) + tone(330, 0.5) * 0.5) * env(int(SR * 0.5), 0.05, 0.3) * 0.4, 0.25)
    return buf


def fx_alarm(v):
    """A clipped containment klaxon blip."""
    d = 0.25
    t = T(d)
    y = tone(np.where(t < 0.12, 880, 660), d, 'tri') + tone(np.where(t < 0.12, 1760, 1320), d) * 0.3
    return lp(y, 2500) * np.exp(-((t % 0.12) * 14)) * env(int(SR * d), 0.005, 0.05)


# block sounds: machines (a servo-and-magnet feel) and crystals
def blk_machine(kind):
    def f(v):
        if kind == 'break':
            buf = mix(clink(160 + 30 * v, 0.5, decay=8) * 1.2, lp(noise(0.5), 500) * np.exp(-T(0.5) * 10))
            for k in range(4):
                place(buf, hp(noise(0.03), 3000) * np.exp(-T(0.03) * 120) * 0.5, 0.05 + rng.random() * 0.3)
            return buf
        if kind == 'place':
            return mix(clink(200 + 25 * v, 0.35, decay=14), (tone(110, 0.3) * env(int(SR * 0.3), 0.01, 0.2)) * 0.5)
        if kind == 'step':
            return clink(320 + 40 * v, 0.12, decay=40) * 0.7
        if kind == 'hit':
            return clink(260 + 50 * v, 0.2, decay=25) * 0.8
        return clink(140, 0.3, decay=16)          # fall
    return f


def blk_crystal(kind):
    def f(v):
        if kind == 'break':
            return fx_shatter(v)
        if kind == 'place':
            return mix(clink(1400 + 200 * v, 0.4, decay=10) * 0.8, bell(1760 + 200 * v, 0.6) * 0.3)
        if kind == 'step':
            return clink(1800 + 300 * v, 0.15, decay=35) * 0.6
        if kind == 'hit':
            return clink(1500 + 250 * v, 0.2, decay=25) * 0.7
        return clink(1100, 0.3, decay=16)
    return f


FX = {
    'ui_click': (fx_ui_click, 2, 'Console clicks'),
    'power_up': (fx_power_up, 1, 'Field powers up'),
    'power_down': (fx_power_down, 1, 'Field powers down'),
    'confirm': (fx_confirm, 2, 'Console confirms'),
    'resonate': (fx_resonate, 2, 'Lattice resonates'),
    'chime': (fx_chime, 2, 'Crystal chimes'),
    'shatter': (fx_shatter, 2, 'Lattice shatters'),
    'phase_shift': (fx_phase_shift, 2, 'Something phases through space'),
    'rift_hum': (fx_rift_hum, 1, 'Rift hums'),
    'charge': (fx_charge, 1, 'Energy gathers'),
    'pulse': (fx_pulse, 1, 'Field pulses'),
    'emergence': (fx_emergence, 1, 'Something vast arrives'),
    'shockwave': (fx_shockwave, 2, 'Shockwave'),
    'ascend': (fx_ascend, 1, 'Understanding'),
    'gateway': (fx_gateway, 1, 'A gateway forms'),
    'vent': (fx_vent, 2, 'Coolant vents'),
    'fluid_fill': (fx_fluid_fill, 2, 'Fluid drawn up'),
    'fluid_empty': (fx_fluid_empty, 2, 'Fluid poured out'),
    'mutate': (fx_mutate, 1, 'Matter is rewritten'),
    'hatch_open': (fx_hatch_open, 1, 'Hatch opens'),
    'hatch_close': (fx_hatch_close, 1, 'Hatch closes'),
    'equip': (fx_equip, 1, 'Armour seals'),
    'alarm': (fx_alarm, 1, 'Containment alarm'),
}
for _k, _v in FX.items():
    SOUNDS['fx.' + _k] = _v
for _k in ('break', 'place', 'step', 'hit', 'fall'):
    SOUNDS['machine_block.' + _k] = (blk_machine(_k), 3 if _k in ('step', 'hit') else 2, {'break': 'Machine dismantled', 'place': 'Machine placed', 'step': 'Footsteps', 'hit': 'Machine struck', 'fall': 'Something falls'}[_k])
    SOUNDS['crystal_block.' + _k] = (blk_crystal(_k), 3 if _k in ('step', 'hit') else 2, {'break': 'Crystal shatters', 'place': 'Crystal placed', 'step': 'Footsteps', 'hit': 'Crystal struck', 'fall': 'Something falls'}[_k])


def main():
    sounds_json = {}
    lang = {}
    with tempfile.TemporaryDirectory() as tmp:
        for event, (fn, variants, subtitle) in SOUNDS.items():
            mob, name = event.split('.')
            os.makedirs(os.path.join(OUT, mob), exist_ok=True)
            files = []
            for v in range(variants):
                y = norm(np.asarray(fn(v), dtype=float))
                wav = os.path.join(tmp, 'x.wav')
                wavfile.write(wav, SR, (y * 32767).astype(np.int16))
                ogg = os.path.join(OUT, mob, f'{name}{v + 1}.ogg')
                subprocess.run(['ffmpeg', '-y', '-loglevel', 'error', '-i', wav, '-ac', '1', '-c:a', 'libvorbis', '-q:a', '4', ogg], check=True)
                files.append(f'theoreticalsciences:entity/{mob}/{name}{v + 1}')
            key = f'entity.{event}'
            sounds_json[key] = {'subtitle': f'subtitles.theoreticalsciences.{key}', 'sounds': files}
            lang[f'subtitles.theoreticalsciences.{key}'] = subtitle
    json.dump(sounds_json, open(os.path.join(A, 'sounds.json'), 'w'), indent=1)
    json.dump(lang, open(os.path.join(ROOT, 'sound_lang.json'), 'w'), indent=1)
    print('sounds:', len(sounds_json), 'events')


if __name__ == '__main__':
    main()
