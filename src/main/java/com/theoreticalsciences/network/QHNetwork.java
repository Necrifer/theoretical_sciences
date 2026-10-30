package com.theoreticalsciences.network;

import com.theoreticalsciences.research.ResearchService;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class QHNetwork {
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar r = event.registrar("1").optional();
        r.playToClient(ResearchDefsPayload.TYPE, ResearchDefsPayload.CODEC,
                (p, ctx) -> com.theoreticalsciences.client.ClientHooks.receiveResearchDefs(p.json()));
        r.playToClient(ResearchStatePayload.TYPE, ResearchStatePayload.CODEC,
                (p, ctx) -> com.theoreticalsciences.client.ClientHooks.receiveResearchState(p.unlocked(), p.insight(), p.observations(), p.active(), p.total(), p.remaining()));
        r.playToClient(DestabilisedPayload.TYPE, DestabilisedPayload.CODEC, (p, ctx) -> {
            if (p.on()) com.theoreticalsciences.client.ClientHooks.DESTABILISED.add(p.entityId());
            else com.theoreticalsciences.client.ClientHooks.DESTABILISED.remove(p.entityId());
        });
        r.playToServer(UnlockResearchPayload.TYPE, UnlockResearchPayload.CODEC, (p, ctx) -> {
            if (ctx.player() instanceof ServerPlayer sp) {
                ResearchService.UnlockResult res = ResearchService.tryUnlock(sp, p.id());
                if (res != ResearchService.UnlockResult.OK && res != ResearchService.UnlockResult.ALREADY && res != ResearchService.UnlockResult.STARTED && res != ResearchService.UnlockResult.NEEDS_PROOF)
                    sp.displayClientMessage(Component.translatable("message.theoreticalsciences.research_" + res.name().toLowerCase(java.util.Locale.ROOT))
                            .withStyle(ChatFormatting.RED), true);
            }
        });
        r.playToServer(TankClickPayload.TYPE, TankClickPayload.CODEC, (p, ctx) -> {
            if (ctx.player() instanceof ServerPlayer sp) TankClickPayload.handle(sp, p.tank());
        });
        r.playToServer(PuzzlePayload.TYPE, PuzzlePayload.CODEC, (p, ctx) -> {
            if (ctx.player() instanceof ServerPlayer sp) ResearchService.handlePuzzle(sp, p);
        });
        r.playToClient(PuzzleStatePayload.TYPE, PuzzleStatePayload.CODEC, (p, ctx) -> com.theoreticalsciences.client.ClientResearch.setPuzzles(p.state()));
        r.playToClient(ContainmentFailurePayload.TYPE, ContainmentFailurePayload.CODEC,
                (p, ctx) -> com.theoreticalsciences.client.ClientHooks.containmentFailure(p.pos(), p.hazard()));
        r.playToClient(CollapseConfirmPayload.TYPE, CollapseConfirmPayload.CODEC,
                (p, ctx) -> com.theoreticalsciences.client.ClientHooks.confirmCollapse(p.pos()));
        r.playToServer(CollapseAnswerPayload.TYPE, CollapseAnswerPayload.CODEC, (p, ctx) -> {
            if (ctx.player() instanceof ServerPlayer sp && sp.level().isLoaded(p.pos()) && sp.blockPosition().closerThan(p.pos(), 96)
                    && sp.level().getBlockEntity(p.pos()) instanceof com.theoreticalsciences.machine.ControllerBlockEntity c) c.answerCollapse(sp, p.yes());
        });
        r.playToServer(com.theoreticalsciences.transcend.OmnipotencePayload.TYPE, com.theoreticalsciences.transcend.OmnipotencePayload.CODEC, (p, ctx) -> {
            if (ctx.player() instanceof ServerPlayer sp) com.theoreticalsciences.transcend.OmnipotencePayload.handle(sp, p);
        });
        r.playToClient(com.theoreticalsciences.transcend.CorePayloads.Open.TYPE, com.theoreticalsciences.transcend.CorePayloads.Open.CODEC,
                (p, ctx) -> com.theoreticalsciences.client.ClientHooks.openOmnipotence(p.core(), p.phase(), p.seconds()));
        r.playToClient(com.theoreticalsciences.transcend.ForgeDrift.Sync.TYPE, com.theoreticalsciences.transcend.ForgeDrift.Sync.CODEC,
                (p, ctx) -> com.theoreticalsciences.client.screen.ForgeDriftClient.accept(p.data()));
        r.playToServer(com.theoreticalsciences.transcend.CorePayloads.Action.TYPE, com.theoreticalsciences.transcend.CorePayloads.Action.CODEC, (p, ctx) -> {
            if (ctx.player() instanceof ServerPlayer sp) com.theoreticalsciences.transcend.CorePayloads.Action.handle(sp, p);
        });
        // round 34c: the forged Core's attunement
        r.playToClient(com.theoreticalsciences.transcend.AttunePayloads.State.TYPE, com.theoreticalsciences.transcend.AttunePayloads.State.CODEC,
                (p, ctx) -> com.theoreticalsciences.client.ClientHooks.attune(p.core(), p.tag()));
        r.playToServer(com.theoreticalsciences.transcend.AttunePayloads.Set.TYPE, com.theoreticalsciences.transcend.AttunePayloads.Set.CODEC, (p, ctx) -> {
            if (ctx.player() instanceof ServerPlayer sp) com.theoreticalsciences.transcend.AttunePayloads.Set.handle(sp, p);
        });
        r.playToClient(DistortionPayload.TYPE, DistortionPayload.CODEC,
                (p, ctx) -> com.theoreticalsciences.client.DistortionRenderer.receive(p.mode(), p.intensity(), p.centre(), p.radius()));
        // also sent while the client is still joining (a configuration task), so JEI already knows every singularity
        r.commonToClient(SingularityDefsPayload.TYPE, SingularityDefsPayload.CODEC, (p, ctx) -> {
            com.theoreticalsciences.extreme.Singularities.receive(p.json());
            com.theoreticalsciences.client.SingularityColours.clear();
        });
        r.playToClient(GlyphDefsPayload.TYPE, GlyphDefsPayload.CODEC,
                (p, ctx) -> com.theoreticalsciences.research.GlyphRegistry.loadClient(p.json()));
        r.playToClient(PhysicsDefsPayload.TYPE, PhysicsDefsPayload.CODEC,
                (p, ctx) -> com.theoreticalsciences.machine.physics.PhysicsManager.loadClient(p.json()));
        r.playToServer(PhysicsSetPayload.TYPE, PhysicsSetPayload.CODEC, (p, ctx) -> {
            if (ctx.player() instanceof ServerPlayer sp && sp.containerMenu instanceof com.theoreticalsciences.machine.MachineMenu menu
                    && menu.be.getBlockPos().equals(p.pos()) && menu.inReach(sp)      // (near the controller or one of its ports)
                    && menu.be instanceof com.theoreticalsciences.machine.ControllerBlockEntity c) c.applyPhysics(p.index(), p.value());
        });
        r.playToServer(TuningPayload.TYPE, TuningPayload.CODEC, (p, ctx) -> {
            // only accept changes from a player standing at the machine with its screen open
            if (ctx.player() instanceof ServerPlayer sp && sp.containerMenu instanceof com.theoreticalsciences.machine.MachineMenu menu
                    && menu.be.getBlockPos().equals(p.pos()) && menu.inReach(sp)      // (near the controller or one of its ports)
                    && menu.be instanceof com.theoreticalsciences.machine.ControllerBlockEntity c) {
                if (p.field() == 3) c.toggleOmega(sp);
                else c.applyTuning(p.field(), Math.max(-500, Math.min(500, p.delta())));
            }
        });
        // round 27: dimension travel animations / loading pages, and the Pleroma Reconstitutor's finale
        r.playToClient(com.theoreticalsciences.world.transit.TransitPayload.TYPE, com.theoreticalsciences.world.transit.TransitPayload.CODEC,
                (p, ctx) -> com.theoreticalsciences.client.transit.TransitClient.receive(p));
        r.playToClient(com.theoreticalsciences.world.transit.RekindlePayload.TYPE, com.theoreticalsciences.world.transit.RekindlePayload.CODEC,
                (p, ctx) -> com.theoreticalsciences.client.cinematic.RekindleCinematicClient.receive(p));
        com.theoreticalsciences.endgame.EndgameNetwork.register(r);
        com.theoreticalsciences.custom.PackData.registerPayload(r);      // round 31: datapack component tiers / forge recipes / machine numbers
    }

    private QHNetwork() {}
}
