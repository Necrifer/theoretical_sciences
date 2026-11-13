"""
1.9.0 creature sounds: those who live in Omnipotency (everything synthesised here, with sounds13.py's building blocks).

  * Halo Lamb: a small bleat, a reedy voice with the quick tremble a lamb's has, soft and high.
  * Cherub: laughter made of little bells: a few quick rising notes over the flutter of small wings.
  * Choir Seraph: a sung vowel, two voices a fifth apart with slow vibrato; when it sings, a phrase of four notes.
  * Cloud Ray: long low calls that slide, as whales' do, heard through a great deal of air.
  * Dweller: a contented hum of two notes; an "oh" when struck; a long breath out.
  * Threshold Warden: stone. Grinding when it moves, a deep bell and a crack when it wakes, the rush and ring of the
    sword, chips when it is struck, and a fall of rubble.

Run:  python3 tools/sounds190.py
"""
import json, os
import numpy as np
import sounds13 as S
from sounds13 import n_, t_, curve, adsr, osc, noise, lp, hp, bp, reso, sweep_bp, place, reverb, softclip, write, SR

S.rng = np.random.default_rng(1900)
rng = S.rng


def vowel(f0, d, formants, breath=0.06, vib=0.012, rate=5.2):
    f = (np.full(n_(d), float(f0)) if np.isscalar(f0) else np.asarray(f0, float)[:n_(d)]) * (1 + vib * np.sin(2 * np.pi * rate * t_(d) + rng.random() * 6))
    src = osc(f, d, 'pulse', jitter=0.002)
    y = sum(reso(src, fr, q) * a for fr, q, a in formants)
    return y + bp(noise(d), 1500, 6000) * breath


AH = [(800, 6, 1.0), (1150, 7, 0.7), (2900, 9, 0.35)]
OO = [(350, 6, 1.0), (750, 7, 0.5), (2500, 9, 0.15)]
MM = [(280, 5, 1.0), (1000, 6, 0.15)]
OH = [(500, 6, 1.0), (900, 7, 0.6), (2600, 9, 0.2)]


def bell(f, d, decay=4.0):
    t = t_(d)
    return sum(a * np.sin(2 * np.pi * f * r * t) * np.exp(-t * decay * k) for r, a, k in [(1, 1, 1), (2.0, 0.4, 1.4), (3.01, 0.2, 2.2), (4.2, 0.1, 3)])


# ------------------------------------------------------------------ Halo Lamb
def lamb():
    m = 'halo_lamb'

    def bleat(d, f0, f1, tremble=0.35):
        f = curve([(0, f0 * 0.92), (0.15, f0), (0.7, f1), (1, f1 * 0.9)], d)
        x = vowel(f, d, [(900, 5, 1.0), (1700, 6, 0.8), (3100, 8, 0.4)], breath=0.04, vib=0.0)
        x *= 1 - tremble * (0.5 + 0.5 * np.sin(2 * np.pi * 11 * t_(d)))                      # the tremble in it
        return x * adsr(d, 0.03, 0.1, 0.85, 0.2)

    write(m, 'ambient1', bleat(0.55, 560, 520))
    write(m, 'ambient2', bleat(0.7, 600, 500, 0.45))
    write(m, 'hurt1', bleat(0.35, 760, 700, 0.5))
    write(m, 'hurt2', bleat(0.4, 820, 640, 0.5))
    write(m, 'death1', bleat(1.0, 620, 330, 0.3) * curve([(0, 1), (0.6, 0.8), (1, 0)], 1.0))


# ------------------------------------------------------------------ Cherub
def cherub():
    """Four faces, four voices at once (a man's hum, a lion's low growl, an ox's low, an eagle's cry), over wings that sound like many waters."""
    m = 'cherub'

    def waters(d):
        beat = 0.6 + 0.4 * np.sin(2 * np.pi * 2.6 * t_(d))
        return (bp(noise(d, 'pink'), 150, 1800) * beat + lp(noise(d), 220) * 0.6) * adsr(d, 0.3, 0.2, 0.9, 0.5)

    def four(d, f, cry=True, bend=1.0):
        fall = curve([(0, 1), (0.6, 1), (1, bend)], d)
        man = vowel(f * fall, d, OO, vib=0.008) * 0.8
        lion = lp(osc(f * 0.5 * fall, d, 'saw', jitter=0.01) * (1 + 0.5 * np.sin(2 * np.pi * 27 * t_(d))), 500) * 0.7
        ox = vowel(f * 0.75 * fall, d, MM, vib=0.004) * 0.9
        x = (man + lion + ox) * adsr(d, 0.15, 0.2, 0.9, 0.4)
        if cry:
            c = 0.5
            place(x, vowel(curve([(0, f * 6), (0.3, f * 7), (1, f * 4.5)], c), c, [(2600, 6, 1.0), (3800, 8, 0.6)], breath=0.1, vib=0) * adsr(c, 0.02, 0.1, 0.7, 0.25) * 0.5, d * 0.35)
        return x

    def call(d, f, cry=True, bend=1.0, wet=0.3):
        return reverb(four(d, f, cry, bend) + waters(d) * 0.35, 0.5, 1.6, wet, 5000)

    write(m, 'ambient1', call(1.8, 147))
    write(m, 'ambient2', call(2.1, 165, cry=False))
    write(m, 'hurt1', call(0.7, 220, wet=0.2))
    write(m, 'hurt2', call(0.8, 196, wet=0.2))
    write(m, 'death1', call(2.8, 165, cry=False, bend=0.5, wet=0.45) * curve([(0, 1), (0.6, 0.8), (1, 0)], n_(2.8) / SR + 1.0)[:n_(2.8) + n_(1.6 * 0.6)])


# ------------------------------------------------------------------ Choir Seraph
def seraph():
    m = 'choir_seraph'

    def voice(notes, each, form=AH, wet=0.4):
        d = len(notes) * each + 0.6
        x = np.zeros(n_(d))
        for i, n in enumerate(notes):
            f = 440 * 2 ** ((n - 69) / 12)
            seg = (vowel(f, each + 0.35, form) + 0.6 * vowel(f * 1.5, each + 0.35, form, vib=0.009, rate=4.6)) * adsr(each + 0.35, 0.18, 0.2, 0.9, 0.35)
            place(x, seg, i * each)
        return reverb(x, 0.5, 1.8, wet, 6000)

    write(m, 'ambient1', voice([69], 1.4, OO, 0.35) * 0.7)
    write(m, 'ambient2', voice([72], 1.2, OO, 0.35) * 0.7)
    write(m, 'sing1', voice([69, 74, 76, 81], 0.9))
    write(m, 'sing2', voice([74, 78, 76, 81, 86], 0.8))
    d = 0.6
    write(m, 'hurt1', reverb((vowel(curve([(0, 880), (1, 700)], d), d, AH) + vowel(curve([(0, 932), (1, 740)], d), d, AH)) * adsr(d, 0.01, 0.1, 0.7, 0.3), 0.4, 1.0, 0.3))
    write(m, 'hurt2', reverb((vowel(curve([(0, 784), (1, 660)], d), d, AH) + vowel(curve([(0, 830), (1, 700)], d), d, AH)) * adsr(d, 0.01, 0.1, 0.7, 0.3), 0.4, 1.0, 0.3))
    d = 2.4
    fall = curve([(0, 1), (0.3, 0.94), (1, 0.5)], d)
    x = sum(vowel(f * fall, d, OO, vib=0.02) for f in (440, 554, 659)) * curve([(0, 1), (0.5, 0.8), (1, 0)], d)
    write(m, 'death1', reverb(x, 0.6, 2.0, 0.45))


# ------------------------------------------------------------------ Cloud Ray
def ray():
    m = 'cloud_ray'

    def call(d, pts, wet=0.5):
        f = curve(pts, d)
        x = osc(f, d, 'sine') + 0.35 * osc(f * 2, d, 'sine') + 0.15 * osc(f * 3, d, 'sine')
        x = x * (1 + 0.15 * np.sin(2 * np.pi * 3.1 * t_(d))) * adsr(d, 0.3, 0.3, 0.9, 0.6)
        return reverb(x + lp(noise(d), 300) * 0.05, 0.7, 2.4, wet, 2500)

    write(m, 'ambient1', call(2.6, [(0, 110), (0.4, 170), (0.7, 150), (1, 95)]))
    write(m, 'ambient2', call(3.0, [(0, 140), (0.3, 120), (0.6, 200), (1, 130)]))
    write(m, 'hurt1', call(1.0, [(0, 220), (0.3, 260), (1, 120)], 0.35))
    write(m, 'hurt2', call(1.1, [(0, 200), (0.2, 250), (1, 110)], 0.35))
    write(m, 'death1', call(3.4, [(0, 160), (0.3, 130), (1, 45)]))


# ------------------------------------------------------------------ Dweller
def dweller():
    m = 'dweller'

    def hum(notes, each=0.32):
        d = len(notes) * each + 0.25
        x = np.zeros(n_(d))
        for i, n in enumerate(notes):
            place(x, vowel(440 * 2 ** ((n - 69) / 12), each + 0.15, MM, breath=0.02, vib=0.006) * adsr(each + 0.15, 0.05, 0.1, 0.85, 0.15), i * each)
        return lp(x, 2500)

    write(m, 'ambient1', hum([57, 60]))
    write(m, 'ambient2', hum([60, 57, 62]))
    d = 0.4
    write(m, 'hurt1', vowel(curve([(0, 300), (0.2, 340), (1, 240)], d), d, OH, breath=0.12) * adsr(d, 0.01, 0.08, 0.7, 0.2))
    write(m, 'hurt2', vowel(curve([(0, 330), (0.2, 370), (1, 250)], d), d, OH, breath=0.12) * adsr(d, 0.01, 0.08, 0.7, 0.2))
    d = 1.6
    sigh = bp(noise(d), 600, 3000) * curve([(0, 0), (0.15, 1), (1, 0)], d) * 0.5 + vowel(curve([(0, 260), (1, 150)], d), d, OH) * curve([(0, 0.8), (0.5, 0.3), (1, 0)], d)
    write(m, 'death1', reverb(sigh, 0.4, 1.2, 0.3))


# ------------------------------------------------------------------ Threshold Ophan
def warden():
    """The whirling wheels: great rims turning (a slow beating hum, metal on air), a chord of bells when every eye opens, a rush and a ring when it strikes."""
    m = 'threshold_warden'

    def wheels(d, f=82, rate=1.6):
        beat = 0.55 + 0.45 * np.sin(2 * np.pi * rate * t_(d))
        hum = (osc(f, d, 'saw') * 0.5 + osc(f * 1.498, d, 'sine') * 0.5 + osc(f * 2.01, d, 'sine') * 0.3) * beat
        rush = sweep_bp(noise(d), 500 + 400 * np.sin(2 * np.pi * rate * t_(d)), 0.8) * 0.35
        return (lp(hum, 900) + rush) * adsr(d, 0.25, 0.2, 0.9, 0.4)

    def ting(d=0.3, f=1400):
        return bell(f, d, 9) + bell(f * 1.5, d, 12) * 0.5

    write(m, 'ambient1', reverb(wheels(2.0), 0.5, 1.5, 0.3, 4000))
    write(m, 'ambient2', reverb(wheels(2.4, 73, 1.3), 0.5, 1.5, 0.3, 4000))
    for i in range(2):
        x = np.zeros(n_(0.5))
        place(x, ting(0.3, 1250 + i * 240), 0)
        place(x, ting(0.25, 1870 + i * 180) * 0.6, 0.05)
        write(m, f'hurt{i + 1}', x + lp(noise(0.5), 300) * np.exp(-t_(0.5) * 14) * 0.6)
    d = 3.4                                                           # wake: the eyes open one after another, and the wheels begin
    x = np.zeros(n_(d))
    for k_, n in enumerate([50, 57, 62, 66, 69, 74, 78, 81, 86, 90]):
        place(x, bell(440 * 2 ** ((n - 69) / 12), 2.4, 1.6) * 0.5, 0.07 * k_)
    place(x, wheels(2.6, 60, 1.0) * curve([(0, 0), (0.7, 1), (1, 0.6)], 2.6) * 1.2, 0.6)
    place(x, sweep_bp(noise(1.6), curve([(0, 200), (1, 3000)], 1.6), 0.7) * adsr(1.6, 1.0, 0.1, 0.8, 0.3) * 0.5, 0.8)
    write(m, 'wake1', reverb(softclip(x, 1.4), 0.6, 2.0, 0.35, 5000))
    for i in range(2):                                                # strike: fire going out from between the wheels
        d = 1.1
        x = np.zeros(n_(d))
        place(x, sweep_bp(noise(0.3), curve([(0, 300), (0.6, 2600), (1, 800)], 0.3), 0.9) * adsr(0.3, 0.08, 0.05, 0.8, 0.1) * 2.0, 0)
        place(x, bell(520 + i * 80, 0.8, 5) * 0.8, 0.22)
        place(x, hp(noise(0.25), 2000) * np.exp(-t_(0.25) * 20) * 1.2, 0.22)
        place(x, lp(noise(0.3), 160) * np.exp(-t_(0.3) * 10) * 2.5, 0.22)
        write(m, f'strike{i + 1}', softclip(x, 1.5))
    d = 3.0                                                           # death: the wheels run down and ring as they fall
    f = curve([(0, 82), (1, 30)], d)
    x = lp(osc(f, d, 'saw') * (0.55 + 0.45 * np.sin(2 * np.pi * np.cumsum(curve([(0, 1.6), (1, 0.3)], d)) / SR)), 700) * curve([(0, 1), (0.7, 0.7), (1, 0)], d)
    for k_ in range(6):
        place(x, ting(0.5, 900 - k_ * 110) * (0.9 - k_ * 0.1), 0.8 + k_ * 0.3)
    write(m, 'death1', reverb(softclip(x, 1.3), 0.6, 1.8, 0.35, 4000))


EVENTS = {
    'halo_lamb': {'ambient': 2, 'hurt': 2, 'death': 1},
    'cherub': {'ambient': 2, 'hurt': 2, 'death': 1},
    'choir_seraph': {'ambient': 2, 'hurt': 2, 'death': 1, 'sing': 2},
    'cloud_ray': {'ambient': 2, 'hurt': 2, 'death': 1},
    'dweller': {'ambient': 2, 'hurt': 2, 'death': 1},
    'threshold_warden': {'ambient': 2, 'hurt': 2, 'death': 1, 'wake': 1, 'strike': 2},
}
SUBTITLES = {
    'halo_lamb.ambient': 'Halo Lamb bleats', 'halo_lamb.hurt': 'Halo Lamb cries', 'halo_lamb.death': 'Halo Lamb falls quiet',
    'cherub.ambient': 'Cherub calls with four voices', 'cherub.hurt': 'Cherub cries out', 'cherub.death': 'Cherub falls silent',
    'choir_seraph.ambient': 'Choir Seraph hums', 'choir_seraph.hurt': 'Choir Seraph cries out', 'choir_seraph.death': 'Choir Seraph falls silent',
    'choir_seraph.sing': 'Choir Seraph sings',
    'cloud_ray.ambient': 'Cloud Ray calls', 'cloud_ray.hurt': 'Cloud Ray groans', 'cloud_ray.death': 'Cloud Ray sinks',
    'dweller.ambient': 'Dweller hums', 'dweller.hurt': 'Dweller gasps', 'dweller.death': 'Dweller sighs',
    'threshold_warden.ambient': 'Wheels turn', 'threshold_warden.hurt': 'A wheel rings', 'threshold_warden.death': 'Threshold Ophan runs down',
    'threshold_warden.wake': 'Every eye opens', 'threshold_warden.strike': 'Threshold Ophan strikes',
}

if __name__ == '__main__':
    for f in (lamb, cherub, seraph, ray, dweller, warden):
        f()
    p = os.path.join(S.A, 'sounds.json')
    raw = open(p, encoding='utf-8').read()
    J = json.loads(raw)
    for mob, kinds in EVENTS.items():
        for k, n in kinds.items():
            key = f'entity.{mob}.{k}'
            J[key] = {'subtitle': f'subtitles.theoreticalsciences.{key}', 'sounds': [f'theoreticalsciences:entity/{mob}/{k}{i + 1}' for i in range(n)]}
    J['music_disc.homecoming'] = {'sounds': [{'name': 'theoreticalsciences:music/omnipotency', 'stream': True}]}
    json.dump(J, open(p, 'w', encoding='utf-8'), indent=1 if raw.startswith('{\n "') else 2, ensure_ascii=False)
    lp_ = os.path.join(S.A, 'lang', 'en_us.json')
    raw = open(lp_, encoding='utf-8').read()
    L = json.loads(raw)
    for k, v in SUBTITLES.items():
        L[f'subtitles.theoreticalsciences.entity.{k}'] = v
    json.dump(L, open(lp_, 'w', encoding='utf-8'), indent=1 if raw.startswith('{\n "') else 2, ensure_ascii=False)
    print('done')
