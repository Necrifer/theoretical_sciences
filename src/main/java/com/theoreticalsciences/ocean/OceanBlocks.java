package com.theoreticalsciences.ocean;

import com.mojang.serialization.MapCodec;
import com.theoreticalsciences.registry.QHBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** 1.2.0: the blocks of Titan's hidden ocean that do something: the black smoker, and the life that grows round it. */
public final class OceanBlocks {
    private OceanBlocks() {}

    /**
     * A black smoker: the mouth of a hydrothermal vent. Water at 400 C pours out of it, black with metal sulfides that
     * crystallise the moment it meets the cold sea: a column of dark "smoke" rises from it, bubbling and roaring, and
     * whoever swims into the plume is lifted and scalded (OceanRegistry.plume). The grazers and crabs gather round it.
     */
    public static class BlackSmoker extends Block {
        public BlackSmoker(Properties p) {
            super(p);
        }

        @Override
        public void animateTick(BlockState st, Level l, BlockPos pos, RandomSource r) {
            double x = pos.getX() + 0.5, y = pos.getY() + 1.0, z = pos.getZ() + 0.5;
            for (int i = 0; i < 4; i++)
                l.addAlwaysVisibleParticle(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, true, x + r.nextGaussian() * 0.15, y + r.nextDouble() * 0.5,
                        z + r.nextGaussian() * 0.15, r.nextGaussian() * 0.01, 0.07 + r.nextDouble() * 0.05, r.nextGaussian() * 0.01);
            for (int i = 0; i < 3; i++)
                l.addParticle(ParticleTypes.BUBBLE_COLUMN_UP, x + r.nextGaussian() * 0.2, y, z + r.nextGaussian() * 0.2, 0, 0.3, 0);
            if (r.nextInt(3) == 0) l.addParticle(ParticleTypes.LAVA, x, y, z, 0, 0, 0);
            if (r.nextInt(20) == 0)
                l.playLocalSound(x, y, z, SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT, SoundSource.BLOCKS, 0.8F, 0.6F + r.nextFloat() * 0.3F, false);
            if (r.nextInt(60) == 0) l.playLocalSound(x, y, z, SoundEvents.LAVA_AMBIENT, SoundSource.BLOCKS, 0.6F, 0.5F, false);
        }
    }

    /**
     * Life that grows under water and holds its own water (like seagrass): Giant Tube Worms on the floor by the vents,
     * and Brinicles hanging from the ice. Tube worms spread to the floor beside them near a vent; brinicles grow downward.
     */
    public static class Growth extends Block implements LiquidBlockContainer {
        final boolean hanging;
        static final VoxelShape FLOOR = Block.box(3, 0, 3, 13, 14, 13), HANG = Block.box(4, 0, 4, 12, 16, 12);

        public Growth(Properties p, boolean hanging) {
            super(p);
            this.hanging = hanging;
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return simpleCodec(pp -> new Growth(pp, hanging));
        }

        @Override
        protected VoxelShape getShape(BlockState st, BlockGetter l, BlockPos pos, CollisionContext c) {
            return hanging ? HANG : FLOOR;
        }

        @Override
        protected FluidState getFluidState(BlockState st) {
            return Fluids.WATER.getSource(false);
        }

        @Override
        public boolean canPlaceLiquid(Player p, BlockGetter l, BlockPos pos, BlockState st, Fluid f) {
            return false;
        }

        @Override
        public boolean placeLiquid(LevelAccessor l, BlockPos pos, BlockState st, FluidState f) {
            return false;
        }

        @Override
        protected boolean canSurvive(BlockState st, LevelReader l, BlockPos pos) {
            BlockPos on = hanging ? pos.above() : pos.below();
            BlockState s = l.getBlockState(on);
            return s.is(this) || s.isFaceSturdy(l, on, hanging ? Direction.DOWN : Direction.UP);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext ctx) {
            FluidState f = ctx.getLevel().getFluidState(ctx.getClickedPos());
            return f.is(net.minecraft.tags.FluidTags.WATER) && f.getAmount() == 8 ? defaultBlockState() : null;
        }

        @Override
        protected BlockState updateShape(BlockState st, Direction d, BlockState n, LevelAccessor l, BlockPos pos, BlockPos np) {
            if (!st.canSurvive(l, pos)) return net.minecraft.world.level.block.Blocks.WATER.defaultBlockState();
            l.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(l));
            return super.updateShape(st, d, n, l, pos, np);
        }

        @Override
        protected boolean isRandomlyTicking(BlockState st) {
            return true;
        }

        @Override
        protected void randomTick(BlockState st, ServerLevel l, BlockPos pos, RandomSource r) {
            if (hanging) {
                // a brinicle grows down as cold brine sinks from the ice, to four blocks long
                int len = 1;
                while (len < 6 && l.getBlockState(pos.above(len)).is(this)) len++;
                BlockPos below = pos.below();
                if (len < 4 && r.nextInt(8) == 0 && l.getBlockState(below).is(net.minecraft.world.level.block.Blocks.WATER)
                        && l.getFluidState(below).isSource()) l.setBlockAndUpdate(below, st);
                return;
            }
            // a tube worm colony spreads along the floor, but only within reach of a vent's warm water
            if (r.nextInt(6) != 0) return;
            var smoker = QHBlocks.get("black_smoker");
            boolean warm = false;
            for (BlockPos q : BlockPos.betweenClosed(pos.offset(-6, -3, -6), pos.offset(6, 6, 6)))
                if (l.getBlockState(q).is(smoker)) {
                    warm = true;
                    break;
                }
            if (!warm) return;
            BlockPos to = pos.offset(r.nextInt(3) - 1, 0, r.nextInt(3) - 1);
            if (l.getBlockState(to).is(net.minecraft.world.level.block.Blocks.WATER) && l.getFluidState(to).isSource() && st.canSurvive(l, to))
                l.setBlockAndUpdate(to, st);
        }
    }
}
