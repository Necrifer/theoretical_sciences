package com.theoreticalsciences.home;

import com.theoreticalsciences.TheoreticalSciences;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;

import java.util.List;

/**
 * 1.8.0, Recreating Home: the forty-two materials the Core asks for (forty-two: the answer to everything), and how many
 * of each: 33,550,337. That is the fifth perfect number, 2^12 (2^13 - 1) = 33,550,336, and one more: every recipe gives
 * 4,096, so 8,191 crafts reach the perfect number and the 8,192nd is the one that ends the cycle.
 */
public final class HomeMaterials {
    private HomeMaterials() {}

    public static final long TARGET = 33_550_337L;

    /** Twenty-four elements, twelve works, five pillars, and the keystone (the order of the checklist). */
    public static final List<String> IDS = List.of(
            // the elements: what a world is made of
            "remembered_soil", "living_water", "first_breath", "first_light", "hearthfire", "seraphic_alloy", "choir_crystal", "woven_memory",
            "still_stone", "morning_dew", "evening_ember", "quiet_snow", "deep_root", "bright_seed", "kind_shadow", "warm_rain",
            "salt_of_tears", "honey_of_days", "thread_of_fate", "dust_of_stars", "ash_of_worlds", "word_unspoken", "name_forgotten", "song_unsung",
            // the works: what is made of them
            "firmament_plate", "foundation_stone", "garden_bed", "wellspring", "lantern_of_days", "bell_of_hours",
            "loom_of_years", "gate_lintel", "threshold_step", "roof_of_heaven", "table_of_welcome", "bed_of_rest",
            // the pillars: what holds the house up
            "pillar_of_knowing", "pillar_of_making", "pillar_of_keeping", "pillar_of_mercy", "pillar_of_return",
            // and the way in
            "keystone_of_return");

    public static int index(Item item) {
        var key = BuiltInRegistries.ITEM.getKey(item);
        return key.getNamespace().equals(TheoreticalSciences.MODID) ? IDS.indexOf(key.getPath()) : -1;
    }

    public static Item item(int i) {
        return BuiltInRegistries.ITEM.get(TheoreticalSciences.id(IDS.get(i)));
    }
}
