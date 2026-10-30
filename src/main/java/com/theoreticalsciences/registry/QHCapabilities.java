package com.theoreticalsciences.registry;

import com.theoreticalsciences.machine.ControllerBlockEntity;
import com.theoreticalsciences.machine.MachineType;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/** Standard NeoForge capabilities: works with any pipe/cable mod (Mekanism, AE2, XNet, EnderIO, Flux...). */
public final class QHCapabilities {
    public static void register(RegisterCapabilitiesEvent event) {
        for (MachineType t : MachineType.values()) {
            var type = QHBlockEntities.machine(t);
            event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, type, (be, side) -> be.itemCap);
            event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, type, (be, side) -> be.energy);
            // multiblocks always: even one without recipe tanks has a coolant loop when it runs on a physics model
            if (t.tanks() > 0 || t.isMultiblock()) event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, type,
                    (be, side) -> be.fluidCap.getTanks() > 0 ? be.fluidCap : null);
        }
        // Containment cells are bucket subclasses, which NeoForge does not wrap automatically: without this, pipes,
        // tanks, AE2 and every other mod would see them as plain items instead of fluid containers.
        for (QHFluids.Entry e : QHFluids.ALL.values())
            event.registerItem(Capabilities.FluidHandler.ITEM, (stack, ctx) -> new com.theoreticalsciences.item.ContainmentCells.CellWrapper(stack), e.bucket.get());
        event.registerItem(Capabilities.FluidHandler.ITEM, (stack, ctx) -> new com.theoreticalsciences.item.ContainmentCells.CellWrapper(stack), QHItems.get("containment_cell"));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, QHBlockEntities.PORT.get(), (be, side) -> {
            ControllerBlockEntity c = be.controller();
            return c == null ? null : c.itemCap;
        });
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, QHBlockEntities.PORT.get(), (be, side) -> {
            ControllerBlockEntity c = be.controller();
            return c == null || c.fluidCap.getTanks() == 0 ? null : c.fluidCap;
        });
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, QHBlockEntities.PORT.get(), (be, side) -> {
            ControllerBlockEntity c = be.controller();
            return c == null ? null : c.energy;
        });
    }

    private QHCapabilities() {}
}
