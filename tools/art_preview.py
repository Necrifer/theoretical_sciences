"""
Preview item art: python3 art_preview.py OUT.png item_id [item_id ...]
Renders 6 frames of each item's animation (left to right) at 3x, one item per row, with the ids written beside.
Uses exactly the same code path as the real texture build (anim.render_frame, incl. art11 groups).
"""
import json, os, sys
import numpy as np
from PIL import Image, ImageDraw

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import anim, items3d

C = json.load(open(os.path.join(os.path.dirname(os.path.abspath(__file__)), 'content.json')))
BY = {i['id']: i for i in C['items']}


def frames(iid, n=6):
    it = BY[iid]
    total = anim.frames_for(iid)
    out = []
    for k in range(n):
        f = int(k * total / n)
        img = anim.render_frame(iid, it['art'], it['c1'], it['c2'], f / total, 0)
        a = np.clip(img * 255, 0, 255).astype(np.uint8) if img.dtype != np.uint8 else img
        out.append(Image.fromarray(a, 'RGBA'))
    return out


if __name__ == '__main__':
    out, ids = sys.argv[1], sys.argv[2:]
    S = 3
    rows = []
    for iid in ids:
        try:
            rows.append((iid, frames(iid)))
        except Exception as e:
            print('FAILED', iid, repr(e))
    if not rows:
        sys.exit(1)
    w = rows[0][1][0].width * S
    sheet = Image.new('RGBA', (220 + 6 * (w + 4), len(rows) * (w + 4)), (40, 40, 48, 255))
    d = ImageDraw.Draw(sheet)
    for r, (iid, fr) in enumerate(rows):
        d.text((4, r * (w + 4) + w // 2 - 6), iid, fill=(230, 230, 230, 255))
        for k, im in enumerate(fr):
            sheet.paste(im.resize((w, w), Image.NEAREST), (220 + k * (w + 4), r * (w + 4)), im.resize((w, w), Image.NEAREST))
    sheet.save(out)
    print('wrote', out, len(rows), 'items')
