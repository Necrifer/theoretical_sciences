"""
Round 15 - the Sundering and the Rekindling: endgame beyond Omnipotence.

1. The Omnipotence Core gains a Shutdown Sequence. Initiated, it destroys the whole Transcendental in sixty seconds,
   speaking as it goes. The dimension is gone - nobody can enter it - and what is left (Shards of Omnipotence, the
   Echo of the Transcendental) plus the research "The Sundering" unlock new materials in the old machines.
2. Those materials build the Pleroma Reconstitutor, a massive multiblock. Run once, it rebuilds the Transcendental
   (research "The Rekindling"), and the reborn Core gains a Forge: crafting with Omnipotence itself.
3. The Forge makes the last materials and the endgame: the Omnipotent Edge, the Scepter of Singularities, the
   Raiment of Transcendence, the Ex Nihilo Engine (infinite energy) and the Lawgiver's Codex (rewrite the laws of the
   place you stand in).

The Forge recipes live in Java (com.theoreticalsciences.transcend.CoreForge) - they are shown on the Core's own screen.
"""
import json
import os

import shapes

NS = 'theoreticalsciences'
HERE = os.path.dirname(os.path.abspath(__file__))


def _I(i, n, art, c1, c2, lore, **kw):
    d = dict(id=i, name=n, art=art, c1=c1, c2=c2, lore=lore)
    d.update(kw)
    return d


ITEMS = [
    # ---- what the Sundering leaves
    _I('omnipotence_shard', 'Shard of Omnipotence', 'shard', '#fff4d0', '#ffffff',
       'A splinter of the Core that made anything. It still remembers how - it has only forgotten what.'),
    _I('transcendental_echo', 'Echo of the Transcendental', 'orb', '#201030', '#ffe0ff',
       'The last ringing of a destroyed dimension, caught before it faded. It is the shape of the place, without the place.', stack=1),
    # ---- materials of the Sundering (old machines, research The Sundering)
    _I('omnipotence_dust', 'Omnipotence Dust', 'dust', '#fff0c0', '#ffffff',
       'Ground Omnipotence. Each grain can still be anything - for a moment.'),
    _I('apeiron_ingot', 'Apeiron Ingot', 'ingot', '#e8e0ff', '#ffe8a0',
       'Anaximander\'s apeiron: the boundless stuff everything else is cut from. The ingot has edges only where you look.'),
    _I('pleroma_crystal', 'Pleroma Crystal', 'crystal', '#fff0ff', '#a0f0ff',
       'Pleroma - the fullness. Every law the Transcendental held, frozen into one lattice.'),
    _I('logos_thread', 'Logos Thread', 'strand', '#ffe080', '#ffffff',
       'A strand of the Word that orders the world. Wound into a coil it makes space obey a shape.'),
    _I('rekindled_spark', 'Rekindled Spark', 'orb', '#ffd060', '#ffffff',
       'What the Pleroma Reconstitutor leaves behind once the Transcendental is whole again: the first light of the second creation.'),
    # ---- materials of the Core Forge (the reborn Core)
    _I('omnipotent_ingot', 'Omnipotent Ingot', 'ingot', '#ffffff', '#ffd860',
       'Metal that is whatever metal it needs to be. Forged at the reborn Core, out of the boundless and the dust of the old Core.'),
    _I('transcendence_crystal', 'Crystal of Transcendence', 'crystal', '#f0e0ff', '#ffffff',
       'A crystal that has stepped outside every system that could describe it.'),
    _I('absolute_singularity', 'Absolute Singularity', 'orb', '#000000', '#ffffff',
       'Not a point where the laws break down: a point where there are no laws to break.'),
    _I('eschaton_core', 'Eschaton Core', 'core', '#fff8e0', '#ff80ff',
       'The end of all things, held still in a cage of Omnipotent metal. Every endgame artefact is built around one.'),
    # ---- the endgame
    _I('omnipotent_edge', 'Omnipotent Edge', 'sword', '#ffffff', '#ffd860',
       'A blade with no thickness and no limit. What it cuts has never existed.', special='edge', stack=1),
    _I('singularity_scepter', 'Scepter of Singularities', 'staff', '#100818', '#ffd0ff',
       'It does not fire singularities. It decides where one has always been.', special='scepter', stack=1),
    _I('crown_of_transcendence', 'Crown of Transcendence', 'helmet', '#fff4d0', '#ffffff',
       'You see everything, because nothing can hide from what is outside it.', special='armor_head', stack=1),
    _I('mantle_of_omnipotence', 'Mantle of Omnipotence', 'chestplate', '#fff4d0', '#ffd860',
       'Harm needs a law to happen by. You are wearing all of them.', special='armor_chest', stack=1),
    _I('greaves_of_the_absolute', 'Greaves of the Absolute', 'leggings', '#fff4d0', '#a0f0ff',
       'Distance is a law. You may keep it or not.', special='armor_legs', stack=1),
    _I('treads_of_infinity', 'Treads of Infinity', 'boots', '#fff4d0', '#ffd0ff',
       'Every step lands. None of them fall.', special='armor_feet', stack=1),
    _I('lawgivers_codex', 'Lawgiver\'s Codex', 'book', '#2a1a08', '#ffd860',
       'Blank, until you write. Then the place you stand in obeys what you wrote.', special='lawcodex', stack=1),
]

BLOCKS = [
    dict(id='absolute_frame_casing', name='Absolute Frame Casing', art='omnicasing', c1='#f4f0e0', c2='#ffd860', kind='casing', light=6, hardness=12.0, tool='diamond'),
    dict(id='logos_coil_block', name='Logos Coil', art='coilblock', c1='#40300c', c2='#ffe080', kind='simple', light=10, hardness=12.0, tool='diamond'),
    dict(id='pleroma_glass', name='Pleroma Glass', art='fieldglass', c1='#fff0ff', c2='#a0f0ff', kind='glass', light=12, hardness=6.0, tool='diamond'),
    dict(id='ex_nihilo_engine', name='Ex Nihilo Engine', art='transcendent', c1='#fffaf0', c2='#ffd860', kind='exnihilo', light=15, hardness=20.0, tool='diamond'),
]

# items that must never come out of the Core's "make anything" grid (tag theoreticalsciences:beyond_omnipotence)
BEYOND = [i['id'] for i in ITEMS] + ['oracle_lemma', 'laplaces_demon', 'absolute_frame_casing', 'logos_coil_block', 'pleroma_glass', 'ex_nihilo_engine', 'pleroma_reconstitutor']

# the Pleroma Reconstitutor itself is declared in machine/MachineType.java (PLEROMA_RECONSTITUTOR)

MACHINE_TEXT = {
    'omega_simulator': ('Omega Simulator',
                        'Runs the whole confrontation with Alpha Omega again - inside a model of it. Shown one Proof of Everything as a template, it walks the argument once more and writes out another copy.'),
    'pleroma_reconstitutor': ('Pleroma Reconstitutor',
                              'A cathedral of Absolute Frame and Pleroma Glass around a Logos-wound throat. Fed the Echo of the Transcendental and everything the Sundering left, it plays the echo back - until the dimension it remembers exists again.'),
}
MACHINE_CAT = {'pleroma_reconstitutor': 'endgame', 'omega_simulator': 'endgame'}
MACHINE_RESEARCH = {'pleroma_reconstitutor': 'the_sundering', 'omega_simulator': 'the_sundering'}
TIER = {'pleroma_reconstitutor': 5, 'omega_simulator': 7}
MC = {
    'omega_simulator': (['OPO', 'CDC', 'OAO'], {'O': 'omega_point', 'P': 'proof_of_everything', 'C': 'cognitive_singularity', 'D': 'ontological_processor',
                                                 'A': 'apeiron_ingot'}),
    'pleroma_reconstitutor': (['LPL', 'AEA', 'LOL'], {'L': 'logos_coil_block', 'P': 'pleroma_crystal', 'A': 'apeiron_ingot', 'E': 'pleroma_crystal',
                                                       'O': 'omnipotence_casing'}),
}
EXTRA = {
    'omega_simulator': 'Makes more $(item)Proofs of Everything$() without fighting Alpha Omega again: keep one Proof in it as the template (never used up) and feed it Omnipotence Dust and the stuff of the argument.',
    'pleroma_reconstitutor': 'Runs once: fed the $(item)Echo of the Transcendental$(), it rebuilds the Transcendental. Only works while the Transcendental is destroyed.',
}

# id, title, description, icon, x, y, insight, requires, cost   (both are granted by events, never studied)
RESEARCH = [
    ('the_sundering', 'The Sundering', 'You told Omnipotence to stop, and it did. From its shards: Omnipotence Dust, the Apeiron Ingot, the Pleroma Crystal, Logos Thread - and the Pleroma Reconstitutor.',
     'omnipotence_shard', 23, 1, 0, ['beyond_the_omega'], []),
    ('the_rekindling', 'The Rekindling', 'The Transcendental exists again, and the Core with it - now able to forge what even it could not simply make.',
     'rekindled_spark', 24, 1, 0, ['the_sundering'], []),
]

LANG = {
    # the Core's screen
    'gui.theoreticalsciences.core_make': 'Omnipotence',
    'gui.theoreticalsciences.core_forge': 'Forge',
    'gui.theoreticalsciences.core_shutdown': 'Shutdown Sequence',
    'gui.theoreticalsciences.core_shutdown_warn': 'Initiating the Shutdown Sequence destroys the Transcendental in 60 seconds. Nobody will be able to enter it again until it is rebuilt. What is left of the Core becomes the material of everything beyond it.',
    'gui.theoreticalsciences.core_shutdown_confirm': 'Begin the Shutdown Sequence',
    'gui.theoreticalsciences.core_shutdown_cancel': 'Not yet',
    'gui.theoreticalsciences.core_shutdown_running': 'Shutdown in progress: %s s',
    'gui.theoreticalsciences.core_forge_locked': 'The Forge wakes only in a Core that has been destroyed and rebuilt.',
    'gui.theoreticalsciences.core_forge_missing': 'Missing materials',
    'gui.theoreticalsciences.core_forge_make': 'Forge',
    'gui.theoreticalsciences.core_forge_have': '%s / %s',
    # the sequence, in the Core's voice
    'message.theoreticalsciences.shutdown.60': '[Omnipotence] Shutdown Sequence accepted. I will stop in sixty seconds.',
    'message.theoreticalsciences.shutdown.50': '[Omnipotence] Releasing the laws I was holding. The rim of the world is coming loose.',
    'message.theoreticalsciences.shutdown.40': '[Omnipotence] The reliquaries are returning what they held to nothing.',
    'message.theoreticalsciences.shutdown.30': '[Omnipotence] Thirty seconds. I have made everything you asked of me. I have never been asked to stop.',
    'message.theoreticalsciences.shutdown.20': '[Omnipotence] The halo is falling. Leave, or be carried out.',
    'message.theoreticalsciences.shutdown.10': '[Omnipotence] Ten. What I was will be scattered as shards. Gather them.',
    'message.theoreticalsciences.shutdown.5': '[Omnipotence] Five.',
    'message.theoreticalsciences.shutdown.4': '[Omnipotence] Four.',
    'message.theoreticalsciences.shutdown.3': '[Omnipotence] Three.',
    'message.theoreticalsciences.shutdown.2': '[Omnipotence] Two.',
    'message.theoreticalsciences.shutdown.1': '[Omnipotence] One.',
    'message.theoreticalsciences.shutdown.0': '[Omnipotence] ...',
    'message.theoreticalsciences.shutdown.done': 'The Transcendental is gone. In your hands: what is left of Omnipotence.',
    'message.theoreticalsciences.shutdown.world': 'Somewhere beyond every law, the Transcendental has been destroyed.',
    'message.theoreticalsciences.sundered_closed': 'There is nothing there any more. The Transcendental must be rebuilt first.',
    'message.theoreticalsciences.shutdown_closed': 'The Transcendental is shutting down.',
    'message.theoreticalsciences.rekindled': 'The Pleroma Reconstitutor plays the Echo back - and the Transcendental exists again. Its Core can now forge.',
    'message.theoreticalsciences.reconstitutor_waiting': 'Only works while the Transcendental is destroyed.',
    'title.theoreticalsciences.shutdown': 'SHUTDOWN',
    'title.theoreticalsciences.rekindled': 'THE REKINDLING',
}

# ---------------------------------------------------------------- the multiblock: a cathedral around a throat
KEY_ADD = {'2': ['absolute_frame_casing'], '7': ['absolute_frame_casing', 'interface_port'], '8': ['pleroma_glass'],
           '&': ['logos_coil_block'], '*': ['omnipotence_casing']}


def pleroma_reconstitutor():
    """17 x 17 x 17: a broad stepped dais, eight Absolute Frame buttresses rising to a crown, a ring of Pleroma Glass
    windows, and in the middle a hollow throat wound with Logos Coil where the Transcendental is played back."""
    import math
    n = 17
    s = shapes.Shape(n, n, n)
    c = 8
    s.disc(c, c, 0, 8.3, '2')                         # the dais
    s.disc(c, c, 1, 7.2, '2')
    s.disc(c, c, 1, 5.0, '*')                         # Omnipotence casing floor of the nave
    for y in range(2, 15):                            # the throat: Logos coil rings, glass between, hollow inside
        s.disc(c, c, y, 2.4, '&' if y % 3 != 1 else '8', r_in=1.6)
        s.disc(c, c, y, 1.5, '.')
    for k in range(8):                                # buttresses leaning in towards the crown
        a = math.radians(22.5 + 45 * k)
        for y in range(2, 15):
            r = 7.0 - (y - 2) * 0.33
            s.put(c + math.cos(a) * r, y, c + math.sin(a) * r, '2')
    for y in (5, 9):                                  # two rings of Pleroma Glass windows tying the buttresses together
        for (cx_, cz_), _a in shapes.ring_cells(c, c, 6.0 - (y - 2) * 0.33):
            s.put(cx_, y, cz_, '8', force=False)
    s.disc(c, c, 15, 3.4, '2', r_in=2.4)              # the crown
    s.disc(c, c, 16, 2.4, '*', r_in=1.4)
    s.put(c, 16, c, '.')
    s.center = (c, 8, c)
    s.ports('2', '7', 8, y=0)
    s.v[(c, 1, c - 8)] = '2'
    s.v[(c, 1, c - 7)] = 'C'
    s.ctrl = (c, 1, c - 7)
    return s


shapes.BUILDERS['pleroma_reconstitutor'] = pleroma_reconstitutor

import round5 as _r5
_r5.HIDDEN['the_sundering'] = 'grant_only'
_r5.HIDDEN['the_rekindling'] = 'grant_only'

SUNDER = 'the_sundering'
TS = 'transcendent_forging'
RC = 'reality_compilation'
UF = 'universe_forging'
CC = 'concept_compression'


def apply(g):
    w, q, mr, shaped, LANG_ = g['w'], g['q'], g['mr'], g['shaped'], g['LANG']
    LANG_.update(LANG)
    extra = os.path.join(HERE, 'round15_lang.json')       # the endgame items' own strings (powers, modes, the Codex screen)
    if os.path.exists(extra):
        LANG_.update(json.load(open(extra)))
    LANG_['research.theoreticalsciences.the_sundering'] = 'The Sundering'
    LANG_['research.theoreticalsciences.the_rekindling'] = 'The Rekindling'
    # ---- the materials of the Sundering
    # the Proof of Everything, again: a simulated confrontation, with one Proof kept as the template
    mr('simulated_proof_of_everything', 'omega_simulation', [('omnipotence_dust', 2), ('omega_point', 1), ('axiom_fragment', 4), ('cognitive_singularity', 1)],
       [('pleroma', 1000)], [('proof_of_everything', 1)], [], 64000000, 1200, SUNDER, catalysts=['proof_of_everything'])
    # renewable: with a Shard as the seed (never used up), Omnipotence condenses out of Pleroma onto omnium
    mr('omnipotence_dust_condensed', CC, [('omnium_ingot', 4), ('transcendental_crystal', 1)], [('pleroma', 2000)], [('omnipotence_dust', 4)], [],
       1500000000, 600, SUNDER, catalysts=['omnipotence_shard'])
    mr('apeiron_ingot', TS, [('omnium_ingot', 2), ('transcendent_metal_ingot', 2), ('omnipotence_dust', 2)], [('pleroma', 500)],
       [('apeiron_ingot', 2)], [], 1500000000, 800, SUNDER)
    mr('pleroma_crystal', RC, [('law_crystal', 2), ('transcendental_crystal', 1), ('omnipotence_dust', 2)], [('pleroma', 1000)],
       [('pleroma_crystal', 1)], [], 1000000000, 800, SUNDER)
    mr('logos_thread', UF, [('proof_of_everything', 1), ('apeiron_ingot', 1), ('omnipotence_dust', 1)], [],
       [('logos_thread', 4)], [], 256000000, 600, SUNDER)
    # the Transcendental's own blocks, remade after the Sundering (the Reconstitutor is built of them, and the Transcendental is gone)
    mr('omnipotence_casing_reforged', TS, [('omnium_block', 2), ('omnipotence_dust', 4), ('apeiron_ingot', 1)], [('pleroma', 1000)],
       [('omnipotence_casing', 4)], [], 800000000, 600, SUNDER)
    mr('transcendent_glass_reforged', TS, [('golden_ratio_glass', 8), ('omnipotence_dust', 1), ('transcendental_crystal', 1)], [('pleroma', 500)],
       [('transcendent_glass', 8)], [], 400000000, 400, SUNDER)
    mr('transcendent_stone_reforged', TS, [('axiom_stone', 16), ('omnipotence_dust', 1)], [('pleroma', 250)],
       [('transcendent_stone', 16)], [], 200000000, 400, SUNDER)
    shaped('absolute_frame_casing', ['AIA', 'IOI', 'AIA'], {'A': 'apeiron_ingot', 'I': 'omnium_ingot', 'O': 'omnipotence_casing'}, 'absolute_frame_casing', 4)
    shaped('logos_coil_block', ['TTT', 'TAT', 'TTT'], {'T': 'logos_thread', 'A': 'apeiron_ingot'}, 'logos_coil_block', 2)
    shaped('pleroma_glass', ['GPG', 'PGP', 'GPG'], {'G': 'transcendent_glass', 'P': 'pleroma_crystal'}, 'pleroma_glass', 4)
    # ---- the Reconstitutor's one recipe: play the Echo back
    mr('rekindle_the_transcendental', 'pleroma_reconstitution',
       [('transcendental_echo', 1), ('apeiron_ingot', 64), ('pleroma_crystal', 16), ('logos_thread', 32), ('omnipotence_dust', 64), ('omnium_block', 16)],
       [('pleroma', 64000)], [('rekindled_spark', 1)], [], 2000000000, 1200, SUNDER)
    # round 36: after the Rekindling the Reconstitutor is not done. With a Rekindled Spark resting in it (never used
    # up) it plays the Echo's harmonics back: the Core Forge's last materials, made by a machine - automatable and in
    # bulk - instead of by hand from the inventory. Each one rings at its own frequency f0 (the console's Echo frequency).
    REK = 'the_rekindling'
    PR = 'pleroma_reconstitution'
    mr('reconstitute_omnipotent_ingot', PR, [('apeiron_ingot', 32), ('omnipotence_dust', 32), ('pleroma_crystal', 8)], [('pleroma', 8000)],
       [('omnipotent_ingot', 3)], [], 2000000000, 900, REK, catalysts=['rekindled_spark'])
    mr('reconstitute_transcendence_crystal', PR, [('pleroma_crystal', 24), ('transcendental_crystal', 12), ('logos_thread', 24)], [('pleroma', 8000)],
       [('transcendence_crystal', 1)], [], 2000000000, 900, REK, catalysts=['rekindled_spark'])
    mr('resonate_omnipotence_shards', PR, [('omnipotent_ingot', 16), ('omnipotence_dust', 48), ('transcendence_crystal', 4)], [('pleroma', 16000)],
       [('omnipotence_shard', 3)], [], 2000000000, 1000, REK, catalysts=['rekindled_spark'])
    # 1.3.6: the Absolute Singularity, the Echo's replay and the Eschaton Cells moved to the Eschaton Foundry (round43),
    # which makes them - and the Eschaton Core - only for someone who has forged them by hand first
    # there is exactly one Echo: this recipe is never inflated by the quantity scaling
    rf = os.path.join(g['REC'], 'pleroma_reconstitution', 'rekindle_the_transcendental.json')
    r = json.load(open(rf))
    r['unscaled'] = True
    w(rf, r)
    # beyond Omnipotence: the Core's "make anything" never offers these
    g['tag']('item', NS, 'beyond_omnipotence', [q(i) for i in BEYOND])


def finish(g):
    te = g['text_entry']
    te('endgame', 'the_shutdown', 'The Shutdown Sequence', 'omnipotence_shard', [
        'The Omnipotence Core can make anything - except what lies beyond Omnipotence. To go further it must be given up.$(br2)Its screen has a $(thing)Shutdown Sequence$(). Begun, it cannot be stopped: in sixty seconds the Core releases every law it holds, and the Transcendental comes apart from the rim inwards.',
        'Anyone still there is carried out at the end. Afterwards nobody can enter the Transcendental at all.$(br2)Whoever began the sequence receives what is left: $(item)Shards of Omnipotence$() and the $(item)Echo of the Transcendental$(). Everyone learns $(thing)The Sundering$().', 'This is not the end of the road: see $(l:endgame/beyond_the_shutdown)Beyond the Shutdown$() for where it leads - the Rekindling, the Forge and the last things.'],
        sort=60, spot=['omnipotence_shard', 'transcendental_echo'])
    te('endgame', 'the_sundering_materials', 'What the Sundering Leaves', 'apeiron_ingot', [
        'The old machines can work the shards:$(li)$(item)Omnipotence Dust$() - Concept Compressor$(li)$(item)Apeiron Ingot$() - Transcendent Plasma Forge$(li)$(item)Pleroma Crystal$() - Reality Compiler$(li)$(item)Logos Thread$() - Universe Forge',
        'Omnipotence does not run out: with one Shard kept in the Concept Compressor as a seed (it is never used up), $(item)Omnipotence Dust$() condenses out of Pleroma onto omnium - as much as you can feed it.$(br2)More $(item)Proofs of Everything$() come from the $(item)Omega Simulator$(), which replays the fight with Alpha Omega using one Proof as its template.$(br2)From them: $(item)Absolute Frame Casing$(), $(item)Logos Coil$() and $(item)Pleroma Glass$() - the parts of the $(item)Pleroma Reconstitutor$().'],
        sort=61, spot=['apeiron_ingot', 'pleroma_crystal', 'logos_thread'])
    te('endgame', 'the_rekindling', 'The Rekindling', 'pleroma_reconstitutor', [
        'The $(item)Pleroma Reconstitutor$() is the largest machine there is: seventeen blocks in every direction. Fed the $(item)Echo of the Transcendental$() and the rest of the Sundering\'s materials, it plays the echo back.',
        'When it finishes, the Transcendental exists again, rebuilt as it was, and everyone learns $(thing)The Rekindling$(). The reborn Core keeps its Omnipotence - and gains a $(thing)Forge$().',
        'The Rekindling happens once in a world: the Reconstitutor only plays the Echo back while there is no Transcendental. Its screen says why it waits - the Transcendental still stands, is still coming apart, or was already rekindled.',
        'Afterwards the machine is far from idle. Keep the $(item)Rekindled Spark$() in it (it is never used up) and it plays back the Echo\'s $(thing)harmonics$() - the Forge\'s last materials, made by a machine instead of by hand, and some in greater yield:$(li)$(item)Omnipotent Ingot$() x3$(li)$(item)Crystal of Transcendence$()$(li)$(item)Shards of Omnipotence$() x3$(li)$(item)Absolute Singularity$()$(li)another $(item)Echo of the Transcendental$()$(li)$(item)Eschaton Cells$() x4',
        'Each harmonic rings at its own frequency: set the drive onto the $(thing)Echo frequency$() the recipe names (JEI shows it), and give it the playback power its window asks for.'],
        sort=62, spot=['pleroma_reconstitutor'])
    te('endgame', 'the_core_forge', 'The Core Forge', 'eschaton_core', [
        'The reborn Core\'s screen has a $(thing)Forge$() page. Its recipes take materials from your inventory; it shows what each needs and what you have.',
        'It makes the last materials - the $(item)Omnipotent Ingot$(), the $(item)Crystal of Transcendence$(), the $(item)Absolute Singularity$(), the $(item)Eschaton Core$() - and from them everything beyond Omnipotence. None of these can be had from the Core\'s "make anything".',
        'After every forging, of anything, the Core needs three minutes to re-seed its universe before it compiles again. The materials machines can also make - Omnipotent Ingots, Crystals of Transcendence and Shards (the $(item)Pleroma Reconstitutor$()), Absolute Singularities, the Echo, Eschaton Cells and Eschaton Cores (the $(item)Eschaton Foundry$(), once you have forged each by hand) - are cheaper here, but a universe tuned for one of them is $(thing)spent$() by forging it: its laws drift to new values. For quantity, build the machines.'],
        sort=63, spot=['omnipotent_ingot', 'transcendence_crystal', 'absolute_singularity', 'eschaton_core'])
    te('endgame', 'beyond_omnipotence', 'Beyond Omnipotence', 'omnipotent_edge', [
        '$(item)Omnipotent Edge$(): cuts through any armour and any invulnerability. Right-click erases the hostile creatures in front of you; sneak and right-click stops time around you.',
        '$(item)Scepter of Singularities$(): right-click puts a singularity where you look. Sneak-right-click changes which: Black Hole (pulls in and crushes), White Hole (throws out), Tesseract (folds space - you are simply there), Entanglement (one strike, every linked foe), Big Bang (everything near the point at once).',
        'The $(item)Raiment of Transcendence$() (Crown, Mantle, Greaves, Treads): each piece has its own gift; the whole set makes you untouchable, lets you fly, and holds every dimension\'s laws away from you.',
        'The $(item)Ex Nihilo Engine$(): energy from nothing, without end - over two billion FE every tick on every side, and it charges the energy items of everyone nearby.',
        'The $(item)Lawgiver\'s Codex$(): open it and rewrite the place you stand in - its gravity, its time and weather, which creatures may appear and how strong they are; create creatures, or rewrite the one you were looking at. "Restore" puts the natural laws back.'],
        sort=64, spot=['omnipotent_edge', 'singularity_scepter', 'mantle_of_omnipotence', 'ex_nihilo_engine', 'lawgivers_codex'])


# ---------------------------------------------------------------- its physics (round 12 model, like every multiblock)
import round12 as _r12
from round12 import P as _P, D as _D, C as _C, V as _V, K as _K

_r12.model('pleroma_reconstitutor', 'Pleroma Reconstitutor',
           'The Echo of the Transcendental is a standing wave: it rings at its own frequency f₀ and nowhere else. The Logos coils hold it in the throat with a field B; the pump drives it. A driven resonator answers with a Lorentzian, '
           'A(f) = 1/(1 + (2Q·(f − f₀)/f₀)²): only a drive tuned to within f₀/2Q is heard at all. The playback power is the pump power the echo actually absorbs.',
           'The Echo drifts: its quality factor falls to Q₀/(1 + |d|) and its frequency wanders by 0.01%·d.',
           [_P('I', 'Logos coil current', 'kA', 0, 100, 0.1, 10, 'Current through the Logos coils. Sets the field in the throat: B = 0.8·I.'),
            _P('f', 'Drive frequency', 'THz', 0, 10, 0.0001, 4, 'Frequency at which the pump drives the throat. Tune it onto the Echo.'),
            _P('Pp', 'Pump power', 'GW', 0, 100, 0.1, 5, 'Power fed into the throat.')],
           [_D('B', 'Throat field', 'T', '0.8*I', 'B = 0.8·I', 'Field the Logos coils hold in the throat.'),
            _D('f0', 'Echo frequency', 'THz', 'F0*(1 + 0.0001*d)', 'f₀(1 + 10⁻⁴·d)', 'Where the Echo actually rings right now.'),
            _D('Q', 'Quality factor', '', '2000/(1 + abs(d))', 'Q = Q₀/(1 + |d|)', 'How sharply the Echo resonates. Disturbance broadens it.'),
            _D('x', 'Detuning', '', '(f - f0)/f0', 'δ = (f − f₀)/f₀', 'Relative distance of the drive from the Echo.'),
            _D('A', 'Resonant response', '', '1/(1 + (2*Q*x)^2)', 'A = 1/(1 + (2Qδ)²)', 'How much of the drive the Echo answers (1 on resonance).'),
            _D('Pe', 'Playback power', 'GW', 'Pp*A', 'P = P_pump·A', 'Power going into rebuilding the Transcendental.'),
            _D('Pw', 'Wasted power', 'GW', 'Pp*(1 - A)', 'P_pump·(1 − A)', 'Drive the Echo does not take: it heats the throat.'),
            _D('U', 'Field energy', 'GJ', 'B^2/(2*1.2566)*0.05', 'U = B²/2µ₀·V', 'Energy stored in the throat field (5 m³).')],
           [_C('hold', 'stop', 'B - 30', 'Throat field too weak to hold the Echo', 'B ≥ 30 T'),
            _C('tear', 'damage', '70 - B', 'Field above 70 T: the throat is tearing', 'B ≤ 70 T'),
            _C('flood', 'wear', '20 - Pw', 'More than 20 GW of drive is not being absorbed: the throat overheats', 'P_wasted < 20 GW'),
            _C('overheat', 'damage', '900 - T', 'The throat is overheating', 'T < 900 K')],
           rate='clamp(Pe/10, 0, 4)', rate_formula='rate = P_playback / 10 GW', rate_desc='The Transcendental rebuilds as fast as the Echo absorbs power.',
           heat='5 + 2*Pw + 0.02*I', idle_heat='2', quench_energy='U*20', rvars=[_V('F0', 'Echo frequency f₀ (THz)', 4.2)], kappa=60, heat_capacity=80)
_r12.WINDOWS['pleroma_reconstitutor'] = {
    'rekindle_the_transcendental': ({'F0': 4.2}, [('A', 0.6, 1.01), ('Pe', 10, 1000)]),
    # round 36: the Echo's harmonics, each at its own frequency and power
    'reconstitute_omnipotent_ingot': ({'F0': 1.6}, [('A', 0.6, 1.01), ('Pe', 12, 60)]),
    'reconstitute_transcendence_crystal': ({'F0': 2.4}, [('A', 0.6, 1.01), ('Pe', 15, 80)]),
    'resonate_omnipotence_shards': ({'F0': 3.3}, [('A', 0.65, 1.01), ('Pe', 20, 100)]),
}
