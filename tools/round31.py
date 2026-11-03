"""
Round 31: customising the mod from datapacks and KubeJS.

Nothing here generates data of its own - the new folders (ts_component_tiers, ts_forge, ts_machines) are for modpacks
to fill; with none of their files the mod is exactly as before. This round only adds the text the game shows for them
(the KubeJS recipe schemas for the new fields are written by gen_data.py's "KUBEJS SCHEMAS" section).
See docs/KUBEJS.md for the file formats and script examples.
"""

LANG = {
    # the Field Calibrator / structure report on a formed multiblock: what its coil tier means
    'message.theoreticalsciences.structure_coil_speed': 'Coil tier %s: %sx speed',
    'message.theoreticalsciences.structure_coil_physics': 'Coil tier %s: %sx speed, superconductor critical temperature %s K, critical field %s T',
}


def apply(g):
    g['LANG'].update(LANG)


def finish(g):
    # (later rounds may rewrite lang text; ours is put back last)
    g['LANG'].update(LANG)
