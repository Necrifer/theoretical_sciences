package com.theoreticalsciences.machine;

import com.mojang.serialization.MapCodec;
import com.theoreticalsciences.registry.QHBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.fluids.FluidUtil;
import javax.annotation.Nullable;

public class MachineBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    public final MachineType type;
    private final MapCodec<MachineBlock> codec;

    public MachineBlock(Properties props, MachineType type) {
        super(props);
        this.type = type;
        this.codec = simpleCodec(p -> new MachineBlock(p, type));
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(ACTIVE, false));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return codec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ACTIVE);
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

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return MachineBlockEntity.create(type, pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> beType) {
        if (beType != QHBlockEntities.machine(type)) return null;
        return level.isClientSide
                ? (l, p, s, be) -> MachineBlockEntity.clientTick(l, p, s, (MachineBlockEntity) be)
                : (l, p, s, be) -> MachineBlockEntity.serverTick(l, p, s, (MachineBlockEntity) be);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (placer instanceof Player p && level.getBlockEntity(pos) instanceof MachineBlockEntity be) be.setOwner(p);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        // tools that act on the machine itself: skip opening the GUI so the tool's own right-click runs
        if (stack.getItem() instanceof com.theoreticalsciences.item.StructureAssemblerItem
                || stack.getItem() instanceof com.theoreticalsciences.item.SpectralAnalyzerItem)
            return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        if (FluidUtil.getFluidHandler(stack).isPresent()) {
            if (FluidUtil.interactWithFluidHandler(player, hand, level, pos, hit.getDirection()))
                return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof MachineBlockEntity be && player instanceof ServerPlayer sp) {
            if (be.getOwner() == null) be.setOwner(player);
            if (be instanceof ControllerBlockEntity c && player.isShiftKeyDown()) {
                c.reportStructure(sp);
                return InteractionResult.CONSUME;
            }
            sp.openMenu(be, buf -> buf.writeBlockPos(pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** A machine holding more than can be dropped keeps it: its item carries the contents (placing it puts them back). */
    @Override
    protected java.util.List<ItemStack> getDrops(BlockState state, net.minecraft.world.level.storage.loot.LootParams.Builder params) {
        java.util.List<ItemStack> drops = new java.util.ArrayList<>(super.getDrops(state, params));
        BlockEntity be = params.getOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_ENTITY);
        if (be instanceof MachineBlockEntity m && m.keepsContentsInItem()) {
            ItemStack mine = null;
            for (ItemStack s : drops) if (s.is(asItem())) mine = s;
            if (mine == null) drops.add(mine = new ItemStack(this));
            net.minecraft.world.item.BlockItem.setBlockEntityData(mine, m.getType(), m.saveCustomOnly(params.getLevel().registryAccess()));
        }
        return drops;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        // creative breaking drops nothing - but a packed machine must not vanish with millions inside
        if (!level.isClientSide && player.isCreative() && level.getBlockEntity(pos) instanceof MachineBlockEntity m && m.keepsContentsInItem()) {
            ItemStack s = new ItemStack(this);
            net.minecraft.world.item.BlockItem.setBlockEntityData(s, m.getType(), m.saveCustomOnly(level.registryAccess()));
            net.minecraft.world.entity.item.ItemEntity e = new net.minecraft.world.entity.item.ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, s);
            e.setDefaultPickUpDelay();
            level.addFreshEntity(e);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock())) {
            if (level.getBlockEntity(pos) instanceof MachineBlockEntity be) {
                be.dropContents();
                // installed stabiliser / autonomous calibrator come back as items - unless the controller's own item
                // already carries them (then placing it again puts them back)
                if (be instanceof ControllerBlockEntity c && !be.keepsContentsInItem()) c.dropUpgrades();
                if (be instanceof ControllerBlockEntity c) c.onBroken();
            }
        }
        super.onRemove(state, level, pos, newState, moving);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof MachineBlockEntity be) return be.isActive() ? 15 : 0;
        return 0;
    }
}
