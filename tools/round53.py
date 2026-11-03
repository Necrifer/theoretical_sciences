"""
Round 53 (1.12.0, "Earth & Spacetime"): the rest of the 1.1 design's Physical & Technical Geography and General Relativity.

Geography: Hydrology (the Aquifer Pump, Darcy's law), Climatology (the Climate Engine, radiative forcing ΔF = 5.35 ln(C/C₀);
it sets the weather), Remote Sensing (a survey satellite, flown from where the Interstellar Scoop works) and Planetary
Geology (planetary cores).
Relativity: Geodesics, Gravitational Lensing (the Lensing Concentrator, a generator that focuses starlight round a mass),
Frame Dragging (the Precision Gyroscope - the counter to Frame Drag and Tidal Strain), Tidal Physics, Binary Mergers (and
The Binary, 1.10.0), the Time Dilation Chamber, and the Alcubierre Metric: the Warp Field Forge and the Warp Bubble Core,
good for one return trip.
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
    # geography
    I('aquifer_sample', 'Aquifer Sample', 'contained', '#203040', '#60a0d0',
      'Water from a confined aquifer, tens of thousands of years old: rain from the last ice age, still on its way.'),
    I('ice_core', 'Ice Core', 'crystal', '#a0c0e0', '#ffffff',
      'A cylinder of ice drilled from a glacier. Every layer is a year of snow, and its trapped bubbles are samples of the air of that year.'),
    I('permafrost_sample', 'Permafrost Sample', 'chunk', '#403830', '#a0b0c0',
      'Soil frozen for thousands of years, holding the carbon of the plants that grew before it froze.'),
    I('survey_satellite', 'Survey Satellite', 'satellite', '#606870', '#ffd060',
      'A telescope pointed down instead of up, with solar wings and a radio. Flown high enough, it photographs the ground.'),
    I('satellite_image_plate', 'Satellite Image Plate', 'plate', '#203020', '#a0e080',
      'The ground seen from orbit, in visible and infrared: rivers, faults, and the heat of what lies under the surface.'),
    I('lithospheric_plate_fragment', 'Lithospheric Plate Fragment', 'chunk', '#403020', '#a08060',
      'Rock from the base of a tectonic plate, where the cold, rigid lithosphere rides on the soft mantle below.'),
    I('planetary_core_sample', 'Planetary Core Sample', 'quarkstar', '#302010', '#e0b060',
      'Iron and nickel from two planets\' cores, pressed together to the pressure at a core\'s centre: the stuff every rocky planet sinks '
      'to its middle.'),
    # relativity
    I('strain_sensor', 'Strain Sensor', 'chip', '#202838', '#ff5060',
      'A fused-silica fibre that changes its light when it stretches by a part in a billion.'),
    I('precision_gyroscope', 'Precision Gyroscope', 'loop', '#606870', '#e0e8ff',
      'A fused-quartz sphere, the roundest object ever made, spinning in a vacuum. Space itself turns its axis. Carried in the off hand, it '
      'holds you steady against Frame Drag and Tidal Strain.'),
    I('geodesic_compass', 'Geodesic Compass', 'lens', '#203040', '#a0e0ff',
      'A compass that points along the straightest path through curved spacetime - which is the path a falling body takes.'),
    I('lense_thirring_vortex_core', 'Lense-Thirring Vortex Core', 'ghostorb', '#101830', '#80c0ff',
      'A knot of spacetime twisted round by a spinning mass: anything near it is dragged round with it.'),
    I('gravitational_wave_echo', 'Gravitational Wave Echo', 'codex', '#081020', '#c0e0ff',
      'Faint repeats after a merger\'s ringdown - if a horizon is not quite a horizon, the waves bounce. Play it in the Accretion Frontier '
      'and something answers.'),
    I('negative_energy_lattice', 'Negative Energy Lattice', 'lattice', '#100818', '#ff60ff',
      'Casimir cavities stacked by the million: in each, the vacuum between the plates holds less energy than the vacuum outside.'),
    I('warp_bubble_core', 'Warp Bubble Core', 'warp', '#180828', '#c080ff',
      'A bubble of flat space ready to be pushed through curved space. Used, it anchors where you stand; the anchored bubble brings you back '
      'there once.', special='warpcore'),
    I('anchored_warp_bubble', 'Anchored Warp Bubble', 'warp', '#281048', '#ffa0ff',
      'A warp bubble tied to the place it was anchored. Used, it carries you back there and is spent.', special='anchoredwarp', stack=1),
]
BLOCKS = [
    B('climate_casing', 'Climate Engine Casing', 'casing', '#405060', '#a0d0ff'),
    B('aerosol_sprayer', 'Aerosol Sprayer', 'vent', '#506070', '#e0f0ff', light=2),
    B('cloud_seeder_stack', 'Cloud Seeder Stack', 'emitterblock', '#405870', '#c0e0ff', light=4),
    B('radiometer_mast', 'Radiometer Mast', 'detector', '#304050', '#ffd080', light=3),
    B('lens_pylon', 'Lens Pylon', 'casing2', '#202838', '#c0d0ff'),
    B('lens_array_block', 'Lens Array', 'lensarray', '#181830', '#e0e8ff', light=8),
    B('focus_receiver', 'Focus Receiver', 'injector', '#302820', '#ffe0a0', light=6),
    B('chamber_casing', 'Time Dilation Chamber Casing', 'chrono', '#403020', '#ffd080'),
    B('dense_mass_core', 'Dense Mass Core', 'stars', '#000000', '#ffd080', light=2),
    B('warp_casing', 'Warp Forge Casing', 'casing2', '#201030', '#c080ff'),
    B('warp_coil_ring', 'Warp Coil Ring', 'coilblock', '#301048', '#ff80ff', light=7),
    B('bubble_pedestal', 'Bubble Pedestal', 'injector', '#281840', '#e0a0ff', light=5),
]
FLUIDS = [
    FL('supercritical_water', 'Supercritical Water', '#304050', '#c0e0ff', temp=650, density=300, viscosity=300, art='condensate'),
    FL('strained_spacetime', 'Strained Spacetime', '#100820', '#a060ff', light=6, temp=300, density=500, viscosity=2000, art='exotic'),
]

KEY_ADD = {
    'ц': ['climate_casing'], 'ч': ['climate_casing', 'interface_port'], 'ш': ['aerosol_sprayer'], 'щ': ['cloud_seeder_stack'],
    'ы': ['radiometer_mast'],
    'ь': ['lens_pylon'], 'э': ['lens_pylon', 'interface_port'], 'ю': ['lens_array_block'], 'я': ['focus_receiver'],
    'ё': ['chamber_casing'], 'ђ': ['chamber_casing', 'interface_port'], 'ѓ': ['dense_mass_core'],
    'є': ['warp_casing'], 'ѕ': ['warp_casing', 'interface_port'], 'і': ['warp_coil_ring'], 'ї': ['bubble_pedestal'],
}


# =================================================================================== SHAPES
def climate_engine():
    """A low hall of instruments with aerosol sprayers at its four corners, and from its middle a tall cloud-seeder stack
    with radiometer arms near the top."""
    s = shapes.Shape(9, 13, 9)
    c = 4
    s.box(0, 0, 0, 8, 0, 8, 'ц')
    s.box(0, 1, 0, 8, 3, 8, 'ц', hollow=True)
    for x, z in ((0, 3), (0, 5), (8, 3), (8, 5), (3, 0), (5, 0), (3, 8), (5, 8)):
        s.put(x, 2, z, 'G')
    s.box(1, 1, 1, 7, 3, 7, '.')
    s.box(0, 4, 0, 8, 4, 8, 'ц')
    for x, z in ((0, 0), (8, 0), (0, 8), (8, 8)):
        s.put(x, 5, z, 'ш')                                               # aerosol sprayers
    s.column(c, c, 1, 12, 'щ')                                            # the stack
    for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        for i in (1, 2):
            s.put(c + dx * i, 10, c + dz * i, 'ы')                         # radiometer arms
    s.center = (4.0, 8.0, 4.0)
    s.ports('ц', 'ч', 4, y=0)
    s.v[(c, 1, 0)] = 'C'
    s.ctrl = (c, 1, 0)
    return s


def lensing_concentrator():
    """Four lens pylons round a ring, each holding a lens array at its top, aimed at a receiver on a short tower in the
    middle: an array that focuses, as a mass focuses the light behind it."""
    s = shapes.Shape(11, 9, 11)
    c = 5
    for x in range(11):
        for z in range(11):
            d = math.hypot(x - c, z - c)
            if 3.5 <= d <= 5.4:
                s.put(x, 0, z, 'ь')
    for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1), (1, 1), (-1, -1), (1, -1), (-1, 1)):
        r = 4 if dx == 0 or dz == 0 else 3
        x, z = c + dx * r, c + dz * r
        h = 6 if dx == 0 or dz == 0 else 4
        s.column(x, z, 1, h - 1, 'ь')
        s.put(x, h, z, 'ю')                                                # the lens arrays
    s.box(c - 1, 0, c - 1, c + 1, 0, c + 1, 'ь')
    s.column(c, c, 1, 3, 'ь')
    s.put(c, 4, c, 'я')                                                   # the receiver
    s.center = (5.0, 6.0, 5.0)
    s.ports('ь', 'э', 4, y=0)
    s.v[(c, 0, 0)] = 'C'
    s.ctrl = (c, 0, 0)
    return s


def time_dilation_chamber():
    """An octagonal room round a core of dense mass: the floor, one storey of chamber per layer (1 to 7) with windows to
    look in, and a stepped crown over it with a lantern at the top where the core's warped light shows."""
    s = shapes.Shape(7, 5, 7)
    c = 3
    inside = lambda x, z: abs(x - c) + abs(z - c) <= 4
    for x in range(7):
        for z in range(7):
            if not inside(x, z): continue
            s.put(x, 0, z, 'ё')
            edge = x in (0, 6) or z in (0, 6) or abs(x - c) + abs(z - c) == 4
            s.put(x, 1, z, ('G' if (x == c or z == c) else 'ё') if edge else '.')
            s.put(x, 2, z, 'ё')
            if abs(x - c) + abs(z - c) <= 2: s.put(x, 3, z, 'ё')
    s.put(c, 0, c, 'ѓ')
    s.put(c, 1, c, 'ѓ')
    s.put(c, 3, c, 'G')
    s.put(c, 4, c, 'G')
    s.repeat = (1, 1, 6)
    s.center = (3.0, 1.5, 3.0)
    s.ports('ё', 'ђ', 4, y=0)
    s.v[(c, 1, 0)] = 'G'
    s.v[(c, 0, 0)] = 'C'
    s.ctrl = (c, 0, 0)
    return s


def warp_field_forge():
    """A great ring of warp coil standing on its edge over a long base, a pedestal under its centre where the bubble forms,
    and coil banks along both sides."""
    s = shapes.Shape(11, 11, 5)
    c, zc = 5, 2
    s.box(0, 0, 0, 10, 0, 4, 'є')
    for x in range(11):
        for y in range(1, 11):
            r = math.hypot(x - c, y - 5.5)
            if 4.0 <= r <= 4.9:
                s.put(x, y, zc, 'і')
    for x in (1, 9):
        for z in (0, 4):
            s.column(x, z, 1, 2, 'є')
            s.put(x, 3, z, 'і')
    s.put(c, 1, zc, 'ї')
    s.center = (5.0, 5.5, 2.0)
    s.ports('є', 'ѕ', 4, y=0)
    s.v[(c, 0, 0)] = 'C'
    s.ctrl = (c, 0, 0)
    return s


shapes.BUILDERS.update({'climate_engine': climate_engine, 'lensing_concentrator': lensing_concentrator,
                        'time_dilation_chamber': time_dilation_chamber, 'warp_field_forge': warp_field_forge})

MACHINE_TEXT = {
    'aquifer_pump': ('Aquifer Pump', 'Draws water from the rock it stands on - and, in the Planetary Interior, brine and hotter things.'),
    'climate_engine': ('Climate Engine', 'Changes the air\'s carbon dioxide and aerosols, and with them how much heat the world keeps: enough to '
                                         'bring rain, clear the sky, or grow ice.'),
    'lensing_concentrator': ('Lensing Concentrator', 'Lines a star up behind a mass and collects the light the mass bends together: an '
                                                     'Einstein ring, made into power.'),
    'time_dilation_chamber': ('Time Dilation Chamber', 'Holds its samples deep in the gravity well of a dense core, where their clocks run slow '
                                                       'against the world outside.'),
    'warp_field_forge': ('Warp Field Forge', 'Shapes a bubble of flat space inside a wall of negative energy: contracted ahead, expanded behind.'),
}
MACHINE_CAT = {'aquifer_pump': 'geography', 'climate_engine': 'geography', 'lensing_concentrator': 'spacetime',
               'time_dilation_chamber': 'spacetime', 'warp_field_forge': 'spacetime'}
MACHINE_RESEARCH = {'aquifer_pump': 'hydrology', 'climate_engine': 'climatology', 'lensing_concentrator': 'gravitational_lensing',
                    'time_dilation_chamber': 'gravitational_time_dilation', 'warp_field_forge': 'alcubierre_metric'}
TIER = {'aquifer_pump': 1, 'climate_engine': 2, 'lensing_concentrator': 3, 'time_dilation_chamber': 2, 'warp_field_forge': 4}
_S = 'spacetime_casing'
MC = {
    'aquifer_pump': (['IPI', 'BSB', 'IPI'], {'I': 'minecraft:iron_ingot', 'P': 'minecraft:piston', 'B': 'minecraft:bucket', 'S': _S}),
    'climate_engine': (['ASA', 'RCR', 'ASA'], {'A': 'aerosol_sprayer', 'S': 'cloud_seeder_stack', 'R': 'radiometer_mast', 'C': _S}),
    'lensing_concentrator': (['LLL', 'PRP', 'PSP'], {'L': 'lens_array_block', 'P': 'lens_pylon', 'R': 'focus_receiver', 'S': _S}),
    'time_dilation_chamber': (['CMC', 'MSM', 'CMC'], {'C': 'chamber_casing', 'M': 'dense_mass_core', 'S': _S}),
    'warp_field_forge': (['WWW', 'PSP', 'CCC'], {'W': 'warp_coil_ring', 'P': 'bubble_pedestal', 'S': _S, 'C': 'warp_casing'}),
}
EXTRA = {
    'aquifer_pump': 'Pumps Water anywhere. In the Planetary Interior it reaches confined aquifers (an Aquifer Sample and Deep Brine), and in '
                    'its Magma Chambers, Supercritical Water - water above 647 K and 22 MPa, neither liquid nor gas.',
    'climate_engine': 'Set the carbon dioxide and the aerosols. Radiative forcing ΔF = 5.35 ln(C/280 ppm) W/m², less what the aerosols '
                      'reflect; the world warms by about 0.8 K for each W/m². Warmer air holds more water: rain, then storms. Colder grows ice. '
                      'The weather recipes set the weather (the whole world\'s) when they finish.',
    'lensing_concentrator': 'Works only at night under open sky. Choose the lens mass, its distance, and how closely the star is aligned '
                            'behind it (u, in Einstein radii). The magnification is A = (u² + 2)/(u√(u² + 4)) - huge as u → 0, but the '
                            'Einstein radius of a small, distant lens is so tiny that the aim\'s own jitter sets how close you can get.',
    'time_dilation_chamber': 'Set the core\'s mass and how close to it the samples are held. Their clocks run slow by 1/√(1 − r_s/r): '
                             'near the horizon, very slow - work done here is done many times over, as seen from outside. Closer than the '
                             'horizon and they fall in. Taller chambers hold more.',
    'warp_field_forge': 'Set the bubble\'s speed (in c), its radius and the sharpness of its wall. The negative energy it needs grows as '
                        'v²R²σ; the tidal shear at the wall as vσ (a passenger is torn apart above 5); a wall too thick for its radius '
                        '(Rσ < 10) leaks. A Warp Bubble Core must be superluminal.',
}

_r12.model('climate_engine', 'Climate Engine',
           'Carbon dioxide absorbs the infrared the ground gives off: more of it, more heat kept, by a radiative forcing ΔF = 5.35 ln(C/C₀) W/m² '
           '(C₀ = 280 ppm). Aerosols reflect sunlight back out, about −20 W/m² per unit of optical depth. The world\'s temperature follows, '
           'ΔT = λΔF, with a climate sensitivity λ near 0.8 K per W/m².',
           'Feedbacks: the sensitivity varies by ±10%·d.',
           [_P('Cc', 'Carbon dioxide', 'ppm', 150, 1500, 0.1, 400, 'How much carbon dioxide the engine holds in the air.'),
            _P('aer', 'Aerosol optical depth', '', 0, 1, 0.001, 0.1, 'How much sunlight its aerosols reflect.')],
           [_D('dF', 'Radiative forcing', 'W/m²', '5.35*ln(Cc/280) - 20*aer', 'ΔF = 5.35 ln(C/C₀) − 20τ', 'The change in the heat the world keeps.'),
            _D('dT', 'Warming', 'K', '0.8*(1 + 0.1*d)*dF', 'ΔT = λΔF', 'How much warmer (or colder) it gets.')],
           [_C('runaway', 'damage', '6 - dT', 'Runaway warming: the engine\'s own feedbacks are tearing it apart', 'ΔT < 6 K')],
           rate='clamp(1.5 - abs(dF)/20, 0.5, 1.5)', rate_formula='rate = 1.5 − |ΔF|/20', rate_desc='Small changes settle faster.',
           heat='0', idle_heat='0', kappa=40, heat_capacity=40)

_r12.model('lensing_concentrator', 'Lensing Concentrator',
           'A mass M at distance D_l bends the light of a star behind it (at 8 kpc) into a ring of angular radius θ_E = √(4GM/c² · D_ls/D_lD_s). '
           'A star a distance u Einstein radii off the line is magnified A = (u² + 2)/(u√(u² + 4)). The aim jitters by 0.3 milliarcseconds, '
           'so a small, distant lens - a small θ_E - cannot be aimed closely. Starlight only: at night, under open sky.',
           'Pointing jitter: ±0.3 mas·d.',
           [_P('Ml', 'Lens mass', 'M☉', 0.1, 100, 0.01, 1, 'The mass that does the focusing.'),
            _P('Dl', 'Lens distance', 'kpc', 0.1, 7.9, 0.01, 4, 'How far away the lens is (the star is at 8 kpc).'),
            _P('u', 'Alignment', 'θ_E', 0.01, 2, 0.0001, 1, 'How far the star is from exactly behind the lens, in Einstein radii.')],
           [_D('thE', 'Einstein radius', 'mas', '2.85*sqrt(Ml*(8 - Dl)/(8*Dl))', 'θ_E = √(4GM D_ls/(c² D_l D_s))', 'The ring\'s angular radius.'),
            _D('ue', 'Effective alignment', 'θ_E', 'sqrt(u^2 + (0.3*d/thE)^2)', 'u ⊕ jitter/θ_E', 'The alignment, blurred by the aim.'),
            _D('A', 'Magnification', '', '(ue^2 + 2)/(ue*sqrt(ue^2 + 4))', 'A = (u² + 2)/(u√(u² + 4))', 'How much brighter the star appears.')],
           [_C('day', 'stop', 'night - 0.5', 'No starlight to focus: it is daytime', 'night'),
            _C('roof', 'stop', 'sky - 0.5', 'Its lenses cannot see the sky', 'open sky')],
           rate='clamp(A/Aref, 0, 2)', rate_formula='output = A / A_rated', rate_desc='Power follows the magnification.',
           heat='0', idle_heat='0', rvars=[_V('Aref', 'Rated magnification', 5)], kappa=40, heat_capacity=40)

_r12.model('time_dilation_chamber', 'Time Dilation Chamber',
           'A core of mass M has a Schwarzschild radius r_s = 2GM/c². Held at r from it, a sample\'s clock runs slower than the world\'s by '
           '1/√(1 − r_s/r). The work the chamber does in its own time is that many times more, measured outside.',
           'The core\'s mass flickers by ±1%·d.',
           [_P('Mc', 'Core mass', '10²⁴ kg', 0.1, 1000, 0.01, 10, 'The mass at the chamber\'s heart.'),
            _P('r', 'Sample distance', 'mm', 1, 2000, 0.01, 100, 'How close to the core the samples are held.')],
           [_D('rs', 'Schwarzschild radius', 'mm', '1.485*Mc*(1 + 0.01*d)', 'r_s = 2GM/c²', 'The horizon\'s radius.'),
            _D('f', 'Time dilation', '×', '1/sqrt(pos(1 - rs/r) + 1e-9)', '1/√(1 − r_s/r)', 'How much slower the samples\' clocks run.')],
           [_C('fallen', 'damage', 'r - 1.02*rs', 'The samples have fallen through the horizon', 'r > r_s')],
           rate='clamp(f/3*(0.7 + 0.1*size), 0.25, 2)', rate_formula='rate = f/3 · (0.7 + 0.1·storeys)',
           rate_desc='More dilation, and a bigger chamber, do more.',
           heat='0.01*f', idle_heat='0', kappa=40, heat_capacity=60)

_r12.model('warp_field_forge', 'Warp Field Forge',
           'Alcubierre\'s metric moves a bubble of flat space by contracting space ahead of it and expanding it behind, at any speed v. Its wall '
           'needs negative energy, growing as v²R²σ for radius R and wall sharpness σ; the tidal shear across the wall goes as vσ; a wall '
           'that is thick for its bubble (Rσ small) is not a bubble at all.',
           'Casimir supply: the negative energy needed varies by ±5%·d.',
           [_P('v', 'Bubble speed', 'c', 0.1, 10, 0.001, 0.5, 'How fast the bubble is to move, in units of the speed of light.'),
            _P('R', 'Bubble radius', 'm', 1, 100, 0.01, 5, 'The size of the flat region inside.'),
            _P('sg', 'Wall sharpness σ', '1/m', 0.1, 10, 0.001, 1, 'How thin the wall is.')],
           [_D('E', 'Negative energy', 'M☉c²', '0.002*v^2*R^2*sg*(1 + 0.05*d)', 'E ∝ −v²R²σ', 'How much negative energy the wall needs.'),
            _D('tid', 'Wall shear', '', 'v*sg', '∝ vσ', 'The tidal shear across the wall. A passenger is torn apart above 5.'),
            _D('Rs', 'Wall thinness', '', 'R*sg', 'Rσ', 'How sharp the wall is for its bubble.')],
           [_C('exotic', 'damage', '50 - E', 'More negative energy than the forge can hold', 'E < 50')],
           rate='clamp(1/(0.5 + E), 0.25, 2)', rate_formula='rate = 1/(0.5 + E)', rate_desc='Smaller bubbles form faster.',
           heat='0.2*E', idle_heat='0', kappa=40, heat_capacity=60)

_r12.WINDOWS['climate_engine'] = {
    'r53_weather_clear': ({}, [('dT', -1, 0.5)]),
    'r53_weather_rain': ({}, [('dT', 1, 2.5)]),
    'r53_weather_storm': ({}, [('dT', 2.5, 5)]),
    'r53_ice_core': ({}, [('dT', -10, -2.5)]),
    'r53_permafrost': ({}, [('dT', -2.5, -1)]),
}
_r12.WINDOWS['lensing_concentrator'] = {
    'r53_focus_starlight': ({'Aref': 5}, [('A', 3, 1e9)]),
    'r53_einstein_ring': ({'Aref': 20}, [('A', 20, 1e9)]),
}
_r12.WINDOWS['time_dilation_chamber'] = {
    'r53_age_samples': ({}, [('f', 3, 1e9)]),
    'r53_age_radium': ({}, [('f', 2, 1e9)]),
    'r53_strain_spacetime': ({}, [('f', 5, 1e9)]),
}
_r12.WINDOWS['warp_field_forge'] = {'r53_warp_bubble_core': ({}, [('v', 1, 99), ('E', 0, 1), ('tid', 0, 5), ('Rs', 10, 1e9)])}
_r12.WINDOWS.setdefault('deep_drilling_rig', {}).update({'r53_lithosphere': ({}, [('h', 9, 12)])})
_r12.WINDOWS.setdefault('tectonic_press', {}).update({'r53_planetary_core': ({'Ea': 300000, 'Tr': 2300}, [('mB', 0.3, 100), ('Tf', 1800, 2600)])})
_r12.WINDOWS.setdefault('laser_interferometer', {}).update({'r53_gw_echo': ({'hw': 3e-22}, [('SNR', 8, 1e12)])})

# =================================================================================== RESEARCH
RESEARCH = kit.place([
    ('hydrology', 'Hydrology',
     'Water moves through rock as through a sponge, slowly: Darcy\'s law, flow = permeability × pressure gradient. Under the plains lie '
     'aquifers of rain that fell before the last ice age. Unlocks the Aquifer Pump.',
     'aquifer_sample', None, None, 50, ['geomorphology'], [('sediment_core', 4), ('seismic_recorder_drum', 1)]),
    ('climatology', 'Climatology',
     'What the air holds decides how much of the Sun\'s heat the world keeps: carbon dioxide traps it, aerosols send it back. Unlocks the '
     'Climate Engine.',
     'ice_core', None, None, 70, ['hydrology', 'thermodynamics_of_information'], [('minecraft:ice', 8), ('survey_map', 2)]),
    ('remote_sensing', 'Remote Sensing',
     'A telescope turned round: from orbit, in visible light and infrared and radar, the whole ground at once. Unlocks the Survey Satellite.',
     'satellite_image_plate', None, None, 120, ['seismology', 'observational_astronomy'], [('star_field_plate', 2), ('survey_map', 4)]),
    ('planetary_geology', 'Planetary Geology',
     'Every rocky planet sorts itself the same way: iron sinks to a core, rock floats above as mantle and crust. Comparing their cores tells '
     'how planets form.',
     'planetary_core_sample', None, None, 200, ['mantle_geodynamics', 'exoplanetology'], [('exoplanet_core_sample', 4), ('ringwoodite', 4)]),
    ('geodesics', 'Geodesics',
     'In curved spacetime a falling body takes the straightest path there is - a geodesic. Gravity is not a force; it is the shape of the '
     'road. Unlocks the Geodesic Compass.',
     'geodesic_compass', None, None, 140, ['general_relativity', 'geometric_topology'], [('proof_mass', 2), ('metric_tensor', 1)]),
    ('gravitational_lensing', 'Gravitational Lensing',
     'Light follows geodesics too: a mass bends the light passing it, twice as much as Newton would say, and a mass exactly between you and '
     'a star turns it into a ring. Unlocks the Lensing Concentrator.',
     'einstein_ring_lens', None, None, 160, ['geodesics', 'observational_astronomy'], [('einstein_ring_lens', 4), ('deep_sky_plate', 2)]),
    ('frame_dragging', 'Frame Dragging',
     'A spinning mass drags spacetime round with it: a gyroscope in orbit round the Earth turns, by forty milliarcseconds a year, for no '
     'other reason. Unlocks the Lense-Thirring Vortex Core.',
     'precision_gyroscope', None, None, 170, ['geodesics', 'penrose_process'], [('precision_gyroscope', 2), ('pulsar_timing_record', 2)]),
    ('tidal_physics', 'Tidal Physics',
     'Gravity pulls harder on your near side than your far side. Near a small black hole the difference tears anything apart lengthwise - '
     'spaghettification.',
     'tidal_shear_crystal', None, None, 220, ['black_hole_interior', 'frame_dragging'], [('tidal_shear_crystal', 4), ('micro_singularity', 1)]),
    ('binary_mergers', 'Binary Mergers',
     'Two compact stars in orbit radiate gravitational waves, spiral in, and merge in a final whoop of rising pitch. Their chirp mass sets '
     'how fast. Unlocks gravitational-wave echoes - and an answer from the Frontier.',
     'chirp_signal_record', None, None, 230, ['gravitational_waves', 'kilonovae'],
     [('chirp_signal_record', 2), ('merger_remnant', 1), ('gw_record_bbh', 2)]),
    ('alcubierre_metric', 'Alcubierre Metric',
     'General relativity allows a bubble of flat space to be carried faster than light - if its wall is made of negative energy. Unlocks '
     'the Warp Field Forge.',
     'warp_bubble_core', None, None, 300, ['tidal_physics', 'traversable_wormholes'],
     [('lense_thirring_vortex_core', 2), ('negative_energy_lattice', 2)]),
])

PUZZLES = {
    'hydrology': dict(
        goal='Water of viscosity μ seeps through rock of permeability k, across an area A, driven by a pressure difference P over a length L. '
             'Write the flow Q.',
        name='Darcy\'s law', lhs='Q',
        form='k * A * P / ( μ * L )',
        expr='k * A * P / ( m * L ) * 1000',
        ask='Find Q in litres a second for k = 1e-11 m², A = 10 m², P = 1e5 Pa, m = 0.001 Pa·s (water), L = 10 m.',
        vars={'Q': 'flow (L/s)', 'k': 'permeability (m²)', 'A': 'area (m²)', 'P': 'pressure difference (Pa)', 'μ': 'viscosity', 'm': 'viscosity (Pa·s)',
              'L': 'length (m)'},
        given={'k': 1e-11, 'A': 10, 'P': 1e5, 'm': 0.001, 'L': 10}, unit='L/s',
        method='Like current through a resistor: flow grows with the push (the pressure gradient) and the ease of passage (k/μ).',
        distractors=['g', 'ρ', '2']),
    'climatology': dict(
        goal='The air\'s carbon dioxide rises from a concentration c to C. Write the radiative forcing F it adds, in W/m², which grows with the '
             'logarithm of the ratio (coefficient 5.35).',
        name='CO2 radiative forcing', lhs='F',
        form='5.35 * ln ( C / c )',
        expr='5.35 * ln ( C / c )',
        ask='Find F for doubling the pre-industrial carbon dioxide: c = 280 ppm, C = 560 ppm.',
        vars={'F': 'forcing (W/m²)', 'C': 'concentration now (ppm)', 'c': 'concentration before (ppm)'},
        given={'C': 560, 'c': 280}, unit='W/m²',
        method='The strongest absorption bands are already saturated; extra CO₂ only widens their wings, so each doubling adds the same.',
        distractors=['σ', 'T', '4']),
    'remote_sensing': dict(
        goal='A satellite at height h looks down through a telescope of aperture D at light of wavelength λ. Write the smallest separation x '
             'it can tell apart on the surface.',
        name='Diffraction-limited footprint', lhs='x',
        form='1.22 * λ * h / D',
        expr='1.22 * L * h / D',
        ask='Find x in metres for L = 5e-7 m, h = 7e5 m, D = 1.1 m.',
        vars={'x': 'separation (m)', 'λ': 'wavelength', 'L': 'wavelength (m)', 'h': 'height (m)', 'D': 'aperture (m)'},
        given={'L': 5e-7, 'h': 7e5, 'D': 1.1}, unit='m',
        method='The smallest angle a circular aperture resolves is 1.22λ/D; at distance h that angle spans x.',
        distractors=['c', '2', 'π']),
    'planetary_geology': dict(
        goal='A planet of mass M and radius R, of even density, squeezes its centre. Write the pressure P there, with gravity\'s constant G.',
        name='Central pressure of a uniform planet', lhs='P',
        form='3 * G * M ^ 2 / ( 8 * π * R ^ 4 )',
        expr='3 * G * M ^ 2 / ( 25.1327 * R ^ 4 ) / u',
        ask='Find P in GPa (u = 1e9) for the Earth: M = 5.972e24 kg, R = 6.371e6 m, G = 6.674e-11.',
        vars={'P': 'pressure (GPa)', 'G': 'gravitational constant', 'M': 'mass (kg)', 'π': 'pi', 'R': 'radius (m)', 'u': '10⁹'},
        given={'G': 6.674e-11, 'M': 5.972e24, 'R': 6.371e6, 'u': 1e9}, unit='GPa',
        method='Integrate dP/dr = −ρg(r) from the surface in, with g(r) = (4/3)πGρr.',
        distractors=['c', 'h', '2']),
    'geodesics': dict(
        goal='A mass M is squeezed until light cannot escape it. Write the radius r at which that happens, from gravity\'s constant G and the '
             'speed of light c.',
        name='Schwarzschild radius', lhs='r',
        form='2 * G * M / c ^ 2',
        expr='2 * G * M / c ^ 2 / 1000',
        ask='Find r in kilometres for the Sun: M = 1.989e30 kg, G = 6.674e-11, c = 2.998e8 m/s.',
        vars={'r': 'radius (km)', 'G': 'gravitational constant', 'M': 'mass (kg)', 'c': 'speed of light (m/s)'},
        given={'G': 6.674e-11, 'M': 1.989e30, 'c': 2.998e8}, unit='km',
        method='Where the escape speed √(2GM/r) reaches c - which, by luck, is exactly what general relativity gives too.',
        distractors=['π', '4', 'h']),
    'gravitational_lensing': dict(
        goal='A ray passes a mass M at closest distance b and is bent. Write the angle α it turns through, with G and c.',
        name='Einstein deflection', lhs='α',
        form='4 * G * M / ( c ^ 2 * b )',
        expr='4 * G * M / ( c ^ 2 * b ) * 206265',
        ask='Find α in arcseconds for starlight grazing the Sun: M = 1.989e30 kg, b = 6.96e8 m, G = 6.674e-11, c = 2.998e8 m/s.',
        vars={'α': 'deflection (arcsec)', 'G': 'gravitational constant', 'M': 'mass (kg)', 'c': 'speed of light', 'b': 'closest distance (m)'},
        given={'G': 6.674e-11, 'M': 1.989e30, 'b': 6.96e8, 'c': 2.998e8}, unit='″',
        method='Newton gives 2GM/(c²b); curved space adds as much again - which Eddington\'s 1919 eclipse confirmed.',
        distractors=['2', 'π', 'h']),
    'frame_dragging': dict(
        goal='A body with angular momentum L drags the spacetime round it. Write the rate Ω at which a gyroscope\'s axis is turned at distance '
             'r, with G and c.',
        name='Lense-Thirring precession', lhs='Ω',
        form='2 * G * L / ( c ^ 2 * r ^ 3 )',
        expr='2 * G * L / ( c ^ 2 * r ^ 3 ) * u',
        ask='Find Ω in milliarcseconds a year (u = 6.51e15) above the Earth: L = 5.86e33 kg·m²/s, r = 7.02e6 m, G = 6.674e-11, c = 2.998e8.',
        vars={'Ω': 'precession (mas/yr)', 'G': 'gravitational constant', 'L': 'angular momentum', 'c': 'speed of light', 'r': 'distance (m)',
              'u': 'rad/s → mas/yr'},
        given={'G': 6.674e-11, 'L': 5.86e33, 'c': 2.998e8, 'r': 7.02e6, 'u': 6.51e15}, unit='mas/yr',
        method='The spinning mass makes a gravitomagnetic field, a dipole like a spinning charge\'s; a gyroscope precesses in it.',
        distractors=['M', 'π', '4']),
    'tidal_physics': dict(
        goal='A body of length L hangs head-down at distance r from a mass M. Write the difference a between the pull on its two ends, with G.',
        name='Spaghettification', lhs='a',
        form='2 * G * M * L / r ^ 3',
        expr='2 * G * M * L / r ^ 3 / 9.81',
        ask='Find a in g for a 2 m body 100 km from a 10-solar-mass black hole: M = 1.989e31 kg, L = 2, r = 1e5, G = 6.674e-11.',
        vars={'a': 'stretching (g)', 'G': 'gravitational constant', 'M': 'mass (kg)', 'L': 'length (m)', 'r': 'distance (m)'},
        given={'G': 6.674e-11, 'M': 1.989e31, 'L': 2, 'r': 1e5}, unit='g',
        method='Differentiate GM/r² with respect to r and multiply by L.',
        distractors=['c', 'π', '3']),
    'binary_mergers': dict(
        goal='Two masses m and n spiral together. Their gravitational waves depend on one combination of the two, which sets how fast the '
             'pitch rises. Write it, M.',
        name='Chirp mass', lhs='M',
        form='( m * n ) ^ ( 3 / 5 ) / ( m + n ) ^ ( 1 / 5 )',
        expr='( m * n ) ^ 0.6 / ( m + n ) ^ 0.2',
        ask='Find M in solar masses for GW150914: m = 36, n = 29.',
        vars={'M': 'combined mass (M☉)', 'm': 'first mass (M☉)', 'n': 'second mass (M☉)'},
        given={'m': 36, 'n': 29}, unit='M☉',
        method='The frequency rises as df/dt ∝ M^(5/3) f^(11/3) at leading order: only this combination enters.',
        distractors=['c', 'G', '2']),
    'alcubierre_metric': dict(
        goal='Energy and mass are one: write the energy E locked in a mass M, with the speed of light c.',
        name='Mass-energy equivalence', lhs='E',
        form='M * c ^ 2',
        expr='M * c ^ 2 / u',
        ask='Find E in units of 10⁴⁴ J (u = 1e44) for the mass of Jupiter, M = 1.898e27 kg - roughly the negative energy a small warp '
            'bubble needs (c = 2.998e8 m/s).',
        vars={'E': 'energy (10⁴⁴ J)', 'M': 'mass (kg)', 'c': 'speed of light (m/s)', 'u': '10⁴⁴'},
        given={'M': 1.898e27, 'c': 2.998e8, 'u': 1e44}, unit='×10⁴⁴ J',
        method='From the energy of a body at rest in special relativity: E² = (pc)² + (mc²)² with p = 0.',
        distractors=['2', 'G', 'h']),
}

AQ, CF, LN, TD, WF = 'aquifer_pumping', 'climate_forcing', 'lensing', 'time_dilation', 'warp_forging'
DIMS_INTERIOR = [M + 'planetary_interior']
RECIPES = [
    # Aquifer Pump
    ('pump_water', AQ, [], [], [], [('minecraft:water', 250)], 32, 100, 'hydrology', 0),
    ('pump_aquifer', AQ, [('minecraft:glass_bottle', 1)], [], [('aquifer_sample', 1)], [('deep_brine', 100)], 64, 400, 'hydrology', 1),
    ('pump_supercritical', AQ, [('minecraft:bucket', 1)], [], [('minecraft:bucket', 1)], [('supercritical_water', 100)], 128, 400, 'hydrology', 0),
    # Climate Engine
    ('weather_clear', CF, [('minecraft:paper', 1)], [], [], [], 512, 600, 'climatology', 0),
    ('weather_rain', CF, [('minecraft:paper', 1)], [('minecraft:water', 1000)], [], [], 512, 600, 'climatology', 0),
    ('weather_storm', CF, [('minecraft:paper', 1)], [('minecraft:water', 2000)], [], [], 1024, 800, 'climatology', 0),
    ('ice_core', CF, [('minecraft:snow_block', 4)], [], [('ice_core', 1)], [], 512, 800, 'climatology', 2),
    ('permafrost', CF, [('minecraft:dirt', 4), ('minecraft:snowball', 4)], [], [('permafrost_sample', 1)], [], 512, 800, 'climatology', 2),
    # Lensing Concentrator (generator)
    ('focus_starlight', LN, [], [], [], [], 16384, 600, 'gravitational_lensing', 2, ['einstein_ring_lens']),
    ('einstein_ring', LN, [('photographic_plate', 1)], [], [('einstein_ring_lens', 1)], [], 8192, 1200, 'gravitational_lensing', 8,
     ['einstein_ring_lens']),
    # Time Dilation Chamber
    ('age_samples', TD, [('proof_mass', 1), ('minecraft:amethyst_shard', 4)], [], [('time_dilated_sample', 4)], [], 1024, 800,
     'gravitational_time_dilation', 2),
    ('age_radium', TD, [('radium_salt', 4)], [], [('polonium_210_source', 2)], [], 1024, 1200, 'gravitational_time_dilation', 0),
    ('strain_spacetime', TD, [], [('liquid_spacetime', 500)], [], [('strained_spacetime', 500)], 4096, 800, 'tidal_physics', 2),
    # Warp Field Forge
    ('warp_bubble_core', WF, [('negative_energy_lattice', 2), ('alcubierre_coil', 1), ('lense_thirring_vortex_core', 1)],
     [('strained_spacetime', 1000)], [('warp_bubble_core', 1)], [], 65536, 2400, 'alcubierre_metric', 20),
    # elsewhere
    ('satellite_image', 'interstellar_scooping', [('photographic_plate', 1)], [], [('satellite_image_plate', 1)], [], 128, 1200, 'remote_sensing', 6,
     ['survey_satellite']),
    ('lithosphere', 'deep_drilling', [], [('minecraft:water', 500)], [('lithospheric_plate_fragment', 2), ('peridotite', 1)], [], 4096, 1200,
     'plate_tectonics', 0, ['moissanite']),
    ('planetary_core', 'tectonic_pressing', [('exoplanet_core_sample', 1), ('core_iron', 2)], [], [('planetary_core_sample', 1)], [], 8192, 1600,
     'planetary_geology', 10),
    ('gw_echo', 'gw_detection', [('chirp_signal_record', 1), ('strain_sensor', 1)], [], [('gravitational_wave_echo', 1)], [], 8192, 2400,
     'binary_mergers', 15),
]
ENV = {'pump_aquifer': {'dimensions': DIMS_INTERIOR},
       'pump_supercritical': {'dimensions': DIMS_INTERIOR, 'biomes': [M + 'magma_chambers']},
       'satellite_image': {'min_y': 200, 'dimensions': [M + 'accretion_frontier', 'minecraft:the_end']}}
SHAPED = [
    ('survey_satellite', ['SPS', 'TRT', 'SPS'], {'S': 'photographic_plate', 'P': 'minecraft:daylight_detector', 'T': 'telescope_mirror_blank',
                                                 'R': 'radio_telescope'}, 'survey_satellite', 1),
    ('strain_sensor', ['F', 'R', 'F'], {'F': 'fused_silica', 'R': 'minecraft:redstone'}, 'strain_sensor', 2),
    ('precision_gyroscope', ['GFG', 'FTF', 'GFG'], {'G': 'minecraft:gold_nugget', 'F': 'fused_silica', 'T': 'test_mass'}, 'precision_gyroscope', 1),
    ('geodesic_compass', [' E ', 'PCP', ' E '], {'E': 'einstein_ring_lens', 'P': 'proof_mass', 'C': 'minecraft:compass'}, 'geodesic_compass', 1),
    ('lense_thirring_vortex_core', ['TGT', 'GSG', 'TGT'], {'T': 'tidal_shear_crystal', 'G': 'precision_gyroscope', 'S': 'micro_singularity'},
     'lense_thirring_vortex_core', 1),
    ('negative_energy_lattice', ['CCC', 'CXC', 'CCC'], {'C': 'casimir_plate', 'X': 'exotic_matter_crystal'}, 'negative_energy_lattice', 2),
    ('climate_casing', ['III', 'IGI', 'III'], {'I': 'minecraft:iron_ingot', 'G': 'minecraft:glass'}, 'climate_casing', 8),
    ('aerosol_sprayer', ['I I', 'IBI', 'III'], {'I': 'minecraft:iron_ingot', 'B': 'interstellar_dust'}, 'aerosol_sprayer', 2),
    ('cloud_seeder_stack', ['IAI', 'IAI', 'IAI'], {'I': 'minecraft:iron_ingot', 'A': 'minecraft:snowball'}, 'cloud_seeder_stack', 4),
    ('radiometer_mast', ['P', 'I', 'I'], {'P': 'photographic_plate', 'I': 'minecraft:iron_ingot'}, 'radiometer_mast', 2),
    ('lens_pylon', ['III', 'ISI', 'III'], {'I': 'minecraft:iron_ingot', 'S': 'spacetime_casing'}, 'lens_pylon', 8),
    ('lens_array_block', ['LLL', 'LEL', 'LLL'], {'L': 'fused_silica_lens', 'E': 'einstein_ring_lens'}, 'lens_array_block', 2),
    ('focus_receiver', ['GGG', 'GSG', 'III'], {'G': 'minecraft:gold_ingot', 'S': 'stellar_lamp', 'I': 'minecraft:iron_ingot'}, 'focus_receiver', 1),
    ('chamber_casing', ['III', 'ICI', 'III'], {'I': 'minecraft:iron_ingot', 'C': 'minecraft:clock'}, 'chamber_casing', 8),
    ('dense_mass_core', ['NNN', 'NMN', 'NNN'], {'N': 'neutronium_ingot', 'M': 'micro_singularity'}, 'dense_mass_core', 2),
    ('warp_casing', ['III', 'IEI', 'III'], {'I': 'minecraft:iron_ingot', 'E': 'exotic_matter_crystal'}, 'warp_casing', 8),
    ('warp_coil_ring', ['NAN', 'A A', 'NAN'], {'N': 'negative_energy_lattice', 'A': 'alcubierre_coil'}, 'warp_coil_ring', 4),
    ('bubble_pedestal', [' L ', 'VSV', 'III'], {'L': 'lense_thirring_vortex_core', 'V': 'void_lattice_shard', 'S': 'spacetime_casing',
                                               'I': 'minecraft:iron_ingot'}, 'bubble_pedestal', 1),
]
SHAPELESS = []
LANG = {
    'message.theoreticalsciences.warp_anchored': 'The bubble closes round this place. Use it to come back here, once.',
    'message.theoreticalsciences.warp_lost': 'The place this bubble was anchored to no longer exists.',
    'tooltip.theoreticalsciences.warp_bubble_core': 'Use to anchor where you stand - you get an Anchored Warp Bubble that returns you here once.',
    'tooltip.theoreticalsciences.anchored_warp_bubble': 'Anchored at %s, %s, %s in %s. Use to return there (spent).',
}


def _recipes(g):
    kit.recipes(g, 'r53_', RECIPES)
    for name, env in ENV.items():
        typ = next(r[1] for r in RECIPES if r[0] == name)
        p = os.path.join(g['REC'], typ, 'r53_' + name + '.json')
        j = json.load(open(p))
        j['environment'] = env
        g['w'](p, j)
    kit.crafting(g, 'r53_', SHAPED, SHAPELESS)


def apply(g):
    g['LANG'].update(LANG)
    kit.write_puzzles('r53', PUZZLES)
    kit.write_research(g, RESEARCH)
    _recipes(g)


def apply_late(g):
    kit.pickaxe(g, BLOCKS)


def finish(g):
    te = g['text_entry']
    te('geography', 'r53_water', 'Hydrology', 'aquifer_pump', [
        'Water seeps through rock by $(thing)Darcy\'s law$(): Q = kAΔP/(μL) - permeability times the pressure gradient. The $(item)Aquifer '
        'Pump$() draws it up: Water anywhere; in the Planetary Interior, an $(item)Aquifer Sample$() with Deep Brine from a confined aquifer.',
        'In the Interior\'s Magma Chambers it reaches water heated past its critical point, 647 K and 22 MPa: $(fluid)Supercritical Water$(), '
        'neither liquid nor gas.'],
        adv='hydrology', sort=10, spot=['aquifer_pump', 'aquifer_sample'])
    te('geography', 'r53_climate', 'The Climate Engine', 'climate_engine', [
        'Carbon dioxide traps the heat the ground gives off: a forcing $(thing)ΔF = 5.35 ln(C/280 ppm)$() W/m² - 3.7 W/m² for each doubling. '
        'Aerosols reflect sunlight back out. The world warms by about $(thing)0.8 K per W/m²$().',
        'The $(item)Climate Engine$() holds the air\'s CO₂ and aerosols where you set them. Warm it by 1-2.5 K with water and a forecast and it '
        'rains; by 2.5-5 K, storms; hold it near today\'s and the sky clears. (Minecraft\'s weather is the whole world\'s.) Cool it 2.5 K or '
        'more and snow becomes an $(item)Ice Core$(); a little less, and frozen soil a $(item)Permafrost Sample$(). Past 6 K it runs away.'],
        adv='climatology', sort=11, spot=['climate_engine', 'ice_core', 'permafrost_sample'])
    te('geography', 'r53_orbit', 'Remote Sensing and Planetary Geology', 'satellite_image_plate', [
        'A $(item)Survey Satellite$() sits in the Interstellar Scoop as a catalyst - the Scoop works only high up or in space, which is where a '
        'satellite flies - and photographs the ground: a $(item)Satellite Image Plate$(). From 700 km, a 1.1 m mirror sees 40 cm.',
        'The Deep Drilling Rig, drilling to the base of a plate, brings up a $(item)Lithospheric Plate Fragment$(). The Tectonic Press squeezes '
        'an exoplanet\'s core with our own to a core\'s pressure: a $(item)Planetary Core Sample$() - every rocky planet sinks its iron to the '
        'middle, at about 3GM²/(8πR⁴).'],
        adv='remote_sensing', sort=12, spot=['survey_satellite', 'satellite_image_plate', 'planetary_core_sample'])
    te('spacetime', 'r53_lensing', 'Geodesics and Lensing', 'lensing_concentrator', [
        'A falling body follows a $(thing)geodesic$(): the straightest path through curved spacetime. The $(item)Geodesic Compass$() points '
        'along one. A mass squeezed inside $(thing)r_s = 2GM/c²$() lets no light out.',
        'Light follows geodesics too, and is bent by $(thing)α = 4GM/(c²b)$() - 1.75″ at the Sun\'s edge. A mass exactly in front of a star '
        'turns it into an Einstein ring.',
        'The $(item)Lensing Concentrator$() aims at a star through a lens mass and turns the focused light into power - at night, under open '
        'sky. The magnification $(thing)A = (u² + 2)/(u√(u² + 4))$() grows without limit as the alignment u → 0, but the aim jitters by '
        '0.3 milliarcseconds: choose a massive, near lens so the Einstein radius is big. Above A = 20 it grows an $(item)Einstein Ring Lens$().'],
        adv='gravitational_lensing', sort=50, spot=['lensing_concentrator', 'geodesic_compass', 'einstein_ring_lens'])
    te('spacetime', 'r53_drag', 'Frame Dragging and Tides', 'precision_gyroscope', [
        'A spinning mass drags spacetime round with it: a gyroscope near the Earth turns by $(thing)Ω = 2GJ/(c²r³)$(). A $(item)Precision '
        'Gyroscope$() carried in the off hand holds you steady: no Frame Drag, no Tidal Strain.',
        'Tides are the difference between the pull on your two ends: $(thing)2GML/r³$(). Near a small black hole they pull you into spaghetti. '
        'Tidal shear crystals, a gyroscope and a micro-singularity knot spacetime into a $(item)Lense-Thirring Vortex Core$().'],
        adv='frame_dragging', sort=51, spot=['precision_gyroscope', 'lense_thirring_vortex_core', 'tidal_shear_crystal'])
    te('spacetime', 'r53_dilation', 'The Time Dilation Chamber', 'time_dilation_chamber', [
        'Clocks deep in a gravity well run slow: by $(thing)1/√(1 − r_s/r)$() at a distance r from a mass with Schwarzschild radius r_s.',
        'The $(item)Time Dilation Chamber$() holds samples round a dense core. Set its mass and how close they sit: at three times dilation it '
        'grows Time-Dilated Samples in bulk; radium ages into polonium; at five, liquid spacetime is strained into $(fluid)Strained '
        'Spacetime$(). Too close and they fall through the horizon. Each storey (1 to 7) holds more.'],
        adv='gravitational_time_dilation', sort=52, spot=['time_dilation_chamber', 'dense_mass_core'])
    te('spacetime', 'r53_mergers', 'Binary Mergers', 'chirp_signal_record', [
        'Two compact stars spiral in as gravitational waves carry off their orbit; how fast the pitch rises depends only on the $(thing)chirp '
        'mass$() (m₁m₂)^(3/5)/(m₁ + m₂)^(1/5). The Binary of the Accretion Frontier drops Chirp Signal Records and Merger Remnants.',
        'The Laser Interferometer, listening to a chirp with a strain sensor, can hear a $(item)Gravitational Wave Echo$() - repeats after the '
        'ringdown, if a horizon is not quite a horizon. Played in the Frontier, an echo summons The Binary just as a Binary Black Hole Merger record does.'],
        adv='binary_mergers', sort=53, spot=['chirp_signal_record', 'merger_remnant', 'gravitational_wave_echo'])
    te('spacetime', 'r53_warp', 'The Warp Field Forge', 'warp_field_forge', [
        'Alcubierre found a solution of general relativity that moves a bubble of flat space at any speed: space contracts ahead of it and '
        'expands behind. Its wall must be made of $(thing)negative energy$() - from $(item)Negative Energy Lattices$() of Casimir plates.',
        'The $(item)Warp Field Forge$() shapes the bubble. Set its speed, radius and wall sharpness: the negative energy needed grows as v²R²σ, '
        'the shear across the wall as vσ (above 5 it tears a passenger apart), and a wall too thick for its bubble (Rσ < 10) leaks. A '
        '$(item)Warp Bubble Core$() must be superluminal, with under one solar mass of negative energy.',
        'Use a Warp Bubble Core to anchor where you stand: it becomes an $(item)Anchored Warp Bubble$(). Use that, anywhere, to return there - '
        'once. Recipes only take the plain core.'],
        adv='alcubierre_metric', sort=54, spot=['warp_field_forge', 'warp_bubble_core', 'anchored_warp_bubble'])
