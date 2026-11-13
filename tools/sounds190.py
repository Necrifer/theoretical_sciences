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
    m = 'cherub'

    def flutter(d, rate=17):
        beat = np.clip(np.sin(2 * np.pi * rate * t_(d)), 0, 1) ** 3
        return bp(noise(d), 500, 2400) * beat * 0.25

    def laugh(notes, gap=0.085, d=None):
        d = d or (len(notes) * gap + 0.5)
        x = np.zeros(n_(d))
        for i, n in enumerate(notes):
            place(x, bell(440 * 2 ** ((n - 69) / 12), 0.45, 9) * 0.6, i * gap + rng.random() * 0.01)
        return x + flutter(d)

    write(m, 'ambient1', laugh([84, 88, 91, 96]))
    write(m, 'ambient2', laugh([86, 91, 89, 93, 98], 0.075))
    write(m, 'hurt1', laugh([98, 93], 0.06, 0.4))
    write(m, 'hurt2', laugh([96, 90], 0.06, 0.4))
    write(m, 'death1', reverb(laugh([96, 93, 89, 86, 81, 77], 0.12, 1.4) * curve([(0, 1), (0.7, 0.7), (1, 0)], 1.4), 0.4, 1.0, 0.3))


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


# ------------------------------------------------------------------ Threshold Warden
def warden():
    m = 'threshold_warden'

    def grind(d, f=140):
        grit = lp(noise(d), 900) * (0.5 + 0.5 * lp(np.abs(noise(d)), 14, 1) * 6)
        return (reso(grit, f, 4) + reso(grit, f * 2.3, 5) * 0.5 + lp(grit, 200) * 0.6) * adsr(d, 0.1, 0.2, 0.9, 0.3)

    def crack(d=0.25):
        return hp(noise(d), 1200) * np.exp(-t_(d) * 30) + lp(noise(d), 400) * np.exp(-t_(d) * 14) * 1.5

    write(m, 'ambient1', grind(1.2))
    write(m, 'ambient2', grind(1.5, 110))
    for i in range(2):
        x = np.zeros(n_(0.45))
        place(x, crack(0.2), 0)
        place(x, crack(0.15) * 0.6, 0.07 + i * 0.03)
        write(m, f'hurt{i + 1}', x + reso(noise(0.45), 300 + i * 60, 8) * np.exp(-t_(0.45) * 12) * 0.4)
    d = 3.2                                                           # wake: the bell, the crack, the stone rising
    x = np.zeros(n_(d))
    place(x, bell(98, 3.0, 1.1) * 1.2 + bell(147, 3.0, 1.4) * 0.6, 0.0)
    place(x, crack(0.3) * 1.2, 0.05)
    place(x, grind(1.6, 90) * curve([(0, 0.2), (0.6, 1), (1, 0)], 1.6), 0.4)
    place(x, osc(curve([(0, 55), (1, 110)], 1.8), 1.8, 'saw') * lp(np.ones(n_(1.8)), 5) * adsr(1.8, 0.8, 0.2, 0.8, 0.5) * 0.35, 0.6)
    write(m, 'wake1', reverb(softclip(x, 1.4), 0.6, 2.0, 0.35, 3000))
    for i in range(2):                                                # strike: the rush of the blade, then it rings
        d = 1.1
        x = np.zeros(n_(d))
        place(x, sweep_bp(noise(0.3), curve([(0, 400), (0.6, 2400), (1, 700)], 0.3), 0.9) * adsr(0.3, 0.08, 0.05, 0.8, 0.1) * 2.0, 0)
        place(x, bell(620 + i * 90, 0.8, 6) * 0.8, 0.24)
        place(x, crack(0.2), 0.24)
        place(x, lp(noise(0.3), 160) * np.exp(-t_(0.3) * 10) * 2.5, 0.24)
        write(m, f'strike{i + 1}', softclip(x, 1.5))
    d = 2.6                                                           # death: it comes apart and falls
    x = grind(d, 80) * curve([(0, 1), (0.5, 0.8), (1, 0)], d)
    for k in range(14):
        place(x, crack(0.2) * (0.4 + rng.random() * 0.8), 0.1 + rng.random() * 1.8)
    place(x, lp(noise(0.8), 120) * np.exp(-t_(0.8) * 5) * 3, 1.2)
    write(m, 'death1', reverb(softclip(x, 1.3), 0.5, 1.5, 0.3, 2500))


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
    'cherub.ambient': 'Cherub laughs', 'cherub.hurt': 'Cherub yelps', 'cherub.death': 'Cherub fades',
    'choir_seraph.ambient': 'Choir Seraph hums', 'choir_seraph.hurt': 'Choir Seraph cries out', 'choir_seraph.death': 'Choir Seraph falls silent',
    'choir_seraph.sing': 'Choir Seraph sings',
    'cloud_ray.ambient': 'Cloud Ray calls', 'cloud_ray.hurt': 'Cloud Ray groans', 'cloud_ray.death': 'Cloud Ray sinks',
    'dweller.ambient': 'Dweller hums', 'dweller.hurt': 'Dweller gasps', 'dweller.death': 'Dweller sighs',
    'threshold_warden.ambient': 'Stone grinds', 'threshold_warden.hurt': 'Stone chips', 'threshold_warden.death': 'Threshold Warden falls',
    'threshold_warden.wake': 'Threshold Warden wakes', 'threshold_warden.strike': 'Threshold Warden strikes',
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
