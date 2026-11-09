"""Shared helpers for the data generators (taken from round140.py): research, consoles, casings, items, recipes,
multiblocks, tags, the Codex and the English text. Each generator fills LANG and calls finish() at the end."""

import json, math, os, random, re, sys

HERE = os.path.dirname(os.path.abspath(__file__))
RES = os.path.join(HERE, '..', 'src/main/resources')
A = os.path.join(RES, 'assets/theoreticalsciences')
D = os.path.join(RES, 'data/theoreticalsciences')
MC = os.path.join(RES, 'data/minecraft')
NS = 'theoreticalsciences'
LANG = {}


def ts(x):
    return x if ':' in x else NS + ':' + x


def dump(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8') as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)


def edit_json(path, fn):
    raw = open(path, encoding='utf-8').read()
    d = json.loads(raw)
    fn(d)
    ind = 1 if raw.startswith('{\n "') or raw.startswith('[\n "') else 2
    with open(path, 'w', encoding='utf-8') as f:
        json.dump(d, f, indent=ind, ensure_ascii=('\\u' in raw[:200000]))


def tag_add(path, values):
    if not os.path.exists(path):
        dump(path, {'replace': False, 'values': []})

    def fn(d):
        for v in values:
            if v not in d['values']:
                d['values'].append(v)
    edit_json(path, fn)


# ====================================================================== physics check
FUNCS = {'sqrt': math.sqrt, 'cbrt': lambda x: math.copysign(abs(x) ** (1 / 3), x), 'ln': math.log, 'log10': math.log10, 'exp': math.exp,
         'sin': math.sin, 'cos': math.cos, 'tan': math.tan, 'atan': math.atan, 'tanh': math.tanh, 'abs': abs,
         'pos': lambda x: max(0.0, x), 'step': lambda x: 1.0 if x >= 0 else 0.0, 'min': min, 'max': max,
         'clamp': lambda x, a, b: max(a, min(b, x))}


def pyexpr(e):
    return e.replace('^', '**')


def evaluate(model, params, rvars, d=0.0):
    env = dict(FUNCS)
    env.update({'d': d, 'T': 300.0, 'T_amb': 300.0, 'on': 1.0, 'size': 1.0, 'sky': 1.0, 'night': 0.0, 'sun': 0.5, 'moon': 0.5, 'rain': 0.0,
                'alt': 64.0, 'rfi': 0.0, 'coil': 1.0, 'emitter': 1.0, 'oc': 0.0, 'par': 1.0, 'thr': 1.0, 'T_c': 300.0, 'sc_Tc': 0.0, 'sc_B0': 0.0})
    env.update(rvars)
    env.update(params)
    for c in model.get('consts', []):
        env[c['id']] = eval(pyexpr(c['expr']), {}, env)
    for x in model['derived']:
        env[x['id']] = eval(pyexpr(x['expr']), {}, env)
    ok = all(eval(pyexpr(c['expr']), {}, env) > 0 for c in model['constraints'])
    rate = eval(pyexpr(model['rate']), {}, env)
    return env, ok, rate


def check_reachable(machine, model, recipe):
    rv = dict(model.get('recipe_vars', {}))
    rv.update(recipe.get('physics', {}).get('vars', {}))
    wins = recipe.get('physics', {}).get('windows', [])
    rng = random.Random(1400)
    ps = model['params']
    best = None
    for i in range(60000):
        p = {}
        for q in ps:
            if i == 0:
                p[q['id']] = q['default']
            else:
                v = rng.uniform(q['min'], q['max'])
                p[q['id']] = round(v / q['step']) * q['step']
        try:
            env, ok, rate = evaluate(model, p, rv)
        except (ValueError, ZeroDivisionError, OverflowError):
            continue
        if not ok or rate <= 0:
            continue
        if all(w['min'] <= env[w['q']] <= w['max'] for w in wins):
            return p, {w['q']: env[w['q']] for w in wins}, rate
    raise SystemExit('UNREACHABLE: %s recipe %s windows %s' % (machine, recipe.get('_id'), wins))


# ====================================================================== content: research
RESEARCH = {}


def research(rid, title, x, y, insight, requires, cost, time, desc, short, puzzle, icon, parent):
    RESEARCH[rid] = dict(title=title, x=x, y=y, insight=insight, requires=[ts(r) for r in requires],
                         cost=[{'item': ts(i), 'count': n} for i, n in cost], time=time, description=desc, icon=ts(icon), puzzle=puzzle)
    LANG['research.%s.%s' % (NS, rid)] = title
    LANG['research.%s.%s.desc' % (NS, rid)] = desc
    LANG['advancements.%s.research.%s.short' % (NS, rid)] = short
    adv = {'display': {'icon': {'id': ts(icon)}, 'title': {'translate': 'research.%s.%s' % (NS, rid)},
                       'description': {'translate': 'advancements.%s.research.%s.short' % (NS, rid)}, 'frame': 'task', 'show_toast': True,
                       'announce_to_chat': False, 'hidden': False},
           'parent': ts('research/' + parent), 'criteria': {'unlocked': {'trigger': 'minecraft:impossible'}}, 'requirements': [['unlocked']]}
    dump(os.path.join(D, 'advancement/research', rid + '.json'), adv)
    dump(os.path.join(D, 'ts_research', rid + '.json'), RESEARCH[rid])


# ====================================================================== machines
MACHINES = {}   # id -> dict(kind, name, lore, physics, game, recipe(crafting))


def machine(mid, name, lore, game, tier, physics, plang, crafting, multiblock=False):
    MACHINES[mid] = dict(name=name, lore=lore, multiblock=multiblock)
    LANG['block.%s.%s' % (NS, mid)] = name
    LANG['block.%s.%s.lore' % (NS, mid)] = lore
    dump(os.path.join(D, 'ts_physics', mid + '.json'), physics)
    for k, v in plang.items():
        LANG['physics.%s.%s.%s' % (NS, mid, k)] = v
    LANG['physics.%s.%s.title' % (NS, mid)] = name

    def g(dct):
        dct[mid] = {'game': game, 'tier': tier}
    edit_json(os.path.join(D, 'calibration/games.json'), g)
    dump(os.path.join(D, 'recipe/crafting', mid + '.json'), crafting)
    # blockstate and models (machines face the player; on while running)
    var = {}
    for facing, yrot in (('north', 0), ('east', 90), ('south', 180), ('west', 270)):
        for act in (False, True):
            m = {'model': '%s:block/machine/%s_%s' % (NS, mid, 'on' if act else 'off')}
            if yrot:
                m['y'] = yrot
            var['active=%s,facing=%s' % ('true' if act else 'false', facing)] = m
    dump(os.path.join(A, 'blockstates', mid + '.json'), {'variants': var})
    for st in ('on', 'off'):
        dump(os.path.join(A, 'models/block/machine', '%s_%s.json' % (mid, st)),
             {'parent': 'minecraft:block/orientable', 'textures': {'top': '%s:block/machine/%s_top' % (NS, mid),
                                                                     'front': '%s:block/machine/%s_front_%s' % (NS, mid, st),
                                                                     'side': '%s:block/machine/%s_side' % (NS, mid)}})
    dump(os.path.join(A, 'models/item', mid + '.json'), {'parent': '%s:block/machine/%s_on' % (NS, mid)})
    loot(mid)


def loot(bid):
    dump(os.path.join(D, 'loot_table/blocks', bid + '.json'),
         {'type': 'minecraft:block', 'pools': [{'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': ts(bid)}],
                                                'conditions': [{'condition': 'minecraft:survives_explosion'}]}]})


def extreme(result, count, pattern, key):
    return {'type': ts('extreme_shaped'), 'pattern': pattern, 'key': {k: {'item': ts(v)} for k, v in key.items()},
            'result': {'id': ts(result), 'count': count}}


P_CONSOLE = ['AAAFFFAAA', 'AAAFFFAAA', 'AAAFFFAAA', 'FFFLLLFFF', 'FFFLNLFFF', 'FFFLLLFFF', 'AAAFFFAAA', 'AAAFFFAAA', 'AAAFFFAAA']
P_PART = ['OOOOOOOOO', 'OOOOOOOOO', 'OOOOOOOOO', 'OOOIIIOOO', 'OOOIIIOOO', 'OOOIIIOOO', 'OOOOOOOOO', 'OOOOOOOOO', 'OOOOOOOOO']
P_CTRL = ['FFFTTTFFF', 'FFFTTTFFF', 'FFFTTTFFF', 'MMMBBBMMM', 'MMMBBBMMM', 'MMMBBBMMM', 'FFFTTTFFF', 'FFFTTTFFF', 'FFFTTTFFF']


def par(pid, unit, lo, hi, step, default):
    return {'id': pid, 'unit': unit, 'min': lo, 'max': hi, 'step': step, 'default': default}


def der(did, unit, expr, formula):
    return {'id': did, 'unit': unit, 'expr': expr, 'formula': formula}


def con(cid, kind, expr, formula):
    return {'id': cid, 'kind': kind, 'expr': expr, 'formula': formula}


def model(params, rvars, derived, constraints, rate, rate_formula, rate_max=1.5, heat='0', idle='0'):
    return {'params': params, 'recipe_vars': rvars, 'consts': [], 'states': [], 'derived': derived, 'constraints': constraints,
            'rate': rate, 'rate_formula': rate_formula, 'heat': heat, 'idle_heat': idle, 'quench_energy': '0', 'kappa': 40,
            'heat_capacity': 60, 'timescale': 1, 'rate_max': rate_max}


# ====================================================================== casings, blocks and items
def simple_block(bid, name, lore, found=None, light=False):
    dump(os.path.join(A, 'blockstates', bid + '.json'), {'variants': {'': {'model': '%s:block/%s' % (NS, bid)}}})
    dump(os.path.join(A, 'models/block', bid + '.json'), {'parent': 'minecraft:block/cube_all', 'textures': {'all': '%s:block/%s' % (NS, bid)}})
    dump(os.path.join(A, 'models/item', bid + '.json'), {'parent': '%s:block/%s' % (NS, bid)})
    loot(bid)
    LANG['block.%s.%s' % (NS, bid)] = name
    LANG['block.%s.%s.lore' % (NS, bid)] = lore
    if found:
        LANG['block.%s.%s.found' % (NS, bid)] = found


def casing(cid, name, lore):
    dump(os.path.join(A, 'blockstates', cid + '.json'), {'variants': {
        'formed=false,moving=false': {'model': '%s:block/%s' % (NS, cid)}, 'formed=false,moving=true': {'model': '%s:block/%s' % (NS, cid)},
        'formed=true,moving=false': {'model': '%s:block/%s_formed' % (NS, cid)}, 'formed=true,moving=true': {'model': '%s:block/moving_hidden' % NS}}})
    for suf in ('', '_formed'):
        dump(os.path.join(A, 'models/block', cid + suf + '.json'), {'parent': 'minecraft:block/cube_all', 'textures': {'all': '%s:block/%s%s' % (NS, cid, suf)}})
    dump(os.path.join(A, 'models/item', cid + '.json'), {'parent': '%s:block/%s' % (NS, cid)})
    loot(cid)
    LANG['block.%s.%s' % (NS, cid)] = name
    LANG['block.%s.%s.lore' % (NS, cid)] = lore


def item(iid, name, lore, found=None):
    dump(os.path.join(A, 'models/item', iid + '.json'), {'parent': 'minecraft:item/generated', 'textures': {'layer0': '%s:item/%s' % (NS, iid)}})
    LANG['item.%s.%s' % (NS, iid)] = name
    LANG['item.%s.%s.lore' % (NS, iid)] = lore
    if found:
        LANG['item.%s.%s.found' % (NS, iid)] = found


# ====================================================================== machine recipes (each checked against its physics)
RECIPES = []


def mrec(rtype, rid, ins, fins, outs, fouts, energy, dur, rsearch, vars_, windows, machine_id, extra=None):
    r = {'type': ts(rtype)}
    if ins:
        r['item_inputs'] = [{'item': ts(i), 'count': n} for i, n in ins]
    if fins:
        r['fluid_inputs'] = [{'fluid': ts(f), 'amount': n} for f, n in fins]
    if outs:
        r['item_outputs'] = [{'id': ts(i), 'count': n} for i, n in outs]
    if fouts:
        r['fluid_outputs'] = [{'id': ts(f), 'amount': n} for f, n in fouts]
    r['energy'] = energy
    r['duration'] = dur
    r['research'] = ts(rsearch)
    r['unscaled'] = True
    phys = {}
    if vars_:
        phys['vars'] = vars_
    if windows:
        phys['windows'] = [{'q': q, 'min': a, 'max': b} for q, a, b in windows]
    if phys:
        r['physics'] = phys
    if extra:
        r.update(extra)
    RECIPES.append((machine_id, rid, r))
    dump(os.path.join(D, 'recipe', rtype, rid + '.json'), r)


def write_mb(mid, layers, key, ctrl_char='C', extra=None):
    d = {'controller_block': ts(mid), 'controller': ctrl_char, 'layers': layers, 'key': {k: [ts(x) for x in v] for k, v in key.items()}, 'tiers': {}}
    if extra:
        d.update(extra)
    n = sum(row.count(ch) for layer in layers for row in layer for ch in key) + 1
    dump(os.path.join(A, 'multiblocks', mid + '.json'), d)
    return n


def grid(w, h, dpt):
    return [[[' '] * w for _ in range(dpt)] for _ in range(h)]


def rows(g):
    return [[''.join(r) for r in layer] for layer in g]


PORT = ['interface_port']
MB_COUNTS = {}

# ====================================================================== the Codex
def codex(entries, tab_nodes):
    def fe(E):
        for k, v in entries.items():
            E[k] = v
    edit_json(os.path.join(A, 'ts_codex/entries.json'), fe)
    for tab, nodes in tab_nodes.items():
        def ft(T, nodes=nodes):
            mine = {n['entry'] for n in nodes}
            T['nodes'] = [n for n in T['nodes'] if n['entry'] not in mine]
            taken = {(n['x'], n['y']) for n in T['nodes']}
            for n in nodes:
                n = dict(n)
                while (n['x'], n['y']) in taken:          # never on top of another node: step right
                    n['x'] += 2
                taken.add((n['x'], n['y']))
                T['nodes'].append(n)
        edit_json(os.path.join(A, 'ts_codex', tab + '.json'), ft)


def spot(items, text, title=None):
    p = {'type': 'spotlight', 'items': [ts(i) for i in items], 'text': text}
    if title:
        p['title'] = title
    return p


def text(t):
    return {'type': 'text', 'text': t}


def op_pages(mid, intro, sets, disturb, limits, rate, eqs):
    return [text(intro), text('$(thing)Set$(): %s.$(br2)$(thing)Disturbance$(): %s' % (sets, disturb)),
            text('$(thing)Limits$(): %s$(br2)$(thing)Rate$(): %s' % (limits, rate)), text('$(thing)Equations$()$(br)' + '$(br)'.join(eqs))]


def regions(tab, label, y0, y1):
    def f(T):
        if any(r['label'] == label for r in T['regions']):
            return
        tier = max(r['tier'] for r in T['regions']) + 1
        T['regions'].append({'tier': tier, 'label': label, 'x0': -1.5, 'x1': 125.5, 'y0': y0, 'y1': y1})
    edit_json(os.path.join(A, 'ts_codex', tab + '.json'), f)




def finish():
    def lang(L):
        L.update(LANG)
    edit_json(os.path.join(A, 'lang/en_us.json'), lang)


def verify_recipes():
    for mid, rid, r in RECIPES:
        m = json.load(open(os.path.join(D, 'ts_physics', mid + '.json'), encoding='utf-8'))
        r = dict(r)
        r['_id'] = rid
        p, got, rate = check_reachable(mid, m, r)
        print('  ok %-30s %-32s %s rate %.2f' % (mid, rid, {k: round(v, 3) for k, v in got.items()}, rate))
