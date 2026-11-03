import os, sys
from PIL import Image, ImageDraw
base = sys.argv[1]; out = sys.argv[2]; only = sys.argv[3:] 
files = sorted(f for f in os.listdir(base) if f.endswith('.png') and (not only or f[:-4] in only))
SZ=int(os.environ.get("SZ","32")); cols = 8; scale = 3; cw = SZ*scale+40; ch = SZ*scale+18
rows = (len(files)+cols-1)//cols
sheet = Image.new('RGBA', (cols*cw, rows*ch), (70,70,80,255)); d = ImageDraw.Draw(sheet)
for i,f in enumerate(files):
    im = Image.open(os.path.join(base,f)).convert('RGBA'); w=im.width
    fr = im.crop((0,0,w,w)).resize((SZ*scale,SZ*scale), Image.NEAREST)
    x=(i%cols)*cw+20; y=(i//cols)*ch
    sheet.alpha_composite(fr,(x,y)); d.text((x-18,y+SZ*scale+2), f[:-4][:22], fill=(230,230,230,255))
sheet.save(out)
