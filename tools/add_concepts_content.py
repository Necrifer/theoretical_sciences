"""Adds the concept-field content (mathematics, particles, relativity, stars, strings, time, singularities, Platonic Realm) to content.json."""
import json, os
import concepts
from add_complexity_content import upsert

p = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'content.json')
C = json.load(open(p))
upsert(C['items'], concepts.ITEMS)
upsert(C['fluids'], concepts.FLUIDS)
upsert(C['blocks'], concepts.BLOCKS)
json.dump(C, open(p, 'w'), indent=1)
print('concept content ok:', len(C['items']), 'items', len(C['blocks']), 'blocks', len(C['fluids']), 'fluids')
