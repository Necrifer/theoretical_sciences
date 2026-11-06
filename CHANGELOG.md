# Changelog

## 1.2.0 — 3 Forgotten Memories

The first release since 1.1.0, in three parts, newest first: Biology, Mathematics, Chemistry. (They were built
internally as three development versions, never released on their own.)

### Part three: Biology

The branch about life as it runs. Crafting counts are for a 3x3 layout; on the Extreme Assembly Table each recipe is
laid out on the 9x9 grid, so each ingredient is needed about nine times over.

**New item art (the Chemistry, Mathematics and Biology items)**
- Every item added since 1.1 has its own drawing now, in place of a recoloured copy of an older icon. Each one shows the
  thing itself: the Lead Pig is a lead pot with a trefoil band, Halite is a pile of salt cubes, the Vanadium Bed is a
  heap of catalyst rings, Kekulé's Dream is the snake biting its tail in a hexagon, the e Crystal is a nautilus spiral,
  Half Turn is a Möbius band, Stability Margin is a ball resting in a bowl, Geodesic Thread is a globe with a great
  circle, and the biology items are cells, dishes, vials and organs.
- No recipes changed.

**Three new mechanics**
* **Contamination, the Biology failure.** Biology machines do not explode; they get contaminated. Each run handles the
  culture and costs 4% sterility; below half, a run may catch something and the machine stops until it is cleaned. A
  **Sterile Pack** (from the Autoclave) always cleans it. An **Antibiotic** is cheaper, but each dose on the same machine
  selects for what survives, so the n-th works only one time in n. A **Probiotic Seal** upgrade keeps a console clean for
  good.
* **Living cultures.** Yeast, algae, cells and microbes are alive: carried loose or dropped they die off (half in a few
  in-game days of carrying), while machines and chests keep them. A dead culture is no use to any recipe. This reuses the
  1.2 decay system.
* **Infection and immunity.** The creatures a failed machine releases now leave a lasting infection when they hit you
  (ten minutes, it runs its course): Fracture (Destabilised Husk, you slip through space), Grey Goo (Nanite Swarm, your
  food burns), Rewind (Paradox Echo, back ten seconds), Hollowing (Void Wraith, six less health). An **Anomaly Ward**,
  used on yourself, holds its infection off for three in-game days; renew it too soon and the imprint overloads. After
  Immune Memory, a lasting Ward holds for good. Immunity is kept per player and survives respawn.

**After testing (Inner Space and Titan's hidden ocean)**
* Creatures now spawn in both worlds: by the world's own spawning (each biome lists its creatures, and Inner Space's do
  not wait for darkness, since it is always lit), and by a top-up near every player, creative or not.
* Inner Space has no caves now: caves below the plasma's level had filled with plasma, so it turned up inside the ground.
* Every Inner Space block squelches like a slime block.
* The Tumour Hydra is far larger (as wide as a house), its heads growing out of every side on long necks; each head bites
  whoever comes near it. How to call it is told on the Metastatic Spore, in the Inner Space research, and when you first
  reach the Tumour.
* New models for every new creature in the game's own style (chunky shaded blocks on the sixteen-pixel grid, as vanilla's
  mobs are), facing the way they move, and a voice of its own for each (the Polymerase ratchets like a zip;
  the grazers snap like snapping shrimp; the hydra growls with several throats).
* The Polymerase is redrawn: a hand-shaped enzyme closing on the double helix, writing a new strand out of its back.
* Titan's hidden ocean: real vents (mounds of rock, tapering chimneys, and Black Smokers on top that pour out roaring,
  bubbling black plumes which lift and scald whoever swims in), Giant Tube Worms round them (spreading near a smoker),
  Brinicles hanging from the ice (growing longer), boulders on the floor, and two new creatures: the Brine Jelly
  (drifts and stings) and the Vent Crab (a yeti crab that farms bacteria on its claws). New drops: Luminous Mesoglea and
  Crab Chitin. The Brine Jelly swims bell-first in strokes: the bell snaps shut and drives it along, its tentacles
  streaming behind, then opens wide as it coasts.
* Every new block has a use and a way to make more: Bioluminescent Membrane grows algae in the Photobioreactor, Biosafety
  Panels make Sterile Packs in the Autoclave, the three Inner Space ores can be laid again, tube worms give Bacterial
  Cultures, brinicles melt to Concentrated Brine, black smokers give Sulfuric Acid, chitin makes Polymer Sheet.
* Livelier creatures: the Tumour Hydra's body throbs, its necks writhe along their length and its heads rear back and
  strike now and then, jaws snapping; the Vent Crab shuffles, waves and snips its claws and swivels its eyes, and its
  legs step properly when it walks (it is also faster, and its body no longer pokes into walls); the T Cell wobbles as it goes.
* The Brine Jelly turns to face the way it swims, so it always goes bell-first.
* T Cells, Macrophages, Polymerases and the Hydra only spawn standing on solid ground (they had been appearing in mid-air
  and dying from the fall), and Inner Space's cells take no fall damage.
* The Helix Spires are built whole: backbones three blocks thick and a base pair on every level.
* The Polymerase now works the DNA: it finds the nearest helix, walks to it and climbs it rung by rung, copying as it goes
  (its clamp shuts and the strand races through), turns at the top and works back down. Knocked off, it climbs back on.
* The Tumour Hydra fights with its heads: its body only walks to keep them in reach (about five blocks off), and the
  head nearest whatever is in reach draws back and strikes. Each head has a hit box of its own: strike the heads to cut
  them (a blow to the body is mostly soaked up, and cuts nothing). It cannot be called into a cramped spot (it needs
  open ground to rise on) and it never suffocates. Macrophages and T Cells go for it, and it bites them back.
* The Vent Crab's walk is calmer: slower, steadier steps instead of a jitter.
* Fixed: three crafting recipes failed to load (the Lead Pig and both Entanglement Tuner recipes made more than a stack of
  an item that does not stack); each now makes one.
* Tumour Hydra: each head now has its own fixed place round the body, so cutting one no longer shuffles the rest. The stump
  is left right where the head was struck off: the cut neck stays standing, bleeding at its end, and that is where the
  therapy strike lands.
* Tumour Hydra: a blow anywhere along a neck counts as a blow to that head (each neck has two hit boxes besides the head).
  Once every head is gone, blows to the body land in full, so the Hydra can be finished off.
* Untreated stumps regrow their own head plus one in the nearest empty place beside it.
* Vent Crab: turns gradually instead of snapping to each new direction, walks the way it faces, pauses to pick at the rock,
  steps up ledges instead of hopping, and no longer bobs on the floor. Its legs only step while it is really walking, and
  stand still otherwise.

**Animated multiblocks (the mod's own)**
* A structure animation system: a multiblock's own parts can move (rings turning, arms swinging, parts sliding), each
  drawn from the real models of the blocks it is made of, carried by its parent part's movement, and set up from a data
  file alone (`assets/<ns>/ts_animations/`). Parts are shown whenever the machine is formed, move only while a recipe
  runs, ease in and out over their own ramp, and hold where they stopped. A multiblock's JSON names one with `animation`.
* Large effects for the mod's multiblocks to use (`fx` entries `star`, `shell`, `orbiters`, `beam`): a star (three
  turning layers of granulated plasma, drawn by a new shader), a sphere of starry open space, small bodies orbiting, and
  beams. They show while a recipe runs, with the same start-up and shut-down timing as the mod's own machines.
* `/tsanim`, a client command to preview animations and effects where you stand, and to see what they cost per frame.
* Client config: `animatedMultiblocks` (on), `animationDistance` (256 blocks), `animationFreezeDistance` (160 blocks),
  `movingPartsCompatibility` (off; on, or with Sable installed, moving parts are drawn through the game's block buffers).
* **The mod's machines move where a real machine would**: the Gas Centrifuge's rotors spin; the Tectonic Press's ram
  strikes; the Deep Drilling Rig's drill string turns and bobs; the Radio Telescope's dish slews and tilts; the
  Reflecting Telescope's dome turns while its tube tilts; heliostats track, control rods ride, pumps and compressors
  spin, valves swing, trays and coils lift and settle, and the warp core's and accretion disc's rings turn. Coils, lens
  arrays, field emitters and stabilisers hold still (an earlier build of this release set them all turning, which made every
  machine look alike). The list is in tools/out/moving_parts.txt.
* Nothing has to be rebuilt and no structure changed: the moving blocks are the structure's own, still there (they
  collide, drop and count for the structure). While it is formed they are drawn by the renderer, moving, instead of
  in place; when it breaks they show as ordinary blocks again. Machines already built pick it up on their next
  structure check. A slot that takes several tiers shows the tier that was built.
* Moving parts are still while the machine is idle, speed up as it starts and slow down as it stops, and hold where
  they came to rest.
* Config: `movingMultiblockParts` (common, on) and `animatedMultiblocks` (client, on).
* Fixed: every casing turned invisible as soon as its structure formed. A second run of the tool that writes the
  casings' "moving" look had overwritten their formed look with the hidden one; it now keeps them apart, and checks.
* Fixed: moving blocks could be left invisible. They are now drawn the way the game draws a moving piston's block,
  from the controller's own block renderer, and a hidden block that no formed machine is drawing (a structure from an
  older version whose shape has changed, or one taken apart) shows itself again within a minute or two.
* Fixed: moving parts were hidden in the world but never drawn (the Radio Telescope's dish and the Tokamak's field
  coils simply vanished): the first look at the machine's blocks never came, because of an overflow in its timer.

**Every multiblock rebuilt larger (all 76, 1.0's included)**
* Each machine keeps its silhouette but is scaled up: three times for the small ones, twice for the rest (the largest
  are now up to about 100 blocks across). Walls, floors and domes stay one block thick as they grow, so the material
  grows with the area (rings, columns and working parts stay whole).
* **The late and endgame machines are rebuilt from scratch, each to a design of its own**, in the manner of GregTech: New
  Horizons' and MeatballCraft's endgame: shaped by what the machine does, 60 to 130 blocks across, open frameworks
  rather than solid lumps, and each with a working animation no other machine has (in place of the shared effect it
  had before):
  * **Annihilation Reactor**: two injector towers, matter and antimatter, either side of a magnetic bottle, joined to it
    by ribbed beam pipes. Blue and red streams run down the pipes and meet in the bottle in flashes of gamma light.
  * **Causality Loop Engine**: a figure-eight track raised on pylons. A pulse runs the loop, its own echoes following
    round behind it, and flashes where the track crosses itself as the pulse meets its past.
  * **Dyson Array**: a star caged at the middle of a field of sixteen collector masts. A swarm of collectors orbits the
    star and each mast's dish catches a beam in turn.
  * **Matrioshka Brain**: three nested lattice shells round a star (stellar frame, computronium, error correction).
    Thoughts spark across the shells and signals pass out through them and back.
  * **Vacuum Harvester**: a funnel open to the sky. Pairs of particles flicker out of nothing; most meet again and
    vanish, and the ones caught are drawn spiralling down the throat.
  * **Axiomatic Breach Engine**: a pointed gate taller than a tower, wound with coil, anchored at its feet. A crack in
    the world opens down its middle, light spills from it and glyph shards flake off and turn away into the air.
  * **Concept Compressor**: nine arms reaching in from nine fields of knowledge to one compressor. Streams of nine colours
    spiral in along them and are crushed into one bright point, which beats.
  * **Eye of the Horizon**: a black hole the size of a hall inside a geodesic cage. A hot disc turns round it (brighter on
    the side coming toward you), matter spirals in from the cage, and two jets leave through the cage's poles.
  * **Transcendent Plasma Forge**: a star chamber fed by three great arching plasma conduits from three injector
    stations of unequal height. Plasma pours down each conduit into the star.
  * **Universe Forge**: a glass sphere cradled in three curved arms. Over and over: a point, a flash, a fireball cooling as
    it spreads, and out of it a few galaxies.
  * **Inflation Chamber**: a cube of space, a frame within a frame. A lattice of points swells outward faster and faster,
    then begins again.
  * **Mirror Foundry**: two vaulted halls either side of a wall of glass. A left-handed helix turns in one and its mirror
    image, right-handed, in the other, while ripples cross the glass.
  * **Entropy Horizon**: a great disc of radial fins round a deep well. Stars across it go out from the rim inward,
    reddening and drifting down the well, until all is an even dull glow.
  * **Essence Distillery**: a giant alembic, boiler, coiled neck and swan-neck arm to a receiver. Vapour climbs the neck,
    beads along the arm and falls drop by drop into the receiver.
  * **Abiogenesis Crucible**: a primordial sea in a wide basin under a storm cage of ribs. Lightning strikes the sea from
    the crown; where it lands, little cells form, drift and divide.
  * **Anomaly Containment Ward**: a six-sided arena with a pylon at each corner leaning in. The anomaly writhes at the
    middle in a flickering shield the six pylons pour themselves into.
  * **Anomaly Resolution Engine**: a spire on three fins. Strands are drawn out of the anomaly at its foot and up the
    spire, unwinding, to settle calm at the top.
  * **Reality Compiler**: a stepped ziggurat with an open frame on top. Code falls into the frame and compiles, edge by
    edge, into a shape (a cube, an octahedron, a tetrahedron), which holds, then dissolves.
* The other machines keep their own shapes, scaled up; the rings, crowned spires and buttresses an earlier build of this release
  added round every machine are gone.
* Nothing is lost: every block type is still used at least as many times as before (most many times more). Each old
  layout is kept, so the Structure Assembler can still take an old build apart. Built machines must be rebuilt in the
  new shape.
* Working animations grow with their machines.
* Large structures are checked less often (up to every ten seconds for the very largest), since a check reads every
  block.
* **Omnilex Engine**: rebuilt as a world tree. Roots spread through a basin (the sea of laws), a trunk climbs, six great
  boughs fork into a crown ringed with lattice under a glass dome. The
  hologram tree still grows above it; now bubble universes swell on the crown's boughs, break free and drift up, starry
  inside, while drops of Primordial Law gather on the lower boughs and fall into the sea below.
* **Pleroma Reconstitutor**: while it runs, the Transcendental's rekindling is traced again and again above it, at the
  machine's scale: the pillars of light, the spire and its crown, the great halo and the five rings.
* **Tokamak**: the toroidal field coils no longer spin. They heat up while it runs, glowing from dull red to white over
  about ten seconds and cooling slowly after, while waves of energy close in on the plasma from the coils and sparks run
  in along the spokes.
* **Eschaton Foundry**: the tops of its four pillars pour beams into the sphere; the sphere holds a mini universe (starry
  space, three turning spiral galaxies and a young star at its heart), and the ingredients of the recipe it is running
  swirl round it, drawn slowly inward.
* **Einstein-Rosen Bridge Projector**: eight beams from its frame pour energy into the bridge, pulses running along them,
  as if holding it open.

**Pleroma, reworked: five fluids instead of every fluid**
* **Recipe change (Pleroma Confluence):** 1,000,000 mB of water, lava and each of the mod's 67 older fluids, plus
  64,000 mB of every other mod's Essence → 1,000 mB of Pleroma ⟶ 1,000 mB each of **Quintessence**, **Alkahest**,
  **Mathesis**, **Panacea** and **Anima Mundi** → 1,000 mB of Pleroma. The Confluence's screen, JEI and EMI now show
  five tanks instead of a wall of them. Each tank holds up to 16,000 mB.
* **Quintessence** (Physics), in the Universe Forge, with the Theory of Everything as catalyst:
  * **Distillate of Forces:** 16,000 mB each of Quark-Gluon Plasma, Bose-Einstein Condensate, Superfluid Helium, Warm
    Helium, Strange Matter, Exotic Matter, Dark Matter, Neutrino Flux, Antihydrogen, Liquid Antihelium, Antiwater,
    Liquid Metallic Hydrogen, Magnetized Plasma, Heavy Water, Tritium and Uranium Hexafluoride → 1,000 mB.
  * **Distillate of Spacetime:** 16,000 mB each of Liquid Spacetime, Chronon Gas, Tachyon Flux, Hawking Radiation,
    Inflaton Field, False Vacuum, Dark Energy, Reheating Plasma, Dark Era Haze, Residual Starlight, Stellar Plasma,
    Hypernova Plasma, Raw Star Matter, Nebular Gas, Accretion Disk Plasma and Molecular Cloud → 1,000 mB.
  * **Quintessence:** 1,000 mB of each Distillate and 16,000 mB each of Primordial Law, Relativity Breakdown,
    Schrödinger's Limit and Transcendent Plasma → 4,000 mB.
* **Mathesis** (Mathematics), in the Conceptual Singularity Compressor, with Euler's Identity as catalyst: a Riemann
  Zeta Crystal, a Topos, a Surreal Number and 16,000 mB each of Maxwell-Boltzmann Gas, Brownian Suspension, Strained
  Spacetime, Quantum Spacetime Condensate, Renormalised Essence and Dimensional Residue → 4,000 mB.
* **Panacea** (Biology), in the Abiogenesis Crucible, with the Tree of Life as catalyst: a Resolved Anomaly, an Immortal
  Cell Line and 16,000 mB each of Nutrient Broth, Xenoplasm, Primordial Soup, Autocatalytic Broth, Synaptic Fluid,
  Nanite Slurry, Blood Plasma and Psionic Ether → 4,000 mB.
* **Alkahest** (Chemistry) is made as before, in the Alkahest Still.
* **Anima Mundi** (the world), in the Essence Distillery: 16,000 mB each of water, lava, Basaltic Magma and Deep Brine,
  and 4,000 mB of every other mod's whole Essence → 1,000 mB, twelve inputs at a time (with many mods, in lettered
  parts first, as a mod's own Essence is). Like the Essences, it is worked out from the installed mods, so new mods'
  fluids join in by themselves.
* Per 1,000 mB of Pleroma this is about 4,000 mB of each older fluid instead of a million, so it is far cheaper, but it
  now runs through the five branches' keystones.
* **Research:** Pleroma now also needs Alkahest, the Theory of Everything, Euler's Identity and the Tree of Life. Its
  puzzle mixes five fluids, not a thousand.
* **Old saves:** a Confluence part filled under the old list is credited, once, with the share of a batch it had
  gathered, as Pleroma in its output tank. The older fluids keep all their other uses.
* Codex: a new page, The Five Fluids of Pleroma; the Pleroma, Essences, Alkahest and Essence Distillery pages say how
  it works now.

**Endgame gear**
* **The Dodecaract Excavator no longer floods the inventory or the ground.** A big job (up to 33x33x33) used to put
  every drop into the inventory and spill the rest as items at your feet, thousands of them in ore-rich ground, enough to
  stall a server. Now what a job gathers is handed over at the end as **Folded Matter** (a new item: up to 16,384 items
  of 64 kinds in one), the way Avaritia's World Breaker gathers into Matter Clusters; a job's few drops (four kinds,
  four stacks at most) still go straight into the inventory. Use Folded Matter to unfold it into your inventory: what
  does not fit stays folded, so nothing is ever dropped. A job cut short (you log out, change dimension, put it away,
  the server stops) still hands over, or leaves, what it gathered.
* **The Omnipotent Edge's Erasure reaches all round you**: no longer a 16-block cone before you, but every hostile
  creature within 64 blocks (common config `edgeErasureRadius`, 8 to 256), seen or not. The common config
  `edgeErasureAllCreatures` makes it take every creature that is not a player, a villager or someone else's pet
  (never in land you may not use). What it kills comes to you as Folded Matter (a single kill's drops still go
  straight into the inventory); the experience comes to you as before.
* **The Greaves of the Absolute drive you on as Avaritia's Infinity Boots do**: on the ground, flying or swimming, a
  push the way you walk (a quarter of it backwards, under half sideways, a tenth while sneaking) and a further push
  while sprinting, on top of the speed bonus they had. Common config `greavesSpeed` (1 = the Infinity Boots, 0 = the
  plain bonus only).
* **Endgame gear on mobs** (for packs that give mobs custom gear): a mob's charged Raiment now spends its charge over
  time as a player's does, and a mob's charged Edge spends charge with each blow, so neither lasts forever. Mobs never
  get the Edge's unmaking. What each piece does on a mob, and how to give one charged gear: docs/ENDGAME_GEAR_ON_MOBS.md.

**Security, server and performance fixes**
* **The Lawgiver's Codex respects claimed land.** Creating creatures, and unmaking, pacifying, transmuting, growing or
  shrinking one, are refused inside land the player may not use (FTB Chunks' claims, with its allies and settings, and
  the game's spawn protection); healing is still allowed anywhere. A refused action costs no Eschaton Charge.
* **The Codex's laws over a whole dimension** (gravity, time, weather, spawning, mob health, damage and speed, and
  restoring them) need permission: the common config `codexWorldLawsPermissionLevel`, default 2 (operators), 0 for
  anyone. The owner of a single-player world always may; LAN guests follow the setting. The server checks it, so a
  modified client cannot get round it, and the Codex shows those controls locked for those who may not. Laws already
  written stay: an operator can clear them with Restore.
* The Omnipotence Core (and a forged Core) only takes orders from the screen its player opened by using it: no longer
  from a modified client near someone else's Core, through a wall, in a claim, or as a spectator.
* **Fixed a duplication at the Extreme Assembly Table**: with two players at one table, the second could take a copy of
  an output the first had just made. Every player at the table now sees the grid change, and the output is only taken
  while the grid still makes it.
* **The world seed can no longer be worked out by a client.** Puzzle values come from a random number kept with the
  world's research, not from the seed (which a modified client could recover from what it was sent). In an existing
  world the numbers of unsolved puzzles change once; solved ones stay solved.
* Secret research (granted only, never researched) is sent to a client only once that player has it.
* Tighter limits on the sizes of what clients send for puzzles and the Core; an unknown puzzle action changes nothing.
* The Resonant Tuning Fork's answer only counts with the fork still in hand; its pending games are forgotten on logout.
* **Fewer network updates from idle machines**: a physics machine's live readout went to everyone nearby twice a second
  even when idle; now twice a second while someone has its screen open, once a second while it runs, every ten
  seconds otherwise.
* The physics solver runs on a low-priority thread of its own instead of the game's shared background pool (which world
  generation uses), and a recipe with no solution at the machine's tiers is tried again less and less often (up to every
  twenty minutes) instead of forever every ten seconds.
* In single player and LAN, a Field Projector whose chunk the client unloaded (while the server kept it loaded, as with
  chunk loaders) no longer switches off the server's field.
* Radiation sources are forgotten when a world closes (opening another world in the same game could give phantom
  dose).
* Machines save their stored energy (after a reload, a consumer lost its charge and a generator could send energy
  twice). A containment failure under way is saved too.
* After a respawn, a Raiment's or Omnilex's grant of flight still ends when it should; leaving the End alive keeps the
  radiation dose and what it has started.
* An anomaly zone is no longer forgotten because part of its ruin was unloaded.
* Items or fluid a neighbour refuses after offering to take them (a port's export or import, a transceiver's energy) are
  put back or dropped, never lost.
* Animation timing stays smooth on worlds days old (it had gone into visible steps).
* The mod's multiblocks' moving parts are drawn from buffers kept on the graphics card (baked once), each part culled
  by distance and by view, instead of every block of them sent through the game's buffers every frame; one machine's
  parts are baked a frame, so a base full of them loads without a hitch. With Sable, or the new client config
  `movingPartsCompatibility`, the old way is used. Machines with no moving parts are no longer drawn while their
  controller is out of view, their structure checks after a world loads are spread out instead of all on one tick, a
  machine's neighbours' handlers are looked up once and kept, and moving parts are only looked for again when the
  structure forms or breaks.
* The power-down sparks follow the particle density setting; a mistyped animation name is logged; the Core's Forge no
  longer crashes if a pack removes every Forge recipe.
* The GregTech animation connector made for this release is gone: the animations are for this mod's own multiblocks
  only. Their star, shell, orbiter and beam effects are `fx` entries any of the mod's multiblocks can use
  (docs/ANIMATIONS.md).
* A saved Omnipotence phase that cannot be read never falls back to before the Shutdown (a second Shutdown stays
  impossible).
* The mod's description lists 191 machines (76 multiblocks) and fifteen dimensions.

**Coil tiers**
* The Structure Assembler builds every coil of a structure in one tier: the highest you carry enough of for the whole
  structure (your inventory and a bound ME network together), or failing that the highest you have any of. Before, it
  took the best coil you had for each place in turn, so running short part way through left a structure of mixed
  coils, which counts as its lowest. In creative it now uses the highest tier (or the one in your off-hand).
* A third Structure Assembler mode, **coils** (sneak-use in the air cycles build, disassemble, coils). Use it on a
  controller to change every coil of the built structure to the best tier you have enough of; the old coils come back
  to you (or to a bound ME network) and the machine runs at the new tier at once. Sneak-use on the controller first to
  pick a tier yourself: it cycles through the tiers, showing how many coils you need and how many you have.
* Every coil's tooltip names its tier ("Coil, tier 3 of 5"), and every field emitter's too; parts a datapack adds to
  a tier family are included.
* A new Codex entry under Machines, **Coil Tiers**: every tier, where it is made, the recipes it is first needed for,
  and how the assembler builds with and changes coils.
* The Codex's multiblock pages are now drawn from the multiblocks as they are (they still showed the layouts from before
  the rebuild, with the old block counts); the giants are drawn a coloured square a block.

**Fixed (the endgame rebuild)**
* The Eye of the Horizon's cage was drawn wrong (its upright rings had been laid flat, so its frame was a few flat
  rings), and the blocks it was short of were piled onto its floor as loose lumps. Its cage is now a true sphere of
  upright and level rings, and its collectors, stabilisers and emitters sit on the cage and its feet.
* The same flattened rings are fixed in the Annihilation Reactor's magnetic bottle, the Dyson Array's star cage and
  the Matrioshka Brain's shells.
* Interface Ports no longer stick out beside a controller (the Universe Forge's stood out from its leg): the blocks of
  the structure nearest the controller are its port positions now.
* Every endgame machine has enough of each of its blocks by design, in places that belong to it (the Breach Engine's
  spines wound with coil, the Plasma Forge's nozzles round each conduit's mouth, emitters on the Universe Forge's
  poles), instead of extra blocks stuck onto its surfaces. Where any machine is still short of a block, the extra goes
  where the structure closes round it (filling a hollow, thickening a wall), not onto a flat face.
* Emitters that floated a block above the structure (Vacuum Harvester, Mirror Foundry, Entropy Horizon, Inflation
  Chamber, Anomaly Resolution Engine, Reality Compiler) now sit on it.

**Release checks**
* Three newer routes cost more than an older one for the same thing, and are now cheaper: Hydrogen Cyanide by
  photolysis (3 Methane and 3 Nitrogen a run, in 30 ticks, in place of 13 each in 50 ticks), the Calabi-Yau Manifold
  in the Topology Kiln (24 a firing, in place of 8), and Cosmic Record Fragments from cosmic web (one filament and two
  paper make 8, in place of 27 filaments and 54 paper making 32).
* The Codex is brought up to date with this version: the Tumour Hydra (heads in fixed places, necks that count as the
  head, the stump left where the head was, how it can be killed, open ground to call it), the Polymerase working the
  helices, Macrophages and T Cells fighting the Hydra, new pages for the Brine Jelly and the Mirror Microbe, flavour
  for the Vent Crab's page, and the Living Catalyst's wear (it heals while fed, it is not used up).

**Phase 1: The Cell**
* New machines (all on consoles, each with a real-equation window and a calibration game): **Autoclave** (steam
  sterilising, N = N0.10^(-t/D)), **Patch-Clamp Rig** (Nernst membrane potential), **Fermenter** (Haldane growth, air),
  **Photobioreactor** (light response, placement by daylight), **Ribosome Assembler** (speed against accuracy), **Folding
  Chamber** (two-state folding).
* New items: Sterile Pack, Lipid Bilayer, Ion Channel Membrane, **Living Cell** (the first piece of the Tree of Life),
  Yeast Culture, Algae Culture, Messenger RNA.
* New routes: cheap **Nutrient Broth** (yeast), **biogas** (methane from rotten flesh), **oxygen** and a far cheaper
  **Quantum Chlorophyll** (algae), cheap **Engineered Enzymes** and **Folded Protein Crystals**, a new **Synthetic Cell**
  route. Molecular Biology now builds on Chemical Bonding and Quantum Chemistry, not Artificial Intelligence (which moves
  to a later computing patch).
* 8 research nodes with derivation puzzles, from Sterile Technique to Translation.

**Phase 2: Microbes**
* New machines: **Phage Lab** (Poisson infection 1 - e^(-MOI)) and **Mycelium Bed** (fungal growth).
* New items: Bacterial Culture, Bacteriophage, Mould Culture, Antibiotic, Probiotic Seal, Physarum Culture.
* New routes: bacterial **Magnetite** (a renewable route to the ore, from magnetotactic bacteria), cheap **Recombinant
  DNA** (phage gene transfer), **Antibiotics** (penicillin from mould), and cheap **Network Maps** (Physarum laying out
  shortest paths). Extremophiles now builds on Microbiology.
* 7 research nodes, from Microbiology to Slime Moulds.

**Phase 4 (part 1): Defence**
* New machine: **Antibody Bench** (Hill-curve immune response).
* New items: Antibody, and an Anomaly Ward (and a lasting Ward) for each of the four infections.
* The Anomaly storyline begins to be playable: its creatures infect, and the body answers. **Tolerance** research (the
  story's twist) makes the Law Wraiths that pour from a dimensional breach, reality's own antibodies, read you as self
  and leave you be.
* 6 research nodes, from Innate Immunity to Immune Memory.

**Phase 3: The Body**
* New machines: **Perfusion Rig** (Poiseuille flow), **Regeneration Tank** (morphogen gradient, c = c0.e^(-x/lambda)),
  **Circadian Incubator** (keeps the world's day) and **Dosing Pump** (one-compartment kinetics).
* New items: Perfused Tissue, **Grown Organ** (the fourth piece of the Tree of Life), Circadian Culture, Stable Dose.
* New route: another cheap Stem Cell Culture (perfused). Morphogenesis now builds on Reaction-Diffusion (the Pattern
  Basin's Turing patterns). Neuroscience (the Optogenetic Array and Neural Organoid) is held for a later computing patch.
* 6 research nodes, from Physiology to Pharmacokinetics.

**Phase 5: Life at Scale**
* New machines: **Ecosphere** (Lotka-Volterra predator and prey), **Selection Chamber** (fixation probability),
  **Gene Circuit Loom** (bistable toggle switches) and **Mirror Culture Vat** (chirality purity).
* New items: **Stable Ecosystem** (the fifth piece of the Tree of Life), Symbiont Lining (upgrade: catalysts wear 40%
  slower), Evolved Strain, Gene Circuit, **Living Catalyst** (never quite runs down while fed), Mirror Cell, Mirror
  Antibody (the only answer to the Antiverse's Mirror Microbes), and the **Tree of Life** keystone.
* **The Tree of Life**, the branch's keystone (as Euler's Identity was for Mathematics): a Living Cell, a Pure Culture, a Grown
  Organ, an Antibody and a Stable Ecosystem, one from each phase, together make an endgame catalyst that never wears.
* New routes: a cheaper Omnicellular Genome feed for the Evolution Engine, and mirror antibodies. Mirror Life is in Biology
  as decided.
* 6 research nodes, from Ecology to the Tree of Life.

**Phase 4 (part 2): The Anomaly, lived**
* New machine: **Clonal Selector** (affinity maturation: each round multiplies the binding by (1 + s), while the line's
  diversity falls as 100.e^(-n.s/3) and collapses below 10%).
* New items: Matured Antibody, Metastasis Ward and Lasting Metastasis Ward.
* **Immune evasion**: the Tumour Hydra's bite carries **Metastasis** (every ten seconds weakness, hunger or slowness), which
  no plain Anomaly Ward holds; only the Metastasis Ward, made from Matured Antibodies, does.
* **Epidemic Dynamics**: infections pass between players. Each second an infected player passes theirs on, one time in fifty,
  to anyone within four blocks who is not warded (the SIR model).
* Each chapter of the Anomaly storyline now has something to meet (a Codex page, The Anomaly, Lived, lists them).
* New routes: the four lasting Wards far more cheaply from Matured Antibodies (a Ward and two Matured Antibodies, in place of
  a Ward, four Antibodies and a Telomerase Complex).
* 1 research node: Affinity Maturation.
* The Antiverse's **Mirror Microbes** now exist: a few drift round any player there, and their touch gives the **Mirror
  Plague** (less health, and your food burns), which no Anomaly Ward holds. A **Mirror Antibody**, used on yourself, wards it
  for three days.

**The world: Inner Space**
* A new dimension: a living body seen at the scale of its cells, reached with the **Miniaturiser** (research Inner Space,
  after Circulation). Five biomes: the **Bloodstream** (seas and channels of Blood Plasma whose current carries you), **Tissue**
  (walls of cells), the **Lymph Node**, the **Nucleus** (chromatin and the double helix) and the **Tumour**. Falling out of the
  bottom brings you home.
* Hazards, effects only: the current; **inflammation** in Tissue where a fight breaks out (slower, and the heat hurts);
  **tagged** in the Lymph Node (carry an infection and you glow, and T Cells hunt you); **mutation** in the Nucleus (now and
  then one of your effects swaps for its opposite); the **oncogenic field** in the Tumour (weakness that grows the longer you stay).
* Creatures, each with its own mind: the **Red Cell** (drifts with the current; you can ride it), the **Macrophage** (engulfs and
  digests whatever it catches; strike your way out), the **T Cell** (hunts anything infected) and the **Polymerase** (harmless).
* Mini-boss: the **Tumour Hydra**, called with a Metastatic Spore in the Tumour. Every 30 damage cuts off a head; seal the stump
  with fire, a CAR-T Cell Culture or an Apoptosis Trigger within five seconds, or two grow back. With no heads left it can die.
* Landmarks with caches: Germinal Centres, Nuclear Pores, Ribosome Workshops, Cell Clusters, Helix Spires and Tumour Growths;
  and in the Overworld, abandoned **BSL-4 Field Stations** with the branch's first samples.
* New materials, each useful and renewable: **Blood Plasma** (the Lake Pump draws it in the Bloodstream), **Matrix Fibre**,
  **Nucleotide**, **Memory Cell** and **Tumour Specimen**; and blocks: Extracellular Matrix, Cell Membrane, Bone Matrix, Lymph
  Tissue, Chromatin, Tumour Mass, Bioluminescent Membrane, Biosafety Panel, and three ores.
* New routes: nutrient broth from plasma; Antibodies from plasma and a Memory Cell; a cheaper **Grown Organ** (one Stem Cell
  Culture and four Matrix Fibre in plasma, in place of two cultures, two Perfused Tissues and an Assayed Enzyme); cheap
  **Messenger RNA** from nucleotides; Genome Samples read from chromatin; a cheaper **CAR-T Cell Culture** from tumour specimens;
  Lipid Bilayers from membrane; bone meal from bone. Every block can be laid again from matrix and what it holds.
* Its own music, sky and travel animation. 1 research node: Inner Space.

**The world: Titan's hidden ocean**
* Under Titan's Subsurface Seal: dark water between an ice ceiling and a rock floor, with glowing hydrothermal chimneys.
  Reached with the **Cryobot**, a probe that melts down through the ice (research Titan's Hidden Ocean): stand on the Seal and
  use it. Titan's cold and the Oxygen Rebreather carry over. (It is its own dimension under Titan, so Titan's existing
  terrain is left exactly as it was.)
* Its ecosystem is the first for the Ecosphere: **Vent Grazers** feed round the chimneys and multiply; **Ice Eels** hunt them,
  multiply after three meals and starve after a day without one. The two populations rise and fall round each other on their own.
* New items and blocks: Cryobot, Vent Biofilm, Eel Photophore, Abyssal Silicate, Hydrothermal Chimney.
* New routes: a **Stable Ecosystem** at half the old cost (biofilm, a photophore and algae make two), cheap Bacterial Cultures
  from biofilm, hydrogen from Abyssal Silicate (serpentinisation), sulfur dioxide from chimney rock.
* 1 research node: Titan's Hidden Ocean.

**Wear: the living catalysts**
* The **Symbiont Lining** now works as an upgrade: a console fitted with one wears its catalysts 40% slower.
* The **Living Catalyst** now wears (128 runs), but each run that feeds it sugar or broth heals a run of wear instead. It is
  used in two new routes: **nitrogen fixation** (bacteria and sugar turn Nitrogen into Ammonia in the Fermenter, with no
  hydrogen and no iron catalyst: a cheap rival to Haber-Bosch) and cheaper **Gene Circuits** (one Messenger RNA, no Error-Correcting Code).

### Part two: Mathematics

Crafting counts below are for a 3×3 layout; on the Extreme Assembly Table each recipe is laid out on the 9×9 grid,
so each ingredient is needed about nine times over (the Governor's 2 Stability Margins are 18). JEI and EMI show the
exact grid.

**Tuning beyond consoles, part 1: calibration**
* **Tuning Fork** (crafted from iron and an amethyst shard; 256 uses): strike it against a machine with a console, or
  an older multiblock's controller, to calibrate it with a short game that fits its science. Solved, the machine is set
  exactly at its working point for the recipe it runs (older multiblocks: their tuning dial is set on target and their
  containment restored). Six games, harder at later stages:
  * **Match the resonance** (most machines): build a target waveform from sine waves of your own.
  * **Calibrate the spectrometer** (telescopes, spectrometers, interferometers, the optical machines): set a scale's
    offset and stretch until the machine's lines sit on the reference lines. Both strips have a ruler (a tick every
    5 nm, numbered every 50), each reference line is labelled and notched on the lower ruler, your lines show where
    they read, hovering reads off the wavelength, and the average miss in nm is shown.
  * **Route the flow** (reactors, plasma machines, colliders, centrifuges, pumps): turn pipe tiles until the flow runs
    from inlet to outlet. From stage 2: rock that holds no pipe, bolted tiles that cannot be turned, and gold valves
    the flow must pass through; from stage 4 the flow must be sealed, with no open pipe end leaking.
  * **The chemistry machines** play one of four games, by what the machine does:
    * **Balance the reaction**: give an equation coefficients so every element balances; from stage 3 also ionic and
      redox equations (permanganate, dichromate, metal displacement), where the charge must balance too.
    * **Write the equation** (titration, brine, filtration, synthesis and coordination machines): pick the reactants
      and products from species on hand (some do not belong), then balance it; precipitations ask for the net ionic
      equation of two mixed salt solutions, leaving the spectator ions out.
    * **Oxidation states** (the fuel cell, plating bath, kilns and the Alkahest Still): the oxidation state of the
      marked element in compounds and ions, from water to dichromate.
    * **Write the equilibrium constant** (the Haber-Bosch, Ostwald and contact process machines, buffers, the
      Oscillator Vessel): each species' power in K, with pure solids and liquids left out.
  * **Prove it** (the Markov Refinery, Monte Carlo Engine and other logic machines): chain implications from premise to goal.
  * **Sieve the primes**: ready for the Mathematics part's number-theory machines.
  The game is drawn as the machine's own console, in its colour. Values change by scrolling over them (shift: five at
  a time) or with the arrows at their ends. A wrong answer only says "not yet"; nothing is lost.
* **The console is now for fine tuning, the fork for getting a machine working.** A Tuning Fork calibration leaves
  the machine at a working point (every limit and window met) a little off its best. Every multiblock and console
  machine now shows its **precision** along the bottom of its console: how close its settings sit to the recipe's
  ideal point (found once per recipe, the same search as Laplace's Demon). It falls off fast, about a third lost at 1%
  of a setting's range. At full precision each cycle makes half as much again of every product (items and fluids,
  as room allows). Older multiblocks: precision is how close the tuning dial sits on its drifting target.
* **Resonant Tuning Fork** (a Tuning Fork, two Entangled Photon Pairs and two amethyst blocks): never wears out, and
  says how far a wrong answer is off.
* **Calibration Card** (crafted from paper, redstone and a comparator): sneak-use on a tuned machine to copy its
  settings, use on another of the same kind to set it the same. Never used up; sneak-use in the air to wipe it.
* Codex: Calibration (Getting Started).

**Mathematics Phase 1, part 1: Foundations**
* **Primes** (new research, after Number Systems and Combinatorics): the **Sieve of Eratosthenes**, a console. Set its
  limit N and how many small primes its wheel skips: about N / ln N primes come out, a recipe needs a number of them,
  and the work grows as N ln ln N. It makes Prime Shards from 8 Natural Number Lattices and 2 redstone, or from 8
  Axiom Stone (new routes; the Peano Engine's 1.1 route is unchanged). Its calibration game is the prime sieve.
  Number Theory now also needs Primes.
* **Modular Arithmetic** (new research, after Primes): clock arithmetic and Euler's totient.
* **Proof by Induction** (new research, after Formal Logic): the **Proof Certificate** (Formal Proof Assistant: an
  Axiom Schema, 4 paper and 2 Natural Number Lattices). Use it on a multiblock's controller to file it there, up to four;
  each one catches one containment failure, and nothing is released.
* **Cardinality** (new research, after Formal Logic and Combinatorics): **Zero** (from an Empty Set) and **Unity**
  (Zero and an Empty Set), von Neumann's numbers, in the Formal Proof Assistant: two of the five constants of Euler's
  identity. As Physics material (new routes, the old ones unchanged): a Casimir Plate from Zero, 4 Neutronium Plates
  and 2 Cherenkov Quartz; a Qubit from Unity, 2 Entangled Photon Pairs and 2 Quantum Dots.
* **Combinatorics** (1.1): the Peano Engine now counts **Microstate Tallies** (2 Natural Number Lattices and paper),
  and the Maxwell Demon Sorter builds its chip from 4 of them, 2 qubits, 2 Cherenkov Quartz and 4,000 mB of
  superfluid helium (a new route).
* Codex: Foundations.

**Mathematics Phase 1, part 2: Foundations, reworked** (mathematics now starts early)
* The **Peano Arithmetic Engine** (quartz, a comparator, iron, a block of redstone and Quantum Dots) and the **Formal
  Proof Assistant** (books, a Natural Number Lattice, quartz, an Entangled Photon Pair, iron and a comparator) are made
  from early parts. Their old neutronium recipes are gone (they came later and gave less).
* Early routes (the old ones are unchanged): Natural Number Lattices from Cherenkov Quartz and redstone; Integer Rings
  from lattices and a redstone torch (negation); Rational Fields from rings and a comparator; Axiom Schemas from
  lattices, paper and a book; Empty Sets from empty glass bottles and a lattice. So Zero, Unity and Proof
  Certificates are early-game too.
* New textures for the Tuning Fork and the Resonant Tuning Fork.
* Codex: First Steps in Mathematics.

**Mathematics Phase 2: Structure and Space**
* **Euclid's Elements** and **π** (new research, after Number Systems and Geodesy): the **Geometer's Table**, a
  console. Archimedes' method: a polygon of n sides inside a circle gives n·sin(π/n). Set the sides and the care of
  the straightedge until the error is under the recipe's tolerance; more is closer but slower. It makes **π Crystals**
  (one of Euler's five constants) from Rational Fields and a compass. Calibration game: **Fill the vertex** (choose
  regular polygons whose corners add to exactly 360°; later stages ask for rare polygons and an exact count).
* **Curved Geometry** (new research): spherical triangles and their angle excess.
* **Groups and Symmetry** and **Lie Groups** (new research): the **Symmetry Lathe**, a console (its indexing head is
  a crafted **Symmetry Seed**). Set the indexing order k and the spindle speed: the blank's symmetry group has order
  2k, which must be the recipe's; too fast for a fine head and it shakes apart. **Symmetry Crystals** and **Lie Group
  Lattices**, and new, far cheaper routes to Quaternion Crystals and Gauge Boson Clusters. Calibration game:
  **Complete the symmetry** (a mirror, two mirrors, a quarter turn, or all eight symmetries of a square, with cells
  locked on or off).
* **Vectors and Matrices** (new research): eigenvalues from the trace and determinant.
* **Topology of Surfaces** and **Manifolds and Curvature** (new research): the **Topology Kiln**, a console. Set the
  handles, the cross-caps and the temperature: the surface's Euler characteristic χ = 2 − 2g − c and whether it is
  one-sided must be the recipe's. New, far cheaper routes to Möbius strips, Klein bottles, Differential Manifolds and
  Calabi-Yau Manifolds (the old ones are unchanged). Calibration game: route the flow.
* **Compactification** now also needs Manifolds and Curvature, and **Grand Unification** needs Lie Groups: the
  mathematics of the extra dimensions comes first.
* Codex: Structure and Space.

**Mathematics Phase 3: Change**
* **Machine upgrades.** Applied mathematics now fits to machines: use an upgrade on a multiblock's controller or a
  console machine (one per machine; fitting another hands the old one back; it drops when the machine is broken).
  * **Rate Table**: +20% products each cycle, on top of precision (a perfectly tuned machine goes past perfect).
  * **Stability Margin**: precision falls off half as fast, so fine tuning is easier.
* **Rates as Equations** (new research, after Reaction Kinetics): the **Integrator**, a console. Set the step size and
  the method's order (Euler, midpoint, Runge-Kutta): the error must be under the recipe's tolerance, and too big a step
  blows up. It makes Rate Tables, **e Crystals** (one of Euler's five constants, research **e**) and Stability
  Margins (research **Stability**). Calibration game: match the resonance.
* **Fixed Points**, **Stability**, **Bifurcations** (new research): Newton's method, stable and unstable points, the
  Hopf threshold of oscillating reactions.
* **Euler's Formula** (new research): Imaginary Units from Rational Fields, a π Crystal and an e Crystal (a new route).
* **Enzyme Kinetics**, **Enzyme Inhibition**, **Cooperativity** (new research, after Genomics): the **Enzyme Assay
  Bench**, a console. Saturate the enzyme to 90% without choking it (too much substrate) or clumping it (too much
  enzyme). **Assayed Enzymes**, and Porphyrin Oxygen Carriers from a steep cooperative curve. New Bioculture routes
  with Assayed Enzymes: Nutrient Broth from half the sugar, bone meal and water, and (research Enzyme Inhibition) Stem
  Cell Cultures from half the parts. Calibration game: write the equilibrium constant.
* Codex: Change.

**Mathematics Phase 4: Chance and Computation**
* Two more upgrades: the **Confidence Seal** (every chance product comes out halfway closer to certain) and the
  **Error-Correcting Code** (catalysts wear on only every other run).
* **Statistics** (new research, after Distributions and Probability Theory): the **Statistics Engine**, a console. Set
  the sample size and the confidence: the margin z·σ/√n must be under the recipe's tolerance. It makes Confidence
  Seals; **Regression** gives Assayed Enzymes from statistics alone; **Stochastic Kinetics** counts six Microstate
  Tallies from four lattices. Calibration game: prove it.
* **Information Theory**, **Error Correction**, **Cryptography** (new research): the **Cipher Loom**, a console. Set
  the block length and the parity bits of a Hamming code: it must cover the block, and failed blocks ((nε)²/2) must be
  rarer than the recipe allows. It makes Error-Correcting Codes and **One-Time Pads**, which tune two Entanglement
  Tuners at a time (a new recipe). Calibration game: **Break the cipher** (a shift cipher; later a key of two or three
  letters).
* **Algorithms** and **Computability** (new research).
* Codex: Chance and Computation.

**Clean-up after testing**
* **Recipes with a reason.** A new check makes sure every alternative route to a material is worth using: cheaper in
  materials, faster, or on an earlier machine. Fixed by it:
  * the Peano Engine's and Formal Proof Assistant's old neutronium recipes removed (later, and worse than the new ones);
  * the second π Crystal recipe and the second Assayed Enzyme recipe removed (nearly the same ingredients, slower);
  * the ceramic Whipple Shield recipes removed; Ceramic Armour Plates mend Whipple Shield armour instead.
* **One catalyst where one makes sense:** Axiom Stone needs only the Platonic Compass (not Axiom Stone too), Ordinal
  Stone only the Cantor Key, the black and white hole rocks only the Horizon Diver, and Arbor Heartwood only its sample.
  (One late recipe, the Anomaly Resolution Engine, now pulls in Phantom Energy Cells in place of the Infall Rock
  crusher, as a side effect.)
* **Short advancements:** every research advancement says one short thing (what it unlocks, or a few words), instead of
  the research's whole description.
* **Codex, Discovery by field:** the Discovery map is grouped by field (Foundations; Physics in three lanes; Chemistry;
  Biology; Mathematics; Worlds; Transcendence). Each node there is a study's introduction (its opening page) and says
  which tab has the rest; machines, materials and recipes live in the field's own tab (and Machines / Materials).
  Machines' own chapters are no longer on the Discovery map.
* **Codex brought up to date:** containment failures (ten kinds now, not five), the Transfinite Hierarchy as the
  Platonic Realm's upper strata (Fields, the Hierarchy's chapter, Falling Through, the Cantor Key), and the maths
  machines' recipes.

**One Realm: the Transfinite Hierarchy joins the Platonic Realm**
* The Transfinite Hierarchy is no longer a dimension of its own: it is the **Platonic Realm's upper strata**. The Realm
  is twice as tall (Y 0 to 512). Its own islands are where they were, in axiom stone; above Y 256 the Hierarchy's four
  strata rise as before, in ordinal stone, with Hilbert's Hotel, the tesseract frames, the Hopf rings, the ordinal
  spires and its ores (now between Y 256 and 512).
* Each layer keeps its own: the Ordinal Hound, Hilbert Bellhop and Hypercube Drifter spawn only in the strata, the
  Fractal Wyrm and Axiom Sentinel only below; the Hierarchy's law (Infinite Regress, shielded by the Well-Ordering
  Charm), its dark sky and its seven spatial dimensions (for field machines) hold above Y 256, the Realm's below.
* The **Cantor Key** takes you to the strata from anywhere (used there, it takes you home); the **Platonic Compass**
  to the Realm's own islands (used in the strata, it takes you down). Falling out of the bottom of the Realm lands you
  in the Quantum Foam. The "arrival" advancement is now climbing into the strata.
* **Old worlds:** the old dimension is kept so saves load, but anyone in it is carried to the same place in the
  strata. Anything built there stays there, out of reach: bring it out before updating. Areas of the Realm already
  explored keep their old terrain (nothing above Y 256); the strata appear in newly explored areas.
* Codex: One Realm.

**The Elements: a layer between the Realm's islands and the strata** (Y 196 to 256)
* Two bands of **Euclidean Marble** islands fill the gap: Y 202 to 232, and smaller ones Y 238 to 252. **Euclidean
  Ore** in them gives **Euclidean Shards**.
* Landmarks, built only from the mod's blocks: **Compass Towers** (a circle with the hexagon stepped round it on top,
  and a straightedge reaching out), **Geometer's Ruins** (Euclid's proof of Pythagoras inlaid in the floor, with a
  cache), and the floating Platonic solids, here in marble.
* **Euclid's Automaton**, a walking pair of compasses: it walks only in straight lines, turns only through angles it can
  construct, draws a circle wherever it stops, and fights only whoever hurts it. It drops Euclidean Shards (and now and
  then Golden Ratio Glass).
* It is the safe layer: the Fractal Wyrm and Axiom Sentinel now spawn only below Y 196.
* **Getting there:** sneak and use the Platonic Compass anywhere in the Realm below the strata to climb to the Elements
  (from the Elements it takes you back down). Without sneaking it works as before. From outside, it now lands you on
  the Realm's own islands (below Y 192).
* Euclidean Shards **mend the Platonic Compass** at an anvil, and build the new Phase 5 machines below.
* Fractal Trees grow only where nothing floats above them (they no longer grow on the Elements).
* New recipe: **Golden Ratio Glass** ×15 on the Geometer's Table from 8 glass and 1 Euclidean Shard (Euclid's Elements;
  now Constructions).
  The old crafting recipe is unchanged.
* Advancement: The Elements. Codex: The Elements.

**Mathematics Phase 5, part 1: Optimisation and Graphs**
* **Optimiser** (a console; Optimisation): gradient descent on a bowl. Set the step h and the number of steps n: each
  step leaves |1 − hλ| of the distance, the error after n steps must be under the recipe's tolerance, and past hλ = 2
  it runs away. A step of exactly 1/λ lands at once. Calibration game: **find the shortest route** through a network
  (later tiers: through a gold junction, round closed links).
* **Graph Loom** (a console; Graph Theory): networks of V junctions and L links. Each link beyond V − 1 is a spare
  route, and the network fails about q^(L − V + 2) of the time, which must be under the tolerance. Calibration game:
  **lay the lightest network** that joins every junction (later tiers: keep a link already laid, round closed links).
* New: **Optimised Pathway** upgrade (Optimiser: 1 Euclidean Shard, 2 Derivative Operators, 2 Rational Fields): each run
  takes a quarter less power. **Network Map** ×2 (Graph Loom, Networks: 4 Natural Number Lattices, 8 string, 2 Integer
  Rings), the key to Biology.
* New routes: **Metamaterial Sheet** ×2 on the Optimiser from 4 Cherenkov Quartz, 4 Quantum Dots and 2 Rational Fields
  (Topology Optimisation); **Photonic Processor** ×5 on the Graph Loom from 4 boards, 6 dies, 6 Quantum Dots and a
  Network Map (Shortest Paths). The old routes are unchanged.
* Machines: Optimiser (smooth stone, Euclidean Shard, Derivative Operators, comparator, iron, redstone block); Graph
  Loom (string, Euclidean Shard, looms, Natural Number Lattice, iron, redstone block).
* Research with derivation puzzles: Optimisation, Topology Optimisation, Graph Theory, Networks, Shortest Paths.

**Mathematics Phase 5, part 2: Control**
* The **Governor** (Control Theory), set against any machine with a console (a multiblock's controller or a console
  machine): once a second it closes a third of the gap between each setting and the machine's ideal for what it runs,
  holding it working at about 75% precision whatever the recipe. The last quarter is still the console's fine work. On
  older multiblocks it keeps the tuning dial on its target. Needs 32 FE a tick; right-click to see what it holds.
  Crafted from gold, a Euclidean Shard, 2 Stability Margins, a comparator, iron and a block of redstone.
* Research with derivation puzzles: Feedback, Control Theory.

**Mathematics Phase 5, part 3: Games**
* **Strategy Table** (a console; Games and Strategy): the Hawk-Dove game. Set the hawk share p and the rounds n: the
  gap (V − pC)/2 must be under the tolerance (the stable mix is p = V/C) and so must the noise of chance, 1/√n.
  Calibration game: **find the equilibrium** in a table of payoffs (2 × 2, then 3 × 3).
* New: **Evolutionarily Stable Strategy** (Strategy Table, Evolutionary Games: a dice set, 2 Rational Fields, 4 paper).
* New route: **Tardigrade Culture** ×2 in the Bioreactor from 12 Genome Samples, 6 moss and an Evolutionarily Stable
  Strategy (no enzymes, no sterile neutrinos). The old route is unchanged.
* Machine: Strategy Table (dice sets, paper, Euclidean Shard, comparator, iron, redstone block).
* Research with derivation puzzles: Games and Strategy, Nash Equilibrium, Evolutionary Games.

**Mathematics Phase 5, part 4: Patterns**
* **Pattern Basin** (a console; Reaction and Diffusion): Turing's activator and inhibitor. Set the spreading ratio r and
  the reaction rate k: below r of about 5.8 nothing forms; above it a pattern forms about 2π·r^¼/√k apart, and that
  spacing must be inside the recipe's window. Faster reactions are quicker but need a larger ratio. Calibration game:
  complete a symmetric pattern.
* New: **Pattern Template** ×2 (2 slime balls, 2 Rational Fields, an Assayed Enzyme).
* New routes (Turing Patterns): **Turing Morphogen** ×2 on the Pattern Basin from an Assayed Enzyme, 2 Rational Fields
  and 4 slime balls; **Morphogenic Serum** ×4 in the Bioreactor from a Turing Morphogen, a Pattern Template and 4
  Engineered Enzymes. The old routes are unchanged.
* Machine: Pattern Basin (glass, Euclidean Shard, slime balls, cauldron, iron, redstone block).
* Research with derivation puzzles: Reaction and Diffusion, Turing Patterns.

**Mathematics Phase 5, part 5: Euler's Identity**
* The branch's keystone, e^(iπ) + 1 = 0, from its five constants: on the Formal Proof Assistant the e Crystal, Imaginary
  Unit and π Crystal prove the **Half Turn** (e^(iπ) = −1); the Half Turn, Unity and Zero prove **Euler's Identity**.
* Euler's Identity is an abstract catalyst (it never wears). New routes with it: **Mathematics Singularity** in the
  Concept Compressor from half of every item and half of three of the fluids (none of the other three); **Riemann Zeta
  Crystal** on the Proof Assistant from 16 Prime Shards, 2 Complex Planes and a π Crystal. The old routes are unchanged.
* Research with a derivation puzzle: Euler's Identity. Codex: Euler's Identity, and **e Everywhere** (where e turns up
  across the mod: reaction rates, decay, Boltzmann, waiting times, control, statistics, the circle).

**Mathematics: the old nodes rebuilt into the new tree**
* The 1.0 and 1.1 mathematics research no longer hangs off physics. New prerequisites: Formal Logic (Number Systems);
  Set Theory (Formal Logic, Cardinality); Real Analysis (Number Systems, Cardinality; was the Platonic Realm and
  Metamaterials); Complex Analysis (Calculus; was Real Analysis and Tachyonics); Abstract Algebra (Groups and Symmetry,
  Modular Arithmetic; was Complex Analysis and Particle Physics); Topology (Euclid's Elements; was Metamaterials);
  Number Theory (Primes, Modular Arithmetic, Complex Analysis). Category Theory now also needs Lie Groups, and
  Conceptual Singularities Euler's Identity. What each unlocks is unchanged.

**Mathematics: the remaining fields**
* Ten new research entries, each with a derivation puzzle: Integers and Rationals, Permutations and Combinations,
  Constructions, Coordinates, Projective Geometry, Eigenvalues, Fourier Series, the Steady-State Approximation, Markov
  Chains and Numerical Methods.
* Recipes that move to a more specific study: the Integer Ring and Rational Field recipes (Integers and Rationals, was
  Number Systems); Golden Ratio Glass on the Geometer's Table (Constructions, was Euclid's Elements); the Fourier Series
  (Fourier Series, was Complex Analysis).
* Studies that now build on them: Stability on Eigenvalues, Enzyme Inhibition on the Steady-State Approximation,
  Stochastic Kinetics on Markov Chains, Optimisation on Numerical Methods.

**The Analytic Continuator, revamped**
* It is a **console** now: analytic continuation, a disc at a time. Set the disc radius ρ and the terms N: each disc
  leaves about (ρ/R)^(N+1) of error, L/ρ discs cover the distance, and the total error must be under the recipe's
  tolerance; fewer terms in all is faster, and past the radius of convergence the series diverges. Each of its 22
  recipes has a distance and a tolerance (harder recipes: further and finer). Calibration game: match a waveform.
* **New machine recipe** from early parts (2 Rational Fields, glass, 2 clocks, a block of redstone, 3 iron; it
  briefly took a third Rational Field in place of one iron by mistake); the old recipe
  (Quantum Processors, Metamaterial Sheets, superconducting coils) is removed.

**Fixes after testing (Phase 5)**
* **The Tuning Fork no longer gives free coolant.** It used the same search as Laplace's Demon (creative), which also
  filled the coolant tank, emptied the spent coolant, cooled the machine and restored its containment integrity. Now it only moves the settings, and only
  looks for settings that work with the coolant the machine actually holds. If none do, it says which coolant would.
  Laplace's Demon is unchanged.
* **Layer hops in the Platonic Realm** (sneaking with the Platonic Compass) no longer play the travel animation twice:
  with no loading screen in between, the scene used to hold and then fade a few seconds after the jump.
* **The console shows what to tune for**, and precision is now measured on it. The Readout tab has a section **For 100%
  precision**: what each of the recipe's windows reads at the recipe's best point (the fastest point inside every
  window, with the coolant the machine holds), and the speed there. Bring the readout to those values (past a
  one-sided target is fine: lower than an "at most", faster than the speed) and each line turns green; precision is
  how close the whole readout is to them. Working out which settings get there is the tuning.
* The **Governor** now brings a machine up to about 75% precision (a fifth of the way toward the best point each
  second) and then holds; it never lowers a machine tuned higher on its console.
* **Euclid's Automaton rebuilt** as a marble and brass golem: broad legs on brass knees and iron feet, a deep chest in a
  brass frame with a turning gear behind a window and the first figure of the Elements inlaid in gold, brass pauldrons,
  a square head with a lit eye slit, and a straightedge in its right hand. Every part is shaded from a lit top to a
  darker foot, with veins in the marble, seams, and rivets on the brass.
* **It walks like a machine:** each leg swings from the hip, the knee lifting the foot clear as it comes forward and the
  planted foot staying flat; the body dips between steps. It turns on the spot, slowly and all the way, before setting
  off (no more snapping and correcting), a little quicker when it is fighting.
* **How it draws:** it drives a pin into the ground, walks once round it with a gold cord taut from the pin to its
  hand (a circle, drawn where it walks), then walks the six sides of the hexagon inside it, each as long as the cord,
  its straightedge on the ground. It only starts where the whole circle fits on the island. The lines stay about half a
  minute, then fade.
* **Roused:** struck, the Automaton hisses; its eyes widen and burn red and stare at whoever struck it (its head follows
  them), the core behind its chest window glows red and the gear there races, and the vents on its shoulders glow and
  pour steam. It calms down again after the fight.
* **Its fall:** a golem's death, over about two seconds: it shudders with a burst of steam as its eyes flicker; its knees
  buckle and it sinks to kneel; it topples forward onto the ground, arms falling ahead of it; its head breaks from its
  neck and rolls aside, and the light in it goes out.
* **Network minigames:** links are drawn as clean, solid lines (they were dotted at some angles), brighter, and the
  lengths sit beside their links instead of on them. Links no longer pass close by a junction they do not join.
* **Music by layer in the Platonic Realm:** a new piece for the Elements: one plain tune in D Dorian in the middle
  register, on a lyre and then a flute-like voice, over the lyre's broken chords, a soft held chord and a quiet root. Above Y 256
  the Transfinite Hierarchy's own piece plays again (since the merge, the Realm's piece had played there too); below
  Y 196, the Realm's.

**The Realm in Layers: the Platonic Realm, finished** (Y −768 to 1536)
* The Realm is much taller, Y −768 to 1536, and laid out as the Mathematics design drew it: one layer for each part of
  mathematics, bottom to top, with **wide gaps of empty air between them**. Climbing or falling from one layer to the
  next is a long way: the Platonic Compass is the way between them. This replaces the heights given above under One
  Realm and The Elements.
  * **The Axioms** (logic): a floor of dark **Postulate Slate** far below everything else, Y −720 to −650.
  * **The Number Line**: the Realm's own islands, Y 10 to 194, exactly as in 1.1 (so released worlds line up).
  * **The Elements**: Euclid's marble islands, now Y 320 to 400.
  * **Curved Space** (topology): islands of **Hyperbolic Stone**, Y 560 to 720.
  * **The Continuum** (analysis): **Dense Stone** in islands at three scales, each like the last, Y 880 to 1040.
  * **The Infinite** (set theory, Y 1280 to 1536): the Transfinite Hierarchy's strata, above the widest gap of all
    (240 blocks of nothing). (Mathematics was never released on its own; in a world from one of its test builds, areas already explored keep the old heights.)
* **The landmarks are bigger to match:** Compass Towers twice as tall with a wider platform; the Geometer's Ruin drawn
  at twice the size (legs 16 and 12, hypotenuse 20); a bigger Liar's Vault; Proof Stairs of 32 to 48 steps; Möbius
  Arches nearly twice as wide; Hyperbolic Floors 39 blocks across; Fourier Harps of 33 rods, twice as tall.
* **Getting about:** sneak and use the Platonic Compass to climb one layer, or sneak and look down to go down one. It
  stops below the Infinite, which opens only to the Cantor Key (flying up to it turns you back, see below).
* **New ores, new materials** (each renewable from the layer's creature):
  * **Tautology Ore** in the Axioms gives **Tautology Shards**.
  * **Geodesic Ore** in Curved Space gives **Geodesic Thread**.
  * **Epsilon Ore** in the Continuum gives **Epsilon Dust**.
* **New recipes**, cheaper routes for things the layers' fields make:
  * Formal Proof Assistant: **Proof Certificate** ×2 from 2 Tautology Shards and 2 paper (Proof by Induction); **Axiom
    Schema** ×2 from 2 Tautology Shards, 2 Natural Number Lattices and a book (Formal Logic).
  * Topology Kiln: **Differential Manifold** from a Derivative Operator and 2 Geodesic Thread (Manifolds and Curvature).
  * Analytic Continuator: **Real Continuum** ×2 from 8 Rational Fields and 4 Epsilon Dust; **Infinitesimal** ×4 from a
    Real Continuum and 2 Epsilon Dust (Real Analysis).
* **Landmarks**, built only from the mod's blocks, three with caches:
  * In the Axioms: **Liar's Vaults** (a sealed hall floored in true and false, with two doors, only one of them real) and
    **Proof Stairs** (a spiral where every step stands on the one before, with a cache at the top).
  * In Curved Space: **Möbius Arches** (a band with a half twist; walk through one and you come out mirrored) and
    **Hyperbolic Floors** (a Poincaré disc laid in stone).
  * In the Continuum: **Fourier Harps** (rods whose heights add up the first few harmonics of a square wave).
* **Creatures**, each with its own AI:
  * **Paradox Shade** (Axioms): it flips between true and false every few seconds. While false it can neither be hurt
    nor hurt anyone; while true its touch brings Contradiction. Drops Tautology Shards, rarely a Proof Certificate.
  * **Tessellate** (Curved Space): a tile of seven-sided pieces that splits in two when killed, twice. The smallest drop
    Geodesic Thread, sometimes a Symmetry Crystal.
  * **Fourier Wisp** (Continuum): a flying set of circles turning on circles, which keeps away from players. Drops
    Epsilon Dust, rarely a Fourier Series.
* **Hazards** (effects only, never blocks):
  * Axioms: now and then **Contradiction** gives you two opposite effects at once. Carrying a Proof Certificate keeps
    it away.
  * Curved Space: inside a Möbius Arch you are **Mirrored**: left and right swap.
  * Continuum: near a Fourier Harp you feel the **Limit**. The closer you get, the slower you go.
* **Music:** each layer has its own piece (the Axioms, Curved Space, the Continuum).
* Advancements: The Axioms, Curved Space, The Continuum. Codex: The Realm in Layers, and a page for each new layer.

**Fixes after testing (the Realm in Layers)**
* **No more lava at the bottom of the Realm.** With no aquifers, the game fills every gap below Y −54 with lava unless
  the sea level is lower; the Realm's sea level is now at its floor (Y −768), so the Axioms stay dry.
* **The Tessellate is far cheaper to draw:** each tile is one seven-sided prism instead of fourteen boxes, and the
  third ring of tiny tiles is gone (eight tiles where there were twenty-two), so a crowd of them no longer lags.
* **Euclidean Marble, Hyperbolic Stone and Dense Stone** lose the moving glint: still, matte textures.
* **The Fourier Wisp** is about twice the size, with a hitbox to match, so it is easier to see and to hit.
* **The Infinite can no longer be reached by flying up.** Cross Y 1280 from below (flying, climbing or by ender pearl)
  and you are turned back just below it; only the Cantor Key carries you in. Coming down is free. Nothing is built
  there: it is a rule of the place, not a wall.

**Fixes after testing (the taller Realm)**
* **No snow on the high layers.** The game cools every biome a little for each block above Y 80 and lets snow settle
  wherever it gets cold enough while it rains in the Overworld (every dimension shares its weather), even in a biome
  with no rain of its own. The Realm is now warm enough (temperature 2.0) that snow never settles, up to its very top.
* **The layer stones are useful and renewable.** Each is its field's raw feedstock, as axiom stone is the Number Line's,
  and each is made again from 8 cobblestone and one of its layer's materials (8 stone):
  * **Postulate Slate** (Formal Proof Assistant, Formal Logic): 2 Proof Fragments from 32 slate and 2 Tautology Shards;
    8 slate from 8 cobblestone and a Tautology Shard.
  * **Euclidean Marble** (Geometer's Table, Constructions): 37 Golden Ratio Glass from 8 marble and 8 glass; 8 marble
    from 8 cobblestone and a Euclidean Shard.
  * **Hyperbolic Stone** (Topology Kiln): a Möbius Strip from 16 stone and a Function Graph (Euler Characteristic); 8
    stone from 8 cobblestone and a Geodesic Thread, fired as a surface with two handles, χ = −2 (Manifolds and
    Curvature).
  * **Dense Stone** (Analytic Continuator, Real Analysis): a Transcendental Crystal from 32 stone and 4 Rational Fields;
    8 stone from 8 cobblestone and an Epsilon Dust.
* **The Compass works in the Infinite:** sneak, look down and use it to go down to the Continuum (the gap below is too
  far to fall). Climbing into the Infinite still needs the Cantor Key.
* **Codex:** The Realm in Layers, The Axioms, Curved Space, The Continuum, The Elements and One Realm are rewritten for
  the taller Realm: the heights, the gaps and how to cross them, the bigger landmarks, and what each stone is for.

**Recipes, texts and tooltips: the Mathematics audit**
* **Recipe changes** (only to recipes added after 1.1):
  * **Analytic Continuator**: its bottom row is now 3 iron. A key mix-up made it take a third Rational Field (27 on the
    9×9 grid) in place of one iron.
  * **Ceramic Armour Plate** (it mends Whipple Shield armour): now 3 Alumina Ceramic Plates and 3 Aramid Fibre on an
    ordinary crafting grid, instead of 45 plates and 36 fibres.
  * **Entanglement Tuner** from One-Time Pads: now 2 pads, 2 quartz and an iron ingot for 4 tuners, on an ordinary
    crafting grid (it cost more than the early route).
  * **Regression Assay**: 1 Engineered Enzyme and 1 paper now give 16 Assayed Enzymes (it gave 2 for 2 paper, worse than
    the Enzyme Assay Bench's route).
  * New: **Prussian Blue Tablet** ×4 from a Prussian Blue Pigment and sugar (the pigment had no use). The old recipe is
    unchanged.
* **Puzzles:** the vector symbols (vector, dot and cross) are known from the start, since Abstract Algebra can now be
    studied before a Field Calibrator is made.
* **Codex brought up to date:**
  * Research pages list the prerequisites they actually have now (Complex Analysis, Abstract Algebra, Set Theory, Number
    Theory, Category Theory, Grand Unification, Stability, Enzyme Inhibition, Stochastic Kinetics) and the derivation
    the research table sets now.
  * "Unlocked by" names the right study for Fourier Series, Golden Ratio Glass, Integer Rings and Rational Fields, and is
    no longer printed for crafted things on the strength of a vanilla ingredient (it said the Alkahest for some forty
    of them, because the Alkahest can make gold).
  * Tuning: the console, precision, the Tuning Fork and the Governor are mentioned, and a machine at zero containment
    fails (releasing what it held) rather than just faulting.
  * Containment Failure now describes all ten kinds (chemical, radiological, magnetic, thermal and optical were
    missing), and mentions Proof Certificates.
  * The calibration games list includes the mathematics games (here and in the Tuning Fork's tooltip).
  * Consoles on single machines: they take the mathematics upgrades.
  * The chemistry index lists only chemistry, with whole branch names.
  * No more empty "Limits: ." lines on machine pages; 169 "Made in a crafting grid" lines now say the Extreme Assembly
    Table.
  * Euler's identity: what the Mathematics Singularity leaves out (Axiom of Choice, Gold Singularity, Topos).
  * Fourier Series is its own study, not part of Complex Analysis.
  * Research pages no longer carry their icon item's "Found" line.
* **Tooltips:** the Heated Pressure Suit has a real description; "Calls a The Ouroboros" fixed; the Imaginary Unit names
  its Integrator route; about 320 items and blocks whose description never said where they come from now end with
  where they are made.
* Checked: everything is still reachable (also after the Shutdown), no research is cut off, and no newer route costs
  more than the one it replaces.

### Part one: Chemistry

**In numbers** (1.1.0 → Chemistry): 34 new machines and consoles, 37 research entries with derivation puzzles, 41 items,
30 blocks, 27 fluids, 5 creatures (one a mini-boss), a new dimension (Titan, with 3 kinds of ruin) and about 170 new
recipes. Of the 1.1 recipes, only the 9 listed under "Recipe changes" differ (catalysts held instead of used up).

**Phase 1, part 1: wear and decay**
* **Catalysts and instruments wear.** Each time one serves as a recipe's catalyst it uses a run of its life (ten runs
  when the machine is pushed past one of its physics "wear" limits). Its tooltip and a bar on the slot show what is left.
  Worn out, it comes out of the machine marked worn and no longer works until restored:
  * Zeolite Catalyst: 256 runs, restored in the new **Regeneration Kiln**.
  * Muonic Catalyst: 150 runs, then used up (a real muon catalyses about 150 fusions before it sticks to a helium
    nucleus).
  * Lenses, the Schmidt Corrector, Echelle Grating and Apochromatic Objective: 256 runs, restored in the Optical Workshop.
  * Neodymium, Ferrite and Samarium-Cobalt Magnets: 256 runs, restored in the Magnetizer.
  * The Perfect Crystal Interferometer: 512 runs, restored in the Crystal Growth Furnace.
  * Random Number Chips, Boundary CFT Chips and Quantum Processors: 1,024 runs, then used up.
  * Mathematical, abstract and endgame catalysts never wear.
* **Every machine has a catalyst slot** (the gold slot under the progress arrow). It holds one catalyst; shift-click
  puts a catalyst there first. A catalyst that wears or decays only works from this slot, so it is always that one that
  wears (one in an input slot, or in an ME network behind an Interface Port, no longer counts). Catalysts that never wear
  still work from an input slot too. A worn-out catalyst moves to the output slots. Machines from older worlds move a
  wearing catalyst from their input slots into the new slot by themselves.
* **Radioactive sources decay** by their isotope's half-life on the Decay Chamber's clock, wherever they are (a chest,
  a machine, an ME network): Radium Salt, Cobalt-57, Californium-252, Polonium-210 and Hafnium-178m2.
  A source below 1/16 of its first activity is spent and no longer works as a catalyst; it also gives off less
  radiation as it decays. Cobalt-57 now gives off gamma rays (it had no radiation before).
* **Technetium-99m** (15 seconds of half-life on the scaled clock) decays only while out in the open: carried loose or
  lying on the ground. Stored anywhere (a machine, an ME network, a chest, a Chronostatic Vault) it waits. Carry it in
  the new **Lead Pig** (holds 64 vials).
* **The Chemical Reactor has an Operation console** like the physics multiblocks. Set the heater temperature and the
  residence time; the rate constant k = A·e^(−Eₐ/RT) (Arrhenius) gives the conversion X = 1 − e^(−kτ), which must reach
  each recipe's target. A shorter residence time runs faster (up to four times the old pace). A catalyst lowers the
  activation energy, but above its sintering temperature (700 K) it wears ten times as fast. The vessel is rated to
  850 K. Its default settings (400 K, 60 s) still run every easy reaction at the old pace.
* **Recipe pages (JEI and EMI) are drawn from one shared page:** the catalyst in its gold catalyst slot with what wears
  it, every operating-window line, restoring and Technetium notes, and pages that grow to fit (nothing cut off).
* **Chronostatic Vault:** a 27-slot cache that stops the sources in it from decaying (hoppers and pipes work with it).

**Phase 1, part 2: Kinetics & Equilibrium**
* **Haber-Bosch Reactor** (multiblock): hydrogen and nitrogen over an **Iron Catalyst** into ammonia. Its console sets the
  bed temperature, the pressure and the H₂ : N₂ ratio; the equilibrium (from ΔG° = ΔH − TΔS) favours cold and pressure,
  the iron's rate favours heat. A pass needs 15% ammonia. The iron sinters above 823 K; the vessel is rated to 300 atm.
* **Contact Process Tower** (multiblock): sulfur dioxide, oxygen and water over a **Vanadium Bed** into sulfuric acid.
  The bed must light off (above 670 K) without the equilibrium falling back; a pass must convert 95%.
* **Ostwald Converter** (multiblock): ammonia and oxygen through a **Platinum-Rhodium Gauze** into nitric acid. Hot gauze
  and a millisecond's contact for 90% selectivity; above 1250 K the gauze loses platinum.
* **Air Separation Unit** (single block): nitrogen and oxygen from the air, for power alone.
* New fluids: Hydrogen, Nitrogen, Oxygen, Ammonia, Sulfur Dioxide, Nitric Acid and Sulfuric Acid.
* **Shared chemicals:** a recipe that asks for the mod's Hydrogen, Oxygen, Nitrogen, Ammonia, Nitric Acid, Sulfuric
  Acid, Sulfur Dioxide, Heavy Water, Deuterium or Tritium takes any fluid in the matching common tag (c:hydrogen...) -
  Mekanism's, NuclearCraft's or GregTech's as readily as the mod's own. The mod's fluids are in those tags too.
* The three new catalysts wear (Iron Catalyst and Vanadium Bed 512 runs, the gauze 256) and are restored in the
  Regeneration Kiln.
* Research: Reaction Kinetics, Chemical Equilibrium, The Contact Process, The Ostwald Process, each with its derivation.

**Phase 1, part 3: Acids, Bases & Solutions** (four single blocks, each on a console)
* **Titration Bench:** add titrant until the pH sits in the endpoint window (4 to 10) and the reagent comes out
  **assayed** - the purity tag. Sulfuric and nitric acid, ammonia and sulfur dioxide can be assayed.
* **Catalyst poisoning:** a run on an acid, base or sulfur dioxide feed that has not been assayed costs the chemical
  catalysts (Zeolite, Iron, Vanadium, the Platinum-Rhodium Gauze) five runs of life. Pipes and ME networks keep the tag;
  a containment cell does not. (No 1.0 or 1.1 recipe takes one of these feeds, so none is affected.)
* **Buffer Tank:** pH = pKa + log(ratio) at a set pH - Phosphate Buffer (pH 7.4) and Ammonia Buffer (pH 9).
* **Brine Evaporator:** salts come out of Deep Brine in order of solubility - Gypsum, Halite, then Lithium Carbonate -
  in their own bands of concentration; too hot and the crystals are impure.
* **Reverse Osmosis Cell:** water passes the membrane only above the osmotic pressure π = iMRT; makes Concentrated
  Brine (which dries to halite sooner) and fresh water. Rated to 80 bar.
* Research: Acids and Bases, Buffers, Solubility, Osmosis.
* The Pleroma Confluence keeps the list of fluids it asked for in 1.1: the new 1.2 chemicals are not added to it (so a
  confluence already filling is not set back) until its planned rework.

**Phase 1, part 4: Thermochemistry** (two single blocks on consoles)
* **Bomb Calorimeter:** burn a weighed sample in oxygen; size it so the water's rise is readable (1.5 to 3 K) and give it
  enough oxygen to burn completely. Each burn makes a Thermochemical Data Card and Insight.
* **Heat-Pack Reactor:** reactions as heat sources and sinks, the outlet held in each recipe's band. Ammonia and nitric
  acid neutralise to Ammonium Nitrate (mixed too fast, it runs away); dissolving it makes **Cold Pack Solution** (268 K)
  and salt with ice **Eutectic Brine** (252 K) - both new coolants for the physics machines' coolant loops.
* Research: Calorimetry, Thermochemical Cycles.

**Phase 1, part 5: Electrochemistry**
* **Fuel Cell** (a generator on a console): hydrogen or ammonia with oxygen into power (and water). The Nernst voltage
  less its losses: the power peaks near 1 A/cm²; too little current floods the cell, too much starves it.
* **Vanadium Flow Battery** (storage block, 50 million FE): its voltage follows its state of charge; right-click to read
  both. It stops taking charge at 95%.
* **Electroplating Bath** (on a console): Faraday's law sets the coat; 20 to 50 µm of nickel on iron makes
  Nickel-Plated Iron and Corrosion-Proof Casings. Above 8 A/dm² the coat burns.
* The Electrolytic Cell gains the chlor-alkali process (salt and water into Chlorine and Caustic Soda) and lithium from
  lithium carbonate. Chlorine is shared with the pack by its c:chlorine tag.
* Research: Galvanic Cells, Electroplating. This completes Phase 1 (Foundations).

**Phase 2, part 1: Analytical Chemistry, the nebula gases, Catalysis**
* **Chromatography Column** (a tower, one storey of column per layer, up to nine): van Deemter's plate height and the
  resolution Rs (≥ 1.5) on its console; harder splits need a taller column. It splits Molecular Cloud gas into ammonia and
  water, methane and hydrogen cyanide, the interstellar medium into PAH Soot - and **assays gases**: Hydrogen, Nitrogen,
  Oxygen, Methane and Carbon Monoxide now poison a chemical catalyst too when fed in unassayed.
* **Mass Spectrometer** (on a console): set the field so the target mass lands on the detector; deuterium from the cloud
  (cheaper than heavy water) and lithium-6 from lithium.
* **Cloud Siphon:** touching a Hydrogen, Oxygen or Dust Cloud in the Frontier's nebula, it draws that cloud's gas for
  ever. The Fractionation Spire (run cold) parts hydrogen and carbon monoxide out of the cloud's gas.
* New fluids Methane, Carbon Monoxide, Hydrogen Cyanide, Methanol; Methanol from CO and hydrogen; the Strecker route to
  an Amino Acid Mixture; graphene from PAH soot (a cheaper route; the 1.0 route is unchanged); methane runs the Fuel Cell.
* **Catalyst Formulator** (on a console): the Sabatier volcano - set the metal loading and calcination so the binding
  sits at the peak, without sintering. Makes the Iron, Vanadium and Zeolite catalysts more cheaply, and the new **Nickel
  Catalyst** (256 runs, restored in the kiln).
* The **Regeneration Kiln** gains a console: burn the coke off (hot enough, air enough) below the metal's sintering
  temperature; 90% must come back.
* Research: Chromatography, Mass Spectrometry, Nebular Chemistry, Heterogeneous Catalysis.

**Phase 2, part 2: Organic Synthesis & Polymers, Frameworks & Porous Materials**
* **Synthetic Crude** from coal and hydrogen (Bergius) - the pack's crude oil (c:crude_oil) is taken as readily.
* **Cracking Tower** (multiblock, Zeolite Catalyst): Ethylene near 1100 K, Propylene near 1000 K; too hot and coke fouls
  the catalyst, steam keeps it down.
* **Polymeriser** (console): chain length 1/(1 − p); Polymer Sheet, Nylon Fibre and Aramid Fibre. Above 420 K it runs away.
* **Synthesis Bench** (console): a route of n steps keeps 0.92ⁿ - as short as the target allows; makes Fine Reagents.
* **Framework Assembler** (console): the pore must fit the target molecule, dried slowly; MOF Gas Sorbent, Silica Aerogel,
  and a second route to the Metal-Organic Framework.
* **MOF Gas Storage Cell** (block): holds 256,000 mB of one gas (sixteen tanks' worth); keeps it when broken.
* **Aluminium** (Bayer and Hall-Héroult: clay and caustic soda, then electrolysis), tagged c:ingots/aluminum.
* **Quasicrystal Furnace** (console): a narrow cooling window and the right ratio for five-fold order; Quasicrystal Plate.
* Research: Petrochemistry, Polymer Chemistry, Organic Synthesis, Reticular Chemistry, Quasicrystals.

**Phase 2, part 3: Inorganic & Coordination, Spectroscopy, Materials & Solid-State**
* **Coordination Reactor** (console): the ligand field's splitting against the pairing energy picks high or low spin;
  Prussian Blue Pigment, the Porphyrin Oxygen Carrier (haem) and the Copper Ammine Complex.
* **Chelation Column** (console, EDTA in its catalyst slot): rare earths from deep brine; spent fuel salt cleaned back
  into fuel salt. Too much flow saturates it.
* **Infrared Spectrometer** (console): scan across the bond's band, narrowly - Structure Cards. With one in its catalyst
  slot, the Synthesis Bench takes a shorter route (two Fine Reagents for the same inputs).
* **NMR Spectrometer** (multiblock): resonance within 5 ppm of f = γB/2π; a superconducting magnet kept below 10 K and
  under 22 T or it quenches. Structure Cards and Insight.
* **Phase Diagram Kiln** (console): Superconducting Solder from lead and copper at the eutectic (a cheaper route; the
  1.0 route is unchanged).
* **Doping Furnace** (console): Doped Wafers (10¹⁶ to 10¹⁸ carriers), which the Lithographer turns into two Photonic
  Logic Wafers.
* **Sintering Press** (console): dense, fine-grained Alumina Ceramic Plates; with Aramid Fibre, Ceramic Armour Plates.
* Research: Coordination Chemistry, Chelation, Infrared Spectroscopy, Nuclear Magnetic Resonance, Phase Diagrams,
  Semiconductor Doping, Ceramics.

**Phase 3, part 1: Titan** (a new dimension)
* **Titan**, Saturn's largest moon: an orange sky, a seventh of Earth's gravity, and seas, lakes and rain of methane at
  94 K. Six regions: Kraken Mare (a methane sea), the Shangri-La Dunes (tholin sand), the Xanadu Uplands (water-ice
  bedrock), the Ontario Lacus Basin (lakes and dried-up evaporite crust), the Sotra Cryovolcanic Field (cryolava) and the
  plains round Selk Crater. Its floor is the Subsurface Seal, a layer of high-pressure ice nothing can break yet. It has
  its own travel animation and its own music.
* **Cryogenic Lander** (research *Titan*): used under open sky in the Overworld it lands you on Titan; used on Titan it
  takes you home under a parachute. Falling out of the bottom of Titan also sends you home.
* **Hazards** (none of them changes a block):
  * Cold: you freeze as in powder snow, then take frost damage. The **Heated Pressure Suit** (a Pressure Suit crafted with
    two Silica Aerogel and redstone) keeps out a quarter of it per piece, all of it as a full suit. It still counts as a
    Pressure Suit in the Planetary Interior.
  * Methane rain: under open sky in a storm it soaks a suit's lining and chills you as much again. The **Hydrophobic
    Coating** (used while wearing armour; one can a piece) sheds a quarter of the soaking per coated piece. A coated piece
    says so in its tooltip.
  * No oxygen: your air runs out as if under water. The **Oxygen Rebreather** (anywhere in your inventory) holds twenty
    minutes of oxygen and spends a second of it every second; crafting a spent one with an Oxygen Gas Cell refills it.
  * No fire: flint and steel and fire charges fail, and burning stops.
  * Low gravity: you fall more slowly, take less fall damage and jump higher. Liquid methane cannot be swum in - you sink.
* **Lake Pump** (single block): placed on Titan touching Liquid Methane or Cryolava, it pumps that liquid without ever
  emptying the lake.
* New materials: Water-Ice Bedrock, Tholin Sand, Tholin, Cryolava Rock, Evaporite Crust, Evaporite Deposits (Organic
  Evaporite), Liquid Methane and Cryolava. Research: Titan, Cryogenic Survival, Lakes of Titan.
* Codex: Titan, Surviving Titan and Lakes of Titan pages. The material pages of the 1.2 blocks and fluids (49 of them,
  from casings to gases) now show their descriptions instead of only their names.

**Phase 3, part 2: Life on Titan, and Photochemistry**
* **Titan's creatures**, each with its own mind:
  * Tholin Drifter (the dunes, the sea, the uplands): a flake of haze drifting on Titan's wind, which turns with the
    day. Whoever it settles on gets Darkness and Slowness. Drops Tholin.
  * Catalyst Sprite (Ontario Lacus): keeps five blocks off and circles; every three seconds it spends half a minute of
    the oxygen in your rebreather and heals Titan's other creatures. Hurt, it backs off. Drops Organic Evaporite and,
    rarely, a Nickel Catalyst.
  * Methane Skater (Kraken Mare): stands on liquid methane, skates straight at whoever is on the shore and knocks them
    in. Can drop a Hydrophobic Coating.
  * Cryolava Slug (Sotra): slow and armoured; it freezes what it touches and spits cryolava from twelve blocks.
* **The Ouroboros** (mini-boss): summoned with **Kekule's Dream** in Selk Crater. A ring of six segments that, while
  closed, takes a quarter of each blow ("aromatic"); every twenty seconds it opens for five, taking double damage
  while its head lunges. Closed, it spits tholin at everyone in sight. At half health two Catalyst Sprites join it.
  Drops the Kekule Ring.
* **Structures** (the mod's own blocks only): the Abandoned Refinery (basin and sea), the Lander Wreck (dunes, uplands,
  Selk Crater) and the Cryolab (cryovolcanic field, uplands), each with a cache. A Cryolab's cache can hold Kekule's
  Dream.
* **Photochemical Reactor** (console, research *Photochemistry*): light absorbed by the Beer-Lambert law. Set the
  path length so the mixture absorbs 90% to 99.9% of the light. Above 400 W/m² the lamp cooks the cell.
* Codex: Life on Titan, The Ouroboros, Photochemistry.

**Phase 3, part 3: Titan, reworked after testing**
* **Methane Skater** rebuilt. It now has a head with a beak, eyes and twitching antennae, a long thorax with silvery
  water-repellent hairs, and a tapering abdomen. The short front legs are held forward, the long middle legs row and
  the hind legs steer. Every leg rises to a knee and comes down to the surface, where its foot dimples the methane. The
  middle legs row in strokes, and the body surges with each one. It is never still: at rest the legs flex and the body
  bobs. It has its own voice, made of foot ticks, surface "plips" and ripples, with a splash when hurt and sinking
  bubbles when it dies.
* **Cryolava Slug** now moves like a slug. A wave of contraction runs along its foot all the time (slow at rest,
  quick when it crawls), so the body bunches and stretches. Its eye stalks and feelers sway, and it leaves a slime
  film. Its new voice is a wet squelch with sticky pops and ice cracking in its skin.
* **Catalyst Sprite** is about twice the size, with a second ring and a glow, so it is easier to see and hit.
* **Selk** is now a crater field: impact craters 16 to 26 blocks across, each a bowl with a raised rim. The bigger
  ones have a central peak, as Selk itself has. The biome is renamed Selk Crater Field.
* **The Ouroboros** has its own sounds for summoning (a rumble from the crater and six notes climbing as the ring
  closes), for the ring opening, for its strikes and spits, for its voice, and for its death (a falling groan as its
  six bonds break one after another). Summoned, it rises out of the crater over four seconds. Its segments join one by
  one until the ring closes, and it cannot be hurt until then. Dying, the ring breaks and its segments fly apart,
  tumbling and fading, over two and a half seconds.
* **Every Titan material is renewable and has a use:**
  * Organic Evaporite: liquid methane boiled down in the Brine Evaporator (20 to 50 times concentrated), or Evaporite
    Crust dissolved in methanol.
  * Evaporite Crust: crafted from Organic Evaporite and Tholin Sand.
  * Cryolava Rock: remelts into cryolava (Chemical Reactor).
  * Water-Ice Bedrock: crafted from two Packed Ice.
  * The Kekule Ring is a permanent catalyst: in the Polymeriser's catalyst slot it makes Aramid Fibre from Organic
    Evaporite and ammonia. Kekule's Dream can always be crafted again, so the Ouroboros can always be summoned again.
* **More life on Titan:** much higher spawn weights and bigger groups. The floating creatures and the Skater can now
  appear over lakes and the sea, in any light. Every ten seconds, if fewer than eight of Titan's creatures are within
  48 blocks of a player, one of the region's own creatures comes in 20 to 40 blocks away.

**Phase 3, part 4: Titan fixes**
* Cryolava Slug: the see-through shell and the slime film behind it are gone (they showed as a blue plane and
  flickered). The body now sits on its foot instead of sharing its base, and its segments meet without overlapping,
  so it no longer flickers when struck, climbing or dying.
* Methane Skater: the flat "dimple" planes at its feet are gone, and the hair is drawn solid. It now really walks.
  On ground all six legs move in an alternating tripod, each foot lifting, swinging forward and planting, with stride
  and speed set by how fast it actually moves. On the methane both middle legs row together while the others steer.
* Titan's creatures no longer spawn inside liquid methane (or with liquid just above them). Being native to 94 K and
  its liquids, they no longer freeze or drown in it.

**Phase 4: Asymmetric Synthesis, Oscillating Reactions, Alkahest** (the end of the Chemistry branch)
* **Asymmetric Reactor** (console, research *Asymmetric Synthesis*): a handed catalyst favours one mirror image by
  ΔΔG‡ (s = e^(ΔΔG‡/RT)). Run it cold enough for at least 90% of one hand; colder is purer and slower. Its catalyst,
  the **Chiral Ligand**, is made at the Synthesis Bench with the Ouroboros's Kekule Ring in the catalyst slot.
* **The first pair of routes to one product**, by choice: Enantiopure Reagent from the Asymmetric Reactor is fast but
  random (60% of runs give it), or from the Chromatography Column it is slow but sure (two Fine Reagents give one,
  every time, because resolving throws the other hand away).
* **Oscillator Vessel** (console, research *Oscillating Reactions*): the Belousov-Zhabotinsky reaction as the
  Brusselator. It beats only past B = 1 + A², with a period near 2π/A. It makes **Chemical Clocks**.
* **Alkahest Still** (console, research *Alkahest*): **Alkahest**, Chemistry's unifying fluid, distilled in four
  stages that pass through every branch:
  * Tincture of Balance: kinetics, equilibrium, acids, heat and current.
  * Tincture of Form: spectroscopy, synthesis, frameworks and chains.
  * Tincture of Chirality: handedness, time and Titan (with Ammonia Buffer).
  * Alkahest: coordination and materials (with a Copper Ammine Complex), with the Kekule Ring as catalyst.
* Alkahest dissolves ore: four raw iron, gold or copper and 10 mB give twelve ingots. It is kept for Pleroma's
  rework (where it will be one of the five fluids); Pleroma's 1.1 recipe is unchanged.
* Codex: Asymmetric Synthesis, Oscillating Reactions, Alkahest.

**Fixes after testing**
* Suit tooltips (Whipple Shield, Pressure, Refractory, Frontier and Heated Pressure Suits) show one description, at the
  top, instead of a second one split off below the armour values.
* The older fluids' cells and fluid blocks (69 of them, from Quark-Gluon Plasma to Maxwell-Boltzmann Gas) now say what
  the fluid is and where it comes from (the machines that make it, and where it lies in the world), as the new ones
  do, instead of a generic "1,000 mB of ..." line.
* The Codex's "Start here" label sits above the first Getting Started node, so it no longer covers the next ones.
* The Codex no longer counts days ("Day 96. ..." at the start of fifteen notebook pages).
* **Containment failures for every multiblock since 1.0.** 45 multiblocks wore down and sounded the meltdown alarm
  but released nothing. Each now has a failure, and five new kinds fit the newer sciences:
  * Chemical (the chemical plants, the Chromatography Column, the Markov Refinery): a burst and a corrosive cloud that
    poisons, weakens and eats at armour.
  * Radiological (reactors, centrifuges, cyclotrons, X-ray and gamma sources, the Micro Black Hole Evaporator): a dose
    falling off as 1/r², a contaminated cloud, and Fission Husks rising from what it caught.
  * Magnetic (Tokamak, Pulsed Magnet Lab, NMR, MHD Generator, Tesla Coil, Radio Telescope): the field collapses,
    flinging and magnetising everything near, with arcs, and leaves Geodynamo Wraiths.
  * Thermal (presses, drilling rig, furnaces, Impact Forge, Climate Engine, Unruh Accelerator, Analog Black Hole): a
    blast of heat that sets everything near alight, and Plume Elementals.
  * Optical (telescopes, Optical Workshop, Laser Interferometer, Brownian Microscope, Lensing Concentrator): a blinding
    flash, flying glass, and Lens Phantoms.
  * The exotic ones use the existing kinds (spatial, temporal, dimensional, infectious). Block damage still follows
    the config and the mobGriefing rule.
  * These machines also gain the tuning panel's overclock and parallel settings, like the older multiblocks.
  * The Containment Failure page in the Codex lists the new kinds.

**Keeping Things Running** (cross-dimensional transport and chunk loading)
* **Entangled Link** (new research *Entanglement Links*, right after Quantum Entanglement): the first, early tier of
  entangled transport. Placed on a block's face, links on one channel move items, fluids and energy between them at any
  distance and across dimensions: 16 items, 4,000 mB and 262,144 FE a tick. Sneak-use switches sending and receiving.
  The later Relay and Transceiver use the same channels and move far more. Crafted (two at a time) from copper,
  Entangled Photon Pairs, ender pearls and a block of redstone.
* **An early Entanglement Tuner** recipe (an Entangled Photon Pair, quartz and iron), so channels can be set from the
  start; the 1.1 recipe is unchanged.
* **Zeno Anchor** (new research *The Quantum Zeno Effect*): a chunk loader. Powered (64 FE a tick, 1,000,000 FE
  stored), it keeps the 3 x 3 chunks round it loaded and ticking whether or not anyone is near, and across restarts.
  It lets go when its power runs out or it is broken; right-click shows its state. Crafted from obsidian, Entangled
  Photon Pairs, a clock and a block of redstone.
* **The ruins' glitching screen is tied to their Breached Anomaly Engine.** It used to cover the whole ruin for good;
  now it lasts only while the ruin's engine stands, and stops for good once the engine is broken.
* Codex: Keeping Things Running (Getting Started), Entanglement Links, The Quantum Zeno Effect.
* Last uses for Chemistry's materials that had none: Gypsum Crystal roasts to sulfur dioxide (Chemical Reactor); Ceramic
  Armour Plates make Whipple Shield pieces (crafted, a second route; the 1.1 recipes are unchanged); Ammonia Buffer
  goes into the Tincture of Chirality and the Copper Ammine Complex into Alkahest. (Later, in Mathematics: the ceramic Whipple
  Shield routes are gone, as they cost far more than the plain ones; Ceramic Armour Plates now mend Whipple Shield
  armour at an anvil instead.)

**Look and text**
* Every containment cell, filled or empty, now has the same look (the one the newer fluids had); 54 older fluids'
  cells and the empty cell were redrawn.
* No more dashes used as punctuation in anything you read (tooltips, the Codex, research, derivations, physics notes,
  messages, the Living Proof's questions): each became a comma, a pair of commas round an aside, or a semicolon.
  Minus signs in formulas are untouched.
* **Codex maps reorganised.** Discovery is now a grid: a lane (row) for each area (Foundations, Worlds, Quanta &
  Matter, Fields & Nuclei, Stars & Spacetime, Chemistry, Life & Minds, Mathematics, Transcendence), labelled at its
  left, and a band for each stage of the game, labelled across the top. Getting Started runs along the top. Links
  run along their lane; links that would have crossed more than two lanes are left off (hovering still shows what a
  chapter builds on). The branch tabs give each branch a lane of its own, packed in a few columns, with every chapter
  among its things.

**Recipe changes**
* New: Regeneration Kiln (crafted), Chronostatic Vault (crafted), Lead Pig (crafted from 7 Lead Shielding Plates), and the restoring recipes in the Regeneration Kiln,
  Optical Workshop, Magnetizer and Crystal Growth Furnace.
* New recipes: Haber-Bosch Reactor, Contact Process Tower, Ostwald Converter, Air Separation Unit and their casings
  (crafted); hydrogen from coal or charcoal and steam (Chemical Reactor) or water (Electrolytic Cell); sulfur dioxide
  from gunpowder or sulfur dust; the Iron Catalyst, Vanadium Bed (from raw iron, or vanadium dust) and Platinum-Rhodium
  Gauze (from meteoric iron and sulfuric acid, or platinum and rhodium) in the Chemical Reactor.
* The Electrolytic Cell's heavy water, deuterium and tritium recipes (and every other recipe that takes them) now accept
  the pack's heavy water, deuterium and tritium by tag.
* New recipes: the four machines above (crafted), the assays, both buffers, gypsum, halite (from brine or concentrated
  brine), lithium carbonate, and desalination.
* New recipes: the Bomb Calorimeter and Heat-Pack Reactor (crafted), calorimetry of coal, sugar, blaze powder and
  hydrogen, ammonium nitrate and both cold packs.
* New recipes: the Fuel Cell, Electroplating Bath, Vanadium Flow Battery and Corrosion-Proof Casing (crafted), nickel
  sulfate from meteoric iron and nitric acid, nickel plating, chlor-alkali, and lithium from lithium carbonate (a second
  route to the Lithium Ingot; the 1.1 route is unchanged).
* Every Chemical Reactor recipe now has an operating window (a conversion of 90% or 95%, with its own activation
  energy from 25 to 90 kJ/mol); the easier half still runs at the reactor's default settings.
* New recipes: the Cryogenic Lander, Oxygen Rebreather (and its refill), the four Heated Pressure Suit pieces and the Lake
  Pump (crafted); tholin from tholin sand and methanol, amino acids from tholin and water (Chemical Reactor); oxygen from
  water-ice bedrock (Electrolytic Cell); the Hydrophobic Coating (Chemical Reactor); liquid methane and cryolava (Lake
  Pump); methane and nitrogen from liquid methane, ammonia and water from cryolava (Fractionation Spire); ethylene from
  organic evaporite and hydrogen over a Nickel Catalyst (Chemical Reactor).
* New recipes: Kekule's Dream (crafted); tholin and hydrogen cyanide from methane and nitrogen, and hydrogen from water
  (Photochemical Reactor); the Photochemical Reactor (crafted).
* New recipes: organic evaporite from liquid methane (Brine Evaporator) and from evaporite crust and methanol, cryolava
  from cryolava rock (Chemical Reactor), Evaporite Crust and Water-Ice Bedrock (crafted), and Aramid Fibre from organic
  evaporite and ammonia with the Kekule Ring as catalyst (Polymeriser; a second route, the 1.2 route is unchanged).
* New recipes: the Asymmetric Reactor, Oscillator Vessel and Alkahest Still (crafted); the Chiral Ligand (Synthesis
  Bench); Enantiopure Reagent (Asymmetric Reactor, or Chromatography Column); Chemical Clocks (Oscillator Vessel); the
  three tinctures and Alkahest, and iron, gold and copper from their raw ores with Alkahest (Alkahest Still; new
  routes, the vanilla ones are unchanged).
* Zeolite and Muonic Catalysts are now held as a catalyst (one is needed, and it wears) instead of being used up in:
  * Engineered Enzyme (bioculture): Zeolite Catalyst ×6 used up before
  * Nutrient Broth (bioculture): Zeolite Catalyst ×6 used up before
  * Graphene Sheet (chemical synthesis): Zeolite Catalyst ×4 used up before
  * Molecular Orbital Lattice (chemical synthesis): Zeolite Catalyst ×4 used up before
  * Autocatalytic Broth (chemical synthesis): Zeolite Catalyst ×4 used up before
  * Quark Gluon Plasma (collider): Zeolite Catalyst ×12 used up before
  * Strange Matter (collider): Zeolite Catalyst ×24 used up before
  * Antiiron Ingot (mirror synthesis): Muonic Catalyst ×12 used up before
  * Flerovium Ingot (nuclide synthesis): Muonic Catalyst ×12 used up before

## 1.1.0 — The Expansion (3 November 2026)

Seven new branches of science, two new dimensions and nearly fifty new machines. Everything 1.0 had is still there.

**In numbers** (1.0 → 1.1.0): 78 → 127 machines (37 → 70 multiblocks) · 148 → 232 research entries · 10 → 12
dimensions · 48 → 69 fluids · 25 → 35 creatures · about 840 → 1,260 items and blocks · 11 → 13 music tracks.

**New sciences**, each from Tier I to Tier III, with research, derivation puzzles and Codex chapters:
* **Astrophysics:** telescopes that need the real sky, a radio telescope, the spectrograph and the burst observatory.
* **Earth & Mapping:** surveying, seismography, deep drilling and a climate engine that sets the weather.
* **Nuclear Science:** radioactive decay that follows real half-lives and shielding, fission reactors, isotope
  enrichment, a cyclotron and a tokamak.
* **Electromagnetism:** magnets, induction generators, a Tesla coil, an MHD generator, a pulsed magnet lab and a railgun
  forge.
* **Probability & Statistics:** Galton boards, Bayesian inference, a Monte Carlo engine and a Markov chain refinery.
* **General Relativity:** gravitational-wave interferometers, a redshift tower, lensing and time dilation, and a warp
  field forge.
* **Optics & Crystals:** grow, study and cut crystals into lenses for the instruments.
* **Unification:** experiments where quantum mechanics meets gravity: neutron interferometry, analogue black holes,
  the Unruh effect, Hawking evaporation and ER=EPR.

**New dimensions**
* **The Planetary Interior:** crust, mantle and core layered by depth, with lithostatic pressure, magma, huge crystal
  pockets, its own creatures and machines. Enter through a Core Drill or a Mantle Breach.
* **The Accretion Frontier:** islands round a black hole you can see in the sky. Five regions (a cloud nebula you can
  walk through, an asteroid belt, a pulsar field, the accretion rim and a lensing void), each with its own hazard
  and its own counter. Reached by Light Sail, with a mini-boss, The Binary, summoned by a gravitational-wave record.

**Also new**
* Six Overworld field sites and the Frontier's derelict stations, built only from the mod's own blocks.
* Counter-gear: Refractory and Pressure Suits, the Whipple Shield, the Frontier Suit, Radiation Plating (on any armour)
  and off-hand counters for the new effects.
* Ten new creatures, the rare Black Swan among them, each with its own AI, voice and way of dying.
* Each new dimension has its own music and its own travel animation. The machines have their own animations in the world
  and in their operator consoles, and every block sounds like what it is made of.

**Changes to 1.0 content**
* **Twenty late and endgame multiblocks have new, larger shapes.** An existing structure stops working until it is
  rebuilt. The Structure Assembler's new disassemble mode takes any of them apart, old layout or new, and gives every block back, so nothing is lost.
* **Recipes were rebalanced, 1.0's included.** Every route is checked against the others for the same material:
  the newer or the one on a costlier machine must be substantially cheaper per item, or it was retuned (1.0 routes) or removed (newer ones). Every 1.0 recipe that changed is listed at
  the end of this entry.
* **Fixed:** fluid tanks in JEI and EMI drew past their frames, crowded Codex maps lagged, a mod's Essence could
  change its picture on every launch, and the mod's creatures behaved like the vanilla mobs they were built on (chasing
  villagers, riding, joining raids).
* **Fixed (pack-wide):** the vanilla armour tags (head, chest, legs and feet) still listed the removed Lead-Lined Suit, so
  the game threw those tags away for every mod in the pack, and armour enchanting could break with them.
* **Fixed:** villages could get two different Probability Halls, at twice the intended chance. There is one hall now.
* **Faster:** rewritten dimension laws no longer check every creature in the dimension every second, and the links to
  Mekanism, GregTech and Nuclear Radiation look their classes up once instead of every second per machine.
* **Interface Ports can import and export:** sneak-use one with an empty hand to cycle passive, export (pushes the
  machine's products and spent coolant into the inventories and tanks beside it), import (pulls only what the machine's
  recipes can use) or both.
* **The Structure Assembler can draw on an ME network (AE2):** sneak-use it on any block of the network to bind it (again
  on the same block to unbind). Building then takes what your inventory lacks from the network, and taking a structure
  apart puts the blocks back into it - from anywhere in the same dimension while that block's chunk is loaded and the
  network has power. Interface Ports still come only from your inventory.
* **Fused Silica:** the Solar Furnace is now the bulk route it was meant to be (16 Sand into 16 Fused Silica, or 4 Quartz
  into 4, per run). The recipe balance now counts a machine that only works part of the day (the Solar Furnace by day,
  the Reflecting Telescope by night) as that much slower and dearer, so routes on it have to pay for that; a few
  daylight and downstream routes changed with it (the Tesla Coil's fulgurites give 6 Fused Silica from 16 Sand).
* **Where things are found:** every material that is mined, dropped by a creature or found in structure caches now says
  where, under its tooltip description and on its Codex page (for example, Corundum: mined from Corundum Crystal Mass in
  the Planetary Interior's Crustal Caverns).
* **Tags for every item** (shown by `/kjs hand`, usable in KubeJS and other mods): each item now carries the tag of the
  science it belongs to (`#theoreticalsciences:field/optics`, `.../nuclear`, ...) and of its family (`casings`,
  `machines`, `multiblock_controllers`, `processors`, `singularities`, `containment_cells`, `lenses`, `crystals`, `shards`,
  `boules`, `ingots`, `ores`, `records`, `samples`, `research_notes`, `spawn_eggs`, `armour/<suit>`, `glass`,
  `decorative_blocks`). Conventional `c:` tags are added only where they are safe: glass blocks, spawn eggs, and the real
  everyday materials (lithium, hafnium, magnetite) - the exotic materials share names with other mods' items, and a
  shared tag would let unification swap them.
* KubeJS no longer warns about the Interstellar Scoop's height requirement in its recipes.
* **Fixed: the Omnipotence Core could make what lies beyond Omnipotence.** A later step had overwritten the
  `theoreticalsciences:beyond_omnipotence` tag with a single item, so the Omnipotent Ingot, Apeiron, the Raiment of
  Transcendence and the rest were missing from it (and showed no such tag in `/kjs hand`). All 38 are back, and tags
  written in several steps are now always added to, never replaced.
* **Removed: the creative-duplicator watch** ("Beyond Copying"), with its two config options
  (`creativeDuplicatorBlocks`, `creativeDuplicatorHosts`), the `creative_duplicator_parts` item tag and the fluid
  `beyond_omnipotence` tag. It did not hold in practice; what lies beyond Omnipotence stays scarce because the Core will
  not make it.
* **Rewritten text:** about 600 Codex pages and a few tooltips rewritten by the author. Recipe lines now only say where a
  thing is made ("Made in the Femtoscale Assembly Line") - the recipe pages and JEI/EMI show what goes in. Every ore
  describes where it is found, at what height, what it gives and which pickaxe it needs. About 175 blocks, items,
  fluids and creatures that had only a placeholder or their bare name now have a real description.
* Mod Essence parts and Specimen Bundles (samples and archives) are lettered like the Singularity Clusters (Greek, then
  Hebrew), instead of numbered.

### Development notes

The detailed notes below were written as the update was built, in order. Later parts sometimes change what an earlier
part added; where they disagree, the later part is how 1.1.0 ships.

#### Foundations of five new sciences

Five new sciences open at Tier I.

* **Astrophysics:** Observational Astronomy and Spectroscopy. Telescope Mirror Blanks, Spectral Plates and the
  Spectrograph, a new machine that also separates radium from its ore.
* **Earth & Mapping:** Cartography, Geodesy and Geomorphology. Survey Maps, Theodolite Lenses, Geoid Models, Sediment
  Cores and the Geodetic Surveyor, which lists the ores around you and marks the nearest of each.
* **Nuclear Science:** Radioactivity and Nuclear Binding Energy. Pitchblende ore (stone and deepslate), Radium Salt,
  the Geiger Counter, and Radiation Sickness from carrying radioactive things, cured by Prussian Blue Tablets. A config
  option turns it off; with Mekanism, GregTech or NuclearCraft (Nuclear Radiation) installed, their radiation sees this
  mod's radioactive items too.
* **Electromagnetism:** Magnetostatics, Electromagnetic Induction and Ferromagnetism. Magnetite, Ferrite Magnets,
  Electrical Steel Laminations and the Gauss Meter.
* **Probability:** Probability Theory and Combinatorics. The Dice Set and the Quantum Random Number Chip.
* Twelve new research entries with derivation puzzles, five new Codex categories, and new effects for the coming
  releases (Lithostatic Pressure, Seismic Vertigo, Magnetized, Frame Drag, Information Loss, Variance).
* New tags for packs and other mods: `theoreticalsciences:ferromagnetic`, `radioactive`, `strongly_radioactive`
  (items) and `radioactive`, `magnetic` (blocks).
* **Fixed:** Schrödinger's Limit and Relativity Breakdown could be poured out of a cell and then never removed. Their
  cells can no longer be poured into the world (by hand, dispenser or another mod's fluid placer); outside the Quantum
  Foam an Empty Containment Cell takes them for good, and any already placed elsewhere dissolve on their own.
* **Fixed:** a mod's Essence could look different on every launch, and sometimes be invisible. It now always shows the
  same picture of its mod (tabs and items are tried in name order), never a mostly see-through one, and falls back to
  the plain Essence in the mod's colour when no picture fits.
* **Fixed:** the mod's creatures no longer think like the vanilla mobs they were built on. The Paradox Echo and
  Destabilised Husk no longer chase villagers or turn them into zombie villagers, and no longer spawn as babies or
  call reinforcements. The Moduli Crawler and Planck Mite no longer hunt iron golems or spawn with riders. The Void
  Wraith no longer takes sides in raids. Iron and snow golems leave the mod's creatures alone.

#### JEI and EMI tank fix

- **Fixed:** in JEI and EMI, the fluid in a single-block machine's tall tank could be drawn past the tank's frame. The mod now draws these tanks itself: the fluid is built from 16-pixel tiles cut to the tank's inside, and anything outside the frame is clipped.

#### EMI tank fix

- **Fixed:** in EMI, a single-block machine's tall tank had a frame only one slot high at its top, so the fluid looked as if it ran out of it. The frame now covers the whole tank.
- **Fixed:** in JEI, the Vacuum Fluctuation Harvester's long note covered the Research line; the lines below the note now move down to make room.

#### Faster Codex maps

- **Faster Codex maps:** the map's frames, lines and overlays are now drawn in a few batches instead of one at a time. Crowded maps such as Discovery, where nearly every entry is on screen at once, no longer lag.

#### Tier II: instruments, and radiation that follows the physics

- **Radiation, the real way.**
  - Every source is an isotope with its real half-life and kind of radiation: alpha, beta, gamma or neutron.
  - Dose falls with the square of the distance.
  - Gamma rays and neutrons are weakened by e^(−μx) through every block in the way. Lead is the best shield for gamma, water for neutrons.
  - Each dimension has its own natural background level.
  - Packs can set what any item or block gives off, and what any block stops, with the data maps `theoreticalsciences:radiation_source` and `theoreticalsciences:radiation_shielding`.
  - Dose is now counted in millisieverts. The Geiger Counter shows µSv/h and real counts per second, and like a real tube it saturates.
- **Seven new machines, each with its own physics:**
  - **Seismograph:** finds how deep ore and caves lie from the gap between pressure and shear waves.
  - **Decay Chamber:** draws radon from radium. The radon decays on a real (sped-up) clock into lead, and the chamber gives off gamma rays while it works.
  - **Magnetizer:** its field must beat a magnet's coercivity. Iron blocks touching it act as a core and strengthen the field.
  - **Induction Generator:** stronger magnets give more power, and flowing water beside it spins it faster.
  - **Galton Board:** sorts raw ore into one, two or three ingots by where each piece lands, following the exact binomial odds.
  - **Bayesian Analyzer:** turns a Prior Tablet and a piece of evidence into Posterior Tablets, which pay for research hints instead of Insight.
  - **Atomic Clock:** runs slower the deeper it stands. Carried in the off hand, it clears Time Dilation.
- **New materials:** Lead Shielding (plate and block), Neodymium and Samarium-Cobalt magnets, Monazite Sand, Rare Earth Oxide, Radiogenic Lead, Proof Mass and more, plus Radon-222 gas.
- **Research:** 7 new research nodes with derivations: Seismology, Radiochemistry, Decay Chains, Distributions, Bayesian Inference, the Equivalence Principle and Gravitational Time Dilation. 8 new Codex chapters.
- **Recipes:** no 1.0 recipe changes, except a few post-Shutdown (Pleroma) recipes.
- **Mekanism link:** Mekanism now receives exactly the same dose as the mod counts.

#### Detectors and the Decay Chamber

- **Other mods' detectors see the Decay Chamber.**
  - While it gives off radiation, the chamber reports itself as a world source to Mekanism and Nuclear Radiation, so their Geiger counters read it and their own exposure applies.
  - For Mekanism, the source is kept at the matching strength. For Nuclear Radiation, it is caesium-137 of the same activity.
  - The source is removed when the chamber stops or is broken.
  - Mekanism now applies the chamber's dose itself, so the mod no longer adds the same dose a second time.
- **Jade:** the progress bar is drawn in a deep shade of the status colour, so its white text is readable.

#### Radiation debug command

- **New command:** `/theoreticalsciences radiation debug` shows, for the player running it:
  - what the mod reads where they stand (carried, around, from machines, background) and their current dose;
  - for each link to Mekanism and Nuclear Radiation, how often it has fired and what it last did;
  - Mekanism's own reading at the player and the dose Mekanism holds for them.
- **Log:** each link writes one line to the log the first time it works.

#### Nuclear Radiation fix

- **Fixed: Nuclear Radiation and the Decay Chamber.**
  - Nuclear Radiation takes over Mekanism's world radiation: it cancels each Mekanism radiation source and creates a lingering contamination source of its own instead.
  - Because of that, in two development builds a running Decay Chamber left a new piece of contamination behind every second.
  - That contamination also sat on the same block as the chamber, which hid the chamber's proper source from Nuclear Radiation's counters.
  - With Nuclear Radiation installed, the chamber now reports only to Nuclear Radiation, as one source that it keeps track of. Without Nuclear Radiation, it reports to Mekanism as before.
- **New command:** `/theoreticalsciences radiation cleanup [radius]` removes Nuclear Radiation's lingering contamination within the radius around you (8 blocks if no radius is given). Use it at chambers that ran under those builds.

#### Nuclear Radiation fix

- **Fixed:** Nuclear Radiation's counters now read a running Decay Chamber.
  - Nuclear Radiation's container scanner only recognises vanilla-style inventories, so it removed the chamber's source again almost as soon as it was added.
  - The chamber now reports itself as a free-standing source that does not spread contamination. It is kept topped up while the chamber works and removed when the chamber stops.

#### Nuclear Radiation fix

- **Fixed:** Nuclear Radiation now registers the Decay Chamber again.
  - Nuclear Radiation silently refuses any world source weaker than its `world_source_min_bq` setting (5×10⁹ Bq by default). The caesium-only source used in earlier development builds was weaker than that.
  - The chamber now reports with the same isotope mix Nuclear Radiation builds for a Mekanism radiation source of the same strength. That is how the chamber first showed up in its counters.
  - The chamber now keeps a single source and refreshes it, instead of adding a new one every second.
- **Debug:** `/theoreticalsciences radiation debug` now says when Nuclear Radiation refused a source.
- **Mekanism:** the mod no longer reports world radiation to Mekanism when Nuclear Radiation is installed, because Nuclear Radiation handles it.

#### Field Sites

- **Six Overworld structures, the mod's first:**
  - **Survey Benchmark:** a trig pillar with a barrel of survey gear. Found anywhere.
  - **Abandoned Observatory:** a copper-domed tower in the mountains, with mirror blanks and spectral plates.
  - **Crashed Satellite:** wreckage in a scorched crater on plains, savannas, deserts and badlands.
  - **Magnetic Anomaly Site:** rare. A ring of lodestones round a knot of raw iron.
  - **Fallout Shelter:** sealed, lead-lined rooms deep underground with no surface entrance. A Seismograph is the way to find one.
  - **Probability Hall:** in some villages. A chequered hall with a Galton Board to study, plus dice and tablets.
- Each structure has its own loot table under `theoreticalsciences:chests/...`.
- Packs can change where a structure appears through the biome tags `theoreticalsciences:has_structure/<name>`.
- **Codex:** a new chapter, Field Sites.

#### Field Sites, rebuilt

- **The six sites now use only this mod's own blocks**, as the 1.0 ruins do, and keep their loot in Containment Caches like those ruins.
- **Loot is now this mod's own materials:** each site's science items, plus Cherenkov quartz, quantum dots and neutronium.
- **New decoration blocks:**
  - Block of Magnetite, which the Gauss Meter reads as a magnet;
  - Magnetite Bricks;
  - Lead Bricks and Lead Hazard Panel, which shield like lead;
  - Radium Lamp;
  - Chance Tiles;
  - the Decorative Telescope and Satellite Wreck, two decorative instruments with their own models. Working astronomy machines come in a later release.
- **Recipes:** a few endgame recipes (Pleroma, concept compression, transcendent forging, the tesseract core) have had their extra ingredients reshuffled during this update. Everything is still obtainable.

#### Compasses and magnets

- **Compasses feel magnets.**
  - Within 8 blocks of a magnet (anything tagged `theoreticalsciences:magnetic`: magnetite, magnetite bricks, lodestones), a compass you hold swings towards it and shivers, so a Magnetic Anomaly Site throws compasses off.
  - While Magnetized, a compass spins.
  - Away from magnets, and for lodestone-bound compasses, nothing changes.

#### The Planetary Interior

- **A new dimension inside the planet, layered by depth:**
  - **Crustal Caverns:** pools of Deep Brine, and occasional tremors.
  - **Natural Reactor:** patches in the crust. Oklo rock with uraninite and fission-fragment seams, and a high radiation background.
  - **Magma Chambers:** columnar basalt over a sea of Basaltic Magma.
  - **Mantle Convection Zone:** peridotite with Ringwoodite and Bridgmanite. Mantle plumes throw you upwards.
  - **Geodynamo Core:** iron-nickel with magnetite. Geomagnetic storms Magnetize you.
- **Lithostatic Pressure:** the deeper you are, the stronger it gets.
- **The Core Drill** is the first tool that cuts bedrock.
  - Hold it against the Overworld's bedrock floor and a Mantle Breach opens. Step in to enter.
  - A breach always opens beside you when you arrive, so you can always get back. In the Interior, the drill cuts new breaches through its bedrock.
- **Research:** Plate Tectonics (unlocks the Core Drill) and Mantle Geodynamics, each with its derivation.
- **Codex:** a new chapter on the Planetary Interior.
- **Radiation:** background radiation now depends on the biome where the dimension calls for it.

#### The Interior's floor

- **The Planetary Interior's floor is now the Inner Core Boundary:** solid iron under 330 GPa, a block nothing can break yet. The Core Drill still cuts the Interior's bedrock roof. A way through the floor comes in a later release.

#### Mantle Breach fix

- **Fixed:** entering a Mantle Breach could put you on top of the Planetary Interior's bedrock roof.
  - The shared landing search prefers open sky, and the only open sky there is above the roof.
  - The Interior now has its own search: under the roof, on dry solid rock, as near y 100 as possible within 24 blocks.
  - If nothing suitable is found, a small chamber is cut at y 100.

#### Deep Riches

- **Crystal pockets:** far larger than vanilla geodes. A pale shell surrounds masses of Corundum or Garnet (in the crust), or Peridot or the rarer Moissanite (in the mantle). A Crystal Mass breaks into 2-4 gems; Fortune gives more, and Silk Touch keeps the mass whole.
- **Rich veins:**
  - Rich Iron and Rich Copper in the crust, Rich Gold in the basalt.
  - The chunks smelt to two ingots.
  - The Spectrograph's assay gives three ingots plus a by-product: magnetite with iron, gold with copper, uraninite with gold.
- **Moissanite Drill:** a pick of its own tier, above netherite. Iron-Nickel and Core Magnetite Ore now drop nothing to any vanilla pick; only this drill mines them.
- **Basaltic Magma** deals 10 damage a hit, two and a half times lava's. Fire Resistance does not help. A full set of armour in the item tag `theoreticalsciences:magma_proof` will protect; that armour comes in a later release.
- **Deep Brine** now sets you on fire, as lava does.
- **Sounds:** the Interior's rocks and ores use vanilla stone sounds (columnar basalt uses basalt's) for breaking, placing and walking. Crystal masses sound like amethyst.
- **Status effect icons:** the effects added in 1.1 have icons now (Lithostatic Pressure, Seismic Vertigo, Magnetized, Frame Drag, Information Loss, Variance).
- **Textures:** Iron-Nickel and the Inner Core Boundary now look like rock (dark stone with metal grains, and near-black stone with golden seams), not plain metal blocks.

#### Interior ore fixes

- **Core Magnetite Ore is now easy to tell apart from Iron-Nickel.** It shows large blue-black octahedral crystals with bright blue edges, visible from a distance. Iron-Nickel is now a warm, rust-brown stony iron with silvery grains, so the two no longer blend together.
- **Geode Shell retextured:** a pale, rough calcite crust with agate banding, instead of something that looked like a metal block.
- **Planetary Interior ore veins are much larger and more widespread:**
  - Each vein is a thick, long lode (up to 3 blocks across) that forks into two or three side seams.
  - Veins are more frequent and span a far wider range of heights.
  - Applies to Ringwoodite, Bridgmanite, Core Magnetite, Oklo Uraninite, Fission Fragments and the Rich Iron, Copper and Gold ores.
  - Only newly generated Interior chunks get the new veins; chunks you have already explored keep their old ones.

#### Interior Machines

Five new multiblocks, each with its own shape and its own physics model. Older recipes are unchanged.

- **Tectonic Press** (Geography, research *Metamorphic Petrology*). A firebox, a multi-anvil pressure vessel, a hydraulic gantry with a ram, and a chimney.
  - You set the anvil pressure and the furnace temperature. Where that point sits on the real pressure–temperature phase diagram decides what forms:
    - Metamorphic Schist (with the odd Garnet) at a few kilobars.
    - Diamond above the graphite–diamond line. This needs a block of Iron-Nickel in the press as the solvent catalyst.
    - Ringwoodite above about 17.5 GPa.
    - Bridgmanite above the post-spinel line, near 23 GPa.
    - Nuclear graphite, and Boron Control Rods, at low pressure and very high heat.
  - The reaction rate rises with temperature (Arrhenius).
- **Deep Drilling Rig** (Geography, research *Deep Drilling*). An open derrick with a drill string hanging into the ground.
  - You set the depth, the weight on the bit, the rotary speed and the mud density.
  - The mud must press harder than the water in the rock, or the well kicks. It must not press so hard that it fractures the rock.
  - What comes up depends on depth:
    - sediment cores and Deep Brine near the top;
    - Basaltic Magma from hot rock;
    - crustal rock with rich ore chunks;
    - Peridotite from the mantle.
  - These Interior materials can now be produced in the Overworld.
- **Gas Centrifuge Cascade** (Nuclear, research *Isotope Separation*). The first multiblock whose height you choose.
  - Build the stage layer (four rotors behind windows) once, or stack it up to 14 high. Each layer is one more enrichment stage.
  - The Structure Assembler adds one stage each time you use it on a finished tower.
  - Separation follows α = e^(Δm·v²/2RT). Spin the rotors faster for more separation, but:
    - the carbon-fibre rotors burst above 1,500 MPa;
    - there is a flexural resonance near 450 m/s.
  - Enrichment is interlocked at 20%: reactor fuel only.
  - The cascade also:
    - turns Yellowcake and Fluorspar into Uranium Hexafluoride;
    - makes Molten Salt Fuel (from enriched uranium or plutonium);
    - takes Spent Fuel Salt apart into hexafluoride, Plutonium-239 and Fission Fragment Mix.
- **Molten Salt Reactor** (Nuclear, generator, research *Nuclear Fission*). Burns Molten Salt Fuel and leaves Spent Fuel Salt.
  - Set the fuel concentration and the control rods to bring k = ηfpε to 1. The salt then settles at the temperature where k is exactly 1.
  - Keep it between 732 K (the salt freezes) and 980 K (the vessel creeps).
- **MHD Generator** (Electromagnetism, generator, research *Magnetohydrodynamics*). A long duct through magnets, with electrodes on its walls.
  - Power per volume is σu²B²k(1 − k).
  - Fuels: Deep Brine, Basaltic Magma (which leaves basalt), or Stellar Plasma (far more power).
  - Too much current density arcs the electrodes away.
- **New materials:**
  - Yellowcake (smelt Raw Pitchblende).
  - Fluorspar (Spectrograph, from calcite or geode shells).
  - Uranium-235, Uranium-238 and Plutonium-239 Pellets.
  - Graphite Moderator, Boron Control Rods and Metamorphic Schist.
  - Fluids: Uranium Hexafluoride, Molten Salt Fuel and Spent Fuel Salt.
  - 15 new structure blocks.
  - The pellets and yellowcake are radioactive, using real isotopes; Plutonium-239 has been added to the radiation model.
- **Five new research nodes**, each with its own derivation puzzle (Clapeyron slope, geothermal gradient, centrifuge separation factor, four-factor formula, Faraday generator), and five Codex entries.

#### Bigger Endgame

- **Late and endgame multiblocks rebuilt, larger.** Twenty of them have new shapes, drawn from GregTech: New Horizons' and MeatballCraft's endgame structures. They are 17 to 45 blocks across: rings, spokes, lattices, armillary spheres, a tilted ring, a world tree, a ziggurat, linked rings, a mirrored pair, and a great ring with an injector barrel. Each one uses at least as many of every block as its old form did, so an old build's blocks all go into the new one.
  - **Rebuilt:** Abiogenesis Crucible, Annihilation Reactor, Anomaly Containment Ward, Anomaly Resolution Engine, Causality Loop Engine, Entropy Horizon, Essence Distillery, Inflation Chamber, Mirror Foundry, Universe Forge, Vacuum Fluctuation Harvester, Matrioshka Brain, Dyson Array, Reality Compiler, Axiomatic Breach Engine, Conceptual Singularity Compressor, Eye of the Event Horizon, Omnilex Engine, Pleroma Reconstitutor and Transcendent Plasma Forge.
  - **Existing builds of these machines stop working until rebuilt.** Take them apart with the Structure Assembler's new disassemble mode. It recognises the old layouts too. Then build the new form with the Assembler.
- **Structure Assembler: disassemble mode.**
  - **Switch modes:** sneak-use it in the air to switch between building and disassembling. It glints while disassembling.
  - **Take apart:** use it on a controller, then again within 5 seconds to confirm. Every block of the structure's pattern, current or older, comes back to your inventory, controller last, and anything that doesn't fit drops at your feet.
  - **Safety:** blocks outside the pattern are never touched, nor are protected or unbreakable ones.
  - **Preview:** sneak-use on a controller while disassembling lists what you would get back.
- **New multiblock: the Eschaton Foundry** (endgame, after the Rekindling).
  - **What it makes:** the Absolute Singularity, the Echo of the Transcendental, Eschaton Cells and the Eschaton Core, in bulk.
  - **Cost:** far more than the Forge.
  - **Unlock:** it only makes what its owner has forged by hand in the Core's Forge at least once. Until then it shows "Forge it once by hand first". Forgings made before this version are not counted, so forge each item once more.
  - **Physics:** Bekenstein's information bound, against collapsing into a black hole.
- **The Core's Forge now waits 3 minutes after every forging**, of any recipe. Before, only the six drifting recipes waited.
- **Pleroma Reconstitutor:** its Absolute Singularity, Echo replay and Eschaton Cell recipes moved to the Eschaton Foundry, at the same cost. It keeps Omnipotent Ingots, Crystals of Transcendence and Omnipotence Shards.
- **Recipe change:** the Logos Thread now also takes 1 Omnium Singularity and 32 Omnilex Seals. This is a side effect of the recipes moving out of the Reconstitutor.
- **Codex:**
  - The Dimensions tab now includes the Planetary Interior: its chapter, Deep Riches, and a new "Surviving the Interior" entry.
  - "Falling Through" and "Arriving Safely" now cover the Interior.
- **Tooltips corrected.** They had fallen out of date:
  - **Singularity counts:** all 29 singularities now state the real count from their recipe. They had been showing a quarter of it.
  - **Blocks:** Omnipotence Casing (it can be mined now), the storage blocks (40 ingots, not 9), Core Magnetite Ore (needs the Moissanite Drill), and the Cosmogenic, Transfinite and Omnilex casings (their real ingredients).
  - **Catalysts and items:** Higgs and Spacetime Catalysts, Transcendental Crystal, Omniscience Catalyst, Pleroma Confluence, Convergence Engine, and about a dozen more.

#### Life Below

- **Four creatures in the Planetary Interior.** Each follows its own AI (none of vanilla's) and acts out a piece of the planet's physics.
  - **Magma Crawler** (Magma Chambers): a basalt centipede that swims through Basaltic Magma as you would through water. Its bite sets you alight. Drops Basalt Column, and rarely a Rich Gold Chunk.
  - **Plume Elemental** (Mantle Convection Zone): convects. It heats near the bottom, rises while it is hotter than its surroundings, cools and sinks. It throws whoever stands over or beside it into the air. Drops Ringwoodite and Peridotite.
  - **Fission Husk** (Natural Reactor): raises your radiation dose while you are near it, and more with its bite.
    - Killed, it splits into two lighter husks in a burst of neutrons; each of those splits once more.
    - Drops Fission Fragment Mix.
  - **Geodynamo Wraith** (Geodynamo Core): circles its prey along a field line.
    - Ferromagnetic armour near it becomes Magnetized and is pulled in, and it can tear a ferromagnetic item out of your hand.
    - Drops Magnetite, and sometimes a Ferrite Magnet.
- **Three suits of armour**, crafted like ordinary armour:
  - **Refractory Suit** (corundum, moissanite): a full set is proof against Basaltic Magma, Deep Brine, lava and all fire. Magnetite under the magma can now be reached.
  - **Pressure Suit** (bridgmanite, ringwoodite): each piece takes a level off Lithostatic Pressure; a full set cancels it.
  - **Lead-Lined Suit** (lead plate, leather): each piece stops 22% of the radiation dose you take in.
- **Two structures in the Interior.** Both are built from the mod's own blocks, with Containment Caches of the mod's own loot.
  - **Deep Borehole** (Crustal Caverns): a cased drill shaft with a broken drill floor.
  - **Oklo Reactor Ruin** (Natural Reactor): a lead-walled station round the reactor seam.
- **Structure Assembler:** it now places Interface Ports from your inventory wherever the structure accepts one. It uses the plain casing only once your ports run out; in creative it always uses ports.
- **Dodecaract Excavator:** while charged, it breaks any breakable block instantly, whatever slows you (Mining Fatigue, water, mid-air), and always keeps the drops.
- **Omnipotence Casing:** no vanilla tool can break it. Only tools in the item tag `theoreticalsciences:omnipotent_tools` can (by default the Dodecaract Excavator and the Moissanite Drill). The blocks this applies to are set by the block tag `theoreticalsciences:needs_omnipotent_tool`.

#### Fixes from Below

- **Radiation plating replaces the Lead-Lined Suit**, the way NuclearCraft does it. Put any armour piece (from any mod) and a plate together in a crafting grid:
  - **Light Radiation Plating** (8 Lead Shielding Plate around an iron ingot): the piece stops 10% of the radiation dose.
  - **Medium** (Light plating + 4 Graphite Moderator): 18%. **Heavy** (Medium plating + 4 Boron Control Rod): 25%.
  - Four heavy-plated pieces stop all but 5% of the dose. A better plate replaces a worse one; the tooltip shows which plate a piece carries.
  - The Lead-Lined Suit and its recipes are gone.
- **Fission Fragment Mix has uses:**
  - Spectrograph: 2 Fission Fragment Mix + Spectral Plate → 3 Rare Earth Oxide (fission fragments are about a third rare earths).
  - Gas Centrifuge Cascade: 8 Fission Fragment Mix + 2 Fluorspar → 250 mB Uranium Hexafluoride + Rare Earth Oxide (recovers the unburnt uranium).
- **Fireproof drops.** Everything mined in the Interior (Core Magnetite, Iron-Nickel, Ringwoodite, Bridgmanite, Peridotite, the rich ores and gems, and more) no longer burns up in Basaltic Magma, lava or fire.
- **Creatures:**
  - New sounds for all four: the Magma Crawler clicks and scratches like an insect, the Plume Elemental roars and hisses like a flame, the Fission Husk groans over Geiger ticks, the Geodynamo Wraith hums and crackles.
  - Magma Crawler: stays in the magma and only leaves it to chase prey that is close; returns to the magma afterwards. Its gait now follows how far it has actually moved, like vanilla legs: its body waves and its legs step while it travels, and it lies still when it does not.
  - Fission Husk: drops Fission Fragment Mix and sometimes Raw Pitchblende instead of rotten flesh. Its model was facing backwards and is fixed.
- **Working animations stay in view.**
  - They are now drawn before glass, not after it. Glass used to hide them: the Eschaton Foundry's core sits in a sphere of Pleroma Glass, so it never showed at all. Every window now shows the effect behind it.
  - Where a machine's new shape had buried its effect inside solid blocks, the effect moves up the machine's axis to the nearest open spot. Moved: the Omnilex Engine (into its glass halo), Matrioshka Brain, Reality Compiler, Entropy Horizon, Anomaly Resolution Engine and Containment Ward, and a few older machines with sealed cores (Molten Salt Reactor, Tectonic Press, Stellar Plasma Crucible, Fractionation Spire).
  - Big machines draw their effect bigger, as far as the open space around it allows (up to three times).
- **Colours:** the Refractory Suit is now sapphire blue with moissanite flecks, the Pressure Suit bridgmanite brown with ringwoodite blue, matching what they are made from.
- **Codex:** Inner Core Boundary, Mantle Breach, Antisoil, Oklo Rock, Lead Bricks, Lead Hazard Panel, Magnetite Bricks and the Ex Nihilo Engine had pages with only their name; they now explain what they are. Blocks' own descriptions now feed their Codex pages, and the build checks that no page is empty.

#### Under the Stars

Astrophysics reaches Tier II: three instruments that need the real sky above them.

- **Machines now see the world around them.** Physics models can read whether there is open sky above the machine, day and night (from the Sun's height), the Moon's phase, rain, the height the machine stands at, and how many other machines are running nearby (`sky`, `night`, `sun`, `moon`, `rain`, `alt`, `rfi` for pack makers).
- **Reflecting Telescope** (multiblock: a drum and a dome with an open slit):
  - Works only at night, with open sky above the slit and no rain. Every plate it takes gives **Insight**.
  - The aperture and the exposure set the faintest star it records (the limiting magnitude). A bright Moon washes out the faintest stars.
  - Seeing (the air's blur) is better the higher you build. Very short exposures freeze it for sharp planetary plates ("lucky imaging").
  - Makes Star Field, Stellar Spectrum, Planetary and Deep Sky Plates from Photographic Plates.
- **Solar Furnace** (multiblock, after Odeillo: heliostat terraces, a parabolic mirror wall and a target on a tower):
  - Works only by day, under open sky, without rain.
  - The target's temperature follows radiative balance, T = (αCG/εσ)^(1/4). Hold it between the melting point and where the charge boils off; above 3,200 K the crucible fails.
  - Melts quartz or sand into **Fused Silica**, and corundum (or, more slowly, clay) into **Sapphire Windows**.
- **Radio Telescope** (multiblock: a dish on an alt-azimuth mount with a feed horn at its focus):
  - Works day and night, rain or shine, but needs a view of the sky. Every other machine running within 24 blocks adds noise to its receiver.
  - Signal to noise follows the radiometer equation, SNR = T·√(Bτ)/T_sys. A narrow hydrogen line is diluted in too wide a band, and a pulsar's pulse is smeared by dispersion across one.
  - Makes **Hydrogen Line Maps** and **Pulsar Timing Records**, each giving Insight. After Galactic Dynamics, a long galactic survey gives much more.
- **Meteorite craters** in open Overworld country (plains, deserts, savannas, badlands, taiga, snowy plains, meadows). Each is a bowl of Impact Breccia with an Iron Meteorite or a Chondrite half-buried in the middle.
  - Meteoric Iron smelts to iron. The Spectrograph's assay gives more, plus a block of Iron-Nickel.
  - Chondrites hold **Presolar Grains**, silicon carbide stardust. Eight make a Moissanite crystal, as Moissan first found the mineral in a meteorite.
- **Research:** Telescope Optics, Meteoritics, Stellar Astrophysics, Radio Astronomy and Galactic Dynamics, each with its derivation. **Codex:** five new pages in Astrophysics.
- **New working animations:**
  - The telescope: starlight falling through the slit.
  - The Solar Furnace: beams converging on a white-hot target.
  - The radio dish: wavefronts gathered into the feed.
- **Recipes:** no earlier recipe changes.

#### Connections

The newer sciences now feed each other, through the links real science has between them. No recipe from 1.0 changes.

- **Star Field Plates:**
  - In the Bayesian Analyzer, two Star Field Plates (or one Stellar Spectrum Plate) are evidence for Posterior Tablets.
  - A Star Field Plate and two Survey Maps make two Geoid Models. The geoid was first mapped by comparing star positions with the plumb line.
  - Stellar Astrophysics now also asks for 4 Star Field Plates.
- **Metamorphic Schist** (from the Tectonic Press): the Spectrograph picks the Garnet out of it (4 schist + a Spectral Plate → 2 Garnet, 2 Quartz).
- **Garnet** grit grinds telescope mirrors: glass, iron nuggets and a garnet make 4 Telescope Mirror Blanks instead of 2.
- **Peridot** is an olivine refractory: bricks, peridot and a blast furnace make 2 Refractory Fireboxes, as corundum does.
- **Pallasites:** the meteoric iron assay now gives 2 Iron-Nickel blocks, and one time in four a Peridot (olivine crystals set in the iron). It no longer gives iron ingots; smelt the meteoric iron for those.
- **Time-Dilated Samples** (from the Atomic Clock): the Radio Telescope can time a binary pulsar against one, whose orbit decays exactly as general relativity says. It gives 3 Pulsar Timing Records and Insight. Needs Galactic Dynamics.
- **Hydrogen Line Maps:** the Dark Matter Collector, given two with its usual inputs, gathers twice as much dark matter. Needs Galactic Dynamics.

#### Clear Sight

A new branch, **Optics & Crystals**, joining physics and chemistry. It appears in both the Physics and Chemistry tabs of the Codex. Crystals from the Overworld and the Planetary Interior are studied, grown pure and cut into optics.

- **X-ray Diffractometer** (multiblock: a lead cabinet with leaded-glass doors, a goniometer and a high-voltage generator):
  - Set the copper tube above its K-edge (8.98 kV), then the detector angle so that nλ = 2d·sin θ for the crystal's own lattice spacing.
  - The sample comes back with a **Laue Pattern** (its orientation, needed to cut it), and every study gives Insight.
  - Studies sapphire, fluorite, garnet, moissanite, quartz and calcite, each at its real spacing. The anode melts above 2.2 kW.
- **Crystal Growth Furnace** (multiblock: a Czochralski puller with argon cylinders):
  - Grows **Sapphire**, **Fluorite** and **Garnet Boules**. Its **Iridium Crucible** is made of Iron-Nickel, since planetary cores and iron meteorites hoard iridium.
  - Pull rate over thermal gradient (Voronkov's ratio) decides whether the boule grows free of defects.
  - Slow pulling and stirring leave the impurities in the melt (Burton-Prim-Slichter). Optical boules must be close to the purity limit.
- **Optical Workshop** (multiblock: a stone bench with grinding spindles and an interferometer):
  - The lensmaker's equation, P = (n − 1)(C₁ − C₂), sets each piece's power. Polishing brings the surface error under λ/4, or λ/8 and λ/16 for correctors and gratings.
  - Makes Fused Silica, Fluorite and Moissanite Lenses, Sapphire Windows (four from a boule), **Schmidt Corrector Plates** and **Echelle Gratings**.
  - Also a second route to the **Photonic Lens**, since lithography lenses really are fluorite and fused silica.
  - Crystals need a Laue Pattern to be cut; glass doesn't.
- **Better telescopes:** each of these optics sits in the Reflecting Telescope as a catalyst and is never used up.
  - A Schmidt Corrector turns it into a survey camera: 2 Star Field Plates and a Deep Sky Plate per run.
  - An Echelle Grating gives 3 Stellar Spectrum Plates per exposure.
  - An Apochromatic Objective (fluorite between two silica lenses) gives 2 Planetary Plates.
- **Research:** Crystallography, Crystal Growth and Geometric Optics, each with its derivation. **Codex:** a new Optics & Crystals chapter.
- **New working animations:** Debye-Scherrer cones in the diffractometer, and Newton's rings over the workshop's test stand.
- **Changed (post-1.0 content only):**
  - The Solar Furnace now melts corundum and clay into Sapphire Boules; the Optical Workshop cuts them into windows.
  - Radio Astronomy now also needs Geometric Optics.

#### Ripples

General Relativity reaches Tier III: measuring curved spacetime.

- **Laser Interferometer** (multiblock, after LIGO: an L of two 30-block vacuum arms, a corner station and two end stations with fused-silica test masses on isolation stacks):
  - Detects gravitational waves. Black hole mergers (strain 10⁻²¹) and neutron star mergers (five times fainter) each give a record and a lot of Insight.
  - **Shot noise** falls with laser power and arm finesse, while **radiation-pressure noise** rises with them. Their balance is the standard quantum limit.
  - **Seismic noise:** each isolation stage cuts it tenfold, but slows locking. Machines running nearby make it worse; below sea level it is quieter (as at KAGRA).
  - **Thermal noise** in the mirrors falls with their temperature, so neutron star mergers need cryogenic coolant.
- **Pound-Rebka Tower** (multiblock, built 1 to 8 storeys tall):
  - Gamma rays from a **Cobalt-57 Source** climbing the tower lose gH/c² of their frequency. Set the Doppler drive to v = gH/c (under a micrometre a second) to cancel it.
  - Count long enough for the shift to stand five counting errors clear.
  - Keep source and absorber at the same temperature: each kelvin between them shifts the line by 0.66 µm/s.
- **Lasers** (Optics):
  - Neodymium-doped garnet rods are cut from the garnet boule.
  - A **Laser Head** sits behind a **Faraday Isolator**, garnet in a magnetic field that lets light out but not back.
  - The workshop also makes **Test Masses** (polished to λ/27) and **Beam Splitters**.
- **New uses:**
  - A Neutron Star Merger Record points the Reflecting Telescope at the kilonova, where gold and platinum are made.
  - Black hole records are evidence in the Bayesian Analyzer, as real merger parameters are found by Bayesian inference.
- **Frame Drag** is now in the game. Within 8 blocks of a running Penrose Ergosphere or Kugelblitz Chamber, a spinning black hole carries you sideways. *(Behaviour change to two 1.0 machines; it affects players only, never blocks.)*
- **Research:** Lasers, Gravitational Redshift and Gravitational Waves, each with its derivation. **Codex:** a Lasers page (Optics) and three Spacetime pages.
- **New working animations:** laser pulses running out along both arms, and gamma rays climbing the tower.
- **Recipes:** no existing recipe changes.

#### Chance & Order

Probability reaches Tier III, where chance becomes statistical mechanics.

- **Brownian Microscope** (multiblock: an upright microscope on the optical bench), Perrin's experiment:
  - Clay grains in a Colloid Cell wander as molecules strike them. Their mean square displacement grows as ⟨x²⟩ = 2Dt, with D = kT/(6πηr), and that gives Avogadro's number.
  - The coolant sets the water's temperature, and its viscosity changes steeply with it. Keep it between freezing and boiling.
  - The statistical error falls as √(2/n) with the tracks followed. Position errors (from the objective's resolution) inflate the result.
  - Within 3% gives an **Avogadro Record** and a Normal Distribution Crystal: the displacements fall on a bell curve. It also gives Insight.
- **Monte Carlo Engine** (multiblock: a hall of stochastic-core racks with entropy sinks on top):
  - Error σ/√N, for up to 10¹² samples. Importance sampling cuts σ when it fits the problem and inflates it when it doesn't.
  - Every erased bit gives off kT·ln 2 (Landauer), so a cooler engine runs with less heat.
  - Its central limit theorem grows Normal Distribution Crystals, and its Markov chains turn 2 Prior Tablets into 4 Posteriors.
- **Random Walker** (new creature, at night anywhere in the Overworld):
  - A cloud of grains on an honest random walk, drifting toward prey. Its own AI, voice and look.
  - Its touch brings **Variance** (luck evens out). It sometimes drops a Normal Distribution Crystal.
- **Information Loss** is now in the game. Standing beside a running Maxwell's Demon Sorter, its forgetting spills into you. *(Behaviour change to a 1.0 machine; it affects players only.)*
- **Research:** Brownian Motion and Monte Carlo Methods, each with its derivation. **Codex:** three Probability pages.
- **New working animations:** grains jittering under the microscope, and random points finding π in the engine.
- **Recipes:** no existing recipe changes.

#### Fields

Electromagnetism reaches Tier III.

- **Cyclotron** (multiblock: round pole pieces and coils, the two copper dees in the gap, return-yoke posts and an RF oscillator):
  - Tune the RF to the protons' cyclotron frequency, f = qB/2πm (15.25 MHz per tesla). The beam leaves the edge with E = (qBR)²/2m.
  - As the protons gain energy their mass grows and they slip out of phase. Keep the slip under 90°, which caps the classical cyclotron near ten MeV.
  - Iron saturates above 2.2 T, and more than 2 kW of beam melts the target.
  - Protons on iron make **Cobalt-57** (⁵⁷Fe(p,n)⁵⁷Co), two at a time: a second route to the Pound-Rebka source. Gives Insight.
- **Tesla Coil** (multiblock: capacitor bank, flat primary spiral, tall secondary and toroid):
  - Tune the primary (capacitance and turns tapped) to the secondary's 159 kHz. In tune, the voltage steps up by √(C₁/C₂), and arcs reach a metre per 700 kV.
  - Its arcs fix nitrogen from the air into fertiliser (the Birkeland-Eyde process: water into bone meal). Over 2.5 m they fuse sand into Fused Silica, as lightning makes fulgurites.
  - **A running coil strikes anyone within six blocks.** The bolt is visual only: it never starts fires or changes blocks.
- **Research:** Resonant Circuits and The Cyclotron, each with its derivation. **Codex:** two Electromagnetism pages.
- **New working animations:** a proton spiral between the dees, and branching arcs from the toroid.
- **Recipes:** no existing recipe changes.

#### Unification

The capstone of the expansion: two real experiments that put quantum mechanics and gravity into one measurement, and a new road to quantum spacetime.

- **Silicon Boule:** fused silica and coal grown in the Crystal Growth Furnace (silicon melts at 1687 K). Study it in the X-Ray Diffractometer, then cut two into a **Perfect Crystal Interferometer** (three blades from one crystal) at the Optical Workshop.
- **Neutron Interferometer** (multiblock: a lead-shielded source, a neutron guide on stands, and a tilt table carrying the crystal between two helium-3 detectors), the Colella-Overhauser-Werner experiment:
  - A radium-beryllium source's neutrons are split by the crystal. Tilting it lifts one path above the other, and gravity shifts the phase between them by Δφ = 2πm²gλA·sin α/h².
  - Choose the wavelength and tilt: at least 30 rad of shift, landing on a bright fringe. Count long enough to fix the phase to 0.1 rad.
  - Gives a **Gravity-Phase Record** and a great deal of Insight. The Perfect Crystal Interferometer is a catalyst and is never used up.
- **Analog Black Hole** (multiblock: a vacuum tube on the optical bench ringed by trapping magnets, the condensate trap at its middle, and a step laser above):
  - Bose-Einstein Condensate flows over a laser step: subsonic before it and supersonic after it, a horizon for sound.
  - The sound speed grows as √(density) and the step speeds the flow by √(v² + 2kU/m). Phonon pairs appear across the horizon at the Hawking temperature T_H = ħκ/2πk_B.
  - They show only against a colder condensate, summed over enough shots. Keep the machine below 5 K; only Bose-Einstein Condensate as the coolant is cold enough.
  - Gives a **Hawking Correlation Record** and Insight.
- **Semiclassical Gravity** (capstone research, needing both experiments plus General Relativity and Monte Carlo Methods): the Monte Carlo Engine runs **causal dynamical triangulations**. Four records of each experiment and two Metric Tensors make a **Spin Foam Lattice**, a second route alongside graviton condensing.
- **Research:** Neutron Interferometry, Analog Gravity and Semiclassical Gravity, each with its derivation (the gravitational phase, the Hawking temperature, the Planck length). **Codex:** three Spacetime pages.
- **New working animations:** neutron wave packets taking both paths and the two exits brightening in turn; the condensate streaming past a glowing horizon while Hawking pairs part across it.
- **Recipes:** no existing recipe changes. The Spin Foam Lattice's original recipe is unchanged; the new one is an alternative.

#### The Accretion Frontier

The second new dimension of the 1.1 design.

- **The Accretion Frontier:**
  - Floating islands round an accreting black hole, reached with a **Light Sail** (Galactic Dynamics). Unfurl it at y 200 or higher under open sky, and again in the Frontier to return.
  - Five biomes, each with its hazard. None of the hazards changes blocks.
    - **Molecular Cloud Nebula:** dust that blinds you.
    - **Asteroid Belt:** low gravity, and micrometeorites unless you wear a **Whipple Shield**. Each piece stops a quarter; a full set stops all.
    - **Pulsar Field:** a radiation pulse every 5 s on the world clock, high gravity and Time Dilation.
    - **Accretion Rim:** heat, tidal strain and plasma lakes.
    - **Lensing Void:** Frame Drag and warped sight.
  - Ores for interstellar dust, exoplanet cores, brown dwarf cores, magnetar crust, pulsar timing crystals, cosmic ray shards, accretion fragments and Einstein ring lenses.
- **Creatures**, each with its own AI:
  - **Nebuloid:** blinds you.
  - **Pulsar Wisp:** pulses on an exact beat; its radiation falls off as 1/r².
  - **Tidal Stretcher:** a 1/r³ pull, and Tidal Strain.
  - **Lens Phantom:** drawn away from its real body.
  - **Hawking Imp:** grows hotter and faster as it shrinks, then pops without breaking blocks.
  - **Boltzmann Brain:** mutters and vanishes.
  - **Black Swan:** in the Overworld, only after three days with nothing improbable.
  - **The Binary** (mini-boss): play a Binary Black Hole Merger record in the Frontier. The two serpents spiral in as da/dt ∝ −1/a³. Kill both before they merge, or the merger sends out a shockwave.
- **Structures:**
  - Derelict Station, floating in the Frontier.
  - Pulsar Beacon, in the Pulsar Field.
  - Probability Hall, a rare house added to vanilla villages.
- **Astrophysics:**
  - **Interstellar Scoop** (single block): works only at y 200+, or in the Frontier or the End.
  - **Accretion Disk Reactor** (generator): L = ηṁc², with η set by spin. It stops past the Eddington luminosity.
  - **Research:** Interstellar Medium, Exoplanetology, Accretion Physics and The Cosmic Web.
  - Cepheids, redshift lenses, galactic and cosmic-web filaments, and planetary nebulae.
- **Fluids:** Molecular Cloud, Interstellar Medium, Accretion Disk Plasma and Tidal Plasma.
- **Recipes:** recipe conditions now also accept a minimum height (`min_y`).

**From here on:** every recipe and research node is new (until the final balance pass, below).

#### Fields & Nuclei

The rest of Electromagnetism and Nuclear Physics.

- **Tokamak** (generator; a torus with twelve toroidal field coils and a central solenoid):
  - Ignition needs a Lawson triple product nTτ ≥ 3×10²¹. Confinement time grows as B².
  - Keep β under 5%. The coils need to stay below 20 K and below 12.5 T.
  - Burns D-T, D-D, or D-T with a breeding blanket. It also makes Magnetized Plasma.
- **Pulsed Magnet Lab** (variable-height room with capacitor racks outside):
  - B = μ₀NI/L. Coil heating is limited, and more storeys add copper and cooling.
  - At 40 T it magnetises samarium-cobalt in bulk. At 80 T it searches magnetar crust for Magnetic Monopoles.
- **Hypervelocity Impact Forge** (railgun line):
  - F = ½L′I², and impact pressure ≈ ½ρv².
  - Graphite shocked at 15–40 GPa becomes Lonsdaleite. Sand and coal at 50–90 GPa become moissanite.
- **Burst Observatory** (twelve scintillators on a mast). Trade the trigger threshold against false alarms to catch Gamma-Ray Bursts.
- **Single blocks:**
  - **Laser Cutter:** waveguides, Bitter plates, rail armatures, Hall bar chips and plasma nozzles.
  - **Isomer Trigger:** a generator that burns hafnium-178m2 with a Cobalt-57 gamma pulse.
  - **Decontamination Scrubber:** vitrifies waste and reprocesses spent fuel.
  - **Irradiation Chamber:** uses a neutron-source catalyst to make technetium-99m, tritium, californium-252 and plutonium-239.
- **Research:** Electrodynamics, Magnetic Confinement, Quantum Hall Effect, Magnetars, Magnetic Monopoles, Fusion Ignition, Nuclear Astrophysics, Transuranic Synthesis, Nuclear Isomers, Kilonovae and Gamma-Ray Bursts.
- **New material routes:**
  - Lithium from Deep Brine, enriched to lithium-6.
  - Heavy water and deuterium by electrolysis.
  - Polonium-210 from radium decay, and thorium-232 from monazite.
  - Hafnium-178m2 from the Cyclotron.
- **Fluids:** Heavy Water, Deuterium, Tritium, Ferrofluid, Liquid Sodium and Magnetized Plasma.
- **Radiation:**
  - New radioactive items carry their real isotopes. Hafnium-178m2 is a new isotope.
  - Spent fuel and Californium-252 count as strongly radioactive.

#### Earth & Spacetime

The rest of Geography and General Relativity.

- **Aquifer Pump** (single block):
  - Pumps water anywhere.
  - In the Planetary Interior it gives an Aquifer Sample with Deep Brine.
  - In the Magma Chambers it gives Supercritical Water.
- **Climate Engine** (multiblock tower):
  - ΔF = 5.35 ln(C/280) and ΔT ≈ 0.8 K per W/m².
  - Its weather recipes set the world's weather when they finish: clear, rain or storm.
  - Cold settings make Ice Cores and Permafrost Samples.
- **Lensing Concentrator** (generator; four lens pylons round a receiver):
  - Works at night under open sky. Magnification A = (u²+2)/(u√(u²+4)).
  - Aim jitter limits how close the alignment can get. Above A = 20 it also grows Einstein Ring Lenses.
- **Time Dilation Chamber** (variable-height octagonal room round a dense core). Samples age by 1/√(1 − r_s/r).
- **Warp Field Forge** (multiblock: a standing ring of warp coil) makes the **Warp Bubble Core**, good for one return trip:
  - Using the core anchors where you stand and turns it into an Anchored Warp Bubble.
  - Using the anchored bubble brings you back there and is spent.
  - Recipes only take the plain core.
- **Precision Gyroscope:** carried in the off hand, it cancels Frame Drag and Tidal Strain.
- **Mu-Metal Shield** cancels Magnetized, and the **Faraday Cage Mesh** cancels Decoherence, also carried in the off hand.
- **Research:** Hydrology, Climatology, Remote Sensing, Planetary Geology, Geodesics, Gravitational Lensing, Frame Dragging, Tidal Physics, Binary Mergers and Alcubierre Metric.
- **Items:** Survey Satellite (works in the Interstellar Scoop, high up), Satellite Image Plate, Lithospheric Plate Fragment, Planetary Core Sample, Strain Sensor, Geodesic Compass, Lense-Thirring Vortex Core, Gravitational Wave Echo and Negative Energy Lattice.
- **Fluids:** Supercritical Water and Strained Spacetime.

#### Horizons

The rest of the quantum mechanics–general relativity bridge.

- **Unruh Accelerator** (multiblock: a storage ring with bending magnets, cavities and a detector):
  - Set the beam's γ and the bending field. The beam sees the vacuum at T = ħa/2πck.
  - Gentle settings make an Unruh Thermometer. Above 5000 K the ring turns superfluid helium into Hawking radiation.
  - Synchrotron loss is limited to 500 kW.
- **Micro Black Hole Evaporator** (generator; a lead sphere with a caged hole):
  - T = ħc³/(8πGMk) and power grows as 1/M².
  - Feed the hole exactly what it loses. Underfeeding makes it run away; overfeeding makes it swell.
- **ER=EPR Weaver** (multiblock: two mouths, a pair source between them and an optical bridge). Fidelity falls with distance and with heat, so keep it cold.
- **Spin Network Loom** and **Page Curve Analyzer** (single blocks).
- **Research:** Unruh Effect, Hawking Radiation, Black Hole Thermodynamics, Information Paradox, Loop Quantum Gravity, Causal Set Theory, ER = EPR, Asymptotic Safety, Page Curve and Islands, and AdS/CFT.
- **Items:**
  - Unruh Thermometer, Firewall Shard, Soft Hair Filament, Planck Area Tile, Spin Network Node, Causal Set Seed, ER Bridge Thread, Fixed-Point Lattice, Island Fragment, Holographic Screen and Boundary CFT Chip.
  - **Page Curve Record:** carried in the off hand, it cancels Information Loss.
  - The existing Bekenstein Crystal gains a new route.
- **Fluid:** Quantum Spacetime Condensate.

#### Ensembles

The rest of Statistical Probability. With this release the 1.1 expansion design is complete.

- **Markov Chain Refinery** (multiblock: three vats in a ring joined by transition valves):
  - Set the chances A→B, B→C and C→A. The feed settles where π ∝ 1/(chance of leaving).
  - A vat holding at least 60% of the time turns monazite sand into its product: rare earths, thorium or phosphate.
  - With all three vats equal, warm helium thermalises into Maxwell-Boltzmann Gas.
- **Research:** Stochastic Processes, Statistical Mechanics, Extreme Value Theory, Ergodic Theory and The Measure Problem.
- **Items:**
  - Markov Chain Link, Poisson Clock, Monte Carlo Sample Pack, Partition Function Matrix, Large Deviation Shard and Ergodic Mixer Core.
  - **Confidence Interval Gauge:** carried in the off hand, it cancels Variance.
  - A slow, guaranteed route to a Boltzmann Brain through the Monte Carlo Engine.
- **Fluids:** Brownian Suspension and Maxwell-Boltzmann Gas.

#### Accretion Frontier fixes

**The Accretion Frontier**
- **A black hole in the sky.** The Frontier has its own sky: a black shadow with a bright photon ring, a tilted accretion disk (white-hot inside, red outside, brighter on the side turning towards you), the far side of the disk lensed over the top of the hole, two faint jets, and stars near it pushed outwards by gravitational lensing.
- **Travel animations** for the Frontier (a light sail streaking past stars towards a growing black hole) and for the Planetary Interior (rock layers rushing past as the core's glow rises), on the way in and the way out, like every other dimension.
- **Music:** each has its own piece. The Interior: a deep E Phrygian drone with the thuds of settling rock and bubbling magma. The Frontier: a cold A Lydian piece with orbiting bell lines, a distant pulsar and the chirps of merging black holes.
- **The Molecular Cloud Nebula is made of cloud.** Its islands are now Dust, Hydrogen (red) and Oxygen (teal) Cloud round a heart of Nebular Ice.
  - You can walk and fall straight through cloud. Its drag slows a fall to a gentle sink, so dropping through never hurts. Hold jump to climb back up through it.
  - With Silk Touch clouds come away whole. Otherwise the gas leaves nothing (gathering it comes in a later expansion) and dust sometimes leaves Interstellar Dust.
  - **Protostar Fragments**, small glowing pieces of unlit stars, sit inside the clouds and ice. They drop a **Protostellar Ember**: long-burning thermoelectric fuel, or Accretion Disk Reactor feed.
- **Every Frontier block has a use:**
  - Regolith → dust and rare earths (Spectrograph).
  - Nebular Ice → Molecular Cloud (Electrolytic Cell).
  - Asteroid Rock → meteoric iron and chondrites (Spectrograph).
  - Pulsar Crust → Magnetar Crust (Pulsed Magnet Lab).
  - Rim Slag → Accretion Disk Fragments (Tectonic Press).
  - Void Glass → Einstein Ring Lenses (Optical Workshop).
  - Station Hull → the Frontier Suit.
- **Codex pages** for every Frontier block (where it is found, what it is for), plus a page listing the counter to each hazard.
- **Counters for every hazard: the Frontier Suit**, made of Station Hull. Each piece answers one region:
  - **Adaptive Optics Visor:** no nebula blindness, no warped sight in the Lensing Void, and a Nebuloid cannot blind you.
  - **Thermal Shroud:** the Accretion Rim cannot set you alight.
  - **Tidal Brace:** no Tidal Strain in the Rim.
  - **Gravity Compensators:** normal gravity in the Asteroid Belt and the Pulsar Field.
- **The Gravitational Wave Echo now does what its tooltip says.** Played in the Frontier, it summons The Binary, as a black-hole merger record does.

**Creatures**
- **New voices for all eight Frontier creatures; none shares a sound with anything else:**
  - Nebuloid: a drifting breath of wind.
  - Pulsar Wisp: dispersed radio pulses on a beat, spinning down when it dies.
  - Tidal Stretcher: a string pulled tighter until it snaps.
  - Lens Phantom: a beating glass-harmonica shimmer.
  - Hawking Imp: a hiss whose crackle speeds up as it evaporates.
  - Boltzmann Brain: muttering through a wall.
  - Black Swan: a bugling honk and a real swan's hiss.
  - The Binary: two beating tones, ending in an inspiral chirp and ringdown.
- The Annihilator no longer shares the Tidal Horror's voice.
- The pulsar beat, micrometeorite strikes, the Imp's last flash and The Binary's summoning and waves have their own sounds instead of borrowed vanilla ones.
- **The Pulsar Wisp pulses.** On each beat its core flares and swells, its beams brighten, and a shell of light spreads out from it.
- **Death animations:** creatures no longer all shrink away. Each dies its own way, with its own particles:
  - topple: Black Swan, Ordinal Hound, Hilbert Bellhop;
  - flip onto its back: Planck Mite, Positronium Skitterer, Magma Crawler;
  - crumple: Fission Husk;
  - stretch into a thread: Tidal Stretcher, Tidal Horror;
  - disperse like smoke: Nebuloid, Photon Moth;
  - swell and flash: Hawking Imp, Virtual Pair, Annihilator;
  - fold into a lower dimension: Hypercube Drifter;
  - deflate and sink: Vacuum Drifter;
  - slump into a puddle: Plume Elemental;
  - spin into the ground: Geodynamo Wraith, The Binary;
  - wobble like a dying top: Pulsar Wisp;
  - flicker out: Lens Phantom, Antihydrogen Wisp;
  - scatter at random: Random Walker;
  - fluctuate out of existence: Boltzmann Brain.

**Blocks**
- **Fluids render.** 15 of the new fluids had no textures. They now have still and flowing textures and cell icons.
- **Block sounds match the block:**
  - Rock sounds like vanilla stone (or basalt, tuff, deepslate or gravel, as it resembles): bulk stone, the Interior's and Frontier's rocks, and stone ores.
  - Crystals ring like amethyst. Glass, ice and lamps sound like glass. Arbor planks sound like wood. Clouds are soft.
  - Metal blocks and machines keep their sound.
- **Unique machine animations:** 15 multiblocks that shared one animation now each have their own:
  - Molten Salt Reactor: salt glow.
  - Crystal Growth Furnace: a turning Czochralski crystal.
  - Deep Drilling Rig: a turning drill string.
  - Gas Centrifuge Cascade: spinning rotors.
  - MHD Generator: plasma flowing through a field.
  - Eschaton Foundry: heat death.
  - Burst Observatory: a gamma-ray burst.
  - Markov Chain Refinery: hops between vats.
  - Lensing Concentrator: an Einstein ring.
  - Time Dilation Chamber: rings slowing towards the centre.
  - Warp Field Forge: a warp bubble.
  - Unruh Accelerator: a thermal bath along the beam.
  - Micro Black Hole Evaporator: Hawking sparks.
  - ER=EPR Weaver: a bridge between the mouths.
  - Accretion Disk Reactor: quasar jets.

**Materials and recipes** (only recipes from 1.1 onward were changed)
- **Every Frontier material can now be made again by machine:**
  - Cosmic Ray Shards: the Interstellar Scoop, from glass, at y ≥ 200 or in the Frontier or the End.
  - Brown Dwarf Cores: Tectonic Press, from dust and cloud gas.
  - Exoplanet Core Samples: Hypervelocity Impact Forge.
  - Accretion Disk Fragments: Tectonic Press.
  - Magnetar Crust: Pulsed Magnet Lab.
  - Tidal Shear Crystals and Tidal Plasma: Time Dilation Chamber.
  - Merger Remnants: Micro Black Hole Evaporator.
  - Chirp Signal Records: Laser Interferometer.
- **Every material now has a use**, for example:
  - Planetary Nebula Shells press into White Dwarf Cores.
  - Two Merger Remnants make a Micro Singularity.
  - Burst Afterglow Crystals between lead plates give antimatter (pair production).
  - Thorium makes molten-salt fuel. Liquid Sodium is a coolant. Ferrofluid seals the lithium centrifuge.
  - Nuclear Waste Glass, Polonium-210 and Protostellar Embers are thermoelectric fuel.
  - Transit light curves, ice cores, permafrost, tracer vials and Monte Carlo sample packs are Bayesian evidence.
  - The plasma thruster nozzle goes into the Jet Collimator.
  - Boundary CFT chips, fixed-point lattices, Unruh thermometers, geodesic compasses and moissanite lenses are catalysts for cheaper or faster routes.
- **Recipes no longer copy each other:**
  - Fused Silica: the Solar Furnace makes 1 from 1 quartz. The Tesla Coil's fulgurite turns 16 sand into 6 in bulk. They are no longer near-copies of each other or of the furnace.
  - Boule studies give more Laue patterns, faster.
  - Sapphire from corundum gives 3 boules.
  - The Kilonova needs a neutron-star merger record.
  - The Climate Engine's lithosphere drilling needs moissanite, so it differs from the Moho drill.
  - The Markov Refinery's yields are higher than the Spectrograph's.
  - Radium ageing is faster.
  - The galactic survey gives more maps.
- **The Black Swan Feather is a shortcut, not a crop:**
  - Each swan drops exactly one, and Looting does not add more.
  - The recipe that multiplied feathers is gone. Large Deviation Shards are now made in the Monte Carlo Engine from entropy dice and normal-distribution crystals.
  - A feather now replaces long chains in expensive recipes: with an Ergodic Mixer Core it makes a Boltzmann Brain, and with Magnetar Crust it makes a Magnetic Monopole.
- Three research nodes that sat on top of others on the research map have moved to free spots.

#### Every recipe earns its place

**Recipe balance** (all recipes, 1.0 included)
- Every recipe was priced the way you pay for it, and every material with more than one route was checked against its other routes:
  - Vanilla materials cost 1 each, except the truly tedious ones (Nether Star, Netherite Scrap and Ingot, Dragon Head, Dragon Egg, Elytra).
  - The mod's materials cost what their cheapest route costs.
  - Running a machine costs its energy (200,000 FE ≈ 1, about what a lump of coal gives in the mod's generators) and its time.
  - A machine costs its own recipe; a multiblock adds every block of its structure.
- **The rule:**
  - A route on a dearer machine must be substantially cheaper per item (at most 60% of the cheaper machine's cost), or there is no reason to build that machine.
  - So must a newer route on an equal machine (at most 80% of the older route's cost).
  - Two routes side by side on one machine stay within a quarter of each other.
  - A machine route for a vanilla item must beat simply gathering it.
- Routes that failed were given what they needed: more output, or less input and power.
- Machine routes for vanilla items that could not be made worthwhile, and post-1.0 routes over 64 times too dear, were removed.
- 1.0 recipes were rebalanced but never deleted, because progression is built on them. A route that is a material's only use was rebalanced instead of removed.
- The pass now runs on every rebuild (tools/balance.py, reported in tools/balance_report_final.txt), so future recipes are held to the same rule.
- **Examples:**
  - The Tesla Coil's fulgurite turns 16 sand into 35 Fused Silica. The Solar Furnace's sand route is now on a par with its quartz route.
  - The Time Dilation Chamber ages 4 Radium Salt into 5 Polonium-210 Sources, 2.5 times the Decay Chamber's yield per radium.
  - Diamonds, bone meal and iron, copper and gold are no longer made by machines at many times the cost of mining them.
- Shortcuts added earlier in this update were fixed:
  - **Pair production** made antimatter far more cheaply than the Penning Trap. It is gone; Burst Afterglow Crystals are now Bayesian evidence.
  - Poisson Clocks are now a catalyst: the Monte Carlo Engine turns a silicon boule into 8 random number chips.
  - Fixed Point Lattices let the Graviton Condenser weave spin foam from half the material.
  - Planetary Core Samples go into the Gravity Compensators.
**Dimensions, creatures and interface**
- **Falling off the Accretion Frontier** no longer kills you: the light sail's tether pulls you back to the Overworld, as every other dimension's void does.
- **The simulation window of the operator console** draws what each machine does. Before, 36 multiblocks shared one stirred beaker and 16 shared one tesseract. Now 50 machines have scenes of their own, for example:
  - a radio dish with the 21 cm line rising out of the noise;
  - a crystal pulled from its melt;
  - an L-shaped interferometer whose arms breathe as a wave passes;
  - a drill string going down through strata;
  - Monte Carlo darts landing in a quarter circle.
- **The Black Swan** is rebuilt the way vanilla builds its animals:
  - a deep body, legs with webbed feet that swing as it walks, and folded wings with curled feathers and white flight-feather tips;
  - a raised tail, and a long neck of six segments in an S that sways and bobs;
  - red eyes and a red bill banded with white.
  - Struck, it raises its wings and draws its neck back, as a threatened swan does.
- **The Pulsar Wisp's pulse** no longer makes nearby creatures vanish. Its halo and spreading shell are now pure light that hides nothing.
- **The Boltzmann Brain** is solid on both sides. Mirrored parts drawn with their corners reversed were invisible from outside, and every procedural model now sorts its corners.
- **Singularity clusters are lettered:** Greek for clusters (Singularity Cluster Alpha α), Hebrew for hyperclusters (Singularity Hypercluster Aleph א), doubled Greek for the levels above.
- The Codex now says a Gravitational Wave Echo summons The Binary as well as a merger record.

### 1.0 recipes changed in 1.1.0

Every recipe that shipped in 1.0 and works differently now (395 changed, 0 removed), by machine. Recipes new in 1.1.0 are not listed.

- **Abiogenesis Crucible**
  - 16 Spark of Life, 128 Tardigrade Culture, 16 Bubble Universe, 32 Unbihexium Ingot, 32 Spacetime Bearing, 32 Mathematics Singularity, 64 Massless Alloy, 32 Stellar Singularity, 64 Superposition Crystal, 32 Inaccessible Cardinal, 64 Strange Matter Singularity, 1024000 mB Xenoplasm, 1024000 mB Primordial Soup, 64000 mB Warm Helium, 64000 mB Nanite Slurry, 64000 mB False Vacuum → 1 Panspermia Seed (134,217,728 FE/t, 240 s)  ⟶  16 Spark of Life, 128 Tardigrade Culture, 16 Bubble Universe, 32 Unbihexium Ingot, 32 Cosmic Flux Coupler, 32 Spacetime Bearing, 32 Cognitive Singularity, 64 Virtual Quark, 32 Axiom of Choice, 64 Spacetime Alloy Singularity, 32 Banach-Tarski Sphere, 1024000 mB Xenoplasm, 1024000 mB Primordial Soup, 64000 mB Superconducting Solder, 64000 mB Liquid Spacetime, 64000 mB Reheating Plasma → 1 Panspermia Seed (134,217,728 FE/t, 240 s)
  - 32 Telomerase Complex, 64 Quantum Chlorophyll, 64 Minimal Synthetic Cell, 16 Omega Point, 32 Spacetime Bearing, 32 Unified Notes II, 64 Causal Shield Lattice, 16 Unified Dossier, 64 Quine Replicator, 32 Axiom of Choice, 64 Netherite Singularity, 2048000 mB Primordial Soup, 512000 mB Stellar Plasma, 64000 mB Exotic Matter, 64000 mB Autocatalytic Broth, 64000 mB Quantum-Dot Photoresist → 1 Spark of Life (67,108,864 FE/t, 240 s)  ⟶  32 Telomerase Complex, 64 Quantum Chlorophyll, 64 Minimal Synthetic Cell, 16 Omega Point, 32 Cosmic Flux Coupler, 32 Spacetime Bearing, 32 Stellar Singularity, 64 D-Brane Sheet, 32 Temporal Singularity, 64 Spacetime Catalyst, 32 Topos, 2048000 mB Primordial Soup, 512000 mB Stellar Plasma, 64000 mB Warm Helium, 64000 mB Reheating Plasma, 64000 mB Raw Star Matter → 1 Spark of Life (67,108,864 FE/t, 240 s)
- **Analytic Continuator**
  - 12 Real Continuum, 12 Imaginary Unit, 6 Electride Crystal, 24 Maxwell's Demon Chip, 8000 mB Bose-Einstein Condensate, 8000 mB Tachyon Flux → 1 Complex Plane (2,048 FE/t, 18.75 s)  ⟶  12 Real Continuum, 12 Imaginary Unit, 6 Sterile Neutrino, 6 Planck Shard, 8000 mB Bose-Einstein Condensate, 8000 mB Quantum-Dot Photoresist → 1 Complex Plane (2,048 FE/t, 18.75 s)
  - 24 Infinitesimal, 6 Function Graph, 6 Prime Shard, 6 Coherent Wiring Harness, 8000 mB Dark Matter → 1 Derivative Operator (2,048 FE/t, 12.5 s)  ⟶  24 Infinitesimal, 6 Function Graph, 6 Topological Insulator, 12 Fractured Essence, 8000 mB Dark Matter → 1 Derivative Operator (2,048 FE/t, 12.5 s)
  - 12 Derivative Operator, 6 Integral Operator, 6 Topological Insulator, 6 Arrow of Time Shard, 8000 mB Bose-Einstein Condensate, 8000 mB Nutrient Broth → 1 Differential Manifold (4,096 FE/t, 25 s)  ⟶  12 Derivative Operator, 6 Integral Operator, 6 Topological Insulator, 6 Error-Correction Lattice, 8000 mB Bose-Einstein Condensate, 8000 mB Nanite Slurry → 1 Differential Manifold (4,096 FE/t, 25 s)
  - 6 Hamiltonian Operator, 6 Spinor Lattice, 12 Virtual Photon, 6 Entangled Bus Bar, 16000 mB Schrödinger's Limit, 8000 mB Chronon Gas → 2 Eigenstate Lattice (131,072 FE/t, 18.75 s)  ⟶  6 Hamiltonian Operator, 6 Spinor Lattice, 12 Virtual Photon, 6 Entangled Bus Bar, 16000 mB Schrödinger's Limit, 8000 mB Liquid Metallic Hydrogen → 2 Eigenstate Lattice (131,072 FE/t, 18.75 s)
  - 12 Function Graph, 6 Complex Plane, 6 Integral Operator, 6 Higgs Field Capacitor, 8000 mB Superconducting Solder → 2 Fourier Series (4,096 FE/t, 18.75 s)  ⟶  12 Function Graph, 6 Complex Plane, 6 Integral Operator, 6 Exotic Matter Crystal, 8000 mB Synaptic Fluid → 2 Fourier Series (4,096 FE/t, 18.75 s)
  - 12 Complex Plane, 12 Function Graph, 48 Fractal Dust, 6 Femtoscale Inductor, 8000 mB Nanite Slurry → 1 Fractal Set (4,096 FE/t, 25 s)  ⟶  12 Complex Plane, 12 Function Graph, 48 Fractal Dust, 6 Tachyon Klystron, 8000 mB Quantum-Dot Photoresist → 2 Fractal Set (4,096 FE/t, 25 s)
  - 6 Real Continuum, 6 Photonic Neuron, 6 Neural Lattice, 24 Planck Shard, 8000 mB Warm Helium → 2 Function Graph (1,024 FE/t, 12.5 s)  ⟶  6 Real Continuum, 6 Photonic Neuron, 24 Zeolite Catalyst, 24 Electrical Steel Lamination, 8000 mB Superconducting Solder → 2 Function Graph (1,024 FE/t, 12.5 s)
  - 24 Golden Ratio Glass, 12 Rational Field, 6 Neural Lattice, 24 Casimir Plate, 8000 mB Bose-Einstein Condensate, 8000 mB Tachyon Flux → 1 Real Continuum (8,192 FE/t, 18.75 s)  ⟶  24 Golden Ratio Glass, 12 Rational Field, 12 Photonic Processor, 24 Cherenkov Wafer, 8000 mB Bose-Einstein Condensate, 8000 mB Supercritical Solvent → 2 Real Continuum (8,192 FE/t, 18.75 s)
  - 12 Wavefunction, 6 Derivative Operator, 6 Entangled Bus Bar, 6 Cosmic Circuit Board, 16000 mB Schrödinger's Limit, 8000 mB Antihydrogen → 1 Hamiltonian Operator (131,072 FE/t, 18.75 s)  ⟶  12 Wavefunction, 6 Derivative Operator, 6 Entangled Bus Bar, 6 Dilaton Crystal, 16000 mB Schrödinger's Limit, 8000 mB Strange Matter → 1 Hamiltonian Operator (131,072 FE/t, 18.75 s)
  - 6 Real Continuum, 24 Quantum Dot, 6 Tachyon Klystron, 24 Crystalline Matrix Ingot, 3200 mB Superfluid Helium, 8000 mB Neutrino Flux → 8 Infinitesimal, 100 mB Warm Helium (1,024 FE/t, 12.5 s)  ⟶  6 Real Continuum, 24 Quantum Dot, 12 Photonic Processor, 24 Radium Salt, 3200 mB Superfluid Helium, 8000 mB Supercritical Solvent → 8 Infinitesimal, 100 mB Warm Helium (1,024 FE/t, 12.5 s)
  - 48 Infinitesimal, 6 Function Graph, 6 Programmable Matter, 24 Superconducting Coil, 8000 mB Neutrino Flux → 1 Integral Operator (2,048 FE/t, 18.75 s)  ⟶  48 Infinitesimal, 6 Function Graph, 6 Error-Correction Lattice, 24 Sediment Core, 8000 mB Superconducting Solder → 1 Integral Operator (2,048 FE/t, 18.75 s)
  - 24 Koch Crystal, 12 Complex Plane, 6 Function Graph, 6 Fractal Dust, 8000 mB Ionic Liquid → 2 Fractal Set (4,096 FE/t, 18.75 s)  ⟶  24 Koch Crystal, 12 Complex Plane, 6 Function Graph, 6 Fractal Dust, 8000 mB Dark Matter → 2 Fractal Set (4,096 FE/t, 18.75 s)
  - 24 Prime Shard, 6 Complex Plane, 6 Fourier Series, 6 Arrow of Time Shard, 8000 mB Strange Matter → 1 Modular Form (8,192 FE/t, 25 s)  ⟶  24 Prime Shard, 6 Complex Plane, 6 Fourier Series, 6 Dark Matter Lens, 8000 mB Nanite Slurry → 1 Modular Form (8,192 FE/t, 25 s)
  - 12 Hamiltonian Operator, 12 Integral Operator, 24 Virtual Quark, 6 Entangled Bus Bar, 32000 mB Schrödinger's Limit, 8000 mB Inflaton Field → 1 Path Integral (262,144 FE/t, 25 s)  ⟶  12 Hamiltonian Operator, 12 Integral Operator, 24 Virtual Quark, 6 Entangled Bus Bar, 32000 mB Schrödinger's Limit, 8000 mB Chronon Gas → 1 Path Integral (262,144 FE/t, 25 s)
  - 24 Wave Packet, 12 Spinor Lattice, 6 Differential Manifold, 6 Entangled Bus Bar, 32000 mB Bose-Einstein Condensate, 8000 mB Neutrino Flux → 1 Quantum Field Lattice (8,192 FE/t, 37.5 s)  ⟶  24 Wave Packet, 12 Spinor Lattice, 6 Differential Manifold, 6 Entangled Bus Bar, 32000 mB Bose-Einstein Condensate, 8000 mB Carborane Superacid → 1 Quantum Field Lattice (8,192 FE/t, 37.5 s)
  - 24 Rational Field, 6 Transcendental Crystal, 6 Metamaterial Sheet, 6 Tachyon Klystron, 8000 mB Bose-Einstein Condensate, 8000 mB Warm Helium → 2 Real Continuum (1,024 FE/t, 18.75 s)  ⟶  24 Rational Field, 6 Transcendental Crystal, 6 Metamaterial Sheet, 6 Qubit Die, 8000 mB Bose-Einstein Condensate, 8000 mB Warm Helium → 3 Real Continuum (1,024 FE/t, 18.75 s)
  - 96 Prime Shard, 12 Complex Plane, 12 Modular Form, 6 Entangled Bus Bar, 32000 mB Superfluid Helium, 8000 mB Nebular Gas → 1 Riemann Zeta Crystal, 1000 mB Warm Helium (16,384 FE/t, 50 s)  ⟶  96 Prime Shard, 12 Complex Plane, 12 Modular Form, 6 Penrose Spindle, 32000 mB Superfluid Helium, 8000 mB Ionic Liquid → 1 Riemann Zeta Crystal, 1000 mB Warm Helium (16,384 FE/t, 50 s)
  - 6 Fourier Series, 24 Entangled Photon Pair, 6 Metal-Organic Framework, 12 Rational Field, 8000 mB Supercritical Solvent → 4 Wave Packet (4,096 FE/t, 18.75 s)  ⟶  6 Fourier Series, 24 Entangled Photon Pair, 6 Annihilation Core, 24 Quantum Random Number Chip, 8000 mB Tachyon Flux → 4 Wave Packet (4,096 FE/t, 18.75 s)
  - 6 Complex Plane, 12 Qubit, 24 Foam Substrate, 6 Entangled Bus Bar, 16000 mB Schrödinger's Limit, 8000 mB Chronon Gas → 2 Wavefunction (65,536 FE/t, 18.75 s)  ⟶  6 Complex Plane, 12 Qubit, 24 Foam Substrate, 6 Entangled Bus Bar, 16000 mB Schrödinger's Limit, 8000 mB Nutrient Broth → 2 Wavefunction (65,536 FE/t, 18.75 s)
- **Anomaly Containment Ward**
  - 64 Fractured Essence, 16 Dimensional Stabiliser Matrix, 12800 mB Superfluid Helium → 16 Contained Anomaly Specimen (4,194,304 FE/t, 80 s)  ⟶  1 Fractured Essence, 1 Dimensional Stabiliser Matrix, 1 mB Superfluid Helium → 64 Contained Anomaly Specimen (1 FE/t, 80 s)
  - 16 Crystallised Law, 16 Dimensional Stabiliser Matrix, 12800 mB Primordial Law → 16 Contained Anomaly Specimen (4,194,304 FE/t, 80 s)  ⟶  1 Crystallised Law, 1 Dimensional Stabiliser Matrix, 1 mB Primordial Law → 64 Contained Anomaly Specimen (1 FE/t, 80 s)
  - 32 Void Essence, 16 Dimensional Stabiliser Matrix, 12800 mB Dark Matter → 16 Contained Anomaly Specimen (4,194,304 FE/t, 80 s)  ⟶  1 Void Essence, 1 Dimensional Stabiliser Matrix, 1 mB Dark Matter → 64 Contained Anomaly Specimen (1 FE/t, 80 s)
- **Antimatter Penning Trap**
  - 24 Antimatter Pellet, 6 Dilithium Crystal, 12 Planck Shard, 32000 mB Superfluid Helium, 8000 mB Supercritical Solvent → 1 Annihilation Core (65,536 FE/t, 37.5 s)  ⟶  24 Antimatter Pellet, 6 Dilithium Crystal, 12 Photonic Logic Wafer, 32000 mB Superfluid Helium, 8000 mB Bose-Einstein Condensate → 1 Annihilation Core (65,536 FE/t, 37.5 s)
  - 12 Entangled Photon Pair, 6 Superconducting Coil, 12 Maxwell's Demon Chip, 8000 mB Superfluid Helium, 8000 mB Bose-Einstein Condensate → 1 Antimatter Pellet (4,096 FE/t, 25 s)  ⟶  12 Entangled Photon Pair, 6 Superconducting Coil, 12 Photonic Lens, 8000 mB Superfluid Helium, 8000 mB Bose-Einstein Condensate → 1 Antimatter Pellet (4,096 FE/t, 25 s)
- **Antiproton Decelerator**
  - 8 Antiproton Bundle, 8 Antimatter Pellet, 8 Recombinant DNA, 16000 mB Superfluid Helium → 1000 mB Antihydrogen (2,097,152 FE/t, 22.5 s)  ⟶  3 Antiproton Bundle, 3 Antimatter Pellet, 8 Integral Operator, 16000 mB Supercritical Solvent → 8000 mB Antihydrogen (892,085 FE/t, 22.5 s)
  - 8 Antihydrogen Crystal, 8 Up & Down Quarks, 32 Casimir Plate, 16000 mB Tachyon Flux → 500 mB Antihydrogen (524,288 FE/t, 7.5 s)  ⟶  8 Antihydrogen Crystal, 8 Penrose Spindle, 8 Hawking Absorber Plate, 16000 mB Dark Matter → 3709 mB Antihydrogen (524,288 FE/t, 7.5 s)
  - 16 Antimatter Pellet, 32 Virtual Quark, 8 Chronoplastic Gasket, 16000 mB Antihydrogen → 2 Antiproton Bundle (1,048,576 FE/t, 22.5 s)  ⟶  6 Antimatter Pellet, 12 Virtual Quark, 8 Chronoplastic Gasket, 16000 mB Dark Matter → 16 Antiproton Bundle (382,428 FE/t, 22.5 s)
  - 8 Antihelium Nucleus, 8 Topological Logic Wafer, 8 Imaginary Unit, 16000 mB Supercritical Solvent → 4 Antiproton Bundle (1,048,576 FE/t, 15 s)  ⟶  8 Antihelium Nucleus, 8 Formal Proof Tree, 32 Neutronium Plate, 16000 mB Supercritical Solvent → 4 Antiproton Bundle (1,048,576 FE/t, 15 s)
- **Axiom Distiller**
  - 64 Source Code Fragment, 128 Paradox Residue, 64 Reality Anchor, 128 Einstein-Rosen Core, 256000 mB Dark Energy, 128000 mB Synaptic Fluid → 2 Axiom Fragment (16,777,216 FE/t, 180 s)  ⟶  1 Source Code Fragment, 1 Paradox Residue, 64 Surreal Number, 256 Natural Number Lattice, 1 mB Dark Energy, 128000 mB Superconducting Solder → 16 Axiom Fragment (1 FE/t, 180 s)
  - 128 Stellar Singularity, 256 Cosmic Neutronium Singularity, 256000 mB Primordial Law, 128000 mB Strange Matter → 4 Axiom Fragment, 1 Crystallised Law (4,194,304 FE/t, 60 s)  ⟶  128 Theoretical Dossier, 256 Cosmic String Segment, 256000 mB Primordial Law, 128000 mB Schrödinger's Limit → 4 Axiom Fragment, 1 Crystallised Law (4,194,304 FE/t, 60 s)
  - 64 Axiom Fragment, 128 Temporal Singularity, 256 Glowstone Singularity, 256000 mB Primordial Law, 128000 mB Chronon Gas → 2 Crystallised Law (8,388,608 FE/t, 90 s)  ⟶  64 Axiom Fragment, 128 Mathematics Singularity, 256 Fourier Series, 256000 mB Primordial Law, 128000 mB Strange Matter → 2 Crystallised Law (8,388,608 FE/t, 90 s)
  - 128 Cosmic Neutronium Ingot, 64 Transfinite Processor, 1024 Book, 1024 Qubit, 128000 mB Quark-Gluon Plasma →  (143,327,232 FE/t, 300 s)  ⟶  128 Cosmic Neutronium Ingot, 64 Transfinite Processor, 1024 Book, 1024 Qubit, 128000 mB Synaptic Fluid →  (143,327,232 FE/t, 300 s)
- **Axiomatic Breach Engine**
  - 64 Absolute Infinity, 1024 Axiom Fragment, 128 Transfinite Processor, 64 Inaccessible Cardinal, 256 Formal Proof Tree, 64 Dimensional Singularity, 256 Graphene Sheet, 128 Continuum Shard, 256 Quantum Chlorophyll, 64 Mathematics Singularity, 256 Solar Statite, 2048000 mB Dark Energy, 2048000 mB Causal Solder, 256000 mB Neutrino Flux, 256000 mB Xenoplasm, 256000 mB Autocatalytic Broth, 256000 mB Superfluid Helium → 1 Axiom Key (268,435,456 FE/t, 360 s)  ⟶  64 Absolute Infinity, 1024 Axiom Fragment, 128 Transfinite Processor, 64 Temporal Singularity, 256 Engineered Enzyme, 64 Quantum Singularity, 256 Nanite Swarm, 128 Omega-One, 256 General AI Core, 64 Exotic Matter Singularity, 256 Photonic Lens, 2048000 mB Dark Energy, 2048000 mB Causal Solder, 256000 mB Strange Matter, 256000 mB Liquid Antihelium, 256000 mB Ionic Liquid, 256000 mB Superconducting Solder → 1 Axiom Key (268,435,456 FE/t, 360 s)
  - 128 Axiom Fragment, 128 Omega-One, 256 Lepton Triplet, 128 Undecidable Kernel, 256 Graphene Sheet, 128 Godel Sentence, 256 Photonic Die, 128 Russell Paradox Set, 256 Category Lattice, 128 Hilbert's Hotel Key, 1024000 mB Dark Energy, 256000 mB Nebular Gas, 256000 mB Stellar Plasma, 256000 mB Reheating Plasma, 256000 mB Strange Matter → 1 Paradox Residue (67,108,864 FE/t, 90 s)  ⟶  128 Axiom Fragment, 128 Ordinal Shard, 256 Temporal Knot, 128 Hopf Fibration, 256 Retrocausal Crystal, 128 Hilbert's Hotel Key, 256 Omega Point, 64 Inaccessible Cardinal, 256 Recombinant DNA, 128 Omega-One, 1024000 mB Dark Energy, 256000 mB Antihydrogen, 256000 mB Relativity Breakdown, 256000 mB Hypernova Plasma, 256000 mB Superfluid Helium → 1 Paradox Residue (67,108,864 FE/t, 90 s)
- **Bioreactor**
  - 12 Genome Sample, 6 Chiral Crystal, 6 Zeolite Catalyst, 6 Chronon Capacitor, 16000 mB Nutrient Broth, 8000 mB Superconducting Solder → 2 Engineered Enzyme (16,384 FE/t, 25 s)  ⟶  12 Genome Sample, 6 Chiral Crystal, 6 Zeolite Catalyst, 6 Femtoscale Inductor, 16000 mB Nutrient Broth, 8000 mB Warm Helium → 2 Engineered Enzyme (16,384 FE/t, 25 s)
  - 6 Recombinant DNA, 12 Engineered Enzyme, 6 Metal-Organic Framework, 6 Folded Protein Crystal, 32000 mB Nutrient Broth, 8000 mB Quark-Gluon Plasma → 4 Morphogenic Serum (65,536 FE/t, 25 s)  ⟶  6 Recombinant DNA, 12 Engineered Enzyme, 6 Metal-Organic Framework, 6 Folded Protein Crystal, 32000 mB Nutrient Broth, 8000 mB Tachyon Flux → 4 Morphogenic Serum (65,536 FE/t, 25 s)
  - 24 Sugar, 24 Bone Meal, 6 Zeolite Catalyst, 6 Coherent Wiring Harness, 32000 mB Water, 8000 mB Quantum-Dot Photoresist → 1000 mB Nutrient Broth (4,096 FE/t, 18.75 s)  ⟶  24 Sugar, 24 Bone Meal, 6 Zeolite Catalyst, 6 Scintillator Crystal, 32000 mB Water, 8000 mB Superconducting Solder → 1000 mB Nutrient Broth (4,096 FE/t, 18.75 s)
  - 6 Folded Protein Crystal, 12 Entangled Photon Pair, 48 Kelp, 6 Entangled Bus Bar, 16000 mB Nutrient Broth, 8000 mB Carborane Superacid → 2 Quantum Chlorophyll (131,072 FE/t, 25 s)  ⟶  6 Folded Protein Crystal, 12 Entangled Photon Pair, 48 Kelp, 6 Entangled Bus Bar, 16000 mB Nutrient Broth, 8000 mB Dark Matter → 2 Quantum Chlorophyll (131,072 FE/t, 25 s)
  - 6 Anomalous Strain Culture, 6 Engineered Enzyme, 8000 mB Nutrient Broth → 4 Morphogenic Serum (262,144 FE/t, 37.5 s)  ⟶  6 Anomalous Strain Culture, 6 Engineered Enzyme, 8000 mB Nutrient Broth → 12 Morphogenic Serum (262,144 FE/t, 37.5 s)
  - 12 Recombinant DNA, 12 Engineered Enzyme, 24 Genome Sample, 6 Gravitic Lens Element, 32000 mB Nutrient Broth, 8000 mB Quark-Gluon Plasma → 2 Stem Cell Culture (131,072 FE/t, 37.5 s)  ⟶  12 Recombinant DNA, 12 Engineered Enzyme, 24 Genome Sample, 6 Temporal Knot, 32000 mB Nutrient Broth, 8000 mB Nebular Gas → 2 Stem Cell Culture (131,072 FE/t, 37.5 s)
  - 24 Genome Sample, 6 Engineered Enzyme, 12 Moss Block, 6 Dipole Winding, 16000 mB Superfluid Helium, 8000 mB Supercritical Solvent → 2 Tardigrade Culture (65,536 FE/t, 37.5 s)  ⟶  24 Genome Sample, 6 Engineered Enzyme, 12 Moss Block, 6 Sterile Neutrino, 16000 mB Superfluid Helium, 8000 mB Chronon Gas → 2 Tardigrade Culture (65,536 FE/t, 37.5 s)
  - 12 Stem Cell Culture, 12 Nanite Swarm, 6 General AI Core, 6 Entangled Bus Bar, 32000 mB Nutrient Broth, 8000 mB Quantum-Dot Photoresist → 1 Xenobot Swarm (262,144 FE/t, 37.5 s)  ⟶  12 Stem Cell Culture, 12 Nanite Swarm, 6 General AI Core, 6 Entangled Bus Bar, 32000 mB Nutrient Broth, 8000 mB Quark-Gluon Plasma → 1 Xenobot Swarm (262,144 FE/t, 37.5 s)
  - 12 Xenogenome Sample, 24 Foam Substrate, 6 Entangled Bus Bar, 6 Quark Star Core, 32000 mB Nutrient Broth, 8000 mB Causal Solder → 1000 mB Xenoplasm (524,288 FE/t, 37.5 s)  ⟶  12 Xenogenome Sample, 24 Foam Substrate, 6 Entangled Bus Bar, 6 Bounce Core, 32000 mB Nutrient Broth, 8000 mB Dimensional Residue → 1000 mB Xenoplasm (524,288 FE/t, 37.5 s)
- **Bose-Einstein Condenser**
  - 4 Quantum Dot, 8 Cherenkov Lattice, 4000 mB Superfluid Helium → 250 mB Bose-Einstein Condensate (512 FE/t, 15 s)  ⟶  4 Quantum Dot, 4 Casimir Plate, 4000 mB Superfluid Helium → 250 mB Bose-Einstein Condensate (512 FE/t, 15 s)
  - 4 Casimir Plate, 4 Superposition Crystal, 16000 mB Superfluid Helium → 1 Cryocooler Unit, 500 mB Warm Helium (512 FE/t, 15 s)  ⟶  4 Casimir Plate, 8 Cosmic Record Fragment, 16000 mB Superfluid Helium → 1 Cryocooler Unit, 500 mB Warm Helium (512 FE/t, 15 s)
  - 8 Quantum Dot, 4 Entangled Photon Pair, 16000 mB Water → 1000 mB Quantum-Dot Photoresist (256 FE/t, 10 s)  ⟶  8 Quantum Dot, 4 Geoid Model, 16000 mB Water → 1000 mB Quantum-Dot Photoresist (256 FE/t, 10 s)
  - 8 Cherenkov Quartz, 16000 mB Warm Helium → 900 mB Superfluid Helium (384 FE/t, 10 s)  ⟶  8 Ferrite Magnet, 10289 mB Warm Helium → 7200 mB Superfluid Helium (246 FE/t, 10 s)
  - 4 Superconducting Coil, 4 Metamaterial Substrate, 8000 mB Superfluid Helium → 576 mB Superconducting Solder (1,024 FE/t, 15 s)  ⟶  4 Superconducting Coil, 4 Turbomolecular Rotor, 8000 mB Superfluid Helium → 576 mB Superconducting Solder (1,024 FE/t, 15 s)
  - 4 Photonic Neuron, 8 Cherenkov Wafer, 8000 mB Superfluid Helium → 500 mB Synaptic Fluid (4,096 FE/t, 15 s)  ⟶  4 Photonic Neuron, 16 Turbomolecular Rotor, 8000 mB Superfluid Helium → 500 mB Synaptic Fluid (4,096 FE/t, 15 s)
- **Cantor Diagonalizer**
  - 32 Mathematics Singularity, 128 Surreal Number, 64 Inaccessible Cardinal, 256 Continuum Shard, 64 Hopf Fibration, 256000 mB Dark Energy → 1 Absolute Infinity (67,108,864 FE/t, 300 s)  ⟶  32 Mathematics Singularity, 128 Surreal Number, 64 Inaccessible Cardinal, 256 Continuum Shard, 128 Transfinite Weave, 256000 mB Dark Energy → 1 Absolute Infinity (67,108,864 FE/t, 300 s)
  - 64 Power Set, 1024 Natural Number Lattice, 64 Formal Proof Tree, 32 Superintelligence Core, 64 Metastable Vacuum Lattice, 256000 mB Synaptic Fluid → 2 Aleph-Null Shard (262,144 FE/t, 75 s)  ⟶  64 Power Set, 1024 Natural Number Lattice, 64 Formal Proof Tree, 32 Superintelligence Core, 32 True Vacuum Seed, 256000 mB Synaptic Fluid → 2 Aleph-Null Shard (262,144 FE/t, 75 s)
  - 32 Tesseract Core, 64 Aleph-Null Shard, 64 Negative Mass Ingot, 32 Axiom of Choice, 64 Omega, 128000 mB Exotic Matter → 1 Banach-Tarski Sphere (524,288 FE/t, 100 s)  ⟶  32 Tesseract Core, 64 Aleph-Null Shard, 64 Negative Mass Ingot, 32 Axiom of Choice, 64 Russell Paradox Set, 128000 mB Liquid Antihelium → 1 Banach-Tarski Sphere (524,288 FE/t, 100 s)
  - 64 Aleph-Null Shard, 512 Real Continuum, 128 Power Set, 32 Omega-One, 64 Topological Defect Core, 256000 mB Dark Energy → 1 Continuum Shard (524,288 FE/t, 100 s)  ⟶  64 Aleph-Null Shard, 512 Real Continuum, 128 Power Set, 32 Omega-One, 32 Godel Sentence, 256000 mB Dark Energy → 1 Continuum Shard (524,288 FE/t, 100 s)
  - 256 Formal Proof Tree, 64 Aleph-Null Shard, 32 Superintelligence Core, 32 Riemann Zeta Crystal, 64 Theoretical Notes III, 256000 mB Synaptic Fluid → 1 Godel Sentence (524,288 FE/t, 100 s)  ⟶  256 Formal Proof Tree, 64 Aleph-Null Shard, 32 Superintelligence Core, 32 Riemann Zeta Crystal, 64 Empirical Notes I, 256000 mB Synaptic Fluid → 1 Godel Sentence (524,288 FE/t, 100 s)
  - 64 Aleph-One, 64 Topos, 1024 Cardinal Shard, 32 Hilbert's Hotel Key, 64 Hopf Fibration, 256000 mB Dark Energy → 1 Inaccessible Cardinal (8,388,608 FE/t, 200 s)  ⟶  64 Aleph-One, 64 Topos, 1024 Cardinal Shard, 32 Hilbert's Hotel Key, 64 Transfinite Processor, 256000 mB Dark Energy → 1 Inaccessible Cardinal (8,388,608 FE/t, 200 s)
  - 32 Godel Sentence, 64 Aleph-Null Shard, 128 Paradox Residue, 256 Power Set, 32 Prion Lattice, 256000 mB Chronon Gas → 1 Russell Paradox Set (524,288 FE/t, 100 s)  ⟶  32 Godel Sentence, 64 Aleph-Null Shard, 128 Paradox Residue, 256 Power Set, 32 No-Boundary Seed, 256000 mB Chronon Gas → 1 Russell Paradox Set (524,288 FE/t, 100 s)
  - 32 Octonion Crystal, 512 Real Continuum, 32 Aleph-Null Shard, 1024 Infinitesimal, 64 Boltzmann Brain, 256000 mB Dark Energy → 1 Surreal Number (1,048,576 FE/t, 125 s)  ⟶  32 Octonion Crystal, 512 Real Continuum, 32 Aleph-Null Shard, 1024 Infinitesimal, 64 Theoretical Notes II, 256000 mB Dark Energy → 1 Surreal Number (1,048,576 FE/t, 125 s)
  - 64 Cosmic Neutronium Ingot, 32 Ontological Processor, 1024 Book, 1024 Qubit, 64 Multiverse Shard, 128000 mB Quantum-Dot Photoresist →  (23,887,872 FE/t, 225 s)  ⟶  64 Cosmic Neutronium Ingot, 32 Ontological Processor, 1024 Book, 1024 Qubit, 32 Boltzmann Fluctuation, 128000 mB Hypernova Plasma →  (23,887,872 FE/t, 225 s)
- **Casimir Vacuum Press**
  - 8 Neutronium Plate, 4 Cherenkov Quartz, 8 Degenerate Matter Chunk → 1 Casimir Plate (128 FE/t, 10 s)  ⟶  8 Neutronium Plate, 4 Cherenkov Quartz, 4 Theodolite Lens → 1 Casimir Plate (128 FE/t, 10 s)
  - 4 Neutronium Plate, 4 Quantum Dot, 8 Cherenkov Quartz → 2 Metamaterial Substrate (192 FE/t, 10 s)  ⟶  4 Neutronium Plate, 4 Quantum Dot, 4 Radium Salt → 2 Metamaterial Substrate (192 FE/t, 10 s)
  - 4 Block of Neutronium, 4 Spacetime Alloy Ingot, 4 Coherent Wiring Harness, 8 Functor Bridge → 1 Degenerate Armour Plate (8,192 FE/t, 20 s)  ⟶  4 Block of Neutronium, 4 Spacetime Alloy Ingot, 4 Coherent Wiring Harness → 1 Degenerate Armour Plate (8,192 FE/t, 20 s)
  - 8 Neutronium Plate, 4 Metamaterial Sheet, 16 Cherenkov Lattice → 1 Hawking Absorber Plate (2,048 FE/t, 15 s)  ⟶  8 Neutronium Plate, 4 Metamaterial Sheet, 4 Zeolite Catalyst → 1 Hawking Absorber Plate (2,048 FE/t, 15 s)
  - 8 Cherenkov Quartz, 4 Cosmic Record Fragment → 1 Lattice Seed Crystal (128 FE/t, 10 s)  ⟶  8 Cherenkov Quartz, 8 Degenerate Matter Chunk → 1 Lattice Seed Crystal (128 FE/t, 10 s)
  - 4 Neutronium Ingot, 8 Dilithium Crystal → 1 Neutronium Plate (64 FE/t, 5 s)  ⟶  4 Neutronium Ingot, 8 Raw Pitchblende → 1 Neutronium Plate (64 FE/t, 5 s)
  - 8 Neutronium Plate, 4 Topological Insulator, 4 Photonic Logic Wafer → 1 Quadrupole Pole Tip (768 FE/t, 12 s)  ⟶  8 Neutronium Plate, 4 Topological Insulator, 16 Xenogenome Sample → 1 Quadrupole Pole Tip (768 FE/t, 12 s)
  - 4 Cherenkov Quartz, 4 Glowstone Dust, 4 Cosmic Record Fragment → 4 Quantum Dot (96 FE/t, 8 s)  ⟶  4 Cherenkov Quartz, 4 Glowstone Dust, 4 Magnetite → 4 Quantum Dot (96 FE/t, 8 s)
  - 4 Metamaterial Sheet, 4 Neutronium Plate, 8 Quantum Dot → 2 Solar Statite (2,048 FE/t, 10 s)  ⟶  4 Metamaterial Sheet, 4 Neutronium Plate, 8 Entangled Photon Pair → 2 Solar Statite (2,048 FE/t, 10 s)
- **Causality Loop Engine**
  - 64 Closed Timelike Curve, 32 Temporal Singularity, 256 Chronon Capacitor, 1024 Chronosteel Ingot, 64 Tachyon Klystron, 128 Causal Shield Lattice, 512 Pulsar Timing Crystal, 256 Chronoplastic Casing, 128 Arrow of Time Shard, 256 Temporal Knot, 32 E8 Lattice, 64 Chronosteel Singularity, 16 Continuum Shard, 2048000 mB Chronon Gas, 64000 mB Xenoplasm, 64000 mB Dark Energy, 64000 mB Liquid Antihelium → 1 Chronal Accelerator (8,388,608 FE/t, 240 s)  ⟶  64 Closed Timelike Curve, 32 Temporal Singularity, 256 Chronon Capacitor, 1024 Chronosteel Ingot, 64 Tachyon Klystron, 128 Causal Shield Lattice, 512 Pulsar Timing Crystal, 256 Chronoplastic Casing, 128 Arrow of Time Shard, 256 Temporal Knot, 32 Hypersphere Fragment, 64 Cosmic Logic Wafer, 16 Inaccessible Cardinal, 2048000 mB Chronon Gas, 64000 mB Bose-Einstein Condensate, 64000 mB Dark Matter, 64000 mB Liquid Metallic Hydrogen → 1 Chronal Accelerator (8,388,608 FE/t, 240 s)
  - 16 Novikov Crystal, 16 Wormhole Throat, 16 Superintelligence Core, 32 Supernova Remnant, 16 Cosmic Circuit Board, 32 R-Process Cluster, 16 Silicon Life Spore, 64 Maxwell's Demon Chip, 16 Virtual Quark, 1024000 mB Chronon Gas, 64000 mB Bose-Einstein Condensate, 64000 mB Antiwater, 64000 mB Exotic Matter → 1 Closed Timelike Curve, 1 Paradox Residue (524,288 FE/t, 160 s)  ⟶  16 Novikov Crystal, 16 Wormhole Throat, 16 Moduli Crystal, 64 Amethyst Singularity, 16 Virtual Photon, 32 Turing Morphogen, 16 Graviton Wisp Core, 64 Infinitesimal, 16 Hyperdimensional Logic Wafer, 1024000 mB Chronon Gas, 64000 mB Bose-Einstein Condensate, 64000 mB Nutrient Broth, 64000 mB Dimensional Residue → 1 Closed Timelike Curve, 1 Paradox Residue (524,288 FE/t, 160 s)
- **Chemical Reactor**
  - 4 Molecular Orbital Lattice, 16 Amethyst Shard, 4 Quantum Dot, 4 Cryocooler Unit, 4000 mB Supercritical Solvent → 2 Chiral Crystal (8,192 FE/t, 15 s)  ⟶  4 Molecular Orbital Lattice, 16 Amethyst Shard, 4 Quantum Dot, 8 Metamaterial Sheet, 4000 mB Supercritical Solvent → 2 Chiral Crystal (8,192 FE/t, 15 s)
  - 8 Qubit, 8 Cherenkov Quartz, 4 Zeolite Catalyst, 4 Turbomolecular Rotor, 8000 mB Supercritical Solvent → 1 Molecular Orbital Lattice (8,192 FE/t, 15 s)  ⟶  8 Qubit, 8 Cherenkov Quartz, 4 Zeolite Catalyst, 8 Cosmic Record Fragment, 8000 mB Supercritical Solvent → 1 Molecular Orbital Lattice (8,192 FE/t, 15 s)
  - 8 Turing Morphogen, 16 Chiral Crystal, 8 Noble Gas Clathrate, 4 Coherent Wiring Harness, 8 Diamond Singularity, 8000 mB Carborane Superacid → 1000 mB Primordial Soup (1,048,576 FE/t, 30 s)  ⟶  8 Turing Morphogen, 16 Chiral Crystal, 8 Noble Gas Clathrate, 4 Coherent Wiring Harness, 8000 mB Carborane Superacid → 1000 mB Primordial Soup (1,048,576 FE/t, 30 s)
  - 4 Carbon Nanotube Bundle, 8 Slime Ball, 4 Topological Circuit Board, 4 Coherent Wiring Harness, 8 Plasma Nozzle Tip, 4000 mB Ionic Liquid → 2 Self-Healing Polymer (32,768 FE/t, 15 s)  ⟶  4 Carbon Nanotube Bundle, 8 Slime Ball, 4 Topological Circuit Board, 4 Coherent Wiring Harness, 4000 mB Ionic Liquid → 2 Self-Healing Polymer (32,768 FE/t, 15 s)
  - 16 Charcoal, 4 Lattice Seed Crystal, 16000 mB Water → 1000 mB Supercritical Solvent (2,048 FE/t, 10 s)  ⟶  16 Charcoal, 8 Raw Pitchblende, 16000 mB Water → 1000 mB Supercritical Solvent (2,048 FE/t, 10 s)
  - 16 Clay Ball, 8 Quartz, 4 Cherenkov Quartz, 4 Crystalline Matrix Ingot, 4000 mB Supercritical Solvent → 2 Zeolite Catalyst (4,096 FE/t, 15 s)  ⟶  16 Clay Ball, 8 Quartz, 4 Cherenkov Quartz, 4 Entangled Photon Pair, 4000 mB Supercritical Solvent → 2 Zeolite Catalyst (4,096 FE/t, 15 s)
- **Chrono Loom**
  - 8 Negentropy Cell, 8 Chronon Capacitor, 8 Programmable Matter, 32 Cosmic Record Fragment, 16000 mB Chronon Gas → 2 Arrow of Time Shard (8,192 FE/t, 30 s)  ⟶  8 Negentropy Cell, 8 Chronon Capacitor, 8 Imaginary Unit, 32 Zeolite Catalyst, 16000 mB Chronon Gas → 2 Arrow of Time Shard (8,192 FE/t, 30 s)
  - 16 Neutronium Plate, 8 Real Continuum, 8 Chronoplastic Gasket, 8 Riemann Curvature Tensor, 32000 mB Relativity Breakdown → 2 Geodesic Thread (65,536 FE/t, 22.5 s)  ⟶  16 Neutronium Plate, 8 Real Continuum, 8 Chronoplastic Gasket, 8 Last Flash of a Black Hole, 32000 mB Relativity Breakdown → 2 Geodesic Thread (65,536 FE/t, 22.5 s)
  - 2 Temporal Knot, 1 Formal Proof Tree, 2 Lorentz Transform Crystal, 16 Raw Anti-Gold, 16 Antisilicon Shard, 1000 mB Chronon Gas → 1 Novikov Crystal, 250 mB Chronon Gas (65,536 FE/t, 40 s)  ⟶  2 Temporal Knot, 1 Formal Proof Tree, 2 Lorentz Transform Crystal, 16 Tachyon Singularity, 1000 mB Chronon Gas → 1 Novikov Crystal, 250 mB Chronon Gas (65,536 FE/t, 40 s)
  - 16 Arrow of Time Shard, 16 Tachyon Crystal, 8 Möbius Strip, 8 Claytronic Catom, 32000 mB Chronon Gas → 1 Temporal Knot (16,384 FE/t, 45 s)  ⟶  16 Arrow of Time Shard, 16 Tachyon Crystal, 8 Möbius Strip, 8 Hawking Absorber Plate, 32000 mB Chronon Gas → 1 Temporal Knot (16,384 FE/t, 45 s)
- **Chronon Field Inverter**
  - 12 Chronon Capacitor, 12 Chronosteel Ingot, 6 Programmable Matter, 16000 mB Chronon Gas, 8000 mB Dark Matter → 1 Causal Shield Lattice (16,384 FE/t, 37.5 s)  ⟶  12 Chronon Capacitor, 12 Chronosteel Ingot, 6 Hawking Absorber Plate, 16000 mB Chronon Gas, 8000 mB Nanite Slurry → 1 Causal Shield Lattice (16,384 FE/t, 37.5 s)
  - 6 Tachyon Crystal, 6 Chronon Capacitor, 6 Dark Matter Lens, 18432 mB Exotic Solder, 8000 mB Nebular Gas → 576 mB Causal Solder (65,536 FE/t, 37.5 s)  ⟶  6 Tachyon Crystal, 6 Chronon Capacitor, 6 Magnetic Monopole, 18432 mB Exotic Solder, 8000 mB Nebular Gas → 576 mB Causal Solder (65,536 FE/t, 37.5 s)
  - 6 Tachyon Crystal, 6 Qubit, 12 Photonic Circuit Board, 8000 mB Tachyon Flux, 8000 mB Quantum-Dot Photoresist → 1 Chronon Capacitor, 250 mB Chronon Gas (4,096 FE/t, 25 s)  ⟶  6 Tachyon Crystal, 6 Qubit, 12 Photonic Die, 8000 mB Tachyon Flux, 8000 mB Superfluid Helium → 1 Chronon Capacitor, 250 mB Chronon Gas (4,096 FE/t, 25 s)
  - 24 Neutronium Ingot, 12 Chronon Capacitor, 6 Rational Field, 32000 mB Chronon Gas, 8000 mB Synaptic Fluid → 1 Chronosteel Ingot (16,384 FE/t, 75 s)  ⟶  24 Neutronium Ingot, 12 Chronon Capacitor, 6 Tachyon Klystron, 32000 mB Chronon Gas, 8000 mB Bose-Einstein Condensate → 1 Chronosteel Ingot (16,384 FE/t, 75 s)
  - 6 Topological Insulator, 6 Entangled Bus Bar, 6 Complex Plane, 16000 mB Hawking Radiation, 8000 mB Neutrino Flux → 1 Hawking Emitter (8,192 FE/t, 25 s)  ⟶  6 Topological Insulator, 6 Hawking Absorber Plate, 12 Femtoscale Inductor, 16000 mB Hawking Radiation, 8000 mB Chronon Gas → 1 Hawking Emitter (8,192 FE/t, 25 s)
- **Compactification Lathe**
  - 48 Bulk Crystal, 12 Compactified Circle, 12 Superpartner Shard, 48 Attoscale Inductor, 32000 mB Exotic Matter → 2 String Coupling Crystal (131,072 FE/t, 26.25 s)  ⟶  38 Bulk Crystal, 10 Compactified Circle, 12 Superpartner Shard, 48 Sterile Neutrino, 25591 mB Exotic Matter → 16 String Coupling Crystal (104,822 FE/t, 26.25 s)
  - 48 Kaluza-Klein Shard, 24 Quantum String, 12 Superpartner Shard, 24 Gold Singularity, 32000 mB Exotic Matter → 1 Compactified Circle (32,768 FE/t, 35 s)  ⟶  48 Kaluza-Klein Shard, 24 Quantum String, 12 Flerovium Ingot, 48 Geiger Tube, 32000 mB Exotic Matter → 1 Compactified Circle (32,768 FE/t, 35 s)
  - 48 Quantum String, 24 Brane Tension Fiber, 12 Metamaterial Sheet, 12 Primordial Nucleosynthesis Ash, 64000 mB Exotic Matter → 2 D-Brane Sheet (32,768 FE/t, 35 s)  ⟶  48 Quantum String, 24 Brane Tension Fiber, 12 Metamaterial Sheet, 12 Pulsar Timing Crystal, 64000 mB Exotic Matter → 2 D-Brane Sheet (32,768 FE/t, 35 s)
  - 12 Calabi-Yau Manifold, 48 Moduli Crystal, 12 Octonion Crystal, 12 Room-Temperature Superconductor, 128000 mB Exotic Matter → 1 G₂ Manifold, 250 mB Dimensional Residue (131,072 FE/t, 70 s)  ⟶  12 Calabi-Yau Manifold, 48 Moduli Crystal, 12 Octonion Crystal, 12 Grand Unified Core, 128000 mB Exotic Matter → 1 G₂ Manifold, 250 mB Dimensional Residue (131,072 FE/t, 70 s)
  - 24 Graviton Wisp Core, 12 Compactified Circle, 24 Metric Tensor, 48 Differential Manifold, 64000 mB Hawking Radiation → 4 Graviton Crystal (65,536 FE/t, 35 s)  ⟶  24 Graviton Wisp Core, 12 Compactified Circle, 12 Flerovium Ingot, 24 Xenobot Swarm, 64000 mB Hawking Radiation → 4 Graviton Crystal (65,536 FE/t, 35 s)
  - 24 M-Brane, 12 G₂ Manifold, 48 Compactified Circle, 12 False Vacuum Membrane, 256000 mB Exotic Matter → 1 M-Theory Core, 1000 mB Dimensional Residue (1,048,576 FE/t, 140 s)  ⟶  24 M-Brane, 12 G₂ Manifold, 48 Compactified Circle, 12 Rift Anchor, 256000 mB Exotic Matter → 1 M-Theory Core, 1000 mB Dimensional Residue (1,048,576 FE/t, 140 s)
  - 48 Dilaton Crystal, 12 Quantum String, 12 Tesseract Lens, 24 Iron Singularity, 32000 mB Exotic Matter → 1 String Coupling Crystal (32,768 FE/t, 35 s)  ⟶  48 Dilaton Crystal, 12 Quantum String, 12 Wormhole Throat, 48 Sterile Neutrino, 32000 mB Exotic Matter → 1 String Coupling Crystal (32,768 FE/t, 35 s)
- **Conceptual Singularity Compressor**
  - 1024 Neural Lattice, 512 General AI Core, 128 Superintelligence Core, 512 Thought Matrix, 128 Boltzmann Brain, 1024 Holographic Matrix, 512 Bekenstein Crystal, 1024 Maxwell's Demon Chip, 512 Computronium Block, 256 Neural Organoid, 64 Omega, 128 Hyperon Crystal, 2048000 mB Synaptic Fluid, 2048000 mB Psionic Ether, 2048000 mB Nanite Slurry, 128000 mB Stellar Plasma, 128000 mB Liquid Metallic Hydrogen, 128000 mB Transcendent Plasma → 1 Cognitive Singularity (268,435,456 FE/t, 300 s)  ⟶  1024 Neural Lattice, 512 General AI Core, 128 Superintelligence Core, 512 Thought Matrix, 128 Boltzmann Brain, 1024 Holographic Matrix, 512 Bekenstein Crystal, 1024 Maxwell's Demon Chip, 512 Computronium Block, 256 Neural Organoid, 64 Penteract Lattice, 128 Quantum Field Lattice, 2048000 mB Synaptic Fluid, 2048000 mB Psionic Ether, 2048000 mB Nanite Slurry, 128000 mB Inflaton Field, 128000 mB Ionic Liquid, 128000 mB Raw Star Matter → 1 Cognitive Singularity (268,435,456 FE/t, 300 s)
  - 1024 Kaluza-Klein Shard, 1024 Dilaton Crystal, 1024 Moduli Crystal, 1024 D-Brane Sheet, 256 Penteract Lattice, 512 Calabi-Yau Manifold, 512 G₂ Manifold, 512 M-Brane, 128 M-Theory Core, 32 Axiom of Choice, 128 Superconducting Coil, 64 Russell Paradox Set, 2048000 mB Exotic Matter, 2048000 mB Inflaton Field, 2048000 mB Dimensional Residue, 128000 mB Quark-Gluon Plasma, 128000 mB Antiwater, 128000 mB Bose-Einstein Condensate → 1 Dimensional Singularity (268,435,456 FE/t, 300 s)  ⟶  1024 Kaluza-Klein Shard, 1024 Dilaton Crystal, 1024 Moduli Crystal, 1024 D-Brane Sheet, 256 Penteract Lattice, 512 Calabi-Yau Manifold, 512 G₂ Manifold, 512 M-Brane, 128 M-Theory Core, 32 Topos, 128 Wavefunction, 64 Undecidable Kernel, 2048000 mB Exotic Matter, 2048000 mB Inflaton Field, 2048000 mB Dimensional Residue, 128000 mB Neutrino Flux, 128000 mB Xenoplasm, 128000 mB Schrödinger's Limit → 1 Dimensional Singularity (268,435,456 FE/t, 300 s)
  - 1024 Omnipotence Dust, 192 Omnipotent Ingot, 64 Crystal of Transcendence, 256 Logos Thread, 64 Ontological Junction, 64 Cosmic Flux Coupler, 128 Tumour Microenvironment Organoid, 128 Qubit Die, 64 Transfinite Lattice Bolt, 128 Baryogenesis Seed, 2048000 mB Pleroma, 128000 mB Reheating Plasma, 128000 mB Quark-Gluon Plasma, 128000 mB Dark Energy → 1 Eschaton Cell (2,000,000,000 FE/t, 150 s)  ⟶  1024 Omnipotence Dust, 192 Omnipotent Ingot, 64 Crystal of Transcendence, 256 Logos Thread, 64 Quine Replicator, 128 Kaluza-Klein Singularity, 64 Undecidable Kernel, 128 Cosmic Neutronium Ingot, 32 Axiom of Choice, 128 Topological Boule, 2048000 mB Pleroma, 128000 mB Relativity Breakdown, 128000 mB Causal Solder, 128000 mB Exotic Solder → 1 Eschaton Cell (2,000,000,000 FE/t, 150 s)
  - 1024 Exotic Matter Crystal, 1024 Negative Mass Ingot, 1024 Massless Alloy, 1024 Strange Matter Pellet, 1024 Antimatter Pellet, 1024 Dark Matter Lens, 1024 Magnetic Monopole, 1024 Void Lattice Shard, 512 Cosmic String Segment, 32 Axiom of Choice, 128 Chiral Crystal, 64 Epsilon-Naught, 2048000 mB Dark Matter, 2048000 mB Exotic Matter, 2048000 mB False Vacuum, 128000 mB Psionic Ether, 128000 mB Synaptic Fluid, 128000 mB Exotic Solder → 1 Exotic Matter Singularity (268,435,456 FE/t, 300 s)  ⟶  1024 Exotic Matter Crystal, 1024 Negative Mass Ingot, 1024 Massless Alloy, 1024 Strange Matter Pellet, 1024 Antimatter Pellet, 1024 Dark Matter Lens, 1024 Magnetic Monopole, 1024 Void Lattice Shard, 512 Cosmic String Segment, 64 Omega-One, 128 Wave Packet, 64 Russell Paradox Set, 2048000 mB Dark Matter, 2048000 mB Exotic Matter, 2048000 mB False Vacuum, 128000 mB Superconducting Solder, 128000 mB Supercritical Solvent, 128000 mB Antiwater → 1 Exotic Matter Singularity (268,435,456 FE/t, 300 s)
  - 1024 Natural Number Lattice, 1024 Complex Plane, 512 Octonion Crystal, 512 Riemann Zeta Crystal, 1024 Fractal Set, 256 Epsilon-Naught, 512 Klein Bottle, 1024 Power Set, 1024 Formal Proof Tree, 64 Ordinal Shard, 128 Psionic Crystal, 64 Cardinal Shard, 2048000 mB Synaptic Fluid, 2048000 mB Dark Energy, 2048000 mB Bose-Einstein Condensate, 128000 mB False Vacuum, 128000 mB Superconducting Solder, 128000 mB Tachyon Flux → 1 Mathematics Singularity (268,435,456 FE/t, 300 s)  ⟶  1024 Natural Number Lattice, 1024 Complex Plane, 512 Octonion Crystal, 512 Riemann Zeta Crystal, 1024 Fractal Set, 256 Epsilon-Naught, 512 Klein Bottle, 1024 Power Set, 1024 Formal Proof Tree, 32 Axiom of Choice, 128 Gold Singularity, 32 Topos, 2048000 mB Synaptic Fluid, 2048000 mB Dark Energy, 2048000 mB Bose-Einstein Condensate, 128000 mB Chronon Gas, 128000 mB Hawking Radiation, 128000 mB Supercritical Solvent → 1 Mathematics Singularity (268,435,456 FE/t, 300 s)
  - 128 Omnium Ingot, 32 Transcendental Crystal, 64 Ontological Junction, 64 Cosmic Flux Coupler, 64 Immortal Cell Line, 128 Spacetime Alloy Ingot, 64 Transfinite Lattice Bolt, 128 Riemann Curvature Tensor, 64 Arbor Simulacrum, 128 Protostar Core, 1024000 mB Pleroma, 128000 mB Neutrino Flux, 128000 mB Superfluid Helium, 128000 mB Primordial Law → 4 Omnipotence Dust (1,500,000,000 FE/t, 75 s)  ⟶  128 Omnium Ingot, 32 Transcendental Crystal, 64 Cosmic Flux Coupler, 32 Immortal Cell Line, 128 Nanite Swarm, 32 Telomerase Complex, 128 Cosmic Logic Wafer, 32 Spark of Life, 128 Pulsar Timing Crystal, 64 Exotic Matter Singularity, 1024000 mB Pleroma, 128000 mB Superconducting Solder, 128000 mB Superfluid Helium, 128000 mB Warm Helium → 4 Omnipotence Dust (1,500,000,000 FE/t, 75 s)
  - 128 Omniscience Catalyst, 512 Crystalline Matrix Ingot, 1024 Cosmic Neutronium Ingot, 1024 Hypernova Alloy Ingot, 512 Transcendent Metal Ingot, 64 Ontological Junction, 64 Cosmic Flux Coupler, 64 Unified Notes III, 128 D-Brane Sheet, 32 Absolute Notes I, 128 Compactified Circle, 32 Arbor Heartwood Sample, 2048000 mB Primordial Law, 2048000 mB Transcendent Plasma, 2048000 mB Liquid Spacetime, 128000 mB Autocatalytic Broth, 128000 mB Dimensional Residue, 128000 mB Relativity Breakdown → 1 Omnium Ingot (1,073,741,824 FE/t, 750 s)  ⟶  128 Omniscience Catalyst, 512 Crystalline Matrix Ingot, 1024 Cosmic Neutronium Ingot, 1024 Hypernova Alloy Ingot, 512 Transcendent Metal Ingot, 64 Cosmic Flux Coupler, 32 Crystallised Law, 128 Temporal Knot, 64 Unified Notes I, 128 Quantum Chlorophyll, 64 Cognitive Singularity, 128 Superconducting Coil, 2048000 mB Primordial Law, 2048000 mB Transcendent Plasma, 2048000 mB Liquid Spacetime, 128000 mB Superconducting Solder, 128000 mB Hypernova Plasma, 128000 mB Residual Starlight → 1 Omnium Ingot (1,073,741,824 FE/t, 750 s)
  - 128 Omniscience Catalyst, 1024 Crystalline Matrix Ingot, 1024 Cosmic Neutronium Ingot, 1024 Hypernova Alloy Ingot, 1024 Transcendent Metal Ingot, 32 Absolute Infinity, 64 Ontological Junction, 64 Cosmic Flux Coupler, 64 Spacetime Singularity, 128 Femtoscale Transistor, 64 Unified Notes II, 128 Photonic Lens, 2048000 mB Primordial Law, 2048000 mB Transcendent Plasma, 2048000 mB Hypernova Plasma, 128000 mB Supercritical Solvent, 128000 mB Raw Star Matter, 128000 mB Antiwater → 4 Omnium Ingot (2,147,483,647 FE/t, 750 s)  ⟶  128 Omniscience Catalyst, 1024 Crystalline Matrix Ingot, 1024 Cosmic Neutronium Ingot, 1024 Hypernova Alloy Ingot, 1024 Transcendent Metal Ingot, 32 Absolute Infinity, 64 Cosmic Flux Coupler, 64 Unified Notes II, 128 Femtoscale Inductor, 32 Absolute Notes III, 128 Netherite Singularity, 32 Crystallised Law, 2048000 mB Primordial Law, 2048000 mB Transcendent Plasma, 2048000 mB Hypernova Plasma, 128000 mB Dimensional Residue, 128000 mB Residual Starlight, 128000 mB Autocatalytic Broth → 4 Omnium Ingot (2,147,483,647 FE/t, 750 s)
  - 1024 Wavefunction, 1024 Superposition Crystal, 1024 Entangled Photon Pair, 1024 Wave Packet, 1024 Spinor Lattice, 512 Quantum Field Lattice, 512 Feynman Diagram, 1024 Quantum Processor, 512 Eigenstate Lattice, 64 Russell Paradox Set, 128 Buckyball Cage, 32 Inaccessible Cardinal, 2048000 mB Bose-Einstein Condensate, 2048000 mB Superfluid Helium, 2048000 mB Quantum-Dot Photoresist, 128000 mB Liquid Spacetime, 128000 mB Superconducting Solder, 128000 mB Neutrino Flux → 1 Quantum Singularity (268,435,456 FE/t, 300 s)  ⟶  1024 Wavefunction, 1024 Superposition Crystal, 1024 Entangled Photon Pair, 1024 Wave Packet, 1024 Spinor Lattice, 512 Quantum Field Lattice, 512 Feynman Diagram, 1024 Quantum Processor, 512 Eigenstate Lattice, 64 Undecidable Kernel, 128 Stress-Energy Tensor, 64 Axiom Fragment, 2048000 mB Bose-Einstein Condensate, 2048000 mB Superfluid Helium, 2048000 mB Quantum-Dot Photoresist, 128000 mB Transcendent Plasma, 128000 mB Residual Starlight, 128000 mB Dark Matter → 1 Quantum Singularity (268,435,456 FE/t, 300 s)
  - 1024 Lorentz Transform Crystal, 1024 Minkowski Lattice, 1024 Metric Tensor, 1024 Riemann Curvature Tensor, 512 Einstein Field Core, 512 Stress-Energy Tensor, 1024 Gravitational Wave Crystal, 128 Wormhole Throat, 128 Einstein-Rosen Core, 64 Epsilon-Naught, 128 Inflaton Condensate, 64 Cardinal Shard, 2048000 mB Exotic Matter, 2048000 mB Hawking Radiation, 2048000 mB Liquid Spacetime, 128000 mB Autocatalytic Broth, 128000 mB Schrödinger's Limit, 128000 mB Relativity Breakdown → 1 Spacetime Singularity (268,435,456 FE/t, 300 s)  ⟶  1024 Lorentz Transform Crystal, 1024 Minkowski Lattice, 1024 Metric Tensor, 1024 Riemann Curvature Tensor, 512 Einstein Field Core, 512 Stress-Energy Tensor, 1024 Gravitational Wave Crystal, 128 Wormhole Throat, 128 Einstein-Rosen Core, 64 Ordinal Shard, 128 Penrose Spindle, 64 Hopf Fibration, 2048000 mB Exotic Matter, 2048000 mB Hawking Radiation, 2048000 mB Liquid Spacetime, 128000 mB Antihydrogen, 128000 mB Hypernova Plasma, 128000 mB Bose-Einstein Condensate → 1 Spacetime Singularity (268,435,456 FE/t, 300 s)
  - 1024 Up & Down Quarks, 1024 Strange & Charm Quarks, 1024 Bottom & Top Quarks, 1024 Lepton Triplet, 1024 Gauge Boson Cluster, 512 Higgs Boson Crystal, 256 Standard Model Matrix, 512 Superpartner Shard, 128 Grand Unified Core, 64 Hypersphere Fragment, 128 Neural Organoid, 64 Epsilon-Naught, 2048000 mB Quark-Gluon Plasma, 2048000 mB Strange Matter, 2048000 mB Neutrino Flux, 128000 mB Dark Energy, 128000 mB Superfluid Helium, 128000 mB Nutrient Broth → 1 Standard Model Singularity (268,435,456 FE/t, 300 s)  ⟶  1024 Up & Down Quarks, 1024 Strange & Charm Quarks, 1024 Bottom & Top Quarks, 1024 Lepton Triplet, 1024 Gauge Boson Cluster, 512 Higgs Boson Crystal, 256 Standard Model Matrix, 512 Superpartner Shard, 128 Grand Unified Core, 32 Theoretical Dossier, 128 Inertia Damper, 64 Godel Sentence, 2048000 mB Quark-Gluon Plasma, 2048000 mB Strange Matter, 2048000 mB Neutrino Flux, 128000 mB Superfluid Helium, 128000 mB Residual Starlight, 128000 mB Dark Era Haze → 1 Standard Model Singularity (268,435,456 FE/t, 300 s)
  - 512 Protostar Core, 256 Stellar Core, 1024 Red Giant Envelope, 1024 White Dwarf Core, 1024 Stellar Iron Ingot, 512 Supernova Remnant, 1024 R-Process Cluster, 1024 Magnetar Shard, 128 Quark Star Core, 128 Muonic Catalyst, 64 Undecidable Kernel, 128 Massless Alloy, 2048000 mB Stellar Plasma, 2048000 mB Nebular Gas, 2048000 mB Raw Star Matter, 128000 mB Dark Era Haze, 128000 mB Synaptic Fluid, 128000 mB Relativity Breakdown → 1 Stellar Singularity (268,435,456 FE/t, 300 s)  ⟶  512 Protostar Core, 256 Stellar Core, 1024 Red Giant Envelope, 1024 White Dwarf Core, 1024 Stellar Iron Ingot, 512 Supernova Remnant, 1024 R-Process Cluster, 1024 Magnetar Shard, 128 Quark Star Core, 128 Muonic Catalyst, 64 Russell Paradox Set, 128 Fractal Core, 2048000 mB Stellar Plasma, 2048000 mB Nebular Gas, 2048000 mB Raw Star Matter, 128000 mB Relativity Breakdown, 128000 mB Schrödinger's Limit, 128000 mB Hypernova Plasma → 1 Stellar Singularity (268,435,456 FE/t, 300 s)
  - 1024 Arrow of Time Shard, 1024 Temporal Knot, 512 Novikov Crystal, 1024 Chronon Capacitor, 1024 Tachyon Crystal, 128 Closed Timelike Curve, 1024 Paradox Residue, 256 Bootstrap Artifact, 512 Pulsar Timing Crystal, 64 Cardinal Shard, 128 Derivative Operator, 64 Hilbert's Hotel Key, 2048000 mB Chronon Gas, 2048000 mB Tachyon Flux, 2048000 mB Causal Solder, 128000 mB Liquid Spacetime, 128000 mB Synaptic Fluid, 128000 mB Residual Starlight → 1 Temporal Singularity (268,435,456 FE/t, 300 s)  ⟶  1024 Arrow of Time Shard, 1024 Temporal Knot, 512 Novikov Crystal, 1024 Chronon Capacitor, 1024 Tachyon Crystal, 128 Closed Timelike Curve, 1024 Paradox Residue, 256 Bootstrap Artifact, 512 Pulsar Timing Crystal, 64 Ordinal Shard, 128 Lorentz Transform Crystal, 32 Theoretical Dossier, 2048000 mB Chronon Gas, 2048000 mB Tachyon Flux, 2048000 mB Causal Solder, 128000 mB Dark Energy, 128000 mB Exotic Matter, 128000 mB Exotic Solder → 1 Temporal Singularity (268,435,456 FE/t, 300 s)
  - 32 Standard Model Singularity, 32 Dimensional Singularity, 32 Spacetime Singularity, 32 Quantum Singularity, 128 Grand Unified Core, 128 M-Theory Core, 256 Einstein Field Core, 64 E8 Lattice, 256 Path Integral, 64 Godel Sentence, 128 Cosmological Constant, 64 Omega-One, 2048000 mB Exotic Matter, 2048000 mB Dark Energy, 2048000 mB Quark-Gluon Plasma, 128000 mB Transcendent Plasma, 128000 mB Nebular Gas, 128000 mB Superconducting Solder → 1 Theory of Everything (536,870,912 FE/t, 600 s)  ⟶  32 Standard Model Singularity, 32 Dimensional Singularity, 32 Spacetime Singularity, 32 Quantum Singularity, 128 Grand Unified Core, 128 M-Theory Core, 256 Einstein Field Core, 64 E8 Lattice, 256 Path Integral, 64 Penteract Lattice, 128 Bootstrap Artifact, 64 Russell Paradox Set, 2048000 mB Exotic Matter, 2048000 mB Dark Energy, 2048000 mB Quark-Gluon Plasma, 128000 mB Schrödinger's Limit, 128000 mB Autocatalytic Broth, 128000 mB Quantum-Dot Photoresist → 1 Theory of Everything (536,870,912 FE/t, 600 s)
- **Convergence Engine**
  - 1 Chronosteel Ingot, 1 Topological Processor, 2 Entangled Bus Bar, 1 Assembly Matrix, 1 Recombinant DNA, 1 Magnetic Monopole, 1 Positronium Hydride → 8 Chronoplastic Gasket (16,777 FE/t, 25 s)  ⟶  1 Chronosteel Ingot, 1 Topological Processor, 2 Entangled Bus Bar, 1 Function Graph, 1 Tardigrade Culture, 1 Infinitesimal, 1 Recombinant DNA → 8 Chronoplastic Gasket (16,777 FE/t, 25 s)
  - 1 Neutronium Ingot, 1 Photonic Processor, 1 Molecular Orbital Lattice, 1 Photonic Die, 1 Maxwell's Demon Chip, 1 Photonic Logic Wafer → 8 Coherent Wiring Harness (436 FE/t, 15 s)  ⟶  1 Neutronium Ingot, 1 Photonic Processor, 1 Photonic Lens, 1 Photonic Logic Wafer, 1 Chiral Crystal, 1 Spacetime Slag → 8 Coherent Wiring Harness (436 FE/t, 15 s)
  - 1 Cosmic Neutronium Ingot, 1 Cosmic Processor, 2 Spacetime Bearing, 1 Bubble Universe, 1 Telomerase Complex, 1 Temporal Singularity, 1 Standard Model Singularity, 1 Crystallised Law, 1 Quantum Singularity → 8 Cosmic Flux Coupler (766,958 FE/t, 35 s)  ⟶  1 Cosmic Neutronium Ingot, 1 Cosmic Processor, 2 Spacetime Bearing, 1 Absolute Infinity, 1 Stellar Singularity, 1 Theory of Everything, 1 Crystallised Law, 1 Spacetime Singularity, 1 Bubble Universe → 8 Cosmic Flux Coupler (766,958 FE/t, 35 s)
  - 1 Neutronium Plate, 1 Quantum Processor, 2 Coherent Wiring Harness, 1 Topological Logic Wafer, 1 Hawking Absorber Plate, 1 Engineered Enzyme → 8 Entangled Bus Bar (2,621 FE/t, 20 s)  ⟶  1 Neutronium Plate, 1 Quantum Processor, 2 Coherent Wiring Harness, 1 Calabi-Yau Manifold, 1 Causal Shield Lattice, 1 Hawking Absorber Plate → 8 Entangled Bus Bar (2,621 FE/t, 20 s)
  - 1 Cosmic Neutronium Ingot, 1 Transfinite Processor, 2 Transfinite Lattice Bolt, 1 Spark of Life, 1 Bubble Universe, 1 Crystallised Law, 1 Primordial Ocean Simulacrum, 1 Arbor Heartwood Sample, 1 Absolute Infinity, 1 Panspermia Seed, 1 Arbor Simulacrum, 1 Telomerase Complex → 8 Omnilex Seal (274,877,906 FE/t, 50 s)  ⟶  1 Cosmic Neutronium Ingot, 1 Transfinite Processor, 2 Transfinite Lattice Bolt, 1 Crystallised Law, 1 Arbor Heartwood Sample, 1 Spark of Life, 1 Arbor Simulacrum, 1 Telomerase Complex, 1 Omnicellular Genome, 1 Bubble Universe, 1 Primordial Ocean Simulacrum, 1 Panspermia Seed → 8 Omnilex Seal (274,877,906 FE/t, 50 s)
  - 1 Cosmic Neutronium Ingot, 1 Ontological Processor, 2 Cosmic Flux Coupler, 1 Mathematics Singularity, 1 Dimensional Singularity, 1 Bubble Universe, 1 Standard Model Singularity, 1 Telomerase Complex, 1 Crystallised Law, 1 Absolute Infinity → 8 Ontological Junction (5,368,709 FE/t, 40 s)  ⟶  1 Cosmic Neutronium Ingot, 1 Ontological Processor, 2 Cosmic Flux Coupler, 1 Spacetime Singularity, 1 Spark of Life, 1 Absolute Infinity, 1 Telomerase Complex, 1 Stellar Singularity, 1 Mathematics Singularity, 1 Cognitive Singularity → 8 Ontological Junction (5,368,709 FE/t, 40 s)
  - 1 Spacetime Alloy Ingot, 1 Hyperdimensional Processor, 2 Chronoplastic Gasket, 1 Stellar Singularity, 1 Exotic Matter Singularity, 1 Theory of Everything, 1 Dimensional Singularity, 1 Mathematics Singularity → 8 Spacetime Bearing (111,848 FE/t, 30 s)  ⟶  1 Spacetime Alloy Ingot, 1 Hyperdimensional Processor, 2 Chronoplastic Gasket, 1 Quantum Singularity, 1 Standard Model Singularity, 1 Mathematics Singularity, 1 Absolute Infinity, 1 Cognitive Singularity → 8 Spacetime Bearing (111,848 FE/t, 30 s)
  - 1 Cosmic Neutronium Ingot, 1 Transfinite Processor, 2 Ontological Junction, 1 Arbor Simulacrum, 1 Primordial Ocean Simulacrum, 1 Spark of Life, 1 Panspermia Seed, 1 Crystallised Law, 1 Telomerase Complex, 1 Omnicellular Genome, 1 Bubble Universe → 8 Transfinite Lattice Bolt (38,177,487 FE/t, 45 s)  ⟶  1 Cosmic Neutronium Ingot, 1 Transfinite Processor, 2 Ontological Junction, 1 Spark of Life, 1 Panspermia Seed, 1 Arbor Heartwood Sample, 1 Bubble Universe, 1 Arbor Simulacrum, 1 Telomerase Complex, 1 Absolute Infinity, 1 Omnicellular Genome → 8 Transfinite Lattice Bolt (38,177,487 FE/t, 45 s)
- **Crafting**
  - 40 Cherenkov Quartz, 16 Redstone, 12 Glowstone Dust, 8 Obsidian, 4 Amethyst Shard, 1 Degenerate Matter Chunk → 1 Block of Cherenkov Quartz  ⟶  40 Cherenkov Quartz, 16 Glowstone Dust, 12 Obsidian, 8 Amethyst Shard, 4 Raw Pitchblende, 1 Degenerate Matter Chunk → 1 Block of Cherenkov Quartz
  - 40 Chronosteel Ingot, 16 Plasma Nozzle Tip, 12 Photonic Processor, 8 Carbon Nanotube Bundle, 4 Causal Shield Lattice, 1 Empty Set → 1 Block of Chronosteel  ⟶  40 Chronosteel Ingot, 16 Hawking Absorber Plate, 12 Plasma Nozzle Tip, 8 Photonic Processor, 4 Carbon Nanotube Bundle, 1 Empty Set → 1 Block of Chronosteel
  - 40 Cosmic Neutronium Ingot, 16 Quantum String, 12 White Hole Fragment, 8 Power Set, 4 Raw Anti-Iron, 1 Stellar Iron Ingot → 1 Block of Cosmic Neutronium  ⟶  40 Cosmic Neutronium Ingot, 16 White Hole Fragment, 12 Muonic Catalyst, 8 Power Set, 4 Raw Anti-Iron, 1 Stellar Iron Ingot → 1 Block of Cosmic Neutronium
  - 40 Dilithium Crystal, 16 Gold Ingot, 12 Lapis Lazuli, 8 Amethyst Shard, 4 Emerald, 1 Degenerate Matter Chunk → 1 Block of Dilithium  ⟶  40 Dilithium Crystal, 16 Lapis Lazuli, 12 Amethyst Shard, 8 Emerald, 4 Raw Pitchblende, 1 Degenerate Matter Chunk → 1 Block of Dilithium
  - 40 Neutronium Ingot, 16 Obsidian, 12 Amethyst Shard, 8 Prismarine Shard, 4 Degenerate Matter Chunk, 1 Cherenkov Lattice → 1 Block of Neutronium  ⟶  40 Neutronium Ingot, 16 Cherenkov Lattice, 12 Electrical Steel Lamination, 8 Telescope Mirror Blank, 4 Raw Pitchblende, 1 Prussian Blue Tablet → 1 Block of Neutronium
  - 40 Spacetime Alloy Ingot, 16 Integral Operator, 12 Red Giant Envelope, 8 Octonion Crystal, 4 Möbius Strip, 1 Self-Healing Polymer → 1 Block of Spacetime Alloy  ⟶  40 Spacetime Alloy Ingot, 16 Assembly Matrix, 12 Red Giant Envelope, 8 Octonion Crystal, 4 Möbius Strip, 1 Self-Healing Polymer → 1 Block of Spacetime Alloy
  - 40 Void Lattice Shard, 16 Entangled Photon Pair, 12 Maxwell's Demon Chip, 8 Cherenkov Boule, 4 Casimir Plate, 1 Superconducting Coil → 1 Void Lattice Block  ⟶  40 Void Lattice Shard, 16 Maxwell's Demon Chip, 12 Quantum Random Number Chip, 8 Cherenkov Boule, 4 Casimir Plate, 1 Superconducting Coil → 1 Void Lattice Block
- **Dark Energy Siphon**
  - 16 Dark Matter Lens, 16 Silicon Life Spore, 64 Muonic Catalyst, 16 String Coupling Crystal, 32 Spacetime Anchor Core, 16 Superintelligence Core, 64 Cherenkov Wafer, 16 Kaluza-Klein Singularity, 64000 mB Chronon Gas, 64000 mB Quantum-Dot Photoresist, 64000 mB Psionic Ether → 1 Cosmological Constant, 1000 mB Dark Energy (2,097,152 FE/t, 80 s)  ⟶  16 Dark Matter Lens, 16 Cosmic String Lens, 32 Supernova Remnant, 16 Graviton Wisp Core, 32 Spacetime Alloy Singularity, 16 Kaluza-Klein Shard, 64 Ferrite Magnet, 16 Dilaton Crystal, 64000 mB Neutrino Flux, 64000 mB Dimensional Residue, 64000 mB Stellar Plasma → 1 Cosmological Constant, 1000 mB Dark Energy (2,097,152 FE/t, 80 s)
  - 16 Graviton Wisp Core, 64 Quadrupole Pole Tip, 16 Virtual Monopole, 64 Tachyon Singularity, 16 Dilaton Crystal, 64 Integral Operator, 16 G₂ Manifold, 64000 mB Exotic Matter, 64000 mB Dimensional Residue, 64000 mB Superconducting Solder → 250 mB Dark Energy (1,048,576 FE/t, 40 s)  ⟶  16 Moduli Crystal, 64 Glowstone Singularity, 16 Virtual Monopole, 32 Hyperdimensional Boule, 16 Neural Organoid, 64 Category Lattice, 16 Dilaton Crystal, 64000 mB Chronon Gas, 64000 mB Schrödinger's Limit, 64000 mB Synaptic Fluid → 250 mB Dark Energy (1,048,576 FE/t, 40 s)
  - 16 De Sitter Horizon Shard, 16 Phantom Energy Cell, 25600 mB Dark Energy → 1 Cosmological Constant (2,097,152 FE/t, 160 s)  ⟶  1 De Sitter Horizon Shard, 1 Phantom Energy Cell, 1 mB Dark Energy → 8 Cosmological Constant (1 FE/t, 160 s)
- **Dark Matter Halo Collector**
  - 8 Fractured Essence, 32 Qubit, 8 Chronon Capacitor, 32 Degenerate Matter Chunk, 8 Qubit Array Wafer, 16000 mB Superfluid Helium, 16000 mB Neutrino Flux → 250 mB Dark Matter (16,384 FE/t, 30 s)  ⟶  8 Strange Matter Pellet, 32 Cosmic Record Fragment, 32 Photonic Lens, 32 Cherenkov Lattice, 8 Femtoscale Transistor, 16000 mB Quantum-Dot Photoresist, 16000 mB Bose-Einstein Condensate → 250 mB Dark Matter (16,384 FE/t, 30 s)
  - 8 Void Lattice Shard, 8 Graphene Sheet, 32 Photonic Lens, 8 Annihilation Core, 32 Genome Sample, 8 Buckyball Cage, 16000 mB Quantum-Dot Photoresist, 16000 mB Superconducting Solder → 1 Exotic Matter Crystal, 100 mB Dark Matter (32,768 FE/t, 30 s)  ⟶  8 Void Lattice Shard, 16 Coherent Wiring Harness, 32 Dilithium Crystal, 32 Photonic Lens, 32 Superposition Crystal, 8 Topological Insulator, 16000 mB Supercritical Solvent, 16000 mB Superconducting Solder → 1 Exotic Matter Crystal, 100 mB Dark Matter (32,768 FE/t, 30 s)
  - 8 Metamaterial Sheet, 8 Nanite Swarm, 32 Molecular Orbital Lattice, 8 Chronon Capacitor, 8 Void Essence, 8 Tachyon Crystal, 16000 mB Supercritical Solvent, 16000 mB Chronon Gas → 1000 mB Nebular Gas (4,096 FE/t, 30 s)  ⟶  8 Metamaterial Sheet, 8 Maxwell's Demon Chip, 32 Qubit, 8 Planck Shard, 32 Neutronium Ingot, 8 Void Lattice Shard, 16000 mB Bose-Einstein Condensate, 16000 mB Superfluid Helium → 1000 mB Nebular Gas (4,096 FE/t, 30 s)
- **Evolution Engine**
  - 12 Panspermia Seed, 96 Silicon Life Spore, 48 Neural Organoid, 768 Recombinant DNA, 24 Spacetime Bearing, 24 Chronoplastic Gasket, 24 Unified Notes I, 48 Cryocooler Unit, 24 Absolute Infinity, 48 Theoretical Notes II, 1024000 mB Xenoplasm, 32000 mB Relativity Breakdown, 32000 mB Stellar Plasma → 1 Omnicellular Genome (268,435,456 FE/t, 420 s)  ⟶  12 Panspermia Seed, 96 Silicon Life Spore, 48 Neural Organoid, 768 Recombinant DNA, 24 Spacetime Bearing, 24 Chronoplastic Gasket, 12 Absolute Dossier, 48 Anti-Iron Ingot, 24 Stellar Singularity, 48 Dilithium Singularity, 1024000 mB Xenoplasm, 32000 mB Schrödinger's Limit, 32000 mB Transcendent Plasma → 1 Omnicellular Genome (268,435,456 FE/t, 420 s)
  - 48 Stem Cell Culture, 24 Tardigrade Culture, 24 Turing Morphogen, 24 Arrow of Time Shard, 24 Spacetime Bearing, 24 Chronoplastic Gasket, 24 Topos, 48 Spacetime Alloy Ingot, 24 Standard Model Singularity, 48 Contained Anomaly Specimen, 128000 mB Xenoplasm, 32000 mB Raw Star Matter, 32000 mB Bose-Einstein Condensate → 1 Telomerase Complex (4,194,304 FE/t, 105 s)  ⟶  48 Stem Cell Culture, 24 Tardigrade Culture, 24 Turing Morphogen, 24 Arrow of Time Shard, 24 Spacetime Bearing, 24 Chronoplastic Gasket, 24 Standard Model Singularity, 48 Positronium Hydride, 12 Unified Dossier, 48 Horizon Shard, 128000 mB Xenoplasm, 32000 mB Bose-Einstein Condensate, 32000 mB Strange Matter → 1 Telomerase Complex (4,194,304 FE/t, 105 s)
- **Eye of the Event Horizon**
  - 64 Stellar Core, 64 Micro Singularity, 64 Contained Anomaly Specimen, 256 Quantum Processor, 128 Frozen Photon, 256 Biosphere Compendium, 128 Feynman Diagram, 256 Femtoscale Capacitor, 128 Closed Timelike Curve, 128 Cosmic String Lens, 64 Boltzmann Fluctuation, 2048000 mB Stellar Plasma, 2048000 mB Dark Matter, 256000 mB Chronon Gas, 256000 mB Nebular Gas, 256000 mB False Vacuum, 256000 mB Quantum-Dot Photoresist → 64 Neutronium Ingot, 64 Degenerate Matter Chunk, 8 Magnetar Shard, 2 Quark Star Core, 4 Pulsar Timing Crystal, 1 Horizon Shard, 8000 mB Liquid Metallic Hydrogen, 4000 mB Hawking Radiation, 2000 mB Raw Star Matter (16,777,216 FE/t, 180 s)  ⟶  64 Stellar Core, 64 Micro Singularity, 128 Eigenstate Lattice, 256 Cosmic String Segment, 128 De Sitter Horizon Shard, 128 G₂ Manifold, 64 True Vacuum Seed, 256 Möbius Strip, 128 Stress-Energy Tensor, 256 Photonic Circuit Board, 128 Primordial Ejecta, 2048000 mB Stellar Plasma, 2048000 mB Dark Matter, 256000 mB Exotic Solder, 256000 mB Relativity Breakdown, 256000 mB Dimensional Residue, 256000 mB Schrödinger's Limit → 64 Neutronium Ingot, 64 Degenerate Matter Chunk, 8 Magnetar Shard, 2 Quark Star Core, 4 Pulsar Timing Crystal, 1 Horizon Shard, 8000 mB Liquid Metallic Hydrogen, 4000 mB Hawking Radiation, 2000 mB Raw Star Matter (16,777,216 FE/t, 180 s)
- **Femto Fabricator**
  - 12 Phase-Locked Stabiliser, 24 General AI Core, 48 Topological Processor, 48 Error-Correction Lattice, 24 Dimensional Stabiliser Matrix, 8000 mB Psionic Ether → 1 Adaptive Feedback Core (1,048,576 FE/t, 75 s)  ⟶  12 Phase-Locked Stabiliser, 24 General AI Core, 48 Topological Processor, 48 Error-Correction Lattice, 24 Dimensional Stabiliser Matrix, 8000 mB Antihydrogen → 1 Adaptive Feedback Core (1,048,576 FE/t, 75 s)
  - 24 Femtoscale Capacitor, 6 Higgs Field Capacitor, 6 Electride Crystal, 24 Cherenkov Boule, 4608 mB Exotic Solder, 8000 mB Supercritical Solvent → 8 Attoscale Capacitor (65,536 FE/t, 37.5 s)  ⟶  24 Femtoscale Capacitor, 6 Higgs Field Capacitor, 6 Noble Gas Clathrate, 6 Coherent Wiring Harness, 4608 mB Exotic Solder, 8000 mB Nebular Gas → 8 Attoscale Capacitor (65,536 FE/t, 37.5 s)
  - 24 Femtoscale Inductor, 6 Magnetic Monopole, 6 Infinitesimal, 24 Superposition Crystal, 4608 mB Exotic Solder, 8000 mB Quantum-Dot Photoresist → 8 Attoscale Inductor (65,536 FE/t, 37.5 s)  ⟶  24 Femtoscale Inductor, 6 Magnetic Monopole, 6 Electride Crystal, 12 Pixel Tracker Module, 4608 mB Exotic Solder, 8000 mB Ionic Liquid → 8 Attoscale Inductor (65,536 FE/t, 37.5 s)
  - 24 Femtoscale Transistor, 6 Superposition Crystal, 6 Topological Insulator, 6 Topological Die, 24 Cherenkov Boule, 4608 mB Exotic Solder, 8000 mB Tachyon Flux → 8 Attoscale Transistor (65,536 FE/t, 37.5 s)  ⟶  24 Femtoscale Transistor, 6 Superposition Crystal, 6 Topological Insulator, 6 Magnetic Monopole, 24 Cherenkov Wafer, 4608 mB Exotic Solder, 8000 mB Warm Helium → 8 Attoscale Transistor (65,536 FE/t, 37.5 s)
  - 6 Casimir Plate, 12 Neutronium Plate, 6 Axiom Schema, 24 Qubit, 4608 mB Superconducting Solder, 8000 mB Quantum-Dot Photoresist → 8 Femtoscale Capacitor (2,048 FE/t, 18.75 s)  ⟶  6 Casimir Plate, 12 Neutronium Plate, 24 Zeolite Catalyst, 24 Dilithium Crystal, 4608 mB Superconducting Solder, 8000 mB Quantum-Dot Photoresist → 8 Femtoscale Capacitor (2,048 FE/t, 18.75 s)
  - 12 Superconducting Coil, 6 Neutronium Ingot, 24 Zeolite Catalyst, 24 Cherenkov Lattice, 4608 mB Superconducting Solder, 8000 mB Tachyon Flux → 8 Femtoscale Inductor (2,048 FE/t, 18.75 s)  ⟶  12 Superconducting Coil, 6 Neutronium Ingot, 24 Cryocooler Unit, 24 Electrical Steel Lamination, 4608 mB Superconducting Solder, 8000 mB Supercritical Solvent → 8 Femtoscale Inductor (2,048 FE/t, 18.75 s)
  - 6 Qubit, 24 Quantum Dot, 24 Superposition Crystal, 24 Dilithium Crystal, 8000 mB Quantum-Dot Photoresist, 8000 mB Superfluid Helium → 8 Femtoscale Transistor (2,048 FE/t, 18.75 s)  ⟶  6 Qubit, 24 Quantum Dot, 6 Photonic Die, 24 Cherenkov Quartz, 8000 mB Quantum-Dot Photoresist, 8000 mB Warm Helium → 8 Femtoscale Transistor (2,048 FE/t, 18.75 s)
  - 6 Cherenkov Wafer, 12 Qubit, 24 Quantum Dot, 24 Zeolite Catalyst, 24 Crystalline Matrix Ingot, 4608 mB Superconducting Solder, 8000 mB Supercritical Solvent → 2 Pixel Tracker Module (2,048 FE/t, 18.75 s)  ⟶  6 Cherenkov Wafer, 12 Qubit, 24 Quantum Dot, 24 Quantum Random Number Chip, 24 Lattice Seed Crystal, 4608 mB Superconducting Solder, 8000 mB Superfluid Helium → 2 Pixel Tracker Module (2,048 FE/t, 18.75 s)
  - 12 Neutronium Plate, 6 Tachyon Crystal, 6 Topological Insulator, 6 Metamaterial Sheet, 6 Fractured Essence, 9216 mB Superconducting Solder, 8000 mB Tachyon Flux → 1 Plasma Nozzle Tip (4,096 FE/t, 25 s)  ⟶  12 Neutronium Plate, 6 Tachyon Crystal, 6 Topological Insulator, 6 Metamaterial Sheet, 6 Void Lattice Shard, 9216 mB Superconducting Solder, 8000 mB Quantum-Dot Photoresist → 1 Plasma Nozzle Tip (4,096 FE/t, 25 s)
  - 6 Photonic Circuit Board, 12 Qubit Die, 12 Photonic Processor, 24 Femtoscale Transistor, 12 Molecular Orbital Lattice, 4608 mB Superconducting Solder, 8000 mB Tachyon Flux → 2 Quantum Processor (8,192 FE/t, 37.5 s)  ⟶  6 Photonic Circuit Board, 12 Qubit Die, 12 Photonic Processor, 24 Femtoscale Transistor, 12 Photonic Logic Wafer, 4608 mB Superconducting Solder, 8000 mB Bose-Einstein Condensate → 2 Quantum Processor (8,192 FE/t, 37.5 s)
  - 6 Topological Circuit Board, 12 Topological Die, 12 Quantum Processor, 48 Femtoscale Capacitor, 6 Penrose Spindle, 4608 mB Exotic Solder, 8000 mB Nebular Gas → 2 Topological Processor (131,072 FE/t, 50 s)  ⟶  6 Topological Circuit Board, 12 Topological Die, 12 Quantum Processor, 48 Femtoscale Capacitor, 6 Arrow of Time Shard, 4608 mB Exotic Solder, 8000 mB Tachyon Flux → 2 Topological Processor (131,072 FE/t, 50 s)
- **Femtoscale Assembly Line**
  - 64 Hyperdimensional Processor, 32 Plasma Crucible Casing, 32 Precision Assembly Casing, 16 Containment Field Glass, 16 Evolution Engine, 16 Universe Seed, 256 Attoscale Capacitor, 256 Attoscale Inductor, 16 Chronoplastic Gasket, 16 Entangled Bus Bar, 16 Standard Model Singularity, 32 Inflaton Condensate, 16 Theoretical Notes I, 147456 mB Causal Solder, 256000 mB Superfluid Helium, 16000 mB Chronon Gas, 16000 mB Supercritical Solvent → 1 Abiogenesis Crucible (4,194,304 FE/t, 180 s)  ⟶  64 Hyperdimensional Processor, 32 Plasma Crucible Casing, 32 Precision Assembly Casing, 16 Containment Field Glass, 16 Evolution Engine, 16 Universe Seed, 256 Attoscale Capacitor, 256 Attoscale Inductor, 16 Chronoplastic Gasket, 16 Entangled Bus Bar, 16 Theory of Everything, 32 Hawking Absorber Plate, 16 Stellar Singularity, 147456 mB Causal Solder, 256000 mB Superfluid Helium, 16000 mB Strange Matter, 16000 mB False Vacuum → 1 Abiogenesis Crucible (4,194,304 FE/t, 180 s)
  - 16 Absolute Notes I, 16 Absolute Notes II, 16 Absolute Notes III, 8 Unified Dossier, 16 Chronoplastic Gasket, 16 Entangled Bus Bar, 8 Crystallised Law, 32 Antiquark Shard, 16 Topos, 512000 mB Primordial Law, 16000 mB Carborane Superacid, 16000 mB Hypernova Plasma → 1 Absolute Dossier (134,217,728 FE/t, 120 s)  ⟶  16 Absolute Notes I, 16 Absolute Notes II, 16 Absolute Notes III, 8 Unified Dossier, 16 Chronoplastic Gasket, 16 Entangled Bus Bar, 16 Axiom of Choice, 32 Minkowski Lattice, 16 Unified Notes III, 512000 mB Primordial Law, 16000 mB Autocatalytic Broth, 16000 mB Stellar Plasma → 1 Absolute Dossier (134,217,728 FE/t, 120 s)
  - 32 Axiom Fragment, 32 Paradox Residue, 32 Axiomatic Field Emitter, 32 White Hole Fragment, 32 Banach-Tarski Sphere, 32 Godel Sentence, 32 Omega-One, 32 Penteract Lattice, 32 Transfinite Processor, 16 Chronoplastic Gasket, 16 Entangled Bus Bar, 8 Unified Dossier, 32 Chronosteel Singularity, 16 Stellar Singularity, 256000 mB Primordial Law, 16000 mB Antiwater, 16000 mB Inflaton Field → 1 Absolute Notes I (67,108,864 FE/t, 105 s)  ⟶  32 Axiom Fragment, 32 Paradox Residue, 32 Axiomatic Field Emitter, 32 White Hole Fragment, 32 Banach-Tarski Sphere, 32 Godel Sentence, 32 Omega-One, 32 Penteract Lattice, 32 Transfinite Processor, 16 Chronoplastic Gasket, 16 Entangled Bus Bar, 16 Quantum Singularity, 32 Void Lattice Singularity, 16 Inaccessible Cardinal, 256000 mB Primordial Law, 16000 mB Ionic Liquid, 16000 mB Chronon Gas → 1 Absolute Notes I (67,108,864 FE/t, 105 s)
  - 32 Crystallised Law, 32 Theory of Everything, 32 Neutronium Ingot, 32 Aleph-One, 32 E8 Lattice, 32 Hopf Fibration, 32 Omega, 32 Power Set, 16 Chronoplastic Gasket, 16 Entangled Bus Bar, 16 Standard Model Singularity, 32 Metastable Vacuum Lattice, 8 Absolute Infinity, 256000 mB Primordial Law, 16000 mB Liquid Antihelium, 16000 mB Exotic Solder → 1 Absolute Notes II (67,108,864 FE/t, 105 s)  ⟶  32 Crystallised Law, 32 Theory of Everything, 32 Neutronium Ingot, 32 Aleph-One, 32 E8 Lattice, 32 Hopf Fibration, 32 Omega, 32 Power Set, 16 Chronoplastic Gasket, 16 Entangled Bus Bar, 8 Unified Notes I, 32 Qubit Die, 8 Absolute Infinity, 256000 mB Primordial Law, 16000 mB Nanite Slurry, 16000 mB Synaptic Fluid → 1 Absolute Notes II (67,108,864 FE/t, 105 s)
  - 32 Axiom Key, 32 Absolute Infinity, 32 Pocket Universe, 32 Axiom Schema, 32 Epsilon-Naught, 32 Inaccessible Cardinal, 32 Ordinal Shard, 32 Russell Paradox Set, 16 Chronoplastic Gasket, 16 Entangled Bus Bar, 16 Aleph-One, 32 Quantum Dot, 16 Stellar Singularity, 256000 mB Primordial Law, 16000 mB Warm Helium, 16000 mB Exotic Solder → 1 Absolute Notes III (67,108,864 FE/t, 105 s)  ⟶  32 Axiom Key, 32 Absolute Infinity, 32 Pocket Universe, 32 Axiom Schema, 32 Epsilon-Naught, 32 Inaccessible Cardinal, 32 Ordinal Shard, 32 Russell Paradox Set, 16 Chronoplastic Gasket, 16 Entangled Bus Bar, 16 Continuum Shard, 32 Photonic Processor, 16 Standard Model Singularity, 256000 mB Primordial Law, 16000 mB Antihydrogen, 16000 mB Strange Matter → 1 Absolute Notes III (67,108,864 FE/t, 105 s)
  - 32 Containment Field Glass, 32 Causal Shield Lattice, 32 Chronoplastic Casing, 16 Dimensional Stabiliser Matrix, 48 Hyperdimensional Processor, 16 Empty Containment Cell, 256 Attoscale Capacitor, 256 Attoscale Inductor, 16 Chronoplastic Gasket, 16 Entangled Bus Bar, 8 Rift Anchor, 32 Xenobot Swarm, 8 Kerr Metric Shard, 147456 mB Causal Solder, 256000 mB Superfluid Helium, 16000 mB Supercritical Solvent, 16000 mB Exotic Matter → 1 Anomaly Containment Ward (4,194,304 FE/t, 180 s)  ⟶  32 Containment Field Glass, 32 Causal Shield Lattice, 32 Chronoplastic Casing, 16 Dimensional Stabiliser Matrix, 48 Hyperdimensional Processor, 16 Empty Containment Cell, 256 Attoscale Capacitor, 256 Attoscale Inductor, 16 Chronoplastic Gasket, 16 Entangled Bus Bar, 8 Hyperdimensional Pearl, 32 Radium Salt, 8 Virtual Graviton, 147456 mB Causal Solder, 256000 mB Superfluid Helium, 16000 mB Liquid Metallic Hydrogen, 16000 mB Antihydrogen → 1 Anomaly Containment Ward (4,194,304 FE/t, 180 s)
  - 64 Reality Lattice Casing, 32 Gravitic Lens Element, 48 Hyperdimensional Processor, 16 Error-Correction Lattice, 16 Negentropy Cell, 256 Attoscale Capacitor, 256 Attoscale Inductor, 16 Chronoplastic Gasket, 16 Entangled Bus Bar, 8 Theoretical Notes II, 32 Empty Set, 8 Eigenstate Lattice, 147456 mB Causal Solder, 256000 mB Superfluid Helium, 16000 mB Schrödinger's Limit, 16000 mB Synaptic Fluid → 1 Anomaly Resolution Engine (4,194,304 FE/t, 180 s)  ⟶  64 Reality Lattice Casing, 32 Gravitic Lens Element, 48 Hyperdimensional Processor, 16 Error-Correction Lattice, 16 Negentropy Cell, 256 Attoscale Capacitor, 256 Attoscale Inductor, 16 Chronoplastic Gasket, 16 Entangled Bus Bar, 8 Feynman Diagram, 16 Virtual Monopole, 8 Boltzmann Brain, 147456 mB Causal Solder, 256000 mB Superfluid Helium, 16000 mB Superconducting Solder, 16000 mB Supercritical Solvent → 1 Anomaly Resolution Engine (4,194,304 FE/t, 180 s)
  - 16 Photonic Processor, 16 Metamaterial Sheet, 16 Chronosteel Ingot, 8 Quantum Processor, 8 Engineered Enzyme, 16 Photonic Die, 8 Exotic Matter Crystal, 8 Graphene Sheet, 8 Qubit Array Wafer, 16000 mB Ionic Liquid, 16000 mB Nanite Slurry → 4 Assembly Matrix (65,536 FE/t, 30 s)  ⟶  16 Photonic Processor, 16 Metamaterial Sheet, 16 Chronosteel Ingot, 8 Quantum Processor, 8 Topological Die, 32 Cherenkov Lattice, 8 Proof Fragment, 32 Raw Pitchblende, 8 Pixel Tracker Module, 16000 mB Dark Matter, 16000 mB Bose-Einstein Condensate → 4 Assembly Matrix (65,536 FE/t, 30 s)
  - 8 Field Calibrator, 8 Superintelligence Core, 16 Cosmic Processor, 128 Attoscale Capacitor, 128 Attoscale Transistor, 128 Attoscale Inductor, 16 Chronoplastic Gasket, 16 Entangled Bus Bar, 8 Kaluza-Klein Shard, 32 Dilithium Crystal, 8 Wavefunction, 73728 mB Causal Solder, 128000 mB Superfluid Helium, 16000 mB Ionic Liquid, 16000 mB Dimensional Residue → 1 Autonomous Calibrator (16,777,216 FE/t, 180 s)  ⟶  8 Field Calibrator, 8 Superintelligence Core, 16 Cosmic Processor, 128 Attoscale Capacitor, 128 Attoscale Transistor, 128 Attoscale Inductor, 16 Chronoplastic Gasket, 16 Entangled Bus Bar, 8 Graviton Wisp Core, 32 Bottom & Top Quarks, 8 M-Brane, 73728 mB Causal Solder, 128000 mB Superfluid Helium, 16000 mB Nanite Slurry, 16000 mB Carborane Superacid → 1 Autonomous Calibrator (16,777,216 FE/t, 180 s)
  - 96 Ontological Processor, 32 Reality Anchor, 16 Godel Sentence, 16 Transfinite Casing, 16 Banach-Tarski Sphere, 512 Attoscale Capacitor, 512 Attoscale Inductor, 16 Chronoplastic Gasket, 16 Entangled Bus Bar, 16 Russell Paradox Set, 32 Source Code Fragment, 16 Cardinal Shard, 294912 mB Causal Solder, 256000 mB Superfluid Helium, 16000 mB Hawking Radiation, 16000 mB Antihydrogen → 1 Axiomatic Breach Engine (67,108,864 FE/t, 180 s)  ⟶  96 Ontological Processor, 32 Reality Anchor, 16 Godel Sentence, 16 Transfinite Casing, 16 Banach-Tarski Sphere, 512 Attoscale Capacitor, 512 Attoscale Inductor, 16 Chronoplastic Gasket, 16 Entangled Bus Bar, 16 Hopf Fibration, 32 Boltzmann Fluctuation, 16 Omega Point, 294912 mB Causal Solder, 256000 mB Superfluid Helium, 16000 mB Xenoplasm, 16000 mB Hawking Radiation → 1 Axiomatic Breach Engine (67,108,864 FE/t, 180 s)
  - 8 Graviton Field Emitter, 16 Axiom Fragment, 16 Horizon Shard, 16 Transcendent Metal Ingot, 8 Transfinite Processor, 16 Chronoplastic Gasket, 16 Entangled Bus Bar, 16 Standard Model Singularity, 32 Surreal Number, 16 Banach-Tarski Sphere, 73728 mB Causal Solder, 32000 mB Primordial Law, 16000 mB Schrödinger's Limit, 16000 mB Tachyon Flux → 1 Axiomatic Field Emitter (16,777,216 FE/t, 90 s)  ⟶  8 Graviton Field Emitter, 16 Axiom Fragment, 16 Horizon Shard, 16 Transcendent Metal Ingot, 8 Transfinite Processor, 16 Chronoplastic Gasket, 16 Entangled Bus Bar, 8 Unified Dossier, 32 Rift Anchor, 16 Spacetime Singularity, 73728 mB Causal Solder, 32000 mB Primordial Law, 16000 mB Chronon Gas, 16000 mB Nanite Slurry → 1 Axiomatic Field Emitter (16,777,216 FE/t, 90 s)
  - 96 Hyperdimensional Processor, 32 Tachyon Crystal, 32 Pulsar Timing Crystal, 16 Chronoplastic Casing, 256 Attoscale Capacitor, 256 Attoscale Inductor, 16 Chronoplastic Gasket, 16 Entangled Bus Bar, 8 D-Brane Sheet, 32 Quantum Field Lattice, 8 Hyperdimensional Pearl, 147456 mB Causal Solder, 256000 mB Superfluid Helium, 16000 mB Dimensional Residue, 16000 mB Nebular Gas → 1 Causality Loop Engine (4,194,304 FE/t, 180 s)  ⟶  96 Hyperdimensional Processor, 32 Tachyon Crystal, 32 Pulsar Timing Crystal, 16 Chronoplastic Casing, 256 Attoscale Capacitor, 256 Attoscale Inductor, 16 Chronoplastic Gasket, 16 Entangled Bus Bar, 8 Moduli Crystal, 16 Higgs Boson Crystal, 8 Hyperdimensional Logic Wafer, 147456 mB Causal Solder, 256000 mB Superfluid Helium, 16000 mB Synaptic Fluid, 16000 mB Ionic Liquid → 1 Causality Loop Engine (4,194,304 FE/t, 180 s)
  - 96 Cosmic Processor, 32 Aleph-Null Shard, 16 Topos, 16 Kugelblitz Chamber, 16 Conceptual Containment Casing, 384 Attoscale Capacitor, 384 Attoscale Inductor, 16 Chronoplastic Gasket, 16 Entangled Bus Bar, 16 Russell Paradox Set, 32 Dilaton Crystal, 16 Surreal Number, 221184 mB Causal Solder, 256000 mB Superfluid Helium, 16000 mB Stellar Plasma, 16000 mB Residual Starlight → 1 Conceptual Singularity Compressor (16,777,216 FE/t, 180 s)  ⟶  96 Cosmic Processor, 32 Aleph-Null Shard, 16 Topos, 16 Kugelblitz Chamber, 16 Conceptual Containment Casing, 384 Attoscale Capacitor, 384 Attoscale Inductor, 16 Chronoplastic Gasket, 16 Entangled Bus Bar, 16 Cardinal Shard, 32 Photonic Neuron, 16 Ordinal Shard, 221184 mB Causal Solder, 256000 mB Superfluid Helium, 16000 mB Nanite Slurry, 16000 mB Synaptic Fluid → 1 Conceptual Singularity Compressor (16,777,216 FE/t, 180 s)
  - 8 Spacetime Alloy Coil Block, 64 Cosmic Neutronium Ingot, 32 Pentaquark Shard, 8 Hyperdimensional Processor, 32 Cryocooler Unit, 16 Chronoplastic Gasket, 16 Entangled Bus Bar, 8 Kaluza-Klein Shard, 32 Antiquark Shard, 8 D-Brane Sheet, 73728 mB Exotic Solder, 128000 mB Superfluid Helium, 16000 mB Strange Matter, 16000 mB Quantum-Dot Photoresist → 1 Cosmic Neutronium Coil Block (1,048,576 FE/t, 60 s)  ⟶  8 Spacetime Alloy Coil Block, 64 Cosmic Neutronium Ingot, 32 Pentaquark Shard, 8 Hyperdimensional Processor, 32 Cryocooler Unit, 16 Chronoplastic Gasket, 16 Entangled Bus Bar, 8 Hyperdimensional Logic Wafer, 32 Antisilicon Shard, 8 Neural Organoid, 73728 mB Exotic Solder, 128000 mB Superfluid Helium, 16000 mB Liquid Metallic Hydrogen, 16000 mB Carborane Superacid → 1 Cosmic Neutronium Coil Block (1,048,576 FE/t, 60 s)
  - 8 Cosmic Circuit Board, 32 Cosmic Die, 16 Hyperdimensional Processor, 128 Attoscale Capacitor, 128 Attoscale Transistor, 128 Attoscale Inductor, 16 Graviton Crystal, 16 Chronoplastic Gasket, 16 Entangled Bus Bar, 8 Graviton Wisp Core, 32 Quantum Field Lattice, 8 Kaluza-Klein Shard, 36864 mB Causal Solder, 128000 mB Superfluid Helium, 64000 mB Dark Matter, 16000 mB Warm Helium, 16000 mB Antiwater → 2 Cosmic Processor (8,388,608 FE/t, 120 s)  ⟶  8 Cosmic Circuit Board, 32 Cosmic Die, 16 Hyperdimensional Processor, 128 Attoscale Capacitor, 128 Attoscale Transistor, 128 Attoscale Inductor, 16 Graviton Crystal, 16 Chronoplastic Gasket, 16 Entangled Bus Bar, 8 Silicon Life Spore, 32 Chronosteel Singularity, 8 Brane Tension Fiber, 36864 mB Causal Solder, 128000 mB Superfluid Helium, 64000 mB Dark Matter, 16000 mB Nanite Slurry, 16000 mB Strange Matter → 2 Cosmic Processor (8,388,608 FE/t, 120 s)
  - 64 Dark Matter Lens, 32 Field Emitter, 64 Hyperdimensional Processor, 16 Event Horizon Casing, 256 Attoscale Capacitor, 256 Attoscale Inductor, 16 Chronoplastic Gasket, 16 Entangled Bus Bar, 8 Hyperdimensional Die, 32 Emerald Singularity, 8 Cosmic String Lens, 147456 mB Causal Solder, 256000 mB Superfluid Helium, 16000 mB Tachyon Flux, 16000 mB Liquid Metallic Hydrogen → 1 Dark Energy Siphon (4,194,304 FE/t, 180 s)  ⟶  64 Dark Matter Lens, 32 Field Emitter, 64 Hyperdimensional Processor, 16 Event Horizon Casing, 256 Attoscale Capacitor, 256 Attoscale Inductor, 16 Chronoplastic Gasket, 16 Entangled Bus Bar, 8 D-Brane Sheet, 16 Standard Model Matrix, 8 Virtual Monopole, 147456 mB Causal Solder, 256000 mB Superfluid Helium, 16000 mB Liquid Antihelium, 16000 mB Dimensional Residue → 1 Dark Energy Siphon (4,194,304 FE/t, 180 s)
  - 96 Hyperdimensional Processor, 64 Dyson Swarm Satellite, 16 Stellar Core, 256 Attoscale Capacitor, 256 Attoscale Inductor, 16 Chronoplastic Gasket, 16 Entangled Bus Bar, 8 Superintelligence Core, 32 Natural Number Lattice, 8 Hyperdimensional Die, 147456 mB Causal Solder, 256000 mB Superfluid Helium, 16000 mB Dimensional Residue, 16000 mB Nutrient Broth → 1 Dyson Array (4,194,304 FE/t, 180 s)  ⟶  96 Hyperdimensional Processor, 64 Dyson Swarm Satellite, 16 Stellar Core, 256 Attoscale Capacitor, 256 Attoscale Inductor, 16 Chronoplastic Gasket, 16 Entangled Bus Bar, 8 False Vacuum Membrane, 32 Annihilation Core, 8 Moduli Crystal, 147456 mB Causal Solder, 256000 mB Superfluid Helium, 16000 mB Inflaton Field, 16000 mB Bose-Einstein Condensate → 1 Dyson Array (4,194,304 FE/t, 180 s)
  - 16 Empirical Notes I, 16 Empirical Notes II, 16 Empirical Notes III, 16 Chronoplastic Gasket, 16 Entangled Bus Bar, 8 Kerr Metric Shard, 16 Quark Star Core, 8 Wavefunction, 512000 mB Superfluid Helium, 16000 mB Dark Energy, 16000 mB Ionic Liquid → 1 Empirical Dossier (2,097,152 FE/t, 75 s)  ⟶  16 Empirical Notes I, 16 Empirical Notes II, 16 Empirical Notes III, 16 Chronoplastic Gasket, 16 Entangled Bus Bar, 8 Christoffel Symbol, 32 Massless Alloy, 8 Geodesic Thread, 512000 mB Superfluid Helium, 16000 mB Chronon Gas, 16000 mB Nebular Gas → 1 Empirical Dossier (2,097,152 FE/t, 75 s)
  - 32 Arrow of Time Shard, 32 Kugelblitz Seed, 32 Hyperon Crystal, 32 Graviton Field Emitter, 32 Stellar Core, 32 Magnetic Monopole, 32 Fractal Dust, 32 Femtoscale Inductor, 32 Hyperdimensional Wafer, 16 Chronoplastic Gasket, 16 Entangled Bus Bar, 8 Kaluza-Klein Shard, 32 Computronium Singularity, 8 Rift Anchor, 256000 mB Superfluid Helium, 16000 mB Synaptic Fluid, 16000 mB Chronon Gas → 1 Empirical Notes I (1,048,576 FE/t, 60 s)  ⟶  32 Arrow of Time Shard, 32 Kugelblitz Seed, 32 Hyperon Crystal, 32 Graviton Field Emitter, 32 Stellar Core, 32 Magnetic Monopole, 32 Fractal Dust, 32 Femtoscale Inductor, 32 Hyperdimensional Wafer, 16 Chronoplastic Gasket, 16 Entangled Bus Bar, 8 Moduli Crystal, 32 Geiger Tube, 8 Hyperdimensional Die, 256000 mB Superfluid Helium, 16000 mB Tachyon Flux, 16000 mB Ionic Liquid → 1 Empirical Notes I (1,048,576 FE/t, 60 s)
  - 32 Psionic Crystal, 32 Attoscale Capacitor, 32 Strange & Charm Quarks, 32 Quaternion Crystal, 32 Complex Plane, 32 Tachyon Klystron, 32 Topological Boule, 32 Quantum Processor, 32 Photonic Logic Wafer, 16 Chronoplastic Gasket, 16 Entangled Bus Bar, 8 Wave Packet, 8 Pentaquark Shard, 8 Antihelium Nucleus, 256000 mB Superfluid Helium, 16000 mB Nutrient Broth, 16000 mB Carborane Superacid → 1 Empirical Notes II (1,048,576 FE/t, 60 s)  ⟶  32 Psionic Crystal, 32 Attoscale Capacitor, 32 Strange & Charm Quarks, 32 Quaternion Crystal, 32 Complex Plane, 32 Tachyon Klystron, 32 Topological Boule, 32 Quantum Processor, 32 Photonic Logic Wafer, 16 Chronoplastic Gasket, 16 Entangled Bus Bar, 8 Self-Healing Polymer, 8 Pentaquark Shard, 8 Coal Singularity, 256000 mB Superfluid Helium, 16000 mB Bose-Einstein Condensate, 16000 mB Stellar Plasma → 1 Empirical Notes II (1,048,576 FE/t, 60 s)
  - 32 Gravitic Lens Element, 32 Bottom & Top Quarks, 32 Up & Down Quarks, 32 Minkowski Lattice, 32 Fractal Set, 32 Photonic Neuron, 32 Void Lattice Shard, 32 Integer Ring, 32 Casimir Plate, 16 Chronoplastic Gasket, 16 Entangled Bus Bar, 8 Red Giant Envelope, 16 Recombinant DNA, 8 Morphogenic Serum, 256000 mB Superfluid Helium, 16000 mB Warm Helium, 16000 mB Exotic Solder → 1 Empirical Notes III (1,048,576 FE/t, 60 s)  ⟶  32 Gravitic Lens Element, 32 Bottom & Top Quarks, 32 Up & Down Quarks, 32 Minkowski Lattice, 32 Fractal Set, 32 Photonic Neuron, 32 Void Lattice Shard, 32 Integer Ring, 32 Casimir Plate, 16 Chronoplastic Gasket, 16 Entangled Bus Bar, 8 Blue Ice Singularity, 32 Entangled Photon Pair, 8 Lorentz Transform Crystal, 256000 mB Superfluid Helium, 16000 mB Quark-Gluon Plasma, 16000 mB Carborane Superacid → 1 Empirical Notes III (1,048,576 FE/t, 60 s)
  - 64 Event Horizon Casing, 32 Negentropy Cell, 48 Hyperdimensional Processor, 16 Kugelblitz Chamber, 16 Hawking Absorber Plate, 256 Attoscale Capacitor, 256 Attoscale Inductor, 16 Chronoplastic Gasket, 16 Entangled Bus Bar, 8 Virtual Monopole, 16 Coherent Wiring Harness, 8 D-Brane Sheet, 147456 mB Causal Solder, 256000 mB Superfluid Helium, 16000 mB Antiwater, 16000 mB Tachyon Flux → 1 Entropy Horizon (4,194,304 FE/t, 180 s)  ⟶  64 Event Horizon Casing, 32 Negentropy Cell, 48 Hyperdimensional Processor, 16 Kugelblitz Chamber, 16 Hawking Absorber Plate, 256 Attoscale Capacitor, 256 Attoscale Inductor, 16 Chronoplastic Gasket, 16 Entangled Bus Bar, 8 Virtual Graviton, 32 Claytronic Catom, 8 Kaluza-Klein Shard, 147456 mB Causal Solder, 256000 mB Superfluid Helium, 16000 mB Liquid Antihelium, 16000 mB Hawking Radiation → 1 Entropy Horizon (4,194,304 FE/t, 180 s)
  - 96 Hyperdimensional Processor, 32 Containment Field Glass, 32 Chronosteel Coil Block, 16 Fractionation Spire, 256 Attoscale Capacitor, 256 Attoscale Inductor, 16 Chronoplastic Gasket, 16 Entangled Bus Bar, 8 Brane Tension Fiber, 32 Quantum Field Lattice, 8 Cosmic Circuit Board, 147456 mB Causal Solder, 256000 mB Superfluid Helium, 16000 mB Inflaton Field, 16000 mB Tachyon Flux → 1 Essence Distillery (4,194,304 FE/t, 180 s)  ⟶  96 Hyperdimensional Processor, 32 Containment Field Glass, 32 Chronosteel Coil Block, 16 Fractionation Spire, 256 Attoscale Capacitor, 256 Attoscale Inductor, 16 Chronoplastic Gasket, 16 Entangled Bus Bar, 8 Virtual Graviton, 16 Standard Model Matrix, 8 Hyperdimensional Pearl, 147456 mB Causal Solder, 256000 mB Superfluid Helium, 16000 mB Nebular Gas, 16000 mB Hawking Radiation → 1 Essence Distillery (4,194,304 FE/t, 180 s)
  - 64 Topological Processor, 32 Arrow of Time Shard, 32 Chronoplastic Casing, 16 Arbor Heartwood Sample, 16 Gene Sequencer, 16 Temporal Knot, 128 Attoscale Capacitor, 128 Attoscale Inductor, 16 Chronoplastic Gasket, 16 Entangled Bus Bar, 16 Continuum Shard, 32 Prime Singularity, 16 Quantum Singularity, 73728 mB Exotic Solder, 256000 mB Superfluid Helium, 16000 mB Ionic Liquid, 16000 mB Carborane Superacid → 1 Evolution Engine (1,048,576 FE/t, 180 s)  ⟶  64 Topological Processor, 32 Arrow of Time Shard, 32 Chronoplastic Casing, 16 Arbor Heartwood Sample, 16 Gene Sequencer, 16 Temporal Knot, 128 Attoscale Capacitor, 128 Attoscale Inductor, 16 Chronoplastic Gasket, 16 Entangled Bus Bar, 16 Theory of Everything, 32 Standard Model Matrix, 16 Dimensional Singularity, 73728 mB Exotic Solder, 256000 mB Superfluid Helium, 16000 mB Liquid Spacetime, 16000 mB Superconducting Solder → 1 Evolution Engine (1,048,576 FE/t, 180 s)
  - 96 Ontological Processor, 32 Graviton Lens Array, 16 Hypernova Alloy Ingot, 16 Kugelblitz Chamber, 16 Hawking Radiation Collector, 512 Attoscale Capacitor, 512 Attoscale Inductor, 16 Chronoplastic Gasket, 16 Entangled Bus Bar, 16 Theoretical Notes II, 32 Supernova Remnant, 8 True Vacuum Seed, 294912 mB Causal Solder, 256000 mB Superfluid Helium, 16000 mB Psionic Ether, 16000 mB Dimensional Residue → 1 Eye of the Event Horizon (67,108,864 FE/t, 180 s)  ⟶  96 Ontological Processor, 32 Graviton Lens Array, 16 Hypernova Alloy Ingot, 16 Kugelblitz Chamber, 16 Hawking Radiation Collector, 512 Attoscale Capacitor, 512 Attoscale Inductor, 16 Chronoplastic Gasket, 16 Entangled Bus Bar, 8 True Vacuum Seed, 32 Assembly Matrix, 16 Einstein-Rosen Core, 294912 mB Causal Solder, 256000 mB Superfluid Helium, 16000 mB Inflaton Field, 16000 mB Quantum-Dot Photoresist → 1 Eye of the Event Horizon (67,108,864 FE/t, 180 s)
  - 8 Field Emitter, 32 Graviton Crystal, 16 Gravitic Lens Element, 8 Hyperdimensional Processor, 8 Hyperon Crystal, 16 Chronoplastic Gasket, 16 Entangled Bus Bar, 8 Dilaton Crystal, 32 Qubit Array Wafer, 8 Rift Anchor, 36864 mB Exotic Solder, 64000 mB Exotic Matter, 16000 mB Xenoplasm, 16000 mB Hawking Radiation → 1 Graviton Field Emitter (1,048,576 FE/t, 60 s)  ⟶  8 Field Emitter, 32 Graviton Crystal, 16 Gravitic Lens Element, 8 Hyperdimensional Processor, 8 Hyperon Crystal, 16 Chronoplastic Gasket, 16 Entangled Bus Bar, 8 G₂ Manifold, 32 Photonic Die, 8 Virtual Monopole, 36864 mB Exotic Solder, 64000 mB Exotic Matter, 16000 mB Carborane Superacid, 16000 mB Quark-Gluon Plasma → 1 Graviton Field Emitter (1,048,576 FE/t, 60 s)
  - 8 Topological Circuit Board, 32 Hyperdimensional Die, 16 Topological Processor, 64 Attoscale Capacitor, 64 Attoscale Transistor, 64 Attoscale Inductor, 8 Tesseract Lens, 16 Chronoplastic Gasket, 16 Entangled Bus Bar, 8 Spacetime Catalyst, 16 Dyson Shell Segment, 8 Magnetar Shard, 36864 mB Exotic Solder, 64000 mB Superfluid Helium, 16000 mB Exotic Matter, 16000 mB Nanite Slurry → 2 Hyperdimensional Processor (1,048,576 FE/t, 90 s)  ⟶  8 Topological Circuit Board, 32 Hyperdimensional Die, 16 Topological Processor, 64 Attoscale Capacitor, 64 Attoscale Transistor, 64 Attoscale Inductor, 8 Tesseract Lens, 16 Chronoplastic Gasket, 16 Entangled Bus Bar, 8 Riemann Curvature Tensor, 32 Void Essence, 8 Primordial Nucleosynthesis Ash, 36864 mB Exotic Solder, 64000 mB Superfluid Helium, 16000 mB Quantum-Dot Photoresist, 16000 mB Synaptic Fluid → 2 Hyperdimensional Processor (1,048,576 FE/t, 90 s)
  - 16 Assembly Matrix, 8 Hyperframe Casing, 8 Graviton Crystal, 8 Tesseract Core, 16 Chronoplastic Gasket, 16 Entangled Bus Bar, 8 Von Neumann Probe, 16 Lapis Singularity, 8 Cosmic Neutronium Singularity, 16000 mB Superconducting Solder, 16000 mB Supercritical Solvent → 4 Hyperlattice Frame (1,048,576 FE/t, 45 s)  ⟶  16 Assembly Matrix, 8 Hyperframe Casing, 8 Graviton Crystal, 8 Tesseract Core, 16 Chronoplastic Gasket, 16 Entangled Bus Bar, 8 Spacetime Alloy Ingot, 32 Electrical Steel Lamination, 8 Hyperon Crystal, 16000 mB Superfluid Helium, 16000 mB Nebular Gas → 4 Hyperlattice Frame (1,048,576 FE/t, 45 s)
  - 64 Cosmogenic Casing, 32 Field Emitter, 48 Hyperdimensional Processor, 16 Dark Energy Siphon, 16 Dark Matter Lens, 256 Attoscale Capacitor, 256 Attoscale Inductor, 16 Chronoplastic Gasket, 16 Entangled Bus Bar, 8 Frozen Photon, 32 Dyson Swarm Satellite, 8 Universe Seed, 147456 mB Causal Solder, 256000 mB Superfluid Helium, 16000 mB Nutrient Broth, 16000 mB Neutrino Flux → 1 Inflation Chamber (4,194,304 FE/t, 180 s)  ⟶  64 Cosmogenic Casing, 32 Field Emitter, 48 Hyperdimensional Processor, 16 Dark Energy Siphon, 16 Dark Matter Lens, 256 Attoscale Capacitor, 256 Attoscale Inductor, 16 Chronoplastic Gasket, 16 Entangled Bus Bar, 8 Feynman Diagram, 32 Modular Form, 8 Cosmological Constant, 147456 mB Causal Solder, 256000 mB Superfluid Helium, 16000 mB Carborane Superacid, 16000 mB Synaptic Fluid → 1 Inflation Chamber (4,194,304 FE/t, 180 s)
  - 96 Hyperdimensional Processor, 64 Computronium Block, 16 Stellar Core, 256 Attoscale Capacitor, 256 Attoscale Inductor, 16 Chronoplastic Gasket, 16 Entangled Bus Bar, 8 Virtual Monopole, 32 Anti-Iron Ingot, 8 Dilaton Crystal, 147456 mB Causal Solder, 256000 mB Superfluid Helium, 16000 mB Schrödinger's Limit, 16000 mB Supercritical Solvent → 1 Matrioshka Brain (4,194,304 FE/t, 180 s)  ⟶  96 Hyperdimensional Processor, 64 Computronium Block, 16 Stellar Core, 256 Attoscale Capacitor, 256 Attoscale Inductor, 16 Chronoplastic Gasket, 16 Entangled Bus Bar, 8 Hyperdimensional Pearl, 32 Empty Set, 8 Cosmic Circuit Board, 147456 mB Causal Solder, 256000 mB Superfluid Helium, 16000 mB Nanite Slurry, 16000 mB Synaptic Fluid → 1 Matrioshka Brain (4,194,304 FE/t, 180 s)
  - 64 Topological Processor, 32 Antiproton Bundle, 32 Magnetic Confinement Casing, 16 Photonic Lens, 16 Matter-Antimatter Annihilation Reactor, 16 Event Horizon Casing, 128 Attoscale Capacitor, 128 Attoscale Inductor, 16 Entangled Bus Bar, 8 Fourier Series, 16 Topological Wafer, 8 General AI Core, 32 Photonic Die, 73728 mB Exotic Solder, 256000 mB Superfluid Helium, 16000 mB Synaptic Fluid, 16000 mB Supercritical Solvent → 1 Mirror Matter Foundry (1,048,576 FE/t, 180 s)  ⟶  64 Topological Processor, 32 Antiproton Bundle, 32 Magnetic Confinement Casing, 16 Photonic Lens, 16 Matter-Antimatter Annihilation Reactor, 16 Event Horizon Casing, 128 Attoscale Capacitor, 128 Attoscale Inductor, 16 Entangled Bus Bar, 8 Computronium Ingot, 16 Topological Wafer, 8 Antisilicon Shard, 16 Strange Matter Pellet, 73728 mB Exotic Solder, 256000 mB Superfluid Helium, 16000 mB Synaptic Fluid, 16000 mB Tachyon Flux → 1 Mirror Matter Foundry (1,048,576 FE/t, 180 s)
  - 96 Hyperdimensional Processor, 32 Rift Anchor, 16 Tesseract Core, 16 Rift Frame Casing, 16 Graviton Crystal, 256 Attoscale Capacitor, 256 Attoscale Inductor, 16 Chronoplastic Gasket, 16 Entangled Bus Bar, 8 Hyperdimensional Logic Wafer, 32 Holographic Matrix, 8 Superintelligence Core, 147456 mB Causal Solder, 256000 mB Superfluid Helium, 16000 mB Nebular Gas, 16000 mB Exotic Solder → 1 Multiverse Rift Stabilizer (4,194,304 FE/t, 180 s)  ⟶  96 Hyperdimensional Processor, 32 Rift Anchor, 16 Tesseract Core, 16 Rift Frame Casing, 16 Graviton Crystal, 256 Attoscale Capacitor, 256 Attoscale Inductor, 16 Chronoplastic Gasket, 16 Entangled Bus Bar, 8 Superintelligence Core, 32 Chronon Capacitor, 8 Dilaton Crystal, 147456 mB Causal Solder, 256000 mB Superfluid Helium, 16000 mB Exotic Solder, 16000 mB Quark-Gluon Plasma → 1 Multiverse Rift Stabilizer (4,194,304 FE/t, 180 s)
  - 64 Block of Neutronium, 32 Magnetic Monopole, 64 Topological Processor, 16 Magnetic Confinement Casing, 128 Attoscale Capacitor, 128 Attoscale Inductor, 16 Entangled Bus Bar, 8 Protostar Core, 16 Real Continuum, 8 Möbius Strip, 32 Dilithium Crystal, 73728 mB Exotic Solder, 256000 mB Superfluid Helium, 16000 mB Nutrient Broth, 16000 mB Bose-Einstein Condensate → 1 Neutron Star Forge (1,048,576 FE/t, 180 s)  ⟶  64 Block of Neutronium, 32 Magnetic Monopole, 64 Topological Processor, 16 Magnetic Confinement Casing, 128 Attoscale Capacitor, 128 Attoscale Inductor, 16 Entangled Bus Bar, 8 Tardigrade Culture, 16 Topological Logic Wafer, 8 Positronium Hydride, 32 Neutronium Ingot, 73728 mB Exotic Solder, 256000 mB Superfluid Helium, 16000 mB Supercritical Solvent, 16000 mB Quark-Gluon Plasma → 1 Neutron Star Forge (1,048,576 FE/t, 180 s)
  - 64 Topological Processor, 32 Calorimeter Detector Array, 32 Magnetic Confinement Casing, 16 RF Accelerating Cavity, 16 Chemical Reactor, 16 R-Process Cluster, 128 Attoscale Capacitor, 128 Attoscale Inductor, 16 Chronoplastic Gasket, 16 Entangled Bus Bar, 8 Hyperdimensional Boule, 32 Proof Fragment, 8 Thought Matrix, 73728 mB Exotic Solder, 256000 mB Superfluid Helium, 16000 mB Nanite Slurry, 16000 mB Chronon Gas → 1 Nuclide Forge (1,048,576 FE/t, 180 s)  ⟶  64 Topological Processor, 32 Calorimeter Detector Array, 32 Magnetic Confinement Casing, 16 RF Accelerating Cavity, 16 Chemical Reactor, 16 R-Process Cluster, 128 Attoscale Capacitor, 128 Attoscale Inductor, 16 Chronoplastic Gasket, 16 Entangled Bus Bar, 8 Anti-Iron Ingot, 32 Neutronium Ingot, 8 Dyson Shell Segment, 73728 mB Exotic Solder, 256000 mB Superfluid Helium, 16000 mB Supercritical Solvent, 16000 mB Ionic Liquid → 1 Nuclide Forge (1,048,576 FE/t, 180 s)
  - 96 Transfinite Processor, 32 Crystallised Law, 16 Arbor Heartwood, 16 Omnilex Casing, 16 Omega Point, 640 Attoscale Capacitor, 640 Attoscale Inductor, 16 Chronoplastic Gasket, 16 Entangled Bus Bar, 16 Axiom of Choice, 32 Quaternion Crystal, 16 Banach-Tarski Sphere, 368640 mB Causal Solder, 256000 mB Superfluid Helium, 16000 mB Residual Starlight, 16000 mB Ionic Liquid → 1 Omnilex Engine (268,435,456 FE/t, 180 s)  ⟶  96 Transfinite Processor, 32 Crystallised Law, 16 Arbor Heartwood, 16 Omnilex Casing, 16 Omega Point, 640 Attoscale Capacitor, 640 Attoscale Inductor, 16 Chronoplastic Gasket, 16 Entangled Bus Bar, 8 Absolute Infinity, 32 Geiger Tube, 16 Stellar Singularity, 368640 mB Causal Solder, 256000 mB Superfluid Helium, 16000 mB Superconducting Solder, 16000 mB Neutrino Flux → 1 Omnilex Engine (268,435,456 FE/t, 180 s)
  - 8 Cosmic Circuit Board, 8 Source Code Fragment, 16 Cosmic Processor, 16 Holographic Matrix, 256 Attoscale Transistor, 16 Bekenstein Crystal, 8 Boltzmann Brain, 16 Chronoplastic Gasket, 16 Entangled Bus Bar, 16 Paradox Residue, 32 Antimatter Singularity, 16 Eigenstate Lattice, 73728 mB Causal Solder, 256000 mB Synaptic Fluid, 256000 mB Superfluid Helium, 16000 mB Dark Energy, 16000 mB Ionic Liquid → 2 Ontological Processor (67,108,864 FE/t, 180 s)  ⟶  8 Cosmic Circuit Board, 8 Source Code Fragment, 16 Cosmic Processor, 16 Holographic Matrix, 256 Attoscale Transistor, 16 Bekenstein Crystal, 8 Boltzmann Brain, 16 Chronoplastic Gasket, 16 Entangled Bus Bar, 16 Primordial Ejecta, 32 Netherite Singularity, 16 Feynman Diagram, 73728 mB Causal Solder, 256000 mB Synaptic Fluid, 256000 mB Superfluid Helium, 16000 mB Neutrino Flux, 16000 mB Psionic Ether → 2 Ontological Processor (67,108,864 FE/t, 180 s)
  - 96 Hyperdimensional Processor, 32 Pleroma Crystal, 32 Apeiron Ingot, 16 Omnipotence Casing, 256 Attoscale Capacitor, 256 Attoscale Inductor, 16 Chronoplastic Gasket, 16 Entangled Bus Bar, 16 Primordial Ocean Simulacrum, 32 Strange Matter Singularity, 32 Absolute Notes II, 147456 mB Causal Solder, 256000 mB Superfluid Helium, 16000 mB Quark-Gluon Plasma, 16000 mB Relativity Breakdown → 1 Pleroma Reconstitutor (4,194,304 FE/t, 180 s)  ⟶  96 Hyperdimensional Processor, 32 Pleroma Crystal, 32 Apeiron Ingot, 16 Omnipotence Casing, 256 Attoscale Capacitor, 256 Attoscale Inductor, 16 Chronoplastic Gasket, 16 Entangled Bus Bar, 8 D-Brane Sheet, 32 Lorentz Transform Crystal, 8 Moduli Crystal, 147456 mB Causal Solder, 256000 mB Superfluid Helium, 16000 mB Warm Helium, 16000 mB Schrödinger's Limit → 1 Pleroma Reconstitutor (4,194,304 FE/t, 180 s)
  - 16 Adaptive Feedback Core, 32 Superintelligence Core, 64 Hyperdimensional Processor, 32 Closed Timelike Curve, 8 Autonomous Calibrator, 16 Chronoplastic Gasket, 16 Entangled Bus Bar, 8 Cosmic String Lens, 32 Magnetic Monopole, 8 Silicon Life Spore, 16000 mB Bose-Einstein Condensate, 16000 mB Ionic Liquid → 1 Predictive Lattice Governor (16,777,216 FE/t, 180 s)  ⟶  16 Adaptive Feedback Core, 32 Superintelligence Core, 64 Hyperdimensional Processor, 32 Closed Timelike Curve, 8 Autonomous Calibrator, 16 Chronoplastic Gasket, 16 Entangled Bus Bar, 8 Gravitational Wave Crystal, 16 Grand Unified Core, 8 Virtual Quark, 16000 mB Liquid Antihelium, 16000 mB Exotic Solder → 1 Predictive Lattice Governor (16,777,216 FE/t, 180 s)
  - 96 Cosmic Processor, 32 Holographic Matrix, 16 Bekenstein Crystal, 16 Reality Lattice Casing, 16 Boltzmann Brain, 384 Attoscale Capacitor, 384 Attoscale Inductor, 16 Chronoplastic Gasket, 16 Entangled Bus Bar, 8 Tidal Alloy Scrap, 32 Fractal Set, 8 Metastable Vacuum Lattice, 16 Phantom Energy Cell, 221184 mB Causal Solder, 256000 mB Superfluid Helium, 16000 mB Dark Era Haze, 16000 mB Tachyon Flux → 1 Reality Compiler (16,777,216 FE/t, 180 s)  ⟶  96 Cosmic Processor, 32 Holographic Matrix, 16 Bekenstein Crystal, 16 Reality Lattice Casing, 16 Boltzmann Brain, 384 Attoscale Capacitor, 384 Attoscale Inductor, 16 Chronoplastic Gasket, 16 Entangled Bus Bar, 8 Einstein-Rosen Core, 16 Flerovium Ingot, 8 Retrocausal Crystal, 221184 mB Causal Solder, 256000 mB Superfluid Helium, 16000 mB Liquid Antihelium, 16000 mB Quark-Gluon Plasma → 1 Reality Compiler (16,777,216 FE/t, 180 s)
  - 8 Chronosteel Coil Block, 64 Spacetime Alloy Ingot, 64 Superconducting Coil, 8 Topological Processor, 16 Cryocooler Unit, 16 Chronoplastic Gasket, 16 Entangled Bus Bar, 8 Blue Ice Singularity, 16 Attoscale Inductor, 8 Anti-Iron Ingot, 36864 mB Exotic Solder, 64000 mB Superfluid Helium, 16000 mB Superconducting Solder, 16000 mB Supercritical Solvent → 1 Spacetime Alloy Coil Block (262,144 FE/t, 45 s)  ⟶  8 Chronosteel Coil Block, 64 Spacetime Alloy Ingot, 64 Superconducting Coil, 8 Topological Processor, 16 Cryocooler Unit, 16 Chronoplastic Gasket, 16 Entangled Bus Bar, 8 Obsidian Singularity, 32 Entangled Photon Pair, 8 Gold Singularity, 36864 mB Exotic Solder, 64000 mB Superfluid Helium, 16000 mB Warm Helium, 16000 mB Supercritical Solvent → 1 Spacetime Alloy Coil Block (262,144 FE/t, 45 s)
  - 128 Up & Down Quarks, 64 Strange & Charm Quarks, 32 Bottom & Top Quarks, 64 Lepton Triplet, 32 Gauge Boson Cluster, 8 Higgs Boson Crystal, 8 Topological Processor, 16 Chronoplastic Gasket, 16 Entangled Bus Bar, 8 Red Giant Envelope, 16 Formal Proof Tree, 8 Emerald Singularity, 16 Functor Bridge, 36864 mB Exotic Solder, 128000 mB Quark-Gluon Plasma, 16000 mB Nanite Slurry, 16000 mB Supercritical Solvent → 1 Standard Model Matrix (1,048,576 FE/t, 90 s)  ⟶  128 Up & Down Quarks, 64 Strange & Charm Quarks, 32 Bottom & Top Quarks, 64 Lepton Triplet, 32 Gauge Boson Cluster, 8 Higgs Boson Crystal, 8 Topological Processor, 16 Chronoplastic Gasket, 16 Entangled Bus Bar, 8 Quartz Singularity, 32 Casimir Plate, 8 Bekenstein Crystal, 36864 mB Exotic Solder, 128000 mB Quark-Gluon Plasma, 16000 mB Warm Helium, 16000 mB Strange Matter → 1 Standard Model Matrix (1,048,576 FE/t, 90 s)
  - 64 Calabi-Yau Manifold, 32 White Hole Fragment, 64 Topological Processor, 16 Hyperframe Casing, 128 Attoscale Capacitor, 128 Attoscale Inductor, 16 Chronoplastic Gasket, 16 Entangled Bus Bar, 8 Self-Healing Polymer, 16 Real Continuum, 8 Power Set, 73728 mB Exotic Solder, 256000 mB Superfluid Helium, 16000 mB Supercritical Solvent, 16000 mB Nutrient Broth → 1 Tesseract Assembler (1,048,576 FE/t, 180 s)  ⟶  64 Calabi-Yau Manifold, 32 White Hole Fragment, 64 Topological Processor, 16 Hyperframe Casing, 128 Attoscale Capacitor, 128 Attoscale Inductor, 16 Entangled Bus Bar, 8 Anticarbon Crystal, 8 Hawking Emitter, 8 Category Lattice, 16 Topological Boule, 73728 mB Exotic Solder, 256000 mB Superfluid Helium, 16000 mB Nebular Gas, 16000 mB Nanite Slurry → 1 Tesseract Assembler (1,048,576 FE/t, 180 s)
  - 16 Theoretical Notes I, 16 Theoretical Notes II, 16 Theoretical Notes III, 8 Empirical Dossier, 16 Chronoplastic Gasket, 16 Entangled Bus Bar, 16 Transcendent Metal Ingot, 32 Hyperdimensional Pearl, 16 Omega Point, 512000 mB Exotic Matter, 16000 mB Reheating Plasma, 16000 mB Synaptic Fluid → 1 Theoretical Dossier (8,388,608 FE/t, 90 s)  ⟶  16 Theoretical Notes I, 16 Theoretical Notes II, 16 Theoretical Notes III, 8 Empirical Dossier, 16 Chronoplastic Gasket, 16 Entangled Bus Bar, 16 Cardinal Shard, 32 Biosphere Compendium, 16 Transfinite Processor, 512000 mB Exotic Matter, 16000 mB Nanite Slurry, 16000 mB Schrödinger's Limit → 1 Theoretical Dossier (8,388,608 FE/t, 90 s)
  - 32 Autonomous Calibrator, 32 Dyson Shell Segment, 32 Topos, 32 Dimensional Stabiliser Matrix, 32 R-Process Cluster, 32 Von Neumann Probe, 32 Hyperdimensional Boule, 32 Riemann Curvature Tensor, 32 Higgs Catalyst, 16 Chronoplastic Gasket, 16 Entangled Bus Bar, 16 Reality Anchor, 32 Cryptochrome Magnetoreceptor, 8 Omega-One, 256000 mB Exotic Matter, 16000 mB Hawking Radiation, 16000 mB Exotic Solder → 1 Theoretical Notes I (4,194,304 FE/t, 75 s)  ⟶  32 Autonomous Calibrator, 32 Dyson Shell Segment, 32 Topos, 32 Dimensional Stabiliser Matrix, 32 R-Process Cluster, 32 Von Neumann Probe, 32 Hyperdimensional Boule, 32 Riemann Curvature Tensor, 32 Higgs Catalyst, 16 Chronoplastic Gasket, 16 Entangled Bus Bar, 16 Surreal Number, 32 Stem Cell Culture, 8 Omega-One, 256000 mB Exotic Matter, 16000 mB Synaptic Fluid, 16000 mB Dark Matter → 1 Theoretical Notes I (4,194,304 FE/t, 75 s)
  - 32 Superintelligence Core, 32 M-Brane, 32 Compactified Circle, 32 Degenerate Armour Plate, 32 Spacetime Slag, 32 Cosmic Neutronium Ingot, 32 Hyperdimensional Logic Wafer, 32 Spacetime Catalyst, 32 Massless Alloy, 16 Chronoplastic Gasket, 16 Entangled Bus Bar, 8 Hyperdimensional Pearl, 32 Maxwell's Demon Chip, 8 Hyperdimensional Die, 256000 mB Exotic Matter, 16000 mB Dark Matter, 16000 mB Quark-Gluon Plasma → 1 Theoretical Notes II (4,194,304 FE/t, 75 s)  ⟶  32 Superintelligence Core, 32 M-Brane, 32 Compactified Circle, 32 Degenerate Armour Plate, 32 Spacetime Slag, 32 Cosmic Neutronium Ingot, 32 Hyperdimensional Logic Wafer, 32 Spacetime Catalyst, 32 Massless Alloy, 16 Chronoplastic Gasket, 16 Entangled Bus Bar, 8 Hyperdimensional Die, 16 Riemann Curvature Tensor, 8 D-Brane Sheet, 256000 mB Exotic Matter, 16000 mB Schrödinger's Limit, 16000 mB Superfluid Helium → 1 Theoretical Notes II (4,194,304 FE/t, 75 s)
  - 32 Category Lattice, 32 Multiverse Shard, 32 String Coupling Crystal, 32 Metallic Hydrogen Ingot, 32 Supernova Remnant, 32 Gravitational Wave Crystal, 32 Metric Tensor, 32 Tesseract Core, 32 Protostar Core, 16 Chronoplastic Gasket, 16 Entangled Bus Bar, 8 Christoffel Symbol, 32 Glueball Core, 8 Silicon Life Spore, 256000 mB Exotic Matter, 16000 mB Psionic Ether, 16000 mB Inflaton Field → 1 Theoretical Notes III (4,194,304 FE/t, 75 s)  ⟶  32 Category Lattice, 32 Multiverse Shard, 32 String Coupling Crystal, 32 Metallic Hydrogen Ingot, 32 Supernova Remnant, 32 Gravitational Wave Crystal, 32 Metric Tensor, 32 Tesseract Core, 32 Protostar Core, 16 Chronoplastic Gasket, 16 Entangled Bus Bar, 8 Geodesic Thread, 16 Empirical Notes III, 8 Cosmic Circuit Board, 256000 mB Exotic Matter, 16000 mB Quark-Gluon Plasma, 16000 mB Strange Matter → 1 Theoretical Notes III (4,194,304 FE/t, 75 s)
  - 96 Cosmic Processor, 32 Cosmic Neutronium Coil Block, 16 Glueball Core, 16 Stellar Plasma Crucible, 16 Hyperon Crystal, 384 Attoscale Capacitor, 384 Attoscale Inductor, 16 Chronoplastic Gasket, 16 Entangled Bus Bar, 8 Dilaton Crystal, 32 Chronon Capacitor, 8 Kerr Metric Shard, 221184 mB Causal Solder, 256000 mB Superfluid Helium, 16000 mB Strange Matter, 16000 mB Warm Helium → 1 Transcendent Plasma Forge (16,777,216 FE/t, 180 s)  ⟶  96 Cosmic Processor, 32 Cosmic Neutronium Coil Block, 16 Glueball Core, 16 Stellar Plasma Crucible, 16 Hyperon Crystal, 384 Attoscale Capacitor, 384 Attoscale Inductor, 16 Chronoplastic Gasket, 16 Entangled Bus Bar, 8 Hyperdimensional Processor, 32 Iron Singularity, 8 Cosmological Constant, 221184 mB Causal Solder, 256000 mB Superfluid Helium, 16000 mB Xenoplasm, 16000 mB Liquid Metallic Hydrogen → 1 Transcendent Plasma Forge (16,777,216 FE/t, 180 s)
  - 8 Cosmic Neutronium Coil Block, 64 Hypernova Alloy Ingot, 16 Aleph-Null Shard, 8 Cosmic Processor, 32 Hyperon Crystal, 16 Room-Temperature Superconductor, 16 Chronoplastic Gasket, 16 Entangled Bus Bar, 16 Einstein-Rosen Core, 32 Computronium Ingot, 8 Anomalous Strain Culture, 73728 mB Causal Solder, 256000 mB Superfluid Helium, 16000 mB Dark Era Haze, 16000 mB Strange Matter → 1 Transfinite Coil Block (4,194,304 FE/t, 90 s)  ⟶  8 Cosmic Neutronium Coil Block, 64 Hypernova Alloy Ingot, 16 Aleph-Null Shard, 8 Cosmic Processor, 32 Hyperon Crystal, 16 Room-Temperature Superconductor, 16 Chronoplastic Gasket, 16 Entangled Bus Bar, 8 Inflaton Condensate, 32 Stellar Iron Singularity, 16 Heat Death Remnant, 73728 mB Causal Solder, 256000 mB Superfluid Helium, 16000 mB Superconducting Solder, 16000 mB Reheating Plasma → 1 Transfinite Coil Block (4,194,304 FE/t, 90 s)
  - 16 Cosmic Circuit Board, 32 Aleph-Null Shard, 16 Ontological Processor, 8 Godel Sentence, 512 Attoscale Capacitor, 512 Attoscale Transistor, 512 Attoscale Inductor, 16 Chronoplastic Gasket, 16 Entangled Bus Bar, 8 No-Boundary Seed, 32 Anti-Gold Ingot, 8 Inflaton Condensate, 147456 mB Causal Solder, 256000 mB Dark Energy, 512000 mB Superfluid Helium, 16000 mB Quantum-Dot Photoresist, 16000 mB Xenoplasm → 2 Transfinite Processor (268,435,456 FE/t, 270 s)  ⟶  16 Cosmic Circuit Board, 32 Aleph-Null Shard, 16 Ontological Processor, 8 Godel Sentence, 512 Attoscale Capacitor, 512 Attoscale Transistor, 512 Attoscale Inductor, 16 Chronoplastic Gasket, 16 Entangled Bus Bar, 8 Aeon Boundary Shard, 32 Unbihexium Ingot, 8 Source Code Fragment, 147456 mB Causal Solder, 256000 mB Dark Energy, 512000 mB Superfluid Helium, 16000 mB Bose-Einstein Condensate, 16000 mB Residual Starlight → 2 Transfinite Processor (268,435,456 FE/t, 270 s)
  - 16 Hyperlattice Frame, 16 Aleph-Null Shard, 8 Cosmic Processor, 8 Crystalline Matrix Ingot, 16 Chronoplastic Gasket, 16 Entangled Bus Bar, 8 Eternity Catalyst, 32 Femtoscale Capacitor, 8 Ontological Processor, 16000 mB Exotic Matter, 16000 mB Superconducting Solder → 4 Transfinite Weave (16,777,216 FE/t, 60 s)  ⟶  16 Hyperlattice Frame, 16 Aleph-Null Shard, 8 Cosmic Processor, 8 Crystalline Matrix Ingot, 16 Chronoplastic Gasket, 16 Entangled Bus Bar, 16 True Vacuum Seed, 32 Pentaquark Shard, 8 Baryon Asymmetry Crystal, 16000 mB Warm Helium, 16000 mB Dark Energy → 4 Transfinite Weave (16,777,216 FE/t, 60 s)
  - 16 Unified Notes I, 16 Unified Notes II, 16 Unified Notes III, 8 Theoretical Dossier, 16 Chronoplastic Gasket, 16 Entangled Bus Bar, 8 Stellar Singularity, 32 Lattice Seed Crystal, 8 Theory of Everything, 1024000 mB Synaptic Fluid, 16000 mB Liquid Metallic Hydrogen, 16000 mB Strange Matter → 1 Unified Dossier (33,554,432 FE/t, 105 s)  ⟶  16 Unified Notes I, 16 Unified Notes II, 16 Unified Notes III, 8 Theoretical Dossier, 16 Chronoplastic Gasket, 16 Entangled Bus Bar, 8 Mathematics Singularity, 32 Genome Sample, 16 Axiom Fragment, 1024000 mB Synaptic Fluid, 16000 mB Inflaton Field, 16000 mB Bose-Einstein Condensate → 1 Unified Dossier (33,554,432 FE/t, 105 s)
  - 32 Absolute Infinity, 32 Hypernova Alloy Ingot, 32 Reality Anchor, 32 Geodesic Thread, 32 Superposition Crystal, 32 Eigenstate Lattice, 32 Stellar Core, 32 Wormhole Throat, 32 Cosmic Logic Wafer, 16 Chronoplastic Gasket, 16 Entangled Bus Bar, 8 Stellar Singularity, 16 Coherent Wiring Harness, 8 Axiom of Choice, 512000 mB Synaptic Fluid, 16000 mB Transcendent Plasma, 16000 mB Dimensional Residue → 1 Unified Notes I (16,777,216 FE/t, 90 s)  ⟶  32 Absolute Infinity, 32 Hypernova Alloy Ingot, 32 Reality Anchor, 32 Geodesic Thread, 32 Superposition Crystal, 32 Eigenstate Lattice, 32 Stellar Core, 32 Wormhole Throat, 32 Cosmic Logic Wafer, 16 Chronoplastic Gasket, 16 Entangled Bus Bar, 8 Axiom of Choice, 32 Neural Organoid, 8 Cognitive Singularity, 512000 mB Synaptic Fluid, 16000 mB Supercritical Solvent, 16000 mB Residual Starlight → 1 Unified Notes I (16,777,216 FE/t, 90 s)
  - 32 Aleph-Null Shard, 32 Eternity Catalyst, 32 Christoffel Symbol, 32 Pocket Universe, 32 White Hole Fragment, 32 Graviton Crystal, 32 Tesseract Core, 32 Cosmic Circuit Board, 32 Cosmological Constant, 16 Chronoplastic Gasket, 16 Entangled Bus Bar, 8 Horizon Shard, 32 Computronium Ingot, 8 No-Boundary Seed, 512000 mB Synaptic Fluid, 16000 mB Relativity Breakdown, 16000 mB Neutrino Flux → 1 Unified Notes II (16,777,216 FE/t, 90 s)  ⟶  32 Aleph-Null Shard, 32 Eternity Catalyst, 32 Christoffel Symbol, 32 Pocket Universe, 32 White Hole Fragment, 32 Graviton Crystal, 32 Tesseract Core, 32 Cosmic Circuit Board, 32 Cosmological Constant, 16 Chronoplastic Gasket, 16 Entangled Bus Bar, 8 Source Code Fragment, 32 Nanite Swarm, 8 Topological Defect Core, 512000 mB Synaptic Fluid, 16000 mB Strange Matter, 16000 mB Schrödinger's Limit → 1 Unified Notes II (16,777,216 FE/t, 90 s)
  - 32 Surreal Number, 32 Omega Point, 32 Degenerate Matter Chunk, 32 Stress-Energy Tensor, 32 Bootstrap Artifact, 32 Magnetic Monopole, 32 Virtual Quark, 32 Cosmic Die, 32 Multiverse Shard, 16 Chronoplastic Gasket, 16 Entangled Bus Bar, 8 No-Boundary Seed, 32 Netherite Singularity, 16 Hypernova Alloy Ingot, 512000 mB Synaptic Fluid, 16000 mB Schrödinger's Limit, 16000 mB Strange Matter → 1 Unified Notes III (16,777,216 FE/t, 90 s)  ⟶  32 Surreal Number, 32 Omega Point, 32 Degenerate Matter Chunk, 32 Stress-Energy Tensor, 32 Bootstrap Artifact, 32 Magnetic Monopole, 32 Virtual Quark, 32 Cosmic Die, 32 Multiverse Shard, 16 Chronoplastic Gasket, 16 Entangled Bus Bar, 8 Ontological Processor, 32 Fractal Dust, 8 No-Boundary Seed, 512000 mB Synaptic Fluid, 16000 mB Psionic Ether, 16000 mB Superfluid Helium → 1 Unified Notes III (16,777,216 FE/t, 90 s)
  - 96 Hyperdimensional Processor, 32 Graviton Crystal, 16 Tesseract Core, 16 Cosmogenic Casing, 16 Universe Seed, 256 Attoscale Capacitor, 256 Attoscale Inductor, 16 Chronoplastic Gasket, 16 Entangled Bus Bar, 8 Tidal Alloy Scrap, 16 Virtual Monopole, 8 Boltzmann Brain, 16 Phantom Energy Cell, 147456 mB Causal Solder, 256000 mB Superfluid Helium, 16000 mB Supercritical Solvent, 16000 mB Nebular Gas → 1 Universe Forge (4,194,304 FE/t, 180 s)  ⟶  96 Hyperdimensional Processor, 32 Graviton Crystal, 16 Tesseract Core, 16 Cosmogenic Casing, 16 Universe Seed, 256 Attoscale Capacitor, 256 Attoscale Inductor, 16 Chronoplastic Gasket, 16 Entangled Bus Bar, 8 Frozen Photon, 32 Raw Pitchblende, 8 Boltzmann Brain, 16 Phantom Energy Cell, 147456 mB Causal Solder, 256000 mB Superfluid Helium, 16000 mB Quark-Gluon Plasma, 16000 mB Inflaton Field → 1 Universe Forge (4,194,304 FE/t, 180 s)
  - 64 Hyperdimensional Processor, 32 Field Emitter, 32 Planck Shard, 16 Graviton Lens Array, 16 Planck Casing, 16 Casimir Plate, 256 Attoscale Capacitor, 256 Attoscale Inductor, 16 Chronoplastic Gasket, 16 Entangled Bus Bar, 8 Graviton Wisp Core, 32 Mirror Alloy Ingot, 8 False Vacuum Membrane, 147456 mB Causal Solder, 256000 mB Superfluid Helium, 16000 mB Tachyon Flux, 16000 mB Warm Helium → 1 Vacuum Fluctuation Harvester (4,194,304 FE/t, 180 s)  ⟶  64 Hyperdimensional Processor, 32 Field Emitter, 32 Planck Shard, 16 Graviton Lens Array, 16 Planck Casing, 16 Casimir Plate, 256 Attoscale Capacitor, 256 Attoscale Inductor, 16 Chronoplastic Gasket, 16 Entangled Bus Bar, 8 Virtual Photon, 32 Amethyst Singularity, 8 Hyperdimensional Logic Wafer, 147456 mB Causal Solder, 256000 mB Superfluid Helium, 16000 mB Hawking Radiation, 16000 mB Nebular Gas → 1 Vacuum Fluctuation Harvester (4,194,304 FE/t, 180 s)
  - 64 Exotic Matter Crystal, 32 Wormhole Throat, 64 Topological Processor, 16 Wormhole Frame Casing, 128 Attoscale Capacitor, 128 Attoscale Inductor, 16 Chronoplastic Gasket, 16 Entangled Bus Bar, 8 Standard Model Matrix, 32 Chronon Capacitor, 8 Klein Bottle, 73728 mB Exotic Solder, 256000 mB Superfluid Helium, 16000 mB Nanite Slurry, 16000 mB Quark-Gluon Plasma → 1 Einstein-Rosen Bridge Projector (1,048,576 FE/t, 180 s)  ⟶  64 Exotic Matter Crystal, 32 Wormhole Throat, 64 Topological Processor, 16 Wormhole Frame Casing, 128 Attoscale Capacitor, 128 Attoscale Inductor, 16 Chronoplastic Gasket, 16 Entangled Bus Bar, 8 Primordial Nucleosynthesis Ash, 32 Xenogenome Sample, 8 Klein Bottle, 73728 mB Exotic Solder, 256000 mB Superfluid Helium, 16000 mB Liquid Metallic Hydrogen, 16000 mB Supercritical Solvent → 1 Einstein-Rosen Bridge Projector (1,048,576 FE/t, 180 s)
- **Formal Proof Assistant**
  - 12 Aleph Glass, 12 Empty Set, 24 Natural Number Lattice, 6 Formal Proof Tree, 6 Entangled Bus Bar, 8000 mB Ionic Liquid → 2 Power Set (8,192 FE/t, 18.75 s)  ⟶  1 Aleph Glass, 1 Empty Set, 1 Natural Number Lattice, 1 Formal Proof Tree, 6 Entangled Bus Bar, 8000 mB Quark-Gluon Plasma → 16 Power Set (1 FE/t, 18.75 s)
  - 48 Axiom Schema, 6 Continuum Shard, 12 Formal Proof Tree, 6 Entangled Bus Bar, 6 Undecidable Kernel, 8000 mB Causal Solder → 1 Axiom of Choice (65,536 FE/t, 37.5 s)  ⟶  48 Axiom Schema, 6 Continuum Shard, 12 Formal Proof Tree, 6 Entangled Bus Bar, 6 Ordinal Shard, 8000 mB Dark Matter → 1 Axiom of Choice (65,536 FE/t, 37.5 s)
  - 12 Natural Number Lattice, 6 Qubit, 24 Lattice Seed Crystal, 24 Cosmic Record Fragment, 8000 mB Supercritical Solvent → 2 Axiom Schema (512 FE/t, 12.5 s)  ⟶  12 Natural Number Lattice, 6 Qubit, 12 Spacetime Slag, 24 Telescope Mirror Blank, 8000 mB Superfluid Helium → 2 Axiom Schema (512 FE/t, 12.5 s)
  - 24 Function Graph, 12 Axiom Schema, 6 Power Set, 6 Entangled Bus Bar, 6 Muonic Catalyst, 8000 mB Quark-Gluon Plasma → 1 Category Lattice (16,384 FE/t, 25 s)  ⟶  24 Function Graph, 12 Axiom Schema, 6 Power Set, 6 Lepton Triplet, 12 Void Lattice Shard, 8000 mB Warm Helium → 1 Category Lattice (16,384 FE/t, 25 s)
  - 6 Void Essence, 6 Photonic Logic Wafer, 24 Entangled Photon Pair, 8000 mB Bose-Einstein Condensate → 4 Empty Set (512 FE/t, 12.5 s)  ⟶  6 Void Essence, 24 Crystalline Matrix Ingot, 24 Sediment Core, 8000 mB Bose-Einstein Condensate → 4 Empty Set (512 FE/t, 12.5 s)
  - 24 Void Lattice Shard, 6 Axiom Schema, 24 Metamaterial Substrate, 24 Cosmic Record Fragment, 8000 mB Quantum-Dot Photoresist → 1 Empty Set (1,024 FE/t, 25 s)  ⟶  14 Void Lattice Shard, 3 Axiom Schema, 7 Photonic Lens, 14 Geoid Model, 4551 mB Superfluid Helium → 32 Empty Set (582 FE/t, 25 s)
  - 6 Quantum Field Lattice, 6 Gauge Boson Cluster, 12 Integral Operator, 6 Wavefunction, 6 Entangled Bus Bar, 8000 mB Nebular Gas → 2 Feynman Diagram (16,384 FE/t, 25 s)  ⟶  6 Quantum Field Lattice, 6 Gauge Boson Cluster, 12 Integral Operator, 6 Wavefunction, 6 Entangled Bus Bar, 8000 mB Chronon Gas → 2 Feynman Diagram (16,384 FE/t, 25 s)
  - 24 Axiom Schema, 6 Quantum Processor, 6 Function Graph, 6 Error-Correction Lattice, 12 Empty Set, 8000 mB Synaptic Fluid, 8000 mB Nanite Slurry → 2 Formal Proof Tree (2,048 FE/t, 25 s)  ⟶  24 Axiom Schema, 6 Quantum Processor, 6 Function Graph, 6 Fractal Dust, 24 Crystalline Matrix Ingot, 8000 mB Synaptic Fluid, 8000 mB Bose-Einstein Condensate → 2 Formal Proof Tree (2,048 FE/t, 25 s)
  - 12 Category Lattice, 6 Differential Manifold, 6 Octonion Crystal, 6 Entangled Bus Bar, 6 Folded Protein Crystal, 8000 mB Exotic Solder → 1 Functor Bridge (65,536 FE/t, 37.5 s)  ⟶  12 Category Lattice, 6 Differential Manifold, 6 Octonion Crystal, 6 Entangled Bus Bar, 6 Topological Circuit Board, 8000 mB Exotic Solder → 1 Functor Bridge (65,536 FE/t, 37.5 s)
  - 24 Hilbert Brick, 12 Natural Number Lattice, 6 Entangled Bus Bar, 12 No-Boundary Seed, 8000 mB Relativity Breakdown → 4 Axiom Schema (2,048 FE/t, 12.5 s)  ⟶  1 Hilbert Brick, 1 Natural Number Lattice, 6 Entangled Bus Bar, 12 Aeon Boundary Shard, 8000 mB Antihydrogen → 32 Axiom Schema (1 FE/t, 12.5 s)
  - 12 Empty Set, 24 Natural Number Lattice, 6 Formal Proof Tree, 6 Function Graph, 12 Rational Field, 8000 mB Warm Helium → 1 Power Set (4,096 FE/t, 25 s)  ⟶  12 Empty Set, 24 Natural Number Lattice, 6 Formal Proof Tree, 6 Topological Logic Wafer, 12 Femtoscale Transistor, 8000 mB Dark Matter → 1 Power Set (4,096 FE/t, 25 s)
  - 12 Functor Bridge, 6 Klein Bottle, 6 Continuum Shard, 6 Godel Sentence, 6 Entangled Bus Bar, 64000 mB Synaptic Fluid, 8000 mB Relativity Breakdown → 1 Topos (262,144 FE/t, 75 s)  ⟶  12 Functor Bridge, 6 Klein Bottle, 6 Continuum Shard, 6 Godel Sentence, 6 Entangled Bus Bar, 64000 mB Synaptic Fluid, 8000 mB Nanite Slurry → 1 Topos (262,144 FE/t, 75 s)
- **Fractionation Spire**
  - 16 Chronoplastic Gasket, 16 Entangled Bus Bar, 8 Negative Mass Ingot, 16 Attoscale Capacitor, 8 Chronosteel Singularity, 16 Modular Form, 16 Morphogenic Serum, 64000 mB Dimensional Residue, 16000 mB Exotic Solder, 16000 mB Bose-Einstein Condensate → 1 Spacetime Slag, 400 mB Dark Matter, 250 mB Exotic Matter, 100 mB Inflaton Field, 200 mB Chronon Gas (524,288 FE/t, 45 s)  ⟶  16 Chronoplastic Gasket, 16 Entangled Bus Bar, 8 White Dwarf Core, 32 Superconducting Coil, 8 Glowstone Singularity, 64000 mB Dimensional Residue, 16000 mB Nutrient Broth, 16000 mB Exotic Solder → 1 Spacetime Slag, 400 mB Dark Matter, 250 mB Exotic Matter, 100 mB Inflaton Field, 200 mB Chronon Gas (524,288 FE/t, 45 s)
  - 8 Quantum Processor, 16 Photonic Logic Wafer, 8 Qubit Die, 32 Cherenkov Quartz, 8 Dipole Winding, 16 RF Resonator Cell, 256000 mB Warm Helium, 16000 mB Neutrino Flux, 16000 mB Bose-Einstein Condensate → 3800 mB Superfluid Helium (65,536 FE/t, 30 s)  ⟶  8 Narrow AI Core, 32 Entangled Photon Pair, 8 Scintillator Crystal, 16 Femtoscale Transistor, 8 Hawking Absorber Plate, 256000 mB Warm Helium, 16000 mB Synaptic Fluid, 16000 mB Dark Matter → 3800 mB Superfluid Helium (65,536 FE/t, 30 s)
- **Gene Sequencer**
  - 12 Folded Protein Crystal, 12 Entangled Photon Pair, 24 Feather, 6 Entangled Bus Bar, 16000 mB Nutrient Broth, 8000 mB Superconducting Solder → 1 Cryptochrome Magnetoreceptor (131,072 FE/t, 25 s)  ⟶  12 Folded Protein Crystal, 12 Entangled Photon Pair, 24 Feather, 6 Entangled Bus Bar, 16000 mB Nutrient Broth, 8000 mB Ionic Liquid → 1 Cryptochrome Magnetoreceptor (131,072 FE/t, 25 s)
  - 6 Recombinant DNA, 12 Molecular Orbital Lattice, 6 Quantum Processor, 6 Femtoscale Inductor, 16000 mB Nutrient Broth, 8000 mB Quark-Gluon Plasma → 2 Folded Protein Crystal (65,536 FE/t, 25 s)  ⟶  6 Recombinant DNA, 12 Molecular Orbital Lattice, 6 Quantum Processor, 6 Carbon Nanotube Bundle, 16000 mB Nutrient Broth, 8000 mB Ionic Liquid → 2 Folded Protein Crystal (65,536 FE/t, 25 s)
  - 24 Genome Sample, 6 Engineered Enzyme, 6 Femtoscale Inductor, 24 Chiral Crystal, 16000 mB Nutrient Broth, 8000 mB Superfluid Helium → 2 Recombinant DNA (16,384 FE/t, 25 s)  ⟶  24 Genome Sample, 6 Engineered Enzyme, 6 Error-Correction Lattice, 24 Crystalline Matrix Ingot, 16000 mB Nutrient Broth, 8000 mB Tachyon Flux → 2 Recombinant DNA (16,384 FE/t, 25 s)
  - 24 Xenogenome Sample, 24 Bulk Crystal, 6 Minimal Synthetic Cell, 6 Entangled Bus Bar, 16000 mB Xenoplasm, 8000 mB Synaptic Fluid → 2 Silicon Life Spore (524,288 FE/t, 50 s)  ⟶  24 Xenogenome Sample, 24 Bulk Crystal, 6 Minimal Synthetic Cell, 6 Entangled Bus Bar, 16000 mB Xenoplasm, 8000 mB Tachyon Flux → 2 Silicon Life Spore (524,288 FE/t, 50 s)
  - 12 Recombinant DNA, 12 Buckyball Cage, 6 Self-Healing Polymer, 6 Entangled Bus Bar, 32000 mB Nutrient Broth, 8000 mB Superconducting Solder → 1 Minimal Synthetic Cell (262,144 FE/t, 37.5 s)  ⟶  12 Recombinant DNA, 12 Buckyball Cage, 6 Self-Healing Polymer, 6 Entangled Bus Bar, 32000 mB Nutrient Broth, 8000 mB Strange Matter → 1 Minimal Synthetic Cell (262,144 FE/t, 37.5 s)
- **Graviton Condenser**
  - 12 Negative Mass Ingot, 24 Spacetime Alloy Ingot, 12 Spinor Lattice, 64000 mB Exotic Matter → 1 Spacetime Anchor Core (32,768 FE/t, 52.5 s)  ⟶  12 Negative Mass Ingot, 24 Spacetime Alloy Ingot, 12 Quartz Singularity, 64000 mB Exotic Matter → 1 Spacetime Anchor Core (32,768 FE/t, 52.5 s)
  - 12 Micro Singularity, 12 Quantum String, 12 Spinor Lattice, 128000 mB Hawking Radiation → 4 Graviton Crystal (65,536 FE/t, 70 s)  ⟶  1 Micro Singularity, 1 Quantum String, 12 Computronium Singularity, 1 mB Hawking Radiation → 32 Graviton Crystal (1 FE/t, 70 s)
  - 24 Graviton Crystal, 12 Void Lattice Shard, 12 Degenerate Armour Plate, 128000 mB Exotic Matter → 1 Rift Anchor (131,072 FE/t, 52.5 s)  ⟶  24 Graviton Crystal, 12 Void Lattice Shard, 12 Hyperdimensional Boule, 128000 mB Exotic Matter → 1 Rift Anchor (131,072 FE/t, 52.5 s)
  - 24 Graviton Crystal, 12 Qubit, 12 Hyperon Crystal, 64000 mB Bose-Einstein Condensate → 2 Spin Foam Lattice (32,768 FE/t, 35 s)  ⟶  24 Graviton Crystal, 12 Qubit, 12 Higgs Catalyst, 64000 mB Bose-Einstein Condensate → 2 Spin Foam Lattice (32,768 FE/t, 35 s)
  - 24 Spacetime Alloy Ingot, 12 Hyperdimensional Processor, 192 Book, 32000 mB Supercritical Solvent →  (663,552 FE/t, 122.5 s)  ⟶  24 Spacetime Alloy Ingot, 12 Hyperdimensional Processor, 192 Book, 32000 mB Stellar Plasma →  (663,552 FE/t, 122.5 s)
  - 12 M-Brane, 12 Graviton Crystal, 12 Compactified Circle, 128000 mB Inflaton Field → 1 Universe Seed (262,144 FE/t, 105 s)  ⟶  12 M-Brane, 12 Graviton Crystal, 12 Hyperdimensional Logic Wafer, 128000 mB Inflaton Field → 1 Universe Seed (262,144 FE/t, 105 s)
  - 96 Virtual Graviton, 12 Riemann Curvature Tensor, 48 Integral Operator, 64000 mB Superfluid Helium → 2 Graviton Crystal (262,144 FE/t, 17.5 s)  ⟶  96 Virtual Graviton, 12 Bounce Core, 48 Neutronium Plate, 64000 mB Superfluid Helium → 2 Graviton Crystal (262,144 FE/t, 17.5 s)
- **Higgs Field Modulator**
  - 8 Negative Mass Ingot, 8 Superconducting Coil, 8 Chronoplastic Gasket, 16000 mB Nanite Slurry → 1 Inertia Damper (65,536 FE/t, 30 s)  ⟶  8 Negative Mass Ingot, 8 Superconducting Coil, 8 Antiproton Bundle, 16000 mB Nanite Slurry → 1 Inertia Damper (65,536 FE/t, 30 s)
  - 16 Neutronium Ingot, 8 Sterile Neutrino, 8 Qubit Array Wafer, 32000 mB Bose-Einstein Condensate → 2 Massless Alloy (65,536 FE/t, 30 s)  ⟶  16 Neutronium Ingot, 8 Sterile Neutrino, 8 Real Continuum, 32000 mB Bose-Einstein Condensate → 2 Massless Alloy (65,536 FE/t, 30 s)
  - 8 Massless Alloy, 8 Exotic Matter Crystal, 8 Chronoplastic Gasket, 32000 mB Exotic Matter → 1 Negative Mass Ingot (131,072 FE/t, 45 s)  ⟶  8 Massless Alloy, 8 Exotic Matter Crystal, 8 Claytronic Catom, 32000 mB Exotic Matter → 1 Negative Mass Ingot (131,072 FE/t, 45 s)
  - 32 Virtual Monopole, 8 Chronoplastic Gasket, 8 Quark Star Core, 32000 mB Superfluid Helium → 1 Magnetic Monopole (262,144 FE/t, 15 s)  ⟶  32 Virtual Monopole, 8 Chronoplastic Gasket, 8 Unbihexium Ingot, 32000 mB Superfluid Helium → 5 Magnetic Monopole (262,144 FE/t, 15 s)
- **Holographic Encoder**
  - 8 Holographic Matrix, 8 Hawking Emitter, 8 Chronoplastic Gasket, 64000 mB Hawking Radiation → 1 Bekenstein Crystal (262,144 FE/t, 45 s)  ⟶  8 Holographic Matrix, 8 Hawking Emitter, 8 Positronium Hydride, 64000 mB Hawking Radiation → 1 Bekenstein Crystal (262,144 FE/t, 45 s)
  - 8 Metamaterial Sheet, 8 Quantum Processor, 8 Pixel Tracker Module, 32000 mB Bose-Einstein Condensate → 1 Holographic Matrix (65,536 FE/t, 30 s)  ⟶  8 Metamaterial Sheet, 8 Quantum Processor, 8 Femtoscale Inductor, 32000 mB Bose-Einstein Condensate → 1 Holographic Matrix (65,536 FE/t, 30 s)
  - 16 Chronosteel Ingot, 8 Topological Processor, 64 Book, 16000 mB Bose-Einstein Condensate →  (110,592 FE/t, 90 s)  ⟶  16 Chronosteel Ingot, 8 Topological Processor, 64 Book, 16000 mB Carborane Superacid →  (110,592 FE/t, 90 s)
- **Hyperion Synchrotron**
  - 16 Strange & Charm Quarks, 8 Pentaquark Shard, 8 Infinitesimal, 16 Antimatter Pellet, 8 Penrose Spindle, 8 Electride Crystal, 8 Fractal Core, 64000 mB Quark-Gluon Plasma, 16000 mB Ionic Liquid, 16000 mB Strange Matter → 1 Bottom & Top Quarks (131,072 FE/t, 45 s)  ⟶  16 Strange & Charm Quarks, 8 Pentaquark Shard, 8 Fractal Set, 16 Maxwell's Demon Chip, 8 General AI Core, 32 Photonic Circuit Board, 8 Derivative Operator, 64000 mB Quark-Gluon Plasma, 16000 mB Superconducting Solder, 16000 mB Chronon Gas → 1 Bottom & Top Quarks (131,072 FE/t, 45 s)
  - 32 Qubit, 16 Strange Matter Pellet, 8 Error-Correction Lattice, 8 Scintillator Crystal, 8 Neural Lattice, 16 Empty Set, 8 Quadrupole Pole Tip, 64000 mB Quark-Gluon Plasma, 16000 mB Supercritical Solvent, 16000 mB Superfluid Helium → 1 Glueball Core (131,072 FE/t, 45 s)  ⟶  32 Qubit, 16 Strange Matter Pellet, 8 Plasma Nozzle Tip, 32 Quantum Dot, 8 Chronon Capacitor, 32 Neutronium Ingot, 8 Femtoscale Inductor, 64000 mB Quark-Gluon Plasma, 16000 mB Warm Helium, 16000 mB Neutrino Flux → 1 Glueball Core (131,072 FE/t, 45 s)
  - 16 Gauge Boson Cluster, 8 Bottom & Top Quarks, 8 Higgs Field Capacitor, 16 Entangled Bus Bar, 8 Attoscale Capacitor, 16 Empty Set, 8 Recombinant DNA, 32 Cherenkov Lattice, 128000 mB Quark-Gluon Plasma, 16000 mB Bose-Einstein Condensate, 16000 mB Tachyon Flux → 1 Higgs Boson Crystal (524,288 FE/t, 75 s)  ⟶  16 Gauge Boson Cluster, 8 Bottom & Top Quarks, 8 Higgs Field Capacitor, 16 Entangled Bus Bar, 8 Attoscale Inductor, 32 Casimir Plate, 8 Hawking Emitter, 16 Hawking Absorber Plate, 128000 mB Quark-Gluon Plasma, 16000 mB Nutrient Broth, 16000 mB Quantum-Dot Photoresist → 1 Higgs Boson Crystal (524,288 FE/t, 75 s)
  - 8 Sterile Neutrino, 16 Strange Matter Pellet, 8 Tachyon Crystal, 8 Quantum Processor, 32 Crystalline Matrix Ingot, 8 Error-Correction Lattice, 16 Fractured Essence, 8 Exotic Matter Crystal, 32000 mB Strange Matter, 16000 mB Quantum-Dot Photoresist, 16000 mB Warm Helium → 1 Hyperon Crystal, 1 Pentaquark Shard (262,144 FE/t, 60 s)  ⟶  8 Sterile Neutrino, 16 Strange Matter Pellet, 8 Tachyon Crystal, 8 Graphene Sheet, 16 Qubit Array Wafer, 8 Tachyon Klystron, 32 Natural Number Lattice, 8 Nanite Swarm, 32000 mB Strange Matter, 16000 mB Nebular Gas, 16000 mB Superfluid Helium → 1 Hyperon Crystal, 1 Pentaquark Shard (262,144 FE/t, 60 s)
  - 8 Strange Matter Pellet, 8 Antimatter Pellet, 32 Entangled Photon Pair, 8 Fractal Core, 8 Femtoscale Transistor, 8 Neural Lattice, 32 Molecular Orbital Lattice, 8 Fractal Dust, 32000 mB Quark-Gluon Plasma, 16000 mB Synaptic Fluid, 16000 mB Tachyon Flux → 2 Pentaquark Shard, 1 Magnetic Monopole (65,536 FE/t, 30 s)  ⟶  8 Strange Matter Pellet, 8 Antimatter Pellet, 32 Entangled Photon Pair, 8 Tachyon Klystron, 32 Quantum Dot, 8 Scintillator Crystal, 32 Cosmic Record Fragment, 8 Transcendental Crystal, 32000 mB Quark-Gluon Plasma, 16000 mB Bose-Einstein Condensate, 16000 mB Tachyon Flux → 2 Pentaquark Shard, 1 Magnetic Monopole (65,536 FE/t, 30 s)
  - 32 Neutronium Ingot, 16 Entangled Photon Pair, 8 Femtoscale Inductor, 32 Cosmic Record Fragment, 8 Qubit Array Wafer, 16 Topological Insulator, 8 Dipole Winding, 16000 mB Quark-Gluon Plasma, 16000 mB Tachyon Flux, 16000 mB Supercritical Solvent → 2 Strange Matter Pellet, 100 mB Strange Matter (32,768 FE/t, 22.5 s)  ⟶  32 Neutronium Ingot, 16 Entangled Photon Pair, 8 Graphene Sheet, 16 Chiral Crystal, 8 Pixel Tracker Module, 32 Casimir Plate, 8 Gravitic Lens Element, 16000 mB Quark-Gluon Plasma, 16000 mB Chronon Gas, 16000 mB Supercritical Solvent → 2 Strange Matter Pellet, 100 mB Strange Matter (32,768 FE/t, 22.5 s)
- **Inflation Chamber**
  - 16 Aeon Boundary Shard, 16 No-Boundary Seed, 32 Inflaton Condensate, 25600 mB Reheating Plasma → 2 Universe Seed (67,108,864 FE/t, 240 s)  ⟶  7 Aeon Boundary Shard, 7 No-Boundary Seed, 15 Inflaton Condensate, 11792 mB Reheating Plasma → 16 Universe Seed (30,912,655 FE/t, 240 s)
  - 16 Bounce Core, 16 Baryon Asymmetry Crystal, 32 Inflaton Condensate, 25600 mB Reheating Plasma → 1 Universe Seed (67,108,864 FE/t, 240 s)  ⟶  14 Bounce Core, 14 Baryon Asymmetry Crystal, 29 Inflaton Condensate, 23135 mB Reheating Plasma → 8 Universe Seed (60,646,444 FE/t, 240 s)
- **Kugelblitz Chamber**
  - 16 Micro Singularity, 8 Einstein Field Core, 8 Kerr Metric Shard, 16 Chronoplastic Gasket, 16 Entangled Bus Bar, 8 Last Flash of a Black Hole, 16 Emerald Singularity, 16 Primordial Harvest, 16 Hyperlattice Frame, 16 Proton Decay Residue, 64000 mB Hawking Radiation, 16000 mB Psionic Ether, 16000 mB Warm Helium → 4 Gravitational Wave Crystal (131,072 FE/t, 60 s)  ⟶  16 Micro Singularity, 8 Einstein Field Core, 8 Kerr Metric Shard, 16 Chronoplastic Gasket, 16 Entangled Bus Bar, 8 Riemann Curvature Tensor, 32 Entangled Photon Pair, 16 Cosmic Neutronium Singularity, 64000 mB Hawking Radiation, 16000 mB Antiwater, 16000 mB Liquid Antihelium → 4 Gravitational Wave Crystal (131,072 FE/t, 60 s)
  - 32 Antimatter Pellet, 16 Strange Matter Pellet, 8 Qubit Die, 32 Superposition Crystal, 8 Graphene Sheet, 32 Genome Sample, 8 Rational Field, 64000 mB Dark Matter, 16000 mB Neutrino Flux, 16000 mB Bose-Einstein Condensate → 1 Kugelblitz Seed (262,144 FE/t, 60 s)  ⟶  32 Antimatter Pellet, 16 Strange Matter Pellet, 16 Photonic Processor, 32 Geoid Model, 16 Photonic Logic Wafer, 32 Metamaterial Substrate, 8 Void Lattice Shard, 64000 mB Dark Matter, 16000 mB Superconducting Solder, 16000 mB Bose-Einstein Condensate → 1 Kugelblitz Seed (262,144 FE/t, 60 s)
  - 128 Entangled Photon Pair, 16 Metamaterial Sheet, 8 Dipole Winding, 32 Spacetime Slag, 8 Carbon Nanotube Bundle, 32 Cosmic Record Fragment, 8 Femtoscale Capacitor, 64000 mB Quark-Gluon Plasma, 16000 mB Nanite Slurry, 16000 mB Ionic Liquid → 1 Micro Singularity, 250 mB Hawking Radiation (524,288 FE/t, 90 s)  ⟶  128 Entangled Photon Pair, 16 Metamaterial Sheet, 8 Tachyon Klystron, 8 Topological Wafer, 8 Quadrupole Pole Tip, 8 Topological Boule, 8 Narrow AI Core, 64000 mB Quark-Gluon Plasma, 16000 mB Bose-Einstein Condensate, 16000 mB Supercritical Solvent → 1 Micro Singularity, 250 mB Hawking Radiation (524,288 FE/t, 90 s)
  - 8 Kugelblitz Seed, 64 Entangled Photon Pair, 16 Entangled Bus Bar, 8 Attoscale Inductor, 8 Topological Logic Wafer, 8 Up & Down Quarks, 8 Infinitesimal, 32000 mB Quark-Gluon Plasma, 16000 mB Strange Matter, 16000 mB Chronon Gas → 2 Micro Singularity, 500 mB Hawking Radiation (524,288 FE/t, 60 s)  ⟶  8 Kugelblitz Seed, 64 Entangled Photon Pair, 8 Lepton Triplet, 32 Cherenkov Lattice, 8 Noble Gas Clathrate, 16 Void Essence, 8 Fractal Set, 32000 mB Quark-Gluon Plasma, 16000 mB Ionic Liquid, 16000 mB Chronon Gas → 3 Micro Singularity, 500 mB Hawking Radiation (524,288 FE/t, 60 s)
- **Lattice Growth Vat**
  - 4 Lattice Seed Crystal, 32 Cherenkov Quartz, 4 Casimir Plate, 16000 mB Superfluid Helium → 1 Cherenkov Boule, 1000 mB Warm Helium (512 FE/t, 60 s)  ⟶  4 Lattice Seed Crystal, 32 Cherenkov Quartz, 8 Electrical Steel Lamination, 16000 mB Superfluid Helium → 1 Cherenkov Boule, 1000 mB Warm Helium (512 FE/t, 60 s)
  - 4 Topological Insulator, 16 Cherenkov Quartz, 8 Void Lattice Shard, 4000 mB Superfluid Helium → 1 RF Resonator Cell, 250 mB Warm Helium (1,024 FE/t, 20 s)  ⟶  4 Topological Insulator, 16 Cherenkov Quartz, 16 Natural Number Lattice, 4000 mB Superfluid Helium → 1 RF Resonator Cell, 250 mB Warm Helium (1,024 FE/t, 20 s)
  - 16 Cherenkov Quartz, 8 Quantum Dot, 8 Dilithium Crystal, 4000 mB Superfluid Helium → 2 Scintillator Crystal, 250 mB Warm Helium (768 FE/t, 20 s)  ⟶  16 Cherenkov Quartz, 8 Quantum Dot, 8 Cosmic Record Fragment, 4000 mB Superfluid Helium → 2 Scintillator Crystal, 250 mB Warm Helium (768 FE/t, 20 s)
  - 4 Lattice Seed Crystal, 8 Topological Insulator, 16 Crystalline Matrix Ingot, 16000 mB Bose-Einstein Condensate → 1 Topological Boule (8,192 FE/t, 80 s)  ⟶  4 Lattice Seed Crystal, 8 Topological Insulator, 4 Integer Ring, 16000 mB Bose-Einstein Condensate → 1 Topological Boule (8,192 FE/t, 80 s)
- **Matrioshka Brain**
  - 256 Qubit, 16 Computronium Ingot, 16 D-Brane Sheet, 64 Chronosteel Singularity, 16 False Vacuum Membrane, 16 Einstein Field Core, 16 Hyperdimensional Pearl, 64 Lorentz Transform Crystal, 16 Hyperdimensional Die, 256000 mB Synaptic Fluid, 64000 mB Inflaton Field, 64000 mB Bose-Einstein Condensate, 64000 mB Warm Helium → 16 Superposition Crystal (8,388,608 FE/t, 120 s)  ⟶  1 Qubit, 1 Computronium Ingot, 16 Silicon Life Spore, 32 Graviton Crystal, 16 Hyperdimensional Die, 32 Degenerate Armour Plate, 16 Virtual Graviton, 32 Metallic Hydrogen Ingot, 16 Superintelligence Core, 1 mB Synaptic Fluid, 64000 mB Antihydrogen, 64000 mB Hawking Radiation, 64000 mB Psionic Ether → 64 Superposition Crystal (1 FE/t, 120 s)
  - 32 Computronium Ingot, 16 Hyperdimensional Pearl, 64 Engineered Enzyme, 16 Virtual Quark, 64 Xenobot Swarm, 16 Brane Tension Fiber, 64 Cherenkov Lattice, 16 False Vacuum Membrane, 128000 mB Synaptic Fluid, 64000 mB Warm Helium, 64000 mB Inflaton Field, 64000 mB Ionic Liquid → 8 Quantum Processor (4,194,304 FE/t, 40 s)  ⟶  9 Computronium Ingot, 16 Cosmic Circuit Board, 64 Electrical Steel Lamination, 16 Kaluza-Klein Singularity, 64 General AI Core, 16 Virtual Monopole, 32 Empirical Notes II, 16 String Coupling Crystal, 37048 mB Synaptic Fluid, 64000 mB Chronon Gas, 64000 mB Liquid Antihelium, 64000 mB Warm Helium → 64 Quantum Processor (1,213,997 FE/t, 40 s)
  - 64 General AI Core, 128 Computronium Ingot, 16 G₂ Manifold, 64 Transcendental Crystal, 16 Virtual Photon, 64 Mirror Alloy Ingot, 16 Graviton Wisp Core, 64 Prime Shard, 16 Hyperdimensional Logic Wafer, 1024000 mB Synaptic Fluid, 64000 mB Dark Matter, 64000 mB Antiwater, 64000 mB Nanite Slurry → 2 Superintelligence Core (33,554,432 FE/t, 240 s)  ⟶  64 General AI Core, 128 Computronium Ingot, 16 Kaluza-Klein Singularity, 64 Quantum Field Lattice, 16 Rift Anchor, 64 Novikov Crystal, 16 Neural Organoid, 64 Qubit Die, 16 Hyperdimensional Die, 1024000 mB Synaptic Fluid, 64000 mB Relativity Breakdown, 64000 mB Antihydrogen, 64000 mB Ionic Liquid → 2 Superintelligence Core (33,554,432 FE/t, 240 s)
- **Maxwell's Demon Sorter**
  - 64 Bulk Stone, 4 Coherent Wiring Harness, 4000 mB Superfluid Helium → 2 Void Lattice Shard, 1 Kaluza-Klein Shard (65,536 FE/t, 15 s)  ⟶  64 Bulk Stone, 4 Coherent Wiring Harness, 4000 mB Superfluid Helium → 3 Void Lattice Shard, 1 Kaluza-Klein Shard (65,536 FE/t, 15 s)
  - 32 Fractured Essence, 8 Void Lattice Shard, 4000 mB Superfluid Helium → 2 Degenerate Matter Chunk, 1 Strange Matter Pellet, 250 mB Warm Helium (32,768 FE/t, 15 s)  ⟶  32 Fractured Essence, 8 Photonic Lens, 4000 mB Superfluid Helium → 2 Degenerate Matter Chunk, 1 Strange Matter Pellet, 250 mB Warm Helium (32,768 FE/t, 15 s)
  - 4 Cherenkov Quartz, 4 Qubit, 8 Entangled Photon Pair, 8000 mB Superfluid Helium → 1 Maxwell's Demon Chip (2,048 FE/t, 15 s)  ⟶  4 Cherenkov Quartz, 4 Qubit, 8 Telescope Mirror Blank, 8000 mB Superfluid Helium → 1 Maxwell's Demon Chip (2,048 FE/t, 15 s)
  - 16 Spacetime Slag, 4 Metamaterial Substrate, 4000 mB Superfluid Helium → 4 Void Lattice Shard, 1 Planck Shard, 250 mB Warm Helium (65,536 FE/t, 20 s)  ⟶  16 Spacetime Slag, 8 Quantum Dot, 4000 mB Superfluid Helium → 4 Void Lattice Shard, 1 Planck Shard, 250 mB Warm Helium (65,536 FE/t, 20 s)
- **Metamaterial Weaver**
  - 4 Metamaterial Sheet, 4 Void Lattice Shard, 4 Coherent Wiring Harness, 8000 mB Dark Matter → 1 Dark Matter Lens (4,096 FE/t, 20 s)  ⟶  4 Metamaterial Sheet, 4 Void Lattice Shard, 4 Coherent Wiring Harness, 8000 mB Dark Matter → 2 Dark Matter Lens (4,096 FE/t, 20 s)
  - 12 Superconducting Coil, 8 Neutronium Plate, 4 Metamaterial Sheet, 4 Cherenkov Boule, 8000 mB Superfluid Helium → 1 Dipole Winding (1,024 FE/t, 15 s)  ⟶  12 Superconducting Coil, 8 Neutronium Plate, 4 Metamaterial Sheet, 8 Superposition Crystal, 8000 mB Superfluid Helium → 1 Dipole Winding (1,024 FE/t, 15 s)
  - 16 Stellar Iron Ingot, 8 Metamaterial Sheet, 4 Higgs Field Capacitor, 4 Coherent Wiring Harness, 8 Diamond Singularity, 16000 mB Stellar Plasma → 1 Dyson Shell Segment (8,192 FE/t, 20 s)  ⟶  16 Stellar Iron Ingot, 8 Metamaterial Sheet, 4 Higgs Field Capacitor, 4 Coherent Wiring Harness, 8 Tachyon Singularity, 16000 mB Stellar Plasma → 1 Dyson Shell Segment (8,192 FE/t, 20 s)
  - 16 Horizon Glass, 4 Metamaterial Sheet, 4 Coherent Wiring Harness, 4000 mB Dark Matter → 2 Dark Matter Lens (65,536 FE/t, 15 s)  ⟶  16 Horizon Glass, 4 Metamaterial Sheet, 4 Coherent Wiring Harness, 4000 mB Dark Matter → 3 Dark Matter Lens (65,536 FE/t, 15 s)
  - 4 Neutronium Plate, 8 Quantum Dot, 4 Cherenkov Quartz, 4 Crystalline Matrix Ingot, 1600 mB Bose-Einstein Condensate → 1 Metamaterial Sheet (768 FE/t, 15 s)  ⟶  4 Neutronium Plate, 8 Quantum Dot, 4 Cherenkov Quartz, 8 Entangled Photon Pair, 1600 mB Bose-Einstein Condensate → 1 Metamaterial Sheet (768 FE/t, 15 s)
  - 4 Metamaterial Substrate, 4 Superconducting Coil, 8 Cherenkov Quartz, 8 Entangled Photon Pair, 4000 mB Quantum-Dot Photoresist → 1 Photonic Circuit Board (512 FE/t, 15 s)  ⟶  4 Metamaterial Substrate, 4 Superconducting Coil, 8 Cherenkov Quartz, 8 Neutronium Plate, 4000 mB Quantum-Dot Photoresist → 1 Photonic Circuit Board (512 FE/t, 15 s)
  - 4 Photonic Circuit Board, 8 Photonic Die, 8 Quantum Dot, 4 Coherent Wiring Harness, 1152 mB Superconducting Solder → 2 Photonic Processor (768 FE/t, 15 s)  ⟶  4 Photonic Circuit Board, 8 Photonic Die, 8 Quantum Dot, 16 Qubit, 1152 mB Superconducting Solder → 3 Photonic Processor (768 FE/t, 15 s)
  - 4 Photonic Circuit Board, 16 Photonic Die, 16 Quantum Dot, 4 Photonic Lens, 4000 mB Superfluid Helium → 1 Photonic Processor (768 FE/t, 30 s)  ⟶  4 Photonic Circuit Board, 16 Photonic Die, 16 Quantum Dot, 16 Spectral Plate, 4000 mB Superfluid Helium → 3 Photonic Processor (768 FE/t, 30 s)
  - 4 Neutronium Plate, 16 c:ingots/copper, 4 Superposition Crystal, 8 Degenerate Matter Chunk, 1600 mB Superfluid Helium → 2 Superconducting Coil (512 FE/t, 12 s)  ⟶  4 Neutronium Plate, 16 c:ingots/copper, 8 Geiger Tube, 8 Raw Pitchblende, 1600 mB Superfluid Helium → 2 Superconducting Coil (512 FE/t, 12 s)
  - 8 Void Essence, 4 Metamaterial Sheet, 4 Coherent Wiring Harness, 4000 mB Dark Matter → 1 Dark Matter Lens (8,192 FE/t, 15 s)  ⟶  8 Void Essence, 4 Metamaterial Sheet, 4 Coherent Wiring Harness, 4000 mB Dark Matter → 2 Dark Matter Lens (8,192 FE/t, 15 s)
- **Mirror Matter Foundry**
  - 24 Raw Anti-Gold, 12 Recombinant DNA, 12 Claytronic Catom, 12 Formal Proof Tree, 12 Hawking Absorber Plate, 12 Quaternion Crystal, 24 Empty Set, 12800 mB Liquid Antihelium, 32000 mB Ionic Liquid, 32000 mB Exotic Solder → 2 Anti-Gold Ingot (4,194,304 FE/t, 26.25 s)  ⟶  24 Raw Anti-Gold, 24 Entangled Bus Bar, 24 Photonic Neuron, 12 Raw Anti-Iron, 24 Topological Wafer, 12 Pentaquark Shard, 48 Void Essence, 12800 mB Liquid Antihelium, 32000 mB Exotic Solder, 32000 mB Synaptic Fluid → 2 Anti-Gold Ingot (4,194,304 FE/t, 26.25 s)
  - 24 Raw Anti-Iron, 12 Formal Proof Tree, 48 Neutronium Ingot, 12 Integral Operator, 48 Zeolite Catalyst, 12 Attoscale Inductor, 24 Topological Insulator, 12800 mB Liquid Antihelium, 32000 mB Nutrient Broth, 32000 mB Nebular Gas → 2 Anti-Iron Ingot (4,194,304 FE/t, 26.25 s)  ⟶  24 Raw Anti-Iron, 12 Antisilicon Shard, 24 Femtoscale Inductor, 12 Antiproton Bundle, 12 Penrose Spindle, 12 Muonic Catalyst, 24 Tachyon Crystal, 12800 mB Liquid Antihelium, 32000 mB Superfluid Helium, 32000 mB Hawking Radiation → 2 Anti-Iron Ingot (4,194,304 FE/t, 26.25 s)
  - 48 Anti-Gold Ingot, 48 Mirror Alloy Ingot, 24 Arrow of Time Shard, 12 Mirror Processor, 24 Chronoplastic Gasket, 12 Anticarbon Crystal, 24 Topological Boule, 12 Hawking Emitter, 24 Imaginary Unit, 12 Antiquark Shard, 24 Cherenkov Singularity, 24 Raw Anti-Gold, 128000 mB Liquid Antihelium, 512000 mB Antihydrogen, 32000 mB Superconducting Solder, 32000 mB Synaptic Fluid → 1 Baryogenesis Seed (268,435,456 FE/t, 105 s)  ⟶  48 Anti-Gold Ingot, 48 Mirror Alloy Ingot, 24 Arrow of Time Shard, 12 Mirror Processor, 24 Chronoplastic Gasket, 12 Chronosteel Singularity, 12 Anticarbon Crystal, 12 Octonion Crystal, 24 Penrose Spindle, 12 Folded Protein Crystal, 128000 mB Liquid Antihelium, 512000 mB Antihydrogen, 32000 mB Nanite Slurry, 32000 mB Chronon Gas → 1 Baryogenesis Seed (268,435,456 FE/t, 105 s)
  - 24 Antiproton Bundle, 12 Antihelium Nucleus, 12 Glueball Core, 48 Neutronium Ingot, 12 Assembly Matrix, 24 Empty Set, 12 Hawking Absorber Plate, 48 Molecular Orbital Lattice, 512000 mB Antihydrogen, 32000 mB Nutrient Broth, 32000 mB Exotic Solder → 1000 mB Liquid Antihelium (8,388,608 FE/t, 35 s)  ⟶  24 Antiproton Bundle, 12 Antihelium Nucleus, 12 Strange & Charm Quarks, 24 Nanite Swarm, 24 Entangled Bus Bar, 24 Fractal Core, 12 Pentaquark Shard, 12 Engineered Enzyme, 512000 mB Antihydrogen, 32000 mB Exotic Solder, 32000 mB Hawking Radiation → 1000 mB Liquid Antihelium (8,388,608 FE/t, 35 s)
  - 96 Antiproton Bundle, 12 Topological Circuit Board, 24 Fractured Essence, 12 Fourier Series, 24 Strange Matter Pellet, 12 Strange & Charm Quarks, 24 Qubit Die, 1024000 mB Antihydrogen, 32000 mB Strange Matter, 32000 mB Chronon Gas → 500 mB Liquid Antihelium (8,388,608 FE/t, 70 s)  ⟶  96 Antiproton Bundle, 12 Category Lattice, 48 Quantum Random Number Chip, 12 Attoscale Transistor, 12 Causal Shield Lattice, 12 Riemann Zeta Crystal, 12 Carbon Nanotube Bundle, 1024000 mB Antihydrogen, 32000 mB Superfluid Helium, 32000 mB Strange Matter → 1817 mB Liquid Antihelium (8,388,608 FE/t, 70 s)
  - 12 Anti-Iron Ingot, 12 Neutronium Ingot, 24 Superconducting Coil, 24 Chronoplastic Gasket, 12 Category Lattice, 48 Cryocooler Unit, 12 Self-Healing Polymer, 24 Qubit Die, 12 Antiquark Shard, 32000 mB Liquid Antihelium, 32000 mB Superconducting Solder, 32000 mB Hawking Radiation → 1 Mirror Alloy Ingot (16,777,216 FE/t, 35 s)  ⟶  12 Anti-Iron Ingot, 12 Neutronium Ingot, 24 Superconducting Coil, 24 Chronoplastic Gasket, 12 Obsidian Singularity, 24 Annihilation Core, 12 Blue Ice Singularity, 24 Magnetic Monopole, 12 Dilithium Singularity, 24 Cryptochrome Magnetoreceptor, 24 Morphogenic Serum, 32000 mB Liquid Antihelium, 32000 mB Superconducting Solder, 32000 mB Nutrient Broth → 1 Mirror Alloy Ingot (16,777,216 FE/t, 35 s)
  - 48 Antisilicon Shard, 12 Anticarbon Crystal, 12 Antiquark Shard, 12 Quantum Processor, 12 Integral Operator, 24 Empty Set, 12 Up & Down Quarks, 48 Metamaterial Substrate, 12 Attoscale Inductor, 48 Photonic Lens, 32000 mB Liquid Antihelium, 32000 mB Exotic Solder, 32000 mB Quantum-Dot Photoresist → 1 Mirror Processor (16,777,216 FE/t, 35 s)  ⟶  48 Antisilicon Shard, 12 Anticarbon Crystal, 12 Antiquark Shard, 12 Quantum Processor, 12 Raw Anti-Gold, 24 Imaginary Unit, 12 Attoscale Transistor, 12 Noble Gas Clathrate, 12 Protostar Core, 24 Scintillator Crystal, 32000 mB Liquid Antihelium, 32000 mB Neutrino Flux, 32000 mB Chronon Gas → 1 Mirror Processor (16,777,216 FE/t, 35 s)
- **Multiverse Rift Stabilizer**
  - 16 Multiverse Shard, 16 Rift Anchor, 64 Fractal Set, 16 Hyperdimensional Processor, 64 Holographic Matrix, 16 Gravitational Wave Crystal, 64 Neutronium Ingot, 16 D-Brane Sheet, 128000 mB Exotic Matter, 64000 mB Hawking Radiation, 64000 mB Carborane Superacid, 64000 mB Xenoplasm → 32 Cherenkov Quartz, 16 Degenerate Matter Chunk, 16 Void Lattice Shard, 4 Echo Shard (2,097,152 FE/t, 80 s)  ⟶  16 Multiverse Shard, 16 Kaluza-Klein Shard, 64 Copper Singularity, 16 Christoffel Symbol, 64 Power Set, 16 Path Integral, 64 Obsidian Singularity, 16 Rift Anchor, 128000 mB Exotic Matter, 64000 mB Inflaton Field, 64000 mB Psionic Ether, 64000 mB Superfluid Helium → 32 Cherenkov Quartz, 16 Degenerate Matter Chunk, 16 Void Lattice Shard, 4 Echo Shard (2,097,152 FE/t, 80 s)
  - 16 Rift Anchor, 16 M-Brane, 16 False Vacuum Membrane, 64 Antiquark Shard, 16 Kaluza-Klein Singularity, 64 Arrow of Time Shard, 16 D-Brane Sheet, 64 Superposition Crystal, 16 Hyperdimensional Die, 256000 mB Exotic Matter, 64000 mB Causal Solder, 64000 mB Nutrient Broth, 64000 mB Tachyon Flux → 4 Multiverse Shard, 8 Void Lattice Shard, 2 Exotic Matter Crystal, 1000 mB Inflaton Field (8,388,608 FE/t, 240 s)  ⟶  16 Rift Anchor, 16 M-Brane, 16 False Vacuum Membrane, 32 Biosphere Compendium, 16 Cosmic Circuit Board, 64 Void Essence, 16 Graviton Wisp Core, 64 Holographic Matrix, 16 Virtual Monopole, 256000 mB Exotic Matter, 64000 mB Chronon Gas, 64000 mB Nanite Slurry, 64000 mB Superfluid Helium → 4 Multiverse Shard, 8 Void Lattice Shard, 2 Exotic Matter Crystal, 1000 mB Inflaton Field (8,388,608 FE/t, 240 s)
- **Nanite Assembler**
  - 6 Programmable Matter, 12 Superposition Crystal, 6 Sterile Neutrino, 24 Cherenkov Wafer, 8000 mB Nanite Slurry, 8000 mB Synaptic Fluid → 4 Claytronic Catom (32,768 FE/t, 18.75 s)  ⟶  6 Programmable Matter, 12 Superposition Crystal, 6 Proof Fragment, 24 Sediment Core, 8000 mB Nanite Slurry, 8000 mB Tachyon Flux → 4 Claytronic Catom (32,768 FE/t, 18.75 s)
  - 12 Quantum Dot, 6 Neutronium Plate, 6 Maxwell's Demon Chip, 12 Photonic Lens, 8000 mB Superfluid Helium, 8000 mB Supercritical Solvent → 4 Nanite Swarm (8,192 FE/t, 18.75 s)  ⟶  12 Quantum Dot, 6 Neutronium Plate, 6 Maxwell's Demon Chip, 6 Chiral Crystal, 8000 mB Superfluid Helium, 8000 mB Supercritical Solvent → 4 Nanite Swarm (8,192 FE/t, 18.75 s)
  - 12 Field Calibrator, 48 Superconducting Coil, 24 Quantum Processor, 24 Chronon Capacitor, 8000 mB Synaptic Fluid → 1 Phase-Locked Stabiliser (65,536 FE/t, 37.5 s)  ⟶  12 Field Calibrator, 48 Superconducting Coil, 24 Quantum Processor, 24 Chronon Capacitor, 8000 mB Warm Helium → 1 Phase-Locked Stabiliser (65,536 FE/t, 37.5 s)
  - 24 Nanite Swarm, 6 Metamaterial Sheet, 6 Qubit, 6 Photonic Neuron, 16000 mB Nanite Slurry, 8000 mB Superfluid Helium → 1 Programmable Matter (32,768 FE/t, 25 s)  ⟶  24 Nanite Swarm, 6 Metamaterial Sheet, 6 Qubit, 6 Real Continuum, 16000 mB Nanite Slurry, 8000 mB Superfluid Helium → 1 Programmable Matter (32,768 FE/t, 25 s)
  - 12 Quine Replicator, 12 Claytronic Catom, 6 Quantum Processor, 8000 mB Nanite Slurry → 1 Von Neumann Probe (1,048,576 FE/t, 75 s)  ⟶  1 Quine Replicator, 1 Claytronic Catom, 1 Quantum Processor, 1 mB Nanite Slurry → 8 Von Neumann Probe (1 FE/t, 75 s)
  - 24 Claytronic Catom, 6 Quantum Processor, 6 Dyson Swarm Satellite, 6 Entangled Bus Bar, 64000 mB Nanite Slurry, 8000 mB Bose-Einstein Condensate → 1 Von Neumann Probe (1,048,576 FE/t, 75 s)  ⟶  24 Claytronic Catom, 6 Quantum Processor, 6 Dyson Swarm Satellite, 6 Entangled Bus Bar, 64000 mB Nanite Slurry, 8000 mB Exotic Matter → 1 Von Neumann Probe (1,048,576 FE/t, 75 s)
- **Neural Synthesizer**
  - 12 Neutronium Ingot, 6 Quantum Processor, 6 Photonic Neuron, 6 Nanite Swarm, 8000 mB Synaptic Fluid, 8000 mB Chronon Gas → 2 Computronium Ingot (65,536 FE/t, 25 s)  ⟶  12 Neutronium Ingot, 6 Quantum Processor, 6 Photonic Neuron, 6 Void Essence, 8000 mB Synaptic Fluid, 8000 mB Quantum-Dot Photoresist → 2 Computronium Ingot (65,536 FE/t, 25 s)
  - 48 Qubit, 6 Superposition Crystal, 6 Quantum Processor, 6 Quadrupole Pole Tip, 8000 mB Synaptic Fluid, 8000 mB Superfluid Helium → 1 Error-Correction Lattice (4,096 FE/t, 25 s)  ⟶  48 Qubit, 6 Superposition Crystal, 6 Quantum Processor, 6 Qubit Array Wafer, 8000 mB Synaptic Fluid, 8000 mB Bose-Einstein Condensate → 1 Error-Correction Lattice (4,096 FE/t, 25 s)
  - 24 Narrow AI Core, 24 Superposition Crystal, 12 Formal Proof Tree, 6 Higgs Field Capacitor, 64000 mB Synaptic Fluid, 8000 mB Supercritical Solvent → 1 General AI Core (65,536 FE/t, 75 s)  ⟶  24 Narrow AI Core, 24 Superposition Crystal, 12 Formal Proof Tree, 6 Function Graph, 64000 mB Synaptic Fluid, 8000 mB Bose-Einstein Condensate → 1 General AI Core (65,536 FE/t, 75 s)
  - 6 Neural Lattice, 12 Quantum Processor, 6 Photonic Neuron, 24 Cherenkov Quartz, 32000 mB Synaptic Fluid, 8000 mB Bose-Einstein Condensate → 1 Narrow AI Core (32,768 FE/t, 37.5 s)  ⟶  6 Neural Lattice, 12 Quantum Processor, 6 Strange Matter Pellet, 24 Cherenkov Lattice, 32000 mB Synaptic Fluid, 8000 mB Superfluid Helium → 1 Narrow AI Core (32,768 FE/t, 37.5 s)
  - 48 Photonic Neuron, 6 Metamaterial Sheet, 12 Planck Shard, 24 Entangled Photon Pair, 16000 mB Synaptic Fluid, 8000 mB Bose-Einstein Condensate → 1 Neural Lattice (16,384 FE/t, 25 s)  ⟶  48 Photonic Neuron, 6 Metamaterial Sheet, 24 Quantum Random Number Chip, 24 Geiger Tube, 16000 mB Synaptic Fluid, 8000 mB Superfluid Helium → 1 Neural Lattice (16,384 FE/t, 25 s)
  - 12 Quantum Dot, 6 Cherenkov Quartz, 12 Metamaterial Sheet, 24 Neutronium Ingot, 3200 mB Superfluid Helium, 8000 mB Quantum-Dot Photoresist → 2 Photonic Neuron (8,192 FE/t, 12.5 s)  ⟶  12 Quantum Dot, 6 Cherenkov Quartz, 24 Lattice Seed Crystal, 24 Theodolite Lens, 3200 mB Superfluid Helium, 8000 mB Supercritical Solvent → 2 Photonic Neuron (8,192 FE/t, 12.5 s)
  - 24 General AI Core, 6 Tesseract Core, 6 Spin Foam Lattice, 12 Neural Organoid, 128000 mB Synaptic Fluid, 8000 mB Causal Solder → 1 Superintelligence Core (1,048,576 FE/t, 150 s)  ⟶  24 General AI Core, 6 Tesseract Core, 6 Spin Foam Lattice, 12 Neural Organoid, 128000 mB Synaptic Fluid, 8000 mB Supercritical Solvent → 1 Superintelligence Core (1,048,576 FE/t, 150 s)
  - 12 Neutronium Plate, 6 Quantum Processor, 192 Paper, 24 Qubit, 8000 mB Superfluid Helium →  (18,432 FE/t, 62.5 s)  ⟶  12 Neutronium Plate, 6 Quantum Processor, 192 Paper, 24 Qubit, 8000 mB Superconducting Solder →  (18,432 FE/t, 62.5 s)
- **Neutrino Condenser**
  - 12 Natural Number Lattice, 24 Degenerate Matter Chunk, 8000 mB Supercritical Solvent → 100 mB Neutrino Flux (4,096 FE/t, 12.5 s)  ⟶  12 Cryocooler Unit, 24 Magnetite, 8000 mB Superfluid Helium → 100 mB Neutrino Flux (4,096 FE/t, 12.5 s)
  - 6 Degenerate Matter Chunk, 12 Cherenkov Wafer, 24 Entangled Photon Pair, 8000 mB Warm Helium → 1 Sterile Neutrino, 250 mB Neutrino Flux (16,384 FE/t, 25 s)  ⟶  6 Degenerate Matter Chunk, 24 Metamaterial Substrate, 24 Telescope Mirror Blank, 8000 mB Bose-Einstein Condensate → 1 Sterile Neutrino, 250 mB Neutrino Flux (16,384 FE/t, 25 s)
- **Neutron Star Forge**
  - 192 Infall Rock, 12 Neutronium Plate, 24 Chronoplastic Gasket, 12 Hyperdimensional Die, 48 Power Set, 12 Dilaton Crystal, 48 Tachyon Singularity, 12 Christoffel Symbol, 32000 mB Stellar Plasma, 32000 mB Strange Matter, 32000 mB Antiwater → 8 Degenerate Matter Chunk, 1 Frozen Photon (524,288 FE/t, 26.25 s)  ⟶  1 Infall Rock, 1 Neutronium Plate, 24 Chronoplastic Gasket, 12 Cosmic Circuit Board, 48 Self-Healing Polymer, 12 Virtual Graviton, 48 Telescope Mirror Blank, 12 Silicon Life Spore, 24 Phantom Energy Cell, 28 mB Stellar Plasma, 32000 mB Tachyon Flux, 32000 mB Carborane Superacid → 64 Degenerate Matter Chunk, 1 Frozen Photon (462 FE/t, 26.25 s)
  - 192 Degenerate Matter Chunk, 12 Up & Down Quarks, 24 Electride Crystal, 12 Topological Circuit Board, 24 Tachyon Crystal, 12 Quaternion Crystal, 24 Qubit Die, 128000 mB Superfluid Helium, 32000 mB Dark Matter, 32000 mB Ionic Liquid → 2 Block of Neutronium, 1000 mB Liquid Metallic Hydrogen (1,048,576 FE/t, 70 s)  ⟶  192 Degenerate Matter Chunk, 12 Raw Anti-Gold, 48 Casimir Plate, 12 Differential Manifold, 24 Topological Wafer, 12 Up & Down Quarks, 48 Photonic Logic Wafer, 128000 mB Superfluid Helium, 32000 mB Carborane Superacid, 32000 mB Quantum-Dot Photoresist → 2 Block of Neutronium, 1000 mB Liquid Metallic Hydrogen (1,048,576 FE/t, 70 s)
  - 12 Magnetar Shard, 12 Block of Cherenkov Quartz, 12 Chronon Capacitor, 24 Chronoplastic Gasket, 12 Higgs Boson Crystal, 24 Quantum Chlorophyll, 12 Spacetime Anchor Core, 48 Maxwell's Demon Chip, 12 Stellar Iron Singularity, 64000 mB Neutrino Flux, 32000 mB Exotic Matter, 32000 mB Nanite Slurry → 2 Pulsar Timing Crystal (1,048,576 FE/t, 52.5 s)  ⟶  12 Magnetar Shard, 12 Block of Cherenkov Quartz, 12 Chronon Capacitor, 24 Chronoplastic Gasket, 12 Anti-Gold Ingot, 24 Entangled Bus Bar, 12 Higgs Catalyst, 24 Antiquark Shard, 12 Turing Morphogen, 64000 mB Neutrino Flux, 32000 mB Exotic Matter, 32000 mB Hawking Radiation → 2 Pulsar Timing Crystal (1,048,576 FE/t, 52.5 s)
  - 48 Block of Neutronium, 12 Strange Matter Pellet, 12 Supernova Remnant, 24 Chronoplastic Gasket, 12 Spin Foam Lattice, 24 Attoscale Capacitor, 12 Hyperdimensional Wafer, 24 Kugelblitz Seed, 12 Cosmic Neutronium Ingot, 256000 mB Quark-Gluon Plasma, 32000 mB Neutrino Flux, 32000 mB Antiwater → 1 Quark Star Core, 2 Magnetar Shard (2,097,152 FE/t, 105 s)  ⟶  48 Block of Neutronium, 12 Strange Matter Pellet, 12 Supernova Remnant, 24 Chronoplastic Gasket, 12 Hyperdimensional Boule, 48 Entangled Photon Pair, 12 Mirror Processor, 48 Topological Logic Wafer, 12 Quantum String, 256000 mB Quark-Gluon Plasma, 32000 mB Nanite Slurry, 32000 mB Synaptic Fluid → 1 Quark Star Core, 2 Magnetar Shard (2,097,152 FE/t, 105 s)
  - 24 Red Giant Envelope, 96 Stellar Iron Ingot, 12 Block of Neutronium, 24 Chronoplastic Gasket, 12 Void Lattice Singularity, 24 Causal Shield Lattice, 12 Exotic Crystal Singularity, 48 Zeolite Catalyst, 12 Mirror Processor, 24 Cherenkov Singularity, 128000 mB Neutrino Flux, 32000 mB Carborane Superacid, 32000 mB Supercritical Solvent → 1 Supernova Remnant, 1 Magnetar Shard (1,048,576 FE/t, 105 s)  ⟶  24 Red Giant Envelope, 96 Stellar Iron Ingot, 12 Block of Neutronium, 24 Chronoplastic Gasket, 12 White Dwarf Core, 48 Ferrite Magnet, 12 Strange Matter Singularity, 24 Femtoscale Capacitor, 12 Massless Alloy, 24 Prime Singularity, 24 Morphogenic Serum, 128000 mB Neutrino Flux, 32000 mB Bose-Einstein Condensate, 32000 mB Synaptic Fluid → 1 Supernova Remnant, 1 Magnetar Shard (1,048,576 FE/t, 105 s)
- **Nucleosynthesis Chamber**
  - 8 Protostar Core, 16 Antimatter Pellet, 8 Imaginary Unit, 256000 mB Nebular Gas, 64000 mB Quark-Gluon Plasma → 1 Stellar Core, 1000 mB Stellar Plasma (262,144 FE/t, 120 s)  ⟶  8 Protostar Core, 16 Antimatter Pellet, 8 Penrose Spindle, 256000 mB Nebular Gas, 64000 mB Quark-Gluon Plasma → 1 Stellar Core, 1000 mB Stellar Plasma (262,144 FE/t, 120 s)
  - 64 Degenerate Matter Chunk, 8 Graviton Crystal, 8 Chronoplastic Gasket, 256000 mB Nebular Gas, 16000 mB Psionic Ether → 1 Protostar Core (65,536 FE/t, 60 s)  ⟶  64 Degenerate Matter Chunk, 8 Graviton Crystal, 8 Chronoplastic Gasket, 256000 mB Nebular Gas, 16000 mB Dimensional Residue → 1 Protostar Core (65,536 FE/t, 60 s)
  - 128 Degenerate Matter Chunk, 8 Block of Neutronium, 8 Prime Shard, 256000 mB Nebular Gas, 16000 mB Synaptic Fluid → 1 Protostar Core (65,536 FE/t, 90 s)  ⟶  128 Degenerate Matter Chunk, 8 Block of Neutronium, 8 Chronon Capacitor, 256000 mB Nebular Gas, 16000 mB Tachyon Flux → 1 Protostar Core (65,536 FE/t, 90 s)
  - 8 Neutronium Ingot, 64000 mB Reheating Plasma → 8 Primordial Nucleosynthesis Ash (262,144 FE/t, 90 s)  ⟶  1 Neutronium Ingot, 2 mB Reheating Plasma → 64 Primordial Nucleosynthesis Ash (8 FE/t, 90 s)
  - 8 Supernova Remnant, 8 Sterile Neutrino, 8 Chronoplastic Gasket, 128000 mB Neutrino Flux, 16000 mB Psionic Ether → 4 R-Process Cluster, 8 Gold Ingot (262,144 FE/t, 60 s)  ⟶  8 Supernova Remnant, 8 Sterile Neutrino, 8 Chronoplastic Gasket, 128000 mB Neutrino Flux, 16000 mB Ionic Liquid → 4 R-Process Cluster, 8 Gold Ingot (262,144 FE/t, 60 s)
  - 8 Stellar Core, 8 Attoscale Capacitor, 16 Topological Insulator, 256000 mB Stellar Plasma, 16000 mB Quantum-Dot Photoresist → 2 Red Giant Envelope, 1 White Dwarf Core (262,144 FE/t, 120 s)  ⟶  8 Stellar Core, 8 Power Set, 8 Raw Anti-Gold, 256000 mB Stellar Plasma, 16000 mB Quark-Gluon Plasma → 2 Red Giant Envelope, 1 White Dwarf Core (262,144 FE/t, 120 s)
  - 8 White Dwarf Core, 128 Iron Ingot, 8 Chronoplastic Gasket, 64000 mB Stellar Plasma, 16000 mB Bose-Einstein Condensate → 8 Stellar Iron Ingot (131,072 FE/t, 60 s)  ⟶  8 White Dwarf Core, 128 Iron Ingot, 8 Attoscale Transistor, 64000 mB Stellar Plasma, 16000 mB Nanite Slurry → 8 Stellar Iron Ingot (131,072 FE/t, 60 s)
- **Nuclide Forge**
  - 48 R-Process Cluster, 12 Muonic Catalyst, 96 Neutronium Ingot, 24 Chronoplastic Gasket, 12 Last Flash of a Black Hole, 24 Degenerate Armour Plate, 12 Riemann Curvature Tensor, 24 White Dwarf Core, 12 Magnetar Shard, 64000 mB Quark-Gluon Plasma, 32000 mB Chronon Gas, 32000 mB Liquid Metallic Hydrogen → 1 Flerovium Ingot (16,777,216 FE/t, 70 s)  ⟶  48 R-Process Cluster, 12 Muonic Catalyst, 96 Neutronium Ingot, 24 Chronoplastic Gasket, 12 Turing Morphogen, 24 Von Neumann Probe, 12 Alcubierre Coil, 48 Noble Gas Clathrate, 12 Biosphere Compendium, 64000 mB Quark-Gluon Plasma, 32000 mB Ionic Liquid, 32000 mB Strange Matter → 1 Flerovium Ingot (16,777,216 FE/t, 70 s)
  - 24 Flerovium Ingot, 24 Supernova Remnant, 12 Positronium Hydride, 24 Chronoplastic Gasket, 12 Spin Foam Lattice, 24 Glowstone Singularity, 12 Primordial Harvest, 24 Tachyon Singularity, 12 Magnetar Shard, 128000 mB Quark-Gluon Plasma, 32000 mB Superfluid Helium, 32000 mB Exotic Matter → 1 Unbihexium Ingot (33,554,432 FE/t, 105 s)  ⟶  24 Flerovium Ingot, 24 Supernova Remnant, 12 Positronium Hydride, 24 Chronoplastic Gasket, 12 Stellar Iron Singularity, 48 Penrose Spindle, 12 Metallic Hydrogen Ingot, 24 Topological Circuit Board, 12 Spin Foam Lattice, 128000 mB Quark-Gluon Plasma, 32000 mB Dark Matter, 32000 mB Nebular Gas → 1 Unbihexium Ingot (33,554,432 FE/t, 105 s)
- **Omnilex Engine**
  - 1024 Crystallised Law, 1024 Axiom Fragment, 256 Russell Paradox Set, 256 Godel Sentence, 256 Spacetime Bearing, 512 Novikov Crystal, 256 Inaccessible Cardinal, 512 Cosmic Neutronium Ingot, 128 Unified Dossier, 512 Cosmic Logic Wafer, 128 Arbor Heartwood Sample, 512 Theoretical Notes II, 2048000 mB Primordial Law, 512000 mB Neutrino Flux, 512000 mB Antiwater, 512000 mB Inflaton Field, 512000 mB Schrödinger's Limit → 1 Primordial Ocean Simulacrum (536,870,912 FE/t, 720 s)  ⟶  1024 Crystallised Law, 1024 Axiom Fragment, 256 Russell Paradox Set, 256 Godel Sentence, 256 Unified Notes III, 512 Quadrupole Pole Tip, 128 Arbor Heartwood Sample, 512 Quantum Dot, 128 Unified Dossier, 512 Tardigrade Culture, 256 Topos, 512 Photonic Die, 2048000 mB Primordial Law, 512000 mB False Vacuum, 512000 mB Carborane Superacid, 512000 mB Strange Matter, 512000 mB Residual Starlight → 1 Primordial Ocean Simulacrum (536,870,912 FE/t, 720 s)
  - 1024 Arbor Heartwood, 1024 Bubble Universe, 128 Absolute Infinity, 512 Superintelligence Core, 256 Mathematics Singularity, 512 Standard Model Matrix, 256 Spacetime Bearing, 512 Folded Protein Crystal, 256 Standard Model Singularity, 512 Programmable Matter, 256 Theoretical Dossier, 512 Degenerate Armour Plate, 2048000 mB Primordial Law, 512000 mB Dimensional Residue, 512000 mB Antihydrogen, 512000 mB False Vacuum, 512000 mB Antiwater → 1 Arbor Simulacrum (536,870,912 FE/t, 720 s)  ⟶  1024 Arbor Heartwood, 1024 Bubble Universe, 128 Absolute Infinity, 512 Superintelligence Core, 256 Dimensional Singularity, 512 Penteract Lattice, 256 Exotic Matter Singularity, 512 Neutronium Plate, 128 Unified Dossier, 512 Octonion Crystal, 256 Topos, 512 Paradox Residue, 2048000 mB Primordial Law, 512000 mB Quantum-Dot Photoresist, 512000 mB Warm Helium, 512000 mB Quark-Gluon Plasma, 512000 mB Autocatalytic Broth → 1 Arbor Simulacrum (536,870,912 FE/t, 720 s)
- **Ordinal Recursor**
  - 16 Omega-One, 256 Cardinal Shard, 64 Real Continuum, 16 Hypersphere Fragment, 256000 mB Synaptic Fluid → 1 Aleph-One (1,048,576 FE/t, 80 s)  ⟶  16 Omega-One, 256 Cardinal Shard, 64 Real Continuum, 32 Transfinite Weave, 256000 mB Synaptic Fluid → 1 Aleph-One (1,048,576 FE/t, 80 s)
  - 64 Omega, 16 Power Set, 256 Ordinal Shard, 32 Aeon Boundary Shard, 128000 mB Dark Energy → 1 Epsilon-Naught (524,288 FE/t, 60 s)  ⟶  64 Omega, 16 Power Set, 256 Ordinal Shard, 32 Ontological Processor, 128000 mB Dark Energy → 1 Epsilon-Naught (524,288 FE/t, 60 s)
  - 32 Epsilon-Naught, 16 Aleph-Null Shard, 128 Cardinal Shard, 16 Transcendent Metal Ingot, 256000 mB Dark Energy → 1 Omega-One (1,048,576 FE/t, 80 s)  ⟶  32 Epsilon-Naught, 16 Aleph-Null Shard, 128 Cardinal Shard, 32 Inflaton Condensate, 256000 mB Dark Energy → 1 Omega-One (1,048,576 FE/t, 80 s)
  - 128 Ordinal Shard, 256 Natural Number Lattice, 64 Ordinal Stone, 16 Reality Anchor, 64000 mB False Vacuum → 1 Omega (262,144 FE/t, 40 s)  ⟶  128 Ordinal Shard, 256 Natural Number Lattice, 64 Ordinal Stone, 32 Boltzmann Fluctuation, 64000 mB Causal Solder → 1 Omega (262,144 FE/t, 40 s)
- **Particle Spectrometer**
  - 24 Entangled Photon Pair, 6 Glueball Core, 6 Penrose Spindle, 32000 mB Quark-Gluon Plasma, 8000 mB Carborane Superacid → 2 Gauge Boson Cluster (8,192 FE/t, 25 s)  ⟶  24 Entangled Photon Pair, 6 Glueball Core, 6 Higgs Field Capacitor, 32000 mB Quark-Gluon Plasma, 8000 mB Nanite Slurry → 2 Gauge Boson Cluster (8,192 FE/t, 25 s)
  - 6 Sterile Neutrino, 6 Antimatter Pellet, 6 Rational Field, 16000 mB Neutrino Flux, 8000 mB Bose-Einstein Condensate → 2 Lepton Triplet (4,096 FE/t, 18.75 s)  ⟶  6 Sterile Neutrino, 6 Antimatter Pellet, 6 Dipole Winding, 16000 mB Neutrino Flux, 8000 mB Supercritical Solvent → 2 Lepton Triplet (4,096 FE/t, 18.75 s)
  - 6 Strange Matter Pellet, 6 Electride Crystal, 6 Topological Wafer, 16000 mB Quark-Gluon Plasma, 8000 mB Ionic Liquid → 2 Strange & Charm Quarks, 1 Up & Down Quarks, 100 mB Strange Matter (4,096 FE/t, 18.75 s)  ⟶  6 Strange Matter Pellet, 6 Hawking Absorber Plate, 6 Sterile Neutrino, 16000 mB Quark-Gluon Plasma, 8000 mB Chronon Gas → 2 Strange & Charm Quarks, 1 Up & Down Quarks, 100 mB Strange Matter (4,096 FE/t, 18.75 s)
  - 6 Neutronium Ingot, 6 Electride Crystal, 24 Turbomolecular Rotor, 8000 mB Quark-Gluon Plasma, 8000 mB Supercritical Solvent → 4 Up & Down Quarks (2,048 FE/t, 12.5 s)  ⟶  6 Neutronium Ingot, 6 Gravitic Lens Element, 12 Strange Matter Pellet, 8000 mB Quark-Gluon Plasma, 8000 mB Warm Helium → 16 Up & Down Quarks (2,048 FE/t, 12.5 s)
  - 48 Virtual Quark, 6 Entangled Bus Bar, 6 Quark Star Core, 16000 mB Superfluid Helium, 8000 mB Hawking Radiation → 2 Up & Down Quarks, 1 Strange & Charm Quarks (131,072 FE/t, 12.5 s)  ⟶  48 Virtual Quark, 6 Entangled Bus Bar, 6 Proton Decay Residue, 16000 mB Superfluid Helium, 8000 mB Exotic Solder → 2 Up & Down Quarks, 1 Strange & Charm Quarks (131,072 FE/t, 12.5 s)
- **Peano Arithmetic Engine**
  - 8 Natural Number Lattice, 4 Antimatter Pellet, 8 Cryocooler Unit → 4 Integer Ring (256 FE/t, 10 s)  ⟶  1 Natural Number Lattice, 1 Antimatter Pellet, 1 Fractured Essence → 64 Integer Ring (1 FE/t, 10 s)
  - 16 Natural Number Lattice, 4 Fractured Essence, 8 Molecular Orbital Lattice → 2 Integer Ring (256 FE/t, 15 s)  ⟶  16 Natural Number Lattice, 4 Fractured Essence, 4 Photonic Die → 2 Integer Ring (256 FE/t, 15 s)
  - 4 Qubit, 16 Cherenkov Quartz, 4 Neutronium Plate → 2 Natural Number Lattice (128 FE/t, 10 s)  ⟶  4 Qubit, 16 Cherenkov Quartz, 4 Ferrite Magnet → 5 Natural Number Lattice (128 FE/t, 10 s)
  - 8 Complex Plane, 4 Magnetic Monopole, 4 Coherent Wiring Harness, 8 Plasma Nozzle Tip, 8000 mB Bose-Einstein Condensate → 1 Quaternion Crystal (4,096 FE/t, 20 s)  ⟶  8 Complex Plane, 4 Magnetic Monopole, 4 Coherent Wiring Harness, 8000 mB Bose-Einstein Condensate → 1 Quaternion Crystal (4,096 FE/t, 20 s)
  - 8 Integer Ring, 4 Casimir Plate, 16 Xenogenome Sample → 2 Rational Field (384 FE/t, 10 s)  ⟶  8 Integer Ring, 4 Casimir Plate, 4 Photonic Logic Wafer → 2 Rational Field (384 FE/t, 10 s)
- **Planck-Scale Cutter**
  - 4 Hyperdimensional Logic Wafer, 4 Coherent Wiring Harness, 1600 mB Superfluid Helium → 8 Hyperdimensional Die, 100 mB Warm Helium (131,072 FE/t, 15 s)  ⟶  4 Hyperdimensional Logic Wafer, 4 Coherent Wiring Harness, 8 Hyperlattice Frame, 1600 mB Superfluid Helium → 8 Hyperdimensional Die, 100 mB Warm Helium (131,072 FE/t, 15 s)
  - 4 Photonic Logic Wafer, 8 Void Lattice Shard, 1600 mB Superfluid Helium → 8 Photonic Die, 100 mB Warm Helium (256 FE/t, 15 s)  ⟶  4 Photonic Logic Wafer, 8 Quantum Random Number Chip, 1600 mB Superfluid Helium → 8 Photonic Die, 100 mB Warm Helium (256 FE/t, 15 s)
  - 4 Qubit Array Wafer, 4 Coherent Wiring Harness, 1600 mB Superfluid Helium → 8 Qubit Die, 100 mB Warm Helium (2,048 FE/t, 15 s)  ⟶  4 Qubit Array Wafer, 16 Zeolite Catalyst, 1600 mB Superfluid Helium → 8 Qubit Die, 100 mB Warm Helium (2,048 FE/t, 15 s)
  - 4 Cherenkov Boule, 4 Xenogenome Sample, 4000 mB Superfluid Helium → 16 Cherenkov Wafer, 1 Spacetime Slag, 250 mB Warm Helium (512 FE/t, 30 s)  ⟶  4 Cherenkov Boule, 4 Metamaterial Substrate, 4000 mB Superfluid Helium → 16 Cherenkov Wafer, 1 Spacetime Slag, 250 mB Warm Helium (512 FE/t, 30 s)
  - 4 Hyperdimensional Boule, 4 Coherent Wiring Harness, 8 Modular Form, 8 Amethyst Singularity, 4000 mB Superfluid Helium → 16 Hyperdimensional Wafer, 1 Spacetime Slag, 250 mB Warm Helium (512 FE/t, 30 s)  ⟶  4 Hyperdimensional Boule, 4 Coherent Wiring Harness, 4000 mB Superfluid Helium → 16 Hyperdimensional Wafer, 1 Spacetime Slag, 250 mB Warm Helium (512 FE/t, 30 s)
  - 4 Neutronium Plate, 8 Dilithium Crystal, 1600 mB Superfluid Helium → 2 Turbomolecular Rotor, 100 mB Warm Helium (512 FE/t, 10 s)  ⟶  4 Neutronium Plate, 4 Quantum Random Number Chip, 1600 mB Superfluid Helium → 2 Turbomolecular Rotor, 100 mB Warm Helium (512 FE/t, 10 s)
- **Psionic Resonator**
  - 8 Thought Matrix, 8 Heat Death Remnant, 8 Neural Organoid, 64000 mB Psionic Ether → 1 Boltzmann Brain (524,288 FE/t, 60 s)  ⟶  1 Thought Matrix, 1 Heat Death Remnant, 1 Neural Organoid, 1 mB Psionic Ether → 8 Boltzmann Brain (1 FE/t, 60 s)
- **Quantum Entangler**
  - 8 Cherenkov Quartz, 4 Amethyst Shard, 4 Dilithium Crystal → 2 Entangled Photon Pair (128 FE/t, 8 s)  ⟶  8 Cherenkov Quartz, 4 Amethyst Shard, 4 Magnetite → 2 Entangled Photon Pair (128 FE/t, 8 s)
  - 4 Entangled Photon Pair, 4 Quantum Dot, 8 Cherenkov Quartz → 1 Qubit (256 FE/t, 10 s)  ⟶  4 Entangled Photon Pair, 4 Quantum Dot, 4 Ferrite Magnet → 1 Qubit (256 FE/t, 10 s)
  - 4 Qubit, 4 Cherenkov Quartz, 8 Degenerate Matter Chunk → 1 Superposition Crystal (384 FE/t, 12 s)  ⟶  4 Qubit, 4 Cherenkov Quartz, 8 Raw Pitchblende → 1 Superposition Crystal (384 FE/t, 12 s)
  - 32 Virtual Photon, 4 Coherent Wiring Harness, 8 Hyperlattice Frame, 8 Proton Decay Residue → 4 Entangled Photon Pair (65,536 FE/t, 8 s)  ⟶  32 Virtual Photon, 4 Coherent Wiring Harness → 25 Entangled Photon Pair (65,536 FE/t, 8 s)
- **Quantum Lithographer**
  - 4 Hyperdimensional Wafer, 4 Coherent Wiring Harness, 8 Quartz Singularity, 1600 mB Quantum-Dot Photoresist → 1 Cosmic Logic Wafer (1,048,576 FE/t, 20 s)  ⟶  4 Hyperdimensional Wafer, 4 Coherent Wiring Harness, 1600 mB Quantum-Dot Photoresist → 1 Cosmic Logic Wafer (1,048,576 FE/t, 20 s)
  - 4 Hyperdimensional Wafer, 4 Coherent Wiring Harness, 8 Quartz Singularity, 1600 mB Quantum-Dot Photoresist → 1 Hyperdimensional Logic Wafer (131,072 FE/t, 20 s)  ⟶  4 Hyperdimensional Wafer, 4 Coherent Wiring Harness, 1600 mB Quantum-Dot Photoresist → 1 Hyperdimensional Logic Wafer (131,072 FE/t, 20 s)
  - 4 Cherenkov Wafer, 8 Metamaterial Substrate, 1600 mB Quantum-Dot Photoresist → 1 Photonic Logic Wafer (256 FE/t, 20 s)  ⟶  4 Cherenkov Wafer, 8 Genome Sample, 1600 mB Quantum-Dot Photoresist → 1 Photonic Logic Wafer (256 FE/t, 20 s)
  - 4 Cherenkov Wafer, 4 Superconducting Coil, 1600 mB Quantum-Dot Photoresist → 1 Qubit Array Wafer (2,048 FE/t, 20 s)  ⟶  4 Cherenkov Wafer, 4 Cryocooler Unit, 1600 mB Quantum-Dot Photoresist → 1 Qubit Array Wafer (2,048 FE/t, 20 s)
- **Quark-Gluon Collider**
  - 12 Superconducting Coil, 12 Coherent Wiring Harness, 6 Error-Correction Lattice, 12 Photonic Die, 6 Proof Fragment, 32000 mB Quark-Gluon Plasma, 8000 mB Chronon Gas → 1 Magnetic Monopole (131,072 FE/t, 37.5 s)  ⟶  1 Superconducting Coil, 12 Coherent Wiring Harness, 6 Quadrupole Pole Tip, 24 Photonic Lens, 6 Hawking Absorber Plate, 12 RF Resonator Cell, 1808 mB Quark-Gluon Plasma, 8000 mB Nebular Gas → 8 Magnetic Monopole (7,403 FE/t, 37.5 s)
  - 12 Neutronium Ingot, 12 Chiral Crystal, 24 Cherenkov Lattice, 24 Crystalline Matrix Ingot, 24 Lattice Seed Crystal, 16000 mB Superfluid Helium, 8000 mB Quantum-Dot Photoresist → 1000 mB Quark-Gluon Plasma (32,768 FE/t, 25 s)  ⟶  12 Neutronium Ingot, 12 Zeolite Catalyst, 24 Dilithium Crystal, 24 Quantum Dot, 24 Geiger Tube, 16000 mB Superfluid Helium, 8000 mB Quantum-Dot Photoresist → 1000 mB Quark-Gluon Plasma (32,768 FE/t, 25 s)
  - 6 Void Lattice Shard, 6 Degenerate Matter Chunk, 12 Coherent Wiring Harness, 6 Transcendental Crystal, 6 Buckyball Cage, 6 Fractal Core, 16000 mB Quark-Gluon Plasma, 8000 mB Neutrino Flux → 1 Strange Matter Pellet, 250 mB Strange Matter (65,536 FE/t, 25 s)  ⟶  6 Void Lattice Shard, 6 Degenerate Matter Chunk, 12 Coherent Wiring Harness, 6 Imaginary Unit, 24 Zeolite Catalyst, 6 Femtoscale Capacitor, 16000 mB Quark-Gluon Plasma, 8000 mB Superfluid Helium → 1 Strange Matter Pellet, 250 mB Strange Matter (65,536 FE/t, 25 s)
  - 1 Standard Model Matrix, 1 Dark Matter Lens, 12 Spacetime Alloy Singularity, 2000 mB Dark Matter → 2 Superpartner Shard, 500 mB Dark Matter (262,144 FE/t, 40 s)  ⟶  1 Standard Model Matrix, 1 Dark Matter Lens, 12 Functor Bridge, 2000 mB Dark Matter → 2 Superpartner Shard, 500 mB Dark Matter (262,144 FE/t, 40 s)
- **Reality Compiler**
  - 32 Spacetime Catalyst, 32 Closed Timelike Curve, 32 Bootstrap Artifact, 32 Cosmological Constant, 32 Feynman Diagram, 128 Higgs Boson Crystal, 32 Paradox Residue, 128 Mirror Alloy Ingot, 32 Baryogenesis Seed, 64 Kerr Metric Shard, 32 Hypernova Alloy Ingot, 64 Heat Death Ember, 1179648 mB Causal Solder, 2048000 mB Dark Energy, 128000 mB Liquid Antihelium, 128000 mB Supercritical Solvent, 128000 mB Xenoplasm → 1 Eternity Catalyst, 2000 mB Dimensional Residue (268,435,456 FE/t, 750 s)  ⟶  32 Spacetime Catalyst, 32 Closed Timelike Curve, 32 Bootstrap Artifact, 32 Cosmological Constant, 32 True Vacuum Seed, 128 Calabi-Yau Manifold, 32 Paradox Residue, 128 Minkowski Lattice, 32 Empirical Notes I, 64 Neural Organoid, 32 Heat Death Remnant, 128 Power Set, 1179648 mB Causal Solder, 2048000 mB Dark Energy, 128000 mB Stellar Plasma, 128000 mB Synaptic Fluid, 128000 mB Relativity Breakdown → 1 Eternity Catalyst, 2000 mB Dimensional Residue (268,435,456 FE/t, 750 s)
  - 128 Source Code Fragment, 32 Pocket Universe, 64 Ontological Processor, 64 Mirror Processor, 64 Einstein-Rosen Core, 128 Muonic Catalyst, 64 Universe Seed, 128 Void Lattice Shard, 64 Retrocausal Crystal, 128 Bekenstein Crystal, 64 Feynman Diagram, 2048000 mB Dark Energy, 2048000 mB Synaptic Fluid, 128000 mB Supercritical Solvent, 128000 mB Superfluid Helium, 128000 mB Residual Starlight → 1 Omega Point, 4000 mB Dimensional Residue (134,217,728 FE/t, 1500 s)  ⟶  128 Source Code Fragment, 32 Pocket Universe, 64 Ontological Processor, 64 Mirror Processor, 64 Tidal Alloy Scrap, 128 Spacetime Anchor Core, 64 Paradox Residue, 128 Diamond Singularity, 64 Retrocausal Crystal, 128 Spinor Lattice, 64 Einstein-Rosen Core, 2048000 mB Dark Energy, 2048000 mB Synaptic Fluid, 128000 mB Causal Solder, 128000 mB Residual Starlight, 128000 mB Quantum-Dot Photoresist → 1 Omega Point, 4000 mB Dimensional Residue (134,217,728 FE/t, 1500 s)
  - 64 Crystallised Law, 32 Transcendental Crystal, 64 Omnipotence Dust, 64 Ontological Junction, 64 Cosmic Flux Coupler, 64 Tumour Microenvironment Organoid, 128 Lepton Triplet, 64 Absolute Dossier, 128 Qubit Array Wafer, 64 Contagion Network Map, 128 Lapis Singularity, 512000 mB Pleroma, 128000 mB False Vacuum, 128000 mB Superfluid Helium, 128000 mB Exotic Matter → 1 Pleroma Crystal (1,000,000,000 FE/t, 100 s)  ⟶  64 Crystallised Law, 32 Transcendental Crystal, 64 Omnipotence Dust, 64 Dimensional Singularity, 128 Ontological Processor, 32 Absolute Infinity, 128 Heat Death Remnant, 64 Unified Notes III, 128 Cherenkov Quartz, 64 Theory of Everything, 128 Quantum Processor, 512000 mB Pleroma, 128000 mB Causal Solder, 128000 mB Relativity Breakdown, 128000 mB Strange Matter → 1 Pleroma Crystal (1,000,000,000 FE/t, 100 s)
  - 32 Source Code Fragment, 32 Bootstrap Artifact, 128 Claytronic Catom, 32 Quark Star Core, 32 Theoretical Notes III, 64 Hyperdimensional Pearl, 64 Metastable Vacuum Lattice, 64 Gravitational Wave Crystal, 32 Hypernova Alloy Ingot, 128 Metal-Organic Framework, 32 True Vacuum Seed, 128 Xenobot Swarm, 1024000 mB Psionic Ether, 128000 mB Dimensional Residue, 128000 mB Stellar Plasma, 128000 mB Strange Matter → 1 Reality Anchor (16,777,216 FE/t, 300 s)  ⟶  32 Source Code Fragment, 32 Bootstrap Artifact, 128 Claytronic Catom, 32 Quark Star Core, 64 Cosmic Processor, 128 Topological Wafer, 64 Heat Death Remnant, 128 Crystalline Matrix Ingot, 64 Stress-Energy Tensor, 128 Anti-Iron Ingot, 32 Contained Anomaly Specimen, 128 Strange & Charm Quarks, 1024000 mB Psionic Ether, 128000 mB Supercritical Solvent, 128000 mB Dark Matter, 128000 mB Strange Matter → 1 Reality Anchor (16,777,216 FE/t, 300 s)
  - 32 Bekenstein Crystal, 32 Cosmic Processor, 32 Holographic Matrix, 32 Boltzmann Brain, 32 Einstein-Rosen Core, 128 Cherenkov Boule, 32 Baryogenesis Seed, 128 Riemann Curvature Tensor, 32 Hypernova Alloy Ingot, 128 Femtoscale Inductor, 32 Frozen Photon, 128 Spacetime Alloy Ingot, 2048000 mB Synaptic Fluid, 1024000 mB Dark Energy, 128000 mB Liquid Antihelium, 128000 mB Nebular Gas, 128000 mB Hawking Radiation → 2 Source Code Fragment, 1000 mB Dimensional Residue (33,554,432 FE/t, 600 s)  ⟶  32 Bekenstein Crystal, 32 Cosmic Processor, 32 Holographic Matrix, 32 Boltzmann Brain, 32 Tidal Alloy Scrap, 128 Hyperdimensional Boule, 32 Theoretical Notes III, 128 Copper Singularity, 32 De Sitter Horizon Shard, 128 Photonic Die, 32 Empirical Notes I, 128 Lapis Singularity, 2048000 mB Synaptic Fluid, 1024000 mB Dark Energy, 128000 mB Nebular Gas, 128000 mB Nutrient Broth, 128000 mB Exotic Matter → 2 Source Code Fragment, 1000 mB Dimensional Residue (33,554,432 FE/t, 600 s)
- **Stellar Plasma Crucible**
  - 16 Neutronium Ingot, 8 Chronon Capacitor, 8 Quantum Processor, 8 Qubit Array Wafer, 8 Pixel Tracker Module, 16 Antimatter Pellet, 8 Topological Boule, 32000 mB Chronon Gas, 64000 mB Superfluid Helium, 16000 mB Supercritical Solvent, 16000 mB Nanite Slurry → 2 Chronosteel Ingot, 1000 mB Warm Helium (32,768 FE/t, 45 s)  ⟶  16 Neutronium Ingot, 8 Chronon Capacitor, 8 Hawking Absorber Plate, 32 Superposition Crystal, 8 Sterile Neutrino, 32 Crystalline Matrix Ingot, 8 Transcendental Crystal, 16 RF Resonator Cell, 32000 mB Chronon Gas, 64000 mB Superfluid Helium, 16000 mB Superconducting Solder, 16000 mB Dark Matter → 2 Chronosteel Ingot, 1000 mB Warm Helium (32,768 FE/t, 45 s)
  - 16 Block of Neutronium, 8 Dark Matter Lens, 8 Stellar Core, 16 Entangled Bus Bar, 8 Formal Proof Tree, 16 Topological Wafer, 8 Tardigrade Culture, 8 Chronosteel Ingot, 128000 mB Stellar Plasma, 128000 mB Dark Matter, 16000 mB Nebular Gas, 16000 mB Supercritical Solvent → 16 Cosmic Neutronium Ingot, 2 Spacetime Slag, 1000 mB Dimensional Residue (8,388,608 FE/t, 180 s)  ⟶  16 Block of Neutronium, 8 Dark Matter Lens, 8 Stellar Core, 16 Entangled Bus Bar, 8 Tardigrade Culture, 16 Sterile Neutrino, 8 Riemann Zeta Crystal, 16 Qubit Array Wafer, 128000 mB Stellar Plasma, 128000 mB Dark Matter, 16000 mB Superconducting Solder, 16000 mB Strange Matter → 16 Cosmic Neutronium Ingot, 2 Spacetime Slag, 1000 mB Dimensional Residue (8,388,608 FE/t, 180 s)
  - 8 Exotic Matter Crystal, 8 Qubit Array Wafer, 32 Neutronium Ingot, 8 Prime Shard, 32 Spacetime Slag, 8 Carbon Nanotube Bundle, 16 RF Resonator Cell, 36864 mB Superconducting Solder, 16000 mB Quark-Gluon Plasma, 16000 mB Neutrino Flux, 16000 mB Ionic Liquid → 576 mB Exotic Solder (65,536 FE/t, 30 s)  ⟶  8 Exotic Matter Crystal, 8 Tachyon Crystal, 16 Coherent Wiring Harness, 8 Nanite Swarm, 32 Cherenkov Boule, 8 Femtoscale Inductor, 36864 mB Superconducting Solder, 16000 mB Quark-Gluon Plasma, 16000 mB Quantum-Dot Photoresist, 16000 mB Dark Matter → 576 mB Exotic Solder (65,536 FE/t, 30 s)
  - 32 Neutronium Ingot, 8 Cosmic String Segment, 16 Exotic Matter Crystal, 16 Chronoplastic Gasket, 16 Entangled Bus Bar, 8 Self-Healing Polymer, 8 Integral Operator, 8 Raw Anti-Iron, 16 Antisilicon Shard, 16 Morphogenic Serum, 64000 mB Quark-Gluon Plasma, 64000 mB Exotic Matter, 16000 mB Exotic Solder, 16000 mB Nutrient Broth → 4 Spacetime Alloy Ingot, 1 Spacetime Slag, 500 mB Dimensional Residue (1,048,576 FE/t, 90 s)  ⟶  32 Neutronium Ingot, 8 Cosmic String Segment, 16 Exotic Matter Crystal, 16 Entangled Bus Bar, 8 Wave Packet, 16 Molecular Orbital Lattice, 8 Antihydrogen Crystal, 32 Raw Pitchblende, 64000 mB Quark-Gluon Plasma, 64000 mB Exotic Matter, 16000 mB Ionic Liquid, 16000 mB Dark Matter → 4 Spacetime Alloy Ingot, 1 Spacetime Slag, 500 mB Dimensional Residue (1,048,576 FE/t, 90 s)
- **String Resonance Loom**
  - 24 Cosmic String Segment, 12 Tesseract Lens, 12 Graviton Crystal, 12 Primordial Harvest, 128000 mB Exotic Matter → 1 Cosmic String Lens (4,194,304 FE/t, 175 s)  ⟶  24 Cosmic String Segment, 12 Tesseract Lens, 12 Graviton Crystal, 12 Spacetime Catalyst, 128000 mB Exotic Matter → 1 Cosmic String Lens (4,194,304 FE/t, 175 s)
  - 24 D-Brane Sheet, 12 G₂ Manifold, 12 String Coupling Crystal, 12 Bounce Core, 128000 mB Exotic Matter → 1 M-Brane (65,536 FE/t, 70 s)  ⟶  24 D-Brane Sheet, 12 G₂ Manifold, 12 String Coupling Crystal, 12 Room-Temperature Superconductor, 128000 mB Exotic Matter → 1 M-Brane (65,536 FE/t, 70 s)
  - 12 Cosmic String Segment, 12 Tachyon Crystal, 12 Superposition Crystal, 12 Red Giant Envelope, 32000 mB Exotic Matter → 4 Quantum String (32,768 FE/t, 35 s)  ⟶  12 Cosmic String Segment, 12 Tachyon Crystal, 12 Superposition Crystal, 12 Stem Cell Culture, 32000 mB Exotic Matter → 4 Quantum String (32,768 FE/t, 35 s)
  - 12 Calabi-Yau Manifold, 24 Quantum String, 24 Topological Insulator, 12 Neutronium Singularity, 64000 mB Exotic Matter → 1 Dimensional Stabiliser Matrix (32,768 FE/t, 52.5 s)  ⟶  12 Calabi-Yau Manifold, 24 Quantum String, 24 Topological Insulator, 12 White Dwarf Core, 64000 mB Exotic Matter → 1 Dimensional Stabiliser Matrix (32,768 FE/t, 52.5 s)
- **Tachyon Resonator**
  - 24 Cherenkov Quartz, 6 Void Lattice Shard, 12 Xenogenome Sample, 16000 mB Superfluid Helium, 8000 mB Quantum-Dot Photoresist → 1 Tachyon Crystal, 250 mB Tachyon Flux (2,048 FE/t, 18.75 s)  ⟶  24 Cherenkov Quartz, 6 Void Lattice Shard, 24 Photonic Circuit Board, 16000 mB Superfluid Helium, 8000 mB Warm Helium → 1 Tachyon Crystal, 250 mB Tachyon Flux (2,048 FE/t, 18.75 s)
  - 6 Tachyon Crystal, 12 Chiral Crystal, 24 Entangled Photon Pair, 8000 mB Superfluid Helium, 8000 mB Bose-Einstein Condensate → 1000 mB Tachyon Flux (1,024 FE/t, 12.5 s)  ⟶  6 Tachyon Crystal, 6 Photonic Neuron, 24 Crystalline Matrix Ingot, 8000 mB Superfluid Helium, 8000 mB Supercritical Solvent → 1000 mB Tachyon Flux (1,024 FE/t, 12.5 s)
  - 12 Superconducting Coil, 24 Quantum Dot, 12 Photonic Lens, 8000 mB Superfluid Helium, 8000 mB Bose-Einstein Condensate → 1 Tachyon Klystron (2,048 FE/t, 18.75 s)  ⟶  12 Superconducting Coil, 24 Quantum Dot, 12 Genome Sample, 8000 mB Superfluid Helium, 8000 mB Warm Helium → 1 Tachyon Klystron (2,048 FE/t, 18.75 s)
- **Tensor Calculus Engine**
  - 8 Einstein Field Core, 8 Penrose Spindle, 8 Micro Singularity, 8 Chronoplastic Gasket, 16000 mB Antihydrogen → 2 Kerr Metric Shard (65,536 FE/t, 60 s)  ⟶  8 Einstein Field Core, 8 Penrose Spindle, 8 Micro Singularity, 8 Chronoplastic Gasket, 16000 mB Tachyon Flux → 2 Kerr Metric Shard (65,536 FE/t, 60 s)
  - 32 Entangled Photon Pair, 8 Tachyon Crystal, 8 Real Continuum, 8 Chronoplastic Gasket, 16000 mB Tachyon Flux → 2 Lorentz Transform Crystal (4,096 FE/t, 22.5 s)  ⟶  32 Entangled Photon Pair, 8 Tachyon Crystal, 8 Real Continuum, 8 Antiproton Bundle, 16000 mB Tachyon Flux → 2 Lorentz Transform Crystal (4,096 FE/t, 22.5 s)
  - 8 Minkowski Lattice, 16 Derivative Operator, 32 Neutronium Plate, 8 Chronoplastic Gasket, 16000 mB Neutrino Flux → 2 Metric Tensor (16,384 FE/t, 30 s)  ⟶  8 Minkowski Lattice, 16 Derivative Operator, 32 Neutronium Plate, 8 Chronoplastic Gasket, 16000 mB Quark-Gluon Plasma → 2 Metric Tensor (16,384 FE/t, 30 s)
  - 16 Lorentz Transform Crystal, 8 Differential Manifold, 8 Quaternion Crystal, 8 Chronoplastic Gasket, 16000 mB Strange Matter → 1 Minkowski Lattice (8,192 FE/t, 30 s)  ⟶  16 Lorentz Transform Crystal, 8 Differential Manifold, 8 Quaternion Crystal, 8 Protostar Core, 16000 mB Supercritical Solvent → 1 Minkowski Lattice (8,192 FE/t, 30 s)
  - 16 Metric Tensor, 32 Derivative Operator, 8 Micro Singularity, 8 Chronoplastic Gasket, 16000 mB Chronon Gas → 1 Riemann Curvature Tensor (32,768 FE/t, 45 s)  ⟶  16 Metric Tensor, 32 Derivative Operator, 8 Micro Singularity, 8 Chronoplastic Gasket, 16000 mB Exotic Solder → 1 Riemann Curvature Tensor (32,768 FE/t, 45 s)
- **Tesseract Assembler**
  - 48 Superconducting Coil, 48 Exotic Matter Crystal, 24 Metric Tensor, 24 Lorentz Transform Crystal, 24 Chronoplastic Gasket, 12 Spacetime Anchor Core, 48 Graphene Sheet, 12 Inertia Damper, 48 Neutronium Plate, 12 Xenobot Swarm, 24 Spacetime Alloy Singularity, 256000 mB Exotic Matter, 32000 mB Liquid Antihelium, 32000 mB Supercritical Solvent → 1 Alcubierre Coil (131,072 FE/t, 70 s)  ⟶  48 Superconducting Coil, 48 Exotic Matter Crystal, 24 Metric Tensor, 24 Lorentz Transform Crystal, 24 Chronoplastic Gasket, 12 Hyperdimensional Boule, 48 Sediment Core, 12 Inertia Damper, 48 Photonic Die, 12 Spacetime Anchor Core, 24 Primordial Harvest, 256000 mB Exotic Matter, 32000 mB Carborane Superacid, 32000 mB Quark-Gluon Plasma → 1 Alcubierre Coil (131,072 FE/t, 70 s)
  - 48 Metamaterial Substrate, 12 Calabi-Yau Manifold, 12 Cosmic String Segment, 96 Attoscale Inductor, 24 Chronoplastic Gasket, 12 Dilithium Singularity, 48 Strange Matter Pellet, 12 Mirror Processor, 48 Zeolite Catalyst, 12 Copper Singularity, 73728 mB Exotic Solder, 32000 mB Synaptic Fluid, 32000 mB Superfluid Helium → 1 Cosmic Circuit Board, 500 mB Dimensional Residue (4,194,304 FE/t, 140 s)  ⟶  48 Metamaterial Substrate, 12 Calabi-Yau Manifold, 12 Cosmic String Segment, 96 Attoscale Inductor, 24 Chronoplastic Gasket, 12 Exotic Crystal Singularity, 48 Photonic Lens, 12 Quantum Field Lattice, 12 Antimatter Singularity, 12 Gauge Boson Cluster, 73728 mB Exotic Solder, 32000 mB Synaptic Fluid, 32000 mB Bose-Einstein Condensate → 1 Cosmic Circuit Board, 500 mB Dimensional Residue (4,194,304 FE/t, 140 s)
  - 24 Penteract Lattice, 48 Octonion Crystal, 24 Hopf Fibration, 12 G₂ Manifold, 24 Chronoplastic Gasket, 24 Contained Anomaly Specimen, 48 Exotic Matter Crystal, 24 Hypernova Alloy Ingot, 48 Topological Die, 12 Transfinite Weave, 256000 mB Exotic Matter, 32000 mB Reheating Plasma, 32000 mB Ionic Liquid → 1 E8 Lattice (8,388,608 FE/t, 105 s)  ⟶  24 Penteract Lattice, 48 Octonion Crystal, 24 Hopf Fibration, 12 G₂ Manifold, 24 Chronoplastic Gasket, 24 Theoretical Notes III, 48 Graphene Sheet, 24 Prion Lattice, 48 Infinitesimal, 24 Eternity Catalyst, 256000 mB Exotic Matter, 32000 mB Exotic Solder, 32000 mB Stellar Plasma → 1 E8 Lattice (8,388,608 FE/t, 105 s)
  - 96 Tidal Alloy Scrap, 96 Retrocausal Crystal, 48 Frozen Photon, 48 Primordial Ejecta, 24 Chronoplastic Gasket, 12 Hamiltonian Operator, 48 Topological Processor, 12 M-Brane, 48 Fourier Series, 12 Wavefunction, 128000 mB Relativity Breakdown, 32000 mB Ionic Liquid, 32000 mB Antiwater → 1 Einstein-Rosen Core (4,194,304 FE/t, 70 s)  ⟶  96 Tidal Alloy Scrap, 96 Retrocausal Crystal, 48 Frozen Photon, 48 Primordial Ejecta, 24 Chronoplastic Gasket, 12 Kaluza-Klein Singularity, 48 Recombinant DNA, 12 Cosmic String Lens, 48 Natural Number Lattice, 12 Hyperdimensional Pearl, 128000 mB Relativity Breakdown, 32000 mB Liquid Metallic Hydrogen, 32000 mB Ionic Liquid → 1 Einstein-Rosen Core (4,194,304 FE/t, 70 s)
  - 24 Standard Model Matrix, 48 Superpartner Shard, 48 Magnetic Monopole, 48 Quantum String, 24 Chronoplastic Gasket, 12 Primordial Harvest, 48 Fractal Dust, 12 Biosphere Compendium, 48 Qubit, 12 Dyson Shell Segment, 512000 mB Quark-Gluon Plasma, 256000 mB Exotic Matter, 32000 mB Hawking Radiation, 32000 mB Carborane Superacid → 1 Grand Unified Core, 500 mB Dimensional Residue (1,048,576 FE/t, 140 s)  ⟶  24 Standard Model Matrix, 48 Superpartner Shard, 48 Magnetic Monopole, 48 Quantum String, 24 Chronoplastic Gasket, 12 Empirical Notes III, 24 Cherenkov Singularity, 12 Cosmic Neutronium Ingot, 48 Imaginary Unit, 12 Von Neumann Probe, 24 Functor Bridge, 512000 mB Quark-Gluon Plasma, 256000 mB Exotic Matter, 32000 mB Dark Matter, 32000 mB Neutrino Flux → 1 Grand Unified Core, 500 mB Dimensional Residue (1,048,576 FE/t, 140 s)
  - 48 Hypersphere Fragment, 24 Quaternion Crystal, 48 Compactified Circle, 24 Aleph Glass, 24 Chronoplastic Gasket, 24 Topological Defect Core, 48 Virtual Quark, 12 Aleph-Null Shard, 48 M-Brane, 12 Transfinite Weave, 64000 mB Exotic Matter, 32000 mB Exotic Solder, 32000 mB Nanite Slurry → 1 Hopf Fibration (2,097,152 FE/t, 52.5 s)  ⟶  48 Hypersphere Fragment, 24 Quaternion Crystal, 48 Compactified Circle, 24 Aleph Glass, 24 Chronoplastic Gasket, 24 Boltzmann Fluctuation, 48 Fractal Set, 24 Prion Lattice, 48 Neutronium Plate, 24 Eternity Catalyst, 64000 mB Exotic Matter, 32000 mB Xenoplasm, 32000 mB False Vacuum → 1 Hopf Fibration (2,097,152 FE/t, 52.5 s)
  - 24 Möbius Strip, 12 Differential Manifold, 12 Tesseract Core, 24 Chronoplastic Gasket, 12 Dyson Shell Segment, 48 Entangled Photon Pair, 12 Cosmic Neutronium Singularity, 48 Topological Wafer, 12 Degenerate Armour Plate, 64000 mB Exotic Matter, 32000 mB Dark Matter, 32000 mB Nanite Slurry → 1 Klein Bottle, 100 mB Dimensional Residue (32,768 FE/t, 52.5 s)  ⟶  24 Möbius Strip, 12 Differential Manifold, 12 Tesseract Core, 24 Chronoplastic Gasket, 12 Thought Matrix, 48 Lattice Seed Crystal, 12 Spin Foam Lattice, 48 Photonic Die, 12 Stellar Iron Singularity, 24 Primordial Harvest, 64000 mB Exotic Matter, 32000 mB Quantum-Dot Photoresist, 32000 mB Antihydrogen → 1 Klein Bottle, 100 mB Dimensional Residue (32,768 FE/t, 52.5 s)
  - 24 Tesseract Core, 96 Hypersphere Fragment, 12 Hopf Fibration, 24 Chronoplastic Gasket, 24 Topological Defect Core, 48 Crystalline Matrix Ingot, 24 Theoretical Notes III, 48 Metamaterial Substrate, 24 Contained Anomaly Specimen, 128000 mB Exotic Matter, 32000 mB Synaptic Fluid, 32000 mB Superfluid Helium → 1 Penteract Lattice (4,194,304 FE/t, 70 s)  ⟶  24 Tesseract Core, 96 Hypersphere Fragment, 12 Hopf Fibration, 24 Chronoplastic Gasket, 12 Aleph-Null Shard, 48 Degenerate Matter Chunk, 24 True Vacuum Seed, 48 Kugelblitz Seed, 12 Transfinite Processor, 128000 mB Exotic Matter, 32000 mB Liquid Spacetime, 32000 mB Dark Era Haze → 1 Penteract Lattice (4,194,304 FE/t, 70 s)
  - 12 White Hole Fragment, 12 Micro Singularity, 12 Cosmic String Segment, 24 Chronoplastic Gasket, 12 Computronium Singularity, 12 Coal Singularity, 12 Strange Matter Singularity, 48 Superposition Crystal, 12 Massless Alloy, 24 Amethyst Singularity, 128000 mB Exotic Matter, 32000 mB Chronon Gas, 32000 mB Exotic Solder → 4 Planck Shard (4,194,304 FE/t, 175 s)  ⟶  12 White Hole Fragment, 12 Micro Singularity, 12 Cosmic String Segment, 24 Chronoplastic Gasket, 12 Red Giant Envelope, 48 Quantum Random Number Chip, 12 Bekenstein Crystal, 48 Empty Set, 12 Exotic Crystal Singularity, 128000 mB Exotic Matter, 32000 mB Bose-Einstein Condensate, 32000 mB Carborane Superacid → 4 Planck Shard (4,194,304 FE/t, 175 s)
  - 12 Higgs Catalyst, 96 Spacetime Alloy Ingot, 12 Tesseract Core, 12 Micro Singularity, 24 Chronoplastic Gasket, 12 Thought Matrix, 48 Spacetime Slag, 12 Cosmic Neutronium Ingot, 48 Photonic Lens, 12 Stellar Iron Singularity, 512000 mB Exotic Matter, 256000 mB Chronon Gas, 32000 mB Inflaton Field, 32000 mB Quantum-Dot Photoresist → 1 Spacetime Catalyst, 1000 mB Dimensional Residue (16,777,216 FE/t, 420 s)  ⟶  12 Higgs Catalyst, 96 Spacetime Alloy Ingot, 12 Tesseract Core, 12 Micro Singularity, 24 Chronoplastic Gasket, 12 Stellar Iron Ingot, 24 Kugelblitz Seed, 12 Spin Foam Lattice, 24 Computronium Singularity, 12 Empirical Notes II, 512000 mB Exotic Matter, 256000 mB Chronon Gas, 32000 mB Stellar Plasma, 32000 mB Strange Matter → 1 Spacetime Catalyst, 1000 mB Dimensional Residue (16,777,216 FE/t, 420 s)
  - 12 Calabi-Yau Manifold, 24 Topological Processor, 48 Exotic Matter Crystal, 12 White Hole Fragment, 24 Chronoplastic Gasket, 12 Anti-Iron Ingot, 24 Fourier Series, 12 Obsidian Singularity, 24 Derivative Operator, 12 Anti-Gold Ingot, 128000 mB Exotic Matter, 64000 mB Chronon Gas, 32000 mB Antihydrogen, 32000 mB Hawking Radiation → 1 Tesseract Core, 250 mB Dimensional Residue (2,097,152 FE/t, 140 s)  ⟶  12 Calabi-Yau Manifold, 24 Topological Processor, 48 Exotic Matter Crystal, 12 White Hole Fragment, 24 Chronoplastic Gasket, 12 Dilithium Singularity, 12 Up & Down Quarks, 12 Neutronium Singularity, 24 Gravitic Lens Element, 12 Coal Singularity, 24 Prime Singularity, 24 Cryptochrome Magnetoreceptor, 128000 mB Exotic Matter, 64000 mB Chronon Gas, 32000 mB Strange Matter, 32000 mB Nebular Gas → 1 Tesseract Core, 250 mB Dimensional Residue (2,097,152 FE/t, 140 s)
  - 12 Tesseract Core, 24 Dark Matter Lens, 12 Photonic Lens, 24 Chronoplastic Gasket, 12 Cosmic Neutronium Singularity, 48 Topological Die, 12 Dyson Shell Segment, 24 Psionic Crystal, 12 Degenerate Armour Plate, 128000 mB Exotic Matter, 32000 mB Exotic Solder, 32000 mB Liquid Antihelium → 1 Tesseract Lens, 250 mB Dimensional Residue (2,097,152 FE/t, 140 s)  ⟶  12 Tesseract Core, 24 Dark Matter Lens, 12 Photonic Lens, 24 Chronoplastic Gasket, 12 Hyperon Crystal, 24 Power Set, 12 Metric Tensor, 24 Bekenstein Crystal, 12 Turing Morphogen, 128000 mB Exotic Matter, 32000 mB Antihydrogen, 32000 mB Bose-Einstein Condensate → 1 Tesseract Lens, 250 mB Dimensional Residue (2,097,152 FE/t, 140 s)
  - 48 Negative Mass Ingot, 48 Exotic Matter Crystal, 12 White Hole Fragment, 12 Einstein Field Core, 24 Chronoplastic Gasket, 12 Riemann Curvature Tensor, 48 Dilithium Crystal, 12 Alcubierre Coil, 48 Topological Insulator, 12 Spin Foam Lattice, 256000 mB Exotic Matter, 32000 mB Chronon Gas, 32000 mB Exotic Solder → 1 Wormhole Throat, 500 mB Dimensional Residue (262,144 FE/t, 105 s)  ⟶  48 Negative Mass Ingot, 48 Exotic Matter Crystal, 12 White Hole Fragment, 12 Einstein Field Core, 24 Chronoplastic Gasket, 12 R-Process Cluster, 24 Kugelblitz Seed, 12 Last Flash of a Black Hole, 24 Higgs Catalyst, 12 Dyson Shell Segment, 24 Hyperlattice Frame, 256000 mB Exotic Matter, 32000 mB Stellar Plasma, 32000 mB Exotic Solder → 1 Wormhole Throat, 500 mB Dimensional Residue (262,144 FE/t, 105 s)
- **Topological Phase Transmuter**
  - 12 Topological Insulator, 24 Void Lattice Shard, 24 Superposition Crystal, 24 Neutronium Plate, 16000 mB Bose-Einstein Condensate, 8000 mB Quantum-Dot Photoresist → 1 Calabi-Yau Manifold (16,384 FE/t, 37.5 s)  ⟶  12 Topological Insulator, 24 Void Lattice Shard, 12 Photonic Lens, 24 Entangled Photon Pair, 16000 mB Bose-Einstein Condensate, 8000 mB Superfluid Helium → 1 Calabi-Yau Manifold (16,384 FE/t, 37.5 s)
  - 6 Superconducting Coil, 6 Magnetic Monopole, 6 Qubit, 6 Electride Crystal, 8000 mB Bose-Einstein Condensate, 8000 mB Nutrient Broth → 1 Higgs Field Capacitor (8,192 FE/t, 25 s)  ⟶  6 Superconducting Coil, 6 Magnetic Monopole, 6 Qubit, 6 Graphene Sheet, 8000 mB Bose-Einstein Condensate, 8000 mB Dark Matter → 1 Higgs Field Capacitor (8,192 FE/t, 25 s)
  - 12 Metamaterial Substrate, 6 Topological Insulator, 24 Femtoscale Inductor, 6 Coherent Wiring Harness, 16000 mB Quantum-Dot Photoresist, 8000 mB Superconducting Solder → 1 Topological Circuit Board (16,384 FE/t, 37.5 s)  ⟶  12 Metamaterial Substrate, 6 Topological Insulator, 24 Femtoscale Inductor, 12 Photonic Processor, 16000 mB Quantum-Dot Photoresist, 8000 mB Bose-Einstein Condensate → 1 Topological Circuit Board (16,384 FE/t, 37.5 s)
  - 6 Metamaterial Sheet, 6 Qubit, 12 Superconducting Coil, 24 Degenerate Matter Chunk, 8000 mB Bose-Einstein Condensate, 8000 mB Warm Helium → 1 Topological Insulator (2,048 FE/t, 18.75 s)  ⟶  6 Metamaterial Sheet, 6 Qubit, 12 Cherenkov Boule, 24 Cosmic Record Fragment, 8000 mB Bose-Einstein Condensate, 8000 mB Superfluid Helium → 1 Topological Insulator (2,048 FE/t, 18.75 s)
- **Transcendent Plasma Forge**
  - 64 Omnium Ingot, 64 Transcendent Metal Ingot, 64 Omnipotence Dust, 64 Ontological Junction, 64 Cosmic Flux Coupler, 64 Resistant Clone Colony, 128 Metamaterial Sheet, 128 Crystallised Law, 128 Anti-Gold Ingot, 64 Absolute Notes I, 128 M-Theory Core, 256000 mB Pleroma, 128000 mB Primordial Soup, 128000 mB Nanite Slurry, 128000 mB Antihydrogen → 2 Apeiron Ingot (1,500,000,000 FE/t, 100 s)  ⟶  64 Omnium Ingot, 64 Transcendent Metal Ingot, 64 Omnipotence Dust, 64 Cosmic Flux Coupler, 64 Mathematics Singularity, 128 Magnetite, 64 Quantum Singularity, 128 Emerald Singularity, 64 Unified Notes I, 128 Fractured Essence, 64 Dimensional Singularity, 256000 mB Pleroma, 128000 mB Superfluid Helium, 128000 mB Warm Helium, 128000 mB Quark-Gluon Plasma → 2 Apeiron Ingot (1,500,000,000 FE/t, 100 s)
  - 512 Spacetime Alloy Ingot, 256 Block of Neutronium, 32 Quark Star Core, 32 Metastable Vacuum Lattice, 128 Cosmic Neutronium Singularity, 32 Wavefunction, 128 Penrose Spindle, 32 M-Theory Core, 128 Stellar Iron Singularity, 32 Cosmic Die, 64 Graviton Wisp Core, 256000 mB Residual Starlight, 2048000 mB Stellar Plasma, 128000 mB Synaptic Fluid, 128000 mB Warm Helium, 128000 mB Superfluid Helium → 16 Cosmic Neutronium Ingot, 250 mB Dimensional Residue (2,097,152 FE/t, 75 s)  ⟶  512 Spacetime Alloy Ingot, 256 Block of Neutronium, 32 Quark Star Core, 32 Empirical Dossier, 128 Neural Lattice, 32 Stress-Energy Tensor, 128 Metric Tensor, 32 Closed Timelike Curve, 128 Stem Cell Culture, 32 Heat Death Ember, 128 Cherenkov Boule, 256000 mB Residual Starlight, 2048000 mB Stellar Plasma, 128000 mB Dark Energy, 128000 mB False Vacuum, 128000 mB Relativity Breakdown → 16 Cosmic Neutronium Ingot, 250 mB Dimensional Residue (2,097,152 FE/t, 75 s)
  - 256 Cosmic Neutronium Ingot, 256 Spacetime Alloy Ingot, 64 Glueball Core, 64 Hyperon Crystal, 32 Supernova Remnant, 128 R-Process Cluster, 32 Unbihexium Ingot, 32 Mirror Alloy Ingot, 32 Retrocausal Crystal, 32 Christoffel Symbol, 32 Universe Seed, 128 Causal Shield Lattice, 512000 mB Residual Starlight, 1024000 mB Stellar Plasma, 128000 mB Liquid Antihelium, 128000 mB Nanite Slurry, 128000 mB Warm Helium → 4 Hypernova Alloy Ingot, 500 mB Dimensional Residue (2,097,152 FE/t, 100 s)  ⟶  256 Cosmic Neutronium Ingot, 256 Spacetime Alloy Ingot, 64 Glueball Core, 64 Hyperon Crystal, 32 Supernova Remnant, 128 R-Process Cluster, 32 Unbihexium Ingot, 32 Mirror Alloy Ingot, 32 Cosmic Processor, 128 Photonic Lens, 32 Retrocausal Crystal, 128 Self-Healing Polymer, 512000 mB Residual Starlight, 1024000 mB Stellar Plasma, 128000 mB Strange Matter, 128000 mB Nutrient Broth, 128000 mB Psionic Ether → 4 Hypernova Alloy Ingot, 500 mB Dimensional Residue (2,097,152 FE/t, 100 s)
  - 64 Hypernova Alloy Ingot, 32 Quark Star Core, 128 Pentaquark Shard, 32 Paradox Residue, 128 Void Lattice Singularity, 32 Bootstrap Artifact, 64 Superpartner Shard, 32 Multiverse Shard, 64 Virtual Quark, 32 Empirical Dossier, 128 Lepton Triplet, 2048000 mB Residual Starlight, 512000 mB Exotic Matter, 128000 mB Psionic Ether, 128000 mB Carborane Superacid, 128000 mB Exotic Solder → 2000 mB Hypernova Plasma, 500 mB Dimensional Residue (4,194,304 FE/t, 125 s)  ⟶  64 Hypernova Alloy Ingot, 32 Quark Star Core, 128 Pentaquark Shard, 32 Gravitational Wave Crystal, 128 Spacetime Alloy Ingot, 32 Boltzmann Brain, 64 Brane Tension Fiber, 32 Primordial Ejecta, 128 Qubit Die, 32 Tidal Alloy Scrap, 128 Imaginary Unit, 2048000 mB Residual Starlight, 512000 mB Exotic Matter, 128000 mB Antihydrogen, 128000 mB Carborane Superacid, 128000 mB Bose-Einstein Condensate → 2000 mB Hypernova Plasma, 500 mB Dimensional Residue (4,194,304 FE/t, 125 s)
  - 64 Block of Omnium, 128 Omnipotence Dust, 32 Apeiron Ingot, 64 Ontological Junction, 64 Cosmic Flux Coupler, 64 Resistant Clone Colony, 128 Prion Lattice, 32 Quarantine Field Matrix, 128 Last Flash of a Black Hole, 64 Absolute Notes III, 128 Fractured Essence, 64 Omnilex Seal, 512000 mB Pleroma, 128000 mB Schrödinger's Limit, 128000 mB Bose-Einstein Condensate, 128000 mB Antihydrogen → 4 Omnipotence Casing (800,000,000 FE/t, 75 s)  ⟶  64 Block of Omnium, 128 Omnipotence Dust, 32 Apeiron Ingot, 64 Cosmic Flux Coupler, 64 Dimensional Singularity, 128 Scintillator Crystal, 64 Mathematics Singularity, 128 Bekenstein Crystal, 64 Spacetime Singularity, 128 Graviton Wisp Core, 32 Absolute Dossier, 512000 mB Pleroma, 128000 mB Antiwater, 128000 mB Raw Star Matter, 128000 mB Hypernova Plasma → 4 Omnipotence Casing (800,000,000 FE/t, 75 s)
  - 32 Stellar Core, 128 Magnetar Shard, 64 Pentaquark Shard, 32 Graviton Wisp Core, 128 Möbius Strip, 32 Virtual Graviton, 128 Scintillator Crystal, 32 Cosmic Die, 128 Penrose Spindle, 32 Dilaton Crystal, 32 Neural Organoid, 2048000 mB Stellar Plasma, 512000 mB Hawking Radiation, 128000 mB Warm Helium, 128000 mB Psionic Ether, 128000 mB Neutrino Flux → 2000 mB Residual Starlight (1,048,576 FE/t, 75 s)  ⟶  32 Stellar Core, 128 Magnetar Shard, 64 Pentaquark Shard, 32 Gravitational Wave Crystal, 128 Raw Anti-Iron, 32 Cosmological Constant, 128 Attoscale Inductor, 32 Silicon Life Spore, 64 R-Process Cluster, 32 Hyperdimensional Logic Wafer, 128 Cherenkov Boule, 2048000 mB Stellar Plasma, 512000 mB Hawking Radiation, 128000 mB Strange Matter, 128000 mB Supercritical Solvent, 128000 mB Causal Solder → 2000 mB Residual Starlight (1,048,576 FE/t, 75 s)
  - 256 Golden Ratio Glass, 32 Omnipotence Dust, 32 Transcendental Crystal, 64 Ontological Junction, 64 Cosmic Flux Coupler, 32 Apoptosis Trigger, 128 Integer Ring, 32 Anomaly Catalogue, 128 String Coupling Crystal, 64 Omnium Ingot, 128 Quantum Field Lattice, 1 Omnium Singularity, 256000 mB Pleroma, 128000 mB Residual Starlight, 128000 mB Dimensional Residue, 128000 mB Primordial Soup → 8 Transcendent Glass (400,000,000 FE/t, 50 s)  ⟶  256 Golden Ratio Glass, 32 Omnipotence Dust, 32 Transcendental Crystal, 32 Brane Tension Fiber, 128 Void Lattice Shard, 32 Wavefunction, 32 Graviton Wisp Core, 32 Virtual Photon, 128 Cosmic Record Fragment, 32 Cosmological Constant, 128 Massless Alloy, 256000 mB Pleroma, 128000 mB Xenoplasm, 128000 mB Tachyon Flux, 128000 mB Chronon Gas → 8 Transcendent Glass (400,000,000 FE/t, 50 s)
  - 256 Hypernova Alloy Ingot, 64 Aleph-Null Shard, 32 Russell Paradox Set, 32 Cosmic Processor, 128 Hyperon Crystal, 32 Grand Unified Core, 32 Flerovium Ingot, 64 Mirror Alloy Ingot, 64 Horizon Shard, 128 Cosmic Neutronium Singularity, 32 Omega Point, 128 Coal Singularity, 512000 mB Hypernova Plasma, 294912 mB Causal Solder, 128000 mB Superconducting Solder, 128000 mB Exotic Matter, 128000 mB Reheating Plasma → 2 Transcendent Metal Ingot, 250 mB Transcendent Plasma, 500 mB Dimensional Residue (16,777,216 FE/t, 200 s)  ⟶  256 Hypernova Alloy Ingot, 64 Aleph-Null Shard, 32 Russell Paradox Set, 32 Cosmic Processor, 128 Hyperon Crystal, 32 Grand Unified Core, 32 Flerovium Ingot, 64 Mirror Alloy Ingot, 64 Ontological Processor, 128 Error-Correction Lattice, 64 Boltzmann Fluctuation, 128 Empirical Dossier, 512000 mB Hypernova Plasma, 294912 mB Causal Solder, 128000 mB Liquid Spacetime, 128000 mB Neutrino Flux, 128000 mB Antihydrogen → 2 Transcendent Metal Ingot, 250 mB Transcendent Plasma, 500 mB Dimensional Residue (16,777,216 FE/t, 200 s)
  - 512 Axiom Stone, 32 Omnipotence Dust, 64 Ontological Junction, 64 Cosmic Flux Coupler, 64 Absolute Notes II, 128 Imaginary Unit, 64 Absolute Notes III, 128 Dilithium Singularity, 32 Apoptosis Trigger, 128 Topological Logic Wafer, 1 Omnium Singularity, 64 Omnilex Seal, 128000 mB Pleroma, 128000 mB Carborane Superacid, 128000 mB Raw Star Matter, 128000 mB Stellar Plasma → 16 Transcendent Stone (200,000,000 FE/t, 50 s)  ⟶  512 Axiom Stone, 32 Omnipotence Dust, 32 Geodesic Thread, 128 Protostar Core, 32 Hamiltonian Operator, 64 Riemann Curvature Tensor, 32 Cosmic Logic Wafer, 128 Cosmic Record Fragment, 32 Cosmic Circuit Board, 128 Survey Map, 128000 mB Pleroma, 128000 mB Schrödinger's Limit, 128000 mB Dark Era Haze, 128000 mB Quantum-Dot Photoresist → 16 Transcendent Stone (200,000,000 FE/t, 50 s)
- **Universe Forge**
  - 16 Pocket Universe, 32 Primordial Ejecta, 64 Cosmic Neutronium Ingot, 32 Eigenstate Lattice, 64 Massless Alloy, 16 Theoretical Notes III, 64 Fractal Core, 32 Boltzmann Brain, 64000 mB Psionic Ether, 64000 mB Hypernova Plasma, 64000 mB Dimensional Residue → 64 Degenerate Matter Chunk, 64 Cherenkov Quartz, 16 Exotic Matter Crystal, 1 Heat Death Remnant, 8000 mB Stellar Plasma (16,777,216 FE/t, 480 s)  ⟶  16 Pocket Universe, 32 Empirical Dossier, 64 Folded Protein Crystal, 32 Frozen Photon, 64 Geiger Tube, 16 Boltzmann Fluctuation, 64 Redstone Singularity, 32 Eigenstate Lattice, 64000 mB Antiwater, 64000 mB Dark Matter, 64000 mB Quark-Gluon Plasma → 64 Degenerate Matter Chunk, 64 Cherenkov Quartz, 16 Exotic Matter Crystal, 1 Heat Death Remnant, 8000 mB Stellar Plasma (16,777,216 FE/t, 480 s)
  - 16 Proof of Everything, 16 Apeiron Ingot, 16 Omnipotence Dust, 32 Cosmic Flux Coupler, 32 Spacetime Bearing, 16 Anomaly Catalogue, 64 Moduli Crystal, 32 Spark of Life, 64 Claytronic Catom, 32 Transfinite Lattice Bolt, 64000 mB False Vacuum, 64000 mB Liquid Metallic Hydrogen, 64000 mB Dark Era Haze → 4 Logos Thread (256,000,000 FE/t, 60 s)  ⟶  16 Proof of Everything, 16 Apeiron Ingot, 16 Omnipotence Dust, 32 Cosmic Flux Coupler, 32 Spacetime Bearing, 32 Absolute Notes II, 64 Glueball Core, 32 Absolute Notes III, 64 Closed Timelike Curve, 32 Immortal Cell Line, 64000 mB Primordial Law, 64000 mB Xenoplasm, 64000 mB Dark Era Haze → 4 Logos Thread (256,000,000 FE/t, 60 s)
  - 16 Universe Seed, 64 Spin Foam Lattice, 128 Cosmic Neutronium Ingot, 16 Theoretical Notes III, 32 Path Integral, 16 Boltzmann Fluctuation, 32 Christoffel Symbol, 16 Tidal Alloy Scrap, 64 Cherenkov Quartz, 16 Multiverse Shard, 1024000 mB False Vacuum, 1024000 mB Inflaton Field, 64000 mB Nebular Gas, 64000 mB Schrödinger's Limit, 64000 mB Nanite Slurry → 1 Pocket Universe, 2000 mB Dimensional Residue (33,554,432 FE/t, 600 s)  ⟶  16 Universe Seed, 64 Spin Foam Lattice, 128 Cosmic Neutronium Ingot, 16 True Vacuum Seed, 64 Programmable Matter, 16 Bootstrap Artifact, 32 Hamiltonian Operator, 16 Theoretical Notes II, 64 Coal Singularity, 16 Feynman Diagram, 1024000 mB False Vacuum, 1024000 mB Inflaton Field, 64000 mB Schrödinger's Limit, 64000 mB Quantum-Dot Photoresist, 64000 mB Superfluid Helium → 1 Pocket Universe, 2000 mB Dimensional Residue (33,554,432 FE/t, 600 s)
  - 16 Universe Seed, 16 True Vacuum Seed, 16 Baryon Asymmetry Crystal, 32 Primordial Nucleosynthesis Ash, 16 Inflaton Condensate, 256000 mB Reheating Plasma, 256000 mB Inflaton Field → 1 Pocket Universe (33,554,432 FE/t, 960 s)  ⟶  16 Universe Seed, 16 True Vacuum Seed, 16 Baryon Asymmetry Crystal, 32 Primordial Nucleosynthesis Ash, 16 Inflaton Condensate, 256000 mB Reheating Plasma, 256000 mB Inflaton Field → 2 Pocket Universe (33,554,432 FE/t, 960 s)
  - 64 Heat Death Remnant, 16 Stress-Energy Tensor, 64 Graviton Crystal, 16 Feynman Diagram, 64 Redstone Singularity, 16 Frozen Photon, 64 Prime Singularity, 16 Empirical Dossier, 512000 mB Inflaton Field, 64000 mB Exotic Matter, 64000 mB Xenoplasm, 64000 mB Dark Era Haze → 1 Universe Seed (8,388,608 FE/t, 240 s)  ⟶  64 Heat Death Remnant, 16 Tidal Alloy Scrap, 64 Cherenkov Lattice, 16 Einstein-Rosen Core, 64 Antihydrogen Crystal, 16 Baryogenesis Seed, 32 Tesseract Lens, 16 Frozen Photon, 512000 mB Inflaton Field, 64000 mB Liquid Metallic Hydrogen, 64000 mB Stellar Plasma, 64000 mB Quark-Gluon Plasma → 2 Universe Seed (8,388,608 FE/t, 240 s)
- **White Hole Emitter**
  - 8 White Hole Fragment, 8 Hawking Emitter, 16 Chronoplastic Gasket, 16 Entangled Bus Bar, 8 Wave Packet, 32 Metamaterial Substrate, 8 Power Set, 64000 mB Hawking Radiation, 16000 mB Dark Matter, 16000 mB Nanite Slurry → 2 Cosmic String Segment, 500 mB Exotic Matter (524,288 FE/t, 60 s)  ⟶  8 White Hole Fragment, 8 Hawking Emitter, 16 Entangled Bus Bar, 8 Pentaquark Shard, 32 Dilithium Crystal, 8 Antiquark Shard, 8 Lepton Triplet, 64000 mB Hawking Radiation, 16000 mB Exotic Solder, 16000 mB Chronon Gas → 2 Cosmic String Segment, 500 mB Exotic Matter (524,288 FE/t, 60 s)
  - 128 Ejecta Rock, 16 Primordial Ejecta, 16 Chronoplastic Gasket, 16 Entangled Bus Bar, 8 Kaluza-Klein Singularity, 32 Octonion Crystal, 8 Christoffel Symbol, 32000 mB Hawking Radiation, 16000 mB Dimensional Residue, 16000 mB Exotic Matter → 1 White Hole Fragment (1,048,576 FE/t, 30 s)  ⟶  128 Ejecta Rock, 16 Primordial Ejecta, 16 Chronoplastic Gasket, 16 Entangled Bus Bar, 8 Cosmic String Lens, 32 Superposition Crystal, 8 Hamiltonian Operator, 32000 mB Hawking Radiation, 16000 mB Carborane Superacid, 16000 mB Quantum-Dot Photoresist → 1 White Hole Fragment (1,048,576 FE/t, 30 s)
  - 8 White Hole Fragment, 32 Block of Neutronium, 16 Chronoplastic Gasket, 16 Entangled Bus Bar, 8 Folded Protein Crystal, 32 Degenerate Matter Chunk, 8 Category Lattice, 256000 mB Quark-Gluon Plasma, 16000 mB Nebular Gas, 16000 mB Superfluid Helium → 1 Stellar Core, 1000 mB Stellar Plasma (2,097,152 FE/t, 180 s)  ⟶  8 White Hole Fragment, 32 Block of Neutronium, 16 Entangled Bus Bar, 8 Modular Form, 8 Assembly Matrix, 8 Tardigrade Culture, 16 Topological Die, 256000 mB Quark-Gluon Plasma, 16000 mB Superfluid Helium, 16000 mB Tachyon Flux → 1 Stellar Core, 1000 mB Stellar Plasma (2,097,152 FE/t, 180 s)
  - 8 Micro Singularity, 8 Chronon Capacitor, 16 Entangled Bus Bar, 8 Muonic Catalyst, 32 Photonic Circuit Board, 8 Recombinant DNA, 8 Arrow of Time Shard, 64000 mB Chronon Gas, 16000 mB Nanite Slurry, 16000 mB Dark Matter → 1 White Hole Fragment, 2 Exotic Matter Crystal, 1000 mB Exotic Matter (1,048,576 FE/t, 90 s)  ⟶  8 Micro Singularity, 8 Chronon Capacitor, 8 Calabi-Yau Manifold, 16 Coherent Wiring Harness, 8 Graphene Sheet, 8 Topological Die, 8 Electride Crystal, 64000 mB Chronon Gas, 16000 mB Supercritical Solvent, 16000 mB Neutrino Flux → 1 White Hole Fragment, 2 Exotic Matter Crystal, 1000 mB Exotic Matter (1,048,576 FE/t, 90 s)

## 1.0.0 — first release

The first public release of Theoretical Sciences for NeoForge 1.21.1.

* 78 machines (37 multiblocks) with physics models, tuning, overclocking, parallel recipes and containment hazards.
* A research tree of 148 entries with derivation puzzles whose values differ in every world.
* Ten dimensions with their own laws, creatures, structures and soundtrack.
* The endgame: the Omnilex Engine, the Transcendental, Alpha Omega, the Omnipotence Core (Materialize, Forge,
  Shutdown, Sundering, Rekindling), Eschaton-powered gear and a forged Omnipotence Core of your own.
* Automatic singularities for every installed mod's ingots.
* The Quantum Codex guide book, its maps grouped into six tiers of progression.
* Every machine screen in one holographic style, with its own glass buttons; the late-game machines open a full
  operator console (live simulation of the recipe, oscilloscope, terminal log and program listing) when the window
  is big enough.
* Its own synthesised sound effects (machines, crystals, fields, rifts, shockwaves...) and its own particles
  (photons, phase motes, arcs, glyphs, shockwave rings...) - nothing borrowed from vanilla's.
* Lighter on memory and load time: one guide (the Codex) instead of three, cheaper animated textures, a smaller
  language file.
* JEI, EMI, Jade, KubeJS and AE2 integration (Jade shows status, recipe and progress).
* Full datapack and KubeJS support (docs/DATAPACKS.md, docs/KUBEJS.md).
