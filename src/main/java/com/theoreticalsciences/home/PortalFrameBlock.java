package com.theoreticalsciences.home;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The Homeward Portal Frame (1.8.0). It works as an End Portal frame does: twelve of them in a ring round a three by
 * three opening, each facing in, each given a Keystone of Return as an End Portal frame is given an Eye of Ender. When
 * the twelfth keystone is set the opening fills.
 */
public class PortalFrameBlock extends Block {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty KEYSTONE = BooleanProperty.create("keystone");
    private static final VoxelShape BASE = Block.box(0, 0, 0, 16, 13, 16);
    private static final VoxelShape FULL = Shapes.or(BASE, Block.box(4, 13, 4, 12, 16, 12));

    public PortalFrameBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(KEYSTONE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING, KEYSTONE);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return state.getValue(KEYSTONE) ? FULL : BASE;
    }

    @Override
    protected boolean useShapeForLightOcclusion(BlockState state) {
        return true;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rot) {
        return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return state.getValue(KEYSTONE) ? 15 : 0;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (state.getValue(KEYSTONE) || HomeMaterials.index(stack.getItem()) != HomeMaterials.IDS.size() - 1) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;
        level.setBlock(pos, state.setValue(KEYSTONE, true), 3);
        level.updateNeighbourForOutputSignal(pos, this);
        stack.consume(1, player);
        level.playSound(null, pos, SoundEvents.END_PORTAL_FRAME_FILL, SoundSource.BLOCKS, 1F, 1.3F);
        ((ServerLevel) level).sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 16, 0.2, 0.2, 0.2, 0.02);
        tryOpen((ServerLevel) level, pos, state.getValue(FACING));
        return ItemInteractionResult.SUCCESS;
    }

    /** The frame at {@code pos} faces the opening: its centre is two blocks in, and up to one to either side. */
    private static void tryOpen(ServerLevel level, BlockPos pos, Direction facing) {
        Direction side = facing.getClockWise();
        for (int k = -1; k <= 1; k++) {
            BlockPos centre = pos.relative(facing, 2).relative(side, k);
            if (!ringComplete(level, centre)) continue;
            for (int dx = -1; dx <= 1; dx++)
                for (int dz = -1; dz <= 1; dz++) level.setBlock(centre.offset(dx, 0, dz), HomeRegistry.PORTAL.get().defaultBlockState(), 2);
            level.globalLevelEvent(1038, centre, 0);                       // the End Portal's opening sound, heard everywhere
            level.sendParticles(ParticleTypes.END_ROD, centre.getX() + 0.5, centre.getY() + 1.5, centre.getZ() + 0.5, 240, 1.4, 1.0, 1.4, 0.08);
            return;
        }
    }

    private static boolean ringComplete(Level level, BlockPos centre) {
        for (Direction d : Direction.Plane.HORIZONTAL)
            for (int o = -1; o <= 1; o++) {
                BlockState s = level.getBlockState(centre.relative(d, 2).relative(d.getClockWise(), o));
                if (!(s.getBlock() instanceof PortalFrameBlock) || !s.getValue(KEYSTONE) || s.getValue(FACING) != d.getOpposite()) return false;
            }
        return true;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moved) {
        if (!state.is(next.getBlock()) && !level.isClientSide) {
            if (state.getValue(KEYSTONE)) Block.popResource(level, pos, new ItemStack(HomeMaterials.item(HomeMaterials.IDS.size() - 1)));
            // a broken ring closes: the opening this frame faced empties
            Direction f = state.getValue(FACING);
            for (int k = -1; k <= 1; k++) {
                BlockPos centre = pos.relative(f, 2).relative(f.getClockWise(), k);
                if (!level.getBlockState(centre).is(HomeRegistry.PORTAL.get())) continue;
                for (int dx = -1; dx <= 1; dx++)
                    for (int dz = -1; dz <= 1; dz++)
                        if (level.getBlockState(centre.offset(dx, 0, dz)).is(HomeRegistry.PORTAL.get())) level.removeBlock(centre.offset(dx, 0, dz), false);
            }
        }
        super.onRemove(state, level, pos, next, moved);
    }
}
