"""Shrinks the jar copy of item/block textures: an animated strip rarely uses more than a few hundred colours, so a
256-colour palette PNG is visually identical and about a quarter of the size. Only the build copy is changed (the
source textures stay full colour); a texture is kept as it was if the palette version is not smaller or differs
visibly (mean error over visible pixels above 6/255)."""
import glob, io, os, sys
from PIL import Image
import numpy as np

root = sys.argv[1]
before = after = 0
for f in glob.glob(os.path.join(root, 'assets/theoreticalsciences/textures/item/**/*.png'), recursive=True) + glob.glob(os.path.join(root, 'assets/theoreticalsciences/textures/block/**/*.png'), recursive=True):   # (1.2.0: fluids and machine faces too)
    b = open(f, 'rb').read()
    before += len(b)
    im = Image.open(io.BytesIO(b)).convert('RGBA')
    q = im.quantize(256, method=Image.Quantize.FASTOCTREE, dither=Image.Dither.NONE)
    o = io.BytesIO()
    q.save(o, 'PNG', optimize=True)
    a = np.asarray(im).astype(int)
    c = np.asarray(q.convert('RGBA')).astype(int)
    vis = a[..., 3] > 0
    err = np.abs(a - c)[vis].mean() if vis.any() else 0
    if len(o.getvalue()) < len(b) and err <= 6:
        open(f, 'wb').write(o.getvalue())
        after += len(o.getvalue())
    else:
        after += len(b)

# 1.2.0: every other texture (fluids, machine faces, armour, GUI, entities) - only where an exact palette exists, so
# nothing changes by a single pixel
done = set(glob.glob(os.path.join(root, 'assets/theoreticalsciences/textures/item/**/*.png'), recursive=True) + glob.glob(os.path.join(root, 'assets/theoreticalsciences/textures/block/**/*.png'), recursive=True))
for f in glob.glob(os.path.join(root, 'assets/theoreticalsciences/textures/**/*.png'), recursive=True):
    if f in done: continue
    b = open(f, 'rb').read()
    before += len(b)
    im = Image.open(io.BytesIO(b)).convert('RGBA')
    cols = im.getcolors(256)
    best = b
    if cols is not None:
        pal = [c for _, c in cols]
        idx = {c: i for i, c in enumerate(pal)}
        P = Image.new('P', im.size)
        P.putdata([idx[px] for px in im.get_flattened_data()] if hasattr(im, 'get_flattened_data') else [idx[px] for px in im.getdata()])
        P.putpalette([v for c in pal for v in c[:3]] + [0] * (768 - 3 * len(pal)))
        o = io.BytesIO()
        P.save(o, 'PNG', optimize=True, transparency=bytes(c[3] for c in pal))
        if len(o.getvalue()) < len(best) and np.array_equal(np.asarray(Image.open(io.BytesIO(o.getvalue())).convert('RGBA')), np.asarray(im)):
            best = o.getvalue()
    if best is b:
        o = io.BytesIO()
        im.save(o, 'PNG', optimize=True)
        if len(o.getvalue()) < len(b): best = o.getvalue()
    if best is not b: open(f, 'wb').write(best)
    after += len(best)
print(f'textures packed: {before / 1e6:.1f} MB -> {after / 1e6:.1f} MB')

# 1.2.0: and the JSON written without indentation (the game reads it the same; the source files keep theirs)
import json
jb = ja = 0
for f in glob.glob(os.path.join(root, '**/*.json'), recursive=True) + glob.glob(os.path.join(root, '**/*.mcmeta'), recursive=True):
    t = open(f, 'rb').read()
    try:
        d = json.loads(t)
    except ValueError:
        continue
    m = json.dumps(d, ensure_ascii=False, separators=(',', ':')).encode('utf-8')
    jb += len(t)
    if len(m) < len(t):
        open(f, 'wb').write(m)
        ja += len(m)
    else:
        ja += len(t)
print(f'json packed: {jb / 1e6:.1f} MB -> {ja / 1e6:.1f} MB')
