"""
The machine art revamp (1.5.1). Every machine and multiblock controller gets its own face, side and top, built from
three choices so no two look alike:

  * a body material, from the science it belongs to (brass instruments, white enamel chemistry, slate mathematics,
    green ceramic biology, riveted gunmetal heavy physics, basalt earth science, aerospace panels, frosted cryogenics,
    indigo quantum hardware, obsidian and gold for the endgame);
  * a face layout (big screen, porthole, screen and dial, twin displays, glass tank, slit and grille, arched window,
    turbine for generators);
  * an animated readout that matches what the machine does (spectra for spectrometers, a helix for gene work, an
    orbit for space machines, chalk curves for mathematics and so on), drawn frame by frame rather than flickered.

Sides show what goes in and out (pipes and a sight glass for fluids, fins for generators, a linked conduit band for
multiblock controllers, a hatch for items). Tops follow the material (fans, flask caps, lenses, domes, chimneys).

Run:  python3 tools/machine_art.py [preview <png>]
It reads every machine from MachineType.java, so new machines are covered automatically.
"""
import hashlib, math, os, re, sys
import numpy as np
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import blocks13_anim as BA
from blocks14 import hexc, save, N

TAU = math.tau
FRAMES = 16
SRC = os.path.join(HERE, '..', 'src/main/java/com/theoreticalsciences/machine/MachineType.java')


def H(s, salt=''):
    return int(hashlib.md5((salt + s).encode()).hexdigest(), 16)


def C(c):
    return hexc(c) if isinstance(c, str) else np.asarray(c, float)


def mix(a, b, t):
    a, b = C(a), C(b)
    return a * (1 - t) + b * t


def P(img, x, y, c, a=1.0):
    x, y = int(round(x)), int(round(y))
    if 0 <= x < N and 0 <= y < N:
        c = C(c)
        img[y, x, :3] = img[y, x, :3] * (1 - a) + c[:3] * a
        img[y, x, 3] = 1


def R(img, x0, y0, x1, y1, c):
    img[y0:y1 + 1, x0:x1 + 1] = C(c)


def L(img, x0, y0, x1, y1, c, a=1.0):
    n = int(max(abs(x1 - x0), abs(y1 - y0))) + 1
    for i in range(n):
        t = i / max(1, n - 1)
        P(img, x0 + (x1 - x0) * t, y0 + (y1 - y0) * t, c, a)


def D(img, cx, cy, r, c, a=1.0):
    y, x = np.mgrid[0:N, 0:N]
    m = np.hypot(x - cx, y - cy) <= r
    img[m, :3] = img[m, :3] * (1 - a) + C(c)[:3] * a


def glow(img, cx, cy, r, c, a=1.0):
    y, x = np.mgrid[0:N, 0:N]
    k = np.clip(1 - np.hypot(x - cx, y - cy) / r, 0, 1)[..., None] * a
    img[..., :3] = np.clip(img[..., :3] + C(c)[:3] * k, 0, 1)


# ====================================================================== materials
# base, dark, light, trim, and the surface pattern drawn on every face
MATERIALS = {
    'quantum':    ('#2a2850', '#14132a', '#4a4880', '#60e0ff', 'hex'),
    'cosmic':     ('#16121c', '#08060c', '#2c2436', '#e8c060', 'filigree'),
    'instrument': ('#b08840', '#6a4c1c', '#e0c070', '#5a3a1c', 'brass'),
    'heavy':      ('#4c525c', '#2a2e34', '#767e8a', '#f0c030', 'rivets'),
    'chem':       ('#dfe3e8', '#9aa2ae', '#ffffff', '#3a78c0', 'enamel'),
    'math':       ('#2c3a34', '#18221e', '#46584e', '#d8c8a0', 'slate'),
    'bio':        ('#a8d0b4', '#6a9a7c', '#d8f0e0', '#2f7a58', 'ceramic'),
    'earth':      ('#4a4440', '#2a2624', '#6e6660', '#b0602c', 'basalt'),
    'space':      ('#e8e8ec', '#a8acb4', '#ffffff', '#e07020', 'aero'),
    'cryo':       ('#c8d8e8', '#8aa0b8', '#f4faff', '#60b0ff', 'frost'),
}

FAMILY_WORDS = [
    ('cosmic', 'femtoscale stellar_plasma fractionation_spire kugelblitz penrose white_hole tesseract multiverse universe_forge '
               'matrioshka annihilation neutron_star wormhole causality dark_energy reality_compiler axiomatic omnilex '
               'transcendent eye_of_the_horizon vacuum_harvester research_nexus pleroma convergence essence_distillery '
               'inflation entropy_horizon anomaly eschaton omega_simulator mirror_foundry chrono_loom compactification '
               'concept_compressor nucleosynthesis'),
    ('cryo', 'bose_einstein quantum_computer neutrino_condenser ct_scanner isomer irradiation decay_chamber decontamination '
             'time_dilation micro_black_hole'),
    ('instrument', 'spectrograph seismograph atomic_clock galton reflecting_telescope radio_telescope x_ray optical_workshop '
                   'laser_interferometer pound_rebka brownian burst_observatory heliograph dating_spectrometer tribometer '
                   'neutron_interferometer particle_spectrometer mass_spectrometer infrared nmr lensing radar_survey luminescence isotope_provenancer photogrammetry'),
    ('space', 'rocket bussard space_elevator oneill stellar_engine interstellar_scoop dyson_array wind_tunnel load_test '
              'composite_layup warp_field'),
    ('earth', 'bloomery tectonic deep_drilling aquifer climate wave_energy geothermal ice_core storm_chamber lake_pump cloud_siphon '
              'solar_furnace'),
    ('math', 'peano analytic_continuator proof_assistant tensor ordinal monte_carlo markov eratosthenes geometers symmetry '
             'topology_kiln integrator statistics cipher optimiser graph_loom strategy pattern_basin bayesian knot_loom '
             'automaton certificate chaos_controller page_curve spin_network cantor axiom_distiller tree_ring'),
    ('bio', 'bioreactor gene_sequencer evolution abiogenesis enzyme patch_clamp fermenter photobioreactor ribosome folding '
            'phage mycelium antibody perfusion regeneration_tank circadian dosing ecosphere selection gene_circuit '
            'mirror_culture clonal hydroponic phytotron breeding ancient_dna revival neural_culture synapse psychophysics '
            'connectome dose_response neural_synthesizer decipherment'),
    ('chem', 'chemical_reactor electrolytic haber ostwald contact_process air_separation titration buffer brine reverse_osmosis '
             'bomb_calorimeter heat_pack fuel_cell electroplating chromatography cracking polymeriser synthesis_bench '
             'framework coordination chelation phase_diagram doping sintering photochemical asymmetric oscillator alkahest '
             'conservation catalyst quasicrystal autoclave regeneration_kiln crystal_growth lattice_growth corrosion gas_centrifuge '
             'molten_salt'),
    ('heavy', 'collider synchrotron cyclotron tokamak forge press rig drill accelerator magnet impact laser_cutter '
              'decelerator reactor mhd tesla_coil nuclide analog_black_hole resonance_chamber pulsed'),
]


def family(mid):
    for fam, words in FAMILY_WORDS:
        for w in words.split():
            if w in mid:
                return fam
    return 'quantum'


def surface(img, fam, seed):
    base, dark, light, trim, pat = MATERIALS[fam]
    rng = np.random.default_rng(seed)
    img[:] = C(base)
    img[..., :3] = np.clip(img[..., :3] + rng.normal(0, 0.018, (N, N, 1)), 0, 1)
    y, x = np.mgrid[0:N, 0:N]
    if pat == 'hex':
        for cy in range(0, N + 4, 4):
            for cx in range(-2, N + 4, 6):
                ox = cx + (3 if (cy // 4) % 2 else 0)
                L(img, ox, cy, ox + 2, cy, dark, 0.6)
    elif pat == 'filigree':
        for k in rng.integers(0, N * N, 9):
            P(img, k % N, k // N, '#7a6ca0', 0.7)                                   # star specks
        for (cx, cy, sx, sy) in ((1, 1, 1, 1), (14, 1, -1, 1), (1, 14, 1, -1), (14, 14, -1, -1)):
            L(img, cx, cy, cx + 3 * sx, cy, trim, 0.8); L(img, cx, cy, cx, cy + 3 * sy, trim, 0.8)
            P(img, cx + sx, cy + sy, trim, 0.6)
    elif pat == 'brass':
        img[..., :3] *= (0.86 + 0.22 * np.cos((x + y * 0.3) / 16 * math.pi - 0.4) ** 2)[..., None]
        for yy in (0, 15):
            img[yy, :, :3] = C('#5a3a1c')[:3]                                     # walnut rails
    elif pat == 'rivets':
        img[7, 1:15, :3] = C(dark)[:3]; img[8, 1:15, :3] = C(light)[:3] * 0.8      # plate seam
        for (rx, ry) in ((2, 2), (13, 2), (2, 13), (13, 13), (2, 5), (13, 10)):
            P(img, rx, ry, light); P(img, rx + 1, ry + 1, dark, 0.6)
    elif pat == 'enamel':
        img[..., :3] *= (0.92 + 0.1 * (1 - y / 15))[..., None]
    elif pat == 'slate':
        for k in range(6):
            yy = 2 + int(rng.integers(0, 12)); xx = int(rng.integers(1, 10))
            L(img, xx, yy, xx + int(rng.integers(2, 5)), yy, '#5c6c62', 0.5)           # old chalk smudges
    elif pat == 'ceramic':
        img[..., :3] *= (0.9 + 0.14 * np.cos((x - 4) / 16 * math.pi) * np.cos((y - 4) / 16 * math.pi))[..., None]
    elif pat == 'basalt':
        img[..., :3] = np.clip(img[..., :3] + rng.normal(0, 0.05, (N, N, 1)), 0, 1)
        for k in range(3):
            yy = int(rng.integers(2, 14))
            L(img, 1, yy, 14, yy + int(rng.integers(-1, 2)), dark, 0.5)
    elif pat == 'aero':
        for yy in (5, 10):
            img[yy, 1:15, :3] = C(dark)[:3]
        img[11:15, 1:4, :3] = C('#d8a830')[:3]                                     # gold foil patch
    elif pat == 'frost':
        for k in rng.integers(0, N * N, 14):
            P(img, k % N, k // N, '#ffffff', 0.7)
    return img


def border(img, fam, rounded=False):
    base, dark, light, trim, pat = MATERIALS[fam]
    edge = mix(dark, '#000000', 0.35)
    img[0, :, :3] = img[-1, :, :3] = img[:, 0, :3] = img[:, -1, :3] = edge[:3]
    img[1, 1:-1, :3] = img[1, 1:-1, :3] * 0.6 + C(light)[:3] * 0.4
    img[1:-1, 1, :3] = img[1:-1, 1, :3] * 0.6 + C(light)[:3] * 0.4
    img[-2, 1:-1, :3] = img[-2, 1:-1, :3] * 0.6 + C(dark)[:3] * 0.4
    img[1:-1, -2, :3] = img[1:-1, -2, :3] * 0.6 + C(dark)[:3] * 0.4
    if rounded:
        for (x, y) in ((0, 0), (15, 0), (0, 15), (15, 15)):
            img[y, x, :3] = C(base)[:3] * 0.7
    return img


# ====================================================================== readouts
# each draws onto a 16x16 screen layer for time t in [0,1); the layout clips it to its window
SCREEN = '#081018'


def dim(c, k):
    return mix(c, SCREEN, 1 - k)


def r_osc(s, a, t, p):
    f = 1 + p % 3
    for x in range(N):
        y = 7.5 + 3.5 * math.sin((x / 15 * f + t) * TAU) * (0.6 + 0.4 * math.sin(t * TAU + p))
        P(s, x, y, a); P(s, x, y + 1, a, 0.3)
    for x in range(0, N, 4):
        for y in range(0, N, 4):
            P(s, x, y, '#1c2a36')


def r_spectrum(s, a, t, p):
    rng = np.random.default_rng(p)
    lines = rng.integers(1, 15, 3)
    for x in range(1, 15):
        hue = mix('#ff4040', '#4060ff', x / 14)
        h = 2 + 3 * abs(math.sin(x * 0.7 + p))
        if x in lines:
            h += 6 * (0.6 + 0.4 * math.sin(t * TAU + x))
        for y in range(int(14 - h), 15):
            P(s, x, y, hue if y > 14 - h + 1 else a)


def r_radar(s, a, t, p):
    D(s, 7.5, 7.5, 6.5, '#0c2018');
    for r in (2.5, 4.5, 6.5):
        for k in range(36):
            ang = k / 36 * TAU
            P(s, 7.5 + math.cos(ang) * r, 7.5 + math.sin(ang) * r, '#1c4030')
    ang = t * TAU
    for k in range(5):
        aa = ang - k * 0.12
        L(s, 7.5, 7.5, 7.5 + math.cos(aa) * 6.5, 7.5 + math.sin(aa) * 6.5, a, 1 - k * 0.2)
    bx, by = 7.5 + math.cos(p) * 4, 7.5 + math.sin(p) * 4
    if ((ang - p) % TAU) < 1.5:
        P(s, bx, by, '#ffffff')


def r_orbit(s, a, t, p):
    glow(s, 7.5, 7.5, 3.5, '#ffd060', 0.9); D(s, 7.5, 7.5, 1.2, '#fff0a0')
    for k in range(48):
        ang = k / 48 * TAU
        P(s, 7.5 + math.cos(ang) * 6, 7.5 + math.sin(ang) * 3.2, '#203850')
    ang = t * TAU + p
    D(s, 7.5 + math.cos(ang) * 6, 7.5 + math.sin(ang) * 3.2, 1.1, a)


def r_lattice(s, a, t, p):
    for gx in range(3):
        for gy in range(3):
            x, y = 3 + gx * 5, 3 + gy * 5
            ph = math.sin(t * TAU + gx + gy * 2 + p)
            L(s, x, y, x + 5, y, '#284050'); L(s, x, y, x, y + 5, '#284050')
            D(s, x + ph * 0.6, y, 1.0, a)


def r_helix(s, a, t, p):
    for y in range(N):
        ph = y / 15 * TAU * 1.2 + t * TAU
        x1, x2 = 7.5 + math.sin(ph) * 4.5, 7.5 - math.sin(ph) * 4.5
        front = math.cos(ph) > 0
        if y % 2 == 0:
            L(s, x1, y, x2, y, '#3a5060')
        P(s, x1, y, a if front else dim(a, 0.45)); P(s, x2, y, dim(a, 0.45) if front else a)


def r_fringes(s, a, t, p):
    y, x = np.mgrid[0:N, 0:N]
    v = 0.5 + 0.5 * np.cos(np.hypot(x - 7.5, y - 7.5) * 1.3 - t * TAU)
    s[..., :3] = s[..., :3] * (1 - v[..., None] * 0.9) + C(a)[:3] * v[..., None] * 0.9


def r_heat(s, a, t, p):
    y, x = np.mgrid[0:N, 0:N]
    cx = 7.5 + 3 * math.cos(t * TAU + p); cy = 7.5 + 2 * math.sin(t * TAU * 2)
    v = np.clip(1 - np.hypot(x - cx, y - cy) / 7, 0, 1)
    v = np.floor(v * 5) / 5
    cols = np.stack([v * 1.0, v ** 2 * 0.8, (1 - v) * 0.4 * (v > 0)], -1)
    s[..., :3] = np.clip(cols + C(SCREEN)[:3] * (v[..., None] == 0), 0, 1)


def r_rain(s, a, t, p):
    rng = np.random.default_rng(p)
    for col in range(1, 15, 2):
        sp = rng.integers(1, 3); off = rng.integers(0, 16)
        head = int((t * 16 * sp + off) % 16)
        for k in range(5):
            P(s, col, (head - k) % 16, a if k == 0 else dim(a, 0.8 - k * 0.15))


def r_field(s, a, t, p):
    for k in range(-3, 4):
        for x in range(N):
            y = 7.5 + k * 2.2 * (1 + 0.6 * math.cos((x - 7.5) / 8 * math.pi))
            P(s, x, y, dim(a, 0.55))
    xx = (t * 16) % 16
    D(s, xx, 7.5, 1.0, a)


def r_bell(s, a, t, p):
    L(s, 1, 14, 14, 14, '#304050')
    fill = (t * 1.3) % 1
    for x in range(1, 15):
        h = 11 * math.exp(-((x - 7.5) / 3.2) ** 2) * min(1, fill * 1.4)
        for y in range(int(14 - h), 14):
            P(s, x, y, dim(a, 0.7))
        P(s, x, 14 - 11 * math.exp(-((x - 7.5) / 3.2) ** 2), a)
    P(s, 7 + int(3 * math.sin(t * TAU * 3)), 2, '#ffffff')


def r_spikes(s, a, t, p):
    rng = np.random.default_rng(p)
    for row, y in enumerate((4, 8, 12)):
        L(s, 1, y, 14, y, '#203040')
        for k in rng.integers(0, 16, 3):
            x = int((k + t * 16 * (row + 1)) % 16)
            L(s, x, y, x, y - 3, a); P(s, x + 1, y + 1, dim(a, 0.5))


def r_clock(s, a, t, p):
    for k in range(12):
        ang = k / 12 * TAU
        P(s, 7.5 + math.cos(ang) * 6, 7.5 + math.sin(ang) * 6, '#c0c8d0' if k % 3 else a)
    ang = t * TAU - math.pi / 2
    L(s, 7.5, 7.5, 7.5 + math.cos(ang) * 5, 7.5 + math.sin(ang) * 5, a)
    ang2 = t * TAU / 12 - math.pi / 2 + p
    L(s, 7.5, 7.5, 7.5 + math.cos(ang2) * 3, 7.5 + math.sin(ang2) * 3, '#ffffff')


def r_core(s, a, t, p):
    k = 0.75 + 0.25 * math.sin(t * TAU * 2)
    glow(s, 7.5, 7.5, 7 * k, a, 1.0); glow(s, 7.5, 7.5, 3, '#ffffff', 0.8 * k)
    for i in range(6):
        ang = i / 6 * TAU + t * TAU / 3
        P(s, 7.5 + math.cos(ang) * 6, 7.5 + math.sin(ang) * 6, a)


def r_torus(s, a, t, p):
    for k in range(64):
        ang = k / 64 * TAU
        x, y = 7.5 + math.cos(ang) * 6, 7.5 + math.sin(ang) * 3
        br = 0.5 + 0.5 * math.cos(ang * 3 - t * TAU * 3)
        P(s, x, y, mix(SCREEN, a, 0.3 + 0.7 * br)); P(s, x, y + 1, mix(SCREEN, a, 0.2 + 0.4 * br))
    glow(s, 7.5, 7.5, 2.5, '#ffffff', 0.4)


def r_tracks(s, a, t, p):
    rng = np.random.default_rng(p)
    for k in range(4):
        r0 = 2 + rng.random() * 4; sgn = 1 if k % 2 else -1; a0 = rng.random() * TAU
        n = int(16 * ((t + k * 0.25) % 1))
        for i in range(n):
            ang = a0 + sgn * i * 0.25
            rr = r0 * (1 - i / 20)
            P(s, 7.5 + math.cos(ang) * rr * 1.3 + (math.cos(a0) * i * 0.2), 7.5 + math.sin(ang) * rr, a, 0.4 + 0.6 * i / 16)
    P(s, 7, 7, '#ffffff'); P(s, 8, 8, '#ffffff')


def r_graph(s, a, t, p):
    rng = np.random.default_rng(p)
    pts = [(2 + rng.integers(0, 12), 2 + rng.integers(0, 12)) for _ in range(6)]
    for i in range(6):
        for j in range(i + 1, 6):
            if (i * 7 + j * 3 + p) % 3 == 0:
                L(s, *pts[i], *pts[j], '#2a4a5a')
    hot = int(t * 6) % 6
    for i, (x, y) in enumerate(pts):
        D(s, x, y, 0.9, a if i == hot else dim(a, 0.55))


def r_contour(s, a, t, p):
    y, x = np.mgrid[0:N, 0:N]
    v = np.sin(x / 3 + p) + np.cos(y / 2.6 - p) + 0.5 * np.sin((x + y) / 4 + t * TAU)
    m = (np.abs((v * 2.2) % 1 - 0.5) < 0.13)
    s[m, :3] = C(dim(a, 0.85))[:3]


def r_gauge(s, a, t, p):
    for k in range(13):
        ang = math.pi + k / 12 * math.pi
        P(s, 7.5 + math.cos(ang) * 6, 10 + math.sin(ang) * 6, '#ff5040' if k > 9 else '#c0c8d0')
    v = 0.5 + 0.35 * math.sin(t * TAU + p) + 0.08 * math.sin(t * TAU * 5)
    ang = math.pi + v * math.pi
    L(s, 7.5, 10, 7.5 + math.cos(ang) * 5, 10 + math.sin(ang) * 5, a)
    D(s, 7.5, 10, 1, '#ffffff')
    L(s, 3, 13, 12, 13, dim(a, 0.5))


def r_glyphs(s, a, t, p):
    rng = np.random.default_rng(p)
    shift = int(t * 16)
    for row in range(5):
        y = 2 + row * 3
        for x in range(1, 15):
            if rng.random() < 0.65:
                P(s, x, y - (shift % 3 == row % 3), a if (x + row + shift) % 7 else '#ffffff', 0.9)


def r_molecule(s, a, t, p):
    ang = t * TAU
    atoms = [(0, 0, 0), (4, 0, 0), (-2, 3.4, 0), (-2, -3.4, 0), (0, 0, 4), (0, 0, -4)]
    proj = []
    for (x, y, z) in atoms[:3 + p % 4]:
        xr = x * math.cos(ang) - z * math.sin(ang); zr = x * math.sin(ang) + z * math.cos(ang)
        proj.append((7.5 + xr, 7.5 + y, zr))
    for (x, y, z) in proj[1:]:
        L(s, 7.5, 7.5, x, y, '#4a6070')
    for (x, y, z) in sorted(proj, key=lambda q: q[2]):
        D(s, x, y, 1.3 + z * 0.08, a if z >= 0 else dim(a, 0.6))
    D(s, 7.5, 7.5, 1.4, '#ffffff')


def r_cells(s, a, t, p):
    k = (t * 1.0) % 1
    sep = 3 * min(1, k * 1.6)
    for sx in (-1, 1):
        D(s, 7.5 + sx * sep, 7.5, 3.4 - sep * 0.25, dim(a, 0.55))
        D(s, 7.5 + sx * sep, 7.5, 1.2, a)
    if sep < 1.5:
        D(s, 7.5, 7.5, 3.6, dim(a, 0.55)); D(s, 7.5, 7.5, 1.3, a)


def r_chalk(s, a, t, p):
    s[..., :3] = C('#1e2a24')[:3]
    L(s, 3, 12, 13, 12, '#a8b0a0'); L(s, 3, 12, 3, 3, '#a8b0a0')
    n = int(13 * ((t * 1.5) % 1)) + 4
    prev = None
    for i in range(16):
        x = 3 + i * 10 / 16
        y = 11 - 7 * (0.5 + 0.5 * math.sin(i / 13 * TAU * (1 + p % 2) + p))
        if prev:
            L(s, prev[0], prev[1], x, y, '#5a6a60')
        prev = (x, y)
    prev = None
    for i in range(n):
        x = 3 + i * 10 / 16
        y = 11 - 7 * (0.5 + 0.5 * math.sin(i / 13 * TAU * (1 + p % 2) + p))
        if prev:
            L(s, prev[0], prev[1], x, y, '#f0ead8')
        prev = (x, y)
    for k in range(3):
        P(s, 10 + k, 3, a)


def r_plasma(s, a, t, p):
    y, x = np.mgrid[0:N, 0:N]
    v = np.sin(x / 2.3 + t * TAU) + np.sin(y / 1.9 - t * TAU) + np.sin((x + y) / 3 + p)
    v = (v + 3) / 6
    s[..., :3] = np.clip(C(SCREEN)[:3] * (1 - v[..., None]) + C(a)[:3] * v[..., None] ** 1.5 + v[..., None] ** 6, 0, 1)


def r_bars(s, a, t, p):
    for i in range(5):
        x = 2 + i * 3
        h = 3 + 8 * (0.5 + 0.5 * math.sin(t * TAU * (1 + i % 2) + i + p))
        R(s, x, int(14 - h), x + 1, 14, dim(a, 0.8))
        R(s, x, int(14 - h), x + 1, int(14 - h), C('#ffffff'))


def r_starfield(s, a, t, p):
    rng = np.random.default_rng(p)
    for k in range(18):
        x0, y0, z = rng.random() * 16 - 8, rng.random() * 16 - 8, (rng.random() + t) % 1
        sc = 1 / (1.05 - z)
        P(s, 7.5 + x0 * sc * 0.4, 7.5 + y0 * sc * 0.4, a if z > 0.6 else dim(a, 0.5 + z * 0.4))
    glow(s, 7.5, 7.5, 3, a, 0.5)


READOUTS = {f.__name__[2:]: f for f in (r_osc, r_spectrum, r_radar, r_orbit, r_lattice, r_helix, r_fringes, r_heat,
                                        r_rain, r_field, r_bell, r_spikes, r_clock, r_core, r_torus, r_tracks, r_graph,
                                        r_contour, r_gauge, r_glyphs, r_molecule, r_cells, r_chalk, r_plasma, r_bars,
                                        r_starfield)}

READOUT_WORDS = [
    ('spectrum', 'spectro spectrograph infrared nmr mass_spectro dating x_ray chromatography isotope'),
    ('contour', 'tree_ring photogrammetry'),
    ('radar', 'radar_survey'),
    ('core', 'luminescence'),
    ('helix', 'gene dna ribosome sequencer clonal antibody gene_circuit selection evolution'),
    ('cells', 'culture bioreactor fermenter phage mycelium regeneration perfusion clonal mirror_culture abiogenesis breeding revival'),
    ('spikes', 'neural synapse connectome patch_clamp psychophysics'),
    ('orbit', 'rocket space_elevator oneill stellar_engine bussard dyson telescope interstellar heliograph matrioshka'),
    ('clock', 'clock chrono circadian time_dilation causality pound_rebka'),
    ('torus', 'tokamak synchrotron cyclotron collider accretion penrose kugelblitz decelerator'),
    ('tracks', 'particle quark neutrino higgs tachyon antimatter antiproton annihilation nuclide nucleosynthesis'),
    ('core', 'reactor zero_point star_forge neutron_star white_hole universe eschaton reality pleroma omega inflation'),
    ('fringes', 'interferometer resonance laser lensing holographic wave_energy'),
    ('lattice', 'lattice crystal quasicrystal framework metamaterial sinter doping lithograph femto nanite tesseract'),
    ('molecule', 'chemical synthesis polymer catalyst coordination chelation asymmetric photochemical haber ostwald contact enzyme alkahest'),
    ('bell', 'galton statistics monte_carlo bayesian markov dose_response'),
    ('chalk', 'peano proof analytic integrator geometers eratosthenes ordinal cantor axiom tensor symmetry optimiser certificate'),
    ('graph', 'graph strategy automaton knot network spin_network page_curve concept'),
    ('glyphs', 'cipher decipherment omnilex research compiler'),
    ('heat', 'bloomery furnace kiln calorimeter heat geothermal solar climate storm thermoelectric bomb'),
    ('radar', 'seismograph radio burst scoop siphon aquifer ice_core drilling'),
    ('field', 'magnet induction mhd tesla wind_tunnel field'),
    ('plasma', 'plasma fusion stellar transcendent forge'),
    ('starfield', 'wormhole multiverse warp horizon dark_energy entropy vacuum rift'),
    ('gauge', 'press rig tribometer corrosion load_test pump tank conservation'),
]


def readout_for(mid):
    for name, words in READOUT_WORDS:
        for w in words.split():
            if w in mid:
                return name
    keys = sorted(READOUTS)
    return keys[H(mid, 'ro') % len(keys)]


# ====================================================================== face layouts
# each returns (body image, window mask, lamp positions) and is drawn over the material surface
def mask_rect(x0, y0, x1, y1):
    m = np.zeros((N, N), bool); m[y0:y1 + 1, x0:x1 + 1] = True; return m


def mask_disc(cx, cy, r):
    y, x = np.mgrid[0:N, 0:N]; return np.hypot(x - cx, y - cy) <= r


def bezel(img, m, col):
    """A dark rim one pixel outside the window."""
    grown = m.copy()
    grown[1:] |= m[:-1]; grown[:-1] |= m[1:]; grown[:, 1:] |= m[:, :-1]; grown[:, :-1] |= m[:, 1:]
    rim = grown & ~m
    img[rim, :3] = C(col)[:3]


def lay_screen(img, fam, acc, seed):
    m = mask_rect(3, 2, 12, 10)
    bezel(img, m, '#101418')
    R(img, 3, 12, 12, 13, mix(MATERIALS[fam][1], '#000000', 0.2))
    lamps = [(4, 13), (6, 13), (8, 13)]
    R(img, 10, 13, 11, 13, MATERIALS[fam][3])
    return m, lamps


def lay_porthole(img, fam, acc, seed):
    m = mask_disc(7.5, 7.5, 4.6)
    ring = mask_disc(7.5, 7.5, 6.2) & ~m
    img[ring, :3] = C(MATERIALS[fam][3] if fam not in ('chem', 'space', 'cryo') else '#7a828c')[:3]
    for k in range(8):
        ang = k / 8 * TAU + math.pi / 8
        P(img, 7.5 + math.cos(ang) * 5.5, 7.5 + math.sin(ang) * 5.5, '#202428')
    return m, [(1, 14), (14, 14)]


def lay_dial(img, fam, acc, seed):
    m = mask_rect(2, 2, 9, 11)
    bezel(img, m, '#101418')
    D(img, 12.5, 4.5, 2.3, '#e8e4d8'); D(img, 12.5, 4.5, 0.6, '#202020')
    ang = (H(str(seed)) % 100) / 100 * math.pi + math.pi
    L(img, 12.5, 4.5, 12.5 + math.cos(ang) * 2, 4.5 + math.sin(ang) * 2, '#c02020')
    for yy in (9, 11, 13):
        R(img, 11, yy, 14, yy, '#2a2e34'); P(img, 11 + (yy + seed) % 4, yy, MATERIALS[fam][3])
    return m, [(3, 13), (5, 13), (7, 13)]


def lay_twin(img, fam, acc, seed):
    m = mask_rect(2, 2, 13, 7) | mask_rect(2, 10, 7, 13)
    bezel(img, m, '#101418')
    for yy in (10, 12):
        R(img, 9, yy, 13, yy, '#2a2e34')
    return m, [(10, 14), (12, 14), (9, 11)]


def lay_tank(img, fam, acc, seed):
    m = mask_rect(5, 2, 10, 13)
    bezel(img, m, '#6a7480')
    for yy in (1, 14):
        R(img, 4, yy, 11, yy, '#8a929c')
    for yy in (4, 8, 12):
        P(img, 11, yy, '#202428'); P(img, 12, yy, '#202428')                  # graduation marks
    return m, [(2, 3), (2, 6), (13, 3)]


def lay_slit(img, fam, acc, seed):
    for yy in range(2, 8, 2):
        R(img, 2, yy, 13, yy, mix(MATERIALS[fam][1], '#000000', 0.4))       # grille
    m = mask_rect(2, 10, 13, 13)
    bezel(img, m, '#101418')
    return m, [(3, 8), (12, 8)]


def lay_arch(img, fam, acc, seed):
    m = mask_rect(3, 6, 12, 12) | (mask_disc(7.5, 6, 4.6) & mask_rect(3, 1, 12, 6))
    bezel(img, m, mix(MATERIALS[fam][3], '#000000', 0.3))
    return m, [(3, 14), (7, 14), (12, 14)]


def lay_turbine(img, fam, acc, seed):
    m = mask_disc(7.5, 7.5, 5.5)
    ring = mask_disc(7.5, 7.5, 6.8) & ~m
    img[ring, :3] = C('#30343a')[:3]
    return m, [(1, 1), (14, 1)]


LAYOUTS = {'screen': lay_screen, 'porthole': lay_porthole, 'dial': lay_dial, 'twin': lay_twin, 'tank': lay_tank,
           'slit': lay_slit, 'arch': lay_arch, 'turbine': lay_turbine}

FAMILY_LAYOUTS = {
    'quantum': ('screen', 'porthole', 'twin', 'slit'), 'cosmic': ('porthole', 'arch', 'screen', 'twin'),
    'instrument': ('dial', 'porthole', 'screen'), 'heavy': ('slit', 'porthole', 'dial', 'twin'),
    'chem': ('tank', 'screen', 'dial', 'porthole'), 'math': ('screen', 'twin', 'arch'),
    'bio': ('arch', 'tank', 'porthole'), 'earth': ('slit', 'dial', 'screen'),
    'space': ('porthole', 'twin', 'screen'), 'cryo': ('porthole', 'tank', 'screen'),
}


def turbine_readout(s, a, t, p):
    D(s, 7.5, 7.5, 6, '#14181c')
    for k in range(5):
        ang = k / 5 * TAU + t * TAU / 5 * 2
        for r in np.linspace(1, 5.2, 9):
            P(s, 7.5 + math.cos(ang + r * 0.18) * r, 7.5 + math.sin(ang + r * 0.18) * r, '#9aa2ae')
    D(s, 7.5, 7.5, 1.4, a)
    glow(s, 7.5, 7.5, 6, a, 0.25)


def tank_readout(fluid):
    def f(s, a, t, p):
        level = 4
        R(s, 0, level, 15, 15, mix(fluid, SCREEN, 0.25))
        for k in range(4):
            bx = 6 + (k * 3 + p) % 5
            by = 15 - ((t * 16 + k * 4.5) % 12)
            if by > level:
                P(s, bx, by, '#ffffff', 0.7)
        R(s, 0, level, 15, level, mix(fluid, '#ffffff', 0.4))
    return f


def front(m, fam):
    seed = H(m['id']) % 100000
    base = surface(np.zeros((N, N, 4)), fam, seed)
    fluids = m['fi'] + m['fo'] > 0
    if m['gen'] and not m['mb']:
        lay = 'turbine'
    else:
        opts = FAMILY_LAYOUTS[fam]
        if not fluids:
            opts = tuple(o for o in opts if o != 'tank') or opts
        lay = opts[H(m['id'], 'lay') % len(opts)]
    mask, lamps = LAYOUTS[lay](base, fam, m['col'], seed)
    border(base, fam, rounded=fam in ('bio', 'space'))
    if m['mb']:                                                                   # controllers wear a trim chevron
        for k in range(3):
            P(base, 1 + k, 1, MATERIALS[fam][3]); P(base, 1, 1 + k, MATERIALS[fam][3])
    if lay == 'turbine':
        ro = turbine_readout
    elif lay == 'tank':
        ro = tank_readout(m['col'])
    else:
        ro = READOUTS[readout_for(m['id'])]
    acc = m['col']
    if sum(C(acc)[:3]) < 0.9:
        acc = mix(acc, '#ffffff', 0.4)

    def frame_at(t, on):
        img = base.copy()
        s = np.zeros((N, N, 4)); s[:] = C(SCREEN)
        if on:
            ro(s, acc, t, seed % 97)
        else:
            ro(s, acc, 0.0, seed % 97)
            s[..., :3] = s[..., :3] * 0.28 + C(SCREEN)[:3] * 0.72
        img[mask] = s[mask]
        img[mask, :3] = np.clip(img[mask, :3] + 0.05, 0, 1) if on else img[mask, :3]
        for i, (x, y) in enumerate(lamps):
            if on:
                lit = (int(t * 8) + i) % 3 != 0
                P(img, x, y, acc if lit else mix(acc, '#000000', 0.5))
            else:
                P(img, x, y, '#2a2020')
        img[..., 3] = 1
        return img
    return frame_at(0, False), [frame_at(f / FRAMES, True) for f in range(FRAMES)]


# ====================================================================== sides and tops
def side(m, fam):
    seed = H(m['id'], 'side') % 100000
    img = surface(np.zeros((N, N, 4)), fam, seed)
    base, dark, light, trim, pat = MATERIALS[fam]
    fluids = m['fi'] + m['fo'] > 0
    v = H(m['id'], 'sv') % 3
    if m['gen']:
        for x in range(3, 13, 2):
            R(img, x, 2, x, 13, mix(light, '#ffffff', 0.2)); R(img, x + 1, 2, x + 1, 13, dark)   # cooling fins
    elif fluids:
        px_ = (3, 11)[v % 2]
        R(img, px_, 0, px_ + 2, 15, '#8a929c'); R(img, px_ + 1, 0, px_ + 1, 15, '#b0b8c2')
        for yy in (3, 12):
            R(img, px_ - 1, yy, px_ + 3, yy, '#5a626c')                                 # flanges
        sx = 7 if px_ == 3 else 3
        R(img, sx, 4, sx + 3, 11, '#2a2e34'); R(img, sx + 1, 5, sx + 2, 10, SCREEN)
        R(img, sx + 1, 8 - v, sx + 2, 10, m['col'])                                     # sight glass
    else:
        kind = ('hatch', 'vent', 'cables', 'plate', 'pipe')[H(m['id'], 'sk') % 5]
        if kind == 'hatch':
            R(img, 4, 4, 11, 11, mix(dark, '#000000', 0.2)); R(img, 5, 5, 10, 10, mix(base, dark, 0.4))
            R(img, 6, 7, 9, 8, '#1a1c20'); P(img, 5, 5, light); P(img, 10, 10, dark)
        elif kind == 'vent':
            for yy in range(4, 12, 2):
                R(img, 3, yy, 12, yy, mix(dark, '#000000', 0.45))
            P(img, 13, 12, m['col'])
        elif kind == 'cables':
            for i, x in enumerate((4, 7, 10)):
                cc = ('#202428', mix(m['col'], '#303030', 0.4), '#5a3a20')[i]
                for yy in range(1, 15):
                    P(img, x + round(math.sin(yy / 3 + i) * 0.6), yy, cc)
            R(img, 3, 6, 12, 7, '#3a3e44')                                               # a clamp
        elif kind == 'plate':
            R(img, 3, 3, 12, 8, mix(light, '#ffffff', 0.25)); R(img, 3, 8, 12, 8, dark)
            for x in range(4, 12, 2):
                P(img, x, 5, '#303030', 0.7)                                             # a stamped label
            P(img, 3, 3, '#606060'); P(img, 12, 3, '#606060')
            R(img, 5, 10, 10, 12, mix(dark, '#000000', 0.3)); P(img, 7, 11, m['col'])
        else:
            R(img, 0, 6, 15, 8, '#7a828c'); R(img, 0, 7, 15, 7, '#a8b0ba')
            for x in (3, 12):
                R(img, x, 5, x, 9, '#4a525c')                                            # pipe brackets
            D(img, 8, 11.5, 1.6, '#30363e'); P(img, 8, 11, m['col'])
    if m['mb']:
        img[14, 1:15, :3] = C(trim)[:3]
        for x in range(2, 14, 4):
            P(img, x, 14, m['col'])
    if v == 2 and not m['gen']:
        for x in range(2, 14, 3):
            P(img, x, 1, '#e8c020' if fam in ('heavy', 'earth') else trim)
    border(img, fam, rounded=fam in ('bio', 'space'))
    return img


def top(m, fam):
    seed = H(m['id'], 'top') % 100000
    img = surface(np.zeros((N, N, 4)), fam, seed)
    base, dark, light, trim, pat = MATERIALS[fam]
    v = H(m['id'], 'tv') % 2
    if fam in ('heavy', 'quantum') or m['gen']:
        D(img, 7.5, 7.5, 5.5, '#1e2228')
        for k in range(6):
            ang = k / 6 * TAU + v * 0.5
            L(img, 7.5, 7.5, 7.5 + math.cos(ang) * 5, 7.5 + math.sin(ang) * 5, '#5a626c')   # fan
        D(img, 7.5, 7.5, 1.2, m['col'])
    elif fam == 'chem':
        for (cx, cy) in ((5, 5), (10.5, 10.5)) if v else ((5, 10.5), (10.5, 5)):
            D(img, cx, cy, 2.4, '#7a828c'); D(img, cx, cy, 1.4, '#30363e')                   # flask necks
        P(img, 10, 5 if v else 10, m['col'])
    elif fam == 'instrument' and v:
        for r in (5.5, 4.5):
            for k in range(40):
                ang = k / 40 * TAU
                P(img, 7.5 + math.cos(ang) * r, 7.5 + math.sin(ang) * r, '#5a3a1c' if r > 5 else '#e0c070')   # gimbal
        L(img, 2, 7.5, 13, 7.5, '#5a3a1c'); D(img, 7.5, 7.5, 1.5, '#203048')
    elif fam == 'math' and v:
        R(img, 2, 2, 13, 13, '#7a5a34')
        for k in range(8):
            ang = k / 8 * TAU
            L(img, 7.5, 7.5, 7.5 + math.cos(ang) * (5 if k % 2 == 0 else 3), 7.5 + math.sin(ang) * (5 if k % 2 == 0 else 3), '#e0c070')
        P(img, 7.5, 7.5, m['col'])                                                         # compass rose inlay
    elif fam == 'bio' and v:
        for (cx, cy) in ((5, 5), (10.5, 10.5), (10.5, 4.5)):
            D(img, cx, cy, 2.3, mix('#c8f0d8', base, 0.3)); D(img, cx, cy, 1.4, mix(m['col'], '#40a060', 0.5))
    elif fam == 'earth' and v:
        for cx in (4.5, 10.5):
            D(img, cx, 7.5, 2.4, '#2a2420'); D(img, cx, 7.5, 1.4, '#100c0a')
    elif fam == 'space' and v:
        D(img, 7.5, 7.5, 5.5, '#c8ccd4'); D(img, 7.5, 7.5, 4.2, '#e8ecf0')
        L(img, 7.5, 7.5, 11, 4, '#606870'); D(img, 7.5, 7.5, 1, m['col'])                # dish antenna
    elif fam == 'cryo' and v:
        for k in range(2, 14, 3):
            L(img, k, 2, k, 13, '#e8f4ff'); L(img, 2, k, 13, k, '#a8c4e0')
    elif fam == 'cosmic' and v:
        pts = [(7.5 + math.cos(a) * 5, 7.5 + math.sin(a) * 5) for a in (-math.pi / 2, math.pi / 6, 5 * math.pi / 6)]
        for i in range(3):
            L(img, *pts[i], *pts[(i + 1) % 3], trim)
        D(img, 7.5, 8, 1.5, m['col'])                                                     # sigil
    elif fam == 'instrument':
        D(img, 7.5, 7.5, 4.5, '#5a3a1c'); D(img, 7.5, 7.5, 3.4, '#c8a050'); D(img, 7.5, 7.5, 2.2, '#203048')
        P(img, 6.5, 6.5, '#a0c8ff')                                                       # lens
    elif fam == 'bio':
        D(img, 7.5, 7.5, 5, mix('#c8f0d8', base, 0.3)); D(img, 7.5, 7.5, 3.6, mix(m['col'], '#40a060', 0.5))
        P(img, 6, 6, '#ffffff')                                                            # culture dome
    elif fam == 'math':
        R(img, 2, 2, 13, 13, '#6a4a2a')
        for k in range(2, 14, 3):
            L(img, k, 2, k, 13, '#d8c8a0', 0.4); L(img, 2, k, 13, k, '#d8c8a0', 0.4)      # inlaid grid
    elif fam == 'earth':
        D(img, 7.5, 7.5, 3.4, '#2a2420'); D(img, 7.5, 7.5, 2.2, '#100c0a')               # chimney
        P(img, 7, 7, '#ff8030', 0.6)
    elif fam == 'space':
        for x0 in (2, 9):
            R(img, x0, 2, x0 + 4, 13, '#30486a')
            for yy in range(3, 13, 2):
                R(img, x0, yy, x0 + 4, yy, '#4a6a90')                                    # radiator panels
    elif fam == 'cryo':
        for r in (5.5, 3.5):
            for k in range(40):
                ang = k / 40 * TAU
                P(img, 7.5 + math.cos(ang) * r, 7.5 + math.sin(ang) * r, '#e8f4ff')         # frosted coil
    elif fam == 'cosmic':
        D(img, 7.5, 7.5, 5, '#05040a')
        rng = np.random.default_rng(seed)
        for k in range(10):
            P(img, 7.5 + rng.normal(0, 2), 7.5 + rng.normal(0, 2), '#ffffff', 0.8)
        for k in range(24):
            ang = k / 24 * TAU
            P(img, 7.5 + math.cos(ang) * 5.5, 7.5 + math.sin(ang) * 5.5, trim)
    border(img, fam, rounded=fam in ('bio', 'space'))
    return img


# ====================================================================== write
def machines():
    src = open(SRC).read()
    ms = re.findall(r'^\s+([A-Z_0-9]+)\s*\(\s*"(\w+)",\s*(\d+),\s*(\d+),\s*(\d+),\s*(\d+),\s*(true|false),\s*(null|"\w+"),'
                    r'.*?Effect\.(\w+),\s*0x([0-9A-Fa-f]{6})', src, re.M)
    return [dict(id=m[0].lower(), i=int(m[2]), fi=int(m[3]), o=int(m[4]), fo=int(m[5]), gen=m[6] == 'true',
                 mb=m[7] != 'null', col='#' + m[9].lower()) for m in ms]


def write_all():
    out = {}
    for m in machines():
        fam = family(m['id'])
        off, on = front(m, fam)
        save(off, 'machine/%s_front_off' % m['id'])
        path = save(on[0], 'machine/%s_front_on' % m['id'])
        BA.write(on, path)
        save(side(m, fam), 'machine/%s_side' % m['id'])
        save(top(m, fam), 'machine/%s_top' % m['id'])
        out[m['id']] = (on[4], side(m, fam), top(m, fam), off, fam)
    return out


if __name__ == '__main__':
    out = write_all()
    if len(sys.argv) > 2 and sys.argv[1] == 'preview':
        names = list(out)
        cols = 12
        W = Image.new('RGBA', (cols * 104, ((len(names) + cols - 1) // cols) * 36), (60, 60, 66, 255))
        for i, n in enumerate(names):
            for j in range(3):
                im = Image.fromarray((np.clip(out[n][j], 0, 1) * 255).astype(np.uint8), 'RGBA').resize((32, 32), Image.NEAREST)
                W.alpha_composite(im, ((i % cols) * 104 + 2 + j * 33, (i // cols) * 36 + 2))
        W.save(sys.argv[2])
    from collections import Counter
    print('wrote', len(out), 'machines', Counter(v[4] for v in out.values()))
