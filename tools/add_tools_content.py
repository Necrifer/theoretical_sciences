"""Adds the multiblock tool items (Field Calibrator, Autonomous Calibrator, Structure Assembler) to content.json."""
import json, os

p = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'content.json')
C = json.load(open(p))
have = {i['id'] for i in C['items']}
NEW = [
    dict(id='field_calibrator', name='Field Calibrator', art='analyzer', c1='#3a4458', c2='#ffd040', special='calibrator', stack=1,
         lore='Use on a multiblock controller to restore its containment, clear a fault and read out the ideal tuning value.'),
    dict(id='autonomous_calibrator', name='Autonomous Calibrator', art='aicore', c1='#1a2a30', c2='#60ffd0', special='autocalibrator', stack=16,
         lore='Installs into a multiblock controller for good: it follows the drifting tuning and repairs containment by itself, for a 25% power tax.'),
    dict(id='structure_assembler', name='Structure Assembler', art='analyzer', c1='#2a3040', c2='#60c0ff', special='assembler', stack=1,
         lore='Use on a multiblock controller to build every missing block from your inventory; sneak-use to preview. Sneak-use in the air to switch to taking structures apart.'),
]
NEW += [
    dict(id='fractured_essence', name='Fractured Essence', art='chunk', c1='#3a2a60', c2='#80ffe0',
         lore='A splinter of reality that broke away from a destabilised creature. It is in several places at once.'),
    dict(id='void_essence', name='Void Essence', art='ghostorb', c1='#14081f', c2='#c060ff',
         lore='What is left of a Void Wraith: a little pocket of nothing that refuses to be filled.'),
]
for it in NEW:
    if it['id'] not in have:
        C['items'].append(it)
if not any(b['id'] == 'fractured_reality' for b in C['blocks']):
    C['blocks'].append(dict(id='fractured_reality', name='Fractured Reality', art='fracture', c1='#201838', c2='#80ffe0',
                            kind='fracture', light=6, hardness=2.0, tool='none'))
json.dump(C, open(p, 'w'), indent=1)
print('tool items ok')
