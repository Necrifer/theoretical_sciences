package com.theoreticalsciences.ocean;

import com.theoreticalsciences.TheoreticalSciences;
import com.theoreticalsciences.interior.InteriorGear;
import com.theoreticalsciences.research.ResearchService;
import com.theoreticalsciences.world.QHDimensions;
import com.theoreticalsciences.world.transit.Transit;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.List;

/**
 * 1.4.0: the Hadal Trench - the deepest sea floor, eleven kilometres down (the Challenger Deep), reached from the
 * Overworld's deep oceans in a <b>Bathyscaphe</b>.
 * <ul>
 *   <li>Down: use the Bathyscaphe under water, in a deep-ocean biome, below Y 40. Research: Oceanography.</li>
 *   <li>Up: use it again anywhere in the trench; you surface on the same spot of the Overworld's sea.</li>
 *   <li>Its laws: a thousand atmospheres - without a full pressure suit (Pressure, Heated Pressure or Frontier, four
 *   pieces between them) the water crushes you; and you need air (an Oxygen Rebreather keeps you breathing).</li>
 *   <li>Life is topped up near players: anglerfish and siphonophores, with caps, every ten seconds.</li>
 * </ul>
 */
@EventBusSubscriber(modid = TheoreticalSciences.MODID)
public final class HadalTrench {
    public static final ResourceLocation RESEARCH = TheoreticalSciences.id("oceanography");

    public static class BathyscapheItem extends com.theoreticalsciences.item.LoreItem {
        public BathyscapheItem(Properties p) {
            super(p.stacksTo(1));
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            ItemStack st = player.getItemInHand(hand);
            if (!(player instanceof ServerPlayer sp)) return InteractionResultHolder.success(st);
            if (!ResearchService.has(sp, RESEARCH)) return fail(sp, st, "bathyscaphe_locked");
            boolean down = level.dimension() != QHDimensions.HADAL_TRENCH;
            if (down && (level.dimension() != Level.OVERWORLD || !sp.isUnderWater() || sp.getY() > 40
                    || !level.getBiome(sp.blockPosition()).is(BiomeTags.IS_DEEP_OCEAN))) return fail(sp, st, "bathyscaphe_where");
            if (!Transit.start(sp, QHDimensions.HADAL_TRENCH, !down, p -> travelNow(p, down))) return InteractionResultHolder.fail(st);
            sp.getCooldowns().addCooldown(this, 100);
            return InteractionResultHolder.consume(st);
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
            super.appendHoverText(stack, ctx, tip, flag);
            tip.add(Component.translatable("tooltip.theoreticalsciences.bathyscaphe").withStyle(ChatFormatting.GRAY));
        }
    }

    static InteractionResultHolder<ItemStack> fail(ServerPlayer sp, ItemStack st, String key) {
        sp.displayClientMessage(Component.translatable("message.theoreticalsciences." + key).withStyle(ChatFormatting.GRAY), true);
        return InteractionResultHolder.fail(st);
    }

    static void travelNow(ServerPlayer sp, boolean down) {
        ServerLevel target = sp.server.getLevel(down ? QHDimensions.HADAL_TRENCH : Level.OVERWORLD);
        if (target == null) return;
        int x = sp.getBlockX(), z = sp.getBlockZ();
        target.getChunk(x >> 4, z >> 4);
        BlockPos dest;
        if (down) {
            // settle two blocks over the floor, in the water
            int y = target.getMaxBuildHeight() - 20;
            while (y > target.getMinBuildHeight() + 2 && !target.getFluidState(new BlockPos(x, y, z)).isEmpty()) y--;
            dest = new BlockPos(x, y + 2, z);
        } else {
            dest = new BlockPos(x, target.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) + 1, z);   // bob up at the surface
        }
        sp.stopRiding();
        if (sp.changeDimension(new DimensionTransition(target, Vec3.atBottomCenterOf(dest), Vec3.ZERO, sp.getYRot(), sp.getXRot(),
                DimensionTransition.DO_NOTHING)) == null) return;
        sp.fallDistance = 0;
        if (down) sp.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 600, 0, false, true));   // the sphere's air, for a while
        target.playSound(null, dest, com.theoreticalsciences.registry.QHSounds.GATEWAY.get(), SoundSource.PLAYERS, 0.8F, 0.5F);
        sp.displayClientMessage(Component.translatable(down ? "message.theoreticalsciences.hadal_arrival" : "message.theoreticalsciences.hadal_return")
                .withStyle(ChatFormatting.DARK_AQUA), false);
    }

    static boolean suited(Player p) {
        return InteriorGear.pieces(p, InteriorGear.Suit.PRESSURE) + InteriorGear.pieces(p, InteriorGear.Suit.HEATED)
                + InteriorGear.pieces(p, InteriorGear.Suit.FRONTIER) >= 4;
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || p.isSpectator() || p.level().dimension() != QHDimensions.HADAL_TRENCH) return;
        if ((p.tickCount + p.getId()) % 200 == 0) populate(p, p.serverLevel());
        if (p.isCreative()) return;
        // breath: a rebreather keeps you going, as in Titan's ocean
        ItemStack rb = com.theoreticalsciences.titan.TitanGear.rebreather(p);
        if (!rb.isEmpty() && p.isUnderWater()) {
            p.setAirSupply(p.getMaxAirSupply());
            if ((p.level().getGameTime() + p.getId()) % 20 == 0) rb.setDamageValue(Math.min(rb.getMaxDamage(), rb.getDamageValue() + 1));
        }
        // pressure: crushing, unless suited
        if ((p.tickCount + p.getId()) % 40 == 0 && !suited(p) && p.isInWater()) {
            p.hurt(p.damageSources().inWall(), 3.0F);
            p.displayClientMessage(Component.translatable("message.theoreticalsciences.hadal_pressure").withStyle(ChatFormatting.DARK_RED), true);
        }
    }

    static void populate(ServerPlayer p, ServerLevel level) {
        if (level.getDifficulty() == Difficulty.PEACEFUL) return;
        var box = p.getBoundingBox().inflate(48);
        if (level.getEntitiesOfClass(HadalMobs.Anglerfish.class, box).size() < 3) arrive(p, level, OceanRegistry.HADAL_ANGLERFISH.get());
        if (level.getEntitiesOfClass(HadalMobs.Siphonophore.class, box).size() < 2) arrive(p, level, OceanRegistry.SIPHONOPHORE.get());
    }

    static void arrive(ServerPlayer p, ServerLevel level, EntityType<? extends OceanMobs.Swimmer> type) {
        var r = p.getRandom();
        for (int tries = 0; tries < 8; tries++) {
            double a = r.nextDouble() * Math.PI * 2, d = 12 + r.nextDouble() * 16;
            BlockPos at = BlockPos.containing(p.getX() + Math.cos(a) * d, p.getY() + r.nextInt(9) - 4, p.getZ() + Math.sin(a) * d);
            if (!level.isLoaded(at) || !level.getFluidState(at).isSource() || !level.getFluidState(at.above()).isSource()) continue;
            OceanMobs.Swimmer s = type.create(level);
            if (s == null) return;
            s.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, r.nextFloat() * 360, 0);
            s.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.NATURAL, null);
            level.addFreshEntity(s);
            return;
        }
    }

    private HadalTrench() {}
}
