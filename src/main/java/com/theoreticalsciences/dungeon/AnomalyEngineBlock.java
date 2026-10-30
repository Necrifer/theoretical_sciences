package com.theoreticalsciences.dungeon;

import com.mojang.serialization.MapCodec;
import com.theoreticalsciences.registry.QHLogistics;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/** A breached anomaly engine found in ruins: it keeps pulling this dimension's creatures into being until it is broken. */
public class AnomalyEngineBlock extends BaseEntityBlock {
    public static final MapCodec<AnomalyEngineBlock> CODEC = simpleCodec(AnomalyEngineBlock::new);

    public AnomalyEngineBlock(Properties p) {
        super(p);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AnomalyEngineBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, QHLogistics.ANOMALY_ENGINE.get(), AnomalyEngineBlockEntity::serverTick);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable net.minecraft.world.entity.LivingEntity by, net.minecraft.world.item.ItemStack stack) {
        // placed by hand: a built engine (the mod's spawner), not a wild one
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof AnomalyEngineBlockEntity be)
            be.setBuilt(by instanceof net.minecraft.world.entity.player.Player p ? p.getUUID() : null);
    }

    @Override
    protected net.minecraft.world.InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, net.minecraft.world.entity.player.Player player,
                                                                  net.minecraft.world.phys.BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof AnomalyEngineBlockEntity be) || !be.isBuilt()) return net.minecraft.world.InteractionResult.PASS;
        if (level.isClientSide) return net.minecraft.world.InteractionResult.SUCCESS;
        if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
            if (!sp.isCreative() && !com.theoreticalsciences.research.ResearchService.has(sp, AnomalyEngineBlockEntity.RESEARCH)) {
                sp.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.theoreticalsciences.engine_research")
                        .withStyle(net.minecraft.ChatFormatting.RED), true);
                return net.minecraft.world.InteractionResult.CONSUME;
            }
            sp.openMenu(be, buf -> buf.writeBlockPos(pos));
        }
        return net.minecraft.world.InteractionResult.CONSUME;
    }

    /** Broken by hand without a good enough pick: a built engine must still not vanish with what is in it. */
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, net.minecraft.world.entity.player.Player player) {
        if (!level.isClientSide && !player.isCreative() && !player.hasCorrectToolForDrops(state, level, pos)
                && level.getBlockEntity(pos) instanceof AnomalyEngineBlockEntity a && a.isBuilt()) {
            net.minecraft.world.item.ItemStack s = new net.minecraft.world.item.ItemStack(this);
            net.minecraft.world.item.BlockItem.setBlockEntityData(s, a.getType(), a.saveCustomOnly(level.registryAccess()));
            popResource(level, pos, s);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    /** A built engine drops itself (keeping what is in it); one found in ruins drops salvage (its loot table). */
    @Override
    protected java.util.List<net.minecraft.world.item.ItemStack> getDrops(BlockState state, net.minecraft.world.level.storage.loot.LootParams.Builder params) {
        var be = params.getOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_ENTITY);
        if (be instanceof AnomalyEngineBlockEntity a && a.isBuilt()) {
            net.minecraft.world.item.ItemStack s = new net.minecraft.world.item.ItemStack(this);
            net.minecraft.world.item.BlockItem.setBlockEntityData(s, a.getType(), a.saveCustomOnly(params.getLevel().registryAccess()));
            return java.util.List.of(s);
        }
        return super.getDrops(state, params);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource r) {
        // it leaks: sparks and torn space around the casing
        for (int i = 0; i < 2; i++)
            level.addParticle(r.nextBoolean() ? com.theoreticalsciences.registry.QHParticles.PHASE_MOTE.get() : com.theoreticalsciences.registry.QHParticles.ARC_SPARK.get(),
                    pos.getX() + r.nextDouble(), pos.getY() + r.nextDouble() * 1.2, pos.getZ() + r.nextDouble(), 0, 0.02, 0);
    }
}
