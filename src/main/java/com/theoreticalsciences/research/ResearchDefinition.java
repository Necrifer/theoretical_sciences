package com.theoreticalsciences.research;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

import java.util.List;

/**
 * A research entry, loaded from {@code data/<namespace>/ts_research/<id>.json}.
 * Datapacks and KubeJS can add, remove or override entries.
 * <pre>
 * {
 *   "title": "Quantum Entanglement",
 *   "description": "...",
 *   "icon": "theoreticalsciences:entangled_photon_pair",
 *   "category": "quantum",
 *   "requires": ["theoreticalsciences:quantum_foundations"],
 *   "insight": 5,
 *   "cost": [ {"item": "theoreticalsciences:cherenkov_quartz", "count": 4} ],
 *   "x": 2, "y": 0,
 *   "time": 900,          // seconds of study after paying (0 = instant); a Research Nexus can shorten it
 *   "hidden": false,      // true: not shown until every prerequisite is researched
 *   "grant_only": false   // true: cannot be researched, only granted (secret milestones)
 * }
 * </pre>
 */
public record ResearchDefinition(String title, String description, ResourceLocation icon, String category,
                                 List<ResourceLocation> requires, List<SizedIngredient> cost, int insight, int x, int y,
                                 int time, boolean hidden, boolean grantOnly, java.util.Optional<ResearchPuzzle> puzzle) {

    public static final Codec<ResearchDefinition> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("title").forGetter(ResearchDefinition::title),
            Codec.STRING.optionalFieldOf("description", "").forGetter(ResearchDefinition::description),
            ResourceLocation.CODEC.optionalFieldOf("icon", ResourceLocation.withDefaultNamespace("book")).forGetter(ResearchDefinition::icon),
            Codec.STRING.optionalFieldOf("category", "general").forGetter(ResearchDefinition::category),
            ResourceLocation.CODEC.listOf().optionalFieldOf("requires", List.of()).forGetter(ResearchDefinition::requires),
            SizedIngredient.FLAT_CODEC.listOf().optionalFieldOf("cost", List.of()).forGetter(ResearchDefinition::cost),
            Codec.intRange(0, 1_000_000).optionalFieldOf("insight", 0).forGetter(ResearchDefinition::insight),
            Codec.INT.optionalFieldOf("x", 0).forGetter(ResearchDefinition::x),
            Codec.INT.optionalFieldOf("y", 0).forGetter(ResearchDefinition::y),
            Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("time", 0).forGetter(ResearchDefinition::time),
            Codec.BOOL.optionalFieldOf("hidden", false).forGetter(ResearchDefinition::hidden),
            Codec.BOOL.optionalFieldOf("grant_only", false).forGetter(ResearchDefinition::grantOnly),
            ResearchPuzzle.CODEC.optionalFieldOf("puzzle").forGetter(ResearchDefinition::puzzle)
    ).apply(i, ResearchDefinition::new));

    /** Which tier of Research Dossier a Research Nexus needs to speed this entry up (1-4). */
    public int dossierTier() {
        return insight <= 150 ? 1 : insight <= 400 ? 2 : insight <= 1000 ? 3 : 4;
    }

    public Component titleComponent(ResourceLocation id) {
        return Component.translatableWithFallback("research." + id.getNamespace() + "." + id.getPath(), title);
    }

    public Component descriptionComponent(ResourceLocation id) {
        return Component.translatableWithFallback("research." + id.getNamespace() + "." + id.getPath() + ".desc", description);
    }
}
