"""
1.11.0 (Patch B): the 25 console windows 1.7.2 left wide, narrowed from the game's own physics.

1.7.2's tool (r172_windows.py) samples consoles offline, and could not for 25 recipes: their windows depend on the
machine's temperature (held by its coolant), on a plasma that settles while the machine runs, or on settings that
work only in a small region. The game's own solver (DemonSolver.reach, run by "/theoreticalsciences windows reach
<type>" on a server) samples all of that: every coolant, coil tiers 1 to 5, the temperature and state variables
settled as in play, random settings and a walk through the working settings next to the demon's own point. Its
output (ts_window_reach/<type>.json in the server folder) is read here.

The rules are 1.7.2's: a tier's window allows its share of what the console can reach (beyond physics a sixth, the
final tier a tenth, cosmic engineering a quarter); a threshold keeps its bound and gets a limit on its open side; a
band shrinks round the middle of the working settings that met the old window; the new window lies inside the old.
In place of the offline disturbance test, at least a tenth of the working settings that met the old window (and at
least three) must still meet the new one, or the window is widened step by step until they do.

Run:  python3 tools/r1110_windows.py <server>/ts_window_reach            (report)
      python3 tools/r1110_windows.py <server>/ts_window_reach apply
"""
import glob, json, math, os, sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import r172_windows as W
import tsgen as T

HERE = os.path.dirname(os.path.abspath(__file__))


def left_alone():
    """The recipes 1.7.2 could not narrow: (type, id) -> tier."""
    rows = json.load(open(os.path.join(HERE, 'r172_windows.json')))
    return {(r['type'], r['id']): r['tier'] for r in rows if r['new'] is None and r['note']}


def narrow(wins, samples, share):
    k_n = len(wins)
    S = [s[1:1 + k_n] for s in samples]
    P = [v for v in S if all(w['min'] <= v[k] <= w['max'] for k, w in enumerate(wins))]
    if len(P) < 3:
        return None, 'only %d of %d working settings meet the old window: left as it is' % (len(P), len(S))
    plan = []
    for k, w in enumerate(wins):
        vals = sorted(v[k] for v in S)
        lo, hi = vals[int(len(vals) * 0.02)], vals[max(0, int(len(vals) * 0.98) - 1)]
        log = lo > 0 and hi / lo > 50
        f = (lambda x: math.log10(max(x, 1e-300))) if log else (lambda x: x)
        g = (lambda x: 10.0 ** x) if log else (lambda x: x)
        span = f(hi) - f(lo)
        pv = sorted(v[k] for v in P)
        plan.append(dict(f=f, g=g, want=share * span, L=max(w['min'], lo), H=min(w['max'], hi), up=w['max'] >= hi, down=w['min'] <= lo,
                         mid=pv[len(pv) // 2], span=span))
    need = max(3, int(len(P) * 0.1))
    for widen in (1.0, 1.3, 1.7, 2.2, 3.0, 4.0, 6.0, 10.0):
        new = []
        for pl, w in zip(plan, wins):
            f, g, width = pl['f'], pl['g'], pl['want'] * widen
            if pl['span'] <= 0 or f(pl['H']) - f(pl['L']) <= width * 1.05 or (pl['up'] and pl['down']):
                new.append((w['min'], w['max']))
            elif pl['up']:
                new.append((w['min'], min(w['max'], g(f(pl['L']) + width))))
            elif pl['down']:
                new.append((max(w['min'], g(f(pl['H']) - width)), w['max']))
            else:
                c = min(max(f(pl['mid']), f(pl['L']) + width / 2), f(pl['H']) - width / 2)
                new.append((max(w['min'], g(c - width / 2)), min(w['max'], g(c + width / 2))))
        new = [(W.sig(a, False) if a != w['min'] else a, W.sig(b, True) if b != w['max'] else b) for (a, b), w in zip(new, wins)]
        new = [(max(a, w['min']), min(b, w['max'])) for (a, b), w in zip(new, wins)]
        hit = [v for v in P if all(a <= v[k] <= b for k, (a, b) in enumerate(new))]
        if len(hit) >= need:
            return new, ('%d of %d kept' % (len(hit), len(P))) + (', share x%.1f' % widen if widen > 1 else '')
    return None, 'no narrower window keeps enough working settings'


ORIG = json.load(open(os.path.join(HERE, 'r1110_windows_orig.json')))


def main():
    src, apply = sys.argv[1], 'apply' in sys.argv
    todo = left_alone()
    reach = {}
    for f in glob.glob(os.path.join(src, '*.json')):
        for rid, o in json.load(open(f)).items():
            reach[rid] = o
    changed = 0
    for (rtype, rid), tier in sorted(todo.items()):
        key = 'theoreticalsciences:%s/%s' % (rtype, rid)
        if key not in reach:
            print('E%d %-24s %-40s not sampled' % (tier, rtype, rid))
            continue
        path = os.path.join(T.D, 'recipe', rtype, rid + '.json')
        r = json.load(open(path, encoding='utf-8'))
        wins = r['physics']['windows']
        for w, o in zip(wins, ORIG.get(rtype + '/' + rid, [])):     # (run again: start from the windows as they were at 1.10.0)
            w['min'], w['max'] = o[0], o[1]
        new, note = narrow(wins, reach[key]['samples'], W.SHARE[tier])
        if new is None:
            print('E%d %-24s %-40s %s' % (tier, rtype, rid, note))
            continue
        if all((a, b) == (w['min'], w['max']) for (a, b), w in zip(new, wins)):
            print('E%d %-24s %-40s already narrow for what can be reached  %s' % (tier, rtype, rid, note))
            continue
        print('E%d %-24s %-40s %s  %s' % (tier, rtype, rid, '; '.join('%s %g..%g -> %g..%g' % (w['q'], w['min'], w['max'], a, b) for w, (a, b) in zip(wins, new)), note))
        changed += 1
        if apply:
            for w, (a, b) in zip(wins, new):
                w['min'], w['max'] = a, b
            T.dump(path, r)
            open(path, 'a').write('\n')
    print('narrowed:', changed, 'of', len(todo))


if __name__ == '__main__':
    main()
