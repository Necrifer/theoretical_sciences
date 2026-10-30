package com.theoreticalsciences.transcend;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A display pedestal holding a working miniature: a black hole, a pocket universe, a tesseract... The pedestal is an
 * ordinary model; the thing on it is drawn live by the effect renderer (see EffectRenderer#drawDecor).
 */
public class DecorBlock extends BaseEntityBlock {
    public enum Kind {
        BLACK_HOLE(true), WHITE_HOLE(true), WORMHOLE(true), POCKET_UNIVERSE(false), TESSERACT(false), PULSAR(true),
        GALAXY(false), QUASAR(false), DYSON_SPHERE(false), PENTERACT(false);

        public final boolean lensing;

        Kind(boolean lensing) {
            this.lensing = lensing;
        }

        public String id() {
            return "display_" + name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 4, 15);
    public final Kind kind;

    public DecorBlock(Properties p, Kind kind) {
        super(p);
        this.kind = kind;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(p -> new DecorBlock(p, kind));
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
        return SHAPE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DecorBlockEntity(pos, state);
    }
}
