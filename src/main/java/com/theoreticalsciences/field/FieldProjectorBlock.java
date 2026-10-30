package com.theoreticalsciences.field;

import com.theoreticalsciences.registry.QHLogistics;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import org.joml.Vector3f;

/**
 * A Field Projector bends one physical property in a 33x33x33 cube around itself while it is powered:
 * gravity (graviton), the number of spatial dimensions (dimensional lift), the rate of time (chronal) or the vacuum
 * state (vacuum). Right-click to step its setting up, sneak-right-click to step it down. Multiblocks inside the field
 * can run recipes that need those conditions - and anyone walking in without protection feels them too.
 */
public class FieldProjectorBlock extends BaseEntityBlock {
    public static final com.mojang.serialization.MapCodec<FieldProjectorBlock> CODEC = simpleCodec(FieldProjectorBlock::new);

    public FieldProjectorBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(BlockStateProperties.POWERED, false));
    }

    @Override
    protected com.mojang.serialization.MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(BlockStateProperties.POWERED);
    }

    @Override
    protected RenderShape getRenderShape(BlockState s) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FieldProjectorBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, QHLogistics.FIELD_PROJECTOR.get(), (l, p, s, be) -> be.serverTick());
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof FieldProjectorBlockEntity be) {
            be.step(player.isShiftKeyDown() ? -1 : 1);
            player.displayClientMessage(be.describe().copy().withStyle(ChatFormatting.AQUA), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource r) {
        if (!state.getValue(BlockStateProperties.POWERED)) return;
        // the edge of the field shimmers
        int R = FieldEnvironment.RADIUS;
        for (int i = 0; i < 4; i++) {
            double x = pos.getX() + 0.5 + (r.nextBoolean() ? R : -R) * (r.nextInt(3) == 0 ? 1 : r.nextDouble() * 2 - 1);
            double z = pos.getZ() + 0.5 + (r.nextDouble() * 2 - 1) * R;
            double y = pos.getY() + 0.5 + (r.nextDouble() * 2 - 1) * 4;
            level.addParticle(com.theoreticalsciences.registry.QHGlowOptions.of(0.5F, 0.8F, 1F, 1F), x, y, z, 0, 0, 0);
        }
    }
}
