"""
1.6.0: Archaeology. Digging up the past with the tools of physics, chemistry and mathematics: ground-penetrating radar,
luminescence dating, dendrochronology, conservation science, isotope provenance, archaeometallurgy and photogrammetry.
A new Overworld site (the Buried Tell) whose finds are brushed out of suspicious soils, a handheld Survey Radar that
finds them, five consoles and two multiblocks (the Bloomery Shaft and the Photogrammetry Gantry). The finds end in an
Excavation Record, which gives a new, cheaper route to the Arrow of Time Shard; Bloomery Steel gives cheaper I-beams.

Run:  python3 tools/round160.py
"""
import json, math, os, sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from tsgen import *          # noqa: F401,F403
import tsgen as T

L = T.LANG

# ====================================================================== research
research('ground_penetrating_radar', 'Ground-Penetrating Radar', 12, 30, 160, ['remote_sensing', 'electrodynamics'],
         [('minecraft:copper_ingot', 16), ('minecraft:redstone', 16)], 200,
         "A radio pulse sent into the ground slows by the square root of the soil's permittivity and echoes back from anything "
         "that is not soil: a wall, a hearth, a buried pot. Time the echo and you know how deep it lies. Lower frequencies "
         "see deeper; higher ones see finer. Unlocks the Radar Survey Bench and the Survey Radar, which finds buried finds.",
         'Seeing into the ground',
         {'goal': 'A radar pulse travels down through soil of relative permittivity εr and back up, taking a two-way time t. Write '
                  'the depth d of the reflector, if c is the speed of light.',
          'name': 'Echo depth', 'lhs': 'd', 'form': 'c * t / ( 2 * sqrt ( εr ) )', 'expr': 'c * t / ( 2 * sqrt ( er ) )',
          'ask': 'Find d in metres for c = 0.3 m/ns, t = @{t} ns and εr = @{er}.',
          'vars': {'d': 'depth', 'c': 'speed of light', 't': 'two-way travel time', 'εr': 'relative permittivity of the soil'},
          'given': {'c': 0.3, 't': 40.0, 'er': 9.0}, 'unit': 'm',
          'method': 'In the soil the pulse moves at v = c/√εr; it goes down and back, so the depth is half of v·t.',
          'distractors': ['*', '4', '+'], 'vary': {'t': [10.0, 100.0, 5.0], 'er': [4.0, 25.0, 1.0]}},
         'radar_survey_bench', 'remote_sensing')
research('luminescence_dating', 'Luminescence Dating', 14, 30, 180, ['radiometric_dating', 'radioactivity'],
         [('ancient_potsherd', 2), ('minecraft:glowstone_dust', 4)], 240,
         "Quartz grains in a pot or a hearth stone are dosimeters. Firing empties their electron traps; afterwards the trace "
         "radioactivity of the soil fills them again, at a steady rate. Heat or light the grains in the laboratory and they "
         "glow with what they stored: the dose since the fire, divided by the yearly dose, is the age. Unlocks the "
         "Luminescence Reader.",
         'A clock started by fire',
         {'goal': 'A sherd has received an equivalent dose De since it was fired, at an annual dose rate Ḋ. Write its age A.',
          'name': 'The luminescence age', 'lhs': 'A', 'form': 'De / Ḋ', 'expr': 'De / Dr',
          'ask': 'Find A in thousands of years for De = @{De} Gy and Ḋ = @{Dr} Gy per thousand years.',
          'vars': {'A': 'age', 'De': 'equivalent dose', 'Ḋ': 'annual dose rate'},
          'given': {'De': 12.0, 'Dr': 3.0}, 'unit': 'ka',
          'method': 'The traps fill at a steady rate since the firing: the dose stored is the rate times the time.',
          'distractors': ['*', '-', 'ln'], 'vary': {'De': [2.0, 60.0, 1.0], 'Dr': [1.0, 6.0, 0.5]}},
         'luminescence_reader', 'radiometric_dating')
research('dendrochronology', 'Dendrochronology', 16, 30, 180, ['statistics', 'plant_physiology'],
         [('charred_beam', 2), ('minecraft:oak_log', 8)], 240,
         "A tree lays down a ring a year, wide in good years and narrow in bad, and every tree in a region keeps the same "
         "pattern. Slide a beam's rings along a master chronology until the patterns match, and its outermost ring is dated "
         "to the year it was felled. The match is tested by correlation: Baillie and Pilcher's t. Unlocks the Tree-Ring "
         "Correlator.",
         'A calendar in the wood',
         {'goal': "A beam's ring widths correlate with the master chronology with coefficient r over n overlapping years. Write "
                  "Student's t for that correlation.",
          'name': 'The cross-dating t', 'lhs': 't', 'form': 'r * sqrt ( ( n - 2 ) / ( 1 - r ^ 2 ) )',
          'expr': 'r * sqrt ( ( n - 2 ) / ( 1 - r ^ 2 ) )',
          'ask': 'Find t for r = @{r} over n = @{n} years.',
          'vars': {'t': "Student's t", 'r': 'correlation', 'n': 'overlapping years'},
          'given': {'r': 0.6, 'n': 80.0}, 'unit': '',
          'method': 'The correlation measured over n pairs is tested against no match at all: t has n − 2 degrees of freedom.',
          'distractors': ['+', '*', 'exp'], 'vary': {'r': [0.3, 0.8, 0.05], 'n': [30.0, 200.0, 10.0]}},
         'tree_ring_correlator', 'statistics')
research('conservation_science', 'Conservation Science', 18, 30, 200, ['corrosion_science', 'electrochemistry'],
         [('corroded_artefact', 1), ('minecraft:glass_bottle', 4)], 240,
         "An iron object out of the ground is full of chloride. Left in the air it rusts from the inside and splits apart "
         "within years. Soaked in an alkaline bath, the chloride diffuses out of it; the bath is changed until none is left. "
         "Unlocks the Conservation Bath: Corroded Artefacts become Stabilised Artefacts.",
         'Saving it from the air',
         {'goal': 'A slab of thickness L soaks in a bath; the chloride in it, C₀ at first, diffuses out with diffusivity D. Write '
                  'the chloride C left after a time t (the slowest mode of the diffusion).',
          'name': 'Desalination by diffusion', 'lhs': 'C', 'form': 'C0 * exp ( - π ^ 2 * D * t / L ^ 2 )',
          'expr': 'C0 * exp ( - π ^ 2 * D * t / L ^ 2 )',
          'ask': 'Find C in ppm for C₀ = @{C0} ppm, D = @{D} mm²/day, t = @{t} days and L = @{L} mm.',
          'vars': {'C': 'chloride left', 'C0': 'chloride at first', 'D': 'diffusivity', 't': 'time', 'L': 'thickness'},
          'given': {'C0': 2000.0, 'D': 0.5, 't': 20.0, 'L': 10.0}, 'unit': 'ppm',
          'method': "Fick's second law: the slowest mode of a slab decays as e^(−π²Dt/L²); the faster ones are long gone.",
          'distractors': ['ln', '+', '2'], 'vary': {'t': [5.0, 60.0, 5.0]}},
         'conservation_bath', 'corrosion_science')
research('isotope_provenance', 'Isotope Provenance', 20, 30, 220, ['mass_spectrometry', 'conservation_science'],
         [('stabilised_artefact', 1), ('minecraft:quartz', 8)], 300,
         "Every ore body and every landscape carries its own ratio of strontium or lead isotopes, and that ratio passes "
         "unchanged into the metal smelted from it and the bones of those who ate from it. Measure the ratio in a find and "
         "it tells where it came from, or how much came from each of two sources. Unlocks the Isotope Provenancer.",
         'Where it came from',
         {'goal': 'A find mixes metal from two sources with isotope ratios Ra and Rb; its own ratio is R. Write the fraction f '
                  'that came from source a.',
          'name': 'Two-source mixing', 'lhs': 'f', 'form': '( R - Rb ) / ( Ra - Rb )', 'expr': '( R - Rb ) / ( Ra - Rb )',
          'ask': 'Find f for R = @{R}, Ra = 0.712 and Rb = 0.708.',
          'vars': {'f': 'fraction from source a', 'R': 'ratio in the find', 'Ra': 'ratio of source a', 'Rb': 'ratio of source b'},
          'given': {'R': 0.7095, 'Ra': 0.712, 'Rb': 0.708}, 'unit': '',
          'method': 'The ratio is a weighted mean: R = f·Ra + (1 − f)·Rb. Solve for f.',
          'distractors': ['*', '+', 'ln'], 'vary': {'R': [0.7085, 0.7115, 0.0005]}},
         'isotope_provenancer', 'mass_spectrometry')
research('archaeometallurgy', 'Archaeometallurgy', 22, 30, 260, ['ceramics', 'corrosion_science', 'conservation_science'],
         [('slag_cake', 2), ('minecraft:charcoal', 16)], 300,
         "Before blast furnaces, iron was won in a bloomery: ore and charcoal in a clay shaft, air blown in through a nozzle, "
         "the iron reduced to a spongy bloom that never melts, carbon soaking into it from the hot charcoal around it. How "
         "deep the carbon goes, and so how hard the steel, is set by diffusion. The slag the smiths left behind says how "
         "they did it. Unlocks the Bloomery Shaft, a multiblock: Bloomery Steel.",
         'Iron without melting',
         {'goal': 'Carbon diffuses into hot iron with diffusivity D. Write the depth x it reaches in a time t.',
          'name': 'Carburising depth', 'lhs': 'x', 'form': '2 * sqrt ( D * t )', 'expr': '2 * sqrt ( D * t )',
          'ask': 'Find x in mm for D = 0.0001 mm²/s and t = @{t} s.',
          'vars': {'x': 'depth', 'D': 'diffusivity of carbon in iron', 't': 'time'},
          'given': {'D': 0.0001, 't': 3600.0}, 'unit': 'mm',
          'method': 'Diffusion spreads as the square root of time: the error function falls to half near x = 2√(Dt).',
          'distractors': ['*', '/', 'exp'], 'vary': {'t': [600.0, 36000.0, 600.0]}},
         'bloomery_shaft', 'conservation_science')
research('photogrammetry', 'Photogrammetry', 24, 30, 320, ['cartography', 'luminescence_dating', 'dendrochronology', 'isotope_provenance'],
         [('dated_potsherd', 2), ('tree_ring_chronology', 1)], 360,
         "Two photographs of a trench taken a baseline apart see each point at slightly different places: the disparity. "
         "From it, the focal length and the baseline, every point's depth follows, and hundreds of overlapping photographs "
         "rebuild the whole excavation in three dimensions, layer by layer, find by find. Unlocks the Photogrammetry "
         "Gantry, a multiblock: Excavation Records.",
         'The dig, kept for ever',
         {'goal': 'A stereo pair with focal length f and baseline B sees a point with disparity d. Write its distance Z from the '
                  'cameras.',
          'name': 'Depth from disparity', 'lhs': 'Z', 'form': 'f * B / d', 'expr': 'f * B / d',
          'ask': 'Find Z in metres for f = 0.05 m, B = @{B} m and d = 0.005 m.',
          'vars': {'Z': 'distance', 'f': 'focal length', 'B': 'baseline', 'd': 'disparity on the sensor'},
          'given': {'f': 0.05, 'B': 1.0, 'd': 0.005}, 'unit': 'm',
          'method': 'Similar triangles: the baseline over the distance equals the disparity over the focal length.',
          'distractors': ['+', '*', 'sqrt'], 'vary': {'B': [0.2, 2.0, 0.1]}},
         'photogrammetry_gantry', 'cartography')

# ====================================================================== consoles
RS = model([par('f', 'MHz', 50, 1600, 10, 250), par('Pw', 'W', 1, 100, 1, 10)],
           {'er': 9},
           [der('v', 'm/ns', '0.3/sqrt(er)', 'v = c/√εr'),
            der('res', 'm', '75/(f*sqrt(er))*(1 + 0.01*d)', 'resolution: a quarter wavelength'),
            der('reach', 'm', '30*sqrt(Pw/10)/(1 + f/400)', 'reach: power against attenuation, which grows with frequency')],
           [con('burn', 'wear', '80 - Pw', 'under 80 W, or the transmitter overheats')],
           'clamp(f/400, 0.3, 1.5)', 'higher frequencies tune faster')
machine('radar_survey_bench', 'Radar Survey Bench', "A bench of antennas and a pulse generator, with a tank of damp soil to "
        "test them in: it tunes a ground-penetrating radar to the ground it will survey, and builds it into a Survey Radar. "
        "Made by crafting.", 'wave', 2, RS,
        {'p.f': 'Antenna frequency', 'p.f.desc': 'Higher sees finer detail, lower sees deeper.', 'p.Pw': 'Pulse power',
         'p.Pw.desc': 'Stronger pulses reach deeper. Over 80 W the transmitter overheats.', 'd.v': 'Speed in soil',
         'd.v.desc': 'How fast the pulse travels in the ground.', 'd.res': 'Resolution', 'd.res.desc': 'The smallest thing it can tell apart.',
         'd.reach': 'Reach', 'd.reach.desc': 'How deep its echoes still come back from.', 'c.burn': 'The transmitter is overheating',
         'v.er': 'Soil permittivity', 'disturb': 'Damp: resolution varies by ±1%·d.', 'rate.desc': 'Higher frequencies tune faster.'},
        extreme('radar_survey_bench', 1, P_CONSOLE, {'A': 'minecraft:copper_ingot', 'F': 'ferrite_magnet', 'L': 'minecraft:compass', 'N': 'minecraft:observer'}))

LR = model([par('Tp', '°C', 100, 320, 5, 200), par('St', 'mW/cm²', 5, 80, 1, 20)],
           {'De': 12, 'Dr': 3},
           [der('Rec', '', '(1 - exp(-pos(Tp - 160)/40))*exp(-pos(Tp - 260)/30)*(1 - 0.01*d)', 'signal recovered: preheat empties the shallow traps, too hot empties the deep ones'),
            der('Unc', '', '0.03 + 2/St', 'relative uncertainty of the dose'),
            der('A', 'ka', 'De/Dr', 'age = De/Ḋ')],
           [con('bleach', 'wear', '70 - St', 'stimulation under 70 mW/cm², or the reader bleaches its own reference')],
           'clamp(St/40, 0.2, 1.5)', 'brighter stimulation reads faster')
machine('luminescence_reader', 'Luminescence Reader', "A dark chamber with a heater plate, blue lamps and a photomultiplier: "
        "grains from a sherd or a flint are preheated, lit, and made to give up the glow they stored since they were last "
        "fired. Made by crafting.", 'spectrum', 2, LR,
        {'p.Tp': 'Preheat', 'p.Tp.desc': 'Clears the shallow, unstable traps. Too hot, and the deep ones that keep time empty too.',
         'p.St': 'Stimulation', 'p.St.desc': 'How bright the blue lamps shine on the grains.', 'd.Rec': 'Recovery',
         'd.Rec.desc': 'How much of the stored signal is read.', 'd.Unc': 'Uncertainty', 'd.Unc.desc': 'Of the measured dose.',
         'd.A': 'Age', 'd.A.desc': 'Thousands of years since the firing.', 'c.bleach': 'The reference is bleaching',
         'v.De': 'Equivalent dose', 'v.Dr': 'Annual dose', 'disturb': 'Background: recovery varies by ±1%·d.',
         'rate.desc': 'Brighter stimulation reads faster.'},
        extreme('luminescence_reader', 1, P_CONSOLE, {'A': 'minecraft:smooth_stone', 'F': 'minecraft:glowstone_dust', 'L': 'minecraft:daylight_detector', 'N': 'minecraft:furnace'}))

TR = model([par('n', 'years', 20, 300, 5, 50), par('lag', 'years', -40, 40, 1, 0), par('dt', 'years', 10, 300, 5, 100)],
           {'L0': 17, 'rho': 0.7},
           [der('m', '', 'exp(-((lag - L0)/2)^2)', 'match: the patterns line up'),
            der('r', '', 'rho*exp(-((lag - L0)/2)^2)*(1 - exp(-dt/30))*exp(-dt/600)*(1 - 0.01*d)', 'correlation after detrending'),
            der('t', '', 'rho*exp(-((lag - L0)/2)^2)*(1 - exp(-dt/30))*exp(-dt/600)*sqrt((n - 2)/(1 - (rho*exp(-((lag - L0)/2)^2)*(1 - exp(-dt/30))*exp(-dt/600))^2))',
                "Student's t = r·√((n − 2)/(1 − r²))")],
           [],
           'clamp(100/n, 0.3, 1.5)', 'shorter overlaps match faster')
machine('tree_ring_correlator', 'Tree-Ring Correlator', "A travelling microscope that measures a beam's rings to a hundredth of "
        "a millimetre, and a computer that slides them along the master chronology year by year until they match. Made by "
        "crafting.", 'chain', 2, TR,
        {'p.n': 'Overlap', 'p.n.desc': 'How many rings to compare: more give a surer match.', 'p.lag': 'Offset',
         'p.lag.desc': 'Where on the master chronology the beam is tried.', 'p.dt': 'Detrending',
         'p.dt.desc': 'The span of the curve that removes the slow growth trend: too short removes the signal too.',
         'd.m': 'Match', 'd.m.desc': 'Whether the patterns line up.', 'd.r': 'Correlation', 'd.r.desc': 'Of the ring widths.',
         'd.t': "Student's t", 'd.t.desc': 'Over 6 is a sure date.', 'v.L0': 'True offset', 'v.rho': 'Signal strength',
         'disturb': 'Charring: correlation varies by ±1%·d.', 'rate.desc': 'Shorter overlaps match faster.'},
        extreme('tree_ring_correlator', 1, P_CONSOLE, {'A': 'minecraft:oak_planks', 'F': 'minecraft:spyglass', 'L': 'minecraft:clock', 'N': 'minecraft:lectern'}))

CB = model([par('cN', '%', 0, 2, 0.05, 0.5), par('Tb', '°C', 10, 70, 1, 20), par('ts', 'days', 1, 60, 1, 10)],
           {'C0': 3000, 'Lt': 8},
           [der('Df', 'mm²/day', '0.3*(1 + cN)*exp((Tb - 20)/30)', 'chloride diffusivity: alkali and warmth open the rust'),
            der('C', 'ppm', 'C0*exp(-9.8696*0.3*(1 + cN)*exp((Tb - 20)/30)*ts/Lt^2)*(1 + 0.02*d)', 'C = C₀·e^(−π²Dt/L²)')],
           [con('etch', 'wear', '1.6 - cN', 'under 1.6% alkali, or it etches the metal'),
            con('boil', 'wear', '60 - Tb', 'under 60 °C, or the bath boils off the coatings')],
           'clamp(10/ts, 0.2, 1.5)', 'shorter soaks are quicker')
machine('conservation_bath', 'Conservation Bath', "A heated tank of alkaline solution, stirred and changed on a timer: an "
        "object from the ground soaks in it until the chloride inside has diffused out, then is dried and sealed. Made by "
        "crafting.", 'oxidation', 2, CB,
        {'p.cN': 'Alkali', 'p.cN.desc': 'Strength of the bath. Over 1.6% it etches the metal.', 'p.Tb': 'Temperature',
         'p.Tb.desc': 'Warmer baths draw the chloride out faster. Over 60 °C they boil off the coatings.', 'p.ts': 'Soak',
         'p.ts.desc': 'Days in the bath.', 'd.Df': 'Diffusivity', 'd.Df.desc': 'How freely the chloride moves out.',
         'd.C': 'Chloride left', 'd.C.desc': 'Under 100 ppm the object is stable.', 'c.etch': 'The bath is etching the metal',
         'c.boil': 'The bath is too hot', 'v.C0': 'Chloride at first', 'v.Lt': 'Thickness',
         'disturb': 'Old salts: chloride varies by ±2%·d.', 'rate.desc': 'Shorter soaks are quicker.'},
        extreme('conservation_bath', 1, P_CONSOLE, {'A': 'minecraft:glass', 'F': 'passivated_plate', 'L': 'minecraft:cauldron', 'N': 'minecraft:brewing_stand'}))

IP = model([par('Pp', 'kW', 0.8, 1.6, 0.01, 1.0), par('acq', 's', 10, 300, 5, 30)],
           {'R': 0.7095, 'Ra': 0.712, 'Rb': 0.708},
           [der('sig', '', '0.0002*sqrt(60/acq)*(1 + 3*abs(Pp - 1.25))*(1 + 0.01*d)', 'precision of the ratio'),
            der('f', '', '(R - Rb)/(Ra - Rb)', 'f = (R − Rb)/(Ra − Rb)'),
            der('sep', 'σ', '(Ra - Rb)/(0.0002*sqrt(60/acq)*(1 + 3*abs(Pp - 1.25))*(1 + 0.01*d))', 'how cleanly the sources are told apart')],
           [con('cone', 'wear', '1.55 - Pp', 'plasma under 1.55 kW, or it erodes the cones')],
           'clamp(60/acq, 0.2, 1.5)', 'shorter acquisitions are quicker')
machine('isotope_provenancer', 'Isotope Provenancer', "A plasma-source mass spectrometer with a ring of collectors: a speck "
        "drilled from a find is burnt in argon plasma and its isotopes counted, side by side, to the sixth figure. Made by "
        "crafting.", 'spectrum', 3, IP,
        {'p.Pp': 'Plasma power', 'p.Pp.desc': 'Steadiest near 1.25 kW. Over 1.55 kW it erodes the cones.', 'p.acq': 'Acquisition',
         'p.acq.desc': 'Seconds of counting: longer is more precise.', 'd.sig': 'Precision', 'd.sig.desc': 'Of the isotope ratio.',
         'd.f': 'Mixing fraction', 'd.f.desc': 'How much came from the first source.', 'd.sep': 'Separation',
         'd.sep.desc': 'How many σ apart the two sources lie. Over 25 is unmistakable.', 'c.cone': 'The cones are eroding',
         'v.R': 'Ratio in the find', 'v.Ra': 'Source a', 'v.Rb': 'Source b', 'disturb': 'Drift: precision varies by ±1%·d.',
         'rate.desc': 'Shorter acquisitions are quicker.'},
        extreme('isotope_provenancer', 1, P_CONSOLE, {'A': 'minecraft:iron_ingot', 'F': 'ferrite_magnet', 'L': 'mass_spectrometer', 'N': 'minecraft:blast_furnace'}))

BS = model([par('Tf', '°C', 900, 1550, 10, 1100), par('cr', '', 0.5, 3, 0.05, 1), par('th', 'h', 1, 24, 0.5, 4)],
           {},
           [der('Dc', 'µm²/s', '23000000*exp(-148000/(8.314*(Tf + 273)))', 'D = D₀·e^(−Q/RT) for carbon in iron'),
            der('x', 'mm', '2*sqrt(0.000023*exp(-148000/(8.314*(Tf + 273)))*th*3600)*1000*(1 - 0.01*d)', 'x = 2√(Dt)'),
            der('Cw', '%', '0.9*(1 - exp(-2*sqrt(0.000023*exp(-148000/(8.314*(Tf + 273)))*th*3600)*1000/2))*clamp(cr/1.5, 0, 1.2)',
                'carbon in the bloom')],
           [con('melt', 'stop', '1450 - Tf', 'under 1450 °C: hotter, the bloom melts into brittle cast iron'),
            con('choke', 'wear', '2.8 - cr', 'charcoal under 2.8 per ore, or the shaft chokes')],
           'clamp(8/th, 0.2, 1.5)', 'shorter smelts are quicker')
machine('bloomery_shaft', 'Bloomery Shaft', "The controller of a clay bloomery: a tapering shaft of fired clay with bellows "
        "blowing through its tuyeres, ore and charcoal fed in at the top, a spongy bloom of steel drawn out at the foot. A "
        "multiblock.", 'oxidation', 3, BS,
        {'p.Tf': 'Furnace heat', 'p.Tf.desc': 'Hotter carburises faster. Over 1450 °C the bloom melts into cast iron.',
         'p.cr': 'Charcoal', 'p.cr.desc': 'Charcoal per measure of ore: more makes more carbon, too much chokes the shaft.',
         'p.th': 'Smelt', 'p.th.desc': 'Hours in the shaft.', 'd.Dc': 'Diffusivity', 'd.Dc.desc': 'Of carbon in the hot iron.',
         'd.x': 'Carbon depth', 'd.x.desc': 'How far the carbon has gone in.', 'd.Cw': 'Carbon', 'd.Cw.desc': 'In the bloom: steel is 0.5 to 1.2%.',
         'c.melt': 'The bloom is melting', 'c.choke': 'The shaft is choking', 'disturb': 'Draught: depth varies by ±1%·d.',
         'rate.desc': 'Shorter smelts are quicker.'},
        extreme('bloomery_shaft', 1, P_CTRL, {'F': 'fired_clay_shaft', 'T': 'tuyere_block', 'M': 'bellows_block', 'B': 'minecraft:blast_furnace'}),
        multiblock=True)

PG = model([par('B', 'm', 0.2, 3, 0.05, 0.5), par('fl', 'mm', 20, 200, 1, 35), par('ov', '', 0.3, 0.9, 0.01, 0.5)],
           {'Z0': 4},
           [der('gsd', 'mm', 'Z0*4/fl', 'ground sample distance: what one pixel covers'),
            der('sz', 'mm', '(Z0*1000)^2/(fl*B*1000)*0.002*(1 + 0.02*d)', 'depth precision σZ = Z²·σd/(f·B)'),
            der('bh', '', 'B/Z0', 'base to height ratio')],
           [con('match', 'wear', '0.8 - B/Z0', 'base to height under 0.8, or the views are too different to match')],
           'clamp(1.2 - ov, 0.2, 1.5)', 'less overlap, fewer photographs')
machine('photogrammetry_gantry', 'Photogrammetry Gantry', "The controller of a camera gantry over an excavation: a bridge on "
        "rails carrying a stereo pair of cameras down the trench, photographing every layer from above as the dig goes "
        "down. A multiblock.", 'chain', 3, PG,
        {'p.B': 'Baseline', 'p.B.desc': 'How far apart the two cameras are.', 'p.fl': 'Focal length', 'p.fl.desc': 'Of the lenses.',
         'p.ov': 'Overlap', 'p.ov.desc': 'How much each photograph shares with the next.', 'd.gsd': 'Pixel size',
         'd.gsd.desc': 'On the ground.', 'd.sz': 'Depth precision', 'd.sz.desc': 'Under 2 mm resolves every trowel cut.',
         'd.bh': 'Base to height', 'd.bh.desc': 'Of the stereo pair.', 'c.match': 'The views are too different to match',
         'v.Z0': 'Camera height', 'disturb': 'Vibration: precision varies by ±2%·d.', 'rate.desc': 'Less overlap, fewer photographs.'},
        extreme('photogrammetry_gantry', 1, P_CTRL, {'F': 'survey_gantry_frame', 'T': 'camera_rig', 'M': 'datum_plinth', 'B': 'minecraft:spyglass'}),
        multiblock=True)

# ====================================================================== blocks and items
FOUND_TELL = ("Found in Buried Tells: low mounds in plains, savannas, deserts and badlands of the Overworld, brushed out of "
              "Suspicious Tell Soil.")
FOUND_ASH = ("Found in Buried Tells: low mounds in plains, savannas, deserts and badlands of the Overworld, brushed out of "
             "Suspicious Ash in their burnt layers.")
simple_block('tell_soil', 'Tell Soil', "Earth that was once floors and walls and rubbish: centuries of a town trodden flat and "
             "built on again. It is darker than the ground round it, and full of crumbs of charcoal and brick.",
             "Found in Buried Tells, low mounds in the plains, savannas, deserts and badlands of the Overworld.")
simple_block('ash_layer', 'Ash Layer', "A band of grey ash and charcoal through a tell: the night one of its towns burnt down.",
             "Found in the burnt layers of Buried Tells.")
simple_block('ancient_mud_brick', 'Ancient Mud Brick', "Sun-dried brick of mud and straw, laid in the walls of a town four "
             "thousand years ago. The next town built straight on top of its stubs.",
             "Found in the walls of Buried Tells.")
for bid, name, base, found in (('suspicious_tell_soil', 'Suspicious Tell Soil', 'tell_soil', FOUND_TELL),
                               ('suspicious_ash_layer', 'Suspicious Ash', 'ash_layer', FOUND_ASH)):
    dump(os.path.join(A, 'blockstates', bid + '.json'),
         {'variants': {'dusted=%d' % k: {'model': '%s:block/%s_%d' % (NS, bid, k)} for k in range(4)}})
    for k in range(4):
        dump(os.path.join(A, 'models/block', '%s_%d.json' % (bid, k)), {'parent': 'minecraft:block/cube_all', 'textures': {'all': '%s:block/%s_%d' % (NS, bid, k)}})
    dump(os.path.join(A, 'models/item', bid + '.json'), {'parent': '%s:block/%s_0' % (NS, bid)})
    dump(os.path.join(D, 'loot_table/blocks', bid + '.json'), {'type': 'minecraft:block', 'pools': []})
    L['block.%s.%s' % (NS, bid)] = name
    L['block.%s.%s.lore' % (NS, bid)] = ("Something is buried in this. Brush it with a Brush to bring it out; dig it and whatever "
                                         "was in it is lost." if bid == 'suspicious_tell_soil' else
                                         "Something lies in the ash. Brush it with a Brush to bring it out; dig it and it is lost.")
    L['block.%s.%s.found' % (NS, bid)] = 'Found in Buried Tells. The Survey Radar finds them.'

casing('fired_clay_shaft', 'Fired Clay Shaft', "Courses of fired clay daubed over a wattle core, burnt hard by the first "
       "smelt: the walls of a bloomery. Made by crafting.")
casing('tuyere_block', 'Tuyere Block', "A clay nozzle set through the bloomery's wall, glowing at its tip, where the bellows' "
       "air is blown into the charcoal. Made by crafting.")
casing('bellows_block', 'Bellows', "Leather bellows on a wooden frame, pumped in turn so the blast never stops. Made by crafting.")
casing('survey_gantry_frame', 'Survey Gantry Frame', "Scaffold tube and rail of a camera gantry, braced against the wind. Made "
       "by crafting.")
casing('camera_rig', 'Camera Rig', "A stereo pair of calibrated cameras on a sliding bar, looking straight down. Made by crafting.")
casing('datum_plinth', 'Datum Plinth', "A concrete block with a brass survey point set in its top: every measurement of the "
       "dig is taken from it. Made by crafting.")

item('ancient_potsherd', 'Ancient Potsherd', "A broken piece of a fired clay pot, its outer face burnished and painted with a "
     "band of dark lines. The quartz in its clay has kept count of the years since it was fired.", FOUND_TELL)
item('burnt_flint', 'Burnt Flint', "A flint blade that fell into a hearth: crazed and reddened by the heat, which reset its "
     "luminescence clock.", FOUND_ASH)
item('corroded_artefact', 'Corroded Artefact', "A lump of rust in the shape of something: a blade, a fitting, a tool. Inside "
     "there is still metal, and salt that will split it apart unless it is drawn out.", FOUND_TELL)
item('charred_beam', 'Charred Beam', "The end of a roof beam that burnt when the town did. Charring kept its rings, and the last "
     "of them is the year the tree was felled.", FOUND_ASH)
item('slag_cake', 'Slag Cake', "A heavy, glassy bun of iron slag from the floor of an ancient bloomery, bubbled and run. It "
     "still carries the smiths' iron, and the recipe they used.", FOUND_ASH)
item('dated_potsherd', 'Dated Potsherd', "A potsherd with a luminescence date: so many thousand years since it was fired, give "
     "or take a few centuries. Made in the Luminescence Reader.")
item('tree_ring_chronology', 'Tree-Ring Chronology', "A beam cross-dated to the master chronology: the year each ring grew, "
     "and the year the tree was cut. Made in the Tree-Ring Correlator.")
item('stabilised_artefact', 'Stabilised Artefact', "The corroded find, its chloride drawn out, dried and sealed in wax. It will "
     "not rust any further. Made in the Conservation Bath.")
item('provenanced_artefact', 'Provenanced Artefact', "A stabilised find with an isotope signature that says where its metal was "
     "mined. Made in the Isotope Provenancer.")
item('bloomery_steel_ingot', 'Bloomery Steel', "A bar folded and hammered from a bloom: soft iron at the heart, hard steel at "
     "the skin, with the grain of the folds showing. Made in the Bloomery Shaft.")
item('excavation_record', 'Excavation Record', "A whole dig in three dimensions: every layer in order, every find dated and "
     "placed. Stratigraphy is time written in the ground in one direction only, so this is a record of the arrow of time. Made "
     "in the Photogrammetry Gantry.")
item('survey_radar', 'Survey Radar', "A ground-penetrating radar on a handle. Use it to send a pulse into the ground: every "
     "buried find within 16 blocks (in Buried Tells and dig sites) glows where its echo came from, and the nearest is reported "
     "with its distance, direction and depth. A pulse every three seconds. Made in the Radar Survey Bench.")
L['message.%s.radar_quiet' % NS] = 'No echoes: nothing buried within 16 blocks.'
L['message.%s.radar_echo' % NS] = '%s echoes. Nearest %s m away, %s, %s m down.'
L['message.%s.radar_below' % NS] = 'straight below'
for k, v in (('n', 'north'), ('ne', 'north-east'), ('e', 'east'), ('se', 'south-east'), ('s', 'south'), ('sw', 'south-west'), ('w', 'west'), ('nw', 'north-west')):
    L['message.%s.radar_dir.%s' % (NS, k)] = v

for pid, n, outer, inner in [('fired_clay_shaft', 16, 'minecraft:terracotta', 'minecraft:clay_ball'), ('tuyere_block', 4, 'minecraft:terracotta', 'minecraft:blaze_powder'),
                             ('bellows_block', 4, 'minecraft:oak_planks', 'minecraft:leather'), ('survey_gantry_frame', 16, 'minecraft:iron_ingot', 'bloomery_steel_ingot'),
                             ('camera_rig', 4, 'minecraft:iron_ingot', 'fused_silica_lens'), ('datum_plinth', 8, 'minecraft:smooth_stone', 'minecraft:copper_ingot')]:
    dump(os.path.join(D, 'recipe/crafting', 'r160_%s.json' % pid), extreme(pid, n, P_PART, {'O': outer, 'I': inner}))

dump(os.path.join(D, 'recipe/crafting', 'r160_tell_soil.json'),
     {'type': 'minecraft:crafting_shaped', 'pattern': ['DDD', 'DBD', 'DDD'],
      'key': {'D': {'item': 'minecraft:dirt'}, 'B': {'item': 'minecraft:brick'}}, 'result': {'id': ts('tell_soil'), 'count': 8}})
L['block.%s.tell_soil.found' % NS] = 'Found in Buried Tells; also made by crafting (dirt round a brick).'

# ====================================================================== machine recipes
mrec('radar_survey', 'r160_survey_radar', [('minecraft:copper_ingot', 4), ('ferrite_magnet', 1), ('minecraft:compass', 1)], [], [('survey_radar', 1)], [],
     256, 300, 'ground_penetrating_radar', {'er': 9}, [('res', 0, 0.1), ('reach', 8, 100)], 'radar_survey_bench')
mrec('luminescence_dating', 'r160_dated_potsherd', [('ancient_potsherd', 2)], [], [('dated_potsherd', 1)], [], 128, 300, 'luminescence_dating',
     {'De': 12, 'Dr': 3}, [('Rec', 0.85, 1), ('Unc', 0, 0.1)], 'luminescence_reader')
mrec('luminescence_dating', 'r160_dated_flint', [('burnt_flint', 2)], [], [('dated_potsherd', 1)], [], 128, 300, 'luminescence_dating',
     {'De': 30, 'Dr': 2.5}, [('Rec', 0.85, 1), ('Unc', 0, 0.08)], 'luminescence_reader')
mrec('cross_dating', 'r160_tree_ring_chronology', [('charred_beam', 3)], [], [('tree_ring_chronology', 1)], [], 128, 300, 'dendrochronology',
     {'L0': 17, 'rho': 0.7}, [('m', 0.9, 1), ('t', 6, 1000)], 'tree_ring_correlator')
mrec('conservation', 'r160_stabilised_artefact', [('corroded_artefact', 1), ('minecraft:honeycomb', 1)], [], [('stabilised_artefact', 1), ('rust_flakes', 2)], [],
     256, 400, 'conservation_science', {'C0': 3000, 'Lt': 8}, [('C', 0, 100)], 'conservation_bath')
mrec('provenancing', 'r160_provenanced_artefact', [('stabilised_artefact', 1), ('minecraft:quartz', 1)], [], [('provenanced_artefact', 1)], [],
     512, 400, 'isotope_provenance', {'R': 0.7095, 'Ra': 0.712, 'Rb': 0.708}, [('sep', 25, 100000)], 'isotope_provenancer')
mrec('bloomery_smelting', 'r160_bloomery_steel', [('slag_cake', 1), ('minecraft:raw_iron', 2), ('minecraft:charcoal', 4)], [], [('bloomery_steel_ingot', 6)], [],
     1024, 600, 'archaeometallurgy', None, [('x', 2, 8), ('Cw', 0.5, 1.2)], 'bloomery_shaft')
mrec('photogrammetry', 'r160_excavation_record', [('dated_potsherd', 2), ('tree_ring_chronology', 1), ('provenanced_artefact', 1), ('minecraft:paper', 8)], [],
     [('excavation_record', 1)], [], 4096, 800, 'photogrammetry', {'Z0': 4}, [('sz', 0, 2), ('gsd', 0, 0.3)], 'photogrammetry_gantry')
# newer, cheaper routes elsewhere: I-beams of bloomery steel; Arrow of Time Shards from the stratigraphic record
mrec('load_testing', 'r160_rated_i_beam_bloomery', [('bloomery_steel_ingot', 1)], [], [('rated_i_beam', 6)], [], 128, 200, 'archaeometallurgy',
     {'Pk': 500, 'Eg': 210}, [('SF', 2.0, 4.0)], 'load_test_rig')
verify_recipes()
dump(os.path.join(D, 'recipe/chronoweaving', 'r160_arrow_of_time_shard_record.json'),
     {'type': ts('chronoweaving'),
      'item_inputs': [{'item': ts('excavation_record'), 'count': 2}, {'item': ts('chronon_capacitor'), 'count': 8},
                      {'item': ts('zeolite_catalyst'), 'count': 16}],
      'fluid_inputs': [{'fluid': ts('chronon_gas'), 'amount': 8000}],
      'item_outputs': [{'id': ts('arrow_of_time_shard'), 'count': 2}],
      'energy': 8192, 'duration': 600, 'research': ts('photogrammetry'), 'scaled': 8, 'expanded': True})

# ====================================================================== loot: the finds, and the excavators' cache
def entry(i, w, lo=1, hi=1):
    e = {'type': 'minecraft:item', 'name': ts(i), 'weight': w}
    if hi > 1:
        e['functions'] = [{'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': lo, 'max': hi}}]
    return e


dump(os.path.join(D, 'loot_table/archaeology/tell_soil.json'),
     {'type': 'minecraft:archaeology', 'pools': [{'rolls': 1, 'entries': [
         entry('ancient_potsherd', 40), entry('corroded_artefact', 22), entry('charred_beam', 12), entry('burnt_flint', 12),
         entry('minecraft:brick', 8), entry('fossil_fragment', 4), entry('minecraft:emerald', 2)]}]})
dump(os.path.join(D, 'loot_table/archaeology/ash_layer.json'),
     {'type': 'minecraft:archaeology', 'pools': [{'rolls': 1, 'entries': [
         entry('charred_beam', 34), entry('burnt_flint', 26), entry('slag_cake', 26), entry('ancient_potsherd', 10),
         entry('minecraft:charcoal', 4)]}]})
dump(os.path.join(D, 'loot_table/chests/buried_tell.json'),
     {'type': 'minecraft:chest', 'pools': [
         {'rolls': {'type': 'minecraft:uniform', 'min': 4, 'max': 7}, 'entries': [
             entry('ancient_potsherd', 30, 1, 4), entry('charred_beam', 18, 1, 3), entry('slag_cake', 14, 1, 2), entry('burnt_flint', 14, 1, 3),
             entry('corroded_artefact', 10), entry('minecraft:paper', 12, 2, 6), entry('minecraft:string', 8, 2, 5)]},
         {'rolls': 1, 'entries': [entry('minecraft:brush', 3), {'type': 'minecraft:empty', 'weight': 2}]}]})

# ====================================================================== world generation
dump(os.path.join(D, 'worldgen/configured_feature/buried_tell.json'), {'type': ts('buried_tell'), 'config': {}})
dump(os.path.join(D, 'worldgen/placed_feature/buried_tell.json'),
     {'feature': ts('buried_tell'), 'placement': [{'type': 'minecraft:rarity_filter', 'chance': 260}, {'type': 'minecraft:in_square'},
                                                  {'type': 'minecraft:heightmap', 'heightmap': 'WORLD_SURFACE_WG'}, {'type': 'minecraft:biome'}]})
dump(os.path.join(D, 'neoforge/biome_modifier/overworld_buried_tells.json'),
     {'type': 'neoforge:add_features', 'biomes': ['minecraft:plains', 'minecraft:sunflower_plains', 'minecraft:savanna', 'minecraft:savanna_plateau',
                                                 'minecraft:desert', 'minecraft:badlands', 'minecraft:wooded_badlands', 'minecraft:meadow'],
      'features': ts('buried_tell'), 'step': 'surface_structures'})

# ====================================================================== tags
CAS = ['fired_clay_shaft', 'tuyere_block', 'bellows_block', 'survey_gantry_frame', 'camera_rig', 'datum_plinth']
CON = ['radar_survey_bench', 'luminescence_reader', 'tree_ring_correlator', 'conservation_bath', 'isotope_provenancer']
CTL = ['bloomery_shaft', 'photogrammetry_gantry']
tag_add(os.path.join(MC, 'tags/block/mineable/pickaxe.json'), [ts(x) for x in CAS + CON + CTL + ['ancient_mud_brick']])
tag_add(os.path.join(MC, 'tags/block/needs_iron_tool.json'), [ts(x) for x in CAS + CON + CTL])
tag_add(os.path.join(MC, 'tags/block/mineable/shovel.json'), [ts('tell_soil'), ts('ash_layer')])
tag_add(os.path.join(D, 'tags/block/survey_targets.json'), [ts('suspicious_tell_soil'), ts('suspicious_ash_layer'), ts('fossil_ore'), ts('amber_ore'),
                                                           'minecraft:suspicious_sand', 'minecraft:suspicious_gravel'])
for kind in ('block', 'item'):
    tag_add(os.path.join(D, 'tags', kind, 'casings.json'), [ts(x) for x in CAS])
    tag_add(os.path.join(D, 'tags', kind, 'machines.json'), [ts(x) for x in CON + CTL])
    tag_add(os.path.join(D, 'tags', kind, 'multiblock_controllers.json'), [ts(x) for x in CTL])
    tag_add(os.path.join(D, 'tags', kind, 'decorative_blocks.json'), [ts('tell_soil'), ts('ash_layer'), ts('ancient_mud_brick')])

# ====================================================================== multiblocks
# --- Bloomery Shaft: a tapering clay shaft on a broad foot, open at the top, a ring of tuyeres low down with a bellows
#     behind each, a tapping arch at the front where the bloom is drawn; the bellows pump while it works
H = 16
W = 13
g = grid(W, H, W)
c = W // 2
for y in range(H):
    r = 5.6 - y * 0.24                                                     # the shaft narrows as it rises
    for z in range(W):
        for x in range(W):
            rr = math.hypot(x - c, z - c)
            if y == 0:
                if rr <= 6.2:
                    g[y][z][x] = 'S'
            elif r - 1.0 < rr <= r and y < H - 1:
                g[y][z][x] = 'S'
            elif y == H - 1 and r - 1.0 < rr <= r + 0.4:
                g[y][z][x] = 'S'                                           # the charging mouth's lip
for k in range(6):                                                         # tuyeres at y 3, with bellows outside them
    a = k * math.tau / 6
    r = 5.6 - 3 * 0.24
    tx, tz = c + round(math.cos(a) * (r - 0.5)), c + round(math.sin(a) * (r - 0.5))
    g[3][tz][tx] = 'T'
    bx, bz = c + round(math.cos(a) * (r + 1.2)), c + round(math.sin(a) * (r + 1.2))
    for yy in (2, 3):
        if 0 <= bx < W and 0 <= bz < W:
            g[yy][bz][bx] = 'B'
front = min(z for z in range(W) if g[1][z][c] == 'S')
for yy in (1, 2):                                                          # the tapping arch
    for xx in (c - 1, c, c + 1):
        g[yy][front][xx] = ' '
g[3][front][c] = 'S'
g[0][front][c - 2] = 'P'
g[0][front][c + 2] = 'P'
g[0][front][c] = 'C'
T.MB_COUNTS['bloomery_shaft'] = write_mb('bloomery_shaft', rows(g),
                                         {'S': ['fired_clay_shaft'], 'T': ['tuyere_block'], 'B': ['bellows_block'], 'P': ['fired_clay_shaft'] + PORT},
                                         extra={'effect_center': [c, 4, c], 'effect_scale': 2.0,
                                                'moving': [{'id': 'bellows', 'chars': 'B', 'pivot': 'self', 'ramp_seconds': 1,
                                                            'split': 'components', 'stagger': 0.17,
                                                            'motion': [{'type': 'oscillate', 'mode': 'move', 'axis': 'up', 'amplitude': 0.18, 'period_seconds': 1.2}]}],
                                                'fx': [{'type': 'glow', 'chars': 'T', 'color': '#FF7030', 'hot': '#FFE080'},
                                                       {'type': 'inflow', 'radius': 2.0, 'color': '#FF9040', 'waves': 3, 'period': 1.5}]})

# --- Photogrammetry Gantry: a stepped trench cut into a long platform, rails along both sides, a bridge spanning the
#     trench carrying the camera rig, datum plinths at its corners; the bridge runs up and down the trench while it works
Lz = 25
Wx = 13
H = 9
g = grid(Wx, H, Lz)
cx = Wx // 2
for z in range(Lz):
    for x in range(Wx):
        dxa = abs(x - cx)
        top = 3 if dxa > 4 else 2 if dxa > 3 else 1                        # the trench, stepped to show the layers
        for y in range(top):
            g[y][z][x] = 'S'
        if dxa == 5:
            if z % 4 == 0:
                g[3][z][x] = 'F'                                            # rail posts
                g[4][z][x] = 'F'
            g[5][z][x] = 'F'                                                # the rails
for x in range(cx - 5, cx + 6):                                            # the bridge across the trench, cameras under it
    g[6][Lz // 2][x] = 'F'
g[5][Lz // 2][cx - 1] = 'R'
g[5][Lz // 2][cx + 1] = 'R'
for (x, z) in ((0, 0), (Wx - 1, 0), (0, Lz - 1), (Wx - 1, Lz - 1)):
    g[3][z][x] = 'D'
g[1][0][cx - 6] = 'P'                                                      # in the platform's front face
g[1][0][cx + 6] = 'P'
g[2][0][cx + 6] = 'C'
T.MB_COUNTS['photogrammetry_gantry'] = write_mb('photogrammetry_gantry', rows(g),
                                                {'S': ['tell_soil'], 'M': ['datum_plinth'], 'F': ['survey_gantry_frame'], 'R': ['camera_rig'],
                                                 'D': ['datum_plinth'], 'P': ['survey_gantry_frame'] + PORT},
                                                extra={'effect_center': [cx, 2, Lz // 2], 'effect_scale': 2.5,
                                                       'moving': [{'id': 'bridge', 'chars': 'R', 'split': 'none', 'pivot': 'centre', 'ramp_seconds': 2,
                                                                   'motion': [{'type': 'oscillate', 'mode': 'move', 'axis': 'back', 'amplitude': 9.0, 'period_seconds': 14.0}]}],
                                                       'fx': [{'type': 'glow', 'chars': 'R', 'color': '#FFF0C0', 'hot': '#FFFFFF'},
                                                              {'type': 'beam', 'start_back': Lz // 2, 'end_back': Lz // 2 + 1, 'up': 1, 'width': 0.1, 'color': '#FFF8E0'}]})
print('multiblock blocks:', {k: T.MB_COUNTS[k] for k in CTL})

# ====================================================================== the Codex
E = {}
RSR = {'ground_penetrating_radar': 'radar_survey_bench', 'luminescence_dating': 'luminescence_reader', 'dendrochronology': 'tree_ring_correlator',
       'conservation_science': 'conservation_bath', 'isotope_provenance': 'isotope_provenancer', 'archaeometallurgy': 'bloomery_shaft',
       'photogrammetry': 'photogrammetry_gantry'}
E['chemistry/buried_tells'] = {'title': 'Buried Tells', 'icon': ts('suspicious_tell_soil'), 'category': 'chemistry', 'research': 'ground_penetrating_radar', 'pages': [
    text("A $(thing)Buried Tell$() is a low mound of soil in the plains, savannas, deserts and badlands of the Overworld: a town rebuilt on its own ruins for thousands of years. Grey $(thing)ash layers$() mark the nights its towns burnt, and the stubs of $(thing)mud-brick walls$() stand in every layer. A trial trench cuts into one flank."),
    spot(['suspicious_tell_soil', 'suspicious_ash_layer', 'survey_radar'], "Brush $(item)Suspicious Tell Soil$() for potsherds, corroded artefacts and flints, and $(item)Suspicious Ash$() for charred beams and slag. Dig them instead and the find is lost. Most lie hidden in the mound: the $(item)Survey Radar$() shows where.", 'The finds')]}
E['chemistry/ground_penetrating_radar'] = {'title': 'Ground-Penetrating Radar', 'icon': ts('survey_radar'), 'category': 'chemistry', 'research': 'ground_penetrating_radar', 'pages': [
    spot(['radar_survey_bench', 'survey_radar'], "d = c·t/(2√εr). The $(thing)Radar Survey Bench$() tunes a $(item)Survey Radar$(): use it and every buried find within 16 blocks glows where its echo came from, the nearest reported by distance, direction and depth.", 'Radar Survey Bench')]}
E['chemistry/luminescence_dating'] = {'title': 'Luminescence Dating', 'icon': ts('dated_potsherd'), 'category': 'chemistry', 'research': 'luminescence_dating', 'pages': [
    spot(['luminescence_reader', 'dated_potsherd'], "A = De/Ḋ. The $(thing)Luminescence Reader$() dates $(item)Ancient Potsherds$() and $(item)Burnt Flint$() into $(item)Dated Potsherds$().", 'Luminescence Reader')]}
E['chemistry/dendrochronology'] = {'title': 'Dendrochronology', 'icon': ts('tree_ring_chronology'), 'category': 'chemistry', 'research': 'dendrochronology', 'pages': [
    spot(['tree_ring_correlator', 'tree_ring_chronology'], "t = r·√((n − 2)/(1 − r²)). The $(thing)Tree-Ring Correlator$() cross-dates $(item)Charred Beams$() into a $(item)Tree-Ring Chronology$().", 'Tree-Ring Correlator')]}
E['chemistry/conservation_science'] = {'title': 'Conservation Science', 'icon': ts('stabilised_artefact'), 'category': 'chemistry', 'research': 'conservation_science', 'pages': [
    spot(['conservation_bath', 'stabilised_artefact'], "C = C₀·e^(−π²Dt/L²). The $(thing)Conservation Bath$() draws the chloride out of a $(item)Corroded Artefact$(): a $(item)Stabilised Artefact$() and some rust flakes.", 'Conservation Bath')]}
E['chemistry/isotope_provenance'] = {'title': 'Isotope Provenance', 'icon': ts('provenanced_artefact'), 'category': 'chemistry', 'research': 'isotope_provenance', 'pages': [
    spot(['isotope_provenancer', 'provenanced_artefact'], "f = (R − Rb)/(Ra − Rb). The $(thing)Isotope Provenancer$() reads where a $(item)Stabilised Artefact$()'s metal was mined: a $(item)Provenanced Artefact$().", 'Isotope Provenancer')]}
E['chemistry/archaeometallurgy'] = {'title': 'Archaeometallurgy', 'icon': ts('bloomery_steel_ingot'), 'category': 'chemistry', 'research': 'archaeometallurgy', 'pages': [
    spot(['bloomery_shaft', 'bloomery_steel_ingot'], "x = 2√(Dt). The $(thing)Bloomery Shaft$() is %d blocks: a tapering clay shaft, its bellows pumping air through six tuyeres. From $(item)Slag Cakes$(), raw iron and charcoal it makes $(item)Bloomery Steel$(), which the Load Test Rig rolls into I-beams more cheaply than iron." % T.MB_COUNTS['bloomery_shaft'], 'Bloomery Shaft'),
    {'type': 'multiblock', 'name': 'Bloomery Shaft', 'text': 'Interface Ports either side of the tapping arch.', 'multiblock': 'bloomery_shaft'}]}
E['chemistry/photogrammetry'] = {'title': 'Photogrammetry', 'icon': ts('excavation_record'), 'category': 'chemistry', 'research': 'photogrammetry', 'pages': [
    spot(['photogrammetry_gantry', 'excavation_record'], "Z = f·B/d. The $(thing)Photogrammetry Gantry$() is %d blocks: a camera bridge on rails over a stepped trench. It brings the dated, provenanced finds together into an $(item)Excavation Record$(). In the Chrono Loom, two records make Arrow of Time Shards without negentropy cells or imaginary units." % T.MB_COUNTS['photogrammetry_gantry'], 'Photogrammetry Gantry'),
    {'type': 'multiblock', 'name': 'Photogrammetry Gantry', 'text': 'Interface Ports in the front face of the platform, under the controller and at the other corner.', 'multiblock': 'photogrammetry_gantry'}]}
OPS = {
    'radar_survey_bench': ("A Survey Radar needs to see a pot (a tenth of a metre) at least eight metres down. Higher frequencies see finer, lower ones deeper; more power reaches further.",
                           'Antenna frequency, Pulse power', 'damp: resolution varies by ±1%·d.', 'over 80 W the transmitter overheats (wear).', 'higher frequencies faster.',
                           ['v = 0.3/√εr m/ns', 'Resolution = λ/4 = 75/(f√εr) m', 'Reach = 30·√(P/10)/(1 + f/400) m']),
    'luminescence_reader': ("Preheat to clear the unstable traps but not the ones that keep time, and light the grains bright enough to read the dose precisely.",
                            'Preheat, Stimulation', 'background: recovery varies by ±1%·d.', 'over 70 mW/cm² bleaches the reference (wear).', 'brighter faster.',
                            ['Recovery = (1 − e^(−(T − 160)/40))·e^(−(T − 260)⁺/30)', 'Uncertainty = 0.03 + 2/S', 'Age = De/Ḋ']),
    'tree_ring_correlator': ("Slide the beam to the offset where the patterns match, detrend gently, and compare enough rings for t to pass 6.",
                             'Overlap, Offset, Detrending', 'charring: correlation varies by ±1%·d.', 'none.', 'shorter overlaps faster.',
                             ["Match = e^(−((lag − L₀)/2)²)", 'r = ρ·match·(1 − e^(−s/30))·e^(−s/600)', 't = r·√((n − 2)/(1 − r²))']),
    'conservation_bath': ("Soak long enough, warm and alkaline enough, to bring the chloride under 100 ppm; but not so hot or strong that the bath attacks the find.",
                          'Alkali, Temperature, Soak', 'old salts: chloride varies by ±2%·d.', 'over 1.6% alkali or 60 °C wears the bath.', 'shorter soaks faster.',
                          ['D = 0.3·(1 + c)·e^((T − 20)/30) mm²/day', 'C = C₀·e^(−π²Dt/L²)']),
    'isotope_provenancer': ("Run the plasma near 1.25 kW and count long enough that the two sources lie more than 25 σ apart.",
                            'Plasma power, Acquisition', 'drift: precision varies by ±1%·d.', 'over 1.55 kW erodes the cones (wear).', 'shorter acquisitions faster.',
                            ['σ = 0.0002·√(60/t)·(1 + 3|P − 1.25|)', 'f = (R − Rb)/(Ra − Rb)', 'Separation = (Ra − Rb)/σ']),
    'bloomery_shaft': ("Hot enough for the carbon to soak in two millimetres or more, never so hot the bloom melts, with charcoal enough for steel.",
                       'Furnace heat, Charcoal, Smelt', 'draught: depth varies by ±1%·d.', 'over 1450 °C the bloom melts (stops); over 2.8 charcoal per ore chokes (wear).',
                       'shorter smelts faster.', ['D = 2.3×10⁻⁵·e^(−148 kJ/RT) m²/s', 'x = 2√(Dt)', 'Carbon = 0.9·(1 − e^(−x/2))·min(1.2, c/1.5) %']),
    'photogrammetry_gantry': ("Four metres above the trench, choose a lens long enough for 0.3 mm pixels and a baseline wide enough for 2 mm depth, but not so wide the views stop matching.",
                              'Baseline, Focal length, Overlap', 'vibration: precision varies by ±2%·d.', 'base to height over 0.8 wears the matcher.',
                              'less overlap faster.', ['Pixel = 4·Z/f mm', 'σZ = Z²·σd/(f·B)', 'B/Z between 0.1 and 0.8']),
}
rmap = {v: k for k, v in RSR.items()}
for mid, (intro, sets, dist, lim, rate, eqs) in OPS.items():
    E['operation/' + mid] = {'title': T.MACHINES[mid]['name'], 'icon': ts(mid), 'category': 'operation', 'research': rmap[mid],
                             'pages': op_pages(mid, intro, sets, dist, lim, rate, eqs)}
NODES = {
    'chemistry': [{'entry': 'chemistry/buried_tells', 'x': 3, 'y': 61, 'parents': []},
                  {'entry': 'chemistry/ground_penetrating_radar', 'x': 5, 'y': 61, 'parents': ['chemistry/buried_tells']},
                  {'entry': 'chemistry/luminescence_dating', 'x': 7, 'y': 61, 'parents': ['chemistry/buried_tells']},
                  {'entry': 'chemistry/dendrochronology', 'x': 7, 'y': 63, 'parents': ['chemistry/buried_tells']},
                  {'entry': 'chemistry/conservation_science', 'x': 5, 'y': 63, 'parents': ['chemistry/buried_tells']},
                  {'entry': 'chemistry/isotope_provenance', 'x': 9, 'y': 63, 'parents': ['chemistry/conservation_science']},
                  {'entry': 'chemistry/archaeometallurgy', 'x': 9, 'y': 61, 'parents': ['chemistry/conservation_science']},
                  {'entry': 'chemistry/photogrammetry', 'x': 11, 'y': 62, 'parents': ['chemistry/luminescence_dating', 'chemistry/dendrochronology',
                                                                                        'chemistry/isotope_provenance']}],
    'machines': [{'entry': 'operation/' + m, 'x': 28 + 2 * (i % 6), 'y': 16 + 2 * (i // 6), 'parents': []} for i, m in enumerate(OPS)],
}
codex(E, NODES)
regions('chemistry', 'Archaeology', 60, 64.5)

finish()
print('research', len(T.RESEARCH), 'machines', len(T.MACHINES), 'recipes', len(T.RECIPES), 'lang', len(L))
