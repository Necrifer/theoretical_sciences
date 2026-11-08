package com.theoreticalsciences.client.render;

import com.theoreticalsciences.TheoreticalSciences;
import com.theoreticalsciences.world.QHDimensions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;
import org.joml.Vector3f;

/**
 * 1.3.0: the Hadal Trench is the Challenger Deep: eleven kilometres of water over you, and no light has ever reached it.
 * <ul>
 *   <li><b>Pitch dark.</b> No sunlight and no ambient light at all; the lightmap is black except right next to a light
 *   source. A torch or lantern lights only the few blocks around it (block light 11 and up: three or four blocks out),
 *   falling off steeply, as a lamp does in murky water.</li>
 *   <li><b>Bioluminescence</b> is drawn full-bright (the creatures' glowing parts ignore the lightmap), so in the dark it
 *   is the one thing you see from far off: an anglerfish's lure or a siphonophore's stingers, forty blocks away.</li>
 *   <li><b>Night vision</b> (the potion) lifts the dark, but the water is thick with marine snow: you see only about a
 *   dozen blocks into the murk.</li>
 *   <li><b>Low-light gear</b>: anything worn on the head tagged {@code theoreticalsciences:hadal_vision} sees the trench
 *   clearly, far into the water (for the bioluminescent and night-vision technology to come; datapacks may add to it).</li>
 * </ul>
 */
@EventBusSubscriber(modid = TheoreticalSciences.MODID, value = Dist.CLIENT)
public class HadalSkyEffects extends DimensionSpecialEffects {
    public static final TagKey<Item> HADAL_VISION = TagKey.create(Registries.ITEM, TheoreticalSciences.id("hadal_vision"));

    public HadalSkyEffects() {
        super(Float.NaN, false, SkyType.NONE, false, false);
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 color, float brightness) {
        return color;
    }

    @Override
    public boolean isFoggyAt(int x, int z) {
        return true;
    }

    /** How well the player sees down here: 0 (bare eyes), 1 (night vision), 2 (low-light gear). */
    public static int vision(Player p) {
        if (p == null) return 0;
        if (p.getItemBySlot(EquipmentSlot.HEAD).is(HADAL_VISION)) return 2;
        return p.hasEffect(MobEffects.NIGHT_VISION) ? 1 : 0;
    }

    /**
     * Light only near a source: block light below 11 is black, and from 11 to 15 it rises steeply. Sky light (there is
     * none) counts for nothing. Never exactly zero (night vision divides by the brightest channel).
     */
    @Override
    public void adjustLightmapColors(ClientLevel level, float partialTicks, float skyDarken, float blockLightRedFlicker, float skyLight,
                                     int pixelX, int pixelY, Vector3f colors) {
        if (vision(Minecraft.getInstance().player) == 2) return;                // the gear sees it as it is
        float k = pixelX >= 11 ? (float) Math.pow((pixelX - 10) / 5.0, 1.6) : 0F;
        float m = Math.max(colors.x, Math.max(colors.y, colors.z));
        if (m <= 0) {
            colors.set(0.004F, 0.004F, 0.006F);
            return;
        }
        float target = Math.max(0.004F, k * 0.95F);
        colors.mul(Math.min(1F, target / m));
        colors.set(Math.max(colors.x, 0.003F), Math.max(colors.y, 0.003F), Math.max(colors.z, 0.004F));
    }

    static boolean here() {
        var l = Minecraft.getInstance().level;
        return l != null && l.dimension() == QHDimensions.HADAL_TRENCH;
    }

    /** The water closes in: far with bare eyes (it is the dark that blinds you, and the glow carries), near with night vision. */
    @SubscribeEvent
    public static void onFog(ViewportEvent.RenderFog e) {
        if (!here() || e.getType() == FogType.LAVA || e.getType() == FogType.POWDER_SNOW) return;
        int v = vision(Minecraft.getInstance().player);
        float far = v == 2 ? 72F : v == 1 ? 14F : 40F;
        e.setNearPlaneDistance(v == 1 ? -2F : 0F);
        e.setFarPlaneDistance(far);
        e.setCanceled(true);
    }

    @SubscribeEvent
    public static void onFogColor(ViewportEvent.ComputeFogColor e) {
        if (!here()) return;
        int v = vision(Minecraft.getInstance().player);
        if (v == 0) e.setRed(0.0F);
        if (v == 0) {
            e.setGreen(0.004F);
            e.setBlue(0.012F);
        } else if (v == 1) {                                                   // the murk, lit
            e.setRed(0.03F);
            e.setGreen(0.07F);
            e.setBlue(0.09F);
        } else {
            e.setRed(0.01F);
            e.setGreen(0.05F);
            e.setBlue(0.10F);
        }
    }
}
