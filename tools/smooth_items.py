"""
1.15.1: smoother animated items.

Every animated item texture was rendered at 40 steps per loop, then cut to every second step (20 frames, two ticks
each) by tools/trim_animations.py, so a spinning item turned 18 degrees between frames and looked jerky. Here each one
becomes 32 frames at two ticks each (a 3.2 s loop; the cost rules of trim_animations still hold), and each frame is
the average of two renders half a frame apart: a touch of motion blur that joins one frame to the next.

  * items whose builder still exists are re-rendered: the tools/items3d_* modules, and the 1.1.0 pipeline
    (gen_textures + anim + content.json, restored from git into a scratch folder: --old <dir>);
  * the rest (fluid cells added after 1.1.0, flat 2D items, a few special strips) are re-timed to 32 frames, each new
    frame cross-faded from the two old frames around it.

Run:  python3 tools/smooth_items.py --old <restored tools dir> [--only id ...] [--out dir] [--budget seconds]
Resumable: it records what is done in tools/out/smooth_done.json and skips those.
"""
import argparse, glob, json, os, subprocess, sys, time
import numpy as np
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
TEX = os.path.join(HERE, '..', 'src/main/resources/assets/theoreticalsciences/textures/item')
N = 32                 # frames kept
SUB = 2                # renders per frame (motion blur)
MODULES = [13, 14, 15, 16, 17, 171, 172, 180, 190, 1120, 1130, 1140, 1150]
DONE = os.path.join(HERE, 'out', 'smooth_done.json')
REDRAWN = {'boltzmann_brain', 'dodecaract_excavator', 'dynamic_singularity', 'penteract_lattice', 'simulated_universe', 'singularity_cluster'}


def save(frames, path):
    img = np.concatenate(frames, axis=0)
    Image.fromarray((np.clip(img, 0, 1) * 255).round().astype(np.uint8), 'RGBA').save(path)
    json.dump({'animation': {'frametime': 2, 'interpolate': False}}, open(path + '.mcmeta', 'w'))


def blur(phases):
    """N frames from N*SUB phases: each frame the mean of its SUB phases (colour weighted by alpha, so edges stay clean)."""
    out = []
    for f in range(N):
        ps = np.stack(phases[f * SUB:(f + 1) * SUB])
        a = ps[..., 3:4]
        rgb = (ps[..., :3] * a).sum(0) / np.maximum(a.sum(0), 1e-6)
        alpha = a.mean(0)
        alpha = np.where(alpha > 0.5, 1.0, np.where(alpha < 0.2, 0.0, alpha))     # pixel art: keep edges mostly hard
        out.append(np.concatenate([rgb, alpha], -1))
    return out


def retime(path):
    """For a strip with no builder: N frames cross-faded from the old ones (looping)."""
    im = np.asarray(Image.open(path).convert('RGBA')).astype(float) / 255
    w = im.shape[1]
    k = im.shape[0] // w
    if k < 2 or k >= N:
        return None
    old = [im[i * w:(i + 1) * w] for i in range(k)]
    out = []
    for f in range(N):
        x = f * k / N
        i, a = int(x), x - int(x)
        A, B = old[i % k], old[(i + 1) % k]
        al = A[..., 3:4] * (1 - a) + B[..., 3:4] * a
        rgb = (A[..., :3] * A[..., 3:4] * (1 - a) + B[..., :3] * B[..., 3:4] * a) / np.maximum(al, 1e-6)
        al = np.where(al > 0.5, 1.0, np.where(al < 0.2, 0.0, al))
        out.append(np.concatenate([rgb, al], -1))
    return out


# --------------------------------------------------------------------------- workers (run in their own process)
def work_module(mod, ids, outdir):
    sys.path.insert(0, HERE)
    import io, contextlib
    sys.argv = ['x']
    with contextlib.redirect_stdout(io.StringIO()):
        m = __import__('items3d_%d' % mod)
    for iid in ids:
        fr = m.frames(iid, n=N * SUB) if hasattr(m, 'frames') else __import__('items3d_171').frames(iid, n=N * SUB)
        save(blur(fr), os.path.join(outdir, iid + '.png'))
        print('done', iid, flush=True)


def work_old(old, ids, outdir):
    sys.path.insert(0, old)
    import gen_textures as G, anim, items3d
    jobs, groups = {}, {}
    for it in G.C['items']:
        cat = anim.CATEGORY.get(it['art'], 'device')
        jobs[it['id']] = (it['id'], it['art'], it['c1'], it['c2'], groups.get(cat, 0))
        groups[cat] = groups.get(cat, 0) + 1
    for fl in G.C['fluids']:
        jobs[fl['id'] + '_cell'] = ('@cell:' + fl['id'], 'gas' if fl['gas'] else 'liquid', fl['c1'], fl['c2'], 0)
    n = N * SUB
    for name in ids:
        iid, art, c1, c2, gi = jobs[name]
        fr = []
        if iid.startswith('@cell:'):
            fid = iid[6:]
            k = anim.h(fid) % 3
            for f in range(n):
                t = f / n
                res = items3d.b_cell(c1, c2, t, fluid_cell=True)
                view = anim.apply_motions(res[0], res[1], res[2], t, (('rock',), ('bob',), ('tumble',))[k], anim.h(fid))
                img = items3d.render(res[0], res[1], **view)
                anim.ov_glint(img, t, anim.h(fid), start=(anim.h(fid) % 5) / 5)
                if art == 'gas':
                    anim.ov_embers(img, t, anim.h(fid), items3d.H(c2), 5)
                fr.append(img)
        elif iid in items3d.ITEMS:
            fr = [anim.render_frame(iid, art, c1, c2, f / n, gi) for f in range(n)]
        else:
            print('flat', name, flush=True)
            continue
        save(blur(fr), os.path.join(outdir, name + '.png'))
        print('done', name, flush=True)


# --------------------------------------------------------------------------- plan
def plan(old):
    animated = sorted(os.path.basename(f)[:-11] for f in glob.glob(os.path.join(TEX, '*.png.mcmeta')))
    src = {}
    for mod in MODULES:
        p = os.path.join(HERE, 'out', 'mod_%d.json' % mod)
        if not os.path.exists(p):
            subprocess.run([sys.executable, '-c', '''
import sys, io, contextlib, json
sys.path.insert(0, %r); sys.argv = ['x']
with contextlib.redirect_stdout(io.StringIO()):
    m = __import__('items3d_%d')
B = getattr(m, 'B', None)
if B is None:
    import items3d_171 as P; B = P.B
json.dump(sorted(B), open(%r, 'w'))''' % (HERE, mod, p)], check=True)
        for i in json.load(open(p)):
            src[i] = ('module', mod)
    C = json.load(open(os.path.join(old, 'content.json')))
    oldset = {it['id'] for it in C['items']} | {f['id'] + '_cell' for f in C['fluids']}
    out = {}
    for a in animated:
        meta = json.load(open(os.path.join(TEX, a + '.png.mcmeta'))).get('animation', {})
        if 'frames' in meta:
            continue
        # fluid cells and a few items were redrawn after 1.1.0: the old pipeline would bring back the old art, so those are re-timed
        redrawn = a.endswith('_cell') or a in REDRAWN
        out[a] = src.get(a) or (('old', 0) if a in oldset and not redrawn else ('retime', 0))
    return out


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--old', required=True)
    ap.add_argument('--only', nargs='*')
    ap.add_argument('--out', default=TEX)
    ap.add_argument('--budget', type=float, default=1e9)
    a = ap.parse_args()
    os.makedirs(os.path.join(HERE, 'out'), exist_ok=True)
    P = plan(a.old)
    done = set(json.load(open(DONE))) if os.path.exists(DONE) and a.out == TEX else set()
    if a.out == TEX:                                 # and whatever earlier (interrupted) runs logged
        for f in glob.glob(os.path.join(HERE, 'out', 'smooth_*.log')):
            for line in open(f):
                if line.startswith('done ') or line.startswith('flat '):
                    done.add(line.split()[1])
    todo = [i for i in (a.only or sorted(P)) if i in P and i not in done]
    t0 = time.time()
    # the cheap ones first
    for i in [i for i in todo if P[i][0] == 'retime']:
        fr = retime(os.path.join(TEX, i + '.png'))
        if fr:
            save(fr, os.path.join(a.out, i + '.png'))
        done.add(i)
    groups = {}
    for i in todo:
        if P[i][0] != 'retime':
            groups.setdefault(P[i], []).append(i)
    procs = []
    queue = [(k, v[j:j + 4]) for k, v in groups.items() for j in range(0, len(v), 4)]     # small batches, so a budget can stop between them
    for (kind, mod), ids in queue:
        while len(procs) >= 2:
            procs = [p for p in procs if p.poll() is None]
            time.sleep(1)
        if time.time() - t0 > a.budget:
            break
        cmd = [sys.executable, '-c', 'import sys; sys.path.insert(0, %r); import smooth_items as S; S.%s(%r, %r, %r)' % (
            HERE, 'work_module' if kind == 'module' else 'work_old', mod if kind == 'module' else a.old, ids, a.out)]
        procs.append(subprocess.Popen(cmd, stdout=open(os.path.join(HERE, 'out', 'smooth_%s_%s_%s.log' % (kind, mod, ids[0])), 'a'),
                                      stderr=subprocess.STDOUT))
    for p in procs:
        p.wait()
    for f in glob.glob(os.path.join(HERE, 'out', 'smooth_*.log')):
        for line in open(f):
            if line.startswith('done ') or line.startswith('flat '):
                done.add(line.split()[1])
    flats = set()
    for f in glob.glob(os.path.join(HERE, 'out', 'smooth_*.log')):
        for line in open(f):
            if line.startswith('flat '):
                flats.add(line.split()[1])
    for i in flats:                                  # 2D items under the old pipeline: re-time them
        fr = retime(os.path.join(TEX, i + '.png'))
        if fr and a.out == TEX:
            save(fr, os.path.join(a.out, i + '.png'))
    if a.out == TEX:
        json.dump(sorted(done), open(DONE, 'w'))
    left = [i for i in P if i not in done]
    print('done', len(done), 'left', len(left), 'in %.0fs' % (time.time() - t0))


if __name__ == '__main__':
    main()
