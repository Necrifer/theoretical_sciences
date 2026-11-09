"""
1.5.1: every machine's console must draw a simulation of its own. Checks that each machine in MachineType is drawn by
MachineScenes, ProcessScenes or an effect scene in SimScenes (never the generic fallback pictures), that no two
ProcessScenes entries use the same vessel and processes, that titles are unique, and that every effect scene in
SimScenes belongs to one machine only.
Run:  python3 tools/check_scenes.py
"""
import os, re, sys, collections
HERE = os.path.dirname(os.path.abspath(__file__))
J = os.path.join(HERE, '..', 'src/main/java/com/theoreticalsciences')
mt = open(os.path.join(J, 'machine/MachineType.java')).read()
eff = {m[0].lower(): m[1] for m in re.findall(r'^\s+([A-Z_0-9]+)\s*\(.*?Effect\.(\w+)', mt, re.M)}
sim = open(os.path.join(J, 'client/screen/SimScenes.java')).read()
sim = sim[sim.index('static boolean draw'):sim.index('// ------------------------------------------------------------------ helpers')]
sim_cases = set(re.findall(r'case (\w+) ->', sim))
ms = open(os.path.join(J, 'client/screen/MachineScenes.java')).read()
ms_ids = set(re.findall(r'case "(\w+)"', ms))
ms_titles = dict(re.findall(r'Map\.entry\("(\w+)", "([^"]+)"\)', ms))
ps = open(os.path.join(J, 'client/screen/ProcessScenes.java')).read()
specs = re.findall(r'e\("(\w+)", "([^"]+)", V\.(\w+), C\.(\w+), C\.(\w+), (\d+)\);', ps)
ps_ids = {s[0] for s in specs}
bad = []
for mid, e in eff.items():
    if mid in ms_ids or mid in ps_ids:
        continue
    if e in sim_cases:
        continue
    bad.append((mid, e))
combos = collections.Counter((s[2], s[3], s[4]) for s in specs)
dupes = [c for c, n in combos.items() if n > 1]
titles = collections.Counter([s[1] for s in specs] + list(ms_titles.values()))
tdupes = [t for t, n in titles.items() if n > 1]
both = ps_ids & ms_ids
unknown = (ps_ids | ms_ids) - set(eff)
shared = collections.Counter(e for mid, e in eff.items() if mid not in ps_ids and mid not in ms_ids and e in sim_cases)
shared = {e: n for e, n in shared.items() if n > 1}
print(len(eff), 'machines;', len(ms_ids), 'own scenes,', len(ps_ids), 'process scenes,',
      len(eff) - len(ms_ids) - len(ps_ids), 'effect scenes')
for name, v in (('fall back to a generic picture', bad), ('share a vessel and processes', dupes), ('share a title', tdupes),
                ('are in both scene tables', both), ('are not machines', unknown), ('effect scenes used by several machines', shared)):
    if v:
        print('PROBLEM:', name, v)
sys.exit(1 if (bad or dupes or tdupes or both or unknown or shared) else 0)
