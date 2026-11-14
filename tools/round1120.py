"""
1.12.0 (Patch C1): getting there. Space travel stops being free.

  * The Transfer Console becomes a Mission Planner: every destination costs a delta-v (in its data file, with real
    ratios), and its transfer windows open on a schedule (the synodic period, a real month to a Minecraft day). The
    first trip needs Transfer Stages to pay for it; a Return Beacon set down at the destination charts the route, and
    after that the station goes back at no cost. The way home is always free.
  * Transfer Stages in four engine tiers (hydrolox and methalox, nuclear thermal, ion, antimatter), fired and filled in
    a new multiblock, the Propulsion Test Stand, whose console is the rocket equation.
  * A Mission Bay on the station flies Sample Return Probes to a near-Earth asteroid, Titan, and (when Ad Astra is not
    installed, since then you can go there yourself) the Moon, Mars, Venus and Mercury. Missions take the time of their
    transfer orbit and come back with samples.
  * Space weather reaches space: a solar particle storm floods the station and the Frontier with protons, which water
    and polyethylene stop. The Van Allen belts are crossed on the way up. The solar wind machines run harder in a storm.
  * The suits become a line: Refractory into Whipple, Whipple into Frontier, Pressure into Heated (as before), and
    Frontier with Heated into the Spacefarer Suit, which counts as every suit at once.

Run:  python3 tools/round1120.py
"""
import json, math, os, sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from tsgen import *          # noqa: F401,F403
import tsgen as T

L = T.LANG
G0 = 9.80665

# ====================================================================== research
research('rocket_equation', 'The Rocket Equation', 19, 7, 260, ['orbital_mechanics', 'rocketry'],
         [('rocket_stage', 1), ('liquid_hydrogen_cell', 2)], 300,
         "A rocket is pushed forward by everything it throws back. Each bit of exhaust leaving at speed ve takes momentum "
         "with it, and the rocket, now a little lighter, gains the same. Add it up over the whole burn and the change in "
         "speed is ve times the logarithm of how much heavier the rocket was full than empty. That logarithm is why "
         "rockets are mostly propellant, and why a better engine (a faster exhaust) is worth more than a bigger tank. "
         "Unlocks the Propulsion Test Stand, a multiblock: Transfer Stages.",
         'Mostly propellant',
         {'goal': 'A rocket of mass m throws back propellant dm at speed ve relative to itself. Momentum is kept, so m·dv = −ve·dm. '
                  'Integrate from the full mass m0 to the empty mass mf: write the change in speed Δv.',
          'name': 'Tsiolkovsky', 'lhs': 'Δv', 'form': 've * ln ( m0 / mf )', 'expr': 've * ln ( m0 / mf )',
          'ask': 'Find Δv in km/s for ve = @{ve} km/s, m0 = @{m0} t and mf = 25 t.',
          'vars': {'Δv': 'change in speed', 've': 'exhaust speed', 'm0': 'mass full', 'mf': 'mass empty'},
          'given': {'ve': 4.4, 'm0': 100.0, 'mf': 25.0}, 'unit': 'km/s',
          'method': 'dv = −ve·dm/m; integrate: Δv = ve·(ln m0 − ln mf).',
          'distractors': ['*', '-', 'exp'], 'vary': {'ve': [3.0, 9.0, 0.1], 'm0': [40.0, 200.0, 5.0]}},
         'rocket_stage', 'orbital_mechanics')
research('transfer_orbits', 'Transfer Orbits', 21, 7, 300, ['rocket_equation'],
         [('rocket_stage', 2), ('minecraft:compass', 2)], 320,
         "The cheapest way between two circular orbits is half an ellipse that touches both: Hohmann's transfer, one burn to "
         "leave and one to arrive. It only works when the target is where the ellipse will meet it, so the planets must "
         "stand in the right places: the transfer window, which comes round every synodic period (for Mars, every 26 "
         "months). Leave at another time and the trip costs far more: the Transfer Console's Mission Planner draws this as a "
         "porkchop plot. Unlocks the Return Beacon, which charts a route so that later trips cost nothing.",
         'Half an ellipse, at the right time',
         {'goal': 'In an orbit about a body of gravitational parameter μ, the speed v at distance r on an ellipse of semi-major axis a '
                  'follows from energy: v²/2 − μ/r = −μ/(2a). Write v.',
          'name': 'Vis-viva', 'lhs': 'v', 'form': 'sqrt ( μ * ( 2 / r - 1 / a ) )', 'expr': 'sqrt ( mu * ( 2 / r - 1 / a ) )',
          'ask': 'Find v in km/s for μ = 398600 km³/s², r = @{r} km and a = @{a} km.',
          'vars': {'v': 'speed', 'μ': 'gravitational parameter', 'r': 'distance from the centre', 'a': 'semi-major axis'},
          'given': {'mu': 398600.0, 'r': 6778.0, 'a': 24400.0}, 'unit': 'km/s',
          'method': 'The energy per unit mass of an orbit is −μ/(2a), whatever its shape; the kinetic part is what is left over.',
          'distractors': ['+', '*', 'ln'], 'vary': {'r': [6600.0, 8000.0, 50.0], 'a': [15000.0, 42000.0, 500.0]}},
         'orbital_transfer_console', 'rocket_equation')
research('sample_return', 'Sample Return', 23, 7, 320, ['transfer_orbits'],
         [('survey_probe', 1), ('minecraft:glass_bottle', 4)], 360,
         "Hayabusa2 brought back grains of the asteroid Ryugu; OSIRIS-REx brought back Bennu's; Chang'e 5 brought back the "
         "Moon. A probe flies the transfer orbit out, takes its sample, and flies it home: half an ellipse each way, so the "
         "time of the trip is set by Kepler's third law. Unlocks the Mission Bay and the Sample Return Probe.",
         'Bringing a world home',
         {'goal': 'A Hohmann transfer between circular orbits of radii r1 and r2 about a body of parameter μ is half an ellipse of '
                  'semi-major axis (r1 + r2)/2. Write the time t it takes (half the period).',
          'name': 'Time of flight', 'lhs': 't', 'form': 'π * sqrt ( ( r1 + r2 ) ^ 3 / ( 8 * μ ) )', 'expr': 'π * sqrt ( ( r1 + r2 ) ^ 3 / ( 8 * mu ) )',
          'ask': 'Find t in days for r1 = 1 AU, r2 = @{r2} AU and μ of the Sun = 39.48 AU³/year² (give years × 365.25).',
          'vars': {'t': 'time of flight', 'r1': 'inner radius', 'r2': 'outer radius', 'μ': 'gravitational parameter'},
          'given': {'r1': 1.0, 'r2': 1.524, 'mu': 39.48}, 'unit': 'years',
          'method': "Kepler's third law: P = 2π√(a³/μ). Half of it, with a = (r1 + r2)/2.",
          'distractors': ['*', '2', 'ln'], 'vary': {'r2': [1.2, 9.6, 0.1]}},
         'mission_bay', 'transfer_orbits')
research('nuclear_thermal_propulsion', 'Nuclear Thermal Propulsion', 19, 5, 320, ['rocket_equation', 'nuclear_fission'],
         [('uranium_235_pellet', 2), ('graphite_moderator', 2)], 360,
         "A chemical rocket's exhaust is heavy water vapour; a nuclear thermal rocket heats pure hydrogen in a reactor "
         "instead. The exhaust speed goes as the square root of temperature over molar mass, and hydrogen's molar mass is "
         "2 against steam's 18, so at a lower temperature it still leaves twice as fast. NERVA ran on the test stand in the "
         "1960s at about 900 seconds of specific impulse. Unlocks the Nuclear Thermal Core.",
         'Light exhaust',
         {'goal': 'A gas of ratio of specific heats γ and molar mass M leaves a chamber at temperature T through an ideal nozzle into '
                  'vacuum. Write its exhaust speed ve, with R the gas constant.',
          'name': 'Exhaust speed', 'lhs': 've', 'form': 'sqrt ( 2 * γ / ( γ - 1 ) * R * T / M )', 'expr': 'sqrt ( 2 * g / ( g - 1 ) * R * T / M )',
          'ask': 'Find ve in m/s for hydrogen (γ = 1.4, M = 0.002 kg/mol) at T = @{T} K, R = 8.314.',
          'vars': {'ve': 'exhaust speed', 'γ': 'ratio of specific heats', 'R': 'gas constant', 'T': 'chamber temperature', 'M': 'molar mass'},
          'given': {'g': 1.4, 'R': 8.314, 'T': 2700.0, 'M': 0.002}, 'unit': 'm/s',
          'method': "The gas's enthalpy cp·T, all of it turned into motion: ve² = 2·cp·T, with cp = γR/((γ − 1)M).",
          'distractors': ['*', '+', 'ln'], 'vary': {'T': [1500.0, 3000.0, 50.0]}},
         'nuclear_thermal_core', 'rocket_equation')
research('electric_propulsion', 'Electric Propulsion', 21, 5, 360, ['rocket_equation', 'electromagnetic_induction'],
         [('neodymium_magnet', 2), ('noble_gas_clathrate', 2)], 400,
         "An ion engine does not burn anything: it ionises a heavy gas (xenon, here freed from a clathrate) and pulls the "
         "ions out through charged grids. The exhaust leaves ten times faster than any flame, but the thrust is a breath, "
         "so an ion stage takes months to do what a chemical one does in minutes. Dawn and DS1 flew this way. Unlocks the "
         "Ion Thruster Grid.",
         'Thrown by a field',
         {'goal': 'An ion of charge q and mass m falls through a potential difference V between the grids. Write the speed v it leaves '
                  'with.',
          'name': 'Ion exhaust', 'lhs': 'v', 'form': 'sqrt ( 2 * q * V / m )', 'expr': 'sqrt ( 2 * q * V / m )',
          'ask': 'Find v in km/s for a xenon ion (q = 1.602e-19 C, m = 2.18e-25 kg) through V = @{V} V (give m/s ÷ 1000).',
          'vars': {'v': 'exhaust speed', 'q': 'ion charge', 'V': 'grid voltage', 'm': 'ion mass'},
          'given': {'q': 1.602e-19, 'V': 1100.0, 'm': 2.18e-25}, 'unit': 'm/s',
          'method': 'The field does work qV on the ion; all of it becomes kinetic energy, ½mv².',
          'distractors': ['*', '+', 'exp'], 'vary': {'V': [500.0, 2000.0, 50.0]}},
         'ion_thruster_grid', 'rocket_equation')
research('antimatter_propulsion', 'Antimatter Propulsion', 23, 5, 480, ['electric_propulsion', 'annihilation_power'],
         [('antiproton_bundle', 1), ('superconducting_coil', 2)], 480,
         "Antiprotons meeting hydrogen turn their whole mass into energy: charged pions leaving at near the speed of light, "
         "which a magnetic nozzle can turn into thrust. A plasma-core engine of this kind would have a specific impulse in "
         "the tens of thousands of seconds: a gram of antiprotons holds the energy of forty kilotons. Unlocks the Antimatter "
         "Injector.",
         'Mass into motion',
         {'goal': 'An antiproton meets a proton and both vanish into energy. Write the energy E released when a mass m of antimatter '
                  'annihilates with as much matter, with c the speed of light.',
          'name': 'Annihilation', 'lhs': 'E', 'form': '2 * m * c ^ 2', 'expr': '2 * m * c ^ 2',
          'ask': 'Find E in TJ for m = @{m} g (give J ÷ 1e12, with m in kg and c = 3e8 m/s).',
          'vars': {'E': 'energy released', 'm': 'mass of antimatter', 'c': 'speed of light'},
          'given': {'m': 0.001, 'c': 3.0e8}, 'unit': 'J',
          'method': "E = mc² for each: the antimatter and the matter it meets, both turned wholly into energy.",
          'distractors': ['+', '/', 'sqrt'], 'vary': {'m': [0.0001, 0.01, 0.0001]}},
         'antimatter_injector', 'electric_propulsion')
research('space_radiation', 'Radiation in Space', 20, 9, 280, ['space_weather', 'orbital_mechanics'],
         [('polymer_sheet', 4), ('minecraft:water_bucket', 1)], 300,
         "Above the air there is nothing between you and two kinds of radiation. Cosmic rays are few and very fast, and "
         "almost nothing stops them. Solar storms are many protons at lower energy, and a little hydrogen-rich material "
         "stops them: water, or polyethylene, which is why the ISS's sleeping quarters are lined with it. On the way up you "
         "also cross the Van Allen belts, where Earth's field holds trapped particles. Unlocks Polyethylene Shielding and "
         "the Water Shield Tank.",
         'Hydrogen stops protons',
         {'goal': 'Radiation passing a shield of areal density x, with an attenuation length λ for that radiation in that material, falls '
                  'off exponentially. Write the fraction f that gets through.',
          'name': 'Attenuation', 'lhs': 'f', 'form': 'exp ( - x / λ )', 'expr': 'exp ( - x / L )',
          'ask': 'Find f for x = @{x} g/cm² of water and λ = 8 g/cm² (storm protons).',
          'vars': {'f': 'fraction through', 'x': 'areal density', 'λ': 'attenuation length'},
          'given': {'x': 20.0, 'L': 8.0}, 'unit': '',
          'method': 'Each thin layer removes the same fraction of what reaches it: df/f = −dx/λ.',
          'distractors': ['*', '-', 'ln'], 'vary': {'x': [2.0, 40.0, 1.0]}},
         'polyethylene_shielding', 'space_weather')

# ====================================================================== the Propulsion Test Stand
PT = model([par('Tc', 'K', 1000, 4000, 10, 2500), par('eps', '', 5, 200, 1, 40), par('zeta', '', 0.3, 0.95, 0.01, 0.6),
            par('Vg', 'kV', 0.5, 5, 0.05, 1.0)],
           {'M': 13, 'g': 1.2, 'ion': 0, 'Tmax': 3600},
           [der('eta', '', '1 - eps^(-1.7*(g - 1)/g)', 'nozzle efficiency: the share of the heat the expansion turns into speed'),
            der('ve', 'm/s', '(1 - ion)*sqrt(2*g/(g - 1)*8.314*Tc/(M/1000)*(1 - eps^(-1.7*(g - 1)/g)))*(1 - 0.01*d)'
                             ' + ion*0.75*sqrt(2*1.602e-19*Vg*1000/(M*1.6605e-27))*(1 - 0.01*d)',
                'thermal: ve = √(2γ/(γ−1)·RT/M·η); ion: ve = √(2qV/m)'),
            der('Isp', 's', '((1 - ion)*sqrt(2*g/(g - 1)*8.314*Tc/(M/1000)*(1 - eps^(-1.7*(g - 1)/g))) + ion*0.75*sqrt(2*1.602e-19*Vg*1000/(M*1.6605e-27)))*(1 - 0.01*d)/9.80665',
                'Isp = ve/g₀'),
            der('dv', 'km/s', '((1 - ion)*sqrt(2*g/(g - 1)*8.314*Tc/(M/1000)*(1 - eps^(-1.7*(g - 1)/g))) + ion*0.75*sqrt(2*1.602e-19*Vg*1000/(M*1.6605e-27)))*(1 - 0.01*d)*ln(1/(1 - zeta))/1000',
                'Δv = ve·ln(m₀/m_f), with m₀/m_f = 1/(1 − ζ)')],
           [con('melt', 'wear', 'Tmax - Tc*(1 - ion)', 'chamber under its limit, or the throat erodes'),
            con('tank', 'stop', '0.93 - zeta', 'propellant under 93% of the stage, or the tank walls are too thin to hold it'),
            con('arc', 'wear', '4.6 - Vg*ion', 'grids under 4.6 kV, or they arc')],
           'clamp(0.4 + Tc*(1 - ion)/4000 + ion*Vg/3, 0.3, 1.5)', 'hotter burns (or higher grid voltage) finish the firing sooner')
machine('propulsion_test_stand', 'Propulsion Test Stand', "The controller of an engine test stand: a steel tower over a "
        "concrete flame trench, the engine bolted to it nozzle down, tanks of propellant beside, the exhaust thrown sideways by "
        "a curved deflector. Engines are fired here until they make their rated thrust, then built into stages and filled. "
        "A multiblock.", 'chain', 3, PT,
        {'p.Tc': 'Chamber temperature', 'p.Tc.desc': 'Hotter is faster exhaust, up to what the chamber stands.',
         'p.eps': 'Expansion ratio', 'p.eps.desc': 'How far the nozzle opens out: more turns more of the heat into speed.',
         'p.zeta': 'Propellant fraction', 'p.zeta.desc': 'How much of the stage is propellant: the more, the more Δv; past 93% the tank will not hold.',
         'p.Vg': 'Grid voltage', 'p.Vg.desc': 'Ion stages only: the voltage that throws the ions. Over 4.6 kV the grids arc.',
         'd.eta': 'Nozzle efficiency', 'd.eta.desc': 'Of the expansion.', 'd.ve': 'Exhaust speed', 'd.ve.desc': 'How fast the exhaust leaves.',
         'd.Isp': 'Specific impulse', 'd.Isp.desc': 'Exhaust speed in seconds of standard gravity: the engine\'s efficiency.',
         'd.dv': 'Stage Δv', 'd.dv.desc': 'What the filled stage can change its speed by.',
         'c.melt': 'The throat is eroding', 'c.tank': 'The tank walls are too thin', 'c.arc': 'The grids are arcing',
         'v.M': 'Exhaust molar mass', 'v.g': 'Ratio of specific heats', 'v.ion': 'Ion engine', 'v.Tmax': 'Chamber limit',
         'disturb': 'Combustion instability: exhaust speed varies by ±1%·d.', 'rate.desc': 'Hotter burns (or higher grid voltage) finish sooner.'},
        extreme('propulsion_test_stand', 1, P_CTRL, {'F': 'test_stand_frame', 'T': 'propellant_tank_block', 'M': 'flame_trench_block', 'B': 'rocket_engine'}),
        multiblock=True)

casing('test_stand_frame', 'Test Stand Frame', "Heavy steel lattice of an engine test tower, built to hold an engine while it "
       "pushes with the weight of a ship. Made by crafting.")
casing('flame_trench_block', 'Flame Trench', "Refractory concrete faced with a curved steel deflector, cooled by a curtain of "
       "water: it turns the exhaust sideways and survives it. Made by crafting.")
casing('propellant_tank_block', 'Propellant Tank', "An insulated tank for cryogenic propellant, frost on its skin, a vent "
       "pluming at its top. Made by crafting.")

# ====================================================================== items and blocks
STAGE_DV = {'hydrolox_transfer_stage': (6100, 450, 4.0), 'methalox_transfer_stage': (4900, 360, 4.0), 'nuclear_transfer_stage': (9700, 900, 3.0),
            'ion_transfer_stage': (20400, 3000, 2.0), 'antimatter_transfer_stage': (79600, 20000, 1.5)}
item('nuclear_thermal_core', 'Nuclear Thermal Core', "A reactor small enough to sit in a rocket: bundles of uranium carbide fuel "
     "rods in a graphite block, threaded with channels. Liquid hydrogen pumped through the channels leaves the other end at "
     "2,700 K. Made in the Propulsion Test Stand.")
item('ion_thruster_grid', 'Ion Thruster Grid', "A pair of carbon grids drilled with thousands of aligned holes, a millimetre apart, "
     "held at a kilovolt: ions from the discharge chamber behind fall through them and leave at 30 km/s. Made in the Propulsion "
     "Test Stand.")
item('antimatter_injector', 'Antimatter Injector', "A Penning trap feeding a stream of antiprotons into a hydrogen jet inside a "
     "magnetic nozzle. Each annihilation gives off charged pions at near light speed, which the field turns into thrust. Made "
     "in the Propulsion Test Stand.")
for sid, name, eng, lore in [
        ('hydrolox_transfer_stage', 'Hydrolox Transfer Stage', 'hydrogen and oxygen', "Liquid hydrogen and oxygen, the best chemical pair there is. "),
        ('methalox_transfer_stage', 'Methalox Transfer Stage', 'methane and oxygen', "Methane and oxygen: a little less Δv than hydrogen, but both stay liquid at the same temperature and methane can be made on Mars. "),
        ('nuclear_transfer_stage', 'Nuclear Transfer Stage', 'hydrogen heated by a reactor', "Hydrogen heated by a Nuclear Thermal Core: twice a chemical stage's exhaust speed. "),
        ('ion_transfer_stage', 'Ion Transfer Stage', 'xenon thrown by a field', "Xenon, freed from clathrate and thrown by Ion Thruster Grids, the slow way that goes furthest. "),
        ('antimatter_transfer_stage', 'Antimatter Transfer Stage', 'antiprotons annihilating in hydrogen', "Antihydrogen fed through an Antimatter Injector: a plasma-core engine, for the far destinations. ")]:
    dv, isp, mr = STAGE_DV[sid]
    item(sid, name, lore + "Specific impulse %s s, mass ratio %s: Δv = Isp·g₀·ln(m₀/m_f) = %s km/s. Keep it in your inventory and "
         "use a Mission Planner, or load it into a Mission Bay: stages add up (staging), and a trip takes the fewest that pay "
         "for it. Made in the Propulsion Test Stand." % (f'{isp:,}', mr, f'{dv / 1000:.1f}'))
item('sample_return_probe', 'Sample Return Probe', "A small craft with a sampling horn, a capsule to seal the sample in, and a "
     "heat shield to bring it back through the air. Load it into a Mission Bay with Transfer Stages enough for the trip. "
     "Made by crafting.")
SAMPLES = {
    'asteroid_regolith': ('Asteroid Regolith', "Grains from the surface of a near-Earth asteroid, sealed in their capsule since they "
                          "left it: chondritic rock with flecks of native iron-nickel, as Hayabusa2 brought back from Ryugu.",
                          "Brought back by a Mission Bay's Sample Return Probe from a near-Earth asteroid."),
    'lunar_regolith': ('Lunar Regolith', "Grey dust of the Moon's surface, glassy and sharp, never weathered by wind or water. Nearly half "
                       "of it by weight is oxygen, locked in the minerals.",
                       "Brought back by a Mission Bay from the Moon (offered when Ad Astra is not installed; with Ad Astra its Moon Sand serves)."),
    'martian_regolith': ('Martian Regolith', "Red Martian soil, rusty with iron oxide and salted with perchlorate, as the Phoenix lander "
                         "found it.", "Brought back by a Mission Bay from Mars (offered when Ad Astra is not installed; with Ad Astra its Mars Sand serves)."),
    'venusian_aerosol': ('Venusian Aerosol', "Droplets from Venus's clouds, caught by a probe skimming through them at fifty kilometres: "
                         "three parts sulfuric acid to one of water, in air thick with sulfur dioxide.",
                         "Brought back by a Mission Bay from Venus (offered when Ad Astra is not installed; with Ad Astra its Venus Sand serves)."),
    'mercurian_regolith': ('Mercurian Regolith', "Dark rock from Mercury, darker than the Moon's: MESSENGER found it darkened by graphite, "
                           "the remains of the planet's first crust.",
                           "Brought back by a Mission Bay from Mercury (offered when Ad Astra is not installed; with Ad Astra its Mercury Cobblestone serves)."),
}
for sid, (name, lore, found) in SAMPLES.items():
    item(sid, name, lore, found)

simple_block('polyethylene_shielding', 'Polyethylene Shielding', "Thick slabs of polyethylene, the best shield for its weight "
             "against the protons of a solar storm: it is mostly hydrogen, and hydrogen nuclei are what slow a proton best. Each "
             "block between you and open space stops most of a storm. Made by crafting.")
simple_block('water_shield_tank', 'Water Shield Tank', "A tank of water in a steel frame, built into a wall: water is nearly as "
             "good a shield as polyethylene, and the crew can drink it in an emergency. Made by crafting.")
simple_block('return_beacon', 'Return Beacon', "A radio beacon and a laser retroreflector on a tripod. Set it down at a "
             "destination and the route there is charted: from then on the Mission Planner sends you back at no cost, in or out of "
             "a window. Made by crafting.")
simple_block('mission_bay', 'Mission Bay', "A berth for uncrewed craft on the station. Use it with a Sample Return Probe and "
             "Transfer Stages to load them; use it empty-handed to choose a target and fly the mission (it works only aboard the "
             "Orbital Station). The probe is gone for its transfer orbit and back, then its samples wait here (pipes can take them "
             "out). Made by crafting.")

# ====================================================================== recipes: the Propulsion Test Stand
THERMAL_H = {'M': 13, 'g': 1.2, 'ion': 0, 'Tmax': 3600}
THERMAL_CH4 = {'M': 21, 'g': 1.2, 'ion': 0, 'Tmax': 3600}
NTR = {'M': 2, 'g': 1.4, 'ion': 0, 'Tmax': 2900}
ION = {'M': 131, 'g': 1.67, 'ion': 1, 'Tmax': 3600}
AM = {'M': 2, 'g': 1.4, 'ion': 0, 'Tmax': 4000}
mrec('propulsion_testing', 'r1120_hydrolox_transfer_stage', [('rocket_stage', 1), ('rocket_engine', 1), ('pressure_hull_plate', 4)],
     [('liquid_hydrogen', 4000), ('liquid_oxygen', 8000)], [('hydrolox_transfer_stage', 1)], [], 2048, 600, 'rocket_equation',
     THERMAL_H, [('Isp', 440, 480), ('dv', 5.9, 6.8)], 'propulsion_test_stand')
mrec('propulsion_testing', 'r1120_methalox_transfer_stage', [('rocket_stage', 1), ('rocket_engine', 1), ('pressure_hull_plate', 4)],
     [('liquid_methane', 3000), ('liquid_oxygen', 9000)], [('methalox_transfer_stage', 1)], [], 2048, 600, 'rocket_equation',
     THERMAL_CH4, [('Isp', 340, 380), ('dv', 4.7, 5.6)], 'propulsion_test_stand')
mrec('propulsion_testing', 'r1120_nuclear_thermal_core', [('uranium_235_pellet', 6), ('graphite_moderator', 4), ('corundum', 8)],
     [('liquid_hydrogen', 2000)], [('nuclear_thermal_core', 1)], [], 8192, 800, 'nuclear_thermal_propulsion',
     NTR, [('Isp', 820, 940)], 'propulsion_test_stand')
mrec('propulsion_testing', 'r1120_nuclear_transfer_stage', [('rocket_stage', 1), ('nuclear_thermal_core', 1), ('pressure_hull_plate', 6)],
     [('liquid_hydrogen', 16000)], [('nuclear_transfer_stage', 1)], [], 8192, 800, 'nuclear_thermal_propulsion',
     NTR, [('Isp', 820, 940), ('dv', 9.3, 10.6)], 'propulsion_test_stand')
mrec('propulsion_testing', 'r1120_ion_thruster_grid', [('graphene_sheet', 4), ('neodymium_magnet', 2), ('noble_gas_clathrate', 2)],
     [], [('ion_thruster_grid', 1)], [], 16384, 800, 'electric_propulsion', ION, [('Isp', 2800, 3600)], 'propulsion_test_stand')
mrec('propulsion_testing', 'r1120_ion_transfer_stage', [('rocket_stage', 1), ('ion_thruster_grid', 2), ('solar_array', 4), ('noble_gas_clathrate', 8)],
     [], [('ion_transfer_stage', 1)], [], 16384, 1200, 'electric_propulsion', ION, [('Isp', 2800, 3600), ('dv', 18.5, 23.0)], 'propulsion_test_stand')
mrec('propulsion_testing', 'r1120_antimatter_injector', [('superconducting_coil', 4), ('antiproton_bundle', 2), ('metamaterial_sheet', 2)],
     [('superfluid_helium', 1000)], [('antimatter_injector', 1)], [], 65536, 1200, 'antimatter_propulsion', AM, [('Isp', 900, 1100)], 'propulsion_test_stand')
mrec('propulsion_testing', 'r1120_antimatter_transfer_stage', [('rocket_stage', 1), ('antimatter_injector', 1), ('antihydrogen_crystal', 4), ('pressure_hull_plate', 8)],
     [('liquid_hydrogen', 8000)], [('antimatter_transfer_stage', 1)], [], 65536, 1600, 'antimatter_propulsion', AM, [('Isp', 900, 1100)], 'propulsion_test_stand')
verify_recipes()

# ====================================================================== recipes: what the samples are good for
def tag_file(path, values):
    dump(path, {'replace': False, 'values': values})


SAMPLE_TAG = {'lunar_regolith': 'ad_astra:moon_sand', 'martian_regolith': 'ad_astra:mars_sand', 'venusian_aerosol': 'ad_astra:venus_sand',
              'mercurian_regolith': 'ad_astra:mercury_cobblestone'}
for sid, aa in SAMPLE_TAG.items():
    tag_file(os.path.join(D, 'tags/item/samples', sid + '.json'), [ts(sid), {'id': aa, 'required': False}])
tag_file(os.path.join(D, 'tags/item/samples/asteroid_regolith.json'), [ts('asteroid_regolith')])


def trec(rtype, rid, tins, ins, fins, outs, fouts, energy, dur, research_id, extra=None):
    """A machine recipe whose first inputs are item tags (the samples)."""
    r = {'type': ts(rtype), 'item_inputs': [{'tag': ts('samples/' + t), 'count': n} for t, n in tins] + [{'item': ts(i), 'count': n} for i, n in ins]}
    if fins:
        r['fluid_inputs'] = [{'fluid': ts(f) if f != 'water' else 'minecraft:water', 'amount': a} for f, a in fins]
    if outs:
        r['item_outputs'] = [{'id': ts(i), 'count': n} for i, n in outs]
    if fouts:
        r['fluid_outputs'] = [{'id': ts(f), 'amount': a} for f, a in fouts]
    r.update({'energy': energy, 'duration': dur, 'research': ts(research_id), 'unscaled': True})
    if extra:
        r.update(extra)
    dump(os.path.join(D, 'recipe', rtype, rid + '.json'), r)


# molten regolith electrolysis: nearly half the Moon's soil is oxygen (a real plan for lunar oxygen)
trec('electrolysis', 'r1120_lunar_oxygen', [('lunar_regolith', 4)], [], [], [], [('oxygen', 2000)], 1024, 400, 'sample_return')
# Mars: the perchlorate broken for oxygen, the iron oxide left as rust
trec('electrolysis', 'r1120_martian_oxygen', [('martian_regolith', 4)], [], [], [('rust_flakes', 12)], [('oxygen', 600)], 1024, 400, 'sample_return')
# Venus's clouds, boiled apart: sulfuric acid and sulfur dioxide
trec('air_separation', 'r1120_venusian_acid', [('venusian_aerosol', 2)], [], [], [], [('sulfuric_acid', 1500), ('sulfur_dioxide', 500)], 512, 300, 'sample_return')
# Mercury's dark crust: graphite
trec('sintering', 'r1120_mercurian_graphite', [('mercurian_regolith', 1)], [], [], [('graphite_moderator', 6)], [], 512, 300, 'sample_return')
# M-type asteroid metal: iron-nickel, as the meteorites that fall from such asteroids
trec('plasma_smelting', 'r1120_asteroid_metal', [('asteroid_regolith', 2)], [], [], [('meteoric_iron', 3)], [],
     4096, 600, 'sample_return', {'physics': json.load(open(os.path.join(D, 'recipe/plasma_smelting/chronosteel.json'), encoding='utf-8'))['physics']})

# ====================================================================== recipes: crafting
def shaped(rid, pattern, key, result, count=1):
    dump(os.path.join(D, 'recipe/crafting', rid + '.json'),
         {'type': 'minecraft:crafting_shaped', 'pattern': pattern, 'key': {k: ({'item': v} if ':' in v else {'item': ts(v)}) for k, v in key.items()},
          'result': {'id': ts(result), 'count': count}})


for pid, n, outer, inner in [('test_stand_frame', 16, 'pressure_hull_plate', 'minecraft:iron_block'),
                             ('flame_trench_block', 8, 'corundum', 'minecraft:water_bucket'),
                             ('propellant_tank_block', 4, 'pressure_hull_plate', 'silica_aerogel')]:
    dump(os.path.join(D, 'recipe/crafting', 'r1120_%s.json' % pid), extreme(pid, n, P_PART, {'O': outer, 'I': inner}))
shaped('r1120_sample_return_probe', ['HSH', 'GPG', 'HCH'], {'H': 'pressure_hull_plate', 'S': 'survey_probe', 'G': 'minecraft:glass_bottle',
                                                            'P': 'precision_gyroscope', 'C': 'corundum'}, 'sample_return_probe')
shaped('r1120_return_beacon', [' L ', 'RCR', 'I I'], {'L': 'fused_silica_lens', 'R': 'minecraft:redstone', 'C': 'minecraft:compass',
                                                      'I': 'minecraft:iron_ingot'}, 'return_beacon')
shaped('r1120_mission_bay', ['HPH', 'GCG', 'HHH'], {'H': 'station_hull', 'P': 'precision_gyroscope', 'G': 'minecraft:hopper',
                                                    'C': 'orbital_transfer_console'}, 'mission_bay')
shaped('r1120_polyethylene_shielding', ['PPP', 'PPP', 'PPP'], {'P': 'polymer_sheet'}, 'polyethylene_shielding', 2)
shaped('r1120_water_shield_tank', ['IGI', 'GWG', 'IGI'], {'I': 'minecraft:iron_ingot', 'G': 'minecraft:glass', 'W': 'minecraft:water_bucket'},
       'water_shield_tank', 4)

# ====================================================================== the suit line (changes to 1.0 recipes, see the changelog)
PIECES = ['helmet', 'chestplate', 'leggings', 'boots']
SUITS_CHANGED = []
for piece in PIECES:
    # Whipple Shield: the bumper over a Refractory layer (the piece now sits in the middle of its recipe)
    for old, new_inner in (('whipple_shield', 'refractory_suit'), ('frontier_suit', 'whipple_shield')):
        p = None
        for f in os.listdir(os.path.join(D, 'recipe/crafting')):
            if f.endswith('_%s_%s.json' % (old, piece)) or f == '%s_%s.json' % (old, piece):
                p = os.path.join(D, 'recipe/crafting', f)
        if p is None:
            raise SystemExit('no recipe for %s %s' % (old, piece))
        r = json.load(open(p, encoding='utf-8'))
        if 'A' in r['key']:
            continue                                   # done before
        pat = r['pattern']
        # the centre of the "M" block becomes the earlier piece
        cells = [(y, x) for y, row in enumerate(pat) for x, ch in enumerate(row) if ch == 'M']
        if not cells:
            raise SystemExit('no M in %s' % p)
        cy = sorted(set(y for y, x in cells))[len(set(y for y, x in cells)) // 2]
        cx = sorted(set(x for y, x in cells))[len(set(x for y, x in cells)) // 2]
        if pat[cy][cx] != 'M':
            cy, cx = cells[len(cells) // 2]
        pat[cy] = pat[cy][:cx] + 'A' + pat[cy][cx + 1:]
        r['key']['A'] = {'item': ts('%s_%s' % (new_inner, piece))}
        dump(p, r)
        SUITS_CHANGED.append((os.path.basename(p)[:-5], '%s %s' % (new_inner, piece)))
    # the Spacefarer Suit: a Frontier piece and a Heated Pressure piece, in polyethylene and metamaterial
    shaped_xl = {'type': ts('extreme_crafting') if False else 'theoreticalsciences:extended_crafting', }
    fr = json.load(open(next(os.path.join(D, 'recipe/crafting', f) for f in os.listdir(os.path.join(D, 'recipe/crafting'))
                             if f.endswith('_frontier_suit_%s.json' % piece)), encoding='utf-8'))
    r = json.loads(json.dumps(fr))
    pat = r['pattern']
    pat = [row.replace('B', 'P') for row in pat]
    # Frontier piece already sits at 'A'; put the Heated piece beside it, and the rest of the M block becomes metamaterial
    done = False
    out = []
    for row in pat:
        if not done and 'AM' in row:
            row = row.replace('AM', 'AH', 1)
            done = True
        elif not done and 'MA' in row:
            row = row.replace('MA', 'HA', 1)
            done = True
        out.append(row)
    if not done:
        out = [row.replace('M', 'H', 1) if 'M' in row and not done else row for row in out]
    r['pattern'] = out
    r['key'] = {'A': {'item': ts('frontier_suit_%s' % piece)}, 'H': {'item': ts('heated_pressure_suit_%s' % piece)},
                'M': {'item': ts('metamaterial_sheet')}, 'P': {'item': ts('polyethylene_shielding')}}
    r['result'] = {'id': ts('spacefarer_suit_%s' % piece), 'count': 1}
    dump(os.path.join(D, 'recipe/crafting', 'r1120_spacefarer_suit_%s.json' % piece), r)
NAMES = {'helmet': 'Helmet', 'chestplate': 'Chestplate', 'leggings': 'Leggings', 'boots': 'Boots'}
for piece in PIECES:
    L['item.%s.spacefarer_suit_%s' % (NS, piece)] = 'Spacefarer Suit ' + NAMES[piece]
L['tooltip.%s.spacefarer_suit' % NS] = ("Every suit in one: sealed against vacuum and the deep sea, warm on Titan, cool in the Interior's "
                                         "fire, a Whipple bumper against micrometeorites, the Frontier's visor, shroud, brace and "
                                         "compensators, and a polyethylene lining that stops a fifth of a solar storm's protons per "
                                         "piece. Counts as each of the other suits wherever one is needed.")
L['item.%s.spacefarer_suit_chestplate.lore' % NS] = L['tooltip.%s.spacefarer_suit' % NS]

# ====================================================================== missions and destinations (data)
MISSIONS = {
    'near_earth_asteroid': dict(name='Near-Earth Asteroid', delta_v=4500, synodic_days=13, penalty=0.7, transfer_days=300, order=10,
                                samples=[('asteroid_regolith', 8), ('meteoric_iron', 2)]),
    'moon': dict(name='The Moon', delta_v=8700, synodic_days=0, penalty=0, transfer_days=4, order=20, mod_absent='ad_astra',
                 samples=[('lunar_regolith', 8)]),
    'mars': dict(name='Mars', delta_v=10500, synodic_days=26, penalty=0.9, transfer_days=259, order=30, mod_absent='ad_astra',
                 samples=[('martian_regolith', 8)]),
    'venus': dict(name='The Clouds of Venus', delta_v=7500, synodic_days=19.5, penalty=0.9, transfer_days=146, order=40, mod_absent='ad_astra',
                  samples=[('venusian_aerosol', 6)]),
    'mercury': dict(name='Mercury', delta_v=23000, synodic_days=3.9, penalty=0.6, transfer_days=105, order=50, mod_absent='ad_astra',
                    samples=[('mercurian_regolith', 8)]),
    'titan': dict(name='Titan', delta_v=16000, synodic_days=12.6, penalty=0.8, transfer_days=2200, order=60, research='titan',
                  samples=[('tholin', 8), ('organic_evaporite', 2)]),
}
for mid, m in MISSIONS.items():
    o = {'name': 'mission.%s.%s' % (NS, mid), 'delta_v': m['delta_v'], 'synodic_days': m['synodic_days'], 'penalty': m['penalty'],
         'transfer_days': m['transfer_days'], 'order': m['order'], 'research': ts(m.get('research', 'sample_return')),
         'samples': [{'item': ts(i), 'count': n} for i, n in m['samples']]}
    if 'mod_absent' in m:
        o['mod_absent'] = m['mod_absent']
    dump(os.path.join(D, 'ts_missions', mid + '.json'), o)
    L['mission.%s.%s' % (NS, mid)] = m['name']
# delta-v from the station's orbit to the destination's surface (aerobraking where there is air), real values; synodic periods
# are the real ones, a month to a Minecraft day; penalty is how much dearer the trip is at worst, halfway between windows
DEST = {'overworld': (0, 0, 0), 'titan': (8000, 12.6, 0.8), 'accretion_frontier': (50000, 0, 0), 'ad_astra_moon': (5900, 0, 0),
        'ad_astra_mars': (4300, 26, 0.9), 'ad_astra_venus': (4500, 19.5, 0.9), 'ad_astra_mercury': (13000, 3.9, 0.6), 'ad_astra_glacio': (30000, 0, 0)}
for did, (dv, syn, pen) in DEST.items():
    def f(o, dv=dv, syn=syn, pen=pen):
        o['delta_v'] = dv
        o['synodic_days'] = syn
        o['penalty'] = pen
    T.edit_json(os.path.join(D, 'ts_destinations', did + '.json'), f)

# ====================================================================== tags
CAS = ['test_stand_frame', 'flame_trench_block', 'propellant_tank_block']
BLK = ['polyethylene_shielding', 'water_shield_tank', 'return_beacon', 'mission_bay']
tag_add(os.path.join(MC, 'tags/block/mineable/pickaxe.json'), [ts(x) for x in CAS + BLK + ['propulsion_test_stand']])
tag_add(os.path.join(MC, 'tags/block/needs_iron_tool.json'), [ts(x) for x in CAS + ['propulsion_test_stand']])
for kind in ('block', 'item'):
    tag_add(os.path.join(D, 'tags', kind, 'casings.json'), [ts(x) for x in CAS])
    tag_add(os.path.join(D, 'tags', kind, 'machines.json'), [ts('propulsion_test_stand')])
    tag_add(os.path.join(D, 'tags', kind, 'multiblock_controllers.json'), [ts('propulsion_test_stand')])
tag_add(os.path.join(D, 'tags/block/radiation_shielding.json'), [ts('polyethylene_shielding'), ts('water_shield_tank')])

# ====================================================================== the multiblock: a test tower over a flame trench
W, H, Dp = 11, 22, 15
g = grid(W, H, Dp)
c = W // 2
for z in range(Dp):
    for x in range(W):
        g[0][z][x] = 'M'                                                    # the pad
for z in range(3, 12):                                                      # the trench, open down the middle
    for x in range(c - 2, c + 3):
        g[0][z][x] = ' ' if abs(x - c) <= 1 else 'M'
for z in range(9, 14):                                                      # the deflector curving up at the far end
    for x in range(c - 2, c + 3):
        for y in range(1, 1 + (z - 8)):
            if y <= 4:
                g[y][z][x] = 'M'
for y in range(1, H):                                                       # the tower: four legs over the trench
    for (x, z) in ((c - 3, 2), (c + 3, 2), (c - 3, 8), (c + 3, 8)):
        g[y][z][x] = 'F'
    if y % 4 == 0:                                                          # bracing rings
        for x in range(c - 3, c + 4):
            g[y][2][x] = 'F'
            g[y][8][x] = 'F'
        for z in range(2, 9):
            g[y][z][c - 3] = 'F'
            g[y][z][c + 3] = 'F'
for x in range(c - 3, c + 4):                                               # the thrust frame the engine hangs from
    for z in range(2, 9):
        g[12][z][x] = 'F'
for y in range(6, 12):                                                      # the engine, nozzle down into the trench
    g[y][5][c] = 'E'
for y in range(13, 20):                                                     # the tanks above the thrust frame
    for (x, z) in ((c - 1, 4), (c + 1, 4), (c - 1, 6), (c + 1, 6)):
        g[y][z][x] = 'T'
g[1][0][c - 2] = 'P'
g[1][0][c + 2] = 'P'
g[1][0][c] = 'C'
T.MB_COUNTS['propulsion_test_stand'] = write_mb('propulsion_test_stand', rows(g),
                                                {'M': ['flame_trench_block'], 'F': ['test_stand_frame'], 'T': ['propellant_tank_block'],
                                                 'E': ['rocket_engine'], 'P': ['test_stand_frame'] + PORT},
                                                extra={'effect_center': [c, 3, 5], 'effect_scale': 3.0,
                                                       'fx': [{'type': 'glow', 'chars': 'E', 'color': '#FFB050', 'hot': '#FFFFFF'},
                                                              {'type': 'beam', 'start_back': 5, 'end_back': 12, 'up': 1, 'width': 0.5, 'color': '#FFE0A0'}]})
print('propulsion test stand blocks:', T.MB_COUNTS['propulsion_test_stand'])

# ====================================================================== text the Java reads
LANGX = {
    'screen.%s.planner': 'Mission Planner', 'screen.%s.mission_bay': 'Mission Bay',
    'screen.%s.planner.base': 'Δv at the window: %s km/s', 'screen.%s.planner.now': 'Δv now: %s km/s',
    'screen.%s.planner.stages': 'Your stages: %s km/s', 'screen.%s.planner.charted': 'Charted: no cost',
    'screen.%s.planner.window_open': 'Window open', 'screen.%s.planner.window_in': 'Next window in %s days',
    'screen.%s.planner.always': 'Always open', 'screen.%s.planner.home': 'Home: always free',
    'screen.%s.planner.go': 'Depart', 'screen.%s.planner.launch': 'Launch', 'screen.%s.planner.short': 'Not enough Δv',
    'screen.%s.planner.porkchop': 'Departure (days from now) against time of flight; green is cheap',
    'screen.%s.planner.tof': 'Time of flight: %s of the Hohmann time (mission %s min)',
    'screen.%s.planner.locked': 'Needs research', 'screen.%s.planner.busy': 'A mission is under way: back in %s min',
    'screen.%s.planner.no_probe': 'Load a Sample Return Probe',
    'message.%s.planner_paid': 'Burned %s for %s km/s of Δv.',
    'message.%s.planner_short': 'Not enough Δv: this trip costs %s km/s now, your stages give %s km/s.',
    'message.%s.beacon_charted': 'Route charted: the Mission Planner sends you here at no cost from now on.',
    'message.%s.beacon_nowhere': 'Nothing to chart here: this is not one of the Mission Planner\'s destinations.',
    'message.%s.bay_loaded': 'Loaded: %s.', 'message.%s.bay_grounded': 'A Mission Bay works only aboard the Orbital Station.',
    'message.%s.bay_launched': 'Launched to %s: back in %s minutes.', 'message.%s.bay_returned': 'A sample capsule from %s is back in the Mission Bay.',
    'message.%s.bay_collected': 'Took the samples.', 'message.%s.van_allen': 'Through the Van Allen belts: %s mSv on the way up.',
    'message.%s.storm_space': 'A solar particle storm: protons flood the station. Shelter behind water or polyethylene until it passes.',
    'message.%s.storm_space_ends': 'The proton storm has passed.',
    'tooltip.%s.stage_dv': 'Δv %s km/s (Isp %s s)',
}
for k, v in LANGX.items():
    L[k % NS] = v

# ====================================================================== the Codex
E = {}
E['fluids/rocket_equation'] = {'title': 'The Rocket Equation', 'icon': ts('hydrolox_transfer_stage'), 'category': 'fluids', 'research': 'rocket_equation', 'pages': [
    spot(['propulsion_test_stand', 'hydrolox_transfer_stage'], "Δv = ve·ln(m₀/m_f). The $(thing)Propulsion Test Stand$() is %d blocks: a steel tower over a flame trench, the engine nozzle down. It fires engines and fills $(item)Transfer Stages$()." % T.MB_COUNTS['propulsion_test_stand'], 'Propulsion Test Stand'),
    {'type': 'multiblock', 'name': 'Propulsion Test Stand', 'text': 'Interface Ports either side of the controller, at the foot of the pad.', 'multiblock': 'propulsion_test_stand'},
    text("$(thing)Stages$()$(br)Hydrolox: Isp 450 s, 6.1 km/s.$(br)Methalox: 360 s, 4.9 km/s.$(br)Nuclear thermal: 900 s, 9.7 km/s.$(br)Ion: 3,000 s, 20.4 km/s.$(br)Antimatter: 20,000 s, 79.6 km/s.$(br2)Stages add up: carry two hydrolox stages and you have 12.2 km/s. A trip takes the fewest that pay for it.")]}
E['fluids/transfer_orbits'] = {'title': 'Transfer Orbits', 'icon': ts('return_beacon'), 'category': 'fluids', 'research': 'transfer_orbits', 'pages': [
    text("v² = μ(2/r − 1/a). The $(thing)Transfer Console$() is a $(thing)Mission Planner$(). Each destination costs a Δv; inside its transfer window it costs the least, and between windows up to nearly twice as much. The windows come round every synodic period, a real month to a Minecraft day: Mars every 26 days, Venus every 19, Titan every 12 and a half, Mercury every 4. The Moon and the deep destinations are always open."),
    text("$(thing)Cost from the station$()$(br)The Moon 5.9 km/s; Mars 4.3 and Venus 4.5 (aerobraking in their air); Titan 8.0 (aerocapture); Mercury 13; Glacio 30; the Accretion Frontier 50. The Overworld is always free.$(br2)The Planner's chart is a $(thing)porkchop plot$(): when you leave against how long you take, green where it is cheap."),
    spot(['return_beacon'], "Your first trip to a destination is paid in Transfer Stages from your inventory. Set a $(item)Return Beacon$() down when you arrive, and the route is charted: from then on the Planner sends you there at no cost.", 'Return Beacon')]}
E['fluids/sample_return'] = {'title': 'Sample Return', 'icon': ts('sample_return_probe'), 'category': 'fluids', 'research': 'sample_return', 'pages': [
    spot(['mission_bay', 'sample_return_probe'], "t = π√((r₁ + r₂)³/8μ). A $(item)Mission Bay$() aboard the station flies a $(item)Sample Return Probe$() with stages enough for the round trip, in its window or not. It is gone for the transfer orbit there and back (a long one is about a Minecraft day), then its samples wait in the bay, for you or for a pipe.", 'Mission Bay'),
    text("$(thing)Targets$()$(br)Near-Earth asteroid: 4.5 km/s, Asteroid Regolith and Meteoric Iron.$(br)Titan: 16 km/s, Tholin and Organic Evaporite.$(br2)When Ad Astra is not installed (with it, you can go yourself): the Moon 8.7 km/s, Mars 10.5, the clouds of Venus 7.5, Mercury 23, for their regolith and aerosol. Recipes that take these samples take Ad Astra's Moon Sand, Mars Sand, Venus Sand and Mercury Cobblestone too."),
    spot(['asteroid_regolith', 'lunar_regolith', 'martian_regolith', 'venusian_aerosol', 'mercurian_regolith'], "Lunar regolith gives oxygen in the Electrolytic Cell (molten regolith electrolysis), Martian regolith oxygen and rust; Venus's aerosol gives sulfuric acid and sulfur dioxide in the Air Separation Unit; Mercury's dark crust gives graphite in the Sintering Press; asteroid metal gives Meteoric Iron in the Stellar Plasma Crucible.", 'Samples')]}
E['fluids/propulsion_tiers'] = {'title': 'Beyond Chemistry', 'icon': ts('nuclear_thermal_core'), 'category': 'fluids', 'research': 'nuclear_thermal_propulsion', 'pages': [
    spot(['nuclear_thermal_core', 'nuclear_transfer_stage'], "ve = √(2γ/(γ−1)·RT/M). Hydrogen alone, heated by a reactor: molar mass 2 against steam's 18, so twice the exhaust speed at a lower temperature. The $(item)Nuclear Thermal Core$() runs under 2,900 K.", 'Nuclear thermal'),
    spot(['ion_thruster_grid', 'ion_transfer_stage'], "v = √(2qV/m). Xenon ions thrown through charged grids at 30 km/s: the slow way that goes furthest. (Research: Electric Propulsion.)", 'Ion'),
    spot(['antimatter_injector', 'antimatter_transfer_stage'], "E = 2mc². Antiprotons annihilating in hydrogen, the pions turned by a magnetic nozzle: tens of thousands of seconds of specific impulse. (Research: Antimatter Propulsion.)", 'Antimatter')]}
E['fluids/space_radiation'] = {'title': 'Radiation in Space', 'icon': ts('polyethylene_shielding'), 'category': 'fluids', 'research': 'space_radiation', 'pages': [
    text("f = e^(−x/λ). When a solar storm breaks, the Orbital Station and the open Frontier fill with protons: about 5 Sv an hour in the open, enough for radiation sickness in a few minutes. Cosmic rays are few and almost nothing stops them; storm protons are many and a little hydrogen stops them."),
    spot(['polyethylene_shielding', 'water_shield_tank', 'spacefarer_suit_chestplate'], "Each block of $(item)Polyethylene Shielding$() or $(item)Water Shield Tank$() between you and open space (looked for in the six directions, up to eight blocks) stops most of a storm; water itself, stone and the station's hull help less. Each piece of the $(item)Spacefarer Suit$() stops a fifth. The Van Allen belts give a dose on every crew capsule ride up.", 'Shelter')]}
E['fluids/suit_line'] = {'title': 'The Suit Line', 'icon': ts('spacefarer_suit_helmet'), 'category': 'fluids', 'research': 'space_radiation', 'pages': [
    text("Each suit is built on the one before. The $(item)Whipple Shield$() is a bumper over a Refractory Suit piece; the $(item)Frontier Suit$() is built over a Whipple piece; the $(item)Heated Pressure Suit$() over a Pressure Suit piece, as before."),
    spot(['spacefarer_suit_helmet', 'spacefarer_suit_chestplate', 'spacefarer_suit_leggings', 'spacefarer_suit_boots'], "A Frontier piece and a Heated Pressure piece in polyethylene and metamaterial make the $(item)Spacefarer Suit$(): it counts as every other suit wherever one is needed, and its lining stops storm protons.", 'Spacefarer Suit')]}
OPS = {'propulsion_test_stand': ("Fire the engine as hot as its chamber stands, open the nozzle wide, and fill the stage as full as its tank holds, so the specific impulse and the Δv come out at the stage's rating.",
                                 'Chamber temperature, Expansion ratio, Propellant fraction, Grid voltage (ion only)', 'combustion instability: exhaust speed varies by ±1%·d.',
                                 'over the chamber limit the throat erodes (wear); over 93% propellant the tank fails (stops); over 4.6 kV the grids arc (wear).',
                                 'hotter burns, or higher grid voltage, faster.',
                                 ['η = 1 − ε^(−1.7(γ−1)/γ)', 've = √(2γ/(γ−1)·RT/M·η)', 've (ion) = 0.75·√(2qV/m)', 'Isp = ve/g₀', 'Δv = ve·ln(1/(1 − ζ))'])}
for mid, (intro, sets, dist, lim, rate, eqs) in OPS.items():
    E['operation/' + mid] = {'title': T.MACHINES[mid]['name'], 'icon': ts(mid), 'category': 'operation', 'research': 'rocket_equation',
                             'pages': op_pages(mid, intro, sets, dist, lim, rate, eqs)}
NODES = {'physics': [{'entry': 'fluids/rocket_equation', 'x': 45, 'y': 85, 'parents': ['fluids/orbital_mechanics']},
                     {'entry': 'fluids/transfer_orbits', 'x': 47, 'y': 85, 'parents': ['fluids/rocket_equation']},
                     {'entry': 'fluids/sample_return', 'x': 49, 'y': 85, 'parents': ['fluids/transfer_orbits']},
                     {'entry': 'fluids/propulsion_tiers', 'x': 45, 'y': 87, 'parents': ['fluids/rocket_equation']},
                     {'entry': 'fluids/space_radiation', 'x': 47, 'y': 87, 'parents': ['fluids/orbital_mechanics']},
                     {'entry': 'fluids/suit_line', 'x': 49, 'y': 87, 'parents': ['fluids/space_radiation']}],
         'machines': [{'entry': 'operation/propulsion_test_stand', 'x': 28, 'y': 22, 'parents': []}]}
codex(E, NODES)

finish()
print('suit recipes changed:', SUITS_CHANGED)
print('research', len(T.RESEARCH), 'machines', len(T.MACHINES), 'recipes', len(T.RECIPES))
