"""
Round 49 (1.8.0, "Fields"): Electromagnetism, Tier III.

* Cyclotron (Lawrence, 1931): protons circle between two copper dees in a magnet's gap, kicked across the gap at every
  half turn by an RF voltage. They stay in step only if the RF matches their cyclotron frequency, f = qB/2πm - and as
  they gain energy their mass grows (γ), so they fall slowly out of phase: the classical cyclotron cannot go much past
  ten MeV. Extracted at radius R the beam has E = (qBR)²/2m. Protons on iron make cobalt-57 (⁵⁷Fe(p,n)⁵⁷Co).
* Tesla Coil: a resonant transformer. The primary circuit (capacitor and a few turns) must ring at the secondary's own
  frequency for energy to slosh across; the voltage steps up as √(C₁/C₂). Its arcs fix the air's nitrogen
  (Birkeland-Eyde) into fertiliser and fuse sand into glass, as lightning makes fulgurites. Those near it get struck.
"""
import json
import math
import os

import shapes
import round12 as _r12
from round12 import P as _P, D as _D, C as _C, V as _V

NS = 'theoreticalsciences'
HERE = os.path.dirname(os.path.abspath(__file__))


def _B(i, n, art, c1, c2, kind='casing', light=2, hardness=5.0, tool='iron', **kw):
    d = dict(id=i, name=n, art=art, c1=c1, c2=c2, kind=kind, light=light, hardness=hardness, tool=tool)
    d.update(kw)
    return d


ITEMS = []
BLOCKS = [
    _B('cyclotron_yoke', 'Cyclotron Yoke', 'magcasing', '#4a4c50', '#9a9ca0', light=0),
    _B('cyclotron_dee', 'Cyclotron Dee', 'coilblock', '#8a5030', '#ffb070', light=3),
    _B('rf_oscillator', 'RF Oscillator', 'injector', '#6a3020', '#ff8040', light=4),
    _B('tesla_primary', 'Tesla Primary Coil', 'coilblock', '#8a5030', '#ffa060', light=0),
    _B('tesla_secondary', 'Tesla Secondary Coil', 'beampipe', '#7a4a28', '#e0c080', light=0),
    _B('tesla_toroid', 'Tesla Toroid', 'casing2', '#a0a4a8', '#e8ecf0', light=2),
    _B('capacitor_bank', 'Capacitor Bank', 'vent', '#303438', '#80c0ff', light=2),
]
FLUIDS = []

KEY_ADD = {
    'Б': ['cyclotron_yoke'], 'Г': ['cyclotron_yoke', 'interface_port'], 'Д': ['cyclotron_dee'], 'Ж': ['rf_oscillator'],
    'П': ['tesla_primary'], 'Ф': ['tesla_secondary'], 'Ш': ['tesla_toroid'], 'Э': ['capacitor_bank'], 'Я': ['capacitor_bank', 'interface_port'],
}


# =================================================================================== SHAPES
def cyclotron():
    """A squat electromagnet: round pole pieces above and below, coils round them, the two D-shaped copper dees in the
    gap with a straight slit between them, four return-yoke posts, and the RF oscillator at the side."""
    s = shapes.Shape(11, 5, 11)
    c = 5
    for y in (0, 4):
        s.disc(c, c, y, 4.6, 'Б')                                         # pole pieces
    for y in (1, 3):
        s.disc(c, c, y, 4.6, ':', r_in=3.4)                               # the magnet's coils (MHD field magnets)
        s.disc(c, c, y, 3.4, 'Б')
    for x in range(11):
        for z in range(11):
            r = math.hypot(x - c, z - c)
            if r <= 4.2:
                s.put(x, 2, z, '.' if x == c else 'Д')                    # the dees, with the accelerating gap between them
    for x, z in ((0, 0), (10, 0), (0, 10), (10, 10)):
        s.column(x, z, 0, 4, 'Б')                                         # return-yoke posts
        for y in (0, 4):
            s.line((x, y, z), (5 + (2 if x else -2), y, 5 + (2 if z else -2)), 'Б')   # and the arms joining them to the poles
    s.put(10, 2, 5, 'Ж'); s.put(9, 2, 5, 'Ж')                             # the RF oscillator feeding the dees
    s.center = (c, 2.0, c)
    s.ports('Б', 'Г', 4, y=0)
    s.v[(c, 1, 0)] = 'C'
    s.ctrl = (c, 1, 0)
    return s


def tesla_coil():
    """A capacitor bank and primary spiral at the foot, the tall secondary winding rising from it, and the toroid on
    top that the arcs leap from."""
    s = shapes.Shape(7, 12, 7)
    c = 3
    s.box(0, 0, 0, 6, 0, 6, 'Э')
    s.disc(c, c, 1, 3.2, 'П', r_in=1.6)                                   # the flat primary spiral
    for y in range(1, 10):
        s.put(c, y, c, 'Ф')                                               # the secondary
    for x in range(7):
        for z in range(7):
            r = math.hypot(x - c, z - c)
            if 1.5 <= r <= 3.2:
                s.put(x, 10, z, 'Ш')                                      # the toroid
    s.put(c, 10, c, 'Ф'); s.put(c, 11, c, 'Ш')
    for x, z in ((0, 0), (6, 6)):
        s.column(x, z, 1, 2, 'Э')                                         # tank capacitors
    s.center = (c, 10.0, c)
    s.ports('Э', 'Я', 4, y=0)
    s.v[(c, 1, 0)] = 'C'
    s.ctrl = (c, 1, 0)
    return s


shapes.BUILDERS.update({'cyclotron': cyclotron, 'tesla_coil': tesla_coil})

MACHINE_TEXT = {
    'cyclotron': ('Cyclotron', 'Protons spiralling outward between two copper dees, kicked across the gap twice a turn by a radio-frequency '
                               'voltage that must keep exact step with them.'),
    'tesla_coil': ('Tesla Coil', 'A resonant transformer: a spark-gap primary ringing in tune with a tall secondary steps a few kilovolts up to '
                                 'millions, and the toroid throws arcs metres long.'),
}
MACHINE_CAT = {k: 'electromagnetism' for k in MACHINE_TEXT}
MACHINE_RESEARCH = {'cyclotron': 'cyclotron_physics', 'tesla_coil': 'resonant_circuits'}
TIER = {k: 2 for k in MACHINE_TEXT}
_S = 'spacetime_casing'
MC = {
    'cyclotron': (['YMY', 'DSD', 'YRY'], {'Y': 'cyclotron_yoke', 'M': 'mhd_field_magnet', 'D': 'cyclotron_dee', 'S': _S, 'R': 'rf_oscillator'}),
    'tesla_coil': (['TTT', 'PSP', 'CCC'], {'T': 'tesla_toroid', 'P': 'tesla_primary', 'S': _S, 'C': 'capacitor_bank'}),
}
EXTRA = {
    'cyclotron': 'Set the field, then tune the RF to the protons\' cyclotron frequency, f = qB/2πm (15.25 MHz per tesla). The beam reaches '
                 'E = (qBR)²/2m at the extraction radius; as it gains energy its mass grows and it slips out of phase - keep the slip under 90°. '
                 'Above 2.2 T the iron saturates. Beam power heats the target.',
    'tesla_coil': 'Tune the primary - its capacitance and the turns tapped - to ring at the secondary\'s 159 kHz. In tune, the voltage steps up as '
                  '√(C₁/C₂) and the arcs grow a metre for every 700 kV. Stand clear: anyone within six blocks of a running coil gets struck.',
}

# =================================================================================== PHYSICS
_r12.model('cyclotron', 'Cyclotron',
           'A proton in a field B circles at f = qB/2πm, whatever its speed - so an RF voltage at that frequency kicks it across the dees\' gap at '
           'every half turn. Each turn adds energy and radius; at the edge, R, it has E = (qBR)²/2m. But its mass grows with energy (γ = 1 + E/mc²), '
           'its frequency falls, and over its turns it slips out of phase with the RF. Past about 90° the gap decelerates it.',
           'Field ripple: B varies by ±0.05%·d.',
           [_P('B', 'Magnetic field B', 'T', 0, 3, 0.001, 1, 'Field between the pole pieces. Iron saturates above about 2.2 T.'),
            _P('frf', 'RF frequency', 'MHz', 1, 60, 0.001, 15, 'Frequency of the voltage on the dees. It must match the protons\' cyclotron frequency.'),
            _P('I', 'Beam current', 'µA', 1, 100, 0.1, 20, 'Protons per second. More current, more yield - and more heat in the target.')],
           [_D('Be', 'Effective field', 'T', 'B*(1 + 0.0005*d)', 'B·(1 + δ)', 'The field the protons actually see.'),
            _D('fc', 'Cyclotron frequency', 'MHz', '15.2449*Be', 'f = qB/2πm', 'How often a proton circles, at low energy.'),
            _D('E', 'Beam energy', 'MeV', '47.9*(Be*0.75)^2', 'E = (qBR)²/2m', 'Energy at the extraction radius, R = 0.75 m.'),
            _D('N', 'Turns', '', 'E*10', 'N = E/2qV', 'Turns taken to reach it, at 50 kV across the gap.'),
            _D('slip', 'Phase slip', '°', '360*N*(abs(frf*(1 + E/1876)/fc - 1) + E/3752)', 'Δφ = 360°·N·|f_RF·γ/f_c − 1|', 'How far out of step '
                                                                                                                       'the beam has fallen.'),
            _D('Pb', 'Beam power', 'W', 'E*I', 'P = E·I', 'Power the beam drops into the target.')],
           [_C('phase', 'stop', '90 - slip', 'The beam has slipped out of phase with the RF', 'Δφ < 90°'),
            _C('saturation', 'wear', '2.2 - B', 'The iron yoke is saturating', 'B < 2.2 T'),
            _C('target', 'damage', '2000 - Pb', 'The target is melting under the beam', 'P < 2 kW')],
           rate='clamp(I/30, 0.2, 2)', rate_formula='rate = I / 30 µA', rate_desc='More current makes the isotope faster.',
           heat='Pb/1000', idle_heat='0', rvars=[_V('Eth', 'Energy needed (MeV)', 5)], kappa=40, heat_capacity=60)

_r12.model('tesla_coil', 'Tesla Coil',
           'The secondary - a long winding and its toroid - rings at its own frequency, 1/2π√(L₂C₂) = 159 kHz. The primary, a capacitor and a few '
           'turns, rings at 1/2π√(L₁C₁). Only when the two agree does the energy slosh across to the secondary, where it appears as a voltage '
           'higher by √(C₁/C₂). In air an arc reaches about a metre for every 700 kV.',
           'Spark-gap jitter: the primary\'s frequency wanders by ±0.5%·d.',
           [_P('C1', 'Primary capacitance', 'nF', 1, 200, 0.1, 20, 'The tank capacitor bank.'),
            _P('Np', 'Primary turns tapped', '', 1, 15, 0.1, 8, 'Where on the flat spiral the primary is tapped: its inductance grows as the square.'),
            _P('Vin', 'Charging voltage', 'kV', 5, 30, 0.1, 15, 'The voltage the bank is charged to before the gap fires.')],
           [_D('Lp', 'Primary inductance', 'µH', '0.5*Np^2', 'L₁ ∝ N²', 'The tapped spiral\'s inductance.'),
            _D('fp', 'Primary frequency', 'kHz', '1/(6.2832*sqrt(Lp*1e-6*C1*1e-9))/1000*(1 + 0.005*d)', 'f₁ = 1/2π√(L₁C₁)', 'Where the primary rings.'),
            _D('fs', 'Secondary frequency', 'kHz', '159.15', 'f₂ = 1/2π√(L₂C₂)', 'Where the secondary rings (50 mH, 20 pF).'),
            _D('mis', 'Detuning', '%', '100*abs(fp/fs - 1)', '|f₁/f₂ − 1|', 'How far apart the two are.'),
            _D('Vout', 'Secondary voltage', 'kV', 'Vin*sqrt(C1*1e-9/20e-12)*0.8/(1 + (mis/3)^2)', 'V₂ = η·V₁·√(C₁/C₂)', 'The voltage on the toroid.'),
            _D('arc', 'Arc length', 'm', 'Vout/700', 'ℓ ≈ V/(700 kV/m)', 'How far its sparks reach.')],
           [_C('flashover', 'damage', '5000 - Vout', 'Flashover down the secondary: above 5 MV it arcs along its own winding', 'V < 5 MV')],
           rate='clamp(arc/amin, 0.25, 2)', rate_formula='rate = arc length / arc needed', rate_desc='Longer arcs work the charge faster.',
           heat='0.002*Vin*C1/20', idle_heat='0', rvars=[_V('amin', 'Arc needed (m)', 1)], kappa=40, heat_capacity=40)

_r12.WINDOWS['cyclotron'] = {'r49_cobalt_57': ({'Eth': 5}, [('E', 5, 99)])}
_r12.WINDOWS['tesla_coil'] = {
    'r49_birkeland_eyde': ({'amin': 1.0}, [('arc', 1.0, 99)]),
    'r49_fulgurite': ({'amin': 2.5}, [('arc', 2.5, 99)]),
}

RESEARCH = [
    ('resonant_circuits', 'Resonant Circuits',
     'A capacitor and a coil pass energy back and forth, electric then magnetic, at one frequency of their own. Two such circuits in tune trade it '
     'between them - and one can step the voltage up enormously. Unlocks the Tesla Coil.',
     'tesla_coil', 4, -6, 50, ['electromagnetic_induction'], [('electrical_steel_lamination', 4), ('minecraft:copper_ingot', 16)]),
    ('cyclotron_physics', 'The Cyclotron',
     'A charge in a magnetic field circles at a frequency that does not depend on its speed. Kick it at that frequency and it spirals out, '
     'faster every turn - until relativity puts it out of step. Unlocks the Cyclotron.',
     'cyclotron', 6, -6, 80, ['resonant_circuits', 'radioactivity', 'magnetohydrodynamics'], [('neodymium_magnet', 4), ('radium_salt', 1)]),
]

PUZZLES = {
    'resonant_circuits': dict(
        goal='A capacitor of capacitance C discharges through a coil of inductance L; the current swings back and forth, its energy moving between '
             'the two. Write the frequency f it rings at.',
        name='Resonance of an LC circuit', lhs='f',
        form='1 / ( 2 * π * sqrt ( L * C ) )',
        expr='1 / ( 6.28318 * ( L * C ) ^ 0.5 ) / 1000',
        ask='Find f in kHz for a Tesla secondary of L = 0.05 H and C = 2e-11 F.',
        vars={'f': 'frequency (kHz)', 'L': 'inductance (H)', 'C': 'capacitance (F)', 'π': 'pi'},
        given={'L': 0.05, 'C': 2e-11}, unit='kHz',
        method='Charge obeys L d²q/dt² = −q/C: a harmonic oscillator of angular frequency 1/√(LC).',
        distractors=['R', '4', '+']),
    'cyclotron_physics': dict(
        goal='A charge q of mass m moves across a uniform magnetic field B. The magnetic force bends it into a circle; write how many times a second '
             'it goes round, f.',
        name='Cyclotron frequency', lhs='f',
        form='q * B / ( 2 * π * m )',
        expr='q * B / ( 6.28318 * m ) / 1000000',
        ask='Find f in MHz for a proton (q = 1.602e-19 C, m = 1.6726e-27 kg) in B = 1.5 T.',
        vars={'f': 'frequency (MHz)', 'q': 'charge (C)', 'B': 'field (T)', 'm': 'mass (kg)', 'π': 'pi'},
        given={'q': 1.602e-19, 'B': 1.5, 'm': 1.6726e-27}, unit='MHz',
        method='qvB = mv²/r gives r = mv/qB; the time round, 2πr/v = 2πm/qB, does not depend on the speed.',
        distractors=['v', 'r', '2']),
}

CY, AD = 'cyclotron_irradiation', 'arc_discharge'
RECIPES = [
    ('cobalt_57', CY, [('minecraft:iron_ingot', 2)], [], [('cobalt_57_source', 2)], [], 1024, 800, 'cyclotron_physics', 4, []),
    ('birkeland_eyde', AD, [], [('minecraft:water', 1000)], [('minecraft:bone_meal', 12)], [], 512, 400, 'resonant_circuits', 0, []),
    ('fulgurite', AD, [('minecraft:sand', 16)], [], [('fused_silica', 6)], [], 4096, 200, 'resonant_circuits', 0, []),
]
CRAFTING = [
    ('cyclotron_yoke', ['III', 'IBI', 'III'], {'I': 'minecraft:iron_ingot', 'B': 'minecraft:iron_block'}, 'cyclotron_yoke', 4),
    ('cyclotron_dee', ['CCC', 'C  ', 'CCC'], {'C': 'minecraft:copper_ingot'}, 'cyclotron_dee', 2),
    ('rf_oscillator', ['CRC', 'RQR', 'CRC'], {'C': 'minecraft:copper_ingot', 'R': 'minecraft:redstone', 'Q': 'minecraft:quartz'}, 'rf_oscillator', 1),
    ('tesla_primary', ['CCC', 'C C', 'CCC'], {'C': 'minecraft:copper_ingot'}, 'tesla_primary', 2),
    ('tesla_secondary', ['CWC', 'CWC', 'CWC'], {'C': 'minecraft:copper_ingot', 'W': 'minecraft:stripped_oak_log'}, 'tesla_secondary', 2),
    ('tesla_toroid', ['III', 'I I', 'III'], {'I': 'minecraft:iron_ingot'}, 'tesla_toroid', 4),
    ('capacitor_bank', ['GRG', 'CPC', 'GRG'], {'G': 'minecraft:glass', 'R': 'minecraft:redstone', 'C': 'minecraft:copper_ingot',
                                                'P': 'minecraft:paper'}, 'capacitor_bank', 2),
]
LANG = {
    'message.theoreticalsciences.tesla_strike': 'The Tesla coil\'s arc finds you!',
}


def _write_research(g):
    import round42
    saved = round42.RESEARCH
    round42.RESEARCH = RESEARCH
    try:
        round42._write_research(g)
    finally:
        round42.RESEARCH = saved


def _write_puzzles():
    p = os.path.join(HERE, 'puzzles2', 'group_r49.json')
    data = json.dumps(PUZZLES, ensure_ascii=False, indent=1)
    if not os.path.exists(p) or open(p, encoding='utf-8').read() != data:
        with open(p, 'w', encoding='utf-8') as f:
            f.write(data)


def _recipes(g):
    REC, w = g['REC'], g['w']
    for name, typ, ins, fins, outs, fouts, fe, t, res, insight, cats in RECIPES:
        g['mr']('r49_' + name, typ, ins, fins, outs, fouts, energy=fe, duration=t, research=res, catalysts=cats)
        p = os.path.join(REC, typ, 'r49_' + name + '.json')
        r = json.load(open(p))
        r['unscaled'] = True
        if insight: r['insight_per_run'] = insight
        w(p, r)
    for name, pat, key, res, n in CRAFTING:
        g['shaped']('r49_' + name, pat, key, res, n)


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
    te('electromagnetism', 'r49_cyclotron', 'The Cyclotron', 'cyclotron', [
        'A proton crossing a magnetic field circles, and how often it circles - $(thing)f = qB/2πm$() - does not depend on how fast it goes. '
        'Lawrence saw what that meant: put an RF voltage at that frequency across a gap, and the proton is kicked every half turn, spiralling out '
        'faster and faster in step.',
        'Set the field B, then the RF to 15.25 MHz per tesla. At the edge, 0.75 m out, the beam has $(thing)E = (qBR)²/2m$(). But relativity: '
        'the faster it goes the heavier it is, its frequency drops, and over its hundreds of turns it slips out of phase. Past 90° the gap slows '
        'it instead. Tune a hair low to split the difference - the classical cyclotron runs out at about ten MeV.',
        'Five MeV protons on iron make $(item)Cobalt-57$() (⁵⁷Fe(p,n)⁵⁷Co): the Pound-Rebka source, two at a time, without radium. Above 2.2 T '
        'the iron yoke saturates; more than 2 kW of beam melts the target.'],
        adv='cyclotron_physics', sort=6, spot=['cyclotron', 'cyclotron_dee', 'rf_oscillator'])
    te('electromagnetism', 'r49_tesla', 'The Tesla Coil', 'tesla_coil', [
        'Two circuits, each a capacitor and a coil, each ringing at its own frequency. Tune the primary to the secondary and energy sloshes '
        'across - and appears at a voltage higher by $(thing)√(C₁/C₂)$(): kilovolts in, megavolts out.',
        'The secondary of the $(item)Tesla Coil$() rings at 159 kHz. Set the primary capacitance and where its spiral is tapped (inductance grows '
        'as the turns squared) so it rings there too; a few per cent off and most of the energy stays in the primary. In air an arc reaches '
        'about a metre for every 700 kV.',
        'Its arcs fix the air\'s nitrogen into nitrates - the Birkeland-Eyde process that made the first fertilisers: water becomes bone meal. '
        'Arcs over 2.5 m fuse sand into glass, as lightning makes fulgurites: Fused Silica, without the Sun.',
        'Stand well clear. A running coil strikes anyone within six blocks of it.'],
        adv='resonant_circuits', sort=5, spot=['tesla_coil', 'tesla_toroid', 'tesla_primary'])
