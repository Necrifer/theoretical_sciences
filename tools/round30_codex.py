"""
Round 30 (round30_codex): the mod's own Codex, and the Codex brought up to date.

* The Quantum Codex no longer depends on Patchouli or Modonomicon (their node maps could not scroll far enough to
  reach every node). The mod has its own Codex screen (client/codex/*). This file writes the data it reads:
  assets/theoreticalsciences/ts_codex/index.json (the tabs), <tab>.json (the nodes of each tab map) and entries.json
  (the text of every entry, shared by all its copies on the different maps). The Patchouli and Modonomicon books
  are still generated as before, from the same entries; they are simply not opened by the Codex item any more.
* Brought up to date (refresh, run before the books are built): every "Made in ..." line is worked out again from
  the recipes as they are at the very end (before, they were written before round 24 moved the crafting-table
  recipes onto the Extreme Assembly Table, and so said "a crafting grid"); the Core's Forge recipes are read from
  CoreForge.java; pages whose numbers changed (Pleroma, the Omnium Ingot) are rewritten from the data; new chapters
  for the Transcendental and the Omnipotence Core (incl. "access denied") and for arriving safely in a dimension;
  and every item, block, fluid, machine and creature that still had no node of its own gets one.

Called from round23.finish: refresh(g) at its start, export(...) at its end.
"""
import glob
import json
import os
import re

NS = 'theoreticalsciences'

LANG = {
    'gui.theoreticalsciences.codex.title': 'Quantum Codex',
    'gui.theoreticalsciences.codex.search': 'Search the Codex...',
    'gui.theoreticalsciences.codex.fit': 'Fit',
    'gui.theoreticalsciences.codex.fit_tip': 'Zoom out until every node of this map is on screen (F)',
    'gui.theoreticalsciences.codex.centre': 'Centre',
    'gui.theoreticalsciences.codex.centre_tip': 'Go back to where this map begins (C or Home)',
    'gui.theoreticalsciences.codex.research': 'Research',
    'gui.theoreticalsciences.codex.research_tip': 'Open the research tree (sneak-using the Codex opens it too)',
    'gui.theoreticalsciences.codex.zoom_in': 'Zoom in (mouse wheel up, +)',
    'gui.theoreticalsciences.codex.zoom_out': 'Zoom out (mouse wheel down, -)',
    'gui.theoreticalsciences.codex.map_tip': 'Drag (or use the arrow keys) to move the map. Mouse wheel to zoom.',
    'gui.theoreticalsciences.codex.back': 'Back',
    'gui.theoreticalsciences.codex.back_tip': 'Back to the previous page (Backspace)',
    'gui.theoreticalsciences.codex.map': 'Map',
    'gui.theoreticalsciences.codex.map_back_tip': 'Back to the map (Esc)',
    'gui.theoreticalsciences.codex.prev': 'Previous page (Left arrow)',
    'gui.theoreticalsciences.codex.next': 'Next page (Right arrow)',
    'gui.theoreticalsciences.codex.page': 'Page %s of %s',
    'gui.theoreticalsciences.codex.locked': 'Locked',
    'gui.theoreticalsciences.codex.locked_by': 'Locked: needs the research %s',
    'gui.theoreticalsciences.codex.locked_page': 'This entry is still locked.$(br2)Research $(thing)%s$() to read it.',
    'gui.theoreticalsciences.codex.new': 'New - not read yet',
    'gui.theoreticalsciences.codex.click_open': 'Click to read',
    'gui.theoreticalsciences.codex.no_results': 'Nothing found',
    'gui.theoreticalsciences.codex.results': '%s found',
    'gui.theoreticalsciences.codex.missing': 'The Codex data is missing (a resource pack may have removed it).',
    'gui.theoreticalsciences.codex.made_in': 'Made in: %s',
    'gui.theoreticalsciences.codex.recipe': 'How it is made',
    'gui.theoreticalsciences.codex.recipe_n': 'How it is made (%s of %s)',
    'gui.theoreticalsciences.codex.no_recipe': 'Recipe %s is not loaded in this world.',
    'gui.theoreticalsciences.codex.crafting': 'Crafting table',
    'gui.theoreticalsciences.codex.extreme': 'Extreme Assembly Table',
    'gui.theoreticalsciences.codex.forge': "The reborn Core's Forge",
    'gui.theoreticalsciences.codex.kept': 'kept, not used up',
    'gui.theoreticalsciences.codex.energy': '%s FE/t for %s',
    'gui.theoreticalsciences.codex.energy_total': '%s FE over %s',
    'gui.theoreticalsciences.codex.min_tier': 'Needs coil tier %s or better',
    'gui.theoreticalsciences.codex.research_needed': 'Research: %s',
    'gui.theoreticalsciences.codex.chance': '%s%% chance',
    'gui.theoreticalsciences.codex.compaction': 'Singularity Compactor: %s of',
    'gui.theoreticalsciences.codex.layer': 'Layer %s of %s',
    'gui.theoreticalsciences.codex.layer_up': 'Layer above',
    'gui.theoreticalsciences.codex.layer_down': 'Layer below',
    'gui.theoreticalsciences.codex.structure': 'Structure',
    'gui.theoreticalsciences.codex.blocks_needed': 'Blocks needed:',
    'gui.theoreticalsciences.codex.any_of': 'Any of these fits',
    'gui.theoreticalsciences.codex.open_entry': 'Click to open its entry',
    'gui.theoreticalsciences.codex.progress': '%s of %s read',
}


# ------------------------------------------------------------------------------------------------ helpers
def _load_json(f):
    with open(f) as fh:
        return json.load(fh)


class _Data:
    """Names, recipes (what makes each thing and what each thing goes into) and the Core's Forge recipes."""

    def __init__(self, g):
        self.g = g
        here = os.path.dirname(os.path.abspath(__file__))
        self.here = here
        self.LANG = g['LANG']
        self.C = _load_json(os.path.join(here, 'content.json'))
        self.M = _load_json(os.path.join(here, 'machines.json'))
        self.fluids = {f['id'] for f in self.C['fluids']}
        self.mach_of_type = {m['recipe']: m['id'] for m in self.M}
        self.made, self.used = {}, {}
        self._recipes(g['REC'])
        self._forge()

    def name(self, i):
        i = i.split(':')[-1]
        for k in ('item.', 'block.', 'fluid_type.', 'entity.'):
            v = self.LANG.get(f'{k}{NS}.{i}')
            if v:
                return v
        return i.replace('_', ' ').title()

    def vname(self, x):
        """A display name for an ingredient id ('ns:item' or '#tag')."""
        if x.startswith('#'):                  # a tag: "#c:ingots/iron" -> "Iron Ingots (any)"
            parts = x[1:].split(':')[-1].split('/')
            words = (parts[1:] + parts[:1]) if len(parts) == 2 else parts
            return ' '.join(words).replace('_', ' ').title() + ' (any)'
        if x.startswith(NS + ':') or ':' not in x:
            return self.name(x)
        return x.split(':')[-1].replace('_', ' ').title()

    def _add(self, outs, where, ins, fl, kind):
        for o in outs:
            if not o:
                continue
            o = o.split(':')[-1] if o.startswith(NS + ':') else o
            self.made.setdefault(o, []).append((where, ins, fl, kind))
            for i, _ in ins + fl:
                if i.startswith(NS + ':'):
                    self.used.setdefault(i.split(':')[1], set()).add(o)

    def _recipes(self, REC):
        for f in glob.glob(os.path.join(REC, '**', '*.json'), recursive=True):
            try:
                r = _load_json(f)
            except ValueError:
                continue
            if r.get('secret'):
                continue
            t = r.get('type', '')
            ing = lambda x: (x.get('item') or '#' + x.get('tag', '?')) if isinstance(x, dict) else (
                (x[0].get('item') or '#' + x[0].get('tag', '?')) if isinstance(x, list) and x and isinstance(x[0], dict) else '?')
            res = r.get('result') if isinstance(r.get('result'), dict) else {}
            if t.startswith('minecraft:crafting_shaped') or t == f'{NS}:extreme_shaped':
                cnt = {}
                for row in r.get('pattern', []):
                    for ch in row:
                        if ch != ' ':
                            cnt[ch] = cnt.get(ch, 0) + 1
                ins = [(ing(r['key'][k]), n) for k, n in cnt.items() if k in r.get('key', {})]
                where = 'a crafting grid' if t.startswith('minecraft') else 'the $(item)Extreme Assembly Table$()'
                self._add([res.get('id')], where, ins, [], 'craft')
            elif t == 'minecraft:crafting_shapeless' or t == f'{NS}:extreme_shapeless':
                cnt = {}
                for x in r.get('ingredients', []):
                    k = ing(x)
                    cnt[k] = cnt.get(k, 0) + 1
                where = 'a crafting grid' if t.startswith('minecraft') else 'the $(item)Extreme Assembly Table$()'
                kind = 'unpack' if len(cnt) == 1 and res.get('count', 1) == 9 else 'craft'
                self._add([res.get('id')], where, list(cnt.items()), [], kind)
            elif t == 'minecraft:stonecutting':
                self._add([res.get('id')], 'a $(item)Stonecutter$()', [(ing(r.get('ingredient', {})), 1)], [], 'cut')
            elif t in ('minecraft:smelting', 'minecraft:blasting'):
                self._add([res.get('id')], 'a furnace', [(ing(r.get('ingredient', {})), 1)], [], 'smelt')
            elif t == f'{NS}:compaction':
                self._add([res.get('id')], '$(item)Singularity Compactor$()', [(ing(r.get('ingredient', {})), r.get('count', 1))], [], 'compact')
            elif t == f'{NS}:convergence':
                cnt = {}
                for x in [r.get('core', {})] + r.get('pedestals', []):
                    k = ing(x)
                    cnt[k] = cnt.get(k, 0) + 1
                self._add([res.get('id')], 'the $(item)Convergence Altar$()', list(cnt.items()), [], 'craft')
            elif t.startswith(NS + ':') and t.split(':')[1] in self.mach_of_type:
                ins = [(ing(x), x.get('count', 1)) for x in r.get('item_inputs', [])]
                fl = [(x['fluid'], x.get('amount', 0)) for x in r.get('fluid_inputs', []) if 'fluid' in x]
                outs = [o.get('id') for o in r.get('item_outputs', [])] + [o.get('id') for o in r.get('fluid_outputs', [])]
                where = '$(item)' + self.name(self.mach_of_type[t.split(':')[1]]) + '$()'
                self._add(outs, where, ins, fl, 'machine')

    def _forge(self):
        """The Core's Forge recipes live in Java (transcend/CoreForge.java): read the fixed list from there."""
        f = os.path.join(self.here, '..', 'src', 'main', 'java', 'com', 'theoreticalsciences', 'transcend', 'CoreForge.java')
        self.forge = {}
        if not os.path.exists(f):
            return
        src = open(f).read()
        pat = re.compile(r'new Recipe\("([^"]+)",\s*i\("([^"]+)",\s*(\d+)\),\s*List\.of\(((?:\s*i\("[^"]+",\s*\d+\),?)+)\)\)')
        for m in pat.finditer(src):
            out, n = m.group(2), int(m.group(3))
            ins = [(NS + ':' + a, int(b)) for a, b in re.findall(r'i\("([^"]+)",\s*(\d+)\)', m.group(4))]
            self.forge.setdefault(out, []).append((n, ins))
            self._add([out], "the reborn Core's $(item)Forge$()", ins, [], 'forge')

    def made_text(self, i):
        rs = self.made.get(i)
        if not rs:
            return ''
        # prefer a real recipe to taking a storage block apart or cutting a block in a stonecutter
        good = [r for r in rs if r[3] not in ('unpack', 'cut', 'smelt')] or rs
        where, ins, fl, kind = min(good, key=lambda r: (len(r[1]) + len(r[2]), r[0]))
        parts = []
        for x, n in ins[:5]:
            if kind == 'forge' and n == 0:
                parts.append(f'a {self.vname(x)} (kept, not used up)')
            else:
                parts.append(f'{n:,} {self.vname(x)}' if n > 1 else self.vname(x))
        for x, n in fl[:2]:
            parts.append(f'{n:,} mB {self.vname(x)}')
        more = ' and more' if len(ins) > 5 or len(fl) > 2 else ''
        verb = 'Forged at' if kind == 'forge' else 'Made in' if kind in ('machine', 'compact') else 'Made on' if 'Table' in where or 'Altar' in where else 'Made in'
        # 1.1.0 (text rewrite): only where it is made - the recipe pages and JEI/EMI show what goes in
        return f'{verb} {where}.'

    def used_text(self, i):
        u = sorted({self.name(x) for x in self.used.get(i, ())} - {self.name(i)})
        if not u:
            return ''
        return '$(thing)Used in$(): ' + ', '.join(u[:8]) + (' and more.' if len(u) > 8 else '.')


def _entries(BA):
    out = {}
    for f in sorted(glob.glob(os.path.join(BA, 'entries', '*', '*.json'))):
        out[(f.split(os.sep)[-2], os.path.basename(f)[:-5])] = f
    return out


def _subject(page, eid, e):
    """Which thing a page is about (for its "Made in" line): a spotlight's first item, or a node's own icon."""
    if page.get('type') == 'patchouli:spotlight' and page.get('item'):
        return page['item'].split(',')[0].split('{')[0].split(':')[-1]
    if eid.startswith('node_'):
        return e.get('icon', '').split('{')[0].split(':')[-1]
    return None


# "Made in X from a, b, c." up to the end of the page or the next line break
_MADE = re.compile(r'(?:Made|Forged) (?:in|on|at) (?:a crafting grid|a furnace|a \$\(item\)Stonecutter\$\(\)|(?:the )?(?:reborn Core\'s )?\$\(item\)[^$]*\$\(\)) from [^$]*?(?=\$\(br|$)')


# ------------------------------------------------------------------------------------------------ refresh
def refresh(g):
    BA = g['BA']
    d = _Data(g)
    w = g['w']
    _new_chapters(g, d)
    _rewrite_stale(g, d)
    added = _fill_missing(g, d)
    files = _entries(BA)
    fixed = 0
    for (cat, eid), f in files.items():
        e = _load_json(f)
        ch = False
        for pg in e.get('pages', []):
            t = pg.get('text')
            if not t:
                continue
            s = _subject(pg, eid, e)
            if s and s not in d.made and s.endswith('_cell') and s[:-5] in d.fluids:
                s = s[:-5]                     # a fluid's page shows its containment cell
            nt = t
            if s and _MADE.search(t):
                mt = d.made_text(s)
                if mt:
                    nt = _MADE.sub(lambda m: mt, t, count=1)
            # the Codex shows recipes itself now (and JEI still does)
            nt = nt.replace('(it is also a crafting table). JEI shows the pattern.', '(it is also a crafting table). The recipe pages at the end of this entry - and JEI - show the pattern.')
            if nt != t:
                pg['text'] = nt
                ch = True
        if ch:
            w(f, e)
            fixed += 1
    print('round30 codex: entries brought up to date:', fixed, '; new nodes:', added)


def _page_text(g, cat, eid, n, text):
    f = os.path.join(g['BA'], 'entries', cat, eid + '.json')
    if not os.path.exists(f):
        return False
    e = _load_json(f)
    if n >= len(e['pages']):
        return False
    e['pages'][n]['text'] = text
    g['w'](f, e)
    return True


def _rewrite_stale(g, d):
    """Pages whose numbers changed since they were written: rebuilt from the data as it is now."""
    REC = g['REC']
    # ---- Pleroma (round 26: every other mod's Essence too)
    _page_text(g, 'endgame', 'pleroma', 0,
               'Pour every fluid of this mod - and water and lava - into the $(item)Pleroma Confluence$(): '
               '$(thing)1,000,000 mB$() of each. Every other mod installed counts too, through its $(fluid)Essence$() '
               '(distilled in the $(item)Essence Distillery$()): $(thing)64,000 mB$() of each mod\'s Essence. '
               'The Confluence keeps a running total; when every one is full, 1,000 mB of $(fluid)Pleroma$() appears.')
    _page_text(g, 'endgame', 'pleroma', 1,
               'The old word means \'fullness\': the totality of everything. Fitting. It accepts fluids from any pipe or AE2 on any side, and Pleroma can be drained from any side. '
               'Its screen (and JEI) shows what is still missing.$(br2)Mine is fed by every tank I own.')
    # ---- the Omnium Ingot in the Concept Compressor
    try:
        a = _load_json(os.path.join(REC, 'concept_compression', 'omnium_ingot.json'))
        b = _load_json(os.path.join(REC, 'concept_compression', 'omnium_ingot_transfinite.json'))
        e = _load_json(os.path.join(REC, 'extreme', 'omnium_ingot.json'))
        cat_of = lambda r: next((x.get('count', 1) for x in r['item_inputs'] if x.get('item') == f'{NS}:omniscience_catalyst'), '?')
        cnt = {}
        for row in e['pattern']:
            for ch in row:
                if ch != ' ':
                    cnt[ch] = cnt.get(ch, 0) + 1
        by = {e['key'][k].get('item', '').split(':')[-1]: n for k, n in cnt.items()}
        nb = b['item_outputs'][0].get('count', 1)
        txt = (f'The $(item)Omnium Ingot$() needs {by.get("omniscience_catalyst", "?")} catalysts, {by.get("cosmic_neutronium_ingot", "?")} Cosmic Neutronium, '
               f'{by.get("crystalline_matrix_ingot", "?")} Crystalline Matrix and an Omega Point in the 9x9 table.$(br2)'
               f'In bulk: with $(thing)cosmic neutronium coils$() the Conceptual Singularity Compressor forges one from {cat_of(a)} catalysts and stacks of the late metals; '
               f'with $(thing)transfinite coils$(), {nb} at a time from {cat_of(b)}.')
        _page_text(g, 'endgame', 'mass_crafting', 4, txt)
        _page_text(g, 'endgame', 'concept_compressor', 3,
                   'The Omniscience Catalyst is made in the $(item)Extreme Assembly Table$() from every singularity there is. Here, with cosmic neutronium or '
                   f'transfinite coils, catalysts are forged into $(item)Omnium$() in bulk - {cat_of(a)} catalysts and stacks of the late metals for one ingot, '
                   f'or {nb} ingots from {cat_of(b)} with transfinite coils - with no 9x9 grid to fill by hand.')
    except (OSError, ValueError, KeyError, IndexError, StopIteration):
        pass


def _new_chapters(g, d):
    te = g['text_entry']
    te('endgame', 'transcendental', 'The Transcendental', 'forged_omnipotence_core', [
        'Beyond every universe, beyond even Hyperdimensional Reality, lies the $(thing)Transcendental$(). The way in is made, not found: '
        'with the $(item)Proof of Everything$() and $(thing)Beyond the Omega$() researched, the $(item)Omnilex Engine$() does the impossible and '
        'collapses into a five-dimensional $(thing)hyperobject$(). It eats its own structure, then drags in everything nearby; whoever reaches its heart is carried across.',
        'The hyperobject stays where the engine stood, a doorway that pulls only those who come close.$(br2)'
        'The first arrival builds the place: a disc of transcendent stone a hundred and twelve blocks across, a Hall of Matter on its pedestals, '
        'eight reliquaries holding the controllers of the great machines of the endgame, and a rift back to the Overworld. Carry an $(item)Identity Kernel$().',
        'At its centre, on a dais within eight omnium pillars, stands the $(item)Omnipotence Core$(). Its screen lists every item of every mod installed: '
        '$(thing)Materialize$() any of them, as many as a stack, as often as you like.',
        '$(thing)Access denied$(): the Core lists everything of $(thing)this$() mod too, but will not make any of it - ask, and it refuses. '
        'Nor does it make what lies beyond Omnipotence, nor the creative items of other mods (only the reborn Core\'s $(item)Forge$() makes those).$(br2)'
        'The Core cannot be broken, and nothing of Omnipotency can be built elsewhere. What comes next is the $(thing)Shutdown Sequence$().'],
        adv='beyond_the_omega', sort=-3, spot=['forged_omnipotence_core', 'proof_of_everything', 'identity_kernel'])
    te('world', 'arriving_safely', 'Arriving Safely', 'brane_shifter', [
        'Every journey to another dimension - by Brane Shifter, rift, wormhole, falling through the void or the hyperobject - now takes a moment: the travel '
        'animation builds up on screen for under two seconds, and only then do you jump. The loading screen of each world is its own.',
        'Wherever you arrive, you arrive standing: never inside blocks, never in mid-air, never on top of a world\'s ceiling (the Quantum Foam\'s Relativity '
        'Breakdown, the build limit), never in a fluid or on something that burns. The ground nearby is searched for two free blocks over solid ground - '
        'open sky first, otherwise the spot nearest the height you left from.',
        'Only when there is nowhere at all is a small platform built for you, with the room above it cleared. Laws of the place still apply: carry its attunement item.',
        'The $(thing)Planetary Interior$() is the exception to open sky: it has a bedrock roof, so you arrive under it, on dry rock as near y 100 as the cave allows, with a Mantle Breach beside you for the way back.'],
        sort=1, spot=['brane_shifter'])


# things that need more than the generic node text: (parent chapter, extra pages)
HAND = {
    'fractal_core': (('mathematics', 'fractal_space'), ['Dropped by the $(thing)Fractal Wyrm$() of the Platonic Realm, a serpent made of smaller copies of itself. The smallest copy is the core.']),
    'proof_fragment': (('mathematics', 'fractal_space'), ['Dropped by the $(thing)Axiom Sentinel$() of the Platonic Realm - a page of the proof it was writing about you.']),
    'oracle_lemma': (('endgame', 'alpha_omega'), ['$(thing)Creative mode only$(). Used inside one of Alpha Omega\'s trial chambers, the chamber counts as solved - for pack makers and testers. It cannot be crafted and the Core will not make it.']),
    'laplaces_demon': (('basics', 'tuning'), ['$(thing)Creative mode only$(). Use it on a multiblock controller: it tunes the machine for the recipe its inputs match (or hold a recipe\'s product in your off hand to choose one) - every control, the coolant, containment and drift. For pack makers and testers.']),
    'akashic_record': (('basics', 'research'), ['$(thing)Creative mode only$(). Every research entry and every symbol of the Book of Symbols, learned at once. It cannot be crafted and the Core will not make it.']),
    'omnipotence_shard': (('endgame', 'the_shutdown'), ['What is left of the Core after the Shutdown Sequence. Once the Transcendental is rekindled, the reborn Core\'s Forge resonates more of them.']),
    'omnipotent_ingot': (('endgame', 'the_core_forge'), []),
    'transcendence_crystal': (('endgame', 'the_core_forge'), []),
    'eschaton_core': (('endgame', 'the_core_forge'), ['The heart of every endgame artefact, and of each creative item the Forge makes.']),
    'omnipotent_edge': (('endgame', 'beyond_omnipotence'), ['Cuts through any armour and any invulnerability. Use: $(thing)Erasure$() - hostile creatures in a cone before you cease to exist. Sneak-use: $(thing)Chronal Stasis$() - time stops for every creature within 24 blocks for 10 seconds.']),
    'singularity_scepter': (('endgame', 'beyond_omnipotence'), ['Use to put a singularity where you look, up to 64 blocks away; sneak-use to change which: Black Hole, White Hole, Tesseract, Entanglement, Big Bang. It never harms players.']),
    'crown_of_transcendence': (('endgame', 'beyond_omnipotence'), ['One of the four pieces of the $(item)Raiment of Transcendence$(). Each piece has its own gift; the whole set makes you untouchable, lets you fly and holds every dimension\'s laws away from you.']),
    'mantle_of_omnipotence': (('endgame', 'beyond_omnipotence'), ['One of the four pieces of the $(item)Raiment of Transcendence$(). Each piece has its own gift; the whole set makes you untouchable, lets you fly and holds every dimension\'s laws away from you.']),
    'greaves_of_the_absolute': (('endgame', 'beyond_omnipotence'), ['One of the four pieces of the $(item)Raiment of Transcendence$(). Each piece has its own gift; the whole set makes you untouchable, lets you fly and holds every dimension\'s laws away from you.']),
    'treads_of_infinity': (('endgame', 'beyond_omnipotence'), ['One of the four pieces of the $(item)Raiment of Transcendence$(). Each piece has its own gift; the whole set makes you untouchable, lets you fly and holds every dimension\'s laws away from you.']),
    'lawgivers_codex': (('endgame', 'beyond_omnipotence'), ['Open it to rewrite the laws of the place you stand in: gravity, time and weather, which creatures may appear and how strong they are. Look at a creature while opening it to rewrite that creature too. "Restore" puts the natural laws back.']),
    'entropy_censer': (('endgame', 'entropy_censer'), ['Sneak-use to choose any harmful effect of any mod, its level (up to 255), how long it lasts (up to an hour) and the radius of its cloud. Use on a creature to lay it on; use in the air to leave a lingering cloud up to 48 blocks away.']),
    'singularity_cluster': (('fabrication', 'more_singularities'), ['When a pack has so many singularities that one recipe cannot hold them, nine at a time are condensed into a $(item)Singularity Cluster$() on the Extreme Assembly Table - and nine clusters into a Hypercluster, and so on. The Omniscience Catalyst then asks for the clusters.']),
    'simulated_universe': (('endgame', 'simulated_universes'), ['Each mod with creative items has its own, forged at the reborn Core from universe seeds, pocket universes, Omnipotent Ingots and that mod\'s own most advanced things. The Forge page shows exactly which.']),
    'specimen_bundle': (('basics', 'extreme_table_first'), ['One for each mod: one of every meat and drop (with an egg) or every crop, seed, fruit and flower (with bone meal) that mod has, gathered on the Extreme Assembly Table. Too many bundles are bound nine at a time into an Archive. The Biosphere Compendium and the Primordial Harvest want them all.']),
    'equilibrium_lattice': (('basics', 'stabilisers'), ['The fourth and perfect stabiliser: a multiblock holding it never drifts, never wears, never quenches and never faults.']),
    'rekindled_heart': (('endgame', 'second_core'), []),
    'echo_lattice': (('endgame', 'second_core'), []),
    'omnipotence_seed': (('endgame', 'second_core'), ['Four of them go into an Omnipotence Core of your own.']),
    'demiurge_hand': (('endgame', 'second_core'), ['The only thing that can handle a forged Omnipotence Core. Sneak and use it in the air to change mode:$(li)$(thing)Tune$(): turn the Core\'s resonance (sneak: the other way) until it is in tune with the place$(li)$(thing)Activate$(): wake a Core in tune - or let an awake one sleep$(li)$(thing)Move$(): lift the Core into your inventory (it must be tuned again where you set it down)$(br2)It does not work on the Core of the Transcendental.']),
    'mod_essence': (('chemistry', 'essences'), ['One fluid for each mod installed: every fluid of that mod distilled together in the $(item)Essence Distillery$(). The Pleroma Confluence wants 64,000 mB of each mod\'s Essence. Essences live in tanks and pipes only.']),
}


def _fill_missing(g, d):
    """Every item, block, fluid, machine and creature gets a node of its own (unless a chapter is entirely about it)."""
    import round5
    BA, w, q = g['BA'], g['w'], g['q']
    files = _entries(BA)
    taken = set()
    spotted = {}
    mention = {}
    for (cat, eid), f in files.items():
        if eid.startswith('guide_'):
            continue                     # round 33: the Getting Started steps are nobody's home chapter
        e = _load_json(f)
        if eid.startswith('node_'):
            taken.add(eid[5:])
        taken.add(e.get('icon', '').split('{')[0].split(':')[-1])
        if not eid.startswith('node_'):
            taken.add(eid)
            for p in e['pages']:
                for x in (p.get('item') or '').split(','):
                    if x:
                        spotted.setdefault(x.split('{')[0].split(':')[-1], (cat, eid))
            for m in re.finditer(r'\$\((?:item|thing|fluid)\)([^$]+)\$\(\)', ' '.join(p.get('text', '') for p in e['pages'])):
                mention.setdefault(m.group(1).strip().lower(), (cat, eid))
    C, M = d.C, d.M
    things = {}
    for i in C['items']:
        things[i['id']] = ('item', i.get('lore', ''))
    for b in C['blocks']:
        things[b['id']] = ('block', b.get('lore', ''))
    for m in M:
        things[m['id']] = ('machine', '')
    for fl in d.fluids:
        things[fl] = ('fluid', '')
    creatures = g.get('CREATURES', {})
    LORE11 = {}
    for f in sorted(glob.glob(os.path.join(d.here, 'lore11', 'lore_*.json'))):
        try:
            LORE11.update(_load_json(f))
        except ValueError:
            pass
    added = 0
    unhomed = []
    for i, (kind, lore) in sorted(things.items()):
        if i.endswith('_spawn_egg') or re.search(r'_(stairs|slab|wall)$', i):
            continue
        icon_id = i + '_cell' if kind == 'fluid' else i
        if i in HAND:
            parent = HAND[i][0]
        else:
            if i in taken or icon_id in taken:
                continue
            parent = spotted.get(icon_id) or mention.get(d.name(i).lower())
            if not parent and i.endswith('_singularity'):
                parent = next((p for p in (('fabrication', 'more_singularities'), ('endgame', 'mass_crafting')) if p in files), None)
        if not parent or (parent[0], parent[1]) not in files:
            unhomed.append(i)
            continue
        if os.path.exists(os.path.join(BA, 'entries', parent[0], 'node_' + i + '.json')) and i not in HAND:
            continue
        nm = d.name(i)
        lo = d.LANG.get(f'item.{NS}.{i}.lore') or lore or LORE11.get(i, {}).get('flavour') or (('$(fluid)' if kind == 'fluid' else '$(item)') + nm + '$().')
        pages = [{'type': 'patchouli:spotlight', 'item': q(icon_id), 'title': nm, 'text': lo}]
        for t in (HAND.get(i, (None, []))[1]):
            pages.append({'type': 'patchouli:text', 'text': t})
        t2 = []
        mt = d.made_text(i)
        if mt:
            t2.append(mt)
        dropped = sorted({cn for c, (cn, dr) in creatures.items() for (x, *_) in dr if x.split(':')[-1] == i})
        if dropped:
            t2.append('$(thing)Dropped by$(): ' + ', '.join(dropped[:4]) + '.')
        ut = d.used_text(i)
        if ut:
            t2.append(ut)
        if t2:
            pages.append({'type': 'patchouli:text', 'text': '$(br2)'.join(t2)})
        pe = _load_json(files[parent])
        ent = {'name': nm, 'icon': q(icon_id), 'category': f'{NS}:{parent[0]}', 'sortnum': 600 + added, 'pages': pages}
        if pe.get('advancement'):
            ent['advancement'] = pe['advancement']
        w(os.path.join(BA, 'entries', parent[0], 'node_' + i + '.json'), ent)
        round5.NODES[(parent[0], 'node_' + i)] = parent[1]
        added += 1
    # creatures without a node
    for cid, (cname, drops) in creatures.items():
        if cid in taken or cid + '_spawn_egg' in taken:
            continue
        parent = spotted.get(cid + '_spawn_egg') or mention.get(cname.lower()) or mention.get(cname.lower() + 's') or mention.get(cname.lower() + 'es')
        if not parent:
            unhomed.append(cid)
            continue
        dn = ', '.join(d.vname(x if ':' in x else NS + ':' + x) for x, *_ in drops[:4])
        ent = {'name': cname, 'icon': q(cid + '_spawn_egg'), 'category': f'{NS}:{parent[0]}', 'sortnum': 600 + added,
               'pages': [{'type': 'patchouli:spotlight', 'item': q(cid + '_spawn_egg'), 'title': cname,
                          'text': f'$(thing){cname}$().' + (f'$(br2)$(thing)Drops$(): {dn}.' if dn else '')}]}
        w(os.path.join(BA, 'entries', parent[0], 'node_' + cid + '.json'), ent)
        round5.NODES[(parent[0], 'node_' + cid)] = parent[1]
        added += 1
    # research topics that have neither a chapter nor a node (the first research of all, for one): a node beside
    # "The Research Matrix", open from the start - it says what to do to begin
    covered = set()
    for (cat, eid), f in _entries(BA).items():
        a = _load_json(f).get('advancement', '')
        if 'research/' in a:
            covered.add(a.split('research/')[1])
        if eid.startswith('node_research_'):
            covered.add(eid[len('node_research_'):])
    for f in sorted(glob.glob(os.path.join(g['D'], 'ts_research', '*.json'))):
        rid = os.path.basename(f)[:-5]
        if rid in covered or rid in ('conquered_alpha_omega', 'beyond_the_omega') or ('basics', 'research') not in files:
            continue
        r = _load_json(f)
        title = r.get('title', rid.replace('_', ' ').title())
        t2 = [f'$(thing)Insight$(): {r.get("insight", 0)}']
        req = [x.split(':')[-1] for x in r.get('requires', [])]
        if req:
            t2.append('$(thing)Builds on$(): ' + ', '.join(x.replace('_', ' ').title() for x in req) + '.')
        else:
            t2.append('$(thing)Builds on$(): nothing - it is where research begins.')
        if r.get('puzzle', {}).get('goal'):
            t2.append('$(thing)Derivation$(): ' + r['puzzle']['goal'])
        ent = {'name': title, 'icon': r.get('icon', q('research_terminal')), 'category': f'{NS}:basics', 'sortnum': 600 + added,
               'pages': [{'type': 'patchouli:spotlight', 'item': r.get('icon', q('research_terminal')), 'title': title,
                          'text': f'$(thing){title}$()$(br2){r.get("description", "")}'},
                         {'type': 'patchouli:text', 'text': '$(br2)'.join(t2)}]}
        w(os.path.join(BA, 'entries', 'basics', 'node_research_' + rid + '.json'), ent)
        round5.NODES[('basics', 'node_research_' + rid)] = 'research'
        added += 1
    print('round30 codex: things without a node:', len(unhomed), unhomed[:30])
    return added


# ------------------------------------------------------------------------------------------------ export
def _item_id(s):
    s = s.strip().split('{')[0].split('[')[0]
    return s if ':' in s else f'{NS}:{s}'


def _page(pg):
    t = pg.get('type', '')
    out = None
    if t == 'patchouli:text':
        out = {'type': 'text', 'text': pg.get('text', '')}
    elif t == 'patchouli:spotlight':
        out = {'type': 'spotlight', 'items': [_item_id(x) for x in pg.get('item', '').split(',') if x.strip()], 'text': pg.get('text', '')}
    elif t == 'patchouli:multiblock':
        mb = pg.get('multiblock', {})
        out = {'type': 'multiblock', 'name': pg.get('name', ''), 'text': pg.get('text', ''),
               'pattern': mb.get('pattern', []), 'mapping': mb.get('mapping', {})}
    elif t == 'patchouli:crafting':
        out = {'type': 'recipe', 'recipes': [r for r in (pg.get('recipe'), pg.get('recipe2')) if r], 'text': pg.get('text', '')}
    elif pg.get('text'):
        out = {'type': 'text', 'text': pg['text']}
    if out is not None and pg.get('title'):
        out['title'] = pg['title']
    return out


# round 30b: entries that named no research were open from the very first moment (the whole endgame among them).
# Each now waits for a research: the one of the recipe that makes what it shows, the Core's Forge for Forge items,
# or a fitting one by subject - only the first-steps chapters of the Basics stay open from the start.
_GATE_BY_NAME = {
    'void_wraith': 'field_engineering', 'destabilised_husk': 'field_engineering', 'containment_failure': 'field_engineering',
    'law_wraith': 'hyperdimensional_reality', 'paradox_echo': 'causality_violation', 'nanite_swarm': 'nanotechnology',
    'akashic_record': 'research_synthesis', 'laplaces_demon': 'research_synthesis', 'equilibrium_lattice': 'the_rekindling',
    'essences': 'fractional_distillation', 'mod_essence': 'fractional_distillation', 'stabilisers': 'field_engineering',
    'me_ports': 'assembly_lines', 'auto_assembler': 'assembly_lines', 'anomaly_spawner': 'synthetic_biology',
    'convergence_altar': 'assembly_lines', 'more_singularities': 'assembly_lines', 'specimen_bundle': 'assembly_lines',
    'containment_cells': 'cryogenics', 'tuning': 'field_engineering', 'singularity_cluster': 'assembly_lines',
    'principles': 'field_engineering', 'arriving_safely': 'brane_cosmology', 'attunement_hierarchy': 'brane_cosmology',
    'containment_caches': 'brane_cosmology', 'cosmic_plating': 'stellar_engineering', 'cosmic_tiles': 'stellar_engineering',
    'proof_floor': 'omnilex', 'proof_membrane': 'omnilex', 'return_rift': 'hyperdimensional_reality', 'tidal_ore': 'black_hole_interior',
    'spacetime_bricks': 'special_relativity', 'spacetime_panel': 'special_relativity', 'spacetime_tiles': 'special_relativity',
    'void_bricks': 'dark_sector', 'void_pillar': 'dark_sector', 'void_tiles': 'dark_sector', 'transcendent_cache': 'beyond_the_omega',
    'hex_glass': 'metamaterials', 'cloaking_glass': 'metamaterials',
    'the_shutdown': 'beyond_the_omega', 'beyond_omnipotence': 'beyond_the_omega', 'uncopyable': 'beyond_the_omega',
    'miniatures': 'beyond_the_omega', 'the_sundering_materials': 'the_sundering', 'the_rekindling': 'the_sundering',
}
_OPEN_FROM_START = {'insight', 'research', 'symbols', 'power', 'thermoelectric_generator', 'extreme_table_first', 'assembler',
                    'recipe_cost', 'thought_experiments', 'world_numbers', 'node_research_quantum_foundations'}


def _gate(g, out):
    import re
    rec_research = {}
    for f in glob.glob(os.path.join(g['REC'], '**', '*.json'), recursive=True):
        try:
            d = _load_json(f)
        except Exception:
            continue
        r = d.get('research')
        if not r:
            continue
        outs = [o.get('id') for o in d.get('item_outputs', []) if isinstance(o, dict)] + [o.get('id') for o in d.get('fluid_outputs', []) if isinstance(o, dict)]
        if isinstance(d.get('result'), dict):
            outs.append(d['result'].get('id'))
        for o in outs:
            if o:
                rec_research.setdefault(o.split(':')[-1], r.split(':')[-1])
    src = open(os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'src/main/java/com/theoreticalsciences/transcend/CoreForge.java')).read()
    forge = set(re.findall(r'new Recipe\("\w+", i\("(\w+)"', src))
    known = {os.path.basename(f)[:-5] for f in glob.glob(os.path.join(g['D'], 'ts_research', '*.json'))}
    still_open = []
    for k, e in out.items():
        if e.get('research'):
            continue
        cat, eid = k.split('/', 1)
        name = eid[5:] if eid.startswith('node_') else eid
        if cat == 'basics' and (eid in _OPEN_FROM_START or eid.startswith('guide_')):
            continue                     # (round 33: a guide step waits only for the research it names, if any)
        r = _GATE_BY_NAME.get(name) or (g.get('MACHINE_RESEARCH') or {}).get(name)
        if not r and name.endswith('_singularity'):
            r = 'assembly_lines'
        if not r:
            subjects = [name] + [i.split(':')[-1] for p in e['pages'] for i in p.get('items', [])] + [e['icon'].split(':')[-1]]
            for sbj in subjects:
                if sbj in forge:
                    r = 'the_rekindling'
                    break
                if sbj in rec_research:
                    r = rec_research[sbj]
                    break
        if not r and cat == 'world':
            # a dimension's ores and landmarks wait for that dimension; decorative blocks for the material they are cut from
            for kw, rr in (('anti', 'cpt_symmetry'), ('cardinal', 'transfinite_hierarchy'), ('ordinal', 'transfinite_hierarchy'),
                           ('hyperspherical', 'transfinite_hierarchy'), ('dilaton', 'brane_cosmology'), ('moduli', 'brane_cosmology'),
                           ('ejecta', 'white_holes'), ('infall', 'black_hole_interior'), ('foam', 'quantum_foam'), ('axiom', 'platonic_realm'),
                           ('answer_pedestal', 'platonic_realm'), ('observer_plate', 'platonic_realm'), ('arbor', 'hyperdimensional_reality'),
                           ('frozen_light', 'special_relativity'), ('dimension_laws', 'brane_cosmology'), ('falling_through', 'brane_cosmology')):
                if kw in name:
                    r = rr
                    break
            if not r:
                base = name.replace('polished_', '')
                for suf in ('_hazard_panel', '_pillar', '_plating', '_tiles', '_bricks', '_vent', '_panel', '_glass', '_ore'):
                    if base.endswith(suf):
                        base = base[:-len(suf)]
                for cand in (base, base + '_ingot', base + '_crystal', base + '_block', base + '_plate'):
                    if cand in rec_research:
                        r = rec_research[cand]
                        break
        if not r and cat == 'endgame':
            r = 'the_rekindling' if ('forge' in eid or 'core' in eid or 'universe' in eid) else 'the_sundering'
        if r and r in known:
            e['research'] = r
        else:
            still_open.append(k)
    print('round30 codex: open from the start:', len(still_open), still_open[:40])


def _guide(g, out, placed):
    """Round 33: the chapters that hang off a Getting Started step open with it (on every map), and wait for the
    research the guide names instead of the gate worked out above (or for none)."""
    G = g.get('GUIDE') or {}
    links = {f'{k[0]}/{k[1]}': (f'{s[0]}/{s[1]}', r) for k, (s, r) in G.get('links', {}).items()}
    for k, (step, r) in links.items():
        e = out.get(k)
        if e is None or step not in out:
            continue
        if r:
            e['research'] = r
        else:
            e.pop('research', None)
    for tab, nodes in placed.items():
        for i, (k, pos, par, kid) in enumerate(nodes):
            kk = f'{k[0]}/{k[1]}'
            if kk in links and links[kk][0] in out:
                sk = tuple(links[kk][0].split('/', 1))
                if sk not in par:
                    nodes[i] = (k, pos, list(par) + [sk], kid)
    print('round33 guide: steps', len(G.get('chain', [])), '; chapters linked to them:', sum(1 for k in links if k in out))


def _open_at_start(out, placed):
    """What a new player can read the moment they open the Codex (the same rule as CodexState: an entry waits for its
    research; one without opens with any of its parents on any map; the roots of the maps are open)."""
    par = {}
    for tab, nodes in placed.items():
        for k, pos, ps, kid in nodes:
            par.setdefault(f'{k[0]}/{k[1]}', set()).update(f'{p[0]}/{p[1]}' for p in ps)
    memo = {}

    def op(i, seen=()):
        if i in memo:
            return memo[i]
        e = out.get(i)
        if e is None or e.get('research'):
            r = False
        elif not par.get(i):
            r = True
        elif i in seen:
            return False
        else:
            r = any(op(p, seen + (i,)) for p in par[i])
        memo[i] = r
        return r
    opened = sorted(k for k in out if op(k))
    roots = sorted(k for k in opened if not par.get(k))
    print('round33 codex: readable at the start:', len(opened), 'entries; roots:', roots, '; the rest:', [k for k in opened if k not in roots][:40])


def export(g, entries, cats, placed, tabs, depth_of=None, regions=None):
    """Writes the Codex data the mod's own screen reads.

    entries: (category, entry) -> Patchouli entry; cats: category -> Patchouli category;
    placed: tab -> list of (entry key, (x, y), [parent keys], is_node); tabs: [(id, name, description, icon)]."""
    A, w = g['A'], g['w']
    root = os.path.join(A, 'ts_codex')
    for f in glob.glob(os.path.join(root, '*.json')):
        os.remove(f)
    key = lambda k: f'{k[0]}/{k[1]}'
    out = {}
    for k, e in entries.items():
        a = e.get('advancement', '')
        ent = {'title': e.get('name', k[1]), 'icon': _item_id(e.get('icon', f'{NS}:quantum_codex')), 'category': k[0],
               'pages': [p for p in (_page(pg) for pg in e.get('pages', [])) if p]}
        if 'research/' in a:
            ent['research'] = a.split('research/')[1]
        if k[1].startswith('node_'):
            ent['node'] = True
        out[key(k)] = ent
    _gate(g, out)
    _guide(g, out, placed)
    w(os.path.join(root, 'entries.json'), out)
    index = {'version': 1, 'home': tabs[0][0], 'tabs': [], 'categories': {c: v.get('name', c) for c, v in cats.items()}}
    total = 0
    for tab, n, desc, icon in tabs:
        nodes = placed.get(tab, [])
        if not nodes:
            continue
        index['tabs'].append({'id': tab, 'name': n, 'description': desc, 'icon': _item_id(icon)})
        w(os.path.join(root, tab + '.json'), {
            'id': tab, 'name': n, 'description': desc, 'icon': _item_id(icon),
            'nodes': [dict({'entry': key(k), 'x': p[0], 'y': p[1], 'parents': [key(x) for x in par]}, **({'node': True} if kid else {}))
                      for k, p, par, kid in nodes],
            'regions': (regions or {}).get(tab, [])})          # round 36: the tier bands drawn behind the map
        total += len(nodes)
    w(os.path.join(root, 'index.json'), index)
    _open_at_start(out, placed)
    # check: every map node's entry exists, and every entry is on some map
    on_map = {key(k) for ns in placed.values() for k, *_ in ns}
    missing = [x for x in out if x not in on_map]
    size = sum(os.path.getsize(f) for f in glob.glob(os.path.join(root, '*.json')))
    print(f'round30 codex: own Codex data: {len(out)} entries, {total} map nodes in {len(index["tabs"])} tabs, {size // 1024} KB; entries on no map: {len(missing)}', missing[:10])


def apply(g):
    g['LANG'].update(LANG)


def finish(g):
    """Nothing here: the Codex is refreshed and exported from round23.finish, after every round has added its chapters."""
    pass
