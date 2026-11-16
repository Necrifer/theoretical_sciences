package com.theoreticalsciences.client.screen;

import com.theoreticalsciences.machine.physics.PhysicsModel;

/**
 * 1.16.0: every quantity of a machine's physics gets a colour of its own, used wherever that quantity appears: its
 * dial, its readout line, its name in another quantity's "built from" list, and the console's oscilloscope label.
 * The same colour means the same thing, so an equation's pieces can be matched to what they stand for at a glance.
 * <p>
 * The colours are spaced round the colour wheel and kept bright enough to read on the console's dark glass; the
 * world's built-in quantities (temperature, gravity, the disturbance...) share one quiet grey.
 */
public final class QuantityColours {
    static final int[] PALETTE = {0x5FB8FF, 0xFFB347, 0xC08CFF, 0x62E3A1, 0xFF7FA8, 0xE6E25A, 0x6FE0E6, 0xFF8C5A, 0xA0D060, 0xD0A0FF};
    static final int WORLD = 0xA8B0C0;

    private QuantityColours() {}

    public static int of(PhysicsModel m, String id) {
        int i = 0;
        for (PhysicsModel.Param p : m.params) {
            if (p.id().equals(id)) return PALETTE[i % PALETTE.length];
            i++;
        }
        for (PhysicsModel.Named d : m.derived) {
            if (d.id().equals(id)) return PALETTE[i % PALETTE.length];
            i++;
        }
        for (PhysicsModel.State s : m.states) {
            if (s.id().equals(id)) return PALETTE[i % PALETTE.length];
            i++;
        }
        return WORLD;
    }
}
