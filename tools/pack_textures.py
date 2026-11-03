"""Shrinks the jar copy of item/block textures: an animated strip rarely uses more than a few hundred colours, so a
256-colour palette PNG is visually identical and about a quarter of the size. Only the build copy is changed (the
source textures stay full colour); a texture is kept as it was if the palette version is not smaller or differs
visibly (mean error over visible pixels above 6/255)."""
import glob, io, os, sys
from PIL import Image
import numpy as np

root = sys.argv[1]
before = after = 0
for f in glob.glob(os.path.join(root, 'assets/theoreticalsciences/textures/item/*.png')) + glob.glob(os.path.join(root, 'assets/theoreticalsciences/textures/block/*.png')):
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
print(f'textures packed: {before / 1e6:.1f} MB -> {after / 1e6:.1f} MB')
