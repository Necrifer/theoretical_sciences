"""
1.7.3: two of the magic mods' own mechanics, beside their items (1.7.2).

A recipe can now ask for "magic": {"mana": n} (Botania: taken from a Mana Pool standing against the machine) or
{"aura": n} (Nature's Aura: taken from the air round the machine, never below half of what the land holds by nature).
Three Field Syntheses get such a recipe, each the same as its 1.7.2 item route without the mod's items.

Run:  python3 tools/round173.py
"""
import copy, json, os, sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from tsgen import *          # noqa: F401,F403
import tsgen as T

L = T.LANG
R = os.path.join(D, 'recipe')

L['jei.%s.mana' % NS] = 'Mana: %s, from a Mana Pool against the machine'
L['jei.%s.aura' % NS] = 'Aura: %s, from the air round the machine'
L['status.%s.no_mana' % NS] = 'Needs mana: stand a Mana Pool against it'
L['status.%s.no_aura' % NS] = 'Too little aura in the air here'

# (the 1.7.2 item route it is built on, the new id, the mod, its items to leave out, what it draws instead)
ROUTES = [
    ('revival/r172_living_archive_mana.json', 'r173_living_archive_mana_pool', 'botania', {'mana': 50000}),
    ('heliography/r172_planetary_survey_aura.json', 'r173_planetary_survey_aura_air', 'naturesaura', {'aura': 60000}),
    ('causality_loop/r172_deep_record_hand_of_time.json', 'r173_deep_record_aura_air', 'naturesaura', {'aura': 80000}),
]
for base, rid, mod, magic in ROUTES:
    b = json.load(open(os.path.join(R, base), encoding='utf-8'))
    r = copy.deepcopy(b)
    r['item_inputs'] = [i for i in b['item_inputs'] if not i.get('item', '').startswith(mod + ':')]
    assert len(r['item_inputs']) < len(b['item_inputs']), rid
    r['magic'] = magic
    dump(os.path.join(R, os.path.dirname(base), rid + '.json'), r)


def upd(Eall):
    e = Eall['materials/magic_routes']
    e['pages'] = [p for p in e['pages'] if not p.get('r173')]
    e['pages'].append(dict(text("$(thing)Mana and aura themselves$()$(br)Two of those arts can give the thing itself in place of what is made with it.$(br2)"
                                "Living Archive: 50,000 mana in place of the pearls and diamonds. Stand a $(item)Mana Pool$() against the Revival Vat's controller.$(br2)"
                                "Planetary Survey: 60,000 aura in place of the ingots and bottles. Deep Record: 80,000 aura in place of the Hand of Time and the ingots. "
                                "The machine takes it from the air within twenty blocks, and stops when the land is down to half of what it holds by nature."), r173=True))


edit_json(os.path.join(A, 'ts_codex/entries.json'), upd)
finish()
print('1.7.3 written:', len(ROUTES), 'recipes')
