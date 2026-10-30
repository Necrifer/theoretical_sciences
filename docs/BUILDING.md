# Building Theoretical Sciences

## The jar

Requirements: **JDK 21** and an internet connection (for Gradle, NeoForge and the Maven dependencies).

1. Put the three compile-only jars listed in [`libs/README.md`](../libs/README.md) into `libs/`.
2. Run:

   ```
   ./gradlew build        (Windows: gradlew.bat build)
   ```

3. The jar is in `build/libs/theoreticalsciences-<version>.jar`.

`./gradlew runClient` starts a development client with the mod loaded.

The version number is `mod_version` in `gradle.properties`.

## Changing content

Everything the mod uses is in `src/`, as ordinary Java, JSON, PNG and OGG files: edit those directly and rebuild.
The JSON files (recipes, research, physics models, multiblocks, loot tables, tags, the language file) were produced
by the author's own content tools, which are not part of this repository. They are kept tidy and readable so that
you can edit them by hand.

To change the mod **without** rebuilding it (recipes, research, machine physics, multiblock shapes), use a datapack
or KubeJS instead: see [`DATAPACKS.md`](DATAPACKS.md) and [`KUBEJS.md`](KUBEJS.md). For a separate mod that builds
on this one, see [`ADDONS.md`](ADDONS.md).

## Where things live

| Folder | What |
|---|---|
| `src/main/java/com/theoreticalsciences/` | the mod: `machine/` (machines, tuning, physics), `research/`, `transcend/` (the Transcendental and the Omnipotence Core), `endgame/`, `world/` (dimensions), `client/` (screens, renderers, the Codex), `compat/` (JEI, EMI, Jade, KubeJS, AE2), `custom/` (the KubeJS / datapack folders) |
| `src/main/resources/assets/theoreticalsciences/multiblocks/` | structure definitions |
| `src/main/resources/data/theoreticalsciences/ts_physics/` | the multiblocks' physics models |
| `src/main/resources/data/theoreticalsciences/ts_research/` | the research tree |
| `docs/KUBEJS.md` | customisation guide for modpack developers |
