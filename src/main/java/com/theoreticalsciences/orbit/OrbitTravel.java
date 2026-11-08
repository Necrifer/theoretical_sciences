package com.theoreticalsciences.orbit;

import com.theoreticalsciences.TheoreticalSciences;
import com.theoreticalsciences.machine.ControllerBlockEntity;
import com.theoreticalsciences.machine.MachineType;
import com.theoreticalsciences.research.ResearchService;
import com.theoreticalsciences.world.QHDimensions;
import com.theoreticalsciences.world.SafeLanding;
import com.theoreticalsciences.world.transit.Transit;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 1.3.0: getting to the Orbital Station and on from it.
 * <ul>
 *   <li>Up: a <b>Fuelled Crew Capsule</b> (flown on the Rocket Assembly) is boarded under open sky in the Overworld, within
 *   32 blocks of a formed Rocket Assembly, once the station's four modules are in orbit. Research: Orbital Mechanics.</li>
 *   <li>On: each <b>Transfer Console</b> aboard lists the destinations (data-driven, see {@link Destinations}): sneak and
 *   use it to pick the next one, use it to go. Every destination lands you on safe ground; the Overworld is always on the
 *   list, so there is always a way home.</li>
 * </ul>
 */
public final class OrbitTravel {
    public static final ResourceLocation RESEARCH = TheoreticalSciences.id("orbital_mechanics");
    private static final Map<UUID, Integer> CHOICE = com.theoreticalsciences.world.SessionMaps.perPlayer(new HashMap<>());

    // ------------------------------------------------------------------ up
    public static class CrewCapsuleItem extends com.theoreticalsciences.item.LoreItem {
        public CrewCapsuleItem(Properties p) {
            super(p.stacksTo(1));
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            ItemStack st = player.getItemInHand(hand);
            if (!(player instanceof ServerPlayer sp)) return InteractionResultHolder.success(st);
            if (!ResearchService.has(sp, RESEARCH)) return fail(sp, st, "capsule_locked");
            if (level.dimension() != Level.OVERWORLD || !level.canSeeSky(sp.blockPosition().above())) return fail(sp, st, "capsule_sky");
            if (!nearPad((ServerLevel) level, sp.blockPosition())) return fail(sp, st, "capsule_pad");
            OrbitData d = OrbitData.get(sp.server);
            if (!d.ready()) {
                sp.displayClientMessage(Component.translatable("message.theoreticalsciences.capsule_station", d.modules(), OrbitData.REQUIRED)
                        .withStyle(ChatFormatting.GRAY), true);
                return InteractionResultHolder.fail(st);
            }
            if (!Transit.start(sp, QHDimensions.ORBITAL_STATION, false, OrbitTravel::arrive)) return InteractionResultHolder.fail(st);
            if (!sp.isCreative()) st.shrink(1);
            sp.getCooldowns().addCooldown(this, 100);
            return InteractionResultHolder.consume(st);
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
            super.appendHoverText(stack, ctx, tip, flag);
            tip.add(Component.translatable("tooltip.theoreticalsciences.fuelled_crew_capsule").withStyle(ChatFormatting.GRAY));
        }
    }

    static InteractionResultHolder<ItemStack> fail(ServerPlayer sp, ItemStack st, String key) {
        sp.displayClientMessage(Component.translatable("message.theoreticalsciences." + key).withStyle(ChatFormatting.GRAY), true);
        return InteractionResultHolder.fail(st);
    }

    /** A formed Rocket Assembly (or, 1.4.0, Space Elevator) within 32 blocks (loaded chunks only; nothing is loaded to look). */
    static boolean nearPad(ServerLevel level, BlockPos at) {
        int cx = at.getX() >> 4, cz = at.getZ() >> 4;
        for (int dx = -2; dx <= 2; dx++)
            for (int dz = -2; dz <= 2; dz++) {
                if (!level.hasChunk(cx + dx, cz + dz)) continue;
                for (BlockEntity be : level.getChunk(cx + dx, cz + dz).getBlockEntities().values())
                    if (be instanceof ControllerBlockEntity c && (c.type == MachineType.ROCKET_ASSEMBLY || c.type == MachineType.SPACE_ELEVATOR) && c.isFormed()
                            && c.getBlockPos().distSqr(at) <= 32 * 32) return true;
            }
        return false;
    }

    static void arrive(ServerPlayer sp) {
        ServerLevel orbit = sp.server.getLevel(QHDimensions.ORBITAL_STATION);
        if (orbit == null) return;
        OrbitBuilder.ensureBuilt(orbit);
        BlockPos dest = SafeLanding.clear(orbit, OrbitBuilder.ARRIVAL);
        sp.stopRiding();
        if (sp.changeDimension(new DimensionTransition(orbit, Vec3.atBottomCenterOf(dest), Vec3.ZERO, 180F, 0F, DimensionTransition.DO_NOTHING)) == null) return;
        sp.fallDistance = 0;
        orbit.playSound(null, dest, com.theoreticalsciences.registry.QHSounds.GATEWAY.get(), SoundSource.PLAYERS, 0.8F, 1.2F);
        sp.displayClientMessage(Component.translatable("message.theoreticalsciences.orbit_arrival").withStyle(ChatFormatting.GOLD), false);
    }

    // ------------------------------------------------------------------ on
    public static class ConsoleBlock extends Block {
        public ConsoleBlock(Properties p) {
            super(p);
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (level.isClientSide || !(player instanceof ServerPlayer sp)) return InteractionResult.SUCCESS;
            if (level.dimension() != QHDimensions.ORBITAL_STATION) {
                sp.displayClientMessage(Component.translatable("message.theoreticalsciences.console_grounded").withStyle(ChatFormatting.GRAY), true);
                return InteractionResult.CONSUME;
            }
            List<Destinations.Destination> list = Destinations.INSTANCE.offered(sp);
            if (list.isEmpty()) return InteractionResult.CONSUME;
            int i = Math.floorMod(CHOICE.getOrDefault(sp.getUUID(), 0), list.size());
            if (sp.isShiftKeyDown()) {
                i = (i + 1) % list.size();
                CHOICE.put(sp.getUUID(), i);
                sp.displayClientMessage(Component.translatable("message.theoreticalsciences.console_target", Component.translatable(list.get(i).name()),
                        i + 1, list.size()).withStyle(ChatFormatting.AQUA), true);
                return InteractionResult.CONSUME;
            }
            depart(sp, list.get(i));
            return InteractionResult.CONSUME;
        }
    }

    static void depart(ServerPlayer sp, Destinations.Destination d) {
        // the arrival scene of one of our own worlds, otherwise the station falling away behind you
        boolean ours = d.dimension().location().getNamespace().equals(TheoreticalSciences.MODID);
        Transit.start(sp, ours ? d.dimension() : QHDimensions.ORBITAL_STATION, !ours, p -> land(p, d));
    }

    static void land(ServerPlayer sp, Destinations.Destination d) {
        ServerLevel target = sp.server.getLevel(d.dimension());
        if (target == null) return;
        BlockPos spawn = target.dimension() == Level.OVERWORLD ? target.getSharedSpawnPos() : BlockPos.ZERO;
        int x = spawn.getX(), z = spawn.getZ();
        target.getChunk(x >> 4, z >> 4);
        BlockPos dest = d.slowFall()
                ? new BlockPos(x, Math.min(target.getMaxBuildHeight() - 4, target.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) + 24), z)
                : SafeLanding.find(target, x, z, d.landingY(), d.platform(), null);
        sp.stopRiding();
        if (sp.changeDimension(new DimensionTransition(target, Vec3.atBottomCenterOf(dest), Vec3.ZERO, sp.getYRot(), sp.getXRot(),
                DimensionTransition.DO_NOTHING)) == null) return;
        sp.fallDistance = 0;
        if (d.slowFall()) sp.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 400, 0, false, true));   // the capsule's parachute
        target.playSound(null, dest, com.theoreticalsciences.registry.QHSounds.GATEWAY.get(), SoundSource.PLAYERS, 0.8F, 0.8F);
        sp.displayClientMessage(Component.translatable("message.theoreticalsciences.orbit_departure", Component.translatable(d.name()))
                .withStyle(ChatFormatting.GOLD), false);
    }

    private OrbitTravel() {}
}
