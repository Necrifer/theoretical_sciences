"""
1.15.0 (Patch D2): young worlds and deep sky. The rest of the space document's section 6 (concepts 8, 9 and 11 to 14),
its two biomes, the optional Ad Astra recipes, and the story tie.

  8. Icy moons and tidal heating: a new single-block console, the Ice Rheometer (a Maxwell solid's dissipation and the
     tidal heating it gives a moon, Ė = (21/2)·(k₂/Q)·G·M²·R⁵·n·e²/a⁶); a new biome, the Plume Field, in the Titan Ocean,
     whose Plume Ice gives Plume Ice Grains.
  9. How planets form: a new biome, the Protoplanetary Disk, in the Accretion Frontier, whose Chondritic Regolith gives
     Chondrules and Calcium-Aluminium-rich Inclusions; the Dating Spectrometer dates the inclusions (the oldest solids).
 11. Habitability: the Reflecting Telescope gains transit spectroscopy (photon-limited precision against the depth of an
     atmosphere's features); Transmission Spectrum Plates, Biosignature Records.
 12. The oldest light: a new multiblock, the CMB Bolometer Array (Planck's law against the galaxy's foregrounds, and
     the noise of the detectors); the Last Scattering Map, which feeds the Inflation Chamber.
 13. Radio flashes and torn stars: fast radio bursts in the Radio Telescope (dispersion), which weigh the cosmic web's
     gas; tidal disruption events in the Burst Observatory.
 14. Listening for others: a narrowband candidate at the water hole, which the Omnipotence Core recognises: the signal
     came from Home.
 Ad Astra (only when installed): its desh, ostrum and calorite plates and its engines stand in for some of the mod's
     parts.

Run:  python3 tools/round1150.py
"""
import glob, json, math, os, sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from tsgen import *          # noqa: F401,F403
import tsgen as T

L = T.LANG
T.WORLD['reflecting_telescope'] = {'night': 1, 'sky': 1, 'moon': 0, 'rain': 0, 'alt': 100}
T.WORLD['radio_telescope'] = {'sky': 1, 'rfi': 0, 'rain': 0}
T.WORLD['cmb_bolometer_array'] = {'T': 4.0, 'alt': 200}
# tuned exactly to the hydrogen line, the narrowest band, a night's listening
T.HINTS['r1150_narrowband_candidate'] = [{'nu': 1420.4, 'B': 0.001, 'tau': 36000}]


def edit_model(mid, f):
    p = os.path.join(D, 'ts_physics', mid + '.json')
    m = json.load(open(p, encoding='utf-8'))
    if f(m) is not False:
        dump(p, m)


def has(m, kind, ident):
    return any(x['id'] == ident for x in m[kind])


# research positions: the asked-for spot, or the nearest free one two steps from any other node
TAKEN = [(json.load(open(f))['x'], json.load(open(f))['y']) for f in glob.glob(os.path.join(D, 'ts_research', '*.json'))]


def spot_at(x, y, rid):
    p = os.path.join(D, 'ts_research', rid + '.json')
    if os.path.exists(p):
        r = json.load(open(p))
        return r['x'], r['y']
    for r in range(0, 12):
        for dx in range(-r, r + 1):
            for dy in range(-r, r + 1):
                if max(abs(dx), abs(dy)) != r:
                    continue
                q = (x + dx, y + dy)
                if all(max(abs(q[0] - a), abs(q[1] - b)) >= 1 and q != (a, b) for a, b in TAKEN) and \
                        all(not (abs(q[0] - a) < 2 and q[1] == b) for a, b in TAKEN):
                    TAKEN.append(q)
                    return q
    raise SystemExit('no room for ' + rid)


# ====================================================================== research
def R(rid, title, x, y, *a):
    x, y = spot_at(x, y, rid)
    research(rid, title, x, y, *a)


R('tidal_heating', 'Icy Moons', 22, 12, 320, ['comets', 'thermodynamics_of_ice'] if os.path.exists(os.path.join(D, 'ts_research/thermodynamics_of_ice.json')) else ['comets'],
  [('cometary_ice', 2), ('minecraft:blue_ice', 2)], 340,
  "Far from the Sun there should be nothing but ice, yet Europa and Enceladus hide oceans. Their planets keep flexing them: "
  "an eccentric orbit brings a moon nearer and farther each turn, so its tidal bulge rises and falls, and the ice rubs "
  "itself warm. How warm depends on how the ice yields, which a lab measures by squeezing it in cycles. Cassini flew "
  "through Enceladus's plumes and found grains of silica, which form only where hot water meets rock. Unlocks the Ice "
  "Rheometer; the Plume Field lies in the Titan Ocean.",
  'Warmed by squeezing',
  {'goal': 'A moon of radius R at distance d from a planet of mass M feels a stronger pull on its near side than its far side. For R much '
           'less than d, write the difference in the planet\'s acceleration across the moon, with G the gravitational constant.',
   'name': 'Tidal acceleration', 'lhs': 'Δa', 'form': '2 * G * M * R / d ^ 3', 'expr': '2 * G * M * R / d ^ 3',
   'ask': 'Find Δa in m/s² for Saturn (M = 5.68e26 kg) acting across Enceladus (R = 2.52e5 m) at d = @{d} m, G = 6.674e-11.',
   'vars': {'Δa': 'tidal acceleration', 'G': 'gravitational constant', 'M': 'planet mass', 'R': 'moon radius', 'd': 'distance'},
   'given': {'G': 6.674e-11, 'M': 5.68e26, 'R': 2.52e5, 'd': 2.38e8}, 'unit': 'm/s²',
   'method': 'GM/(d − R)² − GM/(d + R)² ≈ GM·4R/d³ across the diameter, 2GMR/d³ from centre to surface: the derivative of 1/d² adds a power.',
   'distractors': ['^ 2', '+', 'sqrt'], 'vary': {'d': [1.5e8, 6.0e8, 1e6]}},
  'ice_rheometer', 'comets')
R('planet_formation', 'How Planets Form', 24, 12, 340, ['asteroid_taxonomy', 'radiometric_dating'],
  [('chondrite_fragment', 4), ('interstellar_dust', 4)], 360,
  "Planets grow in the disk of gas and dust about a young star. Dust sticks into pebbles, pebbles into planetesimals, "
  "and where the disk is heavy enough it can break straight into clumps: whether it does is told by Toomre's Q, its "
  "pressure and spin against its own gravity. The first solids still survive in meteorites: chondrules, droplets melted "
  "in flashes, and calcium-aluminium-rich inclusions, which lead-lead dating puts at 4.567 billion years, the age of the "
  "solar system. The Protoplanetary Disk lies in the Accretion Frontier.",
  'Dust into worlds',
  {'goal': 'A disk of surface density Σ, sound speed cs and epicyclic frequency κ resists collapsing into clumps by pressure and by '
           'spin. Write Toomre\'s Q: below 1 it breaks up.',
   'name': 'Toomre parameter', 'lhs': 'Q', 'form': 'cs * κ / ( π * G * Σ )', 'expr': 'cs * k / ( π * G * S )',
   'ask': 'Find Q for cs = @{cs} m/s, κ = 2e-7 /s, Σ = 1700 kg/m² and G = 6.674e-11.',
   'vars': {'Q': 'Toomre parameter', 'cs': 'sound speed', 'κ': 'epicyclic frequency', 'G': 'gravitational constant', 'Σ': 'surface density'},
   'given': {'cs': 1000.0, 'k': 2e-7, 'G': 6.674e-11, 'S': 1700.0}, 'unit': '',
   'method': 'A ring of size λ collapses if πGΣ beats both the pressure (cs²/λ) and the spin (κ²λ); the worst case gives Q = csκ/πGΣ.',
   'distractors': ['*', '+', 'sqrt'], 'vary': {'cs': [200.0, 2000.0, 10.0]}},
  'calcium_aluminium_inclusion', 'asteroid_taxonomy')
R('habitability', 'Habitability and Signs of Life', 26, 12, 360, ['tidal_heating', 'exoplanetology'] if os.path.exists(os.path.join(D, 'ts_research/exoplanetology.json')) else ['tidal_heating'],
  [('transit_light_curve', 1), ('spectral_plate', 2)] if 'item.%s.transit_light_curve' % NS in L else [('spectral_plate', 4)], 380,
  "A planet's warmth comes first from its star: how bright, how near, how much light the planet reflects. Its air can be "
  "read when it passes in front of the star: starlight through the thin ring of atmosphere picks up the gases' lines, a "
  "change of a few parts in a million. Oxygen and methane together would be a sign of life, since each destroys the "
  "other and something must keep making both. Adds transit spectroscopy to the Reflecting Telescope.",
  'Air against starlight',
  {'goal': 'A planet at distance a from a star of temperature T★ and radius R★ reflects a fraction A of the light. Balancing what it '
           'absorbs against what it radiates, write its equilibrium temperature T.',
   'name': 'Equilibrium temperature', 'lhs': 'T', 'form': 'Ts * ( 1 - A ) ^ ( 1 / 4 ) * sqrt ( Rs / ( 2 * a ) )',
   'expr': 'Ts * ( 1 - A ) ^ ( 1 / 4 ) * sqrt ( Rs / ( 2 * a ) )',
   'ask': 'Find T in kelvin for the Sun (Ts = 5772 K, Rs = 6.96e8 m), a = @{a} m and A = 0.3.',
   'vars': {'T': 'equilibrium temperature', 'Ts': 'star temperature', 'A': 'albedo', 'Rs': 'star radius', 'a': 'distance'},
   'given': {'Ts': 5772.0, 'A': 0.3, 'Rs': 6.96e8, 'a': 1.496e11}, 'unit': 'K',
   'method': 'Absorbed: σTs⁴·(Rs/a)²·πR²·(1 − A). Radiated: 4πR²·σT⁴. Solve for T.',
   'distractors': ['*', '+', '^ 2'], 'vary': {'a': [5e10, 4e11, 1e9]}},
  'biosignature_record', 'tidal_heating')
R('cosmic_microwave_background', 'The Oldest Light', 28, 12, 400, ['nebular_gases', 'superconductivity'] if os.path.exists(os.path.join(D, 'ts_research/superconductivity.json')) else ['nebular_gases'],
  [('superconducting_coil', 2), ('helium_3_cell', 1)], 420,
  "380,000 years after the Big Bang the universe cooled enough for atoms to form, and the light that had been trapped in "
  "the hot plasma went free. It is still arriving from every direction, stretched by the universe's expansion from "
  "3,000 kelvin to 2.725, so it is now microwaves, with the spectrum of a perfect black body. Its temperature varies by "
  "a hundred-thousandth from place to place: the seeds of every galaxy. Unlocks the CMB Bolometer Array.",
  'Stretched into microwaves',
  {'goal': 'Light released by a gas at temperature T₀ travels while the universe stretches by a factor 1 + z. Wavelengths stretch with '
           'it, so a black body stays a black body, cooler. Write the temperature T we see.',
   'name': 'Cooling by expansion', 'lhs': 'T', 'form': 'T0 / ( 1 + z )', 'expr': 'T0 / ( 1 + z )',
   'ask': 'Find T in kelvin for T₀ = 3000 K and z = @{z}.',
   'vars': {'T': 'temperature now', 'T0': 'temperature then', 'z': 'redshift'},
   'given': {'T0': 3000.0, 'z': 1100.0}, 'unit': 'K',
   'method': "Every wavelength grows by 1 + z; Wien's law makes the peak, and so the temperature, fall by the same factor.",
   'distractors': ['*', '-', 'sqrt'], 'vary': {'z': [500.0, 1500.0, 10.0]}},
  'last_scattering_map', 'nebular_gases')
R('fast_transients', 'Radio Flashes and Torn Stars', 30, 10, 380, ['cosmic_microwave_background'],
  [('pulsar_timing_record', 1), ('burst_afterglow_crystal', 1)], 400,
  "A fast radio burst lasts a thousandth of a second and comes from another galaxy. On its way it crosses free electrons, "
  "which slow the low frequencies more than the high, so it arrives as a sweep: how long a sweep says how much gas it "
  "crossed, and so the bursts have weighed the gas between galaxies that nothing else could see. Some come from magnetars. "
  "A star that strays too near a supermassive black hole is torn into a stream and half of it falls in, flaring for "
  "months: a tidal disruption event. Adds both to the Radio Telescope and the Burst Observatory.",
  'Swept by electrons',
  {'goal': 'A pulse crossing a dispersion measure DM (in pc/cm³) arrives later at lower frequency. Write the delay Δt in ms between '
           'frequencies f₁ < f₂ (in GHz), with the constant 4.15 ms.',
   'name': 'Dispersion delay', 'lhs': 'Δt', 'form': '4.15 * DM * ( 1 / f1 ^ 2 - 1 / f2 ^ 2 )', 'expr': '4.15 * DM * ( 1 / f1 ^ 2 - 1 / f2 ^ 2 )',
   'ask': 'Find Δt in ms for DM = @{DM}, f₁ = 1.2 GHz and f₂ = 1.5 GHz.',
   'vars': {'Δt': 'delay', 'DM': 'dispersion measure', 'f1': 'lower frequency', 'f2': 'upper frequency'},
   'given': {'DM': 500.0, 'f1': 1.2, 'f2': 1.5}, 'unit': 'ms',
   'method': 'In a plasma the group speed falls short of c by an amount growing as the plasma frequency squared over f²: integrate over the path.',
   'distractors': ['*', '+', 'sqrt'], 'vary': {'DM': [50.0, 2000.0, 10.0]}},
  'dispersion_record', 'cosmic_microwave_background')
R('search_for_others', 'Listening for Others', 30, 12, 480, ['fast_transients', 'habitability'],
  [('hydrogen_line_map', 2), ('biosignature_record', 1)], 480,
  "Where would anyone call from, and at what frequency? Between the hydrogen line at 1420 MHz and the hydroxyl lines at "
  "1660, the galaxy is quiet: the water hole, where water's two halves both sing. Nature makes nothing narrower than "
  "about a kilohertz there; a carrier one hertz wide would be made. How many might be out there is Drake's equation, a "
  "product of guesses, most of them unknown by factors of a thousand. The Omnipotence Core, though, may know a signal "
  "when it hears one.",
  'A narrow line at the water hole',
  {'goal': 'Stars form at a rate R★; a fraction fp have planets; ne per system could support life; fractions fl, fi and fc of those '
           'develop life, intelligence and transmitters; each transmits for L years. Write the number N transmitting now.',
   'name': 'Drake equation', 'lhs': 'N', 'form': 'Rs * fp * ne * fl * fi * fc * L', 'expr': 'Rs * fp * ne * fl * fi * fc * L',
   'ask': 'Find N for R★ = 1.5 /year, fp = 0.9, ne = 0.4, fl = 0.1, fi = 0.01, fc = 0.1 and L = @{L} years.',
   'vars': {'N': 'civilisations transmitting', 'Rs': 'star formation rate', 'fp': 'with planets', 'ne': 'habitable per system',
            'fl': 'with life', 'fi': 'with intelligence', 'fc': 'transmitting', 'L': 'lifetime'},
   'given': {'Rs': 1.5, 'fp': 0.9, 'ne': 0.4, 'fl': 0.1, 'fi': 0.01, 'fc': 0.1, 'L': 10000.0}, 'unit': '',
   'method': 'In a steady state, the number present is the rate they appear times how long each lasts.',
   'distractors': ['+', '/', '^ 2'], 'vary': {'L': [100.0, 1e6, 100.0]}},
  'narrowband_candidate', 'fast_transients')

# ====================================================================== 8: the Ice Rheometer (a console)
IR = model([par('P', 'h', 1, 100, 0.1, 33), par('e', '', 0, 0.1, 0.0005, 0.005), par('Ti', 'K', 150, 272, 1, 250)],
           {'M': 5.68e26, 'R': 2.52e5, 'k2': 0.02},
           [der('a', 'km', '(6.674e-11*M*(P*3600)^2/(4*3.14159^2))^(1/3)/1000', 'the orbit of that period, by Kepler'),
            der('wt', '', '2*3.14159/(P*3600)*1e14*exp(7217*(1/Ti - 1/270))/3.5e9', 'ωτ: forcing against the ice\'s Maxwell time η/μ'),
            der('qi', '', '2*3.14159/(P*3600)*1e14*exp(7217*(1/Ti - 1/270))/3.5e9/(1 + (2*3.14159/(P*3600)*1e14*exp(7217*(1/Ti - 1/270))/3.5e9)^2)*(1 + 0.01*d)',
                '1/Q = ωτ/(1 + ω²τ²): most at ωτ = 1'),
            der('Ed', 'GW', '10.5*k2*(2*3.14159/(P*3600)*1e14*exp(7217*(1/Ti - 1/270))/3.5e9/(1 + (2*3.14159/(P*3600)*1e14*exp(7217*(1/Ti - 1/270))/3.5e9)^2))*(1 + 0.01*d)'
                           '*6.674e-11*M^2*R^5*(2*3.14159/(P*3600))*e^2/((6.674e-11*M*(P*3600)^2/(4*3.14159^2))^(1/3))^6/1e9',
                'Ė = (21/2)·(k₂/Q)·G·M²·R⁵·n·e²/a⁶')],
           [con('crack', 'wear', '0.05 - e', 'a strain under 0.05, or the sample cracks')],
           'clamp(0.3 + 40/P, 0.3, 1.5)', 'faster cycles finish sooner')
machine('ice_rheometer', 'Ice Rheometer', "A cold press that squeezes a cylinder of ice in cycles, as a moon's tides do, and "
        "measures the lag between push and give: the ice's dissipation. From it, and the moon's orbit, comes the heat the tides "
        "put into the ice shell, and whether an ocean can stay liquid beneath.", 'chain', 3, IR,
        {'p.P': 'Forcing period', 'p.P.desc': "The moon's orbital period: the tide rises and falls once each orbit.",
         'p.e': 'Eccentricity', 'p.e.desc': 'How far the orbit is from a circle: the tide\'s swing grows with it (and the strain of the test).',
         'p.Ti': 'Ice temperature', 'p.Ti.desc': 'Warm ice flows and dissipates; cold ice is stiff and stores the energy.',
         'd.a': 'Orbit radius', 'd.a.desc': 'Of an orbit of that period, by Kepler.', 'd.wt': 'ωτ', 'd.wt.desc': 'The forcing against the Maxwell time.',
         'd.qi': 'Dissipation 1/Q', 'd.qi.desc': 'The share of each cycle\'s energy turned to heat.', 'd.Ed': 'Tidal heating',
         'd.Ed.desc': 'What the tides put into the moon.', 'c.crack': 'The sample is cracking',
         'v.M': 'Planet mass', 'v.R': 'Moon radius', 'v.k2': 'Love number k₂',
         'disturb': 'Grain growth: the dissipation drifts by 1%·d.', 'rate.desc': 'Faster cycles finish sooner.'},
        extreme('ice_rheometer', 1, P_CONSOLE, {'A': 'minecraft:blue_ice', 'F': 'silica_aerogel', 'L': 'pressure_hull_plate', 'N': 'precision_gyroscope'}))

# ====================================================================== 12: the CMB Bolometer Array (a multiblock console)
CB = model([par('nu', 'GHz', 20, 900, 1, 150), par('n', '', 1, 1024, 1, 64), par('t', 'h', 1, 500, 1, 24)],
           {'Tc': 2.725},
           [der('x', '', '0.04799*nu/Tc', 'hν/kT'),
            der('Bc', 'W/m²/Hz/sr', '1.4745e-50*(nu*1e9)^3/(exp(0.04799*nu/Tc) - 1)', "Planck's law, B = (2hν³/c²)/(e^(hν/kT) − 1)"),
            der('fg', 'W/m²/Hz/sr', '2e-21*((nu/100)^(-0.7) + 0.3*(nu/100)^3.6)*(1 + 0.02*d)', 'the galaxy: synchrotron below, dust above'),
            der('cf', '', '1.4745e-50*(nu*1e9)^3/(exp(0.04799*nu/Tc) - 1)/(2e-21*((nu/100)^(-0.7) + 0.3*(nu/100)^3.6)*(1 + 0.02*d))', 'CMB over foreground'),
            der('dT', 'µK', '300/sqrt(n*t*3600)', 'map noise: a detector\'s 300 µK·√s over all the detectors and the time')],
           [con('cryo', 'stop', '10 - T', 'the focal plane under 10 K (it is cooled further with helium-3)'),
            con('load', 'wear', '200000 - n*nu', 'readout heat: detectors × frequency under 200,000')],
           'clamp(30/t, 0.2, 1.5)', 'a shorter integration finishes sooner')
machine('cmb_bolometer_array', 'CMB Bolometer Array', "The controller of a microwave telescope: an off-axis mirror on a "
        "turning mount over a cryostat, its focal plane a wafer of superconducting bolometers held a third of a degree above "
        "absolute zero by a helium-3 refrigerator. It maps the oldest light in the sky. A multiblock.", 'chain', 4, CB,
        {'p.nu': 'Observing frequency', 'p.nu.desc': 'Where to look: the CMB is brightest near 160 GHz; the galaxy\'s dust rises above, its synchrotron below.',
         'p.n': 'Detectors', 'p.n.desc': 'Bolometers on the focal plane: more map faster, but heat the readout.',
         'p.t': 'Integration', 'p.t.desc': 'Hours on the sky: longer is quieter, but slower.',
         'd.x': 'hν/kT', 'd.x.desc': 'Frequency against the CMB\'s temperature.', 'd.Bc': 'CMB brightness', 'd.Bc.desc': "By Planck's law.",
         'd.fg': 'Foregrounds', 'd.fg.desc': 'The galaxy\'s own glow.', 'd.cf': 'Contrast', 'd.cf.desc': 'CMB over foreground.',
         'd.dT': 'Map noise', 'd.dT.desc': 'In microkelvin.', 'c.cryo': 'The focal plane is too warm', 'c.load': 'The readout is overheating',
         'v.Tc': 'CMB temperature', 'disturb': 'Atmospheric drift: the foregrounds rise by 2%·d.', 'rate.desc': 'A shorter integration finishes sooner.'},
        extreme('cmb_bolometer_array', 1, P_CTRL, {'F': 'cryostat_shell', 'T': 'radio_dish_panel', 'M': 'superconducting_coil', 'B': 'bolometer_focal_plane'}),
        multiblock=True)
# the Cryostat Shell is the mod's own (1.0's cryogenics casing), reused here
casing('bolometer_focal_plane', 'Bolometer Focal Plane', "A silicon wafer of feedhorns, each over a superconducting "
       "transition-edge sensor that warms by a millionth of a degree when a microwave photon lands. Made by crafting.")

# ====================================================================== 11, 13, 14: the telescopes gain modes
def telescope(m):
    if has(m, 'derived', 'snrT'):
        return False
    m.setdefault('recipe_vars', {}).update({'mstar': 0.0, 'dep': 0.0})
    m['derived'].append(der('snrT', '', 'dep*sqrt(1e10*D^2*t*10^(-0.4*mstar))/1e6',
                            'transit spectroscopy: the feature\'s depth (ppm) over the photon noise 1/√N'))


edit_model('reflecting_telescope', telescope)
L['physics.%s.reflecting_telescope.d.snrT' % NS] = 'Transit S/N'
L['physics.%s.reflecting_telescope.d.snrT.desc' % NS] = "An atmosphere's spectral feature, in parts per million of the starlight, against the photon noise."
L['physics.%s.reflecting_telescope.v.mstar' % NS] = 'Star magnitude'
L['physics.%s.reflecting_telescope.v.dep' % NS] = 'Feature depth (ppm)'

# ====================================================================== items and blocks
item('plume_ice_grains', 'Plume Ice Grains', "Grains of ice frozen from the spray of a cryovolcanic plume, salty, with specks of "
     "silica a few nanometres across: the sign of hot water working on rock far below. Found in the Titan Ocean's Plume Field, "
     "from mining Plume Ice.")
item('ocean_world_record', 'Ocean World Record', "A model of a moon's ice shell and the ocean under it, its thickness set by the "
     "tidal heat flowing up through it. Made in the Ice Rheometer.")
item('chondrule', 'Chondrule', "A grey bead of glassy rock a millimetre across, melted in a flash in the young disk and frozen "
     "in minutes, before it was swept into an asteroid. Found in the Accretion Frontier's Protoplanetary Disk, from mining "
     "Chondritic Regolith.")
item('calcium_aluminium_inclusion', 'Calcium-Aluminium-rich Inclusion', "A white, irregular lump of the minerals that condense "
     "first from a cooling gas: the oldest solids in the solar system. Found in the Accretion Frontier's Protoplanetary Disk, "
     "rarely, from mining Chondritic Regolith.")
item('solar_system_age_record', 'Solar System Age Record', "Lead-lead ages of the oldest inclusions, 4,567.3 million years, to "
     "within a few hundred thousand: the moment the solar system's first solids formed. Made in the Dating Spectrometer.")
item('transmission_spectrum_plate', 'Transmission Spectrum Plate', "The spectrum of starlight that passed through a planet's "
     "air as it crossed its star, divided by the starlight alone: the gases' lines, a few parts in a million deep. Made in the "
     "Reflecting Telescope.")
item('biosignature_record', 'Biosignature Record', "An ocean world's model set beside its air's spectrum: oxygen with methane, "
     "out of balance, which something must keep making. Made in the Spectrograph.")
item('last_scattering_map', 'Last Scattering Map', "A map of the whole sky in microwaves, the CMB's warm and cool spots a "
     "hundred-thousandth apart: the universe at 380,000 years old, and the seeds of every galaxy. Made in the CMB Bolometer "
     "Array.")
item('dispersion_record', 'Dispersion Record', "A fast radio burst caught sweeping down the band, its delay fitted to a "
     "dispersion measure: the free electrons along a billion light years. Made in the Radio Telescope.")
item('disrupted_stellar_stream', 'Disrupted Stellar Stream', "The light curve and spectra of a star torn apart by a "
     "supermassive black hole: the flare, the months-long fade as the stream falls back, the hydrogen and helium of the "
     "star. Made in the Burst Observatory.")
item('narrowband_candidate', 'Narrowband Candidate', "A carrier one hertz wide at the water hole, drifting as a transmitter on "
     "a turning planet would. Nature makes nothing so narrow. Made in the Radio Telescope. Take it to the Omnipotence Core.")
item('signal_from_home', 'Signal from Home', "The narrowband candidate, as the Omnipotence Core read it: a count of primes, then "
     "a picture built from them, of a ring of light around a place the Core remembers. It came from Home. Read by the "
     "Omnipotence Core.")
simple_block('plume_ice', 'Plume Ice', "Ice frozen from the spray of the vents below, granular and faintly salty, with a "
             "bluish sheen where the silica grains catch the light. Found in the Titan Ocean's Plume Field. Gives Plume Ice "
             "Grains when mined.")
simple_block('chondritic_regolith', 'Chondritic Regolith', "Loose dust of the young disk, packed with chondrules like grey beads "
             "and, now and then, a white inclusion. Found in the Accretion Frontier's Protoplanetary Disk. Gives Chondrules, "
             "and rarely a Calcium-Aluminium-rich Inclusion, when mined.")

# ====================================================================== recipes
mrec('ice_rheometry', 'r1150_enceladus_model', [('plume_ice_grains', 4)], [('minecraft:water', 500)], [('ocean_world_record', 1)], [], 1024, 600,
     'tidal_heating', {'M': 5.68e26, 'R': 2.52e5, 'k2': 0.02}, [('Ed', 5, 50)], 'ice_rheometer')
mrec('ice_rheometry', 'r1150_europa_model', [('plume_ice_grains', 2), ('cometary_ice', 2)], [('minecraft:water', 500)], [('ocean_world_record', 2)], [], 2048, 600,
     'tidal_heating', {'M': 1.898e27, 'R': 1.561e6, 'k2': 0.25}, [('Ed', 100, 2000)], 'ice_rheometer')
mrec('radiometric_dating', 'r1150_cai_age', [('calcium_aluminium_inclusion', 2)], [], [('solar_system_age_record', 1)], [], 1024, 600,
     'planet_formation', {'tau': 6.446e9, 'age': 4.567e9}, [('sig', 0, 3e7)], 'dating_spectrometer')
mrec('astrophotography', 'r1150_transmission_spectrum', [('spectral_plate', 2), ('minecraft:paper', 2)], [], [('transmission_spectrum_plate', 1)], [],
     512, 800, 'habitability', {'mreq': 9.0, 'mstar': 9.0, 'dep': 150.0}, [('snrT', 10, 1e9)], 'reflecting_telescope')
mrec('cmb_mapping', 'r1150_last_scattering_map', [('doped_wafer', 2)], [('helium_3', 50)], [('last_scattering_map', 2)], [], 16384, 1200,
     'cosmic_microwave_background', {'Tc': 2.725}, [('cf', 400, 1e12), ('dT', 0, 0.05)], 'cmb_bolometer_array')
mrec('radio_observation', 'r1150_fast_radio_burst', [('minecraft:paper', 2)], [], [('dispersion_record', 1)], [], 1024, 600,
     'fast_transients', {'S': 1.0, 'al': -1.5, 'wl': 1e6, 'nl': 1400.0, 'DM': 500.0, 'Sreq': 10.0}, [('SNR', 10, 1e9), ('ts', 0, 1.0)], 'radio_telescope')
mrec('radio_observation', 'r1150_narrowband_candidate', [('biosignature_record', 1)], [], [('narrowband_candidate', 1)], [], 4096, 1200,
     'search_for_others', {'S': 10.0, 'al': 0.0, 'wl': 0.0001, 'nl': 1420.4, 'DM': 0.0, 'Sreq': 10.0}, [('SNR', 10, 1e9), ('inb', 0, 1e9)], 'radio_telescope')
mrec('burst_detection', 'r1150_tidal_disruption', [('photographic_plate', 2), ('spectral_plate', 1)], [], [('disrupted_stellar_stream', 1)], [], 2048, 800,
     'fast_transients', {}, [('Pdet', 0.97, 1.0), ('FA', 0, 0.3)], 'burst_observatory')
verify_recipes()


def copy_recipe(rtype, src, rid, ins, outs, research_id, fouts=None, physics=None):
    r = json.load(open(os.path.join(D, 'recipe', rtype, src + '.json'), encoding='utf-8'))
    o = {'type': r['type'], 'item_inputs': [{'item': ts(i) if ':' not in i else i, 'count': n} for i, n in ins]}
    if outs:
        o['item_outputs'] = [{'id': ts(i) if ':' not in i else i, 'count': n} for i, n in outs]
    if fouts:
        o['fluid_outputs'] = [{'id': ts(f), 'amount': a} for f, a in fouts]
    for k in ('energy', 'duration', 'unscaled'):
        if k in r:
            o[k] = r[k]
    o['research'] = ts(research_id)
    if physics is not None:
        o['physics'] = physics
    elif 'physics' in r:
        o['physics'] = r['physics']
    dump(os.path.join(D, 'recipe', rtype, rid + '.json'), o)


# the Spectrograph (no console): assays and the biosignature
copy_recipe('spectroscopy', 'r45_chondrite_assay', 'r1150_chondrule_assay', [('chondrule', 4), ('spectral_plate', 1)],
            [('meteoric_iron', 2), ('presolar_grains', 3)], 'planet_formation')
copy_recipe('spectroscopy', 'r45_chondrite_assay', 'r1150_biosignature', [('ocean_world_record', 1), ('transmission_spectrum_plate', 1)],
            [('biosignature_record', 1)], 'habitability')
copy_recipe('spectroscopy', 'r45_chondrite_assay', 'r1150_cosmic_web_from_bursts', [('dispersion_record', 2), ('spectral_plate', 1)],
            [('cosmic_web_filament', 2)], 'fast_transients')
# the Inflation Chamber: the CMB's spectrum of fluctuations fixes the inflaton's
infl = json.load(open(os.path.join(D, 'recipe/inflation/r32_inflaton_condensation.json'), encoding='utf-8'))
copy_recipe('inflation', 'r32_inflaton_condensation', 'r1150_inflaton_from_cmb', [('last_scattering_map', 1), ('dark_matter_lens', 1)],
            [('inflaton_condensate', 4)], 'cosmic_microwave_background')
# the Stellar Plasma Crucible: the torn star's stream is tidal plasma
copy_recipe('plasma_smelting', 'chronosteel', 'r1150_stream_plasma', [('disrupted_stellar_stream', 1)], [], 'fast_transients', fouts=[('tidal_plasma', 2000)])
# Chronoweaving: the solar system's first layer, in place of tree rings
sto = json.load(open(os.path.join(D, 'recipe/chronoweaving/r170_stratigraphic_timeline.json'), encoding='utf-8'))
o = json.loads(json.dumps(sto))
o['item_inputs'] = [x for x in o['item_inputs'] if not x['item'].endswith('tree_ring_chronology')] + [{'item': ts('solar_system_age_record'), 'count': 1}]
o['item_outputs'] = [{'id': ts('stratigraphic_timeline'), 'count': 2}]
o['research'] = ts('planet_formation')
dump(os.path.join(D, 'recipe/chronoweaving/r1150_stratigraphic_timeline.json'), o)

# crafting: the CMB array's casings
dump(os.path.join(D, 'recipe/crafting', 'r1150_bolometer_focal_plane.json'), extreme('bolometer_focal_plane', 4, P_PART, {'O': 'doped_wafer', 'I': 'superconducting_coil'}))

# ====================================================================== Ad Astra (only when installed)
AA = {'neoforge:conditions': [{'type': 'neoforge:mod_loaded', 'modid': 'ad_astra'}]}


def aa_shaped(rid, pattern, key, result, count):
    r = dict(AA)
    r.update({'type': 'minecraft:crafting_shaped', 'pattern': pattern,
              'key': {k: {'item': v if ':' in v else ts(v)} for k, v in key.items()}, 'result': {'id': ts(result), 'count': count}})
    dump(os.path.join(D, 'recipe/crafting', rid + '.json'), r)


aa_shaped('r1150_ad_astra_hull_plate', ['DD', 'DD'], {'D': 'ad_astra:desh_plate'}, 'pressure_hull_plate', 4)
aa_shaped('r1150_ad_astra_station_hull', ['OOO', 'OGO', 'OOO'], {'O': 'ad_astra:ostrum_plate', 'G': 'minecraft:glass'}, 'station_hull', 8)
aa_shaped('r1150_ad_astra_flame_trench', ['CCC', 'CWC', 'CCC'], {'C': 'ad_astra:calorite_plate', 'W': 'minecraft:water_bucket'}, 'flame_trench_block', 8)
aa_shaped('r1150_ad_astra_rocket_engine', ['E', 'P'], {'E': 'ad_astra:desh_engine', 'P': 'pressure_hull_plate'}, 'rocket_engine', 1)
L['tooltip.%s.ad_astra_routes' % NS] = 'With Ad Astra installed, its desh, ostrum and calorite plates and its engines stand in for some parts.'

# ====================================================================== tags and loot
CAS = ['bolometer_focal_plane']
tag_add(os.path.join(MC, 'tags/block/mineable/pickaxe.json'), [ts(x) for x in CAS + ['cmb_bolometer_array', 'ice_rheometer']])
tag_add(os.path.join(MC, 'tags/block/mineable/shovel.json'), [ts('chondritic_regolith')])
tag_add(os.path.join(MC, 'tags/block/mineable/pickaxe.json'), [ts('plume_ice')])
tag_add(os.path.join(MC, 'tags/block/needs_iron_tool.json'), [ts(x) for x in CAS + ['cmb_bolometer_array']])
for kind in ('block', 'item'):
    tag_add(os.path.join(D, 'tags', kind, 'casings.json'), [ts(x) for x in CAS])
    tag_add(os.path.join(D, 'tags', kind, 'machines.json'), [ts('cmb_bolometer_array'), ts('ice_rheometer')])
    tag_add(os.path.join(D, 'tags', kind, 'multiblock_controllers.json'), [ts('cmb_bolometer_array')])


def drops(bid, entries):
    dump(os.path.join(D, 'loot_table/blocks', bid + '.json'), {'type': 'minecraft:block', 'pools': [
        {'rolls': 1, 'entries': [{'type': 'minecraft:alternatives', 'children': [
            {'type': 'minecraft:item', 'name': ts(bid), 'conditions': [{'condition': 'minecraft:match_tool', 'predicate': {
                'predicates': {'minecraft:enchantments': [{'enchantments': 'minecraft:silk_touch', 'levels': {'min': 1}}]}}}]},
            {'type': 'minecraft:item', 'name': ts(entries[0][0]), 'functions': [
                {'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': entries[0][1], 'max': entries[0][2]}},
                {'function': 'minecraft:apply_bonus', 'enchantment': 'minecraft:fortune', 'formula': 'minecraft:uniform_bonus_count',
                 'parameters': {'bonusMultiplier': 1}}]}]}]}] + [
        {'rolls': 1, 'conditions': [{'condition': 'minecraft:survives_explosion'},
                                    {'condition': 'minecraft:inverted', 'term': {'condition': 'minecraft:match_tool', 'predicate': {
                                        'predicates': {'minecraft:enchantments': [{'enchantments': 'minecraft:silk_touch', 'levels': {'min': 1}}]}}}},
                                    {'condition': 'minecraft:random_chance', 'chance': ch}],
         'entries': [{'type': 'minecraft:item', 'name': ts(i)}]} for i, ch in entries[1:]]})


drops('plume_ice', [('plume_ice_grains', 1, 3)])
drops('chondritic_regolith', [('chondrule', 1, 2), ('calcium_aluminium_inclusion', 0.08)])

# ====================================================================== the multiblock: a microwave telescope on its mount
W, H, Dp = 11, 13, 11
g = grid(W, H, Dp)
c = W // 2
for z in range(Dp):                                   # the mount's base ring
    for x in range(W):
        if 3.5 <= math.hypot(x - c, z - c) <= 5.4:
            g[0][z][x] = 'F'
for y in range(1, 6):                                 # the cryostat column
    for z in range(c - 1, c + 2):
        for x in range(c - 1, c + 2):
            g[y][z][x] = 'F' if (x, z) != (c, c) else ('B' if y == 5 else 'F')
for y in range(6, 13):                                # the tilted dish: a disc leaning back
    for z in range(Dp):
        for x in range(W):
            # plane through (c, 9, c) tilted 40° toward -z
            zz = (z - c) * math.cos(0.7) + (y - 9) * math.sin(0.7)
            yy = -(z - c) * math.sin(0.7) + (y - 9) * math.cos(0.7)
            if abs(yy - 0.04 * ((x - c) ** 2 + zz ** 2)) < 0.55 and math.hypot(x - c, zz) <= 5.2:
                g[y][z][x] = 'T'
g[0][0][c] = 'C'
g[0][0][c - 2] = 'P'
g[0][0][c + 2] = 'P'
for x in range(c - 2, c + 3):                         # a path of frame from the controller to the ring
    for z in range(0, 6):
        if g[0][z][x] == ' ' and (x == c):
            g[0][z][x] = 'F'
T.MB_COUNTS['cmb_bolometer_array'] = write_mb('cmb_bolometer_array', rows(g),
                                              {'F': ['cryostat_shell'], 'B': ['bolometer_focal_plane'], 'T': ['cryostat_shell'],
                                               'P': ['cryostat_shell'] + PORT},
                                              extra={'effect_center': [c, 9, c], 'effect_scale': 3.0,
                                                     'fx': [{'type': 'glow', 'chars': 'B', 'color': '#80C8FF', 'hot': '#E0F4FF'}]})
print('cmb bolometer array blocks:', T.MB_COUNTS['cmb_bolometer_array'])

# ====================================================================== text the Java reads
LANGX = {
    'message.%s.core_hears_1': 'The Core falls silent, listening. Then, for the first time, it hums back.',
    'message.%s.core_hears_2': 'A count of primes. A picture built from them: a ring of light about a place it knows.',
    'message.%s.core_hears_3': 'The signal came from Home.',
    'message.%s.core_hears_again': 'The Core has heard this signal before. It came from Home.',
    'message.%s.home_signal_arrival': 'The signal you heard is here, all around you: this is where it came from.',
}
for k, v in LANGX.items():
    L[k % NS] = v

# ====================================================================== the Codex
E = {}
E['fluids/tidal_heating'] = {'title': 'Icy Moons', 'icon': ts('plume_ice_grains'), 'category': 'fluids', 'research': 'tidal_heating', 'pages': [
    text("Ė = (21/2)·(k₂/Q)·G·M²·R⁵·n·e²/a⁶. A moon on an eccentric orbit is squeezed once each turn, and its ice rubs itself warm. The $(thing)Plume Field$() in the Titan Ocean has $(item)Plume Ice$(), which gives Plume Ice Grains."),
    spot(['ice_rheometer', 'ocean_world_record'], "The $(thing)Ice Rheometer$() squeezes the ice in cycles. Warm ice dissipates most when the forcing matches its Maxwell time, ωτ = 1; set the period, eccentricity and temperature until the moon's tidal heating is what keeps its ocean: Enceladus 5 to 50 GW, Europa 100 to 2,000.", 'Ice Rheometer')]}
E['fluids/planet_formation'] = {'title': 'How Planets Form', 'icon': ts('chondrule'), 'category': 'fluids', 'research': 'planet_formation', 'pages': [
    text("Q = cs·κ/(πGΣ). Below 1 a disk breaks into clumps. The $(thing)Protoplanetary Disk$() in the Accretion Frontier is made of $(item)Chondritic Regolith$(), which gives Chondrules and, rarely, Calcium-Aluminium-rich Inclusions."),
    spot(['calcium_aluminium_inclusion', 'solar_system_age_record'], "The Dating Spectrometer dates the inclusions by uranium's decay to lead, 4.567 billion years: a Solar System Age Record, which the Chronoweaver sets as the first layer of a Stratigraphic Timeline. The Spectrograph assays chondrules for Meteoric Iron and Presolar Grains.", 'The oldest solids')]}
E['fluids/habitability'] = {'title': 'Habitability and Signs of Life', 'icon': ts('biosignature_record'), 'category': 'fluids', 'research': 'habitability', 'pages': [
    text("T = T★·(1 − A)^¼·√(R★/2a). The $(thing)Reflecting Telescope$() gains transit spectroscopy: an atmosphere's lines against the photon noise, S/N = depth·√N. Long exposures with a wide mirror on a bright star make a Transmission Spectrum Plate."),
    spot(['transmission_spectrum_plate', 'biosignature_record'], "The Spectrograph sets an Ocean World Record beside a Transmission Spectrum Plate: a $(item)Biosignature Record$().", 'Biosignatures')]}
E['fluids/cosmic_microwave_background'] = {'title': 'The Oldest Light', 'icon': ts('last_scattering_map'), 'category': 'fluids', 'research': 'cosmic_microwave_background', 'pages': [
    spot(['cmb_bolometer_array'], "B(ν, T) = (2hν³/c²)/(e^(hν/kT) − 1), at T = 2.725 K. The $(thing)CMB Bolometer Array$() (%d blocks: a tilted mirror on a mount over a cryostat) maps it. Look where the CMB outshines the galaxy (60 to 250 GHz), with detectors and hours enough for a noise under 0.05 µK, and keep the focal plane cold: it burns helium-3." % T.MB_COUNTS['cmb_bolometer_array'], 'CMB Bolometer Array'),
    {'type': 'multiblock', 'name': 'CMB Bolometer Array', 'text': 'Interface Ports either side of the controller, on the mount.', 'multiblock': 'cmb_bolometer_array'},
    spot(['last_scattering_map'], "The $(item)Last Scattering Map$() fixes the inflaton's spectrum: with a Dark Matter Lens, the Inflation Chamber makes Inflaton Condensate.", 'Last Scattering Map')]}
E['fluids/fast_transients'] = {'title': 'Radio Flashes and Torn Stars', 'icon': ts('dispersion_record'), 'category': 'fluids', 'research': 'fast_transients', 'pages': [
    text("Δt = 4.15 ms·DM·(f₁⁻² − f₂⁻²). The $(thing)Radio Telescope$() catches fast radio bursts: bright enough, and with the sweep across each channel under a millisecond. A Dispersion Record weighs the gas between galaxies; the Spectrograph turns two into Cosmic Web Filaments."),
    spot(['disrupted_stellar_stream'], "The $(thing)Burst Observatory$() catches a star torn apart by a black hole: almost certain to see one, almost never fooled. The Stellar Plasma Crucible makes its stream into tidal plasma.", 'Tidal disruption')]}
E['fluids/search_for_others'] = {'title': 'Listening for Others', 'icon': ts('narrowband_candidate'), 'category': 'fluids', 'research': 'search_for_others', 'pages': [
    text("N = R★·fp·ne·fl·fi·fc·L. With a Biosignature Record to point it, the $(thing)Radio Telescope$() listens at the water hole for a carrier a hundred hertz wide: tune exactly to 1420.4 MHz with the narrowest band, and listen long."),
    spot(['narrowband_candidate', 'signal_from_home'], "Take the $(item)Narrowband Candidate$() to the $(thing)Omnipotence Core$() and use it there. The Core has heard this before.", 'The signal')]}
OPS = {'ice_rheometer': ("Squeeze the ice in cycles until the moon's tidal heating is what its ocean needs.",
                         'Forcing period, Eccentricity, Ice temperature', 'grain growth: the dissipation drifts by 1%·d.',
                         'a strain over 0.05 cracks the sample (wear).', 'faster cycles, sooner.',
                         ['a = (GM·P²/4π²)^⅓', 'ωτ = ω·η/μ', '1/Q = ωτ/(1 + ω²τ²)', 'Ė = (21/2)(k₂/Q)GM²R⁵ne²/a⁶'], 'tidal_heating'),
       'cmb_bolometer_array': ("Look where the CMB outshines the galaxy, quietly enough to see a hundred-thousandth.",
                               'Observing frequency, Detectors, Integration', 'atmospheric drift: the foregrounds rise by 2%·d.',
                               'over 10 K it stops; too many detectors at too high a frequency overheat the readout (wear).',
                               'a shorter integration, sooner.',
                               ['B = (2hν³/c²)/(e^(hν/kT) − 1)', 'contrast = B/foreground', 'noise = 300 µK·√s/√(n·t)'], 'cosmic_microwave_background')}
for mid, (intro, sets, dist, lim, rate, eqs, rs) in OPS.items():
    E['operation/' + mid] = {'title': T.MACHINES[mid]['name'], 'icon': ts(mid), 'category': 'operation', 'research': rs,
                             'pages': op_pages(mid, intro, sets, dist, lim, rate, eqs)}
NODES = {'physics': [{'entry': 'fluids/tidal_heating', 'x': 54, 'y': 93, 'parents': ['fluids/comets']},
                     {'entry': 'fluids/planet_formation', 'x': 50, 'y': 93, 'parents': ['fluids/asteroid_taxonomy']},
                     {'entry': 'fluids/habitability', 'x': 56, 'y': 93, 'parents': ['fluids/tidal_heating']},
                     {'entry': 'fluids/cosmic_microwave_background', 'x': 56, 'y': 89, 'parents': ['fluids/nebular_gases']},
                     {'entry': 'fluids/fast_transients', 'x': 58, 'y': 89, 'parents': ['fluids/cosmic_microwave_background']},
                     {'entry': 'fluids/search_for_others', 'x': 58, 'y': 93, 'parents': ['fluids/habitability', 'fluids/fast_transients']}],
         'machines': [{'entry': 'operation/ice_rheometer', 'x': 40, 'y': 22, 'parents': []},
                      {'entry': 'operation/cmb_bolometer_array', 'x': 42, 'y': 22, 'parents': []}]}
codex(E, NODES)

finish()
print('research', len(T.RESEARCH), 'machines', len(T.MACHINES), 'recipes', len(T.RECIPES))
