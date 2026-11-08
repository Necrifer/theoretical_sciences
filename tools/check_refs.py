"""Checks that every theoreticalsciences: id named by the given data files is a real item, block or fluid of the mod,
and that no two Codex nodes in one tab sit on the same spot."""
import json, os, re, sys, glob, collections
HERE = os.path.dirname(os.path.abspath(__file__))
SRC = os.path.join(HERE, '..', 'src/main')
J = os.path.join(SRC, 'java/com/theoreticalsciences')
known = set()
for f in glob.glob(J + '/**/*.java', recursive=True):
    s = open(f, encoding='utf-8').read()
    known |= set(re.findall(r'(?:ItemDef|BlockDef|FluidDef)\("([a-z0-9_]+)"', s))
    known |= set(re.findall(r'register(?:Block|Item)?\("([a-z0-9_]+)"', s))
    known |= set(re.findall(r'ITEMS\.register\("([a-z0-9_]+)"', s))
mt = open(J + '/machine/MachineType.java').read()
known |= {m.lower() for m in re.findall(r'^\s+([A-Z0-9_]+)\s+\("', mt, re.M)}
# fluids are referenced by id and by their cells
known |= {k + '_cell' for k in list(known)} | {k + '_bucket' for k in list(known)}
bad = collections.Counter()
files = sys.argv[1:]
for f in files:
    s = open(f, encoding='utf-8').read()
    for m in re.findall(r'"theoreticalsciences:([a-z0-9_]+)"', s):
        if m not in known and not os.path.exists(os.path.join(SRC, 'resources/data/theoreticalsciences/ts_research', m + '.json')):
            bad[(os.path.relpath(f, SRC), m)] += 1
for (f, m), n in sorted(bad.items()):
    print('UNKNOWN', m, 'in', f)
T = os.path.join(SRC, 'resources/assets/theoreticalsciences/ts_codex')
for tab in glob.glob(T + '/*.json'):
    d = json.load(open(tab, encoding='utf-8'))
    if not isinstance(d, dict) or 'nodes' not in d: continue
    pos = collections.defaultdict(list)
    for n in d['nodes']: pos[(n['x'], n['y'])].append(n['entry'])
    for p, es in pos.items():
        if len(es) > 1: print('OVERLAP', os.path.basename(tab), p, es)
print('checked', len(files), 'files;', len(bad), 'unknown ids')
