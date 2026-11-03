"""
Round 50 (1.9.0, "Unification"): where quantum mechanics and gravity meet - the capstone of the expansion.

No experiment has reached the Planck scale, but two real ones put quantum theory and gravity in the same equation:

* Neutron Interferometer (Colella, Overhauser and Werner, 1975): a neutron's wave is split by a perfect silicon crystal
  and recombined. Tilt the interferometer and one path runs higher in gravity than the other; the phase between them
  shifts by Δφ = 2πm²gλA·sin α/h² - gravity acting on a quantum wave. Set the tilt to land on a bright fringe.
* Analog Black Hole (Steinhauer): a Bose-Einstein condensate flows over a step, subsonic before it and supersonic after.
  Sound cannot climb back over that line - a sonic horizon - and pairs of phonons appear across it, at the Hawking
  temperature T_H = ħκ/2πk_B set by how steeply the flow speeds up. Only a condensate colder than T_H shows them.
* The capstone, Semiclassical Gravity, brings both to the Monte Carlo Engine: causal dynamical triangulations - quantum
  spacetime summed over at random - give a second way to the Spin Foam Lattice.
"""
import json
import math
import os

import shapes
import round12 as _r12
from round12 import P as _P, D as _D, C as _C, V as _V

NS = 'theoreticalsciences'
HERE = os.path.dirname(os.path.abspath(__file__))


def _I(i, n, art, c1, c2, lore, **kw):
    d = dict(id=i, name=n, art=art, c1=c1, c2=c2, lore=lore)
    d.update(kw)
    return d


def _B(i, n, art, c1, c2, kind='casing', light=2, hardness=5.0, tool='iron', **kw):
    d = dict(id=i, name=n, art=art, c1=c1, c2=c2, kind=kind, light=light, hardness=hardness, tool=tool)
    d.update(kw)
    return d


ITEMS = [
    _I('silicon_boule', 'Silicon Boule', 'crystal', '#505868', '#c8d8f0',
       'Silicon reduced from silica with carbon, then pulled from its melt as one flawless crystal: the most perfect solid people make.'),
    _I('perfect_crystal_interferometer', 'Perfect Crystal Interferometer', 'lattice', '#606878', '#e0ecff',
       'Three slabs cut from one silicon crystal, still joined at the base, so their lattice planes line up to a fraction of an atom: they split '
       'a neutron\'s wave, steer it, and bring it back together.'),
    _I('cow_record', 'Gravity-Phase Record', 'codex', '#202838', '#a0c0ff',
       'Neutron counts swinging between the two exits as the interferometer tilts: the fringes gravity draws on a quantum wave.'),
    _I('hawking_record', 'Hawking Correlation Record', 'codex', '#081018', '#c0f0ff',
       'Phonon pairs straddling a sonic horizon, one on each side, correlated as Hawking said they must be: the glow of a horizon, seen.'),
]
BLOCKS = [
    _B('neutron_guide', 'Neutron Guide', 'beampipe', '#4a5060', '#c0d0e0', light=0),
    _B('tilt_table', 'Tilt Table', 'coilblock', '#6a6e72', '#c0c4c8', light=0),
    _B('crystal_interferometer_mount', 'Crystal Interferometer Mount', 'rfcavity', '#505868', '#c8d8f0', light=4),
    _B('helium3_detector', 'Helium-3 Detector', 'injector', '#5a5a60', '#ffe0a0', light=3),
    _B('bec_trap', 'Condensate Trap', 'cryo', '#203048', '#a0e0ff', light=6),
    _B('step_laser', 'Step Laser', 'injector', '#601830', '#ff5060', light=5),
]
FLUIDS = []

KEY_ADD = {
    'Ц': ['neutron_guide'], 'Ч': ['tilt_table'], 'Щ': ['crystal_interferometer_mount'], 'Ъ': ['helium3_detector'],
    'Ю': ['bec_trap'], 'Ы': ['step_laser'], 'Й': ['tilt_table', 'interface_port'],
}


def neutron_interferometer():
    """A shielded radium-beryllium source, a long neutron guide, and at its end a tilt table carrying the three-bladed
    silicon interferometer between the guide and two helium-3 detectors."""
    s = shapes.Shape(12, 5, 5)
    s.box(0, 0, 1, 1, 3, 3, '>')                                          # the source in lead
    for x in range(2, 7):
        s.put(x, 2, 2, 'Ц')                                               # the neutron guide
        if x % 2 == 0:
            s.put(x, 1, 2, 'Ч'); s.put(x, 0, 2, 'Ч')                      # on stands
    s.box(6, 0, 0, 11, 0, 4, 'Ч')                                         # the tilt table
    s.box(7, 1, 1, 10, 1, 3, 'Ч')
    for x in (7, 8, 9):
        s.put(x, 2, 2, 'Щ')                                               # the three blades of the crystal
    s.put(10, 2, 1, 'Ъ'); s.put(10, 2, 3, 'Ъ')                            # the two exit detectors
    s.put(11, 1, 2, 'Ч'); s.put(11, 2, 2, 'Ч')                            # the tilt drive
    s.center = (8.0, 2.6, 2.0)
    s.ports('Ч', 'Й', 4, y=0)
    s.v[(8, 1, 0)] = 'C'
    s.ctrl = (8, 1, 0)
    return s


def analog_black_hole():
    """A vacuum tube on the optical bench, ringed by trapping magnets; the condensate trap in the middle, and above it
    the laser that makes the step the flow falls over."""
    s = shapes.Shape(13, 6, 5)
    s.box(0, 0, 0, 12, 0, 4, 'Ξ')
    for x in range(1, 12):
        s.put(x, 2, 2, 'Λ')                                               # the vacuum tube
    for x in (2, 10):
        s.put(x, 1, 2, 'Ξ')                                               # its supports
    for x in (3, 9):
        for dy, dz in ((-1, -1), (-1, 0), (-1, 1), (0, -1), (0, 1), (1, -1), (1, 0), (1, 1)):
            s.put(x, 2 + dy, 2 + dz, ':')                                 # trapping magnets round the tube
    for x in (5, 6, 7):
        s.put(x, 2, 2, 'Ю')                                               # the condensate trap
    s.put(6, 3, 2, 'Ξ')                                                   # the laser's mast
    s.put(6, 4, 2, 'Ы')                                                   # the step laser
    s.put(6, 5, 2, 'Ξ')
    s.center = (6.0, 2.6, 2.0)
    s.ports('Ξ', 'Π', 4, y=0)
    s.v[(6, 1, 0)] = 'C'
    s.ctrl = (6, 1, 0)
    return s


shapes.BUILDERS.update({'neutron_interferometer': neutron_interferometer, 'analog_black_hole': analog_black_hole})

MACHINE_TEXT = {
    'neutron_interferometer': ('Neutron Interferometer',
                               'A neutron\'s wave split by a perfect silicon crystal and joined again. Tilt it, and gravity pulls one path more '
                               'than the other: quantum mechanics and gravity in one measurement.'),
    'analog_black_hole': ('Analog Black Hole',
                          'A condensate flowing faster than its own speed of sound: a horizon that sound cannot cross back over - and, at it, '
                          'Hawking radiation of phonons.'),
}
MACHINE_CAT = {k: 'spacetime' for k in MACHINE_TEXT}
MACHINE_RESEARCH = {'neutron_interferometer': 'neutron_interferometry', 'analog_black_hole': 'analog_gravity'}
TIER = {k: 3 for k in MACHINE_TEXT}
_S = 'spacetime_casing'
MC = {
    'neutron_interferometer': (['DCD', 'GSG', 'TTT'], {'D': 'helium3_detector', 'C': 'crystal_interferometer_mount', 'G': 'neutron_guide', 'S': _S,
                                                       'T': 'tilt_table'}),
    'analog_black_hole': (['MLM', 'VSV', 'BTB'], {'M': 'mhd_field_magnet', 'L': 'step_laser', 'V': 'vacuum_beam_tube', 'S': _S, 'B': 'optical_bench',
                                                  'T': 'bec_trap'}),
}
EXTRA = {
    'neutron_interferometer': 'Choose the neutron wavelength and tilt the interferometer: gravity shifts the phase between the paths by '
                              'Δφ = 2πm²gλA·sin α/h² (39.4 rad per Å here, upright). Land on a bright fringe with at least 30 rad of shift, and count '
                              'long enough to fix the phase to 0.1 rad.',
    'analog_black_hole': 'Feed it Bose-Einstein Condensate and keep it below 5 K. Set the density, the flow, and the laser step: the flow must be '
                         'subsonic before the step and supersonic after it. The Hawking temperature, ħκ/2πk_B, must stand out of the condensate\'s '
                         'own warmth over enough shots - only Bose-Einstein Condensate as the coolant is cold enough.',
}

_r12.model('neutron_interferometer', 'Neutron Interferometer',
           'A neutron of wavelength λ is split by the first silicon blade, its two paths enclosing an area A, and joined at the third. Tilt the '
           'plane of the paths by α and one path climbs higher than the other: gravity changes its momentum, and the phase between them shifts by '
           'Δφ = 2πm²gλA·sin α/h². The counts at an exit follow (1 + V·cos Δφ)/2; N neutrons fix the phase to 1/(V√N).',
           'Thermal bending of the crystal: the phase drifts by ±0.05 rad·d.',
           [_P('al', 'Tilt α', '°', 0, 90, 0.01, 30, 'How far the plane of the two paths is tilted from the horizontal.'),
            _P('lam', 'Neutron wavelength λ', 'Å', 1, 3, 0.001, 1.4, 'Set by the velocity selector: slower neutrons, longer waves.'),
            _P('t', 'Counting time', 'min', 1, 600, 1, 30, 'How long the neutrons are counted at this setting.')],
           [_D('phi', 'Gravitational phase Δφ', 'rad', '39.38*lam*sin(al*0.0174533)', 'Δφ = 2πm²gλA·sin α/h²', 'The phase gravity puts between '
                                                                                                         'the paths (A = 10 cm²).'),
            _D('fr', 'Fringe', '', 'cos(phi + 0.05*d)', 'cos Δφ', '1 on a bright fringe, −1 on a dark one.'),
            _D('sp', 'Phase error', 'rad', '1/(0.85*sqrt(40*t))', 'σ = 1/(V√N)', 'How well the counts fix the phase (40 neutrons a minute).')],
           [],
           rate='clamp(2*60/(t + 60), 0.2, 2)', rate_formula='rate = 2·60/(t + 60 min)', rate_desc='Shorter counts are faster.',
           heat='0.02', idle_heat='0', kappa=40, heat_capacity=40)

_r12.model('analog_black_hole', 'Analog Black Hole',
           'A condensate of density n carries sound at c = √(gn/m). It flows at v toward a step of depth U that speeds it to √(v² + 2kU/m) and '
           'thins it, so beyond the step it outruns its own sound: a horizon. There the flow speeds up over about a healing length, giving a '
           'surface gravity κ, and phonon pairs appear across the horizon at T_H = ħκ/2πk_B. They show only against a condensate colder than that, '
           'summed over many shots.',
           'Shot-to-shot atom number: the density varies by ±3%·d.',
           [_P('n', 'Density', '10¹⁴ cm⁻³', 0.1, 10, 0.01, 1, 'Atoms per volume in the condensate. Denser carries sound faster.'),
            _P('v', 'Upstream flow', 'mm/s', 0.05, 2, 0.001, 0.5, 'How fast the condensate flows toward the step.'),
            _P('U', 'Step depth', 'nK', 0, 500, 0.1, 50, 'How deep the laser step it falls over is.'),
            _P('sh', 'Shots', '', 1, 5000, 1, 100, 'How many condensates are made and imaged, their correlations summed.')],
           [_D('cu', 'Sound speed before', 'mm/s', '1.9*sqrt(n*(1 + 0.03*d))', 'c = √(gn/m)', 'Speed of sound in the upstream condensate.'),
            _D('vd', 'Flow after the step', 'mm/s', 'sqrt(v^2 + 0.1914*U)', 'v₂ = √(v² + 2kU/m)', 'The flow, sped up by the step.'),
            _D('cd', 'Sound speed after', 'mm/s', '1.9*sqrt(n*(1 + 0.03*d)*v/vd)', 'c₂ = c·√(n₂/n)', 'The thinned condensate carries sound slower.'),
            _D('mu', 'Mach number before', '', 'v/cu', 'v/c', 'Below 1: sound can still travel upstream.'),
            _D('md', 'Mach number after', '', 'vd/cd', 'v₂/c₂', 'Above 1: sound is swept away - past the horizon.'),
            _D('TH', 'Hawking temperature', 'nK', '1.215*pos(vd - cd)', 'T_H = ħκ/2πk_B', 'The horizon\'s glow (κ from the flow\'s rise over a '
                                                                                         'healing length of 1 µm).'),
            _D('Tb', 'Condensate temperature', 'nK', '1 + 25*T', 'T_c', 'The condensate\'s own warmth, set by the trap walls.'),
            _D('SNR', 'Correlation signal', '', '0.5*TH/Tb*sqrt(sh)', 'S ∝ (T_H/T)·√shots', 'How clearly the pairs stand out.')],
           [_C('warm', 'stop', '5 - T', 'The trap is too warm for a condensate to form', 'T < 5 K')],
           rate='clamp(400/(sh + 100), 0.2, 2)', rate_formula='rate = 400/(shots + 100)', rate_desc='Fewer shots are faster.',
           heat='0.02', idle_heat='0', kappa=40, heat_capacity=40)

_r12.WINDOWS['neutron_interferometer'] = {'r50_cow': ({}, [('fr', 0.9, 1), ('phi', 30, 1e9), ('sp', 0, 0.1)])}
_r12.WINDOWS['analog_black_hole'] = {'r50_hawking': ({}, [('mu', 0, 0.9), ('md', 1.2, 1e9), ('SNR', 5, 1e12)])}
_r12.WINDOWS.setdefault('crystal_growth_furnace', {}).update({
    'r50_grow_silicon': ({'Tm': 1687, 'k0': 0.05}, [('xi', 2.4, 3.6), ('pur', 99.85, 100), ('dT', 5, 80)])})
_r12.WINDOWS.setdefault('x_ray_diffractometer', {}).update({'r50_study_silicon_boule': ({'dl': 3.1356}, [('err', 0, 0.4)])})
_r12.WINDOWS.setdefault('optical_workshop', {}).update({
    'r50_cut_perfect_crystal_interferometer': ({'n': 3.42}, [('Pw', -0.01, 0.01), ('e', 0, 20)])})
_r12.WINDOWS.setdefault('monte_carlo_engine', {}).update({'r50_causal_triangulation': ({'sg': 40, 'opt': 0.7}, [('err', 0, 25)])})

RESEARCH = [
    ('neutron_interferometry', 'Neutron Interferometry',
     'A neutron is a wave too. Split it with a perfect crystal, let gravity pull on one path a little more than the other, and the waves no '
     'longer meet in step: gravity, measured on a single quantum particle. Unlocks the Neutron Interferometer and the Silicon Boule.',
     'neutron_interferometer', 8, 7, 150, ['gravitational_redshift', 'crystal_growth_physics'], [('laue_pattern', 4), ('redshift_record', 2)]),
    ('analog_gravity', 'Analog Gravity',
     'A fluid flowing faster than its own sound has a horizon for sound, and the mathematics of sound on it is the mathematics of light near a '
     'black hole - Hawking radiation included. Unlocks the Analog Black Hole.',
     'analog_black_hole', 10, 7, 180, ['gravitational_waves', 'cryogenics'], [('gw_record_bbh', 2), ('test_mass', 1)]),
    ('semiclassical_gravity', 'Semiclassical Gravity',
     'Quantum fields on curved spacetime: what can be said with gravity still classical and everything else quantum. Its two tested predictions '
     '- gravity on a quantum wave, and a horizon\'s glow - point past themselves, to spacetime that is quantum too. Unlocks causal dynamical '
     'triangulations in the Monte Carlo Engine.',
     'cow_record', 11, 9, 260, ['neutron_interferometry', 'analog_gravity', 'general_relativity', 'monte_carlo_methods'],
     [('cow_record', 4), ('hawking_record', 4)]),
]

PUZZLES = {
    'neutron_interferometry': dict(
        goal='A neutron of mass m and wavelength λ is split into two paths that enclose an area A, the plane of the paths tilted by α. Gravity g '
             'changes the higher path\'s momentum; write the phase difference φ that builds up between them, from the quantum of action h.',
        name='Gravitationally induced quantum phase', lhs='φ',
        form='2 * π * m ^ 2 * g * λ * A * sin ( α ) / h ^ 2',
        expr='6.28318 * m ^ 2 * g * L * A * sin ( a ) / h ^ 2',
        ask='Find φ in radians for m = 1.675e-27 kg, g = 9.81 m/s², L = 1.4e-10 m, A = 0.001 m², a = 0.5236 rad (30°), h = 6.626e-34 J·s.',
        vars={'φ': 'phase difference (rad)', 'm': 'neutron mass (kg)', 'g': 'gravity (m/s²)', 'λ': 'wavelength', 'A': 'enclosed area (m²)',
              'α': 'tilt', 'h': 'Planck constant (J·s)', 'π': 'pi', 'L': 'wavelength (m)', 'a': 'tilt (rad)'},
        given={'m': 1.675e-27, 'g': 9.81, 'L': 1.4e-10, 'A': 0.001, 'a': 0.5236, 'h': 6.626e-34}, unit='rad',
        method='Climbing a height H costs the neutron mgH of kinetic energy, so its wavenumber falls by m²gHλ/h²·2π; over a path length L at that '
               'height, the phases part by that times L - and H·L = A·sin α.',
        distractors=['c', 'ħ', '4']),
    'analog_gravity': dict(
        goal='A horizon whose surface gravity is κ glows at a temperature T set by the quantum of action and the thermal energy scale. Write T.',
        name='Hawking temperature', lhs='T',
        form='ħ * κ / ( 2 * π * k _ B )',
        expr='h * K / ( 6.28318 * k ) * 1000000000',
        ask='Find T in nK for a sonic horizon with K = 4000 s⁻¹ (h = 1.0546e-34 J·s for ħ, k = 1.381e-23 J/K).',
        vars={'T': 'temperature (nK)', 'ħ': 'reduced Planck constant', 'κ': 'surface gravity', 'k': 'Boltzmann constant', 'B': 'Boltzmann',
              'π': 'pi', 'h': 'ħ (J·s)', 'K': 'surface gravity (1/s)'},
        given={'h': 1.0546e-34, 'K': 4000, 'k': 1.381e-23}, unit='nK',
        method='Near the horizon, modes are stretched exponentially at the rate κ; seen from far away that stretching looks like a thermal '
               'spectrum at kT = ħκ/2π.',
        distractors=['c', 'G', '4']),
    'semiclassical_gravity': dict(
        goal='Quantum effects of gravity become as strong as gravity itself at a length built only from the quantum of action ħ, gravity\'s '
             'constant G and the speed of light c. Write that length l.',
        name='Planck length', lhs='l',
        form='sqrt ( ħ * G / c ^ 3 )',
        expr='( h * G / c ^ 3 ) ^ 0.5 * 1000000000000000000000000000000000000',
        ask='Find l in units of 10⁻³⁶ m (h = 1.0546e-34 for ħ, G = 6.674e-11, c = 2.998e8).',
        vars={'l': 'Planck length (×10⁻³⁶ m)', 'ħ': 'reduced Planck constant', 'G': 'gravitational constant', 'c': 'speed of light',
              'h': 'ħ (J·s)'},
        given={'h': 1.0546e-34, 'G': 6.674e-11, 'c': 2.998e8}, unit='×10⁻³⁶ m',
        method='Only one combination of ħ, G and c has the dimensions of a length. At that size a particle\'s quantum wavelength equals the '
               'radius of the black hole its energy would make.',
        distractors=['π', '2', 'k']),
}

NI, AB, CP, XD, OF, MCE = 'gravity_interferometry', 'analog_horizon', 'crystal_pulling', 'xray_diffraction', 'optical_finishing', 'monte_carlo'
RECIPES = [
    # (name, type, item inputs, fluid inputs, item outputs, FE/t, ticks, research, insight, catalysts)
    ('cow', NI, [('radium_salt', 1), ('minecraft:paper', 1)], [], [('cow_record', 1)], 1024, 1200, 'neutron_interferometry', 20,
     ['perfect_crystal_interferometer']),
    ('hawking', AB, [('minecraft:paper', 1)], [('bose_einstein_condensate', 250)], [('hawking_record', 1)], 2048, 1600, 'analog_gravity', 25, []),
    ('grow_silicon', CP, [('fused_silica', 2), ('minecraft:coal', 2)], [], [('silicon_boule', 1)], 512, 800, 'neutron_interferometry', 0, []),
    ('study_silicon_boule', XD, [('silicon_boule', 1), ('photographic_plate', 1)], [], [('silicon_boule', 1), ('laue_pattern', 3)], 256, 200,
     'crystallography', 3, []),
    ('cut_perfect_crystal_interferometer', OF, [('silicon_boule', 2), ('laue_pattern', 1)], [], [('perfect_crystal_interferometer', 1)], 256, 1200,
     'neutron_interferometry', 0, []),
    # causal dynamical triangulations: quantum spacetime as a sum over random triangulated histories
    ('causal_triangulation', MCE, [('cow_record', 4), ('hawking_record', 4), ('metric_tensor', 2)], [], [('spin_foam_lattice', 1)], 4096, 2400,
     'semiclassical_gravity', 40, ['random_number_chip']),
]
CRAFTING = [
    ('neutron_guide', ['LGL', 'G G', 'LGL'], {'L': 'lead_shielding_plate', 'G': 'minecraft:glass'}, 'neutron_guide', 4),
    ('tilt_table', ['III', 'PRP', 'III'], {'I': 'minecraft:iron_ingot', 'P': 'minecraft:piston', 'R': 'minecraft:redstone'}, 'tilt_table', 4),
    ('crystal_interferometer_mount', ['IXI', 'X X', 'IXI'], {'I': 'minecraft:iron_ingot', 'X': 'perfect_crystal_interferometer'},
     'crystal_interferometer_mount', 2),
    ('helium3_detector', ['IGI', 'GTG', 'IGI'], {'I': 'minecraft:iron_ingot', 'G': 'minecraft:glass', 'T': 'geiger_tube'}, 'helium3_detector', 1),
    ('bec_trap', ['MFM', 'F F', 'MFM'], {'M': 'neodymium_magnet', 'F': 'fused_silica'}, 'bec_trap', 1),
    ('step_laser', ['L', 'F', 'I'], {'L': 'laser_head', 'F': 'fused_silica_lens', 'I': 'minecraft:iron_ingot'}, 'step_laser', 1),
]
LANG = {}


def _write_research(g):
    import round42
    saved = round42.RESEARCH
    round42.RESEARCH = RESEARCH
    try:
        round42._write_research(g)
    finally:
        round42.RESEARCH = saved


def _write_puzzles():
    p = os.path.join(HERE, 'puzzles2', 'group_r50.json')
    data = json.dumps(PUZZLES, ensure_ascii=False, indent=1)
    if not os.path.exists(p) or open(p, encoding='utf-8').read() != data:
        with open(p, 'w', encoding='utf-8') as f:
            f.write(data)


def _recipes(g):
    REC, w = g['REC'], g['w']
    for name, typ, ins, fins, outs, fe, t, res, insight, cats in RECIPES:  # no fluid outputs here
        g['mr']('r50_' + name, typ, ins, fins, outs, [], energy=fe, duration=t, research=res, catalysts=cats)
        p = os.path.join(REC, typ, 'r50_' + name + '.json')
        r = json.load(open(p))
        r['unscaled'] = True
        if insight: r['insight_per_run'] = insight
        w(p, r)
    for name, pat, key, res, n in CRAFTING:
        g['shaped']('r50_' + name, pat, key, res, n)


def apply(g):
    g['LANG'].update(LANG)
    _write_puzzles()
    _write_research(g)
    _recipes(g)


def apply_late(g):
    import round42
    round42._extend_tag(g, os.path.join(g['RES'], 'data', 'minecraft', 'tags', 'block', 'mineable', 'pickaxe.json'),
                        [f'{NS}:{b["id"]}' for b in BLOCKS])


def finish(g):
    te = g['text_entry']
    te('spacetime', 'r50_cow', 'Gravity on a Quantum Wave', 'neutron_interferometer', [
        'In 1975 Colella, Overhauser and Werner split single neutrons with a perfect silicon crystal and joined them again - and tilted the '
        'whole thing. One path ran a few centimetres higher than the other; gravity slowed the neutron on it; the two waves came back out of '
        'step. Gravity, acting on a quantum wave: $(thing)Δφ = 2πm²gλA·sin α/h²$().',
        'Grow a $(item)Silicon Boule$() (fused silica and coal in the Crystal Growth Furnace), study it for its Laue Pattern, and cut two into a '
        '$(item)Perfect Crystal Interferometer$() - three blades still joined at the base, their lattices aligned to a fraction of an atom.',
        'The $(item)Neutron Interferometer$() runs on a radium-beryllium source. Choose the wavelength and tilt it until gravity has turned the '
        'phase at least 30 radians and lands it on a bright fringe; count until the phase is known to 0.1 rad. Each record is a great deal of '
        'Insight.'],
        adv='neutron_interferometry', sort=43, spot=['neutron_interferometer', 'perfect_crystal_interferometer', 'silicon_boule'])
    te('spacetime', 'r50_hawking', 'A Horizon in a Bottle', 'analog_black_hole', [
        'Sound in a fluid flowing faster than sound cannot make headway upstream. Where the flow crosses its own speed of sound there is a '
        'horizon for sound - and the mathematics of sound near it is the mathematics of light near a black hole. Hawking radiation included.',
        'The $(item)Analog Black Hole$() makes the flow out of $(fluid)Bose-Einstein Condensate$(): it streams toward a laser step, subsonic, and '
        'falls over it, thinned and fast - supersonic. Set the density (sound speed grows as its square root), the flow and the step depth so '
        'that both hold.',
        'At the horizon phonon pairs appear, one carried off downstream, its partner escaping up: a glow at $(thing)T_H = ħκ/2πk_B$(), a few '
        'nanokelvin. The condensate itself must be colder than that to see it, and the correlations summed over many shots. Nothing but '
        'Bose-Einstein Condensate is cold enough as the coolant.'],
        adv='analog_gravity', sort=44, spot=['analog_black_hole', 'bec_trap', 'hawking_record'])
    te('spacetime', 'r50_unification', 'Toward Unification', 'cow_record', [
        'Two experiments have put quantum theory and gravity into one equation: gravity bending a neutron\'s phase, and a horizon glowing with '
        'quantum pairs. Both are $(thing)semiclassical$(): the quantum world on a smooth, classical spacetime.',
        'Beyond them, spacetime itself must be quantum. One way to compute it is to sum over every way of building spacetime out of simple '
        'pieces - at random. With Semiclassical Gravity, the Monte Carlo Engine runs these $(thing)causal dynamical triangulations$(): '
        'four records of each experiment and two Metric Tensors make a $(item)Spin Foam Lattice$().',
        'From here the road leads on to quantum gravity itself, and to the strings and singularities of the Codex\'s far end.'],
        adv='semiclassical_gravity', sort=45, spot=['cow_record', 'hawking_record', 'spin_foam_lattice'])
