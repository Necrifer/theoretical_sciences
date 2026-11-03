"""Checks that every multiblock controller can be reached: the block in front of it (toward -Z, where the controller
faces) must be connected to the outside of the structure through non-solid cells."""
import json, glob, collections, sys, os

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..')


def check(m):
    L = m['layers']; H = len(L); D = len(L[0]); W = max(len(r) for l in L for r in l)
    ch = m.get('controller', 'C')
    cpos = None
    for y in range(H):
        for z in range(D):
            x = L[y][z].find(ch)
            if x >= 0: cpos = (x, y, z)
    cx, cy, cz = cpos

    def cell(x, y, z):
        if not (0 <= x < W and 0 <= y < H and 0 <= z < D): return ' '
        r = L[y][z]
        return r[x] if x < len(r) else ' '

    def solid(x, y, z):
        return cell(x, y, z) not in ' .'
    seen = {(-1, -1, -1)}; q = collections.deque(seen)
    while q:
        x, y, z = q.popleft()
        for dx, dy, dz in ((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1)):
            n = (x + dx, y + dy, z + dz)
            if n in seen or not (-1 <= n[0] <= W and -1 <= n[1] <= H and -1 <= n[2] <= D) or solid(*n): continue
            seen.add(n); q.append(n)
    return (cx, cy, cz - 1) in seen, cpos


if __name__ == '__main__':
    bad = 0
    for f in sorted(glob.glob(os.path.join(ROOT, 'src/main/resources/assets/theoreticalsciences/multiblocks/*.json'))):
        ok, c = check(json.load(open(f)))
        if not ok:
            bad += 1
            print('BURIED CONTROLLER', os.path.basename(f), c)
    print('controllers ok' if not bad else f'{bad} buried controllers')
    sys.exit(1 if bad else 0)
