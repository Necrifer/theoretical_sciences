package com.theoreticalsciences.machine;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import javax.annotation.Nullable;

/**
 * 1.7.4: the Mana Port. It takes the place of a structure's block wherever an Interface Port may, holds Botania mana,
 * and takes it from Mana Spreaders' bursts (aim a spreader at it) or from a Spark; the structure's recipes that ask
 * for mana draw on it. Use it to read how much it holds. Without Botania it is an inert block.
 */
public class ManaPortBlock extends Block implements EntityBlock {
    public static final MapCodec<ManaPortBlock> CODEC = simpleCodec(ManaPortBlock::new);

    public ManaPortBlock(Properties props) {
        super(props);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ManaPortBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof ManaPortBlockEntity be)
            player.displayClientMessage(Component.translatable("message.theoreticalsciences.mana_port",
                    String.format(java.util.Locale.ROOT, "%,d", be.mana()), String.format(java.util.Locale.ROOT, "%,d", ManaPortBlockEntity.CAPACITY)), true);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof ManaPortBlockEntity be ? (int) Math.ceil(14.0 * be.mana() / ManaPortBlockEntity.CAPACITY) + (be.mana() > 0 ? 1 : 0) : 0;
    }
}
