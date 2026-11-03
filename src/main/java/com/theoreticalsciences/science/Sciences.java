package com.theoreticalsciences.science;

import com.theoreticalsciences.TheoreticalSciences;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/** Round 37 (1.1): the tags the new sciences read. Packs and other mods can add to any of them. */
public final class Sciences {
    /** Iron, steel, nickel, cobalt and anything else a magnetic field pulls on (Magnetized, the Gauss Meter). */
    public static final TagKey<Item> FERROMAGNETIC = item("ferromagnetic");
    /** Weakly radioactive items (ores, raw uranium minerals): a small dose while carried. */
    public static final TagKey<Item> RADIOACTIVE = item("radioactive");
    /** Strongly radioactive items (radium, refined isotopes): ten times the dose of the tag above. */
    public static final TagKey<Item> STRONGLY_RADIOACTIVE = item("strongly_radioactive");
    /** Blocks the Geiger Counter hears. */
    public static final TagKey<Block> RADIOACTIVE_BLOCKS = block("radioactive");
    /** Blocks the Gauss Meter reads as magnets (lodestones, magnet blocks...). */
    public static final TagKey<Block> MAGNETIC_BLOCKS = block("magnetic");

    private static TagKey<Item> item(String id) {
        return TagKey.create(Registries.ITEM, TheoreticalSciences.id(id));
    }

    private static TagKey<Block> block(String id) {
        return TagKey.create(Registries.BLOCK, TheoreticalSciences.id(id));
    }

    private Sciences() {}
}
