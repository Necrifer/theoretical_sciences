package com.theoreticalsciences.machine;

import net.minecraft.network.chat.Component;

public enum MachineStatus {
    IDLE(0x8899AA), RUNNING(0x40FFA0), GENERATING(0x40FFA0), NO_POWER(0xFF6050), OUTPUT_FULL(0xFFC040),
    NEEDS_RESEARCH(0xC080FF), INCOMPLETE(0xFF4060), BLOCKED(0xFF8040), FAULT(0xFF2020), LOW_TIER(0xFF9040),
    AWAITING_CONFIRMATION(0xFF2060), DECLINED(0x808080),
    NO_ACTIVE_RESEARCH(0x8899AA), DOSSIER_TOO_WEAK(0xFF9040), WRONG_ENVIRONMENT(0xC060FF), WRONG_DIMENSION(0xFF60C0),
    QUENCHED(0xFF5020), UNSTABLE(0xFF8040), OVERHEATING(0xFF3010), OUT_OF_WINDOW(0xFFC040), NEEDS_OMEGA(0xFF2060),
    TRANSCENDENTAL_STANDS(0xFFC040), TRANSCENDENTAL_COLLAPSING(0xFF8040), ALREADY_REKINDLED(0xC0A060),
    NOT_YET_FORGED(0xFFC040), CONTAMINATED(0x90C040),
    NO_MANA(0x40C8FF), NO_AURA(0x70E080),          // 1.7.3: a recipe that draws on Botania's mana or Nature's Aura's aura
    NO_VIS(0xB070FF), NO_ASPECTS(0xD0A0FF), NO_LUMEN(0x80B0FF);          // 1.11.0: Thaumaturge's vis and aspects, Astral Sorcery's Lumen          // 1.7.3: a recipe that draws on Botania's mana or Nature's Aura's aura

    public final int color;

    MachineStatus(int color) {
        this.color = color;
    }

    /** Round 35: the label for a machine - a structure built in the wrong dimension says which one it needs. */
    public Component label(MachineType type) {
        if (this == WRONG_DIMENSION) {
            var dims = MachineType.allowedDimensions(type);
            if (dims != null) return Component.translatable("status.theoreticalsciences.wrong_dimension_in", MachineType.dimensionList(dims));
        }
        return label();
    }

    public Component label() {
        return Component.translatable("status.theoreticalsciences." + name().toLowerCase(java.util.Locale.ROOT));
    }
}
