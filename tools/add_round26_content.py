"""Round 26's fluid."""
import json, os
import round26
from add_complexity_content import upsert

p = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'content.json')
C = json.load(open(p))
upsert(C['fluids'], round26.FLUIDS)
json.dump(C, open(p, 'w'), indent=1)
print('round26 content ok:', len(C['fluids']), 'fluids')
