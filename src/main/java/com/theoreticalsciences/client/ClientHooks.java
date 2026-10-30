package com.theoreticalsciences.client;

import com.theoreticalsciences.QHConfig;
import com.theoreticalsciences.client.screen.ResearchScreen;
import com.theoreticalsciences.machine.ControllerBlockEntity;
import com.theoreticalsciences.machine.MachineBlockEntity;
import com.theoreticalsciences.machine.MachineType;
import com.theoreticalsciences.registry.QHParticles;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

/** Entry points from common code into client-only code. Only ever called on the logical client. */
public final class ClientHooks {
    /** Loaded multiblock controllers in the client world, for the effect renderer. */
    /** Meltdowns in progress: controller position -> {start game tick, hazard}. */
    public static final java.util.Map<net.minecraft.core.BlockPos, long[]> FAILURES = new java.util.concurrent.ConcurrentHashMap<>();

    public static void containmentFailure(net.minecraft.core.BlockPos pos, int hazard) {
        var mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.level == null) return;
        FAILURES.put(pos.immutable(), new long[]{mc.level.getGameTime(), hazard});
    }

    public static final Set<ControllerBlockEntity> CONTROLLERS = Collections.newSetFromMap(new WeakHashMap<>());

    /** Loaded fractured-reality blocks (the effect renderer glitches the view around clusters of them). */
    public static final Set<net.minecraft.world.level.block.entity.BlockEntity> FRACTURES = Collections.newSetFromMap(new WeakHashMap<>());
    /** Entity ids currently destabilised (synced from the server), drawn glitching and splitting. */
    public static final Set<Integer> DESTABILISED = java.util.concurrent.ConcurrentHashMap.newKeySet();
    /** Void / law wraiths in view, drawn by the effect renderer (space-warping bodies). */
    public static final Set<net.minecraft.world.entity.Entity> WRAITHS = Collections.newSetFromMap(new WeakHashMap<>());

    /** Active containment ruptures (drawn by the effect renderer). */
    public static final Set<net.minecraft.world.entity.Entity> RUPTURES = Collections.newSetFromMap(new WeakHashMap<>());

    /** Loaded bubble universes on the World Tree. */
    public static final Set<net.minecraft.world.level.block.entity.BlockEntity> BUBBLES = Collections.newSetFromMap(new WeakHashMap<>());

    /** Display miniatures (black holes, tesseracts... on pedestals), drawn by the effect renderer. */
    public static final Set<net.minecraft.world.level.block.entity.BlockEntity> DECOR = Collections.newSetFromMap(new WeakHashMap<>());
    /** Collapsed Omnilex hyperobjects in view. */
    public static final Set<net.minecraft.world.entity.Entity> HYPEROBJECTS = Collections.newSetFromMap(new WeakHashMap<>());
    /** Axiom Sentinels mid-proof (their beam is drawn by the effect renderer). */
    /** Singularities the Scepter made: drawn by EffectRenderer for their lifetime. */
    public record SingularityFx(net.minecraft.world.phys.Vec3 pos, int kind, long start, int ticks, net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dim) {}

    public static final java.util.List<SingularityFx> SINGULARITIES = new java.util.concurrent.CopyOnWriteArrayList<>();

    public static void openCenser(net.minecraft.world.item.ItemStack stack) {
        net.minecraft.client.Minecraft.getInstance().setScreen(new com.theoreticalsciences.client.screen.CenserScreen(stack));
    }

    public static void singularity(com.theoreticalsciences.endgame.SingularityFxPayload p) {
        var mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.level == null) return;
        if (SINGULARITIES.size() > 32) SINGULARITIES.remove(0);
        SINGULARITIES.add(new SingularityFx(new net.minecraft.world.phys.Vec3(p.x(), p.y(), p.z()), p.kind(), mc.level.getGameTime(), p.ticks(), mc.level.dimension()));
    }

    public static final Set<net.minecraft.world.entity.Entity> SENTINELS = Collections.newSetFromMap(new WeakHashMap<>());

    /** Round 34c: the forged Core's attunement state - opens its screen, or updates the open one. */
    public static void attune(BlockPos core, net.minecraft.nbt.CompoundTag tag) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof com.theoreticalsciences.client.screen.AttuneScreen s && s.core().equals(core)) s.receive(tag);
        else if (tag.getBoolean("open")) mc.setScreen(new com.theoreticalsciences.client.screen.AttuneScreen(core, tag));
    }

    public static void openOmnipotence(BlockPos core, int phase, int seconds) {
        Minecraft.getInstance().setScreen(new com.theoreticalsciences.client.screen.OmnipotenceScreen(core, phase, seconds));
    }

    /** The server asks: really collapse the Omnilex Engine? */
    public static void confirmCollapse(BlockPos pos) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) mc.level.playLocalSound(mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                com.theoreticalsciences.registry.QHSounds.OMNILEX_WARNING.get(), net.minecraft.sounds.SoundSource.MASTER, 1F, 1F, false);
        net.minecraft.client.gui.screens.Screen prev = mc.screen;
        mc.setScreen(new net.minecraft.client.gui.screens.ConfirmScreen(yes -> {
            net.neoforged.neoforge.network.PacketDistributor.sendToServer(new com.theoreticalsciences.network.CollapseAnswerPayload(pos, yes));
            mc.setScreen(prev instanceof com.theoreticalsciences.client.screen.MachineScreen ? prev : null);
        }, net.minecraft.network.chat.Component.translatable("gui.theoreticalsciences.collapse_title").withStyle(net.minecraft.ChatFormatting.DARK_RED, net.minecraft.ChatFormatting.BOLD),
                net.minecraft.network.chat.Component.translatable("gui.theoreticalsciences.collapse_body"),
                net.minecraft.network.chat.Component.translatable("gui.theoreticalsciences.collapse_yes").withStyle(net.minecraft.ChatFormatting.RED),
                net.minecraft.network.chat.Component.translatable("gui.theoreticalsciences.collapse_no")));
    }

    public static void onMachineLoaded(MachineBlockEntity be) {
        if (be instanceof ControllerBlockEntity c) CONTROLLERS.add(c);
    }

    public static void onMachineUnloaded(MachineBlockEntity be) {
        if (be instanceof ControllerBlockEntity c) CONTROLLERS.remove(c);
    }

    public static void openResearchScreen() {
        Minecraft.getInstance().setScreen(new ResearchScreen());
    }

    public static void receiveResearchDefs(String json) {
        ClientResearch.setDefs(json);
    }

    public static void receiveResearchState(List<ResourceLocation> unlocked, int insight, int observations, String active, int total, int remaining) {
        ClientResearch.setState(unlocked, insight, observations);
        ClientResearch.setActive(active, total, remaining);
    }

    /** Ambient particles for running machines. */
    public static void machineParticles(MachineBlockEntity be) {
        int density = QHConfig.PARTICLE_DENSITY.get();
        if (density <= 0) return;
        Level level = be.getLevel();
        if (level == null) return;
        RandomSource r = level.random;
        if (r.nextInt(4) >= density + 1) return;
        BlockPos p = be.getBlockPos();
        MachineType t = be.type;
        if (be instanceof ControllerBlockEntity c) {
            if (!c.isFormed()) return;
            Vec3 ctr = c.effectCenter();
            switch (t.effect) {
                case BLACK_HOLE, BLACK_HOLE_DISK, EYE -> {
                    // matter spiralling in
                    double ang = r.nextDouble() * Math.PI * 2, rad = 2.0 + r.nextDouble() * 1.5;
                    double x = ctr.x + Math.cos(ang) * rad, z = ctr.z + Math.sin(ang) * rad, y = ctr.y + (r.nextDouble() - 0.5) * 0.6;
                    level.addParticle(QHParticles.INFALL.get(), x, y, z, ctr.x, ctr.y, ctr.z);
                    if (r.nextInt(3) == 0) level.addParticle(QHParticles.HAWKING.get(), ctr.x, ctr.y, ctr.z,
                            (r.nextDouble() - 0.5) * 0.15, (r.nextDouble() - 0.5) * 0.15, (r.nextDouble() - 0.5) * 0.15);
                }
                case WHITE_HOLE -> {
                    for (int i = 0; i < density; i++)
                        level.addParticle(QHParticles.HAWKING.get(), ctr.x, ctr.y, ctr.z,
                                (r.nextDouble() - 0.5) * 0.35, (r.nextDouble() - 0.5) * 0.35, (r.nextDouble() - 0.5) * 0.35);
                }
                case DARK_LENS -> spawnAround(level, QHParticles.DARK_MOTE.get(), ctr, 2.2, r);
                case TESSERACT -> spawnAround(level, QHParticles.QUANTUM_FOAM.get(), ctr, 1.5, r);
                case COLLIDER -> {
                    double s = (r.nextBoolean() ? 1 : -1) * 3.0;
                    double a = (r.nextDouble() - 0.5) * 6.0;
                    boolean alongX = r.nextBoolean();
                    double x = ctr.x + (alongX ? a : s), z = ctr.z + (alongX ? s : a);
                    level.addParticle(QHParticles.TACHYON.get(), x, ctr.y, z, alongX ? 0.6 : 0, 0, alongX ? 0 : 0.6);
                }
                default -> {}
            }
            return;
        }
        double x = p.getX() + 0.5 + (r.nextDouble() - 0.5) * 0.9, y = p.getY() + 1.05, z = p.getZ() + 0.5 + (r.nextDouble() - 0.5) * 0.9;
        ParticleOptions particle = switch (t) {
            case TACHYON_RESONATOR -> QHParticles.TACHYON.get();
            case CHRONON_FIELD_INVERTER, ANTIMATTER_PENNING_TRAP -> QHParticles.HAWKING.get();
            default -> QHParticles.QUANTUM_FOAM.get();
        };
        if (t == MachineType.TACHYON_RESONATOR) level.addParticle(particle, x, y, z, (r.nextDouble() - 0.5) * 0.4, 0.25, (r.nextDouble() - 0.5) * 0.4);
        else level.addParticle(particle, x, y, z, 0, 0.02, 0);
    }

    private static void spawnAround(Level level, ParticleOptions p, Vec3 c, double rad, RandomSource r) {
        level.addParticle(p, c.x + (r.nextDouble() - 0.5) * rad * 2, c.y + (r.nextDouble() - 0.5) * rad * 2, c.z + (r.nextDouble() - 0.5) * rad * 2, 0, 0, 0);
    }

    private ClientHooks() {}
}
