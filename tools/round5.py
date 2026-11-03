"""
Round 5 expansion data:
  * every dimension has a law and an attunement item; projected fields and the kit that protects against them
  * timed research, Research Dossiers and the Research Nexus
  * Alpha Omega: the Living Proof, the boss, the Proof of Everything, the secret research gating the Transcendental
  * long-capacity energy cells, Entangled Transceivers, Field Projectors and recipe environment conditions
  * the Codex: journal-voice rewrite, $(fluid) fixed, and a Modonomicon node-map edition
"""
import glob
import json
import math
import os
import re

import shapes


def _I(id, name, art, c1, c2, lore, glyph=None, **kw):
    d = dict(id=id, name=name, art=art, c1=c1, c2=c2, lore=lore)
    if glyph:
        d['glyph'] = glyph
    d.update(kw)
    return d


ITEMS = [
    # ---- dimension attunement
    _I('brane_tether', 'Brane Tether', 'anchor', '#201848', '#a080ff', 'Ties your mass to our brane. Carry it in the Bulk - or in any field of extra dimensions.', stack=1),
    _I('primordial_charter', 'Primordial Charter', 'codex', '#103028', '#60ffc0', 'A copy of the laws as they were first written. Carry it in Hyperdimensional Reality.', stack=1),
    _I('form_of_the_self', 'Form of the Self', 'artifact', '#d0d8ff', '#ffffff', 'Your own ideal form, cut in crystal. Carry it in the Platonic Realm.', stack=1),
    _I('coherence_stabiliser', 'Coherence Stabiliser', 'contained', '#102838', '#80f0ff', 'Keeps your wavefunction in one piece. Carry it in the Quantum Foam - or in an altered vacuum.', stack=1),
    _I('well_ordering_charm', 'Well-Ordering Charm', 'loop', '#202048', '#ffd060', 'Every descent ends somewhere. Carry it in the Transfinite Hierarchy.', stack=1),
    _I('identity_kernel', 'Identity Kernel', 'singularity', '#100810', '#ffffff', 'The smallest thing that is still you. Carry it in the Transcendental.', stack=1),
    _I('graviton_harness', 'Graviton Harness', 'damper', '#181830', '#8090ff', 'Spreads crushing gravity evenly through you. Carry it into high-gravity fields.', stack=1),
    _I('chronal_anchor', 'Chronal Anchor', 'clock', '#2a2008', '#ffc040', 'Holds your own clock steady. Carry it into fields where time runs fast or slow.', stack=1),
    # ---- Alpha Omega
    _I('proof_of_everything', 'Proof of Everything', 'omega', '#100818', '#ffe080', 'What Alpha Omega left behind: a proof that you understood. The Transcendental opens only to it.', stack=1),
    # ---- logistics
    _I('entanglement_tuner', 'Entanglement Tuner', 'probe', '#201030', '#c080ff', 'Links Entangled Transceivers to one channel. Sneak-use a transceiver to switch it between sending and receiving.', special='tuner', stack=1),
    # ---- research
    _I('empirical_dossier', 'Empirical Dossier', 'chip', '#203040', '#80c0ff', 'Every early discovery, written up. A Research Nexus can study it in your place.'),
    _I('theoretical_dossier', 'Theoretical Dossier', 'chip', '#302040', '#c080ff', 'The theories of the middle ages of your research. Feeds a Research Nexus.'),
    _I('unified_dossier', 'Unified Dossier', 'chip', '#403010', '#ffd060', 'Everything up to the edge of mathematics, unified. Feeds a Research Nexus.'),
    _I('absolute_dossier', 'Absolute Dossier', 'chip', '#101010', '#ffffff', 'The research of a civilisation that has nearly finished. Feeds a Research Nexus.'),
]
NOTE_TIERS = [('empirical', 'Empirical', '#203040', '#80c0ff'), ('theoretical', 'Theoretical', '#302040', '#c080ff'),
              ('unified', 'Unified', '#403010', '#ffd060'), ('absolute', 'Absolute', '#101010', '#ffffff')]
for _t, _n, _a, _b in NOTE_TIERS:
    for _k, _r in enumerate(('I', 'II', 'III')):
        ITEMS.append(_I(f'{_t}_notes_{_k + 1}', f'{_n} Notes {_r}', 'holo', _a, _b, 'One third of a Research Dossier.'))

BLOCKS = [
    dict(id='proof_wall', name='Living Proof', art='transcendent', c1='#140a1c', c2='#ffd060', kind='unbreakable', light=6, hardness=-1, tool='none'),
    dict(id='proof_floor', name='Ground of the Proof', art='polished', c1='#1a1028', c2='#806040', kind='unbreakable', light=4, hardness=-1, tool='none'),
    dict(id='proof_membrane', name='Membrane of the Proof', art='fieldglass', c1='#c080ff', c2='#ffffff', kind='proof_membrane', light=12, hardness=-1, tool='none'),
    dict(id='answer_pedestal', name='Answer Pedestal', art='omnicasing', c1='#302040', c2='#80f0ff', kind='proof_pedestal', light=10, hardness=-1, tool='none'),
    dict(id='axiom_toggle', name='Axiom Toggle', art='lamp', c1='#202838', c2='#80f0ff', kind='proof_toggle', light=3, hardness=-1, tool='none'),
    dict(id='axiom_lamp', name='Axiom Lamp', art='lamp', c1='#201028', c2='#ffd060', kind='proof_lamp', light=3, hardness=-1, tool='none'),
    dict(id='observer_plate', name='Observer Plate', art='rift', c1='#000000', c2='#80f0ff', kind='unbreakable', light=15, hardness=-1, tool='none'),
    dict(id='casimir_energy_cell', name='Casimir Energy Cell', art='coilblock', c1='#102838', c2='#40e0ff', kind='energy_cell', light=8, hardness=5.0, tool='iron'),
    dict(id='vacuum_energy_cell', name='Vacuum Energy Cell', art='coilblock', c1='#181040', c2='#a080ff', kind='energy_cell', light=8, hardness=5.0, tool='iron'),
    dict(id='singularity_energy_cell', name='Singularity Energy Cell', art='coilblock', c1='#100808', c2='#ff8040', kind='energy_cell', light=8, hardness=6.0, tool='diamond'),
    dict(id='omnium_energy_cell', name='Omnium Energy Cell', art='coilblock', c1='#102020', c2='#80ffe0', kind='energy_cell', light=10, hardness=8.0, tool='diamond'),
    dict(id='entangled_relay', name='Entangled Relay', art='rift', c1='#201038', c2='#c080ff', kind='transceiver', light=7, hardness=3.0, tool='iron'),
    dict(id='entangled_transceiver', name='Entangled Transceiver', art='rift', c1='#101830', c2='#80f0ff', kind='transceiver', light=10, hardness=4.0, tool='iron'),
    dict(id='graviton_field_projector', name='Graviton Field Projector', art='emitterblock', c1='#181830', c2='#8090ff', kind='field_projector', light=2, hardness=5.0, tool='iron'),
    dict(id='dimensional_lift_projector', name='Dimensional Lift Projector', art='emitterblock', c1='#201848', c2='#c080ff', kind='field_projector', light=2, hardness=5.0, tool='iron'),
    dict(id='chronal_field_projector', name='Chronal Field Projector', art='emitterblock', c1='#2a2008', c2='#ffc040', kind='field_projector', light=2, hardness=5.0, tool='iron'),
    dict(id='vacuum_state_projector', name='Vacuum State Projector', art='emitterblock', c1='#102838', c2='#80f0ff', kind='field_projector', light=2, hardness=5.0, tool='iron'),
]

FLUIDS = []

# ------------------------------------------------------------------ the Research Nexus
MACHINE_TEXT = {'research_nexus': ('Research Nexus', 'A ring of error-corrected cores around a well of light that reads Research Dossiers and '
                                                     'studies them in your place, shortening whatever you are researching.')}
MACHINE_CAT = {'research_nexus': 'information'}
MACHINE_RESEARCH = {'research_nexus': 'research_synthesis'}
TIER = {'research_nexus': 3}
MC = {'research_nexus': (['ABA', 'CDC', 'ABA'], {'A': 'precision_assembly_casing', 'B': 'quantum_processor', 'C': 'chronon_capacitor', 'D': 'research_terminal'})}
EXTRA = {'research_nexus': 'Feed it $(item)Research Dossiers$() while you are studying something: each one replaces minutes of waiting. '
                           'The dossier must be at least as advanced as the entry - empirical, theoretical, unified, absolute.'}


def research_nexus():
    """A reading room: a disc of assembly casing, a ring of error-correction cores, a glass dome over a well of light."""
    s = shapes.Shape(9, 7, 9)
    c = 4
    s.disc(c, c, 0, 4.3, 'a')
    for (x, z), _ in shapes.ring_cells(c, c, 3):
        s.put(x, 1, z, 'n')
    s.sphere(c, 2, c, 4.0, 'G', shell=0.6, filter=lambda x, y, z: y >= 2)
    s.put(c, 1, c, 'Q')
    s.sphere_air(c, 3, c, 3.0)
    s.center = (c, 3, c)
    s.ports('a', 'b', 4, y=0)
    s.v[(c, 1, c - 4)] = 'C'
    s.ctrl = (c, 1, c - 4)
    return s


shapes.BUILDERS['research_nexus'] = research_nexus

# ------------------------------------------------------------------ research
RESEARCH = [
    ('entangled_logistics', 'Entangled Logistics', 'Two particles, one state - and so one channel. Unlocks Entangled Relays, Transceivers and the Entanglement Tuner.',
     'entangled_relay', 4, 3, 25, ['quantum_entanglement', 'femtoengineering'], [('entangled_photon_pair', 4)]),
    ('vacuum_energy_storage', 'Vacuum Energy Storage', 'Store energy in the difference between two vacua. Unlocks the Energy Cells.',
     'casimir_energy_cell', 2, 3, 15, ['casimir_effect', 'cryogenics'], [('casimir_plate', 4)]),
    ('research_synthesis', 'Research Synthesis', 'Teach a machine to study. Unlocks the Research Nexus and Empirical Dossiers; later dossiers come with later research.',
     'research_nexus', 6, 6, 60, ['general_intelligence', 'assembly_lines'], [('quantum_processor', 2)]),
    ('field_engineering', 'Field Engineering', 'Gravity, dimension, time and the vacuum are all fields - and fields can be projected. Unlocks the Field Projectors, the Graviton Harness and the Chronal Anchor.',
     'graviton_field_projector', 11, 8, 120, ['general_relativity', 'hyperdimensional_geometry'], [('graviton_crystal', 4)]),
    ('conquered_alpha_omega', 'Alpha Omega', 'You understood. It let you go.', 'proof_of_everything', 21, 1, 0, ['omnilex'], []),
    ('beyond_the_omega', 'Beyond the Omega', 'With the Proof of Everything, the Omnilex Engine can be pushed past the possible - into the Transcendental.',
     'proof_of_everything', 22, 1, 2500, ['conquered_alpha_omega'], [('omega_point', 4)]),
]
HIDDEN = {'conquered_alpha_omega': 'grant_only', 'beyond_the_omega': 'hidden'}
CREATURES = {'alpha_omega': ('Alpha Omega', [])}

LANG = {
    # effects
    'effect.theoreticalsciences.dimensional_bleed': 'Dimensional Bleed', 'effect.theoreticalsciences.law_flux': 'Law Flux',
    'effect.theoreticalsciences.imperfection': 'Imperfection', 'effect.theoreticalsciences.dissolution': 'Dissolution',
    # dimension laws
    'message.theoreticalsciences.law_warning.the_bulk': 'Your mass is bleeding into the extra dimensions! Carry a %s.',
    'message.theoreticalsciences.law_warning.hyperdimensional_reality': 'The laws are rewriting themselves around you! Carry a %s.',
    'message.theoreticalsciences.law_warning.platonic_realm': 'Among perfect forms you are an imperfect copy - and wearing away. Carry the %s.',
    'message.theoreticalsciences.law_warning.quantum_foam': 'You are decohering! Carry a %s.',
    'message.theoreticalsciences.law_warning.transfinite_hierarchy': 'An infinite regress has begun. Carry a %s.',
    'message.theoreticalsciences.law_warning.black_hole_interior': 'Tidal forces are tearing you apart! You need a %s!',
    'message.theoreticalsciences.law_warning.white_hole_interior': 'Tidal forces are tearing you apart! You need a %s!',
    'message.theoreticalsciences.law_warning.transcendental': 'Surrounded by everything, you are dissolving. Carry an %s.',
    'message.theoreticalsciences.law.gravity_halved': 'A new law: gravity is halved.', 'message.theoreticalsciences.law.gravity_doubled': 'A new law: gravity is doubled.',
    'message.theoreticalsciences.law.light_slowed': 'A new law: light crawls.', 'message.theoreticalsciences.law.entropy_reversed': 'A new law: entropy runs backwards.',
    'message.theoreticalsciences.law.friction_lost': 'A new law: friction is gone.', 'message.theoreticalsciences.law.causality_loosened': 'A new law: causes come quickly.',
    'message.theoreticalsciences.black_hole_arrival': 'You are inside the black hole. An Einstein-Rosen bridge stands at 0, 121, 0 - it leads to the white hole.',
    # research
    'message.theoreticalsciences.research_started': 'Studying %s - it will take %s. A Research Nexus can speed it up.',
    'message.theoreticalsciences.research_started_s': '',
    'message.theoreticalsciences.research_busy': 'You are already studying something. Finish it first.',
    'message.theoreticalsciences.research_secret': 'This cannot be researched - only earned.',
    'message.theoreticalsciences.research_missing_prereq': 'Research its prerequisites first.',
    'message.theoreticalsciences.research_missing_items': 'You are missing the materials it costs.',
    'message.theoreticalsciences.research_unknown': 'Unknown research.',
    'message.theoreticalsciences.nexus_progress': 'Research Nexus: %s of study done for you',
    'gui.theoreticalsciences.studying': 'Studying: %s - %s left', 'gui.theoreticalsciences.studying_tip': 'Being studied - %s left',
    'gui.theoreticalsciences.nexus_tip': 'A Research Nexus can speed this up (dossier tier %s or better)',
    'gui.theoreticalsciences.cost_time': 'Takes %s of study', 'gui.theoreticalsciences.busy': 'Finish your current study first',
    'status.theoreticalsciences.no_active_research': 'Nothing being studied', 'status.theoreticalsciences.dossier_too_weak': 'Dossier tier too low',
    'status.theoreticalsciences.wrong_environment': 'Wrong environment', 'jei.theoreticalsciences.nexus_note': 'Shortens your current study',
    # environment & fields
    'env.theoreticalsciences.gravity_min': 'Gravity at least %s g', 'env.theoreticalsciences.gravity_max': 'Gravity at most %s g',
    'env.theoreticalsciences.dimensions_min': 'At least %s spatial dimensions', 'env.theoreticalsciences.time_min': 'Time running at least %sx',
    'env.theoreticalsciences.time_max': 'Time running at most %sx', 'env.theoreticalsciences.vacuum_min': 'Vacuum: %s or deeper',
    'env.theoreticalsciences.vacuum.1': 'Casimir-depleted', 'env.theoreticalsciences.vacuum.2': 'false vacuum', 'env.theoreticalsciences.vacuum.3': 'true vacuum',
    'jei.theoreticalsciences.env_label': 'Needs %s field conditions', 'jei.theoreticalsciences.env_title': 'Around the controller:',
    'jei.theoreticalsciences.env_hint': 'Project them with Field Projectors, or build where the dimension already has them.',
    'field.theoreticalsciences.gravity': 'Graviton field: %s g (%s FE/t) - %s', 'field.theoreticalsciences.dimension': 'Dimensional lift: %s dimensions (%s FE/t) - %s',
    'field.theoreticalsciences.chronal': 'Chronal field: time x%s (%s FE/t) - %s', 'field.theoreticalsciences.vacuum': 'Vacuum state: level %s (%s FE/t) - %s',
    'field.theoreticalsciences.on': 'running', 'field.theoreticalsciences.off': 'unpowered',
    'message.theoreticalsciences.crushing': 'You are being crushed! Carry a Graviton Harness.',
    'message.theoreticalsciences.cell': 'Stored %s / %s FE (moves %s FE/t)',
    'message.theoreticalsciences.tx_send': 'Transceiver: sending (pulls from what it sits on)', 'message.theoreticalsciences.tx_receive': 'Transceiver: receiving (pushes into what it sits on)',
    'message.theoreticalsciences.tx_copied': 'Tuner holds channel %s', 'message.theoreticalsciences.tx_linked': 'Tuned to channel %s', 'message.theoreticalsciences.tx_cleared': 'Tuner cleared',
    'tooltip.theoreticalsciences.tuner_empty': 'No channel stored', 'tooltip.theoreticalsciences.tuner_freq': 'Channel %s',
    'jei.theoreticalsciences.layer_up': 'Next layer', 'jei.theoreticalsciences.layer_down': 'Previous layer',
    'jei.theoreticalsciences.port_legend': 'Show Interface Port positions',
    # Alpha Omega
    'entity.theoreticalsciences.alpha_omega': 'Alpha Omega', 'entity.theoreticalsciences.alpha_omega_genesis': 'Alpha Omega',
    'biome.theoreticalsciences.living_proof': 'The Living Proof',
    'proof.theoreticalsciences.voice': 'Alpha Omega: "%s"',
    'boss.theoreticalsciences.alpha_omega.genesis_1': 'I am what your engine was made of: every law you broke apart to build it.',
    'boss.theoreticalsciences.alpha_omega.genesis_2': 'You took the universe to pieces. Do you understand any of them?',
    'boss.theoreticalsciences.alpha_omega.genesis_3': 'Why does a clock slow down? What is a measurement? How many infinities are there?',
    'boss.theoreticalsciences.alpha_omega.genesis_4': 'And why did you do it - to make, to know, or to be?',
    'boss.theoreticalsciences.alpha_omega.genesis_5': 'Come inside. Show me what you understand.',
    'boss.theoreticalsciences.alpha_omega.proof_1': 'Enough force. Answer me this.',
    'boss.theoreticalsciences.alpha_omega.proof_2': 'One last question. Everything depends on it.',
    'boss.theoreticalsciences.alpha_omega.shielded': 'It has closed itself into a proof. Answer at the pedestals.',
    'proof.theoreticalsciences.enter_title': 'The Living Proof', 'proof.theoreticalsciences.enter_sub': 'Understand where you are, and it will let you go.',
    'proof.theoreticalsciences.chamber_n': 'Law %s of %s', 'proof.theoreticalsciences.question_n': 'Correct answers: %s / %s (a wrong answer costs two)',
    'proof.theoreticalsciences.right': 'Correct.', 'proof.theoreticalsciences.wrong': 'Wrong - the proof bites back.',
    'proof.theoreticalsciences.yields': 'The law yields', 'proof.theoreticalsciences.qed': 'Q.E.D.', 'proof.theoreticalsciences.qed_sub': 'The proof is complete. It begins to melt.',
    'proof.theoreticalsciences.boss_sub': 'Every concept, rule and dimension - in one.',
    'proof.theoreticalsciences.won': 'Understood.', 'proof.theoreticalsciences.won_sub': 'Alpha Omega lets you go - and leaves you its proof.',
    'proof.theoreticalsciences.failed.died': 'The proof closes over you. The Omnilex will find its way back to your hands.',
    'proof.theoreticalsciences.failed.left': 'You left the proof unfinished. The Omnilex will find its way back to your hands.',
    'proof.theoreticalsciences.returned': 'The Omnilex returns to you.', 'proof.theoreticalsciences.already': 'Alpha Omega is already examining you.',
    'proof.theoreticalsciences.diagonal.submit': 'Touch to submit your number', 'proof.theoreticalsciences.diagonal.yours': 'Your number:',
    'proof.theoreticalsciences.diagonal.number': 'Number %s', 'proof.theoreticalsciences.diagonal.digit': 'digit %s',
    'proof.theoreticalsciences.diagonal.steps': 'HOW TO ESCAPE\n1. On the wall, lit lamps are 1 and dark lamps are 0. Each row is one number.\n'
                                           '2. The gold [digits] run down the diagonal: digit 1 of Number 1, digit 2 of Number 2 ... digit 6 of Number 6.\n'
                                           '3. Flip your switches below so each of YOUR digits is the OPPOSITE of the gold digit in the same column.\n'
                                           '4. Touch the pedestal. Your number now differs from every number on the wall, so it cannot be on the list.',
    'proof.theoreticalsciences.diagonal.wrong': 'The list already knows that number - or one it cannot tell apart from it. Try again.',
    'proof.theoreticalsciences.measurement.observed': 'You looked at it. It is somewhere else now.',
    'proof.theoreticalsciences.proper_time.clock': 'Proper time: %s / %s s',
    'message.theoreticalsciences.meltdown': 'CONTAINMENT FAILING: %s - get clear!',
}
CHAMBERS = {
    'measurement': ('The Measurement Problem', 'Step on the exit plate. Here, whatever is observed collapses somewhere else.',
                    'Looking is not innocent.', 'Observation changes the observed. Measure nothing you want to keep.'),
    'proper_time': ('Proper Time', 'Live through 20 seconds of your own time. Your clock only runs while you are at rest. The singularity strikes where you stand.',
                    'Moving clocks run slow.', 'Moving clocks run slow - and the still ones are easy targets.'),
    'diagonal': ('The Diagonal', 'Six numbers hang on the wall. Make a seventh that no list could contain. Cantor needed only one line to do it.',
                 'Some infinities cannot be listed.', 'Change every diagonal digit and no list can contain you.'),
    'hotel': ("Hilbert's Hotel", 'Answer five questions about infinity at the pedestals.', 'A full infinite hotel always has room.', 'Infinity can be full and still have room.'),
    'forms': ('The Platonic Forms', 'Answer five questions about shape and dimension at the pedestals.', 'Perfection is rarer than you think.', 'Only five solids are perfect.'),
    'bulk': ('The Bulk', 'Answer five questions about spacetime and its extra dimensions at the pedestals.', 'Gravity leaks.', 'Our space is a brane in something larger.'),
    'foundations': ('Foundations', 'Answer five questions about quanta, information and proof at the pedestals.', 'Even logic has limits.', 'Some truths cannot be proven.'),
    'codex': ('The Codex', 'Answer five questions about what you have done to get here.', 'You were here all along.', 'Everything you built was written down.'),
}
for k, (t, rule, voice, lesson) in CHAMBERS.items():
    LANG[f'proof.theoreticalsciences.{k}.title'] = t
    LANG[f'proof.theoreticalsciences.{k}.rule'] = rule
    LANG[f'proof.theoreticalsciences.{k}.voice'] = voice
    LANG[f'proof.theoreticalsciences.{k}.lesson'] = lesson

# ------------------------------------------------------------------ environment conditions (recipe name -> conditions)
ENV = {
    # only recipes researched after Field Engineering, or made where the dimension supplies the condition by nature
    'hopf_fibration': {'dimensions_min': 4},
    'penteract_lattice': {'dimensions_min': 5},
    'e8_lattice': {'dimensions_min': 8},
    'm_theory_core': {'dimensions_min': 5},
    'closed_timelike_curve': {'time_min': 10},
    'eternity_catalyst': {'time_max': 0.01},
    'false_vacuum_from_membrane': {'vacuum_min': 2},
    'wavefunction': {'vacuum_min': 1},
    'quantum_singularity': {'vacuum_min': 3},
    'spacetime_singularity': {'gravity_min': 1000},
    'dimensional_singularity': {'dimensions_min': 11},
    'temporal_singularity': {'time_max': 0.01},
    'theory_of_everything': {'gravity_min': 100, 'dimensions_min': 11, 'vacuum_min': 2},
    'hypernova_alloy_ingot': {'gravity_min': 100},
    'transcendent_metal_ingot': {'gravity_min': 1000, 'time_min': 10},
}

ATTUNE = {'brane_tether': 'brane_cosmology', 'primordial_charter': 'hyperdimensional_reality', 'form_of_the_self': 'platonic_realm',
          'coherence_stabiliser': 'quantum_foam', 'well_ordering_charm': 'transfinite_hierarchy', 'identity_kernel': 'beyond_the_omega'}


def _free(taken, x, y):
    for r in range(0, 6):
        for dx in range(-r, r + 1):
            for dy in range(-r, r + 1):
                if max(abs(dx), abs(dy)) == r and (x + dx, y + dy) not in taken:
                    return x + dx, y + dy
    return x, y + 7


def apply(g):
    w, q, D, A, WG, REC = g['w'], g['q'], g['D'], g['A'], g['WG'], g['REC']
    mr, shaped, shapeless, text_entry = g['mr'], g['shaped'], g['shapeless'], g['text_entry']
    NS = 'theoreticalsciences'
    g['LANG'].update(LANG)
    MR = g['MACHINE_RECIPES']
    DEFS = g['DEFS']

    # ================================================================== research: positions, timing, secrets
    RD = os.path.join(D, 'ts_research')
    files = sorted(glob.glob(os.path.join(RD, '*.json')))
    data = {os.path.basename(f)[:-5]: json.load(open(f)) for f in files}
    taken = {}
    for rid in sorted(data, key=lambda r: (data[r]['insight'], r)):
        p = (data[rid]['x'], data[rid]['y'])
        if p in taken:
            p = _free(set(taken), *p)
            data[rid]['x'], data[rid]['y'] = p
        taken[p] = rid
    for rid, d in data.items():
        ins = d['insight']
        # later research takes real time to study: about 5 minutes at 100 insight, half an hour at 400, two hours at 1000
        d['time'] = 0 if ins < 70 else int(round(0.6 * ins ** 1.35 / 30.0)) * 30
        if HIDDEN.get(rid) == 'grant_only':
            d['grant_only'] = True
            d['hidden'] = True
            d['time'] = 0
        elif HIDDEN.get(rid) == 'hidden':
            d['hidden'] = True
        w(os.path.join(RD, rid + '.json'), d)

    # ================================================================== items: attunement, protection, the Proof
    shaped('brane_tether', ['GMG', 'NCN', 'GMG'], {'G': 'graviton_crystal', 'M': 'metric_tensor', 'N': 'neutronium_plate', 'C': 'chronon_capacitor'}, 'brane_tether')
    shaped('primordial_charter', ['ARA', 'RSR', 'ARA'], {'A': 'axiom_fragment', 'R': 'reality_anchor', 'S': 'source_code_fragment'}, 'primordial_charter')
    shaped('form_of_the_self', ['QDQ', 'DLD', 'QDQ'], {'Q': 'cherenkov_quartz', 'D': 'quantum_dot', 'L': 'cherenkov_lattice'}, 'form_of_the_self')
    shaped('coherence_stabiliser', ['QEQ', 'ESE', 'QEQ'], {'Q': 'quantum_field_lattice', 'E': 'entangled_photon_pair', 'S': 'spin_foam_lattice'}, 'coherence_stabiliser')
    shaped('well_ordering_charm', ['PAP', 'ASA', 'PAP'], {'P': 'power_set', 'A': 'aleph_null_shard', 'S': 'surreal_number'}, 'well_ordering_charm')
    shaped('identity_kernel', ['OPO', 'PCP', 'OPO'], {'O': 'omega_point', 'P': 'ontological_processor', 'C': 'cognitive_singularity'}, 'identity_kernel')
    shaped('graviton_harness', ['N N', 'GDG', 'NGN'], {'N': 'neutronium_plate', 'G': 'graviton_crystal', 'D': 'inertia_damper'}, 'graviton_harness')
    shaped('chronal_anchor', ['CTC', 'TAT', 'CTC'], {'C': 'chronon_capacitor', 'T': 'temporal_knot', 'A': 'arrow_of_time_shard'}, 'chronal_anchor')
    # ---- energy cells, transceivers, projectors
    shaped('casimir_energy_cell', ['PCP', 'CBC', 'PCP'], {'P': 'casimir_plate', 'C': 'chronon_capacitor', 'B': 'chronosteel_block'}, 'casimir_energy_cell')
    shaped('vacuum_energy_cell', ['CXC', 'GKG', 'CXC'], {'C': 'casimir_energy_cell', 'X': 'exotic_matter_crystal', 'G': 'graviton_crystal', 'K': 'dark_matter_lens'}, 'vacuum_energy_cell')
    shaped('singularity_energy_cell', ['VMV', 'MBM', 'VMV'], {'V': 'vacuum_energy_cell', 'M': 'micro_singularity', 'B': 'cosmic_neutronium_block'}, 'singularity_energy_cell')
    shaped('omnium_energy_cell', ['SOS', 'OWO', 'SOS'], {'S': 'singularity_energy_cell', 'O': 'omnium_ingot', 'W': 'omega_point'}, 'omnium_energy_cell')
    shaped('entangled_relay', ['QEQ', 'ECE', 'QEQ'], {'Q': 'quantum_dot', 'E': 'entangled_photon_pair', 'C': 'chronon_capacitor'}, 'entangled_relay', 2)
    shaped('entangled_transceiver', ['RTR', 'WPW', 'RTR'], {'R': 'entangled_relay', 'T': 'tesseract_core', 'W': 'wormhole_throat', 'P': 'cosmic_processor'}, 'entangled_transceiver', 2)
    shaped('entanglement_tuner', ['  E', ' Q ', 'N  '], {'E': 'entangled_photon_pair', 'Q': 'quantum_dot', 'N': 'neutronium_plate'}, 'entanglement_tuner')
    for pid, core in (('graviton_field_projector', 'graviton_crystal'), ('dimensional_lift_projector', 'tesseract_core'),
                      ('chronal_field_projector', 'temporal_knot'), ('vacuum_state_projector', 'exotic_matter_crystal')):
        shaped(pid, ['EFE', 'FCF', 'SFS'], {'E': 'graviton_field_emitter', 'F': 'field_emitter', 'C': core, 'S': 'spacetime_casing'}, pid)

    # ================================================================== dossiers: made from everything researched so far
    ins_of = {rid: DEFS[rid][6] for rid in DEFS}
    ins_of.update({r[0]: r[6] for r in RESEARCH})
    skip = re.compile(r'(casing|_block|coil|glass|_ore$|singularity$|_cell$|relay|transceiver|projector|dossier|notes|_slab|_stairs|_wall|_pillar|_lamp|panel|tiles|bricks|plating)')
    machine_ids = {m['id'] for m in g['M']} if isinstance(g.get('M'), list) else set(g.get('M') or {})
    bands = [(5, 70, 'research_synthesis', 'superfluid_helium', 4000, 1),
             (70, 150, 'superintelligence', 'exotic_matter', 4000, 2),
             (150, 400, 'conceptual_singularities', 'synaptic_fluid', 8000, 3),
             (400, 1000, 'omnilex', 'primordial_law', 4000, 4)]
    for (lo, hi, gate, fluid, famt, tier), (tname, _, _, _) in zip(bands, NOTE_TIERS):
        cands = []
        for rec in MR:
            rid = rec.get('research')
            if not rid or rid not in ins_of or not (lo <= ins_of[rid] < hi) or not rec['outputs']:
                continue
            out = rec['outputs'][0]
            if ':' in out or out in machine_ids or skip.search(out) or out in cands:
                continue
            cands.append((ins_of[rid], out))
        best = {}
        for lvl, o in cands:
            best[o] = max(best.get(o, 0), lvl)
        picked = [o for o, _ in sorted(best.items(), key=lambda t: (-t[1], t[0]))]
        if len(picked) > 27:
            step = len(picked) / 27.0
            picked = [picked[int(i * step)] for i in range(27)]
        for k in range(3):
            part = picked[k::3][:9]
            mr(f'{tname}_notes_{k + 1}', 'femto_assembly', [(x, 4) for x in part], [(fluid, famt)], [(f'{tname}_notes_{k + 1}', 1)], [],
               1 << (18 + tier * 2), 600 + 200 * tier, gate)
        ins = [(f'{tname}_notes_{k + 1}', 2) for k in range(3)]
        if tier > 1:
            ins.append((NOTE_TIERS[tier - 2][0] + '_dossier', 1))
        mr(f'{tname}_dossier', 'femto_assembly', ins, [(fluid, famt * 2)], [(f'{tname}_dossier', 1)], [], 1 << (19 + tier * 2), 800 + 200 * tier, gate)
        mr(f'nexus_{tname}', 'research_synthesis', [(f'{tname}_dossier', 1)], [], [], [], [0, 1_000_000, 8_000_000, 64_000_000, 256_000_000][tier], 200,
           'research_synthesis')
    g['MACHINE_RECIPES'] = MR

    # ================================================================== the Transcendental now needs the Proof
    imp = glob.glob(os.path.join(REC, '**', 'the_impossible.json'), recursive=True)
    for f in imp:
        r = json.load(open(f))
        # four input slots: the Proof takes the Omega Points' place
        r['item_inputs'] = [i for i in r.get('item_inputs', []) if i.get('item') not in (q('proof_of_everything'), q('omega_point'))] + \
                           [{'item': q('proof_of_everything'), 'count': 1}]
        r['research'] = q('beyond_the_omega')
        w(f, r)

    # ================================================================== worldgen: the bridge, the Living Proof
    g['custom_feature']('einstein_rosen_bridge', 'einstein_rosen_bridge', [])
    for b in ('black_hole_interior', 'white_hole_interior'):
        p = os.path.join(WG, 'biome', b + '.json')
        bj = json.load(open(p))
        if q('einstein_rosen_bridge') not in bj['features'][0]:
            bj['features'][0] = [q('einstein_rosen_bridge')] + bj['features'][0]
        w(p, bj)
    w(os.path.join(WG, 'biome', 'living_proof.json'), {
        'has_precipitation': False, 'temperature': 0.5, 'downfall': 0.0,
        'effects': {'sky_color': 0x100410, 'fog_color': 0x1A081A, 'water_color': 0x80F0FF, 'water_fog_color': 0x1A081A,
                    'ambient_sound': 'minecraft:ambient.soul_sand_valley.loop', 'particle': {'options': {'type': 'minecraft:end_rod'}, 'probability': 0.004}},
        'spawners': {}, 'spawn_costs': {}, 'carvers': {}, 'features': []})
    w(os.path.join(D, 'dimension_type', 'living_proof.json'), {
        'ultrawarm': False, 'natural': False, 'piglin_safe': False, 'respawn_anchor_works': False, 'bed_works': False, 'has_raids': False,
        'has_skylight': False, 'has_ceiling': False, 'coordinate_scale': 1.0, 'ambient_light': 0.6, 'logical_height': 256,
        'effects': q('living_proof'), 'infiniburn': '#minecraft:infiniburn_end', 'min_y': 0, 'height': 256, 'fixed_time': 18000,
        'monster_spawn_light_level': 0, 'monster_spawn_block_light_limit': 0})
    w(os.path.join(D, 'dimension', 'living_proof.json'), {'type': q('living_proof'), 'generator': {
        'type': 'minecraft:flat', 'settings': {'layers': [], 'biome': q('living_proof'), 'lakes': False, 'features': False, 'structure_overrides': []}}})
    w(os.path.join(D, 'advancement', 'alpha_omega.json'), {
        'display': {'icon': {'id': q('proof_of_everything')}, 'title': 'Alpha and Omega', 'description': 'Understand everything - and survive it',
                    'frame': 'challenge', 'show_toast': True, 'announce_to_chat': True, 'hidden': True},
        'parent': 'minecraft:end/root', 'criteria': {'got': {'trigger': 'minecraft:inventory_changed', 'conditions': {'items': [{'items': q('proof_of_everything')}]}}},
        'requirements': [['got']]})

    # ================================================================== blockstates / models for the new kinds of block
    BS, BM, IM = os.path.join(A, 'blockstates'), os.path.join(A, 'models', 'block'), os.path.join(A, 'models', 'item')
    for b in BLOCKS:
        bid = b['id']
        if b['kind'] in ('proof_toggle', 'proof_lamp'):
            w(os.path.join(BM, bid + '_on.json'), {'parent': 'minecraft:block/cube_all', 'textures': {'all': f'{NS}:block/{bid}_on'}})
            w(os.path.join(BS, bid + '.json'), {'variants': {'lit=false': {'model': f'{NS}:block/{bid}'}, 'lit=true': {'model': f'{NS}:block/{bid}_on'}}})
        elif b['kind'] == 'energy_cell':
            w(os.path.join(BM, bid + '.json'), {'parent': 'minecraft:block/orientable',
                                               'textures': {'front': f'{NS}:block/{bid}_front', 'side': f'{NS}:block/{bid}', 'top': f'{NS}:block/{bid}'}})
            rot = {'north': {}, 'east': {'y': 90}, 'south': {'y': 180}, 'west': {'y': 270}, 'up': {'x': 270}, 'down': {'x': 90}}
            w(os.path.join(BS, bid + '.json'), {'variants': {f'facing={f}': dict(model=f'{NS}:block/{bid}', **r) for f, r in rot.items()}})
        elif b['kind'] == 'transceiver':
            tex = f'{NS}:block/{bid}'
            w(os.path.join(BM, bid + '.json'), {'parent': 'minecraft:block/block', 'textures': {'particle': tex, 't': tex},
                                               'elements': [{'from': [3, 0, 3], 'to': [13, 6, 13], 'faces': {f: {'texture': '#t', **({'cullface': 'down'} if f == 'down' else {})}
                                                                                                             for f in ('down', 'up', 'north', 'south', 'east', 'west')}},
                                                            {'from': [6, 6, 6], 'to': [10, 9, 10], 'faces': {f: {'texture': '#t'} for f in ('up', 'north', 'south', 'east', 'west')}}]})
            rot = {'down': {}, 'up': {'x': 180}, 'south': {'x': 90}, 'west': {'x': 90, 'y': 90}, 'north': {'x': 90, 'y': 180}, 'east': {'x': 90, 'y': 270}}
            w(os.path.join(BS, bid + '.json'), {'variants': {f'enabled={e},facing={f}': dict(model=f'{NS}:block/{bid}', **r)
                                                             for f, r in rot.items() for e in ('true', 'false')}})
            w(os.path.join(IM, bid + '.json'), {'parent': f'{NS}:block/{bid}'})
        if b['kind'] == 'energy_cell':
            w(os.path.join(D, 'loot_table', 'blocks', bid + '.json'), {'type': 'minecraft:block', 'pools': [{'rolls': 1, 'entries': [{
                'type': 'minecraft:item', 'name': q(bid), 'functions': [{'function': 'minecraft:copy_components', 'source': 'block_entity',
                                                                          'include': ['minecraft:custom_data']}]}],
                'conditions': [{'condition': 'minecraft:survives_explosion'}]}]})

    # ================================================================== the Codex: new entries
    text_entry('basics', 'research_time', 'Patience, Automated', 'research_nexus', [
        'Later research is not bought - it is studied. Once paid for, an entry takes real time: minutes at first, hours near the end. You can study one thing at a time; the Research Terminal shows how long is left.',
        'A $(item)Research Nexus$() studies in your place. Feed it $(item)Research Dossiers$() while you study and each one cuts the wait: Empirical 2 min, Theoretical 5, Unified 10, Absolute 20.',
        'A dossier must be at least as advanced as the entry: $(thing)Empirical$() up to 150 Insight, $(thing)Theoretical$() up to 400, $(thing)Unified$() up to 1000, $(thing)Absolute$() beyond. Each is assembled from three sets of Notes, and those from a broad sample of everything you have researched before - a test of your whole production line.'],
        adv='research_synthesis', sort=-1, spot=['research_nexus', 'empirical_dossier'])
    text_entry('world', 'dimension_laws', 'Laws of Elsewhere', 'brane_tether', [
        'Every other place has its own law, and the law is not kind to visitors. Carry the right $(thing)attunement$() anywhere in your inventory and you will live - with a reminder of where you are.',
        '$(li)$(item)Brane Tether$(), the Bulk: without it, $(thing)Dimensional Bleed$(); with it, half gravity and twice the knockback.$(li)$(item)Primordial Charter$(), Hyperdimensional Reality: without it a hostile new law every few seconds; with it, a new law (good or bad) every minute and a half.',
        '$(li)$(item)Form of the Self$(), the Platonic Realm: without it, $(thing)Imperfection$(); with it, only a $(thing)sword$() can dig - pickaxes barely scratch the ideal.$(li)$(item)Coherence Stabiliser$(), the Quantum Foam: without it, constant $(thing)Decoherence$(); with it, you still blink a few blocks now and then.',
        '$(li)$(item)Well-Ordering Charm$(), the Transfinite Hierarchy: without it, $(thing)Infinite Regress$() deepens every ten seconds; with it, Regress I and hunger.$(li)$(item)Geodesic Anchor$(), both holes: without it, death in two seconds. In the white hole gravity points $(thing)up$() - sneak to fall.$(li)$(item)Identity Kernel$(), the Transcendental: without it, $(thing)Dissolution$().'],
        sort=1, spot=['brane_tether', 'form_of_the_self', 'coherence_stabiliser', 'well_ordering_charm'])
    text_entry('basics', 'energy_logistics', 'Moving Mountains of Energy', 'entangled_transceiver', [
        'Machines now hold at least twenty seconds of whatever their recipe draws, and take power as fast as it arrives. Arriving is the hard part. $(item)Energy Cells$() store it: every face takes energy in, the marked face sends it out.',
        '$(li)Casimir: 4 billion FE, 16 M FE/t$(li)Vacuum: 256 billion, 512 M FE/t$(li)Singularity: 16 trillion, 2.1 billion FE/t$(li)Omnium: effectively bottomless$(br2)Cells keep their charge when broken.',
        '$(item)Entangled Relays$() and $(item)Transceivers$() sit on a block\'s face. Tune several to one channel with an $(item)Entanglement Tuner$(): senders pull items, fluids and energy out of what they sit on and receivers push it in - instantly, at any distance, even between dimensions.',
        'Relay: 256 items, 64,000 mB and 64 M FE per tick. Transceiver: 8,192 items, 4,096,000 mB and 2.1 billion FE per tick. Sneak-use with the tuner to switch sending/receiving.'],
        adv='entangled_logistics', sort=1, spot=['entangled_transceiver', 'casimir_energy_cell', 'entanglement_tuner'])
    text_entry('basics', 'fields', 'Projected Fields', 'graviton_field_projector', [
        'Some recipes only run in the right conditions around the controller - heavy gravity, extra dimensions, slowed or hastened time, an altered vacuum. JEI marks them with a diamond.',
        'Some dimensions supply conditions by nature: the Bulk has 5 dimensions, the Transfinite Hierarchy 7, Hyperdimensional Reality 11; inside a black hole gravity is 1000 g and time runs at 1%; the Foam\'s vacuum is false. Anywhere else, $(item)Field Projectors$() make them within 16 blocks.',
        'Right-click a projector to step its setting up, sneak to step down; it needs power to hold the field. $(item)Graviton$(): 0.01 to 1000 g. $(item)Dimensional Lift$(): 4 to 11 dimensions. $(item)Chronal$(): time x0.01 to x100. $(item)Vacuum State$(): Casimir, false, true vacuum.',
        'Fields act on you too. High gravity crushes ($(item)Graviton Harness$()), extra dimensions bleed you ($(item)Brane Tether$()), altered time slows or ages you ($(item)Chronal Anchor$()) and an altered vacuum decays you ($(item)Coherence Stabiliser$()). Prepare before you walk into your own factory.'],
        adv='field_engineering', sort=2, spot=['graviton_field_projector', 'graviton_harness', 'chronal_anchor'])
    text_entry('endgame', 'omnilex_memory', 'Where It Was Born', 'omnilex', [
        'A note scrawled in the margin, in a hand that might be mine: $(thing)the Omnilex remembers where it began - in the foam beneath everything.$() I do not know what that means. I do not want to find out.'],
        adv='omnilex', sort=20)
    text_entry('endgame', 'alpha_omega', 'Alpha Omega', 'proof_of_everything', [
        'I threw the Omnilex into the Quantum Foam. It shattered into everything I had ever made, and the pieces became a shape with too many directions. It asked me whether I understood.',
        'It took me into itself - $(thing)the Living Proof$(), eight chambers, each a law that had to be understood before it opened. Then the proof melted, and it fought me as a creature, closing itself into questions twice.',
        'It left me the $(item)Proof of Everything$(). With it and $(thing)Beyond the Omega$() researched, the Omnilex Engine can do the impossible - and open the way to the Transcendental. Dying in the proof costs nothing but the attempt.'],
        adv='conquered_alpha_omega', sort=21, spot=['proof_of_everything'])
    text_entry('mathematics', 'research_nexus_machine', 'The Research Nexus', 'research_nexus', [
        'A reading room for a machine: error-corrected cores in a ring around a well of light, under a glass dome. Give it power and Research Dossiers; it shortens whatever its owner is studying.'],
        adv='research_synthesis', sort=30, spot=['research_nexus'])


def finish(g):
    """After every entry exists: the journal rewrite, the $(fluid) macro, environment conditions and the Modonomicon edition."""
    w, q, D, A, LANG = g['w'], g['q'], g['D'], g['A'], g['LANG']
    NS = 'theoreticalsciences'
    BA = g['BA']
    here = os.path.dirname(os.path.abspath(__file__))

    # ---- environment conditions (after scaling, which rewrites recipe files)
    REC = g['REC']
    for name, env in ENV.items():
        for f in glob.glob(os.path.join(REC, '*', name + '.json')):
            r = json.load(open(f))
            if r.get('type', '').startswith(NS + ':') and r['type'] not in (NS + ':extreme_shaped', NS + ':extreme_shapeless', NS + ':compaction'):
                r['environment'] = env
                w(f, r)

    # ---- the journal rewrite
    J = os.path.join(here, 'journal')
    for jf in glob.glob(os.path.join(J, '*.json')):
        cat = os.path.basename(jf)[:-5]
        rew = json.load(open(jf))
        for eid, spec in rew.items():
            p = os.path.join(BA, 'entries', cat, eid + '.json')
            if not os.path.exists(p):
                continue
            e = json.load(open(p))
            texts = list(spec.get('texts', []))
            pages, out, i = e['pages'], [], 0
            last = -1
            for k, pg in enumerate(pages):
                if pg['type'] in ('patchouli:spotlight', 'patchouli:text') and i < len(texts):
                    pg = dict(pg)
                    pg['text'] = texts[i]
                    i += 1
                    last = len(out)
                out.append(pg)
            extra = [{'type': 'patchouli:text', 'text': t} for t in texts[i:]]
            out = out[:last + 1] + extra + out[last + 1:]
            e['pages'] = out
            if spec.get('title'):
                e['name'] = spec['title']
            w(p, e)

    # ---- the Patchouli edition: macros it was missing
    bj_path = os.path.join(D, 'patchouli_books', 'codex', 'book.json')
    bj = json.load(open(bj_path))
    bj['macros'] = {'$(fluid)': '$(#2a86c8)'}
    bj['landing_text'] = ('My notebook. Everything I learned at the edge of physics, in the order I learned it.$(br2)'
                          'It starts with a $(item)Spectral Analyzer$() and a habit of looking closely; $(thing)Insight$() is spent at a $(item)Research Terminal$().')
    w(bj_path, bj)

    # ---- the soundtrack: every biome of every dimension plays its dimension's piece
    MUSIC = {'the_bulk': 'the_bulk', 'hyperdimensional_reality': 'hyperdimensional_reality', 'platonic_realm': 'platonic_realm',
             'quantum_foam': 'quantum_foam', 'transfinite_hierarchy': 'transfinite_hierarchy', 'black_hole_interior': 'black_hole_interior',
             'white_hole_interior': 'white_hole_interior', 'transcendental': 'transcendental', 'living_proof': 'living_proof'}
    for did, track in MUSIC.items():
        dj = os.path.join(D, 'dimension', did + '.json')
        if not os.path.exists(dj): continue
        gen = json.load(open(dj))['generator']
        src = gen.get('biome_source', {})
        biomes = ([src['biome']] if 'biome' in src else []) + [b['biome'] for b in src.get('biomes', [])]
        if isinstance(gen.get('settings'), dict) and gen['settings'].get('biome'): biomes.append(gen['settings']['biome'])
        for b in biomes:
            bf = os.path.join(D, 'worldgen', 'biome', b.split(':')[-1] + '.json')
            if not os.path.exists(bf): continue
            bj = json.load(open(bf))
            bj['effects']['music'] = {'sound': f'{NS}:music.{track}', 'min_delay': 600, 'max_delay': 2400, 'replace_current_music': True}
            w(bf, bj)

    # ---- reference pages from the data, then the Modonomicon edition
    enrich(g)
    # (the Modonomicon edition is built last, by round23.finish, once every round has added its chapters)


# ====================================================================== Modonomicon
def md(text):
    """Patchouli formatting -> Modonomicon markdown."""
    COL = {'item': 'c08a18', 'thing': '2a7ab0', 'fluid': '2a86c8'}
    out, open_ = [], False
    i = 0
    text = text.replace('*', '\\*').replace('_', '\\_')
    while i < len(text):
        if text.startswith('$(', i):
            j = text.find(')', i)
            if j < 0:
                break
            tag = text[i + 2:j]
            i = j + 1
            if tag == '':
                if open_:
                    out.append('[#]()')
                    open_ = False
            elif tag in COL or tag.startswith('#'):
                if open_:
                    out.append('[#]()')
                out.append('[#](%s)' % (COL.get(tag) or tag[1:]))
                open_ = True
            elif tag == 'br2':
                out.append('\\\n\\\n')
            elif tag == 'br':
                out.append('\\\n')
            elif tag == 'li':
                out.append('\\\n• ')
            else:
                pass          # unknown macro: drop it
            continue
        out.append(text[i])
        i += 1
    if open_:
        out.append('[#]()')
    s = ''.join(out)
    while s.startswith('\\\n'):
        s = s[2:]
    return s


def modonomicon(g):
    w, q, D, LANG, BA = g['w'], g['q'], g['D'], g['LANG'], g['BA']
    NS = 'theoreticalsciences'
    DEFS = dict(g['DEFS'])
    for r in RESEARCH:
        DEFS[r[0]] = r
    root = os.path.join(D, 'modonomicon', 'books', 'quantum_codex')
    import shutil
    if os.path.isdir(root):
        shutil.rmtree(root)
    pre = 'book.theoreticalsciences.codex'
    LANG[f'{pre}.name'] = 'Quantum Codex'
    LANG[f'{pre}.tooltip'] = 'A researcher\'s notebook from the edge of physics'
    w(os.path.join(root, 'book.json'), {
        'name': f'{pre}.name', 'tooltip': f'{pre}.tooltip', 'custom_book_item': q('quantum_codex'), 'generate_book_item': False,
        'auto_add_read_conditions': False, 'allow_open_book_with_invalid_links': True, 'default_title_color': 0x10306A,
        # Minecraft's own font, so whatever replaces it (Modern UI) renders the Codex too
        'font': 'minecraft:default'})
    cats = {}
    for f in sorted(glob.glob(os.path.join(BA, 'categories', '*.json'))):
        cid = os.path.basename(f)[:-5]
        cats[cid] = json.load(open(f))
    entries = {}
    for f in sorted(glob.glob(os.path.join(BA, 'entries', '*', '*.json'))):
        cid, eid = f.split(os.sep)[-2], os.path.basename(f)[:-5]
        entries.setdefault(cid, {})[eid] = json.load(open(f))
    for cid, c in cats.items():
        if cid not in entries:
            continue
        LANG[f'{pre}.category.{cid}'] = c['name']
        LANG[f'{pre}.category.{cid}.desc'] = c.get('description', '')
        icon = c.get('icon', q('quantum_codex'))
        w(os.path.join(root, 'categories', cid + '.json'), {
            'name': f'{pre}.category.{cid}', 'description': f'{pre}.category.{cid}.desc', 'icon': {'item': icon.split(',')[0]},
            'sort_number': c.get('sortnum', 0), 'display_mode': 'node', 'max_scroll_x': 600, 'max_scroll_y': 600,
            'background': f'{NS}:textures/gui/codex/space_0.png', 'background_width': 512, 'background_height': 512,
            'background_parallax_layers': [
                {'background': f'{NS}:textures/gui/codex/space_%d.png' % k, 'speed': sp}
                for k, sp in ((0, 0.25), (1, 0.45), (2, 0.7), (3, 1.0))]})
        # ---- lay the entries out as a map: left to right in research order, linked to the research they build on
        es = entries[cid]

        def rid_of(e):
            a = e.get('advancement', '')
            return a.split('research/')[1] if 'research/' in a else None

        cols = {}
        for eid, e in es.items():
            r = rid_of(e)
            cols[eid] = DEFS[r][4] if r in DEFS else -99
        order = sorted(set(cols.values()))
        rank = {v: i for i, v in enumerate(order)}
        rows = {}
        pos = {}
        kids = {}
        for eid in es:
            if (cid, eid) in NODES: kids.setdefault(NODES[(cid, eid)], []).append(eid)
        mains = [eid for eid in es if (cid, eid) not in NODES]
        # each chapter sits in the middle of its own constellation of nodes; constellations are packed in reading
        # order (research tier, then chapter order), left to right and wrapping into rows
        def side(m):
            k = math.ceil(math.sqrt(len(kids.get(m, [])) + 1))
            return k + 1 - k % 2   # odd, so the chapter is in the centre
        ordered = sorted(mains, key=lambda k: (cols[k], es[k].get('sortnum', 0), k))
        area = sum((side(m) * 2 + 2) ** 2 for m in ordered)
        maxw = max(max([side(m) * 2 + 2 for m in ordered] + [0]), int(math.sqrt(area) * 1.7))
        cx = cy = rowh = 0
        occ = set()
        for m in ordered:
            k = side(m)
            sz = k * 2 + 2
            if cx and cx + sz > maxw:
                cx, cy, rowh = 0, cy + rowh, 0
            px, py = cx + (k - 1), cy + (k - 1)
            pos[m] = (px, py)
            occ.add((px, py))
            r = k - 1
            cand = sorted(((px + dx, py + dy) for dx in range(-r, r + 1, 2) for dy in range(-r, r + 1, 2) if dx or dy),
                          key=lambda c: ((c[0] - px) ** 2 + (c[1] - py) ** 2, c[1], c[0]))
            for kid in sorted(kids.get(m, []), key=lambda q_: es[q_].get('sortnum', 0)):
                for c in cand:
                    if c not in occ:
                        occ.add(c)
                        pos[kid] = c
                        break
            cx += sz
            rowh = max(rowh, sz)
        for kid_list in kids.values():
            for kid in kid_list:
                pos.setdefault(kid, (0, -4))
        by_rid = {}
        for eid, e in es.items():
            r = rid_of(e)
            if r:
                by_rid.setdefault(r, []).append(eid)

        def parents(eid):
            r = rid_of(es[eid])
            if not r or r not in DEFS:
                return []
            seen, frontier = {r}, list(DEFS[r][7])
            for _ in range(8):
                hit = [x for f in frontier if f in by_rid for x in by_rid[f] if x != eid]
                if hit:
                    return sorted(set(hit))[:2]
                nxt = []
                for f in frontier:
                    if f in seen or f not in DEFS:
                        continue
                    seen.add(f)
                    nxt += list(DEFS[f][7])
                frontier = nxt
            return []

        if pos:
            xs = [v[0] for v in pos.values()]; ys = [v[1] for v in pos.values()]
            ext = max(max(xs) - min(xs), max(ys) - min(ys))
            cj = os.path.join(root, 'categories', cid + '.json')
            cd = json.load(open(cj))
            cd['max_scroll_x'] = cd['max_scroll_y'] = max(600, ext * 30 + 200)
            w(cj, cd)
        for eid, e in es.items():
            key = f'{pre}.{cid}.{eid}'
            LANG[key] = e['name']
            pages = []
            for k, pg in enumerate(e['pages']):
                t = pg.get('type')
                pk = f'{key}.p{k}'
                if pg.get('text'):
                    LANG[pk] = md(pg['text'])
                if t == 'patchouli:spotlight':
                    it = pg['item'].split(',')[0]
                    pages.append({'type': 'modonomicon:spotlight', 'item': {'item': it}, 'text': pk if pg.get('text') else ''})
                elif t == 'patchouli:text':
                    p = {'type': 'modonomicon:text', 'text': pk}
                    if not pages:
                        p['title'] = key
                    pages.append(p)
                elif t == 'patchouli:crafting':
                    p = {'type': 'modonomicon:crafting_recipe', 'recipe_id_1': pg['recipe']}
                    if pg.get('recipe2'):
                        p['recipe_id_2'] = pg['recipe2']
                    if pg.get('text'):
                        p['text'] = pk
                    pages.append(p)
                elif t == 'patchouli:multiblock':
                    LANG[pk + '.mb'] = md(('$(thing)%s$(): ' % pg.get('name', 'Structure')) + (pg.get('text') or '') +
                                          '$(br2)Explore it layer by layer in JEI (Multiblocks), or place it with the $(item)Structure Assembler$().')
                    pages.append({'type': 'modonomicon:text', 'text': pk + '.mb'})
            first = next((pg.get('text') for pg in e['pages'] if pg.get('text')), '')
            desc = re.sub(r'\$\([^)]*\)', '', first).split('. ')[0][:90]
            LANG[key + '.desc'] = desc
            ent = {'name': key, 'description': key + '.desc', 'icon': {'item': e.get('icon', q('quantum_codex')).split(',')[0]},
                   'category': f'{NS}:{cid}', 'x': pos[eid][0], 'y': pos[eid][1], 'hide_while_locked': False,
                   'background_u_index': 0, 'background_v_index': 0, 'pages': pages,
                   'parents': [{'entry': f'{NS}:{cid}/{p}'} for p in ([NODES[(cid, eid)]] if (cid, eid) in NODES else parents(eid))]}
            if (cid, eid) in NODES:
                ent['parents'][0]['draw_arrow'] = False
            if e.get('advancement'):
                ent['condition'] = {'type': 'modonomicon:advancement', 'advancement_id': e['advancement']}
            w(os.path.join(root, 'entries', cid, eid + '.json'), ent)


# ====================================================================== the Codex, filled in from the data itself
DIM_ENTRIES = {'world/bulk': ['the_bulk'], 'mathematics/platonic_realm': ['platonic_realm'], 'quantum/quantum_foam': ['quantum_foam'],
               'mathematics/transfinite': ['transfinite_hierarchy'], 'spacetime/black_hole_interior': ['black_hole_interior', 'white_hole_interior'],
               'endgame/hyperreal': ['hyperdimensional_reality'], 'endgame/beyond': ['transcendental'], 'endgame/alpha_omega': ['living_proof']}
DIM_INFO = {
    # name, natural (gravity g, dimensions, time, vacuum), attunement item, residual law
    'the_bulk': ('The Bulk', (0.5, 5, 1, 0), 'brane_tether', 'half gravity, twice the knockback'),
    'hyperdimensional_reality': ('Hyperdimensional Reality', (1, 11, 1, 0), 'primordial_charter', 'a new law every minute and a half'),
    'platonic_realm': ('The Platonic Realm', (1, 3, 1, 0), 'form_of_the_self', 'only swords can dig'),
    'quantum_foam': ('The Quantum Foam', (1, 3, 1, 2), 'coherence_stabiliser', 'you blink a few blocks now and then'),
    'transfinite_hierarchy': ('The Transfinite Hierarchy', (1, 7, 1, 0), 'well_ordering_charm', 'Infinite Regress I and hunger'),
    'black_hole_interior': ('Inside the Black Hole', (1000, 3, 0.01, 0), 'geodesic_anchor', 'gravity x2.5, Time Dilation II; below y=4 the singularity spits you out of the White Hole'),
    'white_hole_interior': ('Inside the White Hole', (0.01, 3, 100, 0), 'geodesic_anchor', 'gravity points up (sneak to fall), Time Dilation'),
    'transcendental': ('The Transcendental', (1, 11, 1, 3), 'identity_kernel', 'Weakness and Mining Fatigue'),
    'living_proof': ('The Living Proof', (1, 3, 1, 0), None, 'no attunement - it is the fight itself'),
}
NODES = {}  # (category, node entry) -> parent entry, filled by enrich()
VAC = {0: 'ordinary', 1: 'Casimir-depleted', 2: 'false vacuum', 3: 'true vacuum'}


def enrich(g):
    """Adds data pages to every Codex entry: the items it talks about (what they are, how they are made, what they
    are for), the creatures and fluids it mentions, the machine it describes, and the dimension it is about."""
    w, q, D, A, LANG, BA, REC = g['w'], g['q'], g['D'], g['A'], g['LANG'], g['BA'], g['REC']
    NS = 'theoreticalsciences'
    here = os.path.dirname(os.path.abspath(__file__))
    C = json.load(open(os.path.join(here, 'content.json')))
    M = json.load(open(os.path.join(here, 'machines.json')))
    lore = {i['id']: i.get('lore', '') for i in C['items']}
    lore.update({b['id']: b['lore'] for b in C['blocks'] if b.get('lore') and not lore.get(b['id'])})   # 1.3.8: blocks' lore too
    fluids = {f['id'] for f in C['fluids']}
    item_ids = {i['id'] for i in C['items']} | {b['id'] for b in C['blocks']} | {m['id'] for m in M}

    def name(i):
        i = i.split(':')[-1]
        for k in ('item.', 'block.', 'fluid_type.', 'entity.'):
            v = LANG.get(f'{k}{NS}.{i}')
            if v: return v
        return i.replace('_', ' ').title()

    by_name = {}
    for i in item_ids | fluids:
        by_name.setdefault(name(i).lower(), i)
    mach_of_type = {m['recipe']: m['id'] for m in M}

    # ---- recipes: what makes each thing, and what each thing goes into
    made, used = {}, {}
    for f in glob.glob(os.path.join(REC, '**', '*.json'), recursive=True):
        r = json.load(open(f))
        t = r.get('type', '')
        if t.startswith('minecraft:crafting'):
            ins = list(r.get('key', {}).values()) if 'key' in r else r.get('ingredients', [])
            ins = [(x.get('item') or '#' + x.get('tag', '?'), 1) for x in ins if isinstance(x, dict)]
            outs, where, fl = [r['result']['id']], 'a crafting grid', []
        elif t.startswith(NS + ':extreme_'):
            if 'result' not in r:
                continue                       # the cluster recipe: its results follow the game
            if 'key' in r:
                cnt = {}
                for row in r['pattern']:
                    for ch in row:
                        if ch != ' ': cnt[ch] = cnt.get(ch, 0) + 1
                ins = [(r['key'][k].get('item') or '#' + r['key'][k].get('tag', '?'), n) for k, n in cnt.items()]
            else:
                ins = [(x.get('item') or '#' + x.get('tag', '?'), 1) for x in r.get('ingredients', [])]
            outs, where, fl = [r['result']['id']], '$(item)Extreme Assembly Table$()', []
        elif t.startswith(NS + ':') and t.split(':')[1] in mach_of_type:
            ins = [(x.get('item') or '#' + x.get('tag', '?'), x.get('count', 1)) for x in r.get('item_inputs', [])]
            fl = [(x['fluid'], x['amount']) for x in r.get('fluid_inputs', [])]
            outs = [o['id'] for o in r.get('item_outputs', [])] + [o['id'] for o in r.get('fluid_outputs', [])]
            where = '$(item)' + name(mach_of_type[t.split(':')[1]]) + '$()'
        else:
            continue
        if r.get('secret'): continue
        for o in outs:
            o = o.split(':')[-1]
            made.setdefault(o, []).append((where, ins, fl))
            for i, _ in ins:
                if i.startswith(NS + ':'): used.setdefault(i.split(':')[1], set()).add(o)
            for fi, _ in fl:
                if fi.startswith(NS + ':'): used.setdefault(fi.split(':')[1], set()).add(o)

    def made_text(i):
        rs = made.get(i)
        if not rs: return ''
        where, ins, fl = min(rs, key=lambda r: len(r[1]) + len(r[2]))
        parts = []
        for x, n in ins[:5]:
            parts.append(f'{n} {name(x)}' if n > 1 else name(x))
        for x, n in fl[:2]:
            parts.append(f'{n:,} mB {name(x)}')
        more = ' and more' if len(ins) > 5 else ''
        return f'Made in {where}.'          # 1.1.0: where only; the recipe pages and JEI/EMI show what goes in

    # ---- creatures: where they live (biome spawners) and what they drop
    CRE = g['CREATURES']
    homes = {}
    for f in glob.glob(os.path.join(D, 'worldgen', 'biome', '*.json')):
        b = json.load(open(f))
        for cat, lst in b.get('spawners', {}).items():
            for s in lst:
                homes.setdefault(s['type'].split(':')[-1], set()).add(LANG.get(f'biome.{NS}.{os.path.basename(f)[:-5]}', os.path.basename(f)[:-5]))
    cre_by_name = {}
    for cid, (cname, _) in CRE.items():
        cre_by_name[cname.lower()] = cid
        cre_by_name[cname.lower() + 's'] = cid

    def creature_text(cid):
        cname, drops = CRE[cid]
        h = ', '.join(sorted(homes.get(cid, []))) or 'nowhere natural'
        d = ', '.join(name(x) for x, *_ in drops[:4]) or 'nothing'
        return f'$(thing){cname}$(). Lives in: {h}.$(br)Drops: {d}.'

    # ---- dimensions
    def dim_pages(did):
        nm, (gv, dims, tm, vac), att, residual = DIM_INFO[did]
        dj = os.path.join(D, 'dimension', did + '.json')
        biomes = []
        if os.path.exists(dj):
            src = json.load(open(dj))['generator'].get('biome_source', {})
            if 'biome' in src: biomes = [src['biome']]
            biomes += [b['biome'] for b in src.get('biomes', [])]
        if 'settings' in json.load(open(dj))['generator'] and isinstance(json.load(open(dj))['generator']['settings'], dict):
            biomes.append(json.load(open(dj))['generator']['settings'].get('biome', ''))
        ores, marks, natives = [], [], set()
        for b in biomes:
            bf = os.path.join(D, 'worldgen', 'biome', b.split(':')[-1] + '.json')
            if not os.path.exists(bf): continue
            bj = json.load(open(bf))
            for lst in bj.get('spawners', {}).values():
                for s in lst:
                    if s['type'].split(':')[-1] in CRE: natives.add(CRE[s['type'].split(':')[-1]][0])
            for step in bj.get('features', []):
                for pf in step:
                    pid = pf.split(':')[-1]
                    pj = os.path.join(D, 'worldgen', 'placed_feature', pid + '.json')
                    feat = json.load(open(pj))['feature'] if os.path.exists(pj) else pf
                    cj = os.path.join(D, 'worldgen', 'configured_feature', str(feat).split(':')[-1] + '.json')
                    if os.path.exists(cj):
                        cf = json.load(open(cj))
                        if cf.get('type') == 'minecraft:ore':
                            for t in cf['config'].get('targets', []):
                                n = name(t['state']['Name'])
                                if n not in ores: ores.append(n)
                            continue
                        if cf.get('type', '').startswith(NS + ':') and 'boundary' not in pid and 'bridge' not in pid:
                            n = re.sub(r' (Common|Rare|Small|Large)$', '', pid.replace('_', ' ').title())
                            if n not in marks: marks.append(n)
        envl = f'Gravity {gv:g} g, {dims} spatial dimensions, time x{tm:g}, {VAC[vac]} vacuum.'
        p1 = f'$(thing)Dimension data: {nm}$()$(br2)$(thing)Nature$(): {envl}$(br2)'
        p1 += (f'$(thing)Carry$(): $(item){name(att)}$() - with it, {residual}.' if att else f'$(thing)Law$(): {residual}.')
        p2 = ''
        if ores: p2 += '$(thing)Ores$(): ' + ', '.join(ores[:8]) + '.$(br2)'
        if natives: p2 += '$(thing)Natives$(): ' + ', '.join(sorted(natives)) + '.$(br2)'
        if marks: p2 += '$(thing)Landmarks$(): ' + ', '.join(marks[:8]) + '.'
        return [p1] + ([p2] if p2 else [])

    def clip(t, n):
        plain = re.sub(r'\$\([^)]*\)', '', t)
        if len(plain) <= n: return t
        # cut on a sentence boundary inside the limit
        out, count = '', 0
        for part in re.split(r'(?<=\.) ', t):
            pl = len(re.sub(r'\$\([^)]*\)', '', part)) + 1
            if count + pl > n: break
            out += part + ' '
            count += pl
        if out.strip():
            return out.strip()
        cut = t[:n]
        k = cut.rfind('$(')
        if k >= 0 and ')' not in cut[k:]:
            cut = cut[:k]
        return cut.rstrip() + '...'

    added = 0
    for f in sorted(glob.glob(os.path.join(BA, 'entries', '*', '*.json'))):
        cat, eid = f.split(os.sep)[-2], os.path.basename(f)[:-5]
        e = json.load(open(f))
        text = ' '.join(p.get('text', '') for p in e['pages'])
        spot = []
        for p in e['pages']:
            if p['type'] == 'patchouli:spotlight':
                spot += [x.split(':')[-1] for x in p['item'].split(',')]
        mentioned = []
        for m in re.finditer(r'\$\(item\)([^$]+)\$\(\)', text):
            nm = m.group(1).strip().lower()
            i = by_name.get(nm) or by_name.get(nm.rstrip('s')) or by_name.get(nm[:-2] if nm.endswith('es') else nm)
            if i and i in item_ids: mentioned.append(i)
        rel = []
        for i in spot + mentioned:
            if i not in rel and (i in lore or i in made): rel.append(i)
        rel = rel[:6]
        pages = []
        # the machine itself
        mach = next((m for m in M if m['id'] == eid), None)
        if mach:
            makes = sorted({name(o) for o, rs in made.items() for (wh, _, _) in rs if wh == '$(item)' + name(eid) + '$()'})
            kind = 'multiblock' if mach['multiblock'] else 'single-block machine'
            t = f'$(thing)Machine data$(): a {kind} ({mach["inputs"]} item and {mach["fluid_inputs"]} fluid inputs, {mach["outputs"]} and {mach["fluid_outputs"]} outputs).'
            if makes: t += '$(br2)$(thing)Makes$(): ' + ', '.join(makes[:14]) + ('...' if len(makes) > 14 else '.')
            pages.append({'type': 'patchouli:text', 'text': clip(t, 420)})
        for i in rel:
            lo = lore.get(i, '')
            body = (lo + '$(br2)' if lo else '') + made_text(i)
            if not body.strip(): continue
            pages.append({'type': 'patchouli:spotlight', 'item': q(i), 'text': clip(body, 230)})
        uses = []
        for i in rel:
            u = sorted({name(x) for x in used.get(i, ())})
            if u: uses.append(f'$(li)$(item){name(i)}$(): ' + ', '.join(u[:5]) + (' and more' if len(u) > 5 else ''))
        if uses:
            pages.append({'type': 'patchouli:text', 'text': clip('$(thing)Where these go$():' + ''.join(uses[:5]), 420)})
        # creatures and fluids it mentions
        seen = set()
        for m in re.finditer(r'\$\(thing\)([^$]+)\$\(\)', text):
            cid = cre_by_name.get(m.group(1).strip().lower())
            if cid and cid not in seen:
                seen.add(cid)
                egg = cid + '_spawn_egg'
                if egg in item_ids or True:
                    pages.append({'type': 'patchouli:spotlight', 'item': q(egg), 'text': clip(creature_text(cid), 230)})
        fseen = set()
        for m in re.finditer(r'\$\(fluid\)([^$]+)\$\(\)', text):
            fid = by_name.get(m.group(1).strip().lower())
            if fid in fluids and fid not in fseen:
                fseen.add(fid)
                src = made_text(fid) or 'Found, not made.'
                u = sorted({name(x) for x in used.get(fid, ())})
                t = f'$(fluid){name(fid)}$(). {src}' + ('$(br)Used for: ' + ', '.join(u[:5]) + '.' if u else '')
                pages.append({'type': 'patchouli:spotlight', 'item': q(fid + '_cell'), 'text': clip(t, 230)})
        for did in DIM_ENTRIES.get(f'{cat}/{eid}', []):
            for t in dim_pages(did):
                pages.append({'type': 'patchouli:text', 'text': clip(t, 440)})
        if pages:
            e['pages'] = e['pages'] + pages[:12]
            added += len(pages[:12])
            w(f, e)
    print('codex reference pages added:', added)

    # ================================================================== Spectrum-style node map
    # Every item, block, fluid, machine and creature gets its own small entry, hanging off the entry for the research
    # that unlocks it - so each category becomes a constellation instead of a handful of chapters.
    NODES.clear()
    import puzzle_check
    PUZZLES = puzzle_check.load_all()
    DEFS = dict(g['DEFS'])
    MR = dict(g.get('MACHINE_RESEARCH', {}))
    entry_of = {}      # research id -> (cat, eid)
    taken = set()      # things that already have their own entry
    cat_entries = {}
    for f in sorted(glob.glob(os.path.join(BA, 'entries', '*', '*.json'))):
        cat, eid = f.split(os.sep)[-2], os.path.basename(f)[:-5]
        e = json.load(open(f))
        cat_entries.setdefault(cat, []).append(eid)
        taken.add(eid)
        taken.add(e.get('icon', '').split(',')[0].split(':')[-1])
        a = e.get('advancement', '')
        if 'research/' in a:
            entry_of.setdefault(a.split('research/')[1], (cat, eid))

    def home(rid):
        seen, frontier = set(), [rid]
        for _ in range(12):
            for r in frontier:
                if r in entry_of: return entry_of[r]
            nxt = []
            for r in frontier:
                if r in seen or r not in DEFS: continue
                seen.add(r)
                nxt += list(DEFS[r][7])
            frontier = nxt
            if not frontier: break
        return None

    # which research unlocks each thing
    rec_res, rec_ins = {}, {}
    for f in glob.glob(os.path.join(REC, '**', '*.json'), recursive=True):
        r = json.load(open(f))
        if r.get('secret'): continue
        outs = [o['id'] for o in r.get('item_outputs', [])] + [o['id'] for o in r.get('fluid_outputs', [])]
        if 'result' in r and isinstance(r['result'], dict) and 'id' in r['result']: outs.append(r['result']['id'])
        t = r.get('type', '').split(':')[-1]
        res = r.get('research', '').split(':')[-1] or MR.get(mach_of_type.get(t, ''), None)
        ins = [x.get('item', '').split(':')[-1] for x in (r.get('item_inputs') or [])]
        if 'key' in r: ins += [x.get('item', '').split(':')[-1] for x in r['key'].values() if isinstance(x, dict)]
        if 'ingredients' in r: ins += [x.get('item', '').split(':')[-1] for x in r['ingredients'] if isinstance(x, dict)]
        for o in outs:
            o = o.split(':')[-1]
            if res: rec_res.setdefault(o, set()).add(res)
            else: rec_ins.setdefault(o, set()).update(i for i in ins if i)
    research_of = {}
    for i, rs in rec_res.items():
        research_of[i] = min(sorted(rs), key=lambda r: DEFS[r][6] if r in DEFS else 9e9)   # sorted: ties break the same way every run
    for mid, r in MR.items():
        if r: research_of.setdefault(mid, r)
    for _ in range(6):
        for o, ins in rec_ins.items():
            if o in research_of: continue
            known = [research_of[i] for i in ins if i in research_of and research_of[i] in DEFS]
            if known: research_of[o] = max(sorted(known), key=lambda r: DEFS[r][6])
    # things that are found rather than made: the machine that refines them, then where they are first used
    for i, rs in made.items():
        if i in research_of: continue
        for wh, _, _ in rs:
            mm = re.match(r'\$\(item\)(.+)\$\(\)', wh)
            mid = by_name.get(mm.group(1).lower()) if mm else None
            if mid and MR.get(mid): research_of[i] = MR[mid]; break
    for i, us in used.items():
        if i in research_of: continue
        cands = [research_of[o] for o in us if research_of.get(o) in DEFS]
        if cands: research_of[i] = min(sorted(cands), key=lambda r: DEFS[r][6])
    items_meta = {i['id']: i for i in C['items']}
    kinds = {}
    for i in C['items']: kinds[i['id']] = 'item'
    for b in C['blocks']: kinds[b['id']] = 'block'
    for m in M: kinds[m['id']] = 'machine'
    for fl in fluids: kinds[fl] = 'fluid'

    def priority(i):
        k = kinds[i]
        return ({'machine': 0, 'fluid': 2, 'item': 1, 'block': 3}[k], 0 if lore.get(i) else 1, i.endswith('_singularity'), i)

    groups = {}
    mention = {}
    for f in sorted(glob.glob(os.path.join(BA, 'entries', '*', '*.json'))):
        cat, eid = f.split(os.sep)[-2], os.path.basename(f)[:-5]
        if eid.startswith('node_'): continue
        for m in re.finditer(r'\$\(item\)([^$]+)\$\(\)', ' '.join(p.get('text', '') for p in json.load(open(f))['pages'])):
            mention.setdefault(m.group(1).strip().lower(), (cat, eid))
    have = lambda c, e: os.path.exists(os.path.join(BA, 'entries', c, e + '.json'))
    for i in kinds:
        if i in taken or i.endswith('_spawn_egg') or re.search(r'_(stairs|slab|wall)$', i): continue
        r = research_of.get(i)
        h = home(r) if r else None
        if i.endswith('_singularity') and have('endgame', 'mass_crafting'): h = ('endgame', 'mass_crafting')   # all singularities together
        if not h: h = mention.get(name(i).lower())
        if not h and i.endswith('_ore') and have('world', 'ores'): h = ('world', 'ores')
        if not h and i.endswith('_singularity') and have('endgame', 'mass_crafting'): h = ('endgame', 'mass_crafting')
        if not h and kinds[i] == 'block' and have('world', 'decor'): h = ('world', 'decor')
        if not h: continue
        groups.setdefault(h, []).append(i)
    # creatures hang off the entry that talks about them
    for f in sorted(glob.glob(os.path.join(BA, 'entries', '*', '*.json'))):
        cat, eid = f.split(os.sep)[-2], os.path.basename(f)[:-5]
        e = json.load(open(f))
        text = ' '.join(p.get('text', '') for p in e['pages'])
        for m in re.finditer(r'\$\(thing\)([^$]+)\$\(\)', text):
            cid = cre_by_name.get(m.group(1).strip().lower())
            if cid and ('cre:' + cid) not in taken:
                taken.add('cre:' + cid)
                groups.setdefault((cat, eid), []).append('cre:' + cid)

    # research topics without a chapter of their own become nodes too: what they are, what they cost, what must be derived
    for rid, r in DEFS.items():
        if rid in entry_of or rid in ('conquered_alpha_omega', 'beyond_the_omega'): continue
        h = home(r[7][0]) if r[7] else None
        if h: groups.setdefault(h, []).append('res:' + rid)
    unhomed = [i for i in kinds if i not in taken and not i.endswith('_spawn_egg') and all(i not in v for v in groups.values()) and not re.search(r'_(stairs|slab|wall)$', i)]
    print('codex: things without a node:', len(unhomed), unhomed[:30])
    # ---- round 11: flavour and discovery notes for every node, and where things are found
    LORE11 = {}
    for f in sorted(glob.glob(os.path.join(here, 'lore11', 'lore_*.json'))):
        try:
            LORE11.update(json.load(open(f)))
        except ValueError:
            print('codex: unreadable lore file', f)
    try:
        import round11 as _r11
        for k_, (fl_, di_) in _r11.LORE.items(): LORE11.setdefault(k_, {'flavour': fl_, 'discovery': di_})
    except ImportError:
        pass
    ORE_OF = {b['drops'].split(':')[-1]: b['id'] for b in C['blocks'] if b['kind'] == 'ore' and b.get('drops')}
    ORE_BASE = {b['id']: b.get('base') for b in C['blocks'] if b['kind'] == 'ore'}
    HOST_DIM = {'bulk_stone': 'Bulk', 'axiom_stone': 'Platonic Realm', 'foam_substrate': 'Quantum Foam', 'ordinal_stone': 'Transfinite Hierarchy',
                'infall_rock': 'black hole interior', 'ejecta_rock': 'white hole interior', 'antistone': 'Antiverse', 'deepslate': 'Overworld\'s deepslate',
                'netherrack': 'Nether', 'end_stone': 'End', 'stone': 'Overworld'}
    CHESTS = {}
    for f in glob.glob(os.path.join(D, 'loot_table', 'chests', '*.json')):
        for m_ in re.findall(r'"theoreticalsciences:([a-z_]+)"', open(f).read()):
            CHESTS.setdefault(m_, set()).add(os.path.basename(f)[:-5].replace('_', ' ').title())
    made_n = 0
    for (cat, parent), things in groups.items():
        things = sorted(things, key=lambda i: (-1, i) if i.startswith('res:') else (9, i) if i.startswith('cre:') else priority(i))[:40]
        pe = json.load(open(os.path.join(BA, 'entries', cat, parent + '.json')))
        for n, i in enumerate(things):
            if i.startswith('res:'):
                rid = i[4:]
                r = DEFS[rid]
                nid = 'node_research_' + rid
                t = f'$(thing){r[1]}$()$(br2){r[2]}'
                t2 = [f'$(thing)Insight$(): {r[6]}']
                if r[7]: t2.append('$(thing)Builds on$(): ' + ', '.join(DEFS[x][1] for x in r[7] if x in DEFS) + '.')
                if r[8]: t2.append('$(thing)Samples$(): ' + ', '.join(f'{c} {name(x)}' for x, c in r[8]) + '.')
                pz = PUZZLES.get(rid)
                if pz: t2.append('$(thing)Derivation$(): ' + pz['goal'])
                pages = [{'type': 'patchouli:spotlight', 'item': q(r[3]), 'title': r[1], 'text': clip(t, 300)},
                         {'type': 'patchouli:text', 'text': clip('$(br2)'.join(t2), 560)}]
                ent = {'name': r[1], 'icon': q(r[3]), 'advancement': f'{NS}:research/{rid}'}
            elif i.startswith('cre:'):
                cid = i[4:]
                nid = 'node_' + cid
                pages = [{'type': 'patchouli:spotlight', 'item': q(cid + '_spawn_egg'), 'title': CRE[cid][0], 'text': clip(creature_text(cid), 260)}]
                ent = {'name': CRE[cid][0], 'icon': q(cid + '_spawn_egg')}
            else:
                nid = 'node_' + i
                nm = name(i)
                k = kinds[i]
                icon = q(i + '_cell') if k == 'fluid' else q(i)
                lo = lore.get(i, '') or (items_meta.get(i, {}).get('desc', ''))
                L = LORE11.get(i, {})
                flav = L.get('flavour') or lo or (('$(fluid)' if k == 'fluid' else '$(item)') + nm + '$().')
                pages = [{'type': 'patchouli:spotlight', 'item': icon, 'title': nm, 'text': clip(flav, 330)}]
                # the notebook: how it was found or derived
                if L.get('discovery'):
                    pages.append({'type': 'patchouli:text', 'text': clip('$(thing)Notebook$()$(br)' + L['discovery'], 420)})
                # where it comes from - made, mined, dropped or found
                t2 = []
                mt = made_text(i)
                if mt: t2.append(mt)
                if i in ORE_OF:
                    ob = ORE_OF[i]
                    t2.append(f'$(thing)Mined$(): from $(item){name(ob)}$(), in veins through the ' + HOST_DIM.get(ORE_BASE.get(ob, ''), 'rock') + '.')
                if k == 'block' and ORE_BASE.get(i):
                    t2.append('$(thing)Found$(): in veins through the ' + HOST_DIM.get(ORE_BASE[i], 'rock') + ' (mine it for its drop).')
                dropped = sorted({CRE[c][0] for c, (cn, dr) in CRE.items() for (x, *_) in dr if x.split(':')[-1] == i})
                if dropped: t2.append('$(thing)Dropped by$(): ' + ', '.join(dropped[:4]) + '.')
                if CHESTS.get(i): t2.append('$(thing)Found in$(): ' + ', '.join(sorted(CHESTS[i])[:4]) + ' chests.')
                r = research_of.get(i)
                if r in DEFS: t2.append('$(thing)Unlocked by$(): ' + DEFS[r][1] + '.')
                u = sorted({name(x) for x in used.get(i, ())})
                if u: t2.append('$(thing)Used in$(): ' + ', '.join(u[:8]) + (' and more.' if len(u) > 8 else '.'))
                if k == 'machine':
                    mach = next(m for m in M if m['id'] == i)
                    mk = sorted({name(o) for o, rs in made.items() for (wh, _, _) in rs if wh == '$(item)' + nm + '$()'})
                    t2.append('$(thing)Machine$(): ' + ('multiblock' if mach['multiblock'] else 'single block') + (' - makes ' + ', '.join(mk[:8]) + ('...' if len(mk) > 8 else '.') if mk else '.'))
                if t2:
                    pages.append({'type': 'patchouli:text', 'text': clip('$(br2)'.join(t2), 560)})
                ent = {'name': nm, 'icon': icon}
            ent.update({'category': f'{NS}:{cat}', 'sortnum': 500 + n, 'pages': pages})
            if pe.get('advancement') and 'advancement' not in ent: ent['advancement'] = pe['advancement']
            w(os.path.join(BA, 'entries', cat, nid + '.json'), ent)
            NODES[(cat, nid)] = parent
            made_n += 1
    print('codex nodes added:', made_n)
