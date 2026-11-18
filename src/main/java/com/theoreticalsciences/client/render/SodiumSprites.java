package com.theoreticalsciences.client.render;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.neoforged.fml.ModList;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/**
 * 1.3.0: with Sodium's "animate only visible textures" on, Sodium moves an animated texture on only when it has seen
 * something drawn with it. The moving machine parts are drawn from buffers kept on the graphics card, which Sodium
 * never sees, so their animated textures could look frozen. This tells Sodium they are in use. It does nothing when
 * Sodium is not installed, and it does not need Sodium to build (it looks the call up by name when the game starts).
 */
public final class SodiumSprites {
    private static final MethodHandle MARK = find();

    private static MethodHandle find() {
        if (!ModList.get().isLoaded("sodium")) return null;
        for (String cls : new String[]{"net.caffeinemc.mods.sodium.api.texture.SpriteUtil",          // Sodium 0.6 and later
                "net.caffeinemc.mods.sodium.client.render.texture.SpriteUtil",
                "me.jellysquid.mods.sodium.client.render.texture.SpriteUtil"}) {            // older
            try {
                Class<?> c = Class.forName(cls);
                MethodHandles.Lookup l = MethodHandles.publicLookup();
                try {                                           // an interface with an INSTANCE
                    Object inst = c.getField("INSTANCE").get(null);
                    return l.findVirtual(c, "markSpriteActive", MethodType.methodType(void.class, TextureAtlasSprite.class)).bindTo(inst);
                } catch (NoSuchFieldException e) {              // or a plain static method
                    return l.findStatic(c, "markSpriteActive", MethodType.methodType(void.class, TextureAtlasSprite.class));
                }
            } catch (Throwable ignored) {
                // try the next name
            }
        }
        return null;
    }

    public static boolean active() {
        return MARK != null;
    }

    public static void mark(TextureAtlasSprite[] sprites) {
        if (MARK == null || sprites == null) return;
        for (TextureAtlasSprite s : sprites) {
            try {
                MARK.invoke(s);
            } catch (Throwable ignored) {
                // never let a compatibility call break drawing
            }
        }
    }

    /** True if the texture moves (has more than one distinct frame). */
    public static boolean animated(TextureAtlasSprite s) {
        return s.contents().getUniqueFrames().limit(2).count() > 1;
    }

    private SodiumSprites() {}
}
