"""Adds the round-3 expansion's items, blocks and fluid (Quantum Foam, fractal space, Transcendental) to content.json."""
import json, os
import round3
from add_complexity_content import upsert

p = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'content.json')
C = json.load(open(p))
upsert(C['items'], round3.ITEMS)
upsert(C['blocks'], round3.BLOCKS)
upsert(C['fluids'], round3.FLUIDS)
json.dump(C, open(p, 'w'), indent=1)
print('round3 content ok:', len(C['items']), 'items', len(C['blocks']), 'blocks', len(C['fluids']), 'fluids')
