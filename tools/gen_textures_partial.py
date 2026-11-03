"""Render textures for a subset of items (plus the extreme-crafting blocks) without redoing the whole 45-minute run.
Usage: python3 gen_textures_partial.py item_id [item_id ...]"""
import sys, os, json
from multiprocessing import Pool
import gen_textures as G

if __name__ == '__main__':
    ids = set(sys.argv[1:])
    G.extreme_block_textures()
    items = [it for it in G.C['items'] if it['id'] in ids]
    import anim
    groups, jobs = {}, []
    for it in G.C['items']:
        cat = anim.CATEGORY.get(it['art'], 'device')
        if it['id'] in ids:
            jobs.append((it['id'], it['art'], it['c1'], it['c2'], groups.get(cat, 0)))
        groups[cat] = groups.get(cat, 0) + 1
    with Pool() as pool:
        res = pool.map(G._render3d_job, jobs, chunksize=1)
    done = set()
    for (iid, *_), frames in zip(jobs, res):
        if frames:
            G.save_strip(frames, os.path.join(G.TEX, 'item', iid + '.png'), frametime=1, interpolate=False)
            done.add(iid)
    G.setres(32)
    for i, it in enumerate(G.C['items']):
        if it['id'] in ids and it['id'] not in done:
            c1, c2 = G.hexc(it['c1']), G.hexc(it['c2'])
            fr = []
            for f in range(G.FRAMES):
                cv = G.Canvas()
                G.ITEM_ART[it['art']](cv, f / G.FRAMES, c1, c2, i)
                fr.append(cv.down())
            G.save_strip(fr, os.path.join(G.TEX, 'item', it['id'] + '.png'))
            done.add(it['id'])
    # 1.4.0: blocks and machine faces too (same code as the full run, only for the ids given)
    G.setres(16)
    for i, b in enumerate(G.C['blocks']):
        if b['id'] not in ids or not b.get('art'):
            continue
        c1, c2 = G.hexc(b['c1']), G.hexc(b['c2'])
        frames = [G.block_frame(f / G.FRAMES, b['art'], c1, c2, b.get('base') if b['kind'] == 'ore' else None, i) for f in range(G.FRAMES)]
        G.save_strip(frames, os.path.join(G.TEX, 'block', b['id'] + '.png'), frametime=3)
        if b['kind'] in ('casing', 'glass'):
            frames = [G.formed_frame(f / G.FRAMES, b['art'], c1, c2, i, b['kind'] == 'glass') for f in range(G.FRAMES)]
            G.save_strip(frames, os.path.join(G.TEX, 'block', b['id'] + '_formed.png'), frametime=2)
        done.add(b['id'])
    machines = json.load(open(os.path.join(os.path.dirname(os.path.abspath(__file__)), 'machines.json')))
    for i, m in enumerate(machines):
        if m['id'] not in ids:
            continue
        faces = [G.machine_faces(i, m['color'], f / G.FRAMES, m['multiblock']) for f in range(G.FRAMES)]
        for key in ('side', 'top', 'front_off', 'front_on'):
            fr = [fc[key] for fc in faces]
            if key == 'front_off':
                fr = [fr[0]]
            G.save_strip(fr, os.path.join(G.TEX, 'block', 'machine', m['id'] + '_' + key + '.png'), frametime=2)
        done.add(m['id'])
    # 1.10.1: fluids too - still and flowing textures, and their containment cells
    import numpy as np
    G.setres(16)
    for i, fl in enumerate(G.C['fluids']):
        if fl['id'] not in ids:
            continue
        c1, c2 = G.hexc(fl['c1']), G.hexc(fl['c2'])
        G.save_strip([G.fluid_frame(f / 32, fl['art'], c1, c2, False, i) for f in range(32)], os.path.join(G.TEX, 'block', 'fluid', fl['id'] + '_still.png'), frametime=2)
        flow = [G.fluid_frame(f / 32, fl['art'], c1, c2, True, i) for f in range(32)]
        flow = [np.repeat(np.repeat(fr, 2, axis=0), 2, axis=1) for fr in flow]
        G.save_strip(flow, os.path.join(G.TEX, 'block', 'fluid', fl['id'] + '_flow.png'), frametime=2)
        G.setres(32)
        G.save_strip([G.cell_frame(f / G.FRAMES, c1, c2) for f in range(G.FRAMES)], os.path.join(G.TEX, 'item', fl['id'] + '_cell.png'))
        G.setres(16)
        done.add(fl['id'])
    print('partial textures:', len(done), 'items/blocks/machines, missing:', ids - done)
