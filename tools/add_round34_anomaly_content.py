"""Round 32's content (round34_anomaly)."""
import json, os
import round34_anomaly
from add_complexity_content import upsert

p = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'content.json')
C = json.load(open(p))
if round34_anomaly.ITEMS: upsert(C['items'], round34_anomaly.ITEMS)
if round34_anomaly.BLOCKS: upsert(C['blocks'], round34_anomaly.BLOCKS)
if round34_anomaly.FLUIDS: upsert(C['fluids'], round34_anomaly.FLUIDS)
json.dump(C, open(p, 'w'), indent=1)
print('round34_anomaly content ok')
