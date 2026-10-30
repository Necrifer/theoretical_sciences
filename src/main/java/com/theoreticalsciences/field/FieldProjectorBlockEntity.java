package com.theoreticalsciences.field;

import com.theoreticalsciences.machine.QHEnergyStorage;
import com.theoreticalsciences.registry.QHLogistics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class FieldProjectorBlockEntity extends BlockEntity {
    public final QHEnergyStorage energy = new QHEnergyStorage(400_000_000L, Integer.MAX_VALUE, 0, this::setChanged);
    private int level;
    private boolean on;

    public FieldProjectorBlockEntity(BlockPos pos, BlockState state) {
        super(QHLogistics.FIELD_PROJECTOR.get(), pos, state);
    }

    public FieldEnvironment.Kind kind() {
        String id = BuiltInRegistries.BLOCK.getKey(getBlockState().getBlock()).getPath();
        return id.startsWith("graviton") ? FieldEnvironment.Kind.GRAVITY : id.startsWith("dimensional") ? FieldEnvironment.Kind.DIMENSION
                : id.startsWith("chronal") ? FieldEnvironment.Kind.CHRONAL : FieldEnvironment.Kind.VACUUM;
    }

    public double value() {
        double[] v = FieldEnvironment.VALUES[kind().ordinal()];
        return v[Math.min(level, v.length - 1)];
    }

    public long cost() {
        long[] c = FieldEnvironment.COST[kind().ordinal()];
        return c[Math.min(level, c.length - 1)];
    }

    public boolean powered() {
        return on;
    }

    public void step(int d) {
        int n = FieldEnvironment.VALUES[kind().ordinal()].length;
        level = Math.floorMod(level + d, n);
        setChanged();
    }

    public Component describe() {
        String v = FieldEnvironment.VALUES[kind().ordinal()].length > 0 ? Environment.num(value()) : "";
        return Component.translatable("field.theoreticalsciences." + kind().name().toLowerCase(java.util.Locale.ROOT), v,
                String.format("%,d", cost()), on ? Component.translatable("field.theoreticalsciences.on") : Component.translatable("field.theoreticalsciences.off"));
    }

    void serverTick() {
        long c = cost();
        energy.setCapacity(Math.max(400_000_000L, c * 400));
        boolean now = energy.stored() >= c;
        if (now) energy.consume(c);
        if (now != on) {
            on = now;
            if (on) FieldEnvironment.add(this);
            else FieldEnvironment.remove(this);
            level().setBlock(worldPosition, getBlockState().setValue(BlockStateProperties.POWERED, on), 3);
            setChanged();
        }
    }

    private net.minecraft.world.level.Level level() {
        return getLevel();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (getLevel() != null && !getLevel().isClientSide && on) FieldEnvironment.add(this);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        FieldEnvironment.remove(this);
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        FieldEnvironment.remove(this);
    }

    @Override
    protected void saveAdditional(CompoundTag t, HolderLookup.Provider r) {
        super.saveAdditional(t, r);
        t.putInt("Level", level);
        t.putBoolean("On", on);
        t.putLong("Energy", energy.stored());
    }

    @Override
    protected void loadAdditional(CompoundTag t, HolderLookup.Provider r) {
        super.loadAdditional(t, r);
        level = t.getInt("Level");
        on = t.getBoolean("On");
        energy.setCapacity(Math.max(400_000_000L, t.getLong("Energy")));
        energy.setRaw(t.getLong("Energy"));
    }
}
