package com.theoreticalsciences.engineering;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * 1.4.0: a Sacrificial Anode. Aluminium is less noble than iron: wired to a structure in the same wet or hostile
 * surroundings, it corrodes in the structure's place. Here the Rust Mites that eat machines' charge go for an anode
 * first, if there is one within twelve blocks, and eat it away a stage at a time. Its {@code charge} counts down from
 * 4; at 0 it is spent (pitted grey, it no longer draws the mites) and drops Rust Flakes.
 */
public class SacrificialAnodeBlock extends Block {
    public static final MapCodec<SacrificialAnodeBlock> CODEC = simpleCodec(SacrificialAnodeBlock::new);
    public static final IntegerProperty CHARGE = IntegerProperty.create("charge", 0, 4);

    public SacrificialAnodeBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(CHARGE, 4));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(CHARGE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState();
    }

    /** Whether this anode still draws the mites. */
    public static boolean live(BlockState s) {
        return s.getBlock() instanceof SacrificialAnodeBlock && s.getValue(CHARGE) > 0;
    }

    /** One bite: the anode loses a stage. Returns true if there was anything left to eat. */
    public static boolean corrode(ServerLevel level, BlockPos pos) {
        BlockState s = level.getBlockState(pos);
        if (!live(s)) return false;
        int c = s.getValue(CHARGE) - 1;
        level.setBlock(pos, s.setValue(CHARGE, c), 3);
        level.playSound(null, pos, SoundEvents.AXE_SCRAPE, SoundSource.BLOCKS, 0.6F, 0.7F + c * 0.1F);
        if (c == 0) level.playSound(null, pos, SoundEvents.COPPER_BREAK, SoundSource.BLOCKS, 0.8F, 0.6F);
        return true;
    }

    /** What it gives back when broken: itself while any of it is left; a spent one, only its corrosion. */
    public static ItemStack drop(BlockState s) {
        return s.getValue(CHARGE) > 0 ? new ItemStack(s.getBlock()) : new ItemStack(com.theoreticalsciences.registry.QHItems.get("rust_flakes"));
    }
}
