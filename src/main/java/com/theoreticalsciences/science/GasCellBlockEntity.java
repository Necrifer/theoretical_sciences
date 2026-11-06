package com.theoreticalsciences.science;

import com.theoreticalsciences.registry.QHFluids;
import com.theoreticalsciences.registry.QHLogistics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/**
 * 1.2.0: a MOF Gas Storage Cell. A metal-organic framework holds gas on its inner surface (Langmuir: the uptake rises
 * steeply at low pressure and levels off), so a cell full of it stores far more gas than an empty tank of its size - here
 * 256,000 mB of one gas, against a tank's 16,000. Takes gases only: the mod's, and any in the c:gaseous tag. Pipes fill and
 * drain it from any side; it keeps its gas when broken.
 */
public class GasCellBlockEntity extends BlockEntity {
    public static final int CAPACITY = 256_000;
    static final net.minecraft.tags.TagKey<Fluid> GASEOUS = net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.FLUID,
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("c", "gaseous"));

    public static boolean gas(FluidStack s) {
        if (s.isEmpty()) return false;
        if (s.getFluid().builtInRegistryHolder().is(GASEOUS)) return true;
        var id = net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(s.getFluid());
        if (!com.theoreticalsciences.TheoreticalSciences.MODID.equals(id.getNamespace())) return false;
        QHFluids.Entry e = QHFluids.ALL.get(id.getPath().startsWith("flowing_") ? id.getPath().substring(8) : id.getPath());
        return e != null && e.def.gas();
    }

    public final FluidTank tank = new FluidTank(CAPACITY, GasCellBlockEntity::gas) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };

    public GasCellBlockEntity(BlockPos pos, BlockState state) {
        super(QHLogistics.GAS_CELL.get(), pos, state);
    }

    @Override
    protected void saveAdditional(CompoundTag t, HolderLookup.Provider r) {
        super.saveAdditional(t, r);
        t.put("Tank", tank.writeToNBT(r, new CompoundTag()));
    }

    @Override
    protected void loadAdditional(CompoundTag t, HolderLookup.Provider r) {
        super.loadAdditional(t, r);
        tank.readFromNBT(r, t.getCompound("Tank"));
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder b) {
        super.collectImplicitComponents(b);
        if (level == null) return;
        CompoundTag t = new CompoundTag();
        t.put("Tank", tank.writeToNBT(level.registryAccess(), new CompoundTag()));
        b.set(DataComponents.CUSTOM_DATA, CustomData.of(t));
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput in) {
        super.applyImplicitComponents(in);
        CustomData d = in.get(DataComponents.CUSTOM_DATA);
        if (d != null && level != null) tank.readFromNBT(level.registryAccess(), d.copyTag().getCompound("Tank"));
    }
}
