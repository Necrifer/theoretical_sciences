package com.theoreticalsciences.machine;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import javax.annotation.Nullable;

/**
 * Automation view: fill input tanks, drain output tanks. A multiblock with a physics model also exposes its coolant
 * tank (filled with any known coolant) and its spent-coolant tank (drained, e.g. warm helium back to the liquefier).
 */
public class MachineFluidHandler implements IFluidHandler {
    private final MachineBlockEntity be;

    public MachineFluidHandler(MachineBlockEntity be) {
        this.be = be;
    }

    @Nullable
    private com.theoreticalsciences.machine.physics.PhysicsState physics() {
        return be instanceof ControllerBlockEntity c && c.hasPhysics() ? c.physics : null;
    }

    /** All tanks in order: recipe inputs, recipe outputs, then coolant and spent coolant (physics machines only). */
    @Nullable
    public FluidTank tank(int i) {
        if (i < be.tanks.length) return i >= 0 ? be.tanks[i] : null;
        var p = physics();
        if (p == null) return null;
        return i == be.tanks.length ? p.coolant : i == be.tanks.length + 1 ? p.spent : null;
    }

    public boolean isInputTank(int i) {
        return i < be.type.fluidInputs || (physics() != null && i == be.tanks.length);
    }

    @Override
    public int getTanks() {
        return be.tanks.length + (physics() != null ? 2 : 0);
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        FluidTank t = tank(tank);
        return t == null ? FluidStack.EMPTY : t.getFluid();
    }

    @Override
    public int getTankCapacity(int tank) {
        FluidTank t = tank(tank);
        return t == null ? 0 : t.getCapacity();
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        if (tank < be.type.fluidInputs) return true;
        var p = physics();
        return p != null && tank == be.tanks.length && p.coolant.isFluidValid(stack);
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (resource.isEmpty() || be.isRemoved()) return 0;     // (a pipe still holding a removed machine's handler)
        // 1. an input tank that already holds this fluid (a recipe is waiting for it)
        for (int i = 0; i < be.type.fluidInputs; i++) {
            FluidStack in = be.tanks[i].getFluid();
            if (!in.isEmpty() && FluidStack.isSameFluidSameComponents(in, resource)) return be.tanks[i].fill(resource, action);
        }
        // 2. the coolant loop, if this is a coolant and the loop has room for it - but a fluid some recipe of this
        //    machine also takes (AE2 pushing a pattern's inputs, say) goes to an input tank first, so the coolant
        //    loop never swallows a recipe's ingredient
        var p = physics();
        boolean isCoolant = p != null && p.coolant.isFluidValid(resource);
        boolean coolantOk = isCoolant && p.coolant.getSpace() > 0
                && (p.coolant.isEmpty() || FluidStack.isSameFluidSameComponents(p.coolant.getFluid(), resource));
        boolean ingredient = be.getLevel() != null && MEWanted.of(be.getLevel(), be.type.recipeType).wantsFluid(resource);
        // a coolant no recipe takes goes only to the coolant loop - when that is full it is refused, never parked
        // in a recipe tank (automation cannot drain those, so the machine would stall)
        if (isCoolant && !ingredient) return coolantOk ? p.coolant.fill(resource, action) : 0;
        // and a fluid no recipe of this machine takes at all is refused too, for the same reason
        if (!ingredient && be.getLevel() != null) return 0;
        // 3. any empty input tank
        for (int i = 0; i < be.type.fluidInputs; i++) {
            if (be.tanks[i].getFluid().isEmpty()) return be.tanks[i].fill(resource, action);
        }
        // 4. an ingredient that is also a coolant, with every input tank taken: top up the coolant loop
        return coolantOk ? p.coolant.fill(resource, action) : 0;
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        if (be.isRemoved()) return FluidStack.EMPTY;
        for (int i = be.type.fluidInputs; i < be.tanks.length; i++) {
            FluidStack d = be.tanks[i].drain(resource, action);
            if (!d.isEmpty()) return d;
        }
        var p = physics();
        return p != null ? p.spent.drain(resource, action) : FluidStack.EMPTY;
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        if (be.isRemoved()) return FluidStack.EMPTY;
        for (int i = be.type.fluidInputs; i < be.tanks.length; i++) {
            FluidStack d = be.tanks[i].drain(maxDrain, action);
            if (!d.isEmpty()) return d;
        }
        var p = physics();
        return p != null ? p.spent.drain(maxDrain, action) : FluidStack.EMPTY;
    }
}
