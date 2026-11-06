package com.theoreticalsciences.client.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.theoreticalsciences.TheoreticalSciences;
import net.minecraft.client.renderer.ShaderInstance;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import javax.annotation.Nullable;

import java.io.IOException;

public final class QHShaders {
    @Nullable public static ShaderInstance LENS;
    @Nullable public static ShaderInstance ACCRETION;
    @Nullable public static ShaderInstance DISTORT;
    /** 1.2.0: the animated multiblocks' star surface and space shell (null: drawn in plain colour). */
    @Nullable public static ShaderInstance STAR;
    @Nullable public static ShaderInstance STARFIELD;

    public static void register(RegisterShadersEvent event) {
        try {
            event.registerShader(new ShaderInstance(event.getResourceProvider(), TheoreticalSciences.id("ts_lens"), DefaultVertexFormat.POSITION_TEX), s -> LENS = s);
            event.registerShader(new ShaderInstance(event.getResourceProvider(), TheoreticalSciences.id("ts_accretion"), DefaultVertexFormat.POSITION_TEX_COLOR), s -> ACCRETION = s);
        } catch (IOException e) {
            TheoreticalSciences.LOGGER.error("Failed to load Theoretical Sciences shaders - lensing effects disabled", e);
            LENS = null;
            ACCRETION = null;
        }
        // separate, so a problem with one effect never takes the others down
        try {
            event.registerShader(new ShaderInstance(event.getResourceProvider(), TheoreticalSciences.id("ts_distort"), DefaultVertexFormat.POSITION_TEX), s -> DISTORT = s);
        } catch (IOException e) {
            TheoreticalSciences.LOGGER.error("Failed to load the space distortion shader - structure distortion disabled", e);
            DISTORT = null;
        }
        try {
            event.registerShader(new ShaderInstance(event.getResourceProvider(), TheoreticalSciences.id("ts_star"), DefaultVertexFormat.POSITION_COLOR), s -> STAR = s);
            event.registerShader(new ShaderInstance(event.getResourceProvider(), TheoreticalSciences.id("ts_starfield"), DefaultVertexFormat.POSITION_COLOR), s -> STARFIELD = s);
        } catch (IOException e) {
            TheoreticalSciences.LOGGER.error("Failed to load the star shaders - stars are drawn in plain colour", e);
            STAR = null;
            STARFIELD = null;
        }
    }

    private QHShaders() {}
}
