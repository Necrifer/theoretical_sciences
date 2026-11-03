package com.theoreticalsciences.machine;

import com.theoreticalsciences.TheoreticalSciences;
import com.theoreticalsciences.recipe.QHRecipe;
import com.theoreticalsciences.registry.QHFluids;
import com.theoreticalsciences.registry.QHItems;
import com.theoreticalsciences.science.Isotope;
import com.theoreticalsciences.science.RadiationService;
import com.theoreticalsciences.science.Sciences;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.Locale;

/**
 * Round 38 (1.2): the Tier II instruments. Each is an ordinary recipe machine plus one piece of physics:
 * <ul>
 *   <li><b>Seismograph</b> - must stand on solid ground to hear anything; when a run ends it times the P and S waves
 *   from what lies below and tells you how deep it is: d = Δt · v_P v_S / (v_P − v_S).</li>
 *   <li><b>Decay Chamber</b> - radon in its tanks decays on a real clock (scaled: a day passes in a minute) into
 *   Radiogenic Lead, and everything radioactive inside it gives off gamma rays around it.</li>
 *   <li><b>Magnetizer</b> - its field must beat a magnet's coercivity. An iron core (iron or magnet blocks touching it)
 *   strengthens the field.</li>
 *   <li><b>Induction Generator</b> - EMF = NBAω: the magnet sets B (the recipe), and flowing water turning it adds ω.</li>
 *   <li><b>Galton Board</b> - each piece falls through 12 rows of pegs; where it lands (binomial) picks its grade.</li>
 *   <li><b>Atomic Clock</b> - runs slower the deeper in the planet's gravity it stands (scaled up 10^15 times).</li>
 * </ul>
 */
public class ScienceMachineBlockEntity extends MachineBlockEntity {
    /** Real seconds of decay per game second in the Decay Chamber: one day a minute. */
    public static final double DECAY_SCALE = 1440;
    private static final TagKey<Block> IRON_BLOCKS = TagKey.create(Registries.BLOCK, net.minecraft.resources.ResourceLocation.parse("c:storage_blocks/iron"));

    private double decayCarry, leadCarry;
    /** Whether the other mods' radiation systems hold a source for this chamber now. */
    private boolean reported;

    @Override
    public void setRemoved() {
        if (reported && level instanceof ServerLevel sl) com.theoreticalsciences.science.RadiationCompat.worldSource(sl, worldPosition, 0);
        reported = false;
        super.setRemoved();
    }

    public ScienceMachineBlockEntity(MachineType type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public static boolean handles(MachineType t) {
        return switch (t) {
            case SEISMOGRAPH, DECAY_CHAMBER, MAGNETIZER, INDUCTION_GENERATOR, GALTON_BOARD, ATOMIC_CLOCK -> true;
            default -> false;
        };
    }

    // ------------------------------------------------------------------ every tick
    @Override
    protected void preTick(ServerLevel level) {
        super.preTick(level);
        if (type == MachineType.DECAY_CHAMBER && (level.getGameTime() + worldPosition.hashCode()) % 20 == 0) decaySecond(level);
    }

    /** One second in the Decay Chamber: radon decays (scaled real clock), and what is inside shines. */
    private void decaySecond(ServerLevel level) {
        var radon = QHFluids.ALL.get("radon");
        double strength = 0;
        for (int i = 0; i < items.getSlots(); i++) strength += RadiationService.carried(items.getStackInSlot(i));
        if (radon != null) {
            double lambda = Isotope.RN222.decayConstant() * DECAY_SCALE;
            for (var tank : tanks) {
                FluidStack fs = tank.getFluid();
                if (fs.isEmpty() || !fs.getFluid().isSame(radon.source.get())) continue;
                strength += fs.getAmount() / 1000.0 * 0.08;           // radon's short-lived daughters (Pb-214, Bi-214) give off gamma
                decayCarry += fs.getAmount() * (1 - Math.exp(-lambda));
                int gone = (int) decayCarry;
                if (gone > 0) {
                    decayCarry -= gone;
                    tank.drain(gone, net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
                    leadCarry += gone;
                }
            }
        }
        // every litre of radon that has decayed has become (through its daughters) a little lead
        while (leadCarry >= 1000) {
            ItemStack lead = new ItemStack(QHItems.get("radiogenic_lead"));
            if (!insertOutput(lead)) break;
            leadCarry -= 1000;
        }
        if (strength > 0) RadiationService.emit(level, worldPosition, Isotope.U238_CHAIN, strength);
        // 1.2.1: Mekanism's and Nuclear Radiation's own detectors read it too (a world source while it shines, removed after)
        if (strength > 0 || reported) com.theoreticalsciences.science.RadiationCompat.worldSource(level, worldPosition, strength);
        reported = strength > 0;
        setChanged();
    }

    private boolean insertOutput(ItemStack stack) {
        for (int i = type.itemInputs; i < type.itemInputs + type.itemOutputs; i++) {
            ItemStack cur = items.getStackInSlot(i);
            if (cur.isEmpty()) {
                items.setStackInSlot(i, stack.copy());
                return true;
            }
            if (ItemStack.isSameItemSameComponents(cur, stack) && cur.getCount() + stack.getCount() <= cur.getMaxStackSize()) {
                cur.grow(stack.getCount());
                items.setStackInSlot(i, cur);
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ speed and gates
    @Override
    protected double speedFactor() {
        return switch (type) {
            case SEISMOGRAPH -> coupled() ? 1.0 : 0.2;
            case ATOMIC_CLOCK -> clockRate(level, worldPosition);
            default -> 1.0;
        };
    }

    /** A seismometer hears the ground only through solid rock under it. */
    private boolean coupled() {
        BlockPos below = worldPosition.below();
        return level != null && level.getBlockState(below).isCollisionShapeFullBlock(level, below);
    }

    /**
     * Δt′/Δt = √(1 − 2Φ/c²). On a real planet a metre of height changes a clock by 1 part in 10^16; here the effect is
     * scaled up so it shows: Φ/c² = (320 − y) / 2000 in the Overworld (the floor of the world is deepest in the well).
     */
    public static double clockRate(Level level, BlockPos pos) {
        if (level == null) return 1;
        double phi = level.dimension() == Level.OVERWORLD ? Math.max(0, Math.min(0.24, (320 - pos.getY()) / 2000.0)) : 0.1;
        return Math.sqrt(1 - 2 * phi);
    }

    /** The Magnetizer's field, tesla: 0.6 T from its coil, plus 0.35 T for each iron or magnet block touching it. */
    public double field() {
        if (level == null) return 0.6;
        double b = 0.6;
        for (Direction d : Direction.values()) {
            BlockState s = level.getBlockState(worldPosition.relative(d));
            if (s.is(Sciences.MAGNETIC_BLOCKS) || s.is(IRON_BLOCKS)) b += 0.35;
        }
        return b;
    }

    /** Coercivity a magnet's field must beat to magnetise it (tesla). */
    static double coercivity(QHRecipe r) {
        if (r.itemOutputs.isEmpty()) return 0;
        String id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(r.itemOutputs.get(0).getItem()).getPath();
        return switch (id) {
            case "neodymium_magnet" -> 1.2;
            case "samarium_cobalt_magnet" -> 2.0;
            case "ferrite_magnet" -> 0.35;
            default -> 0;
        };
    }

    @Override
    protected MachineStatus gate(ServerLevel level, QHRecipe r) {
        MachineStatus g = super.gate(level, r);
        if (g != null) return g;
        if (type == MachineType.MAGNETIZER && field() < coercivity(r)) return MachineStatus.WRONG_ENVIRONMENT;
        return null;
    }

    /** Induction Generator: flowing water turning the coil (each side, up to four) spins it faster - EMF ∝ ω. */
    @Override
    protected double outputFactor() {
        if (type != MachineType.INDUCTION_GENERATOR || level == null) return 1.0;
        int wheels = 0;
        for (Direction d : Direction.Plane.HORIZONTAL) {
            var f = level.getFluidState(worldPosition.relative(d));
            if (f.is(FluidTags.WATER) && !f.isSource()) wheels++;
        }
        return 1.0 + 0.5 * wheels;
    }

    // ------------------------------------------------------------------ outcomes
    /** The Galton Board: twelve rows of pegs, left or right at each with even odds; the distance from the centre is the grade. */
    @Override
    protected void rollChances(ServerLevel level, QHRecipe r) {
        if (type != MachineType.GALTON_BOARD || r.itemOutputs.size() < 3) {
            super.rollChances(level, r);
            return;
        }
        int k = 0;
        for (int i = 0; i < 12; i++) if (level.random.nextBoolean()) k++;
        int dev = Math.abs(k - 6), grade = dev <= 1 ? 0 : dev <= 3 ? 1 : 2;
        rolled = new boolean[r.itemOutputs.size()];
        rolled[grade] = true;
    }

    @Override
    protected void onRecipeFinished(ServerLevel level, QHRecipe recipe) {
        super.onRecipeFinished(level, recipe);
        if (type == MachineType.SEISMOGRAPH) listen(level);
    }

    /** P and S waves from the nearest ore (or cave) below: the gap between them gives its depth. */
    private void listen(ServerLevel level) {
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        int best = -1;
        String what = null;
        for (int dy = 1; dy <= 64 && best < 0; dy++) {
            for (int dx = -3; dx <= 3 && best < 0; dx++)
                for (int dz = -3; dz <= 3; dz++) {
                    m.set(worldPosition.getX() + dx, worldPosition.getY() - dy, worldPosition.getZ() + dz);
                    if (!level.isLoaded(m) || level.isOutsideBuildHeight(m)) continue;
                    BlockState s = level.getBlockState(m);
                    if (s.is(Tags.Blocks.ORES)) {
                        best = dy;
                        what = s.getBlock().getName().getString();
                        break;
                    }
                    if (dy > 2 && s.isAir()) {
                        best = dy;
                        what = Component.translatable("message.theoreticalsciences.seismo_cave").getString();
                        break;
                    }
                }
        }
        Component msg;
        if (best < 0) msg = Component.translatable("message.theoreticalsciences.seismo_none").withStyle(ChatFormatting.GRAY);
        else {
            double vp = 6000, vs = 3500, dt = best / vs - best / vp;          // crustal rock, m/s
            msg = Component.translatable("message.theoreticalsciences.seismo", String.format(Locale.ROOT, "%.2f", dt * 1000), best, what)
                    .withStyle(ChatFormatting.GOLD);
        }
        for (var p : level.players())
            if (p.blockPosition().distSqr(worldPosition) < 16 * 16) p.displayClientMessage(msg, false);
    }

    // ------------------------------------------------------------------ save
    @Override
    protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider reg) {
        super.saveAdditional(tag, reg);
        if (type == MachineType.DECAY_CHAMBER) {
            tag.putDouble("DecayCarry", decayCarry);
            tag.putDouble("LeadCarry", leadCarry);
            tag.putBoolean("Reported", reported);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider reg) {
        super.loadAdditional(tag, reg);
        decayCarry = tag.getDouble("DecayCarry");
        leadCarry = tag.getDouble("LeadCarry");
        reported = tag.getBoolean("Reported");
    }

    static {
        TheoreticalSciences.LOGGER.debug("science machines ready");
    }
}
