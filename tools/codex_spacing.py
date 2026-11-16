"""
1.14.1: no two Codex nodes closer than two grid steps (one step apart, their icons overlap).
Nodes are kept in their order in the tab file; a node too close to one before it moves to the nearest clear spot.

Run:  python3 tools/codex_spacing.py [--check]
"""
import glob, json, os, sys

HERE = os.path.dirname(os.path.abspath(__file__))
TABS = os.path.join(HERE, '..', 'src/main/resources/assets/theoreticalsciences/ts_codex')


def clear(x, y, placed):
    return all(max(abs(x - a), abs(y - b)) >= 2 for a, b in placed)


def nearest(x, y, placed):
    for r in range(1, 40):
        cands = [(x + dx, y + dy) for dx in range(-r, r + 1) for dy in range(-r, r + 1) if max(abs(dx), abs(dy)) == r]
        cands.sort(key=lambda p: (abs(p[1] - y), abs(p[0] - x), p[0] < x))      # same row first, then rightward
        for p in cands:
            if p[0] >= 0 and p[1] >= 0 and clear(p[0], p[1], placed):
                return p
    raise SystemExit('no room near %s,%s' % (x, y))


def space(nodes):
    placed, moved = [], []
    for n in nodes:
        if not clear(n['x'], n['y'], placed):
            nx, ny = nearest(n['x'], n['y'], placed)
            moved.append((n['entry'], (n['x'], n['y']), (nx, ny)))
            n['x'], n['y'] = nx, ny
        placed.append((n['x'], n['y']))
    return moved


if __name__ == '__main__':
    check = '--check' in sys.argv
    total = 0
    for f in sorted(glob.glob(os.path.join(TABS, '*.json'))):
        if f.endswith('entries.json'):
            continue
        raw = open(f, encoding='utf-8').read()
        T = json.loads(raw)
        if 'nodes' not in T:
            continue
        moved = space(T['nodes'])
        total += len(moved)
        for m in moved:
            print('%-16s %-48s %s -> %s' % (os.path.basename(f), m[0], m[1], m[2]))
        if moved and not check:
            indent = 1 if raw.startswith('{\n "') else 2
            json.dump(T, open(f, 'w', encoding='utf-8'), ensure_ascii=False, indent=indent)
    print('nodes too close:', total, '(checked only)' if check else '(moved)')
    if check and total:
        sys.exit(1)
