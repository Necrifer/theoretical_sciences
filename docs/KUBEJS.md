# Customising Theoretical Sciences with KubeJS

Theoretical Sciences is built to be customisable by modpacks. Almost everything — machine recipes, the research tree, structure parts, 
machine numbers, the Omnipotence Core's Forge, singularities — is data, and all ofit can be changed from KubeJS without touching the mod.

Requires **KubeJS for NeoForge 1.21.1** (`2101.7` or newer). Everything below is optional: with no scripts, the mod
behaves exactly as shipped.

---

## Contents

1. [Mod folder](#1-mod-folder)
2. [Machine recipes](#2-machine-recipes)
3. [Extreme Assembly Table, Singularity Compactor and Convergence Altar recipes](#3-extreme-assembly-table-singularity-compactor-and-convergence-altar-recipes)
4. [Machine properties (`ts_machines`)](#4-machine-properties-ts_machines)
5. [Coil and emitter tiers (`ts_component_tiers`)](#5-coil-and-emitter-tiers-ts_component_tiers)
6. [Machine physics (`ts_physics`)](#6-machine-physics-ts_physics)
7. [Omnipotence Core Forge recipes (`ts_forge`)](#7-omnipotence-core-forge-recipes-ts_forge)
8. [Research (`ts_research`)](#8-research-ts_research)
9. [Singularities (`ts_singularities`)](#9-singularities-ts_singularities)
10. [Tags from this mod](#10-tags-from-this-mod)
11. [Events](#11-events)
12. [The `TheoreticalSciences` helper](#12-the-theoreticalsciences-helper)
13. [Text, names and the Quantum Codex](#13-text-names-and-the-quantum-codex)
14. [Worked example: a tier 6 coil](#14-worked-example-a-tier-6-coil)
15. [Troubleshooting](#15-troubleshooting)

---

## 1. Mod folder

Sections 4–9 are unique, they only take JSON files in a data folder. 
KubeJS gives you two equivalent ways to call them:

| Method | File location |
|---|---|
| A file | `kubejs/data/<namespace>/<folder>/<name>.json` |
| A server script | `ServerEvents.generateData('after_mods', event => event.json('<namespace>:<folder>/<name>', { ... }))` |

```js
// kubejs/server_scripts/theoreticalsciences_data.js
ServerEvents.generateData('after_mods', event => {
  event.json('mypack:ts_machines/quark_gluon_collider', { speed_multiplier: 2 })
})
```

* `<namespace>` can be anything (`kubejs`, `mypack`, ...). Use `theoreticalsciences` only when a section says so — that is
  how you **replace** one of the mod's own files.
* `/reload` applies changes (block registration in startup scripts needs a full restart, as usual for KubeJS).
* The new data is sent to every player automatically. JEI/EMI, machine screens and the Codex update too.
* A broken file is skipped with a message in `logs/latest.log` (search for `Theoretical Sciences` or the file name). It
  never stops the game from loading.

---

## 2. Machine recipes

Every machine has a KubeJS recipe schema, so `event.recipes.theoreticalsciences.<type>(...)` works in
`ServerEvents.recipes`. The recipe type is the machine's recipe folder name: `entangling`, `collider`,
`plasma_smelting`, `vacuum_harvesting`, ... The full list (82 types) is in the mod jar under
`data/theoreticalsciences/kubejs/recipe_schema/`. Each schema file's `constructors` shows exactly which arguments that
machine takes.

Arguments come in this order — **outputs first, then inputs** — and only acceptable machine inputs has:

1. item outputs
2. fluid outputs
3. item inputs
4. fluid inputs

```js
// kubejs/server_scripts/theoreticalsciences_recipes.js
ServerEvents.recipes(event => {
  // Quantum Entangler: item outputs, item inputs
  event.recipes.theoreticalsciences.entangling(['2x theoreticalsciences:qubit'], ['minecraft:diamond'])
    .energy(1024)
    .duration(100)

  // a machine with all four kinds
  event.recipes.theoreticalsciences.condensing(
      ['theoreticalsciences:quantum_dot'],                        // item outputs
      [Fluid.of('theoreticalsciences:superfluid_helium', 500)],   // fluid outputs
      ['minecraft:snowball'],                                // item inputs
      [Fluid.of('minecraft:water', 1000)])                   // fluid inputs
    .research('theoreticalsciences:cryogenics')

  // remove or change the mod's own recipes the usual KubeJS way
  event.remove({ id: 'theoreticalsciences:collider/superpartner_shard' })
  event.remove({ type: 'theoreticalsciences:kugelblitz' })
  event.replaceInput({ mod: 'theoreticalsciences' }, 'minecraft:diamond', 'minecraft:emerald')
})
```

Optional parts, chained after the call:

| Method | JSON field | Meaning |
|---|---|---|
| `.energy(n)` / `.fe(n)` | `energy` | FE per tick. For generators: FE per tick **produced**. |
| `.duration(n)` / `.time(n)` | `duration` | Duration of recipe, in game ticks. |
| `.research('ns:id')` | `research` | Research the machine's owner must have. |
| `.catalysts([...])` | `catalysts` | Must be present, never used up. |
| `.chances([1, 0.5])` | `chances` | Chance for each item output (1 = always). |
| `.minTier(6)` | `min_tier` | Lowest coil tier the multiblock must have (see section 5). |
| `.optionalInputs([{item: 'mekanism:alloy_atomic', count: 4}])` | `optional_inputs` | Only used if that mod is installed. |
| `.optionalFluidInputs([{fluid: 'mekanism:hydrogen', amount: 1000}])` | `optional_fluid_inputs` | The same for fluids. |
| `.physics({vars: {A: 1}, windows: [{q: 'sNN', min: 3600, max: 4200}]})` | `physics` | A window a physics quantity must be held in (section 6). |
| `.insightPerRun(5)` | `insight_per_run` | Research Insight given to the owner per run. |
| `.environment({...})` | `environment` | Conditions around the machine: gravity, time, vacuum, dimension, biome (below). |

Notes:

* `min_tier` only applies to multiblocks with coils. A single-block machine has no coil tier, so a recipe with
  `min_tier` above 0 never runs in one. JEI/EMI show "Needs coil tier N+".
* Very large numbers are safe: see "Limits and extreme values" in section 4.

### Conditions around the machine (`environment`)

A recipe can require the physical conditions where its machine stands to operate. A dimension sets them naturally, and a Field Projector
modifies them within 16 blocks. Every field is optional:

```js
event.recipes.theoreticalsciences.collider(['theoreticalsciences:superpartner_shard'], [], ['theoreticalsciences:quark_gluon_plasma_cell'], [])
  .energy(4000000).duration(400)
  .environment({
    gravity_min: 100,                                // in gravitational unit g (Earth = 1); gravity_max for micro-gravity
    dimensions_min: 5,                               // spatial dimensions
    time_max: 0.1,                                   // how fast time runs (time_min for faster)
    vacuum_min: 2,                                   // 0 ordinary, 1 Casimir-depleted, 2 false, 3 true vacuum
    dimensions: ['theoreticalsciences:the_bulk', 'minecraft:the_end'],   // only in one of these dimensions
    biomes: ['minecraft:deep_dark', '#c:is_cold']                    // only in one of these biomes (ids or #tags)
  })
```

| Field | Meaning |
|---|---|
| `gravity_min`, `gravity_max` | Gravity required, in g. |
| `dimensions_min` | Number of spatial dimensions. |
| `time_min`, `time_max` | Speed of time, relative to normal. |
| `vacuum_min` | 0 ordinary, 1 Casimir-depleted, 2 false vacuum, 3 true vacuum. |
| `dimensions` | Dimension ids. The recipe runs only in one of them. |
| `biomes` | Biome ids or `#tags`. The recipe runs only in one of them, at the controller's position. |

JEI and EMI list every condition, and the machine shows *Wrong Environment* when one isn't met.

**Dimension checks use the dimension id** (`theoreticalsciences:quantum_foam`), never the biome. A mod that changes or
adds biomes doesn't affect them. Use `biomes` when a recipe should depend on the biome.
* Raw JSON also works: `event.custom({ type: 'theoreticalsciences:collider', item_inputs: [...], ... })`.

---

## 3. Extreme Assembly Table, Singularity Compactor and Convergence Altar recipes

```js
ServerEvents.recipes(event => {
  // Extreme Assembly Table - shaped (any grid up to 9x9) and shapeless
  event.recipes.theoreticalsciences.extreme_shaped('minecraft:beacon',
    ['AAA', 'ABA', 'AAA'], { A: 'minecraft:glass', B: 'minecraft:nether_star' })
  event.recipes.theoreticalsciences.extreme_shapeless('minecraft:elytra',
    ['minecraft:phantom_membrane', 'minecraft:netherite_ingot'])

  // Singularity Compactor: result, ingredient, how many
  event.recipes.theoreticalsciences.compaction('minecraft:diamond_block', 'minecraft:coal_block', 1000)

  // Convergence Altar: result, core item, pedestal items; energy = total FE, tier = lowest pedestal grade (1-3)
  event.recipes.theoreticalsciences.convergence('minecraft:totem_of_undying', 'minecraft:emerald_block',
      ['minecraft:gold_block', 'minecraft:gold_block'])
    .energy(1000000).duration(200).tier(1)
})
```

The Extreme Assembly Table also crafts every ordinary crafting-table recipe.

**Changing and removing** these works like any other recipe. Every recipe's id is its path in the mod jar:
`theoreticalsciences:extreme/<name>`, `theoreticalsciences:compaction/<name>`, `theoreticalsciences:convergence/<name>`. JEI and EMI
show a recipe's id when advanced tooltips are on (F3+H).

```js
ServerEvents.recipes(event => {
  // remove one recipe, by id
  event.remove({ id: 'theoreticalsciences:extreme/cosmic_record_fragment' })
  // remove every recipe of a type that makes something
  event.remove({ type: 'theoreticalsciences:compaction', output: 'theoreticalsciences:amethyst_singularity' })
  // remove a whole type
  event.remove({ type: 'theoreticalsciences:convergence' })

  // change an ingredient or a result everywhere in a type
  event.replaceInput({ type: 'theoreticalsciences:extreme_shaped' }, 'minecraft:nether_star', 'minecraft:dragon_egg')
  event.replaceOutput({ type: 'theoreticalsciences:compaction' }, 'theoreticalsciences:amethyst_singularity', 'minecraft:budding_amethyst')

  // change a recipe completely: remove it, then add your version
  event.remove({ id: 'theoreticalsciences:convergence/breached_anomaly_engine' })
  event.recipes.theoreticalsciences.convergence('theoreticalsciences:breached_anomaly_engine', 'theoreticalsciences:exotic_matter_crystal',
      ['theoreticalsciences:superconducting_coil', 'theoreticalsciences:quantum_processor'])
    .energy(500000000).duration(600).tier(2)
    .id('mypack:breached_anomaly_engine')
})
```

Singularities made in the Compactor from section 9 are not recipe files. See how to turn them off under Section 9.

---

## 4. Machine properties (`ts_machines`)

One file per machine: `ts_machines/<machine id>.json`. The machine id is its block id without `theoreticalsciences:`
(`quark_gluon_collider`, `quantum_entangler`, `stellar_plasma_crucible`, ...). Every field is optional; leave out what
you don't want to change.

```js
ServerEvents.generateData('after_mods', event => {
  event.json('mypack:ts_machines/quark_gluon_collider', {
    energy_capacity: 500000000,
    speed_multiplier: 2.0,
    energy_multiplier: 1.5,
    max_parallel: 8,
    max_overclock: 4
  })
})
```

| Field | Available Machine types| Meaning (the mod's own value in brackets) |
|---|---|---|
| `energy_capacity` | all | FE the buffer holds. It still grows by itself if a recipe needs more per tick. |
| `max_energy_input` | consumers | FE/t accepted from cables (no limit). |
| `max_energy_output` | generators | FE/t pushed into cables (the machine's own rate). |
| `speed_multiplier` | all | Recipes take `duration / this` ticks (1). |
| `energy_multiplier` | all | Recipes draw `this ×` FE/t; generators produce `this ×` (1). |
| `max_parallel` | tuned multiblocks | Parallel recipes before the emitter tier multiplies it (1–8 depending on the machine). Max 4096. |
| `max_overclock` | tuned multiblocks | Overclock levels; each is 2× speed for 4× power (0–3). Max 16. |
| `coil_speed_bonus` | multiblocks with coils | Extra speed per coil tier above 1 (0.25). A tier's own `speed_bonus` (section 5) wins. |
| `integrity_wear_multiplier` | tuned multiblocks | How fast containment integrity wears (1). 0 = never. |
| `drift_multiplier` | tuned multiblocks | How strongly the ideal setting / physics disturbance drifts (1). 0 = never. |
| `dimensions` | all | The dimensions the machine works in, by id. `[]` = anywhere. (The Vacuum Fluctuation Harvester: `["theoreticalsciences:quantum_foam"]`; the Omnilex Engine: `["theoreticalsciences:hyperdimensional_reality"]`; every other machine: anywhere.) |

**Where a machine works.** `dimensions` applies to every recipe of the machine; a recipe's own `environment`
(section 2) can narrow it further. Built anywhere else, the machine stands idle and says *Only works in …*; its
tooltip and JEI/EMI page say so too.

```js
ServerEvents.generateData('after_mods', event => {
  // the Harvester also works in the Bulk
  event.json('mypack:ts_machines/vacuum_harvester', { dimensions: ['theoreticalsciences:quantum_foam', 'theoreticalsciences:the_bulk'] })
  // the Omnilex Engine works anywhere
  event.json('mypack:ts_machines/omnilex_engine', { dimensions: [] })
  // a Quantum Entangler that only works in the End
  event.json('mypack:ts_machines/quantum_entangler', { dimensions: ['minecraft:the_end'] })
})
```

The Transcendental and the Living Proof can only be entered the intended way (the Omnilex Engine's collapse and Alpha
Omega's challenge), whatever a pack does with machines. This is also checked by dimension id.

Machines already in the world pick up new properties right after `/reload`. Lowering `energy_capacity` never
deletes stored energy: the buffer shrinks as the energy is used. JEI/EMI will each recipe's own energy and
duration (before these multipliers). Lowering `max_parallel` or `max_overclock` also lowers machines previously set properties.

### Limits and extreme values

Every number is clamped, so no combination of settings can crash the game:

| Conditions | Limit |
|---|---|
| `speed_multiplier` | 0.0001 – 10,000 |
| `energy_multiplier` | 0 – 10,000 |
| `max_parallel` | 1 – 4,096. The emitter tier multiplies it, but a structure never runs more than **4,096 recipes at once**. |
| `max_overclock` | 0 – 16 (up to 65,536× speed for 4,294,967,296× power) |
| Fastest possible recipe | One finished cycle (× parallel) per tick. Extra speed beyond that is wasted, not an error. |
| Recipe `energy` | up to 2,147,483,647 FE/t. With overclock, parallel and multipliers the real draw may go higher; the buffer grows to match (up to about 4.6 × 10¹⁸ FE). A machine that can't be fed shows *No Power*. |
| Energy I/O | up to 2,147,483,647 FE per side per tick — the same as every FE mod |
| Item input slots | up to 2,147,483,647 items each |
| Item output | at most 99 per output entry in a recipe file (Minecraft's own limit); output slots stack to 64 |
| Fluid tanks | up to 2,147,483,647 mB each, inputs and outputs |

A recipe asking for more than a slot or tank can ever hold simply never starts.

---

## 5. Coil and emitter tiers (`ts_component_tiers`)

Many multiblocks have positions that take a *tiered* part — a **coil** or a **field emitter**. The mod's coils are
tiers 1–5 and its emitters tiers 1–3. A structure's tier is the **lowest** tier among all its coils (or emitters).

* **Coil tier:** recipes can require a minimum (`min_tier`, section 2). Each tier above 1 gives +25% speed. Machines
  with physics use the coil's superconductor limits (`sc_Tc`, `sc_B0`).
* **Emitter tier:** multiplies how many parallel recipes a tuned multiblock may run.

A file in `ts_component_tiers/` adds blocks to a family:

```js
ServerEvents.generateData('after_mods', event => {
  event.json('mypack:ts_component_tiers/dark_matter_coil', {
    family: 'coil',
    tier: 6,
    block: 'kubejs:dark_matter_coil',
    critical_temperature: 16000,
    critical_field: 20000,
    speed_bonus: 1.5
  })
})
```

| Field | Needed? | Meaning |
|---|---|---|
| `family` | yes | Type of coil, either `"coil"` or `"emitter"`. |
| `tier` | yes | Which tier this belongs to. A new tier (6, 7, ...) or an existing one (adds a block to the list). |
| `block` / `blocks` / `tag` | one type | One block id, a list of ids, or a block tag (no `#`). |
| `critical_temperature` | no (yes for coils) | Kelvin: the superconductor's critical temperature at this tier. |
| `critical_field` | no (yes for coils) | Tesla: its upper critical field. |
| `speed_bonus` | no | Speed at this tier = 1 + this (`1.5` = 2.5×). Default: +25% per tier above 1. |

What follows:

* **Every** structure position of that family, in every multiblock, now accepts your block at your tier. You never
  edit structure shapes.
* Listing one of the mod's own blocks replaces its usual tier.
* New coil tiers without the two critical values get them automatically: each tier above 5 is 4× the one below.
* The JEI/EMI structure preview cycles through your blocks and names them ("T6 Dark Matter Coil"). The controller
  screen and the structure report (sneak + right-click the controller with an empty hand) show the tiers.
* The Structure Assembler uses the best tier you carry, including yours.

---

## 6. Machine physics (`ts_physics`)

The multiblocks' physics models — the dials the operator sets, what must hold, how fast they run — are data at
`data/theoreticalsciences/ts_physics/<machine id>.json`, plus `coolants.json`.

To change one, **copy the whole file** out of the mod jar (it is a zip) to
`kubejs/data/theoreticalsciences/ts_physics/<machine id>.json` and edit the copy. It replaces the mod's. Use the
`theoreticalsciences` namespace; a copy under another namespace would compete with the original.

```json
{
  "params": [ {"id": "B", "unit": "T", "min": 0, "max": 60, "step": 0.01, "default": 1} ],
  "constraints": [ {"id": "rf", "kind": "stop", "expr": "qe*V/1000 - U0", "formula": "q·V ≥ U₀"} ],
  "rate": "...", "heat": "...", "rate_max": 4
}
```

* `params` — the dials. `min` / `max` are the range, `step` the click size, `default` the start. Widening a range is
  the usual edit.
* `consts`, `derived`, `states` — named quantities and their equations (`expr`). `formula` is only display text.
* `constraints` — `expr` must stay **above 0**. `kind` is what happens otherwise: `stop` (won't run), `wear` (wears
  4× faster), `quench` (sudden failure), `damage` (loses integrity every second).
* `rate` — speed (1 = normal), capped by `rate_max`. `heat` / `idle_heat` — heat into the coolant.
* Names any expression may use: `T`, `T_c`, `T_amb`, `coil`, `emitter`, `sc_Tc`, `sc_B0`, `oc`, `par`, `thr`, `d`, `on`.
* `coolants.json`: `"<fluid id>": {"temperature": K, "capacity": kJ per mB, "returns": "<warm fluid id>"}`.

A recipe's `physics` window (section 2) must stay reachable. Narrow a model too far and that recipe can never run.
Test with **Laplace's Demon** (creative item), which finds a working setting if one exists.

### Adding and removing physics

* **Every multiblock already has a model** (37 of them). To give one new dials, limits or quantities, edit its copy:
  add entries to `params`, `derived` or `constraints`. Recipes can then ask for windows on your new names.
* **Single-block machines can't have physics.** A model file for a machine without a structure is ignored. New
  machines can't be added from data at all; they need an addon (see `ADDONS.md`).
* **Remove a machine's physics** with a file that says so:

  ```js
  ServerEvents.generateData('after_mods', event => {
    event.json('theoreticalsciences:ts_physics/quark_gluon_collider', { remove: true })
  })
  ```

  The machine then runs without its console, on its plain tuning dial if it has one. Its recipes' `physics` windows
  are ignored.
* **Remove a coolant:** copy `coolants.json` and delete its entry.

### When a model is wrong

Mistakes in a model never crash the game:

* **A file that can't be read** is skipped with an error naming it in `logs/latest.log`. The machine keeps the mod's
  own model. This covers unknown names in an expression, a dial with `min` above `max`, a missing `default`, and
  `kappa`, `heat_capacity` or `timescale` of 0 or less.
* **Physics that can't be satisfied** — a window outside anything the dials can reach, contradictory limits — is
  allowed. The machine simply shows *Unstable* or *Out of window* and doesn't run.
* **Laplace's Demon on an impossible model** searches for at most 15 seconds, in the background (the game doesn't
  freeze). Then it reports that it couldn't find a working setting and leaves the machine at the closest point it
  found. If the model itself fails while being searched, the Demon says so in chat and the reason goes to the log.
* **Values that blow up** (division by zero, overflowing powers) count as "the physics breaks down here": that
  setting is unsafe, nothing more. If a model fails in some unforeseen way while a machine runs, that machine stops
  and the log names the model. The world keeps running.


---

## 7. Omnipotence Core Forge recipes (`ts_forge`)

The reborn Omnipotence Core's Forge (and an awake forged Core) crafts from the player's inventory. Its recipes can be
added, replaced and removed.

**Add** (`mypack:ts_forge/dragon_egg` gets the id `mypack:dragon_egg`):

```js
ServerEvents.generateData('after_mods', event => {
  event.json('mypack:ts_forge/dragon_egg', {
    result: { item: 'minecraft:dragon_egg', count: 1 },
    ingredients: [
      { item: 'theoreticalsciences:omnipotent_ingot', count: 16 },
      { item: 'minecraft:dragon_breath', count: 64 },
      { item: 'theoreticalsciences:rekindled_spark', count: 0 },   // 0 = must be carried, never used up
      { universe: 'minecraft', count: 1 }                     // a Simulated Universe of that mod
    ]
  })
})
```

* 1 to 32 ingredients; result count 1 to 64.
* A recipe naming an item that doesn't exist, or a Simulated Universe of a mod that isn't installed, is skipped (the
  log says so). A universe of any installed mod works, vanilla `minecraft` included: the Forge adds that universe's
  own recipe automatically.
* Every recipe gets its own "constants of nature" puzzle, worked out from its id.

**Replace** one of the mod's recipes: use the `theoreticalsciences` namespace and the recipe's name, e.g.
`theoreticalsciences:ts_forge/omnipotent_ingot`. It takes the original's place in the list.

The mod's own recipe names: `omnipotent_ingot`, `transcendence_crystal`, `absolute_singularity`, `eschaton_core`,
`omnipotent_edge`, `singularity_scepter`, `crown_of_transcendence`, `mantle_of_omnipotence`, `greaves_of_the_absolute`,
`treads_of_infinity`, `ex_nihilo_engine`, `entropy_censer`, `equilibrium_lattice`, `lawgivers_codex`, `resonant_shards`,
`echo_replay`, `second_spark`, `rekindled_heart`, `echo_lattice`, `omnipotence_seed`, `demiurge_hand`,
`dodecaract_excavator`, `eschaton_cell`, `forged_omnipotence_core`, `euclidean_anchor`. There are also generated ones:
`simulate/<mod id>` (that mod's Simulated Universe) and `creative/<item id>` (one per creative item, e.g.
`creative/mekanism:creative_bin`).

**Remove** by id (with or without `theoreticalsciences:`):

```js
event.json('mypack:ts_forge/removals', { remove: ['echo_lattice', 'creative/mekanism:creative_bin'] })
// or remove the recipe with the file's own id:
event.json('theoreticalsciences:ts_forge/echo_lattice', { remove: true })
```

A hardcoded limitation: a forged Core can never forge another Omnipotence Core or a Hand of the Demiurge, through
your recipes either. The Core's screen, the JEI/EMI "Omnipotence Forge" pages and the Codex all show the merged list.

---

## 8. Research (`ts_research`)

The research tree is found under: `ts_research/<name>.json`, the research id being `<namespace>:<name>`. Add your own
entries, or replace one of the mod's by using its id (`theoreticalsciences:ts_research/quantum_entanglement`).

```js
ServerEvents.generateData('after_mods', event => {
  event.json('mypack:ts_research/advanced_containment', {
    title: 'Advanced Containment',
    description: 'Unlocks the pack\'s own containment machines.',
    icon: 'theoreticalsciences:quarantine_field_matrix',
    requires: ['theoreticalsciences:containment_protocols'],
    insight: 400,
    cost: [{ item: 'theoreticalsciences:anomaly_specimen', count: 4 }],
    time: 600,
    x: 30, y: 12
  })
})
```

| Field | Default | Meaning |
|---|---|---|
| `title` | (required) | Name shown on the research screen. |
| `description` | `""` | Descriptive flavour text. |
| `icon` | `minecraft:book` | Item shown on its node. |
| `category` | `general` | Which Chapter/field it falls under. |
| `requires` | `[]` | ID of pre-requisite Research . |
| `insight` | 0 | Insight costs (0 – 1,000,000). |
| `cost` | `[]` | Items consumed: `{item: ..., count: n}` or `{tag: ..., count: n}`. |
| `time` | 0 | Seconds of study after paying (0 = instant). A Research Nexus shortens it. |
| `x`, `y` | 0 | Position on the research map. |
| `hidden` | false | Hidden until every prerequisite is done. |
| `grant_only` | false | Cant be researched, only granted by a script or command |
| `puzzle` | none | A derivation puzzle (see any of the mod's research files for the format). Leave it out for none. |

Gate any recipe behind it with `.research('mypack:advanced_containment')` (section 2), or grant it from a script
(section 12).

---

## 9. Singularities (`ts_singularities`)

Add singularities of your own (like Avaritia's), made in the Singularity Compactor. The Omniscience Catalyst then
needs one of each.

```js
ServerEvents.generateData('after_mods', event => {
  event.json('mypack:ts_singularities/osmium', {
    tag: 'c:ingots/osmium',      // or item: 'mekanism:ingot_osmium'
    count: 16384,                // how many per Singularity
    name: 'Osmium Singularity',
    core: '#30405a',             // optional colours; if empty it will be auto taken from the item's texture
    glow: '#a0c8ff',
    enabled: true                // disables the generation of this singularity
  })
})
```

* Every ingot (`c:ingots`) of every other mod gets a singularity automatically. A file of yours that covers an ingot
  takes the place of that ingot's automatic one. To stop automatic ones, use the config (`autoSingularities`,
  `autoSingularityBlacklist` in `theoreticalsciences-common.toml`).
* With more singularities than the Extreme Assembly Table can hold, they are condensed nine at a time into
  **Singularity Clusters** (and clusters into Hyperclusters). Name the clusters with
  `ts_singularities/_clusters.json`: `{ names: { '1.0': 'Ferrous Cluster', '1.1': '...' } }`.

---

## 10. Tags from this mod

Change them with `ServerEvents.tags`:

```js
ServerEvents.tags('item', event => {
  event.add('theoreticalsciences:hidden_from_viewers', 'kubejs:debug_block')
  event.add('theoreticalsciences:beyond_omnipotence', 'kubejs:final_relic')
  event.add('theoreticalsciences:forge_only', 'somemod:ultimate_reactor')     // only the Core's Forge makes it
  event.add('theoreticalsciences:forge_exempt', 'somemod:creative_mind_tea')  // a normal item despite its name
})
ServerEvents.tags('block', event => {
  event.add('theoreticalsciences:excavator_ores', 'mypack:weird_ore')
  event.add('theoreticalsciences:not_accelerated', 'mypack:timer_block')
})
ServerEvents.tags('entity_type', event => {
  event.add('theoreticalsciences:not_spawnable', 'mypack:boss')
})
```

| Tag | Type | What it does |
|---|---|---|
| `theoreticalsciences:hidden_from_viewers` | item | Left out of JEI's and EMI's item lists. |
| `theoreticalsciences:beyond_omnipotence` | item, fluid | The Omnipotence Core cannot materialize it, and creative duplicators refuse it. |
| `theoreticalsciences:forge_only` | item | Only the Omnipotence Core's Forge makes it: it gets its own generated Forge recipe, and the Core's "make anything" screen refuses it. For creative-only items the automatic detection misses, or late-endgame items you want behind the Forge. Its other recipes are not removed. |
| `theoreticalsciences:forge_exempt` | item | Taken off the Forge-only list: for items the automatic detection picks up by mistake (an id containing "creative" that is a normal item). |
| `theoreticalsciences:creative_duplicator_parts` | item | Parts that turn storage into a duplicator (creative storage cells, vending upgrades). They are stopped from copying anything beyond Omnipotence. |
| `theoreticalsciences:excavator_ores` | block | Extra blocks the Dodecaract Excavator's Ore Extraction takes. |
| `theoreticalsciences:excavator_ignored` | block | Blocks the Excavator never breaks. |
| `theoreticalsciences:not_accelerated` | block | Blocks the Chronal Accelerator never speeds up. |
| `theoreticalsciences:observable` | block, entity_type | What the Spectral Analyzer can study. |
| `theoreticalsciences:not_spawnable` | entity_type | Never summoned by the Lawgiver's Codex (bosses are excluded already). |

---

## 11. Events

Server script events, group `TheoreticalSciencesEvents`:

```js
// a machine is about to start a recipe - cancel to stop it
TheoreticalSciencesEvents.recipeStarted(event => {
  if (event.machineType == 'kugelblitz_chamber' && event.level.isRaining()) event.cancel()
})

// a machine finished a recipe
TheoreticalSciencesEvents.recipeFinished(event => {
  if (event.owner) event.owner.tell(`Your ${event.machineType} finished ${event.recipeId}`)
})

// a player unlocked a research
TheoreticalSciencesEvents.researchUnlocked(event => {
  if (event.research == 'theoreticalsciences:quantum_foam') event.player.give('minecraft:diamond')
})

// a multiblock formed (event.formed true) or broke (false)
TheoreticalSciencesEvents.multiblockFormed(event => {
  console.log(`${event.machineType} at ${event.pos}: ${event.formed ? 'formed' : 'broken'}`)
})
```

| Event | Fields |
|---|---|
| `recipeStarted` (cancellable), `recipeFinished` | `machine` (block entity), `machineType`, `recipeId`, `recipe`, `pos`, `level`, `owner` (the player who placed it; null if offline) |
| `researchUnlocked` | `player`, `research` |
| `multiblockFormed` | `formed`, `machineType`, `pos`, `level` |

---

## 12. The `TheoreticalSciences` helper

Available in every script:

```js
TheoreticalSciences.hasResearch(player, 'theoreticalsciences:quantum_entanglement')   // true / false
TheoreticalSciences.grantResearch(player, 'theoreticalsciences:quantum_entanglement')
TheoreticalSciences.revokeResearch(player, 'theoreticalsciences:quantum_entanglement')
TheoreticalSciences.getInsight(player)
TheoreticalSciences.addInsight(player, 100)
TheoreticalSciences.researchIds()                                                // every research id

// example: a head start for new players
PlayerEvents.loggedIn(event => {
  if (!TheoreticalSciences.hasResearch(event.player, 'theoreticalsciences:quantum_foundations'))
    TheoreticalSciences.addInsight(event.player, 5)
})
```

---

## 13. Text, names and the Quantum Codex

These are client assets, so they go in `kubejs/assets/` (KubeJS loads it like a resource pack):

* **Any name, tooltip or message:** a language file `kubejs/assets/theoreticalsciences/lang/en_us.json` containing only
  the keys you change, e.g. `{"item.theoreticalsciences.qubit": "Quantum Bit"}`. Every item's flavour line is
  `<item key>.lore`.
* **The Quantum Codex** (the mod's guide book) reads `assets/theoreticalsciences/ts_codex/`: `index.json` (tabs and
  categories), `entries.json` (every entry and its pages) and one map file per tab. A copy at
  `kubejs/assets/theoreticalsciences/ts_codex/<file>` replaces the mod's file of that name, so start from the mod's copy and
  edit it. Entries unlock with the research named in their `research` field.

---

## 14. Worked example: a tier 6 coil

**Step 1 — the block** (startup script; needs a full restart):

```js
// kubejs/startup_scripts/coils.js
StartupEvents.registry('block', event => {
  event.create('dark_matter_coil')                  // becomes kubejs:dark_matter_coil
    .displayName('Dark Matter Coil Block')
    .soundType('metal')
    .hardness(6)
    .resistance(12)
    .requiresTool(true)
    .tagBlock('minecraft:mineable/pickaxe')
})
```

Its texture goes in `kubejs/assets/kubejs/textures/block/dark_matter_coil.png`.

**Step 2 — make it tier 6, and use it** (server script; `/reload` is enough):

```js
// kubejs/server_scripts/theoreticalsciences_data.js
ServerEvents.generateData('after_mods', event => {
  event.json('kubejs:ts_component_tiers/dark_matter_coil', {
    family: 'coil', tier: 6, block: 'kubejs:dark_matter_coil',
    critical_temperature: 16000, critical_field: 20000
  })
})

ServerEvents.recipes(event => {
  // a recipe that needs the new tier
  event.recipes.theoreticalsciences.plasma_smelting(['2x minecraft:netherite_ingot'], [], ['4x minecraft:ancient_debris'], [])
    .energy(65536).duration(400).minTier(6)

  // and a way to make the coil
  event.recipes.theoreticalsciences.extreme_shaped('4x kubejs:dark_matter_coil',
    ['TTT', 'TDT', 'TTT'], { T: 'theoreticalsciences:transfinite_coil_block', D: 'theoreticalsciences:dark_matter_lens' })
})
```

Build a Stellar Plasma Crucible with every coil position filled with Dark Matter Coil Blocks. The controller shows
"Coils T6" and the recipe runs.

---

## 15. Troubleshooting

* **Nothing changed after `/reload`:** check the path. The folder name (`ts_machines`, `ts_forge`, ...) must be exactly
  as written, directly under `data/<namespace>/`. Check `logs/latest.log` for a message naming your file.
* **A replaced file has no effect:** replacing needs the `theoreticalsciences` namespace and the mod's exact file name.
* **A recipe never runs:** check JEI/EMI for its conditions — research, `min_tier`, a physics window, a required
  dimension or field environment. Laplace's Demon (creative) shows whether a physics window can be reached at all.
* **A new block isn't accepted in a structure:** blocks come from startup scripts (restart); their tier from server
  data (`/reload`). Both are needed.
