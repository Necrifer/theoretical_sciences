package com.theoreticalsciences.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * Structural block for multiblocks. When the structure it belongs to is complete, the controller flips
 * {@link #FORMED} on every casing so the whole machine "transforms" into a seamless, energised shell
 * (MMCR-style: the blocks stay real blocks, only their look changes, so nothing can desync or vanish).
 */
public class CasingBlock extends Block {
    public static final MapCodec<CasingBlock> CODEC = simpleCodec(CasingBlock::new);
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");

    public CasingBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(FORMED, false));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FORMED);
    }
}
