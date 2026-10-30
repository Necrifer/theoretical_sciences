package com.theoreticalsciences.api.event;

import com.theoreticalsciences.machine.ControllerBlockEntity;
import net.neoforged.bus.api.Event;

/** Fired on {@code NeoForge.EVENT_BUS} when a multiblock structure becomes complete or breaks. */
public class MultiblockFormedEvent extends Event {
    private final ControllerBlockEntity controller;
    private final boolean formed;

    public MultiblockFormedEvent(ControllerBlockEntity controller, boolean formed) {
        this.controller = controller;
        this.formed = formed;
    }

    public ControllerBlockEntity getController() {
        return controller;
    }

    public boolean isFormed() {
        return formed;
    }
}
