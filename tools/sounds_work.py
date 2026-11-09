"""
1.5.1 working sounds for multiblocks. A formed, running structure plays a seamless loop from its middle; the Radio
Telescope has always counted running machines nearby as interference, and now the player hears that noise too.

Fourteen voices, one per kind of machine (they match the body materials of tools/machine_art.py), each a 4 second loop
built only from whole cycles so it repeats without a click:
  quantum     detuned glass tones and soft pings (qubits being read)
  cosmic      a very deep choir drone over a sub rumble
  instrument  clockwork ticks and a small servo whir
  heavy       transformer hum with a slow pump thump
  chem        bubbling over a quiet circulation pump
  math        relay clicks over a cooling fan
  bio         a peristaltic pump's swish and a soft pulse
  earth       a drill's grinding rumble
  space       a low engine roar and hissing gas
  cryo        a compressor's pulse and venting cold gas
  turbine     a turbine's whine over its rumble (generator structures)
  plasma      crackling plasma in a magnetic hum (rings, tokamaks, forges)
  void        a howling wind over a sub tone, with glints (horizons, wormholes, the vacuum)
  data        a computing hum full of data chirps (simulators, compilers, brains)

Also writes src/main/java/.../client/MachineVoices.java, which tells each machine which voice and pitch it has.
Run:  python3 tools/sounds_work.py
"""
import json, os, subprocess, sys, tempfile
import numpy as np
from scipy.io import wavfile

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import sounds13 as S
from sounds13 import lp, hp, bp, reso, softclip, SR, A
import machine_art as MA

D = 4.0
NS = int(D * SR)
T = np.arange(NS) / SR
rng = np.random.default_rng(1510)
OUT = os.path.join(A, 'sounds', 'machine')


def loopnoise(color='white', f=None):
    """Noise that wraps: extra tail crossfaded onto the head after filtering."""
    xf = int(0.25 * SR)
    x = rng.normal(0, 1, NS + xf)
    if color == 'pink':
        X = np.fft.rfft(x); k = np.arange(len(X)); k[0] = 1; x = np.fft.irfft(X / np.sqrt(k), len(x))
    elif color == 'brown':
        x = np.cumsum(x); x -= np.linspace(x[0], x[-1], len(x))
    if f:
        x = f(x)
    x = x / (np.std(x) + 1e-9)
    w = np.linspace(0, 1, xf)
    head = x[:xf] * w + x[NS:NS + xf] * (1 - w)
    return np.concatenate([head, x[xf:NS]])


def sine(f, ph=0.0):
    f = round(f * D) / D                                       # whole cycles in the loop
    return np.sin(2 * np.pi * f * T + ph)


def lfo(rate, lo=0.0, hi=1.0, ph=0.0):
    return lo + (hi - lo) * (0.5 + 0.5 * sine(rate, ph))


def events(times, kernel):
    """Places a short sound at each time, wrapping round the loop."""
    out = np.zeros(NS)
    for t in times:
        i = int(t * SR) % NS
        n = len(kernel)
        end = i + n
        if end <= NS:
            out[i:end] += kernel
        else:
            out[i:] += kernel[:NS - i]; out[:end - NS] += kernel[NS - i:]
    return out


def tick(f=3000, dec=0.002, d=0.02):
    n = int(d * SR); t = np.arange(n) / SR
    return np.sin(2 * np.pi * f * t) * np.exp(-t / dec)


def thump(f=55, d=0.35):
    n = int(d * SR); t = np.arange(n) / SR
    return np.sin(2 * np.pi * (f + 40 * np.exp(-t / 0.03)) * t) * np.exp(-t / 0.09)


def bubble(f, d=0.08):
    n = int(d * SR); t = np.arange(n) / SR
    return np.sin(2 * np.pi * f * (1 + 2.5 * t / d) * t) * np.exp(-t / (d / 3)) * np.minimum(1, t / 0.003)


def wrapreverb(x, decay=0.6, mix=0.25):
    """A short circular reverb so the tail of the loop rings into its head."""
    ir_n = int(decay * SR)
    ir = rng.normal(0, 1, ir_n) * np.exp(-np.arange(ir_n) / (decay * SR / 5))
    X = np.fft.rfft(x); I = np.fft.rfft(np.pad(ir, (0, NS - ir_n)))
    wet = np.fft.irfft(X * I, NS)
    wet = lp(wet, 5000)
    return x + mix * wet / (np.max(np.abs(wet)) + 1e-9) * np.max(np.abs(x))


# ---------------------------------------------------------------------------------------- voices
def quantum():
    x = sum(sine(f) * a for f, a in ((220, 0.3), (221.5, 0.3), (330, 0.18), (331.25, 0.18), (660, 0.06)))
    x *= lfo(0.5, 0.6, 1.0)
    pings = events(np.arange(0, D, 0.5) + rng.uniform(0, 0.05, 8), tick(1760, 0.08, 0.4) * 0.5 + tick(2640, 0.05, 0.4) * 0.3)
    return wrapreverb(x * 0.6 + pings * 0.4 + loopnoise('pink', lambda n: bp(n, 4000, 9000)) * 0.02, 1.2, 0.35)


def cosmic():
    x = sum(sine(f * r) * (0.5 / (k + 1)) * lfo(0.25 * (k + 1), 0.6, 1.0, k) for k, (f, r) in
            enumerate(((55, 1), (55, 1.5), (55, 2), (55, 3), (55, 4.0))))
    x += loopnoise('pink', lambda n: S.formant(n, 500, 900) if hasattr(S, 'formant') else bp(n, 400, 1000)) * 0.08
    x += loopnoise('brown', lambda n: lp(n, 60)) * 0.4
    return wrapreverb(x, 1.6, 0.45)


def instrument():
    ticks = events(np.arange(0, D, 0.25), tick(3200, 0.0015)) + events(np.arange(0.125, D, 0.25), tick(2400, 0.0015)) * 0.6
    servo = sine(180) * 0.3 + sine(360) * 0.15
    servo *= np.clip(sine(0.5) * 2 - 1, 0, 1)                    # whirs now and then
    return wrapreverb(ticks * 0.8 + servo * 0.4 + loopnoise('pink', lambda n: lp(n, 800)) * 0.03, 0.4, 0.2)


def heavy():
    hum = sine(60) * 0.5 + sine(120) * 0.35 + sine(180) * 0.2 + sine(240) * 0.1
    hum = softclip(hum * 1.3, 1.2)
    th = events(np.arange(0, D, 1.0), thump(48)) * 0.9
    return wrapreverb(hum * 0.6 + th + loopnoise('brown', lambda n: lp(n, 200)) * 0.15, 0.7, 0.3)


def chem():
    times = np.sort(rng.uniform(0, D, 34))
    b = sum(events([t], bubble(rng.uniform(250, 700), rng.uniform(0.05, 0.1))) * rng.uniform(0.3, 1) for t in times)
    pump = (sine(50) * 0.3 + sine(100) * 0.15) * lfo(1.0, 0.7, 1.0)
    return wrapreverb(b * 0.7 + pump * 0.4 + loopnoise('pink', lambda n: bp(n, 300, 1200)) * 0.04, 0.5, 0.3)


def math_voice():
    pattern = [t for t in np.arange(0, D, 0.125) if rng.random() < 0.45]
    relays = events(pattern, tick(1800, 0.003, 0.03) + tick(900, 0.004, 0.03) * 0.5)
    fan = loopnoise('pink', lambda n: bp(n, 200, 2000)) * 0.12 * lfo(2.0, 0.85, 1.0) + sine(120) * 0.05
    return wrapreverb(relays * 0.7 + fan, 0.3, 0.2)


def bio():
    swish = loopnoise('pink', lambda n: bp(n, 300, 1500)) * np.clip(sine(1.5), 0, 1) ** 2
    pulse = events(np.arange(0, D, 1.0), thump(70, 0.25)) + events(np.arange(0.28, D, 1.0), thump(64, 0.2)) * 0.6
    return wrapreverb(swish * 0.4 + pulse * 0.7, 0.6, 0.35)


def earth():
    grind = loopnoise('brown', lambda n: lp(n, 300)) * 0.7 + loopnoise('white', lambda n: bp(n, 800, 2500)) * 0.12 * lfo(3.0, 0.4, 1.0)
    motor = sine(35) * 0.4 + sine(70) * 0.25
    return wrapreverb(softclip(grind + motor, 1.3), 0.5, 0.25)


def space():
    roar = loopnoise('brown', lambda n: lp(n, 180)) * 0.9 + loopnoise('pink', lambda n: lp(n, 900)) * 0.3
    hiss = loopnoise('white', lambda n: hp(n, 5000)) * 0.08 * lfo(0.25, 0.5, 1.0)
    return wrapreverb(roar + hiss, 0.8, 0.3)


def cryo():
    comp = (sine(45) * 0.4 + sine(90) * 0.25 + sine(135) * 0.1) * np.clip(sine(0.5), 0, 1) ** 0.5
    vent = loopnoise('white', lambda n: bp(n, 2500, 8000)) * np.clip(sine(0.5, np.pi), 0, 1) ** 3 * 0.35
    return wrapreverb(comp + vent, 0.6, 0.3)


def turbine():
    whine = sine(880) * 0.15 + sine(1760) * 0.06
    whine *= lfo(0.25, 0.8, 1.0)
    rumble = loopnoise('brown', lambda n: lp(n, 150)) * 0.6 + sine(55) * 0.3
    air = loopnoise('pink', lambda n: bp(n, 400, 3000)) * 0.15
    return wrapreverb(whine + rumble + air, 0.5, 0.25)


def plasma():
    hum = sine(100) * 0.35 + sine(150) * 0.2 + sine(300) * 0.08
    crackle = loopnoise('white', lambda n: hp(n, 2000)) * (rng.random(NS) < 0.004) * 3
    crackle = lp(crackle, 7000)
    sizzle = loopnoise('pink', lambda n: bp(n, 1500, 6000)) * 0.1 * lfo(0.75, 0.5, 1.0)
    return wrapreverb(hum + crackle * 0.8 + sizzle, 0.6, 0.3)


def void():
    wind = loopnoise('pink', lambda n: bp(n, 150, 900)) * lfo(0.25, 0.3, 1.0) * 0.6
    sub = sine(30) * 0.5 * lfo(0.5, 0.6, 1.0)
    glints = events(np.sort(rng.uniform(0, D, 7)), tick(4200, 0.12, 0.5) * 0.25)
    return wrapreverb(wind + sub + glints, 1.8, 0.5)


def data():
    chirps = events([t for t in np.arange(0, D, 1 / 16) if rng.random() < 0.35],
                    np.sin(2 * np.pi * np.linspace(1200, 2400, int(0.02 * SR)) * np.arange(int(0.02 * SR)) / SR) * np.hanning(int(0.02 * SR)))
    hum = sine(110) * 0.25 + sine(220) * 0.12 + sine(55) * 0.2
    return wrapreverb(hum + chirps * 0.35 + loopnoise('pink', lambda n: bp(n, 300, 3000)) * 0.06, 0.8, 0.35)


VOICES = {'void': void, 'data': data, 'quantum': quantum, 'cosmic': cosmic, 'instrument': instrument, 'heavy': heavy, 'chem': chem, 'math': math_voice,
          'bio': bio, 'earth': earth, 'space': space, 'cryo': cryo, 'turbine': turbine, 'plasma': plasma}
SUBTITLE = {'void': 'The void howls', 'data': 'Data streams', 'quantum': 'Quantum hardware chimes', 'cosmic': 'Something vast resonates', 'instrument': 'Instrument ticks',
            'heavy': 'Heavy machinery hums', 'chem': 'Reactor vessel bubbles', 'math': 'Relays click',
            'bio': 'Bioreactor pumps', 'earth': 'Drill grinds', 'space': 'Engines roar', 'cryo': 'Cryocooler pulses',
            'turbine': 'Turbine whines', 'plasma': 'Plasma crackles'}
VOID_WORDS = ('horizon', 'wormhole', 'rift', 'multiverse', 'dark_energy', 'vacuum', 'inflation', 'kugelblitz', 'white_hole',
              'penrose', 'black_hole', 'eschaton')
DATA_WORDS = ('matrioshka', 'omega_simulator', 'reality_compiler', 'omnilex', 'research_nexus', 'concept', 'causality',
              'chrono', 'tesseract', 'computer', 'connectome')
PLASMA_WORDS = ('tokamak', 'synchrotron', 'cyclotron', 'collider', 'plasma', 'forge', 'accretion', 'stellar', 'tesla')


def voice_for(m):
    if m['gen']:
        return 'turbine'
    if any(w in m['id'] for w in PLASMA_WORDS):
        return 'plasma'
    if any(w in m['id'] for w in VOID_WORDS):
        return 'void'
    if any(w in m['id'] for w in DATA_WORDS):
        return 'data'
    return MA.family(m['id'])


def write_ogg(name, x):
    x = x - np.mean(x)
    x = x / (np.max(np.abs(x)) + 1e-9) * 10 ** (-3 / 20)
    os.makedirs(OUT, exist_ok=True)
    with tempfile.NamedTemporaryFile(suffix='.wav', delete=False) as tf:
        wavfile.write(tf.name, SR, (x * 32767).astype(np.int16))
        subprocess.run(['ffmpeg', '-v', 'error', '-y', '-i', tf.name, '-ac', '1', '-c:a', 'libvorbis', '-q:a', '4',
                        os.path.join(OUT, name + '.ogg')], check=True)
    os.unlink(tf.name)


def java(ms):
    rows = []
    for m in ms:
        if not m['mb']:
            continue
        pitch = 0.85 + (MA.H(m['id'], 'pitch') % 31) / 100
        rows.append('        V.put("%s", new Voice("%s", %.2fF));' % (m['id'], voice_for(m), pitch))
    src = '''package com.theoreticalsciences.client;

import java.util.HashMap;
import java.util.Map;

/**
 * 1.5.1: which working loop each multiblock plays, and at what pitch. Generated by tools/sounds_work.py from the same
 * material families as the machine art, so a structure sounds like what it looks like.
 */
public final class MachineVoices {
    public record Voice(String sound, float pitch) {}

    private static final Map<String, Voice> V = new HashMap<>();

    static {
%s
    }

    public static Voice of(String machineId) {
        return V.get(machineId);
    }

    private MachineVoices() {}
}
''' % '\n'.join(rows)
    p = os.path.join(HERE, '..', 'src/main/java/com/theoreticalsciences/client/MachineVoices.java')
    open(p, 'w').write(src)
    return len(rows)


def json_and_lang():
    p = os.path.join(A, 'sounds.json')
    J = json.load(open(p, encoding='utf-8'))
    for v in VOICES:
        key = 'machine.work.' + v
        J[key] = {'subtitle': 'subtitles.theoreticalsciences.' + key, 'sounds': [{'name': 'theoreticalsciences:machine/' + v, 'stream': False}]}
    json.dump(J, open(p, 'w', encoding='utf-8'), indent=1, ensure_ascii=False)
    lp_ = os.path.join(A, 'lang', 'en_us.json')
    L = json.load(open(lp_, encoding='utf-8'))
    for v, s in SUBTITLE.items():
        L['subtitles.theoreticalsciences.machine.work.' + v] = s
    json.dump(L, open(lp_, 'w', encoding='utf-8'), indent=1, ensure_ascii=False)


if __name__ == '__main__':
    for name, fn in VOICES.items():
        x = fn()
        assert len(x) == NS
        write_ogg(name, x)
    ms = MA.machines()
    n = java(ms)
    json_and_lang()
    from collections import Counter
    print('wrote', len(VOICES), 'loops;', n, 'multiblocks voiced', Counter(voice_for(m) for m in ms if m['mb']))
