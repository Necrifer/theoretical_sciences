"""Round 25: the Autonomous Extreme Assembler's textures - the Extreme Assembly Table's, shifted to a cold cyan, with
an input/output port ring drawn on the sides (frames kept, so the animation still loops)."""
import colorsys, os, shutil, sys
from PIL import Image

T = sys.argv[1]


def shift(src, dst, ports):
    im = Image.open(os.path.join(T, src)).convert('RGBA')
    px = im.load()
    w, h = im.size
    for y in range(h):
        for x in range(w):
            r, g, b, a = px[x, y]
            hh, ll, ss = colorsys.rgb_to_hls(r / 255, g / 255, b / 255)
            hh = (hh + 0.45) % 1.0
            r2, g2, b2 = colorsys.hls_to_rgb(hh, ll, ss)
            px[x, y] = (int(r2 * 255), int(g2 * 255), int(b2 * 255), a)
    if ports:
        for f in range(h // w):
            oy = f * w
            for (cx, cy, col) in ((4, w // 2, (80, 220, 255)), (w - 5, w // 2, (255, 170, 60))):
                for dy in range(-2, 3):
                    for dx in range(-2, 3):
                        if max(abs(dx), abs(dy)) == 2: px[cx + dx, oy + cy + dy] = col + (255,)
                        elif dx == 0 and dy == 0: px[cx + dx, oy + cy + dy] = (255, 255, 255, 255)
    im.save(os.path.join(T, dst))
    m = os.path.join(T, src + '.mcmeta')
    if os.path.exists(m): shutil.copy(m, os.path.join(T, dst + '.mcmeta'))


for part in ('top', 'side', 'bottom'):
    shift(f'extreme_assembly_table_{part}.png', f'autonomous_extreme_assembler_{part}.png', part == 'side')
print('assembler textures written')
