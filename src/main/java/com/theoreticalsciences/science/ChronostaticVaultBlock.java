package com.theoreticalsciences.science;

import com.mojang.serialization.MapCodec;
import com.theoreticalsciences.dungeon.CacheBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** 1.2.0: the Chronostatic Vault - a cache that stops radioactive sources from decaying (see {@link ChronostaticVaultBlockEntity}). */
public class ChronostaticVaultBlock extends CacheBlock {
    public static final MapCodec<ChronostaticVaultBlock> CODEC = simpleCodec(ChronostaticVaultBlock::new);

    public ChronostaticVaultBlock(Properties p) {
        super(p);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ChronostaticVaultBlockEntity(pos, state);
    }
}
