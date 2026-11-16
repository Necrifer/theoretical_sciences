package com.theoreticalsciences.archaeology;

import com.theoreticalsciences.TheoreticalSciences;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * 1.6.0: the Survey Radar, a ground-penetrating radar on a handle. Swept over the ground it sends a pulse down and
 * listens for echoes from anything that is not soil: the finds in a Buried Tell, fossils and amber at a dig site. Each
 * echo glows for the user where it came from, and the nearest is reported (how far, which way, how deep). A pulse every
 * three seconds; each costs one point of wear. Its reach is 16 blocks, as far as a radar sees into damp ground.
 */
public class SurveyRadarItem extends Item {
    static final TagKey<Block> TARGETS = TagKey.create(Registries.BLOCK, TheoreticalSciences.id("survey_targets"));
    static final int REACH = 16;

    public SurveyRadarItem(Properties p) {
        super(p);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(level instanceof ServerLevel sl) || !(player instanceof ServerPlayer sp)) return InteractionResultHolder.success(stack);
        BlockPos at = player.blockPosition();
        List<BlockPos> hits = new ArrayList<>();
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int dx = -REACH; dx <= REACH; dx++)
            for (int dz = -REACH; dz <= REACH; dz++)
                for (int dy = -REACH; dy <= 4; dy++) {
                    m.set(at.getX() + dx, at.getY() + dy, at.getZ() + dz);
                    if (!sl.isLoaded(m)) continue;
                    if (sl.getBlockState(m).is(TARGETS)) hits.add(m.immutable());
                }
        level.playSound(null, at, com.theoreticalsciences.registry.QHSounds.PULSE.get(), SoundSource.PLAYERS, 0.8F, 1.4F);
        player.getCooldowns().addCooldown(this, 60);
        stack.hurtAndBreak(1, player, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        if (hits.isEmpty()) {
            sp.displayClientMessage(Component.translatable("message.theoreticalsciences.radar_quiet").withStyle(ChatFormatting.GRAY), true);
            return InteractionResultHolder.consume(stack);
        }
        BlockPos near = hits.get(0);
        for (BlockPos h : hits) if (h.distSqr(at) < near.distSqr(at)) near = h;
        DustParticleOptions glow = new DustParticleOptions(new Vector3f(0.45F, 1.0F, 0.7F), 1.4F);
        for (BlockPos h : hits)
            sl.sendParticles(sp, glow, true, h.getX() + 0.5, h.getY() + 0.5, h.getZ() + 0.5, 12, 0.3, 0.3, 0.3, 0.0);
        double dist = Math.sqrt(near.distSqr(at));
        int depth = Math.max(0, at.getY() - near.getY());
        sp.displayClientMessage(Component.translatable("message.theoreticalsciences.radar_echo", hits.size(),
                String.format(java.util.Locale.ROOT, "%.1f", dist), compass(near.getX() - at.getX(), near.getZ() - at.getZ()), depth)
                .withStyle(ChatFormatting.GREEN), true);
        return InteractionResultHolder.consume(stack);
    }

    private static Component compass(int dx, int dz) {
        if (dx == 0 && dz == 0) return Component.translatable("message.theoreticalsciences.radar_below");
        double a = Math.toDegrees(Math.atan2(dx, -dz));
        String[] k = {"n", "ne", "e", "se", "s", "sw", "w", "nw"};
        return Component.translatable("message.theoreticalsciences.radar_dir." + k[(int) Math.floorMod(Math.round(a / 45), 8)]);
    }
}
