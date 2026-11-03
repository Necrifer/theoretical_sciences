"""
Round 51 (1.10.0, "The Accretion Frontier"): the second dimension of the 1.1 expansion design, and the Astrophysics nodes
that go with it.

* The Accretion Frontier: islands of rock, ice and slag in the space round an accreting black hole, reached with a Light
  Sail (Galactic Dynamics) from high above the Overworld. Five biomes, each with its hazard (design doc):
  Molecular Cloud Nebula (dust; low visibility), Asteroid Belt (low gravity; micrometeorites unless you wear a Whipple
  Shield), Pulsar Field (radiation pulses on a fixed beat; high gravity and Time Dilation), Accretion Rim (heat; tidal
  pull), Lensing Void (frame-dragging drift; warped sight).
* Its creatures: Nebuloid, Pulsar Wisp, Tidal Stretcher, Lens Phantom, Hawking Imp, Boltzmann Brain - and The Binary,
  a mini-boss summoned by playing a binary black hole merger record there. The Black Swan can turn up anywhere in the
  Overworld, but only after a long time in which nothing improbable has happened.
* Structures: the Derelict Station (floating in the Frontier), the Pulsar Beacon (Pulsar Field) and the Probability Hall
  (a rare house in villages).
* Astrophysics: Interstellar Medium (the Interstellar Scoop), Exoplanetology, Accretion Physics (the Accretion Disk
  Reactor, a generator limited by the Eddington luminosity) and The Cosmic Web.
"""
import json
import math
import os

import shapes
import round12 as _r12
from round12 import P as _P, D as _D, C as _C, V as _V
import kit
from kit import I, B, ORE, FL

NS = 'theoreticalsciences'
HERE = os.path.dirname(os.path.abspath(__file__))
DIM = 'accretion_frontier'
BIOMES = ['molecular_cloud_nebula', 'asteroid_belt', 'lensing_void', 'pulsar_field', 'accretion_rim']
M = NS + ':'

# =================================================================================== CONTENT
ITEMS = [
    I('light_sail', 'Light Sail', 'sail', '#c0c8d8', '#fff0c0',
      'A sail of aluminised film a few microns thick, pushed by nothing but the pressure of starlight. Unfurl it high above the world, where '
      'there is no air left to drag on it, and it carries you out to the Accretion Frontier.', special='lightsail', stack=1),
    I('interstellar_dust', 'Interstellar Dust', 'dots', '#403848', '#a090b0',
      'Grains of silicate and soot a tenth of a micron across, coated in ice. A hundredth of the interstellar medium by mass - and what every '
      'rocky planet was made of.'),
    I('cosmic_ray_shard', 'Cosmic Ray Shard', 'shard', '#303050', '#c0d0ff',
      'Crystal scarred by the tracks of cosmic rays - nuclei flung across the Galaxy by supernova shocks at nearly the speed of light.'),
    I('exoplanet_core_sample', 'Exoplanet Core Sample', 'chunk', '#404038', '#c0b080',
      'Iron and nickel from the core of a planet that formed round another star, and was shattered in a collision long ago.'),
    I('brown_dwarf_core', 'Brown Dwarf Core', 'quarkstar', '#402418', '#c06030',
      'The dense heart of a failed star: too light to fuse hydrogen, heavy enough once to burn its deuterium. It still glows with the heat of '
      'its own contraction.'),
    I('cepheid_standard_candle', 'Cepheid Standard Candle', 'star', '#604010', '#ffe080',
      'The light curve of a Cepheid variable. How long it takes to pulse tells how bright it truly is (Leavitt\'s law) - and so how far away.'),
    I('redshift_lens', 'Redshift Lens', 'lens', '#401818', '#ff8070',
      'A calibrated lens of spectra: distances from standard candles set against the redshifts of their galaxies. Every one is moving away, '
      'and the farther, the faster.'),
    I('galactic_filament', 'Galactic Filament', 'string', '#182040', '#80a0ff',
      'A map of the hydrogen in a filament of gas feeding a galaxy - a thread of the cosmic web, at the scale of one galaxy.'),
    I('accretion_disk_fragment', 'Accretion Disk Fragment', 'shard', '#402010', '#ffa040',
      'Slag from the inner edge of an accretion disk, frozen where it was flung out. It still remembers ten million kelvin.'),
    I('quasar_jet_filament', 'Quasar Jet Filament', 'streak', '#102040', '#a0e0ff',
      'A thread of the jet that a spinning black hole drives out along its axis at nearly the speed of light, wound up in magnetic field.'),
    I('planetary_nebula_shell', 'Planetary Nebula Shell', 'shell', '#183040', '#80ffd0',
      'The glowing shell a dying Sun-like star throws off: oxygen green, hydrogen red, round a white dwarf at its centre.'),
    I('cosmic_web_filament', 'Cosmic Web Filament', 'lattice', '#100818', '#c0a0ff',
      'The largest structure there is: filaments of dark matter and gas a hundred million light-years long, with galaxies strung along them '
      'like dew on a web.'),
    I('transit_light_curve', 'Transit Light Curve', 'codex', '#202830', '#ffe0a0',
      'A star\'s brightness, dipping for a few hours as a planet crosses in front of it. The depth gives the planet\'s size.'),
    I('einstein_ring_lens', 'Einstein Ring Lens', 'lens', '#181830', '#e0e8ff',
      'A crystal from the Lensing Void that bends light round itself like a mass: a point behind it shows as a ring.'),
    I('magnetar_crust_fragment', 'Magnetar Crust Fragment', 'chunk', '#182030', '#60c0ff',
      'Iron crust from a magnetar, under a field of 10¹¹ tesla. It cracks in starquakes - and it still bends compass needles from across a room.'),
    I('tidal_shear_crystal', 'Tidal Shear Crystal', 'crystal', '#401808', '#ff9040',
      'A crystal pulled long and thin by a tidal force: one end fell faster than the other.'),
    I('hawking_quanta', 'Hawking Quanta', 'ghostorb', '#000000', '#ffe0a0',
      'The last light of a tiny black hole, caught as it evaporated. Hotter the smaller the hole was.'),
    I('black_swan_feather', 'Black Swan Feather', 'shard', '#101014', '#c02020',
      'Proof that the improbable happens. For centuries every swan was white - until one was not.'),
    I('chirp_signal_record', 'Chirp Signal Record', 'codex', '#102040', '#ffd060',
      'The rising whoop of two compact stars spiralling together: their gravitational waves climb in pitch and strength until they merge.'),
    I('merger_remnant', 'Merger Remnant', 'remnant', '#101828', '#ffe080',
      'What is left when two compact stars become one: a single spinning hole, ringing down to stillness.'),
    I('entropy_die', 'Entropy Die', 'artifact', '#202028', '#e0e0e0',
      'A die worn perfectly fair. Every face is equally likely - which is to say it carries the most entropy a die can: log₂ 6 bits a throw.'),
]
BLOCKS = [
    B('frontier_regolith', 'Frontier Regolith', 'bulkstone', '#3a3640', '#6a6470', kind='simple', light=0, hardness=2.5, tool='stone'),
    B('nebular_ice', 'Nebular Ice', 'crystalblock', '#283048', '#8090c0', kind='simple', light=0, hardness=2.0, tool='stone'),
    B('asteroid_rock', 'Asteroid Rock', 'bulkstone', '#302c28', '#6a6050', kind='simple', light=0, hardness=3.5, tool='stone'),
    B('pulsar_crust', 'Pulsar Crust', 'metalblock', '#202838', '#6080b0', kind='simple', light=1, hardness=5.0, tool='iron'),
    B('rim_slag', 'Rim Slag', 'bulkstone', '#301008', '#a04020', kind='simple', light=3, hardness=4.0, tool='stone'),
    B('void_glass', 'Void Glass', 'fieldglass', '#080810', '#404060', kind='simple', light=1, hardness=3.0, tool='stone'),
    B('station_hull', 'Station Hull', 'plating', '#505860', '#a0a8b0', kind='simple', light=0, hardness=5.0, tool='iron'),
    ORE('dust_ice_ore', 'Dusty Ice', 'nebular_ice', '#302838', '#a090b0', 'interstellar_dust', hardness=2.5, tool='stone'),
    ORE('planetesimal_core_ore', 'Planetesimal Core Ore', 'asteroid_rock', '#404038', '#c0b080', 'exoplanet_core_sample', hardness=4.5),
    ORE('brown_dwarf_ore', 'Brown Dwarf Remnant', 'asteroid_rock', '#402418', '#c06030', 'brown_dwarf_core', light=4, hardness=5.0),
    ORE('magnetar_crust_ore', 'Magnetar Crust Ore', 'pulsar_crust', '#182030', '#60c0ff', 'magnetar_crust_fragment', light=3, hardness=6.0),
    ORE('pulsar_timing_ore', 'Pulsar Timing Ore', 'pulsar_crust', '#182040', '#80e0ff', 'pulsar_timing_crystal', light=5, hardness=6.0),
    ORE('cosmic_ray_ore', 'Cosmic Ray Ore', 'pulsar_crust', '#303050', '#c0d0ff', 'cosmic_ray_shard', light=4, hardness=5.0),
    ORE('accretion_fragment_ore', 'Accretion Slag Ore', 'rim_slag', '#402010', '#ffa040', 'accretion_disk_fragment', light=8, hardness=5.0),
    ORE('einstein_ring_ore', 'Einstein Ring Cluster', 'void_glass', '#181830', '#e0e8ff', 'einstein_ring_lens', light=6, hardness=5.0),
    # the Accretion Disk Reactor's parts
    B('accretion_casing', 'Accretion Casing', 'stellarframe', '#302018', '#c08040'),
    B('horizon_containment', 'Horizon Containment', 'stars', '#000000', '#402040', light=1),
    B('disk_confinement_ring', 'Disk Confinement Ring', 'magcasing', '#402010', '#ffb060', light=6),
    B('jet_collimator', 'Jet Collimator', 'beampipe', '#203048', '#a0e0ff', light=5),
]
FLUIDS = [
    FL('molecular_cloud', 'Molecular Cloud', '#302848', '#a090d0', gas=True, temp=15, density=1, viscosity=200, art='darkgas'),
    FL('interstellar_medium', 'Interstellar Medium', '#182040', '#6080c0', gas=True, temp=8000, density=1, viscosity=200, art='gas'),
    FL('accretion_disk_plasma', 'Accretion Disk Plasma', '#402008', '#ffb050', light=15, temp=10_000_000, density=100, viscosity=800, art='plasma'),
    FL('tidal_plasma', 'Tidal Plasma', '#401008', '#ff7040', light=15, temp=50_000_000, density=100, viscosity=800, art='stellar'),
]

# the Whipple Shield: a suit, registered in Java (InteriorGear.Suit.WHIPPLE)
SUIT = ('whipple_shield', 'Whipple Shield', 'fused_silica', 'minecraft:iron_ingot', '#606870', '#d0d8e0',
        'A thin bumper sheet, a gap, and layers of ceramic cloth: a micrometeorite shatters on the bumper and its spray is caught behind. Each '
        'piece stops a quarter of the Asteroid Belt\'s micrometeorites; a full set, all of them.')
PIECES = [('helmet', 'Helmet', ['BMB', 'B B']), ('chestplate', 'Chestplate', ['B B', 'BMB', 'BBB']),
          ('leggings', 'Leggings', ['BMB', 'B B', 'B B']), ('boots', 'Boots', ['M M', 'B B'])]

KEY_ADD = {
    'а': ['accretion_casing'], 'б': ['accretion_casing', 'interface_port'], 'в': ['horizon_containment'], 'г': ['disk_confinement_ring'],
    'д': ['jet_collimator'],
}


# =================================================================================== THE ACCRETION DISK REACTOR
def accretion_disk_reactor():
    """A sphere of horizon containment holding the hole, a flat confinement ring round its equator where the disk turns, jet
    collimators out of both poles, and the ring standing on four legs above a circular footing."""
    s = shapes.Shape(13, 11, 13)
    c = 6
    for x in range(13):
        for z in range(13):
            d = math.hypot(x - c, z - c)
            if 5.4 <= d <= 6.6:
                s.put(x, 0, z, 'а')                                       # the footing
            if 2.6 <= d <= 6.2:
                s.put(x, 5, z, 'г' if (x + z) % 4 else 'G')               # the disk, with windows to watch it by
            for y in range(2, 9):
                r = math.dist((x, y, z), (c, 5, c))
                if 2.0 <= r <= 3.0 and not (y == 5 and d >= 2.6):
                    # the containment sphere: glass between ribs of horizon containment, the hole and its disk inside
                    s.put(x, y, z, 'в' if (x == c or z == c or y == 5) else 'G')
    for x, z in ((c, 0), (c, 12), (0, c), (12, c)):
        for y in range(1, 5):
            s.put(x, y, z, 'а')                                           # the legs, footing to disk
    for y in (0, 1, 9, 10):
        s.put(c, y, c, 'д')                                               # the jets, out of both poles
    s.center = (6.0, 5.5, 6.0)
    s.ports('а', 'б', 4, y=0)
    s.v[(c, 0, 0)] = 'C'
    s.ctrl = (c, 0, 0)
    return s


shapes.BUILDERS.update({'accretion_disk_reactor': accretion_disk_reactor})

MACHINE_TEXT = {
    'interstellar_scoop': ('Interstellar Scoop', 'Sweeps up the thin gas and dust between the stars. Only high above the world, or out in space.'),
    'accretion_disk_reactor': ('Accretion Disk Reactor', 'Feeds matter onto a captive black hole: falling in, it gives up to a third of its rest '
                                                         'energy as light. Too much at once, and its own light blows the disk away.'),
}
MACHINE_CAT = {'interstellar_scoop': 'astrophysics', 'accretion_disk_reactor': 'astrophysics'}
MACHINE_RESEARCH = {'interstellar_scoop': 'interstellar_medium', 'accretion_disk_reactor': 'accretion_physics'}
TIER = {'interstellar_scoop': 1, 'accretion_disk_reactor': 3}
_S = 'spacetime_casing'
MC = {
    'interstellar_scoop': (['FGF', 'GSG', 'FCF'], {'F': 'fused_silica', 'G': 'minecraft:glass', 'S': _S, 'C': 'minecraft:copper_ingot'}),
    'accretion_disk_reactor': (['JRJ', 'HSH', 'CRC'], {'J': 'jet_collimator', 'R': 'disk_confinement_ring', 'H': 'horizon_containment', 'S': _S,
                                                       'C': 'accretion_casing'}),
}
EXTRA = {
    'interstellar_scoop': 'Needs nothing but to be high: at y 200 or above in the Overworld, or anywhere in the Accretion Frontier or the End. '
                          'It gathers Molecular Cloud gas, Interstellar Medium and Interstellar Dust.',
    'accretion_disk_reactor': 'Set the hole\'s mass, how fast it is fed, and its spin. A spinning hole lets matter orbit closer before it falls, '
                              'so more of its rest energy comes out (6% at no spin, a third near the maximum). The light pushes back on the '
                              'infalling gas: past the Eddington luminosity, L = 1.26×10³¹ (M/M☉) W, it blows the disk apart.',
}

_r12.model('accretion_disk_reactor', 'Accretion Disk Reactor',
           'Gas falling onto a black hole of mass M spirals in through a disk, heating as it goes, until it reaches the innermost stable orbit and '
           'drops in. The energy it has radiated by then is η·ṁc², and η depends on the spin a: the faster the hole spins, the closer that orbit, '
           'the more comes out. But the light pushes on the gas: its luminosity cannot exceed the Eddington limit L_E = 1.26×10³¹ (M/M☉) W.',
           'The feed is clumpy: the accretion rate varies by ±5%·d.',
           [_P('M', 'Hole mass', 'M☉', 1, 30, 0.01, 10, 'The mass of the captive black hole.'),
            _P('md', 'Feed rate', '10¹⁵ kg/s', 0, 20, 0.001, 1, 'How fast matter is fed into the disk.'),
            _P('a', 'Spin a', '', 0, 0.998, 0.001, 0.5, 'How fast the hole turns, from 0 to the 0.998 a disk spins it up to.')],
           [_D('eta', 'Radiative efficiency η', '', '0.057 + 0.27*a^6', 'η(a): 0.057 at a = 0, 0.32 near a = 1',
               'The share of the infalling rest energy radiated before matter crosses the innermost stable orbit.'),
            _D('L', 'Luminosity', '10³¹ W', '9*eta*md*(1 + 0.05*d)', 'L = η·ṁ·c²', 'The light of the disk.'),
            _D('LE', 'Eddington luminosity', '10³¹ W', '1.26*M', 'L_E = 1.26×10³¹ (M/M☉) W', 'Where radiation pressure balances gravity.'),
            _D('lam', 'Eddington ratio', '', 'L/LE', 'λ = L/L_E', 'How close the disk runs to blowing itself apart.')],
           [_C('blowout', 'stop', '1 - lam', 'Past the Eddington luminosity: the disk\'s own light has blown it apart', 'L < L_E'),
            _C('starved', 'stop', 'md - 0.001', 'Nothing is being fed in', 'ṁ > 0')],
           rate='clamp(L/Lref, 0, 2)', rate_formula='output = L / L_rated', rate_desc='Power output follows the luminosity.',
           heat='0.02*L', idle_heat='0', rvars=[_V('Lref', 'Rated luminosity (10³¹ W)', 5)], kappa=60, heat_capacity=200)

_r12.WINDOWS['accretion_disk_reactor'] = {
    'r51_accrete_dust': ({'Lref': 5}, [('lam', 0.2, 0.95)]),
    'r51_accrete_fragment': ({'Lref': 8}, [('lam', 0.3, 0.95)]),
    'r51_accrete_brown_dwarf': ({'Lref': 12}, [('lam', 0.5, 0.95)]),
}
import round45 as _r45
_r12.WINDOWS.setdefault('reflecting_telescope', {}).update({
    'r51_planetary_nebula': ({'mreq': 13}, [('mlim', 13, 99)]),
    'r51_cepheid': ({'mreq': 15}, [('mlim', 15, 99)]),
    'r51_transit': ({'mreq': 11}, [('mlim', 11, 99)]),
})
_r12.WINDOWS.setdefault('radio_telescope', {}).update({
    'r51_galactic_filament': (dict(_r45._LINE, Sreq=40), [('SNR', 40, 1e12), ('inb', 0, 1e9)]),
})
_r12.WINDOWS.setdefault('monte_carlo_engine', {}).update({'r51_cosmic_web': ({'sg': 30, 'opt': 0.6}, [('err', 0, 30)])})

# =================================================================================== RESEARCH
RESEARCH = kit.place([
    ('interstellar_medium', 'Interstellar Medium',
     'Between the stars is not nothing: a few atoms in every cubic centimetre, cold molecular clouds where stars are born, hot ionised bubbles '
     'blown by the ones that died, and dust through all of it. Unlocks the Interstellar Scoop.',
     'interstellar_dust', None, None, 70, ['spectroscopy'], [('spectral_plate', 4), ('deep_sky_plate', 1)]),
    ('exoplanetology', 'Exoplanetology',
     'Most stars have planets. Watch a star\'s light dip as one crosses in front of it, and the dip gives its size; its spectrum through the '
     'planet\'s air gives what it is made of. Unlocks transit photometry and the assay of exoplanet cores.',
     'transit_light_curve', None, None, 70, ['spectroscopy'], [('star_field_plate', 4), ('stellar_spectrum_plate', 2)]),
    ('accretion_physics', 'Accretion Physics',
     'Matter falling onto a compact object turns its gravity into light - up to a third of its rest energy for a spinning black hole, fifty '
     'times better than fusion. The brightest things in the universe are lit this way. Unlocks the Accretion Disk Reactor.',
     'accretion_disk_fragment', None, None, 150, ['compact_stars', 'galactic_dynamics'], [('accretion_disk_fragment', 4), ('pulsar_timing_record', 2)]),
    ('cosmic_web', 'The Cosmic Web',
     'On the largest scales the galaxies are not scattered evenly: they lie along filaments and sheets round enormous voids, the shape gravity '
     'has pulled the dark matter into since the Big Bang. Unlocks the cosmic web simulation.',
     'cosmic_web_filament', None, None, 220, ['galactic_dynamics', 'big_bang_cosmology', 'interstellar_medium'],
     [('galactic_filament', 4), ('redshift_lens', 2)]),
])

PUZZLES = {
    'interstellar_medium': dict(
        goal='A hot young star gives off Q ionising photons a second into gas of n atoms per cubic metre. Each ionised atom recombines at the '
             'rate n·α; write the radius R of the ball of gas the star can keep ionised.',
        name='Strömgren sphere', lhs='R',
        form='( 3 * Q / ( 4 * π * n ^ 2 * α ) ) ^ ( 1 / 3 )',
        expr='( 3 * Q / ( 12.5664 * n ^ 2 * a ) ) ^ 0.333333 / p',
        ask='Find R in parsecs for Q = 1e49 /s, n = 1e7 /m³, a = 2.6e-19 m³/s (one parsec p = 3.086e16 m).',
        vars={'R': 'radius (pc)', 'Q': 'ionising photons per second', 'π': 'pi', 'n': 'atoms per m³', 'α': 'recombination coefficient',
              'a': 'recombination coefficient (m³/s)', 'p': 'one parsec (m)'},
        given={'Q': 1e49, 'n': 1e7, 'a': 2.6e-19, 'p': 3.086e16}, unit='pc',
        method='Balance: every photon ionises one atom, and inside the sphere atoms recombine at n²α per volume. So Q = (4/3)πR³·n²α.',
        distractors=['c', 'h', '2']),
    'exoplanetology': dict(
        goal='A planet of radius r crosses in front of its star, whose radius is R, and blocks part of its light. Write the fraction δ of the '
             'starlight that is lost, as a percentage.',
        name='Transit depth', lhs='δ',
        form='( r / R ) ^ 2',
        expr='( r / R ) ^ 2 * 100',
        ask='Find δ in per cent for a Jupiter-sized planet (r = 7.0e7 m) in front of the Sun (R = 6.96e8 m).',
        vars={'δ': 'depth (%)', 'r': 'planet radius (m)', 'R': 'star radius (m)'},
        given={'r': 7.0e7, 'R': 6.96e8}, unit='%',
        method='The planet\'s disc blocks an area πr² of the star\'s disc πR².',
        distractors=['π', '3', 'G']),
    'accretion_physics': dict(
        goal='Light from gas falling onto a mass M pushes on its electrons (cross-section σ) while gravity pulls on its protons (mass m). Write '
             'the luminosity L at which the two balance, using G and c.',
        name='Eddington luminosity', lhs='L',
        form='4 * π * G * M * m * c / σ',
        expr='12.5664 * G * M * m * c / s / u',
        ask='Find L in units of 1e31 W (u = 1e31) for ten solar masses: M = 1.989e31 kg, m = 1.6726e-27 kg, c = 2.998e8 m/s, s = 6.652e-29 m², '
            'G = 6.674e-11.',
        vars={'L': 'luminosity (10³¹ W)', 'π': 'pi', 'G': 'gravitational constant', 'M': 'mass (kg)', 'm': 'proton mass (kg)', 'c': 'speed of light',
              'σ': 'Thomson cross-section', 's': 'Thomson cross-section (m²)', 'u': '10³¹ W'},
        given={'G': 6.674e-11, 'M': 1.989e31, 'm': 1.6726e-27, 'c': 2.998e8, 's': 6.652e-29, 'u': 1e31}, unit='×10³¹ W',
        method='The radiation force on an electron at distance r is σL/(4πr²c); gravity on its proton is GMm/r². The r² cancels.',
        distractors=['h', 'k', '2']),
    'cosmic_web': dict(
        goal='For the Universe to be exactly flat, the mass in each cubic metre must balance its expansion rate H against gravity\'s constant G. '
             'Write how much mass ρ each cubic metre must hold.',
        name='Critical density', lhs='ρ',
        form='3 * H ^ 2 / ( 8 * π * G )',
        expr='3 * H ^ 2 / ( 25.1327 * G * m )',
        ask='Find ρ in protons per cubic metre for H = 2.27e-18 /s (70 km/s/Mpc), G = 6.674e-11 and a proton mass m = 1.6726e-27 kg.',
        vars={'ρ': 'protons per m³', 'H': 'Hubble constant (1/s)', 'π': 'pi', 'G': 'gravitational constant', 'm': 'proton mass (kg)'},
        given={'H': 2.27e-18, 'G': 6.674e-11, 'm': 1.6726e-27}, unit='protons/m³',
        method='Friedmann\'s equation, H² = 8πGρ/3 − k/a², with the curvature k set to zero.',
        distractors=['c', 'Λ', '2']),
}

SC, AP, AS, SP, RO, MCE = 'interstellar_scooping', 'accretion_power', 'astrophotography', 'spectroscopy', 'radio_observation', 'monte_carlo'
RECIPES = [
    # (name, type, item ins, fluid ins, item outs, fluid outs, FE/t, ticks, research, insight[, catalysts[, chances]])
    ('scoop_molecular_cloud', SC, [], [], [], [('molecular_cloud', 100)], 64, 200, 'interstellar_medium', 0),
    ('scoop_ism', SC, [('minecraft:glass_bottle', 1)], [], [('minecraft:glass_bottle', 1)], [('interstellar_medium', 100)], 64, 200,
     'interstellar_medium', 0),
    ('scoop_dust', SC, [('minecraft:paper', 1)], [], [('interstellar_dust', 1)], [], 64, 400, 'interstellar_medium', 0),
    ('accrete_dust', AP, [('interstellar_dust', 4)], [('molecular_cloud', 250)], [('quasar_jet_filament', 1)], [('accretion_disk_plasma', 100)],
     8192, 1200, 'accretion_physics', 4, [], [0.1]),
    ('accrete_fragment', AP, [('accretion_disk_fragment', 1)], [], [('quasar_jet_filament', 1)], [('accretion_disk_plasma', 250)],
     8192, 1600, 'accretion_physics', 4, [], [0.25]),
    ('accrete_brown_dwarf', AP, [('brown_dwarf_core', 1)], [], [('quasar_jet_filament', 1)], [('accretion_disk_plasma', 500)],
     16384, 2400, 'accretion_physics', 6, [], [0.5]),
    ('planetary_nebula', AS, [('photographic_plate', 1), ('interstellar_dust', 1)], [], [('planetary_nebula_shell', 1)], [], 128, 1200,
     'interstellar_medium', 4),
    ('cepheid', AS, [('photographic_plate', 2)], [], [('cepheid_standard_candle', 1)], [], 128, 1600, 'galactic_dynamics', 5),
    ('transit', AS, [('photographic_plate', 1)], [], [('transit_light_curve', 1)], [], 128, 1200, 'exoplanetology', 5),
    ('galactic_filament', RO, [('hydrogen_line_map', 1)], [], [('galactic_filament', 1)], [], 512, 2000, 'galactic_dynamics', 8),
    ('redshift_lens', SP, [('spectral_plate', 1), ('cepheid_standard_candle', 1)], [], [('redshift_lens', 1)], [], 20000, 400,
     'galactic_dynamics', 0),
    ('exoplanet_core_assay', SP, [('exoplanet_core_sample', 1), ('spectral_plate', 1)], [], [('core_iron', 2), ('ringwoodite', 1)], [], 20000, 300,
     'exoplanetology', 2, [], [1.0, 0.3]),
    ('brown_dwarf_assay', SP, [('brown_dwarf_core', 1), ('spectral_plate', 1)], [], [('rare_earth_oxide', 2), ('interstellar_dust', 2)], [],
     20000, 300, 'exoplanetology', 2),
    ('cosmic_web', MCE, [('galactic_filament', 4), ('redshift_lens', 1)], [], [('cosmic_web_filament', 1)], [], 4096, 2400, 'cosmic_web', 30,
     ['random_number_chip']),
]
ENV = {'scoop_molecular_cloud': {'min_y': 200, 'dimensions': [M + DIM, 'minecraft:the_end']},
       'scoop_ism': {'min_y': 200, 'dimensions': [M + DIM, 'minecraft:the_end']},
       'scoop_dust': {'min_y': 200, 'dimensions': [M + DIM, 'minecraft:the_end']}}
SHAPED = [
    ('light_sail', ['FSF', 'SPS', 'FSF'], {'F': 'fused_silica', 'S': 'minecraft:string', 'P': 'photographic_plate'}, 'light_sail', 1),
    ('accretion_casing', ['III', 'ISI', 'III'], {'I': 'minecraft:iron_ingot', 'S': 'accretion_disk_fragment'}, 'accretion_casing', 4),
    ('horizon_containment', ['OGO', 'GPG', 'OGO'], {'O': 'minecraft:obsidian', 'G': 'containment_field_glass', 'P': 'pulsar_timing_crystal'},
     'horizon_containment', 4),
    ('disk_confinement_ring', ['MMM', 'NAN', 'MMM'], {'M': 'neodymium_magnet', 'N': 'magnetar_crust_fragment', 'A': 'accretion_disk_fragment'},
     'disk_confinement_ring', 4),
    ('jet_collimator', ['MLM', 'M M', 'MLM'], {'M': 'neodymium_magnet', 'L': 'fused_silica_lens'}, 'jet_collimator', 2),
    ('station_hull', ['II', 'II'], {'I': 'minecraft:iron_ingot'}, 'station_hull', 4),
]
SHAPELESS = [
    # starlight and dust are what every planet is made of: dust makes clay again
    ('dust_to_clay', ['interstellar_dust'] * 4 + ['minecraft:water_bucket'], 'minecraft:clay_ball', 4),
    # a die thrown ten thousand times against a perfect bell curve, and filed until every face comes up as often
    ('entropy_die', ['dice_set', 'normal_distribution_crystal', 'minecraft:flint'], 'entropy_die', 1),
]

CREATURES = {
    'nebuloid': ('Nebuloid', [('interstellar_dust', 1, 3, 1.0), ('planetary_nebula_shell', 1, 1, 0.08)]),
    'pulsar_wisp': ('Pulsar Wisp', [('pulsar_timing_crystal', 1, 1, 0.35), ('cosmic_ray_shard', 0, 2, 1.0)]),
    'tidal_stretcher': ('Tidal Stretcher', [('tidal_shear_crystal', 1, 1, 0.5), ('accretion_disk_fragment', 0, 1, 1.0)]),
    'lens_phantom': ('Lens Phantom', [('einstein_ring_lens', 1, 1, 0.4)]),
    'hawking_imp': ('Hawking Imp', [('hawking_quanta', 1, 1, 1.0)]),
    'boltzmann_brain': ('Boltzmann Brain', [('boltzmann_brain', 1, 1, 1.0)]),
    'black_swan': ('Black Swan', [('black_swan_feather', 1, 1, 1.0)]),
    'binary_serpent': ('The Binary', [('chirp_signal_record', 1, 1, 1.0), ('merger_remnant', 1, 1, 0.5)]),
}
SPAWNS = {
    'molecular_cloud_nebula': [('nebuloid', 40, 1, 2), ('hawking_imp', 3, 1, 1), ('boltzmann_brain', 1, 1, 1)],
    'asteroid_belt': [('nebuloid', 8, 1, 1), ('hawking_imp', 4, 1, 1), ('boltzmann_brain', 1, 1, 1)],
    'lensing_void': [('lens_phantom', 40, 1, 2), ('hawking_imp', 3, 1, 1), ('boltzmann_brain', 1, 1, 1)],
    'pulsar_field': [('pulsar_wisp', 40, 1, 3), ('hawking_imp', 3, 1, 1)],
    'accretion_rim': [('tidal_stretcher', 40, 1, 2), ('hawking_imp', 5, 1, 1)],
}

LANG = {
    'message.theoreticalsciences.light_sail_locked': 'You need to research Galactic Dynamics to sail between the stars.',
    'message.theoreticalsciences.light_sail_low': 'The sail hangs limp: unfurl it under open sky, at y 200 or higher, where there is no air to drag on it.',
    'message.theoreticalsciences.frontier_arrival': 'The Accretion Frontier. Far off, a disk of fire turns round something black.',
    'message.theoreticalsciences.frontier_return': 'Starlight carries you home - the sail slows your fall.',
    'message.theoreticalsciences.micrometeorite': 'A micrometeorite strikes!',
    'message.theoreticalsciences.binary_not_here': 'Nothing answers. Play it out in the Accretion Frontier.',
    'message.theoreticalsciences.binary_summoned': 'Two compact stars spiral out of the dark. Kill both before they merge!',
    'message.theoreticalsciences.binary_bar': 'The Binary - separation %s',
    'message.theoreticalsciences.binary_merged': 'The Binary merges - the shockwave hits you!',
    'message.theoreticalsciences.boltzmann.thought_1': '"...was I always here? I remember a childhood I never had..."',
    'message.theoreticalsciences.boltzmann.thought_2': '"...more likely that I fluctuated into being than that you are real..."',
    'message.theoreticalsciences.boltzmann.thought_3': '"...entropy is low, so low, why is it so low..."',
    'message.theoreticalsciences.boltzmann.thought_4': '"...I think, therefore I was, briefly..."',
    'message.theoreticalsciences.boltzmann.thought_5': '"...in an infinite time everything happens, even me..."',
    'message.theoreticalsciences.boltzmann.thought_6': '"...is this the measure problem, or a dream of one..."',
    'tooltip.theoreticalsciences.light_sail': 'Use under open sky at y 200+ to sail to the Accretion Frontier; use there to return.',
    'env.theoreticalsciences.min_y': 'At y %s or higher',
    'env.theoreticalsciences.min_y_or': 'At y %s or higher, or in %s',
    'dimname.theoreticalsciences.accretion_frontier': 'the Accretion Frontier',
    'biome.theoreticalsciences.molecular_cloud_nebula': 'Molecular Cloud Nebula',
    'biome.theoreticalsciences.asteroid_belt': 'Asteroid Belt',
    'biome.theoreticalsciences.pulsar_field': 'Pulsar Field',
    'biome.theoreticalsciences.accretion_rim': 'Accretion Rim',
    'biome.theoreticalsciences.lensing_void': 'Lensing Void',
    'advancements.theoreticalsciences.frontier_arrival.title': 'Under Sail',
    'advancements.theoreticalsciences.frontier_arrival.description': 'Ride starlight to the Accretion Frontier.',
    'entity.theoreticalsciences.binary_serpent': 'The Binary',
}


# =================================================================================== STRUCTURES
def _r39():
    import round39
    return round39


def derelict_station():
    """A research station adrift: a hull cylinder with its windows, a broken solar wing on each side, a docking ring, and a
    containment cache aboard."""
    t = _r39().T(17, 9, 9, 51)
    for x in range(3, 14):
        for y in range(1, 8):
            for z in range(1, 8):
                d = math.hypot(y - 4, z - 4)
                if d <= 3.2:
                    edge = d >= 2.3 or x in (3, 13)
                    if edge:
                        win = x % 3 == 0 and y == 4 and x not in (3, 13)
                        t.set(x, y, z, M + ('leaded_glass' if win else 'station_hull'))
                    else:
                        t.set(x, y, z, 'air')
    for x in range(5, 12, 2):                                             # solar wings, one half broken off
        for y in (3, 4, 5):
            t.set(x, y, 0, M + 'computronium_panel')
            if t.r.random() < 0.6: t.set(x, y, 8, M + 'computronium_panel')
    for y in range(2, 7):                                                 # the docking ring
        for z in range(2, 7):
            if math.hypot(y - 4, z - 4) <= 2.2: t.set(14, y, z, M + 'chronosteel_plating')
    t.set(15, 4, 4, M + 'stellar_lamp')
    t.set(8, 2, 4, M + 'neutronium_tiles')
    t.cache(8, 3, 4, 'derelict_station')
    t.set(6, 2, 4, M + 'cherenkov_lamp')
    t.set(11, 2, 4, M + 'cherenkov_lamp')
    return t


def pulsar_beacon():
    """A navigation beacon set on the Pulsar Field: a tapering magnetite spire, timing lamps up its sides, a crown of glass
    round a radium lamp, and a cache at its foot."""
    t = _r39().T(7, 16, 7, 52)
    for y in range(0, 14):
        r = 3 if y < 2 else 2 if y < 6 else 1
        for x in range(7):
            for z in range(7):
                if abs(x - 3) <= r and abs(z - 3) <= r and (abs(x - 3) == r or abs(z - 3) == r or y == 0):
                    t.set(x, y, z, M + ('magnetite_bricks' if (y + x + z) % 5 else 'cherenkov_lamp'))
                elif abs(x - 3) <= r and abs(z - 3) <= r:
                    t.set(x, y, z, 'air')
    for y in (14, 15):
        for x in (2, 3, 4):
            for z in (2, 3, 4):
                t.set(x, y, z, M + 'containment_field_glass')
    t.set(3, 14, 3, M + 'radium_lamp')
    t.cache(3, 1, 3, 'pulsar_beacon')
    return t


def probability_hall():
    """A village hall of games of chance: chance-tile floor, three tables of dice, a Galton Board to study, and a cache.
    The entrance jigsaw faces west at floor level, as vanilla village houses' do."""
    t = _r39().T(11, 7, 13, 53)
    for x in range(11):
        for z in range(13):
            t.set(x, 0, z, M + 'chance_tiles')
            wall = x in (1, 10) or z in (0, 12)
            if x == 0: continue
            for y in range(1, 6):
                if wall:
                    win = y in (2, 3) and (z % 3 == 1 or x % 3 == 1) and not (x == 1 and z in (5, 6, 7))
                    t.set(x, y, z, M + ('leaded_glass' if win else 'spacetime_bricks'))
                else:
                    t.set(x, y, z, 'air')
            t.set(x, 6, z, M + ('spacetime_tiles_slab' if (x + z) % 2 else 'spacetime_tiles'))
    for y in (1, 2, 3):                                                   # the doorway
        for z in (5, 6, 7):
            t.set(1, y, z, 'air')
    for z in range(13):
        t.set(0, 0, z, M + 'chance_tiles')
    t.set(0, 0, 6, 'jigsaw', {'orientation': 'west_up'},
          {'id': 'minecraft:jigsaw', 'name': 'minecraft:building_entrance', 'target': 'minecraft:building_entrance', 'pool': 'minecraft:empty',
           'joint': 'aligned', 'final_state': M + 'chance_tiles'})
    for x, z in ((4, 3), (4, 9), (7, 6)):                                 # dice tables
        t.set(x, 1, z, M + 'chronosteel_pillar', {'axis': 'y'})
        t.set(x, 2, z, M + 'cosmic_tiles_slab', {'type': 'bottom', 'waterlogged': 'false'})
    t.set(9, 1, 6, M + 'galton_board', {'facing': 'west'})
    t.cache(9, 1, 2, 'probability_hall', style='hilbert_locker')
    for x, z in ((3, 2), (3, 10), (8, 2), (8, 10)):
        t.set(x, 5, z, M + 'cherenkov_lamp')
    return t


SITES = {
    # id: (builder, biomes, spacing, separation, salt, placement)
    'derelict_station': (derelict_station, [M + b for b in BIOMES], 28, 10, 51051051, ('float', 90, 160)),
    'pulsar_beacon': (pulsar_beacon, [M + 'pulsar_field'], 20, 8, 52052052, ('surface',)),
}
LOOT = {
    'derelict_station': [('atomic_clock', 1, 1, 6), ('test_mass', 1, 1, 4), ('photographic_plate', 2, 6, 12), ('fused_silica_lens', 1, 2, 8),
                         ('light_sail', 1, 1, 2), ('whipple_shield_chestplate', 1, 1, 3), ('random_number_chip', 1, 2, 6), ('laser_rod', 1, 1, 4)],
    'pulsar_beacon': [('pulsar_timing_crystal', 1, 3, 12), ('magnetar_crust_fragment', 1, 2, 10), ('cosmic_ray_shard', 2, 5, 12),
                      ('pulsar_timing_record', 1, 1, 6), ('neodymium_magnet', 1, 3, 8)],
    'probability_hall': [('dice_set', 1, 2, 12), ('entropy_die', 1, 1, 6), ('random_number_chip', 1, 2, 6), ('prior_tablet', 1, 2, 8),
                         ('normal_distribution_crystal', 1, 1, 4)],
}


def _structures(g):
    D, w = g['D'], g['w']
    for sid, (fn, biomes, spacing, sep, salt, place) in SITES.items():
        fn().save(os.path.join(D, 'structure', sid + '.nbt'))
        w(os.path.join(D, 'tags', 'worldgen', 'biome', 'has_structure', sid + '.json'), {'replace': False, 'values': biomes})
        w(os.path.join(D, 'worldgen', 'template_pool', sid, 'start.json'), {
            'fallback': 'minecraft:empty',
            'elements': [{'weight': 1, 'element': {'element_type': 'minecraft:single_pool_element', 'location': M + sid,
                                                   'projection': 'rigid', 'processors': 'minecraft:empty'}}]})
        st = {'type': 'minecraft:jigsaw', 'biomes': f'#{NS}:has_structure/{sid}', 'step': 'surface_structures', 'spawn_overrides': {},
              'terrain_adaptation': 'none', 'start_pool': f'{NS}:{sid}/start', 'size': 1, 'max_distance_from_center': 80, 'use_expansion_hack': False}
        if place[0] == 'float':
            st['start_height'] = {'type': 'minecraft:uniform', 'min_inclusive': {'absolute': place[1]}, 'max_inclusive': {'absolute': place[2]}}
        else:
            st['start_height'] = {'absolute': 0}
            st['project_start_to_heightmap'] = 'WORLD_SURFACE_WG'
        w(os.path.join(D, 'worldgen', 'structure', sid + '.json'), st)
        w(os.path.join(D, 'worldgen', 'structure_set', sid + '.json'), {
            'structures': [{'structure': f'{NS}:{sid}', 'weight': 1}],
            'placement': {'type': 'minecraft:random_spread', 'spacing': spacing, 'separation': sep, 'salt': salt}})
    probability_hall().save(os.path.join(D, 'structure', 'probability_hall.nbt'))
    for name, entries in LOOT.items():
        w(os.path.join(D, 'loot_table', 'chests', name + '.json'), {
            'type': 'minecraft:chest',
            'pools': [{'rolls': {'type': 'minecraft:uniform', 'min': 3, 'max': 6},
                       'entries': [{'type': 'minecraft:item', 'name': i if ':' in i else M + i, 'weight': wt,
                                    'functions': [{'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': lo, 'max': hi}}]}
                                   for i, lo, hi, wt in entries]}]})


# =================================================================================== THE DIMENSION
def _rock_rule(biome, block):
    return {'type': 'minecraft:condition', 'if_true': {'type': 'minecraft:biome', 'biome_is': [M + biome]},
            'then_run': {'type': 'minecraft:block', 'result_state': {'Name': M + block}}}


def _dimension(g):
    w, D, WG, q = g['w'], g['D'], g['WG'], g['q']
    import zipfile
    import mcpaths
    with zipfile.ZipFile(mcpaths.CLIENT_JAR) as z:
        ns = json.loads(z.read('data/minecraft/worldgen/noise_settings/floating_islands.json'))
        dt = json.loads(z.read('data/minecraft/dimension_type/the_end.json'))
    ns['default_block'] = {'Name': q('frontier_regolith')}
    ns['default_fluid'] = {'Name': 'minecraft:air'}
    ns['surface_rule'] = {'type': 'minecraft:sequence', 'sequence': [
        {'type': 'minecraft:condition', 'if_true': {'type': 'minecraft:not', 'invert': {'type': 'minecraft:biome', 'biome_is': [M + 'lensing_void']}},
         'then_run': {'type': 'minecraft:condition',
                      'if_true': {'type': 'minecraft:stone_depth', 'offset': 0, 'surface_type': 'floor', 'add_surface_depth': False,
                                  'secondary_depth_range': 0},
                      'then_run': {'type': 'minecraft:block', 'result_state': {'Name': q('frontier_regolith')}}}},
        _rock_rule('molecular_cloud_nebula', 'nebular_ice'), _rock_rule('asteroid_belt', 'asteroid_rock'),
        _rock_rule('pulsar_field', 'pulsar_crust'), _rock_rule('accretion_rim', 'rim_slag'), _rock_rule('lensing_void', 'void_glass')]}
    w(os.path.join(WG, 'noise_settings', DIM + '.json'), ns)
    dt.update({'has_skylight': True, 'fixed_time': 18000, 'ambient_light': 0.08, 'natural': False, 'bed_works': False,
               'respawn_anchor_works': False, 'has_raids': False, 'effects': q(DIM), 'infiniburn': '#minecraft:infiniburn_end'})
    w(os.path.join(D, 'dimension_type', DIM + '.json'), dt)

    def P(temp):
        return {'temperature': list(temp), 'humidity': [-1.0, 1.0], 'continentalness': [-1.0, 1.0], 'erosion': [-1.0, 1.0],
                'weirdness': [-1.0, 1.0], 'depth': [-1.0, 1.0], 'offset': 0.0}
    bands = [(-1.0, -0.45), (-0.45, -0.1), (-0.1, 0.2), (0.2, 0.55), (0.55, 1.0)]
    params = [{'biome': q(b), 'parameters': P(t)} for b, t in zip(BIOMES, bands)]
    w(os.path.join(D, 'dimension', DIM + '.json'), {'type': q(DIM), 'generator': {
        'type': 'minecraft:noise', 'settings': q(DIM), 'biome_source': {'type': 'minecraft:multi_noise', 'biomes': params}}})

    def ore(name, host, block, size, count, lo=0, hi=255):
        g['ore_feature'](name, {'predicate_type': 'minecraft:block_match', 'block': q(host)}, block, size, count, lo, hi)
    ore('ore_dust_ice', 'nebular_ice', 'dust_ice_ore', 12, 14)
    ore('ore_planetesimal_core', 'asteroid_rock', 'planetesimal_core_ore', 8, 10)
    ore('ore_brown_dwarf', 'asteroid_rock', 'brown_dwarf_ore', 5, 4)
    ore('ore_magnetar_crust', 'pulsar_crust', 'magnetar_crust_ore', 7, 8)
    ore('ore_pulsar_timing', 'pulsar_crust', 'pulsar_timing_ore', 5, 6)
    ore('ore_cosmic_ray', 'pulsar_crust', 'cosmic_ray_ore', 7, 8)
    ore('ore_accretion_fragment', 'rim_slag', 'accretion_fragment_ore', 8, 10)
    ore('ore_einstein_ring', 'void_glass', 'einstein_ring_ore', 5, 6)
    # plasma lakes in the Accretion Rim
    for fl, cnt in (('accretion_disk_plasma', 3), ('tidal_plasma', 1)):
        w(os.path.join(WG, 'configured_feature', fl + '_lake.json'), {'type': 'minecraft:lake', 'config': {
            'fluid': {'type': 'minecraft:simple_state_provider', 'state': {'Name': q(fl), 'Properties': {'level': '0'}}},
            'barrier': {'type': 'minecraft:simple_state_provider', 'state': {'Name': q('rim_slag')}}}})
        w(os.path.join(WG, 'placed_feature', fl + '_lake.json'), {'feature': q(fl + '_lake'), 'placement': [
            {'type': 'minecraft:rarity_filter', 'chance': 2}, {'type': 'minecraft:in_square'},
            {'type': 'minecraft:heightmap', 'heightmap': 'WORLD_SURFACE_WG'}, {'type': 'minecraft:count', 'count': cnt}, {'type': 'minecraft:biome'}]})
    ores = {'molecular_cloud_nebula': ['ore_dust_ice'], 'asteroid_belt': ['ore_planetesimal_core', 'ore_brown_dwarf'],
            'pulsar_field': ['ore_magnetar_crust', 'ore_pulsar_timing', 'ore_cosmic_ray'], 'accretion_rim': ['ore_accretion_fragment'],
            'lensing_void': ['ore_einstein_ring']}
    look = {  # sky, fog, particle, probability
        'molecular_cloud_nebula': (0x302848, 0x5a4878, 'minecraft:white_ash', 0.02),
        'asteroid_belt': (0x101014, 0x202024, 'minecraft:ash', 0.004),
        'pulsar_field': (0x081828, 0x103050, 'minecraft:electric_spark', 0.004),
        'accretion_rim': (0x301008, 0x602810, 'minecraft:flame', 0.002),
        'lensing_void': (0x000004, 0x080810, 'minecraft:reverse_portal', 0.003),
    }
    for b in BIOMES:
        feats = [[] for _ in range(11)]
        feats[6] = [q(x) for x in ores[b]]
        if b == 'accretion_rim': feats[1] = [q('accretion_disk_plasma_lake'), q('tidal_plasma_lake')]
        sky, fog, part, prob = look[b]
        w(os.path.join(WG, 'biome', b + '.json'), {
            'has_precipitation': False, 'temperature': 0.5, 'downfall': 0.0,
            'effects': {'sky_color': sky, 'fog_color': fog, 'water_color': 0x3f76e4, 'water_fog_color': 0x050533,
                        'particle': {'options': {'type': part}, 'probability': prob}},
            'spawners': {'monster': [{'type': q(e), 'weight': wt, 'minCount': lo, 'maxCount': hi} for e, wt, lo, hi in SPAWNS[b]]},
            'spawn_costs': {}, 'carvers': {}, 'features': feats})
    w(os.path.join(D, 'advancement', 'frontier_arrival.json'), {
        'display': {'icon': {'id': q('light_sail')}, 'title': {'translate': 'advancements.theoreticalsciences.frontier_arrival.title'},
                    'description': {'translate': 'advancements.theoreticalsciences.frontier_arrival.description'}, 'frame': 'goal',
                    'show_toast': True, 'announce_to_chat': True, 'hidden': False},
        'parent': f'{NS}:research/galactic_dynamics',
        'criteria': {'arrived': {'trigger': 'minecraft:changed_dimension', 'conditions': {'to': q(DIM)}}}, 'requirements': [['arrived']]})


def _suit(g):
    A, LANG_, shaped = g['A'], g['LANG'], g['shaped']
    sid, name, mat, body, c1, c2, lore = SUIT
    LANG_[f'tooltip.{NS}.{sid}'] = lore
    for p, pname, pat in PIECES:
        iid = f'{sid}_{p}'
        LANG_[f'item.{NS}.{iid}'] = f'{name} {pname}'
        g['w'](os.path.join(A, 'models', 'item', iid + '.json'), {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'{NS}:item/{iid}'}})
        shaped('r51_' + iid, pat, {'M': mat, 'B': body}, iid, 1)
    for p, _, _ in PIECES:
        slot = {'helmet': 'head', 'chestplate': 'chest', 'leggings': 'legs', 'boots': 'foot'}[p]
        kit.extend_tag(g, os.path.join(g['RES'], 'data', 'minecraft', 'tags', 'item', f'{slot}_armor.json'), [f'{NS}:{sid}_{p}'])


def _recipes(g):
    kit.recipes(g, 'r51_', RECIPES)
    for name, env in ENV.items():
        p = os.path.join(g['REC'], 'interstellar_scooping', 'r51_' + name + '.json')
        j = json.load(open(p))
        j['environment'] = env
        g['w'](p, j)
    kit.crafting(g, 'r51_', SHAPED, SHAPELESS)


def apply(g):
    g['LANG'].update(LANG)
    kit.write_puzzles('r51', PUZZLES)
    kit.write_research(g, RESEARCH)
    _recipes(g)
    _dimension(g)
    _structures(g)
    _suit(g)


def apply_late(g):
    kit.pickaxe(g, BLOCKS)
    kit.extend_tag(g, os.path.join(g['D'], 'tags', 'block', 'magnetic.json'), [M + 'pulsar_crust', M + 'magnetar_crust_ore'])
    kit.extend_tag(g, os.path.join(g['D'], 'tags', 'item', 'ferromagnetic.json'), [M + 'magnetar_crust_fragment'])


def finish(g):
    te = g['text_entry']
    te('world', 'r51_frontier', 'The Accretion Frontier', 'light_sail', [
        'Out past the edge of the sky, round a black hole feeding on a disk of its own fire, islands of rock and ice drift in the dark. The '
        '$(thing)Accretion Frontier$() is reached with a $(item)Light Sail$() (research $(thing)Galactic Dynamics$()): climb to y 200 or higher, '
        'stand under open sky, and unfurl it. Unfurled again in the Frontier, it brings you home and slows your fall.',
        'It has five regions. The $(thing)Molecular Cloud Nebula$(), cold ice and dust where stars are born: you cannot see far. The '
        '$(thing)Asteroid Belt$(), where you weigh almost nothing - and $(thing)micrometeorites$() strike anyone not wearing a '
        '$(item)Whipple Shield$() (a quarter stopped per piece; a full set stops all).',
        'The $(thing)Pulsar Field$(): crust of neutron-star iron, and a pulse of radiation every five seconds, on the beat, for anyone under open '
        'sky. Gravity is high and time runs slow (Time Dilation). The $(thing)Accretion Rim$(): slag lakes of disk plasma; it is hot enough to '
        'burn, and the disk\'s tidal pull stretches you. The $(thing)Lensing Void$(): black glass, sight lines bent, and frame-dragging currents '
        'that push you sideways.',
        'Its ores: $(item)Interstellar Dust$() in the nebula\'s ice, $(item)Exoplanet Core Samples$() and $(item)Brown Dwarf Cores$() in the '
        'asteroids, $(item)Magnetar Crust$(), $(item)Pulsar Timing Crystals$() and $(item)Cosmic Ray Shards$() in the pulsar crust, '
        '$(item)Accretion Disk Fragments$() in the slag, and $(item)Einstein Ring Lenses$() in the void glass. Derelict Stations drift between '
        'the islands; Pulsar Beacons stand on the field.'],
        adv='galactic_dynamics', sort=7, spot=['light_sail', 'whipple_shield_chestplate', 'pulsar_timing_crystal'])
    te('world', 'r51_frontier_life', 'Life on the Frontier', 'nebuloid_spawn_egg', [
        'Each of the Frontier\'s creatures acts out its physics. The $(thing)Nebuloid$() is a drifting knot of gas and dust; inside it you are '
        'blind. The $(thing)Pulsar Wisp$() pulses on an exact beat, once every one and a half seconds, and its pulse hurts and irradiates anyone '
        'it can see within ten blocks - dodge behind something between pulses.',
        'The $(thing)Tidal Stretcher$() pulls you in harder the closer you are (a tidal force grows as 1/r³) and stretches what it grabs. The '
        '$(thing)Lens Phantom$() is never where it appears: its light reaches you bent round the void\'s mass, so its image stands a block or two '
        'from its body. Aim beside it.',
        'The $(thing)Hawking Imp$() is a tiny black hole. It evaporates, and the smaller it gets the hotter and faster it burns (T ∝ 1/M) - until '
        'it pops. The $(thing)Boltzmann Brain$() fluctuates out of nothing, mutters, and is gone within half a minute unless caught.',
        '$(thing)The Binary$(): play a Binary Black Hole Merger record - or a Gravitational Wave Echo - in the Frontier and two compact stars spiral out of the dark. Their '
        'gravitational waves carry off their orbit: they close in (da/dt ∝ −1/a³) and circle ever faster. Kill both before they meet - if they '
        'merge, the shockwave hits everything nearby, and nothing is left to loot.'],
        adv='galactic_dynamics', sort=8, spot=['nebuloid_spawn_egg', 'pulsar_wisp_spawn_egg', 'lens_phantom_spawn_egg'])
    te('astrophysics', 'r51_ism', 'Between the Stars', 'interstellar_scoop', [
        'The $(item)Interstellar Scoop$() gathers what lies between the stars - but only where the air will not swamp it: at y 200 or above in '
        'the Overworld, or anywhere in the Accretion Frontier or the End. It gathers $(fluid)Molecular Cloud$() gas, $(fluid)Interstellar '
        'Medium$() (into glass bottles) and $(item)Interstellar Dust$() (onto paper).',
        'A hot star ionises the gas round it out to a sphere where recombination keeps up: $(thing)Q = (4/3)πR³n²α$(). Photograph a dying star\'s '
        'shell with dust and a plate: a $(item)Planetary Nebula Shell$(). Four dust and a bucket of water make clay again - every rocky planet '
        'began as dust.'],
        adv='interstellar_medium', sort=20, spot=['interstellar_scoop', 'interstellar_dust', 'planetary_nebula_shell'])
    te('astrophysics', 'r51_exoplanets', 'Other Worlds', 'transit_light_curve', [
        'A planet crossing its star blocks a fraction $(thing)δ = (r/R)²$() of its light: a Jupiter dims the Sun by one per cent, an Earth by '
        'one part in twelve thousand. The Reflecting Telescope records a $(item)Transit Light Curve$().',
        'In the Accretion Frontier\'s Asteroid Belt lie the cores of shattered planets that formed round other stars. The Spectrograph assays an '
        '$(item)Exoplanet Core Sample$() into iron-nickel and ringwoodite, and a $(item)Brown Dwarf Core$() - a failed star - into rare earths '
        'and dust.'],
        adv='exoplanetology', sort=21, spot=['transit_light_curve', 'exoplanet_core_sample', 'brown_dwarf_core'])
    te('astrophysics', 'r51_accretion', 'The Accretion Disk Reactor', 'accretion_disk_reactor', [
        'Matter falling onto a black hole gives up energy as it spirals in - η of its rest energy, from 6% for a hole that does not spin to a '
        'third for one spinning at the limit. Fusion manages 0.7%.',
        'The $(item)Accretion Disk Reactor$() holds a black hole in a sphere of horizon containment, its disk turning in the confinement ring. '
        'Set the hole\'s mass, the feed rate and the spin. Power follows the luminosity $(thing)L = η·ṁc²$() - but the light pushes back on the '
        'gas, and past the $(thing)Eddington luminosity$(), L = 1.26×10³¹ (M/M☉) W, the disk is blown away and the reactor stops.',
        'It burns Interstellar Dust with Molecular Cloud gas, Accretion Disk Fragments, or a whole Brown Dwarf Core, leaving $(fluid)Accretion '
        'Disk Plasma$() - and now and then a $(item)Quasar Jet Filament$() from the jets out of its poles.'],
        adv='accretion_physics', sort=22, spot=['accretion_disk_reactor', 'quasar_jet_filament', 'accretion_disk_fragment'])
    te('astrophysics', 'r51_cosmic_web', 'The Cosmic Web', 'cosmic_web_filament', [
        'Cepheid variables pulse with a period that gives their true brightness: the first rung of the distance ladder. A Spectrograph sets a '
        '$(item)Cepheid Standard Candle$() against a spectral plate: a $(item)Redshift Lens$() - every galaxy is receding, faster the farther.',
        'A long survey of the hydrogen line maps a $(item)Galactic Filament$(). Four of them and a Redshift Lens, run through the Monte Carlo '
        'Engine, trace the $(item)Cosmic Web$(): the filaments and voids gravity has drawn the dark matter into since the Big Bang.',
        'For the Universe to be flat, it must hold the $(thing)critical density$() ρ = 3H²/8πG - about five protons in every cubic metre.'],
        adv='cosmic_web', sort=23, spot=['cosmic_web_filament', 'redshift_lens', 'cepheid_standard_candle'])
