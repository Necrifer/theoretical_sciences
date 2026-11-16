"""
1.15.1: reads the rewritten Word files from tools/export_text.py back into the mod.

Each grey label line names the key its text came from; the paragraphs under it (up to the next label or heading) are
that text. A paragraph after the first is joined with $(br2) (a paragraph that already starts with "$(br2) " keeps it
once); a line break inside a paragraph is $(br). Tooltips go to lang/en_us.json; Codex titles and pages to
ts_codex/entries.json and the Patchouli copy.

Run:  python3 tools/import_text.py <folder of .docx> [--write]     (without --write it only lists what would change)
"""
import glob, json, os, re, sys
import docx

HERE = os.path.dirname(os.path.abspath(__file__))
A = os.path.join(HERE, '..', 'src/main/resources/assets/theoreticalsciences')
LABEL = re.compile(r'^(.+?)  ·  ((?:codex|codextab):\S+|[a-z_]+\.theoreticalsciences\.\S+)$')


def para_text(p):
    out = []
    for r in p.runs:
        for child in r._r.iterchildren():
            tag = child.tag.split('}')[1]
            if tag == 't':
                out.append(child.text or '')
            elif tag == 'br':
                out.append('\n')
            elif tag == 'tab':
                out.append('\t')
    s = ''.join(out)
    s = s.replace('\n$(br)', '$(br)').replace('\n', '$(br)')
    return s


def read(path):
    texts, key, buf = {}, None, []

    def flush():
        if key is not None:
            parts = []
            for i, t in enumerate(buf):
                if i == 0:
                    parts.append(t)
                else:
                    parts.append('$(br2)' + (t[len('$(br2) '):] if t.startswith('$(br2) ') else t))
            texts[key] = ''.join(parts)

    for p in docx.Document(path).paragraphs:
        style = p.style.name if p.style is not None else ''
        t = para_text(p)
        m = LABEL.match(t.strip())
        if m:
            flush()
            key, buf = m.group(2), []
        elif style.startswith('Heading') or style == 'Title':
            flush()
            key, buf = None, []
        elif key is not None:
            buf.append(t)
    flush()
    return texts


def main():
    folder = sys.argv[1]
    write = '--write' in sys.argv
    got = {}
    for f in sorted(glob.glob(os.path.join(folder, '*.docx'))):
        got.update(read(f))
    lang_p = os.path.join(A, 'lang/en_us.json')
    L = json.load(open(lang_p, encoding='utf-8'))
    ent_p = os.path.join(A, 'ts_codex/entries.json')
    E = json.load(open(ent_p, encoding='utf-8'))
    changed = []
    for k, v in got.items():
        if k.startswith('codex:'):
            eid, where = k[6:].split('#', 1)
            if eid not in E:
                continue
            if where == 'title':
                old = E[eid].get('title', '')
                if old != v:
                    changed.append(k)
                    E[eid]['title'] = v
            else:
                pi, _, field = where.partition('.')
                page = E[eid]['pages'][int(pi)]
                field = field or 'text'
                if page.get(field, '') != v:
                    changed.append(k)
                    page[field] = v
        elif k.startswith('codextab:'):
            continue                                  # tab descriptions: printed, applied by hand
        elif k in L and L[k] != v:
            changed.append(k)
            L[k] = v
    print('texts read:', len(got), ' changed:', len(changed))
    for k in changed[:40]:
        print('  ', k)
    if write and changed:
        raw = open(lang_p, encoding='utf-8').read()
        json.dump(L, open(lang_p, 'w', encoding='utf-8'), ensure_ascii=False, indent=1 if raw.startswith('{\n "') else 2)
        raw = open(ent_p, encoding='utf-8').read()
        json.dump(E, open(ent_p, 'w', encoding='utf-8'), ensure_ascii=False, indent=1 if raw.startswith('{\n "') else 2)
        print('written (rebuild the Patchouli copy and the mod after this)')


if __name__ == '__main__':
    main()
