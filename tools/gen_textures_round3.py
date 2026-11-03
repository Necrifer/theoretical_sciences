"""Textures for a subset of content (items, blocks, fluids, machines) without the full 45-minute run.
Usage: python3 gen_textures_round3.py ITEM_IDS... -- BLOCK_IDS... -- FLUID_IDS... -- MACHINE_IDS..."""
import sys, os, json
from multiprocessing import Pool
import numpy as np
import gen_textures as G

if __name__ == '__main__':
    groups_args = ' '.join(sys.argv[1:]).split('--')
    items, blocks, fluids, machines = [set(x.split()) for x in (groups_args + ['', '', '', ''])[:4]]
    G.extreme_block_textures()
    G.round3_block_textures()
    import anim
    groups, jobs = {}, []
    for it in G.C['items']:
        cat = anim.CATEGORY.get(it['art'], 'device')
        if it['id'] in items:
            jobs.append((it['id'], it['art'], it['c1'], it['c2'], groups.get(cat, 0)))
        groups[cat] = groups.get(cat, 0) + 1
    jobs += [('@cell:' + fl['id'], 'gas' if fl['gas'] else 'liquid', fl['c1'], fl['c2'], 0) for fl in G.C['fluids'] if fl['id'] in fluids]
    with Pool() as pool:
        res = pool.map(G._render3d_job, jobs, chunksize=1)
    done = set()
    for (iid, *_), frames in zip(jobs, res):
        if frames:
            name = iid[6:] + '_cell' if iid.startswith('@cell:') else iid
            G.save_strip(frames, os.path.join(G.TEX, 'item', name + '.png'), frametime=1, interpolate=False)
            done.add(name)
    G.setres(32)
    for i, it in enumerate(G.C['items']):
        if it['id'] in items and it['id'] not in done:
            c1, c2 = G.hexc(it['c1']), G.hexc(it['c2'])
            fr = []
            for f in range(G.FRAMES):
                cv = G.Canvas()
                G.ITEM_ART[it['art']](cv, f / G.FRAMES, c1, c2, i)
                fr.append(cv.down())
            G.save_strip(fr, os.path.join(G.TEX, 'item', it['id'] + '.png'))
            done.add(it['id'])
    G.setres(16)
    for i, b in enumerate(G.C['blocks']):
        if b['id'] not in blocks:
            continue
        c1, c2 = G.hexc(b['c1']), G.hexc(b['c2'])
        frames = [G.block_frame(f / G.FRAMES, b['art'], c1, c2, b.get('base') if b['kind'] == 'ore' else None, i) for f in range(G.FRAMES)]
        G.save_strip(frames, os.path.join(G.TEX, 'block', b['id'] + '.png'), frametime=3)
        if b['kind'] in ('casing', 'glass'):
            frames = [G.formed_frame(f / G.FRAMES, b['art'], c1, c2, i, b['kind'] == 'glass') for f in range(G.FRAMES)]
            G.save_strip(frames, os.path.join(G.TEX, 'block', b['id'] + '_formed.png'), frametime=2)
        done.add(b['id'])
    for i, fl in enumerate(G.C['fluids']):
        if fl['id'] not in fluids:
            continue
        c1, c2 = G.hexc(fl['c1']), G.hexc(fl['c2'])
        G.save_strip([G.fluid_frame(f / 32, fl['art'], c1, c2, False, i) for f in range(32)], os.path.join(G.TEX, 'block', 'fluid', fl['id'] + '_still.png'), frametime=2)
        flow = [G.fluid_frame(f / 32, fl['art'], c1, c2, True, i) for f in range(32)]
        flow = [np.repeat(np.repeat(fr, 2, axis=0), 2, axis=1) for fr in flow]
        G.save_strip(flow, os.path.join(G.TEX, 'block', 'fluid', fl['id'] + '_flow.png'), frametime=2)
        done.add(fl['id'])
    ms = json.load(open(os.path.join(G.ROOT, 'machines.json')))
    for i, m in enumerate(ms):
        if m['id'] not in machines:
            continue
        faces = [G.machine_faces(i, m['color'], f / G.FRAMES, m['multiblock']) for f in range(G.FRAMES)]
        for key in ('side', 'top', 'front_off', 'front_on'):
            fr = [fc[key] for fc in faces]
            if key == 'front_off':
                fr = [fr[0]]
            G.save_strip(fr, os.path.join(G.TEX, 'block', 'machine', m['id'] + '_' + key + '.png'), frametime=2)
        done.add(m['id'])
    want = items | blocks | fluids | machines
    print('round3 textures:', len(done), 'missing:', want - done - {f + '_cell' for f in fluids})
