package com.theoreticalsciences.storage;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;

import java.util.List;

public class InfinityCacheBlock extends BaseEntityBlock {
    public static final MapCodec<InfinityCacheBlock> CODEC = simpleCodec(InfinityCacheBlock::new);
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;

    public InfinityCacheBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(OPEN, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(OPEN);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new InfinityCacheBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (level.getBlockEntity(pos) instanceof InfinityCacheBlockEntity be && player instanceof ServerPlayer sp) {
            sp.openMenu(be, buf -> buf.writeBlockPos(pos));
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        // creative breaking drops nothing: a full cache must not vanish
        if (!level.isClientSide && player.isCreative() && level.getBlockEntity(pos) instanceof InfinityCacheBlockEntity c && c.items.used() > 0) {
            ItemStack s = new ItemStack(this);
            BlockItem.setBlockEntityData(s, c.getType(), c.saveCustomOnly(level.registryAccess()));
            net.minecraft.world.entity.item.ItemEntity e = new net.minecraft.world.entity.item.ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, s);
            e.setDefaultPickUpDelay();
            level.addFreshEntity(e);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    /** It keeps what it holds: the dropped block carries its contents (placing it puts them back). */
    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        BlockEntity be = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        ItemStack s = new ItemStack(this);
        if (be instanceof InfinityCacheBlockEntity c && c.items.used() > 0)
            BlockItem.setBlockEntityData(s, c.getType(), c.saveCustomOnly(params.getLevel().registryAccess()));
        return List.of(s);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable("tooltip.theoreticalsciences.transcendent_cache").withStyle(net.minecraft.ChatFormatting.GRAY));
        var data = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (data != null && data.contains("Store")) {
            int n = data.copyTag().getCompound("Store").getList("Items", 10).size();
            tip.add(Component.translatable("tooltip.theoreticalsciences.transcendent_cache_holds", n).withStyle(net.minecraft.ChatFormatting.GOLD));
        }
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof InfinityCacheBlockEntity be ? be.comparator() : 0;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource r) {
        if (r.nextInt(state.getValue(OPEN) ? 1 : 4) == 0)
            level.addParticle(state.getValue(OPEN) ? com.theoreticalsciences.registry.QHParticles.PHOTON.get() : com.theoreticalsciences.registry.QHParticles.ARC_SPARK.get(),
                    pos.getX() + 0.2 + r.nextDouble() * 0.6, pos.getY() + 1.02, pos.getZ() + 0.2 + r.nextDouble() * 0.6, 0, 0.02, 0);
    }
}
