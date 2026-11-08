"""
1.3.0 creature sounds, designed one by one (everything synthesised here: no samples, no licensing questions).

Each creature gets sounds that follow what it is:
  * Woolly Mammoth: a trunk-resonated trumpet (a rough, nasal brass voice that rises and breaks), low rumbles with a
    slow throb, snorts through the trunk, and heavy footfalls that thud into the ground.
  * Meganeura: the beat of four big papery wings (each stroke a whoosh and a crackle of chitin film, fore and hind
    pairs out of step), darting changes of pitch, and dry clicks.
  * Karman Wraith: the Aeolian tone of a wire in the wind (a whistle at the vortex-shedding frequency, wavering as the
    wind does, with the faint octave of the alternating eddies), over breathy wind; its gust is a swept whoosh.
  * Storm Elemental: thunder (a sharp crack, then a long rumble that rolls and echoes), rain hiss, crackling charge; its
    strike is a close lightning crack with a sizzle.
  * Hadal Anglerfish: everything heard through water (muffled): swim-bladder drumming grunts, the creak of a jaw, bubbles,
    and for its lunge a rush of water and a snap of teeth.
  * Siphonophore: soft, wet pumping pulses as the swimming bells contract, a glassy shimmer, and for its sting a burst of
    tiny clicks (thousands of nematocysts firing) with a zap.

Writes mono Ogg Vorbis (Minecraft only positions mono sounds) and updates sounds.json and the subtitles.
"""
import json, os, subprocess, tempfile
import numpy as np
from scipy import signal
from scipy.io import wavfile

ROOT = os.path.dirname(os.path.abspath(__file__))
A = os.path.join(ROOT, '..', 'src/main/resources/assets/theoreticalsciences')
OUT = os.path.join(A, 'sounds', 'entity')
SR = 44100
rng = np.random.default_rng(1300)


# ------------------------------------------------------------------ building blocks
def n_(d):
    return int(SR * d)


def t_(d):
    return np.arange(n_(d)) / SR


def curve(points, d):
    """A piecewise-linear control curve over d seconds from [(time_fraction, value), ...]."""
    xs, ys = zip(*points)
    return np.interp(np.linspace(0, 1, n_(d)), xs, ys)


def adsr(d, a=0.01, dec=0.05, s=0.8, r=0.2, shape=2.0):
    n = n_(d); t = np.arange(n) / SR
    e = np.where(t < a, t / max(a, 1e-4), s + (1 - s) * np.exp(-(t - a) / max(dec, 1e-4)))
    rel = np.clip((d - t) / max(r, 1e-4), 0, 1) ** shape
    return e * rel


def osc(freq, d, kind='saw', jitter=0.0):
    n = n_(d)
    f = np.full(n, float(freq)) if np.isscalar(freq) else np.asarray(freq, float)[:n]
    if jitter:
        f = f * (1 + jitter * lp(rng.standard_normal(n), 30, 1) * 8)
    ph = 2 * np.pi * np.cumsum(f) / SR
    if kind == 'sine':
        return np.sin(ph)
    if kind == 'saw':                           # band-limited-ish: sum of harmonics under Nyquist
        out = np.zeros(n)
        fmax = np.max(f) if n else 1
        for k in range(1, int(min(40, 18000 / max(fmax, 1))) + 1):
            out += np.sin(k * ph) / k * (np.max(k * f) < 20000)
        return out * 0.6
    if kind == 'pulse':                          # glottal-like: narrow pulses rich in harmonics
        out = np.zeros(n)
        fmax = np.max(f) if n else 1
        for k in range(1, int(min(60, 18000 / max(fmax, 1))) + 1):
            out += np.sin(k * ph) * (0.92 ** k)
        return out * 0.35
    raise ValueError(kind)


def noise(d, color='white'):
    x = rng.standard_normal(n_(d))
    if color == 'pink':
        b, a = [0.049922035, -0.095993537, 0.050612699, -0.004408786], [1, -2.494956002, 2.017265875, -0.522189400]
        x = signal.lfilter(b, a, x) * 4
    if color == 'brown':
        x = np.cumsum(x); x = x - lp(x, 5, 1); x /= np.max(np.abs(x)) + 1e-9
    return x


def lp(x, f, order=2):
    b, a = signal.butter(order, min(max(f, 5) / (SR / 2), 0.99), btype='low')
    return signal.lfilter(b, a, x)


def hp(x, f, order=2):
    b, a = signal.butter(order, min(f / (SR / 2), 0.99), btype='high')
    return signal.lfilter(b, a, x)


def bp(x, lo, hi, order=2):
    b, a = signal.butter(order, [max(lo, 5) / (SR / 2), min(hi / (SR / 2), 0.99)], btype='band')
    return signal.lfilter(b, a, x)


def reso(x, f, q):
    """A resonant peak (formant) at f with quality q."""
    b, a = signal.iirpeak(min(f, SR / 2 - 100) / (SR / 2), q)
    return signal.lfilter(b, a, x)


def sweep_bp(x, fc, bw_ratio=0.5, step=256):
    """A band-pass whose centre follows the curve fc (per sample), processed in short blocks."""
    out = np.zeros_like(x); zi = None; prev = None
    for i in range(0, len(x), step):
        f = float(np.clip(fc[min(i, len(fc) - 1)], 40, 18000))
        lo, hi = f * (1 - bw_ratio / 2), f * (1 + bw_ratio / 2)
        b, a = signal.butter(2, [lo / (SR / 2), min(hi / (SR / 2), 0.99)], btype='band')
        if zi is None or prev is None:
            zi = signal.lfilter_zi(b, a) * 0
        seg, zi = signal.lfilter(b, a, x[i:i + step], zi=zi)
        out[i:i + step] = seg; prev = f
    return out


def sweep_lp(x, fc, step=256):
    out = np.zeros_like(x); zi = None
    for i in range(0, len(x), step):
        f = float(np.clip(fc[min(i, len(fc) - 1)], 40, 18000))
        b, a = signal.butter(2, f / (SR / 2), btype='low')
        if zi is None:
            zi = signal.lfilter_zi(b, a) * 0
        seg, zi = signal.lfilter(b, a, x[i:i + step], zi=zi)
        out[i:i + step] = seg
    return out


def place(buf, x, at):
    i = n_(at); j = min(len(buf), i + len(x))
    if j > i:
        buf[i:j] += x[:j - i]
    return buf


def reverb(x, room=0.35, decay=1.2, wet=0.25, tone=4000):
    """A simple diffuse tail: noise impulse response, exponentially decaying and darkened."""
    ir = rng.standard_normal(n_(decay)) * np.exp(-np.arange(n_(decay)) / (SR * decay * room))
    ir = lp(ir, tone); ir /= np.sqrt(np.sum(ir ** 2)) + 1e-9
    y = signal.fftconvolve(x, ir)[:len(x) + n_(decay * 0.6)]
    dry = np.concatenate([x, np.zeros(len(y) - len(x))])
    return dry * (1 - wet) + y * wet


def softclip(x, k=1.5):
    return np.tanh(k * x) / np.tanh(k)


def finish(x, peak_db=-1.7, fade=0.01):
    x = x - np.mean(x)
    x = hp(x, 25, 2)
    f = n_(fade)
    if f and len(x) > 2 * f:
        x[:f] *= np.linspace(0, 1, f); x[-f:] *= np.linspace(1, 0, f)
    m = np.max(np.abs(x)) + 1e-9
    return x / m * 10 ** (peak_db / 20)


def write(mob, name, x):
    d = os.path.join(OUT, mob)
    os.makedirs(d, exist_ok=True)
    with tempfile.NamedTemporaryFile(suffix='.wav', delete=False) as tf:
        wavfile.write(tf.name, SR, (finish(x) * 32767).astype(np.int16))
        subprocess.run(['ffmpeg', '-v', 'error', '-y', '-i', tf.name, '-ac', '1', '-c:a', 'libvorbis', '-q:a', '5',
                        os.path.join(d, name + '.ogg')], check=True)
    os.unlink(tf.name)


# ------------------------------------------------------------------ Woolly Mammoth
def trunk_voice(f0, d, rough=0.3, nasal=1.0):
    """The trunk as a long brass tube: a rough, pulsed source through nasal formants, with breath."""
    src = osc(f0, d, 'pulse', jitter=0.004)
    sub = osc(np.asarray(f0) * 0.5 if not np.isscalar(f0) else f0 * 0.5, d, 'saw') * rough   # the rasp: a subharmonic
    am = 1 + rough * 0.6 * np.sin(2 * np.pi * np.cumsum(np.full(n_(d), 31.0)) / SR)       # growl flutter
    x = (src + sub) * am
    y = reso(x, 650, 4) * 1.0 + reso(x, 1300, 5) * 0.8 * nasal + reso(x, 2600, 6) * 0.5 + reso(x, 3900, 8) * 0.25
    breath = bp(noise(d), 900, 4500) * 0.12
    return softclip(y * 0.5 + breath, 1.8)


def mammoth_trumpet(d=1.5, peak=520, start=330, end=260, rough=0.35):
    f0 = curve([(0, start), (0.12, peak * 0.9), (0.3, peak), (0.55, peak * 1.04), (0.8, peak * 0.85), (1, end)], d)
    x = trunk_voice(f0, d, rough) * adsr(d, 0.06, 0.3, 0.85, 0.35)
    return reverb(x, 0.4, 1.3, 0.22)


def mammoth_rumble(d=2.2, f=48):
    f0 = curve([(0, f * 1.1), (0.5, f), (1, f * 0.9)], d)
    x = osc(f0, d, 'saw') * 0.8 + osc(f0 * 2, d, 'sine') * 0.3
    x = lp(x, 260, 2) + reso(x, 180, 3) * 0.4
    throb = 0.65 + 0.35 * np.sin(2 * np.pi * 2.3 * t_(d))
    return x * throb * adsr(d, 0.25, 0.4, 0.8, 0.6) * 1.4


def mammoth_snort(d=0.45):
    x = bp(noise(d), 300, 2400) * adsr(d, 0.005, 0.08, 0.3, 0.2)
    x = reso(x, 700, 3) + x * 0.4
    return x * 1.2


def mammoth_step(d=0.55, f=42):
    t = t_(d)
    thud = np.sin(2 * np.pi * (f * t + 20 * (1 - np.exp(-t * 18)) / 18)) * np.exp(-t * 9)
    knock = lp(noise(d), 600) * np.exp(-t * 45) * 0.5
    crunch = bp(noise(d), 1500, 5000) * np.exp(-t * 30) * 0.12
    return reverb(thud * 1.2 + knock + crunch, 0.3, 0.6, 0.15)


def mammoth():
    m = 'mammoth'
    a1 = place(np.zeros(n_(2.6)), mammoth_rumble(2.4, 46), 0.1)
    write(m, 'ambient1', a1)
    a2 = np.zeros(n_(1.9)); place(a2, mammoth_snort(0.4), 0.05); place(a2, mammoth_rumble(1.4, 52) * 0.8, 0.4); write(m, 'ambient2', a2)
    a3 = np.zeros(n_(1.8)); place(a3, mammoth_trumpet(0.9, 460, 300, 280, 0.25) * 0.55, 0.05); place(a3, mammoth_snort(0.3) * 0.6, 1.15)
    write(m, 'ambient3', lp(a3, 3500))                                   # a softer, more distant call
    h1 = mammoth_trumpet(0.65, 720, 520, 560, 0.55); write(m, 'hurt1', h1)
    h2 = np.zeros(n_(0.8)); place(h2, mammoth_trumpet(0.5, 680, 600, 480, 0.6), 0); place(h2, mammoth_snort(0.25), 0.45); write(m, 'hurt2', h2)
    d1 = np.zeros(n_(3.2)); place(d1, mammoth_trumpet(1.3, 600, 420, 240, 0.5), 0); place(d1, mammoth_rumble(2.0, 40) * 0.9, 1.0)
    write(m, 'death1', d1)
    for i, (f, dd) in enumerate([(40, 0.55), (46, 0.5), (36, 0.6)]):
        write(m, f'step{i + 1}', mammoth_step(dd, f))
    tr = np.zeros(n_(1.9)); place(tr, mammoth_snort(0.2) * 0.6, 0); place(tr, mammoth_trumpet(1.55, 560, 330, 300, 0.4), 0.15)
    write(m, 'trumpet1', tr)
    tr2 = mammoth_trumpet(1.4, 610, 380, 340, 0.45); write(m, 'trumpet2', tr2)


# ------------------------------------------------------------------ Meganeura
def wingbeats(d, rate, rate_end=None, crackle=1.0, pitch=1.0):
    """Fore and hind wing pairs beating out of step: each downstroke a soft whoosh, each turn a papery tick."""
    rate_end = rate if rate_end is None else rate_end
    rt = np.linspace(rate, rate_end, n_(d))
    ph = np.cumsum(rt) / SR
    fore = np.clip(np.sin(2 * np.pi * ph), 0, 1) ** 3
    hind = np.clip(np.sin(2 * np.pi * ph + 1.9), 0, 1) ** 3
    whoosh = bp(noise(d, 'pink'), 120 * pitch, 900 * pitch) * (fore + 0.8 * hind)
    turns = np.abs(np.diff(np.floor(ph * 2), prepend=0)) > 0
    ticks = np.zeros(n_(d)); ticks[turns] = 1
    tick = signal.lfilter([1], [1, -0.6], ticks * rng.uniform(0.5, 1, n_(d)))
    paper = bp(noise(d), 2500 * pitch, 9000) * (lp(tick, 400) * 30) * crackle
    body = osc(rt * 1.0, d, 'sine') * 0.25 * (fore + hind)              # the low thrum at the wingbeat rate
    return whoosh * 1.5 + paper * 0.35 + body


def meganeura():
    m = 'meganeura'
    d = 1.5; a = wingbeats(d, 24, 26) * curve([(0, 0), (0.15, 1), (0.85, 1), (1, 0)], d)
    write(m, 'ambient1', a)
    d = 1.1; a = wingbeats(d, 22, 31, 1.2, 1.15) * curve([(0, 0), (0.1, 0.7), (0.5, 1), (0.75, 1), (1, 0)], d)   # a dart
    write(m, 'ambient2', a)
    for i, dd in enumerate([0.45, 0.4]):
        x = wingbeats(dd, 34, 30, 2.5, 1.3) * adsr(dd, 0.005, 0.1, 0.7, 0.15)
        clicks = np.zeros(n_(dd))
        for k in range(3 + i):
            place(clicks, hp(noise(0.012), 3000) * np.exp(-np.arange(n_(0.012)) / (SR * 0.003)), 0.02 + k * 0.05)
        write(m, f'hurt{i + 1}', x + clicks * 0.8)
    d = 1.3; x = wingbeats(d, 26, 6, 1.5, 0.9) * curve([(0, 1), (0.6, 0.7), (1, 0)], d)
    crumple = bp(noise(d), 2000, 8000) * (rng.random(n_(d)) < 0.004) * 6
    write(m, 'death1', x + lp(crumple, 6000) * curve([(0, 0), (0.6, 0), (0.7, 1), (1, 0)], d))


# ------------------------------------------------------------------ Karman Wraith
def aeolian(d, f, wobble=0.03, octave=0.25, breath=0.35, gust=None):
    """The tone of a wire in the wind: shedding frequency f (St*U/D), wavering as the wind does."""
    wind = 1 + wobble * lp(rng.standard_normal(n_(d)), 3, 1) * 30 + 0.01 * np.sin(2 * np.pi * 5.5 * t_(d))
    if gust is not None:
        wind = wind * gust
    fr = f * wind
    tone = osc(fr, d, 'sine') + octave * osc(fr * 2, d, 'sine') + 0.12 * osc(fr * 3, d, 'sine')
    lock = np.clip(0.5 + 0.5 * lp(rng.standard_normal(n_(d)), 1.5, 1) * 40, 0.2, 1)   # the tone locking in and out
    air = sweep_bp(noise(d, 'pink'), fr * 1.3, 1.2) * breath
    return tone * lock * 0.6 + air


def gust_whoosh(d=0.9):
    fc = curve([(0, 300), (0.3, 1600), (0.6, 900), (1, 200)], d)
    x = sweep_bp(noise(d), fc, 0.9) * adsr(d, 0.15, 0.2, 0.7, 0.4)
    return x * 2.5


def karman():
    m = 'karman_wraith'
    d = 2.2; g = curve([(0, 0.9), (0.4, 1.08), (0.7, 1.0), (1, 0.95)], d)
    write(m, 'ambient1', aeolian(d, 680, gust=g) * curve([(0, 0), (0.25, 1), (0.75, 1), (1, 0)], d))
    d = 1.8; g = curve([(0, 1.0), (0.5, 1.25), (1, 1.1)], d)
    a = aeolian(d, 540, 0.05, 0.4, gust=g) + aeolian(d, 540 * 1.5, 0.05, 0.1, 0.1, gust=g) * 0.4      # a fifth, as two wires sing
    write(m, 'ambient2', a * curve([(0, 0), (0.3, 1), (0.7, 1), (1, 0)], d))
    for i, (f, dd) in enumerate([(700, 0.45), (620, 0.4)]):
        g = curve([(0, 1), (0.3, 1.6), (1, 0.7)], dd)
        x = aeolian(dd, f, 0.12, 0.3, 0.9, gust=g) + bp(noise(dd), 600, 4000) * 0.6
        write(m, f'hurt{i + 1}', x * adsr(dd, 0.005, 0.1, 0.6, 0.2))
    d = 1.8; g = curve([(0, 1.0), (0.3, 0.8), (1, 0.25)], d)
    x = aeolian(d, 640, 0.06, 0.3, 0.8, gust=g) * curve([(0, 1), (0.5, 0.6), (1, 0)], d) + gust_whoosh(d) * 0.5
    write(m, 'death1', x)
    write(m, 'gust1', gust_whoosh(0.9) + aeolian(0.9, 900, 0.1, 0.2, 0.2, curve([(0, 0.7), (0.4, 1.3), (1, 0.8)], 0.9)) * 0.3 * adsr(0.9, 0.2, 0.2, 0.6, 0.3))
    write(m, 'gust2', gust_whoosh(1.1) * 1.1)


# ------------------------------------------------------------------ Storm Elemental
def thunder(d, crack=0.6, rumble_f=90, rolls=5, dist=0.0):
    """A crack, then a rumble that rolls (several delayed, darkening echoes off the cloud and ground)."""
    x = np.zeros(n_(d))
    t = t_(0.08)
    if crack > 0:
        c = hp(noise(0.08), 800) * np.exp(-t * 60) * crack
        x = place(x, lp(c, 9000 * (1 - dist * 0.8)), 0)
    for k in range(rolls):
        at = 0.04 + k * d / (rolls + 1) * rng.uniform(0.6, 1.1)
        dd = d - at
        if dd <= 0.05:
            continue
        r = lp(noise(dd, 'brown'), rumble_f * rng.uniform(0.8, 1.6)) + lp(noise(dd), 220) * 0.3
        e = np.exp(-np.arange(n_(dd)) / (SR * dd * 0.35)) * (1 - np.exp(-np.arange(n_(dd)) / (SR * 0.05)))
        x = place(x, r * e * (1.0 - k * 0.12), at)
    return x


def rain(d, amount=0.25):
    drops = (rng.random(n_(d)) < 0.002) * rng.uniform(0.3, 1, n_(d))
    patter = bp(signal.lfilter([1], [1, -0.7], drops), 1500, 7000) * 1.5
    hiss = bp(noise(d), 3000, 10000) * 0.15
    return (patter + hiss) * amount


def charge_crackle(d, density=0.01):
    sp = (rng.random(n_(d)) < density) * rng.uniform(-1, 1, n_(d))
    return hp(signal.lfilter([1], [1, -0.3], sp), 2000) * 2


def storm():
    m = 'storm_elemental'
    d = 2.6; x = thunder(d, 0, 70, 4, dist=0.8) * 1.4 + rain(d, 0.2) + charge_crackle(d, 0.0015) * 0.3
    write(m, 'ambient1', x * curve([(0, 0), (0.1, 1), (0.8, 1), (1, 0)], d))
    d = 2.0; wind = sweep_bp(noise(d, 'pink'), curve([(0, 300), (0.5, 700), (1, 350)], d), 1.0) * 0.8
    write(m, 'ambient2', (wind + rain(d, 0.35) + thunder(d, 0, 60, 2, dist=1.0) * 0.8) * curve([(0, 0), (0.2, 1), (0.8, 1), (1, 0)], d))
    for i, dd in enumerate([0.7, 0.6]):
        x = charge_crackle(dd, 0.02) * adsr(dd, 0.002, 0.08, 0.4, 0.2) + thunder(dd, 0.4, 120, 2) * 0.6
        write(m, f'hurt{i + 1}', x)
    d = 2.4; x = thunder(d, 1.0, 80, 6) * 1.2 + rain(d, 0.3) * curve([(0, 0.2), (0.5, 1), (1, 0)], d)
    write(m, 'death1', x)
    for i in range(2):                                                       # the strike: close, sharp, sizzling
        d = 1.9
        x = thunder(d, 1.4, 110, 5) * 1.1
        place(x, charge_crackle(0.35, 0.05) * np.linspace(1, 0, n_(0.35)) * 0.6, 0.0)
        hum = osc(curve([(0, 120), (1, 60)], 0.3), 0.3, 'saw') * np.linspace(1, 0, n_(0.3)) ** 2 * 0.25
        place(x, hum, 0)
        x = softclip(x / (np.max(np.abs(x)) + 1e-9) * 3.5, 1.0)          # the crack limited, so the roll carries
        write(m, f'strike{i + 1}', x)


# ------------------------------------------------------------------ Hadal Anglerfish (heard through water)
def underwater(x, f=900):
    return lp(lp(x, f, 2), f * 1.6, 2)


def bubbles(d, count=6, f_lo=300, f_hi=900):
    x = np.zeros(n_(d))
    for _ in range(count):
        dd = rng.uniform(0.03, 0.08); t = t_(dd)
        f0 = rng.uniform(f_lo, f_hi)
        b = np.sin(2 * np.pi * f0 * t * (1 + 0.8 * t / dd)) * np.exp(-t / (dd * 0.3))      # a bubble's rising ring
        place(x, b, rng.uniform(0, max(0.01, d - dd)))
    return x * 0.5


def grunt(d, f=110, pulses=None):
    """Swim-bladder drumming: rapid muscle twitches give a pulsed, knocking croak."""
    pr = pulses or f
    t = t_(d)
    train = (np.sin(2 * np.pi * np.cumsum(np.full(n_(d), pr * 0.4)) / SR) > 0.95).astype(float)
    knock = signal.lfilter([1], [1, -1.9 * np.cos(2 * np.pi * f / SR), 0.95], train)
    return knock * 0.08 * adsr(d, 0.03, 0.1, 0.8, 0.15)


def angler():
    m = 'hadal_anglerfish'
    d = 2.0; x = np.zeros(n_(d)); place(x, grunt(0.6, 95), 0.1); place(x, grunt(0.5, 85) * 0.8, 0.9); place(x, bubbles(0.6, 4), 1.3)
    write(m, 'ambient1', underwater(reverb(x, 0.6, 1.5, 0.35, 1500)))
    d = 1.6; creak = bp(noise(d), 200, 700) * (np.sin(2 * np.pi * np.cumsum(curve([(0, 18), (1, 9)], d)) / SR) > 0.6) * curve([(0, 0), (0.2, 1), (0.8, 1), (1, 0)], d)
    write(m, 'ambient2', underwater(reverb(creak * 0.9 + bubbles(d, 5) * 0.6, 0.6, 1.4, 0.35, 1500)))
    for i, dd in enumerate([0.5, 0.45]):
        t = t_(dd); thump = np.sin(2 * np.pi * 70 * t) * np.exp(-t * 14)
        write(m, f'hurt{i + 1}', underwater(thump + grunt(dd, 140) * 1.5 + bubbles(dd, 6, 500, 1400)))
    d = 2.0; x = grunt(1.2, 120) * curve([(0, 1), (1, 0.2)], 1.2)
    x = np.concatenate([x, np.zeros(n_(0.8))]); place(x, bubbles(1.4, 12, 250, 800), 0.4)
    write(m, 'death1', underwater(reverb(x, 0.6, 1.4, 0.3, 1500)))
    for i in range(2):                                                       # the lunge: a rush of water, then the jaws
        d = 0.75
        rush = sweep_bp(noise(d), curve([(0, 150), (0.5, 500), (1, 200)], d), 1.2) * adsr(d, 0.08, 0.15, 0.6, 0.3) * 2.2
        t = t_(0.06); snap = (hp(noise(0.06), 400) * np.exp(-t * 90) + np.sin(2 * np.pi * 380 * t) * np.exp(-t * 60)) * 1.4
        place(rush, snap, 0.32 + i * 0.04)
        place(rush, bubbles(0.3, 5, 400, 1200), 0.38)
        write(m, f'lunge{i + 1}', underwater(rush, 1400))


# ------------------------------------------------------------------ Siphonophore
def pump(d, rate=1.6, wet=1.0):
    """The swimming bells contracting in turn: a soft, wet 'puh' each time water is squeezed out."""
    x = np.zeros(n_(d))
    k = 0
    while k / rate < d - 0.1:
        dd = 0.18; t = t_(dd)
        p = lp(noise(dd), 600) * np.exp(-t * 18) * (1 - np.exp(-t * 200))
        sq = np.sin(2 * np.pi * (180 * t + 400 * t * t)) * np.exp(-t * 25) * 0.4 * wet       # a little squelch
        place(x, (p + sq) * rng.uniform(0.7, 1), k / rate + rng.uniform(0, 0.05))
        k += 1
    return x


def shimmer(d, base=1800, n=5, amount=0.12):
    x = np.zeros(n_(d)); t = t_(d)
    for i in range(n):
        f = base * (1.0 + 0.5 * i) * rng.uniform(0.98, 1.02)
        x += np.sin(2 * np.pi * f * t + rng.uniform(0, 6)) * (0.5 + 0.5 * np.sin(2 * np.pi * rng.uniform(0.3, 1.2) * t + i))
    return x / n * amount


def siphonophore():
    m = 'siphonophore'
    d = 2.4; write(m, 'ambient1', reverb((lp(pump(d, 1.7), 2000) + shimmer(d, 1700)) * curve([(0, 0), (0.1, 1), (0.85, 1), (1, 0)], d), 0.6, 1.6, 0.4, 3000))
    d = 2.0; write(m, 'ambient2', reverb((lp(pump(d, 2.4, 0.6), 1600) * 0.8 + shimmer(d, 2300, 6, 0.16)) * curve([(0, 0), (0.15, 1), (0.85, 1), (1, 0)], d), 0.6, 1.6, 0.45, 3500))
    for i, dd in enumerate([0.4, 0.35]):
        t = t_(dd)
        sq = np.sin(2 * np.pi * (300 * t - 500 * t * t)) * np.exp(-t * 10) * 0.6 + lp(noise(dd), 1500) * np.exp(-t * 12) * 0.7
        write(m, f'hurt{i + 1}', sq + charge_crackle(dd, 0.004) * 0.15)
    d = 1.6; t = t_(d)
    deflate = sweep_bp(noise(d), curve([(0, 900), (1, 150)], d), 0.8) * curve([(0, 1), (1, 0)], d) * 1.5
    write(m, 'death1', reverb(deflate + shimmer(d, 1500, 4, 0.2) * curve([(0, 1), (1, 0)], d) + bubbles(d, 8, 300, 900) * 0.5, 0.6, 1.4, 0.4, 3000))
    for i in range(2):                                                       # the sting: thousands of tiny harpoons firing
        d = 0.5
        clicks = (rng.random(n_(d)) < 0.012 * np.exp(-t_(d) * 6)) * rng.uniform(0.4, 1, n_(d))
        c = hp(signal.lfilter([1], [1, -0.5], clicks), 3500) * 3
        zap = osc(curve([(0, 2400), (1, 900)], 0.12), 0.12, 'saw') * np.linspace(1, 0, n_(0.12)) ** 2 * 0.3
        x = c; place(x, zap, 0.0 + i * 0.01)
        x = x + lp(noise(d), 800) * np.exp(-t_(d) * 14) * 0.3
        write(m, f'sting{i + 1}', softclip(x / (np.max(np.abs(x)) + 1e-9) * 2.5, 1.0))


# ------------------------------------------------------------------ sounds.json and subtitles
EVENTS = {
    'mammoth': {'ambient': 3, 'hurt': 2, 'death': 1, 'step': 3, 'trumpet': 2},
    'meganeura': {'ambient': 2, 'hurt': 2, 'death': 1},
    'karman_wraith': {'ambient': 2, 'hurt': 2, 'death': 1, 'gust': 2},
    'storm_elemental': {'ambient': 2, 'hurt': 2, 'death': 1, 'strike': 2},
    'hadal_anglerfish': {'ambient': 2, 'hurt': 2, 'death': 1, 'lunge': 2},
    'siphonophore': {'ambient': 2, 'hurt': 2, 'death': 1, 'sting': 2},
}
SUBTITLES = {
    'mammoth.trumpet': 'Mammoth trumpets',
    'karman_wraith.gust': 'Kármán Wraith gusts',
    'storm_elemental.strike': 'Storm Elemental strikes',
    'hadal_anglerfish.lunge': 'Anglerfish lunges',
    'siphonophore.sting': 'Siphonophore stings',
}


def json_and_lang():
    p = os.path.join(A, 'sounds.json')
    S = json.load(open(p, encoding='utf-8'))
    for mob, kinds in EVENTS.items():
        for k, n in kinds.items():
            key = f'entity.{mob}.{k}'
            S[key] = {'subtitle': f'subtitles.theoreticalsciences.{key}',
                      'sounds': [f'theoreticalsciences:entity/{mob}/{k}{i + 1}' for i in range(n)]}
    json.dump(S, open(p, 'w', encoding='utf-8'), indent=1, ensure_ascii=False)
    lp_ = os.path.join(A, 'lang', 'en_us.json')
    L = json.load(open(lp_, encoding='utf-8'))
    for k, v in SUBTITLES.items():
        L[f'subtitles.theoreticalsciences.entity.{k}'] = v
    json.dump(L, open(lp_, 'w', encoding='utf-8'), indent=1, ensure_ascii=False)


if __name__ == '__main__':
    for f in (mammoth, meganeura, karman, storm, angler, siphonophore):
        f()
    json_and_lang()
    print('done')
