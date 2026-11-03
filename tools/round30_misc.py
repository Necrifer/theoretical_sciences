"""
Round 30 (misc).

* Singularity Clusters animate visibly: the core is no longer tinted almost black, and each level has its own glow
  layer (tools/art30/clusters.py) - 3 orbiting singularities for a Cluster, 5 for a Hypercluster, 8 for an
  Ultracluster - chosen by the item property theoreticalsciences:cluster_level (level / 10).
"""
import os

NS = 'theoreticalsciences'
LANG = {
    'gui.theoreticalsciences.censer_search': 'Search effects...',
    'key.theoreticalsciences.codex_lookup': 'Look up in the Quantum Codex',
    'key.categories.theoreticalsciences': 'Theoretical Sciences',
}


def apply(g):
    w, A = g['w'], g['A']
    g['LANG'].update(LANG)
    base = {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'{NS}:item/singularity_cluster', 'layer1': f'{NS}:item/singularity_cluster_glow'}}
    for lv in (2, 3):
        w(os.path.join(A, 'models', 'item', f'singularity_cluster_{lv}.json'),
          {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'{NS}:item/singularity_cluster', 'layer1': f'{NS}:item/singularity_cluster_glow_{lv}'}})
    base['overrides'] = [{'predicate': {f'{NS}:cluster_level': 0.15}, 'model': f'{NS}:item/singularity_cluster_2'},
                         {'predicate': {f'{NS}:cluster_level': 0.25}, 'model': f'{NS}:item/singularity_cluster_3'}]
    g['_cluster_model'] = base


def finish(g):
    # written last, so no earlier round's plain model replaces it
    g['w'](os.path.join(g['A'], 'models', 'item', 'singularity_cluster.json'), g['_cluster_model'])
