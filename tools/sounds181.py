"""
1.8.1: the Choir Crystal Spire's own sounds, and the Homeward Portal's opening. The crystal is clear: pure, high,
ringing partials with long tails and no noise in them at all (the old crystal sound was glassy and dull, and its
break was a crunch). Breaking it is a chord let go, not a shatter.

Run:  python3 tools/sounds181.py
"""
import json, math, os, subprocess, tempfile
import numpy as np
from scipy.io import wavfile

SR = 44100
ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'src/main/resources/assets/theoreticalsciences')
rng = np.random.RandomState(181)


def ring(f, d, decay, amp=1.0, shimmer=0.0):
    """One struck crystal: a pure tone, its octave and a soft twelfth, slightly sharp as glass harmonics are."""
    t = np.arange(int(d * SR)) / SR
    x = (np.sin(2 * np.pi * f * t) + 0.38 * np.sin(2 * np.pi * f * 2.004 * t) * np.exp(-t * decay * 0.8)
         + 0.16 * np.sin(2 * np.pi * f * 3.011 * t) * np.exp(-t * decay * 1.6))
    if shimmer:
        x *= 1 + shimmer * np.sin(2 * np.pi * 6.5 * t)
    return amp * x * np.exp(-t * decay) * np.minimum(1, t / 0.0015)


def put(mix, x, at):
    s = int(at * SR)
    n = min(len(x), len(mix) - s)
    if n > 0:
        mix[s:s + n] += x[:n]


def save(path, mix, peak=0.85):
    mix = mix / (np.max(np.abs(mix)) + 1e-9) * peak
    fade = int(0.02 * SR)
    mix[-fade:] *= np.linspace(1, 0, fade)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with tempfile.NamedTemporaryFile(suffix='.wav', delete=False) as tf:
        wavfile.write(tf.name, SR, (mix * 32767).astype(np.int16))
    subprocess.run(['ffmpeg', '-v', 'error', '-y', '-i', tf.name, '-ac', '1', '-c:a', 'libvorbis', '-q:a', '5', path], check=True)
    os.unlink(tf.name)


def hz(n):
    return 440.0 * 2 ** ((n - 69) / 12.0)


D = os.path.join(ROOT, 'sounds/entity/choir_crystal')
# break: a chord let go, top note first, each note clean
for i, chord in enumerate([[93, 88, 86, 81], [95, 90, 86, 83], [98, 93, 90, 86]]):
    mix = np.zeros(int(1.5 * SR))
    for k, n in enumerate(chord):
        put(mix, ring(hz(n), 1.4, 3.2 + k * 0.3, 1.0 - k * 0.12, 0.08), k * 0.028 + rng.rand() * 0.006)
    put(mix, ring(hz(chord[0] + 12), 0.5, 9, 0.35), 0.0)
    save(os.path.join(D, 'break%d.ogg' % (i + 1)), mix)
# place: two clear notes, a fifth apart
for i, n in enumerate([86, 88]):
    mix = np.zeros(int(0.8 * SR))
    put(mix, ring(hz(n), 0.7, 6.5), 0)
    put(mix, ring(hz(n + 7), 0.6, 7.5, 0.6), 0.035)
    save(os.path.join(D, 'place%d.ogg' % (i + 1)), mix, 0.7)
# hit: one bright ting
for i, n in enumerate([93, 95, 98]):
    mix = np.zeros(int(0.45 * SR))
    put(mix, ring(hz(n), 0.42, 11), 0)
    save(os.path.join(D, 'hit%d.ogg' % (i + 1)), mix, 0.6)
# step: a soft clear note, different each time
for i, n in enumerate([81, 83, 86, 88]):
    mix = np.zeros(int(0.5 * SR))
    put(mix, ring(hz(n), 0.48, 9, 1.0), 0)
    put(mix, ring(hz(n + 12), 0.3, 14, 0.25), 0.004)
    save(os.path.join(D, 'step%d.ogg' % (i + 1)), mix, 0.45)
# fall: two notes close together
for i, n in enumerate([81, 86]):
    mix = np.zeros(int(0.7 * SR))
    put(mix, ring(hz(n), 0.6, 7), 0)
    put(mix, ring(hz(n + 4), 0.6, 7, 0.8), 0.05)
    save(os.path.join(D, 'fall%d.ogg' % (i + 1)), mix, 0.7)

# the portal's opening, 7 seconds: twelve notes rising as the beams go up, a held chord as they meet, the fall, the bloom
mix = np.zeros(int(8.5 * SR))
scale = [62, 66, 69, 74, 78, 81, 86, 90, 93, 98, 102, 105]
for k, n in enumerate(scale):
    put(mix, ring(hz(n), 3.0, 1.4, 0.5, 0.05), 0.12 * k)
t = np.arange(int(2.6 * SR)) / SR
swell = sum(np.sin(2 * np.pi * hz(n) * t + rng.rand() * 6) * a for n, a in [(50, 1.0), (57, 0.8), (62, 0.8), (66, 0.6), (69, 0.6), (74, 0.4)])
swell *= np.minimum(1, t / 1.6) ** 2 * np.minimum(1, (2.6 - t) / 0.25)
put(mix, swell * 0.22, 2.0)
t = np.arange(int(1.0 * SR)) / SR                       # the fall: a tone dropping two octaves
fall = np.sin(2 * np.pi * np.cumsum(hz(86) * 2 ** (-2.0 * (t / 1.0) ** 0.7)) / SR) * np.minimum(1, t / 0.05) * np.minimum(1, (1.0 - t) / 0.1)
put(mix, fall * 0.5, 3.6)
t = np.arange(int(3.6 * SR)) / SR                       # the strike and the bloom
boom = np.sin(2 * np.pi * 55 * t) * np.exp(-t * 2.2) + 0.6 * np.sin(2 * np.pi * 82.5 * t) * np.exp(-t * 2.6)
put(mix, boom * 0.9, 4.6)
for k, n in enumerate([74, 78, 81, 86, 90, 93, 98]):
    put(mix, ring(hz(n), 3.4, 1.0, 0.45, 0.1), 4.6 + 0.03 * k)
for delay, g in ((0.31, 0.3), (0.67, 0.18)):
    d = int(delay * SR)
    mix[d:] += mix[:-d] * g
save(os.path.join(ROOT, 'sounds/entity/homeward_portal/open.ogg'), np.tanh(mix * 0.9), 0.9)

p = os.path.join(ROOT, 'sounds.json')
raw = open(p).read()
S = json.loads(raw)
for kind, n in (('break', 3), ('place', 2), ('hit', 3), ('step', 4), ('fall', 2)):
    S['entity.choir_crystal.' + kind] = {'subtitle': 'subtitles.theoreticalsciences.entity.choir_crystal.' + kind,
                                         'sounds': ['theoreticalsciences:entity/choir_crystal/%s%d' % (kind, i + 1) for i in range(n)]}
S['entity.homeward_portal.open'] = {'subtitle': 'subtitles.theoreticalsciences.entity.homeward_portal.open', 'sounds': ['theoreticalsciences:entity/homeward_portal/open']}
json.dump(S, open(p, 'w', encoding='utf-8'), indent=1 if raw.startswith('{\n "') else 2, ensure_ascii=False)
print('sounds written')
