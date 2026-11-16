package com.theoreticalsciences.client;

import com.theoreticalsciences.machine.ControllerBlockEntity;
import com.theoreticalsciences.multiblock.MultiblockDefinition;
import com.theoreticalsciences.registry.QHSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;

/**
 * 1.5.1: a running multiblock is heard. Each formed structure plays its own working loop from the middle of the
 * structure while it runs, fading in when it starts and out when it stops or is unloaded. Bigger structures carry
 * further (16 blocks for a small one, up to 64 for the largest). This is the same interference the Radio Telescope has
 * always counted from running machines nearby; now the player can hear it as well.
 */
public final class MachineWorkSound extends AbstractTickableSoundInstance {
    private static final Map<BlockPos, MachineWorkSound> PLAYING = new HashMap<>();

    private final ControllerBlockEntity be;
    private final float full;
    private float fade;

    private MachineWorkSound(ControllerBlockEntity be, MachineVoices.Voice v) {
        super(QHSounds.WORK.get(v.sound()).get(), SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
        this.be = be;
        this.looping = true;
        this.delay = 0;
        this.pitch = v.pitch();
        this.attenuation = Attenuation.LINEAR;
        MultiblockDefinition def = be.definition();
        int size = def == null ? 3 : Math.max(def.sizeX, Math.max(def.sizeY, def.sizeZ));
        // linear attenuation reaches volume x 16 blocks, so the volume also sets how far it carries
        this.full = Math.max(1F, Math.min(4F, size / 12F));
        this.volume = 0.01F;
        Vec3 c = be.effectCenter();
        this.x = c.x; this.y = c.y; this.z = c.z;
    }

    /** Called every client tick by a running controller: starts its loop if it is not already playing. */
    public static void keepAlive(ControllerBlockEntity be) {
        MachineVoices.Voice v = MachineVoices.of(be.type.id());
        if (v == null || !QHSounds.WORK.containsKey(v.sound())) return;
        MachineWorkSound s = PLAYING.get(be.getBlockPos());
        if (s != null && !s.isStopped() && s.be == be) return;
        s = new MachineWorkSound(be, v);
        PLAYING.put(be.getBlockPos(), s);
        Minecraft.getInstance().getSoundManager().play(s);
    }

    private boolean running() {
        return !be.isRemoved() && be.getLevel() != null && be.getLevel() == Minecraft.getInstance().level
                && be.isFormed() && be.isActive();
    }

    @Override
    public void tick() {
        fade = running() ? Math.min(1F, fade + 0.05F) : fade - 0.05F;
        if (fade <= 0F) {
            PLAYING.remove(be.getBlockPos(), this);
            stop();
            return;
        }
        // the loop's level (0..1 of full) times how far it should carry; never quite silent so the instance stays alive
        this.volume = Math.max(0.01F, fade * full);
        Vec3 c = be.effectCenter();
        this.x = c.x; this.y = c.y; this.z = c.z;
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }
}
