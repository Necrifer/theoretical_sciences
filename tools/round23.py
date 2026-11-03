"""
Round 23.

* The Codex (Modonomicon) rebuilt as one map of discovery plus branch tabs. The main tab ("Discovery") lays every
  chapter out in the order it is met - left to right by how deep in the research tree it lies, linked to what it
  builds on. Then a tab for each branch - Physics, Chemistry, Biology, Mathematics - and for Dimensions, Materials,
  Creatures and Machines. A node can sit in several tabs at once (the Quantum Foam is on the main map, under
  Dimensions and under Physics): the copies share their text.
* The Codex is now built last, after every round has added its chapters (before, chapters added after round 5's
  finishing step - e.g. the Censer, Simulated Universes, the Chronal Accelerator - never reached it).
"""
import glob
import json
import math
import os
import re
import shutil

NS = 'theoreticalsciences'

LANG = {
    'status.theoreticalsciences.needs_omega': 'Needs the Ω overclock',
    'gui.theoreticalsciences.omega_needs_omnilex': 'The aleph-null overclock: carry the Omnilex to set it.',
    'gui.theoreticalsciences.zoom_tip': 'Zoom (mouse wheel, + / -). Ctrl + wheel scrolls, Shift + wheel scrolls sideways, drag to pan.',
}

PHYS = {'quantum', 'spacetime', 'antimatter', 'exotic', 'strings', 'stellar', 'cosmic', 'singularities', 'hyperdimensions', 'nanotech',
        'astrophysics', 'geography', 'nuclear', 'electromagnetism', 'optics'}      # round 37 (1.1): the new sciences
CHEM = {'chemistry', 'optics'}       # 1.5.0: Optics & Crystals is both
BIO = {'biology', 'minds'}
MATH = {'mathematics', 'information', 'simulation', 'probability'}
# the tabs: id, name, description, icon
TABS = [
    ('discovery', 'Discovery', 'Every concept in the order you meet it, linked to what it builds on.', 'quantum_codex'),
    ('physics', 'Physics', 'Quanta, spacetime, particles, strings, stars and singularities.', 'entangled_photon_pair'),
    ('chemistry', 'Chemistry', 'Bonds, exotic matter and the island of stability.', 'chiral_crystal'),
    ('biology', 'Biology', 'Life - made, evolved, engineered, and thinking.', 'synthetic_cell'),
    ('mathematics', 'Mathematics', 'Numbers, logic, sets, infinities and information.', 'riemann_zeta_crystal'),
    ('dimensions', 'Dimensions', 'Every world beyond the Overworld, its laws and how to survive it.', 'brane_shifter'),
    ('materials', 'Materials', 'Every material: what it is, how it is made, what it is for.', 'neutronium_ingot'),
    ('mobs', 'Creatures', 'What lives (and what breaks loose) out there.', 'void_essence'),
    ('machines', 'Machines', 'Every machine and multiblock, and how to run them.', 'structure_assembler'),
]
DIM_CHAPTERS = {('world', 'bulk'), ('mathematics', 'platonic_realm'), ('quantum', 'quantum_foam'), ('mathematics', 'transfinite'),
                ('spacetime', 'black_hole_interior'), ('endgame', 'hyperreal'), ('endgame', 'beyond'), ('endgame', 'alpha_omega'),
                ('quantum', 'foam_boundaries'), ('mathematics', 'fractal_space'), ('world', 'attunement_hierarchy'), ('world', 'dimension_laws'),
                ('world', 'falling_through'), ('world', 'impossible_interiors'), ('world', 'bulk_life'), ('antimatter', 'antiverse_life'),
                ('world', 'ruins'), ('world', 'containment_caches'), ('geography', 'r40_interior'), ('geography', 'r41_riches'),
                ('world', 'interior_laws')}
MOB_CHAPTERS = {('world', 'bulk_life'), ('antimatter', 'antiverse_life'), ('biology', 'anomaly_spawner'), ('endgame', 'alpha_omega'),
                ('basics', 'containment_failure'), ('world', 'ruins')}


# round 36: the stages of the game the Codex maps are grouped by (research column ranges, numeral, name)
TIERS = [(0, 3, 'I', 'Foundations'), (4, 7, 'II', 'Quanta & Matter'), (8, 11, 'III', 'Stars & Spacetime'),
         (12, 15, 'IV', 'The Cosmos'), (16, 20, 'V', 'Beyond Physics'), (21, 99, 'VI', 'Transcendence')]


def _id(x):
    return x.split(':')[-1]


def finish(g):
    import round5
    import round30_codex
    round30_codex.refresh(g)          # round 30: bring the chapters up to date and give every thing a node, first
    w, q, D, A, LANG_ = g['w'], g['q'], g['D'], g['A'], g['LANG']
    LANG_.update(LANG)
    BA = g['BA']
    here = os.path.dirname(os.path.abspath(__file__))
    C = json.load(open(os.path.join(here, 'content.json')))
    M = json.load(open(os.path.join(here, 'machines.json')))
    machines = {m['id'] for m in M} | {b['id'] for b in C['blocks'] if b.get('kind') in ('energy_cell', 'field_projector', 'transceiver', 'conv_pedestal', 'conv_altar', 'chrono_accel', 'anomaly', 'exnihilo', 'infinity_cache')}
    creatures = set(g.get('CREATURES', {}).keys())
    root = os.path.join(D, 'modonomicon', 'books', 'quantum_codex')
    if os.path.isdir(root):
        shutil.rmtree(root)
    pre = 'book.theoreticalsciences.codex'
    LANG_[f'{pre}.name'] = 'Quantum Codex'
    LANG_[f'{pre}.tooltip'] = 'A researcher\'s notebook from the edge of physics'
    w(os.path.join(root, 'book.json'), {
        'name': f'{pre}.name', 'tooltip': f'{pre}.tooltip', 'custom_book_item': q('quantum_codex'), 'generate_book_item': False,
        'auto_add_read_conditions': False, 'allow_open_book_with_invalid_links': True, 'default_title_color': 0x10306A, 'font': 'minecraft:default'})

    # ---- every Patchouli chapter and node
    cats = {}
    for f in sorted(glob.glob(os.path.join(BA, 'categories', '*.json'))):
        cats[os.path.basename(f)[:-5]] = json.load(open(f))
    entries = {}
    for f in sorted(glob.glob(os.path.join(BA, 'entries', '*', '*.json'))):
        cid, eid = f.split(os.sep)[-2], os.path.basename(f)[:-5]
        entries[(cid, eid)] = json.load(open(f))
    NODES = round5.NODES

    # ---- research depth: how far into the tree each research lies (the order of discovery)
    req = {}
    for f in glob.glob(os.path.join(D, 'ts_research', '*.json')):
        r = json.load(open(f))
        req[os.path.basename(f)[:-5]] = [_id(x) for x in r.get('requires', [])]
    depth = {}

    def dep(r, stack=()):
        if r in depth: return depth[r]
        if r in stack or r not in req: return 0
        depth[r] = 0
        d = 1 + max([dep(x, stack + (r,)) for x in req[r]] + [-1])
        depth[r] = d
        return d
    for r in req: dep(r)

    def rid(e):
        a = e.get('advancement', '')
        return a.split('research/')[1] if 'research/' in a else None

    def order(k):
        cid, eid = k
        e = entries[k]
        r = rid(e)
        return (depth.get(r, -1) if r else -1, cats.get(cid, {}).get('sortnum', 0), e.get('sortnum', 0), eid)

    # ---- page text (shared by every copy of a node)
    def pages_of(k):
        cid, eid = k
        e = entries[k]
        key = f'{pre}.{cid}.{eid}'
        LANG_[key] = e['name']
        pages = []
        for n, pg in enumerate(e['pages']):
            t = pg.get('type')
            pk = f'{key}.p{n}'
            if pg.get('text'):
                LANG_[pk] = round5.md(pg['text'])
            if t == 'patchouli:spotlight':
                pages.append({'type': 'modonomicon:spotlight', 'item': {'item': pg['item'].split(',')[0]}, 'text': pk if pg.get('text') else ''})
            elif t == 'patchouli:text':
                p = {'type': 'modonomicon:text', 'text': pk}
                if not pages: p['title'] = key
                pages.append(p)
            elif t == 'patchouli:crafting':
                p = {'type': 'modonomicon:crafting_recipe', 'recipe_id_1': pg['recipe']}
                if pg.get('recipe2'): p['recipe_id_2'] = pg['recipe2']
                if pg.get('text'): p['text'] = pk
                pages.append(p)
            elif t == 'patchouli:multiblock':
                LANG_[pk + '.mb'] = round5.md(('$(thing)%s$(): ' % pg.get('name', 'Structure')) + (pg.get('text') or '') +
                                             '$(br2)Explore it layer by layer in JEI (Multiblocks), or place it with the $(item)Structure Assembler$().')
                pages.append({'type': 'modonomicon:text', 'text': pk + '.mb'})
        first = next((pg.get('text') for pg in e['pages'] if pg.get('text')), '')
        LANG_[key + '.desc'] = re.sub(r'\$\([^)]*\)', '', first).split('. ')[0][:90]
        return key, pages

    PAGES = {k: pages_of(k) for k in entries}

    # ---- which tab each chapter / node belongs to
    chapters = [k for k in entries if k not in NODES]
    kids = {}
    for k in entries:
        if k in NODES:
            kids.setdefault((k[0], NODES[k]), []).append(k)

    def node_kind(k):
        nid = k[1][5:] if k[1].startswith('node_') else k[1]
        if nid in creatures: return 'mobs'
        if nid in machines: return 'machines'
        return 'materials'

    GUIDE = g.get('GUIDE') or {'chain': [], 'links': {}}
    guide = [k for k in GUIDE['chain'] if k in entries]          # round 33: the Getting Started chain
    guide_set = set(guide)

    def chapter_tabs(k):
        cid, eid = k
        e = entries[k]
        if k in guide_set: return {'discovery'}                   # (only on the main map)
        out = set()
        if cid != 'operation': out.add('discovery')
        if cid in PHYS: out.add('physics')
        if cid in CHEM: out.add('chemistry')
        if cid in BIO: out.add('biology')
        if cid in MATH: out.add('mathematics')
        if cid == 'world' or k in DIM_CHAPTERS: out.add('dimensions')
        if k in MOB_CHAPTERS: out.add('mobs')
        icon = _id(e.get('icon', ''))
        if cid in ('operation', 'machines', 'fabrication') or eid in machines or icon in machines: out.add('machines')
        return out

    tab_members = {t[0]: {} for t in TABS}     # tab -> {chapter: [nodes]}
    for k in chapters:
        ts = chapter_tabs(k)
        ks = kids.get(k, [])
        for t in ts:
            if t == 'discovery':
                tab_members[t][k] = []                                      # the main map: chapters only
            elif t in ('materials', 'mobs', 'machines'):
                tab_members[t][k] = [n for n in ks if node_kind(n) == t]
            else:
                tab_members[t][k] = list(ks)                                # a branch: the chapter with all its nodes
        # nodes of a kind whose chapter is not in that tab: the chapter comes along as their centre
        for kind in ('materials', 'mobs', 'machines'):
            mine = [n for n in ks if node_kind(n) == kind]
            if mine and kind not in ts:
                tab_members[kind][k] = mine

    def eid_of(tab, k):
        return f'{k[0]}__{k[1]}'

    placed = {}                         # round 30: tab -> what was put where, for the mod's own Codex

    def write_entry(tab, k, pos, parents, is_kid):
        placed.setdefault(tab, []).append((k, pos, list(parents), is_kid))
        key, pages = PAGES[k]
        e = entries[k]
        ent = {'name': key, 'description': key + '.desc', 'icon': {'item': e.get('icon', q('quantum_codex')).split(',')[0]},
               'category': f'{NS}:{tab}', 'x': pos[0], 'y': pos[1], 'hide_while_locked': False,
               'background_u_index': 0, 'background_v_index': 0, 'pages': pages,
               'parents': [{'entry': f'{NS}:{tab}/{eid_of(tab, p)}'} for p in parents]}
        if is_kid:
            for p in ent['parents']: p['draw_arrow'] = False
        if e.get('advancement'):
            ent['condition'] = {'type': 'modonomicon:advancement', 'advancement_id': e['advancement']}
        w(os.path.join(root, 'entries', tab, eid_of(tab, k) + '.json'), ent)

    def category(tab, n, desc, icon, sort, ext):
        LANG_[f'{pre}.category.{tab}'] = n
        LANG_[f'{pre}.category.{tab}.desc'] = desc
        w(os.path.join(root, 'categories', tab + '.json'), {
            'name': f'{pre}.category.{tab}', 'description': f'{pre}.category.{tab}.desc', 'icon': {'item': q(icon)},
            'sort_number': sort, 'display_mode': 'node', 'max_scroll_x': max(800, ext * 40 + 600), 'max_scroll_y': max(800, ext * 40 + 600),     # (round 30: room to reach every node)
            'background': f'{NS}:textures/gui/codex/space_0.png', 'background_width': 512, 'background_height': 512,
            'background_parallax_layers': [{'background': f'{NS}:textures/gui/codex/space_%d.png' % i, 'speed': sp}
                                           for i, sp in ((0, 0.25), (1, 0.45), (2, 0.7), (3, 1.0))]})

    def research_parents(k, members):
        """The nearest chapters on this map that this one's research builds on (at most two)."""
        by_rid = {}
        for m in members:
            r = rid(entries[m])
            if r: by_rid.setdefault(r, []).append(m)
        r = rid(entries[k])
        if not r or r not in req: return []
        seen, frontier = {r}, list(req[r])
        for _ in range(10):
            hit = [x for f in frontier if f in by_rid for x in by_rid[f] if x != k]
            if hit: return sorted(set(hit), key=order)[:2]
            nxt = []
            for f in frontier:
                if f in seen or f not in req: continue
                seen.add(f)
                nxt += req[f]
            frontier = nxt
        return []

    # ---- round 36: every map is laid out in tiers, the way the big quest books group a chapter's quests together:
    # a band per stage of the game (left to right), each holding only what belongs to that stage, with the things
    # inside a band ordered so that links stay short (each one placed near what it builds on).
    rx = {}
    for f in glob.glob(os.path.join(D, 'ts_research', '*.json')):
        rx[os.path.basename(f)[:-5]] = json.load(open(f)).get('x', 0)
    MRES = g.get('MACHINE_RESEARCH') or {}
    FALLBACK = {('quantum', 'casimir_press'): 0, ('chemistry', 'essences'): 19, ('biology', 'anomaly_spawner'): 14,
                ('fabrication', 'processors'): 2, ('fabrication', 'wafers'): 2, ('fabrication', 'convergence_altar'): 9,
                ('fabrication', 'more_singularities'): 9, ('world', 'impossible_interiors'): 9, ('world', 'arriving_safely'): 2,
                ('world', 'attunement_hierarchy'): 11, ('world', 'dimension_laws'): 9, ('world', 'falling_through'): 9,
                ('world', 'transcendent_cache'): 21, ('operation', 'principles'): 5, ('operation', 'cooling'): 5}
    CAT_X = {'basics': 0, 'world': 1, 'endgame': 22, 'fabrication': 3, 'operation': 8}

    def research_x(k):
        r = rid(entries[k])
        if r and r in rx: return rx[r]
        if k in FALLBACK: return FALLBACK[k]
        for cand in (k[1], _id(entries[k].get('icon', ''))):
            mr = MRES.get(cand)
            if mr and mr in rx: return rx[mr]
        return CAT_X.get(k[0], 0)

    def tier_of(k):
        x = research_x(k)
        for i, (lo, hi, _r, _n) in enumerate(TIERS):
            if lo <= x <= hi: return i
        return len(TIERS) - 1

    regions = {}

    def region_list(pos, tiers_of):
        out = []
        for i, (lo, hi, rn, nm) in enumerate(TIERS):
            ps = [pos[k] for k in pos if tiers_of.get(k) == i]
            if not ps: continue
            out.append({'tier': i, 'label': f'{rn} \u00b7 {nm}', 'x0': min(p[0] for p in ps) - 1.5, 'x1': max(p[0] for p in ps) + 1.5,
                        'y0': min(p[1] for p in ps) - 2.5, 'y1': max(p[1] for p in ps) + 1.5})
        return out

    def pack_column(ks, ideal, y_mid):
        """Places a column's members as close to their ideal heights as spacing 2 allows, centred on what they want."""
        ks = sorted(ks, key=lambda k: (ideal.get(k, y_mid), order(k)))
        ys, last = [], None
        for k in ks:
            y = round(ideal.get(k, y_mid) / 2) * 2
            if last is not None and y < last + 2: y = last + 2
            ys.append(y)
            last = y
        want = sum(ideal.get(k, y_mid) for k in ks) / len(ks)
        shift = round((want - sum(ys) / len(ys)) / 2) * 2
        return {k: y + shift for k, y in zip(ks, ys)}

    total = 0
    for sort, (tab, n, desc, icon) in enumerate(TABS):
        members = tab_members[tab]
        if not members: continue
        pos = {}
        tiers_of = {}
        if tab == 'discovery':
            # the Getting Started chain first, a column of its own at the left edge, top to bottom
            for i, k in enumerate(guide):
                if k in members:
                    pos[k] = (-4, (i - len(guide) // 2) * 2)
                    tiers_of[k] = 0
            parents_of = {k: research_parents(k, members) for k in members if k not in guide_set}
            for k in members:
                link = GUIDE['links'].get(k)
                if k in parents_of and link and link[0] in members and link[0] not in parents_of[k]:
                    parents_of[k] = [link[0]] + parents_of[k][:1]
            x0 = 0
            for ti in range(len(TIERS)):
                ks = [k for k in members if k not in guide_set and tier_of(k) == ti]
                if not ks: continue
                cols = {}
                for k in ks: cols.setdefault(research_x(k), []).append(k)
                for c in sorted(cols):
                    col = cols[c]
                    for chunk in [col[i:i + 12] for i in range(0, len(col), 12)]:       # a tall column wraps
                        ideal = {}
                        for k in chunk:
                            ps = [pos[p][1] for p in parents_of.get(k, []) if p in pos]
                            if ps: ideal[k] = sum(ps) / len(ps)
                        for k, y in pack_column(chunk, ideal, 0).items():
                            pos[k] = (x0, y)
                            tiers_of[k] = ti
                        x0 += 2
                x0 += 3                                     # the gap between two tiers
            for k in members:
                if k in guide_set:
                    i = guide.index(k)
                    write_entry(tab, k, pos[k], [guide[i - 1]] if i else [], False)
                else:
                    write_entry(tab, k, pos[k], parents_of.get(k, []), False)
        else:
            # constellations (a chapter in the middle of its things), stacked in columns inside their tier's band
            def side(m):
                kk = math.ceil(math.sqrt(len(members[m]) + 1))
                return kk + 1 - kk % 2
            ordered = sorted(members, key=order)
            area = sum((side(m) * 2 + 1) ** 2 for m in ordered)
            band_h = max(max([side(m) * 2 + 1 for m in ordered] + [0]), int(math.sqrt(area / 1.8)) + 1)
            occ = set()
            cx = 0
            parents_of = {m: research_parents(m, list(members)) for m in ordered}
            for ti in range(len(TIERS)):
                ms = [m for m in ordered if tier_of(m) == ti]
                if not ms: continue
                # within the band: split into columns in research order, then each column sorted so every
                # constellation sits level with what it builds on (placed further left) - short links
                ms.sort(key=lambda m: (research_x(m), order(m)))
                columns, cur, h = [], [], 0
                for m in ms:
                    sz = side(m) * 2 + 1
                    if cur and h + sz > band_h:
                        columns.append(cur)
                        cur, h = [], 0
                    cur.append(m)
                    h += sz
                if cur: columns.append(cur)
                col_x = cx
                for col in columns:
                    def ideal(m):
                        ps = [pos[p][1] for p in parents_of.get(m, []) if p in pos]
                        return sum(ps) / len(ps) if ps else None
                    keyed = [(ideal(m), i, m) for i, m in enumerate(col)]
                    known = [k for k in keyed if k[0] is not None]
                    mean = sum(k[0] for k in known) / len(known) if known else band_h / 2
                    col = [m for _, _, m in sorted(keyed, key=lambda k: (k[0] if k[0] is not None else mean, k[1]))]
                    col_w = max(side(m) * 2 + 1 for m in col)
                    total_h = sum(side(m) * 2 + 1 for m in col)
                    cy = max(0, min(band_h - total_h, int(mean - total_h / 2)))    # the column centred on its links
                    for m in col:
                        kk = side(m)
                        sz = kk * 2 + 1
                        px, py = col_x + kk - 1, cy + kk - 1
                        pos[m] = (px, py)
                        tiers_of[m] = ti
                        occ.add((px, py))
                        rr = kk - 1
                        cand = sorted(((px + dx, py + dy) for dx in range(-rr, rr + 1, 2) for dy in range(-rr, rr + 1, 2) if dx or dy),
                                      key=lambda c: ((c[0] - px) ** 2 + (c[1] - py) ** 2, c[1], c[0]))
                        for kid in sorted(members[m], key=lambda x: (entries[x].get('sortnum', 0), x[1])):
                            for c in cand:
                                if c not in occ:
                                    occ.add(c)
                                    pos[kid] = c
                                    tiers_of[kid] = ti
                                    break
                        cy += sz
                    col_x += col_w
                cx = col_x + 3                            # the gap between two tiers
            for m in ordered:
                write_entry(tab, m, pos[m], parents_of[m], False)
                for kid in members[m]:
                    if kid in pos: write_entry(tab, kid, pos[kid], [m], True)
        regions[tab] = region_list(pos, tiers_of)
        xs = [v[0] for v in pos.values()]
        ys = [v[1] for v in pos.values()]
        ext = max(max(xs) - min(xs), max(ys) - min(ys)) if pos else 0
        category(tab, n, desc, icon, sort, ext)
        total += len(pos)
    print('round23 codex:', len(entries), 'chapters and nodes ->', total, 'map nodes in', len(TABS), 'tabs')
    # round 30: the same maps and text for the mod's own Codex screen (client/codex)
    round30_codex.export(g, entries, cats, placed, TABS, regions=regions)
