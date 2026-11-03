"""
Round 27.

* What the Sundering and the Rekindling leave (Shards of Omnipotence, the Echo of the Transcendental, the Rekindled
  Spark) are no longer one-offs: the reborn Core resonates more of them, and they make the last things in the mod -
  the Rekindled Heart, the Echo Lattice, the Seed of Omnipotence, the Hand of the Demiurge and, at the very end, an
  Omnipotence Core of your own (transcend/ForgedCore*; recipes in transcend/CoreForge.java).
* The forged Core cannot be broken. Only the Hand of the Demiurge tunes it to the place it stands in, wakes it and
  moves it (endgame/DemiurgeHandItem).
* Fixes: JEI's Pleroma page and the Confluence's status, codex entry category (the whole Patchouli book was empty),
  energy above the int limit, the entity tag.
* The dimension travel animations, loading screens and the Rekindling cinematic (client/transit, client/cinematic)
  bring their text in round27_fx.LANG.
"""
import json
import os

NS = 'theoreticalsciences'


def _I(i, n, art, c1, c2, lore, **kw):
    d = dict(id=i, name=n, art=art, c1=c1, c2=c2, lore=lore)
    d.update(kw)
    return d


ITEMS = [
    _I('rekindled_heart', 'Rekindled Heart', 'orb', '#ff9040', '#fff4d0',
       'Four sparks of the second creation, beating together around an Eschaton Core.', stack=16),
    _I('echo_lattice', 'Echo Lattice', 'lattice', '#301848', '#ffe0ff',
       'Echoes of the Transcendental, crossed and woven into a frame that remembers the shape of a world.', stack=16),
    _I('omnipotence_seed', 'Seed of Omnipotence', 'seed', '#fff8e0', '#ffd860',
       'Everything a Core needs to grow, folded small: a heart, a remembered world, and the proof that it can be done.', stack=16),
    _I('demiurge_hand', 'Hand of the Demiurge', 'artifact', '#fff4d0', '#ffd0ff',
       'The craftsman of the world had to hold it somehow. The only thing that can tune, wake or move an Omnipotence Core.',
       special='demiurgehand', stack=1),
]
BEYOND = [i['id'] for i in ITEMS] + ['forged_omnipotence_core']

LANG = {
    'block.theoreticalsciences.forged_omnipotence_core': 'Omnipotence Core',
    # the forged Core
    'message.theoreticalsciences.forged_core.dormant': 'The Core is dormant. Tune it to this place and wake it with the Hand of the Demiurge.',
    # round 34c: the attunement screen
    'gui.theoreticalsciences.attune.title': 'Attunement of the Omnipotence Core',
    'gui.theoreticalsciences.attune.weakest': 'Weakest branch: %s%%',
    'gui.theoreticalsciences.attune.clouded': 'Readings clouded by the anomaly',
    'gui.theoreticalsciences.attune.awake': 'AWAKE - in tune with this place',
    'gui.theoreticalsciences.attune.wake': 'Wake the Core',
    'gui.theoreticalsciences.attune.is_awake': 'The Core is awake',
    'gui.theoreticalsciences.attune.ring': 'each leans on the one before',
    'gui.theoreticalsciences.attune.branch.physics': 'Physics',
    'gui.theoreticalsciences.attune.branch.chemistry': 'Chemistry',
    'gui.theoreticalsciences.attune.branch.biology': 'Biology',
    'gui.theoreticalsciences.attune.branch.mathematics': 'Mathematics',
    'gui.theoreticalsciences.attune.branch.origin': 'Origin',
    'gui.theoreticalsciences.attune.branch.finality': 'Finality',
    'gui.theoreticalsciences.attune.branch.abnormality': 'Abnormality',
    'gui.theoreticalsciences.attune.param.physics.0': 'Field Strength',
    'gui.theoreticalsciences.attune.param.physics.1': 'Coupling Constant',
    'gui.theoreticalsciences.attune.param.physics.2': 'Vacuum Temperature',
    'gui.theoreticalsciences.attune.param.chemistry.0': 'Reagent A',
    'gui.theoreticalsciences.attune.param.chemistry.1': 'Reagent B',
    'gui.theoreticalsciences.attune.param.chemistry.2': 'Reagent C',
    'gui.theoreticalsciences.attune.param.biology.0': 'Growth',
    'gui.theoreticalsciences.attune.param.biology.1': 'Mutation',
    'gui.theoreticalsciences.attune.param.biology.2': 'Selection',
    'gui.theoreticalsciences.attune.param.mathematics.0': 'a',
    'gui.theoreticalsciences.attune.param.mathematics.1': 'b',
    'gui.theoreticalsciences.attune.param.mathematics.2': 'n',
    'gui.theoreticalsciences.attune.param.origin.0': 'Primordial Phase',
    'gui.theoreticalsciences.attune.param.origin.1': 'Inflation Phase',
    'gui.theoreticalsciences.attune.param.origin.2': 'Recombination Phase',
    'gui.theoreticalsciences.attune.param.finality.0': 'Entropy',
    'gui.theoreticalsciences.attune.param.finality.1': 'Decay Rate',
    'gui.theoreticalsciences.attune.param.finality.2': 'Horizon',
    'gui.theoreticalsciences.attune.param.abnormality.0': 'Infection',
    'gui.theoreticalsciences.attune.param.abnormality.1': 'Containment',
    'gui.theoreticalsciences.attune.param.abnormality.2': 'Mirror',
    'gui.theoreticalsciences.attune.hint.physics': 'Three plain fields. But its target leans on Abnormality, the branch before it in the ring.',
    'gui.theoreticalsciences.attune.hint.chemistry': 'The three reagents always add up to a whole: raising one lowers the other two.',
    'gui.theoreticalsciences.attune.hint.biology': 'Living parameters: what is measured is what they settle into together, not each one on its own.',
    'gui.theoreticalsciences.attune.hint.mathematics': 'Whole numbers, exact or nothing. Each lamp is one law: a mod 7, a times b mod 11, a + b + n mod 13, and n prime.',
    'gui.theoreticalsciences.attune.hint.origin': 'Phases wrap round. The first is absolute; each of the others counts from the one before it.',
    'gui.theoreticalsciences.attune.hint.finality': 'Entropy creeps up while you work on a dormant Core: leave it a little low, and wake the Core soon after.',
    'gui.theoreticalsciences.attune.hint.abnormality': 'While uncontained, the anomaly spreads: it nudges the other branches and clouds every other reading. Contain it first.',
    'message.theoreticalsciences.forged_core.tune': 'Resonance with this place: %s%% (at %s°)',
    'message.theoreticalsciences.forged_core.in_tune': 'In tune with this place: %s%% (at %s°). It can be woken.',
    'message.theoreticalsciences.forged_core.not_in_tune': 'The Core is not in tune with this place (weakest branch %s%%): attune it first.',
    'message.theoreticalsciences.forged_core.wakes': 'The Omnipotence Core wakes.',
    'message.theoreticalsciences.forged_core.sleeps': 'The Core falls dormant.',
    'message.theoreticalsciences.forged_core.only_reborn': 'Only the reborn Core in the Transcendental can forge this.',
    'message.theoreticalsciences.forged_core.lifted': 'The Core is lifted out of the world. Wherever you set it down, it must be tuned again.',
    # the Hand
    'message.theoreticalsciences.demiurge_hand.mode': 'Hand of the Demiurge: %s',
    'message.theoreticalsciences.demiurge_hand.original': 'This Core belongs to the Transcendental. The Hand works only on a Core forged anew.',
    'item.theoreticalsciences.demiurge_hand.mode.tune': 'Tune',
    'item.theoreticalsciences.demiurge_hand.mode.activate': 'Activate',
    'item.theoreticalsciences.demiurge_hand.mode.move': 'Move',
    'item.theoreticalsciences.demiurge_hand.help.tune': 'Use on an Omnipotence Core to open its attunement: every branch you studied, tuned to this place.',
    'item.theoreticalsciences.demiurge_hand.help.activate': 'Use on an Omnipotence Core in tune to wake it - or on an awake one to let it sleep.',
    'item.theoreticalsciences.demiurge_hand.help.move': 'Use on an Omnipotence Core to lift it into your inventory.',
    'item.theoreticalsciences.demiurge_hand.switch': 'Sneak and use in the air to change mode.',
    # JEI and the Confluence: this mod's fluids and the Essences are wanted in different amounts
    'jei.theoreticalsciences.pleroma_each': 'This mod\'s fluids: %s mB each',
    'jei.theoreticalsciences.pleroma_essences': '%s mod Essences: %s mB each',
    'gui.theoreticalsciences.confluence_status': '%s / %s full (fluids %s mB, Essences %s mB)',
}


def _dormant_texture(A, bid):
    """The Core's animated texture, drained of colour and light, and slowed to a third: a Core asleep."""
    from PIL import Image, ImageEnhance
    src = os.path.join(A, 'textures', 'block', 'omnipotence_core.png')
    out = os.path.join(A, 'textures', 'block', bid + '_dormant.png')
    im = Image.open(src).convert('RGBA')
    rgb = ImageEnhance.Color(im.convert('RGB')).enhance(0.25)
    rgb = ImageEnhance.Brightness(rgb).enhance(0.45)
    # a cold violet cast over the grey
    tint = Image.new('RGB', rgb.size, (70, 55, 110))
    rgb = Image.blend(rgb, tint, 0.22)
    res = rgb.convert('RGBA')
    res.putalpha(im.getchannel('A'))
    res.save(out)
    with open(out + '.mcmeta', 'w') as f:
        json.dump({'animation': {'frametime': 6, 'interpolate': True}}, f)


def apply(g):
    w, D, A = g['w'], g['D'], g['A']
    g['LANG'].update(LANG)
    try:
        import round27_fx
        g['LANG'].update(round27_fx.LANG)
    except ImportError:
        pass
    # the forged Core: the Transcendental Core's look, its own block
    bid = 'forged_omnipotence_core'
    # round 34c: dormant (a dim, slow copy of the Core's texture) and awake (the Core's own); the block entity
    # renderer adds the motes, rings and beams (client/render/ForgedCoreRenderer)
    w(os.path.join(A, 'blockstates', bid + '.json'), {'variants': {
        'active=false': {'model': f'{NS}:block/{bid}_dormant'}, 'active=true': {'model': f'{NS}:block/{bid}'}}})
    w(os.path.join(A, 'models', 'block', bid + '.json'), {'parent': 'minecraft:block/cube_all', 'textures': {'all': f'{NS}:block/omnipotence_core'}})
    w(os.path.join(A, 'models', 'block', bid + '_dormant.json'), {'parent': 'minecraft:block/cube_all', 'textures': {'all': f'{NS}:block/{bid}_dormant'}})
    _dormant_texture(A, bid)
    w(os.path.join(A, 'models', 'item', bid + '.json'), {'parent': f'{NS}:block/{bid}'})
    # nothing breaks it: not the Wither, not the Dragon
    for tag in ('wither_immune', 'dragon_immune'):
        tp = os.path.join(g['RES'], 'data', 'minecraft', 'tags', 'block', tag + '.json')
        t = json.load(open(tp)) if os.path.exists(tp) else {'replace': False, 'values': []}
        for b in (f'{NS}:{bid}', f'{NS}:omnipotence_core'):
            if b not in t['values']:
                t['values'].append(b)
        w(tp, t)
    tf = os.path.join(D, 'tags', 'item', 'beyond_omnipotence.json')
    t = json.load(open(tf))
    for b in BEYOND:
        if f'{NS}:{b}' not in t['values']:
            t['values'].append(f'{NS}:{b}')
    w(tf, t)


def finish(g):
    g['text_entry']('endgame', 'second_core', 'An Omnipotence Core of Your Own', 'forged_omnipotence_core', [
        'Once the Transcendental is rekindled, nothing the Sundering left is a one-off any more. At the reborn Core\'s Forge, $(item)Shards of Omnipotence$() resonate into more shards, Shards replay the $(item)Echo of the Transcendental$(), and Echoes strike a second $(item)Rekindled Spark$().',
        'They make the last things there are: the $(item)Rekindled Heart$(), the $(item)Echo Lattice$(), the $(item)Seed of Omnipotence$() and the $(item)Hand of the Demiurge$() - and, last of all, an $(item)Omnipotence Core$() of your own.',
        'Nothing can break the Core: not a pickaxe, not an explosion, not a piston, not the Wither. Only the $(item)Hand of the Demiurge$() can handle it. Sneak and use the Hand in the air to switch its mode.',
        '$(li)$(bold)Tune$(): opens the Core\'s attunement.$(li)$(bold)Activate$(): wakes a Core that is in tune, or lets an awake one sleep.$(li)$(bold)Move$(): lifts the Core into your inventory. Set down somewhere else, it must be attuned again.',
        'Attunement is the hardest tuning there is. Every branch you studied has three parameters - 21 in all - and every place (world, dimension, spot) wants its own values. The screen only says how in tune each $(thing)branch$() is, never which parameter is off. All seven must reach 97% (Mathematics: all four lamps) before the Core can be woken. Hover over a branch\'s name for its rule.',
        'Each branch behaves differently. $(thing)Chemistry$()\'s reagents always make a whole. $(thing)Biology$() measures what its parameters settle into together. $(thing)Mathematics$() is exact: congruences and a prime. $(thing)Origin$()\'s phases wrap round, each counting from the one before. $(thing)Finality$()\'s entropy creeps up while you work.',
        'Contain $(thing)Abnormality$() first: while it is loose it spreads into the other branches, nudging them, and clouds every other reading. And the branches lean on each other in a ring - Physics, Chemistry, Biology, Origin, Finality, Abnormality - so settling one moves where the next one wants to be. Go round more than once.',
        'Asleep, the Core is dim and seven faint motes drift slowly round it, one for each branch it must be attuned in. Woken, it flares: the motes race, golden rings turn about it and light rises and falls from it.',
        'An awake Core works like the one in the Transcendental, wherever it stands: Materialize and the Forge. It has no Shutdown Sequence. Changing its attunement out of tune puts it back to sleep.'],
        sort=90, spot=['forged_omnipotence_core'])
