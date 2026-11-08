"""
1.4.0 creature sounds, synthesised (no samples), each following what the creature is:
  * Dreaming Colony: a mind murmuring in its sleep. A soft, breathy choir (noise through vowel formants over slow sine
    chords with a drifting vibrato), and under it the popcorn crackle of neurons firing. Hurt, the chord goes dissonant
    and the crackle bursts; dying, the voices slide down and the firing thins out to nothing. Its pulse is a deep throb
    with a shimmer rising out of it.
  * Rust Mite: small and metallic. Dry chitters of its legs on steel and a creak of flaking shell; hurt, a high squeak
    and a clatter of flakes; dying, a crumble; feeding, a rasping file on metal in short strokes.
"""
import os, sys
import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import sounds13 as S
from sounds13 import n_, t_, curve, adsr, osc, noise, lp, hp, bp, reso, place, reverb, softclip, write, SR

rng = np.random.default_rng(1401)


def formant(x, f1, f2, q=6):
    return reso(x, f1, q) + 0.6 * reso(x, f2, q)


def choir(d, root, chord=(1, 1.25, 1.5), vowel=(700, 1150), breath=0.5, vib=0.006):
    t = t_(d)
    v = np.zeros(n_(d))
    for k, r in enumerate(chord):
        f = root * r * (1 + vib * np.sin(2 * np.pi * (4.2 + k * 0.7) * t + k))
        v += osc(f, d, 'sine') * (0.5 / (k + 1) + 0.25)
    br = formant(noise(d, 'pink'), *vowel) * breath
    return v * 0.5 + br


def spikes(d, rate, decay=0.0):
    """Neural firing: sparse clicks (each a short damped tick), thinning out over time if decay > 0."""
    n = n_(d)
    t = np.arange(n) / SR
    p = rate / SR * np.exp(-decay * t)
    clicks = (rng.random(n) < p) * rng.uniform(0.3, 1, n)
    k = np.exp(-np.arange(int(SR * 0.004)) / (SR * 0.0008)) * np.sin(np.arange(int(SR * 0.004)) * 2 * np.pi * 3200 / SR)
    return np.convolve(clicks, k)[:n]


def colony():
    m = 'dreaming_colony'
    for i, (root, vowel) in enumerate(((196, (700, 1150)), (174.6, (400, 1700)), (220, (600, 1000)))):
        d = 3.2
        x = choir(d, root, vowel=vowel) * curve([(0, 0), (0.25, 1), (0.75, 0.9), (1, 0)], d)
        x = x + spikes(d, 22) * 0.25
        write(m, f'ambient{i + 1}', reverb(x, 0.7, 2.2, 0.45, 3500))
    for i in range(2):
        d = 0.7
        x = choir(d, 233 + i * 20, chord=(1, 1.06, 1.41), vowel=(800, 1250), breath=0.8, vib=0.03) * adsr(d, 0.01, 0.15, 0.4, 0.3)
        x = x + spikes(d, 400, 4) * 0.7
        write(m, f'hurt{i + 1}', reverb(softclip(x * 1.6, 1.2), 0.5, 1.2, 0.35, 4000))
    d = 3.0
    f = curve([(0, 220), (1, 70)], d)
    t = t_(d)
    x = sum(S.osc(f * r, d, 'sine') * (0.5 / (k + 1)) for k, r in enumerate((1, 1.2, 1.5)))
    x = x + formant(noise(d, 'pink'), 500, 900) * 0.4 * curve([(0, 1), (1, 0)], d)
    x = x * curve([(0, 1), (0.7, 0.6), (1, 0)], d) + spikes(d, 120, 1.6) * 0.5
    write(m, 'death1', reverb(x, 0.8, 2.6, 0.5, 3000))
    for i in range(2):                                                   # the pulse: a throb, a shimmer rising out of it
        d = 1.6
        throb = osc(curve([(0, 55), (1, 48)], d), d, 'sine') * np.exp(-t_(d) * 2.5) * 1.2
        shim = sum(osc(curve([(0, 600 * r), (1, 1400 * r)], d), d, 'sine') for r in (1, 1.25, 1.5)) * curve([(0, 0), (0.3, 0.4), (1, 0)], d) * 0.25
        x = throb + shim + spikes(d, 60) * 0.3
        write(m, f'pulse{i + 1}', reverb(x, 0.6, 1.8, 0.4, 5000))


def mite():
    m = 'rust_mite'
    for i in range(2):                                                   # chittering legs on steel, a creak of shell
        d = 1.2
        x = np.zeros(n_(d))
        for k in range(14 + i * 4):
            c = hp(noise(0.012), 3000) * np.exp(-t_(0.012) * 300)
            place(x, c * rng.uniform(0.3, 0.9), rng.uniform(0, d - 0.02))
        creak = bp(osc(curve([(0, 340), (0.5, 410), (1, 360)], 0.35), 0.35, 'saw', 0.02), 300, 1200) * adsr(0.35, 0.05, 0.1, 0.6, 0.1) * 0.25
        place(x, creak, 0.3 + i * 0.3)
        write(m, f'ambient{i + 1}', reverb(x, 0.25, 0.5, 0.15, 6000))
    for i in range(2):
        d = 0.45
        sq = osc(curve([(0, 1900 + i * 200), (0.4, 2600), (1, 1500)], d), d, 'saw') * adsr(d, 0.005, 0.08, 0.3, 0.15) * 0.3
        sq = bp(sq, 1200, 5000)
        fl = np.zeros(n_(d))
        for k in range(10):
            place(fl, hp(noise(0.02), 2000) * np.exp(-t_(0.02) * 160) * rng.uniform(0.2, 0.6), rng.uniform(0.05, 0.4))
        write(m, f'hurt{i + 1}', reverb(sq + fl, 0.2, 0.4, 0.12, 6000))
    d = 1.0
    x = np.zeros(n_(d))
    for k in range(40):
        place(x, lp(noise(0.03), 2500) * np.exp(-t_(0.03) * 90) * rng.uniform(0.2, 1) * (1 - k / 45), k / 40 * 0.8)
    place(x, hp(noise(0.01), 3000) * 0.8, 0.9)
    write(m, 'death1', reverb(x, 0.3, 0.6, 0.2, 5000))
    for i in range(2):                                                   # feeding: a file on metal, short strokes
        d = 0.8
        x = np.zeros(n_(d))
        for k in range(4):
            st = 0.14
            stroke = bp(noise(st) * (1 + 0.6 * np.sign(np.sin(2 * np.pi * 180 * t_(st)))), 1800, 7000) * adsr(st, 0.01, 0.05, 0.7, 0.04)
            place(x, stroke * (0.7 + 0.3 * rng.random()), 0.02 + k * 0.19 + i * 0.01)
        write(m, f'chew{i + 1}', reverb(x, 0.2, 0.4, 0.12, 7000))


S.EVENTS = {'dreaming_colony': {'ambient': 3, 'hurt': 2, 'death': 1, 'pulse': 2}, 'rust_mite': {'ambient': 2, 'hurt': 2, 'death': 1, 'chew': 2}}
S.SUBTITLES = {'dreaming_colony.ambient': 'Dreaming Colony murmurs', 'dreaming_colony.hurt': 'Dreaming Colony cries out',
               'dreaming_colony.death': 'Dreaming Colony fades', 'dreaming_colony.pulse': 'Dreaming Colony pulses',
               'rust_mite.ambient': 'Rust Mite chitters', 'rust_mite.hurt': 'Rust Mite squeaks', 'rust_mite.death': 'Rust Mite crumbles',
               'rust_mite.chew': 'Rust Mite gnaws metal'}

if __name__ == '__main__':
    colony()
    mite()
    S.json_and_lang()
    print('done')
