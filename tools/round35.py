"""
Round 35.

* Fixes: the Quantum Foam crash (creature renderers holding a vertex buffer another one had closed:
  client/render/SafeBuffer), text running off the JEI / EMI pages (wrapped now), EMI's structure page (the same 3D
  view as JEI's: compat/MultiblockView), the attunement screen drawn as the Core's own terminal.
* Every item has a tooltip (client/ItemTips): its flavour line always, and with Shift what it is for. The items that
  had no flavour line get one here, written from what they are (ores from the world generation: where and how deep).
* Create's creative items have hand-picked ingredients at the Forge (transcend/CoreForge.TWIN).
* The Resolved Anomaly goes into the last things of the mod (transcend/CoreForge), and makes the Euclidean Anchor,
  which holds space flat around its bearer (endgame/EuclideanAnchorItem).
"""
import glob
import json
import os

NS = 'theoreticalsciences'


def _I(i, n, art, c1, c2, lore, **kw):
    d = dict(id=i, name=n, art=art, c1=c1, c2=c2, lore=lore)
    d.update(kw)
    return d


ITEMS = [
    _I('euclidean_anchor', 'Euclidean Anchor', 'lattice', '#d8f4ff', '#ffffff',
       'Euclid\'s fifth postulate, made to hold. Wherever its bearer stands, parallel lines stay parallel.',
       special='euclideananchor', stack=1),
]
BLOCKS = []
FLUIDS = []
BEYOND = ['euclidean_anchor']

LANG = {
    'death.attack.theoreticalsciences.exotic_heat': '%1$s was boiled away',
    'death.attack.theoreticalsciences.exotic_heat.player': '%1$s was boiled away whilst fighting %2$s',
    'death.attack.theoreticalsciences.stellar_heat': '%1$s was dissolved into plasma',
    'death.attack.theoreticalsciences.stellar_heat.player': '%1$s was dissolved into plasma whilst fighting %2$s',
    # round 37: transport, easy to miss - the Relays move items, fluids and energy
    'jei.theoreticalsciences.info.entangled_relay': 'Item, fluid and energy transport. Place it against a machine, tank or chest, tune several to one channel with an Entanglement Tuner, and sneak-use with the tuner to switch it between sending and receiving. Moves 256 items, 64,000 mB and 64 M FE per tick, at any distance, even between dimensions. Research: Entangled Logistics.',
    'jei.theoreticalsciences.info.entangled_transceiver': 'The upgraded Entangled Relay: 8,192 items, 4,096,000 mB and 2.1 billion FE per tick, on the same channels. Place it against a machine, tank or chest, tune it with an Entanglement Tuner and sneak-use to switch between sending and receiving.',
    'jei.theoreticalsciences.info.entanglement_tuner': 'Tunes Entangled Relays and Transceivers to one channel (use it on them), and sneak-use switches one between sending and receiving. Everything on a channel moves items, fluids and energy between the blocks they face.',
    'jei.theoreticalsciences.forge_laws_drift': 'Compile at the rebuilt Core. Laws drift after each forging; 3 min re-seed. The Pleroma Reconstitutor makes it too.',
    'message.theoreticalsciences.forge_reseeding': 'The Core is re-seeding that universe: %s:%s',
    'message.theoreticalsciences.forge_drifted': 'That universe is spent: the recipe\'s laws have drifted. Tune them again.',
    'status.theoreticalsciences.transcendental_stands': 'Transcendental still stands',
    'status.theoreticalsciences.transcendental_collapsing': 'Wait: Transcendental collapsing',
    'status.theoreticalsciences.already_rekindled': 'Already rekindled (once a world)',
    'message.theoreticalsciences.hyperobject_sealed': 'The Hyperobject is sealed: %s',
    'message.theoreticalsciences.hyperobject_waits': 'The Hyperobject is still open. Step into its heart to enter the Transcendental.',
    # Jade: the machine's working state
    'config.jade.plugin_theoreticalsciences.machine': 'Theoretical Sciences machines',
    'jade.theoreticalsciences.status': 'Status: %s',
    'jade.theoreticalsciences.making': 'Making %s',
    'jade.theoreticalsciences.making_par': 'Making %s (x%s parallel)',
    'jade.theoreticalsciences.progress': '%s%%',
    'jade.theoreticalsciences.progress_time': '%s%%  (%s / %s)',
    'jade.theoreticalsciences.overclock': 'Overclock %s, %s parallel',
    # the late-game operator console
    'gui.theoreticalsciences.console.view_physics': 'CONTROLS',
    'gui.theoreticalsciences.console.view_sim': 'SIMULATION',
    'gui.theoreticalsciences.console.scope': 'Trace: %s',
    'gui.theoreticalsciences.console.scope_window': 'The green band is the window the recipe needs this quantity held in; the line turns red outside it.',
    'gui.theoreticalsciences.console.scope_plain': 'How it has moved over the last eight seconds.',
    'gui.theoreticalsciences.console.scope_power': 'Amber line: the energy buffer.',
    # the attunement screen as a terminal
    'gui.theoreticalsciences.attune.wake_help': 'Wake the Core: every branch must be in tune (Mathematics: all four laws). Costs one action of the Hand\'s Eschaton Charge.',
    'gui.theoreticalsciences.attune.compact_help': 'Drag the track, or scroll on it (Shift: x10, Ctrl: x100).',
    # EMI's structure page
    'emi.theoreticalsciences.zoom': 'Zoom out / in',
    'emi.theoreticalsciences.preview_help': 'Drag to turn, Shift-drag or right-drag to move, - / + to zoom. Hover a block to see what goes there.',
    # tooltips
    'tooltip.theoreticalsciences.hold_shift': 'Hold %s for details',
    'tooltip.theoreticalsciences.structure_part': 'Structure part: %s',
    'tooltip.theoreticalsciences.and_more': ' and %s more',
    'tooltip.theoreticalsciences.forged_at_core': 'Forged at the Omnipotence Core',
    'tooltip.theoreticalsciences.beyond_omnipotence': 'Beyond Omnipotence: the Core cannot materialize it',
    'tooltip.theoreticalsciences.codex_key': 'Press %s for its page in the Quantum Codex',
    # the Omnilex Engine's aleph-null overclock: its state is shown now
    'gui.theoreticalsciences.omega_on': '\u03a9 SET',
    'gui.theoreticalsciences.omega_tip_on': 'The aleph-null overclock is SET. Press to release it.',
    'message.theoreticalsciences.omega_on': 'Aleph-null overclock set: the impossible recipe will ask before it runs.',
    'message.theoreticalsciences.omega_off': 'Aleph-null overclock released.',
    # round 35: the Transcendental and the Living Proof open only the intended way
    'message.theoreticalsciences.gate.transcendental': 'The Transcendental cannot be reached this way. It opens only when the Omnilex Engine runs the impossible recipe - you have been returned to the Overworld.',
    'message.theoreticalsciences.gate.living_proof': 'The Living Proof exists only inside Alpha Omega\'s challenge, and cannot be entered any other way - you have been returned to the Overworld.',
    # round 35: structures that only work in one dimension say so
    'status.theoreticalsciences.wrong_dimension': 'Wrong dimension',
    'status.theoreticalsciences.wrong_dimension_in': 'Only works in %s',
    'tooltip.theoreticalsciences.only_in_dimension': 'Only works in %s - anywhere else it stands idle',
    'jei.theoreticalsciences.multiblock_only_in': 'Multiblock - only works in %s',
    'jei.theoreticalsciences.only_in': 'Only works in %s',
    # the Vacuum Fluctuation Harvester: what "tuned closest" means
    'jei.theoreticalsciences.vacuum_note_1': 'Makes ONE of these per run: the particle whose resonance its tuning dial is set closest to. '
                                       'Each particle has a resonance from 0 to 100 that changes from place to place - read them with a '
                                       'Resonance Probe where the harvester stands, then set the dial to the one you want. The closer, the more it yields.',
    'jei.theoreticalsciences.vacuum_slot': 'Set the harvester\'s dial to this particle\'s resonance where it stands (read it with a Resonance Probe)',
    # round 35: Laplace's Demon also fails politely on a physics model it cannot search
    'message.theoreticalsciences.demon_failed': 'Laplace\'s Demon could not finish: the machine changed or was removed, or its physics model could not be searched (the server log says why).',
    # round 35: recipes that need a dimension or a biome
    'env.theoreticalsciences.in_dimension': 'Only in %s',
    'env.theoreticalsciences.in_biome': 'Only in the biome %s',
    'env.theoreticalsciences.or': ' or ',
    # the Euclidean Anchor
    'tooltip.theoreticalsciences.euclidean_anchor.on': 'Holding space flat: no distortion near impossible structures',
    'tooltip.theoreticalsciences.euclidean_anchor.off': 'Switched off: space bends as it likes',
    'tooltip.theoreticalsciences.euclidean_anchor.use': 'Use to switch on or off. Works anywhere in the inventory; spends charge only while it holds space flat.',
    'message.theoreticalsciences.euclidean_anchor.on': 'Euclidean Anchor: space held flat',
    'message.theoreticalsciences.euclidean_anchor.off': 'Euclidean Anchor: released',
}

# --------------------------------------------------------------------------------- flavour lines for what had none
_BUILDING = {'slab': 'A slab', 'stairs': 'Stairs', 'wall': 'A wall', 'pillar': 'A pillar', 'tiles': 'Tiles', 'bricks': 'Bricks',
             'brick': 'A brick', 'planks': 'Planks'}
_PART = {
    'casing': 'A machine casing: the shell a structure is built from. Hold Shift to see which structures use it.',
    'plating': 'Plating for a structure\'s walls, rated for what the machine inside does.',
    'plate': 'A structural plate. Hold Shift to see which structures take it.',
    'glass': 'Glass rated for what the structure behind it holds back - light gets out, nothing else does.',
    'emitter': 'An emitter for a structure\'s field. Better grades let the machine reach further.',
    'projector': 'Projects a field around a structure; what the field is decides what the machine inside can do.',
    'magnet': 'A magnet for a structure\'s field coils.',
    'array': 'An array of sensors or emitters, built into a structure.',
    'panel': 'A panel for a structure\'s walls.',
    'vent': 'Lets a structure shed what it must not keep.',
    'jacket': 'A jacket for a structure\'s chambers.',
    'solder': 'Solder for the joints of a structure, as a block.',
}


def _dims(RES):
    """placed feature -> the places (dimension names) it generates in."""
    d = os.path.join(RES, 'data', NS)
    out = {}
    mods = {'overworld_ores': 'the Overworld', 'nether_ores': 'the Nether', 'end_ores': 'the End'}
    for f in glob.glob(os.path.join(d, 'neoforge', 'biome_modifier', '*.json')):
        k = os.path.basename(f)[:-5]
        if k not in mods:
            continue
        j = json.load(open(f))
        feats = j.get('features', [])
        for x in [feats] if isinstance(feats, str) else feats:
            out.setdefault(x.split(':')[-1], []).append(mods[k])
    return out


def _dimname(LANG, dim):
    """'the Quantum Foam', 'Hyperdimensional Reality': a dimension's name as it sits in a sentence."""
    name = LANG.get(f'transit.{NS}.{dim}.name') or LANG.get(f'dimension.{NS}.{dim}') or dim.replace('_', ' ').title()
    return 'the ' + name[4:] if name.startswith('The ') else name


_LAST = None


def _last_lang():
    global _LAST
    if _LAST is None:
        p = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'out', 'lang_full.json')     # round 36: with the Codex text
        try:
            _LAST = json.load(open(p))
        except Exception:
            _LAST = {}
    return _LAST


_TOOLS = ('wooden', 'wood', 'stone', 'iron', 'diamond', 'netherite')


def _codex_intro(LANG, path, limit=200):
    """The first sentences of the item's own Codex page (written for it), as a flavour line."""
    import re
    src = LANG
    keys = [k for k in src if k.startswith(f'book.{NS}.codex.') and k.endswith(f'.node_{path}.p0')]
    if not keys:
        # the Codex texts are written later in the run (round23): use the last build's, which are the same
        src = _last_lang()
        keys = [k for k in src if k.startswith(f'book.{NS}.codex.') and k.endswith(f'.node_{path}.p0')]
    if not keys:
        return None
    LANG = src
    t = re.sub(r'\$\([^)]*\)|\[#\]\([^)]*\)|\[#\]', '', LANG[keys[0]]).replace('\\', ' ').strip()
    out = ''
    for sent in re.split(r'(?<=[.!?])\s+', t):
        if out and len(out) + len(sent) > limit:
            break
        out = (out + ' ' + sent).strip()
    return out or None


def _dimension_names(RES, LANG):
    """biome id -> the name of the dimension it is in."""
    d = os.path.join(RES, 'data', NS, 'dimension')
    out = {}
    for f in glob.glob(os.path.join(d, '*.json')):
        dim = os.path.basename(f)[:-5]
        name = _dimname(LANG, dim)
        src = json.load(open(f)).get('generator', {}).get('biome_source', {})
        biomes = [src['biome']] if 'biome' in src else [b.get('biome') for b in src.get('biomes', []) if isinstance(b, dict)]
        for b in biomes:
            if b:
                out[b.split(':')[-1]] = name
    return out


def _ores(RES, LANG):
    """ore block -> (where, low, high)."""
    d = os.path.join(RES, 'data', NS, 'worldgen')
    placed_dims = _dims(RES)
    bdim = _dimension_names(RES, LANG)
    for f in glob.glob(os.path.join(d, 'biome', '*.json')):
        b = os.path.basename(f)[:-5]
        for step in json.load(open(f)).get('features', []):
            for x in step if isinstance(step, list) else [step]:
                if isinstance(x, str) and b in bdim:
                    placed_dims.setdefault(x.split(':')[-1], []).append(bdim[b])
    out = {}
    for f in glob.glob(os.path.join(d, 'placed_feature', '*.json')):
        pid = os.path.basename(f)[:-5]
        j = json.load(open(f))
        cf = os.path.join(d, 'configured_feature', j.get('feature', '').split(':')[-1] + '.json')
        if not os.path.exists(cf):
            continue
        c = json.load(open(cf)).get('config', {})
        states = [c['state'].get('Name')] if 'state' in c else [t.get('state', {}).get('Name') for t in c.get('targets', [])]
        lo = hi = None
        for p in j.get('placement', []):
            if p.get('type') == 'minecraft:height_range':
                h = p.get('height', {})
                lo = (h.get('min_inclusive') or {}).get('absolute')
                hi = (h.get('max_inclusive') or {}).get('absolute')
        where = sorted(set(placed_dims.get(pid, [])))
        for s in states:
            if s and s.startswith(NS + ':'):
                out[s.split(':')[1]] = (where, lo, hi)
    return out


def _flavour(g):
    """A flavour line for every item and block that has none."""
    LANG, RES = g['LANG'], g['RES']
    try:
        C = json.load(open(os.path.join(os.path.dirname(os.path.abspath(__file__)), 'content.json')))
    except Exception:
        C = {'blocks': [], 'fluids': []}
    blocks = {b['id']: b for b in C.get('blocks', [])}
    fluids = {f['id']: f for f in C.get('fluids', [])}
    ores = _ores(RES, LANG)
    made = 0
    for key in sorted(k for k in list(LANG) if (k.startswith(f'item.{NS}.') or k.startswith(f'block.{NS}.')) and k.count('.') == 2):
        lk = key + '.lore'
        if lk in LANG:
            continue
        path = key.split('.')[2]
        name = LANG[key]
        from round35_manual import MANUAL
        text = MANUAL.get(path)
        if text:
            LANG[lk] = text
            made += 1
            continue
        if path.endswith('_spawn_egg'):
            mob = LANG.get(f'entity.{NS}.{path[:-10]}', path[:-10].replace('_', ' ').title())
            text = f'Calls a {mob} into the world. Creative mode only: nothing hatches from it in survival.'
        elif path.endswith('_cell') and path[:-5] in fluids:
            f = fluids[path[:-5]]
            fname = LANG.get(f'block.{NS}.{f["id"]}') or f.get('name') or f['id']
            state = 'a gas' if f.get('gas') else 'a liquid'
            t = f.get('temp')
            temp = f', at {t:,} K' if isinstance(t, (int, float)) else ''
            text = f'1,000 mB of {fname} - {state}{temp} - sealed in a containment cell. Pours out and fills up like a bucket.'
        elif path in fluids:
            text = f'{name}, loose in the world. An empty Containment Cell scoops it up.'
        elif path in ores or blocks.get(path, {}).get('kind') == 'ore':
            b = blocks.get(path, {})
            where, lo, hi = ores.get(path, ([], None, None))
            drop = b.get('drops')
            dname = LANG.get(f'item.{NS}.{drop}', drop.replace('_', ' ').title()) if drop else None
            intro = _codex_intro(LANG, path, 140) or ''
            parts = [intro if len(intro.rstrip('.')) > len(name) + 4 else f'{name}.']
            if where:
                parts.append('Found in ' + ' and '.join(where) + (f', between Y {lo} and {hi}' if lo is not None and hi is not None else '') + '.')
            if dname:
                parts.append(f'Gives {dname} when mined.')
            tool = b.get('tool')
            if tool in _TOOLS:            # (no tool, or none needed: say nothing rather than "a none pickaxe")
                parts.append(f'Needs {"an" if tool[0] in "aeiou" else "a"} {tool} pickaxe or better.')
            text = ' '.join(parts)
        elif len((_codex_intro(LANG, path) or '').rstrip('.')) > len(name) + 4:
            # round 35: the item's own Codex page says best what it is (unless it only names it)
            text = _codex_intro(LANG, path)
        else:
            suf = path.split('_')[-1]
            if suf in _BUILDING:
                base = name
                for w in ('Slab', 'Stairs', 'Wall', 'Pillar'):
                    base = base.replace(' ' + w, '')
                text = f'{_BUILDING[suf]} of {base}, for building with.' if suf in ('slab', 'stairs', 'wall', 'pillar') else \
                    f'{name}: a building block of the strange places, for building with.'
            elif suf in _PART:
                text = _PART[suf]
            elif key.startswith('block.') and suf in ('stone', 'rock', 'soil', 'antisoil', 'antistone', 'ice', 'heartwood', 'crystal', 'matter',
                                                     'residue', 'substrate', 'condensate'):
                text = f'{name}: the ground of one of the strange places beyond the Overworld.'
            else:
                text = f'{name}. Its page in the Quantum Codex tells what it is for.'
        LANG[lk] = text
        made += 1
    print('round35: flavour lines written for', made, 'items without one')


# never in the item viewers: can't be had in survival and does nothing in an inventory (round 35)
HIDDEN = ['proof_wall', 'proof_floor', 'proof_membrane', 'answer_pedestal', 'axiom_toggle', 'axiom_lamp', 'observer_plate']
# (round 36: not the Omnipotence Core - it is a block with no item, and a tag naming a missing item fails to load whole)


def _hidden(g):
    """#theoreticalsciences:hidden_from_viewers - the list above, plus any block the progression check found nobody can
    obtain that is not an ore, a fluid, a spawn egg or one of the items made in code (a pack can add more to the tag)."""
    ids = set(HIDDEN)
    rp = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'out', 'reachable.json')
    try:
        have = set(json.load(open(rp)))
        C = json.load(open(os.path.join(os.path.dirname(os.path.abspath(__file__)), 'content.json')))
        for b in C.get('blocks', []):
            i = b['id']
            if f'{NS}:{i}' in have or b.get('kind') == 'ore' or i.endswith('_ore') or b.get('hardness', 1) >= 0:
                continue
            ids.add(i)          # unbreakable and never made: only ever part of a built structure
    except Exception:
        pass
    g['w'](os.path.join(g['D'], 'tags', 'item', 'hidden_from_viewers.json'), {'replace': False, 'values': [f'{NS}:{i}' for i in sorted(ids)]})
    print('round35: hidden from JEI / EMI:', sorted(ids))


def apply(g):
    w, D = g['w'], g['D']
    g['LANG'].update(LANG)
    g['LANG'].update({f'dimname.{NS}.{os.path.basename(f)[:-5]}': _dimname(g['LANG'], os.path.basename(f)[:-5])
                      for f in glob.glob(os.path.join(g['RES'], 'data', NS, 'dimension', '*.json'))})
    _hidden(g)
    # round 36: things a world has once, or that only remake themselves - protected when dropped (world/Irreplaceable)
    g['w'](os.path.join(D, 'tags', 'item', 'irreplaceable.json'), {'replace': False, 'values': [f'{NS}:{i}' for i in
           ('rekindled_spark', 'transcendental_echo', 'omnilex', 'proof_of_everything', 'forged_omnipotence_core', 'demiurge_hand', 'omnipotence_seed')]})
    # round 37: the mod's hot liquids burn (exotic_heat is fire; stellar_heat is beyond what fire resistance stops)
    for t in ('exotic_heat', 'stellar_heat'):
        g['w'](os.path.join(D, 'damage_type', t + '.json'), {'message_id': f'{NS}.{t}', 'exhaustion': 0.1, 'scaling': 'when_caused_by_living_non_player',
                                                                'effects': 'burning'})
    g['w'](os.path.join(g['RES'], 'data', 'minecraft', 'tags', 'damage_type', 'is_fire.json'), {'replace': False, 'values': [f'{NS}:exotic_heat']})
    g['w'](os.path.join(g['RES'], 'data', 'minecraft', 'tags', 'damage_type', 'no_knockback.json'), {'replace': False, 'values': [f'{NS}:exotic_heat', f'{NS}:stellar_heat']})
    # round 37: pack makers' and mod devs' own say on which items only the Core's Forge makes (empty: the mod's own
    # detection covers creative items; these add to it or take from it)
    for t in ('forge_only', 'forge_exempt'):
        g['w'](os.path.join(D, 'tags', 'item', t + '.json'), {'replace': False, 'values': []})
    # round 36: the Transcendental's Core has no item, so no item model either
    om = os.path.join(g['RES'], 'assets', NS, 'models', 'item', 'omnipotence_core.json')
    if os.path.exists(om): os.remove(om)
    tf = os.path.join(D, 'tags', 'item', 'beyond_omnipotence.json')
    t = json.load(open(tf))
    for b in BEYOND:
        if f'{NS}:{b}' not in t['values']:
            t['values'].append(f'{NS}:{b}')
    w(tf, t)


def _codex_gaps(g):
    """Round 36 (final Codex check): the few things that had no page of their own yet."""
    BA = g['BA']
    f = os.path.join(BA, 'entries', 'operation', 'principles.json')
    if os.path.exists(f):
        e = json.load(open(f))
        if not any('chunk loader' in (p.get('text') or '') for p in e['pages']):
            e['pages'].append({'type': 'patchouli:text', 'text': 'Structures may cross chunk borders freely. Away from home, a $(thing)chunk loader$() keeps a machine running: load every chunk the structure touches. If only some are loaded, the controller keeps the state it last saw (formed, its ports) and waits - it never reaches into an unloaded chunk.'})
        g['w'](f, e)
    f = os.path.join(BA, 'entries', 'endgame', 'miniatures.json')
    if os.path.exists(f):
        e = json.load(open(f))
        have = {it.split(':')[-1] for p in e['pages'] for it in (p.get('item') or '').split(',') if it}
        for d in ('white_hole', 'wormhole', 'pocket_universe', 'pulsar', 'quasar', 'dyson_sphere', 'penteract'):
            if 'display_' + d not in have:
                e['pages'].append({'type': 'patchouli:spotlight', 'item': f'{NS}:display_{d}',
                                   'text': 'Made on the $(item)Extreme Assembly Table$(); the Codex\'s recipe page shows exactly what it takes.'})
        g['w'](f, e)
    f = os.path.join(BA, 'entries', 'endgame', 'pleroma.json')
    if os.path.exists(f):
        e = json.load(open(f))
        if not any('pleroma_cell' in (p.get('item') or '') for p in e['pages']):
            e['pages'].append({'type': 'patchouli:spotlight', 'item': f'{NS}:pleroma_cell',
                               'text': 'Pleroma carried in a $(item)Containment Cell$(), like every fluid of the mod: fill it at a tank, empty it into one.'})
        g['w'](f, e)


def finish(g):
    _codex_gaps(g)
    te = g['text_entry']
    te('endgame', 'euclidean_anchor', 'The Euclidean Anchor', 'euclidean_anchor', [
        'Near the structures of impossible geometry - tesseracts, fractals, Calabi-Yau folds, event horizons - space itself bends on your screen. The $(item)Euclidean Anchor$() holds it flat.',
        'Carry it anywhere in your inventory and use it to switch it on. While you are inside a distortion, the Anchor spends a little $(thing)Eschaton Charge$() to keep parallel lines parallel; outside one it spends nothing. An $(item)Eschaton Cell$() fills it again.',
        'It is forged at the Omnipotence Core, from what the $(thing)Abnormality$() branch resolved: 24 $(item)Resolved Anomalies$(), Geodesic and Reality Anchors, Stress-Energy Tensors and a Quarantine Field Matrix around two Eschaton Cores.'],
        sort=95, spot=['euclidean_anchor'])
    te('endgame', 'resolved_endgame', 'What the Anomaly Leaves Behind', 'resolved_anomaly', [
        'Once explained, an anomaly is not gone: it is a $(item)Resolved Anomaly$() - a law that was broken and now holds. The last things of all are built on them.',
        '$(li)Eschaton Core: 4$(li)Echo Lattice: 6$(li)Equilibrium Lattice: 8$(li)Hand of the Demiurge: 8$(li)Seed of Omnipotence: 12$(li)Lawgiver\'s Codex: 16$(li)Euclidean Anchor: 24$(li)a forged Omnipotence Core: 32',
        'So the Abnormality branch is not a side road: every Eschaton Core - and everything made from one - goes through it.'],
        sort=60, spot=['resolved_anomaly'])


def late(g):
    """After the Codex is written (round23): flavour lines can quote each item's own Codex page."""
    _flavour(g)
