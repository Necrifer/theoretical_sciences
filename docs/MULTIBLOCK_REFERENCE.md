# Multiblock reference: GregTech Odyssey, GT New Horizons, MeatballCraft

Notes for the coming endgame rework. They come from reading the three reference instances: the gtocore structure
definitions (GregTech Odyssey 0.6.0 dev27), the StructureLib shapes compiled into gregtech 5.09.54 (GTNH), and the
ModularMachinery JSON in the Cleanroom MMC 0.5.14 instance (MeatballCraft). The sizes below are measured, not guessed.

## How big they are

| Pack | Structures read | Typical (median) | Largest |
| --- | --- | --- | --- |
| GT Odyssey | 102 | about 110 blocks | Space Elevator connector module, 45 x 5 x 146, about 34,500 blocks |
| GTNH | 436 classes | 50 to 300 blocks | Nano Forge (about 33,000 shape cells), Plasma Forge, Black Hole Compressor, Large Fusion Computer (47 wide) |
| MeatballCraft | 16 sampled | 200 to 500 blocks | Utopic Spires 53 x 27 x 53 (4,274), Dyson Revolver 97 x 31 x 97 (2,764) |
| Theoretical Sciences now | 89 | about 1,840 blocks | Entropy Horizon 113 x 21 x 113 (20,077), Eye of the Horizon 111 x 105 x 111 |

Our structures are already larger than all three packs on average. The rework should spend effort on silhouette and
variety, not on size.

## Shape families worth borrowing

1. **Ring or torus** (GTNH Large Fusion Computer, MeatballCraft Particle Accelerator, GTNH Synchrotron). A hollow ring a
   few blocks thick with the controller on the rim, plus spokes or a cross brace through the middle. Fill is only 15 to
   30 percent, so it reads as huge without the block cost.
2. **Twin towers or mirrored pylons** (GTNH Black Hole Compressor, Eye of Harmony). Two matching structures 30 or more
   blocks apart with the work happening in the empty gap between them. This suits field and beam machines.
3. **Sphere with equatorial arms** (MeatballCraft Dyson Sphere, 29 x 13 x 29). A shell that tapers top and bottom, with a
   flat belt that reaches out to the full width.
4. **Disc with a stepped dome** (MeatballCraft Black Hole Juicer, 15 x 5 x 15). A rounded footprint, solid caps, hollow
   middle layers.
5. **Long tube or linear line** (MeatballCraft Linear Fusion Accelerator 5 x 5 x 15, GTNH Component Assembly Line, Odyssey
   integrated_assembler 10 x 5 x 54). Repeating segments, so building it is easy to learn but long.
6. **Spire field** (MeatballCraft Utopic Spires). A huge solid base (2,229 blocks in the bottom layer alone) with thin
   spires that get sparser going up. Most of the cost is in the base.
7. **Organic or tree** (MeatballCraft Tree of Life 26 x 20 x 25). Root mass, a narrow trunk, then a canopy with an
   irregular outline. A good template for Biology endgame machines.
8. **Solid cube** (MeatballCraft The Cube, 15 cubed at 63 percent fill; Two Phase Fission Reactor fully solid). Use it
   sparingly, as a deliberate brute force contrast.
9. **Stacked tower with a changing cross section** (GTNH Mega Distillation Tower, Odyssey extreme_electric_furnace).
   Each layer adds a different role (inputs, heat, outputs).

## Rules that keep them readable

- The controller is always on an outer face, at ground level or on a platform the player can reach.
- Hatches and ports sit in one or two marked bands, never scattered through the whole structure.
- The largest builds repeat a module (fusion ring segments, spires, arms) so the player learns one piece and copies it.
- A hollow interior is common, and it is where effects play (plasma, beams, the star). Our `effect_center` and `fx`
  fields already support this.
- Big structures mix two or three casing types to mark zones (frame, active core, shielding), which also helps the art
  revamp: a machine's casings set its colour identity.

## Ideas mapped to our endgame

- Rings: Transcendent Plasma Forge, Entropy Horizon (already wide and flat), a future collider.
- Twin pylons: Eye of the Horizon, warp field machines.
- Sphere with arms: Matrioshka Brain.
- Tree: Abiogenesis Crucible.
- Spires: Omnilex Engine.

Older endgame structures must keep at least their current block counts when they are reworked.
