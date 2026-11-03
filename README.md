# Theoretical Sciences

**An endgame tech mod at the edge of theoretical physics — NeoForge 1.21.1.**

Entangle qubits, trap antimatter, condense Bose-Einstein matter, forge micro black holes, fold space into tesseracts,
harvest the quantum foam, prove theorems to unlock machines, and finally take apart and rebuild the Omnipotence Core
at the end of everything.

![Theoretical Sciences](src/main/resources/theoreticalsciences_logo.png)

**Download:** [CurseForge](https://www.curseforge.com/minecraft/mc-mods/theoretical-sciences) · **Bugs and suggestions:** [Issues](https://github.com/Necrifer/theoretical_sciences/issues)

## What is in it

* **127 machines, 70 of them multiblocks.** The big ones run on real physics models: the operator sets fields,
  temperatures and beam energies on a console, and each recipe needs its quantities held inside a window. Tuned
  multiblocks drift, wear their containment, overclock and run parallel recipes. A failing containment releases
  hazards.
* **A research tree of 232 entries**, with seven branches added in 1.1: astrophysics, Earth & mapping, nuclear
  science, electromagnetism, general relativity, optics and probability. Many entries are unlocked by solving a
  derivation puzzle (build the equation, find the answer) with values that differ in every world.
* **Twelve dimensions:** the Quantum Foam, the Bulk, Hyperdimensional Reality, the Platonic Realm, the Transfinite
  Hierarchy, the Antiverse, black and white hole interiors, the Living Proof, the Transcendental, the Planetary
  Interior and the Accretion Frontier. Each has its own laws, creatures and soundtrack.
* **The endgame:** the Omnilex Engine's impossible recipe opens the Transcendental; Alpha Omega waits in the Living
  Proof; the Omnipotence Core can materialize and forge anything — until its Shutdown Sequence, the Sundering and the
  Rekindling. After that come tools, armour and weapons that run on Eschaton Charge, and an Omnipotence Core of your
  own that must be attuned to every place it stands in.
* **Singularities for every mod:** every ingot of every installed mod gets one automatically.
* **The Quantum Codex:** the mod's own guide book, revealed as you progress, with every recipe in it.
* **About 1,260 items and blocks, 69 fluids, 35 creatures**, all with generated art and a synthesised soundtrack.

## Compatibility

* No mixins, no access transformers, no coremods, no required dependencies.
* Standard FE, item and fluid capabilities on every machine and on Interface Ports.
* Optional integrations: **JEI** or **EMI** (every recipe, 3D structure previews), **Jade** (machine status, recipe
  and progress), **KubeJS** (see below),
  **AE2** (Interface Ports join an ME network).

## For modpack developers

Recipes, research, machine numbers, structure parts, the Forge and singularities can all be changed with a plain
datapack or with KubeJS:

* **[docs/DATAPACKS.md](docs/DATAPACKS.md)** — every recipe format and data folder, with examples.
* **[docs/KUBEJS.md](docs/KUBEJS.md)** — the same from KubeJS scripts, plus events and helpers.

Addons are welcome: **[docs/ADDONS.md](docs/ADDONS.md)** (and the licence below).

Server config: `config/theoreticalsciences-common.toml` (energy, duration and ingredient multipliers, drift and wear,
research and puzzles, singularities, Chronal Accelerator). Client config:
`config/theoreticalsciences-client.toml` (gravitational lensing, structure distortion, sky, particles).

Commands (operators):

```
/theoreticalsciences research grant|revoke <player> <id|*>
/theoreticalsciences research list <player>
/theoreticalsciences insight add <player> <amount>
```

## Building

See **[docs/BUILDING.md](docs/BUILDING.md)**.

## License

Theoretical Sciences is made by **Innight** and released under the [MIT License](LICENSE).

You're free to use, modify, copy and share it: in modpacks, videos and streams, addons, ports,
or your own projects, including commercial ones. The only requirement is that the copyright
notice and the MIT License text stay with any copy or substantial portion of the mod.

**Appreciated, not required:**

- a link back to the mod's [CurseForge page](https://www.curseforge.com/minecraft/mc-mods/theoretical-sciences) when you feature or redistribute it;
- credit to Innight in the description and mod info of addons and ports;
- downloading the mod from CurseForge, which supports its development.

Minecraft is a trademark of Mojang Studios. Theoretical Sciences is not affiliated with or endorsed by Mojang or Microsoft.
