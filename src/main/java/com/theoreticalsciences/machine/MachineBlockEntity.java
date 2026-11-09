package com.theoreticalsciences.machine;

import com.theoreticalsciences.QHConfig;
import com.theoreticalsciences.api.event.MachineRecipeEvent;
import com.theoreticalsciences.recipe.QHRecipe;
import com.theoreticalsciences.recipe.QHRecipeInput;
import com.theoreticalsciences.recipe.RecipeMatcher;
import com.theoreticalsciences.registry.QHBlockEntities;
import com.theoreticalsciences.registry.QHRecipes;
import com.theoreticalsciences.research.ResearchService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.ItemStackHandler;
import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Recipe-driven engine shared by every machine and multiblock controller.
 * Slots: [item inputs..., item outputs...]; tanks: [fluid inputs..., fluid outputs...].
 */
public class MachineBlockEntity extends BlockEntity implements MenuProvider {
    public final MachineType type;
    public final BigItemStackHandler items;
    /**
     * 1.2.0: the catalyst slot - one catalyst at a time (anything in the theoreticalsciences:catalysts tag). A catalyst
     * that wears only works from here, so it is always this one that wears. Filled by hand; pipes and ME do not reach it.
     */
    public final ItemStackHandler catalyst = new ItemStackHandler(1) {
        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return com.theoreticalsciences.science.Wear.isCatalyst(stack);
        }

        @Override
        protected void onContentsChanged(int slot) {
            onInventoryChanged();
        }
    };
    public final FluidTank[] tanks;
    public final QHEnergyStorage energy;
    public final MachineItemHandler itemCap;
    public final MachineFluidHandler fluidCap;

    protected int progress;
    protected int maxProgress;
    protected int burnTime;      // generators: ticks of output remaining
    protected int burnRate;      // generators: FE/t while burning
    protected MachineStatus status = MachineStatus.IDLE;
    @Nullable protected ResourceLocation currentRecipe;
    @Nullable protected UUID owner;
    protected String ownerName = "";

    private boolean recipeDirty = true;
    private boolean syncDirty = true;
    /** 1.2.0: players with this machine's screen open (server side), so idle readouts go out often only while watched. */
    public int viewers;
    private int idleTimer;
    private int syncTimer;
    private boolean lastActive;

    public MachineBlockEntity(MachineType type, BlockPos pos, BlockState state) {
        super(QHBlockEntities.machine(type), pos, state);
        this.type = type;
        this.items = new BigItemStackHandler(type.itemSlots(), type.itemInputs) {
            @Override
            protected void onContentsChanged(int slot) {
                onInventoryChanged();
            }
        };
        this.tanks = new FluidTank[type.tanks()];
        for (int i = 0; i < tanks.length; i++) {
            tanks[i] = new FluidTank(type.tankCapacity()) {
                @Override
                protected void onContentsChanged() {
                    onInventoryChanged();
                }
            };
        }
        // intake is limited only by the buffer; generators push out as fast as anything will take it
        // (round 31: a datapack may change the buffer size and the intake limit - data/<ns>/ts_machines/<machine>.json)
        this.energy = new QHEnergyStorage(com.theoreticalsciences.custom.MachineParams.energyCapacity(type), type.generator ? 0 : Integer.MAX_VALUE, type.generator ? Integer.MAX_VALUE : 0, () -> {
            markSync();
            // 1.2.0: the stored energy is saved too (a reload lost a consumer's charge, gave a generator back what it sent);
            // only the chunk is marked for saving, not setChanged's neighbour updates, as this runs every tick
            if (level != null && !level.isClientSide) level.blockEntityChanged(worldPosition);
        });
        applyEnergyLimits();
        this.itemCap = new MachineItemHandler(this);
        this.fluidCap = new MachineFluidHandler(this);
    }

    // ------------------------------------------------------------------ round 31: numbers a datapack can change
    /** Which version of the datapack numbers this machine last applied (-1: none yet). */
    private int paramsGen = -1;

    /** The pack's numbers for this machine (all 1x / unset without a ts_machines file). */
    protected com.theoreticalsciences.custom.MachineParams.Params params() {
        return com.theoreticalsciences.custom.MachineParams.get(type);
    }

    private void applyEnergyLimits() {
        var p = params();
        long in = p.maxInput() != null ? p.maxInput() : Integer.MAX_VALUE;
        energy.setLimits(type.generator ? 0 : in, type.generator ? Integer.MAX_VALUE : 0);
    }

    /**
     * After a /reload (or the first tick): the buffer takes the pack's size. Lowering it never throws energy away - the
     * buffer only shrinks as far as what it holds - and a recipe that needs more still grows it (see ensureBuffer).
     */
    private void applyParams() {
        paramsGen = com.theoreticalsciences.custom.PackData.generation();
        long cap = com.theoreticalsciences.custom.MachineParams.energyCapacity(type);
        long want = Math.max(cap, energy.stored());
        if (energy.capacity() != want && (energy.capacity() < cap || energy.capacity() > want)) {
            energy.setCapacity(want);
            markSync();
        }
        applyEnergyLimits();
    }

    public static MachineBlockEntity create(MachineType type, BlockPos pos, BlockState state) {
        if (type == MachineType.DYSON_ARRAY) return new DysonControllerBlockEntity(type, pos, state);
        if (ScienceMachineBlockEntity.handles(type)) return new ScienceMachineBlockEntity(type, pos, state);
        return type.isMultiblock() || type.standaloneConsole() ? new ControllerBlockEntity(type, pos, state) : new MachineBlockEntity(type, pos, state);
    }

    /** FE/t actually produced while burning (Dyson Array scales this with its swarm). */
    public int effectiveBurnRate() {
        return burnRate;
    }

    // ------------------------------------------------------------------ state helpers

    protected void recipeDirty() {
        recipeDirty = true;
    }

    protected void onInventoryChanged() {
        recipeDirty = true;
        markSync();
        setChanged();
    }

    protected void markSync() {
        syncDirty = true;
    }

    public boolean isActive() {
        return status == MachineStatus.RUNNING || status == MachineStatus.GENERATING;
    }

    public MachineStatus getStatus() {
        return status;
    }

    public int getProgress() {
        return progress;
    }

    public int getMaxProgress() {
        return maxProgress;
    }

    /** For Jade / the console: work per tick, parallel recipes and the recipe in hand. */
    public double shownSpeed() {
        double sp = speedFactor();
        return Double.isFinite(sp) && sp > 0 ? sp : 0;
    }

    public int shownParallel() {
        return Math.max(1, parallelCount());
    }

    public int shownOverclock() {
        return 0;
    }

    @Nullable
    public ResourceLocation shownRecipe() {
        return currentRecipe;
    }

    public int getBurnTime() {
        return burnTime;
    }

    public int getBurnRate() {
        return burnRate;
    }

    @Nullable
    public UUID getOwner() {
        return owner;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwner(Player player) {
        this.owner = player.getUUID();
        this.ownerName = player.getGameProfile().getName();
        setChanged();
    }

    /** Multiblock controllers override this to require a complete structure. */
    public boolean canOperate() {
        return !wrongDimension() && placementOk();
    }

    /** 1.2.0: the Cloud Siphon draws only from a nebula cloud block it touches, the Lake Pump only from a Titan lake. */
    static final java.util.List<String> CLOUDS = java.util.List.of("hydrogen_cloud", "oxygen_cloud", "dust_cloud");
    static final java.util.List<String> LAKES = java.util.List.of("liquid_methane", "cryolava", "blood_plasma");

    /** The blocks this machine draws on (empty for machines that draw on nothing round them). */
    static java.util.List<String> sourcesFor(MachineType t) {
        return t == MachineType.CLOUD_SIPHON ? CLOUDS : t == MachineType.LAKE_PUMP ? LAKES : java.util.List.of();
    }

    protected java.util.List<String> touchingClouds() {
        java.util.List<String> out = new java.util.ArrayList<>();
        java.util.List<String> want = sourcesFor(type);
        if (level == null || want.isEmpty()) return out;
        for (net.minecraft.core.Direction d : net.minecraft.core.Direction.values()) {
            var id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(level.getBlockState(worldPosition.relative(d)).getBlock());
            if (com.theoreticalsciences.TheoreticalSciences.MODID.equals(id.getNamespace()) && want.contains(id.getPath()) && !out.contains(id.getPath()))
                out.add(id.getPath());
        }
        return out;
    }

    public boolean placementOk() {
        return sourcesFor(type).isEmpty() || !touchingClouds().isEmpty();
    }

    /** A recipe this machine may run where it stands (a siphon's or pump's recipes are each named after what they draw on). */
    protected boolean recipeAllowed(net.minecraft.resources.ResourceLocation id) {
        if (sourcesFor(type).isEmpty()) return true;
        for (String c : touchingClouds()) if (id.getPath().endsWith(c)) return true;
        return false;
    }

    /** Round 35: built outside the dimensions this machine works in (MachineType.allowedDimensions; a pack may set any). */
    public boolean wrongDimension() {
        return !MachineType.allowedIn(type, level);
    }

    /** What the recipe may draw on: the input slots, then anything else (an ME network behind an Interface Port). */
    public List<ItemStack> inputStacks() {
        List<ItemStack> list = new ArrayList<>(type.itemInputs);
        for (int i = 0; i < type.itemInputs; i++) list.add(items.getStackInSlot(i));
        list.addAll(extraItems());
        return list;
    }

    public List<FluidStack> inputFluids() {
        List<FluidStack> list = new ArrayList<>(type.fluidInputs);
        for (int i = 0; i < type.fluidInputs; i++) list.add(tanks[i].getFluid());
        list.addAll(extraFluids());
        return list;
    }

    /** Copies of stacks held outside the machine that recipes may use (see {@link #takeExtra}). */
    protected List<ItemStack> extraItems() {
        return List.of();
    }

    protected List<FluidStack> extraFluids() {
        return List.of();
    }

    /** Takes exactly these from outside the machine, all or nothing. */
    protected boolean takeExtra(List<ItemStack> items, List<FluidStack> fluids) {
        return false;
    }

    // ------------------------------------------------------------------ ticking

    public static void serverTick(Level level, BlockPos pos, BlockState state, MachineBlockEntity be) {
        be.tickServer((ServerLevel) level);
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, MachineBlockEntity be) {
        be.tickClient();
    }

    protected void tickClient() {
        if (isActive()) {
            com.theoreticalsciences.client.ClientHooks.machineParticles(this);
            com.theoreticalsciences.client.ClientHooks.machineWork(this);       // 1.5.1: running multiblocks are heard
        }
    }

    protected void tickServer(ServerLevel level) {
        if (paramsGen != com.theoreticalsciences.custom.PackData.generation()) applyParams();
        preTick(level);
        if (!canOperate()) {
            setStatus(blockedStatus());
            progress = 0;
        } else if (type.generator) {
            tickGenerator(level);
        } else {
            tickProcessor(level);
        }
        boolean active = isActive();
        // 1.7.0: Maxwell's demon has to forget what it sorted - its erasures spill into whoever stands by it (Landauer)
        if (active && type == MachineType.MAXWELL_DEMON_SORTER && level.getGameTime() % 40 == 0)
            for (net.minecraft.server.level.ServerPlayer p : level.getEntitiesOfClass(net.minecraft.server.level.ServerPlayer.class,
                    new net.minecraft.world.phys.AABB(worldPosition).inflate(4)))
                if (!p.isSpectator() && !p.isCreative())
                    p.addEffect(new net.minecraft.world.effect.MobEffectInstance(com.theoreticalsciences.registry.QHEffects.INFORMATION_LOSS, 60, 0, false, true));
        if (active != lastActive) {
            lastActive = active;
            BlockState st = getBlockState();
            if (st.hasProperty(MachineBlock.ACTIVE) && st.getValue(MachineBlock.ACTIVE) != active)
                level.setBlock(worldPosition, st.setValue(MachineBlock.ACTIVE, active), 3);
            markSync();
        }
        if (++syncTimer >= 10) {
            syncTimer = 0;
            if (syncDirty) {
                syncDirty = false;
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
            }
        }
    }

    /** Hook for controllers (structure checks, ambient effects). */
    protected void preTick(ServerLevel level) {}

    /** Status shown while {@link #canOperate()} is false. */
    protected MachineStatus blockedStatus() {
        return wrongDimension() ? MachineStatus.WRONG_DIMENSION : !placementOk() ? MachineStatus.WRONG_ENVIRONMENT : MachineStatus.INCOMPLETE;
    }

    // ---- tuning hooks (multiblock controllers override these; plain machines run at 1x)
    /** Progress gained per tick. */
    protected double speedFactor() {
        return 1.0;
    }

    /** Multiplier on FE/t drawn. */
    protected double energyFactor() {
        return 1.0;
    }

    /** Recipes processed per cycle. */
    protected int parallelCount() {
        return 1;
    }

    /** Whether this machine's components are good enough for the recipe (multiblock coil tiers). */
    protected boolean tierOk(QHRecipe r) {
        return r.minTier <= 0;
    }

    /** Extra conditions before a recipe may start (null = go). Plain machines refuse impossible (secret) recipes outright. */
    @Nullable
    protected MachineStatus gate(ServerLevel level, QHRecipe r) {
        return r.secret ? MachineStatus.BLOCKED : null;
    }

    /** Multiplier on generator output. */
    protected double outputFactor() {
        return 1.0;
    }

    private double workCarry;

    protected void setStatus(MachineStatus s) {
        if (status != s) {
            status = s;
            markSync();
        }
    }

    /** The recipe the inputs match right now, or the one it is running / last ran (for tools; no caching side effects). */
    @Nullable
    public RecipeHolder<QHRecipe> matchingRecipe(ServerLevel level) {
        QHRecipeInput input = new QHRecipeInput(inputStacks(), inputFluids(), catalyst.getStackInSlot(0));
        RecipeHolder<QHRecipe> best = null;
        int bestScore = -1;
        for (RecipeHolder<QHRecipe> h : recipesFor(level)) {
            QHRecipe r = h.value();
            if (!recipeAllowed(h.id()) || !r.matches(input, level)) continue;
            int score = (r.itemInputs.size() + r.catalysts.size()) * 2 + r.fluidInputs.size();
            if (score > bestScore) {
                best = h;
                bestScore = score;
            }
        }
        if (best == null && currentRecipe != null) {
            var cached = byKey(level, currentRecipe);
            if (cached.isPresent() && cached.get().value() instanceof QHRecipe r && r.typeId().equals(type.recipeType)) {
                @SuppressWarnings("unchecked") RecipeHolder<QHRecipe> h = (RecipeHolder<QHRecipe>) cached.get();
                best = h;
            }
        }
        return best;
    }

    /** This machine's recipes: the datapack's, and (the Essence Distillery) the ones made from the fluid registry. */
    protected java.util.List<RecipeHolder<QHRecipe>> recipesFor(ServerLevel level) {
        java.util.List<RecipeHolder<QHRecipe>> l = level.getRecipeManager().getAllRecipesFor(QHRecipes.type(type.recipeType));
        if (type != MachineType.ESSENCE_DISTILLERY) return l;
        java.util.List<RecipeHolder<QHRecipe>> all = new java.util.ArrayList<>(l);
        all.addAll(com.theoreticalsciences.transcend.Essences.recipes());
        return all;
    }

    protected static Optional<RecipeHolder<?>> byKey(ServerLevel level, net.minecraft.resources.ResourceLocation id) {
        Optional<RecipeHolder<?>> r = level.getRecipeManager().byKey(id);
        if (r.isPresent()) return r;
        return Optional.ofNullable(com.theoreticalsciences.transcend.Essences.byId(id));
    }

    @Nullable
    protected RecipeHolder<QHRecipe> findRecipe(ServerLevel level) {
        QHRecipeInput input = new QHRecipeInput(inputStacks(), inputFluids(), catalyst.getStackInSlot(0));
        if (currentRecipe != null) {
            Optional<RecipeHolder<?>> cached = byKey(level, currentRecipe);
            if (cached.isPresent() && cached.get().value() instanceof QHRecipe r && r.typeId().equals(type.recipeType) && recipeAllowed(currentRecipe) && r.matches(input, level)) {
                @SuppressWarnings("unchecked") RecipeHolder<QHRecipe> h = (RecipeHolder<QHRecipe>) cached.get();
                return h;
            }
        }
        if (!recipeDirty && ++idleTimer < 40) return null;
        idleTimer = 0;
        recipeDirty = false;
        // Prefer the most specific match (most ingredients) so catalyst-free recipes never shadow richer ones.
        RecipeHolder<QHRecipe> best = null;
        int bestScore = -1;
        for (RecipeHolder<QHRecipe> h : recipesFor(level)) {
            QHRecipe r = h.value();
            if (!recipeAllowed(h.id()) || !r.matches(input, level)) continue;
            int score = (r.itemInputs.size() + r.catalysts.size()) * 2 + r.fluidInputs.size();
            if (score > bestScore) {
                best = h;
                bestScore = score;
            }
        }
        return best;
    }

    private long scaledEnergyL(QHRecipe r) {
        return Math.round(r.energy * (type.generator ? QHConfig.GENERATOR_OUTPUT_MULTIPLIER.get() : QHConfig.ENERGY_COST_MULTIPLIER.get()) * params().energy());
    }

    /**
     * Energy intake used to be a fixed per-tick limit per machine type, and some recipes needed more per tick than
     * that limit (or more than the whole buffer), so they could never run. Now the buffer grows to hold at least
     * 20 seconds of whatever the current recipe draws per tick, and intake is limited only by the buffer itself.
     */
    protected void ensureBuffer(long perTick) {
        long want = Math.max(com.theoreticalsciences.custom.MachineParams.energyCapacity(type), perTick > Long.MAX_VALUE / 800 ? Long.MAX_VALUE / 2 : perTick * 400L);
        if (energy.capacity() < want) energy.setCapacity(want);
    }

    private int scaledEnergy(QHRecipe r) {
        return (int) Math.min(Integer.MAX_VALUE, Math.round(r.energy * (type.generator ? QHConfig.GENERATOR_OUTPUT_MULTIPLIER.get() : QHConfig.ENERGY_COST_MULTIPLIER.get()) * params().energy()));
    }

    /** The recipe's time, after the config's multiplier and a pack's speed_multiplier (2 = twice as fast). */
    private int scaledDuration(QHRecipe r) {
        return Math.max(1, (int) Math.min(Integer.MAX_VALUE, Math.round(r.duration * QHConfig.DURATION_MULTIPLIER.get() / params().speed())));
    }

    private boolean researchOk(QHRecipe r) {
        if (!r.hasResearch() || !QHConfig.RESEARCH_REQUIRED.get()) return true;
        ResourceLocation id = ResourceLocation.tryParse(r.research);
        return id == null || (owner != null && level instanceof ServerLevel sl && ResearchService.has(sl.getServer(), owner, id));
    }

    protected void tickProcessor(ServerLevel level) {
        RecipeHolder<QHRecipe> holder = findRecipe(level);
        if (holder == null) {
            if (currentRecipe != null || progress > 0) {
                currentRecipe = null;
                progress = 0;
            }
            setStatus(MachineStatus.IDLE);
            return;
        }
        QHRecipe r = holder.value();
        if (!holder.id().equals(currentRecipe)) {
            currentRecipe = holder.id();
            progress = 0;
        }
        if (!researchOk(r)) {
            setStatus(MachineStatus.NEEDS_RESEARCH);
            return;
        }
        if (!tierOk(r)) {
            setStatus(MachineStatus.LOW_TIER);
            return;
        }
        if (!r.environment.isAny() && !r.environment.test(level, worldPosition)) {
            setStatus(MachineStatus.WRONG_ENVIRONMENT);
            return;
        }
        MachineStatus gated = gate(level, r);
        if (gated != null) {
            setStatus(gated);
            return;
        }
        if (!canOutput(r, true)) {
            setStatus(MachineStatus.OUTPUT_FULL);
            return;
        }
        long cost = Math.max(1L, (long) Math.min(Long.MAX_VALUE / 4, (double) scaledEnergyL(r) * energyFactor()));
        ensureBuffer(cost);
        if (energy.stored() < cost) {
            setStatus(MachineStatus.NO_POWER);
            return;
        }
        if (progress == 0 && NeoForge.EVENT_BUS.post(new MachineRecipeEvent.Start(this, holder.id(), r)).isCanceled()) {
            setStatus(MachineStatus.BLOCKED);
            return;
        }
        energy.consume(cost);
        maxProgress = scaledDuration(r);
        // round 35: a pack can push the speed very high (speed_multiplier, overclock, coil bonus, a physics rate): never
        // more than one finished cycle per tick, and no overflow of the progress counter. A broken physics model can
        // also give NaN or a negative speed: that simply makes no progress (instead of poisoning workCarry forever).
        double sp = speedFactor();
        if (!Double.isFinite(sp) || sp < 0) sp = 0;
        if (!Double.isFinite(workCarry) || workCarry < 0) workCarry = 0;
        workCarry = Math.min(workCarry + sp, (double) Math.max(1, maxProgress) + 1);
        int steps = (int) Math.min(workCarry, Math.max(0, maxProgress - progress) + 1L);
        workCarry -= steps;
        progress = (int) Math.min((long) progress + steps, Integer.MAX_VALUE);
        setStatus(MachineStatus.RUNNING);
        onWorkTick(level, r);
        if (progress >= maxProgress) {
            progress = 0;
            workCarry = 0;
            // parallel: run the recipe again as long as inputs and output space allow, up to the parallel limit
            // (a recipe whose finish does something to the world, not just make items, runs once - see singleRunOnly)
            int runs = singleRunOnly(r) ? 1 : parallelCount();
            int runsDone = 0;
            for (int k = 0; k < runs; k++) {
                if (k > 0 && !(r.matches(new QHRecipeInput(inputStacks(), inputFluids(), catalyst.getStackInSlot(0)), level) && canOutput(r, true))) break;
                if (!consumeInputs(r)) break;          // (what it needed from the ME network was gone)
                wearCatalysts(level, r);               // 1.2.0: catalysts and instruments wear
                runsDone++;
                rollChances(level, r);
                canOutput(r, false);
                bonusOutputs(level, r);                // 1.2.0: a finely tuned multiblock makes more
            }
            rolled = null;
            if (runsDone > 0) {
                NeoForge.EVENT_BUS.post(new MachineRecipeEvent.Finish(this, holder.id(), r));
                onRecipeFinished(level, r, runsDone);
            }
            if (r.insight > 0 && owner != null) ResearchService.addInsight(level.getServer(), owner, (int) Math.min(Integer.MAX_VALUE, (long) r.insight * Math.max(1, runsDone)));
            recipeDirty = true;
        }
        markSync();
        setChanged();
    }

    protected void tickGenerator(ServerLevel level) {
        if (burnTime > 0) {
            long produced = energy.generate((long) (effectiveBurnRate() * outputFactor()));
            burnTime--;
            progress = maxProgress - burnTime;
            setStatus(produced > 0 || energy.getEnergyStored() < energy.getMaxEnergyStored() ? MachineStatus.GENERATING : MachineStatus.OUTPUT_FULL);
            if (burnTime == 0) {
                progress = 0;
                burnRate = 0;
                recipeDirty = true;
            }
            markSync();
            setChanged();
        } else {
            RecipeHolder<QHRecipe> holder = findRecipe(level);
            if (holder == null) {
                setStatus(MachineStatus.IDLE);
            } else {
                QHRecipe r = holder.value();
                currentRecipe = holder.id();
                if (!researchOk(r)) setStatus(MachineStatus.NEEDS_RESEARCH);
                else if (!tierOk(r)) setStatus(MachineStatus.LOW_TIER);
                else if (!canOutput(r, true)) setStatus(MachineStatus.OUTPUT_FULL);
                else if (energy.getEnergyStored() >= energy.getMaxEnergyStored()) setStatus(MachineStatus.OUTPUT_FULL);
                else if (!NeoForge.EVENT_BUS.post(new MachineRecipeEvent.Start(this, holder.id(), r)).isCanceled()) {
                    if (!consumeInputs(r)) {
                        setStatus(MachineStatus.BLOCKED);
                        return;
                    }
                    wearCatalysts(level, r);
                    canOutput(r, false);
                    burnRate = scaledEnergy(r);
                    burnTime = maxProgress = scaledDuration(r);
                    progress = 0;
                    NeoForge.EVENT_BUS.post(new MachineRecipeEvent.Finish(this, holder.id(), r));
                    onRecipeFinished(level, r);
                    setStatus(MachineStatus.GENERATING);
                } else setStatus(MachineStatus.BLOCKED);
            }
        }
        pushEnergy(level);
    }

    /** 1.2.0: the neighbours' energy handlers, looked up once and kept (the game keeps each cache current). */
    private final java.util.Map<Long, net.neoforged.neoforge.capabilities.BlockCapabilityCache<IEnergyStorage, Direction>> energyCaches = new java.util.HashMap<>();

    protected IEnergyStorage energyCap(ServerLevel level, BlockPos target, Direction side) {
        long k = target.asLong() * 6 + side.ordinal();
        var c = energyCaches.get(k);
        if (c == null || c.level() != level) {
            c = net.neoforged.neoforge.capabilities.BlockCapabilityCache.create(Capabilities.EnergyStorage.BLOCK, level, target.immutable(), side);
            if (energyCaches.size() > 256) energyCaches.clear();
            energyCaches.put(k, c);
        }
        return c.getCapability();
    }

    protected void pushEnergy(ServerLevel level) {
        if (energy.stored() <= 0) return;
        for (BlockPos p : energyOutputPositions()) {
            for (Direction d : Direction.values()) {
                BlockPos target = p.relative(d);
                if (target.equals(worldPosition) || isOwnPart(target) || !level.isLoaded(target)) continue;     // (never load a chunk to push power)
                IEnergyStorage storage = energyCap(level, target, d.getOpposite());
                if (storage != null && storage.canReceive()) {
                    Long cap = params().maxOutput();         // round 31: a pack may set the generator's output rate
                    long rate = cap != null ? Math.min(Integer.MAX_VALUE, cap) : Math.max(type.maxIO, Math.min(Integer.MAX_VALUE, effectiveBurnRate() * 4L));
                    int sent = storage.receiveEnergy((int) Math.min(energy.stored(), rate), false);
                    if (sent > 0) energy.consume(sent);
                    if (energy.stored() <= 0) return;
                }
            }
        }
    }

    protected List<BlockPos> energyOutputPositions() {
        return List.of(worldPosition);
    }

    protected boolean isOwnPart(BlockPos pos) {
        return false;
    }

    protected void onWorkTick(ServerLevel level, QHRecipe recipe) {}

    protected void onRecipeFinished(ServerLevel level, QHRecipe recipe) {}

    /** Round 29: the same, told how many parallel runs just finished together (normally 1). */
    protected void onRecipeFinished(ServerLevel level, QHRecipe recipe, int runs) {
        onRecipeFinished(level, recipe);
    }

    /**
     * Round 29: recipes whose finish has a one-off effect on the world (secret recipes: the Omnilex collapse...) must
     * not be run in parallel - extra copies would eat their inputs and do nothing more.
     */
    protected boolean singleRunOnly(QHRecipe r) {
        // 1.3.0: a launch with nothing to bring back (a Station Module) changes the world, so it runs once and is counted once
        if ((type == MachineType.ROCKET_ASSEMBLY || type == MachineType.SPACE_ELEVATOR) && r.itemOutputs.isEmpty() && r.fluidOutputs.isEmpty()) return true;
        return r.secret;
    }

    /**
     * Takes one run's inputs. The machine's own slots are used first, then anything outside (an ME network). The outside
     * part is taken first and all at once; if it is not all there any more (someone else took it), nothing is used and
     * this returns false.
     */
    /** 1.2.0: whether the last run's feeds were all assayed (or could not be) - see Wear.feedsPure. */
    protected boolean lastFeedsPure = true;

    protected boolean consumeInputs(QHRecipe r) {
        List<ItemStack> in = inputStacks();
        List<FluidStack> fin = inputFluids();
        lastFeedsPure = r.catalysts.isEmpty() || com.theoreticalsciences.science.Wear.feedsPure(r.fluidInputs, fin);
        int base = Math.min(type.itemInputs, in.size()), fbase = Math.min(type.fluidInputs, fin.size());
        boolean extra = in.size() > base || fin.size() > fbase;
        if (extra) {
            // work it out on copies first
            List<ItemStack> ci = new ArrayList<>(in.size());
            for (ItemStack s : in) ci.add(s.copy());
            List<FluidStack> cf = new ArrayList<>(fin.size());
            for (FluidStack s : fin) cf.add(s.copy());
            if (!RecipeMatcher.matchItems(r.itemInputs, ci, true) || !RecipeMatcher.matchFluids(r.fluidInputs, cf, true)) return false;
            List<ItemStack> wantI = new ArrayList<>();
            for (int i = base; i < in.size(); i++) {
                int used = in.get(i).getCount() - ci.get(i).getCount();
                if (used > 0) wantI.add(in.get(i).copyWithCount(used));
            }
            List<FluidStack> wantF = new ArrayList<>();
            for (int i = fbase; i < fin.size(); i++) {
                int used = fin.get(i).getAmount() - cf.get(i).getAmount();
                if (used > 0) wantF.add(fin.get(i).copyWithAmount(used));
            }
            if ((!wantI.isEmpty() || !wantF.isEmpty()) && !takeExtra(wantI, wantF)) return false;
            // the machine's own part
            for (int i = 0; i < base; i++) in.get(i).setCount(ci.get(i).getCount());
            for (int i = 0; i < fbase; i++) fin.get(i).setAmount(cf.get(i).getAmount());
        } else {
            RecipeMatcher.matchItems(r.itemInputs, in, true);
            RecipeMatcher.matchFluids(r.fluidInputs, fin, true);
        }
        for (int i = 0; i < type.fluidInputs; i++) if (tanks[i].getFluid().isEmpty()) tanks[i].setFluid(FluidStack.EMPTY);
        onInventoryChanged();
        return true;
    }

    /** 1.2.0: the share of catalyst wear that counts (a console's Symbiont Lining lowers it). */
    protected double wearScale() {
        return 1.0;
    }

    /** 1.2.0: the recipe feeds a living catalyst: it takes sugar or nutrient broth. */
    static boolean feeds(QHRecipe r) {
        for (var ing : r.itemInputs)
            if (ing.ingredient().test(new ItemStack(net.minecraft.world.item.Items.SUGAR))) return true;
        net.minecraft.world.level.material.Fluid broth = net.minecraft.core.registries.BuiltInRegistries.FLUID.get(
                com.theoreticalsciences.TheoreticalSciences.id("nutrient_broth"));
        for (var f : r.fluidInputs)
            if (f.ingredient().test(new FluidStack(broth, 1000))) return true;
        return false;
    }

    /** 1.2.0: true while the machine's physics shows a "wear" condition - catalysts then wear ten times as fast. */
    protected boolean wearHot() {
        return false;
    }

    /**
     * 1.2.0: the run used a run of life of the catalyst in the catalyst slot (see {@link com.theoreticalsciences.science.Wear}).
     * A worn-out restorable one goes to the output slots, or out of the top of the machine if they are full.
     */
    protected void wearCatalysts(ServerLevel level, QHRecipe r) {
        if (r.catalysts.isEmpty() || catalyst.getStackInSlot(0).isEmpty()) return;
        List<ItemStack> off = com.theoreticalsciences.science.Wear.afterRun(r.catalysts, List.of(catalyst.getStackInSlot(0)), wearHot(), lastFeedsPure,
                wearScale(), feeds(r), level);
        if (catalyst.getStackInSlot(0).isEmpty()) catalyst.setStackInSlot(0, ItemStack.EMPTY);
        for (ItemStack w : off) {
            for (int i = 0; i < type.itemOutputs && !w.isEmpty(); i++) {
                int slot = type.itemInputs + i;
                ItemStack o = items.getStackInSlot(slot);
                if (o.isEmpty()) {
                    items.setStackInSlot(slot, w.copy());
                    w = ItemStack.EMPTY;
                } else if (ItemStack.isSameItemSameComponents(o, w) && o.getCount() < o.getMaxStackSize()) {
                    o.grow(1);
                    w = ItemStack.EMPTY;
                }
            }
            if (!w.isEmpty()) net.minecraft.world.level.block.Block.popResource(level, worldPosition.above(), w);
        }
        onInventoryChanged();
    }

    /** Which chance outputs were produced this cycle (null = all; used while simulating). */
    @Nullable
    protected boolean[] rolled;

    protected void rollChances(ServerLevel level, QHRecipe r) {
        rolled = new boolean[r.itemOutputs.size()];
        for (int i = 0; i < rolled.length; i++) rolled[i] = level.random.nextFloat() < r.chance(i);
    }

    /** 1.2.0: extra products after a cycle (multiblocks: precision). Nothing here. */
    protected void bonusOutputs(ServerLevel level, QHRecipe r) {}

    /** Puts an extra product in the output slots, as much as fits (the rest is not made). */
    protected void addOutput(ItemStack rem) {
        if (level != null) com.theoreticalsciences.science.Wear.stamp(rem, level.getGameTime());
        for (int i = 0; i < type.itemOutputs && !rem.isEmpty(); i++) {
            ItemStack s = items.getStackInSlot(type.itemInputs + i);
            if (s.isEmpty()) {
                int m = Math.min(rem.getCount(), Math.min(rem.getMaxStackSize(), items.getSlotLimit(type.itemInputs + i)));
                items.setStackInSlot(type.itemInputs + i, rem.copyWithCount(m));
                rem.shrink(m);
            } else if (ItemStack.isSameItemSameComponents(s, rem)) {
                int room = Math.min(s.getMaxStackSize(), items.getSlotLimit(type.itemInputs + i)) - s.getCount();
                int m = Math.min(room, rem.getCount());
                if (m > 0) {
                    ItemStack n = s.copy();
                    n.grow(m);
                    items.setStackInSlot(type.itemInputs + i, n);
                    rem.shrink(m);
                }
            }
        }
    }

    /** Puts extra fluid in the output tanks, as much as fits. */
    protected void addFluidOutput(FluidStack f) {
        int rem = f.getAmount(), cap = type.tankCapacity();
        for (int i = 0; i < type.fluidOutputs && rem > 0; i++) {
            var t = tanks[type.fluidInputs + i];
            FluidStack cur = t.getFluid();
            if (cur.isEmpty()) {
                int m = Math.min(cap, rem);
                t.setFluid(f.copyWithAmount(m));
                rem -= m;
            } else if (FluidStack.isSameFluidSameComponents(cur, f)) {
                int m = Math.min(cap - cur.getAmount(), rem);
                if (m > 0) {
                    t.setFluid(cur.copyWithAmount(cur.getAmount() + m));
                    rem -= m;
                }
            }
        }
    }

    /** Checks (simulate) or inserts (execute) all outputs. Simulation assumes every chance output is produced. */
    protected boolean canOutput(QHRecipe r, boolean simulate) {
        ItemStack[] slots = new ItemStack[type.itemOutputs];
        for (int i = 0; i < slots.length; i++) slots[i] = items.getStackInSlot(type.itemInputs + i).copy();
        for (int oi = 0; oi < r.itemOutputs.size(); oi++) {
            if (!simulate && rolled != null && oi < rolled.length && !rolled[oi]) continue;
            ItemStack out = r.itemOutputs.get(oi);
            ItemStack rem = out.copy();
            if (level != null) com.theoreticalsciences.science.Wear.stamp(rem, level.getGameTime());     // 1.2.0: a source's clock starts
            for (int i = 0; i < slots.length && !rem.isEmpty(); i++) {
                if (slots[i].isEmpty()) {
                    slots[i] = rem.copy();
                    rem = ItemStack.EMPTY;
                } else if (ItemStack.isSameItemSameComponents(slots[i], rem)) {
                    int room = Math.min(slots[i].getMaxStackSize(), items.getSlotLimit(type.itemInputs + i)) - slots[i].getCount();
                    int moved = Math.min(room, rem.getCount());
                    slots[i].grow(moved);
                    rem.shrink(moved);
                }
            }
            if (!rem.isEmpty()) return false;
        }
        FluidStack[] fl = new FluidStack[type.fluidOutputs];
        for (int i = 0; i < fl.length; i++) fl[i] = tanks[type.fluidInputs + i].getFluid().copy();
        int cap = type.tankCapacity();
        for (FluidStack out : r.fluidOutputs) {
            int rem = out.getAmount();
            for (int i = 0; i < fl.length && rem > 0; i++) {
                if (fl[i].isEmpty()) {
                    int m = Math.min(cap, rem);
                    fl[i] = out.copyWithAmount(m);
                    rem -= m;
                } else if (FluidStack.isSameFluidSameComponents(fl[i], out)) {
                    int m = Math.min(cap - fl[i].getAmount(), rem);
                    fl[i].grow(m);
                    rem -= m;
                }
            }
            if (rem > 0) return false;
        }
        if (!simulate) {
            for (int i = 0; i < slots.length; i++) items.setStackInSlot(type.itemInputs + i, slots[i]);
            for (int i = 0; i < fl.length; i++) tanks[type.fluidInputs + i].setFluid(fl[i]);
        }
        return true;
    }

    // ------------------------------------------------------------------ menu

    public final ContainerData data = new ContainerData() {
        @Override
        public int get(int i) {
            return switch (i) {
                case 0 -> (int) energy.stored() & 0xFFFF;
                case 1 -> (int) (energy.stored() >>> 16) & 0xFFFF;
                case 2 -> (int) energy.capacity() & 0xFFFF;
                case 3 -> (int) (energy.capacity() >>> 16) & 0xFFFF;
                case 4 -> Math.min(progress, 0x7FFF);
                case 5 -> Math.min(maxProgress, 0x7FFF);
                case 6 -> status.ordinal();
                case 7 -> effectiveBurnRate() & 0xFFFF;
                case 8 -> effectiveBurnRate() >>> 16;
                case 9 -> (int) (energy.stored() >>> 32) & 0xFFFF;
                case 10 -> (int) (energy.stored() >>> 48) & 0xFFFF;
                case 11 -> (int) (energy.capacity() >>> 32) & 0xFFFF;
                case 12 -> (int) (energy.capacity() >>> 48) & 0xFFFF;
                default -> 0;
            };
        }

        @Override
        public void set(int i, int v) {}

        @Override
        public int getCount() {
            return 13;
        }
    };

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.theoreticalsciences." + type.id());
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new MachineMenu(id, inv, this, data);
    }

    // ------------------------------------------------------------------ persistence & sync

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider regs) {
        super.saveAdditional(tag, regs);
        writeShared(tag, regs);
        tag.putInt("BurnTime", burnTime);
        if (owner != null) tag.putUUID("Owner", owner);
    }

    protected void writeShared(CompoundTag tag, HolderLookup.Provider regs) {
        tag.put("Items", items.serializeNBT(regs));
        tag.put("Catalyst", catalyst.serializeNBT(regs));
        tag.putInt("InSlots", type.itemInputs);
        tag.putInt("FluidInSlots", type.fluidInputs);
        ListTag tl = new ListTag();
        for (FluidTank t : tanks) tl.add(t.writeToNBT(regs, new CompoundTag()));
        tag.put("Tanks", tl);
        tag.putLong("EnergyL", energy.stored());
        tag.putLong("Capacity", energy.capacity());
        tag.putInt("Progress", progress);
        tag.putInt("MaxProgress", maxProgress);
        tag.putInt("BurnRate", burnRate);
        tag.putInt("Status", status.ordinal());
        tag.putString("OwnerName", ownerName);
        if (currentRecipe != null) tag.putString("Recipe", currentRecipe.toString());     // the console draws it
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider regs) {
        super.loadAdditional(tag, regs);
        // saved with fewer input slots (before round 16, or a config change): outputs move up to their new place
        int oldIn = tag.contains("InSlots") ? tag.getInt("InSlots") : type.legacyItemInputs;
        int oldFin = tag.contains("FluidInSlots") ? tag.getInt("FluidInSlots") : type.legacyFluidInputs;
        if (tag.contains("Items")) {
            CompoundTag it = tag.getCompound("Items");
            it.putInt("Size", type.itemSlots());
            if (oldIn != type.itemInputs) {
                ListTag list = it.getList("Items", Tag.TAG_COMPOUND);
                for (int i = 0; i < list.size(); i++) {
                    CompoundTag e = list.getCompound(i);
                    int slot = e.getInt("Slot");
                    if (slot >= oldIn) e.putInt("Slot", slot - oldIn < type.itemOutputs ? type.itemInputs + (slot - oldIn) : -1);
                    else if (slot >= type.itemInputs) e.putInt("Slot", -1);
                }
            }
            items.deserializeNBT(regs, it);
        }
        if (tag.contains("Catalyst")) catalyst.deserializeNBT(regs, tag.getCompound("Catalyst"));
        else migrateCatalyst();
        ListTag tl = tag.getList("Tanks", Tag.TAG_COMPOUND);
        for (int i = 0; i < tl.size(); i++) {
            int to = i < oldFin ? (i < type.fluidInputs ? i : -1) : (i - oldFin < type.fluidOutputs ? type.fluidInputs + (i - oldFin) : -1);
            if (to >= 0 && to < tanks.length) tanks[to].readFromNBT(regs, tl.getCompound(i));
        }
        if (tag.contains("Capacity")) energy.setCapacity(Math.max(com.theoreticalsciences.custom.MachineParams.energyCapacity(type), tag.getLong("Capacity")));
        energy.setRaw(tag.contains("EnergyL") ? tag.getLong("EnergyL") : tag.getInt("Energy"));
        progress = tag.getInt("Progress");
        maxProgress = tag.getInt("MaxProgress");
        burnRate = tag.getInt("BurnRate");
        if (tag.contains("BurnTime")) burnTime = tag.getInt("BurnTime");
        int st = tag.getInt("Status");
        status = st >= 0 && st < MachineStatus.values().length ? MachineStatus.values()[st] : MachineStatus.IDLE;
        currentRecipe = tag.contains("Recipe") ? ResourceLocation.tryParse(tag.getString("Recipe")) : null;
        if (tag.hasUUID("Owner")) owner = tag.getUUID("Owner");
        ownerName = tag.getString("OwnerName");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider regs) {
        CompoundTag tag = new CompoundTag();
        writeShared(tag, regs);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && level.isClientSide) com.theoreticalsciences.client.ClientHooks.onMachineLoaded(this);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null && level.isClientSide) com.theoreticalsciences.client.ClientHooks.onMachineUnloaded(this);
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        if (level != null && level.isClientSide) com.theoreticalsciences.client.ClientHooks.onMachineUnloaded(this);
    }

    /**
     * Too much to drop as item entities (a stack of millions would be millions of entities): then the machine's own item
     * carries its contents instead, like a shulker box (see MachineBlock.getDrops).
     */
    public boolean oversized() {
        for (int i = 0; i < items.getSlots(); i++) {
            ItemStack s = items.getStackInSlot(i);
            if (!s.isEmpty() && s.getCount() > 64L * s.getMaxStackSize()) return true;
        }
        for (FluidTank t : tanks) if (t.getFluidAmount() >= 1_000_000) return true;
        return false;
    }

    /**
     * True when breaking this machine should pack everything into its dropped item (instead of spilling the slots).
     * Normally that is only when it is too full to spill; some machines (the Dyson controller) also hold things that
     * cannot be dropped as items at all, and override this.
     */
    public boolean keepsContentsInItem() {
        return oversized();
    }

    /**
     * 1.2.0: a machine saved before the catalyst slot: a catalyst that wears, left in an input slot, moves into the catalyst
     * slot (one of it; the rest stay where they were).
     */
    private void migrateCatalyst() {
        for (int i = 0; i < type.itemInputs; i++) {
            ItemStack s = items.getStackInSlot(i);
            if (s.isEmpty() || com.theoreticalsciences.science.Wear.profile(s) == null || !com.theoreticalsciences.science.Wear.isCatalyst(s)) continue;
            catalyst.setStackInSlot(0, s.copyWithCount(1));
            s.shrink(1);
            if (s.isEmpty()) items.setStackInSlot(i, ItemStack.EMPTY);
            return;
        }
    }

    public void dropContents() {
        if (level == null || keepsContentsInItem()) return;
        ItemStack cat = catalyst.getStackInSlot(0);
        if (!cat.isEmpty()) net.minecraft.world.Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, cat.copy());
        for (int i = 0; i < items.getSlots(); i++) {
            ItemStack s = items.getStackInSlot(i);
            if (!s.isEmpty()) net.minecraft.world.Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, s.copy());
        }
    }
}
