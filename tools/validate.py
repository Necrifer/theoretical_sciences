import glob
import json, os, re, sys, zipfile
R = 'src/main/resources'
A = R + '/assets/theoreticalsciences'
errs = []
def load(p):
    try: return json.load(open(p))
    except Exception as e: errs.append(f'bad json {p}: {e}')
for root, _, fs in os.walk(R):
    for f in fs:
        if f.endswith('.json') or f.endswith('.mcmeta'): load(os.path.join(root, f))
# registered ids from java + content
C = json.load(open('tools/content.json')); M = json.load(open('tools/machines.json'))
items = {i['id'] for i in C['items']} | {b['id'] for b in C['blocks']} | {m['id'] for m in M} | {'interface_port', 'research_terminal', 'extreme_assembly_table', 'singularity_compactor', 'autonomous_extreme_assembler', 'forged_omnipotence_core', 'pleroma_confluence'} | {'display_' + k for k in ('black_hole','white_hole','wormhole','pocket_universe','tesseract','pulsar','galaxy','quasar','dyson_sphere','penteract')} | {f['id'] + '_cell' for f in C['fluids']}
# 1.3.7: the Interior's suits are registered in Java (InteriorRegistry)
items |= {f'{s_}_{p_}' for s_ in ('refractory_suit', 'pressure_suit', 'whipple_shield', 'frontier_suit') for p_ in ('helmet', 'chestplate', 'leggings', 'boots')}
fluids = {f['id'] for f in C['fluids']}
lang = load(A + '/lang/en_us.json')
for i in items:
    if not os.path.exists(f'{A}/models/item/{i}.json'): errs.append('no item model ' + i)
    if f'item.theoreticalsciences.{i}' not in lang and f'block.theoreticalsciences.{i}' not in lang: errs.append('no lang ' + i)
# texture refs
vanilla = zipfile.ZipFile(__import__("mcpaths").CLIENT_JAR).namelist()
for root, _, fs in os.walk(A + '/models'):
    for f in fs:
        m = load(os.path.join(root, f))
        for k, v in m.get('textures', {}).items():
            if v.startswith('#'): continue
            ns, p = v.split(':') if ':' in v else ('minecraft', v)
            path = f'{R}/assets/{ns}/textures/{p}.png'
            if ns == 'theoreticalsciences' and not os.path.exists(path): errs.append(f'missing texture {v} in {f}')
        par = m.get('parent', '')
        if par.startswith('theoreticalsciences:') and not os.path.exists(f'{A}/models/{par.split(":")[1]}.json'): errs.append('missing parent ' + par)
for root, _, fs in os.walk(A + '/blockstates'):
    for f in fs:
        bs = load(os.path.join(root, f))
        vs = list(bs['variants'].values()) if 'variants' in bs else [p_['apply'] for p_ in bs['multipart']]
        for v in vs:
            for vv in (v if isinstance(v, list) else [v]):
                if not os.path.exists(f'{A}/models/{vv["model"].split(":")[1]}.json'): errs.append('missing model ' + vv['model'])
# recipe refs
def chk(id_, kind):
    ns, p = id_.split(':')
    if ns == 'theoreticalsciences':
        if kind == 'item' and p not in items: errs.append('unknown item ' + id_)
        if kind == 'fluid' and p not in fluids: errs.append('unknown fluid ' + id_)
    elif ns == 'minecraft':
        if kind == 'item' and not any(n.endswith(f'/{p}.json') for n in vanilla if '/models/item/' in n): errs.append('unknown vanilla item ' + id_)
for root, _, fs in os.walk(R + '/data/theoreticalsciences/recipe'):
    for f in fs:
        r = load(os.path.join(root, f))
        s = json.dumps(r)
        for m_ in re.finditer(r'"(?:item|id)": "([a-z_]+:[a-z_/]+)"', s):
            chk(m_.group(1), 'fluid' if any(m_.group(1).endswith(':' + x) for x in fluids) and 'fluid_outputs' in s and m_.group(1).split(':')[1] in fluids else 'item')
        for m_ in re.finditer(r'"fluid": "([a-z_]+:[a-z_]+)"', s):
            if not m_.group(1).startswith('minecraft:'): chk(m_.group(1), 'fluid')
for root, _, fs in os.walk(R + '/data/theoreticalsciences/ts_research'):
    for f in fs:
        r = load(os.path.join(root, f))
        chk(r['icon'], 'item')
        for c in r['cost']: chk(c['item'], 'item')
# patchouli recipe pages exist
for root, _, fs in os.walk(A + '/patchouli_books'):
    for f in fs:
        e = load(os.path.join(root, f))
        for pg in e.get('pages', []):
            if pg.get('type') == 'patchouli:crafting':
                p = pg['recipe'].split(':')[1]
                if not os.path.exists(f'{R}/data/theoreticalsciences/recipe/{p}.json'): errs.append('patchouli missing recipe ' + p)

# research nodes must not sit on top of each other (a hidden node under a visible one can't be clicked)
_pos = {}
for _f in glob.glob('src/main/resources/data/theoreticalsciences/ts_research/*.json'):
    _d = json.load(open(_f))
    _k = (_d.get('x', 0), _d.get('y', 0))
    if _k in _pos: errs.append(f'research overlap at {_k}: {_pos[_k]} and {os.path.basename(_f)}')
    _pos[_k] = os.path.basename(_f)
# every research that can be studied needs a derivation puzzle, and every puzzle must be well formed
import sys as _sys
_sys.path.insert(0, 'tools')
import puzzle2 as _p2
_gl = json.load(open('src/main/resources/data/theoreticalsciences/ts_glyphs/glyphs.json'))
for _f in glob.glob('src/main/resources/data/theoreticalsciences/ts_research/*.json'):
    _d = json.load(open(_f))
    if _d.get('grant_only'): continue
    if 'puzzle' not in _d: errs.append('research without puzzle: ' + os.path.basename(_f)); continue
    if 'form' not in _d['puzzle']: errs.append('research with an old-style puzzle: ' + os.path.basename(_f)); continue
    _pe, _pw = [], []
    _p2.check(os.path.basename(_f)[:-5], dict(_d['puzzle']), _gl, _pe, _pw)
    errs += _pe
# the Modonomicon codex: every entry's category, parents and lang keys must exist
_MB = 'src/main/resources/data/theoreticalsciences/modonomicon/books/quantum_codex'
_lang = json.load(open('tools/out/lang_full.json'))     # round 36: the Codex text lives there now
_cats = {os.path.basename(f)[:-5] for f in glob.glob(_MB + '/categories/*.json')}
_ents = {f.split('entries/')[1][:-5] for f in glob.glob(_MB + '/entries/*/*.json')}
for _f in glob.glob(_MB + '/entries/*/*.json'):
    _e = json.load(open(_f))
    if _e['category'].split(':')[1] not in _cats: errs.append(f'modonomicon: bad category in {_f}')
    for _p in _e.get('parents', []):
        if _p['entry'].split(':')[1] not in _ents: errs.append(f"modonomicon: missing parent {_p['entry']} in {_f}")
    for _pg in _e['pages']:
        for _k in ('text', 'title'):
            if _pg.get(_k) and _pg[_k] not in _lang: errs.append(f'modonomicon: missing lang {_pg[_k]}')
    if _e['name'] not in _lang: errs.append(f'modonomicon: missing lang {_e["name"]}')
# every controller must be reachable from outside its structure
import importlib.util as _ilu
_spec = _ilu.spec_from_file_location('controller_check', 'tools/controller_check.py'); _cc = _ilu.module_from_spec(_spec); _spec.loader.exec_module(_cc)
for _f in glob.glob('src/main/resources/assets/theoreticalsciences/multiblocks/*.json'):
    _ok, _c = _cc.check(json.load(open(_f)))
    if not _ok: errs.append(f'buried controller in {os.path.basename(_f)} at {_c}')
# round 22: no recipe lists the same ingredient twice (a slot holds any number)
import sys as _sys
_sys.path.insert(0, 'tools')
import round22 as _r22
for _f in glob.glob('src/main/resources/data/theoreticalsciences/recipe/**/*.json', recursive=True):
    for _d in _r22.duplicates(json.load(open(_f))):
        errs.append(f'duplicate ingredient in {os.path.relpath(_f, "src/main/resources/data/theoreticalsciences/recipe")}: {_d}')
# round 22: every research question with values that can vary still has a sensible answer in any world
import puzzle_check as _pc
for _f in glob.glob('src/main/resources/data/theoreticalsciences/ts_research/*.json'):
    _z = json.load(open(_f)).get('puzzle') or {}
    for _k, _v in (_z.get('vary') or {}).items():
        if _k not in _z.get('given', {}) or len(_v) != 3 or not (_v[2] > 0 and _v[1] >= _v[0]):
            errs.append(f'bad vary range {_k} in {_f}')
# round 27: every codex entry names a category that exists (one bad entry empties the whole Patchouli book), and no
# recipe asks for more energy than fits (a larger number fails to load the recipe)
for _book in glob.glob(A + '/patchouli_books/*/en_us'):
    _cats = {os.path.splitext(os.path.basename(c))[0] for c in glob.glob(_book + '/categories/*.json')}
    for _f in glob.glob(_book + '/entries/**/*.json', recursive=True):
        _c = json.load(open(_f)).get('category', '')
        if _c.split(':')[-1] not in _cats: errs.append(f'codex entry {os.path.relpath(_f, _book)} has no category {_c}')
for _f in glob.glob('src/main/resources/data/theoreticalsciences/recipe/**/*.json', recursive=True):
    if '/convergence/' in _f.replace(os.sep, '/'): continue      # (the Convergence Altar counts energy in a long)
    _e = json.load(open(_f)).get('energy', 0)
    if isinstance(_e, (int, float)) and not (0 <= _e <= 2147483647): errs.append(f'energy out of range in {_f}: {_e}')
# 1.3.8: every Codex page must say more than the name of its subject
import re as _re
_tse = json.load(open('src/main/resources/assets/theoreticalsciences/ts_codex/entries.json'))
for _k, _e in _tse.items():
    _t = _re.sub(r'\$\([^)]*\)', '', ' '.join(_p.get('text', '') for _p in _e.get('pages', []))).strip()
    if len(_t) < len(_e.get('title', '')) + 25: errs.append(f'codex node {_k} has no text beyond its name')
print('\n'.join(errs) if errs else 'ALL OK')
