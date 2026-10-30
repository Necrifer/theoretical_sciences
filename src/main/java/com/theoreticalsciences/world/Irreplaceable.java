package com.theoreticalsciences.world;

import com.theoreticalsciences.TheoreticalSciences;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * Round 36 (progression audit): a few things exist once per world, or can only be made again with themselves - the
 * Rekindled Spark (every second Spark needs an Eschaton Core, which needs a Spark), the Echo of the Transcendental before
 * the Rekindling, the Omnilex, the Proof of Everything. Lost in lava, a cactus, an explosion, the void or by despawning,
 * the endgame would be stuck for good. Dropped, they are untouchable, never despawn, and come back up out of the void
 * (to whoever dropped them, or the world spawn). Packs can add items to #theoreticalsciences:irreplaceable.
 */
@EventBusSubscriber(modid = TheoreticalSciences.MODID)
public final class Irreplaceable {
    public static final TagKey<Item> TAG = TagKey.create(Registries.ITEM, TheoreticalSciences.id("irreplaceable"));

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent e) {
        if (e.getLevel().isClientSide() || !(e.getEntity() instanceof ItemEntity it) || !it.getItem().is(TAG)) return;
        it.setUnlimitedLifetime();
        it.setInvulnerable(true);
    }

    @SubscribeEvent
    public static void onTick(EntityTickEvent.Pre e) {
        if (!(e.getEntity() instanceof ItemEntity it) || it.level().isClientSide()) return;
        if (it.getY() > it.level().getMinBuildHeight() - 8 || !it.getItem().is(TAG)) return;
        ServerLevel l = (ServerLevel) it.level();
        var owner = it.getOwner();
        if (owner != null && owner.level() == l) {
            it.teleportTo(owner.getX(), owner.getY() + 1, owner.getZ());
        } else {
            BlockPos s = l.getSharedSpawnPos();
            int y = l.getHeight(Heightmap.Types.MOTION_BLOCKING, s.getX(), s.getZ());
            if (y <= l.getMinBuildHeight()) y = Math.max(l.getMinBuildHeight() + 1, s.getY());   // a void level: the spawn height
            it.teleportTo(s.getX() + 0.5, y + 1, s.getZ() + 0.5);
        }
        it.setDeltaMovement(0, 0, 0);
        it.setNoGravity(true);      // hovers where it came back, so it cannot fall again
    }

    private Irreplaceable() {}
}
