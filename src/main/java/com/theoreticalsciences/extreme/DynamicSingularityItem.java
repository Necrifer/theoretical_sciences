package com.theoreticalsciences.extreme;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** A singularity added by a pack (or made automatically from an ingot), or a cluster of them (round 21). */
public class DynamicSingularityItem extends Item {
    final boolean cluster;

    public DynamicSingularityItem(Properties p, boolean cluster) {
        super(p.fireResistant());
        this.cluster = cluster;
    }

    @Override
    public Component getName(ItemStack stack) {
        boolean client = net.neoforged.fml.loading.FMLEnvironment.dist.isClient();
        return Singularities.name(stack, client);
    }

    /**
     * Round 34c: no enchantment glint on clusters any more. With Sodium's "animate only visible textures" an item drawn
     * with the glint never tells Sodium its texture is on screen, so the animation froze on its first frame (only in
     * packs with Sodium - the test instance without it was fine). The orbiting glow layer marks a cluster instead.
     */
    @Override
    public boolean isFoil(ItemStack stack) {
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        String id = Singularities.idOf(stack);
        if (id == null) return;
        if (cluster) {
            tip.add(Component.translatable("tooltip.theoreticalsciences.cluster").withStyle(ChatFormatting.DARK_PURPLE));
            return;
        }
        Singularities.Def d = null;
        for (Singularities.Def x : Singularities.any()) if (x.id().equals(id)) d = x;
        if (d != null) tip.add(Component.translatable("tooltip.theoreticalsciences.dynamic_singularity", String.format("%,d", d.count()), d.source().getHoverName())
                .withStyle(ChatFormatting.DARK_AQUA));
    }

    /** Colour of a tint layer (0 core, 1 glow); -1 means "work it out from the item's texture" (client). */
    public static int[] colours(ItemStack stack) {
        String id = Singularities.idOf(stack);
        if (id == null) return new int[]{0x202020, 0xFFFFFF};
        if (id.startsWith("#")) {
            int at = id.indexOf('@');
            int h = (at < 0 ? id : id.substring(0, at)).hashCode();
            int glow = net.minecraft.util.Mth.hsvToRgb(((h >>> 8) & 0xFF) / 255F, 0.5F, 1F) & 0xFFFFFF;
            // round 30: the body in a deep shade of the glow, not near-black - so its swirling animation can be seen
            return new int[]{(glow >> 1) & 0x7F7F7F, glow};
        }
        for (Singularities.Def d : Singularities.any())
            if (d.id().equals(id)) return new int[]{d.core(), d.glow()};
        return new int[]{0x202020, 0xFFFFFF};
    }
}
