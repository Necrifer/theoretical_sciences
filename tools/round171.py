"""
1.7.1: the endgame rework, second step (design document, 10 November). Every endgame structure makes a signature material
that nothing else can make, and each tier's recipes take one from the tier below. Eight structures are reshaped (see
r171_shapes.py), and four machines that were single blocks become structures: the Cantor Diagonalizer, the Axiom
Distiller, the Convergence Engine and the Pleroma Confluence (which is a machine with a console now, not a block with a
screen of its own).

Run:  python3 tools/round171.py
"""
import copy, glob, json, os, sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from tsgen import *          # noqa: F401,F403
import tsgen as T
import r171_shapes as S

L = T.LANG
R = os.path.join(D, 'recipe')

# ====================================================================== the signature materials
# tier, id, name, the structure (machine id, recipe type), what it is
SIG = [
    (2, 'dyson_lattice_panel', 'Dyson Lattice Panel', 'dyson_array', 'dyson',
     "A collector panel that has spent a burn in the swarm: its lattice has taken the star's light for so long that it holds a little of it."),
    (2, 'stellar_wind_condensate', 'Stellar Wind Condensate', 'stellar_engine', 'stellar_thrust',
     "What the Caplan jets skim off a star and cannot burn: the heavy end of its wind, frozen into a dull, warm ingot."),
    (2, 'recursive_thought_shell', 'Recursive Thought Shell', 'matrioshka_brain', 'matrioshka',
     "A shell of computronium that models the shell outside it, which models the next: a thought about thinking, cast solid."),
    (2, 'embryonic_vacuum', 'Embryonic Vacuum', 'universe_forge', 'universe_forging',
     "A bead of vacuum from the first instant of a universe that was never let grow: its laws are chosen, and nothing has happened in it yet."),
    (3, 'compiled_law', 'Compiled Law', 'reality_compiler', 'reality_compilation',
     "A law of nature as the compiler writes it out: no longer a description of what happens, but the instruction that makes it happen."),
    (3, 'lex_prima', 'Lex Prima', 'omnilex_engine', 'omnilex_simulation',
     "The first law of a simulated world, the one its other laws are derived from, lifted out whole."),
    (3, 'breach_shard', 'Breach Shard', 'axiomatic_breach_engine', 'reality_breach',
     "A splinter of the edge of a breach: one face of it obeys this world's axioms and the other face does not."),
    (3, 'pure_concept_ingot', 'Pure Concept Ingot', 'concept_compressor', 'concept_compression',
     "An idea pressed until nothing is left of it but the idea: no example of it, no word for it, and it weighs what an ingot weighs."),
    (3, 'diagonal_ingot', 'Diagonal Ingot', 'cantor_diagonalizer', 'transfinite_computation',
     "Cast from the number the Diagonalizer writes: it differs from everything on the list, so it cannot be counted among them."),
    (3, 'distilled_axiom', 'Distilled Axiom', 'axiom_distiller', 'axiom_distillation',
     "The seventh fraction off the top of the still: an axiom with every theorem boiled out of it."),
    (4, 'transcendent_alloy', 'Transcendent Alloy', 'transcendent_plasma_forge', 'transcendent_forging',
     "An alloy of the six materials beyond physics, forged where the forge sets the properties: it has the ones it was told to have."),
    (4, 'event_horizon_lens', 'Event Horizon Lens', 'eye_of_the_horizon', 'horizon_simulation',
     "A lens ground from a simulated horizon: light that enters it leaves in the order the lens decides."),
    (4, 'pleromatic_weave', 'Pleromatic Weave', 'pleroma_reconstitutor', 'pleroma_reconstitution',
     "Pleroma drawn into thread and woven: a cloth of fullness, with no gap in it anywhere for anything to be missing."),
    (4, 'confluent_essence', 'Confluent Essence', 'pleroma_confluence', 'pleroma_confluence',
     "What settles in the basin where the five fluids meet, when the six materials beyond physics are steeped in them."),
    (4, 'eschaton_steel', 'Eschaton Steel', 'eschaton_foundry', 'eschaton_founding',
     "Steel founded at the density where one more atom would close a horizon round it: the last metal that is still a metal."),
    (4, 'convergent_matter', 'Convergent Matter', 'convergence_engine', 'convergence_automation',
     "Thirteen things brought to one point and not allowed to stay thirteen: matter with a single history where there were many."),
]
NAMES = {'dyson_array': 'Dyson Array', 'stellar_engine': 'Stellar Engine', 'matrioshka_brain': 'Matrioshka Brain', 'universe_forge': 'Universe Forge',
         'reality_compiler': 'Reality Compiler', 'omnilex_engine': 'Omnilex Engine', 'axiomatic_breach_engine': 'Axiomatic Breach Engine',
         'concept_compressor': 'Concept Compressor', 'cantor_diagonalizer': 'Cantor Diagonalizer', 'axiom_distiller': 'Axiom Distiller',
         'transcendent_plasma_forge': 'Transcendent Plasma Forge', 'eye_of_the_horizon': 'Eye of the Horizon', 'pleroma_reconstitutor': 'Pleroma Reconstitutor',
         'pleroma_confluence': 'Pleroma Confluence', 'eschaton_foundry': 'Eschaton Foundry', 'convergence_engine': 'Convergence Engine'}
for tier, iid, name, mid, rtype, lore in SIG:
    item(iid, name, lore + " Made in the %s, and nowhere else." % NAMES[mid])
TIER = {t: [s[1] for s in SIG if s[0] == t] for t in (2, 3, 4)}
ALLSIG = {ts(s[1]) for s in SIG}

# ====================================================================== the Pleroma Confluence: a machine with a console
PC = model([par('N', 'rev/s', 0.5, 6, 0.1, 1.0), par('Di', 'm', 0.3, 1.5, 0.05, 0.5), par('Q', 'L/s', 1, 50, 1, 10)],
           {'V': 2000},
           [der('Re', '', '1000000*N*Di^2', 'Reynolds number Re = ρND²/μ: over 10,000 the basin is turbulent'),
            der('tm', 's', '5.3/N*(1.5/Di)^2*(1 + 0.02*d)', 'blend time θ = 5.3/N·(T/D)²'),
            der('mix', '', 'V/Q/(5.3/N*(1.5/Di)^2*(1 + 0.02*d))', 'residence time V/Q over blend time: 3 or more and the five are one fluid before they leave'),
            der('P', 'kW', '5*N^3*Di^5', 'stirring power P = Np·ρ·N³·D⁵')],
           [con('shear', 'wear', '40 - 5*N^3*Di^5', 'under 40 kW, or the stirrer shears the Pleroma apart')],
           'clamp(Q/20, 0.2, 1.5)', 'more flow, more Pleroma')
machine('pleroma_confluence', 'Pleroma Confluence', "The controller of the confluence: five channels come down in terraces from five "
        "fonts, one each for Quintessence, Alkahest, Mathesis, Panacea and Anima Mundi, and meet in a stirred basin where they "
        "blend into Pleroma. A multiblock. Made on the Extreme Assembly Table.", 'flow', 7, PC,
        {'p.N': 'Stirrer speed', 'p.N.desc': 'Faster blends sooner, and takes power as the cube of the speed.',
         'p.Di': 'Stirrer width', 'p.Di.desc': 'Wider blends much sooner, and takes power as the fifth power of the width.',
         'p.Q': 'Flow', 'p.Q.desc': 'How fast the five fluids come down the channels: more leaves less time in the basin.',
         'd.Re': 'Reynolds number', 'd.Re.desc': 'Over 10,000 the basin is turbulent and the fluids fold into each other.',
         'd.tm': 'Blend time', 'd.tm.desc': 'How long the basin takes to become one fluid.',
         'd.mix': 'Blend margin', 'd.mix.desc': 'Time in the basin over blend time: it must be 3 or more.',
         'd.P': 'Stirring power', 'd.P.desc': 'Over 40 kW the stirrer tears the Pleroma apart.',
         'c.shear': 'The stirrer is shearing the Pleroma', 'v.V': 'Basin', 'disturb': 'Surges in the channels: blend time varies by ±2%·d.',
         'rate.desc': 'More flow, more Pleroma.'},
        json.load(open(os.path.join(D, 'recipe/crafting/pleroma_confluence.json'), encoding='utf-8')), multiblock=True)
FIVE = ['quintessence', 'alkahest', 'mathesis', 'panacea', 'anima_mundi']
mrec('pleroma_confluence', 'r171_pleroma', [], [(f, 1000) for f in FIVE], [], [('pleroma', 1000)], 65536, 200, 'pleroma', None,
     [('Re', 10000, 1e9), ('mix', 3, 1e6)], 'pleroma_confluence')
for k in ('gui.%s.confluence_needs' % NS, 'gui.%s.confluence_status' % NS):
    pass                                        # (their lines stay in the language file: harmless, and other packs may have translated them)
for f in ('models/block/pleroma_confluence.json',):
    p = os.path.join(A, f)
    if os.path.exists(p):
        os.remove(p)                            # the old block's model: the machine has its own on and off models now

# the three that were single blocks
for mid, lore in (
        ('cantor_diagonalizer', "The controller of a hall of digit columns: it lists every number, then writes down the diagonal one that is on no list. Manufactures infinities, and then bigger ones. A multiblock. Made on the Extreme Assembly Table."),
        ('axiom_distiller', "The controller of a seven-stage still: it boils reality down past its source code to the axioms underneath, and condenses them into fragments. A multiblock. Made on the Extreme Assembly Table."),
        ('convergence_engine', "The controller of a ring of thirteen altars round a column: it does what the Convergence Altar does with pedestals and patience, every ritual but the Breached Anomaly Engine, fed by pipes like any machine. A multiblock. Made on the Extreme Assembly Table.")):
    L['block.%s.%s.lore' % (NS, mid)] = lore
for kind in ('block', 'item'):
    tag_add(os.path.join(D, 'tags', kind, 'multiblock_controllers.json'), [ts(x) for x in S.NEW])
    tag_add(os.path.join(D, 'tags', kind, 'machines.json'), [ts('pleroma_confluence')])

# ====================================================================== the structures
S.build()

# ====================================================================== recipes
def load(path):
    return json.load(open(os.path.join(R, path), encoding='utf-8'))


def is_syn(i):
    return 'items' in i                         # a Field Synthesis of a set level (a components ingredient)


def plain(i):
    return i.get('item')


def signature(rtype, tpl, out, extra, keep=3, outs=None, drop=('item_outputs', 'fluid_outputs', 'optional_inputs', 'optional_fluid_inputs'), without=(), **over):
    """The structure's signature recipe, built on one of its own recipes (so its fluids, catalyst, research and
    console windows are ones the structure can already meet)."""
    t = load(os.path.join(rtype, tpl + '.json'))
    r = {k: copy.deepcopy(v) for k, v in t.items() if k not in drop}
    own = [i for i in t.get('item_inputs', []) if not is_syn(i) and plain(i) not in ALLSIG and plain(i) not in [ts(w) for w in without]]
    syn = [i for i in t.get('item_inputs', []) if is_syn(i)]
    plain_syn = [i for i in own if plain(i) and plain(i).split(':')[1] in ('unified_field_lattice', 'perennial_catalyst_matrix', 'proof_lattice',
                                                                               'living_archive', 'engineered_mind', 'planetary_survey', 'deep_record')]
    own = [i for i in own if i not in plain_syn]
    r['item_inputs'] = own[:keep] + plain_syn + syn + [{'item': ts(i), 'count': n} for i, n in extra]
    r['item_outputs'] = [{'id': ts(out), 'count': 1}] + [{'id': ts(i), 'count': n} for i, n in (outs or [])]
    r.update(over)
    assert len(r['item_inputs']) <= 16, (out, len(r['item_inputs']))
    dump(os.path.join(R, rtype, 'r171_' + out + '.json'), r)
    return r


E2, E3, E4 = TIER[2], TIER[3], TIER[4]
# cosmic engineering: two plain syntheses each (already in the recipes they are built on)
signature('dyson', 'stellar_core', 'dyson_lattice_panel', [('stellar_containment_plate', 4)], energy=8192, duration=24000)
r = signature('stellar_thrust', 'r140_stellar_thrust', 'stellar_wind_condensate', [('caplan_jet', 1)], drop=('item_outputs', 'optional_inputs', 'optional_fluid_inputs'),
              duration=24000)
T.RECIPES.append(('stellar_engine', 'r171_stellar_wind_condensate', r))
r = signature('matrioshka', 'r140_uploaded_minds', 'recursive_thought_shell', [('superintelligence_core', 1)])
signature('universe_forging', 'r32_pocket_universe_from_first_principles', 'embryonic_vacuum', [('cosmogenic_casing', 4)])
# beyond physics: two each of three of the four below
signature('reality_compilation', 'r34_reality_anchor_from_resolution', 'compiled_law', [(E2[0], 2), (E2[2], 2), (E2[3], 2)])
signature('omnilex_simulation', 'grow_bubble_universes', 'lex_prima', [(E2[1], 2), (E2[2], 2), (E2[3], 2)], without=['tree_simulacrum'], keep=4)
signature('reality_breach', 'open_breach', 'breach_shard', [(E2[0], 2), (E2[1], 2), (E2[3], 2)])
signature('concept_compression', 'r98_mathesis', 'pure_concept_ingot', [(E2[2], 2), (E2[0], 2), (E2[1], 2)])
signature('transfinite_computation', 'aleph_null_shard', 'diagonal_ingot', [(E2[2], 2), (E2[3], 2), (E2[0], 2)])
signature('axiom_distillation', 'law_crystal', 'distilled_axiom', [(E2[1], 2), (E2[3], 2), (E2[2], 2)])
# the final tier: three each of all six below
SIX = [(i, 3) for i in E3]
signature('transcendent_forging', 'transcendent_metal_ingot', 'transcendent_alloy', SIX, keep=4)
signature('horizon_simulation', 'simulate_cosmology', 'event_horizon_lens', SIX)
signature('pleroma_reconstitution', 'reconstitute_omnipotent_ingot', 'pleromatic_weave', SIX)
signature('eschaton_founding', 'r43_eschaton_cells', 'eschaton_steel', SIX)
signature('convergence_automation', 'omnilex_seal', 'convergent_matter', SIX, keep=4)
mrec('pleroma_confluence', 'r171_confluent_essence', SIX, [(f, 4000) for f in FIVE], [('confluent_essence', 1)], [], 2000000000, 2400, 'pleroma', None,
     [('Re', 10000, 1e9), ('mix', 3, 1e6)], 'pleroma_confluence')

# each tier's other recipes take one material of the tier below, in turn (not the Convergence Engine's: its parts are
# what everything else is built from; and not the runs that only give insight)
BELOW = {'reality_compilation': E2, 'omnilex_simulation': E2, 'reality_breach': E2, 'concept_compression': E2, 'transfinite_computation': E2,
         'axiom_distillation': E2, 'transcendent_forging': E3, 'horizon_simulation': E3, 'pleroma_reconstitution': E3, 'eschaton_founding': E3}
# A recipe may only take a material of the tier below if that material can be made without what the recipe makes:
# several of these structures also make everyday things (hypernova alloy, cosmic neutronium, aleph-null shards) that the
# tiers below are built from. So: work out how each signature material is first reached with no such inputs anywhere,
# and leave alone every recipe that makes something on the way to the materials it would take.
import check_progression as CP
recs = CP.recipes()
can, first, made, have, mach = CP.closure(recs, ignore=lambda r, k: k in ALLSIG and '/r171_' not in r['file'].replace(os.sep, '/'))
missing = [m for m in sorted(ALLSIG) if m not in can]
if missing:
    raise SystemExit('signature materials that cannot be made at all: %s' % missing)
EARLY = {('fluid:' + ts(g[6:])) if g.startswith('fluid:') else ts(g) for g in CP.GIVEN}       # things the check takes as given: never made dearer
ON_THE_WAY = {tuple(mats): CP.used(first, [ts(m) for m in mats]) | EARLY for mats in (E2, E3)}
added, kept, n = [], [], 0
for rtype, mats in BELOW.items():
    way = ON_THE_WAY[tuple(mats)]
    for f in sorted(glob.glob(os.path.join(R, rtype, '*.json'))):
        rid = os.path.basename(f)[:-5]
        r = json.load(open(f, encoding='utf-8'))
        if rid.startswith('r171_') or r.get('neoforge:conditions'):
            continue
        ins = r.setdefault('item_inputs', [])
        had = [i for i in ins if plain(i) in ALLSIG]
        outs = [o['id'] for o in r.get('item_outputs', [])] + ['fluid:' + o['id'] for o in r.get('fluid_outputs', [])]
        if not outs or any(o in way for o in outs) or len(ins) - len(had) >= 16:
            kept.append(rtype + '/' + rid)       # only insight, or something the tier below is built from: as it was
            if had:
                r['item_inputs'] = [i for i in ins if plain(i) not in ALLSIG]
                dump(f, r)
            continue
        n += 1
        want = ts(mats[n % len(mats)])
        if [plain(i) for i in had] != [want]:
            r['item_inputs'] = [i for i in ins if plain(i) not in ALLSIG] + [{'item': want, 'count': 1}]
            dump(f, r)
        added.append(rtype + '/' + rid)
print('recipes that take a material of the tier below: %d; left as they were: %d' % (len(added), len(kept)))
json.dump({'take_one': sorted(added), 'unchanged': sorted(kept)}, open(os.path.join(os.path.dirname(os.path.abspath(__file__)), 'r171_below.json'), 'w'), indent=0)

# 1.7.1 fix: the Wind Tunnel's Turning Vanes were made of the Aerofoil Composite only the Wind Tunnel makes
def vane(r):
    r['key']['I'] = {'item': ts('polymer_sheet')}
edit_json(os.path.join(R, 'crafting', 'r102_turning_vane.json'), vane)

# ====================================================================== the Codex
C = S.COUNTS
E = {}
E['materials/signature_materials'] = {'title': 'Signature Materials', 'icon': ts('compiled_law'), 'category': 'materials', 'research': 'femtoengineering', 'pages': [
    text("Every structure of the endgame makes one material that nothing else can make: its $(thing)signature$(). The four of cosmic engineering are made from plain Field Syntheses. Each of the six beyond physics takes two each of three of those four. Each of the six of the final tier takes three each of all six beyond physics."),
    text("The other recipes of each tier take one signature material of the tier below as well, so no tier can be run without the structures under it. The Convergence Engine's parts are the exception: they are what everything is built from, and cost what they always did."),
    spot(E2, "$(item)Dyson Lattice Panel$(): Dyson Array. $(item)Stellar Wind Condensate$(): Stellar Engine. $(item)Recursive Thought Shell$(): Matrioshka Brain. $(item)Embryonic Vacuum$(): Universe Forge.", 'Cosmic engineering'),
    spot(E3[:3], "$(item)Compiled Law$(): Reality Compiler. $(item)Lex Prima$(): Omnilex Engine. $(item)Breach Shard$(): Axiomatic Breach Engine.", 'Beyond physics (1)'),
    spot(E3[3:], "$(item)Pure Concept Ingot$(): Concept Compressor. $(item)Diagonal Ingot$(): Cantor Diagonalizer. $(item)Distilled Axiom$(): Axiom Distiller.", 'Beyond physics (2)'),
    spot(E4[:3], "$(item)Transcendent Alloy$(): Transcendent Plasma Forge. $(item)Event Horizon Lens$(): Eye of the Horizon. $(item)Pleromatic Weave$(): Pleroma Reconstitutor.", 'The final tier (1)'),
    spot(E4[3:], "$(item)Confluent Essence$(): Pleroma Confluence. $(item)Eschaton Steel$(): Eschaton Foundry. $(item)Convergent Matter$(): Convergence Engine.", 'The final tier (2)')]}


def mbpage(mid, where):
    return {'type': 'multiblock', 'name': NAMES[mid], 'text': where, 'multiblock': mid}


SHAPE_TEXT = {
    'stellar_engine': ("A Shkadov thruster, %d blocks: a great mirror dish behind the star, held on ribs from a spine, and a ring of Caplan jets ahead of it that turns as it burns. The thrust leaves through the ring.", "Interface Ports either side of the controller, at the front of the spine."),
    'dyson_array': ("%d blocks: a sphere of collector panels round the star, open between its ribs, an equatorial belt, and eight arms out to dishes that beam the power in.", "Interface Ports either side of the controller, at the foot of the mast."),
    'matrioshka_brain': ("%d blocks: four nested rings round the star, each tilted on its own axis like an armillary sphere, frame outside and the thinking layers inside. The innermost ring turns.", "Interface Ports either side of the controller, at the foot of the mast."),
    'universe_forge': ("%d blocks: a broad disc under a dome of glass that steps in as it rises, a coil round every step, and a spire through the crown. The new universe swells inside the dome.", "Interface Ports either side of the controller, in the rim of the disc."),
    'concept_compressor': ("%d blocks: a press line of nine bays, one for each idea fed in, each with a lens array over a ram that beats down on the bed.", "Interface Ports in the bed, either side of the controller."),
    'omnilex_engine': ("%d blocks: nineteen glass spires on a solid base of heartwood sheathed in casing, the tallest in the middle. Worlds rise as bubbles from every tip.", "Interface Ports either side of the controller, in the side of the base."),
    'reality_compiler': ("%d blocks: a tower whose plan changes at every stage, square, octagon, circle and diamond, then an open lantern of eight piers where the code is compiled.", "Interface Ports either side of the controller, at the foot of the first stage."),
    'axiomatic_breach_engine': ("%d blocks: twin towers on one plinth, lattice below and transfinite casing above, with a coil strip down each inner face. The breach is torn open in the gap between them.", "Interface Ports either side of the controller, in the front of the plinth."),
    'cantor_diagonalizer': ("%d blocks: a hall over a grid of eight rows of eight digit columns, roofed in a lattice of glass. The columns on the diagonal stand taller, and their heads flip as the machine writes.", "Interface Ports either side of the controller, in the front of the floor."),
    'axiom_distiller': ("%d blocks: a still of seven stages, a coil-wound boiler at its foot, a tray and a band of windows in every stage, a cooling collar at every joint and a condenser dome on top.", "Interface Ports either side of the controller, in the boiler's foot."),
    'pleroma_confluence': ("%d blocks: five channels come down in terraces from five fonts and meet in a round basin ringed with pylons.", "Interface Ports either side of the controller, at the front of the basin, between two channels."),
    'convergence_engine': ("%d blocks: thirteen altars in a ring on a round floor, a pedestal on each (Resonance, Harmonic or Absolute), a glass arch from every altar to the head of the column in the middle.", "Interface Ports either side of the controller, in the rim of the floor."),
}
ENTRY = {'stellar_engine': 'engineering/stellar_engines', 'dyson_array': 'stellar/dyson_array', 'matrioshka_brain': 'minds/matrioshka_brain',
         'universe_forge': 'strings/universe_forge', 'concept_compressor': 'endgame/concept_compressor', 'omnilex_engine': 'endgame/omnilex_engine',
         'reality_compiler': 'simulation/reality_compiler', 'axiomatic_breach_engine': 'endgame/axiomatic_breach_engine',
         'cantor_diagonalizer': 'endgame/cantor_diagonalizer', 'axiom_distiller': 'endgame/axiom_distiller', 'pleroma_confluence': 'endgame/pleroma',
         'convergence_engine': 'fabrication/convergence_engine'}
SIGOF = {s[3]: s[1] for s in SIG}
SIGNAME = {s[1]: s[2] for s in SIG}


def update(Eall):
    for mid, (shape, where) in SHAPE_TEXT.items():
        e = Eall[ENTRY[mid]]
        pages = [p for p in e['pages'] if not (p.get('type') == 'multiblock' and p.get('multiblock', '').split(':')[-1] == mid) and not p.get('r171')]
        for p in pages:                          # a structure now: no longer "a single-block machine"
            if p.get('type') == 'text' and 'a single-block machine' in p['text']:
                p['text'] = p['text'].replace('a single-block machine', 'a multiblock')
        if mid == 'stellar_engine':
            pages[0]['text'] = pages[0]['text'].replace('is 951 blocks', 'is %d blocks' % C[mid])
        if mid == 'pleroma_confluence':
            pages = [spot(['pleroma_confluence'], "Pleroma is made of five fluids, one for each branch of knowledge and one for the world: $(fluid)Quintessence$(), $(fluid)Alkahest$(), $(fluid)Mathesis$(), $(fluid)Panacea$() and $(fluid)Anima Mundi$() (see $(l)The Five Fluids of Pleroma$()). The $(item)Pleroma Confluence$() blends 1,000 mB of each into 1,000 mB of $(fluid)Pleroma$().", 'Pleroma'),
                     text("The old word means 'fullness': the totality of everything. Fitting. θ = 5.3/N·(T/D)²: the basin must blend the five before they leave it, so set the stirrer for a blend time a third of the time they spend there or less, and keep it turbulent.$(br2)Every branch I ever studied ends in this one basin."),
                     spot(['pleroma_confluence'], "Made on the $(item)Extreme Assembly Table$()."),
                     spot(['pleroma_cell'], "Pleroma carried in a $(item)Containment Cell$(), like every fluid of the mod: fill it at a tank, empty it into one.")]
        s = SIGOF[mid]
        extra = [dict(text(shape % C[mid]), r171=True), dict(mbpage(mid, where), r171=True),
                 dict(spot([s], "Its signature material is the $(item)%s$(): made here and nowhere else.$(br2)Made in $(item)%s$()." % (SIGNAME[s], NAMES[mid])), r171=True)]
        e['pages'] = pages[:1] + extra + pages[1:]
    for mid in ('transcendent_plasma_forge', 'eye_of_the_horizon', 'pleroma_reconstitutor', 'eschaton_foundry'):
        for k, e in Eall.items():
            if e.get('icon') == ts(mid) and k.split('/')[-1] == mid:
                e['pages'] = [p for p in e['pages'] if not p.get('r171')]
                s = SIGOF[mid]
                e['pages'].insert(1, dict(spot([s], "Its signature material is the $(item)%s$(): made here and nowhere else.$(br2)Made in $(item)%s$()." % (SIGNAME[s], NAMES[mid])), r171=True))


edit_json(os.path.join(A, 'ts_codex/entries.json'), update)
E['operation/pleroma_confluence'] = {'title': 'Pleroma Confluence', 'icon': ts('pleroma_confluence'), 'category': 'operation', 'research': 'pleroma',
                                     'pages': op_pages('pleroma_confluence', "Blend the five fluids into one before they leave the basin: turbulent, and with at least three blend times in the basin, without tearing the Pleroma apart.",
                                                       'Stirrer speed, Stirrer width, Flow', 'surges in the channels: blend time varies by ±2%·d.',
                                                       'over 40 kW of stirring shears the Pleroma (wear).', 'more flow faster.',
                                                       ['Re = ρND²/μ', 'θ = 5.3/N·(T/D)²', 'P = Np·ρ·N³·D⁵', 'Margin = (V/Q)/θ'])}
codex(E, {'materials': [{'entry': 'materials/signature_materials', 'x': 5, 'y': 1, 'parents': ['materials/field_syntheses']}],
          'machines': [{'entry': 'operation/pleroma_confluence', 'x': 40, 'y': 18, 'parents': []}]})
finish()
verify_recipes()
print('structures:', {k: v for k, v in C.items()})
