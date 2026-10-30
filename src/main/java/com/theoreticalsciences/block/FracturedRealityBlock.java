package com.theoreticalsciences.block;

import com.mojang.serialization.MapCodec;
import com.theoreticalsciences.registry.QHBlockEntities;
import com.theoreticalsciences.registry.QHBlocks;
import com.theoreticalsciences.registry.QHEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;

import javax.annotation.Nullable;
import java.util.List;

/**
 * A block of the world that reality destabilisation has fractured. It still remembers what it was, and is drawn
 * as that block splitting into smaller fractal copies of itself that glitch and fade in and out, like a higher-
 * dimensional object passing through our space. The fracture spreads a limited distance, then reality heals and
 * the original block returns. Mining one gives back the original block's drops.
 */
public class FracturedRealityBlock extends BaseEntityBlock {
    public static final MapCodec<FracturedRealityBlock> CODEC = simpleCodec(FracturedRealityBlock::new);

    public FracturedRealityBlock(Properties props) {
        super(props.noOcclusion().isViewBlocking((s, l, p) -> false).isSuffocating((s, l, p) -> false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;      // drawn entirely by the fractal block-entity renderer
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FracturedRealityBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, QHBlockEntities.FRACTURED_REALITY.get(), FracturedRealityBlockEntity::serverTick);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (!level.isClientSide && entity instanceof LivingEntity le && level.getGameTime() % 20 == 0)
            le.addEffect(new MobEffectInstance(QHEffects.REALITY_DESTABILISATION, 120, 0));
        super.stepOn(level, pos, state, entity);
    }

    @Override
    protected List<net.minecraft.world.item.ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        // you get back what the block used to be, plus sometimes a splinter of the fracture itself
        List<net.minecraft.world.item.ItemStack> drops = new java.util.ArrayList<>(super.getDrops(state, params));
        BlockEntity be = params.getOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_ENTITY);
        if (be instanceof FracturedRealityBlockEntity f && !f.original().isAir())
            drops.addAll(f.original().getDrops(params));
        return drops;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource r) {
        if (r.nextInt(5) == 0)
            level.addParticle(com.theoreticalsciences.registry.QHParticles.PHASE_MOTE.get(), pos.getX() + r.nextDouble(), pos.getY() + r.nextDouble(), pos.getZ() + r.nextDouble(), 0, 0, 0);
    }

    /** Can this block be fractured? Never air, liquids, unbreakables, or anything with a block entity (chests, machines...). */
    public static boolean fracturable(ServerLevel level, BlockPos p) {
        BlockState st = level.getBlockState(p);
        return !st.isAir() && st.getFluidState().isEmpty() && !st.hasBlockEntity() && st.getDestroySpeed(level, p) >= 0
                && !st.is(QHBlocks.get("fractured_reality")) && level.getBlockEntity(p) == null;
    }

    public static void fracture(ServerLevel level, BlockPos p, int generation, int maxGeneration) {
        BlockState original = level.getBlockState(p);
        level.setBlock(p, QHBlocks.get("fractured_reality").defaultBlockState(), 3);
        if (level.getBlockEntity(p) instanceof FracturedRealityBlockEntity f) f.init(original, generation, maxGeneration);
    }

    /**
     * Seeds the fracture around a failed machine. Fractures are not destruction - every fractured block heals back
     * into what it was - so this happens whether or not containment failures may destroy blocks. Candidates are
     * gathered from the whole surrounding volume (machines are mostly hollow, and the blast has just cleared their
     * heart), then a spread of them is fractured; the fracture creeps on from there by itself.
     */
    public static void seed(ServerLevel level, BlockPos c, int severity, RandomSource r) {
        int seeds = Math.max(6, severity * 2), radius = Math.max(4, severity), maxGen = Math.min(4, 1 + severity / 3);
        java.util.List<BlockPos> cand = new java.util.ArrayList<>();
        for (BlockPos p : BlockPos.betweenClosed(c.offset(-radius, -radius / 2, -radius), c.offset(radius, radius, radius)))
            if (p.distSqr(c) <= (double) radius * radius && fracturable(level, p)) cand.add(p.immutable());
        java.util.Collections.shuffle(cand, new java.util.Random(r.nextLong()));
        // prefer blocks that touch air: the fracture shows up where you can see it
        cand.sort(java.util.Comparator.comparingInt(p -> exposed(level, p) ? 0 : 1));
        for (int i = 0; i < Math.min(seeds, cand.size()); i++) fracture(level, cand.get(i), 0, maxGen);
    }

    private static boolean exposed(ServerLevel level, BlockPos p) {
        for (net.minecraft.core.Direction d : net.minecraft.core.Direction.values()) if (level.getBlockState(p.relative(d)).isAir()) return true;
        return false;
    }

    @Override
    public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player, boolean willHarvest, net.minecraft.world.level.material.FluidState fluid) {
        if (!level.isClientSide)
            ((ServerLevel) level).sendParticles(com.theoreticalsciences.registry.QHParticles.PHASE_MOTE.get(), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 30, 0.4, 0.4, 0.4, 0.2);
        return super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid);
    }
}
