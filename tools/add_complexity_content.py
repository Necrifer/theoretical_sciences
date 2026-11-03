"""Adds the multiblock-complexity content (components, tiered coils/emitters, particles, catalyst plasmas) to content.json."""
import json, os
import complexity

p = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'content.json')
C = json.load(open(p))


def upsert(lst, new):
    idx = {e['id']: i for i, e in enumerate(lst)}
    for e in new:
        if e['id'] in idx:
            lst[idx[e['id']]] = e
        else:
            lst.append(e)


upsert(C['items'], complexity.ITEMS)
upsert(C['blocks'], complexity.COMPONENT_BLOCKS)
upsert(C['fluids'], complexity.FLUIDS)
json.dump(C, open(p, 'w'), indent=1)
print('complexity content ok:', len(C['items']), 'items', len(C['blocks']), 'blocks', len(C['fluids']), 'fluids')
