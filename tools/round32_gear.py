"""
Round 32 (gear): Eschaton Charge and the Dodecaract Excavator.

* Every endgame tool, weapon and piece of armour (the Omnipotent Edge, the Scepter of Singularities, the Raiment of
  Transcendence, the Lawgiver's Codex, the Censer of Entropy, the Hand of the Demiurge and the new Excavator) now runs
  on Eschaton Charge, kept on the item (endgame/EschatonCharge.java). Empty, an item is dormant. Eschaton Cells
  (endgame/EschatonCellItem.java) fill everything you carry.
* The Dodecaract Excavator (endgame/DodecaractExcavatorItem.java, ExcavatorJobs.java): a pickaxe with Single, Area
  and Ore Extraction modes and a reach from 3x3x3 to 33x33x33.
* The Omnipotent Edge also sends what it kills - drops and experience - straight to its wielder.
* Core Forge recipes: transcend/CoreForge.java. The renewable route for cells: the Concept Compressor (below).
* Textures: art32/gear.py (a rotating 12-cube shadow, a charged capsule).
"""
import json
import os

NS = 'theoreticalsciences'


def _I(i, n, art, c1, c2, lore, **kw):
    d = dict(id=i, name=n, art=art, c1=c1, c2=c2, lore=lore)
    d.update(kw)
    return d


ITEMS = [
    _I('dodecaract_excavator', 'Dodecaract Excavator', 'tesseract', '#fff4d0', '#80f0ff',
       'A cube with twelve dimensions has 4096 corners. One of them is always already inside the rock.', special='excavator', stack=1),
    _I('eschaton_cell', 'Eschaton Cell', 'orb', '#ffe0ff', '#ffd860',
       'The end of all things, metered out in doses. What the last artefacts burn to stay what they are.', special='eschatoncell', stack=16),
]
BLOCKS = []
FLUIDS = []
BEYOND = [i['id'] for i in ITEMS]

LANG = {
    # Eschaton Charge
    'tooltip.theoreticalsciences.eschaton.charge': 'Eschaton Charge: %s / %s',
    'tooltip.theoreticalsciences.eschaton.dormant': 'DORMANT - recharge with an Eschaton Cell',
    'message.theoreticalsciences.eschaton.dormant': '%s is dormant: it needs Eschaton Charge.',
    'message.theoreticalsciences.eschaton.too_little': 'Not enough Eschaton Charge (needs %s, holds %s).',
    'message.theoreticalsciences.raiment.fading': 'The Raiment of Transcendence is fading: a piece is almost out of Eschaton Charge.',
    # the Cell
    'tooltip.theoreticalsciences.eschaton_cell.holds': 'Holds %s / %s Eschaton Charge',
    'tooltip.theoreticalsciences.eschaton_cell.use': 'Use: pour it into every endgame item you carry or wear, emptiest first',
    'message.theoreticalsciences.eschaton_cell.used': 'Recharged %s item(s) with %s Eschaton Charge.',
    'message.theoreticalsciences.eschaton_cell.nothing': 'Nothing you carry needs Eschaton Charge.',
    # the Edge's harvest
    'tooltip.theoreticalsciences.omnipotent_edge.harvest': 'What it kills - drops and experience - comes straight to you',
    # the Excavator
    'item.theoreticalsciences.dodecaract_excavator.mode.single': 'Single',
    'item.theoreticalsciences.dodecaract_excavator.mode.area': 'Area',
    'item.theoreticalsciences.dodecaract_excavator.mode.extract': 'Ore Extraction',
    'item.theoreticalsciences.dodecaract_excavator.help.single': 'Breaks any breakable block at once - stone, wood, earth or Omnipotence Casing - and always keeps the drops.',
    'item.theoreticalsciences.dodecaract_excavator.help.area': 'Breaking a block breaks the whole cube behind it; the drops come to you.',
    'item.theoreticalsciences.dodecaract_excavator.help.extract': 'Use on a block (or in the air, around you): every ore in the cube comes to you and stone takes its place.',
    'item.theoreticalsciences.dodecaract_excavator.switch': 'Sneak and use to change mode; sneak and scroll to change the reach.',
    'message.theoreticalsciences.excavator.mode': 'Excavator: %s (%s)',
    'message.theoreticalsciences.excavator.radius': 'Excavator reach: %s',
    'message.theoreticalsciences.excavator.scanning': 'Folding through %s...',
    'message.theoreticalsciences.excavator.extracted': 'Extracted %s ores from %s blocks.',
    'message.theoreticalsciences.excavator.busy': 'The Excavator is still working.',
    'message.theoreticalsciences.excavator.out': 'The Dodecaract Excavator ran out of Eschaton Charge.',
}


def apply(g):
    w, D, A, mr = g['w'], g['D'], g['A'], g['mr']
    g['LANG'].update(LANG)
    # the Excavator is held like a tool
    w(os.path.join(A, 'models', 'item', 'dodecaract_excavator.json'),
      {'parent': 'minecraft:item/handheld', 'textures': {'layer0': f'{NS}:item/dodecaract_excavator'}})
    # renewable cells: with an Eschaton Core and a Rekindled Spark as catalysts (never used up), Omnipotence condenses
    # into a cell out of Pleroma - slower and dearer per cell than the Forge, but it can be automated
    mr('eschaton_cell_condensed', 'concept_compression',
       [('omnipotence_dust', 48), ('omnipotent_ingot', 6), ('transcendence_crystal', 2), ('logos_thread', 8)],
       [('pleroma', 8000)], [('eschaton_cell', 1)], [], 2000000000, 1200, 'the_rekindling',
       catalysts=['eschaton_core', 'rekindled_spark'])
    # beyond Omnipotence: the Core's "make anything" never offers these
    tf = os.path.join(D, 'tags', 'item', 'beyond_omnipotence.json')
    t = json.load(open(tf))
    for b in BEYOND:
        if f'{NS}:{b}' not in t['values']:
            t['values'].append(f'{NS}:{b}')
    w(tf, t)
    # the Excavator is a pickaxe (Efficiency, Fortune, Silk Touch from the enchanting table and anvil)
    tp = os.path.join(g['RES'], 'data', 'minecraft', 'tags', 'item', 'pickaxes.json')
    t = json.load(open(tp)) if os.path.exists(tp) else {'replace': False, 'values': []}
    if f'{NS}:dodecaract_excavator' not in t['values']:
        t['values'].append(f'{NS}:dodecaract_excavator')
    w(tp, t)
    # Ore Extraction: extra ores, and blocks it must never take (empty: for modpacks)
    g['tag']('block', NS, 'excavator_ores', [])
    g['tag']('block', NS, 'excavator_ignored', [])


def finish(g):
    g['LANG'].update(LANG)
    te = g['text_entry']
    te('endgame', 'eschaton_charge', 'Eschaton Charge', 'eschaton_cell', [
        'Everything beyond Omnipotence now has to be kept that way. Every endgame tool, weapon and piece of armour holds $(thing)Eschaton Charge$() - shown as a bar under it and in its tooltip - and spends it as it works.',
        'With none left an item is $(thing)dormant$(): the $(item)Omnipotent Edge$() is only a blade, the $(item)Scepter$(), $(item)Codex$(), $(item)Censer$() and $(item)Hand$() refuse to act, and a piece of the $(item)Raiment$() gives no armour and no gifts. The Raiment drains while worn and for every blow it keeps away.',
        'An $(item)Eschaton Cell$() holds 100,000. Use it and it fills every endgame item you carry or wear, emptiest first; what is left stays in the cell. Things come out of the Forge full.',
        'Cells come four at a time from the $(thing)Core Forge$(), or one at a time from the $(item)Concept Compressor$(), which condenses them out of Pleroma with an Eschaton Core and a Rekindled Spark held as catalysts.',
        '$(li)Edge: 20 a hit, 400 Erasure, 600 Stasis, 5 a kill$(li)Scepter: 150-1500 a singularity$(li)Codex: 500 a law$(li)Censer: 200 a touch, 400 a cloud$(li)Hand: 250 an action$(li)Excavator: 5 a block, 20 an ore$(li)Raiment: 1 a second, 10 a point of damage$(br)Creative players spend nothing.'],
        adv='the_rekindling', sort=91, spot=['eschaton_cell'])
    te('endgame', 'dodecaract_excavator', 'The Dodecaract Excavator', 'dodecaract_excavator', [
        'A pickaxe in the shape of a twelve-dimensional cube. It mines anything a netherite pickaxe can, far faster, and never wears out - it spends Eschaton Charge instead.',
        'Sneak and use it to change mode; sneak and scroll to change its reach, from 3x3x3 to 33x33x33.$(li)$(bold)Single$(): one block at a time - any breakable block, broken at once, with its drops, whatever slows you down.$(li)$(bold)Area$(): breaking a block breaks the whole cube reaching into the wall behind it. The drops come straight to you.',
        '$(li)$(bold)Ore Extraction$(): use it on a block, or in the air to work around yourself. Every ore in the cube is pulled out into your inventory - with the tool\'s Fortune or Silk Touch - and the stone it grew in takes its place. The rest of the rock is left as it was.',
        'It respects spawn protection and claims, and never takes unbreakable blocks or blocks with contents. Pack makers can add ores with the block tag $(thing)theoreticalsciences:excavator_ores$() and protect blocks with $(thing)theoreticalsciences:excavator_ignored$().'],
        adv='the_rekindling', sort=92, spot=['dodecaract_excavator'])
