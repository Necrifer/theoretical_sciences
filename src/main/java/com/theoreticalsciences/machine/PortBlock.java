package com.theoreticalsciences.machine;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import javax.annotation.Nullable;

/** Multiblock I/O block: exposes the controller's items, fluids and energy on all six faces. */
public class PortBlock extends Block implements EntityBlock {
    public static final MapCodec<PortBlock> CODEC = simpleCodec(PortBlock::new);
    public static final BooleanProperty LINKED = BooleanProperty.create("linked");

    public PortBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(LINKED, false));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LINKED);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PortBlockEntity(pos, state);
    }

    public static void setLinked(ServerLevel level, BlockPos pos, @Nullable BlockPos controller) {
        if (!level.isLoaded(pos)) return;          // round 36: a port in an unloaded chunk checks its controller itself when it loads
        if (level.getBlockEntity(pos) instanceof PortBlockEntity be) {
            be.setController(controller);
            BlockState st = level.getBlockState(pos);
            if (st.hasProperty(LINKED) && st.getValue(LINKED) != (controller != null))
                level.setBlock(pos, st.setValue(LINKED, controller != null), 3);
            level.invalidateCapabilities(pos);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof PortBlockEntity be && player instanceof ServerPlayer sp) {
            MachineBlockEntity c = be.controller();
            if (c != null) {
                sp.openMenu(c, buf -> buf.writeBlockPos(c.getBlockPos()));
                return InteractionResult.CONSUME;
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
