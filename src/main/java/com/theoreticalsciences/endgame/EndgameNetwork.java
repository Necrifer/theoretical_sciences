package com.theoreticalsciences.endgame;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** Payloads of the round 15 endgame (the Lawgiver's Codex). */
public final class EndgameNetwork {
    public static void register(PayloadRegistrar r) {
        r.playToServer(CodexActionPayload.TYPE, CodexActionPayload.CODEC, (p, ctx) -> {
            if (ctx.player() instanceof ServerPlayer sp) CodexLaws.handle(sp, p);
        });
        r.playToServer(CenserPayload.TYPE, CenserPayload.CODEC, (p, ctx) -> {
            if (ctx.player() instanceof ServerPlayer sp) EntropyCenserItem.set(sp, new EntropyCenserItem.Settings(p.effect(), p.seconds(), p.level(), p.radius()));
        });
        r.playToServer(ExcavatorPayload.TYPE, ExcavatorPayload.CODEC, (p, ctx) -> {
            if (ctx.player() instanceof ServerPlayer sp) DodecaractExcavatorItem.changeRadius(sp, p.delta());
        });
        r.playToClient(CodexStatePayload.TYPE, CodexStatePayload.CODEC, (p, ctx) -> EndgameClient.receiveState(p));
        r.playToClient(SingularityFxPayload.TYPE, SingularityFxPayload.CODEC, (p, ctx) -> com.theoreticalsciences.client.ClientHooks.singularity(p));
    }

    private EndgameNetwork() {}
}
