package com.theoreticalsciences.client.transit;

import com.theoreticalsciences.TheoreticalSciences;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

/**
 * One look per dimension of the mod: its background colour, its main ("ink") colour, its accent colour and the text
 * colours for its loading page. The drawings themselves live in {@link TransitScenes}.
 */
public enum TransitStyle {
    //            dimension id               background ink       accent    title     subtitle  bright page?
    BULK("the_bulk", 0x05061A, 0x4060FF, 0xB050FF, 0xD8D0FF, 0x9890C8, false),
    FOAM("quantum_foam", 0x021318, 0x40E8FF, 0xFFFFFF, 0xE0FCFF, 0x88C8D0, false),
    PLATONIC("platonic_realm", 0x1C1508, 0xFFD060, 0xFFF2D0, 0xFFF0C8, 0xC8B080, false),
    BLACK_HOLE("black_hole_interior", 0x000000, 0xFF8A30, 0xFFE0A0, 0xFFD8A8, 0xA07858, false),
    WHITE_HOLE("white_hole_interior", 0xF0F6FF, 0xA8D8FF, 0xFFE8B0, 0x1C2C48, 0x506080, true),
    TRANSFINITE("transfinite_hierarchy", 0x0C0418, 0xB070FF, 0xFFD0FF, 0xE8D0FF, 0x9878C0, false),
    ANTIVERSE("antiverse", 0x14040A, 0xFF3048, 0x30F0FF, 0xFFE0E4, 0xC08890, false),
    HYPERREAL("hyperdimensional_reality", 0x12021A, 0xFF40E0, 0xA060FF, 0xFFD8F8, 0xC088C0, false),
    LIVING_PROOF("living_proof", 0x010A04, 0x40FF70, 0xD0FFD8, 0xD0FFD8, 0x70B880, false),
    TRANSCENDENTAL("transcendental", 0x140C02, 0xFFE8A0, 0xFFFFFF, 0xFFF4D8, 0xC8B488, false);

    public final String id;
    public final int bg, ink, accent, title, subtitle;
    /** True for pages so bright that their text must be dark (the White Hole's flash). */
    public final boolean bright;

    TransitStyle(String id, int bg, int ink, int accent, int title, int subtitle, boolean bright) {
        this.id = id;
        this.bg = bg;
        this.ink = ink;
        this.accent = accent;
        this.title = title;
        this.subtitle = subtitle;
        this.bright = bright;
    }

    /** "transit.theoreticalsciences.the_bulk" etc.: the start of this dimension's lang keys. */
    public String key() {
        return "transit.theoreticalsciences." + id;
    }

    /** The style for a dimension, or null if it is not one of ours. */
    public static TransitStyle of(ResourceLocation dim) {
        if (dim == null || !dim.getNamespace().equals(TheoreticalSciences.MODID)) return null;
        for (TransitStyle s : values()) if (s.id.equals(dim.getPath())) return s;
        return null;
    }

    public static TransitStyle of(ResourceKey<Level> dim) {
        return dim == null ? null : of(dim.location());
    }
}
