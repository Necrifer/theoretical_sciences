package com.theoreticalsciences.machine;

import com.theoreticalsciences.QHConfig;
import com.theoreticalsciences.TheoreticalSciences;
import com.theoreticalsciences.api.event.MultiblockFormedEvent;
import com.theoreticalsciences.multiblock.MultiblockDefinition;
import com.theoreticalsciences.multiblock.Multiblocks;
import com.theoreticalsciences.registry.QHBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Controller of a multiblock: the machine engine plus a periodic structure check and port linking. */
public class ControllerBlockEntity extends MachineBlockEntity {
    private boolean formed;
    private final Set<BlockPos> ports = new HashSet<>();
    private int checkTimer, lookRefresh;
    /** Lowest coil / field-emitter tier in the formed structure (0 = this structure has none). */
    private int coilTier, emitterTier;

    public int coilTier() {
        return coilTier;
    }

    public int emitterTier() {
        return emitterTier;
    }

    /** Parallel limit: the profile's base, multiplied by the field-emitter tier (GT-style: better parts, more parallels). */
    /**
     * Round 35: the most recipes a structure may run at once - its base parallel times its emitter tier, never more
     * than {@link #PARALLEL_CAP} (a pack's huge emitter tiers would otherwise overflow, or make one tick do millions
     * of runs and stall the server).
     */
    public static final int PARALLEL_CAP = 4096;

    public int maxParallel() {
        var p = tuningProfile();
        return p == null ? 1 : (int) Math.min(PARALLEL_CAP, (long) Math.max(1, p.maxParallel()) * Math.max(1, emitterTier));
    }

    public ControllerBlockEntity(MachineType type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        this.physics = new com.theoreticalsciences.machine.physics.PhysicsState(type.id(), () -> {
            markSync();
            setChanged();
        }, () -> level != null && level.isClientSide);
    }

    // ------------------------------------------------------------------ physics (QMD-style operation)
    /** The machine's physics: operator settings, temperature, coolant loop and the last evaluation. */
    public final com.theoreticalsciences.machine.physics.PhysicsState physics;

    /** True when this multiblock runs on a physics model (every multiblock that has one shipped). */
    public boolean hasPhysics() {
        return physics.model() != null;
    }

    /**
     * 1.4.0: what the machine's physics sees of the world (the telescopes need a dark, clear, open sky; the Solar
     * Furnace the Sun; the Radio Telescope quiet): the sky above the structure's top, day and night from the Sun's
     * elevation, the Moon's phase, rain, the height it stands at, and how many other machines are running nearby.
     */
    private void senseWorld(ServerLevel level) {
        double[] w = physics.world;
        MultiblockDefinition def = definition();
        Vec3 c = effectCenter();
        int top = worldPosition.getY() + (def == null ? 1 : def.sizeY - def.cy);
        BlockPos above = BlockPos.containing(c.x, Math.max(top, c.y + 1), c.z);
        boolean skyLight = level.dimensionType().hasSkyLight() && !level.dimensionType().hasCeiling();
        w[0] = skyLight && level.canSeeSky(above) ? 1 : 0;
        double elev = Math.cos(level.getTimeOfDay(1F) * Math.PI * 2);           // 1 at noon, -1 at midnight
        w[1] = skyLight ? Math.max(0, Math.min(1, (-elev - 0.05) / 0.15)) : 0;  // dark once the Sun is ~12 degrees down
        w[2] = skyLight ? Math.max(0, elev) : 0;
        w[3] = (1 + Math.cos(level.getMoonPhase() * Math.PI / 4)) / 2;          // phase 0 is full
        boolean wet = level.isRaining() && level.getBiome(above).value().getPrecipitationAt(above) != net.minecraft.world.level.biome.Biome.Precipitation.NONE;
        w[4] = wet ? 1 : 0;
        w[5] = worldPosition.getY();
        w[6] = noisyNeighbours(level);
    }

    /**
     * 1.6.0: a spinning black hole drags spacetime round with it. While the Penrose Ergosphere or the Kugelblitz Chamber
     * runs, everyone within 8 blocks of its core is carried sideways (Frame Drag) - an effect only, never the blocks.
     */
    private void frameDrag(ServerLevel level) {
        if ((type != MachineType.PENROSE_ERGOSPHERE && type != MachineType.KUGELBLITZ_CHAMBER) || !formed || !isActive()) return;
        Vec3 c = effectCenter();
        for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class, new AABB(c, c).inflate(8)))
            if (!p.isSpectator() && p.position().distanceToSqr(c) <= 64)
                p.addEffect(new net.minecraft.world.effect.MobEffectInstance(com.theoreticalsciences.registry.QHEffects.FRAME_DRAG, 40, 0, false, true));
    }

    /**
     * 1.8.0: a running Tesla coil arcs to whoever stands within six blocks of its toroid - a visual-only bolt (it never
     * sets anything alight or changes a block) and the shock itself.
     */
    private void teslaStrike(ServerLevel level) {
        if (!formed || !isActive()) return;
        Vec3 c = effectCenter();
        for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class, new AABB(c, c).inflate(6)))
            if (!p.isSpectator() && !p.isCreative() && p.position().distanceToSqr(c) <= 36 && level.random.nextInt(3) == 0) {
                var bolt = net.minecraft.world.entity.EntityType.LIGHTNING_BOLT.create(level);
                if (bolt != null) {
                    bolt.setVisualOnly(true);
                    bolt.moveTo(p.getX(), p.getY(), p.getZ());
                    level.addFreshEntity(bolt);
                }
                p.hurt(level.damageSources().lightningBolt(), 4F);
                p.displayClientMessage(Component.translatable("message.theoreticalsciences.tesla_strike").withStyle(ChatFormatting.AQUA), true);
            }
    }

    /** Other machines working within 24 blocks: their switching noise is what a radio receiver hears. */
    private int noisyNeighbours(ServerLevel level) {
        int n = 0;
        int cx = worldPosition.getX() >> 4, cz = worldPosition.getZ() >> 4;
        for (int dx = -2; dx <= 2; dx++)
            for (int dz = -2; dz <= 2; dz++) {
                if (!level.hasChunk(cx + dx, cz + dz)) continue;
                for (var be : level.getChunk(cx + dx, cz + dz).getBlockEntities().values())
                    if (be != this && be instanceof MachineBlockEntity m && m.isActive() && be.getBlockPos().distSqr(worldPosition) <= 24 * 24) n++;
            }
        return n;
    }

    /** Re-evaluates the model for the current structure. */
    private void evaluatePhysics(boolean on) {
        physics.size = units();
        physics.evaluate(coilTier, emitterTier, overclockNow(), parallelCount(), on, tuning.autoCalibrator);
    }

    // ------------------------------------------------------------------ Laplace's Demon (creative tuning)
    /** What the demon did: whether it found a point inside every limit and window, the rate there, and the coolant used. */
    public record DemonResult(boolean ok, double rate, String coolant, int violations) {}

    private boolean demonBusy;

    /**
     * Creative: put this machine exactly where the recipe needs it. The search (see {@link com.theoreticalsciences.machine.physics.DemonSolver})
     * runs off the server thread on a private copy of the machine's physics; the answer is applied back here.
     * Returns false if a search is already running for this controller.
     */
    public boolean demonTune(ServerLevel level, @javax.annotation.Nullable com.theoreticalsciences.recipe.QHRecipe r, java.util.function.Consumer<DemonResult> done) {
        if (demonBusy) return false;
        tuning.calibrate();
        if (!hasPhysics()) {
            if (tuningProfile() != null) tuning.setting = tuning.target;
            markSync();
            setChanged();
            done.accept(new DemonResult(true, 1, "", 0));
            return true;
        }
        demonBusy = true;
        var phys = r == null ? com.theoreticalsciences.machine.physics.RecipePhysics.NONE : r.physics;
        var job = new com.theoreticalsciences.machine.physics.DemonSolver(type.id(), phys, coilTier, emitterTier, overclockNow(),
                tuningProfile() == null ? 1 : tuning.parallel, physics.params.clone(), physics.coolant.getFluid().isEmpty() ? null : physics.coolant.getFluid().getFluid(),
                worldPosition.asLong()).size(units()).world(physics.world);
        var server = level.getServer();
        java.util.concurrent.CompletableFuture.supplyAsync(job::solve, net.minecraft.Util.backgroundExecutor()).whenComplete((sol, err) -> server.execute(() -> {
            demonBusy = false;
            // round 35: a model the search cannot work with (a pack's broken edit) fails politely - said in chat, logged here
            if (err != null) com.theoreticalsciences.TheoreticalSciences.LOGGER.warn("Laplace's Demon could not search {} at {}: {}", type.id(), worldPosition, err.toString());
            if (isRemoved() || sol == null) {
                done.accept(new DemonResult(false, 0, "", -1));
                return;
            }
            var m = physics.model();
            if (m == null) return;
            physics.recipe = phys;
            for (int i = 0; i < Math.min(sol.params().length, m.params.size()); i++) physics.setParam(i, sol.params()[i]);
            if (sol.coolant() != null) physics.coolant.setFluid(new net.neoforged.neoforge.fluids.FluidStack(sol.coolant(), physics.coolant.getCapacity()));
            physics.spent.setFluid(net.neoforged.neoforge.fluids.FluidStack.EMPTY);
            physics.calibrate();
            physics.temperature = sol.temperature();
            for (int i = 0; i < m.params.size(); i++)
                if ("setting".equals(m.params.get(i).bind())) tuning.setting = (float) physics.params[i];
            tuning.calibrate();
            evaluatePhysics(isActive());
            markSync();
            setChanged();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
            String cname = sol.coolant() == null ? "" : net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(sol.coolant()).toString();
            done.accept(new DemonResult(sol.ok(), sol.rate(), cname, sol.ok() ? 0 : 1));
        }));
        return true;
    }

    /** Sets an operator parameter from the screen (validated on the server). */
    public void applyPhysics(int index, double value) {
        var m = physics.model();
        if (m == null || index < 0 || index >= m.params.size()) return;
        physics.setParam(index, value);
        // a parameter bound to the old dial (the Vacuum Harvester's resonance) writes through to it
        if ("setting".equals(m.params.get(index).bind())) tuning.setting = (float) physics.params[index];
        evaluatePhysics(isActive());
        markSync();
        setChanged();
    }

    /** What stops the machine, physically, or null if it may run. Quenches happen here. */
    @javax.annotation.Nullable
    private MachineStatus physicsGate(ServerLevel level, boolean mayQuench) {
        var m = physics.model();
        if (m == null) return null;
        if (physics.quenchTicks > 0) return MachineStatus.QUENCHED;
        if (physics.stopped()) return MachineStatus.UNSTABLE;
        if (physics.quenchIdx >= 0) {
            // a machine that is not running yet will not ramp into a quench: the interlock holds it (and with the
            // Equilibrium Lattice it never quenches at all: it simply holds until the condition clears)
            if (!mayQuench || tuning.perfect()) return MachineStatus.UNSTABLE;
            quench(level);
            return MachineStatus.QUENCHED;
        }
        if (physics.damageIdx >= 0) return MachineStatus.OVERHEATING;
        if (physics.windowIdx >= 0) return MachineStatus.OUT_OF_WINDOW;
        if (physics.rate <= 0.001) return MachineStatus.UNSTABLE;
        return null;
    }

    /** Superconductors went normal (or a plasma disrupted): the stored energy dumps as heat, the containment takes a hit. */
    private void quench(ServerLevel level) {
        physics.quench();
        progress = 0;
        com.theoreticalsciences.machine.tuning.TuningProfile tp = tuningProfile();
        tuning.hit(tp == null ? 6F : 4F + tp.severity());
        Vec3 c = effectCenter();
        level.sendParticles(com.theoreticalsciences.registry.QHParticles.ARC_SPARK.get(), c.x, c.y, c.z, 60, 1.5, 1.5, 1.5, 0.4);
        level.sendParticles(com.theoreticalsciences.registry.QHParticles.VAPOUR.get(), c.x, c.y, c.z, 40, 1.5, 1.0, 1.5, 0.08);
        level.playSound(null, worldPosition, com.theoreticalsciences.registry.QHSounds.VENT.get(), net.minecraft.sounds.SoundSource.BLOCKS, 2.0F, 0.6F);
        level.playSound(null, worldPosition, com.theoreticalsciences.registry.QHSounds.SHOCKWAVE.get(), net.minecraft.sounds.SoundSource.BLOCKS, 0.8F, 1.6F);
        var m = physics.model();
        Component why = null;
        if (m != null && physics.stateIdx >= 0 && physics.stateIdx < m.states.size())
            why = Component.translatable("physics.theoreticalsciences." + type.id() + ".s." + m.states.get(physics.stateIdx).id());
        else if (m != null && physics.quenchIdx >= 0 && physics.quenchIdx < m.constraints.size())
            why = Component.translatable("physics.theoreticalsciences." + type.id() + ".c." + m.constraints.get(physics.quenchIdx).id());
        if (owner != null && why != null) {
            ServerPlayer p = level.getServer().getPlayerList().getPlayer(owner);
            if (p != null) p.displayClientMessage(Component.translatable("message.theoreticalsciences.quench",
                    Component.translatable("block.theoreticalsciences." + type.id()), why).withStyle(ChatFormatting.GOLD), true);
        }
        if (tuning.faulted && meltdown <= 0) beginMeltdown(level);
        markSync();
    }

    public MultiblockDefinition definition() {
        MultiblockDefinition base = Multiblocks.get(type.multiblock).orElse(null);
        return base == null || !base.variable() ? base : base.variant(extraUnits);
    }

    /** 1.3.5: extra copies of a variable-height structure's repeated unit (0 = the smallest build). */
    private int extraUnits;

    /** Units in the structure as built: the cascade's stages. 1 for every fixed-size machine. */
    public int units() {
        MultiblockDefinition base = Multiblocks.get(type.multiblock).orElse(null);
        return base == null ? 1 : base.units(extraUnits);
    }

    /** The next taller build of a variable-height structure (null at full height or for fixed structures): the Assembler grows it. */
    @javax.annotation.Nullable
    public MultiblockDefinition nextSize() {
        MultiblockDefinition base = Multiblocks.get(type.multiblock).orElse(null);
        return base == null || !base.variable() || extraUnits >= base.repeatMax ? null : base.variant(extraUnits + 1);
    }

    /** Picks the tallest build that stands complete; if none does, keeps the size it had (so the hologram shows that). */
    private void pickSize(Level level) {
        MultiblockDefinition base = Multiblocks.get(type.multiblock).orElse(null);
        if (base == null || !base.variable()) return;
        for (int e = base.repeatMax; e >= 0; e--) {
            if (base.variant(e).quickMatch(level, worldPosition, facing())) {
                if (e != extraUnits) {
                    extraUnits = e;
                    recipeDirty();
                    markSync();
                    setChanged();
                }
                return;
            }
        }
    }

    public boolean isFormed() {
        return formed;
    }

    public Set<BlockPos> ports() {
        return ports;
    }

    public Direction facing() {
        BlockState st = getBlockState();
        return st.hasProperty(MachineBlock.FACING) ? st.getValue(MachineBlock.FACING) : Direction.NORTH;
    }

    /** 1.3.8: how much bigger than standard this machine draws its working animation. */
    public float effectScale() {
        MultiblockDefinition def = definition();
        return def == null ? 1F : (float) def.escale;
    }

    public Vec3 effectCenter() {
        MultiblockDefinition def = definition();
        return def == null ? Vec3.atCenterOf(worldPosition) : def.effectCenter(worldPosition, facing());
    }

    @Override
    public boolean canOperate() {
        // The Omnilex Engine simulates the World Tree and the Ocean of Law, so it only works where they are; the Vacuum
        // Fluctuation Harvester needs the Quantum Foam to harvest from (round 35: MachineType.allowedDimensions)
        if (wrongDimension()) return false;
        if (!formed || tuning.faulted) return false;
        // a generator's physics is checked while it burns: outside its limits it simply stops producing
        if (type.generator && hasPhysics() && burnTime > 0 && physicsBlock != null) return false;
        return true;
    }

    /** Last tick a recipe was waiting on (only) the physics: the machine stays energised. */
    private long energisedAt = -100;

    /** Generators only: why the physics is holding the machine (shown instead of "incomplete"). */
    @javax.annotation.Nullable
    private MachineStatus physicsBlock;

    // ------------------------------------------------------------------ tuning (GTNH-style interaction)
    public final com.theoreticalsciences.machine.tuning.TuningState tuning = new com.theoreticalsciences.machine.tuning.TuningState();

    @javax.annotation.Nullable
    public com.theoreticalsciences.machine.tuning.TuningProfile tuningProfile() {
        return com.theoreticalsciences.machine.tuning.TuningProfile.of(type);
    }

    @Override
    protected MachineStatus blockedStatus() {
        // round 35: say why it will not run - not "incomplete" when the structure is fine but in the wrong dimension
        if (formed && wrongDimension()) return MachineStatus.WRONG_DIMENSION;
        if (formed && tuning.faulted) return MachineStatus.FAULT;
        if (formed && physicsBlock != null) return physicsBlock;
        return MachineStatus.INCOMPLETE;
    }

    /**
     * How much faster the coils make it: each coil tier above the first gives +25% (a pack's ts_machines
     * "coil_speed_bonus" changes the 25%), unless a datapack coil tier sets its own "speed_bonus" (round 31).
     */
    public double coilSpeed() {
        if (coilTier <= 0) return 1.0;
        Double own = com.theoreticalsciences.custom.ComponentTiers.speedBonus("coil", coilTier);
        if (own != null) return Math.max(0.01, 1.0 + own);
        return coilTier > 1 ? 1.0 + com.theoreticalsciences.custom.MachineParams.coilSpeedBonus(type) * (coilTier - 1) : 1.0;
    }

    @Override
    protected double speedFactor() {
        double coil = coilSpeed();
        int oc = overclockNow();
        if (hasPhysics()) return (1 << oc) * physics.rate * coil;   // the physics says how well it is running
        return tuningProfile() == null ? coil : (1 << oc) * tuning.efficiency() * coil;
    }

    @Override
    protected boolean tierOk(com.theoreticalsciences.recipe.QHRecipe r) {
        return r.minTier <= coilTier;
    }

    @Override
    protected double energyFactor() {
        if (tuningProfile() == null) return 1.0;
        double f = Math.pow(4, overclockNow()) * parallelCount();
        if (hasPhysics() && type.generator) return 1.0;
        // power drawn follows how hard the physics is driving it: energy per craft stays the same
        if (hasPhysics()) f *= Math.max(0.05, physics.rate);
        return tuning.autoCalibrator ? f * 1.25 : f;    // the auto-calibrator costs a 25% power tax
    }

    /** Round 35: the overclock in use, within what the machine allows now (a /reload may have lowered the limit). */
    @Override
    public int shownOverclock() {
        return overclockNow();
    }

    public int overclockNow() {
        var p = tuningProfile();
        return p == null ? 0 : Math.max(0, Math.min(tuning.overclock, Math.min(16, p.maxOverclock())));
    }

    @Override
    protected int parallelCount() {
        // (a save from before a /reload that lowered the limit may hold more than is allowed now)
        return tuningProfile() == null ? 1 : Math.max(1, Math.min(tuning.parallel, maxParallel()));
    }

    @Override
    protected double outputFactor() {
        if (hasPhysics()) return physics.rate * (tuning.autoCalibrator ? 0.9 : 1.0);
        return tuningProfile() == null ? 1.0 : tuning.efficiency() * (tuning.autoCalibrator ? 0.9 : 1.0);
    }

    /** Applies a GUI change from a player (validated on the server). */
    public void applyTuning(int field, int value) {
        com.theoreticalsciences.machine.tuning.TuningProfile p = tuningProfile();
        if (p == null) return;
        switch (field) {
            case 0 -> tuning.setting = net.minecraft.util.Mth.clamp(tuning.setting + value / 10F, 0F, 100F);
            case 1 -> tuning.overclock = net.minecraft.util.Mth.clamp(tuning.overclock + value, 0, p.maxOverclock());
            case 2 -> tuning.parallel = net.minecraft.util.Mth.clamp(tuning.parallel + value, 1, maxParallel());
            default -> {}
        }
        markSync();
        setChanged();
    }

    private static ResourceLocation portId() {
        return TheoreticalSciences.id("interface_port");
    }

    // ------------------------------------------------------------------ ME networks behind the Interface Ports (AE2)
    private List<net.minecraft.world.item.ItemStack> meItems = List.of();
    private List<net.neoforged.neoforge.fluids.FluidStack> meFluids = List.of();
    private long meAt;
    private boolean meDirty = true;
    @javax.annotation.Nullable private com.theoreticalsciences.compat.MEAccess meNet;

    /** The online networks this multiblock's Interface Ports are cabled to. */
    private List<com.theoreticalsciences.compat.MEAccess> networks() {
        List<com.theoreticalsciences.compat.MEAccess> out = new ArrayList<>();
        if (level == null || !isFormed() || !com.theoreticalsciences.compat.MEBridge.present()) return out;
        for (BlockPos p : ports)
            if (level.isLoaded(p) && level.getBlockEntity(p) instanceof PortBlockEntity pe) {
                var n = pe.network();
                if (n != null) out.add(n);
            }
        return out;
    }

    /** The first network (several ports on one network would otherwise count its contents twice). */
    @javax.annotation.Nullable
    private com.theoreticalsciences.compat.MEAccess network() {
        List<com.theoreticalsciences.compat.MEAccess> n = networks();
        return n.isEmpty() ? null : n.get(0);
    }

    /** At most once a tick (and every 10 ticks otherwise) the network is asked about what this machine could use. */
    private void refreshME() {
        if (level == null) return;
        long now = level.getGameTime();
        if (!meDirty && now >= meAt && now - meAt < 10) return;
        if (meDirty && now == meAt && meNet != null) return;
        meAt = now;
        meDirty = false;
        meNet = network();
        if (meNet == null) {
            meItems = List.of();
            meFluids = List.of();
            return;
        }
        var w = MEWanted.of(level, type.recipeType);
        meItems = meNet.items(w.items());
        meFluids = meNet.fluids(w.fluids());
    }

    @Override
    protected List<net.minecraft.world.item.ItemStack> extraItems() {
        refreshME();
        List<net.minecraft.world.item.ItemStack> out = new ArrayList<>(meItems.size());
        for (var s : meItems) out.add(s.copy());
        return out;
    }

    @Override
    protected List<net.neoforged.neoforge.fluids.FluidStack> extraFluids() {
        refreshME();
        List<net.neoforged.neoforge.fluids.FluidStack> out = new ArrayList<>(meFluids.size());
        for (var s : meFluids) out.add(s.copy());
        return out;
    }

    @Override
    protected boolean takeExtra(List<net.minecraft.world.item.ItemStack> want, List<net.neoforged.neoforge.fluids.FluidStack> wantF) {
        var me = meNet;
        if (me == null || !me.online()) return false;
        boolean ok = me.extractAll(want, wantF);
        if (ok) {
            // keep the snapshot true without asking the network again this tick
            for (var w : want)
                for (var s : meItems) if (net.minecraft.world.item.ItemStack.isSameItemSameComponents(s, w)) s.shrink(w.getCount());
            for (var w : wantF)
                for (var s : meFluids) if (net.neoforged.neoforge.fluids.FluidStack.isSameFluidSameComponents(s, w)) s.shrink(w.getAmount());
        } else meDirty = true;
        return ok;
    }

    private boolean hasOutput() {
        for (int i = 0; i < type.itemOutputs; i++) if (!items.getStackInSlot(type.itemInputs + i).isEmpty()) return true;
        for (int i = 0; i < type.fluidOutputs; i++) if (!tanks[type.fluidInputs + i].getFluid().isEmpty()) return true;
        return false;
    }

    /** Products go straight into the network (like an ME output bus). */
    private void pushToME() {
        var me = network();
        if (me == null) return;
        boolean moved = false;
        for (int i = 0; i < type.itemOutputs; i++) {
            var s = items.getStackInSlot(type.itemInputs + i);
            if (s.isEmpty()) continue;
            long in = me.insert(s.copy());
            if (in > 0) {
                items.setStackInSlot(type.itemInputs + i, s.copyWithCount((int) (s.getCount() - in)));
                moved = true;
            }
        }
        for (int i = 0; i < type.fluidOutputs; i++) {
            var t = tanks[type.fluidInputs + i];
            if (t.getFluid().isEmpty()) continue;
            long in = me.insert(t.getFluid().copy());
            if (in > 0) {
                t.drain((int) in, net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
                moved = true;
            }
        }
        if (moved) onInventoryChanged();
    }

    @Override
    protected void preTick(ServerLevel level) {
        if (level.getGameTime() % 5 == 1 && hasOutput()) pushToME();     // 4x a second, only with something to push
        if ((level.getGameTime() + worldPosition.hashCode()) % 20 == 0 && hasPhysics()) senseWorld(level);
        if (level.getGameTime() % 20 == 0) frameDrag(level);
        if (type == MachineType.TESLA_COIL && level.getGameTime() % 30 == 0) teslaStrike(level);
        if (--checkTimer <= 0) {
            checkTimer = 40;
            pickSize(level);
            MultiblockDefinition def = definition();
            if (def == null) return;
            MultiblockDefinition.ScanResult scan = def.scan(level, worldPosition, facing(), portId());
            // round 36: part of the structure sits in an unloaded chunk (a chunk loader keeps only this one): keep
            // the state from the last full check rather than "breaking" it - nothing outside loaded chunks is touched
            if (!scan.partial()) {
                setFormed(level, scan.complete(), scan.ports());
                updateTiers(scan);
                // casings that were unloaded when the structure formed get their formed look once they are back
                if (formed && ++lookRefresh % 10 == 0) applyFormedLook(level, true);
            }
            // round 31: a /reload may have lowered this machine's limits (data/<ns>/ts_machines)
            var lim = tuningProfile();
            if (lim != null && (tuning.overclock > lim.maxOverclock() || tuning.parallel > maxParallel())) {
                tuning.overclock = Math.min(tuning.overclock, lim.maxOverclock());
                tuning.parallel = Math.max(1, Math.min(tuning.parallel, maxParallel()));
                markSync();
                setChanged();
            }
        }
        com.theoreticalsciences.machine.tuning.TuningProfile tp = tuningProfile();
        if (hasPhysics()) tickPhysics(level, tp);
        else if (tp != null) {
            boolean wasFaulted = tuning.faulted;
            int before = (int) tuning.integrity;
            tuning.tick(tp, formed && isActive(), level.random);
            if (type == MachineType.VACUUM_HARVESTER && !wrongDimension()) {
                int m = harvestTarget(level, null);
                tuning.target = (float) com.theoreticalsciences.foam.FoamField.resonance(level, m, worldPosition);
            }
            if (tuning.faulted && !wasFaulted) beginMeltdown(level);
            if (meltdown > 0) tickMeltdown(level);
            if ((int) tuning.integrity != before || level.getGameTime() % 20 == 0) markSync();
            if (level.getGameTime() % 200 == 0) setChanged();
        }
        if (formed && isActive()) {
            hazards(level);
            if (type == MachineType.AXIOMATIC_BREACH_ENGINE) breach(level);
        }
    }

    /** One tick of the machine's physics: disturbance, evaluation, heat into the coolant, states, limits. */
    private void tickPhysics(ServerLevel level, @javax.annotation.Nullable com.theoreticalsciences.machine.tuning.TuningProfile tp) {
        boolean running = formed && isActive();
        boolean wasFaulted = tuning.faulted;
        int before = (int) tuning.integrity;
        if (physics.quenchTicks > 0) physics.quenchTicks--;
        // "on" = energised: running, or holding a recipe it is trying to bring into its window (fields up, hole held),
        // or (generators) with fuel burning
        boolean burning = type.generator && burnTime > 0 && formed && !tuning.faulted && physicsBlock == null;
        boolean on = running || burning || (formed && !tuning.faulted && level.getGameTime() - energisedAt <= 2);
        boolean working = running || burning;
        physics.stabiliser = tuning.stabiliser;
        physics.tickDisturbance(tp, on, level.random, QHConfig.TUNING_DIFFICULTY.get());
        evaluatePhysics(on);
        physics.tickThermal();
        physics.tickStates(on);
        if (formed && working && !physics.stopped()) {
            if (physics.quenchIdx >= 0 && physics.quenchTicks <= 0 && !tuning.perfect()) quench(level);
            if (physics.damageIdx >= 0 && level.getGameTime() % 20 == 0) {
                tuning.hit(1.5F);       // overheating: the structure loses integrity every second
                Vec3 c = effectCenter();
                level.sendParticles(com.theoreticalsciences.registry.QHParticles.VAPOUR.get(), c.x, c.y, c.z, 6, 1.0, 1.0, 1.0, 0.02);
            }
        }
        if (formed && on) {
            if (physics.stateIdx >= 0) {
                var m = physics.model();
                var kind = m == null ? null : m.states.get(physics.stateIdx).outOfRange();
                if (kind == com.theoreticalsciences.machine.physics.PhysicsModel.Kind.QUENCH && physics.quenchTicks <= 0 && !tuning.perfect()) quench(level);
                else if (kind == com.theoreticalsciences.machine.physics.PhysicsModel.Kind.DAMAGE && level.getGameTime() % 20 == 0) tuning.hit(1.5F);
            }
        }
        if (type.generator) {
            // re-checked against an evaluation as it would run, so a held generator can come back
            if (burnTime > 0 && formed && !tuning.faulted) {
                if (!burning) evaluatePhysics(true);
                physicsBlock = physicsGate(level, burning);
            } else physicsBlock = null;
        }
        float stress = physics.wearIdx >= 0 || physics.damageIdx >= 0 ? 4F : 1F;
        tuning.tickPhysics(tp, running, stress);
        if (type == MachineType.VACUUM_HARVESTER && !wrongDimension()) {
            int m = harvestTarget(level, null);
            tuning.target = (float) com.theoreticalsciences.foam.FoamField.resonance(level, m, worldPosition);
        }
        if (tuning.faulted && !wasFaulted && meltdown <= 0) beginMeltdown(level);
        if (meltdown > 0) tickMeltdown(level);
        if ((int) tuning.integrity != before || level.getGameTime() % 10 == 0) markSync();
        if (level.getGameTime() % 200 == 0) setChanged();
    }

    private void updateTiers(MultiblockDefinition.ScanResult scan) {
        int c = scan.complete() ? scan.tier("coil") : 0, e = scan.complete() ? scan.tier("emitter") : 0;
        if (c != coilTier || e != emitterTier) {
            coilTier = c;
            emitterTier = e;
            if (tuning.parallel > maxParallel()) tuning.parallel = Math.max(1, maxParallel());
            recipeDirty();
            markSync();
            setChanged();
        }
    }

    /** Ticks left before a failing containment gives way (the animation plays in the meantime). */
    private int meltdown;
    public static final int MELTDOWN_TICKS = 80;

    private void beginMeltdown(ServerLevel level) {
        meltdown = MELTDOWN_TICKS;
        progress = 0;
        com.theoreticalsciences.machine.tuning.TuningProfile tp = tuningProfile();
        int hazard = tp == null ? 0 : tp.hazard().ordinal();
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayersNear(level, null, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), 160,
                new com.theoreticalsciences.network.ContainmentFailurePayload(worldPosition, hazard));
        if (owner != null) {
            ServerPlayer p = level.getServer().getPlayerList().getPlayer(owner);
            if (p != null) p.displayClientMessage(Component.translatable("message.theoreticalsciences.meltdown",
                    Component.translatable("block.theoreticalsciences." + type.id())).withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD), true);
        }
    }

    /** The structure strains: alarms, sparks and light leaking out of the casing, faster and faster, then the blast. */
    private void tickMeltdown(ServerLevel level) {
        int t = MELTDOWN_TICKS - meltdown;
        Vec3 c = effectCenter();
        int alarmEvery = Math.max(6, 20 - t / 5);
        if (t % alarmEvery == 0)
            level.playSound(null, worldPosition, com.theoreticalsciences.registry.QHSounds.ALARM.get(), net.minecraft.sounds.SoundSource.BLOCKS, 2.5F, t % (2 * alarmEvery) == 0 ? 0.6F : 0.5F);
        if (t % 10 == 0) level.playSound(null, worldPosition, com.theoreticalsciences.registry.QHSounds.POWER_UP.get(), net.minecraft.sounds.SoundSource.BLOCKS, 1.5F, 0.5F + t / 160F);
        if (t == MELTDOWN_TICKS - 20) level.playSound(null, worldPosition, com.theoreticalsciences.registry.QHSounds.CHARGE.get(), net.minecraft.sounds.SoundSource.BLOCKS, 4F, 0.6F);
        double r = 1.5 + t / 20.0;
        level.sendParticles(com.theoreticalsciences.registry.QHParticles.ARC_SPARK.get(), c.x, c.y, c.z, 4 + t / 6, r, r, r, 0.3);
        level.sendParticles(com.theoreticalsciences.registry.QHParticles.VAPOUR.get(), c.x, c.y, c.z, 1 + t / 20, r * 0.6, r * 0.6, r * 0.6, 0.02);
        if (--meltdown <= 0) containmentFailure(level);
    }

    /** Containment gave way: the work in progress is lost and the machine shuts down until recalibrated. */
    private void containmentFailure(ServerLevel level) {
        progress = 0;
        Vec3 c = effectCenter();
        level.sendParticles(com.theoreticalsciences.registry.QHParticles.BURST.get(), c.x, c.y, c.z, 3, 0.8, 0.8, 0.8, 0);
        level.sendParticles(com.theoreticalsciences.registry.QHParticles.PHASE_MOTE.get(), c.x, c.y, c.z, 120, 1.5, 1.5, 1.5, 0.2);
        level.playSound(null, worldPosition, com.theoreticalsciences.registry.QHSounds.POWER_DOWN.get(), net.minecraft.sounds.SoundSource.BLOCKS, 1.5F, 0.5F);
        com.theoreticalsciences.machine.tuning.TuningProfile tp = tuningProfile();
        MultiblockDefinition md = definition();
        // measured from the effect centre, which can sit off the middle of a long machine: use the whole length to be sure
        double reach = md == null ? 3 : Math.max(md.sizeX, md.sizeZ);
        if (tp != null) com.theoreticalsciences.machine.tuning.ContainmentHazards.release(level, c, tp, tuning.overclock, reach);
        if (owner != null) {
            ServerPlayer p = level.getServer().getPlayerList().getPlayer(owner);
            if (p != null) p.displayClientMessage(Component.translatable("message.theoreticalsciences.containment_failure",
                    Component.translatable("block.theoreticalsciences." + type.id()), worldPosition.getX(), worldPosition.getY(), worldPosition.getZ()).withStyle(ChatFormatting.RED), false);
        }
    }

    /** While the breach is open, anyone who steps into its heart falls through into Hyperdimensional Reality Space. */
    private void breach(ServerLevel level) {
        Vec3 c = effectCenter();
        if (level.getGameTime() % 4 == 0)
            level.sendParticles(com.theoreticalsciences.registry.QHParticles.PHOTON.get(), c.x, c.y, c.z, 3, 0.6, 0.6, 0.6, 0.08);
        if (level.dimension() == com.theoreticalsciences.world.QHDimensions.HYPERREAL) return;
        for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class, new AABB(c, c).inflate(1.3))) {
            if (p.isSpectator() || p.isOnPortalCooldown()) continue;
            p.setPortalCooldown();
            com.theoreticalsciences.world.HyperrealTravel.enter(p);
        }
    }

    private void setFormed(ServerLevel level, boolean value, List<BlockPos> newPorts) {
        Set<BlockPos> next = value ? new HashSet<>(newPorts) : Set.of();
        if (value == formed && next.equals(ports)) return;
        for (BlockPos p : ports) if (!next.contains(p)) PortBlock.setLinked(level, p, null);
        for (BlockPos p : next) PortBlock.setLinked(level, p, worldPosition);
        ports.clear();
        ports.addAll(next);
        boolean changed = value != formed;
        formed = value;
        markSync();
        setChanged();
        if (changed) {
            applyFormedLook(level, value);
            NeoForge.EVENT_BUS.post(new MultiblockFormedEvent(this, value));
            if (value) {
                Vec3 c = effectCenter();
                level.sendParticles(com.theoreticalsciences.registry.QHParticles.PHOTON.get(), c.x, c.y, c.z, 60, 1.2, 1.2, 1.2, 0.05);
                level.playSound(null, worldPosition, com.theoreticalsciences.registry.QHSounds.POWER_UP.get(), net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 0.6F);
            }
        }
    }

    /** Breaking the controller: hand back the upgrades installed in it (they live only in its data otherwise). */
    public void dropUpgrades() {
        if (level == null || level.isClientSide) return;
        double x = worldPosition.getX() + 0.5, y = worldPosition.getY() + 0.5, z = worldPosition.getZ() + 0.5;
        int st = tuning.stabiliser;
        if (st > 0 && st < com.theoreticalsciences.machine.tuning.StabiliserItem.IDS.length) {
            var item = com.theoreticalsciences.registry.QHItems.get(com.theoreticalsciences.machine.tuning.StabiliserItem.IDS[st]);
            if (item != null) net.minecraft.world.Containers.dropItemStack(level, x, y, z, new net.minecraft.world.item.ItemStack(item));
        }
        if (tuning.autoCalibrator) {
            var item = com.theoreticalsciences.registry.QHItems.get("autonomous_calibrator");
            if (item != null) net.minecraft.world.Containers.dropItemStack(level, x, y, z, new net.minecraft.world.item.ItemStack(item));
        }
        // so they can never be handed out twice
        tuning.stabiliser = 0;
        tuning.autoCalibrator = false;
    }

    public void onBroken() {
        if (level instanceof ServerLevel sl) {
            for (BlockPos p : ports) PortBlock.setLinked(sl, p, null);
            applyFormedLook(sl, false);
        }
        ports.clear();
    }

    /**
     * MMCR-style transformation: every casing / glass of the structure switches to its "formed" look (seamless
     * energised panels) when the structure completes, and back when it breaks. Only the blockstate property changes,
     * so the blocks themselves are never replaced. Flag 2 = send to clients without neighbour updates.
     */
    private void applyFormedLook(ServerLevel level, boolean value) {
        MultiblockDefinition def = definition();
        if (def == null) return;
        Direction f = facing();
        for (int y = 0; y < def.sizeY; y++) {
            List<String> rows = def.layers.get(y);
            for (int z = 0; z < rows.size(); z++) {
                String row = rows.get(z);
                for (int x = 0; x < row.length(); x++) {
                    char c = row.charAt(x);
                    if (c == ' ' || c == '.' || c == def.controllerChar) continue;
                    BlockPos p = def.toWorld(worldPosition, f, x, y, z);
                    if (!level.isLoaded(p)) continue;
                    BlockState st = level.getBlockState(p);
                    if (st.hasProperty(com.theoreticalsciences.block.CasingBlock.FORMED) && st.getValue(com.theoreticalsciences.block.CasingBlock.FORMED) != value)
                        level.setBlock(p, st.setValue(com.theoreticalsciences.block.CasingBlock.FORMED, value), 2);
                }
            }
        }
    }

    /** Active singularities tug on loose items and creatures nearby. */
    private void hazards(ServerLevel level) {
        if (type.effect != MachineType.Effect.BLACK_HOLE && type.effect != MachineType.Effect.BLACK_HOLE_DISK && type.effect != MachineType.Effect.EYE) return;
        int radius = QHConfig.SINGULARITY_PULL_RADIUS.get();
        if (radius <= 0 || level.getGameTime() % 2 != 0) return;
        Vec3 c = effectCenter();
        for (Entity e : level.getEntities((Entity) null, new AABB(c, c).inflate(radius), e -> e instanceof ItemEntity || e instanceof LivingEntity)) {
            if (e instanceof Player p && (p.isCreative() || p.isSpectator())) continue;
            Vec3 d = c.subtract(e.position().add(0, e.getBbHeight() / 2, 0));
            double dist = d.length();
            if (dist < 0.01 || dist > radius) continue;
            double pull = 0.04 * (1.0 - dist / radius) + 0.01;
            e.setDeltaMovement(e.getDeltaMovement().add(d.normalize().scale(pull)));
            e.hurtMarked = true;
            if (e instanceof LivingEntity le && dist < 1.6 && level.getGameTime() % 20 == 0)
                le.hurt(level.damageSources().magic(), 2.0F);
        }
    }


    // ------------------------------------------------------------------ the impossible recipe (Omnilex Engine only)
    private enum Confirm { NONE, PENDING, YES, NO }

    private Confirm confirm = Confirm.NONE;
    private int confirmResend;
    private boolean collapsing;
    /** Round 29: who the collapse question was actually put to - only they may answer it. (Not saved: asked again after a reload.) */
    private final java.util.Set<java.util.UUID> askedPlayers = new java.util.HashSet<>();

    /** Field 3 from the tuning panel: the hidden aleph-null overclock. Only someone carrying the Omnilex can set it. */
    public void toggleOmega(ServerPlayer sp) {
        if (type != MachineType.OMNILEX_ENGINE || !carriesOmnilex(sp)) return;
        tuning.omega = !tuning.omega;
        // round 35: say which way it went - the screen showed no sign of it before
        sp.displayClientMessage(Component.translatable(tuning.omega ? "message.theoreticalsciences.omega_on" : "message.theoreticalsciences.omega_off")
                .withStyle(tuning.omega ? ChatFormatting.DARK_RED : ChatFormatting.GRAY), true);
        confirm = Confirm.NONE;
        progress = 0;
        recipeDirty();
        markSync();
        setChanged();
    }

    public static boolean carriesOmnilex(net.minecraft.world.entity.player.Player p) {
        net.minecraft.world.item.Item om = com.theoreticalsciences.registry.QHItems.get("omnilex");
        for (net.minecraft.world.item.ItemStack s : p.getInventory().items) if (s.is(om)) return true;
        for (net.minecraft.world.item.ItemStack s : p.getInventory().offhand) if (s.is(om)) return true;
        return false;
    }

    public void answerCollapse(ServerPlayer sp, boolean yes) {
        if (type != MachineType.OMNILEX_ENGINE || confirm != Confirm.PENDING) return;
        // only someone the question was put to (the owner, or someone at its screen when asked) - or who has the
        // engine's screen open right now - may answer it; a passer-by cannot decide for them
        boolean viewing = sp.containerMenu instanceof MachineMenu m && m.be == this;
        if (!viewing && !askedPlayers.contains(sp.getUUID())) return;
        askedPlayers.clear();
        confirm = yes ? Confirm.YES : Confirm.NO;
        sp.displayClientMessage(Component.translatable(yes ? "message.theoreticalsciences.collapse_accepted" : "message.theoreticalsciences.collapse_declined")
                .withStyle(yes ? ChatFormatting.DARK_RED : ChatFormatting.GRAY), false);
        markSync();
    }

    @Override
    protected MachineStatus gate(ServerLevel level, com.theoreticalsciences.recipe.QHRecipe r) {
        if (hasPhysics()) {
            if (physics.recipe != r.physics) {
                physics.recipe = r.physics;
                evaluatePhysics(true);
            } else if (!isActive()) evaluatePhysics(true);    // about to start: check it as it would run
            MachineStatus ps = physicsGate(level, isActive());
            // still energised while only the window is missed: the operator is bringing it in
            if (ps == null || ps == MachineStatus.OUT_OF_WINDOW) energisedAt = level.getGameTime();
            if (ps != null) return ps;
        }
        if (type == MachineType.RESEARCH_NEXUS) return com.theoreticalsciences.research.ResearchNexus.gate(level, owner, r);
        // 1.3.6: the Eschaton Foundry repeats only what its owner has forged by hand in the Core's Forge
        if (type == MachineType.ESCHATON_FOUNDRY) {
            for (var o : r.itemOutputs)
                if (!com.theoreticalsciences.transcend.ForgeAttainment.has(level.getServer(), owner,
                        net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(o.getItem()).toString()))
                    return MachineStatus.NOT_YET_FORGED;
        }
        // it plays the Transcendental back - only while there is no Transcendental (round 36: and it says why it waits;
        // its other recipes - the Echo's harmonics - run whenever)
        if (rekindles(r)) {
            var ph = com.theoreticalsciences.transcend.Omnipotence.phase(level.getServer());
            switch (ph) {
                case INTACT -> { return MachineStatus.TRANSCENDENTAL_STANDS; }
                case SHUTDOWN -> { return MachineStatus.TRANSCENDENTAL_COLLAPSING; }
                case REBORN -> { return MachineStatus.ALREADY_REKINDLED; }
                default -> { }
            }
        }
        if (type != MachineType.OMNILEX_ENGINE) return super.gate(level, r);
        if (collapsing) return MachineStatus.BLOCKED;
        if (!r.secret) return tuning.omega ? MachineStatus.BLOCKED : null;     // at aleph-null nothing ordinary can run
        if (!tuning.omega) return MachineStatus.NEEDS_OMEGA;    // (say so: carry the Omnilex and set the Ω overclock)
        // the impossible recipe opens the Transcendental: not while it is being, or has been, destroyed
        var ph = com.theoreticalsciences.transcend.Omnipotence.phase(level.getServer());
        if (ph == com.theoreticalsciences.transcend.Omnipotence.Phase.SHUTDOWN || ph == com.theoreticalsciences.transcend.Omnipotence.Phase.SUNDERED) return MachineStatus.BLOCKED;
        switch (confirm) {
            case YES -> {
                return null;
            }
            case NO -> {
                return MachineStatus.DECLINED;
            }
            default -> {
                if (confirm == Confirm.NONE || --confirmResend <= 0) {
                    confirm = Confirm.PENDING;
                    confirmResend = 600;
                    askForConfirmation(level);
                }
                return MachineStatus.AWAITING_CONFIRMATION;
            }
        }
    }

    /** Ask the owner (if online) and anyone looking at the engine's screen. */
    private void askForConfirmation(ServerLevel level) {
        java.util.Set<ServerPlayer> ask = new java.util.HashSet<>();
        if (owner != null) {
            ServerPlayer p = level.getServer().getPlayerList().getPlayer(owner);
            if (p != null) ask.add(p);
        }
        for (ServerPlayer p : level.players())
            if (p.containerMenu instanceof MachineMenu m && m.be == this) ask.add(p);
        for (ServerPlayer p : ask) {
            askedPlayers.add(p.getUUID());
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(p, new com.theoreticalsciences.network.CollapseConfirmPayload(worldPosition));
        }
    }

    /** The recipe completed: the engine folds into a five-dimensional object that drags everything near it into the Transcendental. */
    private void collapse(ServerLevel level) {
        if (collapsing) return;
        collapsing = true;
        confirm = Confirm.NONE;
        tuning.omega = false;
        MultiblockDefinition def = definition();
        List<BlockPos> parts = new ArrayList<>(def == null ? List.of() : def.solidPositions(worldPosition, facing()));
        parts.add(worldPosition);
        for (BlockPos p : ports) PortBlock.setLinked(level, p, null);
        ports.clear();
        Vec3 c = effectCenter();
        for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class, new AABB(c, c).inflate(96)))
            p.displayClientMessage(Component.translatable("message.theoreticalsciences.collapse_begins").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD), false);
        com.theoreticalsciences.transcend.HyperobjectEntity.spawn(level, c, parts);
    }

    // ------------------------------------------------------------------ Vacuum Fluctuation Harvester: output decided by tuning vs. position
    /** Which virtual particle this harvester is tuned to here, and how close it is (0..1). */
    public int harvestTarget(ServerLevel level, double[] closeness) {
        double[] d = new double[1];
        int m = com.theoreticalsciences.foam.FoamField.nearest(level, worldPosition, tuning.setting, d);
        if (closeness != null) closeness[0] = Math.max(0, 1 - d[0] / 12.0);
        return m;
    }

    @Override
    protected void rollChances(ServerLevel level, com.theoreticalsciences.recipe.QHRecipe r) {
        if (type != MachineType.VACUUM_HARVESTER) {
            super.rollChances(level, r);
            return;
        }
        double[] k = new double[1];
        int m = harvestTarget(level, k);
        rolled = new boolean[r.itemOutputs.size()];
        net.minecraft.world.item.Item want = com.theoreticalsciences.foam.FoamField.item(m);
        for (int i = 0; i < rolled.length; i++)
            rolled[i] = r.itemOutputs.get(i).is(want) && level.random.nextDouble() < 0.1 + 0.9 * k[0];   // off-tune: mostly nothing condenses
    }

    /** The Omnilex Engine and the Pleroma Reconstitutor do one thing to the world when they finish: never in parallel. */
    @Override
    protected boolean singleRunOnly(com.theoreticalsciences.recipe.QHRecipe r) {
        return super.singleRunOnly(r) || type == MachineType.OMNILEX_ENGINE || rekindles(r);
    }

    /** The Reconstitutor's recipe that makes the Rekindled Spark is the one that rebuilds the Transcendental. */
    private boolean rekindles(com.theoreticalsciences.recipe.QHRecipe r) {
        if (type != MachineType.PLEROMA_RECONSTITUTOR) return false;
        for (var o : r.itemOutputs)
            if (net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(o.getItem()).equals(com.theoreticalsciences.TheoreticalSciences.id("rekindled_spark"))) return true;
        return false;
    }

    @Override
    protected void onRecipeFinished(ServerLevel level, com.theoreticalsciences.recipe.QHRecipe recipe, int runs) {
        // the Research Nexus: every dossier fed in counts, so parallel runs speed the research up that many times
        if (type == MachineType.RESEARCH_NEXUS) {
            com.theoreticalsciences.research.ResearchNexus.apply(level, owner, recipe, runs);
            return;
        }
        onRecipeFinished(level, recipe);
    }

    @Override
    protected void onRecipeFinished(ServerLevel level, com.theoreticalsciences.recipe.QHRecipe recipe) {
        if (type.generator && hasPhysics()) physics.recipe = recipe.physics;   // generators: the burning fuel's window
        if (recipe.secret && type == MachineType.OMNILEX_ENGINE) {
            collapse(level);
            return;
        }
        if (type == MachineType.RESEARCH_NEXUS) {
            com.theoreticalsciences.research.ResearchNexus.apply(level, owner, recipe);
            return;
        }
        if (rekindles(recipe)) {
            com.theoreticalsciences.transcend.Omnipotence.rekindle(level.getServer());
            com.theoreticalsciences.world.transit.RekindleCinematic.play(level, effectCenter());   // round 27: the finale cinematic
            return;
        }
        // (Insight from thinking machines - the Matrioshka Brain and every "thought experiment" - is now a recipe field: insight_per_run)
    }

    // ------------------------------------------------------------------ 1.1.0: Interface Ports that import and export
    @Override
    protected void tickServer(ServerLevel level) {
        super.tickServer(level);
        if (isFormed() && !ports.isEmpty() && Math.floorMod(level.getGameTime() + worldPosition.hashCode(), 10L) == 0) portIO(level);
    }

    /**
     * Every half second each port set to export pushes the machine's products (output slots, output tanks, spent
     * coolant) into the inventories and tanks beside it, and each port set to import pulls what this machine's recipes
     * can use (ingredients, catalysts, input fluids, coolant) from them. Never into or out of the machine's own blocks.
     */
    private void portIO(ServerLevel level) {
        MEWanted wanted = null;
        for (BlockPos p : ports) {
            if (!level.isLoaded(p) || !(level.getBlockEntity(p) instanceof PortBlockEntity pe) || pe.ioMode() == 0) continue;
            int mode = pe.ioMode();
            for (net.minecraft.core.Direction d : net.minecraft.core.Direction.values()) {
                BlockPos t = p.relative(d);
                if (t.equals(worldPosition) || ports.contains(t) || !level.isLoaded(t)) continue;
                var inv = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, t, d.getOpposite());
                var tank = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, t, d.getOpposite());
                if (inv == null && tank == null) continue;
                if ((mode & 1) != 0) {
                    if (inv != null) exportItems(inv);
                    if (tank != null) exportFluids(tank);
                }
                if ((mode & 2) != 0) {
                    if (wanted == null) wanted = MEWanted.of(level, type.recipeType);
                    if (inv != null) importItems(inv, wanted);
                    if (tank != null) importFluids(tank);
                }
            }
        }
    }

    private void exportItems(net.neoforged.neoforge.items.IItemHandler inv) {
        for (int slot = type.itemInputs; slot < itemCap.getSlots(); slot++) {
            net.minecraft.world.item.ItemStack s = itemCap.extractItem(slot, 64, true);
            if (s.isEmpty()) continue;
            int n = s.getCount() - net.neoforged.neoforge.items.ItemHandlerHelper.insertItemStacked(inv, s, true).getCount();
            if (n <= 0) continue;
            net.minecraft.world.item.ItemStack got = itemCap.extractItem(slot, n, false);
            net.minecraft.world.item.ItemStack left = net.neoforged.neoforge.items.ItemHandlerHelper.insertItemStacked(inv, got, false);
            if (!left.isEmpty()) items.insertItem(slot, left, false);         // (the neighbour changed its mind: back it goes)
        }
    }

    private void importItems(net.neoforged.neoforge.items.IItemHandler inv, MEWanted wanted) {
        int moved = 0;
        for (int slot = 0; slot < inv.getSlots() && moved < 4; slot++) {
            net.minecraft.world.item.ItemStack s = inv.extractItem(slot, 64, true);
            if (s.isEmpty() || !wanted.wantsItem(s)) continue;
            int n = s.getCount() - net.neoforged.neoforge.items.ItemHandlerHelper.insertItem(itemCap, s, true).getCount();
            if (n <= 0) continue;
            net.minecraft.world.item.ItemStack got = inv.extractItem(slot, n, false);
            net.minecraft.world.item.ItemStack left = net.neoforged.neoforge.items.ItemHandlerHelper.insertItem(itemCap, got, false);
            if (!left.isEmpty()) net.neoforged.neoforge.items.ItemHandlerHelper.insertItem(inv, left, false);
            moved++;
        }
    }

    private void exportFluids(net.neoforged.neoforge.fluids.capability.IFluidHandler tank) {
        for (int i = 0; i < fluidCap.getTanks(); i++) {
            if (fluidCap.isInputTank(i)) continue;
            var ft = fluidCap.tank(i);
            if (ft == null || ft.isEmpty()) continue;
            var s = ft.drain(16000, net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE);
            int n = tank.fill(s, net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE);
            if (n <= 0) continue;
            tank.fill(ft.drain(n, net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE),
                    net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
        }
    }

    private void importFluids(net.neoforged.neoforge.fluids.capability.IFluidHandler tank) {
        for (int i = 0; i < tank.getTanks(); i++) {
            var f = tank.getFluidInTank(i);
            if (f.isEmpty()) continue;
            var want = f.copyWithAmount(Math.min(f.getAmount(), 16000));
            int n = fluidCap.fill(want, net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE);
            if (n <= 0) continue;
            var got = tank.drain(want.copyWithAmount(n), net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
            if (!got.isEmpty()) fluidCap.fill(got, net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
        }
    }

    @Override
    protected List<BlockPos> energyOutputPositions() {
        List<BlockPos> list = new ArrayList<>();
        list.add(worldPosition);
        list.addAll(ports);
        return list;
    }

    @Override
    protected boolean isOwnPart(BlockPos pos) {
        return ports.contains(pos);
    }

    public void reportStructure(ServerPlayer player) {
        if (level instanceof ServerLevel l0) pickSize(l0);
        MultiblockDefinition def = definition();
        if (def == null || !(level instanceof ServerLevel sl)) return;
        MultiblockDefinition.ScanResult scan = def.scan(sl, worldPosition, facing(), portId());
        setFormed(sl, scan.complete(), scan.ports());
        updateTiers(scan);
        if (scan.complete()) {
            player.displayClientMessage(Component.translatable("message.theoreticalsciences.structure_complete", ports.size()).withStyle(ChatFormatting.AQUA), false);
            MultiblockDefinition base = Multiblocks.get(type.multiblock).orElse(null);
            if (base != null && base.variable())
                player.displayClientMessage(Component.translatable("message.theoreticalsciences.structure_units", units(), base.units(base.repeatMax))
                        .withStyle(ChatFormatting.AQUA), false);
            if (tuning.stabiliser > 0)
                player.displayClientMessage(Component.translatable("message.theoreticalsciences.stabiliser_report", Component.translatable("item.theoreticalsciences."
                        + com.theoreticalsciences.machine.tuning.StabiliserItem.IDS[Math.min(4, tuning.stabiliser)])).withStyle(ChatFormatting.DARK_AQUA), false);
            if (coilTier > 0 || emitterTier > 0)
                player.displayClientMessage(Component.translatable("message.theoreticalsciences.structure_tiers", coilTier, emitterTier).withStyle(ChatFormatting.GOLD), false);
            // round 31: what the coil tier means - its speed, and (machines with physics) its superconductor's limits
            if (coilTier > 0) {
                String speed = String.format(java.util.Locale.ROOT, "%.2f", coilSpeed());
                player.displayClientMessage((hasPhysics()
                        ? Component.translatable("message.theoreticalsciences.structure_coil_physics", coilTier, speed,
                        fmtSig(com.theoreticalsciences.machine.physics.PhysicsModel.scTc(coilTier)), fmtSig(com.theoreticalsciences.machine.physics.PhysicsModel.scB0(coilTier)))
                        : Component.translatable("message.theoreticalsciences.structure_coil_speed", coilTier, speed)).withStyle(ChatFormatting.YELLOW), false);
            }
            return;
        }
        player.displayClientMessage(Component.translatable("message.theoreticalsciences.structure_missing", scan.missing().size()).withStyle(ChatFormatting.RED), false);
        int shown = 0;
        for (BlockPos p : scan.missing()) {
            char c = scan.expected().get(p);
            Component what;
            if (c == '.') what = Component.translatable("message.theoreticalsciences.air");
            else {
                List<Block> bs = def.blocksFor(c);
                what = bs.isEmpty() ? Component.literal("?") : bs.get(0).getName();
                if (def.tiers.containsKey(c)) what = Component.translatable("message.theoreticalsciences.any_tier", what);
            }
            if (shown++ < 5)
                player.displayClientMessage(Component.literal("  " + p.getX() + ", " + p.getY() + ", " + p.getZ() + " → ").append(what).withStyle(ChatFormatting.GRAY), false);
            sl.sendParticles(player, com.theoreticalsciences.registry.QHParticles.ARC_SPARK.get(), true, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 8, 0.25, 0.25, 0.25, 0.0);
        }
    }

    private static String fmtSig(double v) {
        return Math.abs(v) >= 1e6 || (v != 0 && Math.abs(v) < 0.01) ? String.format(java.util.Locale.ROOT, "%.3g", v) : String.format(java.util.Locale.ROOT, "%,.1f", v);
    }

    @Override
    protected void writeShared(CompoundTag tag, HolderLookup.Provider regs) {
        super.writeShared(tag, regs);
        tag.putBoolean("Formed", formed);
        tag.put("Tuning", tuning.save());
        tag.put("Physics", physics.save(regs));
        tag.putInt("Confirm", confirm.ordinal());
        tag.putInt("CoilTier", coilTier);
        tag.putInt("EmitterTier", emitterTier);
        tag.putInt("ExtraUnits", extraUnits);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider regs) {
        super.saveAdditional(tag, regs);
        net.minecraft.nbt.ListTag pl = new net.minecraft.nbt.ListTag();
        for (BlockPos p : ports) pl.add(LongTag.valueOf(p.asLong()));
        tag.put("Ports", pl);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider regs) {
        super.loadAdditional(tag, regs);
        formed = tag.getBoolean("Formed");
        if (tag.contains("Tuning")) tuning.load(tag.getCompound("Tuning"));
        if (tag.contains("Physics")) physics.load(tag.getCompound("Physics"), regs);
        int cf = tag.getInt("Confirm");
        confirm = cf >= 0 && cf < Confirm.values().length ? Confirm.values()[cf] : Confirm.NONE;
        if (confirm == Confirm.PENDING) confirmResend = 0;
        coilTier = tag.getInt("CoilTier");
        emitterTier = tag.getInt("EmitterTier");
        extraUnits = tag.getInt("ExtraUnits");
        if (tag.contains("Ports")) {
            ports.clear();
            for (Tag t : tag.getList("Ports", Tag.TAG_LONG)) ports.add(BlockPos.of(((LongTag) t).getAsLong()));
        }
    }

    public static Block portBlock() {
        return QHBlocks.INTERFACE_PORT.get();
    }
}
