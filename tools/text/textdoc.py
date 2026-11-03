"""
1.1.0: the mod's player-facing text as one Word document, for a full rewrite, and the way back in.

    python3 tools/text/textdoc.py export     -> tools/text/out/payload.json (+ snapshot.json); build_docx.js turns it into the .docx
    python3 tools/text/textdoc.py import X.docx  -> tools/text/overrides.json (only what was changed)

Every Codex page and every lang line gets a stable ID in the document. On import, a piece whose text reads exactly as
it was exported is left alone (so the document's way of showing the Codex's formatting never changes untouched text);
only edited pieces become overrides, which gen_data applies last (round_text_overrides.py).

Codex formatting in the document:
  bold = $(item)...$()   italic = $(thing)...$()   bold italic = $(fluid)...$()
  underlined = $(l)...$() (a sub-heading)   bold underlined = $(bold)...$()
  a new paragraph = $(br2)   a line break (Shift+Enter) = $(br)   a bulleted paragraph = $(li)
Anything else in $(...) stays as literal text.
"""
import json
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.join(HERE, '..', '..')
ASSETS = os.path.join(ROOT, 'src', 'main', 'resources', 'assets', 'theoreticalsciences')
CODEX = os.path.join(ASSETS, 'ts_codex')
LANG = os.path.join(ASSETS, 'lang', 'en_us.json')
OUT = os.path.join(HERE, 'out')
NS = 'theoreticalsciences'

TOKEN = re.compile(r'\$\([^)]*\)')
STYLE = {'$(item)': 'item', '$(thing)': 'thing', '$(fluid)': 'fluid', '$(l)': 'head', '$(bold)': 'strong'}


# ------------------------------------------------------------------ Codex markup <-> paragraphs
def to_paras(text):
    """Codex text -> [{'li': bool, 'runs': [{'t': str, 's': style|None} | {'br': True}]}]."""
    paras = [{'li': False, 'runs': []}]
    style = None
    pos = 0

    def put(t):
        if t:
            paras[-1]['runs'].append({'t': t, 's': style})

    for m in TOKEN.finditer(text):
        put(text[pos:m.start()])
        tok = m.group(0)
        if tok in STYLE:
            style = STYLE[tok]
        elif tok == '$()':
            style = None
        elif tok == '$(br2)':
            paras.append({'li': False, 'runs': []})
        elif tok == '$(br)':
            paras[-1]['runs'].append({'br': True})
        elif tok == '$(li)':
            if paras[-1]['runs'] or paras[-1]['li']:
                paras.append({'li': True, 'runs': []})
            else:
                paras[-1]['li'] = True
        else:
            put(tok)                 # unknown codes stay as they are written
        pos = m.end()
    put(text[pos:])
    return paras


def from_paras(paras):
    """The inverse: paragraphs (as read back from the document) -> Codex text."""
    out = []
    for i, p in enumerate(paras):
        if p['li']:
            out.append('$(li)')
        elif i:
            out.append('$(br2)')
        cur = None
        for r in p['runs']:
            if r.get('br'):
                if cur:
                    out.append('$()')
                    cur = None
                out.append('$(br)')
                continue
            s = r['s']
            if s != cur:
                if cur:
                    out.append('$()')
                if s:
                    out.append({v: k for k, v in STYLE.items()}[s])
                cur = s
            out.append(r['t'])
        if cur:
            out.append('$()')
    return ''.join(out)


def canon(text):
    """What a piece of Codex text reads as after a trip through the document."""
    return from_paras(merge(to_paras(text)))


def merge(paras):
    for p in paras:
        runs = []
        for r in p['runs']:
            if runs and not r.get('br') and not runs[-1].get('br') and runs[-1]['s'] == r['s']:
                runs[-1] = {'t': runs[-1]['t'] + r['t'], 's': r['s']}
            elif r.get('br') or r['t']:
                runs.append(r)
        p['runs'] = runs
    return paras


# ------------------------------------------------------------------ what goes in, in what order
def short(key):
    """Display ID of a lang key: the namespace segment dropped (the snapshot maps it back)."""
    return key.replace('.' + NS + '.', ':', 1) if ('.' + NS + '.') in key else key


LANG_SECTIONS = [
    # (title, intro, test)
    ('Research', 'Research titles and the descriptions shown in the research tree.', lambda k: k.startswith('research.')),
    ('Tooltips and item hints', 'Extra tooltip lines: usage hints, modes, shields, JEI/EMI/Jade lines.',
     lambda k: k.startswith(('tooltip.', 'jei.', 'emi.', 'jade.')) or (k.startswith(('item.', 'block.')) and k.count('.') >= 3)),
    ('Machine consoles', 'Names and hover descriptions on each machine\'s operator console: its settings, readings, conditions and '
                         'disturbances. Grouped by machine.', lambda k: k.startswith('physics.')),
    ('Other names', 'Fluids, creatures, effects, biomes and dimensions.',
     lambda k: k.startswith(('fluid_type.', 'fluid.', 'entity.', 'effect.', 'biome.', 'dimname.'))),
    ('Interface, messages and puzzles', 'Screens, chat messages, statuses, travel scenes, proofs and puzzles.', lambda k: True),
    ('Sound subtitles', 'Shown with subtitles on.', lambda k: k.startswith('subtitles.')),
]


def is_item_line(k, L):
    p = k.split('.')
    return p[0] in ('item', 'block') and len(p) >= 3 and p[1] == NS and (len(p) == 3 or (len(p) == 4 and p[3] == 'lore'))


def export():
    E = json.load(open(os.path.join(CODEX, 'entries.json')))
    I = json.load(open(os.path.join(CODEX, 'index.json')))
    L = json.load(open(LANG))
    snap = {'codex': {}, 'lang': {}, 'tabs': {}, 'chapters': {}}
    pay = {'tabs': [], 'chapters': [], 'codex': [], 'items': [], 'lang': []}

    # tabs and chapter names
    for t in I['tabs']:
        f = os.path.join(CODEX, t['id'] + '.json')
        d = json.load(open(f)) if os.path.exists(f) else t
        snap['tabs'][t['id']] = {'name': d['name'], 'description': d['description']}
        pay['tabs'].append({'id': 'tab:' + t['id'], 'name': d['name'], 'description': d['description']})
    for c, n in I['categories'].items():
        snap['chapters'][c] = n
        pay['chapters'].append({'id': 'chapter:' + c, 'name': n})

    # the Codex, chapter by chapter
    cats = list(I['categories'])
    cats = sorted(cats, key=lambda c: (c != 'basics', list(I['categories']).index(c)))
    names = {}
    for k, v in L.items():
        if is_item_line(k, L) and k.count('.') == 2:
            names[k.split('.')[0] + ':' + k.split('.')[2]] = v
    for c in cats:
        ch = {'cat': c, 'name': I['categories'][c], 'entries': []}
        for eid, e in E.items():
            if e['category'] != c:
                continue
            ent = {'id': eid, 'title': e['title'], 'pages': []}
            snap['codex'][eid] = {'title': e['title'], 'pages': []}
            for n, p in enumerate(e['pages']):
                tkey = 'name' if p['type'] == 'multiblock' else 'title'
                info = p['type']
                if p.get('items'):
                    shown = []
                    for it in p['items'][:3]:
                        iid = it.split(':')[-1]
                        shown.append(names.get('item:' + iid) or names.get('block:' + iid) or iid)
                    info += ' · shows ' + ', '.join(shown) + (' …' if len(p['items']) > 3 else '')
                ent['pages'].append({'n': n + 1, 'info': info, 'title': p.get(tkey), 'paras': merge(to_paras(p.get('text', '')))})
                snap['codex'][eid]['pages'].append({'title': p.get(tkey), 'tkey': tkey, 'text': p.get('text', '')})
            ch['entries'].append(ent)
        pay['codex'].append(ch)

    # items and blocks: name + description on one row
    used = set()
    rows = {}
    for k, v in L.items():
        if is_item_line(k, L):
            p = k.split('.')
            rid = p[0] + ':' + p[2]
            rows.setdefault(rid, {'id': rid, 'name': None, 'lore': None})
            rows[rid]['lore' if len(p) == 4 else 'name'] = v
            used.add(k)
            snap['lang'][rid + ('#lore' if len(p) == 4 else '#name')] = {'key': k, 'text': v}
    pay['items'] = [rows[r] for r in sorted(rows, key=lambda r: (r.split(':')[1], r))]

    # everything else in the lang file
    left = [k for k in L if k not in used]
    for title, intro, test in LANG_SECTIONS:
        if title == 'Interface, messages and puzzles':
            mine = [k for k in left if not k.startswith('subtitles.')]
        else:
            mine = [k for k in left if test(k)]
        left = [k for k in left if k not in set(mine)]
        sec = {'title': title, 'intro': intro, 'rows': []}
        for k in sorted(mine):
            sid = short(k)
            group = k.split('.')[2] if title == 'Machine consoles' and k.count('.') >= 2 else None
            sec['rows'].append({'id': sid, 'text': L[k], 'group': group})
            snap['lang'][sid] = {'key': k, 'text': L[k]}
        if title == 'Interface, messages and puzzles':
            interface = sec
        else:
            pay['lang'].append(sec)
    pay['lang'].insert(4, interface)
    os.makedirs(OUT, exist_ok=True)
    json.dump(pay, open(os.path.join(OUT, 'payload.json'), 'w'), ensure_ascii=False)
    json.dump(snap, open(os.path.join(HERE, 'snapshot.json'), 'w'), ensure_ascii=False, indent=0)
    words = sum(len(p['text'].split()) for e in snap['codex'].values() for p in e['pages'])
    print(f"export: {len(snap['codex'])} Codex entries ({sum(len(e['pages']) for e in snap['codex'].values())} pages, {words} words), "
          f"{len(snap['lang'])} lang lines, {len(snap['tabs'])} tabs, {len(snap['chapters'])} chapters")


# ------------------------------------------------------------------ the way back: the edited document -> overrides.json
MARK = re.compile(r'^\s*⟦([^⟧]+)⟧')
PLACEHOLDER = re.compile(r'%(?:\d+\$)?[sd]')


def _runs(p):
    from docx.text.run import Run
    from docx.oxml.ns import qn
    out = []
    for r in p._p.iter(qn('w:r')):
        if r.getparent().tag == qn('w:del'):
            continue
        run = Run(r, p)
        b, i, u = bool(run.bold), bool(run.italic), bool(run.underline)
        s = 'fluid' if b and i else 'strong' if b and u else 'head' if u else 'item' if b else 'thing' if i else None
        for j, piece in enumerate(run.text.replace('\t', ' ').split('\n')):
            if j:
                out.append({'br': True})
            if piece:
                out.append({'t': piece, 's': s})
    return out


def _is_li(p):
    from docx.oxml.ns import qn
    ppr = p._p.pPr
    return ppr is not None and ppr.find(qn('w:numPr')) is not None or (p.style is not None and 'List' in (p.style.name or ''))


def read_doc(path):
    """The document -> {'codex': {eid: {'title', 'pages': {n: {'title', 'paras'}}}}, 'cells': {id: [cell texts]}}."""
    import docx
    from docx.table import Table
    from docx.text.paragraph import Paragraph
    from docx.oxml.ns import qn
    d = docx.Document(path)
    codex, cells = {}, {}
    pending, page, expect_title = None, None, False
    for el in d.element.body.iterchildren():
        if el.tag == qn('w:tbl'):
            page = None
            for row in Table(el, d).rows[1:]:
                vals = [c.text for c in row.cells]
                if vals and vals[0].strip():
                    cells[vals[0].strip()] = vals[1:]
            continue
        if el.tag != qn('w:p'):
            continue
        p = Paragraph(el, d)
        style = p.style.name if p.style is not None else ''
        text = p.text
        m = MARK.match(text)
        if m:
            ident = m.group(1).strip()
            if ident.startswith('entry:'):
                codex.setdefault(ident[6:], {'title': pending, 'pages': {}})
                pending = None
                page = None
            elif ident.startswith('codex:'):
                eid, n = ident[6:].rsplit(':', 1)
                page = codex.setdefault(eid, {'title': None, 'pages': {}})['pages'].setdefault(int(n), {'title': None, 'paras': []})
                expect_title = True
            continue
        if style.startswith('Heading 1') or style == 'Title':
            page = None
            continue
        if style.startswith('Heading 2'):
            pending, page = text.strip(), None
            continue
        if page is None:
            continue
        if style.startswith('Heading 3') and expect_title and not page['paras']:
            page['title'] = text.strip()
            expect_title = False
            continue
        expect_title = False
        runs = _runs(p)
        if any(r.get('br') or r['t'].strip() for r in runs):
            page['paras'].append({'li': _is_li(p), 'runs': runs})
    return codex, cells


def _trim(t):
    while t.endswith(('$(br2)', '$(br)')):
        t = t[:-6] if t.endswith('$(br2)') else t[:-5]
    return t


def _accepted(path):
    """A copy of the document with tracked changes accepted (when it has any)."""
    import zipfile
    import subprocess
    xml = zipfile.ZipFile(path).read('word/document.xml').decode('utf8', 'replace')
    if '<w:ins ' not in xml and '<w:del ' not in xml:
        return path
    skill = os.environ.get('DOCX_SKILL', '')
    out = os.path.join(OUT, 'accepted.docx')
    subprocess.run([sys.executable, os.path.join(skill, 'scripts', 'accept_changes.py'), path, out], check=True)
    return out


def import_doc(path):
    snap = json.load(open(os.path.join(HERE, 'snapshot.json')))
    codex, cells = read_doc(_accepted(path))
    ov = {'codex': {}, 'lang': {}, 'tabs': {}, 'chapters': {}}
    notes = []
    for eid, e in snap['codex'].items():
        got = codex.get(eid)
        if got is None:
            notes.append(f'missing from the document (left as it is): Codex entry {eid}')
            continue
        o = {}
        if got['title'] and got['title'] != e['title']:
            o['title'] = got['title']
        for n, sp in enumerate(e['pages'], 1):
            gp = got['pages'].get(n)
            if gp is None:
                notes.append(f'missing from the document (left as it is): {eid} page {n}')
                continue
            new = from_paras(merge(gp['paras']))
            new = re.sub(r'([.,;:!?]+)\$\(\)', r'$()\1', new)        # a full stop typed inside the bold goes after it
            po = {}
            if _trim(new) != _trim(canon(sp['text'])):
                po['text'] = new
            if sp['title'] is not None and gp['title'] is not None and gp['title'] != sp['title']:
                po['title'], po['tkey'] = gp['title'], sp['tkey']
            if po:
                o.setdefault('pages', {})[str(n)] = po
        if o:
            ov['codex'][eid] = o
    for eid in codex:
        if eid not in snap['codex']:
            notes.append(f'unknown Codex ID in the document (ignored): {eid}')

    def lang(sid, new):
        old = snap['lang'][sid]
        new = new.strip('\n')
        if new == old['text']:
            return
        if sorted(PLACEHOLDER.findall(new)) != sorted(PLACEHOLDER.findall(old['text'])):
            notes.append(f'placeholders changed in {sid} - check: {old["text"]!r} -> {new!r}')
        ov['lang'][old['key']] = new

    for cid, vals in cells.items():
        if cid.startswith('tab:'):
            t = snap['tabs'].get(cid[4:])
            if t and len(vals) >= 2:
                o = {k: v for k, v in (('name', vals[0].strip()), ('description', vals[1].strip())) if v != t[k]}
                if o:
                    ov['tabs'][cid[4:]] = o
        elif cid.startswith('chapter:'):
            c = cid[8:]
            if c in snap['chapters'] and vals and vals[0].strip() != snap['chapters'][c]:
                ov['chapters'][c] = vals[0].strip()
        elif cid + '#name' in snap['lang'] or cid + '#lore' in snap['lang']:
            for k, v in (('#name', vals[0] if vals else None), ('#lore', vals[1] if len(vals) > 1 else None)):
                if cid + k in snap['lang'] and v is not None:
                    lang(cid + k, v)
        elif cid in snap['lang']:
            if vals:
                lang(cid, vals[0])
        else:
            notes.append(f'unknown ID in a table (ignored): {cid}')
    # 1.1.0: each returned document adds to the overrides already kept (a later document wins where both changed a piece)
    op = os.path.join(HERE, 'overrides.json')
    if os.path.exists(op) and os.environ.get('TS_TEXT_FRESH') != '1':
        prev = json.load(open(op))
        for eid, o in ov['codex'].items():
            pe = prev['codex'].setdefault(eid, {})
            if 'title' in o: pe['title'] = o['title']
            pe.setdefault('pages', {}).update(o.get('pages', {}))
        for k in ('lang', 'tabs', 'chapters'):
            prev.setdefault(k, {}).update(ov[k])
        ov = prev
    json.dump(ov, open(op, 'w'), ensure_ascii=False, indent=1)
    n = sum(len(e.get('pages', {})) + ('title' in e) for e in ov['codex'].values())
    print(f"import: {n} Codex changes in {len(ov['codex'])} entries, {len(ov['lang'])} lang lines, "
          f"{len(ov['tabs'])} tabs, {len(ov['chapters'])} chapters")
    for x in notes:
        print('  note:', x)
    open(os.path.join(OUT, 'import_notes.txt'), 'w').write('\n'.join(notes))


# ------------------------------------------------------------------ applied as the last step of tools/regenerate.sh
def apply():
    p = os.path.join(HERE, 'overrides.json')
    if not os.path.exists(p):
        print('text overrides: none')
        return
    ov = json.load(open(p))
    E = json.load(open(os.path.join(CODEX, 'entries.json')))
    n = 0
    for eid, o in ov.get('codex', {}).items():
        e = E.get(eid)
        if e is None:
            print('  text overrides: no Codex entry', eid)
            continue
        if 'title' in o:
            e['title'] = o['title']
            n += 1
        for k, po in o.get('pages', {}).items():
            i = int(k) - 1
            if i >= len(e['pages']):
                print('  text overrides: no page', k, 'in', eid)
                continue
            if 'text' in po:      # (and "Made: Made in X" - the recipe lines name only the place)
                e['pages'][i]['text'] = re.sub(r'\$\(thing\)Made\$\(\): (?=Made |Forged |Crafted )', '', po['text'])
            if 'title' in po:
                e['pages'][i][po.get('tkey', 'title')] = po['title']
            n += 1
    json.dump(E, open(os.path.join(CODEX, 'entries.json'), 'w'), indent=2)
    I = json.load(open(os.path.join(CODEX, 'index.json')))
    for c, name in ov.get('chapters', {}).items():
        if c in I['categories']:
            I['categories'][c] = name
            n += 1
    for t in I['tabs']:
        o = ov.get('tabs', {}).get(t['id'])
        if not o:
            continue
        t.update(o)
        f = os.path.join(CODEX, t['id'] + '.json')
        if os.path.exists(f):
            d = json.load(open(f))
            d.update(o)
            json.dump(d, open(f, 'w'), indent=2)
        n += 1
    json.dump(I, open(os.path.join(CODEX, 'index.json'), 'w'), indent=2)
    L = json.load(open(LANG))
    for k, v in ov.get('lang', {}).items():
        if k in L:
            L[k] = v
            n += 1
        else:
            print('  text overrides: no lang key', k)
    json.dump(L, open(LANG, 'w'), indent=2)
    print(f'text overrides: {n} pieces of text applied')


if __name__ == '__main__':
    {'export': export, 'import': import_doc, 'apply': apply}[sys.argv[1]](*sys.argv[2:])
