"""
Round 33: getting started.

Players starting a fresh world found the Codex open on a wall of machines that all needed Neutronium, with nothing
saying where to begin, where Neutronium comes from, or that sneak-using the Codex opens the research tree.

* A guided "Getting Started" chain on the Codex's main (Discovery) map: a clearly marked "Start Here" node, then one
  node per step, each explaining one thing concretely (what to mine and where, which block to craft it on, what to
  research next), with the real items in the spotlight - the Codex adds their recipe pages itself. Every number in the
  text (recipe counts, ore heights, loot, Insight, research costs, fuel values) is read from the data as generated, so
  it cannot go stale. The first steps are open from the start; the later ones wait for the research that makes them
  possible (Quantum Foundations, then the Casimir Effect), so the chain unfolds as the player progresses.
* The chapters that used to be open from the start (the Extreme Assembly Table, Insight, the research tree, power,
  the Assembler, the Casimir Press...) now hang off the guide step that explains them, so they open with it - and the
  base materials (Cherenkov Quartz, Neutronium, Dilithium, Degenerate Matter, the Codex itself) are no longer locked
  behind late research just because some late recipe also makes them. (g['GUIDE'] carries this to round23, which lays
  the chain out, and to round30_codex, which writes the links and gates into the Codex data.)
* Text for the Java side: the first-join gift and message, the Codex's tooltip, its first-open hint banner and its
  "Next steps" panel.

apply(): language keys (and round33_emi's, when that module exists). finish(): the guide entries - before
round23.finish builds the Codex.
"""
import glob
import json
import os

NS = 'theoreticalsciences'

LANG = {
    # first join (server/FirstJoinGift)
    'message.theoreticalsciences.first_join': 'Welcome to Theoretical Sciences! You have been given the Quantum Codex. Use it to read the guide (begin at the gold "Start Here" node); sneak + use it to open the Research Tree.',
    'message.theoreticalsciences.first_join_craft': 'Welcome to Theoretical Sciences! Craft a Quantum Codex (a Book and one Cherenkov Quartz, mined from deepslate) and use it: its "Start Here" node shows the way. Sneak + use it to open the Research Tree.',
    'message.theoreticalsciences.first_join_lookup': 'Tip: in any inventory, hover an item and press %s to look it up in the Codex.',
    # the Codex item's tooltip
    'tooltip.theoreticalsciences.codex_use': 'Use: open the Codex (start at "Start Here")',
    'tooltip.theoreticalsciences.codex_sneak': 'Sneak + use: open the Research Tree',
    'tooltip.theoreticalsciences.codex_lookup': 'In any inventory: hover an item and press %s to look it up',
    # the Codex screen
    'gui.theoreticalsciences.codex.intro_title': 'Welcome to the Quantum Codex',
    'gui.theoreticalsciences.codex.intro_1': 'Begin at the glowing "Start Here" node and follow the numbered steps downwards - each opens as you get there.',
    'gui.theoreticalsciences.codex.intro_2': 'Drag to move the map, mouse wheel to zoom, hover a node to light up what it links to, click to read. Search: bottom right.',
    'gui.theoreticalsciences.codex.intro_3': 'Sneak + use the Codex (or the Research button) opens the Research Tree. In an inventory, hover an item and press %s to look it up.',
    'gui.theoreticalsciences.codex.intro_ok': 'Got it',
    'gui.theoreticalsciences.codex.next_steps': 'Next steps',
    'gui.theoreticalsciences.codex.next_steps_tip': 'What you can do now: research ready to start, and guide steps not read yet (N)',
    'gui.theoreticalsciences.codex.next_research': 'Research you can start now',
    'gui.theoreticalsciences.codex.next_research_cost': '%s Insight (you have %s)',
    'gui.theoreticalsciences.codex.next_guide': 'Guide steps to read',
    'gui.theoreticalsciences.codex.next_none': 'Nothing waiting: gather Insight with the Spectral Analyzer, or explore the map.',
    'gui.theoreticalsciences.codex.next_open_research': 'Click to open the Research Tree',
    'gui.theoreticalsciences.codex.guide_locked': 'Opens with the research %s',
    'gui.theoreticalsciences.codex.start_here': 'Start here',
}

START = 'guide_00_start'


def apply(g):
    g['LANG'].update(LANG)
    try:
        import round33_emi
        g['LANG'].update(getattr(round33_emi, 'LANG', {}))
    except ImportError:
        pass


# ------------------------------------------------------------------------------------------------ data helpers
class _D:
    def __init__(self, g):
        self.g = g
        self.D, self.REC, self.LANG = g['D'], g['REC'], g['LANG']
        self.R = {os.path.basename(f)[:-5]: self._j(f) for f in glob.glob(os.path.join(self.D, 'ts_research', '*.json'))}
        self.G = {}
        for f in glob.glob(os.path.join(self.D, 'ts_glyphs', '*.json')):
            self.G.update(self._j(f))

    @staticmethod
    def _j(f):
        with open(f) as fh:
            return json.load(fh)

    def rec(self, rel):
        f = os.path.join(self.REC, rel + '.json')
        return self._j(f) if os.path.exists(f) else None

    def name(self, x):
        x = x.get('item') or ('#' + x.get('tag', '?')) if isinstance(x, dict) else x
        if x.startswith('#'):
            p = x[1:].split(':')[-1].split('/')
            return (' '.join(p[1:] + p[:1]) if len(p) == 2 else ' '.join(p)).replace('_', ' ').title()
        i = x.split(':')[-1]
        if x.startswith(NS + ':') or ':' not in x:
            for k in ('item.', 'block.', 'fluid_type.'):
                v = self.LANG.get(f'{k}{NS}.{i}')
                if v:
                    return v
        return i.replace('_', ' ').title()

    def it(self, x):
        return '$(item)' + self.name(x) + '$()'

    def grid(self, rel):
        """'36 Iron Ingot, 9 Copper Block, ...' for a shaped / shapeless recipe (crafting or Extreme Assembly Table)."""
        r = self.rec(rel)
        if not r:
            return '?'
        cnt = {}
        if 'pattern' in r:
            for row in r['pattern']:
                for ch in row:
                    if ch != ' ' and ch in r['key']:
                        cnt[ch] = cnt.get(ch, 0) + 1
            parts = [(n, r['key'][ch]) for ch, n in cnt.items()]
        else:
            order = []
            for x in r.get('ingredients', []):
                k = json.dumps(x, sort_keys=True)
                if k not in cnt:
                    order.append((k, x))
                cnt[k] = cnt.get(k, 0) + 1
            parts = [(cnt[k], x) for k, x in order]
        out = ', '.join((f'{n} ' if n > 1 else '') + self.it(x) for n, x in parts)
        n = r.get('result', {}).get('count', 1)
        return out + (f' (makes {n})' if n > 1 else '')

    def machine(self, rel):
        """'4 Neutronium Ingot + 8 Dilithium Crystal -> 1 Neutronium Plate (64 FE/t, 5 s)'."""
        r = self.rec(rel)
        if not r:
            return '?'
        ins = ' + '.join(f'{x.get("count", 1)} {self.it(x)}' for x in r.get('item_inputs', []))
        outs = ', '.join(f'{x.get("count", 1)} {self.it(x["id"])}' for x in r.get('item_outputs', []))
        return f'{ins} → {outs} ({r.get("energy", 0):,} FE/t, {r.get("duration", 0) / 20:g} s)'

    def fuels(self, sub):
        out = []
        for f in sorted(glob.glob(os.path.join(self.REC, sub, '*.json'))):
            r = self._j(f)
            ins = r.get('item_inputs', [])
            if not ins:
                continue
            e, t = r.get('energy', 0), r.get('duration', 0)
            out.append((e * t, f'{self.it(ins[0])}: {e:,} FE/t for {t / 20:g} s'))
        out.sort()
        return [s for _, s in out]

    def loot(self, table, item):
        f = os.path.join(self.D, 'loot_table', 'chests', table + '.json')
        if not os.path.exists(f):
            return None
        for p in self._j(f).get('pools', []):
            for e in p.get('entries', []):
                if e.get('name', '').split(':')[-1] == item:
                    for fn in e.get('functions', []):
                        c = fn.get('count')
                        if isinstance(c, dict):
                            return f'{int(c.get("min", 1))}-{int(c.get("max", 1))}'
                    return '1'
        return None

    def ore_y(self, feat):
        f = os.path.join(self.D, 'worldgen', 'placed_feature', feat + '.json')
        if not os.path.exists(f):
            return '?'
        for p in self._j(f).get('placement', []):
            if p.get('type') == 'minecraft:height_range':
                h = p['height']
                return f'Y {h["min_inclusive"].get("absolute", "?")} to {h["max_inclusive"].get("absolute", "?")}'
        return '?'

    def research(self, rid):
        r = self.R.get(rid, {})
        cost = ', '.join(f'{c.get("count", 1)} {self.it(c["item"])}' for c in r.get('cost', []))
        return r.get('title', rid), r.get('insight', 0), cost

    def glyphs(self, rid):
        """The symbols a research's derivation needs that are learned from an item: [(symbol, its name, item)]."""
        pz = self.R.get(rid, {}).get('puzzle') or {}
        toks = (pz.get('form') or pz.get('expr', '')).split() + pz.get('lhs', '').split()
        out, seen = [], set()
        for t in toks:
            gl = self.G.get(t) or {}
            u = gl.get('unlock') or {}
            if 'item' in u and t not in seen:
                seen.add(t)
                sym = t
                if t.isascii() and len(t) > 1 and ':' in gl.get('meaning', ''):
                    sym = gl['meaning'].split(':')[0].strip()      # "pd" is drawn as ∂f/∂x
                out.append((sym, gl.get('name', t), u['item']))
        return out

    def depth(self):
        dep = {}

        def d(r, st=()):
            if r in dep:
                return dep[r]
            if r in st or r not in self.R:
                return 0
            v = 1 + max([d(x.split(':')[-1], st + (r,)) for x in self.R[r].get('requires', [])] + [-1])
            dep[r] = v
            return v
        for r in self.R:
            d(r)
        return dep


def _glyph_text(d, rid):
    gl = d.glyphs(rid)
    if not gl:
        return ''
    return 'Its derivation uses symbols you learn by $(thing)holding an item$() once: ' + '; '.join(
        f'$(thing){t}$() ({nm[0].lower() + nm[1:]}) from {d.it(i)}' for t, nm, i in gl) + '.'


# ------------------------------------------------------------------------------------------------ the guide
def _endgame_hints(te):
    """Round 34b: the end of the mod is no longer a secret until it happens. Once Beyond the Omega is reached, the Codex
    says plainly what the Shutdown Sequence leads to - the Sundering, the Rekindling, the Forge and the last things -
    and the later chapters (opened by those events) give the details."""
    te('endgame', 'beyond_the_shutdown', 'Beyond the Shutdown', 'transcendental_echo', [
        'The Omnipotence Core is not the end. Its $(thing)Shutdown Sequence$() destroys the Transcendental - and that is the way forward, not a loss.$(br2)What happens next, in order:',
        '$(li)$(bold)The Sundering$(): the Core leaves $(item)Shards of Omnipotence$() and the $(item)Echo of the Transcendental$(). The old machines turn shards into $(item)Omnipotence Dust$(), $(item)Apeiron Ingots$(), $(item)Pleroma Crystal$() and $(item)Logos Thread$().'
        '$(li)$(bold)The Rekindling$(): those build the $(item)Pleroma Reconstitutor$(), the largest machine there is. Fed the Echo, it rebuilds the Transcendental.'
        '$(li)$(bold)The Forge$(): the reborn Core can $(thing)forge$() - by solving a simulated universe\'s constants of nature - what even Omnipotence could not simply make.',
        'The last things, all forged at the reborn Core:$(li)$(item)Omnipotent Ingot$(), $(item)Crystal of Transcendence$(), $(item)Absolute Singularity$(), $(item)Eschaton Core$()'
        '$(li)the $(item)Omnipotent Edge$(), the $(item)Scepter of Singularities$(), the $(item)Raiment of Transcendence$() and the $(item)Dodecaract Excavator$()'
        '$(li)the $(item)Ex Nihilo Engine$() (endless power), the $(item)Lawgiver\'s Codex$(), the $(item)Censer of Entropy$() and the $(item)Equilibrium Lattice$()'
        '$(li)$(item)Eschaton Cells$() - every endgame item runs on their charge'
        '$(li)and at the very end, an $(item)Omnipotence Core$() of your own, with the $(item)Hand of the Demiurge$() to tune, wake and move it.',
        'Each of these has its own chapter, which opens as you get there: The Sundering opens the shards\' chapters, The Rekindling the Forge\'s.'],
        adv='beyond_the_omega', sort=59)


def finish(g):
    te = g['text_entry']
    _endgame_hints(te)
    d = _D(g)
    here = os.path.dirname(os.path.abspath(__file__))
    M = json.load(open(os.path.join(here, 'machines.json')))
    MR = g.get('MACHINE_RESEARCH') or {}
    dep = d.depth()
    it = d.it

    qf_t, qf_i, qf_c = d.research('quantum_foundations')
    ce_t, ce_i, ce_c = d.research('casimir_effect')
    ruin_n = d.loot('observatory_ruin', 'neutronium_ingot')
    vault_n = d.loot('anomaly_vault', 'neutronium_ingot')
    ruin_q = d.loot('observatory_ruin', 'cherenkov_quartz')
    smelt = d.rec('crafting/neutronium_ingot_smelting') or {}
    blast = d.rec('crafting/neutronium_ingot_blasting') or {}
    early = [r for r in ('quantum_foundations', 'casimir_effect', 'precision_lithography')]

    def taught(item):
        """'Holding X teaches the symbols a (..) and b (..) - Quantum Foundations needs them. '"""
        toks, users = [], []
        for rid in early:
            for t, nm, i in d.glyphs(rid):
                if i.split(':')[-1] == item:
                    if t not in [x for x, _ in toks]:
                        toks.append((t, nm[0].lower() + nm[1:]))
                    if d.R[rid]['title'] not in users:
                        users.append(d.R[rid]['title'])
        if not toks:
            return ''
        return (f'Holding {it(item)} once teaches the research symbol' + ('s ' if len(toks) > 1 else ' ')
                + ' and '.join(f'$(thing){t}$() ({nm})' for t, nm in toks) + ' - the derivation of ' + ' and '.join(f'$(thing){u}$()' for u in users)
                + (' needs them. ' if len(toks) > 1 else ' needs it. '))
    press_dm = d.machine('casimir_pressing/degenerate_matter_pressing')
    teg_f = d.fuels('thermoelectric')
    zpe_f = d.fuels('zero_point')
    # the research that comes next, straight after the first two
    nxt = []
    for rid, r in sorted(d.R.items(), key=lambda kv: (dep.get(kv[0], 99), kv[0])):
        req = {x.split(':')[-1] for x in r.get('requires', [])}
        if req and req <= {'quantum_foundations', 'casimir_effect'} and rid != 'casimir_effect' and not r.get('grant_only') and not r.get('hidden'):
            t, i, c = d.research(rid)
            nxt.append(f'$(li)$(thing){t}$() ({i} Insight' + (f', {c}' if c else '') + f'): {r.get("description", "")}')
    # the first multiblocks: the multiblock machines whose research lies least deep
    mbs = sorted(((dep.get(MR.get(m['id']), 99), m['id'], MR.get(m['id'])) for m in M if m.get('multiblock') and MR.get(m['id']) in d.R))
    first_mb = []
    for dd, mid, rid in mbs:
        if dd == mbs[0][0] and len(first_mb) < 3:
            first_mb.append(f'the {it(mid)} ($(thing){d.R[rid]["title"]}$())')
    first_mb_txt = ' or '.join(first_mb) if first_mb else 'the first big machines'

    steps = []        # (eid, title, icon, texts, research, spotlight)

    steps.append((START, 'Start Here', 'quantum_codex', [
        '$(l)Welcome.$() This Codex is your guide to Theoretical Sciences. The gold-marked nodes in this column are the $(thing)Getting Started$() steps: '
        'read them from the top down. Later steps open by themselves as you reach them (a padlock says what opens it), and every chapter of the map '
        'to the right opens with the step that explains it.',
        '$(l)Using the Codex$()$(li)$(thing)Drag$() the map (or arrow keys) to move it, $(thing)mouse wheel$() to zoom, $(thing)Fit$() (F) shows the whole map, '
        '$(thing)Centre$() (C) comes back here.$(li)$(thing)Hover$() a node: everything it links to lights up.$(li)$(thing)Click$() a node to read it; highlighted names in the text are links. '
        '$(li)$(thing)Search$() (bottom right, or Ctrl+F) looks through every page you have unlocked.$(li)The tabs along the top sort the same entries by branch: Physics, Materials, Machines...',
        '$(l)Two shortcuts$()$(li)$(thing)Sneak + use$() the Codex opens the $(thing)Research Tree$() (so does the $(thing)Research$() button at the bottom left).'
        '$(li)In any inventory, hover an item of this mod and press $(thing)K$() (changeable in Controls): the Codex opens at its page, with every way to make it.'
        '$(br2)The $(thing)Next steps$() button lists the research you can start right now and the guide steps you have not read yet.',
        f'Lost your Codex? It is crafted in an ordinary crafting table: {d.grid("crafting/quantum_codex")}.'],
        None, ['quantum_codex']))

    steps.append(('guide_01_quartz', 'Step 1: Cherenkov Quartz', 'cherenkov_quartz', [
        'Everything in this mod begins with $(item)Cherenkov Quartz$(). It grows as $(item)Deepslate Cherenkov Quartz Ore$() deep underground, '
        f'{d.ore_y("ore_cherenkov_quartz")} in the deepslate layer, in long thin veins; the ore glows faintly blue, so look along cave walls. '
        'Mine it with an $(thing)iron pickaxe$() or better.',
        f'Also found: {ruin_q} at a time in the supply caches of $(l:world/ruins)Observatory Ruins$(/l) - crashed research domes on the Overworld surface, '
        'which also hold Neutronium, a Spectral Analyzer or a spare Codex.$(br2)' + taught('cherenkov_quartz')
        + 'Collect a few dozen: the first machines use it by the nine.'],
        None, ['cherenkov_quartz', 'deepslate_cherenkov_quartz_ore']))

    steps.append(('guide_02_table', 'Step 2: The Extreme Assembly Table', 'extreme_assembly_table', [
        f'Your first block, made in an ordinary crafting table: {d.grid("crafting/extreme_assembly_table")}.$(br2)'
        'The $(item)Extreme Assembly Table$() is a $(thing)9x9$() crafting grid, and a normal crafting table as well.',
        '$(l)The 9x9 rule$(): almost every recipe of this mod is made here, filling the whole 9x9 grid - what would be one ingredient in a 3x3 recipe '
        'is a $(thing)3x3 block of nine$() of it. So "a furnace in the middle" means nine furnaces in the middle block. Only the Codex itself and this table are made '
        'in a normal 3x3 crafting grid (and decorative blocks in a stonecutter).$(br2)The recipe pages of every entry draw the full 9x9 pattern; JEI or EMI show it too.'],
        None, ['extreme_assembly_table']))

    steps.append(('guide_03_power', 'Step 3: First Power', 'thermoelectric_generator', [
        f'Machines run on $(thing)FE$(). The first generator needs nothing exotic: the $(item)Thermoelectric Generator$(), on the Extreme Assembly Table from '
        f'{d.grid("crafting/thermoelectric_generator")}. No research needed.',
        'It burns ordinary fuel:' + ''.join('$(li)' + f for f in teg_f) + '$(br2)It pushes its power into the blocks touching it, so set your first machine right beside it '
        '(or use any cable mod: every machine takes and gives FE on every side). Feed it with a hopper or pipe to keep it going.'],
        None, ['thermoelectric_generator', 'minecraft:coal']))

    steps.append(('guide_04_neutronium', 'Step 4: Neutronium', 'neutronium_ingot', [
        'Nearly every machine is built from $(item)Neutronium Ingot$() - and a lot of it (the first few machines take well over a hundred). Where it comes from:'
        f'$(li)$(thing)The End$(): $(item)Degenerate Matter Ore$() in End stone ({d.ore_y("ore_degenerate_matter")}), needs a $(thing)diamond pickaxe$(). '
        f'Smelt the $(item)Degenerate Matter Chunk$() it drops in a furnace ({smelt.get("cookingtime", 400) / 20:g} s; {blast.get("cookingtime", 200) / 20:g} s in a blast furnace) - one chunk, one ingot. This is the main source early on.'
        f'$(li)$(thing)Observatory Ruins$() on the Overworld surface: {ruin_n} ingots in a cache.'
        f'$(li)$(thing)Anomaly Vaults$(): half-sunk containment vaults, {vault_n} ingots per chest - but the anomaly engine inside keeps spawning creatures.',
        f'Later, once you have the Casimir Vacuum Press (step 9), you can make Degenerate Matter at home: {press_dm}.$(br2)'
        + taught('degenerate_matter_chunk')],
        None, ['neutronium_ingot', 'degenerate_matter_chunk', 'degenerate_matter_ore']))

    steps.append(('guide_05_dilithium', 'Step 5: Dilithium from the Nether', 'dilithium_crystal', [
        f'$(item)Dilithium Ore$() grows in the Nether\'s netherrack, {d.ore_y("ore_dilithium")}; an $(thing)iron pickaxe$() mines it into $(item)Dilithium Crystal$().',
        'You need it early:$(li)' + taught('dilithium_crystal') + '$(li)It goes into $(item)Neutronium Plates$() (step 9), which most later parts are made of.$(br2)'
        'While you are in the Nether: blazes and ghasts are worth observing with the Spectral Analyzer (next step), and the Nether sky is worth Insight too.'],
        None, ['dilithium_crystal', 'dilithium_ore']))

    steps.append(('guide_06_analyzer', 'Step 6: The Spectral Analyzer & Insight', 'spectral_analyzer', [
        f'Research is paid for in $(thing)Insight$(), and Insight comes from observing things with the $(item)Spectral Analyzer$(): on the Extreme Assembly Table from '
        f'{d.grid("crafting/spectral_analyzer")} (or found in Observatory Ruins).',
        'Every $(thing)new$() observation counts once:$(li)$(thing)Sneak + use$() it in the air: the sky of the dimension you are in - 8 Insight each for the Overworld, the Nether, the End and every other world.'
        '$(li)Use it on unusual blocks - amethyst clusters, budding amethyst, obsidian, crying obsidian, magma, glowstone, spawners, enchanting tables, beacons, sculk... - 3 each.'
        '$(li)On creatures - endermen, blazes, ghasts, phantoms, shulkers, wardens... - 6 each.$(li)On a $(thing)running$() machine of this mod: 4, or 12 for a multiblock.',
        f'The first research, $(thing){qf_t}$(), costs only {qf_i} Insight: observing the Overworld sky is enough. Later, machines running $(thing)thought experiments$() make Insight without end.'],
        None, ['spectral_analyzer']))

    steps.append(('guide_07_research', 'Step 7: The Research Tree', 'research_terminal', [
        '$(thing)Sneak + use the Codex$() to open the Research Tree (or press the Research button here, or use a Research Terminal block - you do not need one).'
        '$(br2)Machines refuse recipes you have not researched, and many entries of this Codex wait for their research too.',
        'Each research shows what it builds on, its $(thing)Insight$() price and sometimes items it uses up. Most end in a $(thing)derivation$(): '
        'the situation is described, and you lay out the real equation from symbol tiles, then use it to compute a number from the given values (each world has its own numbers). '
        'Hints can be bought with Insight. The $(thing)Book of Symbols$() in the tree explains every tile and how to learn it.',
        f'$(l)Your first: {qf_t}$() - {qf_i} Insight' + (f', {qf_c}' if qf_c else ', no items') + '. The derivation is the time-dependent Schrödinger equation. '
        + _glyph_text(d, 'quantum_foundations') + ' With the tiles learned, build the equation, then compute the electron\'s de Broglie wavelength from the given numbers.'],
        None, ['research_terminal', 'qubit']))

    steps.append(('guide_08_casimir', 'Step 8: The Casimir Effect', 'casimir_plate', [
        f'With {qf_t} done, research $(thing){ce_t}$() next: {ce_i} Insight' + (f' and {ce_c}' if ce_c else '') + '. ' + _glyph_text(d, 'casimir_effect')
        + ' It unlocks the recipes of the Casimir Vacuum Press and the Zero-Point Energy Extractor.',
        f'Meanwhile, make $(item)Spacetime Casings$() - the frame of most machines - on the Extreme Assembly Table: {d.grid("crafting/spacetime_casing")}. '
        'The rings of Observatory Ruins are made of them too: a pickaxe takes them.'],
        'quantum_foundations', ['spacetime_casing', 'casimir_plate']))

    steps.append(('guide_09_press', 'Step 9: The Casimir Vacuum Press', 'casimir_press', [
        f'Your first real machine. On the Extreme Assembly Table: {d.grid("crafting/casimir_press")}. Place it next to the Thermoelectric Generator.',
        'What it makes first:$(li)' + d.machine('casimir_pressing/neutronium_plate') + '$(li)' + press_dm + ' - renewable Neutronium.$(li)'
        + d.machine('casimir_pressing/casimir_plate') + '$(li)' + d.machine('casimir_pressing/quantum_dot')
        + '$(br2)Put the ingredients in its slots (or pipe them in); its screen shows progress and what is missing. Its own entry lists everything else it makes.'],
        'casimir_effect', ['casimir_press', 'neutronium_plate']))

    steps.append(('guide_10_zpe', 'Step 10: Zero-Point Energy', 'zero_point_extractor', [
        f'More power: the $(item)Zero-Point Energy Extractor$(), on the Extreme Assembly Table from {d.grid("crafting/zero_point_extractor")}.',
        'It draws power from the vacuum itself, using up a catalyst:' + ''.join('$(li)' + f for f in zpe_f[:4])
        + '$(br2)Like every generator it pushes power into the blocks it touches. Keep the Thermoelectric Generator for the small jobs.'],
        'casimir_effect', ['zero_point_extractor']))

    steps.append(('guide_11_paths', 'Step 11: Choosing a Path', 'qubit', [
        'From here the research tree branches. The next research open to you:' + ''.join(nxt[:6]),
        '$(thing)Precision Lithography$() matters early: its Lattice Growth Vat grows Cherenkov Quartz from ordinary quartz and amethyst, so you stop depending on ore. '
        '$(thing)Quantum Entanglement$() gives the Quantum Entangler, whose $(thing)thought experiment$() turns materials into Insight - the renewable way to pay for research.$(br2)'
        'Every machine\'s entry on the map says what it makes; machines need their own research before they run.'],
        'casimir_effect', ['qubit', 'lattice_growth_vat', 'quantum_entangler']))

    steps.append(('guide_12_multiblocks', 'Step 12: Multiblocks', 'structure_assembler', [
        f'The big machines are $(thing)multiblocks$(): a controller block plus casings and glass around it. Your first will be {first_mb_txt}.',
        'How to build one:$(li)Open the machine\'s entry: its $(thing)Structure$() page shows the build layer by layer, with the blocks needed.'
        '$(li)Place the controller, then use a $(item)Structure Assembler$() on it - it places every missing block from your inventory. '
        '$(thing)Sneak + use$() first for a dry run and a shopping list.$(li)Swap $(item)Interface Ports$() into the marked spots: items, fluids and power go in and out through them.'
        '$(li)Running multiblocks drift and wear: their tuning panel and stabilisers keep them safe - a containment failure is dangerous.',
        f'Structure Assembler: {d.grid("crafting/structure_assembler")}.$(br2)Interface Port: {d.grid("crafting/interface_port")}.'],
        'casimir_effect', ['structure_assembler', 'interface_port']))

    steps.append(('guide_13_onward', 'Step 13: The Road Ahead', 'quantum_codex', [
        'That is the beginning. The rest of the $(thing)Discovery$() map runs left to right in the order things are met, each chapter linked to what it builds on; '
        'locked chapters show as $(thing)???$() until their research is done. The other tabs sort the same pages by branch (Physics, Chemistry, Biology, '
        'Mathematics), by world (Dimensions) and by kind (Materials, Creatures, Machines).',
        'Habits that help:$(li)Press $(thing)K$() over any item in an inventory to see how it is made.$(li)Check $(thing)Next steps$() whenever you have new Insight.'
        '$(li)Every tier has a thought experiment for Insight: build each tier\'s thinking machine.$(li)Storage blocks are woven, not packed: they cannot be taken apart again.'],
        'casimir_effect', ['quantum_codex']))

    chain = []
    for n, (eid, title, icon, texts, rid, spot) in enumerate(steps):
        te('basics', eid, title, icon, texts, adv=rid, sort=-100 + n, spot=spot)
        chain.append(('basics', eid))
    # the chapters that used to open from the start (and the base materials): which step they open with, and the
    # research they should wait for instead of their old gate (None: no research - they open with the step)
    links = {
        ('fabrication', 'node_quantum_codex'): (START, None),
        ('basics', 'symbols'): ('guide_07_research', None),
        ('basics', 'research'): ('guide_07_research', None),
        ('basics', 'world_numbers'): ('guide_07_research', None),
        ('world', 'ores'): ('guide_01_quartz', None),
        ('fabrication', 'node_cherenkov_quartz'): ('guide_01_quartz', None),
        ('basics', 'extreme_table_first'): ('guide_02_table', None),
        ('basics', 'thermoelectric_generator'): ('guide_03_power', None),
        ('basics', 'power'): ('guide_03_power', None),
        ('world', 'ruins'): ('guide_04_neutronium', None),
        ('singularities', 'node_neutronium_ingot'): ('guide_04_neutronium', None),
        ('basics', 'node_degenerate_matter_chunk'): ('guide_04_neutronium', None),
        ('world', 'node_degenerate_matter_ore'): ('guide_04_neutronium', None),
        ('world', 'dilithium'): ('guide_05_dilithium', None),
        ('fabrication', 'node_dilithium_crystal'): ('guide_05_dilithium', None),
        ('basics', 'insight'): ('guide_06_analyzer', None),
        ('quantum', 'casimir_press'): ('guide_09_press', 'casimir_effect'),
        ('singularities', 'node_neutronium_plate'): ('guide_09_press', 'casimir_effect'),
        ('basics', 'zero_point_extractor'): ('guide_10_zpe', 'casimir_effect'),
        ('basics', 'thought_experiments'): ('guide_11_paths', None),
        ('basics', 'assembler'): ('guide_12_multiblocks', None),
        ('basics', 'recipe_cost'): ('guide_12_multiblocks', None),
        ('world', 'decor'): ('guide_13_onward', None),
    }
    g['GUIDE'] = {'chain': chain, 'links': {k: (('basics', s), r) for k, (s, r) in links.items()}}
    print('round33 guide:', len(chain), 'steps;', len(links), 'chapters hang off them')
