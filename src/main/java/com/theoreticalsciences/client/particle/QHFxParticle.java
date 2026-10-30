package com.theoreticalsciences.client.particle;

import com.theoreticalsciences.registry.QHGlowOptions;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;

/**
 * Round 36: the mod's own versions of the particles it used to borrow from vanilla. One class, a behaviour per
 * {@link Kind}: glowing, sprite-animated and tinted, drawn in the mod's colours.
 */
public class QHFxParticle extends TextureSheetParticle {
    public enum Kind { PHOTON, PHASE, ARC, FLASH, GLYPH, INK, RING, BURST, VAPOUR, FAULT, RESOLVE, PLASMA, GLOW }

    private final SpriteSet sprites;
    private final Kind kind;
    private final float size0;
    private final boolean animate;

    protected QHFxParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, SpriteSet sprites, Kind kind, int rgb, float scale) {
        super(level, x, y, z);
        this.sprites = sprites;
        this.kind = kind;
        this.hasPhysics = false;
        this.gravity = 0;
        xd = vx;
        yd = vy;
        zd = vz;
        boolean anim = true;
        switch (kind) {
            case PHOTON -> {
                lifetime = 30 + random.nextInt(30);
                quadSize = 0.08F + random.nextFloat() * 0.04F;
                tint(0xFFF2D0);
            }
            case PHASE -> {
                lifetime = 30 + random.nextInt(20);
                quadSize = 0.07F + random.nextFloat() * 0.05F;
                tint(random.nextBoolean() ? 0xB070FF : 0x8060FF);
                // like the portal motes it replaces, the velocity given is the whole way it travels in its life
                xd = vx / lifetime;
                yd = vy / lifetime + 0.004;
                zd = vz / lifetime;
            }
            case ARC -> {
                lifetime = 3 + random.nextInt(4);
                quadSize = 0.12F + random.nextFloat() * 0.08F;
                tint(random.nextInt(3) == 0 ? 0xFFFFFF : 0x70E0FF);
                roll = oRoll = random.nextFloat() * 6.28F;
                anim = false;
            }
            case FLASH -> {
                lifetime = 4;
                quadSize = 1.6F;
                tint(0xFFFFFF);
            }
            case GLYPH -> {
                lifetime = 30 + random.nextInt(25);
                quadSize = 0.1F + random.nextFloat() * 0.05F;
                tint(random.nextBoolean() ? 0x80E8FF : 0xC090FF);
                anim = false;
                // (the enchanting runes it replaces were given the whole offset they travel, not a speed)
                xd = vx / lifetime * 0.6;
                yd = Math.abs(vy) / lifetime + 0.012;
                zd = vz / lifetime * 0.6;
            }
            case INK -> {
                lifetime = 25 + random.nextInt(15);
                quadSize = 0.25F + random.nextFloat() * 0.15F;
                tint(0x3A2A50);
                alpha = 0.85F;
            }
            case RING -> {
                lifetime = 10;
                quadSize = 0.3F;
                tint(0xD0E8FF);
                xd = yd = zd = 0;
            }
            case BURST -> {
                lifetime = 8 + random.nextInt(4);
                quadSize = 1.0F + random.nextFloat();
                tint(0xFFC070);
            }
            case VAPOUR -> {
                lifetime = 30 + random.nextInt(30);
                quadSize = 0.2F + random.nextFloat() * 0.2F;
                tint(0xB8C4D8);
                alpha = 0.5F;
                yd += 0.02;
            }
            case FAULT -> {
                lifetime = 20;
                quadSize = 0.15F;
                tint(0xFF4A3A);
                yd = 0.02;
                xd = zd = 0;
            }
            case RESOLVE -> {
                lifetime = 20;
                quadSize = 0.15F;
                tint(0x60FFA0);
                yd = 0.02;
                xd = zd = 0;
            }
            case PLASMA -> {
                lifetime = 12 + random.nextInt(10);
                quadSize = 0.1F + random.nextFloat() * 0.05F;
                tint(0xFFB060);
                yd += 0.01;
            }
            case GLOW -> {
                lifetime = 14 + random.nextInt(12);
                quadSize = 0.1F * Math.max(0.2F, scale);
                tint(rgb);
                xd = xd * 0.2;
                yd = yd * 0.2;
                zd = zd * 0.2;
            }
        }
        this.animate = anim;
        this.size0 = quadSize;
        if (anim) setSpriteFromAge(sprites);
        else setSprite(sprites.get(random.nextInt(8), 8));
    }

    private void tint(int rgb) {
        setColor(((rgb >> 16) & 255) / 255F, ((rgb >> 8) & 255) / 255F, (rgb & 255) / 255F);
    }

    @Override
    public void tick() {
        super.tick();
        if (removed) return;
        if (animate) setSpriteFromAge(sprites);
        float life = (float) age / lifetime;
        switch (kind) {
            case PHOTON -> {
                xd *= 0.92; yd *= 0.92; zd *= 0.92;
                alpha = (1 - life) * (0.8F + 0.2F * random.nextFloat());
            }
            case PHASE -> {
                alpha = (float) Math.sin(Math.PI * life);
                quadSize = size0 * (1 - life * 0.5F);
            }
            case ARC -> {
                xd *= 0.6; yd *= 0.6; zd *= 0.6;
                setSprite(sprites.get(random.nextInt(8), 8));
                alpha = random.nextFloat() < 0.3F ? 0.4F : 1F;
            }
            case FLASH -> {
                alpha = 1 - life;
                quadSize = size0 * (1 + life * 0.6F);
            }
            case GLYPH -> {
                alpha = (float) Math.sin(Math.PI * life);
            }
            case INK -> {
                xd *= 0.9; yd *= 0.9; zd *= 0.9;
                quadSize = size0 * (1 + life);
                alpha = 0.85F * (1 - life);
            }
            case RING -> {
                quadSize = 0.3F + 2.7F * life;
                alpha = 1 - life * life;
            }
            case BURST -> {
                quadSize = size0 * (1 + life * 1.5F);
                alpha = 1 - life;
                setColor(1F, 0.75F - 0.35F * life, 0.45F - 0.3F * life);
            }
            case VAPOUR -> {
                xd *= 0.95; zd *= 0.95; yd = yd * 0.96 + 0.001;
                quadSize = size0 * (1 + life * 1.5F);
                alpha = 0.5F * (1 - life);
            }
            case FAULT, RESOLVE -> alpha = 1 - life * life;
            case PLASMA -> {
                xd *= 0.9; zd *= 0.9;
                quadSize = size0 * (1 - life * 0.7F);
                alpha = 1 - life;
                setColor(1F, 0.85F - 0.45F * life, 0.6F - 0.5F * life);
            }
            case GLOW -> {
                xd *= 0.9; yd *= 0.9; zd *= 0.9;
                alpha = 1 - life;
            }
        }
    }

    @Override
    protected int getLightColor(float partial) {
        return kind == Kind.INK || kind == Kind.VAPOUR ? super.getLightColor(partial) : 0xF000F0;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public record Provider(SpriteSet sprites, Kind kind) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double vx, double vy, double vz) {
            return new QHFxParticle(level, x, y, z, vx, vy, vz, sprites, kind, 0xFFFFFF, 1F);
        }
    }

    public record GlowProvider(SpriteSet sprites) implements ParticleProvider<QHGlowOptions> {
        @Override
        public Particle createParticle(QHGlowOptions o, ClientLevel level, double x, double y, double z, double vx, double vy, double vz) {
            return new QHFxParticle(level, x, y, z, vx, vy, vz, sprites, Kind.GLOW, o.rgb(), o.scale());
        }
    }
}
