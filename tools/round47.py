"""
Round 47 (1.6.0, "Ripples"): General Relativity, Tier III - measuring curved spacetime.

* Laser Interferometer (gravitational waves): an L of two long vacuum arms with fused-silica test masses at their ends,
  after LIGO. A passing wave stretches one arm and squeezes the other by a strain h of 10⁻²¹. Shot noise falls with laser
  power and arm finesse, radiation-pressure noise rises with them - their balance is the standard quantum limit; seismic
  noise falls by ten with every isolation stage, rises with machines running nearby, and is quieter underground (KAGRA);
  thermal noise in the mirrors falls with their temperature, so neutron-star mergers need cryogenic test masses.
* Pound-Rebka Tower (gravitational redshift): a tower built 1 to 8 stages tall. Gamma rays from cobalt-57 climbing it lose
  gh/c² of their frequency; a Doppler drive moving the source at exactly gh/c cancels it - under a micrometre a second.
  Counting statistics decide how sure the measurement is; a degree's difference between source and absorber shifts the
  line as much as the whole effect (the second-order Doppler shift).
* Lasers (optics): an Nd-doped garnet rod, cut from the Crystal Growth Furnace's garnet boule.
* Frame Drag: spinning black holes drag spacetime round with them - near a running Penrose Ergosphere or Kugelblitz
  Chamber, you are carried sideways (Java: ControllerBlockEntity.frameDrag).
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


# =================================================================================== CONTENT
ITEMS = [
    _I('laser_rod', 'Laser Rod', 'crystal', '#802040', '#ff70a0',
       'Garnet doped with neodymium, cut along its lattice and polished flat at both ends. Pumped with light, it gives back more light than it '
       'took in, all of one colour: infrared at 1064 nanometres.'),
    _I('laser_head', 'Laser Head', 'emitter', '#601830', '#ff5060',
       'A laser rod between two mirrors, pumped by a flash of light, behind a Faraday isolator so no reflection can come back into it. A single '
       'frequency, steady for hours.'),
    _I('faraday_isolator', 'Faraday Isolator', 'capacitor', '#602028', '#d04040',
       'A garnet crystal in a strong magnetic field turns the plane of polarised light - the same way whichever way the light goes. Light passes '
       'out; its reflection, turned twice, cannot pass back.'),
    _I('test_mass', 'Test Mass', 'orb', '#c8d8e0', '#ffffff',
       'Forty kilograms of fused silica, polished to a few nanometres and hung on silica fibres: a mirror that does nothing but follow spacetime.'),
    _I('beam_splitter', 'Beam Splitter', 'lens', '#b8c8d8', '#f0f8ff',
       'A disc of fused silica coated so it lets exactly half the light through and reflects the other half.'),
    _I('cobalt_57_source', 'Cobalt-57 Source', 'pellet', '#404858', '#80c0ff',
       'Iron-54 struck by the alpha particles of radium becomes cobalt-57, which decays into iron-57 and gives off a gamma ray so sharp in '
       'energy it can be shifted by gravity across a few metres.', stack=16),
    _I('gw_record_bbh', 'Black Hole Merger Record', 'codex', '#101828', '#80ffff',
       'A chirp a fifth of a second long: two black holes of thirty suns each spiralling together a billion light-years away.'),
    _I('gw_record_bns', 'Neutron Star Merger Record', 'codex', '#181028', '#ffc0ff',
       'A minute-long chirp of two neutron stars colliding. Where they merged, gold and platinum were made - and a telescope can still see it '
       'glow.'),
    _I('redshift_record', 'Gravitational Redshift Record', 'codex', '#202818', '#e0ff80',
       'Counts of gamma rays climbing a tower, and the source speed that brought them back into tune: gravity measured with a stopwatch of '
       'atomic nuclei.'),
]
BLOCKS = [
    # the Laser Interferometer (its corner and end stations reuse the Observatory Wall)
    _B('vacuum_beam_tube', 'Vacuum Beam Tube', 'beampipe', '#8a8e94', '#d0d4d8', light=0),
    _B('test_mass_chamber', 'Test Mass Chamber', 'cryo', '#a0b0c0', '#ffffff', light=4),
    _B('seismic_isolator', 'Seismic Isolator', 'dipole', '#5a6a50', '#a0c080', light=0),
    # the Pound-Rebka Tower
    _B('redshift_tower_casing', 'Redshift Tower Casing', 'casing2', '#8a5a48', '#c8a080', light=0),
    _B('mossbauer_absorber', 'Mössbauer Absorber', 'injector', '#5a5a60', '#ffe080', light=4),
    _B('doppler_drive', 'Doppler Drive', 'coilblock', '#8a5030', '#ffa060', light=2),
]
FLUIDS = []

KEY_ADD = {
    'Λ': ['vacuum_beam_tube'], 'Θ': ['test_mass_chamber'], 'Σ': ['seismic_isolator'],
    'Ψ': ['redshift_tower_casing'], 'Ω': ['redshift_tower_casing', 'interface_port'], 'Δ': ['mossbauer_absorber'], 'ε': ['doppler_drive'],
}


# =================================================================================== SHAPES
def laser_interferometer():
    """An L: the corner station (laser, beam splitter, the input test masses), two vacuum arms on piers running out
    along the ground, and an end station at the far end of each, its test mass hung on isolation stacks."""
    s = shapes.Shape(31, 6, 31)
    L = 30

    def station(x0, z0, x1, z1):
        s.box(x0, 0, z0, x1, 0, z1, 'κ')
        s.box(x0, 1, z0, x1, 4, z1, 'κ', hollow=True)
        s.air_box(x0 + 1, 1, z0 + 1, x1 - 1, 3, z1 - 1)

    station(0, 0, 6, 6)                                                   # the corner station
    station(L - 4, 0, L, 6)                                               # end station X
    station(0, L - 4, 6, L)                                               # end station Y
    for i in range(7, L - 4):
        s.put(i, 2, 3, 'Λ')                                               # arm X
        s.put(3, 2, i, 'Λ')                                               # arm Y
        if i % 4 == 0:
            s.put(i, 1, 3, 'κ'); s.put(i, 0, 3, 'κ')                      # its piers
            s.put(3, 1, i, 'κ'); s.put(3, 0, i, 'κ')
    s.put(4, 2, 3, 'Θ'); s.put(3, 2, 4, 'Θ')                              # input test masses
    s.put(L - 2, 2, 3, 'Θ'); s.put(3, 2, L - 2, 'Θ')                      # end test masses
    for x, z in ((4, 3), (3, 4), (L - 2, 3), (3, L - 2)):
        s.put(x, 1, z, 'Σ')                                               # isolation stacks under them
    s.put(2, 1, 2, 'Σ')
    s.put(5, 2, 5, 'Θ')                                                   # (the laser bench's own reference mirror)
    for x in (6, L - 4):
        s.put(x, 2, 3, 'Λ')                                               # the tube passes through the station walls
    for z in (6, L - 4):
        s.put(3, 2, z, 'Λ')
    s.center = (3.0, 2.0, 3.0)
    s.ports('κ', 'λ', 4, y=0)
    s.v[(3, 1, 0)] = 'C'
    s.ctrl = (3, 1, 0)
    return s


def pound_rebka_tower():
    """A tower: the Doppler drive and the source at the foot, one storey per stage stacked above (1 to 8), and the
    absorber with its counter at the top."""
    s = shapes.Shape(5, 5, 5)
    c = 2
    s.box(0, 0, 0, 4, 0, 4, 'Ψ')
    s.put(c, 0, c, 'ε')                                                   # the Doppler drive
    s.box(0, 1, 0, 4, 1, 4, 'Ψ', hollow=True)
    s.put(c, 1, c, 'ε')                                                   # the source holder on it
    for x in range(5):                                                    # the stage: a storey of the tower, hollow for the beam,
        for z in range(5):                                                # with a leaded-glass window in each face
            if x in (0, 4) or z in (0, 4):
                s.put(x, 2, z, 'ρ' if c in (x, z) else 'Ψ')
            else:
                s.put(x, 2, z, '.')
    s.box(0, 3, 0, 4, 3, 4, 'Ψ')                                          # the roof
    s.put(c, 3, c, '.')
    s.box(1, 4, 1, 3, 4, 3, 'Ψ')                                          # the counting room on top
    s.put(c, 4, c, 'Δ')                                                   # the absorber and counter
    s.repeat = (2, 2, 7)
    s.center = (c, 2.0, c)
    s.ports('Ψ', 'Ω', 4, y=0)
    s.v[(c, 1, 0)] = 'C'
    s.ctrl = (c, 1, 0)
    return s


shapes.BUILDERS.update({'laser_interferometer': laser_interferometer, 'pound_rebka_tower': pound_rebka_tower})

# =================================================================================== MACHINES
MACHINE_TEXT = {
    'laser_interferometer': ('Laser Interferometer',
                             'Two long arms at right angles, a laser split between them, and mirrors that hang free. A passing gravitational wave '
                             'stretches one arm and squeezes the other by a thousandth of a proton\'s width.'),
    'pound_rebka_tower': ('Pound-Rebka Tower',
                          'Gamma rays climbing a tower lose energy to gravity. A source moving at the right speed - less than a micrometre a '
                          'second - puts them back in tune with the absorber above.'),
}
MACHINE_CAT = {k: 'spacetime' for k in MACHINE_TEXT}
MACHINE_RESEARCH = {'laser_interferometer': 'gravitational_waves', 'pound_rebka_tower': 'gravitational_redshift'}
TIER = {k: 3 for k in MACHINE_TEXT}
_S = 'spacetime_casing'
MC = {
    'laser_interferometer': (['TLT', 'BSB', 'IVI'], {'T': 'test_mass', 'L': 'laser_head', 'B': 'beam_splitter', 'S': _S, 'I': 'seismic_isolator',
                                                     'V': 'vacuum_beam_tube'}),
    'pound_rebka_tower': (['RMR', 'GSG', 'RDR'], {'R': 'redshift_tower_casing', 'M': 'mossbauer_absorber', 'G': 'geiger_tube', 'S': _S,
                                                  'D': 'doppler_drive'}),
}
EXTRA = {
    'laser_interferometer': 'Balance the laser power and the arms\' finesse between shot noise and radiation-pressure noise; add isolation stages '
                            'against the ground\'s shaking (other machines nearby make it worse; underground is quieter); cool the test masses '
                            'to beat thermal noise. A black hole merger needs a signal-to-noise of 8 at h = 10⁻²¹; a neutron star merger, five '
                            'times fainter, needs cold mirrors.',
    'pound_rebka_tower': 'Built 1 to 8 storeys tall (7.5 m each). Set the Doppler drive to v = gH/c to cancel the gravitational shift, and count '
                         'long enough that the shift stands five times above the counting error. Keep source and absorber at the same '
                         'temperature: each kelvin between them shifts the line by 0.66 µm/s.',
}

# =================================================================================== PHYSICS
_r12.model('laser_interferometer', 'Laser Interferometer',
           'A gravitational wave of strain h changes the arms\' lengths by ±hL/2, and the light returning to the beam splitter interferes a little '
           'differently. Four noises hide it. Shot noise - the graininess of light - falls as 1/(F√P) with the arms\' finesse F and the laser '
           'power P; radiation-pressure noise - the photons\' kicks on the mirrors - rises as F√P: where they are equal is the standard quantum '
           'limit. Each isolation stage cuts the ground\'s shaking tenfold; the thermal jiggling of the mirrors falls as √T.',
           'Microseism: the ground shakes by ±50%·d.',
           [_P('P', 'Laser power P', 'W', 1, 200, 0.1, 50, 'Light circulating from the laser. More beats shot noise - and kicks the mirrors harder.'),
            _P('F', 'Arm finesse F', '', 1, 500, 1, 100, 'How many times the light bounces along each arm before it returns: the arm is that '
                                                           'much longer in effect.'),
            _P('ns', 'Isolation stages', '', 0, 7, 1, 3, 'Pendulums and springs between the ground and each test mass. Each cuts the shaking '
                                                         'tenfold - and makes the mirrors slower to lock.')],
           [_D('x', 'Optical gain F√P', '', 'F*sqrt(P)', 'F·√P', 'What both quantum noises depend on.', show=False),
            _D('hs', 'Shot noise', 'strain', '1e-20/x', 'h_shot ∝ 1/(F√P)', 'Graininess of the light.'),
            _D('hr', 'Radiation-pressure noise', 'strain', '2.5e-27*x', 'h_rp ∝ F√P', 'Random kicks of photons on the test masses.'),
            _D('hg', 'Seismic noise', 'strain', '1e-19*(1 + 0.5*rfi)*(1 + pos(alt)/128)*(1 + 0.5*d)*10^(-ns)', 'h_seis ∝ ground · 10⁻ⁿ',
               'The ground\'s shaking, after the isolation stages. Machines nearby make it worse; below sea level it is quieter.'),
            _D('ht', 'Thermal noise', 'strain', '4e-23*sqrt(max(T, 0.01)/300)', 'h_th ∝ √T', 'Brownian jiggling of the mirrors and their fibres.'),
            _D('hn', 'Total noise', 'strain', 'sqrt(hs^2 + hr^2 + hg^2 + ht^2)', 'h_n = √Σh²', 'Every noise added in quadrature.'),
            _D('SNR', 'Signal to noise', '', 'hw/hn', 'SNR = h/h_n', 'How far the wave stands above the noise.')],
           [_C('lock', 'stop', '1e-20 - hg', 'The ground shakes too hard: the arms cannot hold lock', 'h_seis < 10⁻²⁰')],
           rate='clamp((0.5 + 0.05*(SNR - 8))/(1 + 0.1*ns), 0.2, 2)', rate_formula='rate = (0.5 + 0.05·(SNR − 8)) / (1 + 0.1n)',
           rate_desc='A clearer detection is faster; every isolation stage makes locking slower.',
           heat='0.01*P', idle_heat='0', rvars=[_V('hw', 'Wave strain h', 1e-21)], kappa=40, heat_capacity=60)

_r12.model('pound_rebka_tower', 'Pound-Rebka Tower',
           'Light climbing a height H in gravity g loses a fraction gH/c² of its frequency. The gamma ray of iron-57 is so sharp that this shift - '
           'a few parts in 10¹⁵ - can be seen: move the source up at v = gH/c and the Doppler shift cancels it. Counting N gamma rays measures the '
           'line\'s centre to its width over √N. A temperature difference between source and absorber shifts it too, by 2.2×10⁻¹⁵ a kelvin.',
           'Vibration of the tower: the thermal shift wanders by ±10%·d.',
           [_P('v', 'Source speed v', 'µm/s', -5, 5, 0.001, 0, 'How fast the Doppler drive moves the source. Upward is positive.'),
            _P('t', 'Counting time', 'min', 1, 600, 1, 60, 'How long the gamma rays are counted at this speed.')],
           [_D('H', 'Tower height', 'm', '7.5*size', 'H = 7.5 m · storeys', 'How far the gamma rays climb.'),
            _D('sh', 'Gravitational shift', '×10⁻¹⁵', '9.81*H/8.988e16*1e15', 'Δf/f = gH/c²', 'How much frequency the gamma rays lose.'),
            _D('vg', 'Speed that cancels it', 'µm/s', '9.81*H/2.998e8*1e6', 'v = gH/c', 'The source speed whose Doppler shift makes up the loss.'),
            _D('vt', 'Thermal shift', 'µm/s', '0.66*(T - T_amb)*(1 + 0.1*d)', 'Δv = 0.66 µm/s per K', 'From a temperature difference between source and '
                                                                                  'absorber (the second-order Doppler shift).'),
            _D('sg', 'Counting error', 'µm/s', '190/sqrt(30000*t)', 'σ = Γ/√N', 'The line is 190 µm/s wide; 30,000 counts a minute.'),
            _D('mis', 'Mistuning', 'σ', 'abs(v - vg - vt)/sg', '|v − v_g − v_th|/σ', 'How far the setting is from cancelling the shift.'),
            _D('zs', 'Significance', 'σ', 'vg/sg', 'v_g/σ', 'How many counting errors the whole effect amounts to.')],
           [],
           rate='clamp(2*60/(t + 60)*(0.6 + 0.05*size), 0.1, 2)', rate_formula='rate = 2·60/(t + 60 min) · (0.6 + 0.05·storeys)',
           rate_desc='Shorter counts are faster; a taller tower measures more at once.',
           heat='0.02', idle_heat='0', kappa=40, heat_capacity=40)

_r12.WINDOWS['laser_interferometer'] = {
    'r47_bbh_merger': ({'hw': 1e-21}, [('SNR', 8, 1e12)]),
    'r47_bns_merger': ({'hw': 2e-22}, [('SNR', 8, 1e12)]),
}
_r12.WINDOWS['pound_rebka_tower'] = {'r47_redshift': ({}, [('mis', 0, 2), ('zs', 5, 1e9)])}
_r12.WINDOWS.setdefault('optical_workshop', {}).update({
    'r47_cut_laser_rod': ({'n': 1.82}, [('Pw', -0.02, 0.02), ('e', 0, 69)]),
    'r47_cut_test_mass': ({'n': 1.458}, [('Pw', -0.01, 0.01), ('e', 0, 20)]),
    'r47_cut_beam_splitter': ({'n': 1.458}, [('Pw', -0.02, 0.02), ('e', 0, 34)]),
})
_r12.WINDOWS.setdefault('reflecting_telescope', {}).update({'r47_kilonova': ({'mreq': 14}, [('mlim', 14, 99)])})

# =================================================================================== RESEARCH
RESEARCH = [
    ('laser_physics', 'Lasers',
     'An excited atom struck by a photon of the right colour gives off a second, identical one. Pump a crystal full of excited atoms between two '
     'mirrors and the light builds on itself: one colour, one direction, in step. Unlocks the Laser Rod, Laser Head and Faraday Isolator.',
     'laser_head', 7, -10, 70, ['geometric_optics', 'crystal_growth_physics'], [('garnet_boule', 2), ('fused_silica_lens', 2)]),
    ('gravitational_redshift', 'Gravitational Redshift',
     'Light climbing out of a gravity well loses energy, so its frequency falls: clocks below run slow seen from above. In 1959 Pound and '
     'Rebka measured it up a 22.5-metre tower. Unlocks the Pound-Rebka Tower and the Cobalt-57 Source.',
     'pound_rebka_tower', 7, 8, 80, ['gravitational_time_dilation', 'radioactivity'], [('time_dilated_sample', 2), ('radium_salt', 2)]),
    ('gravitational_waves', 'Gravitational Waves',
     'Masses that accelerate send ripples through spacetime itself, stretching and squeezing everything they cross. The strongest reach us '
     'from merging black holes - as a strain of one part in 10²¹. Unlocks the Laser Interferometer and its Test Masses.',
     'laser_interferometer', 9, 8, 140, ['gravitational_redshift', 'laser_physics'], [('redshift_record', 4), ('laser_head', 1)]),
]

PUZZLES = {
    'laser_physics': dict(
        goal='A laser gives off light of one wavelength λ. Each photon carries an energy set by the quantum of action h and the speed of light c. '
             'Write the energy E of one photon.',
        name='Planck relation for light', lhs='E',
        form='h * c / λ',
        expr='h * c / L * 6241509074460763000',
        ask='Find E in electronvolts for a neodymium laser, L = 1.064e-6 m (h = 6.626e-34 J·s, c = 2.998e8 m/s; 1 eV = 1.602e-19 J).',
        vars={'E': 'photon energy (eV)', 'h': 'Planck constant (J·s)', 'c': 'speed of light (m/s)', 'λ': 'wavelength', 'L': 'wavelength (m)'},
        given={'h': 6.626e-34, 'c': 2.998e8, 'L': 1.064e-6}, unit='eV',
        method='A photon\'s energy is hν, and its frequency is c over its wavelength.',
        distractors=['2', 'π', 'k']),
    'gravitational_redshift': dict(
        goal='Light climbs a height H in a gravity g. Moving up it loses energy as a ball thrown upward would, in proportion to its own energy '
             'divided by c². Write the fraction s of its frequency it loses.',
        name='Gravitational shift over a height', lhs='s',
        form='g * H / c ^ 2',
        expr='g * H / c ^ 2 * 1000000000000000',
        ask='Find s in units of 10⁻¹⁵ for Pound and Rebka\'s tower: g = 9.81 m/s², H = 22.5 m, c = 2.998e8 m/s.',
        vars={'s': 'fractional shift (×10⁻¹⁵)', 'g': 'gravity (m/s²)', 'H': 'height (m)', 'c': 'speed of light (m/s)'},
        given={'g': 9.81, 'H': 22.5, 'c': 2.998e8}, unit='×10⁻¹⁵',
        method='A photon of energy E has an effective mass E/c²; climbing H costs it (E/c²)·g·H of energy.',
        distractors=['2', 'G', '+']),
    'gravitational_waves': dict(
        goal='Two bodies of masses m _ 1 and m _ 2 circle each other a distance a apart, far away at a distance r. Their changing shape sends out '
             'gravitational waves; write the strain h they make here, to order of magnitude.',
        name='Quadrupole strain of a binary', lhs='h',
        form='4 * G ^ 2 * m _ 1 * m _ 2 / ( c ^ 4 * a * r )',
        expr='4 * G ^ 2 * A * B / ( c ^ 4 * a * r ) * 1000000000000000000000',
        ask='Find h in units of 10⁻²¹ for two black holes of A = B = 5.97e31 kg, a = 3.5e5 m apart, r = 1.27e25 m away (G = 6.674e-11, c = 2.998e8).',
        vars={'h': 'strain (×10⁻²¹)', 'G': 'gravitational constant', 'm': 'mass', 'c': 'speed of light', 'a': 'separation (m)', 'r': 'distance (m)',
              'A': 'first mass (kg)', 'B': 'second mass (kg)'},
        given={'G': 6.674e-11, 'A': 5.97e31, 'B': 5.97e31, 'c': 2.998e8, 'a': 3.5e5, 'r': 1.27e25}, unit='×10⁻²¹',
        method='The quadrupole formula: h ≈ (2G/c⁴r)·d²Q/dt², and for a circular binary the second derivative of its quadrupole is about 2μa²ω², '
               'with ω² = G(m₁+m₂)/a³.',
        distractors=['π', '2', 'ln']),
}

# =================================================================================== RECIPES
GW, PR, OF, AP, BA = 'gw_detection', 'redshift_measurement', 'optical_finishing', 'astrophotography', 'bayesian_inference'
RECIPES = [
    # (name, type, item inputs, item outputs, FE/t, ticks, research, insight, catalysts)
    ('bbh_merger', GW, [('minecraft:paper', 2)], [('gw_record_bbh', 1)], 2048, 1200, 'gravitational_waves', 10, []),
    ('bns_merger', GW, [('minecraft:paper', 2)], [('gw_record_bns', 1)], 2048, 2400, 'gravitational_waves', 15, []),
    ('redshift', PR, [('minecraft:paper', 1)], [('redshift_record', 1)], 256, 1200, 'gravitational_redshift', 5, ['cobalt_57_source']),
    ('cut_laser_rod', OF, [('garnet_boule', 1), ('laue_pattern', 1)], [('laser_rod', 2)], 128, 400, 'laser_physics', 0, []),
    ('cut_test_mass', OF, [('fused_silica', 8)], [('test_mass', 1)], 256, 1200, 'gravitational_waves', 0, []),
    ('cut_beam_splitter', OF, [('fused_silica', 2)], [('beam_splitter', 2)], 128, 600, 'laser_physics', 0, []),
    # a neutron star merger's afterglow: the kilonova where the heavy elements were made
    ('kilonova', AP, [('photographic_plate', 2), ('gw_record_bns', 1)], [('deep_sky_plate', 2), ('stellar_spectrum_plate', 1)], 128, 1200, 'gravitational_waves', 12,
     ['gw_record_bns']),
    # merger parameters are read off the chirp by Bayesian inference
    ('posterior_chirp', BA, [('prior_tablet', 1), ('gw_record_bbh', 1)], [('posterior_tablet', 3)], 30000, 300, 'bayesian_inference', 0, []),
]
CRAFTING = [
    ('faraday_isolator', ['FGF'], {'F': 'ferrite_magnet', 'G': 'garnet_boule'}, 'faraday_isolator', 1),
    ('laser_head', ['MLM', 'FRF', 'MIM'], {'M': 'telescope_mirror_blank', 'L': 'laser_rod', 'F': 'fused_silica_lens', 'R': 'minecraft:glowstone_dust',
                                           'I': 'faraday_isolator'}, 'laser_head', 1),
    ('cobalt_57_source', ['ILI', 'IRI', 'ILI'], {'I': 'minecraft:iron_ingot', 'L': 'lead_shielding_plate', 'R': 'radium_salt'}, 'cobalt_57_source', 2),
    ('vacuum_beam_tube', ['III', 'G G', 'III'], {'I': 'minecraft:iron_ingot', 'G': 'minecraft:glass'}, 'vacuum_beam_tube', 8),
    ('test_mass_chamber', ['IFI', 'F F', 'IFI'], {'I': 'minecraft:iron_ingot', 'F': 'fused_silica'}, 'test_mass_chamber', 2),
    ('seismic_isolator', ['ISI', 'PSP', 'ISI'], {'I': 'minecraft:iron_ingot', 'S': 'minecraft:slime_ball', 'P': 'minecraft:piston'}, 'seismic_isolator', 2),
    ('redshift_tower_casing', ['BIB', 'I I', 'BIB'], {'B': 'minecraft:bricks', 'I': 'minecraft:iron_ingot'}, 'redshift_tower_casing', 8),
    ('mossbauer_absorber', ['LIL', 'IGI', 'LIL'], {'L': 'lead_shielding_plate', 'I': 'minecraft:iron_ingot', 'G': 'geiger_tube'}, 'mossbauer_absorber', 1),
    ('doppler_drive', ['CRC', 'RPR', 'CRC'], {'C': 'minecraft:copper_ingot', 'R': 'minecraft:redstone', 'P': 'minecraft:piston'}, 'doppler_drive', 1),
]
LANG = {}


# =================================================================================== WRITERS
def _write_research(g):
    import round42
    saved = round42.RESEARCH
    round42.RESEARCH = RESEARCH
    try:
        round42._write_research(g)
    finally:
        round42.RESEARCH = saved


def _write_puzzles():
    p = os.path.join(HERE, 'puzzles2', 'group_r47.json')
    data = json.dumps(PUZZLES, ensure_ascii=False, indent=1)
    if not os.path.exists(p) or open(p, encoding='utf-8').read() != data:
        with open(p, 'w', encoding='utf-8') as f:
            f.write(data)


def _recipes(g):
    REC, w = g['REC'], g['w']
    for name, typ, ins, outs, fe, t, res, insight, cats in RECIPES:
        g['mr']('r47_' + name, typ, ins, [], outs, [], energy=fe, duration=t, research=res, catalysts=cats)
        p = os.path.join(REC, typ, 'r47_' + name + '.json')
        r = json.load(open(p))
        r['unscaled'] = True
        if insight: r['insight_per_run'] = insight
        w(p, r)
    for name, pat, key, res, n in CRAFTING:
        g['shaped']('r47_' + name, pat, key, res, n)


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
    te('optics', 'r47_lasers', 'Lasers', 'laser_head', [
        'An excited atom struck by a photon of just the right energy gives off a second, identical photon: $(thing)stimulated emission$(). Fill a '
        'crystal with excited atoms, put it between two mirrors, and the light builds on itself.',
        'A garnet boule grown with a rare earth is the crystal: neodymium in garnet lases at 1064 nm, in the infrared. Cut it along its lattice into '
        '$(item)Laser Rods$(); with mirrors, silica lenses and a flash to pump it, it becomes a $(item)Laser Head$().',
        'Behind it sits a $(item)Faraday Isolator$(): garnet in a strong magnetic field turns polarised light the same way going and coming, so its '
        'own reflection cannot get back into the laser and upset it.'],
        adv='laser_physics', sort=5, spot=['laser_head', 'laser_rod', 'faraday_isolator'])
    te('spacetime', 'r47_redshift', 'Gravity and Light', 'pound_rebka_tower', [
        'Light climbing out of gravity loses energy. Over a height H the fraction lost is $(thing)gH/c²$() - about 2.5×10⁻¹⁵ up a 22.5-metre '
        'tower. In 1959 Pound and Rebka measured it with the gamma ray of iron-57, whose line is so sharp that even this shows.',
        'The $(item)Pound-Rebka Tower$() is built 1 to 8 storeys tall, 7.5 m each. A $(item)Cobalt-57 Source$() sits on a Doppler drive at the foot, '
        'the absorber at the top. Move the source up at exactly $(thing)v = gH/c$() - under a micrometre a second - and the climb is cancelled.',
        'Counting decides how sure you are: the line is 190 µm/s wide, and N gamma rays find its centre to 190/√N. Count until the shift stands '
        'five times above that error, and set v within two of it. Keep the tower at the air\'s temperature: each kelvin between source and absorber '
        'shifts the line by 0.66 µm/s - as much as the whole effect.'],
        adv='gravitational_redshift', sort=40, spot=['pound_rebka_tower', 'cobalt_57_source', 'redshift_record'])
    te('spacetime', 'r47_waves', 'Gravitational Waves', 'laser_interferometer', [
        'Two black holes spiralling together shake spacetime: a wave passes, stretching space one way and squeezing it the other, by a strain h '
        'of 10⁻²¹ when it reaches us. The $(item)Laser Interferometer$() is an L of two long arms with a $(item)Test Mass$() at each end: a laser '
        'split between the arms comes back to interfere, and a wave changes the pattern.',
        'Four noises hide the wave. $(thing)Shot noise$(), the graininess of light, falls as the laser power P and the arms\' finesse F rise - '
        'but $(thing)radiation pressure$(), the photons\' kicks on the mirrors, grows with them. Both depend only on F√P; where they are equal is '
        'the standard quantum limit.',
        '$(thing)Seismic noise$() falls tenfold with each isolation stage - more stages, slower to lock. Machines running nearby shake the ground; '
        'below sea level it is quieter, as at KAGRA. $(thing)Thermal noise$() in the mirrors falls as √T: cool them with your coolant.',
        'A black hole merger, h = 10⁻²¹, needs a signal-to-noise of 8. A neutron star merger is five times fainter: only cold mirrors will do. Its '
        'record points a telescope at the $(thing)kilonova$() - where gold and platinum were made - and every detection gives a great deal of '
        'Insight. The chirp\'s parameters are read off by Bayesian inference: the Analyzer takes black hole records as evidence.'],
        adv='gravitational_waves', sort=41, spot=['laser_interferometer', 'test_mass', 'gw_record_bbh', 'gw_record_bns'])
    te('spacetime', 'r47_frame_drag', 'Frame Dragging', 'penrose_ergosphere', [
        'A spinning mass drags spacetime round with it. Near the Earth the effect is tiny; near a spinning black hole, inside its ergosphere, '
        'nothing can stand still.',
        'Stand near a running $(item)Penrose Ergosphere$() or $(item)Kugelblitz Chamber$() and you are carried sideways: $(thing)Frame Drag$().'],
        adv='gravitational_waves', sort=42, spot=['penrose_ergosphere', 'kugelblitz_chamber'])
