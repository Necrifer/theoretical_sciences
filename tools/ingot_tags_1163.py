"""
1.16.3: every ingot of the mod gets a tag of its own, theoreticalsciences:ingots/<material>, holding the mod's ingot and
(optionally) the common tag other mods use for the same metal, c:ingots/<material>. Every recipe ingredient that named
the mod's ingot by id now names that tag instead, so another mod's ingot of the same metal (NuclearCraft's Lithium,
GregTech's Aluminium, Mekanism's Steel...) works in the mod's recipes too. Packs can add more ingots to these tags.

The mod's own ingots are NOT put into other mods' c: tags here (except where they already were), so the balance of the
other mods is untouched: GregTech gates progress on its own steel and aluminium.

Run:  python3 tools/ingot_tags_1163.py           (idempotent)
"""
import glob
import json
import os
import re

HERE = os.path.dirname(os.path.abspath(__file__))
RES = os.path.join(HERE, '..', 'src/main/resources')
LANG = os.path.join(RES, 'assets/theoreticalsciences/lang/en_us.json')
TAGS = os.path.join(RES, 'data/theoreticalsciences/tags/item/ingots')
RECIPES = os.path.join(RES, 'data/theoreticalsciences/recipe')

# material -> the common tag names other mods use for it (checked in GregTech 8.1, NuclearCraft, Mekanism, Ad Astra,
# Re-Avaritia, EnderIO and Create). Materials not listed use their own name.
ALIASES = {
    'aluminium': ['aluminium', 'aluminum'],       # GregTech: aluminium; NuclearCraft: aluminum
    'bloomery_steel': ['steel'],                  # Mekanism, GregTech, Ad Astra, NuclearCraft steel
    'lithium_6': ['lithium_6', 'lithium6'],
    'stellar_iron': [],                           # iron from a star's core is not everyday iron
    'antiiron': [], 'antigold': [],               # antimatter is never ordinary metal
}


# recipe types whose slots hold an exact item with a count (an item stack, not an ingredient), so they cannot take a tag
SKIP = {'theoreticalsciences:empyrean'}


def ingots():
    L = json.load(open(LANG, encoding='utf-8'))
    out = []
    for k in L:
        m = re.fullmatch(r'item\.theoreticalsciences\.([a-z0-9_]+)_ingot', k)
        if m:
            out.append(m.group(1))
    return sorted(out)


def main():
    mats = ingots()
    os.makedirs(TAGS, exist_ok=True)
    for m in mats:
        vals = ['theoreticalsciences:%s_ingot' % m]
        for a in ALIASES.get(m, [m]):
            vals.append({'id': '#c:ingots/' + a, 'required': False})
        with open(os.path.join(TAGS, m + '.json'), 'w') as f:
            json.dump({'replace': False, 'values': vals}, f, indent=2)
            f.write('\n')
    ids = {'theoreticalsciences:%s_ingot' % m: 'theoreticalsciences:ingots/' + m for m in mats}

    changed = 0

    def fix(o):
        nonlocal changed
        if isinstance(o, dict):
            # an ingredient: {"item": id} (+ count). Results use "id", so they are never touched.
            if set(o) <= {'item', 'count'} and o.get('item') in ids:
                tag = ids[o.pop('item')]
                o_new = {'tag': tag}
                o_new.update(o)
                o.clear()
                o.update(o_new)
                changed += 1
                return
            for v in o.values():
                fix(v)
        elif isinstance(o, list):
            for v in o:
                fix(v)

    files = 0
    for p in glob.glob(os.path.join(RECIPES, '**/*.json'), recursive=True):
        txt = open(p, encoding='utf-8').read()
        if '_ingot"' not in txt:
            continue
        j = json.loads(txt)
        if j.get('type') in SKIP:
            continue
        before = changed
        fix(j)
        if changed != before:
            files += 1
            with open(p, 'w', encoding='utf-8') as f:
                json.dump(j, f, ensure_ascii=False, indent=2)
                if txt.endswith('\n'):
                    f.write('\n')
    print('ingot tags:', len(mats), '; ingredients switched:', changed, 'in', files, 'recipes')


if __name__ == '__main__':
    main()
