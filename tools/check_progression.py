"""
1.7.1: the progression check. A machine's recipes can only be run once the machine exists: its controller has been
crafted and, for a multiblock, every block of its structure can be obtained. This walks all recipes outward from what
the world gives (anything no recipe makes, loot, vanilla) and prints every item that can never be reached: a loop, where
a thing needs itself (a casing made in the structure it builds, a material that needs the tier above it).

Run:  python3 tools/check_progression.py [--why item_id ...]      (exit code 1 if anything is unreachable)
"""
import glob, json, os, re, sys, collections

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import check_costs as cc

HERE = os.path.dirname(os.path.abspath(__file__))
JAVA = os.path.join(HERE, '..', 'src/main/java/com/theoreticalsciences/machine/MachineType.java')
MB = os.path.join(HERE, '..', 'src/main/resources/assets/theoreticalsciences/multiblocks')
NS = cc.NS
WORLD = ['arbor_heartwood',        # a block the world grows, which only drops itself
         'proof_of_everything',    # dropped by the Alpha and Omega
         'omnipotence_shard']      # nine are given back when the Omnipotence Core is sundered

# what the cost check took as rare world finds at 1.7.0 (dimension fluids, drops, and a few old loops a player enters
# from outside): given here too, by name, so that a new loop is never excused
GIVEN = ['fluid:primordial_law', 'aleph_null_shard', 'fluid:schrodinger_limit', 'absolute_infinity', 'fluid:residual_starlight', 'cosmic_processor',
         'fluid:xenoplasm', 'fluid:relativity_breakdown', 'fluid:reheating_plasma', 'cosmic_flux_coupler', 'law_crystal', 'fluid:false_vacuum',
         'matured_antibody', 'memory_cell']


def machines():
    """recipe type -> list of (machine id, multiblock id or None)"""
    out = collections.defaultdict(list)
    for m in re.finditer(r'^\s+([A-Z0-9_]+)\s*\("([a-z0-9_]+)",\s*\d+,\s*\d+,\s*\d+,\s*\d+,\s*(?:true|false),\s*(null|"[a-z0-9_]+")', open(JAVA).read(), re.M):
        out[m.group(2)].append((m.group(1).lower(), None if m.group(3) == 'null' else m.group(3).strip('"')))
    return out


def structure(mb):
    """Blocks a structure needs: for each character used, its first (cheapest) alternative."""
    d = json.load(open(os.path.join(MB, mb + '.json')))
    used = {ch for layer in d['layers'] for row in layer for ch in row}
    return {v[0] for k, v in d['key'].items() if k in used}


def extra():
    """Recipes the cost check does not read: pumps and scoops with no inputs, and the Convergence Altar's."""
    out = []
    for f in glob.glob(os.path.join(cc.D, 'recipe', '**', '*.json'), recursive=True):
        r = json.load(open(f, encoding='utf-8'))
        if r.get('neoforge:conditions'):
            continue
        rel = os.path.relpath(f, cc.D)
        if 'pedestals' in r and 'result' in r:
            ins = [(cc.norm(i['item']), 1) for i in [r.get('core', {})] + r['pedestals'] if 'item' in i]
            out.append(dict(file=rel, type=r['type'], ins=ins, fins=[], outs=[(cc.norm(r['result']['id']), 1)], fouts=[], extra=0))
        elif 'fluid_outputs' in r and not any(k in r for k in ('item_inputs', 'item_outputs', 'fluid_inputs')):
            out.append(dict(file=rel, type=r['type'], ins=[], fins=[], outs=[], fouts=[(cc.norm(o['id']), o.get('amount', 1000)) for o in r['fluid_outputs']], extra=0))
    return out


def core_forge():
    """The Core Forge's recipes, which are written in code (transcend/CoreForge.java)."""
    src = open(os.path.join(HERE, '..', 'src/main/java/com/theoreticalsciences/transcend/CoreForge.java'), encoding='utf-8').read()
    out = []
    for m in re.finditer(r'new Recipe\("(\w+)", i\("(\w+)", (\d+)\), List\.of\((.*?)\)\)[,)]', src, re.S):
        ins = [(NS + a, max(1, int(n))) for a, n in re.findall(r'i\("(\w+)", (\d+)\)', m.group(4))]
        out.append(dict(file='CoreForge.java:' + m.group(1), type='core_forge', ins=ins, fins=[], outs=[(NS + m.group(2), int(m.group(3)))], fouts=[], extra=0))
    return out


def recipes():
    """Every recipe, with its catalysts counted as inputs (a catalyst has to exist before the recipe can run)."""
    recs = cc.load() + extra() + core_forge()
    for r in recs:
        try:
            j = json.load(open(os.path.join(cc.D, r['file']), encoding='utf-8'))
        except Exception:
            continue
        r['ins'] = list(r['ins']) + [(cc.norm(c['item']), 1) for c in j.get('catalysts', []) if 'item' in c]
    return recs


def closure(recs, ignore=lambda r, k: False):
    """What can be reached, and for each thing the recipe that first reached it. ignore(recipe, input) drops an input."""
    mach = machines()
    made = {k for r in recs for k, _ in r['outs']} | {'fluid:' + k for r in recs for k, _ in r['fouts']}
    can = {(('fluid:' + NS + b[6:]) if b.startswith('fluid:') else NS + b) for b in WORLD + GIVEN}
    for f in glob.glob(os.path.join(cc.D, 'loot_table', '**', '*.json'), recursive=True):
        own = cc.norm(os.path.basename(f)[:-5]) if os.sep + 'blocks' + os.sep in f else None
        for m in re.findall(r'"name":\s*"([a-z0-9_:]+)"', open(f).read()):
            if cc.norm(m) != own:                          # a block dropping itself gives nothing new
                can.add(cc.norm(m))
    first = {}
    struct = {}

    def have(k):
        return k not in made or k in can or not k.replace('fluid:', '').startswith(NS)       # vanilla and other mods: given

    def needs(rtype):
        """What has to exist for a recipe of this type to run: the first machine of that type that can be built."""
        if rtype.split(':')[-1] == 'empyrean':           # 1.8.0: the Empyrean Assembly Table has to exist first
            return [NS + 'empyrean_assembly_table'] if have(NS + 'empyrean_assembly_table') else None
        ms = mach.get(rtype.split(':')[-1])
        if not ms:
            return []                                      # crafting, the Extreme table, the altar: no machine of ours to build first
        for mid, mb in ms:
            if mb is not None and mb not in struct:
                struct[mb] = structure(mb)
            need = [NS + mid] + (sorted(struct[mb]) if mb else [])
            if all(have(b) for b in need):
                return need
        return None

    changed = True
    while changed:
        changed = False
        for r in recs:
            need = needs(r['type'])
            if need is None:
                continue
            ins = [i for i, _ in r['ins'] if not i.startswith('#') and not ignore(r, i)] + ['fluid:' + i for i, _ in r['fins']]
            if all(have(i) for i in ins):
                for k in [k for k, _ in r['outs']] + ['fluid:' + k for k, _ in r['fouts']]:
                    if k not in can:
                        can.add(k)
                        first[k] = (r, ins + need)
                        changed = True
    return can, first, made, have, mach


def used(first, targets):
    """Everything the first-found way of making the targets goes through (the targets included)."""
    seen = set()
    todo = list(targets)
    while todo:
        k = todo.pop()
        if k in seen:
            continue
        seen.add(k)
        if k in first:
            todo += first[k][1]
    return seen


def slots():
    """recipe type -> the most item and fluid inputs any machine of that type has (MachineType's own arithmetic)."""
    out = {}
    for m in re.finditer(r'^\s+([A-Z0-9_]+)\s*\("([a-z0-9_]+)",\s*(\d+),\s*(\d+),\s*\d+,\s*\d+,\s*(true|false),\s*(null|"[a-z0-9_]+")', open(JAVA).read(), re.M):
        rt, n, f, gen, mb = m.group(2), int(m.group(3)), int(m.group(4)), m.group(5) == 'true', m.group(6) != 'null'
        if mb and not gen:
            it, fl = 16, 16
        elif gen:
            it = max(n, 6 if rt == 'stellar_thrust' else 8 if rt == 'dyson' else 0) - (2 if rt == 'dyson' else 0)     # the Dyson Array's second and third slots are its swarm's
            fl = f
        else:
            it = n if n >= 6 else min(6, max(n + 2, 4))
            fl = f if f >= 3 else f + 1
        a, b = out.get(rt, (0, 0))
        out[rt] = (max(a, it), max(b, fl))
    return out


def too_big():
    """Recipes with more inputs than their machine has slots."""
    sl = slots()
    bad = []
    for f in glob.glob(os.path.join(cc.D, 'recipe', '*', '*.json')):
        rt = os.path.basename(os.path.dirname(f))
        if rt not in sl:
            continue
        r = json.load(open(f, encoding='utf-8'))
        ni, nf = len(r.get('item_inputs', [])), len(r.get('fluid_inputs', []))
        if ni > sl[rt][0] or nf > sl[rt][1]:
            bad.append('%s/%s: %d items, %d fluids; the machine has %d and %d' % (rt, os.path.basename(f)[:-5], ni, nf, sl[rt][0], sl[rt][1]))
    return bad


def main():
    recs = recipes()
    can, first, made, have, mach = closure(recs)
    lost = sorted(k for k in made if k not in can and k.replace('fluid:', '').startswith(NS))
    prod = collections.defaultdict(list)
    for r in recs:
        for k in [k for k, _ in r['outs']] + ['fluid:' + k for k, _ in r['fouts']]:
            prod[k].append(r)

    def why(k, d=0, seen=None):
        seen = set() if seen is None else seen
        if k in seen or d > 6:
            return
        seen.add(k)
        for r in prod.get(k, [])[:2]:
            t = r['type'].split(':')[-1]
            miss = [i for i, _ in r['ins'] if not have(i) and not i.startswith('#')] + ['fluid:' + i for i, _ in r['fins'] if not have('fluid:' + i)]
            mm = []
            for mid, mb in mach.get(t, []):
                if not have(NS + mid):
                    mm.append(NS + mid)
                if mb:
                    mm += [b for b in structure(mb) if not have(b)]
            print('  ' * d + k.split(':')[-1], '<-', r['file'], '| missing inputs', [m.split(':')[-1] for m in miss], '| machine needs', [m.split(':')[-1] for m in mm])
            for m in (miss + mm)[:3]:
                why(m, d + 1, seen)

    if '--why' in sys.argv:
        for a in sys.argv[sys.argv.index('--why') + 1:]:
            why('fluid:' + NS + a[6:] if a.startswith('fluid:') else NS + a)
    print('items and fluids with a recipe: %d; reachable: %d; unreachable: %d' % (len(made), len(made) - len(lost), len(lost)))
    for k in lost[:60]:
        print('  UNREACHABLE', k)
    if '--save' in sys.argv:
        json.dump(lost, open(os.path.join(HERE, 'progression_baseline.json'), 'w'), indent=0)
    base = os.path.join(HERE, 'progression_baseline.json')
    known = set(json.load(open(base))) if os.path.exists(base) else set()
    new = [k for k in lost if k not in known]
    if new:
        print('NEW since the baseline:', len(new))
    big = too_big()
    for b in big:
        print('  TOO MANY INPUTS', b)
    print('recipes with more inputs than their machine has slots:', len(big))
    sys.exit(1 if new or big else 0)


if __name__ == '__main__':
    main()
