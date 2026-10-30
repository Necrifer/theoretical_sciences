package com.theoreticalsciences.chrono;

import com.theoreticalsciences.registry.QHLogistics;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Chronal Accelerator: projects a field in which time runs faster. Right-click to change the speed, sneak-right-click
 * to change the size of the field; a redstone signal switches it off. Hungry: every extra tick it gives anything in the
 * field costs energy and Chronon Gas (see {@link ChronalAcceleratorBlockEntity}).
 */
public class ChronalAcceleratorBlock extends BaseEntityBlock {
    public static final com.mojang.serialization.MapCodec<ChronalAcceleratorBlock> CODEC = simpleCodec(ChronalAcceleratorBlock::new);
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public ChronalAcceleratorBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(LIT, false));
    }

    @Override
    protected com.mojang.serialization.MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(LIT);
    }

    @Override
    protected RenderShape getRenderShape(BlockState s) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ChronalAcceleratorBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, QHLogistics.CHRONAL_ACCELERATOR.get(), (l, p, s, be) -> be.serverTick());
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof ChronalAcceleratorBlockEntity be) be.use(player, player.isShiftKeyDown());
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource r) {
        if (!state.getValue(LIT)) return;
        for (int i = 0; i < 4; i++)
            level.addParticle(com.theoreticalsciences.registry.QHParticles.PHASE_MOTE.get(), pos.getX() + r.nextDouble(), pos.getY() + 1.05, pos.getZ() + r.nextDouble(),
                    (r.nextDouble() - 0.5) * 0.4, 0.15 + r.nextDouble() * 0.2, (r.nextDouble() - 0.5) * 0.4);
        if (r.nextInt(3) == 0)
            level.addParticle(com.theoreticalsciences.registry.QHParticles.PHOTON.get(), pos.getX() + 0.5 + (r.nextDouble() - 0.5) * 6, pos.getY() + 0.5 + (r.nextDouble() - 0.5) * 6,
                    pos.getZ() + 0.5 + (r.nextDouble() - 0.5) * 6, 0, 0, 0);
    }

    /** Round 29: breaking it keeps the stored energy and Chronon Gas in the dropped item (see KeepContents). */
    @Override
    protected java.util.List<net.minecraft.world.item.ItemStack> getDrops(BlockState state, net.minecraft.world.level.storage.loot.LootParams.Builder params) {
        return com.theoreticalsciences.block.KeepContents.drops(this, super.getDrops(state, params), params, ChronalAcceleratorBlockEntity.class, ChronalAcceleratorBlockEntity::keptData);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        com.theoreticalsciences.block.KeepContents.creative(this, level, pos, player, ChronalAcceleratorBlockEntity.class, ChronalAcceleratorBlockEntity::keptData);
        return super.playerWillDestroy(level, pos, state, player);
    }
}
