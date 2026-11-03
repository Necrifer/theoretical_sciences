package com.theoreticalsciences.item;

import com.theoreticalsciences.TheoreticalSciences;
import com.theoreticalsciences.endgame.DodecaractExcavatorItem;
import com.theoreticalsciences.endgame.EschatonCharge;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * 1.3.7: who may break what.
 * <ul>
 *   <li>Blocks in {@code #theoreticalsciences:needs_omnipotent_tool} (the Omnipotence Casing) can only be mined - and only drop -
 *   with a tool in {@code #theoreticalsciences:omnipotent_tools}. Both tags are data, so packs can change them.</li>
 *   <li>A charged Dodecaract Excavator breaks any breakable block at once, whatever slows the player down (mining
 *   fatigue, water, the air) - and always gets its drops. It runs last, after every dimension's own digging rules.</li>
 * </ul>
 */
@EventBusSubscriber(modid = TheoreticalSciences.MODID)
public final class ToolRules {
    public static final TagKey<Block> NEEDS_OMNIPOTENT_TOOL = TagKey.create(Registries.BLOCK, TheoreticalSciences.id("needs_omnipotent_tool"));
    public static final TagKey<Item> OMNIPOTENT_TOOLS = TagKey.create(Registries.ITEM, TheoreticalSciences.id("omnipotent_tools"));

    private static boolean excavating(Player p) {
        ItemStack s = p.getMainHandItem();
        return s.getItem() instanceof DodecaractExcavatorItem && EschatonCharge.get(s) > 0;
    }

    private static boolean forbidden(Player p, BlockState st) {
        return st.is(NEEDS_OMNIPOTENT_TOOL) && !p.getMainHandItem().is(OMNIPOTENT_TOOLS);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBreakSpeed(PlayerEvent.BreakSpeed e) {
        Player p = e.getEntity();
        BlockState st = e.getState();
        if (forbidden(p, st)) {
            e.setNewSpeed(0F);
            e.setCanceled(true);
            return;
        }
        if (excavating(p)) {
            float h = e.getPosition().map(pos -> st.getDestroySpeed(p.level(), pos)).orElse(st.getBlock().defaultDestroyTime());
            if (h >= 0) e.setNewSpeed(Math.max(e.getNewSpeed(), 1.0E9F));      // any breakable block, in one tick
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onHarvestCheck(PlayerEvent.HarvestCheck e) {
        Player p = e.getEntity();
        if (forbidden(p, e.getTargetBlock())) e.setCanHarvest(false);
        else if (excavating(p)) e.setCanHarvest(true);
    }

    private ToolRules() {}
}
