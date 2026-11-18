# Theoretical Sciences 1.3.0

Everything since 1.2.0 (3 Forgotten Memories). The full list, with every recipe change and number, is in
[CHANGELOG.md on GitHub](https://github.com/Necrifer/theoretical_sciences/blob/main/CHANGELOG.md).

## Before updating

* **Empty the Cantor Diagonalizer, Axiom Distiller, Convergence Engine and Pleroma Confluence.** They are now the
  controllers of multiblocks, and what an old one held (items, fluids, energy) is not carried over.
* **Eight endgame structures have new shapes** and stop working until rebuilt. The Structure Assembler takes an old
  build apart, and every new shape uses at least as many of each block as the old one, so nothing you built is lost.
* **Recipes have changed.** Every endgame recipe now takes Field Syntheses, the middle of the progression is much
  cheaper, and many machines gained new and cheaper routes. JEI or EMI shows the current recipes.

## New branches and concepts

Every new concept comes with a console window driven by a real equation, a derivation puzzle and a material chain
that leads into the endgame.

**Fluids and Flight.** Fluid dynamics, aerodynamics and compressible flow, with the Wind Tunnel, the Laminar Flow
Conduit and the MHD Generator.

**Into Orbit.** Rocketry (Liquid Oxygen and Liquid Hydrogen, the Rocket Assembly), orbital mechanics and the
**Orbital Station**, a new dimension. The Jet Stream above the Accretion Frontier, with the Kármán Wraiths, and the
Bussard Ramscoop.

**The Living Planet.** Oceanography and ocean waves, with the **Hadal Trench**, a new dimension at the bottom of the
Overworld's sea, its vent fields, Hadal Anglerfish and Siphonophore Colonies, and the Wave Energy Converter.
Meteorology, volcanology and glaciology, with the Storm Chamber, the Storm Elemental, the Ice Core Drill and the
Geothermal Plant.

**The Green Kingdom.** Plant physiology, plant hormones, plant breeding, soil science and food science: the
Hydroponic Tower, the Phytotron, the Breeding Bench, Biochar, Humus, Living Soil and Field Rations (the Well Fed effect).

**Deep Time.** Three new Overworld ores (Fossil, Amber, Stromatolite), radiometric dating, ancient DNA and
de-extinction. The Revival Vat brings back the **Woolly Mammoth** and the **Meganeura**. New ruins: Dig Sites and
Greenhouse Ruins.

**Mind and Language.** The action potential, synaptic plasticity, psychophysics, linguistics and integrated
information, with eight consoles, the Neural Cortex (a new region of Inner Space), Library Ruins and the Low-Light Visor.

**Engineering of the Large.** Structural mechanics, composite materials, tribology and corrosion science: the Load Test
Rig, I-Beams, the Composite Layup Press, carbon-fibre and nanotube composites, wear coatings and sacrificial anodes.

**Small Wonders.** Acoustic resonance, space weather (a new world event: geomagnetic storms), chaos theory, knot
theory, cellular automata, complexity theory, quantum computing, pharmacology (two medicines) and medical imaging.

**Archaeology.** The **Buried Tell**, a new Overworld site with finds you brush out of the ground, the
Ground-Penetrating Radar, luminescence dating, tree-ring dating, conservation, isotope provenance, the Bloomery Shaft
(Bloomery Steel) and photogrammetry.

**Space, from launch to the edge of the universe.**
* Getting there: every destination has a delta-v and transfer windows. The **Mission Planner**, Transfer Stages and
  the Propulsion Test Stand. The **Mission Bay** sends probes for samples. Radiation in space, and one line of space
  suits that ends in the Spacefarer Suit.
* The station grows: station modules (Microgravity Factory, Habitat Ring, Storm Shelter), satellites, space solar power
  beamed to a Microwave Rectenna, and orbital debris (on by default, `spaceDebris` in the config turns it off).
* The Sun and small bodies: light sails (Radiation Pressure Balance, Laser Sail), the Sun's magnetism, magnetospheres
  and the Active Magnetic Shield, asteroids by type and comets, the nebula's gases, helium-3 and the Tokamak.
* Young worlds and deep sky: icy moons and the **Plume Field**, how planets form and the **Protoplanetary Disk**,
  habitability, the oldest light (the CMB Bolometer Array), radio flashes (Radio Telescope, Burst Observatory) and the
  search for others, which ties the space branch to the Omnipotence Core.
* With **Ad Astra** installed: recipes for Ad Astra's metals, and Ad Astra's space suits hold off the station's vacuum.

## The endgame, reworked

* **Field Syntheses:** the endgame now needs every field. Seven Field Syntheses, each made by one field's machines, go
  into every endgame recipe.
* **Signature materials:** every endgame structure makes one material nothing else can, and each tier needs the tier
  below. Eight structures have new shapes, and four machines became multiblocks.
* **The Consilience Core:** one last material that needs every field.
* **Recreating Home**, an expansion beyond the end, opened only after every recipe of the Core Forge has been forged
  once: the Empyrean Assembly Table (a 15 by 15 table), forty two materials, the Offering Altar, the Homeward Portal
  and **Omnipotency**, a new dimension.
* **Those Who Live There:** Omnipotency is inhabited, with the Halo Lamb, Cherub, Choir Seraph, Cloud Ray, Dweller and
  Threshold Ophan, and new structures (Wayside Shrines, Hamlets, Choir Halls, the Temple of Remembrance), blocks, the
  Remembrance Tablets and a music disc.
* **Magic mods (optional):** Botania mana, Nature's Aura, Thaumaturge (vis, aspects, flux) and Astral Sorcery (Lumen)
  can give their own mechanics to recipes, with the Mana Port. Without those mods nothing changes.

## Progression and balance

* The middle of the progression is far cheaper: a machine recipe now costs at most four times its dearest ingredient.
  Large counts are kept for the late game and endgame.
* No machine on the main path is built for a single craft any more.
* The Antiverse, the Quantum Foam and the Black and White Hole Interiors each gained two new biomes.
* The last 25 console windows that were left too wide are narrowed, and recipes were added to the machines that had
  only one or two.
* For testing and for packs: `/theoreticalsciences testkit` gives a kit of items and grants its research.

## Learning the science

* **Consoles explain before they calculate.** Each quantity is explained in words first, with its own colour. Its
  equation appears after you have watched that machine finish one cycle (a client config option shows them all at once).
* **"In plain words"**: the hardest Codex entries (sets and infinities, imaginary numbers, quantum physics, general
  relativity) open with a plain sentence, an everyday comparison and where that comparison stops being true.
* Every console has its own animated simulation, and every machine has new art and working sounds.
* Animated items are smoother (32 frames with light motion blur).

## Working with other mods

* **Fluids:** recipes accept other mods' versions of the shared chemicals: crude oil (Immersive Petroleum, Oritech,
  Ad Astra, GregTech's four oils), ethylene, propylene, methane, methanol, Liquid Oxygen and Liquid Hydrogen
  (GregTech, Mekanism), Liquid Sodium, radon, helium-3, carbon monoxide, hydrogen cyanide and uranium hexafluoride.
  The mod's own fluids carry the common tags too.
* **Ingots:** every ingot of the mod has a tag (`theoreticalsciences:ingots/<material>`), and recipes take other mods'
  ingots of the same metal (lithium, aluminium, steel, neutronium).
* Immersive Petroleum no longer places oil and lava reservoirs in the mod's dimensions.
* **Anima Mundi config:** `animaMundiNeedsModEssences` and `animaMundiSkipMods` choose whether, and which, other
  mods' Essences Anima Mundi needs.

## Security and performance

An outside review for security and performance, done before release. Nothing here changes recipes or how things look.

* **Security:** a large Structure Assembler check can no longer disconnect a player. Attuning a Forged Core and
  launching from a Mission Bay now need the right screen and respect claims. The Omnipotence Core no longer makes admin
  or unbreakable blocks (packs add more to `theoreticalsciences:not_materializable`). Right-clicking a full pile no
  longer deletes it, and saved data is bounded when loaded.
* **Server:** huge multiblocks are checked over several ticks instead of one, running machines sync far less often,
  neighbour and attribute updates happen only when something really changes, arrivals on Titan, Inner Space and the
  Accretion Frontier no longer generate chunks while searching for land, and closing a world frees its memory.
* **Client:** glows and fixed sky parts are drawn in far fewer calls, lensing copies only part of the screen, animated
  textures store each frame once (5,066 fewer frames, pixel for pixel the same), and machine sounds no longer go
  silent until a relog.
* **Sodium:** keep "Animate only visible textures" on. Moving machine parts now tell Sodium their textures are in use,
  so they keep animating.

## Fixes

* A crash in Omnipotency when a creature looked for a Choir Bell or a Sealed Urn.
* Planetary Interior gem pockets could grow past the world generation limit and stall large packs.
* Codex nodes no longer overlap.
* The Structure Assembler now really marks obstructions (a pulsing red outline).
* The Mission Planner and Mission Bay screen no longer overlap their text.
* The Empyrean Assembly Table is safe from chunk banning (its saved data stays a few kilobytes).
