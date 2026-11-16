"""
1.15.1: exports the mod's tooltips and Codex text for a rewrite, as JSON that tools/export_text.js turns into Word files.
Every piece of text carries the key it came from (in grey), so a rewritten file can be read back in.

Run:  python3 tools/export_text.py <out dir> && node tools/export_text.js <out dir>
"""
import json, os, re, sys, glob

HERE = os.path.dirname(os.path.abspath(__file__))
A = os.path.join(HERE, '..', 'src/main/resources/assets/theoreticalsciences')
NS = 'theoreticalsciences'
out = sys.argv[1]
os.makedirs(out, exist_ok=True)
L = json.load(open(os.path.join(A, 'lang/en_us.json'), encoding='utf-8'))
E = json.load(open(os.path.join(A, 'ts_codex/entries.json'), encoding='utf-8'))
INDEX = json.load(open(os.path.join(A, 'ts_codex/index.json'), encoding='utf-8'))


def name_of(full):
    """'theoreticalsciences:foo' or 'minecraft:bar' -> a display name."""
    ns, i = full.split(':', 1) if ':' in full else (NS, full)
    for k in ('item.%s.%s' % (ns, i), 'block.%s.%s' % (ns, i)):
        if k in L:
            return L[k]
    return i.replace('_', ' ').title()


# ------------------------------------------------------------------ tooltips
def kind_entries(prefix, label):
    ids = sorted({k.split('.')[2] for k in L if k.startswith(prefix + '.' + NS + '.') and len(k.split('.')) == 3})
    rows = []
    for i in ids:
        base = '%s.%s.%s' % (prefix, NS, i)
        parts = []
        for suffix, lab in (('lore', 'Description'), ('found', 'Where found')):
            k = base + '.' + suffix
            if k in L:
                parts.append({'label': lab, 'key': k, 'text': L[k]})
        if parts:
            rows.append({'name': L[base], 'id': base, 'parts': parts})
    rows.sort(key=lambda r: r['name'].lower())
    return {'heading': label, 'entries': rows}


fluids = set(k.split('.')[2] for k in L if k.startswith('fluid_type.' + NS + '.'))
items = kind_entries('item', 'Items')
blocks = kind_entries('block', 'Blocks')
fluid_blocks = {'heading': 'Fluids', 'entries': [e for e in blocks['entries'] if e['id'].split('.')[2] in fluids]}
blocks['entries'] = [e for e in blocks['entries'] if e['id'].split('.')[2] not in fluids]
mobs = kind_entries('entity', 'Creatures')
extra = sorted(k for k in L if k.startswith('tooltip.' + NS + '.'))
extra_sec = {'heading': 'Other tooltip lines', 'entries': [{'name': k.split('.', 2)[2].replace('_', ' ').replace('.', ' · '), 'id': k,
                                                               'parts': [{'label': 'Text', 'key': k, 'text': L[k]}]} for k in extra]}
TIP = {'file': '01 Tooltips.docx', 'title': 'Theoretical Sciences: Tooltips',
       'intro': ['Every item, block, fluid and creature with a description, in alphabetical order within each section, then the '
                 'other tooltip lines. "Description" is the lore shown on the item; "Where found" is the line saying where it is '
                 'found or made.'],
       'sections': [items, blocks, fluid_blocks, mobs, extra_sec]}

# ------------------------------------------------------------------ the Codex, a file per tab
tabs = [t if isinstance(t, str) else t.get('id') for t in INDEX.get('tabs', [])]
tab_files = {os.path.basename(f)[:-5]: f for f in glob.glob(os.path.join(A, 'ts_codex', '*.json'))}
order = [t for t in tabs if t in tab_files] + [t for t in sorted(tab_files) if t not in tabs and t not in ('entries', 'index')]
# each entry once, on the most specific tab it appears on (the tab with the fewest nodes)
TABS = {t: json.load(open(tab_files[t], encoding='utf-8')) for t in order}
home = {}
for t in sorted(order, key=lambda t: len(TABS[t].get('nodes', []))):
    for node in TABS[t].get('nodes', []):
        home.setdefault(node['entry'], t)
placed, docs = set(), [TIP]
for n, tab in enumerate(order, 2):
    T = TABS[tab]
    regions = {r.get('id', r.get('name')): r for r in T.get('regions', [])} if isinstance(T.get('regions'), list) else {}
    entries = []
    for node in T.get('nodes', []):
        eid = node['entry']
        if eid in placed or eid not in E or home.get(eid) != tab:
            continue
        placed.add(eid)
        e = E[eid]
        parts = [{'label': 'Title', 'key': 'codex:%s#title' % eid, 'text': e.get('title', '')}]
        for pi, p in enumerate(e.get('pages', [])):
            kind = p.get('type', 'text')
            if kind == 'multiblock':
                parts.append({'label': 'Page %d (multiblock: %s)' % (pi + 1, p.get('multiblock', '')), 'key': 'codex:%s#%d.name' % (eid, pi),
                              'text': p.get('name', '')})
                if p.get('text'):
                    parts.append({'label': 'Page %d caption' % (pi + 1), 'key': 'codex:%s#%d' % (eid, pi), 'text': p['text']})
                continue
            if kind == 'spotlight':
                shown = ', '.join(name_of(i) for i in p.get('items', []))
                if p.get('title'):
                    parts.append({'label': 'Page %d heading' % (pi + 1), 'key': 'codex:%s#%d.title' % (eid, pi), 'text': p['title']})
                parts.append({'label': 'Page %d (showing %s)' % (pi + 1, shown), 'key': 'codex:%s#%d' % (eid, pi), 'text': p.get('text', '')})
            else:
                if p.get('title'):
                    parts.append({'label': 'Page %d heading' % (pi + 1), 'key': 'codex:%s#%d.title' % (eid, pi), 'text': p['title']})
                parts.append({'label': 'Page %d' % (pi + 1), 'key': 'codex:%s#%d' % (eid, pi), 'text': p.get('text', '')})
        entries.append({'name': e.get('title', eid), 'id': 'codex:' + eid, 'parts': parts})
    title = T.get('name', tab)
    intro = []
    docs.append({'file': '%02d Codex - %s.docx' % (n, title), 'title': 'Theoretical Sciences Codex: ' + title,
                 'intro': intro, 'tab': {'key': 'codextab:%s#description' % tab, 'text': T.get('description', '')},
                 'sections': [{'heading': title, 'entries': entries}]})
print('codex entries placed', len(placed), 'of', len(E))
json.dump(docs, open(os.path.join(out, 'export.json'), 'w', encoding='utf-8'), ensure_ascii=False)
for d in docs:
    print(d['file'], sum(len(s['entries']) for s in d['sections']), 'entries',
          sum(len(p['text'].split()) for s in d['sections'] for e in s['entries'] for p in e['parts']), 'words')
