package com.theoreticalsciences.client;

import com.theoreticalsciences.QHConfig;
import com.theoreticalsciences.TheoreticalSciences;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.List;

/**
 * 1.3.1 (the text rework): the mod's items and blocks no longer describe themselves in tooltips; that lives in the
 * Quantum Codex. Runs after every other tooltip handler and takes out the mod's own lines from the mod's own items,
 * keeping what is live about that one item: what it holds, its charge, mode, channel, wear, coating, traits and the
 * like (the Codex cannot show those). The item's name, vanilla lines (enchantments, attributes, F3+H) and other mods'
 * lines stay. Off with the client option itemDescriptionsInTooltips.
 */
@EventBusSubscriber(modid = TheoreticalSciences.MODID, value = Dist.CLIENT)
public final class TooltipTrim {
    /** Key families of the mod's text. */
    private static final String[] OURS = {"tooltip.", "item.", "block.", "message.", "trait.", "scepter.", "gui."};
    /** Lines that say something about this one item, not about the kind of item: kept. */
    private static final String[] LIVE = {
            "tooltip.theoreticalsciences.assembler_bound", "tooltip.theoreticalsciences.assembler_coil_", "tooltip.theoreticalsciences.assembler_mode_",
            "tooltip.theoreticalsciences.anchored_warp_bubble",
            "tooltip.theoreticalsciences.calibration_card.",
            "tooltip.theoreticalsciences.censer_set",
            "tooltip.theoreticalsciences.coated",
            "tooltip.theoreticalsciences.dynamic_singularity",
            "tooltip.theoreticalsciences.empyrean.holds", "tooltip.theoreticalsciences.empyrean.more",
            "tooltip.theoreticalsciences.eschaton.", "tooltip.theoreticalsciences.eschaton_cell.holds",
            "tooltip.theoreticalsciences.folded_matter.holds", "tooltip.theoreticalsciences.folded_matter.more",
            "tooltip.theoreticalsciences.lead_pig.",
            "tooltip.theoreticalsciences.radiation_plating.",
            "tooltip.theoreticalsciences.rebreather_left",
            "tooltip.theoreticalsciences.singularity_scepter.mode",
            "tooltip.theoreticalsciences.species",
            "tooltip.theoreticalsciences.transcendent_cache_holds",
            "tooltip.theoreticalsciences.tuner_",
            "tooltip.theoreticalsciences.wear.",
            "trait.theoreticalsciences.",
            "message.theoreticalsciences.demiurge_hand.mode", "message.theoreticalsciences.excavator.mode",
    };

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onTooltip(ItemTooltipEvent e) {
        if (e.getItemStack().isEmpty() || QHConfig.ITEM_DESCRIPTIONS.get()) return;
        if (!TheoreticalSciences.MODID.equals(BuiltInRegistries.ITEM.getKey(e.getItemStack().getItem()).getNamespace())) return;
        List<Component> tip = e.getToolTip();
        for (int i = tip.size() - 1; i >= 1; i--)                // (line 0 is the name)
            if (descriptive(tip.get(i))) tip.remove(i);
    }

    public static boolean descriptive(Component line) {
        if (!(line.getContents() instanceof TranslatableContents t)) return false;   // literal lines: counts, contents lists
        String k = t.getKey();
        boolean ours = false;
        for (String f : OURS)
            if (k.startsWith(f + TheoreticalSciences.MODID + ".")) {
                ours = true;
                break;
            }
        if (!ours) return false;
        for (String l : LIVE) if (k.startsWith(l)) return false;
        return true;
    }

    private TooltipTrim() {}
}
