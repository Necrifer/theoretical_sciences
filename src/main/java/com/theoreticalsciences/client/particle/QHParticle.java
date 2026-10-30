package com.theoreticalsciences.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;

/** Glowing, sprite-animated particles. Behaviour depends on {@link Kind}. */
public class QHParticle extends TextureSheetParticle {
    public enum Kind { TACHYON, FOAM, HAWKING, INFALL, DARK }

    private final SpriteSet sprites;
    private final Kind kind;
    private final double tx, ty, tz;

    protected QHParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, SpriteSet sprites, Kind kind) {
        super(level, x, y, z);
        this.sprites = sprites;
        this.kind = kind;
        this.tx = vx;
        this.ty = vy;
        this.tz = vz;
        this.hasPhysics = false;
        this.gravity = 0;
        switch (kind) {
            case TACHYON -> {
                xd = vx; yd = vy; zd = vz;
                lifetime = 14 + random.nextInt(8);
                quadSize = 0.09F;
                setColor(1.0F, 0.95F, 0.6F);
            }
            case FOAM -> {
                xd = (random.nextDouble() - 0.5) * 0.02; yd = (random.nextDouble() - 0.5) * 0.02; zd = (random.nextDouble() - 0.5) * 0.02;
                lifetime = 8 + random.nextInt(10);
                quadSize = 0.05F + random.nextFloat() * 0.05F;
                float hue = random.nextFloat();
                setColor(0.4F + hue * 0.6F, 0.7F, 1.0F - hue * 0.5F);
            }
            case HAWKING -> {
                xd = vx; yd = vy; zd = vz;
                lifetime = 20 + random.nextInt(20);
                quadSize = 0.06F;
                setColor(0.85F, 0.75F, 1.0F);
            }
            case INFALL -> {
                lifetime = 40 + random.nextInt(20);
                quadSize = 0.08F;
                setColor(1.0F, 0.7F, 0.35F);
            }
            case DARK -> {
                xd = (random.nextDouble() - 0.5) * 0.01; yd = 0.005; zd = (random.nextDouble() - 0.5) * 0.01;
                lifetime = 40 + random.nextInt(40);
                quadSize = 0.12F;
                setColor(0.35F, 0.28F, 0.9F);
                alpha = 0.6F;
            }
        }
        setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        setSpriteFromAge(sprites);
        float life = (float) age / lifetime;
        switch (kind) {
            case INFALL -> {
                // spiral toward target (passed as velocity args)
                double dx = tx - x, dy = ty - y, dz = tz - z;
                double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
                if (dist < 0.25) { remove(); return; }
                double pull = 0.06 / Math.max(0.3, dist);
                xd = xd * 0.9 + dx * pull - dz * 0.05;
                zd = zd * 0.9 + dz * pull + dx * 0.05;
                yd = yd * 0.9 + dy * pull;
                setColor(1.0F, 0.7F + 0.3F * (float) (1 / (1 + dist)), 0.35F + 0.6F * (float) (1 / (1 + dist)));
            }
            case FOAM -> alpha = (float) Math.sin(Math.PI * life);
            case DARK -> alpha = 0.6F * (float) Math.sin(Math.PI * life);
            case HAWKING -> {
                alpha = 1.0F - life;
                xd *= 0.96; yd *= 0.96; zd *= 0.96;
            }
            case TACHYON -> alpha = 1.0F - life * 0.5F;
        }
    }

    @Override
    public void render(com.mojang.blaze3d.vertex.VertexConsumer buffer, net.minecraft.client.Camera camera, float partial) {
        super.render(buffer, camera, partial);
        if (kind == Kind.TACHYON) {
            // Tachyons arrive before they leave: draw ghost images AHEAD of the particle, fading forward.
            double ox = x, oy = y, oz = z, oxo = xo, oyo = yo, ozo = zo;
            float oa = alpha, os = quadSize;
            for (int i = 1; i <= 4; i++) {
                x = ox + xd * i * 1.4; y = oy + yd * i * 1.4; z = oz + zd * i * 1.4;
                xo = oxo + xd * i * 1.4; yo = oyo + yd * i * 1.4; zo = ozo + zd * i * 1.4;
                alpha = oa * (1.0F - i * 0.22F);
                quadSize = os * (1.0F - i * 0.15F);
                super.render(buffer, camera, partial);
            }
            x = ox; y = oy; z = oz; xo = oxo; yo = oyo; zo = ozo; alpha = oa; quadSize = os;
        }
    }

    @Override
    protected int getLightColor(float partial) {
        return 0xF000F0;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public record Provider(SpriteSet sprites, Kind kind) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double vx, double vy, double vz) {
            return new QHParticle(level, x, y, z, vx, vy, vz, sprites, kind);
        }
    }
}
