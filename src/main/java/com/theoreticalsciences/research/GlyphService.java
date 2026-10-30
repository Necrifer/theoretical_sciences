package com.theoreticalsciences.research;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Teaches players research symbols as they progress: a glyph unlocked "by an item" is learned the first time the
 * player holds, picks up or crafts it; one unlocked "by a research" when that research is complete. Learned glyphs
 * are kept for good.
 */
public final class GlyphService {
    public static void onPlayerTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer sp) || sp.tickCount % 40 != 7) return;
        ResearchSavedData data = ResearchSavedData.get(sp.server);
        ResearchSavedData.Entry entry = data.of(sp.getUUID());
        List<GlyphRegistry.Glyph> fresh = new ArrayList<>();
        boolean added = false;
        for (GlyphRegistry.Glyph g : GlyphRegistry.server().values()) {
            if (entry.glyphs.contains(g.token())) continue;
            if (learned(sp, entry, g)) {
                entry.glyphs.add(g.token());
                added = true;
                if (g.unlock() != GlyphRegistry.Unlock.START) fresh.add(g);
            }
        }
        if (!added) return;
        data.setDirty();
        ResearchService.sync(sp);
        if (fresh.isEmpty()) return;
        MutableComponent list = Component.empty();
        for (int i = 0; i < Math.min(6, fresh.size()); i++) {
            if (i > 0) list.append(Component.literal(", "));
            list.append(Component.literal(fresh.get(i).token()).withStyle(ChatFormatting.WHITE))
                    .append(Component.literal(" (" + fresh.get(i).name() + ")").withStyle(ChatFormatting.GRAY));
        }
        if (fresh.size() > 6) list.append(Component.literal(" +" + (fresh.size() - 6)));
        sp.displayClientMessage(Component.translatable("message.theoreticalsciences.glyphs_learned", list).withStyle(ChatFormatting.LIGHT_PURPLE), false);
        sp.level().playSound(null, sp.blockPosition(), com.theoreticalsciences.registry.QHSounds.CONFIRM.get(), SoundSource.PLAYERS, 0.6F, 1.6F);
    }

    static boolean learned(ServerPlayer sp, ResearchSavedData.Entry entry, GlyphRegistry.Glyph g) {
        switch (g.unlock()) {
            case START -> {
                return true;
            }
            case RESEARCH -> {
                ResourceLocation r = ResourceLocation.tryParse(g.unlockId());
                return r != null && entry.unlocked.contains(r);
            }
            default -> {
                ResourceLocation r = ResourceLocation.tryParse(g.unlockId());
                if (r == null) return false;
                Item it = BuiltInRegistries.ITEM.getOptional(r).orElse(null);
                if (it == null) return false;
                if (sp.getInventory().contains(st -> st.is(it))) return true;
                var stats = sp.getStats();
                return stats.getValue(Stats.ITEM_PICKED_UP.get(it)) > 0 || stats.getValue(Stats.ITEM_CRAFTED.get(it)) > 0;
            }
        }
    }

    /** Whether this player may use every glyph in the set (creative players know everything). */
    public static boolean knowsAll(ServerPlayer sp, Set<String> glyphs) {
        if (sp.isCreative()) return true;
        ResearchSavedData.Entry e = ResearchSavedData.get(sp.server).of(sp.getUUID());
        for (String g : glyphs) if (GlyphRegistry.server().containsKey(g) && !e.glyphs.contains(g)) return false;
        return true;
    }

    private GlyphService() {}
}
