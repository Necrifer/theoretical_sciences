package com.theoreticalsciences.client;

import com.theoreticalsciences.TheoreticalSciences;
import com.theoreticalsciences.multiblock.MultiblockDefinition;
import com.theoreticalsciences.multiblock.Multiblocks;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Round 35: every item and block of the mod gets a useful tooltip, not only the ones with special behaviour.
 * <ul>
 *   <li>Always: its flavour line (the "lore" every item has in the language file) if the item did not add it itself.</li>
 *   <li>Holding Shift: what it is for - which structures it is a part of, whether it is made at the Omnipotence
 *       Core's Forge, whether it lies beyond Omnipotence - and the key that opens its page in the Quantum Codex.</li>
 * </ul>
 */
@EventBusSubscriber(modid = TheoreticalSciences.MODID, value = Dist.CLIENT)
public final class ItemTips {
    private static Map<Block, List<String>> partOf;
    private static Set<String> forged;

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onTooltip(ItemTooltipEvent e) {
        ItemStack s = e.getItemStack();
        if (s.isEmpty()) return;
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(s.getItem());
        if (!TheoreticalSciences.MODID.equals(id.getNamespace())) return;
        List<Component> tip = e.getToolTip();
        // advanced tooltips (F3+H) end with the item id: our lines go above it
        int at = tip.size();
        for (int i = tip.size() - 1; i > 0; i--)
            if (tip.get(i).getString().equals(id.toString())) {
                at = i;
                break;
            }
        List<Component> add = new ArrayList<>();
        String lore = s.getDescriptionId() + ".lore";
        if (I18n.exists(lore) && !mentions(tip, lore))
            add.add(Component.translatable(lore).withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.ITALIC));

        List<Component> details = details(s.getItem(), id);
        if (!details.isEmpty()) {
            if (Screen.hasShiftDown()) add.addAll(details);
            else add.add(Component.translatable("tooltip.theoreticalsciences.hold_shift",
                    Component.literal("Shift").withStyle(ChatFormatting.YELLOW)).withStyle(ChatFormatting.DARK_GRAY));
        }
        tip.addAll(at, add);
    }

    private static List<Component> details(Item item, ResourceLocation id) {
        List<Component> out = new ArrayList<>();
        // part of a structure
        if (item instanceof BlockItem bi) {
            List<String> names = structures().get(bi.getBlock());
            if (names != null && !names.isEmpty()) {
                String list = String.join(", ", names.subList(0, Math.min(3, names.size())));
                if (names.size() > 3) list += I18n.get("tooltip.theoreticalsciences.and_more", names.size() - 3);
                out.add(Component.translatable("tooltip.theoreticalsciences.structure_part", list).withStyle(ChatFormatting.GRAY));
            }
        }
        if (forged().contains(id.getPath()))
            out.add(Component.translatable("tooltip.theoreticalsciences.forged_at_core").withStyle(ChatFormatting.GOLD));
        if (new ItemStack(item).is(com.theoreticalsciences.transcend.OmnipotencePayload.BEYOND))
            out.add(Component.translatable("tooltip.theoreticalsciences.beyond_omnipotence").withStyle(ChatFormatting.LIGHT_PURPLE));
        try {
            if (com.theoreticalsciences.client.codex.CodexData.ensure() && com.theoreticalsciences.client.codex.CodexData.entryForItem(id.toString()) != null)
                out.add(Component.translatable("tooltip.theoreticalsciences.codex_key",
                        Component.keybind("key.theoreticalsciences.codex_lookup").withStyle(ChatFormatting.YELLOW)).withStyle(ChatFormatting.DARK_GRAY));
        } catch (Throwable ignored) {
            // the Codex data could not be read: no hint, rather than no tooltip
        }
        return out;
    }

    private static boolean mentions(List<Component> tip, String key) {
        for (Component c : tip) {
            if (c.getContents() instanceof TranslatableContents t && t.getKey().equals(key)) return true;
            for (Component sib : c.getSiblings()) if (sib.getContents() instanceof TranslatableContents t && t.getKey().equals(key)) return true;
        }
        return false;
    }

    /** Block -> the names of the structures that use it (worked out once). */
    private static synchronized Map<Block, List<String>> structures() {
        if (partOf != null) return partOf;
        Map<Block, List<String>> m = new HashMap<>();
        try {
            for (MultiblockDefinition d : Multiblocks.all().values()) {
                Block controller = BuiltInRegistries.BLOCK.get(d.controllerBlock);
                String name = I18n.get("block." + d.controllerBlock.getNamespace() + "." + d.controllerBlock.getPath());
                for (Block b : d.billOfMaterials().keySet()) {
                    if (b == controller) continue;
                    List<String> l = m.computeIfAbsent(b, k -> new ArrayList<>());
                    if (!l.contains(name)) l.add(name);
                }
            }
            m.values().forEach(l -> l.sort(String::compareTo));
        } catch (Throwable t) {
            TheoreticalSciences.LOGGER.debug("Theoretical Sciences tooltips: no structure list", t);
        }
        return partOf = m;
    }

    /** What the Omnipotence Core's Forge makes (its own list, not the generated creative ones). */
    private static synchronized Set<String> forged() {
        if (forged != null) return forged;
        Set<String> s = new HashSet<>();
        try {
            for (var r : com.theoreticalsciences.transcend.CoreForge.RECIPES) {
                String it = r.out().item();
                s.add(it.contains(":") ? it.substring(it.indexOf(':') + 1) : it);
            }
        } catch (Throwable ignored) {
        }
        return forged = s;
    }

    private ItemTips() {}
}
