package com.theoreticalsciences.endgame;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.joml.Vector3f;

import java.util.List;

/** The Ex Nihilo Engine: energy from nothing, without end. */
public class ExNihiloEngineBlock extends BaseEntityBlock {
    public static final com.mojang.serialization.MapCodec<ExNihiloEngineBlock> CODEC = simpleCodec(ExNihiloEngineBlock::new);
    private static final com.theoreticalsciences.registry.QHGlowOptions GOLD = com.theoreticalsciences.registry.QHGlowOptions.of(1F, 0.88F, 0.45F, 0.9F);

    public ExNihiloEngineBlock(Properties p) {
        super(p.lightLevel(s -> 15));
    }

    @Override
    protected com.mojang.serialization.MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState s) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ExNihiloEngineBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, EndgameRegistry.EX_NIHILO.get(), (l, p, s, be) -> be.serverTick());
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide)
            player.displayClientMessage(Component.translatable("message.theoreticalsciences.ex_nihilo").withStyle(ChatFormatting.GOLD), true);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource r) {
        double cx = pos.getX() + 0.5, cy = pos.getY() + 0.5, cz = pos.getZ() + 0.5;
        // light condensing out of nothing onto the engine
        for (int i = 0; i < 2; i++) {
            double ox = (r.nextDouble() - 0.5) * 3, oy = (r.nextDouble() - 0.5) * 3, oz = (r.nextDouble() - 0.5) * 3;
            level.addParticle(com.theoreticalsciences.registry.QHParticles.PHOTON.get(), cx + ox, cy + oy, cz + oz, -ox * 0.06, -oy * 0.06, -oz * 0.06);
        }
        if (r.nextInt(3) == 0) level.addParticle(GOLD, cx + (r.nextDouble() - 0.5) * 1.1, cy + 0.6, cz + (r.nextDouble() - 0.5) * 1.1, 0, 0.02, 0);
        if (r.nextInt(8) == 0) level.addParticle(com.theoreticalsciences.registry.QHParticles.ARC_SPARK.get(), cx, cy + 0.55, cz, 0, 0.05, 0);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("block.theoreticalsciences.ex_nihilo_engine.lore").withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.ITALIC));
        tooltip.add(Component.translatable("tooltip.theoreticalsciences.ex_nihilo_engine").withStyle(ChatFormatting.GOLD));
    }
}
