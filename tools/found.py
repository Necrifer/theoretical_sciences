"""
1.1.0: where each of the mod's materials is found in the world, written where the player looks.

Worked out from the data the game itself uses:
  * a block's loot table says what mining it gives;
  * world-generation features say which blocks they place, biomes which features they carry, dimensions which biomes
    they hold (and their terrain settings which blocks they are built of); NeoForge biome modifiers add the
    Overworld/Nether/End ores;
  * creature loot tables say what a creature drops, biome spawn lists where it lives;
  * chest loot tables say what a structure's caches hold.

For each material found this way it writes:
  * a lang line  item|block.theoreticalsciences.<id>.found  ("Found: ...") that the tooltip shows under the description;
  * a "Found" line on the material's Codex page.
Run after the Codex is built (tools/regenerate.sh), before the text overrides.
"""
import glob
import json
import os
import re
from collections import defaultdict

HERE = os.path.dirname(os.path.abspath(__file__))
RES = os.path.join(HERE, '..', 'src', 'main', 'resources')
D = os.path.join(RES, 'data', 'theoreticalsciences')
A = os.path.join(RES, 'assets', 'theoreticalsciences')
NS = 'theoreticalsciences'
LANG = os.path.join(A, 'lang', 'en_us.json')
CODEX = os.path.join(A, 'ts_codex', 'entries.json')
ID = re.compile(r'theoreticalsciences:[a-z0-9_/]+')

NETHER = {'minecraft:nether_wastes', 'minecraft:crimson_forest', 'minecraft:warped_forest', 'minecraft:soul_sand_valley', 'minecraft:basalt_deltas'}
VANILLA_TAGS = {'#minecraft:is_overworld': 'the Overworld', '#minecraft:is_nether': 'the Nether', '#minecraft:is_end': 'the End',
                '#c:is_overworld': 'the Overworld', '#c:is_nether': 'the Nether', '#c:is_end': 'the End'}


def load(p):
    return json.load(open(p))


def human(s):
    return ' '.join(w.capitalize() for w in s.split('_'))


def main():
    L = load(LANG)

    def name(kind, i):
        return L.get(f'{kind}.{NS}.{i}') or L.get(f'block.{NS}.{i}') or L.get(f'item.{NS}.{i}') or human(i)

    # ---------------------------------------------------------------- biomes -> dimensions
    biome_dims = defaultdict(set)
    dim_names = {}
    for f in glob.glob(os.path.join(D, 'dimension', '*.json')):
        dim = os.path.basename(f)[:-5]
        dim_names[dim] = L.get(f'dimname.{NS}.{dim}') or 'the ' + human(dim)
        d = load(f)
        txt = json.dumps(d.get('generator', {}).get('biome_source', {}))
        for b in ID.findall(txt):
            biome_dims[b.split(':')[1]].add(dim)

    def biome_tag(tag):
        """#ns:path -> biome ids (only the mod's own)."""
        ns, path = tag[1:].split(':')
        f = os.path.join(RES, 'data', ns, 'tags', 'worldgen', 'biome', path + '.json')
        out = []
        if os.path.exists(f):
            for v in load(f).get('values', []):
                v = v if isinstance(v, str) else v.get('id')
                if v in VANILLA_TAGS: out.append(v)
                elif v.startswith('#'): out += biome_tag(v)
                elif v.startswith(NS + ':'): out.append(v.split(':')[1])
                elif v.startswith('minecraft:'): out.append('#minecraft:is_nether' if v in NETHER else '#minecraft:is_end' if 'end' in v else '#minecraft:is_overworld')
        return out

    def places_of_biomes(biomes):
        """{dimension name: [biome names]}"""
        out = defaultdict(list)
        for b in biomes:
            if b in VANILLA_TAGS:
                out[VANILLA_TAGS[b]]
                continue
            for dim in sorted(biome_dims.get(b, ())):
                bn = L.get(f'biome.{NS}.{b}', human(b))
                if bn not in out[dim_names[dim]]: out[dim_names[dim]].append(bn)
        return out

    # ---------------------------------------------------------------- features -> blocks
    conf_blocks = {}
    for f in glob.glob(os.path.join(D, 'worldgen', 'configured_feature', '*.json')):
        conf_blocks[os.path.basename(f)[:-5]] = {x.split(':')[1] for x in ID.findall(open(f).read())}
    placed_blocks = {}
    yrange = {}                     # ore block -> [lowest, highest] Y its features place it at
    for f in glob.glob(os.path.join(D, 'worldgen', 'placed_feature', '*.json')):
        d = load(f)
        feat = d.get('feature')
        bl = set()
        if isinstance(feat, str) and feat.startswith(NS + ':'): bl |= conf_blocks.get(feat.split(':')[1], set())
        elif isinstance(feat, dict): bl |= {x.split(':')[1] for x in ID.findall(json.dumps(feat))}
        placed_blocks[os.path.basename(f)[:-5]] = bl
        for pl in d.get('placement', []):
            h = pl.get('height', {}) if pl.get('type') == 'minecraft:height_range' else {}
            lo, hi = h.get('min_inclusive', {}).get('absolute'), h.get('max_inclusive', {}).get('absolute')
            if lo is not None and hi is not None:
                for b in bl:
                    r = yrange.setdefault(b, [lo, hi])
                    r[0], r[1] = min(r[0], lo), max(r[1], hi)

    block_places = defaultdict(lambda: defaultdict(list))     # block -> {place: [biomes]}

    def add(block, places):
        for p, bs in places.items():
            for b in bs:
                if b not in block_places[block][p]: block_places[block][p].append(b)
            block_places[block].setdefault(p, [])

    for f in glob.glob(os.path.join(D, 'worldgen', 'biome', '*.json')):
        biome = os.path.basename(f)[:-5]
        d = load(f)
        pl = places_of_biomes([biome])
        for step in d.get('features', []):
            for pf in step:
                if pf.startswith(NS + ':'):
                    for b in placed_blocks.get(pf.split(':')[1], ()):
                        add(b, pl)
    for f in glob.glob(os.path.join(D, 'neoforge', 'biome_modifier', '*.json')):
        d = load(f)
        if d.get('type') != 'neoforge:add_features': continue
        biomes = d.get('biomes')
        biomes = biomes if isinstance(biomes, list) else [biomes]
        pl = defaultdict(list)
        for b in biomes:
            if b in VANILLA_TAGS: pl[VANILLA_TAGS[b]]
            elif b.startswith('#'):
                for k, v in places_of_biomes(biome_tag(b)).items(): pl[k] += v
        feats = d.get('features')
        feats = feats if isinstance(feats, list) else [feats]
        for pf in feats:
            for b in placed_blocks.get(pf.split(':')[1], ()):
                add(b, pl)
    # terrain: the blocks a dimension is built of
    for f in glob.glob(os.path.join(D, 'worldgen', 'noise_settings', '*.json')):
        dim = os.path.basename(f)[:-5]
        if dim not in dim_names: continue
        for b in {x.split(':')[1] for x in ID.findall(open(f).read())}:
            block_places[b].setdefault(dim_names[dim], [])

    # ---------------------------------------------------------------- creatures
    mob_places = defaultdict(lambda: defaultdict(list))
    for f in glob.glob(os.path.join(D, 'worldgen', 'biome', '*.json')):
        biome = os.path.basename(f)[:-5]
        for lst in load(f).get('spawners', {}).values():
            for s in lst:
                if s.get('type', '').startswith(NS + ':'):
                    for p, bs in places_of_biomes([biome]).items():
                        for b in bs:
                            if b not in mob_places[s['type'].split(':')[1]][p]: mob_places[s['type'].split(':')[1]][p].append(b)
    for f in glob.glob(os.path.join(D, 'neoforge', 'biome_modifier', '*.json')):
        d = load(f)
        if d.get('type') != 'neoforge:add_spawns': continue
        biomes = d.get('biomes')
        biomes = biomes if isinstance(biomes, list) else [biomes]
        sp = d.get('spawners')
        sp = sp if isinstance(sp, list) else [sp]
        for s in sp:
            e = s.get('type', '').split(':')[-1]
            for b in biomes:
                if b in VANILLA_TAGS: mob_places[e][VANILLA_TAGS[b]]
                elif b.startswith('#'):
                    for p, bs in places_of_biomes(biome_tag(b)).items(): mob_places[e][p] += bs

    dim_biomes = defaultdict(set)
    for b, ds in biome_dims.items():
        for dd in ds: dim_biomes[dim_names[dd]].add(b)

    def where(places):
        parts = []
        for p, bs in places.items():
            bs = [b for b in bs if b.lower().replace('the ', '') not in p.lower()]
            if len(dim_biomes.get(p, ())) <= 1: bs = []
            parts.append(p + (' (' + ', '.join(bs[:3]) + (' ...' if len(bs) > 3 else '') + ')' if bs else ''))
        return ', '.join(parts)

    # ---------------------------------------------------------------- structures
    structs = {}
    for f in glob.glob(os.path.join(D, 'worldgen', 'structure', '*.json')):
        sid = os.path.basename(f)[:-5]
        bt = load(f).get('biomes', '')
        pl = places_of_biomes(biome_tag(bt)) if isinstance(bt, str) and bt.startswith('#') else {}
        structs[sid] = pl

    # blocks built into structures (crater rocks, ...), for blocks no recipe makes: from the structures' own templates
    import gzip
    made_blocks = set()
    for f in glob.glob(os.path.join(D, 'recipe', '**', '*.json'), recursive=True):
        d = load(f)
        for o in d.get('item_outputs', []) + ([d['result']] if isinstance(d.get('result'), dict) else []):
            o = o.get('id') or o.get('item') or ''
            if o.startswith(NS + ':'): made_blocks.add(o.split(':')[1])
    for sid, pl in structs.items():
        locs = set()
        for f in glob.glob(os.path.join(D, 'worldgen', 'template_pool', sid, '*.json')) + glob.glob(os.path.join(D, 'worldgen', 'template_pool', sid + '.json')):
            locs |= {x.split(':')[1] for x in re.findall(r'"location":\s*"(theoreticalsciences:[a-z0-9_/]+)"', open(f).read())}
        label = human(sid) + ' structures' + (' in ' + where(pl) if pl else '')
        for loc in locs:
            nf = os.path.join(D, 'structure', loc + '.nbt')
            if not os.path.exists(nf): continue
            raw = gzip.open(nf).read().decode('latin-1')
            for b in {x.split(':')[1] for x in ID.findall(raw)}:
                if '/' in b or b in made_blocks: continue
                block_places[b].setdefault(label, [])

    def struct_for(chest):
        for sid in sorted(structs, key=len, reverse=True):
            if chest.startswith(sid) or sid.startswith(chest) or sid.rstrip('s') == chest.rstrip('s'):
                return sid
        return None

    # ---------------------------------------------------------------- what gives each item
    found = defaultdict(list)          # item -> [phrases]

    def items_of(table):
        out = set()
        for p in table.get('pools', []):
            for e in p.get('entries', []):
                for x in [e] + e.get('children', []):
                    n = x.get('name', '')
                    if n.startswith(NS + ':'): out.add(n.split(':')[1])
        return out

    mined = defaultdict(dict)
    for f in sorted(glob.glob(os.path.join(D, 'loot_table', 'blocks', '*.json'))):
        block = os.path.basename(f)[:-5]
        if block not in block_places: continue
        pl = where(block_places[block])
        for it in items_of(load(f)):
            if it == block: found[it].append('in ' + pl)
            else: mined[it].setdefault(pl, []).append(name('block', block))
    for it, by_place in mined.items():               # one phrase per place: "mined from A or B in X"
        for pl, names in by_place.items():
            found[it].append('mined from ' + ' or '.join(names) + ' in ' + pl)
    for block, pl in block_places.items():         # a placed block with no loot table of its own still is itself
        if not os.path.exists(os.path.join(D, 'loot_table', 'blocks', block + '.json')) and not any(p.startswith('in ') for p in found[block]):
            found[block].append('in ' + where(pl))
    for f in glob.glob(os.path.join(D, 'loot_table', 'entities', '*.json')):
        mob = os.path.basename(f)[:-5]
        mp = mob_places.get(mob)
        who = L.get(f'entity.{NS}.{mob}', human(mob))
        for it in items_of(load(f)):
            found[it].append(f'dropped by the {who}' + (f' ({where(mp)})' if mp else ''))
    made = set()
    for f in glob.glob(os.path.join(D, 'recipe', '**', '*.json'), recursive=True):
        d = load(f)
        for o in d.get('item_outputs', []) + ([d['result']] if isinstance(d.get('result'), dict) else []):
            o = o.get('id') or o.get('item') or ''
            if o.startswith(NS + ':'): made.add(o.split(':')[1])
    SPECIAL = {'probability_hall': 'Probability Halls (in villages)', 'anomaly_vault': 'Anomaly Vaults'}
    for f in glob.glob(os.path.join(D, 'loot_table', 'chests', '*.json')):
        chest = os.path.basename(f)[:-5]
        sid = struct_for(chest)
        nm = human(sid or chest)
        for it in items_of(load(f)):
            if it in made: continue                      # (made by a recipe: the caches are only a head start)
            ph = (f'in the caches of {SPECIAL[chest]}' if chest in SPECIAL else
                  f'in the caches of {nm}' + (f' structures in {where(structs[sid])}' if sid and structs.get(sid) else 's'))
            if ph not in found[it]: found[it].append(ph)

    # ---------------------------------------------------------------- ores: one description, by the template
    # (1.1.0 text rewrite) "Found in the Accretion Frontier, between Y 0 and 255. Gives Accretion Disk Fragment when mined
    # with iron pickaxe or better." - the dimension, the height, what it gives and the pickaxe it needs, from the data
    C = load(os.path.join(HERE, 'content.json'))
    E = load(CODEX)
    ores = 0
    ore_ids = set()
    for b in C['blocks']:
        if b.get('kind') != 'ore' or b['id'] not in block_places: continue
        oid = b['id']
        dims = []
        for p in block_places[oid]:
            dn = p if not p.endswith(' structures') and ' structures in ' not in p else None
            if dn and dn not in dims: dims.append(dn)
        if not dims: continue
        where_txt = ' and '.join(dims)
        y = yrange.get(oid)
        drop = (b.get('drops') or oid).split(':')[-1]
        tool = {'wood': 'wooden', 'stone': 'stone', 'iron': 'iron', 'diamond': 'diamond', 'netherite': 'netherite'}.get(b.get('tool'), b.get('tool') or 'stone')
        text = f'Found in {where_txt}' + (f', between Y {y[0]} and {y[1]}' if y else '') + '. '
        text += (f'Gives {name("item", drop)} when mined with {tool} pickaxe or better.' if drop != oid
                 else f'Mined with {tool} pickaxe or better.')
        key = f'block.{NS}.{oid}.lore'
        old = L.get(key)
        L[key] = text
        ore_ids.add(oid)
        for e in E.values():                          # an ore page that only said the ore's name
            for pg in e['pages']:
                if pg.get('type') == 'spotlight' and f'{NS}:{oid}' in (pg.get('items') or []):
                    m = re.match(r'\s*(\$\((?:item|thing)\))?([^$.]+)(\$\(\))?\.\s*(?=\$\(br2\)|$)', pg.get('text', ''))
                    if m and m.group(2).strip() == L.get(f'block.{NS}.{oid}', '').strip():
                        pg['text'] = text + pg['text'][m.end():]
        if old and old != text:                       # the ore's Codex page showed the old description
            for e in E.values():
                for pg in e['pages']:
                    if old in pg.get('text', ''): pg['text'] = pg['text'].replace(old, text)
        ores += 1
    json.dump(E, open(CODEX, 'w'), indent=2)

    # ---------------------------------------------------------------- write: lang, and the Codex
    n = 0
    lines = {}
    for it, phrases in found.items():
        if it in ore_ids: continue                   # an ore's own description already says where
        uniq = []
        for p in phrases:
            if p not in uniq: uniq.append(p)
        kind = 'item' if f'item.{NS}.{it}' in L else 'block' if f'block.{NS}.{it}' in L else None
        if not kind: continue
        text = 'Found: ' + '; '.join(uniq[:4]) + ('; and elsewhere.' if len(uniq) > 4 else '.')
        L[f'{kind}.{NS}.{it}.found'] = text
        lines[it] = text
        n += 1
    json.dump(L, open(LANG, 'w'), indent=2)

    E = load(CODEX)
    pages = 0
    for eid, e in E.items():
        for i, p in enumerate(e['pages']):
            if p.get('type') != 'spotlight' or not p.get('items'): continue
            it = p['items'][0].split(':')[-1]
            if it not in lines or 'Found$()' in p.get('text', ''): continue
            line = '$(thing)Found$():' + lines[it][len('Found:'):]
            body = p.get('text', '').rstrip()
            while body.endswith('$(br2)'): body = body[:-6].rstrip()
            p['text'] = (body + '$(br2)' + line) if body else line
            pages += 1
            break
    json.dump(E, open(CODEX, 'w'), indent=2)
    print(f'found: {n} materials located in the world, {pages} Codex pages told, {ores} ore descriptions')


if __name__ == '__main__':
    main()
