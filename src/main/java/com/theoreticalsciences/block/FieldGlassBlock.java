package com.theoreticalsciences.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

/** Translucent, connected-looking glass (culls faces against itself). Glows brighter while part of a formed multiblock. */
public class FieldGlassBlock extends TransparentBlock {
    public static final MapCodec<FieldGlassBlock> CODEC = simpleCodec(FieldGlassBlock::new);

    public FieldGlassBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(CasingBlock.FORMED, false));
    }

    @Override
    protected MapCodec<? extends TransparentBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CasingBlock.FORMED);
    }
}
