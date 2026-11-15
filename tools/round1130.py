"""
1.13.0 (Patch C2): the station grows.

  * Station modules: the Microgravity Factory (a console: the Rayleigh and Marangoni numbers decide whether a melt
    stays still, and only aboard the station is gravity small enough), the Habitat Ring Module (spin gravity for the
    crew: aboard the station it brings back your weight nearby, and keeps off deconditioning), and the Storm Shelter
    Module (a water-walled refuge from proton storms).
  * Deconditioning: weeks in free fall weaken bone and muscle (a Minecraft day stands for a week here); back in gravity
    you are slow, then weak, until you recover. It can be turned off in the config.
  * Satellites, launched by the Rocket Assembly into the orbits that suit them: a Weather Satellite (forecasts rain and
    thunder), a Solar Monitor at L1 (warns of solar storms), a Relay Satellite in geostationary orbit (probes return
    more), and a Power Satellite, which beams microwaves to a new multiblock generator, the Microwave Rectenna.
  * Orbital debris (on by default, can be turned off in the config): every launch leaves some; it decays slowly; while
    it is thick, strikes hurt anyone outside the hub without a Whipple bumper and can destroy satellites, whose wreckage
    adds more (Kessler). A Debris Sweeper clears most of it.

Run:  python3 tools/round1130.py
"""
import json, math, os, sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from tsgen import *          # noqa: F401,F403
import tsgen as T

L = T.LANG
STATION_G = 9.81e-5          # the quasi-steady residual at the station's centre of mass, ten micro-g
T.WORLD['microgravity_factory'] = {'grav': STATION_G}

# ====================================================================== research
research('microgravity_processing', 'Microgravity Processing', 22, 8, 300, ['orbital_mechanics', 'crystal_growth_physics'],
         [('microgravity_crystal', 1), ('silicon_boule', 2)], 340,
         "On the ground a melt with a warm side and a cool side stirs itself: the warm liquid is lighter and rises. Whether it "
         "does is told by one number, Rayleigh's, the push of buoyancy against the drag of viscosity and the smoothing of heat; "
         "past about 1,708 the melt rolls. In orbit gravity is a hundred thousand times smaller and the melt stays still, so "
         "crystals grow without striations and glasses cool without crystallising. A free surface still stirs itself, pulled by "
         "surface tension (the Marangoni effect), unless it is capped. Unlocks the Microgravity Factory, which works only "
         "aboard the Orbital Station.",
         'A melt that stays still',
         {'goal': 'A layer of fluid of depth L is warmer by ΔT at the bottom, under gravity g. It has thermal expansion β, kinematic viscosity '
                  'ν and thermal diffusivity α. Write the Rayleigh number Ra, the ratio of buoyancy to the damping.',
          'name': 'Rayleigh number', 'lhs': 'Ra', 'form': 'g * β * ΔT * L ^ 3 / ( ν * α )', 'expr': 'g * b * dT * L ^ 3 / ( nu * a )',
          'ask': 'Find Ra for water (β = 2e-4 /K, ν = 1e-6 m²/s, α = 1.4e-7 m²/s), ΔT = @{dT} K, L = 0.02 m, at g = 9.81 m/s².',
          'vars': {'Ra': 'Rayleigh number', 'g': 'gravity', 'β': 'thermal expansion', 'ΔT': 'temperature difference', 'L': 'depth',
                   'ν': 'kinematic viscosity', 'α': 'thermal diffusivity'},
          'given': {'g': 9.81, 'b': 2e-4, 'dT': 5.0, 'L': 0.02, 'nu': 1e-6, 'a': 1.4e-7}, 'unit': '',
          'method': 'Buoyancy gβΔT against the two damping rates ν/L² and α/L², over the depth: the L³ comes from both.',
          'distractors': ['+', '*', 'sqrt'], 'vary': {'dT': [1.0, 20.0, 0.5]}},
         'microgravity_factory', 'orbital_mechanics')
research('satellite_orbits', 'Satellite Orbits', 24, 7, 320, ['transfer_orbits'],
         [('survey_satellite', 1), ('precision_gyroscope', 2)], 340,
         "Each job has its orbit. A weather satellite flies over the poles in a sun-synchronous orbit, crossing every place at "
         "the same time of day. A relay sits in geostationary orbit, where the period is one sidereal day and it hangs over one "
         "spot, at 42,164 km from Earth's centre. A solar monitor waits at the Lagrange point L1, 1.5 million km sunward, where "
         "it sees a storm leave the Sun an hour before it arrives. Unlocks the Weather, Solar Monitor and Relay Satellites, "
         "launched by the Rocket Assembly.",
         'The orbit for the job',
         {'goal': 'A circular orbit about a body of gravitational parameter μ has period T. From gravity providing the centripetal force, '
                  'write its radius r.',
          'name': 'Orbit radius', 'lhs': 'r', 'form': '( μ * T ^ 2 / ( 4 * π ^ 2 ) ) ^ ( 1 / 3 )', 'expr': '( mu * T ^ 2 / ( 4 * π ^ 2 ) ) ^ ( 1 / 3 )',
          'ask': 'Find r in km for μ = 398600 km³/s² and T = @{T} s (a sidereal day is 86164 s).',
          'vars': {'r': 'orbit radius', 'μ': 'gravitational parameter', 'T': 'period'},
          'given': {'mu': 398600.0, 'T': 86164.0}, 'unit': 'km',
          'method': 'μ/r² = ω²r with ω = 2π/T: so r³ = μT²/4π².',
          'distractors': ['sqrt', '*', '2'], 'vary': {'T': [5400.0, 90000.0, 100.0]}},
         'relay_satellite', 'transfer_orbits')
research('space_solar_power', 'Space Solar Power', 26, 7, 400, ['satellite_orbits', 'semiconductor_doping'],
         [('solar_array', 4), ('radio_dish_panel', 2)], 420,
         "In geostationary orbit the Sun shines almost all the time, through no air. A power satellite gathers it, turns it into "
         "microwaves, and beams them to a field of antennas on the ground, a rectenna, that turns them back into current. "
         "Diffraction spreads the beam: the spot on the ground is wider the longer the wavelength and the smaller the "
         "transmitter. It must be wide enough to keep the beam's power per square metre safe, and the rectenna wide enough to "
         "catch it. Unlocks the Power Satellite and the Microwave Rectenna.",
         'Sunlight by radio',
         {'goal': 'A transmitter of diameter D sends waves of wavelength λ over a distance R. Diffraction spreads the beam; write the '
                  'diameter d of the spot it makes (to its first dark ring).',
          'name': 'Diffraction spot', 'lhs': 'd', 'form': '2.44 * λ * R / D', 'expr': '2.44 * lam * R / D',
          'ask': 'Find d in m for λ = 0.0517 m (5.8 GHz), R = 35786000 m and D = @{D} m.',
          'vars': {'d': 'spot diameter', 'λ': 'wavelength', 'R': 'distance', 'D': 'transmitter diameter'},
          'given': {'lam': 0.0517, 'R': 35786000.0, 'D': 1000.0}, 'unit': 'm',
          'method': 'The angle to the first dark ring of a circular aperture is 1.22λ/D; across both sides and over R it is 2.44λR/D.',
          'distractors': ['*', '+', 'sqrt'], 'vary': {'D': [200.0, 2000.0, 10.0]}},
         'microwave_rectenna', 'satellite_orbits')
research('orbital_debris', 'Orbital Debris', 24, 8, 300, ['satellite_orbits'],
         [('pressure_hull_plate', 2), ('light_sail', 1)], 320,
         "Every launch leaves something in orbit: spent stages, bolts, flecks of paint. At 10 km/s a fleck of paint chips a "
         "window and a bolt carries a grenade's energy. When there is enough of it, collisions break satellites into more "
         "debris, which breaks more: Kessler syndrome. Low orbits clean themselves slowly, as the thin air drags things down; "
         "a sweeper can speed it up. Unlocks the Debris Sweeper. (Debris can be turned off in the config.)",
         'What a launch leaves',
         {'goal': 'A fragment of mass m strikes at speed v. Write the energy E it carries.',
          'name': 'Impact energy', 'lhs': 'E', 'form': 'm * v ^ 2 / 2', 'expr': 'm * v ^ 2 / 2',
          'ask': 'Find E in J for m = @{m} kg at v = 10000 m/s.',
          'vars': {'E': 'energy', 'm': 'mass', 'v': 'speed'},
          'given': {'m': 0.001, 'v': 10000.0}, 'unit': 'J',
          'method': 'Kinetic energy, ½mv²: at orbital speed even a gram is tens of kilojoules.',
          'distractors': ['*', '+', 'sqrt'], 'vary': {'m': [0.0001, 0.05, 0.0001]}},
         'debris_sweeper', 'satellite_orbits')

# ====================================================================== the Microgravity Factory (a console)
MG = model([par('dT', 'K', 1, 100, 0.5, 10), par('L', 'mm', 1, 40, 0.5, 8), par('enc', '', 0, 0.95, 0.01, 0.5)],
           {'beta': 2e-4, 'nu': 1e-6, 'alpha': 1.4e-7, 'sigT': 1.5e-4, 'mu': 1e-3, 'Mac': 2e4},
           [der('Ra', '', 'grav*beta*dT*(L/1000)^3/(nu*alpha)*(1 + 0.01*d)', 'Ra = gβΔT·L³/(να): buoyancy against the damping'),
            der('Ma', '', 'sigT*dT*(L/1000)/(mu*alpha)*(1 - enc)', 'Ma = (dσ/dT)·ΔT·L/(μα): the free surface pulling, less what the cap stops'),
            der('vg', 'mm/h', '0.05*dT*(1 - 0.6*enc)', 'growth speed: faster with a steeper gradient, slower under a thick cap'),
            der('wid', 'mm', 'L', 'the crystal (or fibre preform) is as wide as the melt')],
           [con('buoy', 'stop', '1708 - Ra', 'Ra under 1708, or the melt rolls over and stirs itself'),
            con('stri', 'wear', 'Mac - Ma', 'Ma under its critical value, or surface flow leaves striations')],
           'clamp(0.05*dT*(1 - 0.6*enc)*L/12, 0.2, 1.5)', 'a steeper gradient and a wider melt grow faster; a thick cap slows it')
machine('microgravity_factory', 'Microgravity Factory', "A furnace module for the station: a melt held between a hot and a cold "
        "plate inside a vibration-isolated rack at the station's centre of mass, where the residual gravity is ten millionths of "
        "the ground's. Only there does a melt stay still enough. It works only aboard the Orbital Station.", 'chain', 3, MG,
        {'p.dT': 'Temperature difference', 'p.dT.desc': 'Across the melt: the steeper, the faster it grows, and the harder it pushes.',
         'p.L': 'Melt width', 'p.L.desc': 'How wide the melt (and the crystal) is: buoyancy grows as its cube.',
         'p.enc': 'Encapsulation', 'p.enc.desc': 'A liquid cap over the free surface, stopping the surface-tension flow; it slows the growth.',
         'd.Ra': 'Rayleigh number', 'd.Ra.desc': 'Buoyancy against damping: over 1708 the melt rolls.',
         'd.Ma': 'Marangoni number', 'd.Ma.desc': 'Surface-tension flow at the free surface.', 'd.vg': 'Growth speed', 'd.vg.desc': 'How fast the solid advances.',
         'd.wid': 'Crystal width', 'd.wid.desc': 'As wide as the melt.',
         'c.buoy': 'The melt is convecting (only aboard the station is gravity small enough)', 'c.stri': 'Striations: surface flow in the melt',
         'v.beta': 'Thermal expansion', 'v.nu': 'Kinematic viscosity', 'v.alpha': 'Thermal diffusivity', 'v.sigT': 'Surface tension slope',
         'v.mu': 'Dynamic viscosity', 'v.Mac': 'Critical Marangoni number',
         'disturb': 'g-jitter: crew and machinery shake the rack, raising the residual gravity by 1%·d.', 'rate.desc': 'Steeper and wider is faster; a cap slows it.'},
        extreme('microgravity_factory', 1, P_CONSOLE, {'A': 'station_hull', 'F': 'silica_aerogel', 'L': 'crystal_growth_furnace', 'N': 'precision_gyroscope'}))

# ====================================================================== the Microwave Rectenna (a multiblock generator)
MR = model([par('Dt', 'm', 100, 2000, 10, 900), par('f', 'GHz', 1, 30, 0.1, 5.8), par('Dr', 'km', 0.5, 10, 0.1, 6)],
           {'P': 2e9},
           [der('lam', 'm', '0.2998/f', 'λ = c/f'),
            der('Ds', 'm', '2.44*0.2998/f*3.5786e7/Dt*(1 + 0.01*d)', 'spot diameter 2.44·λ·R/D from geostationary height'),
            der('eta', '', '1 - exp(-2*(Dr*1000/(2.44*0.2998/f*3.5786e7/Dt*(1 + 0.01*d)))^2)', 'the share of the beam the field catches'),
            der('Ipk', 'W/m²', '8*P/(3.14159*(2.44*0.2998/f*3.5786e7/Dt*(1 + 0.01*d))^2)', 'peak power density at the beam centre'),
            der('out', '', '(1 - exp(-2*(Dr*1000/(2.44*0.2998/f*3.5786e7/Dt*(1 + 0.01*d)))^2))*(1 - 0.02*Dr)*0.85', 'caught, less wiring loss, rectified')],
           [con('safe', 'stop', '230 - 8*P/(3.14159*(2.44*0.2998/f*3.5786e7/Dt*(1 + 0.01*d))^2)', 'the beam under 230 W/m² at its centre, the safety limit'),
            con('fade', 'wear', '12 - f*(1 + rain)', 'under 12 GHz (6 in rain), or the air absorbs the beam')],
           'clamp((1 - exp(-2*(Dr*1000/(2.44*0.2998/f*3.5786e7/Dt*(1 + 0.01*d)))^2))*(1 - 0.02*Dr)*0.85*1.4, 0.3, 1.25)',
           'what the field catches and rectifies')
machine('microwave_rectenna', 'Microwave Rectenna', "The controller of a field of rectifying antennas: dipoles over a ground mesh, "
        "each with its diode, turning a microwave beam from a Power Satellite in geostationary orbit back into current, day and "
        "night. Each Power Satellite in orbit can feed four rectennas. A multiblock generator.", 'chain', 3, MR,
        {'p.Dt': 'Transmitter aperture', 'p.Dt.desc': "The satellite's antenna: wider focuses the beam into a smaller, more intense spot.",
         'p.f': 'Beam frequency', 'p.f.desc': 'Higher focuses tighter, but over 12 GHz (6 in rain) the air absorbs it.',
         'p.Dr': 'Field in use', 'p.Dr.desc': 'How much of the field is strung together: more catches more, with longer wiring.',
         'd.lam': 'Wavelength', 'd.lam.desc': 'Of the beam.', 'd.Ds': 'Spot diameter', 'd.Ds.desc': 'How wide the beam is on the ground.',
         'd.eta': 'Capture', 'd.eta.desc': 'Of the beam, by the field.', 'd.Ipk': 'Peak power density', 'd.Ipk.desc': 'At the centre of the spot.',
         'd.out': 'Overall efficiency', 'd.out.desc': 'Beam into current.',
         'c.safe': 'The beam is over the safety limit', 'c.fade': 'The air is absorbing the beam', 'v.P': 'Beam power',
         'disturb': 'Pointing jitter: the spot widens by 1%·d.', 'rate.desc': 'What the field catches and rectifies.'},
        extreme('microwave_rectenna', 1, P_CTRL, {'F': 'rectenna_panel', 'T': 'radio_dish_panel', 'M': 'doped_wafer', 'B': 'superconducting_coil'}),
        multiblock=True)
casing('rectenna_panel', 'Rectenna Panel', "A square of half-wave dipoles over a wire ground mesh, a Schottky diode at each feed: "
       "it turns microwaves into direct current. Made by crafting.")

# ====================================================================== items and blocks
item('zblan_fibre', 'ZBLAN Fibre', "Optical fibre of a fluoride glass (zirconium, barium, lanthanum, aluminium and sodium "
     "fluorides) drawn from a preform that was cooled in orbit. On the ground ZBLAN crystallises as it cools and scatters light; "
     "made in free fall it can be a hundred times clearer than silica. Made in the Microgravity Factory.")
item('multijunction_solar_cell', 'Multijunction Solar Cell', "Three solar cells grown one over another, each tuned to a band of "
     "the spectrum, on a flawless crystal substrate: a third of the sunlight becomes current. Made by crafting.")
item('microwave_phased_array', 'Microwave Phased Array', "Thousands of small antennas, each with its own amplifier, fed in step "
     "so their waves add up into one steerable beam. Made by crafting.")
SATS = {
    'weather_satellite': ('Weather Satellite', "An imager and a sounder on a polar platform, to be flown into a sun-synchronous orbit. "
                          "In orbit it watches the clouds form and sends a forecast down before rain or thunder reaches you."),
    'solar_monitor_satellite': ('Solar Monitor', "A plasma analyser, a magnetometer and a coronagraph, to be flown to the Lagrange point L1 "
                                "between Earth and the Sun. There it sees a solar storm coming and warns everyone three minutes ahead."),
    'relay_satellite': ('Relay Satellite', "A geostationary communications satellite with high-gain dishes and ZBLAN fibre links. With "
                        "one in orbit, probes stay in touch all the way, and a Mission Bay's probes bring home half as much again."),
    'power_satellite': ('Power Satellite', "A sail of multijunction cells a kilometre wide and a phased array beneath it, for geostationary "
                        "orbit, where the Sun nearly always shines. It beams its power to up to four Microwave Rectennas on the ground."),
    'debris_sweeper': ('Debris Sweeper', "A small craft with a pulsed laser and a drag sail: it nudges fragments into the air to burn up, "
                       "then drags itself down after them. Launched, it clears most of the debris in orbit."),
}
for sid, (name, lore) in SATS.items():
    item(sid, name, lore + " Launch it with a Rocket Assembly. Made by crafting.")
simple_block('storm_shelter_module', 'Storm Shelter Module', "A small room walled with water tanks and polyethylene, where the crew "
             "waits out a solar storm, as on the ISS and Orion. Standing within two blocks of it you take a fiftieth of a storm's dose. "
             "Works aboard the Orbital Station and in the Accretion Frontier. Made by crafting.")
simple_block('habitat_ring_module', 'Habitat Ring Module', "The hub of a ring that spins for the crew, docked to the station. Aboard "
             "the Orbital Station, within twelve blocks of it you have your full weight back, and the free fall's toll on bone and "
             "muscle does not build up (it slowly goes). Made by crafting.", light=True)

# ====================================================================== recipes: the Microgravity Factory
GOOD = 'microgravity_processing'
# silicon melt: low viscosity, high diffusivity (a liquid metal): its Rayleigh number is small, and only wide melts matter
SI = {'beta': 1.4e-4, 'nu': 3e-7, 'alpha': 2.2e-5, 'sigT': 2.8e-4, 'mu': 7e-4, 'Mac': 4e4}
# ZBLAN glass: very viscous
ZB = {'beta': 3e-5, 'nu': 1e-3, 'alpha': 4e-7, 'sigT': 1e-4, 'mu': 3.0, 'Mac': 5e3}
# protein solution: water
PR = {'beta': 2e-4, 'nu': 1e-6, 'alpha': 1.4e-7, 'sigT': 1.5e-4, 'mu': 1e-3, 'Mac': 2e5}
mrec(GOOD, 'r1130_microgravity_crystal', [('silicon_boule', 1)], [], [('microgravity_crystal', 4)], [], 4096, 600, 'microgravity_processing',
     SI, [('Ra', 0, 30), ('wid', 25, 40), ('vg', 0.3, 1.5)], 'microgravity_factory')
mrec(GOOD, 'r1130_zblan_fibre', [('fluorspar', 4), ('rare_earth_oxide', 1)], [], [('zblan_fibre', 4)], [], 4096, 600, 'microgravity_processing',
     ZB, [('Ra', 0, 1), ('wid', 10, 30), ('vg', 0.2, 1.0)], 'microgravity_factory')
mrec(GOOD, 'r1130_folded_protein_crystal', [('amino_acid_mixture', 2)], [('minecraft:water', 500)], [('folded_protein_crystal', 2)], [], 2048, 600,
     'microgravity_processing', PR, [('Ra', 0, 5), ('wid', 15, 40), ('vg', 0.1, 0.5)], 'microgravity_factory')

# ====================================================================== recipes: the Microwave Rectenna
mrec('microwave_reception', 'r1130_power_beam', [], [], [], [], 8192, 400, 'space_solar_power',
     {'P': 2e9}, [('eta', 0.85, 1.0), ('Ipk', 100, 230)], 'microwave_rectenna')

# ====================================================================== recipes: launches (the Rocket Assembly)
LAUNCH = {   # satellite: (payload tonnes, Δv to its orbit from the ground, research)
    'weather_satellite': (4, 9800, 'satellite_orbits'),
    'solar_monitor_satellite': (1, 12600, 'satellite_orbits'),
    'relay_satellite': (5, 13200, 'satellite_orbits'),
    'power_satellite': (5, 13200, 'space_solar_power'),
    'debris_sweeper': (3, 9600, 'orbital_debris'),
}
for sid, (mp, drq, rs) in LAUNCH.items():
    mrec('orbital_launch', 'r1130_launch_' + sid, [('rocket_stage', 2), (sid, 1)], [('liquid_oxygen', 20000), ('liquid_hydrogen', 4000)], [], [],
         4096, 600, rs, {'Ipk': 450.0, 'rpk': 5.0, 'rs': 8.0, 'mp': float(mp), 'drq': float(drq)}, [('margin', 0, 1e6)], 'rocket_assembly')
verify_recipes()


# a check of our own: none of the Microgravity Factory's recipes can be made on the ground
def ground_check():
    m = json.load(open(os.path.join(D, 'ts_physics', 'microgravity_factory.json'), encoding='utf-8'))
    for mid, rid, r in T.RECIPES:
        if mid != 'microgravity_factory':
            continue
        T.WORLD['microgravity_factory'] = {'grav': 9.81}
        try:
            check_reachable(mid, m, dict(r, _id=rid))
            raise SystemExit('GROUND-REACHABLE: ' + rid)
        except SystemExit as e:
            if 'GROUND' in str(e):
                raise
        finally:
            T.WORLD['microgravity_factory'] = {'grav': STATION_G}
    print('  microgravity recipes: none can be made on the ground')


ground_check()


# ====================================================================== recipes: crafting
def shaped(rid, pattern, key, result, count=1):
    dump(os.path.join(D, 'recipe/crafting', rid + '.json'),
         {'type': 'minecraft:crafting_shaped', 'pattern': pattern, 'key': {k: ({'item': v} if ':' in v else {'item': ts(v)}) for k, v in key.items()},
          'result': {'id': ts(result), 'count': count}})


dump(os.path.join(D, 'recipe/crafting', 'r1130_rectenna_panel.json'), extreme('rectenna_panel', 16, P_PART, {'O': 'minecraft:copper_ingot', 'I': 'doped_wafer'}))
shaped('r1130_multijunction_solar_cell', ['DDD', 'GCG', 'DDD'], {'D': 'doped_wafer', 'G': 'minecraft:gold_nugget', 'C': 'microgravity_crystal'}, 'multijunction_solar_cell', 2)
shaped('r1130_microwave_phased_array', ['DRD', 'RAR', 'DRD'], {'D': 'doped_wafer', 'R': 'minecraft:copper_ingot', 'A': 'radio_dish_panel'}, 'microwave_phased_array')
shaped('r1130_weather_satellite', ['S S', 'GVG', 'S S'], {'S': 'solar_array', 'G': 'precision_gyroscope', 'V': 'survey_satellite'}, 'weather_satellite')
shaped('r1130_solar_monitor_satellite', ['S S', 'MVC', 'S S'], {'S': 'solar_array', 'M': 'neodymium_magnet', 'V': 'survey_satellite',
                                                              'C': 'scintillator_crystal'}, 'solar_monitor_satellite')
shaped('r1130_relay_satellite', ['DSD', 'ZGZ', 'DSD'], {'D': 'radio_dish_panel', 'S': 'solar_array', 'Z': 'zblan_fibre', 'G': 'precision_gyroscope'}, 'relay_satellite')
shaped('r1130_power_satellite', ['MMM', 'SGS', 'PPP'], {'M': 'multijunction_solar_cell', 'S': 'solar_array', 'G': 'precision_gyroscope',
                                                      'P': 'microwave_phased_array'}, 'power_satellite')
shaped('r1130_debris_sweeper', ['LSL', 'GHG', 'LSL'], {'L': 'light_sail', 'S': 'solar_array', 'G': 'precision_gyroscope', 'H': 'laser_head'}, 'debris_sweeper')
shaped('r1130_storm_shelter_module', ['WPW', 'PHP', 'WPW'], {'W': 'water_shield_tank', 'P': 'polyethylene_shielding', 'H': 'station_hull'}, 'storm_shelter_module')
shaped('r1130_habitat_ring_module', ['HWH', 'HBH', 'SSS'], {'H': 'habitat_hull', 'W': 'habitat_window', 'B': 'spin_bearing', 'S': 'station_hull'}, 'habitat_ring_module')

# ====================================================================== tags
BLK = ['storm_shelter_module', 'habitat_ring_module', 'rectenna_panel', 'microwave_rectenna', 'microgravity_factory']
tag_add(os.path.join(MC, 'tags/block/mineable/pickaxe.json'), [ts(x) for x in BLK])
tag_add(os.path.join(MC, 'tags/block/needs_iron_tool.json'), [ts(x) for x in ['rectenna_panel', 'microwave_rectenna', 'microgravity_factory']])
for kind in ('block', 'item'):
    tag_add(os.path.join(D, 'tags', kind, 'casings.json'), [ts('rectenna_panel')])
    tag_add(os.path.join(D, 'tags', kind, 'machines.json'), [ts('microwave_rectenna'), ts('microgravity_factory')])
    tag_add(os.path.join(D, 'tags', kind, 'multiblock_controllers.json'), [ts('microwave_rectenna')])
tag_add(os.path.join(D, 'tags/block/radiation_shielding.json'), [ts('storm_shelter_module')])

# ====================================================================== the multiblock: a field of dipoles
W, H, Dp = 11, 2, 11
g = grid(W, H, Dp)
for z in range(Dp):
    for x in range(W):
        g[0][z][x] = 'F'
        if (x + z) % 2 == 0 and 0 < x < W - 1 and 0 < z < Dp - 1:
            g[1][z][x] = 'F'
c = W // 2
g[0][0][c] = 'C'
g[0][0][c - 2] = 'P'
g[0][0][c + 2] = 'P'
T.MB_COUNTS['microwave_rectenna'] = write_mb('microwave_rectenna', rows(g), {'F': ['rectenna_panel'], 'P': ['rectenna_panel'] + PORT},
                                             extra={'effect_center': [c, 2, 5], 'effect_scale': 4.0,
                                                    'fx': [{'type': 'glow', 'chars': 'F', 'color': '#60C8FF', 'hot': '#E0F4FF'}]})
print('rectenna blocks:', T.MB_COUNTS['microwave_rectenna'])

# ====================================================================== text the Java reads
LANGX = {
    'message.%s.satellite_launched': '%s is in orbit (%s in orbit now).',
    'message.%s.satellite_lost': 'Debris struck %s in orbit; it is lost, and its wreckage adds to the debris.',
    'message.%s.sweeper_done': 'The Debris Sweeper has cleared most of the debris in orbit.',
    'message.%s.debris_strike': 'A debris fragment strikes you: a Whipple bumper would have stopped it.',
    'message.%s.debris_warning': 'Debris is thick in orbit: stay inside the hub, or wear a Whipple bumper.',
    'message.%s.forecast_rain': 'Weather Satellite: rain in about five minutes.',
    'message.%s.forecast_thunder': 'Weather Satellite: a thunderstorm in about five minutes.',
    'message.%s.forecast_clear': 'Weather Satellite: the rain will clear in about five minutes.',
    'message.%s.storm_warning': 'Solar Monitor: a solar storm will arrive in about three minutes.',
    'message.%s.deconditioned': 'Weeks of free fall have weakened your bones and muscles: you will be slow (and later weak) until you recover in gravity.',
    'message.%s.reconditioned': 'Your strength has come back.',
    'message.%s.orbit_status': 'In orbit: %s. Debris: %s.',
    'message.%s.orbit_status_none': 'no satellites',
    'message.%s.debris_off': 'none (turned off)',
    'message.%s.debris_level': '%s (strikes %s)',
    'message.%s.debris_rare': 'rare', 'message.%s.debris_some': 'now and then', 'message.%s.debris_often': 'often',
    'status.%s.no_beam': 'No beam: put a Power Satellite in orbit (each feeds four rectennas)',
    'status.%s.no_beam.short': 'No beam',
}
for k, v in LANGX.items():
    L[k % NS] = v
for sid, (name, _) in SATS.items():
    L['satellite.%s.%s' % (NS, sid)] = name

# ====================================================================== the Codex
E = {}
E['fluids/microgravity_processing'] = {'title': 'Microgravity Processing', 'icon': ts('microgravity_factory'), 'category': 'fluids', 'research': 'microgravity_processing', 'pages': [
    spot(['microgravity_factory'], "Ra = gβΔT·L³/(να). On the ground a melt with a gradient across it stirs itself; aboard the station, where gravity is ten millionths of the ground's, it stays still. The $(thing)Microgravity Factory$() works only aboard the Orbital Station.", 'Microgravity Factory'),
    spot(['microgravity_crystal', 'zblan_fibre', 'folded_protein_crystal'], "It grows Microgravity Crystals from a Silicon Boule (cheaper than flying a whole lab), draws ZBLAN Fibre from fluorspar and rare earths, and crystallises proteins from amino acids. Keep Ra under its window, cap the free surface against Marangoni flow, and make the melt wide.", 'Products')]}
E['fluids/station_modules'] = {'title': 'Station Modules', 'icon': ts('habitat_ring_module'), 'category': 'fluids', 'research': 'space_radiation', 'pages': [
    text("Weeks in free fall cost bone and muscle: here a Minecraft day aboard the station stands for a week. Back in gravity you are slow, and after long stays weak, until you recover (a quarter of the time you were up). (It can be turned off in the config.)"),
    spot(['habitat_ring_module'], "A $(item)Habitat Ring Module$() spins for the crew. Within twelve blocks of it aboard the station you have your full weight back, and the toll does not build up; it slowly goes.", 'Habitat Ring'),
    spot(['storm_shelter_module'], "A $(item)Storm Shelter Module$(), walled with water and polyethylene: within two blocks of it a solar storm's dose is a fiftieth. Aboard the station or in the Accretion Frontier.", 'Storm Shelter')]}
E['fluids/satellite_orbits'] = {'title': 'Satellite Orbits', 'icon': ts('relay_satellite'), 'category': 'fluids', 'research': 'satellite_orbits', 'pages': [
    text("r = ∛(μT²/4π²). Satellites are launched by the $(thing)Rocket Assembly$(), each to its own orbit: the further, the more Δv, so lighter payloads go higher. Sneak and use a Transfer Console to see what is in orbit."),
    spot(['weather_satellite', 'solar_monitor_satellite', 'relay_satellite'], "$(item)Weather Satellite$() (sun-synchronous, 9.8 km/s): forecasts rain, thunder and clearing five minutes ahead. $(item)Solar Monitor$() (L1, 12.6 km/s): warns of solar storms three minutes ahead. $(item)Relay Satellite$() (geostationary, 13.2 km/s): a Mission Bay's probes bring home half as much again.", 'Satellites')]}
E['fluids/space_solar_power'] = {'title': 'Space Solar Power', 'icon': ts('power_satellite'), 'category': 'fluids', 'research': 'space_solar_power', 'pages': [
    spot(['power_satellite', 'microwave_rectenna'], "d = 2.44λR/D. A $(item)Power Satellite$() in geostationary orbit beams microwaves to the ground; a $(thing)Microwave Rectenna$() (%d blocks: a field of dipoles) turns them back into current, day and night. Each Power Satellite feeds four rectennas." % T.MB_COUNTS['microwave_rectenna'], 'Power Satellite'),
    {'type': 'multiblock', 'name': 'Microwave Rectenna', 'text': 'Interface Ports either side of the controller.', 'multiblock': 'microwave_rectenna'},
    text("A wider transmitter or a higher frequency focuses the beam, so the field catches more of it, but the spot's centre must stay under 230 W/m² (the safety limit), and above 12 GHz (6 in rain) the air absorbs it. Stringing more of the field together catches more, with more wiring loss.")]}
E['fluids/orbital_debris'] = {'title': 'Orbital Debris', 'icon': ts('debris_sweeper'), 'category': 'fluids', 'research': 'orbital_debris', 'pages': [
    text("E = ½mv². Every launch leaves debris in orbit, and the thin air drags it down slowly (a fiftieth a day). While it is thick, strikes hurt anyone outside the station's hub without a Whipple bumper (a Whipple Shield or Spacefarer piece), and can destroy satellites; a destroyed satellite adds more (Kessler syndrome). It can be turned off in the config."),
    spot(['debris_sweeper'], "A $(item)Debris Sweeper$(), launched by the Rocket Assembly, clears most of it.", 'Debris Sweeper')]}
OPS = {'microgravity_factory': ("Keep the melt still: aboard the station its Rayleigh number is small, but a wide melt raises it as the cube. Cap the free surface against surface-tension flow, and use a steep gradient to grow fast.",
                                'Temperature difference, Melt width, Encapsulation', 'g-jitter: the residual gravity rises by 1%·d.',
                                'Ra over 1708 stops it (it convects); Ma over its critical value wears it (striations).', 'steeper and wider, faster; a cap slows it.',
                                ['Ra = gβΔT·L³/(να)', 'Ma = (dσ/dT)·ΔT·L/(μα)·(1 − cap)', 'growth = 0.05·ΔT·(1 − 0.6·cap) mm/h'], 'microgravity_processing'),
       'microwave_rectenna': ("Aim the beam so the field catches most of it while its centre stays under the safety limit.",
                              'Transmitter aperture, Beam frequency, Field in use', 'pointing jitter: the spot widens by 1%·d.',
                              'over 230 W/m² it stops; above 12 GHz (6 in rain) it wears.', 'what the field catches and rectifies.',
                              ['λ = c/f', 'd = 2.44λR/D', 'η = 1 − e^(−2(Dr/d)²)', 'I = 8P/(πd²)'], 'space_solar_power')}
for mid, (intro, sets, dist, lim, rate, eqs, rs) in OPS.items():
    E['operation/' + mid] = {'title': T.MACHINES[mid]['name'], 'icon': ts(mid), 'category': 'operation', 'research': rs,
                             'pages': op_pages(mid, intro, sets, dist, lim, rate, eqs)}
NODES = {'physics': [{'entry': 'fluids/microgravity_processing', 'x': 45, 'y': 89, 'parents': ['fluids/rocket_equation']},
                     {'entry': 'fluids/station_modules', 'x': 47, 'y': 89, 'parents': ['fluids/space_radiation']},
                     {'entry': 'fluids/satellite_orbits', 'x': 51, 'y': 85, 'parents': ['fluids/transfer_orbits']},
                     {'entry': 'fluids/space_solar_power', 'x': 53, 'y': 85, 'parents': ['fluids/satellite_orbits']},
                     {'entry': 'fluids/orbital_debris', 'x': 51, 'y': 87, 'parents': ['fluids/satellite_orbits']}],
         'machines': [{'entry': 'operation/microgravity_factory', 'x': 30, 'y': 22, 'parents': []},
                      {'entry': 'operation/microwave_rectenna', 'x': 32, 'y': 22, 'parents': []}]}
codex(E, NODES)

finish()
print('research', len(T.RESEARCH), 'machines', len(T.MACHINES), 'recipes', len(T.RECIPES))
