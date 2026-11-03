"""
Round 54 (1.13.0, "Horizons"): the rest of the 1.1 design's bridge between quantum mechanics and general relativity.

Unruh Effect (the Unruh Accelerator: an accelerated observer sees the vacuum as warm), Hawking Radiation (the Micro Black
Hole Evaporator, a generator: small holes burn hot and fast), Black Hole Thermodynamics, the Information Paradox (the Page
Curve Analyzer), Loop Quantum Gravity (the Spin Network Loom), Causal Set Theory, ER = EPR (the ER=EPR Weaver: entangled
pairs woven into wormhole threads), Asymptotic Safety, the Page Curve and Islands, and the AdS/CFT Correspondence.
"""
import json
import math
import os

import shapes
import round12 as _r12
from round12 import P as _P, D as _D, C as _C, V as _V
import kit
from kit import I, B, FL

NS = 'theoreticalsciences'
M = NS + ':'

ITEMS = [
    I('unruh_thermometer', 'Unruh Thermometer', 'probe', '#202838', '#ffb060',
      'A detector that clicks when it is accelerated through empty space: to it, the vacuum is a warm bath, T = ħa/2πck.'),
    I('firewall_shard', 'Firewall Shard', 'shard', '#401008', '#ff6020',
      'If information escapes a black hole, the entanglement across its horizon must break - and a broken horizon would burn whatever '
      'falls in. A shard of that hypothetical wall of fire.'),
    I('soft_hair_filament', 'Soft Hair Filament', 'string', '#202030', '#e0e0ff',
      'Zero-energy excitations on a horizon that remember what fell in: a black hole may not be quite as bald as it seems.'),
    I('planck_area_tile', 'Planck Area Tile', 'hexgrid', '#101020', '#a0c0ff',
      'The smallest area there is, in loop quantum gravity: 8πγℓₚ²√(j(j+1)), where a spin network\'s link pierces a surface.'),
    I('spin_network_node', 'Spin Network Node', 'lattice', '#101828', '#c0e0ff',
      'A node of a spin network: a grain of volume, its faces the links that join it to its neighbours. Space is built from these.'),
    I('causal_set_seed', 'Causal Set Seed', 'dots', '#181020', '#e0c0ff',
      'Events scattered at random into spacetime, with nothing but which came before which. From that order alone, geometry grows.'),
    I('er_bridge_thread', 'ER Bridge Thread', 'string', '#100818', '#ff80ff',
      'Two entangled particles, and a wormhole too small to pass through joining them: Einstein-Rosen equals Einstein-Podolsky-Rosen.'),
    I('fixed_point_lattice', 'Fixed-Point Lattice', 'lattice', '#202018', '#ffe080',
      'Gravity\'s strength, run to ever shorter distances, settles on a fixed value instead of blowing up: a theory safe all the way down.'),
    I('page_curve_record', 'Page Curve Record', 'codex', '#081018', '#80e0ff',
      'The entropy of a black hole\'s radiation: rising as it evaporates, until halfway - then falling back to zero. Information comes out. '
      'Carried in the off hand, it puts your hotbar back in order (no Information Loss).'),
    I('island_fragment', 'Island Fragment', 'shard', '#102018', '#80ffc0',
      'A region inside the horizon that belongs, after the Page time, to the radiation outside: the island that makes the curve turn over.'),
    I('holographic_screen', 'Holographic Screen', 'holo', '#101828', '#a0f0ff',
      'A surface that holds everything in the volume behind it, one bit per four Planck areas.'),
    I('boundary_cft_chip', 'Boundary CFT Chip', 'chip', '#181030', '#c0a0ff',
      'A quantum field theory without gravity, on a boundary - which is the same theory as gravity in the space inside it.'),
]
BLOCKS = [
    B('unruh_ring_casing', 'Unruh Ring Casing', 'casing', '#202838', '#ffb060'),
    B('acceleration_cavity', 'Acceleration Cavity', 'rfcavity', '#302820', '#ffc080', light=4),
    B('bending_magnet', 'Bending Magnet', 'dipole', '#203048', '#80b0ff', light=3),
    B('unruh_detector', 'Unruh Detector', 'detector', '#302818', '#ffe0a0', light=5),
    B('evaporator_shield', 'Evaporator Shield', 'plating', '#303030', '#808890'),
    B('horizon_cage', 'Horizon Cage', 'riftframe', '#100810', '#ffe0a0', light=6),
    B('weaver_casing', 'Weaver Casing', 'casing2', '#180818', '#ff80ff'),
    B('entanglement_node', 'Entanglement Node', 'emitterblock', '#201030', '#ffa0ff', light=6),
    B('pair_source', 'Entangled Pair Source', 'injector', '#201030', '#e0c0ff', light=5),
]
FLUIDS = [
    FL('quantum_spacetime_condensate', 'Quantum Spacetime Condensate', '#080810', '#a0c0ff', light=8, temp=300, density=500, viscosity=2500,
       art='exotic'),
]

KEY_ADD = {
    'ј': ['unruh_ring_casing'], 'љ': ['unruh_ring_casing', 'interface_port'], 'ћ': ['acceleration_cavity'], 'ќ': ['bending_magnet'],
    'ў': ['unruh_detector'],
    'џ': ['evaporator_shield'], 'ѣ': ['evaporator_shield', 'interface_port'], 'ѧ': ['horizon_cage'],
    'ѫ': ['weaver_casing'], 'ѳ': ['weaver_casing', 'interface_port'], 'ѵ': ['entanglement_node'], 'ѱ': ['pair_source'],
}


# =================================================================================== SHAPES
def unruh_accelerator():
    """A ring: beam pipe round a circle on supports, bending magnets above and below it, two accelerating cavities, and
    the detector beside the ring that the beam's own acceleration warms."""
    s = shapes.Shape(15, 5, 15)
    c, R = 7, 6
    for x in range(15):
        for z in range(15):
            d = math.hypot(x - c, z - c)
            if abs(d - R) < 0.55:
                s.put(x, 2, z, 'Λ')
    for k in range(16):
        a = k * math.pi / 8
        x, z = c + math.cos(a) * R, c + math.sin(a) * R
        if k % 2 == 0:
            s.put(x, 1, z, 'ќ')
            s.put(x, 3, z, 'ќ')
            s.put(x, 0, z, 'ј')
        if k in (4, 12):
            s.put(x, 2, z, 'ћ')
    for x in range(15):
        for z in range(15):
            if abs(math.hypot(x - c, z - c) - R) < 0.55 and (x, 0, z) not in s.v:
                s.put(x, 0, z, 'ј')
    s.put(c, 2, c + R + 1, 'ў')
    s.put(c, 1, c + R + 1, 'ј')
    s.put(c, 0, c + R + 1, 'ј')
    s.center = (7.0, 2.0, 7.0)
    s.ports('ј', 'љ', 4, y=0)
    s.v[(c + R, 0, c)] = 'ј'
    s.v[(c, 0, c - R)] = 'C'
    s.ctrl = (c, 0, c - R)
    return s


def micro_black_hole_evaporator():
    """A lead sphere round a hole too small to see, held in a cage at its centre, with a ring of windows at its equator to
    watch it glow through."""
    s = shapes.Shape(11, 11, 11)
    c = 5
    for x in range(11):
        for y in range(11):
            for z in range(11):
                r = math.dist((x, y, z), (c, c, c))
                if 4.3 <= r <= 5.4:
                    s.put(x, y, z, 'G' if y == c and (x + z) % 2 == 0 else 'џ')
                elif r < 4.3:
                    s.put(x, y, z, '.')
    for x in (c - 1, c + 1):
        for y in (c - 1, c + 1):
            for z in (c - 1, c + 1):
                s.put(x, y, z, 'ѧ')                                       # the cage's corners
    for dy in (-2, -3):
        s.put(c, c + dy, c, 'ѧ')                                          # its stalk
    s.center = (5.0, 5.0, 5.0)
    s.ports('џ', 'ѣ', 4, y=1)
    s.v[(c, 1, 1)] = 'C'
    s.ctrl = (c, 1, 1)
    return s


def er_epr_weaver():
    """Two towers - the two mouths - far apart, an entangled-pair source between them, and an optical bridge carrying each
    pair's halves out to the two ends."""
    s = shapes.Shape(15, 6, 5)
    zc = 2
    s.box(0, 0, 0, 14, 0, 4, 'ѫ')
    for x0 in (0, 12):
        s.box(x0, 1, 1, x0 + 2, 3, 3, 'ѫ')
        s.put(x0 + 1, 4, zc, 'ѵ')                                         # the entanglement nodes
        s.put(x0 + 1, 5, zc, 'G')
    for x in range(3, 12):
        s.put(x, 3, zc, 'G')                                              # the bridge
        if x % 2 == 0: s.put(x, 2, zc, 'ѫ'); s.put(x, 1, zc, 'ѫ')
    s.put(7, 1, zc, 'ѱ')                                                  # the pair source
    s.put(7, 2, zc, 'ѱ')
    s.center = (7.0, 4.0, 2.0)
    s.ports('ѫ', 'ѳ', 4, y=0)
    s.v[(7, 0, 0)] = 'C'
    s.ctrl = (7, 0, 0)
    return s


shapes.BUILDERS.update({'unruh_accelerator': unruh_accelerator, 'micro_black_hole_evaporator': micro_black_hole_evaporator,
                        'er_epr_weaver': er_epr_weaver})

MACHINE_TEXT = {
    'unruh_accelerator': ('Unruh Accelerator', 'A storage ring whose electrons are accelerated so hard round their curve that, to them, the '
                                               'empty vacuum is warm.'),
    'micro_black_hole_evaporator': ('Micro Black Hole Evaporator', 'A black hole lighter than a mountain, held in a cage and fed as fast '
                                                                   'as it evaporates: it glows with Hawking radiation hotter than any star.'),
    'er_epr_weaver': ('ER=EPR Weaver', 'Shares the halves of entangled pairs between two distant mouths - weaving, if Maldacena and Susskind '
                                       'are right, a wormhole between them.'),
    'spin_network_loom': ('Spin Network Loom', 'Weaves quanta of area and volume into a spin network: space, a grain at a time.'),
    'page_curve_analyzer': ('Page Curve Analyzer', 'Follows the entropy of a black hole\'s radiation as it evaporates - and finds the '
                                                   'information coming back out.'),
}
MACHINE_CAT = {k: 'spacetime' for k in MACHINE_TEXT}
MACHINE_RESEARCH = {'unruh_accelerator': 'unruh_effect', 'micro_black_hole_evaporator': 'hawking_radiation', 'er_epr_weaver': 'er_epr',
                    'spin_network_loom': 'loop_quantum_gravity', 'page_curve_analyzer': 'information_paradox'}
TIER = {'unruh_accelerator': 3, 'micro_black_hole_evaporator': 4, 'er_epr_weaver': 4, 'spin_network_loom': 4, 'page_curve_analyzer': 4}
_S = 'spacetime_casing'
MC = {
    'unruh_accelerator': (['BCB', 'DSD', 'BCB'], {'B': 'bending_magnet', 'C': 'acceleration_cavity', 'D': 'unruh_detector', 'S': _S}),
    'micro_black_hole_evaporator': (['LCL', 'CSC', 'LCL'], {'L': 'evaporator_shield', 'C': 'horizon_cage', 'S': _S}),
    'er_epr_weaver': (['NPN', 'WSW', 'WWW'], {'N': 'entanglement_node', 'P': 'pair_source', 'W': 'weaver_casing', 'S': _S}),
    'spin_network_loom': (['PNP', 'MSM', 'PNP'], {'P': 'planck_shard', 'N': 'metric_tensor', 'M': 'spin_foam_lattice', 'S': _S}),
    'page_curve_analyzer': (['HQH', 'ESE', 'HQH'], {'H': 'hawking_quanta', 'Q': 'quantum_processor', 'E': 'entangled_photon_pair', 'S': _S}),
}
EXTRA = {
    'unruh_accelerator': 'Set the beam\'s Lorentz factor and the bending field. An electron of γ in a field B circles at r = γmc/eB with '
                         'acceleration a = γ²c²/r - and sees the vacuum at T = ħa/2πck. The faster and tighter, the hotter; but synchrotron '
                         'radiation drains the beam as γ³B, and the cavities can only replace so much. Above 2 T the magnets saturate.',
    'micro_black_hole_evaporator': 'Set the hole\'s mass and how fast it is fed. It radiates at T = ħc³/(8πGMk): lighter is hotter, and the '
                                   'power grows as 1/M². Feed it just what it loses: feed it less and it shrinks, runs away and explodes; more '
                                   'and it swells into the feed. Lighter than 6×10⁸ kg, its gamma rays are too hot for the shield.',
    'er_epr_weaver': 'Set how many entangled pairs a second it makes, and how far apart its mouths are taken. Fidelity falls with the '
                     'distance and with the machine\'s own temperature - and every pair heats it. Keep it cold.',
    'spin_network_loom': 'Weaves Planck Shards into Planck Area Tiles, tiles into Spin Network Nodes, and nodes with liquid spacetime into '
                         'Quantum Spacetime Condensate.',
    'page_curve_analyzer': 'Follows Hawking quanta through an evaporation: firewall shards and soft hair, the Page curve, islands, and the '
                           'Bekenstein bound.',
}

_r12.model('unruh_accelerator', 'Unruh Accelerator',
           'An electron of Lorentz factor γ, bent by a field B, circles at r = γmc/eB with proper acceleration a = γ²c²/r. An accelerated '
           'observer sees the vacuum as a thermal bath at the Unruh temperature T = ħa/(2πck_B): about 214 K per thousand of γ per tesla, '
           'here. Synchrotron radiation drains the beam as γ³B; the cavities can replace up to 500 kW.',
           'Beam energy jitter: γ varies by ±1%·d.',
           [_P('g', 'Lorentz factor', '×1000', 0.05, 50, 0.001, 1, 'How relativistic the electrons are.'),
            _P('Bb', 'Bending field', 'T', 0.01, 3, 0.001, 0.5, 'The field that bends the beam round the ring.')],
           [_D('ge', 'Lorentz factor', '×1000', 'g*(1 + 0.01*d)', 'γ', 'The beam\'s actual Lorentz factor.'),
            _D('Tu', 'Unruh temperature', 'K', '214*ge*Bb', 'T = ħa/(2πck_B)', 'How warm the vacuum looks to the beam.'),
            _D('Ps', 'Synchrotron loss', 'kW', '0.1*ge^3*Bb', 'P ∝ γ³B', 'Power the beam radiates away.')],
           [_C('rf', 'stop', '500 - Ps', 'The cavities cannot replace what the beam radiates', 'P < 500 kW'),
            _C('saturate', 'wear', '2 - Bb', 'The bending magnets are saturating', 'B < 2 T')],
           rate='clamp(0.5 + 0.2*g, 0.25, 2)', rate_formula='rate = 0.5 + 0.2·γ/1000', rate_desc='A more energetic beam works faster.',
           heat='0.001*Ps', idle_heat='0', kappa=40, heat_capacity=60)

_r12.model('micro_black_hole_evaporator', 'Micro Black Hole Evaporator',
           'A black hole of mass M radiates as a black body at the Hawking temperature T = ħc³/(8πGMk) - 1.2×10¹⁴ K for 10⁹ kg - and its power '
           'grows as 1/M², 356 TW at 10⁹ kg. Fed exactly what it loses, it holds its mass; fed less, it shrinks, burns hotter, and runs away.',
           'The feed is uneven: by ±2%·d.',
           [_P('Mb', 'Hole mass', '10⁹ kg', 0.5, 100, 0.001, 10, 'The mass of the hole.'),
            _P('fd', 'Feed balance', '', 0.5, 1.5, 0.0001, 1, 'Mass fed in, as a fraction of what it radiates away.')],
           [_D('TH', 'Hawking temperature', '10¹³ K', '12.27/Mb', 'T = ħc³/(8πGMk)', 'How hot the hole glows.'),
            _D('Ph', 'Hawking power', 'TW', '356/Mb^2', 'P ∝ 1/M²', 'What it radiates.'),
            _D('fe', 'Feed balance', '', 'fd*(1 + 0.02*d)', '', 'The feed it actually gets.')],
           [_C('evaporate', 'damage', 'fe - 0.95', 'The hole is shrinking faster than it is fed - it is running away', 'feed > 95%'),
            _C('swell', 'stop', '1.05 - fe', 'The hole is growing into its own feed', 'feed < 105%'),
            _C('gamma', 'damage', '20 - TH', 'Its radiation is too hot for the shield', 'T < 2×10¹⁴ K')],
           rate='clamp(Ph/Pref, 0, 2)', rate_formula='output = P / P_rated', rate_desc='Power follows the Hawking power.',
           heat='0.01*Ph', idle_heat='0', rvars=[_V('Pref', 'Rated power (TW)', 10)], kappa=60, heat_capacity=200)

_r12.model('er_epr_weaver', 'ER=EPR Weaver',
           'Entangled pairs are made in the middle and their halves sent to the two mouths, a distance L apart. Their fidelity falls as '
           'e^(−L/50 km) and with the weaver\'s temperature, (1 − T/1000 K) - and each pair a second warms it.',
           'Path noise: the coherence length varies by ±5%·d.',
           [_P('Np', 'Pair rate', 'pairs/s', 1, 1000, 0.1, 100, 'How many entangled pairs a second it makes.'),
            _P('L', 'Mouth separation', 'km', 0.1, 100, 0.001, 10, 'How far apart the two ends are taken.')],
           [_D('Fd', 'Fidelity', '', 'exp(-L/(50*(1 + 0.05*d)))*pos(1 - T/1000)', 'F = e^(−L/ℓ)(1 − T/1000 K)', 'How well the pairs stay entangled.')],
           [],
           rate='clamp(Np/200, 0.25, 2)', rate_formula='rate = N/200', rate_desc='More pairs, faster weaving.',
           heat='0.005*Np', idle_heat='0', kappa=40, heat_capacity=60)

_r12.WINDOWS['unruh_accelerator'] = {
    'r54_unruh_thermometer': ({}, [('Tu', 1, 10)]),
    'r54_unruh_photons': ({}, [('Tu', 5000, 1e9)]),
}
_r12.WINDOWS['micro_black_hole_evaporator'] = {
    'r54_evaporate_dust': ({'Pref': 10}, []),
    'r54_hawking_quanta': ({'Pref': 10}, [('TH', 1, 1e9)]),
}
_r12.WINDOWS['er_epr_weaver'] = {'r54_er_bridge_thread': ({}, [('Fd', 0.9, 1)])}
_r12.WINDOWS.setdefault('monte_carlo_engine', {}).update({
    'r54_causal_set_seed': ({'sg': 20, 'opt': 0.6}, [('err', 0, 40)]),
    'r54_fixed_point_lattice': ({'sg': 30, 'opt': 0.7}, [('err', 0, 20)]),
})

# =================================================================================== RESEARCH
RESEARCH = kit.place([
    ('unruh_effect', 'Unruh Effect',
     'Empty space is empty only for an observer who is not accelerating. Accelerate, and the vacuum fills with a warm glow - at a temperature '
     'proportional to the acceleration. Unlocks the Unruh Accelerator.',
     'unruh_thermometer', None, None, 200, ['semiclassical_gravity'], [('hall_bar_chip', 2), ('test_mass', 1)]),
    ('hawking_radiation', 'Hawking Radiation',
     'Near a horizon the same happens without anyone accelerating: a black hole glows, at a temperature inversely proportional to its mass, '
     'and slowly evaporates. Unlocks the Micro Black Hole Evaporator.',
     'hawking_quanta', None, None, 220, ['semiclassical_gravity', 'black_hole_era'], [('hawking_quanta', 2), ('hawking_record', 2)]),
    ('black_hole_thermodynamics', 'Black Hole Thermodynamics',
     'A black hole has a temperature, so it has an entropy - and that entropy is its horizon\'s area in Planck units, divided by four. The '
     'most information any region can hold is set by its surface, not its volume.',
     'bekenstein_crystal', None, None, 260, ['hawking_radiation'], [('hawking_quanta', 4), ('bekenstein_crystal', 1)]),
    ('information_paradox', 'Information Paradox',
     'If a black hole evaporates into thermal radiation, whatever fell in is lost - and quantum mechanics forbids that. Something must give. '
     'Unlocks the Page Curve Analyzer.',
     'firewall_shard', None, None, 280, ['black_hole_thermodynamics'], [('hawking_quanta', 8), ('entangled_photon_pair', 4)]),
    ('loop_quantum_gravity', 'Loop Quantum Gravity',
     'Quantise geometry itself: areas and volumes come in discrete steps, carried by a spin network. Space is woven, not continuous. Unlocks '
     'the Spin Network Loom.',
     'spin_network_node', None, None, 280, ['quantum_gravity', 'black_hole_thermodynamics'], [('spin_foam_lattice', 1), ('metric_tensor', 2)]),
    ('causal_set_theory', 'Causal Set Theory',
     'Spacetime as a set of events and nothing else but which can affect which. Scatter them at random, and a manifold appears: order plus '
     'number gives geometry.',
     'causal_set_seed', None, None, 260, ['quantum_gravity', 'set_theory'], [('normal_distribution_crystal', 4), ('metric_tensor', 2)]),
    ('er_epr', 'ER = EPR',
     'Maldacena and Susskind\'s conjecture: two entangled particles are joined by a wormhole, too small to pass through. Entanglement is '
     'geometry. Unlocks the ER=EPR Weaver.',
     'er_bridge_thread', None, None, 300, ['quantum_entanglement', 'traversable_wormholes'], [('entangled_photon_pair', 8), ('exotic_matter_crystal', 2)]),
    ('asymptotic_safety', 'Asymptotic Safety',
     'Gravity\'s coupling, followed to ever shorter distances, may settle onto a fixed point instead of blowing up - and then general '
     'relativity is a complete quantum theory as it stands.',
     'fixed_point_lattice', None, None, 300, ['loop_quantum_gravity', 'quantum_field_theory'], [('spin_network_node', 2), ('causal_set_seed', 2)]),
    ('page_curve', 'Page Curve and Islands',
     'The radiation\'s entropy rises as a black hole evaporates, then - after the Page time - falls back to zero: the information comes out. '
     'Islands inside the horizon, counted as part of the radiation, make it turn over.',
     'page_curve_record', None, None, 340, ['information_paradox', 'er_epr'],
     [('firewall_shard', 2), ('soft_hair_filament', 2), ('er_bridge_thread', 1)]),
    ('ads_cft', 'AdS/CFT Correspondence',
     'A theory of gravity in a curved space and a quantum field theory on its boundary, without gravity, are the same theory in two '
     'languages. The universe as a hologram, made exact.',
     'holographic_screen', None, None, 400, ['holographic_principle', 'page_curve'],
     [('page_curve_record', 2), ('island_fragment', 1), ('holographic_matrix', 1)]),
])

PUZZLES = {
    'unruh_effect': dict(
        goal='An observer accelerating at a through empty space sees it filled with thermal radiation. Write its temperature T, from ħ, the '
             'speed of light c and Boltzmann\'s constant k.',
        name='Unruh temperature', lhs='T',
        form='ħ * a / ( 2 * π * c * k )',
        expr='h * a / ( 6.28318 * c * k )',
        ask='Find T in kelvin for a = 2.47e20 m/s² (h = 1.0546e-34 for ħ, c = 2.998e8, k = 1.381e-23).',
        vars={'T': 'temperature (K)', 'ħ': 'reduced Planck constant', 'a': 'acceleration (m/s²)', 'π': 'pi', 'c': 'speed of light',
              'k': 'Boltzmann constant', 'h': 'ħ (J·s)'},
        given={'h': 1.0546e-34, 'a': 2.47e20, 'c': 2.998e8, 'k': 1.381e-23}, unit='K',
        method='The accelerated observer\'s horizon sits a distance c²/a behind them; seen from their frame the vacuum modes are thermal at '
               'that horizon\'s surface gravity.',
        distractors=['G', 'M', '4']),
    'hawking_radiation': dict(
        goal='A black hole of mass M glows as a black body. Write its temperature T, from ħ, c, gravity\'s constant G and Boltzmann\'s k.',
        name='Hawking temperature of a mass', lhs='T',
        form='ħ * c ^ 3 / ( 8 * π * G * M * k )',
        expr='h * c ^ 3 / ( 25.1327 * G * M * k )',
        ask='Find T in kelvin for M = 1e12 kg (h = 1.0546e-34 for ħ, c = 2.998e8, G = 6.674e-11, k = 1.381e-23).',
        vars={'T': 'temperature (K)', 'ħ': 'reduced Planck constant', 'c': 'speed of light', 'π': 'pi', 'G': 'gravitational constant',
              'M': 'mass (kg)', 'k': 'Boltzmann constant', 'h': 'ħ (J·s)'},
        given={'h': 1.0546e-34, 'c': 2.998e8, 'G': 6.674e-11, 'M': 1e12, 'k': 1.381e-23}, unit='K',
        method='The surface gravity of a Schwarzschild horizon is κ = c⁴/4GM; put it in kT = ħκ/2πc.',
        distractors=['2', 'a', 'σ']),
    'black_hole_thermodynamics': dict(
        goal='A horizon of area A carries an entropy S, counted in units of Boltzmann\'s constant, set only by c, G and ħ. Write S.',
        name='Bekenstein-Hawking entropy', lhs='S',
        form='A * c ^ 3 / ( 4 * G * ħ )',
        expr='A * c ^ 3 / ( 4 * G * h ) / u',
        ask='Find S in units of 10⁷⁰ k (u = 1e70) for a solar-mass hole, A = 1.096e8 m² (c = 2.998e8, G = 6.674e-11, h = 1.0546e-34 for ħ).',
        vars={'S': 'entropy (10⁷⁰ k)', 'A': 'horizon area (m²)', 'c': 'speed of light', 'G': 'gravitational constant', 'ħ': 'reduced Planck constant',
              'h': 'ħ (J·s)', 'u': '10⁷⁰'},
        given={'A': 1.096e8, 'c': 2.998e8, 'G': 6.674e-11, 'h': 1.0546e-34, 'u': 1e70}, unit='×10⁷⁰ k',
        method='Integrate dS = dE/T with E = Mc² and the Hawking temperature: S grows as M², which is the area.',
        distractors=['π', '2', 'k']),
    'information_paradox': dict(
        goal='A black hole of mass M evaporates completely in a time t that grows as the cube of its mass, from G, ħ and c. Write t.',
        name='Evaporation time', lhs='t',
        form='5120 * π * G ^ 2 * M ^ 3 / ( ħ * c ^ 4 )',
        expr='5120 * 3.14159 * G ^ 2 * M ^ 3 / ( h * c ^ 4 ) / u',
        ask='Find t in billions of years (u = 3.156e16 s) for M = 1e11 kg (G = 6.674e-11, h = 1.0546e-34 for ħ, c = 2.998e8).',
        vars={'t': 'lifetime (Gyr)', 'π': 'pi', 'G': 'gravitational constant', 'M': 'mass (kg)', 'ħ': 'reduced Planck constant', 'c': 'speed of light',
              'h': 'ħ (J·s)', 'u': 's per Gyr'},
        given={'G': 6.674e-11, 'M': 1e11, 'h': 1.0546e-34, 'c': 2.998e8, 'u': 3.156e16}, unit='Gyr',
        method='Power radiated goes as 1/M² (Stefan-Boltzmann at T ∝ 1/M over an area ∝ M²); dM/dt ∝ −1/M² integrates to t ∝ M³.',
        distractors=['k', '2', 'σ']),
    'loop_quantum_gravity': dict(
        goal='Where a spin network\'s link of spin j crosses a surface it gives that surface an area A, in units of the Planck area l, '
             'scaled by the Immirzi parameter γ. Write A.',
        name='Quantised geometry', lhs='A',
        form='8 * π * γ * l ^ 2 * sqrt ( j * ( j + 1 ) )',
        expr='8 * 3.14159 * g * sqrt ( j * ( j + 1 ) )',
        ask='Find A in Planck areas for the smallest step, j = 0.5, with g = 0.2375 for γ.',
        vars={'A': 'area (Planck areas)', 'π': 'pi', 'γ': 'Immirzi parameter', 'l': 'Planck length', 'j': 'spin', 'g': 'γ'},
        given={'g': 0.2375, 'j': 0.5}, unit='ℓₚ²',
        method='The area operator acts on each link like the length of an angular momentum: its eigenvalues are √(j(j+1)).',
        distractors=['2', 'c', 'h']),
    'causal_set_theory': dict(
        goal='Events are scattered at random with density ρ through spacetime. Write the chance P that a region of volume V holds none at all.',
        name='Poisson sprinkling', lhs='P',
        form='1 / exp ( ρ * V )',
        expr='1 / exp ( p * V )',
        ask='Find P for p = 0.5 events per unit volume in a region of V = 3.',
        vars={'P': 'chance of an empty region', 'ρ': 'density', 'p': 'density', 'V': 'volume'},
        given={'p': 0.5, 'V': 3}, unit='',
        method='A Poisson process with mean ρV gives n events with probability (ρV)ⁿe^(−ρV)/n!; put n = 0.',
        distractors=['2', 'n', 'π']),
    'er_epr': dict(
        goal='Each maximally entangled pair of qubits shares one unit of entanglement entropy. Write the entropy S of n such pairs, in nats.',
        name='Bell-pair counting', lhs='S',
        form='n * ln ( 2 )',
        expr='n * ln ( 2 )',
        ask='Find S for n = 10 pairs.',
        vars={'S': 'entropy (nats)', 'n': 'pairs'},
        given={'n': 10}, unit='nats',
        method='Each half of a Bell pair is in a mixed state, ½ and ½: its entropy is −Σ p ln p = ln 2.',
        distractors=['e', 'π', '4']),
    'asymptotic_safety': dict(
        goal='Newton\'s constant G, probed at momentum k, weakens with a coefficient ω. Write the dimensionless strength g = G k² it runs to.',
        name='Running coupling', lhs='g',
        form='G * k ^ 2 / ( 1 + ω * G * k ^ 2 )',
        expr='G * k ^ 2 / ( 1 + w * G * k ^ 2 )',
        ask='Find g for G = 1, k = 10, w = 1.414 (it approaches the fixed point 1/w as k grows).',
        vars={'g': 'dimensionless coupling', 'G': 'Newton constant', 'k': 'momentum', 'ω': 'running coefficient', 'w': 'ω'},
        given={'G': 1, 'k': 10, 'w': 1.414}, unit='',
        method='Solve the renormalisation-group equation dg/d ln k = 2g − 2ωg² - it flows to g* = 1/ω.',
        distractors=['c', 'π', 'h']),
    'page_curve': dict(
        goal='A black hole takes a time τ to evaporate from mass M to nothing, its remaining lifetime going as the cube of its mass. Write the '
             'time t it takes to shrink from M to m.',
        name='Page time', lhs='t',
        form='τ * ( 1 - ( m / M ) ^ 3 )',
        expr='T * ( 1 - ( m / M ) ^ 3 )',
        ask='Find t in billions of years for T = 10, M = 1 and m = 0.7071 (half the entropy gone: m = M/√2).',
        vars={'t': 'time (Gyr)', 'τ': 'evaporation time', 'T': 'evaporation time (Gyr)', 'm': 'mass then', 'M': 'mass at the start'},
        given={'T': 10, 'M': 1, 'm': 0.7071}, unit='Gyr',
        method='The time left goes as M³, so the time taken to fall to m is τ − τ(m/M)³. Entropy goes as M², so half is gone at m = M/√2.',
        distractors=['2', 'π', 'G']),
    'ads_cft': dict(
        goal='Gravity in a three-dimensional anti-de Sitter space of radius L, with gravity\'s constant G, is the same as a two-dimensional '
             'conformal field theory whose central charge C counts its degrees of freedom. Write C.',
        name='Brown-Henneaux relation', lhs='C',
        form='3 * L / ( 2 * G )',
        expr='3 * L / ( 2 * G )',
        ask='Find C for L = 1000 and G = 1 (in Planck units).',
        vars={'C': 'central charge', 'L': 'AdS radius', 'G': 'Newton constant'},
        given={'L': 1000, 'G': 1}, unit='',
        method='The asymptotic symmetries of AdS₃ form two Virasoro algebras whose central charge comes out as 3L/2G.',
        distractors=['π', 'c', '4']),
}

UA, EV, EW, SW, PA = 'unruh_heating', 'hawking_evaporation', 'er_weaving', 'spin_weaving', 'page_analysis'
RECIPES = [
    ('unruh_thermometer', UA, [('hall_bar_chip', 1), ('test_mass', 1)], [], [('unruh_thermometer', 1)], [], 4096, 800, 'unruh_effect', 10),
    ('unruh_photons', UA, [], [('superfluid_helium', 100)], [], [('hawking_radiation', 100)], 8192, 800, 'unruh_effect', 6),
    ('evaporate_dust', EV, [('interstellar_dust', 2)], [], [], [('hawking_radiation', 100)], 65536, 600, 'hawking_radiation', 4, ['kugelblitz_seed']),
    ('hawking_quanta', EV, [('interstellar_dust', 4)], [], [('hawking_quanta', 1)], [('hawking_radiation', 200)], 65536, 1200, 'hawking_radiation',
     10, ['kugelblitz_seed']),
    ('er_bridge_thread', EW, [('entangled_photon_pair', 4), ('exotic_matter_crystal', 1)], [], [('er_bridge_thread', 1)], [], 16384, 1600, 'er_epr', 20),
    ('planck_area_tile', SW, [('planck_shard', 1)], [], [('planck_area_tile', 4)], [], 16384, 800, 'loop_quantum_gravity', 4),
    ('spin_network_node', SW, [('planck_area_tile', 4), ('metric_tensor', 1)], [], [('spin_network_node', 1)], [], 16384, 1200, 'loop_quantum_gravity', 8),
    ('quantum_condensate', SW, [('spin_network_node', 2)], [('liquid_spacetime', 500)], [], [('quantum_spacetime_condensate', 500)], 32768, 1600,
     'loop_quantum_gravity', 6),
    ('firewall', PA, [('hawking_quanta', 2)], [], [('firewall_shard', 1), ('soft_hair_filament', 1)], [], 16384, 1200, 'information_paradox', 10),
    ('bekenstein', PA, [('hawking_quanta', 1), ('firewall_shard', 1)], [], [('bekenstein_crystal', 1)], [], 16384, 1200, 'black_hole_thermodynamics', 8),
    ('page_curve_record', PA, [('hawking_quanta', 4), ('entangled_photon_pair', 1)], [], [('page_curve_record', 1)], [], 32768, 1600, 'page_curve', 20),
    ('island', PA, [('page_curve_record', 1), ('er_bridge_thread', 1)], [], [('island_fragment', 1)], [], 32768, 2400, 'page_curve', 25),
    ('causal_set_seed', 'monte_carlo', [('normal_distribution_crystal', 2), ('metric_tensor', 1)], [], [('causal_set_seed', 2)], [], 4096, 1200,
     'causal_set_theory', 10, ['random_number_chip']),
    ('fixed_point_lattice', 'monte_carlo', [('spin_network_node', 1), ('causal_set_seed', 1)], [], [('fixed_point_lattice', 1)], [], 8192, 2400,
     'asymptotic_safety', 25, ['random_number_chip']),
    ('holographic_screen', 'holographic_encoding', [('holographic_matrix', 1), ('island_fragment', 1)], [('quantum_spacetime_condensate', 1000)],
     [('holographic_screen', 1)], [], 131072, 2400, 'ads_cft', 40),
    ('boundary_cft_chip', 'holographic_encoding', [('holographic_screen', 1), ('hall_bar_chip', 4)], [], [('boundary_cft_chip', 2)], [], 65536, 1600,
     'ads_cft', 20),
]
SHAPED = [
    ('unruh_ring_casing', ['III', 'ISI', 'III'], {'I': 'minecraft:iron_ingot', 'S': 'spacetime_casing'}, 'unruh_ring_casing', 8),
    ('acceleration_cavity', ['CWC', 'W W', 'CWC'], {'C': 'minecraft:copper_block', 'W': 'waveguide'}, 'acceleration_cavity', 1),
    ('bending_magnet', ['MMM', 'L L', 'MMM'], {'M': 'neodymium_magnet', 'L': 'litz_wire'}, 'bending_magnet', 2),
    ('unruh_detector', ['GHG', 'HTH', 'GHG'], {'G': 'minecraft:glass', 'H': 'hall_bar_chip', 'T': 'test_mass'}, 'unruh_detector', 1),
    ('evaporator_shield', ['LLL', 'LTL', 'LLL'], {'L': 'lead_shielding_plate', 'T': 'tokamak_casing'}, 'evaporator_shield', 4),
    ('horizon_cage', ['ODO', 'D D', 'ODO'], {'O': 'horizon_containment', 'D': 'disk_confinement_ring'}, 'horizon_cage', 2),
    ('weaver_casing', ['III', 'IEI', 'III'], {'I': 'minecraft:iron_ingot', 'E': 'entangled_photon_pair'}, 'weaver_casing', 8),
    ('entanglement_node', ['EGE', 'GXG', 'EGE'], {'E': 'entangled_photon_pair', 'G': 'containment_field_glass', 'X': 'exotic_matter_crystal'},
     'entanglement_node', 1),
    ('pair_source', ['ILI', 'LBL', 'ILI'], {'I': 'minecraft:iron_ingot', 'L': 'laser_rod', 'B': 'beam_splitter'}, 'pair_source', 1),
]


def apply(g):
    kit.write_puzzles('r54', PUZZLES)
    kit.write_research(g, RESEARCH)
    kit.recipes(g, 'r54_', RECIPES)
    kit.crafting(g, 'r54_', SHAPED)


def apply_late(g):
    kit.pickaxe(g, BLOCKS)


def finish(g):
    te = g['text_entry']
    te('spacetime', 'r54_unruh', 'The Unruh Effect', 'unruh_accelerator', [
        'Empty space looks empty only if you are not accelerating. Accelerate at a, and the vacuum glows at $(thing)T = ħa/(2πck)$() - one '
        'kelvin needs 2.5×10²⁰ m/s².',
        'The $(item)Unruh Accelerator$() is a storage ring: electrons of Lorentz factor γ bent by a field B feel a = γ²c²/r, with r = γmc/eB. '
        'Gently, it calibrates an $(item)Unruh Thermometer$() (1-10 K). Hard - above 5000 K - its bath turns superfluid helium into Hawking '
        'radiation. But the beam radiates as γ³B, and the cavities replace only 500 kW.'],
        adv='unruh_effect', sort=60, spot=['unruh_accelerator', 'unruh_thermometer'])
    te('spacetime', 'r54_hawking', 'Micro Black Hole Evaporator', 'micro_black_hole_evaporator', [
        'A black hole of mass M glows at $(thing)T = ħc³/(8πGMk)$(), radiates power ∝ 1/M², and evaporates in a time ∝ M³. A stellar hole is '
        'colder than the sky; one of a billion kilograms, hotter than any star.',
        'The $(item)Micro Black Hole Evaporator$() holds one (a Kugelblitz Seed in a slot) and feeds it dust exactly as fast as it radiates. '
        'Feed it less and it shrinks, heats, runs away and explodes; more, and it swells. Below 6×10⁸ kg its gamma rays are too much for the '
        'lead. It gives great power and $(fluid)Hawking Radiation$(); above 10¹³ K, $(item)Hawking Quanta$() too.'],
        adv='hawking_radiation', sort=61, spot=['micro_black_hole_evaporator', 'hawking_quanta'])
    te('spacetime', 'r54_information', 'The Information Paradox', 'page_curve_analyzer', [
        'A black hole has entropy $(thing)S = Ac³/(4Għ)$() - its area in Planck units over four - the most any region can hold: the '
        '$(item)Bekenstein Crystal$(). It evaporates; if what comes out is purely thermal, what fell in is gone, and quantum mechanics forbids '
        'that.',
        'The $(item)Page Curve Analyzer$() follows Hawking quanta through an evaporation. Firewalls and soft hair are two answers. The third: '
        'the radiation\'s entropy rises, then after the $(thing)Page time$() - when half the hole\'s entropy is gone, at M/√2 - falls back to '
        'zero. A $(item)Page Curve Record$() carried in the off hand keeps your hotbar in order (no Information Loss). With an ER bridge, it '
        'finds the $(item)Island$() that makes the curve turn.'],
        adv='information_paradox', sort=62, spot=['page_curve_analyzer', 'page_curve_record', 'firewall_shard'])
    te('spacetime', 'r54_quantum_geometry', 'Quantum Geometry', 'spin_network_loom', [
        'Loop quantum gravity quantises space: areas come in steps of $(thing)8πγℓₚ²√(j(j+1))$(). The $(item)Spin Network Loom$() weaves Planck '
        'Shards into $(item)Planck Area Tiles$(), tiles into $(item)Spin Network Nodes$(), and nodes with liquid spacetime into $(fluid)Quantum '
        'Spacetime Condensate$().',
        'Causal set theory scatters events at random and keeps only their order: the Monte Carlo Engine sprinkles a $(item)Causal Set Seed$(). '
        'Asymptotic safety follows gravity\'s coupling to a fixed point instead of infinity: a node and a seed together make a '
        '$(item)Fixed-Point Lattice$().'],
        adv='loop_quantum_gravity', sort=63, spot=['spin_network_loom', 'spin_network_node', 'causal_set_seed'])
    te('spacetime', 'r54_er_epr', 'ER = EPR and AdS/CFT', 'er_epr_weaver', [
        'Maldacena and Susskind: two entangled particles are joined by a wormhole too small to cross. The $(item)ER=EPR Weaver$() makes pairs '
        'and sends their halves to two mouths. Fidelity falls with the distance and with the weaver\'s own heat - and every pair heats it. '
        'Cold, and close, it weaves an $(item)ER Bridge Thread$().',
        'AdS/CFT: gravity inside a space is a quantum field theory on its boundary. The Holographic Encoder turns a holographic matrix, an '
        'island and quantum spacetime condensate into a $(item)Holographic Screen$(), and a screen and Hall bars into a $(item)Boundary CFT '
        'Chip$(). For gravity in three dimensions, the boundary theory\'s central charge is exactly 3L/2G.'],
        adv='er_epr', sort=64, spot=['er_epr_weaver', 'er_bridge_thread', 'holographic_screen'])
