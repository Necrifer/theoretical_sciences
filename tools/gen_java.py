"""Generates QHContentList.java from content.json."""
import json, os
ROOT = os.path.dirname(os.path.abspath(__file__))
c = json.load(open(os.path.join(ROOT, 'content.json')))
def col(h): return int(h[1:], 16)
lines = []
lines.append('package com.theoreticalsciences.registry;\n\nimport java.util.List;\n\n/** The mod\'s content list: every simple item, block and fluid it registers. */\npublic final class QHContentList {')
lines.append('    public record ItemDef(String id, String special, int stack) {}')
lines.append('    public record BlockDef(String id, String kind, int light, float hardness, String tool, String base) {}')
lines.append('    public record FluidDef(String id, boolean gas, int light, int temperature, int density, int viscosity, int color) {}')
lines.append('    public static final List<ItemDef> ITEMS = List.of(')
lines.append(',\n'.join(f'        new ItemDef("{i["id"]}", "{i.get("special","")}", {i.get("stack",64)})' for i in c['items']) + ');')
lines.append('    public static final List<BlockDef> BLOCKS = List.of(')
lines.append(',\n'.join(f'        new BlockDef("{b["id"]}", "{b["kind"]}", {b.get("light",0)}, {b["hardness"]}F, "{b["tool"]}", "{b.get("base") or ""}")' for b in c['blocks']) + ');')
lines.append('    public static final List<FluidDef> FLUIDS = List.of(')
lines.append(',\n'.join(f'        new FluidDef("{f["id"]}", {str(f["gas"]).lower()}, {f["light"]}, {min(f["temp"], 2**31-1)}, {f["density"]}, {f["viscosity"]}, 0x{col(f["c1"]):06X})' for f in c['fluids']) + ');')
lines.append('    private QHContentList() {}\n}\n')
out = os.path.join(ROOT, '..', 'src/main/java/com/theoreticalsciences/registry/QHContentList.java')
open(out, 'w').write('\n'.join(lines))
print('wrote', out)

# ---- export machine table for the other generators
import re
src = open(os.path.join(ROOT, '..', 'src/main/java/com/theoreticalsciences/machine/MachineType.java')).read()
machines = []
for m in re.finditer(r'^\s+([A-Z_]+)\s*\("(\w+)",\s*(\d+),\s*(\d+),\s*(\d+),\s*(\d+),\s*(true|false),\s*(null|"\w+"),\s*([\d_]+),\s*([\d_]+),\s*Effect\.(\w+),\s*0x([0-9A-Fa-f]+)\)', src, re.M):
    machines.append(dict(id=m.group(1).lower(), recipe=m.group(2), inputs=int(m.group(3)), fluid_inputs=int(m.group(4)),
                         outputs=int(m.group(5)), fluid_outputs=int(m.group(6)), generator=m.group(7) == 'true',
                         multiblock=m.group(8) != 'null', mb_id=m.group(8).strip('"') if m.group(8) != 'null' else None,
                         energy=int(m.group(9).replace('_', '')), effect=m.group(11), color=int(m.group(12), 16)))
# round 16 (mirrors MachineType's constructor): non-generator multiblocks 16 item + 16 fluid inputs and at least 4 + 4 outputs; single-block
# machines a little more; generators unchanged
for m_ in machines:
    i_, f_ = m_['inputs'], m_['fluid_inputs']
    m_['legacy_inputs'], m_['legacy_fluid_inputs'] = i_, f_
    if m_['multiblock'] and not m_['generator']:
        m_['inputs'], m_['fluid_inputs'] = 16, 16
        m_['outputs'], m_['fluid_outputs'] = max(m_['outputs'], 4), max(m_['fluid_outputs'], 4)
    elif not m_['generator']:
        m_['inputs'] = i_ if i_ >= 6 else min(6, max(i_ + 2, 4))
        m_['fluid_inputs'] = f_ if f_ >= 3 else f_ + 1
json.dump(machines, open(os.path.join(ROOT, 'machines.json'), 'w'), indent=1)
print(len(machines), 'machines')
