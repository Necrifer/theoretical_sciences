"""
1.1.0 (text rewrite): real descriptions for what had only a placeholder ("X. Its page in the Quantum Codex tells what it
is for.", "A panel for a structure's walls.") or a Codex page that only repeated its name.

tools/text/flavour.json maps "item:<id>" / "block:<id>" / "entity:<id>" / "fluid:<id>" to the description. It is written
into the lang file (the tooltip) and onto the Codex page in place of the placeholder or the bare name. Runs after the
Codex is built and before the text overrides (tools/regenerate.sh), so the author's own edits always win.
"""
import json
import os
import re

HERE = os.path.dirname(os.path.abspath(__file__))
A = os.path.join(HERE, '..', 'src', 'main', 'resources', 'assets', 'theoreticalsciences')
LANG = os.path.join(A, 'lang', 'en_us.json')
CODEX = os.path.join(A, 'ts_codex', 'entries.json')
SRC = os.path.join(HERE, 'text', 'flavour.json')
NS = 'theoreticalsciences'


def main():
    if not os.path.exists(SRC):
        print('flavour: none')
        return
    F = json.load(open(SRC))
    L = json.load(open(LANG))
    E = json.load(open(CODEX))
    lang = pages = 0
    for key, text in F.items():
        kind, i = key.split(':', 1)
        subjects = {f'{NS}:{i}'}
        if kind in ('item', 'block'):
            lk = f'{kind}.{NS}.{i}.lore'
            old = L.get(lk)
            L[lk] = text
            lang += 1
            if old:
                for e in E.values():
                    for pg in e['pages']:
                        if old in pg.get('text', ''):
                            pg['text'] = pg['text'].replace(old, text)
                            pages += 1
        elif kind == 'entity':
            subjects = {f'{NS}:{i}_spawn_egg'}
            lk = f'item.{NS}.{i}_spawn_egg.lore'
            if lk in L or True:
                L[lk] = text
                lang += 1
        elif kind == 'fluid':
            subjects = {f'{NS}:{i}_cell', f'{NS}:{i}'}
        # a Codex page that only says the name: the name sentence becomes the description
        for e in E.values():
            for pg in e['pages']:
                if pg.get('type') != 'spotlight' or not (set(pg.get('items') or []) & subjects): continue
                t = pg.get('text', '')
                m = re.match(r'\s*(\$\((?:item|thing|fluid)\))?([^$.]+)(\$\(\))?\.\s*(?=\$\(br2\)|$)', t)
                if m and m.group(2).strip() == (pg.get('title') or e.get('title', '')).strip():
                    pg['text'] = text + t[m.end():]
                    pages += 1
    # recipe lines name only the place now: "Made: Made in X." -> "Made in X."
    for e in E.values():
        for pg in e['pages']:
            t = pg.get('text', '')
            if '$(thing)Made$(): ' in t:
                pg['text'] = re.sub(r'\$\(thing\)Made\$\(\): (?=Made |Forged |Crafted )', '', t)
    json.dump(L, open(LANG, 'w'), indent=2)
    json.dump(E, open(CODEX, 'w'), indent=2)
    print(f'flavour: {lang} descriptions, {pages} Codex pages')


if __name__ == '__main__':
    main()
