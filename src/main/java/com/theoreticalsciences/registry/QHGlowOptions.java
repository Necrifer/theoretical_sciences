package com.theoreticalsciences.registry;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** Round 36: a soft glowing mote in any colour - the mod's own replacement for vanilla's coloured dust. */
public record QHGlowOptions(int rgb, float scale) implements ParticleOptions {
    public static final MapCodec<QHGlowOptions> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.INT.fieldOf("rgb").forGetter(QHGlowOptions::rgb),
            Codec.FLOAT.optionalFieldOf("scale", 1F).forGetter(QHGlowOptions::scale)
    ).apply(i, QHGlowOptions::new));
    public static final StreamCodec<ByteBuf, QHGlowOptions> STREAM = StreamCodec.composite(
            ByteBufCodecs.INT, QHGlowOptions::rgb, ByteBufCodecs.FLOAT, QHGlowOptions::scale, QHGlowOptions::new);

    /** From the colour components and size vanilla's dust used (0..1 each). */
    public static QHGlowOptions of(float r, float g, float b, float scale) {
        return new QHGlowOptions(((int) (r * 255) & 255) << 16 | ((int) (g * 255) & 255) << 8 | ((int) (b * 255) & 255), scale);
    }

    @Override
    public ParticleType<?> getType() {
        return QHParticles.GLOW.get();
    }

    public static ParticleType<QHGlowOptions> type() {
        return new ParticleType<>(false) {
            @Override
            public MapCodec<QHGlowOptions> codec() {
                return CODEC;
            }

            @Override
            public StreamCodec<? super net.minecraft.network.RegistryFriendlyByteBuf, QHGlowOptions> streamCodec() {
                return STREAM;
            }
        };
    }
}
