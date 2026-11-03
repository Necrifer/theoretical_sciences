"""
Round 26.

* Pleroma takes every other mod's fluids too. They are distilled first, mod by mod, in a new multiblock - the
  Essence Distillery - into one fluid per mod, its Essence (transcend/Essences: like singularities for ingots and
  specimen bundles for drops). The Pleroma Confluence wants every mod's Essence on top of what it wanted before.
"""
import os

import shapes
import round12 as _r12
from round12 import P as _P, D as _D, C as _C, V as _V

NS = 'theoreticalsciences'

FLUIDS = [dict(id='mod_essence', name='Mod Essence', art='exotic', c1='#e8e8e8', c2='#ffffff', gas=True, light=8, temp=300, density=1, viscosity=1)]

MACHINE_TEXT = {'essence_distillery': ('Essence Distillery',
                                       'A tower of fractionation trays wound with chronosteel coils. Pour in every fluid one mod has and it boils them together, tray after tray, until what is left is a single fluid that stands for all of them: that mod\'s Essence.')}
MACHINE_CAT = {'essence_distillery': 'chemistry'}
MACHINE_RESEARCH = {'essence_distillery': 'fractional_distillation'}
TIER = {'essence_distillery': 5}
MC = {'essence_distillery': (['FGF', 'CSC', 'FGF'], {'F': 'fractionation_casing', 'G': 'containment_field_glass', 'C': 'chronosteel_coil_block',
                                                     'S': 'fractionation_spire'})}
EXTRA = {'essence_distillery': 'Distils every fluid of one mod into that mod\'s $(fluid)Essence$(), which the $(item)Pleroma Confluence$() wants. JEI lists, mod by mod, which fluids go in.'}

LANG = {
    'fluid_type.theoreticalsciences.mod_essence': 'Mod Essence',
    'fluid.theoreticalsciences.mod_essence.of': 'Essence of %s',
    'fluid.theoreticalsciences.mod_essence.part': 'Essence of %s, part %s',
}


def essence_distillery():
    """A distillation tower: a 7x7 base, a column of glass and coil rings around a hollow core, a cap on top."""
    s = shapes.Shape(7, 11, 7)
    c = 3
    s.disc(c, c, 0, 3.3, 'f')
    for y in range(1, 10):
        s.disc(c, c, y, 2.3, 'k' if y % 3 == 0 else 'G', r_in=1.3)
        s.disc(c, c, y, 1.2, '.')
    s.disc(c, c, 10, 2.3, 'f')
    s.center = (c, 5, c)
    s.controller(c, 0, 0)
    s.ports('f', 'g', 4, y=0)
    return s


shapes.BUILDERS['essence_distillery'] = essence_distillery

_r12.model('essence_distillery', 'Essence Distillery',
           'Many fluids boiled together separate only if the column is hot enough for the heaviest and the reflux high enough to keep them apart: the separation is S = R/(R+1)·exp(−|T_c − T_b|/300). The column must stay below the coils\' limit.',
           'The mixture\'s boiling point wanders by ±15%·d.',
           [_P('Tc', 'Column temperature', 'K', 200, 2000, 1, 600, 'Temperature the reboiler holds the column at. Bring it to the mixture\'s boiling point.'),
            _P('Rr', 'Reflux ratio', '', 0, 30, 0.1, 4, 'Liquid sent back down the column per unit drawn off.')],
           [_D('Tb', 'Mixture boiling point', 'K', '600*(1 + 0.15*d)', 'T_b = 600 K·(1 + 0.15·d)', 'Where the blend of fluids boils right now.'),
            _D('S', 'Separation', '', 'Rr/(Rr + 1)*exp(-abs(Tc - Tb)/300)', 'S = R/(R+1)·e^(−|T_c−T_b|/300)', 'How cleanly the fluids part (1 is perfect).'),
            _D('Qd', 'Reboiler duty', 'kW', '0.05*Tc*(Rr + 1)', 'Q = 0.05·T_c·(R+1)', 'Heat into the column; all of it leaves through the coolant.')],
           [_C('flood', 'stop', '25 - Rr', 'Reflux above 25: the column floods', 'R ≤ 25'),
            _C('weak', 'stop', 'S - 0.2', 'The fluids do not separate (S below 0.2): bring the column to their boiling point', 'S ≥ 0.2'),
            _C('overheat', 'damage', '1200 - T', 'The coils are overheating', 'T < 1200 K')],
           rate='clamp(S*3, 0, 4)', rate_formula='rate = 3·S', rate_desc='The cleaner the separation, the faster the Essence comes off.',
           heat='0.02*Qd', idle_heat='0.5', kappa=50, heat_capacity=50)
_r12.WINDOWS['essence_distillery'] = {}


def apply(g):
    g['LANG'].update(LANG)


def finish(g):
    g['text_entry']('chemistry', 'essences', 'Essences of Every Mod', 'essence_distillery', [
        'Pleroma is everything at once - and that now includes every fluid of every mod installed. They do not each go into the $(item)Pleroma Confluence$(): first, every mod\'s fluids are distilled together in the $(item)Essence Distillery$() into that mod\'s $(fluid)Essence$() - one fluid that stands for all of them.',
        'Each distillation takes up to twelve of one mod\'s fluids (16,000 mB of each) and gives 1,000 mB of Essence. A mod with more fluids is distilled in parts, and the parts (4,000 mB of each) into the whole Essence. JEI lists every mod\'s distillations; new mods\' fluids join in by themselves. Only fluids that fit in a bucket count, and a server can leave fluids or mods out (essenceBlacklist).',
        'The Confluence then wants 64,000 mB of every mod\'s Essence beside its million of each of this mod\'s fluids. Essences live in tanks and pipes only: a containment cell cannot remember whose Essence it held.'],
        sort=20, spot=['essence_distillery'])
