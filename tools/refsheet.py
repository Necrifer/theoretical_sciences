import sys, glob, os
from PIL import Image, ImageDraw
files = sys.argv[2:]; out = sys.argv[1]
cols = 8; sc = 6
cell = 32 * sc + 10
sheet = Image.new('RGBA', (cols * cell, ((len(files) + cols - 1) // cols) * (cell + 12)), (60, 60, 70, 255)); d = ImageDraw.Draw(sheet)
for i, f in enumerate(files):
    im = Image.open(f).convert('RGBA'); w = im.width
    s2 = 192; fr = im.crop((0, 0, w, w)).resize((s2, s2), Image.NEAREST)
    x = (i % cols) * cell; y = (i // cols) * (cell + 12)
    sheet.alpha_composite(fr, (x + 5, y + 5)); d.text((x + 5, y + cell - 2), os.path.basename(f)[:28], fill=(230, 230, 230, 255))
sheet.save(out)
