package com.theoreticalsciences.transcend;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;

/**
 * Round 27: the Omnipotence Core, forged anew at the reborn Core - the last thing in the mod. Nothing can break it:
 * not a pickaxe, not an explosion, not a piston. Only the Hand of the Demiurge can tune it, wake it and carry it.
 * Awake, using it opens the Core's own screen (Materialize and the Forge), wherever it stands.
 */
public class ForgedCoreBlock extends BaseEntityBlock {
    public static final MapCodec<ForgedCoreBlock> CODEC = simpleCodec(ForgedCoreBlock::new);
    /** Round 34c: awake or dormant, for the block's look (the block entity holds the truth). */
    public static final net.minecraft.world.level.block.state.properties.BooleanProperty ACTIVE =
            net.minecraft.world.level.block.state.properties.BooleanProperty.create("active");

    public ForgedCoreBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(ACTIVE, false));
    }

    @Override
    protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> b) {
        b.add(ACTIVE);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                                                          net.minecraft.world.level.block.entity.BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, TranscendRegistry.FORGED_CORE_BE.get(), ForgedCoreBlockEntity::serverTick);
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
        return new ForgedCoreBlockEntity(pos, state);
    }

    /** The Hand of the Demiurge works on the Core instead of opening it. */
    @Override
    protected net.minecraft.world.ItemInteractionResult useItemOn(net.minecraft.world.item.ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                                                 net.minecraft.world.InteractionHand hand, BlockHitResult hit) {
        if (stack.getItem() instanceof com.theoreticalsciences.endgame.DemiurgeHandItem) return net.minecraft.world.ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player.getOffhandItem().getItem() instanceof com.theoreticalsciences.endgame.DemiurgeHandItem) return InteractionResult.PASS;   // the Hand in the off hand
        if (!(player instanceof ServerPlayer sp) || !(level.getBlockEntity(pos) instanceof ForgedCoreBlockEntity core))
            return InteractionResult.sidedSuccess(level.isClientSide);
        if (core.active()) CorePayloads.openForged(sp, pos);
        else sp.displayClientMessage(Component.translatable("message.theoreticalsciences.forged_core.dormant").withStyle(ChatFormatting.GRAY), true);
        return InteractionResult.CONSUME;
    }

    /** True if this block is a forged Core that is awake. */
    public static boolean awake(Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof ForgedCoreBlockEntity c && c.active();
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource r) {
        boolean on = awake(level, pos);
        // awake: the same orbiting light as the Transcendental's Core; dormant: a faint, slow glimmer
        for (int i = 0; i < (on ? 3 : 1); i++) {
            double a = r.nextDouble() * Math.PI * 2, rad = on ? 1.5 + r.nextDouble() : 0.7 + r.nextDouble() * 0.3;
            if (on)
                level.addParticle(com.theoreticalsciences.registry.QHParticles.PHOTON.get(), pos.getX() + 0.5 + Math.cos(a) * rad, pos.getY() + 0.5 + r.nextGaussian() * 0.5,
                        pos.getZ() + 0.5 + Math.sin(a) * rad, -Math.cos(a) * 0.08, 0, -Math.sin(a) * 0.08);
            else if (r.nextInt(3) == 0)
                level.addParticle(com.theoreticalsciences.registry.QHParticles.GLYPH.get(), pos.getX() + 0.5 + Math.cos(a) * rad, pos.getY() + 0.6, pos.getZ() + 0.5 + Math.sin(a) * rad,
                        -Math.cos(a) * 0.3, 0.2, -Math.sin(a) * 0.3);
        }
    }
}
