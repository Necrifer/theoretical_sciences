"""Round 36: keeps every animated texture cheap to run (the audit's biggest client cost).
- no "interpolate": vanilla blends interpolated frames on the CPU every tick and re-uploads them;
- frametime at least 2 (a strip of frametime 1 re-uploads every tick);
- at most MAX frames.
Frames are dropped evenly and the frametime scaled up to match, so every animation keeps the speed and length of its
loop - it only steps a little more coarsely. Idempotent: run it after anything that writes textures."""
import glob, json, math, os, sys
from PIL import Image

MAX = 32       # (1.1.0: 16 was tried after the audit and looked choppy; back to 32)
ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'src/main/resources/assets/theoreticalsciences/textures')


def trim(png):
    mf = png + '.mcmeta'
    meta = json.load(open(mf))
    a = meta.get('animation', {})
    if 'frames' in a:
        return False
    im = Image.open(png)
    w, h = im.size
    fh = a.get('height', w)
    n = h // fh if fh else 1
    ft = max(1, int(a.get('frametime', 1)))
    step = max(math.ceil(n / MAX), math.ceil(2 / ft))
    changed = bool(a.get('interpolate')) or step > 1
    if step > 1 and n > 1:
        keep = list(range(0, n, step))
        out = Image.new(im.mode, (w, fh * len(keep)))
        for j, k in enumerate(keep):
            out.paste(im.crop((0, k * fh, w, (k + 1) * fh)), (0, j * fh))
        out.save(png)
        a['frametime'] = ft * step
    a['interpolate'] = False
    meta['animation'] = a
    if changed:
        json.dump(meta, open(mf, 'w'))
    return changed


# FREEZE is off: tried in 1.1.0 after the audit and reverted (the materials looked better animated).
FREEZE = False
# 1.1.0 (audit): 1,677 of 2,106 textures were animated. Only things that move or glow because of what they are keep their
# animation; the everyday materials and building blocks show their first frame, as vanilla's own ingots, ores and bricks do:
#  * item forms of a material (ingots, plates, dusts, wires, fluid cells, shards, samples...);
#  * ores, plain building blocks, decorative blocks and glass;
#  * a multiblock part before it is formed (the formed one, shown while the machine stands, stays animated).
# The full strip is kept in tools/out/anim_frozen, so nothing is lost if one should move again.
FORMS = {'ingot', 'plate', 'nugget', 'dust', 'wire', 'rod', 'gear', 'foil', 'sheet', 'wafer', 'shard', 'fragment', 'pellet',
         'powder', 'chunk', 'sample', 'boule', 'filament', 'cell', 'fibre', 'fiber', 'salt', 'oxide', 'bar', 'tile', 'brick',
         'ball', 'blend', 'mix', 'slurry', 'ash', 'grit', 'scrap', 'raw'}
STATIC_KINDS = {'ore', 'simple', 'deco', 'deco_pillar', 'deco_stairs', 'deco_slab', 'deco_wall', 'glass', 'casing'}
STILL = ('euclidean_marble', 'hyperbolic_stone', 'dense_stone')
FROZEN = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'out', 'anim_frozen')


def frozen_names():
    C = json.load(open(os.path.join(os.path.dirname(os.path.abspath(__file__)), 'content.json')))
    return {b['id'] for b in C['blocks'] if b.get('kind') in STATIC_KINDS}


def freeze(png):
    """Keeps only the first frame (and a copy of the strip). Returns True if it was animated."""
    mf = png + '.mcmeta'
    if not os.path.exists(mf):
        return False
    im = Image.open(png)
    w, h = im.size
    fh = json.load(open(mf)).get('animation', {}).get('height', w)
    rel = os.path.relpath(png, ROOT)
    keep = os.path.join(FROZEN, rel)
    os.makedirs(os.path.dirname(keep), exist_ok=True)
    im.save(keep)
    open(keep + '.mcmeta', 'w').write(open(mf).read())
    if h > fh:
        im.crop((0, 0, w, fh)).save(png)
    os.remove(mf)
    return True


def main(root=ROOT):
    n = c = f = 0
    static = frozen_names()
    for mf in glob.glob(os.path.join(root, '**', '*.png.mcmeta'), recursive=True):
        png = mf[:-7]
        name, folder = os.path.basename(png)[:-4], os.path.basename(os.path.dirname(png))
        if FREEZE and ((folder == 'item' and name.split('_')[-1] in FORMS) or (folder == 'block' and name in static)):
            f += freeze(png)
    # 1.2.0: the Platonic Realm's layer stones cover whole islands: no moving glint on them (it read as a shiny finish, and
    # every animated texture is uploaded again as it steps)
    for name in STILL:
        png = os.path.join(root, 'block', name + '.png')
        if os.path.exists(png): f += freeze(png)
    print(f'animations: {f} everyday textures made still')
    for mf in glob.glob(os.path.join(root, '**', '*.png.mcmeta'), recursive=True):
        n += 1
        c += trim(mf[:-7])
    print(f'animations: {n} animated textures, {c} trimmed (no interpolation, frametime >= 2, <= {MAX} frames)')


if __name__ == '__main__':
    main(*sys.argv[1:])
