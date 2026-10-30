package com.theoreticalsciences.foam;

import com.theoreticalsciences.TheoreticalSciences;
import com.theoreticalsciences.registry.QHBlocks;
import com.theoreticalsciences.registry.QHParticles;
import com.theoreticalsciences.registry.QHSounds;
import com.theoreticalsciences.world.QHDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.item.ItemExpireEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * Round 36: the Quantum Foam's fizz, made dependable. Its ores condense out of the substrate and give themselves back
 * (EphemeralOreBlock / FoamSubstrateBlock) on random ticks - but random ticks are rare (each block gets one every
 * minute or so), so in practice the walls looked still. Around every player in the Foam, twice a second, a handful of
 * spots are checked: exposed substrate now and then condenses an ore, and an ore now and then pops back into the
 * vacuum. Virtual particles lying on the ground (they last half a minute here) shimmer as they fade and vanish with a
 * pop instead of silently.
 * Only near players, so a chunk kept loaded by a chunk loader is not churned while nobody is there to see it.
 */
@EventBusSubscriber(modid = TheoreticalSciences.MODID)
public final class FoamFizz {
    private static final int RADIUS = 20, SAMPLES = 48;

    @SubscribeEvent
    public static void onTick(LevelTickEvent.Post e) {
        if (!(e.getLevel() instanceof ServerLevel l) || l.dimension() != QHDimensions.FOAM || l.getGameTime() % 10 != 0) return;
        RandomSource r = l.random;
        for (ServerPlayer p : l.players()) {
            if (p.isSpectator()) continue;
            BlockPos c = p.blockPosition();
            for (int i = 0; i < SAMPLES; i++) {
                BlockPos at = c.offset(r.nextInt(RADIUS * 2 + 1) - RADIUS, r.nextInt(RADIUS * 2 + 1) - RADIUS, r.nextInt(RADIUS * 2 + 1) - RADIUS);
                if (!l.isLoaded(at)) continue;
                BlockState st = l.getBlockState(at);
                if (st.getBlock() instanceof EphemeralOreBlock) {
                    if (r.nextInt(5) == 0) popBack(l, at);
                } else if (st.getBlock() instanceof FoamSubstrateBlock && r.nextInt(40) == 0 && exposed(l, at)) {
                    l.setBlock(at, FoamField.ore(FoamField.dominant(l, at)).defaultBlockState(), 3);
                    l.sendParticles(QHParticles.PHOTON.get(), at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5, 12, 0.4, 0.4, 0.4, 0.02);
                    l.playSound(null, at, QHSounds.FOAM_POP.get(), SoundSource.BLOCKS, 0.5F, 1.4F);
                }
            }
            // virtual particles on the ground fade: a shimmer in their last five seconds
            if (l.getGameTime() % 20 == 0)
                for (ItemEntity it : l.getEntitiesOfClass(ItemEntity.class, p.getBoundingBox().inflate(RADIUS), FoamFizz::virtual))
                    if (it.lifespan - it.getAge() < 100)
                        l.sendParticles(QHParticles.PHASE_MOTE.get(), it.getX(), it.getY() + 0.25, it.getZ(), 3, 0.15, 0.15, 0.15, 0.01);
        }
    }

    static void popBack(ServerLevel l, BlockPos at) {
        l.setBlock(at, QHBlocks.get("foam_substrate").defaultBlockState(), 3);
        l.sendParticles(QHParticles.PHASE_MOTE.get(), at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5, 20, 0.4, 0.4, 0.4, 0.05);
        l.playSound(null, at, QHSounds.FOAM_POP.get(), SoundSource.BLOCKS, 0.8F, 0.5F);
    }

    private static boolean exposed(ServerLevel l, BlockPos at) {
        for (Direction d : Direction.values()) if (l.getBlockState(at.relative(d)).isAir()) return true;
        return false;
    }

    static boolean virtual(ItemEntity it) {
        return BuiltInRegistries.ITEM.getKey(it.getItem().getItem()).getPath().startsWith("virtual_");
    }

    /** The half minute is up: it gives itself back to the vacuum, visibly. */
    @SubscribeEvent
    public static void onExpire(ItemExpireEvent e) {
        ItemEntity it = e.getEntity();
        if (!(it.level() instanceof ServerLevel l) || l.dimension() != QHDimensions.FOAM || !virtual(it)) return;
        l.sendParticles(QHParticles.PHASE_MOTE.get(), it.getX(), it.getY() + 0.25, it.getZ(), 16, 0.2, 0.2, 0.2, 0.05);
        l.playSound(null, it.blockPosition(), QHSounds.FOAM_POP.get(), SoundSource.NEUTRAL, 0.6F, 1.2F);
    }

    private FoamFizz() {}
}
