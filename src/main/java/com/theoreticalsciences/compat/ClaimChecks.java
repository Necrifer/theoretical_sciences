package com.theoreticalsciences.compat;

import com.theoreticalsciences.TheoreticalSciences;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.neoforged.fml.ModList;

import javax.annotation.Nullable;
import java.lang.reflect.Method;

/**
 * 1.2.0: may a player's power reach this spot (or this creature) here? Spawn protection (the game's own
 * {@code mayInteract}), then the claim mods: FTB Chunks for now, looked up by reflection so the mod needs nothing of it
 * to load (another claim mod is one more method here). Allies, a claim's own settings and FTB's bypass mode are all
 * FTB Chunks' to decide.
 */
public final class ClaimChecks {
    /** What is being done: building or placing (creating creatures), or acting on a creature. */
    public enum Kind { PLACE, ENTITY }

    /** True when sp may do kind at pos (to target, for ENTITY). */
    public static boolean mayAffect(ServerPlayer sp, BlockPos pos, @Nullable Entity target, Kind kind) {
        if (!sp.level().mayInteract(sp, pos)) return false;
        if (ftb != Boolean.FALSE && ModList.get().isLoaded("ftbchunks") && ftbPrevents(sp, pos, target, kind)) return false;
        return true;
    }

    // ------------------------------------------------------------------ FTB Chunks
    private static Boolean ftb;                 // null: not looked up yet; FALSE: unusable
    private static Method api, isLoaded, manager, prevent;
    private static Object editBlock, interactEntity;

    private static boolean ftbPrevents(ServerPlayer sp, BlockPos pos, @Nullable Entity target, Kind kind) {
        try {
            if (ftb == null) {
                Class<?> apiC = Class.forName("dev.ftb.mods.ftbchunks.api.FTBChunksAPI");
                Class<?> apiI = Class.forName("dev.ftb.mods.ftbchunks.api.FTBChunksAPI$API");
                Class<?> mgr = Class.forName("dev.ftb.mods.ftbchunks.api.ClaimedChunkManager");
                Class<?> prot = Class.forName("dev.ftb.mods.ftbchunks.api.Protection");
                api = apiC.getMethod("api");
                isLoaded = apiI.getMethod("isManagerLoaded");
                manager = apiI.getMethod("getManager");
                prevent = mgr.getMethod("shouldPreventInteraction", Entity.class, InteractionHand.class, BlockPos.class, prot, Entity.class);
                editBlock = prot.getField("EDIT_BLOCK").get(null);
                interactEntity = prot.getField("INTERACT_ENTITY").get(null);
                ftb = Boolean.TRUE;
            }
            Object a = api.invoke(null);
            if (a == null || !(Boolean) isLoaded.invoke(a)) return false;
            Object m = manager.invoke(a);
            Object p = kind == Kind.ENTITY ? interactEntity : editBlock;
            return (Boolean) prevent.invoke(m, sp, InteractionHand.MAIN_HAND, pos, p, target);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            if (ftb != Boolean.FALSE) TheoreticalSciences.LOGGER.warn("FTB Chunks claims could not be checked (an unexpected version?): {}", e.toString());
            ftb = Boolean.FALSE;
            // can't ask it (logged once): the spawn protection check still stands
            return false;
        }
    }

    private ClaimChecks() {}
}
