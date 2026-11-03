import os, sys
from PIL import Image
base = sys.argv[1]; out = sys.argv[2]
files = sorted(f for f in os.listdir(base) if f.endswith('.png'))
cols = 10; scale = 4; cell = 16 * scale + 4
rows = (len(files) + cols - 1) // cols
sheet = Image.new('RGBA', (cols * cell, rows * cell), (60, 60, 70, 255))
for i, f in enumerate(files):
    im = Image.open(os.path.join(base, f)).convert('RGBA')
    w = im.width
    fr = im.crop((0, 0, w, w)).resize((16 * scale, 16 * scale), Image.NEAREST)
    sheet.alpha_composite(fr, ((i % cols) * cell + 2, (i // cols) * cell + 2))
sheet.save(out)
print(len(files))
